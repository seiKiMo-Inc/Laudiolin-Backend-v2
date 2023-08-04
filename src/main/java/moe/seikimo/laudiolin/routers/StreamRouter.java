package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.utils.SpotifyUtils;

import java.io.FileInputStream;

import static moe.seikimo.laudiolin.utils.HttpUtils.INVALID_ARGUMENTS;

public interface StreamRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/download", StreamRouter::download);
//        javalin.get("/stream", StreamRouter::stream);
//        javalin.get("/cache", StreamRouter::cache);
    }

    /**
     * Downloads the specified video.
     *
     * @param ctx The context.
     */
    static void download(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.queryParam("id");
            var engine = ctx.queryParam("engine");

            // Validate arguments.
            if (id == null || id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Identify source.
            var source = Source.identify(engine, id);
            // Download the video.
            var node = Laudiolin.getNode();
            var path = switch (source) {
                case UNKNOWN -> "";
                case ALL, YOUTUBE -> node.youtubeDownload(id);
                case SPOTIFY -> {
                    // Get the track by ID.
                    var track = id.length() == 12 ?
                            SpotifyUtils.searchIsrc(id) :
                            SpotifyUtils.searchSpotifyId(id);
                    if (track == null) yield "";
                    var trackData = SpotifyUtils.toTrackData(track);

                    // Prepare a YouTube query.
                    var query = String.format("%s - %s - Topic",
                            trackData.getTitle(), trackData.getArtist());
                    // Perform a YouTube search.
                    var search = node.youtubeSearch(query, false);
                    if (search.isEmpty()) yield "";

                    // Download the file.
                    yield node.youtubeDownload(search.get(0).getId());
                }
            };

            // Validate the path.
            if (path.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Serve the file.
            ctx.status(200).result(
                    new FileInputStream(path));
        } catch (Exception exception) {
            ctx.status(500);
            Laudiolin.getLogger().warn("Failed to download video.", exception);
        }
    }
}
