package com.zerog.neoessentials.notifications.channels;

import com.zerog.neoessentials.notifications.NotificationEvent;

/**
 * Interface for notification channels (Discord, Email, SMS, etc.)
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public interface NotificationChannel {
    
    /**
     * Send a notification through this channel
     * 
     * @param event The notification event to send
     * @throws Exception if the notification fails to send
     */
    void sendNotification(NotificationEvent event) throws Exception;
    
    /**
     * Check if this channel is enabled and ready to send notifications
     * 
     * @return true if the channel is enabled
     */
    boolean isEnabled();
    
    /**
     * Check if this channel supports the given event type
     * 
     * @param eventType The event type to check
     * @return true if the channel supports this event type
     */
    boolean supportsEventType(NotificationEvent.Type eventType);
    
    /**
     * Get the name of this notification channel
     * 
     * @return The channel name
     */
    String getChannelName();
    
    /**
     * Close the channel and cleanup resources
     */
    default void close() throws Exception {
        // Default implementation does nothing
    }
}
