package com.zerog.neoessentials.webdashboard.security;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Service for synchronizing permissions from Discord roles
 */
public class DiscordPermissionSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordPermissionSync.class);
    private static DiscordPermissionSync INSTANCE;
    
    private boolean enabled = true;
    private DiscordAuthConfig authConfig;
    
    private DiscordPermissionSync() {
        // Load Discord auth config for role mappings
        this.authConfig = DiscordAuthConfig.load();
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
            com.zerog.neoessentials.permissions.PermissionManager permManager = com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            
            for (String role : discordUser.getDiscordRoles()) {
                // Map Discord roles to permission groups
                String permissionGroup = mapDiscordRoleToPermissionGroup(role);
                
                if (permissionGroup != null && !permissionGroup.isEmpty()) {
                    LOGGER.debug("Granting permission group '{}' to player {} based on Discord role '{}'", 
                                permissionGroup, player.getName().getString(), role);
                    
                    // Get or create the user and set their group
                    com.zerog.neoessentials.permissions.PermissionUser user = permManager.getUser(player.getUUID());
                    if (user != null) {
                        user.setGroup(permissionGroup);
                        permissionsGranted++;
                        
                        // Save the permission changes
                        try {
                            com.zerog.neoessentials.permissions.PermissionStorage.save(permManager);
                        } catch (Exception saveEx) {
                            LOGGER.error("Failed to save permission changes for player {}", player.getName().getString(), saveEx);
                        }
                    }
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
     * Maps a Discord role name or ID to a permission group name.
     * First checks DiscordAuthConfig for custom mappings, then falls back to basic mappings.
     * 
     * @param discordRole The Discord role name or ID
     * @return The permission group name, or null if no mapping exists
     */
    private String mapDiscordRoleToPermissionGroup(String discordRole) {
        if (discordRole == null || discordRole.isEmpty()) {
            return null;
        }
        
        // Load custom role mappings from DiscordAuthConfig
        if (authConfig != null && authConfig.isPermissionSyncEnabled()) {
            Map<String, java.util.List<String>> permissionMappings = authConfig.getPermissionMappings();
            
            // Check if this Discord role (ID or name) has a custom permission mapping
            if (permissionMappings.containsKey(discordRole)) {
                java.util.List<String> permissions = permissionMappings.get(discordRole);
                // For now, we treat the first permission as a group name
                // In the future, this could be extended to grant multiple individual permissions
                if (permissions != null && !permissions.isEmpty()) {
                    String mappedGroup = permissions.getFirst();
                    LOGGER.debug("Found custom role mapping: Discord role '{}' -> permission group '{}'", discordRole, mappedGroup);
                    return mappedGroup;
                }
            }
        }
        
        // Fallback to basic role mappings (case-insensitive name matching)
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
        
        return null; // No mapping found
    }
    
    /**
     * Reload the Discord auth config (useful after config changes)
     */
    public void reloadConfig() {
        this.authConfig = DiscordAuthConfig.load();
        LOGGER.info("Discord auth config reloaded for permission sync");
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
