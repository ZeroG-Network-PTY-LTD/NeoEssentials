# WarpSettings Options

This document lists all warp-related configuration options available in `main.json` for NeoEssentials. Each option can be customized to control warp behavior, safety, permissions, costs, and more.

## Options

- **checkForVoid**: Check for void below player when teleporting to warp
- **checkForSuffocation**: Check for suffocation blocks at warp location
- **safeLocationRadius**: Radius to check for safe location around warp
- **enablePublicWarps**: Enable public warps (accessible by all players)
- **enablePrivateWarps**: Enable private warps (owner only)
- **allowCrossDimensionTeleport**: Allow teleporting between dimensions
- **enableWarpCategories**: Enable warp categories (group warps by type)
- **defaultCategories**: Default categories for warps
- **noTeleportWorlds**: Worlds where teleporting to warps is not allowed
- **maxWarpNameLength**: Maximum length for warp names
- **allowSpacesInNames**: Allow spaces in warp names
- **allowSpecialCharacters**: Allow special characters in warp names
- **bannedWarpNames**: List of banned warp names
- **enablePermissionWarps**: Enable permission-based warps
- **permissionPrefix**: Permission prefix for warps
- **enableWarpSigns**: Enable warp signs (physical sign teleport)
- **warpSignFormat**: Format for warp signs
- **warpSignCost**: Cost to use a warp sign
- **enabled**: Enable warp system
- **maxWarps**: Maximum number of warps allowed on the server
- **maxWarpsPerPlayer**: Maximum number of warps per player
- **createWarpCost**: Cost to create a warp
- **teleportWarpCost**: Cost to teleport to a warp
- **deleteWarpCost**: Cost to delete a warp
- **createWarpCooldown**: Cooldown (seconds) after creating a warp
- **teleportWarpCooldown**: Cooldown (seconds) after teleporting to a warp
- **deleteWarpCooldown**: Cooldown (seconds) after deleting a warp
- **teleportWarmup**: Warmup (seconds) before teleporting to a warp
- **cancelOnMove**: Cancel teleport if player moves
- **cancelOnDamage**: Cancel teleport if player takes damage
- **maxMoveDistance**: Maximum allowed movement distance before canceling teleport
- **requireSafeLocation**: Require safe location for teleport
- **checkForLava**: Check for lava at warp location
- **restrictedWorlds**: Worlds where warps are restricted

## How to Use
Edit the relevant options in your `main.json` config file under the `warpSettings` section. Example:

```json
"warpSettings": {
  "checkForVoid": true,
  "checkForSuffocation": true,
  "safeLocationRadius": 3,
  "enablePublicWarps": true,
  "enablePrivateWarps": true,
  "allowCrossDimensionTeleport": true,
  "enableWarpCategories": true,
  "defaultCategories": ["spawn", "shops", "arenas", "farms", "builds"],
  "noTeleportWorlds": [],
  "maxWarpNameLength": 20,
  "allowSpacesInNames": false,
  "allowSpecialCharacters": false,
  "bannedWarpNames": ["spawn", "home", "admin", "server", "console"],
  "enablePermissionWarps": true,
  "permissionPrefix": "neoessentials.warp.",
  "enableWarpSigns": true,
  "warpSignFormat": "[Warp]",
  "warpSignCost": 100.0,
  "enabled": true,
  "maxWarps": 50,
  "maxWarpsPerPlayer": 5,
  "createWarpCost": 500.0,
  "teleportWarpCost": 25.0,
  "deleteWarpCost": 0.0,
  "createWarpCooldown": 600,
  "teleportWarpCooldown": 120,
  "deleteWarpCooldown": 60,
  "teleportWarmup": 5,
  "cancelOnMove": true,
  "cancelOnDamage": true,
  "maxMoveDistance": 0.5,
  "requireSafeLocation": true,
  "checkForLava": true,
  "restrictedWorlds": []
}
```

Refer to this document for descriptions of each option.
