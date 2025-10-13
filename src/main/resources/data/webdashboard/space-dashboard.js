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


// Fetch real stats from API
async function fetchDemoStats() {
  const API_BASE = 'http://localhost:8080/api';
  
  // Fetch player stats
  try {
    setLoading(document.getElementById('player-stats-content'), 'Loading player stats...');
    const playerResponse = await fetch(`${API_BASE}/players`);
    if (playerResponse.ok) {
      const playerData = await playerResponse.json();
      updatePlayerStats(playerData);
    } else {
      setError(document.getElementById('player-stats-content'), 'Failed to load player data');
    }
  } catch (error) {
    setError(document.getElementById('player-stats-content'), 'Connection error: ' + error.message);
  }
  
  // Fetch server stats
  try {
    setLoading(document.getElementById('server-stats-content'), 'Loading server stats...');
    const serverResponse = await fetch(`${API_BASE}/server`);
    if (serverResponse.ok) {
      const serverData = await serverResponse.json();
      updateServerStats(serverData);
      
      // Update TPS chart with real data
      renderServerChart({
        labels: ['Now'],
        tps: [serverData.tps || 20]
      });
    } else {
      setError(document.getElementById('server-stats-content'), 'Failed to load server data');
    }
  } catch (error) {
    setError(document.getElementById('server-stats-content'), 'Connection error: ' + error.message);
  }
  
  // Fetch logs
  try {
    setLoading(document.getElementById('log-viewer-content'), 'Loading logs...');
    const logsResponse = await fetch(`${API_BASE}/logs?lines=50`);
    if (logsResponse.ok) {
      const logsData = await logsResponse.json();
      updateLogViewer(logsData);
    } else {
      setError(document.getElementById('log-viewer-content'), 'Failed to load logs');
    }
  } catch (error) {
    setError(document.getElementById('log-viewer-content'), 'Connection error: ' + error.message);
  }
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
