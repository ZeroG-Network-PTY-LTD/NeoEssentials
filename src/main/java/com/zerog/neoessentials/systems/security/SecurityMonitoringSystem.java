package com.zerog.neoessentials.systems.security;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Security Monitoring System for NeoEssentials
 * Monitors system security and provides real-time alerts
 */
public class SecurityMonitoringSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static SecurityMonitoringSystem instance;
    
    private final SecurityManager securityManager;
    private final Map<String, Long> lastActivityMap;
    private boolean monitoringActive;
    
    private SecurityMonitoringSystem() {
        this.securityManager = SecurityManager.getInstance();
        this.lastActivityMap = new ConcurrentHashMap<>();
        this.monitoringActive = true;
        LOGGER.info("SecurityMonitoringSystem initialized");
    }
    
    public static SecurityMonitoringSystem getInstance() {
        if (instance == null) {
            instance = new SecurityMonitoringSystem();
        }
        return instance;
    }
    
    /**
     * Monitor user activity
     */
    public void monitorActivity(String username, String activity) {
        if (!monitoringActive) return;
        
        lastActivityMap.put(username, System.currentTimeMillis());
        
        // Log suspicious patterns
        if (isSuspiciousActivity(username, activity)) {
            securityManager.logSecurityEvent(
                SecurityEventType.SUSPICIOUS_ACTIVITY.toString(),
                "Suspicious activity detected for user: " + username + " - " + activity,
                SecurityLevel.MEDIUM
            );
        }
    }
    
    /**
     * Check if activity is suspicious
     */
    private boolean isSuspiciousActivity(String username, String activity) {
        // Simple heuristics for suspicious activity
        if (activity.contains("grief") || activity.contains("hack") || activity.contains("exploit")) {
            return true;
        }
        
        // Check for rapid command execution
        Long lastActivity = lastActivityMap.get(username);
        if (lastActivity != null && (System.currentTimeMillis() - lastActivity) < 100) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get security status
     */
    public Map<String, Object> getSecurityStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("monitoring_active", monitoringActive);
        status.put("tracked_users", lastActivityMap.size());
        status.put("recent_events", securityManager.getRecentEvents(10).size());
        return status;
    }
    
    /**
     * Enable/disable monitoring
     */
    public void setMonitoringActive(boolean active) {
        this.monitoringActive = active;
        LOGGER.info("Security monitoring {}", active ? "activated" : "deactivated");
    }
    
    /**
     * Check if monitoring is active
     */
    public boolean isMonitoringActive() {
        return monitoringActive;
    }
    
    /**
     * Clear monitoring data
     */
    public void clearMonitoringData() {
        lastActivityMap.clear();
        LOGGER.info("Security monitoring data cleared");
    }
}
