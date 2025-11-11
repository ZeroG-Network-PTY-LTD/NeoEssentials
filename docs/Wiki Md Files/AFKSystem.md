# AFK System

## Config File: config.json (afk section)

### Options
- `enabled`: Enable/disable AFK system
- `timeout`: Time in seconds before a player is marked AFK
- `kickTimeout`: Time in seconds before AFK players are kicked (0 = disabled)
- `afkkickMessage`: Message shown when kicked for AFK
- `enableafkBroadcasts`: Enable AFK status broadcasts
- `broadcastOnAfk`: Broadcast when a player goes AFK
- `broadcastOnReturn`: Broadcast when a player returns
- `afkMessage`: Message when player goes AFK
- `returnMessage`: Message when player returns
- `enableTablistIndicator`: Show AFK indicator in tablist
- `tablistAfkPrefix`/`tablistAfkSuffix`: Tablist prefix/suffix for AFK players
- `ignoreAfkInSleep`: Ignore AFK players for sleep checks
- `enableActivityTracking`: Track player activity for AFK detection
- `trackMovement`, `trackChat`, `trackCommands`, `trackInteractions`: What activities reset AFK timer
- `movementThreshold`, `rotationThreshold`: Sensitivity for movement/rotation
- `excludedCommands`: Commands that don't reset AFK
- `autoSave`: Enable auto-saving AFK data
- `saveInterval`: Interval for auto-save (seconds)

### Example
```json
{
  "enabled": true,
  "timeout": 300,
  "kickTimeout": 0,
  "afkkickMessage": "Kicked for being AFK too long",
  "enableafkBroadcasts": true,
  "broadcastOnAfk": true,
  "broadcastOnReturn": true,
  "afkMessage": "{player} is now AFK",
  "returnMessage": "{player} is no longer AFK",
  "enableTablistIndicator": true,
  "tablistAfkPrefix": "[AFK] ",
  "tablistAfkSuffix": "",
  "ignoreAfkInSleep": true,
  "enableActivityTracking": true,
  "trackMovement": true,
  "trackChat": true,
  "trackCommands": true,
  "trackInteractions": true,
  "movementThreshold": 0.1,
  "rotationThreshold": 5.0,
  "excludedCommands": ["afk", "list", "who", "tps", "ping", "help", "?"],
  "autoSave": true,
  "saveInterval": 60
}
```

---

For more details, see the main documentation or ask in the Discord support server.