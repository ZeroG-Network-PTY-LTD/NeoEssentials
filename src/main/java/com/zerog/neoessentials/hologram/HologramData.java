package com.zerog.neoessentials.hologram;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/**
 * Persistent data for one hologram.
 */
public class HologramData {
    /** Admin-defined unique name (lower-case slug). */
    public String id = "";
    /** Dimension key, e.g. "minecraft:overworld". */
    public String world = "minecraft:overworld";
    public double x = 0, y = 64, z = 0;
    /** Ordered list of text lines (top to bottom). */
    public List<HologramLine> lines = new ArrayList<>();
    /**
     * Placeholder refresh interval in seconds.
     * 0 = static / never refresh.
     */
    public int refreshInterval = 5;
    /** Whether this hologram is currently visible. */
    public boolean visible = true;

    // ── Billboard & rotation ──────────────────────────────────────────────────
    /**
     * Billboard constraint applied to every Display.TextDisplay entity for this hologram.
     * 0 = FIXED, 1 = VERTICAL (face player horizontally), 2 = HORIZONTAL, 3 = CENTER (always face player).
     * Default is CENTER (3) so the text always faces the viewer.
     */
    public int billboardMode = 3;

    /**
     * Spin animation — rotates around the specified axis each animation tick.
     * Speed is in degrees-per-animation-tick (scheduler fires every ~50 ms ≈ 20 ticks/s).
     * Example: 3.0 ≈ 60°/s = one full rotation every ~6 seconds.
     */
    public boolean spinEnabled = false;
    public float spinSpeedDegrees = 3.0f;
    /**
     * Spin axis: "Y" (world-space vertical spin — useful with FIXED billboard),
     *            "Z" (roll spin visible to player — recommended with CENTER billboard).
     */
    public String spinAxis = "Y";

    /**
     * Hover / subtle bob animation. Moves the hologram smoothly up and down.
     * {@code hoverAmplitude} = peak displacement in blocks.
     * {@code hoverSpeedDegrees} = degrees-per-animation-tick of the sine wave.
     */
    public boolean hoverEnabled = false;
    public float hoverAmplitude  = 0.08f;
    public float hoverSpeedDegrees = 1.5f;

    // ── Transient state ───────────────────────────────────────────────────────
    public transient long   lastRefreshMs   = 0L;
    public transient List<UUID> entityUUIDs = new ArrayList<>();
    /** Current spin angle in degrees (runtime only, not persisted). */
    public transient float  currentSpinAngle = 0f;
    /** Current hover sine-wave phase in degrees (runtime only). */
    public transient float  hoverPhase       = 0f;

    // ── Helpers ───────────────────────────────────────────────────────────────
    public BlockPos blockPos() {
        return new BlockPos((int) x, (int) y, (int) z);
    }
    /** Base Y position for line at given index (0 = topmost, before hover offset). */
    public double lineY(int index) {
        return y + (lines.size() - 1 - index) * 0.3;
    }
    /** Y position for line at given index accounting for current hover offset. */
    public double lineYWithHover(int index) {
        if (!hoverEnabled) return lineY(index);
        double offset = hoverAmplitude * Math.sin(Math.toRadians(hoverPhase));
        return lineY(index) + offset;
    }
    public boolean needsRefresh(long nowMs) {
        if (refreshInterval <= 0) return false;
        return nowMs - lastRefreshMs >= refreshInterval * 1_000L;
    }
    /** Returns true if any per-tick animation (spin or hover) is active. */
    public boolean hasTickAnimation() {
        return spinEnabled || hoverEnabled || hasFrameAnimation();
    }
    private boolean hasFrameAnimation() {
        for (HologramLine l : lines) {
            if (!l.frames.isEmpty() && l.animFrameIntervalTicks > 0) return true;
        }
        return false;
    }
}