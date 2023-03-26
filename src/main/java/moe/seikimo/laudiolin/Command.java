package moe.seikimo.laudiolin;

import moe.seikimo.laudiolin.enums.Source;
import moe.seikimo.laudiolin.gateway.Gateway;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.utils.ElixirUtils;
import moe.seikimo.laudiolin.utils.SpotifyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Command {
    private static final Logger logger = LoggerFactory.getLogger("Command Output");
    private static final Map<String, Consumer<List<String>>> commands = new HashMap<>() {{
        this.put("elixir", Command::elixirCommand);
    }};

    /**
     * Executes a command.
     *
     * @param command The command to execute.
     * @param args The arguments to pass to the command.
     */
    public static void execute(String command, List<String> args) {
        var consumer = Command.commands.get(command);

        if (consumer != null) {
            consumer.accept(args);
        }
    }

    private static String targetGuildId = null;

    /**
     * Command handler for '/elixir'.
     *
     * @param args The arguments to pass to the command.
     */
    private static void elixirCommand(List<String> args) {
        if (args.isEmpty()) {
            logger.info("No arguments provided.");
            logger.info("Usage: /elixir <play|target> <id>");
            return;
        }

        var subCommand = args.get(0);
        args = args.subList(1, args.size());
        switch (subCommand) {
            default -> logger.info("Invalid argument provided.");
            case "target" -> {
                // Check if a guild ID was provided.
                if (args.isEmpty()) {
                    logger.info("No arguments provided.");
                    logger.info("Usage: /elixir target <guildId>");
                    return;
                }

                var targetGuild = args.get(0);
                if (targetGuild.isEmpty()) {
                    Command.targetGuildId = null;
                    logger.info("Cleared target guild ID.");
                } else {
                    Command.targetGuildId = targetGuild;
                    logger.info("Set target guild ID to {}.", Command.targetGuildId);
                }
            }
            case "play" -> {
                // Check if a track ID was provided.
                if (args.isEmpty()) {
                    logger.info("No arguments provided.");
                    logger.info("Usage: /elixir play <track id>");
                    return;
                }

                // Check for a target guild.
                if (Command.targetGuildId == null) {
                    logger.info("No target guild ID set.");
                    logger.info("Usage: /elixir target <guildId>");
                    return;
                }

                // Fetch the guild.
                var guild = Gateway.getConnectedUser(targetGuildId);
                if (guild == null) {
                    logger.info("No guild found with ID {}.", Command.targetGuildId);
                    return;
                }

                // Perform a lookup for the track.
                var id = args.get(0);
                var track = switch (Source.identify(null, id)) {
                    case ALL, YOUTUBE -> TrackData.toTrack(Laudiolin.getNode().youtubeFetch(id));
                    case SPOTIFY -> SpotifyUtils.toTrackData(SpotifyUtils.searchId(id));
                    case UNKNOWN -> null;
                };

                if (track == null) {
                    logger.info("No track found with ID {}.", id);
                } else {
                    ElixirUtils.playTrack(guild, track);
                    logger.info("Playing track {}.", track);
                }
            }
        }
    }
}
