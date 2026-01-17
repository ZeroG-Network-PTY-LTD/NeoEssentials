package com.zerog.neoessentials.integrations;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Central integration manager for external chat mods.
 * Provides hooks for mods like DiscordIntegration, ChatMods, etc.
 * Works within the NeoForge modded environment.
 */
public class ChatIntegrationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatIntegrationManager.class);
    private static final List<ChatIntegrationAdapter> adapters = new ArrayList<>();
    
    /**
     * Register a chat integration adapter for a NeoForge mod
     * @param adapter The adapter to register
     */
    public static void registerAdapter(ChatIntegrationAdapter adapter) {
        if (adapter != null && !adapters.contains(adapter)) {
            adapters.add(adapter);
            LOGGER.info("Registered chat mod integration adapter: {}", adapter.getName());
        }
    }
    
    /**
     * Unregister a chat integration adapter
     * @param adapter The adapter to unregister
     */
    public static void unregisterAdapter(ChatIntegrationAdapter adapter) {
        if (adapters.remove(adapter)) {
            LOGGER.info("Unregistered chat mod integration adapter: {}", adapter.getName());
        }
    }
    
    /**
     * Broadcast a player chat message event to all registered adapters
     * @param player The player sending the message
     * @param channel The channel name (e.g., "local", "global", "staff")
     * @param message The raw message content
     * @param formattedMessage The fully formatted message
     * @param discordChannelId Optional Discord channel ID (null = use default)
     */
    public static void broadcastPlayerChat(ServerPlayer player, String channel, String message, String formattedMessage, String discordChannelId) {
        for (ChatIntegrationAdapter adapter : adapters) {
            try {
                adapter.onPlayerChat(player, channel, message, formattedMessage, discordChannelId);
            } catch (Exception e) {
                LOGGER.error("Error in chat integration adapter {}: {}", adapter.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Broadcast a private message event to all registered adapters
     * @param sender The sender
     * @param recipient The recipient  
     * @param message The message content
     */
    public static void broadcastPrivateMessage(ServerPlayer sender, ServerPlayer recipient, String message) {
        for (ChatIntegrationAdapter adapter : adapters) {
            try {
                adapter.onPrivateMessage(sender, recipient, message);
            } catch (Exception e) {
                LOGGER.error("Error in chat integration adapter {}: {}", adapter.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Broadcast a player mute event to all registered adapters
     * @param player The muted player
     * @param reason The mute reason
     * @param isMuted Whether the player is being muted or unmuted
     */
    public static void broadcastMuteEvent(ServerPlayer player, String reason, boolean isMuted) {
        for (ChatIntegrationAdapter adapter : adapters) {
            try {
                adapter.onPlayerMute(player, reason, isMuted);
            } catch (Exception e) {
                LOGGER.error("Error in chat integration adapter {}: {}", adapter.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Broadcast an AFK status change to all registered adapters
     * @param player The player
     * @param isAfk Whether the player is AFK
     * @param reason The AFK reason (if any)
     */
    public static void broadcastAfkEvent(ServerPlayer player, boolean isAfk, String reason) {
        for (ChatIntegrationAdapter adapter : adapters) {
            try {
                adapter.onAfkStatusChange(player, isAfk, reason);
            } catch (Exception e) {
                LOGGER.error("Error in chat integration adapter {}: {}", adapter.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Broadcast a player join event to all registered adapters
     * @param player The joining player
     */
    public static void broadcastPlayerJoin(ServerPlayer player) {
        for (ChatIntegrationAdapter adapter : adapters) {
            try {
                adapter.onPlayerJoin(player);
            } catch (Exception e) {
                LOGGER.error("Error in chat integration adapter {}: {}", adapter.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Broadcast a player quit event to all registered adapters
     * @param player The quitting player
     */
    public static void broadcastPlayerQuit(ServerPlayer player) {
        for (ChatIntegrationAdapter adapter : adapters) {
            try {
                adapter.onPlayerQuit(player);
            } catch (Exception e) {
                LOGGER.error("Error in chat integration adapter {}: {}", adapter.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Get all registered adapters
     * @return List of registered adapters
     */
    public static List<ChatIntegrationAdapter> getAdapters() {
        return new ArrayList<>(adapters);
    }
    
    /**
     * Check if any integration adapters are registered
     * @return true if adapters are registered
     */
    public static boolean hasIntegrations() {
        return !adapters.isEmpty();
    }
    
    /**
     * Clear all registered adapters (used for shutdown)
     */
    public static void clearAdapters() {
        adapters.clear();
        LOGGER.info("Cleared all chat mod integration adapters");
    }
}