import { useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import { Send } from 'lucide-react';

export default function ReportPlayer() {
  const { showToast } = useToast();
  const [targetName, setTargetName] = useState('');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!targetName.trim() || !reason.trim()) return;
    setSubmitting(true);
    try {
      await mcApi.fileReport(targetName.trim(), reason.trim());
      showToast(`Report filed against '${targetName.trim()}'. Staff will review it soon.`);
      setTargetName('');
      setReason('');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to file report.', true);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <DashboardLayout>
      <PageHeading
        title="Report a player"
        icon={Send}
        subtitle="Let staff know about rule-breaking or bad behavior. Same as using /report in-game."
      />

      <div className="max-w-[420px]">
        <Card title="File a report" icon={Send} accent="purple" padded>
          <form onSubmit={submit} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Player
              <input
                value={targetName}
                onChange={(e) => setTargetName(e.target.value)}
                placeholder="Username"
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Reason
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={4}
                placeholder="What happened?"
                className="text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)] resize-none"
              />
            </label>

            <button
              type="submit"
              disabled={submitting}
              className="btn-pop mt-1 flex items-center justify-center gap-1.5 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
            >
              <Send size={13} strokeWidth={2} />
              File report
            </button>
          </form>
        </Card>
      </div>
    </DashboardLayout>
  );
}
