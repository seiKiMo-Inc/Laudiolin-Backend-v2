package moe.seikimo.laudiolin.models;

import lombok.Data;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.objects.user.PresenceMode;

@Data
public final class MinifiedPresence {
    private TrackData track;
    private PresenceMode broadcast;
    private long started, shouldEnd;
    private boolean remove;
}
