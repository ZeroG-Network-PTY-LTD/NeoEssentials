# NeoEssentials Dashboard API

This is the REST API the NeoEssentials Minecraft mod exposes for external tools — most
importantly, the external web dashboard. **The mod no longer ships its own dashboard UI.**
This API is the only integration surface; everything a dashboard needs (players, economy,
permissions, moderation, backups, Discord bridge status, server control, etc.) goes through it.

> **Building another NeoForge mod (not a web dashboard) that needs economy/permission/chat
> interop instead?** See [`docs/VaultAPI.md`](VaultAPI.md) — a separate, much smaller
> standalone Java API for that, unrelated to the HTTP surface documented below.

- Base URL: `http://<server-host>:<dashboardPort>/api/...` (default port `8090`, configurable —
  see `webDashboard.port` in `config.json`).
- All responses are JSON. Almost every response includes a top-level `"success": true/false`
  field; treat its absence as `true` for legacy 200 responses that predate this convention, and
  always check HTTP status first.
- **Transport security is out of scope for the mod.** It speaks plain HTTP. If this API needs to
  be reachable over the public internet, put a reverse proxy (nginx, Caddy, Cloudflare Tunnel,
  etc.) in front of it to terminate TLS — do not expose port 8090 directly.

## Authentication

Every route below is gated by one of three tiers. All three are enforced by the same
`Authorization: Bearer <token>` header — the *value* of the token determines which of the three
credential types below it's checked against, tried in this order:

1. **Session ID** — from a human login via `POST /api/auth/login` (see "Human login" below).
2. **API key** — a long-lived, non-expiring credential for a trusted backend service (the
   external dashboard's own server). See "API keys" below — **this is what you actually want**
   for building an external dashboard; sessions are for the mod's own login UI concept, not for
   a service integration.
3. Legacy Minecraft-account tokens (in-game `/login`-style flows) — present for backwards
   compatibility, not relevant to a new external integration.

Whichever credential is used, the request is tagged as one of:

| Tier | Meaning |
|---|---|
| **PUBLIC** | No `Authorization` header needed at all. Only a small, deliberate set of routes (public moderation lookup, docs). |
| **AUTH** | Any valid session or API key, regardless of role. Read-mostly routes. |
| **ADMIN** | The authenticated identity's role must be `ADMIN`. Write/control routes — kicking players, changing config, managing money, etc. |

An API key's role (`ADMIN` / `OPERATOR` / `MODERATOR` / `VIEWER`) is set at creation time and
determines whether it satisfies ADMIN-gated routes. **For an external dashboard that needs full
control (the stated goal — "control the server and mods and players etc externally"), create
the key with role `ADMIN`.**

### API keys — the integration credential you actually want

Created and managed in-game by a server operator (not self-service, not exposed to players) —
this keeps the mod fully authoritative over who can control it externally, and means the mod
keeps working standalone with zero dashboard involvement.

```
/apikey create <label> [role]     # role defaults to ADMIN if omitted
/apikey list
/apikey revoke <id>
```

`create` prints the full token **exactly once** — it is never stored or shown again (only its
salted hash is persisted, same PBKDF2 scheme as dashboard user passwords). Copy it straight into
the external dashboard's own server-side config/secrets store.

**Never put this token in frontend/browser code.** It belongs on the external dashboard's own
backend, which then proxies authenticated calls to this mod's API — exactly the same
"service-account" shape a previous Laravel-based dashboard used here. A key never expires on its
own; revoke it (`/apikey revoke <id>`) the moment it's no longer needed or you suspect it leaked.

Once at least one ADMIN-role key exists, keys can also be managed over REST by an
already-authenticated ADMIN caller (session or another API key) — useful for rotation without
console access:

| Method | Path | Tier | Body | Response |
|---|---|---|---|---|
| GET | `/api/apikeys` | ADMIN | — | `{"success":true,"keys":[{"id","label","role","createdAt","lastUsedAt","enabled"}...]}` (secret never included) |
| POST | `/api/apikeys` | ADMIN | `{"label":"...", "role":"ADMIN"}` (`role` optional, defaults `ADMIN`) | `{"success":true,"token":"neo_...","message":"..."}` — token shown once, same as the command |
| DELETE | `/api/apikeys/{id}` | ADMIN | — | `{"success":true,"message":"API key revoked"}` |

### Human login (session-based) — `/api/auth`

This is the mod's own dashboard-account system (username/password, stored in the mod's own
storage backend, distinct from Minecraft accounts). Not the recommended path for a service
integration, but documented since some routes may still assume a "current user" concept (e.g.
audit-log attribution).

| Method | Path | Tier | Body | Notes |
|---|---|---|---|---|
| POST | `/api/auth/login` | PUBLIC | `{"username","password"}` | Returns `{"success","sessionId","user":{...}}`. Also sets a `sessionId` cookie. |
| POST | `/api/auth/logout` | AUTH | — | Invalidates the current session. |
| GET | `/api/auth/validate` | AUTH | — | Confirms the token is still good; returns the user object. |
| GET | `/api/auth/session` | — | — | Same as validate but reads the session from cookie, not header. |
| POST | `/api/auth/change-password` | AUTH | `{"oldPassword","newPassword"}` | Self-service password change. |
| POST | `/api/auth/link-minecraft/start` | AUTH\* | `{"username"}` (API-key callers only) | Generates a short code; the player types `/linkaccount <code>` in-game to complete the link. Fails if this account already has one. |
| GET | `/api/auth/link-minecraft/status` | AUTH\* | — (`?username=` for API-key callers) | Returns `{"linked","mcUuid","mcUsername"}` for the resolved user. |
| POST | `/api/auth/unlink-minecraft` | AUTH\* | `{"username"}` (API-key callers only) | Self-service — clears the resolved user's linked Minecraft account, no code needed. |
| GET | `/api/auth/discord-status` | AUTH\* | — (`?username=` for API-key callers) | Whether the resolved user's linked Minecraft account is also linked to Discord via whichever companion bot (SDLink etc.) is installed — read-only, this mod never performs Discord OAuth2 itself. |

\* These four accept **either** a same-origin session cookie (used by the bundled internal
dashboard — always acts on the calling user, no `username` needed) **or** a valid API key Bearer
token plus an explicit `username` (used by the external dashboard, which has no mod-side session
of its own — it passes its own logged-in user's `mod_username`).
| GET/POST/PUT/DELETE | `/api/auth/users*` | ADMIN | — | Manage dashboard user accounts (separate from Minecraft players and from API keys). |

### Rate limiting

Applies to every route (including PUBLIC ones), per source IP, before auth is even checked.
Configurable via `webDashboard.securitySettings.enableRateLimiting` /
`webDashboard.securitySettings.maxRequestsPerMinute` in `config.json`. Exceeding it returns
`429` with `Retry-After: 60` and `{"success":false,"error":"Rate limit exceeded. Max N requests/min."}`.
A single external-dashboard backend proxying many end-users through one shared IP/key should
size its own request pattern with this in mind, or ask the operator to raise the limit.

## Server-side configuration (`config.json` → `webDashboard`)

The operator (not the external dashboard) controls these:

```json
"webDashboard": {
  "enabled": true,
  "autoStart": true,
  "port": 8090,
  "websocketPort": 8091,
  "bindAddress": "0.0.0.0",
  "securitySettings": {
    "requireAuthentication": true,
    "enableRateLimiting": true,
    "maxRequestsPerMinute": 60,
    "publicModerationLookupEnabled": true
  },
  "mode": "external",
  "externalDashboard": { "url": "", "token": "", "keyId": "" }
}
```

- `externalDashboard` is populated automatically by `/dashboard pair` (see "Pairing" above) —
  don't hand-edit it. `url`/`token` are used for the outbound user-sync webhook;
  `keyId` lets `/dashboard unpair` cleanly revoke the API key that was minted for this connection.

- `mode: "external"` is what disables the mod's own bundled dashboard UI at `/` — REST-only, as
  intended now that there's no internal dashboard. Keep it set to `"external"`.
- `bindAddress: "0.0.0.0"` means it listens on every network interface. If the reverse proxy
  terminating TLS runs on the same machine, consider binding to `127.0.0.1` instead and letting
  only the proxy reach it.

## WebSocket (`websocketPort`, default 8091)

A live event stream runs alongside the REST API on a separate port
(`ws://<server-host>:8091`) — useful for a live dashboard feed instead of polling REST
endpoints. Text-frame JSON messages both ways.

**Auth (send first, before subscribing):**
```json
{ "type": "authenticate", "apiKey": "neo_..." }
```
(or `{"type":"authenticate","sessionId":"..."}` for a human session — API key is the
recommended path for an external dashboard backend, same credential as REST). Response:
```json
{ "type":"authenticated", "username":"apikey:my-dashboard", "role":"ADMIN", "timestamp":... }
```
or `{"type":"auth_error","message":"Invalid or revoked API key"}`.

**Subscribe to channels:**
```json
{ "type": "subscribe", "channels": ["events", "chat", "stats"] }
```
`{"type":"unsubscribe","channels":[...]}` to stop. `{"type":"ping"}` → `{"type":"pong","timestamp":...}`
keepalive. Unauthenticated clients get an error if they try to subscribe (`"Authentication required."`).
Per-connection message rate limit: 1 message / 100ms.

**Channels:**

| Channel | Payload shape | Fired on |
|---|---|---|
| `events` | `{"type":"event","event":"player_join"\|"player_leave"\|"chat"\|"death","player":"...","message":"..."}` | Player join/leave/chat/death |
| `chat` | `{"type":"chat","player":"...","message":"..."}` | Every chat message (undecorated) |
| `stats` | `{"type":"stats","tps":20.0,"memUsedMb":...,"memMaxMb":...,"memPercent":...,"players":...,"playersMax":...,"uptimeMs":...}` | Every ~60s |

Every broadcast message also gets `channel` and `timestamp` fields added automatically server-side.

---

<!-- ENDPOINT CATALOG BEGINS HERE — one ## section per path prefix -->

## /api/apikeys

**Handler:** `ApiKeyEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/ApiKeyEndpoint.java`

**Every route is ADMIN.** See the "API keys" section above for the full picture (this is the REST-convenience twin of the in-game `/apikey` command). Duplicated here only for the table's sake:

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/apikeys` | ADMIN | List keys (never includes the secret) |
| POST | `/api/apikeys` | ADMIN | Create `{label, role?}` → token shown once |
| DELETE | `/api/apikeys/{id}` | ADMIN | Revoke |

## /api/player

**Handler:** `PlayerEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/api/endpoints/PlayerEndpoint.java`

All GET routes take a **username** (not UUID) as the path segment and internally resolve it to a UUID via the online player list — so every profile/stats/etc. GET requires the player to be **online** (returns `404 {"error":"Player not found"}` otherwise). GET responses are collected on the server thread with a 10s timeout. The four POST actions are admin-only and require the target to be online.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/player/online` | AUTH | List online (and some offline) players |
| GET | `/api/player/profile/{username}` | AUTH | Profile (uuid, gamemode, operator, online) |
| GET | `/api/player/stats/{username}` | AUTH | Vanilla statistics |
| GET | `/api/player/achievements/{username}` | AUTH | Advancements/achievements |
| GET | `/api/player/inventory/{username}` | AUTH | Inventory contents |
| GET | `/api/player/status/{username}` | AUTH | Status summary |
| GET | `/api/player/health/{username}` | AUTH | Health/food |
| GET | `/api/player/xp/{username}` | AUTH | XP level/progress |
| GET | `/api/player/location/{username}` | AUTH | Position + dimension |
| GET | `/api/player/homes/{username}` | AUTH | Player's homes (requires online) |
| GET | `/api/player/lookup/{username}` | AUTH | Player lookup (online + offline data) |
| POST | `/api/player/kick/{username}` | ADMIN | Kick an online player |
| POST | `/api/player/gamemode/{username}` | ADMIN | Change gamemode |
| POST | `/api/player/teleport/{username}` | ADMIN | Teleport to player or coords |
| POST | `/api/player/heal/{username}` | ADMIN | Full heal + feed |

**POST `/api/player/teleport/{username}`** — body is either `targetUsername` OR x/y/z (+ optional world):
```json
{ "x": 100, "y": 64, "z": -200, "world": "minecraft:overworld" }
```
```json
{ "targetUsername": "Steve" }
```
Response: `{"success":true,"message":"Notch teleported to 100.0, 64.0, -200.0"}`

**POST `/api/player/gamemode/{username}`** — body `{"gamemode":"creative"}` (survival|creative|adventure|spectator; defaults to survival). Response: `{"success":true,"message":"Notch's game mode is now creative"}`

**POST `/api/player/kick/{username}`** — body `{"reason":"..."}` (optional). Kick is also recorded in KickManager so it shows in `/api/moderation/kicks`. Response: `{"success":true,"message":"Notch was kicked: ..."}`. Player-not-online returns 400.

**GET `/api/player/online`** — returns two arrays:
```json
{
  "online": { "count": 1, "players": [
    { "uuid":"...", "username":"Notch", "displayName":"Notch", "ping":42,
      "gameMode":"survival", "health":20.0, "maxHealth":20.0, "foodLevel":20,
      "experienceLevel":30, "x":10.5,"y":64.0,"z":-3.2,
      "dimension":"minecraft:overworld", "operator":true } ] },
  "offline": { ... }
}
```

**GET `/api/player/homes/{username}`**:
```json
{ "homes":[ {"name":"base","x":10.0,"y":64.0,"z":-3.0,"yaw":0.0,"pitch":0.0,
  "dimension":"minecraft:overworld","createdBy":"Notch","timestamp":1700000000000} ],
  "count":1, "maxHomes":5 }
```

**GET `/api/player/profile/{username}`** (online): `{"uuid","username","displayName","online":true,"gameMode","operator"}`.

---

## /api/server

**Handler:** `ServerEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/api/endpoints/ServerEndpoint.java`

GET-only, all AUTH tier. Note `/api/server/profile` is special-cased: even if collection produces an `error` field it still returns 200.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/server/profile` | AUTH | Name, MOTD, versions, difficulty, maxPlayers, pvp |
| GET | `/api/server/performance` | AUTH | Current TPS + avg tick time |
| GET | `/api/server/statistics` | AUTH | Server statistics |
| GET | `/api/server/status` | AUTH | Server status |
| GET | `/api/server/health` | AUTH | Health metrics |
| GET | `/api/server/worlds` | AUTH | Loaded worlds/dimensions |
| GET | `/api/server/config` | AUTH | Server config summary |
| GET | `/api/server/assets` | AUTH | All namespace asset catalog |
| GET | `/api/server/assets/{namespace}` | AUTH | Assets for one namespace (e.g. `minecraft`) |

**GET `/api/server/profile`** (real example, live server):
```json
{ "serverName":"neoforge", "motd":"A Minecraft Server",
  "minecraftVersion":"1.21.1", "modVersion":"NeoForge 21.1.179",
  "neoforgeVersion":"NeoForge 21.1.179", "gameVersion":"1.21.1",
  "difficulty":"easy", "hardcore":false, "maxPlayers":20, "pvpEnabled":true,
  "onlineMode":false, "commandBlocksEnabled":false,
  "mods":[{"id":"neoessentials","name":"neoessentials","version":"1.0.3"}, ...],
  "modCount":5, "modsLoaded":5 }
```

**GET `/api/server/performance`**: `{"currentTPS":"20.0","averageTickTime":"2.34"}` (both are formatted strings, not numbers).

---

## /api/game

**Handler:** `GameEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/api/endpoints/GameEndpoint.java`

GET-only, all AUTH. **Quirk:** `statistics`/`activity`/`blocks` are largely stubs — `getGameStatistics()` returns all-zero counters (persistent tracking is not yet implemented).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/game/statistics` | AUTH | Aggregate player/block/combat counters (stub, all 0) |
| GET | `/api/game/events` | AUTH | Recent game events (limit 100) |
| GET | `/api/game/activity` | AUTH | Activity summary |
| GET | `/api/game/blocks` | AUTH | Top blocks |

**GET `/api/game/statistics`** (current stub shape):
```json
{ "players":{"totalPlayers":0,"totalPlayTime":0},
  "blocks":{"totalBroken":0,"totalPlaced":0},
  "combat":{"totalKills":0,"totalDeaths":0} }
```

---

## /api/logging

**Handler:** `LoggingEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/api/endpoints/LoggingEndpoint.java`

GET-only, all AUTH. Limits are hard-coded server-side (no query params).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/logging/requests` | AUTH | Last 100 API request logs |
| GET | `/api/logging/errors` | AUTH | Last 100 error logs (severity "ALL") |
| GET | `/api/logging/performance` | AUTH | Performance metrics |

---

## /api/admin

**Handler:** `AdminEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/api/endpoints/AdminEndpoint.java`

`/status` is the only non-admin route; **all others require ADMIN** (a `SecurityException` → 403). Restart/stop broadcast a warning and act after a 5-second delay.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/admin/status` | AUTH | Capability flags + serverRunning |
| POST | `/api/admin/restart` | ADMIN | Save + halt (relies on wrapper to relaunch) |
| POST | `/api/admin/stop` | ADMIN | Save + halt |
| POST | `/api/admin/reload` | ADMIN | Reload configs, translations, permissions |
| POST | `/api/admin/save` | ADMIN | Save all worlds |
| POST | `/api/admin/broadcast` | ADMIN | Broadcast a chat message to all players |

**POST `/api/admin/broadcast`** — body `{"message":"Server maintenance in 10 minutes!"}` (required, non-blank). Message is prefixed with `§6[Dashboard §e<username>§6]§f`. Response: `{"success":true,"recipients":3,"message":"Message sent to 3 player(s)"}`

**GET `/api/admin/status`**: `{"success":true,"serverRunning":true,"canRestart":true,"canStop":true,"canReload":true,"canSave":true}`

**POST `/api/admin/reload`**: `{"success":true,"successCount":3,"totalCount":3,"message":"Reload completed: 3/3 systems reloaded successfully","details":"..."}`

---

## /api/files

**Handler:** `FileManagementHandler` — `src/main/java/com/zerog/neoessentials/webdashboard/handlers/FileManagementHandler.java`

**Every route is ADMIN** (a blanket `auth-admin` check at the top rejects non-admins with 403 before routing). File ops are sandboxed to allowed roots: `config/`, `logs/`, `neoessentials/`, `world/` — anything outside returns 403 "Access denied". Editable extensions: `.json .txt .properties .yml .yaml .toml .conf .cfg .log`. Max read size 10 MB. Writes/deletes auto-create a timestamped backup under `neoessentials/backups/files/`.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/files/browse?path=` | ADMIN | List directory entries |
| GET | `/api/files/read?path=` | ADMIN | Read a text file |
| GET | `/api/files/download?path=` | ADMIN | Download raw file (**binary octet-stream**) |
| GET | `/api/files/listBackups?path=` | ADMIN | List backups for a file |
| GET | `/api/files/cloudProviders` | ADMIN | Cloud provider link status (stub) |
| GET | `/api/files/server/statistics` | ADMIN | Live TPS/RAM/CPU snapshot |
| GET | `/api/files/player/statistics?player=` | ADMIN | Per-player live stats (all online if no player) |
| GET | `/api/files/user/activityLog?limit=` | ADMIN | Dashboard audit log lines (default 100) |
| POST | `/api/files/write` | ADMIN | Overwrite existing file |
| POST | `/api/files/create` | ADMIN | Create file or directory |
| POST | `/api/files/upload` | ADMIN | Upload base64 content |
| POST | `/api/files/restore` | ADMIN | Restore file from a backup |
| POST | `/api/files/cloudBackup` | ADMIN | (stub → 501) |
| POST | `/api/files/cloudRestore` | ADMIN | (stub → 501) |
| POST | `/api/files/cloudLink` | ADMIN | Store a cloud OAuth token |
| POST | `/api/files/cloudUnlink` | ADMIN | Remove a cloud token |
| DELETE | `/api/files/delete?path=` | ADMIN | Delete file/dir (recursive) |

**POST `/api/files/write`** — body `{"path":"config/main.json","content":"..."}`. File must already exist. Response: `{"success":true,"message":"File written successfully","path":"config/main.json","backup":"neoessentials/backups/files/main.json.1700000000000.backup"}`

**POST `/api/files/create`** — body `{"path":"config/newfile.json","type":"file","content":"{}"}` (type: `file`|`directory`; content optional). 409 if path exists. Returns 201.

**POST `/api/files/upload`** — body `{"path":"config/x.dat","content":"<base64>"}`. Not multipart — base64 in JSON. Returns 201 with `size`.

**GET `/api/files/browse?path=config`**:
```json
{ "path":"config", "absolutePath":"/srv/mc/config",
  "items":[ {"name":"main.json","type":"file","size":1024,"modified":1700000000000,
    "created":1690000000000,"extension":".json","editable":true} ] }
```

**GET `/api/files/read`**: `{"path":"config/main.json","content":"...","size":1024,"extension":".json"}`

**GET `/api/files/download`** — returns raw bytes with `Content-Type: application/octet-stream` and `Content-Disposition: attachment`, **not JSON**.

---

## /api/permissions

**Handler:** `PermissionEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/PermissionEndpoint.java`

**GET = AUTH; every POST/PUT/DELETE = ADMIN.** Path is matched after stripping the `/api/permissions` prefix. `{username}` segments resolve offline (online list → profile cache → Mojang API); `{group}` is a group name. All management returns `{"success":false}` when an external permission system (LuckPerms/FTB) is active — internal manager only.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/permissions` or `/overview` | AUTH | Counts + per-group stats |
| GET | `/api/permissions/groups` | AUTH | All groups with perms/inherits |
| GET | `/api/permissions/group/{name}` | AUTH | One group |
| GET | `/api/permissions/group/{name}/context` | AUTH | Group contextual perms |
| GET | `/api/permissions/group/{name}/temp` | AUTH | Group temp perms |
| GET | `/api/permissions/users` | AUTH | Online users + groups/perms |
| GET | `/api/permissions/user/{name}` | AUTH | One user (offline-resolvable) |
| GET | `/api/permissions/user/{name}/context` | AUTH | User contextual perms |
| GET | `/api/permissions/user/{name}/temp` | AUTH | User temp perms |
| GET | `/api/permissions/permissions/all` | AUTH | Full node catalog by category |
| GET | `/api/permissions/system/status` | AUTH | System type/health |
| GET | `/api/permissions/aliases` | AUTH | Permission aliases |
| POST | `/api/permissions/reload` | ADMIN | Reload permission system |
| POST | `/api/permissions/group/create` | ADMIN | Create group |
| POST | `/api/permissions/group/{name}/rename` | ADMIN | Rename group `{newName}` |
| POST | `/api/permissions/group/{name}/permission/add` | ADMIN | Add node to group `{permission}` |
| POST | `/api/permissions/group/{name}/context` | ADMIN | Add contextual override |
| POST | `/api/permissions/group/{name}/temp` | ADMIN | Add temp perm `{node,duration}` |
| POST | `/api/permissions/user/{name}/group/set` | ADMIN | Set user's group `{group}` |
| POST | `/api/permissions/user/{name}/permission/add` | ADMIN | Add node to user `{permission}` |
| POST | `/api/permissions/user/{name}/context` | ADMIN | Add user contextual override |
| POST | `/api/permissions/user/{name}/temp` | ADMIN | Add user temp perm |
| POST | `/api/permissions/aliases` | ADMIN | Add alias `{alias,canonical}` |
| PUT | `/api/permissions/group/{name}/update` | ADMIN | Update prefix/suffix/priority/inherits |
| PUT | `/api/permissions/user/{name}/update` | ADMIN | (stub — "not yet implemented") |
| DELETE | `/api/permissions/group/{name}` | ADMIN | Delete group |
| DELETE | `/api/permissions/group/{name}/context` | ADMIN | Remove contextual (body `{contextKey,node}`) |
| DELETE | `/api/permissions/group/{name}/temp/{node}` | ADMIN | Remove group temp perm |
| DELETE | `/api/permissions/group/{name}/permission/remove/{perm}` | ADMIN | Remove node from group |
| DELETE | `/api/permissions/user/{name}/context` | ADMIN | Remove user contextual (body `{contextKey,node}`) |
| DELETE | `/api/permissions/user/{name}/temp/{node}` | ADMIN | Remove user temp perm |
| DELETE | `/api/permissions/user/{name}/permission/remove/{perm}` | ADMIN | Remove node from user |
| DELETE | `/api/permissions/aliases/{alias}` | ADMIN | Remove alias |

**POST `/api/permissions/group/create`** — `{"name":"vip","prefix":"&a[VIP] ","suffix":"","priority":10,"isDefault":false,"inherits":["default"]}`. Response: `{"success":true,"message":"Group created: vip"}`

**PUT `/api/permissions/group/{name}/update`** — `{"prefix":"&a","suffix":"","priority":20,"inherits":["default"],"isDefault":false}`. `inherits` is a **full replace**, not a merge.

**POST `/api/permissions/user/{name}/temp`** — `{"node":"neoessentials.fly","duration":"1d"}` (duration like `1d`,`12h`,`30m`).

**POST `.../context`** — `{"contextKey":"world:minecraft:overworld","node":"neoessentials.fly","allow":true}`.

**GET `/api/permissions/groups`**:
```json
{ "success":true, "groups":[ {"name":"default","prefix":"","suffix":"","priority":0,
  "isDefault":true,"permissionCount":5,"permissions":["..."],"inherits":[]} ] }
```

**GET `/api/permissions/permissions/all`**: `{"success":true,"categories":[{"category":"...","key":"...","permissions":[{"node":"...","description":"...","defaultValue":false}]}]}`

---

## /api/motd

**Handler:** `MotdEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/MotdEndpoint.java`

**GET = AUTH; every POST/PUT/DELETE = ADMIN.** `{name}` is a profile name (lowercased server-side).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/motd` | AUTH | All profiles + rotation + active |
| GET | `/api/motd/active` | AUTH | Active profile |
| POST | `/api/motd/profiles` | AUTH | Create/update profile `{name,motd,author?}` |
| POST | `/api/motd/rotation/next` | AUTH | Rotate to next profile immediately |
| POST | `/api/motd/broadcast` | AUTH | Broadcast active MOTD to all players |
| PUT | `/api/motd/active` | AUTH | Switch active profile `{name}` |
| PUT | `/api/motd/rotation` | AUTH | Set rotation `{enabled,intervalMinutes?}` |
| DELETE | `/api/motd/profiles/{name}` | AUTH | Delete a profile |

**GET `/api/motd`**:
```json
{ "success":true, "activeProfile":"default",
  "rotation":{"enabled":false,"intervalMinutes":60},
  "profiles":{ "default":{"name":"default","motd":"Welcome!","author":"Dashboard","timestamp":"07/15/2026 19:11"} } }
```
Note `timestamp` here is a pre-formatted `MM/dd/yyyy HH:mm` string, not epoch millis (unlike most other timestamp fields in this API).

**POST `/api/motd/profiles`** — `{"name":"summer","motd":"&aSummer event!","author":"Admin"}`. Response includes the saved `profile` object.

**PUT `/api/motd/rotation`** — `{"enabled":true,"intervalMinutes":30}`. Response: `{"success":true,"rotationEnabled":true,"intervalMinutes":30,"message":"Rotation enabled every 30 minutes"}`

---

## /api/rules

**Handler:** `RulesEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/RulesEndpoint.java`

**GET = AUTH; every POST/PUT/DELETE = ADMIN.** `{number}` is a **1-based index**. Rule text max 200 chars.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/rules` | AUTH | List rules (numbered) |
| POST | `/api/rules` | AUTH | Replace all rules `{"rules":["...",...]}` |
| POST | `/api/rules/add` | AUTH | Append a rule `{"rule":"..."}` |
| POST | `/api/rules/reload` | AUTH | Reload rules from disk |
| PUT | `/api/rules/{number}` | AUTH | Edit rule at 1-based index `{"rule":"..."}` |
| DELETE | `/api/rules/{number}` | AUTH | Delete rule at 1-based index |

**GET `/api/rules`**: `{"success":true,"count":2,"rules":[{"number":1,"text":"No griefing"},{"number":2,"text":"Be kind"}]}`

**POST `/api/rules`** (replace all) — `{"rules":["No griefing","Be kind"]}`. Response: `{"success":true,"count":2,"message":"2 rule(s) saved"}`

---

## /api/teleport

**Handler:** `TeleportEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/TeleportEndpoint.java`

Only one resource (`/settings`). **GET = AUTH; PUT/POST = ADMIN.** PUT is a partial merge into `config.json`'s `teleportation` section, with per-field min/max clamping, then reloads teleport managers.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/teleport/settings` | AUTH | Read all teleport settings |
| PUT/POST | `/api/teleport/settings` | ADMIN | Update teleport settings |

**GET `/api/teleport/settings`**:
```json
{ "success":true,
  "generalSettings":{"teleportDelay":3,"enableTeleportWarmup":true,"cancelTeleportOnMove":true,"cancelTeleportOnDamage":true,"maxTeleportDistance":0},
  "homeSettings":{"homeSetCooldown":0,"homeTeleportCooldown":0,"homeDeleteCooldown":0,"maxHomes":5,"enableHomeSafety":true,"allowCrossDimensionHomes":true},
  "warpSettings":{"warpSetCooldown":0,"warpCooldown":0,"maxWarps":50,"enableWarpSafety":true},
  "spawnSettings":{"spawnCooldown":0,"enableSpawnSafety":true},
  "backSettings":{"backCooldown":0,"teleportDelay":3,"enableDeathBack":true,"enableTeleportBack":true} }
```
**PUT** accepts the same nested structure; send only the sub-objects/keys you want to change. Response: `{"success":true,"message":"Teleport settings saved and managers reloaded."}`

---

## /api/placeholders

**Handler:** `PlaceholderEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/PlaceholderEndpoint.java`

GET-only, all AUTH.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/placeholders/list` | AUTH | All registered placeholder identifiers |
| GET | `/api/placeholders/resolve?player=&text=` | AUTH | Resolve a string server-side |
| GET | `/api/placeholders/stats` | AUTH | Registry statistics |

**GET `/api/placeholders/resolve?player=Notch&text=Hi %player_name%`** (player optional; if omitted, resolution runs with no player context):
```json
{ "success":true, "player":"Notch", "input":"Hi %player_name%", "resolved":"Hi Notch" }
```
**GET `/api/placeholders/list`**: `{"success":true,"count":42,"placeholders":["player_name","server_tps",...]}`

---

## /api/shops

**Handler:** `ShopEndpoint` — `src/main/java/com/zerog/neoessentials/shop/dashboard/ShopEndpoint.java`

**GET = AUTH; every POST/PUT = ADMIN** (list/stats/npc/csv-export reads stay open). `/list` supports `?page=&size=` pagination (defaults page=1, size=50). Shops are keyed by a **`signKey`** (opaque string from `ShopData.toKey()`), not a UUID.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/shops/list?page=&size=` | AUTH | Paginated sign-shop list |
| GET | `/api/shops/stats` | AUTH | Aggregate stats + top sellers |
| GET | `/api/shops/npc` | AUTH | NPC/entity shops |
| GET | `/api/shops/csv/export` | AUTH | Download shops as **CSV** (text/csv) |
| POST | `/api/shops/csv/import` | AUTH | Import CSV (raw CSV text as body) |
| PUT | `/api/shops/price` | AUTH | Update one shop's prices |

**GET `/api/shops/list`**:
```json
{ "page":1,"size":50,"total":120,
  "shops":[ {"signKey":"world:10:64:-3","ownerName":"Notch","ownerUUID":"...","itemId":"minecraft:diamond",
    "quantity":1,"buyPrice":"100","sellPrice":"80","isAdmin":false,"shopType":"BUY_SELL","totalSales":42} ] }
```

**PUT `/api/shops/price`** — `{"signKey":"world:10:64:-3","buyPrice":"120","sellPrice":"90"}` (prices are strings → BigDecimal; either omittable). 404 if signKey not found. Response: `{"success":true,"signKey":"world:10:64:-3"}`

**POST `/api/shops/csv/import`** — **body is raw CSV text**, not JSON. Response: `{"updated":5,"created":2,"skipped":1,"details":"..."}`

**GET `/api/shops/csv/export`** — returns `text/csv` attachment `shop_prices.csv`, **not JSON**.

---

## /api/backup

**Handler:** `BackupEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/BackupEndpoint.java`

**`/status` and `/list` are AUTH; download/create/restore/delete are ADMIN.** Snapshots keyed by **`name`**.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/backup/status` | AUTH | Storage stats + available targets |
| GET | `/api/backup/list` | AUTH | List snapshots (newest first) |
| GET | `/api/backup/download?name=` | ADMIN | Download snapshot (**binary ZIP**) |
| POST | `/api/backup/create` | ADMIN | Create snapshot |
| POST | `/api/backup/restore` | ADMIN | Restore snapshot `{name}` |
| DELETE | `/api/backup/delete?name=` | ADMIN | Delete snapshot |

**POST `/api/backup/create`** — `{"name":"pre-update","targets":["configs","neodata","world"]}` (name optional → `backup-<millis>`; targets is a JSON string array). Response is `BackupManager.createSnapshot(...)` result plus `"success":true`.

**GET `/api/backup/download?name=pre-update`** — returns `application/zip` attachment, **not JSON**.

`/status` and `/list` bodies are produced by `BackupManager` (`getStatus()` / `listSnapshots()`).

---

## /api/discord

**Handler:** `DiscordEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/DiscordEndpoint.java`

Read routes AUTH; mutating/config-write routes ADMIN. This mod never performs Discord OAuth2 itself — `/link-lookup` is for an external app that did its own OAuth to reverse-resolve a Discord ID.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/discord/status` | AUTH | Loaded adapters + anyActive |
| GET | `/api/discord/events?limit=` | AUTH | Recent relay events (default 100) |
| GET | `/api/discord/auth-config` | AUTH | Read discord_auth.json (non-secret fields) |
| GET | `/api/discord/link-lookup?discordId=` | AUTH | Resolve Discord ID → Minecraft account |
| POST | `/api/discord/test` | ADMIN | Send test message to a channel |
| POST | `/api/discord/auth-config` | ADMIN | Update discord_auth.json (partial) |
| DELETE | `/api/discord/events` | ADMIN | Clear the event log |

**POST `/api/discord/test`** — `{"channel":"123456789012345678","message":"Test!"}`. `channel` **must be a numeric snowflake ID** (15–25 digits), not a channel name — otherwise 400. 503 if no adapters loaded, 502 if delivery failed.

**GET `/api/discord/status`**:
```json
{ "success":true,"anyActive":true,"adapterCount":1,"eventCount":12,
  "adapters":[{"name":"SDLink","enabled":true,"ready":true}] }
```

**GET `/api/discord/link-lookup?discordId=...`**: `{"success":true,"linked":true,"minecraftUuid":"...","minecraftUsername":"Notch"}` (or `{"success":true,"linked":false}`).

**POST `/api/discord/auth-config`** — partial: `{"enabled":true,"requireLinkedAccount":true,"allowAutoRegistration":false,"defaultRole":"VIEWER"}` (role ∈ ADMIN|MODERATOR|VIEWER).

---

## /api/cloud

**Handler:** `CloudStorageEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/CloudStorageEndpoint.java`

`/status`, `/config` (GET), and file-listing GETs are AUTH; **all config-writes, tests, uploads, deletes are ADMIN.** Two providers: `dropbox`, `google`.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/cloud/status` | AUTH | Both providers' config + quota |
| GET | `/api/cloud/config` | AUTH | Masked config |
| GET | `/api/cloud/files/dropbox` | AUTH | List Dropbox files |
| GET | `/api/cloud/files/google` | AUTH | List Google Drive files |
| POST | `/api/cloud/config/dropbox` | ADMIN | Save Dropbox token/path |
| POST | `/api/cloud/config/google` | ADMIN | Save Google OAuth creds |
| POST | `/api/cloud/test/dropbox` | ADMIN | Test Dropbox connection |
| POST | `/api/cloud/test/google` | ADMIN | Test Google connection |
| POST | `/api/cloud/upload/dropbox/{backupId}` | ADMIN | Upload a backup zip to Dropbox |
| POST | `/api/cloud/upload/google/{backupId}` | ADMIN | Upload a backup zip to Google |
| DELETE | `/api/cloud/files/dropbox/{path}` | ADMIN | Delete Dropbox file (URL-encoded path) |
| DELETE | `/api/cloud/files/google/{fileId}` | ADMIN | Delete Google file by ID |

**POST `/api/cloud/config/dropbox`** — `{"accessToken":"...","uploadPath":"/NeoEssentials-Backups"}`.
**POST `/api/cloud/config/google`** — `{"refreshToken":"...","clientId":"...","clientSecret":"...","folderId":"..."}`.
**`{backupId}`** resolves to `<backupId>.zip` under the backup dir (sanitised). **Dropbox delete** takes a URL-encoded file *path*; **Google delete** takes a file *ID*.

**GET `/api/cloud/status`**:
```json
{ "success":true, "providers":{
  "dropbox":{"configured":true,"uploadPath":"/NeoEssentials-Backups","tokenMasked":"sl.****","quotaUsedMB":120,"quotaTotalMB":2048,"connected":true},
  "googleDrive":{"configured":false,"folderId":"","clientId":""} } }
```

---

## /api/users

**Handler:** `UserManagementEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/UserManagementEndpoint.java`

**Every route is ADMIN** (blanket check up front). These are *dashboard* accounts (RBAC), not Minecraft players. `{id}` is the internal user id (UUID-like string); `{sid}` is a session id.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/users/list` | ADMIN | All dashboard users |
| GET | `/api/users/sessions` | ADMIN | Active sessions |
| POST | `/api/users/create` | ADMIN | Create user (errors if username taken) |
| POST | `/api/users/sync` | ADMIN | Create-or-update by username (idempotent) |
| POST | `/api/users/{id}/role` | ADMIN | Change role `{role}` |
| POST | `/api/users/{id}/password` | ADMIN | Set password, or generate temp if empty |
| POST | `/api/users/{id}/enable` | ADMIN | Enable account |
| POST | `/api/users/{id}/disable` | ADMIN | Disable account |
| DELETE | `/api/users/{id}` | ADMIN | Delete user (can't delete self) |
| DELETE | `/api/users/sessions/{sid}` | ADMIN | Revoke a session |

**POST `/api/users/create`** — `{"username":"mod1","password":"secret","email":"m@x.com","role":"MODERATOR"}` (role ∈ ADMIN|MODERATOR|VIEWER, default VIEWER). Response: `{"success":true,"message":"User created","user":{...}}`.

**User object** (`User.toJson`): `id, username, email, role, enabled, createdAt, lastLoginAt, lastLoginIp, failedLoginAttempts, lockoutUntil, requiresPasswordChange, isTempPassword, permissions[]`.

**POST `/api/users/{id}/password`** with empty/absent password → generates a temp password: `{"success":true,"message":"Temporary password generated","tempPassword":"..."}`.

**GET `/api/users/sessions`**: each session has `sessionId, username, role, ipAddress, createdAt, lastAccessAt`.

### Keeping dashboard accounts in sync with the external dashboard's own user table

There are two directions here — use whichever (or both) fit your architecture:

**External dashboard → mod:** if the external dashboard has its own user accounts and wants a
matching mod-side account to exist (e.g. so that account's Minecraft-side permissions/identity
line up), call:

**POST `/api/users/sync`** — `{"username":"mod1","email":"m@x.com","role":"MODERATOR"}` (no
`password` field — this route mints its own throwaway one internally, since the real login
surface for this account is the external dashboard, not this mod's `/api/auth/login`). Unlike
`/create`, **this never errors on an existing username** — it updates role/email in place
instead — so it's safe to re-POST an account's current state at any time without checking
whether it already exists here first. Returns `201` with `"created":true` the first time,
`200` with `"created":false` on subsequent syncs of the same username.

**Mod → external dashboard:** whenever a dashboard_user is created, role/enabled-changed, or
deleted here — via in-game `/dashboardregister`, `/api/users/create`, `/api/users/sync`, or any
other admin action — the mod pushes a notification to the paired dashboard's webhook endpoint,
so it mirrors the change into its own table automatically instead of needing to poll
`/api/users/list`. This is set up automatically by the pairing flow below, not hand-configured.

Empty `webDashboard.externalDashboard.url`/`.token` (the default, before pairing) disables this
entirely — it's optional, on top of the REST endpoints above, not a replacement for them. Once
paired, every event fires a `POST` to `<dashboardUrl>/webhooks/mod/user-sync`:

```json
{ "event": "user_created", "id": "...", "username": "mod1", "email": "m@x.com",
  "role": "MODERATOR", "enabled": true, "timestamp": 1700000000000 }
```
`event` is one of `user_created` / `user_updated` / `user_deleted`, sent with
`Authorization: Bearer <token>` — the token the dashboard minted for the mod during pairing (see
below). This is fire-and-forget: if the dashboard is briefly unreachable, the actual user-account
change already happened regardless — nothing on the mod side blocks or retries on delivery
failure.

### Pairing — the recommended one-step setup

Manually running `/apikey create` and hand-copying the token into the external dashboard's
config, *and* separately configuring a shared webhook secret on both sides, used to be two
different manual steps that had to be kept in sync by hand. **Pairing replaces both with one
command**, run once:

1. On the dashboard's own admin Configuration page, click "Generate Pairing Code." It shows a
   one-time code (valid 10 minutes) and the exact command to run.
2. On the Minecraft server console (or in-game, if OP), run:
   ```
   /dashboard pair "<dashboardUrl>" <code>
   ```
   The quotes around the URL are required (Brigadier's unquoted string parsing can't contain
   `:` or `/`, which every URL does) — the dashboard's own generated command already includes
   them. Requires permission `neoessentials.dashboard.pair`.
3. In that single request/response round-trip:
   - The mod mints an API key (via the same `ApiKeyManager` `/apikey` uses) and sends it, along
     with this server's WebSocket port (`webDashboard.websocketPort` — a separately-configured
     port from the REST API's own, so a dashboard can auto-configure a live WebSocket connection
     without the admin hand-entering it), to the dashboard at `POST <dashboardUrl>/api/pair/
     complete`. The dashboard stores both and uses them for its own outbound REST calls and
     WebSocket subscription to this mod's API from then on.
   - The dashboard mints its own token and returns it in the response — the mod stores this (as
     `webDashboard.externalDashboard.url`/`.token`) and uses it to authenticate the user-sync
     webhook above.
4. `/dashboard unpair` clears the stored connection and revokes the API key that was minted for
   it — use this before re-pairing with a different dashboard instance.

Both directions of traffic now authenticate with a token the *receiving* side generated for the
*calling* side — the same trust model both ways, nothing to manually keep in sync afterward.

---

## /api/moderation

**Handler:** `ModerationEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/ModerationEndpoint.java`

**Every GET is AUTH; every mutating route is ADMIN.** Backed by the real `BanManager`/`MuteManager` (same stores the in-game commands enforce). **Path-segment types vary** — bans use **UUID**, mutes/kicks/warns/notes use **name**, IP routes use the (URL-encoded) **IP**, reports use **id**. Durations in POST bodies are **seconds** (`-1` or omitted = permanent for bans; `0` = permanent for mutes).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/moderation/overview` | AUTH | Counts across all punishment types |
| GET | `/api/moderation/bans/active` | AUTH | Active player bans |
| GET | `/api/moderation/bans` | AUTH | Active + archived bans |
| GET | `/api/moderation/bans/{uuid}` | AUTH | Ban history for one player (UUID) |
| POST | `/api/moderation/ban` | ADMIN | Ban `{playerName,reason,duration}` |
| DELETE | `/api/moderation/ban/{uuid}` | ADMIN | Unban (UUID) |
| GET | `/api/moderation/ipbans/active` \| `/ipbans` | AUTH | IP bans |
| POST | `/api/moderation/ipban` | ADMIN | IP ban `{ip,reason,duration}` |
| DELETE | `/api/moderation/ipban/{ip}` | ADMIN | Unban IP |
| GET | `/api/moderation/mutes/active` \| `/mutes` | AUTH | Mutes |
| GET | `/api/moderation/mutes/{name}` | AUTH | Mute history (name) |
| POST | `/api/moderation/mute` | ADMIN | Mute `{targetName,reason,duration}` |
| DELETE | `/api/moderation/mute/{name}` | ADMIN | Unmute (name) |
| GET | `/api/moderation/ipmutes` | AUTH | Active IP mutes |
| POST | `/api/moderation/ipmute` | ADMIN | IP mute `{ip,reason,duration}` |
| DELETE | `/api/moderation/ipmute/{ip}` | ADMIN | Unmute IP |
| GET | `/api/moderation/kicks` | AUTH | All kicks |
| GET | `/api/moderation/kicks/{name}` | AUTH | Kick history (name) |
| GET | `/api/moderation/warns` | AUTH | All warns |
| GET | `/api/moderation/warns/{name}` | AUTH | Warns for a player (name) |
| DELETE | `/api/moderation/warn/{id}` | ADMIN | Remove warn (body `{targetName}`) |
| GET | `/api/moderation/notes/{name}` | AUTH | Staff notes (name) |
| POST | `/api/moderation/note` | ADMIN | Add note `{targetName,text}` |
| DELETE | `/api/moderation/note/{id}` | ADMIN | Remove note (body `{targetName}`) |
| GET | `/api/moderation/reports` | AUTH | Pending reports |
| GET | `/api/moderation/reports/all` | AUTH | All reports |
| GET | `/api/moderation/reports/{id}` | AUTH | One report |
| POST | `/api/moderation/reports/{id}/review` | ADMIN | Review `{status,notes?}` |

**POST `/api/moderation/ban`** — `{"playerName":"Griefer","reason":"Griefing spawn","duration":86400}` (duration seconds; `-1`/omit = permanent). Player resolved online→profile-cache; 404 if unknown, 409 if already banned/disabled. Response: `{"success":true,"message":"Ban created","playerId":"<uuid>"}`.

**Ban object**: `id, playerName, playerId, reason, bannedBy, banTime, expireTime, permanent, evidence, active, unbannedBy, unbannedAt`. **Mute object**: `id, target, reason, mutedBy, muteTime, expireTime, permanent, active, unmutedBy, unmutedAt`.

**GET `/api/moderation/overview`**:
```json
{ "success":true,"activeBans":3,"activeIPBans":1,"mutedCount":2,"totalKicks":10,
  "totalWarns":5,"totalNotes":4,"pendingReports":2,"totalReports":8,"jailedCount":0 }
```

**POST `/api/moderation/reports/{id}/review`** — `{"status":"REVIEWED","notes":"handled"}` (status ∈ REVIEWED|DISMISSED etc., mapped to `ReportEntry.Status`).

**DELETE `/api/moderation/warn/{id}`** — the warn *id* is in the path, but the target player's name must be in the **body**: `{"targetName":"Griefer"}`.

---

## /api/public/moderation

**Handler:** `PublicModerationEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/PublicModerationEndpoint.java`

**PUBLIC — no Bearer token required** (registered with `requireAuth=false`; still CORS + rate-limited). Gated only by config flag `webDashboard.securitySettings.publicModerationLookupEnabled` (default on) — returns 404 if disabled. Deliberately excludes IP bans/mutes, notes, and reports (privacy). Exposes only bans/mutes/kicks/warns.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/public/moderation/lookup/{name}` | PUBLIC | Bans/mutes/kicks/warns for one player |
| GET | `/api/public/moderation/recent` | PUBLIC | Recent active bans + mutes (max 30) |

**GET `/api/public/moderation/lookup/Griefer`**:
```json
{ "success":true, "playerName":"Griefer", "playerId":"<uuid or null>",
  "bans":[{"id":1,"playerName":"Griefer","playerId":"...","reason":"...","bannedBy":"...","banTime":...,"expireTime":...,"permanent":false,"active":true,"unbannedBy":null,"unbannedAt":0}],
  "mutes":[...], "kicks":[...], "warns":[{"id":"...","targetName":"Griefer","warnedBy":"...","reason":"...","timestamp":...}] }
```
(Ban/mute JSON here omits the `evidence` field present in the authed `/api/moderation` variant.)

**GET `/api/public/moderation/recent`**: `{"success":true,"count":5,"recent":[{...,"type":"ban"},{...,"type":"mute"}]}` (each entry tagged with `type`, sorted newest first).

---

## /api/kits

**Handler:** `KitsEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/KitsEndpoint.java`

GET-only, all AUTH. `{name}` is a kit name (any path not ending `/list` or `/stats` is treated as a kit name).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/kits/list` | AUTH | All kits with metadata |
| GET | `/api/kits/stats` | AUTH | Count/enabled/cooldown breakdown |
| GET | `/api/kits/{name}` | AUTH | One kit (404 if not found) |

**GET `/api/kits/list`**:
```json
{ "success":true,"count":2,"kits":[
  {"name":"starter","displayName":"Starter Kit","description":"...","enabled":true,
   "permission":"neoessentials.kit.starter","cooldownMs":86400000,"cooldownDisplay":"1d 0h",
   "maxUses":0,"itemCount":5} ] }
```
**GET `/api/kits/stats`**: `{"success":true,"total":10,"enabled":8,"withPermission":6,"withCooldown":7,"withUsageLimit":2}`

---

## /api/holograms

**Handler:** `HologramEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/HologramEndpoint.java`

**GET = AUTH; every POST/PUT/DELETE = ADMIN.** `{id}` is the hologram string id. **Quirk:** action routes are `/{id}/spawn|despawn|visible`; the handler parses the last two path segments, so `/list`, `/stats`, `/create` are reserved names.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/holograms/list` | AUTH | All holograms |
| GET | `/api/holograms/stats` | AUTH | Counts (total/visible/animated/shop) |
| GET | `/api/holograms/{id}` | AUTH | One hologram |
| POST | `/api/holograms/create` | AUTH | Create (body = full `HologramData` JSON) |
| PUT | `/api/holograms/{id}` | AUTH | Update (id preserved from path) |
| DELETE | `/api/holograms/{id}` | AUTH | Delete + despawn |
| POST | `/api/holograms/{id}/spawn` | AUTH | Force re-spawn |
| POST | `/api/holograms/{id}/despawn` | AUTH | Despawn |
| POST | `/api/holograms/{id}/visible` | AUTH | Toggle visibility |

**POST `/api/holograms/create`** — body is a full `HologramData` object (deserialised directly via Gson); `id` required. Minimal example:
```json
{ "id":"welcome","world":"minecraft:overworld","x":0.5,"y":70,"z":0.5,"visible":true,
  "refreshInterval":20,"scale":1.0,"lineSpacing":0.25,"textShadow":true,"textOpacity":255,
  "backgroundColorArgb":0,"billboardMode":"CENTER","spinEnabled":false,
  "lines":[{"lineId":0,"text":"&aWelcome!","animFrameIntervalTicks":0,"frames":[]}] }
```
Response: `{"success":true,"id":"welcome"}`. GET list/get returns the same fields plus `lineCount`, and spin/hover animation fields (`spinSpeedDegrees`,`spinAxis`,`hoverEnabled`,`hoverAmplitude`,`hoverSpeedDegrees`).

**GET `/api/holograms/stats`**: `{"success":true,"total":5,"visible":4,"animated":1,"shopHolograms":2}`

---

## /api/warps

**Handler:** `WarpsEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/WarpsEndpoint.java`

**Server warps: GET = AUTH; POST/DELETE = ADMIN.** `{name}` is a warp name.
**Player warps (`/players` sub-path): every route is ADMIN, including GET** — unlike server
warps, player-created warps are personal, not public, so listing them needs the same gate as
deleting them.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/warps` | AUTH | List all server warps |
| GET | `/api/warps/{name}` | AUTH | One server warp |
| POST | `/api/warps` | ADMIN | Create a server warp |
| DELETE | `/api/warps/{name}` | ADMIN | Delete a server warp |
| GET | `/api/warps/players` | ADMIN | List every player's warps (`/pwarp`) |
| GET | `/api/warps/players/{uuid}` | ADMIN | One player's warps |
| DELETE | `/api/warps/players/{uuid}/{name}` | ADMIN | Delete one player's warp |

**POST `/api/warps`** — `{"name":"spawn","world":"minecraft:overworld","x":0.5,"y":64,"z":0.5,"yaw":0,"pitch":0}` (yaw/pitch optional, default 0). Response: `{"success":true,"message":"Warp 'spawn' created"}`.

**GET `/api/warps`**:
```json
{ "success":true,"count":1,
  "warps":[{"world":"minecraft:overworld","x":0.5,"y":64.0,"z":0.5,"yaw":0.0,"pitch":0.0,
    "timestamp":1700000000000,"createdBy":"Dashboard","name":"spawn"}] }
```
(The per-warp object is `TeleportLocation.toJson()` — note the field is `world`, plus `createdBy`/`timestamp` — with `name` added.)

**GET `/api/warps/players`** — every player who has ≥1 player warp, with their warps inline:
```json
{ "success":true,"totalPlayers":1,"totalWarps":2,
  "players":[{"uuid":"165050d9-86e2-3846-9415-a9ba682ff149","name":"MrWhiteFlamesYT","warpCount":2,
    "warps":[{"world":"minecraft:overworld","x":10.0,"y":70.0,"z":-4.0,"yaw":0.0,"pitch":0.0,
      "timestamp":1700000000000,"createdBy":"MrWhiteFlamesYT","name":"base"}, ...]}] }
```
`GET /api/warps/players/{uuid}` returns the same per-player shape (`uuid`, `name`, `warpCount`,
`warps`) for a single player; 400 with an `error` if that UUID has no warps. `name` is resolved via
the server's profile cache, falling back to the online player list, then a truncated UUID if
neither has it.

**DELETE `/api/warps/players/{uuid}/{name}`** — admin cleanup of a single player warp. Response:
`{"success":true,"message":"Warp '<name>' deleted for player <uuid>"}`.

---

## /api/commands

**Handler:** `CommandExecutionHandler` — `src/main/java/com/zerog/neoessentials/webdashboard/handlers/CommandExecutionHandler.java`

**Every route is ADMIN** (blanket check). Commands run at **OP level 4**. A hard blocklist (`stop, restart, reload, op, deop, whitelist, ban, ban-ip`) is always rejected with 403 regardless of any request flag.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/commands/execute` | ADMIN | Run a server command, capture output |
| GET | `/api/commands/history?limit=` | ADMIN | Command history (default 50) |
| GET | `/api/commands/suggestions?input=` | ADMIN | Autocomplete matching root commands |

**POST `/api/commands/execute`** — `{"command":"list"}` (leading `/` stripped automatically; a `checkPermissions` field, if sent, is ignored). Response:
```json
{ "success":true,"command":"list","result":1,"executionId":"<uuid>","output":["There are 1 of a max of 20 players online: Notch"] }
```
Blocked command → `403 {"error":"Command 'stop' requires elevated permissions"}`. Note: even execution *errors* return HTTP 200 with `success:false` + `error`.

**GET `/api/commands/suggestions?input=ga`**: `{"suggestions":[{"command":"gamemode","restricted":false}],"input":"ga"}`

---

## /api/economy

**Handler:** `EconomyEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/EconomyEndpoint.java`

**GET = AUTH; POST = ADMIN.** The path segment `{username}` may be **either a username or a raw UUID** (tries `UUID.fromString` first, then online list → profile cache). Read-only leaderboards are under `/api/stats/economy` instead.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/economy/{usernameOrUuid}` | AUTH | Current balance |
| POST | `/api/economy/{usernameOrUuid}` | ADMIN | Adjust balance |

**POST `/api/economy/Notch`** — `{"action":"give","amount":100}` (action ∈ give|take|set; amount ≥ 0). Response: `{"success":true,"username":"Notch","newBalance":"1100.00"}`. Rejected adjustment (insufficient funds / over limit) → 400.

**GET `/api/economy/Notch`**: `{"success":true,"username":"Notch","uuid":"...","balance":"1000.00"}` (balance is a 2-dp string).

---

## /api/stats

**Handler:** `StatsEndpoint` — `src/main/java/com/zerog/neoessentials/webdashboard/endpoints/StatsEndpoint.java`

GET-only, all AUTH. Samples TPS + memory into a 60-point/60-minute ring buffer once per minute (also drives WebSocket "stats pulse").

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/stats/overview` | AUTH | economy + activity + performance combined |
| GET | `/api/stats/economy` | AUTH | Wealth, top-10 balances, distribution |
| GET | `/api/stats/activity` | AUTH | Sessions (active/peak/unique/recent) |
| GET | `/api/stats/performance` | AUTH | TPS/mem/uptime/CPU + 60-min history |

**GET `/api/stats/economy`**:
```json
{ "totalWealth":"152340.00","accountCount":87,"currencySymbol":"$","startingBalance":100.0,
  "averageBalance":"1751.03",
  "topPlayers":[{"rank":1,"uuid":"...","name":"Notch","balance":"50000.00","online":true}],
  "distribution":[{"label":"$0","count":3},{"label":"$1–100","count":10}, ...] }
```

**GET `/api/stats/performance`**:
```json
{ "tps":20.0,"tickMs":2.3,"memUsedMb":1800,"memMaxMb":4096,"memPercent":44,
  "uptimeMs":7200000,"uptimeHours":2,"uptimeMinutes":0,"players":3,"playersMax":20,
  "cpuCores":8,"loadAvg":1.25,
  "tpsHistory":[20.0,19.9,...],"memHistory":[1750,1800,...],"timeLabels":["14:01","14:02",...] }
```

**GET `/api/stats/activity`**: `{"currentOnline","peakOnlineToday","uniqueToday","activeSessions":[{"name","sessionMs","sessionMin"}],"completedSessionsToday","avgSessionMinutes","totalPlayMinutesToday","recentSessions":[{"name","sessionMin","endedAt"}]}`

**GET `/api/stats/overview`** nests all three under `economy`/`activity`/`performance`.

---

## /api/docs

**Handler:** `DocumentationHandler` — `src/main/java/com/zerog/neoessentials/docs/DocumentationHandler.java` (note: package `com.zerog.neoessentials.docs`, not under `webdashboard`)

**PUBLIC — registered WITHOUT `withAuth`.** GET-only. Serves the mod's built-in documentation content (sections/tutorials/FAQ/videos + its own API-doc catalog).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/docs/sections` | PUBLIC | All doc sections |
| GET | `/api/docs/sections/{id}` | PUBLIC | One section |
| GET | `/api/docs/api` | PUBLIC | All documented API endpoints |
| GET | `/api/docs/api/{endpoint}` | PUBLIC | One API-doc entry |
| GET | `/api/docs/tutorials` | PUBLIC | All tutorials |
| GET | `/api/docs/tutorials/{id}` | PUBLIC | One tutorial |
| GET | `/api/docs/faq` | PUBLIC | FAQ items (grouped by tag) |
| GET | `/api/docs/faq/search?q=` | PUBLIC | Search FAQ |
| GET | `/api/docs/videos` | PUBLIC | Video tutorials |
| GET | `/api/docs/videos/{id}` | PUBLIC | One video |
| GET | `/api/docs/search?q=` | PUBLIC | Search across all doc types |

**GET `/api/docs/search?q=ban`**:
```json
{ "success":true,"query":"ban","totalResults":4,
  "results":{"sections":[...],"api":[...],"tutorials":[...],"faq":[...]} }
```
`/faq/search` and `/search` return `400` if `q` is missing/blank.

---

## Cross-cutting notes

- **`/api/ping`** (registered inline in `DashboardAPI`, PUBLIC, no auth): `GET` returns `{"success":true,"mod":"neoessentials","mode":"internal|external"}` — a reachability check independent of auth. Good first call to verify connectivity before attempting login/API-key auth.
- **CORS:** every handler sets `Access-Control-Allow-Origin: *` and answers `OPTIONS` preflight (204). This is permissive by design given the intended integration shape (external dashboard's own backend holds the API key and calls this API server-to-server — never a browser calling directly with the key embedded). If the external dashboard's frontend ever needs to call this API directly from a browser, that would need the key threaded through the dashboard's own backend as a proxy instead; do not embed an API key in frontend JS under any circumstance.
- **Security tiers, current as of the last lockdown pass:** `/api/files`, `/api/users`, `/api/commands`, `/api/apikeys` are **fully admin-only**. `/api/motd`, `/api/rules`, `/api/shops`, `/api/holograms`, `/api/warps` are **mixed — GET = AUTH, every mutating route = ADMIN** (this was tightened from a previous "no admin check at all" state; if you're looking at an older copy of this doc, re-check). `/api/kits` is GET-only — there's nothing to lock down, the mod has no create/update/delete routes for kits at all. `/api/permissions`, `/api/admin`, `/api/teleport`, `/api/economy`, `/api/backup`, `/api/moderation`, `/api/cloud`, `/api/discord` are also **mixed** (read = AUTH, write = ADMIN) as tabled above. If building fine-grained UI permissions into the external dashboard, use these actual gates, not assumptions from the route name.
- **Binary/non-JSON responses:** `/api/files/download` (octet-stream), `/api/backup/download` (zip), `/api/shops/csv/export` (text/csv). `/api/shops/csv/import` takes a **raw CSV body**, not JSON.
- **Path-segment identity varies by endpoint group:** player GETs and moderation mutes/kicks/warns use **username**; moderation bans use **UUID**; economy accepts **either**; shops use an opaque **signKey**; permissions groups by **name**, users by **name** (offline-resolvable via Mojang API fallback).
