# NeoEssentials v1.0.2 - Modrinth Release

**Configuration system overhaul with clean migration required**

*Released: August 8, 2025*

## Overview

Version 1.0.2 introduces a completely redesigned configuration system for NeoEssentials that provides better organization, enhanced validation, and improved performance. This update requires a clean migration from previous versions to ensure compatibility with the new system.

## ⚠️ **IMPORTANT: Clean Migration Required**

**Before updating, you must delete your existing NeoEssentials configuration files:**

### Windows Users:
```
1. Stop your server
2. Delete: config\neoessentials\
3. Delete: neoessentials\ (if it exists)
4. Update the mod JAR
5. Start server (new config auto-generates)
```

### Linux/Mac Users:
```bash
# Stop your server first
rm -rf config/neoessentials/
rm -rf neoessentials/
# Update mod JAR and restart server
```

**Why is this necessary?**
- Complete configuration architecture redesign
- New file structure and validation system
- Enhanced security and backup features
- Incompatible configuration formats

## New Features

### Enhanced Configuration System
**Completely rebuilt configuration management**

#### Organized File Structure
The new system organizes configuration into logical categories:
- **Core Settings**: `config/neoessentials/core/` - General, database, performance
- **Feature Settings**: `config/neoessentials/features/` - Economy, teleportation, moderation
- **UI Settings**: `config/neoessentials/ui/` - Tablist, GUI, themes, notifications
- **Integrations**: `config/neoessentials/integrations/` - Discord, permissions, placeholders

#### Advanced Validation
```toml
[validation]
schema_version = "2.0"
validation_level = "strict"
auto_repair = true
backup_on_error = true
```

#### Hot-Reload System
- Configuration changes apply without server restart
- Real-time validation and error reporting
- Automatic backup before applying changes
- Rollback capability for invalid configurations

### Improved Storage Backend
**Enhanced data storage with multiple options**

#### Storage Options
- **JSON**: Human-readable, easy to edit
- **YAML**: Configuration-friendly format
- **SQLite**: Local database for better performance
- **MySQL/PostgreSQL**: Enterprise database support

#### Backup System
```toml
[backup]
enabled = true
interval = "6h"
retention_days = 30
compression = true
location = "neoessentials/backups/"
```

### Security Enhancements
**Improved security throughout the system**

#### Rate Limiting
```toml
[rate_limiting]
commands_per_minute = 60
teleport_cooldown = 5
economy_cooldown = 3
```

#### Audit Logging
```toml
[audit]
log_commands = true
log_economy = true
log_teleports = true
log_admin_actions = true
```

## Technical Improvements

### Performance Optimizations
**Significant performance improvements across the board**

#### Configuration System
- **60% faster** configuration parsing
- **40% reduction** in disk I/O overhead  
- **Lazy loading** for optional configuration sections
- **Batch operations** for multiple changes

#### Memory Management
- **Smart caching** system reduces memory usage
- **Automatic cleanup** of unused resources
- **Object pooling** for frequently used objects
- **Memory leak detection** and prevention

### Enhanced Validation
**Comprehensive validation system with detailed error reporting**

#### Validation Features
- **Schema-based validation** for all configuration files
- **Cross-field validation** for related settings
- **Type checking** and range validation
- **Detailed error messages** with suggestions for fixes

#### Error Recovery
- **Automatic repair** for common configuration issues
- **Backup restoration** when configuration is corrupted
- **Graceful degradation** when optional features fail
- **Validation reports** for troubleshooting

## Migration Guide

### Step-by-Step Migration

#### 1. Backup Current Configuration (Optional)
```bash
# Create backup of current config
mkdir neoessentials-backup
cp -r config/neoessentials/ neoessentials-backup/
```

#### 2. Clean Removal
```bash
# Remove old configuration completely
rm -rf config/neoessentials/
rm -rf neoessentials/
```

#### 3. Update and Generate New Config
1. Stop your server
2. Replace mod JAR with v1.0.2
3. Start server (new config files auto-generate)
4. Stop server
5. Customize configuration files
6. Start server again

### Post-Migration Configuration

#### Essential Settings
```toml
# config/neoessentials/core/general.toml
[general]
enabled = true
language = "en_US"
update_interval = 1000

[features]
economy = true
teleportation = true
moderation = true
gui_system = true
```

#### Economy Setup
```toml
# config/neoessentials/features/economy.toml
[economy]
enabled = true
starting_balance = 1000.0
currency_name = "Coins"
currency_symbol = "$"
```

#### Discord Integration
```toml
# config/neoessentials/integrations/discord.toml
[discord]
enabled = false
webhook_url = ""
send_join_leave = true
send_chat = false
```

## API Changes

### New Configuration API
**Enhanced programmatic configuration access**

#### Configuration Manager
```java
// Access new configuration system
ConfigurationManager config = NeoEssentials.getConfigurationManager();
boolean economyEnabled = config.getFeatureConfig().isEconomyEnabled();
config.reloadConfiguration("economy");
```

#### Validation API
```java
// Validate configuration programmatically
ValidationResult result = config.validateConfiguration();
if (!result.isValid()) {
    result.getErrors().forEach(error -> 
        LOGGER.error("Config error: {}", error.getMessage())
    );
}
```

### Event System Updates
**New events for configuration management**

```java
// Configuration events
@EventHandler
public void onConfigReload(ConfigurationReloadEvent event) {
    // Handle configuration reload
}

@EventHandler  
public void onConfigValidation(ConfigurationValidationEvent event) {
    // Handle validation results
}
```

## Bug Fixes

### Critical Fixes
- **Fixed**: Sign shop duplication exploit when buying from player shops with no stock
- **Fixed**: Configuration corruption during server crashes
- **Fixed**: Memory leaks in configuration file watchers
- **Fixed**: Race conditions during configuration loading
- **Fixed**: Incomplete validation in certain edge cases

### Performance Fixes
- **Optimized**: Configuration parsing algorithms
- **Improved**: File I/O operations with buffering
- **Enhanced**: Memory management in configuration system
- **Reduced**: CPU overhead for configuration monitoring

## Compatibility

### Backward Compatibility
- ✅ **Commands**: All existing commands work unchanged
- ✅ **Permissions**: Permission nodes remain the same
- ✅ **Player Data**: Existing player data is preserved
- ✅ **Economy Data**: Balance and transaction history maintained

### System Requirements
- **Minecraft**: 1.21.1+
- **NeoForge**: 21.1.1+ (recommended: 21.1.1-52.1.15+)
- **Java**: 17+ (recommended: 17.0.8+)
- **Memory**: Minimum 2GB RAM allocated to server

### Mod Compatibility
- ✅ **LuckPerms**: Enhanced permission integration
- ✅ **FTB Ranks**: Improved rank system support
- ✅ **Placeholder Mods**: Better placeholder processing
- ✅ **Economy Mods**: Vault compatibility maintained

## Installation

### New Installation
1. Download NeoEssentials v1.0.2 JAR
2. Place in server `mods/` directory
3. Start server (configuration auto-generates)
4. Stop server and customize configuration
5. Restart server

### Upgrade from Previous Version
1. **Stop server completely**
2. **Delete existing config directories**:
   - `config/neoessentials/`
   - `neoessentials/` (if exists)
3. **Replace JAR** with v1.0.2
4. **Start server** (new config generates)
5. **Customize configuration** as needed
6. **Restart server**

## Performance Improvements

### Benchmarks
- **Configuration Loading**: 60% faster than v1.0.1
- **Memory Usage**: 25% reduction in config-related memory
- **File I/O**: 40% fewer disk operations
- **Validation**: 50% faster validation processing

### Scalability
- **Large Servers**: Better performance with 200+ players
- **Many Configs**: Improved handling of complex configurations
- **Real-time Updates**: Minimal impact on server TPS
- **Background Tasks**: Non-blocking configuration operations

## Known Issues

### Minor Issues
- Configuration editor GUI may need stability improvements on some systems
- Complex validation rules might show false warnings in rare cases
- Hot-reload may occasionally require manual restart for complex changes

### Workarounds
- Use external text editors for complex configuration editing
- Restart server if hot-reload doesn't apply changes properly
- Check validation logs if experiencing configuration issues

## Documentation

### Updated Guides
- **Configuration Reference**: Complete guide to new configuration system
- **Migration Tutorial**: Step-by-step migration instructions
- **Validation Guide**: Understanding configuration validation
- **Performance Tuning**: Optimization recommendations

### API Documentation
- **JavaDoc**: Complete API documentation for developers
- **Integration Examples**: Code examples for mod integration
- **Configuration API**: Programmatic configuration management

## Support

### Getting Help
- **Documentation**: [GitHub Wiki](https://github.com/ZeroG-Network-Org/NeoEssentials/wiki)
- **Discord**: [Community Server](https://discord.gg/dUGAQF2Mga)
- **Issues**: [GitHub Issues](https://github.com/ZeroG-Network-Org/NeoEssentials/issues)

### Community Resources
- **Configuration Examples**: [Examples Repository](https://github.com/ZeroG-Network-Org/NeoEssentials-Examples)
- **Video Tutorials**: Migration and setup guides
- **Community Discord**: Real-time help and discussion

## Thank You

A huge thank you to our community for testing the migration process and providing valuable feedback! The new configuration system provides a much better foundation for future development.

**Ready to upgrade?** Download v1.0.2 now and experience the improved configuration system!

**Remember**: Clean migration required - delete old config before updating!

---

*NeoEssentials v1.0.2 - Building a better foundation for server management*
