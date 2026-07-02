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

    /**
     * When {@code true} this hologram responds to player interactions (right-click / left-click).
     * Used by {@link com.zerog.neoessentials.hologram.integration.ShopHologramManager} to gate
     * click-through shop transactions on the hologram.  Non-shop holograms leave this {@code false}
     * so accidental clicks pass through harmlessly.
     */
    public boolean interactive = false;

    // ── Visual appearance ─────────────────────────────────────────────────────
    /**
     * Uniform scale applied to every text entity in this hologram.
     * 1.0 = normal size; 0.5 = half-size; 2.0 = double-size.
     * Clamped to [0.1, 10.0] on spawn.
     */
    public float scale = 1.0f;

    /**
     * Vertical gap between lines, in blocks.
     * Default is 0.3 (≈ one text-line height).
     */
    public float lineSpacing = 0.3f;

    /**
     * Text shadow rendered behind each character (like vanilla chat).
     * Default is false for clean floating text.
     */
    public boolean textShadow = false;

    /**
     * Text opacity: 0 = fully transparent, 255 = fully opaque.
     * Default 255.
     */
    public int textOpacity = 255;

    /**
     * Background colour of the text panel (ARGB integer, e.g. 0x40000000 = 25% black).
     * 0x00000000 = fully transparent (default).
     */
    public int backgroundColorArgb = 0x00000000;

    /**
     * Text alignment applied to every line in this hologram.
     * 0 = CENTER (default), 1 = LEFT, 2 = RIGHT.
     */
    public int textAlign = 0;

    /**
     * Whether text renders through solid blocks (like a beacon beam effect).
     * Default is false.
     */
    public boolean seeThrough = false;

    /**
     * Maximum text width in pixels before the text wraps to the next line.
     * Default is 200 (vanilla TextDisplay default).
     * Set higher (e.g. 1000) to prevent wrapping for long lines.
     */
    public int lineWidth = 200;

    /**
     * How far away (in blocks) players can see this hologram.
     * Corresponds to the Display entity's view range multiplier.
     * Default is 1.0 (vanilla default ≈ 64 blocks).
     * Higher = visible further, lower = closer range only.
     */
    public float viewRange = 1.0f;

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
     * Spin axis: "Y" (world-space vertical spin — automatically uses player-tracking mode),
     *            "X" (pitch spin, visible with CENTER billboard),
     *            "Z" (roll spin visible to player — recommended with CENTER billboard).
     */
    public String spinAxis = "Y";

    /**
     * When {@code true} (the default) and {@code spinAxis} is {@code "Y"}, the renderer
     * switches the billboard to <b>FIXED</b> and computes the effective Y rotation as
     * {@code yaw_to_nearest_player + currentSpinAngle}.  This makes the hologram rotate
     * a full 360 degrees around its vertical axis while the "front face" always follows
     * the nearest player — so the text is readable once per revolution no matter where
     * the player stands.
     *
     * <p>Set to {@code false} to fall back to the raw LEFT_ROTATION Y spin (only useful
     * with a FIXED billboard set manually; CENTER/VERTICAL will cancel it out).
     */
    public boolean spinTrackPlayer = true;

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
    /**
     * UUID of this hologram's invisible {@code minecraft:interaction} hitbox entity,
     * or {@code null} if not spawned. Only created when {@link #interactive} is
     * {@code true} — vanilla {@code Display} entities never override
     * {@code Entity#isPickable()} (defaults to {@code false}), so a {@code Display.TextDisplay}
     * can never be targeted by interaction/attack raycasting on its own; the
     * {@code Interaction} entity is Mojang's own dedicated companion type for exactly
     * this purpose. Kept separate from {@link #entityUUIDs} (one per line) so line-count
     * bookkeeping elsewhere doesn't need to account for it. Not persisted.
     */
    public transient UUID interactionEntityUUID = null;
    /** Current spin angle in degrees (runtime only, not persisted). */
    public transient float  currentSpinAngle = 0f;
    /**
     * Yaw angle (degrees) from the hologram centre to the nearest tracked player
     * (runtime only, not persisted).  Refreshed each animation tick by
     * {@link HologramRenderer} when Y-axis tracking spin is active.
     */
    public transient float  spinPlayerYaw    = 0f;
    /** Current hover sine-wave phase in degrees (runtime only). */
    public transient float  hoverPhase       = 0f;

    // ── Helpers ───────────────────────────────────────────────────────────────
    public BlockPos blockPos() {
        return new BlockPos((int) x, (int) y, (int) z);
    }
    /** Base Y position for line at given index (0 = topmost, before hover offset). */
    public double lineY(int index) {
        return y + (lines.size() - 1 - index) * lineSpacing;
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
    /** Distance (2-D, XZ) from this hologram to the given world coordinates. */
    public double distanceXZ(double px, double pz) {
        double dx = x - px, dz = z - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }
}