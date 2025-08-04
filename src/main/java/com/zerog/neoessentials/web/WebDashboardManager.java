package com.zerog.neoessentials.web;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Web Dashboard Manager for NeoEssentials
 * Provides web-based administration interface
 */
public class WebDashboardManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static WebDashboardManager instance;
    
    private final Map<String, DashboardSession> activeSessions;
    private final Map<String, Object> dashboardData;
    private boolean dashboardEnabled;
    private int port;
    
    private WebDashboardManager() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.dashboardData = new ConcurrentHashMap<>();
        this.dashboardEnabled = false;
        this.port = 8080;
        LOGGER.info("WebDashboardManager initialized");
    }
    
    public static WebDashboardManager getInstance() {
        if (instance == null) {
            instance = new WebDashboardManager();
        }
        return instance;
    }
    
    /**
     * Start the web dashboard
     */
    public boolean start() {
        if (dashboardEnabled) {
            LOGGER.warn("Dashboard is already running on port {}", port);
            return false;
        }
        
        try {
            // Initialize dashboard data
            initializeDashboardData();
            dashboardEnabled = true;
            LOGGER.info("Web dashboard started on port {}", port);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to start web dashboard", e);
            return false;
        }
    }
    
    /**
     * Stop the web dashboard
     */
    public void stop() {
        if (!dashboardEnabled) {
            return;
        }
        
        activeSessions.clear();
        dashboardEnabled = false;
        LOGGER.info("Web dashboard stopped");
    }
    
    /**
     * Initialize dashboard data
     */
    private void initializeDashboardData() {
        dashboardData.put("server_status", "online");
        dashboardData.put("player_count", 0);
        dashboardData.put("uptime", System.currentTimeMillis());
        dashboardData.put("version", "1.0.2");
    }
    
    /**
     * Update dashboard data
     */
    public void updateData(String key, Object value) {
        dashboardData.put(key, value);
    }
    
    /**
     * Get dashboard data
     */
    public Map<String, Object> getDashboardData() {
        return new HashMap<>(dashboardData);
    }
    
    /**
     * Create a new dashboard session
     */
    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        DashboardSession session = new DashboardSession(sessionId, username, System.currentTimeMillis());
        activeSessions.put(sessionId, session);
        LOGGER.info("Created dashboard session for user: {}", username);
        return sessionId;
    }
    
    /**
     * Validate a dashboard session
     */
    public boolean validateSession(String sessionId) {
        DashboardSession session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        // Check if session is expired (1 hour)
        long sessionAge = System.currentTimeMillis() - session.getCreatedTime();
        if (sessionAge > 3600000) { // 1 hour in milliseconds
            activeSessions.remove(sessionId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Remove a dashboard session
     */
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
    
    /**
     * Get active sessions count
     */
    public int getActiveSessionsCount() {
        return activeSessions.size();
    }
    
    /**
     * Check if dashboard is enabled
     */
    public boolean isDashboardEnabled() {
        return dashboardEnabled;
    }
    
    /**
     * Set dashboard port
     */
    public void setPort(int port) {
        if (!dashboardEnabled) {
            this.port = port;
            LOGGER.info("Dashboard port set to {}", port);
        } else {
            LOGGER.warn("Cannot change port while dashboard is running");
        }
    }
    
    /**
     * Get dashboard port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Dashboard session class
     */
    public static class DashboardSession {
        private final String sessionId;
        private final String username;
        private final long createdTime;
        
        public DashboardSession(String sessionId, String username, long createdTime) {
            this.sessionId = sessionId;
            this.username = username;
            this.createdTime = createdTime;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public long getCreatedTime() {
            return createdTime;
        }
    }
}
