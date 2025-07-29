package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerPlayer;

/**
 * Utility class for handling permissions
 * Integrates with various permission systems
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionUtil {
    
    /**
     * Check if player has a specific permission
     * This is a placeholder implementation - would integrate with LuckPerms, etc.
     */
    public static boolean hasPermission(ServerPlayer player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        // Check if player is OP (default fallback)
        if (player.hasPermissions(4)) {
            return true;
        }
        
        // Placeholder for permission system integration
        // In a real implementation, this would check with LuckPerms, PermissionsEx, etc.
        
        // Default permissions for basic functionality
        if (permission.startsWith("essentials.")) {
            // Grant basic essentials permissions to all players by default
            if (permission.equals("essentials.home") ||
                permission.equals("essentials.sethome") ||
                permission.equals("essentials.delhome") ||
                permission.equals("essentials.warp") ||
                permission.equals("essentials.spawn") ||
                permission.equals("essentials.back") ||
                permission.equals("essentials.msg") ||
                permission.equals("essentials.reply") ||
                permission.equals("essentials.mail") ||
                permission.equals("essentials.balance") ||
                permission.equals("essentials.pay") ||
                permission.equals("essentials.kit")) {
                return true;
            }
            
            // Admin permissions require OP
            if (permission.equals("essentials.setwarp") ||
                permission.equals("essentials.delwarp") ||
                permission.equals("essentials.setspawn") ||
                permission.equals("essentials.ban") ||
                permission.equals("essentials.kick") ||
                permission.equals("essentials.mute") ||
                permission.equals("essentials.jail") ||
                permission.equals("essentials.eco") ||
                permission.equals("essentials.give") ||
                permission.equals("essentials.gamemode") ||
                permission.equals("essentials.tp") ||
                permission.equals("essentials.tphere")) {
                return player.hasPermissions(2); // Require at least level 2 permissions
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
        return hasPermission(player, "essentials.admin") || player.hasPermissions(3);
    }
    
    /**
     * Check if player is moderator
     */
    public static boolean isModerator(ServerPlayer player) {
        return hasPermission(player, "essentials.moderator") || player.hasPermissions(2);
    }
    
    /**
     * Check if player is staff (admin or moderator)
     */
    public static boolean isStaff(ServerPlayer player) {
        return isAdmin(player) || isModerator(player);
    }
    
    /**
     * Get player's group/rank (placeholder)
     */
    public static String getPlayerGroup(ServerPlayer player) {
        if (player == null) {
            return "default";
        }
        
        if (isAdmin(player)) {
            return "admin";
        } else if (isModerator(player)) {
            return "moderator";
        } else if (hasPermission(player, "essentials.vip")) {
            return "vip";
        } else {
            return "default";
        }
    }
    
    /**
     * Check permission with wildcards
     */
    public static boolean hasWildcardPermission(ServerPlayer player, String permission) {
        if (hasPermission(player, permission)) {
            return true;
        }
        
        // Check for wildcard permissions
        String[] parts = permission.split("\\.");
        StringBuilder wildcard = new StringBuilder();
        
        for (int i = 0; i < parts.length - 1; i++) {
            wildcard.append(parts[i]).append(".");
            if (hasPermission(player, wildcard + "*")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get permission prefix for messages
     */
    public static String getPermissionPrefix(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        
        // This would integrate with permission system to get prefixes
        String group = getPlayerGroup(player);
        
        return switch (group) {
            case "admin" -> "&c[ADMIN] ";
            case "moderator" -> "&6[MOD] ";
            case "vip" -> "&b[VIP] ";
            default -> "";
        };
    }
    
    /**
     * Get permission suffix for messages
     */
    public static String getPermissionSuffix(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        
        // This would integrate with permission system to get suffixes
        String group = getPlayerGroup(player);
        
        return switch (group) {
            case "admin" -> " &c⚡";
            case "moderator" -> " &6★";
            case "vip" -> " &b♦";
            default -> "";
        };
    }
}
