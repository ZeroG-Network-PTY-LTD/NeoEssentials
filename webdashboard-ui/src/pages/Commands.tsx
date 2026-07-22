import { useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import { Terminal, Play, Zap } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Commands.tsx — useForm() became plain
 * useState; recentlySuccessful became a local timed flag set from the mcApi call's own result. */

const QUICK_COMMANDS = ['time set day', 'time set night', 'weather clear', 'weather rain'];

export default function Commands() {
  const { showToast } = useToast();
  const [command, setCommand] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!command.trim()) return;
    setSubmitting(true);
    try {
      await mcApi.runCommand(command.trim());
      showToast('Command sent.');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Command failed.', true);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <DashboardLayout>
      <PageHeading title="Commands" icon={Terminal} subtitle="Run a command directly on the server console." />

      <Card title="Console" icon={Terminal} padded>
        <form onSubmit={submit}>
          <div className="flex gap-2">
            <span className="font-data text-[13px] text-[var(--mc-text-muted)] py-2">/</span>
            <input
              value={command}
              onChange={(e) => setCommand(e.target.value)}
              placeholder="broadcast Hello everyone!"
              className="flex-1 font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-3 py-2 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
            />
            <button
              type="submit"
              disabled={submitting}
              className="btn-pop flex items-center gap-1.5 text-[13px] px-4 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
            >
              <Play size={13} strokeWidth={2} />
              Run
            </button>
          </div>
          <div className="flex flex-wrap gap-2 mt-3">
            {QUICK_COMMANDS.map((cmd) => (
              <button
                key={cmd}
                type="button"
                onClick={() => setCommand(cmd)}
                className="flex items-center gap-1.5 text-[11px] px-2.5 py-1 rounded-[6px] border border-[var(--mc-border-strong)] font-data transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
              >
                <Zap size={11} strokeWidth={2} className="text-[var(--mc-purple-400)]" />/{cmd}
              </button>
            ))}
          </div>
        </form>
      </Card>
    </DashboardLayout>
  );
}
