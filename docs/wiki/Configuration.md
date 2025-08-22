
# NeoEssentials Configuration Guide

This guide documents only the configuration files, options, and features that are actually present in the NeoEssentials mod. All examples and sections below are based on the real codebase and config structure.

---

## 📁 Configuration Files & Structure

**Config Files:**
- `main.json` — Global mod settings
- `homes.json` — Home system
- `kits.json` — Kits system
- `warps.json` — Warp system
- `moderation.json` — Moderation (ban, kick, mute, ignore)
- `messaging.json` — Messaging/chat settings
- `chat.json` — Chat formatting & rules
- `tablist.json` — Tablist customization
- `spawn.json` — Spawn system
- `animations.json` — Animation system (auto-generated)

**Directory Layout:**
```
config/neoessentials/
├── main.json
├── homes.json
├── kits.json
├── warps.json
├── moderation.json
├── messaging.json
├── chat.json
├── tablist.json
├── spawn.json
├── animations.json
├── backup/            # Timestamped config backups
└── languages/         # Language files (localization)
```

**Data Storage:**
```
neoessentials/
├── playerdata/        # Player persistent data
├── homes/             # Home locations
├── warps/             # Warp locations
├── kits/              # Kit usage data
├── permissions/       # Permission cache
├── shops/             # Shop data
├── backups/           # Data backups
└── ...                # Other runtime data
```

---

## ⚙️ Main Configuration (`main.json`)

Controls global mod settings, feature toggles, language, debug, and performance options.

```jsonc
{
    "enabled": true,
    "defaultLanguage": "en_US",
    "debugMode": false,
    "serverName": "My Minecraft Server",
    "checkUpdates": true,
    "features": {
        "essentialCommands": true,
        "teleportation": true,
        "bossbarSystem": true,
        "notifications": true,
        "security": true,
        "placeholders": true,
        "economy": false,
        "guiSystem": true
    },
    "performance": {
        "maxConcurrentTeleports": 5,
        "commandTimeout": 30,
        "performanceMonitoring": true,
        "slowCommandThreshold": 1000
    }
}
```

---

## 🏠 Teleportation (`homes.json`, `warps.json`, `back.json`)

**Home System:**
```jsonc
{
    "enabled": true,
    "limits": { "default": 3, "vip": 5, "moderator": 10, "admin": 20 },
    "teleport": {
        "teleportDelay": 3,
        "teleportCooldown": 30,
        "teleportWarmup": true,
        "cancelOnDamage": true,
        "crossDimensional": true
    },
    "restrictions": {
        "disabledWorlds": [],
        "allowNether": true,
        "allowEnd": true,
        "minimumDistance": 0
    }
}
```

**Warp System:**
```jsonc
{
    "enabled": true,
    "permissions": {
        "playerWarps": false,
        "staffWarps": true
    },
    "teleport": {
        "teleportDelay": 0,
        "teleportCooldown": 10
    },
    "enableCategories": true,
    "restrictions": {
        "maxWarpsPerCategory": 50,
        "disabledWorlds": []
    }
}
```

**Back System:**
```jsonc
{
    "enabled": true,
    "maxStoredLocations": 5,
    "storeDeathLocations": true,
    "storeTeleportLocations": true,
    "cooldown": 60,
    "crossDimensional": true
}
```

---

## 🎮 Command Settings (`main.json`, `commands.json`)

Controls command aliases, cooldowns, limits, and restrictions. All commands use EssentialsX-style permission nodes and config-driven formatting.

```jsonc
{
    "global": {
        "prefix": "",
        "enableAliases": true,
        "logCommands": true
    },
    "cooldowns": {
        "heal": 30,
        "feed": 30,
        "fly": 0,
        "god": 0,
        "vanish": 0,
        "speed": 0,
        "repair": 60,
        "give": 0
    },
    "limits": {
        "maxSpeed": 10.0,
        "maxGiveAmount": 2304,
        "maxRepairsPerMinute": 10
    },
    "restrictions": {
        "worldBans": {},
        "gameModeBans": {}
    }
}
```

---

## 🎨 Bossbar System

- Bossbar system is enabled/disabled via `main.json` (`bossbarSystem: true/false`).
- Bossbar templates are managed in-memory; config file loading is not currently implemented.
- All bossbar messages support color codes and placeholders.

---

## 🔔 Notifications (`notifications.json`)

Configure notification channels, Discord/email integration, and event triggers.

```jsonc
{
    "enabled": true,
    "defaultChannels": ["console", "log"],
    "discord": {
        "enabled": false,
        "webhookUrl": "",
        "botName": "NeoEssentials",
        "botAvatar": "",
        "richEmbeds": true
    },
    "email": {
        "enabled": false,
        "smtpHost": "",
        "smtpPort": 587,
        "smtpUsername": "",
        "smtpPassword": "",
        "smtpTLS": true,
        "recipients": []
    },
    "events": {
        "playerJoin": true,
        "playerLeave": true,
        "serverStart": true,
        "serverStop": true,
        "errors": true
    }
}
```

---

## 🔒 Security (`security.json`)

Controls threat detection, IP monitoring, player analysis, and staff notifications.

```jsonc
{
    "enabled": true,
    "securityLevel": "MEDIUM",
    "threatDetection": true,
    "ipMonitoring": true,
    "behaviorAnalysis": true,
    "thresholds": {
        "maxFailedLogins": 5,
        "maxCommandsPerMinute": 60,
        "maxChatPerMinute": 20
    },
    "responses": {
        "autoKickOnHighThreat": false,
        "autoBanOnCriticalThreat": false,
        "notifyStaff": true
    },
    "whitelist": {
        "whitelistedIPs": ["127.0.0.1"],
        "whitelistedPlayers": []
    }
}
```

---

## 🌍 Language System

Language files are stored in `config/neoessentials/languages/` and support multi-language, auto-detect, hot-reload, and fallback.

---

## 🎯 Placeholders

Placeholders are managed by the PlaceholderManager and are available in messages, bossbars, notifications, etc.

---

## 📊 Performance (`main.json`)

Performance monitoring is enabled/disabled via `main.json` (`performanceMonitoring: true/false`).

---

## 🛡️ Permissions (`permissions.json`)

Integrates with LuckPerms or built-in system. All permission nodes follow EssentialsX-style and are documented in [Permissions](Permissions).

---

## 📈 Data Storage (`main.json`)

Controls persistent storage type, backup, and auto-save. All player and runtime data is stored in `neoessentials/`.

---

## 🔧 Advanced (`main.json`)

Feature flags for experimental/beta/dev features and mod compatibility.

---

##  Reloading & Validation

Most config changes can be reloaded live:
```bash
/neoessentials reload
```
Some changes (storage type, database, core features, performance) require a server restart.

Validate config syntax and settings:
```bash
/neoessentials config validate
```

---

## 🛠️ Troubleshooting

- Validate JSON syntax
- Check file permissions
- Review server logs for errors
- Ensure feature is enabled
- Check permissions
- Verify dependencies

---

**Related Docs:** [Installation](Installation) | [Essential Commands](Essential-Commands) | [Permissions](Permissions)

*Last Updated: August 22, 2025*
```toml
[general]
# Enable or disable the entire mod
enabled = true

# Default language for messages
defaultLanguage = "en_US"

# Enable debug mode for troubleshooting
debugMode = false

# Server display name
serverName = "My Minecraft Server"

# Enable automatic updates check
checkUpdates = true

[features]
# Enable essential commands (/heal, /feed, etc.)
essentialCommands = true

# Enable teleportation system (/home, /warp, etc.)
teleportation = true

# Enable bossbar system
bossbarSystem = true

# Enable notification system
notifications = true

# Enable security features
security = true

# Enable placeholder system
placeholders = true

# Enable economy features (if implemented)
economy = false

# Enable GUI system
guiSystem = true

[performance]
# Maximum concurrent teleportations
maxConcurrentTeleports = 5

# Command execution timeout (seconds)
commandTimeout = 30

# Enable performance monitoring
performanceMonitoring = true

# Log slow commands (threshold in milliseconds)
slowCommandThreshold = 1000
```

## 🏠 Teleportation Configuration

### Home System Configuration

```toml
[homes]
# Enable home system
enabled = true

# Maximum homes per permission group
[homes.limits]
default = 3
vip = 5
moderator = 10
admin = 20

# Teleportation settings
[homes.teleport]
# Delay before teleportation (seconds)
teleportDelay = 3

# Cooldown between teleports (seconds)
teleportCooldown = 30

# Enable teleport warmup (player must stand still)
teleportWarmup = true

# Cancel teleport if player takes damage
cancelOnDamage = true

# Enable cross-dimensional teleports
crossDimensional = true

[homes.restrictions]
# Worlds where homes are disabled
disabledWorlds = []

# Enable homes in Nether
allowNether = true

# Enable homes in End
allowEnd = true

# Minimum distance between homes
minimumDistance = 0
```

### Warp System Configuration

```toml
[warps]
# Enable warp system
enabled = true

# Who can create warps
[warps.permissions]
# Allow players to create personal warps
playerWarps = false

# Allow staff to create server warps
staffWarps = true

[warps.teleport]
# Warp teleport delay (seconds)
teleportDelay = 0

# Warp teleport cooldown (seconds)
teleportCooldown = 10

# Enable warp categories
enableCategories = true

[warps.restrictions]
# Maximum warps per category
maxWarpsPerCategory = 50

# Disabled worlds for warps
disabledWorlds = []
```

### Back System Configuration

```toml
[back]
# Enable /back command
enabled = true

# Maximum stored locations per player
maxStoredLocations = 5

# Store death locations
storeDeathLocations = true

# Store teleport locations
storeTeleportLocations = true

# Back command cooldown (seconds)
cooldown = 60

# Cross-dimensional back teleports
crossDimensional = true
```

## 🎮 Command Configuration

### Essential Commands Settings

```toml
[commands]
# Global command settings
[commands.global]
# Command prefix (empty for default)
prefix = ""

# Enable command aliases
enableAliases = true

# Log all command usage
logCommands = true

# Command cooldowns (seconds)
[commands.cooldowns]
heal = 30
feed = 30
fly = 0
god = 0
vanish = 0
speed = 0
repair = 60
give = 0

# Command limits
[commands.limits]
# Maximum speed multiplier
maxSpeed = 10.0

# Maximum items given at once
maxGiveAmount = 2304

# Maximum repair operations per minute
maxRepairsPerMinute = 10

[commands.restrictions]
# Commands disabled in specific worlds
[commands.restrictions.worldBans]
# Example: heal = ["nether", "end"]

# Commands restricted to specific game modes
[commands.restrictions.gameModeBans]
# Example: fly = ["survival"]
```

## 🎨 Bossbar Configuration

### Bossbar System Settings

```toml
[bossbar]
# Enable bossbar system
enabled = true

# Default bossbar duration (seconds)
defaultDuration = 10

# Maximum concurrent bossbars per player
maxPerPlayer = 3

# Enable bossbar templates
enableTemplates = true

# Template definitions
[bossbar.templates.welcome]
text = "Welcome to {server_name}!"
color = "GREEN"
style = "SOLID"
duration = 5

[bossbar.templates.serverinfo]
text = "Players: {server_players}/{server_max_players} | TPS: {server_tps}"
color = "BLUE"
style = "SEGMENTED_10"
duration = 15
updateInterval = 1

[bossbar.templates.event]
text = "Server Event: {event_name}"
color = "YELLOW"
style = "SOLID"
duration = 30

[bossbar.templates.warning]
text = "Warning: {warning_message}"
color = "RED"
style = "SOLID"
duration = 8

[bossbar.templates.progress]
text = "Progress: {progress_text}"
color = "PURPLE"
style = "SEGMENTED_20"
duration = 60
```

## 🔔 Notification Configuration

### Notification System Settings

```toml
[notifications]
# Enable notification system
enabled = true

# Default notification channels
defaultChannels = ["console", "log"]

# Enable Discord notifications
discord = false

# Enable email notifications
email = false

[notifications.discord]
# Discord webhook URL
webhookUrl = ""

# Bot username
botName = "NeoEssentials"

# Bot avatar URL
botAvatar = ""

# Enable rich embeds
richEmbeds = true

[notifications.email]
# SMTP server settings
smtpHost = ""
smtpPort = 587
smtpUsername = ""
smtpPassword = ""
smtpTLS = true

# Email recipients
recipients = []

[notifications.events]
# Notify on player join
playerJoin = true

# Notify on player leave
playerLeave = true

# Notify on server start
serverStart = true

# Notify on server stop
serverStop = true

# Notify on errors
errors = true
```

## 🔒 Security Configuration

### Security System Settings

```toml
[security]
# Enable security system
enabled = true

# Security level (LOW, MEDIUM, HIGH, CRITICAL)
securityLevel = "MEDIUM"

# Enable threat detection
threatDetection = true

# Enable IP monitoring
ipMonitoring = true

# Enable player behavior analysis
behaviorAnalysis = true

[security.thresholds]
# Failed login attempts before flagging
maxFailedLogins = 5

# Commands per minute before rate limiting
maxCommandsPerMinute = 60

# Chat messages per minute before flagging
maxChatPerMinute = 20

[security.responses]
# Auto-kick on high threat level
autoKickOnHighThreat = false

# Auto-ban on critical threat level
autoBanOnCriticalThreat = false

# Notify staff of security events
notifyStaff = true

[security.whitelist]
# Whitelisted IP addresses (never flagged)
whitelistedIPs = ["127.0.0.1"]

# Whitelisted players (never flagged)
whitelistedPlayers = []
```

## 🌍 Language Configuration

### Language System Settings

```toml
[language]
# Enable multi-language support
enabled = true

# Default language
defaultLanguage = "en_US"

# Available languages
availableLanguages = ["en_US", "es_ES", "fr_FR", "de_DE"]

# Auto-detect player language from client
autoDetect = true

# Enable hot-reload of language files
hotReload = true

# Language file format (YAML or JSON)
format = "YAML"

[language.fallback]
# Use default language if translation missing
useDefault = true

# Show missing translation keys
showMissingKeys = false
```

## 🎯 Placeholder Configuration

### Placeholder System Settings

```toml
[placeholders]
# Enable placeholder system
enabled = true

# Placeholder format ({} or %%)
format = "{}"

# Alternative format support
alternativeFormat = "%%"

# Enable custom placeholders
customPlaceholders = true

# Cache placeholder results (seconds)
cacheTime = 30

[placeholders.builtin]
# Enable built-in placeholders
player = true
server = true
world = true
time = true
performance = true
color = true
utility = true

[placeholders.performance]
# Update interval for performance placeholders (seconds)
updateInterval = 5

# Enable TPS monitoring
tpsMonitoring = true

# Enable memory monitoring
memoryMonitoring = true
```

## 📊 Performance Configuration

### Performance Monitoring Settings

```toml
[performance]
# Enable performance monitoring
enabled = true

# Monitoring interval (seconds)
monitoringInterval = 30

# Enable TPS monitoring
tpsMonitoring = true

# Enable memory monitoring
memoryMonitoring = true

# Enable command timing
commandTiming = true

[performance.alerts]
# Enable performance alerts
enabled = true

# Low TPS threshold
lowTPSThreshold = 15.0

# High memory usage threshold (percentage)
highMemoryThreshold = 85.0

# Alert cooldown (seconds)
alertCooldown = 300

[performance.optimization]
# Enable automatic optimization
autoOptimize = false

# Optimization interval (minutes)
optimizationInterval = 60

# Enable garbage collection hints
gcHints = true
```

## 🛡️ Permission Integration

### Permission System Configuration

```toml
[permissions]
# Use built-in permission system
useBuiltinPermissions = true

# Permission system integration
[permissions.integration]
# Integrate with LuckPerms
luckPerms = false

# Integrate with other permission plugins
otherPlugins = []

[permissions.builtin]
# Enable built-in groups
enableGroups = true

# Default group for new players
defaultGroup = "default"

# Enable temporary permissions
temporaryPermissions = true

# Permission cache time (seconds)
cacheTime = 300
```

## 📈 Data Storage Configuration

### Storage Settings

```toml
[storage]
# Storage type (FILE, DATABASE)
type = "FILE"

# Data directory
dataDirectory = "neoessentials"

# Auto-save interval (minutes)
autoSaveInterval = 5

# Enable data backup
enableBackup = true

# Backup interval (hours)
backupInterval = 24

# Maximum backup files to keep
maxBackups = 7

[storage.database]
# Database connection settings (if using DATABASE storage)
url = ""
driver = "mysql"
username = ""
password = ""
maxConnections = 10
```

## 🔧 Advanced Configuration

### Feature Flags

```toml
[advanced]
# Enable experimental features
experimentalFeatures = false

# Enable beta features
betaFeatures = false

# Enable developer mode
developerMode = false

[advanced.compatibility]
# Enable compatibility mode with other mods
compatibilityMode = true

# Specific mod compatibility
[advanced.compatibility.mods]
# Example mod compatibility settings
# otherMod = true
```

## 🚀 Configuration Best Practices

### Performance Optimization
1. **Adjust monitoring intervals** based on server load
2. **Enable caching** for frequently accessed data
3. **Limit concurrent operations** to prevent lag
4. **Use appropriate storage type** for your setup

### Security Hardening
1. **Enable security features** for public servers
2. **Configure appropriate thresholds** for your player base
3. **Regularly review security logs**
4. **Keep whitelists updated**

### User Experience
1. **Set reasonable cooldowns** to prevent spam
2. **Configure appropriate limits** for player actions
3. **Enable helpful features** like auto-detection
4. **Customize messages** for your community

## 🔄 Configuration Reloading

### Live Reload
Most configuration changes can be applied without restarting the server:

```bash
/neoessentials reload
```

### Restart Required
Some changes require a server restart:
- Storage type changes
- Database connection settings
- Core feature enablement
- Performance monitoring changes

## 🛠️ Troubleshooting Configuration

### Common Issues

#### Configuration Not Loading
1. Check TOML syntax with a validator
2. Verify file permissions
3. Check server logs for parsing errors

#### Features Not Working
1. Verify feature is enabled in configuration
2. Check permission settings
3. Ensure all required dependencies are met

#### Performance Issues
1. Adjust monitoring intervals
2. Reduce cache times if memory is limited
3. Disable unnecessary features

### Validation
Use the built-in configuration validation:

```bash
/neoessentials config validate
```

---

**Related Documentation**: [Installation](Installation) | [Essential Commands](Essential-Commands) | [Permissions](Permissions)

*Last Updated: August 3, 2025*
