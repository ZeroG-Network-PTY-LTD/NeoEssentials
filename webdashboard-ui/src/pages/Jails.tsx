import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { JailLocation, JailShape } from '../types';
import type { ServerWorld } from '../lib/mcApi';
import { Lock, Plus, X } from 'lucide-react';

function formatDate(ms: number) {
  return ms ? new Date(ms).toLocaleString() : '—';
}

export default function Jails() {
  const { showToast } = useToast();

  const [jails, setJails] = useState<JailLocation[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [worlds, setWorlds] = useState<ServerWorld[]>([]);

  const [name, setName] = useState('');
  const [dimension, setDimension] = useState('minecraft:overworld');
  const [shape, setShape] = useState<JailShape>('SPHERE');
  const [x, setX] = useState('');
  const [y, setY] = useState('');
  const [z, setZ] = useState('');
  const [radius, setRadius] = useState('');
  const [x2, setX2] = useState('');
  const [y2, setY2] = useState('');
  const [z2, setZ2] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const refresh = () => {
    setLoading(true);
    mcApi.jailLocationsDetailed().then(setJails).finally(() => setLoading(false));
  };

  useEffect(() => {
    refresh();
    mcApi.serverWorlds().then(setWorlds).catch(() => setWorlds([]));
  }, []);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim() || x === '' || y === '' || z === '') return;
    if (shape === 'CUBOID' && (x2 === '' || y2 === '' || z2 === '')) return;
    setSubmitting(true);
    try {
      const position = { x: Number(x), y: Number(y), z: Number(z) };
      if (shape === 'CUBOID') {
        const corner2 = { x: Number(x2), y: Number(y2), z: Number(z2) };
        await mcApi.createJailLocationCuboid(name.trim(), dimension, position, corner2);
      } else {
        await mcApi.createJailLocationSphere(name.trim(), dimension, position, radius.trim() ? Number(radius) : undefined);
      }
      showToast(`Jail '${name.trim()}' created.`);
      setName('');
      setX('');
      setY('');
      setZ('');
      setRadius('');
      setX2('');
      setY2('');
      setZ2('');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create jail.', true);
    } finally {
      setSubmitting(false);
    }
  };

  const removeJail = async (jailName: string) => {
    setBusy(jailName);
    try {
      await mcApi.removeJailLocation(jailName);
      showToast(`Jail '${jailName}' removed.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to remove jail.', true);
    } finally {
      setBusy(null);
    }
  };

  const inputClass = "font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]";

  return (
    <DashboardLayout>
      <PageHeading
        title="Jails"
        icon={Lock}
        subtitle="Jail cells, defined by coordinates. Create one here, or in-game with /setjail and the jail wand — both write to the same store."
      />

      <div className="grid grid-cols-[1fr_360px] gap-5">
        <Card title={`${jails.length} jail${jails.length === 1 ? '' : 's'}`} icon={Lock}>
          {!loading && jails.length === 0 && (
            <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No jails set up yet.</div>
          )}
          {jails.map((j) => (
            <div key={j.name} className="flex items-start gap-3 px-4 py-3 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
              <Badge variant={j.shape === 'CUBOID' ? 'purple' : 'cyan'}>{j.shape.toLowerCase()}</Badge>
              <div className="flex-1 min-w-0">
                <div className="font-medium">{j.name}</div>
                <div className="mt-0.5 text-[12.5px] text-[var(--mc-text-secondary)] font-data">
                  {j.dimension} · {j.position.x}, {j.position.y}, {j.position.z}
                  {j.shape === 'SPHERE' && j.radius !== undefined && ` (r=${j.radius})`}
                  {j.shape === 'CUBOID' && j.corner1 && j.corner2 && (
                    ` to ${j.corner2.x}, ${j.corner2.y}, ${j.corner2.z}`
                  )}
                </div>
                <div className="mt-1 text-[11px] text-[var(--mc-text-muted)]">
                  Created by {j.createdBy} · {formatDate(j.createdTime)}
                </div>
              </div>
              <button
                onClick={() => removeJail(j.name)}
                disabled={busy === j.name}
                className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors disabled:opacity-50 shrink-0"
              >
                <X size={12} strokeWidth={2} />
                Remove
              </button>
            </div>
          ))}
        </Card>

        <Card title="Create a jail" icon={Plus} accent="purple" padded className="h-fit">
          <form onSubmit={submit} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Name
              <input value={name} onChange={(e) => setName(e.target.value)} placeholder="mainjail" className={inputClass} />
            </label>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Dimension
              {worlds.length > 0 ? (
                <select value={dimension} onChange={(e) => setDimension(e.target.value)} className={inputClass}>
                  {worlds.map((w) => (
                    <option key={w.dimension} value={w.dimension}>{w.name} ({w.dimension})</option>
                  ))}
                </select>
              ) : (
                <input value={dimension} onChange={(e) => setDimension(e.target.value)} placeholder="minecraft:overworld" className={inputClass} />
              )}
            </label>

            <div className="flex gap-1.5 rounded-[var(--radius)] border border-[var(--mc-border-strong)] p-0.5 w-fit">
              {(['SPHERE', 'CUBOID'] as JailShape[]).map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setShape(s)}
                  className={`text-[12px] px-2.5 py-1 rounded-[6px] transition-colors ${
                    shape === s ? 'bg-[var(--mc-cyan-500)] text-[#0a1620]' : 'text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)]'
                  }`}
                >
                  {s === 'SPHERE' ? 'Sphere' : 'Cuboid'}
                </button>
              ))}
            </div>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              {shape === 'SPHERE' ? 'Center (X, Y, Z)' : 'Corner 1 (X, Y, Z)'}
              <div className="flex gap-1.5">
                <input value={x} onChange={(e) => setX(e.target.value)} placeholder="X" className={inputClass + ' w-full'} />
                <input value={y} onChange={(e) => setY(e.target.value)} placeholder="Y" className={inputClass + ' w-full'} />
                <input value={z} onChange={(e) => setZ(e.target.value)} placeholder="Z" className={inputClass + ' w-full'} />
              </div>
            </label>

            {shape === 'SPHERE' ? (
              <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                Radius (blank = server default)
                <input value={radius} onChange={(e) => setRadius(e.target.value)} placeholder="10" className={inputClass} />
              </label>
            ) : (
              <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                Corner 2 (X, Y, Z)
                <div className="flex gap-1.5">
                  <input value={x2} onChange={(e) => setX2(e.target.value)} placeholder="X" className={inputClass + ' w-full'} />
                  <input value={y2} onChange={(e) => setY2(e.target.value)} placeholder="Y" className={inputClass + ' w-full'} />
                  <input value={z2} onChange={(e) => setZ2(e.target.value)} placeholder="Z" className={inputClass + ' w-full'} />
                </div>
              </label>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="btn-pop mt-1 flex items-center justify-center gap-1.5 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
            >
              <Lock size={13} strokeWidth={2} />
              Create jail
            </button>
          </form>
        </Card>
      </div>
    </DashboardLayout>
  );
}
