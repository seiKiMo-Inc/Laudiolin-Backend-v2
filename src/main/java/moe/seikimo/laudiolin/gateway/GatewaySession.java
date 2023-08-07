package moe.seikimo.laudiolin.gateway;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import lombok.Data;
import moe.seikimo.laudiolin.Config;
import moe.seikimo.laudiolin.models.OfflineUser;
import moe.seikimo.laudiolin.models.OnlineUser;
import moe.seikimo.laudiolin.models.data.TrackData;
import moe.seikimo.laudiolin.models.data.User;
import moe.seikimo.laudiolin.objects.DiscordPresence;
import moe.seikimo.laudiolin.objects.DiscordPresence.*;
import moe.seikimo.laudiolin.objects.JObject;
import moe.seikimo.laudiolin.objects.user.PresenceMode;
import moe.seikimo.laudiolin.objects.user.SocialStatus;
import moe.seikimo.laudiolin.utils.EncodingUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static moe.seikimo.laudiolin.gateway.Gateway.GATEWAY_INIT;
import static moe.seikimo.laudiolin.gateway.Gateway.GATEWAY_PING;

@Data
public final class GatewaySession {
    @NotNull private final Session session;
    private User user = null;

    // Internal gateway properties.
    private boolean initialized = false;
    private long lastPing = System.currentTimeMillis();

    // The user's broadcasting settings.
    private SocialStatus broadcastStatus = SocialStatus.EVERYONE;
    private PresenceMode broadcastPresence = PresenceMode.NONE;

    // The user's current player information.
    private float volume = 100;
    private float trackPosition = 0.0f;
    private boolean paused = true;
    @Nullable private TrackData trackData = null;
    @Nullable private Long startedListening = 0L;

    // The user's social properties.
    private long lastUpdateTime = 0;

    private List<GatewaySession> listeningAlong = new CopyOnWriteArrayList<>();
    @Nullable private GatewaySession listeningWith = null;

    /**
     * Attempts to disconnect the client.
     */
    public void disconnect() {
        this.session.close();
    }

    /**
     * Event method.
     * Fired when the client connects.
     */
    public void onConnect() {
        // Send the client a hello message.
        this.sendMessage(GATEWAY_INIT());
    }

    /**
     * Event method.
     * Fires when the client disconnects.
     */
    public void onDisconnect() {
        // Check if the client is listening along.
        if (this.getListeningWith() != null) {
            this.stopListening(false);
        }

        var listeners = this.getListeningAlong();
        if (!listeners.isEmpty()) {
            listeners.forEach(listener -> listener.stopListening(true));
        }

        // Clear the rich presence of the client.
        DiscordPresence.apply(this.getUser(), null);

        // Add the user as a recent/offline user.
        var userId = this.getUser().getUserId();
        var offline = Gateway.getOfflineUsers();
        if (!offline.containsKey(userId) &&
                this.getTrackData() != null) {
            var offlineUser = this.asOfflineUser();
            if (offlineUser != null) {
                offline.put(userId, offlineUser);
            }
        }

        // Remove the user from the online users.
        Gateway.getOnlineUsers().remove(userId);
        // Remove this user from the gateway.
        var list = Gateway.getUsers().get(userId);
        if (list != null) {
            list.remove(this);
        }
    }

    /**
     * Requests a latency update from the client.
     */
    public void pingClient() {
        // Calculate the existing latency.
        var latency = System.currentTimeMillis() - this.getLastPing();
        // Send the ping message.
        this.sendMessage(GATEWAY_PING(latency));
    }

    /**
     * Sets the client's listening status.
     * This will force a client track sync.
     *
     * @param session The session to listen with.
     */
    public void listenWith(GatewaySession session) {
        this.setListeningWith(session); // Set our own target.
        session.getListeningAlong().add(this); // Add ourselves to the target's list.

        this.syncWith(true); // Perform a sync.
    }

    /**
     * Stops listening along with the target user.
     *
     * @param host Was this a result of the host?
     */
    public void stopListening(boolean host) {
        var target = this.getListeningWith();
        if (target == null) return;

        // Remove ourselves from the target's list.
        target.getListeningAlong().remove(this);

        // Remove the target.
        this.setListeningWith(null);

        if (host) {
            // Send a null sync message.
            this.sendMessage(JObject.c()
                    .add("type", "sync")
                    .add("track", JsonNull.INSTANCE)
                    .add("progress", -1)
                    .add("paused", true));
        }
    }

    /**
     * Syncs the client's track information with the target.
     *
     * @param seek Should we seek to the target's position?
     */
    public void syncWith(boolean seek) {
        var target = this.getListeningWith();
        if (target == null) return;

        this.sendMessage(JObject.c()
                .add("type", "sync")
                .add("track", target.getTrackData())
                .add("progress", target.getTrackPosition())
                .add("paused", target.isPaused())
                .add("seek", seek));
    }

    /**
     * Updates the user's rich presence.
     */
    public void updatePresence() {
        // Check if the client should update.
        if (System.currentTimeMillis() -
                this.getLastUpdateTime() < 4e3) return;
        // Update the last update time.
        this.setLastUpdateTime(System.currentTimeMillis());

        // Check the presence mode.
        if (this.getBroadcastPresence() == PresenceMode.NONE) {
            // Clear the existing presence.
            DiscordPresence.apply(this.getUser(), null);
            return;
        }

        // Check if a track is currently playing.
        var track = this.getTrackData();
        if (track == null) {
            // Clear the existing presence.
            DiscordPresence.apply(this.getUser(), null);
            return;
        }

        // Get the start listening time.
        var started = this.getStartedListening();
        if (started == null) started = System.currentTimeMillis();

        // Build the presence.
        var config = Config.get();
        var clientId = config.discord.getDiscordClientId();
        var webTarget = config.getWebTarget();

        var assets = Assets.builder()
                .largeImage(track.getIcon())
                .largeText(track.getTitle())
                .smallImage("icon")
                .smallText("Laudiolin");
        var presence = DiscordPresence.builder()
                .platform(Platform.DESKTOP.getValue())
                .type(PresenceType.PLAYING.getValue())
                .id("laudiolin").name("Laudiolin")
                .applicationId(clientId)
                .details("Listening to " + track.getTitle())
                .state(track.getArtist())
                .timestamps(Timestamps.builder()
                        .start((int) (started / 1000f))
                        .end((int) ((started / 1000f) + track.getDuration()))
                        .build())
                .buttons(List.of(
                        Button.builder()
                                .label("Play on Laudiolin")
                                .url(webTarget + "/track/" + track.getId())
                                .build(),
                        Button.builder()
                                .label("Listen Along")
                                .url(webTarget + "/listen/" + this.getUser().getUserId())
                                .build()
                ));

        // Check if the simple rich presence should be used.
        if (this.getBroadcastPresence() == PresenceMode.LISTENING) {
            presence.type(PresenceType.LISTENING.getValue())
                    .details(track.getTitle());

            if (config.discord.isPresenceDetails()) {
                assets.largeText("Laudiolin");
                presence.id("spotify:1")
                        .name("Spotify")
                        .sessionId("4efa609dfa405bb70c0da334220d4a3f")
                        .party(Party.builder()
                                .id("spotify:852697865012117544")
                                .build())
                        .flags(48);
            }
        }

        // Set the presence.
        presence.assets(assets.build())
                .build().apply(this.getUser());
    }

    /**
     * Syncs listeners with the client.
     */
    public void updateListeners() {
        this.getListeningAlong().forEach(session ->
                session.syncWith(true));
    }

    /**
     * Updates the online status of the client.
     */
    public void updateOnlineStatus() {
        this.updateOnlineStatus(null);
    }

    /**
     * Updates the online status of the client.
     *
     * @param sync The sync position.
     */
    public void updateOnlineStatus(Float sync) {
        var userId = this.getUser().getUserId();
        var online = Gateway.getOnlineUsers().get(userId);
        if (online == null) {
            online = this.asOnlineUser();
        }

        // Check if the user is online.
        if (online == null) return;

        // Update the online status.
        online.setListeningTo(this.getTrackData());
        online.setProgress(sync == null ?
                this.getTrackPosition() : sync);
    }

    /**
     * @return The client as an online user.
     */
    public OnlineUser asOnlineUser() {
        var user = this.getUser();
        var info = user.publicInfo();
        if (info == null) return null;

        return OnlineUser.builder()
                .socialStatus(this.getBroadcastStatus())
                .username(info.getDisplayName())
                .userId(info.getUserId())
                .avatar(info.getIcon())
                .progress(this.getTrackPosition())
                .listeningTo(this.getTrackData())
                .build();
    }

    /**
     * @return The client as an offline user.
     */
    public OfflineUser asOfflineUser() {
        var user = this.getUser();
        var info = user.publicInfo();
        if (info == null) return null;

        return OfflineUser.builder()
                .socialStatus(this.getBroadcastStatus())
                .username(info.getDisplayName())
                .userId(info.getUserId())
                .avatar(info.getIcon())
                .lastSeen(System.currentTimeMillis())
                .lastListeningTo(this.getTrackData())
                .build();
    }

    /**
     * Attempts to send the client a message.
     * The message is JSON-encoded.
     *
     * @param message The message to send.
     */
    public void sendMessage(JObject message) {
        this.sendMessage(message.gson());
    }

    /**
     * Attempts to send the client a message.
     * The message is JSON-encoded.
     *
     * @param message The message to send.
     */
    public void sendMessage(JsonObject message) {
        this.sendMessage(EncodingUtils.jsonEncode(message));
    }

    /**
     * Attempts to send the client a message.
     *
     * @param data The message to send.
     */
    public void sendMessage(String data) {
        try {
            this.getSession().getRemote().sendString(data);
        } catch (Exception ignored) {
            this.getSession().close(); // Close the session.
        }
    }
}
