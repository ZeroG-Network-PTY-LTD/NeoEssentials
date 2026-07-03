import { Head, router } from '@inertiajs/react';
import { useState } from 'react';
import DashboardLayout from '@/Layouts/DashboardLayout';
import type { McPlayer } from '@/types/minecraft';

interface Props {
  players: McPlayer[];
}

const RANK_STYLE: Record<string, string> = {
  owner: 'bg-[var(--mc-ember-50)] text-[var(--mc-ember-500)]',
  op: 'bg-[var(--mc-ember-50)] text-[var(--mc-ember-500)]',
  mod: 'bg-[var(--mc-copper-50)] text-[var(--mc-copper-500)]',
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

export default function Players({ players }: Props) {
  const [selected, setSelected] = useState<McPlayer | null>(null);
  const [pending, setPending] = useState<PendingAction | null>(null);
  const [reason, setReason] = useState('');
  const [duration, setDuration] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const heal = (uuid: string) => router.post(route('dashboard.players.heal', uuid));

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

  const submitConfirm = () => {
    if (!pending) return;
    if ((pending.type === 'kick' || pending.type === 'ban') && !reason.trim()) return;

    const routeName = `dashboard.players.${pending.type}` as const;
    const payload: Record<string, string> =
      pending.type === 'kick'
        ? { reason: reason.trim() }
        : pending.type === 'ban'
        ? { reason: reason.trim(), ...(duration.trim() ? { duration: duration.trim() } : {}) }
        : { ...(duration.trim() ? { duration: duration.trim() } : {}) };

    setSubmitting(true);
    router.post(route(routeName, pending.player.uuid), payload, {
      preserveScroll: true,
      onFinish: () => {
        setSubmitting(false);
        setPending(null);
      },
    });
  };

  return (
    <DashboardLayout>
      <Head title="Players" />
      <h1 className="font-display text-[20px] font-semibold mb-5">
        Players <span className="text-[var(--mc-text-muted)] font-data text-[16px]">({players.length})</span>
      </h1>

      <div className="rounded-[var(--radius-lg)] bg-[var(--mc-bg-surface)] border border-[var(--mc-border)] overflow-hidden">
        <table className="w-full text-[13px]" style={{ tableLayout: 'fixed' }}>
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
              <tr key={p.uuid} className="border-b border-[var(--mc-border)] last:border-0">
                <td className="px-4 py-2.5">{p.username}</td>
                <td className="px-4 py-2.5">
                  <span className={`text-[11px] px-2 py-0.5 rounded-[6px] ${RANK_STYLE[p.rank]}`}>
                    {p.rank}
                  </span>
                </td>
                <td className="px-4 py-2.5 font-data text-[12px]">
                  {p.health.toFixed(0)}/{p.maxHealth.toFixed(0)}
                </td>
                <td className="px-4 py-2.5 font-data text-[12px] text-[var(--mc-text-secondary)]">
                  {p.x.toFixed(0)}, {p.y.toFixed(0)}, {p.z.toFixed(0)} · {p.dimension}
                </td>
                <td className="px-4 py-2.5">
                  <div className="flex gap-2">
                    <button
                      onClick={() => heal(p.uuid)}
                      className="text-[11px] px-2 py-1 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)]"
                    >
                      Heal
                    </button>
                    <button
                      onClick={() => setSelected(p)}
                      className="text-[11px] px-2 py-1 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)]"
                    >
                      More
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Action menu — pick which action to take on this player */}
      {selected && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-10"
          onClick={() => setSelected(null)}
        >
          <div
            className="bg-[var(--mc-bg-surface)] border border-[var(--mc-border)] rounded-[var(--radius-lg)] p-5 w-80"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="font-display text-[15px] font-semibold mb-3">{selected.username}</div>
            <div className="flex flex-col gap-2">
              <button
                onClick={() => { heal(selected.uuid); setSelected(null); }}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left"
              >
                Heal and feed
              </button>
              <button
                onClick={() => openConfirm('mute', selected)}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left"
              >
                Mute
              </button>
              <button
                onClick={() => openConfirm('kick', selected)}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left"
              >
                Kick
              </button>
              <button
                onClick={() => openConfirm('ban', selected)}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-ember-400)] text-[var(--mc-ember-500)] hover:bg-[var(--mc-ember-50)] text-left"
              >
                Ban
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm dialog — collects reason/duration, then submits the action */}
      {pending && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-20"
          onClick={closeConfirm}
        >
          <div
            className="bg-[var(--mc-bg-surface)] border border-[var(--mc-border)] rounded-[var(--radius-lg)] p-5 w-96"
            onClick={(e) => e.stopPropagation()}
          >
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
                <label className="block text-[11px] text-[var(--mc-text-muted)] mb-1">
                  Reason (required)
                </label>
                <input
                  type="text"
                  autoFocus
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  maxLength={255}
                  className="w-full text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] outline-none focus:border-[var(--mc-copper-400)]"
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
                  className="w-full text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] outline-none focus:border-[var(--mc-copper-400)] font-data"
                  placeholder="e.g. 1d, 7d, 30m"
                />
              </div>
            )}

            <div className="flex gap-2 justify-end">
              <button
                onClick={closeConfirm}
                disabled={submitting}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={submitConfirm}
                disabled={submitting || ((pending.type === 'kick' || pending.type === 'ban') && !reason.trim())}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white hover:bg-[var(--mc-ember-600,var(--mc-ember-500))] disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? 'Working…' : `Confirm ${ACTION_LABEL[pending.type]}`}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
