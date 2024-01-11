package moe.seikimo.laudiolin.utils;

import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.objects.DiscordPresence;
import moe.seikimo.laudiolin.objects.user.PresenceMode;

import java.util.List;

public interface TrackUtils {
    /**
     * Attempts to look up a track's data from its ID.
     *
     * @param trackId The track ID.
     * @return The track data.
     */
    static TrackData lookup(String trackId) {
        var source = Source.identify(null, trackId);
        if (source == Source.UNKNOWN) throw new IllegalArgumentException("Invalid track ID.");

        return switch (source) {
            case ALL, UNKNOWN -> throw new IllegalArgumentException("Invalid track ID.");
            case YOUTUBE -> TrackData.toTrack(Laudiolin.getNode().youtubeFetch(trackId));
            case SPOTIFY -> SpotifyUtils.toTrackData(SpotifyUtils.searchId(trackId));
        };
    }

    /**
     * Creates a Discord rich presence from a track data.
     *
     * @param track The track data.
     * @param user The user.
     * @param broadcastType The broadcast presence type.
     * @param started The time the track started.
     * @return The rich presence.
     */
    static DiscordPresence fromTrack(
            TrackData track, User user,
            PresenceMode broadcastType, long started, long shouldEnd
    ) {
        // Build the presence.
        var config = Config.get();
        var clientId = config.discord.getClientId();
        var webTarget = config.getWebTarget();

        var assets = DiscordPresence.Assets.builder()
                .largeImage(track.getIcon())
                .largeText(track.getTitle())
                .smallImage(config.discord.getLogoHash())
                .smallText("Laudiolin");
        var presence = DiscordPresence.builder()
                .platform(DiscordPresence.Platform.DESKTOP.getValue())
                .type(DiscordPresence.PresenceType.PLAYING.getValue())
                .id("laudiolin").name("Laudiolin")
                .applicationId(clientId)
                .details("Listening to " + track.getTitle())
                .state(track.getArtist())
                .timestamps(DiscordPresence.Timestamps.builder()
                        .start(started)
                        .end(shouldEnd)
                        .build())
                .buttons(List.of(
                        DiscordPresence.Button.builder()
                                .label("Play on Laudiolin")
                                .url(webTarget + "/track/" + track.getId())
                                .build(),
                        DiscordPresence.Button.builder()
                                .label("Listen Along")
                                .url(webTarget + "/listen/" + user.getUserId())
                                .build()
                ));

        // Check if the simple rich presence should be used.
        if (PresenceMode.IS_LISTENING.contains(broadcastType)) {
            presence.type(DiscordPresence.PresenceType.LISTENING.getValue())
                    .details(track.getTitle());

            if (broadcastType == PresenceMode.SPOTIFY ||
                    config.discord.isPresenceDetails()) {
                assets.largeText("Laudiolin");
                presence.id("spotify:1")
                        .name("Spotify")
                        .sessionId("4efa609dfa405bb70c0da334220d4a3f")
                        .party(DiscordPresence.Party.builder()
                                .id("spotify:852697865012117544")
                                .build())
                        .flags(48);
            }
        }

        // Set the presence.
        return presence
                .assets(assets.build())
                .build();
    }
}
