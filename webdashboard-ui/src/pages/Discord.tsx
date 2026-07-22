import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useAuth } from '../lib/auth';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { DiscordAuthConfig, DiscordEvent, DiscordStatus, ModUserRole } from '../types';
import { MessageCircle, Radio, ScrollText, Send, Settings2, Trash2, PlugZap } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Discord.tsx — usePage().props.auth
 * became useAuth().isAdmin; the two useForm() instances became plain useState. This is the
 * mod's OWN Discord integration status/config (account-linking via a companion mod like
 * Simple Discord Link) — not the external Laravel app's separate OAuth-login-client config,
 * which has no equivalent here. */

export default function Discord() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const [status, setStatus] = useState<DiscordStatus | null>(null);
  const [events, setEvents] = useState<DiscordEvent[]>([]);
  const [authConfig, setAuthConfig] = useState<DiscordAuthConfig | null>(null);
  const [loading, setLoading] = useState(true);

  const [testChannel, setTestChannel] = useState('');
  const [testMessage, setTestMessage] = useState('');
  const [sendingTest, setSendingTest] = useState(false);

  const [configEnabled, setConfigEnabled] = useState(false);
  const [configRequireLinked, setConfigRequireLinked] = useState(false);
  const [configAutoRegister, setConfigAutoRegister] = useState(false);
  const [configDefaultRole, setConfigDefaultRole] = useState<ModUserRole>('VIEWER');
  const [savingConfig, setSavingConfig] = useState(false);

  const refresh = () => {
    const promises: [Promise<DiscordStatus>, Promise<DiscordEvent[]>, Promise<DiscordAuthConfig | null>] = [
      mcApi.discordStatus(),
      mcApi.discordEvents(),
      isAdmin ? mcApi.discordAuthConfig() : Promise.resolve(null),
    ];
    Promise.all(promises)
      .then(([s, e, cfg]) => {
        setStatus(s);
        setEvents(e);
        setAuthConfig(cfg);
        if (cfg) {
          setConfigEnabled(cfg.enabled);
          setConfigRequireLinked(cfg.requireLinkedAccount);
          setConfigAutoRegister(cfg.allowAutoRegistration);
          setConfigDefaultRole(cfg.defaultRole);
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(refresh, []);

  const sendTest = async (e: FormEvent) => {
    e.preventDefault();
    setSendingTest(true);
    try {
      await mcApi.sendDiscordTestMessage(testChannel, testMessage);
      showToast('Test message sent.');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Send failed.', true);
    } finally {
      setSendingTest(false);
    }
  };

  const saveConfig = async (e: FormEvent) => {
    e.preventDefault();
    setSavingConfig(true);
    try {
      await mcApi.updateDiscordAuthConfig({
        enabled: configEnabled,
        requireLinkedAccount: configRequireLinked,
        allowAutoRegistration: configAutoRegister,
        defaultRole: configDefaultRole,
      });
      showToast('Saved Discord config.');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Save failed.', true);
    } finally {
      setSavingConfig(false);
    }
  };

  const clearEvents = async () => {
    if (!confirm('Clear the Discord event log?')) return;
    try {
      await mcApi.clearDiscordEvents();
      showToast('Cleared event log.');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Clear failed.', true);
    }
  };

  if (loading || !status) {
    return (
      <DashboardLayout>
        <div className="text-[13px] text-[var(--mc-text-muted)]">Loading…</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <PageHeading
        title="Discord"
        icon={MessageCircle}
        subtitle="Bridge status, account-linking config, and recent bridge activity."
        action={
          <Badge variant={status.anyActive ? 'moss' : 'ember'} dot>
            {status.anyActive ? 'Active' : 'Inactive'}
          </Badge>
        }
      />

      <div className="grid grid-cols-[1fr_340px] gap-5">
        <div className="flex flex-col gap-5">
          <Card title="Bridge status" icon={PlugZap} accent="cyan" padded>
            <div className="flex items-center gap-4 text-[13px] mb-3">
              <span className="text-[var(--mc-text-muted)]">
                <span className="font-data text-[var(--mc-text-primary)]">{status.adapterCount}</span> adapter(s)
              </span>
              <span className="text-[var(--mc-text-muted)]">
                <span className="font-data text-[var(--mc-text-primary)]">{status.eventCount}</span> events logged
              </span>
            </div>
            <div className="flex flex-col gap-1.5">
              {(status.adapters ?? []).map((a) => (
                <div key={a.name} className="flex items-center gap-2.5 text-[12.5px] rounded-[8px] border border-[var(--mc-border)] bg-[var(--mc-bg-surface-raised)] px-3 py-2">
                  <span
                    className="relative h-1.5 w-1.5 shrink-0 rounded-full"
                    style={{ background: a.ready ? 'var(--mc-moss-500)' : a.enabled ? 'var(--mc-cyan-500)' : 'var(--mc-text-muted)' }}
                  >
                    {a.ready && <span className="pulse-dot absolute inset-0 rounded-full text-[var(--mc-moss-500)]" />}
                  </span>
                  <span className="font-medium">{a.name}</span>
                  <span className="ml-auto text-[var(--mc-text-muted)]">{a.ready ? 'connected' : a.enabled ? 'installed, not connected' : 'not installed'}</span>
                </div>
              ))}
              {(status.adapters ?? []).length === 0 && (
                <div className="text-[12.5px] text-[var(--mc-text-muted)] px-1 py-2">No Discord companion mod adapters detected.</div>
              )}
            </div>
          </Card>

          <Card
            title="Recent events"
            icon={ScrollText}
            accent="purple"
            action={
              isAdmin && (
                <button
                  onClick={clearEvents}
                  className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
                >
                  <Trash2 size={12} strokeWidth={2} />
                  Clear
                </button>
              )
            }
          >
            {events.length === 0 && <div className="px-4 py-8 text-center text-[13px] text-[var(--mc-text-muted)]">No events yet.</div>}
            {events.map((e, i) => (
              <div key={i} className="px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] flex items-center gap-3 transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
                <span className="font-data text-[11.5px] text-[var(--mc-text-muted)] w-32 shrink-0">{new Date(e.timestamp).toLocaleString()}</span>
                <Badge variant="purple">{e.type}</Badge>
                {e.actor && <span className="text-[var(--mc-text-muted)]">by {e.actor}</span>}
                {e.message && <span className="text-[var(--mc-text-muted)] truncate">{e.message}</span>}
              </div>
            ))}
          </Card>
        </div>

        <div className="flex flex-col gap-5">
          {isAdmin && (
            <Card title="Send test message" icon={Send} accent="cyan" padded>
              <form onSubmit={sendTest} className="flex flex-col gap-3">
                <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                  Channel ID
                  <input
                    value={testChannel}
                    onChange={(e) => setTestChannel(e.target.value)}
                    placeholder="e.g. 123456789012345678"
                    pattern="\d{15,25}"
                    title="The channel's numeric Discord ID, not its name"
                    required
                    className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                  />
                  <span className="text-[var(--mc-text-muted)]">Right-click the channel in Discord (Developer Mode on) → Copy Channel ID.</span>
                </label>
                <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                  Message
                  <input
                    value={testMessage}
                    onChange={(e) => setTestMessage(e.target.value)}
                    className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                  />
                </label>
                <button
                  type="submit"
                  disabled={sendingTest}
                  className="btn-pop flex items-center justify-center gap-1.5 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
                >
                  <Send size={13} strokeWidth={2} />
                  Send
                </button>
              </form>
            </Card>
          )}

          {isAdmin && authConfig && (
            <Card title="Account-linking config" icon={Settings2} accent="purple" padded>
              <form onSubmit={saveConfig} className="flex flex-col gap-3">
                <p className="text-[12px] text-[var(--mc-text-muted)] -mt-1 mb-1">
                  Players link their Discord account in-game via Simple Discord Link, Mc2Discord, or DCIntegration's own commands. NeoEssentials
                  never contacts Discord directly — it only reads the link once one of those mods reports it.
                  {!authConfig.linkAdapterAvailable && (
                    <span className="mt-2 flex items-center gap-1.5 text-[var(--mc-ember-500)]">
                      <Radio size={12} strokeWidth={2} className="shrink-0" />
                      No Discord companion mod is currently installed/connected.
                    </span>
                  )}
                </p>

                <div className="flex flex-col gap-1.5 rounded-[8px] border border-[var(--mc-border)] bg-[var(--mc-bg-surface-raised)] p-2.5">
                  <label className="flex items-center gap-2 text-[12px] text-[var(--mc-text-secondary)] py-0.5">
                    <input type="checkbox" checked={configEnabled} onChange={(e) => setConfigEnabled(e.target.checked)} className="accent-[var(--mc-cyan-500)]" />
                    enabled
                  </label>
                  <label className="flex items-center gap-2 text-[12px] text-[var(--mc-text-secondary)] py-0.5">
                    <input type="checkbox" checked={configRequireLinked} onChange={(e) => setConfigRequireLinked(e.target.checked)} className="accent-[var(--mc-cyan-500)]" />
                    requireLinkedAccount
                  </label>
                  <label className="flex items-center gap-2 text-[12px] text-[var(--mc-text-secondary)] py-0.5">
                    <input type="checkbox" checked={configAutoRegister} onChange={(e) => setConfigAutoRegister(e.target.checked)} className="accent-[var(--mc-cyan-500)]" />
                    allowAutoRegistration
                  </label>
                </div>

                <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                  Default role
                  <select
                    value={configDefaultRole}
                    onChange={(e) => setConfigDefaultRole(e.target.value as ModUserRole)}
                    className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                  >
                    <option value="VIEWER">VIEWER</option>
                    <option value="MODERATOR">MODERATOR</option>
                    <option value="OPERATOR">OPERATOR</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </label>

                <button
                  type="submit"
                  disabled={savingConfig}
                  className="btn-pop mt-1 text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
                >
                  Save
                </button>
              </form>
            </Card>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
