# Database Integration

NeoEssentials provides robust integration with various database systems for storing player data, configuration, and other information. This guide covers database setup, configuration, migration, and optimization.

## Supported Database Systems

NeoEssentials supports the following database systems:

- **SQLite** (embedded, default)
- **MySQL** / **MariaDB**
- **PostgreSQL**
- **MongoDB** (via extension)

## When to Use Database Integration

While the default file-based storage works well for small servers, consider using a database for:

- Servers with many players (100+)
- Multi-server networks
- Data that needs to be accessed by external applications
- Environments requiring frequent backups
- Situations where data integrity is critical

## Basic Database Configuration

Database settings are configured in `config/neoessentials/storage.toml`:

```toml
[storage]
# Available types: file, sqlite, mysql, postgresql, mongodb
type = "sqlite"  # Default
syncInterval = 300  # Save interval in seconds
useAsyncWrites = true
cacheDuration = 600  # Cache duration in seconds
enableCompression = false

# SQLite configuration (default)
[storage.sqlite]
filename = "neoessentials.db"
backups = 3

# MySQL configuration
[storage.mysql]
host = "localhost"
port = 3306
database = "neoessentials"
username = "neouser"
password = "password"
useSSL = true
connectionPoolSize = 5
tablePrefix = "ne_"

# PostgreSQL configuration
[storage.postgresql]
host = "localhost"
port = 5432
database = "neoessentials"
username = "neouser"
password = "password"
sslMode = "require"
connectionPoolSize = 5
tablePrefix = "ne_"

# MongoDB configuration
[storage.mongodb]
connectionString = "mongodb://localhost:27017"
database = "neoessentials"
authDatabase = "admin"
username = "neouser"
password = "password"
collectionPrefix = "ne_"
```

## Setting Up Each Database Type

### SQLite (Default)

SQLite is the simplest setup and requires no external database server:

```toml
[storage]
type = "sqlite"

[storage.sqlite]
filename = "neoessentials.db"  # Path relative to server directory
backups = 3  # Number of backup files to keep
journalMode = "WAL"  # Write-Ahead Logging for better performance
```

### MySQL / MariaDB

For MySQL or MariaDB:

1. Create a MySQL database and user:

```sql
CREATE DATABASE neoessentials;
CREATE USER 'neouser'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON neoessentials.* TO 'neouser'@'localhost';
FLUSH PRIVILEGES;
```

2. Configure NeoEssentials:

```toml
[storage]
type = "mysql"

[storage.mysql]
host = "localhost"
port = 3306
database = "neoessentials"
username = "neouser"
password = "password"
useSSL = true
connectionPoolSize = 10
tablePrefix = "ne_"
```

### PostgreSQL

For PostgreSQL:

1. Create a PostgreSQL database and user:

```sql
CREATE DATABASE neoessentials;
CREATE USER neouser WITH ENCRYPTED PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE neoessentials TO neouser;
```

2. Configure NeoEssentials:

```toml
[storage]
type = "postgresql"

[storage.postgresql]
host = "localhost"
port = 5432
database = "neoessentials"
username = "neouser"
password = "password"
sslMode = "require"
connectionPoolSize = 10
tablePrefix = "ne_"
```

### MongoDB

For MongoDB:

1. Create a MongoDB database and user:

```javascript
use neoessentials
db.createUser({
  user: "neouser",
  pwd: "password",
  roles: [{ role: "readWrite", db: "neoessentials" }]
})
```

2. Configure NeoEssentials:

```toml
[storage]
type = "mongodb"

[storage.mongodb]
connectionString = "mongodb://localhost:27017"
database = "neoessentials"
authDatabase = "admin"
username = "neouser"
password = "password"
collectionPrefix = "ne_"
```

## Advanced Database Configuration

### Connection Pooling

For high-traffic servers, configure connection pooling:

```toml
[storage.connectionPool]
initialSize = 5
maxSize = 20
maxIdleTime = 300000  # 5 minutes in milliseconds
validationInterval = 30000  # 30 seconds in milliseconds
```

### Query Timeout

Set query timeout to prevent long-running queries:

```toml
[storage.queryTimeout]
enabled = true
timeoutSeconds = 10
retryCount = 3
retryDelayMillis = 1000
```

### Prepared Statements

Enable prepared statement caching:

```toml
[storage.preparedStatements]
cache = true
cacheSize = 250
```

## Data Migration

### Migrating Between Storage Types

To migrate from one storage type to another:

1. Make a backup of your current data
2. Configure the new storage type in `storage.toml`
3. Run the migration command:

```
/neoessentials:migrate storage <old-type> <new-type>
```

For example, to migrate from SQLite to MySQL:

```
/neoessentials:migrate storage sqlite mysql
```

### Automated Migration

You can also configure automatic migration:

```toml
[storage.migration]
autoMigrate = true
validateAfterMigration = true
keepOldData = true
logMigration = true
```

## Data Security

### Database Security Best Practices

1. **Dedicated User**: Create a database user specifically for NeoEssentials
2. **Minimal Privileges**: Grant only necessary permissions to the database user
3. **Strong Password**: Use a strong, unique password
4. **Encryption**: Enable SSL/TLS connections when possible
5. **Firewall Rules**: Restrict database access to your server's IP address
6. **Regular Updates**: Keep your database server software updated

### Sensitive Data Handling

Configure how sensitive data is handled:

```toml
[storage.security]
encryptSensitiveData = true
encryptionKey = "generate-a-strong-key"  # Or use environment variable: ${NEOESSENTIALS_ENCRYPTION_KEY}
anonymizeIpAddresses = true
logDataAccess = true
```

## Database Backup

### Automated Backup

Configure automatic database backups:

```toml
[storage.backup]
enabled = true
interval = 86400  # Daily backup in seconds
maxBackups = 7  # Keep a week of backups
backupPath = "backups/database"
compressBackups = true
backupBeforeUpdates = true
```

### Manual Backup

Run manual backup commands:

```
/neoessentials:database backup create [name]
/neoessentials:database backup list
/neoessentials:database backup restore <name>
```

## Performance Optimization

### Optimizing Database Performance

```toml
[storage.performance]
useIndexes = true
batchSize = 50
enableQueryCache = true
queryCacheSize = 100
queryCacheExpiry = 300  # Seconds
useAsyncOperations = true
```

### Database-Specific Optimizations

#### MySQL Optimizations

```toml
[storage.mysql.optimization]
useCompression = true
autoReconnect = true
useUnicode = true
characterEncoding = "utf8mb4"
useServerPrepStmts = true
cachePrepStmts = true
prepStmtCacheSize = 250
prepStmtCacheSqlLimit = 2048
```

#### PostgreSQL Optimizations

```toml
[storage.postgresql.optimization]
tcpKeepAlive = true
ApplicationName = "NeoEssentials"
reWriteBatchedInserts = true
```

## Multi-Server Setup

For servers in a network, sharing the same database:

```toml
[storage.multiServer]
enabled = true
serverId = "survival1"  # Unique identifier for this server
networkId = "mynetwork"  # Shared network identifier
syncInterval = 10  # Seconds between synchronization
resolveConflicts = "newest"  # Options: newest, oldest, manual
```

## Monitoring and Maintenance

### Database Health Commands

Monitor database health with these commands:

```
/neoessentials:database status
/neoessentials:database stats
/neoessentials:database verify
/neoessentials:database repair
/neoessentials:database optimize
```

### Database Logging

Configure database logging:

```toml
[storage.logging]
enabled = true
logLevel = "INFO"  # DEBUG, INFO, WARN, ERROR
logQueries = false
logSlowQueries = true
slowQueryThreshold = 1000  # Milliseconds
```

## Troubleshooting

### Common Database Issues

1. **Connection Failures**
   - Check database server is running
   - Verify credentials are correct
   - Check firewall rules
   - Verify network connectivity

2. **Slow Performance**
   - Add appropriate indexes
   - Increase connection pool size
   - Optimize query cache settings
   - Check database server resources

3. **Data Corruption**
   - Run database verification
   - Restore from backup if necessary
   - Check for disk space issues

### Diagnostic Commands

```
/neoessentials:database diagnostic
/neoessentials:database query test
/neoessentials:database connections
```

## Best Practices

1. **Regular Backups**: Schedule regular database backups
2. **Monitoring**: Set up monitoring for database health
3. **Updates**: Keep database software and drivers updated
4. **Sizing**: Size your database server appropriately for your player count
5. **Maintenance**: Schedule regular maintenance windows for optimization
6. **Documentation**: Document your database setup and procedures

## Advanced Usage

### Custom SQL Queries

For advanced users, NeoEssentials allows running custom queries through API:

```java
DatabaseManager dbManager = NeoEssentials.getInstance().getDatabaseManager();
dbManager.executeQuery("SELECT COUNT(*) FROM ne_players WHERE last_login > ?", 
    preparedStatement -> {
        preparedStatement.setLong(1, System.currentTimeMillis() - (86400000 * 7));
    }, 
    resultSet -> {
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        return 0;
    });
```

### External Access

For external application access, consider:

1. Creating a read-only database user
2. Using a database proxy
3. Implementing API endpoints instead of direct database access

## Example Setups

### Small Server (< 50 players)

```toml
[storage]
type = "sqlite"
syncInterval = 300
useAsyncWrites = true

[storage.sqlite]
filename = "neoessentials.db"
backups = 3
```

### Medium Server (50-200 players)

```toml
[storage]
type = "mysql"
syncInterval = 120
useAsyncWrites = true
cacheDuration = 300

[storage.mysql]
host = "localhost"
port = 3306
database = "neoessentials"
username = "neouser"
password = "password"
useSSL = true
connectionPoolSize = 10

[storage.connectionPool]
initialSize = 5
maxSize = 15
```

### Large Server Network (200+ players)

```toml
[storage]
type = "postgresql"
syncInterval = 60
useAsyncWrites = true
cacheDuration = 180

[storage.postgresql]
host = "db.example.com"
port = 5432
database = "neoessentials"
username = "neouser"
password = "password"
sslMode = "require"
connectionPoolSize = 20

[storage.connectionPool]
initialSize = 10
maxSize = 30

[storage.multiServer]
enabled = true
serverId = "main-survival"
networkId = "mynetwork"
syncInterval = 10
```

## Additional Resources

- [Server Optimization](Performance-Optimization) guide for database performance tips
- [Multi-Server Configuration](Multi-Server-Configuration) for network database setup
- [NeoEssentials API Documentation](API-Documentation) for custom database usage
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for database support
