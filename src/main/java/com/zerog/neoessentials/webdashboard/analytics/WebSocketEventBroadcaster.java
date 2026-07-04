package com.zerog.neoessentials.webdashboard.analytics;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.webdashboard.websocket.DashboardWebSocketServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

/**
 * Broadcasts real-time game events to connected WebSocket dashboard clients.
 *
 * <p>WebSocket channels used:</p>
 * <ul>
 *   <li>{@code events}  — player join/leave/death/chat events</li>
 *   <li>{@code chat}    — raw chat messages (player name + text)</li>
 *   <li>{@code stats}   — periodic TPS/memory/player-count pulse</li>
 * </ul>
 *
 * <p>The periodic pulse is triggered by {@link #broadcastStatsPulse(MinecraftServer)}
 * which is called every minute from the {@code StatsEndpoint} sampler thread.</p>
 */
@EventBusSubscriber(modid = "neoessentials")
public class WebSocketEventBroadcaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventBroadcaster.class);

    // ── Periodic TPS / memory pulse ──────────────────────────────────────────

    /**
     * Broadcast a lightweight stats pulse to all clients subscribed to the
     * {@code "stats"} channel.  Called by the StatsEndpoint sampler (≈60 s cadence).
     */
    public static void broadcastStatsPulse(MinecraftServer server) {
        try {
            DashboardWebSocketServer ws = DashboardWebSocketServer.getInstance();
            if (ws.getClientCount() == 0) return;

            double avgMs = server.getAverageTickTimeNanos() / 1_000_000.0;
            double tps   = Math.min(20.0, 1000.0 / Math.max(50.0, avgMs));
            Runtime rt   = Runtime.getRuntime();
            long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long maxMb   = rt.maxMemory() / (1024 * 1024);

            JsonObject pulse = new JsonObject();
            pulse.addProperty("type",         "stats");
            pulse.addProperty("tps",          Math.round(tps * 10.0) / 10.0);
            pulse.addProperty("memUsedMb",    usedMb);
            pulse.addProperty("memMaxMb",     maxMb);
            pulse.addProperty("memPercent",   Math.round((double) usedMb / maxMb * 100.0));
            pulse.addProperty("players",      server.getPlayerCount());
            pulse.addProperty("playersMax",   server.getMaxPlayers());
            pulse.addProperty("uptimeMs",     ManagementFactory.getRuntimeMXBean().getUptime());
            ws.broadcast("stats", pulse);

        } catch (IllegalStateException e) {
            // WebSocket server not running yet — silently ignore
        } catch (Throwable e) {
            LOGGER.debug("[WSBroadcast] Stats pulse error: {}", e.getMessage());
        }
    }

    // ── Player join ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            String name = event.getEntity().getGameProfile().name();
            JsonObject msg = new JsonObject();
            msg.addProperty("type",    "event");
            msg.addProperty("event",   "player_join");
            msg.addProperty("player",  name);
            msg.addProperty("message", name + " joined the server");
            DashboardWebSocketServer.getInstance().broadcast("events", msg);
        } catch (Throwable ignored) {}
    }

    // ── Player leave ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        try {
            String name = event.getEntity().getGameProfile().name();
            JsonObject msg = new JsonObject();
            msg.addProperty("type",    "event");
            msg.addProperty("event",   "player_leave");
            msg.addProperty("player",  name);
            msg.addProperty("message", name + " left the server");
            DashboardWebSocketServer.getInstance().broadcast("events", msg);
        } catch (Throwable ignored) {}
    }

    // ── Server chat ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            if (event.isCanceled()) return;
            String name = event.getPlayer().getGameProfile().name();
            String text = event.getRawText();

            // Send to dedicated chat channel
            JsonObject chat = new JsonObject();
            chat.addProperty("type",    "chat");
            chat.addProperty("player",  name);
            chat.addProperty("message", text);
            DashboardWebSocketServer.getInstance().broadcast("chat", chat);

            // Also surface into the events feed
            JsonObject evt = new JsonObject();
            evt.addProperty("type",    "event");
            evt.addProperty("event",   "chat");
            evt.addProperty("player",  name);
            evt.addProperty("message", "<" + name + "> " + text);
            DashboardWebSocketServer.getInstance().broadcast("events", evt);
        } catch (Throwable ignored) {}
    }

    // ── Player death ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            String name     = player.getGameProfile().name();
            String deathMsg;
            try {
                deathMsg = event.getSource().getLocalizedDeathMessage(event.getEntity()).getString();
            } catch (Exception e2) {
                deathMsg = name + " died";
            }
            JsonObject msg = new JsonObject();
            msg.addProperty("type",    "event");
            msg.addProperty("event",   "death");
            msg.addProperty("player",  name);
            msg.addProperty("message", deathMsg);
            DashboardWebSocketServer.getInstance().broadcast("events", msg);
        } catch (Throwable ignored) {}
    }
}

