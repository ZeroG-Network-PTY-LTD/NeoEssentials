package com.zerog.neoessentials.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration System Unifier for NeoEssentials
 * 
 * Simplified configuration access that delegates to ConfigManager to eliminate duplication.
 * This replaces the previous dual configuration system with a single unified approach.
 * 
 * @author ZeroG
 * @since 2.0.0 (Phase 5 - Configuration Fix)
 * @deprecated Use ConfigManager.getInstance() directly for better performance and less indirection
 */
@Deprecated
public class ConfigurationUnifier {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationUnifier.class);
    private static ConfigurationUnifier instance;
    
    private final ConfigManager configManager;
    private boolean isInitialized = false;
    
    private ConfigurationUnifier() {
        this.configManager = ConfigManager.getInstance();
        LOGGER.warn("ConfigurationUnifier is deprecated. Use ConfigManager.getInstance() directly.");
    }
    
    public static ConfigurationUnifier getInstance() {
        if (instance == null) {
            instance = new ConfigurationUnifier();
        }
        return instance;
    }
    
    /**
     * Initialize the unified configuration system
     * @deprecated Use ConfigManager.getInstance().initialize() directly
     */
    @Deprecated
    public void initialize() {
        if (isInitialized) {
            LOGGER.warn("Configuration system already initialized");
            return;
        }
        
        LOGGER.info("Initializing Configuration System via ConfigManager...");
        
        try {
            configManager.initialize();
            isInitialized = true;
            LOGGER.info("Configuration System initialized successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize configuration system", e);
            throw new RuntimeException("Configuration system initialization failed", e);
        }
    }
    
    /**
     * Get the configuration manager - delegates to ConfigManager
     * @deprecated Use ConfigManager.getInstance() directly
     */
    @Deprecated
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * @deprecated Use ConfigManager.getInstance().getConfigPath()
     */
    @Deprecated
    public Path getConfigPath() {
        return configManager.getConfigPath();
    }
    
    /**
     * @deprecated Use ConfigManager.getInstance().reloadAll()
     */
    @Deprecated
    public void reloadAll() {
        configManager.reloadAll();
    }
    
    /**
     * @deprecated Use ConfigManager.getInstance().saveAll()
     */
    @Deprecated
    public void saveAll() {
        configManager.saveAll();
    }
    
    /**
     * @deprecated Use ConfigManager.getInstance().getConfigStatus()
     */
    @Deprecated
    public ConfigStatus getConfigStatus() {
        return configManager.getConfigStatus();
    }
    
    /**
     * @deprecated Use ConfigManager.getInstance() methods directly
     */
    @Deprecated
    public void loadRuntimeConfigs() {
        // This functionality should be in ConfigManager, not here
        LOGGER.warn("loadRuntimeConfigs() is deprecated - move to ConfigManager");
    }
    
    /**
     * @deprecated Use ConfigManager.getInstance().isInitialized() if available
     */
    @Deprecated
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * @deprecated Use ConfigManager.getInstance().shutdown() if available
     */
    @Deprecated
    public void shutdown() {
        if (isInitialized && configManager != null) {
            LOGGER.info("Shutting down configuration system...");
            configManager.saveAll();
            isInitialized = false;
            LOGGER.info("Configuration system shutdown completed");
        }
    }
}
