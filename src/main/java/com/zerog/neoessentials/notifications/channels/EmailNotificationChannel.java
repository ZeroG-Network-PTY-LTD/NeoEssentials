package com.zerog.neoessentials.notifications.channels;

import com.zerog.neoessentials.notifications.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Email notification channel (placeholder implementation)
 * Requires javax.mail dependency for full functionality
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EmailNotificationChannel implements NotificationChannel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationChannel.class);
    
    private final String smtpHost;
    private final int smtpPort;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final List<String> toAddresses;
    private final boolean useTLS;
    private final Set<NotificationEvent.Type> supportedEvents;
    
    public EmailNotificationChannel(String smtpHost, int smtpPort, String username, String password,
                                  String fromAddress, List<String> toAddresses, boolean useTLS) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.toAddresses = new ArrayList<>(toAddresses);
        this.useTLS = useTLS;
        
        // Email is typically used for critical events only
        this.supportedEvents = EnumSet.of(
                NotificationEvent.Type.SERVER_START,
                NotificationEvent.Type.SERVER_STOP,
                NotificationEvent.Type.SECURITY_ALERT,
                NotificationEvent.Type.PERFORMANCE_ALERT,
                NotificationEvent.Type.ERROR_ALERT
        );
        
        LOGGER.warn("Email notifications configured but javax.mail dependency not available. " +
                   "Email notifications will be logged instead.");
    }
    
    @Override
    public void sendNotification(NotificationEvent event) throws Exception {
        // For now, just log the email that would be sent
        LOGGER.info("EMAIL NOTIFICATION [{}]: {} - {}", 
                event.getSeverity().name(), 
                event.getTitle(), 
                event.getMessage());
        
        if (event.getPlayerName() != null && !event.getPlayerName().isEmpty()) {
            LOGGER.info("  Player: {}", event.getPlayerName());
        }
        
        LOGGER.info("  Would send to: {}", toAddresses);
        LOGGER.info("  SMTP: {}:{} (TLS: {})", smtpHost, smtpPort, useTLS);
        
        // TODO: Implement actual email sending when javax.mail is available
        // This would require adding javax.mail dependency to build.gradle
    }
    
    @Override
    public boolean isEnabled() {
        return smtpHost != null && !smtpHost.isEmpty() && 
               fromAddress != null && !fromAddress.isEmpty() &&
               !toAddresses.isEmpty();
    }
    
    @Override
    public boolean supportsEventType(NotificationEvent.Type eventType) {
        return supportedEvents.contains(eventType);
    }
    
    @Override
    public String getChannelName() {
        return "Email";
    }
    
    @Override
    public void close() throws Exception {
        LOGGER.debug("Email notification channel closed");
    }
}
