package moe.seikimo.laudiolin.models.data;

import com.google.gson.JsonObject;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.Data;
import moe.seikimo.laudiolin.interfaces.DatabaseObject;
import moe.seikimo.laudiolin.objects.JObject;

import java.util.List;

@Data
@Entity(value = "playlists", useDiscriminator = false)
public class Playlist implements DatabaseObject<Playlist> {
    @Id private String id;
    private String owner;

    private String name;
    private String description;
    private String icon;

    private boolean isPrivate;
    private List<TrackData> tracks;

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
