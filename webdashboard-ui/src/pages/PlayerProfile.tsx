import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { Gamemode } from '../lib/mcApi';
import type {
  BanEntry,
  KickEntry,
  MuteEntry,
  NoteEntry,
  PermissionGroup,
  PermissionUserLookupResult,
  PlayerInventory,
  PlayerLookupResult,
  WarnEntry,
} from '../types';
import {
  ArrowLeft,
  Gamepad2,
  UserCog,
  Coins,
  ShieldBan,
  VolumeX,
  LogOut,
  StickyNote,
  Backpack,
  Shield,
  Plus,
  X,
} from 'lucide-react';

export default function PlayerProfile() {
  const { username = '' } = useParams<{ username: string }>();
  const { showToast } = useToast();

  const [lookup, setLookup] = useState<PlayerLookupResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [balance, setBalance] = useState<string | null>(null);
  const [permInfo, setPermInfo] = useState<PermissionUserLookupResult | null>(null);
  const [groups, setGroups] = useState<PermissionGroup[]>([]);
  const [inventory, setInventory] = useState<PlayerInventory | null>(null);

  const [bans, setBans] = useState<BanEntry[]>([]);
  const [mutes, setMutes] = useState<MuteEntry[]>([]);
  const [kicks, setKicks] = useState<KickEntry[]>([]);
  const [warns, setWarns] = useState<WarnEntry[]>([]);
  const [notes, setNotes] = useState<NoteEntry[]>([]);

  const [newPermission, setNewPermission] = useState('');
  const [newNote, setNewNote] = useState('');
  const [economyAmount, setEconomyAmount] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () => {
    setLoading(true);
    setNotFound(false);
    mcApi.lookupPlayer(username).then(async (result) => {
      setLookup(result);
      if (!result.success || !result.uuid) {
        setNotFound(true);
        setLoading(false);
        return;
      }
      const uuid = result.uuid;
      const [bal, perm, grp, inv, banList, muteList, kickList, warnList, noteList] = await Promise.allSettled([
        mcApi.getBalance(username),
        mcApi.permissionUserLookup(username),
        mcApi.permissionGroups(),
        mcApi.getInventory(username),
        mcApi.banHistory(uuid),
        mcApi.muteHistory(username),
        mcApi.kickHistory(username),
        mcApi.warnsForPlayer(username),
        mcApi.notesForPlayer(username),
      ]);
      if (bal.status === 'fulfilled') setBalance(bal.value.balance);
      if (perm.status === 'fulfilled') setPermInfo(perm.value);
      if (grp.status === 'fulfilled') setGroups(grp.value);
      if (inv.status === 'fulfilled') setInventory(inv.value);
      if (banList.status === 'fulfilled') setBans(banList.value);
      if (muteList.status === 'fulfilled') setMutes(muteList.value);
      if (kickList.status === 'fulfilled') setKicks(kickList.value);
      if (warnList.status === 'fulfilled') setWarns(warnList.value);
      if (noteList.status === 'fulfilled') setNotes(noteList.value);
      setLoading(false);
    });
  };

  useEffect(load, [username]);

  const runAction = async (label: string, fn: () => Promise<unknown>, after?: () => void) => {
    setBusy(true);
    try {
      await fn();
      showToast(label);
      after?.();
    } catch (err) {
      showToast(err instanceof Error ? err.message : `${label} failed.`, true);
    } finally {
      setBusy(false);
    }
  };

  const changeGroup = (group: string) =>
    runAction(`Group set to '${group}'.`, () => mcApi.setUserGroup(username, group), () =>
      setPermInfo((p) => (p ? { ...p, group } : p)),
    );

  const changeGamemode = (gamemode: Gamemode) =>
    runAction(`Game mode set to ${gamemode}.`, () => mcApi.setGamemode(username, gamemode));

  const addPermission = (e: FormEvent) => {
    e.preventDefault();
    const perm = newPermission.trim();
    if (!perm) return;
    runAction(`Added permission '${perm}'.`, () => mcApi.addUserPermission(username, perm), () => {
      setNewPermission('');
      setPermInfo((p) => (p ? { ...p, permissions: [...(p.permissions ?? []), perm] } : p));
    });
  };

  const removePermission = (perm: string) =>
    runAction(`Removed permission '${perm}'.`, () => mcApi.removeUserPermission(username, perm), () =>
      setPermInfo((p) => (p ? { ...p, permissions: (p.permissions ?? []).filter((x) => x !== perm) } : p)),
    );

  const adjustBalance = (action: 'give' | 'take' | 'set') => {
    const amount = Number(economyAmount);
    if (!Number.isFinite(amount) || amount < 0) return;
    runAction(`Balance ${action === 'give' ? 'increased' : action === 'take' ? 'decreased' : 'set'}.`,
      () => mcApi.economyAdjust(username, action, amount),
      async () => {
        setEconomyAmount('');
        const bal = await mcApi.getBalance(username);
        setBalance(bal.balance);
      },
    );
  };

  const doUnban = (uuid: string) =>
    runAction('Ban lifted.', () => mcApi.unban(uuid), () =>
      setBans((b) => b.map((x) => (x.playerId === uuid ? { ...x, active: false } : x))),
    );

  const doUnmute = () =>
    runAction('Unmuted.', () => mcApi.unmute(username), () =>
      setMutes((m) => m.map((x) => ({ ...x, active: false }))),
    );

  const doRemoveWarn = (id: string) =>
    runAction('Warning removed.', () => mcApi.removeWarn(id, username), () =>
      setWarns((w) => w.filter((x) => x.id !== id)),
    );

  const addNote = (e: FormEvent) => {
    e.preventDefault();
    const text = newNote.trim();
    if (!text) return;
    runAction('Note added.', () => mcApi.createNote(username, text), () => {
      setNewNote('');
      load();
    });
  };

  const doRemoveNote = (id: string) =>
    runAction('Note removed.', () => mcApi.removeNote(id, username), () =>
      setNotes((n) => n.filter((x) => x.id !== id)),
    );

  if (loading) {
    return (
      <DashboardLayout>
        <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>
      </DashboardLayout>
    );
  }

  if (notFound || !lookup) {
    return (
      <DashboardLayout>
        <PageHeading title={username} icon={UserCog} />
        <div className="text-[13px] text-[var(--mc-ember-500)]">
          {lookup?.message ?? `Could not find a player named '${username}'.`}
        </div>
        <Link to="/players" className="mt-3 inline-block text-[13px] text-[var(--mc-cyan-400)]">
          <ArrowLeft size={13} className="inline -mt-0.5 mr-1" /> Back to players
        </Link>
      </DashboardLayout>
    );
  }

  const activeBans = bans.filter((b) => b.active);
  const activeMutes = mutes.filter((m) => m.active);
  const inventorySlots = [
    ...(inventory?.main ?? []).filter((i) => i.id),
    ...(inventory?.armor ?? []).filter((i) => i.id),
    ...(inventory?.offhand ?? []).filter((i) => i.id),
  ];

  return (
    <DashboardLayout>
      <PageHeading
        title={lookup.username ?? username}
        icon={UserCog}
        subtitle="Full single-player control — moderation history, economy, permissions, and inventory."
        action={
          <Link
            to="/players"
            className="flex items-center gap-1.5 text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
          >
            <ArrowLeft size={13} />
            Back to players
          </Link>
        }
      />

      <div className="flex items-center gap-3 mb-6">
        <img
          src={`https://mc-heads.net/avatar/${lookup.uuid}/48`}
          alt=""
          className="h-11 w-11 rounded-[9px] [image-rendering:pixelated] border border-[var(--mc-border-strong)]"
        />
        <div>
          <div className="font-display text-[16px] font-semibold flex items-center gap-2">
            {lookup.username}
            <Badge variant={lookup.online ? 'moss' : 'neutral'} dot={lookup.online}>
              {lookup.online ? 'online' : 'offline'}
            </Badge>
            {activeBans.length > 0 && <Badge variant="ember">banned</Badge>}
            {activeMutes.length > 0 && <Badge variant="purple">muted</Badge>}
          </div>
          <div className="font-data text-[12px] text-[var(--mc-text-muted)]">
            {lookup.uuid}
            {!lookup.online && lookup.lastSeen && ` · Last seen ${lookup.lastSeen}`}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card title="Quick actions" icon={Shield} padded>
          <div className="flex flex-wrap gap-2 mb-3">
            <button
              disabled={busy}
              onClick={() => runAction('Healed and fed.', () => mcApi.healPlayer(username))}
              className="text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors"
            >
              Heal
            </button>
            <button
              disabled={busy}
              onClick={() =>
                runAction('Kicked.', () => mcApi.kickPlayer(username, 'Kicked from dashboard'), load)
              }
              className="flex items-center gap-1 text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors"
            >
              <LogOut size={12} /> Kick
            </button>
            {activeMutes.length > 0 ? (
              <button
                disabled={busy}
                onClick={doUnmute}
                className="flex items-center gap-1 text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors"
              >
                <VolumeX size={12} /> Unmute
              </button>
            ) : (
              <button
                disabled={busy}
                onClick={() => runAction('Muted.', () => mcApi.mutePlayer(username), load)}
                className="flex items-center gap-1 text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors"
              >
                <VolumeX size={12} /> Mute
              </button>
            )}
            {activeBans.length === 0 && (
              <button
                disabled={busy}
                onClick={() => runAction('Banned.', () => mcApi.banPlayer(username, 'Banned from dashboard'), load)}
                className="flex items-center gap-1 text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-ember-400)] text-[var(--mc-ember-500)] hover:bg-[var(--mc-ember-50)] disabled:opacity-50 transition-colors"
              >
                <ShieldBan size={12} /> Ban
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 text-[11px] text-[var(--mc-text-muted)] mb-1.5">
            <Gamepad2 size={13} />
            Game mode
          </div>
          <div className="grid grid-cols-4 gap-1.5 mb-3">
            {(['survival', 'creative', 'adventure', 'spectator'] as Gamemode[]).map((gm) => (
              <button
                key={gm}
                disabled={busy}
                onClick={() => changeGamemode(gm)}
                className="text-[11px] px-2 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] capitalize disabled:opacity-50 transition-colors"
              >
                {gm}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2 text-[11px] text-[var(--mc-text-muted)] mb-1.5">
            <UserCog size={13} />
            Permission group
          </div>
          <select
            value={permInfo?.group ?? ''}
            disabled={busy || groups.length === 0}
            onChange={(e) => changeGroup(e.target.value)}
            className="w-full text-[13px] px-2.5 py-1.5 rounded-[6px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] outline-none focus:border-[var(--mc-cyan-400)] disabled:opacity-50"
          >
            <option value="" disabled>
              {groups.length === 0 ? 'Loading groups…' : 'Select group'}
            </option>
            {groups.map((g) => (
              <option key={g.name} value={g.name}>{g.name}</option>
            ))}
          </select>
        </Card>

        <Card title="Economy" icon={Coins} padded>
          <div className="text-[20px] font-display font-semibold mb-3">
            {balance !== null ? `$${balance}` : '…'}
          </div>
          <div className="flex gap-1.5">
            <input
              type="number"
              min="0"
              value={economyAmount}
              onChange={(e) => setEconomyAmount(e.target.value)}
              placeholder="Amount"
              className="flex-1 font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[6px] px-2.5 py-1.5 outline-none focus:border-[var(--mc-cyan-400)]"
            />
            <button disabled={busy} onClick={() => adjustBalance('give')} className="text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors">Give</button>
            <button disabled={busy} onClick={() => adjustBalance('take')} className="text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors">Take</button>
            <button disabled={busy} onClick={() => adjustBalance('set')} className="text-[12px] px-2.5 py-1.5 rounded-[6px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] disabled:opacity-50 transition-colors">Set</button>
          </div>
        </Card>

        <Card title="Individual permissions" icon={Shield} padded>
          <form onSubmit={addPermission} className="flex gap-1.5 mb-3">
            <input
              value={newPermission}
              onChange={(e) => setNewPermission(e.target.value)}
              placeholder="neoessentials.some.permission"
              className="flex-1 font-data text-[12px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[6px] px-2.5 py-1.5 outline-none focus:border-[var(--mc-cyan-400)]"
            />
            <button type="submit" disabled={busy} className="flex items-center gap-1 text-[12px] px-2.5 py-1.5 rounded-[6px] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] disabled:opacity-50 transition-colors">
              <Plus size={12} /> Add
            </button>
          </form>
          <div className="flex flex-col gap-1 max-h-48 overflow-y-auto">
            {(permInfo?.permissions ?? []).length === 0 && (
              <div className="text-[12px] text-[var(--mc-text-muted)]">No individual permission overrides — only the group's permissions apply.</div>
            )}
            {(permInfo?.permissions ?? []).map((perm) => (
              <div key={perm} className="flex items-center justify-between font-data text-[12px] px-2 py-1 rounded-[5px] bg-[var(--mc-bg-surface-raised)]">
                <span className="break-all">{perm}</span>
                <button disabled={busy} onClick={() => removePermission(perm)} className="text-[var(--mc-text-muted)] hover:text-[var(--mc-ember-500)] shrink-0 ml-2">
                  <X size={13} />
                </button>
              </div>
            ))}
          </div>
        </Card>

        <Card title="Inventory" icon={Backpack} padded>
          {inventory?.error && <div className="text-[12px] text-[var(--mc-text-muted)]">{inventory.error}</div>}
          {!inventory?.error && inventorySlots.length === 0 && (
            <div className="text-[12px] text-[var(--mc-text-muted)]">No items, or inventory data isn't available for this player.</div>
          )}
          {inventorySlots.length > 0 && (
            <div className="flex flex-col gap-1 max-h-48 overflow-y-auto font-data text-[12px]">
              {inventorySlots.map((item, i) => (
                <div key={i} className="flex items-center justify-between px-2 py-1 rounded-[5px] bg-[var(--mc-bg-surface-raised)]">
                  <span>{item.displayName ?? item.id}</span>
                  <span className="text-[var(--mc-text-muted)]">×{item.count ?? 1}</span>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card title={`Moderation history (${bans.length + mutes.length + kicks.length + warns.length})`} icon={ShieldBan} padded>
          <div className="flex flex-col gap-3 max-h-72 overflow-y-auto text-[12px]">
            {bans.map((b) => (
              <div key={b.id} className="flex items-center justify-between px-2 py-1.5 rounded-[5px] bg-[var(--mc-bg-surface-raised)]">
                <div>
                  <span className={b.active ? 'text-[var(--mc-ember-500)]' : 'text-[var(--mc-text-muted)]'}>
                    {b.active ? 'Banned' : 'Ban (lifted)'}
                  </span>
                  {': '}{b.reason} <span className="text-[var(--mc-text-muted)]">— by {b.bannedBy}</span>
                </div>
                {b.active && (
                  <button disabled={busy} onClick={() => doUnban(b.playerId)} className="text-[11px] px-2 py-1 rounded-[5px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface)] shrink-0 ml-2 transition-colors">
                    Unban
                  </button>
                )}
              </div>
            ))}
            {mutes.map((m) => (
              <div key={m.id} className="px-2 py-1.5 rounded-[5px] bg-[var(--mc-bg-surface-raised)]">
                <span className={m.active ? 'text-[var(--mc-purple-400)]' : 'text-[var(--mc-text-muted)]'}>
                  {m.active ? 'Muted' : 'Mute (lifted)'}
                </span>
                {m.reason ? `: ${m.reason}` : ''} <span className="text-[var(--mc-text-muted)]">— by {m.mutedBy}</span>
              </div>
            ))}
            {kicks.map((k) => (
              <div key={k.id} className="px-2 py-1.5 rounded-[5px] bg-[var(--mc-bg-surface-raised)]">
                Kicked: {k.reason} <span className="text-[var(--mc-text-muted)]">— by {k.kickedBy}</span>
              </div>
            ))}
            {warns.map((w) => (
              <div key={w.id} className="flex items-center justify-between px-2 py-1.5 rounded-[5px] bg-[var(--mc-bg-surface-raised)]">
                <div>Warned: {w.reason} <span className="text-[var(--mc-text-muted)]">— by {w.warnedBy}</span></div>
                <button disabled={busy} onClick={() => doRemoveWarn(w.id)} className="text-[11px] px-2 py-1 rounded-[5px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface)] shrink-0 ml-2 transition-colors">
                  Remove
                </button>
              </div>
            ))}
            {bans.length + mutes.length + kicks.length + warns.length === 0 && (
              <div className="text-[var(--mc-text-muted)]">No moderation history on file.</div>
            )}
          </div>
        </Card>

        <Card title="Notes" icon={StickyNote} padded>
          <form onSubmit={addNote} className="flex gap-1.5 mb-3">
            <input
              value={newNote}
              onChange={(e) => setNewNote(e.target.value)}
              placeholder="Add a private admin note…"
              className="flex-1 text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[6px] px-2.5 py-1.5 outline-none focus:border-[var(--mc-cyan-400)]"
            />
            <button type="submit" disabled={busy} className="text-[12px] px-2.5 py-1.5 rounded-[6px] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] disabled:opacity-50 transition-colors">
              Add
            </button>
          </form>
          <div className="flex flex-col gap-1.5 max-h-48 overflow-y-auto">
            {notes.length === 0 && <div className="text-[12px] text-[var(--mc-text-muted)]">No notes yet.</div>}
            {notes.map((n) => (
              <div key={n.id} className="flex items-start justify-between text-[12px] px-2 py-1.5 rounded-[5px] bg-[var(--mc-bg-surface-raised)]">
                <div>
                  <div>{n.text}</div>
                  <div className="text-[var(--mc-text-muted)] mt-0.5">— {n.authorName}</div>
                </div>
                <button disabled={busy} onClick={() => doRemoveNote(n.id)} className="text-[var(--mc-text-muted)] hover:text-[var(--mc-ember-500)] shrink-0 ml-2">
                  <X size={13} />
                </button>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
}
