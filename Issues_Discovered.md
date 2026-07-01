---
---
#  Issues That Were Discovered

- Player Warps having issues

---

- config missing its own key we added for languages.

---

# ✅ Issues That Were Fixed

## 🐛 Bug Fix — Inventory & Ender Chest Duplication Exploit (NeoForge 1.21.1, build.1.0.2.6+119, fixed build.214)

**`InventoryViewCommands.java` / `PlayerInventoryContainerMenu.java`**

**Root cause (read-only mode):** `/invsee`, `/inv`, `/ec`, `/enderchest` created a
`SimpleContainer` filled with `.copy()` items and opened it via the standard
`ChestMenu.threeRows` / `ChestMenu.sixRows`.  Standard chest menus allow items to be
freely moved out of the container, so a viewer could drag copies into their own
inventory while the originals remained in the target's inventory — duplicating every
item they touched.

**Fix:** Both read-only open methods now use `buildReadOnlyMenu()`, a custom
`AbstractContainerMenu` factory whose top-section slots override `mayPickup() → false`
and `mayPlace() → false`.  Items are display-only and cannot be removed or inserted.

**Secondary fix (edit mode desync):** `PlayerInventoryContainerMenu` now registers the
viewer's own inventory and hotbar slots.  Previously they were absent, causing
server-client desync when the viewer tried to move items between the target's inventory
and their own.

---

## 🐛 Bug Fix — Shop Hologram Not Removed When Sign Is Broken (NeoForge 1.21.1, fixed build.214)

**`ShopSignHandler.java`**

When a player physically broke a shop sign, no event handler called `ShopManager.removeShop()`.
The shop entry remained in `shops.json` and the hologram entity stayed floating in the world.

**Fix:** Added `BlockEvent.BreakEvent` listener to `ShopSignHandler`.  When a `SignBlock` is
broken, any shop at that position is removed atomically via `ShopManager.removeShop()`, which
in turn calls `ShopHologramManager.deleteShopHologram()` to clean up the entity.

---

## 🐛 Bug Fix — Shop Hologram Orphaned After Manual shops.json Edit (NeoForge 1.21.1, fixed build.214)

**`ShopHologramManager.java` / `ShopManager.java` / `NeoEssentials.java`**

Manually deleting a shop from `shops.json` left its hologram ID in `holograms.json`.
On the next server start or `/chestshop reload` the hologram entity was re-spawned
even though no matching shop existed.

**Fix:** Added `ShopHologramManager.cleanOrphanedShopHolograms()`.  It computes the
expected hologram ID for every active shop, then removes any `shop_*` holograms from
`HologramManager` that are not in that set.  Called after both managers are loaded at
server start, and at the end of `ShopManager.reload()`.

---

## ✅ TPA Message Key Already Fixed (build.157)

`commands.neoessentials.teleport.request.recived` (typo) was corrected to
`commands.neoessentials.teleport.request.received` and sender context was added in
build 157.  Confirmed absent from codebase in build.214.

---

## 🐛 Bug Fix — Chat Rich Text Formatting Ignores Config (NeoForge 1.21.1, build.1.0.2.6+119)

**`RichTextFormatter.java` / `ChatFormatter.java` — Rich text applied even when `rich_text=false`**

Rich text formatting was applied to chat messages even when disabled in configuration.

- Observed: Debug output showed `After rich text: [[Admin] OtaaRL > d]` even with `rich_text=false` in config.
- **Root Cause 1**: `preprocessTags()` only gated gradient/rainbow tags on `isRichTextEnabled()` but processed named-color and format tags (`<red>`, `<bold>`, etc.) unconditionally — stripping the tag markers from the string and applying color/format regardless of the setting.
- **Root Cause 2**: The non-enhancement path in `ChatFormatter.formatMessage()` called `processRichText(formatted)` using the raw un-preprocessed string, bypassing the config gate entirely.

**Fixes applied:**

| File | Change |
|---|---|
| `RichTextFormatter.java` | All tag processing (gradient, rainbow, named-color, format tags) now gated on `isRichTextEnabled()`. Added `stripAllRichTags()` helper that removes tag markers without applying any formatting, used when rich text is disabled. |
| `ChatFormatter.java` | Non-enhancement path now uses `richPreProcessed` (the output of `preprocessTags()`) instead of the raw `formatted` string. Added debug logging showing rich text enabled/disabled state. |

---

## 🗑️ Feature Removed — Web Dashboard 429 Too Many Requests (NeoForge 1.21.1, build.1.0.2.6+119)

**N/A — Entire webdashboard removed**

The web dashboard was reporting `429 Too Many Requests` errors — browser console showed repeated `HTTP 429` failures in `loadServerInfo()` inside `dashboard.js`, causing a perpetual "Connection Error" loop that could not be interrupted.

- Observed: Client-side `refreshData()` loop spammed API endpoints; server-side rate limiting rejected the requests; dashboard became permanently unusable.
- **Resolution**: The entire webdashboard feature was **completely removed** from NeoEssentials. All dashboard HTML/JS/CSS files, server-side endpoint handlers (`DashboardHttpServer`, `AuthHandler`, `CommandExecutionHandler`, `FileManagementHandler`, `PermissionEndpoint`, etc.), and all references in `NeoEssentials.java`, `ConfigManager.java`, `ConfigSplitter.java`, and `ModRootCommand.java` were deleted. The 429 rate-limiting issue, client-side refresh loop, and all related stability concerns are no longer applicable.

---

## 🐛 Bug Fix — Teleportation Unsafe Fallback Triggered (NeoForge 1.21.1, build.1.0.2.6+119)

**`SpawnManager.java` — `/spawn` always fell back to world-spawn when target chunks were unloaded**

Three interlocking bugs caused `/spawn` (and less commonly `/home`) to show "teleportation is unsafe" messages and fall through to the vanilla world-spawn fallback, even when safety checks were disabled in config.

**Root Cause 1 — Safety check ran BEFORE chunks were loaded (primary bug):**
`SpawnManager.teleportToSpawn()` called `spawnLocation.isSafe()` before any chunk-loading had occurred. `TeleportLocation.isSafe()` returns `false` whenever `!level.isLoaded(pos)` — so if the spawn world's chunks weren't already resident in memory (common in multiworld setups), `isSafe()` returned `false`, `findSafeLocation()` likewise found nothing (all candidate positions were also unloaded), and `teleportToWorldSpawn()` was unconditionally invoked. The server log showed: `Player Ovaredge teleported to world spawn fallback`.
`TeleportUtil.teleportPlayer()` already force-loads the surrounding 3×3 chunk grid *before* checking safety, but the pre-check in `SpawnManager` ran *before* `TeleportUtil` was ever called — making the chunk-loading code unreachable for this code path.

**Root Cause 2 — `requireSafeLocation` overridden by stale `spawn.json` value:**
The constructor calls `loadConfig()` first (reads `enableSpawnSafety` from `config.json`), then calls `loadSpawn()` which read `requireSafeLocation` from the legacy `config` section of `spawn.json` — **overwriting** the value just set by `loadConfig()`. If `spawn.json` was saved when safety was enabled (the default), setting `enableSpawnSafety: false` in `config.json` had no effect — it was silently reverted on every server start.

**Root Cause 3 — `teleportToWorldSpawn()` hardcoded `findSafe=true`:**
The fallback always passed `true` for safety checks regardless of what `requireSafeLocation` was configured as, making it impossible to safely reach the vanilla world spawn when safety was disabled.

**Fixes applied:**

| File | Change |
|---|---|
| `SpawnManager.java` | `teleportToSpawn()`: added `TeleportUtil.preloadChunksForTeleport()` for the 3×3 chunk grid **before** the safety check — matching the pattern already in `HomeManager.teleportToHome()`. Safety is now read at runtime via `ConfigManager.isSpawnSafetyEnabled()` (not from the cached field). `TeleportUtil.teleportPlayer()` is called with `findSafe=false` since safety is fully resolved above, matching `HomeManager`. |
| `SpawnManager.java` | `loadSpawn()`: removed the `requireSafeLocation` read from the legacy `spawn.json` config section. Safety config is the sole responsibility of `config.json` via `loadConfig()` / `isSpawnSafetyEnabled()`. |
| `SpawnManager.java` | `teleportToWorldSpawn()`: changed hardcoded `findSafe=true` → `requireSafeLocation` so the fallback respects the configured safety setting. |
| `ConfigManager.java` | Added `isSpawnSafetyEnabled()` — reads `teleportation.spawnSettings.enableSpawnSafety` at runtime, analogous to `isHomeTeleportSafetyEnabled()`. |

Debug logging added throughout `teleportToSpawn()` — when `logging.enableDebugLogging = true`, log lines show which chunk grid is being preloaded, whether safety is active, and when spawn is moved to a safe location.

---

## ✨ Feature — Build #158 — 2026-05-25

**Named Animation System — `{animation:NAME}` placeholder**

- Tablist had no support for named, reusable text animations defined in a dedicated config file.
- Requested: an `animations.json` file containing named animations (each with `frames[]` and `frameDuration`), with a universal `{animation:NAME}` placeholder usable anywhere in the mod.
- **Implemented:**
  - `AnimationManager.java` — new singleton that loads `animations.json`, ticks frame indices on every server tick using wall-clock ms, and resolves `{animation:NAME}` tokens.
  - `animations.json` — new default config bundled with the mod; includes `Rainbow`, `PulseStar`, `StatusDot`, `LoadingDots`, `GoldBanner`, `Spinner`, `HeartBeat`.
  - `TablistManager` — `AnimationManager.tick()` called on every server tick (before refresh-rate guard); `resolveAnimations()` called at end of `applyPlaceholders()`.
  - `TablistCommand` — new `/tablist animations list` sub-command.
  - `ConfigManager` — `ANIMATIONS_CONFIG = "animations.json"` registered as a version-tracked config (v1).
  - `tablist.json` `_doc_header` updated to document `{animation:NAME}`; `_configVersion` bumped `3 → 4`.

---

## 🐛 Bug Fix — Build #157 — 2026-05-25

**`TeleportRequestManager.java` — TPA confirmation message showed `"to you"` instead of the expiry countdown**

- `sendTeleportRequest()` computed a `typeText` string (`"to you"` for `/tpa`, `"you to them"` for `/tpahere`) to describe the request direction, then incorrectly passed it as argument `{1}` of the **sender's** confirmation message `"commands.neoessentials.teleport.request.sent"` (`"Teleport request sent to {0}. Expires in {1} second(s)."`).
- Result: `"Teleport request sent to Xtron. Expires in to you second(s)."` — `"to you"` appeared where the seconds countdown should be.
- **Root cause**: `{1}` in the `sent` message expects the expiry time (an integer), but `typeText` (a string) was passed instead. `typeText` should only be used in the **target-side** `received` message (`"{0} wants {1}. Use /tpaccept or /tpdeny."`).
- **Fix**: `sent` message now receives `requestTimeoutSeconds` as `{1}`; `typeText` is kept only for the `received` message.
- **Bonus**: Aligned `typeText` values from `"to you"` / `"you to them"` to `"to teleport to you"` / `"you to teleport to them"` to match the phrasing already used in `getPendingRequestInfo()`.

---

## 🧹 Code Quality Pass — Build #156 — 2026-05-25

**`Arrays.asList` → `List.of` / `Set.of` sweep**

- **`ConfigSplitter.java`** — `FILE_SECTIONS_MAP` entry for `main.json`: `Arrays.asList(...)` → `List.of(...)`.
- **`ProxyIntegration.java`** — Runtime `knownServers.addAll(Arrays.asList(servers))` → `Collections.addAll(knownServers, servers)`.
- **`PermissionsCommand.java`** — All 15+ inline `java.util.Arrays.asList(...)` → `java.util.List.of(...)` (tab-completion lists).
- **`FunCommands.java`** — Inline colour list `Arrays.asList(...)` → `List.of(...)`; removed now-unused `import java.util.Arrays`.
- **`ItemCustomisationCommands.java`** — Inline `Arrays.asList(...)` → `List.of(...)`.
- **`UtilityCommands.java`** — Static final + inline `Arrays.asList(...)` → `List.of(...)`.
- **`ServerAdminCommands.java`** — `private static final TIME_NAMES = Arrays.asList(...)` → `List.of(...)`.
- **`WorldInteractionCommands.java`** — Two static final lists: `Arrays.asList(...)` → `List.of(...)`; removed `import java.util.Arrays`.
- **`DashboardFileManager.java`** — `private static final DASHBOARD_FILES`: `Arrays.asList(...)` → `List.of(...)`.
- **`AuthHandler.java`** — `roles.addAll(Arrays.asList("Admin", "Moderator", "Staff"))` → `Collections.addAll(roles, "Admin", "Moderator", "Staff")`; removed `import java.util.Arrays`.
- **`CommandExecutionHandler.java`** — `new HashSet<>(Arrays.asList(...))` static final set → `Set.of(...)`.
- **`FileManagementHandler.java`** — `ALLOWED_PATHS`: `Arrays.asList(...)` → `List.of(...)`; `EDITABLE_EXTENSIONS`: `new HashSet<>(Arrays.asList(...))` → `Set.of(...)`.
- **`AfkManager.java`** — `new HashSet<>(java.util.Arrays.asList(...))` → `new HashSet<>(java.util.List.of(...))`.

**`.get(0)` → `.getFirst()` modernisation (Java 21)**

- **`WarnManager.java`** — `entry.getValue().get(0).getTargetName()` → `.getFirst()`.
- **`JailCommand.java`** — `locations.get(0).name` → `.getFirst()`.
- **`NpcShopCommand.java`** — `nearby.get(0)` → `.getFirst()`.
- **`RealnameCommand.java`** — Two `matches.get(0)` → `.getFirst()`.
- **`DiscordPermissionSync.java`** — `permissions.get(0)` → `.getFirst()`.
- **`ProxyIntegration.java`** — `players.get(0)` → `.getFirst()` in `getAnyPlayer()`.
- **`TaskScheduler.java`** — `commands.get(0)` → `.getFirst()`.

**`ProxyIntegration.java` additional fixes**

- Added `@SuppressWarnings("unused")` to `BUNGEE_CHANNEL` and `BUNGEE_CHANNEL_LEGACY` (public API constants, referenced externally).
- Added `//noinspection unused` + `@SuppressWarnings("unused")` to `onPluginMessage()` (registered via NeoForge plugin-messaging API, not called from Java code directly).
- Renamed unused `player` param in stub `sendBungeeMessage()` to `ignoredPlayer`.
- Added `//noinspection unused` + `@SuppressWarnings("unused")` to `isShowNetworkPlayers()` (public API).

**`CommandExecutionHandler.java` additional fixes**

- Added `//noinspection unused` + `@SuppressWarnings("unused")` to the class — handler is registered by the web-dashboard init code, not instantiated via normal Java call chain visible to IntelliJ.
- Added `@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")` to `commandOutputs` — the map is written for future use; IntelliJ "Contents of collection ... are updated but never queried".
- `commandHistory.remove(0)` → `commandHistory.removeFirst()` (Java 21).

**`FileManagementHandler.java` fix**

- Added `//noinspection resource` before `p.serverLevel()` call — `ServerLevel` implements `AutoCloseable` but its lifecycle is managed entirely by the Minecraft server; closing it manually would be incorrect.

---

## 🧹 Code Quality Pass — Build #155 — 2026-05-25

**PermissionScanner.java**
- Replaced `Arrays.asList()` with `List.of()` for `PERMISSION_PATTERNS` and `DYNAMIC_PATTERNS` static final fields — `List.of()` is unmodifiable and null-hostile, correctly expressing immutable intent.
- Removed unused `import java.util.Arrays`.
- Removed always-true `if (sourcePath != null)` null-check — `Paths.get(URI)` never returns null; IDE reported "Condition is always true".
- Removed `throws IOException` from `scanJarFile()` signature — the entire method body is wrapped in `catch (Exception e)`, so `IOException` is never thrown to callers; IDE reported "Checked exception never thrown".
- Fixed `peek()` optimization warning — replaced `stream.peek(this::scanClassFile).count()` with `.toList()` + `forEach()`. In Java 21, the terminal `count()` operation may short-circuit intermediate `peek()` calls; collecting first guarantees all elements are processed.
- Renamed unused `source` parameter in `addDiscoveredPermission()` to `ignoredSource`.
- Added `//noinspection unused` + `@SuppressWarnings("unused")` to `getFilePermissionMap()`, `generateDynamicPermissions()`, and `exportDiscoveredPermissions()` (intentional public API surface).

**ExternalPermissionProvider.java**
- `collect(Collectors.toList())` → `.toList()` (×3); removed `import java.util.stream.Collectors`.
- Added `//noinspection unused` + `@SuppressWarnings("unused")` to `getPermissionsStartingWith()` and `exportForPermissionsEX()` (intentional public API surface).

**PermissionValidator.java**
- Removed unused `import java.util.stream.Collectors` (no `Collectors.` usage in file).

**PermissionManager.java**
- Inline `collect(java.util.stream.Collectors.toList())` → `.toList()`.

**BaltopCommand.java**
- Removed unused `import java.util.stream.Collectors`.

**ModerationManager.java / WarnManager.java**
- `collect(Collectors.toList())` → `.toList()`; removed `Collectors` imports.

**BanCommand.java / FreezeCommand.java / JailCommand.java / VanishCommand.java (moderation/commands)**
- `collect(Collectors.toList())` → `.toList()` (×2 each for Ban/Freeze/Jail); removed `Collectors` imports.

**ModRootCommand.java**
- `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.

**DocumentationManager.java**
- `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.

**EconomyLeaderboard.java**
- `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.

**KitManager.java**
- `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.

**ListKitsCommand.java**
- `collect(Collectors.toList())` → `.toList()`; `Collectors` import kept (`Collectors.toSet()` still used).

**ShopEntityManager.java**
- `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.

**HelpCommand.java / ListCommand.java / RealnameCommand.java / ServerAdminCommands.java / UtilityCommands.java**
- `collect(Collectors.toList())` → `.toList()` (multiple instances); removed `Collectors` imports.

**webdashboard/security/AuthenticationManager.java**
- `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.

---

## 🧹 Code Quality Pass — Build #154 — 2026-05-20

**IgnoreManager.java**
- Removed always-false `IGNORE_FILE == null` check (static final field initialised at class-load, never null). `||` branch was dead code — IDE reported "Condition 'IGNORE_FILE == null' is always 'false'".
- Fixed: `File.mkdirs()` result silently ignored in `save()`. Now logs a WARN if parent directory creation fails.
- Added `@SuppressWarnings("unused")` to `getIgnoreList()` (intentional public API).
- Renamed `cleanupPlayer(ServerPlayer player)` unused-param to `ignoredPlayer` (body is intentionally empty by design).

**MuteManager.java**
- Removed always-false `MUTE_FILE == null` check — same pattern as IgnoreManager.
- Fixed: `File.mkdirs()` result silently ignored in `save()`.
- Removed dead `mute(ServerPlayer sender, String targetName)` and `unmute(ServerPlayer sender, String targetName)` overloads — `sender` was accepted but never read; callers now use `mute(String)` / `unmute(String)` directly.
- Added `@SuppressWarnings("unused")` to `getMuteExpiry()` (intentional public API).

**MuteCommand.java / UnmuteCommand.java**
- Updated callers to drop the unused `sender` argument and call `MuteManager.mute(targetName)` / `MuteManager.unmute(targetName)` directly.

**MessageUtil.java**
- Replaced `e.printStackTrace()` with `LOGGER.error(...)` in `loadCustomLanguageFile()` — stack traces should always go through SLF4J.
- Merged identical `FileNotFoundException` and `Exception` catch branches into a single `Exception` catch.
- Fixed: `File.delete()` result ignored in `deleteDirectoryRecursively()` — now logs a WARN on failure.
- Removed unused `import java.io.FileNotFoundException` (no longer referenced after catch merge).
- Removed dead private `getLanguageVersion(Map)` method (never called).
- Removed deprecated dead private `escapeNamedPlaceholders(String)` method (`@Deprecated`, never called).
- Added `@SuppressWarnings("unused")` to intentional API-surface public methods: `getDebugInfo`, `clickableSuggestion`, `balanceComponent`, `playerComponent`, `permissionComponent`, `progressBar`, `loadAllCustomLanguages`.

**PlayerChatFormatManager.java**
- Fixed: `File.mkdirs()` result ignored in `save()` — now logs WARN on failure.
- Added `@SuppressWarnings("unused")` to `hasFormat()`.

**ShopManager.java**
- Modernised `collect(Collectors.toList())` → `.toList()` (Java 21) in both `removeShopsByOwner()` and `getShopsByOwner()`.
- Removed now-unused `import java.util.stream.Collectors`.
- Added `@SuppressWarnings("unused")` to `removeShopsByOwner()`.

**PlayerJoinQuitHandler.java**
- Fixed: `File.mkdirs()` result ignored — now logs WARN.
- Removed always-true `if (config != null)` dead-code guard (config was already used at line 57 without NPE, so the check at line 103 was redundant).
- Fixed: `player.getServer().getPlayerList()` called without null-checking `getServer()` at both join and quit broadcast paths (lines 152 and 210). While `ServerPlayer.getServer()` is rarely null, IntelliJ marks it `@Nullable`. Now guarded with `var server = player.getServer(); if (server != null) { ... }`.

**LocalizationManager.java**
- Fixed resource leak: `Files.list(langDirectory)` in `loadDashboardTranslations()` used without `try`-with-resources. `DirectoryStream<Path>` is `Closeable` — if `forEach` threw, the stream would never be closed. Wrapped in `try (var dirStream = Files.list(...))`.
- Added `@SuppressWarnings("unused")` to `translate(key, language, args)` and `getAllTranslations()`.
- Added `isLanguageUnsupported(String)` convenience inverse method so every call site that previously used `!isLanguageSupported(...)` can use the clearer positive form.

**TranslationHandler.java**
- Updated both `!isLanguageSupported(language)` call sites to use the new `isLanguageUnsupported(language)`.

**TaskManager.java**
- `history.add(0, execution)` → `history.addFirst(execution)` (Java 21 `Deque` API).
- `history.remove(history.size() - 1)` → `history.removeLast()`.
- Simplified time-range check: `if (t < start || t > end) return false; ... return true;` → `return t >= start && t <= end;`.

**BanManager.java**
- `server.getProfileCache().get(uuid)` returned an `Optional`; code did `.isPresent()` then `.get()` — refactored to `.orElse(null)` to avoid the IDE NPE warning on `Optional.get()`.
- Removed always-true ternary null checks on `entry.getReason()` and `entry.getSource()` in vanilla-ban import path (both fields are set by `UserBanListEntry` constructor and can't be null at that point).

**Web Dashboard — HTML Accessibility (`for` / `aria-label`)**
- `index.html` — Changed `href="#players"` / `href="#performance"` / `href="#worlds"` / `href="#events"` to `href="#"` (navigation is JS-driven via `data-page`; unresolvable anchors caused IDE warnings); added `aria-label` to `#broadcastInput`.
- `shop.html` — Added `aria-label` to `#filterInput` and `#typeFilter`.
- `permissions.html` — Added `aria-label` to `#userSearchInput`.
- `kits.html` — Added `aria-label` to `#kitSearch`.
- `moderation.html` — Added `aria-label` to `#warnSearch`; added `for` attributes to ban-form labels (`banTarget`, `banName`, `banReason`, `banType`, `banDuration`).
- `users.html` — Added `for` to create-user form labels; added `aria-label` to modal `#roleSelect` and `#pwInput`.
- `cloud.html` — Added `for` to Dropbox and Google Drive config labels.
- `discord.html` — Added `for` to OAuth2 config labels (`cfgDefaultRole`, `cfgClientId`, `cfgClientSecret`, `cfgRedirectUri`).
- `holograms.html` — Added `aria-label` to `#holoSearch`; added `for` to all Create-modal and Edit-modal labels.

---

## ✨ Bug Hunt — Build #147 — 2026-05-19

- **`MuteManager` — Mutes Not Persisted Across Server Restarts → ✅ FIXED**
  All player mutes were stored in an in-memory `ConcurrentHashMap.newKeySet()` with no backing file. Every server restart silently cleared all mutes. Players who had been muted would be able to chat again after any restart.
  - **Root cause**: `MuteManager` had no `load()` / `save()` methods and no data file. The static `mutedPlayers` set existed only in JVM memory.
  - **Additionally**: Timed (`/tempmute`) mutes had no expiry tracking — the set stored only player names, not when the mute should expire. Even while the server was running, there was no way for a timed mute to auto-expire.
  - **Fix**:
    - Rewrote `MuteManager` to store `Map<String, Long>` (lowercase name → expiry timestamp; `0` = permanent).
    - Added `load()` called from a `static {}` block on class init; loads `data/moderation/mutes.json`.
    - Added `save()` called on every `mute()` / `unmute()`.
    - `isMuted()` now checks expiry — auto-removes expired timed mutes and calls `save()`.
    - Added `mute(String targetName, long durationMillis)` overload for timed mutes.
    - Added `getMuteExpiry(String playerName)` helper for UI/command display.
  - Affected files: `MuteManager.java`

- **`IgnoreManager` — Ignore Lists Not Persisted Across Server Restarts → ✅ FIXED**
  Player ignore lists were stored only in memory (`Map<String, Set<String>>`). Every server restart wiped all ignore lists. Players would have to re-run `/ignore` for every person they had previously ignored.
  - **Root Cause 1 — No persistence**: No `load()` / `save()` methods, no backing file. Data lived only in JVM heap.
  - **Root Cause 2 — `cleanupPlayer()` destroyed data on disconnect**: The method removed the disconnecting player's own ignore list (`ignoreMap.remove(playerName)`) AND removed them from all _other_ players' lists (`ignoreMap.values().forEach(... .remove(playerName))`). Even within a single server session, a player's ignore list would be deleted the moment they logged out, meaning it would not apply when they logged back in during the same uptime.
  - **Fix**:
    - Rewrote `IgnoreManager` to back the map with `data/chat/ignore_lists.json`.
    - Added `load()` + `save()` — `save()` called on every `ignore()` / `unignore()`.
    - `cleanupPlayer()` is now a deliberate no-op (ignore lists are permanent preferences, not session state).
  - Affected files: `IgnoreManager.java`

- **`BanManager` — Temporary IP Bans Never Expire + Expiry Not Saved/Loaded → ✅ FIXED**
  Three interlocking bugs caused temporary IP bans (`/banip <ip> <duration>`) to be effectively permanent:
  1. **`isIPBanned()` never checked expiry**: The method called `ipBans.containsKey(ipAddress)` — a pure key existence check with no expiry logic, unlike `isPlayerBanned()` which correctly calls `ban.isExpired()`.
  2. **`saveIPBans()` never wrote `expireTime`**: The `JsonObject` built during save omitted the `expireTime` field entirely. Even if loading had checked it, the value would have been missing from the file.
  3. **`loadIPBans()` never read `expireTime`**: The deserialization loop never read the `expireTime` field, so `ban.expireTime` was always left at the constructor default of `0` (permanent) after a restart.
  - **Fix**:
    - `isIPBanned()` now looks up the `IPBanEntry`, calls `ban.isExpired()`, auto-removes via `ipBans.remove()` + `saveIPBans()` when `autoExpireTempBans` is enabled, and returns `false` for expired bans.
    - `saveIPBans()` now includes `banObj.addProperty("expireTime", ban.expireTime)`.
    - `loadIPBans()` now reads the `expireTime` field (`banObj.has("expireTime") ? ... : 0`) and skips entries that are already expired at load time.
  - Affected files: `BanManager.java`

- **`PlayerJoinQuitHandler` — `newPlayerKit` Blocked by Kit Permissions → ✅ FIXED**
  The "starter kit on first join" feature called `kitManager.giveKit(player, kitName)`, which internally calls `canUseKit()` — a full permission/cooldown/max-uses check. New players who weren't yet in any permission group with the kit node would receive the error `"You don't have permission to use this kit"` instead of their starter items. The config comment said permissions were bypassed, but the code did not bypass them.
  - **Fix**: On first join, the kit items are now given directly by iterating `starterKit.getItems()` and calling `inventory.add()` / `player.drop()`, completely bypassing `canUseKit()`. Logging, cooldown tracking, and usage limits are intentionally skipped for the first-join gift.
  - Affected files: `PlayerJoinQuitHandler.java`

- **`PlayerJoinQuitHandler` — `first_joined.json` Written to Wrong Directory → ✅ FIXED**
  The file tracking which players have already received their starter kit used a raw relative path: `new java.io.File("neoessentials/first_joined.json")`. On dedicated servers where the working directory is not the server root (or differs from the NeoEssentials data directory), this file would be created in the wrong location — causing every join to be treated as a first join and re-giving the starter kit on each login.
  - **Fix**: Changed to `ResourceUtil.getDataFile("first_joined.json")` for a consistent absolute path. Also added a `parent.mkdirs()` guard before the write so the directory is created if it doesn't exist.
  - Affected files: `PlayerJoinQuitHandler.java`

---

- **NeoEssentials AFK Kick Timeout Not Working (NeoForge 1.21.1, build.1.0.2.6+119)**
  Players were never kicked for being AFK even when `kickTimeout` was set to a value greater than 0 in config.

    - Root Causes:
        1. **Hidden `kickAfkPlayers` gate**: `loadConfiguration()` only set `kickAfkPlayers = true` when the JSON key `"kickAfkPlayers": true` was explicitly present in config. The bundled `config.json` never included this key, so `kickAfkPlayers` stayed `false` — silently suppressing all AFK kicks regardless of `kickTimeout`.
        2. **`neoessentials.afk.exempt` permission not enforced**: `checkForAfkPlayers()` never checked the kick-exempt permission before disconnecting a player, making the permission node effectively non-functional.
        3. **Wiki listed wrong permission name**: Wiki said `neoessentials.afk.kickexempt`; the actual registered node is `neoessentials.afk.exempt`.
    - Fix Applied:
        - **`AfkManager.loadConfiguration()`**: Timeout keys (`kickTimeout` / `kickTimeoutMinutes`) are now parsed **before** `kickAfkPlayers` is evaluated. When `"kickAfkPlayers"` is absent from config, it is auto-derived as `true` whenever `kickTimeout > 0`. Setting `"kickAfkPlayers": false` explicitly still force-disables kicking.
        - **`AfkManager.checkForAfkPlayers()`**: Added `PermissionAPI.hasPermission(uuid, "neoessentials.afk.exempt")` check before the kick block — players with this permission are skipped.
        - **`config.json`** (`afk` section): Added `timeout_comment`, `kickTimeout_comment`, and `kickAfkPlayers_comment` inline documentation so server admins know that only `kickTimeout > 0` is needed to activate AFK kicking.
        - **`AFKSystem.md` wiki**: Fixed permission name from `neoessentials.afk.kickexempt` → `neoessentials.afk.exempt` to match `PermissionRegistry`.

- **NeoEssentials Teleportation Safety Checks Ignored Config (NeoForge 1.21.1, build.1.0.2.6+119)**
  Safety checks ran unconditionally even when all safety flags were disabled in config, causing `/back` to fail with *"No safe landing spot found"* for destinations in caves, underground bases, or any location that didn't pass the `isSafe()` check.
    - Root Causes:
        1. **Missing `enableBackSafety` key in bundled `config.json`**: The `backSettings` section had no `enableBackSafety` key, so `ConfigManager.isBackTeleportSafetyEnabled()` always returned the hardcoded default `true`. Server admins had no way to disable back-teleport safety even if they wanted to, because the key wasn't present to configure.
        2. **Hardcoded `findSafe=true` in `/top`, `/jump`, `/jumpto`**: `MiscTeleportManager.teleportToTop()`, `teleportJump()`, and `teleportToLookingAt()` all called `TeleportUtil.teleportPlayer(..., true)` unconditionally. These commands already compute a valid destination themselves (top block scan, open-air scan, player look-at), so re-running `findSafeLocation()` was redundant and could fail on valid spots.
    - Fix Applied:
        - **`config.json`** (`teleportation.backSettings`): Added `"enableBackSafety": true` with descriptive comment so admins can set it to `false` to skip safety enforcement on `/back`.
        - **`MiscTeleportManager.java`**: Changed `teleportToTop()`, `teleportJump()`, and `teleportToLookingAt()` to pass `findSafe=false` to `TeleportUtil.teleportPlayer()`. These methods already guarantee a valid open-air destination, making the redundant safety scan both unnecessary and harmful.
        - `/tpr` (random teleport) intentionally retains `findSafe=true` since random coordinates are not pre-validated.

- **NeoEssentials Economy Manager NullPointerException on Shutdown (NeoForge 1.21.1, build.1.0.2.6+119) → ✅ FIXED**
  Server shutdown threw an NPE deep inside `EconomyManager`, crashing the shutdown sequence with a stack trace.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 119`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.227`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Server log on stop:
          ```
          Failed to shutdown Economy Manager
          java.lang.NullPointerException: Cannot invoke "ConcurrentHashMap.entrySet()" because "this.balancesCache" is null
          ```
        - Only occurred when the economy module was **disabled** in config.
    - **Root Cause**: `balancesCache` was declared as a bare field (`private ConcurrentHashMap<UUID, BigDecimal> balancesCache;`) with no initializer. The `EconomyManager` constructor exits early when `ConfigManager.isEconomyEnabled()` returns `false` — before the line `balancesCache = new ConcurrentHashMap<>()` was ever reached. When `shutdown()` called `saveBalancesAtomic()`, the method immediately dereferenced the null field via `balancesCache.entrySet()`. The same null field would have caused NPEs in `logCacheMetrics()`, `getAllBalances()`, and `getCacheStats()` for the same reason.
    - **Fix**:
        1. **Initialized `balancesCache` at the field declaration** (`= new ConcurrentHashMap<>()`) so it is never `null` regardless of whether the constructor completes fully.
        2. Added an `initialized` boolean flag set to `true` only once the constructor finishes a full initialization (economy enabled, balances loaded, tasks scheduled).
        3. Added `if (!initialized) return;` guards at the top of `saveBalancesAtomic()`, `saveLastActivityAtomic()`, and `logCacheMetrics()` — so when the economy is disabled, these no-ops emit nothing and write no files.
        4. In `shutdown()`, added the same `initialized` check: if the economy was never initialized, the executor is stopped immediately with `shutdownNow()` and a clear log message is emitted instead of proceeding to the (now-redundant) save calls.
    - Affected files: `EconomyManager.java`

---

- **NeoEssentials Duplicate Translation Keys (NeoForge 1.21.1, build.1.0.2.6+119) → ✅ FIXED**
  JAR translations failed to load due to duplicate keys in `en_us.json`, causing `JsonSyntaxException` at startup and leaving all translation keys null.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 119`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.227`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Server log at startup:
          ```
          Failed to load JAR translations: duplicate key: commands.neoessentials.teleport.home.delete_no_pending
          com.google.gson.JsonSyntaxException: duplicate key: commands.neoessentials.teleport.home.delete_no_pending
          ```
        - All translation keys returned `null`; players saw raw key names or garbled text.
        - Version-merge logic never ran, so deployed server lang files were never updated.
    - **Root Cause**: `en_us.json` contained 13 duplicate keys across several sections:
        - `commands.neoessentials.teleport.home.delete_no_pending` (×2)
        - Five `commands.neoessentials.kits.admin.*` keys each defined twice
        - `commands.neoessentials.seen.online` and `seen.offline` (×2 each)
        - `commands.neoessentials.realname.not_found` (×2)
        - `commands.neoessentials.teleport.admin.tpall.no_players` (×2)
        - `commands.neoessentials.general.player_not_found` (×2 — identical)
        - A missing comma between `mutelist.list` and `gamemode.spectator` caused an additional JSON syntax error.
        Gson (used by the mod on NeoForge 21.1.x) throws `JsonSyntaxException` on duplicate keys.
    - **Fix**: Removed all duplicate entries from `en_us.json`, keeping the best version of each; fixed the missing comma. Bumped `_langVersion` from `15` → `16` in `MessageUtil.java` so all deployed server-side lang files are automatically re-merged on next server start.
    - Affected files: `src/main/resources/data/lang/en_us.json`, `MessageUtil.java`

---

- **NeoEssentials Home Delete Message Formatting Failure (NeoForge 1.21.1, build.1.0.2.6+119) → ✅ FIXED**
  Deleting a home triggered a `MessageFormat` exception, displaying a raw error in the log and falling back to the unformatted template string.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 119`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.227`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Log error on `/delhome`:
          ```
          Failed to format message - Key: commands.neoessentials.teleport.home.delete_success,
          Template: 'Home '{HOME}' has been deleted successfully.', Args: [base],
          Error: can't parse argument number: 'HOME'
          ```
        - Player saw unformatted template text; home name was never interpolated.
    - **Root Cause**: Two separate problems interacted:
        1. Some deployed server-side lang files contained legacy `{HOME}`-style named placeholders (e.g. `Home '{HOME}' has been deleted successfully.`) from an older version of the mod before positional `{0}` args were adopted.
        2. The `escapeNamedPlaceholders()` method in `MessageUtil` tried to wrap named tokens in MessageFormat quote spans (`'{'NAME'}'`), but when a token was already surrounded by single-quotes in the template (e.g. `'{HOME}'`), the resulting string `''{'HOME'}''` was mis-parsed by `MessageFormat`, causing `can't parse argument number: 'HOME'`.
        Because the JAR translations **failed to load** (due to the duplicate-key bug above), the version-merge never triggered, so old server files remained in use indefinitely.
    - **Fix**:
        - **Removed `MessageFormat` from the localization pipeline entirely.** `MessageUtil.localize()` now uses a simple custom `applyArgs()` method that:
          - Converts `''` → `'` (backward-compat with existing templates that use MessageFormat-style single-quote escaping),
          - Replaces `{0}`, `{1}`, … with the corresponding positional args via `String.replace()`,
          - Leaves all named tokens (`{HOME}`, `{MESSAGE}`, `{neoessentials_*}`) untouched for later resolution by `PlaceholderAPI`.
        - Added **automatic migration** of legacy `{HOME}`-style keys during the version-bump merge: if a key in the deployed server file still contains an uppercase named placeholder (`{[A-Z][A-Z0-9_]+}`) but the JAR version has a positional `{0}`, the server file value is overwritten with the JAR value.
        - Bumped `CURRENT_LANG_VERSION` to `16` to trigger the merge on all existing deployments.
    - Affected files: `MessageUtil.java`

---

- **NeoEssentials /help Pagination Broken (NeoForge 1.21.1, build.1.0.2.6+69) → ✅ FIXED**
  The `/help` command works for the first page, but `/help 2` (and subsequent pages) does not function at all.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 69`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.227`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - `/help` displays the first page of commands correctly.
        - `/help 2` produces no output or fails to display the second page.
        - Pagination appears to be ignored or broken in command registration.
        - Console reports error of "Unknown command or insufficient permissions".
    - Expected Behavior:
        - `/help <page>` should display the corresponding page of available commands.
        - Should work in console and for players, with correct page counts and navigation.
    - **Root Cause**: Vanilla `/help <command:string>` claimed `"2"` before NeoEssentials' integer `<page>` argument could fire. Additionally, `neoessentials.help` was missing from the `default` group so non-OP players were blocked entirely.
    - **Fix**: Replaced integer `<page>` branch with a single `<page_or_command>` string argument that checks `Integer.parseInt()` first. Added `neoessentials.help` to the `default` group in `permissions.json`.
    - Affected files: `HelpCommand.java`, `permissions.json`

---

- **NeoEssentials Registry Key Error for Shop NPC (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ FIXED**
  Client disconnects when server sends registries containing unknown keys related to NeoEssentials shop NPCs.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 21`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.222`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Client disconnects with warning:
          ```
          Client disconnected with reason: The server send registries with unknown keys: ResourceKey[minecraft:entity_type / neoessentials:shop_npc]
          ```
        - Occurs when server attempts to sync registry data for NeoEssentials custom entity type `shop_npc`.
        - Client does not recognize the registry key, leading to forced disconnect.
    - Expected Behavior:
        - Client should recognize and handle NeoEssentials custom entity types without disconnecting.
    - **Root Cause**: NeoForge 21.1.x mandatorily synchronises every `DeferredRegister<EntityType<?>>` entry to clients during the login handshake. The custom `neoessentials:shop_npc` type was registered server-side only, so every vanilla client disconnected on join with the unknown-key error.
    - **Fix**: Removed the custom `EntityType` entirely. Shop NPCs are now plain vanilla `ArmorStand` entities tagged with the NBT key `NeoEssentials_ShopId` (UUID value stored as two longs). Right-click interaction is intercepted by `ShopEntityRegistry` via `PlayerInteractEvent.EntityInteract` on the GAME event bus — no custom entity type registration required, no registry sync issue possible.
    - Affected files: `ShopEntityRegistry.java`, `ShopNpcEntity.java`, `ShopEntityManager.java`

---

- **NeoEssentials Permission Validation Ignores External Mod Permissions (NeoForge 1.21.1, builds 81–97) → ✅ FIXED**
  Permission validation fails to recognize permission nodes from other mods (e.g., WorldEdit), and some NeoEssentials nodes are flagged as unknown.
    - Environment:
        - NeoEssentials Versions: `1.0.2.6 build 81` (last working), `1.0.2.6 build 87`, `1.0.2.6 build 97` (errors observed)
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.227`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Permission validator logs warnings such as:
          ```
          ✗ Group 'moderateur': Unknown permission 'worldedit.selection.pos'
          ✗ Group 'moderateur': Unknown permission 'neoessentials.chat.msgtoggle.bypass'
          ✗ Group 'architecte': Unknown permission 'worldedit.selection.pos'
          ⚠ PERMISSION VALIDATION FOUND 3 ISSUES!
          ⚠ Some permissions may not work correctly!
          ```
        - Other mods' permissions (e.g., WorldEdit) are not recognized.
        - NeoEssentials-specific nodes (`neoessentials.chat.msgtoggle.bypass`) also flagged as unknown.
        - Builds 87 and 97 show errors, while build 81 still works correctly.
    - Expected Behavior:
        - NeoEssentials should respect and validate external mod permissions (WorldEdit, LuckPerms, etc.).
        - NeoEssentials permission nodes should be properly registered and recognized.
    - **Root Cause 1**: `PermissionValidator` only checked nodes against the internal NeoEssentials registry. Any permission node whose namespace did not begin with `neoessentials.` was treated as unknown, generating spurious warnings for WorldEdit, LuckPerms, etc.
    - **Root Cause 2**: `neoessentials.chat.msgtoggle.bypass` was not registered in `PermissionRegistry.registerAllPermissions()`.
    - **Fix 1 (`PermissionValidator.java`)**: Validator now skips the "unknown" warning for any node whose namespace does not match `neoessentials` — external-mod nodes are silently accepted as valid. Warnings are only emitted for `neoessentials.*` nodes genuinely absent from the registry.
    - **Fix 2 (`PermissionRegistry.java`)**: Registered `neoessentials.chat.msgtoggle.bypass` and all other missing nodes surfaced during audit in `registerAllPermissions()`.
    - Affected files: `PermissionValidator.java`, `PermissionRegistry.java`

---

- **NeoEssentials Default Permissions Not Applied with LuckPerms (NeoForge 1.21.1, build.1.0.2.6+69) → ✅ FIXED**
  Default permissions documented for NeoEssentials are not being granted to users in the LuckPerms default group.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 69`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.227`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Users in the LuckPerms default group do not receive the ✅ default permissions listed in NeoEssentials documentation.
        - Removing **FTB Essentials** restored MiniMOTD functionality, but highlighted that NeoEssentials and FTB Essentials were both trying to register home aliases, resulting in neither working.
        - Conflicts between NeoEssentials and FTB Essentials cause overlapping command registration and permission handling.
    - Expected Behavior:
        - NeoEssentials should correctly apply its documented default permissions to the LuckPerms default group.
        - Home aliases should not conflict when multiple mods are present.
    - **Root Cause 1 — `externalAvailable` guard blocked registry defaults when LuckPerms was unhealthy**:
      `PermissionAPI.hasPermission()` guarded the registry-default fallback with `if (externalAvailable)`. When `LuckPermsAdapter` accumulated ≥ 5 consecutive failures (e.g. during startup before user data was cached), `isHealthy()` returned `false`, `externalAvailable = false`, and the registry-default block was never reached. Non-OP players lost all NeoEssentials default permissions without any visible error.
    - **Root Cause 2 — `queryTristate` called twice per check, doubling failure count**:
      `hasPermission()` called `queryTristate` once, and if it returned anything other than `TRUE`, `checkRegistryDefault()` called `isExplicitlyDenied()` which called `queryTristate` a **second time** for the same node. Every failed load incremented `consecutiveFailures` **twice**, causing the adapter to flip to "unhealthy" in half as many checks — directly triggering Root Cause 1.
    - **Root Cause 3 — Home command aliases conflicted with FTB Essentials**:
      Both NeoEssentials and FTB Essentials registered `/home`, `/sethome`, `/delhome`, and `/homes`, causing Brigadier node-merge conflicts. Neither mod's `requires()` predicate applied cleanly, so `/home` tab-completed but failed silently for players who lacked the conflicting mod's permission node.
    - **Fix 1 (`PermissionAPI.java`)**: Removed the `if (externalAvailable)` guard from the registry-default block. Registry defaults are now always evaluated as a last resort before vanilla-OP fallback. When the adapter is healthy the cached `explicitDeny` flag is used (no extra API call). When the adapter is unhealthy `explicitDeny == null`, treated conservatively as "not denied" — NeoEssentials defaults still apply even when LuckPerms is temporarily unreachable.
    - **Fix 2 (`PermissionAPI.java`)**: Eliminated the double `queryTristate` call. After `hasPermission()` returns `false`, the code calls `isExplicitlyDenied()` once and caches the result in `Boolean explicitDeny`. New helper `checkRegistryDefaultNoAdapterCall()` reads that cached value instead of calling back into the adapter, halving LuckPerms API calls per check and preventing premature failure-counter growth.
    - **Fix 3 (`HomeCommands.java`)**: Added `CONFLICTING_HOME_MODS` detection (`ftbessentials`, `ftb_essentials`, `essentials`). Short aliases (`/h`, `/createhome`) are suppressed when a conflicting mod is present. A clear startup warning is logged. The `isCommandRegistered()` guard prevents duplicate registration.

---

- **NeoEssentials Chat Config File Misread (NeoForge 1.21.1, build.1.0.2.6+69) → ✅ FIXED in build.107**
  Chat configuration failed to load unless the file was symlinked or renamed.
  - **Root cause:** `getConfig("chat")` tried to open a file literally named `chat` (no `.json`) in old code. After the section-extraction guard was added, a stale MAIN_CONFIG cache (populated before split configs were activated) could still leave the `"chat"` section missing, returning an empty object.
  - **Fix 1 (`ConfigManager.java`):** `getConfig(sectionName)` now falls back to reading `sectionName.json` directly from disk and unwrapping the nested section if the merged MAIN_CONFIG doesn't contain the key.
  - **Fix 2 (`ConfigSplitter.java`):** `migrateToSplitConfigs()` now calls `ConfigManager.getInstance().clearCache()` immediately after creating split files — the stale entry is evicted without requiring a manual `/neoe reload`.

- **Gson HTML Escaping Corrupts Chat Format Strings → ✅ FIXED in build.109**
  Gson's default HTML-escaping converted `<`, `>`, `&` in saved JSON to `\u003c`, `\u003e`, `\u0026`, corrupting chat format strings like `<{prefix} {name}> {MESSAGE}`.
  - **Fix:** `.disableHtmlEscaping()` added to every `GsonBuilder` instance that writes JSON files (30+ files across config, chat, moderation, scheduler, web-dashboard, i18n, and more).

## ✨ Build #86 — 2026-04-27 — `/nick` System Non-Functional + Shop Entity Compile Errors

- **`/nick` sets nickname but tab list and chat still show real username → ✅ FIXED in build.86**
  Player reported: "I only get 'Nickname set successfully' but when I open chat or press tab I still have my original name. Others still see my original nickname."
  | `/warp` (no args) | Now shows paginated warp list (page 1). Matches Essentials `args.length==0` behaviour. |
  | Per-warp permission | `isPerWarpPermissionEnabled()` added to ConfigManager. When `true`, `/warp <name>` checks `neoessentials.warps.<name>`. |
  | `perWarpPermission` config | Added `perWarpPermission: false` default to `warpSettings` in `config.json`. |
  | `/warps [page]` pagination | 20 per page, sorted case-insensitively. Shows `(N total, page X/Y)` header when multi-page. Filters by per-warp perms. |
  | `/delwarp` permission | Now correctly uses `PERMISSION_DELWARP` (`warp.delete`) not create perm. |
  | Console `/delwarp` | `deleteWarpByAdmin(String, String)` — new method in `WarpManager`. No `ServerPlayer` needed. |
  | `/warps` console NPE | `executeWarps` uses `source.getPlayer()` (nullable) not unchecked cast. |
  | 26 warp lang keys | All `commands.neoessentials.teleport.warp.*` keys added to `en_us.json`. Previously showed raw keys. |
  | Permission nodes | Added: `warp.others`, `warps.*`. Updated docs for `warp.list`. |
  | PermissionSystem.md | Warp section fully updated with all nodes, per-warp info, and correct command associations. |

- **Economy system — Missing Essentials features: /eco reset, percent amounts, offline pay, baltop async cache, pagination, total wealth, exempt players**

  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Commandeco.java`, `Commandpay.java`, `BalanceTopImpl.java`):**

  Five root causes identified and fixed:
  **Root Cause 1 — Wrong API: `player.setCustomName()` has no effect on tab list or chat:**
  `NickCommand.updatePlayerDisplayName()` called `player.setCustomName(Component)` — the entity cosmetic API designed for mob name tags. On `ServerPlayer` instances this adds a *second* floating label above the player's standard name tag; it does not touch the tab list, chat format pipelines, or any placeholder resolution. The actual tab list display name in Minecraft 1.21.1 is controlled by `ClientboundPlayerInfoUpdatePacket(UPDATE_DISPLAY_NAME)`.
  **Fix:** `updatePlayerDisplayName()` completely rewritten. Now builds a `ClientboundPlayerInfoUpdatePacket.Entry` with the formatted nickname as `displayName` and broadcasts it to every connected player using the same reflection-based packet construction already used by `FakePlayerManager`. When the nick is cleared, `displayName = null` reverts the entry to the game-profile name.
  Affected file: `NickCommand.java`
  - **`/baltop` exempt permission missing** — No `baltop.exempt` node; admins/NPCs could appear on the list.
  - **`/baltop` raw UUIDs in output** — `EconomyLeaderboard.formatLeaderboard()` used `entry.getKey()` (UUID string) not a resolved player name.
  - **3 new permission nodes missing** — `pay.offline`, `baltop.exempt`, `eco.eco` (reset alias) unregistered.

  **Fixes applied:**

  | Area | Change |
  |---|---|
  **Root Cause 2 — `{neoessentials_displayname}` placeholder ignored NickCommand:**
  **Root Cause 3 — Hover/click name injection bypassed nickname:**
  **Root Cause 4 — `TablistManager.getDisplayName()` checked its own unpopulated map:**
  **Root Cause 5 — Nickname not re-applied on relog:**
  No packet was sent when a player joined the server, so the stored nickname was invisible until the next `/nick` execution.
  **Fix:** `NickCommand.onPlayerJoin(ServerPlayer)` public method added, called from `TablistEventHandler.onPlayerJoin()` after the tablist setup. Sends the display-name packet immediately on login.
  Affected files: `NickCommand.java`, `TablistEventHandler.java`
  | Player name resolution | Profile cache lookup, falls back to UUID string if unresolvable. |
  | Cache invalidation | `BaltopCommand.invalidateCache()` called after every `eco give/take/set/reset` and `pay` to keep data fresh. |
  | Permission nodes | Added: `pay.offline`, `baltop.exempt`, `eco` (eco admin). Updated `pay` description. |
  | Lang keys | `eco.reset`, `eco.reset_notify`, `eco.received_give`, `eco.set_notify`, `eco.player_not_found`, `pay.offline_not_allowed`, `pay.player_not_found`, `baltop.empty`, `baltop.refreshing`, `baltop.total`. Updated header + entry formatting with §colours. |

- **Jail system — Missing Essentials features: timed jails, deljail, full event enforcement (respawn, teleport, interact, attack, gamemode)**
  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Jails.java` / `JailListener`):**

  - **Timed jails missing** — `JailEntry` had no `expireAt` field. No way to jail someone for "30 minutes" and have them auto-release. Essentials has `checkJailTimeout(currentTime)` called on join and periodically.
  - **`/jailfor` missing** — No timed-jail command. Essentials: `Commandtogglejail` uses `DateUtil.parseDateDiff`.
  - **`/deljail` missing** — No command to remove a jail location. Essentials: `Commanddeljail`.
  - **Interaction not blocked for jailed players** — `onPlayerRightClick` only checked freeze/vanish, never jail. Essentials: `onJailPlayerInteract` cancels `PlayerInteractEvent` unless `essentials.jail.allow-interact`.
---
- **Shop entity layer — 11 compile errors blocked every build → ✅ FIXED in build.86**
  | `onPlayerRespawn` | Schedules 1-tick delayed teleport back to jail after respawn. |
  | `onPlayerTeleport` | Cancels `TeleportCommandEvent` for jailed players, redirects back to jail. |
  | `onPlayerMove` (dimension change) | Catches cross-dimension escapes via `PlayerChangedDimensionEvent`. |
  | `onPlayerRightClick` + `onPlayerRightClickBlock` | Cancels both for jailed players unless `neoessentials.jail.allow-interact`. |
  | `onLivingAttack` | Cancels attacks by jailed players unless `neoessentials.jail.allow-attack`. |
  | `onBlockBreak` / `onBlockPlace` | Now checks `allow-break` / `allow-place` bypass perms before cancelling. |
  | `onServerTick` | Replaced all-player per-tick scan → runs every 20 ticks (1s), skips non-jailed players, also calls `checkJailTimeout`. |
  | Permission nodes | Added: `jail.timed`, `deljail`, `jail.allow-break`, `jail.allow-place`, `jail.allow-interact`, `jail.allow-attack`. |
  | Lang keys | Added: `jail.message`, `jail.escape_prevented`, `jail.released_expired`, `jail.invalid_duration`, `jail.deljail_success`, `jail.deljail_had_inmates`. |

- **Mail system — Missing Essentials features: timed mail, sendall, clearall, mute/ignore checks, rate limiting, console support**

  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Commandmail.java` / `MailServiceImpl.java`):**

  - **`sendtemp` missing** — No way to send expiring/timed mail. Essentials supports `sendtemp <player> <duration> <message>` where the mail auto-deletes when expired and shows an expiry timestamp.
  - **`sendall` / `sendtempall` missing** — Admins had no way to broadcast a mail to all players.
  - **`clearall` missing** — No admin command to wipe every player's mailbox.
  - **`clear <index>` and `clear <player>` missing** — Players couldn't delete a specific message by position; admins couldn't clear another player's mailbox. Only own full-clear existed.
  | Error | File | Fix |
  |---|---|---|
  | `clicked()` return type `ItemStack` incompatible with `void` (MC 1.21.1) | `NpcShopMenu.java` | Changed return type to `void`; removed `ItemStack` return values |
---
## ✨ Build #78 — 2026-04-27 — /back History Chain Corruption Fix
  | Mute check | Muted players blocked from sending. Returns `§cYou are muted and cannot send mail.` |
  | Ignore check | If target ignores sender and both are online, mail is silently dropped (Essentials behaviour). |
  | Rate limiting | Configurable `mail.mailsPerMinute` in `config.json` (default 10). Atomic per-minute window. |
  | Console support | `/mail send <player> <msg>` works from server console (sender shown as "Console"). |
  | `senderUUID` field | Now stored alongside `senderName` in `mail_data.json`. |
  | Message length | Raised from 200 → 1000 characters (matches Essentials). |
  | Expired mail cleanup | `readMail()` removes expired messages before rendering, same as Essentials `iterator.remove()`. |
  | Login notification | `MailCommand.notifyOnLogin()` hooked into `PlayerJoinQuitHandler.onPlayerJoin()`. |
  | Backward compatibility | Old `mail_data.json` format (with `sender`/`timestamp` fields) loads correctly alongside new format. |
  | Permission nodes | Added: `mail.sendtemp`, `mail.sendall`, `mail.sendtempall`, `mail.clear.others`, `mail.clearall`. All registered in `PermissionRegistry`. |
  | Lang keys | 8 new keys added; all existing mail keys updated with better formatting. |
  | Pages | Increased from 5 per page → 9 per page (matches Essentials). |

- **NeoEssentials Proxy Integration with BungeeTabListPlus (Independent Mode) → ✅ Implemented in build.74–77**
  Full BTLP-inspired tablist rework:
  - `TablistManager.java` — complete rewrite; 20+ placeholder tokens including proxy/session/stats tokens; per-player + per-group header/footer frame overrides; AFK indicator; group-colour overrides; session tracking; vanish filtering; delegates to sub-systems.
  - `TablistLayout.java` — new; BTLP-style layout/sorting: 1–4 columns, `sortByGroupWeight`, `groupSections`, `playersByServer`, `excludeServers`, `hiddenServers`, `maxSlotsPerColumn`.
  - `FakePlayerManager.java` — new; BTLP `fakePlayers` concept; stable UUIDs via `UUID.nameUUIDFromBytes`; reflection-based packet injection; per-viewer injection tracking to avoid duplicate ADD packets.
  - `ProxyIntegration.java` — new; BungeeCord plugin-messaging bridge; `GetServers` / `PlayerCount` / `GetServer` sub-channel handling; `{network_online}`, `{server_online:NAME}`, `{current_server}` placeholders; per-player server tracking; independent of tablist rendering.
  - `TablistCommand.java` — extended with BTLP sub-commands: `proxy`, `fakeplayer`, `layout`, `independent`.
  - `TablistEventHandler.java` — added join/quit lifecycle hooks; session start time tracking.
  - `tablist.json` — `_configVersion` 2→3; added `independentMode`, `proxy`, `fakePlayers`, `layout` sections with full documentation comments.

  *(Fixed: 2026-03-01)*

  **Root causes found:**

- **`/back` acting weird after using warps/tps/back multiple times → ✅ FIXED in build.78**
  After a server restart `/back` worked correctly, but degraded after multiple teleport operations (warps, /tp, /tpa accepts, /back chains). Three root causes were identified and fixed:

  **Root Cause 1 — Wrong player's back location saved on `/tpaccept` (primary bug):**
  `TeleportRequestCommands.executeTpAccept()` called `MiscTeleportManager.saveBackLocation(teleportedPlayer)` where `teleportedPlayer` is the **acceptor** (the player who runs `/tpaccept`). For a `/tpa` request, the acceptor is NOT the one being teleported — the requester is. This caused the acceptor's back location to be silently overwritten with their current (unchanged) position every time they accepted someone's `/tpa`. Consequently, running `/back` after accepting a `/tpa` would either teleport the acceptor to their own current location (no-op) or to a stale position, not their intended prior destination. `TeleportRequestManager.executeTeleportRequest()` already correctly saves the back location for the actual teleporter, so the Commands-level save was both **wrong** (for `/tpa`) and **redundant** (for `/tpahere`).
  **Fix**: Removed `saveBackLocation(teleportedPlayer)` from `TeleportRequestCommands.executeTpAccept()` entirely. The Manager is the sole authoritative back-location saver for TPA/TPAHERE teleports.
  Affected file: `TeleportRequestCommands.java`
  | **Dashboard** | `admin.dashboard`, `dashboard.access`, `dashboard.view`, `dashboard.manage`, `dashboard.moderator`, `dashboard.admin` |
  | **Vanish alias** | `vanish.see` |

  **Structural fixes:**
  - Added `MODERATION` to `PermissionCategory` enum — moderation commands now appear in their own category in `/permissions list`, exports, and the dashboard
  - Updated `PermissionRegistry.categorizePermission()` and `PermissionBridge.categorizePermission()` to return `MODERATION` for ban/kick/freeze/jail/vanish prefixes
  - Updated `PermissionBridge.categorizePermission()` — previously returned `MISC` for `moderation`, `mod`, `mute`, `ban`; now returns `MODERATION`

  **Permission suggestion fix:**
  - `PermissionValidator.validatePermission()` — denial message now reads:
    `"You don't have permission to use this command.§7Required: §f<node>"`
  - `PermissionValidator.validateAnyPermission()` — shows all accepted nodes:
    `"You don't have permission. §7Required (any): §f<node1>§7 or §f<node2>"`
  - `PermissionValidator.validateTargetPermission()` — same treatment

- **SocialSpy broadcast missing translation key `neoessentials.socialspy.format` → ✅ FIXED in build.70**
  `SocialSpyManager.broadcast()` called `MessageUtil.component("neoessentials.socialspy.format", ...)` but the key was absent from `en_us.json`, causing the spy message to display a raw humanized fallback string.
    - Fix Applied (build.70): Added `"neoessentials.socialspy.format": "&8[&eSocialSpy&8] &b{0} &7→ &b{1}&7: &f{2}"` to `en_us.json`.  Arguments `{0}` = sender name, `{1}` = receiver name, `{2}` = message text.
    - `_langVersion` bumped `13 → 14`; `CURRENT_LANG_VERSION` constant in `MessageUtil` updated to match — existing deployments will auto-merge the new key on next server start.
    - Affected files: `en_us.json`, `MessageUtil.java`

  **Root Cause 2 — Race condition: warmup-period concurrent teleport overwrites undo-back timestamp:**
## ✨ Build #77 — 2026-04-27 — BungeeTabListPlus-Inspired Tablist Rework
- **Tablist duplicate class definition compile error → ✅ FIXED in build.77**
  `TablistCommand.java` contained two complete `class TablistCommand { ... }` definitions — the new BTLP-style class (lines 1–471) followed immediately by the old handler class (lines 473–727). This caused a compile-time "class already defined in package" error. **Fix**: Removed the duplicate old block; retained only the full BTLP-style implementation.
  Affected file: `TablistCommand.java`

  - **`CustomLanguageManager.initialize()` only deployed `en_us.json`** — when the server started it copied only `en_us.json` from the JAR to disk. No other bundled lang files were ever extracted, so even if they existed in the JAR they would never reach the `languages/custom/` directory where the system reads from.

  **Fixes applied:**

  | Fix | Detail |
  |---|---|
  | Fixed all broken colour codes | All TPR/misc teleport keys in `en_us.json` corrected (`e` → `§e`, `a` → `§a`, `c` → `§c`). Lang version bumped 102 → 103 |
  | Added `fr_fr.json` | French (France) — full coverage of all major command categories |
  | Added `de_de.json` | German (Germany) — full coverage |
  | Added `es_es.json` | Spanish (Spain) — full coverage |
  | Added `pt_br.json` | Portuguese (Brazil) — full coverage |
  | Added `zh_cn.json` | Chinese (Simplified) — full coverage |
  | Added `nl_nl.json` | Dutch (Netherlands) — full coverage |
  | Added `pl_pl.json` | Polish (Poland) — full coverage |
  | Added `ru_ru.json` | Russian (Russia) — full coverage |
- **`ProxyIntegration` — `@Override write(FriendlyByteBuf)` method does not override supertype → ✅ FIXED in build.77**
## ✨ Build #73 — 2026-04-27 — Messaging & SocialSpy Improvements
  Affected file: `ProxyIntegration.java`
    **Fix:** Raised `REPETITIVE_ACTION_THRESHOLD` from 10 → 30, raised `SUSPICIOUS_SCORE_THRESHOLD` from 100 → 300, fixed score decay to compare against `lastActionTime` for the relevant action type, and reset per-type count when the 60-second window expires.

  - **Root cause 3 — `AfkMovementDetector` was missing `@EventBusSubscriber`:**
- **Fallback formatting if template parsing fails → ✅ Implemented in build.73**
  `resolveTemplate()` never throws. If PlaceholderAPI fails, the partially-resolved template is returned safely. `MessageUtil.localize()` already had a catch block; `resolveTemplate()` extends that safety to the PlaceholderAPI stage.
- **Debug logging for missing/misparsed placeholders → ✅ Implemented in build.73**
  When `logging.enableDebugLogging = true`, any `{TOKEN}` tokens still present in a template after full resolution are logged as `WARN` with the original template and the list of unresolved tokens. SocialSpy adds format-resolution trace logs (which source selected, and the pre/post strings).
- **Admin-configurable SocialSpy formatting in config → ✅ Implemented in build.73**
  New `chat.messaging` section in `config.json`:
  ```json
  "socialspyFormat":  "",   // override neoessentials.socialspy.format lang key
  "msgFormatTo":      "",   // override commands.neoessentials.msg.format.to
## ✨ Build #72 — 2026-04-27 — FTB Ranks Adapter API Correction
  "replyFormatTo":    "",   // override commands.neoessentials.reply.format.to
  "replyFormatFrom":  ""    // override commands.neoessentials.reply.format.from
  ```
  Leave blank to use lang-file defaults. Config always takes priority when non-empty.
## ✨ Build #70 — 2026-04-27 — `/msg` & SocialSpy Formatting Fix
    - `MsgCommand` and `ReplyCommand` migrated to use `resolveTemplate()`.
    **Fix:** Added `@EventBusSubscriber(modid = "neoessentials")` annotation to the class.

  - **Root cause 4 — AFK broadcasts silently failed (`MessageUtil.info()` used as raw string):**
    `onPlayerGoAfk()` and `onPlayerReturnFromAfk()` called `MessageUtil.info(message)` where `message` was a plain string like `"Steve is now AFK"`. `MessageUtil.info()` treats its argument as a **translation key**, looks it up in the lang file, finds nothing, and returns the key unchanged — without colour or formatting. The broadcasts were also not logged to the server console.
    **Fix:** Replaced with `Component.literal("§e" + message)` directly. Added `server.sendSystemMessage()` call so broadcasts also appear in the server console.

  - **Root cause 5 — `/afk` command gave no feedback to the player:**
    `toggleAfk()` broadcasts a message to all players, but the player who typed `/afk` received no direct personal confirmation that the command worked — especially confusing since the broadcast message may not be visible to the player themselves if it's formatted differently.
    **Fix:** After calling `toggleAfk()`, the command now sends a direct `§eYou are now AFK.` / `§eYou are no longer AFK.` message to the executing player. Auto-AFK (inactivity timeout) also sends a personal notification: `§eYou are now AFK due to inactivity.`

- **NeoEssentials Chat Logging — chat messages not shown in server console (NeoForge 1.21.1, All The Mons)**
  *(Fixed: 2026-03-01)*
- **`/msg` & `/reply` format templates broken by `MessageFormat` named-placeholder collision → ✅ FIXED in build.70**
---
    Template: '&7[&aTo &f{neoessentials_displayname}&7] &f{MESSAGE}',
    Args: [], Error: can't parse argument number: neoessentials_displayname
---
    - Fix Applied (build.70):
- **Tablist player-row prefix/suffix not rendering hex/gradient colors → ✅ FIXED in build.69**
---
    - Root Cause: `updatePlayerTeam()` called `Component.literal(prefix)` / `Component.literal(suffix)` and had no rich-text conversion step.
- **Color codes inside placeholders corrupted after substitution → ✅ FIXED in build.69**
  `applyPlaceholders()` was internally converting `&` → `§` *before* returning the frame text. This caused `&#RRGGBB` hex tokens to become `§#RRGGBB` (invalid) and `<gradient:…>` tags to pass through unchanged to the `processTablistText()` pipeline where `&`-codes had already been consumed.
---
    - Affected file: `TablistManager.java` — `applyPlaceholders()`
    - Affected file: `TablistManager.java` — `updatePlayerTeam()`
- **`RichTextFormatter` lacked a tablist-safe text processor → ✅ ADDED in build.69**
  The existing `processRichText()` method could emit hover/click event markers (used in chat) that are silently dropped by `ClientboundTabListPacket`, causing malformed output.
    - Fix Applied (build.69): Added `RichTextFormatter.processTablistText(String)` — runs the full gradient → rainbow → named-color → format-tag → `<color:#RRGGBB>` pipeline, strips any hover/click markers, then calls `ChatComponentUtil.parseColorCodes()`. Enabled unconditionally (does not depend on the `enableChatEnhancements` server flag).
    - Affected file: `RichTextFormatter.java`
  - Config version bumped to 20.

  6. **Extended placeholder set**
     Added `{displayname}`, `{server_name}`, `{x}`, `{y}`, `{z}`, `{balance}`, `{time}`, `{bar}` alongside the existing 12 placeholders. Per-group `groupColors` map applies a color prefix to `{displayname}`.
- **NeoEssentials Teleportation — chunk not loaded causes "No safe teleport location found" even with safety disabled (NeoForge 1.21.1, All The Mons)**
  *(Fixed: 2026-03-01)*
---
  - **Root cause 2 — `isSafe()` never checked dangerous blocks:** Lava, fire, cactus, nether portal, magma, etc. were all considered "safe" as long as feet/head space was air.
    **Fix:** Added `isDangerous()` helper in both `TeleportLocation` and `TeleportUtil` covering: lava, water, fire, soul fire, magma, cactus, sweet berry bush, wither rose, nether portal, campfire, soul campfire, powder snow.
## ✨ Build #67 — 2026-04-24 — Custom Player Tablist (full feature)
    **Fix:** `findSafeLocation()` now first does a full top-down column scan at the same X,Z (finds the surface in one pass), then falls back to the XZ expanding radius. `TeleportUtil.getHighestSafeY()` updated to use the same logic.
- **Custom Player Tablist system implemented → ✅ Build #67**
  **What was built:**
    **Fix:** Both managers now pass `findSafe=false` since safety is fully handled before the `TeleportUtil` call.

  7. **Vanish + AFK integration**
     `hideVanished: true` excludes vanished players from `{online}` for non-staff viewers. `showAfkIndicator: true` appends configurable `afkSuffix` (default `&7[AFK]`) to AFK players in the tab row.
- **`/tpr` (Random Teleport) — basic brute-force with no config, safety, or biome awareness**
  *(Fixed: 2026-03-01)*
  - Old implementation was 50 blind random attempts with no safety checks, no cooldown, no world border awareness, no biome exclusions, no cache, no nether support.
  - **Fix:** Full port of EssentialsX's `RandomTeleport` system as `RandomTeleportManager.java`:
  1. **Hex colors & gradients in header/footer**
     `TablistManager.updatePlayer()` now builds header and footer through `RichTextFormatter` (build.69 refined this further with the dedicated `processTablistText()` method). Supports `&#RRGGBB`, `<gradient:FF0000-0000FF>text</gradient>`, `<rainbow>text</rainbow>`, named color tags (`<red>`, `<gold>`, …), and format tags (`<bold>`, `<italic>`, …).
  - Config: new `randomTeleportSettings` section added to `teleportation` in `config.json` (version bumped to 19).
  - Language keys added for all new messages.

  8. **`tablist.json` config template**
     Bundled default config updated with gradient header example, per-group and per-player sections, `groupColors` map, and inline syntax reference comments.
- **Web Dashboard files not updating when newer versions are available**
  *(Fixed: previous session)*  
  Config version tracking (`_configVersion`) was already in place for config files. Dashboard HTML/JS/CSS files are now versioned and updated from JAR on server start when the bundled version is newer than what is deployed.

  - Affected files: `TablistManager.java`, `TablistCommand.java`, `tablist.json`
  2. **Animated header/footer frames**
     `header` and `footer` in `tablist.json` accept a JSON array. Each refresh tick advances one frame creating smooth text animations. `refreshInterval` (ticks, default 20) controls speed.

##  Build #66 — 2026-04-24
- **Dashboard register command not working**
  *(Fixed: previous session)*  
  `/dashboard register` command was not properly creating accounts. Registration flow fixed — generates token, stores credentials, confirms in-game.

- **Tablist prefix not appearing before username → ✅ FIXED in build.66**
  Group prefix/suffix set in `permissions.json` was not displaying before player names in the tab list. Reported during post-build.64 testing.
    - Root Causes:
        1. `getPermissionPrefix()` / `getPermissionSuffix()` called `PermissionSystem.getManager()` which throws `IllegalStateException` before the permission system is fully initialised; the exception was silently swallowed in the `catch`, returning `""` every time.
        2. All three helpers (`getPermissionPrefix`, `getPermissionSuffix`, `getPermissionGroup`) had inconsistent fallback behaviour — `getPermissionGroup()` returned `"default"` when the user record was absent, but the prefix/suffix helpers returned `""` instead of looking up the default group's values.
    - Fix Applied (build.66):
        - Switched all three helpers to use `PermissionAPI.getManager()` (returns `null` instead of throwing), with an explicit null guard.
        - When the player has no explicit user entry (or `user.getGroup()` is `null`), all three helpers now fall back to `mgr.getDefaultGroup()` before looking up the group's prefix/suffix. The scoreboard team (and thus the tab list prefix row) now reliably shows the correct group prefix for every player, including freshly-joined players whose user entry was auto-created.
    - Affected file: `TablistManager.java` — `getPermissionPrefix()`, `getPermissionSuffix()`, `getPermissionGroup()`
- **Rich text (gradients/rainbow) not working despite being enabled in config**
  3. **Per-group header/footer**
     New `"groups"` section in `tablist.json` — each permission group (e.g. `admin`, `moderator`) can define its own `header`/`footer` arrays. Priority: **per-player → per-group → global**.

- **Warn command not logging to server console → ✅ FIXED in build.66**
  `/warn <player> <reason>` used `source.sendSuccess(..., broadcastToOps=true)` but had no explicit `LOGGER.info()` call — unlike `executeClearWarnings()` and `executeRemoveWarn()` which both had direct logger calls. On some server configurations (particularly when stdin is not a terminal, or the server uses a custom logging appender), `sendSuccess` feedback is not routed to the persistent log file.
    - Observed: Warn records were being saved correctly to `warns.json`, but no timestamped console/log line appeared for `/warn` specifically. Other warn commands (`/clearwarnings`, `/removewarn`) did log correctly.
    - Fix Applied (build.66): Added `LOGGER.info("[Warn] {} warned {} for: {} (warn #{}, ID: {})", warnedBy, playerName, reason, total, shortId)` in `WarnCommand.executeWarn()`, matching the style of the other warn-management commands.
    - Affected file: `WarnCommand.java` — `executeWarn()`
- **PowerTool system — powertools affecting item slots instead of items**
  *(Fixed: previous session)*  
  PowerTool data was keyed on inventory slot index rather than item identity (NBT/item type). When a player moved items around, the powertool followed the slot, not the item. Fixed to key on item identity so the command travels with the item regardless of which slot it occupies.

---

- **WarnManager failed to compile — duplicate `getInstance()` method → ✅ FIXED in build.66**
  `WarnManager.java` contained two identical `public static WarnManager getInstance()` declarations (lines 28 and 44), causing `error: method getInstance() is already defined in class WarnManager` at compile time. The mod JAR could not be built until this was resolved.
    - Fix Applied (build.66): Removed the duplicate declaration at line 44 (line 28 is the canonical definition, adjacent to the `INSTANCE` field).
    - Affected file: `WarnManager.java`

---

##  Build #64 — 2026-04-24

- **`/help [page]` returns "no permission" for regular players → ✅ FIXED in build.64**
  Non-operator players received a "no permission" response when running `/help` or `/help <page>`. The `HelpCommand` guards the command with `PermissionAPI.hasPermission(uuid, "neoessentials.help")`, but this node was absent from the `default` group in `permissions.json`, so all non-op players were blocked.
    - Root Cause: `neoessentials.help` was missing from the `default` group's `permissions` array in both the bundled `src/main/resources/data/config/neoessentials/permissions.json` and the deployed `run/config/neoessentials/permissions.json`.
    - Fix Applied (build.64): Added `"neoessentials.help"` to the `default` group's permission list in `permissions.json`. Help is now accessible to all players by default with no operator status required.
    - Affected file: `permissions.json` — `default` group
  4. **Per-player header/footer overrides**
     - `"players"` UUID map in `tablist.json` for persistent per-player frames.
     - New runtime commands: `/tablist player <name> header <text>`, `/tablist player <name> footer <text>`, `/tablist player <name> reset`.
  - Top-down column scan ported from Essentials surface-finding behaviour

---

- **Localization Audit — 54 missing translation keys + no fallback for unknown keys → ✅ FIXED in build.64**
  *(See full entry further below in this file)*

---
#  Additional Features

##  Configuration Notes (not code bugs)

- **`/kick` and `/ban` returning "no permission" for moderators**
  Reported during post-build.64 testing. Investigation confirmed this is **not a code bug** — the permission nodes `neoessentials.moderation.kick` and `neoessentials.moderation.ban` are correctly present in the `moderator` group in `permissions.json`.
  The cause is that players must be **explicitly assigned** to the `moderator` (or `admin`) group before those permissions apply. New players are auto-created in the `default` group; the `default` group intentionally does not include moderation permissions.
    - **Resolution**: Assign the player to the correct group in-game:
      ```
      /permissions user <playername> setgroup moderator
      ```
      Or promote to admin:
      ```
      /permissions user <playername> setgroup admin
      ```
      Changes take effect immediately without a server restart. Use `/permissions user <playername> info` to verify the current group assignment.

- **Chat color codes / formatting**
  Reported during post-build.64 testing. Confirmed working — `ChatFormatter` correctly processes `&` codes and `§` codes via `ChatComponentUtil.parseColorCodes()`. No code change required.

---

## ✅ Previously Fixed Issues (older builds)

- **NeoEssentials Freeze System Not Working (NeoForge 1.21.1, build.1.0.2.6+52) → ✅ FIXED in build.1.0.2.6+53**
  `/freeze <player>` reports success and the player receives a message, but they can still walk around freely, interact with blocks, and nothing prevents them from moving.
    - Environment:
        - Mod Version: `neoessentials-1.0.2.6+52`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.220`
        - Java Version: `openjdk 21`
        - Dedicated Server
    - Observed Behavior:
        - Frozen player can walk and move around the world freely — no position lock.
        - Frozen player receives the notification message twice on `/freeze`.
        - Frozen player's notification sometimes shows the raw key string `neoessentials.moderation.frozen_message` instead of the actual message.
        - When a frozen player reconnects, they receive no reminder and no position lock is applied.
        - When the Jail system is disabled in config, freeze enforcement also stops working entirely.
    - Root Causes (5 bugs found across `ModerationEventHandler.java`, `FreezeManager.java`, `FreezeCommand.java`):
        1. **`FreezeManager.enforceFreezePosition()` was never called** — the method exists and correctly teleports the player back if they have moved, but it had zero call-sites in the event handler. Frozen players could walk anywhere without restriction.
        2. **`FreezeManager.onPlayerJoin()` was never called on login** — `ModerationEventHandler.onPlayerLogin()` called `VanishManager.onPlayerJoin()` and `JailManager.onPlayerJoin()` but had no equivalent call for `FreezeManager`. Reconnecting frozen players never got the reminder message and their `frozenPosition` was never initialised from their spawn position.
        3. **`onServerTick` returned early on `!isJailSystemEnabled()`** — even if freeze enforcement had been wired in, the early `return` on jail being disabled would have prevented it from running. Freeze enforcement must run independently of the jail system's enabled flag.
        4. **Wrong message key in `FreezeCommand`** — `executeFreeze()` checked `template.equals("commands.neoessentials.moderation.frozen_message")` but `ConfigManager.getFreezeMessage()` returns the default `"neoessentials.moderation.frozen_message"` (no `commands.` prefix). The condition always evaluated to `false` → the `else` branch ran `.replace()` on the raw fallback key → the player saw the literal string `neoessentials.moderation.frozen_message` as their notification. Same bug in `executeUnfreeze()` with `unfrozen_message`.
        5. **Duplicate player notification on `/freeze`** — `FreezeManager.freezePlayer()` sent the frozen message to the player, and `FreezeCommand.executeFreeze()` also sent it → the player received two identical notifications.
    - Fix Applied (build.1.0.2.6+53):
        - **`ModerationEventHandler.onPlayerLogin()`**: Added `FreezeManager.getInstance().onPlayerJoin(player)` call, gated by `isFreezeSystemEnabled()`, matching the pattern already used for vanish and jail.
        - **`ModerationEventHandler.onServerTick()`**: Added a separate freeze-enforcement loop that runs **before** the jail guard. Every online frozen player has `enforceFreezePosition()` called once per second (20-tick cycle). The loop is independently gated by `isFreezeSystemEnabled()` so it works regardless of whether jail is enabled or disabled.
        - **`FreezeManager.freezePlayer()`**: Removed the player notification send. Commands (`executeFreeze`, `executeFreezeAll`) are the sole senders, eliminating the duplicate message.
        - **`FreezeCommand.executeFreeze()`**: Fixed key check from `"commands.neoessentials.moderation.frozen_message"` → `"neoessentials.moderation.frozen_message"` to match `ConfigManager.getFreezeMessage()`'s actual fallback value.
        - **`FreezeCommand.executeUnfreeze()`**: Fixed key check from `"commands.neoessentials.moderation.unfrozen_message"` → `"neoessentials.moderation.unfrozen_message"` to match `ConfigManager.getUnfreezeMessage()`'s actual fallback value.

---

- **NeoEssentials Vanish — Players Remain Visible Despite "You are now vanished" Message (NeoForge 1.21.1, build.1.0.2.6+50) → ✅ FIXED in build.1.0.2.6+52**
  After running `/vanish`, the confirmation message appears in chat but other players can still see the vanished player in the world.
    - Environment:
        - Mod Version: `neoessentials-1.0.2.6+50`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.220`
        - Java Version: `openjdk 21`
        - Dedicated Server (LuckPerms present)
    - Root Causes (4 bugs found in `VanishManager.java`):
        1. **Entity never removed from the world** — `hidePlayerFromOthers()` opened with `if (!isHideFromTabListEnabled()) return;`. It never sent `ClientboundRemoveEntitiesPacket`, so the player's body was always visible regardless of config.
        2. **`showPlayerToSpecific()` was completely empty** — contained only a comment and sent zero packets. Unvanishing therefore did nothing for observers already online.
        3. **Priority check logic was inverted** — `hidePlayerFromOthers()` used `if (viewerPriority > vanishedPriority)`. Both defaults are `10`, so `10 > 10 = false` → nobody was ever hidden.
        4. **Newly joining players could always see vanished players** — `onPlayerJoin()` never hid already-vanished players from the joining player.
    - Fix Applied (build.1.0.2.6+51):
        - **`hidePlayerFromSpecific()`**: Now sends both `ClientboundPlayerInfoRemovePacket` (conditional on tab-list config) **and** `ClientboundRemoveEntitiesPacket` (always).
        - **`showPlayerToSpecific()`**: Fully implemented — sends the complete packet sequence: `ClientboundPlayerInfoUpdatePacket.createPlayerInitializing()`, `ClientboundAddEntityPacket`, `ClientboundSetEntityDataPacket`, `ClientboundSetEquipmentPacket`, `ClientboundRotateHeadPacket`.
        - **`hidePlayerFromOthers()`**: Removed early `return`. Fixed priority check: observer may see vanished only when explicitly in `viewerPriorities` AND priority `<=` vanished player's.
        - **`onPlayerJoin()`**: Deferred by 1 tick so `ClientboundRemoveEntitiesPacket` arrives after vanilla entity-spawn packets. Added missing branch: hides all vanished players from joining player if they lack see-vanished permission.

---

- **NeoEssentials Teleportation Safety Bug (NeoForge 1.21.1, build.1.0.2.5) → ✅ FIXED in build.1.0.2.6+36**
  Teleportation to `/home` fails with *"No safe teleport location found"* even when `enableHomeSafety` is `false`.
    - Root Causes:
        1. **Config flag not respected** — safety was always applied regardless of the setting.
        2. **Unloaded chunk caused false failure** — `findSafeLocation()` scans ±16 blocks in X/Z, crossing unloaded chunk boundaries whose `isSafe()` checks always returned `false`.
    - Fix Applied (build.1.0.2.6+36):
        - `teleportToHome()` now reads `isHomeTeleportSafetyEnabled()` at runtime.
        - `TeleportUtil.preloadChunksForTeleport()` added — force-loads the 3×3 chunk grid unconditionally.
        - Safety block only executed when `requireSafe=true`; skipped entirely when `enableHomeSafety=false`.

---

- **NeoEssentials Web Dashboard Permissions & Admin Control Blank (NeoForge 1.21.1, build.1.0.2.6) → ✅ FIXED in build.1.0.2.6+46**
  The web dashboard shows blank menus for permissions and admin controls after login.
    - Root Causes:
        1. `showLoginScreen()` hid `dashboardWrapper` on sub-pages that have no `loginContainer`.
        2. `permissions.js` init guard never matched — `initPermissionSystem()` was never called.
        3. Nine `fetchWithAuth()` calls missing `.json()` — all modal actions silently failed.
        4. Username not shown on sub-page topbars (`id="userName"` vs `id="usernameDisplay"` mismatch).
    - Fix Applied (build.1.0.2.6+46):
        - `showLoginScreen()` now redirects to `index.html` when called on sub-pages.
        - `permissions.js` init changed to use `document.getElementById('permOverviewTab')`.
        - All 9 `fetchWithAuth` calls fixed to call `.json()`.
        - `showDashboard()` username fallback added.

---

- **NeoEssentials Teleportation Message Bug (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ Fixed in build.1.0.2.6+38**
  Teleportation messages sometimes display raw translation keys instead of localized text.
    - Root Causes: All `commands.neoessentials.teleport.spawn.*` keys were missing from `en_us.json`.
    - Fix Applied: Added all missing spawn/warp/home message keys. Bumped `_langVersion` 10→11.

---

- **NeoEssentials Teleport Cooldowns & Warmups Not Working (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ Fixed in build.1.0.2.6+38**
  Cooldowns and warmups configured for teleportation commands do not function at all.
    - Root Causes:
        1. `HomeManager`: `teleportDelay` hardcoded to `3`; cooldown never checked.
        2. `WarpManager`: `warpCooldown` config present but never read or enforced.
        3. `SpawnManager`: `spawnCooldown` never read or enforced; warmup overridden by `loadSpawn()`.
        4. No warmup countdown messages sent to players.
    - Fix Applied: All three managers now read cooldown/warmup from config, enforce them, and send warmup messages before delayed teleports.

---

- **NeoEssentials Inventory & Ender Chest Commands Not Restricted (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ Fixed in build.1.0.2.6+40**
  Non-OP and non-admin players could use `/inv` and `/ec` commands, leading to duplication exploits.
    - Root Causes:
        1. Brigadier `redirect()` aliases had no `requires()` predicate — everyone could use them.
        2. Typo: `.getChild("enderchestdit")` (missing 'e') caused NPE on `/ecedit`.
        3. Missing permission nodes in `permissions.json` moderator group.
        4. Hardcoded raw message strings instead of translation keys.
    - Fix Applied: Replaced all `redirect()`-based aliases with full registrations including `requires()`. Typo fixed. Permission nodes and translation keys added.

---

- **NeoEssentials Vanish Cannot Be Disabled (NeoForge 1.21.1, builds 1.0.2.5 & 1.0.2.6+21) → ✅ FIXED in build.41**
  Disabling the vanish module in config does not actually disable it.
    - Root Causes:
        1. `isVanishSystemEnabled()` read from wrong config path — always returned `true`.
        2. Interaction guards did not check `isVanishSystemEnabled()`.
        3. `VanishManager.onPlayerJoin()` was never called on login.
    - Fix Applied: Config path fixed. All interaction guards updated. `onPlayerJoin()` wired in `ModerationEventHandler`.

---

- **NeoEssentials Home Confirmation Actions Broken (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ FIXED in build.44**
  Clicking confirm on `/sethome` overwrite or `/delhome` appends "confirm" to the home name repeatedly.
    - Root Cause: `confirm`/`deny` literals were registered as Brigadier children **under** the `<name>` argument. Client dispatched `/sethome Colony confirm`; server received `"Colony confirm"` as the name value.
    - Fix Applied: Moved `confirm`/`deny` to top-level literal siblings of `<name>`. Home name now held server-side and retrieved from pending maps.

---

- **NeoEssentials /back Command Fails in Unloaded Chunks (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ FIXED in build.1.0.2.6+42**
  The `/back` command cannot find last death points or previous locations if they are in unloaded chunks.
    - Root Causes:
        1. `TeleportUtil` only loaded the single target chunk; `findSafeLocation()` scans ±16 blocks crossing into unloaded neighbour chunks.
        2. `MiscTeleportManager.teleportDelay` was hardcoded to `3` — never read from config.
    - Fix Applied:
        - `TeleportUtil`: Added `preloadChunksForTeleport()` loading a 3×3 chunk grid.
        - `ConfigManager`: Added `getBackTeleportDelay()`, `isDeathBackEnabled()`, `isTeleportBackEnabled()`.
        - `MiscTeleportManager`: Added `loadConfig()` reading all back-settings from config.

---

- **Permissions System — GUI, External Systems & Fine-Grained Control not complete**
  *(Status: Fixed → v1.0.2.6+build.30)*

  **Root cause**: Three remaining Permissions System items were unimplemented: GUI Management, External Systems documentation, Fine-Grained Command Control.

  **Fix (build.30)**:
  - `PermissionEndpoint` — 12 new REST methods added (context CRUD, temp CRUD, alias management, system status).
  - `PermissionSystem.md` — 3 new major sections: External Permission Mods, Fine-Grained Command Control, GUI Management Web Dashboard API.

---

- **Permissions System — Contextual permissions, conditions, API, and aliases not implemented**
  *(Status: Fixed → v1.0.2.6+build.28)*

  **Fix (build.28)**:
  - `PermissionContext` value object capturing `worldId`, `dayTime`, `gamemode`.
  - `PermissionUser` / `PermissionGroup` extended with `contextualPermissions` and `conditions` maps.
  - `PermissionManager.hasPermission(UUID, String, PermissionContext)` — context-aware overload.
  - `PermissionConditionManager` — evaluates `time:day`, `gamemode:X`, `world:X`, `health:above/below:N`, `op:true/false` with `AND`/`OR` support.
  - `PermissionAliasManager` — maps legacy/short node names; resolved transparently in `PermissionAPI.hasPermission`.
  - `PermissionsService` interface + `PermissionsServiceImpl` — clean API for external mods via `NeoEssentialsAPI.getPermissionsService()`.
  - `NeoEssentialsAPI.API_VERSION` bumped `1.0.0` → `1.1.0`.

---

- **NeoEssentials Permissions Not Recognising OP / FTB Ranks NoSuchMethodException**
  *(Status: Fixed → v1.0.2.6+build.9)*

  **Root cause**: External mod permissions not routed through `permissions.json`; OP bypass skipped when external adapter registered; FTB Ranks called non-existent `hasPermission(UUID, String)`.

  **Fix**:
  - Created `NeoEssentialsPermissionHandler` implementing NeoForge's `IPermissionHandler` — every Boolean permission-node check from any mod now goes through `permissions.json`.
  - Auto-activates as `neoessentials:handler` when no competing permission mod is present.
  - OP bypass now checked *before* any external adapter.

---

- **NeoEssentials Invalid Wildcard Permission Formats — Startup Warnings**
  *(Status: Fixed → v1.0.2.6+build.8)*

  **Root cause**: `PermissionRegistry.isValidPermission()` regex `^[a-z0-9._-]+$` rejected `*`, causing `neoessentials.spawner.*` etc. to log `WARN Invalid permission format` and be dropped from the registry.

  **Fix**: Regex updated to explicitly handle `.*` suffix. Both `PermissionRegistry` and `PermissionScanner` fixed.

---

- **NeoEssentials Chat Colors — Format String Colors Stripped (All White Output)**
  *(Status: Fixed → v1.0.2.6+build.8)*

  **Root cause**: `ChatFormatter.formatMessage()` called `processRichText()` then `component.getString()` which strips all formatting codes, returning plain white text to the enhancement pipeline.

  **Fix**:
  - Added `RichTextFormatter.preprocessTags(String)` — converts gradient/rainbow tags to `&#RRGGBB` hex codes as plain strings.
  - `ChatFormatter` now calls `preprocessTags()` instead of `processRichText()` so `&` codes survive into `buildComponentFromMarkup()`.

---

- **NeoEssentials Kits System — ClassCastException (`JsonArray` cast to `JsonObject`)**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: `ConfigSplitter` mapped `"kits"` section to `kits.json`. `KitManager` also wrote kit definitions there as a `JsonArray`. `mergeSplitConfigs()` extracted it and `getAsJsonObject("kits")` crashed with `ClassCastException`.

  **Fix**: `ConfigSplitter` now maps `"kits"` → `"main.json"`. `mergeSplitConfigs()` only merges the key when `isJsonObject()` is true. All ConfigManager kit-settings helpers carry explicit `isJsonObject()` guards.

---

- **NeoEssentials Permissions Not Recognising OP / FTB Ranks NoSuchMethodException**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Fix**: OP bypass now checked before delegating to external adapter. `FtbRanksAdapter` probes two API strategies; first to resolve is used for all subsequent checks.

---

- **NeoEssentials Admin Shop `?` Item Assignment — "This shop is not yet ready"**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: Admin shops have `ownerUUID = null`; ownership check always returned false for admin shops.

  **Fix**: Handler now checks `shop.isAdminShop()` first; any player with `neoessentials.shop.create.admin` may assign the item.

---

- **NeoEssentials `/help 2` Pagination — "No command found"**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: Vanilla `/help <command:string>` claimed `"2"` before NeoEssentials' integer `<page>` argument could fire.

  **Fix**: Replaced integer `<page>` branch with a single `<page_or_command>` string argument that checks `Integer.parseInt()` first.

---

- **NeoEssentials Ban/Unban — Vanilla Bans Not Detected by `/unban`**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: `BanManager` maintained its own list separately from Minecraft's `banned-players.json`.

  **Fix**: `isPlayerBanned()` falls back to vanilla `UserBanList`. `banPlayer()` / `tempBanPlayer()` write to vanilla list. `unbanPlayer()` removes from vanilla list.

---

- **NeoEssentials Rules Command — "Rules are not set" With Existing `rules.json`**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: Renamed `rules.json` → `rules_data.json` in 1.0.2.6; `loadRulesData()` only looked for the new name.

  **Fix**: `loadRulesData()` checks `rules_data.json` first; falls back to legacy `rules.json` and auto-migrates.

---

- **NeoEssentials MOTD — Save Path Inconsistency**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: `MotdCommand` used raw `Paths.get("config", "neoessentials", "motd_data.json")` instead of `ResourceUtil.getConfigFile()`, causing writes to wrong location on some hosts.

  **Fix**: `MOTD_DATA_FILE` now uses `ResourceUtil.getConfigFile("motd_data.json")`.

---

- **TPA permissions not syncing with new role**
  *(Status: Fixed → v1.0.2.6+build.4)*

  **Root cause**: `LuckPermsAdapter` was not subscribing to LuckPerms events. Command trees never re-sent to affected players after group changes.

  **Fix**: `LuckPermsAdapter` now subscribes to `UserDataRecalculateEvent` and `GroupDataRecalculateEvent`; both call `server.getCommands().sendCommands(player)`. `hasPermission` now uses live context-aware `QueryOptions` for online players.

    - **Reload command does not apply configuration changes** *(Status: Fixed)*

  **Root cause 1**: `TablistManager` not included in reload sequence.
  **Root cause 2**: Brigadier command tree not re-sent to online players after reload.

  **Fix**: `reloadConfiguration()` now calls `TablistManager.loadConfig()` + `updateAll()` and `WorthManager.reload()`. Command tree re-pushed to all online players via `server.getCommands().sendCommands(player)`.

---

>  **Features & Improvements** have been moved to [`Features_And_Improvements.md`](./Features_And_Improvements.md)

---

- **NeoEssentials `/back` Returns "No Previous Location" After Death**
  *(Status: Fixed → v1.0.2.6+build.112)*

  **Reported behavior:** After dying, `/back` always returned "§cNo previous location to return to." even though the player had just died at a known location.

  **Root causes found:**

  1. **Missing explicit `bus = Bus.GAME` on `@EventBusSubscriber`** — `MiscTeleportManager` used `@EventBusSubscriber(modid = "neoessentials")` without specifying the bus. Other classes in the mod explicitly use `bus = Bus.GAME`. While NeoForge defaults to `Bus.GAME`, the lack of an explicit declaration could cause silent registration failures in edge cases.

  2. **`receiveCanceled = false` (default) on death event handler** — If another mod or mechanic cancelled `LivingDeathEvent` at a higher priority (e.g. keep-inventory mods, protection plugins, god-mode handlers), our NORMAL-priority handler was silently skipped and `saveDeathLocation` was never called. The player DID die (death screen shown, respawn triggered), but NeoEssentials never recorded the death position. Changed to `@SubscribeEvent(receiveCanceled = true)` to always capture the position when a `ServerPlayer` dies regardless of event cancellation.

  3. **`PlayerDataStore.flush()` silently failed when directory missing** — `flush()` wrote to `neoessentials/playerdata/back_locations/<UUID>.json`. If the directory didn't exist (fresh install, first ever death), `FileWriter` threw and the exception was caught/logged but the death location was not persisted. After a server restart, `/back` would return "no history". Added an explicit `dataDirectory.mkdirs()` guard inside `flush()`.

  4. **Missing `backSettings` section in default `config.json`** — `enableDeathBack`, `enableTeleportBack`, `teleportDelay`, and `backCooldown` had no explicit entries in the bundled config. Added `teleportation.backSettings` with all four keys.

  **Fixes applied:**

  | File | Change |
  |---|---|
  | `MiscTeleportManager.java` | Added `bus = Bus.GAME`; `@SubscribeEvent(receiveCanceled = true)`; INFO-level log in `onPlayerDeathEvent`; `loadConfig` checks `backSettings` then `miscSettings` for `backCooldown`. |
  | `SpawnOnDeathHandler.java` | Added `bus = Bus.GAME` (consistency). |
  | `PlayerDataStore.java` | `flush()` now calls `dataDirectory.mkdirs()` before writing; logs ERROR if creation fails. |
  | `config.json` (bundled) | Added `teleportation.backSettings` section. |

---

## Bugs Discovered — build.150 audit

---

- **`NeoEssentials.java` — `Thread.sleep(2000)` Called on Minecraft Server Main Thread**
  *(Status: Fixed → v1.0.2.6+build.150)*

  **Root cause**: Inside `GameEvents.onPlayerLoggedIn()`, when an admin first joins after server start and the config-split notification needs to be shown, the code called:
  ```java
  server.execute(() -> {
      Thread.sleep(2000); // ← BLOCKS THE SERVER MAIN THREAD
      player.sendSystemMessage(...);
  });
  ```
  `server.execute()` submits work to the **Minecraft server tick thread** — the single thread that drives all game logic. Calling `Thread.sleep(2000)` inside that runnable pauses the tick thread for 2 full seconds, causing:
  - A 2-second complete server freeze visible to all online players (rubber-banding, no block updates, etc.)
  - `Can't keep up! Did the system time change, or is the server overloaded?` log warnings
  - In worst case: watchdog timeout and crash if another thread monitors tick time

  **Fix**: Moved the 2-second sleep to a dedicated daemon background thread (`NeoEssentials-AdminNotify`). Once the sleep completes, the message sending is marshalled back to the server thread via `server.execute()` — the same pattern used by `HologramScheduler` and `TablistManager` for safe server-thread callbacks.

  | File | Change |
  |---|---|
  | `NeoEssentials.java` | Replaced `server.execute(() -> { Thread.sleep(2000); ... })` with a daemon thread that sleeps off-thread, then calls `server.execute()` only for the message sends. `InterruptedException` now correctly re-interrupts the thread instead of being silently swallowed. |

---

## Bugs Discovered — build.149 audit

---

- **EconomyManager — `lastActivityFile` Uses Raw Relative Path**
  *(Status: Fixed → v1.0.2.6+build.149)*

  **Root cause**: `lastActivityFile` was declared as `new File("neoessentials/balances_activity.json")` — a raw relative path resolved from the JVM working directory. The companion `balancesFile` on the very next line correctly used `ResourceUtil.getDataFile("balances.json")`.

  **Effect**: On server hosts where the JVM working directory differs from the server root (Pterodactyl, AMP, etc.), `balances_activity.json` was created in or read from a different location than `balances.json`. This caused the inactive-account cleanup scheduler to never find any activity data and silently wipe all economy account balances it classified as "inactive".

  **Fix**: Changed to `ResourceUtil.getDataFile("balances_activity.json")` to match `balancesFile`.

  | File | Change |
  |---|---|
  | `EconomyManager.java` | `lastActivityFile` now uses `ResourceUtil.getDataFile("balances_activity.json")`. |

---

- **TeleportRequestManager — `sendTpaRequest()` Dead Code with Missing Safety Checks**
  *(Status: Fixed → v1.0.2.6+build.149)*

  **Root cause**: A second method `sendTpaRequest(ServerPlayer, ServerPlayer, boolean)` existed alongside `sendTeleportRequest()`. It was never called from anywhere in the codebase — all commands routed through `sendTeleportRequest()`.

  **Effect**: The dead method duplicated the request logic but was missing three critical checks present in `sendTeleportRequest()`:
  1. **Cooldown** — no `lastRequestTimestamps` check; a player could spam requests if `sendTpaRequest` were ever invoked.
  2. **`allowMultipleRequests`** — no duplicate-request guard.
  3. **tptoggle** — no check for whether the target had disabled incoming teleport requests.

  If the method was ever called externally (e.g., by another mod or a future feature), all three protections would have been silently bypassed.

  **Fix**: Removed `sendTpaRequest()` entirely. Also removed the five now-unused imports (`Component`, `MutableComponent`, `ChatFormatting`, `ClickEvent`, `HoverEvent`) that were only used by that method.

  | File | Change |
  |---|---|
  | `TeleportRequestManager.java` | Removed dead `sendTpaRequest()` method and its unused imports. |

---

## Bugs Discovered — build.148 audit (historical)

---

- **BanManager — `isIPBanned()` Never Checked Expiry of Temp IP Bans**
  *(Status: Fixed → v1.0.2.6+build.148)*

  **Root cause**: `isIPBanned()` returned `ipBans.containsKey(ipAddress)` — a pure key existence check with no expiry logic, unlike `isPlayerBanned()` which correctly calls `ban.isExpired()`.
  **Fix**: Replaced `containsKey` check with a full `isExpired()` check. If the ban is expired and auto-expire is enabled, the entry is removed from `ipBans` and `saveIPBans()` is called immediately.

  | File | Change |
  |---|---|
  | `BanManager.java` | `isIPBanned()` now calls `ban.isExpired()`, removes stale entry, and `saveIPBans()` on auto-remove. |

---

- **BanManager — `saveIPBans()` / `loadIPBans()` Drop `expireTime` (Temp IP Bans Become Permanent After Restart)**
  *(Status: N/A — already handled in current code)*

  **Note**: On re-inspection of the live source, `saveIPBans()` already writes `expireTime` and `loadIPBans()` already reads it back with a null-safe fallback. No change required.

---

- **BanManager — `cleanupExpiredTempBans()` Never Cleans Expired IP Bans**
  *(Status: Fixed → v1.0.2.6+build.148)*

  **Root cause**: `cleanupExpiredTempBans()` iterated only `playerBans` and never touched `ipBans`. Expired temporary IP bans accumulated in memory and on disk until a manual unban was issued.

  **Effect**: Memory leak over time; expired IP bans remained visible in `/ipbanlist`; `saveIPBans()` wrote expired entries back on unrelated save calls.

  **Fix**: Added a second iterator loop over `ipBans` in `cleanupExpiredTempBans()`, mirroring the player-ban sweep. Calls `saveIPBans()` if any expired IP bans were removed.

  | File | Change |
  |---|---|
  | `BanManager.java` | `cleanupExpiredTempBans()` now sweeps both `playerBans` and `ipBans`; logs separately for each type removed. |

---

- **MuteManager — No Persistence (All Mutes Lost on Server Restart)**
  *(Status: N/A — already handled in current code)*

  **Note**: On re-inspection of the live source, `MuteManager` already has a `load()`/`save()` pair backed by `ResourceUtil.getDataFile("moderation/mutes.json")`, with timed-mute expiry support and auto-remove on lookup. No change required.

---

- **IgnoreManager — No Persistence (All Ignores Lost on Server Restart)**
  *(Status: N/A — already handled in current code)*

  **Note**: On re-inspection, `IgnoreManager` already has `load()`/`save()` backed by `ResourceUtil.getDataFile("chat/ignore_lists.json")`. `cleanupPlayer()` is intentionally a no-op with an explanatory comment — ignore lists are designed to survive sessions. No change required.

---

- **PlayerJoinQuitHandler — New-Player Welcome Kit Blocked by Permission Check**
  *(Status: N/A — already handled in current code)*

  **Note**: The first-join kit path already bypasses `KitManager.giveKit()` entirely. It calls `kitManager.getKit(kitName)` to fetch the kit data, then iterates `starterKit.getItems()` and adds them directly to the player's inventory — no `canUseKit()` call, no permission check. No change required.

---

- **PlayerJoinQuitHandler — `first_joined.json` Uses Raw Relative Path**
  *(Status: N/A — already handled in current code)*

  **Note**: Line 61 already reads `com.zerog.neoessentials.util.ResourceUtil.getDataFile("first_joined.json")`. No change required.

---

- **LocalizationManager / LanguageCommand / MessageUtil — i18n Paths Use Raw `Paths.get()` / `new File()` Instead of `ResourceUtil`**
  *(Status: Fixed → v1.0.2.6+build.151)* Three files in the i18n/commands/util layer hardcoded raw paths to `neoessentials/...` directories instead of routing through `ResourceUtil`:
  - `LocalizationManager.java` — `langDirectory = Paths.get("neoessentials", "webdashboard", "lang")`
  - `LanguageCommand.java` — two calls to `Paths.get("neoessentials", "languages", "templates", fileName)`
  - `MessageUtil.java` — `new File("neoessentials/languages/custom/...")` in `loadCustomLanguageFile()` and `loadAllCustomLanguages()`

  **Effect**: Same as other raw-path bugs — mismatched paths on hosts where the JVM working directory differs, plus `ResourceUtil.DATA_DIR` changes would be silently bypassed for the entire language/i18n subsystem.

  **Fix**: Replaced all raw paths with `ResourceUtil.getDataPath()` / `ResourceUtil.getDataFile()`. Removed now-unused `Paths` imports from `LocalizationManager` and `LanguageCommand`. Added `ResourceUtil` import to `LocalizationManager` (same package in `MessageUtil`, no import needed).

  | File | Change |
  |---|---|
  | `LocalizationManager.java` | `langDirectory` now uses `ResourceUtil.getDataPath("webdashboard/lang")` |
  | `LanguageCommand.java` | `generateTemplate()` and `exportMissingKeys()` now use `ResourceUtil.getDataPath("languages/templates/...")` |
  | `MessageUtil.java` | `loadCustomLanguageFile()` and `loadAllCustomLanguages()` now use `ResourceUtil.getDataFile("languages/custom/...")` |

---

- **TaskManager — Scheduler Paths Use Raw `Paths.get()` Instead of `ResourceUtil`**
  *(Status: Fixed → v1.0.2.6+build.150)*

  **Root cause**: `TASKS_DIR`, `TASKS_FILE`, and `HISTORY_FILE` were all initialised with hardcoded `Paths.get("neoessentials", "scheduler")` / `.resolve(...)` instead of the project-standard `ResourceUtil.getDataPath()`. Every other data-file path in the mod uses `ResourceUtil`; the scheduler was the sole outlier.

  **Effect**: Path inconsistency — if `ResourceUtil.DATA_DIR` were ever changed, scheduler files would silently continue writing to the old location. Also defeats any future path-override mechanism built on top of `ResourceUtil`.

  **Fix**: Replaced all three constants with `ResourceUtil.getDataPath("scheduler")`, `ResourceUtil.getDataPath("scheduler/tasks.json")`, and `ResourceUtil.getDataPath("scheduler/execution_history.json")`. Added `ResourceUtil` import; removed now-unused `Paths` import.

  | File | Change |
  |---|---|
  | `TaskManager.java` | `TASKS_DIR`, `TASKS_FILE`, `HISTORY_FILE` now use `ResourceUtil.getDataPath()`. |

---

- **ShopManager / PlayerChatFormatManager — Runtime Data Stored in Config Directory**
  *(Status: Fixed → v1.0.2.6+build.152)*

  **Root cause**: Two managers stored player-generated runtime data using `ResourceUtil.getConfigPath()` / `getConfigFile()`, which resolves to `config/neoessentials/`. The `CONFIG_DIR` is explicitly intended for server configuration files (read-only after initial setup). Runtime data generated by player actions belongs in `DATA_DIR` (`neoessentials/`).

  - `ShopManager.java` — `getDataFile()` returned `ResourceUtil.getConfigPath("shops.json")`. Shops are player-created at runtime with owner UUIDs, block positions, and chest links.
  - `PlayerChatFormatManager.java` — Per-player chat format overrides (assigned via admin commands, keyed by UUID) were stored as `config/neoessentials/player_chat_formats.json`.

  **Effect**: Player-created shops and custom chat formats would accumulate in `config/neoessentials/` rather than `neoessentials/`. On typical Minecraft hosting setups the `config/` directory is often excluded from world backups but not from mod config resets, meaning a config wipe could silently delete all player shops and chat format assignments.

  **Fix**: Changed both to use `getDataPath()` / `getDataFile()` with appropriate subdirectories.

  | File | Change |
  |---|---|
  | `ShopManager.java` | `getDataFile()` returns `ResourceUtil.getDataPath("shops.json")` |
  | `PlayerChatFormatManager.java` | `getDataFile()` returns `ResourceUtil.getDataFile("chat/player_chat_formats.json")` |

---

- **ResourcePackManager — `Thread.sleep(1000)` Called on Server Main Thread at Player Login**
  *(Status: Fixed → v1.0.2.6+build.153)*

  **Root cause**: `onPlayerJoin()` submitted `server.execute(() -> { Thread.sleep(1000); sendResourcePack(player); })`. `server.execute()` enqueues work on the **Minecraft server tick thread** — the single thread that drives all game logic. Sleeping it for 1 second causes a complete server freeze for every player on login: rubber-banding, no block updates, `Can't keep up!` warnings, and potential watchdog crashes.

  **Effect**: Every time a player joined the server, the server tick thread was suspended for 1 second. With multiple concurrent logins, pauses stacked. Same root-cause as the previously fixed `NeoEssentials.java` admin-notify sleep.

  **Fix**: Moved the 1-second sleep to a dedicated daemon background thread (`NeoEssentials-ResourcePackDelay`). Once the sleep completes, the `sendResourcePack()` call is marshalled back to the server tick thread via `server.execute()`.

  | File | Change |
  |---|---|
  | `ResourcePackManager.java` | `onPlayerJoin()` no longer calls `Thread.sleep` on the server tick thread; sleep moved to a daemon background thread. |

