package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.NeoPermissionsConfig;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple permission handler for vanilla servers.
 * This handler provides basic permission functionality when no other permission mods are present.
 * It stores permissions in a properties file for each player and group.
 */
public class VanillaPermissionHandler implements PermissionHandler {

    private static final String CONFIG_DIR = "config/neoessentials/permissions";
    private static final String PLAYERS_DIR = CONFIG_DIR + "/players";
    private static final String GROUPS_DIR = CONFIG_DIR + "/groups";
    private static final String DEFAULT_GROUP = "default";
    
    // Cache player permissions to avoid reading from disk frequently
    private final Map<UUID, Map<String, Boolean>> playerPermissionCache = new ConcurrentHashMap<>();
    
    // Cache group permissions
    private final Map<String, Map<String, Boolean>> groupPermissionCache = new ConcurrentHashMap<>();
    
    // Player group assignments
    private final Map<UUID, Set<String>> playerGroups = new ConcurrentHashMap<>();
    
    // Config reference for default permissions
    private NeoPermissionsConfig config;
    
    public VanillaPermissionHandler() {
        try {
            // Create necessary directories
            createDirectories();
            
            // Load default group if it doesn't exist
            ensureDefaultGroupExists();
            
            // Load all groups first
            loadGroups();
            
            // Load config
            config = NeoPermissionsConfig.get();
            
            NeoEssentials.LOGGER.info("Vanilla permission system initialized");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize vanilla permission system", e);
        }
    }
    
    /**
     * Create the necessary directories for the permission system
     */
    private void createDirectories() throws IOException {
        Path configDir = Paths.get(CONFIG_DIR);
        Path playersDir = Paths.get(PLAYERS_DIR);
        Path groupsDir = Paths.get(GROUPS_DIR);
        
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
        
        if (!Files.exists(playersDir)) {
            Files.createDirectories(playersDir);
        }
        
        if (!Files.exists(groupsDir)) {
            Files.createDirectories(groupsDir);
        }
    }
    
    /**
     * Ensure that the default group exists
     */
    private void ensureDefaultGroupExists() {
        File defaultGroupFile = new File(GROUPS_DIR, DEFAULT_GROUP + ".properties");
        if (!defaultGroupFile.exists()) {
            Properties properties = new Properties();
            // Set some default permissions for the default group
            properties.setProperty("neoessentials.home.basic", "true");
            properties.setProperty("neoessentials.warp.use", "true");
            properties.setProperty("neoessentials.kit.use", "true");
            properties.setProperty("neoessentials.tpa.send", "true");
            properties.setProperty("neoessentials.tpa.receive", "true");
            
            try (FileWriter writer = new FileWriter(defaultGroupFile)) {
                properties.store(writer, "Default group permissions for NeoEssentials");
                NeoEssentials.LOGGER.info("Created default permission group");
            } catch (IOException e) {
                NeoEssentials.LOGGER.error("Failed to create default group", e);
            }
        }
    }
    
    /**
     * Load all group permissions
     */
    private void loadGroups() {
        File groupsDir = new File(GROUPS_DIR);
        File[] groupFiles = groupsDir.listFiles((dir, name) -> name.endsWith(".properties"));
        
        if (groupFiles != null) {
            for (File groupFile : groupFiles) {
                String groupName = groupFile.getName().replace(".properties", "");
                
                try (FileReader reader = new FileReader(groupFile)) {
                    Properties properties = new Properties();
                    properties.load(reader);
                    
                    Map<String, Boolean> permissions = new HashMap<>();
                    for (String key : properties.stringPropertyNames()) {
                        boolean value = Boolean.parseBoolean(properties.getProperty(key));
                        permissions.put(key, value);
                    }
                    
                    groupPermissionCache.put(groupName, permissions);
                    NeoEssentials.LOGGER.debug("Loaded group permissions for: {}", groupName);
                } catch (IOException e) {
                    NeoEssentials.LOGGER.error("Failed to load group permissions: " + groupName, e);
                }
            }
        }
    }
    
    /**
     * Load a player's permissions
     */
    private Map<String, Boolean> loadPlayerPermissions(UUID playerUuid) {
        // Check cache first
        if (playerPermissionCache.containsKey(playerUuid)) {
            return playerPermissionCache.get(playerUuid);
        }
        
        Map<String, Boolean> permissions = new HashMap<>();
        File playerFile = new File(PLAYERS_DIR, playerUuid.toString() + ".properties");
        
        // Load player-specific permissions
        if (playerFile.exists()) {
            try (FileReader reader = new FileReader(playerFile)) {
                Properties properties = new Properties();
                properties.load(reader);
                
                // Special property for group membership
                String groupsProperty = properties.getProperty("groups", DEFAULT_GROUP);
                Set<String> groups = new HashSet<>();
                for (String group : groupsProperty.split(",")) {
                    groups.add(group.trim());
                }
                playerGroups.put(playerUuid, groups);
                
                // Load direct permissions
                for (String key : properties.stringPropertyNames()) {
                    if (!key.equals("groups")) {
                        boolean value = Boolean.parseBoolean(properties.getProperty(key));
                        permissions.put(key, value);
                    }
                }
            } catch (IOException e) {
                NeoEssentials.LOGGER.error("Failed to load player permissions: " + playerUuid, e);
            }
        } else {
            // New player, assign default group
            Set<String> groups = new HashSet<>();
            groups.add(DEFAULT_GROUP);
            playerGroups.put(playerUuid, groups);
        }
        
        // Cache the permissions
        playerPermissionCache.put(playerUuid, permissions);
        return permissions;
    }
    
    /**
     * Get player groups
     */
    public Set<String> getPlayerGroups(UUID playerUuid) {
        // Load player data if not already loaded
        if (!playerGroups.containsKey(playerUuid)) {
            loadPlayerPermissions(playerUuid);
        }
        
        return playerGroups.getOrDefault(playerUuid, Set.of(DEFAULT_GROUP));
    }
    
    /**
     * Check if a player has a permission
     */
    @Override
    public boolean hasPermission(ServerPlayer player, String permission) {
        if (player == null) {
            return false;
        }
        
        // Operators always have permission
        if (player.hasPermissions(2)) {
            return true;
        }
        
        UUID playerUuid = player.getUUID();
        
        // Check player-specific permissions first
        Map<String, Boolean> playerPerms = loadPlayerPermissions(playerUuid);
        if (playerPerms.containsKey(permission)) {
            return playerPerms.get(permission);
        }
        
        // Check wildcard permissions (player-specific)
        String basePermission = getBasePermission(permission);
        if (playerPerms.containsKey(basePermission + ".*")) {
            return playerPerms.get(basePermission + ".*");
        }
        
        // Check group permissions
        Set<String> groups = getPlayerGroups(playerUuid);
        for (String group : groups) {
            Map<String, Boolean> groupPerms = groupPermissionCache.get(group);
            if (groupPerms != null) {
                // Check exact permission
                if (groupPerms.containsKey(permission)) {
                    return groupPerms.get(permission);
                }
                
                // Check wildcard permission
                if (groupPerms.containsKey(basePermission + ".*")) {
                    return groupPerms.get(basePermission + ".*");
                }
            }
        }
        
        // Check global default permissions in config
        if (config != null) {
            Boolean defaultPerm = config.getDefaultPermission(permission);
            if (defaultPerm != null) {
                return defaultPerm;
            }
        }
        
        // Default to false
        return false;
    }
    
    /**
     * Get the base part of a permission (everything before the last dot)
     */
    private String getBasePermission(String permission) {
        int lastDot = permission.lastIndexOf('.');
        if (lastDot > 0) {
            return permission.substring(0, lastDot);
        }
        return permission;
    }
    
    /**
     * Set a permission for a player
     */
    public void setPlayerPermission(UUID playerUuid, String permission, boolean value) {
        // Load player permissions first
        Map<String, Boolean> permissions = loadPlayerPermissions(playerUuid);
        
        // Update the permission
        permissions.put(permission, value);
        
        // Save to disk
        savePlayerPermissions(playerUuid);
    }
    
    /**
     * Add a player to a group
     */
    public boolean addPlayerToGroup(UUID playerUuid, String group) {
        // Check if group exists
        if (!groupPermissionCache.containsKey(group)) {
            return false;
        }
        
        // Get player groups
        Set<String> groups = getPlayerGroups(playerUuid);
        
        // Add to group
        groups.add(group);
        playerGroups.put(playerUuid, groups);
        
        // Save to disk
        savePlayerPermissions(playerUuid);
        return true;
    }
    
    /**
     * Remove a player from a group
     */
    public boolean removePlayerFromGroup(UUID playerUuid, String group) {
        // Cannot remove from default group
        if (group.equals(DEFAULT_GROUP)) {
            return false;
        }
        
        // Get player groups
        Set<String> groups = getPlayerGroups(playerUuid);
        
        // Remove from group
        boolean result = groups.remove(group);
        
        // Ensure player is in at least the default group
        if (groups.isEmpty()) {
            groups.add(DEFAULT_GROUP);
        }
        
        playerGroups.put(playerUuid, groups);
        
        // Save to disk
        savePlayerPermissions(playerUuid);
        return result;
    }
    
    /**
     * Save player permissions to disk
     */
    private void savePlayerPermissions(UUID playerUuid) {
        File playerFile = new File(PLAYERS_DIR, playerUuid.toString() + ".properties");
        Properties properties = new Properties();
        
        // Get the player's permissions
        Map<String, Boolean> permissions = playerPermissionCache.get(playerUuid);
        if (permissions != null) {
            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue().toString());
            }
        }
        
        // Save group memberships
        Set<String> groups = playerGroups.get(playerUuid);
        if (groups != null && !groups.isEmpty()) {
            properties.setProperty("groups", String.join(",", groups));
        } else {
            properties.setProperty("groups", DEFAULT_GROUP);
        }
        
        try (FileWriter writer = new FileWriter(playerFile)) {
            properties.store(writer, "Player permissions for " + playerUuid);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save player permissions: " + playerUuid, e);
        }
    }
    
    /**
     * Create a new permission group
     */
    public boolean createGroup(String groupName) {
        // Check if group already exists
        if (groupPermissionCache.containsKey(groupName)) {
            return false;
        }
        
        // Create a new group
        Map<String, Boolean> permissions = new HashMap<>();
        groupPermissionCache.put(groupName, permissions);
        
        // Save to disk
        return saveGroupPermissions(groupName);
    }
    
    /**
     * Set a permission for a group
     */
    public boolean setGroupPermission(String groupName, String permission, boolean value) {
        // Check if group exists
        if (!groupPermissionCache.containsKey(groupName)) {
            return false;
        }
        
        // Update the permission
        Map<String, Boolean> permissions = groupPermissionCache.get(groupName);
        permissions.put(permission, value);
        
        // Save to disk
        return saveGroupPermissions(groupName);
    }
    
    /**
     * Save group permissions to disk
     */
    private boolean saveGroupPermissions(String groupName) {
        File groupFile = new File(GROUPS_DIR, groupName + ".properties");
        Properties properties = new Properties();
        
        // Get the group's permissions
        Map<String, Boolean> permissions = groupPermissionCache.get(groupName);
        if (permissions != null) {
            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue().toString());
            }
        }
        
        try (FileWriter writer = new FileWriter(groupFile)) {
            properties.store(writer, "Group permissions for " + groupName);
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save group permissions: " + groupName, e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        // This handler is always available as a fallback
        return true;
    }

    @Override
    public String getName() {
        return "NeoEssentials Vanilla Permissions";
    }
}
