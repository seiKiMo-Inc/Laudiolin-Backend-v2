package moe.seikimo.laudiolin.objects.user;

import com.google.gson.annotations.SerializedName;

public enum PresenceMode {
    @SerializedName("None") NONE,
    @SerializedName("Generic") PLAYING,
    @SerializedName("Simple") LISTENING
}
