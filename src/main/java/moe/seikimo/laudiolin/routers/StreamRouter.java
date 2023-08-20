package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.utils.SpotifyUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static moe.seikimo.laudiolin.utils.HttpUtils.INVALID_ARGUMENTS;
import static moe.seikimo.laudiolin.utils.HttpUtils.SUCCESS;

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
            ctx
                    .status(200)
                    .contentType(ContentType.AUDIO_MPEG)
                    .header("Content-Length", String.valueOf(file.length))
                    .result(file);
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
        // Pull arguments.
        var id = ctx.queryParam("id");
        var quality = ctx.queryParam("quality");
        var engine = ctx.queryParam("engine");

        // Validate arguments.
        if (id == null || id.isEmpty()) {
            ctx.status(400).json(INVALID_ARGUMENTS());
            return;
        }

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
            ctx.status(400).json(INVALID_ARGUMENTS());
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

        // Validate the data.
        if (data == null) {
            ctx.status(400).json(INVALID_ARGUMENTS());
            return;
        }

        // Pull the data.
        var buffer = data.getData();
        var bytes = buffer.toByteArray();
        var totalLength = data.getContentLength();
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
        } catch (IOException ignored) { }
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
