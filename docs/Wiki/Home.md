# NeoEssentials Wiki

Welcome to the official documentation hub for **NeoEssentials v1.0.5** — a comprehensive NeoForge server essentials mod for Minecraft 1.21.1–1.21.10 (this is the primary development branch — see [`26.1.x`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/tree/26.1.x) for the pinned Minecraft 26.1-26.1.2 build, or [`26.2.x`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/tree/26.2.x) for the pinned Minecraft 26.2 build).

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
| [Hologram System](HologramSystem) | Animated `/hologram` displays, billboard/spin/hover, shop hologram click-to-trade, dashboard API |
| [Permission System](PermissionSystem) | All permission nodes, groups, wildcards, external mods |
| [Web Dashboard](WebDashboard) | Setup, login, dashboard features, Discord auth |
| [API & Placeholder System](APISystem) | PlaceholderAPI, Vault, custom placeholders, stat tokens |
| [Chat Channels](ChatChannels) | Channel config, permissions, Discord relay |
| [Split Config System](SplitConfigs) | Split config files, validation, repair, migration guide |
| [Tablist System](TablistSystem) | Animated header/footer, hex colors, gradients, per-group/per-player, PlaceholderAPI support |
| [Scoreboard System](ScoreboardSystem) | Config-driven sidebar boards, conditions, animation, per-group/per-player overrides, persisted toggle |
| [Leaderboard System](LeaderboardSystem) | Config-driven ranked boards (vanilla stats + economy + custom + shop sales), per-board styling/GUI, `LeaderboardAPI`, hologram generator |
| [Localization System](LocalizationSystem) | Server language selection, custom translations, language override commands |
| [Storage Backend](Storage) | Pluggable JSON/YAML/SQLite/MySQL storage covering the whole mod |
| [Dashboard Connectivity](DashboardConnectivity) | Internal vs. external dashboard hosting, `webDashboard.mode`, connection troubleshooting |
| [Logging System](Logging) | Per-subsystem `normal`/`debug` logging toggles, categories, `logs/debug.log` |

---

## 🚀 Getting Started

1. Drop `neoessentials-<version>.jar` into your server's `mods/` folder
2. Start the server — config files are auto-generated in `config/neoessentials/`
3. Key config files (split config mode — recommended):
   - `main.json` — modules, logging (see [Logging System](Logging)), storage backend selection, localization, permissions, kits settings, economy settings
   - `dashboard.json` — web dashboard port/auth/UI settings
   - `commands.json` — enable/disable individual commands
   - `chat.json` — chat formatting, channels, anti-spam
   - `teleportation.json` — homes, warps, spawn, TPA
   - `moderation.json` — ban, jail, freeze, kick
   - `tablist.json` — tablist header/footer/formatting
   - `security.json` — input validation, `allowUnsafeCommands` (command safety checks), command length limits
   - Player balances, permission groups/users, and kit definitions are **not** flat config files —
     they're persisted through the pluggable Storage Backend (JSON/YAML/SQLite/MySQL) instead;
     see [Storage Backend](Storage)
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

*NeoEssentials v1.0.5 · Minecraft 1.21.1–1.21.10 · NeoForge 21.1.179+ · build.54*
