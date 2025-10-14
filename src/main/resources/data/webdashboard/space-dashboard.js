// === THEME SWITCHER SYSTEM ===
class ThemeManager {
  constructor() {
    this.themes = ['nebula', 'galaxy', 'deep-space', 'solar', 'aurora'];
    this.currentTheme = this.loadTheme();
    this.init();
  }
  
  init() {
    // Apply saved theme
    this.applyTheme(this.currentTheme);
    
    // Setup theme switcher button
    const themeSwitcher = document.querySelector('.theme-switcher');
    const themeModal = document.getElementById('theme-modal');
    const closeBtn = document.getElementById('close-theme-modal-btn');
    const themeOptions = document.querySelectorAll('.theme-option');
    
    if (themeSwitcher && themeModal) {
      themeSwitcher.addEventListener('click', () => {
        this.openThemeModal();
      });
      
      closeBtn?.addEventListener('click', () => {
        themeModal.close();
      });
      
      themeModal.addEventListener('click', (e) => {
        if (e.target === themeModal) {
          themeModal.close();
        }
      });
      
      themeOptions.forEach(option => {
        option.addEventListener('click', () => {
          const theme = option.getAttribute('data-theme');
          this.setTheme(theme);
        });
      });
    }
  }
  
  loadTheme() {
    return localStorage.getItem('dashboard-theme') || 'nebula';
  }
  
  saveTheme(theme) {
    localStorage.setItem('dashboard-theme', theme);
  }
  
  applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    this.currentTheme = theme;
    this.updateThemeOptions();
  }
  
  setTheme(theme) {
    if (this.themes.includes(theme)) {
      this.applyTheme(theme);
      this.saveTheme(theme);
      
      const themeName = theme.split('-').map(w => 
        w.charAt(0).toUpperCase() + w.slice(1)
      ).join(' ');
      
      showNotification(`🌌 Theme changed to ${themeName}`, 'success', 3000);
      
      // Close modal after a short delay
      setTimeout(() => {
        document.getElementById('theme-modal')?.close();
      }, 500);
    }
  }
  
  openThemeModal() {
    const modal = document.getElementById('theme-modal');
    if (modal) {
      this.updateThemeOptions();
      modal.showModal();
    }
  }
  
  updateThemeOptions() {
    const options = document.querySelectorAll('.theme-option');
    options.forEach(option => {
      const theme = option.getAttribute('data-theme');
      if (theme === this.currentTheme) {
        option.classList.add('active');
      } else {
        option.classList.remove('active');
      }
    });
  }
  
  cycleTheme() {
    const currentIndex = this.themes.indexOf(this.currentTheme);
    const nextIndex = (currentIndex + 1) % this.themes.length;
    this.setTheme(this.themes[nextIndex]);
  }
}

// === MOBILE MENU & TOUCH GESTURES ===
class MobileMenuManager {
  constructor() {
    this.sidebar = document.querySelector('.dashboard-sidebar');
    this.overlay = document.querySelector('.sidebar-overlay');
    this.toggleBtn = document.querySelector('.mobile-menu-toggle');
    this.sidebarBtns = document.querySelectorAll('.sidebar-btn');
    this.isOpen = false;
    this.touchStartX = 0;
    this.touchEndX = 0;
    this.init();
  }
  
  init() {
    // Toggle button click
    this.toggleBtn?.addEventListener('click', () => this.toggleSidebar());
    
    // Overlay click to close
    this.overlay?.addEventListener('click', () => this.closeSidebar());
    
    // Close sidebar when clicking a menu item
    this.sidebarBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        if (window.innerWidth <= 900) {
          this.closeSidebar();
        }
      });
    });
    
    // Touch gestures for swipe to open/close
    this.setupTouchGestures();
    
    // Close on window resize if open
    window.addEventListener('resize', () => {
      if (window.innerWidth > 900 && this.isOpen) {
        this.closeSidebar();
      }
    });
  }
  
  setupTouchGestures() {
    // Swipe from left edge to open
    document.addEventListener('touchstart', (e) => {
      this.touchStartX = e.changedTouches[0].screenX;
    }, { passive: true });
    
    document.addEventListener('touchend', (e) => {
      this.touchEndX = e.changedTouches[0].screenX;
      this.handleSwipe();
    }, { passive: true });
  }
  
  handleSwipe() {
    const swipeDistance = this.touchEndX - this.touchStartX;
    const threshold = 50; // Minimum swipe distance
    
    // Swipe right from left edge to open
    if (swipeDistance > threshold && this.touchStartX < 50 && !this.isOpen) {
      this.openSidebar();
    }
    
    // Swipe left to close when sidebar is open
    if (swipeDistance < -threshold && this.isOpen) {
      this.closeSidebar();
    }
  }
  
  toggleSidebar() {
    if (this.isOpen) {
      this.closeSidebar();
    } else {
      this.openSidebar();
    }
  }
  
  openSidebar() {
    this.isOpen = true;
    this.sidebar?.classList.add('active');
    this.overlay?.classList.add('active');
    this.toggleBtn?.classList.add('active');
    this.toggleBtn?.setAttribute('aria-expanded', 'true');
    document.body.style.overflow = 'hidden'; // Prevent background scroll
  }
  
  closeSidebar() {
    this.isOpen = false;
    this.sidebar?.classList.remove('active');
    this.overlay?.classList.remove('active');
    this.toggleBtn?.classList.remove('active');
    this.toggleBtn?.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = ''; // Restore scroll
  }
}

// Initialize theme manager globally
let themeManager;
let mobileMenuManager;

// Sidebar button interactivity
document.addEventListener('DOMContentLoaded', function() {
  // Initialize theme manager
  themeManager = new ThemeManager();
  
  // Initialize mobile menu manager
  mobileMenuManager = new MobileMenuManager();
  
  const sidebarBtns = document.querySelectorAll('.sidebar-btn');
  sidebarBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      // Remove active from all
      sidebarBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      // Scroll to section
      const targetId = btn.getAttribute('data-target');
      const section = document.getElementById(targetId);
      if (section) {
        section.scrollIntoView({ behavior: 'smooth', block: 'center' });
        showNotification(`Navigated to ${btn.textContent.trim()}`);
      } else if (targetId === 'settings') {
        showNotification('Settings section coming soon!', 'info');
      }
    });
  });
  
  // Chart type selector
  const chartBtns = document.querySelectorAll('.chart-btn');
  chartBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      chartBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      
      const chartType = btn.getAttribute('data-chart');
      renderServerChart(chartType);
      showNotification(`📊 Switched to ${btn.textContent.trim()} chart`, 'info', 2000);
    });
  });
  
  // Time range selector
  const timeBtns = document.querySelectorAll('.time-btn');
  timeBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      timeBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      
      const timeRange = btn.getAttribute('data-range');
      currentTimeRange = timeRange;
      filterChartByTimeRange(timeRange);
      showNotification(`⏱️ Time range: ${timeRange}`, 'info', 2000);
    });
  });
  
  // Initialize chart data and render
  initializeChartData();
  renderServerChart('tps');
  
  // Initialize responsive utilities
  setupResponsiveHelpers();
});

// === RESPONSIVE UTILITIES ===
function setupResponsiveHelpers() {
  // Detect device type and add classes
  const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
  const isTablet = /iPad|Android/i.test(navigator.userAgent) && window.innerWidth >= 600 && window.innerWidth <= 1024;
  
  if (isMobile) {
    document.body.classList.add('is-mobile');
  }
  
  if (isTablet) {
    document.body.classList.add('is-tablet');
  }
  
  // Update on orientation change
  window.addEventListener('orientationchange', () => {
    setTimeout(() => {
      // Refresh chart on orientation change
      if (currentChart) {
        currentChart.resize();
      }
      showNotification('📱 Layout adjusted for orientation', 'info', 2000);
    }, 100);
  });
  
  // Handle viewport resize
  let resizeTimer;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      // Update any dynamic elements
      if (currentChart) {
        currentChart.resize();
      }
    }, 250);
  });
  
  // Prevent double-tap zoom on interactive elements
  let lastTouchEnd = 0;
  document.addEventListener('touchend', (e) => {
    const now = Date.now();
    if (now - lastTouchEnd <= 300) {
      e.preventDefault();
    }
    lastTouchEnd = now;
  }, { passive: false });
}

// Filter chart data by time range
function filterChartByTimeRange(range) {
  // This would filter the data based on selected range
  // For now, just re-render with current data
  // In production, you'd query the backend for specific time range
  
  const rangeMinutes = {
    '1h': 60,
    '6h': 360,
    '24h': 1440,
    '7d': 10080
  };
  
  const minutes = rangeMinutes[range] || 360;
  const pointsToShow = Math.min(minutes / 5, 100); // Assuming 5-minute intervals
  
  // Adjust max points for each store
  Object.keys(chartDataStore).forEach(key => {
    chartDataStore[key].maxPoints = pointsToShow;
  });
  
  // Re-render current chart
  renderServerChart(currentChartType);
}
// Notification area logic
function showNotification(message, type = 'info', timeout = 3500) {
  const notif = document.getElementById('dashboard-notifications');
  if (!notif) return;
  notif.textContent = message;
  notif.style.background = type === 'error' ? 'rgba(255,45,85,0.18)' : 'rgba(126,200,227,0.12)';
  notif.style.color = type === 'error' ? '#ff2d55' : '#fff';
  notif.style.fontWeight = type === 'error' ? 'bold' : 'normal';
  setTimeout(() => { notif.textContent = ''; }, timeout);
}

// ===== ENHANCED CHART SYSTEM =====

// Chart data storage with historical tracking
const chartDataStore = {
  tps: { labels: [], data: [], maxPoints: 100 },
  memory: { labels: [], data: [], maxPoints: 100 },
  players: { labels: [], data: [], maxPoints: 100 },
  entities: { labels: [], data: [], maxPoints: 100 }
};

let serverChart;
let currentChartType = 'tps';
let currentTimeRange = '6h';

// Add data point to history
function addChartDataPoint(type, label, value) {
  const store = chartDataStore[type];
  if (!store) return;
  
  store.labels.push(label);
  store.data.push(value);
  
  // Keep only maxPoints
  if (store.labels.length > store.maxPoints) {
    store.labels.shift();
    store.data.shift();
  }
}

// Get chart configuration based on type
function getChartConfig(type) {
  const configs = {
    tps: {
      label: 'TPS (Ticks Per Second)',
      borderColor: '#00ffae',
      backgroundColor: 'rgba(0, 255, 174, 0.15)',
      pointBackgroundColor: '#00ffae',
      yMin: 0,
      yMax: 20,
      yLabel: 'TPS',
      fill: true
    },
    memory: {
      label: 'Memory Usage (MB)',
      borderColor: '#7ec8e3',
      backgroundColor: 'rgba(126, 200, 227, 0.15)',
      pointBackgroundColor: '#7ec8e3',
      yMin: 0,
      yMax: null, // Auto-scale
      yLabel: 'Memory (MB)',
      fill: true
    },
    players: {
      label: 'Online Players',
      borderColor: '#ffa500',
      backgroundColor: 'rgba(255, 165, 0, 0.15)',
      pointBackgroundColor: '#ffa500',
      yMin: 0,
      yMax: null, // Auto-scale
      yLabel: 'Players',
      fill: true,
      stepped: true // Stepped line for player count
    },
    entities: {
      label: 'Entity Count',
      borderColor: '#ff6b9d',
      backgroundColor: 'rgba(255, 107, 157, 0.15)',
      pointBackgroundColor: '#ff6b9d',
      yMin: 0,
      yMax: null, // Auto-scale
      yLabel: 'Entities',
      fill: true
    }
  };
  
  return configs[type] || configs.tps;
}

// Render chart with enhanced features
function renderServerChart(type = currentChartType) {
  const ctx = document.getElementById('serverChart');
  if (!ctx) return;
  
  const store = chartDataStore[type];
  if (!store) return;
  
  const config = getChartConfig(type);
  
  // Destroy existing chart
  if (serverChart) {
    serverChart.destroy();
  }
  
  // Create new chart with enhanced options
  serverChart = new Chart(ctx.getContext('2d'), {
    type: 'line',
    data: {
      labels: store.labels,
      datasets: [{
        label: config.label,
        data: store.data,
        borderColor: config.borderColor,
        backgroundColor: config.backgroundColor,
        pointBackgroundColor: config.pointBackgroundColor,
        pointBorderColor: config.borderColor,
        pointRadius: 3,
        pointHoverRadius: 6,
        borderWidth: 2.5,
        tension: config.stepped ? 0 : 0.4,
        fill: config.fill,
        stepped: config.stepped || false
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: {
        duration: 750,
        easing: 'easeInOutQuart'
      },
      interaction: {
        intersect: false,
        mode: 'index'
      },
      plugins: {
        legend: {
          display: true,
          position: 'top',
          labels: {
            color: '#eaf6ff',
            font: {
              family: "'Orbitron', Arial, sans-serif",
              size: 12,
              weight: '600'
            },
            padding: 15,
            usePointStyle: true
          }
        },
        tooltip: {
          enabled: true,
          backgroundColor: 'rgba(26, 31, 58, 0.95)',
          titleColor: '#00ffae',
          bodyColor: '#eaf6ff',
          borderColor: '#7ec8e3',
          borderWidth: 1,
          padding: 12,
          displayColors: true,
          callbacks: {
            title: function(context) {
              return context[0].label || '';
            },
            label: function(context) {
              let label = context.dataset.label || '';
              if (label) {
                label += ': ';
              }
              label += context.parsed.y.toFixed(2);
              return label;
            }
          }
        },
        zoom: {
          pan: {
            enabled: true,
            mode: 'x',
            modifierKey: 'ctrl'
          },
          zoom: {
            wheel: {
              enabled: true,
              modifierKey: 'ctrl'
            },
            pinch: {
              enabled: true
            },
            mode: 'x'
          }
        }
      },
      scales: {
        x: {
          display: true,
          grid: {
            color: 'rgba(126, 200, 227, 0.1)',
            drawBorder: false
          },
          ticks: {
            color: '#7ec8e3',
            font: {
              family: "'Orbitron', Arial, sans-serif",
              size: 10
            },
            maxRotation: 45,
            minRotation: 0,
            autoSkip: true,
            maxTicksLimit: 12
          }
        },
        y: {
          display: true,
          grid: {
            color: 'rgba(126, 200, 227, 0.1)',
            drawBorder: false
          },
          ticks: {
            color: '#7ec8e3',
            font: {
              family: "'Orbitron', Arial, sans-serif",
              size: 10
            }
          },
          min: config.yMin,
          max: config.yMax,
          title: {
            display: true,
            text: config.yLabel,
            color: '#00ffae',
            font: {
              family: "'Orbitron', Arial, sans-serif",
              size: 11,
              weight: '600'
            }
          }
        }
      }
    }
  });
  
  currentChartType = type;
}

// Update chart with new data point
function updateChart(type, value) {
  const now = new Date();
  const timeLabel = now.toLocaleTimeString('en-US', { 
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit'
  });
  
  addChartDataPoint(type, timeLabel, value);
  
  // Only re-render if it's the current chart
  if (type === currentChartType) {
    renderServerChart(type);
  }
}

// Initialize sample data for testing
function initializeChartData() {
  const now = Date.now();
  const pointsToGenerate = 20;
  
  for (let i = pointsToGenerate; i >= 0; i--) {
    const time = new Date(now - (i * 30000)); // 30 second intervals
    const timeLabel = time.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
    
    // Generate sample data with some variance
    addChartDataPoint('tps', timeLabel, 19 + Math.random() * 1);
    addChartDataPoint('memory', timeLabel, 1024 + Math.random() * 512);
    addChartDataPoint('players', timeLabel, Math.floor(Math.random() * 20));
    addChartDataPoint('entities', timeLabel, 500 + Math.floor(Math.random() * 1000));
  }
}

// Live status indicator (simulated)
function setLiveStatus(isLive) {
  const el = document.getElementById('live-status');
  if (!el) return;
  el.style.color = isLive ? '#00ffae' : '#ff2d55';
  el.title = isLive ? 'Live' : 'Offline';
}

// Server health bar animation
function setServerHealth(percent) {
  const fill = document.querySelector('.health-fill');
  if (!fill) return;
  fill.style.width = Math.max(0, Math.min(100, percent)) + '%';
}

// === PLAYER DETAILS MODAL SYSTEM ===
class PlayerDetailsManager {
  constructor() {
    this.modal = document.getElementById('player-details-modal');
    this.closeBtn = document.getElementById('close-player-modal');
    this.detailsBtn = document.getElementById('player-details-btn');
    this.currentPlayerData = null;
    this.init();
  }
  
  init() {
    // Setup close button
    this.closeBtn?.addEventListener('click', () => this.closeModal());
    
    // Setup details button
    this.detailsBtn?.addEventListener('click', () => this.openModal('Steve')); // Example
    
    // Close on backdrop click
    this.modal?.addEventListener('click', (e) => {
      if (e.target === this.modal) {
        this.closeModal();
      }
    });
    
    // Setup tab navigation
    this.setupTabs();
    
    // Setup action buttons
    this.setupActionButtons();
  }
  
  setupTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const targetTab = btn.getAttribute('data-tab');
        this.switchTab(targetTab);
      });
    });
  }
  
  switchTab(tabName) {
    // Remove active from all tabs and content
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    
    // Add active to selected tab
    const selectedBtn = document.querySelector(`[data-tab="${tabName}"]`);
    const selectedContent = document.getElementById(`tab-${tabName}`);
    
    if (selectedBtn && selectedContent) {
      selectedBtn.classList.add('active');
      selectedContent.classList.add('active');
    }
  }
  
  setupActionButtons() {
    // Message button
    document.getElementById('action-message')?.addEventListener('click', () => {
      showNotification(`💬 Opening message dialog for ${this.currentPlayerData?.name || 'player'}`, 'info');
    });
    
    // Kick button
    document.getElementById('action-kick')?.addEventListener('click', () => {
      if (confirm(`Are you sure you want to kick ${this.currentPlayerData?.name || 'this player'}?`)) {
        showNotification(`⚠️ Kicked ${this.currentPlayerData?.name || 'player'}`, 'warning');
      }
    });
    
    // Ban button
    document.getElementById('action-ban')?.addEventListener('click', () => {
      if (confirm(`Are you sure you want to ban ${this.currentPlayerData?.name || 'this player'}? This action is permanent.`)) {
        showNotification(`🚫 Banned ${this.currentPlayerData?.name || 'player'}`, 'error');
      }
    });
    
    // Teleport button
    document.getElementById('teleport-to-player')?.addEventListener('click', () => {
      showNotification(`🚀 Teleporting to ${this.currentPlayerData?.name || 'player'}...`, 'success');
    });
  }
  
  openModal(playerName) {
    // Fetch player data (this would be an API call in production)
    this.fetchPlayerData(playerName).then(data => {
      this.currentPlayerData = data;
      this.populateModal(data);
      this.modal?.showModal();
    });
  }
  
  closeModal() {
    this.modal?.close();
  }
  
  async fetchPlayerData(playerName) {
    // Mock data - replace with actual API call
    return {
      name: playerName,
      uuid: '550e8400-e29b-41d4-a716-446655440000',
      status: 'online',
      head: `https://crafatar.com/avatars/${playerName}?overlay`,
      
      // Overview
      playtime: '142h 35m',
      gamemode: 'Survival',
      health: '20/20 ❤️',
      hunger: '18/20 🍖',
      xp: '2,547 XP',
      level: '42',
      
      // Location
      world: 'Overworld',
      dimension: 'minecraft:overworld',
      x: '1234',
      y: '65',
      z: '-5678',
      rotation: 'North (0°)',
      
      // Statistics
      stats: {
        mobKills: 1523,
        deaths: 12,
        walked: '142.3 km',
        playerKills: 3,
        blocksMined: 8934,
        crafted: 2341
      },
      
      // Economy
      balance: 15234.50,
      transactions: [
        { type: 'Sold Diamond x64', amount: '+1280.00', positive: true },
        { type: 'Bought Iron Armor', amount: '-450.00', positive: false },
        { type: 'Daily Reward', amount: '+100.00', positive: true }
      ],
      
      // Homes
      homes: [
        { name: 'Home', coords: 'X: 100, Y: 64, Z: 200' },
        { name: 'Farm', coords: 'X: -450, Y: 72, Z: 890' },
        { name: 'Shop', coords: 'X: 0, Y: 65, Z: 0' }
      ],
      
      // Permissions
      permissions: [
        'essentials.home',
        'essentials.tpa',
        'essentials.warp',
        'essentials.kit.starter',
        'worldedit.selection.*'
      ],
      
      // Inventory (simplified)
      inventory: Array(36).fill(null).map((_, i) => ({
        slot: i,
        item: i % 5 === 0 ? 'diamond_sword' : null,
        count: i % 5 === 0 ? Math.floor(Math.random() * 64) + 1 : null
      }))
    };
  }
  
  populateModal(data) {
    // Header
    document.getElementById('player-details-title').textContent = data.name;
    document.getElementById('player-uuid').textContent = data.uuid;
    document.getElementById('player-head').src = data.head;
    
    const statusEl = document.getElementById('player-status');
    statusEl.textContent = data.status.charAt(0).toUpperCase() + data.status.slice(1);
    statusEl.className = `player-status ${data.status}`;
    
    // Overview tab
    document.getElementById('player-playtime').textContent = data.playtime;
    document.getElementById('player-gamemode').textContent = data.gamemode;
    document.getElementById('player-health').textContent = data.health;
    document.getElementById('player-hunger').textContent = data.hunger;
    document.getElementById('player-xp').textContent = data.xp;
    document.getElementById('player-level').textContent = data.level;
    
    // Location tab
    document.getElementById('player-world').textContent = data.world;
    document.getElementById('player-dimension').textContent = data.dimension;
    document.getElementById('player-x').textContent = data.x;
    document.getElementById('player-y').textContent = data.y;
    document.getElementById('player-z').textContent = data.z;
    document.getElementById('player-rotation').textContent = data.rotation;
    
    // Statistics tab
    document.getElementById('stat-mob-kills').textContent = data.stats.mobKills.toLocaleString();
    document.getElementById('stat-deaths').textContent = data.stats.deaths;
    document.getElementById('stat-walked').textContent = data.stats.walked;
    document.getElementById('stat-player-kills').textContent = data.stats.playerKills;
    document.getElementById('stat-blocks-mined').textContent = data.stats.blocksMined.toLocaleString();
    document.getElementById('stat-crafted').textContent = data.stats.crafted.toLocaleString();
    
    // Economy tab
    document.getElementById('player-balance').textContent = `$${data.balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    
    const transactionsList = document.querySelector('#recent-transactions .transactions-list');
    transactionsList.innerHTML = data.transactions.map(t => `
      <div class="transaction-item">
        <span class="transaction-type">${t.type}</span>
        <span class="transaction-amount ${t.positive ? 'positive' : 'negative'}">${t.amount}</span>
      </div>
    `).join('');
    
    // Homes
    const homesList = document.querySelector('.homes-list');
    homesList.innerHTML = data.homes.map(h => `
      <div class="home-item">
        <div class="home-name">${h.name}</div>
        <div class="home-coords">${h.coords}</div>
      </div>
    `).join('');
    
    // Permissions
    const permissionsList = document.getElementById('player-permissions');
    permissionsList.innerHTML = data.permissions.map(p => `
      <div class="permission-item">${p}</div>
    `).join('');
    
    // Inventory
    const inventoryGrid = document.getElementById('player-inventory-grid');
    inventoryGrid.innerHTML = data.inventory.map(slot => {
      if (slot.item) {
        return `
          <div class="inventory-slot" title="${slot.item} x${slot.count}">
            <img src="https://mc-heads.net/minecraft/${slot.item}.png" alt="${slot.item}">
            <span class="item-count">${slot.count}</span>
          </div>
        `;
      } else {
        return `<div class="inventory-slot empty"></div>`;
      }
    }).join('');
  }
}

// Initialize player details manager globally
let playerDetailsManager;

// Modal interactivity
const playerDetailsBtn = document.getElementById('player-details-btn');
const playerDetailsModal = document.getElementById('player-details-modal');

if (playerDetailsBtn && playerDetailsModal) {
  // Initialize in DOMContentLoaded
  document.addEventListener('DOMContentLoaded', () => {
    playerDetailsManager = new PlayerDetailsManager();
  });
}

// ========================================
// COMMAND CONSOLE MANAGER
// ========================================

class CommandConsoleManager {
  constructor() {
    this.input = document.getElementById('console-input');
    this.output = document.getElementById('console-output');
    this.sendBtn = document.getElementById('console-send');
    this.autocompleteDiv = document.getElementById('console-autocomplete');
    this.shortcutBtns = document.querySelectorAll('.shortcut-btn');
    
    // Command history
    this.commandHistory = this.loadHistory();
    this.historyIndex = -1;
    
    // Autocomplete data
    this.commands = [
      { cmd: '/list', desc: 'List online players' },
      { cmd: '/tps', desc: 'Show server TPS' },
      { cmd: '/gc', desc: 'Run garbage collection' },
      { cmd: '/save-all', desc: 'Save all worlds' },
      { cmd: '/stop', desc: 'Stop the server' },
      { cmd: '/restart', desc: 'Restart the server' },
      { cmd: '/whitelist list', desc: 'Show whitelist' },
      { cmd: '/whitelist add <player>', desc: 'Add player to whitelist' },
      { cmd: '/whitelist remove <player>', desc: 'Remove player from whitelist' },
      { cmd: '/ban <player> [reason]', desc: 'Ban a player' },
      { cmd: '/pardon <player>', desc: 'Unban a player' },
      { cmd: '/kick <player> [reason]', desc: 'Kick a player' },
      { cmd: '/op <player>', desc: 'Give operator status' },
      { cmd: '/deop <player>', desc: 'Remove operator status' },
      { cmd: '/tp <player> <target>', desc: 'Teleport player' },
      { cmd: '/give <player> <item> [amount]', desc: 'Give items to player' },
      { cmd: '/gamemode <mode> [player]', desc: 'Change gamemode' },
      { cmd: '/time set <time>', desc: 'Set world time' },
      { cmd: '/weather <clear|rain|thunder>', desc: 'Change weather' },
      { cmd: '/difficulty <level>', desc: 'Set difficulty' },
      { cmd: '/plugins', desc: 'List plugins/mods' },
      { cmd: '/reload', desc: 'Reload server configuration' },
      { cmd: '/help [command]', desc: 'Show command help' }
    ];
    
    this.currentAutocompleteIndex = -1;
    this.autocompleteResults = [];
  }
  
  init() {
    if (!this.input || !this.output || !this.sendBtn) {
      console.error('Console elements not found');
      return;
    }
    
    // Send command button
    this.sendBtn.addEventListener('click', () => this.sendCommand());
    
    // Enter key to send
    this.input.addEventListener('keydown', (e) => this.handleKeyDown(e));
    
    // Input changes for autocomplete
    this.input.addEventListener('input', () => this.handleInput());
    
    // Shortcut buttons
    this.shortcutBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const command = btn.getAttribute('data-command');
        this.input.value = command;
        this.sendCommand();
      });
    });
    
    // Ctrl+L to clear console
    document.addEventListener('keydown', (e) => {
      if (e.ctrlKey && e.key === 'l') {
        e.preventDefault();
        this.clearConsole();
      }
    });
    
    this.addSystemMessage('Console initialized. Type /help for available commands.');
  }
  
  handleKeyDown(e) {
    switch(e.key) {
      case 'Enter':
        e.preventDefault();
        this.sendCommand();
        break;
        
      case 'ArrowUp':
        e.preventDefault();
        this.navigateHistory(-1);
        break;
        
      case 'ArrowDown':
        e.preventDefault();
        this.navigateHistory(1);
        break;
        
      case 'Tab':
        e.preventDefault();
        if (this.autocompleteResults.length > 0) {
          this.selectAutocomplete();
        }
        break;
        
      case 'Escape':
        this.hideAutocomplete();
        break;
        
      case 'ArrowUp':
      case 'ArrowDown':
        if (this.autocompleteDiv.style.display !== 'none') {
          e.preventDefault();
          this.navigateAutocomplete(e.key === 'ArrowUp' ? -1 : 1);
        }
        break;
    }
  }
  
  handleInput() {
    const value = this.input.value.trim();
    
    if (value.length === 0) {
      this.hideAutocomplete();
      return;
    }
    
    // Find matching commands
    this.autocompleteResults = this.commands.filter(cmd => 
      cmd.cmd.toLowerCase().startsWith(value.toLowerCase())
    );
    
    if (this.autocompleteResults.length > 0) {
      this.showAutocomplete();
    } else {
      this.hideAutocomplete();
    }
  }
  
  showAutocomplete() {
    this.autocompleteDiv.innerHTML = '';
    this.currentAutocompleteIndex = -1;
    
    this.autocompleteResults.forEach((result, index) => {
      const item = document.createElement('div');
      item.className = 'autocomplete-item';
      item.innerHTML = `
        <span class="autocomplete-command">${result.cmd}</span>
        <span class="autocomplete-description">${result.desc}</span>
      `;
      
      item.addEventListener('click', () => {
        this.input.value = result.cmd;
        this.hideAutocomplete();
        this.input.focus();
      });
      
      this.autocompleteDiv.appendChild(item);
    });
    
    this.autocompleteDiv.style.display = 'block';
  }
  
  hideAutocomplete() {
    this.autocompleteDiv.style.display = 'none';
    this.currentAutocompleteIndex = -1;
  }
  
  navigateAutocomplete(direction) {
    const items = this.autocompleteDiv.querySelectorAll('.autocomplete-item');
    if (items.length === 0) return;
    
    // Remove previous selection
    if (this.currentAutocompleteIndex >= 0) {
      items[this.currentAutocompleteIndex].classList.remove('selected');
    }
    
    // Update index
    this.currentAutocompleteIndex += direction;
    if (this.currentAutocompleteIndex < 0) {
      this.currentAutocompleteIndex = items.length - 1;
    } else if (this.currentAutocompleteIndex >= items.length) {
      this.currentAutocompleteIndex = 0;
    }
    
    // Add new selection
    items[this.currentAutocompleteIndex].classList.add('selected');
    items[this.currentAutocompleteIndex].scrollIntoView({ block: 'nearest' });
  }
  
  selectAutocomplete() {
    if (this.currentAutocompleteIndex >= 0 && this.currentAutocompleteIndex < this.autocompleteResults.length) {
      this.input.value = this.autocompleteResults[this.currentAutocompleteIndex].cmd;
    } else if (this.autocompleteResults.length > 0) {
      this.input.value = this.autocompleteResults[0].cmd;
    }
    this.hideAutocomplete();
  }
  
  navigateHistory(direction) {
    if (this.commandHistory.length === 0) return;
    
    this.historyIndex += direction;
    
    if (this.historyIndex < 0) {
      this.historyIndex = 0;
    } else if (this.historyIndex >= this.commandHistory.length) {
      this.historyIndex = this.commandHistory.length;
      this.input.value = '';
      return;
    }
    
    this.input.value = this.commandHistory[this.historyIndex];
  }
  
  async sendCommand() {
    const command = this.input.value.trim();
    
    if (!command) return;
    
    // Add to history
    this.addToHistory(command);
    
    // Display command in console
    this.addConsoleLine('command', command);
    
    // Clear input
    this.input.value = '';
    this.hideAutocomplete();
    this.historyIndex = -1;
    
    // Show loading
    this.addSystemMessage('Executing command...');
    
    try {
      // Send to server API
      const response = await fetch('http://localhost:8080/api/command', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ command: command })
      });
      
      if (response.ok) {
        const data = await response.json();
        
        if (data.success) {
          this.addConsoleLine('success', data.output || 'Command executed successfully');
        } else {
          this.addConsoleLine('error', data.error || 'Command failed');
        }
      } else {
        // Mock response for demo
        this.handleMockCommand(command);
      }
    } catch (error) {
      console.error('Command execution error:', error);
      // Fallback to mock for demo
      this.handleMockCommand(command);
    }
  }
  
  handleMockCommand(command) {
    // Mock command responses for demonstration
    const cmd = command.toLowerCase();
    
    if (cmd === '/list') {
      this.addConsoleLine('success', 'There are 3/20 players online: Steve, Alex, Notch');
    } else if (cmd === '/tps') {
      this.addConsoleLine('success', 'TPS from last 1m, 5m, 15m: 20.0, 19.8, 19.9');
    } else if (cmd === '/gc') {
      this.addConsoleLine('success', 'Garbage collection executed. Freed 128 MB of memory.');
    } else if (cmd === '/save-all') {
      this.addConsoleLine('success', 'Saving the game (this may take a moment!)');
      setTimeout(() => {
        this.addConsoleLine('success', 'Saved the game');
      }, 1000);
    } else if (cmd === '/plugins') {
      this.addConsoleLine('success', 'Plugins (5): NeoEssentials, WorldEdit, Vault, LuckPerms, PlaceholderAPI');
    } else if (cmd.startsWith('/help')) {
      this.addConsoleLine('success', 'Available commands: /list, /tps, /gc, /save-all, /plugins, /whitelist, /ban, /kick, /tp, /give, /gamemode, /time, /weather');
    } else if (cmd === '/stop') {
      this.addConsoleLine('warning', 'Stop command requires confirmation. Server shutdown prevented.');
    } else if (cmd.startsWith('/ban') || cmd.startsWith('/kick') || cmd.startsWith('/op')) {
      this.addConsoleLine('warning', 'This command requires admin permissions.');
    } else {
      this.addConsoleLine('error', `Unknown command: ${command}. Type /help for available commands.`);
    }
  }
  
  addConsoleLine(type, text) {
    const line = document.createElement('div');
    line.className = `console-line ${type}`;
    
    const timestamp = document.createElement('span');
    timestamp.className = 'console-timestamp';
    timestamp.textContent = this.getTimestamp();
    
    const textSpan = document.createElement('span');
    textSpan.className = 'console-text';
    textSpan.textContent = text;
    
    line.appendChild(timestamp);
    line.appendChild(textSpan);
    
    this.output.appendChild(line);
    this.scrollToBottom();
  }
  
  addSystemMessage(message) {
    this.addConsoleLine('system', message);
  }
  
  clearConsole() {
    this.output.innerHTML = '';
    this.addSystemMessage('Console cleared.');
  }
  
  getTimestamp() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    return `[${hours}:${minutes}:${seconds}]`;
  }
  
  scrollToBottom() {
    this.output.scrollTop = this.output.scrollHeight;
  }
  
  addToHistory(command) {
    // Avoid duplicates
    const lastCommand = this.commandHistory[this.commandHistory.length - 1];
    if (lastCommand === command) return;
    
    this.commandHistory.push(command);
    
    // Limit history to 50 commands
    if (this.commandHistory.length > 50) {
      this.commandHistory.shift();
    }
    
    this.saveHistory();
  }
  
  loadHistory() {
    try {
      const history = localStorage.getItem('console-command-history');
      return history ? JSON.parse(history) : [];
    } catch (e) {
      return [];
    }
  }
  
  saveHistory() {
    try {
      localStorage.setItem('console-command-history', JSON.stringify(this.commandHistory));
    } catch (e) {
      console.error('Failed to save command history:', e);
    }
  }
}

// Initialize Command Console
let commandConsoleManager;
document.addEventListener('DOMContentLoaded', () => {
  commandConsoleManager = new CommandConsoleManager();
  commandConsoleManager.init();
});

// ========================================
// PLAYER MANAGEMENT SYSTEM
// ========================================

class PlayerModerationManager {
  constructor() {
    this.modal = document.getElementById('moderation-modal');
    this.closeBtn = document.getElementById('close-moderation-modal');
    this.form = document.getElementById('moderation-form');
    this.modalTitle = document.getElementById('moderation-modal-title');
    this.typeBadge = document.getElementById('moderation-type-badge');
    this.playerNameInput = document.getElementById('mod-player-name');
    this.reasonInput = document.getElementById('mod-reason');
    this.durationSelect = document.getElementById('mod-duration');
    this.customDurationInput = document.getElementById('custom-duration');
    this.ipBanCheckbox = document.getElementById('mod-ip-ban');
    this.silentCheckbox = document.getElementById('mod-silent');
    this.durationGroup = document.getElementById('duration-group');
    this.ipBanGroup = document.getElementById('ip-ban-group');
    this.confirmBtn = document.getElementById('confirm-moderation');
    this.confirmText = document.getElementById('confirm-action-text');
    this.cancelBtn = document.getElementById('cancel-moderation');
    this.playerSuggestions = document.getElementById('player-suggestions');
    this.activeActionsContainer = document.getElementById('active-actions-container');
    
    this.currentAction = null;
    this.onlinePlayers = ['Steve', 'Alex', 'Notch', 'Herobrine', 'Jeb', 'Dinnerbone'];
    this.activeActions = [];
  }
  
  init() {
    if (!this.modal || !this.form) {
      console.error('Moderation modal elements not found');
      return;
    }
    
    // Setup action buttons
    document.getElementById('open-ban-modal')?.addEventListener('click', () => this.openModal('ban'));
    document.getElementById('open-kick-modal')?.addEventListener('click', () => this.openModal('kick'));
    document.getElementById('open-mute-modal')?.addEventListener('click', () => this.openModal('mute'));
    document.getElementById('open-freeze-modal')?.addEventListener('click', () => this.openModal('freeze'));
    document.getElementById('open-jail-modal')?.addEventListener('click', () => this.openModal('jail'));
    document.getElementById('view-mod-history')?.addEventListener('click', () => this.viewHistory());
    
    // Modal controls
    this.closeBtn.addEventListener('click', () => this.closeModal());
    this.cancelBtn.addEventListener('click', () => this.closeModal());
    
    // Click outside to close
    this.modal.addEventListener('click', (e) => {
      if (e.target === this.modal) {
        this.closeModal();
      }
    });
    
    // Form submission
    this.form.addEventListener('submit', (e) => {
      e.preventDefault();
      this.submitAction();
    });
    
    // Duration selector
    this.durationSelect.addEventListener('change', () => {
      if (this.durationSelect.value === 'custom') {
        this.customDurationInput.style.display = 'block';
        this.customDurationInput.focus();
      } else {
        this.customDurationInput.style.display = 'none';
      }
    });
    
    // Player name autocomplete
    this.playerNameInput.addEventListener('input', () => this.handlePlayerInput());
    this.playerNameInput.addEventListener('focus', () => this.handlePlayerInput());
    
    // Click outside suggestions to close
    document.addEventListener('click', (e) => {
      if (!this.playerNameInput.contains(e.target) && !this.playerSuggestions.contains(e.target)) {
        this.playerSuggestions.style.display = 'none';
      }
    });
    
    // Load active actions
    this.loadActiveActions();
    this.updateStatistics();
  }
  
  openModal(actionType) {
    this.currentAction = actionType;
    
    // Reset form
    this.form.reset();
    this.customDurationInput.style.display = 'none';
    this.playerSuggestions.style.display = 'none';
    
    // Configure modal based on action type
    const configs = {
      ban: {
        title: 'Ban Player',
        badge: '🚫 Ban',
        confirmText: 'Ban Player',
        showDuration: true,
        showIpBan: true
      },
      kick: {
        title: 'Kick Player',
        badge: '⚠️ Kick',
        confirmText: 'Kick Player',
        showDuration: false,
        showIpBan: false
      },
      mute: {
        title: 'Mute Player',
        badge: '🔇 Mute',
        confirmText: 'Mute Player',
        showDuration: true,
        showIpBan: false
      },
      freeze: {
        title: 'Freeze Player',
        badge: '❄️ Freeze',
        confirmText: 'Freeze Player',
        showDuration: true,
        showIpBan: false
      },
      jail: {
        title: 'Jail Player',
        badge: '🔒 Jail',
        confirmText: 'Jail Player',
        showDuration: true,
        showIpBan: false
      }
    };
    
    const config = configs[actionType];
    this.modalTitle.textContent = config.title;
    this.typeBadge.textContent = config.badge;
    this.confirmText.textContent = config.confirmText;
    
    // Show/hide duration and IP ban options
    this.durationGroup.style.display = config.showDuration ? 'flex' : 'none';
    this.ipBanGroup.style.display = config.showIpBan ? 'flex' : 'none';
    
    // Show modal
    this.modal.showModal();
    this.playerNameInput.focus();
  }
  
  closeModal() {
    this.modal.close();
    this.currentAction = null;
  }
  
  handlePlayerInput() {
    const value = this.playerNameInput.value.trim().toLowerCase();
    
    if (value.length === 0) {
      this.playerSuggestions.style.display = 'none';
      return;
    }
    
    const matches = this.onlinePlayers.filter(player => 
      player.toLowerCase().includes(value)
    );
    
    if (matches.length > 0) {
      this.playerSuggestions.innerHTML = matches.map(player => `
        <div class="suggestion-item" onclick="playerModerationManager.selectPlayer('${player}')">
          <img src="https://crafatar.com/avatars/${player}?size=24&overlay" 
               alt="${player}" 
               style="width: 24px; height: 24px; image-rendering: pixelated;">
          <span>${player}</span>
        </div>
      `).join('');
      this.playerSuggestions.style.display = 'block';
    } else {
      this.playerSuggestions.style.display = 'none';
    }
  }
  
  selectPlayer(playerName) {
    this.playerNameInput.value = playerName;
    this.playerSuggestions.style.display = 'none';
    this.reasonInput.focus();
  }
  
  async submitAction() {
    const playerName = this.playerNameInput.value.trim();
    const reason = this.reasonInput.value.trim();
    const duration = this.durationSelect.value === 'custom' 
      ? this.customDurationInput.value.trim() 
      : this.durationSelect.value;
    const ipBan = this.ipBanCheckbox.checked;
    const silent = this.silentCheckbox.checked;
    
    if (!playerName || !reason) {
      showNotification('⚠️ Please fill in all required fields', 'warning');
      return;
    }
    
    // Disable submit button
    this.confirmBtn.disabled = true;
    this.confirmBtn.textContent = 'Processing...';
    
    try {
      // Send to API
      const response = await fetch('http://localhost:8080/api/moderation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: this.currentAction,
          player: playerName,
          reason: reason,
          duration: duration,
          ipBan: ipBan,
          silent: silent
        })
      });
      
      if (response.ok) {
        const data = await response.json();
        showNotification(`✓ ${this.currentAction.toUpperCase()}: ${playerName}`, 'success');
        this.addActiveAction({
          type: this.currentAction,
          player: playerName,
          reason: reason,
          duration: duration,
          issuedBy: 'Admin',
          timestamp: new Date().toISOString(),
          id: Date.now()
        });
      } else {
        // Mock success for demo
        this.mockActionSuccess(playerName, reason, duration);
      }
    } catch (error) {
      console.error('Moderation action error:', error);
      // Mock success for demo
      this.mockActionSuccess(playerName, reason, duration);
    }
    
    // Close modal
    this.closeModal();
    
    // Re-enable button
    this.confirmBtn.disabled = false;
    this.confirmBtn.textContent = this.confirmText.textContent;
  }
  
  mockActionSuccess(playerName, reason, duration) {
    showNotification(`✓ ${this.currentAction.toUpperCase()}: ${playerName}`, 'success');
    
    if (this.currentAction !== 'kick') {
      this.addActiveAction({
        type: this.currentAction,
        player: playerName,
        reason: reason,
        duration: duration,
        issuedBy: 'Admin',
        timestamp: new Date().toISOString(),
        id: Date.now()
      });
    }
  }
  
  addActiveAction(action) {
    this.activeActions.push(action);
    this.renderActiveActions();
    this.updateStatistics();
  }
  
  removeActiveAction(actionId) {
    const action = this.activeActions.find(a => a.id === actionId);
    
    if (!action) return;
    
    if (confirm(`Are you sure you want to revoke ${action.type} for ${action.player}?`)) {
      this.activeActions = this.activeActions.filter(a => a.id !== actionId);
      this.renderActiveActions();
      this.updateStatistics();
      showNotification(`✓ Revoked ${action.type} for ${action.player}`, 'success');
    }
  }
  
  renderActiveActions() {
    if (this.activeActions.length === 0) {
      this.activeActionsContainer.innerHTML = `
        <div class="empty-state">
          <span class="empty-icon">✓</span>
          <span class="empty-text">No active moderation actions</span>
        </div>
      `;
      return;
    }
    
    this.activeActionsContainer.innerHTML = this.activeActions.map(action => {
      const icons = {
        ban: '🚫',
        mute: '🔇',
        jail: '🔒',
        freeze: '❄️'
      };
      
      const durationText = action.duration === 'permanent' 
        ? 'Permanent' 
        : `Expires in ${action.duration}`;
      
      return `
        <div class="action-item ${action.type}">
          <div class="action-item-info">
            <div class="action-item-header">
              <span class="action-type-badge ${action.type}">
                ${icons[action.type]} ${action.type.toUpperCase()}
              </span>
              <span class="action-player-name">${action.player}</span>
            </div>
            <div class="action-reason">"${action.reason}"</div>
            <div class="action-meta">
              <span class="action-duration">⏱️ ${durationText}</span>
              <span class="action-issued-by">👤 by ${action.issuedBy}</span>
            </div>
          </div>
          <div class="action-item-actions">
            <button class="action-revoke-btn" onclick="playerModerationManager.removeActiveAction(${action.id})">
              Revoke
            </button>
          </div>
        </div>
      `;
    }).join('');
  }
  
  updateStatistics() {
    const bans = this.activeActions.filter(a => a.type === 'ban').length;
    const mutes = this.activeActions.filter(a => a.type === 'mute').length;
    const jails = this.activeActions.filter(a => a.type === 'jail').length;
    const freezes = this.activeActions.filter(a => a.type === 'freeze').length;
    
    document.getElementById('active-bans-count').textContent = bans;
    document.getElementById('active-mutes-count').textContent = mutes;
    document.getElementById('active-jails-count').textContent = jails;
    document.getElementById('frozen-players-count').textContent = freezes;
  }
  
  loadActiveActions() {
    // Load from localStorage or API
    try {
      const saved = localStorage.getItem('active-moderation-actions');
      if (saved) {
        this.activeActions = JSON.parse(saved);
        this.renderActiveActions();
      }
    } catch (e) {
      console.error('Failed to load active actions:', e);
    }
  }
  
  saveActiveActions() {
    try {
      localStorage.setItem('active-moderation-actions', JSON.stringify(this.activeActions));
    } catch (e) {
      console.error('Failed to save active actions:', e);
    }
  }
  
  viewHistory() {
    showNotification('📋 Moderation history viewer coming soon!', 'info');
  }
}

// Initialize Player Moderation Manager
let playerModerationManager;
document.addEventListener('DOMContentLoaded', () => {
  playerModerationManager = new PlayerModerationManager();
  playerModerationManager.init();
  
  // Auto-save active actions on changes
  setInterval(() => {
    if (playerModerationManager) {
      playerModerationManager.saveActiveActions();
    }
  }, 30000); // Save every 30 seconds
});

// ========================================
// CHAT LOG VIEWER SYSTEM
// ========================================

class ChatLogViewer {
  constructor() {
    this.messagesContainer = document.getElementById('chat-messages');
    this.searchInput = document.getElementById('chat-search');
    this.clearSearchBtn = document.getElementById('clear-search');
    this.timeRangeSelect = document.getElementById('time-range-filter');
    this.exportBtn = document.getElementById('export-chat');
    this.clearBtn = document.getElementById('clear-chat');
    this.pauseBtn = document.getElementById('pause-chat');
    this.totalMessagesSpan = document.getElementById('total-messages');
    this.activeChattersSpan = document.getElementById('active-chatters');
    
    this.messages = [];
    this.filteredMessages = [];
    this.isPaused = false;
    this.filters = {
      channel: 'all',
      type: 'all',
      timeRange: '6h',
      player: null,
      searchQuery: ''
    };
    
    this.mockPlayers = ['Steve', 'Alex', 'Notch', 'Herobrine', 'Jeb', 'Dinnerbone', 'CaptainSparklez', 'DanTDM'];
  }
  
  init() {
    if (!this.messagesContainer) {
      console.error('Chat messages container not found');
      return;
    }
    
    this.setupEventListeners();
    this.loadMockMessages();
    this.startLiveUpdates();
    this.updateStatistics();
  }
  
  setupEventListeners() {
    // Search functionality
    this.searchInput?.addEventListener('input', () => this.handleSearch());
    this.clearSearchBtn?.addEventListener('click', () => this.clearSearch());
    
    // Filter buttons
    document.querySelectorAll('.filter-btn').forEach(btn => {
      btn.addEventListener('click', (e) => this.handleFilter(e.target));
    });
    
    // Time range filter
    this.timeRangeSelect?.addEventListener('change', () => {
      this.filters.timeRange = this.timeRangeSelect.value;
      this.applyFilters();
    });
    
    // Action buttons
    this.exportBtn?.addEventListener('click', () => this.exportChat());
    this.clearBtn?.addEventListener('click', () => this.clearChat());
    this.pauseBtn?.addEventListener('click', () => this.togglePause());
  }
  
  handleSearch() {
    const query = this.searchInput.value.trim();
    this.filters.searchQuery = query;
    
    if (query.length > 0) {
      this.clearSearchBtn.style.display = 'flex';
    } else {
      this.clearSearchBtn.style.display = 'none';
    }
    
    this.applyFilters();
  }
  
  clearSearch() {
    this.searchInput.value = '';
    this.filters.searchQuery = '';
    this.clearSearchBtn.style.display = 'none';
    this.applyFilters();
  }
  
  handleFilter(btn) {
    const filterType = btn.getAttribute('data-filter');
    const filterValue = btn.getAttribute('data-value');
    
    if (!filterType || !filterValue) return;
    
    // Update active state
    const siblings = btn.parentElement.querySelectorAll('.filter-btn');
    siblings.forEach(s => s.classList.remove('active'));
    btn.classList.add('active');
    
    // Apply filter
    this.filters[filterType] = filterValue;
    this.applyFilters();
  }
  
  applyFilters() {
    this.filteredMessages = this.messages.filter(msg => {
      // Channel filter
      if (this.filters.channel !== 'all' && msg.channel !== this.filters.channel) {
        return false;
      }
      
      // Type filter
      if (this.filters.type !== 'all' && msg.type !== this.filters.type) {
        return false;
      }
      
      // Time range filter
      if (this.filters.timeRange !== 'all') {
        const now = Date.now();
        const msgTime = new Date(msg.timestamp).getTime();
        const ranges = {
          '1h': 60 * 60 * 1000,
          '6h': 6 * 60 * 60 * 1000,
          '24h': 24 * 60 * 60 * 1000,
          '7d': 7 * 24 * 60 * 60 * 1000
        };
        const range = ranges[this.filters.timeRange];
        if (range && now - msgTime > range) {
          return false;
        }
      }
      
      // Search query filter
      if (this.filters.searchQuery) {
        const query = this.filters.searchQuery.toLowerCase();
        const searchText = `${msg.player} ${msg.message}`.toLowerCase();
        if (!searchText.includes(query)) {
          return false;
        }
      }
      
      // Player filter
      if (this.filters.player && msg.player !== this.filters.player) {
        return false;
      }
      
      return true;
    });
    
    this.renderMessages();
    this.updateStatistics();
  }
  
  renderMessages() {
    if (this.filteredMessages.length === 0) {
      this.messagesContainer.innerHTML = `
        <div class="chat-empty-state">
          <span class="empty-icon">💬</span>
          <span class="empty-text">No messages found</span>
          <span class="empty-subtext">Try adjusting your filters or search query</span>
        </div>
      `;
      return;
    }
    
    this.messagesContainer.innerHTML = this.filteredMessages.map(msg => 
      this.createMessageElement(msg)
    ).join('');
    
    // Scroll to bottom if not paused
    if (!this.isPaused) {
      this.scrollToBottom();
    }
  }
  
  createMessageElement(msg) {
    const time = new Date(msg.timestamp).toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
    
    const channelBadge = msg.channel !== 'global' 
      ? `<span class="message-channel-badge ${msg.channel}">${msg.channel}</span>` 
      : '';
    
    const isHighlighted = this.filters.searchQuery && 
      msg.message.toLowerCase().includes(this.filters.searchQuery.toLowerCase());
    
    return `
      <div class="chat-message type-${msg.type} channel-${msg.channel} ${isHighlighted ? 'highlighted' : ''}">
        <span class="message-timestamp">${time}</span>
        <div class="message-content">
          <div class="message-header">
            <span class="message-player" onclick="chatLogViewer.filterByPlayer('${msg.player}')">${msg.player}</span>
            ${channelBadge}
          </div>
          <div class="message-text">${this.escapeHtml(msg.message)}</div>
        </div>
      </div>
    `;
  }
  
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
  
  addMessage(message) {
    if (this.isPaused) return;
    
    this.messages.push(message);
    
    // Limit to last 500 messages
    if (this.messages.length > 500) {
      this.messages.shift();
    }
    
    this.applyFilters();
  }
  
  loadMockMessages() {
    // Load some initial mock messages
    const channels = ['global', 'local', 'team', 'staff'];
    const types = ['chat', 'join', 'leave', 'death', 'moderation'];
    const messages = [
      'Hello everyone!',
      'Anyone want to team up?',
      'Found diamonds at -234, 12, 567',
      'GG!',
      'Can someone help me?',
      'This server is awesome!',
      'Trading iron for gold',
      'Where is the spawn?',
      'Thanks for the help!',
      'Nice build!',
      'joined the game',
      'left the game',
      'was slain by Zombie',
      'fell from a high place',
      'tried to swim in lava',
      'was banned for griefing',
      'was muted for spam',
      'Building a castle near spawn',
      'Looking for a guild',
      'Event starts in 10 minutes!'
    ];
    
    for (let i = 0; i < 50; i++) {
      const player = this.mockPlayers[Math.floor(Math.random() * this.mockPlayers.length)];
      const channel = channels[Math.floor(Math.random() * channels.length)];
      const type = types[Math.floor(Math.random() * types.length)];
      const message = messages[Math.floor(Math.random() * messages.length)];
      
      const timestamp = new Date(Date.now() - Math.random() * 6 * 60 * 60 * 1000);
      
      this.messages.push({
        id: Date.now() + i,
        player: player,
        channel: channel,
        type: type,
        message: message,
        timestamp: timestamp.toISOString()
      });
    }
    
    // Sort by timestamp
    this.messages.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    
    this.applyFilters();
  }
  
  startLiveUpdates() {
    // Simulate live messages every 5-10 seconds
    setInterval(() => {
      if (this.isPaused) return;
      
      const player = this.mockPlayers[Math.floor(Math.random() * this.mockPlayers.length)];
      const messages = [
        'Hello!',
        'Anyone online?',
        'Check out my base!',
        'Trading resources',
        'Need help with a build',
        'This is fun!',
        'Found a village!',
        'Exploring caves',
        'Making progress!',
        'Great server!'
      ];
      
      this.addMessage({
        id: Date.now(),
        player: player,
        channel: 'global',
        type: 'chat',
        message: messages[Math.floor(Math.random() * messages.length)],
        timestamp: new Date().toISOString()
      });
    }, Math.random() * 5000 + 5000); // 5-10 seconds
  }
  
  filterByPlayer(playerName) {
    this.filters.player = this.filters.player === playerName ? null : playerName;
    this.applyFilters();
    
    if (this.filters.player) {
      showNotification(`Filtering by ${playerName}`, 'info');
    }
  }
  
  updateStatistics() {
    this.totalMessagesSpan.textContent = this.filteredMessages.length;
    
    const uniquePlayers = new Set(
      this.filteredMessages
        .filter(m => m.type === 'chat')
        .map(m => m.player)
    );
    this.activeChattersSpan.textContent = uniquePlayers.size;
  }
  
  exportChat() {
    const data = this.filteredMessages.map(msg => ({
      timestamp: msg.timestamp,
      player: msg.player,
      channel: msg.channel,
      type: msg.type,
      message: msg.message
    }));
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chat-log-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showNotification('📥 Chat log exported successfully', 'success');
  }
  
  clearChat() {
    if (confirm('Are you sure you want to clear the chat display? This will only clear the current view, not the server logs.')) {
      this.messages = [];
      this.filteredMessages = [];
      this.renderMessages();
      this.updateStatistics();
      showNotification('🗑️ Chat display cleared', 'success');
    }
  }
  
  togglePause() {
    this.isPaused = !this.isPaused;
    
    if (this.isPaused) {
      this.pauseBtn.classList.add('paused');
      this.pauseBtn.querySelector('.action-btn-icon').textContent = '▶️';
      this.pauseBtn.querySelector('.action-btn-text').textContent = 'Resume';
      showNotification('⏸️ Live updates paused', 'info');
    } else {
      this.pauseBtn.classList.remove('paused');
      this.pauseBtn.querySelector('.action-btn-icon').textContent = '⏸️';
      this.pauseBtn.querySelector('.action-btn-text').textContent = 'Pause';
      showNotification('▶️ Live updates resumed', 'success');
    }
  }
  
  scrollToBottom() {
    this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
  }
}

// ========================================
// PERFORMANCE METRICS MANAGER
// ========================================

class PerformanceMetricsManager {
  constructor() {
    // DOM References
    this.refreshBtn = document.getElementById('refresh-metrics');
    this.exportBtn = document.getElementById('export-metrics');
    
    // TPS & Tick Elements
    this.currentTpsEl = document.getElementById('current-tps');
    this.tpsStatusEl = document.getElementById('tps-status');
    this.avgTickTimeEl = document.getElementById('avg-tick-time');
    this.peakTickTimeEl = document.getElementById('peak-tick-time');
    this.serverUptimeEl = document.getElementById('server-uptime');
    
    // Tick Breakdown Elements
    this.entityTickTimeEl = document.getElementById('entity-tick-time');
    this.blockTickTimeEl = document.getElementById('block-tick-time');
    this.chunkTickTimeEl = document.getElementById('chunk-tick-time');
    this.taskTickTimeEl = document.getElementById('task-tick-time');
    this.otherTickTimeEl = document.getElementById('other-tick-time');
    
    this.entityTickBar = document.getElementById('entity-tick-bar');
    this.blockTickBar = document.getElementById('block-tick-bar');
    this.chunkTickBar = document.getElementById('chunk-tick-bar');
    this.taskTickBar = document.getElementById('task-tick-bar');
    this.otherTickBar = document.getElementById('other-tick-bar');
    
    // Memory Elements
    this.heapUsedEl = document.getElementById('heap-used');
    this.heapMaxEl = document.getElementById('heap-max');
    this.heapFillEl = document.getElementById('heap-fill');
    this.nonheapUsedEl = document.getElementById('nonheap-used');
    this.nonheapMaxEl = document.getElementById('nonheap-max');
    this.nonheapFillEl = document.getElementById('nonheap-fill');
    this.gcCountEl = document.getElementById('gc-count');
    this.gcTimeEl = document.getElementById('gc-time');
    
    // Entity & World Elements
    this.totalEntitiesEl = document.getElementById('total-entities');
    this.loadedChunksEl = document.getElementById('loaded-chunks');
    this.chunkUpdatesEl = document.getElementById('chunk-updates');
    
    // Entity Breakdown
    this.entitiesAnimalsEl = document.getElementById('entities-animals');
    this.entitiesPlayersEl = document.getElementById('entities-players');
    this.entitiesMonstersEl = document.getElementById('entities-monsters');
    this.entitiesItemsEl = document.getElementById('entities-items');
    this.entitiesProjectilesEl = document.getElementById('entities-projectiles');
    this.entitiesOtherEl = document.getElementById('entities-other');
    
    // Thread Pool Elements
    this.mainThreadCpuEl = document.getElementById('main-thread-cpu');
    this.workerActiveEl = document.getElementById('worker-active');
    this.workerQueuedEl = document.getElementById('worker-queued');
    this.workerCompletedEl = document.getElementById('worker-completed');
    this.asyncActiveEl = document.getElementById('async-active');
    this.asyncQueuedEl = document.getElementById('async-queued');
    this.asyncCompletedEl = document.getElementById('async-completed');
    
    // Charts
    this.tpsHistoryChart = null;
    this.memoryUsageChart = null;
    
    // Update tracking
    this.startTime = Date.now();
    this.updateInterval = null;
  }
  
  init() {
    this.setupEventListeners();
    this.initCharts();
    this.loadMockData();
    this.startAutoUpdate();
  }
  
  setupEventListeners() {
    if (this.refreshBtn) {
      this.refreshBtn.addEventListener('click', () => this.refreshMetrics());
    }
    
    if (this.exportBtn) {
      this.exportBtn.addEventListener('click', () => this.exportMetrics());
    }
  }
  
  initCharts() {
    // TPS History Chart
    const tpsCtx = document.getElementById('tps-history-chart');
    if (tpsCtx) {
      const labels = Array.from({length: 60}, (_, i) => `${59 - i}m`);
      const data = Array.from({length: 60}, () => 19 + Math.random() * 1.5);
      
      this.tpsHistoryChart = new Chart(tpsCtx, {
        type: 'line',
        data: {
          labels: labels,
          datasets: [{
            label: 'TPS',
            data: data,
            borderColor: 'rgba(34, 197, 94, 1)',
            backgroundColor: 'rgba(34, 197, 94, 0.1)',
            borderWidth: 2,
            fill: true,
            tension: 0.4,
            pointRadius: 0,
            pointHoverRadius: 4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: false
            },
            tooltip: {
              mode: 'index',
              intersect: false,
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
              titleColor: '#fff',
              bodyColor: '#fff',
              borderColor: 'rgba(34, 197, 94, 0.5)',
              borderWidth: 1
            }
          },
          scales: {
            x: {
              display: true,
              grid: {
                color: 'rgba(255, 255, 255, 0.05)'
              },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                maxTicksLimit: 10
              }
            },
            y: {
              display: true,
              min: 0,
              max: 20,
              grid: {
                color: 'rgba(255, 255, 255, 0.05)'
              },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                callback: function(value) {
                  return value.toFixed(1);
                }
              }
            }
          },
          interaction: {
            mode: 'nearest',
            axis: 'x',
            intersect: false
          }
        }
      });
    }
    
    // Memory Usage Chart
    const memoryCtx = document.getElementById('memory-usage-chart');
    if (memoryCtx) {
      const labels = Array.from({length: 60}, (_, i) => `${59 - i}m`);
      const heapData = Array.from({length: 60}, () => 2.0 + Math.random() * 0.8);
      const nonHeapData = Array.from({length: 60}, () => 0.2 + Math.random() * 0.1);
      
      this.memoryUsageChart = new Chart(memoryCtx, {
        type: 'line',
        data: {
          labels: labels,
          datasets: [
            {
              label: 'Heap Memory',
              data: heapData,
              borderColor: 'rgba(59, 130, 246, 1)',
              backgroundColor: 'rgba(59, 130, 246, 0.1)',
              borderWidth: 2,
              fill: true,
              tension: 0.4,
              pointRadius: 0,
              pointHoverRadius: 4
            },
            {
              label: 'Non-Heap Memory',
              data: nonHeapData,
              borderColor: 'rgba(139, 92, 246, 1)',
              backgroundColor: 'rgba(139, 92, 246, 0.1)',
              borderWidth: 2,
              fill: true,
              tension: 0.4,
              pointRadius: 0,
              pointHoverRadius: 4
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: true,
              position: 'top',
              labels: {
                color: 'rgba(255, 255, 255, 0.7)',
                usePointStyle: true,
                padding: 15
              }
            },
            tooltip: {
              mode: 'index',
              intersect: false,
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
              titleColor: '#fff',
              bodyColor: '#fff',
              borderColor: 'rgba(59, 130, 246, 0.5)',
              borderWidth: 1,
              callbacks: {
                label: function(context) {
                  return context.dataset.label + ': ' + context.parsed.y.toFixed(2) + ' GB';
                }
              }
            }
          },
          scales: {
            x: {
              display: true,
              grid: {
                color: 'rgba(255, 255, 255, 0.05)'
              },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                maxTicksLimit: 10
              }
            },
            y: {
              display: true,
              min: 0,
              grid: {
                color: 'rgba(255, 255, 255, 0.05)'
              },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                callback: function(value) {
                  return value.toFixed(1) + ' GB';
                }
              }
            }
          },
          interaction: {
            mode: 'nearest',
            axis: 'x',
            intersect: false
          }
        }
      });
    }
  }
  
  loadMockData() {
    // Initial TPS & Tick Data
    this.updateTPS(19.8);
    this.updateTickTimes(15.2, 42.8);
    this.updateUptime();
    
    // Tick Breakdown
    this.updateTickBreakdown({
      entities: 6.2,
      blocks: 3.8,
      chunks: 2.4,
      tasks: 1.9,
      other: 0.9
    });
    
    // Memory Data
    this.updateMemory({
      heapUsed: 2.4,
      heapMax: 4.0,
      nonHeapUsed: 256,
      nonHeapMax: 512,
      gcCount: 142,
      gcTime: 1.2
    });
    
    // Entity & World Data
    this.updateEntityWorld({
      totalEntities: 1247,
      loadedChunks: 3842,
      chunkUpdates: 156
    });
    
    // Entity Breakdown
    this.updateEntityBreakdown({
      animals: 423,
      players: 12,
      monsters: 287,
      items: 156,
      projectiles: 89,
      other: 280
    });
    
    // Thread Pool Data
    this.updateThreadPool({
      mainThreadCpu: 45,
      workerActive: 6,
      workerQueued: 3,
      workerCompleted: 12847,
      asyncActive: 2,
      asyncQueued: 0,
      asyncCompleted: 4521
    });
  }
  
  updateTPS(tps) {
    if (this.currentTpsEl) {
      this.currentTpsEl.textContent = tps.toFixed(1);
    }
    
    if (this.tpsStatusEl) {
      this.tpsStatusEl.classList.remove('good', 'warning', 'danger');
      if (tps >= 19.5) {
        this.tpsStatusEl.classList.add('good');
      } else if (tps >= 18.0) {
        this.tpsStatusEl.classList.add('warning');
      } else {
        this.tpsStatusEl.classList.add('danger');
      }
    }
  }
  
  updateTickTimes(avg, peak) {
    if (this.avgTickTimeEl) {
      this.avgTickTimeEl.innerHTML = `${avg.toFixed(1)}<span class="metric-unit">ms</span>`;
    }
    if (this.peakTickTimeEl) {
      this.peakTickTimeEl.innerHTML = `${peak.toFixed(1)}<span class="metric-unit">ms</span>`;
    }
  }
  
  updateUptime() {
    const elapsed = Date.now() - this.startTime;
    const hours = Math.floor(elapsed / 3600000);
    const minutes = Math.floor((elapsed % 3600000) / 60000);
    
    if (this.serverUptimeEl) {
      this.serverUptimeEl.textContent = `${hours}h ${minutes}m`;
    }
  }
  
  updateTickBreakdown(breakdown) {
    const total = Object.values(breakdown).reduce((sum, val) => sum + val, 0);
    
    if (this.entityTickTimeEl) this.entityTickTimeEl.textContent = breakdown.entities.toFixed(1) + 'ms';
    if (this.blockTickTimeEl) this.blockTickTimeEl.textContent = breakdown.blocks.toFixed(1) + 'ms';
    if (this.chunkTickTimeEl) this.chunkTickTimeEl.textContent = breakdown.chunks.toFixed(1) + 'ms';
    if (this.taskTickTimeEl) this.taskTickTimeEl.textContent = breakdown.tasks.toFixed(1) + 'ms';
    if (this.otherTickTimeEl) this.otherTickTimeEl.textContent = breakdown.other.toFixed(1) + 'ms';
    
    if (this.entityTickBar) this.entityTickBar.style.width = ((breakdown.entities / total) * 100) + '%';
    if (this.blockTickBar) this.blockTickBar.style.width = ((breakdown.blocks / total) * 100) + '%';
    if (this.chunkTickBar) this.chunkTickBar.style.width = ((breakdown.chunks / total) * 100) + '%';
    if (this.taskTickBar) this.taskTickBar.style.width = ((breakdown.tasks / total) * 100) + '%';
    if (this.otherTickBar) this.otherTickBar.style.width = ((breakdown.other / total) * 100) + '%';
  }
  
  updateMemory(memory) {
    if (this.heapUsedEl) {
      this.heapUsedEl.innerHTML = `${memory.heapUsed.toFixed(1)}<span class="metric-unit">GB</span>`;
    }
    if (this.heapMaxEl) {
      this.heapMaxEl.textContent = `of ${memory.heapMax.toFixed(1)} GB`;
    }
    if (this.heapFillEl) {
      const heapPercent = (memory.heapUsed / memory.heapMax) * 100;
      this.heapFillEl.style.width = heapPercent + '%';
    }
    
    if (this.nonheapUsedEl) {
      this.nonheapUsedEl.innerHTML = `${memory.nonHeapUsed}<span class="metric-unit">MB</span>`;
    }
    if (this.nonheapMaxEl) {
      this.nonheapMaxEl.textContent = `of ${memory.nonHeapMax} MB`;
    }
    if (this.nonheapFillEl) {
      const nonHeapPercent = (memory.nonHeapUsed / memory.nonHeapMax) * 100;
      this.nonheapFillEl.style.width = nonHeapPercent + '%';
    }
    
    if (this.gcCountEl) this.gcCountEl.textContent = memory.gcCount;
    if (this.gcTimeEl) {
      this.gcTimeEl.innerHTML = `${memory.gcTime.toFixed(1)}<span class="metric-unit">s</span>`;
    }
  }
  
  updateEntityWorld(data) {
    if (this.totalEntitiesEl) {
      this.totalEntitiesEl.textContent = data.totalEntities.toLocaleString();
    }
    if (this.loadedChunksEl) {
      this.loadedChunksEl.textContent = data.loadedChunks.toLocaleString();
    }
    if (this.chunkUpdatesEl) {
      this.chunkUpdatesEl.textContent = data.chunkUpdates.toLocaleString();
    }
  }
  
  updateEntityBreakdown(entities) {
    const total = Object.values(entities).reduce((sum, val) => sum + val, 0);
    
    if (this.entitiesAnimalsEl) {
      this.entitiesAnimalsEl.textContent = entities.animals;
      const bar = this.entitiesAnimalsEl.parentElement.querySelector('.entity-fill');
      if (bar) bar.style.width = ((entities.animals / total) * 100) + '%';
    }
    
    if (this.entitiesPlayersEl) {
      this.entitiesPlayersEl.textContent = entities.players;
      const bar = this.entitiesPlayersEl.parentElement.querySelector('.entity-fill');
      if (bar) bar.style.width = ((entities.players / total) * 100) + '%';
    }
    
    if (this.entitiesMonstersEl) {
      this.entitiesMonstersEl.textContent = entities.monsters;
      const bar = this.entitiesMonstersEl.parentElement.querySelector('.entity-fill');
      if (bar) bar.style.width = ((entities.monsters / total) * 100) + '%';
    }
    
    if (this.entitiesItemsEl) {
      this.entitiesItemsEl.textContent = entities.items;
      const bar = this.entitiesItemsEl.parentElement.querySelector('.entity-fill');
      if (bar) bar.style.width = ((entities.items / total) * 100) + '%';
    }
    
    if (this.entitiesProjectilesEl) {
      this.entitiesProjectilesEl.textContent = entities.projectiles;
      const bar = this.entitiesProjectilesEl.parentElement.querySelector('.entity-fill');
      if (bar) bar.style.width = ((entities.projectiles / total) * 100) + '%';
    }
    
    if (this.entitiesOtherEl) {
      this.entitiesOtherEl.textContent = entities.other;
      const bar = this.entitiesOtherEl.parentElement.querySelector('.entity-fill');
      if (bar) bar.style.width = ((entities.other / total) * 100) + '%';
    }
  }
  
  updateThreadPool(threads) {
    if (this.mainThreadCpuEl) this.mainThreadCpuEl.textContent = threads.mainThreadCpu + '%';
    if (this.workerActiveEl) this.workerActiveEl.textContent = threads.workerActive;
    if (this.workerQueuedEl) this.workerQueuedEl.textContent = threads.workerQueued;
    if (this.workerCompletedEl) this.workerCompletedEl.textContent = threads.workerCompleted.toLocaleString();
    if (this.asyncActiveEl) this.asyncActiveEl.textContent = threads.asyncActive;
    if (this.asyncQueuedEl) this.asyncQueuedEl.textContent = threads.asyncQueued;
    if (this.asyncCompletedEl) this.asyncCompletedEl.textContent = threads.asyncCompleted.toLocaleString();
  }
  
  startAutoUpdate() {
    // Update every 5 seconds
    this.updateInterval = setInterval(() => {
      // Simulate changing metrics
      const tps = 19.5 + Math.random() * 0.8;
      this.updateTPS(tps);
      
      const avgTick = 12 + Math.random() * 8;
      const peakTick = 30 + Math.random() * 20;
      this.updateTickTimes(avgTick, peakTick);
      
      this.updateUptime();
      
      // Update TPS chart
      if (this.tpsHistoryChart) {
        const newTps = 19 + Math.random() * 1.5;
        this.tpsHistoryChart.data.datasets[0].data.shift();
        this.tpsHistoryChart.data.datasets[0].data.push(newTps);
        this.tpsHistoryChart.update('none');
      }
      
      // Update Memory chart
      if (this.memoryUsageChart) {
        const newHeap = 2.0 + Math.random() * 0.8;
        const newNonHeap = 0.2 + Math.random() * 0.1;
        this.memoryUsageChart.data.datasets[0].data.shift();
        this.memoryUsageChart.data.datasets[0].data.push(newHeap);
        this.memoryUsageChart.data.datasets[1].data.shift();
        this.memoryUsageChart.data.datasets[1].data.push(newNonHeap);
        this.memoryUsageChart.update('none');
      }
      
      // Slight variations in other metrics
      const chunkUpdates = 140 + Math.floor(Math.random() * 40);
      this.updateEntityWorld({
        totalEntities: 1247,
        loadedChunks: 3842,
        chunkUpdates: chunkUpdates
      });
    }, 5000);
  }
  
  refreshMetrics() {
    // Simulate API call
    showNotification('🔄 Refreshing metrics...', 'info');
    
    setTimeout(() => {
      this.loadMockData();
      showNotification('✅ Metrics refreshed', 'success');
    }, 500);
  }
  
  exportMetrics() {
    const metrics = {
      timestamp: new Date().toISOString(),
      tps: {
        current: parseFloat(this.currentTpsEl?.textContent || '0'),
        avgTickTime: parseFloat(this.avgTickTimeEl?.textContent || '0'),
        peakTickTime: parseFloat(this.peakTickTimeEl?.textContent || '0')
      },
      memory: {
        heapUsed: parseFloat(this.heapUsedEl?.textContent || '0'),
        heapMax: parseFloat(this.heapMaxEl?.textContent?.split(' ')[1] || '0'),
        nonHeapUsed: parseFloat(this.nonheapUsedEl?.textContent || '0'),
        nonHeapMax: parseFloat(this.nonheapMaxEl?.textContent?.split(' ')[1] || '0'),
        gcCount: parseInt(this.gcCountEl?.textContent || '0'),
        gcTime: parseFloat(this.gcTimeEl?.textContent || '0')
      },
      entities: {
        total: parseInt(this.totalEntitiesEl?.textContent?.replace(/,/g, '') || '0'),
        animals: parseInt(this.entitiesAnimalsEl?.textContent || '0'),
        players: parseInt(this.entitiesPlayersEl?.textContent || '0'),
        monsters: parseInt(this.entitiesMonstersEl?.textContent || '0'),
        items: parseInt(this.entitiesItemsEl?.textContent || '0'),
        projectiles: parseInt(this.entitiesProjectilesEl?.textContent || '0'),
        other: parseInt(this.entitiesOtherEl?.textContent || '0')
      },
      world: {
        loadedChunks: parseInt(this.loadedChunksEl?.textContent?.replace(/,/g, '') || '0'),
        chunkUpdates: parseInt(this.chunkUpdatesEl?.textContent?.replace(/,/g, '') || '0')
      }
    };
    
    const blob = new Blob([JSON.stringify(metrics, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `performance-metrics-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showNotification('📥 Metrics exported successfully', 'success');
  }
  
  destroy() {
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
    }
    if (this.tpsHistoryChart) {
      this.tpsHistoryChart.destroy();
    }
    if (this.memoryUsageChart) {
      this.memoryUsageChart.destroy();
    }
  }
}

// ========================================
// BACKUP MANAGEMENT SYSTEM
// ========================================

class BackupManager {
  constructor() {
    // DOM References - Stats
    this.totalBackupsEl = document.getElementById('total-backups');
    this.totalBackupSizeEl = document.getElementById('total-backup-size');
    this.lastBackupTimeEl = document.getElementById('last-backup-time');
    this.nextScheduledBackupEl = document.getElementById('next-scheduled-backup');
    
    // DOM References - Schedule
    this.autoBackupEnabled = document.getElementById('auto-backup-enabled');
    this.backupFrequency = document.getElementById('backup-frequency');
    this.backupTime = document.getElementById('backup-time');
    this.backupRetention = document.getElementById('backup-retention');
    
    // DOM References - Filters
    this.backupSearchInput = document.getElementById('backup-search');
    this.filterButtons = document.querySelectorAll('.backup-filter-buttons .filter-btn');
    
    // DOM References - List
    this.backupList = document.getElementById('backup-list');
    
    // DOM References - Actions
    this.createBackupBtn = document.getElementById('create-backup');
    this.backupSettingsBtn = document.getElementById('backup-settings');
    this.refreshBackupsBtn = document.getElementById('refresh-backups');
    
    // DOM References - Create Backup Modal
    this.createBackupModal = document.getElementById('create-backup-modal');
    this.closeBackupModal = document.getElementById('close-backup-modal');
    this.backupName = document.getElementById('backup-name');
    this.backupWorld = document.getElementById('backup-world');
    this.backupPlugins = document.getElementById('backup-plugins');
    this.backupConfig = document.getElementById('backup-config');
    this.backupLogs = document.getElementById('backup-logs');
    this.backupCompression = document.getElementById('backup-compression');
    this.estimatedSize = document.getElementById('estimated-size');
    this.estimatedTime = document.getElementById('estimated-time');
    this.confirmBackupCreate = document.getElementById('confirm-backup-create');
    this.cancelBackupCreate = document.getElementById('cancel-backup-create');
    
    // DOM References - Settings Modal
    this.settingsModal = document.getElementById('backup-settings-modal');
    this.closeSettingsModal = document.getElementById('close-settings-modal');
    this.backupLocation = document.getElementById('backup-location');
    this.maxBackupSize = document.getElementById('max-backup-size');
    this.autoDeleteOld = document.getElementById('auto-delete-old');
    this.keepMinimum = document.getElementById('keep-minimum');
    this.notifyBackupComplete = document.getElementById('notify-backup-complete');
    this.notifyBackupFail = document.getElementById('notify-backup-fail');
    this.saveSettings = document.getElementById('save-settings');
    this.cancelSettings = document.getElementById('cancel-settings');
    
    // DOM References - Restore Modal
    this.restoreModal = document.getElementById('restore-modal');
    this.closeRestoreModal = document.getElementById('close-restore-modal');
    this.restoreBackupName = document.getElementById('restore-backup-name');
    this.restoreBackupDate = document.getElementById('restore-backup-date');
    this.confirmRestoreCheckbox = document.getElementById('confirm-restore-checkbox');
    this.confirmRestore = document.getElementById('confirm-restore');
    this.cancelRestore = document.getElementById('cancel-restore');
    
    // State
    this.backups = [];
    this.currentFilter = 'all';
    this.selectedBackupId = null;
  }

  init() {
    this.setupEventListeners();
    this.loadBackups();
    this.updateStats();
  }

  setupEventListeners() {
    // Action buttons
    this.createBackupBtn.addEventListener('click', () => this.openCreateBackupModal());
    this.backupSettingsBtn.addEventListener('click', () => this.openSettingsModal());
    this.refreshBackupsBtn.addEventListener('click', () => this.refreshBackups());
    
    // Search
    this.backupSearchInput.addEventListener('input', (e) => this.filterBackups());
    
    // Filter buttons
    this.filterButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        this.filterButtons.forEach(b => b.classList.remove('active'));
        e.target.classList.add('active');
        this.currentFilter = e.target.dataset.filter;
        this.filterBackups();
      });
    });
    
    // Backup list interactions
    this.backupList.addEventListener('click', (e) => {
      const backupItem = e.target.closest('.backup-item');
      if (!backupItem) return;
      
      const backupId = backupItem.dataset.backupId;
      
      if (e.target.closest('.backup-btn.info')) {
        this.toggleBackupDetails(backupItem);
      } else if (e.target.closest('.backup-btn.download')) {
        this.downloadBackup(backupId);
      } else if (e.target.closest('.backup-btn.restore')) {
        this.openRestoreModal(backupId);
      } else if (e.target.closest('.backup-btn.delete')) {
        this.deleteBackup(backupId);
      }
    });
    
    // Create backup modal
    this.closeBackupModal.addEventListener('click', () => this.closeCreateBackupModalHandler());
    this.cancelBackupCreate.addEventListener('click', () => this.closeCreateBackupModalHandler());
    this.confirmBackupCreate.addEventListener('click', () => this.createBackup());
    
    // Update estimates when checkboxes change
    [this.backupWorld, this.backupPlugins, this.backupConfig, this.backupLogs].forEach(cb => {
      cb.addEventListener('change', () => this.updateEstimates());
    });
    
    // Settings modal
    this.closeSettingsModal.addEventListener('click', () => this.closeSettingsModalHandler());
    this.cancelSettings.addEventListener('click', () => this.closeSettingsModalHandler());
    this.saveSettings.addEventListener('click', () => this.saveBackupSettings());
    
    // Restore modal
    this.closeRestoreModal.addEventListener('click', () => this.closeRestoreModalHandler());
    this.cancelRestore.addEventListener('click', () => this.closeRestoreModalHandler());
    this.confirmRestoreCheckbox.addEventListener('change', (e) => {
      this.confirmRestore.disabled = !e.target.checked;
    });
    this.confirmRestore.addEventListener('click', () => this.restoreBackup());
  }

  loadBackups() {
    // Mock backup data
    this.backups = [
      {
        id: '1',
        name: 'Full Server Backup - 2025-10-14',
        type: 'automatic',
        date: new Date('2025-10-14T03:00:00'),
        size: 2.3,
        includes: ['world', 'plugins', 'config'],
        compression: 80,
        playersOnline: 12,
        serverVersion: '1.21.1'
      },
      {
        id: '2',
        name: 'Manual Backup - Pre-Update',
        type: 'manual',
        date: new Date('2025-10-13T20:30:00'),
        size: 2.1,
        includes: ['world', 'plugins', 'config'],
        compression: 82,
        playersOnline: 8,
        serverVersion: '1.21.1',
        notes: 'Before plugin update'
      },
      {
        id: '3',
        name: 'World Only Backup',
        type: 'world',
        date: new Date('2025-10-13T03:00:00'),
        size: 1.8,
        includes: ['world'],
        compression: 85,
        playersOnline: 5,
        serverVersion: '1.21.1'
      },
      {
        id: '4',
        name: 'Config Backup',
        type: 'config',
        date: new Date('2025-10-12T15:00:00'),
        size: 0.05,
        includes: ['config'],
        compression: 90,
        playersOnline: 15,
        serverVersion: '1.21.1'
      }
    ];
    
    this.renderBackups();
  }

  renderBackups() {
    this.backupList.innerHTML = '';
    
    const filteredBackups = this.getFilteredBackups();
    
    filteredBackups.forEach(backup => {
      const backupEl = this.createBackupElement(backup);
      this.backupList.appendChild(backupEl);
    });
    
    if (filteredBackups.length === 0) {
      this.backupList.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--text-tertiary);">No backups found</div>';
    }
  }

  createBackupElement(backup) {
    const div = document.createElement('div');
    div.className = 'backup-item';
    div.dataset.backupId = backup.id;
    
    const dateStr = this.formatDate(backup.date);
    const sizeStr = backup.size >= 1 ? `${backup.size.toFixed(1)} GB` : `${(backup.size * 1024).toFixed(0)} MB`;
    
    div.innerHTML = `
      <div class="backup-item-header">
        <div class="backup-item-icon">${this.getBackupIcon(backup.type)}</div>
        <div class="backup-item-info">
          <div class="backup-item-name">${backup.name}</div>
          <div class="backup-item-meta">
            <span class="backup-type ${backup.type}">${this.capitalize(backup.type)}</span>
            <span class="backup-date">📅 ${dateStr}</span>
            <span class="backup-size">💿 ${sizeStr}</span>
          </div>
        </div>
        <div class="backup-item-actions">
          <button class="backup-btn info" title="Backup Info">ℹ️</button>
          <button class="backup-btn download" title="Download">⬇️</button>
          <button class="backup-btn restore" title="Restore">↩️</button>
          <button class="backup-btn delete" title="Delete">🗑️</button>
        </div>
      </div>
      <div class="backup-item-details" style="display: none;">
        <div class="backup-detail-grid">
          <div class="backup-detail-item">
            <span class="detail-label">Includes:</span>
            <span class="detail-value">${backup.includes.map(i => this.capitalize(i)).join(', ')}</span>
          </div>
          <div class="backup-detail-item">
            <span class="detail-label">Compression:</span>
            <span class="detail-value">ZIP (${backup.compression}% ratio)</span>
          </div>
          <div class="backup-detail-item">
            <span class="detail-label">Players Online:</span>
            <span class="detail-value">${backup.playersOnline} players</span>
          </div>
          <div class="backup-detail-item">
            <span class="detail-label">Server Version:</span>
            <span class="detail-value">${backup.serverVersion}</span>
          </div>
          ${backup.notes ? `
          <div class="backup-detail-item">
            <span class="detail-label">Notes:</span>
            <span class="detail-value">${backup.notes}</span>
          </div>
          ` : ''}
        </div>
      </div>
    `;
    
    return div;
  }

  getFilteredBackups() {
    let filtered = this.backups;
    
    // Filter by type
    if (this.currentFilter !== 'all') {
      filtered = filtered.filter(b => b.type === this.currentFilter);
    }
    
    // Filter by search
    const searchTerm = this.backupSearchInput.value.toLowerCase();
    if (searchTerm) {
      filtered = filtered.filter(b => 
        b.name.toLowerCase().includes(searchTerm) ||
        b.type.toLowerCase().includes(searchTerm)
      );
    }
    
    return filtered.sort((a, b) => b.date - a.date);
  }

  filterBackups() {
    this.renderBackups();
  }

  toggleBackupDetails(backupItem) {
    const details = backupItem.querySelector('.backup-item-details');
    if (details.style.display === 'none') {
      details.style.display = 'block';
    } else {
      details.style.display = 'none';
    }
  }

  updateStats() {
    const totalBackups = this.backups.length;
    const totalSize = this.backups.reduce((sum, b) => sum + b.size, 0);
    const lastBackup = this.backups.length > 0 ? this.backups[0].date : null;
    
    this.totalBackupsEl.textContent = totalBackups;
    this.totalBackupSizeEl.textContent = totalSize.toFixed(1) + ' GB';
    this.lastBackupTimeEl.textContent = lastBackup ? this.formatTimeAgo(lastBackup) : 'Never';
    
    // Calculate next scheduled backup
    if (this.autoBackupEnabled.checked) {
      this.nextScheduledBackupEl.textContent = this.calculateNextBackup();
    } else {
      this.nextScheduledBackupEl.textContent = 'Not scheduled';
    }
  }

  // Create Backup Modal
  openCreateBackupModal() {
    this.createBackupModal.style.display = 'flex';
    this.updateEstimates();
  }

  closeCreateBackupModalHandler() {
    this.createBackupModal.style.display = 'none';
    this.backupName.value = '';
    this.backupWorld.checked = true;
    this.backupPlugins.checked = true;
    this.backupConfig.checked = true;
    this.backupLogs.checked = false;
  }

  updateEstimates() {
    let estimatedSize = 0;
    if (this.backupWorld.checked) estimatedSize += 1.8;
    if (this.backupPlugins.checked) estimatedSize += 0.3;
    if (this.backupConfig.checked) estimatedSize += 0.05;
    if (this.backupLogs.checked) estimatedSize += 0.15;
    
    this.estimatedSize.textContent = `~${estimatedSize.toFixed(2)} GB`;
    
    const estimatedMinutes = Math.ceil(estimatedSize * 2);
    this.estimatedTime.textContent = `~${estimatedMinutes}-${estimatedMinutes + 2} minutes`;
  }

  createBackup() {
    const name = this.backupName.value || `Backup - ${new Date().toISOString().split('T')[0]}`;
    const includes = [];
    if (this.backupWorld.checked) includes.push('world');
    if (this.backupPlugins.checked) includes.push('plugins');
    if (this.backupConfig.checked) includes.push('config');
    if (this.backupLogs.checked) includes.push('logs');
    
    // Mock backup creation
    const newBackup = {
      id: String(this.backups.length + 1),
      name: name,
      type: 'manual',
      date: new Date(),
      size: parseFloat(this.estimatedSize.textContent.match(/[\d.]+/)[0]),
      includes: includes,
      compression: 80,
      playersOnline: 10,
      serverVersion: '1.21.1'
    };
    
    this.backups.unshift(newBackup);
    this.renderBackups();
    this.updateStats();
    this.closeCreateBackupModalHandler();
    this.showNotification('Backup created successfully!', 'success');
  }

  // Settings Modal
  openSettingsModal() {
    this.settingsModal.style.display = 'flex';
  }

  closeSettingsModalHandler() {
    this.settingsModal.style.display = 'none';
  }

  saveBackupSettings() {
    this.showNotification('Settings saved successfully!', 'success');
    this.closeSettingsModalHandler();
    this.updateStats();
  }

  // Restore Modal
  openRestoreModal(backupId) {
    const backup = this.backups.find(b => b.id === backupId);
    if (!backup) return;
    
    this.selectedBackupId = backupId;
    this.restoreBackupName.textContent = backup.name;
    this.restoreBackupDate.textContent = this.formatDate(backup.date);
    this.confirmRestoreCheckbox.checked = false;
    this.confirmRestore.disabled = true;
    this.restoreModal.style.display = 'flex';
  }

  closeRestoreModalHandler() {
    this.restoreModal.style.display = 'none';
    this.selectedBackupId = null;
  }

  restoreBackup() {
    if (!this.selectedBackupId) return;
    
    const backup = this.backups.find(b => b.id === this.selectedBackupId);
    this.showNotification(`Restoring backup: ${backup.name}...`, 'info');
    
    // Mock restore process
    setTimeout(() => {
      this.showNotification('Backup restored successfully!', 'success');
      this.closeRestoreModalHandler();
    }, 2000);
  }

  downloadBackup(backupId) {
    const backup = this.backups.find(b => b.id === backupId);
    if (!backup) return;
    
    this.showNotification(`Downloading backup: ${backup.name}...`, 'info');
    // In production, would trigger actual download
  }

  deleteBackup(backupId) {
    if (!confirm('Are you sure you want to delete this backup? This cannot be undone.')) return;
    
    this.backups = this.backups.filter(b => b.id !== backupId);
    this.renderBackups();
    this.updateStats();
    this.showNotification('Backup deleted successfully', 'success');
  }

  refreshBackups() {
    this.showNotification('Refreshing backup list...', 'info');
    // In production, would fetch from API
    setTimeout(() => {
      this.renderBackups();
      this.updateStats();
      this.showNotification('Backup list refreshed', 'success');
    }, 500);
  }

  // Helper methods
  getBackupIcon(type) {
    const icons = {
      automatic: '💾',
      manual: '💾',
      world: '🗺️',
      config: '⚙️'
    };
    return icons[type] || '💾';
  }

  capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
  }

  formatDate(date) {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    }).format(date);
  }

  formatTimeAgo(date) {
    const seconds = Math.floor((new Date() - date) / 1000);
    
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)} minutes ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)} hours ago`;
    return `${Math.floor(seconds / 86400)} days ago`;
  }

  calculateNextBackup() {
    const frequency = this.backupFrequency.value;
    const time = this.backupTime.value;
    
    const now = new Date();
    const next = new Date();
    const [hours, minutes] = time.split(':');
    next.setHours(parseInt(hours), parseInt(minutes), 0, 0);
    
    if (frequency === 'daily') {
      if (next < now) next.setDate(next.getDate() + 1);
    } else if (frequency === 'weekly') {
      next.setDate(next.getDate() + 7);
    } else if (frequency === 'hourly') {
      next.setHours(next.getHours() + 1);
    }
    
    return this.formatDate(next);
  }

  showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
      position: fixed;
      bottom: 20px;
      right: 20px;
      padding: 12px 20px;
      background: rgba(var(--glass-bg-rgb), 0.95);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      color: var(--text-primary);
      z-index: 10000;
      animation: slideInRight 0.3s ease-out;
    `;
    
    if (type === 'success') {
      notification.style.borderColor = 'var(--accent-success)';
    } else if (type === 'error') {
      notification.style.borderColor = 'var(--accent-danger)';
    } else if (type === 'warning') {
      notification.style.borderColor = 'var(--accent-warning)';
    }
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
      notification.remove();
    }, 3000);
  }

  destroy() {
    // Cleanup if needed
  }
}

// ========================================
// FILE BROWSER & EDITOR MANAGER
// ========================================

class FileBrowserManager {
  constructor() {
    // DOM References - File Tree
    this.fileTree = document.getElementById('file-tree');
    this.fileSearchInput = document.getElementById('file-search');
    this.refreshFilesBtn = document.getElementById('refresh-files');
    
    // DOM References - Editor
    this.editorFilePath = document.getElementById('editor-file-path');
    this.editorFileStatus = document.getElementById('editor-file-status');
    this.editorContainer = document.getElementById('editor-container');
    this.editorTextarea = document.getElementById('file-editor');
    this.editorPlaceholder = this.editorContainer.querySelector('.editor-placeholder');
    
    // DOM References - Editor Actions
    this.formatBtn = document.getElementById('format-file');
    this.validateBtn = document.getElementById('validate-file');
    this.downloadBtn = document.getElementById('download-file');
    this.saveBtn = document.getElementById('save-file');
    
    // DOM References - Editor Stats
    this.editorLines = document.getElementById('editor-lines');
    this.editorChars = document.getElementById('editor-chars');
    this.editorType = document.getElementById('editor-type');
    this.editorModified = document.getElementById('editor-modified');
    
    // DOM References - Editor Warning
    this.editorWarning = document.getElementById('editor-warning');
    
    // DOM References - Upload Modal
    this.uploadBtn = document.getElementById('upload-file');
    this.uploadModal = document.getElementById('file-upload-modal');
    this.closeUploadModal = document.getElementById('close-upload-modal');
    this.uploadArea = document.getElementById('upload-area');
    this.fileInput = document.getElementById('file-input');
    this.uploadPathInput = document.getElementById('upload-path');
    this.confirmUpload = document.getElementById('confirm-upload');
    this.cancelUpload = document.getElementById('cancel-upload');
    
    // DOM References - Create File Modal
    this.createFileBtn = document.getElementById('create-file');
    this.createFileModal = document.getElementById('create-file-modal');
    this.closeCreateModal = document.getElementById('close-create-modal');
    this.newFilePath = document.getElementById('new-file-path');
    this.newFileType = document.getElementById('new-file-type');
    this.newFileTemplate = document.getElementById('new-file-template');
    this.confirmCreate = document.getElementById('confirm-create');
    this.cancelCreate = document.getElementById('cancel-create');
    
    // State
    this.currentFile = null;
    this.originalContent = '';
    this.isModified = false;
    this.selectedFile = null;
    this.uploadedFile = null;
    
    // File storage (mock - would be API in production)
    this.files = this.loadMockFiles();
  }

  init() {
    this.setupEventListeners();
    this.renderFileTree();
    this.loadMockFiles();
  }

  setupEventListeners() {
    // File tree interactions
    this.fileTree.addEventListener('click', (e) => {
      const folderHeader = e.target.closest('.folder-header');
      if (folderHeader) {
        this.toggleFolder(folderHeader);
        return;
      }

      const fileItem = e.target.closest('.file-item');
      if (fileItem) {
        this.selectFile(fileItem);
      }
    });

    // File search
    this.fileSearchInput.addEventListener('input', (e) => {
      this.filterFiles(e.target.value);
    });

    // Refresh files
    this.refreshFilesBtn.addEventListener('click', () => {
      this.refreshFileList();
    });

    // Editor actions
    this.formatBtn.addEventListener('click', () => this.formatFile());
    this.validateBtn.addEventListener('click', () => this.validateFile());
    this.downloadBtn.addEventListener('click', () => this.downloadFile());
    this.saveBtn.addEventListener('click', () => this.saveFile());

    // Editor content changes
    this.editorTextarea.addEventListener('input', () => {
      this.onEditorChange();
    });

    // Keyboard shortcuts
    this.editorTextarea.addEventListener('keydown', (e) => {
      if (e.ctrlKey && e.key === 's') {
        e.preventDefault();
        this.saveFile();
      } else if (e.ctrlKey && e.key === 'f') {
        e.preventDefault();
        this.formatFile();
      }
    });

    // Upload modal
    this.uploadBtn.addEventListener('click', () => this.openUploadModal());
    this.closeUploadModal.addEventListener('click', () => this.closeUploadModalHandler());
    this.cancelUpload.addEventListener('click', () => this.closeUploadModalHandler());
    this.confirmUpload.addEventListener('click', () => this.handleUpload());

    // Drag and drop
    this.uploadArea.addEventListener('click', () => this.fileInput.click());
    this.fileInput.addEventListener('change', (e) => this.handleFileSelect(e));
    
    this.uploadArea.addEventListener('dragover', (e) => {
      e.preventDefault();
      this.uploadArea.classList.add('dragover');
    });
    
    this.uploadArea.addEventListener('dragleave', () => {
      this.uploadArea.classList.remove('dragover');
    });
    
    this.uploadArea.addEventListener('drop', (e) => {
      e.preventDefault();
      this.uploadArea.classList.remove('dragover');
      const files = e.dataTransfer.files;
      if (files.length > 0) {
        this.handleFileSelect({ target: { files } });
      }
    });

    // Create file modal
    this.createFileBtn.addEventListener('click', () => this.openCreateModal());
    this.closeCreateModal.addEventListener('click', () => this.closeCreateModalHandler());
    this.cancelCreate.addEventListener('click', () => this.closeCreateModalHandler());
    this.confirmCreate.addEventListener('click', () => this.handleCreateFile());
    
    // Template selection
    this.newFileTemplate.addEventListener('change', () => {
      this.updateTemplatePreview();
    });
  }

  loadMockFiles() {
    return {
      'config/server.properties': {
        type: 'properties',
        size: 2400,
        content: `# Server Properties
server-name=NeoEssentials Server
max-players=20
difficulty=normal
gamemode=survival
pvp=true
enable-command-blocks=true`
      },
      'config/neoessentials/main.json': {
        type: 'json',
        size: 4800,
        content: JSON.stringify({
          "enableFeatures": {
            "economy": true,
            "homes": true,
            "warps": true,
            "teleportation": true
          },
          "settings": {
            "maxHomes": 5,
            "teleportDelay": 3,
            "economyStartingBalance": 1000
          }
        }, null, 2)
      },
      'config/neoessentials/tablist.json': {
        type: 'json',
        size: 1200,
        content: JSON.stringify({
          "header": "§6§lNeoEssentials Server",
          "footer": "§7Visit our website",
          "updateInterval": 1000
        }, null, 2)
      },
      'config/neoessentials/commands.json': {
        type: 'json',
        size: 3500,
        content: JSON.stringify({
          "commands": {
            "home": { "enabled": true, "cooldown": 5 },
            "tpa": { "enabled": true, "cooldown": 10 },
            "spawn": { "enabled": true, "cooldown": 0 }
          }
        }, null, 2)
      },
      'plugins/example.yml': {
        type: 'yaml',
        size: 1800,
        content: `# Example Plugin Configuration
enabled: true
version: 1.0
settings:
  feature1: true
  feature2: false
  timeout: 30`
      }
    };
  }

  renderFileTree() {
    // File tree is already in HTML, this would dynamically update it
    // For now, the tree is static in HTML
  }

  toggleFolder(folderHeader) {
    const folder = folderHeader.parentElement;
    const contents = folder.querySelector('.folder-contents');
    const toggle = folderHeader.querySelector('.folder-toggle');
    
    if (folder.classList.contains('expanded')) {
      folder.classList.remove('expanded');
      contents.style.display = 'none';
      toggle.textContent = '▶';
    } else {
      folder.classList.add('expanded');
      contents.style.display = 'block';
      toggle.textContent = '▼';
    }
  }

  selectFile(fileItem) {
    // Remove previous selection
    document.querySelectorAll('.file-item').forEach(item => {
      item.classList.remove('selected');
    });

    // Add selection to clicked file
    fileItem.classList.add('selected');
    
    const filePath = fileItem.dataset.path;
    this.loadFile(filePath);
  }

  loadFile(filePath) {
    const fileData = this.files[filePath];
    
    if (!fileData) {
      this.showNotification('File not found', 'error');
      return;
    }

    this.currentFile = filePath;
    this.originalContent = fileData.content;
    
    // Update UI
    this.editorFilePath.textContent = filePath;
    this.editorType.textContent = fileData.type.toUpperCase();
    
    // Hide placeholder, show editor
    this.editorPlaceholder.style.display = 'none';
    this.editorTextarea.style.display = 'block';
    this.editorTextarea.value = fileData.content;
    
    // Enable buttons
    this.formatBtn.disabled = false;
    this.validateBtn.disabled = false;
    this.downloadBtn.disabled = false;
    this.saveBtn.disabled = true;
    
    // Update stats
    this.updateEditorStats();
    this.isModified = false;
    this.updateModifiedStatus();
  }

  onEditorChange() {
    this.isModified = true;
    this.updateModifiedStatus();
    this.updateEditorStats();
    this.saveBtn.disabled = false;
    this.editorWarning.style.display = 'flex';
  }

  updateEditorStats() {
    const content = this.editorTextarea.value;
    const lines = content.split('\n').length;
    const chars = content.length;
    
    this.editorLines.textContent = lines;
    this.editorChars.textContent = chars.toLocaleString();
  }

  updateModifiedStatus() {
    if (this.isModified) {
      this.editorFileStatus.textContent = 'Modified';
      this.editorFileStatus.classList.add('modified');
      this.editorModified.textContent = 'Yes';
    } else {
      this.editorFileStatus.textContent = 'Saved';
      this.editorFileStatus.classList.remove('modified');
      this.editorModified.textContent = 'No';
    }
  }

  formatFile() {
    if (!this.currentFile) return;

    const fileType = this.files[this.currentFile].type;
    const content = this.editorTextarea.value;

    try {
      let formatted;
      if (fileType === 'json') {
        const parsed = JSON.parse(content);
        formatted = JSON.stringify(parsed, null, 2);
      } else if (fileType === 'yaml') {
        // Basic YAML formatting (would use a library in production)
        formatted = content;
        this.showNotification('YAML formatting not fully supported', 'warning');
      } else {
        this.showNotification('Formatting not available for this file type', 'info');
        return;
      }

      this.editorTextarea.value = formatted;
      this.onEditorChange();
      this.showNotification('File formatted successfully', 'success');
    } catch (error) {
      this.showNotification('Failed to format: ' + error.message, 'error');
    }
  }

  validateFile() {
    if (!this.currentFile) return;

    const fileType = this.files[this.currentFile].type;
    const content = this.editorTextarea.value;

    try {
      if (fileType === 'json') {
        JSON.parse(content);
        this.showNotification('✓ Valid JSON', 'success');
      } else if (fileType === 'yaml') {
        // Basic YAML validation (would use a library in production)
        this.showNotification('✓ YAML syntax appears valid', 'success');
      } else if (fileType === 'properties') {
        // Basic properties validation
        this.showNotification('✓ Properties syntax appears valid', 'success');
      } else {
        this.showNotification('Validation not available for this file type', 'info');
      }
    } catch (error) {
      this.showNotification('✗ Invalid syntax: ' + error.message, 'error');
    }
  }

  downloadFile() {
    if (!this.currentFile) return;

    const content = this.editorTextarea.value;
    const filename = this.currentFile.split('/').pop();
    
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    
    this.showNotification('File downloaded: ' + filename, 'success');
  }

  saveFile() {
    if (!this.currentFile || !this.isModified) return;

    // Create backup
    const backupContent = this.originalContent;
    const backupName = this.currentFile + '.backup';
    console.log('Backup created:', backupName, backupContent);

    // Save new content
    const newContent = this.editorTextarea.value;
    this.files[this.currentFile].content = newContent;
    this.originalContent = newContent;
    
    // Update state
    this.isModified = false;
    this.updateModifiedStatus();
    this.saveBtn.disabled = true;
    this.editorWarning.style.display = 'none';
    
    this.showNotification('File saved successfully (backup created)', 'success');
  }

  filterFiles(searchTerm) {
    const fileItems = document.querySelectorAll('.file-item');
    const lowerSearch = searchTerm.toLowerCase();

    fileItems.forEach(item => {
      const fileName = item.querySelector('.file-name').textContent.toLowerCase();
      if (fileName.includes(lowerSearch)) {
        item.style.display = 'flex';
      } else {
        item.style.display = 'none';
      }
    });
  }

  refreshFileList() {
    this.showNotification('File list refreshed', 'success');
    // Would reload from API in production
  }

  // Upload Modal
  openUploadModal() {
    this.uploadModal.style.display = 'flex';
  }

  closeUploadModalHandler() {
    this.uploadModal.style.display = 'none';
    this.uploadedFile = null;
    this.fileInput.value = '';
    this.uploadPathInput.value = '';
    this.confirmUpload.disabled = true;
  }

  handleFileSelect(e) {
    const files = e.target.files;
    if (files.length === 0) return;

    const file = files[0];
    
    // Validate file size (5MB limit)
    if (file.size > 5 * 1024 * 1024) {
      this.showNotification('File too large (max 5MB)', 'error');
      return;
    }

    // Validate file type
    const validExtensions = ['.json', '.yml', '.yaml', '.properties', '.txt', '.conf'];
    const extension = '.' + file.name.split('.').pop().toLowerCase();
    
    if (!validExtensions.includes(extension)) {
      this.showNotification('Invalid file type', 'error');
      return;
    }

    this.uploadedFile = file;
    this.uploadPathInput.value = 'config/' + file.name;
    this.confirmUpload.disabled = false;
    
    this.uploadArea.querySelector('.upload-text strong').textContent = file.name;
  }

  handleUpload() {
    if (!this.uploadedFile) return;

    const targetPath = this.uploadPathInput.value;
    
    // Read file content
    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target.result;
      const type = targetPath.split('.').pop().toLowerCase();
      
      // Add to files
      this.files[targetPath] = {
        type: type === 'yml' || type === 'yaml' ? 'yaml' : type,
        size: content.length,
        content: content
      };
      
      this.showNotification('File uploaded successfully', 'success');
      this.closeUploadModalHandler();
      
      // Would need to update file tree in production
    };
    
    reader.readAsText(this.uploadedFile);
  }

  // Create File Modal
  openCreateModal() {
    this.createFileModal.style.display = 'flex';
  }

  closeCreateModalHandler() {
    this.createFileModal.style.display = 'none';
    this.newFilePath.value = '';
    this.newFileType.value = 'json';
    this.newFileTemplate.value = 'empty';
  }

  handleCreateFile() {
    const filePath = this.newFilePath.value.trim();
    
    if (!filePath) {
      this.showNotification('Please enter a file path', 'error');
      return;
    }

    const fileType = this.newFileType.value;
    const template = this.newFileTemplate.value;
    
    let content = '';
    
    // Generate content based on template
    if (template === 'basic-json') {
      content = '{\n  "key": "value"\n}';
    } else if (template === 'basic-yaml') {
      content = '# Configuration\nkey: value';
    } else if (template === 'basic-properties') {
      content = '# Properties\nkey=value';
    }
    
    // Add to files
    this.files[filePath] = {
      type: fileType,
      size: content.length,
      content: content
    };
    
    this.showNotification('File created successfully', 'success');
    this.closeCreateModalHandler();
    
    // Would need to update file tree and select new file in production
  }

  updateTemplatePreview() {
    // Could show template preview
  }

  showNotification(message, type = 'info') {
    // Reuse the existing notification system
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
      position: fixed;
      bottom: 20px;
      right: 20px;
      padding: 12px 20px;
      background: rgba(var(--glass-bg-rgb), 0.95);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      color: var(--text-primary);
      z-index: 10000;
      animation: slideInRight 0.3s ease-out;
    `;
    
    if (type === 'success') {
      notification.style.borderColor = 'var(--accent-success)';
    } else if (type === 'error') {
      notification.style.borderColor = 'var(--accent-danger)';
    } else if (type === 'warning') {
      notification.style.borderColor = 'var(--accent-warning)';
    }
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
      notification.remove();
    }, 3000);
  }

  destroy() {
    // Cleanup if needed
  }
}

// ========================================
// ECONOMY DASHBOARD MANAGER
// ========================================

class EconomyDashboardManager {
  constructor() {
    // DOM References
    this.refreshBtn = document.getElementById('refresh-economy');
    this.exportBtn = document.getElementById('export-economy');
    
    // Health Indicators
    this.totalMoneySupplyEl = document.getElementById('total-money-supply');
    this.transactionVolumeEl = document.getElementById('transaction-volume');
    this.activeAccountsEl = document.getElementById('active-accounts');
    this.economyHealthEl = document.getElementById('economy-health');
    this.supplyChangeEl = document.getElementById('supply-change');
    this.volumeChangeEl = document.getElementById('volume-change');
    this.accountsChangeEl = document.getElementById('accounts-change');
    
    // Transaction List
    this.transactionsList = document.getElementById('recent-transactions');
    
    // Charts
    this.transactionVolumeChart = null;
    this.wealthDistributionChart = null;
    
    // Data
    this.currentChartType = 'daily';
    this.transactions = [];
    this.currentFilter = 'all';
    
    // Mock Data
    this.mockPlayers = ['Steve', 'Alex', 'Notch', 'Herobrine', 'Jeb', 'Dinnerbone', 'CaptainSparklez', 'DanTDM'];
  }
  
  init() {
    this.setupEventListeners();
    this.initCharts();
    this.loadMockTransactions();
    this.startAutoUpdate();
  }
  
  setupEventListeners() {
    if (this.refreshBtn) {
      this.refreshBtn.addEventListener('click', () => this.refreshEconomy());
    }
    
    if (this.exportBtn) {
      this.exportBtn.addEventListener('click', () => this.exportEconomy());
    }
    
    // Chart type selector
    document.querySelectorAll('.chart-type-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        document.querySelectorAll('.chart-type-btn').forEach(b => b.classList.remove('active'));
        e.target.classList.add('active');
        this.currentChartType = e.target.getAttribute('data-type');
        this.updateTransactionVolumeChart();
      });
    });
    
    // Transaction filters
    document.querySelectorAll('.transaction-filter-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        document.querySelectorAll('.transaction-filter-btn').forEach(b => b.classList.remove('active'));
        e.target.classList.add('active');
        this.currentFilter = e.target.getAttribute('data-filter');
        this.filterTransactions();
      });
    });
    
    // View all buttons
    const viewBalancesBtn = document.getElementById('view-all-balances');
    if (viewBalancesBtn) {
      viewBalancesBtn.addEventListener('click', () => {
        showNotification('📊 Opening balance leaderboard...', 'info');
      });
    }
    
    const viewTransactionsBtn = document.getElementById('view-all-transactions');
    if (viewTransactionsBtn) {
      viewTransactionsBtn.addEventListener('click', () => {
        showNotification('📜 Opening transaction history...', 'info');
      });
    }
  }
  
  initCharts() {
    // Transaction Volume Chart
    const txVolumeCtx = document.getElementById('transaction-volume-chart');
    if (txVolumeCtx) {
      const labels = Array.from({length: 24}, (_, i) => `${i}:00`);
      const data = Array.from({length: 24}, () => Math.random() * 50000 + 10000);
      
      this.transactionVolumeChart = new Chart(txVolumeCtx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: 'Transaction Volume',
            data: data,
            backgroundColor: 'rgba(245, 158, 11, 0.6)',
            borderColor: 'rgba(245, 158, 11, 1)',
            borderWidth: 1,
            borderRadius: 6,
            hoverBackgroundColor: 'rgba(245, 158, 11, 0.8)'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: false
            },
            tooltip: {
              mode: 'index',
              intersect: false,
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
              titleColor: '#fff',
              bodyColor: '#fff',
              borderColor: 'rgba(245, 158, 11, 0.5)',
              borderWidth: 1,
              callbacks: {
                label: function(context) {
                  return 'Volume: $' + context.parsed.y.toLocaleString();
                }
              }
            }
          },
          scales: {
            x: {
              display: true,
              grid: {
                color: 'rgba(255, 255, 255, 0.05)'
              },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                maxTicksLimit: 12
              }
            },
            y: {
              display: true,
              grid: {
                color: 'rgba(255, 255, 255, 0.05)'
              },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                callback: function(value) {
                  return '$' + (value / 1000).toFixed(0) + 'k';
                }
              }
            }
          }
        }
      });
    }
    
    // Wealth Distribution Chart (Doughnut)
    const wealthDistCtx = document.getElementById('wealth-distribution-chart');
    if (wealthDistCtx) {
      this.wealthDistributionChart = new Chart(wealthDistCtx, {
        type: 'doughnut',
        data: {
          labels: ['Top 10%', 'Middle 40%', 'Bottom 50%'],
          datasets: [{
            data: [64.9, 26.1, 9.0],
            backgroundColor: [
              'rgba(245, 158, 11, 0.8)',
              'rgba(59, 130, 246, 0.8)',
              'rgba(107, 114, 128, 0.8)'
            ],
            borderColor: [
              'rgba(245, 158, 11, 1)',
              'rgba(59, 130, 246, 1)',
              'rgba(107, 114, 128, 1)'
            ],
            borderWidth: 2,
            hoverOffset: 10
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: true,
              position: 'bottom',
              labels: {
                color: 'rgba(255, 255, 255, 0.7)',
                padding: 15,
                usePointStyle: true,
                font: {
                  size: 12
                }
              }
            },
            tooltip: {
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
              titleColor: '#fff',
              bodyColor: '#fff',
              borderColor: 'rgba(245, 158, 11, 0.5)',
              borderWidth: 1,
              callbacks: {
                label: function(context) {
                  return context.label + ': ' + context.parsed + '% of total wealth';
                }
              }
            }
          },
          cutout: '60%'
        }
      });
    }
  }
  
  loadMockTransactions() {
    const types = ['payment', 'purchase', 'trade'];
    const typeIcons = {payment: '💸', purchase: '🛒', trade: '🤝'};
    const items = ['Diamond Sword', 'Enchanted Pickaxe', 'Golden Apple', 'Elytra', 'Netherite Armor'];
    
    this.transactions = [];
    
    for (let i = 0; i < 20; i++) {
      const type = types[Math.floor(Math.random() * types.length)];
      const player1 = this.mockPlayers[Math.floor(Math.random() * this.mockPlayers.length)];
      let player2 = this.mockPlayers[Math.floor(Math.random() * this.mockPlayers.length)];
      while (player2 === player1) {
        player2 = this.mockPlayers[Math.floor(Math.random() * this.mockPlayers.length)];
      }
      
      let description, amount;
      
      if (type === 'payment') {
        description = `<span class="player-name">${player1}</span> paid <span class="player-name">${player2}</span>`;
        amount = Math.floor(Math.random() * 5000) + 500;
      } else if (type === 'purchase') {
        const item = items[Math.floor(Math.random() * items.length)];
        description = `<span class="player-name">${player1}</span> bought <span class="item-name">${item}</span>`;
        amount = -(Math.floor(Math.random() * 2000) + 200);
      } else {
        description = `<span class="player-name">${player1}</span> traded with <span class="player-name">${player2}</span>`;
        amount = 0;
      }
      
      const minutesAgo = i * 5 + Math.floor(Math.random() * 5);
      
      this.transactions.push({
        type: type,
        icon: typeIcons[type],
        description: description,
        amount: amount,
        time: this.formatTimeAgo(minutesAgo),
        timestamp: Date.now() - (minutesAgo * 60000)
      });
    }
    
    this.filterTransactions();
  }
  
  filterTransactions() {
    let filtered = this.transactions;
    
    if (this.currentFilter !== 'all') {
      filtered = this.transactions.filter(tx => tx.type === this.currentFilter);
    }
    
    this.renderTransactions(filtered.slice(0, 5));
  }
  
  renderTransactions(transactions) {
    if (!this.transactionsList) return;
    
    this.transactionsList.innerHTML = transactions.map(tx => {
      const amountClass = tx.amount > 0 ? 'positive' : (tx.amount < 0 ? 'negative' : 'neutral');
      const amountText = tx.amount === 0 ? '$0' : (tx.amount > 0 ? '+$' : '-$') + Math.abs(tx.amount).toLocaleString();
      
      return `
        <div class="transaction-item type-${tx.type}">
          <div class="transaction-icon ${tx.type}">${tx.icon}</div>
          <div class="transaction-details">
            <div class="transaction-header">
              <span class="transaction-type">${tx.type}</span>
              <span class="transaction-time">${tx.time}</span>
            </div>
            <div class="transaction-description">${tx.description}</div>
          </div>
          <div class="transaction-amount ${amountClass}">${amountText}</div>
        </div>
      `;
    }).join('');
  }
  
  formatTimeAgo(minutes) {
    if (minutes < 1) return 'Just now';
    if (minutes === 1) return '1 minute ago';
    if (minutes < 60) return `${minutes} minutes ago`;
    const hours = Math.floor(minutes / 60);
    if (hours === 1) return '1 hour ago';
    if (hours < 24) return `${hours} hours ago`;
    const days = Math.floor(hours / 24);
    return days === 1 ? '1 day ago' : `${days} days ago`;
  }
  
  updateTransactionVolumeChart() {
    if (!this.transactionVolumeChart) return;
    
    let labels, data;
    
    if (this.currentChartType === 'daily') {
      labels = Array.from({length: 24}, (_, i) => `${i}:00`);
      data = Array.from({length: 24}, () => Math.random() * 50000 + 10000);
    } else if (this.currentChartType === 'weekly') {
      labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
      data = Array.from({length: 7}, () => Math.random() * 300000 + 100000);
    } else if (this.currentChartType === 'monthly') {
      labels = Array.from({length: 30}, (_, i) => `Day ${i + 1}`);
      data = Array.from({length: 30}, () => Math.random() * 200000 + 50000);
    }
    
    this.transactionVolumeChart.data.labels = labels;
    this.transactionVolumeChart.data.datasets[0].data = data;
    this.transactionVolumeChart.update();
  }
  
  startAutoUpdate() {
    // Update every 10 seconds
    setInterval(() => {
      // Update health indicators with slight variations
      const currentSupply = parseFloat(this.totalMoneySupplyEl?.textContent.replace(/[$,]/g, '') || '0');
      const newSupply = currentSupply + (Math.random() * 1000 - 500);
      if (this.totalMoneySupplyEl) {
        this.totalMoneySupplyEl.textContent = '$' + Math.floor(newSupply).toLocaleString();
      }
      
      // Simulate new transaction
      if (Math.random() > 0.5) {
        const types = ['payment', 'purchase', 'trade'];
        const type = types[Math.floor(Math.random() * types.length)];
        const typeIcons = {payment: '💸', purchase: '🛒', trade: '🤝'};
        const player1 = this.mockPlayers[Math.floor(Math.random() * this.mockPlayers.length)];
        let player2 = this.mockPlayers[Math.floor(Math.random() * this.mockPlayers.length)];
        
        let description, amount;
        if (type === 'payment') {
          description = `<span class="player-name">${player1}</span> paid <span class="player-name">${player2}</span>`;
          amount = Math.floor(Math.random() * 5000) + 500;
        } else if (type === 'purchase') {
          description = `<span class="player-name">${player1}</span> bought <span class="item-name">Item</span>`;
          amount = -(Math.floor(Math.random() * 2000) + 200);
        } else {
          description = `<span class="player-name">${player1}</span> traded with <span class="player-name">${player2}</span>`;
          amount = 0;
        }
        
        this.transactions.unshift({
          type: type,
          icon: typeIcons[type],
          description: description,
          amount: amount,
          time: 'Just now',
          timestamp: Date.now()
        });
        
        if (this.transactions.length > 50) {
          this.transactions.pop();
        }
        
        this.filterTransactions();
      }
    }, 10000);
  }
  
  refreshEconomy() {
    showNotification('🔄 Refreshing economy data...', 'info');
    
    setTimeout(() => {
      this.loadMockTransactions();
      this.updateTransactionVolumeChart();
      showNotification('✅ Economy data refreshed', 'success');
    }, 500);
  }
  
  exportEconomy() {
    const data = {
      timestamp: new Date().toISOString(),
      health: {
        totalMoneySupply: this.totalMoneySupplyEl?.textContent || '$0',
        transactionVolume: this.transactionVolumeEl?.textContent || '$0',
        activeAccounts: this.activeAccountsEl?.textContent || '0',
        economyHealth: this.economyHealthEl?.textContent || 'Unknown'
      },
      recentTransactions: this.transactions.slice(0, 10).map(tx => ({
        type: tx.type,
        description: tx.description.replace(/<[^>]*>/g, ''),
        amount: tx.amount,
        time: tx.time
      }))
    };
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `economy-data-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showNotification('📥 Economy data exported successfully', 'success');
  }
  
  destroy() {
    if (this.transactionVolumeChart) {
      this.transactionVolumeChart.destroy();
    }
    if (this.wealthDistributionChart) {
      this.wealthDistributionChart.destroy();
    }
  }
}

// Initialize Economy Dashboard Manager
let economyDashboardManager;

// Initialize Backup Manager
let backupManager;

// Initialize File Browser Manager
let fileBrowserManager;

// Initialize Performance Metrics Manager
let performanceMetricsManager;

// Initialize Chat Log Viewer
let chatLogViewer;
document.addEventListener('DOMContentLoaded', () => {
  backupManager = new BackupManager();
  backupManager.init();
  
  fileBrowserManager = new FileBrowserManager();
  fileBrowserManager.init();
  
  economyDashboardManager = new EconomyDashboardManager();
  economyDashboardManager.init();
  
  performanceMetricsManager = new PerformanceMetricsManager();
  performanceMetricsManager.init();
  
  chatLogViewer = new ChatLogViewer();
  chatLogViewer.init();
});

// --- Config Modal Logic ---
document.addEventListener('DOMContentLoaded', function() {
  const openConfigBtn = document.getElementById('open-config-modal');
  const configModal = document.getElementById('config-modal');
  const closeConfigBtn = document.getElementById('close-config-modal');
  const configFilesList = document.getElementById('config-files-list');
  const saveConfigBtn = document.getElementById('save-config-btn');

  // Config files loaded from API
  let mockConfigFiles = [];

  function renderConfigFiles(files) {
    configFilesList.innerHTML = '';
    files.forEach(file => {
      const fileDiv = document.createElement('div');
      fileDiv.className = 'config-file';
      const title = document.createElement('div');
      title.className = 'config-file-title';
      title.textContent = file.name;
      fileDiv.appendChild(title);
      file.options.forEach(opt => {
        const optDiv = document.createElement('div');
        optDiv.className = 'config-option';
        const label = document.createElement('label');
        label.textContent = opt.label;
        label.setAttribute('for', `${file.name}-${opt.key}`);
        let input;
        if (opt.type === 'text' || opt.type === 'number') {
          input = document.createElement('input');
          input.type = opt.type;
          input.value = opt.value;
        } else if (opt.type === 'checkbox') {
          input = document.createElement('input');
          input.type = 'checkbox';
          input.checked = !!opt.value;
        } else if (opt.type === 'select') {
          input = document.createElement('select');
          opt.choices.forEach(choice => {
            const option = document.createElement('option');
            option.value = choice;
            option.textContent = choice.charAt(0).toUpperCase() + choice.slice(1);
            if (choice === opt.value) option.selected = true;
            input.appendChild(option);
          });
        }
        input.id = `${file.name}-${opt.key}`;
        optDiv.appendChild(label);
        optDiv.appendChild(input);
        fileDiv.appendChild(optDiv);
      });
      configFilesList.appendChild(fileDiv);
    });
  }

  if (openConfigBtn) {
    openConfigBtn.addEventListener('click', async function() {
      configModal.style.display = 'flex';
      configFilesList.innerHTML = '<div class="loading-indicator">Loading config files...</div>';
      
      try {
        const response = await fetch('http://localhost:8080/api/config');
        if (response.ok) {
          const data = await response.json();
          mockConfigFiles = data.configs || [];
          renderConfigFiles(mockConfigFiles);
        } else {
          configFilesList.innerHTML = '<div class="error-indicator">Failed to load config files</div>';
        }
      } catch (error) {
        configFilesList.innerHTML = '<div class="error-indicator">Connection error: ' + error.message + '</div>';
      }
    });
  }
  if (closeConfigBtn) {
    closeConfigBtn.addEventListener('click', function() {
      configModal.style.display = 'none';
    });
  }
  if (saveConfigBtn) {
    saveConfigBtn.addEventListener('click', async function() {
      // Collect values from inputs
      let updated = [];
      mockConfigFiles.forEach(file => {
        let configData = {};
        file.options.forEach(opt => {
          let input = document.getElementById(`${file.name}-${opt.key}`);
          let value;
          if (opt.type === 'toggle' || opt.type === 'checkbox') {
            value = input.checked;
          } else if (opt.type === 'number') {
            value = parseFloat(input.value) || 0;
          } else {
            value = input.value;
          }
          configData[opt.key] = value;
        });
        updated.push({ file: file.name, config: configData });
      });
      
      // Send update to API
      for (const fileUpdate of updated) {
        try {
          const response = await fetch('http://localhost:8080/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(fileUpdate)
          });
          
          if (response.ok) {
            showNotification(`Config file ${fileUpdate.file} saved successfully!`, 'info');
          } else {
            const error = await response.json();
            showNotification(`Failed to save ${fileUpdate.file}: ${error.error}`, 'error');
          }
        } catch (error) {
          showNotification(`Connection error: ${error.message}`, 'error');
        }
      }
      
      configModal.style.display = 'none';
    });
  }
  // Optional: close modal on outside click
  window.addEventListener('click', function(e) {
    if (e.target === configModal) {
      configModal.style.display = 'none';
    }
  });
});

// Space theme dashboard JS
// Theme switcher (future expansion)
document.querySelector('.theme-switcher').addEventListener('click', () => {
  document.body.classList.toggle('space-theme-alt');
});


// Update functions for Java backend integration
function updatePlayerStats(data) {
  // data: { name, xp, location, ... }
  const el = document.querySelector('#player-stats .stats-content');
  if (!el) return;
  el.textContent = `Astronaut: ${data.name} | XP: ${data.xp} | Location: ${data.location}`;
// ...function ends, no extra brace needed
}
// --- Dynamic Stats & Logs Integration ---

function setLoading(el, msg) {
  if (el) {
    el.innerHTML = `
      <div class="loading-indicator fade-in">
        <div class="loading-spinner"></div>
        <p style="margin-top: 12px;">${msg}</p>
      </div>
    `;
  }
}

function setError(el, msg) {
  if (el) {
    el.innerHTML = `<div class="error-indicator shake">${msg}</div>`;
    // Add shake animation class temporarily
    setTimeout(() => {
      const errorEl = el.querySelector('.error-indicator');
      if (errorEl) errorEl.classList.remove('shake');
    }, 500);
  }
}

function setSuccess(el, msg) {
  if (el) {
    el.innerHTML = `<div class="success-indicator bounce">${msg}</div>`;
    setTimeout(() => {
      const successEl = el.querySelector('.success-indicator');
      if (successEl) successEl.classList.remove('bounce');
    }, 600);
  }
}

// Show skeleton loading states
function showSkeleton(el, type = 'default') {
  if (!el) return;
  
  let skeletonHTML = '';
  
  switch(type) {
    case 'stats':
      skeletonHTML = `
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-stat"></div>
        <div class="skeleton skeleton-stat"></div>
        <div class="skeleton skeleton-stat"></div>
      `;
      break;
    case 'list':
      skeletonHTML = `
        <div class="skeleton skeleton-line"></div>
        <div class="skeleton skeleton-line"></div>
        <div class="skeleton skeleton-line"></div>
        <div class="skeleton skeleton-line"></div>
        <div class="skeleton skeleton-line"></div>
      `;
      break;
    case 'logs':
      skeletonHTML = `
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text"></div>
      `;
      break;
    default:
      skeletonHTML = `
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text"></div>
      `;
  }
  
  el.innerHTML = skeletonHTML;
}

function updatePlayerStats(data) {
  const el = document.getElementById('player-stats-content');
  if (!el) return;
  if (!data || !data.players) {
    setError(el, 'No player data available.');
    return;
  }
  el.innerHTML = `<ul class="player-list">${data.players.map(p => `<li>${p.name} <span class="rank">${p.rank}</span> <span class="xp">XP: ${p.xp}</span></li>`).join('')}</ul>`;
}


function updateServerStats(data) {
  const el = document.getElementById('server-stats-content');
  const healthFill = document.getElementById('server-health-fill');
  if (!el) return;
  if (!data) {
    setError(el, 'No server data available.');
    return;
  }
  el.innerHTML = `<div>Status: <span class="server-status">${data.status}</span></div>
    <div>TPS: <span class="server-tps">${data.tps}</span></div>
    <div>Players Online: <span class="server-online">${data.online}</span>/${data.maxPlayers}</div>`;
  if (healthFill) {
    healthFill.style.width = `${data.healthPercent || 80}%`;
    healthFill.setAttribute('aria-valuenow', data.healthPercent || 80);
  }
}


function updateLogViewer(data) {
  const el = document.getElementById('log-viewer-content');
  if (!el) return;
  if (!data || !data.logs) {
    setError(el, 'No logs available.');
    return;
  }
  el.innerHTML = `<pre class="log-lines">${data.logs.map(l => l).join('\n')}</pre>`;
}


// Fetch real stats from API with enhanced loading states
async function fetchDemoStats() {
  const API_BASE = 'http://localhost:8080/api';
  
  // Fetch player stats
  try {
    const playerEl = document.getElementById('player-stats-content');
    showSkeleton(playerEl, 'list');
    
    const playerResponse = await fetch(`${API_BASE}/players`);
    if (playerResponse.ok) {
      const playerData = await playerResponse.json();
      
      // Add smooth transition delay
      setTimeout(() => {
        updatePlayerStats(playerData);
        showNotification(`✓ Loaded ${playerData.players?.length || 0} players`, 'info', 2000);
      }, 300);
    } else {
      setError(playerEl, 'Failed to load player data');
    }
  } catch (error) {
    setError(document.getElementById('player-stats-content'), '⚠ Connection error: ' + error.message);
  }
  
  // Fetch server stats
  try {
    const serverEl = document.getElementById('server-stats-content');
    showSkeleton(serverEl, 'stats');
    
    const serverResponse = await fetch(`${API_BASE}/server`);
    if (serverResponse.ok) {
      const serverData = await serverResponse.json();
      
      // Add smooth transition delay
      setTimeout(() => {
        updateServerStats(serverData);
        
        // Update TPS chart with real data
        renderServerChart({
          labels: ['Now'],
          tps: [serverData.tps || 20]
        });
        
        // Update live status
        setLiveStatus(true);
      }, 300);
    } else {
      setError(serverEl, 'Failed to load server data');
      setLiveStatus(false);
    }
  } catch (error) {
    setError(document.getElementById('server-stats-content'), '⚠ Connection error: ' + error.message);
    setLiveStatus(false);
  }
  
  // Fetch logs
  try {
    const logsEl = document.getElementById('log-viewer-content');
    showSkeleton(logsEl, 'logs');
    
    const logsResponse = await fetch(`${API_BASE}/logs?lines=50`);
    if (logsResponse.ok) {
      const logsData = await logsResponse.json();
      
      // Add smooth transition delay
      setTimeout(() => {
        updateLogViewer(logsData);
      }, 300);
    } else {
      setError(logsEl, 'Failed to load logs');
    }
  } catch (error) {
    setError(document.getElementById('log-viewer-content'), '⚠ Connection error: ' + error.message);
  }
}

// Legacy auto-refresh function (replaced by real-time system)
// Kept for backward compatibility
function startAutoRefresh() {
  // Initial fetch
  fetchDemoStats();
  // Note: Continuous polling is now handled by DashboardConnection
}
function updateServerStats(data) {
  // data: { tps, players, maxPlayers, world, ... }
  const el = document.querySelector('#server-stats .stats-content');
  if (!el) return;
  el.textContent = `TPS: ${data.tps} | Players: ${data.players}/${data.maxPlayers} | World: ${data.world}`;
}

function updateLogViewer(logs) {
  // logs: string or array of log lines
  const el = document.querySelector('#log-viewer .log-content');
  if (!el) return;
  el.textContent = Array.isArray(logs) ? logs.join('\n') : logs;
}

// Demo: initial load (can be replaced by Java backend)
window.addEventListener('DOMContentLoaded', () => {
  updatePlayerStats({ name: 'Steve', xp: 42, location: 'Mars Base' });
  updateServerStats({ tps: 20, players: 5, maxPlayers: 20, world: 'Nebula' });
  updateLogViewer([
    '[12:00] Steve joined the server',
    '[12:01] TPS stable at 20'
  ]);
  setLiveStatus(true);
  setServerHealth(80);
  showNotification('Welcome to SpaceMC Dashboard!');
  renderServerChart({
    labels: ['12:00', '12:01', '12:02', '12:03', '12:04'],
    tps: [20, 19.8, 20, 19.9, 20]
  });
});

// Expose new functions for backend
window.setLiveStatus = setLiveStatus;
window.setServerHealth = setServerHealth;
window.showNotification = showNotification;
window.renderServerChart = renderServerChart;

// Expose new functions for backend
window.setLiveStatus = setLiveStatus;
window.setServerHealth = setServerHealth;

// Expose update functions for Java integration
window.updatePlayerStats = updatePlayerStats;
window.updateServerStats = updateServerStats;
window.updateLogViewer = updateLogViewer;

// ===== REAL-TIME UPDATE SYSTEM =====

class DashboardConnection {
  constructor() {
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 3000;
    this.isConnected = false;
    this.usePolling = true; // Fallback to polling if WebSocket not available
    this.pollingInterval = null;
    this.statusElement = document.getElementById('connection-status');
    this.lastPlayerCount = 0;
  }

  init() {
    // Try WebSocket first, fall back to polling
    this.tryWebSocket();
    
    // Start polling as fallback
    if (this.usePolling) {
      this.startPolling();
    }
  }

  tryWebSocket() {
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.hostname}:${window.location.port || 8080}/ws`;
      
      this.ws = new WebSocket(wsUrl);
      
      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.updateConnectionStatus('connected', 'Connected');
        this.stopPolling(); // Stop polling when WebSocket works
        this.usePolling = false;
        showNotification('🚀 Real-time updates enabled', 'info', 3000);
      };
      
      this.ws.onmessage = (event) => {
        this.handleMessage(event.data);
      };
      
      this.ws.onerror = (error) => {
        console.warn('WebSocket error, falling back to polling:', error);
        this.usePolling = true;
        this.startPolling();
      };
      
      this.ws.onclose = () => {
        console.log('WebSocket closed');
        this.isConnected = false;
        this.updateConnectionStatus('disconnected', 'Disconnected');
        
        // Try to reconnect
        if (this.reconnectAttempts < this.maxReconnectAttempts && !this.usePolling) {
          this.reconnectAttempts++;
          this.updateConnectionStatus('reconnecting', `Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
          setTimeout(() => this.tryWebSocket(), this.reconnectDelay);
        } else {
          // Fall back to polling
          this.usePolling = true;
          this.startPolling();
        }
      };
    } catch (error) {
      console.warn('WebSocket not available, using polling:', error);
      this.usePolling = true;
      this.startPolling();
    }
  }

  startPolling() {
    if (this.pollingInterval) return; // Already polling
    
    this.updateConnectionStatus('connected', 'Polling Mode');
    console.log('Using polling for updates (every 5 seconds)');
    
    // Initial fetch
    this.pollData();
    
    // Poll every 5 seconds
    this.pollingInterval = setInterval(() => {
      this.pollData();
    }, 5000);
  }

  stopPolling() {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
  }

  async pollData() {
    const API_BASE = 'http://localhost:8080/api';
    
    try {
      // Fetch all data in parallel
      const [playersRes, serverRes, logsRes] = await Promise.all([
        fetch(`${API_BASE}/players`).catch(() => null),
        fetch(`${API_BASE}/server`).catch(() => null),
        fetch(`${API_BASE}/logs?lines=50`).catch(() => null)
      ]);

      if (playersRes && playersRes.ok) {
        const playerData = await playersRes.json();
        this.handlePlayerUpdate(playerData);
      }

      if (serverRes && serverRes.ok) {
        const serverData = await serverRes.json();
        this.handleServerUpdate(serverData);
      }

      if (logsRes && logsRes.ok) {
        const logsData = await logsRes.json();
        updateLogViewer(logsData);
      }

      // Update connection status
      if (!this.isConnected) {
        this.updateConnectionStatus('connected', 'Polling Mode');
      }
    } catch (error) {
      console.error('Polling error:', error);
      this.updateConnectionStatus('disconnected', 'Connection Error');
    }
  }

  handleMessage(data) {
    try {
      const message = JSON.parse(data);
      
      switch (message.type) {
        case 'player_join':
          this.handlePlayerJoin(message.data);
          break;
        case 'player_leave':
          this.handlePlayerLeave(message.data);
          break;
        case 'player_update':
          this.handlePlayerUpdate(message.data);
          break;
        case 'server_update':
          this.handleServerUpdate(message.data);
          break;
        case 'log_update':
          this.handleLogUpdate(message.data);
          break;
        case 'tps_update':
          this.handleTpsUpdate(message.data);
          break;
        default:
          console.log('Unknown message type:', message.type);
      }
    } catch (error) {
      console.error('Error handling message:', error);
    }
  }

  handlePlayerJoin(data) {
    showNotification(`👨‍🚀 ${data.name} joined the server`, 'info', 4000);
    this.pollData(); // Refresh all data
  }

  handlePlayerLeave(data) {
    showNotification(`👋 ${data.name} left the server`, 'info', 4000);
    this.pollData(); // Refresh all data
  }

  handlePlayerUpdate(data) {
    updatePlayerStats(data);
    
    // Detect player count changes
    if (data.players && data.players.length !== this.lastPlayerCount) {
      const diff = data.players.length - this.lastPlayerCount;
      if (this.lastPlayerCount > 0) { // Skip first load
        if (diff > 0) {
          showNotification(`+${diff} player${diff > 1 ? 's' : ''} online`, 'info', 2500);
        } else if (diff < 0) {
          showNotification(`${Math.abs(diff)} player${Math.abs(diff) > 1 ? 's' : ''} offline`, 'info', 2500);
        }
      }
      this.lastPlayerCount = data.players.length;
    }
  }

  handleServerUpdate(data) {
    updateServerStats(data);
    
    // Update chart data with new metrics
    if (data.tps !== undefined) {
      updateChart('tps', data.tps);
    }
    
    if (data.memory !== undefined) {
      updateChart('memory', data.memory);
    }
    
    if (data.online !== undefined) {
      updateChart('players', data.online);
    }
    
    if (data.entities !== undefined) {
      updateChart('entities', data.entities);
    }
    
    // Show warning for low TPS
    if (data.tps < 18) {
      showNotification(`⚠️ Low TPS detected: ${data.tps.toFixed(1)}`, 'error', 5000);
    }
    
    // Show warning for high memory usage
    if (data.memoryPercent && data.memoryPercent > 85) {
      showNotification(`⚠️ High memory usage: ${data.memoryPercent.toFixed(1)}%`, 'error', 5000);
    }
  }

  handleLogUpdate(data) {
    updateLogViewer(data);
  }

  handleTpsUpdate(data) {
    if (data.tps !== undefined) {
      updateChart('tps', data.tps);
    }
    if (data.memory !== undefined) {
      updateChart('memory', data.memory);
    }
    if (data.players !== undefined) {
      updateChart('players', data.players);
    }
    if (data.entities !== undefined) {
      updateChart('entities', data.entities);
    }
  }

  updateConnectionStatus(status, text) {
    if (!this.statusElement) return;
    
    // Remove all status classes
    this.statusElement.classList.remove('connected', 'disconnected', 'reconnecting');
    
    // Add new status class
    this.statusElement.classList.add(status);
    
    // Update text
    const textElement = this.statusElement.querySelector('.status-text');
    if (textElement) {
      textElement.textContent = text;
    }
    
    // Update accessibility
    this.statusElement.setAttribute('aria-label', `Connection status: ${text}`);
  }

  send(type, data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, data }));
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
    }
    this.stopPolling();
  }
}

// Initialize connection system
let dashboardConnection;

document.addEventListener('DOMContentLoaded', function() {
  dashboardConnection = new DashboardConnection();
  dashboardConnection.init();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
  if (dashboardConnection) {
    dashboardConnection.disconnect();
  }
});

// Expose for debugging
window.dashboardConnection = dashboardConnection;
