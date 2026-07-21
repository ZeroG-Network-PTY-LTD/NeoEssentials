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
