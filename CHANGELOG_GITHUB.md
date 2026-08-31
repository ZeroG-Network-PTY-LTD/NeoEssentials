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
- **Leaderboards can now rank things other than players.** Every board previously assumed its
  entries were real Minecraft players (a UUID resolved to a name via a Mojang profile
  lookup) — there was no way to build a "top shops by sales" board at all. Generalized the
  entry/provider contract (new `NamedStatProvider` interface) so a board can be keyed by any
  stable id with its own display name, and added a `shop_sales` board built on it, ranking
  sign/chest and NPC shops by total revenue (a field that didn't exist before — shops only
  tracked a sale count, not money moved, and NPC shops tracked nothing at all).
- **Per-board refresh interval** — `leaderboard.json` boards can now set `refreshInterval`
  (seconds a cached ranking is served before rebuilding), instead of every board sharing one
  hardcoded 60s window. Applies to boards registered by external mods via `LeaderboardAPI`
  too.
- **Leaderboard styling** — three opt-in additions, all backward-compatible (a board with none
  of these set looks exactly as before): automatic per-rank medal/color placeholders
  (`{leaderboard_<board>:<rank>:medal|rankcolor}`, usable anywhere placeholders resolve —
  holograms, scoreboard, tablist); per-board `entryFormat`/`headerFormat` line templates for
  `/leaderboard`'s own chat output (same `&`/hex/gradient support as tablist/scoreboard
  lines); and a new paginated chest-GUI viewer (`/leaderboard <board> gui`) with real player
  heads for player entries and a configurable icon for non-player entries.
- **`/tpr`/`/rtp` can now open a biome-select GUI instead of teleporting instantly** — opt-in
  via `teleportation.randomTeleportSettings.mode: "gui"` (default `"command"` is unchanged
  today's behavior). The GUI lists every biome the current dimension can generate — read live
  from the dimension's own generator, so modded biomes show up automatically with zero
  config — plus a "Random — Any Biome" button. Picking a biome searches for it using the same
  engine `/locate biome` uses, then finds a safe spot the same way plain RTP does; if it can't
  find that biome within the configured search radius or the world border, the player gets a
  clear error instead of a bad teleport. Biome icons default to that biome's own
  sapling/propagule/fungus (or a dedicated block for biomes with no tree), and admins can pin
  specific biomes to fixed GUI slots and/or override their icon via a new
  `biomeMenuItems` config list — everything not pinned still auto-fills the remaining slots,
  including biomes this mod doesn't specifically recognize.

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
- **Five more places had the identical "goes straight to the internal-only permission manager"
  bug** as the fixes above, found by auditing every remaining call site: the public
  `NeoEssentialsAPI.getPermissionsService().getGroup()` contract, the `{group}`/
  `{neoessentials_group}` placeholder (the most widely-used group token — chat, MOTD,
  holograms), `TablistLayout`'s BTLP-style per-group column bucketing, chat badge group
  gating, and player tags — all silently returned `""`/`"default"` for every player whenever
  LuckPerms/FTB Ranks was active. All five now correctly check the active external adapter
  first. Also hardened Discord-role permission sync to fail with a clear message instead of a
  generic error when an external adapter is active (it writes to the internal group directly,
  which has no external-adapter equivalent to redirect to).
- **Config-version upgrades (the mechanism that merges new default keys/boards into an
  existing config file on update) were silently failing on Windows.** The merge held a file
  reader open on the config file while trying to rename a new version over it — Windows
  blocks that rename while the file's still open for reading (POSIX allows it, which is why
  this never surfaced before). Every config using version-tracked upgrades was affected, not
  just leaderboards.
- **A new default board (like the `shop_sales` board above) never actually reached an
  existing install even after that fix** — the generic config-merge logic only adds a key
  that's missing entirely; it doesn't know how to merge a new entry into a JSON array that
  already exists on disk (`leaderboard.json`'s `boards` list). Added a dedicated merge step
  that appends new default boards by id without touching or duplicating any board an admin
  already has, including ones they've customized.
- **`/tpr`/`/rtp` could lag or watchdog-crash a server, especially one without pre-generated
  terrain.** Reported as "every time I rtp it lags out the whole server, and then crash" — root
  cause was excessive main-thread-blocking chunk generation per teleport: the safety-preload
  step force-generated a 3×3 grid (9 chunks) around every destination unconditionally, even
  for RTP, which already verifies its own exact landing spot and never reads the 8 neighbour
  chunks — that's now skipped for RTP specifically. Refilling the background location cache
  after a successful teleport also used to fire the entire gap up to `cacheThreshold` (up to
  10 forced generations) as one burst immediately after teleporting, stacking a second lag
  spike right on top of the first — now capped via a new `prewarmBatchSize` config (default 2),
  spreading the refill across several `/tpr` uses instead of bursting it all at once. Also
  lowered `defaultMaxRange`/`findAttempts`/`cacheThreshold` shipped defaults (10000→5000,
  10→6, 10→5) for new installs, and `/tpr` now actually announces its warmup delay
  ("Teleporting in 3 second(s) — move to cancel.") — the move-to-cancel behavior already
  existed (same warmup every teleport command shares) but was never visible, reported as
  "no way of escaping it."
- **Splitting your config (`/neoe config split`) silently dropped web dashboard settings and
  your storage backend choice entirely** — reported as "settings reverted to default, can't
  find them in any config file" after splitting. Root cause: `webDashboard` and `storage` were
  never registered in `ConfigSplitter`'s section list at all, so neither was ever written to
  any split file — every dashboard/storage getter just silently fell back to its hard-coded
  default forever after (dashboard port/auth/UI settings back to defaults; storage backend
  reverting to `"json"` regardless of a configured SQLite/MySQL setup). `webDashboard` now gets
  its own dedicated `dashboard.json`; `storage` now lives in `main.json`. Existing split
  installs get both auto-created/repaired on next startup (with default values, since neither
  ever existed in a split file to recover real values from) — your original customized values
  are still recoverable from `config.json.backup` (created automatically by every
  `/neoe config split`), see the Split Config wiki page for the exact recovery steps.

---
