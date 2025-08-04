package com.zerog.neoessentials.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced configuration validation system
 * Provides detailed validation for configuration files
 */
public class ConfigValidator {
    
    /**
     * Validation result containing errors and warnings
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final String configName;
        
        public ValidationResult(String configName) {
            this.configName = configName;
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }
        
        public String getConfigName() {
            return configName;
        }
        
        public boolean isValid() {
            return !hasErrors();
        }
        
        public String getSummary() {
            if (!hasErrors() && !hasWarnings()) {
                return configName + ": Valid ✅";
            }
            
            StringBuilder summary = new StringBuilder(configName + ": ");
            if (hasErrors()) {
                summary.append(errors.size()).append(" error(s) ❌");
            }
            if (hasWarnings()) {
                if (hasErrors()) summary.append(", ");
                summary.append(warnings.size()).append(" warning(s) ⚠️");
            }
            return summary.toString();
        }
    }
    
    /**
     * Validate a configuration file
     */
    public static ValidationResult validateConfigFile(File configFile, String expectedConfigType) {
        String configName = configFile.getName().replace(".json", "");
        ValidationResult result = new ValidationResult(configName);
        
        // Check if file exists
        if (!configFile.exists()) {
            result.addError("Configuration file does not exist: " + configFile.getPath());
            return result;
        }
        
        // Check if file is readable
        if (!configFile.canRead()) {
            result.addError("Configuration file is not readable: " + configFile.getPath());
            return result;
        }
        
        // Check file size (warn if empty or too large)
        long fileSize = configFile.length();
        if (fileSize == 0) {
            result.addError("Configuration file is empty");
            return result;
        }
        
        if (fileSize > 1024 * 1024) { // 1MB
            result.addWarning("Configuration file is unusually large (" + fileSize + " bytes)");
        }
        
        // Parse and validate JSON structure
        try (FileReader reader = new FileReader(configFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            
            if (!element.isJsonObject()) {
                result.addError("Configuration must be a JSON object");
                return result;
            }
            
            JsonObject jsonObject = element.getAsJsonObject();
            
            // Validate specific configuration types
            switch (expectedConfigType.toLowerCase()) {
                case "main" -> validateMainConfig(jsonObject, result);
                case "economy" -> validateEconomyConfig(jsonObject, result);
                case "homes" -> validateHomesConfig(jsonObject, result);
                case "kits" -> validateKitsConfig(jsonObject, result);
                case "warps" -> validateWarpsConfig(jsonObject, result);
                case "moderation" -> validateModerationConfig(jsonObject, result);
                case "messaging" -> validateMessagingConfig(jsonObject, result);
                case "discord" -> validateDiscordConfig(jsonObject, result);
                case "tablist" -> validateTablistConfig(jsonObject, result);
                case "spawn" -> validateSpawnConfig(jsonObject, result);
                default -> result.addWarning("Unknown configuration type: " + expectedConfigType);
            }
            
        } catch (IOException e) {
            result.addError("Failed to read configuration file: " + e.getMessage());
        } catch (Exception e) {
            result.addError("Invalid JSON format: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate main configuration structure
     */
    private static void validateMainConfig(JsonObject config, ValidationResult result) {
        // Check required fields
        checkRequiredField(config, "serverName", result);
        checkRequiredField(config, "defaultLanguage", result);
        checkRequiredField(config, "debugMode", result);
        
        // Validate specific values
        if (config.has("debugMode") && !config.get("debugMode").isJsonPrimitive()) {
            result.addError("debugMode must be a boolean value");
        }
    }
    
    /**
     * Validate economy configuration structure
     */
    private static void validateEconomyConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        checkRequiredField(config, "startingBalance", result);
        checkRequiredField(config, "currencySymbol", result);
        
        // Validate numeric fields
        if (config.has("startingBalance")) {
            JsonElement startingBalance = config.get("startingBalance");
            if (!startingBalance.isJsonPrimitive() || !startingBalance.getAsJsonPrimitive().isNumber()) {
                result.addError("startingBalance must be a number");
            } else if (startingBalance.getAsDouble() < 0) {
                result.addWarning("startingBalance is negative");
            }
        }
    }
    
    /**
     * Validate homes configuration structure
     */
    private static void validateHomesConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        checkRequiredField(config, "maxHomes", result);
        checkRequiredField(config, "cooldown", result);
        
        if (config.has("maxHomes")) {
            JsonElement maxHomes = config.get("maxHomes");
            if (!maxHomes.isJsonPrimitive() || !maxHomes.getAsJsonPrimitive().isNumber()) {
                result.addError("maxHomes must be a number");
            } else if (maxHomes.getAsInt() < 1) {
                result.addWarning("maxHomes should be at least 1");
            }
        }
    }
    
    /**
     * Add more specific validation methods for other config types
     */
    private static void validateKitsConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        // Add kit-specific validation
    }
    
    private static void validateWarpsConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        // Add warp-specific validation
    }
    
    private static void validateModerationConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        // Add moderation-specific validation
    }
    
    private static void validateMessagingConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        // Add messaging-specific validation
    }
    
    private static void validateDiscordConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        if (config.has("enabled") && config.get("enabled").getAsBoolean()) {
            checkRequiredField(config, "webhookUrl", result);
        }
    }
    
    private static void validateTablistConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        // Add tablist-specific validation
    }
    
    private static void validateSpawnConfig(JsonObject config, ValidationResult result) {
        checkRequiredField(config, "enabled", result);
        // Add spawn-specific validation
    }
    
    /**
     * Check if a required field exists in the configuration
     */
    private static void checkRequiredField(JsonObject config, String fieldName, ValidationResult result) {
        if (!config.has(fieldName)) {
            result.addError("Missing required field: " + fieldName);
        }
    }
}
