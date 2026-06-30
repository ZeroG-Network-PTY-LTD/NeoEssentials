/**
 * stats.js — NeoEssentials Dashboard: Statistics page
 * Drives the Performance, Economy, and Activity tabs.
 */
(function () {
    'use strict';

    const API   = '/api/stats';
    const token = () => localStorage.getItem('authToken') || '';
    const hdrs  = () => ({ 'Authorization': 'Bearer ' + token(), 'Content-Type': 'application/json' });

    // ── Chart.js defaults ────────────────────────────────────────────────────
    Chart.defaults.color          = '#a6adc8';
    Chart.defaults.font.family    = 'Inter, sans-serif';
    Chart.defaults.font.size      = 11;
    Chart.defaults.plugins.legend.display = false;

    let tpsChartInstance  = null;
    let memChartInstance  = null;
    let baltopChartInst   = null;
    let distChartInst     = null;

    // ── Tabs ─────────────────────────────────────────────────────────────────
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
            btn.classList.add('active');
            const panel = document.getElementById('tab-' + btn.dataset.tab);
            if (panel) panel.classList.add('active');
        });
    });

    // ── Fetch helpers ─────────────────────────────────────────────────────────
    async function fetchJSON(url) {
        const res = await fetch(url, { headers: hdrs() });
        if (!res.ok) throw new Error(res.status + ' ' + res.statusText);
        return res.json();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    function setText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val ?? '—';
    }

    function tpsColor(tps) {
        if (tps >= 19)  return '#a6e3a1';
        if (tps >= 15)  return '#f9e2af';
        return '#f38ba8';
    }

    function memColor(pct) {
        if (pct < 60) return '#a6e3a1';
        if (pct < 80) return '#f9e2af';
        return '#f38ba8';
    }

    function esc(s) {
        return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }

    function fmtNum(n, dec = 0) {
        if (n == null) return '—';
        return Number(n).toFixed(dec).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }

    // ── ① Performance ─────────────────────────────────────────────────────────
    async function loadPerf() {
        const d = await fetchJSON(API + '/performance');

        const tps = Number(d.tps || 0);
        const mem = Number(d.memPercent || 0);

        setText('perfTps',     d.tps ?? '—');
        setText('perfTick',    (d.tickMs ?? '—') + ' ms');
        setText('perfPlayers', d.players ?? '—');
        setText('perfPlayersMax', '/ ' + (d.playersMax ?? '—') + ' max');
        setText('perfMem',     (d.memUsedMb ?? '—') + ' MB');
        setText('perfMemMax',  '/ ' + (d.memMaxMb ?? '—') + ' MB max');
        setText('perfCpu',     d.cpuCores ?? '—');
        setText('perfLoad',    'load avg ' + (d.loadAvg ?? '—'));
        setText('perfUptime',  (d.uptimeHours ?? 0) + 'h ' + (d.uptimeMinutes ?? 0) + 'm');

        // Gauge fills
        const memFill = document.getElementById('memGaugeFill');
        if (memFill) { memFill.style.width = mem + '%'; memFill.style.background = memColor(mem); }
        setText('memGaugeVal', mem + ' %');

        const tpsFill = document.getElementById('tpsGaugeFill');
        if (tpsFill) { tpsFill.style.width = (tps / 20 * 100) + '%'; tpsFill.style.background = tpsColor(tps); }
        setText('tpsGaugeVal', tps + ' / 20');

        const players = Number(d.players || 0), maxP = Number(d.playersMax || 1);
        const playerFill = document.getElementById('playerGaugeFill');
        if (playerFill) { playerFill.style.width = (players / maxP * 100) + '%'; }
        setText('playerGaugeVal', players + ' / ' + maxP);

        // Colour the TPS card
        const tpsCard = document.getElementById('perfTpsCard');
        if (tpsCard) {
            tpsCard.className = 'stat-card ' + (tps >= 19 ? 'green' : tps >= 15 ? 'yellow' : 'red');
        }

        // History charts
        const labels = Array.isArray(d.timeLabels) ? d.timeLabels : [];
        const tpsH   = Array.isArray(d.tpsHistory)  ? d.tpsHistory  : [];
        const memH   = Array.isArray(d.memHistory)  ? d.memHistory  : [];

        if (labels.length === 0) {
            document.getElementById('tpsChartEmpty').style.display = 'block';
            document.getElementById('memChartEmpty').style.display = 'block';
            return;
        }

        // TPS chart
        const tpsCtx = document.getElementById('tpsChart').getContext('2d');
        if (tpsChartInstance) tpsChartInstance.destroy();
        tpsChartInstance = new Chart(tpsCtx, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    data: tpsH,
                    borderColor: '#89b4fa',
                    backgroundColor: 'rgba(137,180,250,0.12)',
                    tension: 0.4, fill: true, borderWidth: 2, pointRadius: 2
                }]
            },
            options: {
                scales: {
                    y: { min: 0, max: 20, grid: { color: 'rgba(255,255,255,.06)' }, ticks: { color: '#a6adc8' } },
                    x: { grid: { display: false }, ticks: { color: '#a6adc8', maxTicksLimit: 8 } }
                },
                plugins: { tooltip: { callbacks: { label: ctx => ctx.parsed.y.toFixed(1) + ' TPS' } } }
            }
        });

        // Memory chart
        const memCtx = document.getElementById('memChart').getContext('2d');
        if (memChartInstance) memChartInstance.destroy();
        memChartInstance = new Chart(memCtx, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    data: memH,
                    borderColor: '#f9e2af',
                    backgroundColor: 'rgba(249,226,175,0.12)',
                    tension: 0.4, fill: true, borderWidth: 2, pointRadius: 2
                }]
            },
            options: {
                scales: {
                    y: { min: 0, grid: { color: 'rgba(255,255,255,.06)' }, ticks: { color: '#a6adc8', callback: v => v + ' MB' } },
                    x: { grid: { display: false }, ticks: { color: '#a6adc8', maxTicksLimit: 8 } }
                },
                plugins: { tooltip: { callbacks: { label: ctx => ctx.parsed.y + ' MB' } } }
            }
        });
    }

    // ── ② Economy ─────────────────────────────────────────────────────────────
    async function loadEconomy() {
        const d = await fetchJSON(API + '/economy');

        const sym = d.currencySymbol || '$';
        setText('ecoTotalWealth', sym + fmtNum(parseFloat(d.totalWealth || 0), 2));
        setText('ecoCurrency',    'across all ' + (d.accountCount || 0) + ' accounts');
        setText('ecoAccounts',    fmtNum(d.accountCount));
        setText('ecoAvgBalance',  sym + fmtNum(parseFloat(d.averageBalance || 0), 2));
        setText('ecoStartBal',    sym + fmtNum(d.startingBalance, 2));

        // Top 10 chart
        const top   = Array.isArray(d.topPlayers) ? d.topPlayers : [];
        const names = top.map(p => p.name || p.uuid?.substring(0,8));
        const bals  = top.map(p => parseFloat(p.balance || 0));

        const btCtx = document.getElementById('baltopChart').getContext('2d');
        if (baltopChartInst) baltopChartInst.destroy();
        baltopChartInst = new Chart(btCtx, {
            type: 'bar',
            data: {
                labels: names,
                datasets: [{
                    data: bals,
                    backgroundColor: names.map((_, i) => i === 0 ? '#f9e2af' : i === 1 ? '#a6adc8' : i === 2 ? '#f38ba8' : '#89b4fa'),
                    borderRadius: 4
                }]
            },
            options: {
                indexAxis: 'y',
                scales: {
                    x: { grid: { color: 'rgba(255,255,255,.06)' }, ticks: { color: '#a6adc8', callback: v => sym + fmtNum(v, 0) } },
                    y: { grid: { display: false }, ticks: { color: '#cdd6f4' } }
                },
                plugins: { tooltip: { callbacks: { label: c => sym + fmtNum(c.parsed.x, 2) } } }
            }
        });

        // Distribution chart
        const dist   = Array.isArray(d.distribution) ? d.distribution : [];
        const dLabel = dist.map(b => b.label);
        const dCount = dist.map(b => b.count);

        const dCtx = document.getElementById('distChart').getContext('2d');
        if (distChartInst) distChartInst.destroy();
        distChartInst = new Chart(dCtx, {
            type: 'doughnut',
            data: {
                labels: dLabel,
                datasets: [{
                    data: dCount,
                    backgroundColor: ['#313244','#45475a','#585b70','#89b4fa','#74c7ec','#89dceb','#a6e3a1','#f9e2af'],
                    borderWidth: 0
                }]
            },
            options: {
                plugins: {
                    legend: { display: true, position: 'right', labels: { color: '#a6adc8', boxWidth: 10, font: { size: 10 } } },
                    tooltip: { callbacks: { label: c => c.label + ': ' + c.parsed + ' players' } }
                }
            }
        });

        // Baltop table
        const maxBal = bals.length > 0 ? bals[0] : 1;
        const rows = top.map(p => {
            const pct = Math.round(parseFloat(p.balance) / maxBal * 100);
            const online = p.online
                ? '<span class="dot-online"></span>'
                : '<span class="dot-offline"></span>';
            return `<tr>
                <td style="width:2.5rem;text-align:center;font-weight:700;color:#a6adc8">#${p.rank}</td>
                <td>${online}${esc(p.name)}</td>
                <td>
                    <div class="bar-wrap">
                        <div class="bar"><div class="bar-fill green" style="width:${pct}%"></div></div>
                        <span style="font-size:.82rem;font-weight:600;min-width:4rem;text-align:right">${sym}${fmtNum(parseFloat(p.balance),2)}</span>
                    </div>
                </td>
            </tr>`;
        }).join('');

        document.getElementById('baltopTableWrap').innerHTML = rows.length
            ? `<table><thead><tr><th>#</th><th>Player</th><th>Balance</th></tr></thead><tbody>${rows}</tbody></table>`
            : '<div style="padding:2rem;text-align:center;color:#6c7086">No economy data yet.</div>';
    }

    // ── ③ Activity ────────────────────────────────────────────────────────────
    async function loadActivity() {
        const d = await fetchJSON(API + '/activity');

        setText('actOnline',     d.currentOnline ?? '0');
        setText('actPeak',       d.peakOnlineToday ?? '0');
        setText('actUnique',     d.uniqueToday ?? '0');
        setText('actAvgSession', d.avgSessionMinutes ?? '0');
        setText('actTotalPlay',  d.totalPlayMinutesToday ?? '0');
        setText('actSessions',   d.completedSessionsToday ?? '0');

        // Active sessions table
        const active = Array.isArray(d.activeSessions) ? d.activeSessions : [];
        if (active.length === 0) {
            document.getElementById('activeSessionsWrap').innerHTML =
                '<div style="padding:2rem;text-align:center;color:#6c7086">No players online right now.</div>';
        } else {
            const rows = active.map(s => `<tr>
                <td><span class="dot-online"></span>${esc(s.name)}</td>
                <td>${s.sessionMin} min</td>
            </tr>`).join('');
            document.getElementById('activeSessionsWrap').innerHTML =
                `<table><thead><tr><th>Player</th><th>Session Duration</th></tr></thead><tbody>${rows}</tbody></table>`;
        }

        // Recent sessions table
        const recent = Array.isArray(d.recentSessions) ? d.recentSessions : [];
        if (recent.length === 0) {
            document.getElementById('recentSessionsWrap').innerHTML =
                '<div style="padding:2rem;text-align:center;color:#6c7086">No completed sessions today.</div>';
        } else {
            const rows = recent.map(s => {
                const ended = s.endedAt ? new Date(s.endedAt).toLocaleTimeString(undefined, { timeStyle:'short' }) : '—';
                return `<tr>
                    <td><span class="dot-offline"></span>${esc(s.name)}</td>
                    <td>${s.sessionMin} min</td>
                    <td style="color:#6c7086;font-size:.8rem">${ended}</td>
                </tr>`;
            }).join('');
            document.getElementById('recentSessionsWrap').innerHTML =
                `<table><thead><tr><th>Player</th><th>Session</th><th>Left At</th></tr></thead><tbody>${rows}</tbody></table>`;
        }
    }

    // ── Load all tabs on demand ───────────────────────────────────────────────
    let loadedTabs = new Set();

    async function loadActiveTab() {
        const active = document.querySelector('.tab-btn.active');
        if (!active) return;
        const tab = active.dataset.tab;
        try {
            if (tab === 'performance') { await loadPerf(); }
            else if (tab === 'economy') { await loadEconomy(); }
            else if (tab === 'activity') { await loadActivity(); }
        } catch (e) {
            console.error('[Stats] Load error for tab ' + tab + ':', e);
        }
    }

    // Load tab on switch
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => setTimeout(loadActiveTab, 50));
    });

    // ── Auto-refresh ──────────────────────────────────────────────────────────
    document.getElementById('refreshBtn').addEventListener('click', loadActiveTab);

    let countdown = 30;
    setInterval(() => {
        countdown--;
        const badge = document.getElementById('autoRefreshBadge');
        if (badge) badge.textContent = 'Auto-refresh: ' + countdown + 's';
        if (countdown <= 0) {
            countdown = 30;
            loadActiveTab();
        }
    }, 1000);

    // ── Init ──────────────────────────────────────────────────────────────────
    loadActiveTab();
    // Pre-load other tabs silently after a short delay
    setTimeout(() => { loadEconomy().catch(() => {}); loadActivity().catch(() => {}); }, 1200);
})();

