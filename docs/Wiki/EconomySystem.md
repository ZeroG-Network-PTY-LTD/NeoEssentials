# Economy System

> **Version:** 1.0.2.6 · **Config files:** `economy.json`, `config.json` → `economy` section

---

## Overview

NeoEssentials provides a full server economy with player balances, payments, admin tools, an async leaderboard, a sign-based ChestShop, and Vault API integration.

---

## Config (`economy.json`)

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable/disable the economy system |
| `startingBalance` | `1000.0` | Balance given to new players |
| `currencySymbol` | `"$"` | Symbol prepended to all amounts |
| `currencyNameSingular` | `"Dollar"` | Full currency name (singular) |
| `currencyNamePlural` | `"Dollars"` | Full currency name (plural) |
| `maxBalance` | `1000000000.0` | Maximum balance a player can hold |
| `allowNegativeBalances` | `false` | Allow balances below zero |
| `taxPercentage` | `0.0` | Tax applied to `/pay` transfers (0.0–1.0) |
| `maxTransferAmount` | `0` | Max single `/pay` amount (0 = unlimited) |
| `paytoggleDefault` | `true` | Whether players accept payments by default |
| `payConfirmThreshold` | `0` | Ask for confirmation above this amount (0 = off) |
| `sellMultiplier` | `1.0` | Global multiplier applied to all `/sell` prices |
| `allowSellNamedItems` | `false` | Allow selling renamed items |
| `baltopCacheSeconds` | `60` | How often the `/baltop` leaderboard refreshes |
| `baltopPageSize` | `10` | Entries per `/baltop` page |

---

## Commands

### Player Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/balance` | `/balance [player]` | `neoessentials.economy.balance` | Check your balance |
| `/bal` | alias | same | Alias |
| `/pay` | `/pay <player> <amount>` | `neoessentials.economy.pay` | Send money to a player |
| `/paytoggle` | `/paytoggle` | `neoessentials.economy.pay.toggle` | Toggle receiving payments |
| `/baltop` | `/baltop [page]` | `neoessentials.economy.baltop` | View top balances (paginated, async) |
| `/worth` | `/worth [item\|hand] [qty]` | `neoessentials.worth` | Check sell value of an item |
| `/sell` | `/sell hand\|inventory\|all\|<item> [qty]` | `neoessentials.sell` | Sell items for money |
| `/payconfirmtoggle` | `/payconfirmtoggle` | `neoessentials.economy.pay.toggle` | Toggle payment confirmation prompts |

### Admin Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/eco give` | `/eco give <player> <amount[%]>` | `neoessentials.economy.eco` | Give money (supports `10%` of balance) |
| `/eco take` | `/eco take <player> <amount[%]>` | `neoessentials.economy.eco` | Take money |
| `/eco set` | `/eco set <player> <amount>` | `neoessentials.economy.eco` | Set balance |
| `/eco reset` | `/eco reset <player>` | `neoessentials.economy.eco` | Reset to starting balance |
| `/setworth` | `/setworth <item\|hand> <price\|remove>` | `neoessentials.setworth` | Set/remove an item's sell price |

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
