package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Moderation tools configuration for NeoEssentials
 * Compatible with EssentialsX moderation system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ModerationConfig {
    
    // Basic moderation settings
    public boolean enabled = true;
    public boolean logActions = true;
    public boolean broadcastActions = true;
    public boolean enableJail = true;
    
    // Ban settings
    public BanConfig ban = new BanConfig();
    
    // Kick settings
    public KickConfig kick = new KickConfig();
    
    // Mute settings
    public MuteConfig mute = new MuteConfig();
    
    // Jail settings
    public JailConfig jail = new JailConfig();
    
    // Monitoring settings
    public MonitoringConfig monitoring = new MonitoringConfig();
    
    // Punishment tracking
    public boolean enablePunishmentHistory = true;
    public int maxPunishmentHistoryDays = 90;
    
    // Auto-punishment settings
    public AutoPunishmentConfig autoPunishment = new AutoPunishmentConfig();
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class BanConfig {
        public boolean enabled = true;
        public boolean enableTempBan = true;
        public int defaultBanDays = 7;
        public int maxBanDays = 365;
        public boolean enableIPBan = true;
        public boolean broadcastBans = true;
        public String defaultBanReason = "Violating server rules";
        public List<String> banExemptPermissions = Arrays.asList("essentials.ban.exempt");
    }
    
    public static class KickConfig {
        public boolean enabled = true;
        public boolean broadcastKicks = true;
        public String defaultKickReason = "Kicked by an administrator";
        public int kickCooldown = 60; // seconds
        public List<String> kickExemptPermissions = Arrays.asList("essentials.kick.exempt");
    }
    
    public static class MuteConfig {
        public boolean enabled = true;
        public boolean enableTempMute = true;
        public int defaultMuteDays = 1;
        public int maxMuteDays = 30;
        public boolean muteChat = true;
        public boolean muteCommands = true;
        public boolean muteSigns = true;
        public boolean muteBooks = true;
        public String defaultMuteReason = "Chat violations";
        public List<String> allowedCommandsWhileMuted = Arrays.asList("msg", "reply", "helpop");
        public List<String> muteExemptPermissions = Arrays.asList("essentials.mute.exempt");
    }
    
    public static class JailConfig {
        public boolean enabled = true;
        public String defaultJailName = "jail";
        public int defaultJailTime = 300; // 5 minutes in seconds
        public int maxJailTime = 86400; // 24 hours
        public boolean preventTeleport = true;
        public boolean preventCommands = true;
        public List<String> allowedCommandsInJail = Arrays.asList("msg", "reply", "helpop");
        public String jailWorld = "world";
        public double jailX = 0.0;
        public double jailY = 64.0;
        public double jailZ = 0.0;
    }
    
    public static class MonitoringConfig {
        public boolean enableWhois = true;
        public boolean enableSeen = true;
        public boolean enableGetPos = true;
        public boolean enableList = true;
        public boolean enableGC = true;
        public boolean trackPlayerStats = true;
        public boolean trackOnlineTime = true;
        public boolean trackLastLogin = true;
        public boolean trackIPAddress = true;
    }
    
    public static class AutoPunishmentConfig {
        public boolean enabled = false;
        public Map<String, AutoRule> rules = new HashMap<>();
        
        public AutoPunishmentConfig() {
            // Example auto-punishment rules
            AutoRule spamRule = new AutoRule();
            spamRule.enabled = true;
            spamRule.trigger = "chat_spam";
            spamRule.maxViolations = 3;
            spamRule.timeWindow = 60; // seconds
            spamRule.punishment = "mute";
            spamRule.duration = 300; // 5 minutes
            spamRule.reason = "Automatic punishment for spamming";
            rules.put("spam", spamRule);
            
            AutoRule swearRule = new AutoRule();
            swearRule.enabled = true;
            swearRule.trigger = "bad_words";
            swearRule.maxViolations = 5;
            swearRule.timeWindow = 300; // 5 minutes
            swearRule.punishment = "tempban";
            swearRule.duration = 3600; // 1 hour
            swearRule.reason = "Automatic punishment for inappropriate language";
            rules.put("swearing", swearRule);
        }
    }
    
    public static class AutoRule {
        public boolean enabled = true;
        public String trigger;
        public int maxViolations;
        public int timeWindow; // in seconds
        public String punishment; // kick, mute, tempban, ban, jail
        public int duration; // in seconds (0 for permanent)
        public String reason;
    }
    
    public static class MessagesConfig {
        // Ban messages
        public String playerBanned = "&c{0} has been banned by {1}. Reason: {2}";
        public String playerTempBanned = "&c{0} has been temporarily banned for {1} by {2}. Reason: {3}";
        public String playerUnbanned = "&a{0} has been unbanned by {1}";
        public String youAreBanned = "&cYou are banned from this server!\n&cReason: {0}\n&cBanned by: {1}\n&cExpires: {2}";
        
        // Kick messages
        public String playerKicked = "&c{0} has been kicked by {1}. Reason: {2}";
        public String youAreKicked = "&cYou have been kicked!\n&cReason: {0}\n&cKicked by: {1}";
        
        // Mute messages
        public String playerMuted = "&c{0} has been muted by {1}. Reason: {2}";
        public String playerTempMuted = "&c{0} has been muted for {1} by {2}. Reason: {3}";
        public String playerUnmuted = "&a{0} has been unmuted by {1}";
        public String youAreMuted = "&cYou are muted! Reason: {0}";
        public String muteExpired = "&aYour mute has expired!";
        
        // Jail messages
        public String playerJailed = "&c{0} has been jailed by {1} for {2}. Reason: {3}";
        public String playerUnjailed = "&a{0} has been released from jail by {1}";
        public String youAreJailed = "&cYou are in jail! Time remaining: {0}";
        public String jailReleased = "&aYou have been released from jail!";
        
        // Info messages
        public String whoisHeader = "&6Player info for {0}:";
        public String whoisEntry = "&7{0}: &f{1}";
        public String seenOnline = "&a{0} is currently online!";
        public String seenOffline = "&7{0} was last seen {1} ago";
        public String seenNever = "&c{0} has never been on this server";
        public String playerNotFound = "&cPlayer {0} not found!";
        
        // Error messages
        public String noPermission = "&cYou don't have permission to do that!";
        public String playerExempt = "&cYou cannot punish this player!";
        public String invalidDuration = "&cInvalid duration format!";
        public String alreadyPunished = "&cPlayer is already {0}!";
        public String notPunished = "&cPlayer is not {0}!";
    }
    
    /**
     * Get auto-punishment rule by name
     */
    public AutoRule getAutoRule(String name) {
        return autoPunishment.rules.get(name.toLowerCase());
    }
    
    /**
     * Check if auto-punishment is enabled for a trigger
     */
    public boolean isAutoRuleEnabled(String trigger) {
        return autoPunishment.enabled && 
               autoPunishment.rules.containsKey(trigger) && 
               autoPunishment.rules.get(trigger).enabled;
    }
}
