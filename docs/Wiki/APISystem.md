# API & Placeholder System

> **Version:** 1.0.2.6

---

## Overview

NeoEssentials exposes a placeholder system, a Vault API integration, a REST API via the web dashboard, and a custom language file system — all usable by server admins and other mods.

---

## Built-in Placeholders

Placeholders work in chat formats, MOTD, join/quit messages, tablist header/footer, and any config value that supports them.

| Placeholder | Value |
|---|---|
| `{player}` | Player's real username |
| `{displayname}` | Player's nickname or real name |
| `{prefix}` | Permission group prefix |
| `{suffix}` | Permission group suffix |
| `{group}` | Player's primary permission group name |
| `{balance}` | Player's current balance (formatted) |
| `{world}` | Current world/dimension name |
| `{x}` | Player X coordinate |
| `{y}` | Player Y coordinate |
| `{z}` | Player Z coordinate |
| `{ping}` | Player's connection latency in ms |
| `{online}` | Number of online players |
| `{max}` | Server max player slots |
| `{tps}` | Server TPS (ticks per second) |
| `{time}` | Real-world server time (HH:mm) |
| `{server_name}` | Server name from `server.properties` |
| `{newline}` | Line break (tablist header/footer) |
| `{bar}` | Horizontal separator bar |

---

## PlaceholderAPI (Custom Placeholders)

Register custom placeholders from your own code using the `PlaceholderManager`:

```java
// Register a single placeholder
PlaceholderManager.registerPlaceholder("my_placeholder", player -> "Hello " + player.getName().getString());

// Register an expansion (multiple related placeholders)
PlaceholderManager.registerExpansion(new MyExpansion());
```

Implement `PlaceholderExpansion` to group related placeholders under a common prefix (e.g. `{mymod_stat1}`, `{mymod_stat2}`).

---

## Vault API

NeoEssentials registers as a Vault Economy, Chat, and Permission provider automatically on server start.

### Economy API

```java
// Via Vault (standard Vault usage)
Economy eco = VaultHook.getEconomy();
eco.depositPlayer(offlinePlayer, 100.0);
eco.withdrawPlayer(offlinePlayer, 50.0);
double balance = eco.getBalance(offlinePlayer);
```

### NeoEssentials Direct API

```java
// Direct access (no Vault needed)
EconomyManager eco = EconomyManager.getInstance();
eco.addBalance(uuid, 100.0);
eco.subtractBalance(uuid, 50.0);
double balance = eco.getBalance(uuid);
```

### Permission API

```java
// Check permission
boolean has = PermissionAPI.hasPermission(uuid, "neoessentials.fly");

// Get player prefix/suffix
String prefix = PermissionAPI.getPrefix(uuid);
String suffix = PermissionAPI.getSuffix(uuid);
```

### Economy Events

Listen to economy events on the NeoForge event bus:

| Event | Fires when |
|---|---|
| `EconomyDepositEvent` | Balance is increased |
| `EconomyWithdrawEvent` | Balance is decreased |

Both events are cancellable and expose `getPlayer()`, `getAmount()`, and `getNewBalance()`.

---

## REST API (Web Dashboard)

All endpoints require authentication via the `Authorization: Bearer <token>` header unless otherwise noted. Enable in `config.json` → `webDashboard.apiSettings.enableApiEndpoints`.

### Auth

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Log in with username + password, returns session token |
| `POST` | `/api/auth/logout` | Invalidate session token |
| `GET` | `/api/auth/status` | Check if current session is valid |

### Players

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/player/online` | List all online players (UUID, name, ping, world, coords) |
| `GET` | `/api/player/{uuid}` | Get detailed info for a player by UUID |

### Server

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/server/status` | Online count, version, TPS, uptime |
| `GET` | `/api/server/performance` | Memory usage, TPS history, chunk count |
| `GET` | `/api/server/worlds` | All loaded dimensions with player counts |
| `GET` | `/api/server/statistics` | Aggregate server stats |

### Logs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/logs` | Latest N log lines (N configured via `logLinesToReturn`) |

### Admin

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/command` | Execute a server command (requires `dashboard.manage`) |
| `GET` | `/api/config/{file}` | Read a config file (requires `dashboard.admin`) |
| `PUT` | `/api/config/{file}` | Write a config file (requires `dashboard.admin` + `allowConfigEditing: true`) |

---

## Custom Language System

NeoEssentials supports full internationalisation with per-server language overrides.

### Bundled Languages

`en_us`, `fr_fr`, `de_de`, `es_es`, `pt_br`, `zh_cn`, `nl_nl`, `pl_pl`, `ru_ru`

All files are auto-deployed to `config/neoessentials/languages/custom/` on first start.

### Switching Language

In `config.json`:
```json
"language": "fr_fr"
```

Or in-game:
```
/language fr_fr
/language reload
```

### Custom / Overriding Translations

Edit any file in `config/neoessentials/languages/custom/<lang>.json`. Changes are preserved across mod updates — new keys from the JAR are merged in without overwriting your edits.

### Adding a New Language

1. Create `config/neoessentials/languages/custom/xx_xx.json`
2. Copy all keys from `en_us.json` and translate values
3. Run `/language reload` to apply

### Lang Key Format

```json
{
  "_langVersion": 10,
  "commands.neoessentials.home.teleported": "§aTeleported to home §e{0}§a.",
  "commands.neoessentials.home.not_found": "§cHome §e{0}§c not found."
}
```

`{0}`, `{1}`, … are positional `MessageFormat` arguments substituted at runtime.

---

*Back to [Wiki Home](Home)*
