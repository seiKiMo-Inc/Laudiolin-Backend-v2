package moe.seikimo.laudiolin.gateway;

import io.javalin.Javalin;
import lombok.Getter;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Simple message broadcasting for WebRTC.
 */
public final class PeerSignaling {
    @Getter private static final Logger logger =
            LoggerFactory.getLogger("WebRTC");

    @Getter private static final Set<Session> sessions
            = new CopyOnWriteArraySet<>();

    /**
     * Adds WebSocket routes for WebRTC signaling.
     *
     * @param javalin The Javalin instance.
     */
    public static void configure(Javalin javalin) {
        javalin.ws("/rtc", cfg -> {
            cfg.onError(ctx -> PeerSignaling.getLogger().warn("Error encountered with signaling.", ctx.error()));
            cfg.onConnect(ctx -> {
                PeerSignaling.getSessions().add(ctx.session);
                ctx.session.setIdleTimeout(Duration.ZERO);
            });
            cfg.onClose(ctx -> PeerSignaling.getSessions().remove(ctx.session));
            cfg.onMessage(ctx -> {
                var message = ctx.message();
                PeerSignaling.getSessions().forEach(session -> {
                    if (session.isOpen()) try {
                        session.getRemote().sendString(message);
                    } catch (Exception exception) {
                        PeerSignaling.getLogger().warn("Failed to send message.", exception);
                    }
                });
            });
        });
    }
}
