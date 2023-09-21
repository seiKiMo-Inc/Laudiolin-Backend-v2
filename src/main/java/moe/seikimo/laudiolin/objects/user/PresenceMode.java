package moe.seikimo.laudiolin.objects.user;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public enum PresenceMode {
    @SerializedName("None") NONE,
    @SerializedName("Generic") PLAYING,
    @SerializedName("Simple") LISTENING,
    @SerializedName("Detailed") SPOTIFY;

    public static final Set<PresenceMode> IS_LISTENING = Set.of(LISTENING, SPOTIFY);
}
