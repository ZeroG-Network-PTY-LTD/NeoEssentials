# Permission System

The NeoEssentials permission system provides integration with popular permission mods and a comprehensive framework for managing access to features and commands.

## Overview

NeoEssentials does not provide its own permission storage system but instead integrates with existing permission mods:

- [LuckPerms](https://luckperms.net/) (recommended)
- [FTB Ranks](https://www.feed-the-beast.com/mods/ftb-ranks)
- Other permission mods that implement the Forge/NeoForge permission API

If no permission mod is detected, NeoEssentials will fall back to the vanilla operator system.

## Permission Structure

All NeoEssentials permissions follow this format:

```
neoessentials.<category>.<feature>[.<sub-feature>]
```

Categories include:
- `command` - Access to commands
- `feature` - Access to features
- `limit` - Numeric limits for features
- `bypass` - Bypass restrictions
- `admin` - Administrative capabilities

## Permission Categories

### Command Permissions

Command permissions control access to NeoEssentials commands:

```
neoessentials.command.<commandname>
```

Examples:
- `neoessentials.command.home`
- `neoessentials.command.warp`
- `neoessentials.command.kit`

For commands with subcommands:

```
neoessentials.command.<commandname>.<subcommand>
```

Examples:
- `neoessentials.command.eco.give`
- `neoessentials.command.warp.set`

### Feature Permissions

Feature permissions control access to NeoEssentials features:

```
neoessentials.feature.<featurename>
```

Examples:
- `neoessentials.feature.teleport`
- `neoessentials.feature.economy`

### Limit Permissions

Limit permissions set numeric limits for certain features:

```
neoessentials.limit.<feature>.<number>
```

Examples:
- `neoessentials.limit.homes.3` - Allow 3 homes
- `neoessentials.limit.warps.5` - Allow creating 5 warps
- `neoessentials.limit.kits.10` - Allow creating 10 kits

Special limit permissions:
- `neoessentials.limit.homes.unlimited` - Allow unlimited homes
- `neoessentials.limit.warps.unlimited` - Allow unlimited warps

### Bypass Permissions

Bypass permissions allow players to bypass certain restrictions:

```
neoessentials.bypass.<restriction>
```

Examples:
- `neoessentials.bypass.cooldown.home` - Bypass home cooldowns
- `neoessentials.bypass.cooldown.warp` - Bypass warp cooldowns
- `neoessentials.bypass.cooldown.kit` - Bypass kit cooldowns
- `neoessentials.bypass.cost` - Bypass economy costs

### Admin Permissions

Admin permissions provide access to administrative features:

```
neoessentials.admin.<feature>
```

Examples:
- `neoessentials.admin.reload` - Reload configuration
- `neoessentials.admin.update` - Check for updates
- `neoessentials.admin.config` - Modify configuration

## Wildcard Permissions

Wildcard permissions grant access to multiple permissions:

```
neoessentials.command.*
```

Grants access to all commands.

```
neoessentials.admin.*
```

Grants access to all administrative features.

## Integration with Permission Systems

### LuckPerms Integration

NeoEssentials integrates seamlessly with LuckPerms:

1. Install [LuckPerms for NeoForge](https://luckperms.net/download)
2. Set up permission groups:

```
/lp creategroup default
/lp creategroup vip
/lp creategroup admin
```

3. Add permissions to groups:

```
/lp group default permission set neoessentials.command.home true
/lp group default permission set neoessentials.limit.homes.1 true

/lp group vip permission set neoessentials.command.fly true
/lp group vip permission set neoessentials.limit.homes.3 true

/lp group admin permission set neoessentials.admin.* true
/lp group admin permission set neoessentials.command.* true
```

4. Assign players to groups:

```
/lp user <player> parent set <group>
```

### FTB Ranks Integration

NeoEssentials also works with FTB Ranks:

1. Install [FTB Ranks](https://www.feed-the-beast.com/mods/ftb-ranks)
2. Use the FTB Ranks commands to set up permissions:

```
/ftbranks add_rank default
/ftbranks add_rank vip
/ftbranks add_rank admin

/ftbranks edit_rank default add neoessentials.command.home
/ftbranks edit_rank default add neoessentials.limit.homes.1

/ftbranks edit_rank vip add neoessentials.command.fly
/ftbranks edit_rank vip add neoessentials.limit.homes.3

/ftbranks edit_rank admin add neoessentials.admin.*
/ftbranks edit_rank admin add neoessentials.command.*
```

## Permission Templates

NeoEssentials includes predefined permission templates for common server setups:

- [Basic Server Template](https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/basic.yml)
- [Economy Server Template](https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/economy.yml)
- [RPG Server Template](https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/rpg.yml)

To import a template into LuckPerms:

```
/lp import https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/basic.yml
```

## Tablist Integration

NeoEssentials can display player ranks in the tablist:

```toml
# In config/neoessentials/tablist.toml
[tablist.display]
showPlayerRanks = true
rankFormat = "&8[%rank%&8] "
```

This requires integration with a permission system to get player ranks.

## Permission-Based Features

### Group-Specific Templates

Create group-specific tablist templates in templates.json:

```json
{
  "groups": {
    "admin": {
      "headers": ["Admin header"],
      "footers": ["Admin footer"]
    },
    "vip": {
      "headers": ["VIP header"],
      "footers": ["VIP footer"]
    }
  }
}
```

Group names must match exactly with your permission system's group names.

### Permission-Based Kit Access

Control kit access with permissions:

```
neoessentials.kit.<kitname>
```

Players need this permission to use the specified kit.

### Permission-Based Warp Access

Control warp access with permissions:

```
neoessentials.warp.<warpname>
```

Players need this permission to use the specified warp.

## Checking Permissions

Server operators can check player permissions:

```
/neoessentials checkperm <player> <permission>
```

This shows whether the player has the specified permission.

## Troubleshooting Permissions

If permissions aren't working correctly:

1. Verify the permission mod is installed and working
2. Check for typos in permission nodes
3. Ensure group names match exactly in templates.json
4. Verify the player is assigned to the correct group
5. Check if a higher-priority permission is overriding the permission

## Need Help?

For more information about permissions:

- Check the [Commands Reference](Commands-Reference) for required permissions
- See the [LuckPerms Integration](LuckPerms-Integration) guide for detailed setup
- Visit our [Discord server](https://discord.gg/dUGAQF2Mga) for support
