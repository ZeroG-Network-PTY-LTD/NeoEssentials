package com.zerog.neoessentials.models.security;

import java.time.LocalDateTime;

/**
 * Represents the current security status of the system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SecurityStatus {
    private final boolean enabled;
    private final int activeSessions;
    private final int securityViolations;
    private final int threatsDetected;
    private final String threatLevel;
    private final double securityScore;
    private final LocalDateTime lastUpdated;
    
    public SecurityStatus(boolean enabled, int activeSessions, int securityViolations, 
                         int threatsDetected, String threatLevel, double securityScore, 
                         LocalDateTime lastUpdated) {
        this.enabled = enabled;
        this.activeSessions = activeSessions;
        this.securityViolations = securityViolations;
        this.threatsDetected = threatsDetected;
        this.threatLevel = threatLevel;
        this.securityScore = securityScore;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public int getActiveSessions() { return activeSessions; }
    public int getSecurityViolations() { return securityViolations; }
    public int getThreatsDetected() { return threatsDetected; }
    public String getThreatLevel() { return threatLevel; }
    public double getSecurityScore() { return securityScore; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    
    /**
     * Check if the security status is healthy
     */
    public boolean isHealthy() {
        return enabled && securityScore > 0.7 && threatLevel.equals("LOW");
    }
    
    @Override
    public String toString() {
        return String.format("SecurityStatus{enabled=%s, sessions=%d, violations=%d, threats=%d, level=%s, score=%.2f}", 
                enabled, activeSessions, securityViolations, threatsDetected, threatLevel, securityScore);
    }
}
