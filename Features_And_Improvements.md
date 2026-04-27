# 🎯 Additional Features & Improvements

---

# 🎯 Additional Features

- **Economy Integration**  
  Expand shop systems with:
    - Chest sign shops, player chest shops, and entity-based shops.
    - Dynamic pricing support with configurable rules.
    - CSV import/export for bulk pricing adjustments. {Other Modded Support}
    - Future-proofing for more advanced economy plugins and integrations.

- **Holographic Displays**  
  Add support for holographic displays to show:
    - Shop information, player stats, server announcements.
    - Customizable text, icons, and animations.
    - Integration with permissions and PlaceholderAPI for dynamic content.

- **Minecraft Assets API Support**  
  Integrate with the Minecraft Assets API to:
    - Display item/block textures in the web dashboard.
    - Provide accurate previews for shops, kits, and inventories.
    - Enable resource syncing for external tools.

- **Web-Dashboard Improvements**  
  Enhance the NeoEssentials web dashboard with:
    - Backup/restore functionality for configs and player data.
    - Integration with cloud storage (Google Drive, Dropbox, etc.).
    - More detailed statistics (economy, player activity, performance).
    - Improved user management with role-based access control.
    - Proper Login & Authentication system for secure access, discord auth using discord bot, and session management, other oauths .
    - More intuitive UI/UX design and mobile responsiveness, more pages for different modules (teleportation, moderation, kits, etc.).

- **Port NeoEssentials to Newer Minecraft + NeoForge Versions**  
  Request to update NeoEssentials for compatibility with the latest Minecraft and NeoForge releases.
    - Requested Update:
        - Port NeoEssentials to **NeoForge 26.1.2** (latest stable).
        - Ensure compatibility with Minecraft `1.21.1` (and future patch releases).
        - Validate integration with LuckPerms `5.4.150` and other common server-side mods.
        - Regression test all modules: teleportation, MOTD, rules, kits, inventory commands, dashboard, economy, and localization.
    - Benefits:
        - Keeps NeoEssentials aligned with the latest NeoForge ecosystem.
        - Ensures server admins can upgrade without losing essential functionality.
        - Provides a stable foundation for fixing existing bugs (permissions, configs, teleportation) in the new environment.

---

# ✅ Improvements Done

- **`/nick` Nickname System — Full Visibility Fix** *(build #86)*

  Complete overhaul of the nickname display pipeline — all five failure points fixed.

  | Item | Build | Status |
  |---|---|---|
  | Tab list shows nickname for all viewers | #86 | ✅ |
  | Tab list nickname persists across relog | #86 | ✅ |
  | `{neoessentials_displayname}` resolves to nickname in chat formats | #86 | ✅ |
  | Hover/click name shows nickname (clickable player names mode) | #86 | ✅ |
  | Tab header/footer `{displayname}` token shows nickname | #86 | ✅ |

  - `NickCommand.updatePlayerDisplayName()` — replaced `player.setCustomName()` (entity cosmetic, no-op for tab/chat) with `ClientboundPlayerInfoUpdatePacket(UPDATE_DISPLAY_NAME)` broadcast to all connected players
  - `NickCommand.onPlayerJoin()` — new public method called from `TablistEventHandler`; sends the tab display-name packet on every login to restore the persistent nickname
  - `DefaultPlaceholderExpansion` — added `getNickOrDisplayName()` helper; `displayname` and `displayname_hover` cases now check `NickCommand.getNickname()` first
  - `ChatFormatter.formatMessage()` — hover/click injection block reads `NickCommand.getNickname()` for the `§HDNAME§` markup token
  - `TablistManager.getDisplayName()` — reads `NickCommand.getNickname()` for header/footer `{displayname}` token

---

- **Shop Entity / NPC Shop Compile Fixes** *(build #86)*

  Fixed 11 pre-existing compile errors in the entity shop layer that blocked every build.

  | File | Fix |
  |---|---|
  | `NpcShopMenu.java` | `clicked()` return type `ItemStack` → `void` (MC 1.21.1 API); added missing `quickMoveStack()` abstract override |
  | `ShopNpcEntity.java` | Removed bogus `damageSources()` override (`DamageSource` ≠ `DamageSources`) |
  | `ShopTransaction.java` | `resolveItem()`, `giveItems()`, `hasSpaceInContainer()` promoted to `public static` |

---

- **Messaging & SocialSpy Improvements** *(build #73)*

  Full enhancement pass on `/msg`, `/reply`, and SocialSpy — all four checklist items delivered.

  | Item | Build | Status |
  |---|---|---|
  | Named placeholders in message templates (`{sender}`, `{receiver}`, `{message}`, `{neoessentials_displayname}`) | #73 | ✅ |
  | Fallback formatting — `resolveTemplate()` never throws; returns original template on error | #73 | ✅ |
  | Debug logging for unresolved placeholders and SocialSpy format resolution (requires `logging.enableDebugLogging`) | #73 | ✅ |
  | Admin-configurable SocialSpy & PM formats in `config.json` (`chat.messaging` section) | #73 | ✅ |

  - `MessageUtil.resolveTemplate()` — centralised helper replacing manual `.replace()` + PlaceholderAPI calls; case-insensitive token matching so `{MESSAGE}` and `{message}` both work
  - `SocialSpyManager` — config-backed format, display-name pre-resolution (one PlaceholderAPI call per broadcast, not per spy player), debug logging
  - `MsgCommand` + `ReplyCommand` — both use `resolveTemplate()`; `getMsgFormat()` config helper added
  - `en_us.json` — `neoessentials.socialspy.format` updated from positional `{0}/{1}/{2}` to named `{sender}/{receiver}/{message}`; `_langVersion` 14→15
  - `config.json` — `chat.messaging` section added (`socialspyFormat`, `msgFormatTo`, `msgFormatFrom`, `replyFormatTo`, `replyFormatFrom`); `_configVersion` 20→21

---

- **NeoEssentials Proxy Integration with BungeeTabListPlus (Independent Mode)** *(build #77)*

  Complete overhaul of the NeoEssentials tablist system, inspired by BungeeTabListPlus (BTLP). NeoEssentials now manages its own tablist in **independent mode** (no proxy plugin required) while optionally integrating with BungeeCord/Velocity proxies for cross-server data. BungeeTabListPlus source code placed in `docs/BungeeTabListPlus/` for reference.

  | Item | Build | Status |
  |---|---|---|
  | Independent mode — NeoEssentials owns tablist end-to-end, no proxy plugin needed | #77 | ✅ |
  | BungeeCord plugin-messaging bridge (`GetServers`, `PlayerCount`, `GetServer`) | #77 | ✅ |
  | `{network_online}`, `{server_online:NAME}`, `{current_server}` placeholders | #77 | ✅ |
  | BTLP-style fake players (decorative/separator tab entries) via `FakePlayerManager` | #77 | ✅ |
  | Layout & sorting — columns, group-weight sort, `groupSections`, `playersByServer` | #77 | ✅ |
  | `excludeServers` / `hiddenServers` — multi-server visibility control | #77 | ✅ |
  | `/tablist proxy`, `fakeplayer`, `layout`, `independent` sub-commands | #77 | ✅ |
  | `tablist.json` `_configVersion` 2→3 with `independentMode`, `proxy`, `fakePlayers`, `layout` sections | #77 | ✅ |
  | BungeeTabListPlus source placed in `docs/BungeeTabListPlus/` | #77 | ✅ |

  **New files:** `TablistLayout.java`, `FakePlayerManager.java`, `ProxyIntegration.java`

  **Updated files:** `TablistManager.java` (full BTLP-inspired rewrite), `TablistCommand.java` (new sub-commands), `TablistEventHandler.java` (session/proxy lifecycle), `tablist.json`

  > ⚠️ **Known limitation:** Outbound BungeeCord plugin-messaging (polling proxy for counts) is a stub pending NeoForge `StreamCodec` registration. Inbound proxy responses and all independent-mode features work fully. Proxy integration is disabled by default (`proxy.enabled: false`).

---

- **Custom Player Tablist** *(builds #67, #69)*

  Full rewrite of the tablist system — all four checklist items delivered.

  | Item | Build | Status |
  |---|---|---|
  | Animated header/footer (frame arrays, `refreshInterval`) | #67 | ✅ |
  | Hex colors & gradients (`&#RRGGBB`, `<gradient:…>`, `<rainbow>`) | #67 | ✅ |
  | Per-group customisation (header/footer, prefix/suffix, runtime commands) | #67 | ✅ |
  | Per-player customisation (header/footer, runtime commands, UUID config section) | #67 | ✅ |
  | Rich-text in player-row prefix/suffix column (scoreboard teams) | #69 | ✅ |
  | Dedicated `RichTextFormatter.processTablistText()` (tablist-safe pipeline) | #69 | ✅ |

  **build.67 — core feature**
  - `TablistManager` fully rewritten: animated frame cycling, per-player/group override maps, extended placeholder set (`{displayname}`, `{server_name}`, `{x}/{y}/{z}`, `{balance}`, `{time}`, `{bar}`), `groupColors` map, vanish + AFK integration, null-safe permission helpers
  - `TablistCommand` expanded: `/tablist player <name> header|footer|reset` and `/tablist group <group> header|footer|reset` branches added; help text updated with color/gradient syntax examples
  - `tablist.json` bundled template updated with gradient header example, per-group and per-player UUID sections, `groupColors` map

  **build.69 — polish**
  - `RichTextFormatter.processTablistText(String)` added — strips hover/click markers (invalid in tablist packets), runs full gradient → rainbow → color-tag → format-tag pipeline unconditionally
  - `updatePlayerTeam()` routed through `processTablistText()` — hex/gradient group prefixes now render in the player-name column
  - `applyPlaceholders()` early `&`→`§` conversion removed — color processing fully deferred to `processTablistText()` so `&#RRGGBB` and `<gradient:…>` survive placeholder substitution

---

- **Localization Improvements** *(build #64)*

  All four checklist items delivered.

  | Item | Build | Status |
  |---|---|---|
  | Audit all commands for missing translation keys (54 keys added) | #64 | ✅ |
  | Fallback text in English when a key is missing | #64 | ✅ |
  | Tooling to regenerate/validate language files | #64 | ✅ |
  | Server-admin override of messages via config (`/language override`) | #64 | ✅ |

  **54 missing translation keys** added to `en_us.json` — TPA/teleport-request flow (25 keys), misc teleport, spawn/warp coordinate errors, home overwrite/delete fallbacks, moderation messages, dashboard, channel, mutelist, near, gamemode.

  **`MessageUtil` improvements:** `localize()` now strips `commands.neoessentials.` prefix and capitalises to produce a readable English fallback when a key is missing. New `localize(key, fallback, args...)` overload for callers that know the expected English text.

  **New `/language` admin commands:** `validate <code>` (coverage % + missing/extra key diff), `regenerate <code>` (refresh + merge from JAR, auto-backup), `override set|get|remove|list|clear|reload` (per-key runtime overrides persisted to `overrides.json`).

  **`_langVersion` bumped 12 → 13** — triggers automatic key-merge on next server start for existing deployments.

  Affected files: `en_us.json`, `MessageUtil.java`, `CustomLanguageManager.java`, `LanguageCommand.java`

---

- **Permissions System Improvements** *(builds #25, #28, #30)*

  All 8 planned improvements fully implemented across three builds.

  | Item | Build | Status |
  |---|---|---|
  | Permission Expiry (temp perms) | #25 | ✅ |
  | Contextual Permissions | #28 | ✅ |
  | API for Other Mods | #28 | ✅ |
  | Permission Aliases | #28 | ✅ |
  | Custom Permission Conditions | #28 | ✅ |
  | GUI Management (web dashboard API) | #30 | ✅ |
  | Integration with External Systems | #30 | ✅ |
  | Fine-Grained Command Control | #30 | ✅ |

  **Permission Expiry** *(build #25)*
  - `tempPermissions: Map<String, Long>` added to `PermissionUser` and `PermissionGroup`
  - `PermissionExpiryHandler` purges expired entries every 30 s (600 ticks)
  - `/permissions user/group addtemp <node> <duration>` · `removetemp` · `listtemp`
  - Auto-notifies online players when their temp perm expires
  - Persisted in `playerdata.json` / `permissions.json`; expired entries discarded on load

  **Contextual Permissions** *(build #28)*
  - `PermissionContext` value object captures `worldId`, `dayTime`, `gamemode`
  - `PermissionUser` / `PermissionGroup` extended with `contextualPermissions` map
  - `PermissionManager.hasPermission(UUID, String, PermissionContext)` — 9-step context-aware resolution chain
  - `/permissions user/group <target> context add <contextKey> <node> allow|deny` · `remove` · `list`
  - Context keys: `world:overworld`, `time:day`, `time:night`, `gamemode:survival/creative/spectator/adventure`
  - Contextual overrides persisted in JSON; fully backward-compatible

  **API for Other Mods** *(build #28)*
  - `PermissionsService` interface: `hasPermission`, `getGroup`, `getPrefix`, `getSuffix`, `registerPermission`, `registerAlias`, `getAliases`, `isEmergencyMode`, `isUsingExternalAdapter`, `getGroupNames`, `getPlayerPermissions`, `contextFor`
  - `PermissionsServiceImpl` singleton wires interface to `PermissionAPI` + internal managers
  - Exposed via `NeoEssentialsAPI.getPermissionsService()`; `API_VERSION` bumped to `1.1.0`

  **Permission Aliases** *(build #28)*
  - `PermissionAliasManager` singleton with load/save to `permission_aliases.json`
  - Aliases resolved transparently before every permission check in `PermissionAPI.hasPermission`
  - Register via file or `PermissionsService.registerAlias()`

  **Custom Permission Conditions** *(build #28)*
  - `PermissionConditionManager` evaluates boolean expressions: `time:day`, `time:night`, `world:X`, `gamemode:X`, `health:above/below:N`, `op:true/false`
  - Compound expressions: `gamemode:survival AND time:day`, `world:overworld OR world:the_nether`
  - Conditions stored per-node on user/group; evaluated at grant time — if condition fails, grant is withheld

  **GUI Management — Web Dashboard** *(build #30)*
  - 15 new REST endpoints on `/api/permissions`:
    - `POST /reload` — reload from disk
    - `GET|POST|DELETE /group/{name}/context` — group contextual override CRUD
    - `GET|POST /group/{name}/temp` + `DELETE /group/{name}/temp/{node}` — group temp perm CRUD
    - `GET|POST|DELETE /user/{name}/context` — user contextual override CRUD
    - `GET|POST /user/{name}/temp` + `DELETE /user/{name}/temp/{node}` — user temp perm CRUD
    - `GET|POST|DELETE /aliases` — alias CRUD (POST persists to `permission_aliases.json`)
    - `GET /system/status` enhanced — emergency mode, adapter name/version/health/failures, alias count
  - `PermissionSystem.md` updated: new **Temporary Permissions** section with duration table, command tables, resolution-order explanation, worked example, and audit-event table.
  - `CommandsReference.md` updated: 6 new rows (`addtemp`/`removetemp`/`listtemp` for user and group) added to the Permissions Management table.

---

- **Improved External Permissions Integration** *(build #17)*
    - ✅ `FtbRanksAdapter` and `LuckPermsAdapter` detect the installed mod version via `ModList` at construction time and log it at `INFO` level.
    - ✅ Boxed `WARN` emitted at startup when FTB Ranks is newer than the last-tested minor version, prompting admins to report the version mismatch.
    - ✅ New `AdapterCompatibilityChecker` class generates a formatted compatibility table at startup listing all detected permission mods with ✓/⚠ status.
    - ✅ FTB Ranks adapter probes four API signatures (current, legacy, future static, alternative naming) before giving up — significantly more resilient to version bumps.
    - ✅ `ExternalPermissionAdapter` interface extended with `getVersion()`, `isHealthy()`, and `getConsecutiveFailures()` default methods (source-compatible; no changes required in existing implementations).
    - ✅ Both adapters track consecutive runtime failures; after 5 failures the adapter declares itself unhealthy and a single boxed `WARN` is logged.
    - ✅ `PermissionAPI.hasPermission()` now falls back to the internal `permissions.json` manager (and then OP-bypass) whenever the external adapter is unhealthy or throws — non-OP players can never be locked out purely because an external permission mod is broken.

- **Rules Command Configuration Improvements** *(build #16)*
    - ✅ Added full `/rules` section to `UtilitySystems.md` — command table, colour codes, data-file format, legacy migration note, dashboard API reference, and console feedback examples.
    - ✅ `rules_data.json` is always auto-generated with 10 default rules on first startup; generated file path is logged with quick-start edit instructions.
    - ✅ `/api/rules` dashboard endpoint added — full CRUD: list, add, edit, delete, replace all, reload from disk — all protected by Bearer-token auth.
    - ✅ Detailed boxed console error when `rules_data.json` fails to load (corrupt JSON or I/O error), including absolute path and step-by-step fix instructions.
    - ✅ `/neoe reload` now reloads server rules alongside all other systems.
    - ✅ `RulesCommand` now uses `ResourceUtil.getConfigPath()` (consistent with every other data file in the mod).

- **Improved Split Config Support** *(build #14)*
    - ✅ All module config files (`main.json`, `commands.json`, `chat.json`, `teleportation.json`, `moderation.json`, `webdashboard.json`, `items.json`, `afk.json`, `security.json`, `tablist.json`) are automatically generated from the bundled JAR `config.json` on fresh installs and when split mode is first activated — no longer silently fails when split files don't exist in the JAR.
    - ✅ Added `economy` section to `main.json` (was previously missing from `CONFIG_FILE_MAP`).
    - ✅ Fixed overwrite bug where `ensureSplitConfigsUpToDate()` processed sections one at a time, causing `main.json` to be overwritten with only one section. Now processes files atomically using `FILE_SECTIONS_MAP` (file → sections).
    - ✅ Missing split files produce clear boxed error messages in the console with exact remediation instructions (`/neoe config repair`).
    - ✅ Added `validateSplitConfigs()` — returns a list of every problem (missing file, parse error, missing section) with fix instructions.
    - ✅ Added `repairSplitConfigs()` — regenerates missing files and fills missing sections from JAR defaults without overwriting user values.
    - ✅ New commands: `/neoe config validate`, `/neoe config repair`, `/neoe config status`.
    - ✅ `SplitConfigs.md` wiki created with full documentation: file layout, migration guide, `allowUnsafeCommands` location, version tracking, and repair/disable instructions.

- **MOTD Improvements** *(build #12)*
    - ✅ MOTD is saved to `config/neoessentials/motd_data.json` and persists across restarts.
    - ✅ Multiple named MOTD profiles supported (`/motd profile list|create|delete|switch|info`).
    - ✅ Auto-rotation between profiles on configurable interval (`/motd rotation enable <minutes>|disable|next`).
    - ✅ Full REST endpoint at `/api/motd` for dashboard editing (CRUD profiles, switch active, rotation control, broadcast).
    - ✅ Clear in-game error feedback when MOTD fails to load (`/motd reload` shows the exact I/O error) or save (shows error in-game instead of silent log-only failure).
    - ✅ Legacy single-MOTD `motd_data.json` automatically migrated to multi-profile format on first load.

- **Teleportation System Improvements** *(build #50)*
    - ✅ Added missing `back_warmup` and `back_cooldown` language keys to `en_us.json` (were referenced in `MiscTeleportManager.java` but absent, causing raw key strings in chat).
    - ✅ Documented all 10 cooldown/warmup bypass permission nodes in `permissions_nodes.txt` (`neoessentials.teleport.bypass.cooldown`, `neoessentials.teleport.bypass.warmup`, plus per-command home/warp/spawn/back variants).
    - ✅ Created `TeleportEndpoint.java` — new REST API (`GET/PUT /api/teleport/settings`) for reading and live-writing all teleport config sections (General, Home, Warp, Spawn, Back/Misc) from the dashboard without a server restart.
    - ✅ Created `teleport.html` + `teleport.js` — new "🌀 Teleport Settings" dashboard page with five settings sections and a Save & Apply button that reloads all managers instantly.
    - ✅ Added `MiscTeleportManager.reload()` method to support live dashboard config reload.
    - ✅ Registered `/api/teleport` endpoint in `DashboardAPI`; added `teleport.html` + `teleport.js` to `DashboardFileManager` managed file list.
    - ✅ Added "🌀 Teleport Settings" nav link (admin-only) to all dashboard pages (`index.html`, `admin.html`, `permissions.html`).
    - ✅ Dashboard script cache-bust version bumped to `419`.

- **Teleportation — Per-Command Bypass Permissions & Safety/Chunk Documentation** *(build #55)*
    - ✅ Registered all 8 per-command cooldown/warmup bypass permission nodes in `PermissionRegistry.java` (`neoessentials.teleport.home.bypass.cooldown`, `neoessentials.teleport.home.bypass.warmup`, `neoessentials.teleport.warp.bypass.cooldown`, `neoessentials.teleport.warp.bypass.warmup`, `neoessentials.teleport.spawn.bypass.cooldown`, `neoessentials.teleport.spawn.bypass.warmup`, `neoessentials.teleport.back.bypass.cooldown`, `neoessentials.teleport.back.bypass.warmup`). Code already checked them, but they were absent from the registry so tools (dashboard, `/neoe permissions`) could not discover them.
    - ✅ Added **"Chunk Loading & Safety Interaction"** section to `docs/Wiki/TeleportationSystem.md` explaining: 3×3 chunk preload before every teleport, order of operations (chunks first, then safety scan), effect of disabling safety checks, error behavior on failed chunk loading, and a configuration quick-reference table.

- **Inventory Management & Security Improvements** *(build #56)*
    - ✅ **Config enable/disable wired** — `InventoryViewCommands` `requires()` predicates now check `ConfigManager.isCommandEnabled("invsee")` / `isCommandEnabled("invseeedit")` / `isCommandEnabled("enderchest")` / `isCommandEnabled("enderchestedit")`. When set to `false` in `config.json` the command vanishes from tab-completion and returns a permission error on use. Previously the config flags in `commands.*` were written but never read.
    - ✅ **Anti-duplication concurrent-edit lock** — Two `ConcurrentHashMap<UUID targetId, UUID viewerId>` maps (`activeInvEdits`, `activeEcEdits`) enforce that only one staff member may hold an editable view of a given player's inventory or ender chest at a time. A second attempt is blocked with a message naming the current editor. Locks are cleaned up automatically on viewer disconnect via the new `InventoryEventHandler` (`@EventBusSubscriber`).
    - ✅ **Persistent inventory audit log** — New `InventoryAuditLogger` writes every view/edit open event to `neoessentials/inventory_audit.log` (append-only, UTC timestamp). 7 action types: `INV_VIEWED`, `INV_EDIT_OPENED`, `INV_EDIT_CLOSED`, `EC_VIEWED`, `EC_EDIT_OPENED`, `EC_EDIT_CLOSED`, `EDIT_BLOCKED`. Controlled by new config key `items.inventoryAuditLog` (default `true`).
    - ✅ **New language keys** — `commands.neoessentials.invsee.disabled`, `commands.neoessentials.invsee.concurrent_edit`, `commands.neoessentials.ec.disabled`, `commands.neoessentials.ec.concurrent_edit` added to `en_us.json`.
    - ✅ **Permission nodes** — `neoessentials.invsee`, `neoessentials.invsee.edit`, `neoessentials.enderchest`, `neoessentials.enderchest.edit` already registered in `PermissionRegistry` (default `false` → OP-only without explicit grant). Dashboard can discover and display them via the permissions page.

- **Chat Formatting Options** *(build #57)*
    - ✅ **Per-player override wired into chat pipeline** — `ChatHandler.onServerChat()` now consults `PlayerChatFormatManager.getInstance().getFormat(player.getUUID())` **before** calling `chatManager.getChatFormat(group, world)`. Per-player overrides set via `/chatformat set <player> <format>` are now the highest-priority step in the format resolution chain. Previously, `PlayerChatFormatManager` persisted overrides but they were never applied during actual chat.
  - ✅ **Format priority chain (highest → lowest):** per-player override → group+world key → group key → world key → default format.
  - ✅ **All rich-text features already implemented and now documented** — `RichTextFormatter` and `ChatFormatter` support: `&#RRGGBB` hex colors, `<gradient:RRGGBB-RRGGBB>text</gradient>`, `<rainbow>text</rainbow>`, `<hover:text:Tooltip>visible</hover>`, `<click:run_command:/cmd>`, `<click:open_url:...>`, `<bold>`, `<italic>`, and all legacy `&` codes. No new code needed.
  - ✅ **`ChatSystem.md` fully rewritten** — Added: Format Priority Hierarchy diagram, `/chatformat` command reference table with all 5 subcommands and permission nodes, complete rich-text tag reference with copy-paste syntax examples, hex color and gradient usage, hover/click event examples, full config key reference table, placeholder list, and working format string examples.

- **API & Placeholder System** *(build #58)*
    - ✅ **`PlaceholderProvider` and `PlaceholderExpansion` made public** — extracted to `public` top-level types so external mods can implement/extend them (were previously package-private).
    - ✅ **`NeoEssentialsAPI.getPlaceholderManager()`** added — exposes the singleton `PlaceholderManager` from the stable API entry-point. `API_VERSION` bumped to `"1.2.0"`.
    - ✅ **`/api/placeholders` REST endpoints** — `PlaceholderEndpoint` added: `GET /api/placeholders/list`, `GET /api/placeholders/resolve?player=&text=`, `GET /api/placeholders/stats`. Registered with auth middleware in `DashboardAPI`.
    - ✅ **`/api/docs` wired** — `DocumentationHandler` was implemented but never registered in `DashboardAPI`. Now wired to `/api/docs` context.
    - ✅ **`/placeholder` command** — new in-game admin command with `list`, `info <id>` (tab-completes), `test <text>`, `stats` sub-commands. Permission: `neoessentials.admin.placeholders`.
    - ✅ Registered `neoessentials.admin.placeholders` permission node in `PermissionRegistry` under `ADMIN` category.
    - ✅ **`DocumentationManager`** updated with `placeholder-api` and `developer-api` sections, and API docs for all three `/api/placeholders/*` endpoints.
    - ✅ **`docs/Wiki/APISystem.md`** completely rewritten — full built-in placeholder table (30+ tokens with short-form aliases), `PlaceholderProvider`/`PlaceholderExpansion` code examples, `NeoEssentialsAPI` full reference, REST endpoint tables, `/placeholder` command reference, versioning contract.

- **Chat Formatting — `{neoessentials_username_hover}` unresolved + duplicate vanilla log line** *(build #59)*
    - ✅ **Root cause fixed** — `ChatFormatter.formatMessage()` was replacing `{neoessentials_username}` with `{neoessentials_username_hover}` when "clickable player names" was enabled, but `username_hover` was never registered in `DefaultPlaceholderExpansion`. The placeholder passed through `PlaceholderAPI.setPlaceholders()` unresolved, leaving the literal string `{neoessentials_username_hover}` in the formatted Component.
    - ✅ **New approach** — The `{username}` → `{username_hover}` substitution is replaced with a `§HNAME§` and `§HDNAME§` internal markup token (only injected when both `clickablePlayerNames` and `enableChatEnhancements` are true). `buildComponentFromMarkup()` now handles `§HNAME§` and `§HDNAME§` tokens to produce proper hover+click Components without touching the placeholder resolution pipeline.
    - ✅ **Fallback safety** — `username_hover` and `displayname_hover` are now registered in `DefaultPlaceholderExpansion` as plain-text aliases for `username`/`displayname`. If the token ever appears in a raw config string it resolves to the player's name instead of showing unresolved.
    - ✅ **Duplicate vanilla log removed** — `ChatHandler.onServerChat()` called `server.sendSystemMessage(formattedMessage)` which caused vanilla's `MinecraftServer` logger to emit a second log line: `<{neoessentials_username_hover}> message`. This call was redundant (chat was already logged via `LOGGER.info`) and is removed.
    - **Affected files:** `ChatFormatter.java`, `ChatHandler.java`, `DefaultPlaceholderExpansion.java`

---

