package com.zerog.neoessentials.integrations;

import net.minecraft.server.level.ServerPlayer;

/**
 * Interface for chat integration adapters.
 * Allows external NeoForge mods to hook into NeoEssentials chat events.
 */
public interface ChatIntegrationAdapter {
    
    /**
     * Get the name of this integration adapter
     * @return The adapter name
     */
    String getName();
    
    /**
     * Called when a private message is sent between players
     * @param sender The message sender
     * @param recipient The message recipient
     * @param message The message content
     */
    default void onPrivateMessage(ServerPlayer sender, ServerPlayer recipient, String message) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a player's mute status changes
     * @param player The affected player
     * @param reason The mute/unmute reason
     * @param isMuted true if being muted, false if being unmuted
     */
    default void onPlayerMute(ServerPlayer player, String reason, boolean isMuted) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a player's AFK status changes
     * @param player The affected player
     * @param isAfk true if going AFK, false if returning
     * @param reason The AFK reason (may be null)
     */
    default void onAfkStatusChange(ServerPlayer player, boolean isAfk, String reason) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a player joins the server
     * @param player The joining player
     */
    default void onPlayerJoin(ServerPlayer player) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a player quits the server
     * @param player The quitting player
     */
    default void onPlayerQuit(ServerPlayer player) {
        // Default implementation does nothing
    }
    
    /**
     * Called to check if this adapter is enabled and functional
     * @return true if the adapter is ready to receive events
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Called when the adapter should initialize itself
     * @return true if initialization was successful
     */
    default boolean initialize() {
        return true;
    }
    
    /**
     * Called when the adapter should clean up resources
     */
    default void shutdown() {
        // Default implementation does nothing
    }
}