package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Handles multiple boss bars, progress, and colors
 */
public class BossBarManager {
    // Config-driven, animated, and per-group bossbar support
    private String defaultTitle = "%if:group=admin:&cAdmin Bar:Player Bar%";
    private float defaultProgress = 1.0f;
    private int defaultColor = 0; // 0 = default color, can be set per group

    // Show bossbar for all players with config-driven title/progress/color
    public void showAllBossBars(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            String title = placeholderManager.processPlaceholders(defaultTitle, player);
            float progress = defaultProgress;
            int color = defaultColor;
            showBossBar(player, "default", title, progress, color);
        }
    }
    /**
     * Custom event for boss bar updates (production-ready for NeoForge event bus)
     */
    public static class CustomBossBarEvent extends net.neoforged.bus.api.Event {
        private final ServerPlayer player;
        private final String title;
        private final float progress;
        private final int color;

        public CustomBossBarEvent(ServerPlayer player, String title, float progress, int color) {
            this.player = player;
            this.title = title;
            this.progress = progress;
            this.color = color;
        }

        public ServerPlayer getPlayer() { return player; }
        public String getTitle() { return title; }
        public float getProgress() { return progress; }
        public int getColor() { return color; }
    }
    // Map: player UUID -> Map of bossbar ID -> BossBarEntry
    private final Map<UUID, Map<String, BossBarEntry>> activeBars = new HashMap<>();
    private final com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();

    public void showBossBar(ServerPlayer player, String title, float progress, int color) {
        showBossBar(player, "default", title, progress, color);
    }

    public void showBossBar(ServerPlayer player, String id, String title, float progress, int color) {
        com.zerog.neoessentials.config.TablistConfig config = com.zerog.neoessentials.features.TabListManager.getInstance().config;
        if (config == null || !config.enableBossbar) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[BossBarManager] Bossbar is disabled in config, skipping showBossBar for " + player.getName().getString());
            return;
        }
        String displayName = com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player);
        net.minecraft.server.level.ServerBossEvent bossbar = new net.minecraft.server.level.ServerBossEvent(
            net.minecraft.network.chat.Component.literal(displayName),
            net.minecraft.world.BossEvent.BossBarColor.values()[color % net.minecraft.world.BossEvent.BossBarColor.values().length],
            net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS
        );
        bossbar.setVisible(true);
        bossbar.setProgress(progress);
        bossbar.addPlayer(player);
        activeBars.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
            .put(id, new BossBarEntry(id, displayName, progress, color, bossbar));
    com.zerog.neoessentials.util.DebugUtil.debugLog("[BossBarManager] Showed boss bar for " + player.getName().getString() + ": " + displayName + " (id: " + id + ", progress: " + progress + ", color: " + color + ")");
    }

    public void removeBossBar(ServerPlayer player) {
        // Remove all bossbars for player
        Map<String, BossBarEntry> bars = activeBars.remove(player.getUUID());
        if (bars != null) {
            for (BossBarEntry entry : bars.values()) {
                entry.bossbar.removePlayer(player);
                entry.bossbar.setVisible(false);
            }
        }
    com.zerog.neoessentials.util.DebugUtil.debugLog("[BossBarManager] Removed all boss bars for " + player.getName().getString());
    }

    public void removeBossBar(ServerPlayer player, String id) {
        Map<String, BossBarEntry> bars = activeBars.get(player.getUUID());
        if (bars != null) {
            BossBarEntry entry = bars.remove(id);
            if (entry != null) {
                entry.bossbar.removePlayer(player);
                entry.bossbar.setVisible(false);
                com.zerog.neoessentials.util.DebugUtil.debugLog("[BossBarManager] Removed boss bar for " + player.getName().getString() + " (id: " + id + ")");
            }
        }
    }

    public static class BossBarEntry {
        public String id;
        public String title;
        public float progress;
        public int color;
        public net.minecraft.server.level.ServerBossEvent bossbar;
        public BossBarEntry(String id, String title, float progress, int color, net.minecraft.server.level.ServerBossEvent bossbar) {
            this.id = id;
            this.title = title;
            this.progress = progress;
            this.color = color;
            this.bossbar = bossbar;
        }
    }
}
