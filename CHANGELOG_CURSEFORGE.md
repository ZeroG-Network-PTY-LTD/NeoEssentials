# NeoEssentials — Changelog

Starting from **v1.0.3** — earlier history (v1.0.2.x and before) is not carried over.

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## 1.0.3+build.9 — 2026-07-12

### ✨ Pluggable Storage Backends (JSON / YAML / SQLite / MySQL)

- New `storage` section in `config.json` lets you pick JSON (default), YAML,
  SQLite, or MySQL as the backend for supported data. MySQL enables true
  multi-server shared data — point every server in your network at the same
  database and they share bans/mutes/etc. in real time.
- This release migrates the moderation system (bans, mutes, kicks, warns,
  notes, reports) onto the new system. Existing data is imported automatically
  and losslessly the first time you switch backends.
- Economy, homes, warps, kits, permissions, and the rest of the mod's data
  are unaffected for now — still plain JSON, with the same treatment planned
  for a future update.

---

## 1.0.3+build.8 — 2026-07-12

### ✨ Moderation System Overhaul

- **Fixed: dashboard bans didn't actually ban anyone.** Two disconnected ban
  stores existed — `/ban` enforced one, the dashboard wrote to the other, and
  they never talked to each other. Consolidated onto one canonical, UUID+IP-aware
  store with ban IDs, full history, and an unban audit trail (who/when).
- **Mutes** now track reason, staff attribution, full history, and an unmute
  audit trail (previously just a bare name→expiry map), plus new IP-mute support.
- **Kicks** are now recorded with a queryable history (previously fire-and-forget).
- **New:** staff notes (`/note`, `/notes`) and player reports (`/report`,
  `/reports`, `/reviewreport`) — players can report others even while staff
  are offline, and staff can leave freeform notes on a player's record.
- The dashboard's moderation API now exposes all of the above, including new
  IP-ban/IP-mute/kick/note/report routes that didn't exist before.

---

## 1.0.3+build.7 — 2026-07-11

### 🐛 Bug Fixes

- **Dashboard account login history reset on every restart:** last-login time,
  last-login IP, failed-attempt count, and lockout state were tracked correctly
  in memory but never actually saved to `dashboard_users.json` — a server
  restart silently wiped all of it. Now persisted and restored correctly.
- **Locale-dependent number formatting corrupted dashboard API data:** TPS,
  tick time, memory/CPU percentages, and backup size fields used the server's
  default locale for decimal formatting, producing `"19,5"` instead of `"19.5"`
  on comma-decimal locales — external dashboard clients parsing these as
  numbers would silently get truncated/wrong values. Now always formatted with
  a fixed locale regardless of server language settings.
- **Dashboard API 500'd on empty POST bodies from non-PHP clients:** routes like
  reload permissions, backup create/restore, and cloud-storage config crashed
  whenever the caller sent an empty JSON array `[]` instead of an empty object
  `{}` for a parameterless action (which is how PHP/Laravel serializes an empty
  parameter list) — a real issue for the standalone dashboard app. These now
  treat an empty array the same as no parameters at all instead of erroring.

---

## 1.0.3+build.6 — 2026-07-11

### 🐛 Bug Fixes

- **Tablist `playerFormat` spacing had no effect:** editing `playerFormat` in
  `tablist.json` — including adding spaces between `{prefix}`/`{player}`/`{suffix}` —
  never actually changed anything, since the setting was loaded but never applied.
  Now the literal text around each token is correctly folded into the prefix/suffix
  sent to the client, so spacing (and other literal text in the template) actually
  renders. Note: the default template's `&f`/`&r` color codes now also apply, which
  they never did before — a visible change for servers that never touched this
  setting.

---

## 1.0.3+build.480 — 2026-07-10

### 🐛 Bug Fixes

- **ChestShop double chests:** Buy/sell, stock checks, and dynamic pricing only read
  one half of a double chest, so filling/emptying one side could block transactions
  even with space/stock free on the other side. Now reads the full combined 54-slot
  inventory.
- **ChestShop admin shop holograms:** `/chestshop hologram enable|disable|move`
  could never be used on admin shops (the ownership check required a player-owner
  UUID that admin shops don't have). Now authorized via
  `neoessentials.shop.create.admin`.

### ✨ Improvements

- **Command feedback messages** now show a small `[NE]` tag and use softer,
  vanilla-matching colors instead of harsh neon ones, so they're both more
  recognizable and easier on the eyes.
- **NPC Shops:** Selling now actually works (previously configuring a sell price did
  nothing — only buying was implemented). Added a permission check
  (`neoessentials.shop.use`) before opening the shop menu. Added
  `/npcshop respawn <shopId>` to re-summon a shop's NPC if it's ever lost (e.g. void
  damage) without losing its configured listings.

### 🔧 Maintenance
- Build version string now includes the target Minecraft version
  (`1.0.2.6-mc1.21.1+build.N`), matching the 26.1.x port branch.
