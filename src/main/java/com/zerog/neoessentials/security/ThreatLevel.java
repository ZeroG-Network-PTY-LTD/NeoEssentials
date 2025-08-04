package com.zerog.neoessentials.security;

/**
 * Threat levels for security classification
 */
public enum ThreatLevel {
    NONE("None", 0),
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4),
    EXTREME("Extreme", 5);

    private final String displayName;
    private final int level;

    ThreatLevel(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(ThreatLevel other) {
        return this.level > other.level;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
