package moe.seikimo.laudiolin.utils;

import com.google.gson.JsonObject;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.models.data.Playlist;
import moe.seikimo.laudiolin.models.data.TrackData;
import org.jetbrains.annotations.NotNull;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public interface SpotifyUtils {
    Map<String, Track> ISRC_CACHE = new ConcurrentHashMap<>();
    Map<TrackData, String> TRACK_CACHE = new ConcurrentHashMap<>();

    SpotifyApi SPOTIFY = new SpotifyApi.Builder()
            .setClientId(Config.get().spotify.getClientId())
            .setClientSecret(Config.get().spotify.getClientSecret())
            .build();

    /**
     * Initializes the Spotify API.
     */
    static void initialize() {
        new Timer().scheduleAtFixedRate(new AuthorizeTask(), 0,
                TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES));
    }

    /**
     * Authorizes with the Spotify API.
     */
    static void authorize() {
        try {
            var credRequest = new ClientCredentialsRequest.Builder(
                    SPOTIFY.getClientId(),
                    SPOTIFY.getClientSecret()
            ).grant_type("client_credentials").build();

            // Apply the access token.
            var credentials = credRequest.execute();
            SPOTIFY.setAccessToken(credentials.getAccessToken());
        } catch (Exception exception) {
            Laudiolin.getLogger().warn("Failed to authorize with Spotify API.", exception);
        }
    }

    /**
     * Searches for a Spotify song.
     *
     * @param query The query to search for.
     * @return The search result.
     */
    static JsonObject search(String query) {
        try {
            // Fetch the tracks from the Spotify API.
            var response = SPOTIFY.searchTracks(query)
                    .build().execute();

            // Parse each individual track, up to 8 + 1 (top).
            var tracks = response.getItems();
            var results = Arrays.stream(tracks)
                    .map(SpotifyUtils::toTrackData)
                    .limit(9)
                    .toList();

            // Return the results.
            return TrackData.toResults(results);
        } catch (Exception exception) {
            Laudiolin.getLogger().warn("Failed to search Spotify for " + query + ".", exception);
            return null;
        }
    }

    /**
     * Searches a Spotify ID for a track.
     *
     * @param spotifyId The Spotify ID to search for.
     * @return The search result.
     */
    static Track searchSpotifyId(String spotifyId) {
        try {
            return SPOTIFY.getTrack(spotifyId).build().execute();
        } catch (Exception exception) {
            Laudiolin.getLogger().warn("Failed to search Spotify ID " + spotifyId + ".", exception);
            return null;
        }
    }

    /**
     * Searches an ISRC for a Spotify track.
     *
     * @param isrc The ISRC to search for.
     * @return The search result.
     */
    @Nullable
    static Track searchIsrc(String isrc) {
        var request = SPOTIFY.searchTracks("isrc:" + isrc)
                .limit(1).build();
        try {
            var response = request.execute();
            if (response.getTotal() < 1) {
                throw new RuntimeException("No results found.");
            }

            return response.getItems()[0];
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Searches a query for a Spotify track.
     *
     * @param isrc The ISRC to search for.
     * @return The resulting song's Spotify ID.
     */
    static String getSpotifyId(String isrc) {
        var track = SpotifyUtils.searchIsrc(isrc);
        if (track == null) {
            return null;
        }

        return track.getId();
    }

    /**
     * Converts the metadata of a Spotify track into a {@link TrackData} object.
     *
     * @param track The track to convert.
     * @return The converted track.
     */
    @NotNull
    static TrackData toTrackData(Track track) {
        // Combine the artists together.
        var artists = new StringBuilder();
        for (var artist : track.getArtists()) {
            artists.append(artist.getName()).append(", ");
        }
        if (!artists.isEmpty()) {
            artists.delete(artists.length() - 2, artists.length());
        }

        return new TrackData()
                .setTitle(track.getName())
                .setArtist(artists.toString())
                .setIcon(track.getAlbum().getImages()[0].getUrl())
                .setId(track.getExternalIds().getExternalIds().get("isrc"))
                .setDuration((int) Math.floor(track.getDurationMs() / 1000f))
                .setUrl(track.getExternalUrls().get("spotify"));
    }

    /**
     * Converts a Spotify ISRC/ID to a YouTube video/ID.
     *
     * @param id The Spotify ISRC/ID to convert.
     * @return The YouTube video/ID.
     */
    static String toYouTubeId(String id) {
        var node = Laudiolin.getNode();

        // Get the track by ID.
        var track = ISRC_CACHE.get(id);
        if (track == null) {
            track = ISRC_CACHE.computeIfAbsent(id, k -> k.length() == 12 ?
                    SpotifyUtils.searchIsrc(id) :
                    SpotifyUtils.searchSpotifyId(id));
        }
        if (track == null) return "";
        var trackData = SpotifyUtils.toTrackData(track);

        var youtubeId = TRACK_CACHE.get(trackData);
        if (youtubeId != null) return youtubeId;

        // Prepare a YouTube query.
        var query = String.format("%s - %s - Topic",
                trackData.getTitle(), trackData.getArtist());
        // Perform a YouTube search.
        var search = node.youtubeSearch(query, false);
        if (search.isEmpty()) return "";

        return TRACK_CACHE.computeIfAbsent(
                trackData, k -> search.get(0).getId());
    }

    /**
     * Converts a Spotify playlist into a Laudiolin playlist.
     *
     * @param url The URL of the playlist.
     * @return The converted playlist.
     */
    static Playlist playlist(String url) {
        try {
            // Extract the ID from the URL.
            var id = url.split("playlist/")[1];
            // Fetch the Spotify playlist.
            var playlist = SPOTIFY.getPlaylist(id)
                    .build().execute();
            var tracks = playlist.getTracks();

            // Convert the playlist.
            var converted = new Playlist()
                    .setName(playlist.getName())
                    .setDescription(playlist.getDescription())
                    .setIcon(playlist.getImages()[0].getUrl())
                    .setPrivate(!playlist.getIsPublicAccess());
            int offset = 0, limit = tracks.getTotal();

            var items = tracks.getItems();
            while (true) {
                for (var item : items) {
                    if (item.getIsLocal()) continue;
                    var rawTrack = item.getTrack();
                    if (!(rawTrack instanceof Track track)) continue;

                    // Parse the track.
                    var parsed = SpotifyUtils.toTrackData(track);
                    converted.getTracks().add(parsed);
                }

                // Check if there are more tracks.
                if (offset < limit) {
                    offset += items.length;
                    items = SPOTIFY.getPlaylistsItems(id)
                            .offset(offset).build().execute()
                            .getItems();
                } else break;
            }

            return converted;
        } catch (Exception ignored) {
            return null;
        }
    }

    final class AuthorizeTask extends TimerTask {
        @Override
        public void run() {
            SpotifyUtils.authorize();
        }
    }
}
