import { YouTubeStreamReq, YouTubeStreamRsp } from "@app/Messages";

import { WebSocket } from "ws";

import { sendPacket } from "@app/java";
import { extractId } from "@app/utils";

import { youtube } from "@app/index";
import { streamToIterable } from "@app/utils";

/**
 * Downloads a portion of a YouTube video.
 *
 * @param id The ID of the video to download.
 * @param quality The quality of the video to download.
 * @param min The minimum byte to download.
 * @param max The maximum byte to download.
 */
async function streamInternal(
    id: string, quality: string,
    min: number, max: number
): Promise<{ buffer: Uint8Array, length: number }> {
    // Fetch the data required for streaming.
    const streamingData =
        await youtube.getStreamingData(id, {
            type: "audio", quality: "best", format: "any"
        });
    const length = streamingData.content_length ?? 0;
    if (length == 0) throw new Error("Invalid content length.");

    // Download the video.
    const stream =
        await youtube.download(id, {
            type: "audio", format: "any",
            quality: quality == "High" ? "best" : "bestefficiency",
            range: { start: min, end: Math.min(max, length) }
        });

    // Convert the stream into a buffer.
    const buffer = await streamToIterable(stream);
    // Return the buffer.
    return { length, buffer };
}

export default async function(socket: WebSocket, retcode: number, req: Buffer) {
    const { videoId, quality, start, end } = YouTubeStreamReq.fromBinary(req);

    // Parse the video ID.
    let id = videoId;
    if (id.includes("http"))
        id = extractId(id);

    // Attempt to stream the video.
    const { buffer, length } =
        await streamInternal(id, quality, start, end);

    // Send the response packet.
    sendPacket(socket, retcode, YouTubeStreamRsp.toBinary(
        { data: buffer, contentLength: length }));
}
