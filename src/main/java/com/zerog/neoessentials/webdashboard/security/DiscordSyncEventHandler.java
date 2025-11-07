package com.zerog.neoessentials.webdashboard.security;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles automatic Discord permission synchronization on player join.
 * When a player with a verified Discord account joins, their permissions
 * are automatically synced based on their Discord roles.
 */
@EventBusSubscriber(modid = "neoessentials")
public class DiscordSyncEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordSyncEventHandler.class);
    
    /**
     * Called when a player joins the server
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            // Run sync asynchronously to avoid blocking player login
            new Thread(() -> syncPlayerPermissions(player), "DiscordPermissionSync-" + player.getName().getString()).start();
        }
    }
    
    /**
     * Sync permissions for a player
     */
    private static void syncPlayerPermissions(net.minecraft.server.level.ServerPlayer player) {
        try {
            // Small delay to ensure player is fully loaded
            Thread.sleep(1000);
            
            // Check if sync is enabled
            DiscordAuthConfig config = DiscordAuthConfig.load();
            if (!config.isPermissionSyncEnabled() || !config.isSyncOnJoin()) {
                return;
            }
            
            DiscordPermissionSync syncService = DiscordPermissionSync.getInstance();
            if (!syncService.isEnabled()) {
                return;
            }
            
            String playerName = player.getName().getString();
            LOGGER.debug("Starting Discord permission sync for player '{}'", playerName);
            
            // Sync permissions
            DiscordPermissionSync.SyncResult result = syncService.syncPlayerPermissions(player);
            
            if (result.isSuccess() && result.getPermissionsGranted() > 0) {
                // Notify player that permissions were synced
                player.sendSystemMessage(
                    Component.literal("✓ ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("Your permissions have been synced from Discord.")
                            .withStyle(ChatFormatting.GRAY))
                );
                
                LOGGER.info("Discord permission sync completed for '{}': {}", playerName, result.getMessage());
            } else if (!result.isSuccess()) {
                // Log failure but don't bother the player unless it's important
                LOGGER.debug("Discord permission sync skipped for '{}': {}", playerName, result.getMessage());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Discord permission sync interrupted for player '{}'", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error during Discord permission sync for player '{}': {}", 
                player.getName().getString(), e.getMessage(), e);
        }
    }
}
