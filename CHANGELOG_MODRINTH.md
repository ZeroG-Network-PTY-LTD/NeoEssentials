# NeoEssentials — Changelog (mc-26.1-port branch)

**Minecraft 26.1.2 · NeoForge 26.1.2.76+**

All notable changes to this branch are documented here, starting from
**v1.0.4** — earlier history (v1.0.3.x and before, including the port's own
migration history) is not carried over.

---

## 1.0.4-mc26.1.2+build.42 — 2026-07-31

### 🐛 Team Chat: Found the Real Bug (FTB Teams Versions)

- Found the actual cause of team chat not resolving on some FTB Teams versions: NeoEssentials
  was checking the wrong internal class for the right method. Should be properly fixed now for
  FTB Teams 2101.1.10 — if it's still not working after this update, please report it again.

---

## 1.0.4-mc26.1.2+build.41 — 2026-07-31

### 🐛 Team Chat: Still Investigating on Some FTB Teams Versions

- The previous attempt to fix team chat still isn't resolving on FTB Teams 2101.1.10. This update
  logs a lot more detail at startup to help pin down the exact fix — team chat isn't expected to
  work yet on that version, but the next update should have it.

---

## 1.0.4-mc26.1.2+build.40 — 2026-07-29

### 🐛 Attempted Fix: Team Chat Not Working on Some FTB Teams Versions

- If the `team` chat channel wasn't working for you even with FTB Teams installed, this update
  makes NeoEssentials try harder to find the right FTB Teams hook automatically instead of only
  checking two fixed possibilities. If it still doesn't work after updating, check your server
  log at startup for an "FTB TEAMS API NOT RESOLVED" message and report it — it now also logs
  extra detail that'll help pin down the exact fix.

---

## 1.0.4-mc26.1.2+build.39 — 2026-07-29

### ✨ More Player Info Available to Dashboard Integrations

- Player lookups (both from the dashboard and its API) now also report total playtime, when the
  player first joined, and their current gamemode.

---

## 1.0.4-mc26.1.2+build.38 — 2026-07-29

### 🐛 Fix: Channel Prefix Messages Showing the Wrong Channel Name

- Using a channel prefix (like `!` or `@`) to send a single message to a different channel now
  correctly shows that channel's name in the message, instead of showing your normal channel.

---

## 1.0.4-mc26.1.2+build.37 — 2026-07-28

### ✨ Internal Dashboard: Player Tools Moved to the Lookup Page

- The full player-management panel (heal, kick, ban, mute, teleport, permissions, inventory,
  notes, and more) used to be its own page. It's now shown right on the Player Lookup page for
  signed-in staff, so there's one less click to get there.

---

## 1.0.4-mc26.1.2+build.36 — 2026-07-28

### ✨ Point the "View Profile" Chat Link at Your Own Website

- The small clickable icon next to player names in chat (added build.24) can now point at your
  own website instead of the built-in dashboard — set `webDashboard.customProfileUrlTemplate` to
  something like `"https://myserver.com/players/{player}"` and it'll use that instead.

---

## 1.0.4-mc26.1.2+build.33 — 2026-07-28

### ✨ New: Team Chat Channel (FTB Teams)

- Added a `team` chat channel — if you're playing a modpack with FTB Teams, chatting in it only
  reaches your own teammates, no radius or permission setup needed.
- If FTB Teams isn't installed, or you're not on a team yet, you'll get a clear message telling
  you why instead of the message just silently vanishing.

---

## 1.0.4-mc26.1.2+build.32 — 2026-07-28

### ✨ Give a Chat Channel a Styled Name, Plus a Reliability Fix

- New `displayName` option per chat channel — lets you show a colored/custom name (e.g.
  "&d⚙ Staff") for a channel in chat, without touching its actual internal name.
- Fixed a bug where a broken chat channel setting could silently stop OTHER channels' commands
  from working too. Each channel is now handled independently.

---

## 1.0.4-mc26.1.2+build.30 — 2026-07-28

### ✨ Customize the Discord Chat Embed Yourself

- New `discordEmbedTemplate` config option lets you customize the look of the Discord embed for
  channel-routed chat messages (Simple Discord Link only, for now) — author name, avatar, message
  text, side-bar color, footer, and timestamp are all configurable, with placeholders for player
  name, message, and channel.
- If you use split config files, this new setting lives in its own file under a new `templates/`
  folder instead of cluttering `chat.json`.

---

## 1.0.4-mc26.1.2+build.29 — 2026-07-28

### 🐛 SDLink Channel-Routed Chat Now Looks Right Again

- If you set a specific Discord channel for one of your chat channels, messages were showing up
  as plain text instead of Simple Discord Link's usual nicely-styled message with your avatar.
  Fixed — it now looks the same as before.
- Also fixed a hidden crash risk in the same area, found while working on this.

---

## 1.0.4-mc26.1.2+build.28 — 2026-07-28

### 🐛 Critical Fix: build.27 Could Crash Your Whole Server on Startup

- If you updated to build.27, some server setups would crash immediately on startup with a
  `NoClassDefFoundError`. This was caused by the Mc2Discord/DCIntegration fixes in that update.
  Fully fixed now — please update immediately if you're on build.27.

---

## 1.0.4-mc26.1.2+build.27 — 2026-07-27

### 🐛 Same Discord Fix Extended to Mc2Discord and DCIntegration

- If you use Mc2Discord instead of Simple Discord Link, it had the same channel-privacy bug fixed
  last update — a configured private channel ID was being ignored. Fixed the same way.
- DCIntegration now also supports sending a specific NeoEssentials chat channel (like staff) to
  its own dedicated Discord channel, which wasn't possible before at all.
- Neither of these was tested against a live Mc2Discord/DCIntegration setup (unlike the earlier
  Simple Discord Link fix) — please report anything unexpected.

---

## 1.0.4-mc26.1.2+build.26 — 2026-07-27

### ✨ New Chat Channel Placeholder, Plus Discord Relay Fixes

- New `{channel}` placeholder — shows which chat channel (local/global/staff/etc.) a message was
  sent in.
- Fixed: a private staff Discord channel you configured could still leak messages to your
  server's default Discord channel, because the Simple Discord Link integration was silently
  ignoring the specific channel ID you set. It now actually goes to the right channel.
- If you're seeing Discord messages posted twice, that's a separate issue caused by Simple
  Discord Link having its own built-in "relay every chat message" feature running at the same
  time as NeoEssentials' own relay — NeoEssentials now warns you about this on startup and tells
  you exactly which setting to turn off in Simple Discord Link's own config.

---

## 1.0.4-mc26.1.2+build.25 — 2026-07-27

### 🐛 Fix: Chat Status Icons (AFK/Vanished/Muted) Weren't Showing

- If you set an icon for AFK/vanished/muted players in `chat.badges.statusIcons`, it silently
  never appeared in chat with the default settings. Fixed — set your icon text and it'll show
  up now. Rank badges weren't affected by this.

---

## 1.0.4-mc26.1.2+build.24 — 2026-07-27

### ✨ Click a Player's Name in Chat to Open Their Dashboard Profile

- Player names in chat now show a small clickable link icon that opens that player's public
  profile (bans/mutes/kicks/warnings) on your dashboard, right in a browser. Works whether
  you use the external (Laravel) dashboard or the mod's own built-in one.
- If you use the built-in dashboard, set the new `webDashboard.publicUrl` config to your
  server's public address so the link actually works for players (e.g.
  `http://your.server.ip:8090`). If you use the external dashboard, nothing extra to set up —
  it already knows its own address.

---

## 1.0.4-mc26.1.2+build.23 — 2026-07-27

### 🐛 Fix: Logging In Could Change Everyone Else's Tab Suffix

- If two players in the same rank ever ended up with slightly different tab-list suffixes (an
  AFK tag, a per-player override, etc.), a new player logging in could make everyone else's
  suffix suddenly switch to match the new player's. Fixed — everyone's own suffix now displays
  correctly no matter who else logs in.

---

## 1.0.4-mc26.1.2+build.22 — 2026-07-27

### ✨ You Can Now Back Up to Microsoft OneDrive Too

- Added OneDrive as a third backup destination, alongside Dropbox and Google Drive. If you've got a
  lot of free space sitting on a personal OneDrive, you can now send your server backups there
  straight from the dashboard.

---

## 1.0.4-mc26.1.2+build.21 — 2026-07-23

### ✨ Internal Dashboard: 3D Player Renders

- Player Profile, Settings, and Public Lookup now show a full 3D character render with a glowing
  color-matched background instead of a flat face icon.

---

## 1.0.4-mc26.1.2+build.20 — 2026-07-23

### 🐛 Fix: Visiting the Warps Page Could Show "API Unreachable"

- The Player Warps tab could hang under real player load instead of loading, which made the
  dashboard think the whole connection was down. Fixed.

---

## 1.0.4-mc26.1.2+build.19 — 2026-07-23

### ✨ Internal Dashboard Permissions Page Redesign

- Groups and online users now collapse to a compact row by default — click to expand for the
  full editor. Permission nodes are grouped by category instead of one long list.

---

## 1.0.4-mc26.1.2+build.18 — 2026-07-23

### ✨ See Player-Created Warps on Both Dashboards

- Both dashboards' Warps page now has a "Player Warps" tab, so admins can finally see every
  player's own `/pwarp` warps (and clean one up if needed) instead of only the shared server
  warps.

---

## 1.0.4-mc26.1.2+build.17 — 2026-07-23

### 🐛 AFK Now Actually Respects Its Own Settings

- The AFK config options for what counts as activity (movement, chat, commands, interactions)
  weren't being enforced — everything always reset your AFK timer no matter how you had them set,
  and chat specifically never reset it at all. Both are now fixed.
- Small `/help` fix: `/tpcancel` now shows up correctly (it was previously mislabeled).
- Cleaned up a few leftover admin permission nodes that looked like they did something but didn't.

---

## 1.0.4-mc26.1.2+build.16 — 2026-07-23

### 🐛 /help Now Shows the Real Permission for Every Command

- `/help <command>` used to guess every command's permission node from its
  name, which was wrong for ~160 commands (e.g. `/apikey` actually needs
  `neoessentials.dashboard.apikeys`, most moderation/teleport/economy/item
  commands use a namespaced node). Fixed — `/help` now shows and checks the
  real permission for every command, so it's actually reliable for figuring
  out what to grant someone.

---

## 1.0.4-mc26.1.2+build.15 — 2026-07-22

### ✨ Account Settings Page + Minecraft Account Linking (Both Dashboards)

- Sidebar now shows your real Minecraft avatar once linked, on both dashboards.
- New Settings/Profile page on the internal dashboard (password change,
  Minecraft account linking, Discord status).
- Any dashboard account can now link a Minecraft account itself via a short
  in-game code (`/linkaccount <code>`) — no Discord required.

---

## 1.0.4-mc26.1.2+build.14 — 2026-07-22

### ✨ New Brand Logo on the Internal Dashboard

- The internal dashboard now shows the real NeoEssentials shield logo instead
  of a generic placeholder icon, everywhere: sidebar, login, public lookup
  page, and browser tab favicon.

---

## 1.0.4-mc26.1.2+build.13 — 2026-07-22

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

## 1.0.4-mc26.1.2+build.12 — 2026-07-22

### ✨ Economy Tab: Overview Stats + Distribution Chart, Plus a Real Bug Fix

- Both dashboards' Economy tab now shows total wealth, account count,
  average/starting balance, and a balance-distribution chart at a glance.
- Adjusting a balance now shows a live lookup preview (avatar, online
  status, current balance) before you commit — no more adjusting blind.
- Fixed: the distribution chart's data was silently failing to compute for
  any balance with cents (i.e. almost all of them) — now works correctly.

---

## 1.0.4-mc26.1.2+build.13-pre-fix — 2026-07-22

> Shipped as build.13 at the time — after a cross-branch build-number-drift fix
> reset the counter, that number now belongs to a later, different entry above.

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
