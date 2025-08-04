package com.zerog.neoessentials.notifications.channels;

import com.zerog.neoessentials.notifications.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log-based notification channel that writes notifications to server logs
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class LogNotificationChannel implements NotificationChannel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("NeoEssentials-Notifications");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void sendNotification(NotificationEvent event) {
        String timestamp = DATE_FORMAT.format(new Date(event.getTimestamp()));
        String logMessage = String.format("[%s] [%s] %s: %s", 
                timestamp, 
                event.getSeverity().name(), 
                event.getTitle(), 
                event.getMessage());
        
        if (event.getPlayerName() != null && !event.getPlayerName().isEmpty()) {
            logMessage += " (Player: " + event.getPlayerName() + ")";
        }
        
        // Log at appropriate level based on severity
        switch (event.getSeverity()) {
            case INFO -> LOGGER.info(logMessage);
            case WARNING -> LOGGER.warn(logMessage);
            case CRITICAL -> LOGGER.error(logMessage);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return true; // Log channel is always enabled
    }
    
    @Override
    public boolean supportsEventType(NotificationEvent.Type eventType) {
        return true; // Log channel supports all event types
    }
    
    @Override
    public String getChannelName() {
        return "Log";
    }
}
