# Custom Player Tablist System

> **Config file:** `config/neoessentials/tablist.json`  
> **Reload in-game:** `/tablist reload`  
> **Admin permission:** `neoessentials.tablist.admin`

---

## Overview

The NeoEssentials tablist system replaces the vanilla player list header and footer with a fully
customizable, animated display. It supports:

| Feature | Details |
|---------|---------|
| **Animated frames** | Header and footer cycle through an array of strings at a configurable tick rate |
| **Hex colors** | `&#RRGGBB` inline hex color codes |
| **Gradients** | `<gradient:RRGGBB-RRGGBB>text</gradient>` — 2-stop or multi-stop |
| **Rainbow** | `<rainbow>text</rainbow>` — automatic ROYGBIV cycling |
| **Named colors** | `<red>`, `<gold>`, `<green>`, `<aqua>`, `<blue>`, … |
| **Format tags** | `<bold>`, `<italic>`, `<underline>`, `<strikethrough>` |
| **Per-group** | Different header/footer for each permission group |
| **Per-player** | Individual header/footer overrides per player UUID |
| **Placeholders** | Player info, server stats, economy balance, permission group |

References: [TAB](https://github.com/NEZNAMY/TAB), [BungeeTabListPlus](https://github.com/CodeCrafter47/BungeeTabListPlus), [Simple TabList](https://modrinth.com/plugin/simple-tablist)

---

## Color & Formatting Syntax

All color and formatting tags are supported in `header`, `footer`, `playerFormat`, `afkSuffix`,
`groupColors`, and every per-group / per-player override field.

### Legacy Codes (`&`)
```
&0 &1 &2 &3 &4 &5 &6 &7 &8 &9  — color codes (black → dark grey)
&a &b &c &d &e &f              — color codes (green → white)
&k  obfuscated   &l  bold   &m  strikethrough
&n  underline    &o  italic  &r  reset
```

### Hex Colors
```
&#RRGGBB           — inline  e.g.  &#FF5500  &#00AAFF
<color:#RRGGBB>text</color>   — span with explicit close
```

### Gradients
```
<gradient:FF0000-0000FF>text</gradient>          — 2-stop (red → blue)
<gradient:FF0000-FFFF00-00FF00>text</gradient>   — multi-stop (red → yellow → green)
```
> **Tip:** Keep plain text inside gradient tags.  Avoid mixing `&` format codes inside the
> gradient span — place them before/after the tag instead.

### Rainbow
```
<rainbow>text</rainbow>
```

### Named Colors
```
<red>  <dark_red>  <gold>  <yellow>  <green>  <dark_green>
<aqua> <dark_aqua> <blue>  <dark_blue> <light_purple> <dark_purple>
<white>  <gray>  <dark_gray>  <black>
```

### Format Tags
```
<bold>text</bold>   <italic>text</italic>   <underline>text</underline>
<strikethrough>text</strikethrough>   <obfuscated>text</obfuscated>   <reset>
```

---

## Placeholders

| Placeholder | Description |
|-------------|-------------|
| `{player}` | Viewing player's username |
| `{displayname}` | Display name (respects nick override + group colour) |
| `{online}` | Online player count (respects `hideVanished`) |
| `{max}` | Server max slots |
| `{ping}` | Viewing player's ping (ms) |
| `{world}` | Current dimension path (`overworld`, `the_nether`, `the_end`) |
| `{tps}` | Server TPS — auto-colored green/yellow/red |
| `{time}` | Server real-world time (`HH:mm`) |
| `{server_name}` | Server name from `server.properties` MOTD |
| `{x}` `{y}` `{z}` | Viewing player's block coordinates |
| `{balance}` | Economy balance (requires EconomyManager) |
| `{prefix}` | Permission group prefix |
| `{suffix}` | Permission group suffix |
| `{group}` | Permission group name |
| `{newline}` | Line break `\n` |
| `{bar}` | Decorative strikethrough separator line |

---

## Configuration

### `tablist.json` structure

```json
{
  "_configVersion": 2,
  "tablist": {
    "enabled": true,
    "refreshInterval": 20,

    "header": [
      "<gradient:FFD700-FF8C00>&l✦ {server_name} ✦&r &8| &e{online}&8/&e{max} &7players",
      "<gradient:FF8C00-FFD700>&l✦ {server_name} ✦&r &8| &eTPS: {tps}"
    ],

    "footer": [
      "&7TPS: {tps} &8| &7Ping: &a{ping}&7ms &8| <green>{world}",
      "&7Balance: &a{balance} &8| &7Group: &#FFD700{group}"
    ],

    "playerFormat": "&f{prefix}&r{player}{suffix}",

    "hideVanished": true,
    "showAfkIndicator": true,
    "afkSuffix": " &7[AFK]",

    "groupColors": {
      "admin":   "&c",
      "vip":     "&#00CFFF",
      "default": "&f"
    },

    "groups": {
      "admin": {
        "header": [
          "<gradient:FF0000-AA0000>&l⚑ ADMIN PANEL ⚑&r {newline}&8{player} — ping &e{ping}ms",
          "<gradient:AA0000-FF0000>&l⚑ ADMIN PANEL ⚑&r {newline}&8Coords &e{x}&8, &e{y}&8, &e{z}"
        ],
        "footer": "&cAdmin tools: &f/vanish /kick /ban /mute &8| &7{world}"
      },
      "vip": {
        "header": [
          "<gradient:00CFFF-0080FF>&l★ VIP ★&r &8| &e{online}&8/&e{max} &7players",
          "<gradient:0080FF-00CFFF>&l★ VIP ★&r &8| &7Welcome back, &b{player}!"
        ],
        "footer": "&#00CFFF{player}&7 — &aBalance: &f{balance} &8| &7Ping: &a{ping}ms"
      }
    },

    "players": {
      "00000000-0000-0000-0000-000000000000": {
        "header": "<rainbow>Hello, {player}!</rainbow>",
        "footer": "&#FF5500Special player footer"
      }
    }
  }
}
```

### Key fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | `true` | Enables/disables the whole system |
| `refreshInterval` | int | `20` | Update interval in ticks (20 = 1 s) |
| `header` | string or array | — | Header frame(s) |
| `footer` | string or array | — | Footer frame(s) |
| `playerFormat` | string | `"&f{prefix}&r{player}{suffix}"` | Player row format |
| `hideVanished` | boolean | `true` | Hide vanished players from `{online}` |
| `showAfkIndicator` | boolean | `true` | Show AFK suffix on player rows |
| `afkSuffix` | string | `" &7[AFK]"` | AFK display suffix |
| `groupColors` | object | — | Quick `{displayname}` colour per group |
| `groups` | object | — | Per-group `header`/`footer` overrides |
| `players` | object | — | Per-player UUID `header`/`footer` overrides |

---

## Priority

The system uses the following priority chain for choosing which header/footer to display:

```
per-player override  >  per-group override  >  global default
```

A player in the `vip` group with a per-player override will always see their own custom
header, not the VIP group one.

---

## Commands

All sub-commands require the `neoessentials.tablist.admin` permission (or operator level 4).

| Command | Description |
|---------|-------------|
| `/tablist` | Show help |
| `/tablist reload` | Reload `tablist.json` and push changes immediately |
| `/tablist enable` | Enable the tablist system |
| `/tablist disable` | Disable; restores vanilla appearance |
| `/tablist preview` | Force-send your own current header/footer |
| `/tablist info` | Show status, frame counts, active group overrides |
| `/tablist set header <text>` | Set global header at runtime (reset on reload) |
| `/tablist set footer <text>` | Set global footer at runtime (reset on reload) |
| `/tablist player <name> header <text>` | Set per-player header override |
| `/tablist player <name> footer <text>` | Set per-player footer override |
| `/tablist player <name> reset` | Clear all per-player overrides |
| `/tablist group <group> header <text>` | Set per-group header override |
| `/tablist group <group> footer <text>` | Set per-group footer override |
| `/tablist group <group> reset` | Clear per-group overrides |

Runtime overrides set via `/tablist player` or `/tablist group` are **not persisted** to disk.
Add them to `tablist.json → players` / `tablist.json → groups` to survive reloads.

---

## Animation

Set `header` (or `footer`) to a JSON **array** to enable frame cycling:

```json
"header": [
  "<gradient:FFD700-FF8C00>&l★ My Server ★",
  "<gradient:FF8C00-FFD700>&l★ My Server ★",
  "<rainbow>&lMy Server"
]
```

Each frame is displayed for `refreshInterval` ticks before advancing.  
Frames for per-group and per-player overrides use the same global counter (synchronized).

**Lower `refreshInterval` → smoother animations but more network packets.**  
Recommended: `10` for smooth (½ s per frame), `20` for light (1 s per frame).

---

## Integration with Other Systems

### Nick System
When a player is nicknamed via `/nick`, their display name in the tablist row automatically
updates via `TablistManager.setCustomName(uuid, nick)`.

### AFK System
The AFK system calls `TablistManager.getAfkSuffix()` and appends it to the player's team
suffix when they go AFK, and removes it when they return.

### Vanish System
Vanished players are excluded from `{online}` counts and scoreboard teams when
`hideVanished: true`.  Staff with `neoessentials.vanish.see` still see them.

### Permission System
- `{prefix}`, `{suffix}`, `{group}` placeholders pull from the permission group system.
- Group prefix/suffix values support all color syntax (hex, gradients, etc.).

---

## Examples

### Gradient server name
```json
"header": "<gradient:FF0000-FFFF00-00FF00>&l{server_name}</gradient>"
```

### Per-player VIP badge
```json
"players": {
  "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx": {
    "header": "<gradient:FFD700-FF8C00>&l✦ Super VIP ✦&r{newline}&7Welcome, &6{player}!",
    "footer": "&#FFD700★ §rBeta Tester &8| &7{ping}ms"
  }
}
```

### Static hex header
```json
"header": "&#FF5500My &lServer &#00AAFF— {online}/{max} online"
```

### Minimal animated frames
```json
"header": [
  "&6&l★ My Server ★",
  "&e&l★ My Server ★",
  "&a&l★ My Server ★"
],
"footer": "&7{online}/{max} online — {tps} TPS"
```

