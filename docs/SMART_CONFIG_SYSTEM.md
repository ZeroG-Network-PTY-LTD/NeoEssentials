# Smart Config Update System

## Overview

NeoEssentials now features an intelligent configuration update system that automatically handles config file version management while preserving user customizations.

## How It Works

### 1. Version Tracking
- Each config file now includes a `_configVersion` field with a warning comment
- The system compares file versions against templates in the mod JAR
- When structure changes are detected, automatic updates are triggered
- **⚠️ Users should never modify the `_configVersion` field manually**

### 2. Smart Detection
The system checks for updates by:
- **Version comparison**: Old configs without `_configVersion` are treated as version 0
- **Structure analysis**: Compares top-level keys and nested object structures
- **Missing sections**: Detects when new config sections are added to templates

### 3. Backup System
When updates are needed:
- Creates incremental backups: `config.json.bak1`, `config.json.bak2`, etc.
- Never overwrites existing backups
- Supports up to 999 backup files per config

### 4. Value Preservation
During updates, the system:
- **Preserves user settings**: Custom values are maintained
- **Adds new sections**: Missing keys from templates are added with defaults
- **Removes obsolete keys**: Old sections not in new templates are excluded
- **Merges recursively**: Nested objects are merged intelligently

## Example Scenario

### Old Config (Version 0)
```json
{
  "modules": {
    "economyEnabled": true,
    "commandsEnabled": false
  },
  "economySettings": {
    "startingBalance": 500.0,
    "currencySymbol": "€"
  }
}
```

### New Template (Version 1)
```json
{
  "_configVersion": 1,
  "_configVersion_comment": "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates. Changing this may cause config corruption.",
  "modules": {
    "economyEnabled": true,
    "commandsEnabled": true,
    "permissionsEnabled": true,
    "debugMode": false
  },
  "economySettings": {
    "startingBalance": 100.0,
    "currencySymbol": "$",
    "maxBalance": 999999999.99,
    "taxPercentage": 0.0
  },
  "chat": {
    "enable-chat-formatting": true,
    "chat-format": "<{PREFIX} {USERNAME} {SUFFIX}> {MESSAGE}"
  }
}
```

### Result After Update
```json
{
  "_configVersion": 1,
  "_configVersion_comment": "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates. Changing this may cause config corruption.",
  "modules": {
    "economyEnabled": true,
    "commandsEnabled": false,
    "permissionsEnabled": true,
    "debugMode": false
  },
  "economySettings": {
    "startingBalance": 500.0,
    "currencySymbol": "€",
    "maxBalance": 999999999.99,
    "taxPercentage": 0.0
  },
  "chat": {
    "enable-chat-formatting": true,
    "chat-format": "<{PREFIX} {USERNAME} {SUFFIX}> {MESSAGE}"
  }
}
```

**Notice:**
- ✅ User's `startingBalance: 500.0` preserved
- ✅ User's `currencySymbol: "€"` preserved  
- ✅ User's `commandsEnabled: false` preserved
- ✅ New sections (`chat`) added with defaults
- ✅ New keys in existing sections added with defaults
- ✅ Version marker added: `_configVersion: 1`
- ✅ Original backed up as `config.json.bak1`

## Benefits

1. **No More Manual Config Migration**: Users don't need to manually update configs when mod updates
2. **Preserves Customizations**: User settings are never lost during updates
3. **Safe Fallback**: Original configs are always backed up before changes
4. **Automatic Detection**: Works seamlessly in the background
5. **Incremental Backups**: Multiple update cycles create numbered backups

## Configuration Files Supported

- `config.json` - Main mod configuration
- `permissions.json` - Permission system settings  
- `economy.json` - Economy system settings
- `kits.json` - Kit system configuration

## Implementation Notes

### For Developers
When making config structure changes:
1. Increment `CURRENT_CONFIG_VERSION` in `ConfigManager.java`
2. Update the template files in `src/main/resources/data/config/neoessentials/`
3. Add `_configVersion` field to new templates
4. The system will automatically handle user config updates

### For Server Admins
- Backup files are created automatically - no action needed
- Check `.bak1`, `.bak2` files if you need to revert changes
- The system only updates when structure changes are detected
- User values are always preserved where possible

## Logging

The system provides detailed logging:
```
[INFO] Config version outdated: existing=0, template=1
[INFO] Config structure outdated for config.json, updating...
[INFO] Created backup: config.json.bak1
[INFO] Updated configuration: config.json (backup saved as config.json.bak1)
```

## Safety Features

- **Atomic Operations**: Config updates are atomic (temp file → rename)
- **Error Recovery**: Falls back to minimal config if update fails
- **Backup Validation**: Ensures backups are created before making changes
- **Structure Validation**: Validates JSON structure before applying updates
- **Rollback Capability**: Admins can manually restore from backups if needed
- **Version Protection**: Warning comments prevent accidental modification of version fields

## ⚠️ Important User Warning

**DO NOT manually edit the `_configVersion` field in any config file!**

All config files now include this warning:
```json
{
  "_configVersion": 1,
  "_configVersion_comment": "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates. Changing this may cause config corruption."
}
```

Modifying the version field can cause:
- Config corruption during updates
- Loss of user settings
- Failure to apply important mod updates
- Unexpected behavior

If you accidentally modify it, restore from a `.bak` file or delete the config to regenerate it.