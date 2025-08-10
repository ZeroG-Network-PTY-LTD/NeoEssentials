<<<<<<< HEAD
# Permissions

NeoEssentials includes a comprehensive permission system that provides fine-grained control over player access to commands and features. The system supports group-based permissions with inheritance, individual player permissions, temporary permissions, and wildcard support.

## 📋 Permission System Overview

### Features
- **Group-based Permissions** - Organize players into permission groups
- **Permission Inheritance** - Groups can inherit permissions from parent groups
- **Individual Player Permissions** - Override group permissions for specific players
- **Temporary Permissions** - Grant permissions for a limited time
- **Wildcard Support** - Use `*` for broader permission grants
- **Permission Caching** - Optimized performance through permission caching
- **Prefix/Suffix Support** - Visual indicators for player groups

### Default Groups
NeoEssentials comes with four default permission groups:

- **default** - Basic player permissions
- **vip** - Enhanced player permissions with some bypass abilities
- **moderator** - Moderation permissions with user management capabilities
- **admin** - Full administrative access to all features

## 🔧 Permission Commands

### Group Management
Manage permission groups and their settings:

```bash
# List all groups
/permissions group list

# View group information
/permissions group info <group>

# Create a new group
/permissions group create <name> <prefix>

# Delete a group
/permissions group delete <group>

# Add permission to group
/permissions group permission add <group> <permission>

# Remove permission from group
/permissions group permission remove <group> <permission>

# Set group inheritance
/permissions group inheritance <group> <parent>

# Set group prefix/suffix
/permissions group prefix <group> <prefix>
/permissions group suffix <group> <suffix>
```

### User Management
Manage individual player permissions:

```bash
# View player permission info
/permissions user <player> info

# Set player's group
/permissions user <player> group set <group>

# Add permission to player
/permissions user <player> permission add <permission>

# Remove permission from player
/permissions user <player> permission remove <permission>

# Add temporary permission (in seconds)
/permissions user <player> permission temp <permission> <duration>

# Clear player's custom permissions
/permissions user <player> clear
```

### System Commands
System-level permission management:

```bash
# Check player's permission
/permissions check <player> <permission>

# View permission statistics
/permissions stats

# List common permission nodes
/permissions nodes

# Reload permission system
/permissions reload
```

## 🎯 Permission Nodes

### Essential Commands
Core utility commands for players:

```
neoessentials.heal              # Heal self
neoessentials.heal.others       # Heal other players
neoessentials.feed              # Feed self
neoessentials.feed.others       # Feed other players
neoessentials.god               # God mode for self
neoessentials.god.others        # God mode for others
neoessentials.fly               # Flight for self
neoessentials.fly.others        # Flight for others
neoessentials.vanish            # Vanish mode
neoessentials.workbench         # Portable workbench
neoessentials.anvil             # Portable anvil
neoessentials.enderchest        # Portable ender chest
```

### Teleportation System
Teleportation and location-based commands:

```
neoessentials.home              # Use homes
neoessentials.sethome           # Set homes
neoessentials.delhome           # Delete homes
neoessentials.homes             # List homes
neoessentials.spawn             # Use spawn
neoessentials.spawn.set         # Set spawn point
neoessentials.warp              # Use warps
neoessentials.warp.set          # Create warps
neoessentials.warp.delete       # Delete warps
neoessentials.tp                # Basic teleportation
neoessentials.tp.others         # Teleport others
neoessentials.tpa               # TPA requests
neoessentials.back              # Return to previous location
```

### Moderation Commands
Player management and moderation tools:

```
neoessentials.kick              # Kick players
neoessentials.ban               # Ban players
neoessentials.tempban           # Temporary bans
neoessentials.mute              # Mute players
neoessentials.jail              # Jail players
neoessentials.vanish.see        # See vanished players
```

### Economy System
Economic features and shop management:

```
neoessentials.balance           # View balance
neoessentials.pay               # Pay other players
neoessentials.eco.give          # Give money to players
neoessentials.eco.take          # Take money from players
neoessentials.shop.use          # Use shops
neoessentials.shop.create       # Create shops
neoessentials.shop.admin        # Shop administration
```

### Administration
Server administration and configuration:

```
neoessentials.config.reload     # Reload configurations
neoessentials.permissions.*     # Permission management
neoessentials.performance.admin # Performance monitoring
neoessentials.security.admin    # Security administration
```

### Wildcard Permissions
Broad permission grants:

```
neoessentials.*                 # All NeoEssentials permissions
neoessentials.teleport.*        # All teleportation permissions
neoessentials.moderation.*      # All moderation permissions
neoessentials.economy.*         # All economy permissions
*                              # All permissions (use with caution)
```

## 🔐 Permission Groups

### Default Group Configuration

#### Default Group
Basic permissions for all players:
```
neoessentials.home
neoessentials.sethome
neoessentials.spawn
neoessentials.balance
neoessentials.pay
neoessentials.msg
neoessentials.reply
```

#### VIP Group
Enhanced permissions with bypass abilities:
```
Inherits: default
Additional permissions:
neoessentials.fly
neoessentials.heal
neoessentials.feed
neoessentials.bypass.cooldown.teleport
=======
# Permissions System

NeoEssentials includes a comprehensive permission system with group-based management, inheritance, wildcards, and temporary permissions. This system can work standalone or integrate with external permission plugins.

## 🛒 Shop System (NEW FEATURE)

NeoEssentials now includes a comprehensive shop system with advanced permissions and security features.

### Shop System Overview

The shop system allows players to create sign-based shops for trading items. It includes:
- **Player Shops**: Regular shops with limited stock based on chest contents
- **Admin Shops**: Infinite stock shops for server economy management
- **Visual Indicators**: Dynamic color coding and clear admin shop identification
- **Security Protection**: Comprehensive permission-based access control

### Shop Commands

#### `/signshop create <item> <buyPrice> <sellPrice> <quantity> [admin]`
Create a new shop sign.

**Examples**:
```bash
# Create regular shop
/signshop create minecraft:diamond 10.0 8.0 32

# Create admin shop (infinite stock)
/signshop create minecraft:diamond 10.0 8.0 32 true
```

**Required Permissions**:
- `neoessentials.shop.create` - For regular shops
- `neoessentials.shop.create.admin` - For admin shops

#### `/signshop refresh`
Refresh all shop signs on the server.

**Required Permissions**:
- `neoessentials.shop.refresh`

#### `/signshop list`
List all shops on the server.

**Required Permissions**:
- `neoessentials.shop.list`

### Shop Features

#### Admin Shop Identification
- Admin shops display `[Admin Shop]` on the first line
- Different color coding to distinguish from player shops
- Infinite stock - never runs out of items

#### Dynamic Visual Feedback
- **Green**: Shop has good stock levels
- **Yellow**: Shop has low stock
- **Red**: Shop is out of stock
- **Blue**: Admin shop (infinite stock)

#### Security Protection
- **Block Breaking**: Only shop owners or admins can break shop signs and chests
- **Chest Access**: Only shop owners or players with `neoessentials.shop.access.others` can access shop chests
- **Modification**: Only shop owners or admins can modify shop settings

### Shop Permission Nodes

#### Basic Shop Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `neoessentials.shop.create` | Create player shops | `true` |
| `neoessentials.shop.use` | Buy from/sell to shops | `true` |
| `neoessentials.shop.break` | Break own shops | `true` |
| `neoessentials.shop.modify` | Modify own shop settings | `true` |

#### Administrative Shop Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `neoessentials.shop.create.admin` | Create admin shops | `op` |
| `neoessentials.shop.break.others` | Break other players' shops | `op` |
| `neoessentials.shop.modify.others` | Modify other players' shops | `op` |
| `neoessentials.shop.access.others` | Access other players' shop chests | `op` |
| `neoessentials.shop.admin` | Full shop administration rights | `op` |
| `neoessentials.shop.bypass` | Bypass all shop restrictions | `op` |

#### Utility Shop Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `neoessentials.shop.refresh` | Refresh shop signs globally | `op` |
| `neoessentials.shop.list` | List all shops on server | `op` |

### Shop Usage

#### Creating a Shop
1. **Place a chest** where you want the shop storage
2. **Place a sign** adjacent to the chest
3. **Run the command**: `/signshop create <item> <buyPrice> <sellPrice> <quantity>`
4. **Stock the chest** with items (not needed for admin shops)

#### Using a Shop
1. **Right-click the sign** to see shop information
2. **Left-click** to buy items (if available)
3. **Right-click with items** to sell to the shop (if shop buys)

#### Managing Your Shop
- **Access the chest** to add/remove stock
- **Break the sign** to remove the shop
- **Use refresh command** to update sign display

### Security Features

#### Protection System
The shop system includes comprehensive protection:

```java
// Block breaking protection
@EventHandler(priority = EventPriority.HIGH)
public void onBlockBreak(BlockBreakEvent event) {
    // Checks shop ownership and permissions
    if (!canBreakShop(player, blockPos)) {
        event.setCanceled(true);
        // Send permission denied message
    }
}

// Chest access protection  
@EventHandler(priority = EventPriority.HIGH)
public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    // Checks chest access permissions
    if (!canAccessShop(player, chestPos)) {
        event.setCanceled(true);
        // Send access denied message
    }
}
```

#### Permission Hierarchy
The system respects permission hierarchy:
1. **Shop Owner**: Full access to their own shops
2. **Admin Permission**: `neoessentials.shop.admin` grants full access
3. **Specific Permissions**: Granular control with specific permission nodes
4. **Bypass Permission**: `neoessentials.shop.bypass` overrides all restrictions

### Integration Examples

#### With Economy Systems
```bash
# Set up shop admin group
/permissions group ShopAdmin permission add neoessentials.shop.admin
/permissions group ShopAdmin permission add neoessentials.shop.create.admin

# Give shop owner permissions
/permissions user Steve permission add neoessentials.shop.create
/permissions user Steve permission add neoessentials.shop.use
```

#### With Protection Plugins
```toml
[shop.integration]
# Respect other protection plugins
respectWorldGuard = true
respectTowny = true
respectGriefPrevention = true

# Shop creation limits
maxShopsPerPlayer = 10
requireClaimPermission = true
```

### Troubleshooting Shop Issues

#### Common Problems

**"Permission denied" when creating shop**
- Check `neoessentials.shop.create` permission
- For admin shops, verify `neoessentials.shop.create.admin` permission

**"Cannot access shop chest"**
- Verify you own the shop or have `neoessentials.shop.access.others` permission
- Check if the chest is properly linked to the shop sign

**"Cannot break shop sign"**
- Ensure you own the shop or have `neoessentials.shop.break.others` permission
- Admin shops require special permissions to break

**Shop sign not updating**
- Use `/signshop refresh` command (requires `neoessentials.shop.refresh`)
- Check if the chest is accessible and contains items

#### Debug Commands
```bash
# Check shop information
/signshop info

# List player's shops
/signshop list player <playername>

# Force refresh specific shop
/signshop refresh <x> <y> <z>
```

## 🎯 Overview

The permission system provides:
- **Group-based permissions** with inheritance
- **Wildcard permission support** (`essentials.*`)
- **Permission negation** (`-permission.node`)
- **Temporary permissions** with automatic expiration
- **High-performance caching** for fast permission checks
- **Integration with external systems** (LuckPerms, etc.)

## 👥 Permission Groups

### Default Groups

NeoEssentials creates four default permission groups:

#### Default Group
**Description**: Basic permissions for all players
**Priority**: 0
**Permissions**:
```
neoessentials.home
neoessentials.sethome
neoessentials.delhome
neoessentials.homes
neoessentials.warp
neoessentials.warps
neoessentials.spawn
neoessentials.back
neoessentials.placeholder.test
neoessentials.shop.create
neoessentials.shop.use
neoessentials.shop.break
neoessentials.shop.modify
```

#### VIP Group
**Description**: Enhanced permissions for VIP players
**Priority**: 10
**Inherits**: Default
**Additional Permissions**:
```
neoessentials.fly
neoessentials.heal
neoessentials.feed
neoessentials.workbench
neoessentials.anvil
neoessentials.enderchest
neoessentials.repair
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)
neoessentials.bossbar.show
```

#### Moderator Group
<<<<<<< HEAD
Moderation capabilities:
```
Inherits: vip
Additional permissions:
neoessentials.kick
neoessentials.mute
neoessentials.jail
neoessentials.vanish
neoessentials.vanish.see
neoessentials.permissions.check
```

#### Admin Group
Full administrative access:
```
Inherits: moderator
Additional permissions:
neoessentials.*
neoessentials.config.*
neoessentials.permissions.*
neoessentials.performance.admin
```

## ⚙️ Configuration

### Permission Integration
The permission system integrates with:
- **Minecraft's built-in OP system** - OPs automatically have all permissions
- **Fallback permissions** - Basic permissions granted when permission system fails
- **External permission plugins** - Compatibility with other permission systems

### Performance Optimization
- **Permission Caching** - Reduces database lookups
- **Lazy Loading** - Permissions loaded only when needed
- **Batch Operations** - Efficient bulk permission changes

### Storage
- Permissions are stored persistently and survive server restarts
- Player group assignments are maintained across sessions
- Temporary permissions automatically expire

## 📝 Usage Examples

### Setting Up Groups
```bash
# Create a builder group
/permissions group create builder "&e[Builder] "

# Give building permissions
/permissions group permission add builder neoessentials.fly
/permissions group permission add builder neoessentials.god
/permissions group permission add builder neoessentials.give

# Set inheritance from VIP
/permissions group inheritance builder vip
```

### Managing Players
```bash
# Promote a player to moderator
/permissions user Steve group set moderator

# Give temporary admin access for 1 hour
/permissions user Steve permission temp neoessentials.* 3600

# Check what permissions a player has
/permissions user Steve info
/permissions check Steve neoessentials.fly
```

### Debugging Permissions
```bash
# Check if player has specific permission
/permissions check Alex neoessentials.tp

# View detailed permission breakdown
/permissions user Alex info

# Check group permissions
/permissions group info moderator
```

## 🛡️ Security Best Practices

### Permission Hierarchy
- Use inheritance to maintain clean permission structures
- Avoid granting wildcard permissions (`*`) unnecessarily
- Regularly audit group permissions

### Temporary Permissions
- Use temporary permissions for short-term access
- Monitor temporary permission usage through logs
- Set reasonable duration limits

### Regular Maintenance
- Review and update group permissions regularly
- Remove unused groups and permissions
- Monitor permission usage statistics

---

*For detailed permission node documentation, use `/permissions nodes` in-game or consult the [API documentation](API.md).*
=======
**Description**: Moderation permissions for staff
**Priority**: 50
**Inherits**: VIP
**Additional Permissions**:
```
neoessentials.kick
neoessentials.mute
neoessentials.unmute
neoessentials.jail
neoessentials.unjail
neoessentials.vanish
neoessentials.god
neoessentials.invsee
neoessentials.bossbar.broadcast
neoessentials.security.view
```

#### Admin Group
**Description**: Full administrative permissions
**Priority**: 100
**Inherits**: Moderator
**Additional Permissions**:
```
neoessentials.*
neoessentials.shop.admin
neoessentials.shop.create.admin
neoessentials.shop.access.others
neoessentials.shop.break.others
neoessentials.shop.modify.others
```

## 🎮 Commands

### User Information

#### `/permissions info [player]`
Display permission information for a player.

**Examples**:
```bash
# View your own permissions
/permissions info

# View another player's permissions
/permissions info Steve
```

**Output**:
```
=== Permission Info for Steve ===
Group: VIP (Priority: 10)
Permissions: 25 total
- neoessentials.fly ✓
- neoessentials.heal ✓
- neoessentials.kick ✗
Temporary Permissions: 2 active
```

#### `/permissions check <player> <permission>`
Test if a player has a specific permission.

**Examples**:
```bash
# Check if player has permission
/permissions check Steve neoessentials.fly

# Check negative permission
/permissions check Alex neoessentials.kick
```

### Group Management

#### `/permissions group list`
List all available permission groups.

**Example Output**:
```
=== Permission Groups ===
1. Default (Priority: 0) - 12 permissions
2. VIP (Priority: 10) - 8 additional permissions
3. Moderator (Priority: 50) - 15 additional permissions
4. Admin (Priority: 100) - All permissions
```

#### `/permissions group info <group>`
Show detailed information about a permission group.

**Example**:
```bash
/permissions group info VIP
```

**Output**:
```
=== Group: VIP ===
Priority: 10
Inherits: Default
Prefix: "&6[VIP] "
Suffix: ""
Permissions: 8 direct, 20 total
Direct Permissions:
- neoessentials.fly
- neoessentials.heal
- neoessentials.feed
[...]
```

#### `/permissions group create <name> <prefix> [priority]`
Create a new permission group.

**Examples**:
```bash
# Create basic group
/permissions group create Builder "&e[Builder] " 25

# Create group with priority
/permissions group create Helper "&b[Helper] " 30
```

#### `/permissions group delete <group>`
Delete a permission group.

**Example**:
```bash
/permissions group delete Builder
```

**Note**: Cannot delete groups that have members or are inherited by other groups.

### Group Permissions

#### `/permissions group permission add <group> <permission>`
Add a permission to a group.

**Examples**:
```bash
# Add specific permission
/permissions group permission add VIP essentials.speed

# Add wildcard permission
/permissions group permission add Admin essentials.*

# Add negative permission (deny)
/permissions group permission add Default -essentials.give
```

#### `/permissions group permission remove <group> <permission>`
Remove a permission from a group.

**Example**:
```bash
/permissions group permission remove VIP essentials.speed
```

### Group Inheritance

#### `/permissions group inheritance <group> <parent>`
Set inheritance for a permission group.

**Examples**:
```bash
# Set VIP to inherit from Default
/permissions group inheritance VIP Default

# Remove inheritance (set to none)
/permissions group inheritance Builder none
```

### User Management

#### `/permissions user <player> info`
Show detailed permission information for a user.

**Example**:
```bash
/permissions user Steve info
```

#### `/permissions user <player> group set <group>`
Set a player's primary permission group.

**Examples**:
```bash
# Promote player to VIP
/permissions user Steve group set VIP

# Demote player to Default
/permissions user Alex group set Default
```

#### `/permissions user <player> permission add <permission>`
Add a direct permission to a player.

**Examples**:
```bash
# Give specific permission
/permissions user Steve permission add essentials.speed

# Give negative permission
/permissions user Griefer permission add -essentials.build
```

#### `/permissions user <player> permission remove <permission>`
Remove a direct permission from a player.

**Example**:
```bash
/permissions user Steve permission remove essentials.speed
```

#### `/permissions user <player> permission temp <permission> <duration>`
Give a player temporary permission.

**Examples**:
```bash
# 1 hour temporary fly permission
/permissions user Steve permission temp essentials.fly 1h

# 30 minutes temporary god mode
/permissions user Alex permission temp essentials.god 30m

# 7 days temporary VIP access
/permissions user NewPlayer permission temp essentials.fly 7d
```

**Duration Formats**:
- `s` - seconds
- `m` - minutes
- `h` - hours
- `d` - days
- `w` - weeks

#### `/permissions user <player> clear`
Remove all custom permissions from a player (keeps group permissions).

**Example**:
```bash
/permissions user Steve clear
```

### System Commands

#### `/permissions reload`
Reload the permission system configuration.

**Example**:
```bash
/permissions reload
```

#### `/permissions stats`
Show permission system statistics.

**Example Output**:
```
=== Permission Statistics ===
Total Users: 157
Total Groups: 4
Cached Permissions: 892
Temporary Permissions: 12
Cache Hit Rate: 94.2%
Last Cleanup: 2 minutes ago
```

## 🔧 Permission Syntax

### Basic Permissions
```
neoessentials.heal          # Allow /heal command
neoessentials.fly           # Allow /fly command
neoessentials.bossbar.*     # All bossbar permissions
```

### Wildcard Permissions
```
neoessentials.*             # All NeoEssentials permissions
*.admin                     # All admin permissions across plugins
*                           # ALL permissions (dangerous!)
```

### Negative Permissions
```
-neoessentials.give         # Deny /give command
-neoessentials.gamemode.*   # Deny all gamemode commands
-neoessentials.admin        # Deny admin access
```

### Permission Hierarchy
When checking permissions, the system follows this order:
1. **Direct user permissions** (positive and negative)
2. **Group permissions** (by priority, highest first)
3. **Inherited group permissions**
4. **Default permissions**

## 📋 Permission Nodes

### Essential Commands

#### Health & Wellness
```
essentials.heal                    # /heal command - heal yourself
essentials.heal.others             # Heal other players
essentials.heal.*                  # All heal permissions

essentials.feed                    # /feed command - feed yourself
essentials.feed.others             # Feed other players
essentials.feed.*                  # All feed permissions

essentials.god                     # /god command - toggle god mode for yourself
essentials.god.others              # Toggle god mode for others
essentials.god.*                   # All god mode permissions
```

#### Movement & Visibility
```
essentials.fly                     # /fly command - toggle flight for yourself
essentials.fly.others              # Toggle flight for others
essentials.fly.*                   # All fly permissions

essentials.speed.walk              # /speed walk command
essentials.speed.fly               # /speed fly command
essentials.speed.others            # Change speed for others
essentials.speed.*                 # All speed permissions

essentials.vanish                  # /vanish command - toggle vanish for yourself
essentials.vanish.others           # Toggle vanish for others
essentials.vanish.see              # See vanished players
essentials.vanish.*                # All vanish permissions
```

#### Item & Environment Management
```
neoessentials.repair                  # /repair command - repair item in hand
neoessentials.repair.all              # Repair all items
neoessentials.repair.others           # Repair items for others

neoessentials.give                    # /give command - give items
neoessentials.give.unlimited          # Give items without limits
neoessentials.give.*                  # All give permissions

neoessentials.time.set                # /time set command
neoessentials.time.add                # /time add command
neoessentials.time.query              # /time query command
neoessentials.time.*                  # All time permissions

neoessentials.weather.set             # /weather command base
neoessentials.weather.clear           # Set clear weather
neoessentials.weather.rain            # Set rain
neoessentials.weather.thunder         # Set thunderstorm
neoessentials.weather.*               # All weather permissions

neoessentials.workbench               # /workbench command - open crafting table
neoessentials.anvil                   # /anvil command - open anvil
essentials.enderchest              # /enderchest command - open your enderchest
essentials.enderchest.others       # View others' enderchests
```

### Teleportation System

#### Basic Teleportation
```
essentials.tp                      # /tp command - teleport to players
essentials.tp.others               # Teleport other players
essentials.tp.coords               # Teleport to coordinates
essentials.tphere                  # /tphere command - teleport players to you
essentials.tp.*                    # All teleport permissions
```

#### Home System
```
essentials.home                    # /home command - teleport to homes
essentials.sethome                 # /sethome command - set home locations
essentials.delhome                 # /delhome command - delete homes
essentials.homes                   # /homes command - list homes
essentials.home.others             # Access others' homes
essentials.home.multiple           # Set multiple homes
essentials.home.*                  # All home permissions
```

#### Warp System
```
essentials.warp                    # /warp command - teleport to warps
essentials.setwarp                 # /setwarp command - create warps (admin)
essentials.delwarp                 # /delwarp command - delete warps (admin)
essentials.warps                   # /warps command - list warps
essentials.warp.*                  # All warp permissions
```

#### TPA (Teleport Request) System
```
essentials.tpa                     # /tpa command - send teleport requests
essentials.tpahere                 # /tpahere command - request player to teleport to you
essentials.tpaccept                # /tpaccept command - accept teleport requests
essentials.tpdeny                  # /tpdeny command - deny teleport requests
essentials.tpcancel                # /tpcancel command - cancel pending requests
essentials.tpa.*                   # All TPA permissions
```

#### Spawn System
```
essentials.spawn                   # /spawn command - teleport to spawn
essentials.setspawn                # /setspawn command - set spawn location (admin)
essentials.spawn.others            # Teleport others to spawn
essentials.spawn.*                 # All spawn permissions
```

#### Back System
```
essentials.back                    # /back command - return to previous location
essentials.back.ondeath           # Use /back after death
essentials.back.onteleport        # Use /back after teleportation
```

### Moderation Commands

#### Ban System
```
essentials.ban                     # /ban command - ban players
essentials.tempban                 # /tempban command - temporary bans
essentials.banip                   # /banip command - IP bans
essentials.unban                   # /unban command - unban players
essentials.ban.exempt             # Exempt from being banned
essentials.ban.*                   # All ban permissions
```

#### Kick & Mute System
```
essentials.kick                    # /kick command - kick players
essentials.kick.exempt             # Exempt from being kicked

essentials.mute                    # /mute command - mute players
essentials.unmute                  # /unmute command - unmute players
essentials.mute.exempt             # Exempt from being muted
essentials.mute.*                  # All mute permissions
```

#### Jail System
```
essentials.jail                    # /jail command - jail players
essentials.unjail                  # /unjail command - unjail players
essentials.setjail                 # /setjail command - create jail locations
essentials.deljail                 # /deljail command - delete jails
essentials.jail.exempt             # Exempt from being jailed
essentials.jail.*                  # All jail permissions
```

### Economy System

#### Basic Economy
```
essentials.balance                 # /balance command - view your balance
essentials.balance.others          # View others' balances
essentials.pay                     # /pay command - send money to players
essentials.balancetop              # /balancetop command - view richest players
```

#### Economy Administration
```
essentials.eco.give                # /eco give command - give money
essentials.eco.take                # /eco take command - take money
essentials.eco.set                 # /eco set command - set balance
essentials.eco.reset               # /eco reset command - reset balances
essentials.eco.*                   # All economy admin permissions
```

#### Advanced Economy Features
```
neoessentials.economy.analytics    # View economy analytics
neoessentials.economy.transactions # View transaction history
neoessentials.economy.history      # View detailed economic history
```

### Messaging System

#### Private Messages
```
neoessentials.msg                     # /msg command - send private messages
neoessentials.reply                   # /reply command - reply to messages
neoessentials.msgtoggle               # /msgtoggle command - toggle message reception
neoessentials.socialspy               # /socialspy command - spy on private messages
```

#### Mail System
```
neoessentials.mail.send               # /mail send command - send mail
neoessentials.mail.read               # /mail read command - read mail
neoessentials.mail.clear              # /mail clear command - clear mailbox
neoessentials.mail.*                  # All mail permissions
```

#### Broadcasting
```
neoessentials.broadcast               # /broadcast command - server-wide messages
neoessentials.broadcast.world         # /broadcast command - world-specific messages
```

### Player Information Commands

#### Player Lists & Information
```
neoessentials.list                    # /list command - view online players
neoessentials.list.hidden             # See hidden/vanished players in list

neoessentials.whois                   # /whois command - detailed player info
neoessentials.seen                    # /seen command - when player was last online
neoessentials.realname                # /realname command - find player by nickname
```

#### Nickname System
```
neoessentials.nick                    # /nick command - set your nickname
neoessentials.nick.others             # Set nicknames for others
neoessentials.nick.color              # Use color codes in nicknames
neoessentials.nick.magic              # Use magic/obfuscated formatting
```

### Kit System

#### Kit Usage
```
essentials.kit                     # /kit command - use kits
essentials.kit.list                # /kit list command - list available kits
essentials.kit.preview             # /kit preview command - preview kit contents
```

#### Kit Administration
```
essentials.kit.create              # /kit create command - create new kits
essentials.kit.delete              # /kit delete command - delete kits
essentials.kit.edit                # /kit edit command - modify kits
essentials.kit.give                # /kit give command - give kits to players
essentials.kit.*                   # All kit permissions
```

### NeoEssentials Features

#### Shop System (NEW)
```
neoessentials.shop.create          # Create player shops
neoessentials.shop.use             # Use/buy from shops
neoessentials.shop.break           # Break own shops
neoessentials.shop.modify          # Modify own shop settings
neoessentials.shop.create.admin    # Create admin shops (infinite stock)
neoessentials.shop.break.others    # Break other players' shops
neoessentials.shop.modify.others   # Modify other players' shops
neoessentials.shop.access.others   # Access other players' shop chests
neoessentials.shop.admin           # Full shop administration rights
neoessentials.shop.bypass          # Bypass all shop restrictions
neoessentials.shop.refresh         # Refresh shop signs globally
neoessentials.shop.list            # List all shops on server
neoessentials.shop.*               # All shop permissions
```

#### Bossbar System
```
neoessentials.bossbar.show         # Show bossbars to yourself
neoessentials.bossbar.show.others  # Show bossbars to other players
neoessentials.bossbar.hide         # Hide bossbars
neoessentials.bossbar.broadcast    # Broadcast bossbars to all players
neoessentials.bossbar.create       # Create custom bossbars
neoessentials.bossbar.update       # Update existing bossbars
neoessentials.bossbar.delete       # Delete bossbars
neoessentials.bossbar.templates    # Access bossbar templates
neoessentials.bossbar.*            # All bossbar permissions
```

#### Placeholder System
```
neoessentials.placeholder.test     # Test placeholder values
neoessentials.placeholder.list     # List available placeholders
neoessentials.placeholder.info     # View placeholder information
neoessentials.placeholder.reload   # Reload placeholder system
neoessentials.placeholder.*        # All placeholder permissions
```

#### GUI System
```
neoessentials.gui.open             # Open GUI menus
neoessentials.gui.admin            # Access admin GUI features
neoessentials.gui.themes           # Change GUI themes
neoessentials.gui.*                # All GUI permissions
```

#### Security System
```
neoessentials.security.view        # View security events and logs
neoessentials.security.admin       # Security system administration
neoessentials.security.alerts     # Receive security alerts
neoessentials.security.*           # All security permissions
```

### Permission Management

#### Permission Commands
```
neoessentials.permissions.info     # /permissions info command
neoessentials.permissions.check    # /permissions check command
neoessentials.permissions.user     # User permission management
neoessentials.permissions.group    # Group permission management
neoessentials.permissions.reload   # Reload permission system
neoessentials.permissions.stats    # View permission statistics
neoessentials.permissions.*        # All permission management commands
```

### Administration & Configuration

#### Configuration Management
```
neoessentials.config.reload        # Reload configuration files
neoessentials.config.save          # Save current configuration
neoessentials.config.reset         # Reset configuration to defaults
neoessentials.config.*             # All configuration permissions
```

#### Language System
```
neoessentials.language.set         # Set language preferences
neoessentials.language.list        # List available languages
neoessentials.language.reload      # Reload language files
neoessentials.language.*           # All language permissions
```

#### Performance Monitoring
```
neoessentials.performance.view     # View performance metrics
neoessentials.performance.admin    # Performance system administration
neoessentials.performance.*        # All performance permissions
```

#### Status Monitoring
```
neoessentials.status.view          # View system status
neoessentials.status.admin         # Status system administration
neoessentials.status.*             # All status permissions
```

#### Analytics
```
neoessentials.analytics.view       # View analytics data
neoessentials.analytics.admin      # Analytics system administration
neoessentials.analytics.*          # All analytics permissions
```

### Player Features

#### Playtime Tracking
```
neoessentials.playtime.view        # View your playtime
neoessentials.playtime.others      # View others' playtime
neoessentials.playtime.top         # View playtime leaderboards
neoessentials.playtime.*           # All playtime permissions
```

#### Achievement System
```
neoessentials.achievements.view    # View your achievements
neoessentials.achievements.others  # View others' achievements
neoessentials.achievements.admin   # Achievement system administration
neoessentials.achievements.*       # All achievement permissions
```

#### Player Preferences
```
neoessentials.preferences.set      # Set your preferences
neoessentials.preferences.view     # View preference settings
neoessentials.preferences.*        # All preference permissions
```

### Animation System

#### Animation Commands
```
neoessentials.animation.play       # Play animations
neoessentials.animation.stop       # Stop animations
neoessentials.animation.list       # List available animations
neoessentials.animation.create     # Create custom animations
neoessentials.animation.delete     # Delete animations
neoessentials.animation.*          # All animation permissions
```

### Web Dashboard

#### Web Dashboard Access
```
neoessentials.webdash.access       # Access web dashboard
neoessentials.webdash.admin        # Web dashboard administration
neoessentials.webdash.*            # All web dashboard permissions
```

### Bypass Permissions

#### Cooldown Bypasses
```
essentials.bypass.cooldown         # Bypass all cooldowns
essentials.bypass.cooldown.teleport # Bypass teleportation cooldowns
essentials.bypass.cooldown.command  # Bypass command cooldowns
```

#### Limit Bypasses
```
essentials.bypass.limit.home       # Bypass home limits
essentials.bypass.limit.warp       # Bypass warp limits
```

#### Cost Bypasses
```
essentials.bypass.cost             # Bypass all costs
essentials.bypass.cost.teleport    # Bypass teleportation costs
essentials.bypass.cost.command     # Bypass command costs
```

### Administrative Wildcard Permissions

#### Category Wildcards
```
essentials.*                       # All Essentials permissions
neoessentials.*                    # All NeoEssentials permissions
essentials.teleport.*              # All teleportation permissions
essentials.moderation.*            # All moderation permissions
essentials.economy.*               # All economy permissions
essentials.messaging.*             # All messaging permissions
*.admin                            # All admin permissions across all plugins
```

#### Ultimate Permission
```
*                                  # ALL permissions (use with extreme caution!)
```

### Permission Node Validation

All permission nodes follow these rules:
- Only contain letters, numbers, dots, underscores, and hyphens
- Maximum length of 100 characters
- Use consistent naming conventions
- Support wildcard matching with `*`
- Support negation with `-` prefix

## ⚙️ Configuration

### Permission System Settings

```toml
[permissions]
# Use built-in permission system
useBuiltinPermissions = true

# Enable group inheritance
enableInheritance = true

# Enable wildcard permissions
enableWildcards = true

# Enable negative permissions
enableNegativePermissions = true

# Enable temporary permissions
enableTemporaryPermissions = true

# Permission cache settings
[permissions.cache]
# Cache duration in seconds
cacheTime = 300

# Maximum cached permissions
maxCacheSize = 10000

# Enable cache statistics
enableStatistics = true

# Cleanup settings
[permissions.cleanup]
# Cleanup interval in minutes
cleanupInterval = 5

# Remove expired temporary permissions
removeExpired = true

# Log cleanup operations
logCleanup = false
```

### Default Group Configuration

```toml
[permissions.groups.default]
name = "Default"
priority = 0
prefix = ""
suffix = ""
permissions = [
    "essentials.home",
    "essentials.sethome",
    "essentials.spawn",
    "essentials.back",
    "neoessentials.shop.create",
    "neoessentials.shop.use",
    "neoessentials.shop.break",
    "neoessentials.shop.modify"
]

[permissions.groups.vip]
name = "VIP"
priority = 10
inherits = "Default"
prefix = "&6[VIP] "
suffix = ""
permissions = [
    "essentials.fly",
    "essentials.heal",
    "essentials.feed"
]

[permissions.groups.admin]
name = "Admin"
priority = 100
inherits = "Moderator"
prefix = "&c[Admin] "
suffix = ""
permissions = [
    "essentials.*",
    "neoessentials.*",
    "neoessentials.shop.admin",
    "neoessentials.shop.create.admin",
    "neoessentials.shop.access.others"
]
```

### Integration Settings

```toml
[permissions.integration]
# External permission plugin integration
[permissions.integration.luckperms]
enabled = false
syncGroups = true
syncPermissions = true

[permissions.integration.other]
# Other permission plugin compatibility
fallbackToVanilla = true
respectOpStatus = true
```

## 🚀 Advanced Features

### Group Templates

Create group templates for easy setup:

```bash
# Create staff template
/permissions template create staff "Staff Template" \
  neoessentials.kick neoessentials.mute neoessentials.vanish

# Apply template to group
/permissions group apply Moderator staff
```

### Batch Operations

```bash
# Give multiple permissions at once
/permissions user Steve permission batch add \
  neoessentials.fly neoessentials.heal neoessentials.feed

# Remove multiple permissions
/permissions group VIP permission batch remove \
  neoessentials.give neoessentials.gamemode
```

### Permission Queries

```bash
# List all players with specific permission
/permissions query has neoessentials.fly

# List all players in group
/permissions query group VIP

# List all permissions containing keyword
/permissions query permission heal
```

### Scheduled Operations

```bash
# Schedule temporary promotion
/permissions schedule user Steve group set VIP 7d

# Schedule permission removal
/permissions schedule user Probation permission remove neoessentials.build 24h
```

## 🔄 Integration Examples

### With External Plugins

#### LuckPerms Integration
```toml
[permissions.integration.luckperms]
enabled = true
syncGroups = true
useNeoEssentialsGroups = false
respectLuckPermsPrefix = true
```

#### Vault Integration
```java
// Example vault integration
if (Vault.isAvailable()) {
    Permission vaultPerms = Vault.getPermission();
    // Sync with Vault API
}
```

### With Custom Systems

```java
// Custom permission check
public boolean hasCustomPermission(Player player, String permission) {
    CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
    return manager.hasPermission(player.getUUID(), permission);
}

// Event-based permission updates
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // Apply automatic permissions based on criteria
    if (isFirstTimePlayer(player)) {
        permissionManager.addTemporaryPermission(
            player.getUUID(), 
            "essentials.newbie", 
            Duration.ofHours(24)
        );
    }
}
```

## 🛡️ Security Considerations

### Best Practices
1. **Principle of least privilege** - Give minimal necessary permissions
2. **Regular audit** - Review permissions periodically
3. **Temporary over permanent** - Use temporary permissions when possible
4. **Group over individual** - Prefer group-based permissions
5. **Negative permissions** - Use to restrict access granularly

### Security Features
- **Permission validation** - Prevents invalid permission assignments
- **Audit logging** - Tracks all permission changes
- **Automatic cleanup** - Removes expired temporary permissions
- **Cache security** - Prevents permission cache poisoning

## 🔍 Troubleshooting

### Common Issues

#### Permission Not Working
1. Check permission node spelling
2. Verify group inheritance chain
3. Check for negative permissions
4. Clear permission cache

#### Group Inheritance Problems
1. Verify parent group exists
2. Check for circular inheritance
3. Review group priorities
4. Test with `/permissions debug inheritance`

#### Performance Issues
1. Reduce cache time for dynamic permissions
2. Increase cache size for large servers
3. Monitor cleanup frequency
4. Use `/permissions stats` to check performance

### Debug Commands

```bash
# Debug permission calculation
/permissions debug player Steve essentials.fly

# Show inheritance chain
/permissions debug inheritance VIP

# Cache statistics
/permissions debug cache

# Performance metrics
/permissions debug performance
```

## 📊 Monitoring & Analytics

### Permission Usage Statistics

```bash
# Most used permissions
/permissions stats usage

# Group distribution
/permissions stats groups

# Temporary permission summary
/permissions stats temporary
```

### Audit Logs

Permission changes are logged for security:
```
[2025-08-03 14:30:15] [PERMISSION] Admin gave essentials.fly to Steve (permanent)
[2025-08-03 14:31:22] [PERMISSION] Steve promoted to VIP group by Admin
[2025-08-03 14:32:05] [PERMISSION] Temporary permission essentials.god expired for Alex
```

---

**Related Documentation**: [Essential Commands](Essential-Commands) | [Configuration](Configuration) | [Security Features](Security)

*Last Updated: August 6, 2025*
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)
