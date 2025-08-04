# Enhanced Discord Integration - NeoEssentials

## Overview
The Enhanced Discord Integration extends NeoEssentials' basic webhook functionality with rich embeds, automated reporting, and advanced notification features. This system provides professional-grade Discord integration suitable for modern Minecraft servers.

## Features

### 🎨 Rich Embed System
- **Professional Formatting**: Rich embeds with custom colors, titles, descriptions, and fields
- **Visual Elements**: Thumbnails, author icons, timestamps, and footers
- **Color Coding**: Automatic color assignment based on message type and severity
- **Field Organization**: Structured information display with inline and block fields

### 📊 Automated Reporting
- **Player Statistics**: Comprehensive player data reports with playtime, levels, and location
- **Economy Reports**: Server economy health, circulation statistics, and top players
- **Server Status**: Real-time server status updates with performance metrics
- **Moderation Alerts**: Instant notifications for bans, kicks, mutes, and warnings

### 🔧 Admin Tools
- **Custom Embeds**: Send custom rich embeds directly from game commands
- **Status Monitoring**: Real-time integration status and configuration validation
- **Test Functions**: Built-in testing tools to verify webhook functionality
- **Enhanced Commands**: Extended command set for Discord management

## Commands

### Enhanced Discord Commands (`/discordenhanced`)
All commands require admin permission (`neoessentials.discord.admin`):

#### `/discordenhanced status`
Display comprehensive integration status including:
- Integration enabled/disabled status
- Webhook configuration validation
- Available enhanced features list
- Connection health information

#### `/discordenhanced test`
Send a test rich embed to verify enhanced integration functionality.

#### `/discordenhanced playerstats <player>`
Send detailed player statistics to Discord including:
- Player name and avatar
- Play time and experience level
- Current health and position
- Dimension information

#### `/discordenhanced economyreport`
Generate and send a comprehensive economy report with:
- Total money in circulation
- Average player balance
- Richest player information
- Recent transaction statistics
- Economy health assessment

#### `/discordenhanced serverstatus <status>`
Send server status update with:
- Current server status (starting/online/stopping/offline)
- Player count and capacity
- Memory usage statistics
- Server version and uptime

#### `/discordenhanced embed <title>`
Send a custom rich embed with specified title and automatic formatting.

## Integration Methods

### Automatic Notifications
The enhanced system automatically sends notifications for:

#### Player Events
- **Join/Leave**: Player connection status with timestamps
- **Achievements**: Player milestone notifications with descriptions
- **Statistics**: Periodic player performance updates

#### Server Events
- **Status Changes**: Server start/stop notifications
- **Performance**: Memory usage and lag alerts
- **Updates**: Maintenance and configuration changes

#### Moderation Actions
- **Bans/Unbans**: Permanent and temporary ban notifications
- **Kicks**: Player removal notifications with reasons
- **Mutes**: Chat restriction notifications
- **Warnings**: Player warning alerts

### Manual Reporting
Admins can trigger reports on-demand:

#### Economy Analytics
```java
DiscordEnhancedIntegration.sendEconomyReport(economyData);
```

#### Player Statistics
```java
DiscordEnhancedIntegration.sendPlayerStats(player);
```

#### Custom Notifications
```java
EmbedBuilder embed = new EmbedBuilder()
    .setTitle("Custom Title")
    .setDescription("Custom description")
    .setColor(Color.BLUE)
    .addField("Field Name", "Field Value", true);
    
DiscordEnhancedIntegration.sendCustomNotification(source, embed);
```

## Configuration

### Webhook Setup
1. **Create Discord Webhook**: Create a webhook in your Discord server
2. **Configure NeoEssentials**: Set webhook URL in DiscordManager
3. **Enable Integration**: Activate Discord features in configuration
4. **Test Connection**: Use `/discordenhanced test` to verify setup

### Security Considerations
- **Webhook Protection**: Store webhook URLs securely
- **Permission Control**: Restrict admin commands to authorized users
- **Rate Limiting**: Built-in protection against message spam
- **Error Handling**: Graceful failure management for network issues

## Embed Builder API

### Basic Usage
```java
EmbedBuilder embed = new EmbedBuilder()
    .setTitle("Embed Title")
    .setDescription("Embed description")
    .setColor(new Color(52, 152, 219)) // Blue
    .addField("Field 1", "Value 1", true)
    .addField("Field 2", "Value 2", false)
    .setFooter("Footer text", null)
    .setTimestamp(Instant.now());
```

### Advanced Features
```java
EmbedBuilder embed = new EmbedBuilder()
    .setAuthor("Author Name", "https://example.com", "icon_url")
    .setThumbnail("https://mc-heads.net/avatar/player")
    .setColor(0x3498DB) // Hex color
    .addField("Inline Field", "Value", true)
    .addField("Block Field", "Longer value that spans full width", false);
```

## Error Handling

### Network Resilience
- **Connection Timeouts**: 10-second timeout for webhook requests
- **Retry Logic**: Automatic retry for temporary failures
- **Fallback Handling**: Graceful degradation when Discord is unavailable
- **Error Logging**: Comprehensive logging for troubleshooting

### Validation
- **Webhook URL**: Automatic validation of Discord webhook format
- **Message Limits**: Respect Discord's character and embed limits
- **Rate Limiting**: Built-in protection against API rate limits
- **Input Sanitization**: Safe handling of user input in embeds

## Performance

### Optimization Features
- **Async Operations**: Non-blocking webhook requests
- **Connection Pooling**: Efficient HTTP client management
- **Message Queuing**: Optional queuing for high-volume servers
- **Resource Management**: Proper cleanup and memory management

### Monitoring
- **Status Tracking**: Real-time integration health monitoring
- **Performance Metrics**: Request success rates and response times
- **Error Analytics**: Detailed error reporting and classification
- **Usage Statistics**: Message volume and feature usage tracking

## Integration Examples

### Moderation Integration
```java
// In ban command
DiscordEnhancedIntegration.sendModerationAction(
    "ban", 
    moderator.getName().getString(), 
    target.getName().getString(), 
    reason
);
```

### Economy Integration
```java
// In economy commands
Map<String, Object> economyData = generateEconomyReport();
DiscordEnhancedIntegration.sendEconomyReport(economyData);
```

### Server Monitoring
```java
// In server lifecycle events
Map<String, Object> serverInfo = getServerInfo();
DiscordEnhancedIntegration.sendServerStatus("starting", serverInfo);
```

## Future Enhancements

### Planned Features
- **Two-way Communication**: Execute server commands from Discord
- **Player Linking**: Link Discord accounts to Minecraft players
- **Voice Integration**: Voice channel management and notifications
- **Advanced Analytics**: Detailed server analytics and reporting

### Extensibility
- **Plugin API**: Extensible embed system for custom integrations
- **Event Hooks**: Customizable event triggers and filters
- **Template System**: Reusable embed templates for common notifications
- **Multi-Server**: Support for multiple server instances

## Troubleshooting

### Common Issues

#### Webhook Not Working
1. Verify webhook URL format (`https://discord.com/api/webhooks/...`)
2. Check Discord server permissions
3. Test with `/discordenhanced test` command
4. Review error logs for network issues

#### Missing Notifications
1. Confirm integration is enabled in configuration
2. Check player permissions for notification triggers
3. Verify webhook URL hasn't changed or expired
4. Test individual notification types

#### Performance Issues
1. Monitor webhook response times
2. Check server network connectivity
3. Review Discord API rate limiting
4. Consider message queuing for high-volume servers

### Support
For additional support and configuration assistance, refer to the main NeoEssentials documentation or contact the development team.
