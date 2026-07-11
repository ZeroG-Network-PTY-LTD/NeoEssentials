# NeoEssentials — Changelog

Starting from **v1.0.3** — earlier history (v1.0.2.x and before) is not carried over.

**Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

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
