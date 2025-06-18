# NeoEssentials Command Implementation Progress

## Recently Implemented Commands

### Utility Commands
- `/jump` or `/j` - Jump to the block you're looking at (with collision detection)

### UI Commands
- `/workbench` or `/wb` or `/craft` - Opens a crafting table UI
- `/anvil` - Opens an anvil UI
- `/cartographytable` or `/carttable` - Opens a cartography table UI
- `/grindstone` - Opens a grindstone UI
- `/loom` - Opens a loom UI
- `/smithingtable` or `/smithtable` - Opens a smithing table UI
- `/stonecutter` - Opens a stonecutter UI

## Previously Implemented Commands

### Player Commands
- `/heal` - Heal yourself or another player
- `/feed` - Feed yourself or another player
- `/fly` - Toggle flight mode for yourself or another player
- `/god` - Toggle god mode for yourself or another player
- `/speed` - Change walking or flying speed
- `/ext` - Extinguish players who are on fire

### Messaging Commands
- `/msg` or `/tell` or `/w` - Send private messages to players
- `/r` or `/reply` - Reply to the last received message
- `/msgtoggle` - Toggle receiving private messages
- `/rtoggle` - Toggle between replying to last sender or last recipient
- `/socialspy` - Toggle seeing other players' private messages
- `/broadcast` or `/bc` - Broadcast a message to the server

### Moderator Commands
- `/ban` - Ban a player
- `/unban` - Unban a player
- `/tempban` - Temporarily ban a player
- `/banip` - Ban an IP address
- `/unbanip` - Unban an IP address
- `/kick` - Kick a player
- `/mute` - Mute a player

### AFK Commands
- `/afk` - Mark yourself as away from keyboard
- `/afkautotoggle` - Toggle automatic AFK detection
- `/afktime` - Set the time after which a player is considered AFK
- `/back` - Return to your previous location

### Vanish Commands
- `/vanish` or `/v` - Hide yourself from other players

## Currently Missing or WIP Commands

### Item Commands (WIP)
- `/item` or `/i` - Spawn items
- `/repair` or `/fix` - Repair items

### Coming Soon
- `/jail` - Jail a player
- `/powertool` - Create command shortcuts with items
- `/mail` - Send and receive messages to offline players

## Feature Status

- ✅ Home system
- ✅ Warp system
- ✅ Teleportation commands
- ✅ AFK tracking
- ✅ Vanish system
- ✅ UI commands
- ✅ Basic player management
- ✅ Chat and messaging system
- ✅ Basic moderation tools
- ⏳ Economy system (basic implementation)
- ⏳ Kit system (basic implementation)
- ❌ Mail system
- ❌ Jail system
- ❌ PowerTool system

## Next Steps

1. Complete ItemCommands implementation when CommandBuildContext issue is resolved
2. Add Jail system
3. Add PowerTool functionality
4. Expand permission checks and integration with LuckPerms/FTB Ranks
5. Improve configuration options
6. Extend documentation
