package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Handles multiple boss bars, progress, and colors
 */
public class BossBarManager {
    private final Map<UUID, BossBarEntry> activeBars = new HashMap<>();
    private final com.zerog.neoessentials.features.PlaceholderManager placeholderManager = new com.zerog.neoessentials.features.PlaceholderManager();

    public void showBossBar(ServerPlayer player, String title, float progress, int color) {
    String parsedTitle = placeholderManager.parse(player, title);
    activeBars.put(player.getUUID(), new BossBarEntry(parsedTitle, progress, color));
    // Placeholder for NeoForge boss bar integration
    // Replace this block with the correct packet/API call when available
    System.out.println("[BossBarManager] Would show boss bar for " + player.getName().getString() + ": " + parsedTitle + " (progress: " + progress + ", color: " + color + ")");
    }

    public void removeBossBar(ServerPlayer player) {
    activeBars.remove(player.getUUID());
    // Placeholder for NeoForge boss bar removal
    // Replace this block with the correct packet/API call when available
    System.out.println("[BossBarManager] Would remove boss bar for " + player.getName().getString());
    }

    public static class BossBarEntry {
        public String title;
        public float progress;
        public int color;
        public BossBarEntry(String title, float progress, int color) {
            this.title = title;
            this.progress = progress;
            this.color = color;
        }
    }
}
