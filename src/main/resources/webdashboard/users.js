/**
 * users.js — NeoEssentials Dashboard · User Management page
 */
(function () {
    'use strict';
    const API   = window.API_BASE || '/api';
    const token = () => localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';
    const esc   = s => s ? String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;') : '';
    const el    = id => document.getElementById(id);

    let pendingRoleUserId = null;
    let pendingPwUserId   = null;

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
        if (type !== 'loading') setTimeout(() => b.style.display='none', 6000);
    }

    function fmtDate(ts) {
        if (!ts) return '—';
        return new Date(ts).toLocaleString(undefined, { dateStyle:'short', timeStyle:'short' });
    }

    // ── Load users ─────────────────────────────────────────────────────────────
    async function loadUsers() {
        el('usersTableWrap').innerHTML = '<div class="empty-state"><div class="icon"><span class="spinner"></span></div></div>';
        try {
            const d = await apiFetch('/users/list');
            if (!d.success) { el('usersTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(d.error||d.message)}</div></div>`; return; }
            const users = d.users || [];

            // Stats
            el('statTotal').textContent    = users.length;
            el('statAdmins').textContent   = users.filter(u => u.role === 'ADMIN').length;
            el('statDisabled').textContent = users.filter(u => !u.enabled).length;

            if (!users.length) {
                el('usersTableWrap').innerHTML = '<div class="empty-state"><div class="icon"></div><div>No dashboard users found.</div></div>';
                return;
            }

            let html = `<table><thead><tr>
                <th>Username</th><th>Email</th><th>Role</th><th>Status</th>
                <th>Last Login</th><th>Fails</th><th>Actions</th>
            </tr></thead><tbody>`;
            for (const u of users) {
                const status = u.enabled ? '<span class="enabled-dot on"></span>Active' : '<span class="enabled-dot off"></span>Disabled';
                html += `<tr>
                    <td><strong>${esc(u.username)}</strong></td>
                    <td style="color:#6c7086;">${esc(u.email||'—')}</td>
                    <td><span class="role-badge role-${esc(u.role)}">${esc(u.role)}</span></td>
                    <td style="font-size:0.82rem;">${status}</td>
                    <td style="color:#6c7086;font-size:0.8rem;">${fmtDate(u.lastLoginAt)}</td>
                    <td style="color:${u.failedLoginAttempts>2?'#f38ba8':'inherit'};text-align:center;">${u.failedLoginAttempts||0}</td>
                    <td><div class="action-cell">
                        <button class="btn-sm btn-role" onclick="openRoleModal('${esc(u.id)}','${esc(u.username)}','${esc(u.role)}')"> Role</button>
                        <button class="btn-sm btn-pw"   onclick="openPwModal('${esc(u.id)}','${esc(u.username)}')"> Pwd</button>
                        <button class="btn-sm btn-toggle" onclick="toggleUser('${esc(u.id)}',${!!u.enabled})">${u.enabled ? ' Disable' : '✅ Enable'}</button>
                        <button class="btn-sm btn-del"  onclick="deleteUser('${esc(u.id)}','${esc(u.username)}')"></button>
                    </div></td>
                </tr>`;
            }
            html += '</tbody></table>';
            el('usersTableWrap').innerHTML = html;
        } catch(e) { el('usersTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(e.message)}</div></div>`; }
    }

    // ── Load sessions ─────────────────────────────────────────────────────────
    window.loadSessions = async function() {
        el('sessionsTableWrap').innerHTML = '<div class="empty-state"><div class="icon"><span class="spinner"></span></div></div>';
        try {
            const d = await apiFetch('/users/sessions');
            if (!d.success) { el('sessionsTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(d.error||d.message)}</div></div>`; return; }
            const sessions = d.sessions || [];
            el('statSessions').textContent = sessions.length;
            if (!sessions.length) { el('sessionsTableWrap').innerHTML = '<div class="empty-state"><div class="icon"></div><div>No active sessions.</div></div>'; return; }
            let html = `<table><thead><tr><th>Username</th><th>Role</th><th>IP Address</th><th>Created</th><th>Last Active</th><th>Actions</th></tr></thead><tbody>`;
            for (const s of sessions) {
                html += `<tr>
                    <td><strong>${esc(s.username)}</strong></td>
                    <td><span class="role-badge role-${esc(s.role)}">${esc(s.role)}</span></td>
                    <td style="font-size:0.8rem;color:#6c7086;">${esc(s.ipAddress)}</td>
                    <td style="font-size:0.8rem;color:#6c7086;">${fmtDate(s.createdAt)}</td>
                    <td style="font-size:0.8rem;color:#6c7086;">${fmtDate(s.lastAccessAt)}</td>
                    <td><button class="btn-sm btn-revoke" onclick="revokeSession('${esc(s.sessionId)}')">⊗ Revoke</button></td>
                </tr>`;
            }
            html += '</tbody></table>';
            el('sessionsTableWrap').innerHTML = html;
        } catch(e) { el('sessionsTableWrap').innerHTML = `<div class="empty-state"><div class="icon">❌</div><div>${esc(e.message)}</div></div>`; }
    };

    // ── Create user ───────────────────────────────────────────────────────────
    async function createUser() {
        const username = el('newUsername').value.trim();
        const password = el('newPassword').value.trim();
        const email    = el('newEmail').value.trim();
        const role     = el('newRole').value;
        if (!username || !password) { banner('Username and password are required', 'error'); return; }
        el('createUserBtn').innerHTML = '<span class="spinner"></span>Creating…';
        const d = await apiFetch('/users/create', { method:'POST', body:JSON.stringify({ username, password, email, role }) });
        el('createUserBtn').textContent = 'Create User';
        if (d.success) {
            banner('✅ User created: '+username, 'success');
            el('newUsername').value = ''; el('newPassword').value = ''; el('newEmail').value = '';
            loadUsers();
        } else banner('❌ '+(d.message||d.error), 'error');
    }

    // ── Role modal ────────────────────────────────────────────────────────────
    window.openRoleModal = function(userId, username, currentRole) {
        pendingRoleUserId = userId;
        el('roleModalDesc').textContent = 'Change role for: ' + username;
        el('roleSelect').value = currentRole;
        el('roleModal').classList.add('open');
    };
    async function confirmRoleChange() {
        if (!pendingRoleUserId) return;
        const role = el('roleSelect').value;
        const d = await apiFetch('/users/'+pendingRoleUserId+'/role', { method:'POST', body:JSON.stringify({ role }) });
        el('roleModal').classList.remove('open');
        banner(d.success ? '✅ Role updated' : '❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) loadUsers();
    }

    // ── Password modal ────────────────────────────────────────────────────────
    window.openPwModal = function(userId, username) {
        pendingPwUserId = userId;
        el('pwInput').value = '';
        el('pwModal').classList.add('open');
    };
    async function confirmPwReset() {
        if (!pendingPwUserId) return;
        const pw = el('pwInput').value.trim();
        const d = await apiFetch('/users/'+pendingPwUserId+'/password', { method:'POST', body:JSON.stringify({ password: pw }) });
        el('pwModal').classList.remove('open');
        if (d.success) {
            if (d.tempPassword) {
                banner('✅ Temp password: ' + d.tempPassword + ' (copy it now!)', 'success');
            } else {
                banner('✅ Password updated', 'success');
            }
        } else banner('❌ '+(d.message||d.error), 'error');
    }

    // ── Toggle enable ─────────────────────────────────────────────────────────
    window.toggleUser = async function(userId, currentlyEnabled) {
        const action = currentlyEnabled ? 'disable' : 'enable';
        if (!confirm('Are you sure you want to ' + action + ' this user?')) return;
        const d = await apiFetch('/users/'+userId+'/'+action, { method:'POST', body:'{}' });
        banner(d.success ? '✅ '+d.message : '❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) loadUsers();
    };

    // ── Delete ────────────────────────────────────────────────────────────────
    window.deleteUser = async function(userId, username) {
        if (!confirm('Delete dashboard user "' + username + '"? This cannot be undone.')) return;
        const d = await apiFetch('/users/'+userId, { method:'DELETE' });
        banner(d.success ? '✅ User deleted' : '❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) loadUsers();
    };

    // ── Revoke session ────────────────────────────────────────────────────────
    window.revokeSession = async function(sessionId) {
        if (!confirm('Revoke this session? The user will be logged out.')) return;
        const d = await apiFetch('/users/sessions/'+sessionId, { method:'DELETE' });
        banner(d.success ? '✅ Session revoked' : '❌ '+(d.message||d.error), d.success?'success':'error');
        if (d.success) window.loadSessions();
    };

    // ── Init ──────────────────────────────────────────────────────────────────
    async function init() {
        await loadUsers();
        el('refreshUsersBtn')?.addEventListener('click', loadUsers);
        el('refreshSessionsBtn')?.addEventListener('click', window.loadSessions);
        el('createUserBtn')?.addEventListener('click', createUser);
        el('confirmRoleBtn')?.addEventListener('click', confirmRoleChange);
        el('confirmPwBtn')?.addEventListener('click', confirmPwReset);
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
