package com.zerog.neoessentials.systems.security;

/**
 * Security level classification
 */
public enum SecurityLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    CRITICAL(5);
    
    private final int priority;
    
    SecurityLevel(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() { return priority; }
    
    public boolean isMoreSevereThan(SecurityLevel other) {
        return this.priority > other.priority;
    }
}
