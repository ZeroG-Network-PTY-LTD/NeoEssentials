# Changelog — NeoEssentials (mc-26.1-port branch)

All notable changes to this branch are documented here, starting from
**v1.0.4** — earlier history (v1.0.3.x and before, including the port's own
migration history) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 26.1.2 · NeoForge 26.1.2.76+**

> The build counter was reset alongside the v1.0.4 bump, so build numbers here start
> back at 0/1 — always go by date/version, not build number, when comparing across
> the reset.

---

## [1.0.4-mc26.1.2+build.9] — 2026-07-22

### ✨ Internal Dashboard: Player Management Overhaul + a Full Per-Player Control Page

- Players page "More" panel gained two controls that were already possible via the
  mod's API but never wired into the UI: **game mode** (survival/creative/adventure/
  spectator) and **permission group** re-assignment.
- The "Look up a player" result now shows balance, permission group, and UUID for
  **every** lookup — online or offline — plus live health/position/playtime when the
  player's online.
- New: a full per-player control page at `/players/player/<username>`, linked from
  both the "More" panel and the lookup result. One page per player with:
  - Quick actions (heal, kick, mute/unmute, ban, game mode)
  - Permission group + individual permission node overrides (add/remove)
  - Economy (balance, give/take/set)
  - Inventory (works for offline players too now, see fix below)
  - Full moderation history — bans (with unban), mutes, kicks, warnings (with
    remove), and admin notes (add/remove)
- **Fixed:** `/api/player/profile`, `/stats`, `/achievements`, `/inventory`, `/status`,
  `/health`, `/xp`, and `/location` all silently 404'd for any player who wasn't
  *currently* online, even though the underlying data collectors already supported
  reading offline player data from disk — the username→UUID lookup just never
  checked the offline profile cache. Fixed once, benefits every one of those
  endpoints.
- Also cleaned up the `/dashboard status` chat output: the header/footer separator
  lines used two different characters (`─` vs `═`) and were long enough to wrap
  awkwardly in a normal-width chat window — now one consistent, shorter line.

---

## [1.0.4-mc26.1.2+build.8] — 2026-07-22

### ✨ Internal Dashboard: Complete — Permissions and Public Lookup

- Finishes the internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with the last two pages: Permissions (groups, per-user overrides,
  inheritance, aliases, node catalog) and the public player-lookup page (no
  login required, same as the mod's own `/api/public/moderation/*` routes).
- All 13 pages now match the external `NeoEssentials-Dashboard` app's
  functionality: Overview, Players, Economy, Warps, Kits, Holograms, Discord,
  Users, Backups, Commands, Logs, Permissions, and public lookup.
- The shipped default remains `"external"` — nothing changes for servers
  already using the separate dashboard app.

---

## [1.0.4-mc26.1.2+build.8] — 2026-07-22

### ✨ Internal Dashboard: Backups, Commands, and Logs Pages

- Continues the internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Backups (create/restore/delete/download,
  plus Dropbox/Google Drive cloud upload config), Commands (run a console
  command directly), and Logs (recent join/leave/chat/command activity).
- Still not there yet: Permissions, public player lookup — coming in further
  follow-up passes.

---

## [1.0.4-mc26.1.2+build.7] — 2026-07-22

### ✨ Internal Dashboard: Holograms, Discord, and Users Pages

- Continues the internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Holograms (full CRUD), Discord (bridge
  status, account-linking config, recent events), and Users (the mod's own
  dashboard-account management).
- Fixed along the way: creating/editing a hologram through the dashboard was
  silently rejected by the mod (a body-shape mismatch on the `lines` field) —
  now works correctly.
- Still not there yet: Permissions, Backups, Commands, Logs, public player
  lookup — coming in further follow-up passes.

---

## [1.0.4-mc26.1.2+build.6] — 2026-07-22

### ✨ Internal Dashboard: Economy, Warps, and Kits Pages

- Continues build.5's internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Economy (leaderboard + give/take/set balance),
  Warps (list/create/delete), and Kits (read-only list + stats).
- Still not there yet: Holograms, Discord, Permissions, Backups, Commands, Logs,
  Users, public player lookup — coming in further follow-up passes.

---

## [1.0.4-mc26.1.2+build.5] — 2026-07-22

### ✨ A Real Internal Dashboard (`webDashboard.mode: "internal"`)

- `webDashboard.mode` has had an `"internal"` option for a long time, but it never
  actually worked — `src/main/resources/webdashboard/` only ever held a Laravel
  *scaffold* (raw PHP/TSX source meant to be copied into a separate project), not
  a servable `index.html`. Setting `mode` to `"internal"` or `"both"` now serves a
  real, working bundled dashboard from `/` on the mod's own port — no separate
  Laravel/PHP app required. Talks to this mod's own REST API directly.
- This first pass covers login, the server overview, and the players page (roster,
  heal/kick/ban/mute, lookup, homes). The remaining pages (economy, warps, kits,
  holograms, Discord, permissions, backups, commands, logs, users, public lookup)
  are still only available via the external `NeoEssentials-Dashboard` app for now
  — more land in follow-up updates.
- The shipped default remains `"external"` — this doesn't change anything for
  servers already using the separate dashboard app.
- Also fixed: a missing static file under `webDashboard.mode: "internal"` used to
  silently hang the connection instead of returning a 404, and there was no
  fallback for a hard-refreshed client-side route (e.g. `/players`) — both fixed
  as part of building this.

---

## [1.0.4-mc26.1.2+build.4] — 2026-07-21

### ✨ Pairing Now Includes the WebSocket Port

- `/dashboard pair` now sends `webDashboard.websocketPort` alongside the API key in its
  `POST /api/pair/complete` request, so a paired external dashboard can auto-configure a live
  WebSocket connection (server status, chat, player join/leave) instead of needing the admin
  to hand-enter that port separately. First step toward the external dashboard actually using
  the mod's existing WebSocket server, which it never has until now.

---

## [1.0.4-mc26.1.2+build.1] — 2026-07-21

### ✨ Support/Discord/GitHub Links on Startup and on Real Detected Problems

- **Console:** a quiet one-line pointer to the support site, Discord, and GitHub repo is now
  printed once every server restart, regardless of health.
- **Console (prominent) + in-game:** if a manager actually fails to initialize
  (`ManagerRegistry.getFailedCount() > 0`) or the permission system falls back to emergency
  mode, a bordered warning block prints right at the point of failure, and the first admin
  (OP or wildcard permission) to join that session gets a clickable chat message —
  `[Support]`/`[Discord]`/`[GitHub]`, each opening the link in the browser when clicked.
  Session-scoped, so it only ever fires once per restart even with multiple detected problems
  or admins joining.
- New `SupportLinks` util centralizes the three URLs (reusing the same ones already in
  README/wiki, not new links) so console and in-game code share one source of truth.

---
