# 🚀 Web Dashboard Quick Start Guide

## What is the Web Dashboard?

A built-in web interface for your NeoEssentials Minecraft server that lets you:
- 👥 Monitor online players in real-time
- 📊 View server performance (TPS, memory, uptime)
- 📝 Read server logs without SSH
- ⚙️ Edit config files from your browser

## 5-Minute Setup

### Step 1: Build the Mod (if not already built)
```bash
./gradlew build
```

### Step 2: Start Your Minecraft Server
```bash
./gradlew runServer  # or your normal start method
```

### Step 3: Start the Dashboard
In-game or in server console:
```
/dashboard start
```

You should see:
```
╔════════════════════════════════════════════╗
║  Web Dashboard Started Successfully!       ║
║  Access at: http://localhost:8080          ║
╚════════════════════════════════════════════╝
```

### Step 4: Open Your Browser
Navigate to: **http://localhost:8080**

That's it! 🎉

## Common Commands

```
/dashboard         - Show help
/dashboard start   - Start the dashboard
/dashboard stop    - Stop the dashboard
/dashboard status  - Check if running
```

## Dashboard Sections

### 📊 Overview
- Live server status indicator
- TPS monitoring
- Player count
- Server health bar

### 👥 Players
- List of online players
- Player ranks (Admin, Moderator, Helper, Player)
- XP levels
- Current dimension

### 🖥️ Server Stats
- Detailed server metrics
- Memory usage graph
- Uptime tracker
- World information

### 📝 Logs
- Real-time server log viewer
- Last 100 lines displayed
- Auto-refresh every 10 seconds
- Sensitive info automatically filtered

### ⚙️ Settings
- View all NeoEssentials config files
- Edit settings directly from browser
- Save changes instantly
- Type-safe editing

## API for Developers

If you want to integrate with the dashboard:

```javascript
// Get player stats
fetch('http://localhost:8080/api/players')
  .then(res => res.json())
  .then(data => console.log(data));

// Get server stats
fetch('http://localhost:8080/api/server')
  .then(res => res.json())
  .then(data => console.log(data));

// Get logs (last 50 lines)
fetch('http://localhost:8080/api/logs?lines=50')
  .then(res => res.json())
  .then(data => console.log(data));

// Get config files
fetch('http://localhost:8080/api/config')
  .then(res => res.json())
  .then(data => console.log(data));
```

## Troubleshooting

### "Port already in use" Error
Another app is using port 8080. Either:
1. Stop the other app
2. Change dashboard port in `WebDashboardServer.java`

### Can't Access Dashboard
1. Check dashboard is running: `/dashboard status`
2. Make sure you're using `http://` not `https://`
3. Try `http://127.0.0.1:8080` instead of `localhost`
4. Check firewall isn't blocking port 8080

### Dashboard Shows No Data
1. Make sure server is fully started
2. Check server console for error messages
3. Press F12 in browser and check console for errors

## Security Notes

🔒 **Default Configuration:**
- Only accessible from the same machine (localhost)
- Requires operator level 3 for commands
- Sensitive info (IPs, UUIDs, passwords) filtered from logs
- Config files restricted to `config/neoessentials/` directory

⚠️ **Warning:** Do NOT expose the dashboard to the internet without:
1. Adding authentication
2. Using HTTPS
3. Implementing rate limiting
4. Restricting IP access

## Need Help?

📖 **Full Documentation:** `docs/WEB_DASHBOARD.md`  
🔧 **Implementation Details:** `docs/DASHBOARD_IMPLEMENTATION.md`  
🐛 **Found a Bug?** Open an issue on GitHub

## Auto-Start on Server Launch

Want the dashboard to start automatically?

Edit `src/main/java/com/zerog/neoessentials/NeoEssentials.java`:

Find this line in `onServerStarting`:
```java
// com.zerog.neoessentials.webdashboard.WebDashboardServer.getInstance().start();
```

Remove the `//` to uncomment it:
```java
com.zerog.neoessentials.webdashboard.WebDashboardServer.getInstance().start();
```

Rebuild the mod:
```bash
./gradlew build
```

Now the dashboard will start automatically when your server starts!

## What's Next?

- 🎨 Customize the theme in `space-theme.css`
- 📊 Add custom metrics to the API
- 🔌 Integrate with other mods
- 📱 Make it mobile-responsive

Enjoy your new web dashboard! 🌟

---

**Made with ❤️ by the NeoEssentials Team**
