package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import lombok.Data;
import lombok.experimental.Accessors;
import moe.seikimo.laudiolin.Messages;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.Assertions;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@Entity
@Accessors(chain = true)
public class TrackData {
    @NotNull private String id;
    @NotNull private String title;
    @NotNull private String artist;
    @NotNull private String icon, url;
    private int duration;

    public TrackData() {
        // Empty constructor for Morphia.
    }

    @Override
    public String toString() {
        return "%s - %s (%s) of duration %s seconds".formatted(
                this.title, this.artist, this.id, this.duration
        );
    }

    /**
     * Converts a {@link Document} to a {@link TrackData}.
     *
     * @param track The track to convert.
     * @return The converted track.
     */
    public static TrackData toTrack(Document track) {
        return new TrackData()
                .setId(track.getString("id"))
                .setTitle(track.getString("title"))
                .setArtist(track.getString("artist"))
                .setIcon(track.getString("icon"))
                .setUrl(track.getString("url"))
                .setDuration(track.getInteger("duration"));
    }

    /**
     * Converts a {@link Messages.Track} to a {@link TrackData}.
     *
     * @param result The result to convert.
     * @return The converted result.
     */
    public static TrackData toTrack(Messages.Track result) {
        // Parse the artists.
        var artistList = result.getArtistsList();
        var artists = new StringBuilder();
        for (var artist : artistList) {
            artists.append(artist).append(", ");
        }
        if (!artistList.isEmpty()) {
            artists.delete(artists.length() - 2, artists.length());
        }

        return new TrackData()
                .setId(result.getId())
                .setTitle(result.getTitle())
                .setArtist(artists.toString())
                .setIcon(result.getIcon())
                .setUrl(result.getUrl())
                .setDuration(result.getDuration());
    }

    /**
     * Provides a {@link JsonObject} representation of the search results.
     *
     * @param results The results to convert.
     * @return The results as a {@link JsonObject}.
     */
    public static JsonObject toResults(List<TrackData> results) {
        return TrackData.toResults(
                results.get(0),
                results.subList(1, results.size())
        );
    }

    /**
     * Provides a {@link JsonObject} representation of the search results.
     *
     * @param top The top search result.
     * @param other The other search results.
     * @return The results as a {@link JsonObject}.
     */
    public static JsonObject toResults(TrackData top, List<TrackData> other) {
        return JObject.c()
                .add("top", top)
                .add("results", other)
                .gson();
    }

    /**
     * Validates a {@link TrackData} object.
     *
     * @param track The track to validate.
     */
    public static boolean valid(TrackData track) {
        Assertions.check(track != null, "Track cannot be null.");

        // Set the artist.
        if (track.getArtist().isEmpty()) {
            track.setArtist("Unknown");
        }

        Assertions.check(!track.getId().isEmpty(), "Track ID cannot be empty.");
        Assertions.check(!track.getTitle().isEmpty(), "Track title cannot be empty.");
        Assertions.check(!track.getIcon().isEmpty(), "Track icon cannot be empty.");
        Assertions.check(!track.getUrl().isEmpty(), "Track URL cannot be empty.");
        Assertions.check(track.getDuration() > 0, "Track duration must be positive.");
        Assertions.check(EncodingUtils.isValidUrl(track.getUrl()), "Track URL is invalid.");
        Assertions.check(EncodingUtils.isValidUrl(track.getIcon()), "Track icon is invalid.");

        return true;
    }
}
