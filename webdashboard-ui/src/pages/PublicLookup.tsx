import { useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import * as mcApi from '../lib/mcApi';
import PlayerRender from '../components/PlayerRender';
import PlayerManagementPanel from '../components/PlayerManagementPanel';
import { Search, ShieldBan, VolumeX, LogOut, TriangleAlert, Ban, History, Gauge } from 'lucide-react';
import type { IPBanEntry, MuteEntry as IpMuteEntry } from '../types';

/**
 * Ported from the external dashboard's Pages/PublicLookup.tsx — this page needs no login on
 * either side (the mod's /api/public/moderation/* routes are registered without the
 * Bearer-token check), so it isn't wrapped in <RequireAuth> in App.tsx, and mcApi.publicLookup/
 * publicRecent() use plain fetch() rather than the session-carrying mcFetch(). The Inertia
 * `query`/`result`/`recent` props (set once per page load) became a client-side search + fetch.
 *
 * The former standalone /players/player/:username page (PlayerProfile.tsx) has since been
 * merged in here as PlayerManagementPanel, mounted below when a signed-in dashboard user
 * looks up a player — see the external dashboard's equivalent PublicLookup.tsx change for the
 * same restructuring. Gated on `token` (any signed-in role), matching the gate the standalone
 * page itself previously had (RequireAuth, no finer-grained per-permission check existed on
 * either side of this before or after the merge).
 */

interface PunishmentBase {
  id: string | number;
  reason: string | null;
  active: boolean;
  permanent: boolean;
}

interface BanRecord extends PunishmentBase {
  playerName: string;
  playerId: string;
  bannedBy: string;
  banTime: number;
  expireTime: number;
  unbannedBy: string | null;
  unbannedAt: number;
}

interface MuteRecord extends PunishmentBase {
  target: string;
  mutedBy: string;
  muteTime: number;
  expireTime: number;
  unmutedBy: string | null;
  unmutedAt: number;
}

interface KickRecord {
  id: string | number;
  playerName: string;
  reason: string | null;
  kickedBy: string;
  kickTime: number;
}

interface WarnRecord {
  id: string | number;
  targetName: string;
  warnedBy: string;
  reason: string | null;
  timestamp: number;
}

interface LookupResult {
  success: boolean;
  playerName: string;
  playerId: string | null;
  bans: BanRecord[];
  mutes: MuteRecord[];
  kicks: KickRecord[];
  warns: WarnRecord[];
}

type RecentEntry = (BanRecord & { type: 'ban' }) | (MuteRecord & { type: 'mute' });

function formatDate(ms: number) {
  return ms ? new Date(ms).toLocaleString() : '—';
}

function StatusPill({ active, permanent }: { active: boolean; permanent: boolean }) {
  if (!active) {
    return <span className="rounded-full bg-[var(--mc-bg-surface-raised)] px-2 py-0.5 text-xs text-[var(--mc-text-muted)]">lifted</span>;
  }
  return (
    <span className="rounded-full bg-[var(--mc-ember-50)] px-2 py-0.5 text-xs text-[var(--mc-ember-500)]">{permanent ? 'active · permanent' : 'active'}</span>
  );
}

type Tab = 'moderation' | 'recent' | 'ipbans';

const TABS: { id: Tab; label: string; icon: typeof ShieldBan }[] = [
  { id: 'moderation', label: 'Moderation', icon: Gauge },
  { id: 'recent', label: 'Recent Activity', icon: History },
  { id: 'ipbans', label: 'IP Bans', icon: Ban },
];

/** Lightweight pill tab bar — matches the toggle-button styling used elsewhere in this
 *  dashboard (e.g. Reports.tsx's Pending/All toggle) since there's no shared SegmentedTabs
 *  component here yet (unlike the external dashboard, which has one). */
function TabBar({ value, onChange }: { value: Tab; onChange: (tab: Tab) => void }) {
  return (
    <div className="mt-8 flex gap-1.5 rounded-[var(--radius)] border border-[var(--mc-border-strong)] p-0.5 w-fit">
      {TABS.map(({ id, label, icon: Icon }) => (
        <button
          key={id}
          onClick={() => onChange(id)}
          className={`flex items-center gap-1.5 text-[12px] px-3 py-1.5 rounded-[6px] transition-colors ${
            value === id
              ? 'bg-[var(--mc-cyan-500)] text-[#0a1620]'
              : 'text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)]'
          }`}
        >
          <Icon size={13} strokeWidth={2} />
          {label}
        </button>
      ))}
    </div>
  );
}

function SectionCard({ icon: Icon, title, count, children }: { icon: typeof ShieldBan; title: string; count: number; children: React.ReactNode }) {
  return (
    <div className="rounded-[var(--radius-lg)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface)] p-5">
      <div className="flex items-center gap-2">
        <Icon size={16} strokeWidth={1.75} className="text-[var(--mc-cyan-500)]" />
        <h2 className="font-display text-sm font-semibold">{title}</h2>
        <span className="ml-auto text-xs text-[var(--mc-text-muted)]">{count}</span>
      </div>
      <div className="mt-3 space-y-2">{count === 0 ? <p className="text-sm text-[var(--mc-text-muted)]">No records.</p> : children}</div>
    </div>
  );
}

export default function PublicLookup() {
  const { token } = useAuth();
  const [searchParams] = useSearchParams();
  const [name, setName] = useState(() => searchParams.get('player') ?? '');
  const [query, setQuery] = useState<string | null>(null);
  const [result, setResult] = useState<LookupResult | null>(null);
  const [recent, setRecent] = useState<RecentEntry[]>([]);
  const [ipBans, setIpBans] = useState<IPBanEntry[]>([]);
  const [ipMutes, setIpMutes] = useState<IpMuteEntry[]>([]);
  const [activeTab, setActiveTab] = useState<Tab>('moderation');

  const runLookup = async (playerName: string) => {
    const q = playerName.trim();
    if (!q) return;
    setQuery(q);
    try {
      setResult(await mcApi.publicLookup<LookupResult>(q));
    } catch {
      setResult(null);
    }
  };

  useEffect(() => {
    mcApi.publicRecent<RecentEntry>().then(setRecent);
    mcApi.publicIpBans().then(setIpBans);
    mcApi.publicIpMutes().then(setIpMutes);
    // Deep-linked from in-game (e.g. the chat "view profile" click) as /lookup?player=<name> —
    // run the lookup immediately instead of requiring the player to re-type the name.
    const initial = searchParams.get('player');
    if (initial && initial.trim()) runLookup(initial);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    runLookup(name);
  };

  return (
    <div className="min-h-screen bg-[var(--mc-bg-base)] text-[var(--mc-text-primary)]">
      <div className="mx-auto max-w-5xl px-6">
        <header className="flex items-center justify-between py-8">
          <Link to="/" className="flex items-center gap-2">
            <img src="/logo.png" alt="" className="h-7 w-7 object-contain" />
            <span className="font-display text-lg font-semibold tracking-tight">NeoEssentials</span>
          </Link>

          <Link to={token ? '/' : '/login'} className="rounded-[var(--radius)] px-4 py-2 text-sm font-medium text-[var(--mc-text-secondary)] transition hover:text-[var(--mc-text-primary)]">
            {token ? 'Dashboard' : 'Staff log in'}
          </Link>
        </header>

        <main className="pb-20">
          <h1 className="font-display text-2xl font-semibold">Player Lookup</h1>
          <p className="mt-1 text-sm text-[var(--mc-text-secondary)]">Search any player to see their public moderation record — bans, mutes, kicks, and warnings, with full history.</p>

          <form onSubmit={submit} className="mt-6 flex gap-2">
            <div className="relative flex-1">
              <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--mc-text-muted)]" />
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Player name"
                className="w-full rounded-[var(--radius)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface-raised)] py-2 pl-9 pr-3 text-sm text-[var(--mc-text-primary)] placeholder:text-[var(--mc-text-muted)] focus:border-[var(--mc-cyan-500)] focus:outline-none focus:ring-1 focus:ring-[var(--mc-cyan-500)]"
              />
            </div>
            <button type="submit" className="rounded-[var(--radius)] bg-[var(--mc-cyan-500)] px-5 py-2 text-sm font-semibold text-[#12151a] transition hover:bg-[var(--mc-cyan-400)]">
              Search
            </button>
          </form>

          {query && !result && (
            <div className="mt-8 rounded-[var(--radius-lg)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface)] p-5 text-sm text-[var(--mc-text-secondary)]">
              Couldn't reach the moderation lookup service. Try again shortly.
            </div>
          )}

          {result && (
            <div className="mt-8 space-y-4">
              <div className="flex items-center gap-4">
                <PlayerRender uuid={result.playerId} size={160} />
                <h2 className="font-display text-lg font-semibold">{result.playerName}</h2>
              </div>

              {token && (
                <div className="rounded-[var(--radius-lg)] border border-[var(--mc-purple-400)] bg-[var(--mc-bg-surface)] p-5">
                  <PlayerManagementPanel username={result.playerName} />
                </div>
              )}
            </div>
          )}

          <TabBar value={activeTab} onChange={setActiveTab} />

          {activeTab === 'moderation' && (
            <div className="mt-4">
              {!result ? (
                <p className="text-sm text-[var(--mc-text-muted)]">Search for a player above to see their moderation record.</p>
              ) : (
                <div className="space-y-4">
                  <SectionCard icon={ShieldBan} title="Bans" count={result.bans.length}>
                    {result.bans.map((b) => (
                      <div key={b.id} className="rounded-[var(--radius)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface-raised)] p-3 text-sm">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[var(--mc-text-secondary)]">{b.reason || 'No reason given'}</span>
                          <StatusPill active={b.active} permanent={b.permanent} />
                        </div>
                        <div className="mt-1 text-xs text-[var(--mc-text-muted)]">
                          Banned by {b.bannedBy} · {formatDate(b.banTime)}
                          {b.active && !b.permanent && <> · Expires {formatDate(b.expireTime)}</>}
                          {!b.active && b.unbannedBy && <> · Unbanned by {b.unbannedBy}</>}
                        </div>
                      </div>
                    ))}
                  </SectionCard>

                  <SectionCard icon={VolumeX} title="Mutes" count={result.mutes.length}>
                    {result.mutes.map((m) => (
                      <div key={m.id} className="rounded-[var(--radius)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface-raised)] p-3 text-sm">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[var(--mc-text-secondary)]">{m.reason || 'No reason given'}</span>
                          <StatusPill active={m.active} permanent={m.permanent} />
                        </div>
                        <div className="mt-1 text-xs text-[var(--mc-text-muted)]">
                          Muted by {m.mutedBy} · {formatDate(m.muteTime)}
                          {m.active && !m.permanent && <> · Expires {formatDate(m.expireTime)}</>}
                          {!m.active && m.unmutedBy && <> · Unmuted by {m.unmutedBy}</>}
                        </div>
                      </div>
                    ))}
                  </SectionCard>

                  <SectionCard icon={LogOut} title="Kicks" count={result.kicks.length}>
                    {result.kicks.map((k) => (
                      <div key={k.id} className="rounded-[var(--radius)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface-raised)] p-3 text-sm">
                        <span className="text-[var(--mc-text-secondary)]">{k.reason || 'No reason given'}</span>
                        <div className="mt-1 text-xs text-[var(--mc-text-muted)]">
                          Kicked by {k.kickedBy} · {formatDate(k.kickTime)}
                        </div>
                      </div>
                    ))}
                  </SectionCard>

                  <SectionCard icon={TriangleAlert} title="Warnings" count={result.warns.length}>
                    {result.warns.map((w) => (
                      <div key={w.id} className="rounded-[var(--radius)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface-raised)] p-3 text-sm">
                        <span className="text-[var(--mc-text-secondary)]">{w.reason || 'No reason given'}</span>
                        <div className="mt-1 text-xs text-[var(--mc-text-muted)]">
                          Warned by {w.warnedBy} · {formatDate(w.timestamp)}
                        </div>
                      </div>
                    ))}
                  </SectionCard>
                </div>
              )}
            </div>
          )}

          {activeTab === 'recent' && (
            <div className="mt-4">
              <div className="divide-y divide-[var(--mc-border)] rounded-[var(--radius-lg)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface)]">
                {recent.length === 0 ? (
                  <p className="p-4 text-sm text-[var(--mc-text-muted)]">Nothing recent.</p>
                ) : (
                  recent.map((entry) => (
                    <div key={`${entry.type}-${entry.id}`} className="flex items-center gap-3 p-3 text-sm">
                      {entry.type === 'ban' ? <ShieldBan size={15} className="text-[var(--mc-ember-500)]" /> : <VolumeX size={15} className="text-[var(--mc-cyan-500)]" />}
                      <button
                        onClick={() => runLookup(entry.type === 'ban' ? entry.playerName : entry.target)}
                        className="font-medium text-[var(--mc-text-primary)] hover:text-[var(--mc-cyan-500)]"
                      >
                        {entry.type === 'ban' ? entry.playerName : entry.target}
                      </button>
                      <span className="text-[var(--mc-text-muted)]">{entry.reason || 'No reason given'}</span>
                      <span className="ml-auto shrink-0 text-xs text-[var(--mc-text-muted)]">{formatDate(entry.type === 'ban' ? entry.banTime : entry.muteTime)}</span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === 'ipbans' && (
            <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2">
              <div>
                <h2 className="font-display text-sm font-semibold text-[var(--mc-text-secondary)]">IP bans</h2>
                <div className="mt-3 divide-y divide-[var(--mc-border)] rounded-[var(--radius-lg)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface)]">
                  {ipBans.length === 0 ? (
                    <p className="p-4 text-sm text-[var(--mc-text-muted)]">No active IP bans.</p>
                  ) : (
                    ipBans.map((b) => (
                      <div key={b.id} className="flex items-center gap-3 p-3 text-sm">
                        <Ban size={15} className="text-[var(--mc-ember-500)]" />
                        <span className="font-data font-medium text-[var(--mc-text-primary)]">{b.ipAddress}</span>
                        <span className="text-[var(--mc-text-muted)]">{b.reason || 'No reason given'}</span>
                        <span className="ml-auto shrink-0 text-xs text-[var(--mc-text-muted)]">{formatDate(b.banTime)}</span>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div>
                <h2 className="font-display text-sm font-semibold text-[var(--mc-text-secondary)]">IP mutes</h2>
                <div className="mt-3 divide-y divide-[var(--mc-border)] rounded-[var(--radius-lg)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface)]">
                  {ipMutes.length === 0 ? (
                    <p className="p-4 text-sm text-[var(--mc-text-muted)]">No active IP mutes.</p>
                  ) : (
                    ipMutes.map((m) => (
                      <div key={m.id} className="flex items-center gap-3 p-3 text-sm">
                        <VolumeX size={15} className="text-[var(--mc-cyan-500)]" />
                        <span className="font-data font-medium text-[var(--mc-text-primary)]">{m.target}</span>
                        <span className="text-[var(--mc-text-muted)]">{m.reason || 'No reason given'}</span>
                        <span className="ml-auto shrink-0 text-xs text-[var(--mc-text-muted)]">{formatDate(m.muteTime)}</span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
