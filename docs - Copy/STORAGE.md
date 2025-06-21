# NeoEssentials Storage System

NeoEssentials supports multiple storage backends for all player and server data. This document explains how the storage system works and how to configure it.

## Available Storage Backends

NeoEssentials supports the following storage backends:

1. **JSON** - Default storage method. Data is stored in JSON files in the `neoessentials` directory.
2. **SQLite** - Data is stored in a SQLite database file in the `neoessentials` directory.
3. **MySQL** - Data is stored in a MySQL database server.

## Configuration

Storage settings can be configured in the `neoessentials-database.toml` file, which is located in the server's `config` directory.

### Basic Configuration

The most important setting is the `storage_type` which determines which storage backend to use:

```toml
# Storage type: JSON, SQLITE, MYSQL
storage_type = "JSON"
```

Possible values are:
- `JSON` - Use JSON file storage
- `SQLITE` - Use SQLite database storage
- `MYSQL` - Use MySQL database storage

### MySQL Configuration

If you choose `MYSQL` as your storage type, you need to configure the MySQL connection details:

```toml
[mysql]
# MySQL Server host
host = "localhost"
# MySQL Server port
port = 3306
# MySQL database name
database = "neoessentials"
# MySQL username
username = "root"
# MySQL password
password = ""
# Whether to use SSL for MySQL connections
use_ssl = false
# Prefix for MySQL tables
table_prefix = "ne_"
```

### SQLite Configuration

If you choose `SQLITE` as your storage type, you can configure the database file name:

```toml
[sqlite]
# SQLite database filename (without extension)
filename = "neoessentials"
```

## Storage System Architecture

The storage system is built around the following components:

1. **StorageHandler Interface** - Defines the contract that all storage handlers must implement.
2. **JsonStorageHandler** - Implements the StorageHandler interface for JSON file storage.
3. **SQLiteStorageHandler** - Implements the StorageHandler interface for SQLite database storage.
4. **MySQLStorageHandler** - Implements the StorageHandler interface for MySQL database storage.
5. **DatabaseConnectionManager** - Manages database connections using connection pooling (HikariCP).
6. **StorageFactory** - Creates the appropriate storage handler based on configuration.
7. **DataManager** - Manages the active storage handler and provides access to different data managers.

## Data Organization

### JSON Storage

When using JSON storage, data is organized in the following structure:

- `neoessentials/` - Base directory
  - `homes/` - Player home data (one file per player)
    - `<uuid>.json` - Home data for a player
  - `economy/` - Economy data (one file per player)
    - `<uuid>.json` - Economy data for a player
  - `warps.json` - Server warps
  - `kits.json` - Server kits
  - `spawn.json` - Server spawn location

### Database Storage (SQLite/MySQL)

When using database storage, data is organized in tables:

- `homes` - Player home data
  - `uuid` - Player UUID
  - `home_name` - Name of the home
  - `dimension` - Dimension ID (e.g., "minecraft:overworld")
  - `x`, `y`, `z` - Position coordinates
  - `pitch`, `yaw` - Player rotation
  - Primary Key: (`uuid`, `home_name`)

- `warps` - Server warps
  - `name` - Warp name (Primary Key)
  - `dimension` - Dimension ID
  - `x`, `y`, `z` - Position coordinates
  - `pitch`, `yaw` - Player rotation
  - `permission` - Permission node required (nullable)

- `economy` - Player economy data
  - `uuid` - Player UUID (Primary Key)
  - `balance` - Player balance as string (BigDecimal)

- `economy_transactions` - Economy transaction history
  - `id` - Auto-incrementing transaction ID
  - `uuid` - Player UUID
  - `other_uuid` - UUID of other player involved (for transfers)
  - `transaction_type` - Type of transaction (deposit, withdraw, transfer, etc.)
  - `amount` - Transaction amount
  - `balance_after` - Balance after the transaction
  - `description` - Description of the transaction
  - `timestamp` - Unix timestamp of the transaction

- `kits` - Server kits
  - `name` - Kit name (Primary Key)
  - `cooldown` - Cooldown time in milliseconds
  - `permission` - Permission node required (nullable)
  - `price` - Price of the kit (0 for free kits)
  - `items_json` - JSON representation of the kit items

- `kit_cooldowns` - Kit usage cooldowns
  - `uuid` - Player UUID
  - `kit_name` - Kit name
  - `timestamp` - Timestamp when the kit was last used
  - Primary Key: (`uuid`, `kit_name`)

- `spawn_data` - Server spawn location
  - `id` - Always 1 (ensures only one spawn entry)
  - `spawn_json` - JSON representation of spawn data

For MySQL, all table names are prefixed with the configured table prefix (default: `ne_`).

## Migrating Between Storage Systems

NeoEssentials currently does not provide automatic migration between storage systems. If you want to change your storage type, you'll need to:

1. Stop the server
2. Export data from your current system (using commands or database tools)
3. Change the storage_type in the config
4. Import data into the new system
5. Start the server

## Best Practices

- **JSON**: Best for small servers with few players.
- **SQLite**: Good for medium-sized servers where you want better performance than JSON but don't want to set up a MySQL server. The current implementation uses connection pooling via HikariCP for optimal performance.
- **MySQL**: Best for large servers or server networks where you need to share data between multiple servers.

## Performance Considerations

- JSON storage is the simplest but less efficient for large amounts of data.
- SQLite is more efficient than JSON but still uses local file storage.
- MySQL can handle the largest amount of data and can be shared between multiple servers, but requires a separate database server.
