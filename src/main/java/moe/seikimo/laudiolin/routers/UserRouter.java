package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.utils.AccountUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import moe.seikimo.laudiolin.utils.ResourceUtils;

import java.util.concurrent.atomic.AtomicReference;

import static moe.seikimo.laudiolin.utils.HttpUtils.*;

public interface UserRouter {
    AtomicReference<String> AUTHORIZE_SCRIPT = new AtomicReference<>();

    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        // Load the authorize script.
        var script = ResourceUtils.getResource("scripts/authorize.html");
        AUTHORIZE_SCRIPT.set(new String(script));

        javalin.get("/login", UserRouter::authorize);
        javalin.get("/user", UserRouter::fetchUser);
        javalin.post("/user/favorite", UserRouter::favorite);
        javalin.get("/user/{id}", UserRouter::fetchUser);
    }

    /**
     * Redirects the user to the login page.
     * Can also authorize the user.
     *
     * @param ctx The context.
     */
    static void authorize(Context ctx) {
        // Check if the handoff code is present.
        var handoff = ctx.queryParam("handoff");
        if (handoff == null || handoff.isEmpty()) {
            var config = Config.get();
            var appUrl = config.getAppTarget();
            var baseUrl = config.seikimo.getBaseUrl();

            // Redirect to the login page.
            ctx.redirect(baseUrl + "/login?redirect="
                    + appUrl + "/login&app=Laudiolin&handoff=true");
            return;
        }

        try {
            // Base64-decode the handoff code.
            var decoded = EncodingUtils.base64Decode(handoff);
            var data = EncodingUtils.jsonDecode(decoded);

            // Get the token from the data.
            var token = data.get("token").getAsString();
            // Attempt to get the account information.
            var accountInfo = AccountUtils.accountInfo(token);
            if (accountInfo == null) {
                ctx.status(400).json(INVALID_TOKEN());
                return;
            }

            // Fetch the account from the database.
            var userId = accountInfo.get("id").getAsString();
            var user = User.getUserById(userId);
            if (user == null) {
                // Create a new user.
                user = new User();
                user.setUserId(userId);

                // Check if the user has data to migrate.
                var discordId = accountInfo.get("discord");
                if (discordId != null && !discordId.isJsonNull()) {
                    User.migrate(user, discordId.getAsString());
                } else {
                    user.save();
                }
            }

            // Redirect the user to the app.
            ctx
                    .contentType(ContentType.TEXT_HTML)
                    .result(AUTHORIZE_SCRIPT.get()
                            .replace("[[TOKEN]]", token));
        } catch (Exception ignored) {
            ctx.status(400).json(INVALID_ARGUMENTS());
        }
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
        ctx.status(301).json(user.userInfo(
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
