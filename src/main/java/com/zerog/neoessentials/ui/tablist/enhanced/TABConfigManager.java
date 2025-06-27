package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.NeoEssentials;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration manager for TAB-like system
 * Handles loading, saving, and reloading of TAB configuration from YAML files
 */
public class TABConfigManager {
    
    private TABConfig currentConfig;
    private final Yaml yaml = new Yaml();
    
    // Configuration file paths - load from neoessentials/ directory in server root
    private static final Path TABLIST_CONFIG_PATH = Paths.get("neoessentials", "tablist.yml");
    private static final Path ANIMATIONS_CONFIG_PATH = Paths.get("neoessentials", "animations.yml");
    
    /**
     * Load configuration from YAML files in neoessentials/ directory
     * @return The loaded configuration
     */
    public TABConfig loadConfig() {
        try {
            currentConfig = new TABConfig();
            
            // Load tablist configuration
            if (Files.exists(TABLIST_CONFIG_PATH)) {
                loadTablistConfig();
                NeoEssentials.LOGGER.info("TAB tablist configuration loaded from: {}", TABLIST_CONFIG_PATH);
            } else {
                NeoEssentials.LOGGER.warn("Tablist config file not found: {}, using defaults", TABLIST_CONFIG_PATH);
            }
            
            // Load animations configuration
            if (Files.exists(ANIMATIONS_CONFIG_PATH)) {
                loadAnimationsConfig();
                NeoEssentials.LOGGER.info("TAB animations configuration loaded from: {}", ANIMATIONS_CONFIG_PATH);
            } else {
                NeoEssentials.LOGGER.warn("Animations config file not found: {}, using defaults", ANIMATIONS_CONFIG_PATH);
            }
            
            NeoEssentials.LOGGER.info("TAB configuration loaded");
            return currentConfig;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load TAB configuration", e);
            return new TABConfig(); // Return default config
        }
    }
    
    /**
     * Load tablist configuration from neoessentials/tablist.yml
     */
    private void loadTablistConfig() throws IOException {
        try (FileInputStream input = new FileInputStream(TABLIST_CONFIG_PATH.toFile())) {
            Map<String, Object> config = yaml.load(input);
            if (config != null) {
                applyTablistConfig(config);
            }
        }
    }
    
    /**
     * Load animations configuration from neoessentials/animations.yml
     */
    private void loadAnimationsConfig() throws IOException {
        try (FileInputStream input = new FileInputStream(ANIMATIONS_CONFIG_PATH.toFile())) {
            Map<String, Object> config = yaml.load(input);
            if (config != null) {
                applyAnimationsConfig(config);
            }
        }
    }
    
    /**
     * Apply tablist configuration values to TABConfig
     */
    private void applyTablistConfig(Map<String, Object> config) {
        // Extract tablist section
        Map<String, Object> tablist = getMapOrDefault(config, "tablist");
        
        // Apply basic settings
        if (tablist.containsKey("enabled")) {
            currentConfig.setEnabled(getBooleanOrDefault(tablist, "enabled", true));
        }
        
        if (tablist.containsKey("updateInterval")) {
            Object interval = tablist.get("updateInterval");
            if (interval instanceof Number) {
                currentConfig.setUpdateInterval(((Number) interval).longValue());
            }
        }
        
        // Apply header/footer templates
        Map<String, Object> headers = getMapOrDefault(config, "headers");
        currentConfig.setHeaders(headers);
        
        Map<String, Object> footers = getMapOrDefault(config, "footers");
        currentConfig.setFooters(footers);
        
        // Apply groups configuration
        Map<String, Object> groups = getMapOrDefault(config, "groups");
        currentConfig.setGroups(groups);
    }
    
    /**
     * Apply animations configuration values to TABConfig
     */
    private void applyAnimationsConfig(Map<String, Object> config) {
        // Apply animations
        Map<String, Object> animations = getMapOrDefault(config, "animations");
        currentConfig.setAnimations(animations);
    }
    
    /**
     * Helper method to get a map from config or return empty map
     */
    private Map<String, Object> getMapOrDefault(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }
    
    /**
     * Helper method to get a boolean value or return default
     */
    private boolean getBooleanOrDefault(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * Save configuration to file system
     * @param config The configuration to save
     */
    public void saveConfig(TABConfig config) {
        try {
            // TODO: Implement saving to configuration file
            this.currentConfig = config;
            NeoEssentials.LOGGER.info("TAB configuration saved");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to save TAB configuration", e);
        }
    }
    
    /**
     * Reload configuration from disk
     * @return The reloaded configuration
     */
    public TABConfig reloadConfig() {
        NeoEssentials.LOGGER.info("Reloading TAB configuration");
        return loadConfig();
    }
    
    /**
     * Get the current configuration
     * @return The current configuration
     */
    public TABConfig getCurrentConfig() {
        return currentConfig != null ? currentConfig : new TABConfig();
    }
}
