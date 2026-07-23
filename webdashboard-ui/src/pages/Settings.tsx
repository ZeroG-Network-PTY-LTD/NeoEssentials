import { useEffect, useRef, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import PlayerRender from '../components/PlayerRender';
import { useToast } from '../lib/toast';
import { useAuth } from '../lib/auth';
import * as mcApi from '../lib/mcApi';
import { Settings as SettingsIcon, KeyRound, Gamepad2, MessageCircle, Copy, Unlink } from 'lucide-react';

/** New this pass — no direct external-dashboard equivalent to port line-for-line; mirrors its
 * Profile/Edit.tsx conceptually (password + account-linking sections) but built against this
 * repo's own mcApi/auth plumbing. See MinecraftLinkForm.tsx on the external side for the same
 * code+poll UX. */
export default function SettingsPage() {
  const { showToast } = useToast();
  const { user } = useAuth();

  // --- Change password ---
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [changingPassword, setChangingPassword] = useState(false);

  const submitPasswordChange = async (e: FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      showToast("New password and confirmation don't match.", true);
      return;
    }
    setChangingPassword(true);
    try {
      await mcApi.changePassword(oldPassword, newPassword);
      showToast('Password changed. Please log in again.');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Password change failed.', true);
    } finally {
      setChangingPassword(false);
    }
  };

  // --- Link Minecraft account ---
  const [mcUuid, setMcUuid] = useState<string | null>(user?.mcUuid ?? null);
  const [mcUsername, setMcUsername] = useState<string | null>(user?.mcUsername ?? null);
  const [linkCode, setLinkCode] = useState<string | null>(null);
  const [linking, setLinking] = useState(false);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => () => { if (pollRef.current) clearInterval(pollRef.current); }, []);

  const startMinecraftLink = async () => {
    setLinking(true);
    try {
      const result = await mcApi.linkMinecraftStart();
      setLinkCode(result.code);
      pollRef.current = setInterval(async () => {
        const status = await mcApi.linkMinecraftStatus();
        if (status.linked) {
          if (pollRef.current) clearInterval(pollRef.current);
          setLinkCode(null);
          setMcUuid(status.mcUuid ?? null);
          setMcUsername(status.mcUsername ?? null);
          showToast('Minecraft account linked!');
        }
      }, 3000);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Could not start linking.', true);
    } finally {
      setLinking(false);
    }
  };

  const unlinkMinecraftAccount = async () => {
    if (!confirm('Unlink this Minecraft account from your dashboard account?')) return;
    try {
      await mcApi.unlinkMinecraft();
      setMcUuid(null);
      setMcUsername(null);
      showToast('Minecraft account unlinked.');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Unlink failed.', true);
    }
  };

  const copyCode = () => {
    if (linkCode) {
      navigator.clipboard.writeText(linkCode).catch(() => {});
      showToast('Code copied.');
    }
  };

  // --- Discord status (read-only — this mod never performs Discord OAuth2 itself) ---
  const [discordLinked, setDiscordLinked] = useState<boolean | null>(null);
  const [discordUsername, setDiscordUsername] = useState<string | null>(null);

  useEffect(() => {
    if (!mcUuid) {
      setDiscordLinked(false);
      return;
    }
    mcApi.accountDiscordStatus()
      .then((status) => {
        setDiscordLinked(status.linked);
        setDiscordUsername(status.discordUsername ?? null);
      })
      .catch(() => setDiscordLinked(false));
  }, [mcUuid]);

  return (
    <DashboardLayout>
      <PageHeading title="Settings" icon={SettingsIcon} subtitle="Your account, linked Minecraft account, and Discord status." />

      <div className="flex flex-col gap-5 max-w-xl">
        <Card title="Change password" icon={KeyRound} accent="cyan" padded>
          <form onSubmit={submitPasswordChange} className="flex flex-col gap-3">
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Current password
              <input
                type="password"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                required
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              New password
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={8}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>
            <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
              Confirm new password
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={8}
                className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
              />
            </label>
            <button
              type="submit"
              disabled={changingPassword}
              className="btn-pop mt-1 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-cyan-400)] transition-colors disabled:opacity-50 self-start"
            >
              {changingPassword ? 'Saving…' : 'Change password'}
            </button>
          </form>
        </Card>

        <Card title="Minecraft account" icon={Gamepad2} accent="moss" padded>
          {mcUuid ? (
            <div className="flex items-center gap-4">
              <PlayerRender uuid={mcUuid} size={110} />
              <div className="flex-1">
                <div className="text-[13px] font-medium">{mcUsername}</div>
                <Badge variant="moss" dot>Linked</Badge>
              </div>
              <button
                type="button"
                onClick={unlinkMinecraftAccount}
                className="flex items-center gap-1.5 text-[12px] px-2.5 py-1.5 rounded-[8px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
              >
                <Unlink size={12} /> Unlink
              </button>
            </div>
          ) : linkCode ? (
            <div className="flex flex-col gap-3">
              <p className="text-[12.5px] text-[var(--mc-text-secondary)]">
                Run this command in-game to finish linking (expires in 5 minutes):
              </p>
              <div className="flex items-center gap-2">
                <code className="flex-1 font-data text-[15px] tracking-wider bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-3 py-2 text-center text-[var(--mc-cyan-400)]">
                  /linkaccount {linkCode}
                </code>
                <button
                  type="button"
                  onClick={copyCode}
                  className="p-2 rounded-[8px] border border-[var(--mc-border-strong)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
                  title="Copy code"
                >
                  <Copy size={14} />
                </button>
              </div>
              <p className="text-[11px] text-[var(--mc-text-muted)]">Waiting for you to run the command…</p>
            </div>
          ) : (
            <div className="flex items-center justify-between gap-3">
              <p className="text-[12.5px] text-[var(--mc-text-secondary)]">
                No Minecraft account linked yet.
              </p>
              <button
                type="button"
                onClick={startMinecraftLink}
                disabled={linking}
                className="btn-pop text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-moss-500)] text-[#0a1620] font-medium hover:bg-[var(--mc-moss-400)] transition-colors disabled:opacity-50 shrink-0"
              >
                {linking ? 'Starting…' : 'Link Minecraft account'}
              </button>
            </div>
          )}
        </Card>

        <Card title="Discord" icon={MessageCircle} accent="purple" padded>
          {!mcUuid ? (
            <p className="text-[12.5px] text-[var(--mc-text-secondary)]">
              Link your Minecraft account above first — Discord status is resolved from there.
            </p>
          ) : discordLinked === null ? (
            <p className="text-[12.5px] text-[var(--mc-text-muted)]">Checking…</p>
          ) : discordLinked ? (
            <div className="flex items-center gap-2">
              <Badge variant="purple" dot>Linked</Badge>
              <span className="text-[13px]">{discordUsername}</span>
            </div>
          ) : (
            <p className="text-[12.5px] text-[var(--mc-text-secondary)]">
              Not linked. Use the server's Discord bot to link your account in-game or in Discord —
              it'll show up here automatically once linked.
            </p>
          )}
        </Card>
      </div>
    </DashboardLayout>
  );
}
