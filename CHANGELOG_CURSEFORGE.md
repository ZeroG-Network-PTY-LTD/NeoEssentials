# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.5+build.1057 — 2026-03-05

### ✨ New: ChestShop System

Sign-based chest shops, fully integrated with the mod's economy and permission systems.

**Creating a shop:**
1. Place a chest
2. Place a sign on/next to the chest and write:
   ```
   Line 1: Your name  (or leave blank — auto-fills your name)
   Line 2: 5          (quantity per trade)
   Line 3: B 10:S 5   (buy price : sell price)
   Line 4: diamond    (item, or ? to assign by right-clicking)
   ```
3. Shop is live immediately!

**Using a shop:**
- **Right-click** sign → BUY items
- **Left-click** sign → SELL items
- **Own sign** → shows shop info

**Admin Shops:** Write `Admin Shop` on line 1 for unlimited stock (requires `neoessentials.shop.create.admin`)

**Auto-fill item (`?`):** Write `?` on line 4, then right-click the sign holding the item you want — the sign updates automatically.

**Commands:** `/chestshop list`, `/chestshop info`, `/chestshop convert`, `/chestshop remove`, `/chestshop reload` (alias: `/cshop`)

### ✨ New: Vault API

Three Vault providers implemented and integrated:
- **Economy** — backed by NeoEssentials economy system; fires deposit/withdraw events
- **Chat** — prefix/suffix respects LuckPerms and FTB Ranks when installed
- **Permissions** — routes through external permission adapters automatically

### 🐛 Fixes

- Vault economy `format()` now uses live currency symbol from config
- Vault chat prefix/suffix now correctly uses LuckPerms/FTBRanks when installed
- Various NeoForge 1.21.1 API compatibility fixes (event classes, ItemStack methods, stats API)
- Duplicate keys in `config.json` fixed
- JSON syntax error in `config.json` fixed

---

## 1.0.2.5+build.908 — 2026-03-03

### ✨ New Commands (50+)

| System | Commands Added |
|---|---|
| Player Info | `/seen`, `/near`, `/ping`, `/playtime`, `/whois`, `/realname`, `/msgtoggle`, `/rtoggle`, `/motd`, `/rules`, `/sudo`, `/suicide` |
| World / Fun | `/fireball`, `/tree`, `/bigtree`, `/break`, `/ice`, `/bottom`, `/tpaall`, `/broadcastworld` |
| Teleport+ | `/renamehome`, `/warpinfo`, `/world`, `/spawner`, `/recipe`, `/tpauto`, `/tpr`/`/rtp` |
| Item / Misc | `/me`, `/tptoggle`, `/gc`, `/lightning`, `/skull`, `/itemname`, `/itemlore`, `/remove`, `/loom`, `/cartography` |
| Utility | `/ptime`, `/pweather`, `/effect`, `/spawnmob`, `/unlimited`, `/condense` |
| Admin | `/broadcast`, `/time`, `/weather`, `/kill`, `/gamemode`, `/tpo`, `/tpohere`, `/tpoffline` |
| Player State | `/fly`, `/god`, `/heal`, `/feed`, `/speed`, `/ext`, `/burn`, `/give`, `/more`, `/hat`, `/exp` |
| Economy | `/eco reset`, `/baltop [page]`, `/worth`, `/sell`, `/setworth` |
| Jail | `/jailfor`, `/deljail` |
| Kits | `/kit <name> <player>` (give to others), `/kitreset` |
| Mail | `/mail sendtemp`, `sendall`, `sendtempall`, `clearall` |
| Dashboard | `/chestshop` (see above) |

### 🌍 New Languages

French, German, Spanish, Portuguese (BR), Chinese (Simplified), Dutch, Polish, Russian — all auto-deployed and merged on server start without overwriting edits.

### 🔀 Random Teleport (`/tpr`)

- Even distribution (no centre-clustering), nether-aware Y, world-border aware
- Configurable cooldown, range, biome exclusions
- Async pre-computation cache
- Named RTP zones via `/settpr`

### 🔐 Permission System

- 50+ missing permission nodes now registered
- New `MODERATION` permission category
- Permission denial messages now show the required permission node
- Wildcard & hierarchical permission support improved

### 🛡️ Moderation Improvements

- Timed jails with auto-release
- Jail now enforces on: respawn, teleport, interact, attack
- Economy: offline pay, async baltop with pagination, percent amounts, IgnoreManager check in `/pay`

### 🐛 Major Fixes

- Teleportation safe-location detection fully rewritten (slabs, stairs, glass now recognised as safe)
- AFK system config loading, score thresholds, broadcast formatting, and personal feedback fixed
- Chat messages now appear in server console
- PowerTools now follow the item, not the slot
- Rich text (gradients/rainbow) rendering fixed
- Dashboard offline login, register command, and file auto-update fixed
- All language keys with broken colour codes fixed

---

## 1.0.2.3 — initial release

Core economy, chat, moderation, teleportation, kits, warps, web dashboard, permissions, item management.

