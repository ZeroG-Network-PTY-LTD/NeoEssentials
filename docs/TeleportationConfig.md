# TeleportationConfig Options

This document lists all teleportation-related configuration options available in `main.json` for NeoEssentials. Each option can be set to `true` (enabled) or `false` (disabled).

## Options

- **tp**: Enable `/tp` command (teleport to another player)
- **tphere**: Enable `/tphere` command (teleport another player to you)
- **tpall**: Enable `/tpall` command (teleport all players to you)
- **tpa**: Enable `/tpa` command (request teleport to another player)
- **tpaccept**: Enable `/tpaccept` command (accept teleport request)
- **tpdeny**: Enable `/tpdeny` command (deny teleport request)
- **tptoggle**: Enable `/tptoggle` command (toggle receiving teleport requests)

- **home**: Enable `/home` command (teleport to a saved home)
- **sethome**: Enable `/sethome` command (set a new home location)
- **delhome**: Enable `/delhome` command (delete a saved home)
- **homes**: Enable `/homes` command (list all saved homes)

- **warp**: Enable `/warp` command (teleport to a warp point)
- **setwarp**: Enable `/setwarp` command (set a new warp point)
- **delwarp**: Enable `/delwarp` command (delete a warp point)
- **warps**: Enable `/warps` command (list all warp points)

- **spawn**: Enable `/spawn` command (teleport to server spawn)
- **setspawn**: Enable `/setspawn` command (set server spawn location)

- **back**: Enable `/back` command (teleport to previous location)

- **rtp**: Enable `/rtp` command (random teleport)
- **randomtp**: Enable `/randomtp` command (alias for random teleport)

## How to Use
Edit the relevant options in your `main.json` config file under the `teleportation` section. Example:

```json
"teleportation": {
  "tp": true,
  "tphere": true,
  "tpall": false,
  "tpa": true,
  "tpaccept": true,
  "tpdeny": true,
  "tptoggle": true,
  "home": true,
  "sethome": true,
  "delhome": true,
  "homes": true,
  "warp": true,
  "setwarp": true,
  "delwarp": true,
  "warps": true,
  "spawn": true,
  "setspawn": true,
  "back": true,
  "rtp": true,
  "randomtp": true
}
```

Refer to this document for descriptions of each option.
