package com.zerog.neoessentials.systems.security;

import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Security analytics and metrics collection
 */
public class SecurityAnalytics {
    private final AtomicLong loginAttempts = new AtomicLong(0);
    private final AtomicLong successfulLogins = new AtomicLong(0);
    private final AtomicLong failedLogins = new AtomicLong(0);
    private final AtomicLong securityViolations = new AtomicLong(0);
    private final AtomicLong threatsDetected = new AtomicLong(0);
    
    private LocalDateTime lastReset = LocalDateTime.now();
    
    public void recordLoginAttempt() {
        loginAttempts.incrementAndGet();
    }
    
    public void recordSuccessfulLogin() {
        successfulLogins.incrementAndGet();
    }
    
    public void recordFailedLogin() {
        failedLogins.incrementAndGet();
    }
    
    public void recordSecurityViolation() {
        securityViolations.incrementAndGet();
    }
    
    public void recordThreatDetected() {
        threatsDetected.incrementAndGet();
    }
    
    public long getLoginAttempts() { return loginAttempts.get(); }
    public long getSuccessfulLogins() { return successfulLogins.get(); }
    public long getFailedLogins() { return failedLogins.get(); }
    public long getSecurityViolations() { return securityViolations.get(); }
    public long getThreatsDetected() { return threatsDetected.get(); }
    
    public double getSuccessRate() {
        long total = loginAttempts.get();
        return total > 0 ? (double) successfulLogins.get() / total : 0.0;
    }
    
    public void reset() {
        loginAttempts.set(0);
        successfulLogins.set(0);
        failedLogins.set(0);
        securityViolations.set(0);
        threatsDetected.set(0);
        lastReset = LocalDateTime.now();
    }
    
    public LocalDateTime getLastReset() {
        return lastReset;
    }
    
    /**
     * Initialize the analytics system
     */
    public void initialize() {
        reset();
    }
    
    /**
     * Process a security event for analytics
     */
    public void processEvent(SecurityEvent event) {
        switch (event.getType()) {
            case LOGIN_SUCCESS:
                recordSuccessfulLogin();
                break;
            case LOGIN_FAILURE:
                recordFailedLogin();
                break;
            case SECURITY_VIOLATION:
                recordSecurityViolation();
                break;
            case THREAT_DETECTED:
                recordThreatDetected();
                break;
            default:
                break;
        }
    }
    
    /**
     * Get overall security score (0.0 - 1.0)
     */
    public double getSecurityScore() {
        double successRate = getSuccessRate();
        long totalViolations = securityViolations.get() + threatsDetected.get();
        
        // Lower score for more violations and lower success rate
        double violationPenalty = Math.min(totalViolations * 0.05, 0.5);
        double successBonus = successRate * 0.3;
        
        return Math.max(0.0, Math.min(1.0, 0.7 + successBonus - violationPenalty));
    }
    
    /**
     * Generate a security report
     */
    public SecurityReport generateReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("login_attempts", getLoginAttempts());
        metrics.put("successful_logins", getSuccessfulLogins());
        metrics.put("failed_logins", getFailedLogins());
        metrics.put("security_violations", getSecurityViolations());
        metrics.put("threats_detected", getThreatsDetected());
        metrics.put("success_rate", getSuccessRate());
        metrics.put("security_score", getSecurityScore());
        
        List<SecurityEvent> events = new ArrayList<>(); // In a real implementation, this would fetch events from storage
        List<String> recommendations = new ArrayList<>();
        
        // Add recommendations based on metrics
        if (getSuccessRate() < 0.8) {
            recommendations.add("Consider implementing additional authentication measures");
        }
        if (getSecurityViolations() > 10) {
            recommendations.add("Review and strengthen security policies");
        }
        if (getThreatsDetected() > 5) {
            recommendations.add("Investigate and address detected threats");
        }
        
        return new SecurityReport(startTime, endTime, metrics, events, recommendations);
    }
}
