# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

## [1.0.5] — 2026-08-19

#### Fixed
- Player chat could inject internal rich-text formatting markers (`§ITEM§`, `§URL§`, `§MENTION§`, and similar) to spoof clickable items, links, or mentions — these are now stripped from all player-submitted chat before formatting.
- Web dashboard shop price changes could race a concurrent buy/sell, applying a stale price.
- A player released from jail on respawn could be re-teleported into the jail cell even after disconnecting mid-respawn.
- FTB Ranks permission integration failed *open* (granted access) instead of denying it when a permission lookup errored.
- `/tpa` and the home/warp cooldowns could be consumed even when the request failed validation, effectively wasting the player's cooldown for nothing.
- Discord mention sanitization (preventing chat-triggered `@everyone`/`@here`/role pings) now also covers the Mc2Discord and DCIntegration bridge adapters, not just the primary integration.
- Web dashboard vanish, jail, and balance-adjustment actions were mutating live player/entity state directly from the HTTP request thread instead of the main server thread, risking corrupted entity state under concurrent use — now correctly synchronized.
- A narrow disconnect race in the vanish system's player-join handling, where a player who disconnected within a 1-tick window could still be sent stale visibility packets.

#### Changed
- Replaced the single global debug-logging toggle with independent per-category logging (`logging.categories` in config) — chat, economy, permissions, teleportation, moderation, auction house, kits, web dashboard, Discord, config, commands, and general subsystems can each be switched on/off separately for normal and debug output, instead of one all-or-nothing flag.
- Dozens of previously-silent error paths across chat, permissions, tablist, teleportation, and command handling now log real diagnostic detail when their category's debug logging is enabled, instead of failing without a trace.

#### Platform
- Added a Minecraft 26.2 build (NeoForge 26.2.0.63+), alongside the existing pinned Minecraft 26.1–26.1.2 build. Both are separate downloads from the primary 1.21.1–1.21.11 build.

## [1.0.2.6+build.1] — 2026-03-06

### Starting fresh from 1.0.2.6

This is the first build of the `1.0.2.6` release series. Build number reset to 1.

**Carried forward from 1.0.2.5 series:**

#### Added
- Sign-based ChestShop system — admin shops, auto-fill (`?`), buy/sell via right/left-click
- Vault API — Economy, Chat, and Permission providers backed by NeoEssentials systems
- Dedicated `tablist.json` config — group colours, 18 placeholders, animation, `&` colour codes
- 50+ new commands across Player Info, World/Fun, Teleport, Item/Misc, Utility, Admin, Player State
- `/tpr` / `/rtp` Random Teleport — even distribution, nether-aware, async pre-computation cache, named zones, biome exclusions, `/settpr`
- Timed jails (`/jailfor`) with auto-release, full event enforcement (respawn, teleport, interact, attack)
- `/kit <name> <player>` give-to-others, `/kitreset`, clean kit list with cooldown status
- `/mail sendtemp`, `sendall`, `sendtempall`, `clearall` — mute/ignore/rate-limit checks
- `/warp <name> <player>`, `/warp` (no args) paginated list, per-warp permission support
- `/eco reset`, async `/baltop` with pagination and total wealth, percent amounts in eco commands
- 8 new bundled languages: FR, DE, ES, PT-BR, ZH-CN, NL, PL, RU — auto-deployed and merged on start
- 50+ permission nodes registered; new `MODERATION` category; denial messages show required node
- `tablist.json` dedicated config; `/tablist config` live settings summary

#### Fixed
- Teleportation safe-location detection rewritten — slabs, stairs, glass, trapdoors now correctly safe; dangerous blocks (lava, fire, magma, cactus) now correctly blocked
- AFK system — config loading, activity score thresholds, broadcast formatting, personal feedback all fixed
- Chat messages now appear in server console
- PowerTool — fires on block right-clicks and empty right-clicks, not just air; `/powertooltoggle` now correctly enables/disables powertools
- Rich text (gradients/rainbow) rendering pipeline fixed
- Dashboard — offline login, register command, file auto-update, admin/permissions split into own pages
- ~120 missing translation keys added to `en_us.json`; auto-merge on load without overwriting edits
- Vault economy `format()` now reads live currency symbol from config
- Vault chat prefix/suffix correctly routes through LuckPerms/FTBRanks when installed
- NeoForge 1.21.1 API compatibility: event classes, `ItemStack` methods, stats API all corrected
