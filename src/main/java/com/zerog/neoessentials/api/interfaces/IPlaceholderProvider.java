package com.zerog.neoessentials.api.interfaces;

import net.minecraft.server.level.ServerPlayer;
import java.util.function.Function;

/**
 * Placeholder provider interface for NeoEssentials API
 * Allows third-party plugins to register custom placeholders
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public interface IPlaceholderProvider {
    
    /**
     * Get the name of this placeholder provider
     * @return Provider name
     */
    String getProviderName();
    
    /**
     * Get the version of this placeholder provider
     * @return Provider version
     */
    String getProviderVersion();
    
    /**
     * Register a placeholder with this provider
     * @param identifier Placeholder identifier (without % or {})
     * @param resolver Function that resolves the placeholder value
     * @return true if registration was successful
     */
    boolean registerPlaceholder(String identifier, Function<PlaceholderContext, String> resolver);
    
    /**
     * Unregister a placeholder
     * @param identifier Placeholder identifier
     * @return true if unregistration was successful
     */
    boolean unregisterPlaceholder(String identifier);
    
    /**
     * Check if a placeholder is registered
     * @param identifier Placeholder identifier
     * @return true if placeholder is registered
     */
    boolean isPlaceholderRegistered(String identifier);
    
    /**
     * Process placeholders in text
     * @param text Text containing placeholders
     * @param context Placeholder context
     * @return Processed text with placeholders replaced
     */
    String processPlaceholders(String text, PlaceholderContext context);
    
    /**
     * Process placeholders in text for a specific player
     * @param text Text containing placeholders
     * @param player Target player
     * @return Processed text with placeholders replaced
     */
    String processPlaceholders(String text, ServerPlayer player);
    
    /**
     * Get all registered placeholder identifiers
     * @return Array of all registered placeholder identifiers
     */
    String[] getRegisteredPlaceholders();
    
    /**
     * Register an animated placeholder
     * @param identifier Placeholder identifier
     * @param frames Animation frames
     * @param intervalSeconds Interval between frames in seconds
     * @return true if registration was successful
     */
    boolean registerAnimatedPlaceholder(String identifier, String[] frames, double intervalSeconds);
    
    /**
     * Register a conditional placeholder
     * @param identifier Placeholder identifier
     * @param condition Condition to evaluate
     * @param trueValue Value when condition is true
     * @param falseValue Value when condition is false
     * @return true if registration was successful
     */
    boolean registerConditionalPlaceholder(String identifier, String condition, String trueValue, String falseValue);
    
    /**
     * Context for placeholder resolution
     */
    interface PlaceholderContext {
        
        /**
         * Get the target player
         * @return ServerPlayer instance, or null if no player context
         */
        ServerPlayer getPlayer();
        
        /**
         * Get custom context data
         * @param key Data key
         * @return Data value, or null if not found
         */
        Object getContextData(String key);
        
        /**
         * Set custom context data
         * @param key Data key
         * @param value Data value
         */
        void setContextData(String key, Object value);
        
        /**
         * Check if context data exists
         * @param key Data key
         * @return true if data exists
         */
        boolean hasContextData(String key);
        
        /**
         * Get the viewer player (for viewer-specific placeholders)
         * @return Viewer player, or same as getPlayer() if no viewer context
         */
        ServerPlayer getViewer();
        
        /**
         * Set the viewer player
         * @param viewer Viewer player
         */
        void setViewer(ServerPlayer viewer);
        
        /**
         * Get current timestamp
         * @return Current time in milliseconds
         */
        long getCurrentTime();
        
        /**
         * Create a new context for a different player
         * @param player Target player
         * @return New context instance
         */
        PlaceholderContext withPlayer(ServerPlayer player);
        
        /**
         * Create a new context with additional data
         * @param key Data key
         * @param value Data value
         * @return New context instance with additional data
         */
        PlaceholderContext withData(String key, Object value);
    }
}
