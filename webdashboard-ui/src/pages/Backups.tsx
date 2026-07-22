import { useEffect, useState, type FormEvent } from 'react';
import DashboardLayout from '../layouts/DashboardLayout';
import Card from '../components/Dashboard/Card';
import PageHeading from '../components/Dashboard/PageHeading';
import Badge from '../components/Dashboard/Badge';
import { useAuth } from '../lib/auth';
import { useToast } from '../lib/toast';
import * as mcApi from '../lib/mcApi';
import type { BackupSnapshot, BackupStatus, CloudConfig, CloudFile, CloudStatus } from '../types';
import { Database, Plus, Cloud, CloudUpload, Trash2, Download, RotateCcw, HardDrive } from 'lucide-react';

/** Ported from the external dashboard's Pages/Dashboard/Backups.tsx — useForm()/router.*()
 * became plain useState + mcApi calls; the download `<a href>` (which relied on Laravel's own
 * session cookie for auth) became a click handler using mcApi.downloadBackup()'s blob/object-URL
 * approach, since a bare anchor can't attach this SPA's Bearer token. */

export default function Backups() {
  const { isAdmin } = useAuth();
  const { showToast } = useToast();
  const [status, setStatus] = useState<BackupStatus | null>(null);
  const [snapshots, setSnapshots] = useState<BackupSnapshot[]>([]);
  const [cloudStatusState, setCloudStatusState] = useState<CloudStatus | null>(null);
  const [cloudConfigState, setCloudConfigState] = useState<CloudConfig | null>(null);
  const [dropboxFiles, setDropboxFiles] = useState<CloudFile[]>([]);
  const [googleFiles, setGoogleFiles] = useState<CloudFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [targets, setTargets] = useState<string[]>([]);
  const [backupName, setBackupName] = useState('');

  const [dropboxToken, setDropboxToken] = useState('');
  const [dropboxPath, setDropboxPath] = useState('/NeoEssentials-Backups');
  const [googleClientId, setGoogleClientId] = useState('');
  const [googleClientSecret, setGoogleClientSecret] = useState('');
  const [googleRefreshToken, setGoogleRefreshToken] = useState('');
  const [googleFolderId, setGoogleFolderId] = useState('');

  const refresh = () => {
    const promises: [Promise<BackupStatus>, Promise<BackupSnapshot[]>, Promise<CloudStatus | null>, Promise<CloudConfig | null>] = [
      mcApi.backupStatus(),
      mcApi.backupList(),
      isAdmin ? mcApi.cloudStatus() : Promise.resolve(null),
      isAdmin ? mcApi.cloudConfig() : Promise.resolve(null),
    ];
    Promise.all(promises)
      .then(([s, snaps, cs, cc]) => {
        setStatus(s);
        setSnapshots(snaps);
        setCloudStatusState(cs);
        setCloudConfigState(cc);
        setTargets((s.availableTargets ?? []).map((t) => t.key));
      })
      .finally(() => setLoading(false));
    if (isAdmin) {
      mcApi.cloudDropboxFiles().then(setDropboxFiles);
      mcApi.cloudGoogleFiles().then(setGoogleFiles);
    }
  };

  useEffect(refresh, []);

  const toggleTarget = (key: string) => setTargets((t) => (t.includes(key) ? t.filter((k) => k !== key) : [...t, key]));

  const createBackup = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await mcApi.createBackup(backupName, targets);
      showToast('Backup created.');
      setBackupName('');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Create failed.', true);
    }
  };

  const download = async (name: string) => {
    try {
      await mcApi.downloadBackup(name);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Download failed.', true);
    }
  };

  const restore = async (name: string) => {
    if (!confirm(`Restore snapshot '${name}'? A pre-restore backup will be made automatically.`)) return;
    try {
      await mcApi.restoreBackup(name);
      showToast(`Restored '${name}'.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Restore failed.', true);
    }
  };

  const destroy = async (name: string) => {
    if (!confirm(`Delete snapshot '${name}'?`)) return;
    try {
      await mcApi.deleteBackup(name);
      showToast(`Deleted '${name}'.`);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Delete failed.', true);
    }
  };

  const saveDropbox = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await mcApi.configureDropbox(dropboxToken, dropboxPath);
      showToast('Saved Dropbox config.');
      setDropboxToken('');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Save failed.', true);
    }
  };

  const saveGoogle = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await mcApi.configureGoogleDrive(googleRefreshToken, googleClientId, googleClientSecret, googleFolderId);
      showToast('Saved Google Drive config.');
      setGoogleRefreshToken('');
      setGoogleClientSecret('');
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Save failed.', true);
    }
  };

  const testDropbox = async () => {
    try {
      await mcApi.testDropbox();
      showToast('Dropbox connection OK.');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Test failed.', true);
    }
  };

  const testGoogle = async () => {
    try {
      await mcApi.testGoogleDrive();
      showToast('Google Drive connection OK.');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Test failed.', true);
    }
  };

  const uploadDropbox = async (name: string) => {
    try {
      await mcApi.uploadBackupToDropbox(name);
      showToast(`Uploaded '${name}' to Dropbox.`);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Upload failed.', true);
    }
  };

  const uploadGoogle = async (name: string) => {
    try {
      await mcApi.uploadBackupToGoogleDrive(name);
      showToast(`Uploaded '${name}' to Google Drive.`);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Upload failed.', true);
    }
  };

  const deleteDropboxFile = async (path: string) => {
    if (!confirm(`Delete '${path}' from Dropbox?`)) return;
    try {
      await mcApi.deleteDropboxFile(path);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Delete failed.', true);
    }
  };

  const deleteGoogleFile = async (id: string) => {
    if (!confirm('Delete this file from Google Drive?')) return;
    try {
      await mcApi.deleteGoogleDriveFile(id);
      refresh();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Delete failed.', true);
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
        title="Backups"
        icon={Database}
        subtitle={`${status.count}/${status.maxSnapshots} snapshots · ${status.totalSizeMb} MB · last backup: ${status.lastBackup ? new Date(status.lastBackup).toLocaleString() : 'never'}`}
      />

      <div className="grid grid-cols-[1fr_320px] gap-5 mb-5">
        <Card title={`${snapshots.length} snapshot${snapshots.length === 1 ? '' : 's'}`} icon={HardDrive} accent="cyan">
          {snapshots.length === 0 && <div className="text-center py-8 text-[13px] text-[var(--mc-text-muted)]">No snapshots yet.</div>}
          {snapshots.map((s) => (
            <div key={s.filename} className="px-4 py-2.5 border-b border-[var(--mc-border)] last:border-0 text-[13px] transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
              <div className="flex items-center gap-3">
                <span className="flex-1 font-medium">{s.name}</span>
                <span className="font-data text-[12px] text-[var(--mc-text-muted)]">
                  {s.sizeMb ?? (s.sizeBytes / 1_048_576).toFixed(2)} MB · {s.created === 'unknown' ? 'unknown' : new Date(s.created).toLocaleString()}
                </span>
              </div>
              <div className="flex gap-1.5 mt-1.5">
                <button
                  onClick={() => download(s.name)}
                  className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]"
                >
                  <Download size={12} strokeWidth={2} />
                  Download
                </button>
                {isAdmin && (
                  <>
                    {cloudStatusState?.providers.dropbox.configured && (
                      <button
                        onClick={() => uploadDropbox(s.name)}
                        className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]"
                      >
                        <CloudUpload size={12} strokeWidth={2} />
                        Dropbox
                      </button>
                    )}
                    {cloudStatusState?.providers.googleDrive.configured && (
                      <button
                        onClick={() => uploadGoogle(s.name)}
                        className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]"
                      >
                        <CloudUpload size={12} strokeWidth={2} />
                        Drive
                      </button>
                    )}
                    <button
                      onClick={() => restore(s.name)}
                      className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-moss-500)] text-white transition-colors hover:bg-[var(--mc-moss-600,var(--mc-moss-500))]"
                    >
                      <RotateCcw size={12} strokeWidth={2} />
                      Restore
                    </button>
                    <button
                      onClick={() => destroy(s.name)}
                      className="flex items-center gap-1.5 text-[12px] px-2.5 py-1 rounded-[var(--radius)] bg-[var(--mc-ember-500)] text-white transition-colors hover:bg-[var(--mc-ember-600,var(--mc-ember-500))]"
                    >
                      <Trash2 size={12} strokeWidth={2} />
                      Delete
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </Card>

        {isAdmin && (
          <Card title="Create backup" icon={Plus} accent="purple" padded className="h-fit">
            <form onSubmit={createBackup} className="flex flex-col gap-3">
              <label className="flex flex-col gap-1 text-[12px] text-[var(--mc-text-secondary)]">
                Name (optional)
                <input
                  value={backupName}
                  onChange={(e) => setBackupName(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
              </label>
              <div className="flex flex-col gap-1.5">
                <span className="text-[12px] text-[var(--mc-text-secondary)]">Targets</span>
                {(status.availableTargets ?? []).map((t) => (
                  <label key={t.key} className="flex items-center gap-2 text-[12px]">
                    <input type="checkbox" checked={targets.includes(t.key)} onChange={() => toggleTarget(t.key)} className="accent-[var(--mc-cyan-500)]" />
                    {t.key} {!t.exists && <span className="text-[var(--mc-text-muted)]">(missing)</span>}
                  </label>
                ))}
              </div>
              <button
                type="submit"
                disabled={targets.length === 0}
                className="btn-pop text-[13px] px-3 py-2 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)] disabled:opacity-50"
              >
                Create
              </button>
            </form>
          </Card>
        )}
      </div>

      {isAdmin && cloudConfigState && (
        <div className="grid grid-cols-2 gap-5">
          <Card title="Dropbox" icon={Cloud} accent="cyan" padded>
            <div className="flex flex-col gap-3">
              <Badge variant={cloudConfigState.dropbox.configured ? 'moss' : 'neutral'} className="w-fit">
                {cloudConfigState.dropbox.configured ? `Configured (${cloudConfigState.dropbox.tokenMasked})` : 'Not configured'}
              </Badge>
              <form onSubmit={saveDropbox} className="flex flex-col gap-2">
                <input
                  type="password"
                  placeholder="Access token"
                  value={dropboxToken}
                  onChange={(e) => setDropboxToken(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
                <input
                  placeholder="Upload path"
                  value={dropboxPath}
                  onChange={(e) => setDropboxPath(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
                <div className="flex gap-2">
                  <button type="submit" className="btn-pop text-[13px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)]">
                    Save
                  </button>
                  <button type="button" onClick={testDropbox} className="text-[13px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]">
                    Test
                  </button>
                </div>
              </form>
              {dropboxFiles.length > 0 && (
                <div className="flex flex-col gap-1 mt-1">
                  {dropboxFiles.map((f) => (
                    <div key={f.path ?? f.name} className="flex items-center text-[12px] font-data rounded-[6px] px-2 py-1 transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
                      <span className="flex-1 truncate">{f.name}</span>
                      <button onClick={() => deleteDropboxFile((f.path as string) ?? f.name)} className="text-[var(--mc-ember-500)] transition-colors hover:text-[var(--mc-ember-400)]">
                        Delete
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Card>

          <Card title="Google Drive" icon={Cloud} accent="purple" padded>
            <div className="flex flex-col gap-3">
              <Badge variant={cloudConfigState.googleDrive.configured ? 'moss' : 'neutral'} className="w-fit">
                {cloudConfigState.googleDrive.configured ? `Configured (folder ${cloudConfigState.googleDrive.folderId})` : 'Not configured'}
              </Badge>
              <form onSubmit={saveGoogle} className="flex flex-col gap-2">
                <input
                  placeholder="Client ID"
                  value={googleClientId}
                  onChange={(e) => setGoogleClientId(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
                <input
                  type="password"
                  placeholder="Client secret"
                  value={googleClientSecret}
                  onChange={(e) => setGoogleClientSecret(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
                <input
                  type="password"
                  placeholder="Refresh token"
                  value={googleRefreshToken}
                  onChange={(e) => setGoogleRefreshToken(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
                <input
                  placeholder="Folder ID"
                  value={googleFolderId}
                  onChange={(e) => setGoogleFolderId(e.target.value)}
                  className="font-data text-[13px] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] rounded-[8px] px-2.5 py-1.5 text-[var(--mc-text-primary)] outline-none transition-colors focus:border-[var(--mc-cyan-400)]"
                />
                <div className="flex gap-2">
                  <button type="submit" className="btn-pop text-[13px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-cyan-500)] text-[#0a1620] font-medium transition-colors hover:bg-[var(--mc-cyan-400)]">
                    Save
                  </button>
                  <button type="button" onClick={testGoogle} className="text-[13px] px-3 py-1.5 rounded-[var(--radius)] bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] transition-colors hover:bg-[var(--mc-bg-surface)]">
                    Test
                  </button>
                </div>
              </form>
              {googleFiles.length > 0 && (
                <div className="flex flex-col gap-1 mt-1">
                  {googleFiles.map((f) => (
                    <div key={f.id ?? f.name} className="flex items-center text-[12px] font-data rounded-[6px] px-2 py-1 transition-colors hover:bg-[var(--mc-bg-surface-raised)]">
                      <span className="flex-1 truncate">{f.name}</span>
                      <button onClick={() => deleteGoogleFile((f.id as string) ?? '')} className="text-[var(--mc-ember-500)] transition-colors hover:text-[var(--mc-ember-400)]">
                        Delete
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Card>
        </div>
      )}
    </DashboardLayout>
  );
}
