# NeoEssentials

![NeoEssentials Logo](docs/images/Logo.png)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.1+-blue.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)
[![GitHub Release](https://img.shields.io/github/v/release/ZeroG-Network-Org/NeoEssentials)](https://github.com/ZeroG-Network-Org/NeoEssentials/releases)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Discord](https://img.shields.io/discord/placeholder?color=7289da&label=Discord&logo=discord&logoColor=white)](https://discord.gg/dUGAQF2Mga)

> A comprehensive server management and essentials mod for Minecraft NeoForge 1.21.1+

## 🌟 Overview

NeoEssentials is a powerful, feature-rich server-side mod that brings essential server management tools to Minecraft NeoForge servers. Designed with performance and flexibility in mind, it provides everything you need to run a professional Minecraft server.

**🎯 Server-Side Only**: Players don't need to install anything - works with vanilla and modded clients!

## ✨ Core Features

### 🏠 **Teleportation System**
- **Homes**: Multiple homes per player with permission-based limits
- **Warps**: Server-wide teleportation points with categories
- **Teleport Requests**: Player-to-player teleportation with accept/deny system
- **Back System**: Return to previous locations with smart history
- **Spawn Management**: Configurable spawn points and first-join spawning
- **Random Teleport**: Safe random location teleportation

### 💰 **Economy System**
- **Multi-Currency Support**: Default currency plus custom currencies
- **Banking System**: Advanced account management with interest
- **Payment System**: Secure player-to-player transactions
- **Shop Integration**: Complete shop system with admin and player shops
- **Auction House**: Bidding system with buyout options
- **Economic Analytics**: Track economy health and player wealth
- **Loan System**: Credit-based lending with configurable terms

### 🎮 **User Interface**
- **Enhanced Tablist**: Fully customizable with animations and hex colors
- **Boss Bar System**: Dynamic boss bars with multiple display modes
- **Animated Displays**: Rotation, scroll, fade, rainbow, typewriter effects
- **Placeholder Support**: Extensive placeholder system with custom placeholders
- **Permission-Based UI**: Different displays for different player groups
- **Real-Time Updates**: Live updating of player information

### 🔧 **Administration Tools**
- **Moderation Commands**: Ban, kick, mute, jail, freeze players
- **Admin Panel**: Comprehensive web-style admin interface
- **Performance Monitoring**: TPS, memory, and entity tracking
- **Maintenance Mode**: Server maintenance with custom messages
- **Vanish System**: Advanced invisibility for staff
- **PowerTools**: Bind commands to items for quick execution
- **Chat Management**: Format and filter chat messages
- **World Control**: Time, weather, and world management

### 📦 **Player Utilities**
- **Kit System**: Configurable item kits with cooldowns and permissions
- **Mail System**: Offline messaging between players
- **AFK Detection**: Automatic AFK detection with customizable timeouts
- **Player Information**: Detailed player stats and information
- **Inventory Management**: Backup, restore, and clear inventories
- **Nickname System**: Custom player nicknames with formatting

### 🌐 **Internationalization**
- **Multi-Language Support**: English, German, Spanish, French, and more
- **Dynamic Language Switching**: Players can change language in-game
- **Localized Messages**: All mod messages support localization
- **Custom Language Files**: Easy to add new languages

### 🛡️ **Permissions & Security**
- **Permission Integration**: Works with LuckPerms, FTB Ranks, and others
- **Security Features**: Rate limiting, command cooldowns, and abuse prevention
- **Group-Based Features**: Different features for different permission groups
- **Secure Storage**: Encrypted player data storage options

## 📋 Requirements

- **Minecraft**: 1.21.1+
- **NeoForge**: 21.1.1 or higher
- **Java**: 21 or higher
- **Server Type**: Dedicated server (client installation not required)

## 🚀 Quick Start

### Installation
1. Download the latest release from [GitHub Releases](https://github.com/ZeroG-Network-Org/NeoEssentials/releases)
2. Place the JAR file in your server's `mods` folder
3. Start your server to generate configuration files
4. Configure the mod in `config/neoessentials/` directory
5. Restart the server to apply changes

### First Configuration
```bash
# Navigate to your server directory
cd /path/to/your/server

# Configuration files will be created in:
config/neoessentials/
├── general.toml          # General mod settings
├── economy.toml          # Economy system configuration
├── homes.toml            # Home system settings
├── warps.toml            # Warp system settings
├── kits.toml             # Kit configuration
├── tablist.toml          # Tablist customization
├── moderation.toml       # Moderation tools settings
├── messaging.toml        # Chat and messaging settings
├── permissions.toml      # Permission system settings
└── storage.toml          # Data storage configuration
```

## 📚 Documentation

### Quick Reference
- 📖 [Installation Guide](docs/wiki/Installation.md) - Detailed installation instructions
- ⚡ [Quick Start Guide](docs/wiki/Quick-Start.md) - Get up and running fast
- ⚙️ [Configuration Guide](docs/wiki/Configuration.md) - Complete configuration reference
- 🎮 [Commands Reference](docs/wiki/Essential-Commands.md) - All available commands
- 🔐 [Permissions Guide](docs/wiki/Permissions.md) - Permission system integration

### Advanced Features
- 🎨 [Tablist Customization](docs/wiki/Tablist-Scoreboard.md) - Custom tablist and scoreboards
- 🌈 [Hex Color Support](docs/wiki/Hex-Colors.md) - Using hex colors in messages
- 🎬 [Animation System](docs/wiki/Animations.md) - Text and UI animations
- 🏪 [Economy System](docs/wiki/Economy.md) - Economy and shop management
- 🏠 [Home & Warp System](docs/wiki/Home.md) - Teleportation management
- 👥 [Player Management](docs/wiki/Player-Management.md) - Player utilities and moderation

### Development
- 🔌 [API Documentation](docs/wiki/API.md) - Developer API reference
- 🎯 [Custom Placeholders](docs/wiki/Placeholders.md) - Creating custom placeholders
- 🔧 [Events System](docs/wiki/Events.md) - Custom event handling

## 🎮 Key Commands

### Player Commands
```
/home [name]              # Teleport to home
/sethome [name]           # Set a home at current location
/delhome <name>           # Delete a home
/homes                    # List all your homes
/warp <name>              # Teleport to a warp
/warps                    # List available warps
/spawn                    # Teleport to spawn
/tpa <player>             # Request teleport to player
/tpaccept                 # Accept teleport request
/back                     # Return to previous location
/kit <name>               # Claim a kit
/kits                     # List available kits
/balance                  # Check your balance
/pay <player> <amount>    # Pay another player
/mail send <player> <msg> # Send mail to player
/language set <lang>      # Change your language
```

### Admin Commands
```
/adminpanel              # Open admin interface
/setwarp <name>          # Create a warp
/delwarp <name>          # Delete a warp
/setspawn                # Set server spawn
/ban <player> [reason]   # Ban a player
/kick <player> [reason]  # Kick a player
/mute <player> [time]    # Mute a player
/jail <player>           # Jail a player
/heal [player]           # Heal player
/feed [player]           # Feed player
/god [player]            # Toggle god mode
/vanish                  # Toggle vanish mode
/maintenance             # Toggle maintenance mode
/economy <subcommand>    # Economy management
/tablist reload          # Reload tablist config
```

## 🔧 Configuration Examples

### Basic Economy Setup
```toml
# config/neoessentials/economy.toml
[economy]
enabled = true
starting_balance = 1000.0
currency_name = "Coins"
currency_symbol = "$"

[banking]
enabled = true
interest_rate = 0.05
compound_frequency = "daily"

[shops]
enabled = true
max_shops_per_player = 3
shop_creation_cost = 500.0
```

### Tablist Configuration
```toml
# config/neoessentials/tablist.toml
[tablist]
enabled = true
update_interval = 1000

[header]
enabled = true
lines = [
    "&#FF6B6B&l&lNeoEssentials Server",
    "&#4ECDC4&lWelcome %player%!",
    "&#45B7D1Online: %online_players%/%max_players%"
]

[footer]
enabled = true
lines = [
    "&#95E1D3TPS: %tps%",
    "&#F9CA24Ping: %ping%ms",
    "&#6C5CE7discord.gg/yourserver"
]

[animations]
type = "rotation"
speed = 2000
```

### Permission Integration
```toml
# config/neoessentials/permissions.toml
[permissions]
system = "luckperms"  # or "ftb_ranks", "default"
prefix = "neoessentials"

[groups]
admin = "neoessentials.admin.*"
moderator = "neoessentials.moderator.*"
vip = "neoessentials.vip.*"
default = "neoessentials.player.*"
```

## 🔗 Integration

### Permission Plugins
- **LuckPerms** ✅ Full support with context integration
- **FTB Ranks** ✅ Complete permission integration
- **Default System** ✅ Built-in fallback permissions

### Placeholder Plugins
- **PlaceholderAPI** ✅ Full placeholder support
- **Custom Placeholders** ✅ Built-in placeholder system

### Economy Plugins
- **Built-in Economy** ✅ Complete economy system
- **External Economy** ✅ Vault-compatible integration

## 🏆 Performance & Compatibility

### Performance Features
- **Asynchronous Operations**: Heavy operations run on separate threads
- **Smart Caching**: Intelligent caching system reduces database calls
- **Memory Optimization**: Efficient memory usage with automatic cleanup
- **Configurable Intervals**: Adjust update frequencies for optimal performance

### Compatibility
- **Server-Side Only**: No client modifications required
- **Vanilla Client Compatible**: Works with unmodded clients
- **Mod Compatibility**: Compatible with most other server mods
- **Version Support**: Supports multiple NeoForge versions

## 🤝 Support & Community

### Getting Help
- 📖 **Documentation**: Check the [Wiki](docs/wiki/) for comprehensive guides
- 💬 **Discord**: Join our [Discord server](https://discord.gg/dUGAQF2Mga) for real-time support
- 🐛 **Bug Reports**: Submit issues on [GitHub](https://github.com/ZeroG-Network-Org/NeoEssentials/issues)
- 💡 **Feature Requests**: Suggest new features on GitHub discussions

### Contributing
- 🔧 **Development**: Fork the repository and submit pull requests
- 🌐 **Translations**: Help translate NeoEssentials to your language
- 📝 **Documentation**: Improve documentation and guides
- 🧪 **Testing**: Test new features and report issues

## 📄 License

NeoEssentials is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**Made with ❤️ by the ZeroG Network Team**
