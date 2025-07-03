package com.zerog.neoessentials.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.NeoEssentials;

/**
 * Enhanced permission system for NeoEssentials with proper admin controls.
 * <p>
 * This system provides:
 * - OP level checking (levels 0-4)
 * - Permission node validation
 * - Admin-only command protection
 * - Integration with external permission plugins
 * </p>
 * 
 * @author ZeroG
 * @since 1.0.2.99
 */
public class PermissionUtil {

    // Permission levels
    public static final int LEVEL_PLAYER = 0;
    public static final int LEVEL_MODERATOR = 2;
    public static final int LEVEL_ADMIN = 3;
    public static final int LEVEL_OWNER = 4;

    /**
     * Checks if a command source has the specified permission.
     * 
     * @param source The command source
     * @param permission The permission node
     * @return true if has permission
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        try {
            // Check if source is from console (always has permission)
            if (!source.isPlayer()) {
                return true;
            }

            // Get the player
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                return false;
            }

            // Check permission with fallback to OP level
            return hasPermission(player, permission);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking permission {}: {}", permission, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player has the specified permission.
     * 
     * @param player The player
     * @param permission The permission node
     * @return true if has permission
     */
    public static boolean hasPermission(ServerPlayer player, String permission) {
        try {
            // Check if player is OP with appropriate level
            if (isOp(player)) {
                return true;
            }

            // Check admin permissions
            if (isAdminPermission(permission) && !isAdmin(player)) {
                return false;
            }

            // TODO: Integration with external permission plugins (LuckPerms, etc.)
            // For now, fall back to basic permission checking
            
            // Basic permission checking based on OP level
            return hasBasicPermission(player, permission);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking permission {} for player {}: {}", 
                permission, player.getDisplayName().getString(), e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player is an operator.
     * 
     * @param player The player
     * @return true if player is OP
     */
    public static boolean isOp(ServerPlayer player) {
        return player.hasPermissions(LEVEL_MODERATOR);
    }

    /**
     * Checks if a player is an admin (OP level 3+).
     * 
     * @param player The player
     * @return true if player is admin
     */
    public static boolean isAdmin(ServerPlayer player) {
        return player.hasPermissions(LEVEL_ADMIN);
    }

    /**
     * Checks if a player is an owner (OP level 4).
     * 
     * @param player The player
     * @return true if player is owner
     */
    public static boolean isOwner(ServerPlayer player) {
        return player.hasPermissions(LEVEL_OWNER);
    }

    /**
     * Checks if a permission requires admin privileges.
     * 
     * @param permission The permission node
     * @return true if admin permission
     */
    public static boolean isAdminPermission(String permission) {
        // Admin-only permissions
        String[] adminPermissions = {
            "neoessentials.admin",
            "neoessentials.reload",
            "neoessentials.debug",
            "neoessentials.maintenance",
            "neoessentials.economy.admin",
            "neoessentials.gamemode.others",
            "neoessentials.clearinventory.others",
            "neoessentials.spawn.admin",
            "neoessentials.shop.admin",
            "neoessentials.auction.admin",
            "neoessentials.invsee.edit",
            "neoessentials.editsign.admin"
        };

        for (String adminPerm : adminPermissions) {
            if (permission.startsWith(adminPerm)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Basic permission checking based on OP levels and permission nodes.
     * 
     * @param player The player
     * @param permission The permission node
     * @return true if has permission
     */
    private static boolean hasBasicPermission(ServerPlayer player, String permission) {
        // Parse permission node and determine required level
        int requiredLevel = getRequiredLevelForPermission(permission);
        
        // Check if player has required OP level
        return player.hasPermissions(requiredLevel);
    }

    /**
     * Gets the required OP level for a permission.
     * 
     * @param permission The permission node
     * @return Required OP level (0-4)
     */
    private static int getRequiredLevelForPermission(String permission) {
        // Owner level permissions (4)
        if (permission.contains(".admin") || permission.contains(".reload") || permission.contains(".debug")) {
            return LEVEL_OWNER;
        }
        
        // Admin level permissions (3)
        if (permission.contains(".others") || permission.contains(".edit") || permission.contains(".manage")) {
            return LEVEL_ADMIN;
        }
        
        // Moderator level permissions (2)
        if (permission.contains(".moderate") || permission.contains(".kick") || permission.contains(".ban")) {
            return LEVEL_MODERATOR;
        }
        
        // Default to player level (0)
        return LEVEL_PLAYER;
    }

    /**
     * Checks if a command source has admin privileges.
     * 
     * @param source The command source
     * @return true if has admin privileges
     */
    public static boolean hasAdminPrivileges(CommandSourceStack source) {
        try {
            // Console always has admin privileges
            if (!source.isPlayer()) {
                return true;
            }

            ServerPlayer player = source.getPlayer();
            return player != null && isAdmin(player);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking admin privileges: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a command source has owner privileges.
     * 
     * @param source The command source
     * @return true if has owner privileges
     */
    public static boolean hasOwnerPrivileges(CommandSourceStack source) {
        try {
            // Console always has owner privileges
            if (!source.isPlayer()) {
                return true;
            }

            ServerPlayer player = source.getPlayer();
            return player != null && isOwner(player);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking owner privileges: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sends appropriate permission error message.
     * 
     * @param source The command source
     * @param permission The permission that was required
     */
    public static void sendPermissionError(CommandSourceStack source, String permission) {
        try {
            if (isAdminPermission(permission)) {
                source.sendFailure(LanguageUtil.adminRequired());
            } else {
                source.sendFailure(LanguageUtil.insufficientPermissions());
            }
        } catch (Exception e) {
            source.sendFailure(LanguageUtil.noPermission());
        }
    }

    /**
     * Checks if a command source has admin permission for the specified permission.
     * This enforces admin-only access for sensitive commands.
     * 
     * @param source The command source
     * @param permission The permission node
     * @return true if has admin permission
     */
    public static boolean hasAdminPermission(CommandSourceStack source, String permission) {
        try {
            // Console always has admin permission
            if (!source.isPlayer()) {
                return true;
            }

            ServerPlayer player = source.getPlayer();
            if (player == null) {
                return false;
            }

            // Must be admin level to use admin commands
            if (!isAdmin(player)) {
                return false;
            }

            // Check the specific permission
            return hasPermission(player, permission);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking admin permission {} for source: {}", 
                permission, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a command source has moderator permission for the specified permission.
     * This enforces moderator-only access for moderation commands.
     * 
     * @param source The command source
     * @param permission The permission node
     * @return true if has moderator permission
     */
    public static boolean hasModeratorPermission(CommandSourceStack source, String permission) {
        try {
            // Console always has moderator permission
            if (!source.isPlayer()) {
                return true;
            }

            ServerPlayer player = source.getPlayer();
            if (player == null) {
                return false;
            }

            // Must be at least moderator level
            if (!player.hasPermissions(LEVEL_MODERATOR)) {
                return false;
            }

            // Check the specific permission
            return hasPermission(player, permission);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking moderator permission {} for source: {}", 
                permission, e.getMessage());
            return false;
        }
    }
}
