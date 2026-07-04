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

            boolean textChanged = false;
            // Frame animation
            for (int i = 0; i < data.lines.size(); i++) {
                HologramLine line = data.lines.get(i);
                if (line.tickAnimation()) {
                    textChanged = true;
                }
                // {animation:NAME} placeholder tokens don't change the raw template text —
                // only what they resolve to — so tickAnimation() alone can't see them
                // advance. Re-resolve any line that might reference one and compare
                // against its last resolved value to detect a frame change.
                if (line.mayContainAnimationPlaceholder()) {
                    String resolved = HologramTextProcessor.processStatic(line.currentText()).getString();
                    if (!resolved.equals(line.lastResolvedText)) {
                        line.lastResolvedText = resolved;
                        textChanged = true;
                    }
                }
            }

            // Advance spin angle
            boolean spinChanged = false;
            if (data.spinEnabled) {
                data.currentSpinAngle = (data.currentSpinAngle + data.spinSpeedDegrees) % 360f;
                spinChanged = true;
            }

            // Advance hover phase
            boolean hoverChanged = false;
            if (data.hoverEnabled) {
                data.hoverPhase = (data.hoverPhase + data.hoverSpeedDegrees) % 360f;
                hoverChanged = true;
            }

            if (!textChanged && !spinChanged && !hoverChanged) continue;

            final HologramData fd       = data;
            final boolean       fText   = textChanged;
            final boolean       fMotion = spinChanged || hoverChanged;

            server.execute(() -> {
                try {
                    ServerLevel level = getLevelForDimension(server, fd.world);
                    if (level == null) return;

                    // Update rotation / position (spin + hover)
                    if (fMotion) {
                        HologramRenderer.updateRotationsAndPositions(fd, level);
                    }

                    // Update animated text frames — either the line's own frame list
                    // (/hologram addframes) or a plain-text line referencing {animation:NAME}.
                    if (fText) {
                        for (int i = 0; i < fd.lines.size(); i++) {
                            HologramLine line = fd.lines.get(i);
                            if (!line.frames.isEmpty() || line.mayContainAnimationPlaceholder()) {
                                HologramRenderer.updateLineText(fd, i,
                                    HologramTextProcessor.processStatic(line.currentText()), level);
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
            if (level.dimension().identifier().toString().equals(dimensionKey)) return level;
        }
        return null;
    }
}
