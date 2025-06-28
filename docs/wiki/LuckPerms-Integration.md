# LuckPerms Integration

NeoEssentials provides seamless integration with LuckPerms, a powerful permission management plugin/mod for Minecraft servers. This integration enables advanced permission-based features, prefix/suffix support, and group-based configuration.

## Prerequisites

- [LuckPerms for NeoForge/Forge](https://luckperms.net/download) installed on your server
- NeoEssentials properly installed

## Automatic Integration

When LuckPerms is detected on your server, NeoEssentials will automatically integrate with it. No additional configuration is required for basic functionality.

## Enabling/Configuring Integration

To configure the LuckPerms integration, edit `config/neoessentials/integrations.toml`:

```toml
[integrations]
enableLuckPerms = true
syncGroupsWithLuckPerms = true
useLuckPermsForChat = true
overrideLuckPermsCache = false
syncInterval = 20  # Ticks between syncs

[integrations.luckperms]
applyPrefixesToTabs = true
applySuffixesToTabs = true
useMetaForFeatures = true
useGroupWeights = true
syncPermissionsOnJoin = true
```

## Prefix/Suffix Support

NeoEssentials automatically uses LuckPerms prefixes and suffixes for:

- Chat messages
- Tablist names
- Name tags
- Commands output

### Chat Format with LuckPerms

Configure chat format in `config/neoessentials/chat.toml`:

```toml
[chat]
format = "{luckperms_prefix}{displayname}{luckperms_suffix}&f: {message}"
useGroupColors = true
```

## Group-Based Permissions

NeoEssentials uses LuckPerms groups to provide different feature access levels:

### Home Limits Example

```toml
[homes]
useLuckPermsForLimits = true

# Used when useLuckPermsForLimits = false or when player has no matching meta
defaultMaxHomes = 1

# These will be used when useLuckPermsForLimits = true
# LuckPerms meta key: "neoessentials.homes.limit"
```

### Warp Cooldown Example

```toml
[warps]
useLuckPermsForCooldowns = true

# Used when useLuckPermsForCooldowns = false or when player has no matching meta
defaultCooldown = 30

# These will be used when useLuckPermsForCooldowns = true
# LuckPerms meta key: "neoessentials.warps.cooldown"
```

## Setting Up LuckPerms for NeoEssentials

### Basic Permission Structure

Here's a recommended permission structure for use with NeoEssentials:

```
/lp creategroup admin
/lp creategroup mod
/lp creategroup vip
/lp creategroup default

# Set inheritance
/lp group admin parent add mod
/lp group mod parent add vip
/lp group vip parent add default

# Set permissions for default group
/lp group default permission set neoessentials.command.help true
/lp group default permission set neoessentials.command.spawn true
/lp group default permission set neoessentials.homes.1 true

# Set permissions for VIP group
/lp group vip permission set neoessentials.command.fly true
/lp group vip permission set neoessentials.homes.3 true
/lp group vip permission set neoessentials.warps.use.* true

# Set permissions for mod group
/lp group mod permission set neoessentials.command.kick true
/lp group mod permission set neoessentials.command.ban true
/lp group mod permission set neoessentials.teleport.others true

# Set permissions for admin group
/lp group admin permission set neoessentials.admin.* true
```

### Setting Meta Values

Use meta values to configure player-specific or group-specific limits:

```
# Set home limit for VIP group
/lp group vip meta set neoessentials.homes.limit 5

# Set home limit for a specific player
/lp user playername meta set neoessentials.homes.limit 10

# Set kit cooldown reduction for VIP group (percentage)
/lp group vip meta set neoessentials.kits.cooldown.reduction 50

# Set teleport cooldown for groups
/lp group default meta set neoessentials.teleport.cooldown 30
/lp group vip meta set neoessentials.teleport.cooldown 10
```

## Prefix/Suffix Configuration

Set up prefixes and suffixes in LuckPerms for use with NeoEssentials:

```
# Set prefix for groups
/lp group admin prefix set "&c[Admin] "
/lp group mod prefix set "&2[Mod] "
/lp group vip prefix set "&6[VIP] "
/lp group default prefix set "&7"

# Set suffix for groups
/lp group admin suffix set " &c✦"
/lp group vip suffix set " &6✪"

# Set individual player prefix
/lp user playername prefix set "&d[Custom] "
```

## Permission Contexts

NeoEssentials supports LuckPerms contexts for fine-grained permission control:

```
# Set permission only in a specific world
/lp group vip permission set neoessentials.command.fly true world=creative

# Set permission only in a specific server (for multi-server networks)
/lp group vip permission set neoessentials.kits.vip true server=survival
```

## Dynamic Features with LuckPerms

### Economy Interest Rates

Configure economy interest rates based on group:

```toml
[economy.interest]
useLuckPermsGroups = true

# Used when useLuckPermsGroups = false or no matching group
defaultRate = 0.01  # 1%

# LuckPerms meta key: "neoessentials.economy.interest.rate"
```

### Command Cooldowns

Set different command cooldowns per group:

```toml
[commands.cooldowns]
useLuckPermsGroups = true

# Define default cooldowns
home = 30
warp = 10
kit = 300

# LuckPerms meta key: "neoessentials.command.cooldown.[command]"
```

## Placeholders

The LuckPerms integration adds several placeholders for use in messages and templates:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{luckperms_prefix}` | Player's LuckPerms prefix | [VIP] |
| `{luckperms_suffix}` | Player's LuckPerms suffix | [Builder] |
| `{luckperms_primary_group}` | Player's primary group | VIP |
| `{luckperms_groups}` | All player's groups | VIP, Builder, Donor |
| `{luckperms_meta:key}` | Get a meta value | 5 |

## Advanced LuckPerms Commands for NeoEssentials

### Group-Based Limits

You can use the following meta keys with LuckPerms to customize NeoEssentials features:

| Meta Key | Description | Example |
|----------|-------------|---------|
| `neoessentials.homes.limit` | Maximum number of homes | `/lp group vip meta set neoessentials.homes.limit 5` |
| `neoessentials.warps.cooldown` | Cooldown between warps (seconds) | `/lp group vip meta set neoessentials.warps.cooldown 10` |
| `neoessentials.kits.cooldown.reduction` | Reduction percentage for kit cooldowns | `/lp group vip meta set neoessentials.kits.cooldown.reduction 50` |
| `neoessentials.teleport.cooldown` | Teleport cooldown (seconds) | `/lp group vip meta set neoessentials.teleport.cooldown 5` |
| `neoessentials.economy.interest.rate` | Interest rate for economy | `/lp group vip meta set neoessentials.economy.interest.rate 0.02` |

### Weight-Based Features

Use LuckPerms group weights for feature prioritization:

```
# Set group weights
/lp group admin setweight 100
/lp group mod setweight 50
/lp group vip setweight 10
/lp group default setweight 0
```

## Tablist Integration

LuckPerms integration enhances tablist functionality:

```toml
[tablist]
useLuckPermsForSorting = true
usePrefixesInTab = true
useSuffixesInTab = true
groupColorInTab = true

[tablist.groups]
sortByWeight = true
showGroupHeaders = true
```

### Group Headers in Tablist

When `showGroupHeaders` is enabled, players are grouped in the tablist with headers:

```toml
[tablist.groupHeaders]
admin = "&c&l=== Admins ==="
mod = "&2&l=== Moderators ==="
vip = "&6&l=== VIP Players ==="
default = "&7&l=== Players ==="
```

## Troubleshooting LuckPerms Integration

### Common Issues

1. **Permissions Not Applied**
   - Ensure LuckPerms is properly installed
   - Check that the permission strings are correct
   - Verify player is in the correct group

2. **Prefixes/Suffixes Not Showing**
   - Check that `useLuckPermsForChat` is enabled
   - Verify prefixes are set in LuckPerms
   - Ensure the chat format includes `{luckperms_prefix}` and `{luckperms_suffix}`

3. **Meta Values Not Working**
   - Confirm meta keys are set correctly
   - Check that the integration options are enabled
   - Verify meta inheritance is working as expected

### Debug Commands

```
/neoessentials:debug luckperms
/lp user <username> info
/lp user <username> parent info
/lp user <username> meta info
```

## Performance Considerations

- Set an appropriate `syncInterval` value (20-100 ticks)
- Enable `overrideLuckPermsCache` only when necessary
- Consider disabling `syncPermissionsOnJoin` on larger servers

## Example LuckPerms Setup Script

Here's a complete example script for setting up LuckPerms with NeoEssentials:

```
# Create basic groups
/lp creategroup admin
/lp creategroup mod
/lp creategroup vip
/lp creategroup default

# Set group weights
/lp group admin setweight 100
/lp group mod setweight 50
/lp group vip setweight 10
/lp group default setweight 0

# Set inheritance
/lp group admin parent add mod
/lp group mod parent add vip
/lp group vip parent add default

# Set prefixes
/lp group admin prefix set "&c[Admin] "
/lp group mod prefix set "&2[Mod] "
/lp group vip prefix set "&6[VIP] "
/lp group default prefix set "&7"

# Set permissions
/lp group default permission set neoessentials.command.help true
/lp group default permission set neoessentials.command.spawn true
/lp group default permission set neoessentials.homes.use true

/lp group vip permission set neoessentials.command.fly true
/lp group vip permission set neoessentials.kit.vip true
/lp group vip permission set neoessentials.warps.use.* true

/lp group mod permission set neoessentials.command.kick true
/lp group mod permission set neoessentials.command.ban true
/lp group mod permission set neoessentials.command.mute true
/lp group mod permission set neoessentials.teleport.others true

/lp group admin permission set neoessentials.admin.* true

# Set meta values
/lp group default meta set neoessentials.homes.limit 1
/lp group vip meta set neoessentials.homes.limit 5
/lp group mod meta set neoessentials.homes.limit 10
/lp group admin meta set neoessentials.homes.limit 20

/lp group default meta set neoessentials.warps.cooldown 30
/lp group vip meta set neoessentials.warps.cooldown 10
/lp group mod meta set neoessentials.warps.cooldown 5
/lp group admin meta set neoessentials.warps.cooldown 0

/lp group vip meta set neoessentials.kits.cooldown.reduction 50
/lp group mod meta set neoessentials.kits.cooldown.reduction 75
/lp group admin meta set neoessentials.kits.cooldown.reduction 100
```

## Additional Resources

- [LuckPerms Documentation](https://luckperms.net/docs)
- [LuckPerms Bytebin Viewer](https://luckperms.net/verbose)
- [NeoEssentials Permission Guide](Permission-System)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for support
