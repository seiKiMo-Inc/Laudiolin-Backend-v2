package moe.seikimo.laudiolin.models;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.objects.JObject;

@Data
public final class BasicUserInfo {
    @SerializedName("id")
    private String userId;
    private String icon;
    private String displayName;

    /**
     * Explains this object.
     *
     * @see User#publicInfo()
     * @return The explanation.
     */
    public JsonObject explain() {
        return JObject.c()
                .add("id", this.getUserId())
                .add("icon", this.getIcon())
                .add("displayName", this.getDisplayName())
                .gson();
    }
}
