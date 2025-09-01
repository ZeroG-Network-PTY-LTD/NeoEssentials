package com.zerog.neoessentials.tablist;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Orchestrates tablist header/footer updates, event listeners, and scheduling.
 */

public class TabUpdateOrchestrator {
    public void refreshTablistForAll() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            headerFooterManager.scheduleHeaderFooterUpdate(player);
        }
        com.zerog.neoessentials.util.DebugUtil.debugLog("[TabList] Forced refresh for all players");
    }
    private final HeaderFooterManager headerFooterManager;
    private final com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager;
    private final AnimationScheduler animationScheduler;

    public TabUpdateOrchestrator(HeaderFooterManager headerFooterManager, com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager, AnimationScheduler animationScheduler) {
        this.headerFooterManager = headerFooterManager;
        this.placeholderManager = placeholderManager;
        this.animationScheduler = animationScheduler;
    }

    public void onPlayerJoin(ServerPlayer player) {
        headerFooterManager.onPlayerJoin(player);
        com.zerog.neoessentials.util.DebugUtil.debugLog("[TabList] Player joined: " + player.getName().getString());
    }
    public void onPlayerQuit(ServerPlayer player) {
        headerFooterManager.onPlayerQuit(player);
        com.zerog.neoessentials.util.DebugUtil.debugLog("[TabList] Player quit: " + player.getName().getString());
    }
    public void onPermissionUpdate(ServerPlayer player) {
        headerFooterManager.scheduleHeaderFooterUpdate(player);
        com.zerog.neoessentials.util.DebugUtil.debugLog("[TabList] Permission update for: " + player.getName().getString());
    }
    public void tick(long now) {
        animationScheduler.tick(now);
    }
    public void setHeaderTemplate(String[] lines, long intervalMs) {
        headerFooterManager.setHeaderTemplate(lines, intervalMs);
    }
    public void setFooterTemplate(String[] lines, long intervalMs) {
        headerFooterManager.setFooterTemplate(lines, intervalMs);
    }
    public void registerPlaceholder(String id, double intervalSeconds, List<String> frames) {
        // Register as animated placeholder in PlaceholderManager
        placeholderManager.registerPlaceholder(id, new com.zerog.neoessentials.placeholders.PlaceholderManager.AnimatedPlaceholder(frames, intervalSeconds));
    }
}
