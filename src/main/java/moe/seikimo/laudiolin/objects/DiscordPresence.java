package moe.seikimo.laudiolin.objects;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import moe.seikimo.laudiolin.utils.HttpUtils;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import javax.annotation.Nullable;
import java.util.List;

@Data @Builder
public final class DiscordPresence {
    @SerializedName("application_id")
    private String applicationId;
    private String id, name;
    private int type;

    private String url;
    private String details, state;

    private Timestamps timestamps;
    @SerializedName("sync_id")
    private String syncId;

    private String platform;
    private Integer flags;

    private Party party;
    private Assets assets;
    private List<Button> buttons;

    @SerializedName("session_id")
    private String sessionId;

    @Data @Builder
    public static class Timestamps {
        private Integer start;
        private Integer end;
    }

    @Data @Builder
    public static class Party {
        private String id;
        private Integer size, max;
    }

    @Data @Builder
    public static class Assets {
        @SerializedName("large_image")
        private String largeImage;
        @SerializedName("large_text")
        private String largeText;
        @SerializedName("small_image")
        private String smallImage;
        @SerializedName("small_text")
        private String smallText;
    }

    @Data @Builder
    public static class Button {
        private String label, url;
    }

    @AllArgsConstructor @Getter
    public enum PresenceType {
        PLAYING(0),
        STREAMING(1),
        LISTENING(2),
        WATCHING(3),
        CUSTOM(4),
        COMPETING(5);

        final int value;
    }

    @AllArgsConstructor @Getter
    public enum Platform {
        DESKTOP("desktop"),
        IOS("ios"),
        ANDROID("android");

        final String value;
    }

    /**
     * Applies this rich presence to a user.
     *
     * @see #apply(User, DiscordPresence)
     * @param user The user to apply the rich presence to.
     */
    public void apply(User user) {
        DiscordPresence.apply(user, this);
    }

    /**
     * Sets a user's rich presence.
     *
     * @param user The user to set the rich presence for.
     * @param presence The rich presence to set.
     */
    public static void apply(
            User user,
            @Nullable DiscordPresence presence
    ) {
        // Prepare the request body.
        var body = JObject.c()
                .add("userId", user.getUserId())
                .add("presence", presence)
                .toString();

        // Prepare the backend request.
        var seikimo = Config.get().seikimo;
        var requestBody = RequestBody.create(
                body, HttpUtils.JSON_MEDIA_TYPE);
        var request = new Request.Builder()
                .url(seikimo.getBaseUrl() + "/account/presence")
                .method("POST", requestBody)
                .header("Authorization", seikimo.getAdminToken())
                .build();

        // Execute the request.
        try (var response = HttpUtils.makeRequest(request)) {
            // Check if the response executed.
            if (response == null)
                throw new RuntimeException("Failed to set rich presence: "
                        + "null response");

            // Check the response code.
            if (!response.isSuccessful())
                throw new RuntimeException("Failed to set rich presence: "
                        + response.code() + " " + response.message());
        }
    }
}
