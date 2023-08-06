package moe.seikimo.laudiolin.models;

import lombok.Builder;
import lombok.Setter;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.user.SocialStatus;

@Setter
@Builder
public final class OfflineUser {
    private String username;
    private String userId;
    private String avatar;
    private SocialStatus socialStatus;
    private Long lastSeen;
    private TrackData lastListeningTo;
}
