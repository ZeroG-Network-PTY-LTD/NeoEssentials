# NeoEssentials — Changelog

Starting from **v1.0.4** — earlier history (v1.0.3.x and before) is not carried over.

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.4+build.8 — 2026-07-22

### ✨ Built-In Dashboard: Now Feature-Complete

- The built-in dashboard (`webDashboard.mode: "internal"`/`"both"` in
  `config.json`) now covers everything: server overview, players, economy,
  warps, kits, holograms, Discord, dashboard accounts, backups, console
  commands, activity logs, permissions, and a no-login-required public
  player-lookup page.
- Already using the separate `NeoEssentials-Dashboard` app? Nothing changes
  for you — the default stays `"external"`.

---

## 1.0.4+build.4 — 2026-07-22

### ✨ A Real Built-In Dashboard Option

- Prefer not to run a separate app for your web dashboard? Set `webDashboard.mode` to
  `"internal"` (or `"both"`) in `config.json` and the mod now serves a real, working
  dashboard straight from its own port — no separate app to install.
- This first update covers logging in, the server overview, and the players page
  (heal/kick/ban/mute, lookup, homes). More pages are coming in future updates.
- If you're already using the separate `NeoEssentials-Dashboard` app, nothing changes
  for you — the default stays the same.

---

## 1.0.4+build.1 — 2026-07-21

### ✨ Support/Discord/GitHub Links on Startup

- The server console now prints a quick link to our support site, Discord, and GitHub once
  every restart, so admins always know where to get help.
- If something actually goes wrong during startup (a system fails to load, or the permission
  system has to fall back to a safe mode), the first admin to join the server now sees a
  clickable in-game message pointing to those same three places.

---
