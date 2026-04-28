/**
 * kits.js — Kits dashboard page logic
 * Fetches /api/kits/stats and /api/kits/list, renders the table, supports
 * per-kit detail modal via /api/kits/{name}.
 */
(function () {
    'use strict';

    const API = '';        // same origin
    let allKits  = [];     // cached kit list
    let refreshTimer = null;
    const REFRESH_INTERVAL = 60_000; // 60 s

    /* ── Helpers ────────────────────────────────────────────────────────── */

    function authHeaders() {
        const token = localStorage.getItem('authToken');
        return token ? { Authorization: 'Bearer ' + token } : {};
    }

    async function apiFetch(path) {
        const resp = await fetch(API + path, { headers: authHeaders() });
        if (resp.status === 401) {
            window.location.href = 'index.html';
            throw new Error('Unauthorized');
        }
        return resp.json();
    }

    function setText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    }

    function esc(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    /* ── Stats ──────────────────────────────────────────────────────────── */

    async function loadStats() {
        try {
            const data = await apiFetch('/api/kits/stats');
            if (!data.success) return;
            setText('statTotal',    data.total   ?? '—');
            setText('statEnabled',  data.enabled ?? '—');
            setText('statCooldown', data.withCooldown    ?? '—');
            setText('statPermed',   data.withPermission  ?? '—');
            setText('statLimited',  data.withUsageLimit  ?? '—');
        } catch (e) {
            console.warn('kits stats error', e);
        }
    }

    /* ── Kit list ───────────────────────────────────────────────────────── */

    async function loadKits() {
        const wrap = document.getElementById('kitsTableWrap');
        wrap.innerHTML = '<div class="empty-state"><div class="icon"><span class="spinner"></span></div><div>Loading…</div></div>';
        try {
            const data = await apiFetch('/api/kits/list');
            allKits = data.kits || [];
            renderKitsTable(allKits);
        } catch (e) {
            wrap.innerHTML = '<div class="empty-state"><div class="icon">⚠️</div><div>Failed to load kits: ' + esc(e.message) + '</div></div>';
        }
    }

    function renderKitsTable(kits) {
        const wrap = document.getElementById('kitsTableWrap');
        if (!kits || kits.length === 0) {
            wrap.innerHTML = '<div class="empty-state"><div class="icon">🎒</div><div>No kits found.</div></div>';
            return;
        }
        let html = '<table><thead><tr>'
            + '<th>Name</th>'
            + '<th>Display Name</th>'
            + '<th>Status</th>'
            + '<th>Cooldown</th>'
            + '<th>Permission</th>'
            + '<th>Items</th>'
            + '<th>Max Uses</th>'
            + '<th></th>'
            + '</tr></thead><tbody>';

        for (const k of kits) {
            const statusBadge = k.enabled
                ? '<span class="enabled-badge">Enabled</span>'
                : '<span class="disabled-badge">Disabled</span>';

            const cooldownHtml = (k.cooldownMs > 0)
                ? '<span class="cooldown-val">⏱ ' + esc(k.cooldownDisplay) + '</span>'
                : '<span class="no-cooldown">None</span>';

            const permHtml = (k.permission)
                ? '<span class="perm-chip" title="' + esc(k.permission) + '">' + esc(k.permission) + '</span>'
                : '<span class="no-perm">—</span>';

            const maxUsesHtml = (k.maxUses > 0) ? k.maxUses : '<span style="color:#6c7086">∞</span>';

            html += '<tr>'
                + '<td><strong>' + esc(k.name) + '</strong></td>'
                + '<td>' + (k.displayName ? esc(k.displayName) : '<span style="color:#6c7086">—</span>') + '</td>'
                + '<td>' + statusBadge + '</td>'
                + '<td>' + cooldownHtml + '</td>'
                + '<td>' + permHtml + '</td>'
                + '<td style="text-align:center;">' + (k.itemCount ?? 0) + '</td>'
                + '<td style="text-align:center;">' + maxUsesHtml + '</td>'
                + '<td><button class="btn-sm btn-view" onclick="window.viewKit(' + JSON.stringify(esc(k.name)) + ')">🔍 View</button></td>'
                + '</tr>';
        }
        html += '</tbody></table>';
        wrap.innerHTML = html;
    }

    /* ── Search filter ──────────────────────────────────────────────────── */

    function onSearch(e) {
        const q = e.target.value.trim().toLowerCase();
        if (!q) {
            renderKitsTable(allKits);
            return;
        }
        const filtered = allKits.filter(k =>
            (k.name        || '').toLowerCase().includes(q) ||
            (k.displayName || '').toLowerCase().includes(q) ||
            (k.description || '').toLowerCase().includes(q) ||
            (k.permission  || '').toLowerCase().includes(q)
        );
        renderKitsTable(filtered);
    }

    /* ── Kit detail modal ───────────────────────────────────────────────── */

    window.viewKit = async function (kitName) {
        const modal = document.getElementById('kitModal');
        document.getElementById('kitModalTitle').textContent = '🎒 ' + kitName;
        document.getElementById('kitModalDesc').style.display = 'none';
        document.getElementById('kitModalGrid').innerHTML = '<div style="padding:1.5rem;text-align:center;"><span class="spinner"></span></div>';
        modal.classList.add('open');

        try {
            const data = await apiFetch('/api/kits/' + encodeURIComponent(kitName));
            if (!data.success || !data.kit) throw new Error(data.error || 'Not found');
            const k = data.kit;

            // Description
            if (k.description) {
                const descEl = document.getElementById('kitModalDesc');
                descEl.textContent = k.description;
                descEl.style.display = 'block';
            }

            // Detail grid
            const fields = [
                { label: 'Name',       val: k.name },
                { label: 'Display Name', val: k.displayName || '—' },
                { label: 'Status',     val: k.enabled ? '✅ Enabled' : '❌ Disabled' },
                { label: 'Cooldown',   val: k.cooldownDisplay || 'None' },
                { label: 'Permission', val: k.permission || 'None (open to all)' },
                { label: 'Items',      val: (k.itemCount ?? 0) + ' item stack(s)' },
                { label: 'Max Uses',   val: k.maxUses > 0 ? k.maxUses + ' times' : 'Unlimited' },
            ];

            let gridHtml = '';
            for (const f of fields) {
                gridHtml += '<div class="kit-detail-field">'
                    + '<div class="field-label">' + esc(f.label) + '</div>'
                    + '<div class="field-val">' + esc(f.val) + '</div>'
                    + '</div>';
            }
            document.getElementById('kitModalGrid').innerHTML = gridHtml;
        } catch (e) {
            document.getElementById('kitModalGrid').innerHTML =
                '<div style="color:#f38ba8;padding:1rem;">Failed to load kit: ' + esc(e.message) + '</div>';
        }
    };

    /* ── Timer + init ───────────────────────────────────────────────────── */

    function resetTimer() {
        clearInterval(refreshTimer);
        let remaining = REFRESH_INTERVAL / 1000;
        const badge = document.getElementById('autoRefreshBadge');
        if (badge) badge.textContent = 'Auto-refresh: ' + remaining + 's';
        refreshTimer = setInterval(() => {
            remaining -= 1;
            if (badge) badge.textContent = 'Auto-refresh: ' + remaining + 's';
            if (remaining <= 0) {
                loadAll();
                remaining = REFRESH_INTERVAL / 1000;
            }
        }, 1000);
    }

    async function loadAll() {
        await Promise.allSettled([loadStats(), loadKits()]);
    }

    document.addEventListener('DOMContentLoaded', () => {
        loadAll();
        resetTimer();

        document.getElementById('refreshBtn')?.addEventListener('click', () => { loadAll(); resetTimer(); });
        document.getElementById('refreshKitsBtn')?.addEventListener('click', () => { loadKits(); });
        document.getElementById('kitSearch')?.addEventListener('input', onSearch);
    });

})();

