# NeoEssentials v1.0.2.2-HotFix - Major Update

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
3. Replace JAR: `neoessentials-1.0.2.2-HotFix+build.524.jar`
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
