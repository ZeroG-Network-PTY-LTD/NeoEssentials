import { Head } from '@inertiajs/react';
import DashboardLayout from '@/Layouts/DashboardLayout';
import type { LogEntry, LogEntryType } from '@/types/minecraft';

interface Props {
  entries: LogEntry[];
}

const TYPE_COLOR: Record<LogEntryType, string> = {
  join: 'text-[var(--mc-moss-500)]',
  leave: 'text-[var(--mc-ember-500)]',
  command: 'text-[var(--mc-copper-500)]',
  chat: 'text-[var(--mc-text-primary)]',
};

function formatTime(ts: number): string {
  return new Date(ts * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export default function Logs({ entries }: Props) {
  return (
    <DashboardLayout>
      <Head title="Logs" />
      <h1 className="font-display text-[20px] font-semibold mb-5">Activity log</h1>

      <div className="rounded-[var(--radius-lg)] bg-[var(--mc-bg-surface)] border border-[var(--mc-border)] px-4 py-2">
        {entries.map((entry, i) => (
          <div
            key={i}
            className="flex gap-3 py-1.5 border-b border-[var(--mc-border)] last:border-0 font-data text-[12px]"
          >
            <span className="text-[var(--mc-text-muted)] w-12 shrink-0">{formatTime(entry.timestamp)}</span>
            <span className={`${TYPE_COLOR[entry.type]} w-16 shrink-0 uppercase`}>{entry.type}</span>
            <span className="text-[var(--mc-text-secondary)]">
              {entry.username}: {entry.message}
            </span>
          </div>
        ))}
        {entries.length === 0 && (
          <div className="px-1 py-6 text-center text-[13px] text-[var(--mc-text-muted)]">
            No activity yet.
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
