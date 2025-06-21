# Configuration System Update

## Overview
The configuration system for NeoEssentials has been refactored to use TOML files for better organization and usability. This document explains how the new configuration system works and how it maintains compatibility with existing code.

## Configuration Files
The mod now uses separate TOML configuration files for different features:

- `neoessentials/general.toml` - General mod settings and feature toggles
- `neoessentials/economy.toml` - Economy-related settings
- `neoessentials/homes.toml` - Home system settings
- `neoessentials/warps.toml` - Warp system settings
- `neoessentials/kits.toml` - Kit system settings
- `neoessentials/tablist.toml` - Tablist customization settings
- `neoessentials/database.toml` - Database connection settings

Each file contains settings specific to that feature, making it easier for server administrators to find and modify relevant settings.

## Configuration Classes
Each TOML file has a corresponding configuration class that defines the available settings. These classes use NeoForge's ModConfigSpec system:

- `GeneralConfig` - General settings and feature toggles
- `EconomyConfig` - Economy settings
- `HomeConfig` - Home system settings
- `WarpConfig` - Warp system settings
- `KitConfig` - Kit system settings
- `TablistTomlConfig` - Tablist settings (TOML version)
- `DatabaseTomlConfig` - Database settings (TOML version)

## Compatibility Layer
To maintain compatibility with existing code that uses the old configuration system, a compatibility layer has been implemented:

1. `CompatNeoEssentialsConfig` - This class provides the same interface as the old `NeoEssentialsConfig` class but pulls values from the new TOML files.

2. `ModConfigManager.getConfig()` - This method returns an instance of `CompatNeoEssentialsConfig` that can be used by code expecting the old config structure.

## Usage
### For New Code
New code should access configuration values directly from the specific config classes using their static fields:

```java
// Check if a feature is enabled
if (GeneralConfig.ENABLE_ECONOMY.get()) {
    // Economy feature is enabled
}

// Get a specific setting value
String currencySymbol = EconomyConfig.CURRENCY_SYMBOL.get();
```

### For Existing Code
Existing code can continue to use the compatibility layer:

```java
// Get the config through the mod instance
CompatNeoEssentialsConfig config = NeoEssentials.getInstance().getConfigManager().getConfig();

// Use it like the old config
if (config.isEconomyEnabled()) {
    // Economy feature is enabled
}

String currencySymbol = config.getCurrencySymbol();
```

## Feature Toggles
The general config file includes toggles for enabling or disabling specific features:

- `ENABLE_ECONOMY` - Enable/disable the economy system
- `ENABLE_HOMES` - Enable/disable the home system
- `ENABLE_WARPS` - Enable/disable the warp system
- `ENABLE_KITS` - Enable/disable the kit system
- `ENABLE_TELEPORTATION` - Enable/disable teleportation commands
- `ENABLE_TABLIST` - Enable/disable tablist customization

## Future Improvements
In the future, we plan to:

1. Add in-game commands for modifying configuration settings
2. Add a web interface for configuration management
3. Implement live reloading of configuration files
4. Add more documentation for each configuration option
