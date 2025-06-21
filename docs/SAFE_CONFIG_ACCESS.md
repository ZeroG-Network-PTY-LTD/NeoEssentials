# Safe Config Access in NeoEssentials

## Overview

NeoEssentials uses NeoForge's config system to manage its configuration. This document explains how the mod safely accesses config values without encountering the "Cannot get config value before config is loaded" error.

## The Problem

In NeoForge's config system, config values cannot be accessed until the config files are fully loaded by the game. Attempting to access a config value before it's loaded will result in an `IllegalStateException` with the message "Cannot get config value before config is loaded."

This is particularly challenging in a mod like NeoEssentials which has many components that need access to config values at different stages of the mod loading process.

## Our Solution

NeoEssentials implements a multi-layered approach to safely access config values:

### 1. ConfigUtil Helper Class

We created a utility class (`ConfigUtil`) that provides safe methods to access config values:

```java
public static <T> boolean isConfigAvailable(ModConfigSpec.ConfigValue<T> configValue) {
    try {
        configValue.get();
        return true;
    } catch (IllegalStateException e) {
        return false;
    }
}

public static <T> T getConfigSafe(ModConfigSpec.ConfigValue<T> configValue, T defaultValue) {
    try {
        return configValue.get();
    } catch (IllegalStateException e) {
        return defaultValue;
    }
}
```

These methods allow us to check if a config value is available and to get a config value with a default fallback if it's not available.

### 2. Improved Config Loading Event Handling

The main `NeoEssentials` class monitors config loading events and only initializes configs when all expected config files are loaded:

```java
private void onConfigReady(final ModConfigEvent.Loading event) {
    String fileName = event.getConfig().getFileName();
    
    if (fileName.contains("neoessentials/")) {
        configsLoaded++;
    }
    
    if (!configsInitialized && configsLoaded >= EXPECTED_CONFIG_FILES) {
        configManager.initializeConfigs();
        configsInitialized = true;
    }
}
```

### 3. Lazy Loading in CompatNeoEssentialsConfig

The compatibility config layer (`CompatNeoEssentialsConfig`) uses lazy loading to access config values only when needed, with defaults provided for when configs aren't available:

```java
public boolean isEconomyEnabled() {
    return ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_ECONOMY, true);
}
```

### 4. Safe Initialization in ModConfigManager

The `ModConfigManager` checks if configs are available before trying to initialize them:

```java
public void initializeConfigs() {
    try {
        if (!ConfigUtil.isConfigAvailable(GeneralConfig.DEBUG_MODE)) {
            return; // Defer initialization
        }
        
        compatConfig.initialize();
    } catch (Exception e) {
        // Handle error
    }
}
```

## Best Practices for Config Access

When working with NeoEssentials configs:

1. **Always use ConfigUtil**: Access config values through `ConfigUtil.getConfigSafe()` to ensure safety.

2. **Provide sensible defaults**: Every config access should have a reasonable default value.

3. **Cache when appropriate**: Consider caching frequently accessed values after confirming configs are loaded.

4. **Handle initialization order**: Be aware of the mod loading sequence and defer config-dependent initialization as needed.

5. **Use lazy accessors**: Only access config values when they're actually needed, not during class initialization.

By following these practices, NeoEssentials maintains stability even when configs are not yet available.
