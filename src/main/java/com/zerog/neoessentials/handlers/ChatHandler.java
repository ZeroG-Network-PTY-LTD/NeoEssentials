package com.zerog.neoessentials.handlers;

import com.zerog.neoessentials.chat.ChatFormatter;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.chat.MuteManager;
import com.zerog.neoessentials.api.ChatAPI;
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
     * 
     * @param event The ServerChatEvent containing the chat message and player
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            ServerPlayer player = event.getPlayer();
            String rawMessage = event.getRawText();
            
            // Check if player is muted
            if (MuteManager.isMuted(player)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cYou are currently muted and cannot send chat messages."));
                return;
            }
            
            // Get the ChatManager instance
            ChatManager chatManager = ChatAPI.getChatManager();
            if (chatManager == null) {
                LOGGER.warn("ChatManager not available, using default chat formatting");
                return;
            }
            
            // Get the configured chat format
            String chatFormat = chatManager.getChatFormat();
            
            // Format the message using our custom formatter
            Component formattedMessage = ChatFormatter.formatMessage(chatFormat, player, rawMessage);
            
            // Set the formatted message
            event.setMessage(formattedMessage);
            
            LOGGER.debug("Applied chat formatting for player {}: {} -> {}", 
                player.getName().getString(), rawMessage, formattedMessage.getString());
            
        } catch (Exception e) {
            LOGGER.error("Error handling chat event for player {}: {}", 
                event.getPlayer().getName().getString(), e.getMessage(), e);
            // Don't cancel the event on error - let vanilla handle it
        }
    }
}