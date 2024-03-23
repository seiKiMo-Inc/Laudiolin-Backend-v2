package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.files.LocalFileManager;
import moe.seikimo.laudiolin.utils.HttpUtils;
import moe.seikimo.laudiolin.utils.SpotifyUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static moe.seikimo.laudiolin.utils.HttpUtils.*;

public interface StreamRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/download", StreamRouter::download);
        javalin.get("/stream", StreamRouter::stream);
        javalin.get("/cache", StreamRouter::cache);
    }

    /**
     * Fetches the video path.
     *
     * @param ctx The context.
     * @return The path.
     */
    private static String fetchPathFor(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.queryParam("id");
            var engine = ctx.queryParam("engine");

            // Validate arguments.
            if (id == null || id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return null;
            }

            // Check if the ID is a local file.
            var localFile = LocalFileManager.getLocalTracks().get(id);
            if (localFile != null) {
                return localFile.trackFile().getAbsolutePath();
            }

            // Identify source.
            var source = Source.identify(engine, id);
            // Download the video.
            var node = Laudiolin.getNode();
            return switch (source) {
                case UNKNOWN -> "";
                case ALL, YOUTUBE -> node.youtubeDownload(id);
                case SPOTIFY -> {
                    // Get the YouTube ID.
                    id = SpotifyUtils.toYouTubeId(id);
                    // Download the file.
                    yield node.youtubeDownload(id);
                }
            };
        } catch (Exception exception) {
            ctx.status(500);
            Laudiolin.getLogger().warn("Failed to download video.", exception);

            return null;
        }
    }

    /**
     * Downloads the specified video.
     *
     * @param ctx The context.
     */
    static void download(Context ctx) {
        try {
            // Fetch the path.
            var path = StreamRouter.fetchPathFor(ctx);
            if (path == null) return;

            // Validate the path.
            if (path.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Serve the file.
            var file = Files.readAllBytes(Path.of(path));
            var data = file;

            // Determine the range.
            var range = HttpUtils.range(ctx);
            if (range != null) {
                // Get the range.
                var start = range.first();
                var end = range.second();
                if (end == -1) {
                    end = file.length - 1;
                }

                // Get the data.
                var length = end - start + 1;
                data = new byte[length];
                System.arraycopy(file, start, data, 0, length);
                ctx.header("Content-Range", "bytes " + start + "-" + end + "/" + file.length);
            }

            ctx
                    .status(200)
                    .contentType(ContentType.AUDIO_MPEG)
                    .header("Content-Length", String.valueOf(data.length))
                    .header("Cache-Control", "public, max-age=86400")
                    .result(data);
        } catch (Exception exception) {
            ctx.status(500);
            Laudiolin.getLogger().warn("Failed to download video.", exception);
        }
    }

    /**
     * Streams the specified video.
     *
     * @param ctx The context.
     */
    static void stream(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.queryParam("id");
            var quality = ctx.queryParam("quality");
            var engine = ctx.queryParam("engine");

            // Validate arguments.
            if (id == null || id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }
            if (quality == null) quality = "High";

            // Identify source.
            var source = Source.identify(engine, id);
            // Parse the range.
            var range = ctx.header("Range");
            int start = 0, end = 0;
            if (range != null && range.startsWith("bytes=")) {
                var parts = range.substring(6).split("-");
                start = Integer.parseInt(parts[0]);
                end = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            }
            if (end == 0) {
                end = (int) (start + 3e5);
            }

            // Check if the range is valid.
            if (start < 0 || end < 0 || start > end) {
                ctx.status(400).json(INTERNAL_ERROR("Invalid range of bytes."));
                return;
            }

            // Stream the video.
            var node = Laudiolin.getNode();
            var data = switch (source) {
                case UNKNOWN -> null;
                case ALL, YOUTUBE -> node.youtubeStream(id, quality, start, end);
                case SPOTIFY -> {
                    // Get the YouTube ID.
                    id = SpotifyUtils.toYouTubeId(id);
                    // Stream the file.
                    yield node.youtubeStream(id, quality, start, end);
                }
            };

            // Check if the ID is a local file.
            var localFile = LocalFileManager.getLocalTracks().get(id);

            // Validate the data.
            if (data == null && localFile == null) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Pull the data.
            var bytes = localFile == null ?
                    data.getData().toByteArray() :
                    Files.readAllBytes(localFile.trackFile().toPath());
            var totalLength = localFile == null ?
                    data.getContentLength() : bytes.length;
            if (end > totalLength) end = totalLength - 1;

            // Prepare the headers.
            if (range == null) {
                ctx
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Type", "audio/mpeg")
                        .header("Transfer-Encoding", "chunked")
                        .header("Connection", "keep-alive")
                        .status(HttpStatus.OK);
            } else {
                ctx
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Type", "audio/mpeg")
                        .header("Content-Length", String.valueOf(bytes.length))
                        .header("Content-Range", "bytes " + start + "-" + end + "/" + totalLength)
                        .header("Connection", "keep-alive")
                        .status(HttpStatus.PARTIAL_CONTENT);
            }

            // Send the bytes.
            try (var stream = ctx.outputStream()) {
                stream.write(bytes);
            }
        } catch (Exception exception) {
            ctx.status(500).json(INTERNAL_ERROR("Failed to stream audio."));
        }
    }

    /**
     * Downloads the specified video.
     * This only caches the song, it doesn't return it.
     *
     * @param ctx The context.
     */
    static void cache(Context ctx) {
        try {
            // Use the path method to download the video.
            StreamRouter.fetchPathFor(ctx);
            // Return the state.
            ctx.status(200).json(SUCCESS());
        } catch (Exception exception) {
            ctx.status(500);
            Laudiolin.getLogger().warn("Failed to download video.", exception);
        }
    }
}
