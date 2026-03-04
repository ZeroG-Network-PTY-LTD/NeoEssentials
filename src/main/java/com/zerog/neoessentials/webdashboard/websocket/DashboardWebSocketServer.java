package com.zerog.neoessentials.webdashboard.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket server for real-time dashboard updates.
 * Extends org.java_websocket.server.WebSocketServer (bundled via JarJar).
 * Supports channel subscriptions, session auth, ping/pong, and rate limiting.
 */
public class DashboardWebSocketServer extends WebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardWebSocketServer.class);
    private static final Gson GSON = new GsonBuilder().create();

    // Rate limiting: minimum ms between messages per connection
    private static final long MESSAGE_COOLDOWN_MS = 100;

    private static DashboardWebSocketServer INSTANCE;

    // Per-connection state
    private final Map<WebSocket, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();
    private final Set<WebSocket> authenticatedClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<WebSocket, Long> lastMessageTime = new ConcurrentHashMap<>();

    private DashboardWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        setReuseAddr(true);
        LOGGER.info("WebSocket server created on port {}", port);
    }

    public static synchronized DashboardWebSocketServer getInstance(int port) {
        if (INSTANCE == null) {
            INSTANCE = new DashboardWebSocketServer(port);
        }
        return INSTANCE;
    }

    public static synchronized DashboardWebSocketServer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("WebSocket server not initialized. Call getInstance(port) first.");
        }
        return INSTANCE;
    }

    // ── WebSocketServer lifecycle ────────────────────────────────────────────

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clientSubscriptions.put(conn, ConcurrentHashMap.newKeySet());
        LOGGER.debug("WebSocket connection opened: {}", conn.getRemoteSocketAddress());

        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "welcome");
        welcome.addProperty("message", "Connected to NeoEssentials Dashboard");
        welcome.addProperty("timestamp", System.currentTimeMillis());
        sendToClient(conn, welcome);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clientSubscriptions.remove(conn);
        authenticatedClients.remove(conn);
        lastMessageTime.remove(conn);
        LOGGER.debug("WebSocket connection closed: {} (code={}, reason={})", conn.getRemoteSocketAddress(), code, reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Rate limiting
        long now = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(conn);
        if (lastTime != null && (now - lastTime) < MESSAGE_COOLDOWN_MS) {
            sendError(conn, "Rate limit exceeded. Please slow down.");
            return;
        }
        lastMessageTime.put(conn, now);

        try {
            JsonObject msg = GSON.fromJson(message, JsonObject.class);
            String type = msg.has("type") ? msg.get("type").getAsString() : "";

            switch (type) {
                case "authenticate" -> handleAuthenticate(conn, msg);
                case "subscribe"    -> handleSubscribe(conn, msg);
                case "unsubscribe"  -> handleUnsubscribe(conn, msg);
                case "ping"         -> handlePing(conn);
                default -> sendError(conn, "Unknown message type: " + type);
            }
        } catch (Exception e) {
            LOGGER.warn("Error processing WebSocket message from {}: {}", conn.getRemoteSocketAddress(), e.getMessage());
            sendError(conn, "Invalid message format");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("WebSocket error on {}: {}", conn != null ? conn.getRemoteSocketAddress() : "unknown", ex.getMessage());
    }

    @Override
    public void onStart() {
        LOGGER.info("WebSocket server started successfully on port {}", getPort());
        setConnectionLostTimeout(30);
    }

    // ── Message handlers ─────────────────────────────────────────────────────

    private void handleAuthenticate(WebSocket conn, JsonObject msg) {
        if (!msg.has("sessionId")) {
            sendError(conn, "Missing sessionId in authentication request");
            return;
        }
        String sessionId = msg.get("sessionId").getAsString();

        com.zerog.neoessentials.webdashboard.security.AuthenticationManager authManager =
            com.zerog.neoessentials.webdashboard.security.AuthenticationManager.getInstance();
        com.zerog.neoessentials.webdashboard.security.Session session =
            authManager.validateSession(sessionId);

        if (session == null) {
            JsonObject error = new JsonObject();
            error.addProperty("type", "auth_error");
            error.addProperty("message", "Invalid or expired session");
            sendToClient(conn, error);
            return;
        }

        authenticatedClients.add(conn);

        JsonObject response = new JsonObject();
        response.addProperty("type", "authenticated");
        response.addProperty("message", "Authentication successful");
        response.addProperty("username", session.getUsername());
        response.addProperty("role", session.getRole().name());
        response.addProperty("timestamp", System.currentTimeMillis());
        sendToClient(conn, response);

        LOGGER.debug("WebSocket client authenticated: {} ({})", session.getUsername(), conn.getRemoteSocketAddress());
    }

    private void handleSubscribe(WebSocket conn, JsonObject msg) {
        if (!authenticatedClients.contains(conn)) {
            sendError(conn, "Authentication required. Please authenticate first.");
            return;
        }
        if (!msg.has("channels")) {
            sendError(conn, "Missing 'channels' field");
            return;
        }
        Set<String> subs = clientSubscriptions.get(conn);
        msg.getAsJsonArray("channels").forEach(ch -> subs.add(ch.getAsString()));

        JsonObject response = new JsonObject();
        response.addProperty("type", "subscribed");
        response.add("channels", msg.get("channels"));
        sendToClient(conn, response);
    }

    private void handleUnsubscribe(WebSocket conn, JsonObject msg) {
        if (!msg.has("channels")) {
            sendError(conn, "Missing 'channels' field");
            return;
        }
        Set<String> subs = clientSubscriptions.getOrDefault(conn, Collections.emptySet());
        msg.getAsJsonArray("channels").forEach(ch -> subs.remove(ch.getAsString()));

        JsonObject response = new JsonObject();
        response.addProperty("type", "unsubscribed");
        response.add("channels", msg.get("channels"));
        sendToClient(conn, response);
    }

    private void handlePing(WebSocket conn) {
        JsonObject pong = new JsonObject();
        pong.addProperty("type", "pong");
        pong.addProperty("timestamp", System.currentTimeMillis());
        sendToClient(conn, pong);
    }

    // ── Broadcast helpers ────────────────────────────────────────────────────

    /**
     * Broadcast to all clients subscribed to a given channel.
     * Adds channel + timestamp to the payload automatically.
     */
    public void broadcast(String channel, JsonObject data) {
        data.addProperty("channel", channel);
        data.addProperty("timestamp", System.currentTimeMillis());
        String json = GSON.toJson(data);

        for (Map.Entry<WebSocket, Set<String>> entry : clientSubscriptions.entrySet()) {
            if (entry.getValue().contains(channel)) {
                WebSocket client = entry.getKey();
                if (client.isOpen()) {
                    client.send(json);
                }
            }
        }
    }

    /**
     * Broadcast to every connected (and open) client.
     */
    public void broadcastToAll(JsonObject data) {
        data.addProperty("timestamp", System.currentTimeMillis());
        String json = GSON.toJson(data);
        for (WebSocket conn : getConnections()) {
            if (conn.isOpen()) {
                conn.send(json);
            }
        }
    }

    /** Send a typed error message to a single client. */
    private void sendError(WebSocket conn, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("type", "error");
        error.addProperty("message", message);
        sendToClient(conn, error);
    }

    /** Send a JsonObject to a single client. */
    public void sendToClient(WebSocket conn, JsonObject data) {
        if (conn != null && conn.isOpen()) {
            data.addProperty("timestamp", System.currentTimeMillis());
            conn.send(GSON.toJson(data));
        }
    }

    // ── Stats ────────────────────────────────────────────────────────────────

    public int getClientCount() {
        return getConnections().size();
    }

    public int getAuthenticatedClientCount() {
        return authenticatedClients.size();
    }
}
