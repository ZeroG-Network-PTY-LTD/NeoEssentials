/**
 * shop.js — NeoEssentials Dashboard: Chest Shop management page
 * Loads shop list, stats, handles pagination / filtering, price editing,
 * and CSV import / export.
 */
(function () {
    'use strict';

    const API   = '/api/shops';
    const token = () => localStorage.getItem('authToken') || '';
    const hdrs  = () => ({ Authorization: 'Bearer ' + token(), 'Content-Type': 'application/json' });

    // ── State ──────────────────────────────────────────────────────────────────
    let allShops     = [];   // full local copy after load
    let filteredShops = [];  // after search / type filter
    let currentPage  = 1;
    const PAGE_SIZE  = 50;

    // ── Initialise ─────────────────────────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
        setupPage();
    });

    function setupPage() {
        // Sidebar toggle
        const toggle = document.getElementById('sidebarToggle');
        if (toggle) toggle.addEventListener('click', () => {
            document.querySelector('.sidebar')?.classList.toggle('open');
        });

        // Refresh button
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) refreshBtn.addEventListener('click', loadAll);

        // CSV export
        const exportBtn = document.getElementById('csvExportBtn');
        if (exportBtn) exportBtn.addEventListener('click', csvExport);

        // CSV import
        const importFile = document.getElementById('csvImportFile');
        if (importFile) importFile.addEventListener('change', csvImport);
    }

    async function loadAll() {
        await Promise.all([loadStats(), loadShops()]);
    }

    // ── Stats ──────────────────────────────────────────────────────────────────
    async function loadStats() {
        try {
            const res  = await fetch(`${API}/stats`, { headers: hdrs() });
            const data = await res.json();
            setText('statTotal',  data.totalShops ?? '—');
            setText('statAdmin',  data.adminShops ?? '—');
            setText('statPlayer', data.playerShops ?? '—');
            setText('statNpc',    data.npcShops ?? '—');
            setText('statTx',     data.totalTransactions ?? '—');
        } catch (e) {
            console.error('[Shops] Stats error:', e.message);
        }
    }

    // ── Shop list (load all pages into memory) ────────────────────────────────
    async function loadShops() {
        allShops = [];
        let page = 1;
        const size = 200; // fetch large pages to minimise requests

        try {
            while (true) {
                const res  = await fetch(`${API}/list?page=${page}&size=${size}`, { headers: hdrs() });
                const data = await res.json();
                const shops = data.shops || [];
                allShops = allShops.concat(shops);
                if (allShops.length >= (data.total || 0) || shops.length < size) break;
                page++;
            }
        } catch (e) {
            console.error('[Shops] List load error:', e.message);
        }

        applyFilter();
    }

    // ── Filter ─────────────────────────────────────────────────────────────────
    window.applyFilter = function () {
        const query   = (document.getElementById('filterInput')?.value || '').toLowerCase();
        const typeVal = document.getElementById('typeFilter')?.value || '';

        filteredShops = allShops.filter(s => {
            const matchType = !typeVal
                || (typeVal === 'admin'  && (s.adminShop || s.isAdminShop))
                || (typeVal === 'player' && !(s.adminShop || s.isAdminShop));
            if (!matchType) return false;
            if (!query) return true;
            const haystack = [
                s.itemId || '', s.ownerName || s.owner || '',
                s.dimension || '', s.world || ''
            ].join(' ').toLowerCase();
            return haystack.includes(query);
        });

        currentPage = 1;
        renderTable();
    };

    // ── Render table ───────────────────────────────────────────────────────────
    function renderTable() {
        const tbody   = document.getElementById('shopTableBody');
        const isAdmin = localStorage.getItem('isAdmin') === 'true';

        if (!filteredShops.length) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;color:var(--text-tertiary);padding:2rem;">
                No shops found${document.getElementById('filterInput')?.value ? ' for that filter' : ''}.
            </td></tr>`;
            document.getElementById('pageInfo').textContent = 'No results';
            document.getElementById('prevPageBtn').disabled = true;
            document.getElementById('nextPageBtn').disabled = true;
            return;
        }

        const start = (currentPage - 1) * PAGE_SIZE;
        const page  = filteredShops.slice(start, start + PAGE_SIZE);
        const total = Math.ceil(filteredShops.length / PAGE_SIZE);

        document.getElementById('pageInfo').textContent =
            `Page ${currentPage} / ${total} (${filteredShops.length} shops)`;
        document.getElementById('prevPageBtn').disabled = currentPage <= 1;
        document.getElementById('nextPageBtn').disabled = currentPage >= total;

        tbody.innerHTML = page.map(s => {
            const key      = esc(s.key || `${s.dimension}@${s.x},${s.y},${s.z}`);
            const item     = esc(friendlyItem(s.itemId));
            const owner    = esc(s.ownerName || s.owner || 'Unknown');
            const isAdm    = s.adminShop || s.isAdminShop;
            const typeBadge = isAdm
                ? '<span class="badge-admin">Admin</span>'
                : '<span class="badge-player">Player</span>';
            const buyPr  = s.buyPrice  != null && s.buyPrice  > 0 ? fmtPrice(s.buyPrice)  : '—';
            const sellPr = s.sellPrice != null && s.sellPrice > 0 ? fmtPrice(s.sellPrice) : '—';
            const stock  = s.stock != null ? s.stock : '∞';
            const sales  = s.totalSalesCount ?? 0;
            const loc    = esc(shortDim(s.dimension || s.world || '')) + ` (${s.x ?? '?'}, ${s.y ?? '?'}, ${s.z ?? '?'})`;
            const actions = isAdmin
                ? `<button class="btn btn-xs btn-secondary" onclick="openPriceModal(${JSON.stringify(s)})">✏️ Prices</button>`
                : '';
            return `<tr>
                <td style="font-weight:500;">${item}</td>
                <td>${owner}</td>
                <td>${typeBadge}</td>
                <td>${buyPr}</td>
                <td>${sellPr}</td>
                <td>${stock}</td>
                <td>${sales}</td>
                <td style="font-size:.78rem;color:var(--text-secondary);">${loc}</td>
                <td class="admin-only"${!isAdmin ? ' style="display:none"' : ''}>${actions}</td>
            </tr>`;
        }).join('');
    }

    // ── Pagination ─────────────────────────────────────────────────────────────
    window.changePage = function (delta) {
        const total = Math.ceil(filteredShops.length / PAGE_SIZE);
        currentPage = Math.max(1, Math.min(currentPage + delta, total));
        renderTable();
        document.querySelector('.shop-table-card')?.scrollIntoView({ behavior: 'smooth' });
    };

    // ── Price editing ──────────────────────────────────────────────────────────
    window.openPriceModal = function (shop) {
        document.getElementById('priceShopKey').value = shop.key || '';
        document.getElementById('priceShopLabel').textContent =
            friendlyItem(shop.itemId) + ' @ ' + shortDim(shop.dimension || '') +
            ` (${shop.x},${shop.y},${shop.z})`;
        document.getElementById('priceInputBuy').value  = shop.buyPrice  ?? 0;
        document.getElementById('priceInputSell').value = shop.sellPrice ?? 0;
        document.getElementById('priceError').style.display = 'none';
        document.getElementById('priceModal').style.display = 'flex';
    };

    window.closePriceModal = function () {
        document.getElementById('priceModal').style.display = 'none';
    };

    window.savePrices = async function () {
        const key      = document.getElementById('priceShopKey').value;
        const buyPrice  = parseFloat(document.getElementById('priceInputBuy').value)  || 0;
        const sellPrice = parseFloat(document.getElementById('priceInputSell').value) || 0;
        const errEl    = document.getElementById('priceError');

        if (buyPrice < 0 || sellPrice < 0) {
            errEl.textContent = 'Prices must be 0 or positive.';
            errEl.style.display = '';
            return;
        }

        try {
            const res  = await fetch(`${API}/price`, {
                method:  'PUT',
                headers: hdrs(),
                body:    JSON.stringify({ key, buyPrice, sellPrice })
            });
            const data = await res.json();
            if (data.success !== false) {
                closePriceModal();
                // Locally update
                const idx = allShops.findIndex(s => (s.key || '') === key);
                if (idx >= 0) {
                    allShops[idx].buyPrice  = buyPrice;
                    allShops[idx].sellPrice = sellPrice;
                    applyFilter();
                }
            } else {
                errEl.textContent = data.error || 'Failed to update prices.';
                errEl.style.display = '';
            }
        } catch (e) {
            errEl.textContent = 'Network error: ' + e.message;
            errEl.style.display = '';
        }
    };

    // ── CSV export ─────────────────────────────────────────────────────────────
    async function csvExport() {
        try {
            const res  = await fetch(`${API}/csv/export`, { headers: hdrs() });
            if (!res.ok) throw new Error(res.status + ' ' + res.statusText);
            const text = await res.text();
            const blob = new Blob([text], { type: 'text/csv' });
            const url  = URL.createObjectURL(blob);
            const a    = document.createElement('a');
            a.href     = url;
            a.download = 'shops.csv';
            a.click();
            URL.revokeObjectURL(url);
        } catch (e) {
            setCsvStatus('❌ Export failed: ' + e.message, true);
        }
    }

    // ── CSV import ─────────────────────────────────────────────────────────────
    async function csvImport(evt) {
        const file = evt.target.files[0];
        if (!file) return;
        setCsvStatus('⏳ Importing…');

        try {
            const text = await file.text();
            const res  = await fetch(`${API}/csv/import`, {
                method:  'POST',
                headers: { ...hdrs(), 'Content-Type': 'text/plain' },
                body:    text
            });
            const data = await res.json();
            if (data.imported !== undefined) {
                setCsvStatus(`✅ Imported ${data.imported} shops (${data.skipped || 0} skipped).`);
                loadAll();
            } else {
                setCsvStatus('❌ ' + (data.error || 'Import failed'), true);
            }
        } catch (e) {
            setCsvStatus('❌ Import error: ' + e.message, true);
        }
        evt.target.value = ''; // reset file input
    }

    function setCsvStatus(msg, isError = false) {
        const el = document.getElementById('csvStatus');
        if (!el) return;
        el.textContent = msg;
        el.style.color = isError ? 'var(--danger)' : 'var(--success)';
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    function esc(s) {
        return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function setText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val ?? '—';
    }

    function fmtPrice(n) {
        return Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    /** Shorten item id: minecraft:diamond → Diamond */
    function friendlyItem(id) {
        if (!id) return 'Unknown';
        const part = String(id).split(':').pop() || id;
        return part.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
    }

    /** Shorten dimension key: minecraft:overworld → Overworld */
    function shortDim(dim) {
        if (!dim) return '?';
        const map = {
            'minecraft:overworld': 'Overworld',
            'minecraft:the_nether': 'Nether',
            'minecraft:the_end': 'End'
        };
        return map[dim] || dim.split(':').pop().replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
    }

    // ── Boot ──────────────────────────────────────────────────────────────────
    // Wait for dashboard auth (dashboard.js calls showDashboard which fires loadAll via checkAuthentication)
    // We hook in once the DOM is ready and auth is done.
    const _origShow = typeof showDashboard === 'function' ? showDashboard : null;
    // Polled check: wait until auth token exists, then load
    (function waitForAuth() {
        if (localStorage.getItem('authToken')) {
            loadAll();
        } else {
            setTimeout(waitForAuth, 500);
        }
    })();

})();

