# 👾 Issues That Were Discovered
- **Languages EN, FR, DE, ES, ect. incomplete**: Some messages and features were not fully translated in all supported languages, a lot of hardcoded English strings, want to allow custom language files.
- **Permissions node investigation**: Permissions were not fully implemented for all features, and there were inconsistencies in permission checks across different commands and features.

---

# ✅ Issues That Were Fixed

- **Command /AFK not working properly**
  *(Fixed: 2026-03-01)*
  Five separate root causes were found and fixed:

  - **Root cause 1 — `AfkManager.loadConfiguration()` was never called:**
    The method to read AFK settings from `config.json` (timeout, kick settings, broadcast messages, activity tracking, etc.) existed but was never wired up. `AfkManager` ran entirely on hardcoded defaults regardless of what was in the config file.
    **Fix:** Added `AfkManager.getInstance().loadConfiguration(afkObj)` call to `NeoEssentials.onServerStarted()`, right after `ChatManager` is initialized.

  - **Root cause 2 — `AfkActivityHandler` suspicious-score blocked real player activity:**
    The anti-AFK-farming filter incremented the suspicious score by 10 for every action beyond 10 of the same type in 60 seconds. The threshold to be considered "suspicious" was 100 — meaning just 10 block interactions (perfectly normal building) would permanently block that player's activity from resetting their AFK timer. The score decay was also broken: it compared `now - lastActivity` where `lastActivity` was set to `now` on every call, so the difference was always ~0 and the score never decayed.
    **Fix:** Raised `REPETITIVE_ACTION_THRESHOLD` from 10 → 30, raised `SUSPICIOUS_SCORE_THRESHOLD` from 100 → 300, fixed score decay to compare against `lastActionTime` for the relevant action type, and reset per-type count when the 60-second window expires.

  - **Root cause 3 — `AfkMovementDetector` was missing `@EventBusSubscriber`:**
    The class had `@SubscribeEvent` methods for player login and logout (to initialize/cleanup position tracking) but was missing the `@EventBusSubscriber(modid = "neoessentials")` class annotation. NeoForge never registered those listeners, so player positions were never cleaned up on logout and never initialized on login.
    **Fix:** Added `@EventBusSubscriber(modid = "neoessentials")` annotation to the class.

  - **Root cause 4 — AFK broadcasts silently failed (`MessageUtil.info()` used as raw string):**
    `onPlayerGoAfk()` and `onPlayerReturnFromAfk()` called `MessageUtil.info(message)` where `message` was a plain string like `"Steve is now AFK"`. `MessageUtil.info()` treats its argument as a **translation key**, looks it up in the lang file, finds nothing, and returns the key unchanged — without colour or formatting. The broadcasts were also not logged to the server console.
    **Fix:** Replaced with `Component.literal("§e" + message)` directly. Added `server.sendSystemMessage()` call so broadcasts also appear in the server console.

  - **Root cause 5 — `/afk` command gave no feedback to the player:**
    `toggleAfk()` broadcasts a message to all players, but the player who typed `/afk` received no direct personal confirmation that the command worked — especially confusing since the broadcast message may not be visible to the player themselves if it's formatted differently.
    **Fix:** After calling `toggleAfk()`, the command now sends a direct `§eYou are now AFK.` / `§eYou are no longer AFK.` message to the executing player. Auto-AFK (inactivity timeout) also sends a personal notification: `§eYou are now AFK due to inactivity.`

- **NeoEssentials Chat Logging — chat messages not shown in server console (NeoForge 1.21.1, All The Mons)**
  *(Fixed: 2026-03-01)*
  - **Root cause:** When `enable-chat-formatting` is `true`, `ChatHandler` calls `event.setCanceled(true)` and takes over dispatch itself — sending messages via `sendSystemMessage()` to players only. `sendSystemMessage()` does **not** write to the server console. The only logging was `LOGGER.debug(...)` which is silent at the default log level. Vanilla's console logging never fires because the event is cancelled.
  - **Fix 1:** Added explicit `LOGGER.info("[channel] <player> message")` after dispatching to each channel type (proximity, permission-gated, global).
  - **Fix 2:** Added `server.sendSystemMessage(formattedMessage)` so the formatted message also appears in the dedicated server terminal exactly as vanilla would show it.
  - **Fix 3:** Added `logChatToConsole` boolean to `chat` config section (default `true`). Set to `false` to suppress chat from console/logs entirely if desired.
  - Config version bumped to 20.

- **NeoEssentials Teleportation — chunk not loaded causes "No safe teleport location found" even with safety disabled (NeoForge 1.21.1, All The Mons)**
  *(Fixed: 2026-03-01)*
  - **Root cause 1 — `isSafe()` used `canOcclude()`:** This is a strict opaque-cube check that returns `false` for slabs, stairs, glass, trapdoors, and many other solid blocks. Any home or warp set on those blocks was wrongly reported as unsafe.
    **Fix:** Replaced `canOcclude()` with `getCollisionShape(...).isEmpty()` in both `TeleportLocation.isSafe()` and `TeleportUtil.isSafeLocation()` — correctly matches the physical collision surface like Essentials does.
  - **Root cause 2 — `isSafe()` never checked dangerous blocks:** Lava, fire, cactus, nether portal, magma, etc. were all considered "safe" as long as feet/head space was air.
    **Fix:** Added `isDangerous()` helper in both `TeleportLocation` and `TeleportUtil` covering: lava, water, fire, soul fire, magma, cactus, sweet berry bush, wither rose, nether portal, campfire, soul campfire, powder snow.
  - **Root cause 3 — `findSafeLocation()` never did a top-down column scan first:** The XZ radius search with only a ±8Y window regularly failed to find the surface, especially for cross-dimension warps where the destination chunk was freshly loaded.
    **Fix:** `findSafeLocation()` now first does a full top-down column scan at the same X,Z (finds the surface in one pass), then falls back to the XZ expanding radius. `TeleportUtil.getHighestSafeY()` updated to use the same logic.
  - **Root cause 4 — `TeleportRequestManager` blocked `/tpa` entirely when destination was unsafe:** Matched old Bukkit plugin behaviour — no fallback, just an error. Essentials finds a nearby safe spot first.
    **Fix:** `executeTeleportRequest()` now calls `findSafeLocation()` first, warns the player ("teleporting to nearest safe location"), and only blocks if absolutely no safe location is found within 16 blocks.
  - **Root cause 5 — Double-safety in `HomeManager` and `WarpManager`:** Both managers already resolved a safe location before calling `TeleportUtil.teleportPlayer(..., findSafe=true)`, causing a second safety pass that could override the already-resolved location.
    **Fix:** Both managers now pass `findSafe=false` since safety is fully handled before the `TeleportUtil` call.

- **`/tpr` (Random Teleport) — basic brute-force with no config, safety, or biome awareness**
  *(Fixed: 2026-03-01)*
  - Old implementation was 50 blind random attempts with no safety checks, no cooldown, no world border awareness, no biome exclusions, no cache, no nether support.
  - **Fix:** Full port of EssentialsX's `RandomTeleport` system as `RandomTeleportManager.java`:
    - Equally-distributed offsets using the 4-rotation rectangle method (no centre-clustering)
    - Nether-aware Y detection (scans up from Y=32 below the bedrock ceiling)
    - World-border clamping
    - Pre-computation cache (filled asynchronously after each use, configurable `cacheThreshold`)
    - Configurable `findAttempts`, `cooldown`, `defaultMinRange`, `defaultMaxRange`
    - Per-location named slots — `/tpr [locationName]`
    - Excluded biomes list (global + per-location; oceans/void excluded by default)
    - Back-location saved before teleporting
    - Respects global `teleportDelay`
  - New `/settpr <locationName>` admin command to set RTP centre in-game.
  - New aliases: `/rtp`, `/randomtp`, `/randomteleport` all work.
  - Config: new `randomTeleportSettings` section added to `teleportation` in `config.json` (version bumped to 19).
  - Language keys added for all new messages.

- **Web Dashboard files not updating when newer versions are available**
  *(Fixed: previous session)*  
  Config version tracking (`_configVersion`) was already in place for config files. Dashboard HTML/JS/CSS files are now versioned and updated from JAR on server start when the bundled version is newer than what is deployed.

- **Dashboard Admin Controls and Permissions on a single page**
  *(Fixed: previous session)*  
  Admin controls and permissions management split into their own dedicated HTML pages (`admin.html`, `permissions.html`) instead of being crammed into one page.

- **Dashboard login requiring player to be online on server**
  *(Fixed: previous session)*  
  Auth system overhauled — players can register in-game with `/dashboard register` (requires permission), then log in from the web even when offline. Simple Discord Link integration added as an optional auth path; works standalone without the mod installed.

- **Dashboard register command not working**
  *(Fixed: previous session)*  
  `/dashboard register` command was not properly creating accounts. Registration flow fixed — generates token, stores credentials, confirms in-game.

- **Rich text (gradients/rainbow) not working despite being enabled in config**
  *(Fixed: previous session)*  
  Rich text tag parsing was not being applied to outgoing chat components. Fixed the chat processing pipeline to apply gradient/rainbow rendering when `richText.enabled` is `true`.

- **`/home` and `/warp` commands checking for safe teleports even when safety disabled in config**
  *(Fixed: previous session — and further strengthened 2026-03-01 per above)*  
  Config flag was being read correctly but the `findSafe=true` hardcoded argument to `TeleportUtil.teleportPlayer()` was overriding it. Fixed so that when safety is disabled in config, no safe-location search is performed.

- **PowerTool system — powertools affecting item slots instead of items**
  *(Fixed: previous session)*  
  PowerTool data was keyed on inventory slot index rather than item identity (NBT/item type). When a player moved items around, the powertool followed the slot, not the item. Fixed to key on item identity so the command travels with the item regardless of which slot it occupies.

- **Essentials teleportation system ported to NeoForge**
  *(Fixed: 2026-03-01)*  
  Investigated `./docs/Essentials/Essentials/src/main/java/com/earth2me/*` (CraftBukkit plugin source) and converted the teleportation architecture to NeoForge 1.21.1:
  - `RandomTeleportManager` (see `/tpr` fix above)
  - `isSafe()` / `findSafeLocation()` logic ported from `LocationUtil.java`
  - Dangerous block list ported from `DAMAGING_TYPES` / `LAVA_TYPES`
  - Top-down column scan ported from Essentials surface-finding behaviour

---

# 🎯 Additional Features

- **Economy integration**: Chest sign shops, Player Chest shops, Entity shops, dynamic pricing, CSV Dynamic pricing list import/export, and ect. more.
- **Holographic displays**: Support for holographic displays to show any information.
- **Chat formatting options**: More options for customizing chat format.
- **Inventory See**: Ability to view other players' inventories, editable inventories, and ender chests, based on permissions.
- **Minecraft Assets API support**: Figure out a way to integrate Minecraft Assets API for better resource assests to show in web-dashboards and other places.
- **Web-dashboard improvements**: Backup/restore functionality, more detailed statistics, and better user management, Backup/Restore from online storage services (Google Drive, Dropbox, etc).
- **Player Tablist**: Custom code for a custom player tab list that is highly customizable {References: Bungee Tablist Plus, TAB [1.7.x - 1.21.11], ☆ Simple TabList ☆《1.16.x - 1.21.x》- Animated - Hex colors}
- **Utility Systems**: Check if all these are in place, Nicknames, MOTD, near, ping, depth, helpop, rules, suicide, etc.
- **API & Placeholder System**: Apply more PlaceholderAPI integration, create more custom placeholders or allow the creation of more custom placeholders, REST API endpoints.
- **Permissions System Improvements**:
  - Wildcard & Hierarchical Permissions: Support for wildcards (e.g., neoessentials.*) and hierarchical permission inheritance, so granting a parent node gives access to all child nodes.
    Contextual Permissions: Allow permissions to be context-sensitive (e.g., per-world, per-channel, per-region, or time-based).
    Dynamic Permission Reloading: Add a command or event to reload permissions without restarting the server.
    Permission Checks in All Features: Ensure every command, event, and feature checks permissions strictly, including edge cases and new features.
    Permission Debugging Tools: Add commands to debug/check a user's effective permissions, showing where a permission is granted or denied.
    Permission Groups & Priorities: Allow group priorities, so if a user is in multiple groups, the highest priority group's permissions/prefixes/suffixes are used.
    Permission Expiry: Support temporary permissions that expire after a set time or event.
    API for Other Mods: Expose a clean API for other mods/plugins to check and register permissions.
    Permission Aliases: Allow aliases for permission nodes for easier migration or compatibility.
    Audit Logging: Log permission changes, grants, and denials for security and debugging.
    GUI Management: Provide a web or in-game GUI for managing permissions, groups, and users.
    Integration with External Systems: Improve and document integration with LuckPerms, FTB Ranks, and other permission mods, including fallback logic.
    Permission Suggestions: When a command is denied, suggest the required permission node in the error message.
    Fine-Grained Command Control: Allow per-argument or per-subcommand permissions (e.g., /home set vs /home delete).
    Custom Permission Conditions: Allow custom logic for permission checks (e.g., based on player stats, inventory, or server state).