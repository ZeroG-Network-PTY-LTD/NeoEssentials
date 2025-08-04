package com.zerog.neoessentials.security;

/**
 * Security levels for categorizing the severity of security events
 */
public enum SecurityLevel {
    INFO("Info", 0),
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    WARNING("Warning", 3),
    HIGH("High", 4),
    CRITICAL("Critical", 5);

    private final String displayName;
    private final int severity;

    SecurityLevel(String displayName, int severity) {
        this.displayName = displayName;
        this.severity = severity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isHigherThan(SecurityLevel other) {
        return this.severity > other.severity;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
