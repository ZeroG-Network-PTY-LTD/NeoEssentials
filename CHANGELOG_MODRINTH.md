# NeoEssentials — Changelog (26.2.x branch)

**Minecraft 26.2 · NeoForge 26.2.0.63+**

All notable changes to this branch are documented here, starting from
**v1.0.5** — earlier history (v1.0.4.x and before, including the port's own
migration history) is not carried over.

## 1.0.5+build.54 — 2026-08-25

### 🐛 Resource Pack Uploads, Login Lockout, and Warp Limits — Three Race-Condition Fixes

- Uploading a resource pack through the web dashboard could silently corrupt the file — the upload parser was decoding the raw zip bytes as text. Fixed with a proper binary-safe parser.
- The dashboard's login lockout (locking an account after repeated failed attempts) could be dodged by firing several login attempts at once — some of the failed-attempt count got lost in the race. Now correctly counted no matter how many attempts land at the same time.
- Creating warps from multiple sources at the same moment could push the total warp count past your configured limit. The limit is now enforced atomically.

---

## 1.0.5+build.53 — 2026-08-25

### 🐛 Kit Items With Enchantments/Custom Names Were Silently Dropped on Every Boot

- If a kit item had saved enchantments, a custom name, or any other item data, it failed to load every single time the server started (you'd see "Failed to deserialize item entry in kit" in the log) — because kits were being loaded before the game's item systems were fully ready. Kits are now re-loaded once the server has actually finished starting, so this data isn't lost anymore.

---

## 1.0.5+build.52 — 2026-08-25

### 🐛 /pay Could Lose Money on a Very Unlucky Double Failure

- In the rare case where `/pay` failed to credit the receiver AND the automatic refund back to the sender also failed, the sender's money vanished into nowhere. The refund now guarantees it lands back in the sender's account no matter what.

---

## 1.0.5+build.51 — 2026-08-25

### 🐛 /tpa Timeout Wasn't Thread-Safe

- Internal fix: the background timer that expires a `/tpa` request after it times out could run into the same off-main-thread issue as several dashboard fixes below. No visible behavior change — just safer under real player load.

---

## 1.0.5+build.50 — 2026-08-25

### 🐛 /mail sendall, Config Saves, and Three More Dashboard Endpoints Made Thread-Safe

- `/mail sendall`/`sendtempall` ran on a background thread that could corrupt the shared mailbox if players were sending/reading mail at the same time. It now runs safely on the main thread.
- Config files and mail data could be left truncated or corrupted if the server crashed mid-save. Saves are now atomic (write-then-rename) everywhere, matching how moderation data already worked.
- The dashboard's Statistics, Permissions, and Discord integration pages were reading live server state directly from the HTTP request thread instead of the main thread — same bug class as the vanish/jail/ban fixes further down, now fixed here too.

---

## 1.0.5+build.49 — 2026-08-25

### 🐛 AFK Detection and Discord Role-Sync Messages Made Thread-Safe

- The AFK system's movement-detection timer, and the chat message sent to a player when their Discord roles sync on join, were both touching player state off the main server thread. Fixed — same underlying issue as the other thread-safety fixes in this update.

---

## 1.0.5+build.48 — 2026-08-25

### 🐛 Fake Tab-List Player Skins, Dashboard Encryption Key Made Safer

- If you use fake/decorative tab-list entries with a custom skin, the skin lookup could send its update off the main server thread. Fixed.
- The dashboard's saved encryption key for stored secrets (like a paired external dashboard's token) could be left corrupted by a crash during first-time key generation. Key writes are now atomic.

---

## 1.0.5+build.47 — 2026-08-25

### 🐛 Dashboard Language/Translation Reload Made Thread-Safe

- The dashboard's translation-reload endpoint could touch shared chat-translation state off the main thread. Fixed (this endpoint isn't wired up to anything yet, so no live impact — just closed the gap before it is).

---

## 1.0.5+build.46 — 2026-08-25

### 🐛 ChestShop Buy/Sell Could Lose Payment on a Rejected Transaction

- If a ChestShop payment got rejected mid-transaction (an economy plugin/hook cancelling it), the buyer or seller's money could vanish instead of the trade failing cleanly. Now guaranteed to settle correctly either way — same fix as the Auction House got further down.

---

## 1.0.5+build.45 — 2026-08-25

### 🐛 Dashboard Restart/Stop and MOTD Broadcast Made Thread-Safe

- The dashboard's server restart/stop buttons, and its MOTD broadcast action, were touching the player list off the main thread. Fixed.

---

## 1.0.5+build.44 — 2026-08-25

### 🐛 Dashboard Scheduled Tasks (Including Manual Restart) Made Thread-Safe

- Manually running a scheduled task from the dashboard — especially a RESTART-type task — could touch the player list off the main server thread. Its execution history could also get corrupted under concurrent use. Both fixed.

---

## 1.0.5+build.43 — 2026-08-19

### 🐛 Dashboard Ban/IP-Ban Actions Made Thread-Safe

- Banning or IP-banning a player from the dashboard was mutating live player state directly from the HTTP request thread instead of the main server thread — risking corrupted state under concurrent use. Ban/mute history tracking had the same issue. Both fixed.

---

## 1.0.5+build.42 — 2026-08-19

### 🐛 Permission Conditions Failing Open, Auction House Payment Loss

- If a permission condition errored while being evaluated, it was granting access instead of denying it — same bug class as the FTB Ranks fix below. Fixed to fail closed.
- Auction house sellers could lose their payment if the economy credit was rejected mid-transaction. Now guaranteed to settle instead of silently dropping the money.

---

## 1.0.5+build.36 — 2026-08-18

### 🐛 Dashboard Vanish/Jail/Balance Actions Made Thread-Safe

- Toggling vanish, jailing a player, or adjusting a balance from the dashboard was mutating live player/entity state directly from the HTTP request thread instead of the main server thread, risking corrupted entity state under concurrent use. Fixed — the first of several dashboard thread-safety fixes in this update cycle.

---

## 1.0.5+build.34 — 2026-08-18

### 🐛 Chat Marker Injection, Shop Price Race, Jail Respawn Bug

- Player chat could inject internal rich-text formatting markers to spoof clickable items, links, or mentions — now stripped from all player-submitted chat.
- Web dashboard shop price changes could race a concurrent buy/sell, applying a stale price.
- A player released from jail on respawn could be re-teleported back into the jail cell even after disconnecting mid-respawn.

---

## 1.0.5+build.33 — 2026-08-18

### 🐛 Home/Warp Cooldown Consumed Even on a Failed Request

- Setting a home or warp that failed validation (bad name, over your limit, etc.) still consumed your cooldown for nothing. Fixed — same fix as `/tpa`/`/pay` got.

---

## 1.0.5+build.32 — 2026-08-18

### 🐛 FTB Ranks Failing Open, /tpa Cooldown Bug

- FTB Ranks permission integration was granting access instead of denying it when a permission lookup errored — a real privilege-escalation risk if you use FTB Ranks.
- `/tpa` had the same cooldown-consumed-on-failure bug as home/warp above.

---

## 1.0.5+build.31 — 2026-08-18

### 🐛 Discord Mention Sanitization Extended to Mc2Discord and DCIntegration

- The `@everyone`/`@here`/role-ping sanitization on chat relayed to Discord only covered the primary bridge integration. Now also covers Mc2Discord and DCIntegration.

---

## 1.0.5+build.30 — 2026-08-18

### 🐛 Non-Atomic Storage Writes, Discord Mention Griefing (SDLink)

- The moderation data storage backend (bans, mutes, kicks, warns, notes, jails) wrote directly to its files with no crash protection — a crash mid-write could corrupt or truncate that data. Saves are now atomic, and a corrupted file found on load gets backed up aside instead of silently treated as empty.
- If you use Simple Discord Link, a player could ping `@everyone`/`@here` or paste a raw Discord mention in bridged chat and have it actually go through to Discord. Fixed with the same sanitization the primary integration already had.
- Added an inline note directly on `storage.type` in `config.json` explaining that changing it requires a restart, not just `/neoe reload`.

---

## 1.0.5+build.28 — 2026-08-18

### ✨ IP Address Validation on /ipban and /ipmute

- `/ipban`/`/ipmute` (and the dashboard forms for the same) accepted any text as an IP address with zero validation — a typo silently became a junk ban/mute entry. Both now validate the address format before accepting it.

---

## 1.0.5+build.26 — 2026-08-18

### 🐛 Path Traversal in File Restore, Orphaned Hologram Entity

- **Security:** the dashboard's file-restore tool didn't properly check that the requested path stayed inside the intended directory, letting an authenticated admin session potentially read files elsewhere on disk. Fixed.
- Overwriting a hologram by reusing an existing hologram's ID left the old entity orphaned in the world (still visible, no longer manageable) instead of removing it first. Fixed.

---

## 1.0.5+build.25 — 2026-08-18

### 🐛 Path Traversal in Backup Restore/Delete/Download

- **Security:** the server backup system's restore, delete, and download actions didn't validate the backup name the same way creating a backup already did, letting an authenticated admin session potentially read or overwrite arbitrary files on the server. Fixed.

---

## 1.0.5+build.24 — 2026-08-18

### 🐛 /pay Cooldown-on-Failure, New shop.pricing Config

- `/pay` had the same cooldown-consumed-on-a-failed-request bug as home/warp/tpa above — fixed here specifically for payments (mistyped name, over your transfer limit, etc. no longer cost you a cooldown).
- Added a `shop` section to `config.json` with a `pricing` block — the dynamic shop-pricing engine (supply/demand, time-based discounts, bulk-purchase tiers) was fully built but had no way to actually turn it on or configure it until now.

---

## 1.0.5+build.23 — 2026-08-18

### ✨ Admin Notice for Unmigrated Legacy Kit/Permission Data

- If you still have real, unmigrated data sitting in `kits.json` or `permissions.json` after your storage backend already imported once, admins now get a one-time in-game notice explaining that editing those files no longer does anything.

---

## 1.0.5+build.22 — 2026-08-18

### 🐛 kits.json/permissions.json Instructions Were Misleading

- Both files' in-file headers implied you could still hand-edit them after the first server boot. In reality both are one-time import files once your storage backend has data — updated the headers to explain that and point you at the real live-editing path (`/createkit`, `/permissions`, or the dashboard).

---

## 1.0.5+build.20 — 2026-08-18

### 🐛 Split-Config Mode Could Crash the Server, Misplaced Localization Section

- If you use split-config mode (`config.json` broken into per-module files), the server could crash with a stack overflow shortly after being enabled. Fixed.
- The `/language` command's setting was stored under the wrong config path and silently had no effect no matter what you set it to. Moved to the correct location — `/language` now actually works.

---

## 1.0.5+build.19 — 2026-08-18

### 🧹 Removed a Dead economy.json Key

- `autoSaveInterval` in `economy.json` was never actually read by anything — removed. If you had it set to something, it was never doing anything.

---

## 1.0.5+build.18 — 2026-08-18

### 🧹 Removed Several Dead Chat Config Keys

- A handful of chat-notification config keys (sleep/AFK-related settings, some death/join-message toggles) had no effect on server behavior despite being present in `config.json` — removed. If you had customized any of these, they were never actually doing anything.

---

## 1.0.5+build.17 — 2026-08-18

### 🛡️ IP Ban/Mute List Was Leaking Real Addresses to Non-Admins

- **Security:** the dashboard's IP ban/mute list-viewing routes were missing an admin check, letting any authenticated dashboard account read real IP addresses, ban reasons, and staff attribution — not just admins. Fixed.

---

## 1.0.5+build.15 — 2026-08-18

### ✨ Jail Cells Can Now Be Created From the Dashboard

- Previously the only way to set up a jail cell was in-game (`/setjail`/jail wand). Now you can create one directly from the dashboard by typing in coordinates — no need to be in-game at all.

---

## 1.0.5+build.12 — 2026-08-18

### ✨ Active IP Bans/Mutes Now on the Public Lookup Page

- The no-login public moderation lookup page now shows active IP bans/mutes alongside regular bans/mutes, with the address partially redacted for privacy.

---

## 1.0.5+build.10 — 2026-08-17

### ✨ Report Filing Opened to Everyone

- Filing a moderation report from the dashboard was accidentally admin-gated, same as reviewing reports — but filing a report in-game (`/report`) has always been open to every player by default. Fixed to match: filing is open to any dashboard user, reviewing the queue stays staff-only.

---

## 1.0.5+build.6 — 2026-08-17

### 🐛 Fixed Dashboard Crash on Permanent Bans/Mutes/Jails

- Banning, muting, or jailing a player from the web dashboard *without* specifying a duration (i.e. a permanent action, the most common case) crashed with an error instead of actually applying the punishment. Fixed.

### ✨ File a Moderation Report From the Dashboard

- The dashboard could already view and review reports, but had no way to create one — you had to go in-game and use `/report`. Now there's a dashboard route (and internal-dashboard page) for filing one directly.

---

## 1.0.5+build.1–5 — 2026-08-17

### 🔁 The v1.0.5 Reset

This is the first build of the `1.0.5` release series — earlier v1.0.4.x history is not carried over (see the old `1.0.4.x` changelog if you need it). Carried over from the v1.0.4 series and folded into this baseline:

- Chat marker injection, shop price race, and jail respawn logout fixes
- FTB Ranks fail-open fix and `/tpa`/home/warp cooldown-before-validation fix
- Discord mention sanitization extended to the Mc2Discord and DCIntegration bridge adapters
- Web dashboard vanish/jail/balance actions synchronized onto the main server thread
- A narrow disconnect race in the vanish system's player-join handling

### 📋 Per-Category Logging (Re-established for 1.0.5)

- Independent per-category logging (`logging.categories` in `config.json`) — chat, economy, permissions, teleportation, moderation, auction house, kits, web dashboard, Discord, config, commands, and general subsystems can each be switched on/off separately for normal and debug output, instead of one all-or-nothing flag.
- Dozens of previously-silent error paths now log real diagnostic detail when their category's debug logging is enabled.

### 🧩 Platform

- This branch now targets Minecraft 26.2 (up from 26.1.2) — see the `26.1.x` branch if you need to stay on Minecraft 26.1–26.1.2.

---
