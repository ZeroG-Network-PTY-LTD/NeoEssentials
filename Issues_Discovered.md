# 👾 Issues That Were Discovered
- **Languages EN, FR, DE, ES, ect. incomplete**: Some messages and features were not fully translated in all supported languages, alot of hardcoded English strings, want to allow custom language files.

# 👾 Issues That Were Fixed

- **Config File Splitting**: Split large config files into smaller, more manageable files for easier editing and maintenance.
    - **FIXED:** Implemented config splitting system
    - **FIXED:** Added `/neoessentials config split` command
    - **FIXED:** Automatic migration with backup creation
    - **FIXED:** Config.json is replaced with minimal stub file when split configs are active
        - Original config backed up to config.json.backup
        - Stub file contains helpful guide to all split config files
        - Includes restoration instructions
        - Prevents confusion about which files to edit
    - **FIXED:** 100% backward compatible - existing code works unchanged
    - **FIXED:** Seamless merging of split configs into single view
    - **Details:** Config.json (685 lines) can now be split into:
      - main.json (~50 lines) - modules, logging, permissions
      - commands.json (~110 lines) - command enable/disable
      - chat.json (~200 lines) - all chat settings
      - teleportation.json (~120 lines) - teleport settings
      - moderation.json (~130 lines) - ban, jail, freeze, kick, vanish
      - webdashboard.json (~80 lines) - web interface settings
      - items.json (~30 lines) - item spawn settings
      - afk.json (~40 lines) - AFK system
      - security.json (~15 lines) - security settings
    - Startup notification prompts admins about splitting option
    - Automatic version management per split file
    - Easy rollback via config.json.backup

- **Channels not working as expected**: There were issues with chat channels not functioning correctly, messages were not being sent to the intended recipients based on channel settings.
    - **FIXED:** Implemented channel commands (/local, /global, /staff, etc.)
    - **FIXED:** Added channel switching with `/l`, `/g`, `/staff` commands
    - **FIXED:** Messages now route to correct recipients based on channel
    - **FIXED:** Prefix support (! for global, @ for staff, etc.)
    - **FIXED:** Per-player channel state tracking
    - **FIXED:** Local channel respects radius setting
    - **FIXED:** Staff channel respects permission setting
    - **FIXED:** Discord integration now receives chat messages
        - Added `onPlayerChat()` method to ChatIntegrationAdapter interface
        - Implemented in DiscordSRV, SDLink, and DCIntegration adapters
        - Messages include channel indicator and emoji (💬 Local, 🌍 Global, 🛡️ Staff)
        - Minecraft formatting codes are stripped for Discord compatibility
        - Integration broadcasts to all registered Discord mod adapters
    - **FIXED:** Channels respect LuckPerms permissions
        - Permission-based channels check neoessentials permissions
        - Works with both internal and external permission systems (LuckPerms, etc.)
        - Staff channel requires `neoessentials.chat.staff` permission
        - Custom channels can define their own permission requirements
    - **FIXED:** Local radius works correctly
        - Checks same dimension requirement
        - Accurately calculates distance between players
        - Only sends to players within configured radius
    - **Details:** Config example:
    ```json
    {
      "chat": {
        "channels": {
          "enabled": true,
          "local": {
            "enabled": true,
            "radius": 100,
            "command": "l",
            "aliases": ["local", "lc"],
            "prefix": "",
            "default": true
          },
          "global": {
            "enabled": true,
            "command": "g",
            "aliases": ["global", "gc"],
            "prefix": "!",
            "default": false
          },
          "staff": {
            "enabled": true,
            "command": "staff",
            "aliases": ["mod", "admin", "s"],
            "prefix": "@",
            "permission": "neoessentials.chat.staff",
            "default": false
          }
        }
      }
    }
    ```

- **Config Files Issue**: When updating the config "_configVersion" in files it does not update the config files with old config files with new one when new version is available.
    - **FIXED:** Implemented config version checking system
    - **FIXED:** Automatic backup creation before updating
    - **FIXED:** Old configs are backed up with timestamp
    - **FIXED:** New config versions are automatically applied on server start
    - **Details:** When `_configVersion` is updated, the system:
      - Detects version mismatch
      - Creates backup: `config_v12_backup_2026-01-10_15-30-00.json`
- **Inventory See**: Ability to view other players' inventories, editable inventories, and ender chests, based on permissions.

    - **FIXED:** Implemented `/invseeedit <player>` command (view and edit inventory)
    - **FIXED:** Implemented `/enderchest <player>` command (view ender chest, read-only)
    - **FIXED:** Implemented `/enderchestedit <player>` command (view and edit ender chest)
    - **FIXED:** Permission-based access control
    - **FIXED:** Read-only mode creates inventory copy (no accidental edits)
    - **FIXED:** Editable mode directly accesses target's inventory
    - **FIXED:** Proper logging of all inventory viewing actions
    - **Details:** Commands and permissions:
      - `/invsee <player>` or `/inv <player>` - Permission: `neoessentials.invsee`
      - `/invseeedit <player>` - Permission: `neoessentials.invsee.edit`
      - `/enderchest <player>` or `/ec <player>` - Permission: `neoessentials.enderchest`
      - `/enderchestedit <player>` or `/ecedit <player>` - Permission: `neoessentials.enderchest.edit`
    - Safety features:
      - Cannot view own inventory (use 'E' key instead)
      - Read-only creates copy to prevent accidental changes
      - All actions are logged for audit trails
    - Web dashboard already had inventory viewing (read-only)

- **Web-dashboard improvements**: Backup/restore functionality, more detailed statistics, and better user management, Backup/Restore from online storage services (Google Drive, Dropbox, etc).


# 🎯 Additional Features

- **Economy integration**: Chest sign shops, Player Chest shops, Entity shops, dynamic pricing, CSV Dynamic pricing list import/export, and ect. more.
- **Holographic displays**: Support for holographic displays to show any information.
- **Chat formatting options**: More options for customizing chat format.
- **Inventory See**: Ability to view other players' inventories, editable inventories, and ender chests, based on permissions.
- **Minecraft Assets API support**: Figure out a way to integrate Minecraft Assets API for better resource assests to show in web-dashboards and other places.
- **Web-dashboard improvements**: Backup/restore functionality, more detailed statistics, and better user management, Backup/Restore from online storage services (Google Drive, Dropbox, etc).
- **Player Tablist**: Custom code for a custom player tab list that is highly customizable {References: Bungee Tablist Plus, TAB [1.7.x - 1.21.11], ☆ Simple TabList ☆《1.16.x - 1.21.x》- Animated - Hex colors}
- **Cofig Files Splitting**: Split large config files into smaller, more manageable files for easier editing and maintenance.
