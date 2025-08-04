package com.zerog.neoessentials.systems.security;

/**
 * Security level enumeration
 */
public enum SecurityLevel {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");
    
    private final String displayName;
    
    SecurityLevel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
