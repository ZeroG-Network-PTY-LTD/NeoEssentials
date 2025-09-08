# Quick Start Guide

Get NeoEssentials up and running on your server in minutes! This guide covers the essential steps to install, configure, and start using NeoEssentials with your Minecraft server.

## 🚀 Prerequisites

Before installing NeoEssentials, ensure you have:

### Server Requirements
- **NeoForge**: Compatible NeoForge version for Minecraft 1.21.
- **Java Version**: Java 21 or higher
- **Server RAM**: Minimum 2GB (4GB+ recommended)
- **Operating System**: Windows, Linux, or macOS

### Check Your Setup

1. **Verify Java Version**:
   ```bash
   java -version
   ```
   Should show Java 21 or higher.

2. **Confirm NeoForge Installation**:
   Look for `neoforge-universal-*.jar` in your server directory.

3. **Check Server Performance**:
   Ensure your server runs smoothly without the mod first.

## ⬇️ Installation

### Step 1: Download NeoEssentials

1. Download the latest NeoEssentials JAR file from:
   - GitHub Releases: `neoessentials-1.0.2.jar`
   - CurseForge (when available)
   - Modrinth (when available)

### Step 2: Install the Mod

1. **Stop your server** if it's currently running
2. **Copy the JAR file** to your server's `mods/` folder:
   ```
   your-server/
   ├── mods/
   │   └── neoessentials-1.0.2.jar  ← Place here
   ├── world/
   ├── config/
   └── ...
   ```
3. **Start your server**

### Step 3: Verify Installation

1. **Check server logs** for successful loading:
   ```
   [INFO] Loading NeoEssentials v1.0.2
   [INFO] NeoEssentials: Essential commands system initialized
   [INFO] NeoEssentials: Configuration loaded successfully
   ```

2. **Test basic command**:
   ```bash
   /heal
   ```
   Should heal you to full health.

## ⚙️ Basic Configuration

### Initial Setup

When NeoEssentials first starts, it creates configuration files in the `config/` directory:

```
config/
├── neoessentials/
│   ├── animations.json          # Animation configurations
│   ├── bossbar.json            # Boss bar settings
│   ├── commands.json           # Command configurations
│   ├── config.json             # Core mod settings
│   ├── customPlaceholders.json # Custom placeholder definitions
│   ├── discord.json            # Discord integration
│   ├── permissions.json        # Permission system
│   ├── placeholders.json       # Placeholder configurations
│   ├── scoreboard.json         # Scoreboard settings
│   ├── settings.json           # General settings
│   ├── shops.json              # Shop system
│   ├── tablist.json            # Tab list customization
│   └── ...
├── main.json                    # Main configuration
├── permissions.json             # Permissions configuration
├── permissions.toml             # TOML permissions format
└── tablist.toml                 # TOML tablist format
```

### Essential Settings

Most basic settings are handled automatically, but you can customize key features by editing the configuration files after first startup.

## 👥 Setting Up Permissions

### Quick Permission Setup

NeoEssentials includes a comprehensive built-in permission system:

1. **View permission information**:
   ```bash
   /permissions info
   /permissions group list
   ```

2. **Set a player's group**:
   ```bash
   /permissions user PlayerName group set Admin
   /permissions user PlayerName group set VIP
   ```

3. **Add specific permissions**:
   ```bash
   /permissions user PlayerName permission add neoessentials.economy.admin
   /permissions group VIP permission add neoessentials.kit.vip
   ```

### Basic Permission Groups

The system includes four default permission groups:

- **Default**: Basic player permissions
- **VIP**: Enhanced player permissions  
- **Moderator**: Staff moderation permissions
- **Admin**: Full administrative access

**Promote a player to VIP**:
```bash
/permissions user PlayerName group set VIP
```

**Set group properties**:
```bash
/permissions group VIP prefix set "§6[VIP]"
/permissions group VIP suffix set " §6♦"
```

## 🎮 Essential Commands

### Player Commands

Try these basic commands to get started:

#### Health & Sustenance
```bash
/heal          # Restore full health
/feed          # Restore full hunger
```

#### Movement & Travel
```bash
/spawn         # Teleport to spawn
/back          # Return to previous location
/home          # Teleport to your home (after setting one)
/sethome       # Set your home location
```

#### Player Utilities
```bash
/god           # Toggle god mode
/vanish        # Toggle vanish mode
/fly           # Toggle flight mode
/speed <walk|fly> <speed>  # Set movement speed
```

#### Information
```bash
/whois <player>    # View player information
/playtime          # View your playtime statistics
```

### Admin Commands

For server administrators:

#### Player Management
```bash
/heal PlayerName        # Heal another player
/feed PlayerName        # Feed another player
/god PlayerName         # Toggle god mode for player
/gamemode <mode> PlayerName  # Change player's gamemode
```

#### Moderation
```bash
/kick <player> [reason]     # Kick a player
/ban <player> [reason]      # Ban a player
/tempban <player> <time> [reason]  # Temporary ban
/mute <player> <time> [reason]     # Mute a player
/jail <player> <time> [reason]     # Jail a player
```

#### Server Management
```bash
/time set day          # Set time to day
/weather clear         # Clear weather
/give <player> <item> [amount]  # Give items to player
```

## 🏠 Setting Up Homes & Warps

### Player Homes

1. **Set your first home**:
   ```bash
   /sethome
   ```

2. **Set a named home**:
   ```bash
   /sethome base
   /sethome farm
   ```

3. **Teleport to home**:
   ```bash
   /home          # Default home
   /home base     # Named home
   ```

4. **List your homes**:
   ```bash
   /homes
   ```

5. **Delete a home**:
   ```bash
   /delhome base
   ```

### Server Warps (Admin)

1. **Create spawn warp**:
   ```bash
   /setspawn
   ```

2. **Create public warps**:
   ```bash
   /setwarp mall
   /setwarp pvp
   /setwarp mining
   ```

3. **List all warps**:
   ```bash
   /warps
   ```

4. **Delete a warp**:
   ```bash
   /delwarp old_location
   ```

Players can then use `/warp mall` to teleport to warps.

## � Kit System

### Using Kits

1. **View available kits**:
   ```bash
   /kit list
   ```

2. **Get a kit**:
   ```bash
   /kit starter
   /kit tools
   ```

### Setting Up Kits (Admin)

Kits are configured in `config/neoessentials/kits.json`. The system supports:

- Custom item sets with enchantments
- Cooldown periods
- Cost requirements (if economy is enabled)
- Permission-based access

## 📊 Bossbar System

### Quick Bossbar Commands

1. **Show a bossbar to yourself**:
   ```bash
   /bossbar show welcome "Welcome to the server!" BLUE SOLID 10
   ```

2. **Broadcast a bossbar to all players**:
   ```bash
   /bossbar broadcast server_info "Players Online: 5" GREEN SOLID 30
   ```

3. **Hide a bossbar**:
   ```bash
   /bossbar hide welcome
   ```

### Bossbar Options

**Colors**: `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`
**Styles**: `SOLID`, `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12`, `SEGMENTED_20`

## 🔧 GUI System

NeoEssentials includes a comprehensive GUI system for enhanced server management:

### Available GUIs

1. **Shop Interface**:
   ```bash
   /shop
   ```
   Interactive shop with categories (weapons, armor, food, blocks, etc.)

2. **Server Menu**:
   ```bash
   /menu
   ```
   Main server information and navigation

3. **Player Statistics**:
   ```bash
   /stats [player]
   ```
   Comprehensive player statistics display

4. **Kit Selector**:
   ```bash
   /kits
   ```
   Visual kit browser with previews and availability

5. **Warp Browser**:
   ```bash
   /warps
   ```
   Teleport destination selector with descriptions

6. **Economy Interface**:
   ```bash
   /economy
   ```
   Economy management and information

### GUI Commands

```bash
/gui <type>              # Open specific GUI interface
/servergui               # Server information GUI
/tpmenu                  # Teleportation options menu
```

## 🌐 Testing Your Setup

### Verification Checklist

**✅ Basic Commands Working**:
- [ ] `/heal` restores health
- [ ] `/feed` restores hunger  
- [ ] `/spawn` teleports to spawn
- [ ] `/fly` toggles flight

**✅ GUI System Working**:
- [ ] `/shop` opens shop interface
- [ ] `/kits` displays available kits  
- [ ] `/stats` shows player statistics
- [ ] `/warps` opens warp browser
- [ ] `/menu` displays server menu

**✅ Permissions Working**:
- [ ] Default players can use basic commands
- [ ] Admins can use all commands
- [ ] Permission groups function correctly

**✅ Teleportation Working**:
- [ ] `/sethome` and `/home` work
- [ ] `/back` returns to previous location
- [ ] Warps can be created and used

**✅ Features Working**:
- [ ] Bossbar displays correctly
- [ ] Kit system functions
- [ ] Configuration loads without errors

### Common Test Commands

Run these to verify everything works:

```bash
# Test basic functionality
/heal
/feed
/god
/fly

# Test GUI system
/shop              # Open shop interface
/kits              # Open kits menu
/stats             # View player stats  
/warps             # Open warp browser
/menu              # Server information

# Test teleportation
/sethome test
/spawn
/home test
/back

# Test bossbar
/bossbar show test "Testing: Works!" GREEN SOLID 5

# Test kits (if available)
/kit list
/kit starter

# Test permissions (as admin)
/permissions info
/permissions user TestPlayer group set vip

# Test placeholders
/placeholder test {player_name}
/placeholder list
```

## 🛠️ Configuration Tips

### Performance Optimization

For better performance on larger servers, you can adjust settings in the configuration files after they're generated on first startup.

### Resource Management

The mod includes built-in performance monitoring and optimization features that work automatically.

### Basic Customization

Key customization options are available through the JSON configuration files in `config/neoessentials/`.

## 🚨 Troubleshooting

### Common Issues

#### Commands Not Working
**Problem**: Commands not recognized  
**Solution**: 
1. Check mod is installed correctly in `mods/` folder
2. Verify NeoForge and Java versions are compatible
3. Check server logs for errors
4. Try restarting server

#### Permission Errors
**Problem**: "You don't have permission"  
**Solution**:
1. Check your permission group: `/permissions info`
2. Set yourself as admin: `/permissions user YourName group set admin`
3. Give yourself permissions: `/permissions user YourName permission add neoessentials.*`

#### Configuration Issues
**Problem**: Settings not loading  
**Solution**:
1. Check that configuration files were created on first startup
2. Restart server after any manual configuration changes
3. Use `/neoessentials reload` to reload config
4. Check logs for configuration errors

#### GUI Not Opening
**Problem**: GUI commands don't work
**Solution**:
1. Verify the GUI system is enabled
2. Check that you have appropriate permissions
3. Try restarting the server
4. Check console for GUI-related errors

### Debug Commands

For troubleshooting:

```bash
# Check mod status
/neoessentials info

# Reload configuration
/neoessentials reload

# Test specific features
/placeholder test {player_name}
/permissions debug YourUsername neoessentials.heal
```

## 🎯 Next Steps

### Explore Advanced Features

Once you have the basics working:

1. **[Commands](Commands.md)** - Learn all available commands
2. **[Configuration](Configuration.md)** - Detailed configuration options
3. **[Permissions](Permissions.md)** - Advanced permission management
4. **[GUI System](GUI-System.md)** - Complete GUI customization
5. **[Bossbar System](Bossbar.md)** - Create advanced bossbar displays
6. **[Player Management](Player-Management.md)** - Comprehensive administration tools

### Customize for Your Server

- **Set up custom warps** for your server's unique locations
- **Create custom kits** for different player groups
- **Configure moderation settings** based on your server's needs
- **Set up economy integration** if using economy plugins
- **Customize GUI interfaces** to match your server's theme

### Community & Support

- **Report bugs or request features** through GitHub issues
- **Share your configurations** with other server administrators
- **Contribute to the project** by submitting improvements

---

**Congratulations!** 🎉 You now have NeoEssentials running on your server. Your players can enjoy essential commands, teleportation, GUI interfaces, and enhanced server features while you benefit from comprehensive administration tools.

**Related Documentation**: [Installation](Installation.md) | [Commands](Commands.md) | [Configuration](Configuration.md)

*Last Updated: August 9, 2025*
