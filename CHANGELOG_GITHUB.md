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

## [1.0.3+build.24] — 2026-07-18

### 🧹 Config Cleanup — Removed Dead/Shadowed Keys, Kit Commands Now Actually Run

Full audit of every shipped config file against actual code usage before touching anything.

- **`config.json`:** removed the entire legacy `tablist` section (~26 keys) — `tablist.json`
  always wins when present (every real deployment), so this was pure dead weight and a
  confusing "which file actually wins" trap for operators. Removed
  `economy.currencySymbol`/`startingBalance` — literal duplicates always shadowed by
  `economy.json`'s own copies. Removed `afk.enableTablistIndicator`/`tablistAfkPrefix`/
  `tablistAfkSuffix` — confirmed `AfkManager` never reads them; `tablist.json`'s own
  `showAfkIndicator`/`afkSuffix` already own this.
- **`discord_auth.json`:** removed `permissionMappings_common_examples`, an empty stub
  self-labeled "not loaded by the mod" in its own comment.
- **`webDashboard.serviceAccount` removed entirely** — config keys plus the
  auto-provisioning sync code in `AuthenticationManager`. A real breaking change,
  deliberately: it was the pre-pairing auth mechanism, already documented as superseded by
  `/dashboard pair`/API keys, and keeping two working-but-redundant dashboard auth paths was
  exactly the kind of confusion this cleanup targets.
- **`kits.json`'s per-kit `commands` array now actually executes.** It was defined in the
  schema and every sample kit but silently never read — `Kit.java` parsed everything else but
  ignored it entirely. `KitManager` now runs each command as console
  (`server.execute`/`performPrefixedCommand`, same pattern `TaskScheduler` already uses)
  right after a kit's items are handed over, with `{player}` replaced by the claiming
  player's name.
- `config.json`'s `_configVersion` bumped to 32, `discord_auth.json`'s to 10.

---

## [1.0.3+build.23] — 2026-07-18

### ✨ Vault API Extracted Into a Standalone Module + First Real Economy Bridge

- **`vault/api` (`VaultServiceRegistry`/`VaultEconomy`/`VaultPermission`/`VaultChat`) is now
  its own Gradle subproject (`:vault-api`)**, published independently via JitPack
  (`com.github.ZeroG-Network-PTY-LTD:NeoEssentials:vault-api-1.0.3`) — see the new
  `docs/VaultAPI.md`. This registry was always architecturally sound (a NeoForge
  reimplementation of Bukkit's Vault concept), but had zero real-world adoption since no
  other mod could target it without depending on all of NeoEssentials. Same package, so
  nothing in the main mod needed to change; its classes are still bundled into the shipped
  jar exactly as before (build size/contents unchanged).
- **Added `SGEconomyAdapter`**, the first real third-party economy bridge, for
  [SG Economy API](https://www.curseforge.com/minecraft/mc-mods/sg-economy-api). Gated
  behind new `economy.useExternalEconomy` (default `false` — opt-in, since silently
  switching which mod controls player money is higher-stakes than the equivalent
  external-permissions toggle). When enabled and SG Economy API is detected installed, it's
  registered into the Vault registry at `HIGH` priority, taking over from the built-in
  economy automatically. Note: SG Economy API's own public API only works with online
  players (no offline-balance lookup exists in its API at all), so the adapter returns
  conservative failure/zero for offline players rather than guessing.

---

## [1.0.3+build.22] — 2026-07-18

### 🧹 Dashboard Command Cleanup + Pairing URL Fix

- **Fixed `/dashboard pair` being unable to accept a URL argument at all** —
  `dashboardUrl` used `StringArgumentType.word()`, which never accepts `:` or `/` in any
  form, quoted or not. Every real URL contains both, so the command was completely unusable
  as shipped. Switched to `StringArgumentType.string()`; usage is now
  `/dashboard pair "http://host:port" <code>` (URL must be quoted).
- **Removed the vestigial bundled-UI file-sync subsystem** — `/dashboard update`/`check`/
  `force` and the "Files: build.X → build.Y available" status line were leftovers from when
  the mod shipped its own bundled dashboard HTML/JS/CSS. There's no bundled UI anymore
  (external-only, REST + WebSocket), so this had nothing to ever find or copy. Deleted
  `DashboardFileManager` entirely; reworded the remaining status/start/stop/restart output to
  describe "the API server" instead of "the dashboard," since there's no browsable UI to
  imply.

---

## [1.0.3+build.21] — 2026-07-17

### ✨ One-Command Dashboard Pairing Handshake

Previously, connecting an external dashboard required an admin to run `/apikey create`,
hand-copy the token into the dashboard's config, and separately configure a matching
webhook secret on both sides for the reverse direction — two manual steps kept in sync by
hand, with nothing that verified both ends actually agreed.

- **`/dashboard pair <dashboardUrl> <code>` now completes both directions in one round
  trip:** the mod mints an API key and hands it to the dashboard (for the dashboard's own
  outbound calls), and the dashboard mints a token back (for the mod's outbound user-sync
  webhook), replacing the old HMAC-signature scheme so both directions share one trust
  model — a token the receiving side generated for the calling side.
- **`/dashboard unpair`** reverses it cleanly, revoking the minted key.

---

## [1.0.3+build.20] — 2026-07-17

### 🐛 LuckPerms Primary Group Now Actually Affects Chat Format & Tablist

- **Root cause:** `hasPermission()`/`getGroupWeight()` already checked the external
  permission adapter (LuckPerms) first, but nothing resolved a player's *group name*
  through it. `ChatHandler` and `TablistManager` both read straight from the internal
  `PermissionManager`, which silently returned the internal default group whenever LuckPerms
  was in charge — so `group:<name>` chat formats and per-group tablist styling never
  matched a player's real LuckPerms primary group.
- **Fix:** added `ExternalPermissionAdapter.getPrimaryGroup()`, implemented in
  `LuckPermsAdapter`, and a `PermissionAPI.getPrimaryGroup()` facade mirroring the existing
  `getGroupWeight()` external-then-internal fallback pattern. `ChatHandler` and
  `TablistManager.getPermissionGroup()` now call it.

---

## [1.0.3+build.19] — 2026-07-17

### ✨ API Keys, Locked-Down Config Endpoints, and Dashboard Account Sync

The internal Laravel dashboard is retired in favor of an external dashboard built directly
against this REST API — these three changes are the missing pieces for that.

- **API key authentication:** new `ApiKeyManager` — long-lived, non-expiring, independently
  revocable credentials for service-to-service auth, stored as a salted PBKDF2 hash (same
  scheme as dashboard passwords), shown exactly once at creation. `/apikey create|list|revoke`
  in-game (console/OP only, so a key can always be created/revoked even if every dashboard
  session is locked out) and `/api/apikeys` (GET/POST/DELETE, ADMIN-gated) over REST. Wired
  into `DashboardAPI`'s existing `withAuth()` as a third Bearer-token type alongside sessions,
  so all ~28 existing endpoint handlers gained API-key support automatically.
- **Locked down `motd`/`rules`/`shops`/`holograms`/`warps` to ADMIN for writes** — these
  previously had no role check at all, so any authenticated identity (including a
  VIEWER-role key) could mutate server-facing MOTD, rules, shop prices, holograms, and warps.
  Reads stay open to any authenticated caller.
- **Dashboard account sync, both directions:** `POST /api/users/sync` (idempotent
  create-or-update, for an external dashboard pushing its own accounts into the mod) and
  `DashboardUserSyncWebhook` (an outbound, HMAC-signed notification fired whenever a
  dashboard account changes through any path — disabled by default, fire-and-forget).
- Added `docs/API.md` — a complete reference for every REST endpoint (auth tier,
  request/response shapes, WebSocket protocol) — and refreshed `docs/Wiki/APISystem.md`/
  `WebDashboard.md` to match the external-only reality (correcting several stale claims:
  the fictional "admin/admin123 default password," "moderator or admin" on moderation writes
  which is actually ADMIN-only, and the non-existent bundled-UI "Dashboard Pages" section).

---

## [1.0.3+build.18] — 2026-07-17

### 🐛 Ban/Kick Tracking Restored, Discord Channel Sending, Permission Group Management

- **Fixed bans and kicks being invisible to the mod entirely.** Vanilla's built-in
  `/ban`/`/kick`/`/banlist`/`/pardon`/`/pardon-ip` were never removed from the command
  dispatcher, and Brigadier's same-name node merging meant vanilla silently handled these
  instead of `BanManager`/`KickManager` — every ban landed straight in vanilla's
  `banned-players.json`, invisible to the dashboard and `/modhistory`. Also fixed `/ban`'s
  player-name argument using `greedyString()` instead of `word()`, which swallowed the
  reason text into the player name and always fell back to the default ban reason.
- **Added `/modhistory`** (alias `/history`) — in-game view of a player's full
  ban/mute/kick/warn record, mirroring the dashboard's public moderation lookup.
- **Discord channel-ID test messages actually send now.** The dashboard's "send test
  message" endpoint was a stub that only logged and faked success; it now delivers to the
  given channel ID for real via `ChatIntegrationAdapter.sendToChannel()` (implemented in
  `SDLinkAdapter` through SDLink's own JDA session), and validates the input is a real
  channel snowflake, not a name.
- **Fixed permission group deletion silently doing nothing** —
  `PermissionEndpoint`'s delete call compared a `String` against `PermissionGroup` objects,
  always failing to match. Added `PermissionManager.removeGroup()`/`renameGroup()`, and
  exposed group priority/inheritance (already persisted, never read back) through the API.
  Permission endpoints now resolve offline players via profile cache + Mojang API fallback
  instead of online-only lookups.
- Fixed a jar-packaging failure (`duplicate META-INF/jarjar entries`) introduced by the
  storage-backend dev-mode fix, caught only by CI's full `./gradlew build` (local testing
  had only run `compileJava`).

---

## [1.0.3+build.17] — 2026-07-16

### 🐛 Storage Backend Fix + 9 Bugs From a Full Command Audit

- **Fixed the SQLite storage backend silently no-op'ing on every read/write in dev.**
  JarJar-embedded dependencies (`sqlite-jdbc`, `mysql-connector-j`, `HikariCP`, `snakeyaml`,
  `Java-WebSocket`) only landed in the packaged `build/libs` jar, never in the exploded
  classes/resources directories `runServer` actually loads from, so FML's JarJar locator
  never found `org.sqlite.JDBC`. This also silently blocked permission group assignment,
  which depends on the same storage layer.
- Also fixed, per a full command audit: `/baltop` cache lost-update race on rapid balance
  changes, `/setworth` rejecting namespaced item IDs, `/invseeedit` using an unregistered
  `MenuType`, jail auto-ban thresholds being unreachable and unannounced, `/chatformat`
  never being registered, the `/mute` family rejecting console/RCON, `/gc` colliding with
  the global-chat alias, `/me` conflicting with vanilla's built-in command node, and removed
  the undocumented `/hub` alias for `/spawn`.

---

## [1.0.3+build.16] — 2026-07-16

### ✨ `/sudo` Chat & Wildcard Targets + Per-Branch CI Releases

- **`/sudo <player> c:<message>`** forces the player to publicly say the message, posted as
  a real `ServerChatEvent` (going through the mod's full chat pipeline — formatting,
  mute/freeze/anti-spam checks, Discord relay — instead of a raw broadcast).
- **`/sudo * <command>`** targets every online player except the sender; `/sudo **` includes
  the sender too. Exempt players (`neoessentials.sudo.exempt`) are silently skipped in bulk
  mode instead of aborting the whole batch.
- Relaxed `GET /api/discord/auth-config` from ADMIN to AUTH tier — every field in the
  response is a non-secret boolean/enum, and requiring ADMIN broke the dashboard's
  MODERATOR-tier service account checking "is Discord login even possible."
- Added a CI workflow that builds the mod jar and publishes it to a per-branch GitHub
  Release on every push — no local Gradle build required to grab the latest build.

---

## [1.0.3+build.15] — 2026-07-15

### ✨ Real Discord Companion-Mod Integrations (SDLink, Mc2Discord, DCIntegration)

The mod never opened its own connections to Discord, but its chat-relay adapters used to
target reflection against guessed (and wrong) class/method names, and the dashboard's
"Login with Discord" performed its own OAuth2 exchange straight against discord.com — both
against the goal of always delegating actual Discord communication to a real companion mod.

- Replaced the 3 reflection-based chat adapters with 2 real compile-time integrations
  (SDLink and Mc2Discord's actual public API classes), still runtime-optional via `ModList`.
  Dropped DiscordSRV support entirely — it's a Bukkit/Paper plugin and can't run on NeoForge.
- Added a real `DCIntegrationAdapter` (`de.erdbeerbaerlp.dcintegration`), restoring the third
  companion mod, against its real API rather than reflection.
- Dashboard "Login with Discord" is now identity-lookup only — a player links their account
  in-game via a companion mod's own commands, and the dashboard reads that link. No direct
  network calls to Discord anywhere in the mod. Added the reverse lookup
  (Discord ID → linked Minecraft account) this requires.
- `webDashboard.mode` now defaults to `"external"` (REST-only) — no built-in dashboard UI has
  ever actually shipped with the mod; the old default silently logged a scary (but harmless)
  "Dashboard resources NOT found" error on every request to `/`.

---

## [1.0.3+build.14] — 2026-07-14

### ✨ Module/Command Toggles Actually Work, Public Moderation Lookup API, Dashboard Command Wiring Fixed

- **Fixed `config.json`'s module/command enable-disable toggles doing nothing in most
  cases** — most `modules` flags were only checked by *some* of their commands, and ~45
  command-registration files never checked their own `commands.json` entry at all. Every
  affected command now checks both flags before registering. Added 4 new module flags
  (`hologramsEnabled`/`shopEnabled`/`auctionHouseEnabled`/`vaultEnabled`) that previously had
  no kill-switch at all, plus `tablistEnabled`/`resourcePacksEnabled`/`playerTagsEnabled`/
  `discordIntegrationEnabled` for subsystems that had none before either. Fixed two
  pre-existing double-registration bugs (`/pt`, `/enchanthand`) and deleted two dead
  duplicate command classes. **Note: toggling any of these still requires a restart, not
  just `/neoe reload`** — Brigadier commands and one-time manager `initialize()` calls can't
  be added/removed at runtime; `/neoe reload` now says so explicitly.
- **New public (no-login) moderation lookup:** `GET /api/public/moderation/lookup/{name}`
  and `/recent`, matching ban-management plugins' public transparency page. Deliberately
  excludes IP bans/mutes, staff notes, and player reports. Gated by
  `webDashboard.securitySettings.publicModerationLookupEnabled` (default on).
- **Fixed `/dashboard` and `/dashboardregister` being completely dead code** — both command
  classes existed and compiled fine but were never actually registered with the command
  dispatcher, so typing them in-game did nothing at all.
- Added `webDashboard.mode` (`internal`/`external`/`both`) and a public, unauthenticated
  `GET /api/ping` reachability check — lets an external dashboard confirm "can I reach this
  port at all" independently of whether login/auth succeeds.

---

## [1.0.3+build.10] — 2026-07-12

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

## [1.0.3+build.9] — 2026-07-12

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
