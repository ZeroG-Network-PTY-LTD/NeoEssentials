# Crates

> **Config file:** `config/neoessentials/crates.json`
> **Module toggle:** `modules.cratesEnabled` (config.json)
> **Introduced:** v1.0.6

---

## Overview

Keys, weighted reward pools, and an animated GUI reveal (like the Spigot plugin
ExcellentCrates), openable either virtually (`/crate open`) or by right-clicking a physical
crate block placed in the world.

[Votifier](votifier) can be configured to grant crate keys as a vote reward — the common "vote
crate" server setup — but the two systems work fully independently too.

---

## Keys — virtual balance, not just an item

A player's key count is a **virtual balance** (`CrateKeyManager`, mirrors how the economy
balance works) — the actual source of truth. A physical key **item** is a convenience
representation: giving one (`/crate key give`) and redeeming one (right-clicking a crate block,
or holding it when using `/crate open`) both move the matching virtual balance in lockstep, so
an item duplicated by some other exploit can never grant more opens than were actually paid
for — the balance is always what's checked and decremented, never the item alone.

## Config (`crates.json`)

Crates are primarily authored as config (like `leaderboard.json` boards) — in-game admin
commands are convenience helpers on top, not a full editor:

```jsonc
"crates": {
  "common": {
    "displayName": "&7Common Crate",
    "block": "minecraft:chest",          // cosmetic only
    "animation": "roulette",              // "roulette" | "sequential" | "instant"
    "keyItem": { "item": "minecraft:tripwire_hook", "count": 1 },
    "rewards": [
      { "id": "diamonds", "weight": 30, "item": { "item": "minecraft:diamond", "count": 3 } },
      { "id": "emerald_block", "weight": 5, "item": { "item": "minecraft:emerald_block", "count": 1 },
        "broadcastRare": true, "broadcastMessage": "&6{player} won an Emerald Block!" }
    ]
  }
}
```

`weight` is relative (doesn't need to sum to 100). Reward items support full item data
(enchantments, custom names, etc. — same `{item, count, components}` serialization `kits.json`
already uses). `broadcastRare` + `broadcastMessage` (`{player}` substituted) announces
server-wide when that specific reward is won.

## Opening animations

- **`roulette`** — a CS:GO-style horizontal strip scrolls and decelerates onto the winning
  reward, ~3-4 seconds.
- **`sequential`** — a handful of slots flicker random rewards a few times before settling,
  ~1.5 seconds — much lighter than roulette.
- **`instant`** — no animation, reward granted and announced immediately.

The reward is always resolved **before** the animation starts — the animation only reveals an
already-determined outcome, it never influences it.

## Physical crate blocks

`/crate admin setblock <crate>` while looking directly at a block registers that exact world
position as a live instance of the crate:
- **Right-click** it (with a key, physical or virtual) to open it.
- **Left-click** it to show the reward-odds preview at no cost — same as `/crate preview`.
  Shift+left-click bypasses the preview and falls through as a normal break attempt.

`/crate admin removeblock` unregisters. This is a position-mapping (like a ChestShop sign), not
a new custom block type — any vanilla block works, purely cosmetic.

`setblock` also auto-creates a floating [Hologram](HologramSystem) above the block (default:
the crate's display name + "Right-click to open!"), requires `modules.hologramEnabled`.
It's a completely ordinary hologram — same registry, same `/hologram` commands — just given a
predictable id (`crate_<dimension>_<x>_<y>_<z>`) so you can immediately customize it further:
`/hologram setline crate_minecraft_overworld_100_64_200 2 &6Rare loot inside!`, plus scale,
spin, background color, hover animation, or anything else `/hologram` supports. `removeblock`
removes it again; `/crate admin reload` and `/crate admin delete` sweep away any hologram left
over a block whose crate no longer exists.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/crate list` | `neoessentials.crate.open` | List defined crates |
| `/crate open <crate>` | `neoessentials.crate.open` | Open virtually (consumes a virtual key) |
| `/crate preview <crate>` | `neoessentials.crate.preview` | Show the reward pool + odds, no key cost |
| `/crate keys [player]` | `neoessentials.crate.open` | Show key balances |
| `/crate key give\|take <player> <crate> <amount>` | `neoessentials.crate.admin` | Adjust a player's virtual key balance |
| `/crate admin create <id> <displayName>` | `neoessentials.crate.admin` | Define a new crate |
| `/crate admin delete <crate>` | `neoessentials.crate.admin` | Delete a crate |
| `/crate admin addreward <crate> <weight>` | `neoessentials.crate.admin` | Add your held item as a reward |
| `/crate admin setkey <crate>` | `neoessentials.crate.admin` | Set the crate's key item to your held item |
| `/crate admin setanimation <crate> <type>` | `neoessentials.crate.admin` | `roulette`\|`sequential`\|`instant` |
| `/crate admin setblock` / `removeblock` | `neoessentials.crate.admin` | Physical block placement (see above) |
| `/crate admin reload` | `neoessentials.crate.admin` | Reload `crates.json` |

## Placeholders

`{crate_keys:<crateId>}` — the viewing player's own key balance for that crate.

---

*Back to [Wiki Home](Home) · See also [Votifier](votifier)*
