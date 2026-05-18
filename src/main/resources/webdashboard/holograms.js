/**
 * holograms.js — Holograms dashboard page logic
 * Supports: list, stats, detail view, create, edit (lines + settings), spawn/despawn/toggle/delete.
 */
(function () {
    'use strict';

    const API = '';
    let allHolograms = [];
    let currentHoloId = null;
    let currentHoloData = null;
    let refreshTimer = null;
    const REFRESH_INTERVAL = 60_000;

    /* ── Helpers ─────────────────────────────────────────────────────────── */

    function authHeaders() {
        const token = localStorage.getItem('authToken');
        return token ? { Authorization: 'Bearer ' + token } : {};
    }

    async function apiFetch(path, options) {
        const opts = Object.assign({ headers: authHeaders() }, options || {});
        if (opts.body && typeof opts.body === 'object') {
            opts.body = JSON.stringify(opts.body);
            opts.headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers);
        }
        const resp = await fetch(API + path, opts);
        if (resp.status === 401) { window.location.href = 'index.html'; throw new Error('Unauthorized'); }
        return resp.json();
    }

    function setText(id, val) { const el = document.getElementById(id); if (el) el.textContent = val; }

    function esc(str) {
        if (str == null) return '';
        return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    function fmt(n) { return n == null ? '?' : parseFloat(n).toFixed(1); }

    function fmtArgb(argb) {
        if (!argb || argb === 0) return 'transparent';
        return '0x' + (argb >>> 0).toString(16).toUpperCase().padStart(8,'0');
    }

    function billboardName(mode) { return ['FIXED','VERTICAL','HORIZONTAL','CENTER'][mode] ?? 'UNKNOWN'; }

    /* ── Stats ───────────────────────────────────────────────────────────── */

    async function loadStats() {
        try {
            const data = await apiFetch('/api/holograms/stats');
            if (!data.success) return;
            setText('statTotal',    data.total         ?? '—');
            setText('statVisible',  data.visible       ?? '—');
            setText('statAnimated', data.animated      ?? '—');
            setText('statShop',     data.shopHolograms ?? '—');
        } catch (e) { console.warn('hologram stats error', e); }
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
            wrap.innerHTML = '<div class="empty-state"><div class="icon">\u2728</div><div>No holograms found. Click <strong>➕ Create Hologram</strong> to add one.</div></div>';
            return;
        }
        let html = '<table><thead><tr><th>ID</th><th>World</th><th>Position</th><th>Lines</th><th>Refresh</th><th>Status</th><th>Flags</th><th></th></tr></thead><tbody>';

        for (const h of holos) {
            const visiBadge = h.visible ? '<span class="visible-badge">Visible</span>' : '<span class="hidden-badge">Hidden</span>';
            const animChip = (h.lines && h.lines.some(l => l.frames && l.frames.length > 0)) ? '<span class="anim-chip">\ud83c\udf9e Anim</span> ' : '';
            const shopChip = (h.id && h.id.startsWith('shop_')) ? '<span class="shop-chip">\ud83d\uded2 Shop</span>' : '';
            const spinChip = h.spinEnabled ? '<span class="anim-chip" title="Spin">🔄</span> ' : '';
            const hoverChip = h.hoverEnabled ? '<span class="anim-chip" title="Hover">〜</span> ' : '';
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
                + '<td>' + animChip + spinChip + hoverChip + shopChip + '</td>'
                + '<td style="white-space:nowrap;">'
                + '<button class="btn-sm btn-view"   onclick="window.viewHologram(\'' + esc(h.id) + '\')">View</button>'
                + '<button class="btn-sm btn-edit"   onclick="window.viewAndEdit(\'' + esc(h.id) + '\')">Edit</button>'
                + '<button class="btn-sm btn-toggle" onclick="window.quickToggle(\'' + esc(h.id) + '\')">Toggle</button>'
                + '</td></tr>';
        }
        html += '</tbody></table>';
        wrap.innerHTML = html;
    }

    /* ── Search ──────────────────────────────────────────────────────────── */

    function onSearch(e) {
        const q = e.target.value.trim().toLowerCase();
        if (!q) { renderTable(allHolograms); return; }
        renderTable(allHolograms.filter(h => (h.id||'').toLowerCase().includes(q) || (h.world||'').toLowerCase().includes(q)));
    }

    /* ── Detail modal ────────────────────────────────────────────────────── */

    window.viewHologram = async function (id) {
        currentHoloId = id;
        currentHoloData = null;
        const modal = document.getElementById('holoModal');
        document.getElementById('holoModalTitle').textContent = '\u2728 ' + id;
        document.getElementById('holoModalGrid').innerHTML = '<div style="grid-column:1/-1;padding:1.5rem;text-align:center;"><span class="spinner"></span></div>';
        document.getElementById('holoModalLines').style.display = 'none';
        document.getElementById('holoModalAnimSection').style.display = 'none';
        modal.classList.add('open');

        try {
            const data = await apiFetch('/api/holograms/' + encodeURIComponent(id));
            if (!data.success || !data.hologram) throw new Error(data.error || 'Not found');
            const h = data.hologram;
            currentHoloData = h;

            const worldShort = (h.world || '?').replace('minecraft:', '');
            const pos = '(' + fmt(h.x) + ', ' + fmt(h.y) + ', ' + fmt(h.z) + ')';
            const lineCount = h.lineCount !== undefined ? h.lineCount : (h.lines ? h.lines.length : 0);

            const fields = [
                { label: 'ID',               val: h.id },
                { label: 'World',            val: worldShort },
                { label: 'Position',         val: pos },
                { label: 'Visible',          val: h.visible ? '✅ Yes' : '❌ No' },
                { label: 'Refresh Interval', val: h.refreshInterval > 0 ? h.refreshInterval + 's' : 'Disabled' },
                { label: 'Lines',            val: lineCount + ' line(s)' },
            ];

            document.getElementById('holoModalGrid').innerHTML = fields.map(f =>
                '<div class="detail-field"><div class="field-label">' + esc(f.label) + '</div><div class="field-val">' + esc(String(f.val)) + '</div></div>'
            ).join('');

            const animFields = [
                { label: 'Scale',        val: (h.scale ?? 1.0) + 'x' },
                { label: 'Billboard',    val: billboardName(h.billboardMode ?? 3) },
                { label: 'Line Spacing', val: (h.lineSpacing ?? 0.3) + ' blocks' },
                { label: 'Opacity',      val: (h.textOpacity ?? 255) + '/255' },
                { label: 'Shadow',       val: h.textShadow ? 'on' : 'off' },
                { label: 'Background',   val: fmtArgb(h.backgroundColorArgb) },
                { label: 'Spin',         val: h.spinEnabled ? 'on (' + h.spinSpeedDegrees + '°/t, ' + h.spinAxis + '-axis)' : 'off' },
                { label: 'Hover',        val: h.hoverEnabled ? 'on (±' + h.hoverAmplitude + 'b, ' + h.hoverSpeedDegrees + '°/t)' : 'off' },
            ];
            document.getElementById('holoModalAnimGrid').innerHTML = animFields.map(f =>
                '<div class="detail-field"><div class="field-label">' + esc(f.label) + '</div><div class="field-val">' + esc(String(f.val)) + '</div></div>'
            ).join('');
            document.getElementById('holoModalAnimSection').style.display = 'block';

            if (h.lines && h.lines.length > 0) {
                document.getElementById('holoModalLinesList').innerHTML = h.lines.map((ln, i) => {
                    const animLabel = (ln.frames && ln.frames.length > 0)
                        ? '<span class="line-anim">\ud83c\udf9e ' + ln.frames.length + ' frames / ' + ln.animFrameIntervalTicks + 't</span>' : '';
                    return '<div class="line-item"><span class="line-idx">' + i + '</span><span class="line-text">' + esc(ln.text || '') + '</span>' + animLabel + '</div>';
                }).join('');
                document.getElementById('holoModalLines').style.display = 'block';
            }
        } catch (e) {
            document.getElementById('holoModalGrid').innerHTML = '<div style="grid-column:1/-1;color:#f38ba8;padding:1rem;">Failed to load: ' + esc(e.message) + '</div>';
        }
    };

    window.viewAndEdit = async function (id) {
        await window.viewHologram(id);
        if (currentHoloData) openEditModal();
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

    /* ── CREATE MODAL ────────────────────────────────────────────────────── */

    window.openCreateModal = function () {
        document.getElementById('createId').value = '';
        document.getElementById('createX').value = '0';
        document.getElementById('createY').value = '64';
        document.getElementById('createZ').value = '0';
        document.getElementById('createWorld').value = 'minecraft:overworld';
        document.getElementById('createRefresh').value = '5';
        document.getElementById('createScale').value = '1.0';
        document.getElementById('createBillboard').value = '3';
        document.getElementById('createLineSpacing').value = '0.3';
        document.getElementById('createError').style.display = 'none';
        document.getElementById('createLinesList').innerHTML = '';
        addCreateLine('');
        document.getElementById('createHoloModal').classList.add('open');
    };

    window.closeCreateModal = function () { document.getElementById('createHoloModal').classList.remove('open'); };

    window.addCreateLine = function (text) {
        const container = document.getElementById('createLinesList');
        const idx = container.children.length;
        const div = document.createElement('div');
        div.className = 'line-editor-item';
        div.innerHTML = '<span class="line-idx" style="min-width:1.5rem;color:#6c7086;">' + idx + '</span>'
            + '<input type="text" placeholder="Line text (&a for green, {placeholders}, etc.)" value="' + esc(text || '') + '">'
            + '<button class="btn-icon-sm danger" title="Remove" onclick="this.parentElement.remove();window.renumberLines(\'createLinesList\')">✕</button>';
        container.appendChild(div);
    };

    window.submitCreate = async function () {
        const id = document.getElementById('createId').value.trim();
        const errEl = document.getElementById('createError');
        if (!id) { errEl.textContent = 'Hologram ID is required.'; errEl.style.display = 'block'; return; }
        errEl.style.display = 'none';

        const lineEls = document.getElementById('createLinesList').querySelectorAll('input[type=text]');
        const lines = Array.from(lineEls).map(el => ({ text: el.value, frames: [], animFrameIntervalTicks: 0 })).filter(l => l.text.trim() !== '');

        const payload = {
            id: id.toLowerCase(),
            world: document.getElementById('createWorld').value.trim() || 'minecraft:overworld',
            x: parseFloat(document.getElementById('createX').value),
            y: parseFloat(document.getElementById('createY').value),
            z: parseFloat(document.getElementById('createZ').value),
            visible: true,
            refreshInterval: parseInt(document.getElementById('createRefresh').value) || 5,
            scale: parseFloat(document.getElementById('createScale').value) || 1.0,
            billboardMode: parseInt(document.getElementById('createBillboard').value),
            lineSpacing: parseFloat(document.getElementById('createLineSpacing').value) || 0.3,
            textShadow: false, textOpacity: 255, backgroundColorArgb: 0,
            spinEnabled: false, spinSpeedDegrees: 3.0, spinAxis: 'Y',
            hoverEnabled: false, hoverAmplitude: 0.08, hoverSpeedDegrees: 1.5,
            lines
        };

        try {
            const resp = await apiFetch('/api/holograms/create', { method: 'POST', body: payload });
            if (!resp.success) { errEl.textContent = resp.error || 'Failed to create.'; errEl.style.display = 'block'; return; }
            closeCreateModal();
            await loadAll();
        } catch (e) { errEl.textContent = 'Error: ' + e.message; errEl.style.display = 'block'; }
    };

    /* ── EDIT MODAL ──────────────────────────────────────────────────────── */

    window.openEditModal = function () {
        const h = currentHoloData;
        if (!h) return;

        document.getElementById('editModalTitle').textContent = '✏️ Edit — ' + h.id;

        const linesContainer = document.getElementById('editLinesList');
        linesContainer.innerHTML = '';
        (h.lines || []).forEach(ln => addEditLine(ln.text || ''));

        document.getElementById('editX').value = h.x ?? 0;
        document.getElementById('editY').value = h.y ?? 64;
        document.getElementById('editZ').value = h.z ?? 0;
        document.getElementById('editRefresh').value = h.refreshInterval ?? 5;
        document.getElementById('editScale').value = h.scale ?? 1.0;
        document.getElementById('editLineSpacing').value = h.lineSpacing ?? 0.3;
        document.getElementById('editBillboard').value = String(h.billboardMode ?? 3);
        document.getElementById('editOpacity').value = h.textOpacity ?? 255;
        document.getElementById('editBackground').value = h.backgroundColorArgb ? fmtArgb(h.backgroundColorArgb) : 'transparent';
        document.getElementById('editShadow').checked = !!h.textShadow;
        document.getElementById('editSpinEnabled').checked = !!h.spinEnabled;
        document.getElementById('editSpinSpeed').value = h.spinSpeedDegrees ?? 3.0;
        document.getElementById('editSpinAxis').value = h.spinAxis ?? 'Y';
        document.getElementById('editHoverEnabled').checked = !!h.hoverEnabled;
        document.getElementById('editHoverAmplitude').value = h.hoverAmplitude ?? 0.08;
        document.getElementById('editHoverSpeed').value = h.hoverSpeedDegrees ?? 1.5;

        document.getElementById('editError').style.display = 'none';
        document.getElementById('editSuccess').style.display = 'none';
        document.getElementById('editHoloModal').classList.add('open');
    };

    window.closeEditModal = function () { document.getElementById('editHoloModal').classList.remove('open'); };

    window.addEditLine = function (text) {
        const container = document.getElementById('editLinesList');
        const idx = container.children.length;
        const div = document.createElement('div');
        div.className = 'line-editor-item';
        div.innerHTML = '<span class="line-idx" style="min-width:1.5rem;color:#6c7086;">' + idx + '</span>'
            + '<input type="text" placeholder="Line text" value="' + esc(text || '') + '">'
            + '<button class="btn-icon-sm" title="Move up" onclick="window.moveLine(this,\'editLinesList\',-1)">↑</button>'
            + '<button class="btn-icon-sm" title="Move down" onclick="window.moveLine(this,\'editLinesList\',1)">↓</button>'
            + '<button class="btn-icon-sm danger" title="Remove" onclick="this.parentElement.remove();window.renumberLines(\'editLinesList\')">✕</button>';
        container.appendChild(div);
    };

    window.renumberLines = function (containerId) {
        Array.from(document.getElementById(containerId).children).forEach((el, i) => {
            const span = el.querySelector('.line-idx');
            if (span) span.textContent = i;
        });
    };

    window.moveLine = function (btn, containerId, dir) {
        const item = btn.parentElement;
        const container = document.getElementById(containerId);
        const items = Array.from(container.children);
        const idx = items.indexOf(item);
        const target = idx + dir;
        if (target < 0 || target >= items.length) return;
        if (dir === -1) container.insertBefore(item, items[target]);
        else container.insertBefore(items[target], item);
        window.renumberLines(containerId);
    };

    window.submitEdit = async function () {
        const h = currentHoloData;
        if (!h) return;

        const errEl = document.getElementById('editError');
        const sucEl = document.getElementById('editSuccess');
        errEl.style.display = 'none';
        sucEl.style.display = 'none';

        const lineEls = document.getElementById('editLinesList').querySelectorAll('input[type=text]');
        const lines = Array.from(lineEls).map((el, i) => {
            const existing = h.lines && h.lines[i];
            return {
                lineId: existing ? existing.lineId : undefined,
                text: el.value,
                frames: existing ? (existing.frames || []) : [],
                animFrameIntervalTicks: existing ? (existing.animFrameIntervalTicks || 0) : 0
            };
        });

        let bgArgb = 0;
        const bgStr = document.getElementById('editBackground').value.trim();
        if (bgStr && bgStr !== 'transparent' && bgStr !== '0') {
            try {
                const hex = bgStr.startsWith('#') ? bgStr.substring(1) : bgStr.startsWith('0x') ? bgStr.substring(2) : bgStr;
                bgArgb = parseInt(hex.padStart(8,'0'), 16);
                if (isNaN(bgArgb)) bgArgb = 0;
            } catch (_) { bgArgb = 0; }
        }

        const payload = Object.assign({}, h, {
            x: parseFloat(document.getElementById('editX').value),
            y: parseFloat(document.getElementById('editY').value),
            z: parseFloat(document.getElementById('editZ').value),
            refreshInterval: parseInt(document.getElementById('editRefresh').value) || 0,
            scale: parseFloat(document.getElementById('editScale').value) || 1.0,
            lineSpacing: parseFloat(document.getElementById('editLineSpacing').value) || 0.3,
            billboardMode: parseInt(document.getElementById('editBillboard').value),
            textOpacity: parseInt(document.getElementById('editOpacity').value),
            backgroundColorArgb: bgArgb,
            textShadow: document.getElementById('editShadow').checked,
            spinEnabled: document.getElementById('editSpinEnabled').checked,
            spinSpeedDegrees: parseFloat(document.getElementById('editSpinSpeed').value) || 3.0,
            spinAxis: document.getElementById('editSpinAxis').value,
            hoverEnabled: document.getElementById('editHoverEnabled').checked,
            hoverAmplitude: parseFloat(document.getElementById('editHoverAmplitude').value) || 0.08,
            hoverSpeedDegrees: parseFloat(document.getElementById('editHoverSpeed').value) || 1.5,
            lines,
            entityUUIDs: [], lastRefreshMs: 0, currentSpinAngle: 0, hoverPhase: 0
        });

        try {
            const resp = await apiFetch('/api/holograms/' + encodeURIComponent(h.id), { method: 'PUT', body: payload });
            if (!resp.success) { errEl.textContent = resp.error || 'Failed to save.'; errEl.style.display = 'block'; return; }
            sucEl.textContent = '✓ Changes saved and applied!';
            sucEl.style.display = 'block';
            currentHoloData = null;
            await loadAll();
            await window.viewHologram(h.id);
        } catch (e) { errEl.textContent = 'Error: ' + e.message; errEl.style.display = 'block'; }
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

    async function loadAll() { await Promise.allSettled([loadStats(), loadHolograms()]); }

    document.addEventListener('DOMContentLoaded', () => {
        loadAll();
        resetTimer();
        document.getElementById('refreshBtn')?.addEventListener('click', () => { loadAll(); resetTimer(); });
        document.getElementById('refreshHologramsBtn')?.addEventListener('click', () => loadHolograms());
        document.getElementById('holoSearch')?.addEventListener('input', onSearch);
    });

})();

