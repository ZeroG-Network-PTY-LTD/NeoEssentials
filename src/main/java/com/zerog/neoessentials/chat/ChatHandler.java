package com.zerog.neoessentials.chat;

import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.ChatDebugUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatHandler manages server chat events and applies formatting.
 * 
 * This handler intercepts chat messages and applies the configured
 * chat format template before broadcasting to other players.
 */
public class ChatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHandler.class);
    
    /**
     * Handles server chat events and applies custom formatting.
     * Only applies custom formatting when chat-format is configured,
     * otherwise preserves vanilla <playername>: message format.
     * 
     * @param event The ServerChatEvent containing the chat message and player
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            ServerPlayer player = event.getPlayer();
            String rawMessage = event.getRawText();
            
            // Check if player is muted
            String playerName = player.getName().getString();
            boolean isMuted = MuteManager.isMuted(player);
            ChatDebugUtil.debug("ChatHandler - Checking mute for %s, result: %s", playerName, isMuted);
            if (isMuted) {
                event.setCanceled(true);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.chat.muted"));
                return;
            }
            
            // Get the ChatManager instance
            ChatManager chatManager = ChatAPI.getChatManager();
            if (chatManager == null) {
                LOGGER.warn("ChatManager not available, using default chat formatting");
                return; // Let vanilla handle the chat
            }
            
            // Get the configured chat format
            String chatFormat = chatManager.getChatFormat();
            
            // Only apply custom formatting if a custom format is configured
            // Default format is "{neoessentials_displayname}: {MESSAGE}" which is essentially vanilla
            if (chatFormat != null && !chatFormat.equals("{neoessentials_displayname}: {MESSAGE}")) {
                // Cancel the original event to apply custom formatting
                event.setCanceled(true);
                
                // Format the message using our custom formatter
                Component formattedMessage = ChatFormatter.formatMessage(chatFormat, player, rawMessage);
                
                // Manually broadcast the formatted message to all players
                player.getServer().getPlayerList().broadcastSystemMessage(formattedMessage, false);
                
                LOGGER.debug("Applied custom chat formatting for player {}: {} -> {}", 
                    player.getName().getString(), rawMessage, formattedMessage.getString());
            }
            // If using default format, let vanilla handle it (preserves <playername>: message)
            
        } catch (Exception e) {
            LOGGER.error("Error handling chat event for player {}: {}", 
                event.getPlayer().getName().getString(), e.getMessage(), e);
            // Don't cancel the event on error - let vanilla handle it
        }
    }
}