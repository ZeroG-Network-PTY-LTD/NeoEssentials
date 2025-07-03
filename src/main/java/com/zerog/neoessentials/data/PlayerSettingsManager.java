package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player settings and preferences.
 * Handles saving and loading player preferences to/from JSON files.
 */
public class PlayerSettingsManager {
    
    private static final String SETTINGS_DIR = "neoessentials/player_settings";
    private static final String SETTINGS_FILE = "player_settings.json";
    
    // In-memory storage for quick access
    private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
    
    private final Gson gson;
    private final Path settingsFile;
    
    public PlayerSettingsManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Initialize storage directory
        Path settingsDir = Paths.get(SETTINGS_DIR);
        try {
            Files.createDirectories(settingsDir);
            this.settingsFile = settingsDir.resolve(SETTINGS_FILE);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create player settings directory: {}", e.getMessage());
            throw new RuntimeException("Could not initialize player settings storage", e);
        }
        
        loadSettings();
    }
    
    /**
     * Gets a player's settings, creating default settings if none exist
     * 
     * @param playerId The player's UUID
     * @return The player's settings
     */
    public PlayerSettings getPlayerSettings(UUID playerId) {
        return playerSettings.computeIfAbsent(playerId, k -> new PlayerSettings());
    }
    
    /**
     * Gets a player's settings, creating default settings if none exist
     * 
     * @param player The player
     * @return The player's settings
     */
    public PlayerSettings getPlayerSettings(ServerPlayer player) {
        return getPlayerSettings(player.getUUID());
    }
    
    /**
     * Updates a player's settings
     * 
     * @param playerId The player's UUID
     * @param settings The new settings
     */
    public void updatePlayerSettings(UUID playerId, PlayerSettings settings) {
        playerSettings.put(playerId, settings);
        saveSettings();
    }
    
    /**
     * Updates a player's settings
     * 
     * @param player The player
     * @param settings The new settings
     */
    public void updatePlayerSettings(ServerPlayer player, PlayerSettings settings) {
        updatePlayerSettings(player.getUUID(), settings);
    }
    
    /**
     * Sets a specific setting for a player
     * 
     * @param playerId The player's UUID
     * @param key The setting key
     * @param value The setting value
     */
    public void setSetting(UUID playerId, String key, Object value) {
        PlayerSettings settings = getPlayerSettings(playerId);
        settings.setSetting(key, value);
        saveSettings();
    }
    
    /**
     * Gets a specific setting for a player
     * 
     * @param playerId The player's UUID
     * @param key The setting key
     * @param defaultValue The default value if not found
     * @return The setting value
     */
    public <T> T getSetting(UUID playerId, String key, T defaultValue) {
        PlayerSettings settings = getPlayerSettings(playerId);
        return settings.getSetting(key, defaultValue);
    }
    
    /**
     * Saves all player settings to disk
     */
    private void saveSettings() {
        try {
            // Convert to saveable format
            Map<String, PlayerSettings> saveData = new HashMap<>();
            for (Map.Entry<UUID, PlayerSettings> entry : playerSettings.entrySet()) {
                saveData.put(entry.getKey().toString(), entry.getValue());
            }
            
            try (FileWriter writer = new FileWriter(settingsFile.toFile())) {
                gson.toJson(saveData, writer);
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save player settings: {}", e.getMessage());
        }
    }
    
    /**
     * Loads player settings from disk
     */
    private void loadSettings() {
        if (!Files.exists(settingsFile)) {
            return;
        }
        
        try (FileReader reader = new FileReader(settingsFile.toFile())) {
            Type type = new TypeToken<Map<String, PlayerSettings>>(){}.getType();
            Map<String, PlayerSettings> loadData = gson.fromJson(reader, type);
            
            if (loadData != null) {
                for (Map.Entry<String, PlayerSettings> entry : loadData.entrySet()) {
                    UUID playerId = UUID.fromString(entry.getKey());
                    playerSettings.put(playerId, entry.getValue());
                }
            }
            
            NeoEssentials.LOGGER.info("Loaded settings for {} players", playerSettings.size());
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to load player settings: {}", e.getMessage());
        }
    }
    
    /**
     * Saves all data when the server shuts down
     */
    public void shutdown() {
        saveSettings();
    }
    
    /**
     * Class to store player settings and preferences
     */
    public static class PlayerSettings {
        // Teleportation settings
        private boolean autoRecordTeleports = true;
        private boolean teleportConfirmations = false;
        private int maxTeleportHistory = 25;
        
        // GUI settings
        private boolean preferGUIInterfaces = true;
        private String guiTheme = "default";
        
        // Chat and messaging settings
        private boolean showTeleportMessages = true;
        private boolean showEconomyMessages = true;
        private boolean showSystemMessages = true;
        
        // Economy settings
        private boolean autoPaymentConfirmations = false;
        private String preferredCurrency = "default";
        
        // World management settings
        private boolean showWorldChangeMessages = true;
        private boolean autoWorldSpawn = false;
        
        // Privacy settings
        private boolean allowTeleportRequests = true;
        private boolean showOnlineStatus = true;
        private boolean allowPlayerInfo = true;
        
        // Additional custom settings
        private final Map<String, Object> customSettings = new HashMap<>();
        
        public PlayerSettings() {
            // Default constructor with default values
        }
        
        // Getters and setters
        public boolean isAutoRecordTeleports() { return autoRecordTeleports; }
        public void setAutoRecordTeleports(boolean autoRecordTeleports) { this.autoRecordTeleports = autoRecordTeleports; }
        
        public boolean isTeleportConfirmations() { return teleportConfirmations; }
        public void setTeleportConfirmations(boolean teleportConfirmations) { this.teleportConfirmations = teleportConfirmations; }
        
        public int getMaxTeleportHistory() { return maxTeleportHistory; }
        public void setMaxTeleportHistory(int maxTeleportHistory) { this.maxTeleportHistory = maxTeleportHistory; }
        
        public boolean isPreferGUIInterfaces() { return preferGUIInterfaces; }
        public void setPreferGUIInterfaces(boolean preferGUIInterfaces) { this.preferGUIInterfaces = preferGUIInterfaces; }
        
        public String getGuiTheme() { return guiTheme; }
        public void setGuiTheme(String guiTheme) { this.guiTheme = guiTheme; }
        
        public boolean isShowTeleportMessages() { return showTeleportMessages; }
        public void setShowTeleportMessages(boolean showTeleportMessages) { this.showTeleportMessages = showTeleportMessages; }
        
        public boolean isShowEconomyMessages() { return showEconomyMessages; }
        public void setShowEconomyMessages(boolean showEconomyMessages) { this.showEconomyMessages = showEconomyMessages; }
        
        public boolean isShowSystemMessages() { return showSystemMessages; }
        public void setShowSystemMessages(boolean showSystemMessages) { this.showSystemMessages = showSystemMessages; }
        
        public boolean isAutoPaymentConfirmations() { return autoPaymentConfirmations; }
        public void setAutoPaymentConfirmations(boolean autoPaymentConfirmations) { this.autoPaymentConfirmations = autoPaymentConfirmations; }
        
        public String getPreferredCurrency() { return preferredCurrency; }
        public void setPreferredCurrency(String preferredCurrency) { this.preferredCurrency = preferredCurrency; }
        
        public boolean isShowWorldChangeMessages() { return showWorldChangeMessages; }
        public void setShowWorldChangeMessages(boolean showWorldChangeMessages) { this.showWorldChangeMessages = showWorldChangeMessages; }
        
        public boolean isAutoWorldSpawn() { return autoWorldSpawn; }
        public void setAutoWorldSpawn(boolean autoWorldSpawn) { this.autoWorldSpawn = autoWorldSpawn; }
        
        public boolean isAllowTeleportRequests() { return allowTeleportRequests; }
        public void setAllowTeleportRequests(boolean allowTeleportRequests) { this.allowTeleportRequests = allowTeleportRequests; }
        
        public boolean isShowOnlineStatus() { return showOnlineStatus; }
        public void setShowOnlineStatus(boolean showOnlineStatus) { this.showOnlineStatus = showOnlineStatus; }
        
        public boolean isAllowPlayerInfo() { return allowPlayerInfo; }
        public void setAllowPlayerInfo(boolean allowPlayerInfo) { this.allowPlayerInfo = allowPlayerInfo; }
        
        // Custom settings
        public void setSetting(String key, Object value) {
            customSettings.put(key, value);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getSetting(String key, T defaultValue) {
            return (T) customSettings.getOrDefault(key, defaultValue);
        }
        
        public Map<String, Object> getCustomSettings() {
            return new HashMap<>(customSettings);
        }
    }
}
