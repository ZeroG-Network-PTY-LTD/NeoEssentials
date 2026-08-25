# Changelog — NeoEssentials

> **This is the UNIVERSAL changelog** — one shared history covering all three dev
> branches (`1.21.x`, `26.1.x`, `26.2.x`), since GitHub releases bundle every branch's
> jar into a single shared release. Write entries here ONCE, describing the change
> itself — not duplicated per branch, and not restated in branch-specific terms unless
> a fix genuinely only applies to one version. This file should read identically across
> all three branches at any given point in time.
>
> `CHANGELOG_CURSEFORGE.md`/`CHANGELOG_MODRINTH.md` stay branch-specific (those platforms
> require separate uploads per Minecraft version), so keep writing those per-branch.

All notable changes to NeoEssentials are documented here, starting from
**v1.0.5** — earlier history (v1.0.4.x and before) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 (`1.21.x`) · Minecraft 26.1–26.1.2 (`26.1.x`) · Minecraft 26.2 (`26.2.x`)**

> The build counter was reset alongside the v1.0.5 bump, so build numbers here start
> back at 0/1, and the shared CI build number was reset to match — no more offset
> between branches.

---

## [1.0.5] — 2026-08-19

#### Fixed
- Player chat could inject internal rich-text formatting markers (`§ITEM§`, `§URL§`, `§MENTION§`, and similar) to spoof clickable items, links, or mentions — these are now stripped from all player-submitted chat before formatting.
- Web dashboard shop price changes could race a concurrent buy/sell, applying a stale price.
- A player released from jail on respawn could be re-teleported into the jail cell even after disconnecting mid-respawn.
- FTB Ranks permission integration failed *open* (granted access) instead of denying it when a permission lookup errored.
- `/tpa` and the home/warp cooldowns could be consumed even when the request failed validation, effectively wasting the player's cooldown for nothing.
- Discord mention sanitization (preventing chat-triggered `@everyone`/`@here`/role pings) now also covers the Mc2Discord and DCIntegration bridge adapters, not just the primary integration.
- Web dashboard vanish, jail, and balance-adjustment actions were mutating live player/entity state directly from the HTTP request thread instead of the main server thread, risking corrupted entity state under concurrent use — now correctly synchronized.
- A narrow disconnect race in the vanish system's player-join handling, where a player who disconnected within a 1-tick window could still be sent stale visibility packets.
- Permission condition evaluation failed *open* (granted access) instead of denying it when a condition errored, the same bug class as the FTB Ranks fix above.
- Auction house sellers, and ChestShop buyers/sellers, could lose their payment if the economy credit was rejected mid-transaction; both now fall back to a guaranteed settlement instead of silently dropping the money.
- A wide range of web dashboard actions — ban/IP-ban management, scheduled-task execution (including server restart), server restart/stop, MOTD broadcast, statistics, permissions, and Discord integration status — were reading or mutating live player/server state directly from the dashboard's HTTP request thread instead of the main server thread, risking corrupted state or crashes under concurrent use. All now correctly synchronized, matching the vanish/jail/economy fix above.
- The AFK movement detector, the fake tab-list player skin refresh, the Discord permission-sync join notification, and the `/tpa` request timeout all had the same off-main-thread state access as above and are now fixed.
- `/mail sendall` ran on a raw background thread and could corrupt the shared mailbox under concurrent mail activity; it now runs safely on the main thread.
- Config files and mail data could be left truncated/corrupted if the server crashed mid-save; saves are now atomic (write-then-rename) everywhere, matching how moderation data already worked.
- The dashboard's saved encryption key for stored secrets (e.g. a paired external dashboard's token) could be left corrupted by a crash during first-time key generation; key writes are now atomic as well.
- `/pay` could silently lose the sender's money if crediting the receiver failed AND the automatic refund to the sender was itself rejected — the refund now falls back to a guaranteed settlement, same as the auction house/shop fix above.
- Kit items with saved enchantments, custom names, or other item data failed to load on every server startup (logged as "Failed to deserialize item entry in kit") because kits were loaded before the game's item-component registries were ready; kits are now re-loaded once the server has fully started, so this data is no longer silently dropped.
- Uploading a resource pack through the web dashboard could corrupt the uploaded file, since the upload parser decoded the binary zip data as text; uploads are now parsed in a binary-safe way.
- The dashboard login lockout (locking an account after repeated failed attempts) could be bypassed by firing multiple login attempts at the same time, losing some of the failed-attempt count; this is now correctly counted under concurrent attempts.
- Creating warps at the same time from multiple sources could let the total warp count exceed the configured limit; warp creation now enforces the limit atomically.

#### Changed
- Replaced the single global debug-logging toggle with independent per-category logging (`logging.categories` in config) — chat, economy, permissions, teleportation, moderation, auction house, kits, web dashboard, Discord, config, commands, and general subsystems can now each be switched on/off separately for normal and debug output, instead of one all-or-nothing flag.
- Dozens of previously-silent error paths across chat, permissions, tablist, teleportation, and command handling now log real diagnostic detail when their category's debug logging is enabled, instead of failing without a trace.

#### Platform
- Added a Minecraft 26.2 build (`26.2.x` branch, NeoForge 26.2.0.63+), alongside the existing pinned Minecraft 26.1–26.1.2 build (`26.1.x`).

---
