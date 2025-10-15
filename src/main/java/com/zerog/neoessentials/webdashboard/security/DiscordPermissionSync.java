package com.zerog.neoessentials.webdashboard.security;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Synchronizes Discord roles to Minecraft permissions automatically.
 * When a player with a verified Discord account joins the server,
 * their Discord roles are used to grant appropriate Minecraft permissions.
 */
public class DiscordPermissionSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordPermissionSync.class);
    private static DiscordPermissionSync instance;
    
    private boolean enabled = true;
    private boolean syncOnJoin = true;
    private boolean removeUnmatchedPermissions = false;
    
    private DiscordPermissionSync() {
        // Private constructor for singleton
    }
    
    public static DiscordPermissionSync getInstance() {
        if (instance == null) {
            instance = new DiscordPermissionSync();
        }
        return instance;
    }
    
    /**
     * Sync permissions for a player based on their Discord roles
     * 
     * @param player The player to sync permissions for
     * @return SyncResult containing what was changed
     */
    public SyncResult syncPlayerPermissions(ServerPlayer player) {
        if (!enabled) {
            return new SyncResult(false, "Discord permission sync is disabled", 0, 0);
        }
        
        String minecraftUsername = player.getName().getString();
        UUID playerUuid = player.getUUID();
        
        try {
            // Check if Discord auth is available
            DiscordAuthProvider discordAuth = DiscordAuthProvider.getInstance();
            if (!discordAuth.isAvailable()) {
                return new SyncResult(false, "Discord integration not available", 0, 0);
            }
            
            // Get linked Discord account
            DiscordUser discordUser = discordAuth.getLinkedAccount(minecraftUsername);
            if (discordUser == null) {
                return new SyncResult(false, "Player does not have a linked Discord account", 0, 0);
            }
            
            // Load Discord config to get role → permission mappings
            DiscordAuthConfig discordConfig = DiscordAuthConfig.load();
            Map<String, List<String>> permissionMappings = discordConfig.getPermissionMappings();
            
            if (permissionMappings.isEmpty()) {
                return new SyncResult(false, "No permission mappings configured", 0, 0);
            }
            
            // Get player's Discord roles
            List<String> userRoles = discordUser.getDiscordRoles();
            
            // Debug logging
            LOGGER.info("=== Permission Sync for {} ===", minecraftUsername);
            LOGGER.info("Player's Discord Roles: {}", userRoles);
            LOGGER.info("Available Permission Mappings: {}", permissionMappings.keySet());
            
            // Collect all permissions this player should have based on their roles
            Set<String> shouldHavePermissions = new HashSet<>();
            for (String roleId : userRoles) {
                if (permissionMappings.containsKey(roleId)) {
                    List<String> perms = permissionMappings.get(roleId);
                    shouldHavePermissions.addAll(perms);
                    LOGGER.info("✓ Role {} grants {} permissions: {}", roleId, perms.size(), perms);
                }
            }
            
            if (shouldHavePermissions.isEmpty()) {
                LOGGER.warn("✗ No permissions matched for player {}", minecraftUsername);
                LOGGER.warn("  Player has roles: {}", userRoles);
                LOGGER.warn("  Config has mappings for: {}", permissionMappings.keySet());
                return new SyncResult(false, "No permissions matched for player's Discord roles", 0, 0);
            }
            
            LOGGER.info("Total permissions to sync: {}", shouldHavePermissions.size());
            
            // Grant permissions to the player
            int granted = 0;
            int removed = 0;
            int skipped = 0;
            
            LOGGER.info("Checking and granting permissions...");
            for (String permission : shouldHavePermissions) {
                try {
                    // Use PermissionAPI to grant permission
                    // Note: This assumes you're using LuckPerms or another permission plugin
                    // that integrates with PermissionAPI
                    
                    // Check if player already has this permission
                    if (!PermissionAPI.hasPermission(playerUuid, permission)) {
                        // Grant the permission through the permission manager
                        LOGGER.debug("  → Granting: {}", permission);
                        grantPermissionToPlayer(playerUuid, minecraftUsername, permission);
                        granted++;
                    } else {
                        LOGGER.debug("  ✓ Already has: {}", permission);
                        skipped++;
                    }
                } catch (Exception e) {
                    LOGGER.warn("  ✗ Failed to grant '{}': {}", permission, e.getMessage());
                }
            }
            
            LOGGER.info("Permission sync complete: {} granted, {} skipped", granted, skipped);
            
            // Optionally remove permissions that don't match any Discord role
            if (removeUnmatchedPermissions) {
                // This feature could be implemented if needed
                // Would require tracking which permissions were granted by Discord sync
            }
            
            String message = String.format("Synced %d permissions from Discord roles", granted);
            return new SyncResult(true, message, granted, removed);
            
        } catch (Exception e) {
            LOGGER.error("Error syncing permissions for player '{}': {}", minecraftUsername, e.getMessage(), e);
            return new SyncResult(false, "Error during sync: " + e.getMessage(), 0, 0);
        }
    }
    
    /**
     * Grant a permission to a player using the permission system
     */
    private void grantPermissionToPlayer(UUID playerUuid, String playerName, String permission) {
        // If using external permission plugin (LuckPerms, etc.)
        if (PermissionAPI.isUsingExternal()) {
            // Try to use LuckPerms API if available
            try {
                // Attempt to get LuckPerms API via reflection
                Class<?> luckPermsProviderClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Object luckPerms = luckPermsProviderClass.getMethod("get").invoke(null);
                
                // Get UserManager
                Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
                
                // Load user
                Object userFuture = userManager.getClass().getMethod("loadUser", UUID.class).invoke(userManager, playerUuid);
                Object user = userFuture.getClass().getMethod("join").invoke(userFuture);
                
                // Get DataHolder
                Object dataHolder = user.getClass().getMethod("data").invoke(user);
                
                // Create permission node
                Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                Object nodeBuilder = nodeClass.getMethod("builder", String.class).invoke(null, permission);
                Object node = nodeBuilder.getClass().getMethod("build").invoke(nodeBuilder);
                
                // Add permission node
                dataHolder.getClass().getMethod("add", nodeClass).invoke(dataHolder, node);
                
                // Save user
                userManager.getClass().getMethod("saveUser", user.getClass()).invoke(userManager, user);
                
                LOGGER.info("Granted permission '{}' to '{}' via LuckPerms", permission, playerName);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("LuckPerms not found. Please grant permissions manually:");
                LOGGER.warn("  /lp user {} permission set {} true", playerName, permission);
            } catch (Exception e) {
                LOGGER.error("Could not grant via LuckPerms: {}", e.getMessage());
                LOGGER.warn("Please grant manually: /lp user {} permission set {} true", playerName, permission);
            }
        } else {
            // Using built-in permission system
            var manager = PermissionAPI.getManager();
            if (manager != null) {
                var user = manager.getUser(playerUuid);
                if (user == null) {
                    // Create user with default group
                    user = new com.zerog.neoessentials.permissions.PermissionUser(
                        playerUuid, 
                        manager.getDefaultGroup()
                    );
                    manager.addUser(user);
                    LOGGER.debug("Created new permission user for '{}'", playerName);
                }
                user.addPermission(permission);
                LOGGER.info("Granted permission '{}' to '{}' via built-in system", permission, playerName);
            } else {
                LOGGER.error("Permission manager is null - cannot grant permission");
            }
        }
    }
    
    /**
     * Sync permissions for all online players with verified Discord accounts
     * 
     * @param server The server instance
     * @return Map of player name to sync result
     */
    public Map<String, SyncResult> syncAllOnlinePlayers(net.minecraft.server.MinecraftServer server) {
        Map<String, SyncResult> results = new HashMap<>();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerName = player.getName().getString();
            SyncResult result = syncPlayerPermissions(player);
            results.put(playerName, result);
        }
        
        return results;
    }
    
    /**
     * Enable or disable automatic permission sync
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("Discord permission sync {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enable or disable sync on player join
     */
    public void setSyncOnJoin(boolean syncOnJoin) {
        this.syncOnJoin = syncOnJoin;
    }
    
    public boolean isSyncOnJoin() {
        return syncOnJoin;
    }
    
    /**
     * Result of a permission sync operation
     */
    public static class SyncResult {
        private final boolean success;
        private final String message;
        private final int permissionsGranted;
        private final int permissionsRemoved;
        
        public SyncResult(boolean success, String message, int granted, int removed) {
            this.success = success;
            this.message = message;
            this.permissionsGranted = granted;
            this.permissionsRemoved = removed;
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
        
        public int getPermissionsRemoved() {
            return permissionsRemoved;
        }
        
        @Override
        public String toString() {
            return String.format("SyncResult{success=%s, granted=%d, removed=%d, message='%s'}", 
                success, permissionsGranted, permissionsRemoved, message);
        }
    }
}
