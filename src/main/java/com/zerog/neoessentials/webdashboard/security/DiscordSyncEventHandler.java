package com.zerog.neoessentials.webdashboard.security;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
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
            // Run sync asynchronously (on a shared bounded pool, not a fresh Thread per join —
            // see DelayedTaskExecutor) to avoid blocking player login. 1s delay to ensure the
            // player is fully loaded, matching the previous Thread.sleep(1000) this replaced.
            com.zerog.neoessentials.util.DelayedTaskExecutor.schedule(
                () -> syncPlayerPermissions(player), 1000);
        }
    }

    /**
     * Sync permissions for a player
     */
    private static void syncPlayerPermissions(net.minecraft.server.level.ServerPlayer player) {
        try {
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
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Starting Discord permission sync for player '{}'", playerName);

            // Sync permissions
            DiscordPermissionSync.SyncResult result = syncService.syncPlayerPermissions(player);

            if (result.isSuccess() && result.getPermissionsGranted() > 0) {
                // Notify player that permissions were synced
                player.sendSystemMessage(
                    Component.literal(MessageUtil.localize("commands.neoessentials.discord.sync_success_icon"))
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(MessageUtil.localize("commands.neoessentials.discord.sync_success_message"))
                            .withStyle(ChatFormatting.GRAY))
                );

                NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Discord permission sync completed for '{}': {}", playerName, result.getMessage());
            } else if (!result.isSuccess()) {
                // Log failure but don't bother the player unless it's important
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Discord permission sync skipped for '{}': {}", playerName, result.getMessage());
            }

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error during Discord permission sync for player '" + player.getName().getString() + "'", e);
        }
    }
}
