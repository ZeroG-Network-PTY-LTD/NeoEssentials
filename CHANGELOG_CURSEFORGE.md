# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.5 — 2026-08-19

### 🛡️ Security & Correctness Fixes
- Player chat could inject internal rich-text markers to spoof clickable items, links, or mentions — now stripped from all player chat
- Fixed a web dashboard shop price race condition that could apply a stale price during concurrent buy/sell
- Fixed jail respawn re-teleporting a player who had already disconnected
- Fixed FTB Ranks integration failing *open* (granting access) instead of denying on a lookup error
- Fixed `/tpa` and home/warp cooldowns being consumed even when the request failed validation
- Discord mention sanitization now also covers the Mc2Discord and DCIntegration bridge adapters
- Fixed web dashboard vanish/jail/balance actions running off the main server thread, risking corrupted entity state
- Fixed a narrow disconnect race in the vanish system's join handling

### 📋 Logging
- Replaced the single global debug toggle with independent per-category logging — chat, economy, permissions, teleportation, moderation, auction house, kits, web dashboard, Discord, config, commands, and general subsystems can each be switched on/off separately
- Dozens of previously-silent error paths now log real diagnostic detail when their category's debug logging is enabled

### 🧩 Platform
- Added a Minecraft 26.2 build (NeoForge 26.2.0.63+), alongside the existing pinned Minecraft 26.1–26.1.2 build

## 1.0.2.6+build.1 — 2026-03-06

### 🔁 Starting fresh from 1.0.2.6

This is the first build of the `1.0.2.6` release series.

**Included from 1.0.2.5 series:**
- Sign-based ChestShop system with admin shops, auto-fill item (`?`), and buy/sell via right/left-click
- Vault API (Economy, Chat, Permission providers)
- Dedicated `tablist.json` config file with group colours, placeholders, and animation support
- 50+ new commands across Player Info, World/Fun, Teleport, Item/Misc, Utility, Admin, and Player State systems
- Random teleport (`/tpr`) — even distribution, nether-aware, async cache, named zones, biome exclusions
- Timed jails with auto-release, full event enforcement (respawn, teleport, interact, attack)
- Offline pay, async baltop with pagination and total wealth, percent eco amounts
- 8 new languages (FR, DE, ES, PT-BR, ZH-CN, NL, PL, RU) — auto-deployed and merged on start
- 50+ permission nodes registered, new MODERATION category, denial messages show required node
- Teleportation safe-location detection fully rewritten (slabs, stairs, glass, etc. now safe)
- AFK system fully wired to config, score thresholds fixed, broadcasts and personal feedback working
- Chat messages now appear in server console
- PowerTool fixes — fires on block right-clicks, `/powertooltoggle` now works correctly
- Rich text (gradients/rainbow) rendering fixed
- Dashboard offline login, register command, and file auto-update fixed
- ~120 missing translation keys added and auto-merged on load
