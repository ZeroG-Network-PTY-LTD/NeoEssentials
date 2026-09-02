# NeoEssentials — Changelog (26.1.x branch)

**Minecraft 26.1–26.1.2 · NeoForge 26.1.2.76+**

All notable changes to this branch are documented here, starting from
**v1.0.6** — earlier history (v1.0.5.x and before, including the port's own
migration history) is not carried over.

---

## [1.0.6] — 2026-08-27

### Added
- Sidebar scoreboard system — `/scoreboard` with config-driven boards, conditions,
  animation, group/player overrides, a toggle command, and a dashboard endpoint.
- General leaderboard system — `/leaderboard` (`/lb`), with `money`/`kills`/`mob_kills`/
  `playtime` boards and `{leaderboard_<board>:<rank>:name|value}` placeholders.
- Leaderboard boards are now config-driven (`leaderboard.json`) — register any vanilla
  per-player stat (blocks mined, mobs killed, distance, etc.) as a board via config, plus
  "custom" point-total boards (`/leaderboard admin set|add|reset|create|delete`) and a
  `LeaderboardAPI` for other mods to register their own boards.
- `/leaderboard hologram create <board> <id> [lines]` — generates a ranked leaderboard
  hologram in one command; the lines keep updating live on their own.
- Leaderboards can now rank non-players — a new `shop_sales` board ranks sign/chest and NPC
  shops by total revenue, shown by shop name instead of a player name.
- Per-board `refreshInterval` in `leaderboard.json` (was a fixed 60s for every board).
- Leaderboard styling: automatic per-rank medal/color placeholders (`:medal`/`:rankcolor`),
  per-board `entryFormat`/`headerFormat` chat templates, and a new paginated GUI viewer
  (`/leaderboard <board> gui`) with real player heads and per-board icons.
- `/tpr`/`/rtp` can open a biome-select chest GUI instead of teleporting instantly — opt-in
  via `randomTeleportSettings.mode: "gui"` (default unchanged). Lists every biome the
  dimension can generate (modded biomes included automatically), plus a "Random — Any Biome"
  button; picks a real spot in that biome or shows a clear error if it can't find one within
  the server border. Icons default to that biome's sapling/dedicated block; admins can pin
  biomes to fixed slots and/or override icons via `biomeMenuItems`.
- Votifier vote listener — its own TCP port, both V1 (RSA) and V2 (NuVotifier-compatible)
  protocols auto-detected, per-site reward config (commands/crate keys), offline vote queueing,
  vote party, and `/vote`/`/votes`/`/voteparty` commands.
- Crates — weighted reward-pool crates with a key balance system, opened via `/crate open` or
  a physical block, three opening animations (roulette/sequential/instant), and full admin
  commands to define crates/rewards in-game. Votifier can grant crate keys per vote site.

### Fixed
- `/permissions group <group> setprefix|setsuffix` no longer shows a raw "unexpected
  error" when the internal permission manager isn't active (e.g. an external permissions
  plugin like LuckPerms is in use) — it now explains why instead.
- `{luckperms_group}`/`{luckperms_primary_group}`/`{ftbranks_rank}`/`{ftbranks_group}`
  placeholders no longer resolve to an empty string when LuckPerms/FTB Ranks is actually
  active — `prefix`/`suffix`/`displayname` variants were unaffected.
- Nicknamed players (`/nick`) now keep their permission-group prefix/suffix in the tab list —
  a nickname used to silently drop it the instant it was set, on any group.
- `PermissionAPI.getPrefix()`/`getSuffix()` now fall back to the internal permission system
  when LuckPerms/FTB Ranks has no opinion, matching `getGroupWeight()`/`getPrimaryGroup()`'s
  existing behavior. FTB Ranks servers previously got no prefix/suffix at all through this
  mod, ever; LuckPerms groups with no `prefix`/`suffix` meta set now fall back to an internal
  one too instead of showing nothing.
- Five more places had the identical internal-manager-only bug (found by auditing every
  remaining call site): the public `NeoEssentialsAPI` group lookup, the `{group}` placeholder,
  per-group tablist column bucketing, chat badge group gating, and player tags all silently
  fell back to `""`/`"default"` under LuckPerms/FTB Ranks — now fixed the same way.
- Config-version upgrades (the merge that adds new default keys/boards to an existing config
  on update) were silently failing on Windows — fixed. Also fixed new default boards (like
  `shop_sales`) not reaching an already-upgraded install even after that fix.
- `/tpr`/`/rtp` could lag or crash a server, especially without pre-generated terrain — it was
  force-generating far more chunks per teleport than necessary (an unneeded 9-chunk safety
  grid for RTP specifically, plus a 10-chunk burst refilling the background cache right after
  every teleport). Both fixed; new `prewarmBatchSize` config caps the refill burst, and lower
  default range/attempts reduce worst-case cost on fresh servers. `/tpr` also now announces
  its warmup delay and that moving cancels it — that already worked, it just wasn't visible.
- Splitting your config (`/neoe config split`) silently dropped web dashboard settings and your
  storage backend choice entirely — neither was ever registered in the split-config section
  list, so both always fell back to defaults after splitting (dashboard settings, and storage
  reverting to `"json"` even if you had SQLite/MySQL configured). Both fixed — `webDashboard`
  now gets its own `dashboard.json`, `storage` now lives in `main.json`. See the Split Config
  wiki page if you already hit this — your original values are recoverable from
  `config.json.backup`.
- Right-clicking a physical crate block ran the whole open flow twice per click (duplicated
  "no keys" message, could double-consume keys) — fixed.
- A crate with no rewards configured yet silently ate a key and reported the misleading "no
  keys" error instead of "no rewards configured" — fixed; the key is no longer spent unless a
  reward actually exists.
- Tab-completion was silently missing on ~25 command arguments across 16 files (crate/hologram/
  home/board/group names, item ids, player targets, etc.) — full audit, all fixed.

---
