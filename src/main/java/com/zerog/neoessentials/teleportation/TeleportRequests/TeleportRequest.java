package com.zerog.neoessentials.teleportation.TeleportRequests;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a teleportation request between two players
 */
public class TeleportRequest {
    private final UUID requesterId;
    private final String requesterName;
    private final UUID targetId;
    private final String targetName;
    private final TeleportRequestType type;
    private final long expiryTime;
    private final long createdTime;
    
    public TeleportRequest(UUID requesterId, String requesterName, 
                          UUID targetId, String targetName,
                          TeleportRequestType type, long expiryTime) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.type = type;
        this.expiryTime = expiryTime;
        this.createdTime = System.currentTimeMillis();
    }
    
    // Getters
    public UUID getRequesterId() {
        return requesterId;
    }
    
    public String getRequesterName() {
        return requesterName;
    }
    
    public UUID getTargetId() {
        return targetId;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public TeleportRequestType getType() {
        return type;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    /**
     * Check if the request has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
    
    /**
     * Get remaining time in seconds
     */
    public long getRemainingTimeSeconds() {
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Get age of request in seconds
     */
    public long getAgeSeconds() {
        return (System.currentTimeMillis() - createdTime) / 1000;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TeleportRequest that = (TeleportRequest) obj;
        return Objects.equals(requesterId, that.requesterId) &&
               Objects.equals(targetId, that.targetId) &&
               type == that.type &&
               createdTime == that.createdTime;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(requesterId, targetId, type, createdTime);
    }
    
    @Override
    public String toString() {
        return String.format("TeleportRequest{requester=%s, target=%s, type=%s, age=%ds, remaining=%ds}",
                           requesterName, targetName, type, getAgeSeconds(), getRemainingTimeSeconds());
    }
}