# Web Dashboard

> **Version:** 1.0.3+build.14 · **Config:** `config.json` → `webDashboard` section (dashboard on/off is now controlled by **both** `webDashboard.enabled` **and** `modules.webDashboardEnabled` — either one set to `false` disables it)

---

## Overview

NeoEssentials runs an embedded HTTP server (plus a WebSocket server for live updates) exposing a
full REST API for server monitoring and administration. **There is no bundled dashboard UI
shipped anymore** — the mod's own dashboard runs REST-only, and a separately-hosted external
dashboard app is the actual interface. The shipped `config.json` template already sets
`webDashboard.mode: "external"` to match this — see [API System](APISystem) and
[`docs/API.md`](../API.md) for the full REST/WebSocket reference an external dashboard is built
against.

The account system, permission model, and moderation/economy/etc. backing data described below
are unaffected by this — they're the same accounts and data an external dashboard authenticates
against and reads/writes through the API.

---

## Setup

1. Set `webDashboard.enabled: true` **and** `modules.webDashboardEnabled: true` in `config.json` (both default to `true`)
2. Configure `port` (default `8080`) and `websocketPort` (default `8081`)
3. Start the server — the dashboard's REST API auto-starts
4. Create a credential for whatever's going to call the API:
   - **An external dashboard backend (recommended path):** create an API key in-game —
     `/apikey create <label> [role]` — and give the printed token to that dashboard's server
     config. See [API System → Authentication](APISystem#authentication) for the full picture.
   - **A human dashboard account:** register in-game via `/dashboardregister start` then
     `/dashboardregister complete <username> <password>` (or `/dashboardregister discord` if
     Simple Discord Link is linked) — or use one of the other paths below.
5. Point your external dashboard app at `http://<server-ip>:<port>/api/...` (`port` from step 2)
   using the API key or session token as a Bearer token.

---

## Account Registration

Players register their dashboard account **in-game**, via the separate `/dashboardregister`
command (not a subcommand of `/dashboard`) — they do not need to be online at login time after
registering.

```
/dashboardregister start
/dashboardregister complete <username> <password>
```

Or, if Simple Discord Link, Mc2Discord, or DCIntegration is installed and the player has linked their Discord account:

```
/dashboardregister discord
```

Requires permission `neoessentials.dashboard.access`. Registration tokens from `start` expire
after 5 minutes; passwords must be at least 8 characters. Run `/dashboardregister status` to
check your current registration state.

**Other ways to get dashboard access:**

1. **API key (for an external dashboard backend, not a human)** — `/apikey create <label>
   [role]` in-game, or `POST /api/apikeys` once a first key exists. Not a login account at all;
   see [API System](APISystem) and `docs/API.md`.
2. **Default admin account** — the first time the dashboard starts with no accounts at all, an
   `admin` account is auto-created with a **random** temporary password, printed once to the
   server console/log (`Temporary password: ...`) and flagged to require a password change on
   first login. There is no fixed default password — check the boot log if you've lost it, or
   reset it via `/api/users/{id}/password` (admin session) if you still have another admin
   account.
3. **Minecraft-permission login (self-service, works offline, deprecated)** — on the login page,
   authenticate with just your Minecraft username (no password). The server checks whether that
   player has `neoessentials.dashboard.access` and auto-creates an account with role assigned
   from `neoessentials.dashboard.admin`/`.moderator`/`.access`. Predates the `/dashboardregister`
   flow above and is marked deprecated in server logs, but still functional.
4. **Discord OAuth (optional, self-service)** — see below. If `allowAutoRegistration: true`
   (the default), logging in with Discord auto-creates an account on first use.
5. **Admin-created accounts** — an existing admin can create accounts directly via
   `POST /api/users/create` (or whatever user-management screen the external dashboard exposes
   for it), or push-sync one from the external dashboard's own user table with
   `POST /api/users/sync` — see `docs/API.md`.

### Discord Auth (Optional)

If a supported Discord companion mod — **Simple Discord Link** or **Mc2Discord** — is installed, players can link
their Discord account, and the dashboard can source their Discord identity and roles from it. The mod is fully
optional — standalone account registration works without it.

---

## Discord-Linked Dashboard Identity

NeoEssentials does **not** talk to Discord's API directly — no bot token, no OAuth2 code exchange, no REST calls to
`discord.com`. All Discord communication is delegated entirely to whichever companion mod is installed
(Simple Discord Link or Mc2Discord); NeoEssentials only reads the account-link data that mod already has.

### How It Works

```
Player links their account in-game (via SDLink's or Mc2Discord's own commands)
         │
         ▼
ChatIntegrationManager.findLinkedDiscordId(uuid)  ── checks whichever companion mod is ready
         │
         ▼
DiscordAuthProvider  ── resolves Discord ID + role IDs for that player
         │
         ▼
Dashboard: GET /api/auth/discord?username=<name>
  1. Look up the linked Discord account
  2. Check blacklist / whitelist roles
  3. Map Discord roles → dashboard role
  4. Get-or-create dashboard user, create session
```

There is no "Login with Discord" OAuth2 button — a player must first link their account in-game through
Simple Discord Link's or Mc2Discord's own linking flow.

### Companion mods

| Mod | Chat relay | Account link lookup | Notes |
|---|---|---|---|
| Simple Discord Link (SDLink) | ✅ | ✅ | Real compile-time API (`com.hypherionmc.sdlink.api.*`), also requires CraterLib |
| Mc2Discord | ✅ | ✅ | Real compile-time API (`fr.denisd3d.mc2discord.core.*`) |
| DCIntegration | — (self-relays) | ✅ | Real compile-time API (`de.erdbeerbaerlp.dcintegration.common.*`). Relays chat/join/leave/commands to Discord on its own via mixins — NeoEssentials doesn't push those events to it, only reads account links |
| DiscordSRV | ❌ | ❌ | Not supported — it's a Bukkit/Paper plugin and cannot run on a NeoForge server |

Detection is purely runtime (`ModList.isLoaded(...)`) — neither mod is a hard dependency, and NeoEssentials works
fine standalone if neither is installed.

### Config (`discord_auth.json`)

Located at `config/neoessentials/discord_auth.json`. Auto-generated on first start.

| Key | Default | Description |
|---|---|---|
| `enabled` | `false` | Enable Discord-linked dashboard identity/login |
| `requireLinkedAccount` | `true` | Require the player's Minecraft account to actually be linked to Discord |
| `allowAutoRegistration` | `true` | Auto-create a dashboard account on first Discord-linked login |
| `defaultRole` | `VIEWER` | Dashboard role given when no Discord role is mapped |

#### Role mapping

Discord role IDs (not names) are mapped to dashboard roles. Enable **Developer Mode** in Discord Settings → Advanced, then right-click a role → **Copy ID**.

```json
"roleMapping": {
  "123456789012345678": "ADMIN",
  "234567890123456789": "MODERATOR",
  "345678901234567890": "VIEWER"
}
```

#### Whitelist / Blacklist

```json
"whitelistedRoles":  ["123456789012345678"],   // only these role IDs can log in (empty = everyone)
"blacklistedUsers":  ["987654321098765432"]    // these Discord user IDs are always denied
```

### Without a Discord companion mod

If none of SDLink, Mc2Discord, or DCIntegration is installed, `GET /api/auth/discord` returns `503` — Discord-linked
login is unavailable entirely (there's no account-link data to read). Standalone username/password login
is unaffected.

### Troubleshooting

| Symptom | Likely Cause |
|---|---|
| `GET /api/auth/discord/status` reports `linkAdapterAvailable: false` | No supported companion mod is installed, or its bot connection isn't ready yet |
| "No Discord account linked" error | Player hasn't linked their account in-game via the companion mod's own command |
| "does not have a whitelisted role" | User doesn't hold one of the `whitelistedRoles` IDs |

---

## Config (`config.json` → `webDashboard`)

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable the dashboard system |
| `autoStart` | `true` | Start dashboard on server boot. If `false`, start it manually with `/dashboard start` |
| `port` | `8080` | HTTP port |
| `websocketPort` | `8081` | WebSocket port for live updates |
| `bindAddress` | `"0.0.0.0"` | IP to bind (use `127.0.0.1` for local-only) |
| `mode` | `"external"` | The shipped config template sets this explicitly (there's no bundled UI to serve anymore). `"internal"`/`"both"` would serve a bundled static UI if `mode` were absent entirely (code-level fallback only — no such UI files actually ship); `"external"` serves only `/api/*`, which is the supported setup. See [Dashboard Connectivity](DashboardConnectivity) |
| `userSyncWebhookUrl` | `""` (disabled) | Optional: POST a notification here whenever a dashboard_user is created/updated/deleted, so an external dashboard can mirror the change into its own user table. See `docs/API.md` |
| `userSyncWebhookSecret` | `""` | HMAC-SHA256 secret for signing the webhook above (`X-NeoEssentials-Signature` header) |
| `securitySettings.requireAuthentication` | `true` | Require a Bearer token (session or API key) on dashboard API endpoints |
| `securitySettings.enableRateLimiting` | `true` | Enable per-IP rate limiting on the dashboard API |
| `securitySettings.maxRequestsPerMinute` | `60` | Max API requests per IP per minute when rate limiting is enabled |
| `securitySettings.publicModerationLookupEnabled` | `true` | Enable the no-login `/api/public/moderation/*` routes (see below) |

The dashboard's on/off switch is now split across two keys that both have to allow it:
`webDashboard.enabled` (this section) **and** `modules.webDashboardEnabled` (the mod-wide
`modules` block at the top of `config.json`, alongside `economyEnabled`/`chatEnabled`/etc.).
Either one set to `false` disables the whole dashboard.

> The `apiSettings`, `uiSettings`, `loggingSettings`, `enableCORS`, and `maxThreads` keys from
> earlier documentation do not exist in the current codebase — the dashboard's security-related
> settings live entirely under `securitySettings`, and there is no config option to disable REST
> endpoints or config editing independently.

---

## What the External Dashboard Can Do

There's no bundled UI to screenshot/reference here anymore — the equivalent of the old
"Overview / Players / Console / Admin Controls / Permissions / Config" pages is now just REST
endpoint groups the external dashboard's own UI calls: `/api/stats` + `/api/server` (overview),
`/api/player` + `/api/moderation` (players), `/api/commands` + `/api/logging` (console),
`/api/admin` (admin controls), `/api/permissions` (permissions), `/api/files` (config editing).
Full detail on every one of these in [API System](APISystem) → `docs/API.md`.

---

## Moderation API

The dashboard's moderation endpoints are backed directly by the same manager classes that power the in-game `/ban`, `/mute`, `/kick`, `/warn`, `/note`, and `/report` commands (see [Moderation System](ModerationSystem)) — there is no separate dashboard-only ban store, so actions taken here actually enforce in-game and vice versa. The dashboard's own accounts (`dashboard_users`, `dashboard_registrations`) are themselves persisted through the same pluggable backend — see [Storage Backend](Storage).

| Route | Method | Description |
|---|---|---|
| `/api/moderation/overview` | GET | Summary counts across all moderation categories |
| `/api/moderation/bans/active`, `/bans`, `/bans/{uuid}` | GET | Active bans / all bans / a player's ban history |
| `/api/moderation/ban` | POST | Issue a ban |
| `/api/moderation/ban/{uuid}` | DELETE | Unban a player |
| `/api/moderation/ipbans/active`, `/ipbans` | GET | Active IP bans / all IP ban history |
| `/api/moderation/ipban` | POST | Issue an IP ban |
| `/api/moderation/ipban/{ip}` | DELETE | Unban an IP |
| `/api/moderation/mutes/active`, `/mutes`, `/mutes/{name}` | GET | Active mutes / all mutes / a player's mute history |
| `/api/moderation/mute` | POST | Issue a mute |
| `/api/moderation/mute/{name}` | DELETE | Unmute a player |
| `/api/moderation/ipmutes` | GET | All IP mutes |
| `/api/moderation/ipmute` | POST | Issue an IP mute |
| `/api/moderation/ipmute/{ip}` | DELETE | Remove an IP mute |
| `/api/moderation/kicks`, `/kicks/{name}` | GET | Kick history (all / one player) |
| `/api/moderation/warns`, `/warns/{name}` | GET | Warnings (all / one player) |
| `/api/moderation/warn/{id}` | DELETE | Remove a warning |
| `/api/moderation/notes/{name}` | GET | A player's staff notes |
| `/api/moderation/note` | POST | Add a staff note |
| `/api/moderation/note/{id}` | DELETE | Remove a staff note |
| `/api/moderation/reports`, `/reports/all`, `/reports/{id}` | GET | Pending / all / one report |
| `/api/moderation/reports/{id}/review` | POST | Accept or dismiss a report |

All routes above require the standard dashboard Bearer-token authentication (session or API key); mutating routes (POST/DELETE) additionally require the **ADMIN** role specifically — a MODERATOR-role credential can read but not act.

### Public Moderation Lookup (no login required)

A separate, unauthenticated set of routes — matching ban-management plugins' public
transparency page, where anyone can look up a player's punishment history without an account:

| Route | Method | Description |
|---|---|---|
| `/api/public/moderation/lookup/{name}` | GET | Bans, mutes, kicks, and warns for one player, by name |
| `/api/public/moderation/recent` | GET | Recent active bans + mutes across all players, newest first |

These deliberately **never** expose IP bans, IP mutes, staff notes, or player reports (privacy —
notes/reports contain staff commentary and a reporter's identity). Still CORS/rate-limited the
same as every other dashboard route, just without the Bearer-token check. Gated by
`webDashboard.securitySettings.publicModerationLookupEnabled` (default `true`) — set to `false`
to disable public lookup entirely.

---

## Commands

All `/dashboard` subcommands (including the bare `/dashboard`) require `neoessentials.admin.dashboard`.
Account registration is a **separate** command tree, `/dashboardregister`, gated by
`neoessentials.dashboard.access`.

| Command | Permission | Description |
|---|---|---|
| `/dashboard` | `neoessentials.admin.dashboard` | Show the REST API + WebSocket server's status |
| `/dashboard start` | `neoessentials.admin.dashboard` | Start the API server if stopped |
| `/dashboard stop` | `neoessentials.admin.dashboard` | Stop the API server |
| `/dashboard restart` | `neoessentials.admin.dashboard` | Restart the API server |
| `/dashboard status` | `neoessentials.admin.dashboard` | Show the API server's status |
| `/dashboard url` | `neoessentials.admin.dashboard` | Show the API base URL |
| `/dashboard pair "<dashboardUrl>" <code>` | `neoessentials.dashboard.pair` | Complete the pairing handshake with an external dashboard — see [API System → Pairing](APISystem#pairing) |
| `/dashboard unpair` | `neoessentials.dashboard.pair` | Clear the paired connection and revoke its API key |
| `/dashboardregister` | `neoessentials.dashboard.access` | Show registration help |
| `/dashboardregister start` | `neoessentials.dashboard.access` | Begin manual registration (issues a 5-minute token) |
| `/dashboardregister complete <username> <password>` | `neoessentials.dashboard.access` | Finish manual registration |
| `/dashboardregister discord` | `neoessentials.dashboard.access` | Register instantly using a linked Discord account (via SDLink, Mc2Discord, or DCIntegration) |
| `/dashboardregister status` | `neoessentials.dashboard.access` | Check your registration status |
| `/apikey create <label> [role]` | `neoessentials.dashboard.apikeys` | Create an API key for an external dashboard backend — prints the token once |
| `/apikey list` | `neoessentials.dashboard.apikeys` | List keys (label/role/enabled/last-used — never the secret) |
| `/apikey revoke <id>` | `neoessentials.dashboard.apikeys` | Revoke a key immediately |

`/apikey` is a **separate** command tree from `/dashboard`/`/dashboardregister` — see
[API System → Authentication](APISystem#authentication) for the full picture of what these
keys are for and how they differ from a human dashboard account.

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `neoessentials.dashboard.access` | 🔒 | Register an account and log in to the dashboard (`/dashboardregister`) |
| `neoessentials.dashboard.view` | 🔒 | View dashboard (read-only) |
| `neoessentials.dashboard.manage` | 🔒 | Access console and management tools |
| `neoessentials.dashboard.moderator` | 🔒 | Moderator-level dashboard access |
| `neoessentials.dashboard.admin` | 🔒 | Full admin dashboard access |
| `neoessentials.admin.dashboard` | 🔒 | Start/stop/manage the API server (`/dashboard`) |
| `neoessentials.dashboard.apikeys` | 🔒 | Create/list/revoke API keys for external dashboard integrations (`/apikey`) |
| `neoessentials.dashboard.pair` | 🔒 | Pair/unpair this server with an external dashboard (`/dashboard pair`, `/dashboard unpair`) |

> Unlike most permission nodes documented on the [Permission System](PermissionSystem) page,
> the dashboard nodes above (other than `neoessentials.dashboard.apikeys`/`.pair`, which **are**
> registered) are not pre-registered in `PermissionRegistry` — they're checked ad hoc, so grant
> them explicitly to non-OP groups rather than relying on a documented default.

---

*Back to [Wiki Home](Home)*
