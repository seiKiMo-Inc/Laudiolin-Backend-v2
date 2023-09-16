package moe.seikimo.laudiolin.gateway;

import com.google.gson.JsonObject;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.models.ElixirMessages;
import moe.seikimo.laudiolin.models.InitializeMessage;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.DiscordPresence;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.AccountUtils;
import moe.seikimo.laudiolin.utils.ElixirUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        // Check if the connection is an Elixir.
        if (Config.get().elixir.getToken()
                .equals(data.getToken())) {
            session.setInitialized(true); // Initialize the user.
            session.setBotId(data.getBotId()); // Set the bot ID.
            session.setGuildId(data.getGuildId()); // Set the guild ID.
            Gateway.addUser(data.getGuildId(), session); // Add the user to the connected users list.
            return;
        }

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

        // Clear the existing presence.
        DiscordPresence.apply(user, null);

        // Add the user to the connected users list.
        Gateway.addUser(user.getUserId(), session);

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
        var seek = message.get("seek").getAsFloat();
        session.updateSeek(seek);

        if (session.getUser() == null) {
            // Broadcast seek event to all clients.
            ElixirManager.broadcastToAll(session, JObject.c()
                    .add("type", "synchronize")
                    .add("position", seek));
        }
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
        var newTrack = track != null && !Objects.equals(currentTrack, track);
        if (newTrack) {
            session.setStartedListening(System.currentTimeMillis());

            // Add the track to the user's recently played.
            var user = session.getUser();
            var recents = user.getRecentlyPlayed();

            var mostRecent = recents.get(0);
            if (!mostRecent.equals(track)) {
                // Add the track to the user's recently played.
                if (recents.size() >= 10)
                    recents.remove(9);
                recents.add(0, track);

                // Remove any duplicates.
                List<TrackData> newList = new ArrayList<>();
                for (var recentTrack : recents) {
                    if (!newList.contains(recentTrack))
                        newList.add(recentTrack);
                }

                // Apply and save the changes.
                user.setRecentlyPlayed(newList);
                user.save();

                // Send a gateway message.
                session.sendMessage(JObject.c()
                        .add("type", "recents")
                        .add("recents", newList)
                        .add("timestamp", System.currentTimeMillis()));
            }
        }

        // Update the user's player information.
        session.setStartedListening(track != null ?
                System.currentTimeMillis() : null);
        session.setTrackData(track);
        session.setTrackPosition(seek);
        session.setPaused(paused);

        // Update the user's rich presence.
        session.updatePresence(paused || newTrack);
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

        var response = JObject.c()
                .add("type", "volume")
                .add("volume", session.getVolume());
        if (session.isUsingElixir()) {
            // Send the volume to all clients using Elixir.
            ElixirUtils.volume(session.getElixirSession(), session.getVolume());
            ElixirManager.broadcastToAll(session, response);
        } else {
            // Send the volume back to the client.
            session.sendMessage(response);
        }
    }

    /**
     * Handles the client's request to select an Elixir.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void setElixir(GatewaySession session, JsonObject message) {
        var guildIdRaw = message.get("guild");
        var botIdRaw = message.get("bot");

        if (guildIdRaw != null && !guildIdRaw.isJsonNull()) {
            // Fetch the guild.
            var guildId = guildIdRaw.getAsString();
            var guild = Gateway.getConnectedUser(guildId);
            if (guild == null) return;

            // Determine the bot ID, from the message or by the guild.
            var botId = botIdRaw != null && !botIdRaw.isJsonNull() ?
                    botIdRaw.getAsString() : guild.getBotId();
            if (botId.isEmpty()) botId = guild.getBotId();

            // Check if the user already has a session.
            if (session.getElixirSession() != null) {
                ElixirManager.removeControllingSession(session);
            }

            // Set the selected Elixir.
            session.setGuildId(guildId);
            session.setBotId(botId);
            session.setUsingElixir(true);
            session.setElixirSession(guild);

            ElixirManager.addControllingSession(session);
        } else {
            ElixirManager.removeControllingSession(session);

            // Unset the selected Elixir.
            session.setGuildId(null);
            session.setBotId(null);
            session.setUsingElixir(false);
            session.setElixirSession(null);
        }
    }

    /**
     * Handles the client's request to use an Elixir.
     *
     * @param session The session that sent the message.
     * @param message The message that was sent.
     */
    static void useElixir(GatewaySession session, JsonObject message) {
        // Check if the session is using an Elixir.
        if (!session.isUsingElixir()) return;
        var elixir = session.getElixirSession();

        switch (message.get("action").getAsString()) {
            default -> session.disconnect();
            case "shuffle" -> {
                ElixirUtils.shuffle(elixir);
                ElixirUtils.broadcastQueue(session);
            }
            case "skip" -> {
                var toTrack = message.get("track").getAsInt();
                ElixirUtils.skip(elixir, toTrack);
                ElixirUtils.broadcastQueue(session);
            }
        }
    }

    /* -------------------------------------------------- ELIXIR -------------------------------------------------- */

    /**
     * Handles the client's request to update the track state.
     *
     * @param session The session that sent the message.
     * @param raw The raw message that was sent.
     */
    static void playing(GatewaySession session, JsonObject raw) {
        // Check if the session is an Elixir.
        if (session.getUser() != null) return;

        // Apply the track data.
        var message = EncodingUtils.jsonDecode(
                raw, ElixirMessages.Playing.class);
        var track = message.getTrack();

        // Determine if the track is new.
        if (track == null) {
            session.setStartedListening(null);
        } else if (!track.equals(session.getTrackData())) {
            session.setStartedListening(System.currentTimeMillis());
        }

        session.setTrackData(message.getTrack());

        // Broadcast the synchronization.
        ElixirManager.broadcastToAll(session, JObject.c()
                .add("type", "synchronize")
                .add("playingTrack", message.getTrack()));
    }

    /**
     * Handles the client's request to update the track position.
     *
     * @param session The session that sent the message.
     * @param raw The raw message that was sent.
     */
    static void pause(GatewaySession session, JsonObject raw) {
        // Check if the session is an Elixir.
        if (session.getUser() != null) return;

        // Apply the track data.
        var message = EncodingUtils.jsonDecode(
                raw, ElixirMessages.Paused.class);
        session.setPaused(message.isPause());

        // Broadcast the synchronization.
        ElixirManager.broadcastToAll(session, JObject.c()
                .add("type", "synchronize")
                .add("paused", message.isPause()));
    }

    /**
     * Handles the client's request to update the track position.
     *
     * @param session The session that sent the message.
     * @param raw The raw message that was sent.
     */
    static void loop(GatewaySession session, JsonObject raw) {
        // Check if the session is an Elixir.
        if (session.getUser() != null) return;

        // Apply the track data.
        var message = EncodingUtils.jsonDecode(
                raw, ElixirMessages.Loop.class);
        session.setLoopMode(message.getLoopMode());

        // Broadcast the synchronization.
        ElixirManager.broadcastToAll(session, JObject.c()
                .add("type", "synchronize")
                .add("loopMode", message.getLoopMode()));
    }

    /**
     * Handles the client's request to update the queue.
     *
     * @param session The session that sent the message.
     * @param raw The raw message that was sent.
     */
    static void queue(GatewaySession session, JsonObject raw) {
        // Check if the session is an Elixir.
        if (session.getUser() != null) return;

        // Apply the track data.
        var message = EncodingUtils.jsonDecode(
                raw, ElixirMessages.Queue.class);

        // Broadcast the synchronization.
        ElixirManager.broadcastToAll(session, JObject.c()
                .add("type", "synchronize")
                .add("queue", message.getQueue()));
    }

    /**
     * Handles the client's request to update the whole player.
     *
     * @param session The session that sent the message.
     * @param raw The raw message that was sent.
     */
    static void synchronize(GatewaySession session, JsonObject raw) {
        if (session.getUser() != null) {
            session.getElixirSession().sendMessage(raw);
        } else {
            // Forward to all controlling users.
            ElixirManager.broadcastToAll(session, raw);
        }
    }
}
