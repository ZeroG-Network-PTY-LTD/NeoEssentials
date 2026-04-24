# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.6+build.40 — 2026-04-24

### 🔒 Security Fix — `/inv` and `/ec` Bypass Permission Checks

`/inv` and `/ec` aliases were registered as Brigadier `redirect()` nodes with **no `requires()` predicate**, meaning any player could open other players' inventories/ender chests regardless of permissions. Brigadier redirect nodes only check their own predicate — not their target's.

- Replaced redirect aliases with full command registrations that include explicit `requires()` checks.
- Fixed typo in `/ecedit` alias (`"enderchestdit"` → `"enderchestedit"`).
- Added `neoessentials.invsee` and `neoessentials.enderchest` to the `moderator` group in `permissions.json`.
- Added missing translation keys for all invsee/ec messages.

**Permission nodes:** `neoessentials.invsee` (moderator), `neoessentials.invsee.edit` (admin), `neoessentials.enderchest` (moderator), `neoessentials.enderchest.edit` (admin)

---

## 1.0.2.6+build.38 — 2026-04-24


### 🐛 Fix — Teleportation Messages & Cooldown/Warmup System

**Teleport messages showing raw keys**
- Added all missing `commands.neoessentials.teleport.spawn.*` message keys to `en_us.json` — spawn success, fallback, failure, info, safety, cooldown, warmup, etc.
- Added missing warp and home warmup message keys.
- Lang version bumped to `11`; new keys are auto-merged into existing server language files on startup.

**Cooldowns and warmup delays now fully enforced**
- `HomeManager`: reads `teleportDelay` from config; enforces `homeTeleportCooldown` before each `/home` use.
- `WarpManager`: enforces `warpCooldown` (use cooldown, not just set cooldown) before each `/warp` use.
- `SpawnManager`: enforces `spawnCooldown` before each `/spawn` use; teleport delay now driven by config.json (spawn.json delay override removed).
- All managers now show a countdown warmup message when `teleportDelay > 0` and `enableTeleportWarmup=true`.

**Config keys:**  
`generalSettings.teleportDelay`, `homeSettings.homeTeleportCooldown`, `warpSettings.warpCooldown`, `spawnSettings.spawnCooldown`, `generalSettings.enableTeleportWarmup`

---



### ✨ Feature — Permissions GUI, External Systems & Fine-Grained Control

**Web Dashboard API** — The `/api/permissions` REST endpoint now covers everything:
- `POST /reload` — reload permissions from disk
- `GET/POST/DELETE /group/{name}/context` — manage contextual overrides (world/time/gamemode)
- `GET/POST /group/{name}/temp` + `DELETE /group/{name}/temp/{node}` — manage temp permissions
- `GET/POST/DELETE /user/{name}/context` + `GET/POST /user/{name}/temp` — same for users
- `GET/POST/DELETE /aliases` — manage permission aliases
- `GET /system/status` — now shows emergency mode, external adapter health/version/failures, alias count

**External System Integration** — Improved documentation and fallback logic:
- Full 5-step fallback chain documented: emergency → OP bypass → external → internal → OP fallback
- Adapter health tracking: 5 consecutive failures → `UNHEALTHY` → auto-fallback to `permissions.json`
- LuckPerms context-aware checks via `QueryOptions`; FTB Ranks 4-API-signature probe
- Startup compatibility report logged with adapter version and ⚠ warnings for newer versions
- Compatibility table: LuckPerms, FTB Ranks, WorldEdit, FTB Chunks, any NeoForge mod

**Fine-Grained Command Control** — Per-subcommand permission nodes fully documented:
- Every Brigadier branch has its own node (e.g. `/home set` → `neoessentials.teleport.home.set`, `/home delete` → `neoessentials.teleport.home.delete`)
- Home, Warp, Kit, Economy, Moderation, and Permission system all documented with full tables
- Negative permissions (`-node`) documented for targeted deny without removing wildcards

---

## 1.0.2.6+build.28 — 2026-04-01

### ✨ Feature — Permissions System Improvements

**Contextual Permissions** — Grant or deny a permission node based on the player's current world, time of day, or gamemode. New commands:
- `/permissions group <group> context add <contextKey> <node> allow|deny`
- `/permissions user <player> context add <contextKey> <node> allow|deny`
- Context keys: `world:overworld`, `world:the_nether`, `world:the_end`, `time:day`, `time:night`, `gamemode:survival/creative/spectator/adventure`

**Permission Conditions** — Attach runtime conditions (e.g. `gamemode:survival AND time:day`) to any permission node. Grant is withheld if the condition fails at check time.

**Permission Aliases** — Map legacy / short node names to canonical NeoEssentials equivalents via `config/neoessentials/permission_aliases.json`. Aliases resolve transparently on every permission check (e.g. `"essentials.fly" → "neoessentials.fly"`).

**API for Other Mods** — `NeoEssentialsAPI.getPermissionsService()` returns a clean `PermissionsService` interface. Other mods can check permissions with context, register their own permission nodes, and register aliases — without importing internal NeoEssentials classes.

**Storage** — Contextual permissions and conditions are now persisted in `permissions.json` (groups) and `permissions/playerdata.json` (users). Fully backward-compatible.

**New permission nodes:** `neoessentials.permissions.user.context`, `neoessentials.permissions.group.context`

---

## 1.0.2.6+build.26 — 2026-04-01

### 🔧 Improvement — Utility Systems Audit & Polish

- **`/nick` / `/nickname`** — Storage path now uses the centralised `ResourceUtil.getConfigPath()` helper; `/nickname` alias registered as a Brigadier redirect so it works identically to `/nick`.
- **`/seen`** — Storage path updated to `ResourceUtil.getConfigPath()`.
- **Command registration** — Removed duplicate `registerCommand()` metadata entries in the "PLAYER INFO" block; every player-info command (`near`, `ping`, `seen`, `whois`, `realname`, `motd`, `rules`, `suicide`, etc.) is now registered exactly once by its dedicated class.
- **Permission registry** — Eliminated stale duplicate `register()` calls that silently overrode correct values (e.g. `neoessentials.whois` was being reset from `ADMIN/false` to `MISC/true`; `neoessentials.ping.others` was being reset from `true` to `false`). All unique sub-nodes (`whois.detailed`, `rules.admin`, `motd.*`) are kept in their canonical positions.
- **All core utility commands verified** — `/nick` `/nickname` `/setnick` `/near` `/nearby` `/ping` `/depth` `/helpop` `/motd` `/rules` `/suicide` `/killme` `/seen` `/whois` `/realname` `/msgtoggle` — fully present, registered once, and using `PermissionValidator` consistently.

---

## 1.0.2.6+build.25 — 2026-04-01

### ⏳ New Feature — Temporary Permissions

- **Permissions** — Grant time-limited permissions to players or groups that automatically expire and are revoked — no manual cleanup required.
- **User commands:** `/permissions user <p> addtemp <node> <duration>`, `removetemp <node>`, `listtemp` (requires `neoessentials.permissions.user.temp` / `info.user`).
- **Group commands:** `/permissions group <g> addtemp <node> <duration>`, `removetemp <node>`, `listtemp` (requires `neoessentials.permissions.group.temp` / `info.group`).
- **Duration format:** combinations of `d`/`h`/`m`/`s` — e.g. `30m`, `12h`, `1d`, `7d`, `1d12h30m`.
- **Auto-expiry:** server-tick handler checks every 30 s; expired entries are removed, affected online players notified, and each expiry written to the audit log (`USER_TEMP_PERM_EXPIRED` / `GROUP_TEMP_PERM_EXPIRED`).
- **Persistence:** temp permissions survive server restarts (stored as `"tempPermissions": {"<node>": <expiryMs>}` in `playerdata.json` / `permissions.json`). Expired entries are stripped on load.
- **Audit log:** 6 new action constants — `USER_TEMP_PERM_ADDED`, `USER_TEMP_PERM_REMOVED`, `USER_TEMP_PERM_EXPIRED`, `GROUP_TEMP_PERM_ADDED`, `GROUP_TEMP_PERM_REMOVED`, `GROUP_TEMP_PERM_EXPIRED`.
- **New nodes:** `neoessentials.permissions.user.temp`, `neoessentials.permissions.group.temp`.

---

## 1.0.2.6+build.23 — 2026-04-01 · [`48763856`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/48763856)

### 📋 New Feature — Permission Audit Logging

- **Added** `PermissionAuditLogger` — every permission change made through NeoEssentials commands is now written to `neoessentials/permissions_audit.log` (append-only). Includes timestamp, executor name, action type, affected target, and detail string.
- **17 actions tracked:** user group set, user perm add/remove/clear, group create/delete/rename/clone, group perm add/remove/clear, group inherit add/remove, group prefix/suffix/priority set, and permissions reload.
- **Added** `permissions.auditLogging` config key (default `true`). Set to `false` to disable.

---

## 1.0.2.6+build.22 — 2026-04-01 · [`a2e1a7ed`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/a2e1a7ed)

### ⚖️ Improvement — Permission Groups & Priorities + Permission Suggestions

- **Added** `priority` field (int, default 0) to every permission group. Higher priority groups are checked first when the engine walks the inheritance chain — stops ambiguous grant/deny ordering when multiple parent groups conflict.
- **Updated** `PermissionStorage` — `priority` is persisted in `permissions.json` (backwards-compatible; missing key reads as 0).
- **Added** `/permissions group <name> setpriority <value>` and `getpriority` commands.
- **Updated** `/permissions info group` now shows the group's current priority.
- **Improved** Denial messages now include the **human-friendly description** of the required permission node (e.g. `§8(Ban a player from the server)`) pulled from `PermissionRegistry`, making it clear to staff exactly which capability they are missing.
- **Docs** `PermissionSystem.md` — new "Group Priorities" section, updated example `groups.json` with priority values, and updated denial-message format description.
- **Docs** `CommandsReference.md` — `setpriority`/`getpriority` rows added.

---

## 1.0.2.6+build.21 — 2026-04-01 · [`81c7a55d`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/81c7a55d)

### 🔍 New Feature — Permission Debugging Tools

- **Added** `/permissions debug <player>` — in-game permission resolution trace. Shows system mode (internal / external adapter / emergency), adapter health and version, active config flags (`opsBypassPermissions`, `vanillaOpFallback`), OP status, assigned group, direct user permissions, group inheritance chain, and a numbered 4-step resolution summary. Requires `neoessentials.permissions.debug`. No log diving needed.
- **Added** `neoessentials.permissions.debug` permission node registered in `PermissionRegistry`.
- **Fixed** `/permissions user check` was calling the internal manager directly, bypassing the external adapter (LuckPerms/FTB Ranks), `opsBypassPermissions`, and `vanillaOpFallback`. Now uses the full 5-step resolution chain — results are now consistent with what actually happens at runtime.

---

## 1.0.2.6+build.19 — 2026-04-01 · [`a22d0323`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/a22d0323)

### 📖 Documentation — `allowUnsafeCommands` & Security Configuration

- **Fixed** `SplitConfigs.md` had a completely wrong description for `allowUnsafeCommands` ("Allow enchantments and item operations beyond vanilla limits" — that's the `items` section).  Replaced with accurate full documentation.
- **Added** Complete `security.json` reference table covering all six keys with types, defaults, and descriptions.
- **Added** Detailed `allowUnsafeCommands` breakdown: every blocked substring explained, the character allowlist, explicit call-out that tilde `~` (Minecraft relative coords) is blocked by default — the most common cause of admin confusion.
- **Added** Tables of commands that work vs. commands that need `allowUnsafeCommands: true`, with step-by-step enable instructions for both split-config and monolithic mode.
- **Added** "Command Safety Filter" subsection in `ItemManagement.md` under Powertool — exact error messages, quick-reference tables, config location.
- **Added** Warning callout on the `/powertool` row in `CommandsReference.md`.
- **Added** `security.json` to the getting-started key files list in `Home.md`.

---

## 1.0.2.6+build.18 — 2026-04-01 · [`4c534da6`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/4c534da6)

### 🛡️ New Feature — Fallback to Vanilla OP Permissions

- **Added** `permissions.vanillaOpFallback` config key (default `true`). After all permission systems (external adapter + internal manager) have been consulted and returned `false`, OPs (level 2+) are granted access as a last resort. Prevents lockouts when FTB Ranks crashes or `permissions.json` is corrupted.
- **Added** Emergency mode: if the permission system fails to initialise at startup, the server no longer crashes. Instead, NeoEssentials activates emergency mode — OPs get all permissions, everyone else is denied — and logs a clear boxed error asking the admin to fix the config and run `/neoe reload`.
- **Improved** `/neoe reload` now detects emergency mode and performs a full re-initialisation so the system can recover without a server restart.
- **Docs** `PermissionSystem.md` updated with new config table row, comparison table (bypass vs fallback), and updated "How Permissions Work" section.

---

## 1.0.2.6+build.17 — 2026-04-01 · [`4d5cf1a1`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/4d5cf1a1)

### 🔌 Improved — External Permissions Integration

- **Added** `FtbRanksAdapter` and `LuckPermsAdapter` now detect the installed mod version at startup and log it. A boxed `WARN` is emitted when FTB Ranks is newer than the last-tested minor version.
- **Added** `AdapterCompatibilityChecker` — a startup compatibility report that lists every detected permission mod, its installed version, last-tested version, and ✓/⚠ status.
- **Improved** FTB Ranks adapter now probes four API signatures (current static, legacy instance, future static, alternative naming) so it stays functional across FTB Ranks version bumps.
- **Added** Both adapters now track consecutive runtime failures. After 5 failures the adapter is marked **unhealthy** and a boxed `WARN` is logged.
- **Improved** `PermissionAPI` now falls back to the **internal `permissions.json`** (and then OP-bypass) when the external adapter is unhealthy or throws — non-OP players are never locked out due to a broken external permission mod.

---

## 1.0.2.6+build.16 — 2026-04-01 · [`c1cc26fa`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/c1cc26fa)

### ✨ New Features — Rules Command

- **Improved** Console feedback when `rules_data.json` fails to load — detailed boxed error with file path, exact error message, and step-by-step fix instructions.
- **Improved** On first startup with no rules file, logs the generated file path and quick-start editing commands so admins know immediately how to configure their rules.
- **Added** `/neoe reload` now reloads server rules alongside all other systems.
- **Added** Dashboard API endpoint `/api/rules` — full CRUD for server rules from the web dashboard (list, add, edit, delete, replace all, reload).
- **Added** Full `/rules` documentation section in `UtilitySystems.md` with command table, data-file format, console feedback examples, and dashboard API reference.
- **Fixed** `/rules`, `/helpop`, and `/suicide` rows were incorrectly placed inside the MOTD Dashboard API table in the wiki — now in their own sections.

---

## 1.0.2.6+build.15 — 2026-04-01

### 🐛 Bug Fixes — Split Config System

- **Fixed** Split config files (main.json, chat.json, etc.) never being created on fresh server installs. `createSplitConfigsFromJar()` previously looked for files that don't exist in the JAR — it now correctly extracts sections from the bundled monolithic `config.json`.
- **Fixed** `main.json` being overwritten with only one section (e.g. only `modules` with `logging`/`permissions`/`kits` lost) when `ensureSplitConfigsUpToDate()` processed section entries instead of file entries.
- **Fixed** The `economy` config section (currency, starting balance, sell multiplier) being completely absent from split configs.

### ✨ New Features — Split Config System

- **Added** `/neoe config validate` — checks all 10 split files for missing files, parse errors, and missing sections with clear remediation hints.
- **Added** `/neoe config repair` — auto-regenerates missing split files and fills missing sections from JAR defaults without touching existing values.
- **Added** `/neoe config status` — visual ✔/✘ overview of all split config files and overall health.
- **Added** Boxed startup error messages when split config files cannot be regenerated, including exact command to fix.
- **Added** `SplitConfigs.md` wiki — full documentation on the split config system.

---

## 1.0.2.6+build.12 — 2026-04-01


### ✨ New Features

- **MOTD – Multi-Profile Support** — The MOTD system now supports multiple named profiles. Create, delete, switch, and inspect profiles with `/motd profile list|create|delete|switch|info`. Each profile has its own message, author, and timestamp, all persisted to `config/neoessentials/motd_data.json`.
- **MOTD – Auto-Rotation** — Automatically cycle through all MOTD profiles on a configurable interval using `/motd rotation enable <minutes>`. Disable with `/motd rotation disable`, or advance manually with `/motd rotation next`.
- **MOTD – Dashboard API** — Full REST endpoint at `/api/motd` in the web dashboard for managing profiles, switching the active profile, controlling rotation, and broadcasting to online players.
- **MOTD – Error Feedback** — Save and load failures now show a descriptive error in-game instead of silently resetting. `/motd reload` reports the exact I/O problem if the data file cannot be read.
- **Legacy Migration** — Existing single-MOTD `motd_data.json` files are automatically promoted to the multi-profile format on first load with no data loss.

### 🔒 New Permissions
- `neoessentials.motd.profile` — Manage MOTD profiles (default: `false`)
- `neoessentials.motd.rotation` — Control auto-rotation (default: `false`)

---

## 1.0.2.6+build.5 — 2026-04-01


### 🐛 Bug Fixes

- **Config** – Fixed `ClassCastException` crash in all kit commands (`/kit`, `/kits`, `/listkits`) when split configs are enabled. Kit settings now live in `main.json`, separate from kit definitions in `kits.json`.
- **Config** – Fixed `getConfig("chat")` and similar section-name lookups throwing `FileNotFoundException`, breaking chat formatting, badges, resource packs, and more.
- **Permissions** – Fixed server OPs being denied commands when FTB Ranks or LuckPerms is installed. OP bypass is now a universal safety net regardless of permission back-end.
- **Permissions** – Fixed FTB Ranks adapter crashing with `NoSuchMethodException` on FTB Ranks 2101.1.x. Updated reflection logic probes the new API format automatically.
- **ChestShop** – Fixed Admin Shops created with `?` (pending item) showing "This shop is not yet ready" when the admin tried to assign the item. Any player with `neoessentials.shop.create.admin` can now assign the item.
- **Help** – Fixed `/help 2` (page numbers) showing "No command found" due to a conflict with vanilla's `/help` command registration.
- **Moderation** – Fixed `/unban` not detecting bans issued by vanilla `/ban` or operator tools. NeoEssentials now syncs with the vanilla ban list in both directions.
- **Rules** – Fixed `/rules` showing "Rules are not set" on servers that had rules configured in the legacy `rules.json` file from older builds. Auto-migration to `rules_data.json` is now performed.
- **MOTD** – Fixed MOTD resetting on server restart due to an incorrect save path. Now uses `ResourceUtil` consistently with all other data files.

---

## 1.0.2.6+build.1 — 2026-03-06

### 🔁 Starting fresh from 1.0.2.6

This is the first build of the `1.0.2.6` release series.

**Included from 1.0.2.5 series:**
- Sign-based ChestShop system with admin shops, auto-fill item (`?`), and buy/sell via right/left-click
- Vault API (Economy, Chat, Permission providers)
- Dedicated `tablist.json` config file with group colours, placeholders, and animation support
- 50+ new commands across Player Info, World/Fun, Teleport, Item/Misc, Utility, Admin, and Player State systems
- Random teleport (`/tpr`) — even distribution, nether-aware, async cache, named zones, biome exclusions
- Timed jails with auto-release, full event enforcement (respawn, teleport, interact, attack)
- Offline pay, async baltop with pagination and total wealth, percent eco amounts
- 8 new languages (FR, DE, ES, PT-BR, ZH-CN, NL, PL, RU) — auto-deployed and merged on start
- 50+ permission nodes registered, new MODERATION category, denial messages show required node
- Teleportation safe-location detection fully rewritten (slabs, stairs, glass, etc. now safe)
- AFK system fully wired to config, score thresholds fixed, broadcasts and personal feedback working
- Chat messages now appear in server console
- PowerTool fixes — fires on block right-clicks, `/powertooltoggle` now works correctly
- Rich text (gradients/rainbow) rendering fixed
- Dashboard offline login, register command, and file auto-update fixed
- ~120 missing translation keys added and auto-merged on load
