import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { IPBanEntry, MuteEntry } from '../types';
import { Ban, VolumeX, Plus, X } from 'lucide-react';

function formatDate(ms: number) {
  return ms ? new Date(ms).toLocaleString() : '—';
}

export default function IpBans() {
  const { showToast } = useToast();

  const [bans, setBans] = useState<IPBanEntry[]>([]);
  const [bansLoading, setBansLoading] = useState(true);
  const [showAllBans, setShowAllBans] = useState(false);
  const [banBusy, setBanBusy] = useState<string | null>(null);

  const [mutes, setMutes] = useState<MuteEntry[]>([]);
  const [mutesLoading, setMutesLoading] = useState(true);
  const [muteBusy, setMuteBusy] = useState<string | null>(null);

  const [banIp, setBanIp] = useState('');
  const [banReason, setBanReason] = useState('');
  const [banDuration, setBanDuration] = useState('');
  const [banSubmitting, setBanSubmitting] = useState(false);

  const [muteIp, setMuteIp] = useState('');
  const [muteReason, setMuteReason] = useState('');
  const [muteDuration, setMuteDuration] = useState('');
  const [muteSubmitting, setMuteSubmitting] = useState(false);

  const refreshBans = (all = showAllBans) => {
    setBansLoading(true);
    mcApi.ipBans(all).then(setBans).finally(() => setBansLoading(false));
  };

  const refreshMutes = () => {
    setMutesLoading(true);
    mcApi.ipMutes().then(setMutes).finally(() => setMutesLoading(false));
  };

  useEffect(() => refreshBans(showAllBans), [showAllBans]);
  useEffect(refreshMutes, []);

  const submitBan = async (e: FormEvent) => {
    e.preventDefault();
    if (!banIp.trim() || !banReason.trim()) return;
    setBanSubmitting(true);
    try {
      await mcApi.createIpBan(banIp.trim(), banReason.trim(), banDuration.trim() || undefined);
      showToast(`IP ${banIp.trim()} banned.`);
      setBanIp('');
      setBanReason('');
      setBanDuration('');
      refreshBans();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to ban IP.', true);
    } finally {
      setBanSubmitting(false);
    }
  };

  const unbanIp = async (ip: string) => {
    setBanBusy(ip);
    try {
      await mcApi.removeIpBan(ip);
      showToast(`IP ${ip} unbanned.`);
      refreshBans();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Unban failed.', true);
    } finally {
      setBanBusy(null);
    }
  };

  const submitMute = async (e: FormEvent) => {
    e.preventDefault();
    if (!muteIp.trim()) return;
    setMuteSubmitting(true);
    try {
      await mcApi.createIpMute(muteIp.trim(), muteReason.trim(), muteDuration.trim() || undefined);
      showToast(`IP ${muteIp.trim()} muted.`);
      setMuteIp('');
      setMuteReason('');
      setMuteDuration('');
      refreshMutes();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to mute IP.', true);
    } finally {
      setMuteSubmitting(false);
    }
  };

  const unmuteIp = async (ip: string) => {
    setMuteBusy(ip);
    try {
      await mcApi.removeIpMute(ip);
      showToast(`IP ${ip} unmuted.`);
      refreshMutes();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Unmute failed.', true);
    } finally {
      setMuteBusy(null);
    }
  };

  return (
    <DashboardLayout>
      <PageHeading
        title="IP Bans"
        icon={Ban}
        subtitle="Bans and mutes applied at the IP-address level, separate from per-player punishments."
      />

      <div className="grid grid-cols-[1fr_320px] gap-5 mb-5">
        <Card
          title={`${bans.length} IP ban${bans.length === 1 ? '' : 's'}`}
          icon={Ban}
          action={
            <div className="flex gap-1.5 rounded-[var(--radius)] border border-[var(--mc-border-strong)] p-0.5">
              <button
                onClick={() => setShowAllBans(false)}
                className={`text-[12px] px-2.5 py-1 rounded-[6px] transition-colors ${
                  !showAllBans ? 'bg-[var(--mc-cyan-500)] text-[#0a1620]' : 'text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)]'
                }`}
              >
                Active
              </button>
              <button
                onClick={() => setShowAllBans(true)}
                className={`text-[12px] px-2.5 py-1 rounded-[6px] transition-colors ${
                  showAllBans ? 'bg-[var(--mc-cyan-500)] text-[#0a1620]' : 'text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)]'
                }`}
              >
                All
              </button>
            </div>
          }
        >
          {!bansLoading && bans.length === 0 && (
            <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">
              {showAllBans ? 'No IP bans on file.' : 'No active IP bans.'}
            </div>
          )}
          {bans.map((b) => (
            <div key={b.id} className="flex items-start gap-3 px-4 py-3 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
              <Badge variant={b.active ? 'ember' : 'neutral'}>{b.active ? 'active' : 'lifted'}</Badge>
              <div className="flex-1 min-w-0">
                <div className="font-data font-medium">{b.ipAddress}</div>
                <div className="mt-0.5 text-[12.5px] text-[var(--mc-text-secondary)] break-words">{b.reason}</div>
                <div className="mt-1 text-[11px] text-[var(--mc-text-muted)]">
                  Banned by {b.bannedBy} · {formatDate(b.banTime)}
                  {b.permanent ? ' · Permanent' : ` · Until ${formatDate(b.expireTime)}`}
                  {!b.active && b.unbannedBy && <> · Unbanned by {b.unbannedBy} · {formatDate(b.unbannedAt ?? 0)}</>}
                </div>
              </div>
              {b.active && (
                <button
                  onClick={() => unbanIp(b.ipAddress)}
                  disabled={banBusy === b.ipAddress}
                  className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors disabled:opacity-50 shrink-0"
                >
                  <X size={12} strokeWidth={2} />
                  Unban
                </button>
              )}
            </div>
          ))}
        </Card>

        <Card title="Ban an IP" icon={Plus} accent="purple" padded className="h-fit">
          <form onSubmit={submitBan} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              IP address
              <input
                value={banIp}
                onChange={(e) => setBanIp(e.target.value)}
                placeholder="203.0.113.42"
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Reason
              <textarea
                value={banReason}
                onChange={(e) => setBanReason(e.target.value)}
                rows={2}
                placeholder="Why is this IP being banned?"
                className="text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)] resize-none"
              />
            </label>
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Duration (seconds, blank = permanent)
              <input
                value={banDuration}
                onChange={(e) => setBanDuration(e.target.value)}
                placeholder="e.g. 86400 for 1 day"
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>
            <button
              type="submit"
              disabled={banSubmitting}
              className="btn-pop mt-1 flex items-center justify-center gap-1.5 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white font-medium transition-colors hover:opacity-90 disabled:opacity-50"
            >
              <Ban size={13} strokeWidth={2} />
              Ban IP
            </button>
          </form>
        </Card>
      </div>

      <div className="grid grid-cols-[1fr_320px] gap-5">
        <Card title={`${mutes.length} IP mute${mutes.length === 1 ? '' : 's'}`} icon={VolumeX}>
          {!mutesLoading && mutes.length === 0 && (
            <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No active IP mutes.</div>
          )}
          {mutes.map((m) => (
            <div key={m.id} className="flex items-start gap-3 px-4 py-3 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
              <Badge variant={m.active ? 'ember' : 'neutral'}>{m.active ? 'active' : 'lifted'}</Badge>
              <div className="flex-1 min-w-0">
                <div className="font-data font-medium">{m.target}</div>
                <div className="mt-0.5 text-[12.5px] text-[var(--mc-text-secondary)] break-words">{m.reason ?? 'No reason given'}</div>
                <div className="mt-1 text-[11px] text-[var(--mc-text-muted)]">
                  Muted by {m.mutedBy} · {formatDate(m.muteTime)}
                  {m.permanent ? ' · Permanent' : ` · Until ${formatDate(m.expireTime)}`}
                </div>
              </div>
              {m.active && (
                <button
                  onClick={() => unmuteIp(m.target)}
                  disabled={muteBusy === m.target}
                  className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors disabled:opacity-50 shrink-0"
                >
                  <X size={12} strokeWidth={2} />
                  Unmute
                </button>
              )}
            </div>
          ))}
        </Card>

        <Card title="Mute an IP" icon={Plus} accent="purple" padded className="h-fit">
          <form onSubmit={submitMute} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              IP address
              <input
                value={muteIp}
                onChange={(e) => setMuteIp(e.target.value)}
                placeholder="203.0.113.42"
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Reason
              <textarea
                value={muteReason}
                onChange={(e) => setMuteReason(e.target.value)}
                rows={2}
                placeholder="Why is this IP being muted?"
                className="text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)] resize-none"
              />
            </label>
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Duration (seconds, blank = permanent)
              <input
                value={muteDuration}
                onChange={(e) => setMuteDuration(e.target.value)}
                placeholder="e.g. 3600 for 1 hour"
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>
            <button
              type="submit"
              disabled={muteSubmitting}
              className="btn-pop mt-1 flex items-center justify-center gap-1.5 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
            >
              <VolumeX size={13} strokeWidth={2} />
              Mute IP
            </button>
          </form>
        </Card>
      </div>
    </DashboardLayout>
  );
}
