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
                var data = String.join(" ", args);
                ElixirUtils.playTrack(guild, data);
                logger.info("Playing using query '{}'.", data);
            }
            case "resume" -> {
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

                // Resume the player.
                ElixirUtils.resume(guild);
                logger.info("Resumed player.");
            }
            case "pause" -> {
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

                // Pause the player.
                ElixirUtils.pause(guild);
                logger.info("Paused player.");
            }
            case "volume" -> {
                // Check if a volume was provided.
                if (args.isEmpty()) {
                    logger.info("No arguments provided.");
                    logger.info("Usage: /elixir volume <volume>");
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

                // Parse the volume.
                var volume = Integer.parseInt(args.get(0));
                if (volume < 0 || volume > 100) {
                    logger.info("Volume must be between 0 and 100.");
                    return;
                }

                // Set the volume.
                ElixirUtils.volume(guild, volume);
                logger.info("Set volume to {}.", volume);
            }
            case "shuffle" -> {
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

                // Shuffle the queue.
                ElixirUtils.shuffle(guild);
                logger.info("Shuffled queue.");
            }
            case "skip" -> {
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

                // Check if a track number was specified.
                var trackNumber = 1;
                if (!args.isEmpty()) try {
                    trackNumber = Integer.parseInt(args.get(0));
                } catch (Exception ignored) {
                    logger.info("Invalid track number provided.");
                    return;
                }
                if (trackNumber < 1) {
                    logger.info("Track number must be greater than 0.");
                    return;
                }

                // Skip the current track.
                ElixirUtils.skip(guild, trackNumber);
                logger.info("Skipped track.");
            }
            case "seek" -> {
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

                // Check if a position was specified.
                if (args.isEmpty()) {
                    // Get the current position.
                    var position = guild.getTrackPosition();
                    logger.info("Current position is {}.", position);
                } else {
                    // Parse the position.
                    var position = Float.parseFloat(args.get(0));
                    if (position < 0) {
                        logger.info("Position must be greater than 0.");
                        return;
                    }

                    // Seek to the position.
                    ElixirUtils.seek(guild, (long) (position * 1000f));
                    logger.info("Seeked to position {}.", position);
                }
            }
            case "queue" -> {
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

                // Get the current track.
                var currentTrack = guild.getTrackData();
                if (currentTrack != null) {
                    logger.info("Current track: {}.", currentTrack);
                }

                // Get the queue.
                var queue = ElixirUtils.queue(guild);
                if (queue.isEmpty()) {
                    logger.info("Queue is empty.");
                    return;
                }

                // Print the queue.
                logger.info("Queue:");
                for (var i = 0; i < queue.size(); i++) {
                    var track = queue.get(i);
                    logger.info("{}. {}.", i + 1, track);
                }
            }
        }
    }
}
