# NeoEssentials — Changelog (26.1.x branch)

**Minecraft 26.1–26.1.2 · NeoForge 26.1.2.76+**

All notable changes to this branch are documented here, starting from
**v1.0.5** — earlier history (v1.0.4.x and before, including the port's own
migration history) is not carried over.

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
- A separate `26.2.x` branch now provides a Minecraft 26.2 build for servers that have updated past 26.1.2 — this branch remains pinned to Minecraft 26.1–26.1.2.

---
