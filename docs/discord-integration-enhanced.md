# NeoEssentials Enhanced Discord Integration

## Overview
Enhanced SimpleDiscordLink integration for NeoEssentials providing seamless user experience with unified messaging, notification systems, and comprehensive permissions/roles synchronization.

## Features Implemented

### 1. Enhanced SimpleDiscordLink Integration
- **File**: `SimpleDiscordLinkIntegration.java`
- **Features**:
  - Advanced message formatting with rich embeds
  - Role synchronization between Discord and Minecraft
  - Enhanced chat synchronization with team/rank information
  - Real-time notifications for various events
  - Player linking system with Discord user data
  - Comprehensive error handling and fallback systems

### 2. Discord Integration Manager
- **File**: `DiscordIntegrationManager.java`
- **Features**:
  - Rich Discord embeds with player stats, team info, and rank information
  - Periodic status updates with server statistics
  - Automated role synchronization with configurable intervals
  - Achievement and advancement notifications
  - Comprehensive notification system for all NeoEssentials events

### 3. Discord Placeholder Provider
- **File**: `DiscordPlaceholderProvider.java`
- **Features**:
  - 30+ Discord-specific placeholders for server statistics
  - Real-time server performance monitoring
  - Player statistics and team/rank information
  - Memory usage and server health monitoring
  - Discord-specific formatting and avatar integration

### 4. Unified Configuration System
- **File**: `tablist.json` (Enhanced with Discord section)
- **Features**:
  - Complete Discord integration configuration
  - Role mapping with Minecraft permissions
  - Notification settings for all event types
  - Rich embed styling and webhook configuration
  - Error handling and retry mechanisms

## Configuration Structure

### Discord Integration Section (`tablist.json`)
```json
{
  "discordIntegration": {
    "enabled": true,
    "messageFormatting": {
      "useEmbeds": true,
      "timestampFormat": "yyyy-MM-dd HH:mm:ss",
      "includePlayerStats": true,
      "includeTeamInfo": true,
      "includeRankInfo": true
    },
    "notifications": {
      "tablistUpdates": {
        "enabled": true,
        "channel": "general",
        "format": "🔄 **{player_name}** | Tablist updated | Team: **{ftb_team_display_name}** | Rank: **{ftb_rank_display_name}**"
      },
      "scoreboardUpdates": {
        "enabled": true,
        "channel": "general",
        "format": "📊 **{player_name}** | Scoreboard updated | Layout: **{layout_name}**"
      }
    },
    "roleSync": {
      "enabled": true,
      "syncOnJoin": true,
      "syncInterval": 300,
      "bidirectional": true,
      "roleMappings": {
        "Owner": {
          "minecraftPermission": "neoessentials.admin",
          "priority": 1000,
          "tablistPrefix": "&4[OWNER]&r",
          "scoreboardTitle": "&4&lOWNER"
        }
      }
    }
  }
}
```

## Enhanced Features

### 1. Rich Discord Embeds
- **Color-coded notifications** based on event type
- **Player statistics** including health, level, ping
- **Team and rank information** from FTB integration
- **Server performance metrics** in status updates
- **Custom thumbnails** using player avatars

### 2. Role Synchronization
- **Bidirectional sync** between Discord roles and Minecraft permissions
- **Priority-based system** for role conflicts
- **Automatic sync on join** and periodic intervals
- **Tablist and scoreboard integration** with role-based prefixes
- **Fallback role system** for unverified users

### 3. Advanced Notifications
- **Tablist updates** with layout and team information
- **Scoreboard changes** with animation status
- **Player join/leave** with session duration and stats
- **Team and rank updates** with before/after information
- **Permission changes** with admin tracking
- **Achievement notifications** with team context

### 4. Server Status Integration
- **Real-time TPS monitoring** with performance metrics
- **Memory usage tracking** (used/max/free)
- **Player statistics** (online count, top players)
- **Team and rank analytics** (total teams, top team)
- **NeoEssentials status** (active layouts, linked players)

## Discord Placeholders

### Server Statistics
- `{server_status}` - Online/Offline status
- `{server_tps}` - Current TPS
- `{server_uptime}` - Server uptime in hours/minutes
- `{used_memory}` - Used RAM in MB
- `{max_memory}` - Maximum RAM in MB
- `{free_memory}` - Free RAM in MB

### Player Information
- `{players_online}` - Current online players
- `{max_players}` - Maximum player limit
- `{top_player_name}` - Highest level player
- `{top_player_level}` - Highest player level
- `{discord_linked_players}` - Players linked to Discord

### Team and Rank Data
- `{total_teams}` - Total FTB teams
- `{top_team_name}` - Team with most members
- `{top_team_members}` - Member count of top team
- `{active_ranks}` - Number of active ranks

### Performance Metrics
- `{tick_time}` - Average tick time in ms
- `{loaded_chunks}` - Total loaded chunks
- `{loaded_entities}` - Total loaded entities

### Discord-Specific
- `{discord_player_name}` - Discord name + Minecraft name
- `{discord_player_avatar}` - Player avatar URL
- `{discord_player_roles}` - Discord role status
- `{discord_integration_status}` - Integration status

## Event Integration

### TabList Manager Integration
- **Enhanced updatePlayerEntry()** with Discord notifications
- **Automatic role prefix application** based on Discord roles
- **Real-time tablist updates** sent to Discord channels
- **Team and rank change notifications**

### Scoreboard Manager Integration
- **Layout change notifications** with animation status
- **Performance metric integration** in Discord embeds
- **Conditional display updates** based on Discord roles
- **Rich scoreboard status** in Discord channels

## Error Handling and Reliability

### Comprehensive Error Handling
- **Retry mechanisms** for failed Discord messages (3 attempts)
- **Fallback to basic messages** if embeds fail
- **Graceful degradation** when SimpleDiscordLink is unavailable
- **Detailed logging** for troubleshooting

### Performance Optimization
- **Cached player data** for efficient Discord integration
- **Scheduled tasks** for periodic updates
- **Non-blocking operations** to prevent server lag
- **Efficient placeholder processing** with minimal overhead

## Installation and Setup

### Prerequisites
- NeoEssentials mod installed
- SimpleDiscordLink 3.x+ installed and configured
- FTB Teams and FTB Ranks (optional, for enhanced team/rank integration)

### Configuration Steps
1. **Update tablist.json** with Discord integration settings
2. **Configure role mappings** between Discord and Minecraft
3. **Set up notification channels** in Discord
4. **Enable webhooks** for enhanced message formatting
5. **Test integration** with `/neoessentials discord test` command

### Verification
- Check console logs for "Discord integration enabled" message
- Verify role synchronization on player join
- Test notifications by updating tablist/scoreboard
- Monitor Discord channels for rich embed messages

## Performance Impact

### Minimal Server Impact
- **Asynchronous processing** for all Discord operations
- **Efficient caching** for player and role data
- **Optimized placeholder processing** with lazy evaluation
- **Configurable update intervals** to balance performance

### Network Efficiency
- **Batched updates** to reduce API calls
- **Intelligent rate limiting** to prevent Discord API limits
- **Compressed message formats** for large notifications
- **Error recovery** without service interruption

## Future Enhancements

### Planned Features
- **Discord slash commands** for NeoEssentials management
- **Voice channel integration** for team communication
- **Advanced statistics dashboard** with interactive embeds
- **Custom Discord bot** for enhanced features
- **Integration with other mods** (Create, Applied Energistics, etc.)

### Community Features
- **Player voting systems** via Discord reactions
- **Event scheduling** with calendar integration
- **Achievement leaderboards** in Discord channels
- **Team challenges** with Discord coordination
- **Community polls** for server decisions

## Technical Documentation

### API Integration Points
- **SimpleDiscordLink reflection API** for message sending
- **FTB Teams API** for team information
- **FTB Ranks API** for rank data
- **NeoEssentials event system** for real-time updates
- **Minecraft server API** for performance metrics

### Extension Points
- **Custom placeholder providers** for mod-specific data
- **Event listener registration** for additional notifications
- **Role mapping extensions** for complex permission systems
- **Webhook customization** for specialized formatting
- **Performance monitoring** with custom metrics

This enhanced Discord integration provides a seamless, feature-rich experience that bridges Minecraft and Discord communities while maintaining excellent performance and reliability.
