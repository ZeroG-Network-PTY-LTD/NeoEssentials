package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.List;

/**
 * Discord integration configuration for NeoEssentials
 * Compatible with EssentialsX Discord features
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordConfig {
    
    // Basic Discord settings
    public boolean enabled = false; // Disabled by default for security
    public String botToken = "";
    public String guildId = "";
    public boolean enableChatRelay = true;
    public boolean enableCommandRelay = false;
    public boolean enableStatusUpdates = true;
    
    // Channel settings
    public ChannelConfig channels = new ChannelConfig();
    
    // Message relay settings
    public RelayConfig relay = new RelayConfig();
    
    // Status settings
    public StatusConfig status = new StatusConfig();
    
    // Webhook settings
    public WebhookConfig webhooks = new WebhookConfig();
    
    // Security settings
    public SecurityConfig security = new SecurityConfig();
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class ChannelConfig {
        public String generalChannelId = "";
        public String adminChannelId = "";
        public String consoleChannelId = "";
        public String chatChannelId = "";
        public String joinLeaveChannelId = "";
        public String deathChannelId = "";
        public String achievementChannelId = "";
        public String punishmentChannelId = "";
        public String helpopChannelId = "";
        public String auditChannelId = "";
    }
    
    public static class RelayConfig {
        public boolean enabled = true;
        public boolean relayChat = true;
        public boolean relayJoinLeave = true;
        public boolean relayDeaths = true;
        public boolean relayAchievements = true;
        public boolean relayPunishments = true;
        public boolean relayHelpop = true;
        public boolean relayConsole = false; // Security sensitive
        public boolean bidirectionalChat = true; // Discord -> Minecraft
        public boolean filterProfanity = true;
        public boolean useWebhooks = true;
        public List<String> ignoredPlayers = Arrays.asList("Server", "Console");
        public List<String> allowedChannels = Arrays.asList();
    }
    
    public static class StatusConfig {
        public boolean enabled = true;
        public int updateIntervalSeconds = 60;
        public String activityType = "PLAYING"; // PLAYING, LISTENING, WATCHING
        public String activityMessage = "Minecraft | {ONLINE}/{MAX} players";
        public boolean showPlayerCount = true;
        public boolean showTps = false;
        public String statusOnline = "🟢 Server Online";
        public String statusOffline = "🔴 Server Offline";
        public String statusStarting = "🟡 Server Starting";
    }
    
    public static class WebhookConfig {
        public boolean enabled = false;
        public String chatWebhookUrl = "";
        public String joinLeaveWebhookUrl = "";
        public String deathWebhookUrl = "";
        public String achievementWebhookUrl = "";
        public String punishmentWebhookUrl = "";
        public String avatarUrl = "https://crafatar.com/avatars/{UUID}?size=64";
        public boolean usePlayerAvatars = true;
    }
    
    public static class SecurityConfig {
        public boolean requirePermissions = true;
        public boolean logCommands = true;
        public boolean rateLimitEnabled = true;
        public int rateLimitMessages = 5;
        public int rateLimitPeriodMinutes = 1;
        public List<String> adminRoles = Arrays.asList("Admin", "Moderator");
        public List<String> moderatorRoles = Arrays.asList("Moderator", "Helper");
        public List<String> trustedRoles = Arrays.asList("Trusted", "VIP");
        public List<String> blockedUsers = Arrays.asList();
        public boolean allowAtEveryone = false;
        public boolean allowAtHere = false;
    }
    
    public static class MessagesConfig {
        // Chat relay formats
        public String minecraftToDiscordFormat = "**{PLAYER}**: {MESSAGE}";
        public String discordToMinecraftFormat = "&7[&9Discord&7] &b{USER}&7: &f{MESSAGE}";
        public String webhookUsername = "{PLAYER}";
        
        // Join/Leave messages
        public String joinMessage = "📥 **{PLAYER}** joined the server";
        public String leaveMessage = "📤 **{PLAYER}** left the server";
        public String firstJoinMessage = "🎉 **{PLAYER}** joined for the first time!";
        
        // Death messages
        public String deathMessage = "💀 **{PLAYER}** {DEATH_MESSAGE}";
        public String pvpDeathMessage = "⚔️ **{PLAYER}** was killed by **{KILLER}**";
        
        // Achievement messages
        public String achievementMessage = "🏆 **{PLAYER}** earned the achievement **{ACHIEVEMENT}**";
        public String advancementMessage = "⭐ **{PLAYER}** completed the advancement **{ADVANCEMENT}**";
        
        // Punishment messages
        public String banMessage = "🔨 **{PLAYER}** was banned by **{STAFF}** for: {REASON}";
        public String unbanMessage = "✅ **{PLAYER}** was unbanned by **{STAFF}**";
        public String kickMessage = "👢 **{PLAYER}** was kicked by **{STAFF}** for: {REASON}";
        public String muteMessage = "🔇 **{PLAYER}** was muted by **{STAFF}** for: {REASON}";
        public String unmuteMessage = "🔊 **{PLAYER}** was unmuted by **{STAFF}**";
        public String jailMessage = "🏢 **{PLAYER}** was jailed by **{STAFF}** for: {REASON}";
        public String unjailMessage = "🆓 **{PLAYER}** was unjailed by **{STAFF}**";
        
        // HelpOp messages
        public String helpopMessage = "🆘 **{PLAYER}** needs help: {MESSAGE}";
        public String helpopReply = "💬 Staff reply to **{PLAYER}**: {MESSAGE}";
        
        // Status messages
        public String serverStarting = "🟡 **Server is starting up...**";
        public String serverOnline = "🟢 **Server is now online!**";
        public String serverOffline = "🔴 **Server is now offline.**";
        public String serverRestarting = "🔄 **Server is restarting...**";
        
        // Error messages
        public String playerNotFound = "❌ Player **{PLAYER}** not found!";
        public String noPermission = "❌ You don't have permission to use that command!";
        public String rateLimited = "⏰ You're sending messages too quickly! Please slow down.";
        public String messageFiltered = "🚫 Your message was filtered for inappropriate content.";
        public String botOffline = "🤖 Discord bot is currently offline.";
        
        // Command feedback
        public String commandExecuted = "✅ Command executed: `{COMMAND}`";
        public String commandFailed = "❌ Command failed: {ERROR}";
        public String commandCooldown = "⏰ Command on cooldown for {TIME} seconds.";
        
        // Embed settings
        public String embedColor = "#00AA00"; // Green
        public String errorColor = "#FF0000"; // Red
        public String warningColor = "#FFAA00"; // Orange
        public String infoColor = "#0099FF"; // Blue
        public boolean useEmbeds = true;
        public boolean showTimestamps = true;
        public String footerText = "NeoEssentials Discord Bot";
        public String footerIcon = "";
    }
    
    /**
     * Check if Discord integration is enabled and properly configured
     */
    public boolean isEnabled() {
        return enabled && !botToken.isEmpty() && !guildId.isEmpty();
    }
    
    /**
     * Check if chat relay is enabled
     */
    public boolean isChatRelayEnabled() {
        return isEnabled() && enableChatRelay && relay.enabled && relay.relayChat;
    }
    
    /**
     * Check if webhooks are enabled and configured
     */
    public boolean areWebhooksEnabled() {
        return isEnabled() && webhooks.enabled && !webhooks.chatWebhookUrl.isEmpty();
    }
    
    /**
     * Check if status updates are enabled
     */
    public boolean areStatusUpdatesEnabled() {
        return isEnabled() && enableStatusUpdates && status.enabled;
    }
    
    /**
     * Check if bidirectional chat is enabled
     */
    public boolean isBidirectionalChatEnabled() {
        return isChatRelayEnabled() && relay.bidirectionalChat;
    }
    
    /**
     * Validate Discord configuration
     */
    public boolean isValid() {
        if (!enabled) return true; // Valid if disabled
        
        if (botToken.isEmpty()) return false;
        if (guildId.isEmpty()) return false;
        if (channels.chatChannelId.isEmpty() && enableChatRelay) return false;
        
        return true;
    }
    
    /**
     * Configure webhook URL and enable integration
     */
    public void configureWebhook(String webhookUrl) {
        if (webhookUrl != null && !webhookUrl.trim().isEmpty()) {
            this.webhooks.chatWebhookUrl = webhookUrl.trim();
            this.webhooks.enabled = true;
            this.enabled = true;
        }
    }
    
    /**
     * Disable Discord integration
     */
    public void disable() {
        this.enabled = false;
        this.webhooks.enabled = false;
    }
}
