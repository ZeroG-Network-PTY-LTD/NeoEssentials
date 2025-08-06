# Events System

NeoEssentials provides a comprehensive event system that allows for advanced automation, custom behaviors, and integration with other systems. This event-driven architecture enables sophisticated server management through customizable triggers and responses.

## 🎯 Event Architecture

### Event Types
NeoEssentials categorizes events into several major types:

#### Player Events
Events related to player actions and status changes:
- **Player Join/Leave** - Connection events
- **Player Death/Respawn** - Life cycle events  
- **Player Chat** - Communication events
- **Player Movement** - Location and dimension changes
- **Player Interaction** - Block/entity interactions
- **Player Commands** - Command execution events

#### Server Events
Events related to server status and administration:
- **Server Start/Stop** - Lifecycle events
- **Server Performance** - TPS, memory, CPU events
- **Configuration Changes** - Config reload events
- **Plugin Load/Unload** - Module management events

#### Economy Events
Events related to economic transactions:
- **Money Transfer** - Player-to-player payments
- **Shop Transactions** - Buy/sell operations
- **Balance Changes** - Any balance modifications
- **Banking Operations** - Deposits, withdrawals, interest

#### Security Events
Events related to server security:
- **Threat Detection** - Security violations
- **IP Events** - Connection attempts, bans
- **Permission Changes** - Role/permission updates
- **Admin Actions** - Administrative interventions

## 📋 Event Configuration

### Event Registry
Configure events in `config/neoessentials/events.toml`:

```toml
[events]
# Enable event system
enabled = true

# Maximum events processed per tick
maxEventsPerTick = 50

# Event history length
historyLength = 1000

[events.player]
# Player join events
join.enabled = true
join.priority = "NORMAL"
join.async = false

# Player leave events
leave.enabled = true
leave.priority = "NORMAL"
leave.async = true

# Player death events
death.enabled = true
death.priority = "HIGH"
death.async = false

[events.server]
# Performance monitoring events
performance.enabled = true
performance.interval = 60  # seconds
performance.threshold = 15.0  # TPS threshold

# Configuration change events
config.enabled = true
config.watchFiles = true
config.reloadOnChange = false
```

### Event Listeners
Register custom event listeners:

```toml
[events.listeners]
# Welcome system
welcome_new_player = {
  event = "player_join",
  condition = "first_time == true",
  actions = ["send_welcome_message", "give_starter_kit", "teleport_to_spawn"]
}

# Anti-grief system
block_protection = {
  event = "block_break",
  condition = "in_protected_area == true && has_permission == false",
  actions = ["cancel_event", "notify_admins", "log_attempt"]
}

# Economy milestone
wealth_milestone = {
  event = "balance_change", 
  condition = "new_balance >= 100000 && previous_balance < 100000",
  actions = ["broadcast_achievement", "give_title", "play_sound"]
}
```

## 🔧 Event Triggers

### Manual Event Triggering
Manually trigger events for testing or special circumstances:

```bash
/event trigger <event> [data]         # Trigger specific event
/event test <listener> [player]       # Test event listener
/event simulate <scenario>            # Simulate event scenario
/event broadcast <event> [data]       # Broadcast custom event
```

**Examples:**
```bash
/event trigger player_join PlayerName
/event test welcome_new_player TestPlayer
/event simulate server_lag
/event broadcast custom_event {"message": "Special event!"}
```

### Scheduled Events
Create scheduled events for regular server tasks:

```bash
/event schedule <time> <event> [data]    # Schedule one-time event
/event repeat <interval> <event> [data]  # Schedule repeating event
/event cron <expression> <event> [data]  # Schedule with cron expression
```

**Examples:**
```bash
/event schedule "30m" server_announcement {"text": "Server restart in 30 minutes"}
/event repeat "1h" cleanup_entities
/event cron "0 0 * * *" daily_backup  # Daily at midnight
```

### Conditional Events
Events with complex conditions:

```yaml
conditional_events:
  weekend_bonus:
    trigger: "player_join"
    condition: |
      day_of_week in ['saturday', 'sunday'] and 
      player.playtime > 10 and
      not player.claimed_weekend_bonus_today
    actions:
      - give_money: 1000
      - send_message: "Weekend bonus! You received $1000!"
      - set_flag: "claimed_weekend_bonus_today"
```

## 🎬 Event Actions

### Built-in Actions
NeoEssentials provides numerous built-in actions:

#### Communication Actions
```yaml
actions:
  send_message:
    type: "chat"
    message: "Welcome {player}!"
    color: "green"
    
  send_title:
    type: "title" 
    title: "Achievement Unlocked!"
    subtitle: "First Login"
    
  broadcast:
    type: "broadcast"
    message: "{player} reached level 100!"
    
  play_sound:
    type: "sound"
    sound: "entity.player.levelup"
    volume: 1.0
```

#### Economy Actions
```yaml
actions:
  give_money:
    type: "economy"
    action: "give"
    amount: 500
    
  take_money:
    type: "economy"
    action: "take"
    amount: 100
    reason: "Tax"
    
  set_balance:
    type: "economy"
    action: "set"
    amount: 10000
```

#### Teleportation Actions
```yaml
actions:
  teleport_spawn:
    type: "teleport"
    location: "spawn"
    
  teleport_coordinates:
    type: "teleport"
    x: 100
    y: 64
    z: 200
    world: "world"
    
  teleport_player:
    type: "teleport"
    target: "other_player"
```

#### Permission Actions
```yaml
actions:
  add_permission:
    type: "permission"
    action: "add"
    permission: "essentials.fly"
    
  remove_permission:
    type: "permission"
    action: "remove"
    permission: "essentials.god"
    
  set_group:
    type: "permission"
    action: "set_group"
    group: "vip"
```

### Custom Actions
Create custom actions using scripts:

```yaml
custom_actions:
  advanced_welcome:
    type: "script"
    language: "javascript"
    script: |
      // Get player data
      var player = event.getPlayer();
      var playtime = player.getPlaytime();
      
      // Custom welcome based on playtime
      if (playtime == 0) {
        player.sendMessage("§6Welcome new player!");
        player.giveKit("starter");
      } else {
        player.sendMessage("§aWelcome back! Playtime: " + formatTime(playtime));
      }
      
      // Update statistics
      updatePlayerStats(player, "logins", 1);
```

## 📊 Event Monitoring

### Event Dashboard
Monitor events in real-time:

```bash
/event dashboard                 # Open event monitoring dashboard
/event stats                     # Event statistics overview
/event history [count]           # Recent event history
/event performance               # Event system performance
```

**Dashboard Features:**
- **Real-time Event Stream** - Live view of triggered events
- **Event Frequency** - Events per minute/hour statistics
- **Action Success Rate** - Percentage of successful actions
- **Performance Metrics** - Event processing times
- **Error Tracking** - Failed events and error messages

### Event Logging
Comprehensive event logging system:

```toml
[events.logging]
# Enable event logging
enabled = true

# Log file location
logFile = "neoessentials/events.log"

# Log level (DEBUG, INFO, WARN, ERROR)
logLevel = "INFO"

# Rotate log files
rotateFiles = true
maxFileSize = "10MB"
maxFiles = 5

# Log specific event types
logPlayerEvents = true
logServerEvents = true
logSecurityEvents = true
logEconomyEvents = false
```

### Event Analytics
Analyze event patterns and trends:

```bash
/event analytics <timeframe>     # Event analytics report
/event trends                    # Event trend analysis
/event correlations              # Event correlation analysis
/event export <format>           # Export event data
```

## 🔌 Integration Features

### Plugin Integration
Events can integrate with other plugins:

```yaml
integrations:
  discord_bot:
    enabled: true
    webhook_url: "your_discord_webhook"
    events:
      - player_join
      - player_achievement
      - server_lag
      
  database_logger:
    enabled: true
    connection: "mysql://localhost:3306/events"
    tables:
      player_events: "player_events"
      server_events: "server_events"
```

### API Integration
External systems can subscribe to events:

```yaml
api_endpoints:
  webhook_notifications:
    url: "https://yourserver.com/webhook"
    events: ["security_threat", "server_error"]
    authentication: "bearer_token"
    
  rest_api:
    enabled: true
    port: 8080
    events: ["player_join", "player_leave"]
```

## 🎮 Event-Driven Features

### Dynamic Responses
Create sophisticated server behaviors:

```yaml
dynamic_responses:
  smart_difficulty:
    trigger: "player_count_change"
    condition: "player_count > 20"
    actions:
      - set_difficulty: "hard"
      - spawn_rate_multiplier: 1.5
      - broadcast: "Difficulty increased due to high player count!"
      
  resource_management:
    trigger: "server_lag"
    condition: "tps < 15"
    actions:
      - reduce_entity_spawn: 0.5
      - cleanup_items: true
      - notify_admins: "Server experiencing lag, reducing entity spawns"
```

### Automated Moderation
Event-driven moderation system:

```yaml
auto_moderation:
  rapid_commands:
    trigger: "command_execute"
    condition: "commands_per_minute > 30"
    actions:
      - temp_mute: "1m"
      - notify_staff: "{player} is sending commands rapidly"
      - log_security_event: "rapid_commands"
      
  grief_detection:
    trigger: "block_break"
    condition: "blocks_broken_per_minute > 100 && in_protected_area"
    actions:
      - freeze_player: true
      - alert_admins: "Possible griefing detected: {player}"
      - rollback_recent_changes: true
```

## 🔧 Advanced Event Features

### Event Chains
Create complex event sequences:

```yaml
event_chains:
  player_progression:
    start_event: "player_join"
    chain:
      - wait: "5m"
      - check_condition: "player.online && player.location == spawn"
      - action: "send_message: 'Ready for a tour?'"
      - wait_for_event: "player_chat"
      - condition: "message.toLowerCase().contains('yes')"
      - action: "start_tour"
```

### Event Queuing
Manage event processing with queues:

```toml
[events.queuing]
# Enable event queuing
enabled = true

# Queue size limits
maxQueueSize = 10000

# Processing priorities
priorities = {
  "URGENT" = 1000,
  "HIGH" = 100, 
  "NORMAL" = 10,
  "LOW" = 1
}

# Queue processing strategy
strategy = "priority_first"  # or "fifo", "lifo"
```

### Event Persistence
Persist events across server restarts:

```toml
[events.persistence]
# Save events to disk
enabled = true

# Events to persist
persistEvents = ["player_achievement", "economy_milestone", "security_threat"]

# Storage format
format = "json"  # or "yaml", "binary"

# Cleanup old events
cleanupAfter = "30d"
```

## 🛠️ Debugging Events

### Event Debugging Tools
Debug event issues:

```bash
/event debug <event>             # Debug specific event type
/event trace <player>            # Trace events for player
/event verbose <true/false>      # Toggle verbose logging
/event validate <config>         # Validate event configuration
```

### Testing Framework
Test events thoroughly:

```bash
/event test create <name>        # Create test scenario
/event test run <scenario>       # Run test scenario
/event test suite               # Run full test suite
/event test report              # Generate test report
```

## 🔒 Event Security

### Event Permissions
Control event access with permissions:

```yaml
permissions:
  neoessentials.events.trigger     # Manually trigger events
  neoessentials.events.admin       # Event administration
  neoessentials.events.debug       # Event debugging
  neoessentials.events.config      # Event configuration
```

### Security Considerations
- **Input Validation** - Validate all event data
- **Permission Checks** - Verify permissions before actions
- **Rate Limiting** - Prevent event spam
- **Audit Logging** - Log all security-related events

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - Event system configuration
- **[Security Features](Security.md)** - Security event integration
- **[Notifications](Notifications.md)** - Event-driven notifications
- **[API Reference](API.md)** - Event API documentation

*Last Updated: August 6, 2025*
