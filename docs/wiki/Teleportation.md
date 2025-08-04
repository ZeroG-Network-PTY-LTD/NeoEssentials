# Teleportation System

NeoEssentials provides a comprehensive teleportation system with homes, warps, spawn points, player teleportation, and safety features. This system includes cross-dimensional support, cooldowns, and advanced safety mechanisms.

## 🎯 Overview

The teleportation system includes:
- **Home system** with multiple homes per player
- **Warp system** for public teleportation points
- **Spawn management** with multiple spawn points
- **Player teleportation** with request system
- **Back teleportation** to previous locations
- **Safety checks** to prevent teleporting into unsafe locations
- **Cross-dimensional support** for all teleportation types
- **Cooldowns and delays** for balanced gameplay

## 🏠 Home System

### Commands

#### `/home [name]`
Teleport to one of your homes.

**Usage**:
```bash
# Teleport to default home
/home

# Teleport to specific home
/home base
/home farm
/home nether
```

**Features**:
- Instant teleportation (or delayed based on config)
- Cross-dimensional support
- Safety checks for valid locations
- Cooldown protection

#### `/sethome [name]`
Set a home at your current location.

**Usage**:
```bash
# Set default home
/sethome

# Set named home
/sethome base
/sethome farm
/sethome nether
```

**Limits**:
- Default players: 1 home
- VIP players: 3 homes
- Staff: 5 homes
- Admin: Unlimited homes

#### `/delhome <name>`
Delete one of your homes.

**Usage**:
```bash
# Delete default home
/delhome home

# Delete named home
/delhome base
/delhome farm
```

**Confirmation**:
```
Are you sure you want to delete home 'base'? Type /delhome base confirm
```

#### `/homes`
List all your homes with their locations.

**Example Output**:
```
=== Your Homes ===
1. home - Overworld (123, 64, -456)
2. base - Overworld (789, 70, -123)
3. farm - Overworld (-45, 65, 234)
4. nether - Nether (15, 75, -28)

Usage: 4/5 homes
```

### Home Configuration

```toml
[teleportation.homes]
# Default number of homes per player
defaultHomes = 1

# Maximum homes for different groups
[teleportation.homes.limits]
default = 1
vip = 3
moderator = 5
admin = -1  # Unlimited

# Home teleportation settings
[teleportation.homes.teleport]
# Teleportation delay in seconds
delay = 0
# Cooldown between teleportations in seconds
cooldown = 0
# Cancel teleportation if player moves
cancelOnMove = false
# Cancel teleportation if player takes damage
cancelOnDamage = false
```

## 🌐 Warp System

### Commands

#### `/warp <name>`
Teleport to a public warp point.

**Usage**:
```bash
# Teleport to spawn warp
/warp spawn

# Teleport to public areas
/warp mall
/warp pvp
/warp mining
```

#### `/setwarp <name>`
Create a new warp point (Admin only).

**Usage**:
```bash
# Create spawn warp
/setwarp spawn

# Create public warps
/setwarp mall
/setwarp pvp
/setwarp mining
```

**Permission**: `essentials.setwarp`

#### `/delwarp <name>`
Delete a warp point (Admin only).

**Usage**:
```bash
# Delete warp
/delwarp old_spawn
/delwarp unused_warp
```

**Permission**: `essentials.delwarp`

#### `/warps`
List all available warp points.

**Example Output**:
```
=== Available Warps ===
1. spawn - Overworld (0, 64, 0)
2. mall - Overworld (500, 65, -300)
3. pvp - Overworld (-200, 70, 400)
4. mining - Overworld (1000, 12, -800)
5. nether_hub - Nether (0, 75, 0)

Type /warp <name> to teleport
```

### Warp Configuration

```toml
[teleportation.warps]
# Warp teleportation settings
[teleportation.warps.teleport]
# Teleportation delay in seconds
delay = 0
# Cooldown between teleportations in seconds
cooldown = 5
# Cancel teleportation if player moves
cancelOnMove = false
# Cancel teleportation if player takes damage
cancelOnDamage = false

# Warp creation settings
[teleportation.warps.creation]
# Enable warp categories
enableCategories = true
# Default category for uncategorized warps
defaultCategory = "general"
```

## 🏁 Spawn System

### Commands

#### `/spawn`
Teleport to the server spawn point.

**Usage**:
```bash
# Teleport to main spawn
/spawn

# Teleport to specific spawn (if multiple configured)
/spawn pvp
/spawn survival
```

#### `/setspawn [name]`
Set a spawn point (Admin only).

**Usage**:
```bash
# Set main spawn
/setspawn

# Set named spawn points
/setspawn pvp
/setspawn survival
/setspawn creative
```

**Permission**: `essentials.setspawn`

### Spawn Configuration

```toml
[teleportation.spawn]
# Main spawn location
[teleportation.spawn.main]
world = "overworld"
x = 0.0
y = 64.0
z = 0.0
yaw = 0.0
pitch = 0.0

# Multiple spawn points
[teleportation.spawn.points]
pvp = { world = "overworld", x = 500.0, y = 65.0, z = -300.0 }
survival = { world = "overworld", x = -200.0, y = 70.0, z = 400.0 }

# Spawn teleportation settings
[teleportation.spawn.teleport]
delay = 0
cooldown = 0
cancelOnMove = false
cancelOnDamage = false

# New player spawn settings
[teleportation.spawn.newPlayer]
# Teleport new players to spawn on first join
teleportOnFirstJoin = true
# Give new players a starter kit
giveStarterKit = false
```

## 👥 Player Teleportation

### Commands

#### `/tp <player>`
Teleport to another player (Admin only).

**Usage**:
```bash
# Teleport to player
/tp Steve
/tp Alex

# Teleport player to another player
/tp Steve Alex
```

**Permission**: `essentials.tp`

#### `/tpa <player>`
Request to teleport to another player.

**Usage**:
```bash
# Send teleport request
/tpa Steve
```

**Response to target player**:
```
Steve has requested to teleport to you.
Type /tpaccept to accept or /tpdeny to deny.
Request expires in 60 seconds.
```

#### `/tpaccept`
Accept a teleportation request.

**Usage**:
```bash
# Accept the latest request
/tpaccept

# Accept specific request (if multiple)
/tpaccept Steve
```

#### `/tpdeny`
Deny a teleportation request.

**Usage**:
```bash
# Deny the latest request
/tpdeny

# Deny specific request (if multiple)
/tpdeny Steve
```

#### `/tphere <player>`
Request a player to teleport to you.

**Usage**:
```bash
# Request player to come to you
/tphere Alex
```

### Teleport Request Configuration

```toml
[teleportation.requests]
# Request timeout in seconds
timeout = 60

# Maximum pending requests per player
maxPendingRequests = 5

# Allow multiple requests from same player
allowMultipleFromSame = false

# Auto-accept requests from friends/trusted players
autoAcceptFriends = false

# Request settings
[teleportation.requests.settings]
# Teleportation delay for accepted requests
delay = 3
# Cancel if player moves during delay
cancelOnMove = true
# Cancel if player takes damage during delay
cancelOnDamage = true
```

## ⏪ Back Teleportation

### Command

#### `/back`
Teleport to your previous location.

**Usage**:
```bash
# Go back to last location
/back
```

**Triggers**:
- Death location
- Previous teleportation location
- Previous world change location

### Back System Features

- **Death locations**: Automatically saved when player dies
- **Teleport history**: Saved after any teleportation command
- **World changes**: Saved when switching dimensions
- **Multiple back points**: Can store multiple previous locations
- **Cross-dimensional**: Works across all dimensions

### Back Configuration

```toml
[teleportation.back]
# Enable back teleportation
enabled = true

# Maximum back locations to store per player
maxBackLocations = 5

# Save back location on different events
saveOnDeath = true
saveOnTeleport = true
saveOnWorldChange = true

# Back teleportation settings
[teleportation.back.teleport]
delay = 0
cooldown = 0
cancelOnMove = false
cancelOnDamage = false
```

## 🛡️ Safety System

### Safety Checks

The teleportation system includes comprehensive safety checks:

#### Location Validation
- **Solid ground**: Ensures safe landing spot
- **Air space**: Checks for sufficient headroom
- **Void protection**: Prevents teleporting into void
- **Lava/Fire protection**: Avoids dangerous locations
- **Suffocation prevention**: Ensures breathable space

#### World Boundaries
- **World border**: Respects world border limits
- **Protected areas**: Checks for region protections
- **Spawn protection**: Respects spawn protection zones

#### Player Safety
- **Health checks**: Prevents teleporting while critically injured
- **Combat protection**: Blocks teleportation during combat
- **Movement validation**: Ensures valid movement paths

### Safety Configuration

```toml
[teleportation.safety]
# Enable all safety checks
enableSafetyChecks = true

# Specific safety features
checkSolidGround = true
checkAirSpace = true
preventVoidTeleport = true
preventLavaTeleport = true
preventFireTeleport = true
preventSuffocation = true

# Auto-correction features
[teleportation.safety.autoFix]
# Try to find safe location nearby
findSafeLocation = true
# Maximum distance to search for safe spot
searchRadius = 10
# Move player to safe Y level if needed
adjustYLevel = true

# Dangerous location handling
[teleportation.safety.dangerous]
# Warn before teleporting to dangerous locations
warnBeforeDangerous = true
# Require confirmation for dangerous teleports
requireConfirmation = true
# Admin override for safety checks
adminOverride = true
```

## ⏱️ Cooldowns & Delays

### Cooldown System

Prevents spam and balances gameplay:

#### Cooldown Types
- **Per-command cooldowns**: Different cooldowns for each command
- **Global teleportation cooldown**: Affects all teleportation
- **Player-specific cooldowns**: Individual tracking per player
- **Group-based cooldowns**: Different cooldowns for permission groups

#### Delay System

Adds realistic teleportation delays:

#### Delay Features
- **Countdown display**: Shows remaining time
- **Cancellation triggers**: Movement, damage, commands
- **Visual effects**: Particles and sounds during delay
- **Bypass permissions**: Staff can skip delays

### Cooldown Configuration

```toml
[teleportation.cooldowns]
# Global teleportation cooldown in seconds
globalCooldown = 0

# Per-command cooldowns
[teleportation.cooldowns.commands]
home = 0
warp = 5
spawn = 0
tp = 0
tpa = 30
back = 10

# Group-based cooldowns (overrides command cooldowns)
[teleportation.cooldowns.groups]
[teleportation.cooldowns.groups.default]
home = 10
warp = 15
tpa = 60

[teleportation.cooldowns.groups.vip]
home = 5
warp = 10
tpa = 30

[teleportation.cooldowns.groups.staff]
home = 0
warp = 0
tpa = 0
```

### Delay Configuration

```toml
[teleportation.delays]
# Global teleportation delay in seconds
globalDelay = 0

# Per-command delays
[teleportation.delays.commands]
home = 0
warp = 0
spawn = 0
tp = 0
tpa = 3
back = 0

# Delay cancellation settings
[teleportation.delays.cancellation]
cancelOnMove = false
cancelOnDamage = false
cancelOnCommand = false
cancelOnInventoryChange = false

# Movement sensitivity
movementThreshold = 1.0  # blocks

# Visual effects during delay
[teleportation.delays.effects]
showParticles = true
playSound = true
showActionBar = true
showBossBar = false
```

## 🌍 Cross-Dimensional Support

### Supported Dimensions

All teleportation commands work across dimensions:

- **Overworld** ↔ **Nether**
- **Overworld** ↔ **End**
- **Nether** ↔ **End**
- **Custom dimensions** (mod support)

### Dimensional Features

#### Portal Integration
- **Nether portal linking**: Coordinate conversion
- **End portal handling**: Spawn platform management
- **Custom portal support**: Mod compatibility

#### Safety Across Dimensions
- **Dimension-specific safety**: Different rules per dimension
- **Portal exit safety**: Safe arrival points
- **Coordinate validation**: Proper coordinate conversion

### Cross-Dimensional Configuration

```toml
[teleportation.dimensions]
# Enable cross-dimensional teleportation
enableCrossDimensional = true

# Dimension-specific settings
[teleportation.dimensions.nether]
# Convert coordinates when teleporting from overworld
convertCoordinates = true
# Safety multiplier for coordinate conversion
safetyMultiplier = 8

[teleportation.dimensions.end]
# Safe spawn platform management
createSpawnPlatform = true
# Platform material
platformMaterial = "obsidian"
# Platform size (radius)
platformRadius = 3

# Dimension permissions
[teleportation.dimensions.permissions]
# Require special permission for cross-dimensional teleports
requireSpecialPermission = false
# Permission node for cross-dimensional access
crossDimensionalPermission = "essentials.teleport.crossdimensional"
```

## 📊 Statistics & Tracking

### Teleportation Statistics

Track teleportation usage:

#### Player Statistics
- **Teleportation count**: Total teleportations per player
- **Most used destinations**: Popular homes/warps
- **Distance traveled**: Total distance via teleportation
- **Dimension usage**: Cross-dimensional teleportation stats

#### Server Statistics
- **Most popular warps**: Server-wide usage statistics
- **Peak teleportation times**: Usage patterns
- **Performance metrics**: Teleportation processing time

### Statistics Commands

#### `/teleport stats [player]`
View teleportation statistics.

**Examples**:
```bash
# View your own stats
/teleport stats

# View another player's stats
/teleport stats Steve
```

**Example Output**:
```
=== Teleportation Statistics for Steve ===
Total Teleportations: 1,247
- Homes: 856 (69%)
- Warps: 234 (19%)
- Player TP: 89 (7%)
- Spawn: 45 (4%)
- Back: 23 (2%)

Distance Traveled: 2,847,392 blocks
Cross-Dimensional: 67 teleportations
Most Used Home: base (234 times)
Most Used Warp: mall (45 times)
```

#### `/teleport stats server`
View server-wide teleportation statistics.

**Example Output**:
```
=== Server Teleportation Statistics ===
Total Teleportations Today: 15,678
Peak Hour: 19:00-20:00 (2,345 teleports)

Most Popular Warps:
1. spawn - 3,456 uses
2. mall - 2,789 uses
3. pvp - 1,234 uses

Average Teleports per Player: 47.2
Cross-Dimensional Ratio: 12.4%
```

## 🔧 Advanced Features

### Teleportation Economy

Integrate with economy systems:

```toml
[teleportation.economy]
# Enable teleportation costs
enabled = false

# Cost per teleportation type
[teleportation.economy.costs]
home = 0.0
warp = 5.0
spawn = 0.0
tpa = 10.0
back = 2.0

# Free teleportations per day
[teleportation.economy.free]
dailyFree = 5
# Reset time (24-hour format)
resetTime = "00:00"
```

### Teleportation Warmup

Add warmup times before teleportation:

```toml
[teleportation.warmup]
# Enable warmup system
enabled = false

# Warmup times in seconds
[teleportation.warmup.times]
home = 0
warp = 3
spawn = 0
tpa = 5
back = 2

# Warmup effects
[teleportation.warmup.effects]
showParticles = true
playSound = true
preventMovement = false
preventCommands = false
```

### Smart Teleportation

Intelligent teleportation features:

```toml
[teleportation.smart]
# Smart home selection based on current dimension
smartHomeSelection = false

# Auto-warp suggestions based on location
autoWarpSuggestions = false

# Predictive teleportation (pre-load chunks)
predictiveTeleportation = false

# Smart back system (intelligent location history)
smartBackSystem = true
```

## 🛠️ Integration

### Plugin Integration

#### WorldGuard Integration
```toml
[teleportation.integration.worldguard]
enabled = true
respectRegions = true
bypassWithPermission = true
bypassPermission = "essentials.teleport.bypass.worldguard"
```

#### GriefPrevention Integration  
```toml
[teleportation.integration.griefprevention]
enabled = true
respectClaims = true
allowTeleportToClaims = true
requireClaimTrust = false
```

#### Economy Integration
```toml
[teleportation.integration.economy]
enabled = false
plugin = "vault"  # vault, playerpoints, etc.
chargeForTeleports = false
```

### API Usage

```java
// Teleport player to location
TeleportationManager.teleport(player, location, TeleportCause.COMMAND);

// Set player home
HomeManager.setHome(player.getUUID(), "base", location);

// Create warp
WarpManager.createWarp("mall", location, "Shopping center");

// Add teleport request
TeleportRequestManager.createRequest(requester, target, RequestType.TPA);
```

## 🚨 Troubleshooting

### Common Issues

#### "Unsafe destination" Error
**Cause**: Safety system detected dangerous location
**Solution**: 
1. Check destination for lava, void, or suffocation
2. Use `/tp <player> ~ ~1 ~` to adjust height
3. Disable safety checks temporarily for admin

#### Teleportation Cancelled
**Cause**: Player movement or damage during delay
**Solution**:
1. Stay still during teleportation delay
2. Avoid taking damage before teleport
3. Adjust cancellation settings in config

#### Cross-dimensional Issues
**Cause**: Dimension not loaded or inaccessible
**Solution**:
1. Ensure target dimension is loaded
2. Check dimension permissions
3. Verify coordinate conversion settings

### Debug Commands

```bash
# Debug teleportation system
/teleport debug player Steve

# Check destination safety
/teleport debug safety 100 64 -200

# View teleportation history
/teleport debug history Steve

# Test cross-dimensional conversion
/teleport debug convert nether 100 64 -200
```

---

**Related Documentation**: [Essential Commands](Essential-Commands.md) | [Configuration](Configuration.md) | [Permissions](Permissions.md)

*Last Updated: August 3, 2025*
