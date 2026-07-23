import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useAuth } from '../lib/auth';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { PermissionGroup, PermissionOverview, PermissionUser, PermissionUserLookupResult, PermissionNodeCategory } from '../types';
import { ShieldCheck, Users, UserCog, Link2, Plus, RefreshCw, Search, Key, ChevronDown } from 'lucide-react';

/**
 * Ported from the external dashboard's Pages/Dashboard/Permissions.tsx — the largest page in
 * scope (~20+ Inertia call sites). `router.get/post/put/delete()`/`useForm()` all became plain
 * mcApi calls followed by a shared `refresh()` (re-fetches overview/groups/users/aliases —
 * there's no page-prop partial-reload equivalent here, so every mutation just re-pulls
 * everything, same cost as Inertia's own `only: [...]` partial reload in practice since these
 * are all cheap mod-side reads). `usePage().props.auth` became `useAuth().isAdmin`. Toasts are
 * only shown for the "big" explicit actions (create/delete/rename group, create/delete alias,
 * reload) — the frequent inline edits (prefix/suffix/priority-on-blur, permission add/remove,
 * group assignment) only toast on failure, matching the original's low-noise feel (those never
 * flashed a message in the Laravel version either).
 */

function NodeInput({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
  return (
    <input
      list="permission-node-catalog"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="flex-1 font-data text-[12.5px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
    />
  );
}

/**
 * Renders a flat node list clustered under the nodeCatalog's category headings — anything not
 * found in the catalog (a node from an addon plugin, a typo, ...) still shows under "Other"
 * rather than being silently dropped. Much easier to scan than one long wrapped line of raw
 * strings once a group/user has more than a handful of nodes.
 */
function PermissionPills({
  permissions,
  catalog,
  onRemove,
}: {
  permissions: string[];
  catalog: PermissionNodeCategory[];
  onRemove?: (node: string) => void;
}) {
  if (permissions.length === 0) {
    return <p className="text-[12px] text-[var(--mc-text-muted)] italic">No individual permissions set.</p>;
  }

  const nodeInfo = new Map<string, { description: string; category: string }>();
  catalog.forEach((cat) => {
    cat.permissions.forEach((p) => {
      nodeInfo.set(p.node, { description: p.description, category: cat.category });
    });
  });

  const grouped = new Map<string, string[]>();
  permissions.forEach((p) => {
    const category = nodeInfo.get(p)?.category ?? 'Other';
    if (!grouped.has(category)) grouped.set(category, []);
    grouped.get(category)!.push(p);
  });

  return (
    <div className="flex flex-col gap-2.5">
      {Array.from(grouped.entries()).map(([category, nodes]) => (
        <div key={category}>
          <div className="text-[10.5px] font-semibold uppercase tracking-wide text-[var(--mc-text-muted)] mb-1.5">
            {category}
          </div>
          <div className="flex flex-wrap gap-1.5">
            {nodes.map((p) => (
              <span
                key={p}
                title={nodeInfo.get(p)?.description}
                className="font-data text-[12px] pl-2.5 pr-1.5 py-1 rounded-full bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border)] flex items-center gap-1.5"
              >
                {p}
                {onRemove && (
                  <button
                    onClick={() => onRemove(p)}
                    className="flex h-4 w-4 items-center justify-center rounded-full text-[var(--mc-ember-500)] transition-colors hover:bg-[var(--mc-ember-50)] hover:text-[var(--mc-ember-400)]"
                  >
                    &times;
                  </button>
                )}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function Chevron({ open }: { open: boolean }) {
  return <ChevronDown size={15} className={`shrink-0 text-[var(--mc-text-muted)] transition-transform ${open ? 'rotate-180' : ''}`} />;
}

export default function Permissions() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();

  const [overview, setOverview] = useState<PermissionOverview | null>(null);
  const [groups, setGroups] = useState<PermissionGroup[]>([]);
  const [users, setUsers] = useState<PermissionUser[]>([]);
  const [aliases, setAliases] = useState<Record<string, string>>({});
  const [nodeCatalog, setNodeCatalog] = useState<PermissionNodeCategory[]>([]);
  const [loading, setLoading] = useState(true);

  const [newPerm, setNewPerm] = useState<Record<string, string>>({});
  const [renaming, setRenaming] = useState<Record<string, string>>({});
  const [editingInherits, setEditingInherits] = useState<Record<string, boolean>>({});
  const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});
  const [expandedUsers, setExpandedUsers] = useState<Record<string, boolean>>({});
  const [lookupInput, setLookupInput] = useState('');
  const [lookupQuery, setLookupQuery] = useState<string | null>(null);
  const [lookupResult, setLookupResult] = useState<PermissionUserLookupResult | null>(null);

  const [groupName, setGroupName] = useState('');
  const [groupPrefix, setGroupPrefix] = useState('');
  const [groupSuffix, setGroupSuffix] = useState('');
  const [groupIsDefault, setGroupIsDefault] = useState(false);
  const [groupPriority, setGroupPriority] = useState(0);
  const [groupSubmitting, setGroupSubmitting] = useState(false);

  const [aliasName, setAliasName] = useState('');
  const [aliasCanonical, setAliasCanonical] = useState('');
  const [aliasSubmitting, setAliasSubmitting] = useState(false);

  const refresh = () => {
    Promise.all([mcApi.permissionOverview(), mcApi.permissionGroups(), mcApi.permissionUsers(), mcApi.permissionAliases(), mcApi.permissionNodeCatalog()])
      .then(([o, g, u, a, cat]) => {
        setOverview(o);
        setGroups(g);
        setUsers(u);
        setAliases(a);
        setNodeCatalog(cat);
      })
      .finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const withErrorToast = async (action: () => Promise<unknown>, fallback: string) => {
    try {
      await action();
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : fallback, true);
    }
  };

  const createGroup = async (e: FormEvent) => {
    e.preventDefault();
    if (!groupName.trim()) return;
    setGroupSubmitting(true);
    try {
      await mcApi.createPermissionGroup(groupName.trim(), groupPrefix, groupSuffix, groupIsDefault, groupPriority);
      showToast(`Created group '${groupName.trim()}'.`);
      setGroupName('');
      setGroupPrefix('');
      setGroupSuffix('');
      setGroupIsDefault(false);
      setGroupPriority(0);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Create failed.', true);
    } finally {
      setGroupSubmitting(false);
    }
  };

  const deleteGroup = async (name: string) => {
    if (!confirm(`Delete group '${name}'?`)) return;
    try {
      await mcApi.deletePermissionGroup(name);
      showToast(`Deleted group '${name}'.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Delete failed.', true);
    }
  };

  const toggleGroupExpanded = (name: string) => setExpandedGroups((s) => ({ ...s, [name]: !s[name] }));
  const toggleUserExpanded = (username: string) => setExpandedUsers((s) => ({ ...s, [username]: !s[username] }));

  const startRename = (name: string) => setRenaming((s) => ({ ...s, [name]: name }));
  const cancelRename = (name: string) =>
    setRenaming((s) => {
      const next = { ...s };
      delete next[name];
      return next;
    });
  const submitRename = async (name: string) => {
    const newName = (renaming[name] ?? '').trim();
    if (!newName || newName === name) {
      cancelRename(name);
      return;
    }
    try {
      await mcApi.renamePermissionGroup(name, newName);
      showToast(`Renamed to '${newName}'.`);
      cancelRename(name);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Rename failed.', true);
    }
  };

  const setGroupField = (name: string, field: 'prefix' | 'suffix' | 'priority' | 'isDefault', value: string | number | boolean) =>
    withErrorToast(() => mcApi.updatePermissionGroup(name, { [field]: value }), 'Update failed.');

  const toggleInherit = (group: PermissionGroup, parent: string) => {
    const current = new Set(group.inherits);
    if (current.has(parent)) current.delete(parent);
    else current.add(parent);
    return withErrorToast(() => mcApi.updatePermissionGroup(group.name, { inherits: Array.from(current) }), 'Update failed.');
  };

  const addGroupPermission = async (group: string) => {
    const permission = newPerm[`group:${group}`];
    if (!permission) return;
    try {
      await mcApi.addGroupPermission(group, permission);
      setNewPerm((s) => ({ ...s, [`group:${group}`]: '' }));
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Add failed.', true);
    }
  };

  const removeGroupPermission = (group: string, permission: string) =>
    withErrorToast(() => mcApi.removeGroupPermission(group, permission), 'Remove failed.');

  const addUserPermission = async (username: string) => {
    const permission = newPerm[`user:${username}`];
    if (!permission) return;
    try {
      await mcApi.addUserPermission(username, permission);
      setNewPerm((s) => ({ ...s, [`user:${username}`]: '' }));
      refresh();
      if (lookupResult?.username === username) {
        setLookupResult(await mcApi.permissionUserLookup(username));
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Add failed.', true);
    }
  };

  const removeUserPermission = async (username: string, permission: string) => {
    try {
      await mcApi.removeUserPermission(username, permission);
      refresh();
      if (lookupResult?.username === username) {
        setLookupResult(await mcApi.permissionUserLookup(username));
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Remove failed.', true);
    }
  };

  const setUserGroup = async (username: string, group: string) => {
    try {
      await mcApi.setUserGroup(username, group);
      refresh();
      if (lookupResult?.username === username) {
        setLookupResult(await mcApi.permissionUserLookup(username));
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Update failed.', true);
    }
  };

  const runLookup = async (e: FormEvent) => {
    e.preventDefault();
    const q = lookupInput.trim();
    if (!q) return;
    setLookupQuery(q);
    try {
      setLookupResult(await mcApi.permissionUserLookup(q));
    } catch (err) {
      setLookupResult({ success: false, message: err instanceof Error ? err.message : 'Lookup failed.' });
    }
  };

  const createAlias = async (e: FormEvent) => {
    e.preventDefault();
    if (!aliasName.trim() || !aliasCanonical.trim()) return;
    setAliasSubmitting(true);
    try {
      await mcApi.addPermissionAlias(aliasName.trim(), aliasCanonical.trim());
      showToast(`Added alias '${aliasName.trim()}'.`);
      setAliasName('');
      setAliasCanonical('');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Add failed.', true);
    } finally {
      setAliasSubmitting(false);
    }
  };

  const deleteAlias = async (alias: string) => {
    try {
      await mcApi.removePermissionAlias(alias);
      showToast(`Deleted alias '${alias}'.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Delete failed.', true);
    }
  };

  const reload = async () => {
    try {
      await mcApi.reloadPermissions();
      showToast('Reloaded permissions from disk.');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Reload failed.', true);
    }
  };

  if (loading || !overview) {
    return (
      <DashboardLayout>
        <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <datalist id="permission-node-catalog">
        {nodeCatalog.flatMap((cat) =>
          cat.permissions.map((p) => (
            <option key={p.node} value={p.node}>
              {p.description}
            </option>
          )),
        )}
      </datalist>

      <PageHeading
        title="Permissions"
        icon={ShieldCheck}
        subtitle={`${overview.systemType} · ${overview.totalGroups} groups · ${overview.totalUsers} online users${
          overview.usingExternal ? ' · management disabled while an external permission plugin is active' : ''
        }`}
        action={
          isAdmin &&
          !overview.usingExternal && (
            <button
              onClick={reload}
              className="flex items-center gap-1.5 text-[12px] px-2.5 py-1.5 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]"
            >
              <RefreshCw size={12} strokeWidth={2} />
              Reload
            </button>
          )
        }
      />

      {!overview.usingExternal && (
        <div className="flex flex-col gap-5">
          <div className="grid grid-cols-[1fr_320px] gap-5">
            <Card title="Groups" icon={Users} accent="cyan">
              {groups.map((g) => {
                const open = !!expandedGroups[g.name];
                return (
                  <div key={g.name} className="border-b border-[var(--mc-border)] last:border-0">
                    <button
                      type="button"
                      onClick={() => toggleGroupExpanded(g.name)}
                      className="w-full flex items-center gap-2.5 px-4 py-3 text-left text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
                    >
                      <Chevron open={open} />
                      <span className="font-medium">{g.name}</span>
                      {g.isDefault && <Badge variant="cyan">default</Badge>}
                      {(g.prefix || g.suffix) && (
                        <span className="font-data text-[12px] text-[var(--mc-text-muted)]">
                          {g.prefix}{g.suffix}
                        </span>
                      )}
                      <span className="ml-auto flex items-center gap-3 text-[12px] text-[var(--mc-text-muted)]">
                        <span>{(g.permissions ?? []).length} permission{(g.permissions ?? []).length === 1 ? '' : 's'}</span>
                        {isAdmin && <span>priority {g.priority}</span>}
                      </span>
                    </button>

                    {open && (
                      <div className="px-4 pb-4">
                        <div className="flex items-center gap-2 mb-3">
                          {renaming[g.name] !== undefined ? (
                            <>
                              <input
                                value={renaming[g.name]}
                                onChange={(e) => setRenaming((s) => ({ ...s, [g.name]: e.target.value }))}
                                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[6px] px-1.5 py-0.5 outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                                autoFocus
                              />
                              <button onClick={() => submitRename(g.name)} className="btn-pop text-[11px] px-2 py-0.5 rounded bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)]">
                                Save
                              </button>
                              <button onClick={() => cancelRename(g.name)} className="text-[11px] px-2 py-0.5 rounded bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]">
                                Cancel
                              </button>
                            </>
                          ) : (
                            isAdmin && (
                              <button onClick={() => startRename(g.name)} className="text-[11px] text-[var(--mc-text-muted)] underline transition-colors hover:text-[var(--mc-cyan-400)]">
                                rename group
                              </button>
                            )
                          )}
                          {isAdmin && (
                            <button
                              onClick={() => deleteGroup(g.name)}
                              className="ml-auto text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
                            >
                              Delete group
                            </button>
                          )}
                        </div>

                        {isAdmin && (
                          <div className="flex flex-wrap items-center gap-4 mb-3 p-3 rounded-[8px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border)] text-[12px] text-[var(--mc-text-secondary)]">
                            <label className="flex items-center gap-1.5">
                              Prefix
                              <input
                                defaultValue={g.prefix}
                                onBlur={(e) => {
                                  if (e.target.value !== g.prefix) setGroupField(g.name, 'prefix', e.target.value);
                                }}
                                className="w-20 font-data text-[12px] bg-[var(--mc-bg-surface)] border border-[var(--mc-border-strong)] rounded-[6px] px-1.5 py-0.5 outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                              />
                            </label>
                            <label className="flex items-center gap-1.5">
                              Suffix
                              <input
                                defaultValue={g.suffix}
                                onBlur={(e) => {
                                  if (e.target.value !== g.suffix) setGroupField(g.name, 'suffix', e.target.value);
                                }}
                                className="w-20 font-data text-[12px] bg-[var(--mc-bg-surface)] border border-[var(--mc-border-strong)] rounded-[6px] px-1.5 py-0.5 outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                              />
                            </label>
                            <label className="flex items-center gap-1.5">
                              Priority
                              <input
                                type="number"
                                defaultValue={g.priority}
                                onBlur={(e) => {
                                  const v = parseInt(e.target.value, 10);
                                  if (!Number.isNaN(v) && v !== g.priority) setGroupField(g.name, 'priority', v);
                                }}
                                className="w-16 font-data text-[12px] bg-[var(--mc-bg-surface)] border border-[var(--mc-border-strong)] rounded-[6px] px-1.5 py-0.5 outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                              />
                            </label>
                            <label className="flex items-center gap-1.5">
                              <input
                                type="checkbox"
                                defaultChecked={g.isDefault}
                                onChange={(e) => {
                                  if (e.target.checked) setGroupField(g.name, 'isDefault', true);
                                  else e.target.checked = true;
                                }}
                                className="accent-[var(--mc-cyan-500)]"
                              />
                              Default group
                            </label>
                            <button
                              onClick={() => setEditingInherits((s) => ({ ...s, [g.name]: !s[g.name] }))}
                              className="underline transition-colors hover:text-[var(--mc-cyan-400)]"
                            >
                              inherits ({g.inherits.length})
                            </button>
                          </div>
                        )}

                        {editingInherits[g.name] && (
                          <div className="flex flex-wrap gap-2 mb-3 p-3 rounded-[8px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border)]">
                            {groups.filter((other) => other.name !== g.name).map((other) => (
                              <label key={other.name} className="flex items-center gap-1 text-[12px]">
                                <input
                                  type="checkbox"
                                  checked={g.inherits.includes(other.name)}
                                  onChange={() => toggleInherit(g, other.name)}
                                  className="accent-[var(--mc-cyan-500)]"
                                />
                                {other.name}
                              </label>
                            ))}
                            {groups.length <= 1 && <span className="text-[12px] text-[var(--mc-text-muted)]">No other groups to inherit from.</span>}
                          </div>
                        )}

                        <div className="mb-3">
                          <PermissionPills
                            permissions={g.permissions ?? []}
                            catalog={nodeCatalog}
                            onRemove={isAdmin ? (p) => removeGroupPermission(g.name, p) : undefined}
                          />
                        </div>
                        {isAdmin && (
                          <div className="flex gap-1.5">
                            <NodeInput
                              value={newPerm[`group:${g.name}`] ?? ''}
                              onChange={(v) => setNewPerm((s) => ({ ...s, [`group:${g.name}`]: v }))}
                              placeholder="neoessentials.node"
                            />
                            <button
                              onClick={() => addGroupPermission(g.name)}
                              className="btn-pop text-[12px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)]"
                            >
                              Add
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
              {groups.length === 0 && (
                <div className="text-center py-8 text-[13px] text-[var(--mc-text-muted)]">No groups configured.</div>
              )}
            </Card>

            {isAdmin && (
              <Card title="Create group" icon={Plus} accent="purple" padded className="h-fit">
                <form onSubmit={createGroup} className="flex flex-col gap-3">
                  <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                    Name
                    <input
                      value={groupName}
                      onChange={(e) => setGroupName(e.target.value)}
                      className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                    Prefix
                    <input
                      value={groupPrefix}
                      onChange={(e) => setGroupPrefix(e.target.value)}
                      className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                    Suffix
                    <input
                      value={groupSuffix}
                      onChange={(e) => setGroupSuffix(e.target.value)}
                      className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                    Priority
                    <input
                      type="number"
                      value={groupPriority}
                      onChange={(e) => setGroupPriority(parseInt(e.target.value, 10) || 0)}
                      className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                    />
                  </label>
                  <label className="flex items-center gap-2 text-[12px] text-[var(--mc-text-secondary)]">
                    <input type="checkbox" checked={groupIsDefault} onChange={(e) => setGroupIsDefault(e.target.checked)} className="accent-[var(--mc-cyan-500)]" />
                    Make default group
                  </label>
                  <button
                    type="submit"
                    disabled={groupSubmitting}
                    className="btn-pop text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
                  >
                    Create
                  </button>
                </form>
              </Card>
            )}
          </div>

          <Card title="Online users" icon={UserCog} accent="cyan">
            {users.length === 0 && (
              <div className="text-center py-8 text-[13px] text-[var(--mc-text-muted)]">No players online.</div>
            )}
            {users.map((u) => {
              const open = !!expandedUsers[u.username];
              return (
                <div key={u.username} className="border-b border-[var(--mc-border)] last:border-0">
                  <button
                    type="button"
                    onClick={() => toggleUserExpanded(u.username)}
                    className="w-full flex items-center gap-2.5 px-4 py-3 text-left text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]"
                  >
                    <Chevron open={open} />
                    <img
                      src={`https://mc-heads.net/avatar/${u.uuid}/32`}
                      alt=""
                      className="h-5 w-5 rounded-[4px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)]"
                    />
                    <span className="font-medium">{u.username}</span>
                    <span className="ml-auto flex items-center gap-2 text-[12px] text-[var(--mc-text-muted)]">
                      {(u.permissions ?? []).length} permission{(u.permissions ?? []).length === 1 ? '' : 's'}
                      <Badge variant="neutral">{u.group}</Badge>
                    </span>
                  </button>

                  {open && (
                    <div className="px-4 pb-4">
                      {isAdmin && (
                        <label className="flex items-center gap-2 mb-3 text-[12px] text-[var(--mc-text-secondary)]">
                          Group
                          <select
                            value={u.group}
                            onChange={(e) => setUserGroup(u.username, e.target.value)}
                            className="font-data text-[12px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[6px] px-1.5 py-0.5 outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                          >
                            {groups.map((g) => (
                              <option key={g.name} value={g.name}>{g.name}</option>
                            ))}
                          </select>
                        </label>
                      )}
                      <div className="mb-3">
                        <PermissionPills
                          permissions={u.permissions ?? []}
                          catalog={nodeCatalog}
                          onRemove={isAdmin ? (p) => removeUserPermission(u.username, p) : undefined}
                        />
                      </div>
                      {isAdmin && (
                        <div className="flex gap-1.5">
                          <NodeInput
                            value={newPerm[`user:${u.username}`] ?? ''}
                            onChange={(v) => setNewPerm((s) => ({ ...s, [`user:${u.username}`]: v }))}
                            placeholder="neoessentials.node"
                          />
                          <button
                            onClick={() => addUserPermission(u.username)}
                            className="btn-pop text-[12px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)]"
                          >
                            Add
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </Card>

          {isAdmin && (
            <Card title="Manage another player" icon={Search} accent="purple">
              <div className="px-4 py-3 border-b border-[var(--mc-border)]">
                <form onSubmit={runLookup} className="flex gap-1.5">
                  <input
                    value={lookupInput}
                    onChange={(e) => setLookupInput(e.target.value)}
                    placeholder="Username (online or offline)"
                    className="flex-1 font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                  />
                  <button type="submit" className="btn-pop text-[13px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)]">
                    Look up
                  </button>
                </form>
              </div>

              {lookupQuery && (
                <div className="px-4 py-4 text-[13px]">
                  {!lookupResult?.success ? (
                    <div className="text-[var(--mc-ember-500)]">{lookupResult?.message ?? `Could not find a player named '${lookupQuery}'.`}</div>
                  ) : (
                    <>
                      <div className="flex items-center gap-2 mb-3">
                        {lookupResult.uuid && (
                          <img src={`https://mc-heads.net/avatar/${lookupResult.uuid}/32`} alt="" className="h-5 w-5 rounded-[4px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)]" />
                        )}
                        <span className="font-medium">{lookupResult.username}</span>
                        <Badge variant={lookupResult.online ? 'moss' : 'neutral'} dot={lookupResult.online}>
                          {lookupResult.online ? 'online' : 'offline'}
                        </Badge>
                        <select
                          value={lookupResult.group}
                          onChange={(e) => setUserGroup(lookupResult.username!, e.target.value)}
                          className="font-data text-[12px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[6px] px-1.5 py-0.5 outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                        >
                          {groups.map((g) => (
                            <option key={g.name} value={g.name}>
                              {g.name}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="mb-3">
                        <PermissionPills
                          permissions={lookupResult.permissions ?? []}
                          catalog={nodeCatalog}
                          onRemove={(p) => removeUserPermission(lookupResult.username!, p)}
                        />
                      </div>
                      <div className="flex gap-1.5">
                        <NodeInput
                          value={newPerm[`user:${lookupResult.username}`] ?? ''}
                          onChange={(v) => setNewPerm((s) => ({ ...s, [`user:${lookupResult.username}`]: v }))}
                          placeholder="neoessentials.node"
                        />
                        <button
                          onClick={() => addUserPermission(lookupResult.username!)}
                          className="btn-pop text-[12px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)]"
                        >
                          Add
                        </button>
                      </div>
                    </>
                  )}
                </div>
              )}
            </Card>
          )}

          <div className="grid grid-cols-[1fr_320px] gap-5">
            <Card title="Permission aliases" icon={Link2} accent="cyan">
              {Object.entries(aliases).length === 0 && <div className="text-center py-8 text-[13px] text-[var(--mc-text-muted)]">No aliases configured.</div>}
              {Object.entries(aliases).map(([alias, canonical]) => (
                <div key={alias} className="flex items-center px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] font-data transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
                  <span className="flex-1">
                    {alias} &rarr; {canonical}
                  </span>
                  {isAdmin && (
                    <button
                      onClick={() => deleteAlias(alias)}
                      className="text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
                    >
                      Delete
                    </button>
                  )}
                </div>
              ))}
            </Card>

            {isAdmin && (
              <Card title="Add alias" icon={Key} accent="purple" padded className="h-fit">
                <form onSubmit={createAlias} className="flex flex-col gap-3">
                  <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                    Alias
                    <input
                      value={aliasName}
                      onChange={(e) => setAliasName(e.target.value)}
                      className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                    />
                  </label>
                  <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                    Canonical node
                    <NodeInput value={aliasCanonical} onChange={setAliasCanonical} placeholder="" />
                  </label>
                  <button
                    type="submit"
                    disabled={aliasSubmitting}
                    className="btn-pop text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
                  >
                    Add
                  </button>
                </form>
              </Card>
            )}
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
