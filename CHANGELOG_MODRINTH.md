# NeoEssentials — Changelog (mc-26.1-port branch)

**Minecraft 26.1.2 · NeoForge 26.1.2.76+**

All notable changes to this branch are documented here, starting from where it
forked off `Dev-Builds` to begin the Minecraft 26.1.2 port.

> Build numbers below are approximate except build.482 (the real build counter
> isn't tracked in git for historical commits).
>
> **Known gap:** this branch hasn't yet merged several `Dev-Builds` fixes from
> 07-06/07-08 (after this branch had already forked) — including a
> `/jail`-breaking NPE, permission precedence, kit and Auction House
> duplication fixes, dashboard security hardening, and `/invseeedit`/`/pay`/
> `/eco` fixes. These need porting over separately.

---

## 1.0.2.6+build.~330 — 2026-07-04

### 🎉 Port Started: Minecraft 26.1.2 / NeoForge 26.1.2.76

This branch forks from `Dev-Builds` here to begin porting NeoEssentials to
Minecraft 26.1.2. Mojang introduced a new versioning scheme with 26.1.2 and
NeoForge 26.1.2.76 carries substantial breaking API changes over 1.21.1 —
this is not a drop-in recompile.

### 🔀 Initial API Migration
Five commits mechanically migrating the codebase toward NeoForge 26.1.2.76 /
Minecraft 26.1.2 compatibility (reduced compile errors from 645 to 103):
`ResourceLocation`→`Identifier` renames, permission-level API changes,
`GameProfileCache` replacement, `teleportTo` signature changes, `ClickType`→
`ContainerInput`, the new `WorldClock` time API, inventory
armor/offhand restructuring, registry/`CompoundTag` `Optional`-wrapping
changes, and several other mechanical renames.

---

## 1.0.2.6-mc26.1.2+build.~460 — 2026-07-09

### 🚀 Milestone
The full Minecraft/NeoForge 26.1 API migration is complete — the mod compiles
and boots to "Done" with no exceptions on Minecraft 26.1.2 / NeoForge
26.1.2.76.

### 🔧 Maintenance
- Build version string now tags the target Minecraft version
  (`1.0.2.6-mc26.1.2+build.N`).

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
