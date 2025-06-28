# Kit System

The NeoEssentials kit system allows server administrators to create predefined item packages that players can claim at configurable intervals.

## Overview

The kit system provides:

- Customizable item kits
- Cooldown periods between claims
- Permission-based access
- First-join kits
- Economy integration (optional)
- One-time or repeatable kits

## Configuration

Configure the kit system in `config/neoessentials/kits.toml`:

```toml
[kits]
# Enable the kit system
enabled = true

# Show available kits when a player joins
showOnJoin = true

# First join kit (given automatically to new players, empty for none)
firstJoinKit = "starter"

# Economy integration
[kits.economy]
# Enable economy costs for kits
enableCosts = false

# Storage settings
[kits.storage]
# Storage type: "json", "sqlite", or "mysql"
type = "json"

# Storage location (for JSON)
location = "data/neoessentials/kits"
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/kit <name>` | Claims a kit | neoessentials.command.kit |
| `/kits` | Lists available kits | neoessentials.command.kits |
| `/createkit <name>` | Creates a kit from inventory | neoessentials.command.createkit |
| `/editkit <name>` | Edits an existing kit | neoessentials.command.editkit |
| `/deletekit <name>` | Deletes a kit | neoessentials.command.deletekit |
| `/kitinfo <name>` | Shows kit information | neoessentials.command.kitinfo |
| `/kitcooldown <name> <time>` | Sets kit cooldown | neoessentials.command.kitcooldown |
| `/kitcost <name> <amount>` | Sets kit cost | neoessentials.command.kitcost |
| `/kitgui` | Opens kit selection GUI | neoessentials.command.kitgui |

## Permissions

### Basic Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.command.kit` | Allows using /kit command |
| `neoessentials.command.kits` | Allows using /kits command |
| `neoessentials.command.createkit` | Allows creating kits |
| `neoessentials.command.editkit` | Allows editing kits |
| `neoessentials.command.deletekit` | Allows deleting kits |

### Specific Kit Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.kit.<kitname>` | Allows using a specific kit |

### Bypass Permissions
| Permission | Description |
|------------|-------------|
| `neoessentials.bypass.cooldown.kit` | Bypasses kit cooldowns |
| `neoessentials.bypass.cost.kit` | Bypasses kit costs |

## Creating Kits

To create a kit:

1. Fill your inventory with the items you want in the kit
2. Run the command: `/createkit <name>`
3. Configure additional properties (optional):
   - Set cooldown: `/kitcooldown <name> <time>`
   - Set cost: `/kitcost <name> <amount>`
   - Set description: `/kitdesc <name> <description>`
   - Set icon: `/kiticon <name> <material>`

Example:
```
/createkit tools
/kitcooldown tools 24h
/kitdesc tools Basic tools for new players
/kiticon tools iron_pickaxe
```

## Kit Cooldowns

Kit cooldowns determine how often a player can claim a kit:

```
/kitcooldown <name> <time>
```

Time formats:
- `30s` - 30 seconds
- `5m` - 5 minutes
- `2h` - 2 hours
- `1d` - 1 day
- `7d` - 7 days
- `once` - One-time use kit

## Economy Integration

When enabled, players will be charged for claiming kits:

```
/kitcost <name> <amount>
```

Players must have sufficient funds to claim the kit.

## Kit Templates

Kit templates allow for consistent kit creation across servers:

```
/kittemplates save <name>
/kittemplates load <name>
```

Templates are stored in `data/neoessentials/kittemplates/` as JSON files.

## Kit GUI

The kit GUI provides a visual interface for claiming kits:

```
/kitgui
```

This opens a menu showing all available kits with icons, descriptions, and cooldowns.

## Kit Categories

Kits can be organized into categories:

```
/kitcategory <name> <category>
```

Common categories include:
- `starter` - Basic starter kits
- `tools` - Tool kits
- `armor` - Armor kits
- `weapons` - Weapon kits
- `special` - Special or event kits

## Kit Contents

Kit contents are stored as serialized NBT data. A kit can include:

- Regular items
- Custom named items
- Enchanted items
- Items with custom NBT data
- Armor
- Tools
- Weapons
- Food
- Potions
- Books and written books

## Special Kit Types

### First Join Kit

The first join kit is automatically given to new players:

```toml
# In kits.toml
firstJoinKit = "starter"
```

### Rank Kits

Rank kits can be automatically given when a player gets a rank:

```
/kitrank <name> <rank>
```

This requires integration with a permission system.

### One-Time Kits

One-time kits can only be claimed once:

```
/kitcooldown <name> once
```

## Advanced Kit Features

### Command Execution

Kits can execute commands when claimed:

```
/kitcommand <name> add <command>
/kitcommand <name> remove <index>
/kitcommand <name> list
```

Commands can use placeholders like `%player%`.

### Kit Prerequisites

Kits can require other kits to be claimed first:

```
/kitprereq <name> <prerequisite>
```

Players must claim the prerequisite kit before they can claim this kit.

### Kit Effects

Kits can apply potion effects when claimed:

```
/kiteffect <name> add <effect> <duration> <amplifier>
/kiteffect <name> remove <index>
/kiteffect <name> list
```

## Need Help?

For more information about the kit system:

- Check the [Commands Reference](Commands-Reference) for detailed command usage
- See the [Permissions Guide](Permissions-Guide) for permission setup
- Visit our [Discord server](https://discord.gg/dUGAQF2Mga) for support
