# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## [1.0.2.6+build.16] — 2026-04-01

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

## [1.0.2.6+build.15] — 2026-04-01

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

## [1.0.2.6+build.12] — 2026-04-01


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
