# Web Dashboard

> **Version:** 1.0.2.6 · **Config:** `config.json` → `webDashboard` section

---

## Overview

NeoEssentials ships a built-in web dashboard for server monitoring and administration. It runs on an embedded HTTP server with WebSocket support for real-time updates. No external software required.

---

## Setup

1. Set `webDashboard.enabled: true` in `config.json` (default: `true`)
2. Configure `port` (default `8080`) and `websocketPort` (default `8081`)
3. Start the server — the dashboard auto-starts
4. Register a dashboard account in-game: `/dashboardregister start` then
   `/dashboardregister complete <username> <password>` (or `/dashboardregister discord` if
   Simple Discord Link is linked)
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

Or, if Simple Discord Link is installed and the player has linked their Discord account:

```
/dashboardregister discord
```

Requires permission `neoessentials.dashboard.access`. Registration tokens from `start` expire
after 5 minutes; passwords must be at least 8 characters. Run `/dashboardregister status` to
check your current registration state. After registering, the player can log in from the web
browser at any time, even when offline.

### Discord Auth (Optional)

If **Simple Discord Link** is installed and configured, players can also authenticate via Discord. The mod is fully optional — standalone account registration works without it.

---

## Discord OAuth2 Login

NeoEssentials supports logging into the dashboard directly with a Discord account via OAuth2 ("Login with Discord" button). This is separate from — and works alongside — the standard username/password login.

### How It Works

```
Browser                Dashboard Server              Discord
  |                          |                           |
  |-- click Discord Login --> |                           |
  |<-- authorizeUrl ----------|                           |
  |-- redirect ---------------------------------------------> Discord consent screen
  |<----------------------------------------------------- redirect back with ?code=
  |                  /api/auth/discord/callback           |
  |              1. Exchange code → access token          |
  |              2. Fetch Discord user (/users/@me)       |
  |              3. Check blacklist / whitelist roles     |
  |              4. Map Discord roles → dashboard role    |
  |              5. Lookup linked MC account (SDLink)     |
  |              6. Get-or-create dashboard user          |
  |              7. Create session, redirect to dashboard |
  |<-- redirect to /index.html?sessionId=...&auth=discord |
```

### Discord Application Setup

1. Go to **https://discord.com/developers/applications** and create a new application (or open your bot's existing one)
2. Under **OAuth2 → General**, copy the **Client ID** and **Client Secret**
3. Under **OAuth2 → Redirects**, add the exact callback URL:
   ```
   http://YOUR_SERVER_IP:8080/api/auth/discord/callback
   ```
   Replace `YOUR_SERVER_IP` with your server's public IP or domain and `8080` with your dashboard port.
4. Save your changes in the Discord Developer Portal

### Config (`discord_auth.json`)

Located at `config/neoessentials/discord_auth.json`. Auto-generated on first start.

#### OAuth2 section

| Key | Description |
|---|---|
| `oauth2.clientId` | Discord Application Client ID |
| `oauth2.clientSecret` | Discord Application Client Secret (**keep private**) |
| `oauth2.redirectUri` | Must exactly match the redirect URI registered in your Discord app |
| `oauth2.scopes` | OAuth2 scopes — default: `identify guilds.members.read` (do not change) |

#### Auth behavior

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable Discord auth (requires `clientId` + `clientSecret` to actually work) |
| `requireLinkedAccount` | `true` | Require the Discord user to have a linked Minecraft account via SDLink |
| `allowAutoRegistration` | `true` | Auto-create a dashboard account on first Discord login |
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

### Without Simple Discord Link

If SDLink is **not** installed, the OAuth2 flow still works for identification, but:
- `requireLinkedAccount: true` → login will be denied (no Minecraft account can be looked up)
- `requireLinkedAccount: false` → login succeeds; dashboard username = Discord display name

Set `requireLinkedAccount: false` in `discord_auth.json` to allow Discord-only accounts.

### Troubleshooting

| Symptom | Likely Cause |
|---|---|
| Discord button missing on login page | `clientId` / `clientSecret` not set in `discord_auth.json` |
| `discord_auth_failed` error after redirect | Wrong `redirectUri` in config or Discord app |
| "no linked account" error | SDLink not installed or player hasn't used `/link` in Discord |
| "does not have a whitelisted role" | User doesn't hold one of the `whitelistedRoles` IDs |
| Button shows warning tooltip | SDLink absent but `requireLinkedAccount: true` |

---

## Config (`config.json` → `webDashboard`)

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable the dashboard system |
| `autoStart` | `true` | Start dashboard on server boot. If `false`, start it manually with `/dashboard start` |
| `port` | `8080` | HTTP port |
| `websocketPort` | `8081` | WebSocket port for live updates |
| `bindAddress` | `"0.0.0.0"` | IP to bind (use `127.0.0.1` for local-only) |
| `securitySettings.requireAuthentication` | `true` | Require a Bearer token on dashboard API endpoints |
| `securitySettings.enableRateLimiting` | `true` | Enable per-IP rate limiting on the dashboard API |
| `securitySettings.maxRequestsPerMinute` | `60` | Max API requests per IP per minute when rate limiting is enabled |

> The `apiSettings`, `uiSettings`, `loggingSettings`, `enableCORS`, and `maxThreads` keys from
> earlier documentation do not exist in the current codebase — the dashboard's security-related
> settings live entirely under `securitySettings`, and there is no config option to disable REST
> endpoints or config editing independently.

---

## Dashboard Pages

| Page | URL | Permission | Description |
|---|---|---|---|
| Overview | `/` | `neoessentials.dashboard.view` | Server stats, player count, TPS, memory |
| Players | `/players` | `neoessentials.dashboard.view` | Online players, ban/kick/tp actions |
| Console | `/console` | `neoessentials.dashboard.manage` | View logs, send commands |
| Admin Controls | `/admin` | `neoessentials.dashboard.admin` | Server admin tools |
| Permissions | `/permissions` | `neoessentials.dashboard.admin` | Manage permission groups and nodes |
| Config | `/config` | `neoessentials.dashboard.admin` | Edit config files (file writes require the admin role — there is no separate config toggle to disable this) |

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
| `/dashboardregister discord` | `neoessentials.dashboard.access` | Register instantly using a linked Discord account (SDLink) or the OAuth2 web flow |
| `/dashboardregister status` | `neoessentials.dashboard.access` | Check your registration status |

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
> the dashboard nodes above are **not** pre-registered in `PermissionRegistry` — they're checked
> ad hoc, so grant them explicitly to non-OP groups rather than relying on a documented default.

---

## File Auto-Update

Dashboard HTML/JS/CSS files are versioned. On every server start, NeoEssentials checks if the bundled dashboard files in the JAR are newer than the deployed files on disk — if so, they are automatically updated. Customised files will be overwritten if the version number increases.

---

*Back to [Wiki Home](Home)*
