package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.api.PlaceholderAPI;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.integrations.ChatIntegrationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for custom player join and quit messages.
 * Manages displaying customized join/quit messages based on server configuration.
 */
public class PlayerJoinQuitHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerJoinQuitHandler.class);

    /**
     * Handles player join events and displays custom join messages.
     * This event fires when a player successfully joins the server.
     * 
     * @param event The PlayerLoggedInEvent containing the joining player
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        try {
            // Get the ChatManager instance
            ChatManager chatManager = ChatAPI.getChatManager();
            if (chatManager == null) {
                LOGGER.warn("ChatManager not available, using default join messages");
                return;
            }

            // Get custom join message from config
            String customJoinMessage = chatManager.getCustomJoinMessage();
            
            // Only apply custom message if configured (not "none")
            if (customJoinMessage != null && !customJoinMessage.equals("none") && !customJoinMessage.trim().isEmpty()) {
                // Cancel the default join message by setting it to null
                // Note: This doesn't cancel the event, just modifies the message
                
                // Format the custom message with placeholders using PlaceholderAPI
                String resolvedMessage = PlaceholderAPI.setPlaceholders(player, customJoinMessage);
                
                // Convert color codes and create component
                String coloredMessage = resolvedMessage.replaceAll("&([0-9a-fk-or])", "§$1");
                Component formattedMessage = Component.literal(coloredMessage);
                
                // Broadcast the custom join message to all players
                player.getServer().getPlayerList().broadcastSystemMessage(formattedMessage, false);
                
                LOGGER.debug("Displayed custom join message for player {}: {}", 
                    player.getName().getString(), formattedMessage.getString());
            } else {
                // Use default join message behavior
                LOGGER.debug("Using default join message for player {}", player.getName().getString());
            }

            // Notify chat integrations about the join
            ChatIntegrationManager.broadcastPlayerJoin(player);

        } catch (Exception e) {
            LOGGER.error("Error handling join event for player {}: {}", 
                player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Handles player quit events and displays custom quit messages.
     * This event fires when a player disconnects from the server.
     * 
     * @param event The PlayerLoggedOutEvent containing the leaving player
     */
    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        try {
            // Get the ChatManager instance
            ChatManager chatManager = ChatAPI.getChatManager();
            if (chatManager == null) {
                LOGGER.warn("ChatManager not available, using default quit messages");
                return;
            }

            // Get custom quit message from config
            String customQuitMessage = chatManager.getCustomQuitMessage();
            
            // Only apply custom message if configured (not "none")
            if (customQuitMessage != null && !customQuitMessage.equals("none") && !customQuitMessage.trim().isEmpty()) {
                // Format the custom message with placeholders using PlaceholderAPI
                String resolvedMessage = PlaceholderAPI.setPlaceholders(player, customQuitMessage);
                
                // Convert color codes and create component
                String coloredMessage = resolvedMessage.replaceAll("&([0-9a-fk-or])", "§$1");
                Component formattedMessage = Component.literal(coloredMessage);
                
                // Broadcast the custom quit message to all players
                player.getServer().getPlayerList().broadcastSystemMessage(formattedMessage, false);
                
                LOGGER.debug("Displayed custom quit message for player {}: {}", 
                    player.getName().getString(), formattedMessage.getString());
            } else {
                // Use default quit message behavior
                LOGGER.debug("Using default quit message for player {}", player.getName().getString());
            }

            // Notify chat integrations about the quit
            ChatIntegrationManager.broadcastPlayerQuit(player);

        } catch (Exception e) {
            LOGGER.error("Error handling quit event for player {}: {}", 
                player.getName().getString(), e.getMessage(), e);
        }
    }
}