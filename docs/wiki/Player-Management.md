# Player Management

NeoEssentials provides comprehensive player management tools for server administrators. This system includes moderation commands, player monitoring, and administrative utilities to maintain a healthy server environment.

## 🛡️ Moderation Commands

### Basic Player Control

#### Kick Players
```bash
/kick <player> [reason]
```
- Removes a player from the server temporarily
- Optional reason is displayed to the player and logged
- Player can rejoin immediately unless banned

**Examples:**
```bash
/kick PlayerName           # Kick without reason
/kick PlayerName Griefing  # Kick with reason
```

#### Ban Management
```bash
/ban <player> [reason]           # Permanent ban
/tempban <player> <time> [reason] # Temporary ban
/unban <player>                  # Remove ban
/banip <ip> [reason]            # IP ban
/unbanip <ip>                   # Remove IP ban
```

**Time Format Examples:**
- `1h` - 1 hour
- `3d` - 3 days  
- `1w` - 1 week
- `30m` - 30 minutes

**Examples:**
```bash
/ban PlayerName Hacking
/tempban PlayerName 24h Inappropriate behavior
/banip 192.168.1.100 VPN abuse
```

#### Mute System
```bash
/mute <player> [time] [reason]   # Mute player chat
/unmute <player>                # Remove mute
/mutelist                       # List muted players
```

**Examples:**
```bash
/mute PlayerName 1h Spam        # Mute for 1 hour
/mute PlayerName Permanent toxicity  # Permanent mute
/unmute PlayerName              # Remove mute
```

### Advanced Moderation

#### Player Monitoring
```bash
/whois <player>                 # Detailed player information
/seen <player>                  # Last seen information
/lookup <player>                # Player history and statistics
/inspect <player>               # Real-time player monitoring
```

**WhoisInformation Includes:**
- UUID and current username
- IP address and location
- First join and last seen dates
- Playtime statistics
- Permission group
- Current health, hunger, XP
- Current dimension and coordinates

#### Inventory Management
```bash
/invsee <player>                # View player inventory
/enderinv <player>              # View ender chest
/clear <player> [item] [amount] # Clear inventory items
/give <player> <item> [amount]  # Give items to player
```

**Examples:**
```bash
/invsee PlayerName              # Open player's inventory
/clear PlayerName               # Clear entire inventory
/clear PlayerName diamond_sword # Clear specific item
/give PlayerName diamond 64     # Give 64 diamonds
```

## 🎯 Player Assistance Tools

### Teleportation Management
```bash
/tp <player1> <player2>         # Teleport player1 to player2
/tphere <player>                # Teleport player to you
/tpall                          # Teleport all players to you
/tppos <player> <x> <y> <z>     # Teleport to coordinates
```

### Health & Status Management
```bash
/heal <player>                  # Heal specific player
/feed <player>                  # Feed specific player
/god <player>                   # Toggle god mode for player
/fly <player>                   # Toggle flight for player
/vanish <player>                # Toggle vanish for player
/speed <player> <speed>         # Set player movement speed
```

### Game Mode Management
```bash
/gamemode <mode> [player]       # Change game mode
/gms [player]                   # Survival mode
/gmc [player]                   # Creative mode
/gma [player]                   # Adventure mode
/gmsp [player]                  # Spectator mode
```

## 📊 Player Information Systems

### Player Statistics
```bash
/stats [player]                 # View player statistics
/playtime [player]              # View playtime information
/playerinfo <player>            # Comprehensive player data
```

**Statistics Include:**
- Total playtime
- Blocks broken/placed
- Deaths and kills
- Distance traveled
- Items crafted
- Chat messages sent
- Commands used

### Location Tracking
```bash
/getpos [player]                # Get player coordinates
/compass [player]               # Get direction to player
/near [distance]                # List nearby players
/afk [player]                   # Check AFK status
```

## 🎮 Player GUI Management

### Admin Control Panel
```bash
/admin                          # Open admin GUI panel
```

**GUI Features:**
- **Player List** - Browse all online players
- **Quick Actions** - Heal, feed, teleport players
- **Moderation Tools** - Kick, ban, mute from GUI
- **Gamemode Control** - Change player game modes
- **Teleportation** - Teleport to or summon players

### Player Management GUI
```bash
/playermanager                  # Open player management interface
/playermanager <player>         # Open specific player's management
```

**Management Options:**
- **Health Control** - Heal, feed, god mode
- **Inventory Access** - View and modify inventories
- **Teleportation** - Quick teleport options
- **Status Control** - Vanish, fly, speed settings
- **Moderation Actions** - Warn, kick, ban, mute

## 🔒 Permission-Based Management

### Permission Levels
Different management commands require different permission levels:

#### Moderator Permissions
```yaml
neoessentials.kick              # Kick players
neoessentials.mute              # Mute players
neoessentials.tempban           # Temporary bans
neoessentials.invsee            # View inventories
neoessentials.whois             # Player information
```

#### Admin Permissions
```yaml
neoessentials.ban               # Permanent bans
neoessentials.banip             # IP bans
neoessentials.give              # Give items
neoessentials.gamemode.others   # Change other's gamemode
neoessentials.teleport.others   # Teleport others
```

#### Super Admin Permissions
```yaml
neoessentials.admin.*           # All admin commands
neoessentials.override.*        # Override all restrictions
neoessentials.security.manage   # Manage security system
```

## 📝 Logging & Audit Trail

### Command Logging
All moderation actions are automatically logged:

```
[2025-08-06 10:30:45] AdminName kicked PlayerName: Griefing
[2025-08-06 10:31:12] AdminName banned PlayerName: Repeated offenses
[2025-08-06 10:32:00] AdminName gave PlayerName 64x diamond
```

### Audit Commands
```bash
/logs player <player>           # View player's action history
/logs admin <admin>             # View admin's action history
/logs recent [lines]            # View recent moderation actions
/logs search <keyword>          # Search logs for specific events
```

## 🚨 Security Integration

### Threat Detection
Player management integrates with the security system:

- **Automatic Flagging** - Suspicious players are highlighted
- **Risk Levels** - Players shown with risk indicators
- **Alert System** - Notifications for high-risk players
- **Pattern Recognition** - Identify repeat offenders

### Security Commands
```bash
/security player <player>       # View player security profile
/security threats               # List current threats
/security whitelist <player>    # Whitelist trusted player
/security blacklist <player>    # Blacklist problematic player
```

## 🎛️ Bulk Management Tools

### Bulk Operations
```bash
/kickall [reason]               # Kick all players except admins
/healall                        # Heal all online players
/feedall                        # Feed all online players
/tpall                          # Teleport all players to you
/gamemode <mode> @a             # Change gamemode for all players
```

### Group Management
```bash
/group <group> heal             # Heal all players in group
/group <group> feed             # Feed all players in group
/group <group> tp <location>    # Teleport group to location
/group <group> gamemode <mode>  # Set gamemode for group
```

## ⚙️ Configuration Options

### Player Management Settings
Configure in `config/neoessentials/player-management.toml`:

```toml
[moderation]
# Enable moderation commands
enabled = true

# Log all moderation actions
logActions = true

# Require reason for bans/kicks
requireReason = true

# Ban duration limits
maxTempBanDuration = "30d"

[monitoring]
# Enable player monitoring
enabled = true

# Track player statistics
trackStats = true

# Update interval for player data
updateInterval = 300

[security]
# Enable security integration
enabled = true

# Auto-flag suspicious players
autoFlag = true

# Alert threshold for player actions
alertThreshold = 10
```

### GUI Configuration
Customize the admin panel in `config/gui/admin_gui.json`:

```json
{
  "title": "§c§lAdmin Control Panel",
  "size": 54,
  "sections": {
    "player_management": {
      "slot": 10,
      "icon": "minecraft:player_head",
      "name": "§6Player Management",
      "lore": [
        "§7Manage online players",
        "§7• View player information", 
        "§7• Moderation actions",
        "§7• Teleportation tools"
      ]
    }
  }
}
```

## 🛠️ Advanced Features

### Custom Commands
Create custom player management commands:

```bash
/warn <player> <reason>         # Custom warning system
/freeze <player>                # Freeze player movement
/jail <player> <time>           # Jail system integration
/punish <player> <punishment>   # Custom punishment system
```

### Integration Features
- **Database Logging** - Store actions in database
- **Webhook Support** - Send events to external systems
- **API Access** - Programmatic player management

### Automation
- **Auto-moderation** - Automatic responses to violations
- **Scheduled Actions** - Timed moderation actions
- **Rule Enforcement** - Automatic rule violation detection
- **Escalation System** - Progressive punishment system

## 🔧 Troubleshooting

### Common Issues

#### Commands Not Working
- Verify permissions are correctly assigned
- Check if player management is enabled in config
- Ensure target player is online (for most commands)

#### Permission Errors
- Check permission hierarchy
- Verify admin permissions are granted
- Test with different permission levels

#### GUI Not Opening
- Verify GUI system is enabled
- Check admin GUI permissions
- Reload GUI configurations

### Debug Commands
```bash
/neoessentials debug player <player>    # Debug player data
/neoessentials reload playermanagement  # Reload configurations
/neoessentials test permissions <player> # Test player permissions
```

---

## 📚 Related Documentation

- **[Essential Commands](Essential-Commands)** - Complete command reference
- **[Permissions](Permissions)** - Permission system setup
- **[Security Features](Security)** - Security system integration
- **[GUI System](GUI-System)** - Admin panel customization

*Last Updated: August 6, 2025*
