# NeoEssentials — Changelog (mc-26.1-port branch)

**Minecraft 26.1.2 · NeoForge 26.1.2.76+**

All notable changes to this branch are documented here, starting from
**v1.0.3** — earlier history (v1.0.2.x and before, including the port's own
migration history) is not carried over.

> **Known gap:** this branch hasn't yet merged several `Dev-Builds` fixes from
> 07-06/07-08 (after this branch had already forked) — including a
> `/jail`-breaking NPE, permission precedence, kit and Auction House
> duplication fixes, dashboard security hardening, and `/invseeedit`/`/pay`/
> `/eco` fixes. These need porting over separately.

---

## 1.0.3-mc26.1.2+build.7 — 2026-07-11

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

## 1.0.3-mc26.1.2+build.482 — 2026-07-10

### 🐛 Bug Fixes

- **ChestShop double chests:** Buy/sell, stock checks, and dynamic pricing only
  read one half of a double chest. Now reads the full combined 54-slot
  inventory.
- **ChestShop admin shop holograms:** `/chestshop hologram enable|disable|move`
  could never be used on admin shops. Now authorized via
  `neoessentials.shop.create.admin`.

### ✨ Improvements

- **Command feedback messages** now show a small `[NE]` tag and use softer,
  vanilla-matching colors.
- **NPC Shops:** Selling now actually works (previously only buying was
  implemented). Added a permission check before opening the shop menu. Added
  `/npcshop respawn <shopId>` to re-summon a lost shop NPC without losing its
  listings.
