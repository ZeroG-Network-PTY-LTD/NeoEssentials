# NeoEssentials

NeoEssentials is a comprehensive server-side essentials mod for Minecraft NeoForge servers, inspired by EssentialsX for Bukkit/Spigot. It provides all the essential features needed to manage a Minecraft server, including teleportation, economy, administration, and player utilities.

## True Server-Side Implementation

**This mod is designed as a true server-side mod.** It has been carefully engineered to:

- Work without any client-side installation
- Not cause client disconnects in modded environments
- Use only vanilla-compatible command argument types
- Work with both vanilla and modded clients

This means you can run NeoEssentials on your server and clients can connect with any combination of mods without needing to install NeoEssentials on their side.

## Key Features

### 🏠 Teleportation
- Multiple homes system with permissions for additional homes
- Server warp points with customizable permissions
- Player-to-player teleport requests
- Last location tracking with /back command

### 💰 Economy
- Complete economy system with balance tracking
- Player-to-player payments
- Admin economy management commands
- Integration with other economy-based mods

### 🔧 Administration
- Intuitive admin panel interface
- Professional moderation tools (ban, kick, mute)
- Server performance monitoring
- Maintenance mode with bypass permissions

### 📦 Player Utilities
- Kit system with cooldowns and permissions
- Offline mail messaging
- Jail system for managing rule-breakers
- Vanish functionality for staff
- PowerTools for binding commands to items

### ⚙️ Other Features
- AFK detection and notification
- Chat formatting and management
- Time and weather control
- Smart tab completion suggestions
- Integration with permission mods

## Configuration

NeoEssentials is highly configurable with detailed configuration files:
```
config/neoessentials-general.toml   # General mod settings
config/neoessentials-database.toml  # Storage backend configuration
```

All configuration options include helpful comments explaining their purpose.

## Storage Options

Choose from three storage backends:

1. **JSON** (Default) - Simple file-based storage
2. **SQLite** - Local database file
3. **MySQL** - External database support for multi-server setups

## Permissions Support

Works with popular permission mods:
- LuckPerms (recommended)
- FTB Ranks
- Built-in fallback permission system

## Commands

NeoEssentials includes over 50 essential commands across these categories:
- Administrative commands
- Moderation tools
- Teleportation commands
- Economy management
- Player utility commands
- World management

## Links

- [GitHub Repository](https://github.com/zerog/neoessentials)
- [Issue Tracker](https://github.com/zerog/neoessentials/issues)
- [Wiki](https://github.com/zerog/neoessentials/wiki)
- [Discord](https://discord.gg/placeholder)
