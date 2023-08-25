package moe.seikimo.laudiolin.gateway;

import com.google.gson.JsonObject;
import moe.seikimo.laudiolin.objects.JObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ElixirManager {
    // Map of Bot ID -> Map of Guild ID -> List of controlling sessions.
    private static final Map<String, Map<String, List<GatewaySession>>> elixirs
            = new ConcurrentHashMap<>();

    /**
     * Registers a new controlling session for a bot.
     *
     * @param botId The bot ID.
     * @param guildId The guild ID.
     * @param session The session.
     */
    public static void addControllingSession(String botId, String guildId, GatewaySession session) {
        elixirs.computeIfAbsent(botId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(guildId, k -> new CopyOnWriteArrayList<>())
                .add(session);
    }

    /**
     * Registers a new controlling session for a bot.
     *
     * @param initiator The session that initiated the broadcast.
     */
    public static void addControllingSession(GatewaySession initiator) {
        ElixirManager.addControllingSession(
                initiator.getBotId(),
                initiator.getGuildId(),
                initiator);
    }

    /**
     * Removes a controlling session for a bot.
     *
     * @param botId The bot ID.
     * @param guildId The guild ID.
     * @param session The session.
     */
    public static void removeControllingSession(String botId, String guildId, GatewaySession session) {
        elixirs.computeIfAbsent(botId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(guildId, k -> new CopyOnWriteArrayList<>())
                .remove(session);
    }

    /**
     * Removes a controlling session for a bot.
     *
     * @param initiator The session that initiated the broadcast.
     */
    public static void removeControllingSession(GatewaySession initiator) {
        ElixirManager.removeControllingSession(
                initiator.getBotId(),
                initiator.getGuildId(),
                initiator);
    }

    /**
     * Broadcasts a message to all controllers of a bot.
     *
     * @param botId The bot ID.
     * @param guildId The guild ID.
     * @param message The message.
     */
    public static void broadcastToAll(String botId, String guildId, JsonObject message) {
        elixirs.computeIfAbsent(botId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(guildId, k -> new CopyOnWriteArrayList<>())
                .forEach(s -> s.sendMessage(message));
    }

    /**
     * Broadcasts a message to all controllers of a bot.
     *
     * @param initiator The session that initiated the broadcast.
     * @param message The message.
     */
    public static void broadcastToAll(GatewaySession initiator, JObject message) {
        ElixirManager.broadcastToAll(
                initiator.getBotId(),
                initiator.getGuildId(),
                message.gson());
    }

    /**
     * Broadcasts a message to all controllers of a bot.
     *
     * @param initiator The session that initiated the broadcast.
     * @param message The message.
     */
    public static void broadcastToAll(GatewaySession initiator, JsonObject message) {
        ElixirManager.broadcastToAll(
                initiator.getBotId(),
                initiator.getGuildId(),
                message);
    }
}
