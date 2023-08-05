package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.utils.AccountUtils;

import static moe.seikimo.laudiolin.utils.HttpUtils.INVALID_ARGUMENTS;
import static moe.seikimo.laudiolin.utils.HttpUtils.NO_RESULTS;

public interface UserRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/user", UserRouter::fetchUser);
        javalin.post("/user/favorite", UserRouter::favorite);
        javalin.get("/user/{id}", UserRouter::fetchUser);
    }

    /**
     * Fetches a user by ID or the current user.
     *
     * @param ctx The context.
     */
    static void fetchUser(Context ctx) {
        // Fetch either the current user or the user by ID.
        var token = ctx.header("authorization");
        var userId = ctx.pathParamMap().get("id");

        // Check if a parameter has been filled.
        if (token == null && userId == null) {
            ctx.status(400).json(INVALID_ARGUMENTS());
            return;
        }

        // Get the user from the database.
        var authorizedUser = AccountUtils.getUser(token);
        var requestedUser = User.getUserById(userId);
        if (authorizedUser == null && requestedUser == null) {
            ctx.status(404).json(NO_RESULTS());
            return;
        }

        // Return the user.
        var user = requestedUser != null ?
                requestedUser : authorizedUser;
        ctx.status(301).json(user.explain(
                token != null && authorizedUser != null &&
                        userId != null && requestedUser != null &&
                        requestedUser.getUserId().equals(authorizedUser.getUserId())
        ));
    }

    /**
     * Modifies the user's favorite songs.
     *
     * @param ctx The context.
     */
    static void favorite(Context ctx) {
        try {
            // Get the user's information.
            var user = AccountUtils.getUser(ctx);
            if (user == null) return;

            // Get the operation.
            var operation = ctx.header("Operation");
            if (operation == null || operation.isEmpty()) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }
            if (!operation.equals("add") && !operation.equals("remove")) {
                ctx.status(400).json(INVALID_ARGUMENTS());
                return;
            }

            // Perform the operation on the user's favorites.
            var favorites = user.getLikedSongs();
            var track = ctx.bodyAsClass(TrackData.class);
            if (operation.equals("add")) {
                // Check if the track is already in the favorites.
                if (favorites.contains(track)) {
                    ctx.status(400).json(INVALID_ARGUMENTS());
                    return;
                }

                // Add the track to the favorites.
                favorites.add(track);
            } else {
                // Remove the track from the favorites.
                favorites.remove(track);
            }

            // Update the user's favorites.
            user.setLikedSongs(favorites);
            user.save();

            // Send the list of favorites.
            ctx.status(200).json(favorites);
        } catch (Exception ignored) {
            ctx.status(400).json(INVALID_ARGUMENTS());
        }
    }
}
