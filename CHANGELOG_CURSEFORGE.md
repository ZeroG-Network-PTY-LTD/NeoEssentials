# NeoEssentials — Changelog (26.2.x branch)

**Minecraft 26.2 · NeoForge 26.2.0.63+**

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
- Fixed permission condition evaluation failing *open* instead of denying on error, same class of bug as the FTB Ranks fix above
- Fixed auction house and ChestShop buyers/sellers losing their payment if the economy credit was rejected mid-transaction — now guaranteed to settle instead of silently dropping the money
- Fixed a wide range of web dashboard actions (ban/IP-ban management, scheduled-task execution incl. server restart, server restart/stop, MOTD broadcast, statistics, permissions, Discord status) running off the main server thread instead of correctly synchronizing
- Fixed the AFK movement detector, the fake tab-list player skin refresh, the Discord permission-sync join notification, and the `/tpa` request timeout having the same off-main-thread issue as above
- Fixed `/mail sendall` running on a raw background thread, which could corrupt the shared mailbox under concurrent mail activity
- Fixed config files and mail data being left corrupted if the server crashed mid-save — saves are now atomic everywhere
- Fixed the dashboard's saved encryption key for stored secrets being left corrupted by a crash during first-time key generation
- Fixed `/pay` silently losing the sender's money if crediting the receiver failed and the automatic refund was itself rejected
- Fixed kit items with saved enchantments/custom names failing to load on every server startup — kits are now re-loaded once the server has fully started
- Fixed uploading a resource pack through the web dashboard corrupting the uploaded file
- Fixed the dashboard's login lockout being bypassable by firing multiple simultaneous login attempts
- Fixed concurrent warp creation being able to exceed the configured warp limit
- Fixed dashboard crashing when banning/muting/jailing a player without specifying a duration (permanent action)
- Fixed IP ban/mute list dashboard routes leaking real IP addresses, reasons, and staff attribution to non-admin accounts
- Fixed path-traversal vulnerabilities in the backup system (restore/delete/download) and the dashboard's file-restore tool that could read/overwrite arbitrary server files
- Fixed overwriting a hologram by reusing its ID leaving the old entity orphaned in the world
- Fixed `/ipban`/`/ipmute` accepting any text as an IP address with no validation
- Fixed non-atomic moderation data writes that could corrupt ban/mute/kick/warn/note/jail data on a crash mid-save
- Fixed `/pay` consuming the cooldown even when the payment failed for an unrelated reason
- Fixed a stack-overflow crash shortly after enabling split-config mode
- Fixed the `/language` setting being stored in the wrong config location and not actually applying

### ✨ New Features
- The web dashboard can now file a moderation report directly
- Jail cells can now be created directly from the web dashboard via typed-in coordinates
- Active IP bans/mutes are now visible on the public (no-login) moderation lookup page, address redacted
- Added `shop.pricing` config section for the dynamic shop-pricing engine (was implemented but unconfigurable)

### 🧹 Cleanup
- Removed several dead config keys that had no effect on server behavior
- Clarified `kits.json`/`permissions.json`'s in-file instructions — both are one-time import files once a storage backend has data, not always-live-editable

### 📋 Logging
- Replaced the single global debug toggle with independent per-category logging — chat, economy, permissions, teleportation, moderation, auction house, kits, web dashboard, Discord, config, commands, and general subsystems can each be switched on/off separately
- Dozens of previously-silent error paths now log real diagnostic detail when their category's debug logging is enabled

### 🧩 Platform
- This branch now targets Minecraft 26.2 (up from 26.1.2) — see the `26.1.x` branch if you need to stay on Minecraft 26.1–26.1.2.

---
