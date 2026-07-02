package com.zerog.neoessentials.hologram;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents one line of a hologram.
 *
 * <p>The {@link #text} field may contain colour codes {@code &x} / {@code &#RRGGBB}
 * as well as placeholder tokens like {@code {neoessentials_server_online}}.
 *
 * <p>If {@link #frames} is non-empty the line cycles through those strings
 * instead of using {@link #text}. The animation advances every
 * {@link #animFrameIntervalTicks} server ticks.
 */
public class HologramLine {

    /** Unique identifier for this line (used for ordered updates). */
    public String lineId = UUID.randomUUID().toString();

    /**
     * Raw template text — may include {@code &} colour codes and
     * {@code {placeholder}} tokens.
     */
    public String text = "";

    /**
     * Optional animation frames. When non-empty the line cycles through
     * these strings (instead of {@link #text}) at {@link #animFrameIntervalTicks}.
     */
    public List<String> frames = new ArrayList<>();

    /**
     * How many ticks between animation frame advances.
     * 0 = no animation (use {@link #text} / first frame only).
     */
    public int animFrameIntervalTicks = 0;

    // ── Transient / runtime state ────────────────────────────────────────────

    /** Current frame index; not persisted. */
    public transient int currentFrame = 0;
    /** Tick counter for animation; not persisted. */
    public transient int animTickCount = 0;
    /**
     * Last resolved (post-placeholder) plain text, used by the scheduler's fast
     * animation tick to detect when an {@code {animation:NAME}} placeholder inside
     * {@link #text}/{@link #frames} has advanced to a new frame — since the raw
     * template string itself never changes for a placeholder-driven animation, only
     * what it resolves to. Not persisted.
     */
    public transient String lastResolvedText = null;

    // ── Constructors ─────────────────────────────────────────────────────────

    public HologramLine() {}

    public HologramLine(String text) {
        this.text = text != null ? text : "";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the currently-active raw template text (frame or static). */
    public String currentText() {
        if (!frames.isEmpty()) {
            int idx = Math.max(0, Math.min(currentFrame, frames.size() - 1));
            return frames.get(idx);
        }
        return text;
    }

    /** Advance animation by one tick. Returns {@code true} if the frame changed. */
    public boolean tickAnimation() {
        if (frames.isEmpty() || animFrameIntervalTicks <= 0) return false;
        animTickCount++;
        if (animTickCount >= animFrameIntervalTicks) {
            animTickCount = 0;
            currentFrame = (currentFrame + 1) % frames.size();
            return true;
        }
        return false;
    }

    /** Cheap pre-check for whether this line's raw template might reference an {@code {animation:NAME}} token. */
    public boolean mayContainAnimationPlaceholder() {
        String t = currentText();
        return t != null && t.toLowerCase().contains("{animation:");
    }
}
