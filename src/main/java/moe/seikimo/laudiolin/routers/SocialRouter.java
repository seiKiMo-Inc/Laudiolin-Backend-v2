package moe.seikimo.laudiolin.routers;

import io.javalin.Javalin;
import io.javalin.http.Context;
import moe.seikimo.laudiolin.utils.TrackUtils;
import moe.seikimo.laudiolin.gateway.Gateway;
import moe.seikimo.laudiolin.models.MinifiedPresence;
import moe.seikimo.laudiolin.models.OfflineUser;
import moe.seikimo.laudiolin.models.OnlineUser;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.DiscordPresence;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.objects.user.SocialStatus;
import moe.seikimo.laudiolin.utils.AccountUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;

import java.util.ArrayList;
import java.util.List;

import static moe.seikimo.laudiolin.utils.HttpUtils.INTERNAL_ERROR;
import static moe.seikimo.laudiolin.utils.HttpUtils.SUCCESS;

public interface SocialRouter {
    /**
     * Configures the Javalin router.
     *
     * @param javalin The Javalin instance.
     */
    static void configure(Javalin javalin) {
        javalin.get("/social/available", SocialRouter::getAvailable);
        javalin.get("/social/recent", SocialRouter::getOffline);
        javalin.post("/social/presence", SocialRouter::updatePresence);
    }

    /**
     * Fetches all currently available users.
     *
     * @param ctx The Javalin context.
     */
    static void getAvailable(Context ctx) {
        // Pull arguments.
        var token = ctx.header("authorization");
        var activeStr = ctx.queryParam("active");
        var active = activeStr != null && activeStr.equals("true");

        // Get all online users.
        var allUsers = Gateway.getOnlineUsers().values();
        List<OnlineUser> users = new ArrayList<>(allUsers);
        // Check if the request wants to filter out inactive users.
        if (active) {
            users = users.stream()
                    .filter(user -> user.getListeningTo() != null)
                    .toList();
        }

        // Filter out users which are not public.
        users = new ArrayList<>(users.stream()
                .filter(user -> user.getSocialStatus() == SocialStatus.EVERYONE)
                .toList());
        // Check if friends should be considered.
        if (token != null && !token.isEmpty()) {
            // Get the user's friends.
            var friends = AccountUtils.friends(token);
            if (friends != null) {
                var idList = friends.asList().stream()
                        .map(element -> element.getAsJsonObject()
                                .get("id").getAsString())
                        .toList();
                users.addAll(allUsers.stream()
                        .filter(user -> idList.contains(user.getUserId()))
                        .toList());
            }
        }

        // Send the users.
        ctx.status(200).json(SUCCESS(JObject.c()
                .add("onlineUsers", users)
                .gson()));
    }

    /**
     * Fetches all offline users.
     *
     * @param ctx The Javalin context.
     */
    static void getOffline(Context ctx) {
        // Pull arguments.
        var token = ctx.header("authorization");

        // Get the offline users.
        var allUsers = Gateway.getOfflineUsers().values();
        List<OfflineUser> users = new ArrayList<>(allUsers);

        // Filter out users which are not public.
        users = new ArrayList<>(users.stream()
                .filter(user -> user.getSocialStatus() == SocialStatus.EVERYONE)
                .toList());
        // Check if friends should be considered.
        if (token != null && !token.isEmpty()) {
            // Get the user's friends.
            var friends = AccountUtils.friends(token);
            if (friends != null) {
                var idList = friends.asList().stream()
                        .map(element -> element.getAsJsonObject()
                                .get("id").getAsString())
                        .toList();
                users.addAll(allUsers.stream()
                        .filter(user -> user.getSocialStatus() == SocialStatus.FRIENDS)
                        .filter(user -> idList.contains(user.getUserId()))
                        .toList());
            }
        }

        // Send the users.
        ctx.status(200).json(SUCCESS(JObject.c()
                .add("recentUsers", users)
                .gson()));
    }

    /**
     * Updates the user's Discord rich presence. (if applicable)
     *
     * @param ctx The Javalin context.
     */
    static void updatePresence(Context ctx) {
        try {
            // Get the user.
            var user = AccountUtils.getUser(ctx);
            if (user == null) return;

            // Parse the minified presence from the body.
            var presence = ctx.bodyAsClass(MinifiedPresence.class);
            if (presence.isRemove()) {
                DiscordPresence.apply(user, null);
                ctx.status(201);
            } else {
                var trackData = presence.getTrack();
                if (!TrackData.valid(trackData)) throw new Exception("Invalid track data.");

                // Look up the track's ID.
                var result = TrackUtils.lookup(trackData.getId());
                if (result == null) throw new Exception("Unable to look up track.");

                // Update the user's presence.
                var presenceObject = TrackUtils.fromTrack(
                        result, user, presence.getBroadcast(), presence.getStarted(), presence.getShouldEnd()
                );
                DiscordPresence.apply(user, presenceObject);

                ctx.status(200).json(SUCCESS(
                        EncodingUtils.toJson(presenceObject).getAsJsonObject()
                ));
            }
        } catch (Exception exception) {
            ctx.status(500).json(INTERNAL_ERROR(exception.getMessage()));
        }
    }
}
