# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.6+build.19 — 2026-04-01

### 📖 Documentation — `allowUnsafeCommands` & Security Configuration

- **Fixed** `SplitConfigs.md` had a completely wrong description for `allowUnsafeCommands` ("Allow enchantments and item operations beyond vanilla limits" — that's the `items` section).  Replaced with accurate full documentation.
- **Added** Complete `security.json` reference table covering all six keys with types, defaults, and descriptions.
- **Added** Detailed `allowUnsafeCommands` breakdown: every blocked substring explained, the character allowlist, explicit call-out that tilde `~` (Minecraft relative coords) is blocked by default — the most common cause of admin confusion.
- **Added** Tables of commands that work vs. commands that need `allowUnsafeCommands: true`, with step-by-step enable instructions for both split-config and monolithic mode.
- **Added** "Command Safety Filter" subsection in `ItemManagement.md` under Powertool — exact error messages, quick-reference tables, config location.
- **Added** Warning callout on the `/powertool` row in `CommandsReference.md`.
- **Added** `security.json` to the getting-started key files list in `Home.md`.

---

## 1.0.2.6+build.18 — 2026-04-01

### 🛡️ New Feature — Fallback to Vanilla OP Permissions

- **Added** `permissions.vanillaOpFallback` config key (default `true`). After all permission systems (external adapter + internal manager) have been consulted and returned `false`, OPs (level 2+) are granted access as a last resort. Prevents lockouts when FTB Ranks crashes or `permissions.json` is corrupted.
- **Added** Emergency mode: if the permission system fails to initialise at startup, the server no longer crashes. Instead, NeoEssentials activates emergency mode — OPs get all permissions, everyone else is denied — and logs a clear boxed error asking the admin to fix the config and run `/neoe reload`.
- **Improved** `/neoe reload` now detects emergency mode and performs a full re-initialisation so the system can recover without a server restart.
- **Docs** `PermissionSystem.md` updated with new config table row, comparison table (bypass vs fallback), and updated "How Permissions Work" section.

---

## 1.0.2.6+build.17 — 2026-04-01

### 🔌 Improved — External Permissions Integration

- **Added** `FtbRanksAdapter` and `LuckPermsAdapter` now detect the installed mod version at startup and log it. A boxed `WARN` is emitted when FTB Ranks is newer than the last-tested minor version.
- **Added** `AdapterCompatibilityChecker` — a startup compatibility report that lists every detected permission mod, its installed version, last-tested version, and ✓/⚠ status.
- **Improved** FTB Ranks adapter now probes four API signatures (current static, legacy instance, future static, alternative naming) so it stays functional across FTB Ranks version bumps.
- **Added** Both adapters now track consecutive runtime failures. After 5 failures the adapter is marked **unhealthy** and a boxed `WARN` is logged.
- **Improved** `PermissionAPI` now falls back to the **internal `permissions.json`** (and then OP-bypass) when the external adapter is unhealthy or throws — non-OP players are never locked out due to a broken external permission mod.

---

## 1.0.2.6+build.16 — 2026-04-01

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
