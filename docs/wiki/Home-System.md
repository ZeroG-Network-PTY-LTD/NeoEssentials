# Home System

The NeoEssentials home system allows players to set personal teleportation locations on your server.

## Overview

The home system provides:

- Multiple homes per player (permission-based)
- Custom home names
- Teleport cooldowns
- Cost-based teleportation (optional)
- Group-specific home limits

## Configuration

Configure the home system in `config/neoessentials/homes.toml`:

```toml
[homes]
# Enable the home system
enabled = true

# Default maximum homes per player (if no permission)
defaultMaxHomes = 1

# Home teleport cooldown (in seconds, 0 to disable)
cooldown = 5

# Warmup time before teleport (in seconds, 0 to disable)
warmup = 3

# Cancel warmup if player moves
cancelWarmupOnMove = true

# Economy integration
[homes.economy]
# Enable economy costs for homes
enableCosts = false

# Cost to set a home
setHomeCost = 100.0

# Cost to teleport to a home
teleportCost = 10.0

# Storage settings
[homes.storage]
# Storage type: "json", "sqlite", or "mysql"
type = "json"

# Storage location (for JSON)
location = "data/neoessentials/homes"
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/home` | Teleports to your default home | neoessentials.command.home |
| `/home <name>` | Teleports to a specific home | neoessentials.command.home |
| `/homes` | Lists all your homes | neoessentials.command.homes |
| `/sethome` | Sets your default home | neoessentials.command.sethome |
| `/sethome <name>` | Sets a named home | neoessentials.command.sethome |
| `/delhome <name>` | Deletes a home | neoessentials.command.delhome |
| `/home limit` | Shows your home limit | neoessentials.command.home.limit |

## Permissions

### Basic Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.command.home` | Allows using /home command |
| `neoessentials.command.homes` | Allows using /homes command |
| `neoessentials.command.sethome` | Allows using /sethome command |
| `neoessentials.command.delhome` | Allows using /delhome command |

### Home Limit Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.limit.homes.1` | Allows 1 home |
| `neoessentials.limit.homes.3` | Allows 3 homes |
| `neoessentials.limit.homes.5` | Allows 5 homes |
| `neoessentials.limit.homes.10` | Allows 10 homes |
| `neoessentials.limit.homes.unlimited` | Allows unlimited homes |

### Bypass Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.bypass.cooldown.home` | Bypasses home cooldown |
| `neoessentials.bypass.warmup.home` | Bypasses home warmup |
| `neoessentials.bypass.cost.home` | Bypasses home costs |

## Setup Examples

### Basic Home Setup

1. Enable the home system in configuration:
   ```toml
   [homes]
   enabled = true
   defaultMaxHomes = 1
   ```

2. Set up permissions for different player groups:
   ```
   # Default group (1 home)
   /lp group default permission set neoessentials.command.home true
   /lp group default permission set neoessentials.command.sethome true
   /lp group default permission set neoessentials.limit.homes.1 true
   
   # VIP group (5 homes)
   /lp group vip permission set neoessentials.limit.homes.5 true
   
   # Admin group (unlimited homes, no cooldowns)
   /lp group admin permission set neoessentials.limit.homes.unlimited true
   /lp group admin permission set neoessentials.bypass.cooldown.home true
   ```

### Economy Integration

To enable home costs:

1. Update the configuration:
   ```toml
   [homes.economy]
   enableCosts = true
   setHomeCost = 100.0
   teleportCost = 10.0
   ```

2. Ensure the economy system is enabled:
   ```toml
   # In economy.toml
   [economy]
   enabled = true
   ```

## Home Management

### Setting Homes

Players can set homes using:
```
/sethome
/sethome <name>
```

The first command sets the default home, while the second creates a named home.

### Teleporting to Homes

To teleport to a home:
```
/home
/home <name>
```

The first command teleports to the default home, while the second teleports to a named home.

### Listing Homes

Players can view their homes:
```
/homes
```

This shows a list of all their homes with coordinates.

### Deleting Homes

To delete a home:
```
/delhome <name>
```

## Advanced Features

### Teleport Confirmation

For safety, teleporting to homes with potential hazards (like lava or void) will require confirmation:

```
/home dangerous_home
[Warning] This location may be dangerous. Type /confirm to teleport anyway.
```

### Home Sharing

Players can share homes with others:

```
/home share <name> <player>
```

This gives another player access to one of your homes.

### Public Homes

Create homes that anyone can visit:

```
/home public <name>
/home private <name>
```

Public homes appear in the server's public home list:

```
/homes public
```

## Integration with Warps

The home system works alongside the [Warp System](Warp-System) for public teleportation points.

## Need Help?

For more information about the home system:

- Check the [Commands Reference](Commands-Reference) for detailed command usage
- See the [Permissions Guide](Permissions-Guide) for permission setup
- Visit our [Discord server](https://discord.gg/dUGAQF2Mga) for support
