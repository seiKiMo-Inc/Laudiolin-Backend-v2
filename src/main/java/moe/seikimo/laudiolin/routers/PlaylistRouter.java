package moe.seikimo.laudiolin.routers;

import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.models.data.Playlist;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.utils.AccountUtils;
import moe.seikimo.laudiolin.utils.DatabaseUtils;
import moe.seikimo.laudiolin.utils.SpotifyUtils;

import java.util.Objects;

import static moe.seikimo.laudiolin.utils.HttpUtils.INVALID_ARGUMENTS;

public interface PlaylistRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.post("/playlist/create", PlaylistRouter::createPlaylist);
        javalin.patch("/playlist/import", PlaylistRouter::importPlaylist);
        // javalin.get("/playlist/{id}", PlaylistRouter::fetchPlaylist);
        // javalin.patch("/playlist/{id}", PlaylistRouter::updatePlaylist);
        // javalin.delete("/playlist/{id}", PlaylistRouter::deletePlaylist);
    }

    /**
     * Creates a playlist.
     *
     * @param ctx The Javalin context.
     */
    static void createPlaylist(Context ctx) {
        // Get the user info.
        var user = AccountUtils.getUser(ctx);
        if (user == null) return;

        // Check the body data.
        if (!Objects.equals(ctx.contentType(), "application/json")) {
            ctx.status(400).json(INVALID_ARGUMENTS());
            return;
        }

        try {
            // Try to parse the body data.
            var playlist = ctx.bodyAsClass(Playlist.class);
            if (playlist == null) throw new IllegalArgumentException("Invalid body data.");

            PlaylistRouter.addPlaylist(ctx, user, playlist);
        } catch (Exception ignored) {
            ctx.status(400).json(INVALID_ARGUMENTS());
        }
    }

    /**
     * Imports a playlist from a 3rd party source.
     *
     * @param ctx The Javalin context.
     */
    static void importPlaylist(Context ctx) {
        try {
            // Get the user info.
            var user = AccountUtils.getUser(ctx);
            if (user == null) return;

            // Parse with the body data.
            var body = ctx.bodyAsClass(JsonObject.class);
            // Get the 3rd party source.
            var urlRaw = body.get("url");

            // Validate parameters.
            if (urlRaw == null || urlRaw.isJsonNull()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }
            var url = urlRaw.getAsString();

            // Identify the engine to use.
            var source = Source.identify(url);
            if (source == Source.UNKNOWN || source == Source.ALL) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Parse the playlist into a Laudiolin playlist.
            var node = Laudiolin.getNode();
            var playlist = switch (source) {
                default -> null;
                case YOUTUBE -> node.youtubePlaylist(url);
                case SPOTIFY -> SpotifyUtils.playlist(url);
            };

            // Check if the playlist is null.
            if (playlist == null) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            PlaylistRouter.addPlaylist(ctx, user, playlist);
        } catch (Exception ignored) {
            ctx.status(400).json(INVALID_ARGUMENTS());
        }
    }

    /**
     * Adds a playlist to the database.
     * This method is used to avoid code duplication.
     *
     * @param ctx The Javalin context.
     * @param user The user.
     * @param playlist The playlist.
     */
    private static void addPlaylist(Context ctx, User user, Playlist playlist) {
        // Generate the playlist data.
        playlist.setId(DatabaseUtils.uniqueId(Playlist.class));
        playlist.setOwner(user.getUserId());

        // Validate the playlist data.
        if (!Playlist.valid(playlist)) {
            ctx.status(400).json(INVALID_ARGUMENTS());
            return;
        }

        // Save the playlist.
        playlist.save();
        // Add the playlist to the user.
        user.getPlaylists().add(playlist);
        user.save();

        // Return the playlist.
        ctx.status(201).json(playlist);
    }
}
