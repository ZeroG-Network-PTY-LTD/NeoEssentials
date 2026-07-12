# Storage Backend

> **Version:** 1.0.3+build.10 · **Config:** `config.json` → `storage` section

---

## Overview

Almost everything NeoEssentials persists — moderation records, economy balances, homes/warps/spawn, kits, chat state, holograms, shops, permissions, the dashboard's own accounts, and the Auction House — goes through a single pluggable **DataStore** abstraction instead of each manager writing its own bespoke JSON file. One config setting picks the backend for the whole mod:

```json
"storage": {
  "type": "json",
  "autoMigrate": true,
  "sqlite": { "file": "data.db" },
  "mysql": { "host": "localhost", "port": 3306, "database": "neoessentials", "username": "neoessentials", "password": "", "useSSL": false, "poolSize": 10 }
}
```

| Key | Default | Description |
|---|---|---|
| `type` | `"json"` | Backend to use: `json`, `yaml`, `sqlite`, or `mysql` |
| `autoMigrate` | `true` | On first boot with an empty collection, import that manager's existing legacy JSON data automatically and losslessly |
| `sqlite.file` | `"data.db"` | SQLite database filename (under `neoessentials/store/`) |
| `mysql.*` | — | Connection details for a shared MySQL database — point every server in a network at the same database to share bans, economy, permissions, etc. in real time |

If `type` is `mysql` but the connection fails at boot, the mod automatically falls back to the JSON backend instead of failing to start — fix `storage.mysql.*` and restart to pick MySQL back up.

With the default `json` backend, each collection lives at `neoessentials/store/<collection>.json`. With `yaml`, `neoessentials/store/<collection>.yml`. With `sqlite`, every collection is a table in `neoessentials/store/data.db`. With `mysql`, every collection is a table in the configured database.

---

## How Migration Works

Each manager keeps its old bespoke JSON file(s) around and, on first boot after updating, checks whether its DataStore collection is still empty. If it is (and `autoMigrate` is enabled), it imports the legacy file(s) once — losslessly, preserving IDs, history, and audit trails where applicable — and from then on reads/writes exclusively through DataStore. **Legacy files are never deleted automatically**; they're simply no longer written to, so you can always cross-check or roll back manually if needed.

Switching `storage.type` later (e.g. from `json` to `mysql` for a multi-server setup) re-triggers this same import path for any collection that's empty in the new backend — so you can migrate from JSON straight to MySQL in one step, not just from legacy files to JSON first.

---

## Dev-Environment Caveat: `sqlite`/`mysql`/`yaml` Backends Need the Built Jar

The `sqlite`, `mysql`, and `yaml` backends depend on bundled libraries (`sqlite-jdbc`, `mysql-connector-j` + HikariCP, and SnakeYAML respectively) that are shaded into the mod via JarJar. NeoForge's Gradle `runServer` dev task loads each mod through a per-mod module classloader that only sees dependencies from the **packaged** mod jar (`build/generated/jarJar/...`), not the plain Gradle classpath — so running `./gradlew runServer` with `storage.type` set to anything other than `json` will fail to load the driver class (`ClassNotFoundException`) and log an error, even though the dependency is correctly bundled for a real install.

This only affects local development via `runServer` — installing the built mod jar on an actual server resolves the bundled dependency normally, same as it always has for `sqlite-jdbc` (used directly by the Auction House before this release). If a manager's one-time legacy-file migration hits this in a dev session, it logs the error and continues normally on whichever backend is actually active — nothing crashes, and the migration will succeed once run from a real packaged jar.

---

## Collections by System

| System | Collections | Wiki page |
|---|---|---|
| Moderation | `player_bans`, `ip_bans`, `mutes`, `ip_mutes`, `kicks`, `warns`, `notes`, `reports`, `jails`, `jail_locations`, `freezes`, `vanishes` | [Moderation System](ModerationSystem) |
| Economy | `economy_balances`, `pay_toggles`, `transaction_history`, `item_worth` | [Economy System](EconomySystem) |
| Kits | `kits`, `kit_cooldowns`, `kit_usages` | [Kit Management](KitManagement) |
| Teleportation | `warps`, `player_warps`, `spawn`, `playerdata_homes`, `playerdata_back_locations` | [Teleportation System](TeleportationSystem) |
| Chat | `afk_data`, `ignore_lists`, `chat_formats` | [Chat System](ChatSystem) |
| Holograms | `holograms` | [Hologram System](HologramSystem) |
| Shops | `chest_shops`, `npc_shops` | [Utility Systems](UtilitySystems) |
| Auction House | `auction_listings`, `auction_expired` | [Utility Systems](UtilitySystems) |
| Permissions | `permission_groups`, `permission_users`, `permission_meta`, `permission_aliases` | [Permission System](PermissionSystem) |
| Localization | `language_overrides` | [Localization System](LocalizationSystem) / [Custom Languages](CUSTOM_LANGUAGES) |
| Utility | `motd_profiles`, `motd_meta`, `resource_packs` | [Utility Systems](UtilitySystems) |
| Web Dashboard | `dashboard_users`, `dashboard_registrations` | [Web Dashboard](WebDashboard) |

`playerdata_homes` and `playerdata_back_locations` are one record per player (id = UUID) rather than the old one-file-per-player-per-type layout under `neoessentials/playerdata/<type>/<uuid>.json` — both old and new per-player layouts migrate automatically the same way as every other collection.

---

## Notes on Specific Systems

- **Permissions** (`permission_groups`/`permission_users`) preserve group inheritance, priorities, temp permissions, contextual permissions, and per-node conditions exactly as before — this is the one system where `save()` replaces the *entire* current state rather than writing one record at a time, since deleting a group or resetting a user has to actually remove its record from the backend, not just stop referencing it.
- **Auction House** (`auction_listings`/`auction_expired`) no longer opens its own dedicated SQLite database (`auctionhouse.db`) — it now uses the same backend as everything else. A listing keeps the same numeric ID when it moves from active to expired, matching the previous behavior; existing `auctionhouse.db` data is imported automatically on first boot.
- **Vanish**: the "who can see vanished players" viewer-priority toggle is intentionally session-only (not persisted) — only the vanished-state itself survives a restart.

---

*Back to [Wiki Home](Home)*
