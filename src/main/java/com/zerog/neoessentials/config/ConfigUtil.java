package com.zerog.neoessentials.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;

/**
 * Simple unified config access for command enabling/disabling.
 * Uses the same config file and pattern as other parts of the mod.
 */
public class ConfigUtil {
    private static JsonObject commandsConfig = null;
    private static boolean loaded = false;

    /**
     * Load the commands config section from the main config file.
     * This uses the same config.json file that the rest of the mod uses.
     */
    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        
        try {
            File configFile = new File("config/neoessentials/config.json");
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("commands")) {
                        commandsConfig = root.getAsJsonObject("commands");
                    }
                }
            }
        } catch (Exception e) {
            // If loading fails, commands will default to enabled
            commandsConfig = null;
        }
    }

    /**
     * Check if a command is enabled in the config.
     * Defaults to true if config is missing or command not specified.
     */
    public static boolean isCommandEnabled(String command) {
        ensureLoaded();
        return commandsConfig == null || !commandsConfig.has(command) || commandsConfig.get(command).getAsBoolean();
    }
}