package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks configuration status and validation states
 */
public class ConfigStatus {
    private final Map<String, Boolean> loadedConfigs = new HashMap<>();
    private final Map<String, Boolean> validConfigs = new HashMap<>();
    private final Map<String, Long> lastModified = new HashMap<>();
    private final Map<String, String> errors = new HashMap<>();
    
    /**
     * Mark a configuration as loaded
     */
    public void markLoaded(String configName, boolean success) {
        loadedConfigs.put(configName, success);
        if (!success) {
            validConfigs.put(configName, false);
        }
    }
    
    /**
     * Mark a configuration as valid/invalid
     */
    public void markValid(String configName, boolean valid, String error) {
        validConfigs.put(configName, valid);
        if (!valid && error != null) {
            errors.put(configName, error);
        } else {
            errors.remove(configName);
        }
    }
    
    /**
     * Update last modified time
     */
    public void updateModified(String configName, long timestamp) {
        lastModified.put(configName, timestamp);
    }
    
    /**
     * Check if configuration is loaded
     */
    public boolean isLoaded(String configName) {
        return loadedConfigs.getOrDefault(configName, false);
    }
    
    /**
     * Check if configuration is valid
     */
    public boolean isValid(String configName) {
        return validConfigs.getOrDefault(configName, false);
    }
    
    /**
     * Get error message for configuration
     */
    public String getError(String configName) {
        return errors.get(configName);
    }
    
    /**
     * Get last modified time
     */
    public long getLastModified(String configName) {
        return lastModified.getOrDefault(configName, 0L);
    }
    
    /**
     * Get all loaded configurations
     */
    public Map<String, Boolean> getAllLoaded() {
        return new HashMap<>(loadedConfigs);
    }
    
    /**
     * Get all validation states
     */
    public Map<String, Boolean> getAllValid() {
        return new HashMap<>(validConfigs);
    }
    
    /**
     * Get all errors
     */
    public Map<String, String> getAllErrors() {
        return new HashMap<>(errors);
    }
    
    /**
     * Clear all status information
     */
    public void clear() {
        loadedConfigs.clear();
        validConfigs.clear();
        lastModified.clear();
        errors.clear();
    }
    
    /**
     * Get summary of configuration system health
     */
    public String getHealthSummary() {
        int total = loadedConfigs.size();
        long loaded = loadedConfigs.values().stream().mapToLong(b -> b ? 1 : 0).sum();
        long valid = validConfigs.values().stream().mapToLong(b -> b ? 1 : 0).sum();
        int errors = this.errors.size();
        
        return String.format("Config Health: %d/%d loaded, %d/%d valid, %d errors", 
                             loaded, total, valid, total, errors);
    }
}
