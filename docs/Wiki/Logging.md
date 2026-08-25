# Logging System

> **Version:** 1.0.4+build.44 · **Config:** `config.json` → `logging` section

---

## Overview

NeoEssentials logs through a per-subsystem system instead of one all-or-nothing debug switch.
Every log call in the mod is routed through one of 12 **categories**, and each category has two
independent toggles:

```json
"logging": {
  "categories": {
    "chat":          { "normal": true, "debug": false },
    "economy":       { "normal": true, "debug": false },
    "permissions":   { "normal": true, "debug": false },
    "teleportation": { "normal": true, "debug": false },
    "moderation":    { "normal": true, "debug": false },
    "auctionHouse":  { "normal": true, "debug": false },
    "kits":          { "normal": true, "debug": false },
    "webDashboard":  { "normal": true, "debug": false },
    "discord":       { "normal": true, "debug": false },
    "config":        { "normal": true, "debug": false },
    "commands":      { "normal": true, "debug": false },
    "general":       { "normal": true, "debug": false }
  }
}
```

| Toggle | Default | Where it shows up | What it covers |
|---|---|---|---|
| `normal` | `true` | Console + `logs/latest.log` | Routine, non-error messages for that category (e.g. "migrated N transaction records") |
| `debug` | `false` | `logs/debug.log` only | Verbose tracing — request/decision/outcome for that category's key operations |

**Warnings and errors are never gated by either toggle.** Turning a category off can only
silence its routine chatter or opt-in verbose tracing — a real problem in that subsystem will
always be logged, so you can't accidentally hide an actual bug by disabling a category.

`debug` messages don't need any extra setup to reach `logs/debug.log` — the platform's default
Log4j2 configuration already routes DEBUG-level output there, separately from the INFO+ level
that reaches the console and `logs/latest.log`. There's no NeoEssentials-specific log file.

---

## Categories

| Category | Config key | Covers |
|---|---|---|
| Chat | `chat` | Chat formatting, channels, anti-spam, badges/status icons, mute/msg-toggle/ignore state, AFK |
| Economy | `economy` | Balances, pay, transactions, worth/sell, economy modifiers |
| Permissions | `permissions` | Internal permission system, LuckPerms/FTB Ranks/Bukkit-Sponge adapters, permission resolution |
| Teleportation | `teleportation` | Homes, warps, spawn, `/tpa`, `/back`, random teleport, direct/admin teleports |
| Moderation | `moderation` | Bans, kicks, jails, warns, freezes, vanish |
| Auction House | `auctionHouse` | Listing create/bid/buy/cancel/expire lifecycle |
| Kits | `kits` | Kit claim eligibility checks and grants |
| Web Dashboard | `webDashboard` | HTTP request handling, authentication, analytics, backups, cloud storage, the map/websocket features |
| Discord | `discord` | Chat bridge adapters (SDLink, Mc2Discord, DCIntegration) |
| Config | `config` | Config file load/migration/version-check/split-config decisions |
| Commands | `commands` | Command registration/dispatch tracing, **and** (as of build.61) a "PlayerName issued command: /..." console line for every player command — set `commands.normal: false` to turn that line off |
| General | `general` | Everything else: database/storage, scheduler, security, items, inventory, holograms, tags, teams, resource packs, shop, vault, localization |

---

## Example: Debugging a Broken Warp

Flip on verbose tracing for just teleportation, without touching anything else:

```json
"teleportation": { "normal": true, "debug": true }
```

`/neoessentials reload`, reproduce the issue, then check `logs/debug.log` for a trace of the
warp request — target resolution, permission/cooldown checks, and the actual teleport
execution. Turn it back off once you're done; leaving `debug` on for a busy category can add a
meaningful amount of log volume over time.

---

## Migrating from the Old Global Toggle

Older configs used a single `logging.enableDebugLogging` boolean. On first boot after
updating, this is migrated automatically:

- If `enableDebugLogging` was `true`, every category's `debug` flag is seeded to `true`, so you
  don't silently lose verbose output you were relying on — you can then dial individual
  categories back down.
- If it was `false` or unset, categories are simply populated with their defaults above.
- The old key is left on disk (unused, harmless) rather than removed.

This migration is one-time — once `logging.categories` exists in your config, it's yours to
edit freely.
