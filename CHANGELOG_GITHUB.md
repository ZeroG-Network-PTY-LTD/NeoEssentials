# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here, starting from
**v1.0.4** — earlier history (v1.0.3.x and before) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

> The build counter was reset alongside the v1.0.4 bump, so build numbers here start
> back at 0/1 — always go by date/version, not build number, when comparing across
> the reset.

---

## [1.0.4+build.17] — 2026-07-22

### ✨ Account Settings Page + Minecraft Account Linking (Both Dashboards)

- Both dashboards' sidebar now shows your actual Minecraft skin avatar (once
  linked — see below) instead of a generic icon.
- New: a Settings/Profile page on the internal dashboard (previously missing
  entirely — the gear icon just said "not yet ported"), with password
  change, Minecraft account linking, and a read-only Discord status. The
  external dashboard's existing Profile page gains the same Minecraft
  account linking section.
- **New capability on both dashboards:** any dashboard account — however it
  was created — can now link a Minecraft account itself via a short in-game
  code (`/linkaccount <code>`), instead of needing to already have Discord
  linked in-game first. Self-service unlink too.
- Discord status is shown read-only on the internal dashboard (resolved via
  the server's own Discord bot integration) — a full browser "Connect
  Discord" button for the internal dashboard is a larger follow-up, not in
  this pass.

---

## [1.0.4+build.16] — 2026-07-22

### ✨ New Brand Logo on the Internal Dashboard

- The internal dashboard's generic gradient-badge/compass-icon placeholder is
  now the real NeoEssentials shield logo, everywhere it appears: sidebar
  header, login screen, public player-lookup page, and the browser tab
  favicon.

---

## [1.0.4+build.15] — 2026-07-22

### ✨ Dashboard Security: Copyable Keys, Encrypted Token Storage, Permission-Driven Role Sync

- `/apikey create` now prints the token as a click-to-copy chat component instead
  of plain text — click it to copy straight to your clipboard.
- The Bearer token stored for a paired external dashboard
  (`webDashboard.externalDashboard.token`) is now encrypted at rest in
  `config.json` instead of stored in plaintext. Existing plaintext values are
  transparently migrated to encrypted form the next time they're read — no
  action needed. (This mirrors the encrypted-at-rest storage the external
  Laravel dashboard already used for its own copy of the same token.)
- New opt-in feature: **permission-driven dashboard role sync**
  (`webDashboard.roleSync` in `config.json`, off by default). When enabled, a
  player who's linked a dashboard account via `/dashboardregister` automatically
  gets the dashboard `ADMIN` role the moment they have a configured in-game
  permission node or belong to a configured permission group — and loses it
  again the moment they don't, with no manual `/apikey create`/API call needed.
  Runs an immediate check on join plus a periodic sweep (default every 5
  minutes) so it also catches permission changes made outside the mod (e.g. a
  direct LuckPerms edit). A role you set manually is never touched by this —
  it only ever adjusts a role it granted itself.

---

## [1.0.4+build.14] — 2026-07-22

### ✨ Economy Tab: Overview Stats, Distribution Chart, Player Lookup + Bug Fix

- Both dashboards' Economy tab now shows total wealth, account count, and
  average/starting balance at a glance, plus a balance-distribution histogram
  (the mod already computed all of this via `/api/stats/economy` — the tab
  just never displayed it).
- The "Adjust balance" form now has a **Look up** button that shows the
  target's avatar, online status, and current balance before you commit to
  give/take/set — no more adjusting a balance blind by typing a name/UUID.
  Leaderboard entries and a successful lookup both link to that player's
  full profile page.
- **Fixed:** `/api/stats/economy` used `BigDecimal.longValueExact()` to
  bucket balances for the distribution histogram — that throws
  `ArithmeticException: Rounding necessary` for any balance with cents (i.e.
  almost every real balance), silently dropping the `distribution` field
  from the response and returning a stray `"error"` key alongside the
  otherwise-valid data. Now uses `longValue()` (which truncates instead of
  throwing) — the histogram only needs the whole-dollar bucket anyway.

---

## [1.0.4+build.13] — 2026-07-22

### ✨ Player Profile Page: Phase 4 (Final) — Sudo, Ptime/Pweather, Clear Inventory

- Fourth and final planned pass on the per-player dashboard control page. Adds:
  - **Sudo** — run a command (or send a chat message) as the player
  - **Per-player time/weather** (`/ptime`, `/pweather`) — set or reset, with the
    current override shown
  - **Clear inventory** (main + armor + offhand)
- Deliberately **not** ported: `/invseeedit` and `/enderchestedit`. Both are
  fundamentally a live in-game GUI menu opened for a physical viewer player —
  there's no equivalent for a browser tab with no in-game client. A real
  slot-by-slot inventory editor for the dashboard would be a separate,
  much larger feature (drag/drop UI, item picker, NBT editing) rather than a
  wrapper around the existing command, so it's out of scope here.
- This completes the 4-phase player-profile-page effort: game mode, permission
  group + individual overrides, economy, moderation history, freeze/vanish/jail,
  item/fun commands, and now sudo/ptime/pweather/clear-inventory — all on one
  page per player at `/players/player/<username>`.

---

## [1.0.4+build.12] — 2026-07-22

### ✨ Player Profile Page: Phase 3 — Items & Fun Commands

- Third pass on the per-player dashboard control page. Adds:
  - **Give item** (by registry ID, e.g. `minecraft:diamond_sword`)
  - **Potion effects** — apply by effect ID with duration/amplifier, or clear all
  - **Spawn mob** at the player's location
  - **Burn** (set on fire for N seconds), **lightning strike**, and **kill**
- All online-players-only, same as their `/command` equivalents (`/give`,
  `/effect`, `/spawnmob`, `/burn`, `/lightning`, `/kill`).
- Deliberately skipped `/skull` — it gives the *executor* a player-head item, not
  something that acts on the target player, so it doesn't fit this page's model.
- Next (and last planned) pass: inventory/ender chest editing, plus `/sudo`,
  `/clearinventory`, `/ptime`, `/pweather`.

---

## [1.0.4+build.11] — 2026-07-22

### ✨ Player Profile Page: Phase 2 — Freeze, Vanish, Jail

- Second pass on the per-player dashboard control page. Adds:
  - **Freeze/unfreeze** — works even for offline players (matches `/freeze`'s own
    UUID-keyed behavior; takes effect immediately if/when they're online).
  - **Vanish/unvanish** — vanish requires the player to be online (same as
    `/vanish`), unvanish works regardless.
  - **Jail/unjail** — a dropdown of the server's actual configured jail
    locations, since jailing requires picking one.
- New moderation REST routes backing all of the above (`/api/moderation/freeze`,
  `/vanish`, `/jail`, plus `/jails` to list configured jail locations) — GET
  routes open to any logged-in account, mutations admin-only, same convention
  as every other moderation route.
- Next pass: item & fun commands (give, effect, lightning, spawnmob), then
  inventory/ender chest editing.

---

## [1.0.4+build.10] — 2026-07-22

### ✨ Player Profile Page: Phase 1 — State Toggles, Nickname, Teleport

- First of several planned passes bringing more of the mod's ~172 commands to the
  per-player dashboard control page (`/players/player/<username>`). This pass covers
  the commands with the biggest day-to-day admin value:
  - **Fly** and **god mode** toggles
  - **Feed** and **extinguish** (separate from the existing heal action)
  - **Walk/fly speed** (0–10 scale, same as `/speed`)
  - **Nickname** set/clear (same as `/setnick`)
  - **Teleport to another online player** (the mod's teleport endpoint already
    supported this — just wasn't wired into any UI yet)
- All of the above require the target player to be online, same as their `/command`
  equivalents — the dashboard now says so plainly instead of a bare error.
- Next passes: freeze/vanish/jail, item & fun commands (give, effect, lightning,
  spawnmob), and inventory/ender chest editing.

---

## [1.0.4+build.9] — 2026-07-22

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

## [1.0.4+build.8] — 2026-07-22

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

## [1.0.4+build.7] — 2026-07-22

### ✨ Internal Dashboard: Backups, Commands, and Logs Pages

- Continues the internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Backups (create/restore/delete/download,
  plus Dropbox/Google Drive cloud upload config), Commands (run a console
  command directly), and Logs (recent join/leave/chat/command activity).
- Still not there yet: Permissions, public player lookup — coming in further
  follow-up passes.

---

## [1.0.4+build.6] — 2026-07-22

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

## [1.0.4+build.5] — 2026-07-22

### ✨ Internal Dashboard: Economy, Warps, and Kits Pages

- Continues build.4's internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Economy (leaderboard + give/take/set balance),
  Warps (list/create/delete), and Kits (read-only list + stats).
- Still not there yet: Holograms, Discord, Permissions, Backups, Commands, Logs,
  Users, public player lookup — coming in further follow-up passes.

---

## [1.0.4+build.4] — 2026-07-22

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

## [1.0.4+build.3] — 2026-07-21

### ✨ Pairing Now Includes the WebSocket Port

- `/dashboard pair` now sends `webDashboard.websocketPort` alongside the API key in its
  `POST /api/pair/complete` request, so a paired external dashboard can auto-configure a live
  WebSocket connection (server status, chat, player join/leave) instead of needing the admin
  to hand-enter that port separately. First step toward the external dashboard actually using
  the mod's existing WebSocket server, which it never has until now.

---

## [1.0.4+build.1] — 2026-07-21

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
