# Permissions Guide

This guide explains how to set up and manage permissions with NeoEssentials.

## Permission System Integration

NeoEssentials integrates with popular permission systems:

- [LuckPerms](https://luckperms.net/) (recommended)
- [FTB Ranks](https://www.feed-the-beast.com/mods/ftb-ranks)
- Other permission mods that implement the Forge/NeoForge permission API

If no permission system is detected, NeoEssentials will default to Minecraft's built-in operator system.

## Permission Format

All NeoEssentials permissions use the following format:

```
neoessentials.<category>.<feature>[.<sub-feature>]
```

Examples:
```
neoessentials.command.home
neoessentials.command.warps
neoessentials.command.eco.give
```

## Core Permission Categories

| Category | Description | Example |
|----------|-------------|---------|
| `command` | Access to specific commands | `neoessentials.command.home` |
| `feature` | Access to specific features | `neoessentials.feature.teleport` |
| `bypass` | Bypasses certain restrictions | `neoessentials.bypass.cooldown` |
| `admin` | Administrative capabilities | `neoessentials.admin.reload` |
| `limit` | Custom limits/quotas | `neoessentials.limit.homes.5` |

## Essential Permissions

These are the most common permissions you'll want to set up:

### Basic Commands
```
neoessentials.command.home
neoessentials.command.spawn
neoessentials.command.warp
neoessentials.command.tpa
neoessentials.command.back
neoessentials.command.balance
```

### Admin Commands
```
neoessentials.command.sethome
neoessentials.command.setspawn
neoessentials.command.setwarp
neoessentials.command.kit.create
neoessentials.command.eco
neoessentials.command.vanish
```

### Feature Access
```
neoessentials.feature.teleport
neoessentials.feature.economy
neoessentials.feature.kits
neoessentials.feature.tablist.custom
```

## Permission Nodes for Multiple Homes

To allow players to have multiple homes:

```
# Allow 3 homes
neoessentials.limit.homes.3

# Allow unlimited homes
neoessentials.limit.homes.unlimited
```

## Permission Nodes for Teleport Cooldowns

To set cooldown bypasses:

```
# Bypass home cooldown
neoessentials.bypass.cooldown.home

# Bypass warp cooldown  
neoessentials.bypass.cooldown.warp

# Bypass all cooldowns
neoessentials.bypass.cooldown.*
```

## Permission Setup with LuckPerms

Example LuckPerms commands for setting up basic permissions:

### Set up a default group
```
/lp creategroup default
/lp group default setweight 1
/lp group default permission set neoessentials.command.home true
/lp group default permission set neoessentials.command.warp true
/lp group default permission set neoessentials.command.balance true
/lp group default permission set neoessentials.limit.homes.1 true
```

### Set up a VIP group
```
/lp creategroup vip
/lp group vip setweight 10
/lp group vip parent add default
/lp group vip permission set neoessentials.command.fly true
/lp group vip permission set neoessentials.limit.homes.5 true
/lp group vip permission set neoessentials.bypass.cooldown.home true
```

### Set up an Admin group
```
/lp creategroup admin
/lp group admin setweight 100
/lp group admin parent add vip
/lp group admin permission set neoessentials.admin.* true
/lp group admin permission set neoessentials.command.* true
/lp group admin permission set neoessentials.limit.homes.unlimited true
/lp group admin permission set neoessentials.bypass.cooldown.* true
```

## Permission Templates

NeoEssentials includes predefined permission templates you can import into your permission system:

- [Basic Server Template](https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/basic.yml)
- [Economy Server Template](https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/economy.yml)
- [RPG Server Template](https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/rpg.yml)

To import these templates into LuckPerms:

```
/lp import https://github.com/ZeroG-Network/NeoEssentials/wiki/permission-templates/basic.yml
```

## Tablist Integration

NeoEssentials can show player ranks in the tablist when integrated with permission systems:

```toml
# In config/neoessentials/tablist.toml
[tablist.display]
showPlayerRanks = true
rankFormat = "&8[%rank%&8] "
```

## Need Help?

If you need assistance with permissions:

- Visit our [Discord server](https://discord.gg/dUGAQF2Mga)
- Check the [LuckPerms Integration](LuckPerms-Integration) guide for detailed information
- See the [Troubleshooting Guide](Troubleshooting) for common permission issues
