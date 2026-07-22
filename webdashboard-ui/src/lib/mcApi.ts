import { mcFetch } from './auth';
import type {
  McPlayer,
  OfflinePlayer,
  PlayerLookupResult,
  ServerStatus,
  Home,
  Warp,
  LeaderboardEntry,
  Kit,
  KitStats,
  Hologram,
  HologramStats,
  DiscordStatus,
  DiscordEvent,
  DiscordAuthConfig,
  ModUser,
  ModUserSession,
  ModUserRole,
  BackupSnapshot,
  BackupStatus,
  CloudStatus,
  CloudConfig,
  CloudFile,
  LogEntry,
  PermissionOverview,
  PermissionGroup,
  PermissionUser,
  PermissionUserLookupResult,
  PermissionNodeCategory,
} from '../types';

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

async function putJson<T = Record<string, unknown>>(path: string, body: unknown = {}): Promise<T> {
  const res = await mcFetch(path, { method: 'PUT', body: JSON.stringify(body) });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message ?? data.error ?? `Mod API returned an error (${res.status}).`);
  return data;
}

async function del<T = Record<string, unknown>>(path: string): Promise<T> {
  const res = await mcFetch(path, { method: 'DELETE' });
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

// --- Economy ---------------------------------------------------------------

export async function economyLeaderboard(): Promise<LeaderboardEntry[]> {
  const data = await getJson<{ topPlayers?: { uuid: string; name: string; balance: number | string }[] }>('/api/stats/economy');
  const top = data.topPlayers ?? [];
  return top.map((e) => ({ uuid: e.uuid, username: e.name, balance: Number(e.balance) }));
}

/** $identifier may be a username or a raw UUID — the mod accepts either. */
export async function economyAdjust(identifier: string, action: 'give' | 'take' | 'set', amount: number) {
  return postJson(`/api/economy/${encodeURIComponent(identifier)}`, { action, amount });
}

// --- Warps -------------------------------------------------------------------

export async function warps(): Promise<Warp[]> {
  const data = await getJson<{ warps?: { name: string; x: number; y: number; z: number; world?: string; createdBy?: string }[] }>('/api/warps');
  return (data.warps ?? []).map((w) => ({
    name: w.name,
    x: w.x,
    y: w.y,
    z: w.z,
    dimension: w.world ?? 'minecraft:overworld',
    createdBy: w.createdBy ?? 'Unknown',
  }));
}

export async function createWarp(name: string, location: { world: string; x: number; y: number; z: number }) {
  return postJson('/api/warps', { name, ...location });
}

export async function deleteWarp(name: string) {
  return del(`/api/warps/${encodeURIComponent(name)}`);
}

// --- Kits (read-only — the mod has no create/update/delete routes for kits) --

export async function kits(): Promise<Kit[]> {
  const data = await getJson<{ kits?: Kit[] }>('/api/kits/list');
  return data.kits ?? [];
}

export async function kitStats(): Promise<KitStats> {
  return getJson('/api/kits/stats');
}

// --- Holograms (full CRUD, no admin gate on the mod side) -------------------

export async function holograms(): Promise<Hologram[]> {
  const data = await getJson<{ holograms?: Hologram[] }>('/api/holograms/list');
  return data.holograms ?? [];
}

export async function hologramStats(): Promise<HologramStats> {
  return getJson('/api/holograms/stats');
}

export async function createHologram(hologram: Record<string, unknown>) {
  return postJson('/api/holograms/create', hologram);
}

export async function updateHologram(id: string, hologram: Record<string, unknown>) {
  return putJson(`/api/holograms/${encodeURIComponent(id)}`, hologram);
}

export async function deleteHologram(id: string) {
  return del(`/api/holograms/${encodeURIComponent(id)}`);
}

export async function toggleHologramVisibility(id: string) {
  return postJson(`/api/holograms/${encodeURIComponent(id)}/visible`, {});
}

// --- Discord (status/events readable by any logged-in account; clearing
// events, sending a test message, and auth-config are admin-only) -----------

export async function discordStatus(): Promise<DiscordStatus> {
  return getJson('/api/discord/status');
}

export async function discordEvents(limit = 50): Promise<DiscordEvent[]> {
  const data = await getJson<{ events?: DiscordEvent[] }>(`/api/discord/events?limit=${limit}`);
  return data.events ?? [];
}

export async function clearDiscordEvents() {
  return del('/api/discord/events');
}

export async function sendDiscordTestMessage(channel: string, message: string) {
  return postJson('/api/discord/test', { channel, message });
}

export async function discordAuthConfig(): Promise<DiscordAuthConfig> {
  return getJson('/api/discord/auth-config');
}

export async function updateDiscordAuthConfig(config: Partial<DiscordAuthConfig>) {
  return postJson('/api/discord/auth-config', config);
}

// --- Mod dashboard accounts (UserManagementEndpoint — entirely admin-only on
// the mod side; distinct from any accounts the internal dashboard's own
// AuthenticationManager already handles for login) ---------------------------

export async function modUsers(): Promise<ModUser[]> {
  const data = await getJson<{ users?: ModUser[] }>('/api/users/list');
  return data.users ?? [];
}

export async function modUserSessions(): Promise<ModUserSession[]> {
  const data = await getJson<{ sessions?: ModUserSession[] }>('/api/users/sessions');
  return data.sessions ?? [];
}

export async function createModUser(username: string, password: string, email: string, role: ModUserRole) {
  return postJson('/api/users/create', { username, password, email, role });
}

export async function setModUserRole(id: string, role: ModUserRole) {
  return postJson(`/api/users/${encodeURIComponent(id)}/role`, { role });
}

/** Omit password to have the mod generate and return a temp one. */
export async function setModUserPassword(id: string, password = '') {
  return postJson(`/api/users/${encodeURIComponent(id)}/password`, { password });
}

export async function enableModUser(id: string) {
  return postJson(`/api/users/${encodeURIComponent(id)}/enable`, {});
}

export async function disableModUser(id: string) {
  return postJson(`/api/users/${encodeURIComponent(id)}/disable`, {});
}

export async function deleteModUser(id: string) {
  return del(`/api/users/${encodeURIComponent(id)}`);
}

export async function revokeModUserSession(sessionId: string) {
  return del(`/api/users/sessions/${encodeURIComponent(sessionId)}`);
}

// --- Backups (status/list readable by any logged-in account; create/restore/
// delete/download are admin-only) --------------------------------------------

export async function backupStatus(): Promise<BackupStatus> {
  return getJson('/api/backup/status');
}

export async function backupList(): Promise<BackupSnapshot[]> {
  const data = await getJson<BackupSnapshot[] | { snapshots?: BackupSnapshot[] }>('/api/backup/list');
  // The mod returns a bare JSON array for this endpoint, not an object.
  return Array.isArray(data) ? data : (data.snapshots ?? []);
}

export async function createBackup(name: string, targets: string[]) {
  return postJson('/api/backup/create', { name, targets });
}

export async function restoreBackup(name: string) {
  return postJson('/api/backup/restore', { name });
}

export async function deleteBackup(name: string) {
  return del(`/api/backup/delete?name=${encodeURIComponent(name)}`);
}

/**
 * Streams the backup ZIP and triggers a browser download — plain `<a href>` can't attach the
 * Bearer token this route needs, so this fetches the blob via mcFetch() and clicks a
 * throwaway object-URL anchor instead.
 */
export async function downloadBackup(name: string): Promise<void> {
  const res = await mcFetch(`/api/backup/download?name=${encodeURIComponent(name)}`);
  if (!res.ok) throw new Error(`Download failed (${res.status}).`);
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name.endsWith('.zip') ? name : `${name}.zip`;
  a.click();
  URL.revokeObjectURL(url);
}

// --- Cloud storage (status/config/file-listing readable by any logged-in
// account; everything else is admin-only) ------------------------------------

export async function cloudStatus(): Promise<CloudStatus> {
  return getJson('/api/cloud/status');
}

export async function cloudConfig(): Promise<CloudConfig> {
  return getJson('/api/cloud/config');
}

export async function configureDropbox(accessToken: string, uploadPath: string) {
  return postJson('/api/cloud/config/dropbox', { accessToken, uploadPath });
}

export async function configureGoogleDrive(refreshToken: string, clientId: string, clientSecret: string, folderId: string) {
  return postJson('/api/cloud/config/google', { refreshToken, clientId, clientSecret, folderId });
}

export async function testDropbox() {
  return postJson('/api/cloud/test/dropbox', {});
}

export async function testGoogleDrive() {
  return postJson('/api/cloud/test/google', {});
}

export async function cloudDropboxFiles(): Promise<CloudFile[]> {
  const data = await getJson<{ files?: CloudFile[] }>('/api/cloud/files/dropbox');
  return data.files ?? [];
}

export async function cloudGoogleFiles(): Promise<CloudFile[]> {
  const data = await getJson<{ files?: CloudFile[] }>('/api/cloud/files/google');
  return data.files ?? [];
}

export async function uploadBackupToDropbox(backupName: string) {
  return postJson(`/api/cloud/upload/dropbox/${encodeURIComponent(backupName)}`, {});
}

export async function uploadBackupToGoogleDrive(backupName: string) {
  return postJson(`/api/cloud/upload/google/${encodeURIComponent(backupName)}`, {});
}

export async function deleteDropboxFile(filePath: string) {
  return del(`/api/cloud/files/dropbox/${encodeURIComponent(filePath)}`);
}

export async function deleteGoogleDriveFile(fileId: string) {
  return del(`/api/cloud/files/google/${encodeURIComponent(fileId)}`);
}

// --- Commands / logs ---------------------------------------------------------

export async function runCommand(command: string) {
  return postJson('/api/commands/execute', { command });
}

/** Recent join/leave/chat/command activity, shaped to match LogEntry[]. */
export async function logs(): Promise<LogEntry[]> {
  const data = await getJson<{ events?: { type: string; message?: string; timestamp: number }[] }>('/api/game/events');
  const events = data.events ?? [];

  // Types with a LogEntry equivalent — anything else (e.g. block.break) has no matching
  // LogEntryType and is dropped here, same as MinecraftApiService.php's logs().
  const typeMap: Record<string, LogEntry['type']> = {
    'player.join': 'join',
    'player.leave': 'leave',
    'player.chat': 'chat',
    'player.command': 'command',
  };

  const entries: LogEntry[] = [];
  for (const e of events) {
    const type = typeMap[e.type];
    if (!type) continue;

    const message = e.message ?? '';
    // Every message the mod generates for these types starts with the player's name (e.g.
    // "Steve joined the game", "Steve: hi", "Steve ran: /tp").
    const username = message.includes(' ') ? message.slice(0, message.indexOf(' ')) : message;

    entries.push({
      timestamp: Math.round((e.timestamp ?? 0) / 1000),
      type,
      username: username || '',
      message,
    });
  }

  return entries;
}

// --- Permissions (GET is open to any logged-in account; every write requires
// ADMIN on the mod side) ------------------------------------------------------

export async function permissionOverview(): Promise<PermissionOverview> {
  return getJson('/api/permissions/overview');
}

export async function permissionGroups(): Promise<PermissionGroup[]> {
  const data = await getJson<{ groups?: PermissionGroup[] }>('/api/permissions/groups');
  return data.groups ?? [];
}

export async function permissionUsers(): Promise<PermissionUser[]> {
  const data = await getJson<{ users?: PermissionUser[] }>('/api/permissions/users');
  return data.users ?? [];
}

export async function permissionUserLookup(username: string): Promise<PermissionUserLookupResult> {
  return getJson(`/api/permissions/user/${encodeURIComponent(username)}`);
}

export async function permissionAliases(): Promise<Record<string, string>> {
  const data = await getJson<{ aliases?: Record<string, string> }>('/api/permissions/aliases');
  return data.aliases ?? {};
}

export async function permissionNodeCatalog(): Promise<PermissionNodeCategory[]> {
  const data = await getJson<{ categories?: PermissionNodeCategory[] }>('/api/permissions/permissions/all');
  return data.categories ?? [];
}

export async function reloadPermissions() {
  return postJson('/api/permissions/reload', {});
}

export async function createPermissionGroup(
  name: string,
  prefix = '',
  suffix = '',
  isDefault = false,
  priority?: number,
  inherits: string[] = [],
) {
  return postJson('/api/permissions/group/create', { name, prefix, suffix, isDefault, priority, inherits });
}

/** $data may include prefix/suffix/priority/inherits (full replace)/isDefault. */
export async function updatePermissionGroup(name: string, data: Record<string, unknown>) {
  return putJson(`/api/permissions/group/${encodeURIComponent(name)}/update`, data);
}

export async function renamePermissionGroup(name: string, newName: string) {
  return postJson(`/api/permissions/group/${encodeURIComponent(name)}/rename`, { newName });
}

export async function deletePermissionGroup(name: string) {
  return del(`/api/permissions/group/${encodeURIComponent(name)}`);
}

export async function addGroupPermission(group: string, permission: string) {
  return postJson(`/api/permissions/group/${encodeURIComponent(group)}/permission/add`, { permission });
}

export async function removeGroupPermission(group: string, permission: string) {
  return del(`/api/permissions/group/${encodeURIComponent(group)}/permission/remove/${encodeURIComponent(permission)}`);
}

export async function setUserGroup(username: string, group: string) {
  return postJson(`/api/permissions/user/${encodeURIComponent(username)}/group/set`, { group });
}

export async function addUserPermission(username: string, permission: string) {
  return postJson(`/api/permissions/user/${encodeURIComponent(username)}/permission/add`, { permission });
}

export async function removeUserPermission(username: string, permission: string) {
  return del(`/api/permissions/user/${encodeURIComponent(username)}/permission/remove/${encodeURIComponent(permission)}`);
}

export async function addPermissionAlias(alias: string, canonical: string) {
  return postJson('/api/permissions/aliases', { alias, canonical });
}

export async function removePermissionAlias(alias: string) {
  return del(`/api/permissions/aliases/${encodeURIComponent(alias)}`);
}

// --- Public moderation lookup (no session required on either side — the mod's
// /api/public/moderation/* routes are registered without the Bearer-token
// check, so these deliberately use plain fetch(), not mcFetch()/the session
// machinery above) ------------------------------------------------------------

export async function publicLookup<T = Record<string, unknown>>(username: string): Promise<T> {
  const res = await fetch(`/api/public/moderation/lookup/${encodeURIComponent(username)}`);
  if (!res.ok) throw new Error(`Lookup failed (${res.status}).`);
  return res.json();
}

export async function publicRecent<T = Record<string, unknown>>(): Promise<T[]> {
  const res = await fetch('/api/public/moderation/recent');
  if (!res.ok) throw new Error(`Request failed (${res.status}).`);
  const data = await res.json();
  return data.recent ?? [];
}
