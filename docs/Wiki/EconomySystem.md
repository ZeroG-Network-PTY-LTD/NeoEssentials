# Economy System

## Overview
NeoEssentials provides a robust, configurable economy system for Minecraft servers. It supports player balances, payments, transaction logging, and integration with other economy mods.

## Core Manager
- **EconomyManager**: Handles all balance operations, activity tracking, atomic saves, backups, and cache optimization.
- Data stored in `balances.json` and `balances_activity.json`.

## Configuration Options
Managed via `ConfigManager` (formerly `EconomyConfig`). Key options:
- `startingBalance`: Initial balance for new players
- `currencySymbol`: Symbol used for currency
- `maxBalance`: Maximum allowed balance
- `taxPercentage`: Tax applied to transactions
- `allowNegativeBalances`: Allow negative balances
- `paytoggleDefault`: Default payment toggle state
- `maxTransferAmount`: Maximum amount per transfer
- `cleanupInactiveAccounts`: Enable cleanup of inactive accounts
- `inactiveAccountCleanupDays`: Days before inactive accounts are cleaned up
- `cacheMaximumSize`: Max cache size for balances
- `cacheExpireAfterAccessMinutes`: Cache expiration time

## Commands
- `/balance` — Check your balance
- `/pay <player> <amount>` — Send money to another player
- `/paytoggle` — Toggle receiving payments
- `/eco` — Admin economy commands
- `/baltop` — View top balances

## Features
- Transaction logging
- Player activity tracking
- Configurable via JSON and in-game commands
- Automatic backups on data version change
- Integration with other economy mods (via config)

## Example Config
```json
{
  "startingBalance": 1000,
  "currencySymbol": "$",
  "maxBalance": 1000000,
  "taxPercentage": 0.05,
  "allowNegativeBalances": false,
  "paytoggleDefault": true,
  "maxTransferAmount": 50000,
  "cleanupInactiveAccounts": true,
  "inactiveAccountCleanupDays": 30,
  "cacheMaximumSize": 1000,
  "cacheExpireAfterAccessMinutes": 60
}
```

---
For more details, see the main documentation or ask in the Discord support server.