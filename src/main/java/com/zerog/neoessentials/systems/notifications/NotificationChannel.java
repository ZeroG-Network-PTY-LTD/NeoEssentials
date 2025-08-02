package com.zerog.neoessentials.systems.notifications;

/**
 * Channels through which notifications can be delivered
 */
public enum NotificationChannel {
    CONSOLE,
    CHAT,
    DISCORD,
    EMAIL,
    SMS,
    FILE_LOG,
    FILE, // Alias for FILE_LOG
    DATABASE,
    WEBHOOK,
    CUSTOM
}
