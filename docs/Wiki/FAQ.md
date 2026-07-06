# NeoEssentials FAQ (raw notes — needs editing pass)

This is a working dump of Q&A content covering the NeoEssentials mod and its two dashboards
(the mod's own built-in dashboard, and the separate NeoEssentials-Dashboard Laravel app). It is
NOT meant to be published as-is — content is accurate but unpolished, order is rough, and some
answers are more technical than a general audience needs. Intended to be cleaned up/restructured
before publishing.

---

## General

**Q: What is NeoEssentials?**
A NeoForge server-essentials mod (Java 21, targets Minecraft 1.21.1, NeoForge 21.1.179+) covering
economy, moderation, chat, teleportation, kits, permissions, holograms, shops, an auction house,
tablist customization, and a built-in web dashboard — the kind of feature set EssentialsX/Essentials
covers on Bukkit/Spigot, but for NeoForge.

**Q: Is there a Fabric/Forge (non-Neo) version?**
Not covered by this repo. There is a separate `mc-26.1-port` branch targeting a newer Minecraft/
NeoForge version (26.1.2), maintained in parallel with the main 1.21.1 branch — features get
ported over manually, so the two branches can drift out of sync.

**Q: Where do I report bugs / ask questions?**
Discord: https://discord.gg/dUGAQF2Mga (per the wiki's Support section).

---

## Installation & Setup

**Q: How do I install it?**
Drop the jar into `mods/`, start the server once to generate `config/neoessentials/` files, then
configure and restart.

**Q: What config files exist?**
By default everything lives in one `config.json`. There's also a "split config" mode
(`/neoe config split`) that breaks it into `main.json`, `commands.json`, `chat.json`,
`teleportation.json`, `moderation.json`, `tablist.json`, `security.json`, `economy.json`,
`permissions.json`, `kits.json` — see the SplitConfigs wiki page. `webDashboard` settings
currently only live in the monolithic `config.json`, not migrated to a split file yet.

**Q: How do config files get updated when I update the mod?**
Every config file has a `_configVersion` number. On boot, if the file on disk is older than the
version the jar expects, the mod merges in any new keys automatically (preserving your existing
values) and bumps the version. You don't need to delete/regenerate config files after updating —
this auto-merge is why an old-looking config.json can still pick up brand-new settings after an
update.

---

## Modules & Commands (enable/disable)

**Q: How do I turn off a whole feature (like the shop system or auction house)?**
`config.json` → `modules` section has one boolean flag per major subsystem: `economyEnabled`,
`permissionsEnabled`, `kitsEnabled`, `teleportationEnabled`, `moderationEnabled`, `chatEnabled`,
`webDashboardEnabled`, `hologramsEnabled`, `shopEnabled`, `auctionHouseEnabled`, `vaultEnabled`,
`tablistEnabled`, `resourcePacksEnabled`, `playerTagsEnabled`, `discordIntegrationEnabled`. Set
any of these to `false` to disable that whole subsystem.

**Q: How do I disable just one specific command without turning off the whole module?**
`config.json` → `commands` section has one boolean per individual command (e.g. `"tempban": true`).
Set to `false` to remove just that command while leaving the rest of its module enabled.

**Q: I changed a module/command toggle and ran `/neoe reload` but nothing happened. Why?**
`/neoe reload` only refreshes config *values*, translations, and certain managers' in-memory
data (kits, homes, warps, permissions, etc.) — it does NOT add or remove already-registered
in-game commands, and it does NOT re-run the one-time startup initialization for subsystems like
the resource-pack system or Discord chat integration. Whether a command exists in the dispatcher
at all is decided once, at server boot. **You must fully restart the server** for module/command
toggle changes to take effect — reload alone will never do it. (This is now documented directly
in `config.json`'s comments and in `/neoe reload`'s own chat output.)

**Q: Which modules were missing a disable toggle before this got fixed?**
Originally only 7 modules had a flag (economy/permissions/kits/teleportation/moderation/chat/
webDashboard), and even those weren't consistently checked by every command in that module — many
individual commands ignored their own `commands.json` entry entirely. This has since been fixed:
every module now has a flag, and ~45 command files were updated to actually check both their
module flag and their individual command flag before registering. 4 new module flags were also
added for subsystems that had no kill-switch before (holograms, shop, auction house, vault), plus
4 more for tablist, resource packs, player tags/badges, and Discord integration.

**Q: I disabled the web dashboard but it's still running / still off after I re-enabled it. Why?**
The dashboard's on/off state is controlled by TWO separate keys: `webDashboard.enabled` AND
`modules.webDashboardEnabled`. Either one being `false` disables it — they used to be unrelated
(the `modules` one didn't do anything at all, a real bug that's now fixed). Also remember: a
restart is required for either change to take effect, same as any other module toggle.

---

## Storage Backends

**Q: Where does the mod store its data (bans, balances, homes, etc.)?**
Through a pluggable storage system — `config.json` → `storage.type` can be `"json"` (default,
one file per data collection), `"yaml"` (same shape, more human-editable), `"sqlite"` (single
embedded database file), or `"mysql"` (shared database — lets multiple servers in a network see
the same bans/mutes/balances/etc. in real time). Covers moderation, economy, kits, homes/warps/
spawn, jail/freeze/vanish, chat state, holograms, shops, permissions, the dashboard's own
accounts, and the Auction House.

**Q: I switched storage.type but nothing changed / my data's gone.**
Restart required (config changes generally need a restart, see above). On first boot after
switching, any old data is imported automatically and losslessly into the new backend
(`storage.autoMigrate: true`, the default) — old files are never deleted, only stopped being
written to, so nothing is actually lost even if it looks empty at first — check the logs for
"migrated N record(s)" lines confirming the import ran.

**Q: I set storage.type to "sqlite" and I'm getting a `ClassNotFoundException: org.sqlite.JDBC` error in the dev environment (`gradlew runServer`). Is that a bug?**
No — it's a known limitation specific to running the mod via Gradle's dev task, not something
that happens with a real packaged/installed jar. NeoForge's dev-mode classloader isolates each
mod's bundled dependencies to that mod's own module, and doesn't expose them the way the final
packaged jar does. SQLite/MySQL/YAML backends work fine once you're running the actual built jar
on a real server — this only affects local development testing.

**Q: Does the mod depend on any external "library mods" for SQLite/MySQL?**
No. `org.xerial:sqlite-jdbc` and MySQL's Connector/J are both bundled directly inside the mod's
own jar (JarJar), so no separate library mod install is needed. (An earlier attempt to instead
add a soft-dependency on a separate SQLite-JDBC-provider mod from Modrinth was tried and then
reverted — it "didn't work out" and NeoForge's per-mod dependency isolation makes it unnecessary
anyway, since two mods bundling their own copies of the same library don't conflict with each
other.)

---

## Moderation System

**Q: What punishment types does the mod support?**
Bans (player + IP), mutes (player + IP), kicks, warnings, staff notes, and player reports — each
with full history and an audit trail (who reversed a ban/mute and when), matching the feature set
of dedicated ban-management plugins.

**Q: Does the dashboard's ban button actually enforce the ban in-game?**
Yes — this used to be broken (dashboard bans went into a completely separate, unconnected store
from what `/ban` actually enforced, so a dashboard ban silently did nothing), but it's since been
fixed: everything (in-game commands and the dashboard) now goes through the same canonical
manager classes.

**Q: Can players report other players / leave notes without a staff member being online?**
Yes — `/report <player> <reason>` is a player-facing command (granted to everyone by default,
not staff-only) that persists even if no staff are online; staff review the queue later with
`/reports` and `/reviewreport`. `/note`/`/notes`/`/removenote` are staff-only freeform commentary
on a player's record (not shown to players).

**Q: Can the public see who's banned without logging into the dashboard?**
Yes, as of the newest addition — `/api/public/moderation/lookup/{name}` and
`/api/public/moderation/recent` are unauthenticated (no dashboard login needed) REST routes that
return a player's ban/mute/kick/warn history and a "recent activity" feed, matching how
ban-management plugins have a public transparency page. Deliberately never exposes IP bans, IP
mutes, staff notes, or reports (privacy). Can be turned off with
`webDashboard.securitySettings.publicModerationLookupEnabled: false`.

---

## Web Dashboard (the mod's own built-in dashboard)

**Q: What is this, exactly?**
A built-in HTTP server the mod itself runs (default port 8080, WebSocket on 8081) — no external
software required. Different from the separate `NeoEssentials-Dashboard` Laravel web app (see
below), which is a standalone project that talks to this same API from the outside.

**Q: How do I turn it on/off?**
`config.json` → `webDashboard.enabled` and `modules.webDashboardEnabled` (both need to be `true`
for it to run) plus `webDashboard.autoStart` (whether it starts automatically on server boot, vs.
needing `/dashboard start`).

**Q: If autoStart is off, how do I start it without restarting the whole server?**
`/dashboard start` (requires `neoessentials.admin.dashboard`). Also: `/dashboard stop`,
`/dashboard restart`, `/dashboard status`, `/dashboard url`.

**Q: How do I get a dashboard account?**
Several ways: (1) `/dashboardregister start` then `/dashboardregister complete <username>
<password>` in-game (or `/dashboardregister discord` if Simple Discord Link is linked); (2) the
default `admin`/`admin123` account auto-created on first boot (change the password immediately);
(3) log in with just your Minecraft username (no password) if you have
`neoessentials.dashboard.access` — auto-creates an account for you (this path is functionally
fine but marked "deprecated" in logs, since `/dashboardregister` was meant to replace it);
(4) Discord OAuth if configured, auto-registers on first login; (5) an existing admin creates an
account for you via the dashboard's Users page or the API directly.

**Q: `/dashboardregister` said "Unknown command" for me. Bug?**
Was a real bug (not a documentation error) — the command class existed and compiled fine, but
was never actually registered with the game's command system, so it plain didn't exist as far as
the server was concerned. Fixed. Requires a server restart to take effect once you update to a
build with the fix (see the "reload doesn't add commands" answer above — this is the same
underlying limitation).

**Q: What can the dashboard actually do?**
Server stats/TPS/memory (Overview), online players + kick/ban/teleport actions (Players),
send commands + view logs (Console), server admin tools (Admin Controls), manage permission
groups/nodes (Permissions), edit config files directly in-browser (Config) — plus a full
moderation REST API (ban/mute/kick/warn/note/report CRUD), Discord OAuth login, and file-version
auto-updates for its own frontend assets.

**Q: Is the dashboard's REST API documented?**
Yes, on the wiki's WebDashboard page — routes like `/api/moderation/*`, `/api/public/moderation/*`
(no login), `/api/auth/*`, `/api/permissions/*`, etc. All routes except the public moderation
lookup ones require a Bearer token (`Authorization: Bearer <sessionId>`); mutating routes
additionally require moderator/admin role.

---

## The separate NeoEssentials-Dashboard (Laravel) web app

**Q: What's the difference between this and the mod's built-in dashboard?**
`NeoEssentials-Dashboard` is a completely separate project (its own git repo, a Laravel + Inertia +
React app) that runs independently (e.g. via `php artisan serve`, default `127.0.0.1:8000`) and
talks to the mod's built-in dashboard API from the outside, using a shared "service account"
login rather than per-visitor Minecraft-linked sessions. It's a nicer/more customizable frontend
built on top of the same API, not a replacement for the mod's own dashboard server.

**Q: Does it have its own accounts, separate from the mod's dashboard accounts?**
Yes — this app has its own `users` table/auth system (Laravel's own login/register pages) for who
can access ITS pages, completely separate from the mod's own `dashboard_users` (the "Mod dashboard
accounts" section under `/users` in this app is for managing the MOD's accounts, from within
this app, as an admin feature — two distinct user systems).

**Q: I heard the landing/login pages got redesigned. What changed?**
The dashboard has its own dark "mining/ore control panel" design system (copper/moss/ember
accent colors, Space Grotesk display font) already built for its authenticated pages — but the
public-facing pages (landing page, login, register) were still 100% unbranded stock Laravel
scaffolding, because the theme's CSS file was never actually imported into the build (a real,
separate bug). Fixed: the theme now loads everywhere, and the landing/login/register pages were
redesigned to match.

**Q: Is there a public player-lookup page in this app too?**
Yes, `/lookup` — a public (no login required either from this app or from the mod's API), with a
search box and a "recent activity" feed, calling the mod's `/api/public/moderation/*` routes.
Reachable from both the landing page nav and, when logged in, the dashboard's own sidebar.

---

## Permissions

**Q: What permission systems are supported?**
NeoEssentials' own internal permission manager (default), or external LuckPerms/FTB Ranks if
either is installed (auto-detected). `/permissions` (alias `/pex`) manages groups/nodes when
using the internal system.

---

## Economy

**Q: What backs the in-game economy?**
Player balances, a pay-toggle (opt out of receiving `/pay`), transaction history, and per-item
"worth" (sell prices) — all persisted through the same pluggable storage system described above.
Vault API bridge available for other mods/plugins expecting a Vault economy provider (can be
turned off via `modules.vaultEnabled`).

---

## Known Issues / Recently Fixed (worth mentioning so people don't file duplicate bug reports)

- `/dashboard` and `/dashboardregister` were completely unregistered (dead code) until recently —
  fixed, but needs a restart to take effect on any given server.
- `modules.webDashboardEnabled` used to be a completely dead config key (checked nowhere) — fixed,
  it now actually gates the dashboard alongside `webDashboard.enabled`.
- Most individual command toggles in `commands.json` used to be silently ignored by their own
  command's registration code — fixed for ~45 files; if you still find a command that ignores its
  toggle, it's a candidate for a bug report.
- `storage.type: "sqlite"`/`"mysql"` throwing `ClassNotFoundException` is expected in a local
  `gradlew runServer` dev environment only — not a bug, not reproducible on a real installed jar.
- The Auction House used to run its own separate, bespoke SQLite database regardless of
  `storage.type` — fixed, it now uses the same pluggable backend as everything else.
- Two dead duplicate command classes (an old singular `HomeCommand`/`SpawnCommand`, superseded by
  the real `HomeCommands`/`SpawnCommands`) were found and removed — if old documentation or
  forum posts mention a `/home`/`/spawn` behavior that doesn't match reality, this is why.
- A resource-pack-management feature (`resourcepacks.ResourcePackManager`, upload/host named
  resource packs with full CRUD) exists in the codebase with working storage persistence, but is
  currently not wired up to anything (not the dashboard, not any command) — effectively dormant,
  distinct from the smaller "auto-send a badge resource pack on join" feature that IS live.

---

*Raw notes end here — please restructure into a clean, navigable FAQ page, group related
questions, trim anything too implementation-detail-y for an end-user audience, and verify against
the live wiki pages (docs/Wiki/*.md) for anything not covered above.*
