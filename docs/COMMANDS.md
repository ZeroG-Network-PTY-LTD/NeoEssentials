# NeoEssentials Command Implementation Progress

## Recently Implemented Commands

<<<<<<< HEAD
### Jail Commands
- `/setjail` or `/createjail` - Create a new jail at your current location
- `/deljail` or `/remjail` or `/rmjail` - Remove a jail
- `/jails` - List all available jails
- `/jail` or `/togglejail` - Jail a player at a specific jail with optional time limit
- `/unjail` - Release a player from jail

### PowerTool Commands
- `/powertool` or `/pt` - View powertool information for held item
- `/powertool -c` or `/pt -c` - Clear powertool binding from held item
- `/powertool -a` or `/pt -a` - List all your powertools
- `/powertool -r` or `/pt -r` - Clear all your powertool bindings
- `/powertool -e <true/false>` or `/pt -e <true/false>` - Enable or disable powertools
- `/powertool -t` or `/pt -t` - Toggle powertools on/off
- `/powertool <command>` or `/pt <command>` - Bind a command to the held item

### Mail Commands
- `/mail` - Show mail summary
- `/mail read` - Read all mail messages
- `/mail read <index>` - Read a specific mail message
- `/mail clear` - Delete all mail messages
- `/mail delete <index>` - Delete a specific mail message
- `/mail send <player> <message>` - Send mail to a player

=======
>>>>>>> 2061b5e (feat: Add comprehensive command documentation for NeoEssentials, including utility, UI, player, messaging, moderator, AFK, vanish, and item commands)
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

<<<<<<< HEAD
## Currently WIP Commands

### Item Commands (WIP)
- `/item` or `/i` - Spawn items - *Waiting on CommandBuildContext issue resolution*
- `/repair` or `/fix` - Repair items - *Waiting on CommandBuildContext issue resolution*

## All Planned Commands Implemented!
All core commands from EssentialsX have been implemented except for those requiring CommandBuildContext.
=======
## Currently Missing or WIP Commands

### Item Commands (WIP)
- `/item` or `/i` - Spawn items
- `/repair` or `/fix` - Repair items

### Coming Soon
- `/jail` - Jail a player
- `/powertool` - Create command shortcuts with items
- `/mail` - Send and receive messages to offline players
>>>>>>> 2061b5e (feat: Add comprehensive command documentation for NeoEssentials, including utility, UI, player, messaging, moderator, AFK, vanish, and item commands)

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
<<<<<<< HEAD
- ✅ Jail system
- ✅ Mail system
- ✅ PowerTool system
- ⏳ Economy system (basic implementation)
- ⏳ Kit system (basic implementation)
=======
- ⏳ Economy system (basic implementation)
- ⏳ Kit system (basic implementation)
- ❌ Mail system
- ❌ Jail system
- ❌ PowerTool system
>>>>>>> 2061b5e (feat: Add comprehensive command documentation for NeoEssentials, including utility, UI, player, messaging, moderator, AFK, vanish, and item commands)

## Next Steps

1. Complete ItemCommands implementation when CommandBuildContext issue is resolved
<<<<<<< HEAD
2. Expand permission checks and integration with LuckPerms/FTB Ranks
3. Improve configuration options
4. Extend documentation
=======
2. Add Jail system
3. Add PowerTool functionality
4. Expand permission checks and integration with LuckPerms/FTB Ranks
5. Improve configuration options
6. Extend documentation
>>>>>>> 2061b5e (feat: Add comprehensive command documentation for NeoEssentials, including utility, UI, player, messaging, moderator, AFK, vanish, and item commands)
