import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { EconomyStats, PlayerLookupResult } from '../types';
import { Coins, Trophy, Sliders, Wallet, Users, TrendingUp, Search, UserCog } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Economy.tsx — useForm() became plain
 * useState + a try/catch submit; Laravel's redirect-carried flash became useToast(). */

const MEDAL = ['text-[var(--mc-cyan-400)]', 'text-[var(--mc-purple-400)]', 'text-[var(--mc-text-muted)]'];

export default function Economy() {
  const { showToast } = useToast();
  const [stats, setStats] = useState<EconomyStats | null>(null);
  const [loading, setLoading] = useState(true);

  const [identifier, setIdentifier] = useState('');
  const [action, setAction] = useState<'give' | 'take' | 'set'>('give');
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [lookupResult, setLookupResult] = useState<PlayerLookupResult | null>(null);
  const [lookupBalance, setLookupBalance] = useState<string | null>(null);
  const [lookingUp, setLookingUp] = useState(false);

  const refresh = () => {
    mcApi.economyStats().then(setStats).finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const runLookup = async () => {
    const name = identifier.trim();
    if (!name) return;
    setLookingUp(true);
    setLookupResult(null);
    setLookupBalance(null);
    try {
      const result = await mcApi.lookupPlayer(name);
      setLookupResult(result);
      if (result.success && result.username) {
        const bal = await mcApi.getBalance(result.username);
        setLookupBalance(bal.balance);
      }
    } catch {
      setLookupResult({ success: false, message: 'Lookup failed.' });
    } finally {
      setLookingUp(false);
    }
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!identifier.trim() || !amount.trim()) return;
    setSubmitting(true);
    try {
      await mcApi.economyAdjust(identifier.trim(), action, Number(amount));
      showToast(`Adjusted ${identifier.trim()}'s balance.`);
      setAmount('');
      setLookupResult(null);
      setLookupBalance(null);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Adjustment failed.', true);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading || !stats) {
    return (
      <DashboardLayout>
        <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>
      </DashboardLayout>
    );
  }

  const distribution = stats.distribution ?? [];
  const topPlayers = stats.topPlayers ?? [];
  const maxBucket = Math.max(1, ...distribution.map((b) => b.count));

  return (
    <DashboardLayout>
      <PageHeading title="Economy" icon={Coins} subtitle="Balances, leaderboard, distribution, and manual balance adjustments." />

      <div className="grid grid-cols-3 gap-4 mb-5">
        <Card icon={Wallet} accent="moss" padded>
          <div className="text-[11px] text-[var(--mc-text-muted)] mb-1">Total wealth</div>
          <div className="text-[20px] font-display font-semibold">{stats.currencySymbol}{stats.totalWealth}</div>
        </Card>
        <Card icon={Users} accent="cyan" padded>
          <div className="text-[11px] text-[var(--mc-text-muted)] mb-1">Accounts</div>
          <div className="text-[20px] font-display font-semibold">{stats.accountCount}</div>
        </Card>
        <Card icon={TrendingUp} accent="purple" padded>
          <div className="text-[11px] text-[var(--mc-text-muted)] mb-1">Average balance</div>
          <div className="text-[20px] font-display font-semibold">{stats.currencySymbol}{stats.averageBalance}</div>
          <div className="text-[11px] text-[var(--mc-text-muted)] mt-0.5">Starting balance: {stats.currencySymbol}{stats.startingBalance}</div>
        </Card>
      </div>

      <div className="grid grid-cols-[1fr_320px] gap-5">
        <div className="flex flex-col gap-5">
          <Card title="Balance distribution" icon={TrendingUp} accent="purple" padded>
            <div className="flex flex-col gap-2">
              {distribution.map((bucket) => (
                <div key={bucket.label} className="flex items-center gap-2.5 text-[12px]">
                  <span className="w-16 shrink-0 font-data text-[var(--mc-text-secondary)]">{bucket.label}</span>
                  <div className="flex-1 h-4 rounded-[4px] bg-[var(--mc-bg-surface-raised)] overflow-hidden">
                    <div
                      className="h-full rounded-[4px] bg-[var(--mc-purple-400)] transition-all"
                      style={{ width: `${(bucket.count / maxBucket) * 100}%` }}
                    />
                  </div>
                  <span className="w-6 shrink-0 font-data text-right text-[var(--mc-text-muted)]">{bucket.count}</span>
                </div>
              ))}
            </div>
          </Card>

          <Card title="Balance leaderboard" icon={Trophy} accent="cyan">
            {topPlayers.length === 0 && (
              <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No balances tracked yet.</div>
            )}
            {topPlayers.map((entry, i) => (
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
                <Link to={`/lookup?player=${encodeURIComponent(entry.username)}`} className="flex-1 ml-2.5 hover:text-[var(--mc-cyan-400)] hover:underline">
                  {entry.username}
                </Link>
                <span className="font-data text-[13px] text-[var(--mc-moss-400)]">{stats.currencySymbol}{entry.balance.toLocaleString()}</span>
              </div>
            ))}
          </Card>
        </div>

        <Card title="Adjust balance" icon={Sliders} accent="purple" padded className="h-fit">
          <div className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Player UUID or username
              <div className="flex gap-1.5">
                <input
                  value={identifier}
                  onChange={(e) => { setIdentifier(e.target.value); setLookupResult(null); setLookupBalance(null); }}
                  className="flex-1 font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
                <button
                  type="button"
                  onClick={runLookup}
                  disabled={lookingUp || !identifier.trim()}
                  className="flex items-center gap-1 text-[12px] px-2.5 py-1.5 rounded-[8px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors"
                >
                  <Search size={12} /> Look up
                </button>
              </div>
            </label>

            {lookupResult && (
              <div className="text-[12px] rounded-[8px] border border-[var(--mc-border)] px-2.5 py-2">
                {!lookupResult.success ? (
                  <span className="text-[var(--mc-ember-500)]">{lookupResult.message ?? 'Player not found.'}</span>
                ) : (
                  <div className="flex items-center gap-2">
                    <img
                      src={`https://mc-heads.net/avatar/${lookupResult.uuid}/24`}
                      alt=""
                      className="h-5 w-5 rounded-[4px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)]"
                    />
                    <span className="font-medium">{lookupResult.username}</span>
                    <Badge variant={lookupResult.online ? 'moss' : 'neutral'} dot={lookupResult.online}>
                      {lookupResult.online ? 'online' : 'offline'}
                    </Badge>
                    <span className="ml-auto font-data text-[var(--mc-moss-400)]">
                      {lookupBalance !== null ? `${stats.currencySymbol}${lookupBalance}` : '…'}
                    </span>
                  </div>
                )}
              </div>
            )}

            <form onSubmit={submit} className="flex flex-col gap-3">
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

            {lookupResult?.success && lookupResult.username && (
              <Link
                to={`/lookup?player=${encodeURIComponent(lookupResult.username)}`}
                className="flex items-center justify-center gap-1.5 text-[12px] text-[var(--mc-cyan-400)] hover:underline"
              >
                <UserCog size={12} /> Full profile →
              </Link>
            )}
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
}
