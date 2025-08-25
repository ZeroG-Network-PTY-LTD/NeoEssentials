package com.zerog.neoessentials.managers;

import net.minecraft.network.chat.Component;

import com.zerog.neoessentials.config.ConfigurationUnifier;
// import removed: MessagingConfig is now centralized in MainConfig
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.localization.LanguageManager;
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
    // ...existing code...
        com.zerog.neoessentials.config.MainConfig mainConfig = configUnifier.getConfigManager().getMainConfig();
        boolean chatEnabled = mainConfig != null && mainConfig.modules != null && mainConfig.modules.chat;
        if (!chatEnabled) {
            MessageUtil.sendTranslatedMessage(sender, "chat.disabled");
            return false;
        }
        // Check permission
        if (!PermissionUtil.hasPermission(sender, PermissionNodes.MSG)) {
            MessageUtil.sendTranslatedMessage(sender, "no.permission");
            return false;
        }
        // Find target player
        ServerPlayer target = getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendTranslatedMessage(sender, "player.not.found", targetName);
            return false;
        }
        // Check if target is ignoring sender
        if (isPlayerIgnored(target.getUUID(), sender.getUUID())) {
            MessageUtil.sendTranslatedMessage(sender, "player.ignoring.you", target.getName().getString());
            return false;
        }
        // Check cooldown
        if (isOnMessageCooldown(sender)) {
            MessageUtil.sendTranslatedMessage(sender, "message.cooldown", String.valueOf(getRemainingCooldown(sender)));
            return false;
        }
        // Send messages
        MessageUtil.sendTranslatedMessage(sender, "pm.format.sender", target.getName().getString(), message);
        MessageUtil.sendTranslatedMessage(target, "pm.format.receiver", sender.getName().getString(), message);
        // Update last messaged
        lastMessaged.put(sender.getUUID(), target.getUUID());
        lastMessaged.put(target.getUUID(), sender.getUUID());
        // Social spy
        sendToSocialSpy(sender, target, message);
        // Set cooldown
        setMessageCooldown(sender);
        LOGGER.info("Private message from {} to {}: {}", sender.getName().getString(), target.getName().getString(), message);
        return true;
    }
    
    /**
     * Reply to the last received private message
     */
    public boolean replyToMessage(ServerPlayer sender, String message) {
    // ...existing code...
        
        UUID lastSenderUuid = lastMessaged.get(sender.getUUID());
        if (lastSenderUuid == null) {
            MessageUtil.sendTranslatedMessage(sender, "no.reply.target");
            return false;
        }
        ServerPlayer target = getPlayerByUuid(lastSenderUuid);
        if (target == null) {
            MessageUtil.sendTranslatedMessage(sender, "player.offline");
            return false;
        }
        return sendPrivateMessage(sender, target.getName().getString(), message);
    }
    
    /**
     * Send mail to a player
     */
    public boolean sendMail(ServerPlayer sender, String targetName, String message) {
        com.zerog.neoessentials.config.MainConfig mainConfig = configUnifier.getConfigManager().getMainConfig();
        boolean chatEnabled = mainConfig != null && mainConfig.modules != null && mainConfig.modules.chat;
        if (!chatEnabled) {
            MessageUtil.sendTranslatedMessage(sender, "chat.disabled");
            return false;
        }
        // Check permission
        if (!PermissionUtil.hasPermission(sender, PermissionNodes.MAIL_SEND)) {
            MessageUtil.sendTranslatedMessage(sender, "no.permission");
            return false;
        }
        // Get target UUID (could be offline player)
        UUID targetUuid = getPlayerUuidByName(targetName);
        if (targetUuid == null) {
            MessageUtil.sendTranslatedMessage(sender, "player.not.found", targetName);
            return false;
        }
        // Check mail limit (default: 50 mails per player)
        int mailCount = getMailCount(targetUuid);
        int maxMailsPerPlayer = 50;
        if (mailCount >= maxMailsPerPlayer) {
            MessageUtil.sendTranslatedMessage(sender, "mail.full", targetName);
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
        MessageUtil.sendTranslatedMessage(sender, "mail.sent", targetName, message);
        // Notify target if online
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target != null) {
            MessageUtil.sendTranslatedMessage(target, "mail.received", sender.getName().getString());
        }
        LOGGER.info("Mail sent from {} to {}: {}", sender.getName().getString(), targetName, message);
        return true;
    }
    
    /**
     * Read player's mail
     */
    public void readMail(ServerPlayer player) {
        List<MailEntry> mails = getPlayerMail(player.getUUID());
        if (mails.isEmpty()) {
            MessageUtil.sendTranslatedMessage(player, "mail.empty");
            return;
        }
        int maxMailsPerPlayer = 50;
        MessageUtil.sendTranslatedMessage(player, "mail.list.header", String.valueOf(mails.size()), String.valueOf(maxMailsPerPlayer));
        for (int i = 0; i < mails.size(); i++) {
            MailEntry mail = mails.get(i);
            String timeStr = MessageUtil.formatTime(System.currentTimeMillis() - mail.timestamp);
            MessageUtil.sendTranslatedMessage(player, "mail.read", timeStr, mail.senderName, mail.message);
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
    // ...existing code...
        List<MailEntry> mails = getPlayerMail(player.getUUID());
        if (mails.isEmpty()) {
            MessageUtil.sendTranslatedMessage(player, "mail.empty");
            return false;
        }
        clearPlayerMail(player.getUUID());
        MessageUtil.sendTranslatedMessage(player, "mail.clear", String.valueOf(mails.size()));
        return true;
    }
    
    /**
     * Broadcast a message to all players
     */
    public void broadcast(ServerPlayer sender, String message) {
    // ...existing code...
        com.zerog.neoessentials.config.MainConfig mainConfig = configUnifier.getConfigManager().getMainConfig();
        boolean chatEnabled = mainConfig != null && mainConfig.modules != null && mainConfig.modules.chat;
        if (!chatEnabled) {
            MessageUtil.sendTranslatedMessage(sender, "chat.disabled");
            return;
        }
        if (!PermissionUtil.hasPermission(sender, PermissionNodes.BROADCAST)) {
            MessageUtil.sendTranslatedMessage(sender, "no.permission");
            return;
        }
        broadcastToAll(Component.translatable("broadcast.received", message).getString());
        LOGGER.info("Broadcast by {}: {}", sender.getName().getString(), message);
    }
    
    /**
     * Toggle social spy for a player
     */
    public boolean toggleSocialSpy(ServerPlayer player) {
    // ...existing code...
        if (!PermissionUtil.hasPermission(player, PermissionNodes.SOCIALSPY)) {
            MessageUtil.sendTranslatedMessage(player, "no.permission");
            return false;
        }
        boolean currentState = socialSpyEnabled.getOrDefault(player.getUUID(), false);
        boolean newState = !currentState;
        socialSpyEnabled.put(player.getUUID(), newState);
        playerDataManager.setSetting(player.getUUID(), "socialspy_enabled", newState);
        if (newState) {
            MessageUtil.sendTranslatedMessage(player, "socialspy.on");
        } else {
            MessageUtil.sendTranslatedMessage(player, "socialspy.off");
        }
        return newState;
    }
    
    /**
     * Ignore a player
     */
    public boolean ignorePlayer(ServerPlayer player, String targetName) {
    // ...existing code...
        com.zerog.neoessentials.config.MainConfig mainConfig = configUnifier.getConfigManager().getMainConfig();
        boolean chatEnabled = mainConfig != null && mainConfig.modules != null && mainConfig.modules.chat;
        if (!chatEnabled) {
            MessageUtil.sendTranslatedMessage(player, "chat.disabled");
            return false;
        }
        ServerPlayer target = getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendTranslatedMessage(player, "player.not.found", targetName);
            return false;
        }
        if (target.getUUID().equals(player.getUUID())) {
            MessageUtil.sendTranslatedMessage(player, "cannot.ignore.self");
            return false;
        }
        // Check if already ignoring
        if (isPlayerIgnored(player.getUUID(), target.getUUID())) {
            MessageUtil.sendTranslatedMessage(player, "already.ignoring", target.getName().getString());
            return false;
        }
        // Add to ignore list
        addToIgnoreList(player.getUUID(), target.getUUID());
        MessageUtil.sendTranslatedMessage(player, "ignore.added", target.getName().getString());
        return true;
    }
    
    /**
     * Unignore a player
     */
    public boolean unignorePlayer(ServerPlayer player, String targetName) {
    // ...existing code...
        UUID targetUuid = getPlayerUuidByName(targetName);
        if (targetUuid == null) {
            MessageUtil.sendTranslatedMessage(player, "player.not.found", targetName);
            return false;
        }
        if (!isPlayerIgnored(player.getUUID(), targetUuid)) {
            MessageUtil.sendTranslatedMessage(player, "not.ignoring", targetName);
            return false;
        }
        // Remove from ignore list
        removeFromIgnoreList(player.getUUID(), targetUuid);
        MessageUtil.sendTranslatedMessage(player, "ignore.removed", targetName);
        return true;
    }
    
    /**
     * Check if player is on message cooldown
     */
    private boolean isOnMessageCooldown(ServerPlayer player) {
        int cooldownSeconds = 5; // Default cooldown
        if (cooldownSeconds <= 0) {
            return false;
        }
        if (PermissionUtil.hasPermission(player, PermissionNodes.BYPASS_COOLDOWN_COMMAND)) {
            return false;
        }
        Object lastMessageTime = playerDataManager.getSetting(player.getUUID(), "last_message_time");
        if (lastMessageTime == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - ((Number) lastMessageTime).longValue();
        return elapsed < (cooldownSeconds * 1000L);
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    private long getRemainingCooldown(ServerPlayer player) {
        int cooldownSeconds = 5; // Default cooldown
        Object lastMessageTime = playerDataManager.getSetting(player.getUUID(), "last_message_time");
        if (lastMessageTime == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - ((Number) lastMessageTime).longValue();
        long cooldownMs = cooldownSeconds * 1000L;
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
        String spyMessage = LanguageManager.getInstance().getMessage(sender, "pm.format.socialspy", sender.getName().getString(), target.getName().getString(), message);
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
    // For now, announcements are handled through broadcast system
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
