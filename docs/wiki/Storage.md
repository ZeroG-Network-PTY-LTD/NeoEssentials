# Data Storage

NeoEssentials implements a basic file-based data storage system using JSON format. The system provides simple data management for player information, configuration files, and other persistent data.

## 🗄️ Storage Architecture

### File-Based Storage

NeoEssentials uses a simple file-based storage system:

#### JSON Files (Primary Storage)
- **Human-readable format**: Easy to edit manually if needed
- **Structured data**: Well-organized JSON structure
- **Async operations**: Non-blocking file operations
- **Basic caching**: In-memory cache for performance

#### Configuration Management
- **JSON configuration files**: Located in `config/neoessentials/`
- **Hot-reload capability**: Configuration changes apply automatically
- **Backup system**: Automatic configuration backups
- **Validation**: Basic configuration validation

## 📂 Data Organization

### File Structure

The actual file structure used by NeoEssentials:

```
config/neoessentials/
├── main.json                  # Core mod settings
├── economy.json               # Economy configuration
├── homes.json                 # Home system settings
├── kits.json                  # Kit definitions
├── warps.json                 # Warp locations
├── moderation.json            # Moderation settings
├── messaging.json             # Chat and messaging
├── tablist.json               # Tablist customization
├── spawn.json                 # Spawn configuration
└── backup/                    # Configuration backups
    ├── main_[timestamp].json
    ├── economy_[timestamp].json
    └── ...

run/neoessentials/             # Runtime data storage
├── players/                   # Player data (runtime)
├── homes/                     # Player homes (runtime)
├── warps/                     # Server warps (runtime)
├── economy/                   # Economy data (runtime)
└── cache/                     # Temporary cache files
```

### JSON Data Format

#### Player Data Structure
```json
{
  "uuid": "player-uuid-here",
  "username": "PlayerName",
  "firstJoin": 1691234567890,
  "lastSeen": 1691234567890,
  "balance": 1500.50,
  "homes": {
    "home": {
      "world": "minecraft:overworld",
      "x": 100.5,
      "y": 64.0,
      "z": 200.5,
      "yaw": 180.0,
      "pitch": 0.0
    }
  },
  "settings": {
    "language": "en_US",
    "notifications": true
  }
}
```

#### Configuration Structure
```json
{
  "main": {
    "serverName": "My Server",
    "enableEssentialCommands": true,
    "enableTeleportation": true,
    "enableEconomy": true
  },
  "features": {
    "homeSystem": true,
    "warpSystem": true,
    "kitSystem": true
  }
}
```

## ⚙️ Storage Configuration

### Basic Storage Settings

Storage behavior is configured through the main configuration:

```json
{
  "storage": {
    "autoSave": true,
    "autoSaveInterval": 300,
    "enableCache": true,
    "cacheSize": 100,
    "createBackups": true
  }
}
```

**Configuration Options:**
- **autoSave**: Automatically save data periodically
- **autoSaveInterval**: Seconds between auto-saves (300 = 5 minutes)
- **enableCache**: Use in-memory cache for better performance
- **cacheSize**: Maximum number of cached entries
- **createBackups**: Create backups before modifying configurations

### Cache Configuration

```json
{
  "cache": {
    "playerData": true,
    "homeData": true,
    "warpData": true,
    "configData": true,
    "maxAge": 3600
  }
}
```

## 🔧 Storage Management

### Configuration Management

The ConfigManager handles all configuration operations:

#### Features
- **JSON-based configuration**: Human-readable format
- **Hot-reload**: Changes apply without restart
- **Automatic backups**: Backups created before changes
- **Validation**: Basic configuration validation
- **Caching**: Configuration caching for performance

#### Configuration Files Managed
- `main.json` - Core mod settings
- `economy.json` - Economy system configuration
- `homes.json` - Home system settings
- `kits.json` - Kit definitions and settings
- `warps.json` - Warp locations and configuration
- `moderation.json` - Moderation tool settings
- `messaging.json` - Chat and message settings
- `tablist.json` - Tablist customization
- `spawn.json` - Spawn location and settings

### Data Storage Manager

The StorageManager handles runtime data:

#### Features
- **Async file operations**: Non-blocking storage operations
- **JSON format**: Structured, readable data format
- **Caching layer**: In-memory cache for frequently accessed data
- **Category-based organization**: Data organized by type
- **Backup capability**: Create backups of all data

#### Data Categories Managed
- **players**: Player-specific data and settings
- **homes**: Player home locations
- **warps**: Server warp points
- **economy**: Economy and transaction data
- **kits**: Kit usage and cooldown data
- **mail**: Player mail system data

### Player Data Management

Basic player data storage:

#### PlayerDataManager Features
- **UUID-based storage**: Players identified by UUID
- **Session management**: Track player sessions
- **Data persistence**: Save/load player data
- **Memory management**: Efficient data handling

#### Data Types Stored
- **Basic info**: Username, first join, last seen
- **Game data**: Balance, homes, settings
- **Statistics**: Playtime, command usage
- **Preferences**: Language, notification settings

## 📊 Storage Information

### Performance Monitoring

View storage performance with the performance command:

```bash
/performance cache            # View cache statistics
/performance memory           # Check memory usage
/performance clear            # Clear caches
```

**Cache Information:**
- Cache hit rates
- Memory usage by cache
- Number of cached entries
- Cache cleanup statistics

### Storage Statistics

While there are no dedicated storage commands, you can monitor storage through:

#### File System Monitoring
- Check `config/neoessentials/` directory size
- Monitor `run/neoessentials/` for runtime data
- Review backup directory growth

#### Performance Impact
- Use `/performance stats` to see overall system performance
- Monitor memory usage with `/performance memory`
- Check command execution times for storage-related operations

## � Backup System

### Configuration Backups

The ConfigManager automatically creates backups:

#### Backup Features
- **Automatic creation**: Backups created before configuration changes
- **Timestamp naming**: Files named with timestamps for easy identification
- **Storage location**: `config/neoessentials/backup/`
- **File format**: Same JSON format as originals

#### Backup Structure
```
config/neoessentials/backup/
├── main_1691234567890.json
├── economy_1691234567890.json
├── homes_1691234567890.json
└── ...
```

### Manual Backup Creation

The StorageManager can create data backups:

#### Backup Process
1. **Data collection**: Gather all runtime data
2. **Directory creation**: Create timestamped backup directory
3. **File copying**: Copy all data files to backup location
4. **Verification**: Ensure backup completed successfully

#### Backup Location
```
run/neoessentials/backups/
└── backup_[timestamp]/
    ├── players/
    ├── homes/
    ├── warps/
    ├── economy/
    └── ...
```

## 🛠️ Data Management

### Manual Data Operations

#### File-Based Management
Since data is stored in JSON files, you can:

1. **View data**: Open JSON files in any text editor
2. **Edit data**: Modify JSON files carefully (backup first!)
3. **Transfer data**: Copy JSON files between servers
4. **Reset data**: Delete files to reset to defaults

#### Configuration Management
1. **Edit configs**: Modify JSON configuration files
2. **Reload configs**: Changes apply automatically with hot-reload
3. **Restore configs**: Use backup files to restore previous settings
4. **Reset configs**: Delete files to restore defaults

### Data Cleanup

#### Automatic Cleanup
- **Cache cleanup**: Old cache entries removed automatically
- **Memory management**: Garbage collection handles memory cleanup
- **Backup rotation**: Configuration backups managed automatically

#### Manual Cleanup
- **Clear cache**: Use `/performance clear` to clear all caches
- **Remove old backups**: Manually delete old backup files
- **Reset player data**: Delete player JSON files to reset individual players

## 🔧 Troubleshooting

### Common Storage Issues

#### Configuration Problems
**Issue**: Configuration not loading
**Solution**:
1. Check JSON syntax validity
2. Restore from backup if corrupted
3. Delete file to restore defaults
4. Check file permissions

#### Performance Issues
**Issue**: Slow data operations
**Solution**:
1. Clear cache: `/performance clear`
2. Check memory usage: `/performance memory`
3. Restart server to clear all data
4. Reduce cache size in configuration

#### Data Corruption
**Issue**: Corrupted JSON files
**Solution**:
1. Restore from backup files
2. Validate JSON format online
3. Recreate file with default content
4. Check disk space and permissions

### Data Recovery

#### Configuration Recovery
1. **Use backups**: Configuration backups in `config/neoessentials/backup/`
2. **Restore process**: Copy backup file over corrupted file
3. **Restart**: Restart server to reload configuration
4. **Verify**: Check that configuration loaded correctly

#### Player Data Recovery
1. **Backup restoration**: Use backup files if available
2. **Manual recreation**: Create new player data files
3. **Reset to defaults**: Delete corrupted files for fresh start
4. **Partial recovery**: Extract valid data from corrupted files

### Performance Optimization

#### Cache Optimization
- **Monitor cache performance**: Use `/performance cache`
- **Adjust cache size**: Modify cache settings in configuration
- **Clear cache regularly**: Use `/performance clear` during low activity
- **Monitor memory**: Check memory usage with `/performance memory`

#### File System Optimization
- **Regular cleanup**: Remove old backup files periodically
- **Monitor disk space**: Ensure adequate free space
- **Check file permissions**: Verify proper read/write access
- **Use SSD storage**: Faster storage improves performance

## ⚠️ Limitations & Recommendations

### Current Limitations

#### Storage Backend
- **File-based only**: No database support currently
- **Local storage**: No remote or cloud storage options
- **Limited scalability**: May not scale well for very large servers
- **No replication**: No automatic data replication or redundancy

#### Data Management
- **Manual operations**: Most data management requires manual file operations
- **Limited tools**: No built-in data migration or analysis tools
- **Basic backup**: Simple backup system without advanced features
- **No compression**: Data stored uncompressed

### Recommendations

#### For Small to Medium Servers
- **Use default settings**: File-based storage works well
- **Regular backups**: Create manual backups periodically
- **Monitor performance**: Use built-in performance tools
- **Basic maintenance**: Clear caches and clean up old files

#### For Large Servers
- **External backup solutions**: Use dedicated backup plugins
- **Database migration**: Consider migrating to database-based systems
- **Performance monitoring**: Use external monitoring tools
- **Load balancing**: Consider distributed storage solutions

#### Best Practices
- **Regular backups**: Back up configuration and data regularly
- **Monitor disk space**: Ensure adequate storage space
- **Test restores**: Verify backup restoration procedures
- **Documentation**: Document any custom configurations or modifications

---

**Related Documentation**: [Configuration](Configuration.md) | [Performance](Performance.md) | [Essential Commands](Essential-Commands.md)

*Last Updated: August 9, 2025*
    name VARCHAR(32),
    world VARCHAR(64),
    x DOUBLE, y DOUBLE, z DOUBLE,
    yaw FLOAT, pitch FLOAT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_uuid) REFERENCES players(uuid)
);

-- Economy transactions
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_uuid VARCHAR(36),
    to_uuid VARCHAR(36),
    amount DECIMAL(15,2),
    type ENUM('payment', 'shop_buy', 'shop_sell', 'admin'),
    description TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## ⚙️ Storage Configuration

### Backend Selection
Configure storage backend in `config/neoessentials/storage.toml`:

```toml
[storage]
# Primary storage backend
backend = "file"  # file, mysql, postgresql, mongodb, sqlite

# Data directory for file storage
dataDirectory = "neoessentials/data"

# Enable compression for file storage
compression = true

# Auto-save interval (seconds)
autoSaveInterval = 300

[storage.cache]
# Enable caching layer
enabled = true

# Cache backend
backend = "memory"  # memory, redis, file

# Cache size (MB)
maxSize = 128

# Cache TTL (seconds)
ttl = 3600
```

### Database Configuration
Configure database connections:

```toml
[storage.mysql]
host = "localhost"
port = 3306
database = "neoessentials"
username = "neoessentials"
password = "your_password"

# Connection pool settings
maxConnections = 10
minConnections = 2
connectionTimeout = 30

# Advanced settings
useSSL = false
timezone = "UTC"
charset = "utf8mb4"

[storage.postgresql]
host = "localhost"
port = 5432
database = "neoessentials"
username = "neoessentials"
password = "your_password"
schema = "public"

[storage.mongodb]
connectionString = "mongodb://localhost:27017/neoessentials"
authDatabase = "admin"
```

### Performance Tuning
Optimize storage performance:

```toml
[storage.performance]
# Batch operations
batchSize = 100
batchTimeout = 5000

# Write optimization
asyncWrites = true
writeBuffer = 1024

# Read optimization
readCache = true
prefetchData = true

# Compression settings
compressionLevel = 6
compressionThreshold = 1024
```

## 💾 Data Management Commands

### Basic Data Operations
```bash
/data info                      # Storage system information
/data stats                     # Storage statistics
/data cache                     # Cache status and controls
/data migrate <backend>         # Migrate to different backend
/data verify                    # Verify data integrity
```

### Backup Operations
```bash
/backup create [name]           # Create manual backup
/backup list                    # List available backups
/backup restore <name>          # Restore from backup
/backup delete <name>           # Delete backup
/backup auto                    # Configure automatic backups
```

### Player Data Management
```bash
/playerdata <player>            # View player data summary
/playerdata export <player>     # Export player data
/playerdata import <file>       # Import player data
/playerdata reset <player>      # Reset player data
/playerdata migrate <player>    # Migrate player data
```

### Data Maintenance
```bash
/data cleanup                   # Clean orphaned data
/data optimize                  # Optimize storage
/data repair                    # Repair corrupted data
/data vacuum                    # Compact database/files
/data analyze                   # Analyze storage efficiency
```

## 🔄 Data Migration

### Migration Tools
Migrate between different storage backends:

```bash
/migrate from <source> to <target>    # Full migration
/migrate test <source> to <target>    # Test migration
/migrate status                       # Migration status
/migrate rollback                     # Rollback migration
```

**Supported Migration Paths:**
- File → Database
- Database → File  
- MySQL → PostgreSQL
- Local → Cloud storage
- Legacy formats → Current format

### Migration Configuration
```toml
[migration]
# Backup before migration
createBackup = true

# Verification after migration
verifyData = true

# Batch size for large migrations
batchSize = 1000

# Maximum migration time
timeout = 3600

[migration.mapping]
# Field mappings for different formats
legacy_money = "balance"
legacy_homes = "homes.data"
legacy_warps = "warps.global"
```

## 📊 Data Analytics

### Storage Analytics
Monitor storage usage and performance:

```bash
/data analytics                 # Storage analytics dashboard
/data usage                     # Storage space usage
/data performance               # Performance metrics
/data trends                    # Usage trends over time
```

**Analytics Include:**
- **Storage Usage** - Total space used per data type
- **Growth Trends** - Data growth over time
- **Access Patterns** - Most accessed data
- **Performance Metrics** - Read/write speeds, cache hit rates
- **Error Rates** - Storage operation failure rates

### Data Reports
Generate comprehensive data reports:

```bash
/data report daily              # Daily storage report
/data report weekly             # Weekly summary
/data report player <player>    # Player data report
/data export stats <format>     # Export statistics
```

## 🔒 Data Security

### Encryption
Protect sensitive data with encryption:

```toml
[storage.security]
# Enable data encryption
encryption = true

# Encryption algorithm
algorithm = "AES-256-GCM"

# Key management
keyFile = "neoessentials/security/data.key"
rotateKeys = true
keyRotationInterval = "30d"

# Encrypt specific data types
encryptPlayerData = true
encryptEconomyData = true
encryptSensitiveLogs = true
```

### Access Control
Control data access with permissions:

```yaml
permissions:
  neoessentials.data.view          # View data information
  neoessentials.data.backup        # Create/restore backups
  neoessentials.data.migrate       # Migrate data
  neoessentials.data.admin         # Full data administration
  neoessentials.data.debug         # Debug data issues
```

### Audit Logging
Track all data operations:

```toml
[storage.audit]
# Enable audit logging
enabled = true

# Log all data operations
logOperations = true

# Log file location
auditLog = "neoessentials/logs/data_audit.log"

# Log retention
retentionDays = 90

# Alert on suspicious operations
alertThreshold = 100
```

## 🚀 Performance Optimization

### Caching Strategies
Implement intelligent caching:

```toml
[storage.cache.strategies]
# Player data caching
playerData = {
  strategy = "LRU",
  maxSize = 1000,
  ttl = 1800
}

# Economy data caching
economyData = {
  strategy = "write_through",
  maxSize = 500,
  ttl = 900
}

# Configuration caching
configData = {
  strategy = "write_back",
  maxSize = 100,
  ttl = 3600
}
```

### Connection Pooling
Optimize database connections:

```toml
[storage.pool]
# Connection pool size
minConnections = 5
maxConnections = 20
idleConnections = 10

# Connection timeouts
connectionTimeout = 30
idleTimeout = 300
maxLifetime = 1800

# Pool monitoring
monitorConnections = true
validateConnections = true
```

### Batch Operations
Optimize bulk operations:

```toml
[storage.batch]
# Enable batch processing
enabled = true

# Batch sizes
insertBatch = 100
updateBatch = 50
deleteBatch = 25

# Batch timeouts
batchTimeout = 5000
maxWaitTime = 10000
```

## 🔧 Data Recovery

### Backup System
Comprehensive backup and recovery:

```toml
[backup]
# Automatic backup schedule
enabled = true
schedule = "0 2 * * *"  # Daily at 2 AM

# Backup retention
dailyBackups = 7
weeklyBackups = 4
monthlyBackups = 12

# Backup compression
compression = true
compressionLevel = 6

# Backup verification
verifyBackups = true
testRestore = false

[backup.locations]
# Multiple backup locations
local = "neoessentials/backups"
remote = "ftp://backup.server.com/neoessentials"
cloud = "s3://your-bucket/neoessentials"
```

### Recovery Procedures
Structured data recovery process:

```bash
/recovery start <backup>        # Start recovery process
/recovery status                # Check recovery status
/recovery verify <backup>       # Verify backup integrity
/recovery partial <data_type>   # Partial recovery
/recovery rollback              # Rollback recovery
```

### Disaster Recovery
Prepare for disaster scenarios:

```toml
[disaster_recovery]
# Enable disaster recovery features
enabled = true

# Replication settings
replication = {
  enabled = true,
  servers = ["backup1.server.com", "backup2.server.com"],
  interval = 300
}

# Failover configuration
failover = {
  automatic = true,
  timeout = 60,
  healthCheck = true
}
```

## 🛠️ Troubleshooting

### Common Storage Issues

#### Database Connection Problems
- Check connection credentials
- Verify network connectivity
- Review firewall settings
- Test with database client

#### File Permission Errors
- Check file system permissions
- Verify directory ownership
- Review security policies
- Test write access

#### Data Corruption
- Run data verification: `/data verify`
- Check file system integrity
- Review recent backups
- Use recovery tools

#### Performance Issues
- Monitor cache hit rates
- Check connection pool usage
- Review query performance
- Optimize batch sizes

### Debug Tools
```bash
/data debug connection          # Test database connection
/data debug performance         # Performance diagnostics
/data debug corruption          # Check for corruption
/data debug cache               # Cache diagnostics
```

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - Storage configuration options
- **[Performance](Performance.md)** - Performance optimization guide
- **[Security Features](Security.md)** - Data security and encryption
- **[API Reference](API.md)** - Data storage API documentation

*Last Updated: August 6, 2025*
