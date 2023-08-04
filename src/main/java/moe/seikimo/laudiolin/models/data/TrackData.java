package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import lombok.Builder;
import lombok.Data;
import moe.seikimo.laudiolin.objects.JObject;

import java.util.List;

@Data
@Entity
@Builder
public class TrackData {
    private String id;
    private String title;
    private String artist;
    private String icon, url;
    private int duration;

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
                .add("other", other)
                .gson();
    }
}
