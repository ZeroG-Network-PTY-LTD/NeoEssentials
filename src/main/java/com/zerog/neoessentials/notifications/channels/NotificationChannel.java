package com.zerog.neoessentials.notifications.channels;

import com.zerog.neoessentials.notifications.NotificationEvent;

/**
 * Interface for notification channels that can deliver notifications
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public interface NotificationChannel {
    
    /**
     * Send a notification through this channel
     * @param event The notification event to send
     */
    void sendNotification(NotificationEvent event);
    
    /**
     * Check if this channel is enabled
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
    
    /**
     * Check if this channel supports the given event type
     * @param eventType The event type to check
     * @return true if supported, false otherwise
     */
    boolean supportsEventType(NotificationEvent.Type eventType);
    
    /**
     * Close the channel and cleanup resources
     */
    void close();
    
    /**
     * Get the channel name
     * @return The name of this channel
     */
    String getName();
}
