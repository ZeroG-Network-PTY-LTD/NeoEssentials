package com.zerog.neoessentials.discord;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Discord Integration Manager for NeoEssentials
 * Handles Discord webhook integration and player account linking
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManager.class);
    private static DiscordManager instance;
    
    private final HttpClient httpClient;
    private final Map<String, String> linkedAccounts; // UUID -> Discord ID
    private String webhookUrl;
    private boolean enabled;
    private String serverName;
    private String serverIcon;
    
    private DiscordManager() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.linkedAccounts = new HashMap<>();
        this.enabled = false;
        this.serverName = "NeoEssentials Server";
        this.serverIcon = "https://i.imgur.com/default.png";
    }
    
    public static DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }
    
    /**
     * Initialize Discord integration
     */
    public void initialize(String webhookUrl, String serverName, String serverIcon) {
        this.webhookUrl = webhookUrl;
        this.serverName = serverName;
        this.serverIcon = serverIcon;
        this.enabled = webhookUrl != null && !webhookUrl.isEmpty();
        
        if (enabled) {
            LOGGER.info("Discord integration initialized with webhook");
        } else {
            LOGGER.warn("Discord integration disabled - no webhook URL configured");
        }
    }
    
    /**
     * Send a message to Discord
     */
    public CompletableFuture<Boolean> sendMessage(String content) {
        return sendEmbed(null, content, null, null);
    }
    
    /**
     * Send a rich embed to Discord
     */
    public CompletableFuture<Boolean> sendEmbed(String title, String description, String color, String thumbnailUrl) {
        if (!enabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonPayload = buildEmbedPayload(title, description, color, thumbnailUrl);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return true;
                } else {
                    LOGGER.error("Discord webhook failed with status: {}", response.statusCode());
                    return false;
                }
                
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Failed to send Discord message", e);
                return false;
            }
        });
    }
    
    /**
     * Send player join notification
     */
    public void notifyPlayerJoin(ServerPlayer player) {
        if (!enabled) return;
        
        sendEmbed(
            "Player Joined",
            "**" + player.getDisplayName().getString() + "** joined the server",
            "0x00ff00", // Green
            "https://crafatar.com/avatars/" + player.getUUID() + "?size=64"
        );
    }
    
    /**
     * Send player leave notification
     */
    public void notifyPlayerLeave(ServerPlayer player) {
        if (!enabled) return;
        
        sendEmbed(
            "Player Left",
            "**" + player.getDisplayName().getString() + "** left the server",
            "0xff0000", // Red
            "https://crafatar.com/avatars/" + player.getUUID() + "?size=64"
        );
    }
    
    /**
     * Send server start notification
     */
    public void notifyServerStart() {
        if (!enabled) return;
        
        sendEmbed(
            "Server Started",
            "🟢 **" + serverName + "** is now online!",
            "0x00ff00", // Green
            serverIcon
        );
    }
    
    /**
     * Send server stop notification
     */
    public void notifyServerStop() {
        if (!enabled) return;
        
        sendEmbed(
            "Server Stopped",
            "🔴 **" + serverName + "** is now offline",
            "0xff0000", // Red
            serverIcon
        );
    }
    
    /**
     * Send broadcast message to Discord
     */
    public void broadcastToDiscord(String message, ServerPlayer sender) {
        if (!enabled) return;
        
        String playerName = sender != null ? sender.getDisplayName().getString() : "Server";
        String avatarUrl = sender != null ? 
            "https://crafatar.com/avatars/" + sender.getUUID() + "?size=64" : 
            serverIcon;
        
        sendEmbed(
            "Server Broadcast",
            "📢 **" + playerName + "**: " + message,
            "0xffff00", // Yellow
            avatarUrl
        );
    }
    
    /**
     * Link a player's account to Discord
     */
    public void linkAccount(String playerUuid, String discordId) {
        linkedAccounts.put(playerUuid, discordId);
        LOGGER.info("Linked player {} to Discord ID {}", playerUuid, discordId);
    }
    
    /**
     * Unlink a player's account from Discord
     */
    public void unlinkAccount(String playerUuid) {
        String discordId = linkedAccounts.remove(playerUuid);
        if (discordId != null) {
            LOGGER.info("Unlinked player {} from Discord ID {}", playerUuid, discordId);
        }
    }
    
    /**
     * Check if a player is linked to Discord
     */
    public boolean isLinked(String playerUuid) {
        return linkedAccounts.containsKey(playerUuid);
    }
    
    /**
     * Get Discord ID for a player
     */
    public String getDiscordId(String playerUuid) {
        return linkedAccounts.get(playerUuid);
    }
    
    /**
     * Get player UUID for a Discord ID
     */
    public String getPlayerUuid(String discordId) {
        return linkedAccounts.entrySet().stream()
            .filter(entry -> entry.getValue().equals(discordId))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Build JSON payload for Discord embed
     */
    private String buildEmbedPayload(String title, String description, String color, String thumbnailUrl) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (title != null || description != null || color != null || thumbnailUrl != null) {
            json.append("\"embeds\":[{");
            
            if (title != null) {
                json.append("\"title\":\"").append(escapeJson(title)).append("\",");
            }
            
            if (description != null) {
                json.append("\"description\":\"").append(escapeJson(description)).append("\",");
            }
            
            if (color != null) {
                try {
                    int colorInt = Integer.parseInt(color.replace("0x", ""), 16);
                    json.append("\"color\":").append(colorInt).append(",");
                } catch (NumberFormatException e) {
                    // Ignore invalid color
                }
            }
            
            if (thumbnailUrl != null) {
                json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(thumbnailUrl)).append("\"},");
            }
            
            json.append("\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\"");
            json.append("}]");
        } else {
            json.append("\"content\":\"").append(escapeJson(description != null ? description : "")).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Get webhook URL
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    /**
     * Check if Discord integration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get configuration status
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("webhook_configured", webhookUrl != null && !webhookUrl.isEmpty());
        status.put("linked_accounts", linkedAccounts.size());
        status.put("server_name", serverName);
        return status;
    }
    
    /**
     * Shutdown Discord manager
     */
    public void shutdown() {
        // Send shutdown notification if enabled
        if (enabled) {
            try {
                notifyServerStop();
                Thread.sleep(1000); // Give time for message to send
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Clear linked accounts
        linkedAccounts.clear();
        LOGGER.info("Discord manager shutdown complete");
    }
}
