# Hologram System

NeoEssentials uses Minecraft's native `Display.TextDisplay` entities to render holographic floating text in the world.  
Holograms are fully server-side — no client mod required.

---

## Storage

Hologram data lives in the active [storage backend](Storage) (`holograms` collection — JSON files by default, or SQLite/MySQL if configured), loaded automatically on server start / world load. The legacy `neoessentials/holograms.json` file is only read once, to migrate any existing data into the storage backend on first boot after upgrading — it's not written to afterward.  
Shop-managed holograms (prefixed `shop_`) are created and destroyed automatically by the Shop system — see [Shop Holograms](#shop-holograms) below.

---

## Permissions

| Permission | Description |
|---|---|
| `neoessentials.hologram.admin` | Full access to all `/hologram` commands (also granted by OP level 4 or `neoessentials.admin.*`) |

---

## Command Reference

All commands use `/hologram` (alias `/holo`).

### Creation & Deletion

| Command | Description |
|---|---|
| `/hologram create <id> <x> <y> <z> [world]` | Create a new empty hologram at the given coordinates. `world` defaults to your current dimension (e.g. `minecraft:overworld`). |
| `/hologram delete <id>` | Permanently delete a hologram. |
| `/hologram rename <id> <newid>` | Rename a hologram in-place (keeps position, lines, and settings). |
| `/hologram copy <id> <newid>` | Clone an existing hologram (all lines, animations, and visual settings) under a new ID at the same position. |
| `/hologram reload` | Despawn all holograms, reload `holograms.json` from disk, and respawn everything. |

### Line Management

Line numbers are **1-based** (1 = topmost) in every command below.

| Command | Description |
|---|---|
| `/hologram addline <id> <text>` | Append a new text line to the bottom of the hologram. |
| `/hologram insertline <id> <index> <text>` | Insert a line **before** `index` (1 = topmost). Existing lines shift down. `index` may also equal `lineCount + 1` to insert at the end. |
| `/hologram setline <id> <index> <text>` | Replace the text of an existing line (1-based index). Clears any frame animation on that line. |
| `/hologram removeline <id> <index>` | Delete a specific line (1-based index). |
| `/hologram moveline <id> <from> <to>` | Reorder a line within the hologram (1-based indices). |
| `/hologram clearlines <id>` | Remove all lines from the hologram. |

### Frame Animation (per-line)

Lines can cycle through multiple text frames automatically.

| Command | Description |
|---|---|
| `/hologram addframes <id> <lineIndex> <intervalTicks> <frame1\|frame2\|...>` | Set animated frames for a line (1-based `lineIndex`). Requires at least 2 frames, separated by `\|`. `intervalTicks` controls how many ticks each frame is shown (1–200; 20 ticks = 1 second). |
| `/hologram removeframes <id> <lineIndex>` | Remove frame animation from a line, restoring static text. |

**Example:**
```
/hologram addframes welcome 1 10 &aWelcome!|&eHello!|&bHi there!
```
This cycles the top line (line 1) every 10 ticks (0.5 s) through three coloured greetings.

### Positioning

| Command | Description |
|---|---|
| `/hologram moveto <id> <x> <y> <z>` | Teleport the hologram to exact coordinates. |
| `/hologram movehere <id>` | Move the hologram to your current standing position (Y offset +1.5). |
| `/hologram align <id>` | Snap the hologram's X/Z to the nearest block centre (`floor(x)+0.5`, `floor(z)+0.5`). |
| `/hologram tp <id>` | Teleport yourself to the hologram's position. |
| `/hologram near [radius]` | List all holograms within `radius` blocks (default 20, max 1000) sorted by distance. |

### Visibility & Refresh

| Command | Description |
|---|---|
| `/hologram toggle <id>` | Show or hide the hologram without deleting it. |
| `/hologram setrefresh <id> <seconds>` | How often placeholders are re-evaluated. `0` = static (never refresh). |
| `/hologram list` | List all holograms with their world, position, and line count. |
| `/hologram info <id>` | Detailed info including all visual/animation settings and line contents. |

### Billboard Mode

Controls how the hologram faces the viewer.

| Command | Description |
|---|---|
| `/hologram billboard <id> <mode>` | Set billboard: `fixed`, `vertical`, `horizontal`, or `center` (default). |

| Mode | Behaviour |
|---|---|
| `center` | Always faces the player (default). |
| `vertical` | Rotates horizontally only; stays upright. |
| `horizontal` | Rotates to lie flat on the floor/ceiling. |
| `fixed` | No automatic rotation; use with `/hologram spin` for world-space effects. |

### Text Alignment

| Command | Description |
|---|---|
| `/hologram textalign <id> <mode>` | Set per-line text alignment: `center` (default), `left`, or `right`. |

### Animations

#### Spin

| Command | Description |
|---|---|
| `/hologram spin <id> on [speed] [axis]` | Enable rotation. `speed` = degrees/tick, `0.1`–`30.0` (default `3.0`). `axis` = `X`, `Y`, or `Z` (default `Y`). |
| `/hologram spin <id> off` | Stop rotation. |
| `/hologram spintrack <id> on\|off` | Toggle player-tracking for Y-axis spin (see below). Enabled automatically when `/hologram spin <id> on` is used with the `Y` axis. |

**Tip:** `Y`-axis spin with `fixed` billboard = classic record-player spin.  
`Z`-axis spin with `center` billboard = roll-spin visible to all players.

**Player-tracking (`Y`-axis only, default on):** instead of a raw world-space spin, the renderer
switches the billboard to `FIXED` and computes the yaw as `yaw_to_nearest_player + currentSpinAngle`,
so the "front" of the text sweeps past every player once per revolution rather than only being
readable from one fixed direction. Disable with `/hologram spintrack <id> off` to fall back to a
raw `Y`-axis rotation (only useful if you also set billboard to `fixed` manually).

#### Hover / Bob

| Command | Description |
|---|---|
| `/hologram hover <id> on [amplitude] [speed]` | Smooth up-and-down bob. `amplitude` = peak displacement in blocks, `0.01`–`2.0` (default `0.08`). `speed` = degrees/tick of the sine wave, `0.1`–`10.0` (default `1.5`). |
| `/hologram hover <id> off` | Stop hover animation. |

### Visual Appearance

| Command | Description |
|---|---|
| `/hologram scale <id> <scale>` | Uniform scale (0.1–10.0, default 1.0). Example: `2.0` = double size. |
| `/hologram linespacing <id> <spacing>` | Vertical gap between lines in blocks (0.05–3.0, default 0.3). |
| `/hologram shadow <id> on\|off` | Enable/disable the text drop-shadow. |
| `/hologram seethrough <id> on\|off` | Render the text through solid blocks (like a beacon beam). Default off. |
| `/hologram opacity <id> <0-255>` | Text opacity (255 = fully opaque, default; 0 = invisible). |
| `/hologram viewrange <id> <0.1-8.0>` | Display-entity view range multiplier (default `1.0`, vanilla base ≈ 64 blocks, so e.g. `2.0` ≈ 128 blocks). |
| `/hologram linewidth <id> <1-4096>` | Maximum text width in pixels before wrapping (default 200). Set higher to prevent long lines wrapping. |
| `/hologram background <id> <color>` | Background panel colour. Accepts: `transparent` (default), `#RRGGBB`, or `#AARRGGBB` (where AA is alpha). |

**Background examples:**
```
/hologram background lobby transparent         ← fully transparent (default)
/hologram background lobby #40000000           ← 25% black semi-transparent panel
/hologram background lobby #000000             ← opaque solid black
```

---

## Placeholders

Hologram line text supports the full NeoEssentials placeholder syntax `{identifier}`.

Common useful placeholders:

| Placeholder | Value |
|---|---|
| `{neoessentials_online_players}` | Current online player count |
| `{neoessentials_max_players}` | Server max players |
| `{neoessentials_server_name}` | Server name from config |
| `{neoessentials_server_motd}` | Configured MOTD (`/motd` if set, otherwise `server.properties`) |
| `{neoessentials_time}` | Current server time (12h) |
| `{neoessentials_time_24}` | Current server time (24h) |
| `{neoessentials_date}` | Current date |

Set `refreshInterval` > 0 on the hologram to keep placeholder values up-to-date.

### Leaderboard Boards

Any registered [leaderboard](LeaderboardSystem) board's rank/name/value is also available:
`{leaderboard_<board>:<rank>:name}` / `{leaderboard_<board>:<rank>:value}`, e.g.
`{leaderboard_money:1:name}`. Rather than typing each `/hologram addline` by hand,
`/leaderboard hologram create <board> <id> [lines]` generates a full ranked hologram in one
command — see [Leaderboard System → Holograms](LeaderboardSystem#holograms).

---

## Colour Codes

Use `&` colour codes in line text:

| Code | Colour |
|---|---|
| `&a` | Green |
| `&b` | Aqua |
| `&c` | Red |
| `&e` | Yellow |
| `&f` | White |
| `&6` | Gold |
| `&l` | Bold |
| `&o` | Italic |
| `&r` | Reset |

Hex colour via `&#RRGGBB` is also supported.

---

## Example Hologram Setup

```
# Step 1 — create at eye level above a spawn platform
/hologram create spawn-welcome 0 70 0 minecraft:overworld

# Step 2 — add lines (top to bottom)
/hologram addline spawn-welcome &6&lWelcome to &eMyServer!
/hologram addline spawn-welcome &7Online: &f{neoessentials_online_players}/{neoessentials_max_players}
/hologram addline spawn-welcome &7&oRight-click a sign to explore

# Step 3 — refresh every 10 s so player counts stay current
/hologram setrefresh spawn-welcome 10

# Step 4 — increase size slightly and enable gentle hover
/hologram scale spawn-welcome 1.3
/hologram hover spawn-welcome on 0.1 1.2

# Step 5 — inspect the result
/hologram info spawn-welcome
```

---

## holograms.json Format (Advanced)

Fields stored per hologram:

```json
{
  "id": "spawn-welcome",
  "world": "minecraft:overworld",
  "x": 0.0, "y": 70.0, "z": 0.0,
  "visible": true,
  "refreshInterval": 10,
  "interactive": false,
  "scale": 1.3,
  "lineSpacing": 0.3,
  "textShadow": false,
  "textOpacity": 255,
  "backgroundColorArgb": 0,
  "textAlign": 0,
  "seeThrough": false,
  "lineWidth": 200,
  "viewRange": 1.0,
  "billboardMode": 3,
  "spinEnabled": false,
  "spinSpeedDegrees": 3.0,
  "spinAxis": "Y",
  "spinTrackPlayer": true,
  "hoverEnabled": true,
  "hoverAmplitude": 0.1,
  "hoverSpeedDegrees": 1.2,
  "lines": [
    { "text": "&6&lWelcome to &eMyServer!", "frames": [], "animFrameIntervalTicks": 0 },
    { "text": "&7Online: &f{neoessentials_online_players}/{neoessentials_max_players}", "frames": [], "animFrameIntervalTicks": 0 }
  ]
}
```

`billboardMode` values: `0`=FIXED, `1`=VERTICAL, `2`=HORIZONTAL, `3`=CENTER.  
`textAlign` values: `0`=CENTER, `1`=LEFT, `2`=RIGHT.  
`backgroundColorArgb` is an ARGB integer. `0` = fully transparent.  
`textOpacity`: `0`–`255`, `255` = fully opaque.  
`interactive`: when `true`, right/left-clicking the hologram fires click events (used by shop holograms — see below). Regular admin-created holograms leave this `false`.

---

## Shop Holograms

When a sign shop enables its hologram (via the shop hologram toggle — see [Economy System](EconomySystem)), NeoEssentials auto-creates an `interactive` hologram named `shop_<dimension>_<x>_<y>_<z>` above the sign showing the item, owner, and buy/sell prices. This hologram is refreshed automatically on every transaction and behaves as a second click-target for the shop:

| Interaction | Effect |
|---|---|
| Right-click (non-owner) | Buy from the shop (same as right-clicking the sign) — requires `neoessentials.shop.use`. |
| Left-click / attack (non-owner) | Sell to the shop — requires `neoessentials.shop.use`. |
| Right-click (owner) | Shows shop info instead of transacting. |
| **Shift+right-click** (owner), holding an item | (Re)assigns the shop's traded item to the held item, capturing its data (enchantments, custom name, modded NBT) — same gesture as the sign, see [Item Data / NBT](EconomySystem#item-data--nbt-modded-items) in the Economy System page. |

Clicks are rate-limited to one processed interaction per player every 400 ms to prevent held-down attack-spam from firing repeated transactions.  
Shop holograms can be repositioned relative to their sign (up to ±4.5 blocks per axis) via the shop's hologram-offset controls; admin `/hologram` commands can still be used to inspect them (`/hologram info shop_...`), but manual edits to their lines/position are overwritten the next time the shop transacts or reloads.

---

## Web Dashboard API

The dashboard exposes a REST API for hologram management:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/holograms/list` | List all holograms |
| `GET` | `/api/holograms/stats` | Summary counts (total, visible, animated, shop) |
| `GET` | `/api/holograms/{id}` | Get a single hologram |
| `POST` | `/api/holograms/create` | Create (body = hologram JSON) |
| `PUT` | `/api/holograms/{id}` | Update (full hologram JSON body) |
| `DELETE` | `/api/holograms/{id}` | Delete |
| `POST` | `/api/holograms/{id}/spawn` | Force a re-spawn |
| `POST` | `/api/holograms/{id}/despawn` | Despawn without deleting |
| `POST` | `/api/holograms/{id}/visible` | Toggle visibility |

All responses include `billboard`, `spin`, `hover`, `scale`, `lineSpacing`, `textShadow`, `textOpacity`, and `backgroundColorArgb` fields.

