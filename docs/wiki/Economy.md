
# NeoEssentials Economy System

This page documents only the economy features, commands, configuration options, and permission nodes that are actually present in NeoEssentials. No banks, loans, or unsupported shop types are described here.

---

## 💰 Economy Features

- Player balances (persistent)
- Player-to-player payments
- Economy leaderboard
- Admin commands for balance management
- Transaction logging

---

## ⚙️ Configuration

Economy is configured in `config/neoessentials/economy.json`.

```jsonc
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
    "economyName": "NeoEssentials Economy"
  },
  "messages": {
    "insufficientFunds": "&cYou don't have enough money! You need {0} but only have {1}.",
    "commandCostCharged": "&aYou were charged {0} for using /{1}.",
    "balanceUpdated": "&aYour balance has been updated to {0}.",
    "transactionComplete": "&aTransaction complete!",
    "economyDisabled": "&cThe economy system is disabled."
  }
}
```

---

## 📝 Player Commands

```bash
/balance [player]      # Check your own or another player's balance
/bal [player]          # Alias for /balance
/pay <player> <amount> # Pay another player
/baltop [limit]        # View top balances
```

---

## 🛠️ Admin Commands

```bash
/eco give <player> <amount> # Give money to a player
/eco take <player> <amount> # Take money from a player
/eco set <player> <amount>  # Set a player's balance
```

---

## 🔒 Permission Nodes

```
neoessentials.economy.balance         # Use /balance
neoessentials.economy.balance.others  # Check others' balances
neoessentials.economy.pay             # Use /pay
neoessentials.economy.top             # Use /baltop
neoessentials.economy.admin           # Admin economy commands
neoessentials.economy.give            # Use /eco give
neoessentials.economy.take            # Use /eco take
neoessentials.economy.set             # Use /eco set
```

---

## 🔧 Troubleshooting

- "You don't have enough money" errors: Check balance, payment amount, and config limits.
- Permission denied: Check permission nodes and config.
- Commands not working: Confirm economy is enabled and config is valid.

---

**Related Docs:** [Configuration](Configuration.md) | [Permissions](Permissions.md) | [Player Management](Player-Management.md)

*Last Updated: August 22, 2025*
