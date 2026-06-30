/**
 * discord.js — NeoEssentials Dashboard · Discord Integration page
 * Hooks into /api/discord/* endpoints provided by DiscordEndpoint.java
 */
(function () {
    'use strict';

    const API   = window.API_BASE || '/api';
    const token = () => localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';
    const isAdmin = () => {
        try {
            const p = JSON.parse(atob(token().split('.')[1]));
            return p.role === 'admin' || p.admin === true;
        } catch (_) { return false; }
    };

    // ── DOM refs ─────────────────────────────────────────────────────────────
    const el = id => document.getElementById(id);

    const statStatus     = el('statStatus');
    const statStatusSub  = el('statStatusSub');
    const statAdapters   = el('statAdapters');
    const statEvents     = el('statEvents');
    const adapterGrid    = el('adapterGrid');
    const noAdapters     = el('noAdapters');
    const eventContainer = el('eventTableContainer');
    const eventCountLabel= el('eventCountLabel');
    const statusBanner   = el('statusBanner');
    const testPanel      = el('testPanel');

    // ── Helpers ───────────────────────────────────────────────────────────────
    function showBanner(msg, type) {
        statusBanner.textContent = msg;
        statusBanner.className   = 'status-banner ' + type;
        statusBanner.style.display = 'block';
        if (type !== 'loading') setTimeout(() => { statusBanner.style.display = 'none'; }, 6000);
    }
    function hideBanner() { statusBanner.style.display = 'none'; }

    async function apiFetch(path, options = {}) {
        const resp = await fetch(API + path, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token(),
                ...(options.headers || {})
            }
        });
        return resp.json();
    }

    function formatTimestamp(iso) {
        if (!iso) return '—';
        try {
            return new Date(iso).toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'medium' });
        } catch (_) { return iso; }
    }

    function typeBadge(type) {
        const map = {
            chat: ['💬', 'type-chat'],
            join: ['➡️',  'type-join'],
            quit: ['⬅️',  'type-quit'],
            mute: ['🔇', 'type-mute'],
            afk:  ['💤', 'type-afk'],
            pm:   ['📩', 'type-pm'],
            test: ['🔔', 'type-test'],
        };
        const [icon, cls] = map[type] || ['❓', ''];
        return `<span class="type-badge ${cls}">${icon} ${type}</span>`;
    }

    function escHtml(s) {
        if (!s) return '';
        return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    // ── Load status ───────────────────────────────────────────────────────────
    async function loadStatus() {
        try {
            const data = await apiFetch('/discord/status');
            if (!data.success) throw new Error(data.error || 'Unknown error');

            const adapters = data.adapters || [];
            const active   = adapters.filter(a => a.enabled);

            statStatus.textContent    = data.anyActive ? '🟢 Active' : '🔴 Inactive';
            statStatusSub.textContent = data.anyActive ? 'adapter(s) connected' : 'no adapters loaded';
            statAdapters.textContent  = active.length + ' / ' + adapters.length;
            statEvents.textContent    = data.eventCount != null ? data.eventCount : '—';

            // Render adapter cards
            adapterGrid.innerHTML = '';
            if (adapters.length === 0) {
                adapterGrid.style.display = 'none';
                noAdapters.style.display  = 'block';
            } else {
                adapterGrid.style.display = '';
                noAdapters.style.display  = 'none';
                adapters.forEach(a => {
                    const div = document.createElement('div');
                    div.className = 'adapter-card';
                    const icon = adapterIcon(a.name);
                    const badge = a.enabled
                        ? '<span class="adapter-badge active">Active</span>'
                        : '<span class="adapter-badge inactive">Inactive</span>';
                    div.innerHTML = `
                        <div class="adapter-icon">${icon}</div>
                        <div class="adapter-info">
                            <div class="adapter-name">${escHtml(a.name)}</div>
                            <div class="adapter-desc">${adapterDesc(a.name)}</div>
                        </div>
                        ${badge}
                    `;
                    adapterGrid.appendChild(div);
                });
            }
        } catch (err) {
            statStatus.textContent = '⚠️ Error';
            statStatusSub.textContent = err.message;
        }
    }

    function adapterIcon(name) {
        if (!name) return '🔌';
        const n = name.toLowerCase();
        if (n.includes('sdlink') || n.includes('simple'))   return '🔗';
        if (n.includes('discord') && n.includes('srvsrv'))  return '📡';
        if (n.includes('discordsrv'))                        return '📡';
        if (n.includes('dcintegration') || n.includes('dc')) return '🤖';
        return '🔌';
    }

    function adapterDesc(name) {
        if (!name) return '';
        const n = name.toLowerCase();
        if (n.includes('simple') || n.includes('sdlink')) return 'Simple Discord Link by hypherionsa';
        if (n.includes('discordsrv'))                      return 'DiscordSRV (v1/v2)';
        if (n.includes('dcintegration') || n.includes('dc')) return 'DCIntegration by ErdbeerbaerLP';
        return 'Custom adapter';
    }

    // ── Load events ───────────────────────────────────────────────────────────
    async function loadEvents(limit = 100) {
        try {
            const data = await apiFetch(`/discord/events?limit=${limit}`);
            if (!data.success) throw new Error(data.error || 'Unknown error');

            const events = data.events || [];
            eventCountLabel.textContent = `(${data.total} total)`;

            if (events.length === 0) {
                eventContainer.innerHTML = `
                    <div class="empty-state">
                        <div class="icon">📭</div>
                        <div>No relay events yet — events appear here after players join, chat, etc.</div>
                    </div>`;
                return;
            }

            let html = `<table>
                <thead><tr>
                    <th style="width:120px;">Type</th>
                    <th>Player</th>
                    <th>Target</th>
                    <th>Channel</th>
                    <th>Message</th>
                    <th style="width:140px;">Time</th>
                </tr></thead>
                <tbody>`;

            events.forEach(e => {
                html += `<tr>
                    <td>${typeBadge(e.type)}</td>
                    <td>${escHtml(e.actor)}</td>
                    <td>${e.target ? escHtml(e.target) : '<span style="color:#6c7086;">—</span>'}</td>
                    <td><code style="font-size:0.82rem;">#${escHtml(e.channel)}</code></td>
                    <td title="${escHtml(e.message)}">${escHtml(e.message ? (e.message.length > 60 ? e.message.substring(0,60)+'…' : e.message) : '')}</td>
                    <td style="color:#6c7086;font-size:0.8rem;">${formatTimestamp(e.timestamp)}</td>
                </tr>`;
            });

            html += '</tbody></table>';
            eventContainer.innerHTML = html;

        } catch (err) {
            eventContainer.innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>Failed to load events: ${escHtml(err.message)}</div></div>`;
        }
    }

    // ── Test message ──────────────────────────────────────────────────────────
    async function sendTest() {
        const channel = (el('testChannel')?.value || 'chat').trim();
        const message = (el('testMessage')?.value || '🔔 Test message from NeoEssentials Dashboard').trim();

        if (!message) { showBanner('Please enter a message.', 'error'); return; }

        const btn = el('sendTestBtn');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>Sending…';
        showBanner('Sending test message…', 'loading');

        try {
            const data = await apiFetch('/discord/test', {
                method: 'POST',
                body: JSON.stringify({ channel, message })
            });
            if (data.success) {
                showBanner('✅ ' + (data.message || 'Test message sent!'), 'success');
                await loadEvents();
            } else {
                showBanner('❌ ' + (data.error || 'Failed to send test'), 'error');
            }
        } catch (err) {
            showBanner('❌ ' + err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Send Test';
        }
    }

    // ── Clear events ──────────────────────────────────────────────────────────
    async function clearEvents() {
        if (!confirm('Clear the entire Discord relay event log? This cannot be undone.')) return;
        try {
            const data = await apiFetch('/discord/events', { method: 'DELETE' });
            if (data.success) {
                showBanner('✅ Event log cleared.', 'success');
                await loadEvents();
                await loadStatus();
            } else {
                showBanner('❌ ' + (data.error || 'Could not clear log'), 'error');
            }
        } catch (err) {
            showBanner('❌ ' + err.message, 'error');
        }
    }

    // ── Admin visibility ──────────────────────────────────────────────────────
    function applyAdminVisibility() {
        const admin = isAdmin();
        document.querySelectorAll('.admin-only').forEach(el => {
            el.style.display = admin ? '' : 'none';
        });
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    async function init() {
        applyAdminVisibility();
        await loadStatus();
        await loadEvents();

        el('refreshEventsBtn')?.addEventListener('click', async () => {
            await loadStatus();
            await loadEvents();
        });
        el('clearEventsBtn')?.addEventListener('click', clearEvents);
        el('sendTestBtn')?.addEventListener('click', sendTest);

        // Auto-refresh every 30 s
        setInterval(async () => {
            await loadStatus();
            await loadEvents();
        }, 30_000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

