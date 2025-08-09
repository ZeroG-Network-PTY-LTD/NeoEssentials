# Economy System

NeoEssentials provides a basic economy system with player balances, secure transactions, and administrative tools. The system focuses on essential economy features with reliable money management.

## 💰 Basic Economy Features

### Player Commands
The economy system provides essential commands for players to manage their money:

```bash
/balance [player]             # Check balance (yours or another player's)
/bal [player]                 # Shorthand for balance  
/pay <player> <amount>        # Send money to another player
/baltop [limit]               # View the richest players leaderboard
/balancetop [limit]           # Alias for baltop
```

### Administrative Commands
Administrators have full control over the economy system:

```bash
/eco give <player> <amount>   # Give money to a player
/eco take <player> <amount>   # Take money from a player  
/eco set <player> <amount>    # Set a player's exact balance
```

**Core Features:**
- **Personal Balance Management** - Check your current balance or another player's
- **Player-to-Player Payments** - Secure money transfers between players with validation
- **Economy Leaderboards** - View top players by balance (customizable 1-50 entries)
- **Transaction Logging** - All transactions are logged with reasons for audit trails
- **Administrative Controls** - Complete admin management of player balances
- **Permission-Based Access** - Different permission levels for various commands

## ⚙️ Configuration

### Economy Configuration File
The economy system is configured through `config/neoessentials/economy.json`:

```json
{
  "enabled": true,
  "currencySymbol": "$",
  "currencyName": "dollar",
  "currencyNamePlural": "dollars",
  "currencyFormat": "#,##0.00",
  "startingBalance": 100.0,
  "minimumBalance": 0.0,
  "maxBalance": 10000000.0,
  "minimumPayAmount": 0.01,
  "maximumPayAmount": 10000.0,
  "logTransactions": true,
  "transferFeePercent": 0.0,
  "commandCosts": {
    "feed": 5.00,
    "repair": 15.00,
    "fly": 20.00,
    "kit": 25.00,
    "heal": 10.00,
    "god": 50.00
  },
  "vault": {
    "enabled": true,
    "economyName": "NeoEssentials Economy",
    "requireServer": false,
    "supportBanks": false
  },
  "shop": {
    "enabled": true,
    "allowSigns": true,
    "allowChestShops": true,
    "allowAdminShops": true,
    "signShopCost": 100.00,
    "chestShopCost": 250.00,
    "transactionFeePercent": 0.05,
    "minimumTransactionFee": 0.10,
    "maximumTransactionFee": 50.00,
    "maxShopsPerPlayer": 10,
    "enableShopTax": false,
    "dailyShopTaxPercent": 0.01
  },
  "bank": {
    "enabled": false,
    "allowLoans": false,
    "allowSavings": true,
    "interestRate": 0.02,
    "loanInterestRate": 0.05,
    "maximumLoanAmount": 10000.0,
    "minimumBalance": 100.0,
    "maxInterestPayout": 1000.0,
    "interestCalculationHours": 24
  },
  "cleanupInactiveAccounts": false,
  "messages": {
    "insufficientFunds": "&cYou don't have enough money! You need {0} but only have {1}.",
    "commandCostCharged": "&aYou were charged {0} for using /{1}.",
    "balanceUpdated": "&aYour balance has been updated to {0}.",
    "transactionComplete": "&aTransaction complete!",
    "economyDisabled": "&cThe economy system is disabled."
  }
}
```

### Currency Settings
- **Currency Symbol** - Display symbol for money (default: $)
- **Currency Names** - Singular and plural names for currency
- **Number Formatting** - How amounts are displayed (#,##0.00 for thousands separators)
- **Balance Limits** - Starting balance, minimum/maximum balances
- **Transaction Limits** - Minimum and maximum payment amounts

### Command Costs
You can configure costs for certain commands. When enabled, players will be charged money for using these commands:

```json
"commandCosts": {
  "feed": 5.00,      // Cost to use /feed
  "repair": 15.00,   // Cost to use /repair
  "fly": 20.00,      // Cost to use /fly
  "kit": 25.00,      // Cost to use /kit
  "heal": 10.00,     // Cost to use /heal
  "god": 50.00       // Cost to use /god
}
```

### Vault Integration
NeoEssentials includes Vault API compatibility for integration with other plugins:

```json
"vault": {
  "enabled": true,
  "economyName": "NeoEssentials Economy",
  "requireServer": false,
  "supportBanks": false
}
```

## 🔒 Security Features

### Transaction Validation
The economy system includes built-in security measures:

- **Balance Verification** - All transactions verify sufficient funds before processing
- **Self-Payment Prevention** - Players cannot pay themselves
- **Amount Limits** - Configurable minimum and maximum payment amounts
- **Permission Checks** - All commands require appropriate permissions
- **Audit Logging** - Complete transaction history with timestamps and reasons

### Permission Nodes
The economy system uses these permission nodes:

```
neoessentials.economy.balance       # Use /balance command
neoessentials.economy.balance.others # Check others' balances
neoessentials.economy.pay           # Use /pay command
neoessentials.economy.top           # Use /baltop command
neoessentials.economy.admin         # Admin economy commands
neoessentials.economy.give          # Use /eco give
neoessentials.economy.take          # Use /eco take
neoessentials.economy.set           # Use /eco set
```

## 🛠️ Usage Examples

### Checking Balances
```bash
/balance                      # Check your own balance
/bal                          # Shorthand version
/balance PlayerName           # Check another player's balance (requires permission)
```

### Making Payments
```bash
/pay Steve 100               # Pay Steve $100
/pay Alice 50.75             # Pay Alice $50.75
```

### Administrative Management
```bash
/eco give Steve 1000         # Give Steve $1000
/eco take Alice 500          # Take $500 from Alice
/eco set Bob 250             # Set Bob's balance to exactly $250
```

### Viewing Leaderboards
```bash
/baltop                      # Show top 10 richest players
/baltop 20                   # Show top 20 richest players
/balancetop 5                # Show top 5 richest players
```

## 🔧 Troubleshooting

### Common Issues

#### "You don't have enough money" errors
- Check the player's balance with `/balance`
- Verify the payment amount is within configured limits
- Ensure the player has sufficient funds for the transaction

#### Permission denied errors
- Verify the player has the required permission nodes
- Check that the economy system is enabled in the configuration
- Ensure proper permission group setup

#### Commands not working
- Confirm the economy system is enabled in `economy.json`
- Check that the server has properly loaded the configuration
- Verify there are no configuration syntax errors

### Administrative Commands for Debugging
```bash
/eco give <player> <amount>   # Test giving money
/eco set <player> 1000        # Reset a player's balance for testing
/baltop 1                     # Check if leaderboard system is working
```

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - General configuration setup
- **[Permissions](Permissions.md)** - Permission system setup
- **[Player Management](Player-Management.md)** - Managing player data

*Last Updated: January 15, 2025*
