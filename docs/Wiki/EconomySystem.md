# Economy System

> **Version:** 1.0.2.6 · **Config files:** `economy.json`, `config.json` → `economy` section

---

## Overview

NeoEssentials provides a full server economy with player balances, payments, admin tools, an async leaderboard, a sign-based ChestShop, and Vault API integration.

---

## Config (`economy.json`)

Note: `modules.economyEnabled` (in `config.json`) is the master on/off switch — `economy.json` itself has no `enabled` key.

| Key | Default | Description |
|---|---|---|
| `startingBalance` | `100.0` | Balance given to new players |
| `currencySymbol` | `"$"` | Symbol prepended to all amounts |
| `currencyName` | *(currency symbol)* | Full currency name (singular) |
| `currencyNamePlural` | *(currencyName)* | Full currency name (plural) |
| `maxBalance` | `999999999.99` | Maximum balance a player can hold |
| `allowNegativeBalances` | `false` | Allow balances below zero |
| `taxPercentage` | `0.0` | Tax applied to `/pay` transfers (as a percent, e.g. `5` = 5%) |
| `maxTransferAmount` | `10000.0` | Max single `/pay` amount (overridable per-player via LuckPerms meta) |
| `paytoggleDefault` | `true` | Whether players accept payments by default |
| `logTransactions` | `true` | Log transactions to `logs/neoessentials/transactions.log` |
| `transactionHistoryLimit` | `20` | Max entries kept per player for `/eco history` |
| `cleanupInactiveAccounts` | `true` | Automatically delete balances for long-inactive accounts |
| `inactiveAccountCleanupDays` | `30` | Inactivity threshold (days) before an account is cleaned up |
| `cacheMaximumSize` | `10000` | Max entries in the balance cache |
| `cacheExpireAfterAccessMinutes` | `60` | Cache entry expiry (minutes) |

`/sell`'s multiplier and named-item rule are read from **`config.json` → `economy`** (not `economy.json`):

| Key | Default | Description |
|---|---|---|
| `sellMultiplier` | `1.0` | Global multiplier applied to all `/sell` prices |
| `allowSellNamedItems` | `false` | Allow selling renamed items |

`/baltop`'s page size (10) and cache lifetime (60s) are currently hardcoded and not configurable.

---

## Commands

### Player Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/balance` | `/balance [player]` | `neoessentials.economy.balance` (own) / `neoessentials.economy.balance.others` (other players) | Check your balance |
| `/bal` | alias | same | Alias |
| `/pay` | `/pay <player> <amount>` | `neoessentials.economy.pay` | Send money to a player (applies tax; has a per-player cooldown) |
| `/p` | alias | same | Alias |
| `/paytoggle` | `/paytoggle` | `neoessentials.economy.paytoggle` | Toggle receiving payments |
| `/pt` | alias | same | Alias |
| `/baltop` | `/baltop [page]` | `neoessentials.economy.baltop` | View top balances (paginated, async, exempt via `neoessentials.economy.baltop.exempt`) |
| `/balancetop`, `/btop` | alias | same | Aliases |
| `/worth` | `/worth [item\|hand] [qty]` | `neoessentials.worth` | Check sell value of an item |
| `/sell` | `/sell hand\|inventory\|all\|<item> [qty]` | `neoessentials.sell` (+ `neoessentials.sell.hand` / `neoessentials.sell.bulk`) | Sell items for money |
| `/payconfirmtoggle` | `/payconfirmtoggle` | `neoessentials.payconfirmtoggle` | Toggle payment confirmation prompts (registered by the item/misc commands module, not the economy module) |

### Admin Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/eco give` | `/eco give <player> <amount>` | `neoessentials.economy.eco` | Give money |
| `/eco take` | `/eco take <player> <amount>` | `neoessentials.economy.eco` | Take money |
| `/eco set` | `/eco set <player> <amount>` | `neoessentials.economy.eco` | Set balance |
| `/eco reset` | `/eco reset <player>` | `neoessentials.economy.eco` | Reset to starting balance |
| `/eco history [player]` | `/eco history [player]` | none for self; op (level 2) for `[player]` | View transaction history (own, or another player's) |
| `/economy` | alias for `/eco` | same | Alias |
| `/setworth` | `/setworth <item\|hand> <price\|remove>` | `neoessentials.setworth` | Set/remove an item's sell price |

Note: `/eco give\|take\|set` take a plain numeric `<amount>` — percentage syntax (`10%`) is not supported.

---

## ChestShop

Sign-based shops that connect a chest to a sign for automated buy/sell.

### Setup

1. Place a chest
2. Place a sign on the chest (or adjacent block)
3. Write the sign in this format:

```
Line 1: [leave blank or your name]   ← auto-assigns your name if blank
Line 2: 5                            ← quantity per trade
Line 3: B 10:S 5                     ← buy price : sell price  (B only, S only, or both)
Line 4: diamond                      ← item name, or ? to assign by right-clicking with item
```

**Price shortcuts:** `B FREE` = free to buy · `S FREE` = free to sell · `1K` = 1000 · `1.5M` = 1500000

### Admin Shops

Use `Admin Shop` on line 1 — requires `neoessentials.shop.create.admin`. Admin shops have unlimited stock.

### Commands

| Command | Permission | Description |
|---|---|---|
| `/chestshop list [player]` | `neoessentials.shop.list` | List shops |
| `/chestshop info` | `neoessentials.shop.use` | Show info about a looked-at shop |
| `/chestshop remove <x y z>` | `neoessentials.shop.admin.remove` | Admin-remove a shop by coordinates |
| `/chestshop reload` | `neoessentials.shop.admin.reload` | Reload shops from disk |

### Permissions

| Node | Description |
|---|---|
| `neoessentials.shop.create` | Create player shops |
| `neoessentials.shop.create.admin` | Create admin shops |
| `neoessentials.shop.use` | Buy/sell at shops |
| `neoessentials.shop.list.others` | View other players' shops |
| `neoessentials.shop.admin.remove` | Remove any shop |
| `neoessentials.shop.admin.reload` | Reload shop data |

---

## Vault API

NeoEssentials registers itself as a Vault Economy, Chat, and Permission provider. Any mod/plugin using Vault will automatically use NeoEssentials.

| Provider | Class | Notes |
|---|---|---|
| Economy | `NeoEssentialsEconomy` | Backed by `EconomyManager`; `format()` uses live `currencySymbol` |
| Chat | `NeoEssentialsChat` | Prefix/suffix routed through LuckPerms → FTBRanks → internal |
| Permission | `NeoEssentialsPermission` | `playerHas()` → `PermissionAPI.hasPermission()` |

Use `/vault` to check provider status in-game.

---

## Data Files

| File | Contents |
|---|---|
| `neoessentials/balances.json` | Player UUID → balance |
| `neoessentials/transactions.json` | Transaction history log |
| `neoessentials/worth.json` | Item ID → sell price |
| `neoessentials/shops.json` | ChestShop data |

---

*Back to [Wiki Home](Home)*
