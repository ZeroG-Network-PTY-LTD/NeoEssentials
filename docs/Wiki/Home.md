# NeoEssentials Wiki

Welcome to the official documentation hub for **NeoEssentials v1.0.2.6** — a comprehensive NeoForge server essentials mod for Minecraft 1.21.1.

---

## 📚 Wiki Pages

| Page | Description |
|---|---|
| [**Commands Reference**](CommandsReference) | Every command — syntax, permission, aliases |
| [Economy System](EconomySystem) | Balances, pay, baltop, ChestShop, Vault API |
| [Chat System](ChatSystem) | Formatting, channels, rich text, AFK broadcasts |
| [AFK System](AFKSystem) | Auto-AFK, kick, tablist indicator, config |
| [Moderation System](ModerationSystem) | Ban, mute, jail, freeze, vanish |
| [Teleportation System](TeleportationSystem) | Home, warp, TPA, RTP, spawn, safe teleport |
| [Kit Management](KitManagement) | Kits, cooldowns, give-to-others, kitreset |
| [Item Management](ItemManagement) | Repair, enchant, powertool, clearinv, condense |
| [Utility Systems](UtilitySystems) | Ptime, pweather, effects, spawnmob, unlimited, MOTD |
| [Permission System](PermissionSystem) | All permission nodes, groups, wildcards, external mods |
| [Web Dashboard](WebDashboard) | Setup, login, dashboard features, Discord auth |
| [API & Placeholder System](APISystem) | PlaceholderAPI, Vault, custom placeholders |
| [Chat Channels](ChatChannels) | Channel config, permissions, Discord relay |
| [**Split Config System**](SplitConfigs) | Split config files, validation, repair, migration guide |

---

## 🚀 Getting Started

1. Drop `neoessentials-<version>.jar` into your server's `mods/` folder
2. Start the server — config files are auto-generated in `config/neoessentials/`
3. Key config files (split config mode — recommended):
   - `main.json` — modules, logging, permissions, kits settings, economy settings
   - `commands.json` — enable/disable individual commands
   - `chat.json` — chat formatting, channels, anti-spam
   - `teleportation.json` — homes, warps, spawn, TPA
   - `moderation.json` — ban, jail, freeze, kick
   - `tablist.json` — tablist header/footer/formatting
   - `security.json` — input validation, `allowUnsafeCommands` (powertool filter), XSS protection
   - `webdashboard.json` — web dashboard port and auth
   - `economy.json` — player balances (runtime data)
   - `permissions.json` — groups and permission nodes
   - `kits.json` — kit definitions
   - See [Split Config System](SplitConfigs) for the complete reference
4. Assign permissions to players/groups via `/permissions` or LuckPerms/FTBRanks

---

## ⚙️ Optional Dependencies

| Mod | Purpose |
|---|---|
| **LuckPerms** | External permission management (auto-detected) |
| **FTB Ranks** | Alternative external permission management |
| **Simple Discord Link** | Discord ↔ Minecraft auth and chat relay |

All optional — NeoEssentials runs fully standalone without any of them.

---

## 💬 Support

- [Discord](https://discord.gg/dUGAQF2Mga)

---

*NeoEssentials v1.0.2.6 · Minecraft 1.21.1 · NeoForge 21.1.179+*
