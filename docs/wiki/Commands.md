# NeoEssentials Command Reference

NeoEssentials provides a comprehensive command system with 40+ commands organized into logical categories. The system uses Minecraft's native brigadier command framework with advanced permission integration, error handling, and performance monitoring.

**Version**: NeoEssentials 1.0.2

## 🎯 Command System Overview

### Key Features
- **Hierarchical Permission System** - Fine-grained permission control with `.self` and `.others` nodes
- **Centralized Registration** - Commands organized through `CommandRegistry` and `AdminCommandManager`
- **Advanced Error Handling** - Comprehensive validation and user-friendly error messages
- **Performance Monitoring** - Built-in command execution tracking and metrics
- **Multi-language Support** - Localized command messages and descriptions (9 languages)
- **Configuration Integration** - Commands configurable through JSON configs
- **Alias Support** - Multiple aliases for common commands (e.g., `/gm`, `/bal`, `/r`, `/wb`)

### Architecture
Commands are registered through a sophisticated system:
- **CommandRegistry** - Central command registration hub with actual command loading
- **AdminCommandManager** - Centralized admin command management system
- **EssentialsCommandManager** - Organized command category management via events
- **CommandRegistryManager** - Enhanced command organization and error handling

---

## 🔧 Essential Utilities

Core utility commands for everyday server operations:

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/heal [player]` | - | Restore health, hunger, and remove negative effects | `neoessentials.heal[.others]` |
| `/feed [player]` | - | Restore hunger and saturation to full | `neoessentials.feed[.others]` |
| `/god [player]` | - | Toggle invincibility mode | `neoessentials.god[.others]` |
| `/vanish [player]` | - | Toggle invisibility from other players | `neoessentials.vanish[.others]` |
| `/fly [player]` | - | Toggle flight ability | `neoessentials.fly[.others]` |
| `/speed <walk\|fly> <speed> [player]` | - | Set movement speed (0.1-1.0) | `neoessentials.speed[.others]` |
| `/gamemode <mode> [player]` | `/gm`, `/gmc`, `/gms`, `/gma`, `/gmsp` | Change game mode | `neoessentials.gamemode[.others]` |
| `/repair [all]` | - | Repair items in hand or all equipped items | `neoessentials.repair[.all]` |
| `/afk [reason]` | - | Toggle away-from-keyboard status | `neoessentials.afk` |

### GUI & Interface Commands
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/workbench` | `/wb`, `/craft`, `/crafting` | Open virtual crafting table interface | `neoessentials.workbench` |
| `/anvil` | - | Open virtual anvil interface | `neoessentials.anvil` |

---

## 🚀 Teleportation System

Comprehensive teleportation and movement commands:

### Basic Teleportation
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/tp <player\|x y z>` | `/teleport` | Teleport to player or coordinates | `neoessentials.teleport` |
| `/tp <player1> <player2>` | - | Teleport player1 to player2 | `neoessentials.teleport.others` |
| `/back` | - | Return to previous location | `neoessentials.back` |

### TPA (Teleport Request) System
| Command | Description | Permission |
|---------|-------------|------------|
| `/tpa <player>` | Request to teleport to another player | `neoessentials.tpa` |
| `/tpahere <player>` | Request a player to teleport to you | `neoessentials.tpahere` |
| `/tpaccept [player]` | Accept incoming teleport request | `neoessentials.tpaccept` |
| `/tpdeny [player]` | Deny incoming teleport request | `neoessentials.tpdeny` |

### Home System
| Command | Description | Permission |
|---------|-------------|------------|
| `/home [name]` | Teleport to home location | `neoessentials.home` |
| `/sethome [name]` | Set a home location | `neoessentials.sethome` |
| `/delhome <name>` | Delete a home location | `neoessentials.delhome` |
| `/homes` | List your home locations | `neoessentials.homes` |

### Warp System
| Command | Description | Permission |
|---------|-------------|------------|
| `/warp <name>` | Teleport to warp location | `neoessentials.warp` |
| `/setwarp <name>` | Create warp location | `neoessentials.setwarp` |
| `/delwarp <name>` | Delete warp location | `neoessentials.delwarp` |
| `/warps` | List available warps | `neoessentials.warps` |

### Spawn System
| Command | Description | Permission |
|---------|-------------|------------|
| `/spawn` | Teleport to server spawn | `neoessentials.spawn` |
| `/setspawn` | Set server spawn location | `neoessentials.setspawn` |

---

## 💰 Economy System

Advanced economy management with multiple currency support:

### Balance Management
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/balance [player]` | `/bal` | Check player balance | `neoessentials.balance[.others]` |
| `/baltop [limit]` | `/balancetop` | Economy leaderboard | `neoessentials.baltop` |
| `/pay <player> <amount>` | - | Transfer money to another player | `neoessentials.pay` |

### Economy Administration
| Command | Description | Permission |
|---------|-------------|------------|
| `/economy` | Main economy administration command | `neoessentials.economy.admin` |
| `/economy balance <player>` | Check player balance | `neoessentials.economy.admin` |
| `/economy balance set <player> <amount>` | Set player balance | `neoessentials.economy.admin` |
| `/economy balance add <player> <amount>` | Add money to player | `neoessentials.economy.admin` |
| `/economy balance remove <player> <amount>` | Remove money from player | `neoessentials.economy.admin` |

---

## 💬 Communication & Messaging

Social interaction and messaging commands:

### Direct Messaging
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/msg <player> <message>` | `/tell`, `/whisper`, `/message` | Send private message | `neoessentials.msg` |
| `/reply <message>` | `/r` | Reply to last received message | `neoessentials.reply` |

### Mail System
| Command | Description | Permission |
|---------|-------------|------------|
| `/mail send <player> <message>` | Send mail to player | `neoessentials.mail.send` |
| `/mail read` | Read received mail | `neoessentials.mail.read` |
| `/mail clear` | Clear mail inbox | `neoessentials.mail.clear` |

### Server Communication
| Command | Description | Permission |
|---------|-------------|------------|
| `/motd` | Show message of the day | `neoessentials.motd` |
| `/nick <nickname>` | Set display nickname | `neoessentials.nick` |

---

## 🛡️ Moderation & Administration

Comprehensive moderation and administrative tools:

### Player Moderation
| Command | Description | Permission |
|---------|-------------|------------|
| `/ban <player> [reason]` | Ban a player from the server | `neoessentials.ban` |
| `/kick <player> [reason]` | Kick a player from server | `neoessentials.kick` |
| `/mute <player> [duration] [reason]` | Mute a player | `neoessentials.mute` |

### Player Information
| Command | Description | Permission |
|---------|-------------|------------|
| `/whois <player>` | Show detailed player information | `neoessentials.whois` |
| `/seen <player>` | Show last seen information | `neoessentials.seen` |
| `/list` | List online players with formatting | `neoessentials.list` |

### Server Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/give <item> [amount] [player]` | Give items to players | `neoessentials.give[.others]` |
| `/time set <value>` | Set world time | `neoessentials.time` |
| `/time add <value>` | Add to world time | `neoessentials.time` |
| `/weather <clear\|rain\|thunder>` | Change world weather | `neoessentials.weather` |

---

## 📦 Kit System

Predefined item kit management:

| Command | Description | Permission |
|---------|-------------|------------|
| `/kit <name>` | Obtain a predefined item kit | `neoessentials.kit.<kitname>` |
| `/kits` | List available kits | `neoessentials.kit.list` |

---

## ℹ️ Information & Help Commands

Server information and help system:

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/help [category] [page]` | `/help`, `/?` | Show command help with categories | `neoessentials.help` |
| `/info` | - | Show server information | `neoessentials.info` |
| `/rules` | - | Display server rules | `neoessentials.rules` |
| `/playtime [player]` | - | Show playtime statistics | `neoessentials.playtime[.others]` |
| `/achievements [player]` | - | Show player achievements | `neoessentials.achievements[.others]` |
| `/preferences` | - | Open player preferences menu | `neoessentials.preferences` |

---

## 🌐 Language & Localization

Multi-language support system:

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/language [lang]` | `/lang` | Change language or show current | `neoessentials.language` |
| `/language reload` | - | Reload language files (admin) | `neoessentials.language.admin` |

---

## 🔧 System & Administrative Commands

Advanced system management and configuration:

### NeoEssentials Main Command
| Command | Description | Permission |
|---------|-------------|------------|
| `/neoessentials` | Main admin command with subcommands | `neoessentials.admin` |
| `/neoessentials version` | Show mod version information | `neoessentials.admin` |
| `/neoessentials features` | List available features | `neoessentials.admin` |
| `/neoessentials commands` | Show command statistics | `neoessentials.admin` |

### Configuration Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/config reload` | Reload all configuration files | `neoessentials.admin.full` |
| `/config save` | Save current configuration | `neoessentials.admin.full` |
| `/config validate` | Validate configuration files | `neoessentials.admin.full` |
| `/config status` | Show configuration status | `neoessentials.admin.full` |

### Performance Monitoring
| Command | Description | Permission |
|---------|-------------|------------|
| `/performance` | Show server performance metrics | `neoessentials.performance.admin` |
| `/status` | System status monitoring | `neoessentials.admin.basic` |

### Permission System
| Command | Description | Permission |
|---------|-------------|------------|
| `/permissions info [player]` | Check player permissions | `neoessentials.permissions.info` |
| `/permissions check <player> <permission>` | Test permission node | `neoessentials.permissions.check` |
| `/permissiontest <permission>` | Test permission for yourself | `neoessentials.permissions.test` |
| `/animatedprefix` | Manage animated prefixes | `neoessentials.permissions.animatedprefix` |

### Placeholder System
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/placeholder test <text> [player]` | `/placeholders test` | Test placeholder processing | `neoessentials.placeholder.test` |
| `/placeholder list` | `/placeholders list` | List all available placeholders | `neoessentials.placeholder.list` |
| `/placeholder reload` | `/placeholders reload` | Reload custom placeholders | `neoessentials.placeholder.reload` |

### Web Dashboard
| Command | Description | Permission |
|---------|-------------|------------|
| `/dashboard` | Web dashboard management commands | `neoessentials.webdashboard.use` |

### Animation System
| Command | Description | Permission |
|---------|-------------|------------|
| `/animations` | Animation management commands | `neoessentials.animations.admin` |

### Debug & Testing
| Command | Description | Permission |
|---------|-------------|------------|
| `/tablisttest` | Debug tablist display and formatting | `neoessentials.debug.tablist` |

---

## 🚀 Command System Features

### Permission Hierarchy
NeoEssentials uses a sophisticated permission system:

#### Basic Structure
```
neoessentials.command_name        # Self-targeting permission
neoessentials.command_name.others # Other-targeting permission
neoessentials.admin.*             # Full administrative access
neoessentials.moderation.*        # Moderation tools access
```

#### Category Permissions
```
neoessentials.economy.*           # All economy commands
neoessentials.teleport.*          # All teleportation commands
neoessentials.communication.*     # All messaging commands
neoessentials.moderation.*        # All moderation tools
```

### Command Categories in System
Commands are organized into these categories during registration:

1. **Essential Utilities** - Core player commands (`heal`, `feed`, `god`, `vanish`, `fly`)
2. **Player Management** - Player-focused tools (`whois`, `seen`, `list`)
3. **Moderation** - Administrative moderation (`ban`, `kick`, `mute`)
4. **Communication** - Messaging and social features (`msg`, `reply`, `mail`)
5. **Teleportation** - Movement and location commands (`tp`, `home`, `warp`)
6. **Economy** - Economic system commands (`balance`, `pay`, `eco`)
7. **Administration** - Server administration (`config`, `performance`, `status`)
8. **Permissions** - Permission management (`permissions`, `permtest`)
9. **Configuration** - Config management (`config`, `reload`)
10. **Advanced Features** - Specialized tools (`placeholder`, `webdashboard`)

### Configuration Integration
Commands integrate with `commands.json` for:
- **Enable/Disable**: Individual command control
- **Cooldowns**: Per-command cooldown periods
- **Costs**: Economy costs for command usage
- **Warmup**: Delay before command execution
- **Discord Logging**: Automatic Discord integration for admin commands

### Error Handling & Validation
- **Permission Validation**: Automatic permission checking before execution
- **Parameter Validation**: Type checking and range validation for arguments
- **Safety Checks**: Prevention of self-targeting where inappropriate
- **User-Friendly Messages**: Localized error messages in multiple languages
- **Graceful Failure**: Commands fail safely without crashing server

### Performance Features
- **Command Tracking**: Built-in execution time monitoring
- **Performance Metrics**: Command usage statistics and optimization
- **Hot Reloading**: Dynamic configuration changes without restart
- **Lazy Loading**: Commands loaded on-demand for better startup performance

---

## 🔍 Usage Examples

### Basic Command Usage
```bash
# Self-targeting commands
/heal                    # Heal yourself
/god                     # Toggle your god mode
/fly                     # Enable/disable flight

# Targeting other players (requires .others permission)
/heal Steve             # Heal Steve
/tp Alex                # Teleport to Alex
/god NewPlayer          # Toggle god mode for NewPlayer
```

### Advanced Teleportation
```bash
# Coordinate teleportation
/tp 100 64 200          # Teleport to coordinates
/tp Steve 50 70 -100    # Teleport Steve to coordinates

# TPA system workflow
/tpa Steve              # Request to teleport to Steve
/tpaccept               # Steve accepts the request
/tpdeny                 # Steve denies the request

# Home management
/sethome base           # Set home named 'base'
/home base             # Teleport to 'base'
/delhome old_base      # Delete 'old_base' home
/homes                 # List all your homes
```

### Economy Operations
```bash
# Basic balance operations
/balance               # Check your balance
/bal Steve            # Check Steve's balance (requires permission)
/pay Alex 100         # Pay Alex $100
```

### Moderation Examples
```bash
# Player information
/whois Griefer          # Detailed info about Griefer
/seen OldPlayer         # When was OldPlayer last online
/list                   # Formatted online player list

# Moderation actions
/ban Griefer Griefing the spawn    # Ban with reason
/kick Spammer Excessive spam       # Kick with reason
/mute Caps_User 1h Using all caps  # Mute for 1 hour
```

### System Administration
```bash
# Configuration management
/config reload          # Reload all config files
/config validate        # Check config file syntax
/config status         # Show config file status

# Performance monitoring
/performance           # Server performance metrics
/status               # Comprehensive system status

# Placeholder testing
/placeholder test "Welcome %player_name%!"
/placeholder list      # Show all available placeholders
/placeholders reload   # Reload custom placeholders
```

---

## 🔧 Developer Integration

### Command Registration
Commands are registered through the centralized system:

```java
// Basic command registration
manager.registerCommand("Essential Utilities", "heal", "Restore player health", 
                       HealCommand::register);

// Advanced registration with context
manager.registerCommand("Economy", "shop", "Shop management system",
                       (dispatcher, context) -> ShopCommand.register(dispatcher, context));
```

### Error Handling Integration
All commands use the centralized error handling system:

```java
com.zerog.neoessentials.util.ErrorHandler.handleError(
    ErrorCategory.COMMAND_EXECUTION,
    ErrorSeverity.WARNING,
    "Command failed", exception);
```

### Permission Integration
Commands integrate with the permission system:

```java
.requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.HEAL_OTHERS))
```

---

**Related Documentation**: [Permissions](Permissions.md) | [Configuration](Configuration.md) | [Economy](Economy.md) | [API Documentation](API_DOCUMENTATION.md)


---

### 🛠️ Command System Features

#### Core Features
- **Permission System**: Hierarchical permission nodes (e.g., `neoessentials.heal`, `neoessentials.tp.others`)
- **Command Aliases**: Multiple aliases for convenience (/gm for /gamemode, /bal for /balance)
- **Parameter Validation**: Built-in validation for arguments and safety checks
- **Error Handling**: Comprehensive error messages and graceful failure handling
- **Performance Monitoring**: Command execution tracking and performance metrics
- **Localization Support**: Multi-language support with translation keys

#### Permission Structure
Commands use a hierarchical permission system:
- **Self permissions**: `neoessentials.heal` (heal yourself)
- **Others permissions**: `neoessentials.heal.others` (heal other players)
- **Admin permissions**: `neoessentials.admin.*` (full admin access)
- **Module permissions**: `neoessentials.economy.*` (economy features)

#### Configuration Integration
- Commands can be enabled/disabled via `commands.json`
- Cooldowns and costs configurable per command
- Warmup delays for teleportation commands
- Custom messages and formatting options
- Discord logging integration for admin commands

---

### 🔧 Usage Examples

#### Basic Commands
```bash
# Self-targeted commands
/heal                    # Heal yourself
/god                     # Toggle your god mode
/fly                     # Toggle your flight

# Other player targeting (requires permissions)
/heal Steve             # Heal Steve
/tp Alex                # Teleport to Alex
/ban Griefer Griefing   # Ban player with reason
```

#### Teleportation Examples
```bash
# Basic teleportation
/tp Steve               # Teleport to Steve
/tp 100 64 200         # Teleport to coordinates
/tphere Alex           # Bring Alex to you

# TPA system
/tpa Steve             # Request to teleport to Steve
/tpaccept              # Accept incoming request
/tpdeny                # Deny incoming request

# Home system
/sethome base          # Set home named 'base'
/home base             # Go to 'base' home
/homes                 # List all homes
```

#### Economy Commands
```bash
# Check balances
/balance               # Your balance
/bal Steve            # Steve's balance

# Transfer money
/pay Alex 100         # Pay Alex $100
```

#### Administrative Tasks
```bash
# Server management
/config reload        # Reload config
/performance         # Check performance
/status              # System status

# Player moderation
/whois Steve         # Player information
/seen OfflinePlayer  # Last seen info
/mute Spammer 1h Spam # Mute for 1 hour
```

---

### 📋 Command Registration System

Commands are registered through a centralized system in `CommandRegistry.java`:

#### Registration Categories
1. **Essential Utilities** - Core player commands
2. **Player Management** - Player-focused tools  
3. **Moderation** - Admin moderation tools
4. **Communication** - Messaging and social features
5. **Teleportation** - Movement and location commands
6. **Economy** - Economic system commands
7. **Administration** - Server administration
8. **Permissions** - Permission management
9. **Configuration** - Config management
10. **Advanced Features** - Specialized tools

#### Dynamic Command Loading
- Commands are loaded dynamically at server startup
- Failed commands are logged but don't prevent startup
- Hot-reloading support for configuration changes
- Command categorization for better organization

---

### 🔍 Command Validation & Safety

#### Built-in Protections
- **Permission Checks**: All commands verify permissions before execution
- **Parameter Validation**: Arguments are validated for type and range
- **Safety Checks**: Prevent self-targeting where inappropriate
- **Cooldown System**: Prevents command spam and abuse
- **Cost System**: Optional economy costs for commands

#### Error Handling
- Graceful failure with informative error messages
- Automatic error logging for debugging
- User-friendly error messages in multiple languages
- Performance impact tracking for problem commands

---

### 📚 Related Documentation

- [Permissions](Permissions.md) - Permission system guide
- [Configuration](Configuration.md) - Configuration management
- [Teleportation](Teleportation.md) - Teleportation system details
- [Economy](Economy.md) - Economy system documentation
- [API Documentation](API_DOCUMENTATION.md) - Developer API reference

---
