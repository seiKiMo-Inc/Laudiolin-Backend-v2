package moe.seikimo.laudiolin.objects;

import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.models.data.Playlist;
import moe.seikimo.laudiolin.models.data.User;

import java.io.File;

public interface Constants {
    Class<?>[] MODELS = {
            User.class, Playlist.class
    };

    File STORAGE_PATH = new File(Config.get().getStoragePath());
}
