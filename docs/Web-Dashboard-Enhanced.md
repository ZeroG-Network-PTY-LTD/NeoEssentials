# NeoEssentials Web Dashboard

The NeoEssentials Web Dashboard provides a comprehensive, real-time monitoring and management interface for your Minecraft server. This enhanced system offers advanced features including performance monitoring, security tracking, and session management.

## Features

### 🎯 Core Functionality
- **Real-time Server Monitoring**: Live TPS, memory, CPU, and player statistics
- **Performance Analytics**: Historical performance data and trend analysis
- **Event Tracking**: Real-time server events with severity levels
- **Player Management**: Online player monitoring and session tracking
- **Economy Overview**: Shop analytics and transaction monitoring

### 🎨 User Interface
- **Multiple Themes**: Dark, Light, Neo, and Classic themes
- **Responsive Design**: Works on desktop, tablet, and mobile devices
- **Real-time Updates**: WebSocket-based live data streaming
- **Interactive Charts**: Performance graphs and analytics visualizations

### 🔧 Advanced Features
- **Session Management**: Track and manage dashboard user sessions
- **Security Monitoring**: Authentication logs and security events
- **Alert System**: Configurable alerts for server issues
- **Widget System**: Customizable dashboard widgets
- **Configuration Management**: Easy settings and preferences

## Installation

1. **Start the Dashboard**:
   ```
   /dashboard start
   ```

2. **Access the Dashboard**:
   Open your web browser and navigate to:
   ```
   http://localhost:8080
   ```
   (Default port is 8080, configurable via commands)

3. **Configure Settings**:
   ```
   /dashboard config show
   /dashboard config port 8080
   /dashboard config theme neo
   ```

## Commands

### Basic Commands
- `/dashboard status` - Show dashboard status
- `/dashboard start` - Start the web dashboard
- `/dashboard stop` - Stop the web dashboard
- `/dashboard restart` - Restart the dashboard

### Configuration Commands
- `/dashboard config show` - Display current configuration
- `/dashboard config port <port>` - Set dashboard port
- `/dashboard config theme <theme>` - Set dashboard theme
- `/dashboard config maxsessions <count>` - Set maximum sessions
- `/dashboard config realtime <true/false>` - Enable/disable real-time updates

### Analytics Commands
- `/dashboard analytics` - Show server analytics
- `/dashboard performance` - Display performance metrics
- `/dashboard events` - Show recent events
- `/dashboard sessions` - List active dashboard sessions
- `/dashboard alerts` - Display recent alerts
- `/dashboard security` - Show security events
- `/dashboard widgets` - List dashboard widgets

## Themes

The dashboard supports four built-in themes:

### Neo Theme (Default)
- Dark GitHub-inspired design
- Blue accent colors
- Modern, clean interface

### Dark Theme
- Pure dark background
- High contrast text
- Minimal design

### Light Theme
- Clean white background
- Professional appearance
- Easy on the eyes

### Classic Theme
- Traditional dark blue design
- Minecraft-inspired colors
- Familiar interface

## Configuration

### Dashboard Settings
The dashboard can be configured through commands or by editing the configuration files:

```json
{
  "port": 8080,
  "theme": "neo",
  "maxSessions": 10,
  "realTimeUpdates": true,
  "sslEnabled": false,
  "authRequired": false,
  "performanceMonitoring": true
}
```

### Performance Monitoring
- **TPS Monitoring**: Real-time server tick rate
- **Memory Tracking**: JVM memory usage and garbage collection
- **CPU Monitoring**: Server CPU utilization
- **Player Metrics**: Online players and connection statistics

### Security Features
- **Session Tracking**: Monitor dashboard access sessions
- **IP Logging**: Track access by IP address
- **Authentication**: Optional login requirements
- **Security Events**: Log suspicious activities

## API Endpoints

The dashboard provides REST API endpoints for integration:

### Data Endpoints
- `GET /api/dashboard/data` - Complete dashboard data
- `GET /api/server/stats` - Server statistics
- `GET /api/players/online` - Online players list
- `GET /api/economy/overview` - Economy statistics

### Management Endpoints
- `POST /api/dashboard/start` - Start dashboard
- `POST /api/dashboard/stop` - Stop dashboard
- `GET /api/dashboard/config` - Get configuration
- `POST /api/dashboard/config` - Update configuration

### WebSocket Events
- `stats_update` - Real-time statistics updates
- `event` - Server events notification
- `player_join` - Player joined event
- `player_leave` - Player left event
- `alert` - System alerts

## File Structure

```
src/main/resources/assets/neoessentials/web/
├── dashboard.html          # Main dashboard HTML
├── dashboard.js           # Enhanced JavaScript functionality
├── themes.css             # Theme definitions
└── README.md             # This documentation
```

## Troubleshooting

### Common Issues

**Dashboard won't start:**
- Check if port is already in use
- Verify permissions
- Check server logs for errors

**Can't access dashboard:**
- Verify the correct port and URL
- Check firewall settings
- Ensure dashboard is running

**Real-time updates not working:**
- WebSocket connection may be blocked
- Check browser console for errors
- Verify server WebSocket support

**Performance issues:**
- Reduce refresh rate
- Disable real-time updates temporarily
- Check server resources

### Debug Commands
```
/dashboard status          # Check current status
/dashboard performance     # View performance metrics
/dashboard events          # Check recent events
/dashboard security        # Review security logs
```

## Browser Compatibility

The dashboard is compatible with:
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

### Required Features
- WebSocket support
- ES6 JavaScript
- CSS Grid and Flexbox
- Local Storage

## Performance Tips

1. **Optimize Refresh Rate**: Adjust auto-refresh interval based on needs
2. **Use Appropriate Theme**: Dark themes may save battery on mobile
3. **Monitor Resource Usage**: Dashboard itself uses minimal resources
4. **WebSocket Connection**: Provides better performance than polling

## Security Considerations

1. **Network Access**: Limit dashboard access to trusted networks
2. **Authentication**: Enable if dashboard will be publicly accessible
3. **SSL/TLS**: Use HTTPS in production environments
4. **Regular Updates**: Keep the mod updated for security patches

## Contributing

To contribute to the dashboard development:

1. Test new features thoroughly
2. Follow existing code style
3. Document any new functionality
4. Report bugs with detailed information

## Support

For support and questions:
- Check server console logs
- Review this documentation
- Test with default settings
- Report issues with version information

---

**NeoEssentials Web Dashboard** - Enhanced server monitoring for Minecraft administrators.
