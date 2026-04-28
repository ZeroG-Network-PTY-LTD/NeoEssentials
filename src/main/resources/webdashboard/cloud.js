/**
 * cloud.js — NeoEssentials Dashboard · Cloud Storage page
 */
(function () {
    'use strict';
    const API   = window.API_BASE || '/api';
    const token = () => localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';
    const isAdmin = () => {
        try { const p = JSON.parse(atob(token().split('.')[1])); return p.role==='admin'||p.admin===true; }
        catch(_) { return false; }
    };
    const esc = s => s ? String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;') : '';
    const el  = id => document.getElementById(id);
    let currentFilesProvider = 'dropbox';

    async function apiFetch(path, opts={}) {
        const r = await fetch(API + path, {
            ...opts,
            headers: { 'Content-Type':'application/json', 'Authorization':'Bearer '+token(), ...(opts.headers||{}) }
        });
        return r.json();
    }

    function banner(msg, type) {
        const b = el('statusBanner');
        b.textContent = msg;
        b.className = 'status-banner '+type;
        b.style.display = 'block';
        if (type!=='loading') setTimeout(() => b.style.display='none', 6000);
    }

    function fmtBytes(mb) {
        if (mb === undefined || mb === null) return '—';
        if (mb >= 1024) return (mb/1024).toFixed(1)+' GB';
        return mb+' MB';
    }

    // ── Load status ─────────────────────────────────────────────────────────
    async function loadStatus() {
        try {
            const d = await apiFetch('/cloud/status');
            if (!d.success) return;

            // Dropbox
            const dbx = d.providers?.dropbox || {};
            const dbxEl = el('dbxStatus');
            if (dbx.connected) {
                dbxEl.textContent = '✓ Connected'; dbxEl.className = 'provider-status connected';
                el('dbxMeta').innerHTML = `Upload path: <code>${esc(dbx.uploadPath)}</code> · Token: <code>${esc(dbx.tokenMasked)}</code>`;
                const used = dbx.quotaUsedMB, total = dbx.quotaTotalMB;
                if (used !== undefined && total !== undefined && total > 0) {
                    el('dbxQuota').style.display = 'block';
                    el('dbxQuotaLabel').textContent = fmtBytes(used)+' / '+fmtBytes(total);
                    el('dbxQuotaFill').style.width = Math.min(100, Math.round(used/total*100))+'%';
                }
            } else if (dbx.configured) {
                dbxEl.textContent = '⚠ Error'; dbxEl.className = 'provider-status configured';
                el('dbxMeta').innerHTML = 'Configured but connection failed. ' + (dbx.error ? esc(dbx.error) : '');
            } else {
                dbxEl.textContent = 'Not Configured'; dbxEl.className = 'provider-status unconfigured';
            }

            // Google Drive
            const gd = d.providers?.googleDrive || {};
            const gdEl = el('gdStatus');
            if (gd.connected) {
                gdEl.textContent = '✓ Connected'; gdEl.className = 'provider-status connected';
                el('gdMeta').innerHTML = `Client ID: <code>${esc(gd.clientId)}</code>` + (gd.folderId ? ` · Folder: <code>${esc(gd.folderId)}</code>` : '');
                const used = gd.quotaUsedMB, total = gd.quotaTotalMB;
                if (used !== undefined && total !== undefined && total > 0) {
                    el('gdQuota').style.display = 'block';
                    el('gdQuotaLabel').textContent = fmtBytes(used)+' / '+fmtBytes(total);
                    el('gdQuotaFill').style.width = Math.min(100, Math.round(used/total*100))+'%';
                }
            } else if (gd.configured) {
                gdEl.textContent = '⚠ Error'; gdEl.className = 'provider-status configured';
                el('gdMeta').innerHTML = 'Configured but connection failed. ' + (gd.error ? esc(gd.error) : '');
            } else {
                gdEl.textContent = 'Not Configured'; gdEl.className = 'provider-status unconfigured';
            }
        } catch(e) { console.error('Cloud status error', e); }
    }

    // ── Load cloud config ────────────────────────────────────────────────────
    async function loadConfig() {
        try {
            const d = await apiFetch('/cloud/config');
            if (!d.success) return;
            if (d.dropbox) {
                if (d.dropbox.uploadPath) el('dbxPath').value = d.dropbox.uploadPath;
            }
            if (d.googleDrive) {
                if (d.googleDrive.clientId) el('gdClientId').value = d.googleDrive.clientId;
                if (d.googleDrive.folderId) el('gdFolderId').value = d.googleDrive.folderId;
            }
        } catch(e) {}
    }

    // ── Save configs ─────────────────────────────────────────────────────────
    async function saveDropbox() {
        const data = { accessToken: el('dbxToken').value.trim(), uploadPath: el('dbxPath').value.trim()||'/NeoEssentials-Backups' };
        if (!data.accessToken) { banner('Please enter a Dropbox access token', 'error'); return; }
        el('saveDropboxBtn').innerHTML = '<span class="spinner"></span>Saving…';
        const d = await apiFetch('/cloud/config/dropbox', { method:'POST', body:JSON.stringify(data) });
        el('saveDropboxBtn').textContent = ' Save Dropbox Config';
        if (d.success) { banner('✅ Dropbox configured', 'success'); loadStatus(); }
        else banner('❌ '+d.message, 'error');
    }

    async function saveGoogle() {
        const data = {
            clientId:      el('gdClientId').value.trim(),
            clientSecret:  el('gdClientSecret').value.trim(),
            refreshToken:  el('gdRefreshToken').value.trim(),
            folderId:      el('gdFolderId').value.trim()
        };
        if (!data.clientId || !data.clientSecret || !data.refreshToken) { banner('Client ID, Client Secret, and Refresh Token are required', 'error'); return; }
        el('saveGoogleBtn').innerHTML = '<span class="spinner"></span>Saving…';
        const d = await apiFetch('/cloud/config/google', { method:'POST', body:JSON.stringify(data) });
        el('saveGoogleBtn').textContent = ' Save Google Drive Config';
        if (d.success) { banner('✅ Google Drive configured', 'success'); loadStatus(); }
        else banner('❌ '+d.message, 'error');
    }

    // ── Test connections ─────────────────────────────────────────────────────
    async function testDropbox() {
        banner('Testing Dropbox connection…', 'loading');
        const d = await apiFetch('/cloud/test/dropbox', { method:'POST', body:'{}' });
        banner(d.success ? '✅ '+d.message : '❌ '+(d.message||d.error), d.success?'success':'error');
    }

    async function testGoogle() {
        banner('Testing Google Drive connection…', 'loading');
        const d = await apiFetch('/cloud/test/google', { method:'POST', body:'{}' });
        banner(d.success ? '✅ '+d.message : '❌ '+(d.message||d.error), d.success?'success':'error');
    }

    // ── Load files ───────────────────────────────────────────────────────────
    window.loadCloudFiles = async function(provider) {
        currentFilesProvider = provider;
        el('filesContainer').innerHTML = '<div class="empty-state"><div class="icon"><span class="spinner"></span></div><div>Loading files…</div></div>';
        try {
            const d = await apiFetch('/cloud/files/' + (provider === 'google' ? 'google' : 'dropbox'));
            if (!d.success) {
                el('filesContainer').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(d.message || d.error)}</div></div>`;
                return;
            }
            const files = d.files || [];
            if (!files.length) {
                el('filesContainer').innerHTML = '<div class="empty-state"><div class="icon"></div><div>No files uploaded yet.</div></div>';
                return;
            }
            let html = `<table><thead><tr><th>Name</th><th>Size</th><th>Uploaded</th><th class="admin-only">Actions</th></tr></thead><tbody>`;
            for (const f of files) {
                const name = provider === 'google' ? (f.name||'') : (f.path_display||f.name||'');
                const size = fmtBytes(Math.round((f.size||0)/1048576));
                const date = provider === 'google' ? (f.createdTime||'') : (f.client_modified||f.server_modified||'');
                const id   = provider === 'google' ? f.id : (f.path_display||f.id||'');
                html += `<tr>
                    <td> ${esc(name)}</td>
                    <td>${esc(size)}</td>
                    <td style="color:#6c7086;font-size:0.8rem;">${esc(date ? new Date(date).toLocaleString() : '')}</td>
                    <td class="admin-only"><button class="btn-sm btn-del" onclick="deleteCloudFile('${esc(provider)}','${esc(id)}')"></button></td>
                </tr>`;
            }
            html += '</tbody></table>';
            el('filesContainer').innerHTML = html;
            // Hide admin-only cols if not admin
            if (!isAdmin()) document.querySelectorAll('.admin-only').forEach(e => e.style.display='none');
        } catch(e) {
            el('filesContainer').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(e.message)}</div></div>`;
        }
    };

    window.deleteCloudFile = async function(provider, fileId) {
        if (!confirm('Delete this file from '+provider+'? This cannot be undone.')) return;
        const path = provider==='google'
            ? '/cloud/files/google/'+encodeURIComponent(fileId)
            : '/cloud/files/dropbox/'+encodeURIComponent(fileId);
        const d = await apiFetch(path, { method:'DELETE' });
        banner(d.success ? '✅ '+d.message : '❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) loadCloudFiles(provider);
    };

    // ── Init ─────────────────────────────────────────────────────────────────
    function applyAdminVis() {
        const admin = isAdmin();
        document.querySelectorAll('.admin-only').forEach(e => e.style.display = admin ? '' : 'none');
    }

    async function init() {
        applyAdminVis();
        await loadStatus();
        if (isAdmin()) await loadConfig();
        await loadCloudFiles('dropbox');

        el('dbxTestBtn')?.addEventListener('click', testDropbox);
        el('gdTestBtn')?.addEventListener('click', testGoogle);
        el('saveDropboxBtn')?.addEventListener('click', saveDropbox);
        el('saveGoogleBtn')?.addEventListener('click', saveGoogle);
        el('refreshFilesBtn')?.addEventListener('click', () => loadCloudFiles(currentFilesProvider));
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
