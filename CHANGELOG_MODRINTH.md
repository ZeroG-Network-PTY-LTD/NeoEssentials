# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.5+build.1057 — 2026-03-05

### ✨ ChestShop System

Sign-based shops — the classic essentials shop experience, natively in NeoForge.

**Sign format (all 4 lines):**
```
[blank or name]   ← auto-assigns your name if left blank
[quantity]        ← e.g. 5
[B buy:S sell]    ← e.g. B 10:S 5  /  B FREE  /  S 2.50
[item or ?]       ← e.g. diamond  — or ? to right-click-assign
```

- Right-click sign = **BUY**, left-click sign = **SELL**
- Blank line 1 auto-assigns the creating player's name
- Write `?` on line 4, then right-click the sign holding an item to fill it in
- **Admin Shops** — `Admin Shop` on line 1 = unlimited stock, no chest required
- Shops persist across restarts in `neoessentials/shops.json`
- Full economy integration — uses EconomyManager with rollback safety
- Permission-gated creation and usage
- `/chestshop` command suite: `list`, `info`, `convert`, `remove`, `reload`

### ✨ Vault API

- Economy, Chat, and Permission Vault providers
- Chat prefix/suffix now uses LuckPerms or FTB Ranks when installed
- Deposit/withdraw events fire for all Vault-originated transactions

### 🐛 Fixes

- NeoForge 1.21.1 API compatibility fixes across multiple commands
- `config.json` duplicate keys and JSON syntax error fixed
- Vault economy currency format now reads live from config
- Vault chat prefix/suffix now correctly uses external permission adapters

---

## 1.0.2.5+build.908 — 2026-03-03

### ✨ 50+ New Commands

Player info, world interaction, teleportation, item customisation, admin tools, economy enhancements, and more. Full command list: [CHANGELOG_GITHUB.md](CHANGELOG_GITHUB.md)

### 🌍 9 Languages

English + French, German, Spanish, Portuguese (BR), Chinese, Dutch, Polish, Russian. Auto-deployed on server start, safe to edit.

### 🛒 Worth & Sell System

`/worth`, `/sell hand|inventory|all`, `/setworth` — sell multiplier, named item protection.

### 🎲 Random Teleport

`/tpr` — even distribution, nether-aware, biome exclusions, async cache, named zones.

### 🔐 Permissions

50+ missing nodes registered, `MODERATION` category added, denial messages show required node.

### 🐛 Key Fixes

- Safe teleport detection rewritten (slabs/stairs/glass now safe)
- AFK system fully functional
- Chat appears in server console
- PowerTools follow the item not the slot
- Dashboard offline login, auto-update, rich text fixed
- Timed jails with full enforcement

---

## 1.0.2.3 — initial release

Core economy, chat, moderation, teleportation, kits, warps, web dashboard, permissions, item management.

