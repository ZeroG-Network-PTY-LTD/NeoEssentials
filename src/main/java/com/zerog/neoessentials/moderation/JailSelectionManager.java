package com.zerog.neoessentials.moderation;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player pending jail-region selections made with the configurable jail wand item
 * (right-click = pos1, left-click = pos2 — WorldEdit-style), so {@code /setjail} can turn the
 * selection into a cuboid jail. Purely in-memory and per-session; a selection is not persisted
 * and is cleared once consumed by {@code /setjail}.
 */
public class JailSelectionManager {
    private static final JailSelectionManager INSTANCE = new JailSelectionManager();

    private static class Selection {
        BlockPos pos1;
        BlockPos pos2;
        String dimension;
    }

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    private JailSelectionManager() {}

    public static JailSelectionManager getInstance() {
        return INSTANCE;
    }

    public void setPos1(UUID playerId, BlockPos pos, String dimension) {
        Selection sel = selections.computeIfAbsent(playerId, id -> new Selection());
        sel.pos1 = pos;
        sel.dimension = dimension;
    }

    public void setPos2(UUID playerId, BlockPos pos, String dimension) {
        Selection sel = selections.computeIfAbsent(playerId, id -> new Selection());
        sel.pos2 = pos;
        sel.dimension = dimension;
    }

    public BlockPos getPos1(UUID playerId) {
        Selection sel = selections.get(playerId);
        return sel != null ? sel.pos1 : null;
    }

    public BlockPos getPos2(UUID playerId) {
        Selection sel = selections.get(playerId);
        return sel != null ? sel.pos2 : null;
    }

    /** The dimension the selection was made in, or {@code null} if no selection exists. */
    public String getDimension(UUID playerId) {
        Selection sel = selections.get(playerId);
        return sel != null ? sel.dimension : null;
    }

    /** Whether both corners of a cuboid selection have been set. */
    public boolean hasFullSelection(UUID playerId) {
        Selection sel = selections.get(playerId);
        return sel != null && sel.pos1 != null && sel.pos2 != null;
    }

    public void clear(UUID playerId) {
        selections.remove(playerId);
    }
}
