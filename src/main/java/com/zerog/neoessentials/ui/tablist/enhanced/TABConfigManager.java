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
        // Apply settings section first
        Map<String, Object> settings = getMapOrDefault(config, "settings");
        applySettings(settings);
        
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
        
        // Apply bossbars configuration
        Map<String, Object> bossbars = getMapOrDefault(config, "bossbars");
        if (!bossbars.isEmpty()) {
            currentConfig.setBossBarEnabled(true);
            NeoEssentials.LOGGER.info("Loaded bossbar configurations");
        }
    }
    
    /**
     * Apply settings from the settings section
     */
    private void applySettings(Map<String, Object> settings) {
        if (settings.isEmpty()) {
            NeoEssentials.LOGGER.debug("No settings section found, using defaults");
            return;
        }
        
        NeoEssentials.LOGGER.info("Applying tablist settings from YAML configuration");
        
        // Update interval
        if (settings.containsKey("update_interval")) {
            Object interval = settings.get("update_interval");
            if (interval instanceof Number) {
                long intervalMs = ((Number) interval).longValue();
                currentConfig.setUpdateInterval(intervalMs);
                NeoEssentials.LOGGER.info("Set update interval to: {}ms", intervalMs);
            }
        }
        
        // Header animation interval
        if (settings.containsKey("header_animation_interval")) {
            Object interval = settings.get("header_animation_interval");
            if (interval instanceof Number) {
                int intervalTicks = ((Number) interval).intValue();
                currentConfig.setHeaderAnimationInterval(intervalTicks);
                NeoEssentials.LOGGER.info("Set header animation interval to: {} ticks ({}ms)", 
                                         intervalTicks, intervalTicks * 50);
            }
        }
        
        // Footer animation interval
        if (settings.containsKey("footer_animation_interval")) {
            Object interval = settings.get("footer_animation_interval");
            if (interval instanceof Number) {
                int intervalTicks = ((Number) interval).intValue();
                currentConfig.setFooterAnimationInterval(intervalTicks);
                NeoEssentials.LOGGER.info("Set footer animation interval to: {} ticks ({}ms)", 
                                         intervalTicks, intervalTicks * 50);
            }
        }
        
        // Placeholder update interval
        if (settings.containsKey("placeholder_update_interval")) {
            Object interval = settings.get("placeholder_update_interval");
            if (interval instanceof Number) {
                long intervalMs = ((Number) interval).longValue();
                currentConfig.setPlaceholderUpdateInterval(intervalMs);
                NeoEssentials.LOGGER.info("Set placeholder update interval to: {}ms", intervalMs);
            }
        }
        
        // Enable/disable headers and footers
        boolean enableHeaders = getBooleanOrDefault(settings, "enable_headers", true);
        boolean enableFooters = getBooleanOrDefault(settings, "enable_footers", true);
        
        // For TAB compatibility, headers and footers are controlled together
        boolean headerFooterEnabled = enableHeaders || enableFooters;
        currentConfig.setHeaderFooterEnabled(headerFooterEnabled);
        NeoEssentials.LOGGER.info("Header/Footer system enabled: {} (headers: {}, footers: {})", 
                                 headerFooterEnabled, enableHeaders, enableFooters);
        
        // Store individual settings for later use if needed
        currentConfig.setEnableHeaders(enableHeaders);
        currentConfig.setEnableFooters(enableFooters);
        
        // Enable/disable boss bars
        if (settings.containsKey("enable_bossbars")) {
            boolean enableBossbars = getBooleanOrDefault(settings, "enable_bossbars", false);
            currentConfig.setBossBarEnabled(enableBossbars);
            NeoEssentials.LOGGER.info("Boss bars enabled: {}", enableBossbars);
        }
        
        // Enable/disable animations
        if (settings.containsKey("enable_animations")) {
            boolean enableAnimations = getBooleanOrDefault(settings, "enable_animations", true);
            currentConfig.setAnimationsEnabled(enableAnimations);
            NeoEssentials.LOGGER.info("Animations enabled: {}", enableAnimations);
        }
        
        // Enable/disable group-specific templates
        if (settings.containsKey("enable_group_specific")) {
            boolean enableGroupSpecific = getBooleanOrDefault(settings, "enable_group_specific", true);
            currentConfig.setGroupSpecificEnabled(enableGroupSpecific);
            NeoEssentials.LOGGER.info("Group-specific templates enabled: {}", enableGroupSpecific);
        }
    }
    
    /**
     * Apply animations configuration values to TABConfig
     */
    private void applyAnimationsConfig(Map<String, Object> config) {
        // Store animations data for the TablistAnimationManager
        Map<String, Object> animations = getMapOrDefault(config, "animations");
        if (!animations.isEmpty()) {
            currentConfig.setAnimationsData(animations);
            NeoEssentials.LOGGER.info("Loaded {} animation configurations from YAML", animations.size());
        } else {
            NeoEssentials.LOGGER.warn("No animations found in animations.yml");
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
