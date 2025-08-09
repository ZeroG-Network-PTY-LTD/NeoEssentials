# Installation Guide

This guide will walk you through installing and setting up NeoEssentials on your Minecraft server.

## 📋 Requirements

### Server Requirements
- **Java Version**: Java 21 or higher
- **RAM**: Minimum 2GB allocated to server

### Supported Platforms
- Windows Server
- Linux Server
- macOS Server
- Any platform supporting Java 21+

## 📦 Download

### Official Releases
1. Visit the [NeoEssentials GitHub Releases](https://github.com/ZeroG-Network-Org/NeoEssentials/releases)
2. Download the latest `neoessentials-x.x.x.jar` file
3. Verify the file is compatible with your Minecraft/NeoForge version

### Development Builds
Development builds are available from the `Dev-Builds` branch:
- **Warning**: Development builds may be unstable
- Use only for testing new features
- Always backup your server before using dev builds

## 🛠️ Installation Steps

### Step 1: Prepare Your Server
1. Ensure your server is properly configured
2. Stop your server
3. Create a backup of your server files

### Step 2: Install NeoEssentials
1. Download the `neoessentials-x.x.x.jar` file
2. Place it in your server's `mods/` folder
3. If the `mods/` folder doesn't exist, create it

### Step 3: First Startup
1. Start your server
2. NeoEssentials will create default configuration files
3. Check the console for any error messages

### Expected File Structure
```
your-server/
├── mods/
│   └── neoessentials-1.0.2.jar
├── config/
│   ├── neoessentials-common.toml    # Forge mod configuration
│   ├── neoessentials-general.toml   # General NeoEssentials settings  
│   ├── neoessentials-gui.toml       # GUI system configuration
│   └── neoessentials/               # Feature configurations (JSON)
│       ├── main.json                # Core mod settings
│       ├── economy.json             # Economy system
│       ├── homes.json               # Home system
│       ├── kits.json                # Kit definitions
│       ├── warps.json               # Warp locations
│       ├── moderation.json          # Moderation tools
│       ├── messaging.json           # Chat and messages
│       ├── chat.json                # Chat settings
│       ├── tablist.json             # Tablist customization
│       ├── spawn.json               # Spawn settings
│       ├── language/                # Language files
│       ├── templates/               # Configuration templates
│       └── security/                # Security configurations
├── neoessentials/
│   ├── data/
│   ├── homes/
│   ├── warps/
│   └── logs/
└── logs/
    └── latest.log
```

## ⚙️ Initial Configuration

### Basic Setup
1. Edit `config/neoessentials-general.toml`:
   ```toml
   [general]
   enableFeatures = true
   defaultLanguage = "en_US"
   
   [server]
   serverName = "Your Server Name"
   maxHomes = 5
   enableWarps = true
   ```

2. Configure permissions in your permission system or use NeoEssentials' built-in permissions

### Essential Configurations
```toml
# neoessentials-general.toml
[features]
essentialCommands = true
teleportation = true
notifications = true
security = true

[limits]
maxHomesDefault = 5
maxHomesVIP = 10
maxHomesStaff = 20

[teleportation]
teleportDelay = 3
teleportCooldown = 30
enableBack = true
```

```toml
# neoessentials-gui.toml - GUI system configuration
[layout]
inventory_rows = 6
menu_title_format = "&6&l{menu_name} &7(Page {page}/{max_page})"
enable_menu_sounds = true

[colors]
primary_color = "&6"
secondary_color = "&e"
accent_color = "&a"

[shop]
shop_enable_categories = true
shop_items_per_page = 45
shop_show_prices = true

[kits]
kit_show_previews = true
kit_show_cooldowns = true
kit_show_costs = true
```

## 🔑 Permissions Setup

### Using NeoEssentials Permissions
NeoEssentials includes a built-in permission system:

```bash
# Give a player basic permissions
/permissions user <player> group set default

# Create custom groups
/permissions group create vip "&6[VIP] " 100
/permissions group permission add vip essentials.fly
/permissions group permission add vip essentials.heal

# Basic command permissions
/permissions group permission add default neoessentials.commands.home
/permissions group permission add default neoessentials.commands.sethome
/permissions group permission add default neoessentials.commands.warp
/permissions group permission add default neoessentials.commands.kit
```

### Using External Permission Mods
NeoEssentials is compatible with:
- LuckPerms
- Other NeoForge permission mods

## 🧪 Testing Installation

### Verify Commands Work
Test these commands to ensure proper installation:

```bash
# Basic commands
/heal
/feed
/fly

# GUI system commands
/shop           # Open shop interface
/kits           # Open kits menu
/stats          # View statistics
/gui theme list # List available themes

# Admin commands (requires permissions)
/gamemode creative
/time set day
/weather clear
/admin          # Open admin GUI panel

# System commands
/neoessentials info
/neoessentials reload
/neoessentials gui reload  # Reload GUI configurations
```

### Check Console Output
Look for these messages in your server console:
```
[INFO] NeoEssentials: Successfully loaded
[INFO] NeoEssentials: Registered 23 essential commands
[INFO] NeoEssentials: Bossbar system initialized
[INFO] NeoEssentials: Configuration system initialized
[INFO] NeoEssentials: All modules loaded successfully  
[INFO] NeoEssentials: Security framework loaded
```

## 🚨 Troubleshooting

### Common Issues

#### "Command not found" errors
**Problem**: Commands like `/heal` don't work
**Solution**: 
1. Check if NeoEssentials is in the mods folder
2. Verify NeoForge version compatibility
3. Check console for loading errors

#### Config files not generating
**Problem**: No config files created on startup
**Solution**:
1. Ensure server has write permissions
2. Check if config folder exists
3. Restart server after placing mod

#### Permission errors
**Problem**: Players can't use commands
**Solution**:
1. Set up permission system
2. Grant appropriate permissions
3. Check permission configuration

### Console Error Messages

#### ModuleNotFound errors
```
java.lang.NoClassDefFoundError: com/zerog/neoessentials/...
```
**Solution**: Update to compatible NeoForge version

#### Configuration errors
```
Failed to load configuration file
```
**Solution**: 
1. Delete corrupted config files
2. Restart server to regenerate defaults
3. Manually edit configuration if needed

## 🔄 Updating NeoEssentials

### Update Process
1. **Backup** your server and configuration files
2. **Stop** your Minecraft server
3. **Replace** the old jar file with the new version
4. **Start** the server
5. **Check** for any configuration updates needed

### Migration Notes
- Configuration files are usually compatible between versions
- New features may require additional configuration
- Check the changelog for breaking changes

## 🔒 Security Considerations

### File Permissions
Ensure your server has appropriate file permissions:
```bash
# Linux/macOS
chmod 644 mods/neoessentials-*.jar
chmod -R 755 config/neoessentials/
chmod -R 755 neoessentials/
```

### Network Security
- Configure firewall rules for your server
- Use strong passwords for admin accounts
- Regularly update your server software

## 📞 Getting Help

### Before Asking for Help
1. Check this documentation
2. Search existing GitHub issues
3. Verify your setup meets requirements

### Where to Get Support
- **GitHub Issues**: Technical problems and bugs
- **GitHub Discussions**: General questions and feature requests
- **Server Console Logs**: Include relevant error messages

### What to Include in Support Requests
- NeoEssentials version
- NeoForge version
- Minecraft version
- Full error messages from console
- Relevant configuration files

---

**Next Steps**: [Configuration Guide](Configuration) | [Quick Start](Quick-Start)

*Last Updated: August 6, 2025*
