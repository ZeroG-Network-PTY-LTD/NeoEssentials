# Events System

NeoEssentials provides a notification-based event system that handles server events through the NotificationManager and dedicated event listeners. The system focuses on server monitoring, player tracking, and administrative notifications rather than complex event automation.

## 🎯 Event Architecture

### Event Implementation
The event system is built on Java-based event listeners that integrate with Minecraft Forge's event bus:

#### Core Event Listeners
- **NotificationEventListener** - Handles player join/leave and server events
- **PermissionEventListener** - Manages permission system integration  
- **NeoEssentialsEventHandler** - Handles block events and moderation (backup)

#### Notification Events
Events are processed through the NotificationManager with these types:
- **PLAYER_JOIN** - Player connection events
- **PLAYER_LEAVE** - Player disconnection events
- **SERVER_START** - Server startup notifications
- **SERVER_STOP** - Server shutdown notifications
- **COMMAND_EXECUTION** - Command execution monitoring
- **SECURITY_ALERT** - Security violation alerts
- **PERFORMANCE_ALERT** - Server performance warnings

## 📋 Event Configuration

### Configuration Location
Events are configured through the NotificationManager in JSON format, not TOML.

### Available Commands
Manage notifications through the `/notifications` command:

```bash
/notifications status                # Show notification system status
/notifications channels              # List all notification channels
/notifications test [channel]        # Send test notification
/notifications send <type> <message> # Send custom notification
```

### Event Types Configuration
Notification events can be enabled/disabled through the NotificationManager API:

```java
// Enable/disable specific event types
notificationManager.setEventEnabled(NotificationEvent.Type.PLAYER_JOIN, true);
notificationManager.setEventEnabled(NotificationEvent.Type.COMMAND_EXECUTION, false);

// Get enabled events
Set<NotificationEvent.Type> enabledEvents = notificationManager.getEnabledEvents();
```

## 🔧 Event Triggers

### Automatic Event Triggers
Events are automatically triggered by the event listeners:

#### Player Events
- **Player Join**: Triggered by `NotificationEventListener.onPlayerJoin()`
- **Player Leave**: Triggered by `NotificationEventListener.onPlayerLeave()`

#### Server Events  
- **Server Stop**: Triggered by `NotificationEventListener.onServerStop()`
- **Performance Alerts**: Triggered by `NotificationManager.notifyPerformanceAlert()`

#### Administrative Events
- **Command Execution**: Triggered by `NotificationManager.notifyCommandExecution()`
- **Security Alerts**: Triggered by `NotificationManager.notifySecurityAlert()`

### Manual Notification Testing
Test notifications through commands:

```bash
/notifications test                  # Send test to all channels
/notifications test log              # Send test to log channel only
/notifications send PLAYER_JOIN "Test message"  # Send custom notification
```

## 🎬 Event Actions

### Notification Actions
When events are triggered, they create NotificationEvent objects that are sent through configured channels:

#### Notification Channels
- **Log Channel** - Server log output
- **Console Channel** - Server console output
- **Custom Channels** - Extensible through NotificationChannel interface

#### Notification Data
Each notification contains:
- **Type** - Event classification (PLAYER_JOIN, COMMAND_EXECUTION, etc.)
- **Title** - Brief event description
- **Message** - Detailed event information
- **Player Name** - Associated player (if applicable)
- **Timestamp** - Event occurrence time
- **Severity** - Event importance (INFO, WARNING, CRITICAL)
- **Metadata** - Additional event data

## 📊 Event Monitoring

### Command-Based Monitoring
Monitor the notification system through commands:

```bash
/notifications status    # Show system status and enabled events
/notifications channels  # List available notification channels
```

### Event Logging
Events are automatically logged through the configured notification channels. The log channel writes events to the server log files.

## 🔌 Integration Features

### Notification Channel Integration
The system supports custom notification channels through the NotificationChannel interface:

```java
public interface NotificationChannel {
    void sendNotification(NotificationEvent event) throws Exception;
    boolean isEnabled();
    boolean supportsEventType(NotificationEvent.Type eventType);
    String getChannelName();
}
```

### API Integration
External systems can integrate with the notification system:

```java
// Get notification manager instance
NotificationManager manager = NotificationManager.getInstance();

// Send custom notifications
NotificationEvent event = NotificationEvent.builder()
    .type(NotificationEvent.Type.CUSTOM)
    .title("Custom Event")
    .message("Your custom message")
    .severity(NotificationEvent.Severity.INFO)
    .build();
    
manager.sendNotification(event);
```

## 🎮 Event-Driven Features

### Player Tracking
The system automatically tracks:
- Player join/leave events for connection monitoring
- Permission changes through PermissionEventListener
- Command execution for auditing purposes

### Server Monitoring
Automatic monitoring includes:
- Server startup and shutdown events
- Performance alerts when thresholds are exceeded
- Security alerts for violations

### Administrative Features
- Command execution logging
- Security violation tracking
- System performance monitoring

## 🔧 Advanced Event Features

### Notification Event Builder
Create custom notifications using the builder pattern:

```java
NotificationEvent event = NotificationEvent.builder()
    .type(NotificationEvent.Type.SECURITY_ALERT)
    .title("Security Alert")
    .message("Unauthorized access attempt")
    .playerName(playerName)
    .timestamp(System.currentTimeMillis())
    .severity(NotificationEvent.Severity.CRITICAL)
    .metadata("ip", ipAddress)
    .metadata("action", "login_attempt")
    .build();
```

### Channel Management
Manage notification channels programmatically:

```java
// Get all channels
Map<String, NotificationChannel> channels = manager.getChannels();

// Check channel status
boolean enabled = channel.isEnabled();
boolean supports = channel.supportsEventType(NotificationEvent.Type.PLAYER_JOIN);
```

## 🛠️ Debugging Events

### Command-Based Debugging
Debug the notification system:

```bash
/notifications status        # Check system status
/notifications test         # Test all channels
/notifications test log     # Test specific channel
```

### Log-Based Debugging
Event processing is logged through the server logging system. Check server logs for:
- Event listener registration messages
- Notification delivery status
- Channel availability issues
- Event processing errors

## 🔒 Event Security

### Permission Requirements
The `/notifications` command requires appropriate permissions:
- `neoessentials.notifications.admin` - Full notification management
- `neoessentials.notifications.test` - Testing capabilities
- `neoessentials.notifications.send` - Send custom notifications

### Security Considerations
- **Input Validation** - All notification data is validated
- **Permission Checks** - Commands require proper permissions
- **Rate Limiting** - Built into notification channels
- **Audit Logging** - All events are logged for security tracking

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - System configuration
- **[Essential Commands](Essential-Commands.md)** - Command reference
- **[Notifications](Notifications.md)** - Notification system details
- **[API Reference](API.md)** - API documentation

*Last Updated: December 18, 2024*
