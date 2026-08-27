package com.zerog.neoessentials.api;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

    private static volatile PlaceholderManager instance;
    
    // Thread-safe collections for placeholder storage
    private final Map<String, PlaceholderProvider> placeholders = new ConcurrentHashMap<>();
    private final Map<String, PlaceholderExpansion> expansions = new ConcurrentHashMap<>();
    
    private PlaceholderManager() {
        NeoLog.info(LOGGER, LogCategory.GENERAL, "PlaceholderAPI Manager initialized");
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
    @SuppressWarnings("ClassEscapesDefinedScope") // PlaceholderProvider is part of public API
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
        NeoLog.debug(LOGGER, LogCategory.GENERAL, "Registered placeholder: {}", normalizedIdentifier);
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
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Unregistered placeholder: {}", normalizedIdentifier);
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
        StringBuilder result = new StringBuilder();

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
            
            // Resolve the placeholder - try internal first
            String value = getPlaceholderValue(player, identifier, params);
            
            // If not found internally, try external sources (LuckPerms, FTB Ranks, etc.)
            if (value == null && player != null) {
                value = resolveExternalPlaceholder(player, identifier);
            }

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
     * Resolve external placeholders from other mods/plugins.
     * Supports: LuckPerms, FTB Ranks, etc.
     */
    private String resolveExternalPlaceholder(ServerPlayer player, String identifier) {
        // Try LuckPerms placeholders
        String luckPermsValue = resolveLuckPermsPlaceholder(player, identifier);
        if (luckPermsValue != null) return luckPermsValue;

        // Try FTB Ranks placeholders
        return resolveFTBRanksPlaceholder(player, identifier);
    }

    /**
     * Resolve LuckPerms placeholders like {luckperms_prefix}, {luckperms_suffix}, etc.
     */
    private String resolveLuckPermsPlaceholder(ServerPlayer player, String identifier) {
        if (!identifier.startsWith("luckperms_")) {
            return null;
        }

        try {
            String permMeta = identifier.substring("luckperms_".length());

            return switch (permMeta) {
                case "prefix" -> getLuckPermsPrefix(player);
                case "suffix" -> getLuckPermsSuffix(player);
                case "group", "primary_group" -> getLuckPermsPrimaryGroup(player);
                case "displayname" -> getLuckPermsDisplayName(player);
                default -> null;
            };
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to resolve LuckPerms placeholder '{}': {}", identifier, e.getMessage());
        }

        return null;
    }

    /**
     * Get LuckPerms prefix from permission system.
     * Uses PermissionAPI which properly delegates to LuckPerms adapter.
     */
    private String getLuckPermsPrefix(ServerPlayer player) {
        try {
            return com.zerog.neoessentials.api.permissions.PermissionAPI.getPrefix(player.getUUID());
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Error getting LuckPerms prefix: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Get LuckPerms suffix from permission system.
     * Uses PermissionAPI which properly delegates to LuckPerms adapter.
     */
    private String getLuckPermsSuffix(ServerPlayer player) {
        try {
            return com.zerog.neoessentials.api.permissions.PermissionAPI.getSuffix(player.getUUID());
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Error getting LuckPerms suffix: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Get the player's primary group — via {@link com.zerog.neoessentials.api.permissions.PermissionAPI#getPrimaryGroup},
     * which checks the active external adapter (LuckPerms/FTB Ranks) first and falls back to the
     * internal manager. This used to go straight to {@code PermissionAPI.getManager()} (the
     * internal manager only), so {@code {luckperms_group}}/{@code {ftbranks_rank}} silently
     * returned an empty string whenever an external permission plugin was actually active —
     * exactly the scenario these placeholders exist for. Same class of bug already fixed for
     * {@code TablistManager}'s group/weight lookups.
     */
    private String getLuckPermsPrimaryGroup(ServerPlayer player) {
        try {
            String group = com.zerog.neoessentials.api.permissions.PermissionAPI.getPrimaryGroup(player.getUUID());
            if (group != null) return group;
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Error getting primary group: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Get LuckPerms display name (prefix + name + suffix).
     */
    private String getLuckPermsDisplayName(ServerPlayer player) {
        String prefix = getLuckPermsPrefix(player);
        String suffix = getLuckPermsSuffix(player);
        String name = player.getName().getString();
        return prefix + name + suffix;
    }

    /**
     * Resolve FTB Ranks placeholders like {ftbranks_prefix}, {ftbranks_suffix}, etc.
     */
    private String resolveFTBRanksPlaceholder(ServerPlayer player, String identifier) {
        if (!identifier.startsWith("ftbranks_")) {
            return null;
        }

        // FTB Ranks uses the same permission system as LuckPerms in NeoEssentials
        // So we can reuse the same logic with different prefix
        String ftbMeta = identifier.substring("ftbranks_".length());

        return switch (ftbMeta) {
            case "prefix" -> getLuckPermsPrefix(player); // Same source
            case "suffix" -> getLuckPermsSuffix(player); // Same source
            case "rank", "group" -> getLuckPermsPrimaryGroup(player); // Same source
            default -> null;
        };
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
    @SuppressWarnings("ClassEscapesDefinedScope") // PlaceholderExpansion is part of public API
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
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Registered placeholder expansion: {} v{} by {}", 
            normalizedIdentifier, expansion.getVersion(), expansion.getAuthor());
        return true;
    }
    
    /**
     * Unregister a placeholder expansion.
     * 
     * @param expansion The placeholder expansion to unregister
     * @return true if unregistered successfully
     */
    @SuppressWarnings("ClassEscapesDefinedScope") // PlaceholderExpansion is part of public API
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
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Unregistered placeholder expansion: {}", normalizedIdentifier);
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
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Cleared all placeholders and expansions");
    }
}