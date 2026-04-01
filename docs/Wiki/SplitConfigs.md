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
| `main.json` | `modules`, `logging`, `permissions`, `kits` (settings), `economy` |
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
  ✔ main.json          — modules, logging, permissions, kits, economy
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

## allowUnsafeCommands

This setting is in **`security.json`** under the `security` section:

```json
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
| `allowUnsafeCommands` | boolean | `false` | Allow enchantments and item operations beyond vanilla limits |
| `maxUnsafeEnchantmentLevel` | int | `10` | Max level allowed when unsafe enchantments are on |

In monolithic mode it is at `config.json → security.allowUnsafeCommands`.

---

## Command Reference

| Command | Permission | Description |
|---|---|---|
| `/neoe config split` | `neoessentials.admin.reload` | Migrate monolithic config.json to split files |
| `/neoe config status` | `neoessentials.admin.reload` | Show which config files are present |
| `/neoe config validate` | `neoessentials.admin.reload` | Check all split files for problems |
| `/neoe config repair` | `neoessentials.admin.reload` | Auto-fix missing/incomplete split files |
| `/neoe reload` | `neoessentials.admin.reload` | Apply config changes to all live systems |

