package moe.seikimo.laudiolin.utils;

import moe.seikimo.laudiolin.gateway.GatewaySession;
import moe.seikimo.laudiolin.objects.JObject;

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
}
