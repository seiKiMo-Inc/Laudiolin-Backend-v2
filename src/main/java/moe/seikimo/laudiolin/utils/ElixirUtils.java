package moe.seikimo.laudiolin.utils;

import moe.seikimo.laudiolin.gateway.GatewaySession;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.JObject;

public interface ElixirUtils {
    /**
     * Plays a track on the client.
     *
     * @param elixir The Elixir session to send the message to.
     * @param track The track data.
     */
    static void playTrack(GatewaySession elixir, TrackData track) {
        elixir.sendMessage(JObject.c()
                .add("type", "playTrack")
                .add("track", track));
    }
}
