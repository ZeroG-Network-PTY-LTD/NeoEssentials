# Data Storage

NeoEssentials implements a flexible and robust data storage system that supports multiple storage backends, automatic backups, and efficient data management. This system handles player data, configuration, logs, and all persistent information.

## 🗄️ Storage Architecture

### Storage Backends
NeoEssentials supports multiple storage backends to meet different server needs:

#### File-Based Storage (Default)
- **JSON Files** - Human-readable, easy to edit
- **YAML Files** - Configuration-friendly format
- **Binary Files** - Compact, fast access for large datasets
- **SQLite Database** - Local database with SQL capabilities

#### Database Storage (Advanced)
- **MySQL** - Popular relational database
- **PostgreSQL** - Advanced relational database
- **MongoDB** - NoSQL document database
- **Redis** - In-memory data structure store

#### Hybrid Storage
- **Configuration** - YAML/TOML files for easy editing
- **Player Data** - Database for scalability
- **Logs** - File-based for simplicity
- **Cache** - Redis for performance

## 📂 Data Organization

### File Structure
Default file-based storage organization:

```
neoessentials/
├── data/
│   ├── players/               # Player data
│   │   ├── uuid1.json
│   │   ├── uuid2.json
│   │   └── ...
│   ├── homes/                 # Player homes
│   │   ├── uuid1.yml
│   │   └── ...
│   ├── warps/                 # Server warps
│   │   ├── spawn.yml
│   │   ├── shop.yml
│   │   └── ...
│   ├── economy/               # Economy data
│   │   ├── balances.json
│   │   ├── transactions.log
│   │   └── shop_data.json
│   └── cache/                 # Temporary cache files
├── backups/                   # Automatic backups
│   ├── daily/
│   ├── weekly/
│   └── manual/
└── logs/                      # System logs
    ├── debug.log
    ├── events.log
    └── security.log
```

### Database Schema
When using database storage, NeoEssentials creates optimized tables:

```sql
-- Player data table
CREATE TABLE players (
    uuid VARCHAR(36) PRIMARY KEY,
    username VARCHAR(16) NOT NULL,
    first_join TIMESTAMP,
    last_seen TIMESTAMP,
    playtime BIGINT DEFAULT 0,
    language VARCHAR(10) DEFAULT 'en_US',
    balance DECIMAL(15,2) DEFAULT 0.00,
    data JSON
);

-- Homes table
CREATE TABLE homes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36),
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
