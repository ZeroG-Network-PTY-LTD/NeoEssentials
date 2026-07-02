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

export default function Players({ players }: Props) {
  const [selected, setSelected] = useState<McPlayer | null>(null);

  const heal = (uuid: string) => router.post(route('dashboard.players.heal', uuid));

  const kick = (uuid: string) => {
    const reason = window.prompt('Kick reason:');
    if (reason) router.post(route('dashboard.players.kick', uuid), { reason });
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
                onClick={() => { kick(selected.uuid); setSelected(null); }}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] text-left"
              >
                Kick
              </button>
              <button
                onClick={() => setSelected(null)}
                className="text-[13px] px-3 py-2 rounded-[var(--radius)] border border-[var(--mc-ember-400)] text-[var(--mc-ember-500)] hover:bg-[var(--mc-ember-50)] text-left"
              >
                Ban (opens confirm)
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
