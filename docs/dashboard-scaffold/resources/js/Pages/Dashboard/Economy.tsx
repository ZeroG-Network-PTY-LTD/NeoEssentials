import { Head, useForm } from '@inertiajs/react';
import DashboardLayout from '@/Layouts/DashboardLayout';
import type { LeaderboardEntry } from '@/types/minecraft';

interface Props {
  leaderboard: LeaderboardEntry[];
}

export default function Economy({ leaderboard }: Props) {
  const { data, setData, post, processing, errors, reset } = useForm({
    uuid: '',
    action: 'give' as 'give' | 'take' | 'set',
    amount: '',
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    post(route('dashboard.economy.adjust'), { onSuccess: () => reset('amount') });
  };

  return (
    <DashboardLayout>
      <Head title="Economy" />
      <h1 className="font-display text-[20px] font-semibold mb-5">Economy</h1>

      <div className="grid grid-cols-[1fr_320px] gap-5">
        <div className="rounded-[var(--radius-lg)] bg-[var(--mc-bg-surface)] border border-[var(--mc-border)] overflow-hidden">
          <div className="px-4 py-3 border-b border-[var(--mc-border)] font-display text-[14px] font-semibold">
            Balance leaderboard
          </div>
          {leaderboard.map((entry, i) => (
            <div
              key={entry.uuid}
              className="flex items-center px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px]"
            >
              <span className="w-6 text-[var(--mc-text-muted)] font-data">#{i + 1}</span>
              <span className="flex-1 ml-2">{entry.username}</span>
              <span className="font-data text-[13px]">${entry.balance.toLocaleString()}</span>
            </div>
          ))}
        </div>

        <form
          onSubmit={submit}
          className="rounded-[var(--radius-lg)] bg-[var(--mc-bg-surface)] border border-[var(--mc-border)] p-4 h-fit flex flex-col gap-3"
        >
          <div className="font-display text-[14px] font-semibold mb-1">Adjust balance</div>

          <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
            Player UUID or username
            <input
              value={data.uuid}
              onChange={(e) => setData('uuid', e.target.value)}
              className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)]"
            />
            {errors.uuid && <span className="text-[var(--mc-ember-500)]">{errors.uuid}</span>}
          </label>

          <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
            Action
            <select
              value={data.action}
              onChange={(e) => setData('action', e.target.value as typeof data.action)}
              className="text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)]"
            >
              <option value="give">Give</option>
              <option value="take">Take</option>
              <option value="set">Set balance to</option>
            </select>
          </label>

          <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
            Amount
            <input
              type="number"
              min="0"
              value={data.amount}
              onChange={(e) => setData('amount', e.target.value)}
              className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)]"
            />
            {errors.amount && <span className="text-[var(--mc-ember-500)]">{errors.amount}</span>}
          </label>

          <button
            type="submit"
            disabled={processing}
            className="mt-1 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-copper-500)] text-[#1a1410] font-medium disabled:opacity-50"
          >
            Apply
          </button>
        </form>
      </div>
    </DashboardLayout>
  );
}
