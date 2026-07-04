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

> Typing a name directly on line 4 only resolves a bare item ID — it can't carry enchantments,
> custom names, or modded NBT-backed data. Use `?` (or shift+right-click afterward — see
> [Item Data / NBT](#item-data--nbt-modded-items) below) to assign the item by holding the exact
> stack instead, if it has data that needs to be preserved.

**Price shortcuts:** `B FREE` = free to buy · `S FREE` = free to sell · `1K` = 1000 · `1.5M` = 1500000

### Admin Shops

Use `Admin Shop` on line 1 — requires `neoessentials.shop.create.admin`. Admin shops have unlimited stock.

### Item Data / NBT (Modded Items)

Sign text alone can only encode a bare item ID (`diamond`, `thermal:copper_ingot`) — it cannot carry custom data (enchantments, custom names/lore, or a modded item's NBT-backed capability data). To make a shop trade an item **with its data intact**, assign it by holding the exact item and interacting with the sign/hologram rather than typing its name:

| Gesture | When it applies | Effect |
|---|---|---|
| Right-click the sign/hologram while holding the item | Sign's item line is `?` (shop is "pending") | One-time setup — captures the held item's data and activates the shop |
| **Shift+right-click** the sign/hologram while holding the item | Shop already has an item assigned (including ones set by typing a name on the sign) | Re-assigns the item, capturing its data — use this to attach/update NBT on an existing shop |

Only the owner (or, for admin shops, a player with `neoessentials.shop.create.admin`) can do either. If a shop was created by typing an item name directly on the sign and buying/selling against a data-bearing item in the chest reports **"out of stock"** even though the chest visibly has stock, this is almost always the fix — the shop never captured the item's data, so it wasn't matching what's actually in the chest. Shift+right-click it with the correct item to fix.

### Removing a Shop

Left-click (attack) a shop sign to sell, same as always — but a single left-click swing on a shop sign is also the tool used to actually **break** it, so it's intercepted for the sell/info action by default. **Sneak (shift) + left-click** bypasses that interception and lets the swing through as a normal break attempt, removing the shop (if you're the owner or an admin).

### Commands

`/chestshop` (alias `/cshop`) with no arguments prints an in-game help listing.

| Command | Permission | Description |
|---|---|---|
| `/chestshop list [player]` | none (self) / `neoessentials.shop.list.others` (other player) | List shops owned by you or another player |
| `/chestshop info` | none | Show info about the looked-at shop sign |
| `/chestshop convert` | `neoessentials.shop.create` | Register the looked-at (pre-existing) sign as a shop |
| `/chestshop setprice <buy\|sell\|both> <price>` | shop owner, or `neoessentials.shop.setprice` / `neoessentials.shop.admin.setprice` | Change the price(s) on the looked-at shop sign |
| `/chestshop stats` | none | Show your total shops, sales count, and top seller |
| `/chestshop limit` | none | Show how many shops you've placed vs. your `shop.maxShopsPerPlayer` limit |
| `/chestshop pricing` | none | Show whether the dynamic pricing engine is enabled and its rule count |
| `/chestshop hologram enable` | shop owner only | Enable a floating price hologram on the looked-at shop |
| `/chestshop hologram disable` | shop owner only | Remove the hologram from the looked-at shop |
| `/chestshop hologram move <x> <y> <z>` | shop owner only | Reposition the hologram (offset from the sign, ±4.5 blocks per axis) |
| `/chestshop export` | `neoessentials.shop.admin.csv.export` | Export all shops to a CSV file |
| `/chestshop import [create]` | `neoessentials.shop.admin.csv.import` | Import shops from CSV (`create` also creates new signs) |
| `/chestshop remove` | shop owner, or `neoessentials.shop.admin.remove` / OP 3 | Remove the shop you're currently looking at (sign or its linked chest) |
| `/chestshop remove <x> <y> <z>` | `neoessentials.shop.admin.remove` | Admin-only: remove a shop at specific coordinates, without needing to look at it |
| `/chestshop reload` | `neoessentials.shop.admin.reload` | Reload shop data and the dynamic pricing config |

### Dynamic Pricing & Holograms

Shops support optional per-shop **holograms** (floating buy/sell display, clickable) and a configurable **dynamic pricing engine** (`shop.pricing` in `config.json`) with rules such as bulk-quantity discounts, time-based discounts, and supply/demand adjustment. These are off by default — `/chestshop pricing` reports current status.

### Permissions

| Node | Description |
|---|---|
| `neoessentials.shop.create` | Create player shops / register signs via `/chestshop convert` |
| `neoessentials.shop.create.admin` | Create admin shops |
| `neoessentials.shop.use` | Buy/sell at shops |
| `neoessentials.shop.list.others` | View other players' shops via `/chestshop list <player>` |
| `neoessentials.shop.setprice` | Change prices on your own shops via `/chestshop setprice` |
| `neoessentials.shop.admin.setprice` | Change prices on any shop |
| `neoessentials.shop.admin.remove` | Remove any shop |
| `neoessentials.shop.admin.reload` | Reload shop data |
| `neoessentials.shop.admin.csv.export` | Export shops to CSV |
| `neoessentials.shop.admin.csv.import` | Import shops from CSV |

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
