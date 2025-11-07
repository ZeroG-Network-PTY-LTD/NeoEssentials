package com.zerog.neoessentials.webdashboard.security;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for synchronizing permissions from Discord roles
 */
public class DiscordPermissionSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordPermissionSync.class);
    private static DiscordPermissionSync INSTANCE;
    
    private boolean enabled = true;
    
    private DiscordPermissionSync() {
    }
    
    public static DiscordPermissionSync getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DiscordPermissionSync();
        }
        return INSTANCE;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Sync permissions for a player based on their Discord roles
     */
    public SyncResult syncPlayerPermissions(ServerPlayer player) {
        if (!enabled) {
            return new SyncResult(false, "Permission sync is disabled", 0);
        }
        
        try {
            // Check if Discord integration is available
            if (!SDLinkEventListener.isBotReady()) {
                return new SyncResult(false, "Discord bot not ready", 0);
            }
            
            // Get Discord user for this player
            DiscordAuthProvider provider = DiscordAuthProvider.getInstance();
            DiscordUser discordUser = provider.getLinkedAccountByUuid(player.getUUID());
            
            if (discordUser == null || !discordUser.isLinked()) {
                return new SyncResult(false, "Player not linked to Discord", 0);
            }
            
            // Sync permissions based on Discord roles
            int permissionsGranted = 0;
            com.zerog.neoessentials.api.permissions.PermissionAPI permAPI = com.zerog.neoessentials.api.permissions.PermissionAPI.getInstance();
            
            for (String role : discordUser.getDiscordRoles()) {
                // Map Discord roles to permission groups
                // Role mappings should be configured in DiscordAuthConfig
                String permissionGroup = mapDiscordRoleToPermissionGroup(role);
                
                if (permissionGroup != null && !permissionGroup.isEmpty()) {
                    LOGGER.debug("Granting permission group '{}' to player {} based on Discord role '{}'", 
                                permissionGroup, player.getName().getString(), role);
                    
                    // Grant the permission group to the player
                    permAPI.setGroup(player.getUUID(), permissionGroup);
                    permissionsGranted++;
                } else {
                    LOGGER.debug("No permission mapping for Discord role: {}", role);
                }
            }
            
            return new SyncResult(true, "Permissions synced successfully", permissionsGranted);
            
        } catch (Exception e) {
            LOGGER.error("Error syncing permissions for player {}: {}", player.getName().getString(), e.getMessage());
            return new SyncResult(false, "Error: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Maps a Discord role name to a permission group name.
     * This is a basic implementation that can be extended with configuration.
     * 
     * @param discordRole The Discord role name
     * @return The permission group name, or null if no mapping exists
     */
    private String mapDiscordRoleToPermissionGroup(String discordRole) {
        if (discordRole == null || discordRole.isEmpty()) {
            return null;
        }
        
        // Basic role mappings (case-insensitive)
        String roleLower = discordRole.toLowerCase();
        
        // Map common Discord role names to permission groups
        if (roleLower.contains("admin") || roleLower.contains("administrator")) {
            return "admin";
        } else if (roleLower.contains("moderator") || roleLower.contains("mod")) {
            return "moderator";
        } else if (roleLower.contains("helper") || roleLower.contains("support")) {
            return "helper";
        } else if (roleLower.contains("vip") || roleLower.contains("premium")) {
            return "vip";
        } else if (roleLower.contains("member") || roleLower.contains("player")) {
            return "default";
        }
        
        // TODO: Load custom role mappings from DiscordAuthConfig
        // This would allow server owners to define their own Discord role → permission group mappings
        
        return null; // No mapping found
    }
    
    /**
     * Result of a permission sync operation
     */
    public static class SyncResult {
        private final boolean success;
        private final String message;
        private final int permissionsGranted;
        
        public SyncResult(boolean success, String message, int permissionsGranted) {
            this.success = success;
            this.message = message;
            this.permissionsGranted = permissionsGranted;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getPermissionsGranted() {
            return permissionsGranted;
        }
    }
}
