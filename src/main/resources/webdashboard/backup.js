/**
 * backup.js — NeoEssentials Dashboard: Backup & Restore page
 * Handles all interactions with /api/backup/* endpoints.
 */
(function () {
    'use strict';

    const API = '/api/backup';
    const token = () => localStorage.getItem('authToken') || '';

    // ── Helpers ─────────────────────────────────────────────────────────────

    function authHeaders(extra) {
        return Object.assign({ 'Authorization': 'Bearer ' + token(), 'Content-Type': 'application/json' }, extra || {});
    }

    function showBanner(msg, type /* 'success' | 'error' | 'loading' */) {
        const el = document.getElementById('statusBanner');
        el.textContent = msg;
        el.className = 'status-banner ' + type;
        el.style.display = 'block';
        if (type !== 'loading') {
            setTimeout(() => { el.style.display = 'none'; }, 5000);
        }
    }

    function hideBanner() {
        document.getElementById('statusBanner').style.display = 'none';
    }

    function fmtDate(ts) {
        if (!ts) return '—';
        // ts can be epoch ms number or ISO string
        const d = typeof ts === 'number' ? new Date(ts) : new Date(ts);
        if (isNaN(d)) return '—';
        return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    }

    function fmtSize(bytes) {
        if (!bytes) return '0 B';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1048576).toFixed(2) + ' MB';
    }

    // ── Confirm Modal ────────────────────────────────────────────────────────

    let _confirmResolve = null;

    function confirm(title, message) {
        return new Promise(resolve => {
            _confirmResolve = resolve;
            document.getElementById('modalTitle').textContent = title;
            document.getElementById('modalMessage').textContent = message;
            document.getElementById('confirmModal').classList.add('open');
        });
    }

    document.getElementById('modalCancel').addEventListener('click', () => {
        document.getElementById('confirmModal').classList.remove('open');
        if (_confirmResolve) { _confirmResolve(false); _confirmResolve = null; }
    });

    document.getElementById('modalConfirm').addEventListener('click', () => {
        document.getElementById('confirmModal').classList.remove('open');
        if (_confirmResolve) { _confirmResolve(true); _confirmResolve = null; }
    });

    // ── Status cards ─────────────────────────────────────────────────────────

    async function loadStatus() {
        try {
            const res = await fetch(API + '/status', { headers: authHeaders() });
            if (!res.ok) throw new Error(res.status);
            const data = await res.json();
            document.getElementById('statCount').textContent = data.count ?? '0';
            document.getElementById('statMax').textContent   = 'max ' + (data.maxSnapshots ?? 20);
            document.getElementById('statSize').textContent  = data.totalSizeMb ?? '0.00';
            document.getElementById('statLast').textContent  = data.lastBackup ? fmtDate(data.lastBackup) : 'Never';
            document.getElementById('statDir').textContent   = data.backupDir  ?? '—';

            // Populate available target checkboxes if they don't exist in the world
            if (data.availableTargets && Array.isArray(data.availableTargets)) {
                data.availableTargets.forEach(t => {
                    const cb = document.querySelector(`.target-checks input[value="${t.key}"]`);
                    if (cb) {
                        const lbl = cb.closest('label');
                        if (lbl && !t.exists) {
                            lbl.style.opacity = '0.5';
                            lbl.title = t.path + ' does not exist on server';
                        }
                    }
                });
            }
        } catch (e) {
            console.error('[Backup] Failed to load status:', e);
        }
    }

    // ── Snapshot list ────────────────────────────────────────────────────────

    async function loadList() {
        const container = document.getElementById('tableContainer');
        container.innerHTML = '<div class="empty-state"><div class="spinner"></div> Loading…</div>';

        try {
            const res = await fetch(API + '/list', { headers: authHeaders() });
            if (!res.ok) throw new Error(res.status);
            const snapshots = await res.json();

            if (!Array.isArray(snapshots) || snapshots.length === 0) {
                container.innerHTML = '<div class="empty-state"><div class="icon">💾</div><div>No snapshots yet. Create one above!</div></div>';
                return;
            }

            const rows = snapshots.map(s => {
                const targets = s.targets
                    ? (Array.isArray(s.targets) ? s.targets : []).map(t => `<span class="tag">${escHtml(t)}</span>`).join(' ')
                    : '<span class="tag yellow">unknown</span>';
                const files = s.fileCount != null ? s.fileCount + ' files' : '';
                return `<tr>
                    <td><strong>${escHtml(s.name || s.filename)}</strong></td>
                    <td>${escHtml(s.created ? fmtDate(s.created) : '—')}</td>
                    <td>${escHtml(fmtSize(s.sizeBytes))}</td>
                    <td>${targets}</td>
                    <td><span style="color:#a6adc8;font-size:0.8rem;">${escHtml(files)}</span></td>
                    <td>
                        <div class="action-btns">
                            <button class="btn-sm btn-restore"  data-name="${escAttr(s.name)}" data-action="restore">♻️ Restore</button>
                            <button class="btn-sm btn-download" data-name="${escAttr(s.name)}" data-action="download">⬇ Download</button>
                            <button class="btn-sm btn-delete"   data-name="${escAttr(s.name)}" data-action="delete">🗑 Delete</button>
                        </div>
                    </td>
                </tr>`;
            }).join('');

            container.innerHTML = `
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Created</th>
                            <th>Size</th>
                            <th>Targets</th>
                            <th>Files</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>`;

            // Wire action buttons
            container.querySelectorAll('button[data-action]').forEach(btn => {
                btn.addEventListener('click', () => handleAction(btn.dataset.action, btn.dataset.name));
            });
        } catch (e) {
            container.innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>Failed to load snapshots: ${escHtml(String(e))}</div></div>`;
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    async function handleAction(action, name) {
        if (action === 'restore') {
            const ok = await confirm('♻️ Restore Snapshot', `Restore "${name}"?\n\nThis will overwrite live server files. A pre-restore backup will be created automatically before proceeding.`);
            if (!ok) return;
            showBanner('⏳ Restoring snapshot "' + name + '"… this may take a moment.', 'loading');
            try {
                const res = await fetch(API + '/restore', {
                    method: 'POST',
                    headers: authHeaders(),
                    body: JSON.stringify({ name })
                });
                const data = await res.json();
                if (res.ok && data.success) {
                    showBanner(`✅ Restored "${name}" — ${data.filesExtracted} files. Pre-restore backup: "${data.preRestoreBackup}".`, 'success');
                    reload();
                } else {
                    showBanner('❌ Restore failed: ' + (data.error || res.status), 'error');
                }
            } catch (e) { showBanner('❌ Network error: ' + e, 'error'); }
        }

        else if (action === 'download') {
            // Trigger browser download via hidden link
            const a = document.createElement('a');
            a.href = API + '/download?name=' + encodeURIComponent(name) + '&token=' + encodeURIComponent(token());
            a.download = name + '.zip';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            // Note: download endpoint uses token via query param — add support in endpoint for this fallback
        }

        else if (action === 'delete') {
            const ok = await confirm('🗑 Delete Snapshot', `Delete snapshot "${name}" permanently? This cannot be undone.`);
            if (!ok) return;
            try {
                const res = await fetch(API + '/delete?name=' + encodeURIComponent(name), {
                    method: 'DELETE',
                    headers: authHeaders()
                });
                const data = await res.json();
                if (res.ok && data.success) {
                    showBanner('✅ Snapshot "' + name + '" deleted.', 'success');
                    reload();
                } else {
                    showBanner('❌ Delete failed: ' + (data.error || res.status), 'error');
                }
            } catch (e) { showBanner('❌ Network error: ' + e, 'error'); }
        }
    }

    // ── Create backup ─────────────────────────────────────────────────────────

    document.getElementById('createBtn').addEventListener('click', async () => {
        const nameEl = document.getElementById('snapName');
        let name = nameEl.value.trim();
        if (!name) {
            name = 'backup-' + new Date().toISOString().slice(0, 16).replace(/[T:]/g, '-');
            nameEl.value = name;
        }
        if (!/^[a-zA-Z0-9_\-]{1,64}$/.test(name)) {
            showBanner('❌ Invalid name. Use only letters, numbers, - or _ (max 64 chars).', 'error');
            return;
        }

        const targets = [...document.querySelectorAll('.target-checks input:checked')].map(cb => cb.value);
        if (targets.length === 0) {
            showBanner('❌ Select at least one backup target.', 'error');
            return;
        }

        const btnLabel = document.getElementById('createBtnLabel');
        btnLabel.innerHTML = '<span class="spinner"></span> Creating…';
        document.getElementById('createBtn').disabled = true;
        showBanner('⏳ Creating snapshot "' + name + '"…', 'loading');

        try {
            const res = await fetch(API + '/create', {
                method: 'POST',
                headers: authHeaders(),
                body: JSON.stringify({ name, targets })
            });
            const data = await res.json();
            if (res.ok && data.success) {
                const kb = data.sizeBytes ? Math.round(data.sizeBytes / 1024) : '?';
                showBanner(`✅ Snapshot "${name}" created — ${data.fileCount ?? '?'} files, ${kb} KB in ${data.elapsedMs ?? '?'}ms.`, 'success');
                nameEl.value = '';
                reload();
            } else {
                showBanner('❌ Create failed: ' + (data.error || res.status), 'error');
            }
        } catch (e) {
            showBanner('❌ Network error: ' + e, 'error');
        } finally {
            btnLabel.textContent = '📦 Create Backup';
            document.getElementById('createBtn').disabled = false;
        }
    });

    document.getElementById('refreshBtn').addEventListener('click', () => reload());

    // ── Utilities ─────────────────────────────────────────────────────────────

    function escHtml(str) {
        return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }
    function escAttr(str) {
        return String(str || '').replace(/"/g, '&quot;');
    }

    function reload() {
        loadStatus();
        loadList();
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    reload();
})();

