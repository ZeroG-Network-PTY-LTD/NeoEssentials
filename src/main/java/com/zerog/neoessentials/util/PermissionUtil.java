package com.zerog.neoessentials.util;

import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling permissions
 * Integrates with the Custom Permissions Manager and provides comprehensive permission checking
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUtil.class);
    
    /**
     * Check if player has a specific permission
     * Integrates with the Custom Permissions Manager
     */
    public static boolean hasPermission(ServerPlayer player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        // Use Custom Permissions Manager
        try {
            return CustomPermissionsManager.getInstance().hasPermission(player, permission);
        } catch (Exception e) {
            LOGGER.warn("Permission check failed for player {} permission {}, using fallback: {}", 
                player.getName().getString(), permission, e.getMessage());
            // Fallback to basic permission check if Custom Permissions Manager fails
            return fallbackPermissionCheck(player, permission);
        }
    }
    
    /**
     * Check if command source has a specific permission
     * Works with both players and console
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        if (source == null || permission == null) {
            return false;
        }
        
        // Console always has all permissions
        if (!source.isPlayer()) {
            return true;
        }
        
        try {
            ServerPlayer player = source.getPlayerOrException();
            return hasPermission(player, permission);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if player has permission with automatic bypass for ops
     */
    public static boolean hasPermissionOrOp(ServerPlayer player, String permission) {
        if (player == null) {
            return false;
        }
        
        // Ops bypass all permission checks
        if (player.hasPermissions(4)) {
            return true;
        }
        
        return hasPermission(player, permission);
    }
    
    /**
     * Check if command source has permission with automatic bypass for console/ops
     */
    public static boolean hasPermissionOrOp(CommandSourceStack source, String permission) {
        if (source == null) {
            return false;
        }
        
        // Console always has all permissions
        if (!source.isPlayer()) {
            return true;
        }
        
        try {
            ServerPlayer player = source.getPlayerOrException();
            return hasPermissionOrOp(player, permission);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Fallback permission check using vanilla system with comprehensive permission defaults
     */
    private static boolean fallbackPermissionCheck(ServerPlayer player, String permission) {
        // Check if player is OP (default fallback)
        if (player.hasPermissions(4)) {
            return true;
        }
        
        // Default permissions for basic functionality - using PermissionNodes constants
        if (permission.startsWith("essentials.")) {
            // Grant basic essentials permissions to all players by default
            if (permission.equals(PermissionNodes.HOME) ||
                permission.equals(PermissionNodes.HOME_SET) ||
                permission.equals(PermissionNodes.HOME_DELETE) ||
                permission.equals(PermissionNodes.HOME_LIST) ||
                permission.equals(PermissionNodes.WARP) ||
                permission.equals(PermissionNodes.WARP_LIST) ||
                permission.equals(PermissionNodes.SPAWN) ||
                permission.equals(PermissionNodes.BACK) ||
                permission.equals(PermissionNodes.MSG) ||
                permission.equals(PermissionNodes.REPLY) ||
                permission.equals(PermissionNodes.MAIL_SEND) ||
                permission.equals(PermissionNodes.MAIL_READ) ||
                permission.equals(PermissionNodes.ECO_BALANCE) ||
                permission.equals(PermissionNodes.ECO_PAY) ||
                permission.equals(PermissionNodes.KIT) ||
                permission.equals(PermissionNodes.KIT_LIST) ||
                permission.equals(PermissionNodes.LIST) ||
                permission.equals(PermissionNodes.WHOIS) ||
                permission.equals(PermissionNodes.SEEN) ||
                permission.equals(PermissionNodes.TPA_REQUEST) ||
                permission.equals(PermissionNodes.TPA_ACCEPT) ||
                permission.equals(PermissionNodes.TPA_DENY)) {
                return true;
            }
            
            // Admin permissions require OP
            if (permission.equals(PermissionNodes.WARP_SET) ||
                permission.equals(PermissionNodes.WARP_DELETE) ||
                permission.equals(PermissionNodes.SPAWN_SET) ||
                permission.equals(PermissionNodes.BAN) ||
                permission.equals(PermissionNodes.KICK) ||
                permission.equals(PermissionNodes.MUTE) ||
                permission.equals(PermissionNodes.JAIL) ||
                permission.equals(PermissionNodes.ECO_GIVE) ||
                permission.equals(PermissionNodes.ECO_TAKE) ||
                permission.equals(PermissionNodes.ECO_SET) ||
                permission.equals(PermissionNodes.GIVE_ITEM) ||
                permission.equals(PermissionNodes.GOD_OTHERS) ||
                permission.equals(PermissionNodes.HEAL_OTHERS) ||
                permission.equals(PermissionNodes.FEED_OTHERS) ||
                permission.equals(PermissionNodes.FLY_OTHERS) ||
                permission.equals(PermissionNodes.TP_OTHERS) ||
                permission.equals(PermissionNodes.TP_HERE)) {
                return player.hasPermissions(2); // Require at least level 2 permissions
            }
        }
        
        // NeoEssentials permissions - basic user permissions
        if (permission.startsWith("neoessentials.")) {
            if (permission.equals(PermissionNodes.BOSSBAR_SHOW) ||
                permission.equals(PermissionNodes.PLACEHOLDER_TEST) ||
                permission.equals(PermissionNodes.GUI_OPEN) ||
                permission.equals(PermissionNodes.PLAYTIME_VIEW) ||
                permission.equals(PermissionNodes.ACHIEVEMENTS_VIEW) ||
                permission.equals(PermissionNodes.PREFERENCES_SET) ||
                permission.equals(PermissionNodes.PREFERENCES_VIEW) ||
                permission.equals(PermissionNodes.DISCORD_LINK) ||
                permission.equals(PermissionNodes.DISCORD_ITEM)) {
                return true;
            }
            
            // Admin permissions require higher level
            if (permission.equals(PermissionNodes.PERMISSIONS_INFO) ||
                permission.equals(PermissionNodes.PERMISSIONS_CHECK) ||
                permission.equals(PermissionNodes.CONFIG_RELOAD) ||
                permission.equals(PermissionNodes.SECURITY_VIEW) ||
                permission.equals(PermissionNodes.PERFORMANCE_VIEW) ||
                permission.equals(PermissionNodes.STATUS_VIEW)) {
                return player.hasPermissions(2);
            }
            
            // High-level admin permissions
            if (permission.endsWith(".admin") || 
                permission.endsWith(".*") ||
                permission.equals(PermissionNodes.PERMISSIONS_USER) ||
                permission.equals(PermissionNodes.PERMISSIONS_GROUP) ||
                permission.equals(PermissionNodes.CONFIG_SAVE) ||
                permission.equals(PermissionNodes.CONFIG_RESET)) {
                return player.hasPermissions(3);
            }
        }
        
        return false;
    }
    
    /**
     * Check if player has any of the given permissions
     */
    public static boolean hasAnyPermission(ServerPlayer player, String... permissions) {
        if (player == null || permissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (hasPermission(player, permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if command source has any of the given permissions
     */
    public static boolean hasAnyPermission(CommandSourceStack source, String... permissions) {
        if (source == null || permissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (hasPermission(source, permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if player has all of the given permissions
     */
    public static boolean hasAllPermissions(ServerPlayer player, String... permissions) {
        if (player == null || permissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (!hasPermission(player, permission)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if command source has all of the given permissions
     */
    public static boolean hasAllPermissions(CommandSourceStack source, String... permissions) {
        if (source == null || permissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (!hasPermission(source, permission)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get player's permission level (0-4)
     */
    public static int getPermissionLevel(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        // Check various permission levels
        if (player.hasPermissions(4)) return 4; // Full admin
        if (player.hasPermissions(3)) return 3; // Command blocks
        if (player.hasPermissions(2)) return 2; // Moderator
        if (player.hasPermissions(1)) return 1; // Basic commands
        
        return 0; // No special permissions
    }
    
    /**
     * Check if player is admin (has high-level permissions)
     */
    public static boolean isAdmin(ServerPlayer player) {
        return hasPermission(player, PermissionNodes.ALL_NEOESSENTIALS) || 
               hasPermission(player, PermissionNodes.ALL_ESSENTIALS) ||
               player.hasPermissions(3);
    }
    
    /**
     * Check if player is moderator
     */
    public static boolean isModerator(ServerPlayer player) {
        return hasPermission(player, PermissionNodes.ALL_MODERATION) || 
               hasPermission(player, PermissionNodes.KICK) ||
               hasPermission(player, PermissionNodes.MUTE) ||
               player.hasPermissions(2);
    }
    
    /**
     * Check if player is staff (admin or moderator)
     */
    public static boolean isStaff(ServerPlayer player) {
        return isAdmin(player) || isModerator(player);
    }
    
    /**
     * Get player's group/rank using Custom Permissions Manager
     */
    public static String getPlayerGroup(ServerPlayer player) {
        if (player == null) {
            return "default";
        }
        
        try {
            return CustomPermissionsManager.getInstance().getPlayerGroup(player.getUUID());
        } catch (Exception e) {
            // Fallback to basic group detection
            if (isAdmin(player)) {
                return "admin";
            } else if (isModerator(player)) {
                return "moderator";
            } else if (hasPermission(player, PermissionNodes.FLY_SELF) || 
                      hasPermission(player, PermissionNodes.HEAL_SELF)) {
                return "vip";
            } else {
                return "default";
            }
        }
    }
    
    /**
     * Get player's prefix using Custom Permissions Manager
     */
    public static String getPlayerPrefix(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        
        try {
            return CustomPermissionsManager.getInstance().getPlayerPrefix(player.getUUID());
        } catch (Exception e) {
            // Fallback based on detected group
            String group = getPlayerGroup(player);
            switch (group.toLowerCase()) {
                case "admin": return "§c[Admin] ";
                case "moderator": return "§6[Mod] ";
                case "vip": return "§6[VIP] ";
                default: return "";
            }
        }
    }
    
    /**
     * Check if permission node is valid using the PermissionNodes validator
     */
    public static boolean isValidPermission(String permission) {
        return PermissionNodes.isValidPermission(permission);
    }
    
    /**
     * Get appropriate permission for a command with optional sub-permission
     */
    public static String getCommandPermission(String basePermission, String subPermission) {
        if (subPermission == null || subPermission.isEmpty()) {
            return basePermission;
        }
        return basePermission + "." + subPermission;
    }
    
    /**
     * Check if player can bypass cooldowns
     */
    public static boolean canBypassCooldown(ServerPlayer player, String cooldownType) {
        return hasPermission(player, PermissionNodes.BYPASS_COOLDOWN) ||
               hasPermission(player, getCommandPermission(PermissionNodes.BYPASS_COOLDOWN, cooldownType));
    }
    
    /**
     * Check if player can bypass costs
     */
    public static boolean canBypassCost(ServerPlayer player, String costType) {
        return hasPermission(player, PermissionNodes.BYPASS_COST) ||
               hasPermission(player, getCommandPermission(PermissionNodes.BYPASS_COST, costType));
    }
    
    /**
     * Check if player can bypass limits
     */
    public static boolean canBypassLimit(ServerPlayer player, String limitType) {
        return hasPermission(player, getCommandPermission("essentials.bypass.limit", limitType));
    }
    
    /**
     * Get permission prefix for messages using Custom Permissions Manager
     */
    public static String getPermissionPrefix(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        
        try {
            return CustomPermissionsManager.getInstance().getPlayerPrefix(player.getUUID());
        } catch (Exception e) {
            // Fallback to basic prefixes
            String group = getPlayerGroup(player);
            return switch (group) {
                case "admin" -> "&c[ADMIN] ";
                case "moderator" -> "&6[MOD] ";
                case "vip" -> "&b[VIP] ";
                default -> "";
            };
        }
    }
    
    /**
     * Get permission suffix for messages using Custom Permissions Manager
     */
    public static String getPermissionSuffix(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        
        try {
            return CustomPermissionsManager.getInstance().getPlayerSuffix(player.getUUID());
        } catch (Exception e) {
            // Fallback to basic suffixes
            String group = getPlayerGroup(player);
            return switch (group) {
                case "admin" -> " &c⚡";
                case "moderator" -> " &6★";
                case "vip" -> " &b♦";
                default -> "";
            };
        }
    }
}
