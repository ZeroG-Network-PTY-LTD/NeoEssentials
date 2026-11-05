# NeoEssentials — Changelog (26.3.x branch)

**Minecraft 26.3 · NeoForge 26.3.0.101+**

All notable changes to this branch are documented here, starting from
**v1.0.6** — earlier history (v1.0.5.x and before, including the port's own
migration history) is not carried over.

---

## [1.0.7] — 2026-10-16

### Added
- **2026-10-17** — Build 9 ([`4aa84bd9`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/4aa84bd916b79a980ca0e180570fd939be02606a)) — `/chestshop hologram move` now also works with no arguments (moves it to wherever you're standing), plus a new `/chestshop hologram movepos <signX> <signY> <signZ>` that does the same without needing to look at the sign. Max offset from the sign raised from ±4.5 to ±16 blocks.
- **2026-10-24** — Build 14 ([`116a6a58`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/116a6a589a061e04f74048a5483944129f9980e4)) — `/dashboardregister` now shows a clickable link to the paired external dashboard's own `/register` page when one is configured, instead of only the in-game token flow. Internal-only servers are unaffected. Relates to #75.
- **2026-10-30** — Build 15 ([`0afa8273`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/0afa82738d74add75b4b2f5a8851e38f0148ab26)) — The mod can now run the internal (bundled) dashboard behind its own Cloudflare Tunnel, mirroring the external dashboard's existing Docker setup. New `/dashboard tunnel enable <token>|disable|status` commands and `webDashboard.tunnel.*` config launch and supervise an already-installed `cloudflared` connector as a subprocess tied to the dashboard's start/stop lifecycle.
- **2026-11-02** — Build 16 ([`668bfb0b`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/668bfb0b2f683caa6aab322068faf73d71688dcb)) — Per-group dashboard permissions — an in-game permission group can now be given granular dashboard access instead of only the 4 fixed roles. New `webDashboard.groupPermissions` config maps a group to a list of `{tab, access}` grants (read/write per tab); a player's role syncs to this automatically from their primary in-game group. Supported on both the internal and external dashboards.

### Fixed
- **2026-10-17** — Build 9 ([`4aa84bd9`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/4aa84bd916b79a980ca0e180570fd939be02606a)) — `/tp` silently required real vanilla op status instead of any NeoEssentials permission (even a full wildcard admin got "Unknown or incomplete command") — it collided with vanilla's own `/tp`, whose permission check Brigadier's command-merging kept instead of ours.
- **2026-10-18** — Build 10 ([`bce90de2`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/bce90de22ebfc5df7223e3dd1711b4c323a46917)) — Reduced hologram scheduler overhead with no behavior change — dimension lookups are cached once per cycle instead of rescanned per hologram, and static (non-animated) lines no longer re-run a string search on every animation tick.
- **2026-10-20** — Build 11 ([`d18e49df`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/d18e49dfff7721dc7f53a53fd1191b23f3a901e3)) — Dashboard "Reset password" now actually shows the generated temporary password in its toast instead of a generic "Password reset for X." message. Fixes #75.
- **2026-10-24** — Build 12 ([`f00a89a5`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/f00a89a5e04898e6da367d86152c1bf30d0a969c)) — `/dashboardregister complete` no longer takes a password argument (it leaked into the server console log like any command does) — it now generates a one-time temp password and shows it in chat instead. Fixes #75.
- **2026-10-30** — Build 15 ([`595cb7b3`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/595cb7b3d82276bd8ccc701b85d00038d60b30e8)) — Polished the internal dashboard's login page to match the external dashboard's design pass — the login card now uses the shared `dash-card` panel styling and `hover-lift` interaction instead of a plain bordered box.
- **2026-10-30** — Build 15 ([`983ad29c`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/983ad29c7bf907cdb342615af505e0f4e61d085f)) — The "config splitting available" notice could repeat in a joining admin's chat on every server restart instead of showing once. It's now console-only — the unconditional console log lines already covered the same information every boot without that fragility.
- **2026-11-02** — Build 16 ([`c76b1dc2`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/c76b1dc2cf4fd07eb947c1e04de8109d5df89b70)) — Fixed the same unloaded-chunk-at-border teleport bug (issue #72) at 6 remaining call sites in `RandomTeleportManager`/`MiscTeleportManager` — switched a raw `(int)` cast to `Math.floor()`.
- **2026-11-02** — Build 16 ([`48ea547f`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/48ea547f367b6e5b032ddad3162e2928dd20dd69)) — Fixed an error on every server start from kit loading running before vanilla's item-component system was ready — now only loads after the server has fully started.
- **2026-11-02** — Build 16 ([`2299c881`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/2299c88127ab6b0aeee69ac3360c4df151cfc7dd)) — Mute/AFK/advancement messages to Discord could skip mention-sanitization and truncation that chat already got, since each bridge adapter built its own message string separately. These now go through shared, always-sanitized helpers.
- **2026-11-03** — Build 19 ([`37d00ec1`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/37d00ec14a83705a477f9621a9ab38aa2c505f57)) — Security: `GET /api/auth/discord?username=` and the legacy minecraft-type login minted a real dashboard session from a bare username with no proof of ownership — up to full Admin impersonation. Replaced with an in-game confirm flow (`/dashboardlogin <code>`); no session mints until the real player confirms it.
- **2026-11-03** — Build 19 ([`bf05c263`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/bf05c263e91d4aea7cf17f5b9074a65f8c222788)) — Editing a hologram's lines/frames while visible could throw or briefly show garbage text — the backing lists had no synchronization between the command thread and the animation scheduler. Switched to `CopyOnWriteArrayList`.
- **2026-11-03** — Build 19 ([`3af22576`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/3af22576189ba5eefc82e0dee6aa1b1365ece38a)) — ~16 manager singletons could silently construct two separate instances under concurrent dashboard/main-thread access. All now synchronize their `getInstance()`.
- **2026-11-03** — Build 19 ([`fac57cdd`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/fac57cdd52ae2bd07b314b295e73a3eacb3a1d22)) — A failed storage write during a ban, mute, balance change, or permission edit was silently swallowed, reporting success while the change existed only in memory. Every persistence call now checks and surfaces the real result.
- **2026-11-03** — Build 20 ([`7c508355`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/7c5083559edd6793bf4fa2d531f94be7c828314d)) — Security: a CUSTOM-role dashboard user granted write access to the "users" or "apikeys" tab could self-promote to full Admin or mint a durable non-expiring API key, via a stale admin-only path list. Both paths are now always admin-gated regardless of tab grants.
- **2026-11-03** — Build 20 ([`7f4d1437`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/7f4d14374ddffd2a53f5620a54c6f80e6638e73b)) — The dashboard's run-command endpoint and its stats sampler/player-statistics endpoint read and mutated live world state off dashboard HTTP threads, racing the main tick loop — now correctly marshal onto the main thread. Also fixed directory deletes claiming a backup was made when none was.
- **2026-11-03** — Build 20 ([`e1c44450`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/e1c444504e82f07b5c76dbba82700b79c279f3cc)) — Config saves (crates, leaderboard, scoreboard, teleport/warps, cloud storage) silently swallowed write failures the same way storage writes did before Build 19 — "saved" responses now check the actual result.
- **2026-11-03** (Build 21, [`76bf0d34`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/76bf0d34)): Freeze/unfreeze, command autocomplete, and server statistics were the last dashboard handlers still touching live server/player state off the dashboard's own HTTP threads, the same hazard already fixed for their sibling handlers in Build 20. They now correctly marshal onto the main thread.
- **2026-11-04** (Build 25, [`a8f8f5b9`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/a8f8f5b9)): `/effect` built its potion effect with an unregistered holder, which silently broke that player's data saves (every autosave/logout) until the effect was cleared, rolling back progress made in between. Now uses the registered holder for the effect instead. Also fixed `/effect` ignoring its own config toggle.
- **2026-11-04** ([`debb9c8d`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/debb9c8d)): `/ptime`, `/pweather`, `/spawnmob`, and `/unlimited` each had their own config toggle but ignored it, same bug as `/effect` above. All four now respect their config toggle.
- **2026-11-04** ([`47f58292`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/47f58292)): 24 more commands had the same unused-toggle bug (fireball, tree, break, ice, bottom, tpaall, broadcastworld, firework, nuke, antioch, kittycannon, beezooka, itemdb, potion, info, rest, backup, broadcast, time, weather, kill, gamemode, tpo, tpoffline, condense, showkit, powertoollist, customtext, payconfirmtoggle, ciconfirmtoggle, item, rtoggle, me, tptoggle, gc, lightning, skull, itemname, itemlore, remove, loom, cartography). All now respect their config toggle.
- **2026-11-04** ([`79cd739a`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/79cd739a)): `/world`, `/spawner`, `/recipe` had the same bug, and `/warpinfo` had it worse (registered from the mod's bootstrap directly, bypassing its toggle and the teleportation module switch entirely). All now respect their config toggle.
- **2026-11-04** ([`8418149f`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/8418149f)): **Security** - a dashboard database-browser REST API (including a raw-SQL `/query` endpoint) had no authentication at all and was never wired up to the dashboard's HTTP server, so it was unreachable - but if it had ever been registered without the gap being caught, anyone could run arbitrary SQL against every plugin database with no login required. Added the admin-only check every other dashboard endpoint uses, closed a table-name SQL-injection path, rejected stacked SQL statements, and registered it through the normal auth middleware so it's now an actually-secured feature.
- **2026-11-04** ([`4b4626ad`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/4b4626ad)): **Security** - five more dashboard handlers had the identical bug: no auth, never registered, covering resource-pack upload/delete, scheduled-task CRUD/execution, server log access, and a full raw player-data dump with wide-open CORS. All now have proper auth and are registered. Also split a dead duplicate ban-management handler's one unique feature (whitelist) into its own `WhitelistHandler`, and fixed its session lookup, which had been recording "admin" as the audit-trail username for every change regardless of who was actually logged in.
- **2026-11-04** ([`5f411fee`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/5f411fee)): 17 more manager singletons (including `ModerationManager` and `DatabaseManager`) had the same unsynchronized `getInstance()` race already fixed for ~16 others back in Build 19. All now synchronize their `getInstance()`.
- **2026-11-04** ([`2d69f311`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/2d69f311)): **Security** - Votifier V2's vote-signature check used a plain string comparison, leaking a timing side-channel that could help brute-force the signature without the real shared token. Switched to a constant-time comparison.
- **2026-11-04** ([`10ae5333`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/10ae5333)): `/effect` never got the self-vs-others permission split every other admin-targetable command has, so any player with the base permission could apply or clear effects on anyone. Added `neoessentials.effect.others`. Also documented `ptime`/`pweather`/`spawnmob`/`unlimited`'s existing `.others` tiers, previously missing from the permission reference.
- **2026-11-04** ([`a0df2fc5`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/a0df2fc5)): Added dashboard tab permissions for resource packs, server logs, scheduled tasks, and whitelist management, so admins can grant staff access to them without making them full Admin. The database browser and raw player-data dump stay admin-only.
- **2026-11-04** ([`b5b42feb`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/b5b42feb)): Added an opt-in blacklist and per-entity permission system for `/spawnmob`, mirroring `/item`'s existing permission-based item spawning. Both off by default, so nothing changes unless a server enables them.
- **2026-11-04** ([`17b53a7f`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/17b53a7f)): Added an opt-in per-crate permission for `/crate open`, letting admins restrict specific crates to specific ranks even if a player holds a key. Off by default. Also registered and documented the previously-unregistered base crate permissions.
- **2026-11-04** ([`e5d4c52e`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/e5d4c52e)): Added an opt-in per-enchantment-type permission for `/enchant`, letting admins reserve enchantments like Mending for VIP ranks. Off by default.
- **2026-11-04** ([`611032f8`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/611032f8)): `/item` and `/give` silently never enforced the item-spawn blacklist/permission system, despite it being documented as blocking dangerous items from any command. Both now enforce it. `/give` also gained `neoessentials.give.others`.
- **2026-11-04** ([`eef95f65`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/eef95f65)): Crate rewards had the exact same blacklist-bypass bug as `/item`/`/give` above - a blocked reward item is now withheld with a chat notice and a server-log warning instead of being handed out unconditionally.
- **2026-11-04** ([`7efbc963`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/7efbc963)): The dashboard's "Give item" player action had the same blacklist-bypass bug as `/item`/`/give` above - it now returns an error instead of silently handing out a blocked item.
- **2026-11-04** ([`26c90a6f`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/26c90a6f)): The dashboard's "Spawn mob" player action had the same blacklist-bypass bug, this time against `spawnmobSettings.mob-spawn-blacklist`/`permission-based-mob-spawn` - it now returns an error instead of silently spawning a blocked entity type.
- **2026-11-04** ([`8395ca0d`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/8395ca0d)): `/tp <player> <target>` (forcibly teleporting another player) had the same self-vs-others permission gap already fixed for `/effect`/`/give` - added `neoessentials.teleport.tp.others`.
- **2026-11-04** ([`9bdfc414`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/9bdfc414)): Added an opt-in per-effect-type permission for `/potion add`, mirroring `/enchant`'s existing per-enchantment permission - letting admins reserve effects like Instant Damage for staff. Off by default.
- **2026-11-05** ([`d94dac58`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/d94dac58)): Added the same opt-in per-effect-type permission for `/effect`, on top of its existing self-vs-others split. Off by default.

---

## [1.0.6] — 2026-08-27

### Added
- **2026-10-09** — Build 87 ([`41c8f805`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/41c8f8052939a9ae547db14a2a339e65679ea3ba)) — The NPC shop window now shows your live balance on a gold ingot (bottom-right slot), refreshed after every buy/sell — previously only in the title, which couldn't update while the window was open.
- **2026-10-09** — Build 86 ([`c82a9111`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/c82a9111ecf2ad424c0eaaf2a5763cfe45f8d221)) — Fixed `/npcshop additem` and NPC shop listings not recognizing modded items with no `worth.yml` entry (e.g. `mekanism:osmium_ingot`) — now falls back to the item registry.
- **2026-10-09** — Build 85 ([`277ee52f`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/277ee52f4e1a2ed1d4ed7c775542a117a089c082)) — Modded ids work in `/npcshop additem`, `addcommand` and `entitytype`; `entitytype`'s `<ai>` is optional.
- **2026-10-08** — Build 84 ([`1a3920ca`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/1a3920cabba8b83a03b6c7e53ac30203b911347e)) — Easier shops: `/npcshop` accepts shop names or `this`, new `additemhand` and `editlisting ... price`, 1-based slots, shift-click stacks and balance in the NPC shop window, second-click confirmation above `shop.confirmAbove`, and fuller `/chestshop info` for command signs.
- **2026-10-07** — Build 83 ([`0596bf64`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/0596bf64c3e4e18725a94abf5fd270f107b04464)) — Command shops: sell commands from NPC shops (`/npcshop addcommand`, `/npcshop editlisting`) or `[Command]` Admin Shop signs (`/chestshop command ...`). Runs as console or the buyer, `{player}`/`{uuid}` placeholders, optional permission node, and the buyer is never charged for a command that can't run.
- **2026-10-06** — Build 82 ([`caf1ee68`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/caf1ee689e6ed4e895d39b6806facbc52b9fd149)) — The `[NE]` message prefix is now configurable via `general.messagePrefix` (`""` removes it).
- **2026-09-16** — Build 77 ([`e5758be3`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/e5758be361debd0cc7d455b2e98af94e76beee00)) — Valued permission nodes — groups can now hold a `"meta"` map (node -> value) for permission systems expecting a number/string instead of a plain grant, wired into NeoForge's Permission API for `INTEGER`/`STRING` node types. Used to give the rank ladder progressively higher FTB Chunks (`ftbchunks.max_claimed`/`max_force_loaded`) claim limits per tier — only effective if a mod queries limits through NeoForge's Permission API rather than its own separate config.
- **2026-09-16** — Build 76 ([`d63a704f`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/d63a704f3b2be9b45b8f486f35ae98b55f1b07e3)) — Added a `guest -> member -> vip -> vip+ -> mod -> admin -> owner` permission group ladder with progressively higher `/home` limits, plus playtime-based auto-promotion (guest -> member after 24h, configurable). `permissions.json` is no longer a one-time seed — every group it defines is now re-applied on every reload/restart, so editing the file and reloading actually takes effect.
- **2026-09-16** — Build 75 ([`2d53fc9d`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/2d53fc9db193f739b0e9331c2b90a0aacb5c1f7c)) — Third-party command permission gate — map an arbitrary command path (including a specific subcommand) from another mod, or a vanilla command, to a NeoEssentials permission node via `command_permissions.json`. Deepest configured path wins; denial uses the same message NeoEssentials' own commands show; permission-node changes on already-covered commands apply live via `/neoe reload`, no restart needed.
- **2026-09-16** — Build 74 ([`923ef4bb`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/923ef4bb02e32e7584d3bddd9f6706d4633f773f)) — `/back` is now a bounded in-memory undo-stack (default 10, `teleportation.backSettings.maxBackHistory`) instead of remembering only the single most recent death/teleport location — chaining `/tpa`, `/spawn`, `/home` lets `/back` walk back through each hop in turn. Purely in-memory, cleared on disconnect/restart (no more disk persistence).
- **2026-09-15** — Build 70 ([`bbe8d6f7`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/bbe8d6f7dac7c8a9af15e8a639b2d160d4a7d19a)) — Fixed /spawn and /home instability near chunk borders (issue #72) — teleport preloading was silently loading the wrong chunk right at a border on the negative side, causing a false "unsafe location". Also added a PvP system — `/pvp on|off|toggle`, server-wide switch plus mutual per-player opt-out, newbie protection, and safe-zone integration.
- **2026-09-09** — Build 69 ([`d0939a1f`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/d0939a1f2655c8b0abca376fb58e9973421494ff)) — NPC shops can now use any registered entity type (vanilla or modded) with an AI on/off toggle — `/npcshop entitytype <id> <type> <ai>` converts an existing shop in place. AI-enabled Mobs keep their normal animations but are leashed to their spawn point; AI-disabled Mobs are frozen like the classic ArmorStand.
- **2026-09-08** — Build 68 ([`0e8cbb26`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/0e8cbb268d657a4f3d217d3b4418040ac82e3a63)) — Fixed doubled rank prefixes in Discord — NeoEssentials' own Discord messages (chat/join/leave/mute/AFK/advancement/PMs, all three adapters) now resolve the prefix from the permission system directly instead of the vanilla scoreboard team, so they can never double regardless of a bridge mod's own native relay. Verified live against a real Discord server.
- **2026-09-07** — Build 67 ([`66e96854`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/66e96854336f87a9b63234ebda13dece611b17e7)) — Wider Discord event coverage: Mc2Discord now relays AFK/private messages and reads its avatar URL from config instead of hardcoding it; DCIntegration now relays advancement/mute/AFK when routed to an explicit Discord channel (all additive-only, confirmed safe against DCIntegration's own native advancement relay via bytecode).
- **2026-09-07** — Build 66 ([`c2d141c3`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/c2d141c374e0d8ab1825fa39325c8e88f2419bfa)) — SDLink/Mc2Discord's native-relay conflict warnings (previously console-only) are now also exposed via the dashboard's Discord status API, for admins configuring integration through it.
- **2026-09-07** — Build 65 ([`b14cb394`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/b14cb394d5feec4ccd5710cb872751dd2aa4c9fe)) — Rich Discord embeds for SDLink's join/leave/mute/AFK/advancement events when routed to an explicit Discord channel — each now has its own `discordEmbedTemplate.<event>` override with sensible defaults, matching what chat messages already got. Set an event's `enabled` to `false` to keep plain text for just that event.
- **2026-09-07** — Build 64 ([`b048521e`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/b048521e76e54a51fd0d6ea7af1368e862a35fff)) — SDLink's native-relay conflict detection now parses `simple-discord-link.toml` with a real TOML parser instead of a best-effort text scan; long chat/PM/mute/AFK/advancement text sent to Discord is now truncated before sending across all three bridge-mod adapters.
- **2026-09-07** — Build 63 ([`aaace896`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/aaace896091c1bbdeb83867b4f78392d95cbaa46)) — Discord role → permission group sync — new `discordrolesync.json` maps Discord role IDs to permission groups, synced via DCIntegration (highest-priority group wins on multiple matches, re-checked on a timer and on join, only grants/upgrades, never demotes, internal permission manager only).
- **2026-09-07** — Build 62 ([`7fdacaa6`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/7fdacaa66168cf6daa86edf4a644f3054deee36f)) — Mc2Discord native relay conflict detection — NeoEssentials now reads `config/mc2discord.toml` on startup and checks whether Mc2Discord's own channel subscriptions already relay chat/join/leave/advancement events natively, suppressing its own default-route send for that event type to avoid posting it twice. Explicit per-channel overrides still always send.
- **2026-09-07** — Build 61 ([`5f0ac1bb`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/5f0ac1bbf09fe1c78a36caad6571f55696cbfe8f)) — Generic Discord webhook relay — a new `webhookUrl` field alongside `channelId` lets NeoEssentials post events straight to a Discord webhook over plain HTTPS with no bridge mod installed at all. Chat/advancement messages impersonate the sending player. Takes effect immediately on `/neoe reload`, no restart needed.
- **2026-09-07** — Build 60 ([`8398265d`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/8398265d2c084df3d6c8fe683a179a369071e67f)) — Per-event Discord channel routing for join/leave/mute/AFK/advancement/private-message events (new `discordEventChannels` config section) — previously only chat could route to a specific Discord channel. Works with SDLink, Mc2Discord, and DCIntegration (which additionally gained join/leave routing for the first time).
- Sidebar scoreboard system — `/scoreboard` with config-driven boards, conditions,
  animation, group/player overrides, a toggle command, and a dashboard endpoint.
- General leaderboard system — `/leaderboard` (`/lb`), with `money`/`kills`/`mob_kills`/
  `playtime` boards and `{leaderboard_<board>:<rank>:name|value}` placeholders.
- Leaderboard boards are now config-driven (`leaderboard.json`) — register any vanilla
  per-player stat (blocks mined, mobs killed, distance, etc.) as a board via config, plus
  "custom" point-total boards (`/leaderboard admin set|add|reset|create|delete`) and a
  `LeaderboardAPI` for other mods to register their own boards.
- `/leaderboard hologram create <board> <id> [lines]` — generates a ranked leaderboard
  hologram in one command; the lines keep updating live on their own.
- Leaderboards can now rank non-players — a new `shop_sales` board ranks sign/chest and NPC
  shops by total revenue, shown by shop name instead of a player name.
- Per-board `refreshInterval` in `leaderboard.json` (was a fixed 60s for every board).
- Leaderboard styling: automatic per-rank medal/color placeholders (`:medal`/`:rankcolor`),
  per-board `entryFormat`/`headerFormat` chat templates, and a new paginated GUI viewer
  (`/leaderboard <board> gui`) with real player heads and per-board icons.
- `/tpr`/`/rtp` can open a biome-select chest GUI instead of teleporting instantly — opt-in
  via `randomTeleportSettings.mode: "gui"` (default unchanged). Lists every biome the
  dimension can generate (modded biomes included automatically), plus a "Random — Any Biome"
  button; picks a real spot in that biome or shows a clear error if it can't find one within
  the server border. Icons default to that biome's sapling/dedicated block; admins can pin
  biomes to fixed slots and/or override icons via `biomeMenuItems`.
- Votifier vote listener — its own TCP port, both V1 (RSA) and V2 (NuVotifier-compatible)
  protocols auto-detected, per-site reward config (commands/crate keys), offline vote queueing,
  vote party, and `/vote`/`/votes`/`/voteparty` commands.
- Crates — weighted reward-pool crates with a key balance system, opened via `/crate open` or
  a physical block, three opening animations (roulette/sequential/instant), and full admin
  commands to define crates/rewards in-game. Votifier can grant crate keys per vote site.
- `/crate admin setblock` now auto-creates a hologram above the block (named `crate_<crateId>`,
  findable in tab-completion), fully customizable via any `/hologram` subcommand.
- Left-clicking a physical crate block now opens the no-cost reward-odds preview (shift+click to
  still break it).
- `/crate key giveitem <player> <crate> <amount>` gives real, tradeable physical crate key
  items — no virtual balance required to redeem one.
- Tablist `header`/`footer` frames can now be written as their own JSON array of lines instead
  of hand-escaping `\n` inside one long string — e.g. `["Line 1", "Line 2"]` as one frame.
- Crate key item names now support `{animation:NAME}`/gradients/rainbow, not just `&`-codes —
  matching what already worked in the crate's hologram text (a snapshot per-item, since a held
  item can't be live-repainted the way a hologram can).
- Crates now support separate display names for chat and the key item —
  `chatDisplayName`/`crateKeyDisplayName` (`crates.json`), plus `/crate admin
  setchatname`/`setkeyname`, both falling back to `displayName` if unset. Previously an animated
  `displayName` flashed a different random snapshot frame in every chat message with no way to
  opt chat out of it specifically.
- `/crate keys [player]` now lists every registered crate on its own line, including ones you
  have zero keys for, instead of only comma-joining the crates you actually held keys for.
- Fixed the console being flooded with `PermissionManager is null` warnings on servers using an
  external permission adapter with no prefix/suffix support (e.g. FTB Ranks) — that's the
  expected state there, not a failure, and no longer logs a warning.
- Hologram refresh/animation tick rates were hardcoded — now configurable via
  `hologram.refreshInterval`/`animationInterval` in `config.json`, same convention as tablist/
  scoreboard's `refreshInterval`. Applies with `/neoe reload`.
- New per-board `refreshMultiplier` in `scoreboard.json` (default 1) lets one board cycle its
  animation frames slower than the rest without touching the global refresh rate. Default
  `refreshInterval` for tablist/scoreboard/hologram tightened from 20 to 10 ticks (smoother out
  of the box) — only affects fresh installs, existing configs are untouched.

### Fixed
- **2026-10-07** — Build 83 ([`de40fe22`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/de40fe220f28dbafb98324a57b4ad2e363a43045)) — Config upgrade backups are now kept in `config/neoessentials/backups/` (newest 5 per file) instead of piling up next to the configs.
- **2026-10-06** — Build 81 ([`78ae1e4d`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/78ae1e4df658f622d78f5ee7c772f27fc6ada8eb)) — Holograms no longer force-load chunks nobody is near; animated holograms in unloaded chunks were reloading their chunk and re-creating their entities every tick, causing heavy lag.
- **2026-10-06** — Build 80 ([`f975babe`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/f975babed6d029efca6adfb97ca593a621879d54)) — Custom join/quit messages (`customJoinMessage`/`customQuitMessage`) now replace vanilla's "X joined/left the game" message instead of showing alongside it. Leaving them at `"none"` keeps the vanilla message.
- **2026-10-05** — Build 78 ([`429576f9`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/429576f99616286aa057f3bfa2b47c5038ec7cf2)) — Fixed a stale [AFK] tag surviving logout/login (issue #73): AFK state resets on login, and `@s` names in `/tellraw` no longer keep a stale [AFK] tag.
- **2026-09-06** — ([`04d20d5b`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/04d20d5b8d4af2c28b0f03a0d81a911f49d6b9fd), docs only) — Documented `nametagSettings.enabled: false` in `tablist.json` as the fix for Discord chat-bridge mods (e.g. SimpleDiscordLink) showing the rank prefix twice. In-game chat is unaffected; no code change needed.
- **2026-09-06** — Build 59 ([`7a158686`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/commit/7a158686c370e70bd1602c0d01eb3cf4a5da400f)) — Fixed a player's name showing as literal `HNAME<name>/HNAME` in chat instead of a clean clickable name when a chat-format template wrapped `{neoessentials_username}` in `<gradient:...>`/`<rainbow>` (the common way to give an FTB Ranks/LuckPerms rank a colored name). Fixes #70.
- The startup admin-notice chat block (bug-report links, config split available, etc.) is now
  gated by a dedicated `neoessentials.admin.notice` permission node instead of a hardcoded
  OP/wildcard check, and all pending notices render as one combined block. Also removed the
  "LEGACY DATA FILE(S) NO LONGER READ" notice entirely.
- FTB Ranks servers can now use `"group:<rank>"` chat-format keys and the
  `{ftbranks_prefix}`/`{ftbranks_suffix}`/`{ftbranks_rank}`/`{ftbranks_group}` placeholders —
  these never actually worked on FTB Ranks before (only LuckPerms). Group now resolves from the
  player's highest-power rank; prefix/suffix come from that rank's `ftbranks.name_format` value.
  Also fixed `"group:<name>"` keys being case-sensitive against the (always-lowercased) resolved
  group name, which affected every permission backend, not just FTB Ranks.
- `/permissions group <group> setprefix|setsuffix` no longer shows a raw "unexpected
  error" when the internal permission manager isn't active (e.g. an external permissions
  plugin like LuckPerms is in use) — it now explains why instead.
- `{luckperms_group}`/`{luckperms_primary_group}`/`{ftbranks_rank}`/`{ftbranks_group}`
  placeholders no longer resolve to an empty string when LuckPerms/FTB Ranks is actually
  active — `prefix`/`suffix`/`displayname` variants were unaffected.
- Nicknamed players (`/nick`) now keep their permission-group prefix/suffix in the tab list —
  a nickname used to silently drop it the instant it was set, on any group.
- `PermissionAPI.getPrefix()`/`getSuffix()` now fall back to the internal permission system
  when LuckPerms/FTB Ranks has no opinion, matching `getGroupWeight()`/`getPrimaryGroup()`'s
  existing behavior. FTB Ranks servers previously got no prefix/suffix at all through this
  mod, ever; LuckPerms groups with no `prefix`/`suffix` meta set now fall back to an internal
  one too instead of showing nothing.
- Five more places had the identical internal-manager-only bug (found by auditing every
  remaining call site): the public `NeoEssentialsAPI` group lookup, the `{group}` placeholder,
  per-group tablist column bucketing, chat badge group gating, and player tags all silently
  fell back to `""`/`"default"` under LuckPerms/FTB Ranks — now fixed the same way.
- Config-version upgrades (the merge that adds new default keys/boards to an existing config
  on update) were silently failing on Windows — fixed. Also fixed new default boards (like
  `shop_sales`) not reaching an already-upgraded install even after that fix.
- `/tpr`/`/rtp` could lag or crash a server, especially without pre-generated terrain — it was
  force-generating far more chunks per teleport than necessary (an unneeded 9-chunk safety
  grid for RTP specifically, plus a 10-chunk burst refilling the background cache right after
  every teleport). Both fixed; new `prewarmBatchSize` config caps the refill burst, and lower
  default range/attempts reduce worst-case cost on fresh servers. `/tpr` also now announces
  its warmup delay and that moving cancels it — that already worked, it just wasn't visible.
- Splitting your config (`/neoe config split`) silently dropped web dashboard settings and your
  storage backend choice entirely — neither was ever registered in the split-config section
  list, so both always fell back to defaults after splitting (dashboard settings, and storage
  reverting to `"json"` even if you had SQLite/MySQL configured). Both fixed — `webDashboard`
  now gets its own `dashboard.json`, `storage` now lives in `main.json`. See the Split Config
  wiki page if you already hit this — your original values are recoverable from
  `config.json.backup`.
- Right-clicking a physical crate block ran the whole open flow twice per click (duplicated
  "no keys" message, could double-consume keys) — fixed.
- A crate with no rewards configured yet silently ate a key and reported the misleading "no
  keys" error instead of "no rewards configured" — fixed; the key is no longer spent unless a
  reward actually exists.
- Tab-completion was silently missing on ~25 command arguments across 16 files (crate/hologram/
  home/board/group names, item ids, player targets, etc.) — full audit, all fixed.
- A crate's `&`-coded display name showed up as literal text (e.g. `&7Common Crate`) instead of
  being colored, in chat, GUI titles, and the key item's name — fixed mod-wide (every command
  reply built via `MessageUtil`, not just crates).
- `{server_name}` is now a plain, independently-configurable name (`general.serverName`), no
  longer tied to any MOTD — a new `{server_motd}` placeholder covers the "show the actual
  configured MOTD" case instead.
- `/neoe reload` silently did nothing for the scoreboard system (only tablist was fixed for
  this before) — scoreboard.json edits now actually take effect on reload.
- Some inventory-utility client mods' "pull items from the open GUI" button could steal (and
  duplicate) items straight out of `/crate preview`/the opening reveal GUI — an initial fix
  missed the exact method these mods use to blank the slot; now every mutation path on the
  container is locked down, closing it regardless of which mod is doing the pulling. Confirmed
  (via Quark) that some mods don't even need a mutation method — they just read and copy the
  item — so both crate GUIs now also mark every displayed item and sweep it out of your real
  inventory if it ever ends up there, regardless of how it got there. The preview's close/prev/
  next buttons and filler panes were missed by the first pass of that marking; fixed too.
- Physical crate key items were never actually giveable, and even a key item obtained some
  other way still required virtual balance to redeem — both fixed (see `/crate key giveitem`
  above).
- A malformed config file (e.g. a stray brace from a manual edit) could crash the entire server
  repeatedly — a JSON parse error now falls back to defaults for whatever that file drives
  instead of taking the server down.
- A nicknamed player still showed their real IGN in tab to anyone who joined the server after
  the nickname was set — every join now re-syncs everyone's nickname overrides, not just the
  joining player's own.
- `/neoe reload` (and `/scoreboard reload`) could kick every player seeing the sidebar
  scoreboard with a fatal "Network Protocol Error" — fixed.
- The permission system couldn't recover from a failed boot init without a full restart (kept
  logging `PermissionManager is null` all session) — `/neoe reload` now re-initializes it from
  scratch when that happens instead of failing the same way forever.
- The "config splitting available"/"legacy data files" admin notices nagged on every single
  restart forever — each now shows once per install instead of once per session.
- Closing the crate GUI immediately after using a pull-mod extraction button could still win the
  race and keep the item — closing now sweeps a few more times over the next ~4 seconds instead
  of just once, closing the timing window entirely.
- `/crate admin create` didn't say where the new crate went — now points to
  `config/neoessentials/crates.json`.
- `/scoreboard reload` never refreshed `animations.json` — only `/tablist reload` did. Editing
  an animation and reloading via the scoreboard command left the old frames showing (raw
  `{animation:...}`/gradient text, unformatted) until a `/tablist reload` also happened to run.
- `/eco give|set|take`/`/pay` never invalidated the leaderboard "money" board (only `/baltop`'s
  own cache), so a balance change could leave a scoreboard "richest player" line stale for up
  to 60s — fixed. `/baltop` and `/leaderboard <board>` also both misreported "no data" on the
  very first call ever, even with real data present — fixed.
- The economy system had no way to recover if it never finished initializing at boot (silently
  stopped persisting balances for the rest of that session) — `/neoe reload` now retries it,
  same as the permission system's existing self-heal.
- `/pay` and `/eco give|take` crashed on every use whenever `security.enableInputValidation`
  was turned off — a validator returned the wrong type in that bypass path — fixed.
- `{animation:NAME}`/gradient/rainbow tags never resolved in chat — every command reply used a
  color-code-only parser that didn't know those tags existed, so they showed as raw literal text
  (reported for a crate's name showing `{animation:animation}` unresolved when opened). Fixed
  mod-wide at the shared message-building code, not just for crates. Note: a sent chat message is
  a permanent snapshot of whichever animation frame was current the instant it was sent — chat
  has no live-update channel like tablist/scoreboard/holograms do, so there's no
  `intervalMs`/`refreshInterval` equivalent for it.
- Crate GUI titles and a crate key's lore/hover text now also resolve animation/gradient tags,
  matching the key's name (fixed earlier) — same one-time-snapshot behavior.
- Confirmed, not a bug: crate keys (`/crate admin setkey`) already support any vanilla or modded
  item, full NBT/data included — no fixed item list.
- An animation frame's gradient/rainbow tags were silently stripped in chat unless
  `chat.richText.enabled` was separately turned on (off by default) — tablist/hologram always
  rendered them regardless of that setting. Fixed — animation gradients/rainbow now always render
  in chat too; the config still gates a player typing raw gradient tags themselves.
- `/neoe reload` never actually reloaded `leaderboard.json` (only the dedicated `/leaderboard
  reload` did) — every sibling system's refresh interval already worked via `/neoe reload` except
  this one. Fixed.
- `{animation:NAME}` froze on its first frame forever, mod-wide (holograms, scoreboard, chat,
  crate keys), on any server with the tablist module disabled — the only thing that ever advanced
  the animation clock was tablist's own tick handler, which never ran once tablist was off. No
  hologram/scoreboard interval setting could fix this since the clock itself never moved. Fixed —
  animations now advance every tick regardless of whether tablist is enabled.
- Renamed `hologram.refreshInterval` (`config.json`) to `hologram.pollIntervalTicks` — it was
  easy to confuse with each hologram's own, separate, seconds-based `refreshInterval`
  (`/hologram setrefresh`), which is the one that actually controls how often placeholders
  refresh. Lowering the old key below that value did nothing visible. Existing values carry over
  automatically on upgrade.
- Hologram animations advanced in visible bursts ("jumpy"/slow) no matter how low the animation's
  own `frameDuration` was set, even though the same change applied instantly and smoothly in
  tablist/scoreboard — hologram refresh and animation ticking shared one background thread, so a
  slow refresh cycle delayed the next animation tick behind it. Fixed — they now run on
  independent threads. Also promoted a silently-swallowed hologram render error to a visible
  warning.
- Crate rewards/keys with saved item components (custom name, lore, enchantments) could silently
  lose that data on server start due to a startup ordering race between two separate listeners.
  Fixed — crate loading now always happens after the fix that binds the registry access it needs.
- Hologram animations could still burst under load even after the thread split above — the
  scheduler caught up on a delayed cycle by firing back-to-back instead of skipping. Fixed by
  switching to a schedule that waits from when the previous cycle finished, so it can never
  build a backlog to burst through.
- A color-only `{animation:NAME}` (a gradient cycling hex stops over the same literal words —
  the most common animation shape) could freeze on its first frame forever in holograms
  specifically, even with the fixes above. The change-detection compared plain text, which a
  color-only change never affects, so it never saw a change to react to. Fixed.

---
