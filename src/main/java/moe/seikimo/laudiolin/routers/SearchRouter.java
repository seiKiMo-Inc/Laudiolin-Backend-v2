package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.SpotifyUtils;

import static moe.seikimo.laudiolin.utils.HttpUtils.NO_RESULTS;

public interface SearchRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/search/{query}", SearchRouter::searchFor);
        javalin.get("/fetch/{id}", SearchRouter::fetchTrack);
        javalin.get("/reverse/{id}", SearchRouter::reverseTrack);
    }

    /**
     * Performs a search request.
     *
     * @param ctx The context.
     */
    static void searchFor(Context ctx) {
        // Pull arguments.
        var query = ctx.pathParam("query");
        var engine = ctx.queryParam("engine");

        if (engine == null || engine.isEmpty()) {
            engine = "YouTube";
        }

        // Perform a search request.
        var source = Source.identify(engine, "");
        var node = Laudiolin.getNode();
        var results = switch (source) {
            case UNKNOWN -> null;
            case ALL, YOUTUBE -> {
                var search = node.youtubeSearch(query, source != Source.ALL);
                yield TrackData.toResults(search.stream()
                        .map(TrackData::toTrack)
                        .toList());
            }
            case SPOTIFY -> SpotifyUtils.search(query);
        };

        if (results == null) {
            ctx.status(404).json(NO_RESULTS());
        } else {
            ctx.status(301).json(results);
        }
    }

    /**
     * Fetches a track's data.
     *
     * @param ctx The context.
     */
    static void fetchTrack(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.pathParam("id");
            var engine = ctx.queryParam("engine");

            // Check if the arguments are valid.
            if (id.isEmpty()) {
                ctx.status(400).json(NO_RESULTS());
                return;
            }

            // Identify the engine.
            var source = Source.identify(engine, id);
            var node = Laudiolin.getNode();
            var results = switch (source) {
                case UNKNOWN -> null;
                case ALL, YOUTUBE -> {
                    var track = node.youtubeFetch(id);
                    yield TrackData.toTrack(track);
                }
                case SPOTIFY -> {
                    var track = SpotifyUtils.searchIsrc(id);
                    if (track == null) yield null;

                    yield SpotifyUtils.toTrackData(track);
                }
            };

            if (results == null) {
                ctx.status(404).json(NO_RESULTS());
            } else {
                ctx.status(301).json(results);
            }
        } catch (Exception ignored) {
            ctx.status(404).json(NO_RESULTS());
        }
    }

    /**
     * Reverses a track's ID.
     *
     * @param ctx The context.
     */
    static void reverseTrack(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.pathParam("id");
            var engine = ctx.queryParam("engine");

            // Check if the arguments are valid.
            if (id.isEmpty()) {
                ctx.status(400).json(NO_RESULTS());
                return;
            }

            // Identify the engine.
            var source = Source.identify(engine, id);
            var result = switch (source) {
                default -> null;
                case SPOTIFY -> SpotifyUtils.getSpotifyId(id);
            };

            if (result == null) {
                ctx.status(404).json(NO_RESULTS());
            } else {
                ctx.status(301).json(JObject.c()
                        .add("id", result)
                        .gson());
            }
        } catch (Exception ignored) {
            ctx.status(404).json(NO_RESULTS());
        }
    }
}
