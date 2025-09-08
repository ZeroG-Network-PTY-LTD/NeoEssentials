# NeoEssentials Wiki

Welcome to the comprehensive documentation for **NeoEssentials** - a modern Minecraft server administration mod for NeoForge.

## 🏠 Quick Navigation

### 📚 Getting Started
- [Installation Guide](Installation.md) - How to install and set up NeoEssentials
- [Quick Start](Quick-Start.md) - Get up and running in 5 minutes
- [Features Overview](Features.md) - Complete overview of all available features
- [Configuration](Configuration.md) - Complete configuration guide
- [Permissions](Permissions.md) - Permission system documentation

### 🎮 Commands & Features
- [Commands](Commands.md) - Core utility commands and administrative tools
- [Economy System](Economy.md) - Currency management and shop system
- [Custom Placeholders](Custom_Placeholders.md) - Create custom dynamic placeholders

### 🛠️ Advanced Features
- [Tablist Display System](Tablist.md) - Permission-based automatic tablist layouts
- [Placeholder System](Placeholders.md) - Dynamic placeholders and FTB integration
- [Language System](Language.md) - Multi-language support
- [Performance Monitoring](Performance.md) - Real-time server performance tracking

### 🔧 Technical Documentation
- [API Reference](API_DOCUMENTATION.md) - Developer API documentation
- [Events System](Events.md) - Event handling and system integration
- [Data Storage](Storage.md) - JSON-based storage with async operations

## 📊 Current Implementation Status

**NeoEssentials 1.0.2** is actively developed with comprehensive functionality:

### ✅ Core Features
- **Essential Commands** - 25+ utility commands including heal, feed, fly, god, vanish, speed, gamemode
- **Permission System** - Sophisticated 4-group system (Default/VIP/Moderator/Admin) with inheritance
- **Economy System** - Complete currency management with sign-based shops and transaction tracking
- **Performance Monitoring** - Enterprise-grade performance tracking with concurrent analytics
- **Language System** - Multi-language support with configurable message system
- **Data Storage** - Advanced JSON-based storage with async operations and memory-efficient caching

### ✅ Advanced Systems
- **Tablist Display** - Permission-based automatic layouts with FTB integration
- **Placeholder System** - 50+ built-in placeholders with custom placeholder support and FTB integration
- **Configuration Management** - Unified JSON configuration system with hot-reload capability
- **API Framework** - Comprehensive developer API for extensions and integrations

### 🔧 System Components
- **StorageManager** - Async file operations with soft-reference caching (500-entry limit)
- **ConfigManager** - Manages 5 core JSON files (config, commands, customPlaceholders, tablist, shops)
- **CustomPermissionsManager** - Role-based permission system with dynamic group management
- **PlaceholderManager** - Advanced placeholder processing with FTB Teams integration
- **TabListManager** - Permission-based tablist with automatic layout selection
- **PerformanceManager** - Real-time server monitoring with memory and TPS tracking

## 🎯 Key Features

### 🎮 Essential Commands
- `/heal [player]` - Restore health and remove harmful effects
- `/feed [player]` - Restore hunger and saturation
- `/fly [player]` - Toggle flight mode
- `/god [player]` - Toggle invincibility
- `/vanish [player]` - Toggle invisibility
- `/speed <walk|fly> <speed> [player]` - Adjust movement speed
- `/gamemode <mode> [player]` - Change game modes
- `/give <player> <item> [amount]` - Give items to players
- `/repair [all|hand]` - Repair items and equipment

### 🏠 Teleportation & Homes
- `/home [name]` - Teleport to set homes
- `/sethome [name]` - Set home locations
- `/delhome <name>` - Delete home locations
- `/homes` - List your homes
- `/warp <name>` - Teleport to server warps
- `/warps` - List available warps

### 💰 Economy System
- `/balance [player]` - Check balance
- `/pay <player> <amount>` - Send money to players
- `/shop` - Shop management commands
- Sign-based shops with protection system

### 🛠️ Administrative Tools
- `/permissions` - Advanced permission management system
- `/config reload|save|status|validate` - Configuration management
- `/performance stats|memory|cache|clear` - Performance monitoring
- `/language reload` - Reload language files
- `/time <set|add> <value>` - Control world time
- `/weather <clear|rain|thunder>` - Control weather

### 🎨 Display Features
- **Automatic Tablist** - Permission-based layouts with headers/footers
- **Placeholder Integration** - 50+ dynamic placeholders
- **FTB Integration** - Team names, colors, and FTB-specific placeholders
- **Real-time Updates** - Live server statistics and player information

## 🚀 Getting Started

1. **[Install NeoEssentials](Installation.md)** - Download and install the mod for NeoForge
2. **[Quick Start Guide](Quick-Start.md)** - Get essential features running in minutes
3. **[Configure Permissions](Permissions.md)** - Set up the 4-group permission system
4. **[Learn Core Commands](Commands.md)** - Master the 25+ essential commands
5. **[Customize Placeholders](Custom_Placeholders.md)** - Create dynamic custom content

## 🔧 Configuration Overview

NeoEssentials uses a sophisticated JSON-based configuration system:

- **`config.json`** - Main settings, language, FTB integration, Discord settings
- **`commands.json`** - Command costs, cooldowns, warmups, Discord logging
- **`customPlaceholders.json`** - Custom placeholder definitions (conditional, static, animated)
- **`tablist.json`** - Permission-based tablist layouts with headers/footers
- **`shops.json`** - Shop system settings and Discord notifications

All configurations support hot-reload via `/config reload` command.

## 🤝 Community & Support

- **GitHub Repository**: [NeoEssentials](https://github.com/ZeroG-Network-Org/NeoEssentials)
- **Issues & Bug Reports**: Use GitHub Issues for technical problems
- **Feature Requests**: Submit via GitHub Discussions
- **Current Version**: 1.0.2 for NeoForge

## 📝 Contributing

NeoEssentials is open-source and welcomes contributions:
- **Bug Reports** - Help identify and fix issues
- **Feature Development** - Contribute new functionality
- **Code Review** - Review pull requests and suggest improvements
- **Documentation** - Improve and expand this wiki
- **Testing** - Test new features and provide feedback

## 📄 License

NeoEssentials is released under the MIT License. See the [LICENSE](../LICENSE) file for details.

---

*Last Updated: September 7, 2025 - NeoEssentials 1.0.2*

