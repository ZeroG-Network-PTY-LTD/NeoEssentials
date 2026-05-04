package com.zerog.neoessentials.hologram;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
/**
 * Drives placeholder refresh and animation ticking for all active holograms.
 * Uses a single daemon thread; all entity mutations are marshalled back onto
 * the Minecraft server thread via {@code server.execute()}.
 */
public class HologramScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HologramScheduler.class);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NeoEssentials-HologramScheduler");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> refreshTask;
    private static ScheduledFuture<?> animTask;
    /** Start periodic refresh (every second) and animation ticking (every 50ms = 1 tick). */
    public static void start() {
        stop();
        refreshTask = EXECUTOR.scheduleAtFixedRate(HologramScheduler::runRefresh, 2, 1, TimeUnit.SECONDS);
        animTask    = EXECUTOR.scheduleAtFixedRate(HologramScheduler::runAnimation, 2000, 50, TimeUnit.MILLISECONDS);
        LOGGER.info("[Hologram] Scheduler started.");
    }
    public static void stop() {
        if (refreshTask != null) { refreshTask.cancel(false); refreshTask = null; }
        if (animTask    != null) { animTask.cancel(false);    animTask    = null; }
    }
    // ── Refresh (placeholder resolution) ─────────────────────────────────────
    private static void runRefresh() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long now = System.currentTimeMillis();
        for (HologramData data : HologramManager.getInstance().getAllHolograms()) {
            if (!data.visible) continue;
            if (!data.needsRefresh(now)) continue;
            server.execute(() -> {
                try {
                    ServerLevel level = getLevelForDimension(server, data.world);
                    if (level != null) {
                        HologramRenderer.refreshAllLines(data, level, null);
                    }
                } catch (Exception e) {
                    LOGGER.debug("[Hologram] refresh error for '{}': {}", data.id, e.getMessage());
                }
            });
        }
    }
    // ── Animation ticking ─────────────────────────────────────────────────────
    private static void runAnimation() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (HologramData data : HologramManager.getInstance().getAllHolograms()) {
            if (!data.visible) continue;
            boolean changed = false;
            for (int i = 0; i < data.lines.size(); i++) {
                if (data.lines.get(i).tickAnimation()) {
                    changed = true;
                }
            }
            if (!changed) continue;
            final HologramData fd = data;
            server.execute(() -> {
                try {
                    ServerLevel level = getLevelForDimension(server, fd.world);
                    if (level != null) {
                        for (int i = 0; i < fd.lines.size(); i++) {
                            if (!fd.lines.get(i).frames.isEmpty()) {
                                HologramRenderer.updateLineText(fd, i,
                                    HologramTextProcessor.processStatic(fd.lines.get(i).currentText()), level);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("[Hologram] animation error for '{}': {}", fd.id, e.getMessage());
                }
            });
        }
    }
    // ── Helper ────────────────────────────────────────────────────────────────
    private static ServerLevel getLevelForDimension(MinecraftServer server, String dimensionKey) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionKey)) return level;
        }
        return null;
    }
}
