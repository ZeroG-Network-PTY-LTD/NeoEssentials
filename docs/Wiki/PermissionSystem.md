# NeoEssentials — Permission System

> **Last updated:** 2026-04-01 · **Version:** 1.0.2.6+build.28
> **Source of truth:** `PermissionRegistry.registerAllPermissions()` in the mod source.  
> All nodes listed here are **actively registered** and recognised by the permission engine.  
> Nodes marked `✅ default` are granted to every player automatically (including non-OP).  
> Nodes marked ` op-only` require explicit grant or OP level 2+ unless overridden.

---

## Table of Contents
1. [Configuration](#configuration)
2. [How Permissions Work](#how-permissions-work)
3. [Group Priorities](#group-priorities)
4. [Temporary Permissions](#temporary-permissions)
5. [Contextual Permissions](#contextual-permissions)
6. [Permission Conditions](#permission-conditions)
7. [Permission Aliases](#permission-aliases)
8. [API for Other Mods](#api-for-other-mods)
9. [Wildcards & Inheritance](#wildcards--inheritance)
10. [Dynamic Nodes](#dynamic-nodes)
11. [Permission Nodes — Full Reference](#permission-nodes--full-reference)
    - [Core](#core)
    - [Economy](#economy)
    - [Teleportation](#teleportation)
    - [Kits](#kits)
    - [Items](#items)
    - [Chat & Messaging](#chat--messaging)
    - [Moderation](#moderation)
    - [Miscellaneous Utilities](#miscellaneous-utilities)
    - [Admin & Config](#admin--config)
    - [Permission System Commands](#permission-system-commands)
    - [Web Dashboard](#web-dashboard)
12. [Example groups.json](#example-groupsjson)
13. [External Permission Mods](#external-permission-mods)

---

## Configuration

**`config.json` → `permissions` section:**

| Key | Default | Description |
|---|---|---|
| `useExternalPermissions` | `false` | Use LuckPerms / FTB Ranks instead of built-in engine |
| `defaultGroup` | `"default"` | Group assigned to new players |
| `opsBypassPermissions` | `true` | OPs (level 2+) bypass all permission checks **before** they are evaluated (fast path) |
| `vanillaOpFallback` | `true` | OPs (level 2+) are granted access as a **last resort after** all permission systems have been consulted — prevents lockouts when configs are corrupted or external mods crash |
| `cachePermissions` | `true` | Cache permission lookups for performance |
| `permissionCacheExpiryMinutes` | `5` | How long cached results are valid |

> **`opsBypassPermissions` vs `vanillaOpFallback`**
>
> | Setting | When it fires | Effect |
> |---|---|---|
> | `opsBypassPermissions: true` | **Before** any permission check | OPs always get in, permission system not consulted at all |
> | `vanillaOpFallback: true` | **After** all checks return false | OP gets in only when every other system denied or failed — a safety net, not a bypass |
>
> Most servers should keep **both** `true` (the defaults).
> Set `opsBypassPermissions: false` + `vanillaOpFallback: true` if you want OPs to be subject to normal permission management in LuckPerms/FTB Ranks, while still having a lockout-prevention net for when those systems fail.

**Permission data file:** `neoessentials/permissions.json`

---

## How Permissions Work

1. When a player runs a command, `PermissionValidator.validatePermission()` is called.
2. It checks `PermissionAPI.hasPermission(uuid, node)`.
3. `PermissionAPI` checks in this order:
   1. **Emergency mode** — if the permission system failed to initialise at startup, all checks immediately answer `true` for OPs and `false` for everyone else.  Run `/neoe reload` once the config is fixed to exit this mode.
   2. **`opsBypassPermissions`** — if enabled and the player is OP (level 2+), `true` is returned immediately without consulting any permission system.
   3. **External adapter** (LuckPerms / FTB Ranks) — if configured and healthy, its answer is used.  If it throws an exception or is marked unhealthy (5+ consecutive failures) execution falls through.
   4. **Internal `permissions.json`** — the built-in group/user engine is consulted.  Checks explicit grants, group membership, wildcard nodes, and inheritance.
   5. **`vanillaOpFallback`** — if enabled and the player is OP (level 2+), `true` is returned as a last resort regardless of what steps 3–4 said.  This fires only when *all* other systems returned `false` or failed.
4. If denied, the player sees:
   ```
   You don't have permission to use this command.
   §7Required: §f<node>
   §8(<human-friendly description of the node, if registered>)
   ```

---

## Group Priorities

Every group has an integer `priority` field (default `0`). When a group inherits from multiple parent groups, the parents are checked in **descending priority order** — the highest-priority group in the inheritance chain is consulted first.

**Why it matters:** Negative permissions (e.g. `-neoessentials.teleport.tpr`) and positive grants in a higher-priority parent win over lower-priority parents. Without explicit priorities the check order is unspecified.

**Commands:**

| Command | Description |
|---|---|
| `/permissions group <name> setpriority <value>` | Set priority (−999 to 999). Requires `neoessentials.permissions.group.modify` |
| `/permissions group <name> getpriority` | Read current priority. Requires `neoessentials.permissions.info.group` |
| `/permissions info group <name>` | Shows all group details including current priority |

**Example:** Group `vip` (priority 10) and group `donor` (priority 5) both inherited by `vip-donor`:
- Permissions are checked: `vip-donor` own → `vip` (priority 10) → `donor` (priority 5) → `default` (priority 0)

**Typical priority scale:**

| Priority | Typical role |
|---|---|
| 100+ | admin / owner |
| 50 | moderator |
| 10 | vip / donor |
| 1 | trusted |
| 0 | default (unset) |
| −1 to −999 | restricted / muted helper groups |

---

## Temporary Permissions

Temporary permissions are time-limited grants that **expire automatically** — no manual revocation needed. They work for both individual players and groups.

### Duration format

Combine `d` (days), `h` (hours), `m` (minutes), `s` (seconds) in any order:

| Duration string | Meaning |
|---|---|
| `30m` | 30 minutes |
| `12h` | 12 hours |
| `1d` | 1 day |
| `7d` | 7 days |
| `1d12h30m` | 1 day, 12 hours, 30 minutes |
| `2h30m15s` | 2 hours, 30 minutes, 15 seconds |

### Commands — users

| Command | Permission required | Description |
|---|---|---|
| `/permissions user <player> addtemp <node> <duration>` | `neoessentials.permissions.user.temp` | Grant a temporary permission |
| `/permissions user <player> removetemp <node>` | `neoessentials.permissions.user.temp` | Revoke a temporary permission early |
| `/permissions user <player> listtemp` | `neoessentials.permissions.info.user` | List all active temp permissions with time remaining |

### Commands — groups

| Command | Permission required | Description |
|---|---|---|
| `/permissions group <name> addtemp <node> <duration>` | `neoessentials.permissions.group.temp` | Grant a temporary permission to every member of the group |
| `/permissions group <name> removetemp <node>` | `neoessentials.permissions.group.temp` | Revoke a temporary group permission early |
| `/permissions group <name> listtemp` | `neoessentials.permissions.info.group` | List all active group temp permissions with time remaining |

### How it works

1. The expiry timestamp (UTC epoch-ms) is stored alongside the permission node.
2. **Resolution order** — temp permissions are evaluated *after* negative-permission (`-node`) denials but *before* regular user and group permissions. An explicit `-node` deny still wins.
3. **Auto-expiry** — a server-tick handler runs every 30 seconds. When an entry's timestamp has passed:
   - The entry is removed from memory and from disk (`playerdata.json` / `permissions.json`).
   - If the affected player is online they receive the chat message `§eYour temporary permission §f<node>§e has expired.`
   - The expiry is written to the audit log with executor `SYSTEM` and action `USER_TEMP_PERM_EXPIRED` / `GROUP_TEMP_PERM_EXPIRED`.
4. **Restart safety** — temp permissions are persisted on disk. On reload, only entries whose expiry is still in the future are loaded; expired entries are silently discarded.

### Worked example

```
# Grant a player event-only creative permissions for 2 hours:
/permissions user Steve addtemp neoessentials.fly 2h
/permissions user Steve addtemp neoessentials.gamemode.creative 2h

# Give the vip group a bonus ability for 24 hours:
/permissions group vip addtemp neoessentials.fly 1d

# Check what's active:
/permissions user Steve listtemp
# → neoessentials.fly          — expires in 1h 58m 32s
# → neoessentials.gamemode.creative — expires in 1h 58m 32s
```

### Audit log events

| Action constant | Trigger |
|---|---|
| `USER_TEMP_PERM_ADDED` | `/permissions user <p> addtemp` |
| `USER_TEMP_PERM_REMOVED` | `/permissions user <p> removetemp` |
| `USER_TEMP_PERM_EXPIRED` | Auto-expiry engine (executor = `SYSTEM`) |
| `GROUP_TEMP_PERM_ADDED` | `/permissions group <g> addtemp` |
| `GROUP_TEMP_PERM_REMOVED` | `/permissions group <g> removetemp` |
| `GROUP_TEMP_PERM_EXPIRED` | Auto-expiry engine (executor = `SYSTEM`) |

---

## Contextual Permissions

Contextual permissions are per-world, per-gamemode, or time-of-day overrides layered **on top of** the regular permission resolution chain. They let you do things like:

- Give players `neoessentials.fly` only while they are in a creative-mode world.
- Deny `neoessentials.chat.color` during night-time on a hardcore-PvP server.
- Grant VIP `neoessentials.teleport.tpr` only in the Overworld.

### Resolution order with context

When a `PermissionContext` is supplied the resolution chain becomes:

1. **Context deny** (user) → return `false`
2. **Context deny** (group, inheritance-aware) → return `false`
3. Regular negative deny (user) → return `false`
4. Regular negative deny (group) → return `false`
5. User temp-permission grant → return `true` (then evaluate condition)
6. **Context grant** (user) → return `true` (then evaluate condition)
7. Regular user permission grant → return `true` (then evaluate condition)
8. **Context grant** (group, inheritance-aware) → return `true` (then evaluate condition)
9. Regular group permission grant → return `true` (then evaluate condition)

Context **denies always win** over regular grants — they cannot be overridden from below.

### Supported context keys

| Key | Meaning |
|---|---|
| `world:overworld` | Player is in the Overworld |
| `world:the_nether` | Player is in the Nether |
| `world:the_end` | Player is in the End |
| `world:<dim-path>` | Any custom dimension by path (e.g. `world:mymod/dim1`) |
| `time:day` | Server day-time ticks 0–12 999 |
| `time:night` | Server day-time ticks 13 000–23 999 |
| `gamemode:survival` | Player is in Survival |
| `gamemode:creative` | Player is in Creative |
| `gamemode:adventure` | Player is in Adventure |
| `gamemode:spectator` | Player is in Spectator |

All keys have tab-completion in `/permissions`.

### Commands — groups

| Command | Permission | Description |
|---|---|---|
| `/permissions group <g> context add <contextKey> <node> allow` | `neoessentials.permissions.group.context` | Grant node in context |
| `/permissions group <g> context add <contextKey> <node> deny` | `neoessentials.permissions.group.context` | Deny node in context |
| `/permissions group <g> context remove <contextKey> <node>` | `neoessentials.permissions.group.context` | Remove override |
| `/permissions group <g> context list` | `neoessentials.permissions.group.context` | List all overrides |

### Commands — users

| Command | Permission | Description |
|---|---|---|
| `/permissions user <p> context add <contextKey> <node> allow` | `neoessentials.permissions.user.context` | Grant node in context |
| `/permissions user <p> context add <contextKey> <node> deny` | `neoessentials.permissions.user.context` | Deny node in context |
| `/permissions user <p> context remove <contextKey> <node>` | `neoessentials.permissions.user.context` | Remove override |
| `/permissions user <p> context list` | `neoessentials.permissions.user.context` | List all overrides |

### Worked example

```
# Give the vip group fly only in the creative world:
/permissions group vip context add world:creative_world neoessentials.fly allow

# Deny everyone chat colours during night-time:
/permissions group default context add time:night neoessentials.chat.color deny

# Give a player creative-only permissions, then check them:
/permissions user Steve context add gamemode:creative neoessentials.more allow
/permissions user Steve context list
```

### Storage

Contextual overrides are saved in:
- Groups → `neoessentials/permissions.json` under `"contextualPermissions"`
- Users → `neoessentials/permissions/playerdata.json` under `"contextualPermissions"`

Existing files without the key are treated as having no overrides — fully backward-compatible.

### Audit log events

| Action constant | Trigger |
|---|---|
| `USER_CONTEXT_PERM_ADDED` | User context `add … allow/deny` |
| `USER_CONTEXT_PERM_REMOVED` | User context `remove` |
| `GROUP_CONTEXT_PERM_ADDED` | Group context `add … allow/deny` |
| `GROUP_CONTEXT_PERM_REMOVED` | Group context `remove` |

---

## Permission Conditions

Conditions are optional runtime expressions attached to a permission node. When the permission would otherwise be **granted**, the condition is re-evaluated against the player's current context. If the condition fails the grant is withheld (the player does **not** see a "no permission" message — the permission is simply absent at that moment).

> Conditions are a **secondary filter** on top of the regular grant. They cannot make a denied permission succeed.

### Condition syntax

```
atom     ::= "time:day" | "time:night"
           | "world:<name>"           e.g. world:overworld
           | "gamemode:<mode>"        e.g. gamemode:survival
           | "health:above:<0-20>"    e.g. health:above:10
           | "health:below:<0-20>"    e.g. health:below:5
           | "op:true" | "op:false"
compound ::= atom (" AND " atom)*    — ALL atoms must match
           | atom (" OR "  atom)*    — ANY atom must match
```

### Examples

| Expression | Passes when… |
|---|---|
| `time:day` | Server time is day |
| `gamemode:survival AND time:day` | Player is in Survival AND it is day |
| `world:overworld OR world:the_nether` | Player is in Overworld or Nether |
| `health:above:10` | Player has more than 10 HP |
| `op:false` | Player is **not** OP |

### How to set a condition

Conditions are set programmatically via `PermissionsService` (see [API for Other Mods](#api-for-other-mods)) or directly via the `PermissionUser`/`PermissionGroup` Java API. In-game `/permissions condition` commands are planned for a future build.

### Storage

Conditions are persisted under `"conditions"` in the same JSON files as regular permissions.

---

## Permission Aliases

The alias system maps **short or legacy node names** to their canonical NeoEssentials equivalents. Aliases are resolved transparently **before every permission check** — neither the player nor the admin needs to know whether a legacy name was used.

### Storage

> **Storage note:** As of the DataStore migration, aliases are persisted through the active **DataStore** (see [Storage Backend](Storage)) in a `permission_aliases` collection — one record per alias, each holding `alias` and `target` fields. With the default `json` backend this lives at `neoessentials/store/permission_aliases.json`; with `sqlite`/`mysql` it's a table in the configured database.
>
> `config/neoessentials/permission_aliases.json` is now only consulted for a **one-time legacy migration**: if the `permission_aliases` collection is empty on startup, any aliases found in that file are imported and the collection becomes the source of truth from then on. Editing the legacy file after that point has no effect.

```json
{
  "essentials.fly"      : "neoessentials.fly",
  "essentials.warp"     : "neoessentials.teleport.warp",
  "essentials.home"     : "neoessentials.teleport.home",
  "efly"                : "neoessentials.fly",
  "mymmod.feature"      : "neoessentials.admin"
}
```

- Aliases are loaded from the DataStore on start and on `/permissions reload`.
- Empty collection (and no legacy file to migrate) → no aliases active (silently ignored).
- Changes made via the API or `/permissions` commands are written straight to the DataStore.
- Aliases are **one-directional** (alias → canonical). The canonical node itself is never affected.

### Registering aliases via API

```java
NeoEssentialsAPI.getPermissionsService().registerAlias("essentials.fly", "neoessentials.fly");
```

See [API for Other Mods](#api-for-other-mods) for the full API reference.

---

## API for Other Mods

Other NeoForge mods can interact with NeoEssentials' permission system using the clean
`PermissionsService` interface — no need to import internal NeoEssentials classes.

### Getting the service

```java
import com.zerog.neoessentials.api.NeoEssentialsAPI;
import com.zerog.neoessentials.api.permissions.PermissionsService;
import com.zerog.neoessentials.permissions.PermissionContext;

PermissionsService perms = NeoEssentialsAPI.getPermissionsService();
```

### Permission checks

```java
// Simple check (no context)
boolean canFly = perms.hasPermission(player.getUUID(), "neoessentials.fly");

// Convenience overload — builds context automatically
boolean canFly = perms.hasPermission(player, "neoessentials.fly");

// Full context-aware check
PermissionContext ctx = perms.contextFor(player);
boolean granted = perms.hasPermission(player.getUUID(), "mymod.feature", ctx);
```

### Registering your mod's permission nodes

Registered nodes appear in `/permissions search` and tab-completion:

```java
// Single node
perms.registerPermission("mymod.feature.use", "Enables the cool feature");

// Bulk
perms.registerPermissions(Map.of(
    "mymod.admin",   "Admin access",
    "mymod.edit",    "Edit items",
    "mymod.view",    "View items"
));
```

### Registering aliases

```java
perms.registerAlias("mymod-legacy.feature", "mymod.feature.use");
```

### Querying player info

```java
String group  = perms.getGroup(player.getUUID());   // e.g. "vip"
String prefix = perms.getPrefix(player.getUUID());  // e.g. "§6[VIP] "
String suffix = perms.getSuffix(player.getUUID());  // e.g. " §7✦"
Set<String> nodes = perms.getPlayerPermissions(player.getUUID());
Collection<String> groups = perms.getGroupNames();
```

### System state

```java
boolean emergency = perms.isEmergencyMode();        // true = OP-fallback-only mode
boolean external  = perms.isUsingExternalAdapter(); // true = LuckPerms/FTB Ranks active
```

### Full method reference

| Method | Description |
|---|---|
| `hasPermission(UUID, String)` | Check permission (no context) |
| `hasPermission(UUID, String, PermissionContext)` | Check permission with context |
| `hasPermission(ServerPlayer, String)` | Convenience — builds context from live player |
| `getGroup(UUID)` | Get player's permission group name |
| `getPrefix(UUID)` | Get player's display prefix |
| `getSuffix(UUID)` | Get player's display suffix |
| `registerPermission(String, String)` | Register a single permission node |
| `registerPermissions(Map<String, String>)` | Bulk-register permission nodes |
| `registerAlias(String, String)` | Register an alias |
| `getAliases()` | Get all current aliases |
| `isEmergencyMode()` | Query emergency mode state |
| `isUsingExternalAdapter()` | Query external adapter state |
| `getGroupNames()` | Get all group names |
| `getPlayerPermissions(UUID)` | Get a player's direct permission nodes |
| `contextFor(ServerPlayer)` | Build a `PermissionContext` from a live player |

---

## Wildcards & Inheritance

| Wildcard | Grants access to |
|---|---|
| `neoessentials.*` | Every permission in the mod |
| `neoessentials.economy.*` | All economy nodes |
| `neoessentials.teleport.*` | All teleport nodes |
| `neoessentials.teleport.admin.*` | All admin-teleport nodes |
| `neoessentials.teleport.home.*` | All home nodes |
| `neoessentials.teleport.request.*` | All TPA request nodes |
| `neoessentials.teleport.spawn.*` | All spawn nodes |
| `neoessentials.teleport.warp.*` | All warp nodes |
| `neoessentials.kits.*` | All kit nodes |
| `neoessentials.item.*` | All item management nodes |
| `neoessentials.chat.*` | All chat nodes |
| `neoessentials.moderation.*` | All moderation nodes |
| `neoessentials.permissions.*` | All permissions-command nodes |
| `neoessentials.spawner.*` | Change a spawner to **any** mob type |
| `neoessentials.fireball.*` | Shoot **any** projectile type via `/fireball` |
| `neoessentials.warps.*` | Access **all** server warps regardless of per-warp restrictions |

> **Negative permissions** — prefix a node with `-` to explicitly deny it even if a wildcard grants it.  
> Example: give `neoessentials.*` then add `-neoessentials.item.enchant.unsafe` to deny unsafe enchanting.

> **Note on startup warnings (fixed in v1.0.2.6+build.7):**
> Older builds logged `WARN: Invalid permission format: neoessentials.spawner.*` (and `fireball.*`,
> `warps.*`) at startup because the internal permission validator did not recognise the `.*` suffix.
> The permissions themselves **worked at runtime** in all versions; only the registration log was
> wrong.  The validator is now fixed and no warnings are logged for valid wildcard nodes.

---

## Dynamic Nodes

These are **not pre-registered** but are checked at runtime:

### Home limit
Pattern: `neoessentials.home.<number>` (1–100)  
The **highest number** the player has is used as their home limit.  
Example: `neoessentials.home.5` → player can set 5 homes.  
If no home-limit node is found, the config default is used.

### Warp limit
Pattern: `neoessentials.warp.limit.<number>` (1–100)  
Example: `neoessentials.warp.limit.10` → player can create 10 player-warps.  
Special: `neoessentials.warp.limit.unlimited` → no limit.

### Per-kit nodes
Pattern: `neoessentials.kits.<kitname>` — grants access to that specific kit.  
Pattern: `neoessentials.kits.<kitname>.nocooldown` — bypasses the cooldown for that kit.  
These are **registered automatically** when a kit is created via `/createkit`.

---

## Permission Nodes — Full Reference

### Core

| Node | Default | Description |
|---|---|---|
| `neoessentials.use` | ✅ default | Basic mod usage — required for all commands |
| `neoessentials.info` | ✅ default | View mod information (`/neoe`) |

---

### Economy

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.economy.balance` | ✅ default | Check own balance | `/balance` |
| `neoessentials.economy.balance.others` |  op-only | Check another player's balance | `/balance <player>` |
| `neoessentials.economy.pay` | ✅ default | Send money to online players | `/pay` |
| `neoessentials.economy.pay.offline` |  op-only | Send money to offline players | `/pay` |
| `neoessentials.economy.pay.toggle` | ✅ default | Toggle receiving payments | `/paytoggle` |
| `neoessentials.economy.baltop` | ✅ default | View balance leaderboard | `/baltop [page]` |
| `neoessentials.economy.baltop.exempt` |  op-only | Exclude self from baltop ranking | |
| `neoessentials.economy.eco` |  op-only | Run eco admin commands | `/eco` |
| `neoessentials.economy.admin` |  op-only | Economy administration (parent node) | `/eco` |
| `neoessentials.economy.admin.give` |  op-only | Give money to a player | `/eco give` |
| `neoessentials.economy.admin.take` |  op-only | Take money from a player | `/eco take` |
| `neoessentials.economy.admin.set` |  op-only | Set a player's balance | `/eco set` |
| `neoessentials.economy.admin.reset` |  op-only | Reset a player's balance to starting balance | `/eco reset` |
| `neoessentials.worth` | ✅ default | Check sell value of item | `/worth [item] [amount]` |
| `neoessentials.sell` | ✅ default | Use the sell command | `/sell` |
| `neoessentials.sell.hand` | ✅ default | Sell item in hand | `/sell hand [amount]` |
| `neoessentials.sell.bulk` | ✅ default | Sell entire inventory | `/sell inventory\|all` |
| `neoessentials.setworth` |  op-only | Set item sell prices | `/setworth <item\|hand> <price\|remove>` |

---

### Teleportation

#### Admin Teleport
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.admin` |  op-only | Admin teleport (parent node) | |
| `neoessentials.teleport.admin.tp` |  op-only | Teleport a player to another | `/tp <player> <target>` |
| `neoessentials.teleport.tp` |  op-only | Teleport self (alias) | `/tp <player>` |
| `neoessentials.teleport.admin.tphere` |  op-only | Bring a player to you | `/tphere` |
| `neoessentials.teleport.tphere` |  op-only | Bring a player to you (alias) | `/tphere` |
| `neoessentials.teleport.admin.tpall` |  op-only | Teleport all players to a target | `/tpall` |
| `neoessentials.teleport.admin.tppos` |  op-only | Teleport to coordinates | `/tppos` |
| `neoessentials.teleport.tppos` |  op-only | Teleport to coordinates (alias) | `/tppos` |
| `neoessentials.teleport.admin.tpo` |  op-only | Teleport to offline player's last location | `/tpo` |

#### Teleport Requests (TPA)
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.request.tpa` | ✅ default | Send a teleport request | `/tpa <player>` |
| `neoessentials.teleport.request.tpahere` | ✅ default | Request a player teleport to you | `/tpahere <player>` |
| `neoessentials.teleport.request.accept` | ✅ default | Accept a teleport request | `/tpaccept` |
| `neoessentials.teleport.request.deny` | ✅ default | Deny a teleport request | `/tpdeny` |
| `neoessentials.teleport.request.cancel` | ✅ default | Cancel a sent request | `/tpcancel` |

#### Home System
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.home` | ✅ default | Use the home system | `/home` |
| `neoessentials.teleport.home.set` | ✅ default | Set a home location | `/sethome` |
| `neoessentials.teleport.home.delete` | ✅ default | Delete a home | `/delhome` |
| `neoessentials.teleport.home.list` | ✅ default | List homes | `/homes` |
| `neoessentials.teleport.home.others` |  op-only | Access other players' homes | `/home <player>:<name>` |
| `neoessentials.home.<number>` | — | **Dynamic** — sets home limit (see above) | |

#### Warp System
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.warp` | ✅ default | Use warps | `/warp <name>` |
| `neoessentials.teleport.warp.list` | ✅ default | List available warps | `/warps [page]`, `/warp` |
| `neoessentials.teleport.warp.others` |  op-only | Warp another player to a warp | `/warp <name> <player>` |
| `neoessentials.teleport.warp.create` |  op-only | Create a warp | `/setwarp` |
| `neoessentials.teleport.warp.delete` |  op-only | Delete a warp | `/delwarp` |
| `neoessentials.warps.<name>` | — | **Per-warp** — access to specific warp (when `perWarpPermission: true` in config) | |
| `neoessentials.warps.*` |  op-only | Access ALL warps regardless of per-warp permissions | |
| `neoessentials.teleport.pwarp` | ✅ default | Use player warps | `/pwarp` |
| `neoessentials.teleport.pwarp.create` | ✅ default | Create a player warp | `/pwarp create` |
| `neoessentials.teleport.pwarp.delete` | ✅ default | Delete a player warp | `/pwarp delete` |
| `neoessentials.teleport.pwarp.list` | ✅ default | List player warps | `/pwarp list` |
| `neoessentials.warp.limit.<number>` | — | **Dynamic** — sets player-warp limit | |
| `neoessentials.warp.limit.unlimited` |  op-only | Unlimited player warps | |

#### Spawn System
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.spawn` | ✅ default | Teleport to spawn | `/spawn` |
| `neoessentials.teleport.spawn.set` |  op-only | Set the server spawn | `/setspawn` |
| `neoessentials.teleport.spawn.info` |  op-only | View spawn info | `/spawninfo` |
| `neoessentials.teleport.spawn.clear` |  op-only | Clear spawn location | `/clearspawn` |

#### Misc Teleport
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.back` | ✅ default | Return to previous location | `/back` |
| `neoessentials.teleport.death` | ✅ default | Teleport to death location | `/back` (on death) |
| `neoessentials.teleport.top` | ✅ default | Teleport to highest block | `/top` |
| `neoessentials.teleport.jump` | ✅ default | Teleport through walls | `/jump` |
| `neoessentials.teleport.jumpto` | ✅ default | Teleport to block you're looking at | `/jumpto` |
| `neoessentials.teleport.tpr` | ✅ default | Random teleport | `/tpr` |
| `neoessentials.teleport.settpr` |  op-only | Set random teleport centre | `/settpr` |

---

### Kits

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.kits.use` | ✅ default | Use the kit system | `/kit` |
| `neoessentials.kits.list` | ✅ default | List available kits | `/kit`, `/listkits` |
| `neoessentials.kits.nocooldown` |  op-only | Bypass all kit cooldowns | |
| `neoessentials.kit.others` |  op-only | Give a kit to another player | `/kit <name> <player>` |
| `neoessentials.kitreset` |  op-only | Reset own kit cooldown | `/kitreset <kit>` |
| `neoessentials.kitreset.others` |  op-only | Reset another player's kit cooldown | `/kitreset <kit> <player>` |
| `neoessentials.kits.create` |  op-only | Create a kit from inventory | `/createkit` |
| `neoessentials.kits.delete` |  op-only | Delete a kit | `/delkit` |
| `neoessentials.kits.override` |  op-only | Override all kit restrictions | |
| `neoessentials.kits.<kitname>` | — | **Dynamic** — access to specific kit | |
| `neoessentials.kits.<kitname>.nocooldown` | — | **Dynamic** — bypass cooldown for specific kit | |

> **Dead/unused registered nodes:** `neoessentials.kits.admin`, `.admin.create`, `.admin.delete`, and `.admin.list` are registered in `PermissionRegistry` (so they show up in `/permissions list`/`search`) but there is **no `/kit admin` command and nothing checks them** — kit creation/deletion/listing are gated by the plain `neoessentials.kits.create` / `.delete` / `.list` nodes documented above instead.

---

### Player State & Admin Tools

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.fly` |  op-only | Toggle flight mode | `/fly [on\|off]` |
| `neoessentials.fly.others` |  op-only | Toggle flight for another player | `/fly <player> [on\|off]` |
| `neoessentials.god` |  op-only | Toggle god mode (invincibility) | `/god [on\|off]` |
| `neoessentials.god.others` |  op-only | Toggle god mode for another player | `/god <player> [on\|off]` |
| `neoessentials.heal` |  op-only | Restore own health and hunger | `/heal` |
| `neoessentials.heal.others` |  op-only | Restore another player's health | `/heal <player>` |
| `neoessentials.feed` |  op-only | Restore own hunger | `/feed` |
| `neoessentials.feed.others` |  op-only | Restore another player's hunger | `/feed <player>` |
| `neoessentials.speed` |  op-only | Set own walk or fly speed (0–10) | `/speed [walk\|fly] <0-10>` |
| `neoessentials.speed.others` |  op-only | Set another player's speed | `/speed [walk\|fly] <0-10> <player>` |
| `neoessentials.ext` | ✅ default | Extinguish own fire | `/ext` |
| `neoessentials.ext.others` |  op-only | Extinguish another player | `/ext <player>` |
| `neoessentials.burn` |  op-only | Set a player on fire | `/burn <player> [seconds]` |
| `neoessentials.give` |  op-only | Give items to players | `/give <player> <item> [amount]` |
| `neoessentials.more` |  op-only | Fill held stack to max | `/more [amount]` |
| `neoessentials.hat` |  op-only | Wear held item as helmet | `/hat` |
| `neoessentials.exp` | ✅ default | View own XP info | `/exp [show]` |
| `neoessentials.exp.set` |  op-only | Set own XP | `/exp set <amount>` |
| `neoessentials.exp.set.others` |  op-only | Set another player's XP | `/exp set <amount> <player>` |
| `neoessentials.exp.give` |  op-only | Give XP to self | `/exp give <amount>` |
| `neoessentials.exp.give.others` |  op-only | Give XP to another player | `/exp give <amount> <player>` |
| `neoessentials.sudo` |  op-only | Run a command as another player | `/sudo <player> <command>` |
| `neoessentials.sudo.exempt` |  op-only | Cannot be sudo'd by non-console | |
| `neoessentials.playtime` | ✅ default | View own playtime | `/playtime` |
| `neoessentials.playtime.others` |  op-only | View another player's playtime | `/playtime <player>` |

---

### Items

### Server Admin Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.broadcast` |  op-only | Broadcast a message to all players | `/broadcast <msg>`, `/bc`, `/announce` |
| `neoessentials.time` |  op-only | View current world time | `/time` |
| `neoessentials.time.set` |  op-only | Set or add world time | `/time set\|add <value>`, `/day`, `/night` |
| `neoessentials.weather` |  op-only | Set world weather | `/weather <sun\|storm\|thunder> [dur]`, `/sun`, `/storm`, `/thunder` |
| `neoessentials.kill` |  op-only | Kill a player | `/kill <player>` |
| `neoessentials.kill.exempt` |  op-only | Exempt from being killed by /kill | |
| `neoessentials.kill.force` |  op-only | Force kill even exempt players | |
| `neoessentials.gamemode` |  op-only | Change own gamemode | `/gamemode <mode>` |
| `neoessentials.gamemode.others` |  op-only | Change another player's gamemode | `/gamemode <mode> <player>` |
| `neoessentials.teleport.tpo` |  op-only | Teleport to player (bypass tptoggle) | `/tpo <player>` |
| `neoessentials.teleport.tpohere` |  op-only | Bring player to you (bypass tptoggle) | `/tpohere <player>` |
| `neoessentials.teleport.tpoffline` |  op-only | Teleport to offline player's last position | `/tpoffline <player>` |

---

### Utility Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.ptime` |  op-only | Set own per-player time override | `/ptime [reset\|day\|night\|<ticks>]` |
| `neoessentials.ptime.others` |  op-only | Set another player's time override | `/ptime <value> <player>` |
| `neoessentials.pweather` |  op-only | Set own per-player weather override | `/pweather [reset\|sun\|storm]` |
| `neoessentials.pweather.others` |  op-only | Set another player's weather override | `/pweather <type> <player>` |
| `neoessentials.effect` |  op-only | Apply potion effects to players | `/effect <player> <effect\|clear> [dur] [amp]` |
| `neoessentials.spawnmob` |  op-only | Spawn entities at own location | `/spawnmob <mob> [amount]`, `/mob` |
| `neoessentials.spawnmob.others` |  op-only | Spawn entities at another player | `/spawnmob <mob> [amount] <player>` |
| `neoessentials.unlimited` |  op-only | Toggle unlimited item use | `/unlimited [list\|clear\|<item>]` |
| `neoessentials.unlimited.others` |  op-only | Toggle unlimited items for another player | `/unlimited <item> <player>` |
| `neoessentials.condense` |  op-only | Condense items to storage blocks | `/condense [item]` |

---

### Item Customisation & Miscellaneous

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.me` | ✅ default | Broadcast action messages | `/me <action>` |
| `neoessentials.tptoggle` | ✅ default | Toggle own teleport request acceptance | `/tptoggle [on\|off]` |
| `neoessentials.tptoggle.others` |  op-only | Toggle tptoggle for another player | `/tptoggle <player> [on\|off]` |
| `neoessentials.gc` |  op-only | View server TPS, memory, uptime, chunk info | `/gc`, `/mem` |
| `neoessentials.lightning` |  op-only | Strike lightning at look target | `/lightning`, `/smite` |
| `neoessentials.lightning.others` |  op-only | Strike lightning at a named player | `/lightning <player>` |
| `neoessentials.skull` |  op-only | Get a player head item | `/skull [player]` |
| `neoessentials.itemname` |  op-only | Rename the held item | `/itemname [name\|-]`, `/rename` |
| `neoessentials.itemlore` |  op-only | Edit held item lore lines | `/itemlore add\|set\|remove\|clear` |
| `neoessentials.remove` |  op-only | Remove entities in radius | `/remove <type> [radius]` |
| `neoessentials.loom` |  op-only | Open portable loom | `/loom` |
| `neoessentials.cartography` |  op-only | Open portable cartography table | `/cartography`, `/cartographytable` |

---

### Home & Warp Enhancements

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.renamehome` | ✅ default | Rename own home | `/renamehome <old> <new>` |
| `neoessentials.renamehome.others` |  op-only | Rename another player's home | `/renamehome <player:old> <new>` |
| `neoessentials.warpinfo` | ✅ default | Show warp coordinates and world | `/warpinfo <name>` |
| `neoessentials.world` |  op-only | Teleport to a world/dimension | `/world [name]` |
| `neoessentials.world.others` |  op-only | Teleport another player to a world | `/world <name> <player>` |
| `neoessentials.spawner` |  op-only | Change a mob spawner type | `/spawner <mob>` |
| `neoessentials.spawner.*` |  op-only | Change spawner to any mob | wildcard — grants all mob types |
| `neoessentials.spawner.<mob>` |  op-only | Change spawner to a specific mob | e.g. `neoessentials.spawner.zombie` |
| `neoessentials.recipe` | ✅ default | Show/unlock crafting recipe for an item | `/recipe [item]` |
| `neoessentials.tpauto` | ✅ default | Auto-accept all incoming teleport requests | `/tpauto [on\|off]` |
| `neoessentials.tpauto.others` |  op-only | Toggle tpauto for another player | `/tpauto <player> [on\|off]` |

---

### World Interaction & Fun Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.fireball` |  op-only | Shoot a projectile | `/fireball [type] [speed]` |
| `neoessentials.fireball.*` |  op-only | Shoot any projectile type | wildcard |
| `neoessentials.fireball.<type>` |  op-only | Shoot specific type (fireball/small/large/arrow/skull/egg/snowball/expbottle/dragon/trident/windcharge) | e.g. `neoessentials.fireball.arrow` |
| `neoessentials.fireball.ride` |  op-only | Ride the shot projectile | `/fireball <type> <speed> ride` |
| `neoessentials.tree` |  op-only | Grow a tree at look target | `/tree <type>`, `/bigtree` |
| `neoessentials.break` |  op-only | Instantly break the looked-at block (no drops) | `/break` |
| `neoessentials.break.bedrock` |  op-only | Break bedrock blocks | permission bypass |
| `neoessentials.ice` |  op-only | Freeze self solid | `/ice` |
| `neoessentials.ice.others` |  op-only | Freeze another player | `/ice <player>` |
| `neoessentials.bottom` |  op-only | Teleport to world bottom at current XZ | `/bottom` |
| `neoessentials.tpaall` |  op-only | Send tpa-here to all online players | `/tpaall [player]` |
| `neoessentials.tpaall.others` |  op-only | Send tpaall on behalf of another player | `/tpaall <player>` |
| `neoessentials.broadcastworld` |  op-only | Broadcast to players in your current world | `/broadcastworld`, `/bcastworld` |

---

### Player Info & Admin Tools

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.seen` | ✅ default | View when a player was last online | `/seen <player>` |
| `neoessentials.near` | ✅ default | List players within a radius | `/near [radius]` |
| `neoessentials.ping` | ✅ default | View your own ping | `/ping` |
| `neoessentials.ping.others` | ✅ default | View another player's ping | `/ping <player>` |
| `neoessentials.playtime` | ✅ default | View your total play time | `/playtime` |
| `neoessentials.playtime.others` | ✅ default | View another player's play time | `/playtime <player>` |
| `neoessentials.whois` |  op-only | View detailed player info (UUID, pos, gamemode, health) | `/whois <player>` |
| `neoessentials.realname` | ✅ default | Look up real name from nickname | `/realname <nickname>` |
| `neoessentials.sudo` |  op-only | Force a player to run a command | `/sudo <player> <command>` |
| `neoessentials.sudo.exempt` |  op-only | Be immune to /sudo | permission node |
| `neoessentials.suicide` | ✅ default | Kill yourself | `/suicide` |
| `neoessentials.msgtoggle` | ✅ default | Toggle your incoming private messages on/off | `/msgtoggle [on\|off]` |
| `neoessentials.msgtoggle.others` |  op-only | Toggle another player's messages | `/msgtoggle <player> [on\|off]` |
| `neoessentials.rtoggle` | ✅ default | Toggle reply-to-last-sender for `/r` | `/rtoggle [on\|off]` |
| `neoessentials.rtoggle.others` |  op-only | Toggle rtoggle for another player | `/rtoggle <player> [on\|off]` |
| `neoessentials.motd` | ✅ default | View the message of the day | `/motd` |
| `neoessentials.rules` | ✅ default | View server rules | `/rules` |

---

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.item.repair` |  op-only | Repair held item | `/repair` |
| `neoessentials.item.enchant` |  op-only | Enchant held item | `/enchant` |
| `neoessentials.item.enchant.unsafe` |  op-only | Apply enchants beyond vanilla limits | `/enchant` |
| `neoessentials.item.enchant.others` |  op-only | Enchant another player's item | `/enchant <player>` |
| `neoessentials.item.enchant.any` |  op-only | Enchant any item (ignore type restrictions) | `/enchant` |
| `neoessentials.item.powertool` |  op-only | Use the powertool system | `/powertool` |
| `neoessentials.item.powertool.toggle` |  op-only | Toggle powertool on/off | `/pttoggle` |
| `neoessentials.item.dispose` | ✅ default | Use the item disposal chest | `/dispose` |
| `neoessentials.item.clearinventory` |  op-only | Clear own inventory | `/clearinv` |
| `neoessentials.item.clearinventory.others` |  op-only | Clear another player's inventory | `/clearinv <player>` |
| `neoessentials.item.spawn` |  op-only | Spawn items | `/spawnitem` |
| `neoessentials.invsee` |  op-only | View another player's inventory | `/invsee` |
| `neoessentials.invsee.edit` |  op-only | Edit another player's inventory | `/invsee` |
| `neoessentials.enderchest` |  op-only | View another player's ender chest | `/ec <player>` |
| `neoessentials.enderchest.edit` |  op-only | Edit another player's ender chest | `/ec <player>` |

---

### Chat & Messaging

#### Private Messaging
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.chat.msg` | ✅ default | Send private messages | `/msg` |
| `neoessentials.chat.reply` | ✅ default | Reply to messages | `/reply` |
| `neoessentials.chat.ignore` | ✅ default | Ignore a player | `/ignore` |
| `neoessentials.chat.unignore` | ✅ default | Unignore a player | `/unignore` |
| `neoessentials.chat.msgtoggle` | ✅ default | Toggle receiving messages | `/msgtoggle` |
| `neoessentials.chat.socialspy` |  op-only | See all private messages | `/socialspy` |
| `neoessentials.chat.socialspy.exempt` |  op-only | Private messages not visible to socialspy | |
| `neoessentials.chat.msgtoggle.bypass` |  op-only | Message players who have toggled off | |

#### Mail
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.mail` | ✅ default | Use the mail system | `/mail` |
| `neoessentials.mail.send` | ✅ default | Send mail to a player | `/mail send` |
| `neoessentials.mail.clear` | ✅ default | Clear own mailbox | `/mail clear` |

#### Moderation Chat
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.chat.mute` |  op-only | Mute, unmute, and view muted players — `/mute`, `/unmute`, and `/mutelist` all share this single node | `/mute`, `/unmute`, `/mutelist` |
| `neoessentials.chat.mute.exempt` |  op-only | Exempt from being muted | |
| `neoessentials.chat.ignore.exempt` |  op-only | Exempt from being ignored | |

> **Dead/unused registered nodes:** `neoessentials.chat.unmute` and `neoessentials.chat.mutelist` are registered in `PermissionRegistry` (so they appear in `/permissions list`/`search`) but are **not checked anywhere** — `/unmute` and `/mutelist` both enforce `neoessentials.chat.mute`, same as `/mute`. Granting `chat.unmute` or `chat.mutelist` alone does nothing.

#### Formatting & Colours
| Node | Default | Description |
|---|---|---|
| `neoessentials.chat.color` |  op-only | Use `&0-9`, `&a-f` colour codes in chat |
| `neoessentials.chat.color.hex` |  op-only | Use `&#RRGGBB` hex colours in chat |
| `neoessentials.chat.format` |  op-only | Use `&k-o`, `&r` formatting codes in chat |
| `neoessentials.chat.namedcolors` |  op-only | Use `<tag>`-style rich text (named colors, gradient, rainbow, hover, click) in chat messages — this is the node actually enforced by the chat pipeline |
| `neoessentials.chat.richtext` |  op-only | Registered node — not currently checked anywhere |
| `neoessentials.chat.gradient` |  op-only | Registered node — not currently checked anywhere |
| `neoessentials.chat.rainbow` |  op-only | Registered node — not currently checked anywhere |

#### Chat Channels
| Node | Default | Description |
|---|---|---|
| `neoessentials.chat.channel.local` | ✅ default | Use local chat channel |
| `neoessentials.chat.channel.global` | ✅ default | Use global chat channel |
| `neoessentials.chat.staff` |  op-only | Access staff chat channel |
| `neoessentials.chat.mention` | ✅ default | Mention players with `@name` |
| `neoessentials.chat.mention.all` |  op-only | Mention everyone with `@everyone` |
| `neoessentials.chat.itemlink` | ✅ default | Show held item in chat with `[item]` |

#### Anti-Spam Bypasses
| Node | Default | Description |
|---|---|---|
| `neoessentials.chat.caps.bypass` |  op-only | Bypass caps filter |
| `neoessentials.chat.repeat.bypass` |  op-only | Bypass repeat-message filter |
| `neoessentials.chat.links.bypass` |  op-only | Bypass link filter |
| `neoessentials.chat.spam.bypass` |  op-only | Bypass spam rate limit |

---

### Moderation

| Node | Default | Description | Command |
|---|---|---|---|
| **Banning** | | | |
| `neoessentials.moderation.ban` |  op-only | Ban a player | `/ban` |
| `neoessentials.moderation.banip` |  op-only | Ban an IP address | `/banip` |
| `neoessentials.moderation.banlist` |  op-only | View the ban list | `/banlist` |
| `neoessentials.moderation.tempban` |  op-only | Temporarily ban a player | `/tempban` |
| `neoessentials.moderation.tempbanip` |  op-only | Temporarily ban an IP address | `/tempbanip` |
| `neoessentials.moderation.unban` |  op-only | Unban a player | `/unban` |
| `neoessentials.moderation.unbanip` |  op-only | Unban an IP address | `/unbanip` |
| **Kicking** | | | |
| `neoessentials.moderation.kick` |  op-only | Kick a player | `/kick` |
| `neoessentials.moderation.kickall` |  op-only | Kick all players | `/kickall` |
| **Freezing** | | | |
| `neoessentials.moderation.freeze` |  op-only | Freeze a player | `/freeze` |
| `neoessentials.moderation.unfreeze` |  op-only | Unfreeze a player | `/unfreeze` |
| `neoessentials.moderation.freezeall` |  op-only | Freeze all players | `/freezeall` |
| `neoessentials.moderation.unfreezeall` |  op-only | Unfreeze all players | `/unfreezeall` |
| `neoessentials.moderation.freezelist` |  op-only | List frozen players | `/freezelist` |
| **Jailing** | | | |
| `neoessentials.moderation.jail` |  op-only | Jail a player (both indefinite and timed — `/jailfor` shares this node) | `/jail`, `/jailfor`, `/togglejail` |
| `neoessentials.moderation.unjail` |  op-only | Unjail a player | `/unjail` |
| `neoessentials.moderation.setjail` |  op-only | Create or delete a jail location | `/setjail`, `/deljail` |
| `neoessentials.moderation.jaillist` |  op-only | List jail locations | `/jaillist`, `/jails` |
| `neoessentials.moderation.jailinfo` |  op-only | View jail info | `/jailinfo` |
| `neoessentials.jail.wand` |  op-only | Get the jail-region selection wand (right-click = corner 1, left-click = corner 2) | `/jailwand` |
| `neoessentials.jail.allow-break` |  op-only | Break blocks while jailed | (enforcement, no command) |
| `neoessentials.jail.allow-place` |  op-only | Place blocks while jailed | (enforcement, no command) |
| `neoessentials.jail.allow-interact` |  op-only | Interact with blocks/items while jailed | (enforcement, no command) |
| `neoessentials.jail.allow-attack` |  op-only | Attack entities while jailed | (enforcement, no command) |
| **Vanish** | | | |
| `neoessentials.moderation.vanish` |  op-only | Vanish yourself | `/vanish` |
| `neoessentials.moderation.vanish.others` |  op-only | Vanish another player | `/vanish <player>` |
| `neoessentials.moderation.seevanished` |  op-only | See vanished players | |
| `neoessentials.vanish.see` |  op-only | See vanished players (alias) | |
| `neoessentials.moderation.vanishlist` |  op-only | List vanished players | `/vanishlist` |
| **Warnings** | | | |
| `neoessentials.moderation.warn` |  op-only | Issue, clear, or remove warnings | `/warn`, `/clearwarnings`, `/removewarn` |
| `neoessentials.moderation.warnings` |  op-only | View a player's warnings | `/warnings` |
| **Staff Notes** | | | |
| `neoessentials.moderation.note` |  op-only | Add or remove staff notes | `/note`, `/removenote` |
| `neoessentials.moderation.notes` |  op-only | View a player's staff notes | `/notes` |
| **Player Reports** | | | |
| `neoessentials.moderation.report` | ✅ default | Submit a player report | `/report` |
| `neoessentials.moderation.reports` |  op-only | View and resolve the report queue | `/reports`, `/reviewreport` |
| **History** | | | |
| `neoessentials.moderation.history` |  op-only | View a player's full moderation history (bans/mutes/kicks/warns) | `/modhistory`, `/history` |
| **Notifications** | | | |
| `neoessentials.moderation.notify` |  op-only | Receive moderation action notifications | |
| `neoessentials.moderation.notifications` |  op-only | Receive moderation event broadcasts | |

> **Dead/unused registered node:** `neoessentials.moderation.jail.timed` is registered in `PermissionRegistry` (appears in `/permissions list`/`search`) but is **not actually checked anywhere** — both `/jail` and `/jailfor` enforce the plain `neoessentials.moderation.jail` node. Don't rely on `jail.timed` to gate timed jails separately from indefinite ones.

---

### Miscellaneous Utilities

#### Player Info
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.list` | ✅ default | View online player list | `/list`, `/who` |
| `neoessentials.near` | ✅ default | View nearby players | `/near` |
| `neoessentials.seen` | ✅ default | Check when a player was last seen | `/seen` |
| `neoessentials.whois` | ✅ default | View player info | `/whois` |
| `neoessentials.whois.detailed` |  op-only | View detailed player info | `/whois` |
| `neoessentials.ping` | ✅ default | Check own ping | `/ping` |
| `neoessentials.ping.others` |  op-only | Check another player's ping | `/ping <player>` |
| `neoessentials.realname` | ✅ default | Look up a player's real name from nickname | `/realname` |
| `neoessentials.depth` | ✅ default | View depth/Y-level info | `/depth` |
| `neoessentials.depth.others` |  op-only | View another player's depth info | `/depth <player>` |
| `neoessentials.compass` | ✅ default | View compass/direction info | `/compass` |
| `neoessentials.compass.others` |  op-only | View compass info for another player | `/compass <player>` |
| `neoessentials.getpos` | ✅ default | View own position | `/getpos` |
| `neoessentials.getpos.others` |  op-only | View another player's position | `/getpos <player>` |

#### Nicknames
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.nick` | ✅ default | Change own nickname | `/nick` |
| `neoessentials.nick.color` |  op-only | Use colour codes in nickname | `/nick` |
| `neoessentials.nick.others` |  op-only | Change another player's nickname | `/setnick` |

#### Server Info
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.motd` | ✅ default | View the message of the day | `/motd` |
| `neoessentials.rules` | ✅ default | View server rules | `/rules` |
| `neoessentials.helpop` | ✅ default | Send a help request to staff | `/helpop` |
| `neoessentials.helpop.receive` |  op-only | Receive help-op requests | |
| `neoessentials.staff` |  op-only | Access staff chat and features | |

#### Portable Workstations
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.anvil` | ✅ default | Open portable anvil | `/anvil` |
| `neoessentials.crafting` | ✅ default | Open portable crafting table | `/craft` |
| `neoessentials.grindstone` | ✅ default | Open portable grindstone | `/grindstone` |
| `neoessentials.smithing` | ✅ default | Open portable smithing table | `/smithing` |
| `neoessentials.stonecutting` | ✅ default | Open portable stonecutter | `/stonecutter` |

#### Book & Sign
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.book` | ✅ default | Give yourself a writable book | `/book` |
| `neoessentials.book.unlock` |  op-only | Unlock a written book for editing | `/book unlock` |
| `neoessentials.book.title` |  op-only | Set a book's title | `/book title` |
| `neoessentials.book.author` |  op-only | Set a book's author | `/book author` |
| `neoessentials.sign` | ✅ default | Edit sign text | `/sign` |
| `neoessentials.sign.colors` |  op-only | Use colours on signs | `/sign` |

#### AFK, Gamemode & Other
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.afk` | ✅ default | Use the AFK system | `/afk` |
| `neoessentials.afk.exempt` |  op-only | Exempt from AFK kick | |
| `neoessentials.suicide` | ✅ default | Use the suicide command | `/suicide` |
| `neoessentials.gamemode` |  op-only | Change own gamemode | `/gamemode`, `/gmc`, `/gms`, `/gmsp`, `/gma` |
| `neoessentials.gamemode.others` |  op-only | Change another player's gamemode | `/gamemode <mode> <player>` |

---

### Admin & Config

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.admin` |  op-only | General admin access | |
| `neoessentials.reload` |  op-only | Reload the mod configuration | `/neoe reload` |
| `neoessentials.debug` |  op-only | Enable debug logging | |
| `neoessentials.rules.admin` |  op-only | Create/edit/delete server rules | `/rules add` etc. |
| `neoessentials.motd.set` |  op-only | Set the message of the day | `/motd set` |
| `neoessentials.motd.broadcast` |  op-only | Broadcast the MOTD to all players | `/motd broadcast` |
| `neoessentials.motd.reload` |  op-only | Reload MOTD from file | `/motd reload` |

---

### Permission System Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.permissions.admin` |  op-only | Full permissions system access | `/permissions` |
| `neoessentials.permissions.reload` |  op-only | Reload the permissions system | `/permissions reload` |
| `neoessentials.permissions.list` |  op-only | List registered permission nodes | `/permissions list` |
| `neoessentials.permissions.check` |  op-only | Check a player's effective permissions | `/permissions check` |
| `neoessentials.permissions.search` |  op-only | Search permission nodes | `/permissions search` |
| `neoessentials.permissions.user` |  op-only | User management (parent) | `/permissions user` |
| `neoessentials.permissions.user.permissions` |  op-only | Add/remove user permission nodes | |
| `neoessentials.permissions.user.groups` |  op-only | Add/remove user from groups | |
| `neoessentials.permissions.user.clear` |  op-only | Clear all user permissions | |
| `neoessentials.permissions.list.users` |  op-only | List all permission users | |
| `neoessentials.permissions.info.user` |  op-only | View a user's permission info | |
| `neoessentials.permissions.group` |  op-only | Group management (parent) | `/permissions group` |
| `neoessentials.permissions.group.create` |  op-only | Create a new group | |
| `neoessentials.permissions.group.delete` |  op-only | Delete a group | |
| `neoessentials.permissions.group.rename` |  op-only | Rename a group | |
| `neoessentials.permissions.group.clone` |  op-only | Clone a group | |
| `neoessentials.permissions.group.inherit` |  op-only | Set group inheritance | |
| `neoessentials.permissions.group.permissions` |  op-only | Manage group permission nodes | |
| `neoessentials.permissions.group.modify` |  op-only | Modify group settings (prefix/suffix) | |
| `neoessentials.permissions.group.clear` |  op-only | Clear all group permissions | |
| `neoessentials.permissions.list.groups` |  op-only | List all groups | |
| `neoessentials.permissions.info.group` |  op-only | View a group's info | |

---

### Web Dashboard

> Unlike the sections above, dashboard nodes are **not** pre-registered via
> `PermissionRegistry.registerAllPermissions()` — they are checked ad hoc by the `/dashboard`
> and `/dashboardregister` commands and the dashboard's own auth layer, so they won't appear in
> `/permissions list` / `/permissions search`. Grant them explicitly per group.

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.admin.dashboard` |  op-only | Start/stop/restart/update the dashboard server | `/dashboard` |
| `neoessentials.dashboard.access` |  op-only | Register an account and log in to the dashboard | `/dashboardregister` |
| `neoessentials.dashboard.view` |  op-only | View-only dashboard access | |
| `neoessentials.dashboard.manage` |  op-only | Manage dashboard settings | |
| `neoessentials.dashboard.moderator` |  op-only | Moderator-level dashboard access | |
| `neoessentials.dashboard.admin` |  op-only | Full admin dashboard access | |

---

## Example groups.json

```json
{
  "defaultGroup": "default",
  "groups": [
    {
      "name": "default",
      "prefix": "§7",
      "suffix": "",
      "priority": 0,
      "permissions": [
        "neoessentials.use",
        "neoessentials.economy.balance",
        "neoessentials.economy.pay",
        "neoessentials.economy.pay.toggle",
        "neoessentials.economy.baltop",
        "neoessentials.teleport.request.tpa",
        "neoessentials.teleport.request.tpahere",
        "neoessentials.teleport.request.accept",
        "neoessentials.teleport.request.deny",
        "neoessentials.teleport.request.cancel",
        "neoessentials.teleport.home",
        "neoessentials.teleport.home.set",
        "neoessentials.teleport.home.delete",
        "neoessentials.teleport.home.list",
        "neoessentials.home.3",
        "neoessentials.teleport.warp",
        "neoessentials.teleport.warp.list",
        "neoessentials.teleport.spawn",
        "neoessentials.teleport.back",
        "neoessentials.teleport.death",
        "neoessentials.teleport.tpr",
        "neoessentials.kits.use",
        "neoessentials.kits.list",
        "neoessentials.item.dispose",
        "neoessentials.chat.msg",
        "neoessentials.chat.reply",
        "neoessentials.chat.ignore",
        "neoessentials.chat.unignore",
        "neoessentials.chat.msgtoggle",
        "neoessentials.chat.channel.local",
        "neoessentials.chat.channel.global",
        "neoessentials.chat.mention",
        "neoessentials.chat.itemlink",
        "neoessentials.mail",
        "neoessentials.mail.send",
        "neoessentials.mail.clear",
        "neoessentials.list",
        "neoessentials.near",
        "neoessentials.seen",
        "neoessentials.whois",
        "neoessentials.ping",
        "neoessentials.realname",
        "neoessentials.motd",
        "neoessentials.rules",
        "neoessentials.helpop",
        "neoessentials.afk",
        "neoessentials.anvil",
        "neoessentials.crafting",
        "neoessentials.grindstone",
        "neoessentials.smithing",
        "neoessentials.stonecutting",
        "neoessentials.book",
        "neoessentials.sign",
        "neoessentials.nick",
        "neoessentials.suicide",
        "neoessentials.depth",
        "neoessentials.compass",
        "neoessentials.getpos",
        "neoessentials.info"
      ],
      "inherits": []
    },
    {
      "name": "vip",
      "prefix": "§6[VIP] §f",
      "suffix": "",
      "priority": 10,
      "permissions": [
        "neoessentials.home.10",
        "neoessentials.teleport.top",
        "neoessentials.teleport.jump",
        "neoessentials.teleport.jumpto",
        "neoessentials.nick.color",
        "neoessentials.chat.color",
        "neoessentials.chat.format",
        "neoessentials.chat.richtext",
        "neoessentials.teleport.warp.create",
        "neoessentials.warp.limit.5",
        "neoessentials.item.repair",
        "neoessentials.sign.colors"
      ],
      "inherits": ["default"]
    },
    {
      "name": "moderator",
      "prefix": "§2[Mod] §f",
      "suffix": "",
      "priority": 50,
      "permissions": [
        "neoessentials.moderation.ban",
        "neoessentials.moderation.banip",
        "neoessentials.moderation.banlist",
        "neoessentials.moderation.tempban",
        "neoessentials.moderation.unban",
        "neoessentials.moderation.unbanip",
        "neoessentials.moderation.kick",
        "neoessentials.moderation.kickall",
        "neoessentials.moderation.freeze",
        "neoessentials.moderation.unfreeze",
        "neoessentials.moderation.freezeall",
        "neoessentials.moderation.unfreezeall",
        "neoessentials.moderation.freezelist",
        "neoessentials.moderation.jail",
        "neoessentials.moderation.unjail",
        "neoessentials.moderation.setjail",
        "neoessentials.moderation.jaillist",
        "neoessentials.moderation.jailinfo",
        "neoessentials.moderation.vanish",
        "neoessentials.moderation.seevanished",
        "neoessentials.moderation.vanishlist",
        "neoessentials.moderation.notify",
        "neoessentials.chat.mute",
        "neoessentials.chat.unmute",
        "neoessentials.chat.mutelist",
        "neoessentials.chat.socialspy",
        "neoessentials.chat.staff",
        "neoessentials.staff",
        "neoessentials.helpop.receive",
        "neoessentials.whois.detailed",
        "neoessentials.nick.others",
        "neoessentials.teleport.admin.tp",
        "neoessentials.teleport.admin.tphere",
        "neoessentials.teleport.admin.tpo",
        "neoessentials.home.20",
        "neoessentials.warp.limit.unlimited",
        "neoessentials.dashboard.moderator"
      ],
      "inherits": ["vip"]
    },
    {
      "name": "admin",
      "prefix": "§c[Admin] §f",
      "suffix": "",
      "priority": 100,
      "permissions": [
        "neoessentials.*"
      ],
      "inherits": ["moderator"]
    }
  ]
}
```

---

## External Permission Mods

### Overview — Three Permission Tiers

NeoEssentials supports three configurations:

| Tier | Setup | What `permissions.json` controls |
|---|---|---|
| **Built-in + NeoForge bridge** *(default)* | No extra mods needed | All NeoEssentials commands **and** any mod that uses NeoForge's permission API (WorldEdit, FTB Chunks, etc.) |
| **LuckPerms** | Install LuckPerms, set `useExternalPermissions: true` | Nothing — LuckPerms manages everything |
| **FTB Ranks** | Install FTB Ranks, set `useExternalPermissions: true` | Nothing — FTB Ranks manages everything |

---

### Compatibility Report (startup log)

At every startup NeoEssentials logs an **External Permissions Compatibility Report**:

```
╔══════════════════════════════════════════════════════════════════════╗
║        NeoEssentials — External Permissions Compatibility Report      ║
╠══════════════════════════════════════════════════════════════════════╣
║  Active adapter  : FTB Ranks                    v2101.1.3           ║
║  Health status   : ✓ HEALTHY                                         ║
╠══════════════════════════════════════════════════════════════════════╣
║  Installed permission mods:                                          ║
║    ftbranks   detected: 2101.1.3   last tested: 2101.1.3   ✓ compatible  ║
╚══════════════════════════════════════════════════════════════════════╝
```

A `⚠ NEWER THAN TESTED` warning is emitted if the installed version is newer than the
last-tested minor line. This is informational only — permission checks still proceed.

---

### Built-in Mode — NeoForge Permission Handler Bridge

As of **v1.0.2.6**, when no competing permission mod is installed, NeoEssentials automatically
registers itself as the active **NeoForge permission handler** (`neoessentials:handler`).

This means that **any mod** that uses NeoForge's `PermissionAPI.getPermission(player, node)` API —
including WorldEdit, FTB Chunks, WTHIT, and others — will have its Boolean permission nodes
evaluated against `permissions.json`.

#### Adding permissions for external mods

```json
{
  "groups": [
    {
      "name": "admin",
      "permissions": ["neoessentials.*", "worldedit.*", "ftbchunks.*"]
    },
    {
      "name": "builder",
      "permissions": ["neoessentials.player", "worldedit.edit", "worldedit.selection.*"]
    }
  ]
}
```

> Node names must be **lowercase**. `/permissions group add` converts input automatically.

#### Manual handler selection

`config/neoforge-server.toml`:
```toml
# neoessentials:handler = NeoEssentials permissions.json for ALL mods (auto-selected by default)
# neoforge:default_handler = vanilla OP-level only
# luckperms:default = LuckPerms (selected automatically when LuckPerms is installed)
permissionHandler = "neoessentials:handler"
```

---

### Fallback Chain

When an external adapter is in use, each permission check follows this fallback chain:

```
1. emergencyMode?        → YES → OP status only (failsafe)
2. opsBypassPermissions? → YES + player is OP → GRANT (fast-path)
3. External adapter      → healthy? → use its answer (grant or deny)
                         → unhealthy / exception → fall through to step 4
4. Internal permissions.json (fallback when external fails)
5. vanillaOpFallback?    → YES + player is OP → GRANT (last resort)
                         → NO or not OP → DENY
```

**Adapter health tracking:** After 5 consecutive runtime exceptions the adapter marks itself
`UNHEALTHY`, internal `permissions.json` takes over, and a single boxed `WARN` is logged. The
adapter is re-checked on the next `/permissions reload`.

---

### LuckPerms

| Step | Action |
|---|---|
| 1 | Install LuckPerms NeoForge |
| 2 | Set `"useExternalPermissions": true` in `config/neoessentials/config.json` |
| 3 | Run `/permissions reload` |
| 4 | Add NeoEssentials nodes via `/lp group <name> permission set <node> true` |

> All NeoEssentials permission nodes work normally inside LuckPerms. The built-in
> `permissions.json` is **not used** for checks, but keeps accumulating edits if you switch back.

**Context support:** When LuckPerms is active, world and server contexts are evaluated using the
player's live `QueryOptions` (updated since build.4).

---

### FTB Ranks

| Step | Action |
|---|---|
| 1 | Install FTB Ranks NeoForge |
| 2 | Set `"useExternalPermissions": true` in `config/neoessentials/config.json` |
| 3 | Run `/permissions reload` |
| 4 | Add NeoEssentials nodes via FTB Ranks commands |

**API probe order:** NeoEssentials tries four FTB Ranks API signatures at startup to handle
version differences (current, legacy static, alternative naming, older instance method).

---

### Wildcard support for external mods

The `.*` wildcard works for any mod prefix:

| Wildcard | Effect |
|---|---|
| `worldedit.*` | All WorldEdit permissions |
| `worldedit.selection.*` | All WorldEdit selection permissions |
| `ftbchunks.*` | All FTB Chunks permissions |
| `neoessentials.*` | All NeoEssentials permissions |

---

### Compatibility table

| Mod | Tested version | Integration type | Notes |
|---|---|---|---|
| **LuckPerms** | 5.4.x | Full — `LuckPermsAdapter` | Context-aware via QueryOptions |
| **FTB Ranks** | 2101.1.3 | Full — `FtbRanksAdapter` | 4-API-signature probe for compatibility |
| **WorldEdit** (NeoForge) | Any | Passive — NeoForge handler bridge | Grant `worldedit.*` in `permissions.json` |
| **FTB Chunks** | Any | Passive — NeoForge handler bridge | Grant `ftbchunks.*` in `permissions.json` |
| Any NeoForge mod using `PermissionAPI` | Any | Passive — NeoForge handler bridge | Add their nodes to `permissions.json` |

---

## Fine-Grained Command Control

NeoEssentials uses **per-subcommand permission nodes** throughout. Every branch of every command
tree has its own node, so you can grant or deny individual subcommands without granting the whole
command.

### How it works

Every Brigadier branch checks `PermissionValidator.validatePermission(source, node)` (or
`validateAnyPermission`) before executing. The node is shown in the denied-access message so
admins know exactly what to grant.

### Examples by system

#### Home system

| Subcommand | Node | Default |
|---|---|---|
| `/home` (teleport) | `neoessentials.teleport.home` | ✅ |
| `/home set` | `neoessentials.teleport.home.set` | ✅ |
| `/home delete` | `neoessentials.teleport.home.delete` | ✅ |
| `/home list` | `neoessentials.teleport.home.list` | ✅ |
| `/home <name> <player>` (others) | `neoessentials.teleport.home.others` |  |

#### Warp system

| Subcommand | Node | Default |
|---|---|---|
| `/warp <name>` | `neoessentials.teleport.warp` | ✅ |
| `/warp <name> <player>` | `neoessentials.teleport.warp.others` |  |
| `/setwarp` | `neoessentials.teleport.warp.create` |  |
| `/delwarp` | `neoessentials.teleport.warp.delete` |  |
| `/warps` (list) | `neoessentials.teleport.warp.list` | ✅ |

#### Kit system

| Subcommand | Node | Default |
|---|---|---|
| `/kit` | `neoessentials.kits.use` | ✅ |
| `/kit <name>` | `neoessentials.kits.use` + `neoessentials.kits.<kitname>` | ✅ |
| `/kit <name> <player>` | `neoessentials.kit.others` |  |
| `/createkit` | `neoessentials.kits.create` |  |
| `/delkit` | `neoessentials.kits.delete` |  |
| `/kitreset` | `neoessentials.kitreset` |  |
| `/kitreset <player>` | `neoessentials.kitreset.others` |  |

#### Economy

| Subcommand | Node | Default |
|---|---|---|
| `/balance` | `neoessentials.economy.balance` | ✅ |
| `/balance <player>` | `neoessentials.economy.balance.others` |  |
| `/pay` | `neoessentials.economy.pay` | ✅ |
| `/pay <offline>` | `neoessentials.economy.pay.offline` |  |
| `/eco give/take/set` | `neoessentials.economy.eco` |  |

#### Moderation

| Subcommand | Node | Default |
|---|---|---|
| `/ban` | `neoessentials.moderation.ban` |  |
| `/banip` | `neoessentials.moderation.banip` |  |
| `/tempban` | `neoessentials.moderation.tempban` |  |
| `/jail` | `neoessentials.moderation.jail` |  |
| `/jailfor` | `neoessentials.moderation.jail` (same node as `/jail`) |  |
| `/vanish` | `neoessentials.moderation.vanish` |  |
| `/vanish <player>` | `neoessentials.moderation.vanish.others` |  |

#### Permission system commands

| Subcommand | Node | Default |
|---|---|---|
| `/permissions group add/remove` | `neoessentials.permissions.group.permissions` |  |
| `/permissions group setprefix/setsuffix` | `neoessentials.permissions.group.modify` |  |
| `/permissions group setpriority` | `neoessentials.permissions.group.modify` |  |
| `/permissions group context add/remove/list` | `neoessentials.permissions.group.context` |  |
| `/permissions group addtemp/removetemp` | `neoessentials.permissions.group.temp` |  |
| `/permissions group create` | `neoessentials.permissions.group.create` |  |
| `/permissions group delete` | `neoessentials.permissions.group.delete` |  |
| `/permissions user setgroup` | `neoessentials.permissions.user.group` |  |
| `/permissions user add/remove` | `neoessentials.permissions.user.permissions` |  |
| `/permissions user context add/remove/list` | `neoessentials.permissions.user.context` |  |
| `/permissions user addtemp/removetemp` | `neoessentials.permissions.user.temp` |  |
| `/permissions reload` | `neoessentials.permissions.reload` |  |
| `/permissions debug <player>` | `neoessentials.permissions.debug` |  |

### Negative permissions as fine-grained deny

Use a `-` prefix on any node to explicitly deny a subcommand even when a wildcard grants the parent:

```
neoessentials.*                       # grant all
-neoessentials.item.enchant.unsafe    # but deny unsafe enchanting specifically
-neoessentials.moderation.banip       # and deny IP banning
```

---

## GUI Management — Web Dashboard API

The web dashboard exposes a full REST API for permission management.
All endpoints require Bearer token authentication (`Authorization: Bearer <token>`).

**Base URL:** `http://<server>:<port>/api/permissions`

### Quick reference

| Method | Path | Description |
|---|---|---|
| `GET` | `/overview` | Summary: total groups, users, system type |
| `GET` | `/system/status` | Detailed system state: emergency mode, adapter health, alias count |
| `GET` | `/groups` | List all groups |
| `GET` | `/group/{name}` | Get group detail |
| `POST` | `/group/create` | Create group `{name, prefix?, suffix?}` |
| `PUT` | `/group/{name}/update` | Update group `{prefix?, suffix?, priority?}` |
| `DELETE` | `/group/{name}` | Delete group |
| `POST` | `/group/{name}/permission/add` | Add permission `{permission}` |
| `DELETE` | `/group/{name}/permission/remove/{node}` | Remove permission |
| `GET` | `/group/{name}/context` | List contextual overrides |
| `POST` | `/group/{name}/context` | Add override `{contextKey, node, allow}` |
| `DELETE` | `/group/{name}/context` | Remove override `{contextKey, node}` (body) |
| `GET` | `/group/{name}/temp` | List temp permissions with time remaining |
| `POST` | `/group/{name}/temp` | Add temp permission `{node, duration}` |
| `DELETE` | `/group/{name}/temp/{node}` | Remove temp permission |
| `GET` | `/users` | List all known users |
| `GET` | `/user/{name}` | Get user detail |
| `PUT` | `/user/{name}/update` | Update user permissions |
| `POST` | `/user/{name}/group/set` | Set user group `{group}` |
| `POST` | `/user/{name}/permission/add` | Add permission `{permission}` |
| `DELETE` | `/user/{name}/permission/remove/{node}` | Remove permission |
| `GET` | `/user/{name}/context` | List user contextual overrides |
| `POST` | `/user/{name}/context` | Add user override `{contextKey, node, allow}` |
| `DELETE` | `/user/{name}/context` | Remove user override `{contextKey, node}` (body) |
| `GET` | `/user/{name}/temp` | List user temp permissions |
| `POST` | `/user/{name}/temp` | Add user temp permission `{node, duration}` |
| `DELETE` | `/user/{name}/temp/{node}` | Remove user temp permission |
| `GET` | `/permissions/all` | All registered permission nodes |
| `GET` | `/aliases` | List all registered aliases |
| `POST` | `/aliases` | Register alias `{alias, canonical}` |
| `DELETE` | `/aliases/{alias}` | Remove alias |
| `POST` | `/reload` | Reload all permissions from disk |

### Example: add a contextual permission via the API

```bash
curl -X POST http://localhost:8080/api/permissions/group/vip/context \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"contextKey":"world:overworld","node":"neoessentials.fly","allow":true}'
```

### Example: register an alias

```bash
curl -X POST http://localhost:8080/api/permissions/aliases \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"alias":"essentials.fly","canonical":"neoessentials.fly"}'
```

