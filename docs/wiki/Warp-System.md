# Warp System

The NeoEssentials warp system enables server administrators to create public teleportation points that players can easily access.

## Overview

The warp system provides:

- Public teleportation points
- Category-based organization
- Permission-controlled access
- Economy integration (optional)
- Teleport cooldowns and warmups

## Configuration

Configure the warp system in `config/neoessentials/warps.toml`:

```toml
[warps]
# Enable the warp system
enabled = true

# Warp teleport cooldown (in seconds, 0 to disable)
cooldown = 10

# Warmup time before teleport (in seconds, 0 to disable)
warmup = 3

# Cancel warmup if player moves
cancelWarmupOnMove = true

# Economy integration
[warps.economy]
# Enable economy costs for warps
enableCosts = false

# Default cost to use a warp (can be overridden per warp)
defaultWarpCost = 25.0

# Storage settings
[warps.storage]
# Storage type: "json", "sqlite", or "mysql"
type = "json"

# Storage location (for JSON)
location = "data/neoessentials/warps"
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/warp <name>` | Teleports to a warp | neoessentials.command.warp |
| `/warps` | Lists all available warps | neoessentials.command.warps |
| `/warps <category>` | Lists warps in a category | neoessentials.command.warps |
| `/setwarp <name>` | Creates a warp | neoessentials.command.setwarp |
| `/delwarp <name>` | Deletes a warp | neoessentials.command.delwarp |
| `/warp info <name>` | Shows warp information | neoessentials.command.warp.info |
| `/warp cost <name> <cost>` | Sets warp cost | neoessentials.command.warp.cost |
| `/warp category <name> <category>` | Sets warp category | neoessentials.command.warp.category |
| `/warp icon <name> <icon>` | Sets warp icon | neoessentials.command.warp.icon |

## Permissions

### Basic Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.command.warp` | Allows using /warp command |
| `neoessentials.command.warps` | Allows using /warps command |
| `neoessentials.command.setwarp` | Allows using /setwarp command |
| `neoessentials.command.delwarp` | Allows using /delwarp command |

### Specific Warp Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.warp.<warpname>` | Allows using a specific warp |
| `neoessentials.warp.category.<category>` | Allows using warps in a category |

### Bypass Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.bypass.cooldown.warp` | Bypasses warp cooldown |
| `neoessentials.bypass.warmup.warp` | Bypasses warp warmup |
| `neoessentials.bypass.cost.warp` | Bypasses warp costs |

## Creating Warps

To create a warp:

1. Go to the location where you want to create the warp
2. Run the command: `/setwarp <name>`
3. Configure additional properties (optional):
   - Set category: `/warp category <name> <category>`
   - Set cost: `/warp cost <name> <amount>`
   - Set icon: `/warp icon <name> <material>`
   - Set description: `/warp desc <name> <description>`

Example:
```
/setwarp spawn
/warp category spawn essentials
/warp desc spawn Main server spawn point
/warp icon spawn nether_star
```

## Warp Categories

Warps can be organized into categories for better management:

```
/warp category <name> <category>
```

Common categories include:
- `essentials` - Critical server locations
- `biomes` - Different biome locations
- `shops` - Shop locations
- `games` - Minigame locations
- `builds` - Interesting builds

To view warps in a specific category:
```
/warps <category>
```

## Economy Integration

When enabled, players will be charged for using warps:

```
/warp cost <name> <amount>
```

Sets the cost for using a specific warp. Players will be charged this amount when they teleport to the warp.

## Warp Signs

You can create warp signs for easy access to warps:

1. Place a sign
2. Write the following lines:
   ```
   [Warp]
   <warp name>
   ```

Players can right-click the sign to teleport to the warp.

## Warp GUI

NeoEssentials provides a graphical interface for warps:

```
/warps gui
```

This opens a menu showing all available warps, organized by category, with custom icons.

## Warp Events

Server events can be configured to trigger warps:

```toml
[warps.events]
# Warp to send players to on joining the server
joinWarp = "spawn"

# Warp to send players to on respawn
respawnWarp = "spawn"
```

## Warp Metadata

Warps can store additional metadata:

```
/warp meta <name> set <key> <value>
/warp meta <name> get <key>
/warp meta <name> remove <key>
```

This is useful for integration with other plugins or custom scripts.

## Integration with Home System

The warp system works alongside the [Home System](Home-System) for private teleportation points.

## Advanced Usage

### Warp Permissions

To restrict a warp to players with a specific permission:

```
/warp permission <name> <permission>
```

This requires players to have the specified permission to use the warp.

### Warp Notifications

Set notifications for when players use warps:

```
/warp notify <name> <message>
```

This message will be shown to players when they teleport to the warp.

## Need Help?

For more information about the warp system:

- Check the [Commands Reference](Commands-Reference) for detailed command usage
- See the [Permissions Guide](Permissions-Guide) for permission setup
- Visit our [Discord server](https://discord.gg/dUGAQF2Mga) for support
