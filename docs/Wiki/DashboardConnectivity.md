# Dashboard Connectivity — Internal vs. External Hosting

> Covers connecting a separately-hosted dashboard app (e.g. `NeoEssentials-Dashboard`, the
> Laravel app) to the mod's built-in API, diagnosing "the connection isn't establishing", and
> the new `webDashboard.mode` config option. Also serves as the porting checklist for applying
> all of this to the `mc-26.1-port` / `Dev-Build-26.x.x` branch.

---

## The two hosting modes

The mod's dashboard has always been two things bundled together:
1. A **REST API** under `/api/*` (moderation, players, economy, permissions, etc.)
2. A **static UI** served at `/` — the mod's own bundled HTML/JS/CSS dashboard frontend

Until now there was no way to run just the API without the mod also trying (and, in most dev/
production setups, failing) to serve its own bundled UI — every boot logged
`Dashboard resources NOT found - /webdashboard/index.html is null!` even on servers that only
ever use an external dashboard app and never load `http://<server>:8080/` directly.

### New config: `config.json` → `webDashboard.mode`

| Value | Behavior |
|---|---|
| `"internal"` (default) | Serves the bundled UI at `/` **and** the API at `/api/*` — use this if you only ever open `http://<server>:8080` directly in a browser. |
| `"external"` | Serves **only** `/api/*`. The `/` static-file route is never registered — no more misleading "resources NOT found" log line, and the port stops trying to be a website at all. Use this when a separate app (Laravel `NeoEssentials-Dashboard`, or anything else) is the *only* thing that ever talks to this port. |
| `"both"` | Explicit alias for `"internal"` — same behavior, for config clarity when you deliberately use the built-in UI *and* point an external app at the same API. |

Requires a restart to take effect (same as every other config toggle — see the "reload doesn't
add/remove commands" limitation documented elsewhere).

### New endpoint: `GET /api/ping`

Unauthenticated, always registered regardless of `mode`. Returns:
```json
{"success": true, "mod": "neoessentials", "mode": "internal"}
```
Use this to answer "can I reach the mod's HTTP port at all" as a separate question from "does my
service account actually authenticate" — the two failure modes look identical from the outside
(both just time out or error) but need completely different fixes.

```bash
curl http://<mod-server-ip>:8080/api/ping
```
- **Connection refused / timeout** → network problem (see checklist below), not a mod bug.
- **200 with the JSON above** → the mod's API is reachable; if the dashboard app still can't log
  in, the problem is in the service-account credentials, not connectivity.

---

## Root cause found this session (already fixed, but worth documenting)

While investigating, we found the most likely explanation for "the connection isn't
establishing": **`webDashboard.autoStart` was `false`**, and **`/dashboard start` (the in-game
command to start it manually) was dead code** — the class existed and compiled, but was never
actually registered with the command dispatcher (see the git history around
"Wire up /dashboard and /dashboardregister — they were never registered"). Combined, this meant
the dashboard's HTTP server may simply never have been running at all — no amount of correct
`MC_API_URL`/service-account config on the Laravel side would help, because there was nothing
listening on the port in the first place.

**Both are now fixed** (as of this session): `/dashboard start`/`stop`/`status`/`restart` work,
and `webDashboard.mode` lets you make the "API-only, no UI" intent explicit instead of accidental.

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

5. **Does the service account actually exist and have the right role?**
   `/api/ping` succeeding but login failing means steps 1–4 are fine. Check:
   - `MC_SERVICE_USERNAME`/`MC_SERVICE_PASSWORD` in Laravel's `.env` match a real account created
     via the mod's dashboard (see `config/minecraft.php`'s setup comment, or now: register via
     `/dashboardregister` in-game, or the Laravel app's own `/register` page — which as of this
     session also creates a matching mod-side account automatically).
   - The account isn't locked out from repeated failed attempts (`MAX_FAILED_ATTEMPTS` — check
     the mod's server log for lockout warnings, or reset the account's password via
     `/api/users/{id}/password` using a *different*, known-working admin account).
   - The account has at least `MODERATOR` role if the Laravel app needs to call mutating
     endpoints (ban/kick/economy adjust/etc.) — `VIEWER` is read-only.

6. **Is `storage.type`/database connectivity a separate red herring?**
   Not related to dashboard connectivity — don't confuse a `ClassNotFoundException` from the
   SQLite/MySQL storage backend (a dev-environment-only classloader limitation, see the Storage
   wiki page) with the dashboard's own HTTP connectivity. They're independent systems.

---

## Porting this to `mc-26.1-port` / `Dev-Build-26.x.x`

The 26.x branch was forked before all of this session's dashboard/moderation/storage work
landed on `Dev-Builds`, and (per the branch's own changelog "Known gap" notes) has been getting
individual fixes cherry-picked over in batches. This connectivity work should be ported the same
way: **cherry-pick, don't re-implement from scratch** — these changes are pure Java/config, with
no Minecraft-version-sensitive API surface, so a direct cherry-pick should apply cleanly or with
only trivial conflicts (same pattern as the earlier storage-abstraction port, which needed only
~8 small manual conflict resolutions across ~3000 lines changed).

### What to port (in dependency order)

1. **Prerequisite, if not already ported:** the `/dashboard` and `/dashboardregister` command
   registration fix (`NeoEssentials.java` wiring `DashboardCommand.register()` /
   `DashboardRegisterCommand.register()`) — the mode/connectivity work above is much harder to
   test/use without this, since `/dashboard status`/`start` are how you diagnose step 1 above.
2. `ConfigManager.java`: `isDashboardInternalUiEnabled()` getter + the `isWebDashboardEnabled()`
   double-key fix (`modules.webDashboardEnabled` AND `webDashboard.enabled`) if not already
   ported — the mode getter follows the exact same JSON-path-reading pattern.
3. `config.json`: the `webDashboard.mode` key + its explanatory comment, and the
   `EXPECTED_CONFIG_VERSIONS` bump (check the 26.x branch's own current `MAIN_CONFIG` version
   number first — do NOT reuse `30` verbatim if the branch has diverged further, bump from
   whatever its actual current value is).
4. `DashboardAPI.java`: the `isDashboardInternalUiEnabled()` early-return branch in
   `registerEndpoints()` (skips static-file serving + resource-check log spam in external mode),
   and the new `GET /api/ping` context registration.

### Expect these to need manual attention (not auto-mergeable)

- If the 26.x branch's `DashboardAPI.java` has diverged in its `registerEndpoints()` method
  (e.g. different route lists, different log-line formatting per the branch's own changes),
  the early-return block and `/api/ping` registration may need to be re-positioned rather than
  applying as a clean patch — read the actual current method body on that branch before pasting.
- Verify the 26.x branch's own `config.json` — confirm it doesn't already have an unrelated
  `webDashboard.mode`-shaped key from independent work on that branch before adding this one.

### Not port-specific

Everything above is plain Java (`HttpExchange`/`HttpServer`, no `net.minecraft.*` types touched)
and JSON config — none of it should be affected by the 1.21.1 → 26.1.2 API differences that have
tripped up other ports this session (e.g. `GameProfile`/`getProfileCache()` →
`NameAndId`/`server.services().nameToIdCache()`, `ClickType` → `ContainerInput`). Should be a
low-risk, mechanical port once the prerequisite (`/dashboard` command wiring) is confirmed to be
in place on that branch.

---

*Back to [Wiki Home](Home)*
