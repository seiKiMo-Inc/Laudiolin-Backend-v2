package moe.seikimo.laudiolin.models;

import lombok.Getter;
import moe.seikimo.laudiolin.models.data.TrackData;

import java.util.List;

@SuppressWarnings("unused")
public interface ElixirMessages {
    @Getter
    final class Playing {
        // This message is client -> server.
        private TrackData track;
    }

    @Getter
    final class Queue {
        // This message is client -> server.
        private List<TrackData> queue;
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

    @Getter
    final class Guilds {
        // This message is client -> server.
        // This is sent over HTTP.
        private String botId;
        private List<String> inGuilds;
    }
}
