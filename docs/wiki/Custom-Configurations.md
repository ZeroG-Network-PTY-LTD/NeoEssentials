# Custom Configurations

NeoEssentials provides extensive customization options through its configuration system. This guide explains how to create, modify, and optimize your server's configuration files for maximum flexibility.

## Configuration Overview

NeoEssentials uses multiple configuration files to organize settings by function. The main configuration files are located in the `config/neoessentials/` directory.

### Core Configuration Files

- `config.toml` - Main settings and module toggles
- `tablist.toml` - Tablist settings
- `economy.toml` - Economy system settings
- `homes.toml` - Home system settings
- `warps.toml` - Warp system settings
- `kits.toml` - Kit system settings
- `permissions.toml` - Permission settings
- `storage.toml` - Data storage settings

### Data Files

- `neoessentials/templates.json` - UI templates (headers, footers, messages)
- `neoessentials/animations.json` - Text animations
- `neoessentials/data/` - Player and server data

## Configuration Format

NeoEssentials uses TOML as its primary configuration format due to its readability and structure. JSON and YAML are supported for templates and animations.

### TOML Basics

```toml
# This is a comment

# Simple key-value
key = "value"

# Numbers
number = 100
decimal = 10.5
boolean = true

# Arrays
array = [ "item1", "item2", "item3" ]
numbers = [ 1, 2, 3 ]

# Tables (sections)
[section]
key1 = "value1"
key2 = "value2"

# Nested tables
[section.subsection]
key3 = "value3"

# Array of tables
[[items]]
name = "Item 1"
value = 10

[[items]]
name = "Item 2"
value = 20
```

## Main Configuration

The `config.toml` file contains core settings:

```toml
# NeoEssentials Configuration

# General settings
[general]
serverName = "My Awesome Server"
serverPrefix = "&6[&eServer&6] &r"
language = "en_US"
debugMode = false

# Module toggles
[modules]
economy = true
homes = true
warps = true
kits = true
tablist = true
permissions = false  # Set to false if using LuckPerms

# Performance settings
[performance]
cacheSize = 1024
asyncCommands = true
useOptimizedDataStorage = true
```

## Custom Messages

You can customize all messages in `messages.toml`:

```toml
[messages]
# General messages
prefix = "&6[&eNeoEssentials&6] &r"
noPermission = "{prefix}&cYou don't have permission to use this command!"
playerNotFound = "{prefix}&cPlayer not found!"
playerOnly = "{prefix}&cThis command can only be used by players!"

# Economy messages
[messages.economy]
balance = "{prefix}&aYour current balance: &e{balance} {currency_name}"
balanceOther = "{prefix}&a{target}'s balance: &e{balance} {currency_name}"
sent = "{prefix}&aYou sent &e{amount} {currency_name} &ato &e{target}&a!"
received = "{prefix}&aYou received &e{amount} {currency_name} &afrom &e{sender}&a!"
notEnough = "{prefix}&cYou don't have enough {currency_name}! You need &e{amount} &cmore."

# Customized for different modules...
```

## Creating Custom Data Structures

### Custom Ranks

Create custom ranks in `ranks.toml`:

```toml
[[ranks]]
id = "vip"
name = "VIP"
prefix = "&6[&eVIP&6] &r"
permissions = [
  "neoessentials.vip.*",
  "neoessentials.homes.3",
  "neoessentials.warps.all"
]
inheritance = ["default"]

[[ranks]]
id = "admin"
name = "Admin"
prefix = "&c[&4Admin&c] &r"
permissions = ["neoessentials.admin.*"]
inheritance = ["vip"]
```

### Custom Kit Types

Customize kit rewards in `kits.toml`:

```toml
[[kits]]
id = "starter"
name = "Starter Kit"
cooldown = "24h"
permission = "neoessentials.kit.starter"
items = [
  {id = "minecraft:stone_sword", count = 1},
  {id = "minecraft:stone_pickaxe", count = 1},
  {id = "minecraft:stone_axe", count = 1},
  {id = "minecraft:bread", count = 16},
  {id = "minecraft:torch", count = 32}
]
commands = [
  "tell {player} Enjoy your starter kit!",
  "effect give {player} minecraft:regeneration 30 1"
]
```

## Advanced Configuration Techniques

### Environment Variables

Use environment variables in configurations:

```toml
[database]
host = "${DB_HOST:localhost}"
port = "${DB_PORT:3306}"
database = "${DB_NAME:neoessentials}"
username = "${DB_USER:neouser}"
password = "${DB_PASS:password}"
```

### Configuration Inheritance

Create base configurations and extend them:

```toml
# In base.toml
[base]
setting1 = "value1"
setting2 = "value2"

# In specific.toml
[specific]
inherit = "base"
setting2 = "override"  # Overrides base value
setting3 = "value3"    # New setting
```

### Conditional Configuration

Apply settings based on conditions:

```toml
[feature]
enabled = true
settings = [
  {condition = "player.permission:neoessentials.feature.basic", value = "basic"},
  {condition = "player.permission:neoessentials.feature.advanced", value = "advanced"},
  {condition = "default", value = "disabled"}
]
```

## Multi-Server Configurations

For server networks, create server-specific overrides:

```toml
# In shared.toml (shared configuration)
[economy]
enabled = true
startingBalance = 100

# In survival.toml (server-specific override)
[economy]
moneyDropsEnabled = true
moneyPerKill = 5

# In creative.toml (server-specific override)
[economy]
moneyDropsEnabled = false
```

## Template Reference

Use templates in configurations:

```toml
[messages]
welcome = "template:welcome_message"
goodbye = "template:goodbye_message"
```

## Command Configuration

Customize command behavior in `commands.toml`:

```toml
[commands]
# Command aliases
aliases = [
  {command = "balance", aliases = ["bal", "money"]},
  {command = "teleport", aliases = ["tp", "tele"]},
  {command = "warp", aliases = ["warps", "goto"]}
]

# Command permissions
permissions = [
  {command = "kick", permission = "neoessentials.command.kick"},
  {command = "ban", permission = "neoessentials.command.ban"}
]

# Command cooldowns (in seconds)
cooldowns = [
  {command = "home", cooldown = 30},
  {command = "warp", cooldown = 10},
  {command = "kit", cooldown = 300}
]

# Command costs
costs = [
  {command = "heal", cost = 100},
  {command = "repair", cost = 50}
]
```

## Dynamic Configurations

Create configurations that change based on criteria:

```toml
# Dynamic home limits based on permissions
[[homes.limits]]
permission = "neoessentials.homes.vip"
limit = 5

[[homes.limits]]
permission = "neoessentials.homes.premium"
limit = 10

[[homes.limits]]
default = true
limit = 3
```

## Configuration Validation

Enable validation to prevent configuration errors:

```toml
[validation]
enabled = true
reportErrors = true
fixAutomatically = false
```

## Performance Optimization

Optimize configuration loading for performance:

```toml
[config.performance]
cacheConfigs = true
asyncConfigLoading = true
validateOnStartupOnly = true
optimizeReloads = true
```

## Configuration Management Commands

NeoEssentials provides commands for managing configurations:

```
/neoessentials:config reload
/neoessentials:config view <file>
/neoessentials:config set <file> <path> <value>
/neoessentials:config get <file> <path>
```

## Best Practices

1. **Keep Organized**: Group related settings in logical sections
2. **Use Comments**: Add descriptive comments for clarity
3. **Validate Changes**: Test configuration changes before applying in production
4. **Regular Backups**: Back up configurations before making significant changes
5. **Version Control**: Use version control for tracking configuration changes
6. **Documentation**: Document custom configurations and their purpose
7. **Performance**: Be mindful of performance impact from complex configurations

## Example Configurations

### Complete Economy Configuration

```toml
# Economy Configuration

[economy]
enabled = true
currencyName = "Coin"
currencyNamePlural = "Coins" 
currencySymbol = "$"
startingBalance = 100.0
maxBalance = 1000000.0
allowNegativeBalance = false
interestEnabled = true

[economy.interest]
interestRate = 0.01  # 1% interest
interestInterval = 3600  # In seconds (1 hour)
minimumBalance = 100
maximumInterest = 1000

[economy.transactions]
keepTransactionHistory = true
historyDuration = "30d"  # Keep 30 days of history
logTransactions = true
transactionTax = 0.05  # 5% tax on transactions
minimumTransactionAmount = 1

[economy.shops]
enablePlayerShops = true
shopTax = 0.10  # 10% tax on shop transactions
maxPlayerShops = 3
shopCreationCost = 1000
```

### Advanced Tablist Configuration

```toml
# Tablist Configuration

[tablist]
enabled = true
updateInterval = 20  # Ticks between updates
useHeader = true
useFooter = true
headerTemplate = "tablist.header"
footerTemplate = "tablist.footer"
headerAnimation = "server_title"
footerAnimation = "server_stats"

[tablist.playerList]
sortBy = "group"  # Options: name, group, playtime
groupPriority = ["admin", "mod", "vip", "default"]
showPing = true
showHealth = true
colorGroups = true

[tablist.bossbar]
enabled = true
useAnimation = true
animation = "server_announcements"
color = "YELLOW"  # Options: BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
style = "SOLID"  # Options: SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
```

## Common Customizations

### Chat Format

```toml
[chat]
enabled = true
format = "{prefix}{displayname}&f: {message}"
useRankColors = true
mentionHighlight = true
mentionColor = "&e"
clickableCommands = true
hoverText = "&7Rank: {group}\n&7Balance: {balance}\n&7Click to message"

[chat.groups]
admin = "&c[Admin] &r{displayname}&c: {message}"
mod = "&2[Mod] &r{displayname}&2: {message}"
vip = "&6[VIP] &r{displayname}&6: {message}"
default = "&7{displayname}&7: {message}"
```

### Teleportation Settings

```toml
[teleport]
enabled = true
delay = 3  # Seconds
cancelOnMove = true
cancelOnDamage = true
cooldown = 30  # Seconds
allowBack = true
backCooldown = 60  # Seconds
maximumDistance = 10000
crossWorldTeleport = true
restrictedWorlds = ["world_nether"]
safetyChecks = true
```

## Troubleshooting

### Common Issues

- **Invalid Syntax**: Verify TOML syntax is correct
- **Missing Sections**: Ensure required configuration sections exist
- **Value Type Errors**: Check that values match expected types (string, number, boolean)
- **Inheritance Issues**: Verify base configurations exist
- **File Location**: Confirm files are in the correct directories

### Configuration Debug Commands

For server admins:

```
/neoessentials:debug config
/neoessentials:validate config [file]
```

## Additional Resources

- [NeoEssentials Default Configurations](https://github.com/ZeroG-Network/NeoEssentials/tree/main/src/main/resources/default-config)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for configuration support
- [TOML Documentation](https://toml.io/en/) for format reference
