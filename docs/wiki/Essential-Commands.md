# Essential Commands

NeoEssentials provides a comprehensive set of essential commands for server administration and player convenience. This page documents all currently implemented commands, including the new GUI system commands and enhanced features.

## 🎮 Player Utility Commands

### Health & Survival

#### `/heal [player]`
**Description**: Restore health, hunger, and remove harmful effects  
**Permission**: `neoessentials.heal` (self), `neoessentials.heal.others` (other players)  
**Cooldown**: 30 seconds (configurable)  
**Usage**:
```bash
/heal                    # Heal yourself
/heal Steve             # Heal player Steve (admin only)
/heal *                 # Heal all online players (admin only)
```
**Effects**:
- Restores full health (20 HP)
- Restores full hunger (20 points)
- Restores full saturation
- Removes all harmful effects (poison, wither, etc.)
- Extinguishes fire
- Plays success sound notification

**Configuration**:
```toml
# In neoessentials-common.toml
[commands.heal]
enabled = true
cooldown = 30
enableSound = true
healOthersPermission = "neoessentials.heal.others"
```

---

#### `/feed [player]`
**Description**: Restore hunger and saturation to full  
**Permission**: `neoessentials.feed` (self), `neoessentials.feed.others` (other players)  
**Cooldown**: 30 seconds (configurable)  
**Usage**:
```bash
/feed                    # Feed yourself
/feed Steve             # Feed player Steve (admin only)
/feed *                 # Feed all online players (admin only)
```
**Effects**:
- Sets hunger to 20 points
- Sets saturation to 20.0
- Removes hunger effect
- Restores food exhaustion

**Configuration**:
```toml
# In neoessentials-common.toml
[commands.feed]
enabled = true
cooldown = 30
feedOthersPermission = "neoessentials.feed.others"
```

---

#### `/god [player]`
**Description**: Toggle invincibility mode  
**Permission**: `neoessentials.god` (self), `neoessentials.god.others` (other players)  
**Usage**:
```bash
/god                     # Toggle god mode for yourself
/god Steve              # Toggle god mode for Steve (admin only)
/god Steve on           # Enable god mode for Steve
/god Steve off          # Disable god mode for Steve
```
**Features**:
- Complete damage immunity
- Prevents fall damage
- Immunity to suffocation
- Protection from void damage
- Fire immunity
- Visual indicator in tablist (optional)
- Prevents drowning
- Prevents fire/lava damage
- Visual indicator when enabled

---

### Movement & Abilities

#### `/fly [player]`
**Description**: Toggle flight mode
**Permission**: `neoessentials.fly` (self), `neoessentials.fly.others` (other players)
**Usage**:
```bash
/fly                     # Toggle flight for yourself
/fly Steve              # Toggle flight for Steve (admin only)
```
**Features**:
- Enables creative-style flight
- Works in any game mode
- Persists across logouts (configurable)

---

#### `/speed <walking|flying> <speed> [player]`
**Description**: Adjust movement speed
**Permission**: `neoessentials.speed` (self), `neoessentials.speed.others` (other players)
**Usage**:
```bash
/speed walking 2         # Set walking speed to 2x normal
/speed flying 3          # Set flying speed to 3x normal
/speed walking 1 Steve   # Set Steve's walking speed (admin only)
```
**Speed Ranges**:
- Walking: 0.1 - 10.0 (1.0 = normal)
- Flying: 0.1 - 10.0 (1.0 = normal)

---

#### `/vanish [player]`
**Description**: Toggle invisibility for staff members
**Permission**: `neoessentials.vanish` (self), `neoessentials.vanish.others` (other players)
**Usage**:
```bash
/vanish                  # Toggle vanish for yourself
/vanish Steve           # Toggle vanish for Steve (admin only)
```
**Features**:
- Complete invisibility to other players
- Hidden from player lists
- Silent movement (no sound)
- Admin-only feature

---

### Teleportation

#### `/back`
**Description**: Return to previous location after teleportation or death
**Permission**: `neoessentials.back`
**Usage**:
```bash
/back                    # Return to last location
```
**Features**:
- Tracks teleportation locations
- Tracks death locations
- Cross-dimensional support
- Shows time since last location

---

### Item Management

#### `/repair [hand|all] [player]`
**Description**: Repair items and equipment
**Permission**: `neoessentials.repair` (hand), `neoessentials.repair.all` (all items)
**Usage**:
```bash
/repair                  # Repair item in hand
/repair hand            # Repair item in hand (explicit)
/repair all             # Repair all items in inventory
/repair all Steve       # Repair all of Steve's items (admin only)
```
**Features**:
- Restores full durability
- Works on tools, weapons, armor
- Supports enchanted items

---

#### `/give <player> <item> [amount]`
**Description**: Give items to players
**Permission**: `neoessentials.give`
**Usage**:
```bash
/give Steve diamond_sword           # Give Steve a diamond sword
/give Steve diamond 64              # Give Steve 64 diamonds
/give @a bread 10                   # Give 10 bread to all players
```
**Features**:
- Supports all Minecraft items
- Batch giving to multiple players
- Automatic inventory management

---

### Virtual Interfaces

#### `/workbench`
**Description**: Open virtual crafting table
**Permission**: `neoessentials.workbench`
**Usage**:
```bash
/workbench              # Open crafting interface
/wb                     # Alias for /workbench
```

---

#### `/anvil`
**Description**: Open virtual anvil interface
**Permission**: `neoessentials.anvil`
**Usage**:
```bash
/anvil                  # Open anvil interface
```

---

#### `/enderchest [player]`
**Description**: Open ender chest inventory
**Permission**: `neoessentials.enderchest` (self), `neoessentials.enderchest.others` (other players)
**Usage**:
```bash
/enderchest             # Open your ender chest
/enderchest Steve       # Open Steve's ender chest (admin only)
/ec                     # Alias for /enderchest
```

---

## 🛠️ Server Administration Commands

### World Management

#### `/time <set|add> <value>`
**Description**: Control world time
**Permission**: `neoessentials.time`
**Usage**:
```bash
/time set day           # Set time to day (1000)
/time set night         # Set time to night (13000)
/time set 12000         # Set specific time
/time add 1000          # Add 1000 ticks to current time
```
**Presets**:
- `day` = 1000 ticks
- `noon` = 6000 ticks  
- `night` = 13000 ticks
- `midnight` = 18000 ticks

---

#### `/weather <clear|rain|thunder> [duration]`
**Description**: Control weather conditions
**Permission**: `neoessentials.weather`
**Usage**:
```bash
/weather clear          # Clear weather
/weather rain           # Start rain
/weather thunder        # Start thunderstorm
/weather clear 300      # Clear weather for 300 seconds
```

---

### Player Management

#### `/gamemode <mode> [player]`
**Description**: Change game mode
**Permission**: `neoessentials.gamemode` (self), `neoessentials.gamemode.others` (other players)
**Usage**:
```bash
/gamemode creative      # Change to creative mode
/gamemode 1             # Change to creative (numeric)
/gamemode survival Steve # Change Steve to survival (admin only)
/gm c                   # Alias for creative
/gmc                    # Direct creative command
/gms                    # Direct survival command
/gma                    # Direct adventure command
/gmsp                   # Direct spectator command
```

---

#### `/kick <player> [reason]`
**Description**: Kick a player from the server
**Permission**: `neoessentials.kick`
**Usage**:
```bash
/kick Steve             # Kick Steve with default reason
/kick Steve "Bad behavior" # Kick Steve with custom reason
```

---

#### `/ban <player> [reason]`
**Description**: Ban a player from the server
**Permission**: `neoessentials.ban`
**Usage**:
```bash
/ban Steve              # Ban Steve with default reason
/ban Steve "Griefing"   # Ban Steve with custom reason
```

---

### Information Commands

#### `/invsee <player>`
**Description**: View another player's inventory
**Permission**: `neoessentials.invsee`
**Usage**:
```bash
/invsee Steve           # View Steve's inventory
```
**Features**:
- Read-only inventory view
- Shows armor slots
- Shows off-hand slot

---

## 🏠 Home & Warp System

### Home Commands

#### `/home [name]`
**Description**: Teleport to a set home location
**Permission**: `neoessentials.home`
**Usage**:
```bash
/home                   # Teleport to default home
/home base              # Teleport to home named "base"
```

---

#### `/sethome [name]`
**Description**: Set a home location
**Permission**: `neoessentials.sethome`
**Usage**:
```bash
/sethome                # Set default home
/sethome base           # Set home named "base"
```
**Limits**: Configurable per permission group

---

#### `/delhome <name>`
**Description**: Delete a home location
**Permission**: `neoessentials.delhome`
**Usage**:
```bash
/delhome base           # Delete home named "base"
```

---

#### `/homes`
**Description**: List all your home locations
**Permission**: `neoessentials.homes`
**Usage**:
```bash
/homes                  # List all homes
```

---

### Warp Commands

#### `/warp <name>`
**Description**: Teleport to a server warp
**Permission**: `neoessentials.warp`
**Usage**:
```bash
/warp spawn             # Teleport to spawn warp
/warp shop              # Teleport to shop warp
```

---

#### `/setwarp <name> [category]`
**Description**: Create a server warp (admin only)
**Permission**: `neoessentials.setwarp`
**Usage**:
```bash
/setwarp spawn          # Create spawn warp
/setwarp shop town      # Create shop warp in town category
```

---

#### `/delwarp <name>`
**Description**: Delete a server warp (admin only)
**Permission**: `neoessentials.delwarp`
**Usage**:
```bash
/delwarp oldshop        # Delete warp named "oldshop"
```

---

#### `/warps [category]`
**Description**: List available warps
**Permission**: `neoessentials.warps`
**Usage**:
```bash
/warps                  # List all warps
/warps town             # List warps in town category
```

---

### Spawn Commands

#### `/spawn`
**Description**: Teleport to server spawn
**Permission**: `neoessentials.spawn`
**Usage**:
```bash
/spawn                  # Teleport to spawn
```

---

#### `/setspawn`
**Description**: Set server spawn location (admin only)
**Permission**: `neoessentials.setspawn`
**Usage**:
```bash
/setspawn               # Set spawn at current location
```

---

## 📊 System Commands

### Information

#### `/neoessentials info`
**Description**: Show NeoEssentials information
**Permission**: `neoessentials.info`
**Usage**:
```bash
/neoessentials info     # Show mod version and status
```

---

#### `/neoessentials reload`
**Description**: Reload configuration files
**Permission**: `neoessentials.reload`
**Usage**:
```bash
/neoessentials reload   # Reload all configurations
```

---

## 🎨 Bossbar Commands

### Display Commands

#### `/bossbar show <template> [player] [duration]`
**Description**: Display a bossbar with template
**Permission**: `neoessentials.bossbar.show`
**Usage**:
```bash
/bossbar show welcome           # Show welcome bossbar
/bossbar show serverinfo Steve 30 # Show server info to Steve for 30s
```

**Available Templates**:
- `welcome` - Welcome message for new players
- `serverinfo` - Server information and stats
- `event` - Event announcements
- `warning` - Warning messages
- `progress` - Progress indicators

---

#### `/bossbar broadcast <template> <duration>`
**Description**: Broadcast bossbar to all players
**Permission**: `neoessentials.bossbar.broadcast`
**Usage**:
```bash
/bossbar broadcast event 60     # Broadcast event bossbar for 60 seconds
```

---

#### `/bossbar update <text> <progress> [player]`
**Description**: Update bossbar content
**Permission**: `neoessentials.bossbar.update`
**Usage**:
```bash
/bossbar update "New Message" 75    # Update bossbar text and progress
```

---

## 🚫 Command Restrictions

### Permission-Based Access
All commands require appropriate permissions. Use a permission plugin or NeoEssentials' built-in permission system.

### Cooldowns and Limits
Some commands have configurable cooldowns and usage limits:
- Teleportation commands: 3-second default delay
- Home setting: Per-group limits
- Speed commands: Maximum speed limits

### World Restrictions
Commands can be restricted to specific worlds through configuration.

## 💡 Tips and Best Practices

### For Players
1. Use `/homes` to manage your home locations efficiently
2. Set descriptive home names like "base", "farm", "mine"
3. Use `/back` after accidental teleports

### For Administrators
1. Set up permission groups for different player ranks
2. Configure appropriate limits for homes and commands
3. Use the bossbar system for server announcements
4. Regular use of `/neoessentials reload` after config changes

### Performance Considerations
- Limit simultaneous teleportations with delays
- Monitor usage of resource-intensive commands
- Use the built-in performance monitoring features

---

**Related Documentation**: [Teleportation System](Teleportation) | [Permissions](Permissions) | [Configuration](Configuration)

*Last Updated: August 3, 2025*
