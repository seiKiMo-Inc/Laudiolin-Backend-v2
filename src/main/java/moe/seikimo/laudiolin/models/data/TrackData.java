package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import moe.seikimo.laudiolin.Messages;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.EncodingUtils;
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
     * @return Whether the track is valid.
     */
    public static boolean valid(TrackData track) {
        return track != null &&
                !track.getId().isEmpty() &&
                !track.getTitle().isEmpty() &&
                !track.getArtist().isEmpty() &&
                !track.getIcon().isEmpty() &&
                !track.getUrl().isEmpty() &&
                track.getDuration() > 0 &&
                EncodingUtils.isValidUrl(track.getUrl()) &&
                EncodingUtils.isValidUrl(track.getIcon());
    }
}
