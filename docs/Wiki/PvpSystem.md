# PvP System

> **Version:** 1.0.6+build.70 · **Config:** `config.json` → `pvp` section (plus `teleportation.generalSettings.combatLockPvpOnly`)

---

## Overview

Server-wide and per-player PvP control (a PvPManager-plugin-style port): a global on/off
switch, mutual per-player opt-out, temporary newbie-protection immunity for new players, and
safe-zone integration reusing the same YAWP region list teleportation already uses to block
teleports. A blocked hit is cancelled cleanly and messages both players instead of silently
going through.

---

## Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/pvp` | `/pvp` | `neoessentials.pvp.status` | Show global PvP status and your own opt-out/newbie-protection state |
| `/pvp on` | `/pvp on` | `neoessentials.pvp.admin` | Enable PvP server-wide, broadcasts the change to all online players |
| `/pvp off` | `/pvp off` | `neoessentials.pvp.admin` | Disable PvP server-wide, broadcasts the change to all online players |
| `/pvp toggle` | `/pvp toggle` | `neoessentials.pvp.toggle` | Opt yourself in/out of PvP — mutual, so an opted-out player can neither hit nor be hit |

> Ops (`hasPermission(2)`) can always use every subcommand regardless of the permission nodes above.

---

## Config (`config.json` → `pvp`)

| Key | Default | Description |
|---|---|---|
| `enabledByDefault` | `true` | Startup default for the global switch — only used the very first time the server runs with no persisted state yet. Runtime state (set via `/pvp on\|off`) is persisted separately and survives restarts. |
| `allowPerPlayerToggle` | `true` | Let individual players opt themselves out via `/pvp toggle`, even while the server-wide switch is on |
| `notifyOnBlockedHit` | `true` | Message both the attacker and the target whenever a hit is blocked, instead of silently negating the damage |
| `newbieProtection.enabled` | `true` | Grant new players a temporary mutual PvP immunity window |
| `newbieProtection.durationSeconds` | `60` | Length of that immunity window, starting from the first time this system has ever seen the player (independent of vanilla's own "has played before" flag) |
| `safeZoneIntegration.enabled` | `true` | Also block PvP between two players when either stands inside a region listed in `teleportation.generalSettings.protectedAreas` (requires YAWP) |

`modules.pvpEnabled` (default `true`) gates the whole subsystem — set to `false` to disable
`/pvp` entirely and skip registering its event listener (restart required, same as every other
module toggle).

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `neoessentials.pvp.status` | ✅ (default group) | View global/own PvP status via bare `/pvp` |
| `neoessentials.pvp.toggle` | ✅ (default group) | Opt yourself in/out via `/pvp toggle` |
| `neoessentials.pvp.admin` | 🔒 (moderator+) | Toggle the server-wide switch via `/pvp on\|off` |

---

## How a hit gets blocked

`PvpManager.canFight(attacker, target)` gates every player-vs-player hit through three
independent checks, in this order — the first one that fails blocks the hit:

1. **Global switch** — `/pvp off` blocks everyone, no exceptions.
2. **Per-player opt-out** (if `allowPerPlayerToggle` is enabled) — either player having opted
   out via `/pvp toggle` blocks the hit. This is mutual by design: an opted-out player can
   neither deal nor take PvP damage, closing the obvious risk-free-griefing loophole an
   attack-only opt-out would leave open.
3. **Newbie protection / safe zones** — either player being inside their newbie-protection
   window, or either player standing in a configured safe zone, blocks the hit.

A blocked hit is cancelled via `LivingDamageEvent.Pre#setNewDamage(0f)` (the same mechanism
God Mode and AFK invulnerability already use) — the attack visually connects but deals zero
damage. If `notifyOnBlockedHit` is enabled, the attacker and target each get a message
(rate-limited per attacker to roughly one every 2 seconds, so holding down attack against a
protected target can't spam their chat).

---

## Combat-lock interaction (`/spawn`, `/home`, `/warp`, `/tpa`, `/back`)

NeoEssentials already blocks teleportation while a player is "in combat" (see
`teleportation.generalSettings.allowTeleportInCombat` in [Teleportation
System](TeleportationSystem)) — that check is independent of this PvP system and, by default,
fires on **any** combat (PvP or PvE). If you're seeing players get locked out of teleporting by
something that clearly isn't PvP — a mob farm/grinder mod, for example — see
[Teleportation System → Combat Lock](TeleportationSystem#combat-lock) for
`combatLockPvpOnly`, which restricts that lock to real player-vs-player hits only.

---

*Back to [Wiki Home](Home)*
