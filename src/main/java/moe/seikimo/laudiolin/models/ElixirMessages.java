package moe.seikimo.laudiolin.models;

import lombok.Getter;
import moe.seikimo.laudiolin.models.data.TrackData;

@SuppressWarnings("unused")
public interface ElixirMessages {
    @Getter
    final class Playing {
        // This message is client -> server.
        private TrackData track;
    }

    @Getter
    final class Paused {
        // This message is client -> server.
        private boolean pause;
    }

    @Getter
    final class Loop {
        // This message is both ways.
        private int loopMode;
    }
}
