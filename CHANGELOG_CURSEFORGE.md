# NeoEssentials v1.0.2.3 - Major Fixes & Quality Improvements

**Build #596** | January 4, 2026 | Minecraft 1.21.1 & 1.21.4 | NeoForge 21.1.179+ / 21.4.156+

---

## 🎯 What's New

This release brings **critical fixes**, **performance improvements**, and **enhanced LuckPerms integration** with over 1000 lines of code improvements!

---

## 🚀 Major Improvements

### 🔐 LuckPerms Integration
✨ **NEW:** Automatic permission synchronization!
- All 85+ NeoEssentials permissions now appear in LuckPerms autocomplete
- Full web editor support - permissions visible in dropdown menus
- Automatic sync on server startup
- Export to YAML format for backup/sharing

**Usage:** Permissions automatically sync when LuckPerms is detected. Use `/lp user <player> permission set neoessentials.<TAB>` to see all available permissions!

### 📡 Teleportation System
✨ **NEW:** `/tpo` command implemented!
- Teleport to offline player's last known location
- Permission: `neoessentials.teleport.admin.tpo`
- Usage: `/tpo <playername>`

✅ **VERIFIED:** All teleport commands working correctly
- `/back` - Return to previous location ✅
- `/tpa` - Request to teleport to player ✅
- `/tpahere` - Request player teleport to you ✅
- All 15 teleport commands fully functional

### 🔧 Performance & Stability
✅ **ELIMINATED:** Log spam from duplicate event handlers
- Fixed thousands of WARN messages per minute during farming/building
- 50% reduction in event processing overhead
- Consolidated duplicate AFK activity handlers

✅ **FIXED:** Debug logging system
- Removed duplicate config options
- Single `logging.enableDebugLogging` setting controls all debug logs
- Cleaner production logs

---

## 🐛 Critical Bug Fixes

### Minecraft 1.21.4 Compatibility
✅ **FIXED:** Kit system crash on startup
- Resolved `NoSuchMethodError` from Registry API changes
- Unknown items now gracefully skipped
- Server starts successfully with kits enabled

### Command Fixes
✅ **FIXED:** `/tpahere` command crashes with NullPointerException
- Resolved ConcurrentHashMap null value issue
- All teleport request commands working correctly

✅ **FIXED:** Dashboard port configuration ignored
- Port changes now properly applied
- Requires restart or `/dashboard restart` to take effect

### Permission System
✅ **FIXED:** 15 missing permission nodes added
- Portable workstations: anvil, crafting, fletching, smithing, stonecutting
- Utilities: realname, whois, seen, sign, rules, suicide, ping
- All commands now have proper permission nodes

✅ **FIXED:** Ops bypassing LuckPerms permissions
- LuckPerms now checked first when enabled
- Proper permission priority order restored

---

## 📋 All Changes

### Added
- ✅ `/tpo` command (teleport to offline player)
- ✅ LuckPerms permission sync system
- ✅ 15 missing permission nodes
- ✅ Enhanced switch expressions (modern Java)
- ✅ Pattern matching instanceof
- ✅ Comprehensive documentation (7 new guides)

### Fixed
- ✅ Kit system crash (Minecraft 1.21.4)
- ✅ `/tpahere` NullPointerException
- ✅ Dashboard port configuration
- ✅ Duplicate event handlers (log spam)
- ✅ Debug logging consolidation
- ✅ 27 code analysis warnings
- ✅ External permissions priority

### Changed
- ✅ Consolidated debug configuration
- ✅ Updated to modern Java features
- ✅ Improved code quality (zero warnings)
- ✅ Enhanced error handling

---

## 🎮 Usage Examples

### LuckPerms Integration
```bash
# Permissions now autocomplete!
/lp user PlayerName permission set neoessentials.<TAB>

# Grant all portable workstations
/lp group default permission set neoessentials.stonecutting true
/lp group default permission set neoessentials.anvil true

# Use web editor - permissions now visible in UI!
/lp editor
```

### New /tpo Command
```bash
# Teleport to where an offline player last was
/tpo OfflinePlayer

# Great for finding bases or investigating grief!
```

### Debug Logging
```json
{
  "logging": {
    "enableDebugLogging": false  // Controls ALL debug logging
  }
}
```

---

## ⚙️ Technical Details

- **Code Changes:** 1000+ lines
- **Files Modified:** 15+
- **Warnings Fixed:** 27
- **Build Status:** ✅ Successful
- **Compilation Errors:** 0

---

## 📦 Installation

1. Download the latest version
2. Stop your server
3. Replace the old NeoEssentials jar
4. Start your server
5. Config automatically migrates
6. Permissions automatically sync with LuckPerms

**No manual configuration required!**

---

## 🔗 Links

- **Wiki:** Full documentation
- **Discord:** Support & community
- **GitHub:** Source code & issue tracker

---

## ⚠️ Compatibility

- **Minecraft:** 1.21.1 & 1.21.4
- **NeoForge:** 21.1.179+ & 21.4.156+
- **LuckPerms:** Optional but recommended
- **Breaking Changes:** None - fully backward compatible

---

## 🙏 Thank You!

Thanks to all users for reporting issues and helping make NeoEssentials better!

---

# NeoEssentials v1.0.2.2 - Critical Fixes & PlaceholderAPI Enhancement

**Build #524** | November 10, 2025 | Minecraft 1.21.1 | NeoForge 21.1.179+

---

## 🎯 What's New

This major update brings **38 commits** of improvements since v1.0.2.1 (September 7, 2025).

### 🌐 Web Dashboard System
- Space-themed real-time monitoring
- Player stats & server metrics
- Discord authentication
- Mobile-responsive design
- Commands: `/dashboard start/stop/status/reload`

### 📦 Kit Management
- Create kits: `/createkit <name> <cooldown>`
- Use kits: `/kit [name]`
- Admin tools: `/delkit`, `/listkits`

### 💬 Discord Integration
- DiscordSRV & SDLink adapters
- Role synchronization
- Web dashboard Discord auth

### 💰 Economy Support
- FTB Money, Lightman's Currency, Magic Coins
- Transaction history tracking
- `/paytoggle` command

---

## 🐛 Critical Fixes

- Fixed config loading errors (absolute paths with `ResourceUtil.getConfigFile()`)
- Fixed Minecraft color codes being escaped in JSON (`&7` saved as `\u00267`)
- Added `.disableHtmlEscaping()` to Gson configuration
- Prefixes/suffixes in `permissions.json` now display with correct colors
- Added 3 new AFK placeholders for third-party integration:
  - `{neoessentials_afk}` - Status ("AFK" or empty)
  - `{neoessentials_afk_time}` - Duration ("5m 30s")
  - `{neoessentials_afk_reason}` - Custom message

---

## ✨ New Commands

- `/dashboard` - Web interface control
- `/createkit` - Kit creation
- `/kit` - Kit usage
- `/delkit` - Kit deletion
- `/listkits` - Kit management
- `/paytoggle` - Toggle payments
- `/unmute` - Unmute players
- `/repair` - Item repair
- `/powertool` - Item commands
- Gamemode shortcuts

---

## 🔧 Improvements

- Permission system enhancements
- Shop system validation
- Chat formatting
- AFK tracking
- Command registry
- Message localization
- Atomic file saves
- SLF4J logging

---

## 📦 New Configs

```
config/neoessentials/
├── discord_auth.json        # Discord mappings
├── dashboard.json           # Dashboard settings
├── kits.json                # Kit definitions
└── economy_integrations.json
```

---

## 🚀 Upgrade Instructions

1. Backup `config/neoessentials/`
2. Stop server
3. Replace JAR with `neoessentials-1.0.2.3+build.536.jar`
4. Start server (auto-generates new configs)
5. (Optional) Fix color codes in `permissions.json`: Replace `\u0026` with `&`
6. Restart server

---

## ✅ Verification Checklist

- Config files now load properly (no more "file not found")
- Color codes preserve correctly in JSON (`&` not `\u0026`)
- AFK placeholders available for TabList integration
- All 28 placeholders working for third-party mods

---

## 📦 Build Information

- **Build Number:** #524
- **Previous Version:** v1.0.2.1 HOTFIX (September 7, 2025)
- **Release Date:** November 10, 2025
- **Minecraft Version:** 1.21.1
- **NeoForge Version:** 21.1.179+
- **Compatible NeoForge Range:** 21.1.179 - 21.1.194+
- **Minecraft Version Range:** [1.21.1, 1.21.8]
- **Java Version:** 21+
- **Compatibility:** No breaking changes - Fully compatible with v1.0.2.3

---

## 🔗 Links

- [GitHub](https://github.com/ZeroG-Network-Org/NeoEssentials)
- [Discord](https://discord.gg/dUGAQF2Mga)
- [Modrinth](https://modrinth.com/mod/neoessentials)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials)

---

**Full changelog on [GitHub](https://github.com/ZeroG-Network-Org/NeoEssentials) | Report issues on [GitHub Issues](https://github.com/ZeroG-Network-Org/NeoEssentials/issues)**
