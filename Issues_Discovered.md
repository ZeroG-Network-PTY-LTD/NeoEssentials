# 👾 Issues That Were Discovered


---

# ✅ Issues That Were Fixed

- **Economy integration — ChestShop system missing: sign-based player shops, admin shops, item autofill, auto-assign owner**
  *(Fixed: 2026-03-05)*

  **Root causes:** No chest shop system existed at all. The `🎯 Additional Features` section listed it as a wanted feature.

  **Implemented from ChestShop-3 (Bukkit plugin), converted to NeoForge:**

  | Component | Details |
  |---|---|
  | `ShopData.java` | Data model — owner UUID/name, quantity, buy/sell prices, item ID, sign pos, chest pos, `itemPending` flag |
  | `ShopManager.java` | Singleton, `ConcurrentHashMap` in-memory store, persisted to `neoessentials/shops.json` with atomic-move writes |
  | `ShopParser.java` | Validates all 4 sign lines; blank line 0 auto-assigns player name; `?` on line 4 creates pending shop; item resolution via `WorthManager.resolveItem()` then vanilla registry; K/M price suffix support |
  | `ShopTransaction.java` | BUY (right-click) and SELL (left-click) flows; uses `Container` interface for chest access; balance checks; rollback on failure |
  | `ShopInteractHandler.java` | `PlayerInteractEvent.RightClickBlock` → BUY; `LeftClickBlock` → SELL; `BlockEvent.BreakEvent` → shop removal |
  | `ShopSignHandler.java` | Deferred tick-check queue (NeoForge has no sign-update event); detects sign text after player finishes editing; blank owner auto-assign |
  | `ShopCommand.java` | `/chestshop list [player]`, `info`, `convert`, `remove <x y z>`, `reload` — alias `/cshop` |

  **Sign format:**
  ```
  Line 1: owner name or blank (auto-assigns)
  Line 2: quantity (1-3456)
  Line 3: B 10:S 5 / B 10 / S 5 / B FREE / supports K/M suffixes
  Line 4: item name or ? (right-click with item to assign)
  ```

  **Integration:** `EconomyManager` (add/subtract balance), `PermissionAPI` (LuckPerms/FTBRanks respected), `WorthManager` (item resolution), `ConfigManager` (economy enabled check), `ResourceUtil` (shops.json path)

  **Permissions:** `neoessentials.shop.create`, `shop.create.admin`, `shop.use`, `shop.list.others`, `shop.admin.remove`, `shop.admin.reload`

- **Vault API — missing: Economy, Chat, and Permission Vault providers**
  *(Fixed: 2026-03-05)*

  **Root causes:** No Vault API implementation existed. Other mods using Vault could not hook into NeoEssentials economy or permissions.

  **Implemented:**

  | Component | Details |
  |---|---|
  | `NeoEssentialsEconomy` | `VaultEconomy` backed by `EconomyManager`; `format()` uses live `getCurrencySymbol()`; fires `EconomyDepositEvent`/`EconomyWithdrawEvent`; `createPlayerAccount()` uses `ConfigManager.getEconomyStartingBalance()` |
  | `NeoEssentialsChat` | `VaultChat`; `getPlayerPrefix/getSuffix` routes through `PermissionAPI.getPrefix/getSuffix()` (respects LuckPerms → FTBRanks → internal) |
  | `NeoEssentialsPermission` | `VaultPermission`; `playerHas()` → `PermissionAPI.hasPermission()` (external adapters respected); write ops via `PermissionManager`/`PermissionStorage` |
  | `VaultManager` | Initialises/shuts down all three providers; lifecycle hooked into server start/stop in `NeoEssentials.java` |

  **Fixed during audit:**
  - `currencyNameSingular/Plural()` now reads from `EconomyManager.getCurrencySymbol()` (was hardcoded)
  - `getPlayerPrefix/getSuffix` was going directly to internal `PermissionUser`, bypassing LuckPerms/FTBRanks — fixed to use `PermissionAPI`

- **Console Spam**: Reduce the console spam — `LuckPermsAdapter.getPrefix()` and `PermissionAPI.getPrefix()` were logging at INFO level on every chat message/prefix lookup.
  *(Fixed: 2026-03-04)*
  - `LuckPermsAdapter.getPrefix()`: All ~20 `LOGGER.info()` diagnostic lines (the full `=== LUCKPERMS PREFIX REQUEST ===` block) changed to `LOGGER.debug()`. These fired on every single prefix lookup.
  - `PermissionAPI.getPrefix()`: Removed manual `debugEnabled = ConfigManager.isDebugLoggingEnabled()` gate around `LOGGER.info()` calls. Replaced with plain `LOGGER.debug()` — consistent with the rest of the codebase and respects log level automatically.
  - `ChatDebugUtil.java`: Removed `ConfigManager.isDebugLoggingEnabled()` gate + `LOGGER.info()`. Now uses plain `LOGGER.debug()` — fires per chat message.
  - `ChatHandler.java`: Removed all `MessageUtil.isDebugMode()` gated `LOGGER.info("[DEBUG]…")` blocks in Discord relay section. Replaced with plain `LOGGER.debug()`.
  - `MessageUtil.java`: Demoted all per-startup diagnostic `LOGGER.info()` in `loadTranslations()` and `updateServerLanguageFile()` to `LOGGER.debug()`. Kept only the single summary line (`"NeoEssentials: loaded N translations"`) at INFO. Also demoted `syncDebugModeFromConfig()` banner.
  - `ServerDataCollector.java`: Demoted `"=== Collecting Server Statistics ==="` from `LOGGER.info()` to `LOGGER.debug()` — fires on every dashboard poll.
  - `GameEndpoint.java`, `PlayerEndpoint.java`, `LoggingEndpoint.java`: Demoted all per-HTTP-request `LOGGER.info()` (handling/collecting/success lines) to `LOGGER.debug()` — fired on every dashboard page load/refresh.
  - `ListCommand.java`: Removed redundant `MessageUtil.isDebugMode()` gate around `LOGGER.debug()` call — the debug level already suppresses it automatically.
  - **Result:** With `enableDebugLogging: false` (default), zero prefix/permission lines appear in console. With `enableDebugLogging: true`, full diagnostics still available at DEBUG level.

- **Commands Doc Update**: Update the commands document for all registered commands please.
  *(Fixed: 2026-03-02)*
  Created `docs/Wiki/CommandsReference.md` — a comprehensive reference covering all ~172 commands across 17 systems, with syntax, permission node, default access level, aliases, and description for every command. Added link as the first entry in `Home.md` wiki index.

- **Player Info & Admin Tools system — Missing entirely: /seen, /near, /ping, /playtime, /whois, /realname, /sudo, /suicide, /msgtoggle, /rtoggle, /motd, /rules**
  *(Fixed: 2026-03-02)*

  **Root causes:** All 12 commands were completely absent. `ConfigManager` had no `getMotd()`/`getRules()` methods.

  **Implemented in `PlayerInfoCommands.java` based on EssentialsX:**

  | Command | Perm | Description |
  |---|---|---|
  | `/seen <player>` | `neoessentials.seen` | Checks online list first (shows world/pos/ping). Falls back to `ProfileCache.get()` for offline players. |
  | `/near [radius]` | `neoessentials.near` | Iterates online players in same `ServerLevel`, computes `distanceToSqr()`, sorts by name, shows distance in metres. Default 200 block radius. |
  | `/ping [player]` | `neoessentials.ping(.others)` | Reads `player.latency`. Colour-coded green/yellow/red. |
  | `/playtime [player]` | `neoessentials.playtime(.others)` | Reads `Stats.CUSTOM.get(Stats.PLAY_TIME)` ticks → formatted h/m/s. |
  | `/whois <player>` | `neoessentials.whois` | Shows UUID, dimension, XYZ, gamemode, ping, health, food. |
  | `/realname <nick>` | `neoessentials.realname` | Searches online players by `getDisplayName().getString()` with colour stripping. |
  | `/sudo <player> <cmd>` | `neoessentials.sudo` | Respects `neoessentials.sudo.exempt`. Prefix `c:` to send chat. Runs via `player.createCommandSourceStack()`. |
  | `/suicide` | `neoessentials.suicide` | `player.hurt(damageSources().magic(), Float.MAX_VALUE)`. Broadcasts death message to all others. |
  | `/msgtoggle [on\|off]` | `neoessentials.msgtoggle(.others)` | Syncs with existing `MsgToggleManager` (name-based) used by `MsgCommand`, plus UUID shadow map for `isMsgBlocked()`. |
  | `/rtoggle [on\|off]` | `neoessentials.rtoggle(.others)` | Per-player `rtoggleEnabled` map. `isRtoggleEnabled()` available for `ReplyCommand` to check. |
  | `/motd` | `neoessentials.motd` | Reads `ConfigManager.getMotd()` → `general.motd` in config. Replaces `{player}` placeholder. |
  | `/rules` | `neoessentials.rules` | Reads `ConfigManager.getRules()` → `general.rules` in config. |

  **Additional:** `ConfigManager.getMotd()` + `getRules()` added. `general.motd` + `general.rules` added to `config.json`. 17 permission nodes, 18 lang keys, 12 commands registered in `NeoEssentials.java` + `config.json`. `PermissionSystem.md` + `CommandsReference.md` updated.

- **World Interaction & Fun system — Missing entirely: /fireball, /tree, /bigtree, /break, /ice, /bottom, /tpaall, /broadcastworld**
  *(Fixed: 2026-03-02)*

  **Root causes:** All 8 commands were completely absent from the codebase.

  **Implemented in `WorldInteractionCommands.java` based on EssentialsX (`Commandfireball`, `Commandtree`, `Commandbigtree`, `Commandbreak`, `Commandice`, `Commandbottom`, `Commandtpaall`, `Commandbroadcastworld`):**

  | Command | Perm | Description |
  |---|---|---|
  | `/fireball [type] [speed] [ride]` | `neoessentials.fireball.<type>` | Spawns typed projectile in look direction using NeoForge entity constructors. 11 types: fireball, small, large, arrow, skull, egg, snowball, expbottle, dragon, trident, windcharge. Optional `ride` mounts player on projectile. Per-type permission check + wildcard `neoessentials.fireball.*`. |
  | `/tree <type>` / `/bigtree` | `neoessentials.tree` | Raycasts 20 blocks, plants one above. Uses `level.registryAccess()` to resolve `CONFIGURED_FEATURE` by ResourceLocation key and calls `holder.place()`. 12 tree types mapped to vanilla feature keys. |
  | `/break` | `neoessentials.break` | Raycasts 20 blocks via `player.pick()`. Calls `level.destroyBlock(pos, false, player)` (no drops). Bedrock protected unless `neoessentials.break.bedrock`. |
  | `/ice [player]` | `neoessentials.ice(.others)` | Calls `target.setTicksFrozen(target.getTicksRequiredToFreeze() + 1)` to fully freeze via powder-snow mechanic. |
  | `/bottom` | `neoessentials.bottom` | Scans from `level.getMinBuildHeight()` upward looking for solid+air+air pattern. Saves `/back` location before teleport. |
  | `/tpaall [player]` | `neoessentials.tpaall(.others)` | Iterates all online players, checks tptoggle, calls `TeleportRequestManager.sendTeleportRequest()` with `TPAHERE` type for each eligible player. |
  | `/broadcastworld <msg>` / `/bcastworld` | `neoessentials.broadcastworld` | Filters online players by `p.serverLevel() == src.getLevel()`. Sends coloured `§6[World] §e<msg>`. |

  **Additional:** 13 permission nodes, 17 lang keys, all commands registered in `NeoEssentials.java` + `config.json`. `PermissionSystem.md` + `CommandsReference.md` updated.

- **Home & Warp Enhancement system — Missing entirely: /renamehome, /warpinfo, /world, /spawner, /recipe, /tpauto**
  *(Fixed: 2026-03-02)*

  **Root causes:** All 6 command groups were completely absent. `HomeManager` had no rename capability. `TeleportRequestManager.sendTeleportRequest()` had no tpauto check.

  **Implemented from scratch based on EssentialsX (`Commandrenamehome`, `Commandwarpinfo`, `Commandworld`, `Commandspawner`, `Commandrecipe`, `Commandtpauto`):**

  | Command | Perm | Description |
  |---|---|---|
  | `/renamehome <old> <new>` | `neoessentials.renamehome(.others)` | Renames a home atomically via `HomeManager.renameHome()`. Supports `player:homename` format for admin use. Validates name with existing `isValidHomeName()`. |
  | `/warpinfo <name>` | `neoessentials.warpinfo` | Shows warp coordinates and world via `WarpManager.getWarp()`. Tab-completes all warp names. |
  | `/world [name] [player]` | `neoessentials.world(.others)` | Lists all registered `ServerLevel` dimensions. Teleports to world spawn via `player.teleportTo()`. Matches by dimension path or full resource location key. |
  | `/spawner <mob>` | `neoessentials.spawner[.<mob>]` | Raycasts 6 blocks to find `Blocks.SPAWNER`. Sets entity type via `SpawnerBlockEntity.setEntityId()`. Per-mob perm `neoessentials.spawner.<mob>` or wildcard `neoessentials.spawner.*`. |
  | `/recipe [item]` | `neoessentials.recipe` | Scans all server recipes for result matching held/named item. Unlocks via `player.awardRecipes()`. Reports count of matched recipes. |
  | `/tpauto [on\|off] [player]` | `neoessentials.tpauto(.others)` | Per-player auto-accept state. `TeleportRequestManager.sendTeleportRequest()` now calls `HomeWarpEnhancementCommands.isTpAutoEnabled()` and immediately executes the teleport without sending a request if enabled. Warns if tptoggle is also off. |

  **Additional:** `HomeManager.renameHome()` method added. 11 permission nodes, 21 lang keys (incl. auto-accept keys), all commands registered in `NeoEssentials.java` + `config.json`. `PermissionSystem.md` + `CommandsReference.md` updated.

- **Item Customisation & Miscellaneous system — Missing entirely: /me, /tptoggle, /gc, /lightning, /skull, /itemname, /itemlore, /remove, /loom, /cartography**
  *(Fixed: 2026-03-02)*

  **Root causes:** All commands were completely absent. `/tptoggle` was registered in the command list but had no implementation, and `TeleportRequestManager.sendTeleportRequest()` had no tptoggle check.

  **Implemented from scratch based on EssentialsX (`Commandme`, `Commandtptoggle`, `Commandgc`, `Commandlightning`, `Commandskull`, `Commanditemname`, `Commanditemlore`, `Commandremove`):**

  | Command | Perm | Description |
  |---|---|---|
  | `/me <action>` | `neoessentials.me` | Broadcasts `§5* §dName §faction` to all players. |
  | `/tptoggle [on\|off] [player]` | `neoessentials.tptoggle(.others)` | Toggle tp-request acceptance. State stored in `ItemCustomisationCommands.isTpToggleAllowed()`. `TeleportRequestManager.sendTeleportRequest()` now checks this before sending — returns error unless sender has `neoessentials.teleport.tpo`. |
  | `/gc` / `/mem` | `neoessentials.gc` | Shows uptime (JMX), TPS (via `server.getAverageTickTimeNanos()`), used/total/max memory, loaded chunk count across all dimensions. |
  | `/lightning [player]` / `/smite` | `neoessentials.lightning(.others)` | Spawns `EntityType.LIGHTNING_BOLT` at look-target or named player. Essentials: `strikeLightning()`. |
  | `/skull [player]` | `neoessentials.skull` | Creates `PLAYER_HEAD` with `DataComponents.PROFILE` set from server profile cache (`ResolvableProfile(GameProfile)`). Falls back to random UUID + name. |
  | `/itemname [name\|-]` / `/rename` | `neoessentials.itemname` | Sets `DataComponents.CUSTOM_NAME` on held item. Omit or use `-` to clear. |
  | `/itemlore add\|set <n>\|remove <n>\|clear` | `neoessentials.itemlore` | Reads/writes `DataComponents.LORE` (`ItemLore`). Full add/set/remove/clear sub-commands. |
  | `/remove <type> [radius]` | `neoessentials.remove` | Removes entities in AABB-inflated radius. Types: all, items/drops, mobs, animals, monsters, arrows, xp, boats, minecarts, tnt, paintings. Never removes players. |
  | `/loom` | `neoessentials.loom` | Opens `LoomMenu` via `MenuProvider` + `ContainerLevelAccess`. |
  | `/cartography` / `/cartographytable` | `neoessentials.cartography` | Opens `CartographyTableMenu` via `MenuProvider` + `ContainerLevelAccess`. |

  **Additional:** 13 permission nodes, 14 lang keys (incl. `tptoggle_off` for tptoggle-blocked tpa). All commands registered in `NeoEssentials.java` + `config.json`. `PermissionSystem.md` + `CommandsReference.md` updated.

- **Utility Commands system — Missing entirely: /ptime, /pweather, /effect, /spawnmob, /unlimited, /condense**
  *(Fixed: 2026-03-02)*

  **Root causes:** All 6 command groups were completely absent.

  **Implemented from scratch based on EssentialsX (`Commandptime`, `Commandpweather`, `Commandpotion`, `Commandspawnmob`, `Commandunlimited`, `Commandcondense`):**

  | Command | Perm | Description |
  |---|---|---|
  | `/ptime [reset\|day\|noon\|night\|midnight\|<ticks>] [player]` | `neoessentials.ptime(.others)` | Per-player client-side time via `ClientboundSetTimePacket`. Restored on rejoin. |
  | `/pweather [reset\|sun\|storm\|clear\|rain] [player]` | `neoessentials.pweather(.others)` | Per-player weather via `ClientboundGameEventPacket`. Restored on rejoin. |
  | `/effect <player> <effect\|clear> [duration] [amp]` | `neoessentials.effect` | Applies `MobEffectInstance`. Supports all registry effect names. `/effect <player> clear` removes all. |
  | `/spawnmob <mob> [amount] [player]`, `/mob` | `neoessentials.spawnmob(.others)` | Spawns entities at player via `EntityType.create()` + `finalizeSpawn()`. Amount 1–100. |
  | `/unlimited [list\|clear\|<item\|hand>] [player]` | `neoessentials.unlimited(.others)` | Adds item to per-player unlimited set. `isUnlimited()` static method for event handler use. |
  | `/condense [item]` | `neoessentials.condense` | Converts loose items → storage blocks using 21 built-in rules (nugget→ingot→block pattern). |

  **Additional:** `GodModeEventHandler` updated to call `UtilityCommands.onPlayerJoin/Quit` for ptime/pweather restore on login and state cleanup on logout. 10 permission nodes, 19 lang keys, all commands registered in `NeoEssentials.java` + `config.json`. `PermissionSystem.md` updated.

- **Server Admin system — Missing entirely: /broadcast, /time, /weather, /kill, /gamemode (full), /tpo, /tpohere, /tpoffline**
  *(Fixed: 2026-03-02)*

  **Root causes:** All commands were either absent or only partially registered (gamemode only had gms/gmc/gma/gmsp shortcuts, no `/gamemode` command).

  **Implemented from scratch based on EssentialsX:**

  | Command | Perm | Description |
  |---|---|---|
  | `/broadcast <msg>` | `neoessentials.broadcast` | Server-wide coloured announcement. Aliases: `/bc`, `/announce`. |
  | `/time [set\|add] <value>` | `neoessentials.time(.set)` | Get time, set or add ticks. Named values: day/noon/sunset/night/midnight/sunrise. Aliases `/day`, `/night`. |
  | `/weather <sun\|storm\|thunder> [dur]` | `neoessentials.weather` | Sets weather on all sky-light worlds. Optional duration in seconds. Aliases `/sun`, `/storm`, `/thunder`. |
  | `/kill <player>` | `neoessentials.kill` | Kills player via `damageSources().genericKill()`. Respects `kill.exempt` + `kill.force`. |
  | `/gamemode <survival\|creative\|adventure\|spectator\|0-3> [player]` | `neoessentials.gamemode(.others)` | Full gamemode command with all modes + numeric shortcuts. |
  | `/tpo <player>` | `neoessentials.teleport.tpo` | Teleport to player ignoring their tptoggle setting. |
  | `/tpohere <player>` | `neoessentials.teleport.tpohere` | Bring player to sender ignoring tptoggle. Notifies target. |
  | `/tpoffline <player>` | `neoessentials.teleport.tpoffline` | Loads offline player NBT from world saves, teleports to their last recorded Pos/Dimension. |

  **Additional registrations:** 14 permission nodes, 16 lang keys, all commands in `NeoEssentials.java` + `config.json`. `PermissionSystem.md` updated with Server Admin section.

- **Player State / Admin Tool system — Missing entirely: /fly, /god, /heal, /feed, /speed, /ext, /burn, /give, /more, /hat, /exp, /sudo, /playtime**
  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX):**

  - All 13 commands were completely absent from the mod.

  **Implemented from scratch based on EssentialsX pattern:**

  | Command | Perm | Description |
  |---|---|---|
  | `/fly [player] [on\|off]` | `fly` / `fly.others` | Toggle flight. Clears fall distance. Resets flying when disabled. |
  | `/god [player] [on\|off]` | `god` / `god.others` | Toggle god mode. Restores health+hunger on enable. `GodModeEventHandler` cancels all damage. |
  | `/heal [player]` | `heal` / `heal.others` | Full health, full hunger, full saturation, clears all potion effects. Dead-player guard. |
  | `/feed [player]` | `feed` / `feed.others` | Full hunger + saturation. |
  | `/speed [walk\|fly] <0-10> [player]` | `speed` / `speed.others` | Maps 0–10 to Minecraft 0.0–1.0 speed. Auto-detects walk/fly from current state. |
  | `/ext [player]` | `ext` / `ext.others` | `clearFire()`. Alias `/extinguish`. |
  | `/burn <player> [seconds]` | `burn` | Sets fire ticks (seconds × 20). Default 10s. |
  | `/give <player> <item> [amount]` | `give` | Multi-stack distribution. Drops to ground if inventory full. |
  | `/more [amount]` | `more` | Sets held stack count to amount or max stack size. |
  | `/hat` | `hat` | Swaps held item into helmet slot, returns old helmet to hand. |
  | `/exp [show\|set\|give] [amount] [player]` | `exp` + sub-nodes | Show level+total XP. Set/give XP. Console + others support. |
  | `/sudo <player> <command>` | `sudo` | Runs command as target. Blocks if target has `sudo.exempt`. Prevents self-sudo. |
  | `/playtime [player]` | `playtime` / `playtime.others` | Uses `Stats.PLAY_TIME` ticks + current session ms. |

  **Additional files created:**
  - `GodModeEventHandler.java` — `LivingDamageEvent.Pre` cancels damage for god-mode players; `PlayerLoggedIn/Out` events track session start for playtime and clean up state on quit.
  - `PermissionCategory.PLAYER` — Added enum value to PermissionRegistry.
  - 26 permission nodes registered.
  - 33 lang keys added to `en_us.json`.
  - All commands added to `NeoEssentials.java` and `config.json` commands section.
  - `PermissionSystem.md` updated with full Player State section.

- **Worth/Sell system — Missing entirely: /worth, /sell hand|inventory|all|item, /setworth, WorthManager with price persistence**

  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Worth.java`, `Commandworth.java`, `Commandsell.java`):**

  - **Entire system was absent** — No `WorthManager`, no `/worth`, no `/sell`, no `/setworth` commands existed at all.

  **Implemented from scratch based on EssentialsX pattern:**

  | Component | Details |
  |---|---|
  | `WorthManager.java` | Singleton. Loads/saves `worth.json` (item registry ID → price). `getPrice(ItemStack)`, `setPrice()`, `removePrice()`, `getSellMultiplier()`, `isAllowSellNamedItems()`, `resolveItem(name)`. |
  | `/worth [item\|hand] [amount]` | Shows sell value of held item or named item × amount. Essentials: `itemWorth()`. |
  | `/sell hand [amount]` | Sells item in hand. Requires `neoessentials.sell.hand`. |
  | `/sell inventory\|all\|invent` | Sells all priced items in inventory. Skips named items if disabled. Requires `neoessentials.sell.bulk`. |
  | `/sell <item> [amount]` | Sells by item name/ID from inventory. |
  | `/setworth <item\|hand> <price>` | Admin: sets sell price. `hand` uses held item. Requires `neoessentials.setworth`. |
  | `/setworth <item\|hand> remove` | Admin: removes sell price. |
  | Sell multiplier | `economy.sellMultiplier` config (default `1.0`). Applied to all sell prices. Essentials: `getSettings().getMultiplier(user)`. |
  | Named item protection | `economy.allowSellNamedItems` config (default `false`). Essentials: `isAllowSellNamedItems()`. |
  | `economy` config section | Added `currencySymbol`, `startingBalance`, `sellMultiplier`, `allowSellNamedItems` to `config.json`. |
  | 5 permission nodes | `neoessentials.worth`, `sell`, `sell.hand`, `sell.bulk`, `setworth` registered. |
  | 13 lang keys | All `worth.*` and `sell.*` keys added to `en_us.json`. |
  | Commands registered | `worth`, `sell`, `setworth` added to `NeoEssentials.java` and `config.json` commands section. |

- **Kit system — Missing Essentials features: /kit others, /kitreset, clean list, console support, recipient notification, public cooldown API**
  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Commandkit.java`, `Commandkitreset.java`, `Kit.java`):**

  - **`/kit <name> <player>` (give to others) missing** — Essentials `Commandkit` checks `essentials.kit.others` and lets you specify a second player argument. Our command had no `target` argument.
  - **`/kitreset <kit> [player]` command missing entirely** — Essentials has a full `/kitreset` command that sets `user.setKitTimestamp(kitName, 0)`. We had no cooldown reset command at all.
  - **`/kit` (no args) showed wrong format** — Previous list display showed verbose info blocks per kit. Essentials shows a clean single-line per-kit list with cooldown status.
  - **Console support missing** — `KitCommand` blocked console entirely. Essentials allows console to run `/kit <name> <player>`.
  - **Recipient notification missing** — Essentials sends `kitReceive` to the target when given a kit by another player. Our command sent nothing to the recipient.
  - **Redundant double permission check** — Command checked permission, then called `canUseKit()` which checked it again, potentially sending two error messages for one denied action. Cleaned up to single check.
  - **`getRemainingCooldown` private** — KitCommand couldn't show per-kit cooldown status in the list because the method was private. Needed for list display and external access.
  - **`resetCooldown()` / `resetAllCooldowns()` methods missing** — No public API to reset a player's kit cooldown, required for `/kitreset`.
  - **3 new permission nodes missing** — `kit.others`, `kitreset`, `kitreset.others` unregistered.
  - **16 lang keys outdated** — Old keys used `{placeholder}` style instead of `{0}` MessageFormat style, missing new keys for list display, reset, others notifications.

  **Fixes applied:**

  | Area | Change |
  |---|---|
  | `/kit <name> <player>` | New `target` argument. Requires `neoessentials.kit.others`. Notifies recipient with `kits.received_from`. |
  | Console `/kit` | Console allowed when target arg present. Logs as "Console gave kit X to Y". |
  | `/kit` list (no args) | Clean format: per-kit single line with item count + cooldown status (Ready / Cooldown: Xm Ys). Filtered by player's permissions. |
  | `/kitreset <kit> [player]` | New `KitResetCommand.java`. Self-reset + others-reset. Notifies target. Registered in `KitCommands` + `NeoEssentials`. |
  | `getRemainingCooldownPublic()` | Public alias for private `getRemainingCooldown()`. Used by list display and future API. |
  | `resetCooldown(uuid, kit)` | New public method. Removes cooldown entry and saves. |
  | `resetAllCooldowns(uuid)` | New public method. Clears all cooldowns for a player. |
  | Permission nodes | Added: `kit.others`, `kitreset`, `kitreset.others`. |
  | Lang keys | Full rewrite with `{0}` MessageFormat args: `given`, `gave_to`, `received_from`, `list_header`, `list_entry`, `list_ready`, `list_cooldown`, `list_empty`, `reset_self`, `reset_other`, `reset_notify`, `console_needs_target`, `cannot_use`, `charge_failed`, `not_enough_money`. §colour-coded. |
  | PermissionSystem.md | Kits section updated with all new nodes and correct command associations. |

- **Warp system — Missing Essentials features: warp-others, per-warp permission, /warps pagination, /warp no-args list, deleteWarpByAdmin, console NPE fix**

  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Warps.java` / `Commandwarp.java`):**

  - **`/warp <name> <player>` missing** — Essentials supports warping another player with `essentials.warp.others`. Our command accepted only `<name>`.
  - **`/warp` (no args) didn't show list** — Essentials: `if (args.length == 0 || args[0].matches("[0-9]+"))` → show paginated warp list. Ours required a name and threw a syntax error.
  - **Per-warp permission (`neoessentials.warps.<name>`) missing** — Essentials has `getPerWarpPermission()` which checks `essentials.warps.<warpname>` per warp when enabled. Not wired in our command.
  - **`/warps [page]` pagination missing** — Essentials: `WARPS_PER_PAGE = 20`, shows `page/maxPages` header. Our `/warps` dumped all warps as a single blob.
  - **`/delwarp` used wrong permission** — Used `hasSetWarpPermission()` (create perm) instead of `PERMISSION_DELWARP`. Admin with delete-but-not-create permission couldn't delete warps.
  - **`/warps` NPE from console** — `executeWarps()` cast `getEntity()` to `ServerPlayer` unconditionally. Would NPE if run from console.
  - **No console `/delwarp` support** — `deleteWarp(ServerPlayer, String)` requires a player object. Console couldn't delete warps.
  - **All warp lang keys undefined** — `WarpManager` referenced 20+ lang keys (`warp.not_found`, `warp.created`, `warp.list_header`, etc.) but none were in `en_us.json`. Players would see raw key strings.
  - **`perWarpPermission` config option missing** — No config entry to enable/disable per-warp permissions.
  - **3 new permission nodes missing** — `warp.others`, `warp.list` (was registered but undocumented properly), `warps.*`.

  **Fixes applied:**

  | Area | Change |
  |---|---|
  | `/warp <name> <player>` | New variant. Requires `neoessentials.teleport.warp.others`. Teleports target, notifies sender. |
  | `/warp` (no args) | Now shows paginated warp list (page 1). Matches Essentials `args.length==0` behaviour. |
  | Per-warp permission | `isPerWarpPermissionEnabled()` added to ConfigManager. When `true`, `/warp <name>` checks `neoessentials.warps.<name>`. |
  | `perWarpPermission` config | Added `perWarpPermission: false` default to `warpSettings` in `config.json`. |
  | `/warps [page]` pagination | 20 per page, sorted case-insensitively. Shows `(N total, page X/Y)` header when multi-page. Filters by per-warp perms. |
  | `/delwarp` permission | Now correctly uses `PERMISSION_DELWARP` (`warp.delete`) not create perm. |
  | Console `/delwarp` | `deleteWarpByAdmin(String, String)` — new method in `WarpManager`. No `ServerPlayer` needed. |
  | `/warps` console NPE | `executeWarps` uses `source.getPlayer()` (nullable) not unchecked cast. |
  | 26 warp lang keys | All `commands.neoessentials.teleport.warp.*` keys added to `en_us.json`. Previously showed raw keys. |
  | Permission nodes | Added: `warp.others`, `warps.*`. Updated docs for `warp.list`. |
  | PermissionSystem.md | Warp section fully updated with all nodes, per-warp info, and correct command associations. |

- **Economy system — Missing Essentials features: /eco reset, percent amounts, offline pay, baltop async cache, pagination, total wealth, exempt players**

  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Commandeco.java`, `Commandpay.java`, `BalanceTopImpl.java`):**

  - **`/eco reset <player>` missing** — Essentials `EcoCommands` enum has `GIVE`, `TAKE`, `SET`, **`RESET`**. Our `EcoCommand` only had `give`, `take`, `set`. `reset` sets the player's balance back to `startingBalance` from config.
  - **`/eco give/take <player> <amount%>` missing** — Essentials supports percent-of-balance amounts (e.g. `eco take Steve 10%` takes 10% of Steve's current balance). Ported via `scaleByPowerOfTen(-2)` logic.
  - **`/eco give/take/set/reset` online-only** — `EcoPlayerUtil.getUUIDByName` existed but `ecoAdminAction` was already using it correctly; however notify-if-online messages were missing for `give`/`set`.
  - **`/pay` online-only** — `PayCommand` used `validateOnlinePlayer()` which rejected offline targets entirely. Essentials allows offline payment with `essentials.pay.offline` permission.
  - **`/pay` ignore check missing** — Essentials checks `player.isIgnoredPlayer(user)` in addition to `isAcceptingPay()`. If the online recipient was ignoring the sender, payment still went through. Fixed to check `IgnoreManager.isIgnoring()`.
  - **`/baltop` blocking sort every call** — `BaltopCommand.execute()` called `EconomyManager.getAllBalances()` and sorted inline, on the server thread, every time anyone ran `/baltop`. With many players this would stall the server.
  - **`/baltop` no pagination** — Only showed 10 entries with no way to see ranks 11+.
  - **`/baltop` no total wealth** — Essentials shows `balanceTopTotal` (sum of all balances) at the footer.
  - **`/baltop` no cache age** — No way to know if data was stale.
  - **`/baltop` exempt permission missing** — No `baltop.exempt` node; admins/NPCs could appear on the list.
  - **`/baltop` raw UUIDs in output** — `EconomyLeaderboard.formatLeaderboard()` used `entry.getKey()` (UUID string) not a resolved player name.
  - **3 new permission nodes missing** — `pay.offline`, `baltop.exempt`, `eco.eco` (reset alias) unregistered.

  **Fixes applied:**

  | Area | Change |
  |---|---|
  | `/eco reset <player>` | New subcommand. Sets balance to `ConfigManager.getEconomyStartingBalance()`. Notifies target if online. Logs to transaction history. |
  | `/eco give/take <player> <amount%>` | Percent support: detects `%` suffix, applies `current × (amount / 100)`. |
  | `/eco give/set` online notification | Notifies target player if online with `eco.received_give` / `eco.set_notify` message. |
  | `/pay` offline support | Resolves offline UUID from profile cache. Blocked unless sender has `neoessentials.economy.pay.offline`. |
  | `/pay` ignore check | If online recipient ignores sender, payment blocked with "not accepting payments" message (Essentials behaviour). |
  | `BaltopCommand` — full rewrite | Port of `BalanceTopImpl.calculateBalanceTopMapAsync()`: async `CompletableFuture`, thread-safe `CopyOnWriteArrayList`, `AtomicBoolean` cache lock. |
  | Cache auto-refresh | Cache rebuilt asynchronously when stale (>60 s) or empty. Never blocks server thread. |
  | `/baltop [page]` pagination | Default 10/page. Any page number supported. |
  | Total economy wealth | Footer line shows sum of all non-exempt balances. |
  | Cache age display | Header shows how many seconds ago data was calculated. |
  | Exempt players | `neoessentials.economy.baltop.exempt` permission skips player from ranking & total. |
  | Player name resolution | Profile cache lookup, falls back to UUID string if unresolvable. |
  | Cache invalidation | `BaltopCommand.invalidateCache()` called after every `eco give/take/set/reset` and `pay` to keep data fresh. |
  | Permission nodes | Added: `pay.offline`, `baltop.exempt`, `eco` (eco admin). Updated `pay` description. |
  | Lang keys | `eco.reset`, `eco.reset_notify`, `eco.received_give`, `eco.set_notify`, `eco.player_not_found`, `pay.offline_not_allowed`, `pay.player_not_found`, `baltop.empty`, `baltop.refreshing`, `baltop.total`. Updated header + entry formatting with §colours. |

- **Jail system — Missing Essentials features: timed jails, deljail, full event enforcement (respawn, teleport, interact, attack, gamemode)**
  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Jails.java` / `JailListener`):**

  - **Timed jails missing** — `JailEntry` had no `expireAt` field. No way to jail someone for "30 minutes" and have them auto-release. Essentials has `checkJailTimeout(currentTime)` called on join and periodically.
  - **`/jailfor` missing** — No timed-jail command. Essentials: `Commandtogglejail` uses `DateUtil.parseDateDiff`.
  - **`/deljail` missing** — No command to remove a jail location. Essentials: `Commanddeljail`.
  - **Interaction not blocked for jailed players** — `onPlayerRightClick` only checked freeze/vanish, never jail. Essentials: `onJailPlayerInteract` cancels `PlayerInteractEvent` unless `essentials.jail.allow-interact`.
  - **Attack not blocked for jailed players** — No `LivingAttackEvent` handler. Essentials: `onJailEntityDamageByEntity` cancels attacks by jailed players unless `essentials.jail.allow-attack`.
  - **Respawn not redirected to jail** — No `PlayerRespawnEvent` handler. Essentials: `onJailPlayerRespawn` (HIGHEST priority) redirects respawn location back to jail.
  - **Teleport not intercepted for jailed players** — No teleport event handler. Essentials: `onJailPlayerTeleport` (HIGH priority) overrides teleport destination back to jail. Our tick-based enforcement had escape windows.
  - **`onPlayerJoin` / `checkJailTimeout` never called from any event** — `JailManager.onPlayerJoin()` existed but was orphaned. `checkJailTimeout()` didn't exist at all.
  - **Tick-based movement check scanned ALL players every tick** — Extremely expensive. Should only check jailed players and only once per second.
  - **4 new bypass permission nodes missing** — `jail.allow-break`, `jail.allow-place`, `jail.allow-interact`, `jail.allow-attack` were unregistered.

  **Fixes applied:**

  | Area | Change |
  |---|---|
  | `JailEntry.expireAt` | New field. `0` = indefinite. Persisted to/from `jailed_players.json`. |
  | `JailManager.checkJailTimeout()` | New method. Checks if timed jail expired → auto-unjails. Returns `true` if released. |
  | `JailManager.jailPlayer(…, durationMillis)` | New overload. `0L` = indefinite (existing behaviour unchanged). Sets `expireAt`. |
  | `JailManager.formatDuration()` | New static helper. Formats millis as `2h 30m 15s`. |
  | `JailEntry.getFormattedRemaining()` | Returns remaining jail time or `"indefinite"`. |
  | `/jailfor <player> <jail> <duration> [reason]` | New command. Duration: `30s`, `5m`, `2h`, `1d`, `1w`. Reuses `MailCommand.parseDuration()`. |
  | `/deljail <name>` | New command. Warns if players were in that jail. |
  | `ModerationEventHandler` — full rewrite | Replaced 194-line file with complete Essentials port. |
  | `onPlayerLogin` | Calls `checkJailTimeout()` first → auto-release if expired. Then calls `onPlayerJoin()` to teleport to jail. |
  | `onPlayerRespawn` | Schedules 1-tick delayed teleport back to jail after respawn. |
  | `onPlayerTeleport` | Cancels `TeleportCommandEvent` for jailed players, redirects back to jail. |
  | `onPlayerMove` (dimension change) | Catches cross-dimension escapes via `PlayerChangedDimensionEvent`. |
  | `onPlayerRightClick` + `onPlayerRightClickBlock` | Cancels both for jailed players unless `neoessentials.jail.allow-interact`. |
  | `onLivingAttack` | Cancels attacks by jailed players unless `neoessentials.jail.allow-attack`. |
  | `onBlockBreak` / `onBlockPlace` | Now checks `allow-break` / `allow-place` bypass perms before cancelling. |
  | `onServerTick` | Replaced all-player per-tick scan → runs every 20 ticks (1s), skips non-jailed players, also calls `checkJailTimeout`. |
  | Permission nodes | Added: `jail.timed`, `deljail`, `jail.allow-break`, `jail.allow-place`, `jail.allow-interact`, `jail.allow-attack`. |
  | Lang keys | Added: `jail.message`, `jail.escape_prevented`, `jail.released_expired`, `jail.invalid_duration`, `jail.deljail_success`, `jail.deljail_had_inmates`. |

- **Mail system — Missing Essentials features: timed mail, sendall, clearall, mute/ignore checks, rate limiting, console support**

  *(Fixed: 2026-03-02)*

  **Root causes found (vs EssentialsX `Commandmail.java` / `MailServiceImpl.java`):**

  - **`sendtemp` missing** — No way to send expiring/timed mail. Essentials supports `sendtemp <player> <duration> <message>` where the mail auto-deletes when expired and shows an expiry timestamp.
  - **`sendall` / `sendtempall` missing** — Admins had no way to broadcast a mail to all players.
  - **`clearall` missing** — No admin command to wipe every player's mailbox.
  - **`clear <index>` and `clear <player>` missing** — Players couldn't delete a specific message by position; admins couldn't clear another player's mailbox. Only own full-clear existed.
  - **Mute check missing** — Muted players could still send mail. Essentials blocks muted users from sending.
  - **Ignore check missing** — If target ignored the sender, mail was still delivered. Essentials silently drops it.
  - **Rate limiting missing** — No per-minute throttle. Old code only had a 50-message hard cap.
  - **Console couldn't send** — `/mail send <player> <msg>` from console was blocked. Essentials allows it.
  - **`senderUUID` not stored** — Only sender name was saved; no UUID for future cross-reference.
  - **Message length cap was 200** — Essentials uses 1000 characters.
  - **Expired mail not cleaned on read** — Old timed messages stayed in the mailbox forever.
  - **Login notification not connected** — `hasUnreadMail()` existed but was never called on player join.
  - **5 missing permission nodes** — `mail.sendtemp`, `mail.sendall`, `mail.sendtempall`, `mail.clear.others`, `mail.clearall` were unregistered.

  **Fixes applied:**

  | Area | Change |
  |---|---|
  | `/mail sendtemp <player> <duration> <msg>` | New sub-command. Duration: `30s`, `5m`, `2h`, `1d`, `1w`. Shows expiry in read list. Expired msgs auto-purged on read. |
  | `/mail sendall <msg>` | Admin broadcast to all online players. Runs async. |
  | `/mail sendtempall <duration> <msg>` | Admin timed broadcast to all online players. |
  | `/mail clearall` | Admin wipe of all mailboxes. |
  | `/mail clear <index>` | Delete a specific message by 1-based index. |
  | `/mail clear <player> [index]` | Admin: clear another player's mailbox (whole or by index). |
  | Mute check | Muted players blocked from sending. Returns `§cYou are muted and cannot send mail.` |
  | Ignore check | If target ignores sender and both are online, mail is silently dropped (Essentials behaviour). |
  | Rate limiting | Configurable `mail.mailsPerMinute` in `config.json` (default 10). Atomic per-minute window. |
  | Console support | `/mail send <player> <msg>` works from server console (sender shown as "Console"). |
  | `senderUUID` field | Now stored alongside `senderName` in `mail_data.json`. |
  | Message length | Raised from 200 → 1000 characters (matches Essentials). |
  | Expired mail cleanup | `readMail()` removes expired messages before rendering, same as Essentials `iterator.remove()`. |
  | Login notification | `MailCommand.notifyOnLogin()` hooked into `PlayerJoinQuitHandler.onPlayerJoin()`. |
  | Backward compatibility | Old `mail_data.json` format (with `sender`/`timestamp` fields) loads correctly alongside new format. |
  | Permission nodes | Added: `mail.sendtemp`, `mail.sendall`, `mail.sendtempall`, `mail.clear.others`, `mail.clearall`. All registered in `PermissionRegistry`. |
  | Lang keys | 8 new keys added; all existing mail keys updated with better formatting. |
  | Pages | Increased from 5 per page → 9 per page (matches Essentials). |


  *(Fixed: 2026-03-01)*

  **Root causes found:**

  - **~50+ permission nodes used in commands but never registered in `PermissionRegistry`** — commands like `/list`, `/near`, `/nick`, `/motd`, `/mail`, `/ban`, `/kick`, `/freeze`, `/jail`, `/vanish`, and many others checked permissions that weren't in the registry. This meant `PermissionScanner` wouldn't find them, `/permissions list` wouldn't show them, and LuckPerms/FTB Ranks export was incomplete.

  - **Lang message keys being confused for permission nodes** — strings like `neoessentials.moderation.ban_broadcast`, `neoessentials.moderation.ban_success` etc. are **lang keys** (translation strings), not permission nodes. The scanner was incorrectly picking them up as permissions because they follow the same `neoessentials.*` pattern.

  - **`MODERATION` category missing from `PermissionCategory` enum** — all moderation permissions (ban, kick, freeze, jail, vanish) were falling through to `MISC` in both the `PermissionRegistry` categorize helper and `PermissionBridge.categorizePermission()`.

  - **Permission denial gave no indication of what permission was required** — every denied command showed only `"You don't have permission to use this command"` with no hint of the actual node needed. Server admins had no way to know what to grant.

  **Fixes applied:**

  | Category | Newly Registered Nodes |
  |---|---|
  | **Moderation** | `ban`, `banip`, `banlist`, `tempban`, `unban`, `unbanip`, `kick`, `kickall`, `freeze`, `unfreeze`, `freezeall`, `unfreezeall`, `freezelist`, `jail`, `unjail`, `setjail`, `jaillist`, `jailinfo`, `vanish`, `vanish.others`, `seevanished`, `vanishlist`, `notify`, `notifications` |
  | **Utilities** | `list`, `near`, `nick`, `nick.color`, `nick.others`, `staff`, `motd`, `motd.set`, `motd.broadcast`, `motd.reload`, `book`, `book.unlock`, `book.title`, `book.author`, `depth`, `depth.others`, `gamemode`, `gamemode.others`, `helpop`, `helpop.receive` |
  | **Mail** | `mail`, `mail.send`, `mail.clear` |
  | **Items** | `item.enchant.any`, `item.spawn` |
  | **Teleport** | `teleport.settpr`, `teleport.tp`, `teleport.tphere`, `teleport.tppos`, `teleport.pwarp`, `teleport.pwarp.create`, `teleport.pwarp.delete`, `teleport.pwarp.list` |
  | **Kits** | `kits.create`, `kits.delete`, `kits.override` |
  | **Permissions sub-commands** | `permissions.check`, `permissions.search`, `permissions.list.groups`, `permissions.list.users`, `permissions.info.user`, `permissions.info.group`, `permissions.user.permissions`, `permissions.user.groups`, `permissions.user.clear`, `permissions.group.create`, `permissions.group.delete`, `permissions.group.rename`, `permissions.group.clone`, `permissions.group.inherit`, `permissions.group.permissions`, `permissions.group.modify`, `permissions.group.clear` |
  | **Dashboard** | `admin.dashboard`, `dashboard.access`, `dashboard.view`, `dashboard.manage`, `dashboard.moderator`, `dashboard.admin` |
  | **Vanish alias** | `vanish.see` |

  **Structural fixes:**
  - Added `MODERATION` to `PermissionCategory` enum — moderation commands now appear in their own category in `/permissions list`, exports, and the dashboard
  - Updated `PermissionRegistry.categorizePermission()` and `PermissionBridge.categorizePermission()` to return `MODERATION` for ban/kick/freeze/jail/vanish prefixes
  - Updated `PermissionBridge.categorizePermission()` — previously returned `MISC` for `moderation`, `mod`, `mute`, `ban`; now returns `MODERATION`

  **Permission suggestion fix:**
  - `PermissionValidator.validatePermission()` — denial message now reads:
    `"You don't have permission to use this command.§7Required: §f<node>"`
  - `PermissionValidator.validateAnyPermission()` — shows all accepted nodes:
    `"You don't have permission. §7Required (any): §f<node1>§7 or §f<node2>"`
  - `PermissionValidator.validateTargetPermission()` — same treatment


- **Languages EN, FR, DE, ES, etc. incomplete — hardcoded strings, no custom language file support**
  *(Fixed: 2026-03-01)*

  **Root causes found:**

  - **Only `en_us.json` existed** — no translation files for any other language were bundled in the JAR. The infrastructure (`CustomLanguageManager`, `LocalizationManager`) was fully built but had nothing to serve.

  - **Broken colour codes in `en_us.json`** — the TPR/teleport keys added in a previous session had bare letter colour codes (e.g. `"eSearching..."` instead of `"§eSearching..."`), causing those messages to appear without formatting in-game.

  - **`CustomLanguageManager.initialize()` only deployed `en_us.json`** — when the server started it copied only `en_us.json` from the JAR to disk. No other bundled lang files were ever extracted, so even if they existed in the JAR they would never reach the `languages/custom/` directory where the system reads from.

  **Fixes applied:**

  | Fix | Detail |
  |---|---|
  | Fixed all broken colour codes | All TPR/misc teleport keys in `en_us.json` corrected (`e` → `§e`, `a` → `§a`, `c` → `§c`). Lang version bumped 102 → 103 |
  | Added `fr_fr.json` | French (France) — full coverage of all major command categories |
  | Added `de_de.json` | German (Germany) — full coverage |
  | Added `es_es.json` | Spanish (Spain) — full coverage |
  | Added `pt_br.json` | Portuguese (Brazil) — full coverage |
  | Added `zh_cn.json` | Chinese (Simplified) — full coverage |
  | Added `nl_nl.json` | Dutch (Netherlands) — full coverage |
  | Added `pl_pl.json` | Polish (Poland) — full coverage |
  | Added `ru_ru.json` | Russian (Russia) — full coverage |
  | Added `deployBundledLanguageFiles()` | New method in `CustomLanguageManager` — on every server start, iterates all 8 non-English bundled lang codes, copies missing files from JAR to `neoessentials/languages/custom/`, and merges NEW keys into existing files without overwriting user edits |

  **How translations fall back:**
  1. Custom user file on disk (`neoessentials/languages/custom/<lang>.json`) — highest priority
  2. Bundled JAR translation for that language
  3. `en_us.json` (English fallback via `MessageUtil`)
  4. Translation key itself (last resort)

  **Community contribution note:** All non-English files are tagged `"_author": "NeoEssentials (machine-translated, community corrections welcome)"` — admins can edit the files in `neoessentials/languages/custom/` and run `/language reload` to apply changes without restart.

- **Command /AFK not working properly**
  *(Fixed: 2026-03-01)*
  Five separate root causes were found and fixed:

  - **Root cause 1 — `AfkManager.loadConfiguration()` was never called:**
    The method to read AFK settings from `config.json` (timeout, kick settings, broadcast messages, activity tracking, etc.) existed but was never wired up. `AfkManager` ran entirely on hardcoded defaults regardless of what was in the config file.
    **Fix:** Added `AfkManager.getInstance().loadConfiguration(afkObj)` call to `NeoEssentials.onServerStarted()`, right after `ChatManager` is initialized.

  - **Root cause 2 — `AfkActivityHandler` suspicious-score blocked real player activity:**
    The anti-AFK-farming filter incremented the suspicious score by 10 for every action beyond 10 of the same type in 60 seconds. The threshold to be considered "suspicious" was 100 — meaning just 10 block interactions (perfectly normal building) would permanently block that player's activity from resetting their AFK timer. The score decay was also broken: it compared `now - lastActivity` where `lastActivity` was set to `now` on every call, so the difference was always ~0 and the score never decayed.
    **Fix:** Raised `REPETITIVE_ACTION_THRESHOLD` from 10 → 30, raised `SUSPICIOUS_SCORE_THRESHOLD` from 100 → 300, fixed score decay to compare against `lastActionTime` for the relevant action type, and reset per-type count when the 60-second window expires.

  - **Root cause 3 — `AfkMovementDetector` was missing `@EventBusSubscriber`:**
    The class had `@SubscribeEvent` methods for player login and logout (to initialize/cleanup position tracking) but was missing the `@EventBusSubscriber(modid = "neoessentials")` class annotation. NeoForge never registered those listeners, so player positions were never cleaned up on logout and never initialized on login.
    **Fix:** Added `@EventBusSubscriber(modid = "neoessentials")` annotation to the class.

  - **Root cause 4 — AFK broadcasts silently failed (`MessageUtil.info()` used as raw string):**
    `onPlayerGoAfk()` and `onPlayerReturnFromAfk()` called `MessageUtil.info(message)` where `message` was a plain string like `"Steve is now AFK"`. `MessageUtil.info()` treats its argument as a **translation key**, looks it up in the lang file, finds nothing, and returns the key unchanged — without colour or formatting. The broadcasts were also not logged to the server console.
    **Fix:** Replaced with `Component.literal("§e" + message)` directly. Added `server.sendSystemMessage()` call so broadcasts also appear in the server console.

  - **Root cause 5 — `/afk` command gave no feedback to the player:**
    `toggleAfk()` broadcasts a message to all players, but the player who typed `/afk` received no direct personal confirmation that the command worked — especially confusing since the broadcast message may not be visible to the player themselves if it's formatted differently.
    **Fix:** After calling `toggleAfk()`, the command now sends a direct `§eYou are now AFK.` / `§eYou are no longer AFK.` message to the executing player. Auto-AFK (inactivity timeout) also sends a personal notification: `§eYou are now AFK due to inactivity.`

- **NeoEssentials Chat Logging — chat messages not shown in server console (NeoForge 1.21.1, All The Mons)**
  *(Fixed: 2026-03-01)*
  - **Root cause:** When `enable-chat-formatting` is `true`, `ChatHandler` calls `event.setCanceled(true)` and takes over dispatch itself — sending messages via `sendSystemMessage()` to players only. `sendSystemMessage()` does **not** write to the server console. The only logging was `LOGGER.debug(...)` which is silent at the default log level. Vanilla's console logging never fires because the event is cancelled.
  - **Fix 1:** Added explicit `LOGGER.info("[channel] <player> message")` after dispatching to each channel type (proximity, permission-gated, global).
  - **Fix 2:** Added `server.sendSystemMessage(formattedMessage)` so the formatted message also appears in the dedicated server terminal exactly as vanilla would show it.
  - **Fix 3:** Added `logChatToConsole` boolean to `chat` config section (default `true`). Set to `false` to suppress chat from console/logs entirely if desired.
  - Config version bumped to 20.

- **NeoEssentials Teleportation — chunk not loaded causes "No safe teleport location found" even with safety disabled (NeoForge 1.21.1, All The Mons)**
  *(Fixed: 2026-03-01)*
  - **Root cause 1 — `isSafe()` used `canOcclude()`:** This is a strict opaque-cube check that returns `false` for slabs, stairs, glass, trapdoors, and many other solid blocks. Any home or warp set on those blocks was wrongly reported as unsafe.
    **Fix:** Replaced `canOcclude()` with `getCollisionShape(...).isEmpty()` in both `TeleportLocation.isSafe()` and `TeleportUtil.isSafeLocation()` — correctly matches the physical collision surface like Essentials does.
  - **Root cause 2 — `isSafe()` never checked dangerous blocks:** Lava, fire, cactus, nether portal, magma, etc. were all considered "safe" as long as feet/head space was air.
    **Fix:** Added `isDangerous()` helper in both `TeleportLocation` and `TeleportUtil` covering: lava, water, fire, soul fire, magma, cactus, sweet berry bush, wither rose, nether portal, campfire, soul campfire, powder snow.
  - **Root cause 3 — `findSafeLocation()` never did a top-down column scan first:** The XZ radius search with only a ±8Y window regularly failed to find the surface, especially for cross-dimension warps where the destination chunk was freshly loaded.
    **Fix:** `findSafeLocation()` now first does a full top-down column scan at the same X,Z (finds the surface in one pass), then falls back to the XZ expanding radius. `TeleportUtil.getHighestSafeY()` updated to use the same logic.
  - **Root cause 4 — `TeleportRequestManager` blocked `/tpa` entirely when destination was unsafe:** Matched old Bukkit plugin behaviour — no fallback, just an error. Essentials finds a nearby safe spot first.
    **Fix:** `executeTeleportRequest()` now calls `findSafeLocation()` first, warns the player ("teleporting to nearest safe location"), and only blocks if absolutely no safe location is found within 16 blocks.
  - **Root cause 5 — Double-safety in `HomeManager` and `WarpManager`:** Both managers already resolved a safe location before calling `TeleportUtil.teleportPlayer(..., findSafe=true)`, causing a second safety pass that could override the already-resolved location.
    **Fix:** Both managers now pass `findSafe=false` since safety is fully handled before the `TeleportUtil` call.

- **`/tpr` (Random Teleport) — basic brute-force with no config, safety, or biome awareness**
  *(Fixed: 2026-03-01)*
  - Old implementation was 50 blind random attempts with no safety checks, no cooldown, no world border awareness, no biome exclusions, no cache, no nether support.
  - **Fix:** Full port of EssentialsX's `RandomTeleport` system as `RandomTeleportManager.java`:
    - Equally-distributed offsets using the 4-rotation rectangle method (no centre-clustering)
    - Nether-aware Y detection (scans up from Y=32 below the bedrock ceiling)
    - World-border clamping
    - Pre-computation cache (filled asynchronously after each use, configurable `cacheThreshold`)
    - Configurable `findAttempts`, `cooldown`, `defaultMinRange`, `defaultMaxRange`
    - Per-location named slots — `/tpr [locationName]`
    - Excluded biomes list (global + per-location; oceans/void excluded by default)
    - Back-location saved before teleporting
    - Respects global `teleportDelay`
  - New `/settpr <locationName>` admin command to set RTP centre in-game.
  - New aliases: `/rtp`, `/randomtp`, `/randomteleport` all work.
  - Config: new `randomTeleportSettings` section added to `teleportation` in `config.json` (version bumped to 19).
  - Language keys added for all new messages.

- **Web Dashboard files not updating when newer versions are available**
  *(Fixed: previous session)*  
  Config version tracking (`_configVersion`) was already in place for config files. Dashboard HTML/JS/CSS files are now versioned and updated from JAR on server start when the bundled version is newer than what is deployed.

- **Dashboard Admin Controls and Permissions on a single page**
  *(Fixed: previous session)*  
  Admin controls and permissions management split into their own dedicated HTML pages (`admin.html`, `permissions.html`) instead of being crammed into one page.

- **Dashboard login requiring player to be online on server**
  *(Fixed: previous session)*  
  Auth system overhauled — players can register in-game with `/dashboard register` (requires permission), then log in from the web even when offline. Simple Discord Link integration added as an optional auth path; works standalone without the mod installed.

- **Dashboard register command not working**
  *(Fixed: previous session)*  
  `/dashboard register` command was not properly creating accounts. Registration flow fixed — generates token, stores credentials, confirms in-game.

- **Rich text (gradients/rainbow) not working despite being enabled in config**
  *(Fixed: previous session)*  
  Rich text tag parsing was not being applied to outgoing chat components. Fixed the chat processing pipeline to apply gradient/rainbow rendering when `richText.enabled` is `true`.

- **`/home` and `/warp` commands checking for safe teleports even when safety disabled in config**
  *(Fixed: previous session — and further strengthened 2026-03-01 per above)*  
  Config flag was being read correctly but the `findSafe=true` hardcoded argument to `TeleportUtil.teleportPlayer()` was overriding it. Fixed so that when safety is disabled in config, no safe-location search is performed.

- **PowerTool system — powertools affecting item slots instead of items**
  *(Fixed: previous session)*  
  PowerTool data was keyed on inventory slot index rather than item identity (NBT/item type). When a player moved items around, the powertool followed the slot, not the item. Fixed to key on item identity so the command travels with the item regardless of which slot it occupies.

- **Essentials teleportation system ported to NeoForge**
  *(Fixed: 2026-03-01)*  
  Investigated `./docs/Essentials/Essentials/src/main/java/com/earth2me/*` (CraftBukkit plugin source) and converted the teleportation architecture to NeoForge 1.21.1:
  - `RandomTeleportManager` (see `/tpr` fix above)
  - `isSafe()` / `findSafeLocation()` logic ported from `LocationUtil.java`
  - Dangerous block list ported from `DAMAGING_TYPES` / `LAVA_TYPES`
  - Top-down column scan ported from Essentials surface-finding behaviour

---

# 🎯 Additional Features

- **Economy integration**: Chest sign shops, Player Chest shops, Entity shops, dynamic pricing, CSV Dynamic pricing list import/export, and ect. more.
- **Holographic displays**: Support for holographic displays to show any information.
- **Chat formatting options**: More options for customizing chat format.
- **Inventory See**: Ability to view other players' inventories, editable inventories, and ender chests, based on permissions.
- **Minecraft Assets API support**: Figure out a way to integrate Minecraft Assets API for better resource assests to show in web-dashboards and other places.
- **Web-dashboard improvements**: Backup/restore functionality, more detailed statistics, and better user management, Backup/Restore from online storage services (Google Drive, Dropbox, etc).
- **Player Tablist**: Custom code for a custom player tab list that is highly customizable {References: Bungee Tablist Plus, TAB [1.7.x - 1.21.11], ☆ Simple TabList ☆《1.16.x - 1.21.x》- Animated - Hex colors}
- **Utility Systems**: Check if all these are in place, Nicknames, MOTD, near, ping, depth, helpop, rules, suicide, etc.
- **API & Placeholder System**: Apply more PlaceholderAPI integration, create more custom placeholders or allow the creation of more custom placeholders, REST API endpoints.
- **Permissions System Improvements**:
  - Wildcard & Hierarchical Permissions: Support for wildcards (e.g., neoessentials.*) and hierarchical permission inheritance, so granting a parent node gives access to all child nodes.
    Contextual Permissions: Allow permissions to be context-sensitive (e.g., per-world, per-channel, per-region, or time-based).
    Dynamic Permission Reloading: Add a command or event to reload permissions without restarting the server.
    Permission Checks in All Features: Ensure every command, event, and feature checks permissions strictly, including edge cases and new features.
    Permission Debugging Tools: Add commands to debug/check a user's effective permissions, showing where a permission is granted or denied.
    Permission Groups & Priorities: Allow group priorities, so if a user is in multiple groups, the highest priority group's permissions/prefixes/suffixes are used.
    Permission Expiry: Support temporary permissions that expire after a set time or event.
    API for Other Mods: Expose a clean API for other mods/plugins to check and register permissions.
    Permission Aliases: Allow aliases for permission nodes for easier migration or compatibility.
    Audit Logging: Log permission changes, grants, and denials for security and debugging.
    GUI Management: Provide a web or in-game GUI for managing permissions, groups, and users.
    Integration with External Systems: Improve and document integration with LuckPerms, FTB Ranks, and other permission mods, including fallback logic.
    Permission Suggestions: When a command is denied, suggest the required permission node in the error message.
    Fine-Grained Command Control: Allow per-argument or per-subcommand permissions (e.g., /home set vs /home delete).
    Custom Permission Conditions: Allow custom logic for permission checks (e.g., based on player stats, inventory, or server state).