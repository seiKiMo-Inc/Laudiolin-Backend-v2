package moe.seikimo.laudiolin.objects.user;

import com.google.gson.annotations.SerializedName;

public enum SocialStatus {
    @SerializedName("Nobody") NONE,
    @SerializedName("Friends") FRIENDS,
    @SerializedName("Everyone") EVERYONE
}
