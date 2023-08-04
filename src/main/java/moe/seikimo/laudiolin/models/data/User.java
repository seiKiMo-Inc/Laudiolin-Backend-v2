package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import lombok.Data;
import moe.seikimo.laudiolin.interfaces.DatabaseObject;
import moe.seikimo.laudiolin.objects.JObject;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity(value = "users", useDiscriminator = false)
public class User implements DatabaseObject<User> {
    @Id private String userId;

    @Reference(idOnly = true, ignoreMissing = true, lazy = true)
    private List<Playlist> playlists = new ArrayList<>();
    private List<TrackData> likedSongs = new ArrayList<>();
    private List<TrackData> recentlyPlayed = new ArrayList<>();

    private String presenceToken;

    @Override
    public JsonObject explain() {
        return this.explain(false);
    }

    /**
     * Explains the object.
     *
     * @param withPrivate Whether to include private data.
     * @return The object explained.
     */
    public JsonObject explain(boolean withPrivate) {
        var baseData = JObject.c()
                .add("userId", this.getUserId())
                .add("likedSongs", this.getLikedSongs())
                .add("recentlyPlayed", this.getRecentlyPlayed());

        // Resolve all public playlists.
        var playlists = withPrivate ? this.getPlaylists() :
                this.getPlaylists().stream()
                        .filter(playlist -> !playlist.isPrivate())
                        .toList();
        baseData.add("playlists", playlists);

        return baseData.gson();
    }
}
