# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.6+build.30 — 2026-04-01

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
