package com.zerog.neoessentials.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Discord configuration manager for NeoEssentials
 * Handles Discord integration settings and configuration
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordConfig.class);
    
    private final Path configPath;
    private final Properties properties;
    
    // Configuration keys
    public static final String WEBHOOK_URL = "webhook_url";
    public static final String SERVER_NAME = "server_name";
    public static final String SERVER_ICON = "server_icon";
    public static final String NOTIFY_JOIN = "notify_player_join";
    public static final String NOTIFY_LEAVE = "notify_player_leave";
    public static final String NOTIFY_SERVER_START = "notify_server_start";
    public static final String NOTIFY_SERVER_STOP = "notify_server_stop";
    public static final String ENABLED = "enabled";
    
    public DiscordConfig(Path configPath) {
        this.configPath = configPath;
        this.properties = new Properties();
        
        // Load or create config
        loadConfig();
    }
    
    /**
     * Load configuration from file
     */
    private void loadConfig() {
        try {
            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    properties.load(input);
                }
                LOGGER.info("Loaded Discord configuration from: " + configPath);
            } else {
                createDefaultConfig();
                saveConfig();
                LOGGER.info("Created default Discord configuration at: " + configPath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load Discord configuration", e);
            createDefaultConfig();
        }
    }
    
    /**
     * Create default configuration
     */
    private void createDefaultConfig() {
        properties.setProperty(ENABLED, "false");
        properties.setProperty(WEBHOOK_URL, "");
        properties.setProperty(SERVER_NAME, "NeoEssentials Server");
        properties.setProperty(SERVER_ICON, "https://i.imgur.com/default.png");
        properties.setProperty(NOTIFY_JOIN, "true");
        properties.setProperty(NOTIFY_LEAVE, "true");
        properties.setProperty(NOTIFY_SERVER_START, "true");
        properties.setProperty(NOTIFY_SERVER_STOP, "true");
    }
    
    /**
     * Save configuration to file
     */
    public void saveConfig() {
        try {
            // Ensure parent directory exists
            Files.createDirectories(configPath.getParent());
            
            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(output, "NeoEssentials Discord Integration Configuration");
            }
            
            LOGGER.info("Saved Discord configuration to: " + configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save Discord configuration", e);
        }
    }
    
    /**
     * Get string property
     */
    public String getString(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Get string property with default
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get boolean property
     */
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key, "false"));
    }
    
    /**
     * Get boolean property with default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }
    
    /**
     * Set string property
     */
    public void setString(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * Set boolean property
     */
    public void setBoolean(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    /**
     * Check if Discord integration is enabled
     */
    public boolean isEnabled() {
        return getBoolean(ENABLED) && !getString(WEBHOOK_URL, "").isEmpty();
    }
    
    /**
     * Get webhook URL
     */
    public String getWebhookUrl() {
        return getString(WEBHOOK_URL, "");
    }
    
    /**
     * Get server name
     */
    public String getServerName() {
        return getString(SERVER_NAME, "NeoEssentials Server");
    }
    
    /**
     * Get server icon URL
     */
    public String getServerIcon() {
        return getString(SERVER_ICON, "https://i.imgur.com/default.png");
    }
    
    /**
     * Check if player join notifications are enabled
     */
    public boolean isNotifyJoinEnabled() {
        return getBoolean(NOTIFY_JOIN, true);
    }
    
    /**
     * Check if player leave notifications are enabled
     */
    public boolean isNotifyLeaveEnabled() {
        return getBoolean(NOTIFY_LEAVE, true);
    }
    
    /**
     * Check if server start notifications are enabled
     */
    public boolean isNotifyServerStartEnabled() {
        return getBoolean(NOTIFY_SERVER_START, true);
    }
    
    /**
     * Check if server stop notifications are enabled
     */
    public boolean isNotifyServerStopEnabled() {
        return getBoolean(NOTIFY_SERVER_STOP, true);
    }
    
    /**
     * Reload configuration from file
     */
    public void reload() {
        properties.clear();
        loadConfig();
        LOGGER.info("Reloaded Discord configuration");
    }
}
