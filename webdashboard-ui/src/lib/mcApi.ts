import { mcFetch } from './auth';
import type { McPlayer, OfflinePlayer, PlayerLookupResult, ServerStatus, Home } from '../types';

/**
 * The mod's own REST API contract, anti-corruption layer for the internal dashboard —
 * direct TypeScript analog of the external Laravel app's MinecraftApiService.php (renames
 * mod fields, merges endpoints, fabricates `rank`), called straight against /api/* with no
 * PHP process in between. Ground-truthed against the actual Java source, not just
 * docs/API.md, which turned out to have a stale shape for /api/player/online (documents a
 * nested {online:{...},offline:{...}} shape; the real endpoint — PlayerDataCollector.
 * getOnlinePlayers() — returns flat top-level "players"/"offlinePlayers" keys, matching what
 * MinecraftApiService.php actually reads).
 */

async function getJson<T = Record<string, unknown>>(path: string): Promise<T> {
  const res = await mcFetch(path);
  if (!res.ok) throw new Error(`Mod API returned an error (${res.status}).`);
  return res.json();
}

async function postJson<T = Record<string, unknown>>(path: string, body: unknown = {}): Promise<T> {
  const res = await mcFetch(path, { method: 'POST', body: JSON.stringify(body) });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message ?? data.error ?? `Mod API returned an error (${res.status}).`);
  return data;
}

export async function status(): Promise<Partial<ServerStatus>> {
  const [s, perf] = await Promise.all([
    getJson<Record<string, unknown>>('/api/server/status'),
    getJson<Record<string, unknown>>('/api/stats/performance'),
  ]);

  return {
    online: Boolean(s.online),
    // /api/server/status's own "tps" is a formatted string (DecimalFormat), not a number.
    tps: parseFloat(String(s.tps ?? 0)),
    uptimeSeconds: Math.round(Number(s.uptimeMillis ?? 0) / 1000),
    onlineCount: Number(s.playersOnline ?? 0),
    maxPlayers: Number(s.playersMax ?? 0),
    memoryUsedMb: Number(perf.memUsedMb ?? 0),
    memoryMaxMb: Number(perf.memMaxMb ?? 0),
  };
}

interface RawPlayer {
  uuid: string;
  username: string;
  operator?: boolean;
  health?: number;
  maxHealth?: number;
  foodLevel?: number;
  dimension?: string;
  x?: number;
  y?: number;
  z?: number;
}

async function rawPlayerData(): Promise<{ players: RawPlayer[]; offlinePlayers: { uuid: string; username: string; lastSeen?: string }[] }> {
  return getJson('/api/player/online');
}

export async function players(): Promise<McPlayer[]> {
  const { players: online } = await rawPlayerData();
  return online.map((p) => ({
    uuid: p.uuid,
    username: p.username,
    // The mod doesn't expose a bulk "rank" concept cheaply — approximated from operator
    // status here, same fabrication MinecraftApiService.php's players() does. Real per-player
    // rank requires a separate /api/permissions/user/{name} call.
    rank: p.operator ? 'op' : 'player',
    online: true,
    health: p.health ?? 20,
    maxHealth: p.maxHealth ?? 20,
    hunger: p.foodLevel ?? 20,
    dimension: p.dimension ?? 'minecraft:overworld',
    x: p.x ?? 0,
    y: p.y ?? 0,
    z: p.z ?? 0,
    playtimeMinutes: 0,
    balance: 0,
  }));
}

export async function offlinePlayers(): Promise<OfflinePlayer[]> {
  const { offlinePlayers: offline } = await rawPlayerData();
  return offline.map((p) => ({ uuid: p.uuid, username: p.username, lastSeen: p.lastSeen ?? 'Unknown' }));
}

export async function lookupPlayer(username: string): Promise<PlayerLookupResult> {
  return getJson(`/api/player/lookup/${encodeURIComponent(username)}`);
}

export async function homes(username: string): Promise<Home[]> {
  const data = await getJson<{ homes?: Home[] }>(`/api/player/homes/${encodeURIComponent(username)}`);
  return data.homes ?? [];
}

export async function healPlayer(username: string) {
  return postJson(`/api/player/heal/${encodeURIComponent(username)}`, {});
}

export async function kickPlayer(username: string, reason: string) {
  return postJson(`/api/player/kick/${encodeURIComponent(username)}`, { reason });
}

export async function banPlayer(username: string, reason: string, duration?: string) {
  return postJson('/api/moderation/ban', {
    target: username,
    playerName: username,
    reason,
    type: 'NAME',
    duration: duration ? Number(duration) : -1,
  });
}

export async function mutePlayer(username: string, duration?: string) {
  return postJson('/api/moderation/mute', {
    targetName: username,
    duration: duration ? Number(duration) : null,
  });
}
