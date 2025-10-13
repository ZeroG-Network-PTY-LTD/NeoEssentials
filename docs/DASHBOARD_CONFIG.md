# Web Dashboard Configuration Guide

## Overview

The Web Dashboard system is fully configurable through the `config.json` file located in `config/neoessentials/config.json`.

## Config Version

The config has been updated from version **7** to version **8** to include web dashboard settings.

```json
{
  "_configVersion": 8
}
```

## Module Enable/Disable

### Enable Web Dashboard Module

Located in `modules` section:

```json
"modules": {
  "webDashboardEnabled": true
}
```

- **`webDashboardEnabled`**: Master switch for the entire web dashboard system
  - `true` (default): Dashboard can be started with `/dashboard start`
  - `false`: Dashboard is completely disabled, command will not work

## Web Dashboard Section

The main configuration section for all dashboard settings:

```json
"webDashboard": {
  "enabled": true,
  "autoStart": false,
  "port": 8080,
  "bindAddress": "127.0.0.1",
  "enableCORS": true,
  "maxThreads": 4,
  // ... more settings
}
```

### Core Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `enabled` | boolean | `true` | Enable/disable the web dashboard (same as module setting) |
| `autoStart` | boolean | `false` | Automatically start dashboard when server starts |
| `port` | integer | `8080` | HTTP port for the dashboard (1024-65535) |
| `bindAddress` | string | `"127.0.0.1"` | IP address to bind to (`127.0.0.1` = localhost only, `0.0.0.0` = all interfaces) |
| `enableCORS` | boolean | `true` | Enable Cross-Origin Resource Sharing headers |
| `maxThreads` | integer | `4` | Maximum concurrent HTTP request handler threads |

### API Settings

Configure API endpoints behavior:

```json
"apiSettings": {
  "enablePlayersEndpoint": true,
  "enableServerEndpoint": true,
  "enableLogsEndpoint": true,
  "enableConfigEndpoint": true,
  "maxLogLines": 1000,
  "defaultLogLines": 100,
  "enableSensitiveInfoFiltering": true,
  "cacheTimeout": 5
}
```

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `enablePlayersEndpoint` | boolean | `true` | Enable `/api/players` endpoint |
| `enableServerEndpoint` | boolean | `true` | Enable `/api/server` endpoint |
| `enableLogsEndpoint` | boolean | `true` | Enable `/api/logs` endpoint |
| `enableConfigEndpoint` | boolean | `true` | Enable `/api/config` endpoints |
| `maxLogLines` | integer | `1000` | Maximum log lines that can be requested |
| `defaultLogLines` | integer | `100` | Default log lines when not specified |
| `enableSensitiveInfoFiltering` | boolean | `true` | Filter IPs, UUIDs, passwords from logs |
| `cacheTimeout` | integer | `5` | Cache API responses for N seconds (0 = disabled) |

### Security Settings

Control security and access:

```json
"securitySettings": {
  "requireAuthentication": false,
  "allowConfigEditing": true,
  "restrictedConfigPaths": [],
  "enableRateLimiting": false,
  "maxRequestsPerMinute": 60
}
```

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `requireAuthentication` | boolean | `false` | Require auth to access (future feature) |
| `allowConfigEditing` | boolean | `true` | Allow editing configs via web interface |
| `restrictedConfigPaths` | array | `[]` | Config files that cannot be edited |
| `enableRateLimiting` | boolean | `false` | Enable API rate limiting (future) |
| `maxRequestsPerMinute` | integer | `60` | Max requests per IP when rate limiting enabled |

### UI Settings

Configure the user interface:

```json
"uiSettings": {
  "refreshInterval": 10,
  "enableCharts": true,
  "theme": "space",
  "showPlayerUUIDs": false,
  "enableNotifications": true
}
```

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `refreshInterval` | integer | `10` | Auto-refresh interval in seconds |
| `enableCharts` | boolean | `true` | Enable performance charts |
| `theme` | string | `"space"` | Dashboard theme (currently only "space") |
| `showPlayerUUIDs` | boolean | `false` | Show UUIDs in player list |
| `enableNotifications` | boolean | `true` | Enable browser notifications |

### Logging Settings

Control dashboard logging:

```json
"loggingSettings": {
  "logDashboardAccess": true,
  "logAPIRequests": false,
  "logConfigChanges": true
}
```

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `logDashboardAccess` | boolean | `true` | Log when dashboard is accessed |
| `logAPIRequests` | boolean | `false` | Log all API requests (verbose) |
| `logConfigChanges` | boolean | `true` | Log config modifications |

## Commands Configuration

The `/dashboard` command can be enabled/disabled in the `commands` section:

```json
"commands": {
  "dashboard": true
}
```

- **`dashboard`**: Enable/disable the `/dashboard` command
  - `true` (default): Command is available
  - `false`: Command is disabled

## Usage Examples

### Example 1: Localhost Only (Secure)

Default configuration - only accessible from the server machine:

```json
"webDashboard": {
  "enabled": true,
  "autoStart": false,
  "port": 8080,
  "bindAddress": "127.0.0.1"
}
```

Access: `http://localhost:8080`

### Example 2: Auto-Start on Server Launch

Dashboard starts automatically:

```json
"webDashboard": {
  "enabled": true,
  "autoStart": true,
  "port": 8080
}
```

### Example 3: Custom Port

Using port 9090 instead of default:

```json
"webDashboard": {
  "enabled": true,
  "port": 9090
}
```

Access: `http://localhost:9090`

### Example 4: Network Access (Less Secure)

Allow access from other computers on the network:

```json
"webDashboard": {
  "enabled": true,
  "bindAddress": "0.0.0.0",
  "port": 8080
}
```

⚠️ **Warning**: This allows anyone on your network to access the dashboard!

Access: `http://YOUR_SERVER_IP:8080`

### Example 5: Read-Only Dashboard

Disable config editing:

```json
"webDashboard": {
  "enabled": true,
  "securitySettings": {
    "allowConfigEditing": false
  }
}
```

### Example 6: Restricted Configs

Prevent editing specific config files:

```json
"webDashboard": {
  "enabled": true,
  "securitySettings": {
    "allowConfigEditing": true,
    "restrictedConfigPaths": [
      "permissions.json",
      "economy.json"
    ]
  }
}
```

### Example 7: Minimal Logging

Reduce log output:

```json
"webDashboard": {
  "enabled": true,
  "loggingSettings": {
    "logDashboardAccess": false,
    "logAPIRequests": false,
    "logConfigChanges": true
  }
}
```

## Config Helper Methods

The `ConfigManager` class provides helper methods for accessing web dashboard settings:

```java
ConfigManager config = ConfigManager.getInstance();

// Module enabled
boolean enabled = config.isWebDashboardEnabled();

// Auto-start setting
boolean autoStart = config.isWebDashboardAutoStartEnabled();

// Port
int port = config.getWebDashboardPort();

// Bind address
String bindAddress = config.getWebDashboardBindAddress();

// CORS
boolean cors = config.isWebDashboardCORSEnabled();

// Threads
int threads = config.getWebDashboardMaxThreads();

// Config editing
boolean canEdit = config.isWebDashboardConfigEditingAllowed();

// Log lines
int maxLines = config.getWebDashboardMaxLogLines();
int defaultLines = config.getWebDashboardDefaultLogLines();
```

## Applying Configuration Changes

### Method 1: Server Restart

1. Stop the server
2. Edit `config/neoessentials/config.json`
3. Start the server

### Method 2: In-Game Reload (Some settings)

Some settings like `autoStart` only apply on server startup. Others may require:

1. `/dashboard stop`
2. Edit config
3. `/dashboard start`

### Method 3: Hot Reload (Future Feature)

A config reload command is planned for future releases.

## Security Best Practices

### ✅ Recommended Settings (Secure)

```json
"webDashboard": {
  "enabled": true,
  "autoStart": false,
  "port": 8080,
  "bindAddress": "127.0.0.1",
  "securitySettings": {
    "allowConfigEditing": true,
    "restrictedConfigPaths": ["permissions.json"]
  }
}
```

- Localhost only (`127.0.0.1`)
- Manual start
- Restricted permissions config editing

### ⚠️ Use With Caution

```json
"bindAddress": "0.0.0.0"
```

Only use when:
- Behind a firewall
- On a trusted network
- You understand the security implications

### ❌ Not Recommended

```json
"webDashboard": {
  "bindAddress": "0.0.0.0",
  "securitySettings": {
    "allowConfigEditing": true
  }
}
```

This combination allows anyone on the network to:
- View server stats
- See player information
- Read logs
- Edit configuration files

## Troubleshooting

### Dashboard Won't Start

**Check these settings:**

1. Module enabled:
   ```json
   "modules": { "webDashboardEnabled": true }
   ```

2. Dashboard enabled:
   ```json
   "webDashboard": { "enabled": true }
   ```

3. Port not in use:
   - Change `port` to different value (e.g., 9090)

4. Valid bind address:
   - Use `127.0.0.1` for localhost
   - Use `0.0.0.0` for all interfaces

### Can't Access From Browser

**Check:**

1. Dashboard is running: `/dashboard status`
2. Using correct URL: `http://localhost:PORT`
3. Using `http://` not `https://`
4. Firewall isn't blocking the port

### Config Changes Not Working

**Verify:**

1. Syntax is valid JSON
2. Config version is 8 or higher
3. Server was restarted (for some settings)
4. Dashboard was restarted: `/dashboard stop` then `/dashboard start`

## Migration from Previous Versions

If upgrading from NeoEssentials without web dashboard:

1. **Backup your config:**
   ```bash
   cp config/neoessentials/config.json config/neoessentials/config.json.backup
   ```

2. **The config will auto-update to version 8** when the server starts

3. **Default settings will be added** if missing:
   ```json
   "modules": {
     "webDashboardEnabled": true
   },
   "webDashboard": {
     "enabled": true,
     "autoStart": false,
     "port": 8080
   },
   "commands": {
     "dashboard": true
   }
   ```

4. **Verify the config** after first launch

## Advanced Configuration

### Multiple Servers

If running multiple Minecraft servers on the same machine:

```json
// Server 1
"webDashboard": { "port": 8080 }

// Server 2
"webDashboard": { "port": 8081 }

// Server 3
"webDashboard": { "port": 8082 }
```

### Development Mode

For development with auto-refresh:

```json
"webDashboard": {
  "enableCORS": true,
  "apiSettings": {
    "cacheTimeout": 0
  },
  "loggingSettings": {
    "logAPIRequests": true
  }
}
```

### Production Mode

For production with performance optimization:

```json
"webDashboard": {
  "enableCORS": false,
  "maxThreads": 8,
  "apiSettings": {
    "cacheTimeout": 10
  },
  "loggingSettings": {
    "logAPIRequests": false
  }
}
```

## See Also

- [Web Dashboard User Guide](WEB_DASHBOARD.md)
- [Implementation Details](DASHBOARD_IMPLEMENTATION.md)
- [Quick Start Guide](../DASHBOARD_QUICKSTART.md)

---

**Config Version: 8**  
**Last Updated: October 13, 2025**
