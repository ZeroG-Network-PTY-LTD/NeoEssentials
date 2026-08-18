import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { Hologram, HologramStats } from '../types';
import { Sparkles, PlusCircle, Eye, EyeOff, Pencil, Trash2, X } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Holograms.tsx — useForm()/router.*()
 * became plain useState + mcApi calls. The animation knobs (spin/hover/billboard/per-line
 * frames) aren't editable here either, same as the Laravel version — round-tripped as-is on
 * an edit so existing values survive, per that page's own original design note. */

const emptyForm = {
  id: '',
  world: 'minecraft:overworld',
  x: '',
  y: '',
  z: '',
  visible: true,
  lines: ['Line 1'],
};

export default function Holograms() {
  const { showToast } = useToast();
  const [holograms, setHolograms] = useState<Hologram[]>([]);
  const [stats, setStats] = useState<HologramStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const refresh = () => {
    Promise.all([mcApi.holograms(), mcApi.hologramStats()])
      .then(([h, s]) => {
        setHolograms(h);
        setStats(s);
      })
      .finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const startCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const startEdit = (h: Hologram) => {
    setEditingId(h.id);
    setForm({
      id: h.id,
      world: h.world,
      x: String(h.x),
      y: String(h.y),
      z: String(h.z),
      visible: h.visible,
      lines: h.lines && h.lines.length > 0 ? h.lines.map((l) => l.text) : ['Line 1'],
    });
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingId && !form.id.trim()) return;
    // Number('') is 0, not NaN — without this check, tabbing past a blank X/Y/Z field
    // silently creates the hologram at (0,0,0) instead of erroring or prompting.
    if (form.x.trim() === '' || form.y.trim() === '' || form.z.trim() === '') {
      showToast('X, Y, and Z coordinates are required.', true);
      return;
    }
    // createHologram() overwrites any existing hologram with the same id in place (the mod
    // now despawns the old entity first — fixed earlier this session — so nothing orphans),
    // but from the user's side that's still silent data loss: an existing hologram's
    // position/text/visibility just vanishes with no warning, unlike every delete action on
    // this page which confirms first.
    if (!editingId && holograms.some((h) => h.id === form.id.trim())) {
      if (!confirm(`A hologram named '${form.id.trim()}' already exists. Replace it?`)) return;
    }
    setSubmitting(true);
    // The mod deserializes `lines` straight into a List<HologramLine> (Gson), so each entry
    // must be an object with at least a `text` field — a bare string 400s with a
    // "BEGIN_OBJECT but was STRING" Gson error. Found live against a real dev server.
    const payload = {
      world: form.world,
      x: Number(form.x),
      y: Number(form.y),
      z: Number(form.z),
      visible: form.visible,
      lines: form.lines.map((text) => ({ text })),
    };
    try {
      if (editingId) {
        await mcApi.updateHologram(editingId, payload);
        showToast(`Updated hologram '${editingId}'.`);
      } else {
        await mcApi.createHologram({ id: form.id, ...payload });
        showToast(`Created hologram '${form.id}'.`);
      }
      startCreate();
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Save failed.', true);
    } finally {
      setSubmitting(false);
    }
  };

  const destroy = async (id: string) => {
    if (!confirm(`Delete hologram '${id}'?`)) return;
    try {
      await mcApi.deleteHologram(id);
      showToast(`Deleted hologram '${id}'.`);
      if (editingId === id) startCreate();
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Delete failed.', true);
    }
  };

  const toggleVisible = async (id: string) => {
    try {
      await mcApi.toggleHologramVisibility(id);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Toggle failed.', true);
    }
  };

  const setLine = (i: number, text: string) => {
    const lines = [...form.lines];
    lines[i] = text;
    setForm({ ...form, lines });
  };

  const addLine = () => setForm({ ...form, lines: [...form.lines, ''] });
  const removeLine = (i: number) => setForm({ ...form, lines: form.lines.filter((_, idx) => idx !== i) });

  if (loading || !stats) {
    return (
      <DashboardLayout>
        <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <PageHeading
        title="Holograms"
        icon={Sparkles}
        subtitle={`${stats.total} total · ${stats.visible} visible · ${stats.animated} animated · ${stats.shopHolograms} shop`}
      />

      <div className="grid grid-cols-[1fr_340px] gap-5">
        <Card title={`${holograms.length} hologram${holograms.length === 1 ? '' : 's'}`} icon={Sparkles}>
          {holograms.length === 0 && <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No holograms yet.</div>}
          {holograms.map((h) => (
            <div
              key={h.id}
              className="flex items-center px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
            >
              <span className="flex-1 font-medium">{h.id}</span>
              <span className="font-data text-[12px] text-[var(--mc-text-muted)] mr-3">
                {(h.world ?? '').replace('minecraft:', '')} · {Math.round(h.x)}, {Math.round(h.y)}, {Math.round(h.z)}
              </span>
              <Badge variant={h.visible ? 'moss' : 'neutral'} dot={h.visible} className="mr-3">
                {h.visible ? 'visible' : 'hidden'}
              </Badge>
              <button
                onClick={() => toggleVisible(h.id)}
                className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] mr-2 transition-colors hover:bg-[var(--mc-bg-surface)]"
              >
                {h.visible ? <EyeOff size={12} strokeWidth={2} /> : <Eye size={12} strokeWidth={2} />}
                Toggle
              </button>
              <button
                onClick={() => startEdit(h)}
                className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] mr-2 transition-colors hover:bg-[var(--mc-bg-surface)]"
              >
                <Pencil size={12} strokeWidth={2} />
                Edit
              </button>
              <button
                onClick={() => destroy(h.id)}
                className="btn-pop flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
              >
                <Trash2 size={12} strokeWidth={2} />
                Delete
              </button>
            </div>
          ))}
        </Card>

        <Card
          title={editingId ? `Edit '${editingId}'` : 'Create hologram'}
          icon={editingId ? Pencil : PlusCircle}
          accent="purple"
          padded
          className="h-fit"
        >
          <form onSubmit={submit} className="flex flex-col gap-3">
            {!editingId && (
              <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                ID
                <input
                  value={form.id}
                  onChange={(e) => setForm({ ...form, id: e.target.value })}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
              </label>
            )}

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              World
              <input
                value={form.world}
                onChange={(e) => setForm({ ...form, world: e.target.value })}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <div className="grid grid-cols-3 gap-2">
              {(['x', 'y', 'z'] as const).map((axis) => (
                <label key={axis} className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                  {axis.toUpperCase()}
                  <input
                    type="number"
                    value={form[axis]}
                    onChange={(e) => setForm({ ...form, [axis]: e.target.value })}
                    className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                  />
                </label>
              ))}
            </div>

            <label className="flex items-center gap-2 text-[12px] text-[var(--mc-text-secondary)]">
              <input
                type="checkbox"
                checked={form.visible}
                onChange={(e) => setForm({ ...form, visible: e.target.checked })}
                className="accent-[var(--mc-cyan-500)]"
              />
              Visible
            </label>

            <div className="flex flex-col gap-1.5">
              <span className="text-[12px] text-[var(--mc-text-secondary)]">Lines</span>
              {form.lines.map((line, i) => (
                <div key={i} className="flex gap-1.5">
                  <input
                    value={line}
                    onChange={(e) => setLine(i, e.target.value)}
                    className="flex-1 font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                  />
                  {form.lines.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeLine(i)}
                      className="px-2 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] text-[12px] transition-colors hover:bg-[var(--mc-bg-surface)]"
                    >
                      <X size={12} strokeWidth={2} />
                    </button>
                  )}
                </div>
              ))}
              <button
                type="button"
                onClick={addLine}
                className="self-start flex items-center gap-1 text-[12px] text-[var(--mc-cyan-500)] transition-colors hover:text-[var(--mc-cyan-400)]"
              >
                <PlusCircle size={12} strokeWidth={2} />
                Add line
              </button>
            </div>

            <div className="flex gap-2 mt-1">
              <button
                type="submit"
                disabled={submitting}
                className="btn-pop flex-1 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] transition-colors disabled:opacity-50"
              >
                {editingId ? 'Save' : 'Create'}
              </button>
              {editingId && (
                <button
                  type="button"
                  onClick={startCreate}
                  className="text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]"
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </Card>
      </div>
    </DashboardLayout>
  );
}
