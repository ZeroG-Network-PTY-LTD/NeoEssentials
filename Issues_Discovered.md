---
---
#  Issues That Were Discovered

*(No open issues at this time — all discovered issues have been resolved.)*

---

# ✅ Issues That Were Fixed

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

