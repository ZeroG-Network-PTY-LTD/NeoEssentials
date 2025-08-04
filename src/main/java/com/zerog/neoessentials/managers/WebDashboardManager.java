package com.zerog.neoessentials.managers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web Dashboard Manager for NeoEssentials
 * Handles web-based administration interface
 */
public class WebDashboardManager {
    private static WebDashboardManager instance;
    private boolean enabled = false;
    private int port = 8080;
    private String bindAddress = "localhost";
    private final Map<String, Object> dashboardData = new ConcurrentHashMap<>();
    private final Set<String> authenticatedSessions = ConcurrentHashMap.newKeySet();

    private WebDashboardManager() {}

    public static WebDashboardManager getInstance() {
        if (instance == null) {
            instance = new WebDashboardManager();
        }
        return instance;
    }

    public void initialize() {
        // Initialize web dashboard
        enabled = true;
    }

    public void shutdown() {
        enabled = false;
        authenticatedSessions.clear();
    }

    public boolean isRunning() {
        return enabled;
    }

    public WebDashboardManager getWebDashboard() {
        return this;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public Map<String, Object> getDashboardData() {
        return new HashMap<>(dashboardData);
    }

    public void updateDashboardData(String key, Object value) {
        dashboardData.put(key, value);
    }

    public boolean authenticateSession(String sessionId, String credentials) {
        // Simple authentication for demo purposes
        if (credentials != null && credentials.equals("admin:password")) {
            authenticatedSessions.add(sessionId);
            return true;
        }
        return false;
    }

    public boolean isSessionAuthenticated(String sessionId) {
        return authenticatedSessions.contains(sessionId);
    }

    public void invalidateSession(String sessionId) {
        authenticatedSessions.remove(sessionId);
    }

    public Map<String, Object> getServerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", enabled);
        stats.put("port", port);
        stats.put("bind_address", bindAddress);
        stats.put("active_sessions", authenticatedSessions.size());
        stats.put("dashboard_entries", dashboardData.size());
        return stats;
    }

    public void registerEndpoint(String path, Object handler) {
        // Register web endpoint handler
        dashboardData.put("endpoint_" + path, handler);
    }

    public void unregisterEndpoint(String path) {
        dashboardData.remove("endpoint_" + path);
    }
}
