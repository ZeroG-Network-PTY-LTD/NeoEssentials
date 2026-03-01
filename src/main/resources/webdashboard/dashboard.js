// NeoEssentials Dashboard JavaScript

// Configuration
const API_BASE_URL = window.location.origin + '/api';
const REFRESH_INTERVAL = 5000; // 5 seconds

// State
let refreshTimer = null;
let lastUpdateTime = 0;

// Helper function to make authenticated API calls
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('authToken');
    
    if (!token) {
        console.error('No authentication token available');
        showLoginScreen();
        throw new Error('No authentication token');
    }
    
    // Add authorization header
    const headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json'
    };
    
    try {
        const response = await fetch(url, {
            ...options,
            headers
        });
        
        // Check if unauthorized (token expired)
        if (response.status === 401) {
            console.warn('Authentication token expired or invalid');
            localStorage.removeItem('authToken');
            showLoginScreen();
            throw new Error('Session expired - please login again');
        }
        
        return response;
    } catch (error) {
        console.error('Fetch error:', error);
        throw error;
    }
}

// Initialize dashboard when DOM is loaded
document.addEventListener('DOMContentLoaded', async () => {
    console.log('NeoEssentials Dashboard v2.0 initialized');
    
    // Set up event listeners first
    setupEventListeners();
    
    // Setup navigation
    setupNavigation();
    
    console.log('Available API endpoints:');
    console.log('  - /api/auth/* - Authentication endpoints');
    console.log('  - /api/player/online - Get online players');
    console.log('  - /api/server/status - Get server status');
    console.log('  - /api/server/profile - Get server profile');
    console.log('  - /api/server/statistics - Get server statistics');
    console.log('  - /api/server/performance - Get server performance');
    console.log('  - /api/server/worlds - Get worlds info');
    console.log('  - /api/game/events - Get game events');
    console.log('  - /api/game/statistics - Get game statistics');
    
    // Check authentication - WAIT for this to complete before loading data
    await checkAuthentication();
});

// Authentication check
async function checkAuthentication() {
    const token = localStorage.getItem('authToken');
    
    if (!token) {
        showLoginScreen();
        return;
    }
    
    // Validate token with server
    try {
        const response = await fetch(`${API_BASE_URL}/auth/validate`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('username', data.username);
            localStorage.setItem('isAdmin', data.isAdmin);
            localStorage.setItem('authType', data.authType);
            showDashboard();
        } else {
            // Token invalid or expired
            localStorage.removeItem('authToken');
            showLoginScreen();
        }
    } catch (error) {
        console.error('Error validating token:', error);
        showLoginScreen();
    }
}

function showLoginScreen() {
    const loginContainer = document.getElementById('loginContainer');
    const dashboardWrapper = document.getElementById('dashboardWrapper');
    
    if (loginContainer) loginContainer.style.display = 'flex';
    if (dashboardWrapper) dashboardWrapper.style.display = 'none';
}

function showDashboard() {
    const loginContainer = document.getElementById('loginContainer');
    const dashboardWrapper = document.getElementById('dashboardWrapper');
    
    if (loginContainer) loginContainer.style.display = 'none';
    if (dashboardWrapper) dashboardWrapper.style.display = 'flex';
    
    // Update username display
    const username = localStorage.getItem('username');
    const usernameDisplay = document.getElementById('usernameDisplay');
    if (usernameDisplay && username) {
        usernameDisplay.textContent = username;
    }
    
    // Show admin controls if user is admin
    const isAdmin = localStorage.getItem('isAdmin') === 'true';
    const adminElements = document.querySelectorAll('.admin-only');
    adminElements.forEach(element => {
        if (isAdmin) {
            element.classList.add('visible');
            element.style.display = ''; // Remove inline display:none
        } else {
            element.classList.remove('visible');
            element.style.display = 'none';
        }
    });

    console.log('Dashboard authenticated, starting data refresh...');
    console.log('Admin mode:', isAdmin);

    // Start auto-refresh timer
    startAutoRefresh();
    
    // Load initial data
    refreshData();
}

// Handle logout
async function handleLogout() {
    const token = localStorage.getItem('authToken');
    
    if (token) {
        try {
            await fetch(`${API_BASE_URL}/auth/logout`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
        } catch (error) {
            console.error('Logout error:', error);
        }
    }
    
    // Clear local storage
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    localStorage.removeItem('isAdmin');
    localStorage.removeItem('authType');
    
    // Show login screen
    showLoginScreen();
    
    // Stop auto-refresh
    if (refreshTimer) {
        clearInterval(refreshTimer);
        refreshTimer = null;
    }
}

// Setup event listeners
function setupEventListeners() {
    // Refresh button
    const refreshBtn = document.getElementById('refreshBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', async () => {
            const icon = refreshBtn.querySelector('span');
            if (icon) {
                icon.style.animation = 'spin 1s linear infinite';
                refreshBtn.disabled = true;
            }
            
            await refreshData();
            
            if (icon) {
                setTimeout(() => {
                    icon.style.animation = '';
                    refreshBtn.disabled = false;
                }, 500);
            }
        });
    }
    
    // Login form
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            handleLogin();
        });
    }
    
    // Sidebar toggle for mobile
    const sidebarToggle = document.getElementById('sidebarToggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', () => {
            document.querySelector('.sidebar').classList.toggle('open');
        });
    }
    
    // Clear events button
    const clearEventsBtn = document.getElementById('clearEventsBtn');
    if (clearEventsBtn) {
        clearEventsBtn.addEventListener('click', () => {
            document.getElementById('eventsList').innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">📜</span>
                    <p>No recent events</p>
                </div>
            `;
        });
    }
    
    // Logout button
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            await handleLogout();
        });
    }

    // Admin Control Buttons
    const restartServerBtn = document.getElementById('restartServerBtn');
    if (restartServerBtn) {
        restartServerBtn.addEventListener('click', () => handleRestartServer());
    }

    const stopServerBtn = document.getElementById('stopServerBtn');
    if (stopServerBtn) {
        stopServerBtn.addEventListener('click', () => handleStopServer());
    }

    const reloadConfigsBtn = document.getElementById('reloadConfigsBtn');
    if (reloadConfigsBtn) {
        reloadConfigsBtn.addEventListener('click', () => handleReloadConfigs());
    }

    const saveWorldsBtn = document.getElementById('saveWorldsBtn');
    if (saveWorldsBtn) {
        saveWorldsBtn.addEventListener('click', () => handleSaveWorlds());
    }

    // Navigation items - Page switching
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const page = item.getAttribute('data-page');
            if (page) {
                switchPage(page);
            }
        });
    });
}

// Switch between dashboard pages
function switchPage(pageName) {
    // Update active nav item
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        if (item.getAttribute('data-page') === pageName) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Hide all page sections
    const pageCards = document.querySelectorAll('[data-page]');
    pageCards.forEach(card => {
        if (card.classList.contains('nav-item')) {
            // Skip nav items
            return;
        }
        if (card.getAttribute('data-page') === pageName) {
            card.style.display = '';
        } else {
            card.style.display = 'none';
        }
    });

    // Update page title
    const pageTitle = document.getElementById('pageTitle');
    if (pageTitle) {
        const pageTitles = {
            'overview': 'Overview',
            'players': 'Players',
            'performance': 'Performance',
            'worlds': 'Worlds',
            'events': 'Events',
            'admin': 'Admin Controls'
        };
        pageTitle.textContent = pageTitles[pageName] || 'Dashboard';
    }

    // If switching to overview, show stats cards
    const statsRow = document.querySelector('.stats-row');
    if (statsRow) {
        statsRow.style.display = pageName === 'overview' ? '' : 'none';
    }

    console.log('Switched to page:', pageName);
}

// Handle login
async function handleLogin() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password') ? document.getElementById('password').value : null;
    const loginError = document.getElementById('loginError');
    
    if (!username || username.trim() === '') {
        if (loginError) {
            loginError.textContent = 'Please enter your dashboard username';
            loginError.style.display = 'block';
        }
        return;
    }
    
    // Password-based authentication (new system)
    if (password !== null) {
        if (!password || password.trim() === '') {
            if (loginError) {
                loginError.textContent = 'Please enter your password';
                loginError.style.display = 'block';
            }
            return;
        }
    }

    // Clear previous error
    if (loginError) loginError.style.display = 'none';
    
    // Show loading state
    const loginBtn = document.querySelector('#loginForm button[type="submit"]');
    const originalText = loginBtn ? loginBtn.textContent : 'Login';
    if (loginBtn) {
        loginBtn.disabled = true;
        loginBtn.textContent = 'Authenticating...';
    }
    
    try {
        // Prepare login request based on available fields
        let requestBody;
        if (password !== null) {
            // Password-based authentication (new registration system)
            requestBody = {
                username: username.trim(),
                password: password.trim()
            };
        } else {
            // Legacy Minecraft authentication (deprecated, requires online)
            requestBody = {
                username: username.trim(),
                type: 'minecraft'
            };
        }

        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            // Store auth data
            localStorage.setItem('authToken', data.sessionId || data.token);
            localStorage.setItem('sessionId', data.sessionId);
            localStorage.setItem('authType', data.authType || 'password');

            // Extract user data
            if (data.user) {
                localStorage.setItem('username', data.user.username || username.trim());
                localStorage.setItem('isAdmin', data.user.role === 'ADMIN' || data.user.isAdmin || false);
                localStorage.setItem('role', data.user.role || 'VIEWER');
            } else {
                localStorage.setItem('username', username.trim());
                localStorage.setItem('isAdmin', false);
                localStorage.setItem('role', 'VIEWER');
            }

            // Show dashboard
            showDashboard();
            
            console.log('Login successful:', localStorage.getItem('username'), 'Role:', localStorage.getItem('role'));
        } else {
            // Show error
            if (loginError) {
                let errorMessage = data.error || 'Authentication failed';
                if (errorMessage.includes('online')) {
                    errorMessage = 'Invalid credentials. Register in-game: /dashboardregister start';
                }
                loginError.textContent = errorMessage;
                loginError.style.display = 'block';
            }
            console.error('Login failed:', data.error);
        }
    } catch (error) {
        console.error('Login error:', error);
        if (loginError) {
            loginError.textContent = 'Connection error. Is the server running?';
            loginError.style.display = 'block';
        }
    } finally {
        // Reset button state
        if (loginBtn) {
            loginBtn.disabled = false;
            loginBtn.textContent = originalText;
        }
    }
}

// Setup navigation
function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            
            // Remove active class from all items
            navItems.forEach(nav => nav.classList.remove('active'));
            
            // Add active class to clicked item
            item.classList.add('active');
            
            // Get page name
            const pageName = item.getAttribute('data-page');
            const pageTitle = item.querySelector('.nav-text').textContent;
            
            // Update page title
            const pageTitleElement = document.getElementById('pageTitle');
            if (pageTitleElement) {
                pageTitleElement.textContent = pageTitle;
            }
            
            // Switch to the selected page
            switchPage(pageName);
            
            console.log('Navigated to:', pageTitle);
        });
    });
}

// Switch between different dashboard pages
function switchPage(pageName) {
    const statsRow = document.querySelector('.stats-row');
    const allCards = document.querySelectorAll('.card, .card-large');
    
    // Show/hide stats row based on page
    if (statsRow) {
        if (pageName === 'overview' || pageName === 'players' || pageName === 'performance') {
            statsRow.style.display = 'grid';
        } else {
            statsRow.style.display = 'none';
        }
    }
    
    // Show/hide cards based on data-page attribute
    allCards.forEach(card => {
        const cardPages = card.getAttribute('data-page');
        if (cardPages) {
            // Check if current page is in the card's data-page list
            const pages = cardPages.split(' ');
            if (pages.includes(pageName)) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        }
    });
}

// Auto-refresh functionality
function startAutoRefresh() {
    if (refreshTimer) {
        clearInterval(refreshTimer);
    }
    
    refreshTimer = setInterval(() => {
        refreshData();
    }, REFRESH_INTERVAL);
}

function stopAutoRefresh() {
    if (refreshTimer) {
        clearInterval(refreshTimer);
        refreshTimer = null;
    }
}

// Main refresh function
async function refreshData() {
    try {
        updateApiStatus('Refreshing...', true);
        lastUpdateTime = Date.now();
        
        // Fetch all data in parallel with individual error handling
        const results = await Promise.allSettled([
            loadServerStatus(),
            loadServerStatistics(),
            loadAllPlayers(),
            loadGameEvents(),
            loadServerInfo(),
            loadWorlds(),
            loadGameStatistics()
        ]);
        
        // Check if any failed
        const failedCount = results.filter(r => r.status === 'rejected').length;
        if (failedCount > 0) {
            console.warn(`${failedCount} API calls failed`);
            updateApiStatus('Partial Data', true);
        } else {
            updateApiStatus('Connected', true);
        }
        
    } catch (error) {
        console.error('Error refreshing data:', error);
        updateApiStatus('Connection Error', false);
    }
}

// Load server status
async function loadServerStatus() {
    try {
        console.log('Fetching server status from:', `${API_BASE_URL}/server/status`);
        const response = await fetchWithAuth(`${API_BASE_URL}/server/status`);
        
        if (!response.ok) {
            console.warn(`Server status API returned ${response.status}: ${response.statusText}`);
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Server status response:', data);
        
        if (data && data.online !== undefined) {
            // Update server status badge
            const statusBadge = document.getElementById('serverStatusBadge');
            if (statusBadge) {
                if (data.online) {
                    statusBadge.innerHTML = '<span class="dot"></span><span class="text">Online</span>';
                    statusBadge.classList.remove('offline');
                } else {
                    statusBadge.innerHTML = '<span class="dot"></span><span class="text">Offline</span>';
                    statusBadge.classList.add('offline');
                }
            }
            
            // Update sidebar status
            const statusMini = document.querySelector('.server-status-mini .status-text');
            if (statusMini) {
                statusMini.textContent = data.online ? 'Server Online' : 'Server Offline';
            }
            
            // Update player count
            if (data.playersOnline !== undefined) {
                const playerNumEl = document.querySelector('#playerCount .number');
                const playerMaxEl = document.querySelector('#playerCount .max');
                if (playerNumEl) playerNumEl.textContent = data.playersOnline || 0;
                if (playerMaxEl) playerMaxEl.textContent = `/ ${data.playersMax || 20}`;
            }
            
            // Update TPS
            if (data.tps !== undefined) {
                // TPS comes as a string from the API, parse it
                const tps = typeof data.tps === 'string' ? parseFloat(data.tps) : data.tps;
                const tpsNumEl = document.querySelector('#tpsValue .number');
                if (tpsNumEl) tpsNumEl.textContent = tps.toFixed(1);
                updateProgressBar('tpsProgress', (tps / 20) * 100, tps);
            }
            
            // Update uptime - API returns uptimeMillis or uptimeFormatted
            if (data.uptimeFormatted !== undefined) {
                const uptimeEl = document.querySelector('#uptimeValue .number');
                if (uptimeEl) uptimeEl.textContent = data.uptimeFormatted;
            } else if (data.uptimeMillis !== undefined) {
                const formatted = formatUptime(data.uptimeMillis / 1000); // Convert to seconds
                const uptimeEl = document.querySelector('#uptimeValue .number');
                if (uptimeEl) uptimeEl.textContent = formatted;
            }
        } else {
            console.warn('Invalid server status data structure:', data);
            // Don't show offline if we got data, just log the warning
        }
    } catch (error) {
        console.error('Error loading server status:', error);
        
        // Only show offline if it's a real connection error, not auth error
        if (error.message !== 'Session expired - please login again') {
            const statusBadge = document.getElementById('serverStatusBadge');
            if (statusBadge) {
                statusBadge.innerHTML = '<span class="dot"></span><span class="text">Connection Error</span>';
                statusBadge.classList.add('offline');
            }
        }
        // Auth errors will be handled by fetchWithAuth redirecting to login
    }
}

// Load server statistics (including memory)
async function loadServerStatistics() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/server/statistics`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data && data.memory) {
            // Update memory
            const memory = data.memory;
            const usedMB = memory.usedMB || 0;
            const maxMB = memory.maxMB || 1;
            const percent = memory.usedPercent || 0;
            
            const memNumEl = document.querySelector('#memoryValue .number');
            const memUnitEl = document.querySelector('#memoryValue .unit');
            
            if (memNumEl) memNumEl.textContent = usedMB.toFixed(0);
            if (memUnitEl) memUnitEl.textContent = `MB / ${maxMB.toFixed(0)} MB`;
            
            const memBar = document.getElementById('memoryProgress');
            if (memBar) {
                memBar.style.width = `${Math.min(percent, 100)}%`;
                
                // Update color based on usage
                if (percent >= 85) {
                    memBar.style.background = 'linear-gradient(90deg, var(--danger), var(--danger-light))';
                } else if (percent >= 70) {
                    memBar.style.background = 'linear-gradient(90deg, var(--warning), var(--warning-light))';
                } else {
                    memBar.style.background = 'linear-gradient(90deg, var(--success), var(--success-light))';
                }
            }
        }
    } catch (error) {
        console.error('Error loading server statistics:', error);
    }
}

// Load all players (online and offline)
async function loadAllPlayers() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/player/online`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data && data.players) {
            const onlinePlayers = data.players || [];
            const offlinePlayers = data.offlinePlayers || [];
            
            // Update online players
            const onlineListElement = document.getElementById('onlinePlayersList');
            const onlineBadge = document.getElementById('onlinePlayersBadge');
            
            if (onlineBadge) {
                onlineBadge.textContent = onlinePlayers.length;
            }
            
            if (onlineListElement) {
                if (onlinePlayers.length === 0) {
                    onlineListElement.innerHTML = `
                        <div class="empty-state">
                            <span class="empty-icon">👥</span>
                            <p>No players online</p>
                        </div>
                    `;
                } else {
                    onlineListElement.innerHTML = onlinePlayers.map(player => `
                    <div class="player-item" onclick="openPlayerModal('${escapeHtml(player.username || 'Unknown')}', true)">
                        <div class="player-avatar">
                            ${player.username ? player.username.charAt(0).toUpperCase() : '?'}
                        </div>
                        <div class="player-info">
                            <div class="player-name">${escapeHtml(player.username || 'Unknown')}</div>
                            <div class="player-details">
                                ${player.gameMode || 'Unknown'} • 
                                HP: ${player.health !== undefined ? player.health.toFixed(1) : '?'}
                            </div>
                        </div>
                        <div class="player-ping ${getPingClass(player.ping)}">
                            ${player.ping !== undefined ? player.ping : 0}ms
                        </div>
                    </div>
                `).join('');
                }
            }
            
            // Update offline players
            const offlineListElement = document.getElementById('offlinePlayersList');
            const offlineBadge = document.getElementById('offlinePlayersBadge');
            
            if (offlineBadge) {
                offlineBadge.textContent = offlinePlayers.length;
            }
            
            if (offlineListElement) {
                if (offlinePlayers.length === 0) {
                    offlineListElement.innerHTML = `
                        <div class="empty-state">
                            <span class="empty-icon">💤</span>
                            <p>No offline players</p>
                        </div>
                    `;
                } else {
                    offlineListElement.innerHTML = offlinePlayers.map(player => `
                    <div class="player-item" onclick="openPlayerModal('${escapeHtml(player.username || 'Unknown')}', false)">
                        <div class="player-avatar">
                            ${player.username ? player.username.charAt(0).toUpperCase() : '?'}
                        </div>
                        <div class="player-info">
                            <div class="player-name">${escapeHtml(player.username || 'Unknown')}</div>
                            <div class="player-details">
                                Last seen: ${player.lastSeen || 'Unknown'}
                            </div>
                        </div>
                    </div>
                `).join('');
                }
            }
        }
    } catch (error) {
        console.error('Error loading players:', error);
        const onlinePlayersList = document.getElementById('onlinePlayersList');
        const offlinePlayersList = document.getElementById('offlinePlayersList');
        if (onlinePlayersList) {
            onlinePlayersList.innerHTML = '<p class="empty-state">Error loading players</p>';
        }
        if (offlinePlayersList) {
            offlinePlayersList.innerHTML = '<p class="empty-state">Error loading players</p>';
        }
    }
}

// Load game events
async function loadGameEvents() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/game/events`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data && data.events) {
            const events = data.events || [];
            const listElement = document.getElementById('eventsList');
            
            if (listElement) {
                if (events.length === 0) {
                    listElement.innerHTML = `
                        <div class="empty-state">
                            <span class="empty-icon">📜</span>
                            <p>No recent events</p>
                        </div>
                    `;
                } else {
                    // Take only the last 20 events
                    const recentEvents = events.slice(-20).reverse();
                    
                    listElement.innerHTML = recentEvents.map(event => {
                    const eventClass = getEventClass(event.type);
                    const timeAgo = formatTimeAgo(event.timestamp);
                    
                    return `
                        <div class="event-item ${eventClass}">
                            <span class="event-icon">${getEventIcon(event.type)}</span>
                            <span class="event-message">${escapeHtml(event.message)}</span>
                            <span class="event-time">${timeAgo}</span>
                        </div>
                    `;
                }).join('');
                }
            }
        }
    } catch (error) {
        console.error('Error loading game events:', error);
        const eventsList = document.getElementById('eventsList');
        if (eventsList) {
            eventsList.innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">❌</span>
                    <p>Error loading events</p>
                </div>
            `;
        }
    }
}

// Load server info
async function loadServerInfo() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/server/profile`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data) {
            const infoElement = document.getElementById('serverInfo');
            
            if (infoElement) {
                infoElement.innerHTML = `
                <div class="info-row">
                    <span class="info-label">Minecraft</span>
                    <span class="info-value">${escapeHtml(data.minecraftVersion || 'Unknown')}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">NeoForge</span>
                    <span class="info-value">${escapeHtml(data.neoforgeVersion || 'Unknown')}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Difficulty</span>
                    <span class="info-value">${escapeHtml(data.difficulty || 'Unknown')}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Mods</span>
                    <span class="info-value">${data.modsLoaded || 0}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">MOTD</span>
                    <span class="info-value motd-display">${convertMinecraftColors(data.motd || 'N/A')}</span>
                </div>
            `;
            }
        }
    } catch (error) {
        console.error('Error loading server info:', error);
    }
}

// Load worlds
async function loadWorlds() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/server/worlds`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data && data.worlds) {
            const worlds = data.worlds || [];
            const listElement = document.getElementById('worldsList');
            const badge = document.getElementById('worldsBadge');
            
            if (badge) {
                badge.textContent = worlds.length;
            }
            
            if (listElement) {
                if (worlds.length === 0) {
                    listElement.innerHTML = `
                        <div class="empty-state">
                            <span class="empty-icon">🌍</span>
                            <p>No worlds loaded</p>
                        </div>
                    `;
                } else {
                    listElement.innerHTML = worlds.map(world => `
                    <div class="world-item">
                        <div class="world-name">🌍 ${escapeHtml(world.name || 'Unknown')}</div>
                        <div class="world-info">
                            ${world.playersInWorld || 0} players • 
                            ${world.loadedChunks || 0} chunks • 
                            ${world.entities || 0} entities
                        </div>
                    </div>
                `).join('');
                }
            }
        }
    } catch (error) {
        console.error('Error loading worlds:', error);
        const worldsList = document.getElementById('worldsList');
        if (worldsList) {
            worldsList.innerHTML = '<p class="empty-state">Error loading worlds</p>';
        }
    }
}

// Load game statistics
async function loadGameStatistics() {
    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/game/statistics`);
        
        if (!response.ok) {
            return; // Silent fail for optional stats
        }
        
        const data = await response.json();
        
        // You can use this data to enhance the dashboard with additional stats
        if (data) {
            console.log('Game statistics loaded:', data);
        }
    } catch (error) {
        // Silent fail for optional data
        console.debug('Game statistics not available');
    }
}

// Helper functions

function updateProgressBar(id, percent, value) {
    const element = document.getElementById(id);
    if (!element) return;
    
    element.style.width = `${Math.min(percent, 100)}%`;
    
    // Update color based on value
    if (id === 'tpsProgress') {
        if (value >= 18) {
            element.style.background = 'linear-gradient(90deg, var(--success), var(--success-light))';
        } else if (value >= 15) {
            element.style.background = 'linear-gradient(90deg, var(--warning), var(--warning-light))';
        } else {
            element.style.background = 'linear-gradient(90deg, var(--danger), var(--danger-light))';
        }
    } else if (id === 'memoryProgress') {
        if (percent < 70) {
            element.style.background = 'linear-gradient(90deg, var(--success), var(--success-light))';
        } else if (percent < 85) {
            element.style.background = 'linear-gradient(90deg, var(--warning), var(--warning-light))';
        } else {
            element.style.background = 'linear-gradient(90deg, var(--danger), var(--danger-light))';
        }
    }
}

function getPingClass(ping) {
    if (ping < 100) return '';
    if (ping < 200) return 'medium';
    return 'high';
}

function getEventClass(type) {
    const t = type ? type.toLowerCase() : '';
    if (t.includes('join') || t.includes('login')) return 'join';
    if (t.includes('leave') || t.includes('quit') || t.includes('disconnect')) return 'leave';
    if (t.includes('death') || t.includes('died') || t.includes('killed')) return 'death';
    if (t.includes('chat') || t.includes('message')) return 'chat';
    return '';
}

function getEventIcon(type) {
    const t = type ? type.toLowerCase() : '';
    if (t.includes('join') || t.includes('login')) return '➕';
    if (t.includes('leave') || t.includes('quit') || t.includes('disconnect')) return '➖';
    if (t.includes('death') || t.includes('died') || t.includes('killed')) return '💀';
    if (t.includes('chat') || t.includes('message')) return '💬';
    if (t.includes('achievement')) return '🏆';
    if (t.includes('advancement')) return '⭐';
    return '📝';
}

function formatTimeAgo(timestamp) {
    const now = Date.now();
    const diff = now - timestamp;
    
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) return `${days}d ago`;
    if (hours > 0) return `${hours}h ago`;
    if (minutes > 0) return `${minutes}m ago`;
    if (seconds > 0) return `${seconds}s ago`;
    return `just now`;
}

function formatUptime(seconds) {
    if (!seconds || seconds < 0) return '0s';
    
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    
    const parts = [];
    if (days > 0) parts.push(`${days}d`);
    if (hours > 0) parts.push(`${hours}h`);
    if (minutes > 0) parts.push(`${minutes}m`);
    if (secs > 0 || parts.length === 0) parts.push(`${secs}s`);
    
    return parts.slice(0, 2).join(' ');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function updateApiStatus(status, isConnected) {
    // Update footer API status
    const element = document.getElementById('apiStatus');
    if (element) {
        const statusDot = element.querySelector('.status-dot');
        const statusText = element.textContent || element.innerText;
        
        if (statusDot) {
            element.innerHTML = `<span class="status-dot"></span>${status}`;
        } else {
            element.textContent = status;
        }
        
        if (isConnected) {
            element.classList.remove('error');
            element.classList.add('success');
        } else {
            element.classList.remove('success');
            element.classList.add('error');
        }
    }
}

// Handle page visibility change
document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        console.log('Dashboard hidden - stopping auto-refresh');
        stopAutoRefresh();
    } else {
        console.log('Dashboard visible - resuming auto-refresh');
        startAutoRefresh();
        refreshData();
    }
});

// Handle window unload
window.addEventListener('beforeunload', () => {
    stopAutoRefresh();
});

// Add keyboard shortcuts
document.addEventListener('keydown', (e) => {
    // Ctrl+R or F5 to refresh
    if ((e.ctrlKey && e.key === 'r') || e.key === 'F5') {
        e.preventDefault();
        refreshData();
    }
});

// Player Modal Functions - Make them global so onclick attributes can access them
window.openPlayerModal = async function(username, isOnline) {
    const modal = document.getElementById('playerModal');
    const modalPlayerName = document.getElementById('modalPlayerName');
    const modalPlayerAvatar = document.getElementById('modalPlayerAvatar');
    const modalPlayerStatus = document.getElementById('modalPlayerStatus');
    
    // Show modal
    modal.style.display = 'flex';
    
    // Set basic info
    modalPlayerName.textContent = username;
    modalPlayerAvatar.textContent = username.charAt(0).toUpperCase();
    
    // Set status
    const statusDot = modalPlayerStatus.querySelector('.status-dot');
    const statusText = modalPlayerStatus.querySelector('span:last-child');
    
    if (isOnline) {
        statusDot.classList.remove('offline');
        statusText.textContent = 'Online';
    } else {
        statusDot.classList.add('offline');
        statusText.textContent = 'Offline';
    }
    
    // Load player data
    await loadPlayerDetails(username, isOnline);
}

window.closePlayerModal = function() {
    const modal = document.getElementById('playerModal');
    modal.style.display = 'none';
}

async function loadPlayerDetails(username, isOnline) {
    const basicInfo = document.getElementById('modalBasicInfo');
    const statusInfo = document.getElementById('modalStatus');
    const locationInfo = document.getElementById('modalLocation');
    const achievementsInfo = document.getElementById('modalAchievements');
    const homesInfo = document.getElementById('modalHomes');
    const inventoryInfo = document.getElementById('modalInventory');
    
    // Reset to loading state
    basicInfo.innerHTML = '<div class="detail-row"><span class="detail-label">Loading...</span></div>';
    statusInfo.innerHTML = '<div class="detail-row"><span class="detail-label">Loading...</span></div>';
    locationInfo.innerHTML = '<div class="detail-row"><span class="detail-label">Loading...</span></div>';
    achievementsInfo.innerHTML = '<div class="detail-row"><span class="detail-label">Loading...</span></div>';
    homesInfo.innerHTML = '<p class="empty-state-small">Loading...</p>';
    inventoryInfo.innerHTML = '<p class="empty-state-small">Loading...</p>';
    
    try {
        // Load profile
        const profileResponse = await fetchWithAuth(`${API_BASE_URL}/player/profile/${username}`);
        if (profileResponse.ok) {
            const profile = await profileResponse.json();
            basicInfo.innerHTML = `
                <div class="detail-row">
                    <span class="detail-label">Username</span>
                    <span class="detail-value">${escapeHtml(profile.username || username)}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">UUID</span>
                    <span class="detail-value" style="font-size: 11px;">${profile.uuid || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Game Mode</span>
                    <span class="detail-value">${profile.gameMode || 'N/A'}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Operator</span>
                    <span class="detail-value">${profile.operator ? 'Yes' : 'No'}</span>
                </div>
            `;
        }
        
        // Load status (only for online players)
        if (isOnline) {
            const statusResponse = await fetchWithAuth(`${API_BASE_URL}/player/status/${username}`);
            if (statusResponse.ok) {
                const status = await statusResponse.json();
                statusInfo.innerHTML = `
                    <div class="detail-row">
                        <span class="detail-label">Health</span>
                        <span class="detail-value">${status.health !== undefined ? status.health.toFixed(1) : 'N/A'} ❤️</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Food Level</span>
                        <span class="detail-value">${status.foodLevel !== undefined ? status.foodLevel : 'N/A'} 🍖</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Armor</span>
                        <span class="detail-value">${status.armorValue !== undefined ? status.armorValue : 'N/A'} 🛡️</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Experience</span>
                        <span class="detail-value">Level ${status.experienceLevel !== undefined ? status.experienceLevel : 'N/A'}</span>
                    </div>
                `;
            }
            
            // Load location
            const locationResponse = await fetchWithAuth(`${API_BASE_URL}/player/location/${username}`);
            if (locationResponse.ok) {
                const location = await locationResponse.json();
                locationInfo.innerHTML = `
                    <div class="detail-row">
                        <span class="detail-label">World</span>
                        <span class="detail-value">${location.world || 'N/A'}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Position</span>
                        <span class="detail-value">${location.x !== undefined ? `${location.x.toFixed(0)}, ${location.y.toFixed(0)}, ${location.z.toFixed(0)}` : 'N/A'}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Biome</span>
                        <span class="detail-value">${location.biome || 'N/A'}</span>
                    </div>
                `;
            }
        } else {
            statusInfo.innerHTML = '<p class="empty-state-small">Player is offline</p>';
            locationInfo.innerHTML = '<p class="empty-state-small">Player is offline</p>';
        }
        
        // Load achievements
        const achievementsResponse = await fetchWithAuth(`${API_BASE_URL}/player/achievements/${username}`);
        if (achievementsResponse.ok) {
            const achievements = await achievementsResponse.json();
            const total = achievements.total || 0;
            const completed = achievements.completedCount || 0;
            const inProgress = achievements.inProgressCount || 0;
            const percentComplete = total > 0 ? Math.round((completed / total) * 100) : 0;

            achievementsInfo.innerHTML = `
                <div class="detail-row">
                    <span class="detail-label">Total</span>
                    <span class="detail-value">${total}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Completed</span>
                    <span class="detail-value">${completed} 🏆 (${percentComplete}%)</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">In Progress</span>
                    <span class="detail-value">${inProgress} ⏳</span>
                </div>
                ${achievements.error ? `<div class="detail-row"><span class="detail-label error">${achievements.error}</span></div>` : ''}
            `;
        } else {
            achievementsInfo.innerHTML = '<p class="empty-state-small">Could not load achievements</p>';
        }
        
        // Load homes
        const homesResponse = await fetchWithAuth(`${API_BASE_URL}/player/homes/${username}`);
        if (homesResponse.ok) {
            const homes = await homesResponse.json();
            if (homes.homes && homes.homes.length > 0) {
                homesInfo.innerHTML = homes.homes.map(home => `
                    <div class="home-item">
                        <div class="home-name">🏠 ${escapeHtml(home.name)}</div>
                        <div class="home-location">${home.world || 'Unknown'}</div>
                        <div class="home-location">${home.x !== undefined ? `${home.x.toFixed(0)}, ${home.y.toFixed(0)}, ${home.z.toFixed(0)}` : 'Unknown'}</div>
                    </div>
                `).join('');
            } else {
                homesInfo.innerHTML = '<p class="empty-state-small">No homes set</p>';
            }
        }

        // Inventory viewing has been removed from the dashboard
        // Players can check their inventory in-game
        inventoryInfo.innerHTML = '<p class="empty-state-small">Inventory viewing removed - check in-game</p>';

    } catch (error) {
        console.error('Error loading player details:', error);
        basicInfo.innerHTML = '<p class="empty-state-small">Error loading data</p>';
    }
}

// ===== INVENTORY AND TEXTURE CODE REMOVED =====
// Inventory viewing has been removed from the dashboard.
// Players can check their inventory in-game.

/**
 * Helper function to convert Minecraft color codes to HTML
 * Supports both § and & color codes
 */
function convertMinecraftColors(text) {
    if (!text) return '';

    // Minecraft color codes map
    const colorMap = {
        '0': '#000000', // Black
        '1': '#0000AA', // Dark Blue
        '2': '#00AA00', // Dark Green
        '3': '#00AAAA', // Dark Aqua
        '4': '#AA0000', // Dark Red
        '5': '#AA00AA', // Dark Purple
        '6': '#FFAA00', // Gold
        '7': '#AAAAAA', // Gray
        '8': '#555555', // Dark Gray
        '9': '#5555FF', // Blue
        'a': '#55FF55', // Green
        'b': '#55FFFF', // Aqua
        'c': '#FF5555', // Red
        'd': '#FF55FF', // Light Purple
        'e': '#FFFF55', // Yellow
        'f': '#FFFFFF', // White
        'k': 'obfuscated',  // Obfuscated
        'l': 'bold',        // Bold
        'm': 'strikethrough', // Strikethrough
        'n': 'underline',   // Underline
        'o': 'italic',      // Italic
        'r': 'reset'        // Reset
    };

    let html = '';
    let currentColor = null;
    let currentFormats = [];

    // Replace both § and & color codes
    text = text.replace(/&([0-9a-fk-or])/gi, '§$1');

    // Split by color codes
    const parts = text.split(/(§[0-9a-fk-or])/i);

    for (let part of parts) {
        if (part.match(/^§[0-9a-fk-or]$/i)) {
            // This is a color code
            const code = part.charAt(1).toLowerCase();
            const value = colorMap[code];

            if (value === 'reset') {
                // Close any open tags
                if (currentFormats.length > 0) {
                    html += '</span>';
                }
                currentColor = null;
                currentFormats = [];
            } else if (value === 'obfuscated') {
                // Special handling for obfuscated text (animated)
                currentFormats.push('obfuscated');
            } else if (['bold', 'strikethrough', 'underline', 'italic'].includes(value)) {
                currentFormats.push(value);
            } else {
                // It's a color
                if (currentColor || currentFormats.length > 0) {
                    html += '</span>';
                }
                currentColor = value;
                let styles = `color: ${value};`;
                let classes = '';

                if (currentFormats.includes('bold')) styles += ' font-weight: bold;';
                if (currentFormats.includes('italic')) styles += ' font-style: italic;';
                if (currentFormats.includes('underline')) styles += ' text-decoration: underline;';
                if (currentFormats.includes('strikethrough')) styles += ' text-decoration: line-through;';
                if (currentFormats.includes('obfuscated')) classes = 'class="obfuscated"';

                html += `<span style="${styles}" ${classes}>`;
            }
        } else if (part) {
            // Regular text
            if (!currentColor && currentFormats.length > 0) {
                let styles = '';
                let classes = '';
                if (currentFormats.includes('bold')) styles += ' font-weight: bold;';
                if (currentFormats.includes('italic')) styles += ' font-style: italic;';
                if (currentFormats.includes('underline')) styles += ' text-decoration: underline;';
                if (currentFormats.includes('strikethrough')) styles += ' text-decoration: line-through;';
                if (currentFormats.includes('obfuscated')) classes = 'class="obfuscated"';
                html += `<span style="${styles}" ${classes}>`;
                currentColor = 'formatting';
            }
            html += part;
        }
    }
    
    // Close any remaining open tags
    if (currentColor || currentFormats.length > 0) {
        html += '</span>';
    }

    return html;
}

// Close modal on escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        window.closePlayerModal();
    }
});

// ========================================
// Admin Control Functions
// ========================================

function addAdminLog(message, type = 'info') {
    const logMessages = document.getElementById('adminLogMessages');
    if (!logMessages) return;

    // Remove empty state if present
    const emptyState = logMessages.querySelector('.admin-log-empty');
    if (emptyState) {
        emptyState.remove();
    }

    const timestamp = new Date().toLocaleTimeString();
    const logEntry = document.createElement('div');
    logEntry.className = `admin-log-message ${type}`;
    logEntry.innerHTML = `
        <div class="admin-log-time">${timestamp}</div>
        <div class="admin-log-text">${escapeHtml(message)}</div>
    `;

    logMessages.insertBefore(logEntry, logMessages.firstChild);

    // Keep only last 10 log entries
    while (logMessages.children.length > 10) {
        logMessages.removeChild(logMessages.lastChild);
    }
}

async function handleRestartServer() {
    if (!confirm('⚠️ WARNING: This will restart the Minecraft server!\n\nPlayers will be disconnected in 5 seconds.\n\nAre you sure?')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/admin/restart`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        if (data.success) {
            addAdminLog('Server restart initiated - restarting in 5 seconds...', 'success');
            alert('✅ Server restart initiated!\n\nServer will restart in 5 seconds.\nYou may lose connection to this dashboard.');
        } else {
            addAdminLog(`Restart failed: ${data.error || 'Unknown error'}`, 'error');
            alert('❌ Failed to restart server: ' + (data.error || 'Unknown error'));
        }
    } catch (error) {
        console.error('Error restarting server:', error);
        addAdminLog(`Restart request failed: ${error.message}`, 'error');
        alert('❌ Error: ' + error.message);
    }
}

async function handleStopServer() {
    if (!confirm('🛑 CRITICAL WARNING: This will STOP the Minecraft server!\n\nThe server will shutdown completely.\nPlayers will be disconnected in 5 seconds.\n\nYou will need server console access to restart it.\n\nAre you absolutely sure?')) {
        return;
    }

    // Double confirmation for stop
    if (!confirm('🛑 FINAL CONFIRMATION:\n\nStopping the server will require manual restart from server console or control panel.\n\nProceed with shutdown?')) {
        return;
    }

    try {
        const response = await fetchWithAuth(`${API_BASE_URL}/admin/stop`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        if (data.success) {
            addAdminLog('Server shutdown initiated - stopping in 5 seconds...', 'warning');
            alert('⚠️ Server shutdown initiated!\n\nServer will stop in 5 seconds.\nYou will lose connection to this dashboard.');
        } else {
            addAdminLog(`Shutdown failed: ${data.error || 'Unknown error'}`, 'error');
            alert('❌ Failed to stop server: ' + (data.error || 'Unknown error'));
        }
    } catch (error) {
        console.error('Error stopping server:', error);
        addAdminLog(`Stop request failed: ${error.message}`, 'error');
        alert('❌ Error: ' + error.message);
    }
}

async function handleReloadConfigs() {
    if (!confirm('🔃 Reload all configurations?\n\nThis will reload:\n- Config files\n- Translations\n- Permissions\n\nServer will NOT restart.\n\nContinue?')) {
        return;
    }

    try {
        const btn = document.getElementById('reloadConfigsBtn');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span>⏳</span> Reloading...';
        }

        const response = await fetchWithAuth(`${API_BASE_URL}/admin/reload`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<span>🔃</span> Reload Configs';
        }

        if (data.success) {
            const successCount = data.successCount || 0;
            const totalCount = data.totalCount || 0;
            const failedCount = totalCount - successCount;

            addAdminLog(`Configs reloaded: ${successCount}/${totalCount} successful${failedCount > 0 ? `, ${failedCount} failed` : ''}`, failedCount > 0 ? 'warning' : 'success');

            let message = `✅ Configuration reload complete!\n\n${successCount}/${totalCount} configs reloaded successfully.`;
            if (failedCount > 0) {
                message += `\n\n⚠️ ${failedCount} config(s) failed to reload.`;
            }
            alert(message);
        } else {
            addAdminLog(`Reload failed: ${data.error || 'Unknown error'}`, 'error');
            alert('❌ Failed to reload configs: ' + (data.error || 'Unknown error'));
        }
    } catch (error) {
        console.error('Error reloading configs:', error);
        addAdminLog(`Reload request failed: ${error.message}`, 'error');

        const btn = document.getElementById('reloadConfigsBtn');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<span>🔃</span> Reload Configs';
        }

        alert('❌ Error: ' + error.message);
    }
}

async function handleSaveWorlds() {
    try {
        const btn = document.getElementById('saveWorldsBtn');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span>⏳</span> Saving...';
        }

        const response = await fetchWithAuth(`${API_BASE_URL}/admin/save`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<span>💾</span> Save Worlds';
        }

        if (data.success) {
            addAdminLog('All worlds saved successfully', 'success');
            alert('✅ All worlds saved!\n\nAll loaded chunks have been saved to disk.');
        } else {
            addAdminLog(`Save failed: ${data.error || 'Unknown error'}`, 'error');
            alert('❌ Failed to save worlds: ' + (data.error || 'Unknown error'));
        }
    } catch (error) {
        console.error('Error saving worlds:', error);
        addAdminLog(`Save request failed: ${error.message}`, 'error');

        const btn = document.getElementById('saveWorldsBtn');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<span>💾</span> Save Worlds';
        }

        alert('❌ Error: ' + error.message);
    }
}

console.log('NeoEssentials Dashboard v2.1 loaded successfully - Build 417');
console.log('Auto-refresh interval: ' + (REFRESH_INTERVAL / 1000) + ' seconds');
console.log('Press Ctrl+R or F5 to manually refresh data');
console.log('Player modal functions loaded:', typeof window.openPlayerModal === 'function', typeof window.closePlayerModal === 'function');
console.log('Admin control functions loaded:', typeof handleRestartServer === 'function');
