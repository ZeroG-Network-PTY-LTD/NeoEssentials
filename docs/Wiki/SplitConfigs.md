# Split Configuration System

> **Version:** 1.0.5+build.54 · **Config dir:** `config/neoessentials/`

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
| `items.json` | `items` (item spawn, enchantments, stack sizes) and `shop` (dynamic pricing settings — no dedicated split file of its own, so it lands here) |
| `afk.json` | `afk` (AFK timeout, kick, broadcast messages) |
| `security.json` | `security` (input validation, unsafe commands) |
| `tablist.json` | `tablist` (header, footer, player row format, animation) |
| `templates/discord_embed.json` | `discordEmbedTemplate` (Discord chat-embed styling — see [Chat Channels](ChatChannels#discord-interoperability-avoiding-duplicate--leaked-messages)) |

> **Note — `templates/discord_embed.json` is the one split file that lives in a subdirectory.**
> Every other split file sits directly in `config/neoessentials/`; this one lives in its own
> `config/neoessentials/templates/` folder instead. The folder is created automatically the
> first time this file is generated (fresh install, `/neoe config split`, or `/neoe config
> repair`) — no manual setup needed.

> **Note:** `kits.json` historically held **kit definitions** (the actual kit contents as a JSON
> array). It's now a **legacy file** — kit definitions are persisted through the pluggable
> DataStore backend (the `kits` collection; see [Storage Backend](Storage)), and `kits.json` is
> only read once, automatically, to migrate old data in.  
> Kit *settings* (cooldowns, costs, auto-equip flags) live in `main.json` under the `kits` key
> — this part of the split-config system is unaffected by the DataStore migration.

> **Note — `webDashboard` is not part of the split system.** Unlike every other top-level
> section, `webDashboard` (dashboard port, auth, UI settings) is **not** migrated to its own
> split file by `ConfigSplitter` (see `ConfigSplitter.FILE_SECTIONS_MAP` in the source).
> When split configs are enabled, the merged virtual config has no `webDashboard` section, so
> `ConfigManager.getWebDashboardPort()` / `isWebDashboardEnabled()` / related getters silently
> fall back to their hard-coded defaults (port `8080`, enabled `true`, etc.). If you need to
> customise the web dashboard, do so **before** migrating to split configs, or edit it back into
> `config.json` while running in monolithic mode.

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
  ✔ items.json         — items, shop
  ✔ afk.json           — afk
  ✔ security.json      — security
  ✔ tablist.json       — tablist
  ✔ templates/discord_embed.json — discordEmbedTemplate
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
    "allowUnsafeCommands": true,
    "enableCommandLengthEnforcer": true
  }
}
```

| Key | Type | Default | Description |
|---|---|---|---|
| `enableInputValidation` | boolean | `true` | Master switch — disabling this turns off all checks below |
| `maxCommandLength` | int | `256` | Maximum characters allowed in a `/powertool` command string (also enforced globally on every player-typed command — see `enableCommandLengthEnforcer`) |
| `maxReasonLength` | int | `500` | Maximum characters in ban/kick/mute reasons |
| `allowUnsafeCommands` | boolean | `true` | Ships enabled by default. See full explanation below |
| `enableCommandLengthEnforcer` | boolean | `true` | Whether the `CommandLengthEnforcer` event handler validates player-typed commands (length + the dangerous-pattern check below) for **every** command, not just `/powertool` bindings |

> **Path-traversal / XSS protection is not separately toggleable.** There is no
> `enablePathTraversalProtection` or `enableXSSProtection` config key — path-traversal
> checks (`../`, `..\`) run whenever `enableInputValidation` is on (see `validateFilePath`
> in `InputValidator`), and basic XSS-pattern rejection (`<script`, `javascript:`, etc.) runs
> whenever `validateReason` is used (ban/kick/mute reasons), gated by the same
> `enableInputValidation` switch.

---

### `allowUnsafeCommands` — Full Explanation

> **Note:** `allowUnsafeCommands` ships **enabled (`true`) by default** — the dangerous-command
> and unsafe-character checks below only run when it's explicitly set to `false`. This section
> mainly matters for admins who've locked it down and want to know exactly what gets blocked.

#### What it does

`InputValidator.validateCommand()` is used both for `/powertool` command bindings and — via
`CommandLengthEnforcer`, gated by `enableCommandLengthEnforcer` — for **every command a player
types**, not just powertool bindings. When `allowUnsafeCommands` is `false`, it runs two checks:

1. **Dangerous-pattern check** — rejects commands containing any of the following substrings
   (deliberately narrow — see note below):

   | Blocked substring | Why |
   |---|---|
   | `../`, `..\` | Path traversal |
   | `class.forname`, `reflection` | Java reflection abuse |
   | `file:`, `http:`, `https:`, `ftp:`, `jar:` | URL/protocol injection |

2. **Unsafe-character check** — rejects commands containing C0 control characters or a
   backtick (`` ` ``). Everything else — including `~`, `@`, `{`, `}`, `$`, `&&`, `;`, and other
   punctuation used by vanilla selectors/NBT/coordinates — is allowed through.

> **Why so narrow?** Earlier versions of this check used a much longer deny-list (`rm `,
> `eval`, `exec`, `~`, `$`, `` ` ``, `&&`, `\|\|`, `;`, etc.) and an *allow-list* of characters
> that didn't even include `@`. That blocked huge swaths of completely ordinary, safe Minecraft
> syntax by default — target selectors (`@a`, `@e[type=cow]`), relative coordinates (`~ ~6 ~`),
> and `/execute` itself (which contains `exec`). The current check only flags patterns with a
> genuine injection/traversal risk in the context of a real Minecraft command string.

---

#### Commands that work regardless of `allowUnsafeCommands`

None of these match the dangerous-pattern or unsafe-character checks, so they pass even with
`allowUnsafeCommands: false`:

```
/give @s minecraft:diamond 1
/effect give @s minecraft:speed 30 1
/say Hello world
/gamemode creative
/tp PlayerName
/tp ~ 100 ~
/execute as @a run say hi
/tellraw @a {"text":"hi"}
/time set day
/weather clear
/neoe heal
/neoe fly on
```

#### Commands that only get blocked once you set `allowUnsafeCommands: false`

`allowUnsafeCommands: true` (the default) skips validation entirely, so **nothing** in
`validateCommand()` is enforced — these examples pass under the default. Set
`allowUnsafeCommands: false` if you need the dangerous-pattern/backtick checks to run:

```
/say `whoami`           ← blocked (with allowUnsafeCommands: false): contains a backtick
/give @s item{Lore:["../../etc"]}  ← blocked (with allowUnsafeCommands: false): contains ../
```

---

#### How to lock it down

`allowUnsafeCommands` ships as `true`, so no action is needed for the checks above to stay
off. If you want the dangerous-pattern and backtick/control-character checks enforced (e.g.
you let untrusted players use `/powertool`), set it to `false`:

**Split config mode** — edit `config/neoessentials/security.json`:

```json
{
  "security": {
    "enableInputValidation": true,
    "maxCommandLength": 256,
    "maxReasonLength": 500,
    "allowUnsafeCommands": false,
    "enableCommandLengthEnforcer": true
  }
}
```

**Monolithic config mode** — edit `config/neoessentials/config.json`:

```json
"security": {
  "enableInputValidation": true,
  "maxCommandLength": 256,
  "maxReasonLength": 500,
  "allowUnsafeCommands": false,
  "enableCommandLengthEnforcer": true
}
```

Then reload without restart:

```
/neoe reload
```

---

#### Security considerations

With the shipped default (`allowUnsafeCommands: true`), players with `neoessentials.item.powertool`
permission can bind **any** command string to an item, and `CommandLengthEnforcer` only enforces
`maxCommandLength` (not the dangerous-pattern/character checks) on everyday typed commands.  
On servers where powertool is restricted to trusted players (OPs / admin rank), this default is
usually fine. If you allow regular players to use powertools, setting `allowUnsafeCommands: false`
adds the dangerous-pattern and backtick/control-character checks back for both `/powertool`
bindings and every player-typed command.

> **Recommendation:** Give `neoessentials.item.powertool` only to trusted staff if you keep the
> default `allowUnsafeCommands: true`; otherwise set it to `false`.

---

## Command Reference

| Command | Permission | Description |
|---|---|---|
| `/neoe config split` | `neoessentials.admin.reload` | Migrate monolithic config.json to split files |
| `/neoe config status` | `neoessentials.admin.reload` | Show which config files are present |
| `/neoe config validate` | `neoessentials.admin.reload` | Check all split files for problems |
| `/neoe config repair` | `neoessentials.admin.reload` | Auto-fix missing/incomplete split files |
| `/neoe reload` | `neoessentials.admin.reload` | Apply config changes to all live systems |

