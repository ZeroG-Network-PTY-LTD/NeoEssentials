# Notification System

NeoEssentials features a notification system that handles server events and administrative alerts. This system provides automatic notifications for important server events and allows administrators to monitor server activity through multiple channels.

## 📢 Notification System Overview

The notification system automatically tracks and reports on various server events:

- **Player Events** - Join/leave notifications
- **Server Events** - Start/stop notifications
- **Security Events** - Security alerts and threat detection
- **Performance Events** - Performance monitoring alerts
- **Command Execution** - Administrative command tracking

## 🎯 Notification Commands

### Basic Notification Management

#### `/notifications` - Main Notification Command
```bash
/notifications                  # Show notification system status
/notifications channels         # List available notification channels
/notifications test [channel]   # Send test notification
/notifications send <type> <message> # Send custom notification
```

**Permission**: `neoessentials.notifications.admin`

**Examples:**
```bash
# Show notification system status
/notifications

# List all notification channels
/notifications channels

# Send test notification to all channels
/notifications test

# Send test notification to specific channel
/notifications test email

# Send custom notification
/notifications send PLAYER_JOIN "Test join message"
```

## 📋 Event Types

The notification system supports the following event types:

### Player Events
- **PLAYER_JOIN** - Player joins the server
- **PLAYER_LEAVE** - Player leaves the server

### Server Events  
- **SERVER_START** - Server startup completed
- **SERVER_STOP** - Server shutdown initiated

### Security Events
- **SECURITY_ALERT** - Security threats detected
- **COMMAND_EXECUTION** - Administrative command tracking

### Performance Events
- **PERFORMANCE_ALERT** - Performance threshold warnings

## 🔧 Notification Channels

### Log Channel
All notifications are automatically logged to the server console and log files.

**Features:**
- **Always Available** - Built-in logging channel
- **Detailed Information** - Includes timestamps and metadata
- **File Storage** - Persistent notification history

### Email Channel (Optional)
Email notifications for critical events when configured.

**Configuration:**
- Requires SMTP server configuration
- Disabled by default
- Used for critical alerts when administrators are offline

**Note**: Email functionality requires additional configuration and may need external dependencies.

## ⚙️ Configuration

The notification system is automatically configured and enabled. Notifications are triggered by server events and sent to configured channels.

### Automatic Event Detection
The following events automatically trigger notifications:

- **Player joins/leaves** - Tracked automatically
- **Server start/stop** - System lifecycle events  
- **Security alerts** - When threats are detected
- **Performance issues** - When thresholds are exceeded
- **Command execution** - Administrative command monitoring

## 🛠️ Administrative Features

### Monitoring Commands
Track server activity through notifications:

```bash
/notifications                  # View system status
/notifications channels         # List active notification channels
```

### Testing Notifications
Test notification delivery:

```bash
/notifications test             # Send test to all channels
/notifications test log         # Test log channel
/notifications test email       # Test email channel (if configured)
```

### Custom Notifications
Send custom administrative notifications:

```bash
/notifications send SECURITY_ALERT "Suspicious activity detected"
/notifications send PERFORMANCE_ALERT "High memory usage warning"
```

## � Permissions

Control access to notification system features:

```yaml
# Basic notification permissions
neoessentials.notifications.admin     # Access to notification commands
neoessentials.notifications.test      # Send test notifications
neoessentials.notifications.send      # Send custom notifications
```

## �🔧 Troubleshooting

### Common Issues

#### Notifications Not Being Sent
- Check if notification system is enabled
- Verify event types are being triggered
- Check server logs for notification errors
- Test notification channels individually

#### Email Notifications Not Working
- Verify SMTP configuration is correct
- Check email server credentials
- Ensure firewall allows SMTP connections
- Test email connectivity outside NeoEssentials

#### Missing Notification Events
- Check if specific event types are enabled
- Verify events are actually occurring
- Review notification channel configuration
- Check for permission issues

### Debug Commands
```bash
/notifications                  # Show system status and configuration
/notifications channels         # List all available channels
/notifications test             # Test notification delivery
```

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - General system configuration
- **[Security](Security.md)** - Security event notifications
- **[Performance](Performance.md)** - Performance monitoring
- **[Server Administration](Server-Administration.md)** - Administrative tools

*Last Updated: August 9, 2025*
