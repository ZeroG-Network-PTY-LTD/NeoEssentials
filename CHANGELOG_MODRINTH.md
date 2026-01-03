# NeoEssentials v1.0.2.3 - Quality & Performance Update

**Build #596** | January 4, 2026 | MC 1.21.1 & 1.21.4 | NeoForge 21.1.179+ / 21.4.156+

---

## 🎯 Highlights

**Major quality update** with critical fixes and LuckPerms integration!

---

## 🆕 New Features

### 🔐 LuckPerms Auto-Sync
Your NeoEssentials permissions now automatically appear in LuckPerms!

**What you get:**
- ✨ Autocomplete in `/lp` commands
- ✨ Permissions visible in web editor
- ✨ 85+ permissions auto-registered
- ✨ Export to YAML format

**Try it:**
```bash
/lp user PlayerName permission set neoessentials.<TAB>
# All permissions now autocomplete! 🎉
```

### 📡 `/tpo` Command
Teleport to offline player's last location!

```bash
/tpo OfflinePlayerName
```

Perfect for:
- Finding player bases
- Investigating grief
- Admin duties

---

## 🐛 Major Fixes

### Build #575 - Kit System Crash
❌ **Error:** `NoSuchMethodError` on Minecraft 1.21.4  
✅ **Fixed:** Updated to new Registry API  
🎯 **Result:** Server starts successfully with kits

### Build #580 - Log Spam Eliminated
❌ **Problem:** Thousands of WARN logs per minute  
✅ **Fixed:** Removed duplicate event handlers  
🎯 **Result:** 50% less event processing, clean logs

### Build #585 - Permission Nodes
❌ **Missing:** 15 utility command permissions  
✅ **Added:** All portable workstations + utilities  
🎯 **Result:** Full LuckPerms integration

### Build #590 - TPA System
❌ **Crash:** `/tpahere` NullPointerException  
✅ **Fixed:** ConcurrentHashMap null handling  
🎯 **Result:** All teleport requests working

### Build #595 - Debug Logging
❌ **Duplicate:** Two debug config options  
✅ **Merged:** Single `logging.enableDebugLogging`  
🎯 **Result:** Consistent debug control

---

## 📋 Complete Changes

### Added ✨
- `/tpo` command for offline teleports
- LuckPerms automatic permission sync
- 15 missing permission nodes:
  - `neoessentials.anvil`
  - `neoessentials.crafting`
  - `neoessentials.stonecutting`
  - `neoessentials.smithing`
  - `neoessentials.fletching`
  - `neoessentials.realname`
  - `neoessentials.whois`
  - `neoessentials.seen`
  - `neoessentials.sign`
  - `neoessentials.rules`
  - `neoessentials.suicide`
  - `neoessentials.ping`
  - And more!

### Fixed 🔧
- Kit system crash (MC 1.21.4 Registry API)
- `/tpahere` NullPointerException
- Dashboard port config ignored
- Duplicate event handlers (log spam)
- Ops bypassing LuckPerms
- 27 code analysis warnings
- Debug logging consolidation

### Improved 📈
- 50% event processing reduction
- Modern Java features (switch expressions, pattern matching)
- Zero compilation warnings
- Enhanced error handling
- Thread-safe field declarations

---

## 🎮 Quick Start

### Using New Features

**LuckPerms Integration:**
```bash
# Autocomplete now works!
/lp user Steve permission set neoessentials.<TAB>

# Grant portable workstations
/lp group default permission set neoessentials.stonecutting
/lp group default permission set neoessentials.anvil

# Use web editor - NeoEssentials permissions now visible!
/lp editor
```

**Offline Teleport:**
```bash
# Teleport to where an offline player last was
/tpo BuilderBob
```

**Clean Logs:**
```json
// config.json
{
  "logging": {
    "enableDebugLogging": false  // Clean production logs
  }
}
```

---

## ⚙️ Technical

**Performance:**
- 50% reduction in AFK event processing
- Eliminated duplicate handler registration
- Optimized permission checking order

**Code Quality:**
- 0 compilation warnings
- 1000+ lines improved
- 15+ files updated
- Modern Java 16+ features

**Compatibility:**
- Minecraft 1.21.1 ✅
- Minecraft 1.21.4 ✅
- NeoForge 21.1.179+ ✅
- NeoForge 21.4.156+ ✅
- LuckPerms (optional) ✅

---

## 📦 Installation

1. Download latest version
2. Stop server
3. Replace old jar
4. Start server
5. **Done!** Auto-migrates config

No breaking changes - fully backward compatible!

---

## 📚 Documentation

**New Guides Created:**
- Debug Logging Consolidation
- Enhanced Handler Fix
- Teleportation System Guide
- TPA/TPAHERE Implementation
- LuckPerms Integration
- Permission Registry Guide
- Missing Permissions Fix

All available in GitHub repository!

---

## 🔗 Useful Links

- **Issues:** Report bugs on GitHub
- **Wiki:** Full command reference
- **Discord:** Community support
- **Source:** Available on GitHub

---

## 🎉 What's Next?

**Planned for v1.0.2.4:**
- Additional performance optimizations
- More system audits
- Enhanced features

---

## ⭐ Support the Project

Enjoying NeoEssentials? Consider:
- ⭐ Starring on GitHub
- 📝 Writing a review
- 🐛 Reporting bugs
- 💡 Suggesting features

---

## 📊 Stats

- **Build:** #596
- **Date:** January 4, 2026
- **Changes:** 1000+ lines
- **Fixes:** 27 warnings + 8 bugs
- **New Features:** 3 major

---

# NeoEssentials v1.0.2.2 - Major Update

**Build #524** | November 10, 2025 | Minecraft 1.21.1 | NeoForge 21.1.179+

---

## 🎯 Highlights

**38 commits** with major features since v1.0.2.1 (Sept 7, 2025):

### 🌐 Web Dashboard System
- Space-themed real-time monitoring interface
- Player stats, server metrics, live logs
- Discord login integration
- Mobile-responsive design
- Commands: `/dashboard start/stop/status/reload`

### 📦 Kit Management
- Create custom kits from inventory
- Cooldown system & usage tracking
- Commands: `/createkit`, `/kit`, `/delkit`, `/listkits`

### 💬 Discord Integration
- DiscordSRV & SDLink adapters
- Role-to-permission sync
- Web dashboard Discord auth

### 💰 Economy Integrations
- FTB Money, Lightman's Currency, Magic Coins
- Transaction history tracking
- `/paytoggle` command

---

## 🐛 Critical Fixes

### Build #520 - Config Loading
✅ Fixed: `config.json not found` error  
🔧 Solution: Absolute paths with `ResourceUtil.getConfigFile()`

### Build #521 - Color Codes
✅ Fixed: `&7` saved as `\u00267`  
🔧 Solution: Added `.disableHtmlEscaping()` to Gson

### Build #523 - AFK Placeholders
🆕 Added 3 new placeholders:
- `{neoessentials_afk}` - Status ("AFK" or empty)
- `{neoessentials_afk_time}` - Duration ("5m 30s")
- `{neoessentials_afk_reason}` - Custom message

---

## ✨ New Features

### Commands
- `/dashboard` - Web dashboard control
- `/createkit <name>` - Create kits
- `/kit [name]` - Use/list kits
- `/delkit <name>` - Delete kits
- `/listkits` - Admin kit management
- `/paytoggle` - Toggle payments
- `/unmute <player>` - Unmute players
- `/repair [all]` - Repair items
- `/powertool [cmd]` - Item command binding
- Gamemode shortcuts (survival, creative, etc.)

### Systems
- **Permission System:** Enhanced with admin validation, reload, list commands
- **Shop System:** Item tag support, dynamic linking, better validation
- **Chat System:** Custom formatting, clickable components, debug utilities
- **AFK System:** Activity tracking, custom messages, admin controls

---

## 🔧 Improvements

- **ManagerRegistry** - Centralized manager system
- **SLF4J Logging** - Better error tracking
- **Dynamic Commands** - Config-based registration
- **Message Localization** - 100+ new translation keys
- **Atomic File Operations** - Better data integrity
- **Command Validation** - Permission checks across board

---

## 📦 New Config Files

```
config/neoessentials/
├── discord_auth.json        # Discord mappings
├── dashboard.json           # Dashboard settings
├── kits.json               # Kit definitions
└── economy_integrations.json
```

---

## 🚀 Upgrade

1. Backup `config/neoessentials/`
2. Stop server
3. Replace JAR: `neoessentials-1.0.2.3+build.536.jar`
4. Start server (auto-generates new configs)
5. Optional: Fix color codes in `permissions.json` (`\u0026` → `&`)

---

## ✅ Verification

```bash
# Check logs
[ChatManager] Loaded chat-format (object): default=[...]

# Test features
/dashboard start
/createkit starter 3600
/afk Testing
```

---

## 📊 Stats

- 38 commits
- 15+ new commands
- 100+ translation keys
- 28 total placeholders
- 3 economy integrations

---

## 🔗 Links

- [GitHub](https://github.com/ZeroG-Network-Org/NeoEssentials)
- [Discord](https://discord.gg/dUGAQF2Mga)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials)
- [Modrinth](https://modrinth.com/mod/neoessentials)

---

**Full compatibility** with v1.0.2.1 | **No breaking changes** | Minecraft 1.21.1-1.21.8
