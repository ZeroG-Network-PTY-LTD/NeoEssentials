package com.zerog.neoessentials.moderation;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a whitelist entry for a player or IP address.
 */
public class WhitelistEntry {
    private String id;
    private WhitelistType type;
    private String target; // UUID for player whitelist, IP for IP whitelist
    private String playerName; // Display name
    private String addedBy;
    private Instant addedAt;
    private String notes;
    
    public enum WhitelistType {
        UUID,  // Whitelist by player UUID
        IP     // Whitelist by IP address
    }
    
    public WhitelistEntry() {
        this.id = UUID.randomUUID().toString();
        this.addedAt = Instant.now();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public WhitelistType getType() {
        return type;
    }
    
    public void setType(WhitelistType type) {
        this.type = type;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getAddedBy() {
        return addedBy;
    }
    
    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }
    
    public Instant getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
