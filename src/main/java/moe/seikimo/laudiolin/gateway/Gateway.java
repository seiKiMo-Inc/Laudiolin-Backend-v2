package moe.seikimo.laudiolin.gateway;

import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;
import lombok.Getter;
import moe.seikimo.laudiolin.Laudiolin;
import moe.seikimo.laudiolin.models.OfflineUser;
import moe.seikimo.laudiolin.models.OnlineUser;
import moe.seikimo.laudiolin.objects.JObject;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Gateway {
    private static final Map<String, GatewaySession> sessions
            = new ConcurrentHashMap<>();

    @Getter private static final Map<String, List<GatewaySession>> users
            = new ConcurrentHashMap<>();
    @Getter private static final Map<String, OnlineUser> onlineUsers
            = new ConcurrentHashMap<>();
    @Getter private static final Map<String, OfflineUser> offlineUsers
            = new ConcurrentHashMap<>();

    private static final Map<String, MessageHandler> handlers = new HashMap<>() {{
        this.put("initialize", MessageHandler::initialize);
        this.put("latency", MessageHandler::latency);
        this.put("seek", MessageHandler::seek);
        this.put("listen", MessageHandler::listen);
        this.put("player", MessageHandler::player);
        this.put("volume", MessageHandler::volume);
    }};

    /**
     * Configures the websocket-based gateway.
     *
     * @param javalin The Javalin instance.
     */
    public static void configure(Javalin javalin) {
        javalin.ws("/", config -> {
            config.onConnect(Gateway::onConnect);
            config.onMessage(Gateway::onMessage);
            config.onClose(Gateway::onClose);
            config.onError(Gateway::onError);
        });
    }

    /**
     * Handles a new client connecting.
     *
     * @param ctx The context.
     */
    private static void onConnect(WsConnectContext ctx) {
        // Register the client for future reference.
        var session = new GatewaySession(ctx.session);
        Gateway.sessions.put(ctx.getSessionId(), session);

        // Prevent the client from disconnecting.
        ctx.session.setIdleTimeout(Duration.ZERO);

        session.onConnect();
    }

    /**
     * Handles a client sending a message.
     *
     * @param ctx The context.
     */
    private static void onMessage(WsMessageContext ctx) {
        try {
            // Get the client session.
            var session = Gateway.sessions.get(ctx.getSessionId());
            if (session == null) {
                // The client is not connected.
                ctx.closeSession();
                ctx.send(GATEWAY_NOT_INITIALIZED());
                return;
            }

            // Parse the message.
            var content = ctx.messageAsClass(JsonObject.class);
            if (content == null) return;

            // Check if the client has initialized.
            var messageType = content.get("type").getAsString();
            if (!session.isInitialized() && !messageType.equals("initialize")) {
                ctx.send(GATEWAY_NOT_INITIALIZED());
                ctx.closeSession();
                return;
            }

            // Attempt to handle the message.
            var handler = Gateway.handlers.get(messageType);
            if (handler == null) {
                ctx.send(GATEWAY_UNKNOWN_MESSAGE());
                ctx.closeSession();
            } else try {
                handler.handle(session, content);
            } catch (Exception ignored) {
                // This is thrown when a JSON parsing error occurs.
                ctx.send(GATEWAY_UNKNOWN_MESSAGE());
                ctx.closeSession();
            }
        } catch (Exception ignored) {
            ctx.send(INVALID_JSON()); // Send an error message.
            ctx.closeSession(); // Close the session.
        }
    }

    /**
     * Handles a client disconnecting.
     *
     * @param ctx The context.
     */
    private static void onClose(WsCloseContext ctx) {
        // Remove the client session.
        var session = Gateway.sessions.remove(ctx.getSessionId());
        if (session == null) return;

        session.onDisconnect();
    }

    /**
     * Handles a client erroring out, disconnecting the client.
     *
     * @param ctx The context.
     */
    private static void onError(WsErrorContext ctx) {
        var exception = ctx.error();
        if (exception == null) return;

        Laudiolin.getLogger().warn("Client {} disconnected with error {}.",
                ctx.getSessionId(), exception.getMessage());
        Laudiolin.getLogger().warn("Client exception caught!", exception);
    }

    /**
     * Fetches the first connected user.
     *
     * @param userId The user ID.
     * @return The session.
     */
    @Nullable
    public static GatewaySession getConnectedUser(String userId) {
        return Gateway.sessions.values().stream()
                .filter(session -> {
                    var user = session.getUser();
                    return user != null &&
                            user.getUserId().equals(userId);
                })
                .findFirst().orElse(null);
    }

    /** This message is sent when the client first connects. */
    public static JsonObject GATEWAY_INIT() {
        return JObject.c()
                .add("type", "initialize")
                .add("code", 0)
                .add("message", "Welcome to Laudiolin!")
                .add("timestamp", System.currentTimeMillis())
                .gson();
    }

    /** This message sends the current latency between the server and client. */
    public static JsonObject GATEWAY_PING(long latency) {
        return JObject.c()
                .add("type", "latency")
                .add("code", 0)
                .add("message", "")
                .add("timestamp", System.currentTimeMillis())
                .add("latency", latency)
                .gson();
    }

    /** This message states the client sent invalid JSON data. */
    public static JsonObject INVALID_JSON() {
        return JObject.c()
                .add("type", "")
                .add("code", 1)
                .add("message", "Invalid JSON received.")
                .add("timestamp", System.currentTimeMillis())
                .gson();
    }

    /** This message states the client hasn't finished initializing. */
    public static JsonObject GATEWAY_NOT_INITIALIZED() {
        return JObject.c()
                .add("type", "")
                .add("code", 2)
                .add("message", "Gateway not initialized.")
                .add("timestamp", System.currentTimeMillis())
                .gson();
    }

    /** This message states that the client sent an invalid message. */
    public static JsonObject GATEWAY_UNKNOWN_MESSAGE() {
        return JObject.c()
                .add("type", "")
                .add("code", 3)
                .add("message", "Invalid message received.")
                .add("timestamp", System.currentTimeMillis())
                .gson();
    }

    /** This message states the client provided an invalid token. */
    public static JsonObject GATEWAY_INVALID_TOKEN() {
        return JObject.c()
                .add("type", "")
                .add("code", 4)
                .add("message", "Invalid token received.")
                .add("timestamp", System.currentTimeMillis())
                .gson();
    }
}
