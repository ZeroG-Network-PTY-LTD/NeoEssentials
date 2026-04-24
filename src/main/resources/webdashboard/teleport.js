/**
 * teleport.js — Dashboard Teleport Settings Page
 * Handles loading and saving teleportation configuration via /api/teleport/settings
 */
(function () {
    'use strict';

    // ─────────────────────────────────────────────────────────────────────────
    // Init: wait for dashboard.js to finish auth, then load settings
    // ─────────────────────────────────────────────────────────────────────────
    function init() {
        // Guard: ensure this page has the expected elements
        if (!document.getElementById('saveSettingsBtn')) return;

        const permOverviewCheck = document.getElementById('saveSettingsBtn');
        if (!permOverviewCheck) return;

        loadSettings();

        document.getElementById('saveSettingsBtn').addEventListener('click', saveSettings);
        document.getElementById('reloadSettingsBtn').addEventListener('click', loadSettings);
    }

    // ── Banner helpers ────────────────────────────────────────────────────────
    function showBanner(message, type) {
        const banner = document.getElementById('teleportStatusBanner');
        if (!banner) return;
        banner.textContent = message;
        banner.className = 'status-banner ' + type;
        banner.style.display = 'block';
        if (type === 'success') {
            setTimeout(() => { banner.style.display = 'none'; }, 4000);
        }
    }

    // ── Load settings from server ─────────────────────────────────────────────
    async function loadSettings() {
        showBanner('Loading teleport settings…', 'loading');
        try {
            const resp = await fetchWithAuth(API_BASE_URL + '/teleport/settings');
            if (!resp.ok) throw new Error('HTTP ' + resp.status);
            const data = await resp.json();
            if (!data.success) throw new Error(data.error || 'Unknown error');

            populateForm(data);
            showBanner('Settings loaded.', 'success');
        } catch (err) {
            showBanner('Failed to load settings: ' + err.message, 'error');
            console.error('[Teleport] loadSettings error:', err);
        }
    }

    // ── Populate form fields ──────────────────────────────────────────────────
    function populateForm(data) {
        function setNum(id, val) {
            const el = document.getElementById(id);
            if (el && val !== undefined) el.value = val;
        }
        function setBool(id, val) {
            const el = document.getElementById(id);
            if (el && val !== undefined) el.checked = !!val;
        }

        const g = data.generalSettings || {};
        setNum('generalTeleportDelay',  g.teleportDelay);
        setBool('enableTeleportWarmup',  g.enableTeleportWarmup);
        setBool('cancelTeleportOnMove',  g.cancelTeleportOnMove);
        setBool('cancelTeleportOnDamage',g.cancelTeleportOnDamage);
        setNum('maxTeleportDistance',    g.maxTeleportDistance);

        const h = data.homeSettings || {};
        setNum('maxHomes',               h.maxHomes);
        setNum('homeTeleportCooldown',   h.homeTeleportCooldown);
        setNum('homeSetCooldown',        h.homeSetCooldown);
        setNum('homeDeleteCooldown',     h.homeDeleteCooldown);
        setBool('enableHomeSafety',      h.enableHomeSafety);
        setBool('allowCrossDimensionHomes', h.allowCrossDimensionHomes);

        const w = data.warpSettings || {};
        setNum('maxWarps',               w.maxWarps);
        setNum('warpCooldown',           w.warpCooldown);
        setNum('warpSetCooldown',        w.warpSetCooldown);
        setBool('enableWarpSafety',      w.enableWarpSafety);

        const s = data.spawnSettings || {};
        setNum('spawnCooldown',          s.spawnCooldown);
        setBool('enableSpawnSafety',     s.enableSpawnSafety);

        const b = data.backSettings || {};
        setNum('backTeleportDelay',      b.teleportDelay);
        setNum('backCooldown',           b.backCooldown);
        setBool('enableDeathBack',       b.enableDeathBack);
        setBool('enableTeleportBack',    b.enableTeleportBack);
    }

    // ── Read form and build payload ───────────────────────────────────────────
    function readForm() {
        function getNum(id)  { const el = document.getElementById(id); return el ? parseInt(el.value, 10) : undefined; }
        function getBool(id) { const el = document.getElementById(id); return el ? el.checked : undefined; }

        return {
            generalSettings: {
                teleportDelay:         getNum('generalTeleportDelay'),
                enableTeleportWarmup:  getBool('enableTeleportWarmup'),
                cancelTeleportOnMove:  getBool('cancelTeleportOnMove'),
                cancelTeleportOnDamage:getBool('cancelTeleportOnDamage'),
                maxTeleportDistance:   getNum('maxTeleportDistance'),
            },
            homeSettings: {
                maxHomes:              getNum('maxHomes'),
                homeTeleportCooldown:  getNum('homeTeleportCooldown'),
                homeSetCooldown:       getNum('homeSetCooldown'),
                homeDeleteCooldown:    getNum('homeDeleteCooldown'),
                enableHomeSafety:      getBool('enableHomeSafety'),
                enableHomeTeleportSafety: getBool('enableHomeSafety'), // write both aliases
                allowCrossDimensionHomes: getBool('allowCrossDimensionHomes'),
            },
            warpSettings: {
                maxWarps:              getNum('maxWarps'),
                warpCooldown:          getNum('warpCooldown'),
                warpSetCooldown:       getNum('warpSetCooldown'),
                enableWarpSafety:      getBool('enableWarpSafety'),
            },
            spawnSettings: {
                spawnCooldown:         getNum('spawnCooldown'),
                enableSpawnSafety:     getBool('enableSpawnSafety'),
            },
            backSettings: {
                teleportDelay:         getNum('backTeleportDelay'),
                backCooldown:          getNum('backCooldown'),
                enableDeathBack:       getBool('enableDeathBack'),
                enableTeleportBack:    getBool('enableTeleportBack'),
            },
        };
    }

    // ── Save settings to server ───────────────────────────────────────────────
    async function saveSettings() {
        const btn = document.getElementById('saveSettingsBtn');
        if (btn) btn.disabled = true;
        showBanner('Saving settings…', 'loading');

        const payload = readForm();

        // Validate basic numbers
        if (isNaN(payload.generalSettings.teleportDelay) || payload.generalSettings.teleportDelay < 0) {
            showBanner('Teleport warmup delay must be 0 or greater.', 'error');
            if (btn) btn.disabled = false;
            return;
        }

        try {
            const resp = await fetchWithAuth(API_BASE_URL + '/teleport/settings', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            if (!resp.ok) throw new Error('HTTP ' + resp.status);
            const data = await resp.json();
            if (!data.success) throw new Error(data.error || 'Unknown error');

            showBanner('✅ ' + (data.message || 'Settings saved and applied!'), 'success');
        } catch (err) {
            showBanner('❌ Failed to save: ' + err.message, 'error');
            console.error('[Teleport] saveSettings error:', err);
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    // ── Bootstrap ─────────────────────────────────────────────────────────────
    // Run after dashboard.js sets up auth
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        // dashboard.js runs showDashboard asynchronously; give it a tick
        setTimeout(init, 800);
    }
})();

