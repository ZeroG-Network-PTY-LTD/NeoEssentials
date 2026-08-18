import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { PlayerWarpGroup } from '../lib/mcApi';
import type { Warp } from '../types';
import { MapPin, PlusCircle, Trash2, ChevronDown, ChevronRight, Users } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Warps.tsx — useForm()/router.delete()
 * became plain useState + mcApi calls; window.confirm() kept as-is (no framework dependency). */

export default function Warps() {
  const { showToast } = useToast();
  const [tab, setTab] = useState<'server' | 'player'>('server');
  const [warps, setWarps] = useState<Warp[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [world, setWorld] = useState('minecraft:overworld');
  const [x, setX] = useState('');
  const [y, setY] = useState('');
  const [z, setZ] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [playerWarps, setPlayerWarps] = useState<PlayerWarpGroup[]>([]);
  const [playerWarpsLoaded, setPlayerWarpsLoaded] = useState(false);
  const [playerWarpsLoading, setPlayerWarpsLoading] = useState(false);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const refresh = () => {
    mcApi.warps().then(setWarps).finally(() => setLoading(false));
  };

  const refreshPlayerWarps = () => {
    setPlayerWarpsLoading(true);
    mcApi
      .playerWarps()
      .then(setPlayerWarps)
      .finally(() => {
        setPlayerWarpsLoading(false);
        setPlayerWarpsLoaded(true);
      });
  };

  useEffect(refresh, []);

  useEffect(() => {
    if (tab === 'player' && !playerWarpsLoaded) refreshPlayerWarps();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  const toggleExpanded = (uuid: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(uuid)) next.delete(uuid);
      else next.add(uuid);
      return next;
    });
  };

  const destroyPlayerWarp = async (uuid: string, warpName: string, ownerName: string) => {
    if (!confirm(`Delete ${ownerName}'s warp '${warpName}'?`)) return;
    try {
      await mcApi.deletePlayerWarp(uuid, warpName);
      showToast(`Deleted ${ownerName}'s warp '${warpName}'.`);
      refreshPlayerWarps();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Could not delete player warp.', true);
    }
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    // Number('') is 0, not NaN — without this check, tabbing past a blank X/Y/Z field
    // silently creates the warp at (0,0,0) instead of erroring or prompting.
    if (x.trim() === '' || y.trim() === '' || z.trim() === '') {
      showToast('X, Y, and Z coordinates are required.', true);
      return;
    }
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

  const tabButtonClass = (active: boolean) =>
    `btn-pop flex items-center gap-1.5 text-[12px] px-3 py-1.5 rounded-[var(--radius)] border transition-colors ${
      active
        ? 'bg-[var(--mc-cyan-500)] text-[#0a1620] border-[var(--mc-cyan-500)] font-medium'
        : 'bg-transparent text-[var(--mc-text-secondary)] border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)]'
    }`;

  return (
    <DashboardLayout>
      <PageHeading
        title="Warps"
        icon={MapPin}
        count={tab === 'server' ? warps.length : playerWarps.length}
        subtitle="Named teleport points on the server."
      />

      <div className="flex gap-2 mb-4">
        <button className={tabButtonClass(tab === 'server')} onClick={() => setTab('server')}>
          <MapPin size={12} strokeWidth={2} />
          Server Warps
        </button>
        <button className={tabButtonClass(tab === 'player')} onClick={() => setTab('player')}>
          <Users size={12} strokeWidth={2} />
          Player Warps
        </button>
      </div>

      {tab === 'server' ? (
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
      ) : (
        <Card title={`${playerWarps.length} player${playerWarps.length === 1 ? '' : 's'} with warps`} icon={Users}>
          {playerWarpsLoading && <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">Loading…</div>}
          {!playerWarpsLoading && playerWarps.length === 0 && (
            <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No players have created warps yet.</div>
          )}
          {!playerWarpsLoading &&
            playerWarps.map((group) => {
              const isOpen = expanded.has(group.uuid);
              return (
                <div key={group.uuid} className="border-b border-[var(--mc-border)] last:border-0">
                  <button
                    onClick={() => toggleExpanded(group.uuid)}
                    className="w-full flex items-center px-4 py-2.5 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
                  >
                    {isOpen ? <ChevronDown size={14} strokeWidth={2} /> : <ChevronRight size={14} strokeWidth={2} />}
                    <span className="flex-1 text-left font-medium ml-2">{group.name}</span>
                    <span className="text-[12px] text-[var(--mc-text-muted)]">
                      {group.warpCount} warp{group.warpCount === 1 ? '' : 's'}
                    </span>
                  </button>

                  {isOpen && (
                    <div className="pb-1">
                      {group.warps.map((warp) => (
                        <div
                          key={warp.name}
                          className="flex items-center pl-10 pr-4 py-2 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
                        >
                          <span className="flex-1 font-medium">{warp.name}</span>
                          <span className="font-data text-[12px] text-[var(--mc-text-muted)] mr-3">
                            {warp.world.replace('minecraft:', '')} · {Math.round(warp.x)}, {Math.round(warp.y)}, {Math.round(warp.z)}
                          </span>
                          <span className="text-[12px] text-[var(--mc-text-muted)] mr-3">
                            {new Date(warp.timestamp).toLocaleDateString()}
                          </span>
                          <button
                            onClick={() => destroyPlayerWarp(group.uuid, warp.name, group.name)}
                            className="btn-pop flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
                          >
                            <Trash2 size={12} strokeWidth={2} />
                            Delete
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
        </Card>
      )}
    </DashboardLayout>
  );
}
