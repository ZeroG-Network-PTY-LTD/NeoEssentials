package com.zerog.neoessentials.tablist;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Orchestrates tablist header/footer updates, event listeners, and scheduling.
 */

public class TabUpdateOrchestrator {
    public void refreshTablistForAll() {
        // SAFETY CHECK: Don't interfere if TabListManager is handling tablist via config layouts
        if (isTabListManagerActive()) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[TabUpdateOrchestrator] TabListManager config layouts active - stepping back to prevent conflicts");
            return;
        }
        
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            headerFooterManager.scheduleHeaderFooterUpdate(player);
        }
        com.zerog.neoessentials.util.DebugUtil.debugLog("[TabList] Forced refresh for all players");
    }
    
    /**
     * Check if TabListManager is handling tablist via config layouts
     */
    private boolean isTabListManagerActive() {
        try {
            var tablistManager = com.zerog.neoessentials.features.TabListManager.getInstance();
            return tablistManager != null && tablistManager.hasActiveConfigLayouts();
        } catch (Exception e) {
            return false;
        }
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
        // SAFETY CHECK: Don't interfere if TabListManager is handling tablist via config layouts
        if (isTabListManagerActive()) {
            return; // Quietly step back
        }
        
        animationScheduler.tick(now);
    }
    public void setHeaderTemplate(String[] lines, long intervalMs) {
        // SAFETY CHECK: Warn if TabListManager is active
        if (isTabListManagerActive()) {
            com.zerog.neoessentials.util.DebugUtil.warnLog("[TabUpdateOrchestrator] TabListManager config layouts active - header template may not be used");
        }
        headerFooterManager.setHeaderTemplate(lines, intervalMs);
    }
    public void setFooterTemplate(String[] lines, long intervalMs) {
        // SAFETY CHECK: Warn if TabListManager is active
        if (isTabListManagerActive()) {
            com.zerog.neoessentials.util.DebugUtil.warnLog("[TabUpdateOrchestrator] TabListManager config layouts active - footer template may not be used");
        }
        headerFooterManager.setFooterTemplate(lines, intervalMs);
    }
    public void registerPlaceholder(String id, double intervalSeconds, List<String> frames) {
        // Register as animated placeholder in PlaceholderManager
        placeholderManager.registerPlaceholder(id, new com.zerog.neoessentials.placeholders.PlaceholderManager.AnimatedPlaceholder(frames, intervalSeconds));
    }
}
