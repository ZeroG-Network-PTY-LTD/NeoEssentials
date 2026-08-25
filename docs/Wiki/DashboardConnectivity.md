# Dashboard Connectivity — Internal vs. External Hosting

> Covers connecting a separately-hosted dashboard app (e.g. `NeoEssentials-Dashboard`, the
> Laravel app) to the mod's built-in API, diagnosing "the connection isn't establishing", and the
> `webDashboard.mode` config option (`"external"` vs. the mod's own bundled `"internal"` UI).

---

## The two hosting modes

The mod's dashboard has always been two things bundled together:
1. A **REST API** under `/api/*` (moderation, players, economy, permissions, etc.)
2. A **static UI** served at `/` — the mod's own bundled HTML/JS/CSS dashboard frontend

Until now there was no way to run just the API without the mod also trying (and, in most dev/
production setups, failing) to serve its own bundled UI — every boot logged
`Dashboard resources NOT found - /webdashboard/index.html is null!` even on servers that only
ever use an external dashboard app and never load `http://<server>:8080/` directly.

### Config: `config.json` → `webDashboard.mode`

| Value | Behavior |
|---|---|
| `"external"` (default) | Serves **only** `/api/*`. The `/` static-file route is never registered. Use this when a separate app (Laravel `NeoEssentials-Dashboard`) is the *only* thing that ever talks to this port — the shipped config template sets this explicitly. |
| `"internal"` | Serves the mod's own bundled dashboard UI at `/` **and** the API at `/api/*` — no separate app to install. As of build.13 this bundled UI is feature-complete (all 13 pages, matching the external app) — use this if you'd rather not run/maintain a separate Laravel process. |
| `"both"` | Explicit alias for `"internal"` — same behavior, for config clarity when you deliberately use the built-in UI *and* point an external app at the same API at the same time. |

Switching to `"internal"`/`"both"` requires the mod's Gradle build to have bundled the dashboard
UI assets (`webdashboard-ui/` → `build/generated/dashboardUiResources/`, wired into
`processResources` — this happens automatically for anyone building from source; official
released jars already include it).

Requires a restart to take effect (same as every other config toggle — see the "reload doesn't
add/remove commands" limitation documented elsewhere).

### New endpoint: `GET /api/ping`

Unauthenticated, always registered regardless of `mode`. Returns:
```json
{"success": true, "mod": "neoessentials", "mode": "internal"}
```
Use this to answer "can I reach the mod's HTTP port at all" as a separate question from "is the
dashboard actually paired/authenticated" — the two failure modes look identical from the outside
(both just time out or error) but need completely different fixes.

```bash
curl http://<mod-server-ip>:8080/api/ping
```
- **Connection refused / timeout** → network problem (see checklist below), not a mod bug.
- **200 with the JSON above** → the mod's API is reachable; if the dashboard app still can't
  connect, the problem is in the pairing/API key, not connectivity — see
  [API System → Pairing](APISystem#pairing).

---

## Historical root cause (already fixed, worth knowing about if you're on an old build)

The most common reason "the connection isn't establishing" used to come up at all: **`/dashboard
start`/`stop`/`status`/`restart` were dead code** — the class existed and compiled, but was never
actually registered with the command dispatcher, so they were literally unknown commands. Combined
with `webDashboard.autoStart: false`, this meant the dashboard's HTTP server could simply never be
running at all, with no in-game way to diagnose or fix it — no amount of correct `MC_API_URL`/
service-account config on the external dashboard's side would help, since nothing was listening on
the port in the first place. Fixed long since; only relevant if you're troubleshooting a very old build.

**Action item for anyone hitting this:** check `/dashboard status` in-game (after restarting to
pick up the fix) — if it says stopped, either set `autoStart: true` or run `/dashboard start`.

---

## Full diagnostic checklist for "external dashboard can't connect to the mod"

Work through these in order — each rules out one layer:

1. **Is the mod's dashboard actually running?**
   `/dashboard status` in-game. If stopped: `/dashboard start`, or set
   `webDashboard.enabled` / `modules.webDashboardEnabled` / `webDashboard.autoStart` to `true`
   and restart.

2. **Can you reach the port from the SAME machine the Minecraft server is on?**
   `curl http://127.0.0.1:8080/api/ping` on the Minecraft server's own machine. If this fails,
   it's not a network issue — go back to step 1, or check `webDashboard.port` for a typo/conflict
   with another service.

3. **Can you reach the port from the machine the Laravel app runs on?**
   `curl http://<minecraft-server-ip>:8080/api/ping` from the Laravel app's machine (or wherever
   `MC_API_URL` points). If this fails but step 2 succeeded, it's a network-layer problem:
   - `webDashboard.bindAddress` — must be `0.0.0.0` (all interfaces) if the Laravel app is on a
     different machine; `127.0.0.1` only accepts connections from the same machine.
   - Firewall on the Minecraft server's OS (Windows Firewall, ufw, etc.) — port 8080 (and 8081
     for WebSocket) need an inbound allow rule.
   - If the Minecraft server is behind NAT/a router (common for home-hosted servers), the port
     needs to be forwarded.
   - Cloud/VPS firewall (AWS security group, DigitalOcean firewall, etc.) if hosted in the cloud.
   - If accessed via a domain/reverse proxy, check that proxy's own routing/TLS config separately
     — `/api/ping` bypassing it entirely (hit the raw IP:port) isolates whether the proxy or the
     mod itself is the problem.

4. **Is `MC_API_URL` in the Laravel app's `.env` actually correct?**
   Common mistake: leaving it as `http://127.0.0.1:8080` when the Minecraft server is on a
   *different* machine from where Laravel runs — `127.0.0.1` from Laravel's perspective means
   "the Laravel server itself", not the Minecraft server. Must be the Minecraft server's real
   reachable IP/hostname.

5. **Is the dashboard actually paired?**
   `/api/ping` succeeding but the dashboard still can't call mutating endpoints means steps 1–4
   are fine. Check:
   - `MC_SERVICE_API_KEY` in the Laravel app's `.env` is actually populated — it's set
     automatically by running `/dashboard pair "<dashboardUrl>" <code>` (get the code from the
     dashboard's own Configuration page), never hand-typed. See
     [API System → Pairing](APISystem#pairing).
   - The paired key hasn't been revoked — `/apikey list` in-game shows every key's status; if
     it's gone, re-pair.
   - The key's role is at least `MODERATOR` if the Laravel app needs to call mutating endpoints
     (ban/kick/economy adjust/etc.) — `VIEWER` is read-only. Role is set at pairing time via
     `/apikey create`'s role (pairing itself always mints an `ADMIN` key).

6. **Is `storage.type`/database connectivity a separate red herring?**
   Not related to dashboard connectivity — don't confuse a `ClassNotFoundException` from the
   SQLite/MySQL storage backend (a dev-environment-only classloader limitation, see the Storage
   wiki page) with the dashboard's own HTTP connectivity. They're independent systems.

---

## Keeping the other branches in sync

NeoEssentials ships three parallel dev branches, one per supported Minecraft-version line —
`1.21.x` (1.21.1–1.21.11), `26.1.x` (pinned to 26.1–26.1.2), and `26.2.x` (26.2) — kept in sync by
cherry-picking every feature/fix commit across all three as it lands, including all of the
dashboard/connectivity work described on this page. They share one build-number counter (a GitHub
Actions repo variable, `BUILD_NUMBER` — not a file committed to the branches) so a fix shipped on
all three lands under the same build number everywhere. Everything described on this page (the
`webDashboard.mode` config, `/api/ping`, the dashboard security/account-linking features) is
already live on all three branches.

Since this connectivity layer is plain Java (`HttpExchange`/`HttpServer`, no `net.minecraft.*`
types touched) and JSON config, it's historically been a low-risk, mechanical cherry-pick with no
exposure to the Minecraft-version API differences that trip up other cross-branch ports (e.g.
`GameProfile`/`getProfileCache()` → `NameAndId`/`server.services().nameToIdCache()`,
`ClickType` → `ContainerInput`) — worth knowing if you're deciding how to port a *future* change
here yourself. `26.1.x` and `26.2.x` do occasionally need a small API-shape adjustment for changes
in other parts of the mod (not this connectivity layer specifically) since Mojang's official
mappings rename things between Minecraft versions — see each branch's own README for its current
target version.

---

*Back to [Wiki Home](Home)*
