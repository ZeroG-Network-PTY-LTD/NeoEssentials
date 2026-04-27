# 👾 Issues That Were Discovered

---

# ✅ Issues That Were Fixed

## ✨ Build #72 — 2026-04-27 — FTB Ranks Adapter API Correction

- **FTB Ranks Adapter Permission Check Failure (`NoSuchMethodException`) → ✅ FIXED in build.72**  
  NeoEssentials was probing `FTBRanksAPI.getPermission(ServerPlayer, String, boolean)` and `FTBRanksAPI.hasPermission(UUID, String)` as its primary strategies — neither method exists in FTB Ranks `2101.1.3`. All four probed strategies fell through, leaving `resolvedMethod = null`, which caused every permission check to throw `NoSuchMethodException` and silently return `false`, effectively disabling FTB Ranks permission enforcement.
    - Root Cause: The `probeApi()` method in `FtbRanksAdapter.java` was testing API signatures from an older/pre-release build of FTB Ranks. The actual public API in `2101.1.3` exposes `FTBRanksAPI.getPermissionValue(ServerPlayer, String)` (static) which returns a `PermissionValue` interface with `asBooleanOrFalse()`.
    - Fix Applied (build.72):
        - **Strategy 1** corrected to probe `FTBRanksAPI.getPermissionValue(ServerPlayer, String)` — the confirmed static method in FTB Ranks 2101.1.x.
        - **Strategy 2** added: attempts `RankManager.getPermissionValue(ServerPlayer, String)` via `getInstance().getManager()` as a secondary path.
        - Old strategies 3 & 4 (`hasPermission(ServerPlayer,String)` / `checkPermission(ServerPlayer,String)`) kept as fallbacks at positions 3 and 4.
        - Old UUID-based strategy moved to position 5 as last-resort for oldest builds.
        - `invokeResolvedMethod()` updated to handle the new strategy numbering correctly.
        - `extractBoolean()` updated to call `asBooleanOrFalse()` first (before other coercion paths) when a `PermissionValue` instance is returned.
        - `"MISSING"` added to the `toString()` deny-list in `extractBoolean()` to match `PermissionValue.MISSING.toString()`.
    - Affected file: `FtbRanksAdapter.java`

## ✨ Build #70 — 2026-04-27 — `/msg` & SocialSpy Formatting Fix

- **`/msg` & `/reply` format templates broken by `MessageFormat` named-placeholder collision → ✅ FIXED in build.70**  
  Every `/msg` and `/reply` attempt produced the following console error and sent raw template text to players instead of formatted messages:
  ```
  Failed to format message - Key: commands.neoessentials.msg.format.to,
    Template: '&7[&aTo &f{neoessentials_displayname}&7] &f{MESSAGE}',
    Args: [], Error: can't parse argument number: neoessentials_displayname
  java.lang.IllegalArgumentException: can't parse argument number: neoessentials_displayname
  ```
    - Root Cause: `MessageUtil.localize()` passed the raw translation template directly to `MessageFormat.format()`.  
      `MessageFormat` treats any `{…}` token as a numbered format argument.  Templates for `/msg` and `/reply` contain NeoEssentials placeholder tokens such as `{neoessentials_displayname}` and `{MESSAGE}` that do not begin with a digit, so `MessageFormat` tried to parse them as argument indices and threw `IllegalArgumentException`.
    - Fix Applied (build.70):
        - Added `MessageUtil.escapeNamedPlaceholders(String)` — uses the regex `\{([^0-9'{}][^}]*)}` to detect non-numeric `{TOKEN}` patterns and wraps them in MessageFormat's single-quote literal escape (`'{'TOKEN'}'`).  After `MessageFormat.format()` runs, these are output verbatim as `{TOKEN}` and can be resolved normally by `PlaceholderAPI.setPlaceholders()`.
        - Both overloads of `localize()` now call `escapeNamedPlaceholders()` before `MessageFormat.format()`.
        - Positional placeholders `{0}`, `{1}`, … (starting with a digit) are deliberately left untouched so existing positional substitutions continue to work.
    - Affected file: `MessageUtil.java` — `localize(String, Object...)` and `localize(String, String, Object...)`

---

- **SocialSpy broadcast missing translation key `neoessentials.socialspy.format` → ✅ FIXED in build.70**  
  `SocialSpyManager.broadcast()` called `MessageUtil.component("neoessentials.socialspy.format", ...)` but the key was absent from `en_us.json`, causing the spy message to display a raw humanized fallback string.
    - Fix Applied (build.70): Added `"neoessentials.socialspy.format": "&8[&eSocialSpy&8] &b{0} &7→ &b{1}&7: &f{2}"` to `en_us.json`.  Arguments `{0}` = sender name, `{1}` = receiver name, `{2}` = message text.
    - `_langVersion` bumped `13 → 14`; `CURRENT_LANG_VERSION` constant in `MessageUtil` updated to match — existing deployments will auto-merge the new key on next server start.
    - Affected files: `en_us.json`, `MessageUtil.java`

---

## ✨ Build #69 — 2026-04-24 — Custom Player Tablist: Polish Pass

- **Tablist player-row prefix/suffix not rendering hex/gradient colors → ✅ FIXED in build.69**  
  After build.67 introduced rich-text header/footer support, the per-player prefix and suffix rendered in the tab-list **player column** (set via scoreboard teams) still used `Component.literal()` — hex or gradient codes in group prefixes therefore appeared as literal text rather than colors.
    - Root Cause: `updatePlayerTeam()` called `Component.literal(prefix)` / `Component.literal(suffix)` and had no rich-text conversion step.
    - Fix Applied (build.69): Routed both calls through the new `RichTextFormatter.processTablistText()` so group prefixes/suffixes (e.g. `&#FF5500[Admin] ` or `<gradient:FF0000-FF8C00>[Mod] </gradient>`) now render as proper colored Components in the player-name column.
    - Affected file: `TablistManager.java` — `updatePlayerTeam()`

---

- **Color codes inside placeholders corrupted after substitution → ✅ FIXED in build.69**  
  `applyPlaceholders()` was internally converting `&` → `§` *before* returning the frame text. This caused `&#RRGGBB` hex tokens to become `§#RRGGBB` (invalid) and `<gradient:…>` tags to pass through unchanged to the `processTablistText()` pipeline where `&`-codes had already been consumed.
    - Fix Applied (build.69): Removed the early `&` → `§` conversion from `applyPlaceholders()`. Color processing is now deferred entirely to `RichTextFormatter.processTablistText()` so all color syntax survives placeholder substitution intact.
    - Affected file: `TablistManager.java` — `applyPlaceholders()`

---

- **`RichTextFormatter` lacked a tablist-safe text processor → ✅ ADDED in build.69**  
  The existing `processRichText()` method could emit hover/click event markers (used in chat) that are silently dropped by `ClientboundTabListPacket`, causing malformed output.
    - Fix Applied (build.69): Added `RichTextFormatter.processTablistText(String)` — runs the full gradient → rainbow → named-color → format-tag → `<color:#RRGGBB>` pipeline, strips any hover/click markers, then calls `ChatComponentUtil.parseColorCodes()`. Enabled unconditionally (does not depend on the `enableChatEnhancements` server flag).
    - Affected file: `RichTextFormatter.java`

---

## ✨ Build #67 — 2026-04-24 — Custom Player Tablist (full feature)

- **Custom Player Tablist system implemented → ✅ Build #67**  
  Full rewrite and feature expansion of the tablist subsystem. Implements the `Custom Player Tablist` feature milestone. Inspired by TAB, BungeeTabListPlus, and Simple TabList.

  **What was built:**

  1. **Hex colors & gradients in header/footer**  
     `TablistManager.updatePlayer()` now builds header and footer through `RichTextFormatter` (build.69 refined this further with the dedicated `processTablistText()` method). Supports `&#RRGGBB`, `<gradient:FF0000-0000FF>text</gradient>`, `<rainbow>text</rainbow>`, named color tags (`<red>`, `<gold>`, …), and format tags (`<bold>`, `<italic>`, …).

  2. **Animated header/footer frames**  
     `header` and `footer` in `tablist.json` accept a JSON array. Each refresh tick advances one frame creating smooth text animations. `refreshInterval` (ticks, default 20) controls speed.

  3. **Per-group header/footer**  
     New `"groups"` section in `tablist.json` — each permission group (e.g. `admin`, `moderator`) can define its own `header`/`footer` arrays. Priority: **per-player → per-group → global**.

  4. **Per-player header/footer overrides**  
     - `"players"` UUID map in `tablist.json` for persistent per-player frames.
     - New runtime commands: `/tablist player <name> header <text>`, `/tablist player <name> footer <text>`, `/tablist player <name> reset`.

  5. **Per-group runtime commands**  
     `/tablist group <group> header|footer|reset` — adjust groups live without reloading config.

  6. **Extended placeholder set**  
     Added `{displayname}`, `{server_name}`, `{x}`, `{y}`, `{z}`, `{balance}`, `{time}`, `{bar}` alongside the existing 12 placeholders. Per-group `groupColors` map applies a color prefix to `{displayname}`.

  7. **Vanish + AFK integration**  
     `hideVanished: true` excludes vanished players from `{online}` for non-staff viewers. `showAfkIndicator: true` appends configurable `afkSuffix` (default `&7[AFK]`) to AFK players in the tab row.

  8. **`tablist.json` config template**  
     Bundled default config updated with gradient header example, per-group and per-player sections, `groupColors` map, and inline syntax reference comments.

  - Affected files: `TablistManager.java`, `TablistCommand.java`, `tablist.json`

---

## 🔧 Build #66 — 2026-04-24

- **Tablist prefix not appearing before username → ✅ FIXED in build.66**  
  Group prefix/suffix set in `permissions.json` was not displaying before player names in the tab list. Reported during post-build.64 testing.
    - Root Causes:
        1. `getPermissionPrefix()` / `getPermissionSuffix()` called `PermissionSystem.getManager()` which throws `IllegalStateException` before the permission system is fully initialised; the exception was silently swallowed in the `catch`, returning `""` every time.
        2. All three helpers (`getPermissionPrefix`, `getPermissionSuffix`, `getPermissionGroup`) had inconsistent fallback behaviour — `getPermissionGroup()` returned `"default"` when the user record was absent, but the prefix/suffix helpers returned `""` instead of looking up the default group's values.
    - Fix Applied (build.66):
        - Switched all three helpers to use `PermissionAPI.getManager()` (returns `null` instead of throwing), with an explicit null guard.
        - When the player has no explicit user entry (or `user.getGroup()` is `null`), all three helpers now fall back to `mgr.getDefaultGroup()` before looking up the group's prefix/suffix. The scoreboard team (and thus the tab list prefix row) now reliably shows the correct group prefix for every player, including freshly-joined players whose user entry was auto-created.
    - Affected file: `TablistManager.java` — `getPermissionPrefix()`, `getPermissionSuffix()`, `getPermissionGroup()`

---

- **Warn command not logging to server console → ✅ FIXED in build.66**  
  `/warn <player> <reason>` used `source.sendSuccess(..., broadcastToOps=true)` but had no explicit `LOGGER.info()` call — unlike `executeClearWarnings()` and `executeRemoveWarn()` which both had direct logger calls. On some server configurations (particularly when stdin is not a terminal, or the server uses a custom logging appender), `sendSuccess` feedback is not routed to the persistent log file.
    - Observed: Warn records were being saved correctly to `warns.json`, but no timestamped console/log line appeared for `/warn` specifically. Other warn commands (`/clearwarnings`, `/removewarn`) did log correctly.
    - Fix Applied (build.66): Added `LOGGER.info("[Warn] {} warned {} for: {} (warn #{}, ID: {})", warnedBy, playerName, reason, total, shortId)` in `WarnCommand.executeWarn()`, matching the style of the other warn-management commands.
    - Affected file: `WarnCommand.java` — `executeWarn()`

---

- **WarnManager failed to compile — duplicate `getInstance()` method → ✅ FIXED in build.66**  
  `WarnManager.java` contained two identical `public static WarnManager getInstance()` declarations (lines 28 and 44), causing `error: method getInstance() is already defined in class WarnManager` at compile time. The mod JAR could not be built until this was resolved.
    - Fix Applied (build.66): Removed the duplicate declaration at line 44 (line 28 is the canonical definition, adjacent to the `INSTANCE` field).
    - Affected file: `WarnManager.java`

---

## 🔧 Build #64 — 2026-04-24

- **`/help [page]` returns "no permission" for regular players → ✅ FIXED in build.64**  
  Non-operator players received a "no permission" response when running `/help` or `/help <page>`. The `HelpCommand` guards the command with `PermissionAPI.hasPermission(uuid, "neoessentials.help")`, but this node was absent from the `default` group in `permissions.json`, so all non-op players were blocked.
    - Root Cause: `neoessentials.help` was missing from the `default` group's `permissions` array in both the bundled `src/main/resources/data/config/neoessentials/permissions.json` and the deployed `run/config/neoessentials/permissions.json`.
    - Fix Applied (build.64): Added `"neoessentials.help"` to the `default` group's permission list in `permissions.json`. Help is now accessible to all players by default with no operator status required.
    - Affected file: `permissions.json` — `default` group

---

- **Localization Audit — 54 missing translation keys + no fallback for unknown keys → ✅ FIXED in build.64**  
  *(See full entry further below in this file)*

---

## 📝 Configuration Notes (not code bugs)

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
  After running `/vanish`, the confirmation message appears in chat but other players can still see the vanished player in the world. Reported as: *"I can see myself vanished, message appears in chat but players can see me — is this because of LuckPerms?"*
    - Environment:
        - Mod Version: `neoessentials-1.0.2.6+50`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.220`
        - Java Version: `openjdk 21`
        - Dedicated Server (LuckPerms present)
    - Observed Behavior:
        - `/vanish` sends the confirmation message successfully.
        - Players can still see the vanished player's entity walking around in the world.
        - (LuckPerms is not the cause — the bug is entirely server-side in VanishManager.)
    - Root Causes (4 bugs found in `VanishManager.java`):
        1. **Entity never removed from the world** — `hidePlayerFromOthers()` opened with `if (!isHideFromTabListEnabled()) return;`. When the config flag was `true` it only sent `ClientboundPlayerInfoRemovePacket` (removes the name from the tab-list / F3 overlay). It never sent `ClientboundRemoveEntitiesPacket`, so the player's body was always visible in the game world regardless of config.
        2. **`showPlayerToSpecific()` was completely empty** — The method contained only a comment (`// For now, we'll rely on the client's natural player discovery`) and sent zero packets. Unvanishing therefore did nothing for observers who were already online, and staff with see-vanished permission could never actually see vanished players on join.
        3. **Priority check logic was inverted / broken** — `hidePlayerFromOthers()` used `if (viewerPriority > vanishedPriority)` to decide who to hide from. Both the observer default and the vanished-player default are `10`, so `10 > 10 = false` → nobody was ever hidden by the default path.
        4. **Newly joining players could always see vanished players** — `onPlayerJoin()` only handled hiding the vanished player from others (delayed), but never hid already-vanished players from the player who was just joining. The vanilla entity-tracking system unconditionally sent spawn packets for all nearby players during login, including vanished ones, and nothing ever suppressed them for the new observer.
    - Fix Applied (build.1.0.2.6+51):
        - **`hidePlayerFromSpecific()`**: Now sends both `ClientboundPlayerInfoRemovePacket` (conditional on `isHideFromTabListEnabled()`) **and** `ClientboundRemoveEntitiesPacket` (always, unconditionally) — this is the packet that actually removes the player entity from the observer's world.
        - **`showPlayerToSpecific()`**: Fully implemented. Sends the complete packet sequence to restore the player in the observer's world: `ClientboundPlayerInfoUpdatePacket.createPlayerInitializing()` (tab-list), `ClientboundAddEntityPacket` (re-spawn entity), `ClientboundSetEntityDataPacket` (skin/metadata), `ClientboundSetEquipmentPacket` (armour/held items), `ClientboundRotateHeadPacket` (head yaw).
        - **`hidePlayerFromOthers()`**: Removed the early `return` on tab-list config. Fixed priority check: an observer may see a vanished player only when they are **explicitly** in `viewerPriorities` AND their priority number is `<=` the vanished player's (i.e. equal or higher staff rank). All other observers are hidden from.
        - **`onPlayerJoin()`**: Deferred the hide/show logic by 1 tick (`TickTask +1`) so that our `ClientboundRemoveEntitiesPacket` arrives on the client *after* the vanilla entity-spawn packets sent during the login sequence. Added the missing branch: when the joining player cannot see vanished, all currently-vanished players are hidden from them via `hidePlayerFromSpecific()`.

---

- **NeoEssentials Teleportation Safety Bug (NeoForge 1.21.1, build.1.0.2.5) → ✅ FIXED in build.1.0.2.6+36**  
  Teleportation to `/home` fails with *"No safe teleport location found"* even when `teleportation.homeSettings.enableHomeSafety` is set to `false`.
    - Environment:
        - Mod Version: `neoessentials-1.0.2.5`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.220`
        - Java Version: `openjdk 21`
        - Dedicated Server
    - Steps to Reproduce:
        1. Set `teleportation.homeSettings.enableHomeSafety` to `false` in `teleportation.json`.
        2. Run `/sethome main` at coordinates `366, 98, 9`.
        3. Teleport to `/spawn` at `0, 124, 0`.
        4. Attempt `/home main`.
    - Observed Behavior:
        - Teleport failed with error:
          ```
          Failed to teleport player to home 'main': No safe teleport location found
          ```
        - Teleport only worked when the player was close enough for the target chunk to already be loaded.
    - Root Causes (found in investigation):
        1. **Config flag not respected** — `teleportToHome()` was not reading `enableHomeSafety` from config at runtime; safety was always applied regardless of the setting.
        2. **Unloaded chunk caused false failure** — `TeleportUtil` only force-loaded the single target chunk. `findSafeLocation()` scans up to ±16 blocks in X/Z, which can cross chunk boundaries. Neighbouring unloaded chunks caused every candidate position to fail `isSafe()`'s `level.isLoaded()` check, returning `null` → *"No safe teleport location found"*.
    - Fix Applied (build.1.0.2.6+36):
        - `teleportToHome()` now reads `isHomeTeleportSafetyEnabled()` from `ConfigManager` at runtime. Both `enableHomeTeleportSafety` (canonical) and `enableHomeSafety` (alias) key names are recognised.
        - `TeleportUtil.preloadChunksForTeleport()` added — force-loads the 3×3 chunk grid (target + all 8 neighbours) **unconditionally**, before any safety check or teleport attempt.
        - Safety check block (`isSafe()` / `findSafeLocation()`) is now **only executed when `requireSafe=true`**. When `enableHomeSafety=false`, the code skips all safety logic and teleports directly to the saved home coordinates, regardless of chunk load state.

---

- **NeoEssentials Web Dashboard Permissions & Admin Control Blank (NeoForge 1.21.1, build.1.0.2.6) → ✅ FIXED in build.1.0.2.6+46**  
  The web dashboard shows blank menus for permissions and admin controls after login.
    - Environment:
        - Mod Version: `neoessentials-1.0.2.6`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.220`
        - Java Version: `openjdk 21`
        - Dedicated Server
    - Observed Behavior:
        - Permissions menu is blank (expected if LuckPerms is used).
        - Admin control menu is also blank, even though dev tools show no errors and MC logs appear clean.
        - Refreshing the dashboard sometimes causes the buttons to appear briefly after login.
    - Root Causes (found in build.46 investigation):
        1. `showLoginScreen()` hid `dashboardWrapper` on sub-pages that have no `loginContainer`, resulting in a completely blank page with no way back.
        2. `permissions.js` init guard checked `window.location.hash` and `data-page`, neither of which ever matched on the standalone `permissions.html` page — `initPermissionSystem()` was never called.
        3. Nine `fetchWithAuth()` calls in `permissions.js` were missing `.json()` — modal actions for groups/permissions always silently failed.
        4. Username not shown on sub-page topbars (`id="userName"` vs `id="usernameDisplay"` mismatch).
    - Fix Applied (build.1.0.2.6+46):
        - `showLoginScreen()` now redirects to `index.html` when called on sub-pages.
        - `permissions.js` init changed to use `document.getElementById('permOverviewTab')` as the reliable guard.
        - All 9 `fetchWithAuth` calls fixed to call `.json()` on the response.
        - `showDashboard()` username fallback added (`usernameDisplay` → `userName`).

---

- **NeoEssentials Teleportation Message Bug (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ Fixed in build.1.0.2.6+38**  
  Teleportation messages sometimes display raw translation keys instead of localized text.
    - Root Causes:
        1. All `commands.neoessentials.teleport.spawn.*` message keys were missing from `en_us.json` (including `fallback_success`, `success`, `cleared`, `cooldown`, `warmup`, etc.).
        2. `MessageUtil.localize()` returns the raw key when not found, so players see keys like `commands.neoessentials.teleport.spawn.fallback_success` verbatim.
    - Fix Applied:
        - Added all missing spawn message keys to `en_us.json`.
        - Added `commands.neoessentials.teleport.warp.cooldown`, `warp.warmup`, `home.warmup`, `spawn.warmup`, and `spawn.cooldown` message keys.
        - Bumped `_langVersion` from `10` to `11` so existing server deployments auto-merge the new keys on next startup.
        - Updated `CURRENT_LANG_VERSION` constant in `MessageUtil.java` to `11`.

---
- **NeoEssentials Teleport Cooldowns & Warmups Not Working (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ Fixed in build.1.0.2.6+38**  
  Cooldowns and warmups configured for teleportation commands do not function at all.
    - Root Causes:
        1. **HomeManager warmup not reading config**: `teleportDelay` was hardcoded to `3` and never read from `teleportation.generalSettings.teleportDelay` in config.json.
        2. **HomeManager teleport cooldown not checked**: `homeTeleportCooldownSeconds` was read from config but never checked against `lastHomeTeleportTimestamps` in `teleportToHome()`.
        3. **WarpManager missing use cooldown**: `teleportation.warpSettings.warpCooldown` config key was present but never read or enforced in WarpManager. Only the *set* cooldown was tracked.
        4. **SpawnManager missing cooldown**: `teleportation.spawnSettings.spawnCooldown` was never read or enforced.
        5. **SpawnManager warmup overridden**: `loadSpawn()` read `teleportDelay` from spawn.json (stored as `0`) overriding any config.json value.
        6. **No warmup countdown messages**: Warmup messages were never sent to players before delayed teleports.
    - Fix Applied:
        - `HomeManager.loadConfig()`: Now reads `teleportDelay` from `teleportation.generalSettings.teleportDelay`.
        - `HomeManager.teleportToHome()`: Added atomically-checked cooldown using `lastHomeTeleportTimestamps`; displays `teleport_cooldown` message. Added warmup message when `teleportDelay > 0` and `enableTeleportWarmup=true`.
        - `WarpManager`: Added `warpUseCooldown` field and `lastWarpUseTimestamps` map. Reads `warpSettings.warpCooldown` from config. Cooldown check inserted before warp use. Added warmup message.
        - `SpawnManager`: Added `spawnCooldownSeconds` field and `lastSpawnTimestamps` map. Reads `spawnSettings.spawnCooldown` from config. Cooldown check inserted before spawn teleport. Added warmup message. Removed `teleportDelay` override from `loadSpawn()` — it is now driven exclusively by `generalSettings.teleportDelay` in config.json.

---
- **NeoEssentials Inventory & Ender Chest Commands Not Restricted (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ Fixed in build.1.0.2.6+40**  
  Non-OP and non-admin players could use `/inv` and `/ec` commands, leading to duplication exploits.
    - Root Causes:
        1. **Brigadier redirect does not re-evaluate `requires()`**: `/inv` and `/ec` were registered as `dispatcher.register(Commands.literal("inv").redirect(...))` with no `requires()` call on the alias node itself. In Brigadier, the `requires()` predicate of the redirect *target* is not automatically re-checked when the redirect is followed; only the alias node's own predicate is evaluated. Since the alias had no `requires()`, all players could use it.
        2. **Typo**: Line 73 used `.getChild("enderchestdit")` (missing 'e') instead of `"enderchestedit"`, which would return `null` and cause a `NullPointerException` when `/ecedit` was dispatched.
        3. **Missing permission nodes**: `neoessentials.invsee` and `neoessentials.enderchest` were not listed in `permissions.json`'s moderator group, so even if the checks ran they had no default group assignment.
        4. **Hardcoded raw message strings**: `viewInventory()` and `viewEnderChest()` used `MessageUtil.error("You cannot view your own inventory with this command!")` and `MessageUtil.success("Opening editable inventory of ...")` instead of translation keys.
    - Fix Applied:
        - **`InventoryViewCommands.java`**: Removed all `redirect()`-based aliases. Replaced with full command registrations for `/inv`, `/ec`, and `/ecedit` that include their own `requires()` predicate matching the underlying commands (`neoessentials.invsee`, `neoessentials.enderchest`, `neoessentials.enderchest.edit`). This guarantees the permission check runs on every execution path.
        - **Typo fixed**: `/ecedit` now correctly targets the `enderchestedit` logic.
        - **`permissions.json`**: Added `neoessentials.invsee` and `neoessentials.enderchest` to the `moderator` group. `neoessentials.invsee.edit` and `neoessentials.enderchest.edit` remain admin-only (covered by `neoessentials.*`).
        - **Translation keys added**: Added `commands.neoessentials.invsee.*` and `commands.neoessentials.ec.*` keys to `en_us.json` for all invsee/ec messages.


---

- **NeoEssentials Vanish Cannot Be Disabled (NeoForge 1.21.1, builds 1.0.2.5 & 1.0.2.6+21)** ✅ **FIXED in build.41**
  Disabling the vanish module in config does not actually disable it, causing conflicts with other vanish mods.
    - Environment:
        - NeoEssentials Versions: `1.0.2.5` and `1.0.2.6 build 21`
        - Minecraft Version: `1.21.1`
        - NeoForge Versions: `21.1.221` and `21.1.222`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Vanish remains active even when disabled in config.
        - Other vanish mods cannot take priority because NeoEssentials overrides them.
        - Multiple users confirmed the same issue when attempting to disable NeoEssentials vanish.
    - Expected Behavior:
        - Disabling vanish in config should fully disable the module, allowing other vanish mods to function.
    - **Root Causes (3 bugs):**
        1. **`ConfigManager.isVanishSystemEnabled()` read from the wrong config path.**
           Checked `config.has("enableVanishSystem")` at the root of `config.json`, but the key lives at
           `moderation.vanishSettings.enableVanishSystem`. Since the root key was never present, the method
           always returned `true` — making it impossible to ever disable vanish.
        2. **`ModerationEventHandler` interaction guards did not check `isVanishSystemEnabled()`.**
           Block-break / block-place / item-use cancellation for vanished players only checked
           `isVanishPreventInteractionEnabled()`, not whether the vanish system was enabled at all.
           This meant players who were already vanished before vanish was disabled could not interact.
        3. **`VanishManager.onPlayerJoin()` was never called.**
           The method that restores a vanished player's tab-list state on reconnect and sends the "you are
           vanished" reminder existed but had no call-site. Fixed by adding the call in
           `ModerationEventHandler.onPlayerLogin()`, gated by `isVanishSystemEnabled()`.
    - **Fix (build.41):**
        - `ConfigManager.java`: Fixed `isVanishSystemEnabled()` to navigate to `moderation.vanishSettings.enableVanishSystem`.
        - `ModerationEventHandler.java`: Added `isVanishSystemEnabled()` check to all three vanish interaction guards.
        - `ModerationEventHandler.java`: Added `VanishManager.getInstance().onPlayerJoin(player)` call in `onPlayerLogin`, gated by the enabled check.

---

- **NeoEssentials Home Confirmation Actions Broken (NeoForge 1.21.1, build.1.0.2.6+21)** ✅ **FIXED in build.44**
  Confirmation prompts for `/sethome <home>` (overwriting) and `/delhome` do not work correctly, causing invalid home names to be appended with "confirm" repeatedly.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 21`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.222`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - When overwriting or deleting a home, the chat shows:
          ```
          Are you sure you want to overwrite home 'Colony'? [Confirm] [Deny]
          ```  
        - Clicking confirm results in:
          ```
          Invalid home name: Colony confirm. Use letters, numbers, - or _ only (max 20 chars).
          ```  
        - Each subsequent confirm appends "confirm" to the home name:
            - Colony confirm
            - Colony confirm confirm
            - Colony confirm confirm confirm
        - The action never completes successfully.
    - Expected Behavior:
        - Confirmation should execute the overwrite/delete action without altering the home name.
    - **Root Cause:**
      The `confirm` and `deny` literals were registered as Brigadier child nodes **under** the `<name>` word-argument node (i.e. `/sethome <name> confirm`). The confirmation button's `RUN_COMMAND` click event sent `/sethome Colony confirm`. In Minecraft 1.21+, when the client dispatches a `RUN_COMMAND` string, it re-evaluates the command through the client-side Brigadier tree sent by the server. The client-side tree does not correctly represent the nested literal structure, so the full remaining input "Colony confirm" is consumed as a single word-argument value. The server receives "Colony confirm" as the value of `name`, passes it to `setHome()`, which rejects it as invalid (space not allowed), re-triggers the confirmation prompt with the new (invalid) name as the pending entry, and the cycle continues — appending "confirm" on every subsequent click.
    - **Fix (build.44):**
        - **`HomeCommands.java`**: Moved `confirm` and `deny` from being Brigadier children of the `<name>` argument to being **top-level literal siblings** of `<name>` under `sethome`/`createhome` and `delhome`/`deletehome`/`removehome`/`rhome`. In Brigadier, literals always take priority over argument nodes, so `/sethome confirm` reliably routes to the confirm handler while `/sethome Colony` (any non-reserved word) routes to the name argument. The home name is no longer embedded in the confirm/deny button commands — it is held server-side and retrieved from the existing pending maps in the handler.
        - **`HomeCommands.java`**: Updated `executeSetHomeConfirm`, `executeSetHomeDeny`, `executeDelHomeConfirm`, `executeDelHomeDeny` to read the pending home name from the server-side map instead of from command arguments; removed all `StringArgumentType.getString(context, "name")` calls from these four methods.
        - **`HomeCommands.java`**: Updated `executeSetHome` and `executeDelHome` to emit clean confirm/deny buttons (`/sethome confirm` / `/sethome deny`, `/delhome confirm` / `/delhome deny`) instead of `/sethome Colony confirm` etc.
        - **`en_us.json`**: Fixed `delete_success`, `delete_cancelled`, `delete_failed`, `overwrite_success`, `overwrite_failed` to use `{0}` (valid `MessageFormat` pattern) instead of `{HOME}`/`{home}` (were never substituted). Added `overwrite_already_pending`, `no_pending_overwrite_generic`, `delete_already_pending`, `delete_no_pending_generic`, `delete_no_confirm_required`, `limit_exceeded` keys. Lang version bumped `11 → 12`; new keys are auto-merged into existing deployments on next startup.

---

- **NeoEssentials /back Command Fails in Unloaded Chunks (NeoForge 1.21.1, build.1.0.2.6+21) → ✅ FIXED in build.1.0.2.6+42**  
  The `/back` command cannot find last death points or previous locations if they are in unloaded chunks.
    - Environment:
        - NeoEssentials Version: `1.0.2.6 build 21`
        - Minecraft Version: `1.21.1`
        - NeoForge Version: `21.1.222`
        - Java Version: `openjdk 21.0.10`
        - Dedicated Server
    - Observed Behavior:
        - Running `/back` after dying or teleporting fails when the target chunk is unloaded.
        - Error message: *"No safe teleport location found"* or failure to locate last death point.
        - Works correctly when the chunk is already loaded (e.g., when nearby).
    - Expected Behavior:
        - `/back` should teleport to the last death point or location regardless of chunk load state.
    - **Root Causes (2 bugs):**
        1. **`TeleportUtil` only force-loaded the single target chunk.** `findSafeLocation()` scans up to ±16 blocks in X/Z from the target position, which can cross chunk boundaries into adjacent chunks. Those neighbouring chunks were never loaded, so every candidate block position in them failed the `level.isLoaded(pos)` check in `TeleportLocation.isSafe()` → `isSafe()` returned `false` → `findSafeLocation()` returned `null` → teleport failed with "No safe teleport location found".
        2. **`MiscTeleportManager.teleportDelay` was hardcoded to `3`.** The field was never read from config (`teleportation.backSettings.teleportDelay` / `teleportation.generalSettings.teleportDelay`), so the configured warm-up delay was silently ignored for all `/back` and `/death` teleports.
    - **Fix (build.42):**
        - `TeleportUtil.java`: Added `preloadChunksForTeleport(ServerLevel, BlockPos)` public method that loads a 3×3 grid of chunks (target chunk + all 8 neighbours) using `TicketType.PORTAL` tickets before any safety check or `findSafeLocation()` scan runs. Also added a second call after `findSafeLocation()` resolves, to ensure the safe-landing chunk is also loaded. All `teleportPlayer()` calls (immediate and delayed) benefit automatically.
        - `ConfigManager.java`: Added `getBackTeleportDelay()`, `isDeathBackEnabled()`, and `isTeleportBackEnabled()` — the new config-reading methods that read from `teleportation.backSettings.*` with a fallback to `teleportation.generalSettings.teleportDelay`.
        - `MiscTeleportManager.java`: Added `loadConfig()` method that reads `teleportDelay`, `enableDeathBack`, and `enableTeleportBack` from `ConfigManager`; called at construction time and on reload.

---
- **Permissions System — GUI, External Systems & Fine-Grained Control not complete**
  *(Status: Fixed → v1.0.2.6+build.30)*

  **Root cause**: The three remaining Permissions System Improvements items were unimplemented:
  - GUI Management: `PermissionEndpoint` only handled basic group/user CRUD
  - Integration with External Systems: fallback chain, adapter health, and LuckPerms/FTB Ranks setup were undocumented
  - Fine-Grained Command Control: per-subcommand permission node tables were missing from the wiki

  **Fix (build.30)**:
  - `PermissionEndpoint` — 12 new REST methods added:
    - `reloadPermissions()` → `POST /reload`
    - `listGroupContextPerms()`, `addGroupContextPerm()`, `removeGroupContextPerm()` → group context CRUD
    - `listGroupTempPerms()`, `addGroupTempPerm()`, `removeGroupTempPerm()` → group temp CRUD
    - `listUserContextPerms()`, `addUserContextPerm()`, `removeUserContextPerm()` → user context CRUD
    - `listUserTempPerms()`, `addUserTempPerm()`, `removeUserTempPerm()` → user temp CRUD
    - `listAliases()`, `addAlias()`, `removeAlias()` → alias management
    - `getSystemStatus()` enhanced — emergency mode, adapter name/version/health/failures, alias count
  - `PermissionSystem.md` — 3 new major sections added:
    - **External Permission Mods** — rewritten with startup compatibility report, full 5-step fallback chain diagram, LuckPerms context + FTB Ranks probe documentation, compatibility table
    - **Fine-Grained Command Control** — per-subcommand node tables for Home, Warp, Kit, Economy, Moderation, Permission system; negative permission patterns
    - **GUI Management — Web Dashboard API** — full endpoint reference table with examples

---

- **Permissions System — Contextual permissions, conditions, API, and aliases not implemented**
  *(Status: Fixed → v1.0.2.6+build.28)*

  **Root cause**: The permissions system from build.25 covered basic group/user management and
  temporary permissions but lacked contextual overrides, condition evaluation, a mod-interop API,
  and alias resolution.

  **Fix (build.28)**:
  - `PermissionContext` — new value object capturing `worldId`, `dayTime`, `gamemode` for
    context-aware checks. `EMPTY` sentinel for non-player contexts.
  - `PermissionUser` / `PermissionGroup` — both extended with `contextualPermissions`
    (`Map<context, Map<node, Boolean>>`) and `conditions` (`Map<node, expression>`) maps.
  - `PermissionManager.hasPermission(UUID, String, PermissionContext)` — context-aware overload;
    resolution order: context deny → regular deny → temp grant → context grant → regular grant.
  - `PermissionConditionManager` — evaluates condition expressions (`time:day`, `gamemode:X`,
    `world:X`, `health:above/below:N`, `op:true/false`) with `AND`/`OR` compound support.
  - `PermissionAliasManager` — maps legacy/short node names to canonical nodes via
    `permission_aliases.json`; aliases resolved transparently in `PermissionAPI.hasPermission`.
  - `PermissionsService` interface + `PermissionsServiceImpl` — clean API for external mods;
    exposed via `NeoEssentialsAPI.getPermissionsService()`.
  - `PermissionsCommand` — `context add|remove|list` subcommands added to both `group` and
    `user` branches with full tab-completion and audit logging.
  - `PermissionStorage` — groups and users now persist `contextualPermissions` and `conditions`.
  - `PermissionAuditLogger` — 8 new action constants for context and condition events.
  - `PermissionRegistry` — 2 new nodes: `neoessentials.permissions.user.context`,
    `neoessentials.permissions.group.context`.
  - `NeoEssentialsAPI.API_VERSION` bumped from `1.0.0` to `1.1.0`.

---

  *(Status: Fixed → v1.0.2.6+build.9)*

  **Root cause**: NeoEssentials' internal `permissions.json` system only intercepted permission
  checks routed through its own `PermissionAPI.hasPermission()` method.  External mods (e.g.
  WorldEdit, FTB Chunks, WTHIT) check permissions via NeoForge's own
  `net.neoforged.neoforge.server.permission.PermissionAPI.getPermission(player, node)`, which
  did not consult `permissions.json` — so adding `WorldEdit.*` had no effect on WorldEdit.

  **Fix**:
  - Created `NeoEssentialsPermissionHandler` — a full implementation of NeoForge's
    `IPermissionHandler` interface.  When active, every Boolean permission-node check from any
    mod is evaluated against `permissions.json` through the full NeoEssentials chain (OP-bypass →
    external adapter → group/wildcard/user nodes).
  - Registered the handler under the identifier `neoessentials:handler` via
    `PermissionGatherEvent.Handler` so it is available in `config/neoforge-server.toml`.
  - **Auto-activation**: when no competing permission mod (LuckPerms / FTB Ranks) is loaded and
    the NeoForge config still points to the default handler, NeoEssentials automatically switches
    to `neoessentials:handler` at startup.  This means external mod permissions in
    `permissions.json` work out of the box on vanilla NeoForge servers.
  - `PermissionRegistry.isValidPermission()` widened to accept any well-formed dot-separated
    permission node (no longer restricted to `neoessentials.*` prefix).
  - `/permissions group add` now shows a contextual note when an external-mod permission is
    added, confirming whether the NeoEssentials handler is active.


- **NeoEssentials Invalid Wildcard Permission Formats — Startup Warnings**
  *(Status: Fixed → v1.0.2.6+build.8)*

  **Root cause**: `PermissionRegistry.isValidPermission()` used the regex `^[a-z0-9._-]+$` to
  validate permission nodes before registering them.  The `*` character is not in that character
  class, so every permission ending with `.*` failed validation and was silently dropped from the
  registry with the log warning:
  ```
  WARN Invalid permission format: neoessentials.spawner.*
  WARN Invalid permission format: neoessentials.fireball.*
  WARN Invalid permission format: neoessentials.warps.*
  ```
  `PermissionScanner.isValidPermission()` had the identical bug.

  **Important**: The permissions *worked at runtime* in all affected versions because
  `PermissionManager.hasPermissionWithWildcards()` evaluates wildcards directly from the group's
  permission list without consulting the registry.  The warnings were misleading — granting
  `neoessentials.spawner.*` to a group still gave access to all mob-spawner types.

  **Fix**:
  - `PermissionRegistry.isValidPermission()` now handles the `.*` suffix explicitly: it strips
    the suffix, validates the prefix with the same rules as before, and requires the prefix to
    start with `neoessentials`.  Both `neoessentials.*` and `neoessentials.spawner.*` now pass.
  - `PermissionScanner.isValidPermission()` received the same fix for consistency.
  - `PermissionManager.isValidPermission()` (used by `/permissions group add`) already handled
    wildcards correctly and was not changed.
  - `PermissionSystem.md` wiki updated: the three wildcards (`spawner.*`, `fireball.*`,
    `warps.*`) are now listed in the Wildcards table, and a note explains the previous warnings.

---

- **NeoEssentials Chat Colors — Format String Colors Stripped (All White Output)**
  *(Status: Fixed → v1.0.2.6+build.8)*

  **Root cause**: `ChatFormatter.formatMessage()` had a two-phase pipeline mismatch when
  `enableChatEnhancements` was `true` (the default):
  1. `RichTextFormatter.processRichText(formatted)` correctly converted `&c[Fondateur]` →
     a richly-colored Minecraft `Component`.
  2. `componentToFormattedString(richTextResult)` then called `component.getString()` to get
     a String back — but `getString()` **strips every formatting code**, returning plain
     uncolored text like `[Fondateur]`.
  3. `enhanceMessage(strippedText, …)` processed this colorless string and returned a
     Component with no colors.

  The result was that `&` color codes in `chat-format` values were silently discarded and
  every chat line appeared plain white, regardless of what was configured.

  **Also clarified** — the color codes must be placed in the format **value**, not the key:
  ```json
  // ❌ Wrong – color codes in the key break group matching
  "'&c'group:fondateur": "..."
  // ✅ Correct – color codes in the value, key stays clean
  "group:fondateur": "&f[&4Fondateur&f] &f{neoessentials_username}&7: &f{MESSAGE}"
  ```

  **Fix**:
  - Added `RichTextFormatter.preprocessTags(String text)` — a new public method that
    converts `<gradient:…>` and `<rainbow>` tags into `&#RRGGBB` hex codes as a plain
    String, leaving all `&` color codes untouched.
  - `ChatFormatter.formatMessage()` now calls `preprocessTags()` instead of
    `processRichText()` before the enhancement phase, so `&` codes survive as strings.
  - `enhanceMessage()` → `buildComponentFromMarkup()` already calls `parseColorCodes()`
    on every plain-text segment, so all `&c`, `&#RRGGBB`, etc. codes are now rendered
    correctly.
  - When enhancements are disabled, `processRichText()` is still called directly (unchanged
    path), so that scenario is unaffected.
  - Updated `ChatSystem.md` wiki with a "Color Codes in chat-format" section, including a
    correct per-group example and a table of common mistakes to avoid.

---

- **NeoEssentials Kits System — ClassCastException (`JsonArray` cast to `JsonObject`)**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: `ConfigSplitter.CONFIG_FILE_MAP` previously mapped the `"kits"` section key to
  `kits.json`.  `KitManager` also writes kit *definitions* to `kits.json` as `{"kits":[…]}` (a
  JsonArray).  When split-configs were active, `mergeSplitConfigs()` extracted that JsonArray under
  the key `"kits"` in the merged view, and every ConfigManager helper that called
  `config.getAsJsonObject("kits")` crashed with `ClassCastException`.

  **Fix**:
  - `ConfigSplitter` now maps `"kits"` → `"main.json"` (kit *settings* live alongside
    `modules`/`logging`/`permissions`).  `kits.json` is reserved for kit *definitions* only.
  - `mergeSplitConfigs()` only merges the `"kits"` key when `isJsonObject()` is true.
  - All ConfigManager kit-settings helpers now carry an explicit `isJsonObject()` guard before
    calling `getAsJsonObject("kits")`:
    `isAllowKitOverrideEnabled`, `isKitAutoEquipEnabled`, `isLogKitUsageEnabled`, and the
    already-guarded `getKitCommandCost`, `isPastebinCreatekitEnabled`,
    `isSkipUsedOneTimeKitsFromKitList`, `isNewPlayerKitEnabled`, `getNewPlayerKitName`,
    `getMaxKitsPerPlayer`.
  - `ConfigManager.getConfig(String)` now supports *section-name* lookups (no `.json` extension)
    by extracting the named section from the main config.  This fixes the
    `getConfig("chat")` → *"config/neoessentials/chat (No such file or directory)"* errors
    reported by `ChatFormatter`, `BadgeManager`, `ConditionalFormatter`, etc.

---

- **NeoEssentials Permissions Not Recognising OP / FTB Ranks NoSuchMethodException**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause 1**: `PermissionAPI.hasPermission()` skipped the OP-bypass check entirely when an
  external adapter (FTB Ranks) was registered, even if the external system was misconfigured or
  throwing.

  **Root cause 2**: `FtbRanksAdapter` called `hasPermission(UUID, String)` via reflection — a
  method that no longer exists in FTB Ranks 2101.1.x.

  **Fix**:
  - OP bypass is now checked *before* delegating to any external adapter, acting as a safe
    fallback so operators are never locked out regardless of the permission back-end.
  - `FtbRanksAdapter` now probes two API strategies via reflection:
    1. `FTBRanksAPI.getPermission(ServerPlayer, String, boolean)` (2101.1.x, strategy 1).
    2. `instance.hasPermission(UUID, String)` via `INSTANCE` or `getInstance()` (older builds,
       strategy 2).
    The first strategy that resolves at startup is used for all subsequent checks.

---

- **NeoEssentials Admin Shop `?` Item Assignment — "This shop is not yet ready"**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: Admin shops have `ownerUUID = null`.  `ShopInteractHandler.onRightClick()` tested
  `shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())` to decide who could assign the
  pending item; for admin shops this condition is *always false*, so every player (including the
  creating admin) got "This shop is not yet ready."

  **Fix**: For `itemPending` shops, the handler now checks `shop.isAdminShop()` first.  If true, any
  player holding `neoessentials.shop.create.admin` may assign the item; otherwise UUID ownership is
  required as before.

---

- **NeoEssentials `/help 2` Pagination — "No command found"**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: Vanilla Minecraft registers `/help <command:string>` before any mod command is
  loaded.  Brigadier matches children in insertion order, so the vanilla string argument claimed
  `"2"` before NeoEssentials' integer argument could fire; the vanilla handler then searched for a
  command named `"2"` and returned "No command found."

  **Fix**: The separate integer `<page>` branch has been replaced with a single `<page_or_command>`
  string argument that checks `Integer.parseInt()` first.  If the value is a valid page number
  (≥ 1) it shows that page; otherwise it searches for a command by name.

---

- **NeoEssentials Ban/Unban — Vanilla Bans Not Detected by `/unban`**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: `BanManager` maintained its own ban list (`neoessentials/moderation/player_bans.json`)
  separately from Minecraft's `banned-players.json`.  A vanilla `/ban` (or operator-issued ban) was
  never imported into the NeoEssentials list, so `isPlayerBanned()` returned `false` and `/unban`
  reported "Player is not currently banned."

  **Fix**:
  - `isPlayerBanned(UUID)` now falls back to the vanilla `UserBanList` when the player is not in
    the NeoEssentials list.  If found, the entry is imported so it appears in `/banlist`.
  - `banPlayer()` and `tempBanPlayer()` now also write to the vanilla `UserBanList` via
    `addToVanillaBanList()`, keeping both lists in sync.
  - `unbanPlayer()` now also removes the entry from the vanilla `UserBanList` via
    `removeFromVanillaBanList()`, so the player can connect again without a separate
    `/pardon` command.

---

- **NeoEssentials Rules Command — "Rules are not set" With Existing `rules.json`**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: NeoEssentials 1.0.2.6 renamed the rules storage file from `rules.json` to
  `rules_data.json`.  Servers migrating from an older build still had `rules.json` on disk; the
  new `loadRulesData()` method only looked for `rules_data.json` and fell through to
  `createDefaultRules()`, discarding the custom rules.

  **Fix**: `loadRulesData()` now checks for `rules_data.json` first.  If absent, it looks for the
  legacy `rules.json`, loads and migrates the rules into `rules_data.json`, then logs the
  migration.

---

- **NeoEssentials MOTD — Save Path Inconsistency**
  *(Status: Fixed → v1.0.2.6+build.5)*

  **Root cause**: `MotdCommand` built its save path with `Paths.get("config", "neoessentials",
  "motd_data.json")` — a raw relative `Path` — while the rest of the mod uses
  `ResourceUtil.getConfigFile()`.  On some host configurations the relative working directory
  differs, causing the file to be written to (or read from) the wrong location, making MOTD
  appear to reset on restart.

  **Fix**: `MOTD_DATA_FILE` is now declared as `ResourceUtil.getConfigFile("motd_data.json")`
  (a `java.io.File`), matching every other config/data file in the mod.  Load and save methods
  were updated to use the `File` API, and error messages now log the absolute path for
  easier diagnosis.

---

- **TPA permissions not syncing with new role**
  *(Status: Fixed -> v1.0.2.6+build.4)*  
  Created a new role with `tpa` permissions, but the permissions are not syncing with the mod.

    - Verified permissions in the LuckPerms Web Editor
    - Role appears to have the correct permission nodes
    - Users assigned to the role still cannot use `/tpa`
    - Issue persists after saving/syncing permissions

  **Notes**
    - **Root cause**: `LuckPermsAdapter` was not subscribing to LuckPerms events. When a player was
      added to a new group (or a group's permissions changed), the Minecraft command tree was never
      re-sent to the affected players — so tab-completion stayed stale until they rejoined.
    - **Fix**: `LuckPermsAdapter` now subscribes to `UserDataRecalculateEvent` (triggers on user group
      change) and `GroupDataRecalculateEvent` (triggers when a group's permissions are modified). Both
      handlers call `server.getCommands().sendCommands(player)` on the server thread so the permission
      change is reflected immediately in both command execution and tab-completion.
    - Additionally, `hasPermission` now uses the player's live context-aware `QueryOptions` when they
      are online instead of the static default, ensuring world/server contexts are honoured.

    - **Reload command does not apply configuration changes**
      *(Status: Fixed)*  
      The `/reload` command does not appear to apply configuration changes.

    - Disabled tab customization in the configuration
    - Ran the reload command
    - Changes were not applied in-game
    - Restarting the server applied the changes correctly

  **Notes**
    - **Root cause 1**: `TablistManager` was not included in the reload sequence. Disabling the tablist
      via config and running `/neoe reload` had no effect because the manager was never told to re-read
      its config file.
    - **Root cause 2**: After config reload, the Brigadier command tree was not re-sent to online
      players. Permission-gated commands therefore still showed/hid based on pre-reload state until
      the player relogged.
    - **Fix**: `reloadConfiguration()` now also calls `TablistManager.loadConfig()` + `updateAll()`
      and `WorthManager.reload()`. After all systems reload, the command tree is re-pushed to every
      online player via `server.getCommands().sendCommands(player)`.

---

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
    - More intuitive UI/UX design and mobile responsiveness, more pages for different modules (teleportation, moderation, kits, etc.).

- **NeoEssentials Proxy Integration with BungeeTabListPlus (Independent Mode)**  
  Request to add support for hooking into [BungeeTabListPlus](https://github.com/CodeCrafter47/BungeeTabListPlus) when NeoEssentials is running behind a proxy, while also allowing NeoEssentials to operate independently without mimicking other tab plugins.
    - Requested Update:
        - Implement a hook into BungeeTabListPlus API for player list synchronization and proxy-aware features.
        - Ensure NeoEssentials dashboard and commands respect proxy player states.
        - Provide compatibility with BungeeTabListPlus features such as custom tab formatting, placeholders, and cross-server player visibility.
        - Allow NeoEssentials to run in **independent mode**, managing its own tablist logic without relying on other tab plugins.
        - Source code for BungeeTabListPlus will be downloaded and placed in the `docs/` folder for reference.
    - Benefits:
        - Seamless integration with proxy environments.
        - Unified player list management across multiple servers.
        - Independent functionality ensures NeoEssentials tablist logic is consistent and not dependent on external plugins.
        - Enhances NeoEssentials usability for larger networks running behind proxies.

- **Messaging & SocialSpy Improvements**
    - Add support for named placeholders in message templates (`{neoessentials_displayname}`, `{MESSAGE}`).
    - Provide fallback formatting if template parsing fails.
    - Add debug logging to show which placeholders are missing or misparsed.
    - Allow admins to customize SocialSpy formatting in config safely.

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

# Improvements Done 

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




