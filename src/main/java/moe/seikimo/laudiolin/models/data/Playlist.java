package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.Data;
import lombok.experimental.Accessors;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.interfaces.DatabaseObject;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.utils.Assertions;
import moe.seikimo.laudiolin.utils.DatabaseUtils;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import moe.seikimo.laudiolin.utils.RandomUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        Assertions.check(playlist != null, "Playlist cannot be null.");
        Assertions.check(!playlist.getId().isEmpty(), "Playlist ID cannot be empty.");
        Assertions.check(!playlist.getOwner().isEmpty(), "Playlist owner cannot be empty.");
        Assertions.check(!playlist.getName().isEmpty(), "Playlist name cannot be empty.");
        Assertions.check(!playlist.getIcon().isEmpty(), "Playlist icon cannot be empty.");

        // Validate individual tracks.
        for (var i = 0; i < playlist.getTracks().size(); i++) {
            var track = playlist.getTracks().get(i);
            try {
                TrackData.valid(track);
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Invalid track at index " + i + "; " + invalid.getMessage());
            }
        }

        return EncodingUtils.isValidUrl(playlist.getIcon());
    }

    /**
     * Migrates an old playlist to a new one.
     *
     * @param owner The owner of the playlist.
     * @param legacy The old playlist.
     * @return The new playlist.
     */
    public static Playlist migrate(User owner, Document legacy) {
        var playlist = new Playlist();

        // Convert the playlist ID.
        var oldId = legacy.getObjectId("_id");
        playlist.setId(oldId.toHexString());

        // Apply basic properties.
        playlist.setOwner(owner.getUserId());
        playlist.setName(legacy.getString("name"));
        playlist.setDescription(legacy.getString("description"));
        playlist.setIcon(legacy.getString("icon"));
        playlist.setPrivate(legacy.getBoolean("isPrivate"));

        // Add all playlist tracks.
        List<TrackData> tracks = new ArrayList<>();
        for (var track : legacy.getList("tracks", Document.class)) {
            tracks.add(TrackData.toTrack(track));
        }
        playlist.setTracks(tracks);

        return playlist.save();
    }

    @Id private String id;
    @NotNull private String owner;

    @NotNull private String name;
    @NotNull private String description;
    @NotNull private String icon;

    private boolean isPrivate;
    @NotNull private List<TrackData> tracks = new ArrayList<>();

    private String iconId = null;

    public Playlist() {
        // Empty constructor for Morphia.
    }

    /**
     * Sets the playlist's image from a Base64-encoded image.
     *
     * @param image The Base64-encoded image.
     */
    public void setIconRaw(String image) throws IOException {
        // Decode the image.
        var imageData = EncodingUtils.base64Decode(image);

        if (this.iconId != null) {
            // Check if the file exists on the system.
            var file = new File(
                    Config.get().getStoragePath(),
                    this.iconId + ".png");

            if (file.exists() && !file.delete()) {
                throw new IllegalStateException("Failed to delete the old playlist icon.");
            }
        }

        // Save the image to the system.
        var iconId = RandomUtils.randomString(16);
        var file = new File(
                Config.get().getStoragePath(),
                iconId + ".png");
        Files.write(file.toPath(), imageData);

        // Set the new icon ID.
        this.iconId = iconId;
        this.icon = Config.get().getAppTarget() + "/storage/" + iconId;

        this.save();
    }

    @Override
    public boolean delete() {
        // Delete the playlist icon.
        if (this.iconId != null) {
            var file = new File(
                    Config.get().getStoragePath(),
                    this.iconId + ".png");
            if (file.exists() && !file.delete()) {
                throw new IllegalStateException("Failed to delete the playlist icon.");
            }
        }

        return DatabaseObject.super.delete();
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
