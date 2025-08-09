# Player Management

NeoEssentials provides essential player management tools for server administrators. This includes moderation commands, player assistance tools, and basic administrative utilities to help maintain your server.

## 🛡️ Moderation Commands

### Basic Player Control

#### Kick Players
```bash
/kick <player> [reason]
```
Temporarily removes a player from the server. Players can rejoin immediately.

**Examples:**
```bash
/kick PlayerName Griefing
/kick PlayerName
```

#### Ban Management
```bash
/ban <player> [reason]          # Permanent ban (basic implementation)
/tempban <player> <time> [reason] # Temporary ban with duration
```

**Time Format Examples:**
- `60m` - 60 minutes
- `24h` - 24 hours  
- `7d` - 7 days

**Examples:**
```bash
/ban PlayerName Hacking
/tempban PlayerName 1h Spamming
```

#### Mute System
```bash
/mute <player> <duration> [reason]   # Mute player chat
/unmute <player>                     # Remove mute
```

**Examples:**
```bash
/mute PlayerName 30m Inappropriate language
/unmute PlayerName
```

#### Jail System
```bash
/jail <player> <duration> [reason]   # Jail player (restricts movement)
/unjail <player>                     # Release from jail
```

**Examples:**
```bash
/jail PlayerName 1h Griefing
/unjail PlayerName
```

## 🎯 Player Assistance Tools

### Health & Status Management
```bash
/heal [player]                  # Heal yourself or another player
/feed [player]                  # Feed yourself or another player
/god [player]                   # Toggle god mode
/vanish [player]                # Toggle vanish mode
/fly [player]                   # Toggle flight mode
/speed <walk|fly> <speed> [player] # Set movement speed
```

**Examples:**
```bash
/heal                           # Heal yourself
/heal PlayerName                # Heal another player
/god PlayerName                 # Toggle god mode for player
/speed walk 2 PlayerName        # Set walk speed to 2x
```

### Game Mode Management
```bash
/gamemode <mode> [player]       # Change game mode
/gms [player]                   # Survival mode
/gmc [player]                   # Creative mode
/gma [player]                   # Adventure mode
/gmsp [player]                  # Spectator mode
```

### Teleportation Management
```bash
/tp <player1> <player2>         # Teleport player1 to player2
/tphere <player>                # Teleport player to you
/tppos <player> <x> <y> <z>     # Teleport to coordinates
```

### Inventory Management
```bash
/invsee <player>                # View player inventory
/give <player> <item> [amount]  # Give items to player
```

**Examples:**
```bash
/invsee PlayerName
/give PlayerName diamond 64
```

## 📊 Player Information

### Player Data Commands
```bash
/whois <player>                 # View detailed player information
/playtime [player]              # View playtime statistics
```

**Whois Information Includes:**
- Player UUID and username
- Current location and world
- Health, hunger, and experience
- Permission group (if applicable)
- Playtime statistics

## 🛡️ Permissions

### Moderation Permissions
```yaml
neoessentials.kick              # Kick players
neoessentials.ban               # Ban players (permanent)
neoessentials.tempban           # Temporary bans
neoessentials.mute              # Mute players
neoessentials.unmute            # Unmute players
neoessentials.jail              # Jail players
neoessentials.unjail            # Unjail players
```

### Player Assistance Permissions
```yaml
neoessentials.heal              # Heal yourself
neoessentials.heal.others       # Heal other players
neoessentials.feed              # Feed yourself
neoessentials.feed.others       # Feed other players
neoessentials.god               # God mode for yourself
neoessentials.god.others        # God mode for others
neoessentials.vanish            # Vanish for yourself
neoessentials.vanish.others     # Vanish for others
neoessentials.fly               # Flight for yourself
neoessentials.fly.others        # Flight for others
neoessentials.speed             # Speed for yourself
neoessentials.speed.others      # Speed for others
```

### Administrative Permissions
```yaml
neoessentials.gamemode          # Change your gamemode
neoessentials.gamemode.others   # Change others' gamemode
neoessentials.give              # Give items
neoessentials.invsee            # View inventories
neoessentials.teleport.others   # Teleport others
neoessentials.whois             # View player information
```

## ⚙️ Configuration

Player management features are configured through the main NeoEssentials configuration files:

### Main Configuration
```toml
# In neoessentials-common.toml
[moderation]
enabled = true              # Enable moderation commands
logActions = true          # Log moderation actions
requireReason = true       # Require reason for bans/kicks

[commands]
# Individual command toggles
kick = true
ban = true
tempban = true
mute = true
jail = true
heal = true
feed = true
god = true
vanish = true
fly = true
speed = true
gamemode = true
```

### Command-Specific Settings
```toml
# Cooldowns and limits
[commands.heal]
cooldown = 30              # Cooldown in seconds

[commands.feed]
cooldown = 30              # Cooldown in seconds

[commands.speed]
maxWalkSpeed = 10.0        # Maximum walk speed
maxFlySpeed = 10.0         # Maximum fly speed
```

## 🔧 Usage Examples

### Basic Moderation
```bash
# Check player information
/whois SuspiciousPlayer

# Warn with a temporary mute
/mute SuspiciousPlayer 10m Please read the rules

# For more serious issues
/tempban SuspiciousPlayer 1h Rule violation

# Permanent removal
/ban ProblemPlayer Hacking
```

### Player Assistance
```bash
# Help a new player
/heal NewPlayer
/feed NewPlayer
/gamemode survival NewPlayer

# Give starting items
/give NewPlayer bread 32
/give NewPlayer wooden_sword 1
```

### Administrative Tasks
```bash
# Check on players
/whois PlayerName
/invsee PlayerName

# Quick player management
/tp StuckPlayer SafeLocation
/god PlayerName              # Temporary protection
```

## 📝 Logging

All moderation actions are automatically logged to help track administrative activity:

- Kicks, bans, and mutes are logged with timestamp, admin, and reason
- Player assistance commands (heal, feed) are logged when used on others
- Administrative actions (gamemode changes, teleports) are tracked

Log entries follow this format:
```
[TIMESTAMP] ADMIN performed ACTION on PLAYER: REASON
```

## � Troubleshooting

### Common Issues

**Commands not working:**
- Check that you have the required permissions
- Verify the player is online (for most commands)
- Ensure the command syntax is correct

**Moderation not effective:**
- Verify moderation system is enabled in configuration
- Check that target players don't have exemption permissions
- Confirm reasons are provided if required by configuration

**Player assistance commands failing:**
- Check command cooldowns in configuration
- Verify you have permission to use commands on other players
- Ensure target player is online and in the same world

### Debug Commands
```bash
/neoessentials reload        # Reload configuration
/neoessentials info          # View system information
```

---

*NeoEssentials Player Management - Essential tools for server administration and player assistance.*
