package com.zerog.neoessentials.notifications.channels;

import com.zerog.neoessentials.notifications.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Log-based notification channel that outputs to server logs
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class LogNotificationChannel implements NotificationChannel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LogNotificationChannel.class);
    private static final String CHANNEL_NAME = "log";
    
    private boolean enabled = true;
    private final Set<NotificationEvent.Type> supportedTypes = EnumSet.allOf(NotificationEvent.Type.class);
    
    @Override
    public void sendNotification(NotificationEvent event) {
        if (!enabled) {
            return;
        }
        
        String logMessage = formatMessage(event);
        
        switch (event.getSeverity()) {
            case CRITICAL:
                LOGGER.error("[NOTIFICATION] {}", logMessage);
                break;
            case WARNING:
                LOGGER.warn("[NOTIFICATION] {}", logMessage);
                break;
            case INFO:
                LOGGER.info("[NOTIFICATION] {}", logMessage);
                break;
            case DEBUG:
                LOGGER.debug("[NOTIFICATION] {}", logMessage);
                break;
            default:
                LOGGER.info("[NOTIFICATION] {}", logMessage);
                break;
        }
    }
    
    private String formatMessage(NotificationEvent event) {
        StringBuilder message = new StringBuilder();
        message.append("[").append(event.getType()).append("] ");
        message.append(event.getTitle()).append(": ");
        message.append(event.getMessage());
        
        if (event.getPlayerName() != null && !event.getPlayerName().isEmpty()) {
            message.append(" [Player: ").append(event.getPlayerName()).append("]");
        }
        
        if (!event.getMetadata().isEmpty()) {
            message.append(" [Metadata: ").append(event.getMetadata()).append("]");
        }
        
        return message.toString();
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean supportsEventType(NotificationEvent.Type eventType) {
        return supportedTypes.contains(eventType);
    }
    
    @Override
    public void close() {
        // Nothing to close for log channel
        LOGGER.debug("Log notification channel closed");
    }
    
    @Override
    public String getName() {
        return CHANNEL_NAME;
    }
    
    /**
     * Add support for specific event type
     */
    public void addSupportedEventType(NotificationEvent.Type eventType) {
        supportedTypes.add(eventType);
    }
    
    /**
     * Remove support for specific event type
     */
    public void removeSupportedEventType(NotificationEvent.Type eventType) {
        supportedTypes.remove(eventType);
    }
    
    /**
     * Get all supported event types
     */
    public Set<NotificationEvent.Type> getSupportedEventTypes() {
        return EnumSet.copyOf(supportedTypes);
    }
}
