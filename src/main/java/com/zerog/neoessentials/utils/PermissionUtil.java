package com.zerog.neoessentials.utils;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.NeoEssentialsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for handling permission checks with integration for LuckPerms and FTB Ranks.
 * This provides a centralized way to check permissions throughout the mod.
 */
public class PermissionUtil {
    
    // Cache for permission results to reduce performance impact
    private static final Map<UUID, Map<String, PermissionResult>> permissionCache = new HashMap<>();
    // Cache expiration time in milliseconds (5 minutes)
    private static final long CACHE_EXPIRATION_TIME = 300000;
    // Cache last access time for cleanup
    private static final Map<UUID, Long> lastAccessTime = new HashMap<>();
    
    /**
     * Permission result with cache metadata
     */
    private static class PermissionResult {
        final boolean result;
        final long timestamp;
        
        PermissionResult(boolean result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_TIME;
        }
    }
    
    /**
     * Check if a command source has a specific permission.
     * Will check LuckPerms, FTB Ranks, and then fall back to config defaults.
     *
     * @param source The command source to check
     * @param permission The permission string to check
     * @return True if the source has the permission, false otherwise
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        // Operators always have permission
        if (source.hasPermission(2)) {
            return true;
        }
        
        // If not a player, deny permission
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        
        UUID uuid = player.getUUID();
        
        // Update last access time
        lastAccessTime.put(uuid, System.currentTimeMillis());
        
        // Check cache first
        Map<String, PermissionResult> playerCache = permissionCache.computeIfAbsent(uuid, k -> new HashMap<>());
        PermissionResult cachedResult = playerCache.get(permission);
        
        if (cachedResult != null && !cachedResult.isExpired()) {
            return cachedResult.result;
        }
        
        // Cache miss or expired, check actual permission
        boolean hasPermission = checkPermission(player, permission);
        
        // Cache the result
        playerCache.put(permission, new PermissionResult(hasPermission));
        
        // Cleanup cache if necessary (every ~100 checks)
        if (Math.random() < 0.01) {
            cleanupCache();
        }
        
        return hasPermission;
    }
    
    /**
     * Direct permission check for a player without caching
     * 
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if the player has the permission, false otherwise
     */
    private static boolean checkPermission(ServerPlayer player, String permission) {
        // Check LuckPerms
        boolean result = checkLuckPermsPermission(player, permission);
        
        // If not found in LuckPerms, try FTB Ranks
        if (!result) {
            result = checkFTBRanksPermission(player, permission);
        }
        
        // If no permission system gave a result, check default config
        if (!result) {
            result = checkDefaultPermission(permission);
        }
        
        return result;
    }
    
    /**
     * Check permission using LuckPerms API if available
     * 
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if permission is granted, false otherwise
     */
    private static boolean checkLuckPermsPermission(ServerPlayer player, String permission) {
        try {
            // Check if LuckPerms is loaded using reflection
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            
            // Get the LuckPerms API instance
            Object api = luckPermsClass.getMethod("get").invoke(null);
            
            // Get the user
            Class<?> apiClass = api.getClass();
            Object userManager = apiClass.getMethod("getUserManager").invoke(api);
            Object userObj = userManager.getClass().getMethod("getUser", UUID.class)
                .invoke(userManager, player.getUUID());
            
            if (userObj != null) {
                // Check permission
                Object result = userObj.getClass().getMethod("getCachedData").invoke(userObj);
                result = result.getClass().getMethod("getPermissionData").invoke(result);
                result = result.getClass().getMethod("checkPermission", String.class).invoke(result, permission);
                
                // Get the result value
                return (boolean) result.getClass().getMethod("asBoolean").invoke(result);
            }
        } catch (ClassNotFoundException e) {
            // LuckPerms not found, that's fine
            return false;
        } catch (Exception e) {
            // Something went wrong, log it
            NeoEssentials.LOGGER.error("Error checking LuckPerms permission", e);
        }
        
        return false;
    }
    
    /**
     * Check permission using FTB Ranks API if available
     * 
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if permission is granted, false otherwise
     */
    private static boolean checkFTBRanksPermission(ServerPlayer player, String permission) {
        try {
            // Check if FTB Ranks is loaded using reflection
            Class<?> ftbRanksClass = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            
            // Get the FTB Ranks API instance
            Object api = ftbRanksClass.getMethod("getInstance").invoke(null);
            
            // Check permission
            Object result = api.getClass()
                .getMethod("getPermissionValue", ServerPlayer.class, String.class)
                .invoke(api, player, permission);
            
            // Get the result value
            if (result != null) {
                // Check if the result has a getAsBoolean method
                try {
                    Object booleanValue = result.getClass().getMethod("getAsBoolean").invoke(result);
                    return (Boolean) booleanValue;
                } catch (NoSuchMethodException e) {
                    // Try alternative methods if getAsBoolean doesn't exist
                    try {
                        Object booleanValue = result.getClass().getMethod("booleanValue").invoke(result);
                        return (Boolean) booleanValue;
                    } catch (NoSuchMethodException ex) {
                        // Just return the string value converted to boolean
                        return Boolean.parseBoolean(result.toString());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // FTB Ranks not found, that's fine
            return false;
        } catch (Exception e) {
            // Something went wrong, log it
            NeoEssentials.LOGGER.error("Error checking FTB Ranks permission", e);
        }
        
        return false;
    }
    
    /**
     * Check if a permission should be granted by default when no permission system is found
     * 
     * @param permission The permission string to check
     * @return True if the permission should be granted by default, false otherwise
     */
    private static boolean checkDefaultPermission(String permission) {
        // Get the config instance from NeoEssentials
        NeoEssentialsConfig config = NeoEssentials.getInstance().getConfigManager().getConfig();
        
        // Default behavior - will be configurable in the future
        return config.defaultPermissions().getOrDefault(permission, true);
    }
    
    /**
     * Clean expired cache entries to prevent memory leaks
     */
    private static void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        
        // Remove players who haven't accessed permissions in 10 minutes
        lastAccessTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > CACHE_EXPIRATION_TIME * 2);
        
        // Remove those players from the permission cache too
        permissionCache.keySet().removeIf(uuid -> !lastAccessTime.containsKey(uuid));
        
        // For remaining players, clean up expired individual permissions
        permissionCache.values().forEach(playerCache ->
            playerCache.entrySet().removeIf(entry -> entry.getValue().isExpired()));
    }
    
    /**
     * Clear the permission cache for a specific player
     * 
     * @param player The player to clear cache for
     */
    public static void clearCache(ServerPlayer player) {
        permissionCache.remove(player.getUUID());
        lastAccessTime.remove(player.getUUID());
    }
    
    /**
     * Clear the entire permission cache
     */
    public static void clearAllCache() {
        permissionCache.clear();
        lastAccessTime.clear();
    }
}
