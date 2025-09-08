# Installation Guide

This guide will walk you through installing and setting up NeoEssentials on your Minecraft server.

## 📋 Requirements

### Server Requirements
- **Minecraft Version**: 1.21.1
- **NeoForge Version**: 21.1.179 or higher
- **Java Version**: Java 21 or higher
- **RAM**: Minimum 2GB allocated to server (4GB recommended)

### Supported Platforms
- Windows Server
- Linux Server
- macOS Server
- Any platform supporting Java 21+

## 📦 Download

### Official Releases
1. Visit the [NeoEssentials GitHub Releases](https://github.com/ZeroG-Network-Org/NeoEssentials/releases)
2. Download the latest `neoessentials-1.0.2.1_HOTFIX.jar` file
3. Verify the file is compatible with your Minecraft/NeoForge version

### Development Builds
Development builds are available from the `Dev-Builds` branch:
- **Warning**: Development builds may be unstable
- Use only for testing new features
- Always backup your server before using dev builds

## 🛠️ Installation Steps

### Step 1: Prepare Your Server
1. Ensure your server is running NeoForge 21.1.179 or higher
2. Stop your server
3. Create a backup of your server files

### Step 2: Install NeoEssentials
1. Download the `neoessentials-1.0.2.1_HOTFIX.jar` file
2. Place it in your server's `mods/` folder
3. If the `mods/` folder doesn't exist, create it

### Step 3: First Startup
1. Start your server
2. NeoEssentials will create default configuration files automatically
3. Check the console for any error messages
4. Look for successful initialization messages

### Expected File Structure After Installation
```
your-server/
├── mods/
│   └── neoessentials-1.0.2.1_HOTFIX.jar
├── config/
│   └── neoessentials/                   # JSON configuration files
│       ├── config.json                  # Main configuration
│       ├── commands.json                # Command settings  
│       ├── customPlaceholders.json      # Custom placeholders
│       ├── tablist.json                 # Tablist configuration
│       └── shops.json                   # Shop system
├── neoessentials_data/                  # Runtime data storage directory (actual folder name)
│   ├── players/                         # Player data files
│   ├── cooldowns/                       # Command cooldown tracking
│   ├── logs/                           # System logs
│   ├── mail/                           # Player mail storage
│   └── animations_cache/               # Animation system cache
└── logs/
    └── latest.log
```

## ⚙️ Initial Configuration

### Core Configuration Files
NeoEssentials uses a unified JSON configuration system. The main configuration files are:

1. **config.json** - Core system settings (generated automatically)
2. **commands.json** - Command-specific configuration (generated automatically)
3. **customPlaceholders.json** - Custom animated placeholders (generated automatically)
4. **tablist.json** - Tablist display configuration (generated automatically)
5. **shops.json** - Shop system configuration (generated automatically)

**Note**: Only `tablist.json` and `customPlaceholders.json` are currently generated. The full configuration system is in development.

### Basic Configuration Example
The main configuration is currently handled internally. The primary user-configurable file is `tablist.json`:

```json
{
  "tablist": {
    "enabled": true,
    "updateInterval": 20,
    "format": "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}",
    "layouts": [
      {
        "priority": 300,
        "conditionType": "permission", 
        "condition": "neoessentials.tablist.admin",
        "header": [
          "&c&l╔═══════════════════════════════════╗",
          "&c&l║           &f&lADMIN PANEL          &c&l║"
        ],
        "footer": [
          "&c&l║ &7Online: &e{server_players}&7/&e{server_max_players}              &c&l║",
          "&c&l╚═══════════════════════════════════╝"
        ]
      }
    ]
  }
}
```

### Essential Settings to Configure
Currently, most settings are managed internally:
1. **Tablist Display**: Configure tablist layouts in `tablist.json`
2. **Custom Placeholders**: Add animated placeholders in `customPlaceholders.json`
3. **Module Control**: Features are enabled/disabled through the Java configuration system
4. **Permission System**: Set up permissions using your preferred permission plugin (LuckPerms recommended)

## 🔑 Permissions Setup

### Built-in Permission System
NeoEssentials includes a comprehensive permission system with 200+ permission nodes organized by feature modules.

#### Basic Permission Structure
```
neoessentials.<category>.<command>[.<option>]
```

#### Essential Permissions for Players
```
neoessentials.home.*                # Home system access
neoessentials.warp.use             # Use public warps
neoessentials.teleport.tpa         # Send teleport requests
neoessentials.kit.starter          # Access starter kit
neoessentials.chat.*               # Chat features
```

#### Admin Permissions
```
neoessentials.admin.*              # Full administrative access
neoessentials.moderation.*         # Moderation tools (kick, ban, mute)
neoessentials.economy.admin        # Economy management
neoessentials.shop.admin           # Shop administration
neoessentials.warp.admin           # Warp creation/management
```

### Using External Permission Systems
NeoEssentials is compatible with:
- **LuckPerms** (Recommended)
- **Other NeoForge permission mods**

## 🧪 Testing Installation

### Verify Core Systems
Test these commands to ensure proper installation:

```bash
# Basic functionality tests
/home                    # Home system (if homes exist)
/warp                    # Warp system (lists available warps)  
/kit                     # Kit system (framework present)
/balance                 # Economy system (basic balance check)

# Tablist system tests 
/tablist theme           # Tablist theme commands
/language list           # Language system test

# Admin commands (requires permissions)
/heal                    # Player healing
/fly                     # Flight toggle
/gamemode creative       # Game mode changes

# System information
/neoessentials version   # Mod version information
```

### Expected Console Output
Look for these messages during startup:

```
[INFO] NeoEssentials: Configuration system initialized
[INFO] NeoEssentials: Language system loaded (9 languages)
[INFO] NeoEssentials: Tablist system initialized with FTB integration
[INFO] NeoEssentials: Economy system loaded (basic implementation)
[INFO] NeoEssentials: Shop system initialized (SignShop implementation)
[INFO] NeoEssentials: Event system loaded
[INFO] NeoEssentials: Permission system initialized (200+ nodes)
[INFO] NeoEssentials: All core systems operational
```

## 🚨 Troubleshooting

### Common Issues

#### "NeoEssentials commands not working"
**Problem**: Commands like `/home` or `/warp` don't exist
**Solutions**: 
1. Verify NeoEssentials jar is in the mods folder
2. Check NeoForge version compatibility (requires 21.1.179+)
3. Restart the server completely
4. Check console for loading errors

#### "Configuration files not generating"
**Problem**: No config files created in `config/neoessentials/`
**Solutions**:
1. Ensure server has write permissions to config directory
2. Check if the mod is loading properly in console
3. Note: Only `tablist.json` and `customPlaceholders.json` are currently auto-generated
4. Other configuration files are managed internally (this is expected behavior)

#### "Permission denied" errors
**Problem**: Players can't use basic commands
**Solutions**:
1. Set up a permission system (LuckPerms recommended)
2. Grant basic permissions: `neoessentials.home.*`, `neoessentials.warp.use`
3. Check that modules are enabled in config.json

#### "Economy not working"
**Problem**: Balance commands don't work
**Solutions**:
1. Verify economy module is enabled internally
2. Check for economy integration conflicts with other mods
3. Review console for economy system initialization messages
4. Note: Economy system is basic implementation, not full banking system

### Advanced Troubleshooting

#### ModuleNotFound Errors
```
java.lang.NoClassDefFoundError: com/zerog/neoessentials/...
```
**Solution**: Update to NeoForge 21.1.179 or higher

#### Configuration Loading Errors
```
Failed to load configuration: config.json
```
**Solution**: 
1. Delete corrupted configuration files
2. Restart server to regenerate defaults
3. Check JSON syntax if manually edited

#### Web Dashboard Issues
```
WebDashboardManager failed to start
```
**Solution**:
1. Check if port is available (default configuration)
2. Verify network permissions
3. Review web dashboard settings in config.json

## 🔄 Updating NeoEssentials

### Update Process
1. **Stop** your Minecraft server
2. **Backup** your configuration and data files
3. **Replace** the old jar file with the new version
4. **Start** the server
5. **Verify** all systems are working correctly

### Configuration Migration
- NeoEssentials automatically handles configuration updates
- New features may add additional configuration options
- Always check the changelog for any breaking changes

## 🔒 Security Best Practices

### File Permissions
Ensure proper file permissions for security:

#### Linux/macOS
```bash
chmod 644 mods/neoessentials-*.jar
chmod -R 755 config/neoessentials/
chmod -R 755 neoessentials/
```

#### Windows
- Ensure the server user has read/write access to config and data directories
- Protect the neoessentials directory from unauthorized access

### Network Security
- Configure firewall rules appropriately
- If using the web dashboard, secure the web interface
- Regularly update NeoForge and NeoEssentials

## 📞 Getting Support

### Before Requesting Help
1. Review this installation guide thoroughly
2. Check existing GitHub issues for similar problems
3. Verify your setup meets all requirements
4. Test with a minimal server setup if possible

### Support Channels
- **GitHub Issues**: Bug reports and technical problems
- **GitHub Discussions**: Questions and feature requests

### Information to Include in Support Requests
- **NeoEssentials Version**: 1.0.2.1_HOTFIX
- **NeoForge Version**: Your current version
- **Minecraft Version**: 1.21.1
- **Full Console Output**: Include complete error messages
- **Configuration Files**: Relevant config.json sections
- **Steps to Reproduce**: Detailed reproduction steps

---

**Next Steps**: 
- [Configuration Guide](Configuration.md) - Detailed configuration options
- [Quick Start Guide](Quick-Start.md) - Get started quickly
- [Commands Reference](Commands.md) - Complete command documentation

*Last Updated: September 7, 2025*

---

**Next Steps**: 
- [Configuration Guide](Configuration.md) - Detailed configuration options
- [Quick Start Guide](Quick-Start.md) - Get started quickly
- [Commands Reference](Commands.md) - Complete command documentation

*Last Updated: September 7, 2025*
