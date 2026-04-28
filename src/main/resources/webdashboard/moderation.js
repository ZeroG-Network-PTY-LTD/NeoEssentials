/**
 * moderation.js — NeoEssentials Dashboard · Moderation page
 */
(function () {
    'use strict';
    const API   = window.API_BASE || '/api';
    const token = () => localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';
    const esc   = s => s ? String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;') : '';
    const el    = id => document.getElementById(id);

    let showActiveOnly = true;

    async function apiFetch(path, opts={}) {
        const r = await fetch(API + path, {
            ...opts,
            headers: { 'Content-Type':'application/json', 'Authorization':'Bearer '+token(), ...(opts.headers||{}) }
        });
        return r.json();
    }

    function banner(msg, type) {
        const b = el('statusBanner');
        b.textContent = msg; b.className = 'status-banner '+type; b.style.display = 'block';
        if (type !== 'loading') setTimeout(() => b.style.display='none', 6000);
    }

    function fmtDate(ts) { try { return new Date(ts).toLocaleString(undefined, { dateStyle:'short', timeStyle:'short' }); } catch(_) { return ts||'—'; } }

    // ── Overview ───────────────────────────────────────────────────────────────
    async function loadOverview() {
        try {
            const d = await apiFetch('/moderation/overview');
            if (d.success) {
                el('statActiveBans').textContent = d.activeBans;
                el('statTotalBans').textContent  = d.totalBans;
                el('statWarns').textContent      = d.totalWarns;
                el('statJailed').textContent     = d.jailedCount ?? '—';
            }
        } catch(e) {}
    }

    // ── Bans ───────────────────────────────────────────────────────────────────
    async function loadBans() {
        el('bansTableWrap').innerHTML = '<div class="empty-state"><div class="icon"><span class="spinner"></span></div></div>';
        const path = showActiveOnly ? '/moderation/bans/active' : '/moderation/bans';
        try {
            const d = await apiFetch(path);
            if (!d.success) { el('bansTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(d.message||d.error)}</div></div>`; return; }
            const bans = d.bans || [];
            if (!bans.length) { el('bansTableWrap').innerHTML = '<div class="empty-state"><div class="icon">✅</div><div>No bans found.</div></div>'; return; }
            let html = `<table><thead><tr><th>Player</th><th>Type</th><th>Reason</th><th>Banned By</th><th>Banned</th><th>Expires</th><th>Status</th><th class="admin-only">Actions</th></tr></thead><tbody>`;
            for (const b of bans) {
                const status = b.active ? '<span class="active-badge">Active</span>' : '<span class="inactive-badge">Pardoned</span>';
                const expires = b.permanent ? '<span class="permanent-badge">Permanent</span>' : (b.expiresAt ? '<span class="temp-badge">'+fmtDate(b.expiresAt)+'</span>' : '—');
                html += `<tr>
                    <td><strong>${esc(b.playerName||b.target)}</strong><br><span style="font-size:0.75rem;color:#6c7086;">${esc(b.target)}</span></td>
                    <td><span class="ban-type ${esc(b.type)}">${esc(b.type)}</span></td>
                    <td>${esc(b.reason)}</td>
                    <td style="color:#6c7086;">${esc(b.bannedBy)}</td>
                    <td style="font-size:0.8rem;color:#6c7086;">${fmtDate(b.bannedAt)}</td>
                    <td style="font-size:0.8rem;">${expires}</td>
                    <td>${status}</td>
                    <td class="admin-only">${b.active ? `<button class="btn-sm btn-pardon" onclick="pardon('${esc(b.id)}')">✅ Pardon</button>` : ''}</td>
                </tr>`;
            }
            html += '</tbody></table>';
            el('bansTableWrap').innerHTML = html;
            document.querySelectorAll('.admin-only').forEach(e => {
                if (!isAdmin()) e.style.display = 'none';
            });
        } catch(e) { el('bansTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(e.message)}</div></div>`; }
    }

    function isAdmin() {
        try { const p = JSON.parse(atob(token().split('.')[1])); return p.role==='admin'||p.admin===true; }
        catch(_) { return false; }
    }

    window.toggleBansView = function(btn) {
        showActiveOnly = !showActiveOnly;
        btn.textContent = showActiveOnly ? 'Show All' : 'Active Only';
        loadBans();
    };

    window.pardon = async function(banId) {
        if (!confirm('Remove this ban (pardon the player)?')) return;
        const d = await apiFetch('/moderation/ban/'+banId, { method:'DELETE' });
        banner(d.success ? '✅ Ban removed' : '❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) { loadBans(); loadOverview(); }
    };

    // ── Issue ban ──────────────────────────────────────────────────────────────
    async function issueBan() {
        const target   = el('banTarget').value.trim();
        const name     = el('banName').value.trim() || target;
        const reason   = el('banReason').value.trim() || 'No reason provided';
        const type     = el('banType').value;
        const duration = parseInt(el('banDuration').value.trim() || '0', 10);
        if (!target) { banner('Target (UUID or IP) is required', 'error'); return; }
        el('issueBanBtn').innerHTML = '<span class="spinner"></span>Banning…';
        const d = await apiFetch('/moderation/ban', { method:'POST', body:JSON.stringify({ target, playerName: name, reason, type, duration: duration > 0 ? duration : -1 }) });
        el('issueBanBtn').textContent = 'Issue Ban';
        banner(d.success ? '✅ Ban issued ('+name+')' : '❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) { el('banTarget').value=''; el('banName').value=''; el('banReason').value=''; loadBans(); loadOverview(); }
    }

    // ── Warnings ───────────────────────────────────────────────────────────────
    async function loadWarns(playerName) {
        const path = playerName ? '/moderation/warns/'+encodeURIComponent(playerName) : '/moderation/warns';
        el('warnsTableWrap').innerHTML = '<div class="empty-state"><div class="icon"><span class="spinner"></span></div></div>';
        try {
            const d = await apiFetch(path);
            if (!d.success) { el('warnsTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(d.message||d.error)}</div></div>`; return; }
            const warns = d.warns || [];
            if (!warns.length) { el('warnsTableWrap').innerHTML = '<div class="empty-state"><div class="icon">✅</div><div>No warnings found.</div></div>'; return; }
            let html = `<table><thead><tr><th>Player</th><th>Warned By</th><th>Reason</th><th>Date</th><th class="admin-only">Actions</th></tr></thead><tbody>`;
            for (const w of warns) {
                html += `<tr>
                    <td><strong>${esc(w.targetName)}</strong></td>
                    <td style="color:#6c7086;">${esc(w.warnedBy)}</td>
                    <td>${esc(w.reason)}</td>
                    <td style="font-size:0.8rem;color:#6c7086;">${fmtDate(w.timestamp)}</td>
                    <td class="admin-only"><button class="btn-sm btn-del" onclick="removeWarn('${esc(w.id)}','${esc(w.targetName)}')"></button></td>
                </tr>`;
            }
            html += '</tbody></table>';
            el('warnsTableWrap').innerHTML = html;
            document.querySelectorAll('.admin-only').forEach(e => { if (!isAdmin()) e.style.display='none'; });
        } catch(e) { el('warnsTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(e.message)}</div></div>`; }
    }

    window.removeWarn = async function(warnId, targetName) {
        if (!confirm('Remove this warning from '+targetName+'?')) return;
        const d = await apiFetch('/moderation/warn/'+warnId, { method:'DELETE', body:JSON.stringify({ targetName }) });
        banner(d.success?'✅ Warning removed':'❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) loadWarns(el('warnSearch').value.trim() || null);
    };

    // ── Init ───────────────────────────────────────────────────────────────────
    async function init() {
        await loadOverview();
        await loadBans();

        el('refreshBansBtn')?.addEventListener('click', loadBans);
        el('refreshWarnsBtn')?.addEventListener('click', () => loadWarns(null));
        el('issueBanBtn')?.addEventListener('click', issueBan);
        el('searchWarnBtn')?.addEventListener('click', () => loadWarns(el('warnSearch').value.trim()||null));
        el('warnSearch')?.addEventListener('keydown', e => { if (e.key==='Enter') loadWarns(e.target.value.trim()||null); });
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
