# Votifier

> **Config file:** `config/neoessentials/votifier.json`
> **Module toggle:** `modules.votifierEnabled` (config.json)
> **Introduced:** v1.0.6

---

## Overview

A vote-listener compatible with both Votifier protocol versions (V1/RSA and V2/NuVotifier), so
vote-site relay services (PlanetMinecraft, minecraftservers.org, etc.) can notify this server
when a player votes. Runs on its **own TCP port**, separate from the Minecraft server port.

Votifier can be configured to grant [Crate](neoecrates) keys as a vote reward — the common
"vote crate" server setup — but the two systems work fully independently too.

---

## Setup

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

## Protocol

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

## Config (`votifier.json`)

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

## Commands

| Command | Permission | Description |
|---|---|---|
| `/vote` | `neoessentials.votifier.vote` | Show configured vote links |
| `/votes [player]` | `neoessentials.votifier.vote` | Show total vote count |
| `/togglevotebroadcast` | none | Opt out of seeing the vote broadcast |
| `/voteparty` | `neoessentials.votifier.vote` | Show vote party progress |
| `/votifier reload` | `neoessentials.votifier.admin` | Reload `votifier.json` |
| `/votifier testvote <site> [player]` | `neoessentials.votifier.admin` | Simulate a vote — verify reward config without a real vote site round-trip |
| `/votifier genkeys` | `neoessentials.votifier.admin` | Instructions to regenerate the V1 RSA keypair |

## Placeholders

`{votifier_total}`, `{votifier_voteparty_progress}`, `{votifier_voteparty_required}`.

---

*Back to [Wiki Home](Home) · See also [Crates](neoecrates)*
