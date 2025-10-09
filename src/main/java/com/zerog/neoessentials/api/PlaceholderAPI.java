package com.zerog.neoessentials.api;

import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * PlaceholderAPI provides a centralized system for registering and resolving placeholders.
 * This API allows NeoEssentials and other mods to register custom placeholders that can be
 * used in chat messages, join/quit messages, and other text formatting contexts.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Register a placeholder
 * PlaceholderAPI.registerPlaceholder("mymod_health", (player, params) -> {
 *     return String.valueOf((int) player.getHealth());
 * });
 * 
 * // Resolve placeholders in text
 * String result = PlaceholderAPI.setPlaceholders(player, "Health: {mymod_health}");
 * }</pre>
 */
public interface PlaceholderAPI {
    
    /**
     * Register a placeholder with the PlaceholderAPI system.
     * 
     * @param identifier The placeholder identifier (without braces, e.g., "player_name")
     * @param provider The provider that will resolve this placeholder
     * @return true if registered successfully, false if identifier already exists
     */
    static boolean registerPlaceholder(String identifier, PlaceholderProvider provider) {
        return PlaceholderManager.getInstance().registerPlaceholder(identifier, provider);
    }
    
    /**
     * Unregister a placeholder from the system.
     * 
     * @param identifier The placeholder identifier to remove
     * @return true if unregistered successfully, false if not found
     */
    static boolean unregisterPlaceholder(String identifier) {
        return PlaceholderManager.getInstance().unregisterPlaceholder(identifier);
    }
    
    /**
     * Resolve all placeholders in the given text for a specific player.
     * 
     * @param player The player context for placeholder resolution (can be null for server-wide placeholders)
     * @param text The text containing placeholders in the format {placeholder_name}
     * @return The text with all placeholders resolved
     */
    static String setPlaceholders(@Nullable ServerPlayer player, String text) {
        return PlaceholderManager.getInstance().setPlaceholders(player, text);
    }
    
    /**
     * Resolve a single placeholder for a specific player.
     * 
     * @param player The player context for placeholder resolution (can be null)
     * @param identifier The placeholder identifier (without braces)
     * @param params Optional parameters for the placeholder
     * @return The resolved placeholder value, or null if not found
     */
    @Nullable
    static String getPlaceholderValue(@Nullable ServerPlayer player, String identifier, @Nullable String params) {
        return PlaceholderManager.getInstance().getPlaceholderValue(player, identifier, params);
    }
    
    /**
     * Check if a placeholder is registered.
     * 
     * @param identifier The placeholder identifier to check
     * @return true if the placeholder is registered
     */
    static boolean isPlaceholderRegistered(String identifier) {
        return PlaceholderManager.getInstance().isPlaceholderRegistered(identifier);
    }
    
    /**
     * Get all registered placeholder identifiers.
     * 
     * @return A set of all registered placeholder identifiers
     */
    static Set<String> getRegisteredPlaceholders() {
        return PlaceholderManager.getInstance().getRegisteredPlaceholders();
    }
    
    /**
     * Register a placeholder expansion that can provide multiple placeholders.
     * Useful for mods that want to register many related placeholders at once.
     * 
     * @param expansion The placeholder expansion to register
     * @return true if registered successfully
     */
    static boolean registerExpansion(PlaceholderExpansion expansion) {
        return PlaceholderManager.getInstance().registerExpansion(expansion);
    }
    
    /**
     * Unregister a placeholder expansion and all its placeholders.
     * 
     * @param expansion The placeholder expansion to unregister
     * @return true if unregistered successfully
     */
    static boolean unregisterExpansion(PlaceholderExpansion expansion) {
        return PlaceholderManager.getInstance().unregisterExpansion(expansion);
    }
}

/**
 * Interface for providing placeholder values.
 * Implementations should be thread-safe as they may be called from multiple threads.
 */
@FunctionalInterface
interface PlaceholderProvider {
    /**
     * Resolve a placeholder value for the given player and parameters.
     * 
     * @param player The player context (can be null for server-wide placeholders)
     * @param params Optional parameters for the placeholder (can be null)
     * @return The resolved placeholder value, or null if cannot be resolved
     */
    @Nullable
    String onRequest(@Nullable ServerPlayer player, @Nullable String params);
}

/**
 * Abstract base class for placeholder expansions.
 * Allows mods to register multiple related placeholders through a single expansion.
 */
abstract class PlaceholderExpansion {
    
    /**
     * Get the identifier for this expansion.
     * This is used as a prefix for all placeholders in this expansion.
     * 
     * @return The expansion identifier (e.g., "neoessentials", "mymod")
     */
    public abstract String getIdentifier();
    
    /**
     * Get the version of this expansion.
     * 
     * @return The expansion version
     */
    public abstract String getVersion();
    
    /**
     * Get the author of this expansion.
     * 
     * @return The expansion author
     */
    public abstract String getAuthor();
    
    /**
     * Called when a placeholder from this expansion is requested.
     * The placeholder identifier will be without the expansion prefix.
     * 
     * @param player The player context (can be null)
     * @param identifier The placeholder identifier without expansion prefix
     * @param params Optional parameters for the placeholder
     * @return The resolved placeholder value, or null if not handled
     */
    @Nullable
    public abstract String onPlaceholderRequest(@Nullable ServerPlayer player, String identifier, @Nullable String params);
    
    /**
     * Get all placeholder identifiers that this expansion provides.
     * These should not include the expansion prefix.
     * 
     * @return A set of placeholder identifiers
     */
    public abstract Set<String> getPlaceholders();
}