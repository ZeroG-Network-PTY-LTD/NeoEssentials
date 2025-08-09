# Security Features

NeoEssentials includes a basic security system that provides essential monitoring and protection features for your Minecraft server. The system offers IP address management, player activity tracking, and basic threat detection capabilities.

## 🔐 Overview

The security system provides:
- **IP address blocking and monitoring** with manual and automatic controls
- **Player security profiles** with login and activity tracking  
- **Basic event logging** with security event categorization
- **Command-based security management** for administrators
- **Integration with NeoEssentials permission system**

## 🛡️ Basic Security Features

### IP Address Management

The security system provides IP address blocking and monitoring:

#### IP Blocking
- **Manual IP blocking**: Block specific IP addresses with reasons
- **IP unblocking**: Remove blocks from IP addresses
- **Blocked IP tracking**: Monitor all currently blocked addresses
- **Block reason logging**: Track why each IP was blocked

#### IP Security Profiles
- **Connection tracking**: Monitor first and last seen times
- **Failed login attempts**: Track authentication failures
- **Geolocation data**: Basic location information when available
- **Suspicious activity flagging**: Mark IPs showing bot-like behavior

### Player Security Tracking

Basic player monitoring and profiling:

#### Player Security Profiles
- **Login activity**: Track login counts and timestamps
- **Command history**: Monitor recent command usage
- **Suspicion levels**: Basic 0-10 scoring system
- **Activity flags**: Mark players for review when needed
- **IP association**: Link players to their known IP addresses

#### Security Events
- **Event logging**: Track security-related activities
- **Event categorization**: Organize events by type and severity
- **Threat levels**: Classify events from NONE to CRITICAL
- **Historical tracking**: Maintain event history for analysis

## 📊 Security Commands

### Main Security Command

The `/security` command provides access to all security features:

#### `/security` or `/security status`
View overall security system status and statistics.

**Example Output**:
```
=== Security System Status ===
Total Events: 156
Blocked IPs: 3  
Monitored Players: 25
Monitored IPs: 18

Event Types:
- IP Blocked: 3
- Player Login: 142
- Suspicious Activity: 8
- Authentication Failure: 3
```

#### `/security events [limit]`
View recent security events with optional limit (default 10, max 100).

**Example**:
```bash
/security events         # Show last 10 events
/security events 25      # Show last 25 events
```

**Example Output**:
```
=== Recent Security Events (Last 10) ===
[08-09 14:30] IP_BLOCKED - IP blocked: 192.168.1.100 - Suspicious activity (SYSTEM)
[08-09 14:25] PLAYER_LOGIN - Player TestUser logged in (INFO)
[08-09 14:20] AUTHENTICATION_FAILURE - Failed login from 192.168.1.50 (WARNING)
```

#### `/security player <player>`
View detailed security profile for a specific player.

**Example**:
```bash
/security player TestUser
```

**Example Output**:
```
=== Security Profile: TestUser ===
Player ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
First Seen: 2025-08-01 10:30:15
Last Seen: 2025-08-09 14:25:42
Last Known IP: 192.168.1.25
Suspicion Level: 2/10
Login Attempts: 15
Command History: 234
Recent Commands: 12/min
```

#### `/security ip <ip>`
View security profile for a specific IP address.

**Example**:
```bash
/security ip 192.168.1.100
```

**Example Output**:
```
=== IP Security Profile: 192.168.1.100 ===
First Seen: 2025-08-05 16:20:10
Last Seen: 2025-08-09 14:30:00
Failed Login Attempts: 8
Recent Failures: 3/hour
Suspicion Level: 7/10
Blocked: Yes - Suspicious activity
Login Attempts: 12
Web Requests: 0
Bot-like Behavior: Yes
Location: United States (if available)
```

#### `/security block <ip> [reason]`
Block an IP address with optional reason.

**Examples**:
```bash
/security block 192.168.1.100
/security block 192.168.1.100 "Repeated failed login attempts"
```

#### `/security unblock <ip>`
Remove block from an IP address.

**Example**:
```bash
/security unblock 192.168.1.100
```

#### `/security scan`
Run a basic security analysis of current system status.

**Example Output**:
```
=== Security Scan Results ===
Total Security Events: 156
High-Threat Events (Recent): 2
Blocked IP Addresses: 3
⚠ High-threat events detected! Review with '/security events'
```

#### `/security report`
Generate a basic compliance report (creates JSON file).

**Output**:
```
Compliance report generated!
Report Location: neoessentials/security/compliance_report.json
Report includes: Event statistics, blocked IPs, monitoring data
```

## 🔧 Security Event Types

The system tracks various types of security events:

### Standard Event Types
- **IP_BLOCKED**: IP address was blocked
- **IP_UNBLOCKED**: IP address was unblocked
- **PLAYER_LOGIN**: Player successfully logged in
- **PLAYER_LOGOUT**: Player logged out
- **SUSPICIOUS_ACTIVITY**: Unusual behavior detected
- **AUTHENTICATION_FAILURE**: Failed login attempt
- **PERMISSION_VIOLATION**: Unauthorized command access
- **COMMAND_BLOCKED**: Command was blocked
- **RATE_LIMIT_EXCEEDED**: Command rate limit hit
- **SECURITY_SCAN**: Security scan performed
- **THREAT_DETECTED**: Threat identified
- **SYSTEM_EVENT**: System-related security event
- **COMMAND_EXECUTED**: Command execution logged
- **SYSTEM_STARTUP**: Security system started

### Threat Levels
Events are classified by threat level:
- **NONE**: No threat (informational)
- **LOW**: Minor concern
- **MEDIUM**: Moderate concern
- **HIGH**: Serious concern requiring attention
- **CRITICAL**: Major threat requiring immediate action
- **EXTREME**: Severe threat requiring emergency response

## 📋 Configuration

The security system integrates with NeoEssentials' standard configuration:

### Basic Security Settings

```toml
[security]
# Enable security system
enabled = true

# Security level (LOW, MEDIUM, HIGH, CRITICAL)
securityLevel = "MEDIUM"

# Enable IP monitoring
ipMonitoring = true

# Enable player behavior tracking
playerTracking = true

[security.thresholds]
# Failed login attempts before flagging
maxFailedLogins = 5

# Commands per minute before flagging
maxCommandsPerMinute = 60

# Chat messages per minute before flagging
maxChatPerMinute = 20

[security.responses]
# Notify staff of security events
notifyStaff = true

# Log security events to console
logToConsole = true

[security.whitelist]
# Whitelisted IP addresses (never flagged)
whitelistedIPs = ["127.0.0.1"]

# Whitelisted players (reduced monitoring)
whitelistedPlayers = []
```

## 🛠️ Administrative Features

### Permission Requirements

Security commands require appropriate permissions:

```yaml
# Basic security access
neoessentials.security.basic      # View security status
neoessentials.security.events     # View security events
neoessentials.security.profiles   # View player/IP profiles

# Administrative security access
neoessentials.security.block      # Block/unblock IP addresses
neoessentials.security.scan       # Run security scans
neoessentials.security.admin      # Full security management
```

### Integration with Other Systems

The security system works with other NeoEssentials features:

#### Permission System Integration
- **Permission violations**: Track unauthorized command attempts
- **Role-based access**: Different security levels for different permission groups
- **Staff notifications**: Alert administrators about security events

#### Command System Integration
- **Command logging**: Track all command executions
- **Rate monitoring**: Basic command frequency tracking
- **Violation logging**: Record blocked or suspicious commands

## 🔍 Monitoring and Analysis

### Basic Analysis Features

The system provides simple analysis tools:

#### Security Statistics
- **Event counts**: Total events by type and severity
- **IP statistics**: Blocked IPs and connection patterns
- **Player activity**: Login patterns and command usage
- **Trend tracking**: Basic increases or decreases in activity

#### Manual Investigation
- **Event review**: Examine recent security events
- **Profile analysis**: Review player and IP security profiles
- **Pattern identification**: Look for repeated behaviors manually
- **Cross-reference**: Compare events across different players/IPs

### Reporting Features

#### Compliance Reports
- **Event summaries**: Statistical overview of security events
- **Blocked IP lists**: Complete list of blocked addresses with reasons
- **Player profiles**: Summary of monitored players and their activity
- **System health**: Basic security system status information

#### Export Capabilities
- **JSON reports**: Machine-readable security data
- **Event logs**: Exportable event history
- **Profile data**: Player and IP profile information

## 🚨 Basic Response Procedures

### Manual Response Actions

When security events are detected, administrators can:

#### Immediate Actions
- **Block IP addresses**: Stop problematic connections
- **Review player profiles**: Examine suspicious activity
- **Check event logs**: Investigate security incidents
- **Generate reports**: Document security issues

#### Investigation Steps
1. **Review security events**: Use `/security events` to see recent activity
2. **Check player profiles**: Use `/security player <name>` for detailed info
3. **Examine IP patterns**: Use `/security ip <address>` for connection data
4. **Cross-reference data**: Look for patterns across multiple events
5. **Take action**: Block IPs or flag players as needed

### Best Practices

#### Monitoring Guidelines
- **Regular scans**: Run `/security scan` periodically
- **Event review**: Check `/security events` daily
- **Profile monitoring**: Review flagged players weekly
- **Report generation**: Create monthly compliance reports

#### Response Guidelines
- **Quick blocking**: Block obviously malicious IPs immediately
- **Evidence gathering**: Document reasons for security actions
- **Staff coordination**: Share security concerns with team members
- **Follow-up**: Monitor blocked IPs and flagged players over time

## 🔧 Troubleshooting

### Common Issues

#### Security System Not Working
**Problem**: Security commands don't work
**Solution**: 
1. Check that security system is enabled in configuration
2. Verify you have appropriate permissions
3. Restart server if configuration changes were made
4. Check console for security system startup messages

#### Events Not Being Logged
**Problem**: No security events appear
**Solution**:
1. Ensure `ipMonitoring` and `playerTracking` are enabled
2. Check that security level is set appropriately
3. Verify events are being generated (players logging in, commands being used)
4. Restart security system with server restart

#### Can't Block IP Addresses
**Problem**: IP blocking commands fail
**Solution**:
1. Check permissions: `neoessentials.security.block`
2. Verify IP address format is correct
3. Ensure IP isn't already blocked
4. Check console for error messages

### Debug Commands

For troubleshooting security issues:

```bash
# Check security system status
/security status

# View recent activity
/security events 50

# Check specific problems
/security scan

# Generate detailed report
/security report
```

## ⚠️ Limitations

### Current Limitations

The security system has some limitations to be aware of:

#### Feature Limitations
- **Basic monitoring only**: No advanced threat detection or AI analysis
- **Manual responses**: No automated responses beyond basic IP blocking
- **Simple profiling**: Basic player and IP tracking without complex analysis
- **Limited integration**: Works primarily within NeoEssentials ecosystem

#### Technical Limitations
- **Memory usage**: Security profiles are stored in memory (cleared on restart)
- **Event history**: Limited to last 1000 events per session
- **No persistence**: Security data isn't saved between server restarts
- **Single server**: No multi-server security coordination

#### Recommended Additions
For enhanced security, consider using NeoEssentials alongside:
- **Dedicated anti-cheat plugins** for movement and combat monitoring
- **External security tools** for network-level protection
- **Backup systems** for data protection and recovery
- **Log analysis tools** for advanced pattern detection

---

**Related Documentation**: [Permissions](Permissions.md) | [Configuration](Configuration.md) | [Essential Commands](Essential-Commands.md)

*Last Updated: August 9, 2025*
