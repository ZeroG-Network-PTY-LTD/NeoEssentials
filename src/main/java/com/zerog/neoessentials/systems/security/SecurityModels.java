package com.zerog.neoessentials.systems.security;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a security event in the audit trail
 */
public class SecurityEvent {
    private final String id;
    private final SecurityEventType type;
    private final String user;
    private final String action;
    private final SecurityLevel level;
    private final Map<String, Object> details;
    private final LocalDateTime timestamp;
    private final String sourceIP;
    private final String userAgent;
    
    public SecurityEvent(SecurityEventType type, String user, String action, 
                        SecurityLevel level, Map<String, Object> details) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.user = user;
        this.action = action;
        this.level = level;
        this.details = details;
        this.timestamp = LocalDateTime.now();
        this.sourceIP = "127.0.0.1"; // TODO: Get real IP
        this.userAgent = "NeoEssentials"; // TODO: Get real user agent
    }
    
    // Getters
    public String getId() { return id; }
    public SecurityEventType getType() { return type; }
    public String getUser() { return user; }
    public String getAction() { return action; }
    public SecurityLevel getLevel() { return level; }
    public Map<String, Object> getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSourceIP() { return sourceIP; }
    public String getUserAgent() { return userAgent; }
}

/**
 * Types of security events
 */
public enum SecurityEventType {
    // System events
    SYSTEM_STARTUP,
    SYSTEM_SHUTDOWN,
    CONFIG_CHANGE,
    
    // Authentication events
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    SESSION_EXPIRED,
    PASSWORD_CHANGE,
    
    // Authorization events
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    ACCESS_DENIED,
    PRIVILEGE_ESCALATION,
    
    // Administrative events
    COMMAND_EXECUTED,
    FILE_ACCESS,
    DATA_EXPORT,
    BACKUP_CREATED,
    
    // Security events
    SECURITY_VIOLATION,
    SUSPICIOUS_ACTIVITY,
    THREAT_DETECTED,
    INTRUSION_ATTEMPT,
    
    // Audit events
    REPORT_GENERATED,
    AUDIT_LOG_ROTATED,
    SECURITY_SCAN_COMPLETED
}

/**
 * Security level classification
 */
public enum SecurityLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    CRITICAL(5);
    
    private final int priority;
    
    SecurityLevel(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() { return priority; }
    
    public boolean isMoreSevereThan(SecurityLevel other) {
        return this.priority > other.priority;
    }
}

/**
 * Security alert for immediate attention
 */
public class SecurityAlert {
    private final String id;
    private final SecurityEventType type;
    private final SecurityLevel severity;
    private final String description;
    private final Map<String, Object> details;
    private final LocalDateTime timestamp;
    private boolean acknowledged = false;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    
    public SecurityAlert(SecurityEventType type, SecurityLevel severity, 
                        String description, Map<String, Object> details, 
                        LocalDateTime timestamp) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.details = details;
        this.timestamp = timestamp;
    }
    
    public void acknowledge(String acknowledgedBy) {
        this.acknowledged = true;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = LocalDateTime.now();
    }
    
    // Getters
    public String getId() { return id; }
    public SecurityEventType getType() { return type; }
    public SecurityLevel getSeverity() { return severity; }
    public String getDescription() { return description; }
    public Map<String, Object> getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAcknowledged() { return acknowledged; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
}

/**
 * Security violation detection result
 */
public class SecurityViolation {
    private final SecurityEventType type;
    private final SecurityLevel severity;
    private final String user;
    private final String description;
    private final Map<String, Object> details;
    
    public SecurityViolation(SecurityEventType type, SecurityLevel severity, 
                           String user, String description, Map<String, Object> details) {
        this.type = type;
        this.severity = severity;
        this.user = user;
        this.description = description;
        this.details = details;
    }
    
    // Getters
    public SecurityEventType getType() { return type; }
    public SecurityLevel getSeverity() { return severity; }
    public String getUser() { return user; }
    public String getDescription() { return description; }
    public Map<String, Object> getDetails() { return details; }
}

/**
 * Overall security status
 */
public class SecurityStatus {
    private final boolean systemActive;
    private final int activeAlerts;
    private final int recentEvents;
    private final int activeSessions;
    private final String threatLevel;
    private final double securityScore;
    private final LocalDateTime lastUpdate;
    
    public SecurityStatus(boolean systemActive, int activeAlerts, int recentEvents, 
                         int activeSessions, String threatLevel, double securityScore, 
                         LocalDateTime lastUpdate) {
        this.systemActive = systemActive;
        this.activeAlerts = activeAlerts;
        this.recentEvents = recentEvents;
        this.activeSessions = activeSessions;
        this.threatLevel = threatLevel;
        this.securityScore = securityScore;
        this.lastUpdate = lastUpdate;
    }
    
    // Getters
    public boolean isSystemActive() { return systemActive; }
    public int getActiveAlerts() { return activeAlerts; }
    public int getRecentEvents() { return recentEvents; }
    public int getActiveSessions() { return activeSessions; }
    public String getThreatLevel() { return threatLevel; }
    public double getSecurityScore() { return securityScore; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
}

/**
 * Security metrics for a specific user
 */
public class SecurityMetrics {
    private final String username;
    private int totalEvents = 0;
    private int loginAttempts = 0;
    private int failedLogins = 0;
    private int commandsExecuted = 0;
    private int securityViolations = 0;
    private LocalDateTime lastActivity;
    private LocalDateTime firstSeen;
    private double riskScore = 0.0;
    
    public SecurityMetrics(String username) {
        this.username = username;
        this.firstSeen = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }
    
    public void recordEvent(SecurityEvent event) {
        totalEvents++;
        lastActivity = event.getTimestamp();
        
        switch (event.getType()) {
            case LOGIN_SUCCESS, LOGIN_FAILURE -> loginAttempts++;
            case LOGIN_FAILURE -> failedLogins++;
            case COMMAND_EXECUTED -> commandsExecuted++;
            case SECURITY_VIOLATION, SUSPICIOUS_ACTIVITY -> {
                securityViolations++;
                riskScore += 10.0; // Increase risk score
            }
        }
        
        // Calculate risk score based on behavior patterns
        updateRiskScore();
    }
    
    private void updateRiskScore() {
        // Simple risk calculation - can be enhanced
        double baseRisk = 0.0;
        
        // Failed login ratio
        if (loginAttempts > 0) {
            double failureRate = (double) failedLogins / loginAttempts;
            baseRisk += failureRate * 20.0;
        }
        
        // Security violations weight
        baseRisk += securityViolations * 15.0;
        
        // Activity frequency (high activity might indicate automation)
        if (totalEvents > 100) {
            baseRisk += Math.log(totalEvents) * 2.0;
        }
        
        // Cap at 100
        riskScore = Math.min(100.0, baseRisk);
    }
    
    // Getters
    public String getUsername() { return username; }
    public int getTotalEvents() { return totalEvents; }
    public int getLoginAttempts() { return loginAttempts; }
    public int getFailedLogins() { return failedLogins; }
    public int getCommandsExecuted() { return commandsExecuted; }
    public int getSecurityViolations() { return securityViolations; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public double getRiskScore() { return riskScore; }
}

/**
 * Security configuration
 */
public class SecurityConfig {
    private boolean auditLogEnabled = true;
    private long maxAuditLogSize = 100 * 1024 * 1024; // 100MB
    private int sessionTimeout = 3600; // 1 hour
    private int maxFailedLogins = 5;
    private boolean threatDetectionEnabled = true;
    private boolean autoBlockSuspiciousIPs = true;
    private boolean securityReportsEnabled = true;
    private int reportGenerationInterval = 6; // hours
    private double riskScoreThreshold = 75.0;
    private boolean enableRealTimeMonitoring = true;
    
    // Getters and setters
    public boolean isAuditLogEnabled() { return auditLogEnabled; }
    public void setAuditLogEnabled(boolean auditLogEnabled) { this.auditLogEnabled = auditLogEnabled; }
    
    public long getMaxAuditLogSize() { return maxAuditLogSize; }
    public void setMaxAuditLogSize(long maxAuditLogSize) { this.maxAuditLogSize = maxAuditLogSize; }
    
    public int getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }
    
    public int getMaxFailedLogins() { return maxFailedLogins; }
    public void setMaxFailedLogins(int maxFailedLogins) { this.maxFailedLogins = maxFailedLogins; }
    
    public boolean isThreatDetectionEnabled() { return threatDetectionEnabled; }
    public void setThreatDetectionEnabled(boolean threatDetectionEnabled) { this.threatDetectionEnabled = threatDetectionEnabled; }
    
    public boolean isAutoBlockSuspiciousIPs() { return autoBlockSuspiciousIPs; }
    public void setAutoBlockSuspiciousIPs(boolean autoBlockSuspiciousIPs) { this.autoBlockSuspiciousIPs = autoBlockSuspiciousIPs; }
    
    public boolean isSecurityReportsEnabled() { return securityReportsEnabled; }
    public void setSecurityReportsEnabled(boolean securityReportsEnabled) { this.securityReportsEnabled = securityReportsEnabled; }
    
    public int getReportGenerationInterval() { return reportGenerationInterval; }
    public void setReportGenerationInterval(int reportGenerationInterval) { this.reportGenerationInterval = reportGenerationInterval; }
    
    public double getRiskScoreThreshold() { return riskScoreThreshold; }
    public void setRiskScoreThreshold(double riskScoreThreshold) { this.riskScoreThreshold = riskScoreThreshold; }
    
    public boolean isEnableRealTimeMonitoring() { return enableRealTimeMonitoring; }
    public void setEnableRealTimeMonitoring(boolean enableRealTimeMonitoring) { this.enableRealTimeMonitoring = enableRealTimeMonitoring; }
}
