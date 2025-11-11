
# Web Dashboard System

## Overview
The Web Dashboard System in NeoEssentials provides a built-in web interface for server monitoring, management, and configuration. It supports real-time updates, API endpoints, and customizable UI features, all governed by configuration and permissions.

---

## Main Files
- `webdashboard/index.html`: Dashboard UI
- `webdashboard/dashboard.js`: Dashboard logic and API calls
- `webdashboard/styles.css`: Dashboard styling

---

## Configuration (`config.json` > `webDashboard`)
- `enabled`: Enable/disable dashboard system
- `autoStart`: Auto-start dashboard on server start
- `port` / `websocketPort`: HTTP and WebSocket ports
- `bindAddress`: IP address to bind dashboard server
- `enableCORS`: Enable cross-origin resource sharing
- `maxThreads`: Max concurrent HTTP handler threads
- `apiSettings`: Enable/disable API endpoints, log line limits, sensitive info filtering, cache timeout
- `securitySettings`: Config editing, restricted paths, authentication (future), rate limiting (future)
- `uiSettings`: Refresh interval, charts, theme, show player UUIDs, notifications
- `loggingSettings`: Log dashboard access, API requests, config changes

---

## Features
- Real-time server monitoring and management
- API endpoints for players, server, logs, and config
- Config editing via dashboard (with restrictions)
- Performance charts and notifications
- Customizable UI theme and refresh interval
- Logging of dashboard access and config changes

---

## Permissions
- `neoessentials.dashboard.view`: Required to access dashboard
- Additional permissions may be required for config editing and sensitive actions

---

## Commands
- `/dashboard start`: Start dashboard if not auto-started

---

## Logging
- Dashboard access and config changes are logged if enabled in config

---

## Notes
- All features and limits are strictly controlled by config and permissions
- For advanced usage, refer to the comments in `config.json` for each setting