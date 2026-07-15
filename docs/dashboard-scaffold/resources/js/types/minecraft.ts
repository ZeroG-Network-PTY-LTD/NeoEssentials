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
