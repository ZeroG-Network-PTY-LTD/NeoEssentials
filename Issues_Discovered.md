# 👾 Issues That Were Discovered

*(No open issues at this time — all known bugs have been resolved.)*

---

# ✅ Issues That Were Fixed

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

- **NeoEssentials /back Command Fails in Unloaded Chunks (NeoForge 1.21.1, build.1.0.2.6+21)** ✅ **FIXED in build.42**
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
    - CSV import/export for bulk pricing adjustments.
    - Future-proofing for more advanced economy plugins and integrations.

- **Holographic Displays**  
  Add support for holographic displays to show:
    - Shop information, player stats, server announcements.
    - Customizable text, icons, and animations.
    - Integration with permissions and PlaceholderAPI for dynamic content.

- **Chat Formatting Options**  
  Provide more customization for chat formats:
    - Per-group and per-player formatting.
    - Hex color support, gradients, and hover/click events.
    - Easier configuration with examples in documentation.

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

- **Custom Player Tablist**  
  Implement a highly customizable tablist system:
    - Support for animated headers/footers.
    - Hex colors and gradients.
    - References: Bungee Tablist Plus, TAB, Simple TabList.
    - Per-group and per-player customization.

- **API & Placeholder System**  
  Expand API and placeholder support:
    - Deeper PlaceholderAPI integration.
    - Ability to create custom placeholders.
    - REST API endpoints for external tools and dashboards.
    - Documentation for developers to extend NeoEssentials easily.


- **Localization Improvements**
    - Audit all NeoEssentials commands to ensure translation keys exist and are mapped correctly.
    - Add fallback text in English when a translation key is missing.
    - Provide tooling to regenerate or validate `messages.json` automatically.
    - Allow server admins to override messages easily via config.

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

  **Integration with External Systems** *(build #30)*
  - Startup compatibility report: adapter name, version, health, ⚠ NEWER THAN TESTED warning
  - Full 5-step fallback chain documented: emergency → OP bypass → external → internal → OP fallback
  - Adapter health tracking: 5 consecutive failures → `UNHEALTHY` → auto-fallback to `permissions.json`
  - LuckPerms: context-aware checks via live `QueryOptions`; step-by-step setup guide
  - FTB Ranks: 4-API-signature probe for version compatibility
  - Compatibility table: LuckPerms 5.4.x, FTB Ranks 2101.1.3, WorldEdit (any), FTB Chunks (any)

  **Fine-Grained Command Control** *(build #30)*
  - Every Brigadier branch has its own permission node (`/home set` vs `/home delete`, `/warp` vs `/setwarp`, etc.)
  - Per-subcommand node tables documented in `PermissionSystem.md` for: Home, Warp, Kit, Economy, Moderation, Permission system
  - Negative permission patterns (`-neoessentials.item.enchant.unsafe`) documented for targeted deny


    - ✅ `NickCommand` — storage path changed from raw `Paths.get("config", "neoessentials", "nickname_data.json")` to `ResourceUtil.getConfigPath("nickname_data.json")` for consistency with every other data file in the mod.
    - ✅ `NickCommand` — registered `/nickname` as a Brigadier **redirect** to `/nick` so the alias actually works at runtime (was metadata-only in `registry.registerCommand()`).
    - ✅ `SeenCommand` — storage path changed from raw `Paths.get("config", "neoessentials", "seen_data.json")` to `ResourceUtil.getConfigPath("seen_data.json")`.
    - ✅ `NeoEssentials.java` — removed duplicate `registry.registerCommand()` metadata block ("PLAYER INFO & ADMIN TOOL COMMANDS" section) that re-listed `seen`, `near`, `ping`, `playtime`, `whois`, `realname`, `sudo`, `suicide`, `msgtoggle`, `rtoggle`, `motd`, `rules` — all already registered in the UTILITY section above. Replaced with a single comment + `PlayerInfoCommands.register()` call for `/msgtoggle`.
    - ✅ `PermissionRegistry` — removed duplicate `register()` calls in the "Utility commands" and "Utility / misc commands" sections that silently overrode values set in the canonical "Player Info & Admin Tools" section: `neoessentials.whois` (was `ADMIN/false`, overridden to `MISC/true`), `neoessentials.ping.others` (was `PLAYER/true`, overridden to `MISC/false`), `neoessentials.seen`, `neoessentials.realname`, `neoessentials.near`, `neoessentials.ping`, `neoessentials.motd`, `neoessentials.rules`, `neoessentials.suicide`. Unique sub-nodes (`whois.detailed`, `rules.admin`, `motd.*`) kept in their canonical positions.
    - ✅ All core utility commands verified present, registered once, and using `PermissionValidator.validatePermission()` consistently: `/nick` `/nickname` `/setnick` `/near` `/nearby` `/ping` `/depth` `/helpop` `/motd` `/rules` `/suicide` `/killme` `/seen` `/whois` `/realname` `/msgtoggle`.

- **Temporary Permissions** *(build #25)*
    - ✅ Added `tempPermissions: Map<String, Long>` (node → expiry epoch-ms) to both `PermissionUser` and `PermissionGroup`, with `addTempPermission`, `removeTempPermission`, `getTempPermissions`, `purgeExpiredTempPermissions`, and `hasActiveTempPermission` helpers.
    - ✅ `PermissionManager.computePermission()` evaluates temp permissions (with wildcard support) **after** negative-perm denial but **before** regular user/group permissions — explicit `-node` denies still win.
    - ✅ `PermissionManager.purgeExpiredTempPermissions(MinecraftServer)` — iterates all users and groups, removes expired entries, clears the permission cache, notifies affected online players, logs each expiry to the audit log, and persists to disk.
    - ✅ `PermissionManager.parseDurationMs(String)` — parses human-readable durations (`1d`, `12h`, `30m`, `60s`, combinations like `1d12h30m`) into milliseconds; throws `IllegalArgumentException` for blank/zero/invalid input.
    - ✅ `PermissionManager.formatDuration(long ms)` — formats a millisecond remaining-time into a compact string (e.g. `2d 3h 15m 4s`).
    - ✅ `PermissionExpiryHandler` — `@EventBusSubscriber` on `ServerTickEvent.Post`, fires `purgeExpiredTempPermissions()` every **600 ticks (30 s)**.
    - ✅ `PermissionStorage` updated: `save()` strips expired entries before writing; `load()` only reads entries whose expiry is still in the future. Users gain `"tempPermissions"` key in `playerdata.json`; groups gain it in `permissions.json`.
    - ✅ New commands for users: `/permissions user <p> addtemp <node> <duration>`, `removetemp <node>`, `listtemp` (requires `neoessentials.permissions.user.temp` / `info.user`). Successful `addtemp` also notifies the target player if online.
    - ✅ New commands for groups: `/permissions group <g> addtemp <node> <duration>`, `removetemp <node>`, `listtemp` (requires `neoessentials.permissions.group.temp` / `info.group`).
    - ✅ All six actions logged to the audit log: `USER_TEMP_PERM_ADDED`, `USER_TEMP_PERM_REMOVED`, `USER_TEMP_PERM_EXPIRED`, `GROUP_TEMP_PERM_ADDED`, `GROUP_TEMP_PERM_REMOVED`, `GROUP_TEMP_PERM_EXPIRED`. Expiry events use executor `SYSTEM`.
    - ✅ Registered `neoessentials.permissions.user.temp` and `neoessentials.permissions.group.temp` in `PermissionRegistry`.
    - ✅ `PermissionSystem.md` updated: new **Temporary Permissions** section with duration table, command tables, resolution-order explanation, worked example, and audit-event table. Table of Contents updated.
    - ✅ `CommandsReference.md` updated: 6 new rows (`addtemp`/`removetemp`/`listtemp` for user and group) added to the Permissions Management table.

- **Permission Audit Logging** *(build #23)*
    - ✅ Created `PermissionAuditLogger.java` — persistent, append-only log written to `neoessentials/permissions_audit.log` (UTC timestamps, UTF-8). Each line records the timestamp, action type (padded for alignment), executor display name, target (group or player), and a detail string.
    - ✅ 17 action constants tracked: `USER_GROUP_SET`, `USER_PERM_ADDED`, `USER_PERM_REMOVED`, `USER_PERMS_CLEARED`, `GROUP_CREATED`, `GROUP_DELETED`, `GROUP_RENAMED`, `GROUP_CLONED`, `GROUP_PERM_ADDED`, `GROUP_PERM_REMOVED`, `GROUP_PERMS_CLEARED`, `GROUP_INHERIT_ADDED`, `GROUP_INHERIT_REMOVED`, `GROUP_PREFIX_SET`, `GROUP_SUFFIX_SET`, `GROUP_PRIORITY_SET`, `PERMISSIONS_RELOADED`.
    - ✅ `getExecutorDisplay()` helper added to `PermissionsCommand` — logs the player's name for in-game commands or `"CONSOLE"` for server-side execution.
    - ✅ `PermissionAuditLogger.log()` calls added after every successful permission modification in `PermissionsCommand.java` (all 16 mutation paths + reload).
    - ✅ Added `permissions.auditLogging` config key (default `true`) to `config.json`. `ConfigManager.isPermissionAuditEnabled()` public method added. When `false`, all log calls are no-ops.

- **Permission Groups & Priorities + Permission Suggestions** *(build #22)*
    - ✅ Added `priority` (int, default `0`) field to `PermissionGroup`. Higher priority groups are checked first during inheritance resolution — both the positive-grant and negative-deny passes sort inherited groups by `priority` descending before recursing.
    - ✅ `PermissionStorage` saves and loads `priority` in `permissions.json` (backwards-compatible — missing key defaults to `0`).
    - ✅ Added `/permissions group <name> setpriority <value>` (−999–999, requires `neoessentials.permissions.group.modify`) and `getpriority` (requires `neoessentials.permissions.info.group`) commands.
    - ✅ `/permissions info group` output now includes the group's current priority.
    - ✅ Registered `neoessentials.permissions.group.priority` in `PermissionRegistry`.
    - ✅ **Permission Suggestions** — `PermissionValidator.validatePermission()` and `validateAnyPermission()` denial messages now look up the required node(s) in `PermissionRegistry` and append the human-friendly description in a dimmed line (e.g. `§8(Ban a player from the server)`), so staff know exactly which capability they're missing without consulting the wiki.
    - ✅ `PermissionSystem.md` updated: new "Group Priorities" section with command table, priority scale, and worked example; example `groups.json` updated with priority values; ToC updated; denial-message format documented.
    - ✅ `CommandsReference.md` updated: `setpriority` and `getpriority` rows added to Permissions Management table.

- **Permission Debugging Tools** *(build #21)*
    - ✅ Added `/permissions debug <player>` subcommand (requires `neoessentials.permissions.debug`). Displays a full in-game diagnostic trace: system mode (internal / external adapter / emergency), adapter health and version, active config flags (`opsBypassPermissions`, `vanillaOpFallback`), OP status, assigned group, direct user permissions (up to 10 with overflow count), group inheritance chain (recursive with indentation, up to 8 permissions per group), and a numbered 4-step resolution chain summary showing exactly which step would GRANT or continue for that player.
    - ✅ Registered `neoessentials.permissions.debug` permission node in `PermissionRegistry` (between `check` and `search` nodes).
    - ✅ **Fixed** `checkUserPermission()` in `PermissionsCommand` was calling `PermissionAPI.getManager().hasPermission()` directly, silently bypassing the external adapter (LuckPerms / FTB Ranks), `opsBypassPermissions`, and `vanillaOpFallback`. Now calls `PermissionAPI.hasPermission()` — the full 5-step chain — so `/permissions user check` output matches actual runtime behaviour.

- **Documentation Update: allowUnsafeCommands Config** *(build #19)*
    - ✅ Fixed wrong `allowUnsafeCommands` description in `SplitConfigs.md` — it incorrectly said "Allow enchantments and item operations beyond vanilla limits" (that's `items.unsafe-enchantments`). Now accurately describes the command safety filter used by `/powertool`.
    - ✅ Added complete `security.json` reference table covering all six keys (`enableInputValidation`, `maxCommandLength`, `maxReasonLength`, `allowUnsafeCommands`, `enablePathTraversalProtection`, `enableXSSProtection`) with types, defaults, and descriptions.
    - ✅ Documented every blocked substring in the dangerous-pattern check (destructive ops, code-execution, path traversal, shell operators, URL injection, reflection) with explanations.
    - ✅ Documented the character allowlist and which common characters fall outside it (`@`, `{`, `%`, `=`, etc.).
    - ✅ Explicitly called out that tilde `~` (Minecraft relative coordinates, e.g. `/tp ~ 100 ~`) is blocked by default — the most common cause of the confusing "dangerous operations" error for powertool users.
    - ✅ Added tables of commands that work by default vs. commands requiring `allowUnsafeCommands: true`, with step-by-step enable instructions for both split-config and monolithic mode.
    - ✅ Added "Command Safety Filter" subsection to `ItemManagement.md` under Powertool — exact error messages, quick-reference tables, config locations, and cross-link to `SplitConfigs.md`.
    - ✅ Added warning callout on the `/powertool` row in `CommandsReference.md` naming the most common blocked patterns and linking to the full docs.
    - ✅ Added `security.json` to the getting-started key files list in `Home.md`.

- **Fallback to Vanilla OP Permissions** *(build #18)*
    - ✅ Added `permissions.vanillaOpFallback` config key (default `true`). After all permission systems (external adapter + internal manager) have been consulted and returned `false`, OPs (level 2+) are granted access as a last-resort safety net — distinct from `opsBypassPermissions` which skips checks entirely.
    - ✅ Permission system init failure no longer crashes the server with `RuntimeException`. Instead, `PermissionAPI.setEmergencyMode(true)` is activated: OPs get all permissions, non-OPs are denied, and a prominent boxed `ERROR` is logged at startup.
    - ✅ `/neoe reload` detects emergency mode and performs a full re-initialisation (resets manager, adapter, and all flags), allowing recovery without a server restart once the config issue is resolved.
    - ✅ `PermissionSystem.isEmergencyMode()` public accessor added.
    - ✅ `PermissionSystem.md` updated: new `vanillaOpFallback` config row, bypass-vs-fallback comparison table, and updated "How Permissions Work" numbered flow.
    - ✅ `vanillaOpFallback: true` added to the bundled `config.json` default template.

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

