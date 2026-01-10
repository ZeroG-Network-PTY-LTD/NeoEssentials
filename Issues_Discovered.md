# 👾 Issues That Were Discovered



# 👾 Issues That Were Fixed

- **Config File Splitting**: Split large config files into smaller, more manageable files for easier editing and maintenance.
    - **FIXED:** Implemented config splitting system
    - **FIXED:** Added `/neoessentials config split` command
    - **FIXED:** Automatic migration with backup creation
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
    - **Details:** Config example:
    ```json
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
    ```

- **Config Files Issue**: When updating the config "_configVersion" in files it does not update the config files with old config files with new one when new version is available.
    - This causes issues where new features or settings are not applied because the old config file is still in use.


# 🎯 Additional Features

- **Economy integration**: Chest sign shops, Player Chest shops, Entity shops, dynamic pricing, CSV Dynamic pricing list import/export, and more.
- **Holographic displays**: Support for holographic displays to show any information.
- **Chat formatting options**: More options for customizing chat format.
- **Inventory See**: Ability to view other players' inventories, editable inventories, and ender chests, based on permissions.
- **Minecraft Assets API support**: Figure out a way to integrate Minecraft Assets API for better resource assests to show in web-dashboards and other places.
- **Web-dashboard improvements**: Backup/restore functionality, more detailed statistics, and better user management, Backup/Restore from online storage services (Google Drive, Dropbox, etc).
- **Player Tablist**: Custom code for a custom player tab list that is highly customizable {References: Bungee Tablist Plus, TAB [1.7.x - 1.21.11], ☆ Simple TabList ☆《1.16.x - 1.21.x》- Animated - Hex colors}
- **Cofig Files Splitting**: Split large config files into smaller, more manageable files for easier editing and maintenance.