/**
 * NeoEssentials Dashboard - Enhanced JavaScript Functionality
 * Real-time server monitoring and management interface
 */

class NeoEssentialsDashboard {
    constructor() {
        this.websocket = null;
        this.updateInterval = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.currentTheme = 'neo';
        this.autoRefreshEnabled = true;
        this.refreshRate = 30000; // 30 seconds
        
        this.init();
    }
    
    init() {
        console.log('Initializing NeoEssentials Dashboard...');
        
        // Load saved settings
        this.loadSettings();
        
        // Apply theme
        this.applyTheme(this.currentTheme);
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Initialize WebSocket connection
        this.connectWebSocket();
        
        // Start auto-refresh if enabled
        if (this.autoRefreshEnabled) {
            this.startAutoRefresh();
        }
        
        // Initial data load
        this.refreshData();
        
        console.log('Dashboard initialization complete');
    }
    
    loadSettings() {
        const saved = localStorage.getItem('neoessentials-dashboard-settings');
        if (saved) {
            try {
                const settings = JSON.parse(saved);
                this.currentTheme = settings.theme || 'neo';
                this.autoRefreshEnabled = settings.autoRefresh !== false;
                this.refreshRate = settings.refreshRate || 30000;
            } catch (e) {
                console.warn('Failed to load saved settings:', e);
            }
        }
    }
    
    saveSettings() {
        const settings = {
            theme: this.currentTheme,
            autoRefresh: this.autoRefreshEnabled,
            refreshRate: this.refreshRate
        };
        localStorage.setItem('neoessentials-dashboard-settings', JSON.stringify(settings));
    }
    
    setupEventListeners() {
        // Refresh button
        const refreshBtn = document.getElementById('refresh-btn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.refreshData());
        }
        
        // Theme selector
        const themeSelector = document.getElementById('theme-selector');
        if (themeSelector) {
            themeSelector.addEventListener('change', (e) => {
                this.setTheme(e.target.value);
            });
        }
        
        // Auto-refresh toggle
        const autoRefreshToggle = document.getElementById('auto-refresh-toggle');
        if (autoRefreshToggle) {
            autoRefreshToggle.checked = this.autoRefreshEnabled;
            autoRefreshToggle.addEventListener('change', (e) => {
                this.setAutoRefresh(e.target.checked);
            });
        }
        
        // Window focus/blur for performance optimization
        window.addEventListener('focus', () => {
            if (this.autoRefreshEnabled && !this.updateInterval) {
                this.startAutoRefresh();
            }
        });
        
        window.addEventListener('blur', () => {
            // Optionally pause updates when window is not focused
            // this.stopAutoRefresh();
        });
        
        // Cleanup on page unload
        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });
    }
    
    connectWebSocket() {
        try {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = `${protocol}//${window.location.host}/ws`;
            
            this.websocket = new WebSocket(wsUrl);
            
            this.websocket.onopen = () => {
                console.log('WebSocket connected');
                this.reconnectAttempts = 0;
                this.showNotification('Connected to server', 'success');
            };
            
            this.websocket.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    this.handleWebSocketMessage(data);
                } catch (e) {
                    console.error('Failed to parse WebSocket message:', e);
                }
            };
            
            this.websocket.onclose = () => {
                console.log('WebSocket disconnected');
                this.showNotification('Disconnected from server', 'warning');
                this.scheduleReconnect();
            };
            
            this.websocket.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.showNotification('Connection error', 'error');
            };
            
        } catch (e) {
            console.error('Failed to create WebSocket connection:', e);
        }
    }
    
    scheduleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
            
            console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);
            
            setTimeout(() => {
                this.connectWebSocket();
            }, delay);
        } else {
            console.error('Max reconnection attempts reached');
            this.showNotification('Failed to reconnect to server', 'error');
        }
    }
    
    handleWebSocketMessage(data) {
        switch (data.type) {
            case 'stats_update':
                this.updateStats(data.payload);
                break;
            case 'event':
                this.addEvent(data.payload.type, data.payload.message, data.payload.timestamp);
                break;
            case 'player_join':
                this.handlePlayerJoin(data.payload);
                break;
            case 'player_leave':
                this.handlePlayerLeave(data.payload);
                break;
            case 'alert':
                this.showAlert(data.payload);
                break;
            default:
                console.log('Unknown message type:', data.type);
        }
    }
    
    async refreshData() {
        try {
            this.showLoadingState(true);
            
            // Fetch server data
            const response = await fetch('/api/dashboard/data');
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            this.updateStats(data);
            
            // Update events
            if (data.events) {
                this.updateEventList(data.events);
            }
            
            // Update player list
            if (data.players) {
                this.updatePlayerList(data.players);
            }
            
            this.showLoadingState(false);
            this.updateLastRefreshTime();
            
        } catch (error) {
            console.error('Failed to refresh data:', error);
            this.showNotification('Failed to refresh data: ' + error.message, 'error');
            this.showLoadingState(false);
            
            // Simulate data if server is not available
            this.simulateData();
        }
    }
    
    simulateData() {
        console.log('Simulating dashboard data...');
        
        const mockData = {
            server_status: 'online',
            player_count: Math.floor(Math.random() * 50),
            tps: 18 + Math.random() * 2,
            memory_used: 2000000000 + Math.random() * 1000000000,
            memory_total: 4000000000,
            cpu_usage: 15 + Math.random() * 30,
            disk_usage: 40 + Math.random() * 20,
            uptime: Math.floor(Date.now() / 1000) - Math.floor(Math.random() * 86400),
            economy: {
                total_shops: 150 + Math.floor(Math.random() * 50),
                active_shops: 120 + Math.floor(Math.random() * 30),
                daily_transactions: 200 + Math.floor(Math.random() * 100),
                daily_revenue: 1500 + Math.random() * 500
            }
        };
        
        this.updateStats(mockData);
        
        // Add random events
        const events = [
            { type: 'INFO', message: 'Player joined the server', timestamp: new Date().toISOString() },
            { type: 'SUCCESS', message: 'Shop transaction completed', timestamp: new Date().toISOString() },
            { type: 'WARNING', message: 'High memory usage detected', timestamp: new Date().toISOString() }
        ];
        
        events.forEach(event => {
            this.addEvent(event.type, event.message, event.timestamp);
        });
    }
    
    updateStats(data) {
        // Server status
        this.updateElement('server-status', data.server_status || 'Unknown');
        
        // Player count
        this.updateElement('player-count', data.player_count || 0);
        
        // TPS
        if (data.tps !== undefined) {
            this.updateElement('server-tps', data.tps.toFixed(1));
            this.updateElement('tps-value', data.tps.toFixed(1));
            this.updatePerformanceBar('tps-bar', data.tps, 20);
        }
        
        // Memory
        if (data.memory_used !== undefined && data.memory_total !== undefined) {
            const percentage = (data.memory_used / data.memory_total) * 100;
            this.updateElement('memory-usage', percentage.toFixed(1) + '%');
            this.updateElement('memory-value', 
                `${this.formatBytes(data.memory_used)} / ${this.formatBytes(data.memory_total)}`);
            this.updatePerformanceBar('memory-bar', percentage, 100);
        }
        
        // CPU
        if (data.cpu_usage !== undefined) {
            this.updateElement('cpu-value', data.cpu_usage.toFixed(1) + '%');
            this.updatePerformanceBar('cpu-bar', data.cpu_usage, 100);
        }
        
        // Disk
        if (data.disk_usage !== undefined) {
            this.updateElement('disk-value', data.disk_usage.toFixed(1) + '%');
            this.updatePerformanceBar('disk-bar', data.disk_usage, 100);
        }
        
        // Economy
        if (data.economy) {
            this.updateElement('total-shops', this.formatNumber(data.economy.total_shops || 0));
            this.updateElement('active-shops', this.formatNumber(data.economy.active_shops || 0));
            this.updateElement('daily-transactions', this.formatNumber(data.economy.daily_transactions || 0));
            this.updateElement('daily-revenue', '$' + (data.economy.daily_revenue || 0).toFixed(2));
        }
    }
    
    updateElement(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }
    
    updatePerformanceBar(id, value, max) {
        const percentage = Math.min((value / max) * 100, 100);
        const bar = document.getElementById(id);
        if (bar) {
            bar.style.width = percentage + '%';
            
            // Update TPS bar color based on performance
            if (id === 'tps-bar') {
                bar.className = 'meter-fill ' + 
                    (percentage >= 90 ? 'tps-good' : 
                     percentage >= 70 ? 'tps-warning' : 'tps-critical');
            }
        }
    }
    
    addEvent(type, message, timestamp) {
        const eventList = document.getElementById('event-list');
        if (!eventList) return;
        
        // Remove loading message if present
        const loadingEl = eventList.querySelector('.loading');
        if (loadingEl) {
            loadingEl.remove();
        }
        
        const eventItem = document.createElement('div');
        eventItem.className = 'event-item';
        
        const time = timestamp ? new Date(timestamp).toLocaleTimeString() : new Date().toLocaleTimeString();
        
        eventItem.innerHTML = `
            <div class="event-time">${time}</div>
            <div class="event-message">${this.escapeHtml(message)}</div>
            <span class="event-type ${type.toLowerCase()}">${type}</span>
        `;
        
        eventList.insertBefore(eventItem, eventList.firstChild);
        
        // Keep only last 20 events
        while (eventList.children.length > 20) {
            eventList.removeChild(eventList.lastChild);
        }
        
        // Animate new event
        eventItem.style.opacity = '0';
        eventItem.style.transform = 'translateY(-10px)';
        
        requestAnimationFrame(() => {
            eventItem.style.transition = 'all 0.3s ease';
            eventItem.style.opacity = '1';
            eventItem.style.transform = 'translateY(0)';
        });
    }
    
    updateEventList(events) {
        const eventList = document.getElementById('event-list');
        if (!eventList || !Array.isArray(events)) return;
        
        eventList.innerHTML = '';
        
        events.forEach(event => {
            this.addEvent(event.type, event.message, event.timestamp);
        });
    }
    
    updatePlayerList(players) {
        const playerList = document.getElementById('player-list');
        if (!playerList || !Array.isArray(players)) return;
        
        playerList.innerHTML = '';
        
        if (players.length === 0) {
            playerList.innerHTML = '<div class="loading">No players online</div>';
            return;
        }
        
        players.forEach(player => {
            this.addPlayer(player.name, player.status);
        });
    }
    
    addPlayer(name, status = 'Online') {
        const playerList = document.getElementById('player-list');
        if (!playerList) return;
        
        const loadingEl = playerList.querySelector('.loading');
        if (loadingEl) {
            loadingEl.remove();
        }
        
        const playerItem = document.createElement('div');
        playerItem.className = 'player-item';
        playerItem.innerHTML = `
            <div class="player-avatar">${this.escapeHtml(name.charAt(0).toUpperCase())}</div>
            <div class="player-info">
                <div class="player-name">${this.escapeHtml(name)}</div>
                <div class="player-status">${this.escapeHtml(status)}</div>
            </div>
        `;
        
        playerList.appendChild(playerItem);
    }
    
    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 8px;
            color: white;
            font-weight: 500;
            z-index: 1000;
            opacity: 0;
            transform: translateX(100%);
            transition: all 0.3s ease;
            max-width: 300px;
            word-wrap: break-word;
        `;
        
        // Set background color based on type
        switch (type) {
            case 'success':
                notification.style.backgroundColor = '#4caf50';
                break;
            case 'warning':
                notification.style.backgroundColor = '#ff9800';
                break;
            case 'error':
                notification.style.backgroundColor = '#f44336';
                break;
            default:
                notification.style.backgroundColor = '#2196f3';
        }
        
        document.body.appendChild(notification);
        
        // Animate in
        requestAnimationFrame(() => {
            notification.style.opacity = '1';
            notification.style.transform = 'translateX(0)';
        });
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transform = 'translateX(100%)';
            
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 5000);
    }
    
    showAlert(alert) {
        this.showNotification(alert.message, alert.severity.toLowerCase());
        this.addEvent(alert.severity, alert.message, alert.timestamp);
    }
    
    applyTheme(theme) {
        document.body.className = `theme-${theme}`;
        this.currentTheme = theme;
        this.saveSettings();
    }
    
    setTheme(theme) {
        this.applyTheme(theme);
        this.showNotification(`Theme changed to ${theme}`, 'success');
    }
    
    setAutoRefresh(enabled) {
        this.autoRefreshEnabled = enabled;
        
        if (enabled) {
            this.startAutoRefresh();
            this.showNotification('Auto-refresh enabled', 'success');
        } else {
            this.stopAutoRefresh();
            this.showNotification('Auto-refresh disabled', 'info');
        }
        
        this.saveSettings();
    }
    
    startAutoRefresh() {
        this.stopAutoRefresh(); // Clear existing interval
        
        this.updateInterval = setInterval(() => {
            this.refreshData();
        }, this.refreshRate);
        
        console.log(`Auto-refresh started (${this.refreshRate}ms interval)`);
    }
    
    stopAutoRefresh() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
            console.log('Auto-refresh stopped');
        }
    }
    
    showLoadingState(show) {
        const refreshBtn = document.getElementById('refresh-btn');
        if (refreshBtn) {
            refreshBtn.disabled = show;
            refreshBtn.textContent = show ? 'Refreshing...' : 'Refresh Dashboard';
        }
    }
    
    updateLastRefreshTime() {
        const timeEl = document.getElementById('last-refresh');
        if (timeEl) {
            timeEl.textContent = new Date().toLocaleTimeString();
        }
    }
    
    cleanup() {
        console.log('Cleaning up dashboard...');
        
        this.stopAutoRefresh();
        
        if (this.websocket) {
            this.websocket.close();
        }
    }
    
    // Utility functions
    formatBytes(bytes) {
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        if (bytes === 0) return '0 Bytes';
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
    }
    
    formatNumber(num) {
        return new Intl.NumberFormat().format(num);
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize dashboard when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.neoEssentialsDashboard = new NeoEssentialsDashboard();
});

// Global functions for backward compatibility
function refreshData() {
    if (window.neoEssentialsDashboard) {
        window.neoEssentialsDashboard.refreshData();
    }
}

function setTheme(theme) {
    if (window.neoEssentialsDashboard) {
        window.neoEssentialsDashboard.setTheme(theme);
    }
}
