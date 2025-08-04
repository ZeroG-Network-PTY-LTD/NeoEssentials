package com.zerog.neoessentials.player;

import java.util.UUID;

/**
 * Admin note data structure for player records
 * Stores administrative comments and observations about players
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class AdminNote {
    
    private String noteId;
    private UUID authorUUID;
    private String authorName;
    private String content;
    private long timestamp;
    private String category;
    private String severity;
    private boolean isPrivate;
    private long expirationTime; // 0 means never expires
    
    public AdminNote() {
        this.timestamp = System.currentTimeMillis();
        this.category = "general";
        this.severity = "info";
        this.isPrivate = false;
        this.expirationTime = 0L;
    }
    
    public AdminNote(String noteId, UUID authorUUID, String authorName, String content) {
        this();
        this.noteId = noteId;
        this.authorUUID = authorUUID;
        this.authorName = authorName;
        this.content = content;
    }
    
    public AdminNote(String noteId, UUID authorUUID, String authorName, String content, 
                    String category, String severity) {
        this(noteId, authorUUID, authorName, content);
        this.category = category;
        this.severity = severity;
    }
    
    // Getters and Setters
    public String getNoteId() {
        return noteId;
    }
    
    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }
    
    public UUID getAuthorUUID() {
        return authorUUID;
    }
    
    public void setAuthorUUID(UUID authorUUID) {
        this.authorUUID = authorUUID;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public boolean isPrivate() {
        return isPrivate;
    }
    
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
    
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }
    
    // Utility methods
    public boolean isExpired() {
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }
    
    public void setExpirationDays(int days) {
        if (days > 0) {
            this.expirationTime = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L);
        } else {
            this.expirationTime = 0L;
        }
    }
    
    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }
    
    public String getFormattedExpiration() {
        if (expirationTime <= 0) {
            return "Never";
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(expirationTime));
    }
    
    public String getSeverityColor() {
        return switch (severity.toLowerCase()) {
            case "critical" -> "§c";
            case "warning" -> "§e";
            case "info" -> "§b";
            case "positive" -> "§a";
            default -> "§7";
        };
    }
    
    public String getCategoryIcon() {
        return switch (category.toLowerCase()) {
            case "behavior" -> "⚠";
            case "moderation" -> "🛡";
            case "performance" -> "⚡";
            case "technical" -> "🔧";
            case "positive" -> "✅";
            case "general" -> "📝";
            default -> "📄";
        };
    }
    
    @Override
    public String toString() {
        return String.format("AdminNote{id=%s, author=%s, category=%s, severity=%s, content='%s'}", 
            noteId, authorName, category, severity, 
            content.length() > 50 ? content.substring(0, 47) + "..." : content);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AdminNote adminNote = (AdminNote) obj;
        return noteId != null ? noteId.equals(adminNote.noteId) : adminNote.noteId == null;
    }
    
    @Override
    public int hashCode() {
        return noteId != null ? noteId.hashCode() : 0;
    }
}
