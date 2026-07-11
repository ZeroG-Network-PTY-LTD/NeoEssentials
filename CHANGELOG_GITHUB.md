# Changelog — NeoEssentials (mc-26.1-port branch)

All notable changes to this branch are documented here, starting from
**v1.0.3** — earlier history (v1.0.2.x and before, including the port's own
migration history) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 26.1.2 · NeoForge 26.1.2.76+**

> **Known gap:** this branch has not yet merged several `Dev-Builds` fixes
> landed on 07-06/07-08 (after this branch had already forked): permission
> precedence and a `getUser()` race, freeze/jail/vanish/mute enforcement gaps,
> kit permanent-lockout/double-claim/data-loss fixes, an Auction House
> duplication exploit fix, jail bounds ignoring dimension, web dashboard
> admin-role/password-hashing security fixes, the `/jail` NPE (jailing is
> currently broken on this branch), `localize()` overload/placeholder-shift
> fixes, `/invseeedit`/`/pay`/`/eco` fixes, and the tablist nametag feature.
> These need porting over separately.

---

## [1.0.3-mc26.1.2+build.482] — 2026-07-10

### 🐛 Bug Fixes

#### ChestShop Double Chests Only Reading One Half
**Commands:** `/chestshop` (buy/sell, stock checks, dynamic pricing)

- **Root cause:** `ShopTransaction.getChest()` and `SupplyDemandRule.getStock()` cast
  the block entity at the shop's chest position directly to `ChestBlockEntity`, which
  only exposes that one 27-slot half of a double chest. Buy/sell stock checks, item
  add/remove, low-stock notifications, and supply/demand pricing all only saw one
  sign's own chest half instead of the shared 54-slot inventory.
- **Fix:** Both now use `HopperBlockEntity.getContainerAt(level, pos)` — the same
  helper vanilla hoppers use to pull from chests — which returns the properly
  combined container for double chests (single chests are unaffected).

#### Admin Shops Could Never Use `/chestshop hologram enable|disable|move`
- **Root cause:** `ShopCommand.isShopOwner()` only checked
  `shop.ownerUUID.equals(player)`, but admin shops have `ownerUUID == null` by
  design, so the check failed unconditionally for every player — including whoever
  holds `neoessentials.shop.create.admin`.
- **Fix:** Admin shops are now authorized via `neoessentials.shop.create.admin`,
  matching the pattern already used for admin-shop item assignment.

### ✨ Improvements

#### Command Feedback Messages: Branded Tag + Softened Colors
- `MessageUtil.success()`/`error()`/`warning()`/`info()` now prepend a short
  `[NE]` tag (`§8[§bNE§8] `) and use vanilla-matching soft colors (same RGB as
  `§a`/`§c`/`§e`/`§b`) instead of harsh neon primaries. Scoped to the four
  command-feedback wrapper methods only — `localize()` itself is untouched.

#### NPC Shops: Sell Support, Permission Checks, and Entity Recovery
- **Sell was completely non-functional.** `ShopListing` carries a `sellPrice`,
  `/npcshop additem` lets you configure one, and the GUI lore even advertised
  "Sell: $X" — but the shop menu only ever handled buying (right-click a slot).
  Added left-click-to-sell handling, mirroring the buy flow, reusing
  `ShopTransaction`'s `countItems()`/`removeItems()` (widened to `public` for
  this cross-package reuse). On this branch, `AbstractContainerMenu#clicked`'s
  click-type parameter is `ContainerInput` (renamed from `ClickType` in the
  26.1 port), so the left/right-click dispatch uses that instead.
- **No permission check on NPC shop transactions at all.** Added a
  `neoessentials.shop.use` check before the shop menu opens.
- **No way to recover a shop whose NPC entity was lost** without losing its
  listings. Added `/npcshop respawn <shopId>`, which re-summons the NPC at its
  stored spawn position and re-links it to the existing shop data.
- These new messages use raw `Component.literal` (not `MessageUtil`/lang keys)
  to match this branch's current state of the NPC shop files, which predate
  the `MessageUtil` localization pass applied elsewhere.
