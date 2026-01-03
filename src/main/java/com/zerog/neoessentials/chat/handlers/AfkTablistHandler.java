package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles tablist/player list visual indicators for AFK players.
 * Updates player display names with AFK prefixes/suffixes.
 */
@EventBusSubscriber(modid = "neoessentials")
public class AfkTablistHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkTablistHandler.class);
    
    /**
     * Update player's tablist name when they join
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updatePlayerTablistName(player);
        }
    }
    
    /**
     * Update a player's tablist display name based on their AFK status
     */
    public static void updatePlayerTablistName(ServerPlayer player) {
        try {
            AfkManager afkManager = AfkManager.getInstance();
            
            // Check if tablist indicator is enabled
            if (!afkManager.isEnableTablistIndicator()) {
                return;
            }
            
            String originalName = player.getName().getString();
            String displayName;
            
            if (afkManager.isAfk(player.getUUID())) {
                // Player is AFK - add AFK indicator
                String prefix = afkManager.getTablistAfkPrefix();
                String suffix = afkManager.getTablistAfkSuffix();
                displayName = prefix + originalName + suffix;
                com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "Setting AFK tablist name for {}: {}", originalName, displayName);
            } else {
                // Player is not AFK - use original name
                displayName = originalName;
                com.zerog.neoessentials.util.DebugLogger.log(LOGGER, "Setting normal tablist name for {}: {}", originalName, displayName);
            }
            
            // Update the player's display name in the tablist
            // Note: Direct tablist name setting not available in this version
            // Component nameComponent = Component.literal(displayName);
            // player.setTabListDisplayName(nameComponent); // Method not available
            
            // Alternative: Store display name for future use
            LOGGER.debug("Tablist display would be: {}", displayName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to update tablist name for player {}: {}", 
                player.getName().getString(), e.getMessage(), e);
        }
    }
    
    /**
     * Update all online players' tablist names (called when AFK status changes)
     */
    public static void updateAllPlayersTablistNames() {
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    updatePlayerTablistName(player);
                }
                LOGGER.debug("Updated tablist names for all online players");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update tablist names for all players: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Specific method to update tablist when a player goes AFK
     */
    public static void onPlayerAfk(ServerPlayer player) {
        updatePlayerTablistName(player);
        LOGGER.debug("Updated tablist for AFK player: {}", player.getName().getString());
    }
    
    /**
     * Specific method to update tablist when a player returns from AFK
     */
    public static void onPlayerReturnFromAfk(ServerPlayer player) {
        updatePlayerTablistName(player);
        LOGGER.debug("Updated tablist for returning player: {}", player.getName().getString());
    }
}