package com.zerog.neoessentials.chat;

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
 * 
 * This handler intercepts chat messages and applies the configured
 * chat format template before broadcasting to other players.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ChatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHandler.class);
    
    /**
     * Handles server chat events and applies custom formatting.
     * Only applies custom formatting when chat-format is configured,
     * otherwise preserves vanilla <playername>: message format.
     * 
     * @param event The ServerChatEvent containing the chat message and player
     */
    // Per-player channel state (simple static map for now)
    private static final java.util.Map<java.util.UUID, String> playerChannelMap = new java.util.concurrent.ConcurrentHashMap<>();

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
                String trimmed = rawMessage.trim();
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
            String message = rawMessage;
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
                if (player.level() != null) {
                    world = player.level().dimension().location().getPath();
                }
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
                if (channel.equals("local")) {
                    // Local: send to players within radius
                    int radius = 100;
                    if (channelsConfig != null && channelsConfig.has("local")) {
                        var localObj = channelsConfig.getAsJsonObject("local");
                        if (localObj.has("radius")) radius = localObj.get("radius").getAsInt();
                    }
                    var playerPos = player.position();
                    for (ServerPlayer target : player.getServer().getPlayerList().getPlayers()) {
                        if (target.level().dimension().equals(player.level().dimension()) && target.position().distanceTo(playerPos) <= radius) {
                            target.sendSystemMessage(formattedMessage);
                        }
                    }
                    LOGGER.debug("[Local] {}: {}", playerName, message);
                } else if (channel.equals("staff")) {
                    // Staff: send to players with permission
                    String perm = "neoessentials.chat.staff";
                    if (channelsConfig != null && channelsConfig.has("staff")) {
                        var staffObj = channelsConfig.getAsJsonObject("staff");
                        if (staffObj.has("permission")) perm = staffObj.get("permission").getAsString();
                    }
                    for (ServerPlayer target : player.getServer().getPlayerList().getPlayers()) {
                        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(target.getUUID(), perm)) {
                            target.sendSystemMessage(formattedMessage);
                        }
                    }
                    LOGGER.debug("[Staff] {}: {}", playerName, message);
                } else {
                    // Global: send to all players
                    for (ServerPlayer target : player.getServer().getPlayerList().getPlayers()) {
                        target.sendSystemMessage(formattedMessage);
                    }
                    LOGGER.debug("[Global] {}: {}", playerName, message);
                }
            } // else: do not cancel event, let vanilla formatting happen

        } catch (Exception e) {
            LOGGER.error("Error handling chat event for player {}: {}", 
                event.getPlayer().getName().getString(), e.getMessage(), e);
            // Don't cancel the event on error - let vanilla handle it
        }
    }
}