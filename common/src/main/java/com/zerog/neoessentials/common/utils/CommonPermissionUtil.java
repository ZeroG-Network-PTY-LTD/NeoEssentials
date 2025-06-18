package com.zerog.neoessentials.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Common permission utility functions that are version-independent.
 * This class provides a basic permission framework and constants.
 */
public class CommonPermissionUtil {
    
    // Permission constants
    public static final String PERM_PREFIX = "neoessentials.";
    
    // Command categories
    public static final String PERM_COMMAND = PERM_PREFIX + "command.";
    
    // Teleport commands
    public static final String PERM_COMMAND_HOME = PERM_COMMAND + "home";
    public static final String PERM_COMMAND_HOME_SET = PERM_COMMAND + "home.set";
    public static final String PERM_COMMAND_HOME_DELETE = PERM_COMMAND + "home.delete";
    public static final String PERM_COMMAND_HOME_LIST = PERM_COMMAND + "home.list";
    public static final String PERM_COMMAND_BACK = PERM_COMMAND + "back";
    public static final String PERM_COMMAND_SPAWN = PERM_COMMAND + "spawn";
    public static final String PERM_COMMAND_SPAWN_SET = PERM_COMMAND + "spawn.set";
    public static final String PERM_COMMAND_WARP = PERM_COMMAND + "warp";
    public static final String PERM_COMMAND_WARP_SET = PERM_COMMAND + "warp.set";
    public static final String PERM_COMMAND_WARP_DELETE = PERM_COMMAND + "warp.delete";
    public static final String PERM_COMMAND_WARP_LIST = PERM_COMMAND + "warp.list";
    public static final String PERM_COMMAND_TPA = PERM_COMMAND + "tpa";
    public static final String PERM_COMMAND_TPAHERE = PERM_COMMAND + "tpahere";
    public static final String PERM_COMMAND_TPACCEPT = PERM_COMMAND + "tpaccept";
    public static final String PERM_COMMAND_TPDENY = PERM_COMMAND + "tpdeny";
    
    // Utility commands
    public static final String PERM_COMMAND_HEAL = PERM_COMMAND + "heal";
    public static final String PERM_COMMAND_HEAL_OTHERS = PERM_COMMAND + "heal.others";
    public static final String PERM_COMMAND_FEED = PERM_COMMAND + "feed";
    public static final String PERM_COMMAND_FEED_OTHERS = PERM_COMMAND + "feed.others";
    public static final String PERM_COMMAND_FLY = PERM_COMMAND + "fly";
    public static final String PERM_COMMAND_FLY_OTHERS = PERM_COMMAND + "fly.others";
    public static final String PERM_COMMAND_GAMEMODE = PERM_COMMAND + "gamemode";
    public static final String PERM_COMMAND_GAMEMODE_OTHERS = PERM_COMMAND + "gamemode.others";
    public static final String PERM_COMMAND_REPAIR = PERM_COMMAND + "repair";
    public static final String PERM_COMMAND_INVSEE = PERM_COMMAND + "invsee";
    public static final String PERM_COMMAND_ENDERCHEST = PERM_COMMAND + "enderchest";
    public static final String PERM_COMMAND_ENDERCHEST_OTHERS = PERM_COMMAND + "enderchest.others";
    
    // Admin commands
    public static final String PERM_COMMAND_KICK = PERM_COMMAND + "kick";
    public static final String PERM_COMMAND_BAN = PERM_COMMAND + "ban";
    public static final String PERM_COMMAND_UNBAN = PERM_COMMAND + "unban";
    public static final String PERM_COMMAND_TP = PERM_COMMAND + "tp";
    public static final String PERM_COMMAND_TPPOS = PERM_COMMAND + "tppos";
    public static final String PERM_COMMAND_BROADCAST = PERM_COMMAND + "broadcast";
    public static final String PERM_COMMAND_WEATHER = PERM_COMMAND + "weather";
    public static final String PERM_COMMAND_TIME = PERM_COMMAND + "time";
    public static final String PERM_COMMAND_KILL = PERM_COMMAND + "kill";
    
    // Economy commands
    public static final String PERM_COMMAND_BALANCE = PERM_COMMAND + "balance";
    public static final String PERM_COMMAND_BALANCE_OTHERS = PERM_COMMAND + "balance.others";
    public static final String PERM_COMMAND_PAY = PERM_COMMAND + "pay";
    public static final String PERM_COMMAND_ECO = PERM_COMMAND + "eco";
    
    // Chat commands
    public static final String PERM_COMMAND_NICKNAME = PERM_COMMAND + "nick";
    public static final String PERM_COMMAND_NICKNAME_OTHERS = PERM_COMMAND + "nick.others";
    public static final String PERM_COMMAND_NICKNAME_COLOR = PERM_COMMAND + "nick.color";
    public static final String PERM_COMMAND_MAIL = PERM_COMMAND + "mail";
    public static final String PERM_COMMAND_MSG = PERM_COMMAND + "msg";
    public static final String PERM_COMMAND_REPLY = PERM_COMMAND + "reply";
    
    // Permission use
    public static final String PERM_USE_SIGNS_COLOR = PERM_PREFIX + "signs.color";
    public static final String PERM_USE_CHAT_COLOR = PERM_PREFIX + "chat.color";
    public static final String PERM_BYPASS_TELEPORT_COOLDOWN = PERM_PREFIX + "bypass.teleport.cooldown";
    public static final String PERM_BYPASS_TELEPORT_WARMUP = PERM_PREFIX + "bypass.teleport.warmup";
    
    // Bypass permissions
    public static final String PERM_BYPASS_PREFIX = PERM_PREFIX + "bypass.";
    
    // Homes limit group permissions (by default everyone has 3 homes)
    public static final String PERM_HOMES_PREFIX = PERM_PREFIX + "homes.";
    
    // Cached permissions at runtime (to be implemented by version-specific code)
    private static final Map<UUID, Map<String, Boolean>> permissionCache = new HashMap<>();
    
    /**
     * Clear the permission cache for a player
     * 
     * @param playerUuid The UUID of the player
     */
    public static void clearPermissionCache(UUID playerUuid) {
        permissionCache.remove(playerUuid);
    }
    
    /**
     * Clear all permission caches
     */
    public static void clearAllPermissionCaches() {
        permissionCache.clear();
    }
    
    /**
     * Cache a permission value for a player
     * 
     * @param playerUuid The UUID of the player
     * @param permission The permission to cache
     * @param value The value to cache
     */
    public static void cachePermission(UUID playerUuid, String permission, boolean value) {
        permissionCache.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(permission, value);
    }
    
    /**
     * Get a cached permission value for a player
     * 
     * @param playerUuid The UUID of the player
     * @param permission The permission to get
     * @return The cached value, or null if not cached
     */
    public static Boolean getCachedPermission(UUID playerUuid, String permission) {
        Map<String, Boolean> playerCache = permissionCache.get(playerUuid);
        return playerCache != null ? playerCache.get(permission) : null;
    }
}
