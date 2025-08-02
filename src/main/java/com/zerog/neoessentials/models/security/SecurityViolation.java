package com.zerog.neoessentials.models.security;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a security violation detected by the threat detection system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SecurityViolation {
    private final String violationType;
    private final String severity;
    private final String description;
    private final String source;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;
    private boolean resolved;
    private String resolutionAction;
    
    public SecurityViolation(String violationType, String severity, String description, 
                           String source, Map<String, Object> metadata) {
        this.violationType = violationType;
        this.severity = severity;
        this.description = description;
        this.source = source;
        this.metadata = metadata;
        this.timestamp = LocalDateTime.now();
        this.resolved = false;
    }
    
    // Getters
    public String getViolationType() { return violationType; }
    public String getType() { return violationType; } // Alias for compatibility
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
    public String getDetails() { return description; } // Alias for compatibility
    public String getSource() { return source; }
    public String getUser() { return source; } // Alias for compatibility
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    public boolean isResolved() { return resolved; }
    public String getResolutionAction() { return resolutionAction; }
    
    // Resolution methods
    public void resolve(String action) {
        this.resolved = true;
        this.resolutionAction = action;
    }
    
    public void unresolve() {
        this.resolved = false;
        this.resolutionAction = null;
    }
}
