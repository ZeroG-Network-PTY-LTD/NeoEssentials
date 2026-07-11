# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here, starting from
**v1.0.3** — earlier history (v1.0.2.x and before) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## [1.0.3+build.480] — 2026-07-10

### 🐛 Bug Fixes

#### ChestShop Double Chests Only Reading One Half
**Commands:** `/chestshop` (buy/sell, stock checks, dynamic pricing)

- **Root cause:** `ShopTransaction.getChest()` and `SupplyDemandRule.getStock()` cast
  the block entity at the shop's chest position directly to `ChestBlockEntity`, which
  only exposes that one 27-slot half of a double chest. Buy/sell stock checks, item
  add/remove, low-stock notifications, and supply/demand pricing all only saw one
  sign's own chest half instead of the shared 54-slot inventory — filling/emptying
  one side blocked transactions on the other sign even though the double chest still
  had space/stock.
- **Fix:** Both now use `HopperBlockEntity.getContainerAt(level, pos)` — the same
  helper vanilla hoppers use to pull from chests — which returns the properly
  combined container for double chests (single chests are unaffected).

#### Admin Shops Could Never Use `/chestshop hologram enable|disable|move`
- **Root cause:** `ShopCommand.isShopOwner()` only checked
  `shop.ownerUUID.equals(player)`, but admin shops have `ownerUUID == null` by
  design, so the check failed unconditionally for every player — including whoever
  holds `neoessentials.shop.create.admin`. The hologram opt-in prompt still appeared
  at admin shop creation (it doesn't check shop type), but clicking it always failed
  with an "owner only" error.
- **Fix:** Admin shops are now authorized via `neoessentials.shop.create.admin`,
  matching the pattern already used for admin-shop item assignment.

### ✨ Improvements

#### Command Feedback Messages: Branded Tag + Softened Colors
- `MessageUtil.success()`/`error()`/`warning()`/`info()` now prepend a short
  `[NE]` tag (`§8[§bNE§8] `) so players can tell at a glance which mod a message
  came from — useful on servers running several mods with similarly-colored chat
  output.
- Replaced the harsh neon primary colors (`0x00FF00`/`0xFF0000`/`0xFFFF00`/`0x00FFFF`)
  with vanilla-matching soft colors (same RGB as `§a`/`§c`/`§e`/`§b`), consistent
  with the inline `§` colors most lang templates already use.
- Scoped to the four command-feedback wrapper methods only — `localize()` itself is
  untouched, so logs/audit trails/transaction history that read the raw translated
  string are unaffected.

#### NPC Shops: Sell Support, Permission Checks, and Entity Recovery
- **Sell was completely non-functional.** `ShopListing` carries a `sellPrice`,
  `/npcshop additem` lets you configure one, and the GUI lore even advertised
  "Sell: $X" — but the shop menu only ever handled buying (right-click a slot).
  There was no left-click/sell path at all. Added sell handling (left-click a
  listing), mirroring the buy flow: verify the player holds enough of the item,
  credit their balance, then remove the items (rolling back the credit if removal
  unexpectedly fails).
- **No permission check on NPC shop transactions at all.** ChestShop enforces
  `neoessentials.shop.use` before every buy/sell; NPC shops didn't check any
  permission. Added the same check before the shop menu opens.
- **No way to recover a shop whose NPC entity was lost** (void damage bypasses
  `setInvulnerable`, a stray `/kill`, etc.) without losing its listings — the
  relevant recovery methods existed but were never called from anywhere. Added
  `/npcshop respawn <shopId>`, which re-summons the NPC at its stored spawn
  position and re-links it to the existing shop data.

### 🔧 Maintenance
- Build version string now includes the target Minecraft version
  (`1.0.2.6-mc1.21.1+build.N`), matching the format already used on the 26.1.x port
  branch so builds from either branch are distinguishable at a glance.
- `_langVersion` bumped to 23 for the new/changed lang keys in this release.
