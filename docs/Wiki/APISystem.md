# API & Placeholder System

> **Version:** 1.0.5+build.54 · **API Version:** 1.2.0

---

## Overview

NeoEssentials exposes five integration surfaces for server admins and mod developers:

| Surface | What it provides |
|---|---|
| **Built-in Placeholders** | `{neoessentials_*}` tokens usable in chat, MOTD, tablist, join/quit messages, etc. |
| **Placeholder Java API** | Register custom tokens (`PlaceholderProvider`) or grouped expansions (`PlaceholderExpansion`) |
| **REST API** | HTTP endpoints for external tools, dashboards, and automation |
| **`NeoEssentialsAPI`** | Single stable entry-point exposing Economy, Permissions, and Placeholder services |
| **Vault API** | Standalone, dependency-free economy/permission/chat interop for *other NeoForge mods* — see [`docs/VaultAPI.md`](../VaultAPI) |

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
| `{neoessentials_balance_raw}` | — | Economy balance as a plain number with no trailing zeros |
| `{neoessentials_currency_symbol}` | — | Configured currency symbol (e.g. `$`) |
| `{neoessentials_baltop_rank}` | — | Player's rank on the `/baltop` leaderboard (`"N/A"` if unavailable) |
| `{neoessentials_pay_toggle}` | — | `"enabled"`/`"disabled"` — whether the player currently accepts `/pay` payments |
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

## REST API (External Dashboard)

> **The mod no longer ships its own dashboard UI.** `webDashboard.mode` is `"external"` —
> the embedded HTTP server on `config.json → webDashboard.port` (default `8080`) serves
> **REST only**; a separately-hosted dashboard app is the actual UI. Everything below is the
> integration surface for that app.
>
> **This section is a summary.** The full, exhaustive reference — every route across all ~29
> endpoint groups, real example request/response JSON pulled from a live server, quirks like
> binary downloads and non-UUID path segments, and the WebSocket protocol — lives in
> **[`docs/API.md`](../API.md)** at the repo root. That file is what an external dashboard
> developer should actually build against; treat this wiki section as an index into it.

### Authentication

Two credential types, both presented the same way — `Authorization: Bearer <token>` — tried in
this order by the server:

1. **API key** (recommended for a dashboard backend) — a long-lived, independently-revocable
   credential, never expiring on its own. The recommended way to get one connected is
   **pairing**, not manually running `/apikey create` and copy-pasting the token:

   #### Pairing

   1. On the external dashboard's own admin Configuration page, click "Generate Pairing Code" —
      it shows a one-time code (valid 10 minutes) and the exact command to run.
   2. On the Minecraft server console (or in-game, if OP), run the command it showed:
      ```
      /dashboard pair "<dashboardUrl>" <code>
      ```
      The URL must be quoted — unquoted command arguments can't contain `:` or `/`.
      Requires permission `neoessentials.dashboard.pair`.
   3. In that single round trip: the mod mints an API key and sends it to the dashboard (for the
      dashboard's own outbound calls to this API), and the dashboard mints a token back for the
      mod's outbound account-sync webhook (see below) — both directions connected, nothing
      hand-copied between two config files.

   `/dashboard unpair` clears the connection and revokes the key that was minted for it.

   The manual path still exists for scripting/one-off keys:
   ```
   /apikey create <label> [role]     # role: ADMIN (default) | OPERATOR | MODERATOR | VIEWER
   /apikey list
   /apikey revoke <id>
   ```
   The full token (`neo_<keyId>_<secret>`) is printed **exactly once** at creation — only its
   salted hash is stored, so it can't be recovered later, only revoked and re-created. Never put
   it in frontend/browser code; it belongs on the external dashboard's own server, which then
   calls this API on the browser's behalf. Also manageable over REST once a first key exists —
   `GET/POST /api/apikeys`, `DELETE /api/apikeys/{id}` (ADMIN-gated).
2. **Session token** — from `POST /api/auth/login` (username/password) or the Discord-linked
   flow — see [Web Dashboard](WebDashboard) for account registration. This is the mod's own
   human-login concept; an external dashboard backend calling this API as a service should
   generally prefer an API key instead.

Every route is gated as **PUBLIC** (no token needed — a small deliberate set: public moderation
lookup, `/api/docs/*`, `/api/ping`), **AUTH** (any valid key/session, any role), or **ADMIN**
(the credential's role must be `ADMIN`) — see `docs/API.md` for the exact tier of every route.

```bash
# Login (session-based)
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"secret"}'
# Response includes: {"success":true,"sessionId":"...", "user":{...}}

# Authenticated request — works identically with a session id or an API key
curl http://localhost:8080/api/placeholders/list \
     -H "Authorization: Bearer <sessionId-or-api-key>"
```

### Endpoint groups (see `docs/API.md` for full detail on each)

| Prefix | Covers |
|---|---|
| `/api/apikeys` | Manage API keys (see above) |
| `/api/auth` | Login/logout/session validation, dashboard-account CRUD |
| `/api/player` | Online/offline player info, kick/teleport/heal/gamemode |
| `/api/server`, `/api/game` | Server/world profile, performance, game events |
| `/api/logging` | Dashboard's own request/error/performance logs |
| `/api/admin` | Restart/stop/save/reload/broadcast |
| `/api/files` | Sandboxed config/log file browser, editor, backups |
| `/api/permissions` | Groups, users, aliases, node catalog — the internal permission system |
| `/api/motd`, `/api/rules` | MOTD profiles + rotation; server rules (both ADMIN-gated for writes) |
| `/api/teleport` | Teleport/home/warp/spawn/back settings |
| `/api/placeholders` | List/resolve/stats for the placeholder registry |
| `/api/shops` | Sign shops + NPC shops, CSV import/export (ADMIN-gated for writes) |
| `/api/backup` | Snapshot create/list/restore/delete/download |
| `/api/discord` | Bridge adapter status, event log, channel-ID test message, auth-config |
| `/api/cloud` | Dropbox/Google Drive backup upload config |
| `/api/users` | Dashboard account management + external-dashboard user sync (see below) |
| `/api/moderation`, `/api/public/moderation` | Bans/mutes/kicks/warns/notes/reports (public variant is unauthenticated, privacy-limited) |
| `/api/kits`, `/api/holograms`, `/api/warps` | Read/manage kits (read-only), holograms, warps (holograms/warps ADMIN-gated for writes) |
| `/api/commands` | Run arbitrary server commands (blocklist applies) |
| `/api/economy` | Balance lookups + admin give/take/set |
| `/api/stats` | Economy/activity/performance dashboards, 60-min history |
| `/api/docs` | The mod's own public in-game documentation content (unrelated to this API doc) |

A live event feed also runs on `websocketPort` (default `8081`) alongside REST — channels
`events`/`chat`/`stats`, authenticated the same way (session id or API key). Full protocol in
`docs/API.md`.

### Syncing dashboard accounts with an external user table

If the external dashboard has its own user accounts and wants a matching mod-side account to
exist (or vice versa), see the **"Keeping dashboard accounts in sync"** section under `/api/users`
in `docs/API.md` — covers `POST /api/users/sync` (idempotent create-or-update, for the external
dashboard pushing its accounts into the mod) and the outbound webhook fired whenever a
dashboard_user is created/updated/deleted here (in-game `/dashboardregister` included), so the
external dashboard can mirror it back out automatically. The webhook's target URL and auth token
are set automatically by pairing (see above), not hand-configured.

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
