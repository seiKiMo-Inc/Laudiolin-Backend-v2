package moe.seikimo.laudiolin.utils;

import com.google.gson.JsonObject;
import moe.seikimo.laudiolin.gateway.ElixirManager;
import moe.seikimo.laudiolin.gateway.GatewaySession;
import moe.seikimo.laudiolin.models.ElixirMessages;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.JObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ElixirUtils {
    /**
     * Plays a track on the client.
     *
     * @param elixir The Elixir session to send the message to.
     * @param data The track/playlist ID/URL.
     */
    static void playTrack(GatewaySession elixir, String data) {
        elixir.sendMessage(JObject.c()
                .add("type", "playTrack")
                .add("data", data));
    }

    /**
     * Resumes the audio player.
     *
     * @param elixir The Elixir session to send the message to.
     */
    static void resume(GatewaySession elixir) {
        elixir.sendMessage(JObject.c()
                .add("type", "resume"));
    }

    /**
     * Pauses the audio player.
     *
     * @param elixir The Elixir session to send the message to.
     */
    static void pause(GatewaySession elixir) {
        elixir.sendMessage(JObject.c()
                .add("type", "pause"));
    }

    /**
     * Sets the audio player's volume.
     *
     * @param elixir The Elixir session to send the message to.
     * @param volume The volume to set.
     */
    static void volume(GatewaySession elixir, int volume) {
        elixir.sendMessage(JObject.c()
                .add("type", "volume")
                .add("volume", volume));
    }

    /**
     * Shuffles the audio player queue.
     *
     * @param elixir The Elixir session to send the message to.
     */
    static void shuffle(GatewaySession elixir) {
        elixir.sendMessage(JObject.c()
                .add("type", "shuffle"));
    }

    /**
     * Skips to the specified track in the audio player queue.
     *
     * @param elixir The Elixir session to send the message to.
     * @param trackNumber The track's number of the track to skip to.
     */
    static void skip(GatewaySession elixir, int trackNumber) {
        elixir.sendMessage(JObject.c()
                .add("type", "skip")
                .add("track", trackNumber));
    }

    /**
     * Seeks to the specified position in the current track.
     *
     * @param elixir The Elixir session to send the message to.
     * @param position The position to seek to.
     */
    static void seek(GatewaySession elixir, long position) {
        elixir.sendMessage(JObject.c()
                .add("type", "seek")
                .add("position", position));
    }

    /**
     * Fetches the queue of the audio player.
     *
     * @param elixir The Elixir session to send the message to.
     * @return The queue.
     */
    static List<TrackData> queue(GatewaySession elixir) {
        // Prepare to receive the queue message.
        var promise = new CompletableFuture<JsonObject>();
        elixir.addListener("queue", promise::complete);

        // Request the queue.
        elixir.sendMessage(JObject.c()
                .add("type", "queue"));

        // Wait for the queue message.
        var queueRaw = promise.join();
        return EncodingUtils.jsonDecode(queueRaw,
                ElixirMessages.Queue.class).getQueue();
    }

    /**
     * Sets the loop mode of the audio player.
     *
     * @param elixir The Elixir session to send the message to.
     * @param loopMode The loop mode to set.
     */
    static void loop(GatewaySession elixir, int loopMode) {
        elixir.sendMessage(JObject.c()
                .add("type", "loop")
                .add("loopMode", loopMode));
        elixir.setLoopMode(loopMode);
    }

    /* -------------------------------------------------- USER LAND -------------------------------------------------- */

    /**
     * Sends the queue of the selected bot to all controllers.
     *
     * @param initiator The session that requested the queue.
     */
    static void broadcastQueue(GatewaySession initiator) {
        var queue = ElixirUtils.queue(initiator.getElixirSession());
        var message = JObject.c()
                .add("type", "queue")
                .add("queue", queue);
        ElixirManager.broadcastToAll(initiator, message);
    }
}
