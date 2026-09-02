# Votifier & Crates

> **Config files:** `config/neoessentials/votifier.json`, `config/neoessentials/crates.json`  
> **Module toggles:** `modules.votifierEnabled`, `modules.cratesEnabled` (config.json)  
> **Introduced:** v1.0.6

---

## Overview

Two systems, commonly used together on real servers:

- **Votifier** — a vote-listener compatible with both Votifier protocol versions (V1/RSA and
  V2/NuVotifier), so vote-site relay services (PlanetMinecraft, minecraftservers.org, etc.) can
  notify this server when a player votes. Runs on its **own TCP port**, separate from the
  Minecraft server port.
- **Crates** — keys, weighted reward pools, and an animated GUI reveal (like the Spigot plugin
  ExcellentCrates), openable either virtually (`/crate open`) or by right-clicking a physical
  crate block placed in the world.

Votifier can be configured to grant crate keys as a vote reward — the common "vote crate"
server setup — but the two systems work fully independently too.

---

## Votifier

### Setup

1. Start the server once with `modules.votifierEnabled: true` (default). The console logs:
   - A **V1 public key** (base64) — paste this into your vote site's panel wherever it asks
     for a Votifier public key.
   - A **V2 token** — paste this wherever the vote site asks for a "NuVotifier token"/"Votifier
     v2 token". This is auto-generated once and saved to `votifier.json` — don't regenerate it
     unless you also update every vote site using it.
2. Point your vote site(s) at this server's IP and the configured port (default `8192`) —
   **not** the Minecraft server port.
3. Configure a reward under `votifier.sites` for each vote site's exact `serviceName` (the vote
   site's panel usually shows you this string), or rely on the `"default"` entry for any site
   without a specific match.

### Protocol

Both wire protocols are auto-detected on the same port per connection (byte-verified against
the reference NeoForge implementation, `github.com/uberswe/votifier`, rather than reconstructed
from memory):

- **V1** — a 256-byte RSA/PKCS1-encrypted block, no framing. Decrypted plaintext is 5
  newline-separated fields: `VOTE`, serviceName, username, address, timestamp.
- **V2 (NuVotifier-compatible)** — magic bytes `0x73 0x3A`, a 2-byte big-endian length prefix,
  then that many bytes of `{"payload": "<json>", "signature": "<base64 HMAC-SHA256>"}`. The
  signature is HMAC-SHA256 over the raw payload string, keyed by the shared V2 token. The inner
  payload carries `serviceName`/`username`/`address`/`timestamp`/`challenge` (the challenge
  must match this connection's greeting — replay protection).

### Config (`votifier.json`)

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch |
| `host` / `port` | `0.0.0.0` / `8192` | Bind address/port for the vote-listener socket |
| `v2Token` | *(auto-generated)* | Shared secret for V2 signature verification |
| `sites.<serviceName>.commands` | *(list)* | Console commands run on vote, `{player}` substituted |
| `sites.<serviceName>.keys` | *(map)* | Crate id → key amount granted on vote (requires `modules.cratesEnabled`) |
| `voteLinks` | *(map)* | Shown by `/vote` — plain links/instructions, not functional |
| `broadcastMessage` | *(text)* | Server-wide "X voted!" message, `{player}`/`{site}`. Empty = no broadcast |
| `voteParty.enabled` | `true` | Cumulative server-wide vote counter with a bonus at a threshold |
| `voteParty.votesRequired` | `50` | Total votes (server-wide) needed to trigger |
| `voteParty.resetOnRestart` | `false` | Whether the counter resets every restart |

A vote from an offline player is **queued**, not dropped — delivered automatically the next
time that player logs in.

### Commands

| Command | Permission | Description |
|---|---|---|
| `/vote` | `neoessentials.votifier.vote` | Show configured vote links |
| `/votes [player]` | `neoessentials.votifier.vote` | Show total vote count |
| `/togglevotebroadcast` | none | Opt out of seeing the vote broadcast |
| `/voteparty` | `neoessentials.votifier.vote` | Show vote party progress |
| `/votifier reload` | `neoessentials.votifier.admin` | Reload `votifier.json` |
| `/votifier testvote <site> [player]` | `neoessentials.votifier.admin` | Simulate a vote — verify reward config without a real vote site round-trip |
| `/votifier genkeys` | `neoessentials.votifier.admin` | Instructions to regenerate the V1 RSA keypair |

### Placeholders

`{votifier_total}`, `{votifier_voteparty_progress}`, `{votifier_voteparty_required}`.

---

## Crates

### Keys — virtual balance, not just an item

A player's key count is a **virtual balance** (`CrateKeyManager`, mirrors how the economy
balance works) — the actual source of truth. A physical key **item** is a convenience
representation: giving one (`/crate key give`) and redeeming one (right-clicking a crate block,
or holding it when using `/crate open`) both move the matching virtual balance in lockstep, so
an item duplicated by some other exploit can never grant more opens than were actually paid
for — the balance is always what's checked and decremented, never the item alone.

### Config (`crates.json`)

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

### Opening animations

- **`roulette`** — a CS:GO-style horizontal strip scrolls and decelerates onto the winning
  reward, ~3-4 seconds.
- **`sequential`** — a handful of slots flicker random rewards a few times before settling,
  ~1.5 seconds — much lighter than roulette.
- **`instant`** — no animation, reward granted and announced immediately.

The reward is always resolved **before** the animation starts — the animation only reveals an
already-determined outcome, it never influences it.

### Physical crate blocks

`/crate admin setblock <crate>` while looking directly at a block registers that exact world
position as a live instance of the crate — right-clicking it (with a key, physical or virtual)
opens it. `/crate admin removeblock` unregisters. This is a position-mapping (like a
ChestShop sign), not a new custom block type — any vanilla block works, purely cosmetic.

`setblock` also auto-creates a floating [Hologram](HologramSystem) above the block (default:
the crate's display name + "Right-click to open!"), requires `modules.hologramEnabled`.
It's a completely ordinary hologram — same registry, same `/hologram` commands — just given a
predictable id (`crate_<dimension>_<x>_<y>_<z>`) so you can immediately customize it further:
`/hologram setline crate_minecraft_overworld_100_64_200 2 &6Rare loot inside!`, plus scale,
spin, background color, hover animation, or anything else `/hologram` supports. `removeblock`
removes it again; `/crate admin reload` and `/crate admin delete` sweep away any hologram left
over a block whose crate no longer exists.

### Commands

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

### Placeholders

`{crate_keys:<crateId>}` — the viewing player's own key balance for that crate.

---

*Back to [Wiki Home](Home)*
