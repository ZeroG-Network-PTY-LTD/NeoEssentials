// Sidebar button interactivity
document.addEventListener('DOMContentLoaded', function() {
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
});
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

// Chart.js server performance chart
let serverChart;
function renderServerChart(data) {
  // data: { labels: [], tps: [] }
  const ctx = document.getElementById('serverChart').getContext('2d');
  if (serverChart) serverChart.destroy();
  serverChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: data.labels,
      datasets: [{
        label: 'TPS',
        data: data.tps,
        borderColor: '#00ffae',
        backgroundColor: 'rgba(126,200,227,0.18)',
        pointBackgroundColor: '#7ec8e3',
        pointRadius: 3,
        tension: 0.3
      }]
    },
    options: {
      responsive: false,
      plugins: {
        legend: { display: false },
        tooltip: { enabled: true }
      },
      scales: {
        x: { display: true, title: { display: false } },
        y: { display: true, min: 0, max: 20 }
      }
    }
  });
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

// Modal interactivity
const playerDetailsBtn = document.getElementById('player-details-btn');
const playerDetailsModal = document.getElementById('player-details-modal');
const closeModalBtn = document.getElementById('close-modal-btn');

if (playerDetailsBtn && playerDetailsModal && closeModalBtn) {
  playerDetailsBtn.addEventListener('click', () => {
    playerDetailsModal.showModal();
    // Example: load player details (replace with real data)
    playerDetailsModal.querySelector('.modal-content').textContent = 'Astronaut: Steve\nXP: 42\nLocation: Mars Base\nRank: Commander';
  });
  closeModalBtn.addEventListener('click', () => {
    playerDetailsModal.close();
  });
}
// --- Config Modal Logic ---
document.addEventListener('DOMContentLoaded', function() {
  const openConfigBtn = document.getElementById('open-config-modal');
  const configModal = document.getElementById('config-modal');
  const closeConfigBtn = document.getElementById('close-config-modal');
  const configFilesList = document.getElementById('config-files-list');
  const saveConfigBtn = document.getElementById('save-config-btn');

  // Mock config files and options (simulate Java backend)
  const mockConfigFiles = [
    {
      name: 'server.properties',
      options: [
        { key: 'max-players', label: 'Max Players', type: 'number', value: 20 },
        { key: 'motd', label: 'Message of the Day', type: 'text', value: 'Welcome to SpaceCraft!' },
        { key: 'online-mode', label: 'Online Mode', type: 'checkbox', value: true }
      ]
    },
    {
      name: 'mods.cfg',
      options: [
        { key: 'enable-mod-x', label: 'Enable Mod X', type: 'checkbox', value: false },
        { key: 'mod-x-difficulty', label: 'Mod X Difficulty', type: 'select', value: 'normal', choices: ['easy','normal','hard'] }
      ]
    }
  ];

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
    openConfigBtn.addEventListener('click', function() {
      configModal.style.display = 'flex';
      renderConfigFiles(mockConfigFiles);
    });
  }
  if (closeConfigBtn) {
    closeConfigBtn.addEventListener('click', function() {
      configModal.style.display = 'none';
    });
  }
  if (saveConfigBtn) {
    saveConfigBtn.addEventListener('click', function() {
      // Collect values and simulate save
      let updated = [];
      mockConfigFiles.forEach(file => {
        let fileUpdate = { name: file.name, options: [] };
        file.options.forEach(opt => {
          let input = document.getElementById(`${file.name}-${opt.key}`);
          let value;
          if (opt.type === 'checkbox') {
            value = input.checked;
          } else {
            value = input.value;
          }
          fileUpdate.options.push({ key: opt.key, value });
        });
        updated.push(fileUpdate);
      });
      alert('Config changes saved! (Demo)\n' + JSON.stringify(updated, null, 2));
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
  if (el) el.innerHTML = `<div class="loading-indicator">${msg}</div>`;
}

function setError(el, msg) {
  if (el) el.innerHTML = `<div class="error-indicator">${msg}</div>`;
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


// Demo: Simulate backend fetch (replace with real API calls)
function fetchDemoStats() {
  setLoading(document.getElementById('player-stats-content'), 'Loading player stats...');
  setLoading(document.getElementById('server-stats-content'), 'Loading server stats...');
  setLoading(document.getElementById('log-viewer-content'), 'Loading logs...');
  setTimeout(() => {
    // Simulate player stats
    updatePlayerStats({
      players: [
        { name: 'Steve', rank: 'Commander', xp: 42 },
        { name: 'Alex', rank: 'Engineer', xp: 37 }
      ]
    });
    // Simulate server stats
    updateServerStats({
      status: 'Online',
      tps: 19.8,
      online: 2,
      maxPlayers: 20,
      healthPercent: 90
    });
    // Simulate logs
    updateLogViewer({
      logs: [
        '[12:00] Server started.',
        '[12:01] Steve joined the game.',
        '[12:02] Alex joined the game.'
      ]
    });
  }, 800);
}

// Auto-refresh every 10 seconds (replace with backend polling)
function startAutoRefresh() {
  fetchDemoStats();
  setInterval(fetchDemoStats, 10000);
}

document.addEventListener('DOMContentLoaded', function() {
  startAutoRefresh();
});
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
