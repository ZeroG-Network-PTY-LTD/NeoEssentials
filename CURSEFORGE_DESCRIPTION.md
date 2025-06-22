# NeoEssentials - Server-Side Essentials for NeoForge

**NeoEssentials** is a comprehensive server-side essentials mod for Minecraft NeoForge servers, inspired by EssentialsX for Bukkit/Spigot. It provides all the essential features needed to run and manage a Minecraft server, without requiring any client-side installation.

## True Server-Side Implementation

**This mod is 100% server-side!** Players can join your server using either vanilla or modded clients without needing to install NeoEssentials themselves. The mod has been carefully engineered to:

- Work without any client-side installation
- Not cause client disconnects in modded environments
- Use only vanilla-compatible command arguments
- Support both vanilla and modded clients seamlessly

## Key Features

### 🏠 Teleportation System
- **Multiple Homes**: Set multiple homes and teleport between them with `/home`, `/sethome`, and `/delhome`
- **Warps**: Create public teleportation points with `/warp`, `/setwarp`, and `/delwarp`
- **TPA Commands**: Send and accept teleport requests with `/tpa`, `/tpaccept`, and `/tpdeny`
- **Back Command**: Return to your previous location after teleporting with `/back`

### 💰 Economy Management
- **Balance System**: Check and manage player balances with `/balance` and `/eco`
- **Payments**: Transfer money between players with `/pay`
- **Server Shop Integration**: Ready integration with shop systems

### 🔧 Administrative Tools
- **Admin Panel**: Intuitive admin interface with `/adminpanel`
- **Moderation Tools**: Ban, kick, mute players with professional feedback
- **Performance Monitoring**: Track server TPS, memory usage, and entities
- **Maintenance Mode**: Toggle server maintenance status with whitelist bypass options

### 📦 Player Utilities
- **Kit System**: Create and distribute item kits with cooldowns
- **Mail System**: Send offline messages to players
- **Jail System**: Restrict problematic players to designated areas
- **Vanish**: Become invisible to regular players
- **PowerTools**: Bind commands to items for quick execution

### ⚙️ Other Features
- **AFK Detection**: Automatically detect and mark idle players
- **Chat Management**: Format, color, and manage chat messages
- **Time & Weather Control**: Manage game environment with simple commands

## Highly Configurable

NeoEssentials creates detailed configuration files with helpful comments explaining each option:
```
config/neoessentials-general.toml   # General mod settings
config/neoessentials-database.toml  # Storage backend configuration
```

## Flexible Storage Options

Choose from three storage backends:

1. **JSON** (Default) - Simple file-based storage
2. **SQLite** - Local database file for better performance
3. **MySQL** - External database support for multi-server setups

## Permissions Support

Works with popular permission mods:
- **LuckPerms** (recommended)
- **FTB Ranks**
- Built-in fallback permission system

## Over 50 Essential Commands

NeoEssentials includes a comprehensive set of commands across these categories:
- Administrative commands
- Moderation tools
- Teleportation commands
- Economy management
- Player utility commands
- World management

## Requirements
- Minecraft 1.21.1
- NeoForge
- No client-side installation required!

## Links
- [Wiki & Documentation](https://github.com/zerog/neoessentials/wiki)
- [Issue Tracker](https://github.com/zerog/neoessentials/issues)
- [Discord Community](https://discord.gg/placeholder)

Enjoy managing your server with NeoEssentials!
