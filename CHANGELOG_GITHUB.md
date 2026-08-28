# Changelog — NeoEssentials

> **This is the UNIVERSAL changelog** — one shared history covering all three dev
> branches (`1.21.x`, `26.1.x`, `26.2.x`), since GitHub releases bundle every branch's
> jar into a single shared release. Write entries here ONCE, describing the change
> itself — not duplicated per branch, and not restated in branch-specific terms unless
> a fix genuinely only applies to one version. This file should read identically across
> all three branches at any given point in time.
>
> `CHANGELOG_CURSEFORGE.md`/`CHANGELOG_MODRINTH.md` stay branch-specific (those platforms
> require separate uploads per Minecraft version), so keep writing those per-branch.

All notable changes to NeoEssentials are documented here, starting from
**v1.0.6** — earlier history (v1.0.5.x and before) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 (`1.21.x`) · Minecraft 26.1–26.1.2 (`26.1.x`) · Minecraft 26.2 (`26.2.x`)**

> The build counter was reset alongside the v1.0.6 bump, so build numbers here start
> back at 0/1, and the shared CI build number was reset to match — no more offset
> between branches.

---

## [1.0.6] — 2026-08-27

### Added
- **Sidebar scoreboard system** — `/scoreboard toggle|reload|enable|disable|info|preview|
  board list|set title|set line|player ...|group ...`. Config-driven named boards with
  priority-ordered conditions (`perm:`, `world:`, placeholder comparisons), per-line
  animation, per-group/per-player title/line overrides, a persisted per-player toggle, and
  a dashboard endpoint (`/api/scoreboard`) for reading/editing boards. Renders entirely via
  direct per-connection packets (not the shared server scoreboard), so different players can
  see different boards or values at the same time with no cross-player interference.
- **General leaderboard system** — `/leaderboard` (alias `/lb`) generalizes `/baltop`
  (still works unchanged) into a registry any stat can plug into. Ships with four boards:
  `money`, `kills`, `mob_kills`, `playtime` — the last three read Minecraft's own per-player
  stat tracking directly (online players live, offline players from their stats file), no
  new event tracking needed. Exposes `{leaderboard_<board>:<rank>:name|value}` placeholders
  (usable in the new scoreboard's line config) and a read-only dashboard endpoint
  (`/api/leaderboard`).
- **Leaderboard boards are now config-driven** (`leaderboard.json`), not hardcoded — admins
  can register any vanilla-tracked per-player stat (blocks mined by type, mobs killed by
  type, distance traveled, deaths, ...) as a board just by adding an entry, using the same
  criteria-string format vanilla's own `/scoreboard objectives add` accepts. Also adds
  "custom" boards — point totals nothing in Minecraft tracks, settable via
  `/leaderboard admin set|add|reset|create|delete`, the dashboard, or another mod (see
  `LeaderboardAPI`, a one-line integration surface for external mods to register their own
  boards, mirroring `PlaceholderAPI`).
- **`/leaderboard hologram create <board> <id> [lines]`** — generates a hologram with ranked
  leaderboard lines in one command instead of typing each line by hand. Holograms already
  resolve `{placeholder}` tokens live on a refresh timer, so the generated
  `{leaderboard_<board>:<rank>:name|value}` lines keep updating on their own — no new
  rendering mechanism, just a convenience generator on top of the existing hologram system.

### Fixed
- `/permissions group <group> setprefix|setsuffix` no longer surfaces a raw, unhelpful
  "unexpected error" when the internal permission manager isn't initialized (e.g. an
  external permissions plugin like LuckPerms is active) — it now explains why instead of
  crashing into a generic error, matching the error handling every other group-editing
  subcommand already had.
- `{luckperms_group}`/`{luckperms_primary_group}`/`{ftbranks_rank}`/`{ftbranks_group}`
  placeholders no longer silently resolve to an empty string when an external permissions
  plugin (LuckPerms/FTB Ranks) is actually active — they went straight to the internal-only
  permission manager instead of checking the active external adapter first, exactly the
  scenario these placeholders exist for. `{luckperms_prefix}`/`{suffix}`/`{displayname}` and
  the `{ftbranks_}` equivalents were unaffected.
- **Nicknamed players (`/nick`) now keep their permission-group prefix/suffix in the tab
  list.** Vanilla's tab-list rendering only wraps a row with the scoreboard team's prefix/
  suffix when there's no display-name override — a nickname was sent as a bare display-name
  override, so it was shown completely verbatim with the prefix/suffix silently dropped the
  instant a nickname was set (reported as "the nickname overrides it and only shows the
  nickname in tab", and appeared group-dependent purely because whichever test player
  happened to be nicknamed lost their prefix). The override now always contains prefix +
  nickname + suffix, and stays in sync automatically if the prefix/suffix changes later
  (promotion, AFK toggle, config reload) without needing to re-run `/nick`.
- **`PermissionAPI.getPrefix()`/`getSuffix()` now fall back to the internal permission system
  when the active external adapter (LuckPerms/FTB Ranks) has no opinion**, matching the
  fall-through contract `getGroupWeight()`/`getPrimaryGroup()` already correctly had. Two
  concrete effects: FTB Ranks servers previously got no prefix/suffix through this mod at
  all, ever (`FtbRanksAdapter` never implemented them, and the old code refused to fall back
  once any external adapter was active) — now they correctly fall back to NeoEssentials' own
  `permissions.json` prefix/suffix. And on LuckPerms, a group with no `prefix`/`suffix` meta
  node set now also falls back to an internal prefix for that same group name if one's
  configured, instead of silently showing nothing — this was very likely the cause of an
  earlier "one group shows a prefix, another doesn't" report.

---
