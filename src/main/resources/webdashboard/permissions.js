// NeoEssentials Permission Management JavaScript

// Permission System State
const PermissionSystem = {
    groups: [],
    users: [],
    permissions: [],
    systemStatus: null,
    usingExternal: false
};

// Initialize permission system when page loads
async function initPermissionSystem() {
    console.log('Initializing permission system...');

    // Setup tab navigation
    setupPermissionTabs();

    // Load system status
    await loadPermissionSystemStatus();

    // Setup event listeners
    setupPermissionEventListeners();

    // Load overview data
    if (document.getElementById('permOverviewTab').classList.contains('active')) {
        await loadPermissionOverview();
    }
}

// Setup tab navigation
function setupPermissionTabs() {
    const tabs = document.querySelectorAll('.perm-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            // Remove active from all tabs and contents
            document.querySelectorAll('.perm-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.perm-tab-content').forEach(c => c.classList.remove('active'));

            // Add active to clicked tab
            tab.classList.add('active');

            // Show corresponding content
            const tabName = tab.dataset.tab;
            const content = document.getElementById(`perm${capitalize(tabName)}Tab`);
            if (content) {
                content.classList.add('active');
                loadTabData(tabName);
            }
        });
    });
}

// Setup event listeners
function setupPermissionEventListeners() {
    // Create group button
    const createGroupBtn = document.getElementById('createGroupBtn');
    if (createGroupBtn) {
        createGroupBtn.addEventListener('click', showCreateGroupModal);
    }

    // Refresh buttons
    const refreshGroupsBtn = document.getElementById('refreshGroupsBtn');
    if (refreshGroupsBtn) {
        refreshGroupsBtn.addEventListener('click', () => loadAllGroups());
    }

    const refreshUsersBtn = document.getElementById('refreshUsersBtn');
    if (refreshUsersBtn) {
        refreshUsersBtn.addEventListener('click', () => loadAllUsers());
    }

    // User search
    const userSearchInput = document.getElementById('userSearchInput');
    if (userSearchInput) {
        userSearchInput.addEventListener('input', (e) => {
            filterUsers(e.target.value);
        });
    }
}

// Load tab data based on which tab is active
async function loadTabData(tabName) {
    switch (tabName) {
        case 'overview':
            await loadPermissionOverview();
            break;
        case 'groups':
            await loadAllGroups();
            break;
        case 'users':
            await loadAllUsers();
            break;
        case 'permissions':
            await loadAllPermissions();
            break;
    }
}

// Load permission system status
async function loadPermissionSystemStatus() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/system/status`);
        PermissionSystem.systemStatus = response;
        PermissionSystem.usingExternal = response.usingExternal;

        // Update system badge
        const badge = document.getElementById('permSystemBadge');
        if (badge) {
            badge.textContent = response.systemType;
            badge.className = 'card-badge ' + (response.usingExternal ? 'badge-warning' : 'badge-success');
        }

        // Update status alert
        const alert = document.getElementById('permStatusAlert');
        if (alert && response.usingExternal) {
            alert.innerHTML = `
                <div class="alert-content alert-warning">
                    <span class="alert-icon">⚠️</span>
                    <span class="alert-text">
                        Using external permission system: ${response.externalProvider}.
                        Permission management must be done through that system.
                    </span>
                </div>
            `;
        } else if (alert) {
            alert.innerHTML = `
                <div class="alert-content alert-success">
                    <span class="alert-icon">✅</span>
                    <span class="alert-text">
                        Using internal NeoEssentials permission system.
                        You can manage all permissions from this dashboard.
                    </span>
                </div>
            `;
        }

        // Disable management if using external system
        if (response.usingExternal) {
            disablePermissionManagement();
        }

    } catch (error) {
        console.error('Error loading permission system status:', error);
    }
}

// Load permission overview
async function loadPermissionOverview() {
    try {
        const overview = await fetchWithAuth(`${API_BASE_URL}/permissions/overview`);

        // Update stats
        document.getElementById('totalGroupsStat').textContent = overview.totalGroups || 0;
        document.getElementById('totalUsersStat').textContent = overview.totalUsers || 0;
        document.getElementById('systemTypeStat').textContent = overview.systemType || 'Unknown';

        // Render group overview
        if (overview.groupStats && overview.groupStats.length > 0) {
            const groupsList = document.getElementById('permGroupsList');
            groupsList.innerHTML = '';

            overview.groupStats.forEach(group => {
                const groupCard = document.createElement('div');
                groupCard.className = 'perm-group-overview-card';
                groupCard.innerHTML = `
                    <div class="group-overview-header">
                        <span class="group-name">${group.name}</span>
                        ${group.isDefault ? '<span class="badge-default">Default</span>' : ''}
                    </div>
                    <div class="group-overview-stat">
                        <span class="stat-label">Permissions:</span>
                        <span class="stat-value">${group.permissionCount}</span>
                    </div>
                `;
                groupsList.appendChild(groupCard);
            });
        }

    } catch (error) {
        console.error('Error loading permission overview:', error);
    }
}

// Load all groups
async function loadAllGroups() {
    if (PermissionSystem.usingExternal) {
        document.getElementById('permGroupsTable').innerHTML = `
            <div class="alert-warning">
                <span class="alert-icon">⚠️</span>
                <span>Cannot manage groups with external permission system</span>
            </div>
        `;
        return;
    }

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/groups`);
        PermissionSystem.groups = response.groups || [];

        renderGroupsTable(PermissionSystem.groups);
    } catch (error) {
        console.error('Error loading groups:', error);
    }
}

// Render groups table
function renderGroupsTable(groups) {
    const table = document.getElementById('permGroupsTable');
    if (!groups || groups.length === 0) {
        table.innerHTML = '<div class="empty-state">No groups found</div>';
        return;
    }

    let html = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>Group Name</th>
                    <th>Prefix</th>
                    <th>Suffix</th>
                    <th>Weight</th>
                    <th>Permissions</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
    `;

    groups.forEach(group => {
        html += `
            <tr>
                <td>
                    <strong>${group.name}</strong>
                    ${group.isDefault ? '<span class="badge-default">Default</span>' : ''}
                </td>
                <td><code>${group.prefix || '-'}</code></td>
                <td><code>${group.suffix || '-'}</code></td>
                <td>${group.weight}</td>
                <td>${group.permissionCount} permissions</td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editGroup('${group.name}')">
                        <span>✏️</span> Edit
                    </button>
                    <button class="btn btn-sm btn-info" onclick="viewGroupPermissions('${group.name}')">
                        <span>🔑</span> Permissions
                    </button>
                    ${!group.isDefault ? `
                        <button class="btn btn-sm btn-danger" onclick="deleteGroup('${group.name}')">
                            <span>🗑️</span> Delete
                        </button>
                    ` : ''}
                </td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    table.innerHTML = html;
}

// Load all users
async function loadAllUsers() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/users`);
        PermissionSystem.users = response.users || [];

        renderUsersTable(PermissionSystem.users);
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

// Render users table
function renderUsersTable(users) {
    const table = document.getElementById('permUsersTable');
    if (!users || users.length === 0) {
        table.innerHTML = '<div class="empty-state">No users found</div>';
        return;
    }

    let html = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>Username</th>
                    <th>UUID</th>
                    <th>Group</th>
                    <th>Prefix</th>
                    <th>Suffix</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
    `;

    users.forEach(user => {
        html += `
            <tr>
                <td><strong>${user.username}</strong></td>
                <td><code class="uuid-short">${user.uuid.substring(0, 8)}...</code></td>
                <td><span class="badge-group">${user.group}</span></td>
                <td><code>${user.prefix || '-'}</code></td>
                <td><code>${user.suffix || '-'}</code></td>
                <td>
                    ${user.online ? '<span class="status-online">Online</span>' : '<span class="status-offline">Offline</span>'}
                </td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editUserPermissions('${user.username}')">
                        <span>🔑</span> Permissions
                    </button>
                    <button class="btn btn-sm btn-info" onclick="changeUserGroup('${user.username}')">
                        <span>👥</span> Change Group
                    </button>
                </td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    table.innerHTML = html;
}

// Load all available permissions
async function loadAllPermissions() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/permissions/all`);
        PermissionSystem.permissions = response.categories || [];

        renderPermissionsCategories(PermissionSystem.permissions);
    } catch (error) {
        console.error('Error loading permissions:', error);
    }
}

// Render permissions categories
function renderPermissionsCategories(categories) {
    const container = document.getElementById('permPermissionsCategories');
    if (!categories || categories.length === 0) {
        container.innerHTML = '<div class="empty-state">No permissions available</div>';
        return;
    }

    let html = '';
    categories.forEach(category => {
        html += `
            <div class="perm-category-card">
                <h4 class="perm-category-title">${category.category}</h4>
                <div class="perm-list">
        `;

        category.permissions.forEach(perm => {
            html += `
                <div class="perm-item">
                    <code class="perm-node">${perm}</code>
                    <button class="btn btn-sm btn-secondary" onclick="copyPermission('${perm}')">
                        <span>📋</span> Copy
                    </button>
                </div>
            `;
        });

        html += `
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

// Filter users based on search
function filterUsers(searchTerm) {
    const filteredUsers = PermissionSystem.users.filter(user =>
        user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.uuid.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.group.toLowerCase().includes(searchTerm.toLowerCase())
    );
    renderUsersTable(filteredUsers);
}

// Show create group modal
function showCreateGroupModal() {
    // Create modal dynamically
    const modalHTML = `
        <div class="modal" id="createGroupModal" style="display: flex;">
            <div class="modal-overlay" onclick="closeCreateGroupModal()"></div>
            <div class="modal-content">
                <div class="modal-header">
                    <h2 class="modal-title">Create New Group</h2>
                    <button class="btn-close" onclick="closeCreateGroupModal()">✕</button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="groupName">Group Name</label>
                        <input type="text" id="groupName" class="form-input" placeholder="e.g., moderator" required>
                    </div>
                    <div class="form-group">
                        <label for="groupPrefix">Prefix</label>
                        <input type="text" id="groupPrefix" class="form-input" placeholder="e.g., &2[Mod] ">
                    </div>
                    <div class="form-group">
                        <label for="groupSuffix">Suffix</label>
                        <input type="text" id="groupSuffix" class="form-input" placeholder="e.g., &r">
                    </div>
                    <div class="form-group">
                        <label for="groupWeight">Weight</label>
                        <input type="number" id="groupWeight" class="form-input" value="0">
                    </div>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="groupDefault">
                            Set as default group
                        </label>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeCreateGroupModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="submitCreateGroup()">Create Group</button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

// Close create group modal
function closeCreateGroupModal() {
    const modal = document.getElementById('createGroupModal');
    if (modal) modal.remove();
}

// Submit create group
async function submitCreateGroup() {
    const name = document.getElementById('groupName').value.trim();
    const prefix = document.getElementById('groupPrefix').value;
    const suffix = document.getElementById('groupSuffix').value;
    const weight = parseInt(document.getElementById('groupWeight').value);
    const isDefault = document.getElementById('groupDefault').checked;

    if (!name) {
        alert('Group name is required');
        return;
    }

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/group/create`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, prefix, suffix, weight, isDefault })
        });

        if (response.success) {
            showNotification('Group created successfully', 'success');
            closeCreateGroupModal();
            await loadAllGroups();
        } else {
            showNotification('Failed to create group: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error creating group:', error);
        showNotification('Error creating group', 'error');
    }
}

// Edit group
async function editGroup(groupName) {
    const group = PermissionSystem.groups.find(g => g.name === groupName);
    if (!group) return;

    const modalHTML = `
        <div class="modal" id="editGroupModal" style="display: flex;">
            <div class="modal-overlay" onclick="closeEditGroupModal()"></div>
            <div class="modal-content">
                <div class="modal-header">
                    <h2 class="modal-title">Edit Group: ${groupName}</h2>
                    <button class="btn-close" onclick="closeEditGroupModal()">✕</button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="editGroupPrefix">Prefix</label>
                        <input type="text" id="editGroupPrefix" class="form-input" value="${group.prefix || ''}">
                    </div>
                    <div class="form-group">
                        <label for="editGroupSuffix">Suffix</label>
                        <input type="text" id="editGroupSuffix" class="form-input" value="${group.suffix || ''}">
                    </div>
                    <div class="form-group">
                        <label for="editGroupWeight">Weight</label>
                        <input type="number" id="editGroupWeight" class="form-input" value="${group.weight}">
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeEditGroupModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="submitEditGroup('${groupName}')">Save Changes</button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

window.closeEditGroupModal = function() {
    const modal = document.getElementById('editGroupModal');
    if (modal) modal.remove();
};

window.submitEditGroup = async function(groupName) {
    const prefix = document.getElementById('editGroupPrefix').value;
    const suffix = document.getElementById('editGroupSuffix').value;
    const weight = parseInt(document.getElementById('editGroupWeight').value);

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/group/${groupName}/update`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prefix, suffix, weight })
        });

        if (response.success) {
            showNotification('Group updated successfully', 'success');
            closeEditGroupModal();
            await loadAllGroups();
        } else {
            showNotification('Failed to update group: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error updating group:', error);
        showNotification('Error updating group', 'error');
    }
};

// View group permissions
async function viewGroupPermissions(groupName) {
    try {
        const group = await fetchWithAuth(`${API_BASE_URL}/permissions/group/${groupName}`);

        const modalHTML = `
            <div class="modal" id="groupPermModal" style="display: flex;">
                <div class="modal-overlay" onclick="closeGroupPermModal()"></div>
                <div class="modal-content modal-large">
                    <div class="modal-header">
                        <h2 class="modal-title">Permissions for: ${groupName}</h2>
                        <button class="btn-close" onclick="closeGroupPermModal()">✕</button>
                    </div>
                    <div class="modal-body">
                        <div class="perm-actions-bar">
                            <input type="text" class="form-input" id="newGroupPerm" placeholder="Enter permission node...">
                            <button class="btn btn-primary" onclick="addGroupPermission('${groupName}')">
                                <span>➕</span> Add Permission
                            </button>
                        </div>
                        <div class="perm-list" id="groupPermList">
                            ${group.permissions && group.permissions.length > 0 ?
                                group.permissions.map(perm => `
                                    <div class="perm-item">
                                        <code class="perm-node">${perm}</code>
                                        <button class="btn btn-sm btn-danger" onclick="removeGroupPermission('${groupName}', '${perm}')">
                                            <span>🗑️</span> Remove
                                        </button>
                                    </div>
                                `).join('') :
                                '<div class="empty-state">No permissions assigned</div>'
                            }
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeGroupPermModal()">Close</button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);
    } catch (error) {
        console.error('Error loading group permissions:', error);
    }
}

window.closeGroupPermModal = function() {
    const modal = document.getElementById('groupPermModal');
    if (modal) modal.remove();
};

window.addGroupPermission = async function(groupName) {
    const permission = document.getElementById('newGroupPerm').value.trim();
    if (!permission) return;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/group/${groupName}/permission/add`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ permission })
        });

        if (response.success) {
            showNotification('Permission added', 'success');
            closeGroupPermModal();
            await viewGroupPermissions(groupName);
        } else {
            showNotification('Failed to add permission: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error adding permission:', error);
    }
};

window.removeGroupPermission = async function(groupName, permission) {
    if (!confirm(`Remove permission "${permission}" from group "${groupName}"?`)) return;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/group/${groupName}/permission/remove/${encodeURIComponent(permission)}`, {
            method: 'DELETE'
        });

        if (response.success) {
            showNotification('Permission removed', 'success');
            closeGroupPermModal();
            await viewGroupPermissions(groupName);
        } else {
            showNotification('Failed to remove permission: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error removing permission:', error);
    }
};

// Delete group
async function deleteGroup(groupName) {
    if (!confirm(`Are you sure you want to delete group "${groupName}"?`)) return;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/group/${groupName}`, {
            method: 'DELETE'
        });

        if (response.success) {
            showNotification('Group deleted successfully', 'success');
            await loadAllGroups();
        } else {
            showNotification('Failed to delete group: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error deleting group:', error);
        showNotification('Error deleting group', 'error');
    }
}

// Edit user permissions
async function editUserPermissions(username) {
    try {
        const user = await fetchWithAuth(`${API_BASE_URL}/permissions/user/${username}`);

        const modalHTML = `
            <div class="modal" id="userPermModal" style="display: flex;">
                <div class="modal-overlay" onclick="closeUserPermModal()"></div>
                <div class="modal-content modal-large">
                    <div class="modal-header">
                        <h2 class="modal-title">Permissions for: ${username}</h2>
                        <button class="btn-close" onclick="closeUserPermModal()">✕</button>
                    </div>
                    <div class="modal-body">
                        <div class="user-info-section">
                            <p><strong>Group:</strong> ${user.group}</p>
                            <p><strong>Prefix:</strong> <code>${user.prefix || '-'}</code></p>
                            <p><strong>Suffix:</strong> <code>${user.suffix || '-'}</code></p>
                        </div>
                        <div class="perm-actions-bar">
                            <input type="text" class="form-input" id="newUserPerm" placeholder="Enter permission node...">
                            <button class="btn btn-primary" onclick="addUserPermission('${username}')">
                                <span>➕</span> Add Permission
                            </button>
                        </div>
                        <div class="perm-list" id="userPermList">
                            ${user.permissions && user.permissions.length > 0 ?
                                user.permissions.map(perm => `
                                    <div class="perm-item">
                                        <code class="perm-node">${perm}</code>
                                        <button class="btn btn-sm btn-danger" onclick="removeUserPermission('${username}', '${perm}')">
                                            <span>🗑️</span> Remove
                                        </button>
                                    </div>
                                `).join('') :
                                '<div class="empty-state">No custom permissions assigned (inherits from group)</div>'
                            }
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeUserPermModal()">Close</button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);
    } catch (error) {
        console.error('Error loading user permissions:', error);
    }
}

window.closeUserPermModal = function() {
    const modal = document.getElementById('userPermModal');
    if (modal) modal.remove();
};

window.addUserPermission = async function(username) {
    const permission = document.getElementById('newUserPerm').value.trim();
    if (!permission) return;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/user/${username}/permission/add`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ permission })
        });

        if (response.success) {
            showNotification('Permission added', 'success');
            closeUserPermModal();
            await editUserPermissions(username);
        } else {
            showNotification('Failed to add permission: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error adding permission:', error);
    }
};

window.removeUserPermission = async function(username, permission) {
    if (!confirm(`Remove permission "${permission}" from user "${username}"?`)) return;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/user/${username}/permission/remove/${encodeURIComponent(permission)}`, {
            method: 'DELETE'
        });

        if (response.success) {
            showNotification('Permission removed', 'success');
            closeUserPermModal();
            await editUserPermissions(username);
        } else {
            showNotification('Failed to remove permission: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error removing permission:', error);
    }
};

// Change user group
async function changeUserGroup(username) {
    const groups = PermissionSystem.groups.map(g => g.name);

    const modalHTML = `
        <div class="modal" id="changeGroupModal" style="display: flex;">
            <div class="modal-overlay" onclick="closeChangeGroupModal()"></div>
            <div class="modal-content">
                <div class="modal-header">
                    <h2 class="modal-title">Change Group for: ${username}</h2>
                    <button class="btn-close" onclick="closeChangeGroupModal()">✕</button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="newUserGroup">Select Group</label>
                        <select id="newUserGroup" class="form-input">
                            ${groups.map(group => `<option value="${group}">${group}</option>`).join('')}
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeChangeGroupModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="submitChangeGroup('${username}')">Change Group</button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

window.closeChangeGroupModal = function() {
    const modal = document.getElementById('changeGroupModal');
    if (modal) modal.remove();
};

window.submitChangeGroup = async function(username) {
    const group = document.getElementById('newUserGroup').value;

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/permissions/user/${username}/group/set`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ group })
        });

        if (response.success) {
            showNotification('Group changed successfully', 'success');
            closeChangeGroupModal();
            await loadAllUsers();
        } else {
            showNotification('Failed to change group: ' + response.message, 'error');
        }
    } catch (error) {
        console.error('Error changing group:', error);
    }
};

// Copy permission to clipboard
function copyPermission(permission) {
    navigator.clipboard.writeText(permission).then(() => {
        showNotification('Permission copied to clipboard', 'success');
    }).catch(err => {
        console.error('Error copying permission:', err);
    });
}

// Disable permission management for external systems
function disablePermissionManagement() {
    const createGroupBtn = document.getElementById('createGroupBtn');
    if (createGroupBtn) {
        createGroupBtn.disabled = true;
        createGroupBtn.title = 'Not available with external permission system';
    }
}

// Show notification
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.classList.add('show');
    }, 10);

    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Utility function to capitalize first letter
function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

// Export functions to window for onclick handlers
window.showCreateGroupModal = showCreateGroupModal;
window.closeCreateGroupModal = closeCreateGroupModal;
window.submitCreateGroup = submitCreateGroup;
window.editGroup = editGroup;
window.viewGroupPermissions = viewGroupPermissions;
window.deleteGroup = deleteGroup;
window.editUserPermissions = editUserPermissions;
window.changeUserGroup = changeUserGroup;
window.filterUsers = filterUsers;
window.copyPermission = copyPermission;

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initPermissionSystem);
} else {
    // DOM already loaded, init immediately if on permissions page
    if (window.location.hash === '#permissions' || document.querySelector('[data-page="permissions"].active')) {
        initPermissionSystem();
    }
}
