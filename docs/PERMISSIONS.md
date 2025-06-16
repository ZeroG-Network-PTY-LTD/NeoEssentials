# NeoEssentials Permissions System

NeoEssentials provides a flexible permissions system that integrates with LuckPerms and FTB Ranks while also providing fallback default permissions.

## Supported Permission Providers

NeoEssentials supports the following permission providers in order of precedence:

1. **LuckPerms** - If LuckPerms is installed, it will be used as the primary permission provider.
2. **FTB Ranks** - If FTB Ranks is installed and LuckPerms is not, it will be used as the primary permission provider.
3. **Default Permissions** - If neither LuckPerms nor FTB Ranks is installed, the default permissions defined in `neoessentials-general.toml` will be used.

## Permission Node Structure

Permission nodes in NeoEssentials follow a consistent naming pattern:

```
neoessentials.<category>.<feature>[.<subfeature>]
```

For example:
- `neoessentials.command.tpa` - Permission to use the /tpa command
- `neoessentials.command.heal.others` - Permission to heal other players
- `neoessentials.warp.admin` - Permission to use the admin warp

## Default Permissions Configuration

Default permissions are defined in the `neoessentials-general.toml` config file. These permissions are used when no permission provider is available or as fallbacks. The configuration format is:

```toml
[defaultPermissions]
# Command permissions
"neoessentials.command.tpa" = true
"neoessentials.command.tpahere" = true
"neoessentials.command.back" = true
"neoessentials.command.spawn" = true
"neoessentials.command.home" = true
"neoessentials.command.sethome" = true
"neoessentials.command.delhome" = true
"neoessentials.command.warp" = true
"neoessentials.command.heal" = false
"neoessentials.command.heal.others" = false
"neoessentials.command.feed" = true
"neoessentials.command.feed.others" = false
# Add more permissions as needed
```

## PermissionUtil Class

The `PermissionUtil` class is the central component of the permissions system. It provides methods to check if a player has a permission and integrates with external permission providers.

### Usage Example

```java
// Check if a player has a permission
boolean hasPermission = PermissionUtil.hasPermission(player, "neoessentials.command.tpa");

// Check if a player has a permission with a default fallback value
boolean hasPermission = PermissionUtil.hasPermission(player, "neoessentials.command.tpa", true);
```

## Integration with Command System

The command system in NeoEssentials automatically checks permissions for all commands using the `PermissionUtil` class. Each command can define its required permission, and the command will only be available to players who have that permission.

## Permission Levels

Some commands can have different permission levels for different features. For example:

- Basic: `neoessentials.command.home` - Access to basic home functionality
- Advanced: `neoessentials.command.home.multiple` - Permission to have multiple homes
- Admin: `neoessentials.command.home.others` - Permission to manage other players' homes

## LuckPerms Integration

When LuckPerms is installed, NeoEssentials automatically integrates with it. Permission checks are forwarded to LuckPerms, which gives you full control over permissions using LuckPerms' powerful group and permission management system.

### Example LuckPerms Setup

```
/lp group default permission set neoessentials.command.tpa true
/lp group default permission set neoessentials.command.home true
/lp group vip permission set neoessentials.command.heal true
/lp group admin permission set neoessentials.command.heal.others true
```

## FTB Ranks Integration

When FTB Ranks is installed and LuckPerms is not, NeoEssentials integrates with FTB Ranks. Permission checks are forwarded to FTB Ranks, which allows you to manage permissions using FTB Ranks' rank system.

## Additional Notes

- All permissions default to `false` unless specified otherwise in the config or granted by a permission provider.
- Op-level players have all permissions by default.
- When checking permissions, NeoEssentials tries each permission provider in order (LuckPerms, FTB Ranks, default permissions) and uses the first available result.

## Common Permissions

| Permission                                | Description                            | Default |
|-------------------------------------------|----------------------------------------|---------|
| `neoessentials.command.tpa`               | Use the /tpa command                   | true    |
| `neoessentials.command.tpahere`           | Use the /tpahere command               | true    |
| `neoessentials.command.tpaccept`          | Use the /tpaccept command              | true    |
| `neoessentials.command.tpdeny`            | Use the /tpdeny command                | true    |
| `neoessentials.command.back`              | Use the /back command                  | true    |
| `neoessentials.command.spawn`             | Use the /spawn command                 | true    |
| `neoessentials.command.setspawn`          | Use the /setspawn command              | false   |
| `neoessentials.command.home`              | Use the /home command                  | true    |
| `neoessentials.command.sethome`           | Use the /sethome command               | true    |
| `neoessentials.command.delhome`           | Use the /delhome command               | true    |
| `neoessentials.command.warp`              | Use the /warp command                  | true    |
| `neoessentials.command.setwarp`           | Use the /setwarp command               | false   |
| `neoessentials.command.delwarp`           | Use the /delwarp command               | false   |
| `neoessentials.command.heal`              | Use the /heal command                  | false   |
| `neoessentials.command.heal.others`       | Heal other players                     | false   |
| `neoessentials.command.feed`              | Use the /feed command                  | true    |
| `neoessentials.command.feed.others`       | Feed other players                     | false   |
| `neoessentials.command.fly`               | Use the /fly command                   | false   |
| `neoessentials.command.fly.others`        | Toggle flight for other players        | false   |
| `neoessentials.command.gamemode.creative` | Switch to creative mode                | false   |
| `neoessentials.command.gamemode.survival` | Switch to survival mode                | false   |
| `neoessentials.command.money`             | Use the /money command                 | true    |
| `neoessentials.command.pay`               | Use the /pay command                   | true    |
| `neoessentials.command.balance`           | Use the /balance command               | true    |
| `neoessentials.command.baltop`            | Use the /baltop command                | true    |
| `neoessentials.command.eco`               | Use the /eco command (admin)           | false   |
| `neoessentials.command.kit`               | Use the /kit command                   | true    |
| `neoessentials.command.createkit`         | Use the /createkit command             | false   |
