package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

public class UIEventHandler {
    private final TabListManager tabListManager;
    private final PlaceholderManager placeholderManager;

    public UIEventHandler(TabListManager tabListManager, PlaceholderManager placeholderManager) {
        this.tabListManager = tabListManager;
        this.placeholderManager = placeholderManager;
    }

    @SubscribeEvent
    public void onPermissionUpdate(com.zerog.neoessentials.features.PermissionUpdateEvent event) {
        com.zerog.neoessentials.util.DebugUtil.debugLog("[UIEventHandler] Permission update event received");
        ServerPlayer player = event.getPlayer();
        com.zerog.neoessentials.util.DebugUtil.debugLog("[UIEventHandler] onPermissionUpdate called for " + player.getName().getString() + " (UUID: " + player.getUUID() + ")");
        
        if (player != null) {
            // Use new event-based update method instead of full refresh
            tabListManager.onPermissionChange(player);
        }
    }

    @SubscribeEvent
    private void onPlayerJoin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        // Use TabListManager's onPlayerJoin method which handles everything properly
        tabListManager.onPlayerJoin(player);
    }

    // Scoreboard and bossbar event handlers removed - keeping only tablist functionality
}
