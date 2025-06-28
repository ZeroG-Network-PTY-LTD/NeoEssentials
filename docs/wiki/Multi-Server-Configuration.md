# Multi-Server Configuration

This guide explains how to set up NeoEssentials across multiple servers in a network configuration, allowing for synchronized player data, shared economies, and consistent user experiences.

## Prerequisites

- Multiple NeoForge servers set up with NeoEssentials
- A proxy server (Velocity/Waterfall/BungeeCord)
- MySQL database for shared storage (recommended)
- Network infrastructure connecting all servers

## Network Architecture

### Typical Setup

```
                       │ Proxy Server │
                       └──────┬───────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
    ┌─────────▼────────┐ ┌────▼─────────┐ ┌───▼────────────┐
    │  Survival Server │ │  Hub Server  │ │ Creative Server│
    └──────────────────┘ └──────────────┘ └────────────────┘
```

## Setting up the Database

### MySQL Configuration

1. Create a MySQL database for NeoEssentials
2. Create a user with appropriate permissions
3. Configure each server to use the same database

Example SQL:

```sql
CREATE DATABASE neoessentials;
CREATE USER 'neouser'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON neoessentials.* TO 'neouser'@'%';
FLUSH PRIVILEGES;
```

## Server-Specific Configuration

### 1. Shared Storage Configuration

Edit `config/neoessentials/storage.toml` on each server:

```toml
[storage]
type = "mysql"
host = "your-database-host"
port = 3306
database = "neoessentials"
username = "neouser"
password = "password"
tablePrefix = "neo_"
useSSL = true
connectionPoolSize = 10
keepAlive = true
```

### 2. Server Identification

Each server needs a unique identifier in `config/neoessentials/config.toml`:

```toml
[server]
id = "survival"  # Unique per server (e.g., "hub", "survival", "creative")
network = "MyNetwork"  # Common across all servers
useNetworkSync = true
```

### 3. Synchronization Settings

Configure what should be synchronized between servers:

```toml
[sync]
economy = true
homes = true
warps = false  # Server-specific warps
kits = false  # Server-specific kits
permissions = true
playerData = true
syncInterval = 30  # Seconds between syncs
```

## Feature-Specific Configuration

### Economy System

For a shared economy:

```toml
[economy]
networkShared = true
syncTransactions = true
allowCrossServerTransactions = true
currencySymbol = "$"
currencyName = "Coins"
defaultBalance = 100.0
```

### Home System

For cross-server homes:

```toml
[homes]
allowCrossServerTeleport = true
maxHomesPerPlayer = 5
cooldownSeconds = 30
permissionBasedLimits = true
```

### Warp System

For server-specific warps:

```toml
[warps]
networkShared = false  # Each server has unique warps
allowCrossServerWarps = false
```

### Permission System

For synchronized permissions (when not using LuckPerms):

```toml
[permissions]
networkShared = true
syncInterval = 60
```

## Proxy Server Integration

### Velocity Configuration

Add to `velocity.toml`:

```toml
[plugins]
neoessentials-velocity = true

[forwarding]
mode = "modern"
secret = "your-secret-key"  # Must match on all servers
```

### Waterfall/BungeeCord Configuration

Add to `config.yml`:

```yaml
ip_forward: true
player_limit: 100
permissions:
  default:
  - neoessentials.command.server
  - bungeecord.command.server
```

Add to `plugins/NeoEssentialsBridge/config.yml`:

```yaml
settings:
  secret: "your-secret-key"  # Must match on all servers
  sync-interval: 30
  debug: false
```

## Command Routing

Configure which commands work on which servers:

### Global Commands (All Servers)

```toml
[commands]
global = [
  "msg",
  "reply",
  "balance",
  "pay",
  "help"
]
```

### Server-Specific Commands

```toml
[commands.serverSpecific]
hub = [
  "spawn",
  "rules",
  "vote"
]
survival = [
  "home",
  "sethome",
  "tpa",
  "rtp"
]
```

## Cross-Server Teleportation

### Setting Up Server Switching

In `config/neoessentials/commands.toml`:

```toml
[commands.server]
enabled = true
permission = "neoessentials.command.server"
aliases = ["server", "switchserver", "join"]
cooldown = 3
```

### Teleportation Management

In `config/neoessentials/teleport.toml`:

```toml
[teleport]
crossServer = true
timeout = 30
safetyChecks = true
maintainPosition = true  # When possible, teleport to same coordinates
defaultServer = "hub"  # Fallback server
```

## Data Synchronization

### Sync Timing Configuration

```toml
[sync.timing]
playerJoin = true
playerQuit = true
intervalMinutes = 5  # Regular sync interval
forceSyncCommands = true  # Force sync after important commands
```

### Backup Configuration

```toml
[sync.backup]
enabled = true
backupBeforeSync = true
maxBackups = 10
```

## Testing Your Network

1. Start all servers and the proxy
2. Connect to the proxy address
3. Test cross-server commands
4. Verify data synchronization:
   - Check if economy balances are synchronized
   - Test if homes can be accessed from different servers
   - Verify permissions are consistent

## Advanced Configuration

### Load Balancing

For larger networks with multiple instances of the same game mode:

```toml
[loadbalancing]
enabled = true
maxPlayersPerServer = 50
preferredServer = "survival1"
balancingStrategy = "least-players"  # Options: least-players, round-robin
```

### Network Messaging

For custom cross-server messages:

```toml
[messaging]
enabled = true
channels = ["global", "staff", "events", "broadcasts"]
allowCustomChannels = true
filterMessages = true
```

## Troubleshooting

### Common Issues and Solutions

- **Data Not Syncing**: Check database connectivity and permissions
- **Cross-Server Commands Failing**: Verify proxy forwarding setup
- **Inconsistent Player Data**: Increase sync frequency
- **Database Connection Errors**: Check firewall settings and credentials

### Diagnostic Commands

- `/neoessentials:network status` - Check network connection status
- `/neoessentials:network sync` - Force synchronization
- `/neoessentials:network debug` - Enable detailed debug logging
- `/neoessentials:network servers` - List connected servers

## Security Considerations

1. Use strong database passwords
2. Enable SSL for database connections
3. Use a secure forwarding secret
4. Restrict database access to specific IP addresses
5. Regularly backup synchronized data

## Performance Optimization

For large networks:

1. Use connection pooling
2. Implement caching
3. Stagger sync timings
4. Consider dedicated database server
5. Monitor network latency between servers

## Additional Resources

- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for network setup support
- [GitHub Repository](https://github.com/ZeroG-Network/NeoEssentials) for latest updates
- See our [Database Integration](Database-Integration) guide for advanced database setups
