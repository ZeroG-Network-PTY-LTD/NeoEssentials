# Config Initialization Fix

## Issue
The mod was encountering a crash during startup with the following error:
```
java.lang.IllegalStateException: Cannot get config value before config is loaded.
        at com.google.common.base.Preconditions.checkState(Preconditions.java:512)
        at net.neoforged.neoforge.common.ModConfigSpec$ConfigValue.getRaw(ModConfigSpec.java:1235)
        at net.neoforged.neoforge.common.ModConfigSpec$ConfigValue.get(ModConfigSpec.java:1222)
        at com.zerog.neoessentials.config.CompatNeoEssentialsConfig.initialize(CompatNeoEssentialsConfig.java:59)
```

This was occurring because the code was trying to access TOML config values before they were fully loaded by NeoForge's config system.

## Root Cause
In the NeoForge config system, config values cannot be accessed during the initial mod constructor phase or too early in the config loading process. The configs need to be registered first and then fully loaded by the game before their values can be accessed.

The issue was in the initialization sequence:
1. The NeoEssentials constructor creates the ModConfigManager
2. The ModConfigManager constructor creates a new CompatNeoEssentialsConfig
3. When ModConfigEvent.Loading is triggered, the code attempts to initialize configs
4. However, this is attempted too early in some cases when the values aren't fully available yet
5. This causes the IllegalStateException when trying to get config values

## Fix Applied
1. Enhanced `CompatNeoEssentialsConfig` with more robust error handling:
   - Modified the `initialize()` method to safely handle unavailable configs
   - Added individual try-catch blocks around each config value access
   - Strengthened the lazy loading pattern throughout the class

2. Updated `ModConfigManager` with better initialization checks:
   - Added safety checks before attempting to access config values
   - Improved error handling to prevent crashes if configs aren't ready
   - Added more logging to better diagnose any issues

3. Enhanced `NeoEssentials` config loading event handler:
   - Added tracking of all config files to ensure complete loading
   - Only initializes configs when all expected config files are loaded
   - Added a small delay to ensure configs are fully processed
   - Improved logging for better diagnostics

## Technical Details
Config loading in NeoForge follows a sequence of events that can be tricky to get right. The key insights from this fix:

1. **Wait for all configs**: Instead of using a single config file as a trigger, we now count all loaded config files.
2. **Handle unavailable configs gracefully**: Each access to config values is wrapped in try-catch blocks.
3. **Lazy loading throughout**: Default values are used when configs aren't available yet.
4. **Multiple defense layers**: Each component (NeoEssentials, ModConfigManager, CompatConfig) has its own safety checks.

This comprehensive approach ensures the mod doesn't crash due to premature config access and makes the system more robust overall.
