package com.zerog.neoessentials.systems.notifications;

/**
 * Priority levels for notifications
 */
public enum NotificationPriority {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    URGENT(4),
    CRITICAL(5);
    
    private final int level;
    
    NotificationPriority(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(NotificationPriority other) {
        return this.level > other.level;
    }
}
