# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## [1.0.2.5+build.1057] — 2026-03-05

### Added — ChestShop System (full port from ChestShop-3)
- **Sign-based player & admin shops** — place a chest, put a sign next to it, done
- **Sign format:**
  - Line 1: owner name (blank → auto-assigned to placing player)
  - Line 2: quantity per trade (1–3456)
  - Line 3: price — `B 10`, `S 5`, `B 10:S 5`, `B FREE`, K/M suffix support
  - Line 4: item name or `?` for right-click autofill
- **`?` autofill** — write `?` on line 4, right-click the sign holding an item to assign it; sign shows `§e§l?` until assigned
- **Blank owner line** — auto-assigns creating player's name; sign coloured `§b` (aqua)
- **Admin Shops** — write `Admin Shop` on line 1; unlimited stock, no chest required; coloured `§2` (dark green)
- **Transactions** — right-click = BUY (money flows buyer→owner), left-click = SELL (money flows owner→seller); uses `EconomyManager` with full balance checks and rollback safety
- **Break protection** — only owner or `neoessentials.shop.admin.remove` can break a shop sign or its chest
- **`ShopManager`** — singleton with `ConcurrentHashMap` in-memory store, persisted to `neoessentials/shops.json` with atomic-move writes
- **`ShopParser`** — validates all 4 sign lines; item resolution via `WorthManager.resolveItem()` then vanilla registry fallback
- **`ShopTransaction`** — uses `Container` interface for chest access (compatible with all chest types); full inventory space checks; partial-rollback on failure
- **`ShopInteractHandler`** — NeoForge `PlayerInteractEvent` + `BlockEvent.BreakEvent` wired to buy/sell/remove
- **`ShopSignHandler`** — deferred tick-check queue detects sign text finalization without a sign-update event (NeoForge 1.21.1 limitation)
- **Commands:** `/chestshop list [player]`, `/chestshop info`, `/chestshop convert`, `/chestshop remove <x> <y> <z>`, `/chestshop reload` (alias `/cshop`)
- **Permissions:** `neoessentials.shop.create`, `shop.create.admin`, `shop.use`, `shop.list.others`, `shop.admin.remove`, `shop.admin.reload`
- **Integration:** economy (`EconomyManager`), permissions (`PermissionAPI` → LuckPerms/FTBRanks), item prices (`WorthManager`), config (`ConfigManager`)

### Added — Vault API (full NeoForge implementation)
- **`NeoEssentialsEconomy`** — `VaultEconomy` implementation backed by `EconomyManager`; `format()` uses live `getCurrencySymbol()`; fires `EconomyDepositEvent`/`EconomyWithdrawEvent` on every transaction; `createPlayerAccount()` uses `ConfigManager.getEconomyStartingBalance()`
- **`NeoEssentialsChat`** — `VaultChat` implementation; `getPlayerPrefix/getSuffix` routes through `PermissionAPI.getPrefix/getSuffix()` (honours LuckPerms → FTBRanks → internal); group operations through `PermissionManager`/`PermissionStorage`
- **`NeoEssentialsPermission`** — `VaultPermission` implementation; `playerHas()` → `PermissionAPI.hasPermission()` (external adapters respected); write ops via `PermissionManager` + `PermissionStorage`
- **`VaultManager`** — initialises/shuts down all three providers; lifecycle hooked into server start/stop
- **`/vault info`** — shows which Vault providers are active at runtime

### Fixed — Vault Economy integration gaps
- `currencyNameSingular/Plural()` now fetches from `EconomyManager.getCurrencySymbol()` at runtime (was hardcoded)
- `depositPlayer/withdrawPlayer` properly use `EconomyManager.addBalance/subtractBalance` with atomic semantics
- Events fired on all Vault-originated transactions so dashboard stats and transaction history are accurate

### Fixed — Vault Chat integration gaps
- `getPlayerPrefix/getSuffix` was going directly to internal `PermissionUser`, bypassing LuckPerms/FTBRanks; now correctly routes through `PermissionAPI`

---

## [1.0.2.5+build.937] — 2026-03-04

### Fixed — Build errors
- `ModerationEventHandler` — `LivingAttackEvent` import updated to correct NeoForge 1.21.1 package; `TeleportCommandEvent` replaced with correct `EntityTeleportEvent` subclass
- `JailCommand` — `MailCommand.parseDuration()` made `public` so `JailCommand` can call it cross-package
- `SellCommand` — `ItemStack.hasCustomHoverName()` replaced with `ItemStack.getHoverName().getString().isEmpty()` check (NeoForge 1.21.1 API)
- `PlayerStateCommands` — `setFallDistance()` replaced with `resetFallDistance()`; `Abilities.flyingSpeed/walkingSpeed` set via accessor methods; `setArmor()` replaced with `setItem(EquipmentSlot, ItemStack)`; `StatsCounter.getValue()` updated to correct `Stat<?>` overload
- `ServerAdminCommands` — `GameProfile.getId()` call updated; `PlayerList.getSingleplayer()` removed (no such method); `Iterable<ServerLevel>.stream()` replaced with `StreamSupport.stream()`; `awardRecipes()` updated to accept `Collection<RecipeHolder<?>>`; `Item.getId()` replaced with `BuiltInRegistries.ITEM.getKey().getPath()`
- `UtilityCommands` — `EntityType` capture wildcard resolved; all deprecated `finalizeSpawn()` usages suppressed
- `PlayerInfoCommands` — `player.latency` field access replaced with `player.connection.latency()`; `EconomyManager` import corrected; `VanishCommand.isVanished()` made `public`; lambda capture of non-final variable fixed
- `WorldInteractionCommands` — `Entity.addPassenger()` access fixed via reflection wrapper
- `FunCommands` — lambda capturing mutable `rules` string fixed with effectively-final copy
- `config.json` — duplicate keys (`msgtoggle`, `tpo`, `near`, `motd`, `realname`, `rules`, `seen`, `suicide`, `whois`, `condense`) deduplicated; JSON syntax error at line 133 corrected

---

## [1.0.2.5+build.910] — 2026-03-04

### Fixed — Console spam reduction
- `LuckPermsAdapter.getPrefix()` — 20+ `LOGGER.info()` diagnostic lines demoted to `LOGGER.debug()`
- `PermissionAPI.getPrefix()` — `LOGGER.info()` calls replaced with `LOGGER.debug()`
- `ChatDebugUtil` — per-message `LOGGER.info()` calls demoted to `LOGGER.debug()`
- `ChatHandler` — Discord relay `LOGGER.info("[DEBUG]…")` blocks demoted to `LOGGER.debug()`
- `MessageUtil` — per-startup translation diagnostic lines demoted to `LOGGER.debug()`; single summary kept at INFO
- `ServerDataCollector` — stats collection banner demoted to `LOGGER.debug()`
- `GameEndpoint`, `PlayerEndpoint`, `LoggingEndpoint` — per-HTTP-request INFO lines demoted to `LOGGER.debug()`
- `ListCommand` — redundant debug gate removed

---

## [1.0.2.5+build.908] — 2026-03-03

### Added — Player Info & Admin Tools
- `/seen`, `/near`, `/ping`, `/playtime`, `/whois`, `/realname`, `/sudo`, `/suicide`, `/msgtoggle`, `/rtoggle`, `/motd`, `/rules`
- `ConfigManager.getMotd()` / `getRules()` — reads from `general.motd` / `general.rules` in `config.json`

### Added — World Interaction & Fun commands
- `/fireball` (11 types), `/tree`, `/bigtree`, `/break`, `/ice`, `/bottom`, `/tpaall`, `/broadcastworld`

### Added — Home & Warp Enhancements
- `/renamehome`, `/warpinfo`, `/world`, `/spawner`, `/recipe`, `/tpauto`
- `TeleportRequestManager` now checks tpauto state before sending request

### Added — Item Customisation & Miscellaneous
- `/me`, `/tptoggle`, `/gc`, `/lightning`, `/skull`, `/itemname`, `/itemlore`, `/remove`, `/loom`, `/cartography`

### Added — Utility Commands
- `/ptime`, `/pweather`, `/effect`, `/spawnmob`, `/unlimited`, `/condense`

### Added — Server Admin Commands
- `/broadcast`, `/time`, `/weather`, `/kill`, `/gamemode` (full), `/tpo`, `/tpohere`, `/tpoffline`

### Added — Player State / Admin Tool Commands
- `/fly`, `/god`, `/heal`, `/feed`, `/speed`, `/ext`, `/burn`, `/give`, `/more`, `/hat`, `/exp`
- `GodModeEventHandler` — `LivingDamageEvent.Pre` cancels damage; session tracking on login/logout

### Added — Economy Enhancements
- `/eco reset`, percent amounts (`10%`), offline `/pay`, async `/baltop` with pagination, total economy wealth, `baltop.exempt` permission, IgnoreManager check in pay

### Added — Jail System Enhancements
- Timed jails (`/jailfor`), `/deljail`, full event enforcement (respawn redirect, teleport intercept, interact/attack block)
- `JailEntry.expireAt`, `JailManager.checkJailTimeout()`, optimised per-second tick scan

### Added — Kit System Enhancements
- `/kit <name> <player>` (give to others), `/kitreset`, clean paginated list, console support, recipient notification
- `KitManager.resetCooldown()`, `resetAllCooldowns()`, `getRemainingCooldownPublic()`

### Added — Warp System Enhancements
- `/warp <name> <player>` (warp others), `/warp` shows paginated list, per-warp permissions, `/warps [page]` pagination, console `/delwarp`, NPE fix

### Added — Mail System Enhancements
- `/mail sendtemp`, `sendall`, `sendtempall`, `clearall`, `clear <index>`, `clear <player>`
- Mute/ignore checks, rate limiting, console support, 1000-char limit, expired mail auto-cleanup, login notification

### Added — Worth & Sell System
- `WorthManager`, `/worth`, `/sell hand|inventory|all|item`, `/setworth`, sell multiplier, named-item protection

### Fixed — Permission System
- ~50 unregistered permission nodes across all systems now registered
- `MODERATION` category added to `PermissionCategory` enum
- Permission denial messages now show required node
- `PermissionValidator` updated for all deny variants

### Fixed — Teleportation
- `isSafe()` uses `getCollisionShape().isEmpty()` instead of `canOcclude()`
- Dangerous block list (lava, fire, magma, cactus, etc.) added
- `findSafeLocation()` now does top-down column scan first
- `TeleportRequestManager` finds nearest safe spot instead of blocking
- Double-safety pass removed from `HomeManager`/`WarpManager`

### Fixed — AFK System
- `AfkManager.loadConfiguration()` now called on server start
- Anti-AFK score thresholds raised; decay bug fixed
- `@EventBusSubscriber` annotation added to `AfkMovementDetector`
- AFK broadcasts use `Component.literal()` instead of broken `MessageUtil.info(key)`
- `/afk` command sends personal confirmation to executing player

### Fixed — Chat Logging
- Chat messages now appear in server console via `server.sendSystemMessage()`
- `LOGGER.info()` added per channel type
- `logChatToConsole` config option added (default `true`)

### Added — Languages
- `fr_fr.json`, `de_de.json`, `es_es.json`, `pt_br.json`, `zh_cn.json`, `nl_nl.json`, `pl_pl.json`, `ru_ru.json`
- `deployBundledLanguageFiles()` deploys and merges lang files on every server start
- Broken colour codes in `en_us.json` TPR/teleport keys fixed

### Added — Random Teleport (`/tpr`)
- Full port of EssentialsX `RandomTeleport` — equally-distributed offsets, nether-aware Y detection, world-border clamping, async pre-computation cache, configurable attempts/cooldown/biome exclusions, named RTP locations, `/settpr`

### Added — Dashboard Improvements
- Dashboard files versioned — newer JAR versions auto-update deployed HTML/JS/CSS
- Admin controls and permissions split into `admin.html` / `permissions.html`
- Auth system overhauled — offline login support, `/dashboard register` command, Simple Discord Link integration (optional)
- Rich text (gradients/rainbow) pipeline fixed
- `/home` and `/warp` safe-teleport bypass config flag fixed

### Added — PowerTool Fix
- PowerTool commands now keyed on item identity (NBT/type) instead of inventory slot index

---

## [1.0.2.3] — initial public baseline

- Initial release with core economy, chat, moderation, teleportation, kit, warp, web dashboard, permission system, and item management.

