package com.zerog.neoessentials.api;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central manager for the PlaceholderAPI system.
 * Handles registration, resolution, and management of all placeholders.
 * This class is thread-safe and designed to be accessed from multiple threads.
 */
public class PlaceholderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderManager.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    private static volatile PlaceholderManager instance;
    
    // Thread-safe collections for placeholder storage
    private final Map<String, PlaceholderProvider> placeholders = new ConcurrentHashMap<>();
    private final Map<String, PlaceholderExpansion> expansions = new ConcurrentHashMap<>();
    
    private PlaceholderManager() {
        LOGGER.info("PlaceholderAPI Manager initialized");
    }
    
    /**
     * Get the singleton instance of PlaceholderManager.
     * 
     * @return The PlaceholderManager instance
     */
    public static PlaceholderManager getInstance() {
        if (instance == null) {
            synchronized (PlaceholderManager.class) {
                if (instance == null) {
                    instance = new PlaceholderManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a placeholder with the system.
     * 
     * @param identifier The placeholder identifier (without braces)
     * @param provider The provider that will resolve this placeholder
     * @return true if registered successfully, false if identifier already exists
     */
    public boolean registerPlaceholder(String identifier, PlaceholderProvider provider) {
        if (identifier == null || identifier.trim().isEmpty()) {
            LOGGER.warn("Attempted to register placeholder with null or empty identifier");
            return false;
        }
        
        if (provider == null) {
            LOGGER.warn("Attempted to register placeholder '{}' with null provider", identifier);
            return false;
        }
        
        String normalizedIdentifier = identifier.toLowerCase().trim();
        
        if (placeholders.containsKey(normalizedIdentifier)) {
            LOGGER.warn("Placeholder '{}' is already registered", normalizedIdentifier);
            return false;
        }
        
        placeholders.put(normalizedIdentifier, provider);
        LOGGER.debug("Registered placeholder: {}", normalizedIdentifier);
        return true;
    }
    
    /**
     * Unregister a placeholder from the system.
     * 
     * @param identifier The placeholder identifier to remove
     * @return true if unregistered successfully, false if not found
     */
    public boolean unregisterPlaceholder(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        
        String normalizedIdentifier = identifier.toLowerCase().trim();
        boolean removed = placeholders.remove(normalizedIdentifier) != null;
        
        if (removed) {
            LOGGER.debug("Unregistered placeholder: {}", normalizedIdentifier);
        }
        
        return removed;
    }
    
    /**
     * Resolve all placeholders in the given text for a specific player.
     * 
     * @param player The player context for placeholder resolution (can be null)
     * @param text The text containing placeholders in the format {placeholder_name}
     * @return The text with all placeholders resolved
     */
    public String setPlaceholders(@Nullable ServerPlayer player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0); // {placeholder_name}
            String placeholderContent = matcher.group(1); // placeholder_name
            
            // Parse placeholder and parameters
            String identifier;
            String params = null;
            
            int colonIndex = placeholderContent.indexOf(':');
            if (colonIndex != -1) {
                identifier = placeholderContent.substring(0, colonIndex);
                params = placeholderContent.substring(colonIndex + 1);
            } else {
                identifier = placeholderContent;
            }
            
            // Resolve the placeholder
            String value = getPlaceholderValue(player, identifier, params);
            
            if (value != null) {
                // Escape special regex characters in the replacement
                value = Matcher.quoteReplacement(value);
                matcher.appendReplacement(result, value);
            } else {
                // Keep the original placeholder if not found
                matcher.appendReplacement(result, Matcher.quoteReplacement(fullPlaceholder));
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
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
    public String getPlaceholderValue(@Nullable ServerPlayer player, String identifier, @Nullable String params) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        
        String normalizedIdentifier = identifier.toLowerCase().trim();
        
        // First check direct placeholder registrations
        PlaceholderProvider provider = placeholders.get(normalizedIdentifier);
        if (provider != null) {
            try {
                return provider.onRequest(player, params);
            } catch (Exception e) {
                LOGGER.error("Error resolving placeholder '{}': {}", normalizedIdentifier, e.getMessage(), e);
                return null;
            }
        }
        
        // Check expansions for prefixed placeholders (e.g., "neoessentials:player_name")
        int colonIndex = normalizedIdentifier.indexOf('_');
        if (colonIndex != -1) {
            String expansionId = normalizedIdentifier.substring(0, colonIndex);
            String placeholderName = normalizedIdentifier.substring(colonIndex + 1);
            
            PlaceholderExpansion expansion = expansions.get(expansionId);
            if (expansion != null) {
                try {
                    return expansion.onPlaceholderRequest(player, placeholderName, params);
                } catch (Exception e) {
                    LOGGER.error("Error resolving expansion placeholder '{}' from '{}': {}", 
                        placeholderName, expansionId, e.getMessage(), e);
                    return null;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if a placeholder is registered.
     * 
     * @param identifier The placeholder identifier to check
     * @return true if the placeholder is registered
     */
    public boolean isPlaceholderRegistered(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        
        String normalizedIdentifier = identifier.toLowerCase().trim();
        
        // Check direct placeholders
        if (placeholders.containsKey(normalizedIdentifier)) {
            return true;
        }
        
        // Check expansions
        int underscoreIndex = normalizedIdentifier.indexOf('_');
        if (underscoreIndex != -1) {
            String expansionId = normalizedIdentifier.substring(0, underscoreIndex);
            String placeholderName = normalizedIdentifier.substring(underscoreIndex + 1);
            
            PlaceholderExpansion expansion = expansions.get(expansionId);
            if (expansion != null) {
                return expansion.getPlaceholders().contains(placeholderName);
            }
        }
        
        return false;
    }
    
    /**
     * Get all registered placeholder identifiers.
     * 
     * @return A set of all registered placeholder identifiers
     */
    public Set<String> getRegisteredPlaceholders() {
        Set<String> result = new HashSet<>(placeholders.keySet());
        
        // Add expansion placeholders
        for (Map.Entry<String, PlaceholderExpansion> entry : expansions.entrySet()) {
            String expansionId = entry.getKey();
            PlaceholderExpansion expansion = entry.getValue();
            
            for (String placeholder : expansion.getPlaceholders()) {
                result.add(expansionId + "_" + placeholder);
            }
        }
        
        return Collections.unmodifiableSet(result);
    }
    
    /**
     * Register a placeholder expansion.
     * 
     * @param expansion The placeholder expansion to register
     * @return true if registered successfully
     */
    public boolean registerExpansion(PlaceholderExpansion expansion) {
        if (expansion == null) {
            LOGGER.warn("Attempted to register null expansion");
            return false;
        }
        
        String identifier = expansion.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            LOGGER.warn("Attempted to register expansion with null or empty identifier");
            return false;
        }
        
        String normalizedIdentifier = identifier.toLowerCase().trim();
        
        if (expansions.containsKey(normalizedIdentifier)) {
            LOGGER.warn("Expansion '{}' is already registered", normalizedIdentifier);
            return false;
        }
        
        expansions.put(normalizedIdentifier, expansion);
        LOGGER.info("Registered placeholder expansion: {} v{} by {}", 
            normalizedIdentifier, expansion.getVersion(), expansion.getAuthor());
        return true;
    }
    
    /**
     * Unregister a placeholder expansion.
     * 
     * @param expansion The placeholder expansion to unregister
     * @return true if unregistered successfully
     */
    public boolean unregisterExpansion(PlaceholderExpansion expansion) {
        if (expansion == null) {
            return false;
        }
        
        String identifier = expansion.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        
        String normalizedIdentifier = identifier.toLowerCase().trim();
        boolean removed = expansions.remove(normalizedIdentifier) != null;
        
        if (removed) {
            LOGGER.info("Unregistered placeholder expansion: {}", normalizedIdentifier);
        }
        
        return removed;
    }
    
    /**
     * Get statistics about the placeholder system.
     * 
     * @return A map containing placeholder system statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_placeholders", placeholders.size());
        stats.put("total_expansions", expansions.size());
        stats.put("registered_placeholders", getRegisteredPlaceholders().size());
        return Collections.unmodifiableMap(stats);
    }
    
    /**
     * Clear all registered placeholders and expansions.
     * This should only be used during shutdown or testing.
     */
    public void clear() {
        placeholders.clear();
        expansions.clear();
        LOGGER.info("Cleared all placeholders and expansions");
    }
}