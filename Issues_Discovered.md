# 👾 Issues That Were Discovered


---

# ✅ Issues That Were Fixed

- **NeoEssentials Permissions.json for Other Mods (NeoForge 1.21.1, All The Mons)**
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

- **Inventory Management Tools**  
  Add commands for:
    - Viewing other players’ inventories (with permissions).
    - Editing inventories and ender chests.
    - Audit logs for inventory changes to prevent abuse.

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

- **Utility Systems**  
  Ensure core utility commands are present and functional:
    - Nicknames, MOTD, `/near`, `/ping`, `/depth`.
    - Helpop, rules, suicide, and other essentials.
    - Consistent permission handling across all utilities.

- **API & Placeholder System**  
  Expand API and placeholder support:
    - Deeper PlaceholderAPI integration.
    - Ability to create custom placeholders.
    - REST API endpoints for external tools and dashboards.
    - Documentation for developers to extend NeoEssentials easily.

- **Permissions Fallback to OP**  
  Add a safeguard so that vanilla OP status is always respected:
    - Ensures operators retain access even if configs or FTB Ranks fail.
    - Prevents lockouts and reduces downtime.
    - Acts as a fallback layer when external permissions are misconfigured.
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
- **Documentation Update: allowUnsafeCommands Config**  
  Add a new section to the NeoEssentials documentation explaining the `allowUnsafeCommands` option.
    - Clarify where the option is located when configs are split into multiple JSON files.
    - Provide examples of usage and defaults.
    - Reduce confusion for server admins by making the config structure easier to follow.
    - Feedback from community (JJ {MrWhiteFlamesYT}, Chaz) highlighted that split JSON configs can be confusing, so clearer docs would help.
- **Fallback to Vanilla OP Permissions**  
  Add a feature so that NeoEssentials always respects vanilla OP status as a fallback, even if external ranks or configs fail.
    - Prevents lockouts when FTB Ranks or permissions.json misbehave.
    - Ensures server operators retain access to all commands without needing explicit nodes.
    - Reduces downtime and frustration when configs are corrupted or integrations fail.
# Improvements Done 

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
