import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { LeaderboardEntry } from '../types';
import { Coins, Trophy, Sliders } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Economy.tsx — useForm() became plain
 * useState + a try/catch submit; Laravel's redirect-carried flash became useToast(). */

const MEDAL = ['text-[var(--mc-cyan-400)]', 'text-[var(--mc-purple-400)]', 'text-[var(--mc-text-muted)]'];

export default function Economy() {
  const { showToast } = useToast();
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [identifier, setIdentifier] = useState('');
  const [action, setAction] = useState<'give' | 'take' | 'set'>('give');
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const refresh = () => {
    mcApi.economyLeaderboard().then(setLeaderboard).finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!identifier.trim() || !amount.trim()) return;
    setSubmitting(true);
    try {
      await mcApi.economyAdjust(identifier.trim(), action, Number(amount));
      showToast(`Adjusted ${identifier.trim()}'s balance.`);
      setAmount('');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Adjustment failed.', true);
    } finally {
      setSubmitting(false);
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
      <PageHeading title="Economy" icon={Coins} subtitle="Balances, leaderboard, and manual balance adjustments." />

      <div className="grid grid-cols-[1fr_320px] gap-5">
        <Card title="Balance leaderboard" icon={Trophy} accent="cyan">
          {leaderboard.length === 0 && (
            <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No balances tracked yet.</div>
          )}
          {leaderboard.map((entry, i) => (
            <div
              key={entry.uuid}
              className="flex items-center px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
            >
              <span className={`w-6 font-data ${MEDAL[Math.min(i, 2)]}`}>#{i + 1}</span>
              <img
                src={`https://mc-heads.net/avatar/${entry.uuid}/32`}
                alt=""
                className="h-5 w-5 rounded-[4px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)] ml-1"
              />
              <span className="flex-1 ml-2.5">{entry.username}</span>
              <span className="font-data text-[13px] text-[var(--mc-moss-400)]">${entry.balance.toLocaleString()}</span>
            </div>
          ))}
        </Card>

        <Card title="Adjust balance" icon={Sliders} accent="purple" padded className="h-fit">
          <form onSubmit={submit} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Player UUID or username
              <input
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Action
              <select
                value={action}
                onChange={(e) => setAction(e.target.value as typeof action)}
                className="text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
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
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <button
              type="submit"
              disabled={submitting}
              className="btn-pop mt-1 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] transition-colors disabled:opacity-50"
            >
              {submitting ? 'Applying…' : 'Apply'}
            </button>
          </form>
        </Card>
      </div>
    </DashboardLayout>
  );
}
