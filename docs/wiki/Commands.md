## NeoEssentials Command System

NeoEssentials provides a comprehensive command system with essential server management, player utilities, and administrative tools. Commands are organized into categories for better management and use Minecraft's native command system with permission-based access control.

---

### 🗂️ Command Categories & Examples

#### Essential Utilities
- `/heal [player]` — Restore health, hunger, and remove negative effects
- `/feed [player]` — Restore hunger and saturation to full
- `/god [player]` — Toggle god mode (invincibility)
- `/vanish [player]` — Toggle invisibility from other players
- `/fly [player]` — Toggle flight mode
- `/speed <walk|fly> <speed> [player]` — Set movement speed (0.1-1.0)
- `/gamemode <mode> [player]` — Change game mode (/gm, /gmc, /gms, /gma, /gmsp)
- `/give <item> [amount] [player]` — Give items to players
- `/repair [all]` — Repair items in hand or all items
- `/workbench` — Open crafting table interface
- `/anvil` — Open anvil interface

#### Teleportation & Movement
- `/tp <player>` — Teleport to a player
- `/tp <player1> <player2>` — Teleport player1 to player2
- `/tp <x> <y> <z>` — Teleport to coordinates
- `/tphere <player>` — Teleport a player to you
- `/teleport` — Alias for /tp command
- `/tpa <player>` — Request teleportation to a player
- `/tpahere <player>` — Request a player to teleport to you
- `/tpaccept [player]` — Accept teleport request
- `/tpdeny [player]` — Deny teleport request
- `/back` — Return to previous location
- `/home [name]` — Teleport to home location
- `/sethome [name]` — Set home location
- `/delhome [name]` — Delete home location
- `/homes` — List your home locations
- `/warp <name>` — Teleport to warp location
- `/setwarp <name>` — Create warp location (Admin)
- `/delwarp <name>` — Delete warp location (Admin)
- `/warps` — List available warps
- `/spawn` — Teleport to spawn location
- `/setspawn` — Set server spawn location (Admin)

#### Economy & Balance
- `/balance [player]` — Check balance (/bal)
- `/pay <player> <amount>` — Pay another player
- `/baltop [limit]` — Economy leaderboard (/balancetop)
- `/eco give <player> <amount>` — Give money to player (Admin)
- `/eco take <player> <amount>` — Take money from player (Admin)
- `/eco set <player> <amount>` — Set player balance (Admin)

#### Messaging & Communication
- `/msg <player> <message>` — Send private message (/tell, /whisper)
- `/reply <message>` — Reply to last message (/r)
- `/mail send <player> <message>` — Send mail to player
- `/mail read` — Read received mail
- `/mail clear` — Clear mail inbox
- `/motd` — Show message of the day
- `/nick <nickname>` — Set nickname
- `/broadcast <message>` — Server-wide announcement (Admin)

#### Moderation & Administration
- `/ban <player> [reason]` — Ban a player
- `/kick <player> [reason]` — Kick a player
- `/mute <player> [duration] [reason]` — Mute a player
- `/unmute <player>` — Unmute a player
- `/whois <player>` — Show player information
- `/seen <player>` — Show last seen information
- `/list` — List online players
- `/afk [reason]` — Toggle AFK status

#### Server Management & World
- `/time set <value>` — Set world time
- `/time add <value>` — Add to world time
- `/weather clear|rain|thunder` — Change weather
- `/reload` — Reload NeoEssentials configuration
- `/neoessentials` — Main admin command with subcommands
- `/config <reload|validate>` — Configuration management
- `/performance` — Show server performance metrics

#### Player Information & Utilities
- `/help [command]` — Show command help
- `/info` — Show server information
- `/rules` — Display server rules
- `/playtime [player]` — Show playtime statistics
- `/achievements [player]` — Show player achievements
- `/preferences` — Open player preferences menu

#### Kit System
- `/kit <name>` — Get a kit
- `/kits` — List available kits

#### Permission & Testing
- `/permissions <player>` — Check player permissions (Admin)
- `/permtest <permission>` — Test permission node (Admin)
- `/animatedprefix` — Manage animated prefixes (Admin)

#### Language & Localization
- `/language [lang]` — Change language or show current
- `/lang [lang]` — Alias for language command

#### Placeholder System
- `/placeholders` — List available placeholders
- `/placeholder test <placeholder>` — Test placeholder values

#### Advanced Admin Tools
- `/cleanup` — Server cleanup utilities (Admin)
- `/status` — System status monitoring (Admin)
- `/error` — Error management commands (Admin)
- `/webdashboard` — Web dashboard management (Admin)
- `/tablisttest` — Debug tablist display (Admin)

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

# Admin economy
/eco give Steve 1000  # Give Steve $1000
/eco set Alex 500     # Set Alex's balance to $500
/baltop 10           # Show top 10 richest players
```

#### Administrative Tasks
```bash
# Server management
/reload               # Reload config
/performance         # Check performance
/status              # System status

# Player moderation
/whois Steve         # Player information
/seen OfflinePlayer  # Last seen info
/mute Spammer 1h Spam # Mute for 1 hour

# World management
/time set day        # Set to daytime
/weather clear       # Clear weather
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

- [Essential Commands](Essential-Commands.md) - Detailed command reference
- [Permissions](Permissions.md) - Permission system guide
- [Configuration](Configuration.md) - Configuration management
- [Teleportation](Teleportation.md) - Teleportation system details
- [Economy](Economy.md) - Economy system documentation
- [API Documentation](API_DOCUMENTATION.md) - Developer API reference

---

*Last Updated: January 2025*
