/**
 * holograms.js — Holograms dashboard page logic
 * Fetches /api/holograms/stats and /api/holograms/list, renders the table,
 * supports per-hologram detail modal with spawn/despawn/toggle/delete actions.
 */
(function () {
    'use strict';

    const API = '';
    let allHolograms = [];
    let currentHoloId = null;
    let refreshTimer = null;
    const REFRESH_INTERVAL = 60_000;

    /* ── Helpers ─────────────────────────────────────────────────────────── */

    function authHeaders() {
        const token = localStorage.getItem('authToken');
        return token ? { Authorization: 'Bearer ' + token } : {};
    }

    async function apiFetch(path, options) {
        const opts = Object.assign({ headers: authHeaders() }, options || {});
        const resp = await fetch(API + path, opts);
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

    function fmt(n) {
        if (n == null) return '?';
        return parseFloat(n).toFixed(1);
    }

    /* ── Stats ───────────────────────────────────────────────────────────── */

    async function loadStats() {
        try {
            const data = await apiFetch('/api/holograms/stats');
            if (!data.success) return;
            setText('statTotal',    data.total         ?? '—');
            setText('statVisible',  data.visible       ?? '—');
            setText('statAnimated', data.animated      ?? '—');
            setText('statShop',     data.shopHolograms ?? '—');
        } catch (e) {
            console.warn('hologram stats error', e);
        }
    }

    /* ── Hologram list ───────────────────────────────────────────────────── */

    async function loadHolograms() {
        const wrap = document.getElementById('hologramsTableWrap');
        wrap.innerHTML = '<div class="empty-state"><div class="icon"><span class="spinner"></span></div><div>Loading\u2026</div></div>';
        try {
            const data = await apiFetch('/api/holograms/list');
            allHolograms = data.holograms || [];
            renderTable(allHolograms);
        } catch (e) {
            wrap.innerHTML = '<div class="empty-state"><div class="icon">\u26a0\ufe0f</div><div>Failed to load holograms: ' + esc(e.message) + '</div></div>';
        }
    }

    function renderTable(holos) {
        const wrap = document.getElementById('hologramsTableWrap');
        if (!holos || holos.length === 0) {
            wrap.innerHTML = '<div class="empty-state"><div class="icon">\u2728</div><div>No holograms found.</div></div>';
            return;
        }
        let html = '<table><thead><tr>'
            + '<th>ID</th><th>World</th><th>Position</th>'
            + '<th>Lines</th><th>Refresh</th><th>Status</th><th>Flags</th><th></th>'
            + '</tr></thead><tbody>';

        for (const h of holos) {
            const visiBadge = h.visible
                ? '<span class="visible-badge">Visible</span>'
                : '<span class="hidden-badge">Hidden</span>';
            const animChip = (h.lines && h.lines.some(l => l.frames && l.frames.length > 0))
                ? '<span class="anim-chip">\ud83c\udf9e Anim</span> ' : '';
            const shopChip = (h.id && h.id.startsWith('shop_'))
                ? '<span class="shop-chip">\ud83d\uded2 Shop</span>' : '';
            const pos = '(' + fmt(h.x) + ', ' + fmt(h.y) + ', ' + fmt(h.z) + ')';
            const refreshStr = h.refreshInterval > 0 ? h.refreshInterval + 's' : '<span style="color:#6c7086">off</span>';
            const worldShort = (h.world || '?').replace('minecraft:', '');
            const lineCount = h.lineCount !== undefined ? h.lineCount : (h.lines ? h.lines.length : 0);

            html += '<tr>'
                + '<td><strong>' + esc(h.id) + '</strong></td>'
                + '<td>' + esc(worldShort) + '</td>'
                + '<td style="font-family:monospace;font-size:0.8rem;">' + esc(pos) + '</td>'
                + '<td style="text-align:center;">' + lineCount + '</td>'
                + '<td>' + refreshStr + '</td>'
                + '<td>' + visiBadge + '</td>'
                + '<td>' + animChip + shopChip + '</td>'
                + '<td>'
                + '<button class="btn-sm btn-view"   onclick="window.viewHologram(\'' + esc(h.id) + '\')">View</button>'
                + '<button class="btn-sm btn-toggle" onclick="window.quickToggle(\'' + esc(h.id) + '\')">Toggle</button>'
                + '</td>'
                + '</tr>';
        }
        html += '</tbody></table>';
        wrap.innerHTML = html;
    }

    /* ── Search filter ───────────────────────────────────────────────────── */

    function onSearch(e) {
        const q = e.target.value.trim().toLowerCase();
        if (!q) { renderTable(allHolograms); return; }
        renderTable(allHolograms.filter(h =>
            (h.id    || '').toLowerCase().includes(q) ||
            (h.world || '').toLowerCase().includes(q)
        ));
    }

    /* ── Hologram detail modal ───────────────────────────────────────────── */

    window.viewHologram = async function (id) {
        currentHoloId = id;
        const modal = document.getElementById('holoModal');
        document.getElementById('holoModalTitle').textContent = '\u2728 ' + id;
        document.getElementById('holoModalGrid').innerHTML =
            '<div style="grid-column:1/-1;padding:1.5rem;text-align:center;"><span class="spinner"></span></div>';
        document.getElementById('holoModalLines').style.display = 'none';
        modal.classList.add('open');

        try {
            const data = await apiFetch('/api/holograms/' + encodeURIComponent(id));
            if (!data.success || !data.hologram) throw new Error(data.error || 'Not found');
            const h = data.hologram;

            const worldShort = (h.world || '?').replace('minecraft:', '');
            const pos = '(' + fmt(h.x) + ', ' + fmt(h.y) + ', ' + fmt(h.z) + ')';
            const lineCount = h.lineCount !== undefined ? h.lineCount : (h.lines ? h.lines.length : 0);

            const fields = [
                { label: 'ID',               val: h.id },
                { label: 'World',            val: worldShort },
                { label: 'Position',         val: pos },
                { label: 'Visible',          val: h.visible ? '\u2705 Yes' : '\u274c No' },
                { label: 'Refresh Interval', val: h.refreshInterval > 0 ? h.refreshInterval + 's' : 'Disabled' },
                { label: 'Lines',            val: lineCount + ' line(s)' },
            ];

            let gridHtml = '';
            for (const f of fields) {
                gridHtml += '<div class="detail-field">'
                    + '<div class="field-label">' + esc(f.label) + '</div>'
                    + '<div class="field-val">' + esc(f.val) + '</div>'
                    + '</div>';
            }
            document.getElementById('holoModalGrid').innerHTML = gridHtml;

            if (h.lines && h.lines.length > 0) {
                let lHtml = '';
                h.lines.forEach((ln, i) => {
                    const animLabel = (ln.frames && ln.frames.length > 0)
                        ? '<span class="line-anim">\ud83c\udf9e ' + ln.frames.length + ' frames</span>' : '';
                    lHtml += '<div class="line-item">'
                        + '<span class="line-idx">' + i + '</span>'
                        + '<span class="line-text">' + esc(ln.text || '') + '</span>'
                        + animLabel + '</div>';
                });
                document.getElementById('holoModalLinesList').innerHTML = lHtml;
                document.getElementById('holoModalLines').style.display = 'block';
            }
        } catch (e) {
            document.getElementById('holoModalGrid').innerHTML =
                '<div style="grid-column:1/-1;color:#f38ba8;padding:1rem;">Failed to load: ' + esc(e.message) + '</div>';
        }
    };

    window.quickToggle = async function (id) {
        try {
            await apiFetch('/api/holograms/' + encodeURIComponent(id) + '/visible', { method: 'POST' });
            await loadAll();
        } catch (e) { console.warn('toggle error', e); }
    };

    window.holoAction = async function (action) {
        if (!currentHoloId) return;
        try {
            if (action === 'delete') {
                if (!confirm('Delete hologram "' + currentHoloId + '"?')) return;
                await apiFetch('/api/holograms/' + encodeURIComponent(currentHoloId), { method: 'DELETE' });
                document.getElementById('holoModal').classList.remove('open');
                await loadAll();
            } else if (action === 'visible') {
                await apiFetch('/api/holograms/' + encodeURIComponent(currentHoloId) + '/visible', { method: 'POST' });
                await loadAll();
                await window.viewHologram(currentHoloId);
            } else {
                await apiFetch('/api/holograms/' + encodeURIComponent(currentHoloId) + '/' + action, { method: 'POST' });
            }
        } catch (e) { alert('Action failed: ' + e.message); }
    };

    /* ── Timer + init ────────────────────────────────────────────────────── */

    function resetTimer() {
        clearInterval(refreshTimer);
        let remaining = REFRESH_INTERVAL / 1000;
        const badge = document.getElementById('autoRefreshBadge');
        refreshTimer = setInterval(() => {
            remaining -= 1;
            if (badge) badge.textContent = 'Auto-refresh: ' + remaining + 's';
            if (remaining <= 0) { loadAll(); remaining = REFRESH_INTERVAL / 1000; }
        }, 1000);
    }

    async function loadAll() {
        await Promise.allSettled([loadStats(), loadHolograms()]);
    }

    document.addEventListener('DOMContentLoaded', () => {
        loadAll();
        resetTimer();
        document.getElementById('refreshBtn')?.addEventListener('click', () => { loadAll(); resetTimer(); });
        document.getElementById('refreshHologramsBtn')?.addEventListener('click', () => loadHolograms());
        document.getElementById('holoSearch')?.addEventListener('input', onSearch);
    });

})();

