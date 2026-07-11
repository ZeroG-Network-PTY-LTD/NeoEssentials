# Changelog — NeoEssentials (mc-26.1-port branch)

All notable changes to this branch are documented here, starting from where it
forked off `Dev-Builds` to begin the Minecraft 26.1.2 port.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 26.1.2 · NeoForge 26.1.2.76+**

> Build numbers below are **approximate** except build.482 — the real per-commit
> build counter isn't tracked in git for historical commits, but dates and
> content are accurate to this branch's actual commit history.
>
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

## [1.0.2.6+build.~330] — 2026-07-04

### 🎉 Port Started: Minecraft 26.1.2 / NeoForge 26.1.2.76

This branch forks from `Dev-Builds` here to begin porting NeoEssentials to
Minecraft 26.1.2. Mojang introduced a new versioning scheme with 26.1.2 and
NeoForge 26.1.2.76 carries substantial breaking API changes over 1.21.1 —
this is not a drop-in recompile.

### 🔀 Initial API Migration (645 → 103 compile errors)

Five commits mechanically migrating the codebase to compile against NeoForge
26.1.2.76 / Minecraft 26.1.2, verified against the real decompiled 26.1.2 sources
rather than guessing:

- `ResourceLocation` → `Identifier` (renamed starting in 1.21.11, carries into
  26.1); `ResourceLocationArgument` → `IdentifierArgument`;
  `ResourceKey.location()` → `.identifier()`.
- `ChunkPos` is now a record — `.x`/`.z` field access → `.x()`/`.z()`.
- `CommandSourceStack.hasPermission(int)`/`Player.hasPermissions(int)` removed
  for a granular `Permission`/`PermissionSet` model — added `PermissionLevelCompat`
  mapping the old int levels onto `PermissionLevel.byId()`.
- `ServerPlayer.getServer()` removed — replaced with `player.level().getServer()`.
- `CompoundTag`'s single-arg getters now return `Optional<T>` instead of a raw
  primitive with an implicit default — simplified `CompoundTagCompat` to forward
  to the new `get<Type>Or(key, default)` overloads.
- `GameProfile` is now a record — `getName()`/`getId()`/`getProperties()` →
  `name()`/`id()`/`properties()`.
- `GameProfileCache` removed entirely in favor of `MinecraftServer.services()`'s
  `nameToIdCache()` (`UserNameToIdResolver`, lightweight name↔uuid lookups) and
  `profileResolver()` (`ProfileResolver`, full `GameProfile` with textures —
  needed by the tablist fake-player skin feature and `/skull`, which would have
  silently lost skin data with a naive swap). `BanManager.resolveProfile()` now
  returns `NameAndId`. `ResolvableProfile` construction goes through the new
  `ResolvableProfile.createResolved(GameProfile)` factory.
- `Entity.teleportTo(ServerLevel, double, double, double, float, float)` removed
  — the remaining overload takes an explicit `Set<Relative>` and reset-camera
  flag; updated all 20 call sites (and fixed an argument-order bug introduced
  in the first pass — `Set<Relative>` belongs after x/y/z, not right after the
  level).
- `AbstractContainerMenu#clicked`'s `ClickType` parameter renamed to
  `ContainerInput` (same enum values/order) — mechanical rename across the
  auction house / NPC shop menu classes.
- `Level.getDayTime()`/`setDayTime(long)` replaced by a registry-driven
  `WorldClock`/`ServerClockManager` system — added `WorldClockCompat` wrapping
  the dimension's default clock so the rest of the codebase keeps treating world
  time as a single tick counter.
- `Inventory.items`/`armor`/`offhand` fields gone — armor+offhand moved into a
  separate `EntityEquipment` object; switched to
  `player.getItemBySlot(EquipmentSlot)`/`setItemSlot(...)`.
- `RegistryAccess.registryOrThrow()` → `.lookupOrThrow()`;
  `Level.getSharedSpawnPos()` removed → `level.getRespawnData().pos()`;
  `CompoundTag.contains(String, int)` removed; `ListTag.getCompound(int)` etc.
  now return `Optional<T>` too.
- `BuiltInRegistries.ITEM.get(Identifier)` now returns
  `Optional<Holder.Reference<Item>>` — switched to `DefaultedRegistry`'s
  `getValue(Identifier)` (keeps the old implicit-AIR-default behavior) at all 12
  call sites.
- `@EventBusSubscriber`'s `bus=` parameter/`Bus` enum removed entirely.
- `BlockEvent.BreakEvent` relocated to a standalone top-level class,
  `net.neoforged.neoforge.event.level.block.BreakBlockEvent` — mechanical
  rename across the shop sign-break and moderation block-break listeners.
- `Player.playNotifySound(...)` removed — added `SoundCompat` wrapping the raw
  `ClientboundSoundPacket` it used to send internally.

---

## [1.0.2.6-mc26.1.2+build.~460] — 2026-07-09

### 🚀 Milestone: Mod Builds and Boots on Minecraft 26.1.2

The full Minecraft/NeoForge 26.1 API migration is complete — `gradlew compileJava`
succeeds and `runServer` boots to `"Done"` with no exceptions on Minecraft 26.1.2 /
NeoForge 26.1.2.76. Verified against the real decompiled 26.1.2 sources (via the
NeoForm runtime cache) rather than guessing at API shapes.

Covers the remainder of the migration beyond the initial batch above: moved/renamed
entity and item classes (`Cat`, `Arrow`, `Boat`, fireballs, thrown items,
`SwordItem`/`PickaxeItem`/`ArmorItem`/`EnchantedBookItem` removal),
`Entity.moveTo` → `snapTo`, `HoverEvent`/`ClickEvent` becoming sealed-interface
records, `CompoundTag` UUID/list API changes, registry `Holder`-based lookups,
`DimensionType`/`Difficulty`/`MinecraftServer`/`FoodData` accessor removals,
`GameRules` package move, `ChunkPos` factory method, `EntityType.create`'s new
spawn-reason requirement, tab-list/set-time packet constructor changes (the new
`WorldClock` system), `NeoForgeServerConfig` rename, `GameProfile`/`PropertyMap`
becoming records, `PermissionSet` replacing int permission levels, recipe-result
lookup via `RecipeDisplay`/`SlotDisplay`, and server-global weather.

### 🔧 Maintenance
- Build version string now tags the target Minecraft version
  (`1.0.2.6-mc26.1.2+build.N`) — jars from this branch previously used the same
  version string as 1.21.1 builds, making them indistinguishable.

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

