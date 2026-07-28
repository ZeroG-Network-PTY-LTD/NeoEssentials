import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { Home, McPlayer, OfflinePlayer, PermissionGroup, PlayerLookupResult } from '../types';
import type { Gamemode } from '../lib/mcApi';
import { Users, Clock, Search, HeartPulse, MoreHorizontal, Home as HomeIcon, VolumeX, LogOut, ShieldBan, Gamepad2, UserCog } from 'lucide-react';

/**
 * Ported from the external dashboard's Pages/Dashboard/Players.tsx. Inertia's
 * router.get/post()/preserveState partial-reload machinery became plain mcApi calls +
 * useState; the Laravel-side UUID→username resolution (PlayerController::resolveUsername(),
 * needed there because the route only carries a UUID) isn't needed here — this component
 * already holds the full McPlayer objects in memory, so action calls just use p.username
 * directly. Laravel's redirect-carried flash messages became useToast() calls fired from
 * each mutation's own response.
 */

const RANK_STYLE: Record<string, string> = {
  owner: 'bg-[var(--mc-ember-50)] text-[var(--mc-ember-500)]',
  op: 'bg-[var(--mc-ember-50)] text-[var(--mc-ember-500)]',
  mod: 'bg-[var(--mc-cyan-50)] text-[var(--mc-cyan-500)]',
  vip: 'bg-[var(--mc-moss-50)] text-[var(--mc-moss-500)]',
  player: 'bg-[var(--mc-bg-surface-raised)] text-[var(--mc-text-secondary)]',
};

type ActionType = 'kick' | 'ban' | 'mute';

interface PendingAction {
  type: ActionType;
  player: McPlayer;
}

const ACTION_LABEL: Record<ActionType, string> = {
  kick: 'Kick',
  ban: 'Ban',
  mute: 'Mute',
};

export default function Players() {
  const { showToast } = useToast();
  const [players, setPlayers] = useState<McPlayer[]>([]);
  const [offlinePlayers, setOfflinePlayers] = useState<OfflinePlayer[]>([]);
  const [loading, setLoading] = useState(true);

  const [selected, setSelected] = useState<McPlayer | null>(null);
  const [pending, setPending] = useState<PendingAction | null>(null);
  const [reason, setReason] = useState('');
  const [duration, setDuration] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [homesFor, setHomesFor] = useState<McPlayer | null>(null);
  const [homes, setHomes] = useState<Home[] | null>(null);
  const [homesError, setHomesError] = useState<string | null>(null);
  const [lookupInput, setLookupInput] = useState('');
  const [lookupQuery, setLookupQuery] = useState<string | null>(null);
  const [lookupResult, setLookupResult] = useState<PlayerLookupResult | null>(null);
  const [lookupExtra, setLookupExtra] = useState<{ balance?: string; group?: string; live?: McPlayer } | null>(null);

  const [groups, setGroups] = useState<PermissionGroup[]>([]);
  const [currentGroup, setCurrentGroup] = useState<string | null>(null);
  const [groupSaving, setGroupSaving] = useState(false);
  const [gamemodeSaving, setGamemodeSaving] = useState(false);

  const refresh = () => {
    Promise.all([mcApi.players(), mcApi.offlinePlayers()])
      .then(([p, op]) => {
        setPlayers(p);
        setOfflinePlayers(op);
      })
      .finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const runLookup = async (e: FormEvent) => {
    e.preventDefault();
    const q = lookupInput.trim();
    if (!q) return;
    setLookupQuery(q);
    setLookupExtra(null);
    let result: PlayerLookupResult;
    try {
      result = await mcApi.lookupPlayer(q);
    } catch (err) {
      result = { success: false, message: err instanceof Error ? err.message : 'Lookup failed.' };
    }
    setLookupResult(result);
    if (!result.success || !result.username) return;

    const live = players.find((p) => p.uuid === result.uuid);
    const [balanceRes, groupRes] = await Promise.allSettled([
      mcApi.getBalance(result.username),
      mcApi.permissionUserLookup(result.username),
    ]);
    setLookupExtra({
      balance: balanceRes.status === 'fulfilled' ? balanceRes.value.balance : undefined,
      group: groupRes.status === 'fulfilled' && groupRes.value.success ? groupRes.value.group : undefined,
      live,
    });
  };

  const openMore = (player: McPlayer) => {
    setSelected(player);
    setCurrentGroup(null);
    if (groups.length === 0) {
      mcApi.permissionGroups().then(setGroups).catch(() => {});
    }
    mcApi.permissionUserLookup(player.username)
      .then((r) => setCurrentGroup(r.success ? r.group ?? null : null))
      .catch(() => setCurrentGroup(null));
  };

  const changeGroup = async (player: McPlayer, group: string) => {
    setGroupSaving(true);
    try {
      await mcApi.setUserGroup(player.username, group);
      setCurrentGroup(group);
      showToast(`${player.username} is now in group '${group}'.`);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to change group.', true);
    } finally {
      setGroupSaving(false);
    }
  };

  const changeGamemode = async (player: McPlayer, gamemode: Gamemode) => {
    setGamemodeSaving(true);
    try {
      await mcApi.setGamemode(player.username, gamemode);
      showToast(`${player.username}'s game mode is now ${gamemode}.`);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to change game mode.', true);
    } finally {
      setGamemodeSaving(false);
    }
  };

  const heal = async (username: string) => {
    try {
      await mcApi.healPlayer(username);
      showToast(`Healed ${username}.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Heal failed.', true);
    }
  };

  const viewHomes = async (player: McPlayer) => {
    setSelected(null);
    setHomesFor(player);
    setHomes(null);
    setHomesError(null);
    try {
      setHomes(await mcApi.homes(player.username));
    } catch (e) {
      setHomesError(e instanceof Error ? e.message : 'Failed to load homes.');
    }
  };

  const openConfirm = (type: ActionType, player: McPlayer) => {
    setSelected(null);
    setReason('');
    setDuration('');
    setPending({ type, player });
  };

  const closeConfirm = () => {
    if (submitting) return;
    setPending(null);
  };

  const submitConfirm = async () => {
    if (!pending) return;
    if ((pending.type === 'kick' || pending.type === 'ban') && !reason.trim()) return;

    setSubmitting(true);
    try {
      const username = pending.player.username;
      if (pending.type === 'kick') {
        await mcApi.kickPlayer(username, reason.trim());
      } else if (pending.type === 'ban') {
        await mcApi.banPlayer(username, reason.trim(), duration.trim() || undefined);
      } else {
        await mcApi.mutePlayer(username, duration.trim() || undefined);
      }
      showToast(`${ACTION_LABEL[pending.type]}ed ${username}.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : `${ACTION_LABEL[pending.type]} failed.`, true);
    } finally {
      setSubmitting(false);
      setPending(null);
    }
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <PageHeading title="Players" icon={Users} count={players.length} subtitle="Online players and remote moderation actions." />

      <Card title="Online now" icon={Users}>
        <div className="overflow-x-auto">
          <table className="w-full text-[13px]" style={{ tableLayout: 'fixed', minWidth: '640px' }}>
            <thead>
              <tr className="text-left text-[11px] text-[var(--mc-text-muted)] border-b border-[var(--mc-border)]">
                <th className="px-4 py-2.5 font-normal" style={{ width: '26%' }}>Player</th>
                <th className="px-4 py-2.5 font-normal" style={{ width: '14%' }}>Rank</th>
                <th className="px-4 py-2.5 font-normal" style={{ width: '16%' }}>Health</th>
                <th className="px-4 py-2.5 font-normal" style={{ width: '26%' }}>Position</th>
                <th className="px-4 py-2.5 font-normal" style={{ width: '18%' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {players.map((p) => (
                <tr key={p.uuid} className="border-b border-[var(--mc-border)] last:border-0 transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
                  <td className="px-4 py-2.5">
                    <div className="flex items-center gap-2.5">
                      <img
                        src={`https://mc-heads.net/avatar/${p.uuid}/32`}
                        alt=""
                        className="h-6 w-6 rounded-[5px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)]"
                      />
                      {p.username}
                    </div>
                  </td>
                  <td className="px-4 py-2.5">
                    <span className={`text-[11px] px-2 py-0.5 rounded-full ${RANK_STYLE[p.rank]}`}>{p.rank}</span>
                  </td>
                  <td className="px-4 py-2.5 font-data text-[12px]">
                    <span className="inline-flex items-center gap-1.5">
                      <HeartPulse size={12} className="text-[var(--mc-ember-400)]" />
                      {p.health.toFixed(0)}/{p.maxHealth.toFixed(0)}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 font-data text-[12px] text-[var(--mc-text-secondary)]">
                    {p.x.toFixed(0)}, {p.y.toFixed(0)}, {p.z.toFixed(0)} · {p.dimension}
                  </td>
                  <td className="px-4 py-2.5">
                    <div className="flex gap-2">
                      <button
                        onClick={() => heal(p.username)}
                        className="text-[11px] px-2 py-1 rounded-[6px] border border-[var(--mc-border-strong)] hover:border-[var(--mc-cyan-400)] hover:text-[var(--mc-cyan-400)] transition-colors"
                      >
                        Heal
                      </button>
                      <button
                        onClick={() => openMore(p)}
                        className="flex items-center gap-1 text-[11px] px-2 py-1 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface)] transition-colors"
                      >
                        <MoreHorizontal size={12} />
                        More
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {players.length === 0 && (
            <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">Nobody's online right now.</div>
          )}
        </div>
      </Card>

      <div className="mt-6">
        <Card title={`Recently offline (${offlinePlayers.length})`} icon={Clock} accent="purple">
          {offlinePlayers.length === 0 && (
            <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No recently-active offline players on file.</div>
          )}
          {offlinePlayers.map((p) => (
            <div key={p.uuid} className="flex items-center gap-2.5 px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
              <img
                src={`https://mc-heads.net/avatar/${p.uuid}/28`}
                alt=""
                className="h-5 w-5 rounded-[4px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)] grayscale opacity-80"
              />
              <span className="font-medium">{p.username}</span>
              <span className="ml-auto text-[12px] text-[var(--mc-text-muted)]">Last seen {p.lastSeen}</span>
            </div>
          ))}
        </Card>
      </div>

      <div className="mt-6">
        <Card title="Look up a player" icon={Search} accent="cyan" padded>
          <form onSubmit={runLookup} className="flex gap-1.5">
            <input
              value={lookupInput}
              onChange={(e) => setLookupInput(e.target.value)}
              placeholder="Username (online, offline, or never joined)"
              className="flex-1 font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
            />
            <button
              type="submit"
              className="btn-pop text-[13px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] transition-colors"
            >
              Look up
            </button>
          </form>
          {lookupQuery && (
            <div className="mt-3 pt-3 border-t border-[var(--mc-border)] text-[13px]">
              {!lookupResult?.success ? (
                <div className="text-[var(--mc-ember-500)]">{lookupResult?.message ?? `Could not find a player named '${lookupQuery}'.`}</div>
              ) : (
                <div>
                  <div className="flex items-center gap-2.5">
                    <img
                      src={`https://mc-heads.net/avatar/${lookupResult.uuid}/32`}
                      alt=""
                      className="h-6 w-6 rounded-[5px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)]"
                    />
                    <span className="font-medium">{lookupResult.username}</span>
                    <Badge variant={lookupResult.online ? 'moss' : 'neutral'} dot={lookupResult.online}>
                      {lookupResult.online ? 'online' : 'offline'}
                    </Badge>
                    {!lookupResult.online && lookupResult.lastSeen && (
                      <span className="text-[12px] text-[var(--mc-text-muted)]">Last seen {lookupResult.lastSeen}</span>
                    )}
                  </div>

                  <div className="mt-3 pt-3 border-t border-[var(--mc-border)] grid grid-cols-2 gap-x-4 gap-y-2 font-data text-[12px]">
                    <div>
                      <span className="text-[var(--mc-text-muted)]">UUID: </span>
                      <span className="break-all">{lookupResult.uuid}</span>
                    </div>
                    <div>
                      <span className="text-[var(--mc-text-muted)]">Balance: </span>
                      {lookupExtra?.balance !== undefined ? `$${lookupExtra.balance}` : '…'}
                    </div>
                    <div>
                      <span className="text-[var(--mc-text-muted)]">Group: </span>
                      {lookupExtra?.group ?? '…'}
                    </div>
                    {lookupResult.online && lookupExtra?.live && (
                      <>
                        <div>
                          <span className="text-[var(--mc-text-muted)]">Health: </span>
                          {lookupExtra.live.health.toFixed(0)}/{lookupExtra.live.maxHealth.toFixed(0)}
                        </div>
                        <div>
                          <span className="text-[var(--mc-text-muted)]">Position: </span>
                          {lookupExtra.live.x.toFixed(0)}, {lookupExtra.live.y.toFixed(0)}, {lookupExtra.live.z.toFixed(0)} · {lookupExtra.live.dimension}
                        </div>
                        <div>
                          <span className="text-[var(--mc-text-muted)]">Playtime: </span>
                          {lookupExtra.live.playtimeMinutes} min
                        </div>
                      </>
                    )}
                  </div>

                  <Link
                    to={`/lookup?player=${encodeURIComponent(lookupResult.username ?? lookupQuery ?? '')}`}
                    className="mt-3 inline-flex items-center gap-1.5 text-[12px] text-[var(--mc-cyan-400)] hover:underline"
                  >
                    Full profile →
                  </Link>
                </div>
              )}
            </div>
          )}
        </Card>
      </div>

      {selected && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-[2px] flex items-center justify-center z-10" onClick={() => setSelected(null)}>
          <div className="dash-card p-5 w-80" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-2.5 mb-3">
              <img
                src={`https://mc-heads.net/avatar/${selected.uuid}/40`}
                alt=""
                className="h-8 w-8 rounded-[7px] [image-rendering:pixelated] border border-[var(--mc-border-strong)]"
              />
              <div className="font-display text-[15px] font-semibold">{selected.username}</div>
            </div>
            <div className="flex flex-col gap-2">
              <button
                onClick={() => {
                  heal(selected.username);
                  setSelected(null);
                }}
                className="flex items-center gap-2 text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left transition-colors"
              >
                <HeartPulse size={14} className="text-[var(--mc-ember-400)]" />
                Heal and feed
              </button>
              <button
                onClick={() => viewHomes(selected)}
                className="flex items-center gap-2 text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left transition-colors"
              >
                <HomeIcon size={14} className="text-[var(--mc-cyan-400)]" />
                View homes
              </button>
              <button
                onClick={() => openConfirm('mute', selected)}
                className="flex items-center gap-2 text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left transition-colors"
              >
                <VolumeX size={14} className="text-[var(--mc-purple-400)]" />
                Mute
              </button>

              <div className="mt-1 pt-2 border-t border-[var(--mc-border)]">
                <div className="flex items-center gap-2 text-[11px] text-[var(--mc-text-muted)] mb-1.5">
                  <Gamepad2 size={13} />
                  Game mode
                </div>
                <div className="grid grid-cols-2 gap-1.5">
                  {(['survival', 'creative', 'adventure', 'spectator'] as Gamemode[]).map((gm) => (
                    <button
                      key={gm}
                      disabled={gamemodeSaving}
                      onClick={() => changeGamemode(selected, gm)}
                      className="text-[12px] px-2 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] capitalize disabled:opacity-50 transition-colors"
                    >
                      {gm}
                    </button>
                  ))}
                </div>
              </div>

              <div className="mt-1 pt-2 border-t border-[var(--mc-border)]">
                <div className="flex items-center gap-2 text-[11px] text-[var(--mc-text-muted)] mb-1.5">
                  <UserCog size={13} />
                  Permission group
                </div>
                <select
                  value={currentGroup ?? ''}
                  disabled={groupSaving || groups.length === 0}
                  onChange={(e) => changeGroup(selected, e.target.value)}
                  className="w-full text-[13px] px-2.5 py-1.5 rounded-[6px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] outline-none focus:border-[var(--mc-cyan-400)] disabled:opacity-50"
                >
                  <option value="" disabled>
                    {groups.length === 0 ? 'Loading groups…' : currentGroup === null ? 'Unknown' : 'Select group'}
                  </option>
                  {groups.map((g) => (
                    <option key={g.name} value={g.name}>{g.name}</option>
                  ))}
                </select>
              </div>

              <button
                onClick={() => openConfirm('kick', selected)}
                className="flex items-center gap-2 text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left transition-colors"
              >
                <LogOut size={14} className="text-[var(--mc-text-muted)]" />
                Kick
              </button>
              <button
                onClick={() => openConfirm('ban', selected)}
                className="flex items-center gap-2 text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-ember-400)] text-[var(--mc-ember-500)] hover:bg-[var(--mc-ember-50)] text-left transition-colors"
              >
                <ShieldBan size={14} />
                Ban
              </button>
              <Link
                to={`/lookup?player=${encodeURIComponent(selected.username)}`}
                className="mt-1 flex items-center justify-center gap-2 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] text-left transition-colors"
              >
                <UserCog size={14} />
                Full profile
              </Link>
            </div>
          </div>
        </div>
      )}

      {pending && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-[2px] flex items-center justify-center z-20" onClick={closeConfirm}>
          <div className="dash-card p-5 w-96" onClick={(e) => e.stopPropagation()}>
            <div className="font-display text-[15px] font-semibold mb-1">
              {ACTION_LABEL[pending.type]} {pending.player.username}?
            </div>
            <div className="text-[12px] text-[var(--mc-text-muted)] mb-4">
              {pending.type === 'ban'
                ? 'This immediately removes the player and prevents them from rejoining.'
                : pending.type === 'kick'
                ? 'This disconnects the player; they can rejoin immediately.'
                : 'This prevents the player from sending chat messages.'}
            </div>

            {(pending.type === 'kick' || pending.type === 'ban') && (
              <div className="mb-3">
                <label className="block text-[11px] text-[var(--mc-text-muted)] mb-1">Reason (required)</label>
                <input
                  type="text"
                  autoFocus
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  maxLength={255}
                  className="w-full text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] outline-none focus:border-[var(--mc-cyan-400)]"
                  placeholder="e.g. Griefing spawn area"
                />
              </div>
            )}

            {(pending.type === 'ban' || pending.type === 'mute') && (
              <div className="mb-4">
                <label className="block text-[11px] text-[var(--mc-text-muted)] mb-1">
                  Duration (optional — blank = {pending.type === 'ban' ? 'permanent' : 'indefinite'})
                </label>
                <input
                  type="text"
                  value={duration}
                  onChange={(e) => setDuration(e.target.value)}
                  className="w-full text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] outline-none focus:border-[var(--mc-cyan-400)] font-data"
                  placeholder="e.g. 1d, 7d, 30m"
                />
              </div>
            )}

            <div className="flex gap-2 justify-end">
              <button
                onClick={closeConfirm}
                disabled={submitting}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={submitConfirm}
                disabled={submitting || ((pending.type === 'kick' || pending.type === 'ban') && !reason.trim())}
                className="btn-pop text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white hover:bg-[var(--mc-ember-600,var(--mc-ember-500))] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {submitting ? 'Working…' : `Confirm ${ACTION_LABEL[pending.type]}`}
              </button>
            </div>
          </div>
        </div>
      )}

      {homesFor && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-[2px] flex items-center justify-center z-20" onClick={() => setHomesFor(null)}>
          <div className="dash-card p-5 w-96 max-h-[70vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="font-display text-[15px] font-semibold mb-3">{homesFor.username}'s homes</div>
            {homesError && <div className="text-[13px] text-[var(--mc-ember-500)]">{homesError}</div>}
            {!homesError && homes === null && <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>}
            {homes !== null && homes.length === 0 && <div className="text-[13px] text-[var(--mc-text-muted)]">No homes set.</div>}
            {homes && homes.length > 0 && (
              <div className="flex flex-col gap-2">
                {homes.map((h) => (
                  <div key={h.name} className="text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border)] flex items-center gap-2.5">
                    <HomeIcon size={14} className="text-[var(--mc-cyan-400)] shrink-0" />
                    <div>
                      <div className="font-medium mb-0.5">{h.name}</div>
                      <div className="font-data text-[12px] text-[var(--mc-text-muted)]">
                        {h.dimension.replace('minecraft:', '')} · {Math.round(h.x)}, {Math.round(h.y)}, {Math.round(h.z)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
            <button
              onClick={() => setHomesFor(null)}
              className="mt-4 w-full text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
