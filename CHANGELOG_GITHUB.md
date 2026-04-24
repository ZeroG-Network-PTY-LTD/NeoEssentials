# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## [1.0.2.6+build.46] — 2026-04-24

### Bug Fix — Web Dashboard Admin Controls & Permissions Page Blank After Login

Navigating to `admin.html` or `permissions.html` after logging in on `index.html` would show a
blank page. Pressing F5 would briefly reveal the buttons before they disappeared again.

**Root causes:**

1. **`showLoginScreen()` has no redirect on sub-pages.**  
   On `index.html`, `showLoginScreen()` hides `dashboardWrapper` and shows `loginContainer`.
   On `admin.html` and `permissions.html` there is no `loginContainer`, so only `dashboardWrapper`
   was hidden — leaving the user with a completely blank page and no way to log back in.
   If anything caused the auth check or any subsequent `fetchWithAuth` call to fail (expired
   session, server reload clearing in-memory sessions, transient network error), the page went
   blank silently. The brief flash visible on hard-refresh was the HTML rendering before the
   async auth check completed.

2. **`permissions.js` never initialised on `permissions.html`.**  
   The init guard at the bottom of `permissions.js` checked
   `window.location.hash === '#permissions'` or
   `document.querySelector('[data-page="permissions"].active')`.
   Neither condition is ever true on the standalone `permissions.html` page, so
   `initPermissionSystem()` was never called — all permission tabs showed their "Loading…"
   placeholder forever.

3. **Multiple `fetchWithAuth` calls in `permissions.js` were missing `.json()`.**  
   `viewGroupPermissions`, `editUserPermissions`, `submitEditGroup`, `addGroupPermission`,
   `removeGroupPermission`, `deleteGroup`, `addUserPermission`, `removeUserPermission`, and
   `submitChangeGroup` all called `fetchWithAuth(...)` and then checked `response.success` or
   read `response.group` etc. directly on the raw `Response` object (which has no `.success`
   property). Every modal action silently failed instead of succeeding.

4. **Username not shown in topbar on sub-pages.**  
   `showDashboard()` only looked for `id="usernameDisplay"` (exists on `index.html`).
   `admin.html` and `permissions.html` use `id="userName"`, so the username was always "Guest"
   on those pages.

**Changes:**

| File | Change |
|---|---|
| `dashboard.js` | `showLoginScreen()`: when `loginContainer` is absent (sub-pages), redirect to `index.html` instead of just hiding `dashboardWrapper`. |
| `dashboard.js` | `showDashboard()`: username display now tries `id="usernameDisplay"` first, then falls back to `id="userName"`, so the topbar shows the correct username on both index and sub-pages. |
| `dashboard.js` | Version string bumped to Build 418 (cache-bust). |
| `permissions.js` | Replaced the unreliable hash/data-page init guard with `tryInitPermissions()` — checks for `id="permOverviewTab"` which is always present on `permissions.html`. |
| `permissions.js` | Added `.json()` parsing to the raw `Response` in `viewGroupPermissions`, `editUserPermissions`, `submitEditGroup`, `addGroupPermission`, `removeGroupPermission`, `deleteGroup`, `addUserPermission`, `removeUserPermission`, and `submitChangeGroup`. |
| `admin.html`, `permissions.html`, `index.html` | Script `?v=` cache-bust query bumped to `418`. |

---

## [1.0.2.6+build.44] — 2026-04-24


### Bug Fix — `/sethome` and `/delhome` Confirmation Buttons Append "confirm" to Home Name

Clicking the `[Confirm]` button on a `/sethome <name>` overwrite prompt or a `/delhome` deletion
prompt failed with *"Invalid home name: Colony confirm"*. Each subsequent click appended another
`" confirm"`, producing names like *"Colony confirm confirm confirm"*. The action never completed.

**Root cause:**

The `confirm` and `deny` literals were registered as Brigadier **child nodes of the `<name>`
word-argument** (`/sethome <name> confirm`). The confirmation button's `RUN_COMMAND` click event
sent `/sethome Colony confirm`. In Minecraft 1.21+, when the client processes a `RUN_COMMAND` string
it re-validates the command against the client-side Brigadier tree that the server sent via
`ClientboundCommandsPacket`. The client-side tree does not correctly represent the nested literal
structure, so the full remaining input `"Colony confirm"` is consumed as a single word-argument
value. The server then receives `"Colony confirm"` as the `name` argument, `setHome()` rejects it
(space not allowed), the confirmation prompt is re-shown with `"Colony confirm"` as the new pending
name, and the loop repeats on every click.

**Changes:**

| File | Change |
|---|---|
| `HomeCommands.java` | Moved `confirm` and `deny` from being Brigadier children of `<name>` to **top-level literal siblings** under `sethome`/`createhome` and `delhome`/`deletehome`/`removehome`/`rhome`. Brigadier gives literals priority over argument nodes, so `/sethome confirm` routes to the handler while `/sethome Colony` routes to the name argument. |
| `HomeCommands.java` | Updated `executeSetHomeConfirm`, `executeSetHomeDeny`, `executeDelHomeConfirm`, `executeDelHomeDeny` to retrieve the pending home name from the server-side pending map instead of parsing it from command arguments. Removed all `StringArgumentType.getString(context, "name")` calls from the four handlers. |
| `HomeCommands.java` | Updated `executeSetHome` and `executeDelHome` to emit clean buttons (`/sethome confirm` / `/sethome deny`, `/delhome confirm` / `/delhome deny`) instead of embedding the home name in the button command. |
| `en_us.json` | Fixed `delete_success`, `delete_cancelled`, `delete_failed`, `overwrite_success`, `overwrite_failed` to use `{0}` (valid `MessageFormat` pattern) instead of `{HOME}`/`{home}` (were silently unsubstituted). Added `overwrite_already_pending`, `no_pending_overwrite_generic`, `delete_already_pending`, `delete_no_pending_generic`, `delete_no_confirm_required`, `limit_exceeded` keys. |
| `MessageUtil.java` | `CURRENT_LANG_VERSION` bumped `11 → 12`; new keys are auto-merged into existing server language files on next startup. |

---

## [1.0.2.6+build.42] — 2026-04-24

### Bug Fix — `/back` Fails with "No Safe Teleport Location Found" in Unloaded Chunks

Using `/back` (to return to a death point or previous location) failed whenever the target
was in an unloaded chunk, producing the error *"No safe teleport location found"* even when
the destination was perfectly valid.

**Root causes:**

1. **`TeleportUtil` only force-loaded the single target chunk.** `findSafeLocation()` scans
   up to ±16 blocks in X/Z from the target, which can cross into neighbouring chunks.
   Those neighbouring chunks were never loaded, so every candidate position inside them
   returned `false` from `level.isLoaded(pos)` → `isSafe()` = false →
   `findSafeLocation()` returned `null` → teleport failed.

2. **`MiscTeleportManager.teleportDelay` was hardcoded to `3`.** The field was never
   populated from config (`teleportation.backSettings.teleportDelay` /
   `teleportation.generalSettings.teleportDelay`), so the configured warm-up delay was
   silently ignored for all `/back` and implicit death-back teleports.

**Changes:**

| File | Change |
|---|---|
| `TeleportUtil.java` | Added `preloadChunksForTeleport(ServerLevel, BlockPos)` — loads a 3×3 chunk grid (target + 8 neighbours) with `PORTAL` tickets before any `isSafe()` / `findSafeLocation()` call runs. Second call added after `findSafeLocation()` resolves to ensure the safe-landing chunk is also loaded. All `teleportPlayer()` paths benefit automatically. |
| `ConfigManager.java` | Added `getBackTeleportDelay()` (reads `teleportation.backSettings.teleportDelay`, falls back to `generalSettings.teleportDelay`, default 3), `isDeathBackEnabled()`, and `isTeleportBackEnabled()`. |
| `MiscTeleportManager.java` | Added `loadConfig()` — reads `teleportDelay`, `enableDeathBack`, and `enableTeleportBack` from `ConfigManager`; called at construction so config values are honoured from the first use. |

---

## [1.0.2.6+build.41] — 2026-04-24

### Bug Fix — Vanish Module Cannot Be Disabled

Disabling the vanish module via `moderation.vanishSettings.enableVanishSystem: false` had no effect.
Commands remained registered and interaction prevention kept blocking previously-vanished players even
after the flag was set.

**Root causes:**

1. **Wrong config path in `ConfigManager.isVanishSystemEnabled()`** — the method checked for
   `enableVanishSystem` at the root of `config.json`, but the key lives at
   `moderation.vanishSettings.enableVanishSystem`. The root-level key was never present, so the method
   *always* returned `true`.

2. **`ModerationEventHandler` vanish interaction guards did not check `isVanishSystemEnabled()`** —
   even with the config flag corrected, players who were already vanished would still have block-break /
   block-place / item-use interactions cancelled because the guards only checked
   `isVanishPreventInteractionEnabled()`, not whether the vanish *system* was enabled.

3. **`VanishManager.onPlayerJoin()` was never called** — the method that restores a vanished player's
   tab-list hidden state on reconnect and sends the "you are vanished" reminder was defined but had no
   call-site. Re-join behaviour was therefore broken regardless of whether vanish was enabled.

**Changes:**

| File | Change |
|---|---|
| `ConfigManager.java` | Fixed `isVanishSystemEnabled()` to read `moderation.vanishSettings.enableVanishSystem` instead of root-level `enableVanishSystem` |
| `ModerationEventHandler.java` | Added `isVanishSystemEnabled()` guard to all three vanish interaction-prevention blocks (`onPlayerRightClick`, `onBlockBreak`, `onBlockPlace`) |
| `ModerationEventHandler.java` | Added `VanishManager.onPlayerJoin()` call in `onPlayerLogin`, gated by `isVanishSystemEnabled()`, so vanish state is correctly restored and the vanish reminder is shown on reconnect |

---



### Security Fix — `/inv` and `/ec` Bypass Permission Checks

`/inv` and `/ec` (aliases for `/invsee` and `/enderchest`) were accessible by **all** players regardless of
permission because they were registered as Brigadier `redirect()` nodes with no `requires()` predicate.
Brigadier does **not** re-evaluate the redirect target's `requires()` for the alias node itself —
only the alias's own predicate is checked at dispatch time. Since the aliases had none, every player
could open any other player's inventory.

**Changes:**

| File | Change |
|---|---|
| `InventoryViewCommands.java` | Replaced all `redirect()`-based aliases (`/inv`, `/ec`, `/ecedit`) with full command registrations that include their own `requires()` predicate |
| `InventoryViewCommands.java` | Fixed typo: `"enderchestdit"` → `"enderchestedit"` (prevented `/ecedit` from working) |
| `InventoryViewCommands.java` | Replaced hardcoded message strings with proper `MessageUtil` translation key calls |
| `permissions.json` | Added `neoessentials.invsee` and `neoessentials.enderchest` to the `moderator` group |
| `en_us.json` | Added `commands.neoessentials.invsee.*` and `commands.neoessentials.ec.*` message keys |

**Permission nodes:**

| Node | Description | Default group |
|---|---|---|
| `neoessentials.invsee` | View another player's inventory (read-only) | moderator |
| `neoessentials.invsee.edit` | View and edit another player's inventory | admin |
| `neoessentials.enderchest` | View another player's ender chest (read-only) | moderator |
| `neoessentials.enderchest.edit` | View and edit another player's ender chest | admin |

---

## [1.0.2.6+build.38] — 2026-04-24


### Bug Fix — Teleportation Message Keys & Cooldown/Warmup System

Fixes two related teleportation issues reported on build 1.0.2.6+21.

#### Fix 1 — Raw Translation Keys Displayed to Players

Previously, teleportation messages related to `/spawn` (and its fallback path to world spawn) would
show raw translation key strings like `commands.neoessentials.teleport.spawn.fallback_success` in
chat instead of the correct localized message. This happened because the entire
`commands.neoessentials.teleport.spawn.*` key group was missing from `en_us.json`, and
`MessageUtil.localize()` returns the raw key when no entry is found.

**Keys added to `en_us.json`:**
- `teleport.spawn.success`, `teleport.spawn.fallback_success`, `teleport.spawn.failed`, `teleport.spawn.fallback_failed`
- `teleport.spawn.cleared`, `teleport.spawn.set`, `teleport.spawn.info`, `teleport.spawn.info_not_set`
- `teleport.spawn.invalid_location`, `teleport.spawn.no_nether`, `teleport.spawn.no_end`
- `teleport.spawn.unsafe`, `teleport.spawn.unsafe_location`, `teleport.spawn.moved_to_safety`
- `teleport.spawn.critical_failure`, `teleport.spawn.distance_exceeded`
- `teleport.spawn.cooldown`, `teleport.spawn.warmup`
- `teleport.warp.cooldown`, `teleport.warp.warmup`
- `teleport.home.warmup`

`_langVersion` bumped from `10` → `11`; `CURRENT_LANG_VERSION` constant updated in `MessageUtil.java`.
Existing server deployments will auto-merge all new keys on the next startup without overwriting user edits.

#### Fix 2 — Teleport Cooldowns & Warmup Delays Not Applied

Multiple root causes prevented cooldowns and warmup delays from working:

| Manager | Problem | Fix |
|---|---|---|
| `HomeManager` | `teleportDelay` hardcoded to `3`, never read from config | Now reads `teleportation.generalSettings.teleportDelay` |
| `HomeManager` | `homeTeleportCooldownSeconds` read from config but never checked | Cooldown check added in `teleportToHome()` with `lastHomeTeleportTimestamps` |
| `WarpManager` | `warpCooldown` config key ignored — no use-cooldown enforcement | Added `warpUseCooldown` field + `lastWarpUseTimestamps`, enforced in `teleportToWarp()` |
| `SpawnManager` | `spawnCooldown` config key ignored — no cooldown enforcement | Added `spawnCooldownSeconds` field + `lastSpawnTimestamps`, enforced in `teleportToSpawn()` |
| `SpawnManager` | `loadSpawn()` read `teleportDelay: 0` from spawn.json, overriding config.json value | Removed `teleportDelay` from spawn.json loading; now driven exclusively by `generalSettings.teleportDelay` |
| All managers | No warmup countdown message shown to players | Warmup message sent when `teleportDelay > 0` and `enableTeleportWarmup=true` |

**Config keys now fully wired up:**
- `teleportation.generalSettings.teleportDelay` — warmup delay for home/spawn teleports (default: `3` seconds)
- `teleportation.homeSettings.homeTeleportCooldown` — home use cooldown (default: `5` seconds)
- `teleportation.warpSettings.warpCooldown` — warp use cooldown (default: `10` seconds)
- `teleportation.spawnSettings.spawnCooldown` — spawn use cooldown (default: `5` seconds)
- `teleportation.generalSettings.enableTeleportWarmup` — whether to show countdown message (default: `true`)

---



### Feature — Permissions System Improvements (Part 2): GUI, External Systems & Fine-Grained Control

Completes the Permissions System Improvements milestone with three remaining items.

#### GUI Management — Web Dashboard REST API (extended)

The existing `/api/permissions` endpoint has been extended with full support for contextual
permissions, temporary permissions, aliases, and a reload action.

**New endpoints added to `/api/permissions`:**

| Method | Path | Description |
|---|---|---|
| `POST` | `/reload` | Reload all permissions from disk |
| `GET` | `/system/status` | Enhanced — now includes emergency mode, adapter health, adapter version, consecutive failures, alias count |
| `GET/POST/DELETE` | `/group/{name}/context` | Manage group contextual overrides (`{contextKey, node, allow}`) |
| `GET/POST` | `/group/{name}/temp` | List / add group temp permissions (`{node, duration}`) |
| `DELETE` | `/group/{name}/temp/{node}` | Remove group temp permission |
| `GET/POST/DELETE` | `/user/{name}/context` | Manage user contextual overrides |
| `GET/POST` | `/user/{name}/temp` | List / add user temp permissions |
| `DELETE` | `/user/{name}/temp/{node}` | Remove user temp permission |
| `GET` | `/aliases` | List all registered permission aliases |
| `POST` | `/aliases` | Register alias `{alias, canonical}` — persists to `permission_aliases.json` |
| `DELETE` | `/aliases/{alias}` | Remove alias |

All endpoints return `{success: true/false, message?, ...}` JSON and require Bearer auth.

#### Integration with External Systems — Improved Documentation & Fallback

- **Compatibility report** documented: logged at every startup with adapter name, version, health, and a `⚠ NEWER THAN TESTED` warning when the installed version exceeds last-tested.
- **Full fallback chain** documented: emergency mode → OP bypass → external adapter → internal `permissions.json` → vanilla-OP fallback. Adapter health tracking (5 consecutive failures → `UNHEALTHY`, fallback activates) documented.
- **LuckPerms**: context-aware check via live `QueryOptions` documented; step-by-step setup guide added.
- **FTB Ranks**: 4-API-signature probe for version compatibility documented.
- **Compatibility table** added: LuckPerms 5.4.x, FTB Ranks 2101.1.3, WorldEdit (any), FTB Chunks (any), any NeoForge-`PermissionAPI` mod.

#### Fine-Grained Command Control — Per-Subcommand Permission Nodes

Every Brigadier branch in every NeoEssentials command tree has its own permission node.
Documented with per-system tables covering:

- **Home**: `.home`, `.home.set`, `.home.delete`, `.home.list`, `.home.others`
- **Warp**: `.warp`, `.warp.others`, `.warp.create`, `.warp.delete`, `.warp.list`
- **Kit**: `.kits.use`, `.kits.<name>`, `.kit.others`, `.kits.admin.create/delete`, `.kitreset`, `.kitreset.others`
- **Economy**: `.balance`, `.balance.others`, `.pay`, `.pay.offline`, `.economy.eco`
- **Moderation**: `.ban`, `.banip`, `.tempban`, `.jail`, `.jail.timed`, `.vanish`, `.vanish.others`
- **Permission system**: full sub-node table for every `/permissions` action

Full tables in [PermissionSystem.md — Fine-Grained Command Control](docs/Wiki/PermissionSystem.md#fine-grained-command-control).

---

## [1.0.2.6+build.28] — 2026-04-01

### Feature — Permissions System Improvements

Complete overhaul of the permissions subsystem with contextual overrides, condition expressions, a clean mod-interop API, alias resolution, and persistent storage of all new data.

#### Contextual Permissions

Grant or deny a permission node only when the player is in a specific world, time-of-day, or gamemode. Contextual rules are layered on top of the regular permission resolution chain — context denies always win, context grants are checked before regular grants.

```
/permissions group <group> context add <contextKey> <node> allow|deny
/permissions group <group> context remove <contextKey> <node>
/permissions group <group> context list

/permissions user <player> context add <contextKey> <node> allow|deny
/permissions user <player> context remove <contextKey> <node>
/permissions user <player> context list
```

Supported context keys (with tab-completion):

| Key | Meaning |
|---|---|
| `world:overworld` / `world:the_nether` / `world:the_end` | Current dimension |
| `time:day` | Day phase (ticks 0–12 999) |
| `time:night` | Night phase (ticks 13 000–23 999) |
| `gamemode:survival` / `gamemode:creative` / `gamemode:spectator` / `gamemode:adventure` | Player gamemode |

#### Permission Conditions

Optional runtime conditions can be attached to any permission node on a user or group. When the permission would otherwise be granted, the condition is re-evaluated; if it fails, the grant is withheld.

Condition syntax:
```
time:day
gamemode:survival AND time:day
world:overworld OR world:the_nether
health:above:10
op:true
```

Supports `AND` / `OR` compound expressions with atoms: `time:day`, `time:night`, `world:<name>`, `gamemode:<mode>`, `health:above:<n>`, `health:below:<n>`, `op:true`, `op:false`.

#### Permission Aliases

Map legacy or short node names to their canonical NeoEssentials equivalents via `config/neoessentials/permission_aliases.json`. Aliases are resolved transparently in every permission check.

Example `permission_aliases.json`:
```json
{
  "essentials.fly": "neoessentials.fly",
  "essentials.warp": "neoessentials.teleport.warp",
  "efly": "neoessentials.fly"
}
```

#### API for Other Mods — `PermissionsService`

Other NeoForge mods can now interact with NeoEssentials permissions without importing internal classes:

```java
PermissionsService perms = NeoEssentialsAPI.getPermissionsService();

// Simple check
boolean canFly = perms.hasPermission(player, "neoessentials.fly");

// Context-aware check
PermissionContext ctx = perms.contextFor(player);
boolean granted = perms.hasPermission(player.getUUID(), "mymod.feature", ctx);

// Register your mod's own nodes (appear in /permissions search)
perms.registerPermission("mymod.feature", "Enables the feature");
perms.registerPermissions(Map.of("mymod.a", "...", "mymod.b", "..."));

// Register a legacy alias
perms.registerAlias("essentials.fly", "neoessentials.fly");
```

Full method list: `hasPermission`, `getGroup`, `getPrefix`, `getSuffix`, `registerPermission`, `registerPermissions`, `registerAlias`, `getAliases`, `isEmergencyMode`, `isUsingExternalAdapter`, `getGroupNames`, `getPlayerPermissions`, `contextFor`.

#### Storage

Contextual permissions and conditions are now persisted in `permissions.json` (groups) and `permissions/playerdata.json` (users). Existing files are backward-compatible — no migration required.

#### Audit Log

New action constants written to `permissions_audit.log`:
`USER_CONTEXT_PERM_ADDED`, `USER_CONTEXT_PERM_REMOVED`, `GROUP_CONTEXT_PERM_ADDED`, `GROUP_CONTEXT_PERM_REMOVED`, `USER_CONDITION_SET`, `USER_CONDITION_REMOVED`, `GROUP_CONDITION_SET`, `GROUP_CONDITION_REMOVED`

#### New Permission Nodes

| Node | Default | Description |
|---|---|---|
| `neoessentials.permissions.user.context` | OP only | Manage contextual overrides for users |
| `neoessentials.permissions.group.context` | OP only | Manage contextual overrides for groups |

#### Internal changes

- `PermissionManager.hasPermission(UUID, String, PermissionContext)` — new context-aware overload; the existing `hasPermission(UUID, String)` delegates to it with `PermissionContext.EMPTY`
- `PermissionAPI.hasPermission(UUID, String, PermissionContext)` — context threaded through the full 5-step resolution chain; alias resolution runs before every check
- `PermissionStorage` — groups and users now save/load `contextualPermissions` and `conditions`
- `NeoEssentialsAPI.API_VERSION` bumped to `1.1.0`

---

## [1.0.2.6+build.26] — 2026-04-01

### Improvement — Utility Systems Audit & Polish

Audited all core utility commands for correctness, consistency, and clean registration.

#### Fixes & changes

| Area | Change |
|---|---|
| `NickCommand` | Storage path now uses `ResourceUtil.getConfigPath()` (was raw `Paths.get()`); registered `/nickname` alias via Brigadier redirect so `/nickname` works identically to `/nick` |
| `SeenCommand` | Storage path now uses `ResourceUtil.getConfigPath()` for `seen_data.json` |
| `NeoEssentials.java` | Removed duplicate `registry.registerCommand()` entries from the old "PLAYER INFO" metadata block; all player-info commands (`near`, `ping`, `seen`, `whois`, etc.) are now registered exactly once by their dedicated command classes |
| `PermissionRegistry` | Removed stale duplicate `register()` calls for `neoessentials.whois`, `neoessentials.seen`, `neoessentials.realname`, `neoessentials.near`, `neoessentials.ping`, `neoessentials.ping.others`, `neoessentials.motd`, `neoessentials.rules`, `neoessentials.suicide` — earlier duplicates incorrectly overrode canonical values (e.g. `whois` was silently changed from `ADMIN/false` to `MISC/true`); correct values are now authoritative |
| `PermissionRegistry` | `neoessentials.whois.detailed`, `neoessentials.rules.admin`, and all `neoessentials.motd.*` sub-nodes moved to their canonical positions and kept unique |

#### Commands verified as present and fully functional

`/nick` · `/nickname` · `/setnick` · `/near` · `/nearby` · `/ping` · `/depth` · `/helpop` (`/ac` `/amsg`) · `/motd` · `/rules` · `/suicide` · `/killme` · `/seen` · `/whois` · `/realname` · `/msgtoggle`

---



### New Feature — Temporary Permissions

Time-limited permission grants for both users and groups. A permission granted with a duration automatically expires and is revoked — no manual cleanup required.

#### Duration format
Combinations of `d` (days), `h` (hours), `m` (minutes), `s` (seconds):

| Example | Meaning |
|---|---|
| `30m` | 30 minutes |
| `12h` | 12 hours |
| `1d` | 1 day |
| `7d` | 7 days |
| `1d12h30m` | 1 day, 12 hours, 30 minutes |

#### New commands — users

| Command | Permission | Description |
|---|---|---|
| `/permissions user <p> addtemp <node> <duration>` | `neoessentials.permissions.user.temp` | Grant a time-limited permission to a player |
| `/permissions user <p> removetemp <node>` | `neoessentials.permissions.user.temp` | Revoke a temporary permission before it expires |
| `/permissions user <p> listtemp` | `neoessentials.permissions.info.user` | List all active temp permissions with time remaining |

#### New commands — groups

| Command | Permission | Description |
|---|---|---|
| `/permissions group <g> addtemp <node> <duration>` | `neoessentials.permissions.group.temp` | Grant a time-limited permission to a group |
| `/permissions group <g> removetemp <node>` | `neoessentials.permissions.group.temp` | Revoke a temporary group permission early |
| `/permissions group <g> listtemp` | `neoessentials.permissions.info.group` | List all active group temp permissions with time remaining |

#### Auto-expiry engine
- **Added** `PermissionExpiryHandler` — `@EventBusSubscriber` class that hooks `ServerTickEvent.Post` and calls `PermissionManager.purgeExpiredTempPermissions()` every **30 seconds** (600 ticks).
- When a temp permission expires the affected **online player** is notified with a chat message: `§eYour temporary permission §f<node>§e has expired.`
- Every expiry is written to the **audit log** as `USER_TEMP_PERM_EXPIRED` / `GROUP_TEMP_PERM_EXPIRED` with executor `SYSTEM`.
- Expired entries are **never loaded from disk** — `PermissionStorage` skips entries whose expiry timestamp has already passed on load.

#### Persistence
- **`playerdata.json`** — users gain an optional `"tempPermissions": {"<node>": <expiryMs>}` key.
- **`permissions.json`** — groups gain an optional `"tempPermissions"` key with the same format.
- Only unexpired entries are written on save; expired entries are stripped automatically.

#### Resolution order
Temp permissions are evaluated **after** negative-permission denial and **before** regular user/group permissions in the full resolution chain, so they cannot override explicit `-node` denials.

#### New permission nodes

| Node | Description |
|---|---|
| `neoessentials.permissions.user.temp` | Grant/revoke time-limited permissions for a user |
| `neoessentials.permissions.group.temp` | Grant/revoke time-limited permissions for a group |

#### New audit log events

| Action constant | Trigger |
|---|---|
| `USER_TEMP_PERM_ADDED` | `/permissions user <p> addtemp` |
| `USER_TEMP_PERM_REMOVED` | `/permissions user <p> removetemp` |
| `USER_TEMP_PERM_EXPIRED` | Auto-expiry engine (executor = `SYSTEM`) |
| `GROUP_TEMP_PERM_ADDED` | `/permissions group <g> addtemp` |
| `GROUP_TEMP_PERM_REMOVED` | `/permissions group <g> removetemp` |
| `GROUP_TEMP_PERM_EXPIRED` | Auto-expiry engine (executor = `SYSTEM`) |

---

## [1.0.2.6+build.23] — 2026-04-01 · [`48763856`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/48763856)

### New Feature — Permission Audit Logging

#### `PermissionAuditLogger` — persistent append-only audit trail
- **Added** `PermissionAuditLogger.java` — writes every permission modification to `neoessentials/permissions_audit.log` (append-only, UTF-8).
- **Log format:** `[YYYY-MM-DD HH:mm:ss UTC]  ACTION                   | executor=<name> | target=<group/player> | <detail>`
- **17 tracked events:**

  | Action constant | Trigger |
  |---|---|
  | `USER_GROUP_SET` | `/permissions user <p> setgroup <g>` |
  | `USER_PERM_ADDED` | `/permissions user <p> add <node>` |
  | `USER_PERM_REMOVED` | `/permissions user <p> remove <node>` |
  | `USER_PERMS_CLEARED` | `/permissions user <p> clear` |
  | `GROUP_CREATED` | `/permissions create group <g>` |
  | `GROUP_DELETED` | `/permissions delete group <g>` |
  | `GROUP_RENAMED` | `/permissions rename group <old> <new>` |
  | `GROUP_CLONED` | `/permissions clone group <src> <new>` |
  | `GROUP_PERM_ADDED` | `/permissions group <g> add <node>` |
  | `GROUP_PERM_REMOVED` | `/permissions group <g> remove <node>` |
  | `GROUP_PERMS_CLEARED` | `/permissions group <g> clear` |
  | `GROUP_INHERIT_ADDED` | `/permissions group <g> inherit add <p>` |
  | `GROUP_INHERIT_REMOVED` | `/permissions group <g> inherit remove <p>` |
  | `GROUP_PREFIX_SET` | `/permissions group <g> setprefix <v>` |
  | `GROUP_SUFFIX_SET` | `/permissions group <g> setsuffix <v>` |
  | `GROUP_PRIORITY_SET` | `/permissions group <g> setpriority <v>` |
  | `PERMISSIONS_RELOADED` | `/permissions reload` |

- **Executor tracking:** Commands run by online players log the player's name; console commands log `"CONSOLE"`.
- **Controlled by** `permissions.auditLogging` in `config.json` (default `true`). When `false`, all log calls are no-ops with zero overhead.

#### New config key: `permissions.auditLogging`
- **Added** to the default `config.json` template (`"auditLogging": true`).
- `ConfigManager.isPermissionAuditEnabled()` public method added.

---

## [1.0.2.6+build.22] — 2026-04-01 · [`a2e1a7ed`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/a2e1a7ed)

### Improvement — Permission Groups & Priorities + Permission Suggestions

#### Group priorities (`priority` field on every group)
- **Added** `priority` (int, default `0`) field to `PermissionGroup`. Higher values are checked **first** when resolving inherited groups, giving a deterministic order when multiple parent groups conflict.
- **Updated** `PermissionManager` — inherited groups are now sorted by `priority` descending before the recursive permission walk, for both positive-grant and negative-deny passes.
- **Updated** `PermissionStorage` — `priority` is saved/loaded in `permissions.json` (backwards-compatible: files without the key read as `0`).
- **Added** Two new commands:
  - `/permissions group <name> setpriority <value>` (−999–999) — requires `neoessentials.permissions.group.modify`
  - `/permissions group <name> getpriority` — requires `neoessentials.permissions.info.group`
- **Updated** `/permissions info group <name>` now shows the current priority in its output.
- **Updated** `/permissions debug <player>` group-chain display already renders priorities via `showGroupChain` (priority shown in group info line).
- **Added** `neoessentials.permissions.group.priority` registered in `PermissionRegistry` (description: *"Set/get group priority (used to order inheritance resolution)"*).

#### Permission Suggestions — enriched denial messages
- **Improved** `PermissionValidator.validatePermission()` denial message now looks up the required node in `PermissionRegistry` and appends its human-friendly description in a dimmed line:
  ```
  You don't have permission to use this command.
  §7Required: §fneoessentials.moderation.ban
  §8(Ban a player from the server)
  ```
- **Improved** `PermissionValidator.validateAnyPermission()` denial message similarly appends per-node descriptions for each candidate node listed.
- This makes it possible for players/staff to immediately understand *which capability* they are missing without needing to cross-reference the wiki.

#### Documentation
- **Updated** `PermissionSystem.md`:
  - New **Group Priorities** section with command table, how-it-works explanation, priority scale table, and a worked example.
  - Updated Table of Contents to include the new section.
  - Updated the example `groups.json` — all four groups (`default` 0, `vip` 10, `moderator` 50, `admin` 100) now include their `priority` field.
  - Updated the "if denied" description to show the enriched message format.
- **Updated** `CommandsReference.md` — added `setpriority` and `getpriority` rows to the Permissions Management table.

---

## [1.0.2.6+build.21] — 2026-04-01 · [`81c7a55d`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/81c7a55d)

### New Feature — Permission Debugging Tools

#### `/permissions debug <player>` — full permission resolution trace
- **Added** New `debug` subcommand to `/permissions` (requires `neoessentials.permissions.debug`).
- Displays a complete diagnostic trace for any player without needing to enable debug logging:
  - **System mode** — Internal, External adapter name, or EMERGENCY (OP-only)
  - **Adapter health** — healthy / UNHEALTHY (with consecutive failure count) and detected version
  - **Active config flags** — `opsBypassPermissions` and `vanillaOpFallback` on/off
  - **OP status** — checks live `ServerPlayer` (online) or `ProfileCache` (offline)
  - **Assigned group** and every **direct user permission** node (up to 10, with overflow count)
  - **Group inheritance chain** — recursive tree with indentation, up to 8 permissions per group with overflow count, prefix shown inline
  - **Resolution chain summary** — numbered 4-step walkthrough showing which step would GRANT or continue for this specific player, based on current config and OP status
- Result: admins can diagnose "why does player X not have permission Y" entirely in-game without touching logs.

#### `neoessentials.permissions.debug` — new permission node
- **Added** Registered in `PermissionRegistry` between the existing `check` and `search` nodes.

#### Bug fix — `checkUserPermission` full-chain bypass
- **Fixed** `checkUserPermission()` inside `PermissionsCommand` was calling `PermissionAPI.getManager().hasPermission(uuid, node)` directly, which silently bypassed:
  - `opsBypassPermissions` fast-path
  - The external adapter (LuckPerms / FTB Ranks)
  - `vanillaOpFallback` last resort
- **Fixed** Now calls `PermissionAPI.hasPermission(uuid, node)` — the full 5-step resolution chain — so that the in-game `/permissions user check` result is consistent with what actually happens at runtime.

---

## [1.0.2.6+build.19] — 2026-04-01 · [`a22d0323`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/a22d0323)

### Documentation — `allowUnsafeCommands` & Security Configuration

#### `SplitConfigs.md` — Security Configuration section (complete rewrite)
- **Fixed** The previous `allowUnsafeCommands` entry had the **wrong description** ("Allow enchantments and item operations beyond vanilla limits") — that describes `items.unsafe-enchantments`, not the security command filter.  Corrected to accurately reflect what the option does.
- **Added** Full `security.json` / `config.json → security` reference table covering all six keys: `enableInputValidation`, `maxCommandLength`, `maxReasonLength`, `allowUnsafeCommands`, `enablePathTraversalProtection`, `enableXSSProtection`.
- **Added** Detailed `allowUnsafeCommands` breakdown including:
  - Every blocked substring (with explanations for each category: destructive ops, code-execution, path traversal, shell operators, URL injection, reflection).
  - The character allowlist (`A-Z a-z 0-9 _ - / (space) : . & # ~`) and which common characters fall outside it (`@`, `{`, `%`, `=`, `!`, etc.).
  - Explicit call-out that **tilde (`~`) is blocked** even though it's used for Minecraft relative coordinates — the most common cause of the confusing error message.
  - Table of commands that **work by default** and table of commands that need `allowUnsafeCommands: true`.
  - Step-by-step instructions for enabling in both split-config and monolithic mode, with `/neoe reload` reminder.
  - Security recommendation: restrict `neoessentials.item.powertool` to trusted staff when enabling.

#### `ItemManagement.md` — Powertool Command Safety Filter section (new)
- **Added** "Command Safety Filter" subsection directly below the powertool how-it-works bullets.
- Shows the exact error messages players receive when a command is blocked.
- Quick-reference tables of commands that work vs. commands that need `allowUnsafeCommands: true`.
- Config path for both split and monolithic mode, with `/neoe reload` shortcut.
- Cross-link to the full Security Configuration section in `SplitConfigs.md`.

#### `CommandsReference.md` — Powertool note (new)
- **Added** Callout block beneath the `/powertool` / `/pt` rows explaining the command filter, the most common blocked patterns (`~`, `@`, `{`), and where to set `allowUnsafeCommands: true`.

#### `Home.md` — Getting started key files (updated)
- **Added** `security.json` to the getting-started key config files list with a brief description.

---

## [1.0.2.6+build.18] — 2026-04-01 · [`4c534da6`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/4c534da6)

### New Feature — Fallback to Vanilla OP Permissions

#### New config key: `permissions.vanillaOpFallback`
- **Added** `vanillaOpFallback` (default `true`) in the `permissions` config section.  
  Unlike `opsBypassPermissions` (which runs *before* any permission check), this new option runs *after* every system has been consulted. If the external adapter **and** the internal manager both returned `false` for an OP player, NeoEssentials grants access as a last-resort safety net.
- **Purpose:** Prevents admin lockouts when FTB Ranks crashes, `permissions.json` is corrupted, or the external adapter becomes unhealthy at runtime.
- **Distinction from `opsBypassPermissions`:**

  | Setting | When it fires | Typical use |
  |---|---|---|
  | `opsBypassPermissions: true` | *Before* any check — OPs skip the permission system entirely | Fast-path for small/trusted servers |
  | `vanillaOpFallback: true` | *After* all checks return `false` — OPs get in only when everything else fails | Strict environments using LuckPerms/FTB Ranks that still need a lockout-prevention net |

#### Emergency mode on permission-system startup failure
- **Added** `PermissionAPI.setEmergencyMode(true)` is now activated when `PermissionSystem.initialize()` encounters an unrecoverable exception at server start, **instead of** crashing the server with a `RuntimeException`.
- In emergency mode every permission check immediately answers `true` for OPs and `false` for everyone else. A prominent boxed `ERROR` is logged at startup and on every check, prompting the admin to fix the config and run `/neoe reload`.
- **Added** `/neoe reload` now detects emergency mode and performs a **full re-initialisation** (resets manager, adapter, flags) so the system can recover without a restart once the root cause is fixed.
- **Added** `PermissionSystem.isEmergencyMode()` public accessor (useful for dashboard status displays).

#### Documentation
- **Updated** `PermissionSystem.md` config table: added `vanillaOpFallback` row with description and a comparison table explaining the difference between bypass and fallback modes.
- **Updated** "How Permissions Work" section: now lists all five steps in order (emergency → bypass → external → internal → fallback) with explanations.

---

## [1.0.2.6+build.17] — 2026-04-01 · [`4d5cf1a1`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/4d5cf1a1)

### Improvements — External Permissions Integration

#### Version Detection & Compatibility Reports
- **Added** `FtbRanksAdapter` and `LuckPermsAdapter` now read the mod version via `ModList` on construction and log it at `INFO` level (e.g. `FTB Ranks detected — version: 2101.1.3`).
- **Added** `AdapterCompatibilityChecker` — a new startup utility that scans the mod list and emits a formatted compatibility table at `INFO`/`WARN` level, showing each detected permission mod, its installed version, the last-tested version, and a ✓/⚠ status.  Generated in both internal-mode and external-mode startup paths.
- **Added** If the detected FTB Ranks version differs from the last-tested minor line, a prominent boxed `WARN` is emitted advising admins to watch for permission issues and report the version mismatch.

#### Multi-Strategy API Probe (FTB Ranks)
- **Improved** `FtbRanksAdapter` now probes **four** known API signatures in order instead of two:
  1. `FTBRanksAPI.getPermission(ServerPlayer, String, boolean)` — current 2101.1.x
  2. `instance.hasPermission(UUID, String)` — older builds via `INSTANCE`/`getInstance()`
  3. `FTBRanksAPI.hasPermission(ServerPlayer, String)` — possible future static variant
  4. `FTBRanksAPI.checkPermission(ServerPlayer, String)` — alternative naming / forks
- **Added** When all four strategies fail, a boxed error is logged including the detected FTB Ranks version, so it's immediately clear why permission checks will fall back.

#### Health Tracking & Fallback to Internal System
- **Added** `ExternalPermissionAdapter` interface gains three default methods (source-compatible — no changes needed to existing adapters):
  - `getVersion()` — returns the detected mod version string
  - `isHealthy()` — returns `false` once consecutive runtime failures exceed the threshold (default 5)
  - `getConsecutiveFailures()` — exposes the failure counter
- **Added** Both `FtbRanksAdapter` and `LuckPermsAdapter` implement `isHealthy()` / `getConsecutiveFailures()` via an `AtomicInteger` failure counter.  On each successful permission check the counter resets to 0.
- **Improved** `PermissionAPI.hasPermission()` now checks `externalAdapter.isHealthy()` before delegating. If the adapter is unhealthy **or** throws during a check, execution falls through to the **internal `permissions.json` manager** and then, as a last resort, to the OP-bypass check — so non-OP players are never locked out solely because an external permission mod is misbehaving.
- **Added** A single `WARN` is emitted on the 5th consecutive failure naming the adapter and its version, asking the admin to fix the issue and run `/neoe reload`.

---

## [1.0.2.6+build.16] — 2026-04-01 · [`c1cc26fa`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/c1cc26fa)

### New Features — Rules Command

#### Console Feedback on Load Failures
- **Improved** `loadRulesData()` now logs a prominent boxed error to the console whenever `rules_data.json` is corrupt or unreadable, including the absolute file path, the exact parse error, and step-by-step remediation instructions.
- **Improved** When no rules file exists at all, a clear `INFO` log is emitted with the auto-generated file path and quick-start editing instructions (`/rules add`, `/rules edit`, direct JSON edit + `/rules reload`), replacing the previous silent fall-through.

#### Auto-Generation of `rules_data.json`
- **Confirmed** `rules_data.json` is always written on first startup with 10 sensible default rules when neither `rules_data.json` nor the legacy `rules.json` is present. The generated file path is now logged so admins know exactly where to find it.

#### `/neoe reload` Now Reloads Rules
- **Added** `RulesCommand.reload()` is now called by `/neoe reload`, so server rules are refreshed alongside all other systems without a restart.

#### Dashboard API — `/api/rules`
- **Added** `RulesEndpoint` (`/api/rules`) providing full CRUD for server rules from the web dashboard, protected by Bearer-token auth:
  - `GET /api/rules` — list all rules with 1-based index
  - `POST /api/rules` — replace full rule list `{"rules": [...]}`
  - `POST /api/rules/add` — append a single rule `{"rule": "..."}`
  - `PUT /api/rules/{n}` — edit rule at position *n* `{"rule": "..."}`
  - `DELETE /api/rules/{n}` — delete rule at position *n*
  - `POST /api/rules/reload` — reload from disk without restart

#### Documentation
- **Added** Full `/rules` section in `docs/Wiki/UtilitySystems.md` covering: command table, colour codes, data-file format and location, console feedback examples, dashboard API table, and legacy migration note.
- **Fixed** Three rows (`/rules`, `/helpop`, `/suicide`) that were accidentally merged into the MOTD dashboard API table — they are now in their own sections.
- **Improved** `RulesCommand` now uses `ResourceUtil.getConfigPath()` for file paths (consistent with every other data file in the mod).

---

## [1.0.2.6+build.15] — 2026-04-01 · [`e3bb4dd2`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/e3bb4dd2)

### Bug Fixes — Split Configuration System

#### Root Cause Fixed: Split Files Never Created on Fresh Installs
- **Fixed** `createSplitConfigsFromJar()` silently failing for every split file because it looked for pre-split files (e.g. `main.json`, `chat.json`) directly inside the mod JAR — but only the monolithic `config.json` exists in the JAR. All split-file creation on fresh servers would produce 0 files with no error.  
  Fix: `createSplitConfigsFromJar()` now loads the JAR's `config.json`, then extracts each section group into the correct target file using the new `FILE_SECTIONS_MAP`.

#### Root Cause Fixed: `main.json` Overwritten to Single Section
- **Fixed** `ensureSplitConfigsUpToDate()` iterating over *section entries* instead of *file entries*. When three sections (`modules`, `logging`, `permissions`) all map to `main.json`, the file was written three times — each time with only one section — leaving only the last section on disk.  
  Fix: the loop now iterates `FILE_SECTIONS_MAP` (file → all its sections) and writes each file exactly once containing all required sections.

#### New: `economy` Section Added to Split Configs
- **Fixed** The `economy` config section (currency symbol, starting balance, sell multiplier, etc.) was present in `config.json` but missing from `CONFIG_FILE_MAP` and therefore never extracted into any split file. It is now mapped to `main.json`.

### New Features — Split Configuration System

#### Validation & Repair Commands
- **Added** `/neoe config validate` — checks all split config files for missing files, parse errors, and missing sections; prints a clear list of every problem with remediation instructions.
- **Added** `/neoe config repair` — automatically regenerates missing files from the JAR default and fills missing sections into existing files without overwriting user-set values.
- **Added** `/neoe config status` — dashboard-style overview showing each expected file with a ✔/✘ indicator and its section list; reports overall health.

#### Clear Error Messages on Missing Files
- When a split config file cannot be regenerated at startup, a prominent boxed error is now logged:
  ```
  ╔══════════════════════════════════════════════════════════╗
  ║  MISSING SPLIT CONFIG: chat.json
  ║  This file should contain: chat
  ║  Run: /neoe config repair   to regenerate all missing files.
  ╚══════════════════════════════════════════════════════════╝
  ```

#### `FILE_SECTIONS_MAP` — Authoritative File Layout
- Added `ConfigSplitter.FILE_SECTIONS_MAP` (public, `LinkedHashMap<String, List<String>>`): the single source of truth for which sections each split file must contain. Used by generation, validation, repair, and the status command.

#### Documentation
- **Added** `docs/Wiki/SplitConfigs.md` — full reference covering: file layout table, migration guide, fresh-install behaviour, health-check commands with example output, automatic startup checks, version tracking, disabling split configs, and a complete command reference.

---

## [1.0.2.6+build.12] — 2026-04-01 · [`1cebc781`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/1cebc781)


### New Features

#### MOTD System — Multi-Profile & Rotation
- **Added** `MotdManager` singleton (`com.zerog.neoessentials.util.motd.MotdManager`) to own all MOTD state, replacing the static fields that lived inside `MotdCommand`.
- **Added** named **MOTD profiles** — each profile has its own `motd`, `author`, and `timestamp`. The active profile name is persisted in `config/neoessentials/motd_data.json`.
- **Added** automatic **legacy migration** — single-MOTD `motd_data.json` files from previous builds are seamlessly promoted to the multi-profile format on first load with no data loss.
- **Added** `/motd profile list|create|delete|switch|info` subcommands (permission: `neoessentials.motd.profile`).
- **Added** `/motd rotation enable <minutes>|disable|next` subcommands — enables a background daemon thread that cycles through all profiles on a configurable interval (permission: `neoessentials.motd.rotation`).
- **Added** `MotdEndpoint` (`/api/motd`) dashboard REST endpoint supporting full CRUD for profiles, rotation control, and broadcast — all protected by the existing Bearer-token auth middleware.
- **Added** clear **in-game error feedback** for both save and load failures: `/motd reload` now shows the exact I/O error in-game instead of silently resetting to blank.
- **Added** two new permission nodes: `neoessentials.motd.profile` and `neoessentials.motd.rotation` (both default `false`).
- **Added** `MotdManager.shutdown()` called from `DashboardLifecycleManager.onServerStopping` to cleanly terminate the rotation scheduler thread.

#### Dashboard API
- `DashboardAPI.registerEndpoints()` now registers `/api/motd` via `withAuth(new MotdEndpoint(server))`.

### Improvements
- Lang file updated with new keys: `motd.load_error`, `motd.save_error`, `motd.profile.*`, `motd.rotation.*`. Removed orphaned `motd.empty` duplicate.
- `PermissionRegistry` updated with descriptive docs for all six `neoessentials.motd.*` nodes.
- `UtilitySystems.md` and `CommandsReference.md` wikis updated with full MOTD command/API reference.

---

## [1.0.2.6+build.8] — 2026-04-01


### Bug Fixes

#### Chat System
- **Fixed** `&` color codes in `chat.json` format values being silently discarded, causing all chat output to appear plain white regardless of configuration.  
  Root cause: `ChatFormatter.formatMessage()` called `componentToFormattedString(RichTextFormatter.processRichText(…))` which internally called `Component.getString()` — stripping every formatting code before the enhancement phase saw the text.  
  Fix: a new `RichTextFormatter.preprocessTags()` method converts `<gradient:…>` / `<rainbow>` tags to `&#RRGGBB` hex codes as a plain `String` and returns without touching any `&` codes; `ChatFormatter` now calls `preprocessTags()` so that all `&` codes survive into `buildComponentFromMarkup()` / `parseColorCodes()` where they are rendered correctly. The no-enhancements path (`processRichText()` → `Component`) is unchanged.  
  Example format (now works correctly):
  ```json
  "group:fondateur": "&f[&4Fondateur&f] &f{neoessentials_username}&7: &f{MESSAGE}"
  ```

#### Permissions
- **Fixed** Three wildcard permission nodes (`neoessentials.spawner.*`, `neoessentials.fireball.*`, `neoessentials.warps.*`) logging `WARN: Invalid permission format` at every startup.  
  Root cause: `PermissionRegistry.isValidPermission()` and `PermissionScanner.isValidPermission()` used the regex `^[a-z0-9._-]+$` which does not include `*`, so every `.*`-suffixed node failed validation and was silently dropped from the registry.  
  Note: the permissions **worked at runtime** in all affected versions because `PermissionManager.hasPermissionWithWildcards()` evaluates group permissions directly without consulting the registry. Only the startup log was wrong.  
  Fix: both validators now recognise the `.*` wildcard suffix explicitly — the prefix (everything before `.*`) is validated separately with the existing character rules; `neoessentials.*`, `neoessentials.spawner.*`, etc. now register cleanly with no warnings.

---

## [1.0.2.6+build.5] — 2026-04-01

### Bug Fixes

#### Config System
- **Fixed** `ClassCastException: JsonArray cannot be cast to JsonObject` crash in all kit commands (`/kit`, `/kits`, `/listkits`, etc.) when split configs are enabled. Kit settings now live in `main.json`; `kits.json` is reserved for kit definitions only. All `ConfigManager` kit-settings helpers now carry an explicit `isJsonObject()` guard.
- **Fixed** `getConfig("chat")` (and all other section-name lookups) throwing `FileNotFoundException`. `ConfigManager.getConfig()` now handles bare section names (no `.json` extension) by extracting the section from the main config, fixing errors in `ChatFormatter`, `BadgeManager`, `ConditionalFormatter`, `ResourcePackManager`, `PlayerTagManager`, and more.
- **Fixed** `ConfigSplitter.mergeSplitConfigs()` now skips `"kits"` unless the value `isJsonObject()`, preventing leftover `kits.json` from poisoning the merged config view.

#### Permissions
- **Fixed** Server operators (`OP`) being denied commands when an external permission adapter (FTB Ranks, LuckPerms) was configured. OP bypass is now checked *before* delegating to any external system, acting as a universal safety fallback.
- **Fixed** `FtbRanksAdapter` crashing with `NoSuchMethodException: hasPermission(UUID, String)` on FTB Ranks 2101.1.x. The adapter now probes `getPermission(ServerPlayer, String, boolean)` (new API) and falls back to the old instance method, handling multiple return types (Boolean, Optional<Boolean>, TriState).

#### ChestShop
- **Fixed** Admin shops created with `?` on line 4 showing "This shop is not yet ready" when the creating admin right-clicked to assign the item. Admin shops have `ownerUUID = null`; the interact handler now grants assignment rights to any player with `neoessentials.shop.create.admin` instead of checking UUID equality.

#### Commands
- **Fixed** `/help 2` (and any `/help <page>`) showing "No command found" instead of the requested page. Vanilla's `<command:string>` argument was matching the page number first. Replaced the separate integer branch with a combined `<page_or_command>` argument that parses integers first.
- **Fixed** `/unban <player>` reporting "Player is not currently banned" for vanilla-issued bans (`/ban` or operator action). `BanManager` now checks and imports from the vanilla `UserBanList` as a fallback, and syncs all NeoEssentials bans to the vanilla list so both stay consistent.
- **Fixed** `/rules` showing "Rules are not set" on servers migrating from older builds that stored rules in `rules.json`. `loadRulesData()` now detects the legacy file and migrates its contents to `rules_data.json` automatically.
- **Fixed** `/motd set <msg>` appearing to succeed but the MOTD resetting on restart. `MotdCommand` now uses `ResourceUtil.getConfigFile("motd_data.json")` (consistent with every other data file in the mod) instead of a raw relative `Paths.get()` path. Save errors now log the absolute path for easier diagnosis.

---

## [1.0.2.6+build.1] — 2026-03-06

### Starting fresh from 1.0.2.6

This is the first build of the `1.0.2.6` release series. Build number reset to 1.

**Carried forward from 1.0.2.5 series:**

#### Added
- Sign-based ChestShop system — admin shops, auto-fill (`?`), buy/sell via right/left-click
- Vault API — Economy, Chat, and Permission providers backed by NeoEssentials systems
- Dedicated `tablist.json` config — group colours, 18 placeholders, animation, `&` colour codes
- 50+ new commands across Player Info, World/Fun, Teleport, Item/Misc, Utility, Admin, Player State
- `/tpr` / `/rtp` Random Teleport — even distribution, nether-aware, async pre-computation cache, named zones, biome exclusions, `/settpr`
- Timed jails (`/jailfor`) with auto-release, full event enforcement (respawn, teleport, interact, attack)
- `/kit <name> <player>` give-to-others, `/kitreset`, clean kit list with cooldown status
- `/mail sendtemp`, `sendall`, `sendtempall`, `clearall` — mute/ignore/rate-limit checks
- `/warp <name> <player>`, `/warp` (no args) paginated list, per-warp permission support
- `/eco reset`, async `/baltop` with pagination and total wealth, percent amounts in eco commands
- 8 new bundled languages: FR, DE, ES, PT-BR, ZH-CN, NL, PL, RU — auto-deployed and merged on start
- 50+ permission nodes registered; new `MODERATION` category; denial messages show required node
- `tablist.json` dedicated config; `/tablist config` live settings summary

#### Fixed
- Teleportation safe-location detection rewritten — slabs, stairs, glass, trapdoors now correctly safe; dangerous blocks (lava, fire, magma, cactus) now correctly blocked
- AFK system — config loading, activity score thresholds, broadcast formatting, personal feedback all fixed
- Chat messages now appear in server console
- PowerTool — fires on block right-clicks and empty right-clicks, not just air; `/powertooltoggle` now correctly enables/disables powertools
- Rich text (gradients/rainbow) rendering pipeline fixed
- Dashboard — offline login, register command, file auto-update, admin/permissions split into own pages
- ~120 missing translation keys added to `en_us.json`; auto-merge on load without overwriting edits
- Vault economy `format()` now reads live currency symbol from config
- Vault chat prefix/suffix correctly routes through LuckPerms/FTBRanks when installed
- NeoForge 1.21.1 API compatibility: event classes, `ItemStack` methods, stats API all corrected
