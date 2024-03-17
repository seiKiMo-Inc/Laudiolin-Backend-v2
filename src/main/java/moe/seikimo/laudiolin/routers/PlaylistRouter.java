package moe.seikimo.laudiolin.routers;

import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.models.data.Playlist;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.AccountUtils;
import moe.seikimo.laudiolin.utils.DatabaseUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import moe.seikimo.laudiolin.utils.SpotifyUtils;

import java.util.ArrayList;
import java.util.Objects;

import static moe.seikimo.laudiolin.utils.HttpUtils.*;

public interface PlaylistRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.post("/playlist/create", PlaylistRouter::createPlaylist);
        javalin.patch("/playlist/import", PlaylistRouter::importPlaylist);
        javalin.get("/playlist/{id}", PlaylistRouter::fetchPlaylist);
        javalin.patch("/playlist/{id}", PlaylistRouter::updatePlaylist);
        javalin.delete("/playlist/{id}", PlaylistRouter::deletePlaylist);
        javalin.post("/playlist/{id}/icon", PlaylistRouter::updatePlaylistIcon);
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
            ctx.status(400).json(INVALID_ARGUMENTS("Body is not JSON."));
            return;
        }

        try {
            // Try to parse the body data.
            var playlist = ctx.bodyAsClass(Playlist.class);
            if (playlist == null) throw new IllegalArgumentException("Invalid body data.");

            PlaylistRouter.addPlaylist(ctx, user, playlist);
        } catch (Exception exception) {
            Laudiolin.getLogger().error("Unable to save playlist.", exception);
            ctx.status(500).json(INTERNAL_ERROR());
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
                ctx.status(400).json(INVALID_ARGUMENTS("No URL provided."));
                return;
            }
            var url = urlRaw.getAsString();

            // Identify the engine to use.
            var source = Source.identify(url);
            if (source == Source.UNKNOWN || source == Source.ALL) {
                ctx.status(400).json(INVALID_ARGUMENTS("Source is invalid."));
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
                ctx.status(400).json(INVALID_ARGUMENTS("Playlist data is invalid."));
                return;
            }

            PlaylistRouter.addPlaylist(ctx, user, playlist);
        } catch (Exception exception) {
            Laudiolin.getLogger().error("Unable to save playlist.", exception);
            ctx.status(500).json(INTERNAL_ERROR());
        }
    }

    /**
     * Fetches the playlist data.
     *
     * @param ctx The Javalin context.
     */
    static void fetchPlaylist(Context ctx) {
        try {
            // Pull arguments.
            var id = ctx.pathParam("id");

            // Validate arguments.
            if (id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS("No ID provided."));
                return;
            }

            // Fetch the playlist from the database.
            var playlist = Playlist.getPlaylistById(id);
            if (playlist == null) {
                ctx.status(404).json(NO_RESULTS());
                return;
            }

            // Check if the user can view the playlist.
            if (playlist.isPrivate()) {
                // Check for authorization.
                var user = AccountUtils.getUser(ctx);
                if (user == null) return;

                // Check if the user is the owner.
                if (!Objects.equals(user.getUserId(), playlist.getOwner())) {
                    ctx.status(404).json(NO_RESULTS());
                    return;
                }
            }

            // Return the playlist.
            ctx.status(301).json(playlist);
        } catch (Exception exception) {
            Laudiolin.getLogger().error("Unable to save playlist.", exception);
            ctx.status(500).json(INTERNAL_ERROR());
        }
    }

    /**
     * Updates a playlist.
     *
     * @param ctx The Javalin context.
     */
    static void updatePlaylist(Context ctx) {
        try {
            // Get the user info.
            var user = AccountUtils.getUser(ctx);
            if (user == null) return;

            // Pull parameters.
            var id = ctx.pathParam("id");
            var type = ctx.queryParam("type");

            // Validate parameters.
            if (id.isEmpty() || type == null || type.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS("ID or action is invalid."));
                return;
            }

            // Get the playlist from the database.
            var playlist = Playlist.getPlaylistById(id);
            if (playlist == null) {
                ctx.status(404).json(NO_RESULTS());
                return;
            }

            // Check if the user is the owner.
            if (!Objects.equals(user.getUserId(), playlist.getOwner())) {
                ctx.status(403).json(NO_AUTHORIZATION());
                return;
            }

            // Get the body data.
            var body = ctx.bodyAsClass(JsonObject.class);
            switch (type) {
                default -> {
                    ctx.status(400).json(INVALID_ARGUMENTS("Invalid action provided."));
                    return;
                }
                case "rename" -> {
                    // Validate the body.
                    var nameRaw = body.get("name");
                    if (nameRaw == null || nameRaw.isJsonNull()) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Name is invalid."));
                        return;
                    }

                    // Update the playlist.
                    playlist.setName(nameRaw.getAsString());
                }
                case "describe" -> {
                    // Validate the body.
                    var descriptionRaw = body.get("description");
                    if (descriptionRaw == null || descriptionRaw.isJsonNull()) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Description is invalid."));
                        return;
                    }

                    // Update the playlist.
                    playlist.setDescription(descriptionRaw.getAsString());
                }
                case "icon" -> {
                    // Validate the body.
                    var iconRaw = body.get("icon");
                    if (iconRaw == null || iconRaw.isJsonNull()) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Icon is invalid."));
                        return;
                    }

                    // Update the playlist.
                    playlist.setIcon(iconRaw.getAsString());
                }
                case "privacy" -> {
                    // Validate the body.
                    var privacyRaw = body.get("privacy");
                    if (privacyRaw == null || privacyRaw.isJsonNull()) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Privacy is invalid"));
                        return;
                    }

                    // Update the playlist.
                    playlist.setPrivate(privacyRaw.getAsBoolean());
                }
                case "add" -> {
                    // Parse the body into a TrackData object.
                    var trackData = ctx.bodyAsClass(TrackData.class);
                    if (trackData == null) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Track data failed to parse."));
                        return;
                    }
                    if (!TrackData.valid(trackData)) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Track data is invalid."));
                        return;
                    }

                    // Add the song to the playlist.
                    playlist.getTracks().add(trackData);
                }
                case "remove" -> {
                    // Validate the body.
                    var indexRaw = body.get("index");
                    if (indexRaw == null || indexRaw.isJsonNull()) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Index is invalid."));
                        return;
                    }

                    // Remove the song from the playlist.
                    playlist.getTracks().remove(indexRaw.getAsInt());
                }
                case "bulk" -> {
                    // Validate all the tracks.
                    var tracks = new ArrayList<TrackData>();
                    var tracksRaw = body.get("tracks");
                    if (tracksRaw == null || !tracksRaw.isJsonArray()) {
                        ctx.status(400).json(INVALID_ARGUMENTS("Tracks are invalid."));
                        return;
                    }

                    for (var track : tracksRaw.getAsJsonArray()) {
                        // Transmute the track into TrackData.
                        var track1 = track.getAsJsonObject();
                        var trackData = EncodingUtils.jsonDecode(
                                EncodingUtils.jsonEncode(
                                        EncodingUtils.toJson(track1)),
                                TrackData.class
                        );
                        if (!TrackData.valid(trackData)) {
                            ctx.status(400).json(INVALID_ARGUMENTS("A provided track was invalid."));
                            return;
                        }
                        tracks.add(trackData);
                    }

                    // Change the playlist data.
                    playlist.setTracks(tracks);
                    playlist.setPrivate(body.get("private") == null ?
                            playlist.isPrivate() : body.get("private").getAsBoolean());
                    playlist.setName(body.get("name") == null ?
                            playlist.getName() : body.get("name").getAsString());
                    playlist.setDescription(body.get("description") == null ?
                            playlist.getDescription() : body.get("description").getAsString());
                    playlist.setIcon(body.get("icon") == null ?
                            playlist.getIcon() : body.get("icon").getAsString());
                }
            }

            // Validate the playlist data.
            if (!Playlist.valid(playlist)) {
                ctx.status(400).json(INVALID_ARGUMENTS("Playlist is not valid."));
                return;
            }

            // Save the playlist.
            playlist.save();
            // Return the playlist.
            ctx.status(200).json(playlist);
        } catch (IllegalArgumentException invalid) {
            ctx.status(400).json(INVALID_ARGUMENTS(invalid.getMessage()));
        } catch (Exception exception) {
            Laudiolin.getLogger().error("Unable to save playlist.", exception);
            ctx.status(500).json(INTERNAL_ERROR());
        }
    }

    /**
     * Deletes a playlist.
     *
     * @param ctx The Javalin context.
     */
    static void deletePlaylist(Context ctx) {
        try {
            // Get the user.
            var user = AccountUtils.getUser(ctx);
            if (user == null) return;

            // Pull parameters.
            var id = ctx.pathParam("id");

            // Validate parameters.
            if (id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS("No ID provided."));
                return;
            }

            // Get the playlist from the database.
            var playlist = Playlist.getPlaylistById(id);
            if (playlist == null) {
                ctx.status(404).json(NO_RESULTS());
                return;
            }

            // Check if the user is the owner.
            if (!Objects.equals(user.getUserId(), playlist.getOwner())) {
                ctx.status(403).json(NO_AUTHORIZATION());
                return;
            }

            // Delete the playlist.
            playlist.delete();
            // Remove the playlist from the user.
            user.getPlaylists().remove(playlist);
            user.save();

            // Return the playlist.
            ctx.status(200).json(SUCCESS());
        } catch (Exception exception) {
            Laudiolin.getLogger().error("Unable to save playlist.", exception);
            ctx.status(500).json(INTERNAL_ERROR());
        }
    }

    /**
     * Changes the playlist's icon.
     *
     * @param ctx The Javalin context.
     */
    private static void updatePlaylistIcon(Context ctx) {
        try {
            // Get the user.
            var user = AccountUtils.getUser(ctx);
            if (user == null) return;

            // Pull parameters.
            var id = ctx.pathParam("id");

            // Validate parameters.
            if (id.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS("No ID provided."));
                return;
            }

            // Get the playlist from the database.
            var playlist = Playlist.getPlaylistById(id);
            if (playlist == null) {
                ctx.status(404).json(NO_RESULTS());
                return;
            }

            // Check if the user is the owner.
            if (!Objects.equals(user.getUserId(), playlist.getOwner())) {
                ctx.status(403).json(NO_AUTHORIZATION());
                return;
            }

            // Pull the icon from the body.
            var icon = ctx.body();
            if (icon.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS("No icon provided."));
                return;
            }

            // Update the playlist's icon.
            playlist.setIconRaw(icon);

            // Return the playlist's icon URL.
            ctx.status(200).json(JObject.c()
                    .add("url", playlist.getIcon())
                    .gson());
        } catch (Exception exception) {
            ctx.status(500).json(INTERNAL_ERROR(exception.getMessage()));
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
            ctx.status(400).json(INVALID_ARGUMENTS("Playlist is invalid."));
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
