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
    // Transient state
    public transient long lastRefreshMs = 0L;
    public transient List<UUID> entityUUIDs = new ArrayList<>();
    // Helpers
    public BlockPos blockPos() {
        return new BlockPos((int) x, (int) y, (int) z);
    }
    /** Y position for line at given index (0 = topmost). */
    public double lineY(int index) {
        return y + (lines.size() - 1 - index) * 0.3;
    }
    public boolean needsRefresh(long nowMs) {
        if (refreshInterval <= 0) return false;
        return nowMs - lastRefreshMs >= refreshInterval * 1_000L;
    }
}