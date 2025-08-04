package com.zerog.neoessentials.discord;

import com.zerog.neoessentials.error.ErrorHandler;
import com.zerog.neoessentials.exception.NeoEssentialsExceptions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Discord integration with rich embeds and advanced features
 * Extends the basic webhook functionality with professional Discord features
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordEnhancedIntegration {
    
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    
    // Get configuration from DiscordManager singleton
    private static String getWebhookUrl() {
        return DiscordManager.getInstance().getWebhookUrl();
    }
    
    private static boolean isEnabled() {
        return DiscordManager.getInstance().isEnabled();
    }
    
    /**
     * Discord embed builder for rich message formatting
     */
    public static class EmbedBuilder {
        private final Map<String, Object> embed = new HashMap<>();
        
        public EmbedBuilder setTitle(String title) {
            embed.put("title", title);
            return this;
        }
        
        public EmbedBuilder setDescription(String description) {
            embed.put("description", description);
            return this;
        }
        
        public EmbedBuilder setColor(Color color) {
            embed.put("color", color.getRGB() & 0xFFFFFF);
            return this;
        }
        
        public EmbedBuilder setColor(int color) {
            embed.put("color", color);
            return this;
        }
        
        public EmbedBuilder addField(String name, String value, boolean inline) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) embed.get("fields");
            if (fields == null) {
                fields = new java.util.ArrayList<>();
                embed.put("fields", fields);
            }
            
            Map<String, Object> field = new HashMap<>();
            field.put("name", name);
            field.put("value", value);
            field.put("inline", inline);
            fields.add(field);
            
            return this;
        }
        
        public EmbedBuilder setFooter(String text, String iconUrl) {
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", text);
            if (iconUrl != null) {
                footer.put("icon_url", iconUrl);
            }
            embed.put("footer", footer);
            return this;
        }
        
        public EmbedBuilder setTimestamp(Instant timestamp) {
            embed.put("timestamp", timestamp.toString());
            return this;
        }
        
        public EmbedBuilder setThumbnail(String url) {
            Map<String, Object> thumbnail = new HashMap<>();
            thumbnail.put("url", url);
            embed.put("thumbnail", thumbnail);
            return this;
        }
        
        public EmbedBuilder setAuthor(String name, String url, String iconUrl) {
            Map<String, Object> author = new HashMap<>();
            author.put("name", name);
            if (url != null) author.put("url", url);
            if (iconUrl != null) author.put("icon_url", iconUrl);
            embed.put("author", author);
            return this;
        }
        
        public Map<String, Object> build() {
            return new HashMap<>(embed);
        }
    }
    
    /**
     * Send rich embed message to Discord
     */
    public static CompletableFuture<Boolean> sendEmbed(EmbedBuilder embedBuilder, String username, String avatarUrl) {
        String webhookUrl = getWebhookUrl();
        if (!isEnabled() || webhookUrl == null || webhookUrl.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                if (username != null) payload.put("username", username);
                if (avatarUrl != null) payload.put("avatar_url", avatarUrl);
                payload.put("embeds", List.of(embedBuilder.build()));
                
                String jsonPayload = new com.google.gson.Gson().toJson(payload);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() >= 200 && response.statusCode() < 300;
                
            } catch (Exception e) {
                ErrorHandler.handleSystemError("Discord Enhanced Integration", "send embed", e);
                return false;
            }
        });
    }
    
    /**
     * Send player statistics to Discord
     */
    public static void sendPlayerStats(ServerPlayer player) {
        if (!isEnabled()) return;
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📊 Player Statistics")
            .setDescription("Player performance overview")
            .setColor(new Color(52, 152, 219)) // Blue
            .setThumbnail("https://mc-heads.net/avatar/" + player.getUUID())
            .addField("Player", player.getName().getString(), true)
            .addField("Play Time", formatPlayTime(getPlayTime(player)), true)
            .addField("Level", String.valueOf(player.experienceLevel), true)
            .addField("Health", String.format("%.1f/%.1f", player.getHealth(), player.getMaxHealth()), true)
            .addField("Position", String.format("X: %.0f, Y: %.0f, Z: %.0f", 
                player.getX(), player.getY(), player.getZ()), true)
            .addField("Dimension", player.level().dimension().location().toString(), true)
            .setFooter("NeoEssentials Stats", null)
            .setTimestamp(Instant.now());
        
        sendEmbed(embed, "NeoEssentials", null);
    }
    
    /**
     * Send economy report to Discord
     */
    public static void sendEconomyReport(Map<String, Object> economyData) {
        if (!isEnabled()) return;
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("💰 Economy Report")
            .setDescription("Server economy overview")
            .setColor(new Color(46, 204, 113)) // Green
            .addField("Total Money in Circulation", 
                String.format("$%.2f", economyData.getOrDefault("totalMoney", 0.0)), true)
            .addField("Average Player Balance", 
                String.format("$%.2f", economyData.getOrDefault("averageBalance", 0.0)), true)
            .addField("Richest Player", 
                String.valueOf(economyData.getOrDefault("richestPlayer", "Unknown")), true)
            .addField("Recent Transactions", 
                String.valueOf(economyData.getOrDefault("recentTransactions", 0)), true)
            .addField("Active Players", 
                String.valueOf(economyData.getOrDefault("activePlayers", 0)), true)
            .addField("Economy Health", 
                String.valueOf(economyData.getOrDefault("economyHealth", "Good")), true)
            .setFooter("NeoEssentials Economy", null)
            .setTimestamp(Instant.now());
        
        sendEmbed(embed, "NeoEssentials Economy", null);
    }
    
    /**
     * Send server status update to Discord
     */
    public static void sendServerStatus(String status, Map<String, Object> serverInfo) {
        if (!isEnabled()) return;
        
        Color statusColor = switch (status.toLowerCase()) {
            case "starting" -> new Color(241, 196, 15); // Yellow
            case "online" -> new Color(46, 204, 113);   // Green
            case "stopping" -> new Color(230, 126, 34); // Orange
            case "offline" -> new Color(231, 76, 60);   // Red
            default -> new Color(149, 165, 166);        // Gray
        };
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🖥️ Server Status Update")
            .setDescription("Server status change notification")
            .setColor(statusColor)
            .addField("Status", status.toUpperCase(), true)
            .addField("Players Online", 
                String.valueOf(serverInfo.getOrDefault("playersOnline", 0)), true)
            .addField("Max Players", 
                String.valueOf(serverInfo.getOrDefault("maxPlayers", 0)), true)
            .addField("Server Version", 
                String.valueOf(serverInfo.getOrDefault("version", "Unknown")), true)
            .addField("Uptime", 
                String.valueOf(serverInfo.getOrDefault("uptime", "Unknown")), true)
            .addField("Memory Usage", 
                String.valueOf(serverInfo.getOrDefault("memoryUsage", "Unknown")), true)
            .setFooter("NeoEssentials Server Monitor", null)
            .setTimestamp(Instant.now());
        
        sendEmbed(embed, "NeoEssentials Server", null);
    }
    
    /**
     * Send moderation action notification
     */
    public static void sendModerationAction(String action, String moderator, String target, String reason) {
        if (!isEnabled()) return;
        
        Color actionColor = switch (action.toLowerCase()) {
            case "ban", "tempban" -> new Color(231, 76, 60);    // Red
            case "kick" -> new Color(230, 126, 34);             // Orange
            case "mute" -> new Color(241, 196, 15);             // Yellow
            case "warn" -> new Color(52, 152, 219);             // Blue
            default -> new Color(149, 165, 166);                // Gray
        };
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🛡️ Moderation Action")
            .setDescription("Server moderation activity")
            .setColor(actionColor)
            .addField("Action", action.toUpperCase(), true)
            .addField("Moderator", moderator, true)
            .addField("Target", target, true)
            .addField("Reason", reason != null ? reason : "No reason specified", false)
            .setFooter("NeoEssentials Moderation", null)
            .setTimestamp(Instant.now());
        
        sendEmbed(embed, "NeoEssentials Security", null);
    }
    
    /**
     * Send player achievement notification
     */
    public static void sendPlayerAchievement(ServerPlayer player, String achievement, String description) {
        if (!isEnabled()) return;
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🏆 Player Achievement")
            .setDescription("Player milestone reached!")
            .setColor(new Color(155, 89, 182)) // Purple
            .setThumbnail("https://mc-heads.net/avatar/" + player.getUUID())
            .addField("Player", player.getName().getString(), true)
            .addField("Achievement", achievement, true)
            .addField("Description", description, false)
            .setFooter("NeoEssentials Achievements", null)
            .setTimestamp(Instant.now());
        
        sendEmbed(embed, "NeoEssentials", null);
    }
    
    /**
     * Send custom notification with specified embed
     */
    public static CompletableFuture<Boolean> sendCustomNotification(CommandSourceStack source, EmbedBuilder embed) {
        if (!isEnabled()) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("§cDiscord integration is disabled"));
            return CompletableFuture.completedFuture(false);
        }
        
        return sendEmbed(embed, "NeoEssentials Custom", null).whenComplete((success, throwable) -> {
            if (throwable != null) {
                ErrorHandler.handleSystemError("Discord Custom Notification", "send custom embed", new Exception(throwable));
                source.sendFailure(net.minecraft.network.chat.Component.literal("§cFailed to send Discord notification: " + throwable.getMessage()));
            } else if (success) {
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§aCustom Discord notification sent successfully!"), false);
            } else {
                source.sendFailure(net.minecraft.network.chat.Component.literal("§cFailed to send Discord notification"));
            }
        });
    }
    
    /**
     * Test enhanced Discord integration
     */
    public static CompletableFuture<Boolean> testEnhancedIntegration(CommandSourceStack source) {
        EmbedBuilder testEmbed = new EmbedBuilder()
            .setTitle("🧪 Enhanced Integration Test")
            .setDescription("Testing NeoEssentials enhanced Discord features")
            .setColor(new Color(52, 152, 219))
            .addField("Status", "✅ Enhanced integration active", true)
            .addField("Features", "Rich embeds, player stats, economy reports", true)
            .addField("Timestamp", Instant.now().toString(), false)
            .setFooter("NeoEssentials Enhanced Discord", null)
            .setTimestamp(Instant.now());
        
        return sendEmbed(testEmbed, "NeoEssentials Test", null).whenComplete((success, throwable) -> {
            if (throwable != null) {
                source.sendFailure(net.minecraft.network.chat.Component.literal("§cEnhanced integration test failed: " + throwable.getMessage()));
            } else if (success) {
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a✅ Enhanced Discord integration test successful!"), false);
            } else {
                source.sendFailure(net.minecraft.network.chat.Component.literal("§c❌ Enhanced integration test failed"));
            }
        });
    }
    
    /**
     * Helper methods
     */
    private static long getPlayTime(ServerPlayer player) {
        // Placeholder - implement actual playtime tracking
        return player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME));
    }
    
    private static String formatPlayTime(long ticks) {
        long seconds = ticks / 20; // Convert ticks to seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else {
            return String.format("%dm %ds", minutes, seconds % 60);
        }
    }
    
    /**
     * Configuration validation
     */
    public static boolean isConfigurationValid() {
        String webhookUrl = getWebhookUrl();
        return isEnabled() && webhookUrl != null && !webhookUrl.isEmpty() && webhookUrl.startsWith("https://discord.com/api/webhooks/");
    }
    
    /**
     * Get integration status information
     */
    public static Map<String, Object> getStatus() {
        String webhookUrl = getWebhookUrl();
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", isEnabled());
        status.put("webhookConfigured", webhookUrl != null && !webhookUrl.isEmpty());
        status.put("webhookValid", isConfigurationValid());
        status.put("features", List.of("Rich Embeds", "Player Stats", "Economy Reports", "Server Status", "Moderation Alerts"));
        return status;
    }
}
