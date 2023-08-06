package moe.seikimo.laudiolin.models;

import lombok.Builder;
import lombok.Setter;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.user.SocialStatus;

@Setter
@Builder
public final class OnlineUser {
    private String username;
    private String userId;
    private String avatar;
    private SocialStatus socialStatus;
    private TrackData listeningTo;
    private Float progress;
}
