# NeoEssentials Web Dashboard

## Overview

The NeoEssentials Web Dashboard is a built-in RESTful API and web interface for monitoring and managing your Minecraft server. It provides real-time player statistics, server health metrics, log viewing, and config file editing capabilities.

## Features

### 🎮 Player Statistics
- Real-time list of online players
- Player ranks (Admin, Moderator, Helper, Player)
- XP levels for each player
- Additional metrics (health, dimension, UUID)

### 🖥️ Server Statistics
- Server status (Online/Offline)
- TPS (Ticks Per Second) monitoring
- Player count (online/max players)
- Server health percentage
- Memory usage statistics
- Server uptime tracking
- World information
- Minecraft version

### 📝 Log Viewer
- Real-time server log streaming
- Last 100 lines by default (configurable up to 1000)
- Automatic filtering of sensitive information (IPs, UUIDs, passwords)
- Auto-refresh every 10 seconds

### ⚙️ Config Editor
- List all NeoEssentials config files
- View and edit JSON configuration
- Real-time config updates
- Type-safe editing (text, number, boolean, array, object)

## Installation

The web dashboard is built directly into NeoEssentials. No additional mods or dependencies are required.

### File Structure

The dashboard frontend files are automatically deployed from your mod resources to the server's data directory:

```
server/
└── data/
    └── webdashboard/
        ├── index.html
        ├── space-dashboard.js
        ├── space-theme.css
        ├── space-glass.css
        └── orbitron.css
```

## Usage

### Starting the Dashboard

There are two ways to start the web dashboard:

#### Method 1: Manual Start (Recommended)
1. Start your Minecraft server
2. Run the command in-game or console: `/dashboard start`
3. Access the dashboard at `http://localhost:8080`

#### Method 2: Auto-Start on Server Launch
Edit `NeoEssentials.java` and uncomment this line in the `onServerStarting` method:
```java
// com.zerog.neoessentials.webdashboard.WebDashboardServer.getInstance().start();
```

### Dashboard Commands

All commands require operator permission level 3 or higher.

| Command | Description |
|---------|-------------|
| `/dashboard` | Show help message |
| `/dashboard start` | Start the web dashboard server |
| `/dashboard stop` | Stop the web dashboard server |
| `/dashboard status` | Check if dashboard is running |
| `/dashboard port <number>` | Set custom port (future feature) |

### Accessing the Dashboard

Once started, access the dashboard by:
1. Opening a web browser
2. Navigate to `http://localhost:8080`
3. Click the link in the chat message (clickable URL)

**Note:** The dashboard is only accessible from the same machine running the server (localhost). To enable remote access, you would need to modify the binding address and configure firewall rules.

## API Endpoints

The dashboard provides a RESTful API that can be used by external applications:

### GET `/api/players`

Returns list of online players with statistics.

**Response:**
```json
{
  "players": [
    {
      "name": "Steve",
      "rank": "Admin",
      "xp": 42,
      "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "health": 20,
      "maxHealth": 20,
      "dimension": "minecraft:overworld"
    }
  ],
  "count": 1,
  "timestamp": 1704067200000
}
```

### GET `/api/server`

Returns server statistics and health metrics.

**Response:**
```json
{
  "status": "Online",
  "tps": 20.0,
  "online": 5,
  "maxPlayers": 20,
  "healthPercent": 95,
  "memory": {
    "used": 2048,
    "max": 4096,
    "free": 2048,
    "percentUsed": 50
  },
  "uptime": "2h 15m 30s",
  "uptimeMillis": 8130000,
  "worldName": "World",
  "difficulty": "normal",
  "version": "1.21.1",
  "timestamp": 1704067200000
}
```

### GET `/api/logs?lines=N`

Returns recent server log lines.

**Parameters:**
- `lines` (optional): Number of log lines to return (default: 100, max: 1000)

**Response:**
```json
{
  "logs": [
    "[12:00:00] [Server thread/INFO]: Starting minecraft server version 1.21.1",
    "[12:00:01] [Server thread/INFO]: Steve joined the game"
  ],
  "count": 2,
  "file": "latest.log",
  "success": true,
  "timestamp": 1704067200000
}
```

### GET `/api/config`

Returns list of all NeoEssentials config files with their options.

**Response:**
```json
{
  "configs": [
    {
      "name": "main.json",
      "path": "main.json",
      "options": [
        {
          "key": "chatEnabled",
          "label": "Chat Enabled",
          "type": "toggle",
          "value": true
        },
        {
          "key": "maxHomes",
          "label": "Max Homes",
          "type": "number",
          "value": 5
        }
      ]
    }
  ],
  "count": 1,
  "success": true,
  "timestamp": 1704067200000
}
```

### POST `/api/config`

Update a config file.

**Request Body:**
```json
{
  "file": "main.json",
  "config": {
    "chatEnabled": true,
    "maxHomes": 10
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Config updated successfully",
  "file": "main.json",
  "timestamp": 1704067200000
}
```

## CORS Support

All API endpoints include CORS headers to allow cross-origin requests:
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type`

This enables the dashboard to be accessed from any domain (useful for development).

## Security Considerations

### Sensitive Information Filtering

The log viewer automatically filters:
- IP addresses → `***.***.***.**`
- UUIDs → `********-****-****-****-************`
- Passwords/tokens → `password=***`

### Directory Traversal Protection

The config endpoint validates file paths to prevent directory traversal attacks. Only files within the `config/neoessentials/` directory can be accessed.

### Localhost Only

By default, the server only binds to `localhost` (127.0.0.1), making it inaccessible from other machines on the network.

### Operator Permissions

In-game dashboard commands require operator level 3 or higher.

## Customization

### Changing the Port

The default port is 8080. To change it, modify the `WebDashboardServer` class:

```java
private WebDashboardServer(int port) {
    this.port = port;
    // ...
}

public static WebDashboardServer getInstance() {
    if (INSTANCE == null) {
        INSTANCE = new WebDashboardServer(9090); // Custom port
    }
    return INSTANCE;
}
```

### Customizing the Frontend

The dashboard frontend files are located in:
```
src/main/resources/data/webdashboard/
```

You can edit:
- `index.html` - Page structure
- `space-dashboard.js` - JavaScript logic and API calls
- `space-theme.css` - Space-themed styling
- `space-glass.css` - Glass morphism effects
- `orbitron.css` - Orbitron font styling

After editing, rebuild the mod with `./gradlew build` to update the files.

### Adding Custom API Endpoints

1. Create a new handler class in `com.zerog.neoessentials.webdashboard.handlers`
2. Implement `HttpHandler` interface
3. Register in `WebDashboardServer.registerApiEndpoints()`

Example:
```java
package com.zerog.neoessentials.webdashboard.handlers;

public class CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Your custom logic here
    }
}
```

Register:
```java
server.createContext("/api/custom", new CustomHandler());
```

## Troubleshooting

### Dashboard Won't Start

**Error: "Port already in use"**
- Another application is using port 8080
- Solution: Change the port or stop the conflicting application

**Error: "Permission denied"**
- Ports below 1024 require administrator privileges
- Solution: Use a port above 1024 (e.g., 8080, 9090)

### Can't Access Dashboard

**Check these items:**
1. Server is running: `/dashboard status`
2. Dashboard is started: `/dashboard start`
3. Using correct URL: `http://localhost:8080`
4. Browser allows localhost connections
5. No firewall blocking the port

### Logs Not Showing

**If logs are empty or not updating:**
1. Check that `logs/latest.log` exists
2. Verify file permissions
3. Check browser console for JavaScript errors

### Config Changes Not Saving

**If config edits don't persist:**
1. Verify operator permissions (level 3+)
2. Check file write permissions in `config/neoessentials/`
3. Ensure config files are valid JSON format
4. Check server console for error messages

## Performance

The web dashboard is designed to be lightweight:
- **Memory:** < 5 MB overhead
- **CPU:** Minimal impact (only active during HTTP requests)
- **Network:** ~1-5 KB per API call
- **Thread Pool:** 4 threads for handling concurrent requests

## Browser Compatibility

Tested and working on:
- ✅ Chrome/Chromium 90+
- ✅ Firefox 88+
- ✅ Edge 90+
- ✅ Safari 14+
- ✅ Opera 76+

Requires:
- JavaScript enabled
- Chart.js support (automatically loaded)
- Fetch API support (all modern browsers)

## Future Enhancements

Planned features for future releases:
- [ ] Real-time WebSocket updates (instead of polling)
- [ ] Player inventory viewer
- [ ] World map integration
- [ ] Plugin management interface
- [ ] Performance graphs with historical data
- [ ] Mobile-responsive design improvements
- [ ] Authentication system
- [ ] Multi-user support with different permission levels
- [ ] Custom dashboard themes
- [ ] Export/import config functionality

## Contributing

Found a bug or have a feature request? Please open an issue on the GitHub repository.

## License

The web dashboard is part of NeoEssentials and uses the same license as the main mod.

---

**Made with 💙 for the Minecraft community**
