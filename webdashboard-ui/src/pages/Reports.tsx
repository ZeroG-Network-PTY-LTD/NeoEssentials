import { useEffect, useState } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { ReportEntry, ReportStatus } from '../types';
import { Flag, Check, X } from 'lucide-react';

const STATUS_BADGE: Record<ReportStatus, 'ember' | 'moss' | 'neutral'> = {
  PENDING: 'ember',
  REVIEWED: 'moss',
  DISMISSED: 'neutral',
};

export default function Reports() {
  const { showToast } = useToast();
  const [reports, setReports] = useState<ReportEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAll, setShowAll] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const refresh = (all = showAll) => {
    setLoading(true);
    (all ? mcApi.allReports() : mcApi.pendingReports())
      .then(setReports)
      .finally(() => setLoading(false));
  };

  useEffect(() => refresh(showAll), [showAll]);

  const review = async (id: string, status: ReportStatus) => {
    setBusyId(id);
    try {
      await mcApi.reviewReport(id, status);
      showToast(status === 'DISMISSED' ? 'Report dismissed.' : 'Report marked reviewed.');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Review failed.', true);
    } finally {
      setBusyId(null);
    }
  };

  const pendingCount = reports.filter((r) => r.status === 'PENDING').length;

  if (loading) {
    return (
      <DashboardLayout>
        <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <PageHeading
        title="Reports"
        icon={Flag}
        count={reports.length}
        subtitle="Player-filed reports from the in-game /report command."
        action={
          <div className="flex gap-1.5 rounded-[var(--radius)] border border-[var(--mc-border-strong)] p-0.5">
            <button
              onClick={() => setShowAll(false)}
              className={`text-[12px] px-2.5 py-1 rounded-[6px] transition-colors ${
                !showAll ? 'bg-[var(--mc-cyan-500)] text-[#0a1620]' : 'text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)]'
              }`}
            >
              Pending{pendingCount > 0 ? ` (${pendingCount})` : ''}
            </button>
            <button
              onClick={() => setShowAll(true)}
              className={`text-[12px] px-2.5 py-1 rounded-[6px] transition-colors ${
                showAll ? 'bg-[var(--mc-cyan-500)] text-[#0a1620]' : 'text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)]'
              }`}
            >
              All
            </button>
          </div>
        }
      />

      <Card title={`${reports.length} report${reports.length === 1 ? '' : 's'}`} icon={Flag}>
        {reports.length === 0 && (
          <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">
            {showAll ? 'No reports on file.' : 'No pending reports.'}
          </div>
        )}
        {reports.map((r) => (
          <div key={r.id} className="flex items-start gap-3 px-4 py-3 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
            <Badge variant={STATUS_BADGE[r.status]}>{r.status.toLowerCase()}</Badge>
            <div className="flex-1 min-w-0">
              <div>
                <span className="font-medium">{r.reporterName}</span>
                <span className="text-[var(--mc-text-muted)]"> reported </span>
                <span className="font-medium">{r.targetName}</span>
              </div>
              <div className="mt-0.5 text-[12.5px] text-[var(--mc-text-secondary)] break-words">{r.reason}</div>
              <div className="mt-1 text-[11px] text-[var(--mc-text-muted)]">
                {new Date(r.timestamp).toLocaleString()}
                {r.status !== 'PENDING' && r.reviewedBy && (
                  <> · {r.status === 'DISMISSED' ? 'Dismissed' : 'Reviewed'} by {r.reviewedBy} · {new Date(r.reviewedAt).toLocaleString()}</>
                )}
              </div>
            </div>
            {r.status === 'PENDING' && (
              <div className="flex gap-1.5 shrink-0">
                <button
                  onClick={() => review(r.id, 'REVIEWED')}
                  disabled={busyId === r.id}
                  className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-moss-500)] text-white transition-colors hover:bg-[var(--mc-moss-600,var(--mc-moss-500))] disabled:opacity-50"
                >
                  <Check size={12} strokeWidth={2} />
                  Reviewed
                </button>
                <button
                  onClick={() => review(r.id, 'DISMISSED')}
                  disabled={busyId === r.id}
                  className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors disabled:opacity-50"
                >
                  <X size={12} strokeWidth={2} />
                  Dismiss
                </button>
              </div>
            )}
          </div>
        ))}
      </Card>
    </DashboardLayout>
  );
}
