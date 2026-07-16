# Full command/config/permission test findings — Dev-Builds (MC 1.21.1)

## 🔴 CRITICAL: SQLite storage backend silently no-ops on every write — total data loss

**Root cause** (`src/main/java/com/zerog/neoessentials/storage/SqliteDataStore.java:49`, hit via
`EconomyManager.<init>` → `StorageManager.<init>` at mod-construction time):

```
java.lang.ClassNotFoundException: org.sqlite.JDBC
	at ...SqliteDataStore.<init>(SqliteDataStore.java:49)
	at ...StorageManager.<init>(StorageManager.java:42)
	at ...StorageManager.getInstance(StorageManager.java:73)
	at ...EconomyManager.<init>(EconomyManager.java:51)
```

`sqlite-jdbc` is bundled via JarJar, whose isolated classloader (per the code's own comment)
"doesn't reliably trigger SQLite's own ServiceLoader auto-registration" — the existing
`Class.forName("org.sqlite.JDBC")` workaround does NOT fix this specific case, because
`EconomyManager` constructs its `StorageManager` **very early** in the mod lifecycle
(`NeoEssentials.<init>` → `registerAllManagers` → `EconomyManager.getInstance()`, during FML mod
construction), before the JarJar-embedded dependency is classloader-visible.

**The failure is then swallowed silently at every layer:**
- `SqliteDataStore`'s constructor catches the exception, logs one `ERROR` line, and leaves
  `connection = null` — no exception propagates up.
- `StorageManager` never checks whether the store it just built is actually functional; it logs
  `"StorageManager: active backend is 'sqlite'"` regardless.
- `SqliteDataStore.put(...)` starts with `if (connection == null) return;` — every single write,
  for the rest of the server's life, silently no-ops. No error, no warning, nothing.

**Confirmed empirically**: created warps (`/setwarp`) and changed economy balances (`/eco give`)
during a live session — both worked perfectly in-memory (`/warpinfo`, `/eco history`, the
dashboard's `/api/stats/economy` all showed correct data) — but every relevant SQLite table
(`ne_warps`, `ne_economy_balances`, and by the same mechanism presumably `ne_playerdata_homes`,
`ne_afk_data`, `ne_vanishes`, `ne_freezes`, `ne_jails`, etc.) had **0 rows**, even immediately
after the write and even after a **graceful** `/stop` (ruling out "just needs a clean shutdown
hook"). After restarting, `/warpinfo checkwarp` confirmed the warp was **completely gone** —
real, total, silent data loss, reproducing on every single boot of this dev environment.

Tables that *do* have data (`ne_kits`: 1, `ne_motd_profiles`: 1, `ne_motd_meta`: 1,
`ne_permission_groups`: 3) are bootstrap/default rows from a much earlier session, before
whatever changed to trigger this — worth git-bisecting `StorageManager`/`SqliteDataStore`/the
mod's manager-construction order to find when this regressed, since some data clearly did persist
at some point in this dev environment's history.

**Suggested fixes** (any one of these would resolve it):
1. Defer `StorageManager`/`SqliteDataStore` construction until a later FML lifecycle event
   (e.g. `FMLCommonSetupEvent` or later) instead of eagerly during mod construction — bootstrap
   `EconomyManager`/`ManagerRegistry` lazily instead of via a static `SingletonHolder` that runs
   at class-init time.
2. Make `SqliteDataStore`'s constructor failure fatal/loud (throw, don't swallow) so this can
   never again fail 100% silently — at minimum, `StorageManager` should detect a null-connection
   store and refuse to claim that backend is active, falling back to JSON/YAML with a clear
   warning instead.
3. Fix the actual JarJar classloader-visibility timing issue for `org.sqlite.JDBC` so
   `Class.forName` succeeds regardless of when `SqliteDataStore` is constructed.

---

Test rig: live dev server, RCON console + `/sudo <player> <command>` to test under
real player permission levels (WhiteyHyena = non-op, MrWhiteFlamesYT = op).
Tested against build after commit `1a917878` (sudo wildcard fix, uncommitted at
test-start) + persisted lang-file fix.

Legend: ✅ works · ⚠️ works with caveats · ❌ broken · ➖ not yet tested

---

## Economy

- ✅ `/balance`/`/bal` (self-only, no console/admin target variant — by design)
- ✅ `/eco give|take|set|reset <player> <amount>` — all correct, verified via dashboard `/api/stats/economy`
- ✅ `/eco history <player>` — shows correct transaction log
- ❌ **`/baltop` never picks up new accounts.** After `/eco give MrWhiteFlamesYT 250` (his first
  economy transaction), `/baltop` kept showing only `WhiteyHyena` and a stale "Total economy:
  $100.0" — even after its own "(Leaderboard is being recalculated...)" cycle completed (the
  "updated Ns ago" counter visibly reset from 87s→21s, proving the rebuild task ran, but the
  rebuilt list still excluded the new account). Cross-checked via the dashboard's
  `/api/stats/economy` endpoint (uncached), which correctly shows both players
  (MrWhiteFlamesYT: $350, WhiteyHyena: $100, total $450) — so the underlying data is fine, this is
  specifically a bug in `/baltop`'s own leaderboard-rebuild/player-discovery logic (likely only
  enumerating players who existed in its cache/list before their first transaction, not
  re-scanning all known economy accounts on rebuild).
- ❌ **`/setworth minecraft:dirt 0.5` fails to parse** ("Expected whitespace to end one argument,
  but found trailing data") — the `item` argument uses `StringArgumentType.word()`, whose allowed
  unquoted-string charset excludes `:` (same root cause class as the `/sudo *`/`**` bug fixed
  earlier this session). Confirmed the fix: `/setworth dirt 0.5` (no namespace prefix) works fine
  and resolves to `minecraft:dirt` internally — so the command already normalizes bare names, but
  breaks the instant a user types the full `namespace:item` form, which is the natural way anyone
  familiar with `/give` etc. would type an item ID.
- ✅ `/sell hand` (tested via `/sudo`, ran without error — payout not independently verified)
- ✅ `/worth` (tested via `/sudo`, ran without error)
- ✅ `/paytoggle`, `/pay <player> <amount>` (both ran without error via `/sudo`)

---

## Shops / Auction / Vault

- ⚠️ **`/cshop` (alias) bare command errors "Unknown or incomplete command"**, while `/chestshop`
  (full name) bare shows a full help menu. Same command tree, inconsistent bare-command behavior
  between the primary name and its alias — minor UX inconsistency, not a functional break (all
  subcommands work fine under either name).
- ✅ `/chestshop reload`, `/npcshop list`, `/npcshop` (help), `/npcshop create|remove|additem|...`
  (help text only, not exercised end-to-end since it needs a real NPC entity/shop sign in world)
- ✅ `/ah` (help/menu — "Players only" from console, correct), `/ah reload`, `/ah help`,
  `/ah selling`, `/ah expired` (all ran without error via console/`/sudo`)
- ✅ `/vault` (ran without error via `/sudo`, not exercised end-to-end — needs in-world GUI interaction)

---

## Teleport

- ⚠️ **My own testing error, not a mod bug**: assumed EssentialsX-style `/warp set`/`/warp del`
  subcommands exist — they don't. Real syntax: `/warp <name>` (teleport), `/warp <name> <player>`
  (teleport other, `neoessentials.teleport.warp.others`), `/setwarp <name>` (create at current
  position, separate top-level command), `/delwarp`/`deletewarp`/`removewarp`/`rwarp` (aliases,
  separate top-level command). All work correctly once called with the right syntax.
- ✅ `/setwarp`, `/warp <name>` (teleport), `/warpinfo`, `/delwarp` — all correct in-memory
  behavior. **Persistence is broken, but that's the storage-layer bug above, not these commands.**
- ✅ `/setspawn`, `/spawn` — ran without error
- ✅ `/sethome`, `/homes`, `/home <name>`, `/renamehome`, `/delhome` — all ran without error
  (same storage caveat as warps applies to survival across restart)
- ✅ `/setpwarp`, `/pwarps`, `/pwarp <player> <name>`, `/delpwarp` — ran without error
- ✅ `/tpa`, `/tpaccept`, `/tpahere`, `/tpdeny`, `/tpcancel` — full request/accept/deny/cancel
  cycle ran without error
- ✅ `/back`, `/tpauto`, `/tp`, `/tphere`, `/tpall`, `/tppos`, `/top`, `/jumpto`, `/jump`, `/tpo`
  — all ran without error
- ⚠️ **Another testing error on my part**: assumed `/settpr <x> <y> <z> <dimension>` — real syntax
  is `/settpr <locationName>` (captures the sender's *current* position under a named RTP-center
  slot; the `/tpr`/`rtp`/`randomtp`/`randomteleport` aliases then randomize around it). Works
  correctly once called right.

---

## Kits

- ⚠️ **Testing error, not a bug**: `/createkit <name>` requires the player's inventory to be
  non-empty (fails with `createkit.empty_inventory` sent to the *player*, invisible from
  console/`/sudo`'s own success message). Worked correctly once the target actually held an item.
- ⚠️ **Testing error, not a bug**: `/delkit <name>` alone only shows a confirmation prompt —
  actual deletion requires `/delkit <name> confirm`. Verified `listkits` correctly stopped
  showing the kit only after the `confirm` step.
- ✅ `/createkit`, `/listkits`, `/kit <name>` (claim), `/showkit`, `/kitreset <kit> <player>`,
  `/delkit <name> confirm` — full lifecycle all correct in-memory (persistence subject to the
  storage-layer bug above)

---

## Items / Inventory

- ❌ **`/invseeedit <player>` is completely broken** — reproducible in isolation, not a testing
  artifact. Client error: `An unexpected error occurred trying to execute that command` /
  `Unable to construct this menu by type`.
  Root cause (`src/main/java/com/zerog/neoessentials/inventory/InventoryViewCommands.java:350-355`,
  `PlayerInventoryContainerMenu.java`): `openEditableInventory()` opens a
  `SimpleMenuProvider` backed by the custom `PlayerInventoryContainerMenu extends
  AbstractContainerMenu` — but that class is **never registered with a `MenuType<>`** (no
  `DeferredRegister<MenuType<?>>` entry anywhere in the codebase). The client has no registered
  screen constructor to map the network packet to, hence the vanilla "unable to construct this
  menu by type" error. By contrast, `/invsee` (read-only) works because it builds on a real
  registered `MenuType.GENERIC_9x3`/`GENERIC_9x6`, and `/enderchestedit` works because it reuses
  vanilla's built-in `ChestMenu.threeRows` instead of a custom menu class. **Fix**: register
  `PlayerInventoryContainerMenu` as a real `MenuType` via `DeferredRegister<MenuType<?>>` (with a
  matching client-side `MenuScreens.register(...)` binding), the same pattern already used
  correctly elsewhere in the mod for other custom GUIs.
- ✅ `/invsee <player>` (read-only) — works correctly
- ✅ `/enderchest <player>` (read-only), `/enderchestedit <player>` (editable) — both work correctly
- ⚠️ **Testing setup note, not a bug**: `/repair`, `/hat` need an item in the relevant slot to do
  anything meaningful — gave test items first, both then ran without error.
- ✅ `/powertool`, `/powertooltoggle`, `/powertoollist`, `/enchant`, `/dispose`,
  `/clearinventory`, `/customtext`, `/payconfirmtoggle`, `/ciconfirmtoggle`, `/item` — all ran
  without error via console/`/sudo`

---

## Moderation

- ❌ **Auto-ban-on-repeated-jailings silently permanently banned a real test account.**
  Repeatedly jailing/unjailing `WhiteyHyena` while testing `/jail`, `/jailfor`, and `/deljail`
  (all within about 2 minutes) triggered an undocumented "exceeded maximum jailings" auto-ban
  system, which **permanently banned** the account with reason `Exceeded maximum jailings
  (permanent ban)`, banned by `System`. This surfaced no message to the console/tester at the
  time — I only discovered it ~30 minutes later, by accident, while testing `/banlist` for an
  unrelated reason, after wrongly assuming `WhiteyHyena` had simply disconnected. In the
  meantime `/togglejail WhiteyHyena` and `/deljail`'s "remove jailed players" step both correctly
  reported "player not found" — those were **not bugs**, they were accurately describing a banned
  player, but the *ban itself* had zero visible feedback anywhere I was looking (console, RCON,
  server log grep for "error/exception/fail").

  **Root cause found** (`src/main/java/com/zerog/neoessentials/moderation/JailManager.java:250-268`,
  `ConfigManager.getMaxJailsBeforeTempBan()`/`getMaxJailsBeforePermBan()`): both thresholds
  default to **3** when not present in `config.json` — and neither key
  (`moderation.jailSettings.maxJailsBeforeTempBan`/`maxJailsBeforePermBan`) is actually present in
  the shipped `config.json` at all, so every fresh install silently has this active with no
  visible documentation of it. Worse: since both defaults are the same value (3), and the
  perm-ban check (`jailCount >= permBanThreshold`) runs *before* the temp-ban check in the same
  `if`/`else if` chain, **the temp-ban tier is dead code under default settings** — three
  jailings always goes straight to a permanent ban, never a temporary one, regardless of intent.
  The only trace of this firing is one `LOGGER.info` line (gated behind
  `isLogJailActionsEnabled()`), which goes to the server log, not back to whoever ran `/jail`.

  **Fix**: (a) ship sane, clearly-documented defaults in `config.json` itself (not just
  code-level fallbacks) so admins can see and tune this without reading source, (b) make
  `maxJailsBeforePermBan` meaningfully higher than `maxJailsBeforeTempBan` by default so the
  temp-ban tier is actually reachable, and (c) surface a clear message to the admin who triggered
  the jail that just caused an auto-ban, not just a log line.
- ✅ `/warn`, `/warnings`, `/removewarn <player> <shortId>` (the truncated 8-char ID shown in the
  listing works fine — no need for the full ID), `/clearwarnings` — all correct
- ✅ `/note`, `/notes`, `/removenote <player> <index>` — correct (notes use a simple numeric
  index, unlike warns which use an ID — confirmed intentional, not an inconsistency, both work
  as designed)
- ⚠️ **Testing errors on my part, not bugs**: assumed `/jail` itself takes a duration argument —
  it doesn't; the timed variant is a separate command `/jailfor <player> <jail> <duration>
  [reason]`. Assumed `/jailinfo` takes a player name — it takes a *jail location* name. Assumed
  `/togglejail` works bare — it requires a `<player>` argument.
- ✅ `/setjail`, `/jail`, `/jailfor`, `/jaillist`, `/jailinfo <jailname>`, `/unjail`, `/deljail`,
  `/togglejail <player>`, `/jailwand` — all correct once called with proper syntax (see the
  auto-ban caveat above for the one real issue found in this group)
- ✅ `/freeze`, `/unfreeze`, `/freezeall`, `/unfreezeall`, `/freezelist` — full cycle correct
- ✅ `/vanish`, `/unvanish`, `/vanishlist` — full cycle correct
- ✅ `/kick <player> [reason]` — correct, disconnects immediately with reason shown
- ✅ `/ban`, `/unban`, `/tempban <player> <duration> [reason]`, `/banlist` — all correct;
  `/banlist` correctly showed both a permanent and a timed ban with expiry timestamp
  simultaneously

---

## Chat

- ❌ **`/chatformat` is completely unregistered — 100% dead code.** Even the bare `/chatformat`
  (no subcommand) fails with "Unknown or incomplete command", and there is zero log trace of
  `ChatFormatCommand`/`chatformat` anywhere in the boot log (no registration message, no "skipped,
  disabled" debug line — nothing). Confirmed via
  `grep -rln "ChatFormatCommand" src/main/java` excluding its own file: **no other file references
  it at all.** `src/main/java/com/zerog/neoessentials/chat/command/ChatFormatCommand.java` fully
  implements `set`/`clear`/`check`/`list`/`reload` subcommands, but `register(dispatcher)` is
  never called from wherever the mod's central command registry wires up all the other command
  classes. Config already has `"chatformat": true` in `commands`, so this isn't a config-disable
  issue — it's simply never invoked. **Fix: find the central command-registration list (likely
  in `NeoEssentials.java` or a `CommandRegistry` class) and add the missing
  `ChatFormatCommand.register(dispatcher)` call.**
- ✅ `/msg`, `/reply`, `/msgtoggle`, `/ignore`, `/unignore`, `/socialspy` — all ran without error
  via `/sudo` with both accounts back online
- ❌ **`/mute`, `/mutelist`, `/unmute` cannot be run from console/RCON at all**, unlike every
  other moderation command (ban/kick/freeze/jail/warn/note all work fine from console). Root
  cause (`src/main/java/com/zerog/neoessentials/chat/command/MuteCommand.java:57-59`, same
  pattern in `MuteListCommand.java`/`UnmuteCommand.java`): explicitly checks
  `if (source.getPlayer() == null)` and rejects with `neoessentials.error.no_server` — **and that
  error message itself is misleading**: it says "This command requires a server context and
  cannot be run from here," but the actual condition being checked is "requires a real player
  sender" — the opposite of what "server context" would normally suggest to an admin reading it
  (they'd reasonably assume it means the opposite: that it needs *console*, not a player). This
  reproduced identically with real players online and offline — it's unconditional, not related
  to target availability. **Fix: either let console mute/unmute/list like every other moderation
  command (most likely correct, since an admin should be able to `/mute` from console just like
  `/ban`), or if player-only really is intentional, use a correctly-worded error key instead of
  the generic `no_server` one that's clearly meant for a different kind of failure.**

---

## Permissions

- ❌ **CRITICAL: Group assignment doesn't actually reach the real permission-enforcement engine —
  the whole group-permission system is non-functional right now**, almost certainly a downstream
  effect of the SQLite storage bug above. Full reproduction:
  1. `/permissions user WhiteyHyena setgroup testgroup` → `"User group set."` (reported success)
  2. `/permissions info user WhiteyHyena` → shows `Group: testgroup` (confirms the change, from
     the command's own perspective)
  3. `/permissions group testgroup add neoessentials.moderation.ban` → `"Permission added."`,
     and `/permissions info group testgroup` correctly lists it
  4. `/sudo WhiteyHyena ban MrWhiteFlamesYT ...` (WhiteyHyena is non-op, should now have ban
     permission via the group) → **`/ban` silently did nothing** — `/banlist` stayed empty
  5. Server log at the exact moment of that ban attempt:
     `PermissionAPI.getPrefix: No PermissionGroup found for group 'default'` (WARN level) — the
     **live PermissionManager** still resolves WhiteyHyena's group as `default`, not `testgroup`,
     directly contradicting what `/permissions info user` reported two steps earlier. And
     `default` itself apparently isn't even a registered group in the live manager ("No
     PermissionGroup found").

  In other words: **`/permissions info user` reads from a different source of truth than the
  actual enforcement path uses.** An admin who runs `setgroup`, confirms via `info user`, and
  believes the change took effect has no way to know the real permission engine never saw it.
  This very likely ties back to the SQLite init failure above — if permission groups (including
  the built-in `default`) fail to load from storage at startup the same way warps/economy failed
  to *save*, the live `PermissionManager` could be operating with an empty/default-only group
  registry that `setgroup`/`info` update a *different* in-memory or stale-cache copy of. Given
  the severity (this is the mechanism that's supposed to gate every single admin-tier command in
  the mod), this deserves priority investigation right alongside the storage bug — the two are
  likely one root cause with two visible symptoms.
- ✅ `/permissions list groups`, `/permissions list users`, `/permissions info user <name>`,
  `/permissions info group <name>`, `/permissions create group <name>`,
  `/permissions group <name> add <permission>` — all correctly *display* consistent results with
  each other (self-consistent), just disconnected from real enforcement per above.
- ⚠️ **Testing errors, not bugs**: `/permissions` bare, and `/permissions info <name>` (needs
  `info user <name>` or `info group <name>`), and `/permissions user <name> group <name>` (the
  correct subcommand is `setgroup`, not `group`) all correctly rejected my first-attempt syntax.
- ❌ **Corroborates the critical finding above**: created `renamedgroup`/`clonedgroup` via
  `/permissions rename group`/`clone group` (both reported success), then ran `/permissions
  reload` — afterward, both groups were completely gone (`"Group 'renamedgroup' does not
  exist!"`, and `/permissions list groups` → `"No groups found."`). This is exactly consistent
  with the storage bug: `reload` re-reads groups from the (broken, effectively-empty) SQLite
  store, silently discarding whatever existed only in memory. `/permissions search <term>` (7
  matches for "ban") worked correctly.
- ➖ `/neoessentials-permissions`, `/neoe-perms` (external plugin compat bridge commands) — both
  need subcommand arguments I didn't have time to fully map; not exercised beyond confirming they
  register and reject bare invocation. Lower priority than the core permission system above.

---

## Player state

- ✅ `/fly`, `/god`, `/heal`, `/feed`, `/speed walk|fly <n> <player>`, `/ext`, `/burn`, `/more`,
  `/playtime` — all correct
- ⚠️ **Testing error, not a bug**: assumed `/exp give <player> <amount>` — real order is
  `/exp give <amount> [player]` (amount first). Same for `/exp set`. Both correct once called
  right (confirmed level/XP totals update correctly: 100 XP → level 7, then set to 50 → level 4).
- ✅ `/sudo` — already extensively tested/fixed earlier this session (chat + wildcard targets)

---

## Server admin

- ⚠️ **Minor UX gap, not a functional bug**: `/broadcast`/`/bc`/`/announce` give **zero feedback
  via RCON/console** when run there — confirmed the broadcast itself works correctly (verified in
  the server log: `[Broadcast] Console : Test broadcast message`), but the code only sends the
  message to online players and to `server.sendSystemMessage(...)` (the console's own log
  stream) — never an explicit `ctx.getSource().sendSuccess(...)` back to the command issuer. A
  real in-game player using this command would see their own broadcast in chat and feel it
  "worked"; console/RCON gets silence either way, working or not.
- ✅ `/time`, `/time set`, `/day`, `/night`, `/weather`, `/sun`, `/storm`, `/thunder` — all correct
- ✅ `/kill` (vanilla selector-based, e.g. `@e[type=item]`) — correct
- ✅ `/gamemode <mode> <player>` — correctly applied even to an **offline** player (stored for
  next login)
- ✅ `/recipe give <player> *` — correct (unlocked 1290 recipes)
- ⚠️ **Testing errors, not bugs**: `/world` and `/tpoffline` require a real player sender
  ("Only players can use this command" — correct, by design). `/spawner` requires a `<mob>`
  argument I omitted.

---

## Fun / world interaction

- ✅ `/firework`, `/itemdb`, `/potion`, `/info`, `/antioch`, `/kittycannon`, `/beezooka`, `/rest`,
  `/nuke <player>` — all ran without error via `/sudo`
- ✅ `/backup` — gave a clear direct confirmation via console/RCON ("Saving world data..." →
  "Backup complete.") — best console-feedback UX of any command tested so far
- ✅ `/fireball`, `/tree`, `/bigtree`, `/break`, `/ice`, `/bottom`, `/tpaall`, `/broadcastworld`,
  `/bcastworld` — all ran without error via `/sudo`

---

## Utility / misc

- ❌ **`/me <action>` throws a Brigadier argument-type conflict**: `An unexpected error occurred
  trying to execute that command` / `Argument 'action' is defined as Message, not class
  java.lang.String`. Root cause
  (`src/main/java/com/zerog/neoessentials/util/commands/ItemCustomisationCommands.java:70-86`):
  the mod registers its own `/me` using `Commands.argument("action",
  StringArgumentType.greedyString())`, but **vanilla Minecraft already has a built-in `/me
  <action>` command that uses `MessageArgumentType`/`MessageArgument` for its own `action`
  argument**. Registering a second command under the same literal name (`me`) with an
  argument of the same name (`action`) but a different underlying type causes exactly this
  kind of type-mismatch error at execution time — Brigadier's argument redefinition/merge
  behavior for same-named nodes across a literal collision. **Fix: rename the mod's internal
  argument (e.g. `action` → `neMeAction`) won't help since the literal itself collides; the
  real fix is to either not re-register `/me` at all (defer to vanilla, which already does
  almost the same thing) or explicitly override/replace vanilla's dispatcher node for `me`
  rather than adding a second sibling registration.**
- ✅ `/who`, `/online` (aliases for `/list`) — correct
- ✅ `/nick <name>`, `/nick` (clear) — ran without error
- ✅ `/whois <player>` — showed nickname + username correctly (e.g. `JJ {MrWhiteFlamesYT}` — a
  pre-existing nickname from before this test session, not something I set)
- ✅ `/help` — clean paginated output (27 pages)
- ✅ `/tptoggle` — ran without error
- ✅ `/gc`, `/mem` — both show identical uptime/TPS/memory/chunk info, correct
- ❌ **`/gc` is a command-name collision, confirmed via source.** The memory-diagnostics `/gc`
  (`ItemCustomisationCommands.java:137`) and the chat-channel system's dynamically-registered
  `/gc` alias (`ChannelCommands.java`, registered because the shipped `config.json` lists `"gc"`
  as an alias for the `global` chat channel under `chat.channels.global.aliases`) both register
  the same top-level literal `gc`. My live test earlier showed memory-diagnostics output, meaning
  the channel-switch alias is currently shadowed and effectively unusable — a player following
  the config's own stated alias for switching to global chat would get server stats instead.
  **Fix: rename one of the two** (the config's `global` channel alias list is the safer one to
  change, since editing it doesn't require a code change) **or add a startup check that warns
  when config-driven channel aliases collide with built-in command names.**
- ✅ `/lightning`, `/smite <player>`, `/skull`, `/itemname`, `/rename`, `/itemlore` — all ran
  without error
- ⚠️ **Possibly storage-bug-related, worth double-checking**: `/seen WhiteyHyena` showed
  `First seen: 2026-07-16 15:40:40` — identical to the current "Online for" timestamp, despite
  `WhiteyHyena` having joined/rejoined many times over the ~2 hours of this test session. Looks
  like "first seen" data either isn't persisting (same root cause as the storage bug) or is being
  recomputed as "now" rather than tracking real first-join history. Not independently confirmed
  root cause — flagging for follow-up rather than asserting.
- ✅ `/realname`, `/motd`, `/near`, `/mail send`/`/mail read`, `/ping`, `/book`, `/rules`,
  `/afk` (toggle on/off), `/getpos`, `/compass`, `/helpop`, `/ptime`, `/pweather`, `/effect`,
  `/spawnmob`, `/unlimited`, `/suicide` — all ran without error

---

## Other systems

- ⚠️ **Testing error, not a bug**: assumed `/hologram create <id> <text>` — real syntax is
  `/hologram create <id> [x] [y] [z] [world]` (creates empty, at position). Text lines are added
  separately via `/hologram addline <id> <text>`. Full lifecycle (create → addline → info →
  delete) verified correct once called right — `info` output is excellent (world, position,
  visibility, refresh rate, billboard mode, scale, line spacing, shadow, opacity, background,
  text align, see-through, line width, view range, spin, hover, and all lines).
- ✅ `/tablist reload` — correct
- ✅ `/dashboard status`, `/dashboard url` — correct; minor cosmetic glitch in `status`'s
  update-check line ("Files: build.none→ build.29 available" — missing a space/separator around
  the arrow, not a functional issue)
- ✅ `/placeholder list` — correctly listed 39 registered placeholders
- ✅ `/language list`, `/language reload` — both correct (9 custom/bundled languages)
- ✅ `/neoe` (bare help, 261 total commands listed), `/neoessentials` (alias, identical output),
  `/neoe reload` — excellent feedback: "13/13 systems reloaded successfully" plus a clear caveat
  that `modules`/`commands` config toggles need a full restart (reload only refreshes data) —
  best UX of any command tested in this whole sweep.
- 📌 **Corroborates the `/gc` collision finding**: `/neoe`'s own command list explicitly shows
  `/gc — Show server memory and TPS info` with no mention of it also being a chat-channel alias,
  confirming only one of the two registrations is actually live in the dispatcher.

---

## Config testing

Reviewed the full `run/config/neoessentials/config.json` structure (modules, storage, commands,
economy, webDashboard, security, items, permissions, afk, kits, teleportation, moderation, chat,
tablist) while investigating the bugs above. Targeted live verification:

- ✅ **`chat.antiSpam.capsFilter`** (`action: "lowercase"`, `maxPercentage: 70`) — confirmed via
  server log: sent `THIS IS ALL CAPS SHOUTING` (100% caps) as chat, the actual broadcast (per
  `ChatHandler`'s own log line) was correctly lowercased to `this is all caps shouting`.
- ✅ **`chat.antiSpam.repeatFilter`** (`action: "block"`, `cooldownSeconds: 5`) — confirmed via
  server log: sent the identical message `testing repeat filter` twice in immediate succession;
  only the *first* produced a `ChatHandler` broadcast line — the second was silently blocked,
  exactly as configured.
- ❌ **`storage.type: "sqlite"`** — see the critical finding at the top of this document. This is
  the single highest-impact config value in the whole file right now: with the shipped default,
  nothing persists across a restart.
- ❌ **`moderation.jailSettings`** — `maxJailsBeforeTempBan`/`maxJailsBeforePermBan` are used by
  code but never appear in the shipped `config.json`, so their code-level defaults (both `3`,
  making temp-ban unreachable) are silently active with zero visibility to an admin reading the
  config file. See the Moderation section above.
- ❌ **`chat.channels.global.aliases: ["global", "gc"]`** — collides with the built-in `/gc`
  memory-diagnostics command. See the Utility/misc section above.
- ➖ Did not get to live-test: `modules.*Enabled` toggles (confirmed via `/neoe reload`'s own
  output that these specifically require a full restart to take effect, so disabling e.g.
  `economyEnabled` and confirming `/balance` disappears would need yet another restart cycle —
  deferred given the number of restarts already done this session), `webDashboard.securitySettings`
  (rate limiting, auth requirement — dashboard already exercised extensively in a separate part of
  this session), `items.item-spawn-blacklist` (blocking `/give` for bedrock/barrier/command
  blocks/etc. — plausible from reading the code, not independently re-verified here).
