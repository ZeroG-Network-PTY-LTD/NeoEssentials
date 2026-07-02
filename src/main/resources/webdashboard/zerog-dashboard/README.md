# ZeroG Network — Essentials dashboard

Self-hosted Laravel + Inertia + React dashboard that controls a Minecraft
server through the mod's embedded HTTP API. This is a scaffold to drop into
an existing Laravel 11 app (matches the ZeroG Network stack: Inertia v2,
TypeScript, Tailwind, MySQL, Redis).

## Where these files go

Copy each folder into the matching path in your existing Laravel project:

```
config/minecraft.php              → config/minecraft.php
app/Services/...                  → app/Services/
app/Http/Controllers/...          → app/Http/Controllers/
routes/web.php                    → merge into your existing routes/web.php
resources/js/Layouts/...          → resources/js/Layouts/
resources/js/Pages/Dashboard/...  → resources/js/Pages/Dashboard/
resources/js/types/...            → resources/js/types/
resources/css/theme.css           → resources/css/, then @import it from app.css
```

## Environment

Add to `.env`:

```
MC_API_URL=http://127.0.0.1:8080
MC_SERVICE_USERNAME=dashboard-service
MC_SERVICE_PASSWORD=a-strong-password-here
MC_API_TIMEOUT=4
MC_API_CACHE_TTL=3
MC_SESSION_CACHE_TTL=1500
```

The mod's dashboard API (`webDashboard.port` in the mod's `config.json`,
default `8080`) uses **session-based auth**, not a static shared token — log
in via `POST /api/auth/login` to get a session id, which then expires after
30 minutes of inactivity. `MinecraftApiService` handles this transparently
(logs in once, caches the session, re-authenticates on expiry/401), but it
needs its own dashboard user account to do so:

1. Start the Minecraft server once so the mod bootstraps its default admin
   account (`admin` / `admin123` — **change this password immediately**,
   it's meant only to create other accounts).
2. Log into the mod's own web dashboard (`http://<server-ip>:8080`) as that
   admin, or call the API directly:
   ```
   POST /api/auth/login          {"username": "admin", "password": "admin123"}
   POST /api/auth/users          {"username": "dashboard-service", "password": "...", "role": "ADMIN"}
   ```
   Use `role: "MODERATOR"` instead if you don't want this Laravel app able to
   run raw console commands or adjust economy balances.
3. Put those service-account credentials in `.env` above. Never sent to the
   browser — only `MinecraftApiService` (server-side) uses them.

## Frontend dependencies

```
npm install lucide-react
```

Everything else (Inertia, Tailwind, TypeScript) is assumed already present
per the existing ZeroG Network setup.

## Permissions

Routes for kick/ban/mute, economy adjustments, and raw commands are gated
behind `can:` middleware (`players.kick`, `players.ban`, `players.mute`,
`economy.manage`, `console.run`). Wire these to Spatie Laravel Permission
roles so moderators and admins get different capability sets — the scaffold
assumes that package is already in the project.

## Design notes

Dark slate background rather than near-black, copper as the primary action
color, moss green for online/healthy state, ember red reserved only for
destructive actions (kick/ban/take money) so it stays meaningful instead of
decorative. Coordinates, balances, commands, and logs all render in
JetBrains Mono — they're technical strings and read better as columns than
as proportional text. Space Grotesk carries page titles.

## Mod-side API contract (already implemented)

`MinecraftApiService` calls the mod's *actual* REST surface, not a
hypothetical one — the mod already ships a large dashboard API
(`DashboardAPI.java`); this scaffold's service class maps its shapes onto
what the Inertia pages expect:

| Laravel method | Mod endpoint |
|---|---|
| `status()` | `GET /api/server/status` + `GET /api/stats/performance` |
| `players()` | `GET /api/player/online` |
| `teleportPlayer()` / `healPlayer()` / `kickPlayer()` | `POST /api/player/{teleport,heal,kick}/{username}` |
| `banPlayer()` | `POST /api/moderation/ban` |
| `mutePlayer()` | `POST /api/moderation/mute` |
| `economyLeaderboard()` | `GET /api/stats/economy` |
| `economyAdjust()` | `POST /api/economy/{username-or-uuid}` |
| `warps()` / `createWarp()` / `deleteWarp()` | `GET/POST/DELETE /api/warps` |
| `homes()` | `GET /api/player/homes/{username}` |
| `runCommand()` | `POST /api/commands/execute` |
| `logs()` | `GET /api/game/events` (join/leave/block-break only — no chat/command log yet) |

Important: the mod's player-action endpoints are keyed by **username**, not
uuid, but the frontend only knows a player's uuid (the `McPlayer` primary
key). `PlayerController::resolveUsername()` bridges this by looking the uuid
up in the (cached) online-players list — which also means kick/ban/mute/
teleport/heal only work while the target is **online**, matching the mod's
own behavior for these specific endpoints.

## Known gaps to fill in before shipping

- Auth/login pages aren't included — reuses whatever ZeroG Network already has.
- No rate limiting on `/dashboard/commands/run` — add Laravel's throttle
  middleware before exposing this beyond trusted admins.
- Ban/mute confirmation modal in `Players.tsx` is a stub (`window.prompt` for
  kick reason, ban button doesn't yet open a real confirm dialog) — flagged
  in the component, worth replacing with a proper modal before production use.
- `logs()` only surfaces player join/leave and block-break events — the mod
  doesn't currently log chat messages or executed commands into the same
  event queue, so `LogEntryType`'s `'chat'` value never actually appears yet.
- `players()`'s `rank` field is approximated from operator status (`op` vs
  `player`) — the mod's real permission groups aren't cheap to look up for a
  bulk player list, so `'owner'`, `'mod'`, `'vip'` never get returned. Fine
  for now; revisit if per-player rank badges become important.
