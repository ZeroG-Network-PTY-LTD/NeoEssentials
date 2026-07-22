# NeoEssentials — Changelog (mc-26.1-port branch)

**Minecraft 26.1.2 · NeoForge 26.1.2.76+**

All notable changes to this branch are documented here, starting from
**v1.0.4** — earlier history (v1.0.3.x and before, including the port's own
migration history) is not carried over.

---

## 1.0.4-mc26.1.2+build.17 — 2026-07-22

### ✨ Account Settings Page + Minecraft Account Linking (Both Dashboards)

- Sidebar now shows your real Minecraft avatar once linked, on both dashboards.
- New Settings/Profile page on the internal dashboard (password change,
  Minecraft account linking, Discord status).
- Any dashboard account can now link a Minecraft account itself via a short
  in-game code (`/linkaccount <code>`) — no Discord required.

---

## 1.0.4-mc26.1.2+build.16 — 2026-07-22

### ✨ New Brand Logo on the Internal Dashboard

- The internal dashboard now shows the real NeoEssentials shield logo instead
  of a generic placeholder icon, everywhere: sidebar, login, public lookup
  page, and browser tab favicon.

---

## 1.0.4-mc26.1.2+build.15 — 2026-07-22

### ✨ Dashboard Security: Copyable Keys, Encrypted Tokens, Permission-Driven Role Sync

- `/apikey create` tokens are now click-to-copy in chat instead of plain text.
- The paired external dashboard's auth token is now encrypted at rest in
  `config.json` instead of stored in plaintext (existing values migrate
  automatically, no action needed).
- New opt-in setting `webDashboard.roleSync`: automatically grants/revokes a
  linked player's dashboard admin role based on their real in-game permission
  node or group — no more manually running `/apikey create` every time an
  admin's status changes. Off by default.

---

## 1.0.4-mc26.1.2+build.14 — 2026-07-22

### ✨ Economy Tab: Overview Stats + Distribution Chart, Plus a Real Bug Fix

- Both dashboards' Economy tab now shows total wealth, account count,
  average/starting balance, and a balance-distribution chart at a glance.
- Adjusting a balance now shows a live lookup preview (avatar, online
  status, current balance) before you commit — no more adjusting blind.
- Fixed: the distribution chart's data was silently failing to compute for
  any balance with cents (i.e. almost all of them) — now works correctly.

---

## 1.0.4-mc26.1.2+build.13 — 2026-07-22

### ✨ Per-Player Control Page: Now Feature-Complete

- The built-in dashboard's per-player control page now covers just about every
  admin action you'd otherwise type as a command: heal/kick/ban/mute, game
  mode, permission group and overrides, economy, freeze/vanish/jail, give
  item/potion effects/spawn mob/burn/lightning/kill, sudo, per-player time and
  weather, and clear inventory — plus full moderation history in one place.
- Not included: live inventory/ender-chest editing (those are in-game GUI
  menus with no browser equivalent).

---

## 1.0.4-mc26.1.2+build.9 — 2026-07-22

### ✨ Built-In Dashboard: Full Per-Player Control Page

- The built-in dashboard's Players page now has a full control page for each
  player — game mode, permission group and individual permissions, economy
  (give/take/set balance), inventory, and full moderation history (bans, mutes,
  kicks, warnings, staff notes) all in one place.
- Player lookups now show balance and permission group for offline players too,
  not just online ones.
- Fixed a bug where several player-info API endpoints didn't work for offline
  players even though they were supposed to.

---

## 1.0.4-mc26.1.2+build.8 — 2026-07-22

### ✨ Built-In Dashboard: Now Feature-Complete

- The built-in dashboard (`webDashboard.mode: "internal"`/`"both"` in
  `config.json`) now covers everything: server overview, players, economy,
  warps, kits, holograms, Discord, dashboard accounts, backups, console
  commands, activity logs, permissions, and a no-login-required public
  player-lookup page.
- Already using the separate `NeoEssentials-Dashboard` app? Nothing changes
  for you — the default stays `"external"`.

---

## 1.0.4-mc26.1.2+build.5 — 2026-07-22

### ✨ A Real Built-In Dashboard Option

- Prefer not to run a separate app for your web dashboard? Set `webDashboard.mode` to
  `"internal"` (or `"both"`) in `config.json` and the mod now serves a real, working
  dashboard straight from its own port — no separate app to install.
- This first update covers logging in, the server overview, and the players page
  (heal/kick/ban/mute, lookup, homes). More pages are coming in future updates.
- If you're already using the separate `NeoEssentials-Dashboard` app, nothing changes
  for you — the default stays the same.

---

## 1.0.4-mc26.1.2+build.1 — 2026-07-21

### ✨ Support/Discord/GitHub Links on Startup

- The server console now prints a quick link to our support site, Discord, and GitHub once
  every restart, so admins always know where to get help.
- If something actually goes wrong during startup (a system fails to load, or the permission
  system has to fall back to a safe mode), the first admin to join the server now sees a
  clickable in-game message pointing to those same three places.

---
