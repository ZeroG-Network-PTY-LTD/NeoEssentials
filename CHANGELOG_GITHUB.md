# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here, starting from
**v1.0.3** — earlier history (v1.0.2.x and before) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

> The build counter was reset alongside the v1.0.3 bump, so build numbers here are
> not monotonically increasing relative to now-removed v1.0.2.x entries — always
> go by date/version, not build number, when comparing across the reset.

---

## [1.0.3+build.8] — 2026-07-12

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

## [1.0.3+build.7] — 2026-07-11

### 🐛 Bug Fixes

#### Dashboard Account Login History and Lockout State Reset on Every Restart
**File:** `AuthenticationManager` (`dashboard_users.json` persistence)

- **Root cause:** `saveUsers()`/`loadUsers()` only serialized a subset of `User`'s
  fields (id, username, passwordHash, email, role, enabled, createdAt,
  requiresPasswordChange, isTempPassword) — `lastLoginAt`, `lastLoginIp`,
  `failedLoginAttempts`, and `lockoutUntil` were never written to
  `dashboard_users.json` at all, so every server restart silently reset every
  account's login history to "never logged in" and cleared any active
  brute-force lockout, even though `toJson()` (used for live API responses)
  correctly included these fields — the bug only affected disk persistence.
- **Fix:** `saveUsers()` now writes all four fields; `loadUsers()` restores them
  (defensively, since existing `dashboard_users.json` files from before this fix
  won't have them yet).

#### Locale-Dependent Number Formatting Corrupted Dashboard API JSON Fields
**Files:** `BackupManager`, `ServerDataCollector`

- **Root cause:** `String.format("%.2f", ...)` and `new DecimalFormat("#.##")`
  both use the JVM's default locale unless told otherwise. On a server running
  with a comma-decimal locale (this repo's own dev environment runs `ru_ru`),
  these produced strings like `"19,5"` instead of `"19.5"` for `tps`,
  `averageTickTime`, memory/CPU percentages, and backup `sizeMb`/`totalSizeMb` —
  every one of these fields is documented and consumed as a parseable number by
  dashboard clients. PHP's `(float)` cast (and most other JSON number parsers)
  silently truncates at the comma instead of erroring, so e.g. a `19,5` TPS
  reading would silently read as `19.0` on the standalone dashboard with no
  visible failure.
- **Fix:** Both now format with `Locale.ROOT` explicitly.

#### Parameterless Dashboard API POST Routes 500'd for Non-PHP Clients
**Files:** `PermissionEndpoint`, `BackupEndpoint`, `CloudStorageEndpoint`, `UserManagementEndpoint`

- **Root cause:** Several dashboard API POST routes that take no parameters (e.g.
  `/api/permissions/reload`, backup create/restore, cloud-storage config) parsed
  the request body as `body.isBlank() ? new JsonObject() : JsonParser.parseString(body)
  .getAsJsonObject()`. PHP has no distinct empty-array/empty-object literal, so a
  client like Laravel's HTTP client (used by the standalone NeoEssentials dashboard
  app) serialises an empty parameter list as the JSON array `[]` rather than `{}` —
  a non-blank string that `JsonParser` correctly parses as a `JsonArray`, which then
  throws `IllegalStateException: Not a JSON Object: []` on the `.getAsJsonObject()`
  cast. Every one of these routes 500'd whenever called with an empty payload.
- **Fix:** New shared `RequestBodyUtil.parseJsonObject()`/`readJsonObject()`
  (`webdashboard.util` package) treats blank bodies **and** non-object JSON (arrays)
  as "no parameters", returning an empty `JsonObject` instead of throwing. Applied
  across every endpoint using the old blank-guard pattern.

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

## [1.0.3+build.480] — 2026-07-10

### 🐛 Bug Fixes

#### ChestShop Double Chests Only Reading One Half
**Commands:** `/chestshop` (buy/sell, stock checks, dynamic pricing)

- **Root cause:** `ShopTransaction.getChest()` and `SupplyDemandRule.getStock()` cast
  the block entity at the shop's chest position directly to `ChestBlockEntity`, which
  only exposes that one 27-slot half of a double chest. Buy/sell stock checks, item
  add/remove, low-stock notifications, and supply/demand pricing all only saw one
  sign's own chest half instead of the shared 54-slot inventory — filling/emptying
  one side blocked transactions on the other sign even though the double chest still
  had space/stock.
- **Fix:** Both now use `HopperBlockEntity.getContainerAt(level, pos)` — the same
  helper vanilla hoppers use to pull from chests — which returns the properly
  combined container for double chests (single chests are unaffected).

#### Admin Shops Could Never Use `/chestshop hologram enable|disable|move`
- **Root cause:** `ShopCommand.isShopOwner()` only checked
  `shop.ownerUUID.equals(player)`, but admin shops have `ownerUUID == null` by
  design, so the check failed unconditionally for every player — including whoever
  holds `neoessentials.shop.create.admin`. The hologram opt-in prompt still appeared
  at admin shop creation (it doesn't check shop type), but clicking it always failed
  with an "owner only" error.
- **Fix:** Admin shops are now authorized via `neoessentials.shop.create.admin`,
  matching the pattern already used for admin-shop item assignment.

### ✨ Improvements

#### Command Feedback Messages: Branded Tag + Softened Colors
- `MessageUtil.success()`/`error()`/`warning()`/`info()` now prepend a short
  `[NE]` tag (`§8[§bNE§8] `) so players can tell at a glance which mod a message
  came from — useful on servers running several mods with similarly-colored chat
  output.
- Replaced the harsh neon primary colors (`0x00FF00`/`0xFF0000`/`0xFFFF00`/`0x00FFFF`)
  with vanilla-matching soft colors (same RGB as `§a`/`§c`/`§e`/`§b`), consistent
  with the inline `§` colors most lang templates already use.
- Scoped to the four command-feedback wrapper methods only — `localize()` itself is
  untouched, so logs/audit trails/transaction history that read the raw translated
  string are unaffected.

#### NPC Shops: Sell Support, Permission Checks, and Entity Recovery
- **Sell was completely non-functional.** `ShopListing` carries a `sellPrice`,
  `/npcshop additem` lets you configure one, and the GUI lore even advertised
  "Sell: $X" — but the shop menu only ever handled buying (right-click a slot).
  There was no left-click/sell path at all. Added sell handling (left-click a
  listing), mirroring the buy flow: verify the player holds enough of the item,
  credit their balance, then remove the items (rolling back the credit if removal
  unexpectedly fails).
- **No permission check on NPC shop transactions at all.** ChestShop enforces
  `neoessentials.shop.use` before every buy/sell; NPC shops didn't check any
  permission. Added the same check before the shop menu opens.
- **No way to recover a shop whose NPC entity was lost** (void damage bypasses
  `setInvulnerable`, a stray `/kill`, etc.) without losing its listings — the
  relevant recovery methods existed but were never called from anywhere. Added
  `/npcshop respawn <shopId>`, which re-summons the NPC at its stored spawn
  position and re-links it to the existing shop data.

### 🔧 Maintenance
- Build version string now includes the target Minecraft version
  (`1.0.2.6-mc1.21.1+build.N`), matching the format already used on the 26.1.x port
  branch so builds from either branch are distinguishable at a glance.
- `_langVersion` bumped to 23 for the new/changed lang keys in this release.
