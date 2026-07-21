export type PlayerRank = 'owner' | 'op' | 'mod' | 'vip' | 'player';

export interface McPlayer {
  uuid: string;
  username: string;
  rank: PlayerRank;
  online: boolean;
  health: number;
  maxHealth: number;
  hunger: number;
  dimension: string;
  x: number;
  y: number;
  z: number;
  playtimeMinutes: number;
  balance: number;
}

/** A player who's played before but isn't online right now — no live stats, just identity. */
export interface OfflinePlayer {
  uuid: string;
  username: string;
  lastSeen: string;
}

export interface PlayerLookupResult {
  success: boolean;
  message?: string;
  uuid?: string;
  username?: string;
  online?: boolean;
  lastSeen?: string;
}

export interface ServerStatus {
  online: boolean;
  tps: number;
  uptimeSeconds: number;
  onlineCount: number;
  maxPlayers: number;
  memoryUsedMb: number;
  memoryMaxMb: number;
}

export interface Warp {
  name: string;
  x: number;
  y: number;
  z: number;
  dimension: string;
  createdBy: string;
}

export interface LeaderboardEntry {
  uuid: string;
  username: string;
  balance: number;
}

export type LogEntryType = 'join' | 'leave' | 'command' | 'chat';

export interface LogEntry {
  timestamp: number;
  type: LogEntryType;
  username: string;
  message: string;
}

/**
 * Matches KitsEndpoint's kitJson exactly — the mod's kit dashboard API is
 * read-only (list/stats/single-view only), no item contents/cost are
 * exposed, and there's no create/update/delete/give route.
 */
export interface Kit {
  name: string;
  displayName: string;
  description: string;
  enabled: boolean;
  permission: string | null;
  cooldownMs: number;
  cooldownDisplay: string;
  maxUses: number;
  itemCount: number;
}

export interface KitStats {
  total: number;
  enabled: number;
  withPermission: number;
  withCooldown: number;
  withUsageLimit: number;
}

/** Mod dashboard account role — distinct from this app's own admin/moderator. */
export type ModUserRole = 'ADMIN' | 'OPERATOR' | 'MODERATOR' | 'VIEWER';

export interface ModUser {
  id: string;
  username: string;
  email?: string;
  role: ModUserRole;
  enabled: boolean;
  createdAt: number;
  lastLoginAt: number;
  [key: string]: unknown;
}

export interface ModUserSession {
  sessionId: string;
  username: string;
  role: ModUserRole;
  ipAddress: string;
  createdAt: number;
  lastAccessAt: number;
}

/** One line of hologram text, with optional per-line animation frames. */
export interface HologramLine {
  lineId: string;
  text: string;
  animFrameIntervalTicks: number;
  frames: string[];
}

/**
 * Matches HologramEndpoint's hologramJson. The dashboard UI only exposes the
 * common placement/visibility/text fields for create/edit — the animation
 * knobs (spin/hover/billboard/per-line frames) are round-tripped but not
 * editable here yet, so existing values survive an edit made through this UI.
 */
export interface Hologram {
  id: string;
  world: string;
  x: number;
  y: number;
  z: number;
  visible: boolean;
  refreshInterval: number;
  lineCount: number;
  scale: number;
  lineSpacing: number;
  textShadow: boolean;
  textOpacity: number;
  backgroundColorArgb: number;
  billboardMode: string;
  spinEnabled: boolean;
  spinSpeedDegrees: number;
  spinAxis: string;
  hoverEnabled: boolean;
  hoverAmplitude: number;
  hoverSpeedDegrees: number;
  lines: HologramLine[];
}

export interface HologramStats {
  total: number;
  visible: number;
  animated: number;
  shopHolograms: number;
}

export interface DiscordAdapterStatus {
  name: string;
  enabled: boolean;
  /** Whether the underlying Discord bot connection is actually up, not just installed. */
  ready: boolean;
}

export interface DiscordStatus {
  anyActive: boolean;
  adapterCount: number;
  eventCount: number;
  adapters: DiscordAdapterStatus[];
}

export interface DiscordEvent {
  type: string;
  actor: string | null;
  target: string | null;
  channel: string | null;
  message: string | null;
  timestamp: number;
}

export interface DiscordAuthConfig {
  enabled: boolean;
  requireLinkedAccount: boolean;
  allowAutoRegistration: boolean;
  defaultRole: ModUserRole;
  /** A Discord companion mod (Simple Discord Link, Mc2Discord, or DCIntegration) is installed and ready. */
  linkAdapterAvailable: boolean;
}

/**
 * Matches PermissionEndpoint's shapes. The mod's internal permission system is
 * only active when `usingExternal` is false (LuckPerms/FTB Ranks otherwise
 * take over) — pages should treat group/user management as read-only when
 * an external adapter is in charge.
 */
export interface PermissionOverview {
  success: boolean;
  totalGroups: number;
  totalUsers: number;
  usingExternal: boolean;
  systemType: string;
}

export interface PermissionGroup {
  name: string;
  prefix: string;
  suffix: string;
  priority: number;
  isDefault: boolean;
  permissionCount: number;
  permissions: string[];
  inherits: string[];
}

/**
 * The "Online users" list only contains currently-connected players — for anyone else, look
 * them up by name via the "manage another player" search box, which resolves offline players
 * too (profile cache / Mojang API fallback on the mod side).
 */
export interface PermissionUser {
  username: string;
  uuid: string;
  online: boolean;
  group: string;
  prefix: string;
  suffix: string;
  permissions?: string[];
}

export interface PermissionUserLookupResult {
  success: boolean;
  message?: string;
  username?: string;
  uuid?: string;
  online?: boolean;
  group?: string;
  prefix?: string;
  suffix?: string;
  permissions?: string[];
}

export interface PermissionNodeInfo {
  node: string;
  description: string;
  defaultValue: boolean;
}

export interface PermissionNodeCategory {
  category: string;
  key: string;
  permissions: PermissionNodeInfo[];
}

export interface BackupSnapshot {
  filename: string;
  name: string;
  created: string;
  sizeBytes: number;
  sizeMb?: string;
}

export interface BackupTarget {
  key: string;
  path: string;
  exists: boolean;
}

export interface BackupStatus {
  count: number;
  totalSizeMb: string;
  totalSizeBytes: number;
  lastBackup: string | null;
  maxSnapshots: number;
  backupDir: string;
  availableTargets: BackupTarget[];
}

export interface CloudProviderStatus {
  configured: boolean;
  connected?: boolean;
  quotaUsedMB?: number;
  quotaTotalMB?: number;
  error?: string;
  uploadPath?: string;
  tokenMasked?: string;
  folderId?: string;
  clientId?: string;
}

export interface CloudStatus {
  providers: {
    dropbox: CloudProviderStatus;
    googleDrive: CloudProviderStatus;
  };
}

export interface CloudFile {
  name: string;
  path?: string;
  id?: string;
  size?: number;
  [key: string]: unknown;
}

export interface CloudConfig {
  dropbox: { configured: boolean; tokenMasked: string; uploadPath: string };
  googleDrive: { configured: boolean; clientId: string; folderId: string; refreshTokenMasked: string };
}

/**
 * The mod only exposes a read-only per-player homes lookup, and only for
 * players who are currently online (it resolves via the live player object,
 * not a stored profile) — there's no create/delete/rename route to wire up.
 */
export interface Home {
  name: string;
  x: number;
  y: number;
  z: number;
  yaw: number;
  pitch: number;
  dimension: string;
  createdBy: string;
  timestamp: number;
}
