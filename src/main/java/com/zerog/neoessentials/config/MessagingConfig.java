package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.List;

/**
 * Messaging system configuration for NeoEssentials
 * Compatible with EssentialsX messaging system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MessagingConfig {
    
    // Basic messaging settings
    public boolean enabled = true;
    public boolean enablePrivateMessages = true;
    public boolean enableMail = true;
    public boolean enableReply = true;
    public boolean enableSocialSpy = true;
    
    // Private message settings
    public PrivateMessageConfig privateMessages = new PrivateMessageConfig();
    
    // Mail settings
    public MailConfig mail = new MailConfig();
    
    // Social spy settings
    public SocialSpyConfig socialSpy = new SocialSpyConfig();
    
    // Helpop settings
    public HelpOpConfig helpop = new HelpOpConfig();
    
    // Broadcast settings
    public BroadcastConfig broadcast = new BroadcastConfig();
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class PrivateMessageConfig {
        public boolean enabled = true;
        public boolean requirePermission = false;
        public boolean allowCrossWorld = true;
        public boolean allowOfflineMessages = false;
        public boolean logMessages = true;
        public int messageHistoryLimit = 50;
        public int cooldownSeconds = 3;
        public boolean enableToggle = true; // Allow players to toggle PM receiving
        public List<String> blockedWords = Arrays.asList("spam", "advertisement");
        public boolean enableIgnoreList = true;
        public int maxIgnoreListSize = 20;
    }
    
    public static class MailConfig {
        public boolean enabled = true;
        public int maxMailsPerPlayer = 50;
        public int maxMailsPerSender = 10; // Max mails per sender per day
        public int mailExpiryDays = 30;
        public boolean notifyOnJoin = true;
        public boolean notifyOnSend = true;
        public boolean requirePermissionToSend = false;
        public boolean allowAttachments = false; // Future feature
        public int maxMailLength = 500;
        public int cooldownSeconds = 30;
    }
    
    public static class SocialSpyConfig {
        public boolean enabled = true;
        public boolean defaultEnabled = false; // Auto-enable for staff
        public boolean logToConsole = true;
        public boolean logToFile = true;
        public List<String> exemptPermissions = Arrays.asList("essentials.socialspy.exempt");
        public boolean showCommands = true;
        public boolean showPrivateMessages = true;
        public boolean showMail = false;
    }
    
    public static class HelpOpConfig {
        public boolean enabled = true;
        public String targetPermission = "essentials.helpop.receive";
        public boolean logToConsole = true;
        public boolean logToFile = true;
        public int cooldownSeconds = 60;
        public String format = "&c[HelpOp] &7{PLAYER}: &f{MESSAGE}";
        public boolean notifySound = true;
    }
    
    public static class BroadcastConfig {
        public boolean enabled = true;
        public boolean requirePermission = true;
        public boolean allowColors = true;
        public boolean allowFormatting = true;
        public String prefix = "&6[Broadcast]&r ";
        public boolean logBroadcasts = true;
        public int cooldownSeconds = 10;
    }
    
    public static class MessagesConfig {
        // Private message formats
        public String pmFormatSender = "&7[&6me &7-> &6{0}&7] &f{1}";
        public String pmFormatReceiver = "&7[&6{0} &7-> &6me&7] &f{1}";
        public String pmFormatSocialSpy = "&8[SocialSpy] &7{0} -> {1}: &f{2}";
        public String replyFormatSender = "&7[&6me &7-> &6{0}&7] &f{1}";
        public String replyFormatReceiver = "&7[&6{0} &7-> &6me&7] &f{1}";
        
        // Status messages
        public String messageSent = "&7Message sent to {0}";
        public String noReplyTarget = "&cNo one to reply to!";
        public String playerNotFound = "&cPlayer {0} not found!";
        public String playerOffline = "&cPlayer {0} is offline!";
        public String messagesToggleOff = "&cYou have disabled private messages!";
        public String messagesToggleOn = "&aYou have enabled private messages!";
        public String targetMessagesDisabled = "&c{0} has disabled private messages!";
        public String messageCooldown = "&cYou must wait {0} before sending another message!";
        public String socialSpyOn = "&aSocial spy enabled!";
        public String socialSpyOff = "&cSocial spy disabled!";
        
        // Mail messages
        public String mailSent = "&aMail sent to {0}!";
        public String mailReceived = "&aYou have {0} new mail(s)! Use /mail read to view them.";
        public String mailNotification = "&e[Mail] {SENDER}: {PREVIEW}...";
        public String mailRead = "&7[{DATE}] &6{SENDER}&7: &f{MESSAGE}";
        public String mailDeleted = "&cMail deleted!";
        public String mailClear = "&c{COUNT} mails cleared!";
        public String mailEmpty = "&7You have no mail.";
        public String mailListHeader = "&6Your mail ({COUNT}/{MAX}):";
        public String mailListEntry = "&7{ID}. &6{SENDER} &8{DATE} &7{PREVIEW}";
        public String mailFull = "&c{PLAYER}'s mailbox is full!";
        public String mailExpired = "&7{COUNT} expired mails were automatically deleted.";
        
        // Ignore list messages
        public String ignoreAdded = "&aYou are now ignoring {PLAYER}!";
        public String ignoreRemoved = "&aYou are no longer ignoring {PLAYER}!";
        public String ignoreList = "&6Ignored players: &7{PLAYERS}";
        public String ignoreListEmpty = "&7You are not ignoring anyone.";
        public String alreadyIgnoring = "&cYou are already ignoring {PLAYER}!";
        public String notIgnoring = "&cYou are not ignoring {PLAYER}!";
        public String ignoreListFull = "&cYour ignore list is full! Remove someone first.";
        public String cannotIgnoreSelf = "&cYou cannot ignore yourself!";
        public String playerIgnoringYou = "&cThat player is ignoring you!";
        
        // HelpOp messages
        public String helpopSent = "&aYour request has been sent to online staff!";
        public String helpopReceived = "&c[HelpOp] &7{PLAYER}: &f{MESSAGE}";
        public String helpopEmpty = "&cPlease provide a message for the help request!";
        public String helpopCooldown = "&cYou must wait {TIME} before sending another help request!";
        public String helpopNoStaff = "&cNo staff members are currently online!";
        
        // Broadcast messages
        public String broadcastSent = "&aBroadcast sent!";
        public String broadcastReceived = "&6[Broadcast] &f{MESSAGE}";
        public String broadcastEmpty = "&cPlease provide a message to broadcast!";
        public String broadcastCooldown = "&cYou must wait {TIME} before sending another broadcast!";
        
        // Error messages
        public String noPermission = "&cYou don't have permission to do that!";
        public String invalidUsage = "&cInvalid usage! Use: {USAGE}";
        public String messageTooLong = "&cMessage is too long! Maximum {MAX} characters.";
        public String blockedWord = "&cYour message contains blocked words!";
    }
    
    /**
     * Check if private messages are enabled
     */
    public boolean arePrivateMessagesEnabled() {
        return enabled && enablePrivateMessages && privateMessages.enabled;
    }
    
    /**
     * Check if mail is enabled
     */
    public boolean isMailEnabled() {
        return enabled && enableMail && mail.enabled;
    }
    
    /**
     * Check if social spy is enabled
     */
    public boolean isSocialSpyEnabled() {
        return enabled && enableSocialSpy && socialSpy.enabled;
    }
    
    /**
     * Check if helpop is enabled
     */
    public boolean isHelpOpEnabled() {
        return enabled && helpop.enabled;
    }
    
    /**
     * Check if broadcasts are enabled
     */
    public boolean isBroadcastEnabled() {
        return enabled && broadcast.enabled;
    }
}
