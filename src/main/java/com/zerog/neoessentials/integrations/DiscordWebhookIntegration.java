package com.zerog.neoessentials.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Discord Webhook Integration for NeoEssentials
 * Provides simple webhook notifications to Discord channels
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class DiscordWebhookIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordWebhookIntegration.class);
    private static DiscordWebhookIntegration instance;
    
    private final Executor webhookExecutor;
    private String webhookUrl;
    private boolean enabled;
    
    private DiscordWebhookIntegration() {
        this.webhookExecutor = Executors.newFixedThreadPool(2);
        loadConfiguration();
    }
    
    public static DiscordWebhookIntegration getInstance() {
        if (instance == null) {
            instance = new DiscordWebhookIntegration();
        }
        return instance;
    }
    
    /**
     * Load Discord webhook configuration
     */
    private void loadConfiguration() {
        try {
            // Load configuration from ConfigManager
            com.zerog.neoessentials.config.ConfigManager configManager = 
                com.zerog.neoessentials.config.ConfigManager.getInstance();
            com.zerog.neoessentials.config.DiscordConfig discordConfig = configManager.getDiscordConfig();
            
            if (discordConfig.webhooks.enabled && !discordConfig.webhooks.chatWebhookUrl.isEmpty()) {
                this.enabled = true;
                this.webhookUrl = discordConfig.webhooks.chatWebhookUrl;
                LOGGER.info("Discord webhook integration loaded from configuration - ENABLED");
            } else {
                this.enabled = false;
                this.webhookUrl = "";
                LOGGER.info("Discord webhook integration initialized (disabled - no webhook configured)");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load Discord webhook configuration", e);
            this.enabled = false;
        }
    }
    
    /**
     * Send server start notification
     */
    public void sendServerStartNotification() {
        if (!enabled) return;
        
        String message = "🟢 **Server Started**\n" +
                        "The Minecraft server is now online and ready for players!";
        
        sendWebhookMessage(message, "Server", 3066993); // Green color
    }
    
    /**
     * Send server stop notification  
     */
    public void sendServerStopNotification() {
        if (!enabled) return;
        
        String message = "🔴 **Server Stopped**\n" +
                        "The Minecraft server has been shut down.";
        
        sendWebhookMessage(message, "Server", 15158332); // Red color
    }
    
    /**
     * Send player join notification
     */
    public void sendPlayerJoinNotification(String playerName) {
        if (!enabled) return;
        
        String message = "👋 **Player Joined**\n" +
                        "**" + playerName + "** has joined the server!";
        
        sendWebhookMessage(message, "Player Events", 65280); // Lime color
    }
    
    /**
     * Send player leave notification
     */
    public void sendPlayerLeaveNotification(String playerName) {
        if (!enabled) return;
        
        String message = "👋 **Player Left**\n" +
                        "**" + playerName + "** has left the server.";
        
        sendWebhookMessage(message, "Player Events", 16776960); // Yellow color
    }
    
    /**
     * Send admin command notification
     */
    public void sendAdminCommandNotification(String adminName, String command) {
        if (!enabled) return;
        
        String message = "⚡ **Admin Command**\n" +
                        "**" + adminName + "** executed: `" + command + "`";
        
        sendWebhookMessage(message, "Admin Actions", 16711680); // Blue color
    }
    
    /**
     * Send ban notification
     */
    public void sendBanNotification(String adminName, String playerName, String reason) {
        if (!enabled) return;
        
        String message = "🔨 **Player Banned**\n" +
                        "**" + playerName + "** was banned by **" + adminName + "**\n" +
                        "Reason: " + (reason != null ? reason : "No reason provided");
        
        sendWebhookMessage(message, "Moderation", 15158332); // Red color
    }
    
    /**
     * Send custom notification
     */
    public void sendCustomNotification(String title, String message, int color) {
        if (!enabled) return;
        
        sendWebhookMessage("📢 **" + title + "**\n" + message, "Custom", color);
    }
    
    /**
     * Send webhook message asynchronously
     */
    private void sendWebhookMessage(String content, String username, int color) {
        CompletableFuture.runAsync(() -> {
            try {
                sendWebhookMessageSync(content, username, color);
            } catch (Exception e) {
                LOGGER.error("Failed to send Discord webhook message", e);
            }
        }, webhookExecutor);
    }
    
    /**
     * Send webhook message synchronously
     */
    private void sendWebhookMessageSync(String content, String username, int color) throws IOException {
        URL url = new URL(webhookUrl); // Using deprecated constructor for Minecraft mod compatibility
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "NeoEssentials-Webhook/1.0");
            connection.setDoOutput(true);
            
            String jsonPayload = createWebhookPayload(content, username, color);
            
            try (var outputStream = connection.getOutputStream()) {
                outputStream.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 204) {
                LOGGER.debug("Discord webhook message sent successfully");
            } else {
                LOGGER.warn("Discord webhook returned unexpected response code: {}", responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Create JSON payload for Discord webhook
     */
    private String createWebhookPayload(String content, String username, int color) {
        String timestamp = Instant.now().toString();
        
        return String.format("""
            {
                "username": "%s",
                "avatar_url": "https://via.placeholder.com/64x64/5865F2/FFFFFF?text=NE",
                "embeds": [{
                    "description": "%s",
                    "color": %d,
                    "timestamp": "%s",
                    "footer": {
                        "text": "NeoEssentials Server",
                        "icon_url": "https://via.placeholder.com/16x16/5865F2/FFFFFF?text=NE"
                    }
                }]
            }
            """, 
            escapeJson(username), 
            escapeJson(content), 
            color, 
            timestamp
        );
    }
    
    /**
     * Escape JSON special characters
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Test webhook connection
     */
    public boolean testWebhook() {
        if (!enabled) {
            LOGGER.info("Discord webhook is disabled");
            return false;
        }
        
        try {
            sendWebhookMessageSync("🔧 **Webhook Test**\nThis is a test message from NeoEssentials!", 
                                 "Test", 
                                 5793266); // Purple color
            LOGGER.info("Discord webhook test successful");
            return true;
        } catch (Exception e) {
            LOGGER.error("Discord webhook test failed", e);
            return false;
        }
    }
    
    /**
     * Update webhook configuration
     */
    public void updateConfiguration(String newWebhookUrl, boolean enabled) {
        this.webhookUrl = newWebhookUrl;
        this.enabled = enabled && newWebhookUrl != null && !newWebhookUrl.trim().isEmpty();
        
        if (this.enabled) {
            LOGGER.info("Discord webhook configuration updated and enabled");
        } else {
            LOGGER.info("Discord webhook disabled");
        }
    }
    
    /**
     * Reload configuration from ConfigManager
     */
    public void reloadConfiguration() {
        loadConfiguration();
    }
    
    /**
     * Check if webhook integration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get current webhook URL (masked for security)
     */
    public String getMaskedWebhookUrl() {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return "Not configured";
        }
        
        // Show only the last 10 characters for security
        if (webhookUrl.length() > 10) {
            return "***" + webhookUrl.substring(webhookUrl.length() - 10);
        }
        return "***";
    }
}
