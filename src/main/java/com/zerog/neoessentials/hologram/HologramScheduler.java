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
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
/**
 * Drives placeholder refresh and animation ticking for all active holograms.
 * All entity mutations are marshalled back onto the Minecraft server thread via
 * {@code server.execute()}.
 */
public class HologramScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HologramScheduler.class);
    // Two SEPARATE single-thread executors, not one shared between refresh and animation.
    // Both used to run on the same thread via scheduleAtFixedRate — fine when refresh work
    // is cheap, but PlaceholderManager.setPlaceholders()/RichTextFormatter processing inside
    // runRefresh() can take a non-trivial amount of time (external placeholder providers,
    // gradient/rainbow parsing, etc). On a single shared thread, ANY refresh cycle that runs
    // long enough delays the next animation tick behind it (scheduleAtFixedRate on one thread
    // never runs two tasks concurrently — a late task just runs back-to-back with the next
    // one once free), which shows up as the animation frame catching up in visible bursts
    // ("jumpy"/"slow") no matter how short the animation's own frameDuration is set — the
    // frame clock (AnimationManager) was always ticking correctly, delivery to the hologram
    // was what stalled. Splitting these onto independent threads removes that contention.
    private static final ScheduledExecutorService REFRESH_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NeoEssentials-HologramScheduler-Refresh");
        t.setDaemon(true);
        return t;
    });
    private static final ScheduledExecutorService ANIM_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NeoEssentials-HologramScheduler-Animation");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> refreshTask;
    private static ScheduledFuture<?> animTask;
    /** Start periodic refresh and animation ticking, at the tick rates configured under
     *  {@code hologram.pollIntervalTicks}/{@code animationInterval} in config.json (1 tick = 50ms;
     *  defaults 20/1, matching the previous hardcoded 1s/50ms behavior exactly).
     *
     *  <p>Uses {@code scheduleWithFixedDelay}, not {@code scheduleAtFixedRate} — a cycle that
     *  runs long should be skipped, not queued: {@code scheduleAtFixedRate} anchors to the
     *  ORIGINAL schedule and fires back-to-back to catch up once something delays it (a slow
     *  refresh cycle, a main-thread hiccup the queued {@code server.execute()} work is waiting
     *  on, etc.), which is exactly what a visible animation "jump" looks like.
     *  {@code scheduleWithFixedDelay} instead waits the full delay from when the PREVIOUS run
     *  finished — it can never build up a backlog to burst through, at the cost of drifting
     *  slightly off wall-clock cadence under sustained load, which is imperceptible for a
     *  text/spin animation and far preferable to bursting. */
    public static void start() {
        stop();
        long refreshMs = com.zerog.neoessentials.config.ConfigManager.getHologramPollIntervalTicks() * 50L;
        long animMs = com.zerog.neoessentials.config.ConfigManager.getHologramAnimationIntervalTicks() * 50L;
        refreshTask = REFRESH_EXECUTOR.scheduleWithFixedDelay(HologramScheduler::runRefresh, 2000, refreshMs, TimeUnit.MILLISECONDS);
        animTask    = ANIM_EXECUTOR.scheduleWithFixedDelay(HologramScheduler::runAnimation, 2000, animMs, TimeUnit.MILLISECONDS);
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[Hologram] Scheduler started (refresh every {}ms, animation every {}ms).", refreshMs, animMs);
    }
    public static void stop() {
        if (refreshTask != null) { refreshTask.cancel(false); refreshTask = null; }
        if (animTask    != null) { animTask.cancel(false);    animTask    = null; }
    }
    /** Restarts the scheduler so a changed {@code hologram.pollIntervalTicks}/{@code
     *  animationInterval} takes effect without a full server restart — used by {@code /neoe
     *  reload}. */
    public static void restart() {
        start();
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
                    NeoLog.warn(LOGGER, LogCategory.GENERAL, "[Hologram] refresh error for '{}': {}", data.id, e.getMessage());
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
                    NeoLog.warn(LOGGER, LogCategory.GENERAL, "[Hologram] animation error for '{}': {}", fd.id, e.getMessage());
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
