package com.zerog.neoessentials.systems.security;

import java.time.LocalDateTime;

/**
 * Overall security status of the system
 */
public class SecurityStatus {
    private final boolean secure;
    private final String status;
    private final int activeSessions;
    private final int activeAlerts;
    private final int threatLevel;
    private final double riskScore;
    private final LocalDateTime lastAssessment;
    private final String lastIncident;
    private final LocalDateTime lastIncidentTime;
    
    public SecurityStatus(boolean secure, String status, int activeSessions, 
                         int activeAlerts, int threatLevel, double riskScore,
                         String lastIncident, LocalDateTime lastIncidentTime) {
        this.secure = secure;
        this.status = status;
        this.activeSessions = activeSessions;
        this.activeAlerts = activeAlerts;
        this.threatLevel = threatLevel;
        this.riskScore = riskScore;
        this.lastIncident = lastIncident;
        this.lastIncidentTime = lastIncidentTime;
        this.lastAssessment = LocalDateTime.now();
    }
    
    // Alternative constructor for SecurityManager compatibility
    public SecurityStatus(boolean isRunning, int activeAlertsCount, int recentEventsCount,
                         int activeSessionCount, String threatLevel, double securityScore,
                         LocalDateTime timestamp) {
        this.secure = isRunning;
        this.status = isRunning ? "RUNNING" : "STOPPED";
        this.activeSessions = activeSessionCount;
        this.activeAlerts = activeAlertsCount;
        this.threatLevel = convertThreatLevel(threatLevel);
        this.riskScore = securityScore;
        this.lastIncident = "No recent incidents";
        this.lastIncidentTime = null;
        this.lastAssessment = timestamp;
    }
    
    private int convertThreatLevel(String level) {
        return switch (level.toUpperCase()) {
            case "NONE", "MINIMAL" -> 0;
            case "LOW" -> 1;
            case "MEDIUM", "MODERATE" -> 2;
            case "HIGH" -> 3;
            case "SEVERE" -> 4;
            case "CRITICAL" -> 5;
            default -> 1;
        };
    }
    
    public boolean isSecure() { return secure; }
    public String getStatus() { return status; }
    public int getActiveSessions() { return activeSessions; }
    public int getActiveAlerts() { return activeAlerts; }
    public int getThreatLevel() { return threatLevel; }
    public double getRiskScore() { return riskScore; }
    public LocalDateTime getLastAssessment() { return lastAssessment; }
    public String getLastIncident() { return lastIncident; }
    public LocalDateTime getLastIncidentTime() { return lastIncidentTime; }
    
    public String getThreatLevelText() {
        return switch (threatLevel) {
            case 0 -> "MINIMAL";
            case 1 -> "LOW";
            case 2 -> "MODERATE";
            case 3 -> "HIGH";
            case 4 -> "SEVERE";
            case 5 -> "CRITICAL";
            default -> "UNKNOWN";
        };
    }
    
    public String getRiskLevelText() {
        if (riskScore < 0.2) return "VERY_LOW";
        if (riskScore < 0.4) return "LOW";
        if (riskScore < 0.6) return "MODERATE";
        if (riskScore < 0.8) return "HIGH";
        return "CRITICAL";
    }
}
