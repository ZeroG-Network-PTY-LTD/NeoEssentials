# Mod Compatibility

NeoEssentials is designed to work well with other mods in your NeoForge environment. This guide provides information about known compatibilities, integrations, and solutions for common mod conflicts.

## Compatible Mod Categories

NeoEssentials is compatible with most NeoForge mods including:

- Optimization mods
- User interface mods
- World generation mods
- Content mods
- Tech mods
- Magic mods
- Economy and shop mods

## Officially Supported Integrations

### Permission Mods

| Mod | Compatibility | Notes |
|-----|--------------|-------|
| [LuckPerms](LuckPerms-Integration) | Full Support | Complete integration for permissions, prefixes, and group-based features |
| ForgePerms | Compatible | Basic permission checking support |
| PlayerPerms | Compatible | Basic permission checking support |

### Economy Mods

| Mod | Compatibility | Notes |
|-----|--------------|-------|
| EconomyForge | Integrated | Two-way balance synchronization |
| MarketAPI | Integrated | Shop integration |
| TradeSystem | Compatible | Works alongside NeoEssentials economy |

### Utility Mods

| Mod | Compatibility | Notes |
|-----|--------------|-------|
| [Placeholder API](Placeholder-API-Support) | Full Support | Complete integration for placeholders |
| Essentials Core | Compatible | Use only non-overlapping features |
| Waystone | Compatible | Complements NeoEssentials warp system |
| FTB Teams | Compatible | Group management compatibility |

### Chat/UI Mods

| Mod | Compatibility | Notes |
|-----|--------------|-------|
| BetterChat | Integrated | Enhanced chat formatting options |
| TabOverlay | Compatible | Works alongside NeoEssentials tablist |
| MinimapAPI | Compatible | Home and warp waypoint support |

## Configuration for Compatibility

### General Mod Compatibility Settings

In `config/neoessentials/config.toml`:

```toml
[compatibility]
disableOverlappingFeatures = true
detectConflicts = true
notifyOnConflicts = true
alwaysCheckBeforeExecuting = true

[compatibility.modules]
# Disable NeoEssentials modules that overlap with other mods
economy = true
homes = true
warps = true
kit = true
tablist = true
```

### Specific Mod Compatibility Settings

In `config/neoessentials/compatibility.toml`:

```toml
[mods]
# Specific mod compatibility settings

[mods.waystones]
registerWaystonesAsWarps = true
allowTeleportToWaystones = true
shareWaystoneData = true

[mods.ftbteams]
syncTeamsWithRanks = true
teamBasedPermissions = false
teamBasedEconomy = false

[mods.minimapapi]
registerHomesAsWaypoints = true
registerWarpsAsWaypoints = true
waypointUpdateInterval = 300  # Seconds
```

## Known Compatibility Issues and Solutions

### Chat Formatting Conflicts

**Issue**: Multiple mods trying to format chat messages.

**Solution**: Disable chat formatting in one of the mods:

```toml
[chat]
enabled = false  # Disable NeoEssentials chat formatting if using another chat mod
```

### Teleportation Command Conflicts

**Issue**: Multiple mods providing similar teleport commands.

**Solution**: Configure command aliases to avoid conflicts:

```toml
[commands.aliases]
teleport = ["ne-tp", "netp"]  # Use different aliases for NeoEssentials teleport
home = ["ne-home"]  # Use different alias for NeoEssentials home
```

### Economy System Conflicts

**Issue**: Multiple economy systems causing balance inconsistencies.

**Solution**: Either use NeoEssentials as the primary economy or integrate with another economy mod:

```toml
[economy]
enabled = true  # Keep NeoEssentials economy enabled
syncWithExternalEconomy = true  # Sync with external economy mod
primaryEconomyMod = "economyforge"  # Specify primary economy mod
```

### Permission System Conflicts

**Issue**: Multiple permission systems causing confusion.

**Solution**: Use one primary permission system and configure NeoEssentials to defer to it:

```toml
[permissions]
enabled = false  # Disable NeoEssentials permissions if using LuckPerms
checkExternalPermissions = true  # Check permissions from other mods
```

## Compatibility Testing

Before deploying in production, test compatibility:

1. Install NeoEssentials and other mods in a test environment
2. Check logs for compatibility warnings
3. Test overlapping features for conflicts
4. Adjust configurations as needed
5. Monitor server stability after adding mods

## Version-Specific Compatibility

### NeoForge 1.21.1 Compatibility

NeoEssentials is fully compatible with NeoForge 1.21.1. Known compatible mods include:

- LuckPerms (version 5.4.x+)
- Placeholder API (version 2.1.x+)
- JourneyMap (version 7.x+)
- Waystones (version 14.x+)
- FTB Teams (version 2001.x+)

### Forge 1.20.x Compatibility

For Forge 1.20.x, use NeoEssentials version 1.0.0 with these compatible mods:

- LuckPerms (version 5.3.x)
- Placeholder API (version 2.0.x)
- JourneyMap (version 6.x)
- Waystones (version 13.x)
- FTB Teams (version 2001.x)

## Optimizing Multi-Mod Environments

For servers with many mods, optimize compatibility:

1. **Load Order**: Ensure NeoEssentials loads after permission mods
2. **Disable Overlapping Features**: Turn off duplicate functionality
3. **Memory Allocation**: Increase server memory for mod-heavy environments
4. **Regular Updates**: Keep all mods updated to latest compatible versions
5. **Performance Monitoring**: Monitor TPS and memory usage

## Third-Party Plugins and Addons

These addons enhance NeoEssentials compatibility with other mods:

| Addon | Purpose | Link |
|-------|---------|------|
| NeoEssentials Bridge | Bridges NeoEssentials with other economy mods | [Download](https://github.com/ZeroG-Network/NeoEssentialsAddons) |
| Team Integration | Integrates NeoEssentials with team mods | [Download](https://github.com/ZeroG-Network/TeamIntegration) |
| ChatBridge | Enhanced compatibility with chat mods | [Download](https://github.com/ZeroG-Network/ChatBridge) |

## Common Mod Combinations

These mod combinations are known to work well with NeoEssentials:

### Administrative Server Setup

- NeoEssentials
- LuckPerms
- Placeholder API
- Dynmap
- CoreProtect
- ServerTools

### RPG Server Setup

- NeoEssentials
- LuckPerms
- Placeholder API
- MMOCore
- ItemsAdder
- Waystones
- QuestAdder

### Economy-Focused Server

- NeoEssentials
- LuckPerms
- Placeholder API
- ShopGUI+
- AuctionHouse
- Jobs Reborn
- TokenEnchant

## Testing Your Mod Configuration

Use the compatibility test commands to check for issues:

```
/neoessentials:compatibility test
/neoessentials:compatibility detect
/neoessentials:compatibility report
```

## Troubleshooting Common Compatibility Issues

### Log Analysis

Check your server logs for compatibility warnings:

```
[NeoEssentials] [WARN] Detected potential conflict with mod XYZ in feature ABC
```

### Feature Isolation

If you encounter issues with specific features, try isolating them:

```toml
[modules]
economy = false  # Disable just the economy module if having issues
```

### Resolving Command Conflicts

If command conflicts occur:

1. Check which mods are registering the same commands
2. Use command aliases in NeoEssentials configuration
3. Disable conflicting commands in one of the mods

### Resolving Listener Conflicts

If event handling conflicts occur:

1. Adjust event priorities in configuration
2. Disable specific listeners in NeoEssentials:

```toml
[events]
disablePlayerJoinListener = false
disablePlayerQuitListener = false
disablePlayerChatListener = true  # Disable if conflicting with chat mods
```

## Reporting Compatibility Issues

If you discover compatibility issues:

1. Collect server logs showing the issue
2. Note all mods and versions involved
3. Create a detailed report on our [GitHub Issues](https://github.com/ZeroG-Network/NeoEssentials/issues)
4. Join our [Discord](https://discord.gg/dUGAQF2Mga) for assistance

## Additional Resources

- [NeoEssentials API Documentation](API-Documentation) for developers wanting to integrate with NeoEssentials
- [Creating Extensions](Creating-Extensions) guide for creating addon mods
- [Database Integration](Database-Integration) for shared database setups
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for compatibility support
- [NeoForge Forums](https://forums.neoforged.net/) for general mod compatibility questions
