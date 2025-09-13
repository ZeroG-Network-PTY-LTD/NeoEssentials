package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import com.zerog.neoessentials.tablist.TabUpdateOrchestrator;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

public class UIEventHandler {
    private final TabListManager tabListManager;
    private final PlaceholderManager placeholderManager;
    private final TabUpdateOrchestrator tabUpdateOrchestrator;

    public UIEventHandler(TabListManager tabListManager, PlaceholderManager placeholderManager) {
        this.tabListManager = tabListManager;
        this.placeholderManager = placeholderManager;
        // Get the TabUpdateOrchestrator instance to handle permission updates for HeaderFooterManager
        this.tabUpdateOrchestrator = com.zerog.neoessentials.NeoEssentials.getTabUpdateOrchestrator();
    }

    @SubscribeEvent
    public void onPermissionUpdate(com.zerog.neoessentials.features.PermissionUpdateEvent event) {
        com.zerog.neoessentials.util.DebugUtil.debugLog("[UIEventHandler] Permission update event received");
        ServerPlayer player = event.getPlayer();
        com.zerog.neoessentials.util.DebugUtil.debugLog("[UIEventHandler] onPermissionUpdate called for " + player.getName().getString() + " (UUID: " + player.getUUID() + ")");
        
        if (player != null) {
            // First try TabListManager (if enabled)
            if (tabListManager != null) {
                tabListManager.onPermissionChange(player);
            }
            
            // Also trigger HeaderFooterManager via TabUpdateOrchestrator for basic tablist functionality
            if (tabUpdateOrchestrator != null) {
                tabUpdateOrchestrator.onPermissionUpdate(player);
                com.zerog.neoessentials.util.DebugUtil.debugLog("[UIEventHandler] Triggered HeaderFooterManager update for permission change");
            }
        }
    }

    @SubscribeEvent
    private void onPlayerJoin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        // Use TabListManager's onPlayerJoin method which handles everything properly
        tabListManager.onPlayerJoin(player);
        
        // Also notify TabUpdateOrchestrator
        if (tabUpdateOrchestrator != null) {
            tabUpdateOrchestrator.onPlayerJoin(player);
        }
    }

    // Scoreboard and bossbar event handlers removed - keeping only tablist functionality
}
