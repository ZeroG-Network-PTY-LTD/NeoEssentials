# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.6+build.124 — 2026-05-13

### ✨ Feature — `/dashboard update` Smart File Comparison

The `/dashboard update` command now performs **per-file MD5 checksum comparison** instead of unconditionally overwriting all dashboard files.

**New subcommands:**
- `/dashboard update` — Smart update: only overwrites files whose content differs from the bundled JAR version. Reports which files were added, updated, or already up-to-date.
- `/dashboard update check` — **Dry-run preview**: shows exactly which files would change without writing anything.
- `/dashboard update force` — Bypass checksums and replace every file unconditionally (old behavior).

**`/dashboard status`** now shows the installed dashboard file version vs. the current build, with a hint when an update is available.

**Files changed:** `DashboardFileManager.java`, `DashboardCommand.java`

---



### ✨ Feature — Hologram System Improvements

Significant quality-of-life and visual enhancements to the hologram system.

**New visual properties (persisted in `holograms.json`):**
- `scale` — uniform text scale from 0.1× to 10× (default 1.0×)
- `lineSpacing` — configurable gap between lines in blocks (default 0.3, was hardcoded)
- `textShadow` — toggle the Minecraft text drop-shadow (default off)
- `textOpacity` — text opacity 0–255 (default 255 = fully opaque)
- `backgroundColorArgb` — background panel colour as ARGB integer (default `0x00000000` transparent)

**New commands:**
- `/hologram copy <id> <newid>` — clone a hologram with all settings and lines
- `/hologram movehere <id>` — snap hologram to the caller's position (no coordinate typing)
- `/hologram near [radius]` — list holograms within `radius` blocks (default 20), sorted by distance
- `/hologram insertline <id> <index> <text>` — insert a line at any position (not just the end)
- `/hologram addframes <id> <lineIndex> <intervalTicks> <frame1|frame2|...>` — set frame animation on any line
- `/hologram removeframes <id> <lineIndex>` — clear frame animation from a line
- `/hologram scale <id> <scale>` — set text scale
- `/hologram linespacing <id> <spacing>` — set line spacing
- `/hologram shadow <id> on|off` — toggle text shadow
- `/hologram opacity <id> <0-255>` — set text opacity
- `/hologram background <id> <transparent|#RRGGBB|#AARRGGBB>` — set background panel colour

**Dashboard API update:**
- All hologram JSON responses now include `billboard`, `spin`, `hover`, `scale`, `lineSpacing`, `textShadow`, `textOpacity`, and `backgroundColorArgb` fields (previously missing).

**Wiki:** New `docs/Wiki/HologramSystem.md` with full command reference, examples, and JSON schema documentation.

**Files changed:** `HologramData.java`, `HologramRenderer.java`, `HologramCommand.java`, `HologramEndpoint.java`, `docs/Wiki/HologramSystem.md`

---

## 1.0.2.6+build.120 — 2026-05-11

### 🐛 Bug Fix — `/help` Pagination (and Higher Pages) Now Works

**What was wrong:**
- `/help 2` (and higher page numbers) showed *"No command found"* due to a conflict with vanilla's `/help` command registration.
- Vanilla's `/help <command>` would consume numeric input before NeoEssentials' pagination logic could process it.

**What's fixed:**
- NeoEssentials now owns `/help` and `/?` registration during command setup, so page arguments route correctly.
- Resolved Brigadier shadowing that caused the numeric input issue.

**File:** `HelpCommand.java`

---

## 1.0.2.6+build.119 — 2026-05-08

### 🐛 Bug Fix — `/tpa` Landing on Nether Roof / Underwater Caves

Players reported two teleport issues:
- Teleporting to players in the Nether could land on top of the bedrock roof.
- Teleporting to players in oceans/boats could land in caves below the seafloor.

**Fixes applied:**
- `TeleportLocation.scanColumnTopDown()` now caps scan start Y to `dimensionType().logicalHeight() - 1`, preventing Nether-roof selections.
- `/tpa` now teleports to the target player's actual live position (`findSafe=false`) instead of remapping to distant "safe" spots.
- `findSafeLocation()` now first checks a ±16 Y neighborhood around the destination before doing full top-down fallback, improving nearby safety resolution for other teleport systems.

**Files:** `TeleportLocation.java`, `TeleportRequestManager.java`

---

## 1.0.2.6+build.115 — 2026-05-08

### ✨ Feature — Interactive Shop Holograms

The hologram above sign shops is now interactive:
- **Right-click hologram** = buy (same as right-clicking the sign)
- **Left-click hologram** = sell (same as left-clicking the sign)
- **Owner click** = show shop info panel

Interaction reuses the same permission checks and transaction pipeline as sign interaction.

**Files:** `ShopHologramManager.java`

---

## 1.0.2.6+build.113 — 2026-05-08

### ✨ Feature — Hologram Billboard + Spin + Hover

Holograms now support per-hologram display orientation and animation:
- Billboard modes: `fixed`, `vertical`, `horizontal`, `center` (player-facing)
- Optional spin animation with configurable speed and axis (`X|Y|Z`)
- Optional hover/bob animation with configurable amplitude and speed

**New commands:**
- `/hologram billboard <id> <fixed|vertical|horizontal|center>`
- `/hologram spin <id> on [speed] [axis]`
- `/hologram spin <id> off`
- `/hologram hover <id> on [amplitude] [speed]`
- `/hologram hover <id> off`

`/hologram info <id>` now includes billboard/spin/hover state.

**Files:** `HologramData.java`, `HologramRenderer.java`, `HologramScheduler.java`, `HologramCommand.java`

---

## 1.0.2.6+build.112 — 2026-05-04

### 🐛 Bug Fix — `/back` Returns "No Previous Location" After Death

After dying, `/back` always returned *"No previous location to return to"* even though the death location should have been saved.

**What was wrong (multiple issues fixed together):**

- `MiscTeleportManager`'s `@EventBusSubscriber` was missing `bus = Bus.GAME`. While NeoForge defaults to the game bus, not being explicit could cause registration to silently fail in edge cases — matching the explicit pattern used by all other event classes in the mod.
- The death event handler used the default `receiveCanceled = false`. If another mod cancelled `LivingDeathEvent` *before* our NORMAL-priority handler ran (e.g. keep-inventory mods), the handler was skipped and the death position was never saved. Changed to `receiveCanceled = true` so we always capture the position when a `ServerPlayer` dies.
- `PlayerDataStore.flush()` could silently fail when the `back_locations/` directory didn't exist yet, leaving death positions un-persisted across server restarts. Now logs an ERROR and creates the directory if missing.
- Added an explicit `teleportation.backSettings` section to `config.json` so `enableDeathBack`, `enableTeleportBack`, `teleportDelay`, and `backCooldown` have clear defaults that admins can easily adjust.

**Diagnostic logging:** `onPlayerDeathEvent` now logs an INFO message with coordinates every time a player dies, making it easy to confirm NeoEssentials is receiving the event.

---



### 🐛 Bug Fix — Chat Format Strings Saved with Garbled Characters

Gson's default HTML-escaping was silently converting `<`, `>`, and `&` to `\u003c`, `\u003e`, `\u0026` whenever config or data files were written. This corrupted chat format strings like `<{prefix} {name}> {MESSAGE}` the moment they were saved.

Added `.disableHtmlEscaping()` to every `GsonBuilder` instance in the mod (30+ files), including `ConfigSplitter`, `ConfigManager`, `PlayerChatFormatManager`, `AfkManager`, all moderation managers, scheduler, web-dashboard classes, mail/nick/rules commands, language files, and more.

---

## 1.0.2.6+build.107 — 2026-05-04

### 🐛 Bug Fix — Chat Configuration Not Loading (`chat.json`)

**Error seen:** `Failed to read config file chat: config/neoessentials/chat (No such file or directory)`

Even with `chat.json` present and split configs enabled, the chat format would fall back to the built-in default (`[{neoessentials_displayname}: {MESSAGE}]`) if the config cache was stale — for example immediately after running `/neoe config split` before a reload.

**What's fixed:**

- `ConfigManager.getConfig("chat")` now falls back to reading `chat.json` directly from disk and correctly unwraps the nested `"chat"` section when the merged MAIN_CONFIG cache doesn't contain it.
- `ConfigSplitter` now clears the config cache immediately after migration to split files, so the stale monolithic view is evicted right away without requiring a manual `/neoe reload`.

---

## 1.0.2.6+build.102 — 2026-05-04

### 🐛 Bug Fix — Shop NPC Disconnects All Clients on Join

**Error:** `The server sent registries with unknown keys: ResourceKey[minecraft:entity_type / neoessentials:shop_npc]`

Every player was kicked during login because NeoForge syncs custom entity-type registries to clients and the `shop_npc` type was not present on the client side.

**Fix:** Shop NPCs are now vanilla `ArmorStand` entities tagged with an NBT UUID key (`NeoEssentials_ShopId`). Interaction is captured via `PlayerInteractEvent.EntityInteract` — no client-side changes required, no custom entity type registered.

### 🐛 Bug Fix — Hologram Files Failing to Compile (UTF-8 BOM)

Six hologram-related source files contained a UTF-8 BOM character causing `error: illegal character: '\ufeff'` at compile time. BOM stripped from all six files.

### 🐛 Bug Fix — Permission Validator Incorrectly Flagging External Mod Permissions

Permissions from other mods (WorldEdit, FTB Ranks, etc.) were being logged as "unknown" NeoEssentials nodes. The validator now silently skips any node not prefixed with `neoessentials.` and reports a separate external-skip count instead.

### 🐛 Bug Fix — Missing Permission Nodes (`compass`, `msgtoggle.bypass`)

Three permission nodes (`neoessentials.compass`, `neoessentials.compass.others`, `neoessentials.chat.msgtoggle.bypass`) were in use but unregistered, generating spurious validator warnings. All three are now registered with correct default values.

---

## 1.0.2.6+build.91 — 2026-04-27

### ✨ Web Dashboard — Backup & Restore

Create, browse, restore, and download named server snapshots directly from the dashboard:

- **Create Snapshot** — select targets (Configs, Mod Data, Player Data) and a name; ZIP is stored in `neoessentials/backups/`
- **Restore** — automatically creates a pre-restore safety backup before overwriting live files
- **Download** — download the ZIP archive directly from the browser
- **Delete** — remove snapshots with a confirmation dialog
- **Auto-prune** — oldest snapshots removed when count exceeds 20
- New `💾 Backup & Restore` page added to the dashboard sidebar

### 🔧 Code Quality (build #90)
- Added missing `@Nonnull` annotations and `@SuppressWarnings("resource")` for IntelliJ false-positive `Level`/`ServerLevel` warnings

---

## 1.0.2.6+build.86 — 2026-04-27

### 🐛 Bug Fix — `/nick` Nickname Not Showing in Tab List or Chat

`/nick` sent a success message but the nickname was invisible everywhere — the tab list still showed the real username, and chat still showed the real name.

**Root cause:** `updatePlayerDisplayName()` was calling `player.setCustomName()`, which is the entity cosmetic API for mob name tags. On player entities it adds a *second* floating label rather than replacing anything. The tab list, chat format, and all placeholders remained untouched.

**What's fixed:**

- **Tab list** — `NickCommand` now broadcasts `ClientboundPlayerInfoUpdatePacket(UPDATE_DISPLAY_NAME)` to every connected player whenever a nick is set or cleared. The player's row in the tab list shows the nickname immediately.
- **Chat** — `{neoessentials_displayname}` placeholder now resolves to the player's nickname (if set) before falling back to the scoreboard display name. Works in all chat formats and SocialSpy.
- **Hover/click names** — when `chat.clickablePlayerNames` is enabled, the hover component now shows the nickname.
- **Tab header/footer** — `{displayname}` token in tablist layouts now shows the nickname.
- **Reconnect persistence** — nick is re-applied to the tab list on every login (was lost on relog).

---

### 🐛 Bug Fix — Shop Entity Compile Errors (11 errors → 0)

Pre-existing compile errors in the NPC Shop entity layer prevented the mod from building.

- **`NpcShopMenu`** — `clicked()` return type fixed (`void` in MC 1.21.1, was incorrectly `ItemStack`); missing `quickMoveStack()` abstract override added.
- **`ShopNpcEntity`** — removed bogus `damageSources()` override with incompatible return type (`DamageSource` vs `DamageSources`).
- **`ShopTransaction`** — `resolveItem()`, `giveItems()`, `hasSpaceInContainer()` made `public` for cross-package access from `NpcShopMenu`.

---

## 1.0.2.6+build.77 — 2026-04-27

### ✨ Feature — BungeeTabListPlus-Inspired Tablist (Independent Mode + Proxy Integration)

Complete overhaul of the tablist system inspired by **BungeeTabListPlus** — the industry-standard proxy tablist plugin.

**What's new:**
- **Independent mode** (default) — NeoEssentials owns the entire tablist without any proxy plugin. Works on any NeoForge server.
- **20+ BTLP-style placeholders** — `{network_online}`, `{server_online:NAME}`, `{current_server}`, `{server_label}`, `{rank_weight}`, `{session_minutes}`, `{session_hours}`, `{level}`, `{health}`, `{max_health}`, `{afk}`, `{displayname}`, and all existing standard tokens.
- **Fake player entries** — decorative tab-list rows (separators, section labels, padding). BTLP's `fakePlayers` concept. Config: `tablist.fakePlayers`.
- **Group-weight sorting** — players sorted by permission group weight (admins first), then alphabetically. Uses scoreboard teams as sort keys (BTLP `ContextAwareOrdering`).  
- **Layout configuration** — 1–4 columns, `groupSections`, `playersByServer`, `excludeServers`, `hiddenServers` (all BTLP config parity).
- **Proxy integration** — optional BungeeCord bridge for cross-server player counts. Disabled by default (`proxy.enabled=false`). Enable when behind a proxy; polls `GetServers`/`PlayerCount` to populate `{network_online}` and `{server_online:X}`.
- **New `/tablist` sub-commands**: `proxy status`, `proxy setserver`, `fakeplayer list/add/remove/refresh`, `layout info`, `independent on/off`, and improved `info` view.
- `tablist.json` `_configVersion` **2→3** — added `independentMode`, `proxy`, `fakePlayers`, `layout` sections.

**Bug fixes:**
- Fixed duplicate class definition in `TablistCommand.java` (compile error).
- Fixed `ClientboundPlayerInfoUpdatePacket` construction error in `FakePlayerManager` (NeoForge 1.21.x API).
- Fixed `CustomPacketPayload#write()` override error in `ProxyIntegration` (method removed in NeoForge 1.21.1).

---

## 1.0.2.6+build.73 — 2026-04-27

### ✨ Feature — Messaging & SocialSpy Improvements

- **Named placeholder support in all message templates** — `/msg`, `/reply`, and SocialSpy formats now support `{message}`, `{MESSAGE}`, `{sender}`, `{receiver}`, `{sender_displayname}`, `{receiver_displayname}`, and all `{neoessentials_*}` PlaceholderAPI tokens. Both `{message}` and `{MESSAGE}` are accepted (case-insensitive).
- **Fallback formatting on template parse failures** — `MessageUtil.resolveTemplate()` never throws. If PlaceholderAPI fails, the original template is returned safely.
- **Debug logging for unresolved placeholders** — when `logging.enableDebugLogging = true`, any `{TOKEN}` still present after full resolution is logged with a WARN, making template misconfigurations easy to diagnose.
- **Admin-configurable formats in `config.json`** — new `chat.messaging` section lets admins override SocialSpy and PM format templates without editing language files:
  ```json
  "messaging": {
    "socialspyFormat":  "&8[&eSocialSpy&8] &b{sender} &7→ &b{receiver}&7: &f{message}",
    "msgFormatTo":    "", "msgFormatFrom":   "",
    "replyFormatTo":  "", "replyFormatFrom":  ""
  }
  ```
  Leave blank to use the language-file default.
- **SocialSpy format updated** — `neoessentials.socialspy.format` in `en_us.json` now uses `{sender}`, `{receiver}`, `{message}` named vars instead of `{0}`, `{1}`, `{2}`.
- `_langVersion` → 15 · `_configVersion` → 21

---

## 1.0.2.6+build.72 — 2026-04-27

### 🐛 Bug Fix — FTB Ranks Permission Check (`NoSuchMethodException`)

- **FTB Ranks permissions now work correctly with `ftb-ranks-neoforge-2101.1.3`** — The adapter was probing non-existent API methods, causing all FTB Ranks permission checks to quietly return `false`. The adapter now correctly calls `FTBRanksAPI.getPermissionValue(ServerPlayer, String)` (the actual static method in 2101.1.x) and interprets the returned `PermissionValue` via `asBooleanOrFalse()`.
- Additional fallback strategies kept for older FTB Ranks versions.

---

## 1.0.2.6+build.70 — 2026-04-27

### 🐛 Bug Fix — `/msg` & `/reply` SocialSpy Formatting

- **Private messages no longer show raw template text** — `MessageUtil.localize()` was passing message templates containing `{placeholder}` tokens directly to `java.text.MessageFormat`, which threw `IllegalArgumentException` on non-numeric tokens. Non-numeric placeholders are now escaped before `MessageFormat.format()` runs.
- **`neoessentials.socialspy.format` translation key added** — fixes a missing-key crash in the SocialSpy listener.
- `_langVersion` bumped `13 → 14` — new key auto-merges on next startup.

---

## 1.0.2.6+build.69 — 2026-04-24

### ✨ Feature — Custom Player Tablist: Polish Pass

- **New `processTablistText()` method** in `RichTextFormatter` — a tablist-safe rich-text processor that strips hover/click events and runs the full gradient/rainbow/hex/format pipeline, enabled regardless of the `enableChatEnhancements` setting.
- **Player prefix/suffix column now supports hex colors and gradients** — group prefixes defined with `&#RRGGBB` or gradient tags now render as true colored text in the tab-list player rows.
- **Color processing deferred correctly** — placeholder substitution no longer converts `&`→`§` prematurely; color parsing is fully deferred to `processTablistText()` so complex color codes survive intact.

---

## 1.0.2.6+build.67 — 2026-04-24

### ✨ Feature — Custom Player Tablist

Full rewrite of the tablist system with rich-text, animations, and per-group/per-player customisation.

**New features:**

- **Hex colors & gradients** — use `&#RRGGBB`, `<gradient:FF0000-0000FF>text</gradient>`, `<rainbow>`, named color/format tags in any header, footer, or group prefix
- **Animated header/footer** — set `header`/`footer` to a JSON array for smooth frame-by-frame animations (speed set by `refreshInterval`)
- **Per-group header/footer** — `"groups"` section in `tablist.json` gives each permission group its own frames (e.g. a staff-only header panel)
- **Per-player header/footer** — `"players"` UUID section in `tablist.json`; also settable at runtime with new `/tablist player <name> header|footer|reset` commands
- **Per-group runtime commands** — `/tablist group <group> header|footer|reset` for live adjustments without reloading the config
- **Extended placeholders** — `{displayname}`, `{server_name}`, `{x}`, `{y}`, `{z}`, `{balance}`, `{time}`, `{bar}` added (full list in `tablist.json` template comments)
- **Vanish + AFK support** — `{online}` excludes vanished players for non-staff viewers; configurable AFK suffix shown next to AFK players
- **Updated `tablist.json` config template** — gradient header example, annotated group/player sections, color syntax reference

---

## 1.0.2.6+build.66 — 2026-04-24

### 🐛 Bug Fix — Tablist prefix, Warn logging, WarnManager compile error

- **Tablist prefix now shows correctly** — Group prefix/suffix no longer silently returns empty; the system falls back to the configured default group when a player has no explicit user record.
- **Warn console logging restored** — `/warn` now always logs to the server console/log file regardless of op-broadcast settings.
- **WarnManager compile error fixed** — Removed duplicate `getInstance()` method that prevented the mod from building.

---

## 1.0.2.6+build.64 — 2026-04-24

### 🌐 Improvement — Localization Audit & Admin Tooling

Full audit of all in-game translation key usage, 54 missing keys added, and a new `/language` tooling suite for server admins.

**What's new / fixed:**

- **54 missing translation keys** added to `en_us.json` — primarily in the TPA/teleport-request flow (`request.sent`, `request.received`, `request.denied`, `request.expired`, `request.failed`, etc.), misc teleport (`/jumpto`, `/back` info), spawn/warp coordinate errors, moderation (`unfrozen_message`, `reason_too_long`), and several general/utility messages
- **Human-readable fallbacks** — if a translation key is missing, players now see a readable English phrase (e.g. `Home not found`) instead of the raw key string (`commands.neoessentials.home.not_found`)
- **`/language validate <code>`** — compare any language file against `en_us.json`, get coverage % and a list of missing/extra keys
- **`/language regenerate <code>`** — refresh a language file from the bundled JAR copy, merge user translations, auto-backup to `.bak`
- **`/language override`** — override individual message keys in-game; persisted to `overrides.json` and take priority over all language files (`set`, `get`, `remove`, `list`, `clear`, `reload` sub-commands)
- `_langVersion` bumped **12 → 13** — new keys auto-merged on existing deployments at next server start

---

## 1.0.2.6+build.59 — 2026-04-24

### 🐛 Bug Fix — Chat: Unresolved `{neoessentials_username_hover}` & duplicate server log

Two related chat-formatting bugs fixed that caused `{neoessentials_username_hover}` to appear literally in player chat (instead of the player's name) and produced a duplicate log line from vanilla's `MinecraftServer` logger.

**Root cause:** When "clickable player names" was enabled, `ChatFormatter` swapped `{neoessentials_username}` for `{neoessentials_username_hover}` — a placeholder that was never registered, so it was never resolved.

**Fixes:**
- Replaced the broken placeholder-swap with an internal markup token (`§HNAME§name§/HNAME§`) that `buildComponentFromMarkup()` now resolves into a proper hover+click `Component` — no placeholder resolution step needed
- Added `username_hover` / `displayname_hover` to `DefaultPlaceholderExpansion` as plain-text fallbacks
- Removed the redundant `server.sendSystemMessage(formattedMessage)` call in `ChatHandler` that was causing vanilla's logger to emit a second `<{neoessentials_username_hover}> message` line

---

## 1.0.2.6+build.58 — 2026-04-24

### 🔌 Feature — API & Placeholder System

Exposes the NeoEssentials placeholder system as a fully public Java API for external mods, adds in-game admin tooling, REST endpoints, and rewrites developer documentation.

**What's new:**

- **`PlaceholderProvider`** and **`PlaceholderExpansion`** are now `public` top-level types — external mods can implement/extend them to register custom placeholders
- **`NeoEssentialsAPI.getPlaceholderManager()`** — new entry-point to the thread-safe `PlaceholderManager` (API version bumped to `1.2.0`)
- **`/api/placeholders/list`**, **`/api/placeholders/resolve`**, **`/api/placeholders/stats`** — new authenticated REST endpoints for external tools
- **`/api/docs`** now serves the built-in documentation system (was implemented but never wired)
- **`/placeholder`** in-game command — `list`, `info <id>`, `test <text>`, `stats` sub-commands for admins (permission: `neoessentials.admin.placeholders`)
- **`neoessentials.admin.placeholders`** permission node registered in the permission registry
- **`docs/Wiki/APISystem.md`** completely rewritten — full built-in placeholder table (30+ tokens), `PlaceholderProvider` and `PlaceholderExpansion` code examples, `NeoEssentialsAPI` reference, REST endpoint tables, versioning contract

---

## 1.0.2.6+build.57 — 2026-04-24

### ✨ Feature — Chat Formatting: Per-Player Overrides Now Applied

**Bug fixed:** Per-player chat format overrides were stored by `/chatformat set` but **never applied** — `ChatHandler` always used the group/world format. `PlayerChatFormatManager.getFormat()` is now checked first in the format resolution chain.

**Format priority (highest → lowest):** per-player override → group+world → group → world → default

- **Fixed** `ChatHandler.onServerChat()` to check `PlayerChatFormatManager.getInstance().getFormat(uuid)` before falling through to `chatManager.getChatFormat(group, world)`.
- **All rich-text features already implemented** and now fully documented: hex colors (`&#RRGGBB`), `<gradient:RRGGBB-RRGGBB>`, `<rainbow>`, `<hover:text:Tooltip>`, `<click:run_command:/cmd>`, `<bold>`, `<italic>`, legacy `&` codes.
- **Updated** `ChatSystem.md` — format priority diagram, `/chatformat` command table, complete rich-text tag reference with copy-paste examples, placeholder list, and full config key reference.

**Per-player format commands:** `/chatformat set <player> <format>` · `/chatformat clear <player>` · `/chatformat check <player>` · `/chatformat list` · `/chatformat reload`

---

## 1.0.2.6+build.56 — 2026-04-24

### ✨ Feature — Inventory Management & Security Improvements

- **Fixed** Config enable/disable flags for inventory commands were present in `config.json` but never read at runtime. `/invsee`, `/inv`, `/invseeedit`, `/enderchest`, `/ec`, `/enderchestedit`, `/ecedit` now check their respective `commands.*` flags — setting one to `false` hides the command from tab-completion and blocks execution.
- **Added** Concurrent-edit lock: only one staff member may hold an editable inventory view of a given player at a time. A second attempt is blocked with a message naming the current editor. Locks release automatically on viewer disconnect.
- **Added** `InventoryAuditLogger` — every inventory view or edit open is appended to `neoessentials/inventory_audit.log` (UTC timestamp, 7 action types: `INV_VIEWED`, `INV_EDIT_OPENED`, `INV_EDIT_CLOSED`, `EC_VIEWED`, `EC_EDIT_OPENED`, `EC_EDIT_CLOSED`, `EDIT_BLOCKED`). Controlled by new `items.inventoryAuditLog` config key (default `true`).
- **Added** 4 new language keys: `invsee.disabled`, `invsee.concurrent_edit`, `ec.disabled`, `ec.concurrent_edit`.
- **Permission nodes** `neoessentials.invsee`, `neoessentials.invsee.edit`, `neoessentials.enderchest`, `neoessentials.enderchest.edit` remain OP-only by default (unchanged).

---

## 1.0.2.6+build.55 — 2026-04-24

### 🔧 Improvement — Per-Command Teleport Bypass Permissions & Chunk Loading Docs

- **Fixed** 8 per-command cooldown/warmup bypass permission nodes were already enforced in code but absent from `PermissionRegistry` — they were invisible to the dashboard and `/neoe permissions`. All 8 nodes now registered: `neoessentials.teleport.home.bypass.cooldown`, `.warmup`, `warp.bypass.cooldown`, `.warmup`, `spawn.bypass.cooldown`, `.warmup`, `back.bypass.cooldown`, `.warmup`.
- **Added** "Chunk Loading & Safety Interaction" section to `TeleportationSystem.md` — explains the 3×3 chunk preload, safety-scan order of operations, what happens when safety is disabled, and a config quick-reference table.

---

## 1.0.2.6+build.50 — 2026-04-24

### ✨ Teleportation Improvements — Dashboard Settings Page, Language Keys & Permission Docs

- **Added** `/api/teleport/settings` REST endpoint — view and live-update all teleport cooldowns, warmup delays, safety flags, and max-homes/warps from the web dashboard without restarting the server.
- **Added** "🌀 Teleport Settings" dashboard page (`teleport.html`) with sections for General, Home, Warp, Spawn, and Back settings. Save & Apply reloads all managers instantly.
- **Added** missing language keys `teleport.misc.back_warmup` and `teleport.misc.back_cooldown` (were referenced in Java but absent from `en_us.json`, causing fallback raw-key display).
- **Added** documentation for all 10 cooldown/warmup bypass permission nodes to `permissions_nodes.txt` (`neoessentials.teleport.bypass.cooldown`, `neoessentials.teleport.bypass.warmup`, plus per-command home/warp/spawn/back variants).
- **Added** `MiscTeleportManager.reload()` so the new dashboard endpoint can apply config changes without a restart.
- Dashboard `?v=` cache-bust bumped to `419`; new files added to managed dashboard file list.

---

## 1.0.2.6+build.46 — 2026-04-24

### 🐛 Bug Fix — Web Dashboard Admin Controls & Permissions Page Blank After Login

Navigating to Admin Controls or Permissions from the sidebar produced a completely blank page. Pressing F5 would briefly show the buttons before they vanished.

- **Fixed** `showLoginScreen()` in `dashboard.js` — on `admin.html` and `permissions.html` there is no login form, so calling this function previously blanked the whole page. It now redirects to `index.html` instead.
- **Fixed** `permissions.js` initialisation — the old guard checked `window.location.hash` / `data-page` neither of which ever matched on the standalone `permissions.html` page, so tabs showed "Loading…" indefinitely. Now detects the correct page via `document.getElementById('permOverviewTab')`.
- **Fixed** 9 `fetchWithAuth` calls in `permissions.js` that were missing `.json()` (e.g. `viewGroupPermissions`, `editUserPermissions`, `deleteGroup`, permission add/remove modals). Every modal action silently showed "Failed" because `response.success` was checked on the raw `Response` object.
- **Fixed** username not displaying in the topbar on sub-pages (`admin.html`/`permissions.html` use `id="userName"` while `index.html` uses `id="usernameDisplay"`). `showDashboard()` now checks both IDs.
- Dashboard JS/HTML cache-bust version bumped to `418`.

---

## 1.0.2.6+build.44 — 2026-04-24


### 🐛 Bug Fix — Home Confirmation Buttons Append "confirm" to Name

Clicking `[Confirm]` on a `/sethome` overwrite or `/delhome` deletion prompt failed with *"Invalid home name: Colony confirm"*, and each subsequent click appended another `" confirm"` to the name.

- **Root cause**: `confirm`/`deny` literals were nested under the `<name>` word-argument in the Brigadier tree. In Minecraft 1.21+, the client re-validates `RUN_COMMAND` strings against the server-sent Brigadier tree; the nested literal was not recognised, so the full string `"Colony confirm"` was sent as the name argument.
- **Fixed** by moving `confirm` and `deny` to top-level literal siblings of `<name>` in `sethome`/`createhome` and all `delhome` aliases. Brigadier always gives literals priority over argument nodes.
- **Fixed** confirm/deny handlers to read the pending home name from the server-side map instead of the command argument — the home name is no longer embedded in the button click command.
- **Fixed** `delete_success`, `delete_cancelled`, `overwrite_success`, `overwrite_failed` and related message keys to use `{0}` (`MessageFormat`-style) instead of `{HOME}`/`{home}` (which were never substituted). Added missing key variants for pending-state error messages.
- Lang version bumped `11 → 12`; new keys auto-merge into existing deployments on next startup.

---

## 1.0.2.6+build.42 — 2026-04-24

### 🐛 Bug Fix — `/back` Fails in Unloaded Chunks

`/back` (return to death point or previous location) failed with *"No safe teleport location found"* whenever the target was in an unloaded chunk, even when the destination was completely safe.

- **Fixed** `TeleportUtil` now pre-loads a 3×3 chunk grid (target chunk + all 8 neighbours) before running any safety check or `findSafeLocation()` scan. Previously only the single target chunk was loaded; the scan radius can cross chunk boundaries, so neighbouring unloaded chunks caused every candidate position to fail `isSafe()`.
- **Fixed** `MiscTeleportManager.teleportDelay` was hardcoded to `3` and never read from config. A new `loadConfig()` method now reads `teleportation.backSettings.teleportDelay` (falling back to `generalSettings.teleportDelay`) so the configured warm-up delay is properly respected.
- **Added** `ConfigManager.getBackTeleportDelay()`, `isDeathBackEnabled()`, and `isTeleportBackEnabled()` to expose the back-command config values.

---

## 1.0.2.6+build.41 — 2026-04-24

### 🐛 Bug Fix — Vanish Module Cannot Be Disabled

Setting `moderation.vanishSettings.enableVanishSystem: false` had no effect — commands stayed registered and interaction prevention remained active.

- **Fixed** `ConfigManager.isVanishSystemEnabled()` reading from the wrong config path (was checking root-level, key is actually at `moderation.vanishSettings.enableVanishSystem`).
- **Fixed** `ModerationEventHandler` vanish interaction guards now check `isVanishSystemEnabled()` before blocking block-break / block-place / item-use.
- **Fixed** `VanishManager.onPlayerJoin()` is now called from `ModerationEventHandler.onPlayerLogin()` (gated by the enabled flag), so vanish state is correctly restored and the vanish reminder is shown when a vanished player reconnects.

---



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
