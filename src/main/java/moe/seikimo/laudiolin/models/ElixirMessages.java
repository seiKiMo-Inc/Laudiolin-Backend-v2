package moe.seikimo.laudiolin.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import moe.seikimo.laudiolin.models.data.TrackData;

@SuppressWarnings("unused")
public interface ElixirMessages {
    @AllArgsConstructor
    final class PlayTrack {
        // This message is server -> client.
        private final TrackData track;
    }

    @Getter
    final class ChannelCheck {
        // This message is client -> server.
        private String userId;
    }
}
