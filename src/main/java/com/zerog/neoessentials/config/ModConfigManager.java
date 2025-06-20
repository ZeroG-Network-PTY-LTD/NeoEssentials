package com.zerog.neoessentials.config;

import com.zerog.neoessentials.NeoEssentials;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Manages all configuration for the NeoEssentials mod.
 * Handles loading, registration, and providing access to all config classes.
 */
public class ModConfigManager {
    // Reference to the main mod instance
    private final NeoEssentials mod;
    private TablistConfig tablistConfig; // Legacy JSON-based config, to be migrated later
    
<<<<<<< HEAD
<<<<<<< HEAD
    // Compatibility config instance (legacy format)
    private CompatNeoEssentialsConfig compatConfig;
      /**
=======
=======
    // Compatibility config instance (legacy format)
    private CompatNeoEssentialsConfig compatConfig;
<<<<<<< HEAD
    
>>>>>>> fb0eb45 (feat: Update compatibility layer for legacy config structure and enhance TablistManager with new config handling)
    /**
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
=======
      /**
>>>>>>> 6528176 (feat: Enhance scheduler handling and improve configuration management in NeoEssentials)
     * Creates a new config manager
     * 
     * @param mod The mod instance
     * @param container The mod container
     */
    public ModConfigManager(NeoEssentials mod, ModContainer container) {
        this.mod = mod;
        
        // Load legacy tablist config
        tablistConfig = new TablistConfig();
        tablistConfig.load();
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 6528176 (feat: Enhance scheduler handling and improve configuration management in NeoEssentials)
        
        // Create compatibility config
        compatConfig = new CompatNeoEssentialsConfig();
        
        // Register all configuration files
<<<<<<< HEAD
=======
          // Register all configuration files
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
=======
>>>>>>> 6528176 (feat: Enhance scheduler handling and improve configuration management in NeoEssentials)
        container.registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC, "neoessentials/general.toml");
        container.registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC, "neoessentials/economy.toml");
        container.registerConfig(ModConfig.Type.COMMON, HomeConfig.SPEC, "neoessentials/homes.toml");
        container.registerConfig(ModConfig.Type.COMMON, WarpConfig.SPEC, "neoessentials/warps.toml");
        container.registerConfig(ModConfig.Type.COMMON, KitConfig.SPEC, "neoessentials/kits.toml");
        container.registerConfig(ModConfig.Type.COMMON, TablistTomlConfig.SPEC, "neoessentials/tablist.toml");
        container.registerConfig(ModConfig.Type.COMMON, DatabaseTomlConfig.SPEC, "neoessentials/database.toml");
        
        NeoEssentials.LOGGER.info("Registered all NeoEssentials config files");
    }
    
    /**
     * Gets the tablist config
     * @return The tablist config
     */
    public TablistConfig getTablistConfig() {
        return tablistConfig;
    }
    
    /**
     * Helper method to check if a feature is enabled in the general config
     * 
     * @param configValue The config value to check
     * @return True if the feature is enabled, false otherwise
     */
    public boolean isFeatureEnabled(ModConfigSpec.BooleanValue configValue) {
        return configValue.get();
    }
    
    /**
     * Check if the economy system is enabled
     * @return True if enabled
     */
    public boolean isEconomyEnabled() {
        return GeneralConfig.ENABLE_ECONOMY.get();
    }
    
    /**
     * Check if the home system is enabled
     * @return True if enabled
     */
    public boolean isHomesEnabled() {
        return GeneralConfig.ENABLE_HOMES.get();
    }
    
    /**
     * Check if the warp system is enabled
     * @return True if enabled
     */
    public boolean isWarpsEnabled() {
        return GeneralConfig.ENABLE_WARPS.get();
    }
    
    /**
     * Check if the kit system is enabled
     * @return True if enabled
     */
    public boolean isKitsEnabled() {
        return GeneralConfig.ENABLE_KITS.get();
    }
    
    /**
     * Check if the tablist system is enabled
     * @return True if enabled
     */
    public boolean isTablistEnabled() {
        return GeneralConfig.ENABLE_TABLIST.get();
    }
    
    /**
     * Check if teleportation is enabled
     * @return True if enabled
     */
    public boolean isTeleportationEnabled() {
        return GeneralConfig.ENABLE_TELEPORTATION.get();
    }
    
    /**
     * Get the database config spec
     * This is used for backward compatibility with the storage manager
     * @return The database config spec
     */
    public ModConfigSpec getDatabaseConfig() {
        return DatabaseTomlConfig.SPEC;
    }
    
    /**
     * Get the database config spec (alias for backward compatibility)
     * @return The database config spec
     */
    public ModConfigSpec getSpec() {
        return DatabaseTomlConfig.SPEC;
<<<<<<< HEAD
<<<<<<< HEAD
    }    /**
     * Gets the compatibility config that adapts TOML values to the old config structure
     * @return The compatibility config
     */
    public CompatNeoEssentialsConfig getConfig() {
        if (compatConfig == null) {
            compatConfig = new CompatNeoEssentialsConfig();
            // Note: config values will not be initialized here - that happens in initializeConfigs()
            NeoEssentials.LOGGER.info("Created new compatibility config instance");
        }
        return compatConfig;
    }
      /**
     * Initialize config values after all configs are loaded
     * This should be called after the mod loading phase when config values are available
     */
    public void initializeConfigs() {
        NeoEssentials.LOGGER.info("Initializing config values");
        try {
            // Check if configs are available before initializing
            if (!ConfigUtil.isConfigAvailable(GeneralConfig.DEBUG_MODE)) {
                NeoEssentials.LOGGER.warn("Config values are not yet available. Deferring initialization.");
                return;
            }
            
            if (compatConfig != null) {
                compatConfig.initialize();
                NeoEssentials.LOGGER.info("Compatibility config layer initialized successfully");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize configs", e);
        }
=======
>>>>>>> 7409b6f (feat: Add comprehensive configuration management for NeoEssentials, including database, economy, home, kit, warp, and tablist settings)
    }
      /**
=======
    }    /**
>>>>>>> 7ffa71d (feat: Enhance config management with robust error handling and lazy loading)
     * Gets the compatibility config that adapts TOML values to the old config structure
     * @return The compatibility config
     */
    public CompatNeoEssentialsConfig getConfig() {
        if (compatConfig == null) {
            compatConfig = new CompatNeoEssentialsConfig();
            // Note: config values will not be initialized here - that happens in initializeConfigs()
            NeoEssentials.LOGGER.info("Created new compatibility config instance");
        }
        return compatConfig;
    }
      /**
     * Initialize config values after all configs are loaded
     * This should be called after the mod loading phase when config values are available
     */
    public void initializeConfigs() {
        NeoEssentials.LOGGER.info("Initializing config values");
        try {
            // Check if configs are available before initializing
            if (!ConfigUtil.isConfigAvailable(GeneralConfig.DEBUG_MODE)) {
                NeoEssentials.LOGGER.warn("Config values are not yet available. Deferring initialization.");
                return;
            }
            
            if (compatConfig != null) {
                compatConfig.initialize();
                NeoEssentials.LOGGER.info("Compatibility config layer initialized successfully");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize configs", e);
        }
    }
}
