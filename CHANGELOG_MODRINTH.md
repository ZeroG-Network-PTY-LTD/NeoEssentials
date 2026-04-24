# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.6+build.62 — 2026-04-24

### 🌐 Improvement — Localization Audit & Admin Tooling

Full audit of all in-game translation key usage, 54 missing keys added, and a new `/language` tooling suite for server admins.

**What's new / fixed:**

- **54 missing translation keys** added to `en_us.json` — TPA/teleport-request flow (`request.sent`, `request.received`, `request.denied`, `request.expired`, `request.failed`, etc.), misc teleport (`/jumpto`, `/back` info), spawn/warp coordinate errors, moderation (`unfrozen_message`, `reason_too_long`), and several general/utility messages
- **Human-readable fallbacks** — if a translation key is missing at runtime, players now see a readable English phrase (e.g. `Home not found`) instead of the raw key string (`commands.neoessentials.home.not_found`)
- **`/language validate <code>`** — compare any language file against `en_us.json`, get coverage % and lists of missing/extra keys
- **`/language regenerate <code>`** — refresh a language file from the bundled JAR copy, merge user translations, auto-backup to `.bak`
- **`/language override`** — override individual message keys in-game; persisted to `overrides.json` and take priority over all language files (`set`, `get`, `remove`, `list`, `clear`, `reload` sub-commands)
- `_langVersion` bumped **12 → 13** — new keys auto-merged into existing deployments at next server start

---

## 1.0.2.6+build.59 — 2026-04-24

### 🐛 Bug Fix — Chat: Unresolved `{neoessentials_username_hover}` & duplicate server log

Two related chat-formatting bugs fixed:

1. **`{neoessentials_username_hover}` appearing literally** — When "clickable player names" was enabled, `ChatFormatter` substituted `{neoessentials_username}` with `{neoessentials_username_hover}`, which was never registered as a placeholder. The token was never resolved and appeared as raw text in chat.

2. **Duplicate vanilla log line** — `ChatHandler` called `server.sendSystemMessage(formattedMessage)` after already logging via its own logger. This caused vanilla's `MinecraftServer` logger to emit a second line: `<{neoessentials_username_hover}> message`.

**Fixes:**
- Replaced placeholder-swap with `§HNAME§name§/HNAME§` internal markup tokens resolved in `buildComponentFromMarkup()` to real hover+click Components
- Added `username_hover` / `displayname_hover` as plain-text fallback aliases in `DefaultPlaceholderExpansion`
- Removed the redundant `server.sendSystemMessage()` call

---

## 1.0.2.6+build.58 — 2026-04-24

### 🔌 Feature — API & Placeholder System

Exposes the NeoEssentials placeholder system as a fully public Java API for external mods, adds in-game admin tooling, REST endpoints, and rewrites developer documentation.

**What's new:**

- **`PlaceholderProvider`** and **`PlaceholderExpansion`** are now `public` top-level types — external mods can implement/extend them to register custom placeholders
- **`NeoEssentialsAPI.getPlaceholderManager()`** — new entry-point to the thread-safe `PlaceholderManager` (API version bumped to `1.2.0`)
- **`/api/placeholders/list`**, **`/api/placeholders/resolve`**, **`/api/placeholders/stats`** — new authenticated REST endpoints
- **`/api/docs`** now serves the built-in documentation system (was implemented but never wired)
- **`/placeholder`** in-game command — `list`, `info <id>`, `test <text>`, `stats` sub-commands (permission: `neoessentials.admin.placeholders`)
- **`neoessentials.admin.placeholders`** permission node registered
- **`APISystem.md`** completely rewritten with full placeholder table, code examples for `PlaceholderProvider`/`PlaceholderExpansion`, REST endpoint reference, versioning contract

---

## 1.0.2.6+build.57 — 2026-04-24

### ✨ Feature — Chat Formatting: Per-Player Overrides Now Applied

Per-player format overrides set via `/chatformat set <player> <format>` were stored correctly but **never actually applied** — `ChatHandler` always resolved the chat format from the group/world lookup, silently ignoring any stored per-player override. `PlayerChatFormatManager.getFormat()` is now called first, making it the highest-priority step in the chain (per-player → group+world → group → world → default).

All rich-text features were already implemented (`RichTextFormatter`, `ChatFormatter`) and remain unchanged: hex colors (`&#RRGGBB`), gradients (`<gradient:FF0000-0000FF>`), rainbow, hover tooltips, click events, bold/italic, and legacy `&` codes. `ChatSystem.md` has been fully rewritten with a format priority diagram, `/chatformat` command table, complete tag reference with examples, placeholder list, and config key reference.

---

## 1.0.2.6+build.56 — 2026-04-24

### ✨ Feature — Inventory Management & Security Improvements

Config flags for `/invsee`, `/inv`, `/invseeedit`, `/enderchest`, `/ec`, `/enderchestedit`, `/ecedit` existed in `config.json` but were never read — the commands were always available regardless of the flag. All `requires()` predicates now check the corresponding `isCommandEnabled()` flag.

Added a concurrent-edit lock: only one staff member can hold an editable view of a player's inventory or ender chest at a time (second attempt is blocked with an informational message; lock releases on viewer disconnect). Added `InventoryAuditLogger` writing every view/edit open to `neoessentials/inventory_audit.log` (7 action types, controlled by new `items.inventoryAuditLog` config key). Added 4 new language keys for disabled and concurrent-edit error messages.

---

## 1.0.2.6+build.55 — 2026-04-24

### 🔧 Improvement — Per-Command Teleport Bypass Perms & Chunk Loading Docs

8 per-command bypass permission nodes (`neoessentials.teleport.home.bypass.cooldown/warmup`, `warp.*`, `spawn.*`, `back.*`) were already checked in code but absent from `PermissionRegistry`, so the dashboard and permission tools couldn't discover them. All 8 are now registered. Added a "Chunk Loading & Safety Interaction" section to `TeleportationSystem.md` explaining the 3×3 chunk preload and safety-scan order of operations.

---

## 1.0.2.6+build.50 — 2026-04-24

### ✨ Teleportation Improvements — Dashboard Settings Page, Language Keys & Permission Docs

Added a live teleport settings page to the web dashboard, fixed missing `/back` language keys, and documented all bypass permission nodes.

- New **Teleport Settings** page in the dashboard — edit cooldowns, warmup delays, safety flags, and home/warp limits without restarting. Saves instantly reload all teleport managers.
- Fixed `/back` warmup and cooldown messages that showed raw key names instead of text (keys were used in Java but not defined in the language file).
- Documented 10 cooldown/warmup bypass permission nodes in `permissions_nodes.txt`.
- Added `MiscTeleportManager.reload()` for dashboard live-reload support.

---

## 1.0.2.6+build.46 — 2026-04-24

### 🐛 Bug Fix — Web Dashboard Admin Controls & Permissions Page Blank

Admin Controls and Permissions pages showed a blank screen after login. F5 briefly revealed the content before it disappeared.

Three root causes: (1) `showLoginScreen()` hid `dashboardWrapper` without redirecting — on these pages there is no login form, so the result was a blank page with no way forward. Fixed to redirect to `index.html`. (2) `permissions.js` never called `initPermissionSystem()` because its init guard checked `window.location.hash` / `[data-page="permissions"].active`, which never match on the standalone `permissions.html` page. Fixed with a reliable element check. (3) Nine `fetchWithAuth()` calls in `permissions.js` were missing `.json()`, so all modal actions (group edit, permission add/remove, etc.) silently failed by checking `.success` on a raw `Response` object. All fixed. Also fixed username not showing in the sub-page topbars.

---

## 1.0.2.6+build.44 — 2026-04-24


### 🐛 Bug Fix — Home Confirmation Buttons Append "confirm" to Name

Clicking `[Confirm]` on a `/sethome` overwrite or `/delhome` deletion prompt failed with *"Invalid home name: Colony confirm"*. Each subsequent click appended another `" confirm"`, making it impossible to confirm the action.

Root cause: `confirm`/`deny` literals were Brigadier children of the `<name>` word-argument. In Minecraft 1.21+, `RUN_COMMAND` click events re-validate against the client-side command tree; the nested literal structure was not preserved, so `"Colony confirm"` was consumed as the full name value.

Fixed by promoting `confirm` and `deny` to top-level literal siblings of `<name>` (Brigadier literals always take priority over argument nodes). Confirm/deny handlers now read the pending home name from the server-side pending map — no name is embedded in the button command. Also fixed `{HOME}`/`{home}` placeholders in message keys (were never substituted) to `{0}` and added missing message keys. Lang version `11 → 12`.

---

## 1.0.2.6+build.42 — 2026-04-24

### 🐛 Bug Fix — `/back` Fails in Unloaded Chunks

`/back` failed with *"No safe teleport location found"* whenever the death point or previous location was in an unloaded chunk — even though the destination was perfectly safe.

Root causes: (1) `TeleportUtil` only force-loaded the single target chunk, but `findSafeLocation()` scans up to ±16 blocks in X/Z, which can cross chunk boundaries. Neighbouring unloaded chunks caused every candidate to fail `isSafe()`'s `level.isLoaded()` check. (2) `MiscTeleportManager.teleportDelay` was hardcoded to `3` — the field was never populated from config, so the configured warm-up delay was silently ignored.

Fixed by adding `preloadChunksForTeleport()` (3×3 chunk grid loaded with `PORTAL` tickets before any safety check) and a new `loadConfig()` method in `MiscTeleportManager` that reads `teleportDelay`, `enableDeathBack`, and `enableTeleportBack` from `ConfigManager`.

---

## 1.0.2.6+build.41 — 2026-04-24

### 🐛 Bug Fix — Vanish Module Cannot Be Disabled

Setting `moderation.vanishSettings.enableVanishSystem: false` in config had no effect. Commands remained registered and players who were already vanished couldn't break/place blocks or interact, because the config flag was silently ignored.

Root causes: (1) `isVanishSystemEnabled()` read from the wrong JSON path — always returned `true`; (2) interaction-prevention guards never checked `isVanishSystemEnabled()`; (3) `VanishManager.onPlayerJoin()` was defined but never called, breaking vanish-state restoration on reconnect. All three issues fixed.

---



### 🔒 Security Fix — `/inv` and `/ec` Permission Bypass

`/inv` and `/ec` were registered as Brigadier `redirect()` nodes with no `requires()` predicate. Since Brigadier only evaluates the alias node's own predicate (not the target's), every player bypassed permission checks and could open any other player's inventory or ender chest.

Fixed by replacing redirect-based aliases with full command registrations that each carry their own `requires()` predicate. Also fixed a typo that prevented `/ecedit` from working, added `neoessentials.invsee` + `neoessentials.enderchest` to the moderator group, and added missing translation keys.

---

## 1.0.2.6+build.38 — 2026-04-24


### 🐛 Fix — Teleportation Messages & Cooldown/Warmup System

**Missing localisation keys** — All `commands.neoessentials.teleport.spawn.*` message keys were missing from `en_us.json`, causing raw key strings (e.g. `commands.neoessentials.teleport.spawn.fallback_success`) to appear in chat. Added all spawn, warmup, cooldown, and warp/home warmup keys. Lang version bumped to `11` — auto-merges into existing server language files on next startup.

**Cooldowns not enforced** — Multiple root causes fixed:
- `HomeManager` now reads `teleportDelay` from config and enforces `homeTeleportCooldown` on each `/home` use.
- `WarpManager` now reads and enforces `warpCooldown` (use cooldown) before each `/warp` use.
- `SpawnManager` now reads and enforces `spawnCooldown` before each `/spawn` use; `teleportDelay` moved from spawn.json override to `generalSettings.teleportDelay` in config.json.
- All TP managers now display a warmup countdown message to players when `teleportDelay > 0` and `enableTeleportWarmup=true`.

---



### ✨ Feature — Permissions GUI, External Systems & Fine-Grained Control

**Web Dashboard REST API (extended)** — `/api/permissions` now handles context overrides, temp permissions, and aliases via REST. New endpoints: `POST /reload`, `GET|POST|DELETE /group/{name}/context`, `GET|POST /group/{name}/temp`, `DELETE /group/{name}/temp/{node}`, same for users, plus `GET|POST|DELETE /aliases`. Enhanced `/system/status` includes emergency mode, adapter health, failures, and alias count.

**External System Integration** — Documented the full 5-step fallback chain, adapter health tracking (5 failures → UNHEALTHY → auto-fallback), LuckPerms context via QueryOptions, FTB Ranks 4-API probe, startup compatibility report, and a compatibility table covering all major permission mods.

**Fine-Grained Command Control** — Every subcommand has its own node (`/home set` vs `/home delete`, `/warp` vs `/setwarp`, etc.). Documented comprehensive tables for Home, Warp, Kit, Economy, Moderation, and Permission system commands. Negative permission deny patterns documented.

---

## 1.0.2.6+build.28 — 2026-04-01

### ✨ Feature — Permissions System Improvements

**Contextual Permissions** — Permissions can now be world-specific, time-of-day specific, or gamemode-specific. New `/permissions group <group> context` and `/permissions user <player> context` subcommands let admins add/remove/list contextual overrides with tab-completion for all supported context keys.

**Permission Conditions** — Optional condition expressions (e.g. `gamemode:survival AND time:day`, `health:above:10`, `op:true`) can be attached to permission nodes. The permission is only granted when the condition passes.

**Permission Aliases** — `config/neoessentials/permission_aliases.json` maps legacy or short node names to canonical NeoEssentials nodes. Resolved transparently in every permission check.

**Mod Interop API** — `NeoEssentialsAPI.getPermissionsService()` returns a `PermissionsService` interface that other mods can use to check permissions (with context), register their own nodes and aliases, and query group info without importing NeoEssentials internals.

**Storage & Audit** — Contextual permissions and conditions persist across restarts. New audit log entries: `*_CONTEXT_PERM_ADDED/REMOVED`, `*_CONDITION_SET/REMOVED`.

---

## 1.0.2.6+build.26 — 2026-04-01

### 🔧 Improvement — Utility Systems Audit & Polish

- **`/nick` / `/nickname`** — Data file path uses centralised `ResourceUtil.getConfigPath()`. `/nickname` alias now registered as a proper Brigadier redirect (was a metadata-only entry with no actual command).
- **`/seen`** — Data file path uses `ResourceUtil.getConfigPath()`.
- **Duplicate registrations removed** — Stale duplicate `registerCommand()` metadata in `NeoEssentials.java` cleaned up; every player-info command is now registered exactly once.
- **Permission registry de-duped** — Multiple permission nodes were registered twice with conflicting values; notably `neoessentials.whois` (was `ADMIN/false`, silently overridden to `MISC/true`) and `neoessentials.ping.others` (was `PLAYER/true`, overridden to `MISC/false`). Correct values now authoritative; unique sub-nodes (`whois.detailed`, `rules.admin`, `motd.*`) preserved.
- All core utility commands verified functional: `/nick` `/nickname` `/setnick` `/near` `/nearby` `/ping` `/depth` `/helpop` `/motd` `/rules` `/suicide` `/seen` `/whois` `/realname` `/msgtoggle`.

---

## 1.0.2.6+build.25 — 2026-04-01

### ⏳ New — Temporary Permissions

- **Permissions** — Time-limited permissions for players and groups that expire automatically. Supports durations like `30m`, `12h`, `1d`, `7d`, `1d12h30m`. New commands: `addtemp`, `removetemp`, `listtemp` for both `/permissions user` and `/permissions group`. Expiry runs every 30 s server-side; online players are notified when a temp perm expires. Temp perms survive restarts (stored in `playerdata.json` / `permissions.json`, expired entries stripped on load). Six new audit events: `USER_TEMP_PERM_ADDED/REMOVED/EXPIRED` and `GROUP_TEMP_PERM_ADDED/REMOVED/EXPIRED`. Two new permission nodes: `neoessentials.permissions.user.temp`, `neoessentials.permissions.group.temp`.

---

## 1.0.2.6+build.23 — 2026-04-01

### 📋 New — Permission Audit Logging

- **Permissions** — New `PermissionAuditLogger` writes every permission change to `neoessentials/permissions_audit.log` (append-only, timestamped UTC). 17 action types tracked: user group/perm changes, group create/delete/rename/clone/perm/inherit/prefix/suffix/priority changes, and reload events. Executor name logged for player commands, "CONSOLE" for server-side.
- **Config** — New `permissions.auditLogging` key (default `true`). Set `false` to disable.

---

## 1.0.2.6+build.22 — 2026-04-01

### ⚖️ Improvement — Group Priorities + Permission Suggestions

- **Permissions** — New `priority` field on permission groups (default 0). Higher priority parents are consulted first in the inheritance walk — deterministic grant/deny ordering when multiple parents conflict. Set with `/permissions group <name> setpriority <value>` (−999 to 999).
- **Permissions** — `priority` persisted in `permissions.json` (backwards-compatible).
- **Permissions** — Denial messages now show the node's registered description: `§8(e.g. "Ban a player from the server")` — staff know immediately what capability they need without checking the wiki.
- **Docs** — New "Group Priorities" section in `PermissionSystem.md`; example `groups.json` updated with priority values; `CommandsReference.md` updated.

---

## 1.0.2.6+build.21 — 2026-04-01

### 🔍 New — Permission Debugging Tools

- **Permissions** — New `/permissions debug <player>` command (requires `neoessentials.permissions.debug`). Shows system mode, adapter health/version, config flags, OP status, direct user permissions, group chain with inheritance, and a numbered resolution summary — diagnose permission issues in-game without touching logs.
- **Permissions** — New `neoessentials.permissions.debug` permission node registered.
- **Fixed** — `/permissions user check` now uses the full 5-step resolution chain (was bypassing external adapter, `opsBypassPermissions`, and `vanillaOpFallback` by calling the internal manager directly).

---

## 1.0.2.6+build.19 — 2026-04-01

### 📖 Documentation — `allowUnsafeCommands` & Security Config

- **Docs** — Fixed wrong `allowUnsafeCommands` description in `SplitConfigs.md` (previously said "enchantments/item operations" — that's `items.unsafe-enchantments`).
- **Docs** — Full `security.json` reference added: all six keys, blocked-pattern list, character allowlist, tables of commands that work vs. those needing `allowUnsafeCommands: true`.
- **Docs** — Key insight documented: tilde `~` (relative coordinates) is blocked by default — the most common source of the "dangerous operations" error in powertools.
- **Docs** — "Command Safety Filter" section added to `ItemManagement.md` under Powertool; warning note added to `CommandsReference.md` `/powertool` row; `security.json` added to `Home.md` getting-started list.

---

## 1.0.2.6+build.18 — 2026-04-01

### 🛡️ New — Fallback to Vanilla OP Permissions

- **Permissions** — New `permissions.vanillaOpFallback` (default `true`): OPs get access as a last resort after all permission systems return false. Prevents admin lockouts from corrupted configs or crashing external perm mods.
- **Permissions** — Permission system init failure no longer crashes the server — activates emergency OP-only mode with a clear console error. `/neoe reload` exits emergency mode without a restart.
- **Docs** — `PermissionSystem.md` updated with new config option, bypass-vs-fallback comparison, and updated permission-check flow.

---

## 1.0.2.6+build.17 — 2026-04-01

### 🔌 Improved — External Permissions Integration

- **Permissions** — FTB Ranks and LuckPerms adapters now detect mod version at startup and log it; a boxed `WARN` fires when the installed FTB Ranks version is newer than last tested.
- **Permissions** — New `AdapterCompatibilityChecker` prints a formatted compatibility table at startup listing every detected permission mod, its version, and ✓/⚠ status.
- **Permissions** — FTB Ranks adapter probes four API signatures so it survives version bumps without breaking.
- **Permissions** — Both adapters track consecutive failures; after 5 failures they are marked unhealthy and `PermissionAPI` falls back to the internal `permissions.json` manager and then OP-bypass — no player lockouts from a broken external perm mod.

---

## 1.0.2.6+build.16 — 2026-04-01

### ✨ New Features — Rules Command

- **Rules** — Console now logs a detailed boxed error with file path and fix steps when `rules_data.json` is corrupt or missing. Auto-generated defaults are logged with their exact path and edit instructions.
- **Rules** — `/neoe reload` now reloads server rules.
- **Rules** — Dashboard API `/api/rules` for full CRUD on server rules (list, add, edit, delete, replace all, reload from disk).
- **Docs** — Full `/rules` section added to `UtilitySystems.md`; fixed stray `/rules`, `/helpop`, `/suicide` rows in the MOTD API table.

---

## 1.0.2.6+build.15 — 2026-04-01

### 🐛 Bug Fixes

- **Split Configs** — Fixed split files never being created on fresh installs (JAR source was wrong).
- **Split Configs** — Fixed `main.json` being overwritten with only one section.
- **Split Configs** — Added missing `economy` section to `main.json`.

### ✨ New Features

- `/neoe config validate` — validate all split config files.
- `/neoe config repair` — auto-fix missing or incomplete split config files.
- `/neoe config status` — visual overview of config file health.
- Clear boxed startup errors when split files are missing with `/neoe config repair` hint.
- New `SplitConfigs.md` wiki documentation.

---

## 1.0.2.6+build.12 — 2026-04-01


### ✨ New Features

- **MOTD – Multi-Profile Support** — Multiple named MOTD profiles with `/motd profile list|create|delete|switch|info`. Each profile persists independently in `config/neoessentials/motd_data.json`.
- **MOTD – Auto-Rotation** — Cycle profiles automatically: `/motd rotation enable <minutes>|disable|next`.
- **MOTD – Dashboard API** — REST endpoint `/api/motd` for full profile & rotation management from the web dashboard.
- **MOTD – Error Feedback** — In-game error messages on save/load failures; `/motd reload` shows the exact problem.

### 🔒 New Permissions
- `neoessentials.motd.profile` (default: off)
- `neoessentials.motd.rotation` (default: off)

---

## 1.0.2.6+build.5 — 2026-04-01


### 🐛 Bug Fixes

- **Config** – Fixed `ClassCastException` crash in all kit commands when split configs are enabled. Also fixed `getConfig("chat")` throwing `FileNotFoundException`, which broke chat formatting across the mod.
- **Permissions** – Fixed OPs being denied commands when an external permission mod (FTB Ranks, LuckPerms) is installed. OP bypass is now checked before any external adapter.
- **Permissions** – Fixed FTB Ranks adapter `NoSuchMethodException` on FTB Ranks 2101.1.x (new API `getPermission(ServerPlayer, String, boolean)` now probed automatically).
- **ChestShop** – Fixed Admin Shops with `?` item — any admin with `neoessentials.shop.create.admin` can now right-click to assign the item.
- **Help** – Fixed `/help 2` and other page numbers showing "No command found" (conflict with vanilla `/help` resolved).
- **Moderation** – Fixed `/unban` not detecting vanilla-issued bans; NeoEssentials now syncs with `banned-players.json` in both directions.
- **Rules** – Fixed `/rules` ignoring existing `rules.json` from older builds; auto-migrates to `rules_data.json`.
- **MOTD** – Fixed MOTD resetting on restart due to an inconsistent save path.

---

## 1.0.2.6+build.1 — 2026-03-06

### 🔁 Starting fresh from 1.0.2.6

This is the first build of the `1.0.2.6` release series. Build number reset to 1.

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
