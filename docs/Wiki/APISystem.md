# API & Placeholder System

> **Version:** 1.0.2.6 · **API Version:** 1.2.0

---

## Overview

NeoEssentials exposes four integration surfaces for server admins and mod developers:

| Surface | What it provides |
|---|---|
| **Built-in Placeholders** | `{neoessentials_*}` tokens usable in chat, MOTD, tablist, join/quit messages, etc. |
| **Placeholder Java API** | Register custom tokens (`PlaceholderProvider`) or grouped expansions (`PlaceholderExpansion`) |
| **REST API** | HTTP endpoints for external tools, dashboards, and automation |
| **`NeoEssentialsAPI`** | Single stable entry-point exposing Economy, Permissions, and Placeholder services |

---

## Built-in Placeholders

All NeoEssentials placeholders use the `neoessentials` expansion prefix.
Syntax: `{neoessentials_<identifier>}`.
Short-form aliases (legacy, no prefix) are also supported in most contexts.

### Player Placeholders

| Placeholder | Short-form | Description |
|---|---|---|
| `{neoessentials_name}` | `{player}` | Player's real username |
| `{neoessentials_displayname}` | — | Display name (nick or real name) |
| `{neoessentials_prefix}` | `{prefix}` | Permission group prefix |
| `{neoessentials_suffix}` | `{suffix}` | Permission group suffix |
| `{neoessentials_group}` | `{group}` | Primary permission group name |
| `{neoessentials_balance}` | `{balance}` | Economy balance (raw number) |
| `{neoessentials_balance_formatted}` | — | Economy balance (formatted string) |
| `{neoessentials_world}` | `{world}` | Current dimension name |
| `{neoessentials_x}` | `{x}` | Player X coordinate |
| `{neoessentials_y}` | `{y}` | Player Y coordinate |
| `{neoessentials_z}` | `{z}` | Player Z coordinate |
| `{neoessentials_biome}` | — | Current biome name |
| `{neoessentials_health}` | — | Current health |
| `{neoessentials_max_health}` | — | Maximum health |
| `{neoessentials_food}` | — | Food/saturation level |
| `{neoessentials_level}` | — | Experience level |
| `{neoessentials_exp}` | — | Experience progress (0–100 %) |
| `{neoessentials_gamemode}` | — | Gamemode (survival / creative / …) |
| `{neoessentials_ping}` | `{ping}` | Connection latency (ms) |

### Stat Placeholders

| Placeholder | Description |
|---|---|
| `{neoessentials_deaths}` | Total death count |
| `{neoessentials_player_kills}` | Total player kills |
| `{neoessentials_mob_kills}` | Total mob kills |
| `{neoessentials_play_time}` | Total play time (e.g. `3d 2h 15m`, `45m 30s`) |

### AFK Placeholders

| Placeholder | Description |
|---|---|
| `{neoessentials_afk}` | `"AFK"` when the player is AFK, empty string otherwise |
| `{neoessentials_afk_time}` | Time spent AFK (e.g. `"5m 30s"`) |
| `{neoessentials_afk_reason}` | AFK reason text (if set) |

### Server-wide Placeholders

| Placeholder | Short-form | Description |
|---|---|---|
| `{neoessentials_online_players}` | `{online}` | Number of online players |
| `{neoessentials_max_players}` | `{max}` | Maximum player slots |
| `{neoessentials_server_name}` | `{server_name}` | Server MOTD / name |
| `{neoessentials_time}` | `{time}` | Server time (12-hour format) |
| `{neoessentials_time_24}` | — | Server time (24-hour format) |
| `{neoessentials_date}` | — | Current date (`yyyy-MM-dd`) |
| `{neoessentials_tps}` | `{tps}` | Server TPS |

### Utility Placeholders

| Placeholder | Description |
|---|---|
| `{newline}` | Line break (tablist header / footer only) |
| `{bar}` | Horizontal separator bar |

---

## Custom Placeholders (Java API)

### 1. Single placeholder — `PlaceholderProvider`

`PlaceholderProvider` is a public `@FunctionalInterface` you can implement with a lambda or class.

```java
import com.zerog.neoessentials.api.PlaceholderAPI;
import com.zerog.neoessentials.api.NeoEssentialsAPI;

// Via static facade (simplest):
PlaceholderAPI.registerPlaceholder("mymod_kills", (player, params) ->
    player != null ? String.valueOf(MyStats.getKills(player.getUUID())) : "0"
);

// Via PlaceholderManager directly:
NeoEssentialsAPI.getPlaceholderManager()
    .registerPlaceholder("mymod_kills", (player, params) -> "42");
```

`PlaceholderProvider.onRequest(ServerPlayer player, String params)`:
- `player` — the `ServerPlayer` context, **may be null** for server-wide resolution
- `params` — the part after `:` in `{mymod_stat:some_param}` — may be null

Return `null` to leave the original token unchanged in the output string.

### 2. Expansion — multiple placeholders under one prefix

Extend `PlaceholderExpansion` to register many related placeholders at once.
Expansion id `"mymod"` → placeholders `{mymod_kills}`, `{mymod_deaths}`, `{mymod_playtime}`.

> **Important:** the expansion identifier must be lowercase, alphanumeric, and contain **no underscores**.
> Resolution splits a placeholder token on its first `_` to separate the expansion id from the
> placeholder name (`{mymod_kills}` → id `mymod`, placeholder `kills`), so an id like `"my_mod"`
> would never match — `{my_mod_kills}` would look for an expansion named `"my"`.

```java
import com.zerog.neoessentials.api.PlaceholderExpansion;
import com.zerog.neoessentials.api.PlaceholderAPI;
import net.minecraft.server.level.ServerPlayer;
import java.util.Set;

public class MyModExpansion extends PlaceholderExpansion {

    @Override public String getIdentifier() { return "mymod"; }
    @Override public String getVersion()    { return "1.0.0"; }
    @Override public String getAuthor()     { return "YourName"; }

    @Override
    public Set<String> getPlaceholders() {
        return Set.of("kills", "deaths", "playtime");
    }

    @Override
    public String onPlaceholderRequest(ServerPlayer player, String identifier, String params) {
        if (player == null) return null;
        return switch (identifier) {
            case "kills"    -> String.valueOf(MyStats.getKills(player.getUUID()));
            case "deaths"   -> String.valueOf(MyStats.getDeaths(player.getUUID()));
            case "playtime" -> MyStats.getFormattedPlaytime(player.getUUID());
            default         -> null;
        };
    }
}

// Register (call during ServerStartingEvent or your mod's init):
PlaceholderAPI.registerExpansion(new MyModExpansion());
```

### 3. Resolving placeholders in text

```java
import com.zerog.neoessentials.api.PlaceholderManager;

PlaceholderManager pm = NeoEssentialsAPI.getPlaceholderManager();

// Resolve all placeholders in a string
String formatted = pm.setPlaceholders(player, "Hello {neoessentials_name}, kills: {mymod_kills}!");

// Resolve a single placeholder value
String value = pm.getPlaceholderValue(player, "mymod_kills", null);

// Check if a placeholder is registered
boolean exists = pm.isPlaceholderRegistered("mymod_kills");

// List all registered identifiers
Set<String> all = pm.getRegisteredPlaceholders();

// Unregister
pm.unregisterPlaceholder("mymod_kills");
PlaceholderAPI.unregisterExpansion(myExpansionInstance);
```

`PlaceholderManager` is thread-safe (`ConcurrentHashMap`). All operations are safe to call from async threads.

---

## NeoEssentialsAPI — Full Reference

The `NeoEssentialsAPI` class is the single stable entry-point for inter-mod integration.

```java
import com.zerog.neoessentials.api.NeoEssentialsAPI;

// Check availability before calling (useful if NeoEssentials is optional)
if (NeoEssentialsAPI.isAvailable()) { ... }

// API version (SemVer string, e.g. "1.2.0")
String version = NeoEssentialsAPI.API_VERSION;
```

### Economy API

```java
import com.zerog.neoessentials.api.economy.EconomyService;

EconomyService eco = NeoEssentialsAPI.getEconomyService();
eco.deposit(uuid, 100.0);
eco.withdraw(uuid, 50.0);
double balance   = eco.getBalance(uuid);
boolean hasEnough = eco.getBalance(uuid) >= 30.0;   // no has(uuid, amount) helper — compare getBalance()
boolean hasAcct   = eco.hasAccount(uuid);
```

#### Economy Events (NeoForge event bus)

| Event | Fires when | Cancellable |
|---|---|---|
| `EconomyDepositEvent` | Balance increased | ✅ |
| `EconomyWithdrawEvent` | Balance decreased | ✅ |

Both events extend `EconomyEvent` and expose `getPlayerId()`, `getAmount()` (double), and `getBigDecimalAmount()` (precise `BigDecimal`). They implement `ICancellableEvent` — cancel the event to veto the deposit/withdrawal.

### Permissions API

```java
import com.zerog.neoessentials.api.permissions.PermissionsService;

PermissionsService perms = NeoEssentialsAPI.getPermissionsService();

boolean canFly  = perms.hasPermission(playerUUID, "neoessentials.fly");
String  group   = perms.getGroup(playerUUID);
String  prefix  = perms.getPrefix(playerUUID);
String  suffix  = perms.getSuffix(playerUUID);

// Register your mod's permission nodes (visible in /permissions search + dashboard)
perms.registerPermission("mymod.feature", "Enables my mod's cool feature");

// Register legacy alias
perms.registerAlias("oldmod.fly", "neoessentials.fly");
```

### Placeholder API (quick reference)

```java
import com.zerog.neoessentials.api.PlaceholderManager;

PlaceholderManager pm = NeoEssentialsAPI.getPlaceholderManager();
pm.registerPlaceholder("mymod_online", (player, params) ->
    String.valueOf(server.getPlayerCount()));
String resolved = pm.setPlaceholders(player, "Players: {mymod_online}");
```

See the [Custom Placeholders](#custom-placeholders-java-api) section above for the full API.

### Versioning contract

NeoEssentials follows **SemVer** for API changes:

| Change type | Description |
|---|---|
| **PATCH** | Bug fixes only, fully backward compatible |
| **MINOR** | New methods / classes added, backward compatible |
| **MAJOR** | Breaking changes (rare, announced in advance) |

Guard version-specific feature use:

```java
String[] parts = NeoEssentialsAPI.API_VERSION.split("\\.");
int minor = Integer.parseInt(parts[1]);
if (minor >= 2) {
    // getPlaceholderManager() is available since 1.2.0
    NeoEssentialsAPI.getPlaceholderManager().registerPlaceholder("...", ...);
}
```

---

## REST API (Web Dashboard)

All endpoints require authentication via `Authorization: Bearer <token>` unless otherwise noted.
Obtain a token from `POST /api/auth/login`. Enable in `config.json` → `webDashboard`.

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | No | Log in with username + password → returns session token |
| `GET` | `/api/auth/discord` | No | Log in with a Discord-linked Minecraft account (`?username=`) → returns session token |
| `POST` | `/api/auth/logout` | Yes | Invalidate session token |
| `GET` | `/api/auth/validate` | Yes | Check if current session is valid |

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"secret"}'
# Response: {"token":"abc123..."}

# Authenticated request
curl http://localhost:8080/api/placeholders/list \
     -H "Authorization: Bearer abc123..."
```

### Placeholders

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/placeholders/list` | All registered placeholder identifiers (sorted) |
| `GET` | `/api/placeholders/resolve?player=<name>&text=<str>` | Resolve placeholders server-side |
| `GET` | `/api/placeholders/stats` | Registry statistics (total count, expansion count) |

`player` parameter in `/resolve` is optional (must be online if provided).

### Players

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/player/online` | List all online players (UUID, name, ping, world, coords) |
| `GET` | `/api/player/{uuid}` | Detailed info for a player by UUID |

### Server

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/server/status` | Online count, version, TPS, uptime |
| `GET` | `/api/server/performance` | Memory, TPS history, chunk count |
| `GET` | `/api/server/worlds` | All loaded dimensions with player counts |

### Logs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/logging` | Latest N log lines (N configured per dashboard settings) |

### Admin

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/admin/command` | Execute a server command (`dashboard.manage` required) |
| `GET` | `/api/files` | Browse / read config files (`dashboard.admin` required) |

### Permissions

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/permissions/overview` | Permission system overview |
| `GET/PUT` | `/api/permissions/groups` | List / manage permission groups |
| `GET/PUT` | `/api/permissions/users` | List / manage user permissions |

### Teleport Settings

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/teleport/settings` | Read all teleport config sections |
| `PUT` | `/api/teleport/settings` | Write teleport config + live-reload managers |

### Documentation (public — no auth)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/docs/sections` | All documentation sections |
| `GET` | `/api/docs/api` | API endpoint documentation |
| `GET` | `/api/docs/tutorials` | Step-by-step tutorials |
| `GET` | `/api/docs/faq` | Frequently asked questions |
| `GET` | `/api/docs/search?q=<query>` | Search across all docs |

---

## In-Game Admin Commands

### `/placeholder`

Manage and test the placeholder system in-game.

**Permission:** `neoessentials.admin.placeholders` (default: OP-only)

| Sub-command | Description |
|---|---|
| `/placeholder list` | List all registered placeholder identifiers |
| `/placeholder info <id>` | Check if an identifier is registered (tab-completes known IDs) |
| `/placeholder test <text>` | Resolve placeholders in `<text>` using your player context |
| `/placeholder stats` | Show registry statistics |

---

## Custom Language System

NeoEssentials supports full internationalisation with per-server language overrides.

### Bundled Languages

`en_us`, `fr_fr`, `de_de`, `es_es`, `pt_br`, `zh_cn`, `nl_nl`, `pl_pl`, `ru_ru`

All files are available in the JAR and auto-deployed to `neoessentials/languages/custom/` when first selected.

### Switching Language

In `config.json` under the `localization` section:

```json
"localization": {
  "language": "fr_fr"
}
```

Then reload in-game:

```
/neoessentials reload
```

> **Fallback:** Any key not translated in the chosen language file falls back to English automatically.

### Custom / Overriding Translations

Edit any file in `neoessentials/languages/custom/<lang>.json`.
Changes are preserved across mod updates — new keys from the JAR are merged in without overwriting your edits.

### Adding a New Language

1. Create `neoessentials/languages/custom/xx_xx.json`
2. Copy all keys from `en_us.json` and translate values
3. Set `"language": "xx_xx"` in `config.json → localization`
4. Run `/neoessentials reload` to apply

### Lang Key Format

```json
{
  "_langVersion": 17,
  "commands.neoessentials.home.teleported": "§aTeleported to home §e{0}§a.",
  "commands.neoessentials.home.not_found": "§cHome §e{0}§c not found."
}
```

`{0}`, `{1}`, … are positional `MessageFormat` arguments substituted at runtime.

See [Localization System](LocalizationSystem) for full documentation.

---

*Back to [Wiki Home](Home)*
