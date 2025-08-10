# Configuration Guide

<<<<<<< HEAD
This comprehensive guide covers all configuration options available in NeoEssentials. Configuration files are located in the `config/neoessentials/` directory and use JSON format for easy editing and readability.
=======
This comprehensive guide covers all configuration options available in NeoEssentials. Configuration files are located in the `config/` directory and use TOML format.
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)

## 📁 Configuration Files Overview

### Main Configuration Files
<<<<<<< HEAD
- `main.json` - Core mod settings and general configuration
- `economy.json` - Economy system configuration
- `homes.json` - Home system settings
- `kits.json` - Kit system configuration
- `warps.json` - Warp system settings
- `moderation.json` - Moderation tools configuration
- `messaging.json` - Chat and messaging settings
- `chat.json` - Chat system configuration
- `tablist.json` - Tab list customization
- `spawn.json` - Spawn system settings
- `animations.json` - Animation system configuration (auto-generated)
=======
- `neoessentials-general.toml` - General mod settings
- `neoessentials-common.toml` - Common gameplay settings
- `neoessentials/language/` - Language files
- `neoessentials/templates/` - Bossbar and message templates
- `neoessentials/security/` - Security configuration
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)

### Configuration Directory Structure
```
<<<<<<< HEAD
config/neoessentials/
├── main.json                       # Core mod settings
├── economy.json                    # Economy system with balance management
├── homes.json                      # Home system configuration
├── kits.json                       # Kit system settings
├── warps.json                      # Warp system configuration
├── moderation.json                 # Moderation tools (ban, kick, mute)
├── messaging.json                  # Chat and messaging configuration
├── chat.json                       # Chat system settings
├── tablist.json                    # Tab list customization
├── spawn.json                      # Spawn system configuration
├── animations.json                 # Animation system (auto-generated)
├── README.md                       # Auto-generated configuration guide
├── templates/                      # Default configuration templates
│   ├── main.json                  # Template for main config
│   ├── economy.json               # Template for economy config
│   └── [all other templates]      # One template per config file
├── backup/                         # Automatic configuration backups
│   └── [timestamped backups]      # Automatic backups with timestamps
└── languages/                     # Language files for localization
    ├── en_US.properties
    ├── de_DE.properties
    └── [other languages]

neoessentials/
├── [player data files]            # Player-specific data storage
├── [economy data]                  # Economy system data
└── [other runtime data]            # Generated runtime data
=======
config/
├── neoessentials-general.toml
├── neoessentials-common.toml
└── neoessentials/
    ├── language/
    │   ├── en_US.yml
    │   ├── es_ES.yml
    │   └── fr_FR.yml
    ├── templates/
    │   ├── bossbar.yml
    │   └── messages.yml
    ├── security/
    │   └── security.toml
    └── features/
        ├── economy.toml
        ├── homes.toml
        └── warps.toml
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)
```

## ⚙️ General Configuration

### `main.json`

<<<<<<< HEAD
The main configuration file controlling core mod behavior:

```json
{
  "general": {
    "enabled": true,
    "language": "en_US",
    "debug_mode": false,
    "server_name": "My Minecraft Server",
    "server_description": "Welcome to our awesome server!",
    "update_notifications": true,
    "performance_monitoring": true
  },
  "features": {
    "economy": true,
    "homes": true,
    "warps": true,
    "kits": true,
    "moderation": false,
    "messaging": true,
    "chat": true,
    "tablist": true,
    "spawn": true,
    "animations": true
  },
  "security": {
    "validate_commands": true,
    "rate_limiting": true,
    "secure_storage": true,
    "audit_logging": true
  },
  "performance": {
    "async_operations": true,
    "cache_enabled": true,
    "optimize_packets": true,
    "hot_reload": true
  }
}
```

# 🔒 Security Features
securityFeatures = true
rateLimiting = true
commandFiltering = true
securityLogging = true

# 🌐 Internationalization
multiLanguageSupport = true
playerLanguagePreferences = true
dynamicLanguageSwitching = true

# 🔗 Integration Features
placeholderSupport = true
permissionIntegration = true
economyIntegration = false  # Set to true if using economy plugins

[performance]
# Async processing for heavy operations
enableAsyncProcessing = true

# Cache settings for better performance
cacheSize = 1000
cacheExpirationMinutes = 30

# Update intervals (in ticks, 20 ticks = 1 second)
generalUpdateInterval = 20
bossbarUpdateInterval = 20
guiUpdateInterval = 40

# Resource limits
maxConcurrentAsyncTasks = 10
maxMemoryUsagePercent = 15

[logging]
# Enable detailed logging
enableDetailedLogging = false

# Log levels: DEBUG, INFO, WARN, ERROR
logLevel = "INFO"

# Enable specific logging categories
commandLogging = true
securityLogging = true
performanceLogging = false
guiInteractionLogging = false

[compatibility]
# Permission system compatibility
preferredPermissionSystem = "auto"  # auto, luckperms, ftb_ranks, builtin

# Placeholder integration
enablePlaceholderAPI = true

# Economy plugin integration
economyProvider = "auto"  # auto, vault, builtin, none

[gui]
# 🎮 GUI System Configuration
defaultTheme = "default"  # default, dark, ocean, custom

# Enable GUI caching for better performance
enableGuiCaching = true
guiCacheSize = 100

# Default GUI sizes (inventory slots)
defaultShopSize = 54    # 6 rows
defaultKitsSize = 36    # 4 rows
defaultWarpsSize = 45   # 5 rows

# GUI update frequencies (in ticks)
shopUpdateInterval = 100     # Update shop every 5 seconds
statsUpdateInterval = 200    # Update stats every 10 seconds

# GUI sound effects
enableGuiSounds = true
guiClickSound = "minecraft:ui.button.click"
guiSuccessSound = "minecraft:entity.experience_orb.pickup"
guiErrorSound = "minecraft:block.note_block.bass"

[notifications]
# Message notification settings
enableActionBarMessages = true
enableSoundNotifications = true
enableParticleEffects = false

# Notification priorities
criticalNotificationDuration = 10
normalNotificationDuration = 5
infoNotificationDuration = 3

# Default sounds for notifications
successSound = "minecraft:entity.experience_orb.pickup"
errorSound = "minecraft:block.note_block.bass"
infoSound = "minecraft:ui.button.click"

[database]
# Data storage configuration
storageType = "json"  # json, sqlite, mysql

# Backup settings
enableAutoBackup = true
backupInterval = 6  # hours
maxBackupFiles = 10

# Database connection (for MySQL)
# mysql_host = "localhost"
# mysql_port = 3306
# mysql_database = "neoessentials"
# mysql_username = "username"
# mysql_password = "password"
```

## 🔧 Common Configuration

### `neoessentials-common.toml`

Core gameplay settings and feature toggles:

```toml
[commands]
# 🎮 Essential Commands
enableHeal = true
enableFeed = true
enableFly = true
enableGod = true
enableSpeed = true
enableRepair = true

# Teleportation Commands
enableTeleportation = true
enableHomes = true
enableWarps = true
enableBack = true
enableSpawn = true
enableRandomTeleport = true

# Admin Commands
enableGamemodeCommands = true
enableTimeCommands = true
enableWeatherCommands = true
enableVanish = true

# GUI Commands (NEW)
enableGuiCommands = true
enableShopCommand = true
enableKitsCommand = true
enableStatsCommand = true
enableAdminPanelCommand = true

[limits]
# Player limits
maxHomesDefault = 5
maxHomesVIP = 10
maxHomesStaff = 20

# Teleportation limits
maxWarpDistance = 10000
maxRandomTeleportDistance = 5000

# Command usage limits
maxCommandsPerMinute = 60
healCooldownSeconds = 30
feedCooldownSeconds = 30

# GUI interaction limits
maxGuiActionsPerSecond = 10
shopTransactionLimit = 1000000  # Maximum transaction value

[teleportation]
# Teleportation delays and cooldowns
teleportDelay = 3         # seconds
teleportCooldown = 30     # seconds
adminTeleportDelay = 0    # instant for admins

# Safety checks
enableSafetyCheck = true
maxTeleportHeight = 256
minTeleportHeight = 1

# Back command settings
enableBack = true
maxBackLocations = 10
backCooldown = 10

# Spawn settings
forceSpawnOnFirstJoin = true
spawnProtectionRadius = 10

# Random teleport settings
randomTeleportMinDistance = 1000
randomTeleportMaxDistance = 5000
randomTeleportMaxAttempts = 10

[permissions]
# Permission system settings
useBuiltinPermissions = true
defaultGroup = "default"
enableGroupPrefixes = true

# Default permission groups
[permissions.groups]
default = "neoessentials.player.*"
vip = "neoessentials.vip.*"
moderator = "neoessentials.moderator.*"
admin = "neoessentials.admin.*"

# GUI-specific permissions (NEW)
[permissions.gui]
shop_access = "neoessentials.gui.shop"
kits_access = "neoessentials.gui.kits"
stats_access = "neoessentials.gui.stats"
admin_panel = "neoessentials.gui.admin"
theme_change = "neoessentials.gui.theme.change"

[economy]
# Basic economy settings (if enabled)
enableEconomy = false
startingBalance = 1000.0
currencyName = "Coins"
currencySymbol = "$"

# GUI shop economy integration (NEW)
enableShopEconomy = false
shopTaxRate = 0.05  # 5% tax on transactions
maxShopPrice = 1000000

[messaging]
# Chat and message formatting
chatPrefix = "&8[&6NeoEssentials&8]&r"
useHexColors = true
enableClickableMessages = true

# Message display preferences
preferActionBar = true
enableSoundNotifications = true
enableTitleMessages = false

# Spam protection
enableSpamProtection = true
maxMessagesPerSecond = 3
spamTimeoutSeconds = 30

[gui_system]
# 🎮 GUI System Settings (NEW in v1.2+)
enabled = true

# Default theme for all players
defaultTheme = "default"

# Available themes
availableThemes = [
    "default",
    "dark", 
    "ocean",
    "forest",
    "nether",
    "end"
]

# GUI caching and performance
enableCaching = true
cacheRefreshInterval = 300  # 5 minutes
maxCachedGuis = 50

# Player GUI preferences
savePlayerThemes = true
savePlayerLayouts = true

# GUI animations
enableAnimations = true
animationSpeed = 500  # milliseconds

[bossbar]
# 📊 Bossbar System Settings
enabled = true
enableTemplates = true
enableAnimations = true

# Default bossbar settings
defaultColor = "GREEN"
defaultStyle = "SOLID"
defaultDuration = 10

# Bossbar limits
maxActiveBossbars = 5
maxBossbarLength = 64

# Animation settings
animationUpdateInterval = 1000  # 1 second
enableColorAnimations = true
enableTextAnimations = true

[security]
# 🔒 Basic Security Settings
enabled = true
enableLogging = true
enableRateLimiting = true

# Rate limiting
maxCommandsPerSecond = 5
rateLimitWindow = 10  # seconds

# Security monitoring
enablePlayerMonitoring = true
suspiciousActivityThreshold = 10
```
=======
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
>>>>>>> parent of 482ed14 (Implement SignShopData class for persistent storage of sign shop data, including serialization to/from JSON. Added BlockPosData and ItemStackData inner classes for handling position and item stack information.)

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
