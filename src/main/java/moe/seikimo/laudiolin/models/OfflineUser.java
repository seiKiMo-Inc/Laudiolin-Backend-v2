package moe.seikimo.laudiolin.models;

import lombok.Builder;
import lombok.Data;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.user.SocialStatus;

@Data
@Builder
public final class OfflineUser {
    private String username;
    private String userId;
    private String avatar;
    private SocialStatus socialStatus;
    private Long lastSeen;
    private TrackData lastListeningTo;
}
