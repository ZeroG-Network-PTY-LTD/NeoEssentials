package com.zerog.neoessentials.systems.monitoring;

/**
 * Alert severity levels
 */
public enum AlertSeverity {
    LOW("Low priority issue"),
    WARNING("Warning - attention recommended"),
    MEDIUM("Medium priority issue"),
    HIGH("High priority issue"),
    CRITICAL("Critical issue requiring immediate attention"),
    EMERGENCY("Emergency - system may be compromised");
    
    private final String description;
    
    AlertSeverity(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return switch (this) {
            case LOW -> 1;
            case WARNING -> 2;
            case MEDIUM -> 3;
            case HIGH -> 4;
            case CRITICAL -> 5;
            case EMERGENCY -> 6;
        };
    }
}
