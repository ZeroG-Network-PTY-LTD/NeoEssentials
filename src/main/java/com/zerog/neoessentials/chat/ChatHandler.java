package com.zerog.neoessentials.chat;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.ChatDebugUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatHandler manages server chat events and applies formatting.
 * <p>
 * This handler intercepts chat messages and applies the configured
 * chat format template before broadcasting to other players.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ChatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHandler.class);
    
    /**
     * Handles server chat events and applies custom formatting.
     * Only applies custom formatting when chat-format is configured,
     * otherwise preserves vanilla playername: message format.
     */
    // Per-player channel state
    private static final java.util.Map<java.util.UUID, String> playerChannelMap = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Set the channel for a specific player
     */
    public static void setPlayerChannel(java.util.UUID playerUUID, String channel) {
        if (channel == null || channel.isEmpty()) {
            playerChannelMap.remove(playerUUID);
        } else {
            playerChannelMap.put(playerUUID, channel);
        }
    }

    /**
     * Get the current channel for a player
     */
    @SuppressWarnings("unused") // May be used by external systems
    public static String getPlayerChannel(java.util.UUID playerUUID) {
        return playerChannelMap.get(playerUUID);
    }

    /**
     * Clear channel for a player (revert to default)
     */
    @SuppressWarnings("unused") // May be used by external systems
    public static void clearPlayerChannel(java.util.UUID playerUUID) {
        playerChannelMap.remove(playerUUID);
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            ServerPlayer player = event.getPlayer();
            String rawMessage = event.getRawText();
            String playerName = player.getName().getString();

            // Check if player is muted
            boolean isMuted = MuteManager.isMuted(player);
            ChatDebugUtil.debug("ChatHandler - Checking mute for %s, result: %s", playerName, isMuted);
            if (isMuted) {
                event.setCanceled(true);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.chat.muted"));
                return;
            }

            // Phase 3: Apply anti-spam filters
            AntiSpamManager.FilterResult filterResult = AntiSpamManager.getInstance().filterMessage(player, rawMessage);
            if (!filterResult.allowed) {
                event.setCanceled(true);
                if (filterResult.denyReason != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(filterResult.denyReason));
                }
                return;
            }

            // Use filtered message (may be modified by caps filter)
            String processedMessage = filterResult.filteredMessage != null ? filterResult.filteredMessage : rawMessage;

            // Enforce playerChatPermissions: block chat if player lacks any required permission
            ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
            if (chatManager != null) {
                java.util.Set<String> requiredPerms = chatManager.getPlayerChatPermissions();
                if (requiredPerms != null && !requiredPerms.isEmpty()) {
                    boolean hasAny = false;
                    for (String perm : requiredPerms) {
                        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), perm)) {
                            hasAny = true;
                            break;
                        }
                    }
                    if (!hasAny) {
                        event.setCanceled(true);
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.chat.no_permission"));
                        return;
                    }
                }
            }

            // Enforce muteCommands: block chat messages that start with a muted command
            if (chatManager != null) {
                String trimmed = processedMessage.trim();
                if (trimmed.startsWith("/")) {
                    String[] split = trimmed.substring(1).split(" ", 2);
                    String command = split[0].toLowerCase();
                    if (chatManager.isCommandMuted(command)) {
                        event.setCanceled(true);
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.chat.command_muted", command));
                        return;
                    }
                }
            }

            // Get the ChatManager instance (already retrieved above)
            if (chatManager == null) {
                LOGGER.warn("ChatManager not available, using default chat formatting");
                return; // Let vanilla handle the chat
            }

            // Load channel config from chat config (assume loaded as JsonObject)
            com.google.gson.JsonObject mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            com.google.gson.JsonObject chatConfig = mainConfig.has("chat") ? mainConfig.getAsJsonObject("chat") : new com.google.gson.JsonObject();
            com.google.gson.JsonObject channelsConfig = chatConfig.has("channels") ? chatConfig.getAsJsonObject("channels") : null;

            // Check master switch for channels system
            boolean channelsEnabled = true; // Default to true for backwards compatibility
            if (channelsConfig != null && channelsConfig.has("enabled")) {
                channelsEnabled = channelsConfig.get("enabled").getAsBoolean();
            }

            if (channelsConfig == null || !channelsEnabled) {
                LOGGER.debug("Chat channels system disabled, using global chat");
                channelsConfig = null; // Treat as if no channels configured
            }

            // Detect channel by prefix or player state
            String message = processedMessage;
            String channel = null;
            // Check for explicit channel prefix (e.g. ! for global, @ for staff)
            if (channelsConfig != null) {
                for (String ch : channelsConfig.keySet()) {
                    // Skip metadata fields
                    if (ch.equals("enabled") || ch.endsWith("-description")) continue;

                    com.google.gson.JsonObject chObj = channelsConfig.getAsJsonObject(ch);
                    if (chObj.has("enabled") && !chObj.get("enabled").getAsBoolean()) continue;
                    String prefix = chObj.has("prefix") ? chObj.get("prefix").getAsString() : "";
                    if (!prefix.isEmpty() && message.startsWith(prefix)) {
                        channel = ch;
                        message = message.substring(prefix.length()).stripLeading();
                        break;
                    }
                }
            }
            // If not by prefix, check per-player channel state
            if (channel == null) {
                channel = playerChannelMap.getOrDefault(player.getUUID(), null);
            }
            // If still not set, use default (local if enabled, else global)
            if (channel == null && channelsConfig != null) {
                for (String ch : channelsConfig.keySet()) {
                    // Skip metadata fields
                    if (ch.equals("enabled") || ch.endsWith("-description")) continue;

                    com.google.gson.JsonObject chObj = channelsConfig.getAsJsonObject(ch);
                    if (chObj.has("enabled") && chObj.get("enabled").getAsBoolean() && chObj.has("default") && chObj.get("default").getAsBoolean()) {
                        channel = ch;
                        break;
                    }
                }
            }
            if (channel == null) channel = "global"; // fallback

            // Get group and world for per-group/world chat format
            String group = null;
            try {
                var permManager = com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
                if (permManager != null) {
                    var user = permManager.getUser(player.getUUID());
                    if (user != null && user.getGroup() != null) {
                        group = user.getGroup();
                    } else {
                        group = permManager.getDefaultGroup();
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Could not get group for player {}: {}", playerName, e.getMessage());
            }
            String world = null;
            try {
                @SuppressWarnings("resource") // Level is not closeable, warning is false positive
                var level = player.level();
                world = level.dimension().location().getPath();
            } catch (Exception e) {
                LOGGER.debug("Could not get world for player {}: {}", playerName, e.getMessage());
            }




            // Only apply custom chat formatting if enabled in config
            if (com.zerog.neoessentials.config.ConfigManager.isChatFormattingEnabled()) {
                // Get the configured chat format for group/world
                String chatFormat = chatManager.getChatFormat(group, world);
                // Cancel the original event to apply custom formatting
                event.setCanceled(true);
                // Format the message using our custom formatter
                Component formattedMessage = ChatFormatter.formatMessage(chatFormat, player, message);
                // Route message based on channel
                // Get channel config for dynamic routing
                JsonObject channelObj = null;
                if (channelsConfig != null && channelsConfig.has(channel)) {
                    channelObj = channelsConfig.getAsJsonObject(channel);
                }
                
                // Check if channel has radius (proximity-based)
                boolean hasRadius = channelObj != null && channelObj.has("radius");
                int radius = hasRadius ? channelObj.get("radius").getAsInt() : 0;
                
                // Check if channel has permission requirement
                String requiredPermission = null;
                if (channelObj != null && channelObj.has("permission")) {
                    requiredPermission = channelObj.get("permission").getAsString();
                }
                
                var server = player.getServer();
                @SuppressWarnings("ConstantConditions") // Defensive null check
                var playerList = server != null ? server.getPlayerList() : null;
                
                if (playerList != null) {
                    if (hasRadius) {
                        // Proximity-based channel (local, whisper, shout, etc.)
                        var playerPos = player.position();
                        @SuppressWarnings("resource") // Level is not closeable, warning is false positive
                        var playerLevel = player.level();
                        
                        for (ServerPlayer target : playerList.getPlayers()) {
                            // Check permission first if required
                            if (requiredPermission != null && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(target.getUUID(), requiredPermission)) {
                                continue;
                            }
                            
                            // Check proximity
                            @SuppressWarnings("resource") // Level is not closeable, warning is false positive
                            var targetLevel = target.level();
                            if (targetLevel.dimension().equals(playerLevel.dimension()) && target.position().distanceTo(playerPos) <= radius) {
                                target.sendSystemMessage(formattedMessage);
                            }
                        }
                        LOGGER.debug("[{}] (radius: {}) {}: {}", channel, radius, playerName, message);
                    } else if (requiredPermission != null) {
                        // Permission-based channel (staff, admin, donor, etc.)
                        for (ServerPlayer target : playerList.getPlayers()) {
                            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(target.getUUID(), requiredPermission)) {
                                target.sendSystemMessage(formattedMessage);
                            }
                        }
                        LOGGER.debug("[{}] (permission: {}) {}: {}", channel, requiredPermission, playerName, message);
                    } else {
                        // Global channel (no radius, no permission)
                        for (ServerPlayer target : playerList.getPlayers()) {
                            target.sendSystemMessage(formattedMessage);
                        }
                        LOGGER.debug("[{}] (global) {}: {}", channel, playerName, message);
                    }
                }

                // Send message to Discord integration (if available and enabled for this channel)
                try {
                    // Check if Discord relay is enabled for this channel
                    boolean discordEnabled = false;
                    String discordChannelId = null;

                    if (channelObj != null && channelObj.has("discord")) {
                        com.google.gson.JsonObject discordConfig = channelObj.getAsJsonObject("discord");
                        if (discordConfig.has("enabled")) {
                            discordEnabled = discordConfig.get("enabled").getAsBoolean();
                        }
                        if (discordEnabled && discordConfig.has("channelId")) {
                            discordChannelId = discordConfig.get("channelId").getAsString();
                            if (discordChannelId != null && discordChannelId.trim().isEmpty()) {
                                discordChannelId = null; // Treat empty string as null
                            }
                        }
                    }

                    // Only send to Discord if enabled for this channel
                    if (discordEnabled) {
                        String formattedMessageText = formattedMessage.getString();
                        com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastPlayerChat(
                            player, channel, message, formattedMessageText, discordChannelId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to send chat to Discord integration: {}", e.getMessage());
                }
            } // else: do not cancel event, let vanilla formatting happen

        } catch (Exception e) {
            LOGGER.error("Error handling chat event for player {}: {}", 
                event.getPlayer().getName().getString(), e.getMessage(), e);
            // Don't cancel the event on error - let vanilla handle it
        }
    }
}