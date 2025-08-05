package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigurationUnifier;
import com.zerog.neoessentials.config.MessagingConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Messaging management system for NeoEssentials
 * Handles mail, private messages, announcements, and broadcasts
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MessagingManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingManager.class);
    private static MessagingManager instance;
    
    private final ConfigurationUnifier configUnifier;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, UUID> lastMessaged;
    private final Map<UUID, Boolean> socialSpyEnabled;
    
    private MessagingManager() {
        this.configUnifier = ConfigurationUnifier.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.lastMessaged = new ConcurrentHashMap<>();
        this.socialSpyEnabled = new ConcurrentHashMap<>();
    }
    
    public static MessagingManager getInstance() {
        if (instance == null) {
            instance = new MessagingManager();
        }
        return instance;
    }
    
    /**
     * Send a private message between players
     */
    public boolean sendPrivateMessage(ServerPlayer sender, String targetName, String message) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        if (!config.arePrivateMessagesEnabled()) {
            MessageUtil.sendMessage(sender, "&cPrivate messaging is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(sender, "essentials.msg")) {
            MessageUtil.sendMessage(sender, config.messages.noPermission);
            return false;
        }
        
        // Find target player
        ServerPlayer target = getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendMessage(sender, MessageUtil.replacePlaceholders(config.messages.playerNotFound, targetName));
            return false;
        }
        
        // Check if target is ignoring sender
        if (isPlayerIgnored(target.getUUID(), sender.getUUID())) {
            MessageUtil.sendMessage(sender, MessageUtil.replacePlaceholders(config.messages.playerIgnoringYou, 
                target.getName().getString()));
            return false;
        }
        
        // Check cooldown
        if (isOnMessageCooldown(sender)) {
            MessageUtil.sendMessage(sender, MessageUtil.replacePlaceholders(config.messages.messageCooldown,
                String.valueOf(getRemainingCooldown(sender))));
            return false;
        }
        
        // Send messages
        String senderFormat = MessageUtil.replacePlaceholders(config.messages.pmFormatSender,
            target.getName().getString(), message);
        
        String receiverFormat = MessageUtil.replacePlaceholders(config.messages.pmFormatReceiver,
            sender.getName().getString(), message);
        
        MessageUtil.sendMessage(sender, senderFormat);
        MessageUtil.sendMessage(target, receiverFormat);
        
        // Update last messaged
        lastMessaged.put(sender.getUUID(), target.getUUID());
        lastMessaged.put(target.getUUID(), sender.getUUID());
        
        // Social spy
        sendToSocialSpy(sender, target, message);
        
        // Set cooldown
        setMessageCooldown(sender);
        
        LOGGER.info("Private message from {} to {}: {}", 
            sender.getName().getString(), target.getName().getString(), message);
        
        return true;
    }
    
    /**
     * Reply to the last received private message
     */
    public boolean replyToMessage(ServerPlayer sender, String message) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        UUID lastSenderUuid = lastMessaged.get(sender.getUUID());
        if (lastSenderUuid == null) {
            MessageUtil.sendMessage(sender, config.messages.noReplyTarget);
            return false;
        }
        
        ServerPlayer target = getPlayerByUuid(lastSenderUuid);
        if (target == null) {
            MessageUtil.sendMessage(sender, config.messages.playerOffline);
            return false;
        }
        
        return sendPrivateMessage(sender, target.getName().getString(), message);
    }
    
    /**
     * Send mail to a player
     */
    public boolean sendMail(ServerPlayer sender, String targetName, String message) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        if (!config.isMailEnabled()) {
            MessageUtil.sendMessage(sender, "&cMail system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(sender, "essentials.mail.send")) {
            MessageUtil.sendMessage(sender, config.messages.noPermission);
            return false;
        }
        
        // Get target UUID (could be offline player)
        UUID targetUuid = getPlayerUuidByName(targetName);
        if (targetUuid == null) {
            MessageUtil.sendMessage(sender, MessageUtil.replacePlaceholders(config.messages.playerNotFound, targetName));
            return false;
        }
        
        // Check mail limit
        int mailCount = getMailCount(targetUuid);
        if (mailCount >= config.mail.maxMailsPerPlayer) {
            MessageUtil.sendMessage(sender, MessageUtil.replacePlaceholders(config.messages.mailFull, targetName));
            return false;
        }
        
        // Create mail entry
        MailEntry mail = new MailEntry(
            UUID.randomUUID(),
            sender.getUUID(),
            sender.getName().getString(),
            targetUuid,
            targetName,
            message,
            System.currentTimeMillis(),
            false
        );
        
        // Save mail
        saveMail(targetUuid, mail);
        
        // Notify sender
        MessageUtil.sendMessage(sender, MessageUtil.replacePlaceholders(config.messages.mailSent, targetName, message));
        
        // Notify target if online
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target != null) {
            MessageUtil.sendMessage(target, MessageUtil.replacePlaceholders(config.messages.mailReceived,
                sender.getName().getString()));
        }
        
        LOGGER.info("Mail sent from {} to {}: {}", 
            sender.getName().getString(), targetName, message);
        
        return true;
    }
    
    /**
     * Read player's mail
     */
    public void readMail(ServerPlayer player) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        if (!config.enabled || !config.enableMail) {
            MessageUtil.sendMessage(player, "&cMail system is disabled.");
            return;
        }
        
        List<MailEntry> mails = getPlayerMail(player.getUUID());
        if (mails.isEmpty()) {
            MessageUtil.sendMessage(player, config.messages.mailEmpty);
            return;
        }
        
        MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.mailListHeader,
            String.valueOf(mails.size()), String.valueOf(config.mail.maxMailsPerPlayer)));
        
        for (int i = 0; i < mails.size(); i++) {
            MailEntry mail = mails.get(i);
            String timeStr = MessageUtil.formatTime(System.currentTimeMillis() - mail.timestamp);
            
            MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.mailRead,
                timeStr, mail.senderName, mail.message));
            
            // Mark as read
            if (!mail.read) {
                mail.read = true;
                updateMail(player.getUUID(), mail);
            }
        }
    }
    
    /**
     * Clear player's mail
     */
    public boolean clearMail(ServerPlayer player) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        List<MailEntry> mails = getPlayerMail(player.getUUID());
        if (mails.isEmpty()) {
            MessageUtil.sendMessage(player, config.messages.mailEmpty);
            return false;
        }
        
        clearPlayerMail(player.getUUID());
        
        MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.mailClear,
            String.valueOf(mails.size())));
        
        return true;
    }
    
    /**
     * Broadcast a message to all players
     */
    public void broadcast(ServerPlayer sender, String message) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        if (!PermissionUtil.hasPermission(sender, "essentials.broadcast")) {
            MessageUtil.sendMessage(sender, config.messages.noPermission);
            return;
        }
        
        String broadcastMessage = MessageUtil.replacePlaceholders(config.messages.broadcastReceived, message);
        
        broadcastToAll(broadcastMessage);
        
        LOGGER.info("Broadcast by {}: {}", sender.getName().getString(), message);
    }
    
    /**
     * Toggle social spy for a player
     */
    public boolean toggleSocialSpy(ServerPlayer player) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        if (!PermissionUtil.hasPermission(player, "essentials.socialspy")) {
            MessageUtil.sendMessage(player, config.messages.noPermission);
            return false;
        }
        
        boolean currentState = socialSpyEnabled.getOrDefault(player.getUUID(), false);
        boolean newState = !currentState;
        
        socialSpyEnabled.put(player.getUUID(), newState);
        playerDataManager.setSetting(player.getUUID(), "socialspy_enabled", newState);
        
        String message = newState ? config.messages.socialSpyOn : config.messages.socialSpyOff;
        MessageUtil.sendMessage(player, message);
        
        return newState;
    }
    
    /**
     * Ignore a player
     */
    public boolean ignorePlayer(ServerPlayer player, String targetName) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        ServerPlayer target = getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.playerNotFound, targetName));
            return false;
        }
        
        if (target.getUUID().equals(player.getUUID())) {
            MessageUtil.sendMessage(player, config.messages.cannotIgnoreSelf);
            return false;
        }
        
        // Check if already ignoring
        if (isPlayerIgnored(player.getUUID(), target.getUUID())) {
            MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.alreadyIgnoring, 
                target.getName().getString()));
            return false;
        }
        
        // Add to ignore list
        addToIgnoreList(player.getUUID(), target.getUUID());
        
        MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.ignoreAdded,
            target.getName().getString()));
        
        return true;
    }
    
    /**
     * Unignore a player
     */
    public boolean unignorePlayer(ServerPlayer player, String targetName) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        UUID targetUuid = getPlayerUuidByName(targetName);
        if (targetUuid == null) {
            MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.playerNotFound, targetName));
            return false;
        }
        
        if (!isPlayerIgnored(player.getUUID(), targetUuid)) {
            MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.notIgnoring, targetName));
            return false;
        }
        
        // Remove from ignore list
        removeFromIgnoreList(player.getUUID(), targetUuid);
        
        MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.ignoreRemoved, targetName));
        
        return true;
    }
    
    /**
     * Check if player is on message cooldown
     */
    private boolean isOnMessageCooldown(ServerPlayer player) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        if (config.privateMessages.cooldownSeconds <= 0) {
            return false;
        }
        
        if (PermissionUtil.hasPermission(player, "essentials.msg.cooldown.bypass")) {
            return false;
        }
        
        Object lastMessageTime = playerDataManager.getSetting(player.getUUID(), "last_message_time");
        if (lastMessageTime == null) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - ((Number) lastMessageTime).longValue();
        return elapsed < (config.privateMessages.cooldownSeconds * 1000L);
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    private long getRemainingCooldown(ServerPlayer player) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        Object lastMessageTime = playerDataManager.getSetting(player.getUUID(), "last_message_time");
        if (lastMessageTime == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - ((Number) lastMessageTime).longValue();
        long cooldownMs = config.privateMessages.cooldownSeconds * 1000L;
        return Math.max(0, (cooldownMs - elapsed) / 1000);
    }
    
    /**
     * Set message cooldown
     */
    private void setMessageCooldown(ServerPlayer player) {
        playerDataManager.setSetting(player.getUUID(), "last_message_time", System.currentTimeMillis());
    }
    
    /**
     * Send message to social spy users
     */
    private void sendToSocialSpy(ServerPlayer sender, ServerPlayer target, String message) {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        String spyMessage = MessageUtil.replacePlaceholders(config.messages.pmFormatSocialSpy,
            sender.getName().getString(), target.getName().getString(), message);
        
        for (Map.Entry<UUID, Boolean> entry : socialSpyEnabled.entrySet()) {
            if (entry.getValue()) {
                ServerPlayer spy = getPlayerByUuid(entry.getKey());
                if (spy != null && !spy.getUUID().equals(sender.getUUID()) && !spy.getUUID().equals(target.getUUID())) {
                    MessageUtil.sendMessage(spy, spyMessage);
                }
            }
        }
    }
    
    /**
     * Check if a player is ignoring another player
     */
    private boolean isPlayerIgnored(UUID player, UUID ignored) {
        @SuppressWarnings("unchecked")
        List<String> ignoreList = (List<String>) playerDataManager.getSetting(player, "ignore_list");
        if (ignoreList == null) {
            return false;
        }
        return ignoreList.contains(ignored.toString());
    }
    
    /**
     * Add player to ignore list
     */
    private void addToIgnoreList(UUID player, UUID ignored) {
        @SuppressWarnings("unchecked")
        List<String> ignoreList = (List<String>) playerDataManager.getSetting(player, "ignore_list");
        if (ignoreList == null) {
            ignoreList = new ArrayList<>();
        }
        ignoreList.add(ignored.toString());
        playerDataManager.setSetting(player, "ignore_list", ignoreList);
    }
    
    /**
     * Remove player from ignore list
     */
    private void removeFromIgnoreList(UUID player, UUID ignored) {
        @SuppressWarnings("unchecked")
        List<String> ignoreList = (List<String>) playerDataManager.getSetting(player, "ignore_list");
        if (ignoreList != null) {
            ignoreList.remove(ignored.toString());
            playerDataManager.setSetting(player, "ignore_list", ignoreList);
        }
    }
    
    /**
     * Get mail count for player
     */
    private int getMailCount(UUID playerUuid) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mails = (List<Map<String, Object>>) playerDataManager.getSetting(playerUuid, "mails");
        return mails != null ? mails.size() : 0;
    }
    
    /**
     * Save mail for player
     */
    private void saveMail(UUID playerUuid, MailEntry mail) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mails = (List<Map<String, Object>>) playerDataManager.getSetting(playerUuid, "mails");
        if (mails == null) {
            mails = new ArrayList<>();
        }
        
        Map<String, Object> mailData = Map.of(
            "id", mail.id.toString(),
            "senderUuid", mail.senderUuid.toString(),
            "senderName", mail.senderName,
            "message", mail.message,
            "timestamp", mail.timestamp,
            "read", mail.read
        );
        
        mails.add(mailData);
        playerDataManager.setSetting(playerUuid, "mails", mails);
    }
    
    /**
     * Get player's mail
     */
    @SuppressWarnings("unchecked")
    private List<MailEntry> getPlayerMail(UUID playerUuid) {
        List<Map<String, Object>> mailsData = (List<Map<String, Object>>) playerDataManager.getSetting(playerUuid, "mails");
        if (mailsData == null) {
            return new ArrayList<>();
        }
        
        List<MailEntry> mails = new ArrayList<>();
        for (Map<String, Object> mailData : mailsData) {
            MailEntry mail = new MailEntry(
                UUID.fromString((String) mailData.get("id")),
                UUID.fromString((String) mailData.get("senderUuid")),
                (String) mailData.get("senderName"),
                playerUuid,
                "", // Target name not stored
                (String) mailData.get("message"),
                ((Number) mailData.get("timestamp")).longValue(),
                (Boolean) mailData.get("read")
            );
            mails.add(mail);
        }
        
        return mails;
    }
    
    /**
     * Update mail entry
     */
    private void updateMail(UUID playerUuid, MailEntry updatedMail) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mails = (List<Map<String, Object>>) playerDataManager.getSetting(playerUuid, "mails");
        if (mails == null) {
            return;
        }
        
        for (Map<String, Object> mailData : mails) {
            if (updatedMail.id.toString().equals(mailData.get("id"))) {
                mailData.put("read", updatedMail.read);
                break;
            }
        }
        
        playerDataManager.setSetting(playerUuid, "mails", mails);
    }
    
    /**
     * Clear all mail for player
     */
    private void clearPlayerMail(UUID playerUuid) {
        playerDataManager.setSetting(playerUuid, "mails", new ArrayList<>());
    }
    
    /**
     * Broadcast message to all players
     */
    private void broadcastToAll(String message) {
        // This would need server access to broadcast to all players
        LOGGER.info("BROADCAST: {}", message);
    }
    
    /**
     * Get player by name - placeholder
     */
    private ServerPlayer getPlayerByName(String name) {
        // This would need server access
        return null;
    }
    
    /**
     * Get player by UUID - placeholder
     */
    private ServerPlayer getPlayerByUuid(UUID uuid) {
        // This would need server access
        return null;
    }
    
    /**
     * Get player UUID by name - placeholder
     */
    private UUID getPlayerUuidByName(String name) {
        // This would need access to player database
        return null;
    }
    
    /**
     * Load data on startup
     */
    public void loadData() {
        // Load social spy settings and other persistent data
        LOGGER.info("Loading messaging data...");
    }
    
    /**
     * Process automatic announcements
     */
    public void processAnnouncements() {
        MessagingConfig config = configUnifier.getConfigManager().getMessagingConfig();
        
        // For now, announcements are handled through broadcast system
        // This method would be improved with announcement-specific configuration
        if (!config.isBroadcastEnabled()) {
            return;
        }
        
        // Future enhancement: Add automatic announcement system
    }
    
    // Data classes
    public static class MailEntry {
        public final UUID id;
        public final UUID senderUuid;
        public final String senderName;
        public final UUID targetUuid;
        public final String targetName;
        public final String message;
        public final long timestamp;
        public boolean read;
        
        public MailEntry(UUID id, UUID senderUuid, String senderName, UUID targetUuid, 
                        String targetName, String message, long timestamp, boolean read) {
            this.id = id;
            this.senderUuid = senderUuid;
            this.senderName = senderName;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.message = message;
            this.timestamp = timestamp;
            this.read = read;
        }
    }
    
    public static class Announcement {
        public final String message;
        
        public Announcement(String message) {
            this.message = message;
        }
    }
}
