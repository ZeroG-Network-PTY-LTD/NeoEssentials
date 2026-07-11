# Changelog — NeoEssentials (mc-26.1-port branch)

All notable changes to this branch are documented here, starting from
**v1.0.3** — earlier history (v1.0.2.x and before, including the port's own
migration history) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 26.1.2 · NeoForge 26.1.2.76+**

> **Known gap:** this branch has not yet merged several `Dev-Builds` fixes
> landed on 07-06/07-08 (after this branch had already forked): permission
> precedence and a `getUser()` race, freeze/jail/vanish/mute enforcement gaps,
> kit permanent-lockout/double-claim/data-loss fixes, an Auction House
> duplication exploit fix, jail bounds ignoring dimension, web dashboard
> admin-role/password-hashing security fixes, the `/jail` NPE (jailing is
> currently broken on this branch), `localize()` overload/placeholder-shift
> fixes, `/invseeedit`/`/pay`/`/eco` fixes, and the tablist nametag feature.
> These need porting over separately.

> The build counter was reset alongside the v1.0.3 bump, so build numbers here are
> not monotonically increasing relative to now-removed v1.0.2.x entries — always
> go by date/version, not build number, when comparing across the reset.

---

## [1.0.3+build.6] — 2026-07-11

### 🐛 Bug Fixes

#### Tablist `playerFormat` Spacing Had No Effect
**Config:** `tablist.json` → `playerFormat`

- **Root cause:** `playerFormat` was loaded from config but never actually applied
  anywhere — `TablistManager.updatePlayerTeam()` sent the raw permission-system
  prefix/suffix straight to `team.setPlayerPrefix()`/`setPlayerSuffix()`, completely
  ignoring the template. Editing `playerFormat` (including inserting spaces between
  `{prefix}`/`{player}`/`{suffix}`, even Unicode non-breaking spaces) had no effect
  on rendering at all — it was a config no-op, not a case of Minecraft stripping
  whitespace.
- **Fix:** Vanilla's scoreboard-team prefix/suffix mechanism always renders as a
  fixed `prefix + <player name> + suffix` — `{player}` can't be reordered relative
  to `{prefix}`/`{suffix}`. `TablistManager.parsePlayerFormat()` now splits
  `playerFormat` around its three tokens and folds the literal text surrounding them
  into the prefix/suffix strings actually sent to the client (e.g. the space
  between `{prefix}` and `{player}` in `"{prefix} {player} {suffix}"` is appended to
  the prefix, and the space before `{suffix}` is prepended to the suffix) —
  recomputed once per config load/reload, not per-tick.
- **Note:** this also means the *default* `playerFormat` value
  (`"&f{prefix}&r{player}{suffix}"`) now actually applies its `&f`/`&r` color codes,
  which it never did before — a visible (and evidently originally intended) change
  for servers that never touched this setting.

---

## [1.0.3-mc26.1.2+build.10] — 2026-07-12

### ✨ Storage Backends Now Cover the Whole Mod

Extended the `DataStore` abstraction introduced in build.9 to every remaining manager
that used to persist its own bespoke JSON files — `storage.type` (JSON/YAML/SQLite/MySQL)
now applies mod-wide, not just to moderation.

- **Migrated:** economy (balances, pay toggles, transaction history, item worth), kits
  (definitions, cooldowns, usages), homes, `/back` locations, warps (global and
  per-player), spawn, jail (active jail state and jail locations), freeze, vanish, AFK
  data, ignore lists, per-player chat formats, holograms, chest shops, NPC shops,
  permissions (groups with inheritance, users, aliases), the dashboard's own accounts
  and registrations, custom-language admin overrides, resource-pack metadata, and the
  Auction House.
- **Auction House** no longer opens its own dedicated SQLite database
  (`auctionhouse.db`) — it now shares the same backend as everything else, including
  MySQL for cross-server auction listings. Existing `auctionhouse.db` data is imported
  automatically on first boot.
- All of these follow the same auto-migration behavior as the moderation system:
  legacy JSON files are imported once, losslessly, the first time the relevant
  collection is empty (with `storage.autoMigrate` enabled, the default) — old files
  are left in place, never deleted automatically.
- **Fixed in passing:** `/warp`'s per-player warps were being read/written from a
  hard-coded `run/playerwarps.json` relative path instead of the configured data
  directory — this no longer happens (existing data at that path is still imported
  once during migration).
- See the new "Storage Backend" wiki page for the full system-by-system collection list.

---

## [1.0.3-mc26.1.2+build.9] — 2026-07-12

### ✨ New Feature — Pluggable Storage Backends (JSON / YAML / SQLite / MySQL)

Added a generic `DataStore` abstraction (`com.zerog.neoessentials.storage`) so managers
can persist to JSON (default), YAML, an embedded SQLite database, or a shared MySQL/MariaDB
database, selected via the new `storage` section in `config.json` — restart required
after changing `storage.type`.

- **`storage.type`**: `"json"` (default), `"yaml"`, `"sqlite"`, or `"mysql"`. MySQL is
  the one that actually enables multi-server shared data — point every server in a
  network at the same database and they see the same bans/mutes/etc. in real time,
  matching how ban-management plugins like BanManager use MySQL for network-wide
  moderation. If MySQL is configured but unreachable at boot, falls back to JSON
  automatically rather than failing the whole server start.
- Every backend stores the same schema-less JSON-document shape (one record per
  `id` per "collection"), so no bespoke relational schema is needed per data type.
- **This release migrates the moderation system onto it** (bans, IP bans, mutes, IP
  mutes, kicks, warns, notes, reports) as the first, fully-verified rollout —
  existing legacy JSON files are imported automatically and losslessly the first
  time the server boots with `storage.autoMigrate` enabled (the default), including
  ban/mute active-vs-history state and the full unban/unmute audit trail. Verified
  live: created data, restarted the server, confirmed everything survived correctly
  through the new backend.
- **Not yet migrated:** economy, homes, warps, kits, permissions, and the rest of the
  mod's managers still read/write their own JSON files directly. They're unaffected
  by `storage.type` for now and would need the same treatment in a future update to
  benefit from YAML/SQLite/MySQL.

---

## [1.0.3-mc26.1.2+build.8] — 2026-07-12

### ✨ New Features — Moderation System Overhaul

Rebuilt the moderation system to match ban-management plugins' feature set (bans,
mutes, kicks, warnings, notes, reports — all with full history and staff attribution),
and to fix a serious pre-existing correctness bug along the way.

#### Consolidated the Ban System — Dashboard Bans Now Actually Work
**Files:** `BanManager`, `ModerationManager` (deprecated)

- **The bug:** two entirely separate, disconnected ban stores existed. `/ban`
  (the command staff actually run) only ever talked to `BanManager`. The
  dashboard's `POST /api/moderation/ban` only ever talked to a second,
  parallel store (`ModerationManager`/`BanEntry`). **A ban created through the
  dashboard did not block that player from joining** — the two systems never
  reconciled. `ModerationManager`'s ban-tracking half is now marked deprecated
  and no longer used by the dashboard; `ModerationEndpoint` talks directly to
  `BanManager`, the same store `/ban` enforces.
- `BanManager` (player + IP bans) now has: ban IDs, an `active` flag, an
  `evidence` field, and — most importantly — **archives every ban instead of
  hard-deleting it on unban/expiry**, so a player's/IP's full ban history
  (including who unbanned them and when) is preserved and queryable via
  `getBanHistory()`/`getIPBanHistory()`, matching ban-management plugins'
  "view a player's entire record, including unbans and by whom."

#### Mutes Are No Longer Bare-Bones
**File:** `MuteManager`

- Previously just a name→expiry map with no reason, no staff attribution, and
  no history — the reason typed on `/mute <player> <reason>` was discarded
  after the initial chat broadcast. Now tracks reason, muted-by, full
  per-player history, and an unmute audit trail, plus new IP-mute support
  (`muteIP`/`unmuteIP`/`isIPMuted`) that didn't exist at all before. Old
  `mutes.json` files (the flat legacy format) are transparently migrated on load.

#### Kicks Are Now Recorded
**File:** new `KickManager`

- Kicks were previously fire-and-forget with, at most, an optional unstructured
  log line — no persisted, queryable history existed. `/kick`, `/kickall`, and
  the dashboard's kick action now all record through `KickManager`.

#### New: Staff Notes and Player Reports
**Files:** new `NoteManager`/`NoteEntry`, `ReportManager`/`ReportEntry`

- `/note`, `/notes`, `/removenote` — freeform staff notes on a player's record,
  matching "staff can write notes and view a player's entire record."
- `/report`, `/reports`, `/reviewreport` — players can report other players
  even while staff are offline; staff review a persistent queue later,
  matching "players can report wrongful behaviour even when staff are offline."

#### Dashboard API Rewired Onto the Canonical Model
**File:** `ModerationEndpoint`

- Every route now backed by the real, enforced managers above instead of the
  disconnected `ModerationManager` store. Added IP-ban, IP-mute, kick, note,
  and report routes that didn't exist at all before (see the file's own
  route-table Javadoc for the full list). `/api/moderation/overview` now
  reports counts across every punishment type instead of just bans/warns/mutes.

---

## [1.0.3-mc26.1.2+build.7] — 2026-07-11

### 🐛 Bug Fixes

#### Tablist `playerFormat` Spacing Had No Effect
**Config:** `tablist.json` → `playerFormat`

- **Root cause:** `playerFormat` was loaded from config but never actually applied
  anywhere — `TablistManager.updatePlayerTeam()` sent the raw permission-system
  prefix/suffix straight to `team.setPlayerPrefix()`/`setPlayerSuffix()`, completely
  ignoring the template. Editing `playerFormat` (including inserting spaces between
  `{prefix}`/`{player}`/`{suffix}`, even Unicode non-breaking spaces) had no effect
  on rendering at all — it was a config no-op, not a case of Minecraft stripping
  whitespace.
- **Fix:** Vanilla's scoreboard-team prefix/suffix mechanism always renders as a
  fixed `prefix + <player name> + suffix` — `{player}` can't be reordered relative
  to `{prefix}`/`{suffix}`. `TablistManager.parsePlayerFormat()` now splits
  `playerFormat` around its three tokens and folds the literal text surrounding them
  into the prefix/suffix strings actually sent to the client (e.g. the space
  between `{prefix}` and `{player}` in `"{prefix} {player} {suffix}"` is appended to
  the prefix, and the space before `{suffix}` is prepended to the suffix) —
  recomputed once per config load/reload, not per-tick.
- **Note:** this also means the *default* `playerFormat` value
  (`"&f{prefix}&r{player}{suffix}"`) now actually applies its `&f`/`&r` color codes,
  which it never did before — a visible (and evidently originally intended) change
  for servers that never touched this setting.

---

## [1.0.3-mc26.1.2+build.482] — 2026-07-10

### 🐛 Bug Fixes

#### ChestShop Double Chests Only Reading One Half
**Commands:** `/chestshop` (buy/sell, stock checks, dynamic pricing)

- **Root cause:** `ShopTransaction.getChest()` and `SupplyDemandRule.getStock()` cast
  the block entity at the shop's chest position directly to `ChestBlockEntity`, which
  only exposes that one 27-slot half of a double chest. Buy/sell stock checks, item
  add/remove, low-stock notifications, and supply/demand pricing all only saw one
  sign's own chest half instead of the shared 54-slot inventory.
- **Fix:** Both now use `HopperBlockEntity.getContainerAt(level, pos)` — the same
  helper vanilla hoppers use to pull from chests — which returns the properly
  combined container for double chests (single chests are unaffected).

#### Admin Shops Could Never Use `/chestshop hologram enable|disable|move`
- **Root cause:** `ShopCommand.isShopOwner()` only checked
  `shop.ownerUUID.equals(player)`, but admin shops have `ownerUUID == null` by
  design, so the check failed unconditionally for every player — including whoever
  holds `neoessentials.shop.create.admin`.
- **Fix:** Admin shops are now authorized via `neoessentials.shop.create.admin`,
  matching the pattern already used for admin-shop item assignment.

### ✨ Improvements

#### Command Feedback Messages: Branded Tag + Softened Colors
- `MessageUtil.success()`/`error()`/`warning()`/`info()` now prepend a short
  `[NE]` tag (`§8[§bNE§8] `) and use vanilla-matching soft colors (same RGB as
  `§a`/`§c`/`§e`/`§b`) instead of harsh neon primaries. Scoped to the four
  command-feedback wrapper methods only — `localize()` itself is untouched.

#### NPC Shops: Sell Support, Permission Checks, and Entity Recovery
- **Sell was completely non-functional.** `ShopListing` carries a `sellPrice`,
  `/npcshop additem` lets you configure one, and the GUI lore even advertised
  "Sell: $X" — but the shop menu only ever handled buying (right-click a slot).
  Added left-click-to-sell handling, mirroring the buy flow, reusing
  `ShopTransaction`'s `countItems()`/`removeItems()` (widened to `public` for
  this cross-package reuse). On this branch, `AbstractContainerMenu#clicked`'s
  click-type parameter is `ContainerInput` (renamed from `ClickType` in the
  26.1 port), so the left/right-click dispatch uses that instead.
- **No permission check on NPC shop transactions at all.** Added a
  `neoessentials.shop.use` check before the shop menu opens.
- **No way to recover a shop whose NPC entity was lost** without losing its
  listings. Added `/npcshop respawn <shopId>`, which re-summons the NPC at its
  stored spawn position and re-links it to the existing shop data.
- These new messages use raw `Component.literal` (not `MessageUtil`/lang keys)
  to match this branch's current state of the NPC shop files, which predate
  the `MessageUtil` localization pass applied elsewhere.
