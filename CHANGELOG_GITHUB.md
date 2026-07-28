# Changelog — NeoEssentials (mc-26.1-port branch)

All notable changes to this branch are documented here, starting from
**v1.0.4** — earlier history (v1.0.3.x and before, including the port's own
migration history) is not carried over.  
Format: `[version+build] — date`  
Compatibility: **Minecraft 26.1.2 · NeoForge 26.1.2.76+**

> The build counter was reset alongside the v1.0.4 bump, so build numbers here start
> back at 0/1 — always go by date/version, not build number, when comparing across
> the reset.

---

## [1.0.4-mc26.1.2+build.29] — 2026-07-28

### 🐛 SDLink Channel-Routed Chat Now Gets a Real Embed, Plus a Latent Crash Fix

- Reported live: after routing `global` chat to a specific SDLink Discord channel, messages
  arrived as a plain `PlayerName: message` text line instead of SDLink's usual styled chat
  embed (author name + skin avatar). SDLink's `DiscordMessageBuilder` has no channel-override
  API, so channel-routed messages now build the same look manually via JDA's own `EmbedBuilder`
  (author name + `mc-heads.net` avatar) instead of settling for a plain text line.
- While fixing this, found and fixed a **latent crash risk** in `SDLinkAdapter`'s pre-existing
  `sendToChannel` — it referenced SDLink's shaded JDA channel types directly inside the adapter
  class itself, the exact same category of bug that crashed a server in build.28 for
  `DCIntegrationAdapter`. Isolated into a nested `JdaBridge` class using the same pattern, so a
  pack that somehow loads `SDLinkAdapter` without SDLink actually present can no longer crash on
  startup. (This one hadn't been reported — found proactively while addressing the embed request.)
- Verified via `javap` against the compiled bytecode that `SDLinkAdapter`'s own constant pool is
  now clean of the shaded JDA types, same verification method used for the build.28 fix.
- Mc2Discord's channel-routed messages remain plain text for now (no embed path built for it
  yet) — documented on the [Chat Channels wiki page](docs/Wiki/ChatChannels.md#discord-interoperability-avoiding-duplicate--leaked-messages).

---

## [1.0.4-mc26.1.2+build.28] — 2026-07-28

### 🐛 Critical Fix: Server Crash on Startup (`NoClassDefFoundError`) From the build.27 Discord Fixes

- The Mc2Discord/DCIntegration channel-routing fixes shipped in build.27 crashed the ENTIRE
  server on startup with `NoClassDefFoundError: net/dv8tion/jda/api/entities/channel/middleman/MessageChannel`
  — reported live from a real server running that build.
- Root cause: the JVM's bytecode verifier resolves every type referenced in a class's method
  bodies (via StackMapTable frames) at class-LOAD time, not lazily on first invocation — and this
  happens regardless of any `ModList.isLoaded()` runtime guard inside that SAME class, since the
  guard is only checked once the method actually executes, long after the class (and everything
  it references) was already required to resolve. `DCIntegrationAdapter`'s new `sendToChannel`/
  `onPlayerChat` referenced JDA's `MessageChannel`/`TextChannel` types directly, and since
  `new DCIntegrationAdapter()` is constructed unconditionally in `ChatIntegrationManager.initialize()`
  on every server start, simply loading that class now required those types to be resolvable —
  which they weren't on a pack without a fully JDA-5.x-compatible DCIntegration setup — crashing
  server startup entirely.
- Fixed by isolating every direct reference to the risky external types (JDA's channel hierarchy
  in `DCIntegrationAdapter`, Discord4J's `Snowflake`/`Possible` in `Mc2DiscordAdapter`) into
  separate nested classes (`JdaChannelSender`/`Discord4jChannelSender`) that are only ever loaded
  — triggering classloading/verification of those types — at the moment a channel-specific
  message is actually sent, by which point `isReady()` has already confirmed the companion mod
  is genuinely present. Verified via `javap` against the compiled bytecode that the outer adapter
  classes' constant pools are now completely clean of the risky types.
- This is a general lesson for any future optional-dependency integration in this codebase:
  never reference a compileOnly dependency's types directly in a class that gets constructed
  unconditionally — isolate them in a class that's only loaded after a readiness check has
  already passed.

---

## [1.0.4-mc26.1.2+build.27] — 2026-07-27

### 🐛 Same Discord Channel-Routing Fix Extended to Mc2Discord and DCIntegration

- Following the SDLink fix in build.26, `Mc2DiscordAdapter#onPlayerChat` had the identical bug —
  a configured `discord.channelId` was silently ignored, always posting via Mc2Discord's own
  default chat-relay routing instead. Now routes directly to the configured channel via
  `MessageManager.createPlainTextMessage(...)` (verified against Mc2Discord's compiled bytecode:
  its 3rd `Possible<String>` parameter is NOT a channel/target override — passing it non-absent
  instead re-runs the message through Mc2Discord's own `discord_chat_format` template meant for
  console-style broadcasts; `Possible.absent()` sends the text unmodified, which is what's used
  here).
- `DCIntegrationAdapter` previously implemented no chat relay at all (by design — it relays chat
  entirely through its own vanilla-level mixins with no event NeoEssentials can hook into cleanly
  for the default case). `onPlayerChat` now activates *only* when a channel has a specific
  `discord.channelId` configured, sending directly via `JDA#getTextChannelById(...)` — additive,
  not duplicative, since a blank `channelId` still does nothing (unchanged), avoiding a
  double-post against DCIntegration's own native relay.
- Both fixes are based on reading each mod's compiled public API directly — neither Mc2Discord nor
  DCIntegration is installed in this dev environment, so unlike SDLink's fix, these were **not**
  live-tested against a running instance.
- Documented both mods' Discord-interoperability caveats (their own native "relay everything"
  behavior can still duplicate/leak messages the same way SDLink's did) on the
  [Chat Channels wiki page](docs/Wiki/ChatChannels.md#discord-interoperability-avoiding-duplicate--leaked-messages).

---

## [1.0.4-mc26.1.2+build.26] — 2026-07-27

### ✨ New `{channel}` Placeholder, Plus Real Fixes to Discord Chat Relay

- New `{channel}` / `{neoessentials_channel}` placeholder — resolves to the chat channel a
  message was sent in (`local`, `global`, `staff`, or any custom key from `chat.channels`).
  Usable in `chat-format` templates and anywhere else `{neoessentials_*}` placeholders work.
- **Fixed a real bug**: `SDLinkAdapter#onPlayerChat` completely ignored the per-channel
  `discord.channelId` NeoEssentials computed for a message — every relayed message always went
  wherever Simple Discord Link's OWN `messageDestinations.chat` config pointed, regardless of
  what channel-specific Discord ID was configured. A private staff channel with a real
  `channelId` set would still leak to SDLink's default chat channel. Now routes directly to the
  configured channel via SDLink's `sendToChannel` API when one is set (plain-text message,
  since SDLink's rich author-embed builder has no channel-override API), falling back to the
  previous default-channel behavior only when `channelId` is left blank.
- **Investigated and diagnosed the double-post issue**: Simple Discord Link has its own
  independent, config-driven "relay every chat message" feature (`chat.playerMessages` in its
  own `simple-discord-link.toml`, `true` by default) that operates completely separately from
  NeoEssentials' `chat.channels.*.discord` relay. Running both means every message NeoEssentials
  also explicitly relays gets posted twice, and a channel NeoEssentials treats as private (e.g.
  staff) still gets relayed in full by SDLink's own blanket relay — NeoEssentials has no way to
  suppress that from its side. **This is a configuration conflict, not fixable in NeoEssentials'
  own code** — NeoEssentials now logs a startup WARNING when it detects `playerMessages = true`
  while SDLink is loaded, pointing at the exact setting to disable. Verified live against a real
  SDLink instance in this dev environment — the warning fires correctly.
- Documented all of the above on the [Chat Channels wiki page](docs/Wiki/ChatChannels.md#discord-interoperability-avoiding-duplicate--leaked-messages),
  including a full explanation of the interaction and the fix.

---

## [1.0.4-mc26.1.2+build.25] — 2026-07-27

### 🐛 Fix: Chat Status Icons (AFK/Vanished/Muted) Silently Not Showing By Default

- `ChatFormatter#formatMessage` injected the clickable-player-name markers (`§HNAME§`/`§HDNAME§`,
  used for the click-to-message and click-to-view-profile features) BEFORE
  `BadgeManager#applyBadgesAndIcons` ran. Since that marker injection replaces the literal
  `{neoessentials_username}`/`{neoessentials_displayname}` tokens, and `applyBadgesAndIcons`'s
  `before_name`/`after_name` icon-position logic works by string-replacing those exact tokens,
  the tokens were already gone by the time it ran — making its replace() calls silent no-ops.
  Since `chat.clickablePlayerNames` defaults to `true` and `badges.statusIcons.iconPosition`
  defaults to `"after_name"`, this made AFK/vanished/muted status icons a no-op out of the box
  for any server using clickable names (rank badges only survived because their default
  position, `before_prefix`, never touches the username token in the first place).
- Fixed by reordering: badges/status icons are now applied to the raw template first, and the
  clickable-name markers are injected afterward.
- Documented the whole `chat.badges` config block (rank badges + status icons) on the
  [Chat System wiki page](docs/Wiki/ChatSystem.md#rank-badges--status-icons) for the first
  time, including a callout that `statusIcons.streaming` is present in config but not currently
  checked by anything (no "streaming" player status exists in the mod yet).

---

## [1.0.4-mc26.1.2+build.24] — 2026-07-27

### ✨ Click a Player's Name in Chat to Open Their Dashboard Profile

- Player names in chat (when `chat.clickablePlayerNames` is on) now carry a small clickable
  " ↗" link icon that opens the player's public, no-login moderation-lookup profile
  (`/lookup?player=<name>`) in a browser — works whether the dashboard is run in
  `webDashboard.mode` `"external"` or `"internal"`/`"both"`.
- New config: `webDashboard.publicUrl` — the admin-set public-facing base URL for the mod's
  own bundled ("internal"/"both") dashboard, since the mod can't reliably auto-detect this
  behind NAT/port-forwarding/reverse proxies. In `"external"` mode, the already-paired
  `webDashboard.externalDashboard.url` (set via `/dashboard pair`) is reused instead — no new
  setup needed there. New `chat.showProfileLinkInChat` toggle (default on); silently does
  nothing if neither URL is configured.
- New `ConfigManager.getPlayerProfileUrl(username)` centralizes the URL-building logic
  (prefers the paired external dashboard URL, falls back to the internal `publicUrl`).
- Fixed the internal (bundled React) dashboard's `/lookup` page to actually read a
  `?player=` query-string parameter and run the lookup immediately on load — it previously
  ignored the parameter entirely and only searched via the on-page form. The external
  (Laravel) dashboard's `/lookup` route already supported this server-side.

---

## [1.0.4-mc26.1.2+build.23] — 2026-07-27

### 🐛 Fix: Logging In Could Change Everyone Else's Tab-List Suffix

- `TablistManager#updatePlayerTeam` assigned a player's scoreboard team purely from their
  permission group (and optionally weight) — e.g. every "default"-group player shared one team
  named `ne_default`. A scoreboard team's prefix/suffix is one value shared by every member, so
  if any two players in that "same" team ever resolved to different actual text (a per-player
  nametag override, differing AFK state, a per-user permission grant layered on top of the
  group, etc.), whichever player's update ran last on a given tick silently overwrote what every
  OTHER member of that team displayed. New connections are appended to the end of
  `getPlayerList()`, so they're typically processed last — making a fresh login look like it
  reset everyone else's suffix to the new player's.
- Fixed by folding a hash of each player's actually-resolved prefix+suffix into the team key, so
  only players who'd show identical text ever share a team — anyone whose resolved text differs
  now gets their own team instead of clobbering someone else's. Existing sort behavior (by group
  weight) is unaffected; only the previously-shared "same group, different actual suffix" case
  changes.

---

## [1.0.4-mc26.1.2+build.22] — 2026-07-27

### ✨ Cloud Backups: Microsoft OneDrive Support

- Added OneDrive as a third cloud backup provider alongside Dropbox and Google Drive, on both
  dashboards. Same paste-a-refresh-token setup flow as Google Drive (admin registers an Azure/Entra
  app, obtains a refresh token via Microsoft's authorize+token endpoints, pastes clientId/clientSecret/
  refreshToken/uploadPath into the Backups page) — no in-app OAuth consent flow.
- Folder addressing uses a plain path string (default `/NeoEssentials-Backups`, like Dropbox's
  `uploadPath`) rather than an opaque folder ID, since Microsoft Graph supports path-based addressing
  directly.
- Uploads use Graph's upload-session + chunked-PUT flow (`createUploadSession`, then byte-range PUTs
  in 320 KiB-aligned chunks) instead of a single request, since Graph's simple upload endpoint caps
  out at 4MB — too small for most backup zips.
- New routes: `POST /api/cloud/config/onedrive`, `POST /api/cloud/test/onedrive`,
  `GET /api/cloud/files/onedrive`, `POST /api/cloud/upload/onedrive/{id}`,
  `DELETE /api/cloud/files/onedrive/{id}` (delete is ID-based, matching Google Drive's contract).

---

## [1.0.4-mc26.1.2+build.21] — 2026-07-23

### ✨ Internal Dashboard: 3D Player Renders (Matches External)

- Ported the external dashboard's 3D character renders into the internal one: the Player Profile
  page hero, the Settings page's linked-Minecraft-account row, and the Public Lookup page (which
  previously had no avatar at all) now show a full-body vzge.me render with a drop shadow and an
  animated gradient blob behind it, colors extracted live from the player's skin
  (`@universemc/react-palette`), replacing the old flat mc-heads.net face crops. Shared
  `PlayerRender` component instead of duplicated `<img>` tags.

---

## [1.0.4-mc26.1.2+build.20] — 2026-07-23

### 🐛 Fix: `/api/warps/players` Could Silently Hang the Dashboard's API Connection

- The new player-warps endpoint (build.18) resolved player names via
  `server.getProfileCache()`/`server.getPlayerList()` directly from the HTTP handler thread,
  instead of scheduling that work onto the main server thread like every other endpoint that
  touches `MinecraftServer` state (see `ServerEndpoint`'s documented convention). On a live
  server with real concurrent player activity, this could hang indefinitely with no exception
  and no response ever sent — which the external dashboard's client then reported as a general
  "API unreachable" connection failure (not just an error on the Warps page), since a stuck
  request never gets the chance to mark the connection healthy again.
- Now wrapped in the same `CompletableFuture` + 10s timeout pattern used elsewhere, so a slow or
  blocked main thread returns a clean timeout error instead of hanging the connection.

---

## [1.0.4-mc26.1.2+build.19] — 2026-07-23

### ✨ Internal Dashboard: Permissions Page Now Matches External (Collapsible Rows, Categorized Pills)

- Ported the external dashboard's Permissions page redesign into the internal (mod-bundled) one:
  groups and online users now collapse to a compact summary (name, default/group badge,
  permission count) by default instead of showing every inline edit field and every permission
  node at once — click a row to expand it for the full editor.
- Permission-node pills are now clustered under the mod's own permission catalog categories
  (falling back to "Other" for anything not in the catalog) instead of one long wrapped line of
  raw strings, with each node's description shown as a tooltip.
- Both dashboards' Permissions pages are now visually and functionally identical.

---

## [1.0.4-mc26.1.2+build.18] — 2026-07-23

### ✨ Player Warps Are Now Visible on Both Dashboards (Warps Page, Phase 1)

- The mod has always supported player-created warps (`/pwarp`, `/setpwarp`, `/delpwarp`,
  `/pwarps`), but neither dashboard could see or manage a single one of them — there was no REST
  endpoint for them at all. The Warps page on both dashboards now has a "Player Warps" tab
  alongside the existing server-warps view, listing every player who's created a warp, expandable
  to their individual warps (name, world, coordinates, created date) with an admin delete button
  per warp.
- New REST endpoints on `WarpsEndpoint`: `GET /api/warps/players` (every player's warps),
  `GET /api/warps/players/{uuid}` (one player's warps), `DELETE /api/warps/players/{uuid}/{name}`
  (admin cleanup). Unlike the existing server-warp `GET /api/warps`, **all three require admin,
  including the GET routes** — player warps are personal, not public, so listing every player's
  private warp locations needs the same gate as deleting them.
- Read/delete only in this pass — dashboards don't create a warp on a player's behalf, matching
  the in-game model where only the owning player can `/setpwarp` their own.
- This is Phase 1 of a planned three-phase Warps page expansion (player warps → stats/overview →
  search/yaw-pitch/edit-in-place CRUD polish).

---

## [1.0.4-mc26.1.2+build.17] — 2026-07-23

### 🐛 AFK Activity Tracking Now Actually Respects Its Own Config, Plus Cleanup From the Wiki Audit

- The `afk.enableActivityTracking`/`trackMovement`/`trackChat`/`trackCommands`/`trackInteractions`
  config toggles were being parsed but silently ignored — movement, commands, and block/item
  interactions always reset the AFK timer no matter what you set them to. Chat was worse: it never
  reset the AFK timer at all, regardless of `trackChat`. All five toggles are now actually enforced,
  and sending a genuine (non-muted, non-frozen) chat message correctly counts as activity.
- Fixed a `/help` registry mismatch found during the recent wiki audit: `/tpcancel`'s entry was
  registered under the wrong name (`tpacancel`), so `/help tpcancel` showed nothing and `/help
  tpacancel` showed a command that doesn't exist. Now shows correctly under its real name.
- Removed a handful of permission nodes (`neoessentials.kits.admin.*`, `neoessentials.chat.unmute`,
  `neoessentials.chat.mutelist`, `neoessentials.moderation.jail.timed`) that were registered and
  visible in `/permissions list`/`search` but never actually checked by anything — granting them did
  nothing. The commands they looked like they gated (kit admin actions, `/unmute`, `/mutelist`,
  `/jailfor`) already work off other, real nodes documented on the wiki.

## [1.0.4-mc26.1.2+build.16] — 2026-07-23

### 🐛 /help Now Shows the Real Permission for Every Command

- `/help <command>` used to guess every command's permission as
  `neoessentials.<commandname>` — wrong for roughly 160 commands whose real
  permission node is different (e.g. `/apikey` actually requires
  `neoessentials.dashboard.apikeys`, not `neoessentials.apikey`; most
  moderation/teleport/economy/item commands use a namespaced node like
  `neoessentials.moderation.ban`, not a flat one). An admin following
  `/help`'s own displayed permission to grant someone access would grant the
  wrong node and it just wouldn't work.
- Every command now carries its real permission alongside its registration,
  and `/help` displays and permission-gates against that instead of guessing.
  Commands with no permission requirement at all now correctly show "none —
  open to everyone" instead of a made-up node.

---

## [1.0.4-mc26.1.2+build.15] — 2026-07-22

### ✨ Account Settings Page + Minecraft Account Linking (Both Dashboards)

- Both dashboards' sidebar now shows your actual Minecraft skin avatar (once
  linked — see below) instead of a generic icon.
- New: a Settings/Profile page on the internal dashboard (previously missing
  entirely — the gear icon just said "not yet ported"), with password
  change, Minecraft account linking, and a read-only Discord status. The
  external dashboard's existing Profile page gains the same Minecraft
  account linking section.
- **New capability on both dashboards:** any dashboard account — however it
  was created — can now link a Minecraft account itself via a short in-game
  code (`/linkaccount <code>`), instead of needing to already have Discord
  linked in-game first. Self-service unlink too.
- Discord status is shown read-only on the internal dashboard (resolved via
  the server's own Discord bot integration) — a full browser "Connect
  Discord" button for the internal dashboard is a larger follow-up, not in
  this pass.

---

## [1.0.4-mc26.1.2+build.14] — 2026-07-22

### ✨ New Brand Logo on the Internal Dashboard

- The internal dashboard's generic gradient-badge/compass-icon placeholder is
  now the real NeoEssentials shield logo, everywhere it appears: sidebar
  header, login screen, public player-lookup page, and the browser tab
  favicon.

---

## [1.0.4-mc26.1.2+build.13] — 2026-07-22

### ✨ Dashboard Security: Copyable Keys, Encrypted Token Storage, Permission-Driven Role Sync

- `/apikey create` now prints the token as a click-to-copy chat component instead
  of plain text — click it to copy straight to your clipboard.
- The Bearer token stored for a paired external dashboard
  (`webDashboard.externalDashboard.token`) is now encrypted at rest in
  `config.json` instead of stored in plaintext. Existing plaintext values are
  transparently migrated to encrypted form the next time they're read — no
  action needed. (This mirrors the encrypted-at-rest storage the external
  Laravel dashboard already used for its own copy of the same token.)
- New opt-in feature: **permission-driven dashboard role sync**
  (`webDashboard.roleSync` in `config.json`, off by default). When enabled, a
  player who's linked a dashboard account via `/dashboardregister` automatically
  gets the dashboard `ADMIN` role the moment they have a configured in-game
  permission node or belong to a configured permission group — and loses it
  again the moment they don't, with no manual `/apikey create`/API call needed.
  Runs an immediate check on join plus a periodic sweep (default every 5
  minutes) so it also catches permission changes made outside the mod (e.g. a
  direct LuckPerms edit). A role you set manually is never touched by this —
  it only ever adjusts a role it granted itself.

---

## [1.0.4-mc26.1.2+build.12] — 2026-07-22

### ✨ Economy Tab: Overview Stats, Distribution Chart, Player Lookup + Bug Fix

- Both dashboards' Economy tab now shows total wealth, account count, and
  average/starting balance at a glance, plus a balance-distribution histogram
  (the mod already computed all of this via `/api/stats/economy` — the tab
  just never displayed it).
- The "Adjust balance" form now has a **Look up** button that shows the
  target's avatar, online status, and current balance before you commit to
  give/take/set — no more adjusting a balance blind by typing a name/UUID.
  Leaderboard entries and a successful lookup both link to that player's
  full profile page.
- **Fixed:** `/api/stats/economy` used `BigDecimal.longValueExact()` to
  bucket balances for the distribution histogram — that throws
  `ArithmeticException: Rounding necessary` for any balance with cents (i.e.
  almost every real balance), silently dropping the `distribution` field
  from the response and returning a stray `"error"` key alongside the
  otherwise-valid data. Now uses `longValue()` (which truncates instead of
  throwing) — the histogram only needs the whole-dollar bucket anyway.

---

## [1.0.4-mc26.1.2+build.13-pre-fix] — 2026-07-22

> This shipped as build.13 at the time, before the cross-branch build-number-drift
> fix below reset the counter — that number now belongs to a later, different
> entry. Kept here under this label rather than renumbered, since the original
> release artifact no longer exists to renumber against.

### ✨ Player Profile Page: Phase 4 (Final) — Sudo, Ptime/Pweather, Clear Inventory

- Fourth and final planned pass on the per-player dashboard control page. Adds:
  - **Sudo** — run a command (or send a chat message) as the player
  - **Per-player time/weather** (`/ptime`, `/pweather`) — set or reset, with the
    current override shown
  - **Clear inventory** (main + armor + offhand)
- Deliberately **not** ported: `/invseeedit` and `/enderchestedit`. Both are
  fundamentally a live in-game GUI menu opened for a physical viewer player —
  there's no equivalent for a browser tab with no in-game client. A real
  slot-by-slot inventory editor for the dashboard would be a separate,
  much larger feature (drag/drop UI, item picker, NBT editing) rather than a
  wrapper around the existing command, so it's out of scope here.
- This completes the 4-phase player-profile-page effort: game mode, permission
  group + individual overrides, economy, moderation history, freeze/vanish/jail,
  item/fun commands, and now sudo/ptime/pweather/clear-inventory — all on one
  page per player at `/players/player/<username>`.

---

## [1.0.4-mc26.1.2+build.12-pre-fix] — 2026-07-22

> Same as the note above — shipped as build.12 before the drift fix; that number
> now belongs to a later, different entry.

### ✨ Player Profile Page: Phase 3 — Items & Fun Commands

- Third pass on the per-player dashboard control page. Adds:
  - **Give item** (by registry ID, e.g. `minecraft:diamond_sword`)
  - **Potion effects** — apply by effect ID with duration/amplifier, or clear all
  - **Spawn mob** at the player's location
  - **Burn** (set on fire for N seconds), **lightning strike**, and **kill**
- All online-players-only, same as their `/command` equivalents (`/give`,
  `/effect`, `/spawnmob`, `/burn`, `/lightning`, `/kill`).
- Deliberately skipped `/skull` — it gives the *executor* a player-head item, not
  something that acts on the target player, so it doesn't fit this page's model.
- Next (and last planned) pass: inventory/ender chest editing, plus `/sudo`,
  `/clearinventory`, `/ptime`, `/pweather`.

---

## [1.0.4-mc26.1.2+build.11] — 2026-07-22

### ✨ Player Profile Page: Phase 2 — Freeze, Vanish, Jail

- Second pass on the per-player dashboard control page. Adds:
  - **Freeze/unfreeze** — works even for offline players (matches `/freeze`'s own
    UUID-keyed behavior; takes effect immediately if/when they're online).
  - **Vanish/unvanish** — vanish requires the player to be online (same as
    `/vanish`), unvanish works regardless.
  - **Jail/unjail** — a dropdown of the server's actual configured jail
    locations, since jailing requires picking one.
- New moderation REST routes backing all of the above (`/api/moderation/freeze`,
  `/vanish`, `/jail`, plus `/jails` to list configured jail locations) — GET
  routes open to any logged-in account, mutations admin-only, same convention
  as every other moderation route.
- Next pass: item & fun commands (give, effect, lightning, spawnmob), then
  inventory/ender chest editing.

---

## [1.0.4-mc26.1.2+build.10] — 2026-07-22

### ✨ Player Profile Page: Phase 1 — State Toggles, Nickname, Teleport

- First of several planned passes bringing more of the mod's ~172 commands to the
  per-player dashboard control page (`/players/player/<username>`). This pass covers
  the commands with the biggest day-to-day admin value:
  - **Fly** and **god mode** toggles
  - **Feed** and **extinguish** (separate from the existing heal action)
  - **Walk/fly speed** (0–10 scale, same as `/speed`)
  - **Nickname** set/clear (same as `/setnick`)
  - **Teleport to another online player** (the mod's teleport endpoint already
    supported this — just wasn't wired into any UI yet)
- All of the above require the target player to be online, same as their `/command`
  equivalents — the dashboard now says so plainly instead of a bare error.
- Next passes: freeze/vanish/jail, item & fun commands (give, effect, lightning,
  spawnmob), and inventory/ender chest editing.

---

## [1.0.4-mc26.1.2+build.9] — 2026-07-22

### ✨ Internal Dashboard: Player Management Overhaul + a Full Per-Player Control Page

- Players page "More" panel gained two controls that were already possible via the
  mod's API but never wired into the UI: **game mode** (survival/creative/adventure/
  spectator) and **permission group** re-assignment.
- The "Look up a player" result now shows balance, permission group, and UUID for
  **every** lookup — online or offline — plus live health/position/playtime when the
  player's online.
- New: a full per-player control page at `/players/player/<username>`, linked from
  both the "More" panel and the lookup result. One page per player with:
  - Quick actions (heal, kick, mute/unmute, ban, game mode)
  - Permission group + individual permission node overrides (add/remove)
  - Economy (balance, give/take/set)
  - Inventory (works for offline players too now, see fix below)
  - Full moderation history — bans (with unban), mutes, kicks, warnings (with
    remove), and admin notes (add/remove)
- **Fixed:** `/api/player/profile`, `/stats`, `/achievements`, `/inventory`, `/status`,
  `/health`, `/xp`, and `/location` all silently 404'd for any player who wasn't
  *currently* online, even though the underlying data collectors already supported
  reading offline player data from disk — the username→UUID lookup just never
  checked the offline profile cache. Fixed once, benefits every one of those
  endpoints.
- Also cleaned up the `/dashboard status` chat output: the header/footer separator
  lines used two different characters (`─` vs `═`) and were long enough to wrap
  awkwardly in a normal-width chat window — now one consistent, shorter line.

---

## [1.0.4-mc26.1.2+build.8] — 2026-07-22

### ✨ Internal Dashboard: Complete — Permissions and Public Lookup

- Finishes the internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with the last two pages: Permissions (groups, per-user overrides,
  inheritance, aliases, node catalog) and the public player-lookup page (no
  login required, same as the mod's own `/api/public/moderation/*` routes).
- All 13 pages now match the external `NeoEssentials-Dashboard` app's
  functionality: Overview, Players, Economy, Warps, Kits, Holograms, Discord,
  Users, Backups, Commands, Logs, Permissions, and public lookup.
- The shipped default remains `"external"` — nothing changes for servers
  already using the separate dashboard app.

---

## [1.0.4-mc26.1.2+build.8] — 2026-07-22

### ✨ Internal Dashboard: Backups, Commands, and Logs Pages

- Continues the internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Backups (create/restore/delete/download,
  plus Dropbox/Google Drive cloud upload config), Commands (run a console
  command directly), and Logs (recent join/leave/chat/command activity).
- Still not there yet: Permissions, public player lookup — coming in further
  follow-up passes.

---

## [1.0.4-mc26.1.2+build.7] — 2026-07-22

### ✨ Internal Dashboard: Holograms, Discord, and Users Pages

- Continues the internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Holograms (full CRUD), Discord (bridge
  status, account-linking config, recent events), and Users (the mod's own
  dashboard-account management).
- Fixed along the way: creating/editing a hologram through the dashboard was
  silently rejected by the mod (a body-shape mismatch on the `lines` field) —
  now works correctly.
- Still not there yet: Permissions, Backups, Commands, Logs, public player
  lookup — coming in further follow-up passes.

---

## [1.0.4-mc26.1.2+build.6] — 2026-07-22

### ✨ Internal Dashboard: Economy, Warps, and Kits Pages

- Continues build.5's internal bundled dashboard (`webDashboard.mode: "internal"`/
  `"both"`) with three more pages: Economy (leaderboard + give/take/set balance),
  Warps (list/create/delete), and Kits (read-only list + stats).
- Still not there yet: Holograms, Discord, Permissions, Backups, Commands, Logs,
  Users, public player lookup — coming in further follow-up passes.

---

## [1.0.4-mc26.1.2+build.5] — 2026-07-22

### ✨ A Real Internal Dashboard (`webDashboard.mode: "internal"`)

- `webDashboard.mode` has had an `"internal"` option for a long time, but it never
  actually worked — `src/main/resources/webdashboard/` only ever held a Laravel
  *scaffold* (raw PHP/TSX source meant to be copied into a separate project), not
  a servable `index.html`. Setting `mode` to `"internal"` or `"both"` now serves a
  real, working bundled dashboard from `/` on the mod's own port — no separate
  Laravel/PHP app required. Talks to this mod's own REST API directly.
- This first pass covers login, the server overview, and the players page (roster,
  heal/kick/ban/mute, lookup, homes). The remaining pages (economy, warps, kits,
  holograms, Discord, permissions, backups, commands, logs, users, public lookup)
  are still only available via the external `NeoEssentials-Dashboard` app for now
  — more land in follow-up updates.
- The shipped default remains `"external"` — this doesn't change anything for
  servers already using the separate dashboard app.
- Also fixed: a missing static file under `webDashboard.mode: "internal"` used to
  silently hang the connection instead of returning a 404, and there was no
  fallback for a hard-refreshed client-side route (e.g. `/players`) — both fixed
  as part of building this.

---

## [1.0.4-mc26.1.2+build.4] — 2026-07-21

### ✨ Pairing Now Includes the WebSocket Port

- `/dashboard pair` now sends `webDashboard.websocketPort` alongside the API key in its
  `POST /api/pair/complete` request, so a paired external dashboard can auto-configure a live
  WebSocket connection (server status, chat, player join/leave) instead of needing the admin
  to hand-enter that port separately. First step toward the external dashboard actually using
  the mod's existing WebSocket server, which it never has until now.

---

## [1.0.4-mc26.1.2+build.1] — 2026-07-21

### ✨ Support/Discord/GitHub Links on Startup and on Real Detected Problems

- **Console:** a quiet one-line pointer to the support site, Discord, and GitHub repo is now
  printed once every server restart, regardless of health.
- **Console (prominent) + in-game:** if a manager actually fails to initialize
  (`ManagerRegistry.getFailedCount() > 0`) or the permission system falls back to emergency
  mode, a bordered warning block prints right at the point of failure, and the first admin
  (OP or wildcard permission) to join that session gets a clickable chat message —
  `[Support]`/`[Discord]`/`[GitHub]`, each opening the link in the browser when clicked.
  Session-scoped, so it only ever fires once per restart even with multiple detected problems
  or admins joining.
- New `SupportLinks` util centralizes the three URLs (reusing the same ones already in
  README/wiki, not new links) so console and in-game code share one source of truth.

---
