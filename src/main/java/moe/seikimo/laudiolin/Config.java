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
     *
     * @param filePath The path to the configuration file.
     */
    @SneakyThrows
    public static void load(String filePath) {
        var configFile = new File(filePath);

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
    private String webTarget = "https://laudiol.in";
    private String ffmpegPath = "/usr/bin/ffmpeg";
    private String mongoUri = "mongodb://localhost:27017";
    private String storagePath = "files";

    public SeiKiMo seikimo = new SeiKiMo();
    public Elixir elixir = new Elixir();
    public Discord discord = new Discord();
    public Spotify spotify = new Spotify();
    public YouTube youtube = new YouTube();
    public RateLimits rateLimits = new RateLimits();
    public Storage storage = new Storage();
    public PublicData publicData = new PublicData();

    @Data
    public static final class SeiKiMo {
        private String baseUrl = "https://seikimo.moe";
        private String adminToken;
    }

    @Data
    public static final class Elixir {
        private String token = "";
    }

    @Data
    public static final class Discord {
        private String clientId = "";
        private String logoHash = "";
        private boolean presenceDetails = false;
    }

    @Data
    public static final class Spotify {
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static final class YouTube {
        private String originToken;
        private String clientData;
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

    @Data
    public static final class Storage {
        public boolean hostRemote = true; // Toggle to host only local files.
        public boolean searchRemote = true; // Toggle to search only local files.
        public String tracks = "tracks"; // This is where songs are located. Must be in MP3 format with proper metadata.
    }

    @Data
    public static final class PublicData {
        public List<String> playlists = new ArrayList<>(); // This is a list of playlists to appear everywhere.
        public String deleteUrl, tosUrl, privacyUrl;
    }
}
