package moe.seikimo.laudiolin;

import lombok.Data;
import lombok.SneakyThrows;
import moe.seikimo.laudiolin.utils.EncodingUtils;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public final class Config {
    private static Config instance = new Config();

    /**
     * @return The configuration instance.
     */
    public static Config get() {
        return Config.instance;
    }

    /**
     * Loads the configuration from a file.
     */
    @SneakyThrows
    public static void load() {
        var configFile = new File("config.json");

        if (!configFile.exists()) {
            // Save this configuration.
            Config.save();
        } else {
            // Load the configuration.
            Config.instance = EncodingUtils.jsonDecode(
                    new FileReader(configFile), Config.class);

            // Check if the configuration is null.
            if (Config.instance == null) {
                Config.instance = new Config();
            }
        }
    }

    /**
     * Saves the configuration.
     */
    @SneakyThrows
    public static void save() {
        var configFile = new File("config.json");

        // Save the configuration.
        var json = EncodingUtils.jsonEncode(Config.instance);
        Files.write(configFile.toPath(), json.getBytes());
    }

    private int port = 3000;
    private String appTarget = "https://app.seikimo.moe";
    private String webTarget = "https://laudiolin.seikimo.moe";
    private String mongoUri = "mongodb://localhost:27017";
    private String storagePath = "files";

    public SeiKiMo seikimo = new SeiKiMo();
    public Discord discord = new Discord();
    public Spotify spotify = new Spotify();
    public RateLimits rateLimits = new RateLimits();

    @Data
    public static final class SeiKiMo {
        private String baseUrl = "https://seikimo.moe";
        private String adminToken;
    }

    @Data
    public static final class Discord {
        private String discordClientId = "";
        private String logoHash = "";
        private boolean presenceDetails = false;
    }

    @Data
    public static final class Spotify {
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static final class RateLimits {
        private int maxRequests = 100;
        private int withinTime = 1;
        private TimeUnit withinUnit = TimeUnit.MINUTES;
        private int timeToReset = 30;
        private TimeUnit resetUnit = TimeUnit.MINUTES;

        private List<String> exempt = new ArrayList<>();
        private List<String> whitelist = new ArrayList<>();
    }
}
