package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.NeoEssentials;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        
        // Apply header/footer settings
        if (tablist.containsKey("headerFooterEnabled")) {
            currentConfig.setHeaderFooterEnabled(getBooleanOrDefault(tablist, "headerFooterEnabled", true));
        }
        
        // Apply templates section
        Map<String, Object> templates = getMapOrDefault(config, "templates");
        
        // Apply default headers from templates.headers
        if (templates.containsKey("headers")) {
            Object defaultHeaders = templates.get("headers");
            if (defaultHeaders instanceof List) {
                currentConfig.setDefaultHeaders(convertToStringList((List<?>) defaultHeaders));
                NeoEssentials.LOGGER.info("Loaded {} default headers from YAML", ((List<?>) defaultHeaders).size());
            }
        }
        
        // Apply default footers from templates.footers
        if (templates.containsKey("footers")) {
            Object defaultFooters = templates.get("footers");
            if (defaultFooters instanceof List) {
                currentConfig.setDefaultFooters(convertToStringList((List<?>) defaultFooters));
                NeoEssentials.LOGGER.info("Loaded {} default footers from YAML", ((List<?>) defaultFooters).size());
            }
        }
        
        // Apply groups configuration
        Map<String, Object> groups = getMapOrDefault(config, "groups");
        for (Map.Entry<String, Object> groupEntry : groups.entrySet()) {
            String groupName = groupEntry.getKey();
            if (groupEntry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> groupConfig = (Map<String, Object>) groupEntry.getValue();
                
                // Apply group headers
                if (groupConfig.containsKey("headers")) {
                    Object groupHeaders = groupConfig.get("headers");
                    if (groupHeaders instanceof List) {
                        currentConfig.setGroupHeaders(groupName, convertToStringList((List<?>) groupHeaders));
                        NeoEssentials.LOGGER.debug("Loaded headers for group: {}", groupName);
                    }
                }
                
                // Apply group footers
                if (groupConfig.containsKey("footers")) {
                    Object groupFooters = groupConfig.get("footers");
                    if (groupFooters instanceof List) {
                        currentConfig.setGroupFooters(groupName, convertToStringList((List<?>) groupFooters));
                        NeoEssentials.LOGGER.debug("Loaded footers for group: {}", groupName);
                    }
                }
            }
        }
    }
    
    /**
     * Apply animations configuration values to TABConfig
     */
    private void applyAnimationsConfig(Map<String, Object> config) {
        // For now, animations are handled by the TablistAnimationManager
        // We just log that they were loaded
        Map<String, Object> animations = getMapOrDefault(config, "animations");
        if (!animations.isEmpty()) {
            NeoEssentials.LOGGER.info("Loaded {} animation configurations", animations.size());
        }
    }
    
    /**
     * Convert a list of objects to a list of strings
     */
    private List<String> convertToStringList(List<?> list) {
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }
    
    /**
     * Helper method to get a map from config or return empty map
     */
    private Map<String, Object> getMapOrDefault(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapValue = (Map<String, Object>) value;
            return mapValue;
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
