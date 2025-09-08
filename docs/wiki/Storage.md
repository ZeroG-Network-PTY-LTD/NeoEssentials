# Data Storage

NeoEssentials implements a sophisticated file-based storage system using JSON format with memory-optimized caching and asynchronous operations. The system provides efficient data management for player information, configuration files, and persistent data.

## 🗄️ Storage Architecture

### Advanced File-Based Storage

NeoEssentials uses a memory-optimized storage system:

#### JSON Storage with Advanced Features
- **Asynchronous operations**: Non-blocking file I/O using CompletableFuture
- **Memory-efficient caching**: Soft reference cache with automatic cleanup
- **Atomic file operations**: Temporary file writes with atomic moves
- **Streaming JSON processing**: Reduces memory usage for large files
- **Thread-safe operations**: Concurrent access with read/write locks

#### Configuration Management via ConfigManager
- **JSON configuration files**: Located in `config/neoessentials/`
- **Unified management**: Single ConfigManager handles all configuration
- **Automatic cleanup**: Removes unwanted configuration files
- **Memory optimization**: Efficient Gson configuration

## 📂 Data Organization

### Actual File Structure

The implemented file structure used by NeoEssentials 1.0.2:

```
config/neoessentials/
├── config.json                # Main configuration (MainConfig)
├── commands.json              # Command settings (CommandsConfig)
├── customPlaceholders.json    # Custom placeholder definitions
├── tablist.json               # Tablist/scoreboard/bossbar config
└── shops.json                 # Shop system configuration

neoessentials/                 # Runtime data storage (StorageManager)
├── players/                   # Player data files (UUID.json)
│   ├── {uuid}.json           # Individual player data
│   └── ...
└── {category}/               # Category-based data organization
    ├── file1.json
    └── file2.json
```

## ⚙️ Storage Configuration

### StorageManager Configuration

The StorageManager is optimized for performance and memory efficiency:

**Storage Features:**
- **Max Cache Size**: 500 entries with automatic cleanup
- **Soft References**: Memory-sensitive caching that responds to GC pressure
- **Thread Safety**: Concurrent HashMap with ReentrantReadWriteLock
- **Async Execution**: Uses ForkJoinPool.commonPool() for non-blocking operations
- **Atomic Operations**: Temporary files with atomic moves for data integrity

### Configuration Files Managed

ConfigManager handles these specific JSON files:

#### Core Configuration Files
- **`config.json`**: Main configuration (MainConfig class)
  - Language settings
  - Feature toggles (FTB, Discord, moderation)
  - Spawn, kit, chat, and item management settings

- **`commands.json`**: Command system configuration (CommandsConfig class)
  - Command costs, cooldowns, and warmups
  - Discord logging settings
  - Command-specific settings

- **`customPlaceholders.json`**: Custom placeholder definitions
  - User-defined placeholders (conditional, static, animated)
  - Integration with PlaceholderManager
  - Default examples (afk_tag, welcome_message, animations)

- **`tablist.json`**: Display system configuration (TablistConfig class)
  - Tablist, scoreboard, and bossbar unified configuration
  - FTB integration support
  - Multi-line layout support

- **`shops.json`**: Shop system configuration (ShopsConfig class)
  - General shop settings (admin/player shops, tax rates)
  - Discord integration for shop notifications

## 🔧 Storage Management

### StorageManager API

The StorageManager provides comprehensive data management:

#### Player Data Operations
```java
// Save player data
StorageManager.getInstance().savePlayerData(playerUuid, playerDataMap);

// Load player data  
CompletableFuture<Map<String, Object>> playerData = 
    StorageManager.getInstance().loadPlayerData(playerUuid);
```

#### Generic Data Operations
```java
// Save any data by category and filename
StorageManager.getInstance().saveDataAsync("homes", "player_homes", homesData);

// Load data by category and filename
StorageManager.getInstance().loadDataAsync("warps", "server_warps", WarpsClass.class);

// Delete files with cache cleanup
StorageManager.getInstance().deleteFile("category", "filename");
```

#### Cache Management
```java
// Get cache statistics
Map<String, Object> stats = StorageManager.getInstance().getCacheStats();
// Returns: cacheSize, maxCacheSize, activeReferences
```

### ConfigManager API

The ConfigManager handles all configuration operations:

#### Configuration Access
```java
ConfigManager configManager = ConfigManager.getInstance();

// Access specific configurations
MainConfig mainConfig = configManager.getMainConfig();
CommandsConfig commandsConfig = configManager.getCommandsConfig();
TablistConfig tablistConfig = configManager.getTablistConfig();
```

#### Configuration Operations
```java
// Save all configurations
configManager.saveAll();

// Reload all configurations  
configManager.reloadAll();

// Check if config file exists
boolean exists = configManager.configExists("config.json");
```

## 📊 Available Commands

### Configuration Management Commands

#### `/config` Command
Administrative configuration management:

```bash
/config reload     # Reload all configurations
/config save       # Save all configurations
/config status     # Show configuration file status
/config validate   # Validate configuration files
```

**Permission Required**: `neoessentials.admin.full` (operator level 4)

**Example Output for `/config status`**:
```
=== Configuration Status ===
✓ config.json: Loaded and valid
✓ commands.json: Loaded and valid  
✓ customPlaceholders.json: Loaded and valid
✓ tablist.json: Loaded and valid
✓ shops.json: Loaded and valid

All configurations loaded successfully!
```

## 🛠️ Data Management

### Manual Data Operations

#### Configuration File Management
Since configurations are stored in JSON format:

1. **View configurations**: Open JSON files in `config/neoessentials/`
2. **Edit configurations**: Modify JSON files (backup recommended)
3. **Reload configurations**: Use `/config reload` to apply changes
4. **Validate configurations**: Use `/config validate` to check syntax

#### Player Data Management
Player data is stored in `neoessentials/players/{uuid}.json`:

1. **View player data**: Open specific UUID.json files
2. **Transfer player data**: Copy JSON files between servers
3. **Reset player data**: Delete specific UUID.json files
4. **Backup player data**: Copy entire players/ directory

## 🔍 Storage Monitoring

### Performance Monitoring

#### Cache Statistics
Monitor storage performance through cache metrics:

```java
Map<String, Object> stats = StorageManager.getInstance().getCacheStats();
// Provides: current cache size, max cache size, active references
```

#### File System Health
- **Atomic Operations**: All writes use temporary files with atomic moves
- **Memory Management**: Soft references automatically clean up under memory pressure
- **Thread Safety**: Concurrent access safely handled with read/write locks

### Data Integrity

#### Built-in Protections
- **Atomic File Writes**: Prevents data corruption during writes
- **Fallback Mechanisms**: Graceful handling of filesystem limitations
- **Error Handling**: Comprehensive error logging and recovery
- **Memory Optimization**: Streaming JSON processing for large files

## 🔧 Troubleshooting

### Common Storage Issues

#### Configuration Problems
**Issue**: Configuration not loading
**Solution**:
1. Use `/config validate` to check JSON syntax
2. Use `/config reload` to reload configurations
3. Check file permissions in `config/neoessentials/`
4. Review server logs for specific errors

#### Performance Issues
**Issue**: Slow data operations
**Solution**:
1. Monitor cache statistics via StorageManager.getCacheStats()
2. Check available memory and GC pressure
3. Verify disk space and I/O performance
4. Review async operation completion times

#### Data Corruption
**Issue**: Corrupted JSON files
**Solution**:
1. Use JSON validation tools to identify syntax errors
2. Restore from configuration backups (if available)
3. Delete corrupted file to regenerate with defaults
4. Check filesystem integrity and available disk space

### Data Recovery

#### Configuration Recovery
1. **Check backups**: Look for backup files created by ConfigManager
2. **Restore process**: Copy backup over corrupted configuration
3. **Reload**: Use `/config reload` to apply restored configuration
4. **Verify**: Use `/config status` to confirm successful loading

#### Player Data Recovery
1. **Manual backup**: Copy `neoessentials/players/` directory regularly
2. **Partial recovery**: Extract valid data from partially corrupted files
3. **Reset individual players**: Delete specific UUID.json files
4. **Server-wide reset**: Delete entire players/ directory for fresh start

## ⚠️ Limitations & Best Practices

### Current Limitations

#### Storage Backend
- **File-based only**: No database support in current implementation
- **Local storage**: Files stored locally on server filesystem
- **Manual backup**: No automatic backup system for player data
- **Single-threaded writes**: Atomic operations require sequential file writes

### Best Practices

#### For All Server Sizes
- **Regular manual backups**: Copy `config/neoessentials/` and `neoessentials/` directories
- **Monitor disk space**: Ensure adequate free space for data growth
- **Use `/config` commands**: Leverage built-in configuration management
- **Test configuration changes**: Validate configs before applying to production

#### Performance Optimization
- **Monitor cache performance**: Use StorageManager cache statistics
- **Avoid frequent config reloads**: Only reload when necessary
- **Use async operations**: Leverage CompletableFuture for non-blocking operations
- **Regular maintenance**: Clean up old/unused player data files

---

**Related Documentation**: [Configuration](Configuration.md) | [Performance](Performance.md) | [Commands](Commands.md)

*Last Updated: September 7, 2025 - NeoEssentials 1.0.2*
