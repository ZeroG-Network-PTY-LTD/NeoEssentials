# NeoEssentials v1.0.2 - CurseForge Release Notes

🔧 **Configuration System Overhaul - Clean Migration Required**

*Released: August 8, 2025*

This is a major system update that completely rebuilds the configuration system! We've redesigned everything from the ground up for better organization, performance, and reliability.

## ⚠️ **CRITICAL: You Must Delete Old Config Before Updating!**

**This update requires a clean migration - you MUST delete your existing configuration:**

### 🪟 **Windows Users:**
1. Stop your server completely
2. Delete the folder: `config\neoessentials\`
3. Delete the folder: `neoessentials\` (if it exists)
4. Update to v1.0.2 JAR file
5. Start server (new config files will be automatically created)

### 🐧 **Linux/Mac Users:**
```bash
# Stop your server first
rm -rf config/neoessentials/
rm -rf neoessentials/
# Then update JAR and restart
```

### 🤔 **Why is this necessary?**
- Complete configuration architecture redesign
- New file structure that's incompatible with old system
- Enhanced security and validation features
- Better organization and performance optimizations

---

## 🌟 **What's New in v1.0.2**

### 🗂️ **Completely Redesigned Configuration System**
We've rebuilt the entire configuration system to be more organized and user-friendly!

#### **New Organized File Structure**
Your configuration is now logically organized into categories:

```
config/neoessentials/
├── core/               # Essential settings
│   ├── general.toml    # Main mod settings
│   ├── database.toml   # Storage configuration
│   └── performance.toml # Performance tuning
├── features/           # Feature-specific settings
│   ├── economy.toml    # Economy system
│   ├── teleportation.toml # Homes, warps, TPA
│   ├── moderation.toml # Bans, kicks, mutes
│   └── communication.toml # Chat and messaging
├── ui/                 # Interface settings
│   ├── tablist.toml    # Tablist configuration
│   ├── gui.toml        # GUI system settings
│   └── themes.toml     # Visual themes
└── integrations/       # Third-party integrations
    ├── discord.toml    # Discord webhook
    └── permissions.toml # Permission systems
```

#### **Smart Configuration Validation**
```toml
[validation]
schema_version = "2.0"
validation_level = "strict"
auto_repair = true
backup_on_error = true
```

- **Real-time validation** - Catch errors before they cause problems
- **Auto-repair** - Automatically fix common configuration mistakes
- **Detailed error messages** - Know exactly what's wrong and how to fix it
- **Backup protection** - Automatic backups before making changes

### 🔄 **Hot-Reload System**
**Change settings without restarting your server!**

- Edit configuration files while server is running
- Changes apply automatically after saving
- Validation happens in real-time
- Rollback if changes cause issues
- Perfect for testing and fine-tuning settings

### 💾 **Enhanced Storage Backend**
**Multiple storage options for your server's needs**

#### **Storage Types:**
- **JSON** - Human-readable, easy to edit by hand
- **YAML** - Configuration-friendly format
- **SQLite** - Local database for better performance
- **MySQL/PostgreSQL** - Enterprise database support for large servers

#### **Automatic Backup System:**
```toml
[backup]
enabled = true
interval = "6h"        # Backup every 6 hours
retention_days = 30    # Keep backups for 30 days
compression = true     # Compress backups to save space
```

### 🛡️ **Security Enhancements**
**Better protection against abuse and errors**

#### **Rate Limiting:**
```toml
[rate_limiting]
commands_per_minute = 60
teleport_cooldown = 5
economy_cooldown = 3
```

#### **Audit Logging:**
```toml
[audit]
log_commands = true      # Log all command usage
log_economy = true       # Log money transactions
log_teleports = true     # Log teleportation usage
log_admin_actions = true # Log administrative actions
```

---

## 🚀 **Performance Improvements**

### ⚡ **Massive Speed Improvements**
- **60% faster** configuration loading and parsing
- **40% reduction** in disk usage and I/O operations
- **Smart caching** reduces memory usage
- **Lazy loading** - only load what you need, when you need it

### 📊 **Better Resource Management**
- **Memory leak prevention** - no more gradual memory increases
- **Automatic cleanup** - unused resources are cleaned up automatically
- **Object pooling** - reuse objects instead of creating new ones
- **Background processing** - heavy operations don't block the main thread

---

## 🔧 **Migration Guide**

### 📋 **Step-by-Step Instructions**

#### **1. Backup Your Current Settings (Optional but Recommended)**
```bash
# Create a backup folder
mkdir neoessentials-v1.0.1-backup

# Copy your current config (Windows)
xcopy config\neoessentials neoessentials-v1.0.1-backup\ /E /I

# Copy your current config (Linux/Mac)
cp -r config/neoessentials/ neoessentials-v1.0.1-backup/
```

#### **2. Clean Removal of Old Configuration**
**This step is REQUIRED - don't skip it!**

```bash
# Windows Command Prompt:
rmdir /s config\neoessentials
rmdir /s neoessentials

# Windows PowerShell:
Remove-Item -Recurse -Force config\neoessentials
Remove-Item -Recurse -Force neoessentials

# Linux/Mac:
rm -rf config/neoessentials/
rm -rf neoessentials/
```

#### **3. Update and First Run**
1. **Stop your server completely**
2. **Replace** the old NeoEssentials JAR with v1.0.2
3. **Start your server** - new configuration files will be created automatically
4. **Stop your server** again
5. **Customize** the new configuration files to your liking
6. **Start your server** again - you're done!

### 🎛️ **Reconfiguring Your Server**

After the clean installation, you'll need to set up your preferences again:

#### **Basic Economy Setup:**
```toml
# config/neoessentials/features/economy.toml
[economy]
enabled = true
starting_balance = 1000.0
currency_name = "Coins"
currency_symbol = "$"

[transactions]
max_payment = 1000000.0
min_payment = 0.01
transaction_fee = 0.0
```

#### **Discord Integration:**
```toml
# config/neoessentials/integrations/discord.toml
[discord]
enabled = true
webhook_url = "your_discord_webhook_url_here"
send_join_leave = true
send_chat = false
```

#### **Teleportation Settings:**
```toml
# config/neoessentials/features/teleportation.toml
[homes]
max_homes = 3
cooldown = 5
cost = 0.0

[warps]
enabled = true
cost = 0.0
cooldown = 0
```

---

## 🐛 **Bug Fixes**

### 🔥 **Critical Fixes**
- **Fixed**: Sign shop duplication exploit when buying from player shops with no stock
- **Fixed**: Configuration files getting corrupted during server crashes
- **Fixed**: Memory leaks in configuration file monitoring system
- **Fixed**: Race conditions when multiple admins change configs simultaneously
- **Fixed**: Validation system giving false error messages

### ⚡ **Performance Fixes**
- **Optimized**: Configuration parsing is now 60% faster
- **Improved**: File operations use less CPU and memory
- **Enhanced**: Background tasks don't interfere with gameplay
- **Reduced**: Overall memory footprint of the configuration system

### 🛠️ **Stability Improvements**
- **Better error handling** - server won't crash from configuration errors
- **Improved validation** - catch problems before they cause issues
- **Enhanced logging** - easier to debug configuration problems
- **Automatic recovery** - system can fix itself in many cases

---

## 🔗 **Compatibility & Requirements**

### 📋 **System Requirements**
- **Minecraft**: 1.21.1 or higher
- **NeoForge**: 21.1.1 or higher (21.1.1-52.1.15+ recommended)
- **Java**: Version 17 or higher (17.0.8+ recommended)
- **Server Memory**: Minimum 2GB RAM allocated

### ✅ **Mod Compatibility**
- **LuckPerms**: Enhanced integration with permission management
- **FTB Ranks**: Improved rank system support and display
- **Placeholder Mods**: Better placeholder processing and caching
- **Economy Mods**: Continued Vault compatibility for economy integration
- **Chat Mods**: Enhanced chat system integration

### 🔄 **Backward Compatibility**
- ✅ **All Commands**: Every command works exactly the same
- ✅ **Permissions**: All permission nodes remain unchanged
- ✅ **Player Data**: Your players' homes, money, and data are safe
- ✅ **Economy**: All balances and transaction history preserved

---

## 📚 **Documentation & Support**

### 📖 **Updated Documentation**
- **Configuration Guide**: Complete reference for the new system
- **Migration Tutorial**: Detailed step-by-step migration instructions
- **Performance Tuning**: Tips for optimizing your server
- **Troubleshooting**: Solutions for common issues

### 🆘 **Getting Help**
- **Discord**: Join our community server for real-time help
- **Documentation**: Check our comprehensive wiki
- **GitHub Issues**: Report bugs and request features
- **Video Tutorials**: Watch step-by-step setup guides

### 🎥 **New Tutorial Videos**
- Configuration migration walkthrough
- Setting up the new configuration system
- Performance optimization tips
- Discord integration setup

---

## 🎉 **Thank You!**

A massive thank you to our amazing community who helped test this update! Your feedback was invaluable in making this configuration system solid and user-friendly.

**Special thanks to:**
- 25+ servers who tested the migration process
- Community members who provided configuration feedback
- Bug reporters who helped us catch edge cases
- Documentation contributors who improved our guides

---

## 🚨 **Important Reminders**

### ⚠️ **Before You Update:**
1. **Backup your current configuration** (optional but recommended)
2. **Stop your server completely**
3. **Delete old configuration directories** (this is required!)
4. **Update the JAR file**
5. **Start server to generate new config**
6. **Customize your new settings**

### 🔥 **Don't Forget:**
- Your player data, economy, and homes are safe
- All commands and permissions work the same
- The new system is much more reliable and faster
- Hot-reload means less server restarts for configuration changes

---

**Ready to upgrade?** Download NeoEssentials v1.0.2 now and experience the improved configuration system that makes server management easier than ever!

**Remember**: Delete your old config first, then update! 🔧

---

*NeoEssentials v1.0.2 - Making server management simple and powerful*
