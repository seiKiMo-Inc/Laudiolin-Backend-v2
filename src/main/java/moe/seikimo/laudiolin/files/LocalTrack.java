package moe.seikimo.laudiolin.files;

import moe.seikimo.laudiolin.models.data.TrackData;

import java.io.File;

public record LocalTrack(
        TrackData data,
        File trackFile,
        File coverFile
) {
}
