package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-viewer tablist state for diffing and efficient updates.
 */
public class TabViewState {
    public Component lastSentHeader;
    public Component lastSentFooter;
    public Map<UUID, EntryState> lastEntries = new HashMap<>();
    public LayoutSnapshot layoutSnapshot;

    public static class EntryState {
        public String displayName;
        public String teamId;
        public int latencyBucket;
        public EntryState(String displayName, String teamId, int latencyBucket) {
            this.displayName = displayName;
            this.teamId = teamId;
            this.latencyBucket = latencyBucket;
        }
    }

    public static class LayoutSnapshot {
        public Map<Integer, UUID> slotToPlayer = new HashMap<>();
        // Add more fields as needed for custom slots, columns, etc.
    }
}
