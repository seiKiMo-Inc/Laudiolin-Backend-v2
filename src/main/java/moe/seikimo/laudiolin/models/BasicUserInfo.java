package moe.seikimo.laudiolin.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public final class BasicUserInfo {
    @SerializedName("id")
    private String userId;
    private String icon;
    private String displayName;
}
