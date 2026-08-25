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

## [1.0.5+build.54] — 2026-08-25

#### Fixed
- Uploading a resource pack through the web dashboard could corrupt the uploaded file, since the upload parser decoded the binary zip data as text; uploads are now parsed in a binary-safe way.
- The dashboard login lockout (locking an account after repeated failed attempts) could be bypassed by firing multiple login attempts at the same time, losing some of the failed-attempt count; this is now correctly counted under concurrent attempts.
- Creating warps at the same time from multiple sources could let the total warp count exceed the configured limit; warp creation now enforces the limit atomically.

---

## [1.0.5+build.53] — 2026-08-25

#### Fixed
- Kit items with saved enchantments, custom names, or other item data failed to load on every server startup (logged as "Failed to deserialize item entry in kit") because kits were loaded before the game's item-component registries were ready; kits are now re-loaded once the server has fully started, so this data is no longer silently dropped.

---

## [1.0.5+build.52] — 2026-08-25

#### Fixed
- `/pay` could silently lose the sender's money if crediting the receiver failed AND the automatic refund to the sender was itself rejected — the refund now falls back to a guaranteed settlement.

---

## [1.0.5+build.51] — 2026-08-25

#### Fixed
- The `/tpa` request timeout ran off the main server thread; now correctly marshaled.

---

## [1.0.5+build.50] — 2026-08-25

#### Fixed
- `/mail sendall` ran on a raw background thread and could corrupt the shared mailbox under concurrent mail activity; it now runs safely on the main thread.
- Config files and mail data could be left truncated/corrupted if the server crashed mid-save; saves are now atomic (write-then-rename) everywhere, matching how moderation data already worked.
- The dashboard's statistics, permissions, and Discord integration status endpoints were reading live player/server state directly from the HTTP request thread instead of the main server thread; now correctly synchronized.

---

## [1.0.5+build.49] — 2026-08-25

#### Fixed
- The AFK movement detector and the Discord permission-sync join notification had the same off-main-thread state access as the fixes below; both are now fixed.

---

## [1.0.5+build.48] — 2026-08-25

#### Fixed
- The fake tab-list player skin refresh sent packets off the main server thread; now correctly synchronized.
- The dashboard's saved encryption key for stored secrets (e.g. a paired external dashboard's token) could be left corrupted by a crash during first-time key generation; key writes are now atomic.

---

## [1.0.5+build.47] — 2026-08-25

#### Fixed
- The dashboard's i18n/translation-reload handler had unsynchronized shared state; fixed (this endpoint is not currently wired into the dashboard's route table, so this is a defensive fix rather than an active one).

---

## [1.0.5+build.46] — 2026-08-25

#### Fixed
- ChestShop buyers/sellers could lose their payment if the economy credit was rejected mid-transaction; now falls back to a guaranteed settlement instead of silently dropping the money.

---

## [1.0.5+build.45] — 2026-08-25

#### Fixed
- Web dashboard server restart/stop and MOTD broadcast actions were mutating live player state directly from the HTTP request thread instead of the main server thread; now correctly synchronized.

---

## [1.0.5+build.44] — 2026-08-25

#### Fixed
- Web dashboard scheduled-task execution (including manually triggering a server restart) ran off the main server thread, and its execution history was not thread-safe under concurrent use; both fixed.

---

## [1.0.5+build.43] — 2026-08-19

#### Fixed
- Web dashboard ban/IP-ban actions were mutating live player state directly from the HTTP request thread instead of the main server thread; now correctly synchronized. Ban/mute history tracking had the same unsynchronized-state issue and is also fixed.

---

## [1.0.5+build.42] — 2026-08-19

#### Fixed
- Permission condition evaluation failed *open* (granted access) instead of denying it when a condition errored, the same bug class as the FTB Ranks fix in the v1.0.4 baseline below.
- Auction house sellers could lose their payment if the economy credit was rejected mid-transaction; now falls back to a guaranteed settlement instead of silently dropping the money.

---

## [1.0.5+build.36] — 2026-08-18

#### Fixed
- Web dashboard vanish, jail, and balance-adjustment actions were mutating live player/entity state directly from the HTTP request thread instead of the main server thread, risking corrupted entity state under concurrent use — now correctly synchronized. First of several dashboard thread-safety fixes in this update cycle (see builds 42–50 above).

---

## [1.0.5+build.34] — 2026-08-18

#### Fixed
- Player chat could inject internal rich-text formatting markers (`§ITEM§`, `§URL§`, `§MENTION§`, and similar) to spoof clickable items, links, or mentions — these are now stripped from all player-submitted chat before formatting.
- Web dashboard shop price changes could race a concurrent buy/sell, applying a stale price.
- A player released from jail on respawn could be re-teleported into the jail cell even after disconnecting mid-respawn.

---

## [1.0.5+build.33] — 2026-08-18

#### Fixed
- The home/warp cooldowns could be consumed even when the request failed validation, effectively wasting the player's cooldown for nothing.

---

## [1.0.5+build.32] — 2026-08-18

#### Fixed
- FTB Ranks permission integration failed *open* (granted access) instead of denying it when a permission lookup errored.
- `/tpa` had the same cooldown-consumed-on-failed-validation bug as home/warp above.

---

## [1.0.5+build.31] — 2026-08-18

#### Fixed
- Discord mention sanitization (preventing chat-triggered `@everyone`/`@here`/role pings) now also covers the Mc2Discord and DCIntegration bridge adapters, not just the primary integration.

---

## [1.0.5+build.30] — 2026-08-18

#### Fixed
- The moderation data storage backend (bans, mutes, kicks, warns, notes, jails) wrote directly to its files with no crash protection — a crash mid-write could corrupt or truncate that data; saves are now atomic, and a corrupted file found on load is backed up aside instead of silently treated as empty.
- SDLink's chat/DM relay to Discord did not sanitize `@everyone`/`@here`/role mentions, letting any player ping the whole Discord server/role via bridged chat; fixed.

#### Changed
- Added an inline "restart required" note directly on `storage.type` in `config.json`.

---

## [1.0.5+build.28] — 2026-08-18

#### Added
- IP address format validation on `/ipban`/`/ipmute` (mod API and dashboard UI) — previously any text was accepted with no validation, silently storing typos as junk ban/mute entries.

---

## [1.0.5+build.26] — 2026-08-18

#### Fixed
- Path traversal in the dashboard's file-restore tool could let an authenticated admin session read/overwrite arbitrary files outside the intended directory.
- Overwriting a hologram by reusing an existing hologram's ID left the old entity orphaned in the world instead of removing it first.

---

## [1.0.5+build.25] — 2026-08-18

#### Fixed
- Path-traversal vulnerabilities in the server backup system (restore/delete/download) could let an authenticated admin session read or overwrite arbitrary files on the server outside the intended backup directory.

---

## [1.0.5+build.24] — 2026-08-18

#### Fixed
- `/pay` could consume the sender's cooldown even when the payment failed for an unrelated reason (mistyped name, over the transfer limit, etc.); the cooldown is now only consumed on an actual successful payment.

#### Added
- `shop.pricing` config section — the dynamic shop-pricing engine (supply/demand, time-based discounts, bulk-purchase tiers) was already fully implemented but had no config section to actually turn it on or tune it.

---

## [1.0.5+build.23] — 2026-08-18

#### Added
- A one-time in-game admin notice when `kits.json`/`permissions.json` still have real, unmigrated data after the active storage backend already has its own data.

---

## [1.0.5+build.22] — 2026-08-18

#### Fixed
- `kits.json`/`permissions.json`'s in-file instructions incorrectly implied they could still be hand-edited after the first server boot — corrected to explain they're one-time import files once a storage backend has data, and point at the actual live-editing path (`/createkit`, `/permissions` commands, or the dashboard).

---

## [1.0.5+build.20] — 2026-08-18

#### Fixed
- Split-config mode (`config.json` split into per-module files) could crash the server with a stack overflow shortly after being enabled.
- The `/language` command's setting was stored under the wrong config location and silently had no effect; it's now read from and applied to the correct place.

---

## [1.0.5+build.19] — 2026-08-18

#### Changed
- Removed the dead `autoSaveInterval` key from `economy.json` — it was never read anywhere.

---

## [1.0.5+build.18] — 2026-08-18

#### Changed
- Removed several dead chat config keys that had no effect on server behavior despite being present in `config.json`.

---

## [1.0.5+build.17] — 2026-08-18

#### Fixed
- Web dashboard routes for viewing IP ban/mute lists were missing an admin check, letting any authenticated dashboard account read real IP addresses, ban reasons, and staff attribution.

---

## [1.0.5+build.15] — 2026-08-18

#### Added
- Jail cells can now be created directly from the web dashboard by typing in coordinates, not just in-game via `/setjail`/the jail wand.

---

## [1.0.5+build.12] — 2026-08-18

#### Added
- Active IP bans/mutes are now visible on the public (no-login) moderation lookup page, with the address partially redacted.

---

## [1.0.5+build.10] — 2026-08-17

#### Changed
- Report filing (`/report` in-game, and the dashboard route below) is open to any player/dashboard user by default, matching the in-game command's default permission — only reviewing the report queue is staff-only.

---

## [1.0.5+build.6] — 2026-08-17

#### Fixed
- Banning, muting, or jailing a player from the web dashboard without specifying a duration (i.e. a permanent action) crashed with an error instead of applying the permanent punishment.

#### Added
- The web dashboard can now file a moderation report directly (previously reports could only be viewed/reviewed there, and had to be filed in-game via `/report`).

---

## [1.0.5+build.1–5] — 2026-08-17

The v1.0.5 reset baseline, carried over from the v1.0.4.x series:

#### Fixed
- Player chat could inject internal rich-text formatting markers to spoof clickable items, links, or mentions — stripped from all player-submitted chat.
- Web dashboard shop price changes could race a concurrent buy/sell, applying a stale price.
- A player released from jail on respawn could be re-teleported into the jail cell even after disconnecting mid-respawn.
- FTB Ranks permission integration failed *open* instead of denying on a lookup error.
- `/tpa` and the home/warp cooldowns could be consumed even when the request failed validation.
- Discord mention sanitization now also covers the Mc2Discord and DCIntegration bridge adapters.
- Web dashboard vanish, jail, and balance-adjustment actions were mutating live player/entity state directly from the HTTP request thread instead of the main server thread; now correctly synchronized.
- A narrow disconnect race in the vanish system's player-join handling.

#### Changed
- Replaced the single global debug-logging toggle with independent per-category logging (`logging.categories` in config) — chat, economy, permissions, teleportation, moderation, auction house, kits, web dashboard, Discord, config, commands, and general subsystems can now each be switched on/off separately for normal and debug output.
- Dozens of previously-silent error paths now log real diagnostic detail when their category's debug logging is enabled.

#### Platform
- Added a Minecraft 26.2 build (`26.2.x` branch, NeoForge 26.2.0.63+), alongside the existing pinned Minecraft 26.1–26.1.2 build (`26.1.x`).

---
