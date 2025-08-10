# Permission System Fixes and Implementation Guide

## Overview
Fixed the NeoEssentials permission system that was not functioning properly. The main issues were:
1. **Commands were not checking permissions** - Most commands had no permission requirements
2. **Missing permission validation** - Commands executed without checking if users had proper permissions
3. **No debug tools** - No way to test or debug permission issues

## Fixed Issues

### 1. Command Permission Integration
**Problem**: Commands like `/home`, `/warp`, `/spawn` were accessible to everyone regardless of permissions.

**Solution**: Added proper permission checks using `PermissionUtil.hasPermission()` to all command registrations and implementations.

#### Fixed Commands:
- **HomeCommands** (`/home`, `/sethome`, `/delhome`, `/homes`)
  - Added `PermissionNodes.HOME`, `PermissionNodes.HOME_SET`, `PermissionNodes.HOME_DELETE`, `PermissionNodes.HOME_LIST`
  - Both registration-level and execution-level permission checks (defense in depth)

- **WarpCommands** (`/warp`, `/setwarp`, `/delwarp`, `/warps`) 
  - Added `PermissionNodes.WARP`, `PermissionNodes.WARP_SET`, `PermissionNodes.WARP_DELETE`, `PermissionNodes.WARP_LIST`
  - Admin-only permissions for creating/deleting warps

- **SpawnCommands** (`/spawn`, `/setspawn`)
  - Added `PermissionNodes.SPAWN`, `PermissionNodes.SPAWN_SET`
  - Regular users can use spawn, only admins can set spawn

### 2. Permission System Architecture
**Components**:
- **CustomPermissionsManager**: Core permission system with group inheritance, wildcards, temporary permissions
- **PermissionUtil**: Utility class for permission checking with fallback mechanisms
- **PermissionGroup**: Group-based permissions with inheritance support
- **PermissionNodes**: Centralized permission node constants

### 3. Default Permission Groups
Created comprehensive default groups with proper permission assignments:

#### **Default Group** (Priority: 0)
- Basic permissions for all players
- Home system, spawn, basic warps, economy balance/pay, messaging, etc.

#### **VIP Group** (Priority: 10, inherits Default)
- Enhanced permissions: fly, heal, feed, workbench access, multiple homes, colors

#### **Moderator Group** (Priority: 50, inherits VIP)  
- Moderation permissions: kick, mute, jail, vanish, socialspy, teleport others

#### **Admin Group** (Priority: 100, inherits Moderator)
- Full administrative permissions including wildcards `essentials.*` and `neoessentials.*`

### 4. Debug and Testing Tools
**Added PermissionDebugCommand** (`/permdebug`) for administrators:

#### Available Commands:
```bash
/permdebug test <permission>                    # Test permission for yourself
/permdebug testplayer <player> <permission>     # Test permission for another player  
/permdebug showperms [player]                   # Show all permissions for a player
/permdebug groupinfo [player]                   # Show group information and test common permissions
/permdebug setgroup <player> <group>            # Set a player's group
```

## Permission Node Structure

### Basic User Permissions
```
neoessentials.home                    # Use /home command
neoessentials.sethome                 # Use /sethome command  
neoessentials.delhome                 # Use /delhome command
neoessentials.homes                   # Use /homes command
neoessentials.warp                    # Use /warp command
neoessentials.warps                   # List warps
neoessentials.spawn                   # Use /spawn command
neoessentials.back                    # Use /back command
neoessentials.msg                     # Send private messages
neoessentials.reply                   # Reply to messages
neoessentials.balance                 # Check balance
neoessentials.pay                     # Send money
```

### Administrative Permissions  
```
neoessentials.warp.set                # Create warps
neoessentials.warp.delete             # Delete warps
neoessentials.spawn.set               # Set spawn location
neoessentials.ban                     # Ban players
neoessentials.kick                    # Kick players
neoessentials.mute                    # Mute players
neoessentials.give                    # Give items
essentials.*                       # All essentials permissions
neoessentials.*                    # All NeoEssentials permissions
```

## Implementation Details

### Permission Checking Pattern
All commands now follow this pattern:

1. **Registration Level Check**:
```java
dispatcher.register(Commands.literal("home")
    .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.HOME))
    .executes(context -> teleportHome(context, "home"))
);
```

2. **Execution Level Check** (Defense in Depth):
```java
private static int teleportHome(CommandContext<CommandSourceStack> context, String homeName) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    
    if (!PermissionUtil.hasPermission(player, PermissionNodes.HOME)) {
        MessageUtil.sendMessage(player, "&cYou don't have permission to use homes!");
        return 0;
    }
    
    // Execute command logic...
}
```

### Permission System Features
- **Hierarchical Groups**: Groups inherit from parent groups
- **Wildcard Support**: Use `essentials.*` for all essentials permissions  
- **Negative Permissions**: Use `-permission.node` to deny specific permissions
- **Temporary Permissions**: Grant permissions for a specific duration
- **Permission Caching**: High-performance caching with 30-second duration
- **Fallback System**: Falls back to vanilla OP system if permission manager fails

## Testing and Validation

### Testing Steps:
1. **Start server** with the mod loaded
2. **Check default permissions** using `/permdebug groupinfo`
3. **Test basic commands** like `/home` and `/spawn` 
4. **Set player to different groups** using `/permdebug setgroup <player> <group>`
5. **Verify permission inheritance** by checking VIP inherits Default permissions
6. **Test permission negation** by adding negative permissions

### Common Permission Issues:
1. **"You don't have permission"**: Check if player is in correct group and group has required permission
2. **Commands not working**: Use `/permdebug test <permission>` to verify permission checking
3. **Group inheritance not working**: Verify parent group exists and has correct permissions
4. **Cache issues**: Permission cache auto-expires after 30 seconds, or use `/permissions reload`

## Configuration Integration

### Setting Player Groups:
```bash
# Using NeoEssentials commands
/permissions user <player> group set <group>
/permdebug setgroup <player> <group>

# Available groups: default, vip, moderator, admin
```

### Group Management:
```bash
# List all groups
/permissions group list

# View group info  
/permissions group info <group>

# Add permission to group
/permissions group permission add <group> <permission>

# Set group inheritance
/permissions group inheritance <group> <parent>
```

## Security Considerations

### Best Practices:
1. **Principle of Least Privilege**: Give minimal necessary permissions
2. **Use Groups Over Individual Permissions**: Easier to manage and audit
3. **Regular Permission Audits**: Review permissions periodically
4. **Test Permission Changes**: Use debug commands to verify changes
5. **Monitor Permission Usage**: Check logs for permission-related issues

### Default Security:
- All commands require explicit permissions (no open access)
- Admin permissions require OP level or explicit admin group membership
- Permission system has fallback to vanilla OP system for safety
- Comprehensive logging of permission changes and failures

## Results

### Before Fix:
- Commands accessible to everyone regardless of permissions
- No permission validation or checking
- Permission system existed but wasn't integrated with commands
- No tools to debug or test permission issues

### After Fix:
- All commands properly check permissions before execution
- Two-layer permission checking (registration + execution)
- Comprehensive debug tools for testing and troubleshooting
- Default permission groups with proper inheritance
- Working integration between permission system and all commands

The permission system now functions correctly and provides proper access control for all NeoEssentials features. Users must have appropriate permissions to use commands, and administrators have full control over permission assignment through groups and individual permissions.
