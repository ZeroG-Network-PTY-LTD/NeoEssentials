# Teleportation System

## Overview
The Teleportation System in NeoEssentials provides advanced player movement features, including homes, warps, spawn management, and teleport requests. All functionality is strictly governed by configuration options and permissions.

---

## Managers
- **HomeManager**: Handles player homes (set, delete, list, teleport).
- **WarpManager**: Handles public and player warps (set, delete, list, teleport).
- **SpawnManager**: Handles server spawn location (set, teleport, spawn on join/death).

---

## Configuration (`config.json` > `teleportation`)

### Home Settings
- `maxHomes`: Maximum homes per player (default: 5)
- `allowCrossDimensionHomes`: Allow homes in any dimension
- `homeSetCooldown`: Cooldown between setting homes
- `homeTeleportCooldown`: Cooldown between home teleports
- `homeDeleteCooldown`: Cooldown between deleting homes
- `requireConfirmationForDelete`: Require confirmation before deleting a home
- `enableHomeTeleportSafety`: Check for safe home locations
- `logHomeActions`: Log home actions to server logs

### Warp Settings
- `allowPlayerWarps`: Allow players to create their own warps
- `maxPlayerWarps`: Maximum player warps
- `warpCooldown`: Cooldown between warp teleports
- `warpSetCooldown`: Cooldown between setting warps
- `allowCrossDimensionWarps`: Allow warps in any dimension
- `enableWarpSafety`: Check for safe warp locations
- `logWarpActions`: Log warp actions to server logs

### Spawn Settings
- `spawnOnJoin`: Teleport players to spawn on first join
- `spawnOnDeath`: Teleport players to spawn on death
- `spawnCooldown`: Cooldown between spawn teleports
- `allowSpawnSet`: Allow admins to set spawn location
- `enableSpawnSafety`: Check for safe spawn location
- `logSpawnActions`: Log spawn actions to server logs

### Teleport Request Settings
- `requestTimeout`: Time before teleport requests expire
- `maxPendingRequests`: Max pending requests per player
- `cooldownBetweenRequests`: Cooldown between requests
- `allowMultipleRequests`: Allow multiple requests to same player
- `enableRequestNotifications`: Notify players of requests
- `autoAcceptFromFriends`: Auto-accept requests from friends
- `enableTeleportSafety`: Check destination safety
- `logTeleportRequests`: Log request actions to server logs

### General Settings
- `teleportDelay`: Delay before teleport completes
- `cancelOnMovement`: Cancel teleport if player moves
- `cancelOnDamage`: Cancel teleport if player takes damage
- `enableTeleportWarmup`: Show countdown during delay
- `allowTeleportInCombat`: Allow teleport in combat
- `maxTeleportDistance`: Max teleport distance (-1 = unlimited)
- `enableParticleEffects`: Show particles at teleport
- `enableSoundEffects`: Play sounds during teleport
- `protectedAreas`: List of regions where teleport is restricted

### Command Costs
Defines cost per use for each teleportation command (economy integration).

---

## Commands
- `/home`, `/sethome`, `/delhome`, `/listhomes`
- `/warp`, `/setwarp`, `/delwarp`, `/listwarps`
- `/spawn`, `/setspawn`
- `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/tp`, `/tphere`, `/tpall`, `/tpo`, `/tppos`, `/back`, `/top`, `/jump`, `/jumpto`, `/tpr`

---

## Data Storage
- **homes.json**: Stores player home locations
- **warps.json**: Stores public and player warps
- **spawn.json**: Stores server spawn location

---

## Permissions
- `neoessentials.home.<amount>`: Override max homes per player
- `neoessentials.warp.*`: Warp command permissions
- `neoessentials.spawn.*`: Spawn command permissions
- `neoessentials.teleport.*`: Teleport command permissions

---

## Logging
All major actions (set, delete, teleport) are logged if enabled in config.

---

## Safety Features
- Location safety checks for homes, warps, spawn, and teleport requests
- Confirmation prompts for destructive actions
- Cooldown enforcement for all major actions

---

## Notes
- All features and limits are strictly controlled by config and permissions.
- For advanced usage, refer to the comments in `config.json` for each setting.
