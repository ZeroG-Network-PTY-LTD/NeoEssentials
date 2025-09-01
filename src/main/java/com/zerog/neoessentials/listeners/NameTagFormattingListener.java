
package com.zerog.neoessentials.listeners;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NameTagFormattingListener {
    // Track animated placeholders in name tags that need refresh
    private static final Map<UUID, String> playerNameFormats = new ConcurrentHashMap<>();
    private static final Map<String, java.util.concurrent.ScheduledFuture<?>> animatedNameTasks = new ConcurrentHashMap<>();
    private static final java.util.concurrent.ScheduledExecutorService nameScheduler = java.util.concurrent.Executors.newScheduledThreadPool(2);

    public NameTagFormattingListener() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        com.zerog.neoessentials.config.TablistConfig config = com.zerog.neoessentials.features.TabListManager.getInstance().config;
        if (config == null || !config.enableNametag) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[NameTagFormattingListener] Nametag is disabled in config, skipping display name for " + player.getName().getString());
            return;
        }
        String displayName;
        if (player instanceof ServerPlayer serverPlayer) {
            displayName = com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(serverPlayer);
            
            // Process placeholders in the display name
            String processedName = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance().processPlaceholders(displayName, serverPlayer);
            
            // Store the format for animated placeholder updates
            playerNameFormats.put(serverPlayer.getUUID(), displayName);
            
            // Schedule animated placeholder refresh for name tags
            scheduleAnimatedNameRefresh(displayName, serverPlayer);
            
            displayName = processedName;
        } else {
            displayName = player.getScoreboardName();
        }
        event.setDisplayname(com.zerog.neoessentials.util.ColorUtil.colorize(displayName));
    }

    @SubscribeEvent
    public void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerNameFormats.remove(player.getUUID());
            // Clean up any running animated tasks for this player
            String taskKey = "nametag_" + player.getUUID();
            java.util.concurrent.ScheduledFuture<?> future = animatedNameTasks.remove(taskKey);
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    /**
     * Schedules animated placeholder refresh for name tags containing animated placeholders
     */
    private static void scheduleAnimatedNameRefresh(String nameFormat, ServerPlayer player) {
        com.zerog.neoessentials.placeholders.PlaceholderManager pm = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
        java.util.Set<String> animatedPlaceholders = pm.getAnimatedPlaceholderIdsInText(nameFormat);
        
        if (!animatedPlaceholders.isEmpty()) {
            String taskKey = "nametag_" + player.getUUID();
            
            // Cancel existing task if any
            java.util.concurrent.ScheduledFuture<?> existingFuture = animatedNameTasks.get(taskKey);
            if (existingFuture != null) {
                existingFuture.cancel(false);
            }
            
            // Find the fastest refresh rate among all animated placeholders
            double minInterval = animatedPlaceholders.stream()
                .mapToDouble(pm::getAnimationInterval)
                .min()
                .orElse(1.0);
            
            java.util.concurrent.ScheduledFuture<?> future = nameScheduler.scheduleAtFixedRate(() -> {
                try {
                    net.minecraft.server.MinecraftServer server = player.getServer();
                    if (player.isRemoved() || server == null || !server.getPlayerList().getPlayers().contains(player)) {
                        // Player is no longer valid, cancel this task
                        animatedNameTasks.remove(taskKey);
                        return;
                    }
                    
                    String currentFormat = playerNameFormats.get(player.getUUID());
                    if (currentFormat != null) {
                        String refreshedName = pm.processPlaceholders(currentFormat, player);
                        // Update the player's display name
                        net.minecraft.network.chat.Component newDisplayName = com.zerog.neoessentials.util.ColorUtil.colorize(refreshedName);
                        
                        // Force a name update by refreshing the player's tab list entry
                        if (server != null) {
                            // Update custom name for name tag display
                            player.setCustomName(newDisplayName);
                            player.setCustomNameVisible(true);
                        }
                    }
                } catch (Exception e) {
                    com.zerog.neoessentials.util.DebugUtil.debugLog("Error refreshing animated name tag: " + e.getMessage());
                }
            }, (long)(minInterval * 1000), (long)(minInterval * 1000), java.util.concurrent.TimeUnit.MILLISECONDS);
            
            animatedNameTasks.put(taskKey, future);
        }
    }
}
