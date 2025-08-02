package com.zerog.neoessentials.systems.security;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Security metrics for users
 */
public class SecurityMetrics {
    private final String userId;
    private final AtomicLong loginAttempts = new AtomicLong(0);
    private final AtomicLong successfulLogins = new AtomicLong(0);
    private final AtomicLong failedLogins = new AtomicLong(0);
    private final AtomicLong permissionDenials = new AtomicLong(0);
    private final AtomicLong securityViolations = new AtomicLong(0);
    private LocalDateTime lastLogin;
    private LocalDateTime lastFailedLogin;
    private LocalDateTime firstSeen;
    private LocalDateTime lastActivity;
    
    public SecurityMetrics(String userId) {
        this.userId = userId;
        this.firstSeen = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }
    
    public String getUserId() { return userId; }
    
    public void recordLoginAttempt() {
        loginAttempts.incrementAndGet();
        lastActivity = LocalDateTime.now();
    }
    
    public void recordSuccessfulLogin() {
        successfulLogins.incrementAndGet();
        lastLogin = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }
    
    public void recordFailedLogin() {
        failedLogins.incrementAndGet();
        lastFailedLogin = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }
    
    public void recordPermissionDenial() {
        permissionDenials.incrementAndGet();
        lastActivity = LocalDateTime.now();
    }
    
    public void recordSecurityViolation() {
        securityViolations.incrementAndGet();
        lastActivity = LocalDateTime.now();
    }
    
    public void updateActivity() {
        lastActivity = LocalDateTime.now();
    }
    
    /**
     * Record a security event for metrics tracking
     */
    public void recordEvent(SecurityEvent event) {
        updateActivity();
        
        switch (event.getType()) {
            case LOGIN_SUCCESS:
                recordSuccessfulLogin();
                break;
            case LOGIN_FAILURE:
                recordFailedLogin();
                break;
            case ACCESS_DENIED:
                recordPermissionDenial();
                break;
            case SECURITY_VIOLATION:
                recordSecurityViolation();
                break;
            default:
                // Update activity for other events
                break;
        }
    }
    
    // Getters
    public long getLoginAttempts() { return loginAttempts.get(); }
    public long getSuccessfulLogins() { return successfulLogins.get(); }
    public long getFailedLogins() { return failedLogins.get(); }
    public long getPermissionDenials() { return permissionDenials.get(); }
    public long getSecurityViolations() { return securityViolations.get(); }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public LocalDateTime getLastFailedLogin() { return lastFailedLogin; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    
    public double getSuccessRate() {
        long total = loginAttempts.get();
        return total > 0 ? (double) successfulLogins.get() / total : 0.0;
    }
    
    public boolean isSuspicious() {
        return securityViolations.get() > 5 || 
               (failedLogins.get() > 10 && getSuccessRate() < 0.5);
    }
}
