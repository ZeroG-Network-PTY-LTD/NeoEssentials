# NeoEssentials — Changelog

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.2.6+build.18 — 2026-04-01

### 🛡️ New — Fallback to Vanilla OP Permissions

- **Permissions** — New `permissions.vanillaOpFallback` (default `true`): OPs get access as a last resort after all permission systems return false. Prevents admin lockouts from corrupted configs or crashing external perm mods.
- **Permissions** — Permission system init failure no longer crashes the server — activates emergency OP-only mode with a clear console error. `/neoe reload` exits emergency mode without a restart.
- **Docs** — `PermissionSystem.md` updated with new config option, bypass-vs-fallback comparison, and updated permission-check flow.

---

## 1.0.2.6+build.17 — 2026-04-01

### 🔌 Improved — External Permissions Integration

- **Permissions** — FTB Ranks and LuckPerms adapters now detect mod version at startup and log it; a boxed `WARN` fires when the installed FTB Ranks version is newer than last tested.
- **Permissions** — New `AdapterCompatibilityChecker` prints a formatted compatibility table at startup listing every detected permission mod, its version, and ✓/⚠ status.
- **Permissions** — FTB Ranks adapter probes four API signatures so it survives version bumps without breaking.
- **Permissions** — Both adapters track consecutive failures; after 5 failures they are marked unhealthy and `PermissionAPI` falls back to the internal `permissions.json` manager and then OP-bypass — no player lockouts from a broken external perm mod.

---

## 1.0.2.6+build.16 — 2026-04-01

### ✨ New Features — Rules Command

- **Rules** — Console now logs a detailed boxed error with file path and fix steps when `rules_data.json` is corrupt or missing. Auto-generated defaults are logged with their exact path and edit instructions.
- **Rules** — `/neoe reload` now reloads server rules.
- **Rules** — Dashboard API `/api/rules` for full CRUD on server rules (list, add, edit, delete, replace all, reload from disk).
- **Docs** — Full `/rules` section added to `UtilitySystems.md`; fixed stray `/rules`, `/helpop`, `/suicide` rows in the MOTD API table.

---

## 1.0.2.6+build.15 — 2026-04-01

### 🐛 Bug Fixes

- **Split Configs** — Fixed split files never being created on fresh installs (JAR source was wrong).
- **Split Configs** — Fixed `main.json` being overwritten with only one section.
- **Split Configs** — Added missing `economy` section to `main.json`.

### ✨ New Features

- `/neoe config validate` — validate all split config files.
- `/neoe config repair` — auto-fix missing or incomplete split config files.
- `/neoe config status` — visual overview of config file health.
- Clear boxed startup errors when split files are missing with `/neoe config repair` hint.
- New `SplitConfigs.md` wiki documentation.

---

## 1.0.2.6+build.12 — 2026-04-01


### ✨ New Features

- **MOTD – Multi-Profile Support** — Multiple named MOTD profiles with `/motd profile list|create|delete|switch|info`. Each profile persists independently in `config/neoessentials/motd_data.json`.
- **MOTD – Auto-Rotation** — Cycle profiles automatically: `/motd rotation enable <minutes>|disable|next`.
- **MOTD – Dashboard API** — REST endpoint `/api/motd` for full profile & rotation management from the web dashboard.
- **MOTD – Error Feedback** — In-game error messages on save/load failures; `/motd reload` shows the exact problem.

### 🔒 New Permissions
- `neoessentials.motd.profile` (default: off)
- `neoessentials.motd.rotation` (default: off)

---

## 1.0.2.6+build.5 — 2026-04-01


### 🐛 Bug Fixes

- **Config** – Fixed `ClassCastException` crash in all kit commands when split configs are enabled. Also fixed `getConfig("chat")` throwing `FileNotFoundException`, which broke chat formatting across the mod.
- **Permissions** – Fixed OPs being denied commands when an external permission mod (FTB Ranks, LuckPerms) is installed. OP bypass is now checked before any external adapter.
- **Permissions** – Fixed FTB Ranks adapter `NoSuchMethodException` on FTB Ranks 2101.1.x (new API `getPermission(ServerPlayer, String, boolean)` now probed automatically).
- **ChestShop** – Fixed Admin Shops with `?` item — any admin with `neoessentials.shop.create.admin` can now right-click to assign the item.
- **Help** – Fixed `/help 2` and other page numbers showing "No command found" (conflict with vanilla `/help` resolved).
- **Moderation** – Fixed `/unban` not detecting vanilla-issued bans; NeoEssentials now syncs with `banned-players.json` in both directions.
- **Rules** – Fixed `/rules` ignoring existing `rules.json` from older builds; auto-migrates to `rules_data.json`.
- **MOTD** – Fixed MOTD resetting on restart due to an inconsistent save path.

---

## 1.0.2.6+build.1 — 2026-03-06

### 🔁 Starting fresh from 1.0.2.6

This is the first build of the `1.0.2.6` release series. Build number reset to 1.

**Included from 1.0.2.5 series:**
- Sign-based ChestShop system with admin shops, auto-fill item (`?`), and buy/sell via right/left-click
- Vault API (Economy, Chat, Permission providers)
- Dedicated `tablist.json` config file with group colours, placeholders, and animation support
- 50+ new commands across Player Info, World/Fun, Teleport, Item/Misc, Utility, Admin, and Player State systems
- Random teleport (`/tpr`) — even distribution, nether-aware, async cache, named zones, biome exclusions
- Timed jails with auto-release, full event enforcement (respawn, teleport, interact, attack)
- Offline pay, async baltop with pagination and total wealth, percent eco amounts
- 8 new languages (FR, DE, ES, PT-BR, ZH-CN, NL, PL, RU) — auto-deployed and merged on start
- 50+ permission nodes registered, new MODERATION category, denial messages show required node
- Teleportation safe-location detection fully rewritten (slabs, stairs, glass, etc. now safe)
- AFK system fully wired to config, score thresholds fixed, broadcasts and personal feedback working
- Chat messages now appear in server console
- PowerTool fixes — fires on block right-clicks, `/powertooltoggle` now works correctly
- Rich text (gradients/rainbow) rendering fixed
- Dashboard offline login, register command, and file auto-update fixed
- ~120 missing translation keys added and auto-merged on load
