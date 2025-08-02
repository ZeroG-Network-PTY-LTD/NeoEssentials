package com.zerog.neoessentials.systems.security;

import java.time.LocalDateTime;

/**
 * Security alert model
 */
public class SecurityAlert {
    private final String id;
    private final SecurityEventType type;
    private final SecurityLevel severity;
    private final String title;
    private final String description;
    private final String affectedUser;
    private final LocalDateTime timestamp;
    private final String source;
    private boolean resolved = false;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolution;
    
    public SecurityAlert(String id, SecurityEventType type, SecurityLevel severity,
                        String title, String description, String affectedUser, String source) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.affectedUser = affectedUser;
        this.source = source;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public SecurityEventType getType() { return type; }
    public SecurityLevel getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAffectedUser() { return affectedUser; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSource() { return source; }
    public boolean isResolved() { return resolved; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolution() { return resolution; }
    
    public void resolve(String resolvedBy, String resolution) {
        this.resolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
        this.resolution = resolution;
    }
}
