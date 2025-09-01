package com.zerog.neoessentials.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.nio.file.Files;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Handles loading and protection of customPlaceholders.json config file.
 * Ensures no overwrites and provides config data to PlaceholderManager.
 */
public class CustomPlaceholderConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPlaceholderConfig.class);
    private static final String CONFIG_FILE = "customPlaceholders.json";
    public static final String DEFAULT_TEMPLATE = "{\n  \"_comment\": \"NeoEssentials Custom Placeholders Configuration. Define your own placeholders for tablist, chat, GUIs, etc.\",\n  \"_documentation\": \"Supported types: static, conditional, animated. Use ${placeholder_name} in your configs. See examples below.\",\n  \"customPlaceholders\": {\n    \"afk_tag\": {\n      \"type\": \"conditional\",\n      \"condition\": \"${essentials_afk} == true\",\n      \"true\": \"&7[&cAFK&7]\",\n      \"false\": \"\"\n    },\n    \"player_health_bar\": {\n      \"type\": \"conditional\",\n      \"condition\": \"${player_health} >= 15\",\n      \"true\": \"&a❤❤❤❤❤\",\n      \"false\": \"&c❤❤&8❤❤❤\"\n    },\n    \"server_status_animation\": {\n      \"type\": \"animated\",\n      \"frames\": [\n        \"&a◉ &fOnline\",\n        \"&e◉ &fOnline\",\n        \"&6◉ &fOnline\",\n        \"&c◉ &fOnline\"\n      ],\n      \"interval\": 0.8\n    },\n    \"loading_indicator\": {\n      \"type\": \"animated\",\n      \"frames\": [\n        \"&7Loading.\",\n        \"&7Loading..\",\n        \"&7Loading...\",\n        \"&7Loading\"\n      ],\n      \"interval\": 0.4\n    },\n    \"welcome_message\": {\n      \"type\": \"static\",\n      \"value\": \"&6Welcome to &bNeoEssentials &6Server!\"\n    },\n    \"ping_colored\": {\n      \"type\": \"conditional\",\n      \"condition\": \"${player_ping} < 100\",\n      \"true\": \"&a${player_ping}ms\",\n      \"false\": \"&c${player_ping}ms\"\n    },\n    \"rank_prefix\": {\n      \"type\": \"static\",\n      \"value\": \"${prefix}\"\n    }\n  }\n}";

    private static CustomPlaceholderConfig instance;
    private JsonObject configData;
    private Path chosenPath;

    private CustomPlaceholderConfig() {
        loadConfig();
    }

    public static CustomPlaceholderConfig getInstance() {
        if (instance == null) instance = new CustomPlaceholderConfig();
        return instance;
    }

    private void loadConfig() {
        try {
            // Use ConfigurationUnifier to get proper config path
            Path configDir = com.zerog.neoessentials.config.ConfigurationUnifier.getInstance().getConfigPath().resolve("neoessentials");
            Path configFile = configDir.resolve(CONFIG_FILE);
            
            // Ensure config directory exists
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                LOGGER.info("Created NeoEssentials config directory: {}", configDir);
            }
            
            System.out.println("[NeoEssentials] CustomPlaceholderConfig loading:");
            System.out.println("  Config directory: " + configDir.toAbsolutePath());
            System.out.println("  Config file: " + configFile.toAbsolutePath());
            System.out.println("  File exists: " + Files.exists(configFile));
            
            if (Files.exists(configFile)) {
                // Check if file is empty
                if (Files.size(configFile) == 0) {
                    LOGGER.warn("customPlaceholders.json exists but is empty. Writing default template.");
                    Files.writeString(configFile, DEFAULT_TEMPLATE);
                }
                
                String json = Files.readString(configFile);
                configData = new Gson().fromJson(json, JsonObject.class);
                chosenPath = configFile;
                LOGGER.info("Loaded customPlaceholders.json from {}", configFile);
                System.out.println("  Successfully loaded config from: " + configFile.toAbsolutePath());
            } else {
                // Create default config file
                LOGGER.info("Creating default customPlaceholders.json at {}", configFile);
                Files.writeString(configFile, DEFAULT_TEMPLATE);
                configData = new Gson().fromJson(DEFAULT_TEMPLATE, JsonObject.class);
                chosenPath = configFile;
                System.out.println("  Created default config at: " + configFile.toAbsolutePath());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load customPlaceholders.json: {}", e.getMessage());
            System.out.println("[NeoEssentials] ERROR loading customPlaceholders.json: " + e.getMessage());
            e.printStackTrace();
            configData = new JsonObject();
            chosenPath = null;
        }
    }

    public JsonObject getConfigData() {
        return configData;
    }

    public Path getConfigPath() {
        return chosenPath;
    }

    /**
     * Prevent any write attempts to the config file.
     */
    public void saveConfig(JsonObject newData) {
        LOGGER.warn("Attempt to overwrite customPlaceholders.json blocked. No changes written.");
    }
    
    /**
     * Reload the configuration from file
     */
    public void reloadConfig() {
        LOGGER.info("Reloading customPlaceholders.json configuration...");
        loadConfig();
    }
    
    /**
     * Check if the config file exists and is valid
     */
    public boolean isConfigValid() {
        return chosenPath != null && Files.exists(chosenPath) && configData != null;
    }
    
    /**
     * Get all custom placeholder names
     */
    public java.util.Set<String> getCustomPlaceholderNames() {
        if (configData != null && configData.has("customPlaceholders")) {
            return configData.getAsJsonObject("customPlaceholders").keySet();
        }
        return java.util.Collections.emptySet();
    }
}
