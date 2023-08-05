package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.Data;
import lombok.experimental.Accessors;
import moe.seikimo.laudiolin.interfaces.DatabaseObject;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.DatabaseUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@Entity(value = "playlists", useDiscriminator = false)
public class Playlist implements DatabaseObject<Playlist> {
    /**
     * Gets a playlist by its ID.
     *
     * @param id The ID of the playlist.
     * @return The playlist.
     */
    public static Playlist getPlaylistById(String id) {
        return DatabaseUtils.fetch(
                Playlist.class, "_id", id);
    }

    /**
     * Checks if a playlist is valid.
     *
     * @param playlist The playlist data.
     * @return True if the playlist is valid.
     */
    public static boolean valid(Playlist playlist) {
        // Check playlist basics.
        var basic = playlist != null &&
                !playlist.getId().isEmpty() &&
                !playlist.getOwner().isEmpty() &&
                !playlist.getName().isEmpty() &&
                !playlist.getIcon().isEmpty();
        if (!basic) return false;

        // Validate individual tracks.
        for (var track : playlist.getTracks()) {
            if (!TrackData.valid(track)) return false;
        }

        return EncodingUtils.isValidUrl(playlist.getIcon());
    }

    @Id private String id;
    @NotNull private String owner;

    @NotNull private String name;
    @NotNull private String description;
    @NotNull private String icon;

    private boolean isPrivate;
    @NotNull private List<TrackData> tracks = new ArrayList<>();

    public Playlist() {
        // Empty constructor for Morphia.
    }

    @Override
    public JsonObject explain() {
        return JObject.c()
                .add("id", this.id)
                .add("owner", this.owner)
                .add("name", this.name)
                .add("description", this.description)
                .add("icon", this.icon)
                .add("isPrivate", this.isPrivate)
                .add("tracks", this.tracks)
                .gson();
    }
}
