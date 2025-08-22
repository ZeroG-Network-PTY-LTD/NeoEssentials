package com.zerog.neoessentials.features;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
// ...existing code...
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * EssentialsX-style Bossbar manager for NeoEssentials
 * Handles bossbar templates, display, removal, and animation stats
 */
public class CustomBossbarManager {
    // Track active bossbars per player
    private final Map<UUID, ServerBossEvent> activeBossbars = new HashMap<>();
    private static final CustomBossbarManager INSTANCE = new CustomBossbarManager();
    private final Set<String> templateNames = new HashSet<>();
    // ...existing code...

    public static CustomBossbarManager getInstance() {
        return INSTANCE;
    }

    public Set<String> getTemplateNames() {
        return Collections.unmodifiableSet(templateNames);
    }

    public void showBossbar(ServerPlayer player, String template, int durationSeconds) {
        // Example: Display a bossbar to a player using a template name
        BossbarTemplate tpl = getTemplate(template);
        if (tpl == null) return;
        ServerBossEvent bossbar = new ServerBossEvent(
            Component.literal(tpl.text),
            BossBarColor.values()[tpl.color],
            BossBarOverlay.values()[tpl.style]
        );
        bossbar.setVisible(true);
        bossbar.addPlayer(player);
        activeBossbars.put(player.getUUID(), bossbar);
    // To schedule removal after durationSeconds, use a tick event or scheduler from your mod framework.
    }

    public void removeBossbar(ServerPlayer player) {
        // Example: Remove bossbar from player
        ServerBossEvent bossbar = activeBossbars.remove(player.getUUID());
        if (bossbar != null) {
            bossbar.removePlayer(player);
            bossbar.setVisible(false);
        }
    }

    public void broadcastBossbar(String template, int durationSeconds) {
        // Example: Broadcast bossbar to all players
        BossbarTemplate tpl = getTemplate(template);
        if (tpl == null) return;
        ServerBossEvent bossbar = new ServerBossEvent(
            Component.literal(tpl.text),
            BossBarColor.values()[tpl.color],
            BossBarOverlay.values()[tpl.style]
        );
        bossbar.setVisible(true);
        for (ServerPlayer player : getAllOnlinePlayers()) {
            bossbar.addPlayer(player);
            activeBossbars.put(player.getUUID(), bossbar);
        }
    // To schedule removal after durationSeconds, use a tick event or scheduler from your mod framework.
    }

    public void updateBossbar(ServerPlayer player, String text, float progress) {
        // Example: Update bossbar text/progress for player
        ServerBossEvent bossbar = activeBossbars.get(player.getUUID());
        if (bossbar != null) {
            bossbar.setName(Component.literal(text));
            bossbar.setProgress(progress);
        }
    }

    public void reloadAnimations() {
        // Example: Reload bossbar animations/templates
    // Load bossbar templates from config/resource files
    // templateNames.clear();
    // templateNames.addAll(...);
    }

    public String getAnimationStats() {
        // Example: Return stats about bossbar animations
        return String.format("Bossbar templates loaded: %d", templateNames.size());
    }

    public List<String> getAvailableAnimations() {
        // Example: Return available bossbar template names
        return new ArrayList<>(templateNames);
    }

    public void shutdown() {
        // Example: Cleanup bossbars on shutdown
        for (ServerBossEvent bossbar : activeBossbars.values()) {
            bossbar.setVisible(false);
            bossbar.getPlayers().forEach(bossbar::removePlayer);
        }
        activeBossbars.clear();
    }
    // Helper to get all online players (replace with your mod's player list logic)
    private List<ServerPlayer> getAllOnlinePlayers() {
    // Replace with your mod's player list retrieval logic, e.g. MinecraftServer.getPlayerList().getPlayers()
    return new ArrayList<>();
    }
    // Helper to get a template by name
    private BossbarTemplate getTemplate(String name) {
        // Simulate lookup (replace with actual storage)
        for (BossbarTemplate tpl : getAllTemplates()) {
            if (tpl.name.equalsIgnoreCase(name)) return tpl;
        }
        return null;
    }

    // Simulate all templates (replace with actual config loading)
    private List<BossbarTemplate> getAllTemplates() {
        List<BossbarTemplate> templates = new ArrayList<>();
        for (String name : templateNames) {
            templates.add(new BossbarTemplate(name, "Bossbar: " + name, 0, 0));
        }
        return templates;
    }

    // Inner class for bossbar template (EssentialsX-style)
    public static class BossbarTemplate {
        public String name;
        public String text;
        public int color;
        public int style;
        // Add more fields as needed

        public BossbarTemplate(String name, String text, int color, int style) {
            this.name = name;
            this.text = text;
            this.color = color;
            this.style = style;
        }
    }
}
