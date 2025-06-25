package com.zerog.neoessentials.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.zerog.neoessentials.NeoEssentials;

/**
 * Configuration class for Neo Permissions system
 * This handles the default permissions that are applied when no specific permission
 * is defined for a player or group.
 */
public class NeoPermissionsConfig {

    private static final String CONFIG_DIR = "config/neoessentials";
    private static final String CONFIG_FILE = CONFIG_DIR + "/permissions.properties";
    
    private static NeoPermissionsConfig instance;
    
    private final Map<String, Boolean> defaultPermissions = new HashMap<>();
    
    /**
     * Create a new permissions config
     */
    private NeoPermissionsConfig() {
        load();
    }
    
    /**
     * Get the singleton instance
     */
    public static NeoPermissionsConfig get() {
        if (instance == null) {
            instance = new NeoPermissionsConfig();
        }
        return instance;
    }
    
    /**
     * Load the config from disk
     */
    public void load() {
        try {
            // Create the directory if it doesn't exist
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            File configFile = new File(CONFIG_FILE);
            
            // Create default config if it doesn't exist
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }
            
            // Load the config
            Properties properties = new Properties();
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);
            }
            
            // Parse the permissions
            defaultPermissions.clear();
            for (String key : properties.stringPropertyNames()) {
                boolean value = Boolean.parseBoolean(properties.getProperty(key));
                defaultPermissions.put(key, value);
            }
            
            NeoEssentials.LOGGER.info("Loaded {} default permissions", defaultPermissions.size());
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load permissions config", e);
        }
    }
    
    /**
     * Create the default config file
     */
    private void createDefaultConfig(File configFile) throws IOException {
        Properties properties = new Properties();
        
        // Define default permissions here
        properties.setProperty("neoessentials.home.basic", "true");
        properties.setProperty("neoessentials.warp.use", "true");
        properties.setProperty("neoessentials.kit.basic", "true");
        properties.setProperty("neoessentials.tpa.send", "true");
        properties.setProperty("neoessentials.tpa.receive", "true");
        properties.setProperty("neoessentials.spawn.use", "true");
        properties.setProperty("neoessentials.back", "true");
        
        // Set limits as default false
        properties.setProperty("neoessentials.home.unlimited", "false");
        properties.setProperty("neoessentials.warp.create", "false");
        properties.setProperty("neoessentials.kit.create", "false");
        properties.setProperty("neoessentials.kit.admin", "false");
        
        // Admin commands default to false
        properties.setProperty("neoessentials.admin", "false");
        properties.setProperty("neoessentials.admin.teleport", "false");
        properties.setProperty("neoessentials.admin.inventory", "false");
        properties.setProperty("neoessentials.admin.gamemode", "false");
        properties.setProperty("neoessentials.admin.weather", "false");
        properties.setProperty("neoessentials.admin.time", "false");
        
        // Moderator commands default to false
        properties.setProperty("neoessentials.moderator", "false");
        properties.setProperty("neoessentials.moderator.kick", "false");
        properties.setProperty("neoessentials.moderator.ban", "false");
        properties.setProperty("neoessentials.moderator.mute", "false");
        properties.setProperty("neoessentials.moderator.jail", "false");
        
        // Tablist features 
        properties.setProperty("neoessentials.tablist.view", "true");
        properties.setProperty("neoessentials.tablist.admin", "false");
        
        try (FileWriter writer = new FileWriter(configFile)) {
            properties.store(writer, "Default permissions for NeoEssentials");
        }
        
        NeoEssentials.LOGGER.info("Created default permissions config");
    }
    
    /**
     * Get a default permission value
     * 
     * @param permission The permission node to check
     * @return The permission value, or null if not defined
     */
    public Boolean getDefaultPermission(String permission) {
        // Check exact permission
        if (defaultPermissions.containsKey(permission)) {
            return defaultPermissions.get(permission);
        }
        
        // Check for wildcard permissions
        int lastDot = permission.lastIndexOf('.');
        if (lastDot > 0) {
            String basePermission = permission.substring(0, lastDot);
            if (defaultPermissions.containsKey(basePermission + ".*")) {
                return defaultPermissions.get(basePermission + ".*");
            }
            
            // Check parent wildcards recursively
            String parentPerm = basePermission;
            while (parentPerm.contains(".")) {
                lastDot = parentPerm.lastIndexOf('.');
                parentPerm = parentPerm.substring(0, lastDot);
                
                if (defaultPermissions.containsKey(parentPerm + ".*")) {
                    return defaultPermissions.get(parentPerm + ".*");
                }
            }
        }
        
        // No specific permission found
        return null;
    }
    
    /**
     * Set a default permission
     * 
     * @param permission The permission node
     * @param value The value to set
     */
    public void setDefaultPermission(String permission, boolean value) {
        defaultPermissions.put(permission, value);
        save();
    }
    
    /**
     * Save the config to disk
     */
    public void save() {
        try {
            File configFile = new File(CONFIG_FILE);
            Properties properties = new Properties();
            
            // Write all permissions
            for (Map.Entry<String, Boolean> entry : defaultPermissions.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue().toString());
            }
            
            try (FileWriter writer = new FileWriter(configFile)) {
                properties.store(writer, "Default permissions for NeoEssentials");
            }
            
            NeoEssentials.LOGGER.debug("Saved permissions config");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to save permissions config", e);
        }
    }
}
