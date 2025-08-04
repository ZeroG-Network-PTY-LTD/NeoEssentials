package com.zerog.neoessentials.systems.security;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Security Manager for NeoEssentials
 * Provides security monitoring and access control
 */
public class SecurityManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static SecurityManager instance;
    
    private final Map<String, SecurityEvent> recentEvents;
    private final Map<String, Integer> securityLevels;
    private boolean monitoringEnabled;
    
    private SecurityManager() {
        this.recentEvents = new ConcurrentHashMap<>();
        this.securityLevels = new ConcurrentHashMap<>();
        this.monitoringEnabled = true;
        LOGGER.info("SecurityManager initialized");
    }
    
    public static SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }
    
    /**
     * Log a security event
     */
    public void logSecurityEvent(String eventType, String details, SecurityLevel level) {
        if (!monitoringEnabled) return;
        
        SecurityEvent event = new SecurityEvent(eventType, details, level, System.currentTimeMillis());
        recentEvents.put(eventType + "_" + System.currentTimeMillis(), event);
        
        // Keep only recent events (last 100)
        if (recentEvents.size() > 100) {
            List<String> keys = new ArrayList<>(recentEvents.keySet());
            keys.stream().sorted().limit(keys.size() - 100).forEach(recentEvents::remove);
        }
        
        LOGGER.info("Security Event [{}]: {}", level, details);
    }
    
    /**
     * Get recent security events
     */
    public List<SecurityEvent> getRecentEvents(int count) {
        return recentEvents.values().stream()
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(count)
                .collect(ArrayList::new, (list, event) -> list.add(event), ArrayList::addAll);
    }
    
    /**
     * Check security level for a user
     */
    public int getSecurityLevel(String username) {
        return securityLevels.getOrDefault(username, 0);
    }
    
    /**
     * Set security level for a user
     */
    public void setSecurityLevel(String username, int level) {
        securityLevels.put(username, level);
    }
    
    /**
     * Enable/disable monitoring
     */
    public void setMonitoringEnabled(boolean enabled) {
        this.monitoringEnabled = enabled;
        LOGGER.info("Security monitoring {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Check if monitoring is enabled
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
    
    /**
     * Get all monitored users
     */
    public Set<String> getMonitoredUsers() {
        return new HashSet<>(securityLevels.keySet());
    }
    
    /**
     * Clear security events
     */
    public void clearEvents() {
        recentEvents.clear();
        LOGGER.info("Security events cleared");
    }
}
