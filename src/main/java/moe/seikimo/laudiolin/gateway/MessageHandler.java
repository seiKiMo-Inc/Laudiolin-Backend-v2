package moe.seikimo.laudiolin.gateway;

import com.google.gson.JsonObject;
import moe.seikimo.laudiolin.models.InitializeMessage;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.DiscordPresence;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.AccountUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;

import java.util.concurrent.CopyOnWriteArrayList;

import static moe.seikimo.laudiolin.gateway.Gateway.GATEWAY_INVALID_TOKEN;

public interface MessageHandler {
    /**
     * Event method.
     * Fires when the client sends a message.
     * This message is JSON-encoded, and should be decoded by the handler.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    void handle(GatewaySession session, JsonObject message);

    /**
     * Initializes the client's session.
     * Handles authentication.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void initialize(GatewaySession session, JsonObject message) {
        if (session.isInitialized()) return; // Already initialized.

        // Extract message information.
        var data = EncodingUtils.jsonDecode(
                message, InitializeMessage.class);

        // Fetch the user by token.
        var user = AccountUtils.getUser(data.getToken());
        if (user == null) {
            // Invalid token.
            session.sendMessage(GATEWAY_INVALID_TOKEN());
            session.disconnect();
            return;
        }

        // Set the session's data.
        session.setUser(user);
        session.setBroadcastStatus(data.getBroadcast());
        session.setBroadcastPresence(data.getPresence());

        // TODO: Remove user from offline list.
        // TODO: Add user to online list.

        // Check if the user has a presence token.
        if (user.getPresenceToken() != null) {
            // Clear the existing presence.
            DiscordPresence.apply(user, null);
        }

        // Add the user to the connected users list.
        var users = Gateway.getUsers();
        if (!users.containsKey(user.getUserId())) {
            users.put(user.getUserId(), new CopyOnWriteArrayList<>());
        }
        users.get(user.getUserId()).add(session);

        session.setInitialized(true); // Mark the client as initialized.
        session.pingClient(); // Ping the client.
    }

    /**
     * Handles the client -> server gateway ping.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void latency(GatewaySession session, JsonObject message) {
        session.setLastPing(message.get("timestamp").getAsLong());
    }

    /**
     * Handles the client seek event.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void seek(GatewaySession session, JsonObject message) {
        session.setTrackPosition(message.get("seek").getAsFloat());
    }

    /**
     * Handles the request for the client to listen along.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void listen(GatewaySession session, JsonObject message) {
        // Get the target user.
        var targetUser = message.get("with");
        if (targetUser == null || targetUser.isJsonNull()) {
            // Stop listening.
            session.stopListening(false);
        } else {
            // Fetch the connected user.
            var targetUserId = targetUser.getAsString();
            var targetSession = Gateway.getConnectedUser(targetUserId);
            if (targetSession == null) return;

            // Listen along with the user.
            session.listenWith(targetSession);
        }
    }

    /**
     * Handles the client track update event.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void player(GatewaySession session, JsonObject message) {
        // Get the message data.
        var rawTrack = message.get("track");
        var track = rawTrack == null ? null :
                EncodingUtils.jsonDecode(rawTrack, TrackData.class);
        var seek = message.get("seek").getAsFloat();
        var paused = message.get("paused").getAsBoolean();

        // Check if the client is listening to a different track.
        var currentTrack = session.getTrackData();
        if (
                currentTrack == null
                || (track != null && !currentTrack.equals(track))
        ) {
            session.setStartedListening(System.currentTimeMillis());

            // Add the track to the user's recently played.
            var user = session.getUser();
            user.getRecentlyPlayed().add(track);
            user.save();
        }

        // Update the user's player information.
        session.setStartedListening(track != null ?
                System.currentTimeMillis() : null);
        session.setTrackData(track);
        session.setTrackPosition(seek);
        session.setPaused(paused);

        // Update the user's rich presence.
        session.updatePresence();
        // Update the listeners of the user.
        session.updateListeners();
        // update the user's online status.
        session.updateOnlineStatus();
    }

    /**
     * Handles the client volume change event.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void volume(GatewaySession session, JsonObject message) {
        session.setVolume(message.get("volume").getAsInt());

        // Send the volume back to the client.
        session.sendMessage(JObject.c()
                .add("type", "volume")
                .add("volume", session.getVolume()));
    }
}