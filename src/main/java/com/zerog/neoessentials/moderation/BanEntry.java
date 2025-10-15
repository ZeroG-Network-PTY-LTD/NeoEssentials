package com.zerog.neoessentials.moderation;

import java.time.Instant;
import java.util.*;

/**
 * Represents a ban entry for a player or IP address.
 * Supports temporary/permanent bans, appeals, and ban history.
 */
public class BanEntry {
    private String id;
    private BanType type;
    private String target; // UUID for player bans, IP for IP bans
    private String playerName; // Display name (may be null for IP-only bans)
    private String reason;
    private String evidence; // Optional evidence/proof URLs or text
    private Instant bannedAt;
    private Instant expiresAt; // null for permanent bans
    private String bannedBy;
    private boolean isActive;
    private BanAppeal appeal;
    
    public enum BanType {
        UUID,    // Ban by player UUID
        IP,      // Ban by IP address
        BOTH     // Ban both UUID and IP
    }
    
    public static class BanAppeal {
        private String appealText;
        private Instant appealedAt;
        private AppealStatus status;
        private String reviewedBy;
        private Instant reviewedAt;
        private String reviewNotes;
        
        public enum AppealStatus {
            PENDING,   // Awaiting review
            APPROVED,  // Appeal accepted, ban lifted
            DENIED     // Appeal rejected
        }
        
        public BanAppeal() {
            this.appealedAt = Instant.now();
            this.status = AppealStatus.PENDING;
        }
        
        public String getAppealText() {
            return appealText;
        }
        
        public void setAppealText(String appealText) {
            this.appealText = appealText;
        }
        
        public Instant getAppealedAt() {
            return appealedAt;
        }
        
        public void setAppealedAt(Instant appealedAt) {
            this.appealedAt = appealedAt;
        }
        
        public AppealStatus getStatus() {
            return status;
        }
        
        public void setStatus(AppealStatus status) {
            this.status = status;
        }
        
        public String getReviewedBy() {
            return reviewedBy;
        }
        
        public void setReviewedBy(String reviewedBy) {
            this.reviewedBy = reviewedBy;
        }
        
        public Instant getReviewedAt() {
            return reviewedAt;
        }
        
        public void setReviewedAt(Instant reviewedAt) {
            this.reviewedAt = reviewedAt;
        }
        
        public String getReviewNotes() {
            return reviewNotes;
        }
        
        public void setReviewNotes(String reviewNotes) {
            this.reviewNotes = reviewNotes;
        }
    }
    
    public BanEntry() {
        this.id = UUID.randomUUID().toString();
        this.bannedAt = Instant.now();
        this.isActive = true;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public BanType getType() {
        return type;
    }
    
    public void setType(BanType type) {
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
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getEvidence() {
        return evidence;
    }
    
    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
    
    public Instant getBannedAt() {
        return bannedAt;
    }
    
    public void setBannedAt(Instant bannedAt) {
        this.bannedAt = bannedAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getBannedBy() {
        return bannedBy;
    }
    
    public void setBannedBy(String bannedBy) {
        this.bannedBy = bannedBy;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public BanAppeal getAppeal() {
        return appeal;
    }
    
    public void setAppeal(BanAppeal appeal) {
        this.appeal = appeal;
    }
    
    public boolean isPermanent() {
        return expiresAt == null;
    }
    
    public boolean isExpired() {
        return !isPermanent() && Instant.now().isAfter(expiresAt);
    }
    
    public boolean hasAppeal() {
        return appeal != null;
    }
    
    public boolean isAppealPending() {
        return hasAppeal() && appeal.getStatus() == BanAppeal.AppealStatus.PENDING;
    }
}
