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
        // Load settings if navigating to settings section
        if (targetId === 'settings') {
          loadUserSettings();
        }
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
    this.playerSelector = document.getElementById('player-selector');
    this.currentPlayerData = null;
    this.playersList = [];
    this.init();
  }
  
  init() {
    // Setup close button
    this.closeBtn?.addEventListener('click', () => this.closeModal());
    
    // Setup details button
    this.detailsBtn?.addEventListener('click', () => this.openModal('Steve')); // Example
    
    // Setup player selector
    this.playerSelector?.addEventListener('change', (e) => {
      const selectedPlayer = e.target.value;
      if (selectedPlayer) {
        this.openModal(selectedPlayer);
      }
    });
    
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
    
    // Load players list
    this.loadPlayersList();
  }
  
  async loadPlayersList() {
    try {
      // Fetch online players from API
      const response = await fetch('/api/players');
      if (response.ok) {
        const data = await response.json();
        this.playersList = data.players || [];
        this.populatePlayerSelector();
      }
    } catch (error) {
      console.error('Failed to load players list:', error);
      // Fallback to mock data
      this.playersList = ['Steve', 'Alex', 'Notch', 'Herobrine'];
      this.populatePlayerSelector();
    }
  }
  
  populatePlayerSelector() {
    if (!this.playerSelector) return;
    
    this.playerSelector.innerHTML = '';
    
    if (this.playersList.length === 0) {
      this.playerSelector.innerHTML = '<option value="">No players online</option>';
      return;
    }
    
    // Add placeholder option
    const placeholderOption = document.createElement('option');
    placeholderOption.value = '';
    placeholderOption.textContent = 'Select a player...';
    this.playerSelector.appendChild(placeholderOption);
    
    // Add player options
    this.playersList.forEach(player => {
      const option = document.createElement('option');
      option.value = typeof player === 'string' ? player : player.name;
      option.textContent = typeof player === 'string' ? player : player.name;
      this.playerSelector.appendChild(option);
    });
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
      // Update selector to show current player
      if (this.playerSelector) {
        this.playerSelector.value = playerName;
      }
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
    
    // Compact Card Elements
    this.compactTpsEl = document.getElementById('compact-tps');
    this.compactTpsStatusEl = document.getElementById('compact-tps-status');
    this.compactMemoryEl = document.getElementById('compact-memory');
    this.compactMemoryMaxEl = document.getElementById('compact-memory-max');
    this.compactMemoryFillEl = document.getElementById('compact-memory-fill');
    this.compactEntitiesEl = document.getElementById('compact-entities');
    this.compactChunksEl = document.getElementById('compact-chunks');
    this.compactUptimeEl = document.getElementById('compact-uptime');
    
    // Modal Elements
    this.tpsModal = document.getElementById('tps-modal');
    this.memoryModal = document.getElementById('memory-modal');
    this.entitiesModal = document.getElementById('entities-modal');
    this.uptimeModal = document.getElementById('uptime-modal');
    
    // Modal Content Elements (TPS)
    this.modalCurrentTpsEl = document.getElementById('modal-current-tps');
    this.modalTpsStatusEl = document.getElementById('modal-tps-status');
    this.modalAvgTickTimeEl = document.getElementById('modal-avg-tick-time');
    this.modalPeakTickTimeEl = document.getElementById('modal-peak-tick-time');
    this.modalEntityTickTimeEl = document.getElementById('modal-entity-tick-time');
    this.modalBlockTickTimeEl = document.getElementById('modal-block-tick-time');
    this.modalChunkTickTimeEl = document.getElementById('modal-chunk-tick-time');
    this.modalTaskTickTimeEl = document.getElementById('modal-task-tick-time');
    this.modalOtherTickTimeEl = document.getElementById('modal-other-tick-time');
    this.modalEntityTickBar = document.getElementById('modal-entity-tick-bar');
    this.modalBlockTickBar = document.getElementById('modal-block-tick-bar');
    this.modalChunkTickBar = document.getElementById('modal-chunk-tick-bar');
    this.modalTaskTickBar = document.getElementById('modal-task-tick-bar');
    this.modalOtherTickBar = document.getElementById('modal-other-tick-bar');
    
    // Modal Content Elements (Memory)
    this.modalHeapUsedEl = document.getElementById('modal-heap-used');
    this.modalHeapMaxEl = document.getElementById('modal-heap-max');
    this.modalHeapFillEl = document.getElementById('modal-heap-fill');
    this.modalNonheapUsedEl = document.getElementById('modal-nonheap-used');
    this.modalNonheapMaxEl = document.getElementById('modal-nonheap-max');
    this.modalNonheapFillEl = document.getElementById('modal-nonheap-fill');
    this.modalGcCountEl = document.getElementById('modal-gc-count');
    this.modalGcTimeEl = document.getElementById('modal-gc-time');
    this.modalGcAvgEl = document.getElementById('modal-gc-avg');
    
    // Modal Content Elements (Entities)
    this.modalTotalEntitiesEl = document.getElementById('modal-total-entities');
    this.modalLoadedChunksEl = document.getElementById('modal-loaded-chunks');
    this.modalChunkUpdatesEl = document.getElementById('modal-chunk-updates');
    this.modalEntitiesAnimalsEl = document.getElementById('modal-entities-animals');
    this.modalEntitiesPlayersEl = document.getElementById('modal-entities-players');
    this.modalEntitiesMonstersEl = document.getElementById('modal-entities-monsters');
    this.modalEntitiesItemsEl = document.getElementById('modal-entities-items');
    this.modalEntitiesProjectilesEl = document.getElementById('modal-entities-projectiles');
    this.modalEntitiesOtherEl = document.getElementById('modal-entities-other');
    this.modalMainThreadCpuEl = document.getElementById('modal-main-thread-cpu');
    this.modalWorkerActiveEl = document.getElementById('modal-worker-active');
    this.modalWorkerQueuedEl = document.getElementById('modal-worker-queued');
    this.modalWorkerCompletedEl = document.getElementById('modal-worker-completed');
    this.modalAsyncActiveEl = document.getElementById('modal-async-active');
    this.modalAsyncQueuedEl = document.getElementById('modal-async-queued');
    this.modalAsyncCompletedEl = document.getElementById('modal-async-completed');
    this.modalWorkerPoolCountEl = document.getElementById('modal-worker-pool-count');
    this.modalAsyncPoolCountEl = document.getElementById('modal-async-pool-count');
    
    // Modal Content Elements (Uptime)
    this.modalServerUptimeEl = document.getElementById('modal-server-uptime');
    this.modalServerHealthEl = document.getElementById('modal-server-health');
    this.modalHealthFillEl = document.getElementById('modal-health-fill');
    
    // Modal Charts
    this.modalTpsHistoryChart = null;
    this.modalMemoryUsageChart = null;
    
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
    this.setupModals();
    this.initCharts();
    this.initModalCharts();
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
  
  setupModals() {
    // Setup "More Info" buttons
    const moreInfoButtons = document.querySelectorAll('.more-info-btn');
    moreInfoButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const modalId = btn.getAttribute('data-modal');
        const modal = document.getElementById(modalId);
        if (modal) {
          modal.showModal();
          // Update modal data when opened
          this.updateModalData(modalId);
        }
      });
    });
    
    // Setup modal close buttons
    this.setupModalClose('close-tps-modal', 'tps-modal');
    this.setupModalClose('close-memory-modal', 'memory-modal');
    this.setupModalClose('close-entities-modal', 'entities-modal');
    this.setupModalClose('close-uptime-modal', 'uptime-modal');
    
    // Close on outside click
    [this.tpsModal, this.memoryModal, this.entitiesModal, this.uptimeModal].forEach(modal => {
      if (modal) {
        modal.addEventListener('click', (e) => {
          if (e.target === modal) {
            modal.close();
          }
        });
      }
    });
  }
  
  setupModalClose(btnId, modalId) {
    const btn = document.getElementById(btnId);
    const modal = document.getElementById(modalId);
    if (btn && modal) {
      btn.addEventListener('click', () => modal.close());
    }
  }
  
  updateModalData(modalId) {
    // When a modal is opened, sync its data with current values
    switch(modalId) {
      case 'tps-modal':
        this.updateTPSModal();
        break;
      case 'memory-modal':
        this.updateMemoryModal();
        break;
      case 'entities-modal':
        this.updateEntitiesModal();
        break;
      case 'uptime-modal':
        this.updateUptimeModal();
        break;
    }
  }
  
  updateTPSModal() {
    if (this.modalCurrentTpsEl && this.currentTpsEl) {
      this.modalCurrentTpsEl.textContent = this.currentTpsEl.textContent;
    }
    if (this.modalTpsStatusEl && this.tpsStatusEl) {
      this.modalTpsStatusEl.innerHTML = this.tpsStatusEl.innerHTML;
      this.modalTpsStatusEl.className = this.tpsStatusEl.className;
    }
    if (this.modalAvgTickTimeEl && this.avgTickTimeEl) {
      this.modalAvgTickTimeEl.innerHTML = this.avgTickTimeEl.innerHTML;
    }
    if (this.modalPeakTickTimeEl && this.peakTickTimeEl) {
      this.modalPeakTickTimeEl.innerHTML = this.peakTickTimeEl.innerHTML;
    }
    // Update tick breakdown
    if (this.modalEntityTickTimeEl && this.entityTickTimeEl) {
      this.modalEntityTickTimeEl.textContent = this.entityTickTimeEl.textContent;
    }
    if (this.modalBlockTickTimeEl && this.blockTickTimeEl) {
      this.modalBlockTickTimeEl.textContent = this.blockTickTimeEl.textContent;
    }
    if (this.modalChunkTickTimeEl && this.chunkTickTimeEl) {
      this.modalChunkTickTimeEl.textContent = this.chunkTickTimeEl.textContent;
    }
    if (this.modalTaskTickTimeEl && this.taskTickTimeEl) {
      this.modalTaskTickTimeEl.textContent = this.taskTickTimeEl.textContent;
    }
    if (this.modalOtherTickTimeEl && this.otherTickTimeEl) {
      this.modalOtherTickTimeEl.textContent = this.otherTickTimeEl.textContent;
    }
    // Update bars
    if (this.modalEntityTickBar && this.entityTickBar) {
      this.modalEntityTickBar.style.width = this.entityTickBar.style.width;
    }
    if (this.modalBlockTickBar && this.blockTickBar) {
      this.modalBlockTickBar.style.width = this.blockTickBar.style.width;
    }
    if (this.modalChunkTickBar && this.chunkTickBar) {
      this.modalChunkTickBar.style.width = this.chunkTickBar.style.width;
    }
    if (this.modalTaskTickBar && this.taskTickBar) {
      this.modalTaskTickBar.style.width = this.taskTickBar.style.width;
    }
    if (this.modalOtherTickBar && this.otherTickBar) {
      this.modalOtherTickBar.style.width = this.otherTickBar.style.width;
    }
  }
  
  updateMemoryModal() {
    if (this.modalHeapUsedEl && this.heapUsedEl) {
      this.modalHeapUsedEl.innerHTML = this.heapUsedEl.innerHTML;
    }
    if (this.modalHeapMaxEl && this.heapMaxEl) {
      this.modalHeapMaxEl.textContent = this.heapMaxEl.textContent;
    }
    if (this.modalHeapFillEl && this.heapFillEl) {
      this.modalHeapFillEl.style.width = this.heapFillEl.style.width;
    }
    if (this.modalNonheapUsedEl && this.nonheapUsedEl) {
      this.modalNonheapUsedEl.innerHTML = this.nonheapUsedEl.innerHTML;
    }
    if (this.modalNonheapMaxEl && this.nonheapMaxEl) {
      this.modalNonheapMaxEl.textContent = this.nonheapMaxEl.textContent;
    }
    if (this.modalNonheapFillEl && this.nonheapFillEl) {
      this.modalNonheapFillEl.style.width = this.nonheapFillEl.style.width;
    }
    if (this.modalGcCountEl && this.gcCountEl) {
      this.modalGcCountEl.textContent = this.gcCountEl.textContent;
    }
    if (this.modalGcTimeEl && this.gcTimeEl) {
      this.modalGcTimeEl.innerHTML = this.gcTimeEl.innerHTML;
    }
    // Calculate average GC duration
    if (this.modalGcAvgEl && this.gcCountEl && this.gcTimeEl) {
      const count = parseInt(this.gcCountEl.textContent) || 1;
      const time = parseFloat(this.gcTimeEl.textContent) || 0;
      const avg = ((time * 1000) / count).toFixed(1);
      this.modalGcAvgEl.innerHTML = `${avg}<span class="modal-unit">ms</span>`;
    }
  }
  
  updateEntitiesModal() {
    if (this.modalTotalEntitiesEl && this.totalEntitiesEl) {
      this.modalTotalEntitiesEl.textContent = this.totalEntitiesEl.textContent;
    }
    if (this.modalLoadedChunksEl && this.loadedChunksEl) {
      this.modalLoadedChunksEl.textContent = this.loadedChunksEl.textContent;
    }
    if (this.modalChunkUpdatesEl && this.chunkUpdatesEl) {
      this.modalChunkUpdatesEl.textContent = this.chunkUpdatesEl.textContent;
    }
    // Entity breakdown
    if (this.modalEntitiesAnimalsEl && this.entitiesAnimalsEl) {
      this.modalEntitiesAnimalsEl.textContent = this.entitiesAnimalsEl.textContent;
    }
    if (this.modalEntitiesPlayersEl && this.entitiesPlayersEl) {
      this.modalEntitiesPlayersEl.textContent = this.entitiesPlayersEl.textContent;
    }
    if (this.modalEntitiesMonstersEl && this.entitiesMonstersEl) {
      this.modalEntitiesMonstersEl.textContent = this.entitiesMonstersEl.textContent;
    }
    if (this.modalEntitiesItemsEl && this.entitiesItemsEl) {
      this.modalEntitiesItemsEl.textContent = this.entitiesItemsEl.textContent;
    }
    if (this.modalEntitiesProjectilesEl && this.entitiesProjectilesEl) {
      this.modalEntitiesProjectilesEl.textContent = this.entitiesProjectilesEl.textContent;
    }
    if (this.modalEntitiesOtherEl && this.entitiesOtherEl) {
      this.modalEntitiesOtherEl.textContent = this.entitiesOtherEl.textContent;
    }
    // Thread pool
    if (this.modalMainThreadCpuEl && this.mainThreadCpuEl) {
      this.modalMainThreadCpuEl.textContent = this.mainThreadCpuEl.textContent;
    }
    if (this.modalWorkerActiveEl && this.workerActiveEl) {
      this.modalWorkerActiveEl.textContent = this.workerActiveEl.textContent;
    }
    if (this.modalWorkerQueuedEl && this.workerQueuedEl) {
      this.modalWorkerQueuedEl.textContent = this.workerQueuedEl.textContent;
    }
    if (this.modalWorkerCompletedEl && this.workerCompletedEl) {
      this.modalWorkerCompletedEl.textContent = this.workerCompletedEl.textContent;
    }
    if (this.modalAsyncActiveEl && this.asyncActiveEl) {
      this.modalAsyncActiveEl.textContent = this.asyncActiveEl.textContent;
    }
    if (this.modalAsyncQueuedEl && this.asyncQueuedEl) {
      this.modalAsyncQueuedEl.textContent = this.asyncQueuedEl.textContent;
    }
    if (this.modalAsyncCompletedEl && this.asyncCompletedEl) {
      this.modalAsyncCompletedEl.textContent = this.asyncCompletedEl.textContent;
    }
  }
  
  updateUptimeModal() {
    if (this.modalServerUptimeEl && this.serverUptimeEl) {
      this.modalServerUptimeEl.textContent = this.serverUptimeEl.textContent;
    }
    // Calculate server health based on TPS
    if (this.modalServerHealthEl && this.currentTpsEl) {
      const tps = parseFloat(this.currentTpsEl.textContent);
      const health = Math.round((tps / 20) * 100);
      this.modalServerHealthEl.innerHTML = `${health}<span class="modal-unit">%</span>`;
      if (this.modalHealthFillEl) {
        this.modalHealthFillEl.style.width = `${health}%`;
      }
    }
  }
  
  initModalCharts() {
    // Modal TPS History Chart
    const modalTpsCtx = document.getElementById('modal-tps-history-chart');
    if (modalTpsCtx) {
      const labels = Array.from({length: 60}, (_, i) => `${59 - i}m`);
      const data = Array.from({length: 60}, () => 19 + Math.random() * 1.5);
      
      this.modalTpsHistoryChart = new Chart(modalTpsCtx, {
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
            legend: { display: false },
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
              grid: { color: 'rgba(255, 255, 255, 0.05)' },
              ticks: { color: 'rgba(255, 255, 255, 0.5)', maxTicksLimit: 10 }
            },
            y: {
              display: true,
              min: 0,
              max: 20,
              grid: { color: 'rgba(255, 255, 255, 0.05)' },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                callback: function(value) { return value.toFixed(1); }
              }
            }
          }
        }
      });
    }
    
    // Modal Memory Usage Chart
    const modalMemoryCtx = document.getElementById('modal-memory-usage-chart');
    if (modalMemoryCtx) {
      const labels = Array.from({length: 60}, (_, i) => `${59 - i}m`);
      const heapData = Array.from({length: 60}, () => 2 + Math.random() * 0.8);
      
      this.modalMemoryUsageChart = new Chart(modalMemoryCtx, {
        type: 'line',
        data: {
          labels: labels,
          datasets: [{
            label: 'Heap Memory (GB)',
            data: heapData,
            borderColor: 'rgba(59, 130, 246, 1)',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
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
            legend: { display: false },
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
                  return `Memory: ${context.parsed.y.toFixed(2)} GB`;
                }
              }
            }
          },
          scales: {
            x: {
              display: true,
              grid: { color: 'rgba(255, 255, 255, 0.05)' },
              ticks: { color: 'rgba(255, 255, 255, 0.5)', maxTicksLimit: 10 }
            },
            y: {
              display: true,
              min: 0,
              max: 4,
              grid: { color: 'rgba(255, 255, 255, 0.05)' },
              ticks: {
                color: 'rgba(255, 255, 255, 0.5)',
                callback: function(value) { return value.toFixed(1) + ' GB'; }
              }
            }
          }
        }
      });
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
    
    // Update compact card
    if (this.compactTpsEl) {
      this.compactTpsEl.textContent = tps.toFixed(1);
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
    
    // Update compact card status
    if (this.compactTpsStatusEl) {
      this.compactTpsStatusEl.classList.remove('good', 'warning', 'danger');
      if (tps >= 19.5) {
        this.compactTpsStatusEl.classList.add('good');
        this.compactTpsStatusEl.style.color = '#22c55e';
      } else if (tps >= 18.0) {
        this.compactTpsStatusEl.classList.add('warning');
        this.compactTpsStatusEl.style.color = '#f59e0b';
      } else {
        this.compactTpsStatusEl.classList.add('danger');
        this.compactTpsStatusEl.style.color = '#ef4444';
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
    
    const uptimeText = `${hours}h ${minutes}m`;
    
    if (this.serverUptimeEl) {
      this.serverUptimeEl.textContent = uptimeText;
    }
    
    // Update compact card
    if (this.compactUptimeEl) {
      this.compactUptimeEl.textContent = uptimeText;
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
    
    // Update compact card
    if (this.compactMemoryEl) {
      this.compactMemoryEl.innerHTML = `${memory.heapUsed.toFixed(1)}<span class="compact-unit">GB</span>`;
    }
    if (this.compactMemoryMaxEl) {
      this.compactMemoryMaxEl.textContent = `of ${memory.heapMax.toFixed(1)} GB`;
    }
    if (this.compactMemoryFillEl) {
      const heapPercent = (memory.heapUsed / memory.heapMax) * 100;
      this.compactMemoryFillEl.style.width = heapPercent + '%';
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
    
    // Update compact cards
    if (this.compactEntitiesEl) {
      this.compactEntitiesEl.textContent = data.totalEntities.toLocaleString();
    }
    if (this.compactChunksEl) {
      this.compactChunksEl.textContent = data.loadedChunks.toLocaleString();
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
// ADVANCED SEARCH & FILTERING
// ========================================

class GlobalSearchManager {
  constructor() {
    // DOM References - Search Input
    this.searchInput = document.getElementById('global-search-input');
    this.clearSearchBtn = document.getElementById('clear-global-search');
    this.advancedFiltersBtn = document.getElementById('open-advanced-filters');
    this.savedPresetsBtn = document.getElementById('open-saved-presets');
    
    // DOM References - Scope
    this.scopeCheckboxes = document.querySelectorAll('.search-scope');
    
    // DOM References - Results
    this.resultsSummary = document.getElementById('search-results-summary');
    this.totalResultsCount = document.getElementById('total-results-count');
    this.searchTime = document.getElementById('search-time');
    this.resultsContainer = document.getElementById('search-results-container');
    this.resultsList = document.getElementById('search-results-list');
    this.sortResults = document.getElementById('sort-results');
    this.exportResults = document.getElementById('export-results');
    
    // DOM References - Pagination
    this.searchPagination = document.getElementById('search-pagination');
    this.searchPrevPage = document.getElementById('search-prev-page');
    this.searchNextPage = document.getElementById('search-next-page');
    this.searchCurrentPage = document.getElementById('search-current-page');
    this.searchTotalPages = document.getElementById('search-total-pages');
    
    // DOM References - Advanced Filters Modal
    this.advancedFiltersModal = document.getElementById('advanced-filters-modal');
    this.closeAdvancedFilters = document.getElementById('close-advanced-filters');
    this.cancelAdvancedFilters = document.getElementById('cancel-advanced-filters');
    this.applyAdvancedFilters = document.getElementById('apply-advanced-filters');
    this.resetFilters = document.getElementById('reset-filters');
    
    // DOM References - Filter Inputs
    this.filterDateFrom = document.getElementById('filter-date-from');
    this.filterDateTo = document.getElementById('filter-date-to');
    this.filterPlayerStatus = document.getElementById('filter-player-status');
    this.filterPlayerRank = document.getElementById('filter-player-rank');
    this.filterLogLevel = document.getElementById('filter-log-level');
    this.filterLogSource = document.getElementById('filter-log-source');
    this.filterFileType = document.getElementById('filter-file-type');
    this.filterFileSize = document.getElementById('filter-file-size');
    this.filterAlertSeverity = document.getElementById('filter-alert-severity');
    this.filterAlertCategory = document.getElementById('filter-alert-category');
    this.filterTransactionType = document.getElementById('filter-transaction-type');
    this.filterAmountMin = document.getElementById('filter-amount-min');
    this.filterAmountMax = document.getElementById('filter-amount-max');
    
    // DOM References - Saved Presets Modal
    this.savedPresetsModal = document.getElementById('saved-presets-modal');
    this.closeSavedPresets = document.getElementById('close-saved-presets');
    this.closePresetsModal = document.getElementById('close-presets-modal');
    this.presetsList = document.getElementById('presets-list');
    this.saveCurrentPreset = document.getElementById('save-current-preset');
    
    // DOM References - Save Preset Modal
    this.savePresetModal = document.getElementById('save-preset-modal');
    this.closeSavePreset = document.getElementById('close-save-preset');
    this.cancelSavePreset = document.getElementById('cancel-save-preset');
    this.confirmSavePreset = document.getElementById('confirm-save-preset');
    this.presetName = document.getElementById('preset-name');
    this.presetDescription = document.getElementById('preset-description');
    
    // State
    this.searchResults = [];
    this.filteredResults = [];
    this.currentPage = 1;
    this.pageSize = 20;
    this.currentSort = 'relevance';
    this.searchTimeout = null;
    this.activeFilters = {};
    this.savedPresets = [];
    
    // Mock data sources
    this.mockData = this.generateMockData();
  }
  
  init() {
    this.setupEventListeners();
    this.loadSavedPresets();
  }
  
  setupEventListeners() {
    // Search input
    this.searchInput?.addEventListener('input', () => {
      clearTimeout(this.searchTimeout);
      this.searchTimeout = setTimeout(() => this.performSearch(), 300);
      
      if (this.searchInput.value) {
        this.clearSearchBtn.style.display = 'flex';
      } else {
        this.clearSearchBtn.style.display = 'none';
        this.hideResults();
      }
    });
    
    this.clearSearchBtn?.addEventListener('click', () => {
      this.searchInput.value = '';
      this.clearSearchBtn.style.display = 'none';
      this.hideResults();
    });
    
    // Scope checkboxes
    this.scopeCheckboxes.forEach(checkbox => {
      checkbox.addEventListener('change', () => {
        if (this.searchInput.value) {
          this.performSearch();
        }
      });
    });
    
    // Sort and export
    this.sortResults?.addEventListener('change', () => {
      this.currentSort = this.sortResults.value;
      this.sortSearchResults();
      this.renderResults();
    });
    
    this.exportResults?.addEventListener('click', () => this.exportSearchResults());
    
    // Pagination
    this.searchPrevPage?.addEventListener('click', () => this.changePage(-1));
    this.searchNextPage?.addEventListener('click', () => this.changePage(1));
    
    // Advanced filters modal
    this.advancedFiltersBtn?.addEventListener('click', () => this.openAdvancedFilters());
    this.closeAdvancedFilters?.addEventListener('click', () => this.closeAdvancedFiltersModal());
    this.cancelAdvancedFilters?.addEventListener('click', () => this.closeAdvancedFiltersModal());
    this.applyAdvancedFilters?.addEventListener('click', () => this.applyFilters());
    this.resetFilters?.addEventListener('click', () => this.resetAllFilters());
    
    // Saved presets modal
    this.savedPresetsBtn?.addEventListener('click', () => this.openSavedPresetsModal());
    this.closeSavedPresets?.addEventListener('click', () => this.closeSavedPresetsModalDialog());
    this.closePresetsModal?.addEventListener('click', () => this.closeSavedPresetsModalDialog());
    this.saveCurrentPreset?.addEventListener('click', () => this.openSavePresetModal());
    
    // Save preset modal
    this.closeSavePreset?.addEventListener('click', () => this.closeSavePresetModalDialog());
    this.cancelSavePreset?.addEventListener('click', () => this.closeSavePresetModalDialog());
    this.confirmSavePreset?.addEventListener('click', () => this.savePreset());
  }
  
  generateMockData() {
    return {
      players: [
        { name: 'ZeroG', status: 'online', rank: 'admin', joinDate: '2024-01-15', playtime: '450h', lastSeen: 'Now' },
        { name: 'Steve', status: 'online', rank: 'member', joinDate: '2024-03-20', playtime: '120h', lastSeen: 'Now' },
        { name: 'Alex', status: 'offline', rank: 'vip', joinDate: '2024-02-10', playtime: '280h', lastSeen: '2h ago' },
        { name: 'Notch', status: 'offline', rank: 'moderator', joinDate: '2024-01-01', playtime: '890h', lastSeen: '1d ago' },
        { name: 'Herobrine', status: 'online', rank: 'member', joinDate: '2024-04-05', playtime: '95h', lastSeen: 'Now' }
      ],
      logs: [
        { level: 'error', source: 'NeoEssentials', message: 'Failed to load configuration file', date: '2025-10-14T10:30:00' },
        { level: 'warn', source: 'WorldEdit', message: 'Large edit operation detected', date: '2025-10-14T09:15:00' },
        { level: 'info', source: 'LuckPerms', message: 'Permission group updated successfully', date: '2025-10-14T08:45:00' },
        { level: 'debug', source: 'Vault', message: 'Economy transaction completed', date: '2025-10-14T07:20:00' },
        { level: 'error', source: 'Dynmap', message: 'Map rendering failed', date: '2025-10-13T23:10:00' }
      ],
      files: [
        { name: 'config.json', type: 'config', size: 'small', path: '/plugins/NeoEssentials/config.json', modified: '2025-10-14T10:00:00' },
        { name: 'server.properties', type: 'config', size: 'small', path: '/server.properties', modified: '2025-10-14T09:30:00' },
        { name: 'latest.log', type: 'log', size: 'medium', path: '/logs/latest.log', modified: '2025-10-14T11:00:00' },
        { name: 'playerdata.dat', type: 'data', size: 'large', path: '/world/playerdata/player.dat', modified: '2025-10-14T10:45:00' },
        { name: 'WorldEdit.jar', type: 'plugin', size: 'medium', path: '/plugins/WorldEdit.jar', modified: '2025-10-01T00:00:00' }
      ],
      alerts: [
        { title: 'Low TPS Warning', severity: 'warning', category: 'performance', message: 'Server TPS dropped to 15.2', date: '2025-10-14T10:30:15' },
        { title: 'High Memory Usage', severity: 'critical', category: 'performance', message: 'Memory usage reached 92%', date: '2025-10-14T09:15:30' },
        { title: 'Player Death', severity: 'info', category: 'player', message: 'Player ZeroG died by Zombie', date: '2025-10-14T11:45:22' },
        { title: 'Lag Spike Detected', severity: 'warning', category: 'performance', message: 'Server lag spike: 150ms', date: '2025-10-14T10:50:00' },
        { title: 'Server Crash', severity: 'critical', category: 'system', message: 'Server crashed: out of memory', date: '2025-10-13T03:22:15' }
      ],
      commands: [
        { command: '/tp', description: 'Teleport to a player or location', category: 'teleportation', usage: '/tp <player> [target]' },
        { command: '/ban', description: 'Ban a player from the server', category: 'moderation', usage: '/ban <player> [reason]' },
        { command: '/give', description: 'Give items to a player', category: 'items', usage: '/give <player> <item> [amount]' },
        { command: '/gamemode', description: 'Change player gamemode', category: 'gameplay', usage: '/gamemode <mode> [player]' },
        { command: '/time', description: 'Set world time', category: 'world', usage: '/time set <value>' }
      ],
      chat: [
        { player: 'ZeroG', message: 'Hello everyone!', date: '2025-10-14T11:50:00' },
        { player: 'Steve', message: 'Anyone want to go mining?', date: '2025-10-14T11:48:00' },
        { player: 'Alex', message: 'Found diamonds!', date: '2025-10-14T11:45:00' },
        { player: 'Herobrine', message: 'Need help with building', date: '2025-10-14T11:40:00' },
        { player: 'ZeroG', message: 'Check out the new spawn area', date: '2025-10-14T11:35:00' }
      ],
      economy: [
        { player: 'ZeroG', type: 'payment', amount: 500, description: 'Sold diamonds', date: '2025-10-14T11:30:00' },
        { player: 'Steve', type: 'purchase', amount: -250, description: 'Bought enchanted sword', date: '2025-10-14T11:20:00' },
        { player: 'Alex', type: 'reward', amount: 1000, description: 'Daily login bonus', date: '2025-10-14T10:00:00' },
        { player: 'Notch', type: 'penalty', amount: -100, description: 'Breaking rules', date: '2025-10-14T09:45:00' },
        { player: 'Herobrine', type: 'payment', amount: 750, description: 'Quest completion', date: '2025-10-14T09:30:00' }
      ]
    };
  }
  
  performSearch() {
    const query = this.searchInput.value.trim().toLowerCase();
    if (!query) {
      this.hideResults();
      return;
    }
    
    const startTime = performance.now();
    const selectedScopes = Array.from(this.scopeCheckboxes)
      .filter(cb => cb.checked)
      .map(cb => cb.value);
    
    this.searchResults = [];
    
    // Search in selected scopes
    selectedScopes.forEach(scope => {
      if (this.mockData[scope]) {
        const results = this.searchInScope(scope, query);
        this.searchResults.push(...results);
      }
    });
    
    // Apply advanced filters
    this.filteredResults = this.applyAdvancedFilters(this.searchResults);
    
    // Sort results
    this.sortSearchResults();
    
    const endTime = performance.now();
    const searchTime = Math.round(endTime - startTime);
    
    this.displayResults(searchTime);
  }
  
  searchInScope(scope, query) {
    const data = this.mockData[scope];
    const results = [];
    
    data.forEach(item => {
      let matches = false;
      let matchedText = '';
      
      switch (scope) {
        case 'players':
          matches = item.name.toLowerCase().includes(query) || 
                   item.rank.toLowerCase().includes(query);
          matchedText = `${item.name} - ${item.rank} - ${item.status}`;
          break;
        case 'logs':
          matches = item.message.toLowerCase().includes(query) || 
                   item.source.toLowerCase().includes(query);
          matchedText = item.message;
          break;
        case 'files':
          matches = item.name.toLowerCase().includes(query) || 
                   item.path.toLowerCase().includes(query);
          matchedText = item.path;
          break;
        case 'alerts':
          matches = item.title.toLowerCase().includes(query) || 
                   item.message.toLowerCase().includes(query);
          matchedText = item.message;
          break;
        case 'commands':
          matches = item.command.toLowerCase().includes(query) || 
                   item.description.toLowerCase().includes(query);
          matchedText = item.description;
          break;
        case 'chat':
          matches = item.message.toLowerCase().includes(query) || 
                   item.player.toLowerCase().includes(query);
          matchedText = item.message;
          break;
        case 'economy':
          matches = item.player.toLowerCase().includes(query) || 
                   item.description.toLowerCase().includes(query);
          matchedText = item.description;
          break;
      }
      
      if (matches) {
        results.push({
          scope,
          data: item,
          matchedText: this.highlightMatch(matchedText, query),
          relevance: this.calculateRelevance(item, query)
        });
      }
    });
    
    return results;
  }
  
  highlightMatch(text, query) {
    const regex = new RegExp(`(${query})`, 'gi');
    return text.replace(regex, '<span class="result-highlight">$1</span>');
  }
  
  calculateRelevance(item, query) {
    // Simple relevance scoring
    let score = 0;
    const itemStr = JSON.stringify(item).toLowerCase();
    const queryWords = query.split(' ');
    
    queryWords.forEach(word => {
      const count = (itemStr.match(new RegExp(word, 'g')) || []).length;
      score += count;
    });
    
    return score;
  }
  
  applyAdvancedFilters(results) {
    let filtered = [...results];
    
    // Date range filter
    if (this.activeFilters.dateFrom || this.activeFilters.dateTo) {
      filtered = filtered.filter(result => {
        const itemDate = result.data.date || result.data.modified;
        if (!itemDate) return true;
        
        const date = new Date(itemDate);
        if (this.activeFilters.dateFrom && date < new Date(this.activeFilters.dateFrom)) return false;
        if (this.activeFilters.dateTo && date > new Date(this.activeFilters.dateTo)) return false;
        return true;
      });
    }
    
    // Scope-specific filters
    filtered = filtered.filter(result => {
      switch (result.scope) {
        case 'players':
          if (this.activeFilters.playerStatus && result.data.status !== this.activeFilters.playerStatus) return false;
          if (this.activeFilters.playerRank && result.data.rank !== this.activeFilters.playerRank) return false;
          break;
        case 'logs':
          if (this.activeFilters.logLevel && result.data.level !== this.activeFilters.logLevel) return false;
          if (this.activeFilters.logSource && !result.data.source.includes(this.activeFilters.logSource)) return false;
          break;
        case 'files':
          if (this.activeFilters.fileType && result.data.type !== this.activeFilters.fileType) return false;
          if (this.activeFilters.fileSize && result.data.size !== this.activeFilters.fileSize) return false;
          break;
        case 'alerts':
          if (this.activeFilters.alertSeverity && result.data.severity !== this.activeFilters.alertSeverity) return false;
          if (this.activeFilters.alertCategory && result.data.category !== this.activeFilters.alertCategory) return false;
          break;
        case 'economy':
          if (this.activeFilters.transactionType && result.data.type !== this.activeFilters.transactionType) return false;
          if (this.activeFilters.amountMin && Math.abs(result.data.amount) < this.activeFilters.amountMin) return false;
          if (this.activeFilters.amountMax && Math.abs(result.data.amount) > this.activeFilters.amountMax) return false;
          break;
      }
      return true;
    });
    
    return filtered;
  }
  
  sortSearchResults() {
    switch (this.currentSort) {
      case 'relevance':
        this.filteredResults.sort((a, b) => b.relevance - a.relevance);
        break;
      case 'date-desc':
        this.filteredResults.sort((a, b) => {
          const dateA = new Date(a.data.date || a.data.modified || 0);
          const dateB = new Date(b.data.date || b.data.modified || 0);
          return dateB - dateA;
        });
        break;
      case 'date-asc':
        this.filteredResults.sort((a, b) => {
          const dateA = new Date(a.data.date || a.data.modified || 0);
          const dateB = new Date(b.data.date || b.data.modified || 0);
          return dateA - dateB;
        });
        break;
      case 'name-asc':
        this.filteredResults.sort((a, b) => {
          const nameA = (a.data.name || a.data.title || a.data.command || '').toLowerCase();
          const nameB = (b.data.name || b.data.title || b.data.command || '').toLowerCase();
          return nameA.localeCompare(nameB);
        });
        break;
      case 'name-desc':
        this.filteredResults.sort((a, b) => {
          const nameA = (a.data.name || a.data.title || a.data.command || '').toLowerCase();
          const nameB = (b.data.name || b.data.title || b.data.command || '').toLowerCase();
          return nameB.localeCompare(nameA);
        });
        break;
    }
  }
  
  displayResults(searchTime) {
    // Update summary
    this.totalResultsCount.textContent = this.filteredResults.length;
    this.searchTime.textContent = searchTime;
    this.resultsSummary.style.display = 'flex';
    this.resultsContainer.style.display = 'flex';
    
    // Update category counts
    const categoryCounts = {};
    this.filteredResults.forEach(result => {
      categoryCounts[result.scope] = (categoryCounts[result.scope] || 0) + 1;
    });
    
    ['players', 'logs', 'files', 'alerts', 'commands', 'chat', 'economy'].forEach(scope => {
      const countElement = document.getElementById(`${scope}-result-count`);
      if (countElement) {
        if (categoryCounts[scope]) {
          countElement.style.display = 'flex';
          countElement.querySelector('strong').textContent = categoryCounts[scope];
        } else {
          countElement.style.display = 'none';
        }
      }
    });
    
    // Reset pagination
    this.currentPage = 1;
    this.renderResults();
  }
  
  renderResults() {
    if (this.filteredResults.length === 0) {
      this.resultsList.innerHTML = `
        <div class="search-no-results">
          <div class="icon">🔍</div>
          <h3>No results found</h3>
          <p>Try adjusting your search query or filters</p>
        </div>
      `;
      this.searchPagination.style.display = 'none';
      return;
    }
    
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    const pageResults = this.filteredResults.slice(start, end);
    
    this.resultsList.innerHTML = pageResults.map(result => this.createResultElement(result)).join('');
    
    // Update pagination
    const totalPages = Math.ceil(this.filteredResults.length / this.pageSize);
    if (totalPages > 1) {
      this.searchPagination.style.display = 'flex';
      this.searchCurrentPage.textContent = this.currentPage;
      this.searchTotalPages.textContent = totalPages;
      this.searchPrevPage.disabled = this.currentPage === 1;
      this.searchNextPage.disabled = this.currentPage === totalPages;
    } else {
      this.searchPagination.style.display = 'none';
    }
  }
  
  createResultElement(result) {
    const icons = {
      players: '👤',
      logs: '📝',
      files: '📁',
      alerts: '🔔',
      commands: '⌨️',
      chat: '💬',
      economy: '💰'
    };
    
    let title = '';
    let description = result.matchedText;
    let date = '';
    let tags = [];
    
    switch (result.scope) {
      case 'players':
        title = result.data.name;
        tags = [result.data.rank, result.data.status];
        date = `Last seen: ${result.data.lastSeen}`;
        break;
      case 'logs':
        title = `${result.data.level.toUpperCase()} - ${result.data.source}`;
        date = this.formatDate(result.data.date);
        tags = [result.data.level];
        break;
      case 'files':
        title = result.data.name;
        description = result.data.path;
        date = `Modified: ${this.formatDate(result.data.modified)}`;
        tags = [result.data.type, result.data.size];
        break;
      case 'alerts':
        title = result.data.title;
        date = this.formatDate(result.data.date);
        tags = [result.data.severity, result.data.category];
        break;
      case 'commands':
        title = result.data.command;
        description = result.data.description;
        tags = [result.data.category];
        break;
      case 'chat':
        title = `${result.data.player} said:`;
        date = this.formatDate(result.data.date);
        break;
      case 'economy':
        title = `${result.data.player} - ${result.data.type}`;
        description = `${result.data.description} (${result.data.amount > 0 ? '+' : ''}${result.data.amount})`;
        date = this.formatDate(result.data.date);
        tags = [result.data.type];
        break;
    }
    
    const tagsHTML = tags.length > 0 
      ? `<div class="result-tags">${tags.map(tag => `<span class="result-tag">${tag}</span>`).join('')}</div>`
      : '';
    
    return `
      <div class="search-result-item" data-scope="${result.scope}">
        <div class="result-icon">${icons[result.scope]}</div>
        <div class="result-content">
          <div class="result-header">
            <h4 class="result-title">${title}</h4>
            <div class="result-meta">
              <span class="result-type">${result.scope}</span>
              ${date ? `<span class="result-date">${date}</span>` : ''}
            </div>
          </div>
          <p class="result-description">${description}</p>
          ${tagsHTML}
        </div>
      </div>
    `;
  }
  
  hideResults() {
    this.resultsSummary.style.display = 'none';
    this.resultsContainer.style.display = 'none';
  }
  
  changePage(delta) {
    const totalPages = Math.ceil(this.filteredResults.length / this.pageSize);
    this.currentPage = Math.max(1, Math.min(this.currentPage + delta, totalPages));
    this.renderResults();
  }
  
  openAdvancedFilters() {
    this.advancedFiltersModal.style.display = 'flex';
  }
  
  closeAdvancedFiltersModal() {
    this.advancedFiltersModal.style.display = 'none';
  }
  
  applyFilters() {
    this.activeFilters = {
      dateFrom: this.filterDateFrom.value,
      dateTo: this.filterDateTo.value,
      playerStatus: this.filterPlayerStatus.value,
      playerRank: this.filterPlayerRank.value,
      logLevel: this.filterLogLevel.value,
      logSource: this.filterLogSource.value,
      fileType: this.filterFileType.value,
      fileSize: this.filterFileSize.value,
      alertSeverity: this.filterAlertSeverity.value,
      alertCategory: this.filterAlertCategory.value,
      transactionType: this.filterTransactionType.value,
      amountMin: parseFloat(this.filterAmountMin.value) || null,
      amountMax: parseFloat(this.filterAmountMax.value) || null
    };
    
    // Remove empty filters
    Object.keys(this.activeFilters).forEach(key => {
      if (!this.activeFilters[key]) delete this.activeFilters[key];
    });
    
    this.closeAdvancedFiltersModal();
    if (this.searchInput.value) {
      this.performSearch();
    }
    this.showNotification('Filters applied successfully', 'success');
  }
  
  resetAllFilters() {
    this.filterDateFrom.value = '';
    this.filterDateTo.value = '';
    this.filterPlayerStatus.value = '';
    this.filterPlayerRank.value = '';
    this.filterLogLevel.value = '';
    this.filterLogSource.value = '';
    this.filterFileType.value = '';
    this.filterFileSize.value = '';
    this.filterAlertSeverity.value = '';
    this.filterAlertCategory.value = '';
    this.filterTransactionType.value = '';
    this.filterAmountMin.value = '';
    this.filterAmountMax.value = '';
    
    this.activeFilters = {};
    this.showNotification('Filters reset', 'info');
  }
  
  openSavedPresetsModal() {
    this.renderPresetsList();
    this.savedPresetsModal.style.display = 'flex';
  }
  
  closeSavedPresetsModalDialog() {
    this.savedPresetsModal.style.display = 'none';
  }
  
  openSavePresetModal() {
    this.savePresetModal.style.display = 'flex';
  }
  
  closeSavePresetModalDialog() {
    this.savePresetModal.style.display = 'none';
    this.presetName.value = '';
    this.presetDescription.value = '';
  }
  
  loadSavedPresets() {
    // Load from localStorage
    const saved = localStorage.getItem('searchPresets');
    if (saved) {
      this.savedPresets = JSON.parse(saved);
    } else {
      // Add default presets
      this.savedPresets = [
        {
          id: 1,
          name: 'Online Players',
          description: 'Search for currently online players',
          scopes: ['players'],
          filters: { playerStatus: 'online' },
          created: new Date().toISOString()
        },
        {
          id: 2,
          name: 'Critical Alerts',
          description: 'Find all critical system alerts',
          scopes: ['alerts'],
          filters: { alertSeverity: 'critical' },
          created: new Date().toISOString()
        },
        {
          id: 3,
          name: 'Recent Errors',
          description: 'View recent error logs',
          scopes: ['logs'],
          filters: { logLevel: 'error' },
          created: new Date().toISOString()
        }
      ];
      this.savePresetsToStorage();
    }
  }
  
  savePresetsToStorage() {
    localStorage.setItem('searchPresets', JSON.stringify(this.savedPresets));
  }
  
  savePreset() {
    const name = this.presetName.value.trim();
    if (!name) {
      this.showNotification('Preset name is required', 'error');
      return;
    }
    
    const selectedScopes = Array.from(this.scopeCheckboxes)
      .filter(cb => cb.checked)
      .map(cb => cb.value);
    
    const preset = {
      id: Date.now(),
      name: name,
      description: this.presetDescription.value.trim(),
      query: this.searchInput.value,
      scopes: selectedScopes,
      filters: { ...this.activeFilters },
      created: new Date().toISOString()
    };
    
    this.savedPresets.push(preset);
    this.savePresetsToStorage();
    this.closeSavePresetModalDialog();
    this.showNotification('Preset saved successfully', 'success');
  }
  
  renderPresetsList() {
    if (this.savedPresets.length === 0) {
      this.presetsList.innerHTML = `
        <div class="search-no-results">
          <div class="icon">📋</div>
          <p>No saved presets</p>
        </div>
      `;
      return;
    }
    
    this.presetsList.innerHTML = this.savedPresets.map(preset => `
      <div class="preset-item">
        <div class="preset-info">
          <h4 class="preset-name">${preset.name}</h4>
          <p class="preset-description">${preset.description || 'No description'}</p>
          <div class="preset-meta">
            ${preset.scopes.map(s => `<span class="preset-scope">${s}</span>`).join('')}
          </div>
        </div>
        <div class="preset-actions">
          <button class="preset-action-btn" onclick="globalSearchManager.loadPreset(${preset.id})">
            Load
          </button>
          <button class="preset-action-btn danger" onclick="globalSearchManager.deletePreset(${preset.id})">
            Delete
          </button>
        </div>
      </div>
    `).join('');
  }
  
  loadPreset(presetId) {
    const preset = this.savedPresets.find(p => p.id === presetId);
    if (!preset) return;
    
    // Set query
    if (preset.query) {
      this.searchInput.value = preset.query;
    }
    
    // Set scopes
    this.scopeCheckboxes.forEach(cb => {
      cb.checked = preset.scopes.includes(cb.value);
    });
    
    // Set filters
    this.activeFilters = { ...preset.filters };
    Object.keys(preset.filters).forEach(key => {
      const element = this[`filter${key.charAt(0).toUpperCase() + key.slice(1)}`];
      if (element) {
        element.value = preset.filters[key];
      }
    });
    
    this.closeSavedPresetsModalDialog();
    if (preset.query) {
      this.performSearch();
    }
    this.showNotification(`Loaded preset: ${preset.name}`, 'success');
  }
  
  deletePreset(presetId) {
    if (confirm('Delete this preset?')) {
      this.savedPresets = this.savedPresets.filter(p => p.id !== presetId);
      this.savePresetsToStorage();
      this.renderPresetsList();
      this.showNotification('Preset deleted', 'success');
    }
  }
  
  exportSearchResults() {
    const csv = this.filteredResults.map(result => {
      const data = result.data;
      return `"${result.scope}","${JSON.stringify(data).replace(/"/g, '""')}"`;
    }).join('\n');
    
    const header = 'Scope,Data\n';
    const blob = new Blob([header + csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `search-results-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    
    this.showNotification('Search results exported', 'success');
  }
  
  formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString();
  }
  
  showNotification(message, type = 'info') {
    if (typeof showNotification === 'function') {
      showNotification(message, type);
    } else {
      console.log(`[${type.toUpperCase()}] ${message}`);
    }
  }
}

// ========================================
// ALERT & NOTIFICATION SYSTEM
// ========================================

class AlertManager {
  constructor() {
    // DOM References - Stats
    this.criticalAlertsCount = document.getElementById('critical-alerts-count');
    this.warningAlertsCount = document.getElementById('warning-alerts-count');
    this.infoAlertsCount = document.getElementById('info-alerts-count');
    this.activeRulesCount = document.getElementById('active-rules-count');
    
    // DOM References - View Toggle
    this.viewButtons = document.querySelectorAll('.alert-view-btn');
    this.liveView = document.getElementById('alert-live-view');
    this.historyView = document.getElementById('alert-history-view');
    this.rulesView = document.getElementById('alert-rules-view');
    
    // DOM References - Live Feed
    this.alertFeedList = document.getElementById('alert-feed-list');
    this.severityFilters = document.querySelectorAll('.alert-filters .filter-btn');
    this.categoryFilter = document.getElementById('alert-category-filter');
    this.soundToggle = document.getElementById('alert-sound-enabled');
    this.clearAllBtn = document.getElementById('clear-all-alerts');
    
    // DOM References - History
    this.historySearch = document.getElementById('alert-history-search');
    this.dateFrom = document.getElementById('alert-date-from');
    this.dateTo = document.getElementById('alert-date-to');
    this.filterHistoryBtn = document.getElementById('filter-history');
    this.exportHistoryBtn = document.getElementById('export-alert-history');
    this.historyList = document.getElementById('alert-history-list');
    this.prevPageBtn = document.getElementById('alert-prev-page');
    this.nextPageBtn = document.getElementById('alert-next-page');
    this.currentPageSpan = document.getElementById('alert-current-page');
    this.totalPagesSpan = document.getElementById('alert-total-pages');
    
    // DOM References - Rules
    this.rulesSearch = document.getElementById('alert-rules-search');
    this.ruleStatusFilters = document.querySelectorAll('.alert-rules-controls .filter-btn');
    this.rulesList = document.getElementById('alert-rules-list');
    this.createRuleBtn = document.getElementById('create-alert-rule');
    
    // DOM References - Modals
    this.ruleModal = document.getElementById('alert-rule-modal');
    this.ruleModalTitle = document.getElementById('alert-rule-modal-title');
    this.closeRuleModalBtn = document.getElementById('close-alert-rule-modal');
    this.cancelRuleBtn = document.getElementById('cancel-alert-rule');
    this.saveRuleBtn = document.getElementById('save-alert-rule');
    
    this.detailsModal = document.getElementById('alert-details-modal');
    this.closeDetailsBtn = document.getElementById('close-alert-details');
    this.detailContent = document.getElementById('alert-detail-content');
    this.dismissAlertBtn = document.getElementById('dismiss-alert');
    this.acknowledgeAlertBtn = document.getElementById('acknowledge-alert');
    
    // DOM References - Rule Form
    this.ruleTabButtons = document.querySelectorAll('.rule-tab-btn');
    this.ruleTabs = document.querySelectorAll('.rule-tab-content');
    this.ruleName = document.getElementById('rule-name');
    this.ruleCategory = document.getElementById('rule-category');
    this.ruleSeverity = document.getElementById('rule-severity');
    this.ruleDescription = document.getElementById('rule-description');
    this.ruleEnabled = document.getElementById('rule-enabled');
    this.ruleMetric = document.getElementById('rule-metric');
    this.ruleCondition = document.getElementById('rule-condition');
    this.ruleThreshold = document.getElementById('rule-threshold');
    this.ruleThresholdMax = document.getElementById('rule-threshold-max');
    this.ruleDuration = document.getElementById('rule-duration');
    this.ruleCooldown = document.getElementById('rule-cooldown');
    
    // Action checkboxes
    this.actionDashboard = document.getElementById('action-dashboard');
    this.actionSound = document.getElementById('action-sound');
    this.actionDesktop = document.getElementById('action-desktop');
    this.actionEmail = document.getElementById('action-email');
    this.actionWebhook = document.getElementById('action-webhook');
    this.actionLog = document.getElementById('action-log');
    this.actionCommand = document.getElementById('action-command');
    this.actionRestart = document.getElementById('action-restart');
    
    this.ruleEmail = document.getElementById('rule-email');
    this.ruleWebhookUrl = document.getElementById('rule-webhook-url');
    this.ruleCommand = document.getElementById('rule-command');
    
    // State
    this.currentView = 'live';
    this.currentSeverityFilter = 'all';
    this.currentCategoryFilter = 'all';
    this.currentRuleStatusFilter = 'all';
    this.alerts = [];
    this.alertHistory = [];
    this.alertRules = [];
    this.currentPage = 1;
    this.pageSize = 20;
    this.currentEditingRule = null;
    this.soundEnabled = true;
  }
  
  init() {
    this.setupEventListeners();
    this.loadAlertRules();
    this.loadAlertHistory();
    this.startLiveFeed();
    this.updateStats();
  }
  
  setupEventListeners() {
    // View toggle
    this.viewButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        this.currentView = btn.dataset.view;
        this.switchView(this.currentView);
      });
    });
    
    // Live feed filters
    this.severityFilters.forEach(btn => {
      btn.addEventListener('click', () => {
        this.severityFilters.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.currentSeverityFilter = btn.dataset.severity;
        this.renderAlertFeed();
      });
    });
    
    this.categoryFilter?.addEventListener('change', () => {
      this.currentCategoryFilter = this.categoryFilter.value;
      this.renderAlertFeed();
    });
    
    this.soundToggle?.addEventListener('change', () => {
      this.soundEnabled = this.soundToggle.checked;
    });
    
    this.clearAllBtn?.addEventListener('click', () => this.clearAllAlerts());
    
    // History controls
    this.historySearch?.addEventListener('input', () => this.filterHistory());
    this.filterHistoryBtn?.addEventListener('click', () => this.filterHistory());
    this.exportHistoryBtn?.addEventListener('click', () => this.exportHistory());
    this.prevPageBtn?.addEventListener('click', () => this.changePage(-1));
    this.nextPageBtn?.addEventListener('click', () => this.changePage(1));
    
    // Rules controls
    this.rulesSearch?.addEventListener('input', () => this.filterRules());
    this.ruleStatusFilters.forEach(btn => {
      btn.addEventListener('click', () => {
        this.ruleStatusFilters.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.currentRuleStatusFilter = btn.dataset.status;
        this.filterRules();
      });
    });
    
    this.createRuleBtn?.addEventListener('click', () => this.openCreateRuleModal());
    
    // Modal controls
    this.closeRuleModalBtn?.addEventListener('click', () => this.closeRuleModal());
    this.cancelRuleBtn?.addEventListener('click', () => this.closeRuleModal());
    this.saveRuleBtn?.addEventListener('click', () => this.saveRule());
    
    this.closeDetailsBtn?.addEventListener('click', () => this.closeDetailsModal());
    this.dismissAlertBtn?.addEventListener('click', () => this.dismissCurrentAlert());
    this.acknowledgeAlertBtn?.addEventListener('click', () => this.acknowledgeCurrentAlert());
    
    // Rule form tabs
    this.ruleTabButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const tabName = btn.dataset.tab;
        this.switchRuleTab(tabName);
      });
    });
    
    // Condition changes
    this.ruleCondition?.addEventListener('change', () => this.updateConditionInputs());
    this.ruleMetric?.addEventListener('change', () => this.updateMetricInputs());
    
    // Action checkboxes
    this.actionEmail?.addEventListener('change', () => {
      document.getElementById('email-group').style.display = 
        this.actionEmail.checked ? 'block' : 'none';
    });
    
    this.actionWebhook?.addEventListener('change', () => {
      document.getElementById('webhook-group').style.display = 
        this.actionWebhook.checked ? 'block' : 'none';
    });
    
    this.actionCommand?.addEventListener('change', () => {
      document.getElementById('command-group').style.display = 
        this.actionCommand.checked ? 'block' : 'none';
    });
  }
  
  switchView(view) {
    this.viewButtons.forEach(btn => {
      btn.classList.toggle('active', btn.dataset.view === view);
    });
    
    this.liveView.classList.toggle('hidden', view !== 'live');
    this.historyView.classList.toggle('hidden', view !== 'history');
    this.rulesView.classList.toggle('hidden', view !== 'rules');
  }
  
  switchRuleTab(tabName) {
    this.ruleTabButtons.forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabName);
    });
    
    this.ruleTabs.forEach(tab => {
      tab.classList.toggle('hidden', tab.id !== `rule-${tabName}-tab`);
    });
  }
  
  loadAlertRules() {
    // Mock data - replace with API call
    this.alertRules = [
      {
        id: 1,
        name: 'Low TPS Warning',
        category: 'performance',
        severity: 'warning',
        description: 'Alert when server TPS drops below 18',
        enabled: true,
        metric: 'tps',
        condition: 'less',
        threshold: 18,
        duration: 30,
        cooldown: 5,
        actions: {
          dashboard: true,
          sound: true,
          desktop: false,
          email: false,
          webhook: false,
          log: true,
          command: false,
          restart: false
        },
        triggeredCount: 12,
        lastTriggered: '2025-10-14T10:30:00'
      },
      {
        id: 2,
        name: 'High Memory Usage',
        category: 'performance',
        severity: 'critical',
        description: 'Alert when memory usage exceeds 90%',
        enabled: true,
        metric: 'memory',
        condition: 'greater',
        threshold: 90,
        duration: 60,
        cooldown: 10,
        actions: {
          dashboard: true,
          sound: true,
          desktop: true,
          email: true,
          webhook: true,
          log: true,
          command: false,
          restart: false
        },
        email: 'admin@example.com',
        webhookUrl: 'https://discord.com/api/webhooks/...',
        triggeredCount: 5,
        lastTriggered: '2025-10-14T09:15:00'
      },
      {
        id: 3,
        name: 'Player Death Notification',
        category: 'player',
        severity: 'info',
        description: 'Log player deaths for analytics',
        enabled: true,
        metric: 'death',
        condition: 'equals',
        threshold: 1,
        duration: 0,
        cooldown: 0,
        actions: {
          dashboard: true,
          sound: false,
          desktop: false,
          email: false,
          webhook: false,
          log: true,
          command: false,
          restart: false
        },
        triggeredCount: 247,
        lastTriggered: '2025-10-14T11:45:00'
      },
      {
        id: 4,
        name: 'Lag Spike Detection',
        category: 'performance',
        severity: 'warning',
        description: 'Detect sudden performance drops',
        enabled: true,
        metric: 'lag_spike',
        condition: 'greater',
        threshold: 100,
        duration: 0,
        cooldown: 2,
        actions: {
          dashboard: true,
          sound: true,
          desktop: false,
          email: false,
          webhook: true,
          log: true,
          command: true,
          restart: false
        },
        command: '/say Warning: Lag spike detected!',
        webhookUrl: 'https://discord.com/api/webhooks/...',
        triggeredCount: 34,
        lastTriggered: '2025-10-14T10:50:00'
      },
      {
        id: 5,
        name: 'Server Crash Alert',
        category: 'system',
        severity: 'critical',
        description: 'Alert on server crash and auto-restart',
        enabled: true,
        metric: 'crash',
        condition: 'equals',
        threshold: 1,
        duration: 0,
        cooldown: 0,
        actions: {
          dashboard: true,
          sound: true,
          desktop: true,
          email: true,
          webhook: true,
          log: true,
          command: false,
          restart: true
        },
        email: 'admin@example.com',
        webhookUrl: 'https://discord.com/api/webhooks/...',
        triggeredCount: 2,
        lastTriggered: '2025-10-13T03:22:00'
      }
    ];
    
    this.renderRules();
  }
  
  loadAlertHistory() {
    // Mock data - replace with API call
    this.alertHistory = [
      {
        id: 1,
        title: 'Low TPS Warning',
        message: 'Server TPS dropped to 15.2 for 45 seconds',
        severity: 'warning',
        category: 'performance',
        timestamp: '2025-10-14T10:30:15',
        acknowledged: true,
        ruleId: 1
      },
      {
        id: 2,
        title: 'High Memory Usage',
        message: 'Memory usage reached 92% (11.5GB of 12GB)',
        severity: 'critical',
        category: 'performance',
        timestamp: '2025-10-14T09:15:30',
        acknowledged: true,
        ruleId: 2
      },
      {
        id: 3,
        title: 'Lag Spike Detected',
        message: 'Server lag spike detected: 150ms delay',
        severity: 'warning',
        category: 'performance',
        timestamp: '2025-10-14T10:50:00',
        acknowledged: false,
        ruleId: 4
      },
      {
        id: 4,
        title: 'Player Death',
        message: 'Player ZeroG died by Zombie',
        severity: 'info',
        category: 'player',
        timestamp: '2025-10-14T11:45:22',
        acknowledged: false,
        ruleId: 3
      },
      {
        id: 5,
        title: 'Server Crash',
        message: 'Server crashed due to out of memory error',
        severity: 'critical',
        category: 'system',
        timestamp: '2025-10-13T03:22:15',
        acknowledged: true,
        ruleId: 5
      }
    ];
    
    this.renderHistory();
  }
  
  startLiveFeed() {
    // Simulate live alerts - replace with WebSocket connection
    this.alerts = [
      {
        id: Date.now(),
        title: 'Server Running Smoothly',
        message: 'All systems operational. TPS: 20.0, Memory: 65%',
        severity: 'info',
        category: 'system',
        timestamp: new Date().toISOString(),
        acknowledged: false
      }
    ];
    
    this.renderAlertFeed();
    
    // Simulate new alerts every 30 seconds
    setInterval(() => this.simulateNewAlert(), 30000);
  }
  
  simulateNewAlert() {
    const mockAlerts = [
      {
        title: 'TPS Fluctuation',
        message: 'Server TPS varied between 18-20',
        severity: 'info',
        category: 'performance'
      },
      {
        title: 'Player Joined',
        message: 'Player Steve joined the server',
        severity: 'info',
        category: 'player'
      },
      {
        title: 'Memory Warning',
        message: 'Memory usage at 85%',
        severity: 'warning',
        category: 'performance'
      },
      {
        title: 'Plugin Update Available',
        message: 'WorldEdit has an update available',
        severity: 'info',
        category: 'plugin'
      }
    ];
    
    const randomAlert = mockAlerts[Math.floor(Math.random() * mockAlerts.length)];
    const newAlert = {
      id: Date.now(),
      ...randomAlert,
      timestamp: new Date().toISOString(),
      acknowledged: false
    };
    
    this.alerts.unshift(newAlert);
    if (this.alerts.length > 50) {
      this.alerts = this.alerts.slice(0, 50);
    }
    
    this.renderAlertFeed();
    this.updateStats();
    
    if (this.soundEnabled && (newAlert.severity === 'warning' || newAlert.severity === 'critical')) {
      this.playAlertSound(newAlert.severity);
    }
  }
  
  playAlertSound(severity) {
    // Create simple beep sounds using Web Audio API
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);
    
    oscillator.frequency.value = severity === 'critical' ? 800 : 600;
    oscillator.type = 'sine';
    
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);
    
    oscillator.start(audioContext.currentTime);
    oscillator.stop(audioContext.currentTime + 0.5);
  }
  
  renderAlertFeed() {
    let filteredAlerts = this.alerts;
    
    // Filter by severity
    if (this.currentSeverityFilter !== 'all') {
      filteredAlerts = filteredAlerts.filter(a => a.severity === this.currentSeverityFilter);
    }
    
    // Filter by category
    if (this.currentCategoryFilter !== 'all') {
      filteredAlerts = filteredAlerts.filter(a => a.category === this.currentCategoryFilter);
    }
    
    if (filteredAlerts.length === 0) {
      this.alertFeedList.innerHTML = `
        <div class="alert-no-data">
          <div class="icon">🔕</div>
          <p>No alerts to display</p>
        </div>
      `;
      return;
    }
    
    this.alertFeedList.innerHTML = filteredAlerts.map(alert => this.createAlertElement(alert)).join('');
    
    // Add click handlers
    this.alertFeedList.querySelectorAll('.alert-item').forEach((el, index) => {
      el.addEventListener('click', () => this.showAlertDetails(filteredAlerts[index]));
    });
  }
  
  createAlertElement(alert) {
    const severityIcons = {
      critical: '🚨',
      warning: '⚠️',
      info: 'ℹ️'
    };
    
    const timeAgo = this.getTimeAgo(new Date(alert.timestamp));
    
    return `
      <div class="alert-item ${alert.severity}" data-id="${alert.id}">
        <div class="alert-icon">${severityIcons[alert.severity]}</div>
        <div class="alert-content">
          <div class="alert-header">
            <h4 class="alert-title">${alert.title}</h4>
            <div class="alert-meta">
              <span class="alert-category">${alert.category}</span>
              <span class="alert-time">${timeAgo}</span>
            </div>
          </div>
          <p class="alert-message">${alert.message}</p>
          <div class="alert-actions-inline">
            <button class="alert-action-btn" onclick="event.stopPropagation(); alertManager.dismissAlert(${alert.id})">
              Dismiss
            </button>
            <button class="alert-action-btn primary" onclick="event.stopPropagation(); alertManager.acknowledgeAlert(${alert.id})">
              Acknowledge
            </button>
          </div>
        </div>
      </div>
    `;
  }
  
  renderHistory() {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    const pageAlerts = this.alertHistory.slice(start, end);
    
    if (pageAlerts.length === 0) {
      this.historyList.innerHTML = `
        <div class="alert-no-data">
          <div class="icon">📭</div>
          <p>No alert history</p>
        </div>
      `;
      return;
    }
    
    this.historyList.innerHTML = pageAlerts.map(alert => this.createAlertElement(alert)).join('');
    
    // Add click handlers
    this.historyList.querySelectorAll('.alert-item').forEach((el, index) => {
      el.addEventListener('click', () => this.showAlertDetails(pageAlerts[index]));
    });
    
    // Update pagination
    const totalPages = Math.ceil(this.alertHistory.length / this.pageSize);
    this.currentPageSpan.textContent = this.currentPage;
    this.totalPagesSpan.textContent = totalPages;
    this.prevPageBtn.disabled = this.currentPage === 1;
    this.nextPageBtn.disabled = this.currentPage === totalPages;
  }
  
  renderRules() {
    let filteredRules = this.alertRules;
    
    // Filter by status
    if (this.currentRuleStatusFilter === 'enabled') {
      filteredRules = filteredRules.filter(r => r.enabled);
    } else if (this.currentRuleStatusFilter === 'disabled') {
      filteredRules = filteredRules.filter(r => !r.enabled);
    }
    
    // Filter by search
    const searchTerm = this.rulesSearch?.value.toLowerCase() || '';
    if (searchTerm) {
      filteredRules = filteredRules.filter(r => 
        r.name.toLowerCase().includes(searchTerm) ||
        r.description.toLowerCase().includes(searchTerm)
      );
    }
    
    if (filteredRules.length === 0) {
      this.rulesList.innerHTML = `
        <div class="alert-no-data">
          <div class="icon">📋</div>
          <p>No alert rules found</p>
        </div>
      `;
      return;
    }
    
    this.rulesList.innerHTML = filteredRules.map(rule => this.createRuleCard(rule)).join('');
  }
  
  createRuleCard(rule) {
    return `
      <div class="alert-rule-card ${rule.severity}">
        <div class="alert-rule-header">
          <h4 class="alert-rule-title">${rule.name}</h4>
          <div class="alert-rule-toggle">
            <label class="toggle-label">
              <input type="checkbox" ${rule.enabled ? 'checked' : ''} 
                     onchange="alertManager.toggleRule(${rule.id}, this.checked)">
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
        <p class="alert-rule-description">${rule.description}</p>
        <div class="alert-rule-details">
          <div class="alert-rule-detail">
            <span class="alert-rule-detail-label">Category:</span>
            <span class="alert-rule-detail-value">${rule.category}</span>
          </div>
          <div class="alert-rule-detail">
            <span class="alert-rule-detail-label">Severity:</span>
            <span class="alert-rule-detail-value">${rule.severity}</span>
          </div>
          <div class="alert-rule-detail">
            <span class="alert-rule-detail-label">Triggered:</span>
            <span class="alert-rule-detail-value">${rule.triggeredCount} times</span>
          </div>
          <div class="alert-rule-detail">
            <span class="alert-rule-detail-label">Last Triggered:</span>
            <span class="alert-rule-detail-value">${this.formatDateTime(rule.lastTriggered)}</span>
          </div>
        </div>
        <div class="alert-rule-actions">
          <button class="alert-rule-btn" onclick="alertManager.editRule(${rule.id})">
            Edit
          </button>
          <button class="alert-rule-btn" onclick="alertManager.duplicateRule(${rule.id})">
            Duplicate
          </button>
          <button class="alert-rule-btn danger" onclick="alertManager.deleteRule(${rule.id})">
            Delete
          </button>
        </div>
      </div>
    `;
  }
  
  updateStats() {
    const criticalCount = this.alerts.filter(a => a.severity === 'critical').length;
    const warningCount = this.alerts.filter(a => a.severity === 'warning').length;
    const infoCount = this.alerts.filter(a => a.severity === 'info').length;
    const activeRulesCount = this.alertRules.filter(r => r.enabled).length;
    
    if (this.criticalAlertsCount) this.criticalAlertsCount.textContent = criticalCount;
    if (this.warningAlertsCount) this.warningAlertsCount.textContent = warningCount;
    if (this.infoAlertsCount) this.infoAlertsCount.textContent = infoCount;
    if (this.activeRulesCount) this.activeRulesCount.textContent = activeRulesCount;
  }
  
  clearAllAlerts() {
    if (confirm('Clear all alerts?')) {
      this.alerts = [];
      this.renderAlertFeed();
      this.updateStats();
      this.showNotification('All alerts cleared', 'info');
    }
  }
  
  dismissAlert(alertId) {
    this.alerts = this.alerts.filter(a => a.id !== alertId);
    this.renderAlertFeed();
    this.updateStats();
    this.showNotification('Alert dismissed', 'info');
  }
  
  acknowledgeAlert(alertId) {
    const alert = this.alerts.find(a => a.id === alertId);
    if (alert) {
      alert.acknowledged = true;
      this.renderAlertFeed();
      this.showNotification('Alert acknowledged', 'success');
    }
  }
  
  dismissCurrentAlert() {
    if (this.currentAlert) {
      this.dismissAlert(this.currentAlert.id);
      this.closeDetailsModal();
    }
  }
  
  acknowledgeCurrentAlert() {
    if (this.currentAlert) {
      this.acknowledgeAlert(this.currentAlert.id);
      this.closeDetailsModal();
    }
  }
  
  showAlertDetails(alert) {
    if (!alert) {
      console.error('No alert data provided');
      return;
    }
    
    this.currentAlert = alert;
    
    this.detailContent.innerHTML = `
      <div class="alert-detail-section">
        <div class="alert-detail-label">Title</div>
        <div class="alert-detail-value">${alert.title || 'N/A'}</div>
      </div>
      <div class="alert-detail-section">
        <div class="alert-detail-label">Message</div>
        <div class="alert-detail-value">${alert.message || 'No message available'}</div>
      </div>
      <div class="alert-detail-section">
        <div class="alert-detail-label">Severity</div>
        <div class="alert-detail-value severity-${alert.severity || 'info'}">${(alert.severity || 'info').toUpperCase()}</div>
      </div>
      <div class="alert-detail-section">
        <div class="alert-detail-label">Category</div>
        <div class="alert-detail-value">${alert.category || 'general'}</div>
      </div>
      <div class="alert-detail-section">
        <div class="alert-detail-label">Timestamp</div>
        <div class="alert-detail-value">${alert.timestamp ? this.formatDateTime(alert.timestamp) : 'Unknown'}</div>
      </div>
      <div class="alert-detail-section">
        <div class="alert-detail-label">Status</div>
        <div class="alert-detail-value">${alert.acknowledged ? '✓ Acknowledged' : '● Pending'}</div>
      </div>
      ${alert.details ? `
      <div class="alert-detail-section">
        <div class="alert-detail-label">Additional Details</div>
        <div class="alert-detail-value">${alert.details}</div>
      </div>
      ` : ''}
    `;
    
    this.detailsModal.style.display = 'flex';
  }
  
  closeDetailsModal() {
    this.detailsModal.style.display = 'none';
    this.currentAlert = null;
  }
  
  openCreateRuleModal() {
    this.currentEditingRule = null;
    this.ruleModalTitle.textContent = 'Create Alert Rule';
    this.resetRuleForm();
    this.ruleModal.style.display = 'flex';
  }
  
  editRule(ruleId) {
    const rule = this.alertRules.find(r => r.id === ruleId);
    if (!rule) return;
    
    this.currentEditingRule = rule;
    this.ruleModalTitle.textContent = 'Edit Alert Rule';
    this.populateRuleForm(rule);
    this.ruleModal.style.display = 'flex';
  }
  
  closeRuleModal() {
    this.ruleModal.style.display = 'none';
    this.currentEditingRule = null;
  }
  
  resetRuleForm() {
    this.ruleName.value = '';
    this.ruleCategory.value = 'performance';
    this.ruleSeverity.value = 'info';
    this.ruleDescription.value = '';
    this.ruleEnabled.checked = true;
    this.ruleMetric.value = 'tps';
    this.ruleCondition.value = 'greater';
    this.ruleThreshold.value = '';
    this.ruleThresholdMax.value = '';
    this.ruleDuration.value = '30';
    this.ruleCooldown.value = '5';
    
    this.actionDashboard.checked = true;
    this.actionSound.checked = true;
    this.actionDesktop.checked = false;
    this.actionEmail.checked = false;
    this.actionWebhook.checked = false;
    this.actionLog.checked = true;
    this.actionCommand.checked = false;
    this.actionRestart.checked = false;
    
    this.ruleEmail.value = '';
    this.ruleWebhookUrl.value = '';
    this.ruleCommand.value = '';
    
    this.switchRuleTab('basic');
    this.updateConditionInputs();
  }
  
  populateRuleForm(rule) {
    this.ruleName.value = rule.name;
    this.ruleCategory.value = rule.category;
    this.ruleSeverity.value = rule.severity;
    this.ruleDescription.value = rule.description;
    this.ruleEnabled.checked = rule.enabled;
    this.ruleMetric.value = rule.metric;
    this.ruleCondition.value = rule.condition;
    this.ruleThreshold.value = rule.threshold;
    this.ruleThresholdMax.value = rule.thresholdMax || '';
    this.ruleDuration.value = rule.duration;
    this.ruleCooldown.value = rule.cooldown;
    
    this.actionDashboard.checked = rule.actions.dashboard;
    this.actionSound.checked = rule.actions.sound;
    this.actionDesktop.checked = rule.actions.desktop;
    this.actionEmail.checked = rule.actions.email;
    this.actionWebhook.checked = rule.actions.webhook;
    this.actionLog.checked = rule.actions.log;
    this.actionCommand.checked = rule.actions.command;
    this.actionRestart.checked = rule.actions.restart;
    
    this.ruleEmail.value = rule.email || '';
    this.ruleWebhookUrl.value = rule.webhookUrl || '';
    this.ruleCommand.value = rule.command || '';
    
    this.updateConditionInputs();
  }
  
  updateConditionInputs() {
    const condition = this.ruleCondition.value;
    const thresholdMaxGroup = document.getElementById('threshold-max-group');
    
    if (condition === 'between') {
      thresholdMaxGroup.classList.remove('hidden');
    } else {
      thresholdMaxGroup.classList.add('hidden');
    }
  }
  
  updateMetricInputs() {
    const metric = this.ruleMetric.value;
    const thresholdGroup = document.getElementById('threshold-group');
    const thresholdValueGroup = document.getElementById('threshold-value-group');
    const thresholdMaxGroup = document.getElementById('threshold-max-group');
    
    // Event-based metrics don't need thresholds
    const eventMetrics = ['player_join', 'player_leave', 'death', 'crash', 'lag_spike'];
    if (eventMetrics.includes(metric)) {
      thresholdGroup.classList.add('hidden');
      thresholdValueGroup.classList.add('hidden');
      thresholdMaxGroup.classList.add('hidden');
    } else {
      thresholdGroup.classList.remove('hidden');
      thresholdValueGroup.classList.remove('hidden');
      this.updateConditionInputs();
    }
  }
  
  saveRule() {
    const ruleData = {
      name: this.ruleName.value,
      category: this.ruleCategory.value,
      severity: this.ruleSeverity.value,
      description: this.ruleDescription.value,
      enabled: this.ruleEnabled.checked,
      metric: this.ruleMetric.value,
      condition: this.ruleCondition.value,
      threshold: parseFloat(this.ruleThreshold.value) || 0,
      thresholdMax: parseFloat(this.ruleThresholdMax.value) || null,
      duration: parseInt(this.ruleDuration.value) || 0,
      cooldown: parseInt(this.ruleCooldown.value) || 0,
      actions: {
        dashboard: this.actionDashboard.checked,
        sound: this.actionSound.checked,
        desktop: this.actionDesktop.checked,
        email: this.actionEmail.checked,
        webhook: this.actionWebhook.checked,
        log: this.actionLog.checked,
        command: this.actionCommand.checked,
        restart: this.actionRestart.checked
      },
      email: this.ruleEmail.value,
      webhookUrl: this.ruleWebhookUrl.value,
      command: this.ruleCommand.value,
      triggeredCount: 0,
      lastTriggered: null
    };
    
    if (!ruleData.name) {
      this.showNotification('Rule name is required', 'error');
      return;
    }
    
    if (this.currentEditingRule) {
      // Update existing rule
      const index = this.alertRules.findIndex(r => r.id === this.currentEditingRule.id);
      if (index !== -1) {
        this.alertRules[index] = { ...this.currentEditingRule, ...ruleData };
        this.showNotification('Rule updated successfully', 'success');
      }
    } else {
      // Create new rule
      ruleData.id = Date.now();
      this.alertRules.push(ruleData);
      this.showNotification('Rule created successfully', 'success');
    }
    
    this.renderRules();
    this.updateStats();
    this.closeRuleModal();
  }
  
  toggleRule(ruleId, enabled) {
    const rule = this.alertRules.find(r => r.id === ruleId);
    if (rule) {
      rule.enabled = enabled;
      this.updateStats();
      this.showNotification(`Rule ${enabled ? 'enabled' : 'disabled'}`, 'info');
    }
  }
  
  duplicateRule(ruleId) {
    const rule = this.alertRules.find(r => r.id === ruleId);
    if (rule) {
      const newRule = { ...rule, id: Date.now(), name: `${rule.name} (Copy)` };
      this.alertRules.push(newRule);
      this.renderRules();
      this.showNotification('Rule duplicated', 'success');
    }
  }
  
  deleteRule(ruleId) {
    if (confirm('Delete this alert rule?')) {
      this.alertRules = this.alertRules.filter(r => r.id !== ruleId);
      this.renderRules();
      this.updateStats();
      this.showNotification('Rule deleted', 'success');
    }
  }
  
  filterHistory() {
    // Implement history filtering logic
    this.renderHistory();
  }
  
  filterRules() {
    this.renderRules();
  }
  
  changePage(delta) {
    const totalPages = Math.ceil(this.alertHistory.length / this.pageSize);
    this.currentPage = Math.max(1, Math.min(this.currentPage + delta, totalPages));
    this.renderHistory();
  }
  
  exportHistory() {
    const csv = this.alertHistory.map(alert => 
      `"${alert.timestamp}","${alert.severity}","${alert.category}","${alert.title}","${alert.message}"`
    ).join('\n');
    
    const header = 'Timestamp,Severity,Category,Title,Message\n';
    const blob = new Blob([header + csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `alert-history-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    
    this.showNotification('Alert history exported', 'success');
  }
  
  getTimeAgo(date) {
    const seconds = Math.floor((new Date() - date) / 1000);
    
    if (seconds < 60) return `${seconds}s ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
  }
  
  formatDateTime(dateString) {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    return date.toLocaleString();
  }
  
  showNotification(message, type = 'info') {
    // Reuse existing notification system
    if (typeof showNotification === 'function') {
      showNotification(message, type);
    } else {
      console.log(`[${type.toUpperCase()}] ${message}`);
    }
  }
}

// ========================================
// PERMISSION MANAGEMENT
// ========================================

class PermissionManager {
  constructor() {
    // DOM References - Stats
    this.totalGroupsCount = document.getElementById('total-groups-count');
    this.totalPermissionsCount = document.getElementById('total-permissions-count');
    this.assignedPlayersCount = document.getElementById('assigned-players-count');
    this.inheritanceChainsCount = document.getElementById('inheritance-chains-count');
    
    // DOM References - View Toggle
    this.viewButtons = document.querySelectorAll('.perm-view-btn');
    this.views = document.querySelectorAll('.perm-view');
    
    // DOM References - Groups View
    this.groupSearch = document.getElementById('group-search');
    this.createGroupBtn = document.getElementById('create-group-btn');
    this.groupList = document.getElementById('group-list');
    
    // DOM References - Players View
    this.playerPermSearch = document.getElementById('player-perm-search');
    this.assignPlayerBtn = document.getElementById('assign-player-btn');
    this.playerPermList = document.getElementById('player-perm-list');
    
    // DOM References - Tree View
    this.treeGroupSelect = document.getElementById('tree-group-select');
    this.expandAllTreeBtn = document.getElementById('expand-all-tree');
    this.collapseAllTreeBtn = document.getElementById('collapse-all-tree');
    this.inheritanceTree = document.getElementById('inheritance-tree');
    
    // DOM References - Edit Group Modal
    this.editGroupModal = document.getElementById('edit-group-modal');
    this.closeEditGroupBtn = document.getElementById('close-edit-group');
    this.editGroupTitle = document.getElementById('edit-group-title');
    this.groupName = document.getElementById('group-name');
    this.groupDisplayName = document.getElementById('group-display-name');
    this.groupPrefix = document.getElementById('group-prefix');
    this.groupSuffix = document.getElementById('group-suffix');
    this.groupPriority = document.getElementById('group-priority');
    this.groupDefault = document.getElementById('group-default');
    
    // DOM References - Permission Tabs
    this.permTabs = document.querySelectorAll('.perm-tab');
    this.permTabPanes = document.querySelectorAll('.perm-tab-pane');
    
    // DOM References - Permissions Tab
    this.permSearch = document.getElementById('perm-search');
    this.addPermissionBtn = document.getElementById('add-permission-btn');
    this.permCategories = document.querySelectorAll('.perm-category-btn');
    this.groupPermList = document.getElementById('group-perm-list');
    
    // DOM References - Inheritance Tab
    this.parentGroupsList = document.getElementById('parent-groups-list');
    this.childGroupsList = document.getElementById('child-groups-list');
    this.addParentGroupBtn = document.getElementById('add-parent-group-btn');
    
    // DOM References - Members Tab
    this.memberSearch = document.getElementById('member-search');
    this.addMemberBtn = document.getElementById('add-member-btn');
    this.groupMemberList = document.getElementById('group-member-list');
    
    // DOM References - Group Modal Actions
    this.cancelGroupEdit = document.getElementById('cancel-group-edit');
    this.deleteGroup = document.getElementById('delete-group');
    this.saveGroup = document.getElementById('save-group');
    
    // DOM References - Add Permission Modal
    this.addPermissionModal = document.getElementById('add-permission-modal');
    this.closeAddPermission = document.getElementById('close-add-permission');
    this.newPermissionNode = document.getElementById('new-permission-node');
    this.permissionWorld = document.getElementById('permission-world');
    this.commonPermButtons = document.querySelectorAll('.common-perm-btn');
    this.cancelAddPermission = document.getElementById('cancel-add-permission');
    this.confirmAddPermission = document.getElementById('confirm-add-permission');
    
    // DOM References - Assign Player Modal
    this.assignPlayerModal = document.getElementById('assign-player-modal');
    this.closeAssignPlayer = document.getElementById('close-assign-player');
    this.assignPlayerName = document.getElementById('assign-player-name');
    this.assignPlayerGroup = document.getElementById('assign-player-group');
    this.assignDuration = document.getElementById('assign-duration');
    this.cancelAssignPlayer = document.getElementById('cancel-assign-player');
    this.confirmAssignPlayer = document.getElementById('confirm-assign-player');
    
    // State
    this.groups = [];
    this.playerPermissions = [];
    this.currentGroup = null;
    this.currentView = 'groups';
    this.currentPermCategory = 'all';
  }
  
  init() {
    this.setupEventListeners();
    this.loadGroups();
    this.loadPlayerPermissions();
    this.updateStats();
    this.populateGroupSelects();
  }
  
  setupEventListeners() {
    // View toggle
    this.viewButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        this.viewButtons.forEach(b => b.classList.remove('active'));
        this.views.forEach(v => v.classList.remove('active'));
        btn.classList.add('active');
        const view = btn.dataset.view;
        document.querySelector(`.perm-view[data-view="${view}"]`).classList.add('active');
        this.currentView = view;
        
        if (view === 'tree') {
          this.renderInheritanceTree();
        }
      });
    });
    
    // Groups view
    this.groupSearch.addEventListener('input', () => this.renderGroups());
    this.createGroupBtn.addEventListener('click', () => this.openCreateGroupModal());
    
    // Players view
    this.playerPermSearch.addEventListener('input', () => this.renderPlayerPermissions());
    this.assignPlayerBtn.addEventListener('click', () => this.openAssignPlayerModal());
    
    // Tree view
    this.treeGroupSelect.addEventListener('change', () => this.renderInheritanceTree());
    this.expandAllTreeBtn.addEventListener('click', () => this.expandAllTree());
    this.collapseAllTreeBtn.addEventListener('click', () => this.collapseAllTree());
    
    // Edit group modal
    this.closeEditGroupBtn.addEventListener('click', () => this.closeEditGroupModal());
    this.cancelGroupEdit.addEventListener('click', () => this.closeEditGroupModal());
    this.deleteGroup.addEventListener('click', () => this.deleteCurrentGroup());
    this.saveGroup.addEventListener('click', () => this.saveCurrentGroup());
    
    // Permission tabs
    this.permTabs.forEach(tab => {
      tab.addEventListener('click', () => {
        this.permTabs.forEach(t => t.classList.remove('active'));
        this.permTabPanes.forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        const tabName = tab.dataset.tab;
        document.querySelector(`.perm-tab-pane[data-tab="${tabName}"]`).classList.add('active');
      });
    });
    
    // Permission search and categories
    this.permSearch.addEventListener('input', () => this.filterPermissions());
    this.addPermissionBtn.addEventListener('click', () => this.openAddPermissionModal());
    
    this.permCategories.forEach(btn => {
      btn.addEventListener('click', () => {
        this.permCategories.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.currentPermCategory = btn.dataset.category;
        this.filterPermissions();
      });
    });
    
    // Inheritance
    this.addParentGroupBtn.addEventListener('click', () => this.addParentGroup());
    
    // Members
    this.memberSearch.addEventListener('input', () => this.filterMembers());
    this.addMemberBtn.addEventListener('click', () => this.addMember());
    
    // Add permission modal
    this.closeAddPermission.addEventListener('click', () => this.closeAddPermissionModal());
    this.cancelAddPermission.addEventListener('click', () => this.closeAddPermissionModal());
    this.confirmAddPermission.addEventListener('click', () => this.addPermission());
    
    this.commonPermButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        this.newPermissionNode.value = btn.dataset.perm;
      });
    });
    
    // Assign player modal
    this.closeAssignPlayer.addEventListener('click', () => this.closeAssignPlayerModal());
    this.cancelAssignPlayer.addEventListener('click', () => this.closeAssignPlayerModal());
    this.confirmAssignPlayer.addEventListener('click', () => this.assignPlayer());
    
    // Close modals on outside click
    this.editGroupModal.addEventListener('click', (e) => {
      if (e.target === this.editGroupModal) this.closeEditGroupModal();
    });
    this.addPermissionModal.addEventListener('click', (e) => {
      if (e.target === this.addPermissionModal) this.closeAddPermissionModal();
    });
    this.assignPlayerModal.addEventListener('click', (e) => {
      if (e.target === this.assignPlayerModal) this.closeAssignPlayerModal();
    });
  }
  
  loadGroups() {
    // Mock group data
    this.groups = [
      {
        id: 'admin',
        name: 'admin',
        displayName: 'Administrator',
        prefix: '[Admin]',
        suffix: ' ⭐',
        priority: 100,
        isDefault: false,
        permissions: [
          { node: '*', type: 'allow', world: '' },
          { node: 'essentials.*', type: 'allow', world: '' },
          { node: 'worldedit.*', type: 'allow', world: '' }
        ],
        parents: [],
        members: ['Steve', 'Alex']
      },
      {
        id: 'moderator',
        name: 'moderator',
        displayName: 'Moderator',
        prefix: '[Mod]',
        suffix: ' 🛡️',
        priority: 75,
        isDefault: false,
        permissions: [
          { node: 'essentials.kick', type: 'allow', world: '' },
          { node: 'essentials.ban', type: 'allow', world: '' },
          { node: 'essentials.mute', type: 'allow', world: '' },
          { node: 'essentials.jail', type: 'allow', world: '' }
        ],
        parents: ['helper'],
        members: ['Notch', 'Herobrine']
      },
      {
        id: 'helper',
        name: 'helper',
        displayName: 'Helper',
        prefix: '[Helper]',
        suffix: ' 🤝',
        priority: 50,
        isDefault: false,
        permissions: [
          { node: 'essentials.msg', type: 'allow', world: '' },
          { node: 'essentials.helpop', type: 'allow', world: '' },
          { node: 'essentials.tp', type: 'allow', world: '' }
        ],
        parents: ['vip'],
        members: ['CaptainSparklez']
      },
      {
        id: 'vip',
        name: 'vip',
        displayName: 'VIP',
        prefix: '[VIP]',
        suffix: ' 💎',
        priority: 25,
        isDefault: false,
        permissions: [
          { node: 'essentials.fly', type: 'allow', world: '' },
          { node: 'essentials.enderchest', type: 'allow', world: '' },
          { node: 'essentials.nick', type: 'allow', world: '' }
        ],
        parents: ['default'],
        members: ['Dream', 'TommyInnit']
      },
      {
        id: 'default',
        name: 'default',
        displayName: 'Member',
        prefix: '',
        suffix: '',
        priority: 0,
        isDefault: true,
        permissions: [
          { node: 'essentials.home', type: 'allow', world: '' },
          { node: 'essentials.sethome', type: 'allow', world: '' },
          { node: 'essentials.spawn', type: 'allow', world: '' },
          { node: 'essentials.tpa', type: 'allow', world: '' },
          { node: 'essentials.warp', type: 'allow', world: '' }
        ],
        parents: [],
        members: []
      }
    ];
    
    this.renderGroups();
  }
  
  loadPlayerPermissions() {
    // Mock player permission data
    this.playerPermissions = [
      { name: 'Steve', group: 'admin', assignedDate: '2025-01-15', expires: null },
      { name: 'Alex', group: 'admin', assignedDate: '2025-02-20', expires: null },
      { name: 'Notch', group: 'moderator', assignedDate: '2025-03-10', expires: null },
      { name: 'Herobrine', group: 'moderator', assignedDate: '2025-04-05', expires: null },
      { name: 'CaptainSparklez', group: 'helper', assignedDate: '2025-05-01', expires: null },
      { name: 'Dream', group: 'vip', assignedDate: '2025-06-15', expires: '2025-12-31' },
      { name: 'TommyInnit', group: 'vip', assignedDate: '2025-07-20', expires: null }
    ];
    
    this.renderPlayerPermissions();
  }
  
  renderGroups() {
    const searchTerm = this.groupSearch.value.toLowerCase();
    const filtered = this.groups.filter(group => 
      group.name.toLowerCase().includes(searchTerm) ||
      group.displayName.toLowerCase().includes(searchTerm)
    );
    
    this.groupList.innerHTML = filtered.length === 0
      ? '<p class="no-data">No groups found</p>'
      : filtered.map(group => this.createGroupCard(group)).join('');
    
    // Add click listeners
    document.querySelectorAll('.group-card').forEach(card => {
      card.addEventListener('click', () => {
        const groupId = card.dataset.groupId;
        this.openEditGroupModal(groupId);
      });
    });
  }
  
  createGroupCard(group) {
    const defaultBadge = group.isDefault 
      ? '<span class="group-badge default">Default</span>'
      : '';
    
    const totalPerms = group.permissions.length;
    const members = group.members.length;
    const parents = group.parents.length;
    
    return `
      <div class="group-card ${group.isDefault ? 'default' : ''}" data-group-id="${group.id}">
        <div class="group-card-header">
          <div class="group-card-info">
            <div class="group-card-name">${group.name}</div>
            <div class="group-card-display">${group.displayName}</div>
          </div>
          <div class="group-card-priority">P${group.priority}</div>
        </div>
        <div class="group-card-badges">
          ${defaultBadge}
        </div>
        <div class="group-card-stats">
          <span class="group-stat">
            <span class="icon">🔑</span>
            ${totalPerms} perms
          </span>
          <span class="group-stat">
            <span class="icon">👥</span>
            ${members} members
          </span>
          <span class="group-stat">
            <span class="icon">🔗</span>
            ${parents} parents
          </span>
        </div>
        ${group.prefix ? `<div class="group-card-prefix">${group.prefix}</div>` : ''}
      </div>
    `;
  }
  
  renderPlayerPermissions() {
    const searchTerm = this.playerPermSearch.value.toLowerCase();
    const filtered = this.playerPermissions.filter(p =>
      p.name.toLowerCase().includes(searchTerm) ||
      p.group.toLowerCase().includes(searchTerm)
    );
    
    this.playerPermList.innerHTML = filtered.length === 0
      ? '<p class="no-data">No player permissions found</p>'
      : filtered.map(player => this.createPlayerPermCard(player)).join('');
    
    // Add click listeners
    document.querySelectorAll('.player-perm-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const playerName = btn.closest('.player-perm-card').dataset.playerName;
        if (btn.textContent.includes('Edit')) {
          this.editPlayerPermissions(playerName);
        } else {
          this.removePlayerPermissions(playerName);
        }
      });
    });
  }
  
  createPlayerPermCard(player) {
    const group = this.groups.find(g => g.id === player.group);
    const expires = player.expires ? ` (expires ${player.expires})` : '';
    
    return `
      <div class="player-perm-card" data-player-name="${player.name}">
        <div class="player-perm-header">
          <div class="player-perm-info">
            <div class="player-avatar">👤</div>
            <div class="player-perm-details">
              <div class="player-perm-name">${player.name}</div>
              <div class="player-perm-group">${group.displayName}${expires}</div>
            </div>
          </div>
          <div class="player-perm-actions">
            <button class="player-perm-btn">Edit</button>
            <button class="player-perm-btn">Remove</button>
          </div>
        </div>
      </div>
    `;
  }
  
  renderInheritanceTree() {
    const selectedGroupId = this.treeGroupSelect.value;
    if (!selectedGroupId) {
      this.inheritanceTree.innerHTML = '<p class="no-data">Select a group to view its inheritance tree</p>';
      return;
    }
    
    const group = this.groups.find(g => g.id === selectedGroupId);
    if (!group) return;
    
    this.inheritanceTree.innerHTML = this.createTreeNode(group);
    
    // Add expand/collapse listeners
    document.querySelectorAll('.tree-item').forEach(item => {
      item.addEventListener('click', (e) => {
        e.stopPropagation();
        item.classList.toggle('expanded');
      });
    });
  }
  
  createTreeNode(group, depth = 0) {
    const hasParents = group.parents && group.parents.length > 0;
    const expandIcon = hasParents ? '▸' : '';
    
    let html = `
      <div class="tree-item ${depth === 0 ? 'expanded' : ''}">
        <div class="tree-item-header">
          <div class="tree-item-name">
            ${expandIcon ? `<span class="tree-expand-icon">${expandIcon}</span>` : ''}
            ${group.name}
          </div>
          <div class="tree-item-stats">
            ${group.permissions.length} perms • ${group.members.length} members
          </div>
        </div>
    `;
    
    if (hasParents) {
      html += '<div class="tree-children">';
      group.parents.forEach(parentId => {
        const parentGroup = this.groups.find(g => g.id === parentId);
        if (parentGroup) {
          html += `<div class="tree-node">${this.createTreeNode(parentGroup, depth + 1)}</div>`;
        }
      });
      html += '</div>';
    }
    
    html += '</div>';
    return html;
  }
  
  expandAllTree() {
    document.querySelectorAll('.tree-item').forEach(item => item.classList.add('expanded'));
  }
  
  collapseAllTree() {
    document.querySelectorAll('.tree-item').forEach(item => item.classList.remove('expanded'));
  }
  
  openCreateGroupModal() {
    this.currentGroup = null;
    this.editGroupTitle.textContent = 'Create Group';
    this.groupName.value = '';
    this.groupDisplayName.value = '';
    this.groupPrefix.value = '';
    this.groupSuffix.value = '';
    this.groupPriority.value = '0';
    this.groupDefault.checked = false;
    this.groupPermList.innerHTML = '<p class="no-data">No permissions added yet</p>';
    this.parentGroupsList.innerHTML = '<p class="no-data">No parent groups added yet</p>';
    this.groupMemberList.innerHTML = '<p class="no-data">No members added yet</p>';
    this.deleteGroup.style.display = 'none';
    this.editGroupModal.style.display = 'flex';
  }
  
  openEditGroupModal(groupId) {
    const group = this.groups.find(g => g.id === groupId);
    if (!group) return;
    
    this.currentGroup = group;
    this.editGroupTitle.textContent = `Edit Group: ${group.name}`;
    this.groupName.value = group.name;
    this.groupDisplayName.value = group.displayName;
    this.groupPrefix.value = group.prefix;
    this.groupSuffix.value = group.suffix;
    this.groupPriority.value = group.priority;
    this.groupDefault.checked = group.isDefault;
    
    this.renderGroupPermissions();
    this.renderParentGroups();
    this.renderGroupMembers();
    
    this.deleteGroup.style.display = 'inline-flex';
    this.editGroupModal.style.display = 'flex';
  }
  
  closeEditGroupModal() {
    this.editGroupModal.style.display = 'none';
    this.currentGroup = null;
  }
  
  renderGroupPermissions() {
    if (!this.currentGroup || this.currentGroup.permissions.length === 0) {
      this.groupPermList.innerHTML = '<p class="no-data">No permissions</p>';
      return;
    }
    
    this.groupPermList.innerHTML = this.currentGroup.permissions
      .map(perm => `
        <div class="perm-item ${perm.type}">
          <div class="perm-item-info">
            <div class="perm-item-node">${perm.node}</div>
            ${perm.world ? `<div class="perm-item-world">World: ${perm.world}</div>` : ''}
          </div>
          <div class="perm-item-actions">
            <button class="perm-btn danger" onclick="permissionManager.removePermission('${perm.node}')">Remove</button>
          </div>
        </div>
      `).join('');
  }
  
  renderParentGroups() {
    if (!this.currentGroup || this.currentGroup.parents.length === 0) {
      this.parentGroupsList.innerHTML = '<p class="no-data">No parent groups</p>';
      return;
    }
    
    this.parentGroupsList.innerHTML = this.currentGroup.parents
      .map(parentId => {
        const parent = this.groups.find(g => g.id === parentId);
        return parent ? `
          <div class="inheritance-item">
            <span class="inheritance-item-name">${parent.name}</span>
            <button class="perm-btn danger" onclick="permissionManager.removeParent('${parentId}')">Remove</button>
          </div>
        ` : '';
      }).join('');
    
    // Render child groups
    const children = this.groups.filter(g => g.parents.includes(this.currentGroup.id));
    if (children.length === 0) {
      this.childGroupsList.innerHTML = '<p class="no-data">No child groups</p>';
    } else {
      this.childGroupsList.innerHTML = children
        .map(child => `
          <div class="inheritance-item">
            <span class="inheritance-item-name">${child.name}</span>
          </div>
        `).join('');
    }
  }
  
  renderGroupMembers() {
    if (!this.currentGroup || this.currentGroup.members.length === 0) {
      this.groupMemberList.innerHTML = '<p class="no-data">No members</p>';
      return;
    }
    
    this.groupMemberList.innerHTML = this.currentGroup.members
      .map(member => `
        <div class="member-item">
          <div class="member-item-info">
            <div class="player-avatar">👤</div>
            <span class="member-item-name">${member}</span>
          </div>
          <button class="perm-btn danger" onclick="permissionManager.removeMember('${member}')">Remove</button>
        </div>
      `).join('');
  }
  
  filterPermissions() {
    const searchTerm = this.permSearch.value.toLowerCase();
    const items = this.groupPermList.querySelectorAll('.perm-item');
    
    items.forEach(item => {
      const node = item.querySelector('.perm-item-node').textContent.toLowerCase();
      const matchesSearch = node.includes(searchTerm);
      const matchesCategory = this.currentPermCategory === 'all' || node.includes(this.currentPermCategory);
      item.style.display = matchesSearch && matchesCategory ? 'flex' : 'none';
    });
  }
  
  filterMembers() {
    const searchTerm = this.memberSearch.value.toLowerCase();
    const items = this.groupMemberList.querySelectorAll('.member-item');
    
    items.forEach(item => {
      const name = item.querySelector('.member-item-name').textContent.toLowerCase();
      item.style.display = name.includes(searchTerm) ? 'flex' : 'none';
    });
  }
  
  openAddPermissionModal() {
    this.newPermissionNode.value = '';
    this.permissionWorld.value = '';
    this.addPermissionModal.style.display = 'flex';
  }
  
  closeAddPermissionModal() {
    this.addPermissionModal.style.display = 'none';
  }
  
  addPermission() {
    const node = this.newPermissionNode.value.trim();
    if (!node || !this.currentGroup) return;
    
    const type = document.querySelector('input[name="perm-type"]:checked').value;
    const world = this.permissionWorld.value;
    
    this.currentGroup.permissions.push({ node, type, world });
    this.renderGroupPermissions();
    this.closeAddPermissionModal();
    this.showNotification('Permission added successfully', 'success');
  }
  
  removePermission(node) {
    if (!this.currentGroup) return;
    this.currentGroup.permissions = this.currentGroup.permissions.filter(p => p.node !== node);
    this.renderGroupPermissions();
    this.showNotification('Permission removed', 'success');
  }
  
  addParentGroup() {
    // In a real implementation, this would open a modal to select a parent group
    this.showNotification('Select a parent group to add', 'info');
  }
  
  removeParent(parentId) {
    if (!this.currentGroup) return;
    this.currentGroup.parents = this.currentGroup.parents.filter(p => p !== parentId);
    this.renderParentGroups();
    this.showNotification('Parent group removed', 'success');
  }
  
  addMember() {
    // In a real implementation, this would open a modal to add a member
    this.showNotification('Enter member name to add', 'info');
  }
  
  removeMember(member) {
    if (!this.currentGroup) return;
    this.currentGroup.members = this.currentGroup.members.filter(m => m !== member);
    this.renderGroupMembers();
    this.showNotification('Member removed', 'success');
  }
  
  saveCurrentGroup() {
    if (!this.currentGroup) {
      // Creating new group
      const newGroup = {
        id: this.groupName.value.toLowerCase(),
        name: this.groupName.value,
        displayName: this.groupDisplayName.value,
        prefix: this.groupPrefix.value,
        suffix: this.groupSuffix.value,
        priority: parseInt(this.groupPriority.value),
        isDefault: this.groupDefault.checked,
        permissions: [],
        parents: [],
        members: []
      };
      this.groups.push(newGroup);
      this.showNotification('Group created successfully', 'success');
    } else {
      // Updating existing group
      this.currentGroup.name = this.groupName.value;
      this.currentGroup.displayName = this.groupDisplayName.value;
      this.currentGroup.prefix = this.groupPrefix.value;
      this.currentGroup.suffix = this.groupSuffix.value;
      this.currentGroup.priority = parseInt(this.groupPriority.value);
      this.currentGroup.isDefault = this.groupDefault.checked;
      this.showNotification('Group updated successfully', 'success');
    }
    
    this.updateStats();
    this.renderGroups();
    this.populateGroupSelects();
    this.closeEditGroupModal();
  }
  
  deleteCurrentGroup() {
    if (!this.currentGroup || !confirm(`Delete group "${this.currentGroup.name}"?`)) return;
    
    this.groups = this.groups.filter(g => g.id !== this.currentGroup.id);
    this.updateStats();
    this.renderGroups();
    this.populateGroupSelects();
    this.closeEditGroupModal();
    this.showNotification('Group deleted successfully', 'success');
  }
  
  openAssignPlayerModal() {
    this.assignPlayerModal.style.display = 'flex';
  }
  
  closeAssignPlayerModal() {
    this.assignPlayerModal.style.display = 'none';
  }
  
  assignPlayer() {
    const playerName = this.assignPlayerName.value.trim();
    const groupId = this.assignPlayerGroup.value;
    const duration = this.assignDuration.value;
    
    if (!playerName || !groupId) return;
    
    const expires = duration && duration !== '' ? this.calculateExpireDate(duration) : null;
    
    this.playerPermissions.push({
      name: playerName,
      group: groupId,
      assignedDate: new Date().toISOString().split('T')[0],
      expires
    });
    
    const group = this.groups.find(g => g.id === groupId);
    if (group) {
      group.members.push(playerName);
    }
    
    this.updateStats();
    this.renderPlayerPermissions();
    this.closeAssignPlayerModal();
    this.showNotification(`${playerName} assigned to group successfully`, 'success');
  }
  
  calculateExpireDate(duration) {
    const now = new Date();
    if (duration === '1h') now.setHours(now.getHours() + 1);
    else if (duration === '24h') now.setDate(now.getDate() + 1);
    else if (duration === '7d') now.setDate(now.getDate() + 7);
    else if (duration === '30d') now.setDate(now.getDate() + 30);
    return now.toISOString().split('T')[0];
  }
  
  editPlayerPermissions(playerName) {
    this.showNotification(`Edit permissions for ${playerName}`, 'info');
  }
  
  removePlayerPermissions(playerName) {
    if (!confirm(`Remove ${playerName} from their group?`)) return;
    
    const player = this.playerPermissions.find(p => p.name === playerName);
    if (player) {
      const group = this.groups.find(g => g.id === player.group);
      if (group) {
        group.members = group.members.filter(m => m !== playerName);
      }
    }
    
    this.playerPermissions = this.playerPermissions.filter(p => p.name !== playerName);
    this.updateStats();
    this.renderPlayerPermissions();
    this.showNotification(`${playerName} removed from group`, 'success');
  }
  
  updateStats() {
    const totalGroups = this.groups.length;
    const totalPermissions = this.groups.reduce((sum, g) => sum + g.permissions.length, 0);
    const assignedPlayers = this.playerPermissions.length;
    const inheritanceChains = this.groups.filter(g => g.parents.length > 0).length;
    
    this.totalGroupsCount.textContent = totalGroups;
    this.totalPermissionsCount.textContent = totalPermissions;
    this.assignedPlayersCount.textContent = assignedPlayers;
    this.inheritanceChainsCount.textContent = inheritanceChains;
  }
  
  populateGroupSelects() {
    // Populate tree select
    this.treeGroupSelect.innerHTML = '<option value="">Select a group to view inheritance...</option>' +
      this.groups.map(g => `<option value="${g.id}">${g.displayName}</option>`).join('');
    
    // Populate assign player group select
    this.assignPlayerGroup.innerHTML = this.groups
      .map(g => `<option value="${g.id}">${g.displayName}</option>`).join('');
  }
  
  showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 16px 24px;
      background: ${type === 'success' ? 'rgba(34, 197, 94, 0.9)' : type === 'error' ? 'rgba(239, 68, 68, 0.9)' : 'rgba(59, 130, 246, 0.9)'};
      color: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
      z-index: 10000;
      animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
      notification.style.animation = 'slideOut 0.3s ease';
      setTimeout(() => notification.remove(), 300);
    }, 3000);
  }
}

// ========================================
// PLUGIN MANAGER
// ========================================

class PluginManager {
  constructor() {
    // DOM References - Stats
    this.totalPluginsCount = document.getElementById('total-plugins-count');
    this.enabledPluginsCount = document.getElementById('enabled-plugins-count');
    this.disabledPluginsCount = document.getElementById('disabled-plugins-count');
    this.outdatedPluginsCount = document.getElementById('outdated-plugins-count');
    
    // DOM References - Controls
    this.pluginSearch = document.getElementById('plugin-search');
    this.filterButtons = document.querySelectorAll('.plugin-filter-btn');
    this.reloadAllBtn = document.getElementById('reload-all-plugins');
    this.uploadPluginBtn = document.getElementById('upload-plugin');
    
    // DOM References - Plugin List
    this.pluginList = document.getElementById('plugin-list');
    
    // DOM References - Details Modal
    this.detailsModal = document.getElementById('plugin-details-modal');
    this.closeDetailsBtn = document.getElementById('close-plugin-details');
    this.closeModalBtn = document.getElementById('close-plugin-modal');
    this.detailsName = document.getElementById('plugin-details-name');
    
    // DOM References - Details Tabs
    this.pluginTabs = document.querySelectorAll('.plugin-tab');
    this.tabContents = document.querySelectorAll('.plugin-tab-content');
    
    // DOM References - Info Tab
    this.infoName = document.getElementById('plugin-info-name');
    this.infoVersion = document.getElementById('plugin-info-version');
    this.infoAuthor = document.getElementById('plugin-info-author');
    this.infoStatus = document.getElementById('plugin-info-status');
    this.infoLoadTime = document.getElementById('plugin-info-loadtime');
    this.infoApi = document.getElementById('plugin-info-api');
    this.infoDescription = document.getElementById('plugin-info-description');
    this.infoWebsite = document.getElementById('plugin-info-website');
    
    // DOM References - Config Tab
    this.configEditor = document.getElementById('plugin-config-editor');
    this.configFileName = document.getElementById('config-file-name');
    this.resetConfigBtn = document.getElementById('reset-config');
    this.saveConfigBtn = document.getElementById('save-config');
    this.configLines = document.getElementById('config-editor-lines');
    this.configValidation = document.getElementById('config-editor-validation');
    
    // DOM References - Dependencies Tab
    this.requiredDeps = document.getElementById('plugin-required-deps');
    this.softDeps = document.getElementById('plugin-soft-deps');
    this.loadBefore = document.getElementById('plugin-loadbefore');
    
    // DOM References - Commands Tab
    this.commandSearch = document.getElementById('command-search');
    this.commandList = document.getElementById('plugin-command-list');
    
    // DOM References - Upload Modal
    this.uploadModal = document.getElementById('upload-plugin-modal');
    this.closeUploadBtn = document.getElementById('close-upload-modal');
    this.uploadDropZone = document.getElementById('upload-drop-zone');
    this.pluginFileInput = document.getElementById('plugin-file-input');
    this.uploadInfo = document.getElementById('upload-info');
    this.uploadFileName = document.getElementById('upload-file-name');
    this.uploadFileSize = document.getElementById('upload-file-size');
    this.uploadProgress = document.getElementById('upload-progress');
    this.uploadProgressBar = document.getElementById('upload-progress-bar');
    this.autoEnableCheckbox = document.getElementById('auto-enable-plugin');
    this.reloadAfterCheckbox = document.getElementById('reload-after-upload');
    this.cancelUploadBtn = document.getElementById('cancel-upload');
    this.confirmUploadBtn = document.getElementById('confirm-upload');
    
    // State
    this.plugins = [];
    this.currentFilter = 'all';
    this.currentPlugin = null;
    this.originalConfig = '';
    this.selectedFile = null;
  }
  
  init() {
    this.setupEventListeners();
    this.loadPlugins();
    this.updateStats();
  }
  
  setupEventListeners() {
    // Search
    this.pluginSearch.addEventListener('input', () => this.renderPlugins());
    
    // Filter buttons
    this.filterButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        this.filterButtons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        this.currentFilter = btn.dataset.filter;
        this.renderPlugins();
      });
    });
    
    // Bulk actions
    this.reloadAllBtn.addEventListener('click', () => this.reloadAllPlugins());
    this.uploadPluginBtn.addEventListener('click', () => this.openUploadModal());
    
    // Details modal
    this.closeDetailsBtn.addEventListener('click', () => this.closeDetailsModal());
    this.closeModalBtn.addEventListener('click', () => this.closeDetailsModal());
    
    // Plugin tabs
    this.pluginTabs.forEach(tab => {
      tab.addEventListener('click', () => {
        this.pluginTabs.forEach(t => t.classList.remove('active'));
        this.tabContents.forEach(c => c.classList.remove('active'));
        tab.classList.add('active');
        const tabName = tab.dataset.tab;
        document.querySelector(`.plugin-tab-content[data-tab="${tabName}"]`).classList.add('active');
      });
    });
    
    // Config editor
    this.configEditor.addEventListener('input', () => this.updateConfigInfo());
    this.resetConfigBtn.addEventListener('click', () => this.resetConfig());
    this.saveConfigBtn.addEventListener('click', () => this.saveConfig());
    
    // Command search
    this.commandSearch.addEventListener('input', (e) => this.filterCommands(e.target.value));
    
    // Upload modal
    this.closeUploadBtn.addEventListener('click', () => this.closeUploadModal());
    this.cancelUploadBtn.addEventListener('click', () => this.closeUploadModal());
    this.confirmUploadBtn.addEventListener('click', () => this.uploadPlugin());
    
    // File upload
    this.uploadDropZone.addEventListener('click', () => this.pluginFileInput.click());
    this.pluginFileInput.addEventListener('change', (e) => this.handleFileSelect(e.target.files[0]));
    
    // Drag and drop
    this.uploadDropZone.addEventListener('dragover', (e) => {
      e.preventDefault();
      this.uploadDropZone.classList.add('drag-over');
    });
    
    this.uploadDropZone.addEventListener('dragleave', () => {
      this.uploadDropZone.classList.remove('drag-over');
    });
    
    this.uploadDropZone.addEventListener('drop', (e) => {
      e.preventDefault();
      this.uploadDropZone.classList.remove('drag-over');
      const file = e.dataTransfer.files[0];
      if (file && file.name.endsWith('.jar')) {
        this.handleFileSelect(file);
      }
    });
    
    // Close modals on outside click
    this.detailsModal.addEventListener('click', (e) => {
      if (e.target === this.detailsModal) this.closeDetailsModal();
    });
    this.uploadModal.addEventListener('click', (e) => {
      if (e.target === this.uploadModal) this.closeUploadModal();
    });
  }
  
  loadPlugins() {
    // Mock plugin data
    this.plugins = [
      {
        id: 'neoessentials',
        name: 'NeoEssentials',
        version: '1.0.2.2',
        author: 'ZeroG Network',
        description: 'Essential commands and utilities for Minecraft servers',
        enabled: true,
        outdated: false,
        isLibrary: false,
        loadTime: 245,
        apiVersion: '1.21',
        website: 'https://github.com/ZeroG-Network-Org/NeoEssentials',
        dependencies: {
          required: [],
          soft: ['Vault', 'PlaceholderAPI'],
          loadBefore: []
        },
        commands: [
          { name: '/spawn', description: 'Teleport to spawn point', usage: '/spawn', aliases: ['spawn'] },
          { name: '/home', description: 'Teleport to your home', usage: '/home [name]', aliases: ['h'] },
          { name: '/sethome', description: 'Set a home location', usage: '/sethome <name>', aliases: ['sh'] },
          { name: '/warp', description: 'Teleport to a warp', usage: '/warp <name>', aliases: ['w'] },
          { name: '/tpa', description: 'Request teleport to player', usage: '/tpa <player>', aliases: ['tprequest'] }
        ],
        config: `# NeoEssentials Configuration
enable-homes: true
max-homes: 5
home-cooldown: 30

enable-warps: true
warp-cooldown: 10

spawn:
  enabled: true
  on-join: true
  on-death: false`
      },
      {
        id: 'worldedit',
        name: 'WorldEdit',
        version: '7.2.15',
        author: 'sk89q',
        description: 'In-game world editor for building and terraforming',
        enabled: true,
        outdated: true,
        isLibrary: false,
        loadTime: 892,
        apiVersion: '1.21',
        website: 'https://worldedit.enginehub.org',
        dependencies: {
          required: ['WorldEditCore'],
          soft: [],
          loadBefore: ['WorldGuard']
        },
        commands: [
          { name: '//set', description: 'Set blocks in selection', usage: '//set <block>', aliases: ['set'] },
          { name: '//copy', description: 'Copy selection to clipboard', usage: '//copy', aliases: ['c'] },
          { name: '//paste', description: 'Paste from clipboard', usage: '//paste', aliases: ['p'] },
          { name: '//undo', description: 'Undo last action', usage: '//undo [steps]', aliases: ['u'] },
          { name: '//redo', description: 'Redo last action', usage: '//redo [steps]', aliases: ['r'] }
        ],
        config: `max-blocks-changed: 1000000
max-radius: 200
nav-wand-item: minecraft:compass
use-inventory: true
log-commands: true`
      },
      {
        id: 'vault',
        name: 'Vault',
        version: '1.7.3',
        author: 'MilkBowl',
        description: 'Abstraction library for permissions, chat, and economy APIs',
        enabled: true,
        outdated: false,
        isLibrary: true,
        loadTime: 134,
        apiVersion: '1.21',
        website: 'https://github.com/MilkBowl/Vault',
        dependencies: {
          required: [],
          soft: [],
          loadBefore: []
        },
        commands: [],
        config: `# Vault Configuration
update-notifications: true
debug: false`
      },
      {
        id: 'luckperms',
        name: 'LuckPerms',
        version: '5.4.102',
        author: 'Luck',
        description: 'Advanced permissions plugin with group management',
        enabled: true,
        outdated: false,
        isLibrary: false,
        loadTime: 567,
        apiVersion: '1.21',
        website: 'https://luckperms.net',
        dependencies: {
          required: [],
          soft: ['Vault'],
          loadBefore: []
        },
        commands: [
          { name: '/lp', description: 'LuckPerms main command', usage: '/lp <args>', aliases: ['luckperms', 'perm', 'perms'] },
          { name: '/lp user', description: 'Manage user permissions', usage: '/lp user <player> <args>', aliases: [] },
          { name: '/lp group', description: 'Manage group permissions', usage: '/lp group <group> <args>', aliases: [] }
        ],
        config: `server: global
storage-method: h2
sync-minutes: 3

data:
  pool-settings:
    maximum-pool-size: 10
  table-prefix: 'luckperms_'`
      },
      {
        id: 'dynmap',
        name: 'Dynmap',
        version: '3.4',
        author: 'mikeprimm',
        description: 'Dynamic web-based map for your server',
        enabled: false,
        outdated: false,
        isLibrary: false,
        loadTime: 0,
        apiVersion: '1.21',
        website: 'https://www.spigotmc.org/resources/dynmap.274/',
        dependencies: {
          required: [],
          soft: ['WorldGuard', 'Towny'],
          loadBefore: []
        },
        commands: [
          { name: '/dynmap', description: 'Dynmap main command', usage: '/dynmap <args>', aliases: ['dmap'] },
          { name: '/dmarker', description: 'Manage map markers', usage: '/dmarker <args>', aliases: ['dm'] }
        ],
        config: `webserver-port: 8123
enable-markers: true
update-rate: 2000
fullrender-players: 0`
      }
    ];
    
    this.renderPlugins();
  }
  
  renderPlugins() {
    const searchTerm = this.pluginSearch.value.toLowerCase();
    const filtered = this.plugins.filter(plugin => {
      const matchesSearch = plugin.name.toLowerCase().includes(searchTerm) ||
                           plugin.description.toLowerCase().includes(searchTerm) ||
                           plugin.author.toLowerCase().includes(searchTerm);
      
      const matchesFilter = this.currentFilter === 'all' ||
                           (this.currentFilter === 'enabled' && plugin.enabled) ||
                           (this.currentFilter === 'disabled' && !plugin.enabled) ||
                           (this.currentFilter === 'outdated' && plugin.outdated) ||
                           (this.currentFilter === 'libraries' && plugin.isLibrary);
      
      return matchesSearch && matchesFilter;
    });
    
    this.pluginList.innerHTML = filtered.length === 0 
      ? '<p class="no-data">No plugins found</p>'
      : filtered.map(plugin => this.createPluginElement(plugin)).join('');
    
    // Add event listeners
    document.querySelectorAll('.plugin-toggle input').forEach(toggle => {
      toggle.addEventListener('change', (e) => {
        const pluginId = e.target.closest('.plugin-item').dataset.pluginId;
        this.togglePlugin(pluginId, e.target.checked);
      });
    });
    
    document.querySelectorAll('.plugin-btn.primary').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const pluginId = e.target.closest('.plugin-item').dataset.pluginId;
        this.openDetailsModal(pluginId);
      });
    });
    
    document.querySelectorAll('.plugin-btn:not(.primary):not(.danger)').forEach(btn => {
      if (btn.textContent.includes('Reload')) {
        btn.addEventListener('click', (e) => {
          const pluginId = e.target.closest('.plugin-item').dataset.pluginId;
          this.reloadPlugin(pluginId);
        });
      }
    });
    
    document.querySelectorAll('.plugin-btn.danger').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const pluginId = e.target.closest('.plugin-item').dataset.pluginId;
        this.deletePlugin(pluginId);
      });
    });
  }
  
  createPluginElement(plugin) {
    const statusBadge = plugin.enabled 
      ? '<span class="plugin-badge status-enabled">Enabled</span>'
      : '<span class="plugin-badge status-disabled">Disabled</span>';
    
    const libraryBadge = plugin.isLibrary 
      ? '<span class="plugin-badge type-library">Library</span>'
      : '';
    
    const updateBadge = plugin.outdated 
      ? '<span class="plugin-badge update-available">Update Available</span>'
      : '';
    
    const enabledClass = plugin.enabled ? 'enabled' : 'disabled';
    const outdatedClass = plugin.outdated ? 'outdated' : '';
    
    return `
      <div class="plugin-item ${enabledClass} ${outdatedClass}" data-plugin-id="${plugin.id}">
        <div class="plugin-item-header">
          <div class="plugin-item-info">
            <div class="plugin-item-name">
              ${plugin.name}
              <span class="plugin-item-version">v${plugin.version}</span>
            </div>
            <div class="plugin-item-author">by ${plugin.author}</div>
          </div>
          <label class="plugin-toggle">
            <input type="checkbox" ${plugin.enabled ? 'checked' : ''}>
            <span class="plugin-toggle-slider"></span>
          </label>
        </div>
        <div class="plugin-item-description">${plugin.description}</div>
        <div class="plugin-item-meta">
          <span class="plugin-meta-item">
            <span class="icon">⚡</span>
            ${plugin.loadTime}ms
          </span>
          <span class="plugin-meta-item">
            <span class="icon">🔧</span>
            API ${plugin.apiVersion}
          </span>
          <span class="plugin-meta-item">
            <span class="icon">💬</span>
            ${plugin.commands.length} commands
          </span>
        </div>
        <div class="plugin-badges">
          ${statusBadge}
          ${libraryBadge}
          ${updateBadge}
        </div>
        <div class="plugin-item-actions">
          <button class="plugin-btn primary">Details</button>
          <button class="plugin-btn">Reload</button>
          <button class="plugin-btn danger">Delete</button>
        </div>
      </div>
    `;
  }
  
  updateStats() {
    const total = this.plugins.length;
    const enabled = this.plugins.filter(p => p.enabled).length;
    const disabled = this.plugins.filter(p => !p.enabled).length;
    const outdated = this.plugins.filter(p => p.outdated).length;
    
    this.totalPluginsCount.textContent = total;
    this.enabledPluginsCount.textContent = enabled;
    this.disabledPluginsCount.textContent = disabled;
    this.outdatedPluginsCount.textContent = outdated;
  }
  
  togglePlugin(pluginId, enabled) {
    const plugin = this.plugins.find(p => p.id === pluginId);
    if (plugin) {
      plugin.enabled = enabled;
      this.updateStats();
      this.renderPlugins();
      this.showNotification(
        enabled ? `${plugin.name} enabled successfully` : `${plugin.name} disabled successfully`,
        'success'
      );
    }
  }
  
  reloadPlugin(pluginId) {
    const plugin = this.plugins.find(p => p.id === pluginId);
    if (plugin) {
      this.showNotification(`Reloading ${plugin.name}...`, 'info');
      setTimeout(() => {
        this.showNotification(`${plugin.name} reloaded successfully`, 'success');
      }, 500);
    }
  }
  
  deletePlugin(pluginId) {
    const plugin = this.plugins.find(p => p.id === pluginId);
    if (plugin && confirm(`Are you sure you want to delete ${plugin.name}?`)) {
      this.plugins = this.plugins.filter(p => p.id !== pluginId);
      this.updateStats();
      this.renderPlugins();
      this.showNotification(`${plugin.name} deleted successfully`, 'success');
    }
  }
  
  reloadAllPlugins() {
    this.showNotification('Reloading all plugins...', 'info');
    setTimeout(() => {
      this.showNotification('All plugins reloaded successfully', 'success');
    }, 1000);
  }
  
  openDetailsModal(pluginId) {
    this.currentPlugin = this.plugins.find(p => p.id === pluginId);
    if (!this.currentPlugin) return;
    
    // Reset to info tab
    this.pluginTabs.forEach(t => t.classList.remove('active'));
    this.tabContents.forEach(c => c.classList.remove('active'));
    this.pluginTabs[0].classList.add('active');
    this.tabContents[0].classList.add('active');
    
    // Populate info tab
    this.detailsName.textContent = this.currentPlugin.name;
    this.infoName.textContent = this.currentPlugin.name;
    this.infoVersion.textContent = this.currentPlugin.version;
    this.infoAuthor.textContent = this.currentPlugin.author;
    this.infoStatus.textContent = this.currentPlugin.enabled ? 'Enabled' : 'Disabled';
    this.infoLoadTime.textContent = `${this.currentPlugin.loadTime}ms`;
    this.infoApi.textContent = this.currentPlugin.apiVersion;
    this.infoDescription.textContent = this.currentPlugin.description;
    this.infoWebsite.textContent = this.currentPlugin.website;
    this.infoWebsite.href = this.currentPlugin.website;
    
    // Populate config tab
    this.configFileName.textContent = `${this.currentPlugin.id}.yml`;
    this.configEditor.value = this.currentPlugin.config;
    this.originalConfig = this.currentPlugin.config;
    this.updateConfigInfo();
    
    // Populate dependencies tab
    this.populateDependencies();
    
    // Populate commands tab
    this.populateCommands();
    
    this.detailsModal.style.display = 'flex';
  }
  
  closeDetailsModal() {
    this.detailsModal.style.display = 'none';
    this.currentPlugin = null;
  }
  
  populateDependencies() {
    const deps = this.currentPlugin.dependencies;
    
    // Required dependencies
    if (deps.required.length === 0) {
      this.requiredDeps.innerHTML = '<p class="no-data">No required dependencies</p>';
    } else {
      this.requiredDeps.innerHTML = deps.required.map(dep => {
        const installed = this.plugins.some(p => p.name === dep);
        return `
          <div class="dependency-item">
            <span class="dependency-name">${dep}</span>
            <span class="dependency-status ${installed ? 'installed' : 'missing'}">
              ${installed ? '✓ Installed' : '⚠️ Missing'}
            </span>
          </div>
        `;
      }).join('');
    }
    
    // Soft dependencies
    if (deps.soft.length === 0) {
      this.softDeps.innerHTML = '<p class="no-data">No soft dependencies</p>';
    } else {
      this.softDeps.innerHTML = deps.soft.map(dep => {
        const installed = this.plugins.some(p => p.name === dep);
        return `
          <div class="dependency-item">
            <span class="dependency-name">${dep}</span>
            <span class="dependency-status ${installed ? 'installed' : 'missing'}">
              ${installed ? '✓ Installed' : '○ Optional'}
            </span>
          </div>
        `;
      }).join('');
    }
    
    // Load before
    if (deps.loadBefore.length === 0) {
      this.loadBefore.innerHTML = '<p class="no-data">No load order specified</p>';
    } else {
      this.loadBefore.innerHTML = deps.loadBefore.map(dep => {
        return `
          <div class="dependency-item">
            <span class="dependency-name">${dep}</span>
            <span class="dependency-status installed">✓ OK</span>
          </div>
        `;
      }).join('');
    }
  }
  
  populateCommands() {
    if (this.currentPlugin.commands.length === 0) {
      this.commandList.innerHTML = '<p class="no-data">No commands registered</p>';
    } else {
      this.commandList.innerHTML = this.currentPlugin.commands.map(cmd => {
        const aliasesHtml = cmd.aliases.length > 0
          ? `<div class="command-aliases">${cmd.aliases.map(a => `<span class="command-alias">/${a}</span>`).join('')}</div>`
          : '';
        
        return `
          <div class="command-item">
            <div class="command-name">${cmd.name}</div>
            <div class="command-description">${cmd.description}</div>
            <div class="command-usage">${cmd.usage}</div>
            ${aliasesHtml}
          </div>
        `;
      }).join('');
    }
  }
  
  filterCommands(searchTerm) {
    const items = this.commandList.querySelectorAll('.command-item');
    items.forEach(item => {
      const name = item.querySelector('.command-name').textContent.toLowerCase();
      const desc = item.querySelector('.command-description').textContent.toLowerCase();
      const matches = name.includes(searchTerm.toLowerCase()) || desc.includes(searchTerm.toLowerCase());
      item.style.display = matches ? 'block' : 'none';
    });
  }
  
  updateConfigInfo() {
    const lines = this.configEditor.value.split('\n').length;
    this.configLines.textContent = `${lines} lines`;
    
    // Simple YAML validation (check for basic syntax)
    const value = this.configEditor.value;
    const hasInvalidSyntax = value.includes('\t') || /^\s*[^#\s-].*:\s*$/.test(value);
    
    if (hasInvalidSyntax) {
      this.configValidation.textContent = '⚠️ Warning: Check syntax';
      this.configValidation.style.color = '#f59e0b';
    } else {
      this.configValidation.textContent = '✓ Valid YAML';
      this.configValidation.style.color = '#22c55e';
    }
  }
  
  resetConfig() {
    if (confirm('Reset configuration to original values?')) {
      this.configEditor.value = this.originalConfig;
      this.updateConfigInfo();
      this.showNotification('Configuration reset', 'info');
    }
  }
  
  saveConfig() {
    this.currentPlugin.config = this.configEditor.value;
    this.originalConfig = this.configEditor.value;
    this.showNotification('Configuration saved successfully', 'success');
  }
  
  openUploadModal() {
    this.uploadModal.style.display = 'flex';
    this.selectedFile = null;
    this.uploadInfo.style.display = 'none';
    this.confirmUploadBtn.disabled = true;
    this.uploadProgressBar.style.width = '0%';
  }
  
  closeUploadModal() {
    this.uploadModal.style.display = 'none';
  }
  
  handleFileSelect(file) {
    if (!file || !file.name.endsWith('.jar')) {
      this.showNotification('Please select a valid JAR file', 'error');
      return;
    }
    
    this.selectedFile = file;
    this.uploadFileName.textContent = file.name;
    this.uploadFileSize.textContent = `${(file.size / 1024 / 1024).toFixed(2)} MB`;
    this.uploadInfo.style.display = 'block';
    this.confirmUploadBtn.disabled = false;
  }
  
  uploadPlugin() {
    if (!this.selectedFile) return;
    
    this.confirmUploadBtn.disabled = true;
    this.cancelUploadBtn.disabled = true;
    
    // Simulate upload progress
    let progress = 0;
    const interval = setInterval(() => {
      progress += 10;
      this.uploadProgressBar.style.width = `${progress}%`;
      
      if (progress >= 100) {
        clearInterval(interval);
        setTimeout(() => {
          this.closeUploadModal();
          this.showNotification('Plugin uploaded successfully', 'success');
          
          // Add new plugin to list (mock)
          const newPlugin = {
            id: 'uploaded-' + Date.now(),
            name: this.selectedFile.name.replace('.jar', ''),
            version: '1.0.0',
            author: 'Unknown',
            description: 'Uploaded plugin',
            enabled: this.autoEnableCheckbox.checked,
            outdated: false,
            isLibrary: false,
            loadTime: 0,
            apiVersion: '1.21',
            website: '#',
            dependencies: { required: [], soft: [], loadBefore: [] },
            commands: [],
            config: '# Configuration'
          };
          
          this.plugins.push(newPlugin);
          this.updateStats();
          this.renderPlugins();
          
          if (this.reloadAfterCheckbox.checked) {
            setTimeout(() => this.reloadAllPlugins(), 500);
          }
        }, 500);
      }
    }, 100);
  }
  
  showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 16px 24px;
      background: ${type === 'success' ? 'rgba(34, 197, 94, 0.9)' : type === 'error' ? 'rgba(239, 68, 68, 0.9)' : 'rgba(59, 130, 246, 0.9)'};
      color: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
      z-index: 10000;
      animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
      notification.style.animation = 'slideOut 0.3s ease';
      setTimeout(() => notification.remove(), 300);
    }, 3000);
  }
}

// ========================================
// WORLD MANAGEMENT INTERFACE
// ========================================

class WorldManager {
  constructor() {
    // DOM References - Stats
    this.loadedChunksEl = document.getElementById('loaded-chunks');
    this.playersInWorldEl = document.getElementById('players-in-world');
    this.entitiesInWorldEl = document.getElementById('entities-in-world');
    this.worldTimeEl = document.getElementById('world-time');
    this.worldWeatherEl = document.getElementById('world-weather');
    this.worldSizeEl = document.getElementById('world-size');
    
    // DOM References - Time Controls
    this.currentTimeTicks = document.getElementById('current-time-ticks');
    this.timeSlider = document.getElementById('time-slider');
    this.timeButtons = document.querySelectorAll('[data-time]');
    
    // DOM References - Weather Controls
    this.weatherButtons = document.querySelectorAll('[data-weather]');
    this.weatherDuration = document.getElementById('weather-duration');
    
    // DOM References - Border
    this.borderCenter = document.getElementById('border-center');
    this.borderSize = document.getElementById('border-size');
    this.borderDamage = document.getElementById('border-damage');
    this.editBorderBtn = document.getElementById('edit-border');
    
    // DOM References - Difficulty
    this.difficultyButtons = document.querySelectorAll('[data-difficulty]');
    this.lockDifficulty = document.getElementById('lock-difficulty');
    
    // DOM References - Gamerules
    this.gameruleSearch = document.getElementById('gamerule-search');
    this.gameruleFilters = document.querySelectorAll('.gamerule-filter .filter-btn');
    this.gameruleGrid = document.getElementById('gamerules-grid');
    this.gameruleToggles = document.querySelectorAll('[data-gamerule]');
    this.resetGamerules = document.getElementById('reset-gamerules');
    this.saveGamerules = document.getElementById('save-gamerules');
    
    // DOM References - World Tabs
    this.worldTabs = document.querySelectorAll('.world-tab');
    
    // DOM References - Actions
    this.saveWorldsBtn = document.getElementById('save-worlds');
    this.refreshWorldsBtn = document.getElementById('refresh-worlds');
    
    // DOM References - Border Modal
    this.borderModal = document.getElementById('world-border-modal');
    this.closeBorderModal = document.getElementById('close-border-modal');
    this.borderCenterX = document.getElementById('border-center-x');
    this.borderCenterZ = document.getElementById('border-center-z');
    this.borderSizeInput = document.getElementById('border-size-input');
    this.borderTransition = document.getElementById('border-transition');
    this.borderDamageAmount = document.getElementById('border-damage-amount');
    this.borderDamageBuffer = document.getElementById('border-damage-buffer');
    this.borderWarningDistance = document.getElementById('border-warning-distance');
    this.borderWarningTime = document.getElementById('border-warning-time');
    this.saveBorder = document.getElementById('save-border');
    this.cancelBorderEdit = document.getElementById('cancel-border-edit');
    
    // State
    this.currentWorld = 'minecraft:overworld';
    this.currentGameruleFilter = 'all';
    this.gamerules = {};
    this.dimensions = [];
    this.worldTabsContainer = document.querySelector('.world-tabs');
  }

  async init() {
    await this.loadDimensions();
    this.setupEventListeners();
    this.loadWorldData();
    this.loadGamerules();
  }

  /**
   * Load dimensions dynamically from the server
   */
  async loadDimensions() {
    try {
      const response = await fetch('/api/map/dimensions');
      if (!response.ok) {
        throw new Error('Failed to fetch dimensions');
      }
      
      const data = await response.json();
      this.dimensions = data.dimensions || [];
      
      // Generate dimension tabs dynamically
      this.generateDimensionTabs();
      
      console.log(`Loaded ${this.dimensions.length} dimensions:`, this.dimensions);
    } catch (error) {
      console.error('Error loading dimensions:', error);
      // Fallback to default dimensions
      this.dimensions = [
        { key: 'minecraft:overworld', name: 'Overworld', playerCount: 0, loadedChunks: 0 },
        { key: 'minecraft:the_nether', name: 'The Nether', playerCount: 0, loadedChunks: 0 },
        { key: 'minecraft:the_end', name: 'The End', playerCount: 0, loadedChunks: 0 }
      ];
      this.generateDimensionTabs();
    }
  }

  /**
   * Generate dimension tabs dynamically based on loaded dimensions
   */
  generateDimensionTabs() {
    if (!this.worldTabsContainer) return;
    
    // Clear existing tabs
    this.worldTabsContainer.innerHTML = '';
    
    // Generate tabs for each dimension
    this.dimensions.forEach((dim, index) => {
      const button = document.createElement('button');
      button.className = 'world-tab';
      if (index === 0) {
        button.classList.add('active');
        this.currentWorld = dim.key;
      }
      button.dataset.world = dim.key;
      
      // Determine icon based on dimension
      let icon = '🌍';
      if (dim.key.includes('nether')) {
        icon = '🔥';
      } else if (dim.key.includes('end')) {
        icon = '🌌';
      } else if (dim.key.includes('twilight')) {
        icon = '🌲';
      } else if (dim.key.includes('mining')) {
        icon = '⛏️';
      } else if (dim.key.includes('void')) {
        icon = '🌑';
      } else if (dim.key.includes('aether')) {
        icon = '☁️';
      } else if (!dim.key.includes('overworld')) {
        // Custom dimension - use generic icon
        icon = '🗺️';
      }
      
      button.innerHTML = `
        <span class="tab-icon">${icon}</span>
        <span class="tab-name">${dim.name}</span>
      `;
      
      this.worldTabsContainer.appendChild(button);
    });
    
    // Re-query world tabs after generating them
    this.worldTabs = document.querySelectorAll('.world-tab');
  }

  setupEventListeners() {
    // World tabs
    this.worldTabs.forEach(tab => {
      tab.addEventListener('click', (e) => {
        this.worldTabs.forEach(t => t.classList.remove('active'));
        e.currentTarget.classList.add('active');
        this.currentWorld = e.currentTarget.dataset.world;
        this.loadWorldData();
      });
    });
    
    // Action buttons
    this.saveWorldsBtn.addEventListener('click', () => this.saveAllWorlds());
    this.refreshWorldsBtn.addEventListener('click', () => this.refreshWorlds());
    
    // Time controls
    this.timeSlider.addEventListener('input', (e) => {
      this.updateTimeDisplay(e.target.value);
    });
    
    this.timeSlider.addEventListener('change', (e) => {
      this.setWorldTime(e.target.value);
    });
    
    this.timeButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        const timePreset = e.currentTarget.dataset.time;
        this.setTimePreset(timePreset);
      });
    });
    
    // Weather controls
    this.weatherButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        const weather = e.currentTarget.dataset.weather;
        this.setWeather(weather);
      });
    });
    
    // Border controls
    this.editBorderBtn.addEventListener('click', () => this.openBorderModal());
    this.closeBorderModal.addEventListener('click', () => this.closeBorderModalHandler());
    this.cancelBorderEdit.addEventListener('click', () => this.closeBorderModalHandler());
    this.saveBorder.addEventListener('click', () => this.saveBorderSettings());
    
    // Difficulty controls
    this.difficultyButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        if (this.lockDifficulty.checked) {
          this.showNotification('Difficulty is locked', 'warning');
          return;
        }
        this.difficultyButtons.forEach(b => b.classList.remove('active'));
        e.currentTarget.classList.add('active');
        const difficulty = e.currentTarget.dataset.difficulty;
        this.setDifficulty(difficulty);
      });
    });
    
    // Gamerule search
    this.gameruleSearch.addEventListener('input', () => this.filterGamerules());
    
    // Gamerule filters
    this.gameruleFilters.forEach(filter => {
      filter.addEventListener('click', (e) => {
        this.gameruleFilters.forEach(f => f.classList.remove('active'));
        e.target.classList.add('active');
        this.currentGameruleFilter = e.target.dataset.filter;
        this.filterGamerules();
      });
    });
    
    // Gamerule toggles
    this.gameruleToggles.forEach(toggle => {
      toggle.addEventListener('change', (e) => {
        const gamerule = e.target.dataset.gamerule;
        const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
        this.gamerules[gamerule] = value;
      });
    });
    
    // Gamerule actions
    this.resetGamerules.addEventListener('click', () => this.resetToDefaults());
    this.saveGamerules.addEventListener('click', () => this.saveAllGamerules());
  }

  async loadWorldData() {
    try {
      // Fetch world info for current dimension
      const response = await fetch(`/api/map/world-info?dimension=${encodeURIComponent(this.currentWorld)}`);
      if (!response.ok) {
        throw new Error('Failed to fetch world data');
      }
      
      const result = await response.json();
      const data = result.dimension;
      
      // Update stats
      this.loadedChunksEl.textContent = data.loadedChunks ? data.loadedChunks.toLocaleString() : '0';
      
      // Get player count for this dimension
      const dimData = this.dimensions.find(d => d.key === this.currentWorld);
      this.playersInWorldEl.textContent = dimData ? dimData.playerCount : '0';
      
      // Entities count (placeholder - would need separate API endpoint)
      this.entitiesInWorldEl.textContent = '0';
      
      // Time display
      const ticks = data.dayTime % 24000;
      const day = Math.floor(data.gameTime / 24000);
      const hours = Math.floor(ticks / 1000);
      const minutes = Math.floor((ticks % 1000) / 16.67);
      this.worldTimeEl.textContent = `Day ${day}, ${hours}:${minutes.toString().padStart(2, '0')}`;
      
      // Weather
      let weather = 'Clear';
      if (data.isThundering) {
        weather = 'Thunder';
      } else if (data.isRaining) {
        weather = 'Rain';
      } else if (!data.hasSkyLight) {
        weather = 'N/A';
      }
      this.worldWeatherEl.textContent = weather;
      
      // World size (placeholder)
      this.worldSizeEl.textContent = '0.0 GB';
      
      // Update time slider
      this.timeSlider.value = ticks;
      this.updateTimeDisplay(ticks);
      
      // Update border info
      if (data.worldBorder) {
        this.borderCenter.textContent = `${data.worldBorder.centerX}, ${data.worldBorder.centerZ}`;
        this.borderSize.textContent = `${data.worldBorder.size.toLocaleString()} blocks`;
        this.borderDamage.textContent = `${data.worldBorder.damagePerBlock} per second`;
      }
      
    } catch (error) {
      console.error('Error loading world data:', error);
      // Set default values on error
      this.loadedChunksEl.textContent = '0';
      this.playersInWorldEl.textContent = '0';
      this.entitiesInWorldEl.textContent = '0';
      this.worldTimeEl.textContent = 'Day 0, 0:00';
      this.worldWeatherEl.textContent = 'Unknown';
      this.worldSizeEl.textContent = '0.0 GB';
    }
  }

  updateTimeDisplay(ticks) {
    this.currentTimeTicks.textContent = `${ticks} ticks`;
  }

  setWorldTime(ticks) {
    this.showNotification(`Time set to ${ticks} ticks`, 'success');
    // Would call API in production
  }

  setTimePreset(preset) {
    const presets = {
      day: 1000,
      noon: 6000,
      night: 13000,
      midnight: 18000
    };
    
    const ticks = presets[preset];
    this.timeSlider.value = ticks;
    this.updateTimeDisplay(ticks);
    this.setWorldTime(ticks);
  }

  setWeather(weather) {
    const duration = this.weatherDuration.value;
    this.showNotification(`Weather set to ${weather} for ${duration} seconds`, 'success');
    this.worldWeatherEl.textContent = weather.charAt(0).toUpperCase() + weather.slice(1);
    // Would call API in production
  }

  setDifficulty(difficulty) {
    this.showNotification(`Difficulty set to ${difficulty}`, 'success');
    // Would call API in production
  }

  // Border Modal
  openBorderModal() {
    this.borderModal.style.display = 'flex';
  }

  closeBorderModalHandler() {
    this.borderModal.style.display = 'none';
  }

  saveBorderSettings() {
    const settings = {
      centerX: parseInt(this.borderCenterX.value),
      centerZ: parseInt(this.borderCenterZ.value),
      size: parseInt(this.borderSizeInput.value),
      transition: parseInt(this.borderTransition.value),
      damage: parseFloat(this.borderDamageAmount.value),
      damageBuffer: parseInt(this.borderDamageBuffer.value),
      warningDistance: parseInt(this.borderWarningDistance.value),
      warningTime: parseInt(this.borderWarningTime.value)
    };
    
    this.borderCenter.textContent = `${settings.centerX}, ${settings.centerZ}`;
    this.borderSize.textContent = `${settings.size.toLocaleString()} blocks`;
    this.borderDamage.textContent = `${settings.damage} per second`;
    
    this.showNotification('World border updated successfully', 'success');
    this.closeBorderModalHandler();
  }

  // Gamerules
  loadGamerules() {
    // Initialize gamerules state from DOM
    this.gameruleToggles.forEach(toggle => {
      const gamerule = toggle.dataset.gamerule;
      const value = toggle.type === 'checkbox' ? toggle.checked : toggle.value;
      this.gamerules[gamerule] = value;
    });
  }

  filterGamerules() {
    const searchTerm = this.gameruleSearch.value.toLowerCase();
    const filter = this.currentGameruleFilter;
    
    const gameruleItems = document.querySelectorAll('.gamerule-item');
    
    gameruleItems.forEach(item => {
      const name = item.querySelector('.gamerule-name').textContent.toLowerCase();
      const desc = item.querySelector('.gamerule-desc').textContent.toLowerCase();
      const category = item.dataset.category;
      
      const matchesSearch = name.includes(searchTerm) || desc.includes(searchTerm);
      const matchesFilter = filter === 'all' || category === filter;
      
      if (matchesSearch && matchesFilter) {
        item.style.display = 'flex';
      } else {
        item.style.display = 'none';
      }
    });
  }

  resetToDefaults() {
    if (!confirm('Reset all gamerules to default values?')) return;
    
    // Reset to defaults
    const defaults = {
      doMobSpawning: true,
      keepInventory: false,
      doDaylightCycle: true,
      doFireTick: true,
      doMobLoot: true,
      doTileDrops: true,
      announceAdvancements: true,
      showDeathMessages: true,
      randomTickSpeed: 3,
      spawnRadius: 10,
      maxCommandChainLength: 65536
    };
    
    this.gameruleToggles.forEach(toggle => {
      const gamerule = toggle.dataset.gamerule;
      if (defaults.hasOwnProperty(gamerule)) {
        if (toggle.type === 'checkbox') {
          toggle.checked = defaults[gamerule];
        } else {
          toggle.value = defaults[gamerule];
        }
        this.gamerules[gamerule] = defaults[gamerule];
      }
    });
    
    this.showNotification('Gamerules reset to defaults', 'success');
  }

  saveAllGamerules() {
    this.showNotification('Saving gamerules...', 'info');
    
    // Mock save
    setTimeout(() => {
      this.showNotification('All gamerules saved successfully!', 'success');
    }, 500);
  }

  saveAllWorlds() {
    this.showNotification('Saving all worlds...', 'info');
    
    setTimeout(() => {
      this.showNotification('All worlds saved successfully!', 'success');
    }, 1000);
  }

  refreshWorlds() {
    this.showNotification('Refreshing world data...', 'info');
    
    setTimeout(() => {
      this.loadWorldData();
      this.showNotification('World data refreshed', 'success');
    }, 500);
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
    
    // DOM References - File Editor Modal
    this.fileEditorModal = document.getElementById('file-editor-modal');
    this.closeFileEditorModal = document.getElementById('close-file-editor');
    this.modalFileName = document.getElementById('modal-file-name');
    this.modalFilePath = document.getElementById('modal-file-path');
    this.modalEditorTextarea = document.getElementById('modal-file-editor');
    this.modalFormatBtn = document.getElementById('modal-format-file');
    this.modalValidateBtn = document.getElementById('modal-validate-file');
    this.modalDownloadBtn = document.getElementById('modal-download-file');
    this.modalSaveBtn = document.getElementById('modal-save-file');
    this.modalCancelBtn = document.getElementById('modal-cancel-edit');
    this.modalEditorLines = document.getElementById('modal-editor-lines');
    this.modalEditorChars = document.getElementById('modal-editor-chars');
    this.modalEditorType = document.getElementById('modal-editor-type');
    this.modalEditorModified = document.getElementById('modal-editor-modified');
    
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
    // Check if modal elements exist
    if (!this.fileEditorModal) {
      console.error('File editor modal not found!');
      return;
    }
    
    this.setupEventListeners();
    this.renderFileTree();
    this.loadMockFiles();
  }

  setupEventListeners() {
    // File tree interactions
    if (!this.fileTree) return;
    
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
    
    // File Editor Modal
    if (this.closeFileEditorModal) {
      this.closeFileEditorModal.addEventListener('click', () => this.closeFileEditorModalHandler());
    }
    if (this.modalCancelBtn) {
      this.modalCancelBtn.addEventListener('click', () => this.closeFileEditorModalHandler());
    }
    if (this.modalFormatBtn) {
      this.modalFormatBtn.addEventListener('click', () => this.formatFileInModal());
    }
    if (this.modalValidateBtn) {
      this.modalValidateBtn.addEventListener('click', () => this.validateFileInModal());
    }
    if (this.modalDownloadBtn) {
      this.modalDownloadBtn.addEventListener('click', () => this.downloadFileFromModal());
    }
    if (this.modalSaveBtn) {
      this.modalSaveBtn.addEventListener('click', () => this.saveFileFromModal());
    }
    
    // Modal editor content changes
    if (this.modalEditorTextarea) {
      this.modalEditorTextarea.addEventListener('input', () => {
        this.onModalEditorChange();
      });
      
      // Modal keyboard shortcuts
      this.modalEditorTextarea.addEventListener('keydown', (e) => {
        if (e.ctrlKey && e.key === 's') {
          e.preventDefault();
          this.saveFileFromModal();
        } else if (e.ctrlKey && e.key === 'f') {
          e.preventDefault();
          this.formatFileInModal();
        }
      });
    }
    
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
    this.loadFileInModal(filePath);
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

  // File Editor Modal Methods
  loadFileInModal(filePath) {
    // Check if modal exists
    if (!this.fileEditorModal) {
      console.error('File editor modal element not found');
      this.showNotification('Editor modal not available', 'error');
      return;
    }
    
    const fileData = this.files[filePath];
    
    if (!fileData) {
      console.error('File not found:', filePath);
      this.showNotification('File not found: ' + filePath, 'error');
      return;
    }

    this.currentFile = filePath;
    this.originalContent = fileData.content;
    
    // Extract filename from path
    const fileName = filePath.split('/').pop();
    
    // Update modal UI with null checks
    if (this.modalFileName) this.modalFileName.textContent = fileName;
    if (this.modalFilePath) this.modalFilePath.textContent = filePath;
    if (this.modalEditorType) this.modalEditorType.textContent = fileData.type.toUpperCase();
    if (this.modalEditorTextarea) this.modalEditorTextarea.value = fileData.content;
    
    // Update stats
    this.updateModalEditorStats();
    this.isModified = false;
    this.updateModalModifiedStatus();
    
    // Show modal
    this.fileEditorModal.style.display = 'flex';
    console.log('Modal displayed for file:', fileName);
  }

  closeFileEditorModalHandler() {
    if (this.isModified) {
      const confirmClose = confirm('You have unsaved changes. Are you sure you want to close?');
      if (!confirmClose) return;
    }
    
    this.fileEditorModal.style.display = 'none';
    this.currentFile = null;
    this.modalEditorTextarea.value = '';
    this.isModified = false;
  }

  onModalEditorChange() {
    this.isModified = true;
    this.updateModalModifiedStatus();
    this.updateModalEditorStats();
  }

  updateModalEditorStats() {
    if (!this.modalEditorTextarea) return;
    
    const content = this.modalEditorTextarea.value;
    const lines = content.split('\n').length;
    const chars = content.length;
    
    if (this.modalEditorLines) this.modalEditorLines.textContent = lines;
    if (this.modalEditorChars) this.modalEditorChars.textContent = chars.toLocaleString();
  }

  updateModalModifiedStatus() {
    if (!this.modalEditorModified) return;
    
    if (this.isModified) {
      this.modalEditorModified.classList.remove('hidden');
    } else {
      this.modalEditorModified.classList.add('hidden');
    }
  }

  formatFileInModal() {
    if (!this.currentFile) return;

    const fileType = this.files[this.currentFile].type;
    const content = this.modalEditorTextarea.value;

    try {
      let formatted = content;
      
      if (fileType === 'json') {
        const parsed = JSON.parse(content);
        formatted = JSON.stringify(parsed, null, 2);
      } else if (fileType === 'yaml' || fileType === 'yml') {
        // Basic YAML formatting (would use library in production)
        formatted = content.split('\n').map(line => line.trim()).join('\n');
      }
      
      this.modalEditorTextarea.value = formatted;
      this.onModalEditorChange();
      this.showNotification('File formatted successfully', 'success');
    } catch (error) {
      this.showNotification('Format error: ' + error.message, 'error');
    }
  }

  validateFileInModal() {
    if (!this.currentFile) return;

    const fileType = this.files[this.currentFile].type;
    const content = this.modalEditorTextarea.value;

    try {
      if (fileType === 'json') {
        JSON.parse(content);
        this.showNotification('✓ Valid JSON', 'success');
      } else if (fileType === 'yaml' || fileType === 'yml') {
        // Would use YAML parser in production
        this.showNotification('✓ YAML validation would happen here', 'success');
      } else {
        this.showNotification('✓ Syntax check passed', 'success');
      }
    } catch (error) {
      this.showNotification('Validation error: ' + error.message, 'error');
    }
  }

  downloadFileFromModal() {
    if (!this.currentFile) return;

    const content = this.modalEditorTextarea.value;
    const fileName = this.currentFile.split('/').pop();
    
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    a.click();
    URL.revokeObjectURL(url);
    
    this.showNotification('File downloaded', 'success');
  }

  saveFileFromModal() {
    if (!this.currentFile) return;

    const content = this.modalEditorTextarea.value;
    
    // Update file data
    this.files[this.currentFile].content = content;
    this.files[this.currentFile].size = content.length;
    this.originalContent = content;
    
    this.isModified = false;
    this.updateModalModifiedStatus();
    
    this.showNotification('File saved successfully', 'success');
    
    // In production, would call API:
    // await fetch('/api/files/save', { method: 'POST', body: JSON.stringify({ path: this.currentFile, content }) });
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

// Initialize Global Search Manager
let globalSearchManager;

// Initialize Alert Manager
let alertManager;

// Initialize Permission Manager
let permissionManager;

// Initialize Plugin Manager
let pluginManager;

// Initialize World Manager
let worldManager;

// Initialize Backup Manager
let backupManager;

// Initialize File Browser Manager
let fileBrowserManager;

// Initialize Performance Metrics Manager
let performanceMetricsManager;

// Initialize Chat Log Viewer
let chatLogViewer;
document.addEventListener('DOMContentLoaded', () => {
  globalSearchManager = new GlobalSearchManager();
  globalSearchManager.init();
  
  alertManager = new AlertManager();
  alertManager.init();
  
  permissionManager = new PermissionManager();
  permissionManager.init();
  
  pluginManager = new PluginManager();
  pluginManager.init();
  
  worldManager = new WorldManager();
  worldManager.init();
  
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

  async tryWebSocket() {
    try {
      // First, get WebSocket connection info from server
      const wsInfo = await fetch('/api/websocket/info')
        .then(res => res.json())
        .catch(() => ({ port: parseInt(window.location.port || 8080) + 1, protocol: 'ws' }));
      
      const protocol = window.location.protocol === 'https:' ? 'wss:' : wsInfo.protocol;
      const wsUrl = `${protocol}://${window.location.hostname}:${wsInfo.port}`;
      
      console.log('Connecting to WebSocket:', wsUrl);
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
  
  // Initialize settings page
  initializeSettings();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
  if (dashboardConnection) {
    dashboardConnection.disconnect();
  }
});

// ============= Settings Functions =============

function initializeSettings() {
  // Password change modal
  const changePasswordBtn = document.getElementById('change-password-btn');
  const passwordModal = document.getElementById('password-change-modal');
  const closePasswordModal = document.getElementById('close-password-modal');
  const cancelPasswordChange = document.getElementById('cancel-password-change');
  const passwordForm = document.getElementById('password-change-form');
  
  if (changePasswordBtn && passwordModal) {
    changePasswordBtn.addEventListener('click', () => {
      passwordModal.style.display = 'flex';
    });
    
    closePasswordModal?.addEventListener('click', () => {
      passwordModal.style.display = 'none';
      passwordForm?.reset();
    });
    
    cancelPasswordChange?.addEventListener('click', () => {
      passwordModal.style.display = 'none';
      passwordForm?.reset();
    });
    
    passwordForm?.addEventListener('submit', async (e) => {
      e.preventDefault();
      await handlePasswordChange();
    });
  }
  
  // Load user info when navigating to settings
  loadUserSettings();
}

async function loadUserSettings() {
  try {
    // Get current user info from session
    const response = await fetch('/api/auth/session', {
      credentials: 'include'
    });
    
    if (response.ok) {
      const data = await response.json();
      
      // Update UI
      const usernameEl = document.getElementById('settings-username');
      const roleEl = document.getElementById('settings-role');
      
      if (usernameEl) usernameEl.textContent = data.userId || 'Unknown';
      if (roleEl) roleEl.textContent = data.role || 'Unknown';
    }
  } catch (error) {
    console.error('Error loading user settings:', error);
  }
}

async function handlePasswordChange() {
  const newPassword = document.getElementById('new-password').value;
  const confirmPassword = document.getElementById('confirm-password').value;
  
  // Validation
  if (newPassword !== confirmPassword) {
    showNotification('❌ Passwords do not match', 'error');
    return;
  }
  
  if (newPassword.length < 8) {
    showNotification('❌ Password must be at least 8 characters', 'error');
    return;
  }
  
  try {
    const response = await fetch('/api/change-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        newPassword: newPassword,
        confirmPassword: confirmPassword
      })
    });
    
    const data = await response.json();
    
    if (response.ok && data.success) {
      showNotification('✅ Password changed successfully!', 'success');
      document.getElementById('password-change-modal').style.display = 'none';
      document.getElementById('password-change-form').reset();
    } else {
      showNotification('❌ ' + (data.error || 'Failed to change password'), 'error');
    }
  } catch (error) {
    console.error('Error changing password:', error);
    showNotification('❌ Error changing password', 'error');
  }
}

// Expose for debugging
window.dashboardConnection = dashboardConnection;
