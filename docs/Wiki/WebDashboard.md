# Web Dashboard

> **Version:** 1.0.4+build.16 · **Config:** `config.json` → `webDashboard` section (dashboard on/off is controlled by **both** `webDashboard.enabled` **and** `modules.webDashboardEnabled` — either one set to `false` disables it)

---

## Overview

NeoEssentials runs an embedded HTTP server (plus a WebSocket server for live updates) exposing a
full REST API for server monitoring and administration. There are now **two ways to actually use
it**, controlled by `webDashboard.mode`:

- **`"external"` (the shipped default)** — the mod serves only `/api/*`; a separately-hosted
  dashboard app (the `NeoEssentials-Dashboard` Laravel project) is the actual browser UI, talking
  to this API from outside. See [API System](APISystem) and [`docs/API.md`](../API.md) for the
  full REST/WebSocket reference it's built against.
- **`"internal"` / `"both"`** — the mod serves its **own** bundled dashboard UI directly from `/`,
  no separate app to install at all. As of build.13 this internal UI is feature-complete and
  covers the same ground as the external app: Overview, Players (with a full per-player control
  page), Economy, Warps, Kits, Holograms, Discord, dashboard account management, Backups,
  Commands, Logs, Permissions, a public no-login player-lookup page, and (as of build.15) an
  account Settings page — see [Account Settings & Minecraft Account Linking](#account-settings--minecraft-account-linking)
  below. `"both"` is just an explicit alias for `"internal"`, useful when you deliberately run the
  built-in UI *and* point an external app at the same API at the same time.

The account system, permission model, and moderation/economy/etc. backing data described below
are shared by both dashboards — they're the same accounts and data either one authenticates
against and reads/writes through the API.

---

## Setup

1. Set `webDashboard.enabled: true` in `config.json` (default: `true`)
2. Configure `port` (default `8080`) and `websocketPort` (default `8081`)
3. Start the server — the dashboard auto-starts
4. Register a dashboard account in-game: `/dashboardregister start` then
   `/dashboardregister complete <username> <password>` (or `/dashboardregister discord` if
   Simple Discord Link is linked) — or use one of the other paths below
5. Open `http://<server-ip>:8080` in a browser and log in

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

1. **Default admin account** — a random 12-character temp password is generated and logged to the
   server console the first time the dashboard starts with no accounts yet (search for "Created
   default dashboard admin account"). You're required to set a new password on first login.
2. **Minecraft-permission login (self-service, works offline, deprecated)** — on the login page,
   authenticate with just your Minecraft username (no password). The server checks whether that
   player has `neoessentials.dashboard.access` and auto-creates an account with role assigned
   from `neoessentials.dashboard.admin`/`.moderator`/`.access`. Predates the `/dashboardregister`
   flow above and is marked deprecated in server logs, but still functional.
3. **Discord OAuth (optional, self-service)** — see below. If `allowAutoRegistration: true`
   (the default), logging in with Discord auto-creates an account on first use.
4. **Admin-created accounts** — an existing admin can create accounts for other players via the
   dashboard's own Users management page, or `POST /api/users/create`.

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
| `mode` | `"external"` | `"external"` serves only `/api/*` (default — use with the separate `NeoEssentials-Dashboard` app). `"internal"`/`"both"` also serves the mod's own bundled dashboard UI at `/` — see [Dashboard Connectivity](DashboardConnectivity) |
| `userSyncWebhookUrl` | `""` (disabled) | Optional: POST a notification here whenever a dashboard_user is created/updated/deleted, so an external dashboard can mirror the change into its own user table. See `docs/API.md` |
| `userSyncWebhookSecret` | `""` | HMAC-SHA256 secret for signing the webhook above (`X-NeoEssentials-Signature` header) |
| `securitySettings.requireAuthentication` | `true` | Require a Bearer token (session or API key) on dashboard API endpoints |
| `securitySettings.enableRateLimiting` | `true` | Enable per-IP rate limiting on the dashboard API |
| `securitySettings.maxRequestsPerMinute` | `60` | Max API requests per IP per minute when rate limiting is enabled |
| `securitySettings.publicModerationLookupEnabled` | `true` | Enable the no-login `/api/public/moderation/*` routes (see below) |
| `roleSync.enabled` | `false` | Opt-in: automatically sync a linked player's dashboard role from their real in-game permission — see [Dashboard Security](#dashboard-security) above |
| `roleSync.intervalSeconds` | `300` | How often the periodic reconciliation sweep runs, in addition to an immediate check on join |
| `roleSync.adminPermission` | `"neoessentials.admin.dashboard"` | A player with this permission node is granted the dashboard `ADMIN` role |
| `roleSync.adminGroup` | `"admin"` | A player in this permission group is granted the dashboard `ADMIN` role (checked in addition to, i.e. OR'd with, `adminPermission`) |

The dashboard's on/off switch is now split across two keys that both have to allow it:
`webDashboard.enabled` (this section) **and** `modules.webDashboardEnabled` (the mod-wide
`modules` block at the top of `config.json`, alongside `economyEnabled`/`chatEnabled`/etc.).
Either one set to `false` disables the whole dashboard.

> The `apiSettings`, `uiSettings`, `loggingSettings`, `enableCORS`, and `maxThreads` keys from
> earlier documentation do not exist in the current codebase — the dashboard's security-related
> settings live entirely under `securitySettings`, and there is no config option to disable REST
> endpoints or config editing independently.

---

## What the Dashboard Can Do

Both the internal (bundled) and external (Laravel) dashboards cover the same 13 pages, since both
are built against the same REST API: Overview (`/api/stats` + `/api/server`), Players + full
per-player control page (`/api/player` + `/api/moderation`), Economy, Warps, Kits, Holograms,
Discord, dashboard account management (`/api/users`), Backups, Commands/console (`/api/commands`),
Logs (`/api/logging`), Permissions (`/api/permissions`), and a public no-login player-lookup page.
Full endpoint detail in [API System](APISystem) → `docs/API.md`.

### Account Settings & Minecraft Account Linking

Both dashboards have an account Settings/Profile page (the internal one gained this in build.15;
it was previously just a disabled "not yet ported" placeholder) covering:

- **Change password** — same on both dashboards.
- **Link a Minecraft account** — new this pass, and the first way to do this *without* needing
  Discord: the Settings page generates a short one-time code, and you type
  `/linkaccount <code>` in-game to confirm ownership. Works for any dashboard account regardless
  of how it was created (self-registered, admin-created, or via `/dashboardregister`). Self-service
  unlink is also available. See `docs/API.md`'s `/api/auth/link-minecraft/*` routes.
- **Discord status** — read-only on the internal dashboard (resolved via whichever Discord
  companion mod is installed — see [below](#discord-linked-dashboard-identity)); the external
  dashboard keeps its existing full OAuth2 "Connect Discord" flow. A genuine browser-initiated
  Discord OAuth2 flow for the internal dashboard (so it gets the same "Connect Discord" button) is
  a planned follow-up, not built yet — the mod deliberately doesn't perform Discord OAuth2 itself
  today (see `DiscordAuthProvider`'s own doc comment).
- Once linked, the sidebar's user avatar on both dashboards shows your actual Minecraft skin
  instead of a generic icon.

### Dashboard Security

- `/apikey create` now prints the generated token as a click-to-copy chat component instead of
  plain text.
- The external dashboard's paired auth token (the Bearer token it presents on its outbound
  account-sync webhook) is now encrypted at rest in `config.json` (AES-256-GCM) instead of stored
  in plaintext — existing plaintext values are migrated transparently the next time they're read,
  no action needed.
- New opt-in setting, `webDashboard.roleSync` (off by default): automatically grants a linked
  player's dashboard account the `ADMIN` role the moment they have a configured in-game permission
  node or belong to a configured permission group, and revokes it again the moment they don't — no
  more manually re-running `/apikey create`/the API every time an admin's in-game status changes.
  Runs an immediate check on join plus a periodic sweep, so it also catches permission changes made
  outside the mod entirely (e.g. a direct LuckPerms edit). Never touches a role that was set
  manually — it only ever adjusts a role it granted itself.

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

All routes require the standard dashboard Bearer-token authentication; mutating routes (POST/DELETE) additionally require the moderator or admin dashboard role.

### In-Chat "View Profile" Link

Player names in chat show a small clickable `↗` icon that opens that player's public lookup
page in a browser. By default this points at whichever NeoEssentials dashboard is actually
reachable — the paired external (Laravel) dashboard if you've run `/dashboard pair`, else the
mod's own bundled UI via `webDashboard.publicUrl`.

If you have your own website instead (a stats page, forum profile, custom fan site), set
`webDashboard.customProfileUrlTemplate` to point the link there instead — it takes priority over
both of the above when set:

```json
"customProfileUrlTemplate": "https://myserver.com/players/{player}"
```

Supports `{player}` (URL-encoded username) and `{uuid}` placeholders. Leave empty (the default)
to keep using one of the two dashboards.

---

## Commands

All `/dashboard` subcommands (including the bare `/dashboard`) require `neoessentials.admin.dashboard`.
Account registration is a **separate** command tree, `/dashboardregister`, gated by
`neoessentials.dashboard.access`.

| Command | Permission | Description |
|---|---|---|
| `/dashboard` | `neoessentials.admin.dashboard` | Show dashboard status, URL, and installed file version |
| `/dashboard start` | `neoessentials.admin.dashboard` | Start dashboard if stopped |
| `/dashboard stop` | `neoessentials.admin.dashboard` | Stop the dashboard |
| `/dashboard restart` | `neoessentials.admin.dashboard` | Restart the dashboard |
| `/dashboard status` | `neoessentials.admin.dashboard` | Show dashboard status |
| `/dashboard url` | `neoessentials.admin.dashboard` | Show the dashboard URL |
| `/dashboard update [check\|force]` | `neoessentials.admin.dashboard` | Update bundled dashboard files from the JAR (`check` = dry-run, `force` = bypass checksum) |
| `/dashboardregister` | `neoessentials.dashboard.access` | Show registration help |
| `/dashboardregister start` | `neoessentials.dashboard.access` | Begin manual registration (issues a 5-minute token) |
| `/dashboardregister complete <username> <password>` | `neoessentials.dashboard.access` | Finish manual registration |
| `/dashboardregister discord` | `neoessentials.dashboard.access` | Register instantly using a linked Discord account (via SDLink, Mc2Discord, or DCIntegration) |
| `/dashboardregister status` | `neoessentials.dashboard.access` | Check your registration status |
| `/apikey create <label> [role]` | `neoessentials.dashboard.apikeys` | Create an API key for an external dashboard backend — prints the token once, as a click-to-copy chat component |
| `/apikey list` | `neoessentials.dashboard.apikeys` | List keys (label/role/enabled/last-used — never the secret) |
| `/apikey revoke <id>` | `neoessentials.dashboard.apikeys` | Revoke a key immediately |
| `/linkaccount <code>` | none — open to everyone | Finish linking your Minecraft account to a dashboard account, using the code shown on that account's Settings page |

`/apikey` is a **separate** command tree from `/dashboard`/`/dashboardregister` — see
[API System → Authentication](APISystem#authentication) for the full picture of what these
keys are for and how they differ from a human dashboard account. `/linkaccount` is the reverse
direction of `/dashboardregister` — it links an MC account to an *existing* dashboard account
(started from the dashboard's own Settings page), rather than creating a brand-new dashboard
account starting from an in-game player.

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `neoessentials.dashboard.access` | 🔒 | Register an account and log in to the dashboard (`/dashboardregister`) |
| `neoessentials.dashboard.view` | 🔒 | View dashboard (read-only) |
| `neoessentials.dashboard.manage` | 🔒 | Access console and management tools |
| `neoessentials.dashboard.moderator` | 🔒 | Moderator-level dashboard access |
| `neoessentials.dashboard.admin` | 🔒 | Full admin dashboard access |
| `neoessentials.admin.dashboard` | 🔒 | Start/stop/manage the dashboard server (`/dashboard`) |

> Unlike most permission nodes documented on the [Permission System](PermissionSystem) page,
> the dashboard nodes above (other than `neoessentials.dashboard.apikeys`/`.pair`, which **are**
> registered) are not pre-registered in `PermissionRegistry` — they're checked ad hoc, so grant
> them explicitly to non-OP groups rather than relying on a documented default.
>
> As of build.16, `/help <command>` shows each command's **real** permission node (this table
> included) instead of guessing `neoessentials.<commandname>` — that guess used to be wrong for
> `/apikey` specifically (it showed `neoessentials.apikey` instead of the real
> `neoessentials.dashboard.apikeys`), and for roughly 160 other commands mod-wide.

---

## File Auto-Update

Dashboard HTML/JS/CSS files are versioned. On every server start, NeoEssentials checks if the bundled dashboard files in the JAR are newer than the deployed files on disk — if so, they are automatically updated. Customised files will be overwritten if the version number increases.

---

*Back to [Wiki Home](Home)*
