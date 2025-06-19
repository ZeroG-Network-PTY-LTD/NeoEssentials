# Config Initialization Fix

## Issue
The mod was encountering a crash during startup with the following error:
```
java.lang.IllegalStateException: Cannot get config value before config is loaded.
```

This was occurring because the `CompatNeoEssentialsConfig` constructor was trying to access TOML config values before they were fully loaded.

## Root Cause
In the Forge/NeoForge config system, config values cannot be accessed during the initial mod constructor phase. The configs need to be registered first and then loaded by the game before their values can be accessed.

The issue was in the initialization sequence:
1. The NeoEssentials constructor creates the ModConfigManager
2. The ModConfigManager constructor creates a new CompatNeoEssentialsConfig
3. The CompatNeoEssentialsConfig constructor tries to access config values immediately (e.g., GeneralConfig.DEBUG_MODE.get())
4. This fails because the config isn't loaded yet

## Fix Applied
1. Modified `CompatNeoEssentialsConfig` to use lazy initialization:
   - Empty constructor that doesn't access config values
   - Added an `initialize()` method that loads values from configs when called

2. Updated `ModConfigManager` to support this pattern:
   - Created the compatibility config instance in the constructor but doesn't initialize it
   - Added an `initializeConfigs()` method to initialize configs when called

3. Enhanced `NeoEssentials` class:
   - Added config loading event handling
   - Added tracking to ensure configs are only initialized once
   - Triggers initialization after the general.toml config is loaded

## Technical Details
Config loading in NeoForge follows a specific sequence of events. By deferring the actual reading of config values until after we receive a config loading event, we ensure that the values are available and can be safely accessed.

This update follows the proper initialization sequence for NeoForge mods and prevents the "Cannot get config value before config is loaded" crash.
