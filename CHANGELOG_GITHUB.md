# NeoEssentials v1.0.2.2-HotFix - Full Technical Changelog

**Build #524** | November 10, 2025 | Minecraft 1.21.1 | NeoForge 21.1.179+

---

## 🚀 Major Features & Systems

### 🌐 Web Dashboard v2.0
- Real-time server monitoring (space-themed UI)
- Player stats, live logs, Discord login integration
- Mobile-responsive design
- Commands: `/dashboard start/stop/status/reload`

### 📦 Kit System
- Create, use, and manage custom kits
- Cooldown system, usage tracking
- Commands: `/createkit`, `/kit`, `/delkit`, `/listkits`

### 💬 Discord Integration
- DiscordSRV & SDLink adapters
- Role-to-permission sync
- Web dashboard Discord authentication

### 💰 Economy Integrations
- FTB Money, Lightman's Currency, Magic Coins
- Transaction history tracking
- `/paytoggle` command

### 🛡️ Permission System
- Admin validation, reload, list commands
- Prefixes/suffixes with color code support

### 💤 AFK System
- Activity tracking, custom messages, admin controls
- 3 new PlaceholderAPI placeholders:
  - `{neoessentials_afk}` (status)
  - `{neoessentials_afk_time}` (duration)
  - `{neoessentials_afk_reason}` (custom message)

---

## 🐛 Critical Fixes

- Fixed config loading errors (absolute paths with `ResourceUtil.getConfigFile()`)
- Fixed Minecraft color codes being escaped in JSON (`&7` saved as `\u00267`)
- Added `.disableHtmlEscaping()` to Gson configuration
- Prefixes/suffixes in `permissions.json` now display with correct colors
- All 28 placeholders now work for third-party mods

---

## 🧩 Technical Improvements

- ManagerRegistry: centralized manager system
- SLF4J logging: better error tracking
- Dynamic command registration
- Message localization: 100+ new translation keys
- Atomic file operations for data integrity
- Command validation: permission checks across all commands

---

## 🗂️ New & Updated Config Files

```
config/neoessentials/
├── discord_auth.json        # Discord mappings
├── dashboard.json           # Dashboard settings
├── kits.json                # Kit definitions
└── economy_integrations.json
```

---

## 🛠️ Upgrade Instructions

1. Backup `config/neoessentials/`
2. Stop server
3. Replace JAR: `neoessentials-1.0.2.2-HotFix+build.524.jar`
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
- **Compatibility:** No breaking changes - Fully compatible with v1.0.2.2

---

## 🔗 Links & Documentation

- [GitHub Repository](https://github.com/ZeroG-Network-Org/NeoEssentials)
- [Discord Support](https://discord.gg/dUGAQF2Mga)
- [Modrinth](https://modrinth.com/mod/neoessentials)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials)
- [Permission System Guide](docs/PERMISSIONS_API_OVERVIEW.md)
- PlaceholderAPI: See `DefaultPlaceholderExpansion.java` for all 28 placeholders

---

**Full compatibility** with v1.0.2.1 | **No breaking changes** | Minecraft 1.21.1-1.21.8
