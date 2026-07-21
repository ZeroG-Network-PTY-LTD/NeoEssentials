# NeoEssentials — Changelog (mc-26.1-port branch)

**Minecraft 26.1.2 · NeoForge 26.1.2.76+**

All notable changes to this branch are documented here, starting from
**v1.0.3** — earlier history (v1.0.2.x and before, including the port's own
migration history) is not carried over.

---

## 1.0.3-mc26.1.2+build.78 — 2026-07-21

### 🐛 Vanish No Longer Makes Noise or Gets Noticed by Mobs

- Fixed vanished players still being audible — footsteps, splashes, etc. no longer play for
  other players while you're vanished.
- Fixed hostile mobs still targeting and attacking vanished players — they're now ignored
  by mob AI too, not just invisible to other players.
- Vanished players can no longer hurt other players, interact with anything, or break/place
  blocks by default — these each now need a specific permission
  (`neoessentials.vanish.hurt`/`.interact`/`.build`, or the `neoessentials.vanish.*` wildcard
  for all three) rather than being tied to the "can see vanished players" permission.
- The "who can see who" rank system for vanish now uses proper permission nodes
  (`neoessentials.vanish.priority.<number>` — higher number wins, equal numbers can see each
  other too) instead of a fixed list of group names, so it works with any permission setup,
  not just the specific group names the mod used to expect.

---

## 1.0.3-mc26.1.2+build.77 — 2026-07-21

### 🐛 Fixed /sell Selling the Wrong Item at the Wrong Price

- Fixed `/sell hand` sometimes showing "minecraft:air" in the sale message and overcharging
  up to 3x the price you set for the item.
- Server admins (OPs) and permission groups with a broad wildcard permission no longer
  automatically get the highest sell-price bonus, free tax exemption, or no pay cooldown —
  those now have to be granted on purpose. (Configurable if you actually want the old
  behavior back.)
- Typing `/sell` with no arguments now sells whatever's in your hand, same as `/sell hand`.
- Fixed a config file typo that could break Discord-login syncing on server start.

---

## 1.0.3-mc26.1.2+build.24 — 2026-07-18

### 🧹 Config Cleanup — Kit Commands Now Actually Run

- Removed a handful of dead/duplicate config settings that either did nothing or were
  always overridden by another file, so the config files are less confusing to edit.
- **Fixed:** the `commands` list on a kit (run when it's claimed, e.g. a welcome message)
  was silently ignored — it now actually runs.
- The service-account login option for external dashboards has been removed — use
  `/dashboard pair` or `/apikey create` instead (see below).

---

## 1.0.3-mc26.1.2+build.23 — 2026-07-18

### ✨ Economy Now Supports Third-Party Economy Mods

- Added support for [SG Economy API](https://www.curseforge.com/minecraft/mc-mods/sg-economy-api)
  as an optional alternative to NeoEssentials' own economy — enable with
  `economy.useExternalEconomy: true` in config.json if you'd rather players' balances live
  in that mod instead. Off by default.
- The mod's internal economy/permission interop system (used for advanced mod-to-mod
  integrations) was also split into its own publicly-installable component for other mod
  developers.

---

## 1.0.3-mc26.1.2+build.22 — 2026-07-18

### 🧹 Dashboard Command Cleanup

- Fixed `/dashboard pair` not being able to accept a server URL at all — it now works
  correctly (the URL needs quotes: `/dashboard pair "http://host:port" <code>`).
- Removed `/dashboard update`/`check`/`force` — leftovers from an old bundled dashboard UI
  that no longer ships with the mod, so these commands never actually did anything useful.

---

## 1.0.3-mc26.1.2+build.21 — 2026-07-17

### ✨ One-Command External Dashboard Pairing

- Connecting an external dashboard app to your server is now a single command:
  `/dashboard pair "<dashboardUrl>" <code>` (get the code from the dashboard's own setup
  page). Both sides connect to each other automatically — no more manually copying keys
  between two config files.
- `/dashboard unpair` disconnects cleanly.

---

## 1.0.3-mc26.1.2+build.20 — 2026-07-17

### 🐛 Bug Fix — LuckPerms Group Now Affects Chat & Tablist

- **Fixed:** a player's LuckPerms group wasn't being used for `group:<name>` chat formats
  or per-group tablist styling — both always fell back to the default, even though
  permission *nodes* from LuckPerms worked fine. Now the player's real LuckPerms primary
  group is used correctly for both.

---

## 1.0.3-mc26.1.2+build.19 — 2026-07-17

### ✨ API Keys, Locked-Down Admin Settings, and Dashboard Account Sync

- **New: API keys** for external dashboard apps — a proper, revocable login credential
  separate from human dashboard accounts. Create one with `/apikey create <label>`.
- **Security fix:** MOTD, rules, shops, holograms, and warps could previously be edited by
  any logged-in dashboard account, including read-only ones. Editing these now correctly
  requires an admin-level account; viewing them still doesn't.
- Dashboard user accounts can now sync automatically between the mod and an external
  dashboard app in both directions.

---

## 1.0.3-mc26.1.2+build.18 — 2026-07-17

### 🐛 Bans and Kicks Weren't Actually Being Tracked

- **Fixed:** `/ban` and `/kick` (and related commands) were silently being intercepted by
  vanilla Minecraft's own built-in versions instead of NeoEssentials' — meaning bans and
  kicks never showed up in the mod's own history or the dashboard, even though the ban/kick
  itself still worked. Now correctly tracked.
- Fixed `/ban`'s reason text sometimes getting merged into the player name field.
- Added `/modhistory <player>` (alias `/history`) to view a player's full moderation record
  in-game.
- Fixed the dashboard's Discord "send test message" button, which previously always
  reported success without actually sending anything.
- Fixed deleting a permission group silently doing nothing.

---

## 1.0.3-mc26.1.2+build.17 — 2026-07-16

### 🐛 Storage Fix + 9 Bugs From a Full Command Audit

- **Fixed:** the SQLite storage option could silently fail to save/load data in some setups.
- Also fixed: `/baltop` occasionally showing stale balances, `/setworth` rejecting some
  item IDs, `/invseeedit` crashing, jail auto-ban thresholds never actually triggering,
  `/chatformat` not being a registered command, `/mute` commands failing from console/RCON,
  and a couple of command-name conflicts (`/gc`, `/me`).

---

## 1.0.3-mc26.1.2+build.16 — 2026-07-16

### ✨ `/sudo` Improvements

- `/sudo <player> c:<message>` makes a player say something in real chat (going through
  normal chat formatting/filters), instead of just running a command as them.
- `/sudo *` runs a command as every online player at once; `/sudo **` includes yourself too.

---

## 1.0.3-mc26.1.2+build.15 — 2026-07-15

### ✨ Discord Integration Rewrite

- Rebuilt the Discord chat-relay integrations for Simple Discord Link, Mc2Discord, and
  DCIntegration against their real, official APIs (previously used guesswork that could
  break on updates). Dropped DiscordSRV support — it doesn't run on NeoForge servers.
- Dashboard's "Login with Discord" now only works by reading an account link you've
  already set up in-game via one of the above mods — the mod itself never talks to
  Discord directly.

---

## 1.0.3-mc26.1.2+build.14 — 2026-07-14

### ✨ Module Toggles Fixed, Public Moderation Lookup, Dashboard Commands Fixed

- **Fixed:** many of the on/off switches in `config.json`'s `modules`/`commands` sections
  didn't actually do anything for a lot of commands. They all work correctly now — note
  that toggling any of these still needs a server restart, not just `/neoe reload`.
- Added new toggles for holograms, shops, the auction house, and a few other systems that
  previously couldn't be disabled at all.
- **Fixed:** `/dashboard` and `/dashboardregister` were completely non-functional commands
  — typing them did nothing. Both now work.
- Added a public, no-login player moderation lookup page/API for transparency, similar to
  other ban-management plugins.

---

## 1.0.3-mc26.1.2+build.10 — 2026-07-12

### ✨ Storage Backends Now Cover the Whole Mod

- Extended the pluggable JSON/YAML/SQLite/MySQL storage system to every remaining
  manager: economy, kits, homes, `/back`, warps, spawn, jail, freeze, vanish, AFK,
  ignore lists, chat formats, holograms, chest/NPC shops, permissions (groups,
  users, aliases), dashboard accounts, custom-language overrides, resource-pack
  metadata, and the Auction House.
- The Auction House no longer uses its own separate SQLite database — it now shares
  the same backend as everything else, including MySQL for cross-server listings.
- All existing data imports automatically and losslessly the first time you switch
  backends, same as the moderation system did in the previous release.
- Fixed a bug where `/warp`'s per-player warps were read/written from a hard-coded
  path instead of the configured data directory.

---

## 1.0.3-mc26.1.2+build.9 — 2026-07-12

### ✨ Pluggable Storage Backends (JSON / YAML / SQLite / MySQL)

- New `storage` section in `config.json` lets you pick JSON (default), YAML,
  SQLite, or MySQL as the backend for supported data. MySQL enables true
  multi-server shared data — point every server in your network at the same
  database and they share bans/mutes/etc. in real time.
- This release migrates the moderation system (bans, mutes, kicks, warns,
  notes, reports) onto the new system. Existing data is imported automatically
  and losslessly the first time you switch backends.
- Economy, homes, warps, kits, permissions, and the rest of the mod's data
  are unaffected for now — still plain JSON, with the same treatment planned
  for a future update.

---

## 1.0.3-mc26.1.2+build.8 — 2026-07-12

### ✨ Moderation System Overhaul

- **Fixed: dashboard bans didn't actually ban anyone.** Two disconnected ban
  stores existed — `/ban` enforced one, the dashboard wrote to the other, and
  they never talked to each other. Consolidated onto one canonical, UUID+IP-aware
  store with ban IDs, full history, and an unban audit trail (who/when).
- **Mutes** now track reason, staff attribution, full history, and an unmute
  audit trail (previously just a bare name→expiry map), plus new IP-mute support.
- **Kicks** are now recorded with a queryable history (previously fire-and-forget).
- **New:** staff notes (`/note`, `/notes`) and player reports (`/report`,
  `/reports`, `/reviewreport`) — players can report others even while staff
  are offline, and staff can leave freeform notes on a player's record.
- The dashboard's moderation API now exposes all of the above, including new
  IP-ban/IP-mute/kick/note/report routes that didn't exist before.

---

## 1.0.3-mc26.1.2+build.7 — 2026-07-11

### 🐛 Bug Fixes

- **Tablist `playerFormat` spacing had no effect:** editing `playerFormat` in
  `tablist.json` — including adding spaces between `{prefix}`/`{player}`/`{suffix}` —
  never actually changed anything, since the setting was loaded but never applied.
  Now the literal text around each token is correctly folded into the prefix/suffix
  sent to the client, so spacing (and other literal text in the template) actually
  renders. Note: the default template's `&f`/`&r` color codes now also apply, which
  they never did before — a visible change for servers that never touched this
  setting.

---

## 1.0.3-mc26.1.2+build.482 — 2026-07-10

### 🐛 Bug Fixes

- **ChestShop double chests:** Buy/sell, stock checks, and dynamic pricing only
  read one half of a double chest. Now reads the full combined 54-slot
  inventory.
- **ChestShop admin shop holograms:** `/chestshop hologram enable|disable|move`
  could never be used on admin shops. Now authorized via
  `neoessentials.shop.create.admin`.

### ✨ Improvements

- **Command feedback messages** now show a small `[NE]` tag and use softer,
  vanilla-matching colors.
- **NPC Shops:** Selling now actually works (previously only buying was
  implemented). Added a permission check before opening the shop menu. Added
  `/npcshop respawn <shopId>` to re-summon a lost shop NPC without losing its
  listings.
