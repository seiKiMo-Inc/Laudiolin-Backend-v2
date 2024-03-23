package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.files.LocalFileManager;
import moe.seikimo.laudiolin.files.LocalTrack;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.SpotifyUtils;

import java.util.ArrayList;

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

        // Get the local search results.
        var localResults = LocalFileManager.getLocalTracks().values().stream()
                .map(LocalTrack::data)
                .filter(track -> track.getTitle().toLowerCase().contains(query.toLowerCase()))
                .toList();

        // Perform a search request.
        var source = Source.identify(engine, "");
        var node = Laudiolin.getNode();
        var tracks = switch (source) {
            case UNKNOWN -> null;
            case ALL, YOUTUBE -> {
                var search = node.youtubeSearch(query, source != Source.ALL);
                yield search.stream()
                        .map(TrackData::toTrack)
                        .toList();
            }
            case SPOTIFY -> SpotifyUtils.search(query);
        };

        if (tracks == null) {
            ctx.status(404).json(NO_RESULTS());
        } else {
            var results = new ArrayList<>(localResults);
            if (Config.get().getStorage().isSearchRemote()) {
                results.addAll(tracks);
            }

            ctx
                    .status(301)
                    .header("Cache-Control", "public, max-age=86400")
                    .json(TrackData.toResults(results));
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
                case UNKNOWN -> {
                    // Check if the file is local.
                    var track = LocalFileManager.getLocalTracks().get(id);
                    if (track == null) yield null;

                    yield track.data();
                }
                case ALL, YOUTUBE -> {
                    var track = node.youtubeFetch(id);
                    yield TrackData.toTrack(track);
                }
                case SPOTIFY -> {
                    var track = id.length() == 22 ?
                            SpotifyUtils.searchId(id) :
                            SpotifyUtils.searchIsrc(id);
                    if (track == null) yield null;

                    yield SpotifyUtils.toTrackData(track);
                }
            };

            if (results == null) {
                ctx.status(404).json(NO_RESULTS());
            } else {
                if (!Config.get().storage.searchRemote &&
                        source != Source.UNKNOWN) {
                    ctx.status(404).json(NO_RESULTS());
                } else {
                    ctx.status(301).json(results);
                }
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
