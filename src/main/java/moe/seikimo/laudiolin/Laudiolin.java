package moe.seikimo.laudiolin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinGson;
import io.javalin.plugin.bundled.CorsPluginConfig;
import lombok.Getter;
import moe.seikimo.laudiolin.gateway.Gateway;
import moe.seikimo.laudiolin.objects.Constants;
import moe.seikimo.laudiolin.routers.*;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import moe.seikimo.laudiolin.utils.NetUtils;
import moe.seikimo.laudiolin.utils.SpotifyUtils;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public final class Laudiolin {
    private static final long startTime = System.currentTimeMillis();

    static {
        // Set logback configuration file.
        System.setProperty("logback.configurationFile", "logback.xml");
    }

    @Getter private static final Logger logger
            = LoggerFactory.getLogger("Laudiolin Backend");
    @Getter private static final Javalin javalin
            = Javalin.create(Laudiolin::configureJavalin);
    @Getter private static final ExecutorService threadPool
            = new ScheduledThreadPoolExecutor(4);
    @Getter private static final Map<String, String> arguments
            = new HashMap<>();

    @Getter private static Node node = null;
    private static Process nodeProcess = null;
    private static LineReader lineReader = null;

    @Getter private static MongoClient mongoClient;
    @Getter private static Datastore datastore;

    /**
     * Application entrypoint.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        try {
            {
                // Parse the startup arguments.
                Laudiolin.parseArguments(args);
                // Load the configuration.
                Config.load();

                logger.info("Configuration loaded.");
            }

            {
                // Check paths.
                var storageFile = Constants.STORAGE_PATH;
                if (!storageFile.exists() && !storageFile.mkdirs()) {
                    throw new IOException("Failed to create storage directory.");
                }
            }

            {
                // Initialize systems.
                SpotifyUtils.initialize();
            }

            {
                var networkPort = arguments.containsKey("port") ?
                        Integer.parseInt(arguments.get("port")) :
                        NetUtils.findFreePort();

                // Check if a Node instance should start.
                if (!arguments.containsKey("no-node")) {
                    if (Laudiolin.startNode(networkPort)) try {
                        // Wait for the Node instance to start.
                        Thread.sleep(1000);
                    } catch (InterruptedException exception) {
                        logger.error("Failed to wait for Node instance to start.");
                    }
                }

                // Create a node instance.
                Laudiolin.node = new Node(networkPort);
                Laudiolin.node.connect();
            }

            {
                // Start the console.
                Laudiolin.getConsole();
                new Thread(Laudiolin::startConsole).start();

                logger.info("Console started.");
            }

            {
                // Create a MongoDB client.
                var mongoClient = Laudiolin.mongoClient =
                        MongoClients.create(Config.get().getMongoUri());

                // Create mapper options.
                var options = MapperOptions.builder()
                        .mapSubPackages(true)
                        .storeEmpties(true)
                        .storeNulls(true)
                        .build();

                var datastore = Laudiolin.datastore =
                        Morphia.createDatastore(mongoClient, "laudiolin", options);
                // Configure the mapper.
                datastore.getMapper().map(Constants.MODELS);
                // Ensure indexes.
                datastore.ensureIndexes();

                logger.info("Connected to database.");
            }

            {
                var javalin = Laudiolin.getJavalin();

                // Set the exception handler.
                javalin.exception(Exception.class, SiteRouter::handleException);

                // Configure routers.
                SiteRouter.configure(javalin);
                UserRouter.configure(javalin);
                ProxyRouter.configure(javalin);
                SocialRouter.configure(javalin);
                StreamRouter.configure(javalin);
                SearchRouter.configure(javalin);
                PlaylistRouter.configure(javalin);

                Gateway.configure(javalin);

                // Start the Javalin instance.
                javalin.start(Config.get().getPort());

                logger.info("Javalin started.");
            }

            // Log the startup time.
            logger.info("Laudiolin backend started in {}ms.",
                    System.currentTimeMillis() - startTime);
        } catch (ConnectException | URISyntaxException ignored) {
            logger.error("Unable to find the Node pipe.");
        } catch (IOException exception) {
            logger.error("Failed to start Laudiolin.", exception);
        }
    }

    /**
     * Configures the Javalin instance.
     */
    private static void configureJavalin(JavalinConfig config) {
        // Configure the JSON mapper.
        config.jsonMapper(new JavalinGson(EncodingUtils.GSON));

        // Configure CORS.
        config.plugins.enableCors(container ->
                container.add(CorsPluginConfig::anyHost));
    }

    /**
     * Parses the startup arguments.
     *
     * @param args The startup arguments.
     */
    private static void parseArguments(String[] args) {
        var map = new HashMap<String, String>();

        for (var arg : args) {
            if (arg.startsWith("--")) {
                var argument = arg.substring(2).split("=");
                map.put(argument[0], argument.length > 1 ? argument[1] : "");
            }
        }

        // Set the arguments.
        Laudiolin.arguments.putAll(map);
    }

    /**
     * Starts a Node instance.
     *
     * @param port The port to use.
     * @return Whether the Node instance started successfully.
     */
    private static boolean startNode(int port) {
        var nodeExecutable = arguments.getOrDefault(
                "node", "node");

        var storagePath = Constants.STORAGE_PATH.getAbsolutePath();

        try {
            // Execute the Node instance.
            var runtime = Runtime.getRuntime();
            Laudiolin.nodeProcess = runtime.exec(
                    nodeExecutable + " index.js",
                    new String[] {
                            "PORT=" + port,
                            "STORAGE_PATH=" + storagePath,
                            "FFMPEG_PATH=" + Config.get().getFfmpegPath(),
                    }
            );

            // Redirect the Node output to the console.
            var nodeOutput = Laudiolin.nodeProcess.getInputStream();
            new Thread(() -> {
                try {
                    // Read the Node output.
                    var buffer = new byte[1024];
                    var length = 0;

                    while ((length = nodeOutput.read(buffer)) != -1) {
                        // Print the Node output.
                        Laudiolin.getConsole().printAbove(new String(
                                Arrays.copyOfRange(buffer, 0, length)));
                    }
                } catch (IOException exception) {
                    Laudiolin.getLogger().error(
                            "Failed to read Node output.", exception);
                }
            }).start();

            return true;
        } catch (IOException exception) {
            Laudiolin.getLogger().error("Failed to start Node.", exception);
            return false;
        }
    }

    /**
     * @return The terminal line reader.
     *         Creates a new line reader if not already created.
     */
    public static LineReader getConsole() {
        // Check if the line reader exists.
        if (Laudiolin.lineReader == null) {
            Terminal terminal = null; try {
                // Create a native terminal.
                terminal = TerminalBuilder.builder()
                        .jna(true).build();
            } catch (Exception ignored) {
                try {
                    // Fallback to a dumb JLine terminal.
                    terminal = TerminalBuilder.builder()
                            .dumb(true).build();
                } catch (Exception ignored1) { }
            }

            // Set the line reader.
            Laudiolin.lineReader = LineReaderBuilder.builder()
                    .terminal(terminal).build();
        }

        return Laudiolin.lineReader;
    }

    /**
     * Starts the line reader.
     */
    public static void startConsole() {
        String input = null;
        var isLastInterrupted = false;
        var logger = Laudiolin.getLogger();

        while (true) {
            try {
                input = Laudiolin.lineReader.readLine("> ");
            } catch (UserInterruptException ignored) {
                if (!isLastInterrupted) {
                    isLastInterrupted = true;
                    logger.info("Press Ctrl-C again to shutdown.");
                    continue;
                } else {
                    Runtime.getRuntime().exit(0);
                }
            } catch (EndOfFileException ignored) {
                continue;
            } catch (IOError e) {
                logger.error("An IO error occurred while trying to read from console.", e);
                return;
            }

            isLastInterrupted = false;

            try {
                // Invoke the command.
                // TODO: Invoke commands.
            } catch (Exception e) {
                logger.warn("An error occurred while trying to invoke command.", e);
            }
        }
    }
}
