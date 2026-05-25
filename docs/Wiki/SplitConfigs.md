# Split Configuration System

> **Version:** 1.0.2.6 · **Config dir:** `config/neoessentials/`

---

## What Are Split Configs?

By default NeoEssentials stores all settings in a single `config.json` file (~860 lines).  
**Split configs** divide that file into smaller, focused files — one per subsystem — so you can edit only the part you need without scrolling past hundreds of unrelated settings.

Split configs are **recommended for all servers**.  
A `.split_configs` marker file in `config/neoessentials/` activates split mode. When active, `config.json` is replaced with a stub that redirects to the split files.

---

## File Layout

| File | Contains |
|---|---|
| `main.json` | `modules`, `logging`, `localization`, `permissions`, `kits` (settings), `economy` |
| `commands.json` | `commands` (enable/disable toggles for every command) |
| `chat.json` | `chat` (formatting, channels, anti-spam, badges, rich text) |
| `teleportation.json` | `teleportation` (homes, warps, spawn, TPA, random TP) |
| `moderation.json` | `moderation` (ban, jail, vanish, freeze, kick) |
| `webdashboard.json` | `webDashboard` (dashboard port, auth, UI settings) |
| `items.json` | `items` (item spawn, enchantments, stack sizes) |
| `afk.json` | `afk` (AFK timeout, kick, broadcast messages) |
| `security.json` | `security` (input validation, unsafe commands) |
| `tablist.json` | `tablist` (header, footer, player row format, animation) |

> **Note:** `kits.json` holds **kit definitions** (the actual kit contents as a JSON array).  
> Kit *settings* (cooldowns, costs, auto-equip flags) live in `main.json` under the `kits` key.

---

## Migrating From Monolithic Config

Run this command in-game (requires `neoessentials.admin.reload` permission or OP):

```
/neoe config split
```

This will:
1. Back up your current `config.json` → `config.json.backup`
2. Extract each section into its own file
3. Replace `config.json` with a stub file
4. Create the `.split_configs` marker

**No settings are lost** — the backup is always created first.

---

## Fresh Installs

On a brand-new server where `config.json` has never existed, NeoEssentials automatically creates all split files from its bundled defaults and activates split mode.

---

## Checking Config Health

```
/neoe config status     — Shows which files are present/missing and the current mode
/neoe config validate   — Checks every file for missing sections and parse errors
/neoe config repair     — Automatically regenerates missing files and fills missing sections
```

### Example status output

```
━━━━━━━━━ Config Status ━━━━━━━━━
Mode: Split configs (recommended)
Files:
  ✔ main.json          — modules, logging, localization, permissions, kits, economy
  ✔ commands.json      — commands
  ✔ chat.json          — chat
  ✔ teleportation.json — teleportation
  ✔ moderation.json    — moderation
  ✔ webdashboard.json  — webDashboard
  ✔ items.json         — items
  ✔ afk.json           — afk
  ✔ security.json      — security
  ✔ tablist.json       — tablist
✔ All files present and valid.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Example validation output (when a file is missing)

```
⚠ Split config validation found 1 problem(s):
  • MISSING FILE: chat.json  →  should contain: chat  →  fix with: /neoe config repair
Run /neoe config repair to fix automatically.
```

---

## Repairing Split Configs

If a split file is deleted or becomes corrupted run:

```
/neoe config repair
```

NeoEssentials will:
- Regenerate missing files from the bundled JAR default
- Add any missing sections to existing files (without overwriting your custom values)
- Leave already-correct files untouched

After repair, reload with:

```
/neoe reload
```

---

## Automatic Startup Checks

Every time the server starts with split configs enabled, NeoEssentials:
1. Checks that every expected file exists
2. Checks every file's `_configVersion` — merges new keys if outdated
3. Logs a boxed error with remediation steps if a file cannot be regenerated

Missing files are **automatically regenerated** from the JAR default.  
The server **will start normally** even if some split files are missing (defaults are used in-memory).

---

## Version Tracking

Each split file has a `_configVersion` integer at the top:

```json
{
  "_configVersion": 1,
  "_configVersion_comment": "DO NOT MODIFY: Used by NeoEssentials for automatic config updates.",
  ...
}
```

When the mod adds new config keys, only **new keys** are merged in — your existing values are never overwritten. A timestamped backup is created before any merge.

---

## Disabling Split Configs

To return to monolithic mode:
1. Delete `config/neoessentials/.split_configs`
2. Rename `config/neoessentials/config.json.backup` → `config.json`
3. Restart the server

---

## Security Configuration (`security.json`)

In **split config mode** all security settings live in `security.json`.  
In **monolithic mode** they are under the `security` key in `config.json`.

```json
// split:    config/neoessentials/security.json
// monolith: config/neoessentials/config.json  →  "security": { ... }
{
  "security": {
    "enableInputValidation": true,
    "maxCommandLength": 256,
    "maxReasonLength": 500,
    "allowUnsafeCommands": false,
    "enablePathTraversalProtection": true,
    "enableXSSProtection": true
  }
}
```

| Key | Type | Default | Description |
|---|---|---|---|
| `enableInputValidation` | boolean | `true` | Master switch — disabling this turns off all checks below |
| `maxCommandLength` | int | `256` | Maximum characters allowed in a `/powertool` command string |
| `maxReasonLength` | int | `500` | Maximum characters in ban/kick/mute reasons |
| `allowUnsafeCommands` | boolean | `false` | See full explanation below |
| `enablePathTraversalProtection` | boolean | `true` | Block `../` and `..\\` in any user-supplied file path |
| `enableXSSProtection` | boolean | `true` | Block `<script>`, `javascript:`, and similar XSS patterns in text inputs |

---

### `allowUnsafeCommands` — Full Explanation

> **TL;DR:** If your players get *"Command contains potentially dangerous operations"* when setting up a powertool, set `allowUnsafeCommands: true` in `security.json` (or `config.json → security` in monolithic mode).

#### What it does

When `allowUnsafeCommands` is `false` (the default), NeoEssentials validates any command string a player tries to bind via `/powertool` through two checks:

1. **Dangerous-pattern check** — rejects commands containing any of the following substrings:

   | Blocked substring | Why |
   |---|---|
   | `rm `, `del `, `delete `, `format`, `shutdown`, `reboot` | System-level destructive operations |
   | `eval`, `exec`, `system`, `runtime`, `process` | Code-execution / reflection vectors |
   | `../`, `..\` | Path traversal |
   | `~` | Shell home-dir expansion (also used as relative coord — see note below) |
   | `$`, `` ` `` | Shell variable / command substitution |
   | `&&`, `\|\|`, `;` | Shell command chaining |
   | `file:`, `http:`, `https:`, `ftp:`, `jar:` | URL/protocol injection |
   | `class.forname`, `reflection`, `unsafe` | Java reflection abuse |

2. **Character-allowlist check** — rejects commands that contain characters outside:  
   `A-Z a-z 0-9 _ - / (space) : . & # ~`

   Characters **not** in this allowlist include `%`, `@`, `=`, `<`, `>`, `{`, `}`, `[`, `]`, `"`, `'`, `\`, `?`, `!`, `(`, `)`, `*`, `+`, `,` and others.

> ⚠️ **Important — tilde (`~`) in Minecraft commands:**  
> The `~` character is used for relative coordinates (e.g. `/tp ~ 100 ~`).  
> With `allowUnsafeCommands: false` any command containing `~` will be blocked because it also matches the dangerous-pattern list.  
> If your admins need powertools with relative coordinates, set `allowUnsafeCommands: true`.

---

#### Commands that work by default (`allowUnsafeCommands: false`)

These commands pass both checks and can be bound to powertools without any config change:

```
/give @s minecraft:diamond 1
/effect give @s minecraft:speed 30 1
/say Hello world
/gamemode creative
/tp PlayerName
/time set day
/weather clear
/neoe heal
/neoe fly on
```

#### Commands that are blocked by default and need `allowUnsafeCommands: true`

```
/tp ~ 100 ~            ← blocked: contains ~
/tp ~0 ~10 ~0          ← blocked: contains ~
/execute as @a run ...  ← blocked: contains @
/tellraw @a {...}       ← blocked: contains @ and {
/give @s shulker_box{...}  ← blocked: contains @ and {
/say Hello & Goodbye   ← blocked: contains & (only &# codes are allowed, bare & is not)
```

---

#### How to enable

**Split config mode** — edit `config/neoessentials/security.json`:

```json
{
  "security": {
    "enableInputValidation": true,
    "maxCommandLength": 256,
    "maxReasonLength": 500,
    "allowUnsafeCommands": true,
    "enablePathTraversalProtection": true,
    "enableXSSProtection": true
  }
}
```

**Monolithic config mode** — edit `config/neoessentials/config.json`:

```json
"security": {
  "enableInputValidation": true,
  "maxCommandLength": 256,
  "maxReasonLength": 500,
  "allowUnsafeCommands": true,
  "enablePathTraversalProtection": true,
  "enableXSSProtection": true
}
```

Then reload without restart:

```
/neoe reload
```

---

#### Security considerations

Enabling `allowUnsafeCommands: true` means players with `neoessentials.item.powertool` permission can bind **any** command string to an item.  
On servers where powertool is restricted to trusted players (OPs / admin rank), this is usually fine.  
If you allow regular players to use powertools, keeping `allowUnsafeCommands: false` reduces the risk of players binding commands that could grief or exploit the server.

> **Recommendation:** Give `neoessentials.item.powertool` only to trusted staff, then set `allowUnsafeCommands: true` for maximum flexibility.

---

## Command Reference

| Command | Permission | Description |
|---|---|---|
| `/neoe config split` | `neoessentials.admin.reload` | Migrate monolithic config.json to split files |
| `/neoe config status` | `neoessentials.admin.reload` | Show which config files are present |
| `/neoe config validate` | `neoessentials.admin.reload` | Check all split files for problems |
| `/neoe config repair` | `neoessentials.admin.reload` | Auto-fix missing/incomplete split files |
| `/neoe reload` | `neoessentials.admin.reload` | Apply config changes to all live systems |

