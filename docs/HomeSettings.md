# HomeSettings Options

This document lists all home-related configuration options available in `main.json` for NeoEssentials. Each option can be customized to control home limits, costs, cooldowns, safety, and more.

## Options

- **maxHomes**: Maximum number of homes a regular player can set
- **maxHomesVip**: Maximum number of homes a VIP player can set
- **maxHomesAdmin**: Maximum number of homes an admin can set
- **defaultMaxHomes**: Default maximum homes for new players
- **cooldown**: Cooldown (seconds) between home-related actions
- **setHomeCost**: Cost to set a home
- **teleportHomeCost**: Cost to teleport to a home
- **deleteHomeCost**: Cost to delete a home
- **setHomeCooldown**: Cooldown (seconds) after setting a home
- **teleportHomeCooldown**: Cooldown (seconds) after teleporting to a home
- **deleteHomeCooldown**: Cooldown (seconds) after deleting a home
- **teleportWarmup**: Warmup (seconds) before teleporting to a home
- **cancelOnMove**: Cancel teleport if player moves
- **cancelOnDamage**: Cancel teleport if player takes damage
- **maxMoveDistance**: Maximum allowed movement distance before canceling teleport
- **requireSafeLocation**: Require safe location for teleport
- **restrictedWorlds**: Worlds where homes are restricted

## How to Use
Edit the relevant options in your `main.json` config file under the `homeSettings` section. Example:

```json
"homeSettings": {
  "maxHomes": 3,
  "maxHomesVip": 5,
  "maxHomesAdmin": 10,
  "defaultMaxHomes": 3,
  "cooldown": 60,
  "setHomeCost": 50.0,
  "teleportHomeCost": 10.0,
  "deleteHomeCost": 0.0,
  "setHomeCooldown": 300,
  "teleportHomeCooldown": 60,
  "deleteHomeCooldown": 30,
  "teleportWarmup": 3,
  "cancelOnMove": true,
  "cancelOnDamage": true,
  "maxMoveDistance": 0.5,
  "requireSafeLocation": true,
  "restrictedWorlds": []
}
```

Refer to this document for descriptions of each option.
