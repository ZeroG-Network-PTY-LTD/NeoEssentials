# NeoEssentials

A comprehensive server-side essentials mod for Minecraft NeoForge servers, inspired by EssentialsX for Bukkit/Spigot.

## Version Support

This mod is built for:

- **NeoForge 1.21.1** (Primary Version)
- Compatible with future NeoForge 1.21.x versions through NeoForge's compatibility versioning

Note: A separate version for Forge servers will be maintained as a separate project.

## Features

- Economy system with balance tracking and transactions
- Home teleportation with multiple homes per player
- Warp system for server-wide teleportation points
- Teleport requests and last location tracking
- Player and admin commands for server management
- Kits system for distributing items
- Chat formatting and management
- Integration with permission mods (LuckPerms, FTB Ranks)
- Flexible storage system (JSON, SQLite, MySQL)

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

NeoEssentials is highly configurable. The main configuration files are created at:
```
config/neoessentials-general.toml   # General mod settings
config/neoessentials-database.toml  # Storage backend configuration
```

Player data is stored based on your selected storage backend:
- JSON: `neoessentials/` directory
- SQLite: `neoessentials/database.db` file
- MySQL: In your configured MySQL server

## Storage System

NeoEssentials supports three storage backends:

1. **JSON** - Default storage method. Data is stored in JSON files.
2. **SQLite** - Data is stored in a SQLite database file.
3. **MySQL** - Data is stored in a MySQL database server.

For detailed configuration instructions, see [STORAGE.md](docs/STORAGE.md).

## Permissions System

NeoEssentials provides a flexible permissions system with support for:

1. **LuckPerms** - Primary permission provider if installed
2. **FTB Ranks** - Alternative permission provider if LuckPerms is not installed
3. **Default Permissions** - Fallback permissions defined in configuration

For detailed permissions documentation, see [PERMISSIONS.md](docs/PERMISSIONS.md).

## Version Numbering

NeoEssentials uses an automatic build numbering system. The version format is:
```
[major].[minor].[patch].[build]
```

The build number automatically increments with each successful build. This helps track exactly which version is installed on your server.

## Commands

Here are some of the key commands included:

- `/home` - Teleport to your home
- `/sethome` - Set your home location
- `/warp` - Teleport to a server warp
- `/tpa` - Request to teleport to a player
- `/tpaccept` - Accept a teleport request
- `/back` - Return to your previous location
- `/balance` - Check your economy balance

## Credits & Resources

Created by ZeroG Network

Community Documentation: https://docs.neoforged.net/ | NeoForged Discord: https://discord.neoforged.net/
