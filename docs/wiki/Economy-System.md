# Economy System

The NeoEssentials economy system provides a comprehensive solution for managing player balances, transactions, and shop integration on your server.

## Overview

The economy system includes:

- Player balance management
- Multiple currency support
- Transaction history
- Shop integration
- API for other mods to interact with

## Configuration

The economy system is configured in `config/neoessentials/economy.toml`:

```toml
[economy]
# Enable the economy system
enabled = true

# Default balance for new players
startingBalance = 100.0

# Maximum balance allowed (set to -1 for unlimited)
maxBalance = 1000000.0

# Currency name (singular)
currencyName = "coin"

# Currency name (plural)
currencyNamePlural = "coins"

# Currency symbol
currencySymbol = "$"

# Format for displaying currency
currencyFormat = "%symbol%%amount%"

[economy.storage]
# Storage type: "json", "sqlite", or "mysql"
type = "json"

# Storage location (for JSON)
location = "data/neoessentials/economy"

# Database settings (for SQLite/MySQL)
database = "neoessentials_economy"
host = "localhost"
port = 3306
username = "root"
password = ""
```

## Commands

### Basic Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/balance` or `/bal` | Shows your balance | neoessentials.command.balance |
| `/balance <player>` | Shows another player's balance | neoessentials.command.balance.others |
| `/baltop` or `/balancetop` | Shows top balances | neoessentials.command.baltop |
| `/pay <player> <amount>` | Transfers money to another player | neoessentials.command.pay |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/eco give <player> <amount>` | Gives money to a player | neoessentials.command.eco.give |
| `/eco take <player> <amount>` | Takes money from a player | neoessentials.command.eco.take |
| `/eco set <player> <amount>` | Sets a player's balance | neoessentials.command.eco.set |
| `/eco reset <player>` | Resets a player's balance | neoessentials.command.eco.reset |
| `/eco reload` | Reloads economy configuration | neoessentials.command.eco.reload |

## Permissions

| Permission | Description |
|------------|-------------|
| `neoessentials.economy.use` | Allows using the economy system |
| `neoessentials.command.balance` | Allows checking own balance |
| `neoessentials.command.balance.others` | Allows checking others' balances |
| `neoessentials.command.pay` | Allows paying others |
| `neoessentials.command.baltop` | Allows viewing top balances |
| `neoessentials.command.eco.*` | Allows all admin economy commands |
| `neoessentials.economy.bypass.max` | Bypasses maximum balance limit |
| `neoessentials.economy.interest` | Receives interest payments |

## Transaction History

NeoEssentials keeps a record of all economy transactions. Admins can view transaction history:

```
/eco history <player> [page]
```

This shows a paginated list of recent transactions for the specified player.

## Multiple Currencies

NeoEssentials supports multiple currencies through its advanced currency system:

```toml
[economy.currencies]
[economy.currencies.gems]
name = "gem"
namePlural = "gems"
symbol = "G"
conversionRate = 100.0  # 1 gem = 100 coins
primaryColor = "#50C878"
```

Use currency-specific commands:

```
/bal gems
/pay <player> 10 gems
/eco give <player> 5 gems
```

## Interest System

NeoEssentials can automatically pay interest on player balances:

```toml
[economy.interest]
# Enable the interest system
enabled = true

# Interest rate (percentage)
rate = 0.5

# Interest interval (in minutes)
interval = 60

# Minimum balance required to earn interest
minimumBalance = 100.0

# Maximum interest payment per interval
maximumInterest = 1000.0
```

## Shop Integration

The economy system integrates with the shop system:

```
/shop create <name> <price>
/shop buy <item>
/shop sell <item>
```

See the [Shop System](Shop-System) guide for more information.

## API for Developers

Other mods can interact with the economy system through the NeoEssentials API:

```java
// Get a player's balance
double balance = NeoEssentials.getEconomyManager().getBalance(playerUUID);

// Modify a player's balance
boolean success = NeoEssentials.getEconomyManager().addBalance(playerUUID, amount);
```

See the [API Documentation](API-Documentation) for more information.

## Economy Events

NeoEssentials fires events for economy actions that other mods can listen to:

- `EconomyBalanceChangeEvent`
- `EconomyTransactionEvent`
- `EconomyAccountCreateEvent`

## Storage Options

The economy system supports three storage methods:

1. **JSON** (default): Simple file-based storage, good for small servers
2. **SQLite**: Local database storage, better performance for medium servers
3. **MySQL/MariaDB**: Remote database storage, optimal for large or multi-server networks

Configure the storage method in `economy.toml`.

## Migration

To migrate from another economy system:

1. Use `/eco import <source>` to import from supported economy mods
2. Or use the [Migration Tool](Economy-Migration) for manual data conversion

## Placeholders

The economy system provides these placeholders:

- `%balance%` - Player's balance
- `%balance_formatted%` - Formatted balance
- `%currency_symbol%` - Currency symbol
- `%currency_name%` - Currency name
- `%baltop_player_1%` - Top player name
- `%baltop_balance_1%` - Top player balance

## Troubleshooting

If you encounter issues with the economy system:

- Check that the economy system is enabled in the configuration
- Verify storage settings are correct and accessible
- Ensure players have the proper permissions
- Check the server logs for any error messages

For additional help, see the [Troubleshooting](Troubleshooting) guide or join our [Discord server](https://discord.gg/dUGAQF2Mga).
