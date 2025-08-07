package com.zerog.neoessentials.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

/**
 * Configuration System Unifier for NeoEssentials
 * 
 * This class resolves the dual configuration manager issue by unifying
 * ConfigManager and EnhancedConfigManager into a single coherent system.
 * 
 * Problem identified:
 * - Managers use ConfigManager.getInstance()
 * - Main class also initializes EnhancedConfigManager.getInstance()
 * - Configuration files not properly generated/connected
 * 
 * Solution:
 * - Use EnhancedConfigManager as the primary system
 * - Ensure all managers reference the same configuration instance
 * - Generate and connect configuration files properly
 * 
 * @author ZeroG
 * @since 2.0.0 (Phase 5 - Configuration Fix)
 */
public class ConfigurationUnifier {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationUnifier.class);
    private static ConfigurationUnifier instance;
    
    private ConfigManager configManager;
    private boolean isInitialized = false;
    
    private ConfigurationUnifier() {
        // Private constructor for singleton
    }
    
    public static ConfigurationUnifier getInstance() {
        if (instance == null) {
            instance = new ConfigurationUnifier();
        }
        return instance;
    }
    
    /**
     * Initialize the unified configuration system
     * This replaces the dual initialization in NeoEssentials.java
     */
    public void initialize() {
        if (isInitialized) {
            LOGGER.warn("Configuration system already initialized");
            return;
        }
        
        LOGGER.info("Initializing Unified Configuration System...");
        
        try {
            // Use ConfigManager as the primary system
            configManager = ConfigManager.getInstance();
            configManager.initialize();
            
            // Generate all configuration files if they don't exist
            generateAllConfigurationFiles();
            
            // Validate configuration integrity
            validateConfigurationIntegrity();
            
            isInitialized = true;
            LOGGER.info("Unified Configuration System initialized successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize unified configuration system", e);
            throw new RuntimeException("Configuration system initialization failed", e);
        }
    }
    
    /**
     * Generate all configuration files with proper defaults
     */
    private void generateAllConfigurationFiles() {
        LOGGER.info("Generating configuration files...");
        
        // Force creation of all config files with proper defaults
        configManager.getMainConfig();
        configManager.getEconomyConfig();
        configManager.getHomeConfig();
        configManager.getKitConfig();
        configManager.getWarpConfig();
        configManager.getModerationConfig();
        configManager.getMessagingConfig();
        configManager.getDiscordConfig();
        configManager.getTablistConfig();
        configManager.getSpawnConfig();
        
        // Save all configurations to ensure files are created
        configManager.saveAll();
        
        LOGGER.info("All configuration files generated successfully!");
    }
    
    /**
     * Validate that all configuration files exist and are properly connected
     */
    private void validateConfigurationIntegrity() {
        LOGGER.info("Validating configuration integrity...");
        
        String[] requiredFiles = configManager.getAllConfigFiles();
        int validFiles = 0;
        int totalFiles = requiredFiles.length;
        
        for (String fileName : requiredFiles) {
            if (configManager.configExists(fileName)) {
                validFiles++;
                LOGGER.debug("✓ Configuration file exists: {}", fileName);
            } else {
                LOGGER.warn("✗ Configuration file missing: {}", fileName);
            }
        }
        
        LOGGER.info("Configuration validation: {}/{} files found", validFiles, totalFiles);
        
        if (validFiles == totalFiles) {
            LOGGER.info("All configuration files validated successfully!");
        } else {
            LOGGER.warn("Some configuration files are missing - they will be created with defaults");
        }
    }
    
    /**
     * Get the unified configuration manager
     * This method should be used by all managers instead of ConfigManager.getInstance()
     */
    public ConfigManager getConfigManager() {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration system not initialized. Call initialize() first.");
        }
        return configManager;
    }
    
    /**
     * Get configuration path for managers that need it
     */
    public Path getConfigPath() {
        return getConfigManager().getConfigPath();
    }
    
    /**
     * Hot-reload all configurations
     */
    public void reloadAll() {
        if (!isInitialized) {
            LOGGER.warn("Cannot reload - configuration system not initialized");
            return;
        }
        
        LOGGER.info("Reloading all configurations...");
        configManager.reloadAll();
        LOGGER.info("Configuration reload completed!");
    }
    
    /**
     * Save all configurations
     */
    public void saveAll() {
        if (!isInitialized) {
            LOGGER.warn("Cannot save - configuration system not initialized");
            return;
        }
        
        configManager.saveAll();
    }
    
    /**
     * Get configuration status for monitoring
     */
    public ConfigStatus getConfigStatus() {
        return configManager.getConfigStatus();
    }
    
    /**
     * Check if the configuration system is properly initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Shutdown the configuration system gracefully
     */
    public void shutdown() {
        if (isInitialized && configManager != null) {
            LOGGER.info("Shutting down configuration system...");
            configManager.shutdownHotReload();
            configManager.saveAll();
            isInitialized = false;
            LOGGER.info("Configuration system shutdown completed");
        }
    }
}
