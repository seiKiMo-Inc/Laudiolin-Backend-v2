package moe.seikimo.laudiolin;

import com.google.protobuf.GeneratedMessageV3;
import lombok.SneakyThrows;
import moe.seikimo.laudiolin.Messages.*;
import moe.seikimo.laudiolin.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class Node {
    private final Logger logger
            = LoggerFactory.getLogger("Node IPC");

    private final RandomAccessFile pipe;
    private final Thread readThread;

    private final Map<Integer, Consumer<byte[]>> listeners = new HashMap<>();

    /**
     * Creates a named IPC pipe to Node.js.
     *
     * @param name The name of the pipe.
     */
    public Node(String name) throws IOException {
        this.pipe = new RandomAccessFile("\\\\?\\pipe\\" + name, "rw");

        // Create the pipe reader.
        this.readThread = new Thread(this::read);
        this.readThread.start();
    }

    /**
     * Handles reading data from the pipe.
     */
    public void read() {
        final var pipe = this.pipe;

        while (true) try {
            var available = (int) pipe.length();

            // Check if the process should terminate.
            if (available == 1) break;
            if (available < 2) continue;

            // Read the data from the pipe.
            var buffer = ByteBuffer.allocate(available);
            pipe.read(buffer.array(), 0, available);

            // Check the packet origin.
            var origin = buffer.get();
            if (origin != 0x1) continue;

            // Parse the packet.
            var retcode = buffer.getInt();
            var packetLength = buffer.getInt();
            var packetData = new byte[packetLength];
            buffer.get(packetData, 0, packetLength);

            // Handle the packet.
            var listener = this.listeners.getOrDefault(retcode, null);
            if (listener != null) listener.accept(packetData);
        } catch (IOException exception) {
            this.logger.warn("Failed to read from pipe.", exception);
        }

        try {
            // Close the pipe.
            pipe.close();
        } catch (IOException exception) {
            this.logger.warn("Failed to close pipe.", exception);
        }

        System.exit(0);
    }

    /**
     * Writes a packet to the pipe.
     *
     * @param retcode The packet retcode.
     * @param packetId The packet ID.
     * @param data The packet data.
     */
    public void write(int retcode, PacketIds packetId, byte[] data) {
        // Prepare the packet.
        var buffer = ByteBuffer.allocate(13 + data.length);
        buffer.put((byte) 0x2); // Write the origin byte.
        buffer.putInt(retcode); // Write the packet retcode.
        buffer.putInt(packetId.getNumber()); // Write the packet ID.
        buffer.putInt(data.length); // Write the packet length.
        buffer.put(data, 0, data.length); // Write the packet data.

        try {
            // Write the packet to the pipe.
            this.pipe.write(buffer.array());
        } catch (IOException exception) {
            this.logger.warn("Failed to write to pipe.", exception);
        }
    }

    /**
     * Sends a packet to Node.js and expects a response.
     * The response data will be passed to the future for parsing.
     *
     * @param sendId The packet ID to send.
     * @param data The packet data to send.
     * @return The future.
     */
    private byte[] sendExpect(
            PacketIds sendId, GeneratedMessageV3.Builder<?> data
    ) {
        // Generate a random code for receiving the packet.
        var retcode = RandomUtils.random(0, 10000);
        // Create a future which will be resolved when the packet is received.
        var future = this.expect(retcode);
        // Send the packet.
        this.write(retcode, sendId, data.build().toByteArray());

        try {
            return future.get(10L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
            return new byte[0];
        }
    }

    /**
     * Creates a future which will be resolved when a packet is received.
     * The packet data will be passed to the future for parsing.
     *
     * @param retcode The packet retcode to listen for.
     * @return The future.
     */
    private CompletableFuture<byte[]> expect(int retcode) {
        // Prepare a future which will be returned.
        var future = new CompletableFuture<byte[]>();
        // Add a listener.
        this.listeners.put(retcode, future::complete);

        return future;
    }

    /* ---------------------------------------- UTILITY METHODS ---------------------------------------- */

    /**
     * Searches YouTube for a video.
     *
     * @param query The search query.
     * @param music Whether to search for music.
     * @return The search results.
     */
    @SneakyThrows
    public List<SearchResult> youtubeSearch(String query, boolean music) {
        // Send the packet and expect a response.
        var data = this.sendExpect(
                PacketIds._YouTubeSearchReq,
                YouTubeSearchReq.newBuilder()
                        .setQuery(query)
                        .setYoutubeMusic(music));
        var response = YouTubeSearchRsp.parseFrom(data);

        // Check if the search was successful.
        if (!response.getSuccessful())
            throw new RuntimeException("Failed to search YouTube.");

        return response.getResultsList();
    }

    /**
     * Attempts to download a YouTube video.
     *
     * @param id The YouTube video ID.
     * @return The path to the downloaded video. Returns an empty string if the download failed.
     */
    @SneakyThrows
    public String youtubeDownload(String id) {
        // Send the packet and expect a response.
        var data = this.sendExpect(
                PacketIds._YouTubeDownloadReq,
                YouTubeDownloadReq.newBuilder()
                        .setVideoId(id));
        var response = YouTubeDownloadRsp.parseFrom(data);

        return response.getFilePath();
    }

    /**
     * Attempts to download a part of a YouTube video.
     *
     * @param id The YouTube video ID.
     * @param quality The quality of the video.
     * @param start The starting byte.
     * @param end The ending byte.
     * @return The download response.
     */
    @SneakyThrows
    public YouTubeStreamRsp youtubeStream(
            String id, String quality, int start, int end
    ) {
        // Send the packet and expect a response.
        var data = this.sendExpect(
                PacketIds._YouTubeStreamReq,
                YouTubeStreamReq.newBuilder()
                        .setVideoId(id)
                        .setQuality(quality)
                        .setStart(start)
                        .setEnd(end));
        return YouTubeStreamRsp.parseFrom(data);
    }

    /**
     * Performs a YouTube search for a video ID.
     *
     * @param id The video ID.
     * @return The search result.
     */
    @SneakyThrows
    public SearchResult youtubeFetch(String id) {
        // Send the packet and expect a response.
        var data = this.sendExpect(
                PacketIds._YouTubeFetchReq,
                YouTubeFetchReq.newBuilder()
                        .setVideoId(id));
        var result = YouTubeFetchRsp.parseFrom(data);

        if (!result.getSuccessful()) {
            throw new RuntimeException("Failed to fetch YouTube video.");
        }

        return result.getResult();
    }
}
