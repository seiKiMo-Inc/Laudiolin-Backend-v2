package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import lombok.Data;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.interfaces.DatabaseObject;
import moe.seikimo.laudiolin.models.BasicUserInfo;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.DatabaseUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import moe.seikimo.laudiolin.utils.HttpUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity(value = "users", useDiscriminator = false)
public class User implements DatabaseObject<User> {
    /**
     * Fetches a user by their ID.
     *
     * @param id The ID of the user.
     * @return The user.
     */
    public static User getUserById(String id) {
        return DatabaseUtils.fetch(
                User.class, "_id", id);
    }

    @Id private String userId;

    @Reference(idOnly = true, ignoreMissing = true, lazy = true)
    private List<Playlist> playlists = new ArrayList<>();
    private List<TrackData> likedSongs = new ArrayList<>();
    private List<TrackData> recentlyPlayed = new ArrayList<>();

    private String presenceToken;

    public User() {
        // Empty constructor for Morphia.
    }

    /**
     * @return The public information about the user.
     */
    public BasicUserInfo publicInfo() {
        return EncodingUtils.jsonDecode(HttpUtils.makeRequest(Config.get().seikimo
                .getBaseUrl() + "/account/" + this.getUserId()), BasicUserInfo.class);
    }

    /**
     * @param withPrivate Whether to include private data.
     * @return The user's information. Including data from here, and the public information.
     */
    public JsonObject userInfo(boolean withPrivate) {
        var publicInfo = this.publicInfo();

        return JObject.c()
                .addAll(this.explain(withPrivate))
                .addAll(publicInfo.explain())
                // Add fallback fields.
                .add("username", publicInfo.getDisplayName())
                .add("userId", publicInfo.getUserId())
                .add("avatar", publicInfo.getIcon())
                .gson();
    }

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
        baseData.add("playlists", playlists.stream()
                .map(Playlist::getId)
                .toList());

        return baseData.gson();
    }
}
