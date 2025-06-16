# NeoEssentials

A comprehensive server-side essentials mod for Minecraft NeoForge servers, inspired by EssentialsX for Bukkit/Spigot.

## Features

- Economy system with balance tracking and transactions
- Home teleportation with multiple homes per player
- Warp system for server-wide teleportation points
- Teleport requests and last location tracking
- Player and admin commands for server management
- Kits system for distributing items
- Chat formatting and management
- Integration with permission mods (LuckPerms, FTB Ranks)

## Important: Server-Side Only

**This mod is designed to be installed only on the server side.** It does not need to be installed on clients and provides no client-side functionality.

## Installation

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Configuration

NeoEssentials is highly configurable. The main configuration file is created at:
```
config/neoessentials/config.json
```

Player data is stored in:
```
config/neoessentials/data/
```

## Commands

Here are some of the key commands included:

- `/home` - Teleport to your home
- `/sethome` - Set your home location
- `/warp` - Teleport to a server warp
- `/tpa` - Request to teleport to a player
- `/tpaccept` - Accept a teleport request
- `/back` - Return to your previous location
- `/balance` - Check your economy balance

## Permission Integration

NeoEssentials works with LuckPerms and FTB Ranks for permission control.

## Credits & Resources

Created by ZeroG_Essentialsx
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
