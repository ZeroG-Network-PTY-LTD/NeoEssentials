# Security Features

NeoEssentials includes a comprehensive security framework designed to protect servers from abuse, griefing, and unauthorized access. The system provides real-time monitoring, automated response, and detailed logging capabilities.

## 🔐 Overview

The security system provides:
- **Real-time threat detection** with automated responses
- **Advanced logging** with detailed audit trails
- **Rate limiting** to prevent command spam and abuse
- **Player behavior monitoring** with automatic flagging
- **Anti-grief protection** with rollback capabilities
- **Suspicious activity detection** using AI-powered analysis
- **Integration with external security systems**
- **Customizable security policies** for different server types

## 🛡️ Threat Detection

### Real-time Monitoring

The security system continuously monitors:

#### Command Abuse Detection
- **Rapid command execution**: Detects spam and automation
- **Unusual command patterns**: Identifies suspicious behavior
- **Resource-intensive commands**: Monitors performance impact
- **Permission violations**: Tracks unauthorized access attempts

#### Movement Analysis
- **Speed hacking detection**: Impossible movement speeds
- **Flight detection**: Unauthorized flying in survival mode
- **Teleportation anomalies**: Suspicious location changes
- **NoClip detection**: Movement through solid blocks

#### Chat Monitoring
- **Spam detection**: Repeated or rapid messages
- **Inappropriate content**: Configurable word filters
- **Social engineering**: Attempts to trick players
- **Information disclosure**: Sharing sensitive data

#### Block Interaction Monitoring
- **Grief pattern detection**: Large-scale destruction
- **Rapid building**: Potential WorldEdit abuse
- **Suspicious mining**: X-ray behavior patterns
- **Container access**: Unauthorized chest opening

### Detection Configuration

```toml
[security.detection]
# Enable real-time threat detection
enabled = true

# Command abuse detection
[security.detection.commands]
# Maximum commands per second
maxCommandsPerSecond = 5
# Detection window in seconds
detectionWindow = 10
# Commands to monitor closely
monitoredCommands = [
    "essentials.give",
    "essentials.gamemode",
    "essentials.tp"
]

# Movement analysis
[security.detection.movement]
# Enable movement monitoring
enabled = true
# Maximum movement speed (blocks per second)
maxSpeed = 20.0
# Teleportation distance threshold
teleportThreshold = 100.0
# Flight detection sensitivity
flightSensitivity = 0.8

# Chat monitoring
[security.detection.chat]
# Enable chat monitoring
enabled = true
# Maximum messages per minute
maxMessagesPerMinute = 10
# Spam detection threshold
spamThreshold = 3
# Filter inappropriate content
enableContentFilter = true

# Block interaction monitoring
[security.detection.blocks]
# Enable block monitoring
enabled = true
# Maximum blocks per second
maxBlocksPerSecond = 10
# Grief detection threshold
griefThreshold = 100
# Monitor container access
monitorContainers = true
```

## 🚨 Automated Response System

### Response Actions

When threats are detected, the system can automatically:

#### Warning System
- **Progressive warnings**: Escalating warning levels
- **Temporary restrictions**: Limited command access
- **Chat notifications**: Alert staff to suspicious activity
- **Player education**: Provide guidance on proper behavior

#### Temporary Restrictions
- **Command cooldowns**: Increased delays between commands
- **Movement limitations**: Reduced speed or range
- **Chat restrictions**: Temporary mute or rate limiting
- **Building limitations**: Reduced block placement speed

#### Protective Actions
- **Automatic rollback**: Reverse griefing damage
- **Inventory snapshots**: Backup player inventories
- **Location tracking**: Enhanced monitoring for flagged players
- **Staff notifications**: Real-time alerts to online moderators

#### Severe Responses
- **Temporary suspension**: Short-term access restrictions
- **Quarantine mode**: Isolated environment for investigation
- **Automatic reporting**: Generate detailed incident reports
- **Evidence collection**: Gather proof for manual review

### Response Configuration

```toml
[security.responses]
# Enable automated responses
enabled = true

# Warning system
[security.responses.warnings]
# Enable progressive warnings
enabled = true
# Warning levels before escalation
maxWarnings = 3
# Warning reset time in hours
resetTime = 24

# Temporary restrictions
[security.responses.restrictions]
# Command cooldown multiplier for flagged players
cooldownMultiplier = 2.0
# Movement speed reduction factor
speedReduction = 0.5
# Chat rate limit for flagged players
chatRateLimit = 5

# Protective actions
[security.responses.protection]
# Enable automatic rollback
autoRollback = true
# Maximum rollback time in minutes
maxRollbackTime = 30
# Enable inventory snapshots
inventorySnapshots = true
# Snapshot frequency in minutes
snapshotFrequency = 15

# Staff notifications
[security.responses.notifications]
# Notify online staff immediately
immediateNotification = true
# Notification methods
methods = ["chat", "email"]
# Minimum threat level for notifications
minThreatLevel = "MEDIUM"
```

## 📊 Logging & Audit Trail

### Comprehensive Logging

The security system logs all relevant activities:

#### Security Events
- **Threat detections**: All security violations
- **Response actions**: Automated and manual responses
- **System changes**: Configuration updates and modifications
- **Access attempts**: Successful and failed login attempts

#### Player Activities
- **Command execution**: All commands with parameters
- **Chat messages**: Complete chat history with timestamps
- **Movement tracking**: Location changes and teleportations
- **Block interactions**: Placed, broken, and modified blocks

#### Administrative Actions
- **Staff commands**: All administrative actions
- **Permission changes**: Group and individual permission updates
- **Configuration changes**: System setting modifications
- **Manual interventions**: Override actions and investigations

### Log Formats

#### Standard Log Format
```
[2025-08-03 14:30:15] [SECURITY] [HIGH] Player 'Griefer123' triggered grief detection (150 blocks destroyed in 30 seconds) at (123, 64, -456)
[2025-08-03 14:30:16] [RESPONSE] [AUTO] Applied building restrictions to 'Griefer123' for 300 seconds
[2025-08-03 14:30:17] [NOTIFY] [STAFF] Alert sent to online moderators about 'Griefer123' incident
```

#### JSON Log Format
```json
{
  "timestamp": "2025-08-03T14:30:15Z",
  "level": "HIGH",
  "type": "THREAT_DETECTION",
  "player": "Griefer123",
  "event": "grief_detection",
  "details": {
    "blocks_destroyed": 150,
    "time_window": 30,
    "location": {"x": 123, "y": 64, "z": -456}
  },
  "response": "building_restrictions",
  "staff_notified": true
}
```

### Logging Configuration

```toml
[security.logging]
# Enable security logging
enabled = true

# Log file settings
[security.logging.files]
# Log file path
logPath = "logs/security.log"
# Log rotation settings
rotateDaily = true
# Maximum log file size in MB
maxFileSize = 100
# Number of old log files to keep
keepFiles = 30

# Log levels
[security.logging.levels]
# Log all events at or above this level
minLevel = "LOW"
# Available levels: TRACE, LOW, MEDIUM, HIGH, CRITICAL

# Log formats
[security.logging.formats]
# Console output format
consoleFormat = "standard"
# File output format
fileFormat = "json"
# Available formats: standard, json, custom

# Specific logging categories
[security.logging.categories]
threats = true
responses = true
playerActivity = true
adminActions = true
systemEvents = true
```

## 🎯 Rate Limiting

### Command Rate Limiting

Prevents abuse through intelligent command throttling:

#### Per-Player Limits
- **Global command rate**: Overall commands per time period
- **Specific command limits**: Individual command restrictions
- **Resource-based limits**: Commands that affect server performance
- **Permission-based exemptions**: Staff and trusted player bypasses

#### Global Limits
- **Server-wide command rate**: Total commands across all players
- **Performance-based throttling**: Dynamic limits based on server load
- **Peak hour restrictions**: Enhanced limits during busy periods
- **Emergency throttling**: Automatic restrictions under stress

### Rate Limiting Configuration

```toml
[security.rateLimiting]
# Enable rate limiting
enabled = true

# Per-player limits
[security.rateLimiting.player]
# Global commands per minute
globalCommandsPerMinute = 60
# Specific command limits
[security.rateLimiting.player.commands]
"essentials.give" = 5
"essentials.gamemode" = 3
"essentials.tp" = 10

# Burst allowance (short-term higher rate)
burstAllowance = 10
# Burst window in seconds
burstWindow = 5

# Global server limits
[security.rateLimiting.server]
# Total commands per second across all players
maxCommandsPerSecond = 50
# Performance-based throttling
performanceThrottling = true
# CPU usage threshold for throttling (%)
cpuThreshold = 80

# Exemptions
[security.rateLimiting.exemptions]
# Permission to bypass rate limits
bypassPermission = "neoessentials.security.bypass.ratelimit"
# Whitelisted commands (no limits)
whitelistedCommands = [
    "essentials.home",
    "essentials.spawn"
]
```

## 🧠 Behavior Analysis

### AI-Powered Detection

Advanced behavior analysis using machine learning:

#### Pattern Recognition
- **Normal behavior baseline**: Learn typical player patterns
- **Anomaly detection**: Identify deviations from normal behavior
- **Trend analysis**: Spot gradual changes in behavior
- **Correlation analysis**: Connect related suspicious activities

#### Risk Scoring
- **Dynamic risk scores**: Real-time player risk assessment
- **Historical analysis**: Consider past behavior in scoring
- **Contextual factors**: Account for time, location, and situation
- **Predictive modeling**: Anticipate future security risks

#### Adaptive Learning
- **Behavior model updates**: Continuously improve detection
- **False positive reduction**: Learn from incorrect detections
- **New threat recognition**: Adapt to evolving attack methods
- **Community-specific patterns**: Customize for server culture

### Behavior Analysis Configuration

```toml
[security.behaviorAnalysis]
# Enable AI-powered behavior analysis
enabled = true

# Learning settings
[security.behaviorAnalysis.learning]
# Learning period for baseline behavior (days)
learningPeriod = 7
# Minimum data points for analysis
minDataPoints = 100
# Model update frequency (hours)
updateFrequency = 6

# Risk scoring
[security.behaviorAnalysis.scoring]
# Risk score threshold for action
actionThreshold = 75
# Score decay rate (points per hour)
decayRate = 5
# Maximum risk score
maxScore = 100

# Detection categories
[security.behaviorAnalysis.categories]
# Weight factors for different behaviors
commandAbuse = 1.0
movementAnomalies = 0.8
chatViolations = 0.6
blockInteractions = 1.2
socialEngineering = 1.5
```

## 🔍 Investigation Tools

### Forensic Capabilities

Tools for investigating security incidents:

#### `/security investigate <player>`
Launch detailed investigation interface.

**Features**:
- Complete activity timeline
- Risk score history
- Correlation analysis
- Evidence collection
- Report generation

**Example Usage**:
```bash
# Investigate suspicious player
/security investigate Griefer123

# Generate investigation report
/security investigate Griefer123 --report
```

#### `/security timeline <player> [time]`
View detailed activity timeline for a player.

**Examples**:
```bash
# Last 24 hours
/security timeline Griefer123

# Specific time range
/security timeline Griefer123 2025-08-03 2025-08-04

# Last week
/security timeline Griefer123 7d
```

#### `/security correlate <event>`
Find related security events.

**Examples**:
```bash
# Find related grief events
/security correlate grief_detection

# Correlate by location
/security correlate location 123,64,-456

# Correlate by time
/security correlate time 2025-08-03_14:30
```

### Investigation Commands

#### `/security status [player]`
View current security status and active flags.

**Example Output**:
```
=== Security Status for Griefer123 ===
Risk Score: 78/100 (HIGH)
Active Flags: grief_detection, movement_anomaly
Restrictions: building_limited, chat_throttled
Last Incident: 5 minutes ago
Under Investigation: Yes

Recent Activity Summary:
- 150 blocks destroyed in 30 seconds
- Unusual movement patterns detected
- Multiple staff reports received
```

#### `/security history <player>`
View complete security history for a player.

**Example Output**:
```
=== Security History for Griefer123 ===
Total Incidents: 23
First Incident: 2025-07-15 (19 days ago)
Risk Trend: Increasing

Recent Incidents:
1. [2025-08-03 14:30] Grief Detection (HIGH)
2. [2025-08-02 16:45] Command Abuse (MEDIUM)
3. [2025-08-01 12:20] Chat Violation (LOW)
4. [2025-07-30 09:15] Movement Anomaly (MEDIUM)
5. [2025-07-28 20:30] Building Anomaly (LOW)
```

#### `/security quarantine <player> [reason]`
Place player in quarantine for investigation.

**Features**:
- Isolated environment
- Limited permissions
- Enhanced monitoring
- Evidence preservation

**Example**:
```bash
# Quarantine suspicious player
/security quarantine Griefer123 "Suspected grief - under investigation"
```

## 🔐 Access Control

### Advanced Authentication

Enhanced security for player authentication:

#### Multi-Factor Authentication
- **Optional 2FA**: Two-factor authentication for accounts
- **IP whitelist**: Restrict access to specific IP addresses
- **Hardware fingerprinting**: Device identification
- **Geographic restrictions**: Block access from certain regions

#### Session Security
- **Session monitoring**: Track active player sessions
- **Concurrent session limits**: Prevent account sharing
- **Automatic logout**: Idle session termination
- **Suspicious login detection**: Unusual access patterns

### Access Control Configuration

```toml
[security.accessControl]
# Enable advanced access control
enabled = true

# Multi-factor authentication
[security.accessControl.mfa]
# Enable 2FA system
enabled = false
# Require 2FA for staff
requireForStaff = true
# 2FA methods available
methods = ["totp", "email", "sms"]

# IP restrictions
[security.accessControl.ip]
# Enable IP whitelisting for staff
staffWhitelist = false
# Block known VPN/proxy IPs
blockVpns = true
# Geographic restrictions
[security.accessControl.ip.geographic]
enabled = false
allowedCountries = ["US", "CA", "GB"]
blockedCountries = ["XX", "YY"]

# Session security
[security.accessControl.sessions]
# Maximum session duration (minutes)
maxSessionDuration = 480
# Idle timeout (minutes)
idleTimeout = 60
# Maximum concurrent sessions per account
maxConcurrentSessions = 1
```

## 🤖 Automated Moderation

### Smart Moderation Actions

Intelligent automated moderation based on security analysis:

#### Progressive Discipline
- **Warning escalation**: Automatic warning progression
- **Temporary restrictions**: Graduated response system
- **Cooling-off periods**: Automatic breaks for heated situations
- **Rehabilitation tracking**: Monitor improvement over time

#### Context-Aware Actions
- **Situation analysis**: Consider context before taking action
- **Intent recognition**: Distinguish between malicious and accidental
- **Community impact**: Factor in effects on other players
- **Historical context**: Use past behavior for decision making

### Automated Moderation Configuration

```toml
[security.autoModeration]
# Enable automated moderation
enabled = true

# Progressive discipline
[security.autoModeration.progressive]
# Enable graduated response system
enabled = true
# Warning stages before stronger action
warningStages = 3
# Time between warning resets (hours)
resetPeriod = 168  # 1 week

# Action thresholds by risk score
[security.autoModeration.thresholds]
# Thresholds for automatic actions
warning = 25
restriction = 50
suspension = 75
quarantine = 90

# Restriction types and durations
[security.autoModeration.restrictions]
[security.autoModeration.restrictions.chat]
# Chat restrictions
muteMinutes = [5, 15, 60, 180]  # Progressive mute durations
rateLimitFactor = 0.5

[security.autoModeration.restrictions.movement]
# Movement restrictions
speedReduction = 0.3
teleportCooldown = 300  # 5 minutes

[security.autoModeration.restrictions.building]
# Building restrictions
blockPlaceDelay = 2000  # milliseconds
maxBlocksPerMinute = 30
```

## 📈 Performance & Monitoring

### Security Performance

Monitor the security system's performance:

#### Performance Metrics
- **Detection latency**: Time to identify threats
- **Response time**: Speed of automated responses
- **False positive rate**: Accuracy of threat detection
- **System overhead**: Performance impact on server

#### Resource Usage
- **CPU utilization**: Processing power used by security system
- **Memory usage**: RAM consumed by monitoring and logging
- **Disk I/O**: Log writing and data storage impact
- **Network overhead**: Security-related network traffic

### Performance Configuration

```toml
[security.performance]
# Performance optimization settings
[security.performance.optimization]
# Enable performance optimizations
enabled = true
# Maximum CPU usage for security system (%)
maxCpuUsage = 15
# Memory limit for security caching (MB)
memoryCacheLimit = 256

# Monitoring intervals
[security.performance.monitoring]
# How often to check for threats (milliseconds)
detectionInterval = 1000
# How often to update behavior models (minutes)
modelUpdateInterval = 15
# How often to clean up old data (hours)
cleanupInterval = 6

# Data retention
[security.performance.retention]
# Security log retention (days)
logRetention = 90
# Behavior data retention (days)
behaviorDataRetention = 30
# Investigation data retention (days)
investigationRetention = 180
```

## 🔧 Integration

### External System Integration

Connect with external security and monitoring systems:

#### Logging Systems
```toml
[security.integration.logging]
# External log aggregation
[security.integration.logging.elasticsearch]
enabled = false
host = "localhost:9200"
index = "neoessentials-security"

[security.integration.logging.splunk]
enabled = false
host = "splunk.example.com"
token = "your-splunk-token"
```

#### Security Plugins
```toml
[security.integration.plugins]
# Integration with other security plugins
[security.integration.plugins.coreprotect]
enabled = true
shareGriefData = true

[security.integration.plugins.worldguard]
enabled = true
respectRegions = true
```

## 🚨 Incident Response

### Automated Incident Response

Streamlined response to security incidents:

#### Incident Classification
- **Threat level assessment**: Automatic severity classification
- **Impact analysis**: Assess damage and affected players
- **Response prioritization**: Handle incidents by severity
- **Escalation procedures**: Automatic staff notification

#### Response Procedures
- **Immediate containment**: Prevent further damage
- **Evidence preservation**: Secure data for investigation
- **Stakeholder notification**: Alert affected players and staff
- **Recovery planning**: Coordinate restoration efforts

### Incident Response Configuration

```toml
[security.incidentResponse]
# Enable automated incident response
enabled = true

# Classification thresholds
[security.incidentResponse.classification]
# Threat levels and automatic responses
[security.incidentResponse.classification.low]
autoResponse = "warning"
staffNotification = false
logLevel = "INFO"

[security.incidentResponse.classification.medium]
autoResponse = "restriction"
staffNotification = true
logLevel = "WARN"

[security.incidentResponse.classification.high]
autoResponse = "quarantine"
staffNotification = true
logLevel = "ERROR"
emergencyAlert = true

[security.incidentResponse.classification.critical]
autoResponse = "suspend"
staffNotification = true
logLevel = "FATAL"
emergencyAlert = true
administratorAlert = true
```

---

**Related Documentation**: [Permissions](Permissions.md) | [Configuration](Configuration.md) | [Essential Commands](Essential-Commands.md)

*Last Updated: August 3, 2025*
