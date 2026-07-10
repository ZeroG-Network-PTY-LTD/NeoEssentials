# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## [1.0.2.6+build.480] — 2026-07-10

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

---

> The entries below (build.~225 – build.~460) reconstruct work done between
> build.214 and build.480 that was never logged at the time. Build numbers are
> **approximate** — the real per-commit build counter isn't tracked in git — but
> dates and content are accurate to the commit history.

## [1.0.2.6+build.~460] — 2026-07-08

### 🐛 Bug Fixes

#### `/invseeedit` and `/enderchestedit` Permanently Locking Targets
**Commands:** `/invseeedit`, `/ecedit`

- **Root cause:** `InventoryViewCommands.releaseEditLocks()` existed but was never
  called from anywhere — closing the edit GUI didn't release the single-editor lock
  (no close handling existed at all), and disconnecting didn't either despite a doc
  comment claiming so. The first `/invseeedit`/`/enderchestedit` against any target
  permanently locked that target for every future editor — including the original
  viewer — until a server restart cleared the in-memory maps.
- **Fix:** Added a `PlayerContainerEvent.Close` listener in `InventoryViewCommands`
  that releases the viewer's locks whenever their menu closes (a player can only
  have one menu open at a time, so this reliably covers the normal close case), plus
  a `releaseEditLocks()` call in `PlayerJoinQuitHandler.onPlayerQuit` as a disconnect
  backstop.

#### `/pay` Showing Swapped Amount/Player Placeholders and a Decimal Mismatch
- **Root cause:** The `success_fee`/`received_fee` message templates expect
  `{0}` = amount, `{1}` = player, but the call sites passed `(playerName, amount, …)`
  — e.g. "Paid `<player>$` to `1000.0`." instead of "Paid `1000.00$` to `<player>`."
  The raw `amount` `BigDecimal` also kept whatever scale the parsed input had (e.g.
  `1000.0`), while `fee`/`netAmount` always came out at scale 2 from arithmetic,
  causing a `1000.0` vs `1000.00` mismatch within the same message.
- **Fix:** Swapped the argument order to match the templates, and added
  `amount.setScale(2, RoundingMode.HALF_UP)` before display so all three amounts in
  the message are consistently formatted.

#### `/eco give|take|set|reset` Missing Currency Symbol; `/eco take` Never Notified the Target
- **Root cause:** The admin-confirmation templates (`eco.give`/`take`/`set`/`reset`)
  had no currency placeholder at all — unlike the target-facing `_notify` variants —
  so the admin's own confirmation rendered a bare number with no currency symbol.
  Separately, `/eco take` was the only one of the four admin actions with no
  online-target notification at all — the player whose money was taken got no
  indication anything happened.
- **Fix:** Added a `{2}` currency placeholder to all four admin templates and pass
  `getCurrencySymbol()` at each call site. Added a new `eco.take_notify` message and
  the matching notify call, for parity with `give`/`set`/`reset`.

#### `localize()` Overload Ambiguity Shifting `{n}` Placeholders
- **Root cause:** `localize(String key, String fallback, Object... args)` was an
  overload of `localize(String key, Object... args)`. Java's overload resolution
  prefers a fixed `String` parameter over varargs, so any call passing a plain
  `String` as the first substitution argument (the common case for names/reasons)
  silently bound to the fallback overload instead, swallowing that argument and
  shifting every later `{n}` placeholder down by one — with no compile error. This
  was responsible for missing/shifted placeholders across jail/ban/freeze/etc.
  messages in production.
- **Fix:** Renamed the fallback overload to `localizeOrDefault()`, removing the
  ambiguity permanently. Added `MessageUtil.FORCE_REFRESH_KEYS`, a set of keys with
  confirmed broken shipped values, force-refreshed on boot regardless of
  `_langVersion` so already-broken installs converge (bumped 19→22).
- Also fixed several swapped/missing message arguments across moderation/misc
  commands (most notably banlist entries showing the ban reason where "by
  &lt;staff&gt;" was expected and vice versa), and a duplicate-broadcast bug where
  staff with `neoessentials.moderation.notifications` saw every moderation action
  twice — once as their own confirmation, once via the separate staff broadcast.

#### `/jail` Completely Broken (NPE on Every Attempt)
- **Root cause:** `JailManager.jailPlayer()` reserved the jailed-players map slot
  with `jailedPlayers.putIfAbsent(playerId, null)`, but `ConcurrentHashMap` disallows
  `null` values — every jail attempt threw a `NullPointerException` before reaching
  the rest of the method.
- **Fix:** Build the real `JailEntry` first and use it as the sole `putIfAbsent`
  value — both NPE-safe and properly atomic.

#### Tablist Prefix/Suffix Reverting to Blank After First Refresh
- **Root cause:** `TablistLayout.applySortingTeams()` moved sorted players onto a
  sort-only `neL_<weight>_<group>` scoreboard team right after
  `TablistManager.updatePlayerTeam()` had already moved them onto its own
  `ne_<weight>_<group>`/column-key team (which carries the actual prefix/suffix). A
  player can only be on one team, so the second, redundant move undid the first
  every cycle.
- **Fix:** Removed `applySortingTeams()` entirely — `updatePlayerTeam()`'s own
  naming already covers plain, weight-sorted, and BTLP column-key ordering.

### ✨ Features

- **Jail regions:** added cuboid/sphere jail shapes defined via a configurable jail
  wand (right-click = pos1, left-click = pos2) or WorldEdit soft-integration
  (reflection only, no compile-time dependency), with region-wide block break/place
  protection.
- **Tablist nametags:** added an above-head nametag prefix/suffix system with
  per-group/per-player overrides, layered player > group > permission-based default.

### 🧪 Attempted (Reverted)
- Briefly widened `minecraft_version_range` to accept 26.1.x as a version-gate-only
  change. Reverted after confirming the mod hard-crashes on load against the real
  26.1.x API (`NoClassDefFoundError` on a relocated NeoForge event class) — a genuine
  port requires bumping `neo_version` and fixing the resulting compile/runtime
  errors, now tracked separately on the `mc-26.1-port` branch.

---

## [1.0.2.6+build.~400] — 2026-07-06

### 🐛 Bug Fixes

- **Permissions:** fixed group permission precedence, a `getUser()` race condition,
  and LuckPerms group-weight lookup.
- **Moderation enforcement gaps:** closed freeze/jail/vanish/mute enforcement gaps
  across chat, teleportation, and combat — a frozen/jailed/muted player could still
  chat, teleport, or take/deal damage in several code paths that didn't check their
  status.
- **Kits:** fixed a permanent-lockout bug, a double-claim race condition, and item
  data loss on `/kit reload`.
- **Auction House:** fixed an item-duplication exploit and a named-item price bypass
  on `/sell`.
- **Jail:** fixed the bounds check ignoring dimension — a jailed player standing at
  the same X/Z in a different dimension wasn't detected as having escaped.

### 🔒 Security
- Required an admin role on web dashboard endpoints that were previously reachable
  by any authenticated, non-admin account (including the standalone admin endpoint,
  fixed in a follow-up commit the same day).
- Replaced unsalted SHA-256 password hashing with salted PBKDF2WithHmacSHA256 (120k
  iterations); the default admin account now gets a random temporary password
  instead of the previous hardcoded `admin`/`admin123`.

### 🧹 Code Quality
- Replaced hardcoded `Component.literal` strings across ~40 files with
  `MessageUtil`-backed localization keys.

---

## [1.0.2.6+build.~330] — 2026-07-04

### ✨ Features
- Added tablist short-tokens/animations (`{tps}`, `{online}`, etc.) usable directly
  in chat, and `#`-prefixed hex gradient stops.
- ChestShop shop items now preserve full item data components (enchantments, custom
  names, modded NBT) instead of just a bare registry ID when assigned via `?`
  right-click. Added a shift+right-click gesture to (re)assign item data on an
  already-configured shop, and a look-at variant of `/chestshop remove`.
- Permission group prefix/suffix now render as rich text (via `RichTextFormatter`)
  instead of raw color codes.

### 🐛 Bug Fixes
- Fixed `/flyspeed` not actually applying to player flight (was setting a
  non-existent attribute instead of `Abilities.flyingSpeed`).
- Fixed a pending-shop (`?`) autofill ordering bug.

### 📚 Documentation
- Documented the new shop item-data/NBT assignment and shift-click gestures.

---

## [1.0.2.6+build.~300] — 2026-07-03

### 🐛 Bug Fixes
- Fixed unclosed `<gradient>` chat tags corrupting trailing legacy `&`-color codes.
- Fixed ChestShop click-spam (rapid clicking a shop sign could fire multiple
  transactions per swing) and NBT-sensitive item stock matching.

### ✨ Features
- Finished migrating `AuctionHouseCommand` to `MessageUtil`-based messages and
  standardized its command handling (iterated over several commits).
- Added custom skins for fake tablist entries, plus a BTLP-style column-grid
  tablist layout.

### 📚 Documentation
- Corrected several wiki pages to match the actual implementation: economy config
  keys, permission-node/split-config docs, missing `/chestshop` subcommands and
  permission nodes, AFK/hologram/moderation docs, API/chat-channel docs,
  command/permission node names, and kit-management/dashboard docs.

---

## [1.0.2.6+build.~260] — 2026-07-02

### ✨ Features
- Added a configurable movement-distance threshold for AFK detection (previously
  any movement, however small, reset AFK status).
- Landed the initial web dashboard implementation: player management, economy
  adjustment endpoints, and server status reporting.

### 🐛 Bug Fixes
- Hardened enchantment-compatibility checks to tolerate cross-version enchantment
  ID differences instead of crashing.

### 🧹 Code Quality
- Migrated hover-event construction to `HoverEventCompat` for cross-version
  compatibility.
- Refactored `MessageUtil`'s mojibake character-run repair logic.
- Reset `_langVersion` to 0 and reformatted language files.
- Began migrating `AuctionHouseCommand` to `MessageUtil`-based messages (finished
  07-03).

---

## [1.0.2.6+build.~225] — 2026-07-01

### ✨ Features
- Added `localization.preserveCustomTranslations` config option — lets admins opt a
  server's custom lang file out of the automatic merge/auto-fix logic entirely,
  protecting hand-edited translations from ever being touched by the merge system.
- Added web dashboard configuration schema/getters in `ConfigManager` (groundwork
  for the dashboard work landing over the following days), and
  `WarpManager.createWarpByAdmin()` so the dashboard can create warps without a
  `ServerPlayer` context.

### 🧹 Code Quality
- Overhauled `en_us.json`/`ru_ru.json` formatting and bumped `_langVersion`;
  expanded `/help`'s localization coverage.
- Replaced raw `server.tell(new TickTask(...))` calls with a shared
  `DelayedTaskScheduler.schedule()` helper across vanish, jail-respawn,
  misc-teleport, and warp/tpa delay logic.
- Migrated remaining raw click-event construction to `ClickEventCompat` for
  cross-version compatibility.
- Reworked ban message formatting (`BanManager`) for consistency.

---

## [1.0.2.6+build.214] — 2026-07-01

### 🐛 Bug Fixes

#### Inventory & Ender Chest Duplication Exploit Closed
**Commands:** `/invsee`, `/inv`, `/ec`, `/enderchest`

- **Root cause:** `openReadOnlyInventory()` and `openReadOnlyEnderChest()` created a
  `SimpleContainer` filled with `.copy()` items and opened it via the standard
  `ChestMenu.threeRows` / `ChestMenu.sixRows`.  Standard chest menus allow items to
  be freely moved out of the container, so a viewer could drag copies into their own
  inventory while the originals remained in the target's inventory.
- **Fix:** Both read-only methods now use a custom `AbstractContainerMenu` built by
  the new `buildReadOnlyMenu()` helper.  The top-section slots override `mayPickup()`
  → `false` and `mayPlace()` → `false`, making them display-only — items cannot be
  removed from or placed into the snapshot container regardless of client action.
- **Fix (secondary):** `PlayerInventoryContainerMenu` (editable mode) now registers
  the viewer's own inventory and hotbar slots in the menu layout.  Previously they
  were absent, causing server-client desync when the viewer tried to move items
  between the target's inventory and their own.

#### Shop Hologram Orphan / Stale Entity Bugs
- **Root cause 1 — Sign break not handled:** When a player physically broke a shop
  sign, no event handler removed the shop entry or its hologram.  The shop stayed in
  `shops.json` and the hologram entity remained floating in the world indefinitely.
  **Fix:** Added `BlockEvent.BreakEvent` listener in `ShopSignHandler`.  When a sign
  block is broken, the shop at that position (and its hologram) is removed atomically
  via `ShopManager.removeShop()`.
- **Root cause 2 — Manual file edit leaves orphaned holograms:** If a shop was
  manually deleted from `shops.json`, the corresponding hologram ID was still present
  in `holograms.json`.  On the next server start or `/chestshop reload` the hologram
  entity was re-spawned even though no shop existed.  
  **Fix:** Added `ShopHologramManager.cleanOrphanedShopHolograms()`.  It iterates all
  `shop_*` holograms, computes the expected hologram IDs from the currently loaded
  shop list, and removes any holograms whose shop no longer exists.  Called:
  - After `HologramManager.initialize()` on server start (both managers are fully
    loaded at that point).
  - At the end of `ShopManager.reload()` (triggered by `/chestshop reload`).

### ✅ Previously Fixed (Confirmation)
- **TPA message key typo** (`commands.neoessentials.teleport.request.recived`) was
  corrected to `received` and sender context was added in build 157.  Confirmed
  absent from the codebase; no action needed in this build.



### 🧹 Code Quality — Config Comment Migration (`//` / `/* */` style)

All NeoEssentials config files have been migrated from bloated JSON-key comments
(`xxx_comment`, `_doc_*`, `_step*`, `_how_*`, etc.) to proper `//` single-line and
`/* */` block comments.  The config parser was updated to support this format via
Gson's lenient `JsonReader`.

#### Problem

Every setting in every config file had a companion `key_comment: "..."` entry:

```json
"port": 8080,
"port_comment": "Port number for the web dashboard HTTP server (1024-65535)...",
"websocketPort": 8081,
"websocketPort_comment": "Port number for the WebSocket server...",
```

This doubled the file size, made it hard to find actual settings, and was **not** a
standard JSON practice — the comment keys were genuine data the parser had to skip.

#### Solution

**Before:**
```json
"port": 8080,
"port_comment": "Port number for the web dashboard HTTP server (1024-65535). Requires server restart to take effect",
```

**After:**
```json
// HTTP server port (1024–65535). Restart required after changing.
"port": 8080,
```

#### Technical details

| Change | Detail |
|--------|--------|
| `ConfigManager.parseJsonWithComments()` | New helper wrapping `JsonReader.setLenient(true)` — allows `//` and `/* */` in all config files |
| `stripLegacyCommentKeys(JsonObject)` | New recursive method that removes keys ending with `_comment`, ending with `-description`, or starting with `_` (except `_configVersion`) from user config files during version upgrade |
| Config version upgrades | On next server start, all existing user config files are automatically cleaned of old comment keys and have their version bumped |

#### Files rewritten

| Config file | Old version | New version | Key changes |
|------------|-------------|-------------|-------------|
| `config.json` | v21 | v22 | Removed ~150 `xxx_comment` and `xxx-description` keys |
| `discord_auth.json` | v7 | v8 | Removed `_step*`, `_how_*`, `_example`, `_role_*` keys |
| `economy.json` | v2 | v3 | Removed `_configVersion_comment` |
| `kits.json` | v1 | v2 | Removed `_configVersion_comment` |
| `permissions.json` | v6 | v7 | Removed `_configVersion_comment` |
| `tablist.json` | v4 | v5 | Removed `_doc_*` and `_comment*` array keys |
| `animations.json` | v1 | v2 | Removed `_doc` and `_comment*` inline keys |

**Files changed:** `ConfigManager.java`, all 7 config resource files

---

## [1.0.2.6+build.158] — 2026-05-25

### ✨ Feature — Named Animation System (`{animation:NAME}` Placeholder)

Adds a full text-animation engine to NeoEssentials.  Animations are defined in a new first-class config file (`animations.json`) and can be referenced anywhere placeholder processing is active via `{animation:NAME}`.

#### How it works

1. **Define** animations in `config/neoessentials/animations.json`:
   ```json
   {
     "animations": [
       {
         "name": "Rainbow",
         "frames": [
           "&cR&6a&eo&6b&cn&6w",
           "&6R&ca&eo&cb&cn&6w",
           "..."
         ],
         "frameDuration": 500
       }
     ]
   }
   ```
2. **Use** the placeholder anywhere that supports it:
   - Tablist header/footer: `&7Welcome {animation:Rainbow} to the server!`
   - Any future text path that calls `AnimationManager.getInstance().resolveAnimations(text)`

3. **Reload** with `/tablist reload` — reloads both `tablist.json` and `animations.json`.

#### Timing model

- `frameDuration` is in **milliseconds** (minimum 50 ms).
- `AnimationManager.tick()` is called on **every server tick** (before the tablist refresh guard), so frame transitions are accurate to within one server tick (~50 ms) regardless of `refreshInterval`.
- Drift-prevention logic aligns the next deadline to `lastFrameTime + frameDuration` rather than the moment the check runs.

#### Default animations bundled

| Name | Description | Duration |
|------|-------------|----------|
| `Rainbow` | Classic letter-colour cycling effect | 500 ms/frame |
| `PulseStar` | `★` cycling gold/red/yellow | 400 ms/frame |
| `StatusDot` | Blinking green/grey dot | 750 ms/frame |
| `LoadingDots` | Scrolling `Loading.` / `..` / `...` | 400 ms/frame |
| `GoldBanner` | Gradient-shift welcome banner | 600 ms/frame |
| `Spinner` | `| / — \` spinning pipe | 200 ms/frame |
| `HeartBeat` | Alternating `❤` colour pattern | 600 ms/frame |

#### New admin command

```
/tablist animations list   — lists all loaded animations with frame count and duration
```

#### New files

| File | Purpose |
|------|---------|
| `AnimationManager.java` | Singleton — loads animations, ticks frames, resolves `{animation:NAME}` |
| `animations.json` (resource) | Default animation definitions shipped with the mod |

#### Config changes

- `ConfigManager.ANIMATIONS_CONFIG = "animations.json"` constant added; registered as version-tracked config (v1).
- `tablist.json` `_configVersion` bumped `3 → 4` (doc-only; no user-value changes needed).
- `tablist.json` `_doc_header` updated to document `{animation:NAME}`.

**Files changed:** `AnimationManager.java` *(new)*, `animations.json` *(new resource)*, `TablistManager.java`, `TablistCommand.java`, `ConfigManager.java`, `tablist.json`

---

## [1.0.2.6+build.157] — 2026-05-25

### 🐛 Bug Fix — TPA Request: Wrong Argument Passed to Sender Confirmation Message

`TeleportRequestManager.sendTeleportRequest()` built a `typeText` string (`"to you"` / `"you to them"`) to describe the request direction, then passed it as argument `{1}` of both:

1. `commands.neoessentials.teleport.request.sent` → `"Teleport request sent to {0}. Expires in {1} second(s)."` — `{1}` expects the timeout in seconds, but received `"to you"`, producing the broken message **"Teleport request sent to Xtron. Expires in to you second(s)."**
2. `commands.neoessentials.teleport.request.received` → `"{0} wants {1}. Use /tpaccept or /tpdeny."` — `{1}` correctly expects a direction phrase, so this message was fine.

**Fix:**
- `sent` message now receives `requestTimeoutSeconds` as `{1}` (e.g. `"Teleport request sent to Xtron. Expires in 30 second(s)."`).
- `typeText` is now only passed to the `received` message (target-side), where it belongs.
- Corrected `typeText` from `"to you"` / `"you to them"` → `"to teleport to you"` / `"you to teleport to them"` to match the phrasing already used in `getPendingRequestInfo()`.

**Files changed:** `TeleportRequestManager.java`

---

## [1.0.2.6+build.156] — 2026-05-25

### 🧹 Code Quality — Warning Audit Pass (Part 3)

Continued IDE-warning audit: `Arrays.asList` → `List.of` / `Set.of` sweep, `.get(0)` → `.getFirst()` modernisation, and miscellaneous handler cleanup.

**`Arrays.asList` → `List.of` / `Set.of` replacements:**

- **`ConfigSplitter`** — `FILE_SECTIONS_MAP` entry for `main.json`: `Arrays.asList(...)` → `List.of(...)` (static final immutable list).
- **`ProxyIntegration`** — `knownServers.addAll(Arrays.asList(servers))` → `Collections.addAll(knownServers, servers)`.
- **`PermissionsCommand`** — All 15+ inline `java.util.Arrays.asList(...)` tab-completion lists → `java.util.List.of(...)`.
- **`FunCommands`** — Inline `Arrays.asList(...)` colour list → `List.of(...)`; removed now-unused `import java.util.Arrays`.
- **`ItemCustomisationCommands`** — Inline `Arrays.asList(...)` → `List.of(...)`.
- **`UtilityCommands`** — Two `Arrays.asList(...)` (static final + inline) → `List.of(...)`.
- **`ServerAdminCommands`** — Static final `Arrays.asList(...)` → `List.of(...)`.
- **`WorldInteractionCommands`** — Two `private static final` lists: `Arrays.asList(...)` → `List.of(...)`; removed `import java.util.Arrays`.
- **`DashboardFileManager`** — `private static final DASHBOARD_FILES`: `Arrays.asList(...)` → `List.of(...)`.
- **`AuthHandler`** — `roles.addAll(Arrays.asList(...))` → `Collections.addAll(roles, ...)`; removed `import java.util.Arrays`.
- **`CommandExecutionHandler`** — `new HashSet<>(Arrays.asList(...))` static final → `Set.of(...)`.
- **`FileManagementHandler`** — `ALLOWED_PATHS`: `Arrays.asList(...)` → `List.of(...)`; `EDITABLE_EXTENSIONS`: `new HashSet<>(Arrays.asList(...))` → `Set.of(...)`.
- **`AfkManager`** — `new HashSet<>(java.util.Arrays.asList(...))` → `new HashSet<>(java.util.List.of(...))`.

**`.get(0)` → `.getFirst()` modernisation (Java 21):**

- **`WarnManager`** — `.get(0)` → `.getFirst()`.
- **`JailCommand`** — `.get(0)` → `.getFirst()`.
- **`NpcShopCommand`** — `.get(0)` → `.getFirst()`.
- **`RealnameCommand`** — Two `.get(0)` → `.getFirst()`.
- **`DiscordPermissionSync`** — `.get(0)` → `.getFirst()`.
- **`ProxyIntegration`** — `.get(0)` → `.getFirst()` in `getAnyPlayer()`.
- **`TaskScheduler`** — `.get(0)` → `.getFirst()`.

**`ProxyIntegration` additional fixes:**

- Added `@SuppressWarnings("unused")` to `BUNGEE_CHANNEL`, `BUNGEE_CHANNEL_LEGACY` (public API constants).
- Added `//noinspection unused` + `@SuppressWarnings("unused")` to `onPluginMessage()` (registered externally).
- Renamed `player` param → `ignoredPlayer` in stub `sendBungeeMessage()`.
- Added `@SuppressWarnings("unused")` to `isShowNetworkPlayers()`.

**`CommandExecutionHandler` additional fixes:**

- Added `//noinspection unused` + `@SuppressWarnings("unused")` to class (handler instantiated via reflection/DI).
- Added `@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")` to `commandOutputs` (populated for future use).
- `commandHistory.remove(0)` → `commandHistory.removeFirst()`.

**`FileManagementHandler` fix:**

- Added `//noinspection resource` to `p.serverLevel()` call — `ServerLevel` lifecycle is managed by the Minecraft server; manually calling `close()` on it would be incorrect.

**Files changed:** `ConfigSplitter.java`, `ProxyIntegration.java`, `PermissionsCommand.java`, `FunCommands.java`, `ItemCustomisationCommands.java`, `UtilityCommands.java`, `ServerAdminCommands.java`, `WorldInteractionCommands.java`, `DashboardFileManager.java`, `AuthHandler.java`, `CommandExecutionHandler.java`, `FileManagementHandler.java`, `AfkManager.java`, `WarnManager.java`, `JailCommand.java`, `NpcShopCommand.java`, `RealnameCommand.java`, `DiscordPermissionSync.java`, `TaskScheduler.java`

---

## [1.0.2.6+build.155] — 2026-05-25

### 🧹 Code Quality — Warning Audit Pass (Part 2)

Continued IDE-warning audit across previously unreviewed packages: `inventory/`, `items/`, `integrations/`, `api/`, `core/`, `shop/` sub-packages, `vault/`, `webdashboard/`.

**Java fixes:**

- **`PermissionScanner`** — Replaced `Arrays.asList()` with `List.of()` for two static final `Pattern` lists (`PERMISSION_PATTERNS`, `DYNAMIC_PATTERNS`); removed now-unused `import java.util.Arrays`; removed always-true `if (sourcePath != null)` null-check (`Paths.get(URI)` never returns null); removed `throws IOException` from `scanJarFile()` signature (exception is fully caught inside the method, never propagates); fixed `peek()` optimization warning — replaced `stream.peek(this::scanClassFile).count()` with `.toList()` + `forEach()` (terminal `count()` may skip intermediate `peek()` in Java 21); renamed unused `source` parameter in `addDiscoveredPermission()` to `ignoredSource`; added `@SuppressWarnings("unused")` to `getFilePermissionMap()`, `generateDynamicPermissions()`, and `exportDiscoveredPermissions()`.
- **`ExternalPermissionProvider`** — Added `@SuppressWarnings("unused")` to `getPermissionsStartingWith()` and `exportForPermissionsEX()` (intentional public API surface).
- **`PermissionValidator`** — Removed unused `import java.util.stream.Collectors`.
- **`PermissionManager`** — Modernised inline `collect(java.util.stream.Collectors.toList())` → `.toList()`.
- **`ModerationManager` / `WarnManager`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` imports.
- **`BanCommand` / `FreezeCommand` / `JailCommand` / `VanishCommand`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` imports.
- **`ModRootCommand`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.
- **`DocumentationManager`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.
- **`EconomyLeaderboard`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.
- **`BaltopCommand`** — Removed unused `import java.util.stream.Collectors`.
- **`KitManager`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.
- **`ListKitsCommand`** — `collect(Collectors.toList())` → `.toList()`; `Collectors` import retained (`Collectors.toSet()` still used).
- **`ShopEntityManager`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.
- **`HelpCommand` / `ListCommand` / `RealnameCommand` / `ServerAdminCommands` / `UtilityCommands`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` imports.
- **`webdashboard/security/AuthenticationManager`** — `collect(Collectors.toList())` → `.toList()`; removed `Collectors` import.

**Files changed:** `PermissionScanner.java`, `ExternalPermissionProvider.java`, `PermissionValidator.java`, `PermissionManager.java`, `ModerationManager.java`, `WarnManager.java`, `BanCommand.java`, `FreezeCommand.java`, `JailCommand.java`, `VanishCommand.java`, `ModRootCommand.java`, `DocumentationManager.java`, `EconomyLeaderboard.java`, `BaltopCommand.java`, `KitManager.java`, `ListKitsCommand.java`, `ShopEntityManager.java`, `HelpCommand.java`, `ListCommand.java`, `RealnameCommand.java`, `ServerAdminCommands.java`, `UtilityCommands.java`, `AuthenticationManager.java` (security)

---

## [1.0.2.6+build.154] — 2026-05-20

### 🧹 Code Quality — Comprehensive Warning & Bug Fix Pass

Full IDE-warning audit across Java systems and web dashboard HTML files.

**Java fixes:**

- **`IgnoreManager`** — Removed always-false `IGNORE_FILE == null` null-check (static final field is never null); log `mkdirs()` failures instead of silently ignoring them; added `@SuppressWarnings("unused")` to intentional API method `getIgnoreList()` and renamed unused `player` param in `cleanupPlayer()` to `ignoredPlayer`.
- **`MuteManager`** — Same null-check removal and mkdirs logging as above; removed the dead `sender`-overload of `mute(ServerPlayer, String)` and `unmute(ServerPlayer, String)` (parameter was accepted but never read); added `@SuppressWarnings("unused")` to `getMuteExpiry()`.
- **`MuteCommand` / `UnmuteCommand`** — Updated callers to use the clean `mute(targetName)` / `unmute(targetName)` single-arg form.
- **`MessageUtil`** — Replaced `e.printStackTrace()` with `LOGGER.error(...)` in `loadCustomLanguageFile()`; merged the identical `FileNotFoundException` and `Exception` catch branches into one; fixed `File.delete()` result ignored in `deleteDirectoryRecursively()` (now logs a warning on failure); removed unused `FileNotFoundException` import; removed dead private `getLanguageVersion()` method; removed deprecated dead `escapeNamedPlaceholders()` method; annotated intentional-API public methods (`getDebugInfo`, `clickableSuggestion`, `balanceComponent`, `playerComponent`, `permissionComponent`, `progressBar`, `loadAllCustomLanguages`) with `@SuppressWarnings("unused")`.
- **`PlayerChatFormatManager`** — Log `mkdirs()` failure in `save()`; annotate `hasFormat()` with `@SuppressWarnings("unused")`.
- **`ShopManager`** — `collect(Collectors.toList())` → `.toList()` (Java 21); removed now-unused `Collectors` import; annotated `removeShopsByOwner()` with `@SuppressWarnings("unused")`.
- **`PlayerJoinQuitHandler`** — Log `mkdirs()` failure; removed always-true `if (config != null)` dead-code guard (config already used successfully at line 57); guarded both `player.getServer().getPlayerList()` calls with a null-check to prevent theoretical NPE at join/quit time.
- **`LocalizationManager`** — Wrapped `Files.list(langDirectory)` in a `try`-with-resources to prevent a stream resource leak in `loadDashboardTranslations()`; added `@SuppressWarnings("unused")` to `translate(key, language, args)` and `getAllTranslations()`; added `isLanguageUnsupported()` convenience inverse so all `!isLanguageSupported()` call sites can use the positive form.
- **`TranslationHandler`** — Updated two `!isLanguageSupported()` calls to use the new `isLanguageUnsupported()`.
- **`TaskManager`** — `history.add(0, execution)` → `history.addFirst()`; `history.remove(history.size()-1)` → `history.removeLast()`; simplified `if (currentTime < start || currentTime > end) return false; return true;` block to a single `return currentTime >= start && currentTime <= end`.
- **`BanManager`** — Replaced `Optional.get()` (flagged as potential NPE) with `Optional.orElse(null)` for the profile-cache lookup; removed always-true defensive null checks on `entry.getReason()` / `entry.getSource()`.

**Web dashboard accessibility fixes (HTML `for`/`aria-label`):**

- **`index.html`** — Changed `href="#players"` / `href="#performance"` / `href="#worlds"` / `href="#events"` to `href="#"` (SPA navigation handled by `data-page`; avoids unresolvable anchor warnings); added `aria-label` to `#broadcastInput`.
- **`shop.html`** — Added `aria-label` to `#filterInput` and `#typeFilter`.
- **`permissions.html`** — Added `aria-label` to `#userSearchInput`.
- **`kits.html`** — Added `aria-label` to `#kitSearch`.
- **`moderation.html`** — Added `aria-label` to `#warnSearch`; added `for` attributes to all ban-form labels (`banTarget`, `banName`, `banReason`, `banType`, `banDuration`).
- **`users.html`** — Added `for` to create-user form labels (`newUsername`, `newPassword`, `newEmail`, `newRole`); added `aria-label` to modal `#roleSelect` and `#pwInput`.
- **`cloud.html`** — Added `for` to all Dropbox and Google Drive config labels (`dbxToken`, `dbxPath`, `gdClientId`, `gdClientSecret`, `gdRefreshToken`, `gdFolderId`).
- **`discord.html`** — Added `for` to OAuth2 config labels (`cfgDefaultRole`, `cfgClientId`, `cfgClientSecret`, `cfgRedirectUri`).
- **`holograms.html`** — Added `aria-label` to `#holoSearch`; added `for` to all Create-modal and Edit-modal labels (position X/Y/Z, refresh, scale, billboard, line spacing, opacity, background, spin speed/axis, hover amplitude/speed).

**Files changed:** `IgnoreManager.java`, `MuteManager.java`, `MuteCommand.java`, `UnmuteCommand.java`, `MessageUtil.java`, `PlayerChatFormatManager.java`, `ShopManager.java`, `PlayerJoinQuitHandler.java`, `LocalizationManager.java`, `TranslationHandler.java`, `TaskManager.java`, `BanManager.java`, `index.html`, `shop.html`, `permissions.html`, `kits.html`, `moderation.html`, `users.html`, `cloud.html`, `discord.html`, `holograms.html`

---

## [1.0.2.6+build.153] — 2026-05-19

### 🐛 Bug Fix — `ResourcePackManager`: `Thread.sleep(1000)` on Server Main Thread

`onPlayerJoin()` used `server.execute(() -> { Thread.sleep(1000); sendResourcePack(player); })`, which blocks the **Minecraft server tick thread** for 1 second on every player login — causing rubber-banding, no block updates, and `Can't keep up!` warnings. Same root cause as the previously fixed `NeoEssentials.java` admin-notify sleep (build.150).

**Fix:** Sleep moved to a daemon background thread (`NeoEssentials-ResourcePackDelay`). Once the delay completes, the actual packet send is marshalled back to the server tick thread via `server.execute()`.

**Files changed:** `ResourcePackManager.java`

---

## [1.0.2.6+build.152] — 2026-05-19

### 🐛 Bug Fix — `ShopManager` & `PlayerChatFormatManager`: Runtime Data Written to Config Directory

Two managers stored player-generated runtime data in `config/neoessentials/` (via `ResourceUtil.getConfigPath()` / `getConfigFile()`), which is designated as read-only server configuration. On typical Minecraft hosting setups the `config/` directory is excluded from world backups and included in mod config resets — meaning a config wipe could silently delete all player shops and per-player chat format assignments.

- **`ShopManager`** — `shops.json` contains player-created shops with owner UUIDs and block positions (runtime data, not config).
- **`PlayerChatFormatManager`** — Per-player chat format overrides keyed by UUID (admin-assigned runtime data, not config).

**Fix:** Both changed to use `ResourceUtil.getDataPath()` / `getDataFile()` to store data under `neoessentials/` (data dir).

**Files changed:** `ShopManager.java`, `PlayerChatFormatManager.java`

---

## [1.0.2.6+build.151] — 2026-05-19

### 🐛 Bug Fix — i18n/Language System: Raw Paths in `LocalizationManager`, `LanguageCommand`, `MessageUtil`

Three files used hardcoded raw `Paths.get("neoessentials", ...)` / `new File("neoessentials/...")` instead of `ResourceUtil`: `LocalizationManager` (webdashboard lang directory), `LanguageCommand` (template/missing-key export paths), and `MessageUtil` (custom language file loading). On hosts where the JVM working directory differs from the server root all language files would be silently mislocated.

**Fix:** All raw paths replaced with `ResourceUtil.getDataPath()` / `ResourceUtil.getDataFile()`. Removed now-unused `Paths` imports.

**Files changed:** `LocalizationManager.java`, `LanguageCommand.java`, `MessageUtil.java`

---

## [1.0.2.6+build.150] — 2026-05-19

### 🐛 Bug Fix — TaskManager: Scheduler Paths Used Raw `Paths.get()` Instead of `ResourceUtil`

`TASKS_DIR`, `TASKS_FILE`, and `HISTORY_FILE` were hardcoded via `Paths.get("neoessentials", "scheduler")` / `.resolve(...)`. Every other data-file path in the mod uses `ResourceUtil.getDataPath()`; the scheduler was the sole inconsistency.

**Fix:** Replaced all three constants with `ResourceUtil.getDataPath(...)` calls. Removed now-unused `Paths` import.

**Files changed:** `TaskManager.java`

---

## [1.0.2.6+build.149] — 2026-05-19

### 🐛 Bug Fix — EconomyManager: `lastActivityFile` Used Raw Relative Path

`lastActivityFile` was declared with `new File("neoessentials/balances_activity.json")` while its companion `balancesFile` correctly used `ResourceUtil.getDataFile()`. On hosting environments where the JVM working directory differs from the server root, `balances_activity.json` was created and read from a mismatched location, causing the inactive-account cleanup scheduler to silently see every account as "never active" and purge balances incorrectly.

**Fix:** Changed to `ResourceUtil.getDataFile("balances_activity.json")` to stay consistent with all other data files.

**Files changed:** `EconomyManager.java`

---

### 🐛 Bug Fix — TeleportRequestManager: Dead `sendTpaRequest()` Method with Missing Safety Checks

`sendTpaRequest()` was an unreachable duplicate of `sendTeleportRequest()` — no command or code path called it. It omitted three safety checks present in the real method: **request cooldown**, **`allowMultipleRequests`** guard, and **tptoggle** state. Had it ever been invoked, all three protections would have been bypassed silently.

**Fix:** Removed the method entirely along with five now-unused imports it had introduced.

**Files changed:** `TeleportRequestManager.java`

---

## [1.0.2.6+build.148] — 2026-05-19

### 🐛 Bug Fix — BanManager: Expired Temp IP Bans Not Cleaned Up by Scheduler

The periodic ban-cleanup scheduler was only sweeping player bans, leaving expired temporary IP bans alive in memory and on disk indefinitely until a manual `/unban ip` was issued.

**Root cause:** `cleanupExpiredTempBans()` iterated only `playerBans` and had no equivalent loop for `ipBans`.

**Effects before fix:**
- Expired temp IP bans accumulated in memory on busy servers (slow leak)
- `/ipbanlist` continued to show expired entries
- `saveIPBans()` kept writing expired entries back to `ip_bans.json` on unrelated saves

**Fix:** Added a second iterator loop inside `cleanupExpiredTempBans()` that sweeps `ipBans`, respects the `autoExpireTempBans` config flag, calls `saveIPBans()` if any entries were removed, and logs the cleanup separately from player-ban cleanup.

**Files changed:** `BanManager.java`

---

### 🐛 Bug Fix — BanManager: `isIPBanned()` Never Checked Expiry

`isIPBanned()` was returning `containsKey(ipAddress)` — always `true` for any stored IP ban entry, including ones that had already expired — meaning expired temp IP bans effectively acted as permanent bans until the scheduler ran or the server restarted.

**Fix:** `isIPBanned()` now calls `IPBanEntry.isExpired()`. If the ban is expired and `autoExpireTempBans` is enabled, the entry is immediately removed and `saveIPBans()` is called.

**Files changed:** `BanManager.java`

---

## [1.0.2.6+build.147] — 2026-05-18

### 🎨 Improvement — Hologram Icon Visual Consistency

Hologram-related navigation icons updated across all admin dashboard pages for visual consistency with the rest of the icon set. No behaviour changes.

**Files changed:** `admin.html`, `backup.html`, `cloud.html`, `discord.html`, `holograms.html`, `index.html`, `kits.html`, `moderation.html`, `permissions.html`, `shop.html`, `stats.html`, `teleport.html`, `users.html`

---

## [1.0.2.6+build.135] — 2026-05-18

### 🔧 Improvement — Admin Dashboard Navigation & UI Consistency

Navigation and layout cleanup pass across all dashboard pages.

- Navigation sidebar links that should only be visible to server operators are now hidden for read-only and moderator sessions
- Page-level active-link highlighting correctly reflects the current page on all sub-pages
- `dashboard.js` — authentication guard and session-check logic simplified; removed dead code paths
- `styles.css` — minor responsive-layout fixes for narrow-width sidebar
- All sub-pages (`admin.html`, `backup.html`, `cloud.html`, `discord.html`, `holograms.html`, `kits.html`, `moderation.html`, `permissions.html`, `shop.html`, `stats.html`, `teleport.html`, `users.html`) updated with consistent navigation and footer

**Files changed:** `admin.html`, `backup.html`, `cloud.html`, `dashboard.js`, `discord.html`, `holograms.html`, `index.html`, `moderation.html`, `permissions.html`, `styles.css`, `teleport.html`, `users.html`

---

## [1.0.2.6+build.133] — 2026-05-18

### ✨ Feature — WebSocket Real-Time Dashboard Updates

The web dashboard now supports live server-state streaming via WebSocket so data refreshes automatically without manual page reloads.

**Server-side:**
- `WebSocketEventBroadcaster` — new singleton that broadcasts typed JSON events to all connected dashboard sessions: `player_join`, `player_leave`, `player_update`, `server_stats`, `chat_message`, `shop_transaction`, and `hologram_update`
- `AdminEndpoint` — new `POST /api/admin/broadcast` REST endpoint for sending in-game-admin announcements from the dashboard
- `PlayerEndpoint` — extended with ban, kick, and mute actions callable from the dashboard player-list
- `ServerDataCollector` — added TPS and memory fields to the polling loop

**Client-side (`dashboard.js`):**
- WebSocket handshake with automatic reconnection and exponential back-off
- Live stat cards that update on server push events
- Real-time online-player list: avatars appear/disappear as players join/leave
- `index.html` updated with live-update indicators and connection status badge

**New Shop Management dashboard page:**
- `shop.html` / `shop.js` — full CRUD UI for sign-shops: view all shops, edit prices, view transaction history, enable/disable shop holograms

**Files changed:** `WebSocketEventBroadcaster.java`, `AdminEndpoint.java`, `PlayerEndpoint.java`, `ServerDataCollector.java`, `StatsEndpoint.java`, `DashboardAPI.java`, `dashboard.js`, `index.html`, `shop.html`, `shop.js`, `styles.css`

---

## [1.0.2.6+build.131] — 2026-05-18

### ✨ Feature — Shop Hologram Management Commands

Admins can now control the hologram floating above sign-shops without touching the hologram system directly.

**New `/shop hologram` subcommands:**

| Command | Effect |
|---|---|
| `/shop hologram enable <shopId>` | Spawn or re-enable the shop hologram |
| `/shop hologram disable <shopId>` | Hide the hologram without deleting the shop |
| `/shop hologram move <shopId>` | Snap the hologram to the sign's current position |

The hologram is automatically created when a shop is created (if holograms are enabled in config) and destroyed when the shop is removed.

**Other improvements:**
- `ShopInteractHandler` — revised event priority; fixed edge cases where right-clicking the hologram did not trigger a purchase
- `ShopSignHandler` — hologram created/destroyed in sync with sign placement/break
- `ShopData` — new `hologramEnabled` and `hologramId` fields persisted in `shops.json`

**Files changed:** `HologramData.java`, `HologramRenderer.java`, `ShopHologramManager.java`, `ShopManager.java`, `ShopCommand.java`, `ShopInteractHandler.java`, `ShopSignHandler.java`, `ShopData.java`

---

## [1.0.2.6+build.129] — 2026-05-18

### 🎨 Improvement — Hologram & Discord Dashboard UI Rework

Major visual overhaul of the hologram management page and Discord integration page in the web dashboard, plus hologram renderer fixes.

**Hologram dashboard (`holograms.html` / `holograms.js`):**
- Complete page redesign with a responsive card-based layout
- Live preview panel showing scale, opacity, and background colour before saving
- Inline editors for all visual properties: `scale`, `lineSpacing`, `textShadow`, `textOpacity`, `backgroundColorArgb`
- Frame animation editor: add, reorder, and remove frames per line
- Billboard / spin / hover controls now exposed in the UI
- Improved command reference sidebar

**Hologram renderer (`HologramRenderer.java`):**
- Background panel colour now applied correctly to the `TextDisplay` entity
- Scale and line-spacing applied at spawn, eliminating a recalculation tick delay
- Reduced flicker on high-frequency animation frames

**Hologram command (`HologramCommand.java`):**
- Added per-hologram `debug` subcommand for in-game entity diagnostics
- Improved error messages for invalid colour/opacity values

**Discord dashboard (`discord.html`):**
- Redesigned account-link table with live status badges
- One-click unlink with confirmation modal

**Files changed:** `HologramData.java`, `HologramEventHandler.java`, `HologramManager.java`, `HologramRenderer.java`, `HologramCommand.java`, `DiscordEndpoint.java`, `DashboardRegisterCommand.java`, `discord.html`, `holograms.html`, `holograms.js`, `login.html`

---

## [1.0.2.6+build.127] — 2026-05-13

### ✨ Feature — In-Game Discord OAuth2 Registration Flow

Complete in-game Discord OAuth2 integration.

**`/dashboard register discord`** opens a full OAuth2 authorization flow via the dashboard's `/discord/auth` endpoint — no manual token copy-paste.

**New `DiscordEndpoint` REST routes:**

| Route | Purpose |
|---|---|
| `GET /discord/auth/start` | Generate a Discord OAuth2 authorization URL |
| `GET /discord/auth/callback` | Handle OAuth2 redirect, link the account, and redirect to the dashboard |
| `POST /discord/link` | Link a Minecraft player to a Discord account |
| `DELETE /discord/unlink` | Unlink a player's Discord account |
| `GET /discord/linked` | List all currently linked player↔Discord pairs |

**New web dashboard Discord page** (`discord.html`) — view linked accounts, initiate link/unlink from the browser, and manage Discord integration settings.

`discord_auth.json` configuration format updated with new OAuth2 fields.

**Files changed:** `DashboardRegisterCommand.java`, `AuthenticationHandler.java`, `DiscordEndpoint.java`, `discord.html`, `discord_auth.json`

---

## [1.0.2.6+build.125] — 2026-05-13

### ✨ Feature — Discord-Linked Dashboard Registration

Admins can now link their Minecraft account to Discord and use that link to register dashboard access.

- **`/dashboard register discord`** — starts a Discord OAuth2 link flow; sends the player a clickable link to authorize their Discord account through the web dashboard OAuth callback
- `DashboardRegistrationManager` — new server-side component that creates and expires one-time Discord registration tokens, associates the Discord user ID with the Minecraft UUID, and cleans up after successful registration

**Files changed:** `DashboardRegisterCommand.java`, `DashboardRegistrationManager.java`

---

## [1.0.2.6+build.124] — 2026-05-13

### ✨ Feature — `/dashboard update` Smart File Comparison

The `/dashboard update` command now performs **per-file MD5 checksum comparison** instead of unconditionally overwriting all dashboard files.

| Sub-command | Behaviour |
|---|---|
| `/dashboard update` | Smart update: only overwrites files whose content differs from the bundled JAR version. Reports added / updated / already-up-to-date file counts. |
| `/dashboard update check` | Dry-run preview: shows exactly which files would change without writing anything. |
| `/dashboard update force` | Bypass checksums and replace every file unconditionally (previous behaviour). |

**`/dashboard status`** now shows the installed dashboard version vs. the current build number, with an upgrade hint when a newer version is bundled.

**Files changed:** `DashboardFileManager.java`, `DashboardCommand.java`

---

## [1.0.2.6+build.122] — 2026-05-11

### 🐛 Bug Fix — LuckPerms Default Permissions Not Applied When Adapter Is Unhealthy

**Two root causes found and fixed in `PermissionAPI.hasPermission()`:**

| # | Root cause | Fix |
|---|---|---|
| 1 | **Registry-default fallback gated behind `externalAvailable`** — When `LuckPermsAdapter` accumulated ≥ 5 consecutive failures (e.g. during startup before user data was fully cached), `isHealthy()` returned `false`, `externalAvailable = false`, and the entire registry-default block was skipped. Non-OP players in the LuckPerms default group silently lost all NeoEssentials documented default permissions with no visible error. | Removed the `if (externalAvailable)` guard from the registry-default block. Registry defaults are now always evaluated before the vanilla-OP fallback. |
| 2 | **`queryTristate` called twice per check, doubling failure count** — `hasPermission()` called `queryTristate` once, then `checkRegistryDefault()` called `isExplicitlyDenied()` which called `queryTristate` a second time. Every cache-miss or timeout incremented `consecutiveFailures` **twice**, flipping the adapter to unhealthy at half the expected number of checks and immediately triggering root cause 1. | Cached the `isExplicitlyDenied()` result immediately after the first `hasPermission()` call; new helper `checkRegistryDefaultNoAdapterCall()` reads the cached value without triggering a second adapter call. When the adapter is unhealthy (`explicitDeny == null`), the value is treated as "not denied" so registry defaults apply even when LuckPerms is temporarily unavailable. |

**Files changed:** `PermissionAPI.java`

---

### 🐛 Bug Fix — AFK Kick Not Respecting Exempt Permission

AFK kicks were firing on players who held `neoessentials.afk.kickexempt` because the permission was not checked before executing the kick. The kick now verifies the player does **not** hold the exempt node before ejecting them.

**Additional fixes in this build:**
- `AfkManager` now reads AFK config values (kick threshold, warning message, warning advance time) correctly from `config.json` via `ConfigManager`
- `MiscTeleportManager` config-loading path hardened against missing keys
- `MessageUtil.applyPlaceholders()` improved resolution order and null-safety

**Files changed:** `AfkManager.java`, `MiscTeleportManager.java`, `MessageUtil.java`, `ConfigManager.java`

---

## [1.0.2.6+build.123] — 2026-05-13

### ✨ Feature — Hologram System Improvements

#### New visual properties (persisted, applied on spawn)
| Property | Type | Default | Description |
|---|---|---|---|
| `scale` | float | `1.0` | Uniform text scale (0.1–10.0) |
| `lineSpacing` | float | `0.3` | Vertical gap between lines in blocks |
| `textShadow` | bool | `false` | Minecraft text drop-shadow |
| `textOpacity` | int | `255` | Text opacity (0–255) |
| `backgroundColorArgb` | int | `0x00000000` | Background panel colour (ARGB) |

#### New commands
- `/hologram copy <id> <newid>` — deep-clone a hologram (all lines, animations, visual settings)
- `/hologram movehere <id>` — teleport hologram to caller's current standing position
- `/hologram near [radius]` — list all holograms within `radius` blocks (default 20), sorted by distance
- `/hologram insertline <id> <index> <text>` — insert a line at any position (0 = top)
- `/hologram addframes <id> <lineIndex> <intervalTicks> <frame1|frame2|...>` — configure frame animation on any line
- `/hologram removeframes <id> <lineIndex>` — revert a line to static text
- `/hologram scale <id> <scale>` — set scale
- `/hologram linespacing <id> <spacing>` — set line spacing
- `/hologram shadow <id> on|off` — toggle text shadow
- `/hologram opacity <id> <0-255>` — set text opacity 
- `/hologram background <id> <transparent|#RRGGBB|#AARRGGBB>` — set panel background

#### Dashboard API
All hologram JSON responses now include the full set of visual and animation fields: `billboardMode`, `spinEnabled`, `spinSpeedDegrees`, `spinAxis`, `hoverEnabled`, `hoverAmplitude`, `hoverSpeedDegrees`, `scale`, `lineSpacing`, `textShadow`, `textOpacity`, `backgroundColorArgb` — previously these were missing.

#### Documentation
New `docs/Wiki/HologramSystem.md` covering all commands, placeholder support, colour codes, JSON schema, and web dashboard API.

**Files changed:** `HologramData.java`, `HologramRenderer.java`, `HologramCommand.java`, `HologramEndpoint.java`, `docs/Wiki/HologramSystem.md`

---

## [1.0.2.6+build.120] — 2026-05-11

### Bug Fix — `/help 2` Pagination Broken by Vanilla Brigadier Node Shadowing

**Reported behaviour:**
- `/help` showed page 1 correctly.
- `/help 2` was parsed by vanilla help's command-lookup argument and returned unknown/no-permission style output instead of page 2.

#### Root Cause

NeoEssentials and vanilla both register `/help` as Brigadier root literals. In this runtime, vanilla's `/help <command:string>` child shadowed NeoEssentials' pagination argument path, so numeric input (`2`) was consumed by vanilla before NeoEssentials logic executed.

#### Fix

`HelpCommand.register()` now removes existing root literals for `help` and `?` from the dispatcher tree before registering NeoEssentials help nodes. This guarantees `/help [page|command]` and `/? [page|command]` are resolved by NeoEssentials and pagination works as intended.

#### Files Changed

| File | Change |
|---|---|
| `HelpCommand.java` | Added dispatcher root-node replacement for `help` and `?` (reflection-based map removal in Brigadier `CommandNode`) before command registration. |

---

## [1.0.2.6+build.119] — 2026-05-08

### Bug Fix — `/tpa` Lands on Nether Roof or Inside Underwater Caves

**Reported behaviour:**
- Teleporting to a player in the **Nether** sometimes placed the arriving player on top of the bedrock roof (Y≥128).
- Teleporting to a player who was in an **ocean or on a boat** sometimes placed the arriving player inside a submerged cave beneath the seafloor.

#### Root Causes

| # | Root cause | Fix |
|---|---|---|
| 1 | **Nether roof landing** — `TeleportLocation.scanColumnTopDown()` started its scan from `level.getMaxBuildHeight() - 2` (Y=254 in the Nether). Because Y=128–254 is the empty void above the bedrock ceiling, the very first air block found was Y=128 — one block above solid bedrock. The scan happily returned that as "safe". | Cap the starting Y to `level.dimensionType().logicalHeight() - 1` (Y=127 in the Nether). The scan now stays inside the cave space and never touches the roof. |
| 2 | **Underwater cave landing** — When a destination player stood in water (or on a boat), `isSafe()` returned `false` (water has an empty collision shape). This triggered `findSafeLocation()`. The top-down scan skipped all ocean water columns (no solid dry ground) and found the first dry cave *below* the ocean floor as the "safe" spot. | For `/tpa` specifically, pass `findSafe=false`. Teleporting to a live player's exact position is EssentialsX-standard behaviour — if they are alive there, the spot is acceptable. Additionally, `findSafeLocation()` now first tries a ±16 Y neighbourhood around the destination before triggering the full top-down scan, so home/spawn/warp teleports also prefer nearby spots over distant ceilings or caves. |

#### Files Changed

| File | Change |
|---|---|
| `TeleportLocation.java` | `scanColumnTopDown()` caps start Y to `dimensionType().logicalHeight() - 1`; `findSafeLocation()` tries ±16 Y neighbourhood before falling back to top-down scan. |
| `TeleportRequestManager.java` | `executeTeleportRequest()` passes `findSafe=false` when teleporting to a player's live position. |

---

## [1.0.2.6+build.115] — 2026-05-08

### Feature — Interactive Shop Holograms (Click to Buy / Sell)

Players can now interact directly with the hologram floating above a sign-shop, without needing to click the sign itself.

| Action | Result |
|---|---|
| **Right-click** the shop hologram | Buy items (identical to right-clicking the sign) |
| **Left-click** the shop hologram | Sell items (identical to left-clicking the sign) |
| **Owner** right- or left-clicks their own shop hologram | Shows shop info panel |

`Display.TextDisplay` entities are invulnerable, so left-clicking produces no swing animation or damage — it acts purely as a click trigger. All existing permission checks, transaction logic, stock limits, and error messages are reused from the sign handler.

#### Files Changed

| File | Change |
|---|---|
| `ShopHologramManager.java` | Added `onEntityInteract` (`PlayerInteractEvent.EntityInteract`) for right-click buy; added `onEntityAttack` (`AttackEntityEvent`) for left-click sell; added `shopFromHologramEntity()` helper that resolves sign block from hologram NBT tag; added `sendShopInfo()` and `sendTransactionResult()` helpers. |

---

## [1.0.2.6+build.113] — 2026-05-08

### Feature — Hologram Billboard, Spin & Hover Animations

Holograms now support player-facing billboard rotation and smooth animation effects, configurable per-hologram via commands.

#### Billboard Mode (Player-Facing Rotation)

`Display.TextDisplay` entities support a built-in `BillboardConstraints` flag that the game client resolves per-viewer. Setting `CENTER` makes the hologram always face each player's camera client-side with zero server overhead. This replaces the previous `FIXED` (non-rotating) default.

| Mode | Behaviour |
|---|---|
| `CENTER` *(new default)* | Always faces the viewing player's camera |
| `VERTICAL` | Rotates only on the Y axis to face the player |
| `HORIZONTAL` | Rotates only on the horizontal plane |
| `FIXED` | No rotation (original behaviour) |

#### Spin Animation

Applies a quaternion rotation to the `DATA_LEFT_ROTATION` slot of each `Display` entity every tick. Uses 1-tick transformation interpolation for smooth motion. Configurable speed (`°/tick`) and axis (X / Y / Z).

#### Hover Animation

Applies a sine-wave vertical offset to each entity's Y position every tick, producing a smooth floating bob effect. Configurable amplitude (blocks) and speed (`°/tick`).

#### New Fields Added to `HologramData`

| Field | Default | Description |
|---|---|---|
| `billboardMode` | `3` (CENTER) | 0=FIXED, 1=VERTICAL, 2=HORIZONTAL, 3=CENTER |
| `spinEnabled` | `false` | Enable Y/Z/X axis spin |
| `spinSpeedDegrees` | `3.0` | Degrees advanced per animation tick |
| `spinAxis` | `"Y"` | Axis to rotate around |
| `hoverEnabled` | `false` | Enable up/down bob effect |
| `hoverAmplitude` | `0.08` | Peak displacement in blocks |
| `hoverSpeedDegrees` | `1.5` | Speed of sine-wave bob |

#### New Commands

```
/hologram billboard <id> <fixed|vertical|horizontal|center>
/hologram spin <id> on [speed] [axis]
/hologram spin <id> off
/hologram hover <id> on [amplitude] [speed]
/hologram hover <id> off
```

`/hologram info <id>` now displays all billboard, spin, and hover settings.

#### Files Changed

| File | Change |
|---|---|
| `HologramData.java` | Added `billboardMode`, `spinEnabled`, `spinSpeedDegrees`, `spinAxis`, `hoverEnabled`, `hoverAmplitude`, `hoverSpeedDegrees`, transient `currentSpinAngle`, transient `hoverPhase`. |
| `HologramRenderer.java` | Added `DATA_BILLBOARD_CONSTRAINTS_ID`, `DATA_LEFT_ROTATION_ID`, `DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID` via reflection; `spawn()` now applies billboard mode; added `updateRotationsAndPositions()` called by scheduler. |
| `HologramScheduler.java` | Animation tick (every 50 ms) advances spin angle and hover phase, then calls `updateRotationsAndPositions()`. |
| `HologramCommand.java` | Added `billboard`, `spin`, `hover` subcommands; `info` shows all new fields. |

---

## [1.0.2.6+build.112] — 2026-05-04

### Bug Fix — `/back` Returns "No Previous Location" After Death

**Reported behaviour:** After dying, `/back` always showed *"No previous location to return to"*,
even though the player had just died in a known location.

#### Root Causes

| # | Root cause | Fix |
|---|---|---|
| 1 | **Missing explicit `bus` on `@EventBusSubscriber`** — `MiscTeleportManager` used `@EventBusSubscriber(modid = "neoessentials")` without specifying `bus = Bus.GAME`. Other classes in the project (including the inner `GameEvents` class in `NeoEssentials.java`) always pass the bus explicitly. NeoForge defaults to `Bus.GAME`, but being explicit avoids any future ambiguity and matches the project-wide pattern. | Added `bus = EventBusSubscriber.Bus.GAME` to the annotation. |
| 2 | **`receiveCanceled = false` (default) on the death handler** — If another mod or mechanic (e.g. keep-inventory, protection plugins) cancelled `LivingDeathEvent` before our NORMAL-priority handler ran, the handler was silently skipped. The player DID die (the event being cancelled merely prevents loot-drop / spawnpoint reset in some mods), but `saveDeathLocation` was never called. | Changed `@SubscribeEvent` to `@SubscribeEvent(receiveCanceled = true)` so the handler always fires when the dying entity is a `ServerPlayer`, regardless of cancellation. |
| 3 | **`PlayerDataStore.flush` silently fails when directory doesn't exist** — On a fresh install the `neoessentials/playerdata/back_locations/` directory is created in the `PlayerDataStore` constructor, but only when `getInstance()` is first called. If a race condition or unexpected class-load order caused the constructor to run before the data directory was available, `FileWriter` would throw and leave in-memory data un-persisted. After a server restart `/back` would return "no history". | Added an explicit `dataDirectory.mkdirs()` guard inside `flush()` before writing the temp file; failure is now logged at ERROR level. |
| 4 | **Missing `backSettings` section in default `config.json`** — Values like `enableDeathBack`, `enableTeleportBack`, `teleportDelay`, and `backCooldown` had no explicit defaults in the bundled config. They relied on Java field defaults and ConfigManager fallbacks, making it impossible for server admins to knowingly adjust them. | Added `teleportation.backSettings` section to the bundled `config.json` with all four keys explicitly documented. |

#### Diagnostic Logging Added

`onPlayerDeathEvent` now logs an **INFO** message every time a player dies, including coordinates
and whether the event was cancelled. Check your server log for lines like:

```
[MiscTeleportManager] Death event fired for Steve at (128.50, 64.00, -256.30) in minecraft:overworld — cancelled=false
[MiscTeleportManager] Saved death location for Steve at (128.50, 64.00, -256.30)
```

If these lines are absent after a player death, an external mod is preventing `LivingDeathEvent`
from reaching NeoEssentials. If the first line appears but not the second, check the
`enableDeathBack` flag in `config.json → teleportation.backSettings`.

#### Files Changed

| File | Change |
|---|---|
| `MiscTeleportManager.java` | Added `bus = Bus.GAME` to annotation; `@SubscribeEvent(receiveCanceled = true)`; INFO log in `onPlayerDeathEvent`; `loadConfig` reads `backCooldown` from `backSettings` (falls back to legacy `miscSettings`). |
| `SpawnOnDeathHandler.java` | Added `bus = Bus.GAME` to annotation (consistency fix). |
| `PlayerDataStore.java` | `flush()` now calls `dataDirectory.mkdirs()` before writing and logs ERROR if it fails. |
| `config.json` (bundled) | Added `teleportation.backSettings` with `enableDeathBack`, `enableTeleportBack`, `teleportDelay`, `backCooldown`. |

---



### Bug Fix — Gson HTML Character Escaping in Config and Data Files

All `GsonBuilder` instances that write JSON files to disk now use `.disableHtmlEscaping()`.
Previously, Gson's default behaviour silently converted `<`, `>`, and `&` to Unicode escapes
(`\u003c`, `\u003e`, `\u0026`), corrupting any saved value that contained those characters —
most notably chat format strings such as `<{prefix} {name}> {MESSAGE}`.

#### Affected Files (30+)

| Category | Files |
|---|---|
| Config | `ConfigSplitter.java`, `ConfigManager.java` |
| Chat | `PlayerChatFormatManager.java`, `AfkManager.java` |
| Player data | `PlayerDataStore.java`, `NickCommand.java`, `MailCommand.java`, `SeenCommand.java` |
| Moderation | `BanManager.java`, `JailManager.java`, `FreezeManager.java`, `WarnManager.java`, `VanishManager.java`, `ModerationManager.java`, `ModerationHandler.java` |
| Teleportation | `WarpManager.java`, `SpawnManager.java` |
| Economy | `WorthManager.java` |
| Kits | `KitManager.java` |
| Language / i18n | `MessageUtil.java`, `CustomLanguageManager.java` |
| MOTD / Rules | `MotdManager.java`, `RulesCommand.java` |
| Scheduler | `TaskManager.java`, `TaskHandler.java` |
| Resource packs | `ResourcePackGenerator.java`, `ResourcePackHandler.java`, `ResourcePackManager.java` |
| Web dashboard | `DiscordAuthConfig.java`, `DashboardRegistrationManager.java`, `AuthenticationManager.java`, `AuthenticationHandler.java`, `PlayerDataHandler.java`, `FileManagementHandler.java`, `CommandExecutionHandler.java`, `MotdEndpoint.java`, `PermissionEndpoint.java`, `RulesEndpoint.java`, `StatsEndpoint.java`, `CloudStorageManager.java`, `CloudProviderManager.java`, `BackupManager.java` |
| Core | `NeoEssentialsManager.java`, `DocumentationManager.java`, `DocumentationHandler.java` |

---

## [1.0.2.6+build.107] — 2026-05-04

### Bug Fix — Chat Config File Misread (`chat.json` not loading)

**Reported error:**
```
Failed to read config file chat: config/neoessentials/chat (No such file or directory)
```

**Root cause (build.69):** `getConfig("chat")` tried to open a file literally named `chat`
(no `.json` extension) because the section-extraction guard `!configName.endsWith(".json")`
didn't exist yet. After that guard was added, a secondary scenario remained: if the MAIN_CONFIG
cache was populated before split configs were activated (e.g. after running `/neoe config split`
without a subsequent `/neoe reload`), the merged view lacked the `"chat"` section, causing
the fallthrough to return an empty `JsonObject` and ChatManager to log
`No chat-format in config, using default`.

#### Fixes

| File | Change |
|---|---|
| `ConfigManager.java` | `getConfig(sectionName)` now has a **direct-file fallback**: if the section is not found in the merged MAIN_CONFIG, the code attempts to read `sectionName.json` from disk and unwrap the nested section (e.g. `chat.json` → `{"chat": {...}}` → returns the inner object). This handles stale-cache and mid-migration scenarios without requiring a manual reload. |
| `ConfigSplitter.java` | `migrateToSplitConfigs()` now calls `ConfigManager.getInstance().clearCache()` immediately after creating split files and the `.split_configs` marker. Previously the monolithic `config.json` content remained cached in memory until an explicit `/neoe reload`, causing all section lookups (including `"chat"`) to return empty. |

---

## [1.0.2.6+build.102] — 2026-05-04

### Bug Fix — Shop NPC Entity Registry Key Causes Client Disconnect

**Error:** `The server sent registries with unknown keys: ResourceKey[minecraft:entity_type / neoessentials:shop_npc]`

NeoForge 21.1.x mandatorily synchronises all `DeferredRegister<EntityType<?>>` entries to clients
during login handshake. The custom `ShopNpcEntity` type was registered server-side only, so every
client disconnected immediately on join.

**Solution:** Replaced the custom entity with vanilla `ArmorStand` entities tagged with the NBT
key `NeoEssentials_ShopId` (UUID value). Shop NPC interaction is intercepted via
`PlayerInteractEvent.EntityInteract` on the GAME event bus — no custom entity type required.

| File | Change |
|---|---|
| `ShopEntityRegistry.java` | Replaced `DeferredRegister<EntityType<?>>` with `@EventBusSubscriber(GAME bus)` + `onEntityInteract` handler that detects tagged armor stands and opens the shop menu |
| `ShopNpcEntity.java` | Converted from `PathfinderMob` subclass to a `final` static-utility class; exposes `create(Level, UUID, String)`, `getShopId(ArmorStand)`, `isShopNpc(ArmorStand)` |
| `NpcShopCommand.java` | `executeCreate` uses `ShopNpcEntity.create()` (returns `ArmorStand`); `executeRemove` uses `getEntitiesOfClass(ArmorStand.class, aabb, ShopNpcEntity::isShopNpc)` |
| `NeoEssentials.java` | Removed `ShopEntityRegistry.register(modEventBus)` call (no longer needed) |

### Bug Fix — UTF-8 BOM in Hologram Source Files (`illegal character: '\ufeff'`)

Six hologram-related Java files were saved with a UTF-8 BOM, producing javac errors
`error: illegal character: '\ufeff'` on the first line of each file.

BOM stripped from: `HologramEventHandler.java`, `HologramScheduler.java`,
`HologramTextProcessor.java`, `HologramCommand.java`, `ShopHologramManager.java`,
`HologramEndpoint.java`.

### Bug Fix — Permission Validator Incorrectly Flags External Mod Permissions

`PermissionValidator` was emitting "unknown permission" warnings for nodes belonging to other
mods (e.g. `worldedit.selection.pos`, FTB Ranks entries) because it only checked against the
NeoEssentials internal permission registry.

**Fix:** Added an external-mod gate in `PermissionValidator.java` — any node not prefixed with
`neoessentials.` is silently counted as `externalSkipped` and never flagged as an issue.
`ValidationResult` gained an `externalSkipped` field; the post-validation log line now reports
the skip count separately for clarity.

### Bug Fix — Missing Permission Nodes in PermissionRegistry

Three nodes used in command handlers were not registered in `PermissionRegistry`, producing
spurious "unknown permission" warnings in the validator output:

| Node | Default | Description |
|---|---|---|
| `neoessentials.chat.msgtoggle.bypass` | `false` | Message players who have toggled off DMs |
| `neoessentials.compass` | `true` | Use `/compass` |
| `neoessentials.compass.others` | `false` | View compass info for another player |

---

## [1.0.2.6+build.98] — 2026-04-27

### Feature — Web Dashboard: Discord Integration Page

The dashboard now shows live Discord bot integration status and a rolling relay-event log,
hooking directly into the three supported Discord bridge mods
(Simple Discord Link, DCIntegration, DiscordSRV).

#### New Files

| File | Purpose |
|---|---|
| `DiscordEndpoint.java` | REST handler at `/api/discord/*` — status, events, test-message, clear-log |
| `discord.html` | Dashboard page: adapter cards, supported-mods info, test-message panel, event log |
| `discord.js` | Calls all `/api/discord/*` endpoints, renders adapter cards & event table, auto-refreshes every 30 s |

#### Modified Files

| File | Change |
|---|---|
| `ChatIntegrationManager.java` | Added rolling event log (max 200 entries) recording every Discord relay event (chat, join, quit, mute, AFK, private message); `getRecentEvents()`, `getAdapterStatus()`, `clearEventLog()` public methods; log is cleared on server shutdown |
| `DashboardAPI.java` | Registered `/api/discord` context with auth middleware; updated endpoint log line |
| `DashboardFileManager.java` | Added `discord.html` and `discord.js` to managed file list |
| All existing HTML pages | Added 🤖 **Discord Integration** nav link to sidebar after Statistics entry |

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/discord/status` | Any | Loaded adapter list, `anyActive` flag, event count |
| `GET` | `/api/discord/events?limit=N` | Any | Recent relay events, most-recent first |
| `POST` | `/api/discord/test` | Admin | Send a test message (logs to server; relayed if adapters active) |
| `DELETE` | `/api/discord/events` | Admin | Clear the rolling event log |

#### Dashboard Features

- **Adapter cards** — each loaded Discord mod shown with name, description, and Active/Inactive badge
- **No-adapter state** — friendly message listing supported mods and their mod IDs when none are installed
- **Supported-mods info grid** — always-visible cards for SDLink, DCIntegration, DiscordSRV
- **Test-message panel** (admin only) — specify channel name/ID and message, fire a test relay
- **Event log table** — type badge (chat/join/quit/mute/afk/pm), player, target, channel, message preview, timestamp
- **Auto-refresh** every 30 seconds; manual Refresh and Clear Log (admin) buttons

---

## [1.0.2.6+build.97] — 2026-04-27

### Feature — Web Dashboard: Dedicated Login Page + Discord OAuth Frontend Integration

Completes the Discord OAuth2 authentication system (backend delivered in builds 92–96) by wiring
the frontend UI, creating a dedicated login page, and fixing the OAuth callback redirect paths.

#### Bug Fixes

| File | Bug | Fix |
|---|---|---|
| `AuthenticationHandler.java` | `handleDiscordOAuthCallback()` redirected to `/dashboard/login.html` (404) on error and `/dashboard/index.html` (404) on success — the `/dashboard/` prefix is not part of the URL structure | Error → `/login.html?error={code}`, Success → `/index.html?sessionId={id}&auth=discord` |
| `AuthenticationHandler.java` | Session was set as an `HttpOnly` cookie after OAuth success, which JS cannot read — the Bearer-token auth system then had no credentials and fell back to the login screen immediately | Replaced cookie with `sessionId` URL param so `dashboard.js` can store it in `localStorage` |
| `DashboardFileManager.java` | `DASHBOARD_FILES` list was missing `backup.html`, `backup.js`, `stats.html`, `stats.js` — those files were never auto-extracted from the JAR | Added all five new files (`backup.html`, `backup.js`, `stats.html`, `stats.js`, `login.html`) |

#### New Features

| Item | Detail |
|---|---|
| Standalone `login.html` | Dedicated `/login.html` page: password form + Discord login button; handles `?error=` param from OAuth callback; validates existing session and redirects to `index.html` if already logged in |
| Discord login button on `index.html` | "or / Discord Login" divider + button added to the embedded login form on the main dashboard page |
| `handleDiscordLogin()` in `dashboard.js` | Calls `GET /api/auth/discord/authorize`, receives the Discord authorization URL, and redirects the browser; shows inline error if Discord OAuth2 is not configured |
| OAuth `?sessionId=` param handler in `dashboard.js` | `checkAuthentication()` now reads `?sessionId=` and `?auth=discord` URL params at page load; stores token in `localStorage`, cleans the URL via `history.replaceState`, then validates normally |
| OAuth `?error=` param handler in `dashboard.js` | Readable error messages shown for `discord_auth_failed`, `access_denied`, and `missing_code` error codes returned from the callback |
| Discord login button styles | `.btn-discord`, `.login-divider` added to `styles.css`; Discord brand-purple, SVG icon, hover/disabled states |

---

## [1.0.2.6+build.96] — 2026-04-27

### Feature — Web Dashboard: Discord OAuth2 Auth Backend + Change-Password Flow

Completed the backend for the full Discord OAuth2 authentication system introduced in the
Dashboard Improvements roadmap.  No server-side code changes are required by administrators —
the system auto-generates `discord_auth.json` on first start.

#### New Files

| File | Purpose |
|---|---|
| `DiscordAuthConfig.java` | Typed wrapper around `discord_auth.json`; handles role mapping (Discord role ID → Dashboard role), whitelist/blacklist, permission-sync settings, and OAuth2 client credentials |
| `DiscordAuthProvider.java` | SDLink bridge — looks up linked Minecraft accounts and Discord role IDs from the Simple Discord Link mod; gracefully degrades when SDLink is not installed |
| `DiscordPermissionSync.java` | Syncs Discord role IDs → NeoEssentials permission nodes on player join (when `permissionSync.syncOnJoin = true`) |
| `DiscordSyncEventHandler.java` | NeoForge event handler that triggers `DiscordPermissionSync` on `PlayerLoggedInEvent` |
| `DiscordUser.java` | Value object: Discord ID, username, guild roles, linked Minecraft username |
| `SDLinkDataReader.java` | Reads SDLink's internal account-link data file as a fallback when the mod's API is unavailable |
| `SDLinkEventListener.java` | Listens for SDLink link/unlink events to keep account data in sync |

#### Updated Files

| File | Change |
|---|---|
| `AuthenticationHandler.java` | Added `handleDiscordOAuth()` full flow (code exchange → user info → guild roles → whitelist check → role mapping → user create/update → session); `handleDiscordAuthorizeRedirect()` returns authorization URL; `handleDiscordOAuthCallback()` browser redirect handler; `handleChangePassword()` validates old password, updates hash, clears `requiresPasswordChange` / `isTempPassword` flags, invalidates session |
| `ConfigManager.java` | Added `DISCORD_AUTH_CONFIG = "discord_auth.json"` constant; wired into `ensureDefaultConfigs()` with version 6; `EXPECTED_CONFIG_VERSIONS` updated |
| `discord_auth.json` (resource) | Default template with `enabled`, OAuth2 `clientId`/`clientSecret`/`redirectUri`, `roleMapping`, `whitelistedRoles`, `permissionSync.permissionMappings` sections; `_configVersion: 6` |

#### Auth Flow Summary

```
Browser → GET /api/auth/discord/authorize
       ← {"authorizeUrl": "https://discord.com/api/oauth2/authorize?..."}
Browser → Discord OAuth2 page (user approves)
Discord → GET /api/auth/discord/callback?code=XXX
Server  → exchange code → fetch /users/@me → fetch guild roles
       → whitelist/blacklist check → map Discord roles → create/update user → create session
       → 302 /index.html?sessionId=XXX&auth=discord
Browser → stores sessionId → calls /api/auth/validate → dashboard
```

---

## [1.0.2.6+build.91] — 2026-04-27

### Added
- **Web Dashboard — Backup & Restore** (`/api/backup/*` + `backup.html`)
  - `BackupManager` — creates named ZIP snapshots of `config/neoessentials/`, `neoessentials/`, and `world/playerdata/`; stores in `neoessentials/backups/`; writes `backup-manifest.json` inside each ZIP with name, timestamp, targets, and file count; auto-prunes oldest when count exceeds 20
  - `BackupEndpoint` — REST handler: `GET /api/backup/status`, `GET /api/backup/list`, `GET /api/backup/download?name=…`, `POST /api/backup/create`, `POST /api/backup/restore`, `DELETE /api/backup/delete?name=…`; all mutating operations require admin role
  - Restore flow creates an automatic pre-restore backup before overwriting live files
  - New `💾 Backup & Restore` dashboard page with stat cards, create-snapshot panel (name + target checkboxes), snapshot table (name/date/size/targets/file-count), and confirmation modals for restore/delete
  - `💾 Backup & Restore` nav link added to all existing dashboard pages (index, admin, permissions, teleport)

### Fixed
- Code quality pass (build #90): `@Nonnull` added to `ShopNpcEntity.mobInteract` return; `@SuppressWarnings("resource")` added to `applyPlaceholders`, `executeInfo`, `executeRemove` to suppress IntelliJ false-positive `Level`/`ServerLevel` try-with-resources warnings

---

## [1.0.2.6+build.86] — 2026-04-27

### 🐛 Bug Fix — `/nick` Nickname System: Tab List, Chat & Placeholder Integration

The `/nick` command confirmed the nickname was set (sent a success message) but had zero visible effect on the tab list, in chat, or above a player's head. Identical symptoms for self-nick and admin `/setnick`.

#### Root Cause Analysis

| Location | Bug | Impact |
|---|---|---|
| `NickCommand.updatePlayerDisplayName()` | Used `player.setCustomName()` — the entity cosmetic API. On players this adds a **second floating label** above the real name tag; it does not interact with the tab list or chat at all. | Tab list unchanged; chat unchanged |
| `DefaultPlaceholderExpansion` | `{neoessentials_displayname}` resolved via `player.getDisplayName()` which queries the scoreboard — completely unaware of `NickCommand.NICKNAMES`. Same for `{displayname_hover}`. | Nickname invisible in any format using `{neoessentials_displayname}` |
| `ChatFormatter.formatMessage()` | Hover/click name injection (clickable player names feature) hard-coded `player.getName()` and `player.getDisplayName()` for the `§HNAME§`/`§HDNAME§` markup tokens. | Hover popup showed real name even when a nickname was active |
| `TablistManager.getDisplayName()` | Checked its own internal `customNames` map (never populated by NickCommand). | Header/footer `{displayname}` token showed real name |
| Join handling | No packet was sent on reconnect to restore the tab-list display name. | Nickname disappeared from the tab list every relog |

#### Changes

| File | Change |
|---|---|
| `NickCommand.java` | `updatePlayerDisplayName()` rewritten: removed `setCustomName()`, now broadcasts `ClientboundPlayerInfoUpdatePacket(UPDATE_DISPLAY_NAME)` to all online players using the same reflection-based packet builder as `FakePlayerManager`. Added `onPlayerJoin(ServerPlayer)` public method to restore tab display name on reconnect. `applyNicknamesToOnlinePlayers()` also broadcasts packets instead of calling `setCustomName()`. Added `buildNickPacket()` helper (mirrors `FakePlayerManager.buildFakePacket()`). |
| `DefaultPlaceholderExpansion.java` | Added `getNickOrDisplayName()` helper. `displayname` and `displayname_hover` cases now call `NickCommand.getNickname()` first; fall back to `player.getDisplayName()` only when no nick is set. `username`/`username_hover` remain the real game-profile name (correct for admin tools). |
| `ChatFormatter.java` | Hover/click injection block now reads `NickCommand.getNickname()` for the `§HDNAME§` token, falling back to `player.getDisplayName()`. Ensures clickable nickname in chat when `chat.clickablePlayerNames = true`. |
| `TablistManager.java` | `getDisplayName()` now checks `NickCommand.getNickname()` before the internal `customNames` map and the real player name. Header/footer `{displayname}` token now shows the nickname. |
| `TablistEventHandler.java` | `onPlayerJoin()` now calls `NickCommand.onPlayerJoin(player)` after the tablist join handling, restoring the tab display-name packet on every login. |

---

### 🐛 Bug Fix — Shop Entity / NPC Shop Menu Compile Errors (11 errors → 0)

Pre-existing compile errors in the entity shop layer prevented the project from building.

#### Errors Fixed

| File | Error | Fix |
|---|---|---|
| `NpcShopMenu.java` | `clicked()` declared `public ItemStack clicked(...)` — MC 1.21.1 changed the return type to `void`. | Changed to `public void clicked(...)`, replaced `return ItemStack.EMPTY` with early `return`, removed return value from `super.clicked()`. |
| `NpcShopMenu.java` | Missing abstract method `quickMoveStack(Player, int)` — class would not compile without it. | Added `@Override public ItemStack quickMoveStack(Player player, int index)` — returns `ItemStack.EMPTY` for all shop display slots (no shift-click picking); delegates to normal flow for player inventory slots. |
| `NpcShopMenu.java` | `ShopTransaction.resolveItem()`, `.giveItems()`, `.hasSpaceInContainer()` are package-private; `NpcShopMenu` is in a different package. | Made all three methods `public static` in `ShopTransaction.java`. |
| `ShopNpcEntity.java` | `damageSources()` overridden with return type `DamageSource` — MC 1.21.1 signature returns `DamageSources` (the registry). Method was a pure no-op (`return super.damageSources()`). | Removed the override entirely. |

---

## [1.0.2.6+build.77] — 2026-04-27

### Feature — BungeeTabListPlus-Inspired Tablist Rework (Independent Mode + Proxy Integration)

Complete overhaul of the NeoEssentials tablist system, inspired by BungeeTabListPlus (BTLP),
the industry-standard proxy tablist plugin. NeoEssentials now manages its own tablist logic
independently (no proxy plugin required) while optionally integrating with BungeeCord/Velocity
proxies for cross-server player-count data.

#### New Files

| File | Purpose |
|---|---|
| `TablistLayout.java` | BTLP-style columns, group-weight sorting, playersByServer grouping, excludeServers/hiddenServers |
| `FakePlayerManager.java` | BTLP `fakePlayers` — decorative/separator tab entries with stable UUIDs |
| `ProxyIntegration.java` | BungeeCord plugin-messaging bridge; polls network player counts; `{network_online}`, `{server_online:X}` |

#### Updated Files

| File | Change |
|---|---|
| `TablistManager.java` | Full rewrite with BTLP-inspired architecture; per-player/group overrides; 20+ placeholder tokens; per-player session tracking |
| `TablistCommand.java` | Fixed duplicate class definition; added BTLP-style sub-commands (`/tablist proxy`, `fakeplayer`, `layout`, `independent`) |
| `TablistEventHandler.java` | Added `onPlayerJoin`/`onPlayerQuit` with session tracking and proxy state cleanup |
| `tablist.json` | `_configVersion` 2→3; added `independentMode`, `proxy`, `fakePlayers`, `layout` sections |

#### BTLP-Inspired Features Added

**1. Extended Placeholder Set (BTLP parity)**

| Placeholder | Description |
|---|---|
| `{network_online}` | Total players on proxy network (via BungeeCord) |
| `{server_online:NAME}` | Players on a specific proxy server |
| `{current_server}` | Proxy server name the viewing player is on |
| `{server_label}` | This server's configured display label |
| `{rank_weight}` | Numeric permission group weight |
| `{session_minutes}` | Minutes in current session |
| `{session_hours}` | Hours in current session |
| `{level}` | Player XP level |
| `{health}` | Current HP |
| `{max_health}` | Maximum HP |
| `{afk}` | Blank or AFK suffix when idle |
| `{displayname}` | Display name (coloured by group) |

**2. Independent Mode (default: on)**  
NeoEssentials owns the tablist rendering end-to-end. No proxy plugin needed.
Proxy integration (`proxy.enabled=false` by default) adds optional data (network counts)
without taking over the header/footer/player-row formatting.

**3. BTLP-style Fake Players (`fakePlayers`)**  
Decorative entries in the player list — separator rows, section labels, padding slots.
Each entry uses a stable deterministic UUID (survives reloads). Injected via
`ClientboundPlayerInfoUpdatePacket` using reflection-based entry injection (NeoForge limitation).

**4. Layout & Sorting (`TablistLayout`)**  
- 1–4 visual columns (BTLP's 4×20 = 80-slot grid equivalent)  
- Sort players by descending group weight then alphabetically (BTLP `ContextAwareOrdering`)  
- `groupSections` — bucket players with separator rows between group tiers  
- `playersByServer` — bucket players by proxy server name  
- `excludeServers` / `hiddenServers` — BTLP parity for multi-server visibility control  

**5. Proxy Integration (`ProxyIntegration`)**  
- BungeeCord plugin-messaging channel listener for `GetServers`, `PlayerCount`, `GetServer`
- Polls proxy every `pollIntervalTicks` (default 100 = 5s) for network counts  
- Per-player server tracking (`playerServerMap`)  
- `{network_online}` / `{server_online:X}` placeholders feed from proxy data  
- Outbound messaging stub — full BungeeCord outbound support deferred pending NeoForge
  `StreamCodec` registration (proxy is disabled by default so no runtime impact)

**6. `/tablist` Command Extensions**  
New sub-commands following BTLP patterns:
```
/tablist proxy status              — proxy integration status & per-server counts
/tablist proxy setserver NAME N    — manual server count override
/tablist fakeplayer list           — show fake entries
/tablist fakeplayer add ID DISPLAY — add runtime fake entry
/tablist fakeplayer remove ID      — remove fake entry
/tablist fakeplayer refresh        — re-inject all fake entries for all players
/tablist layout info               — show column/sort/server config
/tablist independent [on|off]      — toggle independent mode
/tablist info                      — full status (all sub-systems in one view)
```

**7. `tablist.json` Config Additions** (`_configVersion` 2→3)

```json
"independentMode": true,
"proxy": {
  "enabled": false,
  "serverLabel": "Main",
  "pollIntervalTicks": 100,
  "showNetworkPlayers": false,
  "knownServers": []
},
"fakePlayers": [],
"layout": {
  "columns": 1,
  "sortByGroupWeight": true,
  "groupSections": false,
  "playersByServer": false,
  "excludeServers": [],
  "hiddenServers": [],
  "maxSlotsPerColumn": 20
}
```

#### Bug Fixes
- **`TablistCommand.java`** — Fixed duplicate class definition that caused a compile error
  (the old non-BTLP handler block was left appended after the new class).
- **`FakePlayerManager.java`** — Fixed `ClientboundPlayerInfoUpdatePacket` constructor call;
  NeoForge 1.21.x has no constructor accepting `List<Entry>` — fixed using reflection.
- **`ProxyIntegration.java`** — Removed broken `CustomPacketPayload#write(FriendlyByteBuf)`
  override; NeoForge 1.21.1 removed `write()` from the interface in favour of `StreamCodec`.

---

## [1.0.2.6+build.73] — 2026-04-27

### Feature — Messaging & SocialSpy Improvements

Full enhancement pass on `/msg`, `/reply`, and SocialSpy to make template formatting
more robust, debuggable, and admin-configurable.

#### What's New

**1. Centralized `MessageUtil.resolveTemplate()` helper**

New public utility method that replaces the manual `.replace("{MESSAGE}", …)` +
`PlaceholderAPI.setPlaceholders()` pattern used across `/msg` and `/reply`:

```java
String out = MessageUtil.resolveTemplate(player, template, Map.of("message", text));
```
Resolution order:
1. Apply `extraVars` — case-insensitive token replacement so both `{MESSAGE}` and `{message}` match.
2. Run `PlaceholderAPI.setPlaceholders()` for remaining `{neoessentials_*}` and external tokens.
3. **Debug mode only** — scan the result for any `{TOKEN}` tokens that remain unresolved and log them
   with `WARN` so admins/developers can spot template misconfigurations immediately.

**2. Improved fallback formatting**

- `MessageUtil.localize()` already falls back to a human-readable key name on parse failure.
- `resolveTemplate()` never throws — PlaceholderAPI errors are swallowed; the original
  template is returned safely instead of the server crashing or logging a wall of stacktrace.

**3. Debug logging for missing / misparsed placeholders**

When `logging.enableDebugLogging = true` in `config.json`, the following are now logged:
- Every `{TOKEN}` placeholder still unresolved after full resolution (per template, per call).
- SocialSpy pre-resolution: `format='' → resolved=''` so admins can verify their templates.
- SocialSpy config read: which source (config vs. lang key) was used for the format.

**4. Admin-configurable SocialSpy & PM format in `config.json`**

New `chat.messaging` section added:

```json
"messaging": {
  "socialspyFormat":  "",   // Override neoessentials.socialspy.format lang key
  "msgFormatTo":      "",   // Override commands.neoessentials.msg.format.to
  "msgFormatFrom":    "",   // Override commands.neoessentials.msg.format.from
  "replyFormatTo":    "",   // Override commands.neoessentials.reply.format.to
  "replyFormatFrom":  ""    // Override commands.neoessentials.reply.format.from
}
```

Leave any value blank (`""`) to use the language-file default. When set, the config value
takes priority and changes are picked up on the next message send without restarting.

Supported named placeholders in all five templates:

| Placeholder | Meaning |
|---|---|
| `{sender}` | Raw username of message sender |
| `{receiver}` | Raw username of message recipient |
| `{message}` / `{MESSAGE}` | The private message text (case-insensitive) |
| `{sender_displayname}` | Display name of sender (via PlaceholderAPI) |
| `{receiver_displayname}` | Display name of recipient |
| `{neoessentials_displayname}` | Context-player display name (resolved via PlaceholderAPI) |
| Any `{neoessentials_*}` | Any registered PlaceholderAPI token |

**5. SocialSpy format updated to named placeholders**

`neoessentials.socialspy.format` in `en_us.json` updated from `{0}`, `{1}`, `{2}` positional
args to named vars `{sender}`, `{receiver}`, `{message}`.  Existing customisations using `{0}`,
`{1}`, `{2}` will stop working — admins should update their lang file or use the new
`config.json` format override.

SocialSpy broadcast now pre-resolves display names for sender and receiver before iterating
spy recipients (one PlaceholderAPI call per broadcast instead of one per spy player).

**6. `_configVersion` bumped `20 → 21`**

New `chat.messaging` block added to the shipped `config.json` template.  Existing server
configs are not auto-migrated (the section is simply absent — all values fall back to lang
keys, preserving the previous behaviour).

**7. `_langVersion` bumped `14 → 15`**

Updated `neoessentials.socialspy.format` key auto-merges into existing deployments on next
server start.

| File | Change |
|---|---|
| `MessageUtil.java` | Added `resolveTemplate()`; `NAMED_PLACEHOLDER_PATTERN`; `CURRENT_LANG_VERSION` 14→15 |
| `SocialSpyManager.java` | Config-backed format; named placeholders; display-name pre-resolution; debug logging; LOGGER added |
| `MsgCommand.java` | Uses `resolveTemplate()`; added `getMsgFormat()` config helper |
| `ReplyCommand.java` | Uses `resolveTemplate()` + `MsgCommand.getMsgFormat()` |
| `config.json` | Added `chat.messaging` section; `_configVersion` 20→21 |
| `en_us.json` | SocialSpy format `{0}/{1}/{2}` → `{sender}/{receiver}/{message}`; `_langVersion` 14→15 |

---

## [1.0.2.6+build.72] — 2026-04-27

### Bug Fix — FTB Ranks Adapter: `NoSuchMethodException` on Permission Checks

**Problem:** NeoEssentials threw `java.lang.NoSuchMethodException` for every FTB Ranks permission
check, causing all FTB Ranks permission validation to silently fail and fall through to the internal
permission system (or return `false`). Reported against NeoEssentials `1.0.2.5 build 1074`, still
present in the 1.0.2.6 line.

```
Failed to check FTB Ranks permission
java.lang.NoSuchMethodException: dev.ftb.mods.ftbranks.api.FTBRanksAPI.hasPermission(java.util.UUID,java.lang.String)
at com.zerog.neoessentials.permissions.FtbRanksAdapter.hasPermission(FtbRanksAdapter.java:54)
```

**Root cause:** `FtbRanksAdapter.probeApi()` attempted these signatures in order:

1. `FTBRanksAPI.getPermission(ServerPlayer, String, boolean)` ← **does not exist**
2. `instance.hasPermission(UUID, String)` ← **does not exist in 2101.1.3**
3. `FTBRanksAPI.hasPermission(ServerPlayer, String)` ← **does not exist**
4. `FTBRanksAPI.checkPermission(ServerPlayer, String)` ← **does not exist**

All four probes failed, so `resolvedMethod` stayed `null`, `isAvailable()` returned `false`, and
every subsequent call quietly returned `false`. The FTB Ranks API jar (`2101.1.3`) was inspected
directly and the actual public method is:

```
public static PermissionValue FTBRanksAPI.getPermissionValue(ServerPlayer, String)
```

**Changes:**

- **`FtbRanksAdapter.java`** — Strategy table rewritten with correct signatures:

| # | Method | Target | Notes |
|---|---|---|---|
| 1 | `FTBRanksAPI.getPermissionValue(ServerPlayer, String)` | static | ✅ **Confirmed 2101.1.x** |
| 2 | `RankManager.getPermissionValue(ServerPlayer, String)` | instance via `getInstance().getManager()` | Secondary accessor |
| 3 | `FTBRanksAPI.hasPermission(ServerPlayer, String)` | static | Legacy fallback |
| 4 | `FTBRanksAPI.checkPermission(ServerPlayer, String)` | static | Naming-change fallback |
| 5 | `instance.hasPermission(UUID, String)` | instance | Oldest builds |

- `invokeResolvedMethod()` updated to dispatch strategies 1–5 correctly.
- `extractBoolean()` updated to try `asBooleanOrFalse()` (the `PermissionValue` API) before other
  coercion paths; `"MISSING"` added to the `toString()` deny-list.

| File | Change |
|---|---|
| `FtbRanksAdapter.java` | Strategy 1 corrected; strategy 2 added; UUID strategy moved to 5; `extractBoolean()` improved |

---

## [1.0.2.6+build.70] — 2026-04-27

### Bug Fix — `/msg` & `/reply` SocialSpy Formatting + Missing `neoessentials.socialspy.format` Key

**Problem:** Every `/msg` and `/reply` execution produced a `java.lang.IllegalArgumentException` in the
server console, and players received raw template text instead of formatted private messages:

```
Failed to format message - Key: commands.neoessentials.msg.format.to,
  Template: '&7[&aTo &f{neoessentials_displayname}&7] &f{MESSAGE}',
  Args: [], Error: can't parse argument number: neoessentials_displayname
```

**Root cause:** `MessageUtil.localize()` passed the raw translation template directly to
`java.text.MessageFormat.format()`.  `MessageFormat` treats every `{…}` token as a positional
argument index.  Templates for `/msg` and `/reply` contain NeoEssentials placeholder tokens
(`{neoessentials_displayname}`, `{MESSAGE}`) that do not start with a digit, so `MessageFormat`
tried to parse them as argument names and threw `IllegalArgumentException`.

**Changes:**

- **`MessageUtil.java`** — Added `escapeNamedPlaceholders(String)` private helper.  
  Uses the regex `\{([^0-9'{}][^}]*)}` to detect non-numeric `{TOKEN}` placeholders and wraps
  them in MessageFormat's single-quote literal escape: `'{'TOKEN'}'`.  After `MessageFormat.format()`
  runs, these tokens are emitted verbatim as `{TOKEN}` and resolved later by `PlaceholderAPI`.  
  Positional placeholders `{0}`, `{1}`, … (starting with a digit) are left untouched.  
  Both overloads of `localize()` now call `escapeNamedPlaceholders()` before `MessageFormat.format()`.

- **`en_us.json`** — Added the missing `neoessentials.socialspy.format` translation key:
  ```
  "neoessentials.socialspy.format": "&8[&eSocialSpy&8] &b{0} &7→ &b{1}&7: &f{2}"
  ```
  `{0}` = sender name · `{1}` = receiver name · `{2}` = message content.

- **`_langVersion` bumped `13 → 14`** — `CURRENT_LANG_VERSION` in `MessageUtil.java` updated to
  match.  Existing server deployments auto-merge the new key on next startup.

| File | Change |
|---|---|
| `MessageUtil.java` | Added `escapeNamedPlaceholders()`; both `localize()` overloads updated |
| `en_us.json` | Added `neoessentials.socialspy.format`; `_langVersion` → 14 |

---

## [1.0.2.6+build.69] — 2026-04-24

### Feature — Custom Player Tablist: Refinements & `processTablistText`

Follow-up polish pass on the Custom Player Tablist system introduced in build.67.

**Changes:**

- **`RichTextFormatter.processTablistText(String)`** — new dedicated entry point for tablist-specific
  rich-text processing.  Unlike `processRichText()` (which may emit hover/click events), this method
  strips any hover/click markers that tab-list packets cannot render, then delegates to
  `ChatComponentUtil.parseColorCodes()` for the final color pass.  All five processing layers
  (gradient, rainbow, named-color tags, format tags, `<color:#RRGGBB>` spans) run unconditionally
  regardless of the server's `enableChatEnhancements` flag.
- **`TablistManager`** — replaced remaining `Component.literal()` calls in `updatePlayer()` and
  `updatePlayerTeam()` with `RichTextFormatter.processTablistText()`.  Hex colors and gradient tags
  now render correctly in both the header/footer area **and** the per-player prefix/suffix column.
- **`applyPlaceholders()`** — `&` → `§` conversion removed from this method; color processing is
  deferred entirely to `processTablistText()` so that `&#RRGGBB` tokens and `<gradient:…>` tags
  survive placeholder substitution intact.

| File | Change |
|---|---|
| `RichTextFormatter.java` | Added `processTablistText()` with full tag pipeline + hover/click strip |
| `TablistManager.java` | All Component building now routed through `processTablistText()` |

---

## [1.0.2.6+build.67] — 2026-04-24

### Feature — Custom Player Tablist (full implementation)

Complete rewrite and feature expansion of the tablist system.  **References: TAB [1.7–1.21.x], BungeeTabListPlus, Simple TabList.**

**New features:**

**1 — Hex colors & gradients everywhere**

Header, footer, and per-player prefix/suffix now support the same rich-text syntax as chat:

| Syntax | Effect |
|---|---|
| `&#RRGGBB` | Inline hex color |
| `<gradient:FF0000-0000FF>text</gradient>` | Per-character smooth gradient (2+ color stops) |
| `<rainbow>text</rainbow>` | Cycling rainbow |
| `<red>text</red>`, `<gold>`, … | Named Minecraft colors |
| `<bold>`, `<italic>`, `<underline>`, … | Format tags |
| `<color:#RRGGBB>text</color>` | Arbitrary hex color span |
| `&X` | Legacy `§` codes |

**2 — Animated header/footer**

`header` and `footer` in `tablist.json` accept either a single string or a JSON array of strings.
Each tick cycle advances to the next frame, creating smooth animations.  `refreshInterval` controls
how many server ticks between frame advances (default `20` = 1 s).

**3 — Per-group header/footer**

`tablist.json` accepts a `"groups"` object.  Each key is a permission group name; each value can
supply its own `"header"` and/or `"footer"` (string or array).  Priority: **per-player → per-group → global**.

```json
"groups": {
  "admin": {
    "header": "<gradient:FF0000-FF8C00>&lAdmin Panel&r\n&#FFFFFF{online}&8/&7{max} online",
    "footer": "&7TPS: {tps} &8| &7{world} &8| &3Admin mode active"
  }
}
```

**4 — Per-player header/footer overrides**

- **Config:** `"players"` object in `tablist.json` with UUID keys, same `header`/`footer` schema.
- **In-game runtime commands (new `/tablist player` branch):**
  - `/tablist player <name> header <text>` — set per-player header
  - `/tablist player <name> footer <text>` — set per-player footer
  - `/tablist player <name> reset` — clear all per-player overrides

**5 — Per-group runtime commands**

- `/tablist group <group> header <text>` — runtime per-group header override
- `/tablist group <group> footer <text>` — runtime per-group footer override
- `/tablist group <group> reset` — clear group overrides
- `/tablist info` now lists all active group overrides

**6 — Extended placeholder set**

| Placeholder | Value |
|---|---|
| `{player}` | Raw player name |
| `{displayname}` | Display name (respects nick + per-group colour override) |
| `{online}` | Visible online count (vanished excluded for non-staff) |
| `{max}` | Server player slots |
| `{ping}` | Player latency ms |
| `{world}` | Dimension path (e.g. `overworld`) |
| `{tps}` | Server TPS, pre-coloured `&a`/`&e`/`&c` |
| `{time}` | Real-world server time `HH:mm` |
| `{server_name}` | Server MOTD |
| `{x}` `{y}` `{z}` | Player block coordinates |
| `{balance}` | Player balance (EconomyManager) |
| `{prefix}` `{suffix}` | Permission group prefix/suffix |
| `{group}` | Permission group name |
| `{newline}` | Line break |
| `{bar}` | Decorative `&8&m` separator |

**7 — Vanish & AFK integration**

- `hideVanished: true` excludes vanished players from the `{online}` count for non-staff viewers.
- `showAfkIndicator: true` appends `afkSuffix` (default `&7[AFK]`) to the player's tab row.

**8 — tablist.json config template**

Default config file expanded with rich-text examples, per-group section, per-player UUID section,
gradient/hex syntax reference comments, and `groupColors` override map.

**Changes:**

| File | Change |
|---|---|
| `TablistManager.java` | Full rewrite: animated frames, per-player/group overrides, extended placeholders, rich-text pipeline, vanish/AFK integration, null-safe permission helpers |
| `TablistCommand.java` | Added `player` and `group` subcommand trees; updated help text with syntax examples |
| `tablist.json` (bundled) | Expanded template with gradient header example, per-group section, per-player section, groupColors map |

---

## [1.0.2.6+build.66] — 2026-04-24

### Bug Fix — Tablist prefix, Warn console logging, WarnManager duplicate method

**1 — Tablist prefix not showing before username** (`TablistManager`)

`getPermissionPrefix()` and `getPermissionSuffix()` used `PermissionSystem.getManager()`, which throws
`IllegalStateException` before the permission system initialises and silently returns `""` in the catch.
Additionally, both helpers accessed `PermissionAPI.getManager()` rather than the null-safe accessor.

*Fix:* Switched all three tablist permission helpers
(`getPermissionPrefix`, `getPermissionSuffix`, `getPermissionGroup`) to `PermissionAPI.getManager()`, added
an explicit `null` guard on the manager reference, and made the group fallback consistent: if a player has
no explicit user entry (or their group is `null`), the code now correctly falls back to
`mgr.getDefaultGroup()` before looking up the group prefix/suffix. The scoreboard team (and thus the tab list
prefix row) now reliably shows the group prefix for every player, including freshly-joined players whose user
entry was auto-created by the permission manager.

**2 — Warn command not logging to console** (`WarnCommand`)

`executeWarn()` called `source.sendSuccess(..., true)` (broadcasts to ops) but never called
`LOGGER.info()` directly — unlike `executeClearWarnings` and `executeRemoveWarn` which had explicit
log calls. On some server configurations `sendSuccess` feedback is not routed to the console log.

*Fix:* Added an explicit `LOGGER.info("[Warn] {} warned {} for: {} (warn #{}, ID: {})", ...)` call in
`executeWarn()` so every issued warn is always visible in the server console and log files.

**3 — WarnManager compile error: duplicate `getInstance()` method** (`WarnManager`)

`WarnManager.java` had two identical `public static WarnManager getInstance()` declarations (lines 28 and 44),
causing a compile-time error (`method getInstance() is already defined in class WarnManager`). This prevented
the mod from building.

*Fix:* Removed the second duplicate declaration.

**Changes:**

| File | Change |
|---|---|
| `TablistManager.java` | `getPermissionPrefix/Suffix/Group` now use `PermissionAPI.getManager()` with null guard and consistent `defaultGroup` fallback |
| `WarnCommand.java` | Added `LOGGER.info` to `executeWarn()` for guaranteed console output |
| `WarnManager.java` | Removed duplicate `getInstance()` method |

---

## [1.0.2.6+build.64] — 2026-04-24

### Improvement — Localization Audit & Tooling

Full audit of all in-game translation key usage across 130+ source files.  
54 missing keys added, fallback text improved, and a new suite of `/language` sub-commands added for server admins.

**54 missing translation keys added to `en_us.json`:**

| Category | Keys Added |
|---|---|
| TPA / Teleport Requests | `teleport.request.*` — 25 keys (sent, received, denied, expired, cancelled, failed, etc.) |
| Misc Teleport | `back_info`, `death_info`, `jump_success`, `jump_failed`, `no_open_space` |
| Pending TPA info | `teleport.request.pending_info` |
| Spawn / Warp errors | `teleport.spawn.invalid_coordinates`, `teleport.spawn.no_permission`, `teleport.warp.invalid_coordinates` |
| Warp list | `warp.list_statistics`, `warp.playerwarps_list_header` |
| Home | `home.no_pending_delete_generic`, `home.overwrite_cancelled` |
| Moderation | `player_only_command`, `reason_too_long`, `unfrozen_message`, `jail_success`, `unjail_success` |
| General | `neoessentials.error.no_server`, `channel.error`, `command.player_only`, `error.player_only`, `near.server_error` |
| Dashboard | `dashboard.separator`, `dashboard.title` |
| Gamemode | `gamemode.changed_other` |
| Mutelist | `mutelist.list` |

**`MessageUtil` improvements:**

- `localize(key, args...)` — when a key is not found, generates a human-readable English fallback by stripping the `commands.neoessentials.` prefix and capitalising. Players no longer see raw key strings like `commands.neoessentials.home.not_found` in chat.
- New `localize(key, String fallback, args...)` overload — callers that know the English text can pass it as an explicit fallback.

**New `/language` sub-commands:**

| Command | Description |
|---|---|
| `/language validate <code>` | Compare a language file vs `en_us.json`; shows coverage %, missing keys (first 10), extra keys |
| `/language regenerate <code>` | Refresh a language file from the JAR (backup to `.bak`, merge user values) |
| `/language override set <key> <value>` | Override any message key in-game |
| `/language override get <key>` | View current value for a key |
| `/language override remove <key>` | Remove a specific override |
| `/language override list` | List all active overrides |
| `/language override clear` | Remove all overrides |
| `/language override reload` | Reload overrides from disk |

Overrides are persisted to `neoessentials/languages/overrides.json` and take priority over all language files.  
`_langVersion` bumped **12 → 13** (triggers automatic key-merge on existing deployments).

**Changes:**

| File | Change |
|---|---|
| `en_us.json` | 54 new keys added, `_langVersion` 12 → 13 |
| `MessageUtil.java` | Human-readable fallback for missing keys; new `localize(key, fallback, args)` overload; `CURRENT_LANG_VERSION` 12 → 13 |
| `CustomLanguageManager.java` | Override CRUD (`setOverride`, `removeOverride`, `getOverride`, `getOverrides`, `clearOverrides`); `ValidationReport` class + `validateLanguage()`; `regenerate()` with automatic backup; `loadOverrides()` / `saveOverrides()`; overrides loaded on `initialize()` and `reload()` |
| `LanguageCommand.java` | Added `validate`, `regenerate`, `override set/get/remove/list/clear/reload` sub-commands |

---

## [1.0.2.6+build.59] — 2026-04-24

### Bug Fix — Chat: `{neoessentials_username_hover}` unresolved + duplicate vanilla log line

Two related chat-formatting bugs fixed.

**Bug 1 — Unresolved placeholder in formatted chat:**

`ChatFormatter.formatMessage()` replaced `{neoessentials_username}` with `{neoessentials_username_hover}`
when the `clickablePlayerNames` config option was enabled. `username_hover` was never registered in
`DefaultPlaceholderExpansion`, so `PlaceholderAPI.setPlaceholders()` left the token as literal text in
the Component. Players saw the placeholder text instead of the player's name, and the server console
showed the unresolved token in the formatted output.

**Bug 2 — Duplicate vanilla log line:**

`ChatHandler.onServerChat()` called `server.sendSystemMessage(formattedMessage)` after already logging
via `LOGGER.info(...)`. `MinecraftServer.sendSystemMessage()` writes to vanilla's own logger, producing
a second log line in the format `<component.getString()>` — e.g. `<{neoessentials_username_hover}> chuj`.
This was purely cosmetic (log noise) but made the bug visible and confused server admins.

**Changes:**

| File | Change |
|---|---|
| `ChatFormatter.java` | Lines 70-74: Replaced `{neoessentials_username_hover}` placeholder substitution with `§HNAME§<name>§/HNAME§` and `§HDNAME§<dname>§/HDNAME§` internal markup tokens. Tokens are only injected when **both** `clickablePlayerNames` and `enableChatEnhancements` are true — otherwise `{neoessentials_username}` is left for PlaceholderAPI to resolve to plain text. |
| `ChatFormatter.java` | `buildComponentFromMarkup()`: Added handling for `§HNAME§` and `§HDNAME§` tokens (with corresponding entries in the markers array). Each token produces a `MutableComponent` with a `SUGGEST_COMMAND` click event (`/msg playerName `) and a `SHOW_TEXT` hover event. |
| `ChatFormatter.java` | Added `createClickablePlayerNameComponent(String displayText, ServerPlayer player)` helper method. |
| `DefaultPlaceholderExpansion.java` | Added `username_hover` and `displayname_hover` to the placeholder set, resolving to the same plain text as `username` and `displayname` respectively. Safety net for any config string that uses the `_hover` token directly. |
| `ChatHandler.java` | Removed `server.sendSystemMessage(formattedMessage)` block. Chat is already logged via `LOGGER.info(...)` in the proximity/permission/global branches above. The removed call was producing the duplicate `[net.minecraft.server.MinecraftServer/]: <...> message` log line. |

---

## [1.0.2.6+build.58] — 2026-04-24

### Feature — API & Placeholder System

Completes the API & Placeholder System milestone. Exposes the placeholder system as a fully public,
thread-safe Java API for external mods; adds `/placeholder` in-game admin command; adds
`/api/placeholders` REST endpoints; wires the documentation handler to `/api/docs`; and rewrites
`APISystem.md` with comprehensive developer documentation.

**Changes:**

| File | Change |
|---|---|
| `PlaceholderProvider.java` | Extracted to a `public` top-level `@FunctionalInterface` so external mods can implement it. Previously the type was embedded as a package-private inner type in `PlaceholderAPI.java`. |
| `PlaceholderExpansion.java` | Extracted to a `public` top-level abstract class. Same fix — was package-private, preventing any external mod from extending it. |
| `NeoEssentialsAPI.java` | Added `getPlaceholderManager()` returning `PlaceholderManager.getInstance()`. Bumped `API_VERSION` to `"1.2.0"`. Added Javadoc changelog block. |
| `PlaceholderEndpoint.java` | New REST handler `/api/placeholders` with `GET` list, `POST` resolve, and `DELETE` stats routes, all authenticated by `DashboardAPI.withAuth()`. |
| `DashboardAPI.java` | Registered `/api/placeholders` → `PlaceholderEndpoint` and `/api/docs` → `DocumentationHandler` (was never wired). Added both to startup log. |
| `PlaceholderCommand.java` | New in-game command `/placeholder` with sub-commands: `list`, `info <id>` (tab-completes), `test <text>`, `stats`. Permission: `neoessentials.admin.placeholders`. |
| `NeoEssentials.java` | Registered `PlaceholderCommand` in `registerAllCommands()`. |
| `PermissionRegistry.java` | Registered `neoessentials.admin.placeholders` ("Manage and test the placeholder system") in `ADMIN` category. Fixed a formatting bug on the `neoessentials.admin.dashboard` line (was concatenated with the section comment). |
| `DocumentationManager.java` | Added `placeholder-api` and `developer-api` sections. Added `/api/placeholders/list`, `/api/placeholders/resolve`, `/api/placeholders/stats` entries to API documentation. |
| `docs/Wiki/APISystem.md` | Full rewrite: built-in placeholder table with all 30+ tokens and short-form aliases, `PlaceholderProvider` and `PlaceholderExpansion` code examples, `NeoEssentialsAPI` full reference (Economy/Permissions/Placeholder), REST endpoint tables for all routes, `/placeholder` command reference, versioning contract. |

---

## [1.0.2.6+build.57] — 2026-04-24

### Feature — Chat Formatting Options: Per-Player Overrides, Rich Text & Documentation

Completes the Chat Formatting Options milestone. All rich-text infrastructure (gradients, rainbow,
hex colors, hover/click events) was already implemented in `RichTextFormatter` and `ChatFormatter`.
This build wires per-player format overrides into the live chat pipeline and fully documents the system.

**Bug fixed — per-player format overrides were stored but never applied:**

`PlayerChatFormatManager` already had full CRUD + persistence for per-player format strings, and
`ChatFormatCommand` provided the admin commands to set/clear them. However, `ChatHandler.onServerChat()`
never consulted `PlayerChatFormatManager` when resolving the format string — every chat line always
fell through to the group/world lookup regardless of any stored override.

**Changes:**

| File | Change |
|---|---|
| `ChatHandler.java` | `onServerChat()` now checks `PlayerChatFormatManager.getInstance().getFormat(player.getUUID())` **before** calling `chatManager.getChatFormat(group, world)`. Per-player overrides are now the highest-priority step in the format resolution chain: per-player → group+world → group → world → default. |
| `ChatSystem.md` | Full rewrite and expansion: added Format Priority Hierarchy section, `/chatformat` command reference table, complete rich-text tag reference (all `RichTextFormatter` and `ChatFormatter` tags with syntax and examples), hex color and gradient examples, hover/click event examples, full config key reference table, placeholder list, and copy-paste format string examples. |

**Format resolution priority (highest → lowest):**
1. Per-player override (`/chatformat set <player> <format>`)
2. Group + World key (`group:admin+world:overworld`)
3. Group key (`group:admin`)
4. World key (`world:overworld`)
5. Default format

**Rich text tags (all already implemented, now fully documented):**

| Tag | Example |
|---|---|
| Hex color | `&#FF5500text` or `<color:#FF5500>text</color>` |
| Legacy codes | `&cred &agreen &lbold` |
| Gradient | `<gradient:FF0000-0000FF>colorful text</gradient>` |
| Rainbow | `<rainbow>text</rainbow>` |
| Bold/italic/etc | `<bold>text</bold>` `<italic>text</italic>` |
| Hover tooltip | `<hover:text:Tooltip here>visible text</hover>` |
| Click command | `<click:run_command:/spawn>click me</click>` |
| Click URL | `<click:open_url:https://example.com>link</click>` |

**Per-player format commands:**

| Command | Permission | Description |
|---|---|---|
| `/chatformat set <player> <format>` | `neoessentials.chat.format.set` | Set a per-player format override |
| `/chatformat clear <player>` | `neoessentials.chat.format.set` | Remove per-player override (reverts to group format) |
| `/chatformat check <player>` | `neoessentials.chat.format.check` | View current override for a player |
| `/chatformat list` | `neoessentials.chat.format.check` | List all active per-player overrides |
| `/chatformat reload` | `neoessentials.chat.format.reload` | Reload per-player format data from disk |

---

## [1.0.2.6+build.56] — 2026-04-24

### Feature — Inventory Management & Security Improvements

Completes the Inventory Management Tools and Inventory Command Security Improvements milestones.

**What was already implemented:**
Commands `/invsee`, `/inv`, `/invseeedit`, `/enderchest`, `/ec`, `/enderchestedit`, `/ecedit` existed with permission nodes registered in `PermissionRegistry`. Config flags in `commands.*` were present in `config.json`.

**What was missing (now fixed):**

| Gap | Fix |
|---|---|
| Config flags never read at runtime | `InventoryViewCommands` `requires()` predicates now check `ConfigManager.isCommandEnabled(...)` for all four command groups |
| No concurrent-edit protection | Two `ConcurrentHashMap<UUID targetId, UUID viewerId>` maps (`activeInvEdits`, `activeEcEdits`) enforce single-editor-at-a-time; blocked editors receive a message naming the current holder |
| No audit log | New `InventoryAuditLogger` appends every view/edit open to `neoessentials/inventory_audit.log` |
| Locks not released on disconnect | New `InventoryEventHandler` (`@EventBusSubscriber`) calls `releaseEditLocks(uuid)` on `PlayerLoggedOutEvent` |
| Missing language keys | Added `invsee.disabled`, `invsee.concurrent_edit`, `ec.disabled`, `ec.concurrent_edit` to `en_us.json` |

**Changes:**

| File | Change |
|---|---|
| `InventoryViewCommands.java` | `requires()` for all commands checks `isCommandEnabled()` flag; `ConcurrentHashMap` locks added; `InventoryAuditLogger` called on every action |
| `InventoryAuditLogger.java` | New — append-only audit log, 7 action types: `INV_VIEWED`, `INV_EDIT_OPENED`, `INV_EDIT_CLOSED`, `EC_VIEWED`, `EC_EDIT_OPENED`, `EC_EDIT_CLOSED`, `EDIT_BLOCKED` |
| `InventoryEventHandler.java` | New — `@EventBusSubscriber`; releases edit locks and logs `*_CLOSED` on `PlayerLoggedOutEvent` |
| `en_us.json` | 4 new translation keys for disabled and concurrent-edit scenarios |
| `config.json` | Added `items.inventoryAuditLog: true` config key |

**Permission nodes (all OP-only by default, no change):**

| Node | Default | Description |
|---|---|---|
| `neoessentials.invsee` | false | View another player's inventory (read-only) |
| `neoessentials.invsee.edit` | false | Edit another player's inventory |
| `neoessentials.enderchest` | false | View another player's ender chest (read-only) |
| `neoessentials.enderchest.edit` | false | Edit another player's ender chest |

---

## [1.0.2.6+build.55] — 2026-04-24

### Improvement — Teleportation Per-Command Bypass Permissions & Chunk Loading Documentation

**Per-command bypass permission nodes registered:**

All 8 per-command cooldown/warmup bypass nodes were already checked in code but were absent from
`PermissionRegistry`, making them invisible to the dashboard and `/neoe permissions`.

| Node registered | Description |
|---|---|
| `neoessentials.teleport.home.bypass.cooldown` | Skip home teleport cooldown |
| `neoessentials.teleport.home.bypass.warmup` | Skip home teleport warmup |
| `neoessentials.teleport.warp.bypass.cooldown` | Skip warp use cooldown |
| `neoessentials.teleport.warp.bypass.warmup` | Skip warp teleport warmup |
| `neoessentials.teleport.spawn.bypass.cooldown` | Skip spawn teleport cooldown |
| `neoessentials.teleport.spawn.bypass.warmup` | Skip spawn teleport warmup |
| `neoessentials.teleport.back.bypass.cooldown` | Skip /back cooldown |
| `neoessentials.teleport.back.bypass.warmup` | Skip /back warmup |

**Documentation added:**

Added **"Chunk Loading & Safety Interaction"** section to `docs/Wiki/TeleportationSystem.md` explaining:
- 3×3 chunk preload before every teleport
- Order of operations (chunk load → optional safety scan → teleport)
- Effect of disabling safety checks (`enableHomeSafety: false` skips `findSafeLocation()`)
- Error behaviour on failed chunk loading
- Configuration quick-reference table

**Changes:**

| File | Change |
|---|---|
| `PermissionRegistry.java` | Added 8 `register()` calls for per-command bypass nodes after the existing global bypass pair |
| `TeleportationSystem.md` | New "Chunk Loading & Safety Interaction" section |

---

## [1.0.2.6+build.50] — 2026-04-24

### Teleportation Improvements — Cooldown/Warmup Feedback, Dashboard Controls & Language Keys

Comprehensive teleportation improvements across all managers, the web dashboard, and language files.

**Changes:**

| File | Change |
|---|---|
| `en_us.json` | Added missing `commands.neoessentials.teleport.misc.back_warmup` and `commands.neoessentials.teleport.misc.back_cooldown` message keys (were referenced in Java but absent from the language file). |
| `permissions_nodes.txt` | Added documentation for all 10 cooldown/warmup bypass permission nodes (`neoessentials.teleport.bypass.cooldown`, `neoessentials.teleport.bypass.warmup`, plus per-command variants for home/warp/spawn/back). |
| `TeleportEndpoint.java` | New REST endpoint (`GET/PUT /api/teleport/settings`) — reads and writes all teleportation configuration sections (general, home, warp, spawn, back/misc) from config.json, then triggers a live reload of all four teleport managers. |
| `MiscTeleportManager.java` | Added `public reload()` method so the dashboard endpoint can reload config without a server restart. |
| `DashboardAPI.java` | Registered `/api/teleport` context backed by `TeleportEndpoint`; added import. |
| `DashboardFileManager.java` | Added `teleport.html` and `teleport.js` to the managed dashboard files list. |
| `teleport.html` | New dashboard page — five settings sections (General, Home, Warp, Spawn, Back) with number inputs, toggles, and Save/Reload buttons. |
| `teleport.js` | Client-side JS for the new teleport settings page: loads settings via `GET /api/teleport/settings`, populates form, POSTs changes with validation. |
| `index.html`, `admin.html`, `permissions.html` | Added "🌀 Teleport Settings" nav link (admin-only). Script `?v=` cache-bust query bumped to `419`. |

**Summary of all teleportation improvements implemented across prior builds (build.36–build.50):**
- Chunk pre-loading (3×3 grid) before every teleport via `TeleportUtil.preloadChunksForTeleport()`.
- Safety flag bypass (`enableHomeSafety=false` correctly skips location validation).
- Warmup countdown messages for home, warp, spawn, and /back with `enableTeleportWarmup` guard.
- Cooldown enforcement for all teleport commands with `ConcurrentHashMap`-based tracking.
- Global bypass permissions: `neoessentials.teleport.bypass.cooldown` / `.bypass.warmup`.
- Per-command bypass permissions for home, warp, spawn, back.
- Startup logging in `HomeManager`, `WarpManager`, `SpawnManager`, `MiscTeleportManager`.
- Back command cooldown (`miscSettings.backCooldown`), warmup message, and persistence across restarts.
- Dashboard teleport settings page for live, in-game config changes without server restart.

---

## [1.0.2.6+build.46] — 2026-04-24

### Bug Fix — Web Dashboard Admin Controls & Permissions Page Blank After Login

Navigating to `admin.html` or `permissions.html` after logging in on `index.html` would show a
blank page. Pressing F5 would briefly reveal the buttons before they disappeared again.

**Root causes:**

1. **`showLoginScreen()` has no redirect on sub-pages.**  
   On `index.html`, `showLoginScreen()` hides `dashboardWrapper` and shows `loginContainer`.
   On `admin.html` and `permissions.html` there is no `loginContainer`, so only `dashboardWrapper`
   was hidden — leaving the user with a completely blank page and no way to log back in.
   If anything caused the auth check or any subsequent `fetchWithAuth` call to fail (expired
   session, server reload clearing in-memory sessions, transient network error), the page went
   blank silently. The brief flash visible on hard-refresh was the HTML rendering before the
   async auth check completed.

2. **`permissions.js` never initialised on `permissions.html`.**  
   The init guard at the bottom of `permissions.js` checked
   `window.location.hash === '#permissions'` or
   `document.querySelector('[data-page="permissions"].active')`.
   Neither condition is ever true on the standalone `permissions.html` page, so
   `initPermissionSystem()` was never called — all permission tabs showed their "Loading…"
   placeholder forever.

3. **Multiple `fetchWithAuth` calls in `permissions.js` were missing `.json()`.**  
   `viewGroupPermissions`, `editUserPermissions`, `submitEditGroup`, `addGroupPermission`,
   `removeGroupPermission`, `deleteGroup`, `addUserPermission`, `removeUserPermission`, and
   `submitChangeGroup` all called `fetchWithAuth(...)` and then checked `response.success` or
   read `response.group` etc. directly on the raw `Response` object (which has no `.success`
   property). Every modal action silently failed instead of succeeding.

4. **Username not shown in topbar on sub-pages.**  
   `showDashboard()` only looked for `id="usernameDisplay"` (exists on `index.html`).
   `admin.html` and `permissions.html` use `id="userName"`, so the username was always "Guest"
   on those pages.

**Changes:**

| File | Change |
|---|---|
| `dashboard.js` | `showLoginScreen()`: when `loginContainer` is absent (sub-pages), redirect to `index.html` instead of just hiding `dashboardWrapper`. |
| `dashboard.js` | `showDashboard()`: username display now tries `id="usernameDisplay"` first, then falls back to `id="userName"`, so the topbar shows the correct username on both index and sub-pages. |
| `dashboard.js` | Version string bumped to Build 418 (cache-bust). |
| `permissions.js` | Replaced the unreliable hash/data-page init guard with `tryInitPermissions()` — checks for `id="permOverviewTab"` which is always present on `permissions.html`. |
| `permissions.js` | Added `.json()` parsing to the raw `Response` in `viewGroupPermissions`, `editUserPermissions`, `submitEditGroup`, `addGroupPermission`, `removeGroupPermission`, `deleteGroup`, `addUserPermission`, `removeUserPermission`, and `submitChangeGroup`. |
| `admin.html`, `permissions.html`, `index.html` | Script `?v=` cache-bust query bumped to `418`. |

---

## [1.0.2.6+build.44] — 2026-04-24


### Bug Fix — `/sethome` and `/delhome` Confirmation Buttons Append "confirm" to Home Name

Clicking the `[Confirm]` button on a `/sethome <name>` overwrite prompt or a `/delhome` deletion
prompt failed with *"Invalid home name: Colony confirm"*. Each subsequent click appended another
`" confirm"`, producing names like *"Colony confirm confirm confirm"*. The action never completed.

#### Root cause

The `confirm` and `deny` literals were registered as Brigadier **child nodes of the `<name>`
word-argument** (`/sethome <name> confirm`). The confirmation button's `RUN_COMMAND` click event
sent `/sethome Colony confirm`. In Minecraft 1.21+, when the client processes a `RUN_COMMAND` string
it re-validates the command against the client-side Brigadier tree that the server sent via
`ClientboundCommandsPacket`. The client-side tree does not correctly represent the nested literal
structure, so the full remaining input `"Colony confirm"` is consumed as a single word-argument
value. The server then receives `"Colony confirm"` as the `name` argument, `setHome()` rejects it
(space not allowed), the confirmation prompt is re-shown with `"Colony confirm"` as the new pending
name, and the loop repeats on every click.

**Changes:**

| File | Change |
|---|---|
| `HomeCommands.java` | Moved `confirm` and `deny` from being Brigadier children of `<name>` to **top-level literal siblings** under `sethome`/`createhome` and `delhome`/`deletehome`/`removehome`/`rhome`. Brigadier gives literals priority over argument nodes, so `/sethome confirm` routes to the handler while `/sethome Colony` routes to the name argument. |
| `HomeCommands.java` | Updated `executeSetHomeConfirm`, `executeSetHomeDeny`, `executeDelHomeConfirm`, `executeDelHomeDeny` to retrieve the pending home name from the server-side pending map instead of parsing it from command arguments. Removed all `StringArgumentType.getString(context, "name")` calls from the four handlers. |
| `HomeCommands.java` | Updated `executeSetHome` and `executeDelHome` to emit clean buttons (`/sethome confirm` / `/sethome deny`, `/delhome confirm` / `/delhome deny`) instead of embedding the home name in the button command. |
| `en_us.json` | Fixed `delete_success`, `delete_cancelled`, `delete_failed`, `overwrite_success`, `overwrite_failed` to use `{0}` (valid `MessageFormat` pattern) instead of `{HOME}`/`{home}` (were silently unsubstituted). Added `overwrite_already_pending`, `no_pending_overwrite_generic`, `delete_already_pending`, `delete_no_pending_generic`, `delete_no_confirm_required`, `limit_exceeded` keys. |
| `MessageUtil.java` | `CURRENT_LANG_VERSION` bumped `11 → 12`; new keys are auto-merged into existing server language files on next startup. |

---

## [1.0.2.6+build.42] — 2026-04-24

### Bug Fix — `/back` Fails with "No Safe Teleport Location Found" in Unloaded Chunks

Using `/back` (to return to a death point or previous location) failed whenever the target
was in an unloaded chunk, producing the error *"No safe teleport location found"* even when
the destination was perfectly valid.

#### Root causes:

1. **`TeleportUtil` only force-loaded the single target chunk.** `findSafeLocation()` scans
   up to ±16 blocks in X/Z from the target, which can cross into neighbouring chunks.
   Those neighbouring chunks were never loaded, so every candidate position inside them
   returned `false` from `level.isLoaded(pos)` → `isSafe()` = false →
   `findSafeLocation()` returned `null` → teleport failed.

2. **`MiscTeleportManager.teleportDelay` was hardcoded to `3`.** The field was never
   populated from config (`teleportation.backSettings.teleportDelay` /
   `teleportation.generalSettings.teleportDelay`), so the configured warm-up delay was
   silently ignored for all `/back` and implicit death-back teleports.

**Changes:**

| File | Change |
|---|---|
| `TeleportUtil.java` | Added `preloadChunksForTeleport(ServerLevel, BlockPos)` — loads a 3×3 chunk grid (target + 8 neighbours) with `PORTAL` tickets before any `isSafe()` / `findSafeLocation()` call runs. Second call added after `findSafeLocation()` resolves to ensure the safe-landing chunk is also loaded. All `teleportPlayer()` paths benefit automatically. |
| `ConfigManager.java` | Added `getBackTeleportDelay()` (reads `teleportation.backSettings.teleportDelay`, falls back to `generalSettings.teleportDelay`, default 3), `isDeathBackEnabled()`, and `isTeleportBackEnabled()`. |
| `MiscTeleportManager.java` | Added `loadConfig()` — reads `teleportDelay`, `enableDeathBack`, and `enableTeleportBack` from `ConfigManager`; called at construction so config values are honoured from the first use. |

---

## [1.0.2.6+build.41] — 2026-04-24

### Bug Fix — Vanish Module Cannot Be Disabled

Disabling the vanish module via `moderation.vanishSettings.enableVanishSystem: false` had no effect.
Commands remained registered and interaction prevention kept blocking previously-vanished players even
after the flag was set.

#### Root causes:

1. **Wrong config path in `ConfigManager.isVanishSystemEnabled()`** — the method checked for
   `enableVanishSystem` at the root of `config.json`, but the key lives at
   `moderation.vanishSettings.enableVanishSystem`. The root-level key was never present, so the method
   *always* returned `true`.

2. **`ModerationEventHandler` vanish interaction guards did not check `isVanishSystemEnabled()`** —
   even with the config flag corrected, players who were already vanished would still have block-break /
   block-place / item-use interactions cancelled because the guards only checked
   `isVanishPreventInteractionEnabled()`, not whether the vanish *system* was enabled.

3. **`VanishManager.onPlayerJoin()` was never called** — the method that restores a vanished player's
   tab-list hidden state on reconnect and sends the "you are vanished" reminder was defined but had no
   call-site. Re-join behaviour was therefore broken regardless of whether vanish was enabled.

**Changes:**

| File | Change |
|---|---|
| `ConfigManager.java` | Fixed `isVanishSystemEnabled()` to read `moderation.vanishSettings.enableVanishSystem` instead of root-level `enableVanishSystem` |
| `ModerationEventHandler.java` | Added `isVanishSystemEnabled()` guard to all three vanish interaction-prevention blocks (`onPlayerRightClick`, `onBlockBreak`, `onBlockPlace`) |
| `ModerationEventHandler.java` | Added `VanishManager.onPlayerJoin()` call in `onPlayerLogin`, gated by `isVanishSystemEnabled()`, so vanish state is correctly restored and the vanish reminder is shown on reconnect |

---



### Security Fix — `/inv` and `/ec` Bypass Permission Checks

`/inv` and `/ec` (aliases for `/invsee` and `/enderchest`) were accessible by **all** players regardless of
permission because they were registered as Brigadier `redirect()` nodes with no `requires()` predicate.
Brigadier does **not** re-evaluate the redirect target's `requires()` for the alias node itself —
only the alias's own predicate is checked at dispatch time. Since the aliases had none, every player
could open any other player's inventory.

**Changes:**

| File | Change |
|---|---|
| `InventoryViewCommands.java` | Replaced all `redirect()`-based aliases (`/inv`, `/ec`, `/ecedit`) with full command registrations that include their own `requires()` predicate |
| `InventoryViewCommands.java` | Fixed typo: `"enderchestdit"` → `"enderchestedit"` (prevented `/ecedit` from working) |
| `InventoryViewCommands.java` | Replaced hardcoded message strings with proper `MessageUtil` translation key calls |
| `permissions.json` | Added `neoessentials.invsee` and `neoessentials.enderchest` to the `moderator` group |
| `en_us.json` | Added `commands.neoessentials.invsee.*` and `commands.neoessentials.ec.*` message keys |

**Permission nodes:**

| Node | Description | Default group |
|---|---|---|
| `neoessentials.invsee` | View another player's inventory (read-only) | moderator |
| `neoessentials.invsee.edit` | View and edit another player's inventory | admin |
| `neoessentials.enderchest` | View another player's ender chest (read-only) | moderator |
| `neoessentials.enderchest.edit` | View and edit another player's ender chest | admin |

---

## [1.0.2.6+build.38] — 2026-04-24


### Bug Fix — Teleportation Message Keys & Cooldown/Warmup System

Fixes two related teleportation issues reported on build 1.0.2.6+21.

#### Fix 1 — Raw Translation Keys Displayed to Players

Previously, teleportation messages related to `/spawn` (and its fallback path to world spawn) would
show raw translation key strings like `commands.neoessentials.teleport.spawn.fallback_success` in
chat instead of the correct localized message. This happened because the entire
`commands.neoessentials.teleport.spawn.*` key group was missing from `en_us.json`, and
`MessageUtil.localize()` returns the raw key when no entry is found.

**Keys added to `en_us.json`:**
- `teleport.spawn.success`, `teleport.spawn.fallback_success`, `teleport.spawn.failed`, `teleport.spawn.fallback_failed`
- `teleport.spawn.cleared`, `teleport.spawn.set`, `teleport.spawn.info`, `teleport.spawn.info_not_set`
- `teleport.spawn.invalid_location`, `teleport.spawn.no_nether`, `teleport.spawn.no_end`
- `teleport.spawn.unsafe`, `teleport.spawn.unsafe_location`, `teleport.spawn.moved_to_safety`
- `teleport.spawn.critical_failure`, `teleport.spawn.distance_exceeded`
- `teleport.spawn.cooldown`, `teleport.spawn.warmup`
- `teleport.warp.cooldown`, `teleport.warp.warmup`
- `teleport.home.warmup`

`_langVersion` bumped from `10` → `11`; `CURRENT_LANG_VERSION` constant updated in `MessageUtil.java`.
Existing server deployments will auto-merge all new keys on the next startup without overwriting user edits.

#### Fix 2 — Teleport Cooldowns & Warmup Delays Not Applied

Multiple root causes prevented cooldowns and warmup delays from working:

| Manager | Problem | Fix |
|---|---|---|
| `HomeManager` | `teleportDelay` hardcoded to `3`, never read from config | Now reads `teleportation.generalSettings.teleportDelay` |
| `HomeManager` | `homeTeleportCooldownSeconds` read from config but never checked | Cooldown check added in `teleportToHome()` with `lastHomeTeleportTimestamps` |
| `WarpManager` | `warpCooldown` config key ignored — no use-cooldown enforcement | Added `warpUseCooldown` field + `lastWarpUseTimestamps`, enforced in `teleportToWarp()` |
| `SpawnManager` | `spawnCooldown` config key ignored — no cooldown enforcement | Added `spawnCooldownSeconds` field + `lastSpawnTimestamps`, enforced in `teleportToSpawn()` |
| `SpawnManager` | `loadSpawn()` read `teleportDelay: 0` from spawn.json, overriding config.json value | Removed `teleportDelay` from spawn.json loading; now driven exclusively by `generalSettings.teleportDelay` |
| All managers | No warmup countdown message shown to players | Warmup message sent when `teleportDelay > 0` and `enableTeleportWarmup=true` |

**Config keys now fully wired up:**
- `teleportation.generalSettings.teleportDelay` — warmup delay for home/spawn teleports (default: `3` seconds)
- `teleportation.homeSettings.homeTeleportCooldown` — home use cooldown (default: `5` seconds)
- `teleportation.warpSettings.warpCooldown` — warp use cooldown (default: `10` seconds)
- `teleportation.spawnSettings.spawnCooldown` — spawn use cooldown (default: `5` seconds)
- `teleportation.generalSettings.enableTeleportWarmup` — whether to show countdown message (default: `true`)

---



### Feature — Permissions System Improvements (Part 2): GUI, External Systems & Fine-Grained Control

Completes the Permissions System Improvements milestone with three remaining items.

#### GUI Management — Web Dashboard REST API (extended)

The existing `/api/permissions` endpoint has been extended with full support for contextual
permissions, temporary permissions, aliases, and a reload action.

**New endpoints added to `/api/permissions`:**

| Method | Path | Description |
|---|---|---|
| `POST` | `/reload` | Reload all permissions from disk |
| `GET` | `/system/status` | Enhanced — now includes emergency mode, adapter health, adapter version, consecutive failures, alias count |
| `GET/POST/DELETE` | `/group/{name}/context` | Manage group contextual overrides (`{contextKey, node, allow}`) |
| `GET/POST` | `/group/{name}/temp` | List / add group temp permissions (`{node, duration}`) |
| `DELETE` | `/group/{name}/temp/{node}` | Remove group temp permission |
| `GET/POST/DELETE` | `/user/{name}/context` | Manage user contextual overrides |
| `GET/POST` | `/user/{name}/temp` | List / add user temp permissions |
| `DELETE` | `/user/{name}/temp/{node}` | Remove user temp permission |
| `GET` | `/aliases` | List all registered permission aliases |
| `POST` | `/aliases` | Register alias `{alias, canonical}` — persists to `permission_aliases.json` |
| `DELETE` | `/aliases/{alias}` | Remove alias |

All endpoints return `{success: true/false, message?, ...}` JSON and require Bearer auth.

#### Integration with External Systems — Improved Documentation & Fallback

- **Compatibility report** documented: logged at every startup with adapter name, version, health, and a `⚠ NEWER THAN TESTED` warning when the installed version exceeds last-tested.
- **Full fallback chain** documented: emergency mode → OP bypass → external adapter → internal `permissions.json` → vanilla-OP fallback. Adapter health tracking (5 consecutive failures → `UNHEALTHY`, fallback activates) documented.
- **LuckPerms**: context-aware check via live `QueryOptions` documented; step-by-step setup guide added.
- **FTB Ranks**: 4-API-signature probe for version compatibility documented.
- **Compatibility table** added: LuckPerms 5.4.x, FTB Ranks 2101.1.3, WorldEdit (any), FTB Chunks (any), any NeoForge-`PermissionAPI` mod.

#### Fine-Grained Command Control — Per-Subcommand Permission Nodes

Every Brigadier branch in every NeoEssentials command tree has its own permission node.
Documented with per-system tables covering:

- **Home**: `.home`, `.home.set`, `.home.delete`, `.home.list`, `.home.others`
- **Warp**: `.warp`, `.warp.others`, `.warp.create`, `.warp.delete`, `.warp.list`
- **Kit**: `.kits.use`, `.kits.<name>`, `.kit.others`, `.kits.admin.create/delete`, `.kitreset`, `.kitreset.others`
- **Economy**: `.balance`, `.balance.others`, `.pay`, `.pay.offline`, `.economy.eco`
- **Moderation**: `.ban`, `.banip`, `.tempban`, `.jail`, `.jail.timed`, `.vanish`, `.vanish.others`
- **Permission system**: full sub-node table for every `/permissions` action

Full tables in [PermissionSystem.md — Fine-Grained Command Control](docs/Wiki/PermissionSystem.md#fine-grained-command-control).

---

## [1.0.2.6+build.26] — 2026-04-01

### Feature — Permissions System Improvements

Complete overhaul of the permissions subsystem with contextual overrides, condition expressions, a clean mod-interop API, alias resolution, and persistent storage of all new data.

#### Contextual Permissions

Grant or deny a permission node only when the player is in a specific world, time-of-day, or gamemode. Contextual rules are layered on top of the regular permission resolution chain — context denies always win, context grants are checked before regular grants.

```
/permissions group <group> context add <contextKey> <node> allow|deny
/permissions group <group> context remove <contextKey> <node>
/permissions group <group> context list

/permissions user <player> context add <contextKey> <node> allow|deny
/permissions user <player> context remove <contextKey> <node>
/permissions user <player> context list
```

Supported context keys (with tab-completion):

| Key | Meaning |
|---|---|
| `world:overworld` / `world:the_nether` / `world:the_end` | Current dimension |
| `time:day` | Day phase (ticks 0–12 999) |
| `time:night` | Night phase (ticks 13 000–23 999) |
| `gamemode:survival` / `gamemode:creative` / `gamemode:spectator` / `gamemode:adventure` | Player gamemode |

#### Permission Conditions

Optional runtime conditions can be attached to any permission node on a user or group. When the permission would otherwise be granted, the condition is re-evaluated; if it fails, the grant is withheld.

Condition syntax:
```
time:day
gamemode:survival AND time:day
world:overworld OR world:the_nether
health:above:10
op:true
```

Supports `AND` / `OR` compound expressions with atoms: `time:day`, `time:night`, `world:<name>`, `gamemode:<mode>`, `health:above:<n>`, `health:below:<n>`, `op:true`, `op:false`.

#### Permission Aliases

Map legacy or short node names to their canonical NeoEssentials equivalents via `config/neoessentials/permission_aliases.json`. Aliases are resolved transparently in every permission check.

Example `permission_aliases.json`:
```json
{
  "essentials.fly": "neoessentials.fly",
  "essentials.warp": "neoessentials.teleport.warp",
  "efly": "neoessentials.fly"
}
```

#### API for Other Mods — `PermissionsService`

Other NeoForge mods can now interact with NeoEssentials permissions without importing internal classes:

```java
PermissionsService perms = NeoEssentialsAPI.getPermissionsService();

// Simple check
boolean canFly = perms.hasPermission(player, "neoessentials.fly");

// Context-aware check
PermissionContext ctx = perms.contextFor(player);
boolean granted = perms.hasPermission(player.getUUID(), "mymod.feature", ctx);

// Register your mod's own nodes (appear in /permissions search)
perms.registerPermission("mymod.feature", "Enables the feature");
perms.registerPermissions(Map.of("mymod.a", "...", "mymod.b", "..."));

// Register a legacy alias
perms.registerAlias("essentials.fly", "neoessentials.fly");
```

Full method list: `hasPermission`, `getGroup`, `getPrefix`, `getSuffix`, `registerPermission`, `registerPermissions`, `registerAlias`, `getAliases`, `isEmergencyMode`, `isUsingExternalAdapter`, `getGroupNames`, `getPlayerPermissions`, `contextFor`.

#### Storage

Contextual permissions and conditions are now persisted in `permissions.json` (groups) and `permissions/playerdata.json` (users). Existing files are backward-compatible — no migration required.

#### Audit Log

New action constants written to `permissions_audit.log`:
`USER_CONTEXT_PERM_ADDED`, `USER_CONTEXT_PERM_REMOVED`, `GROUP_CONTEXT_PERM_ADDED`, `GROUP_CONTEXT_PERM_REMOVED`, `USER_CONDITION_SET`, `USER_CONDITION_REMOVED`, `GROUP_CONDITION_SET`, `GROUP_CONDITION_REMOVED`

#### New Permission Nodes

| Node | Default | Description |
|---|---|---|
| `neoessentials.permissions.user.context` | OP only | Manage contextual overrides for users |
| `neoessentials.permissions.group.context` | OP only | Manage contextual overrides for groups |

#### Internal changes

- `PermissionManager.hasPermission(UUID, String, PermissionContext)` — new context-aware overload; the existing `hasPermission(UUID, String)` delegates to it with `PermissionContext.EMPTY`
- `PermissionAPI.hasPermission(UUID, String, PermissionContext)` — context threaded through the full 5-step resolution chain; alias resolution runs before every check
- `PermissionStorage` — groups and users now save/load `contextualPermissions` and `conditions`
- `NeoEssentialsAPI.API_VERSION` bumped to `1.1.0`

---

## [1.0.2.6+build.23] — 2026-04-01 · [`48763856`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/48763856)

### New Feature — Permission Audit Logging

#### `PermissionAuditLogger` — persistent append-only audit trail
- **Added** `PermissionAuditLogger.java` — writes every permission modification to `neoessentials/permissions_audit.log` (append-only, UTF-8).
- **Log format:** `[YYYY-MM-DD HH:mm:ss UTC]  ACTION                   | executor=<name> | target=<group/player> | <detail>`
- **17 tracked events:**

  | Action constant | Trigger |
  |---|---|
  | `USER_GROUP_SET` | `/permissions user <p> setgroup <g>` |
  | `USER_PERM_ADDED` | `/permissions user <p> add <node>` |
  | `USER_PERM_REMOVED` | `/permissions user <p> remove <node>` |
  | `USER_PERMS_CLEARED` | `/permissions user <p> clear` |
  | `GROUP_CREATED` | `/permissions create group <g>` |
  | `GROUP_DELETED` | `/permissions delete group <g>` |
  | `GROUP_RENAMED` | `/permissions rename group <old> <new>` |
  | `GROUP_CLONED` | `/permissions clone group <src> <new>` |
  | `GROUP_PERM_ADDED` | `/permissions group <g> add <node>` |
  | `GROUP_PERM_REMOVED` | `/permissions group <g> remove <node>` |
  | `GROUP_PERMS_CLEARED` | `/permissions group <g> clear` |
  | `GROUP_INHERIT_ADDED` | `/permissions group <g> inherit add <p>` |
  | `GROUP_INHERIT_REMOVED` | `/permissions group <g> inherit remove <p>` |
  | `GROUP_PREFIX_SET` | `/permissions group <g> setprefix <v>` |
  | `GROUP_SUFFIX_SET` | `/permissions group <g> setsuffix <v>` |
  | `GROUP_PRIORITY_SET` | `/permissions group <g> setpriority <v>` |
  | `PERMISSIONS_RELOADED` | `/permissions reload` |

- **Executor tracking:** Commands run by online players log the player's name; console commands log `"CONSOLE"`.
- **Controlled by** `permissions.auditLogging` in `config.json` (default `true`). When `false`, all log calls are no-ops with zero overhead.

#### New config key: `permissions.auditLogging`
- **Added** to the default `config.json` template (`"auditLogging": true`).
- `ConfigManager.isPermissionAuditEnabled()` public method added.

---

## [1.0.2.6+build.22] — 2026-04-01 · [`a2e1a7ed`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/a2e1a7ed)

### Improvement — Permission Groups & Priorities + Permission Suggestions

#### Group priorities (`priority` field on every group)
- **Added** `priority` (int, default `0`) field to `PermissionGroup`. Higher values are checked **first** when resolving inherited groups, giving a deterministic order when multiple parent groups conflict.
- **Updated** `PermissionManager` — inherited groups are now sorted by `priority` descending before the recursive permission walk, for both positive-grant and negative-deny passes.
- **Updated** `PermissionStorage` — `priority` is saved/loaded in `permissions.json` (backwards-compatible: files without the key read as `0`).
- **Added** Two new commands:
  - `/permissions group <name> setpriority <value>` (−999–999) — requires `neoessentials.permissions.group.modify`
  - `/permissions group <name> getpriority` — requires `neoessentials.permissions.info.group`
- **Updated** `/permissions info group <name>` now shows the current priority in its output.
- **Updated** `/permissions debug <player>` group-chain display already renders priorities via `showGroupChain` (priority shown in group info line).
- **Added** `neoessentials.permissions.group.priority` registered in `PermissionRegistry` (description: *"Set/get group priority (used to order inheritance resolution)"*).

#### Permission Suggestions — enriched denial messages
- **Improved** `PermissionValidator.validatePermission()` denial message now looks up the required node in `PermissionRegistry` and appends its human-friendly description in a dimmed line:
  ```
  You don't have permission to use this command.
  §7Required: §fneoessentials.moderation.ban
  §8(Ban a player from the server)
  ```
- **Improved** `PermissionValidator.validateAnyPermission()` denial message similarly appends per-node descriptions for each candidate node listed.
- This makes it possible for players/staff to immediately understand *which capability* they are missing without needing to cross-reference the wiki.

#### Documentation
- **Updated** `PermissionSystem.md`:
  - New **Group Priorities** section with command table, how-it-works explanation, priority scale table, and a worked example.
  - Updated Table of Contents to include the new section.
  - Updated the example `groups.json` — all four groups (`default` 0, `vip` 10, `moderator` 50, `admin` 100) now include their `priority` field.
  - Updated the "if denied" description to show the enriched message format.
- **Updated** `CommandsReference.md` — added `setpriority` and `getpriority` rows to the Permissions Management table.

---

## [1.0.2.6+build.21] — 2026-04-01 · [`81c7a55d`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/81c7a55d)

### New Feature — Permission Debugging Tools

#### `/permissions debug <player>` — full permission resolution trace
- **Added** New `debug` subcommand to `/permissions` (requires `neoessentials.permissions.debug`).
- Displays a complete diagnostic trace for any player without needing to enable debug logging:
  - **System mode** — Internal, External adapter name, or EMERGENCY (OP-only)
  - **Adapter health** — healthy / UNHEALTHY (with consecutive failure count) and detected version
  - **Active config flags** — `opsBypassPermissions` and `vanillaOpFallback` on/off
  - **OP status** — checks live `ServerPlayer` (online) or `ProfileCache` (offline)
  - **Assigned group** and every **direct user permission** node (up to 10, with overflow count)
  - **Group inheritance chain** — recursive tree with indentation, up to 8 permissions per group with overflow count, prefix shown inline
  - **Resolution chain summary** — numbered 4-step walkthrough showing which step would GRANT or continue for this specific player, based on current config and OP status
- Result: admins can diagnose "why does player X not have permission Y" entirely in-game without touching logs.

#### `neoessentials.permissions.debug` — new permission node
- **Added** Registered in `PermissionRegistry` between the existing `check` and `search` nodes.

#### Bug fix — `checkUserPermission` full-chain bypass
- **Fixed** `checkUserPermission()` inside `PermissionsCommand` was calling `PermissionAPI.getManager().hasPermission(uuid, node)` directly, which silently bypassed:
  - `opsBypassPermissions` fast-path
  - The external adapter (LuckPerms / FTB Ranks)
  - `vanillaOpFallback` last resort
- **Fixed** Now calls `PermissionAPI.hasPermission(uuid, node)` — the full 5-step resolution chain — so that the in-game `/permissions user check` result is consistent with what actually happens at runtime.

---

## [1.0.2.6+build.19] — 2026-04-01 · [`a22d0323`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/a22d0323)

### Documentation — `allowUnsafeCommands` & Security Configuration

#### `SplitConfigs.md` — Security Configuration section (complete rewrite)
- **Fixed** The previous `allowUnsafeCommands` entry had the **wrong description** ("Allow enchantments and item operations beyond vanilla limits") — that describes `items.unsafe-enchantments`, not the security command filter.  Corrected to accurately reflect what the option does.
- **Added** Full `security.json` / `config.json → security` reference table covering all six keys: `enableInputValidation`, `maxCommandLength`, `maxReasonLength`, `allowUnsafeCommands`, `enablePathTraversalProtection`, `enableXSSProtection`.
- **Added** Detailed `allowUnsafeCommands` breakdown including:
  - Every blocked substring (with explanations for each category: destructive ops, code-execution, path traversal, shell operators, URL injection, reflection).
  - The character allowlist (`A-Z a-z 0-9 _ - / (space) : . & # ~`) and which common characters fall outside it (`@`, `{`, `%`, `=`, `!`, etc.).
  - Explicit call-out that **tilde (`~`) is blocked** even though it's used for Minecraft relative coordinates — the most common cause of the confusing error message.
  - Table of commands that **work by default** and table of commands that need `allowUnsafeCommands: true`.
  - Step-by-step instructions for enabling in both split-config and monolithic mode, with `/neoe reload` reminder.
  - Security recommendation: restrict `neoessentials.item.powertool` to trusted staff when enabling.

#### `ItemManagement.md` — Powertool Command Safety Filter section (new)
- **Added** "Command Safety Filter" subsection directly below the powertool how-it-works bullets.
- Shows the exact error messages players receive when a command is blocked.
- Quick-reference tables of commands that work vs. commands that need `allowUnsafeCommands: true`.
- Config path for both split and monolithic mode, with `/neoe reload` shortcut.
- Cross-link to the full Security Configuration section in `SplitConfigs.md`.

#### `CommandsReference.md` — Powertool note (new)
- **Added** Callout block beneath the `/powertool` / `/pt` rows explaining the command filter, the most common blocked patterns (`~`, `@`, `{`), and where to set `allowUnsafeCommands: true`.

#### `Home.md` — Getting started key files (updated)
- **Added** `security.json` to the getting-started key config files list with a brief description.

---

## [1.0.2.6+build.18] — 2026-04-01 · [`4c534da6`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/4c534da6)

### New Feature — Fallback to Vanilla OP Permissions

#### New config key: `permissions.vanillaOpFallback`
- **Added** `vanillaOpFallback` (default `true`) in the `permissions` config section.  
  Unlike `opsBypassPermissions` (which runs *before* any permission check), this new option runs *after* every system has been consulted. If the external adapter **and** the internal manager both returned `false` for an OP player, NeoEssentials grants access as a last-resort safety net.
- **Purpose:** Prevents admin lockouts when FTB Ranks crashes, `permissions.json` is corrupted, or the external adapter becomes unhealthy at runtime.
- **Distinction from `opsBypassPermissions`:**

  | Setting | When it fires | Typical use |
  |---|---|---|
  | `opsBypassPermissions: true` | *Before* any check — OPs skip the permission system entirely | Fast-path for small/trusted servers |
  | `vanillaOpFallback: true` | *After* all checks return `false` — OPs get in only when everything else fails | Strict environments using LuckPerms/FTB Ranks that still need a lockout-prevention net |

#### Emergency mode on permission-system startup failure
- **Added** `PermissionAPI.setEmergencyMode(true)` is now activated when `PermissionSystem.initialize()` encounters an unrecoverable exception at server start, **instead of** crashing the server with a `RuntimeException`.
- In emergency mode every permission check immediately answers `true` for OPs and `false` for everyone else. A prominent boxed `ERROR` is logged at startup and on every check, prompting the admin to fix the config and run `/neoe reload`.
- **Added** `/neoe reload` now detects emergency mode and performs a **full re-initialisation** (resets manager, adapter, flags) so the system can recover without a restart once the root cause is fixed.
- **Added** `PermissionSystem.isEmergencyMode()` public accessor (useful for dashboard status displays).

#### Documentation
- **Updated** `PermissionSystem.md` config table: added `vanillaOpFallback` row with description and a comparison table explaining the difference between bypass and fallback modes.
- **Updated** "How Permissions Work" section: now lists all five steps in order (emergency → bypass → external → internal → fallback) with explanations.

---

## [1.0.2.6+build.17] — 2026-04-01 · [`4d5cf1a1`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/4d5cf1a1)

### Improvements — External Permissions Integration

#### Version Detection & Compatibility Reports
- **Added** `FtbRanksAdapter` and `LuckPermsAdapter` now read the mod version via `ModList` on construction and log it at `INFO` level (e.g. `FTB Ranks detected — version: 2101.1.3`).
- **Added** `AdapterCompatibilityChecker` — a new startup utility that scans the mod list and emits a formatted compatibility table at `INFO`/`WARN` level, showing each detected permission mod, its installed version, the last-tested version, and a ✓/⚠ status.  Generated in both internal-mode and external-mode startup paths.
- **Added** If the detected FTB Ranks version differs from the last-tested minor line, a prominent boxed `WARN` is emitted advising admins to watch for permission issues and report the version mismatch.

#### Multi-Strategy API Probe (FTB Ranks)
- **Improved** `FtbRanksAdapter` now probes **four** known API signatures in order instead of two:
  1. `FTBRanksAPI.getPermission(ServerPlayer, String, boolean)` — current 2101.1.x
  2. `instance.hasPermission(UUID, String)` — older builds via `INSTANCE`/`getInstance()`
  3. `FTBRanksAPI.hasPermission(ServerPlayer, String)` — possible future static variant
  4. `FTBRanksAPI.checkPermission(ServerPlayer, String)` — alternative naming / forks
- **Added** When all four strategies fail, a boxed error is logged including the detected FTB Ranks version, so it's immediately clear why permission checks will fall back.

#### Health Tracking & Fallback to Internal System
- **Added** `ExternalPermissionAdapter` interface gains three default methods (source-compatible — no changes needed to existing adapters):
  - `getVersion()` — returns the detected mod version string
  - `isHealthy()` — returns `false` once consecutive runtime failures exceed the threshold (default 5)
  - `getConsecutiveFailures()` — exposes the failure counter
- **Added** Both `FtbRanksAdapter` and `LuckPermsAdapter` implement `isHealthy()` / `getConsecutiveFailures()` via an `AtomicInteger` failure counter.  On each successful permission check the counter resets to 0.
- **Improved** `PermissionAPI.hasPermission()` now checks `externalAdapter.isHealthy()` before delegating. If the adapter is unhealthy **or** throws during a check, execution falls through to the **internal `permissions.json` manager** and then, as a last resort, to the OP-bypass check — so non-OP players are never locked out solely because an external permission mod is misbehaving.
- **Added** A single `WARN` is emitted on the 5th consecutive failure naming the adapter and its version, asking the admin to fix the issue and run `/neoe reload`.

---

## [1.0.2.6+build.16] — 2026-04-01 · [`c1cc26fa`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/c1cc26fa)

### New Features — Rules Command

#### Console Feedback on Load Failures
- **Improved** `loadRulesData()` now logs a prominent boxed error to the console whenever `rules_data.json` is corrupt or unreadable, including the absolute file path, the exact parse error, and step-by-step remediation instructions.
- **Improved** When no rules file exists at all, a clear `INFO` log is emitted with the auto-generated file path and quick-start editing instructions (`/rules add`, `/rules edit`, direct JSON edit + `/rules reload`), replacing the previous silent fall-through.

#### Auto-Generation of `rules_data.json`
- **Confirmed** `rules_data.json` is always written on first startup with 10 sensible default rules when neither `rules_data.json` nor the legacy `rules.json` is present. The generated file path is now logged so admins know exactly where to find it.

#### `/neoe reload` Now Reloads Rules
- **Added** `RulesCommand.reload()` is now called by `/neoe reload`, so server rules are refreshed alongside all other systems without a restart.

#### Dashboard API — `/api/rules`
- **Added** `RulesEndpoint` (`/api/rules`) providing full CRUD for server rules from the web dashboard, protected by Bearer-token auth:
  - `GET /api/rules` — list all rules with 1-based index
  - `POST /api/rules` — replace full rule list `{"rules": [...]}`
  - `POST /api/rules/add` — append a single rule `{"rule": "..."}`
  - `PUT /api/rules/{n}` — edit rule at position *n* `{"rule": "..."}`
  - `DELETE /api/rules/{n}` — delete rule at position *n*
  - `POST /api/rules/reload` — reload from disk without restart

#### Documentation
- **Added** Full `/rules` section in `docs/Wiki/UtilitySystems.md` covering: command table, colour codes, data-file format and location, console feedback examples, dashboard API table, and legacy migration note.
- **Fixed** Three rows (`/rules`, `/helpop`, `/suicide`) that were accidentally merged into the MOTD dashboard API table — they are now in their own sections.
- **Improved** `RulesCommand` now uses `ResourceUtil.getConfigPath()` for file paths (consistent with every other data file in the mod).

---

## [1.0.2.6+build.15] — 2026-04-01 · [`e3bb4dd2`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/e3bb4dd2)

### Bug Fixes — Split Configuration System

#### Root Cause Fixed: Split Files Never Created on Fresh Installs
- **Fixed** `createSplitConfigsFromJar()` silently failing for every split file because it looked for pre-split files (e.g. `main.json`, `chat.json`) directly inside the mod JAR — but only the monolithic `config.json` exists in the JAR. All split-file creation on fresh servers would produce 0 files with no error.  
  Fix: `createSplitConfigsFromJar()` now loads the JAR's `config.json`, then extracts each section group into the correct target file using the new `FILE_SECTIONS_MAP`.

#### Root Cause Fixed: `main.json` Overwritten to Single Section
- **Fixed** `ensureSplitConfigsUpToDate()` iterating over *section entries* instead of *file entries*. When three sections (`modules`, `logging`, `permissions`) all map to `main.json`, the file was written three times — each time with only one section — leaving only the last section on disk.  
  Fix: the loop now iterates `FILE_SECTIONS_MAP` (file → all its sections) and writes each file exactly once containing all required sections.

#### New: `economy` Section Added to Split Configs
- **Fixed** The `economy` config section (currency symbol, starting balance, sell multiplier, etc.) was present in `config.json` but missing from `CONFIG_FILE_MAP` and therefore never extracted into any split file. It is now mapped to `main.json`.

### New Features — Split Configuration System

#### Validation & Repair Commands
- **Added** `/neoe config validate` — checks all split config files for missing files, parse errors, and missing sections; prints a clear list of every problem with remediation instructions.
- **Added** `/neoe config repair` — automatically regenerates missing files from the JAR default and fills missing sections into existing files without overwriting user-set values.
- **Added** `/neoe config status` — dashboard-style overview showing each expected file with a ✔/✘ indicator and its section list; reports overall health.

#### Clear Error Messages on Missing Files
- When a split config file cannot be regenerated at startup, a prominent boxed error is now logged:
  ```
  ╔══════════════════════════════════════════════════════════╗
  ║  MISSING SPLIT CONFIG: chat.json
  ║  This file should contain: chat
  ║  Run: /neoe config repair   to regenerate all missing files.
  ╚══════════════════════════════════════════════════════════╝
  ```

#### `FILE_SECTIONS_MAP` — Authoritative File Layout
- Added `ConfigSplitter.FILE_SECTIONS_MAP` (public, `LinkedHashMap<String, List<String>>`): the single source of truth for which sections each split file must contain. Used by generation, validation, repair, and the status command.

#### Documentation
- **Added** `docs/Wiki/SplitConfigs.md` — full reference covering: file layout table, migration guide, fresh-install behaviour, health-check commands with example output, automatic startup checks, version tracking, disabling split configs, and a complete command reference.

---

## [1.0.2.6+build.12] — 2026-04-01 · [`1cebc781`](https://github.com/ZeroG-Network-Org/NeoEssentials/commit/1cebc781)


### New Features

#### MOTD System — Multi-Profile & Rotation
- **Added** `MotdManager` singleton (`com.zerog.neoessentials.util.motd.MotdManager`) to own all MOTD state, replacing the static fields that lived inside `MotdCommand`.
- **Added** named **MOTD profiles** — each profile has its own `motd`, `author`, and `timestamp`. The active profile name is persisted in `config/neoessentials/motd_data.json`.
- **Added** automatic **legacy migration** — single-MOTD `motd_data.json` files from previous builds are seamlessly promoted to the multi-profile format on first load with no data loss.
- **Added** `/motd profile list|create|delete|switch|info` subcommands (permission: `neoessentials.motd.profile`).
- **Added** `/motd rotation enable <minutes>|disable|next` subcommands — enables a background daemon thread that cycles through all profiles on a configurable interval (permission: `neoessentials.motd.rotation`).
- **Added** `MotdEndpoint` (`/api/motd`) dashboard REST endpoint supporting full CRUD for profiles, rotation control, and broadcast — all protected by the existing Bearer-token auth middleware.
- **Added** clear **in-game error feedback** for both save and load failures: `/motd reload` now shows the exact I/O error in-game instead of silently resetting to blank.
- **Added** two new permission nodes: `neoessentials.motd.profile` and `neoessentials.motd.rotation` (both default `false`).
- **Added** `MotdManager.shutdown()` called from `DashboardLifecycleManager.onServerStopping` to cleanly terminate the rotation scheduler thread.

#### Dashboard API
- `DashboardAPI.registerEndpoints()` now registers `/api/motd` via `withAuth(new MotdEndpoint(server))`.

### Improvements
- Lang file updated with new keys: `motd.load_error`, `motd.save_error`, `motd.profile.*`, `motd.rotation.*`. Removed orphaned `motd.empty` duplicate.
- `PermissionRegistry` updated with descriptive docs for all six `neoessentials.motd.*` nodes.
- `UtilitySystems.md` and `CommandsReference.md` wikis updated with full MOTD command/API reference.

---
