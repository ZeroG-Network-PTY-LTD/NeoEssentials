package com.zerog.neoessentials.economy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a connection to another server's economy
 */
public class EconomyServer {
    private final String serverId;
    private final String serverName;
    private final String connectionType;
    private final Map<String, String> connectionProperties;
    
    private boolean connected;
    private LocalDateTime lastSync;
    private BigDecimal syncedAmount;
    private int failedConnections;
    private String status;
    
    public EconomyServer(String serverId, String serverName, String connectionType) {
        this.serverId = serverId;
        this.serverName = serverName;
        this.connectionType = connectionType;
        this.connectionProperties = new HashMap<>();
        this.connected = false;
        this.lastSync = null;
        this.syncedAmount = BigDecimal.ZERO;
        this.failedConnections = 0;
        this.status = "Disconnected";
    }
    
    // Getters and setters
    public String getServerId() { return serverId; }
    public String getServerName() { return serverName; }
    public String getConnectionType() { return connectionType; }
    public Map<String, String> getConnectionProperties() { return new HashMap<>(connectionProperties); }
    
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { 
        this.connected = connected;
        this.status = connected ? "Connected" : "Disconnected";
    }
    
    public LocalDateTime getLastSync() { return lastSync; }
    public void setLastSync(LocalDateTime lastSync) { this.lastSync = lastSync; }
    
    public BigDecimal getSyncedAmount() { return syncedAmount; }
    public void setSyncedAmount(BigDecimal syncedAmount) { this.syncedAmount = syncedAmount; }
    
    public int getFailedConnections() { return failedConnections; }
    public void incrementFailedConnections() { this.failedConnections++; }
    public void resetFailedConnections() { this.failedConnections = 0; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public void setConnectionProperty(String key, String value) {
        connectionProperties.put(key, value);
    }
    
    public String getConnectionProperty(String key) {
        return connectionProperties.get(key);
    }
}
