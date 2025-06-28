# Staff Management

This guide covers how to effectively manage your server staff team using NeoEssentials' staff management features.

## Staff Hierarchy

NeoEssentials supports a flexible staff hierarchy system that works with permission plugins like LuckPerms. Here's a recommended structure:

```
Owner
 ├── Admin
 │    ├── Senior Moderator
 │    │    ├── Moderator
 │    │    │    ├── Helper
 │    │    │    │    └── Trial Helper
```

## Setting Up Staff Ranks

### Using LuckPerms

NeoEssentials integrates with LuckPerms for permission management:

1. Install LuckPerms alongside NeoEssentials
2. Create staff groups with appropriate permissions

Example commands:

```
/lp creategroup owner
/lp creategroup admin
/lp creategroup srmod
/lp creategroup mod
/lp creategroup helper
/lp creategroup trial

/lp group admin parent set owner
/lp group srmod parent set admin
/lp group mod parent set srmod
/lp group helper parent set mod
/lp group trial parent set helper
```

### Staff Permissions

Assign these permission patterns:

```
/lp group owner permission set neoessentials.admin.*
/lp group admin permission set neoessentials.admin.*
/lp group srmod permission set neoessentials.mod.*
/lp group mod permission set neoessentials.mod.*
/lp group helper permission set neoessentials.helper.*
/lp group trial permission set neoessentials.trial.*
```

## Staff Commands

NeoEssentials provides several commands for staff management:

### Basic Staff Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/staff` | `neoessentials.command.staff` | Displays online staff members |
| `/staffchat` | `neoessentials.command.staffchat` | Toggles staff chat mode |
| `/sc <message>` | `neoessentials.command.staffchat` | Sends a message to staff chat |
| `/vanish` | `neoessentials.command.vanish` | Toggles invisibility to regular players |
| `/freeze <player>` | `neoessentials.command.freeze` | Freezes a player in place |
| `/inspect <player>` | `neoessentials.command.inspect` | Shows information about a player |

### Advanced Moderation Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/warn <player> <reason>` | `neoessentials.command.warn` | Issues a warning to a player |
| `/history <player>` | `neoessentials.command.history` | Shows a player's moderation history |
| `/kick <player> <reason>` | `neoessentials.command.kick` | Kicks a player from the server |
| `/ban <player> <duration> <reason>` | `neoessentials.command.ban` | Bans a player from the server |
| `/mute <player> <duration> <reason>` | `neoessentials.command.mute` | Prevents a player from chatting |
| `/stafftp <player>` | `neoessentials.command.stafftp` | Teleports to a player without notification |

## Staff Management Tools

### Staff Mode

Staff mode provides special capabilities for monitoring and moderating players:

```
/staffmode
```

Features in staff mode:
- Invisibility to regular players
- Ability to see other vanished staff
- Flight capabilities 
- Special item tools for moderation
- Inspection abilities
- Cannot break/place blocks

### Staff Chat

Staff chat allows private communication between staff members:

```
/staffchat
```

Configuration in `config/neoessentials/chat.toml`:

```toml
[staffChat]
enabled = true
prefix = "&8[&cStaff&8] &r"
format = "{prefix}{displayname}&8: &7{message}"
notificationSound = "minecraft:entity.experience_orb.pickup"
```

## Staff Activity Monitoring

NeoEssentials can track staff activity to help with team management:

### Activity Logging

Enable in `config/neoessentials/staff.toml`:

```toml
[staffActivity]
enabled = true
logCommands = true
logLogin = true
logLogout = true
logModActions = true
```

### Staff Reports

Generate staff activity reports:

```
/staffreport <player> <timeframe>
/staffreport all <timeframe>
```

## Staff Notification System

Configure which events notify staff:

```toml
[staffNotifications]
playerJoin = true
playerQuit = true
playerReports = true
serverWarnings = true
notifyMethod = "chat"  # Options: chat, actionbar, title
```

## Staff Training Tools

### Training Mode

For new staff members:

```
/stafftraining <player>
```

When in training mode:
- All commands are logged but don't take effect
- Senior staff can monitor actions
- A confirmation prompt appears before actions

### Permission Templates

Set up templates for different staff ranks:

```
/templateperms create helper
/templateperms add helper neoessentials.command.tp
/templateperms add helper neoessentials.command.staffchat
```

Apply templates to players:

```
/templateperms apply helper NewStaffMember
```

## Staff Application System

NeoEssentials provides an in-game staff application system:

### Setting Up Applications

Configure in `config/neoessentials/staff.toml`:

```toml
[staffApplications]
enabled = true
cooldownDays = 14
minimumPlaytime = "24h"
questions = [
  "How old are you?",
  "How long have you played on the server?",
  "Why do you want to be staff?",
  "What would you do if you saw someone breaking rules?"
]
```

### Application Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/apply` | `neoessentials.command.apply` | Starts the application process |
| `/applications` | `neoessentials.command.applications` | Lists pending applications |
| `/reviewapp <id>` | `neoessentials.command.reviewapp` | Reviews an application |

## Staff Meetings

Schedule and manage staff meetings:

```
/staffmeeting schedule "Server Update Discussion" 2025-07-15T19:00:00
/staffmeeting invite @staff
/staffmeeting start
/staffmeeting notes "Discussed new rules and upcoming events"
```

## Audit Logging

NeoEssentials maintains detailed logs of staff actions:

### Log Configuration

```toml
[staffLogs]
enabled = true
logToFile = true
logToDatabase = true
retentionDays = 30
detailedCommands = true
```

### Viewing Logs

```
/stafflogs <player> <timeframe>
/stafflogs <action> <timeframe>
/stafflogs recent
```

## Best Practices

1. **Clear Guidelines**: Establish clear staff guidelines and rules
2. **Regular Training**: Hold regular staff training sessions
3. **Activity Requirements**: Set minimum activity requirements
4. **Performance Reviews**: Conduct periodic performance reviews
5. **Communication Channels**: Maintain open communication channels
6. **Accountability**: Hold staff accountable for their actions
7. **Reward System**: Recognize and reward good performance

## Additional Resources

- [Staff Training Manual Template](Staff-Training-Manual)
- [Common Moderation Scenarios](Moderation-Scenarios)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for staff management support
- [GitHub Repository](https://github.com/ZeroG-Network/NeoEssentials) for latest updates
