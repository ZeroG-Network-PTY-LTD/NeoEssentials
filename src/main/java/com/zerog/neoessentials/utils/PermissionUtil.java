package com.zerog.neoessentials.utils;

import com.zerog.neoessentials.NeoEssentials;
<<<<<<< HEAD
import com.zerog.neoessentials.config.CompatNeoEssentialsConfig;
=======
import com.zerog.neoessentials.config.NeoEssentialsConfig;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
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
<<<<<<< HEAD
<<<<<<< HEAD
     */    public static boolean hasPermission(CommandSourceStack source, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        
        // Operators always have permission
        if (source.hasPermission(2)) {
            if (debug) {
                NeoEssentials.LOGGER.debug("Permission '{}' granted to operator {}", 
                    permission, source.getTextName());
            }
=======
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        // Operators always have permission
        if (source.hasPermission(2)) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
     */    public static boolean hasPermission(CommandSourceStack source, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        
        // Operators always have permission
        if (source.hasPermission(2)) {
            if (debug) {
                NeoEssentials.LOGGER.debug("Permission '{}' granted to operator {}", 
                    permission, source.getTextName());
            }
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
            return true;
        }
        
        // If not a player, deny permission
        if (!(source.getEntity() instanceof ServerPlayer player)) {
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
            if (debug) {
                NeoEssentials.LOGGER.debug("Permission '{}' denied for non-player source {}", 
                    permission, source.getTextName());
            }
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
            return false;
        }
        
        UUID uuid = player.getUUID();
<<<<<<< HEAD
<<<<<<< HEAD
        String playerName = player.getScoreboardName();
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        String playerName = player.getScoreboardName();
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
        
        // Update last access time
        lastAccessTime.put(uuid, System.currentTimeMillis());
        
        // Check cache first
        Map<String, PermissionResult> playerCache = permissionCache.computeIfAbsent(uuid, k -> new HashMap<>());
        PermissionResult cachedResult = playerCache.get(permission);
        
        if (cachedResult != null && !cachedResult.isExpired()) {
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
            if (debug) {
                NeoEssentials.LOGGER.debug("Using cached permission result for '{}': {} (player: {})", 
                    permission, cachedResult.result, playerName);
            }
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
            return cachedResult.result;
        }
        
        // Cache miss or expired, check actual permission
        boolean hasPermission = checkPermission(player, permission);
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
        // Log the permission check result
        if (debug) {
            NeoEssentials.LOGGER.debug("Permission check for '{}': {} (player: {})", 
                permission, hasPermission, playerName);
        }
        
<<<<<<< HEAD
        // Cache the result
        playerCache.put(permission, new PermissionResult(hasPermission));
        
        // Cleanup cache if necessary (every ~100 checks)
        if (Math.random() < 0.01) {
            cleanupCache();
        }
        
        return hasPermission;
    }
      /**
     * Check if a player has a specific permission.
     * Will check LuckPerms, FTB Ranks, and then fall back to config defaults.
     *
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if the player has the permission, false otherwise
     */
    public static boolean hasPermission(ServerPlayer player, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        
        // Operators always have permission
        if (player.hasPermissions(2)) {
            if (debug) {
                NeoEssentials.LOGGER.debug("Permission '{}' granted to operator {}", 
                    permission, player.getScoreboardName());
            }
            return true;
        }
        
        UUID uuid = player.getUUID();
        String playerName = player.getScoreboardName();
        
        // Update last access time
        lastAccessTime.put(uuid, System.currentTimeMillis());
        
        // Check cache first
        Map<String, PermissionResult> playerCache = permissionCache.computeIfAbsent(uuid, k -> new HashMap<>());
        PermissionResult cachedResult = playerCache.get(permission);
        
        if (cachedResult != null && !cachedResult.isExpired()) {
            if (debug) {
                NeoEssentials.LOGGER.debug("Using cached permission result for '{}': {} (player: {})", 
                    permission, cachedResult.result, playerName);
            }
            return cachedResult.result;
        }
        
        // Cache miss or expired, check actual permission
        boolean hasPermission = checkPermission(player, permission);
        
        // Log the permission check result
        if (debug) {
            NeoEssentials.LOGGER.debug("Permission check for '{}': {} (player: {})", 
                permission, hasPermission, playerName);
        }
        
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
        // Cache the result
        playerCache.put(permission, new PermissionResult(hasPermission));
        
        // Cleanup cache if necessary (every ~100 checks)
        if (Math.random() < 0.01) {
            cleanupCache();
        }
        
        return hasPermission;
    }
<<<<<<< HEAD
    
    /**
<<<<<<< HEAD
     * Check if a GameProfile has a specific permission.
     * This is a simplified check - since there's no active player, we can only check
     * for cached values or use LuckPerms (if available) with UUID lookup.
     *
     * @param profile The GameProfile to check
     * @param permission The permission string to check
     * @return True if the profile has the permission, false otherwise
     */
    public static boolean hasPermission(com.mojang.authlib.GameProfile profile, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        UUID uuid = profile.getId();
        String playerName = profile.getName();
        
        // Check cached permissions first
        Map<String, PermissionResult> playerCache = permissionCache.get(uuid);
        if (playerCache != null) {
            PermissionResult cachedResult = playerCache.get(permission);
            if (cachedResult != null && !cachedResult.isExpired()) {
                if (debug) {
                    NeoEssentials.LOGGER.debug("Using cached permission result for '{}': {} (profile: {})", 
                        permission, cachedResult.result, playerName);
                }
                return cachedResult.result;
            }
        }
        
        // Try LuckPerms
        boolean result = false;
        try {
            // Check if LuckPerms is loaded using reflection
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            
            // Get the LuckPerms API instance
            Object api = luckPermsClass.getMethod("get").invoke(null);
            
            // Get the user
            Class<?> apiClass = api.getClass();
            Object userManager = apiClass.getMethod("getUserManager").invoke(api);
            Object userObj = userManager.getClass().getMethod("getUser", UUID.class)
                .invoke(userManager, uuid);
            
            if (userObj != null) {
                // Check permission
                Object permResult = userObj.getClass().getMethod("getCachedData").invoke(userObj);
                permResult = permResult.getClass().getMethod("getPermissionData").invoke(permResult);
                permResult = permResult.getClass().getMethod("checkPermission", String.class)
                    .invoke(permResult, permission);
                
                // Get the result value
                result = (boolean) permResult.getClass().getMethod("asBoolean").invoke(permResult);
            }
        } catch (ClassNotFoundException e) {
            // LuckPerms not found, fall back to default
            result = checkDefaultPermission(permission);
        } catch (Exception e) {            // Something went wrong, log it
            NeoEssentials.LOGGER.error("Error checking LuckPerms permission for GameProfile", e);
            result = checkDefaultPermission(permission);
        }
        
        // Cache the result
        if (playerCache == null) {
            playerCache = new HashMap<>();
            permissionCache.put(uuid, playerCache);
        }
        playerCache.put(permission, new PermissionResult(result));
        
        // Debug logging if enabled
        if (NeoEssentials.getInstance().getConfigManager().getConfig().isDebug()) {
            NeoEssentials.LOGGER.debug("Permission check for '{}': {} (profile: {})", 
                permission, result, playerName);
        }
        
        return result;
    }
      /**
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
      /**
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
=======
     * Check if a player has a specific permission.
     * Will check LuckPerms, FTB Ranks, and then fall back to config defaults.
     *
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if the player has the permission, false otherwise
     */
    public static boolean hasPermission(ServerPlayer player, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        
        // Operators always have permission
        if (player.hasPermissions(2)) {
            if (debug) {
                NeoEssentials.LOGGER.debug("Permission '{}' granted to operator {}", 
                    permission, player.getScoreboardName());
            }
            return true;
        }
        
        UUID uuid = player.getUUID();
        String playerName = player.getScoreboardName();
        
        // Update last access time
        lastAccessTime.put(uuid, System.currentTimeMillis());
        
        // Check cache first
        Map<String, PermissionResult> playerCache = permissionCache.computeIfAbsent(uuid, k -> new HashMap<>());
        PermissionResult cachedResult = playerCache.get(permission);
        
        if (cachedResult != null && !cachedResult.isExpired()) {
            if (debug) {
                NeoEssentials.LOGGER.debug("Using cached permission result for '{}': {} (player: {})", 
                    permission, cachedResult.result, playerName);
            }
            return cachedResult.result;
        }
        
        // Cache miss or expired, check actual permission
        boolean hasPermission = checkPermission(player, permission);
        
        // Log the permission check result
        if (debug) {
            NeoEssentials.LOGGER.debug("Permission check for '{}': {} (player: {})", 
                permission, hasPermission, playerName);
        }
        
        // Cache the result
        playerCache.put(permission, new PermissionResult(hasPermission));
        
        // Cleanup cache if necessary (every ~100 checks)
        if (Math.random() < 0.01) {
            cleanupCache();
        }
        
        return hasPermission;
    }
    
    /**
     * Check if a GameProfile has a specific permission.
     * This is a simplified check - since there's no active player, we can only check
     * for cached values or use LuckPerms (if available) with UUID lookup.
     *
     * @param profile The GameProfile to check
     * @param permission The permission string to check
     * @return True if the profile has the permission, false otherwise
     */
    public static boolean hasPermission(com.mojang.authlib.GameProfile profile, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        UUID uuid = profile.getId();
        String playerName = profile.getName();
        
        // Check cached permissions first
        Map<String, PermissionResult> playerCache = permissionCache.get(uuid);
        if (playerCache != null) {
            PermissionResult cachedResult = playerCache.get(permission);
            if (cachedResult != null && !cachedResult.isExpired()) {
                if (debug) {
                    NeoEssentials.LOGGER.debug("Using cached permission result for '{}': {} (profile: {})", 
                        permission, cachedResult.result, playerName);
                }
                return cachedResult.result;
            }
        }
        
        // Try LuckPerms
        boolean result = false;
        try {
            // Check if LuckPerms is loaded using reflection
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            
            // Get the LuckPerms API instance
            Object api = luckPermsClass.getMethod("get").invoke(null);
            
            // Get the user
            Class<?> apiClass = api.getClass();
            Object userManager = apiClass.getMethod("getUserManager").invoke(api);
            Object userObj = userManager.getClass().getMethod("getUser", UUID.class)
                .invoke(userManager, uuid);
            
            if (userObj != null) {
                // Check permission
                Object permResult = userObj.getClass().getMethod("getCachedData").invoke(userObj);
                permResult = permResult.getClass().getMethod("getPermissionData").invoke(permResult);
                permResult = permResult.getClass().getMethod("checkPermission", String.class)
                    .invoke(permResult, permission);
                
                // Get the result value
                result = (boolean) permResult.getClass().getMethod("asBoolean").invoke(permResult);
            }
        } catch (ClassNotFoundException e) {
            // LuckPerms not found, fall back to default
            result = checkDefaultPermission(permission);
        } catch (Exception e) {            // Something went wrong, log it
            NeoEssentials.LOGGER.error("Error checking LuckPerms permission for GameProfile", e);
            result = checkDefaultPermission(permission);
        }
        
        // Cache the result
        if (playerCache == null) {
            playerCache = new HashMap<>();
            permissionCache.put(uuid, playerCache);
        }
        playerCache.put(permission, new PermissionResult(result));
        
        // Debug logging if enabled
        if (NeoEssentials.getInstance().getConfigManager().getConfig().isDebug()) {
            NeoEssentials.LOGGER.debug("Permission check for '{}': {} (profile: {})", 
                permission, result, playerName);
        }
        
        return result;
    }
      /**
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
     * Direct permission check for a player without caching
     * 
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if the player has the permission, false otherwise
     */
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
    static boolean checkPermission(ServerPlayer player, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        String playerName = player.getScoreboardName();
        
        // Check LuckPerms
        boolean result = checkLuckPermsPermission(player, permission);
        
        if (result && debug) {
            NeoEssentials.LOGGER.debug("LuckPerms granted permission '{}' to player {}", 
                permission, playerName);
            return true;
        }
        
        // If not found in LuckPerms, try FTB Ranks
        if (!result) {
            result = checkFTBRanksPermission(player, permission);
            
            if (result && debug) {
                NeoEssentials.LOGGER.debug("FTB Ranks granted permission '{}' to player {}", 
                    permission, playerName);
                return true;
            }
=======
    private static boolean checkPermission(ServerPlayer player, String permission) {
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        String playerName = player.getScoreboardName();
        
        // Check LuckPerms
        boolean result = checkLuckPermsPermission(player, permission);
        
        if (result && debug) {
            NeoEssentials.LOGGER.debug("LuckPerms granted permission '{}' to player {}", 
                permission, playerName);
            return true;
        }
        
        // If not found in LuckPerms, try FTB Ranks
        if (!result) {
            result = checkFTBRanksPermission(player, permission);
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            
            if (result && debug) {
                NeoEssentials.LOGGER.debug("FTB Ranks granted permission '{}' to player {}", 
                    permission, playerName);
                return true;
            }
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
        }
        
        // If no permission system gave a result, check default config
        if (!result) {
            result = checkDefaultPermission(permission);
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
            
            if (debug) {
                NeoEssentials.LOGGER.debug("Using default permission for '{}': {} (player: {})", 
                    permission, result, playerName);
            }
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
        }
        
        return result;
    }
<<<<<<< HEAD
<<<<<<< HEAD
      /**
=======
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
      /**
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
     * Check permission using LuckPerms API if available
     * 
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if permission is granted, false otherwise
     */
<<<<<<< HEAD
<<<<<<< HEAD
    static boolean checkLuckPermsPermission(ServerPlayer player, String permission) {
=======
    private static boolean checkLuckPermsPermission(ServerPlayer player, String permission) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    static boolean checkLuckPermsPermission(ServerPlayer player, String permission) {
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
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
<<<<<<< HEAD
<<<<<<< HEAD
      /**
=======
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
      /**
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
     * Check permission using FTB Ranks API if available
     * 
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if permission is granted, false otherwise
     */
<<<<<<< HEAD
<<<<<<< HEAD
    static boolean checkFTBRanksPermission(ServerPlayer player, String permission) {
=======
    private static boolean checkFTBRanksPermission(ServerPlayer player, String permission) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    static boolean checkFTBRanksPermission(ServerPlayer player, String permission) {
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
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
<<<<<<< HEAD
<<<<<<< HEAD
    }    /**     * Check if a permission should be granted by default when no permission system is found
=======
    }
      /**
=======
    }    /**
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
     * Check if a permission should be granted by default when no permission system is found
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
     * 
     * @param permission The permission string to check
     * @return True if the permission should be granted by default, false otherwise
     */
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 16744a4 (feat: Add comprehensive compilation fixes documentation; detail issues resolved and future recommendations)
    static boolean checkDefaultPermission(String permission) {
        // Get the config instance from NeoEssentials
        com.zerog.neoessentials.config.CompatNeoEssentialsConfig config = NeoEssentials.getInstance().getConfigManager().getConfig();
        boolean debug = config.isDebug();
        
        // Check if the permission is explicitly defined in the config
        if (config.defaultPermissions().containsKey(permission)) {
            boolean result = config.defaultPermissions().get(permission);
            if (debug) {
                NeoEssentials.LOGGER.debug("Default permission '{}' explicitly configured as: {}", 
                    permission, result);
            }
            return result;
        } else {
            // Fall back to true if not explicitly defined
            if (debug) {
                NeoEssentials.LOGGER.debug("No explicit default permission for '{}', defaulting to TRUE", 
                    permission);
            }
            return true;
        }
=======
    private static boolean checkDefaultPermission(String permission) {
        // Get the config instance from NeoEssentials
        NeoEssentialsConfig config = NeoEssentials.getInstance().getConfigManager().getConfig();
        boolean debug = config.isDebug();
        
<<<<<<< HEAD
        // Default behavior - will be configurable in the future
        return config.defaultPermissions().getOrDefault(permission, true);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        // Check if the permission is explicitly defined in the config
        if (config.defaultPermissions().containsKey(permission)) {
            boolean result = config.defaultPermissions().get(permission);
            if (debug) {
                NeoEssentials.LOGGER.debug("Default permission '{}' explicitly configured as: {}", 
                    permission, result);
            }
            return result;
        } else {
            // Fall back to true if not explicitly defined
            if (debug) {
                NeoEssentials.LOGGER.debug("No explicit default permission for '{}', defaulting to TRUE", 
                    permission);
            }
            return true;
        }
>>>>>>> 6ae378a (refactor: Enhance storage management and data reloading; improve logging for warps and permissions)
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
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
  
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    
    /**
     * Check if a player has a specific permission.
     * Will check LuckPerms, FTB Ranks, and then fall back to config defaults.
     *
     * @param player The player to check
     * @param permission The permission string to check
     * @return True if the player has the permission, false otherwise
     */
    public static boolean hasPermission(ServerPlayer player, String permission) {
<<<<<<< HEAD
        // Create a temporary command source stack for the player
        CommandSourceStack source = player.createCommandSourceStack();
        return hasPermission(source, permission);
    }
<<<<<<< HEAD
>>>>>>> bac244b (Implement messaging and player state commands)
=======
    
    /**
     * Check if a GameProfile has a specific permission.
     * For offline checking, will check config defaults only.
     *
     * @param profile The GameProfile to check
     * @param permission The permission string to check
     * @return True if the profile has the permission, false otherwise
     */
    public static boolean hasPermission(com.mojang.authlib.GameProfile profile, String permission) {
        // Check if player is an operator
        if (NeoEssentials.getInstance().getServer().getPlayerList().isOp(profile)) {
=======
        boolean debug = NeoEssentials.getInstance().getConfigManager().getConfig().isDebug();
        
        // Operators always have permission
        if (player.hasPermissions(2)) {
            if (debug) {
                NeoEssentials.LOGGER.debug("Permission '{}' granted to operator {}", 
                    permission, player.getScoreboardName());
            }
>>>>>>> 7058369 (feat: Update migration tasks and enhance tablist documentation; refactor permission checks in AdminPanelCommand and CommandManager)
            return true;
        }
        
        UUID playerUUID = player.getUUID();
        
        // Check cache first
        PermissionResult cachedResult = getCachedPermission(playerUUID, permission);
        if (cachedResult != null) {
            return cachedResult.result;
        }
        
        // Try LuckPerms first (most common)
        if (checkLuckPermsPermission(playerUUID, permission)) {
            cachePermission(playerUUID, permission, true);
            return true;
        }
        
        // No permission systems gave access - deny
        if (debug) {
            NeoEssentials.LOGGER.debug("Permission '{}' denied for player {}", 
                permission, player.getScoreboardName());
        }
        
        cachePermission(playerUUID, permission, false);
        return false;
    }
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
  
>>>>>>> 02542de (refactor: Simplify permission checks in AdminPanelCommand; add checkPlayerPermission method in PermissionUtil)
}
