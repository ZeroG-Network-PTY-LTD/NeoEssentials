import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { Warp } from '../types';
import { MapPin, PlusCircle, Trash2 } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Warps.tsx — useForm()/router.delete()
 * became plain useState + mcApi calls; window.confirm() kept as-is (no framework dependency). */

export default function Warps() {
  const { showToast } = useToast();
  const [warps, setWarps] = useState<Warp[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [world, setWorld] = useState('minecraft:overworld');
  const [x, setX] = useState('');
  const [y, setY] = useState('');
  const [z, setZ] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const refresh = () => {
    mcApi.warps().then(setWarps).finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true);
    try {
      await mcApi.createWarp(name.trim(), { world, x: Number(x), y: Number(y), z: Number(z) });
      showToast(`Created warp '${name.trim()}'.`);
      setName('');
      setX('');
      setY('');
      setZ('');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Could not create warp.', true);
    } finally {
      setSubmitting(false);
    }
  };

  const destroy = async (warpName: string) => {
    if (!confirm(`Delete warp '${warpName}'?`)) return;
    try {
      await mcApi.deleteWarp(warpName);
      showToast(`Deleted warp '${warpName}'.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Could not delete warp.', true);
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
      <PageHeading title="Warps" icon={MapPin} count={warps.length} subtitle="Named teleport points on the server." />

      <div className="grid grid-cols-[1fr_320px] gap-5">
        <Card title={`${warps.length} warp${warps.length === 1 ? '' : 's'}`} icon={MapPin}>
          {warps.length === 0 && <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No warps yet.</div>}
          {warps.map((warp) => (
            <div
              key={warp.name}
              className="flex items-center px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
            >
              <span className="flex-1 font-medium">{warp.name}</span>
              <span className="font-data text-[12px] text-[var(--mc-text-muted)] mr-3">
                {warp.dimension.replace('minecraft:', '')} · {Math.round(warp.x)}, {Math.round(warp.y)}, {Math.round(warp.z)}
              </span>
              <span className="text-[12px] text-[var(--mc-text-muted)] mr-3">by {warp.createdBy}</span>
              <button
                onClick={() => destroy(warp.name)}
                className="btn-pop flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
              >
                <Trash2 size={12} strokeWidth={2} />
                Delete
              </button>
            </div>
          ))}
        </Card>

        <Card title="Create warp" icon={PlusCircle} accent="purple" padded className="h-fit">
          <form onSubmit={submit} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Name
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              World
              <input
                value={world}
                onChange={(e) => setWorld(e.target.value)}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <div className="grid grid-cols-3 gap-2">
              <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                X
                <input
                  type="number"
                  value={x}
                  onChange={(e) => setX(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
              </label>
              <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                Y
                <input
                  type="number"
                  value={y}
                  onChange={(e) => setY(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
              </label>
              <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                Z
                <input
                  type="number"
                  value={z}
                  onChange={(e) => setZ(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
              </label>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn-pop mt-1 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] transition-colors disabled:opacity-50"
            >
              {submitting ? 'Creating…' : 'Create'}
            </button>
          </form>
        </Card>
      </div>
    </DashboardLayout>
  );
}
