package com.zerog.neoessentials.notifications.channels;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.zerog.neoessentials.notifications.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

/**
 * Discord webhook notification channel
 * Sends notifications to Discord channels via webhooks
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordNotificationChannel implements NotificationChannel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordNotificationChannel.class);
    
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final HttpClient httpClient;
    private final Set<NotificationEvent.Type> supportedEvents;
    
    public DiscordNotificationChannel(String webhookUrl, String username, String avatarUrl) {
        this.webhookUrl = webhookUrl;
        this.username = username != null ? username : "NeoEssentials";
        this.avatarUrl = avatarUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        // Support most event types for Discord
        this.supportedEvents = EnumSet.of(
                NotificationEvent.Type.PLAYER_JOIN,
                NotificationEvent.Type.PLAYER_LEAVE,
                NotificationEvent.Type.PLAYER_DEATH,
                NotificationEvent.Type.SERVER_START,
                NotificationEvent.Type.SERVER_STOP,
                NotificationEvent.Type.SECURITY_ALERT,
                NotificationEvent.Type.PERFORMANCE_ALERT,
                NotificationEvent.Type.ERROR_ALERT
        );
    }
    
    @Override
    public void sendNotification(NotificationEvent event) throws Exception {
        JsonObject payload = createDiscordPayload(event);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .header("User-Agent", "NeoEssentials/1.0")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.debug("Successfully sent Discord notification: {}", event.getTitle());
            } else {
                LOGGER.warn("Discord webhook returned status {}: {}", response.statusCode(), response.body());
                throw new IOException("Discord webhook failed with status: " + response.statusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send Discord notification", e);
            throw e;
        }
    }
    
    private JsonObject createDiscordPayload(NotificationEvent event) {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            payload.addProperty("avatar_url", avatarUrl);
        }
        
        // Create embed for rich formatting
        JsonObject embed = new JsonObject();
        embed.addProperty("title", event.getTitle());
        embed.addProperty("description", event.getMessage());
        embed.addProperty("timestamp", new Date(event.getTimestamp()).toInstant().toString());
        
        // Set color based on severity
        int color = switch (event.getSeverity()) {
            case INFO -> 0x00FF00;      // Green
            case WARNING -> 0xFFFF00;   // Yellow
            case CRITICAL -> 0xFF0000;  // Red
        };
        embed.addProperty("color", color);
        
        // Add fields for additional information
        JsonArray fields = new JsonArray();
        
        if (event.getPlayerName() != null && !event.getPlayerName().isEmpty()) {
            JsonObject playerField = new JsonObject();
            playerField.addProperty("name", "Player");
            playerField.addProperty("value", event.getPlayerName());
            playerField.addProperty("inline", true);
            fields.add(playerField);
        }
        
        JsonObject typeField = new JsonObject();
        typeField.addProperty("name", "Type");
        typeField.addProperty("value", event.getType().name().replace("_", " "));
        typeField.addProperty("inline", true);
        fields.add(typeField);
        
        JsonObject severityField = new JsonObject();
        severityField.addProperty("name", "Severity");
        severityField.addProperty("value", event.getSeverity().name());
        severityField.addProperty("inline", true);
        fields.add(severityField);
        
        // Add metadata fields if present
        for (String key : event.getMetadata().keySet()) {
            String value = event.getMetadata().get(key);
            if (value != null && !value.isEmpty()) {
                JsonObject metaField = new JsonObject();
                metaField.addProperty("name", key.substring(0, 1).toUpperCase() + key.substring(1));
                metaField.addProperty("value", value);
                metaField.addProperty("inline", true);
                fields.add(metaField);
            }
        }
        
        embed.add("fields", fields);
        
        // Add footer with server info
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "NeoEssentials Server Monitor");
        embed.add("footer", footer);
        
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
        
        return payload;
    }
    
    @Override
    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }
    
    @Override
    public boolean supportsEventType(NotificationEvent.Type eventType) {
        return supportedEvents.contains(eventType);
    }
    
    @Override
    public String getChannelName() {
        return "Discord";
    }
    
    @Override
    public void close() throws Exception {
        // HttpClient doesn't need explicit closing in Java 11+
        LOGGER.debug("Discord notification channel closed");
    }
}
