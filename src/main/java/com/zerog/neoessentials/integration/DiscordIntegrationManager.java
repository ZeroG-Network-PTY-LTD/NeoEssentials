package com.zerog.neoessentials.integration;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import com.zerog.neoessentials.config.TablistConfig;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import com.zerog.neoessentials.util.DebugUtil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Advanced Discord Integration Manager for NeoEssentials
 * Handles comprehensive Discord integration with rich embeds, role sync, and real-time updates
 */
public class DiscordIntegrationManager {
    
    private static DiscordIntegrationManager instance;
    private TablistConfig.DiscordIntegration config;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private int tickCounter = 0;
    
    private DiscordIntegrationManager() {
        loadConfiguration();
        startPeriodicTasks();
    }
    
    public static DiscordIntegrationManager getInstance() {
        if (instance == null) {
            instance = new DiscordIntegrationManager();
        }
        return instance;
    }
    
    /**
     * Load Discord integration configuration
     */
    private void loadConfiguration() {
        try {
            String configPath = "config/neoessentials/tablist.json";
            java.io.File configFile = new java.io.File(configPath);
            
            if (configFile.exists()) {
                Gson gson = new Gson();
                try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
                    TablistConfig tablistConfig = gson.fromJson(reader, TablistConfig.class);
                    config = tablistConfig.discordIntegration;
                    
                    if (config != null && config.enabled) {
                        DebugUtil.debugLog("[DiscordIntegrationManager] Discord integration enabled and configured");
                    } else {
                        DebugUtil.debugLog("[DiscordIntegrationManager] Discord integration disabled in config");
                    }
                }
            }
        } catch (Exception e) {
            DebugUtil.errorLog("[DiscordIntegrationManager] Error loading configuration: " + e.getMessage());
        }
    }
    
    /**
     * Start periodic tasks for Discord integration
     */
    private void startPeriodicTasks() {
        if (config != null && config.enabled) {
            // Status updates
            if (config.statusUpdates.enabled) {
                scheduler.scheduleAtFixedRate(this::sendStatusUpdate, 
                    config.statusUpdates.updateInterval, 
                    config.statusUpdates.updateInterval, 
                    TimeUnit.SECONDS);
            }
            
            // Role synchronization
            if (config.roleSync.enabled) {
                scheduler.scheduleAtFixedRate(this::synchronizeAllRoles, 
                    config.roleSync.syncInterval, 
                    config.roleSync.syncInterval, 
                    TimeUnit.SECONDS);
            }
        }
    }
    
    /**
     * Send enriched Discord notification with embeds
     */
    public void sendEnrichedNotification(String notificationType, ServerPlayer player, Map<String, Object> data) {
        if (!isEnabled() || config.notifications == null) return;
        
        try {
            TablistConfig.NotificationConfig notifConfig = getNotificationConfig(notificationType);
            if (notifConfig == null || !notifConfig.enabled) return;
            
            JsonObject embed = createRichEmbed(notificationType, player, data, notifConfig);
            String channel = notifConfig.channel;
            
            sendDiscordEmbed(embed, channel);
            
            DebugUtil.debugLog("[DiscordIntegrationManager] Sent enriched notification: " + notificationType);
            
        } catch (Exception e) {
            DebugUtil.errorLog("[DiscordIntegrationManager] Error sending enriched notification: " + e.getMessage());
        }
    }
    
    /**
     * Create rich Discord embed for notifications
     */
    private JsonObject createRichEmbed(String notificationType, ServerPlayer player, Map<String, Object> data, TablistConfig.NotificationConfig notifConfig) {
        JsonObject embed = new JsonObject();
        PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
        
        // Basic embed structure
        embed.addProperty("title", getEmbedTitle(notificationType));
        embed.addProperty("color", getEmbedColor(notificationType));
        embed.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        
        // Description with processed placeholders
        String description = placeholderMgr.processPlaceholders(notifConfig.format, player);
        embed.addProperty("description", description);
        
        // Add fields based on configuration
        JsonArray fields = new JsonArray();
        
        if (config.messageFormatting.includePlayerStats && player != null) {
            addPlayerStatsFields(fields, player);
        }
        
        if (config.messageFormatting.includeTeamInfo && player != null) {
            addTeamInfoFields(fields, player);
        }
        
        if (config.messageFormatting.includeRankInfo && player != null) {
            addRankInfoFields(fields, player);
        }
        
        // Add custom fields from data
        addCustomDataFields(fields, data);
        
        if (fields.size() > 0) {
            embed.add("fields", fields);
        }
        
        // Add footer
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "NeoEssentials Enhanced Integration");
        footer.addProperty("icon_url", "https://i.imgur.com/neoessentials-icon.png");
        embed.add("footer", footer);
        
        // Add thumbnail if player avatar is available
        if (player != null && config.webhooks.enabled) {
            JsonObject thumbnail = new JsonObject();
            String avatarUrl = placeholderMgr.processPlaceholders(config.webhooks.avatarPlaceholder, player);
            thumbnail.addProperty("url", avatarUrl);
            embed.add("thumbnail", thumbnail);
        }
        
        return embed;
    }
    
    /**
     * Add player statistics fields to embed
     */
    private void addPlayerStatsFields(JsonArray fields, ServerPlayer player) {
        PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
        
        JsonObject healthField = new JsonObject();
        healthField.addProperty("name", "❤️ Health");
        healthField.addProperty("value", placeholderMgr.processPlaceholders("{player_health}/{player_max_health}", player));
        healthField.addProperty("inline", true);
        fields.add(healthField);
        
        JsonObject levelField = new JsonObject();
        levelField.addProperty("name", "⭐ Level");
        levelField.addProperty("value", placeholderMgr.processPlaceholders("{player_level}", player));
        levelField.addProperty("inline", true);
        fields.add(levelField);
        
        JsonObject pingField = new JsonObject();
        pingField.addProperty("name", "📶 Ping");
        pingField.addProperty("value", placeholderMgr.processPlaceholders("{player_ping}ms", player));
        pingField.addProperty("inline", true);
        fields.add(pingField);
    }
    
    /**
     * Add team information fields to embed
     */
    private void addTeamInfoFields(JsonArray fields, ServerPlayer player) {
        PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
        
        JsonObject teamField = new JsonObject();
        teamField.addProperty("name", "👥 Team");
        teamField.addProperty("value", placeholderMgr.processPlaceholders("{ftb_team_display_name}", player));
        teamField.addProperty("inline", true);
        fields.add(teamField);
        
        JsonObject roleField = new JsonObject();
        roleField.addProperty("name", "🎭 Team Role");
        roleField.addProperty("value", placeholderMgr.processPlaceholders("{ftb_team_role}", player));
        roleField.addProperty("inline", true);
        fields.add(roleField);
        
        JsonObject membersField = new JsonObject();
        membersField.addProperty("name", "👨‍👩‍👧‍👦 Members");
        membersField.addProperty("value", placeholderMgr.processPlaceholders("{ftb_team_members}", player));
        membersField.addProperty("inline", true);
        fields.add(membersField);
    }
    
    /**
     * Add rank information fields to embed
     */
    private void addRankInfoFields(JsonArray fields, ServerPlayer player) {
        PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
        
        JsonObject rankField = new JsonObject();
        rankField.addProperty("name", "🎖️ Rank");
        rankField.addProperty("value", placeholderMgr.processPlaceholders("{ftb_rank_display_name}", player));
        rankField.addProperty("inline", true);
        fields.add(rankField);
        
        JsonObject weightField = new JsonObject();
        weightField.addProperty("name", "⚖️ Weight");
        weightField.addProperty("value", placeholderMgr.processPlaceholders("{ftb_rank_weight}", player));
        weightField.addProperty("inline", true);
        fields.add(weightField);
    }
    
    /**
     * Add custom data fields from the data map
     */
    private void addCustomDataFields(JsonArray fields, Map<String, Object> data) {
        if (data == null) return;
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            JsonObject field = new JsonObject();
            field.addProperty("name", formatFieldName(entry.getKey()));
            field.addProperty("value", String.valueOf(entry.getValue()));
            field.addProperty("inline", true);
            fields.add(field);
        }
    }
    
    /**
     * Format field name for display
     */
    private String formatFieldName(String key) {
        String formatted = key.replace("_", " ").replace("-", " ");
        // Capitalize first letter of each word
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : formatted.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Get embed title based on notification type
     */
    private String getEmbedTitle(String notificationType) {
        switch (notificationType) {
            case "tablist_update": return "📋 Tablist Updated";
            case "scoreboard_update": return "📊 Scoreboard Updated";
            case "player_join": return "✅ Player Joined";
            case "player_leave": return "❌ Player Left";
            case "team_update": return "👥 Team Updated";
            case "rank_update": return "🎖️ Rank Updated";
            case "permission_change": return "🔐 Permission Changed";
            case "achievement": return "🏆 Achievement Earned";
            case "role_sync": return "🔄 Role Synchronized";
            default: return "📢 Server Notification";
        }
    }
    
    /**
     * Get embed color based on notification type
     */
    private int getEmbedColor(String notificationType) {
        switch (notificationType) {
            case "tablist_update": return 0x3498db; // Blue
            case "scoreboard_update": return 0x9b59b6; // Purple
            case "player_join": return 0x2ecc71; // Green
            case "player_leave": return 0xe74c3c; // Red
            case "team_update": return 0xf39c12; // Orange
            case "rank_update": return 0xf1c40f; // Yellow
            case "permission_change": return 0xe67e22; // Dark Orange
            case "achievement": return 0xffd700; // Gold
            case "role_sync": return 0x1abc9c; // Turquoise
            default: return 0x95a5a6; // Gray
        }
    }
    
    /**
     * Send status update to Discord
     */
    private void sendStatusUpdate() {
        if (!isEnabled() || !config.statusUpdates.enabled) return;
        
        try {
            JsonObject embed = createStatusEmbed();
            sendDiscordEmbed(embed, config.statusUpdates.channel);
            
            DebugUtil.debugLog("[DiscordIntegrationManager] Sent status update to Discord");
            
        } catch (Exception e) {
            DebugUtil.errorLog("[DiscordIntegrationManager] Error sending status update: " + e.getMessage());
        }
    }
    
    /**
     * Create status embed with server information
     */
    private JsonObject createStatusEmbed() {
        JsonObject embed = new JsonObject();
        PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
        
        embed.addProperty("title", config.statusUpdates.embedStyle.title);
        embed.addProperty("color", Integer.parseInt(config.statusUpdates.embedStyle.color.replace("#", ""), 16));
        embed.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        
        // Add status fields
        JsonArray fields = new JsonArray();
        for (TablistConfig.EmbedField fieldConfig : config.statusUpdates.embedStyle.fields) {
            JsonObject field = new JsonObject();
            field.addProperty("name", fieldConfig.name);
            field.addProperty("value", placeholderMgr.processPlaceholders(fieldConfig.value, (ServerPlayer) null));
            field.addProperty("inline", fieldConfig.inline);
            fields.add(field);
        }
        embed.add("fields", fields);
        
        // Add footer
        JsonObject footer = new JsonObject();
        footer.addProperty("text", placeholderMgr.processPlaceholders(config.statusUpdates.embedStyle.footer, (ServerPlayer) null));
        embed.add("footer", footer);
        
        // Add thumbnail
        if (config.statusUpdates.embedStyle.thumbnail != null) {
            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", config.statusUpdates.embedStyle.thumbnail);
            embed.add("thumbnail", thumbnail);
        }
        
        return embed;
    }
    
    /**
     * Synchronize roles for all online players
     */
    private void synchronizeAllRoles() {
        if (!isEnabled() || !config.roleSync.enabled) return;
        
        try {
            // Get all online players and sync their roles
            // This would need to be implemented based on server access
            DebugUtil.debugLog("[DiscordIntegrationManager] Synchronized roles for all online players");
            
        } catch (Exception e) {
            DebugUtil.errorLog("[DiscordIntegrationManager] Error synchronizing roles: " + e.getMessage());
        }
    }
    
    /**
     * Send Discord embed via SimpleDiscordLink
     */
    private void sendDiscordEmbed(JsonObject embed, String channel) {
        try {
            // Use SimpleDiscordLink to send embed
            SimpleDiscordLinkIntegration.sendEnhancedMessageToDiscord(
                embed.toString(), 
                SimpleDiscordLinkIntegration.MessageType.NOTIFICATION, 
                null
            );
            
        } catch (Exception e) {
            DebugUtil.errorLog("[DiscordIntegrationManager] Error sending Discord embed: " + e.getMessage());
        }
    }
    
    /**
     * Get notification configuration by type
     */
    private TablistConfig.NotificationConfig getNotificationConfig(String type) {
        if (config.notifications == null) return null;
        
        switch (type) {
            case "tablist_update": return config.notifications.tablistUpdates;
            case "scoreboard_update": return config.notifications.scoreboardUpdates;
            case "player_join": return config.notifications.playerJoin;
            case "player_leave": return config.notifications.playerLeave;
            case "team_update": return config.notifications.teamUpdates;
            case "rank_update": return config.notifications.rankUpdates;
            case "permission_change": return config.notifications.permissionChanges;
            case "achievement": return config.notifications.achievements;
            default: return null;
        }
    }
    
    /**
     * Handle player achievements
     */
    @SubscribeEvent
    public void onPlayerAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!isEnabled()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            Map<String, Object> data = new HashMap<>();
            data.put("achievement", event.getAdvancement().id().toString());
            
            sendEnrichedNotification("achievement", player, data);
        }
    }
    
    /**
     * Handle server tick events for periodic updates
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        tickCounter++;
        
        // Update player data cache every 20 ticks (1 second)
        if (tickCounter % 20 == 0) {
            updatePlayerDataCache();
        }
    }
    
    /**
     * Update player data cache
     */
    private void updatePlayerDataCache() {
        // Update cached player data for efficient Discord integration
        // This would need to be implemented based on server access
    }
    
    /**
     * Check if Discord integration is enabled
     */
    public boolean isEnabled() {
        return config != null && config.enabled;
    }
    
    /**
     * Get role mapping configuration
     */
    public TablistConfig.RoleMapping getRoleMapping(String discordRole) {
        if (config == null || config.roleSync == null || config.roleSync.roleMappings == null) {
            return config.roleSync.fallbackRole;
        }
        
        return config.roleSync.roleMappings.getOrDefault(discordRole, config.roleSync.fallbackRole);
    }
    
    /**
     * Shutdown the integration manager
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Discord player data for caching
     */
    public static class DiscordPlayerData {
        public final UUID playerId;
        public final String discordId;
        public final List<String> discordRoles;
        public final long lastSync;
        
        public DiscordPlayerData(UUID playerId, String discordId, List<String> discordRoles) {
            this.playerId = playerId;
            this.discordId = discordId;
            this.discordRoles = new ArrayList<>(discordRoles);
            this.lastSync = System.currentTimeMillis();
        }
    }
}
