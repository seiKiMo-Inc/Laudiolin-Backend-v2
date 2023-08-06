package moe.seikimo.laudiolin.models;

import lombok.Data;
import moe.seikimo.laudiolin.objects.user.PresenceMode;
import moe.seikimo.laudiolin.objects.user.SocialStatus;

@Data
public final class InitializeMessage {
    private String token;
    private SocialStatus broadcast;
    private PresenceMode presence;
}
