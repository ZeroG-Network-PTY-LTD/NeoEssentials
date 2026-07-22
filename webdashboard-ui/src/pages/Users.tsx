import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { ModUser, ModUserSession, ModUserRole } from '../types';
import { UserCog, UserPlus, KeyRound, Power, Trash2, Radio } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Users.tsx — these are the mod's own
 * dashboard-account store (AuthenticationManager/UserManagementEndpoint), the same one this
 * internal SPA's own login already authenticates against — not a separate account system
 * needing sync, unlike the external Laravel app's copy of this page. */

const ROLES: ModUserRole[] = ['ADMIN', 'OPERATOR', 'MODERATOR', 'VIEWER'];

const ROLE_BADGE: Record<ModUserRole, 'purple' | 'cyan' | 'moss' | 'neutral'> = {
  ADMIN: 'purple',
  OPERATOR: 'moss',
  MODERATOR: 'cyan',
  VIEWER: 'neutral',
};

export default function Users() {
  const { showToast } = useToast();
  const [users, setUsers] = useState<ModUser[]>([]);
  const [sessions, setSessions] = useState<ModUserSession[]>([]);
  const [loading, setLoading] = useState(true);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRoleField] = useState<ModUserRole>('VIEWER');
  const [submitting, setSubmitting] = useState(false);

  const refresh = () => {
    Promise.all([mcApi.modUsers(), mcApi.modUserSessions()])
      .then(([u, s]) => {
        setUsers(u);
        setSessions(s);
      })
      .finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) return;
    setSubmitting(true);
    try {
      await mcApi.createModUser(username.trim(), password, email.trim(), role);
      showToast(`Created account '${username.trim()}'.`);
      setUsername('');
      setPassword('');
      setEmail('');
      setRoleField('VIEWER');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Create failed.', true);
    } finally {
      setSubmitting(false);
    }
  };

  const setUserRole = async (id: string, newRole: ModUserRole) => {
    try {
      await mcApi.setModUserRole(id, newRole);
      showToast('Role updated.');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Role update failed.', true);
    }
  };

  const resetPassword = async (id: string, uname: string) => {
    if (!confirm(`Generate a new temporary password for '${uname}'?`)) return;
    try {
      const result = await mcApi.setModUserPassword(id);
      const tempPassword = (result as { password?: string }).password;
      showToast(tempPassword ? `New temp password for ${uname}: ${tempPassword}` : `Password reset for ${uname}.`);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Reset failed.', true);
    }
  };

  const toggleEnabled = async (id: string, enabled: boolean) => {
    try {
      if (enabled) {
        await mcApi.disableModUser(id);
      } else {
        await mcApi.enableModUser(id);
      }
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Update failed.', true);
    }
  };

  const destroy = async (id: string, uname: string) => {
    if (!confirm(`Delete mod dashboard account '${uname}'? This cannot be undone.`)) return;
    try {
      await mcApi.deleteModUser(id);
      showToast(`Deleted account '${uname}'.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Delete failed.', true);
    }
  };

  const revokeSession = async (sessionId: string) => {
    try {
      await mcApi.revokeModUserSession(sessionId);
      showToast('Session revoked.');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Revoke failed.', true);
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
      <PageHeading title="Mod dashboard accounts" icon={UserCog} subtitle="Logins for this dashboard, and for the external Laravel app's service account, if paired." />

      <div className="grid grid-cols-[1fr_320px] gap-5 mb-5">
        <Card title={`${users.length} account${users.length === 1 ? '' : 's'}`} icon={UserCog}>
          {users.length === 0 && <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No accounts found.</div>}
          {users.map((u) => (
            <div key={u.id} className="flex items-center px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] gap-2 transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
              <Badge variant={u.enabled ? 'moss' : 'neutral'} dot={u.enabled}>
                {u.enabled ? 'enabled' : 'disabled'}
              </Badge>
              <div className="flex-1 ml-1">
                <div className="font-medium">{u.username}</div>
                <div className="text-[11px] text-[var(--mc-text-muted)]">
                  {typeof u.lastLoginAt === 'number' && u.lastLoginAt > 0 ? `Last login ${new Date(u.lastLoginAt).toLocaleString()}` : 'Never logged in'}
                </div>
              </div>
              <Badge variant={ROLE_BADGE[u.role]}>{u.role}</Badge>
              <select
                value={u.role}
                onChange={(e) => setUserRole(u.id, e.target.value as ModUserRole)}
                className="text-[12px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[6px] px-2 py-1 transition-colors focus:border-[var(--mc-cyan-400)] outline-none"
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
              <button
                onClick={() => resetPassword(u.id, u.username)}
                className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
              >
                <KeyRound size={12} strokeWidth={2} />
                Reset password
              </button>
              <button
                onClick={() => toggleEnabled(u.id, u.enabled)}
                className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
              >
                <Power size={12} strokeWidth={2} />
                {u.enabled ? 'Disable' : 'Enable'}
              </button>
              <button
                onClick={() => destroy(u.id, u.username)}
                className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
              >
                <Trash2 size={12} strokeWidth={2} />
                Delete
              </button>
            </div>
          ))}
        </Card>

        <Card title="Create account" icon={UserPlus} accent="purple" padded className="h-fit">
          <form onSubmit={submit} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Username
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Email (optional)
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>

            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Role
              <select
                value={role}
                onChange={(e) => setRoleField(e.target.value as ModUserRole)}
                className="text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </label>

            <button
              type="submit"
              disabled={submitting}
              className="btn-pop mt-1 flex items-center justify-center gap-1.5 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
            >
              <UserPlus size={13} strokeWidth={2} />
              Create
            </button>
          </form>
        </Card>
      </div>

      <Card title="Active sessions" icon={Radio} accent="moss">
        {sessions.length === 0 && <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No active sessions.</div>}
        {sessions.map((s) => (
          <div key={s.sessionId} className="flex items-center px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
            <div className="flex-1">
              <div>
                {s.username} <span className="text-[var(--mc-text-muted)]">({s.role})</span>
              </div>
              <div className="text-[11px] text-[var(--mc-text-muted)]">last active {new Date(s.lastAccessAt).toLocaleString()}</div>
            </div>
            <span className="font-data text-[12px] text-[var(--mc-text-muted)] mr-3">{s.ipAddress}</span>
            <button
              onClick={() => revokeSession(s.sessionId)}
              className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
            >
              <Trash2 size={12} strokeWidth={2} />
              Revoke
            </button>
          </div>
        ))}
      </Card>
    </DashboardLayout>
  );
}
