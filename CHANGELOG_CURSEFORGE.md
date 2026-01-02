# NeoEssentials v1.0.2.3 - Critical Fixes & PlaceholderAPI Enhancement

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
