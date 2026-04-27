# 👾 Issues That Were Discovered

---

# ✅ Issues That Were Fixed

## ✨ Build #78 — 2026-04-27 — /back History Chain Corruption Fix

- **`/back` acting weird after using warps/tps/back multiple times → ✅ FIXED in build.78**  
  After a server restart `/back` worked correctly, but degraded after multiple teleport operations (warps, /tp, /tpa accepts, /back chains). Three root causes were identified and fixed:

  **Root Cause 1 — Wrong player's back location saved on `/tpaccept` (primary bug):**  
  `TeleportRequestCommands.executeTpAccept()` called `MiscTeleportManager.saveBackLocation(teleportedPlayer)` where `teleportedPlayer` is the **acceptor** (the player who runs `/tpaccept`). For a `/tpa` request, the acceptor is NOT the one being teleported — the requester is. This caused the acceptor's back location to be silently overwritten with their current (unchanged) position every time they accepted someone's `/tpa`. Consequently, running `/back` after accepting a `/tpa` would either teleport the acceptor to their own current location (no-op) or to a stale position, not their intended prior destination. `TeleportRequestManager.executeTeleportRequest()` already correctly saves the back location for the actual teleporter, so the Commands-level save was both **wrong** (for `/tpa`) and **redundant** (for `/tpahere`).  
  **Fix**: Removed `saveBackLocation(teleportedPlayer)` from `TeleportRequestCommands.executeTpAccept()` entirely. The Manager is the sole authoritative back-location saver for TPA/TPAHERE teleports.  
  Affected file: `TeleportRequestCommands.java`

  **Root Cause 2 — Race condition: warmup-period concurrent teleport overwrites undo-back timestamp:**  
  In `MiscTeleportManager.teleportBack()`, the undo-position (`currentLocation`) was stored in the `thenAccept` callback with `System.currentTimeMillis()` (the time AFTER the warmup completes). If another teleport (warp, /tp, /tpa accept) fired during the /back warmup and saved its own back-location timestamp `T_warp`, the /back callback's `T_callback > T_warp` caused it to silently shadow the intervening save. This corrupted the back-location chain for players with warmup configured.  
  **Fix**: Snapshot `backTsAtDispatch` and `deathTsAtDispatch` before initiating the async teleport. In the callback, detect if an intervening teleport changed the timestamps. If so, store the /back undo-position with a timestamp one millisecond earlier than the intervening save so the chain is preserved without discarding the intervening save.  
  Affected file: `MiscTeleportManager.java` — `teleportBack()`

  **Root Cause 3 — Death-location saved message sent during `LivingDeathEvent` (wrong timing):**  
  `saveDeathLocation()` sent the "death location saved — use /back" hint to the player during `LivingDeathEvent`, when the player is transitioning to the death screen. The message showed at the wrong moment and was invisible or confusing.  
  **Fix**: Removed the `sendSystemMessage` from `saveDeathLocation()`. Added a new `@SubscribeEvent` handler for `PlayerRespawnEvent` that sends the hint 1 tick after respawn (so it arrives after vanilla respawn messages and the player can actually read it). Null-guarded `player.getServer()` in the tick-scheduled callback.  
  Affected files: `MiscTeleportManager.java` — `saveDeathLocation()`, new `onPlayerRespawn()` handler

## ✨ Build #77 — 2026-04-27 — BungeeTabListPlus-Inspired Tablist Rework

- **Tablist duplicate class definition compile error → ✅ FIXED in build.77**  
  `TablistCommand.java` contained two complete `class TablistCommand { ... }` definitions — the new BTLP-style class (lines 1–471) followed immediately by the old handler class (lines 473–727). This caused a compile-time "class already defined in package" error. **Fix**: Removed the duplicate old block; retained only the full BTLP-style implementation.  
  Affected file: `TablistCommand.java`

- **`FakePlayerManager` — `ClientboundPlayerInfoUpdatePacket(EnumSet, List<Entry>)` does not exist → ✅ FIXED in build.77**  
  NeoForge 1.21.x `ClientboundPlayerInfoUpdatePacket` has no public constructor that accepts a `List<Entry>`. The code tried to call `new ClientboundPlayerInfoUpdatePacket(actions, toAdd)` where `toAdd` is `List<ClientboundPlayerInfoUpdatePacket.Entry>`. **Fix**: Replaced with `buildFakePacket()` — a reflection-based helper that creates an empty packet via `ClientboundPlayerInfoUpdatePacket(actions, Collections.emptyList())` then injects the entries list via `Field.setAccessible(true)`.  
  Affected file: `FakePlayerManager.java`

- **`ProxyIntegration` — `@Override write(FriendlyByteBuf)` method does not override supertype → ✅ FIXED in build.77**  
  NeoForge 21.1.179 removed the `write()` method from `CustomPacketPayload` (replaced by the `StreamCodec` registration system). The anonymous inner class `new CustomPacketPayload() { @Override public void write(FriendlyByteBuf) {...} }` caused a compile error because no such method exists in the interface. **Fix**: Removed the anonymous class; replaced `sendBungeeMessage()` with a documented stub that logs a debug message. Outbound BungeeCord channel messaging is deferred to a future build that will register a proper `StreamCodec` via the NeoForge mod-event bus. The feature is disabled by default (`proxy.enabled=false`) so there is no runtime impact.  
  Affected file: `ProxyIntegration.java`

- **NeoEssentials Proxy Integration with BungeeTabListPlus (Independent Mode) → ✅ Implemented in build.74–77**  
  Full BTLP-inspired tablist rework:
  - `TablistManager.java` — complete rewrite; 20+ placeholder tokens including proxy/session/stats tokens; per-player + per-group header/footer frame overrides; AFK indicator; group-colour overrides; session tracking; vanish filtering; delegates to sub-systems.
  - `TablistLayout.java` — new; BTLP-style layout/sorting: 1–4 columns, `sortByGroupWeight`, `groupSections`, `playersByServer`, `excludeServers`, `hiddenServers`, `maxSlotsPerColumn`.
  - `FakePlayerManager.java` — new; BTLP `fakePlayers` concept; stable UUIDs via `UUID.nameUUIDFromBytes`; reflection-based packet injection; per-viewer injection tracking to avoid duplicate ADD packets.
  - `ProxyIntegration.java` — new; BungeeCord plugin-messaging bridge; `GetServers` / `PlayerCount` / `GetServer` sub-channel handling; `{network_online}`, `{server_online:NAME}`, `{current_server}` placeholders; per-player server tracking; independent of tablist rendering.
  - `TablistCommand.java` — extended with BTLP sub-commands: `proxy`, `fakeplayer`, `layout`, `independent`.
  - `TablistEventHandler.java` — added join/quit lifecycle hooks; session start time tracking.
  - `tablist.json` — `_configVersion` 2→3; added `independentMode`, `proxy`, `fakePlayers`, `layout` sections with full documentation comments.

## ✨ Build #73 — 2026-04-27 — Messaging & SocialSpy Improvements

- **Named placeholder support in message templates → ✅ Implemented in build.73**  
  `/msg`, `/reply`, and SocialSpy now fully support named placeholders (`{message}`, `{MESSAGE}`, `{sender}`, `{receiver}`, `{sender_displayname}`, `{receiver_displayname}`, `{neoessentials_displayname}`, and any `{neoessentials_*}` PlaceholderAPI token) in all format templates. Both `{message}` and `{MESSAGE}` are accepted (case-insensitive).
    - Implementation: New `MessageUtil.resolveTemplate(player, template, extraVars)` method: applies named vars first, then PlaceholderAPI, then logs unresolved tokens in debug mode.
    - `MsgCommand` and `ReplyCommand` migrated to use `resolveTemplate()`.

- **Fallback formatting if template parsing fails → ✅ Implemented in build.73**  
  `resolveTemplate()` never throws. If PlaceholderAPI fails, the partially-resolved template is returned safely. `MessageUtil.localize()` already had a catch block; `resolveTemplate()` extends that safety to the PlaceholderAPI stage.

- **Debug logging for missing/misparsed placeholders → ✅ Implemented in build.73**  
  When `logging.enableDebugLogging = true`, any `{TOKEN}` tokens still present in a template after full resolution are logged as `WARN` with the original template and the list of unresolved tokens. SocialSpy adds format-resolution trace logs (which source selected, and the pre/post strings).

- **Admin-configurable SocialSpy formatting in config → ✅ Implemented in build.73**  
  New `chat.messaging` section in `config.json`:
  ```json
  "socialspyFormat":  "",   // override neoessentials.socialspy.format lang key
  "msgFormatTo":      "",   // override commands.neoessentials.msg.format.to
  "msgFormatFrom":    "",   // override commands.neoessentials.msg.format.from
  "replyFormatTo":    "",   // override commands.neoessentials.reply.format.to
  "replyFormatFrom":  ""    // override commands.neoessentials.reply.format.from
  ```
  Leave blank to use lang-file defaults. Config always takes priority when non-empty.
  SocialSpy format updated to use `{sender}`, `{receiver}`, `{message}` named vars. `_langVersion` bumped 14→15 (auto-merges on start), `_configVersion` 20→21.
    - Affected files: `MessageUtil.java`, `SocialSpyManager.java`, `MsgCommand.java`, `ReplyCommand.java`, `config.json`, `en_us.json`

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

> 📄 **Features & Improvements** have been moved to [`Features_And_Improvements.md`](./Features_And_Improvements.md)

