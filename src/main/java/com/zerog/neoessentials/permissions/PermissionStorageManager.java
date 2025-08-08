package com.zerog.neoessentials.permissions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Dedicated permission storage manager for persistent permissions
 * Handles both group definitions and player permission assignments
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionStorageManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionStorageManager.class);
    private static PermissionStorageManager instance;
    
    private final Gson gson;
    private final Path permissionsDirectory;
    private final Path groupsFile;
    private final Path playersDirectory;
    private final Path backupDirectory;
    
    // In-memory cache for quick access
    private final Map<String, PermissionGroup> groupCache;
    private final Map<UUID, PlayerPermissionData> playerCache;
    
    private PermissionStorageManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
            
        this.permissionsDirectory = Paths.get("neoessentials", "permissions");
        this.groupsFile = permissionsDirectory.resolve("groups.json");
        this.playersDirectory = permissionsDirectory.resolve("players");
        this.backupDirectory = permissionsDirectory.resolve("backups");
        
        this.groupCache = new ConcurrentHashMap<>();
        this.playerCache = new ConcurrentHashMap<>();
        
        initializeStorage();
    }
    
    public static PermissionStorageManager getInstance() {
        if (instance == null) {
            instance = new PermissionStorageManager();
        }
        return instance;
    }
    
    /**
     * Initialize the permission storage system
     */
    private void initializeStorage() {
        try {
            // Create directories if they don't exist
            Files.createDirectories(permissionsDirectory);
            Files.createDirectories(playersDirectory);
            Files.createDirectories(backupDirectory);
            
            LOGGER.info("Initialized permission storage at: {}", permissionsDirectory);
            
            // Load existing data
            loadGroups();
            
        } catch (IOException e) {
            LOGGER.error("Failed to initialize permission storage", e);
        }
    }
    
    // =======================
    // GROUP STORAGE METHODS
    // =======================
    
    /**
     * Save all groups to storage
     */
    public void saveGroups(Map<String, PermissionGroup> groups) {
        try {
            // Create backup before saving
            createGroupBackup();
            
            // Convert groups to a serializable format
            Map<String, GroupData> groupData = new HashMap<>();
            for (Map.Entry<String, PermissionGroup> entry : groups.entrySet()) {
                groupData.put(entry.getKey(), new GroupData(entry.getValue()));
            }
            
            // Write to file
            try (FileWriter writer = new FileWriter(groupsFile.toFile())) {
                gson.toJson(groupData, writer);
            }
            
            // Update cache
            groupCache.clear();
            groupCache.putAll(groups);
            
            LOGGER.info("Saved {} permission groups to storage", groups.size());
            
        } catch (IOException e) {
            LOGGER.error("Failed to save permission groups", e);
        }
    }
    
    /**
     * Load groups from storage
     */
    public Map<String, PermissionGroup> loadGroups() {
        try {
            if (!Files.exists(groupsFile)) {
                LOGGER.info("No groups file found, will create default groups");
                return new HashMap<>();
            }
            
            try (FileReader reader = new FileReader(groupsFile.toFile())) {
                Type type = new TypeToken<Map<String, GroupData>>(){}.getType();
                Map<String, GroupData> groupData = gson.fromJson(reader, type);
                
                if (groupData == null) {
                    LOGGER.warn("Groups file is empty or invalid");
                    return new HashMap<>();
                }
                
                // Convert back to PermissionGroup objects
                Map<String, PermissionGroup> groups = new HashMap<>();
                for (Map.Entry<String, GroupData> entry : groupData.entrySet()) {
                    groups.put(entry.getKey(), entry.getValue().toPermissionGroup());
                }
                
                // Update cache
                groupCache.clear();
                groupCache.putAll(groups);
                
                LOGGER.info("Loaded {} permission groups from storage", groups.size());
                return groups;
                
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load permission groups", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Save a single group
     */
    public void saveGroup(String groupName, PermissionGroup group) {
        Map<String, PermissionGroup> currentGroups = loadGroups();
        currentGroups.put(groupName, group);
        saveGroups(currentGroups);
    }
    
    /**
     * Delete a group
     */
    public void deleteGroup(String groupName) {
        Map<String, PermissionGroup> currentGroups = loadGroups();
        currentGroups.remove(groupName);
        saveGroups(currentGroups);
        groupCache.remove(groupName);
    }
    
    // =======================
    // PLAYER STORAGE METHODS
    // =======================
    
    /**
     * Save player permission data
     */
    public void savePlayerData(UUID playerUUID, String groupName, Map<String, Boolean> permissions) {
        try {
            PlayerPermissionData data = new PlayerPermissionData(playerUUID, groupName, permissions);
            
            Path playerFile = playersDirectory.resolve(playerUUID + ".json");
            
            // Create backup if file exists
            if (Files.exists(playerFile)) {
                createPlayerBackup(playerUUID);
            }
            
            // Write to file
            try (FileWriter writer = new FileWriter(playerFile.toFile())) {
                gson.toJson(data, writer);
            }
            
            // Update cache
            playerCache.put(playerUUID, data);
            
            LOGGER.debug("Saved permission data for player: {}", playerUUID);
            
        } catch (IOException e) {
            LOGGER.error("Failed to save permission data for player: {}", playerUUID, e);
        }
    }
    
    /**
     * Load player permission data
     */
    public PlayerPermissionData loadPlayerData(UUID playerUUID) {
        try {
            // Check cache first
            if (playerCache.containsKey(playerUUID)) {
                return playerCache.get(playerUUID);
            }
            
            Path playerFile = playersDirectory.resolve(playerUUID + ".json");
            
            if (!Files.exists(playerFile)) {
                // Return default data for new players
                PlayerPermissionData defaultData = new PlayerPermissionData(playerUUID, "default", new HashMap<>());
                playerCache.put(playerUUID, defaultData);
                return defaultData;
            }
            
            try (FileReader reader = new FileReader(playerFile.toFile())) {
                PlayerPermissionData data = gson.fromJson(reader, PlayerPermissionData.class);
                
                if (data == null) {
                    LOGGER.warn("Player permission file is empty or invalid: {}", playerUUID);
                    PlayerPermissionData defaultData = new PlayerPermissionData(playerUUID, "default", new HashMap<>());
                    playerCache.put(playerUUID, defaultData);
                    return defaultData;
                }
                
                // Update cache
                playerCache.put(playerUUID, data);
                
                LOGGER.debug("Loaded permission data for player: {}", playerUUID);
                return data;
                
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load permission data for player: {}", playerUUID, e);
            // Return default data on error
            PlayerPermissionData defaultData = new PlayerPermissionData(playerUUID, "default", new HashMap<>());
            playerCache.put(playerUUID, defaultData);
            return defaultData;
        }
    }
    
    /**
     * Save player group assignment
     */
    public void savePlayerGroup(UUID playerUUID, String groupName) {
        PlayerPermissionData data = loadPlayerData(playerUUID);
        data.groupName = groupName;
        data.lastUpdated = System.currentTimeMillis();
        savePlayerData(playerUUID, data.groupName, data.permissions);
    }
    
    /**
     * Save player individual permission
     */
    public void savePlayerPermission(UUID playerUUID, String permission, boolean value) {
        PlayerPermissionData data = loadPlayerData(playerUUID);
        data.permissions.put(permission, value);
        data.lastUpdated = System.currentTimeMillis();
        savePlayerData(playerUUID, data.groupName, data.permissions);
    }
    
    /**
     * Remove player individual permission
     */
    public void removePlayerPermission(UUID playerUUID, String permission) {
        PlayerPermissionData data = loadPlayerData(playerUUID);
        data.permissions.remove(permission);
        data.lastUpdated = System.currentTimeMillis();
        savePlayerData(playerUUID, data.groupName, data.permissions);
    }
    
    /**
     * Get all players in a specific group
     */
    public List<UUID> getPlayersInGroup(String groupName) {
        List<UUID> playersInGroup = new ArrayList<>();
        
        try {
            Files.list(playersDirectory)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        String fileName = path.getFileName().toString();
                        UUID playerUUID = UUID.fromString(fileName.replace(".json", ""));
                        PlayerPermissionData data = loadPlayerData(playerUUID);
                        
                        if (groupName.equals(data.groupName)) {
                            playersInGroup.add(playerUUID);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to process player file: {}", path, e);
                    }
                });
                
        } catch (IOException e) {
            LOGGER.error("Failed to list players in group: {}", groupName, e);
        }
        
        return playersInGroup;
    }
    
    // =======================
    // BACKUP METHODS
    // =======================
    
    /**
     * Create backup of groups file
     */
    private void createGroupBackup() {
        try {
            if (Files.exists(groupsFile)) {
                Path backupFile = backupDirectory.resolve("groups_" + System.currentTimeMillis() + ".json");
                Files.copy(groupsFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.debug("Created groups backup: {}", backupFile);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create groups backup", e);
        }
    }
    
    /**
     * Create backup of player permission file
     */
    private void createPlayerBackup(UUID playerUUID) {
        try {
            Path playerFile = playersDirectory.resolve(playerUUID + ".json");
            if (Files.exists(playerFile)) {
                Path backupFile = backupDirectory.resolve(playerUUID + "_" + System.currentTimeMillis() + ".json");
                Files.copy(playerFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.debug("Created player backup: {}", backupFile);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create player backup for: {}", playerUUID, e);
        }
    }
    
    // =======================
    // CACHE MANAGEMENT
    // =======================
    
    /**
     * Clear all caches
     */
    public void clearCache() {
        groupCache.clear();
        playerCache.clear();
        LOGGER.info("Cleared permission caches");
    }
    
    /**
     * Clear player cache entry
     */
    public void clearPlayerCache(UUID playerUUID) {
        playerCache.remove(playerUUID);
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("groups_cached", groupCache.size());
        stats.put("players_cached", playerCache.size());
        return stats;
    }
    
    // =======================
    // DATA CLASSES
    // =======================
    
    /**
     * Serializable group data
     */
    public static class GroupData {
        public String name;
        public String prefix;
        public String suffix;
        public int priority;
        public String inheritance;
        public Set<String> permissions;
        public long lastUpdated;
        
        public GroupData() {
            this.permissions = new HashSet<>();
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public GroupData(PermissionGroup group) {
            this.name = group.getName();
            this.prefix = group.getPrefix();
            this.suffix = group.getSuffix();
            this.priority = group.getPriority();
            this.inheritance = group.getInheritance();
            this.permissions = new HashSet<>(group.getDirectPermissions());
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public PermissionGroup toPermissionGroup() {
            PermissionGroup group = new PermissionGroup(name, prefix, suffix, priority);
            if (inheritance != null && !inheritance.isEmpty()) {
                group.setInheritance(inheritance);
            }
            for (String permission : permissions) {
                group.addPermission(permission);
            }
            return group;
        }
    }
    
    /**
     * Player permission data
     */
    public static class PlayerPermissionData {
        public UUID playerUUID;
        public String groupName;
        public Map<String, Boolean> permissions;
        public long lastUpdated;
        
        public PlayerPermissionData() {
            this.permissions = new HashMap<>();
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public PlayerPermissionData(UUID playerUUID, String groupName, Map<String, Boolean> permissions) {
            this.playerUUID = playerUUID;
            this.groupName = groupName;
            this.permissions = new HashMap<>(permissions);
            this.lastUpdated = System.currentTimeMillis();
        }
    }
}
