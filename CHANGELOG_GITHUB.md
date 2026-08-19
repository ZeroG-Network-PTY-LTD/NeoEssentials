# Changelog — NeoEssentials

All notable changes to NeoEssentials are documented here.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+**

---

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
