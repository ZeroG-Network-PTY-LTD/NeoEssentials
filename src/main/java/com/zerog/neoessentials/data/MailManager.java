package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mail messages between players.
 */
public class MailManager {
    // Map of player UUID to their mail messages
    private final Map<UUID, List<MailMessage>> playerMail = new ConcurrentHashMap<>();
    private final File mailFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Creates a new MailManager.
     *
     * @param dataFolder The folder to store mail data in
     */
    public MailManager(File dataFolder) {
        this.mailFile = new File(dataFolder, "mail.json");
        loadMail();
    }
    
    /**
     * Loads the mail from the mail.json file.
     */
    private void loadMail() {
        try {
            if (!mailFile.exists()) {
                saveMail();
                return;
            }
            
            try (Reader reader = new FileReader(mailFile)) {
                Type type = new TypeToken<Map<UUID, List<MailMessage>>>() {}.getType();
                Map<UUID, List<MailMessage>> loadedMail = GSON.fromJson(reader, type);
                
                if (loadedMail != null) {
                    playerMail.clear();
                    playerMail.putAll(loadedMail);
                    
                    // Purge expired messages
                    playerMail.entrySet().removeIf(entry -> {
                        List<MailMessage> messages = entry.getValue();
                        messages.removeIf(MailMessage::isExpired);
                        return messages.isEmpty();
                    });
                    
                    NeoEssentials.LOGGER.info("Loaded mail data for {} players", playerMail.size());
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to load mail", e);
        }
    }
    
    /**
     * Saves the mail to the mail.json file.
     */
    public void saveMail() {
        try {
            if (!mailFile.getParentFile().exists() && !mailFile.getParentFile().mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create mail directory");
                return;
            }
            
            try (Writer writer = new FileWriter(mailFile)) {
                GSON.toJson(playerMail, writer);
            }
        } catch (JsonIOException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to save mail", e);
        }
    }
    
    /**
     * Sends a mail message to a player.
     *
     * @param recipientUUID The UUID of the recipient
     * @param sender The name of the sender
     * @param message The message to send
     * @param expiryDays The number of days until the message expires, or -1 for no expiry
     * @return True if the mail was sent successfully, false otherwise
     */
    public boolean sendMail(UUID recipientUUID, String sender, String message, int expiryDays) {
        MailMessage mail = new MailMessage(
                UUID.randomUUID(),
                sender,
                message,
                Instant.now().getEpochSecond(),
                expiryDays > 0 ? Instant.now().plusSeconds(expiryDays * 86400L).getEpochSecond() : -1
        );
        
        List<MailMessage> messages = playerMail.computeIfAbsent(recipientUUID, k -> new ArrayList<>());
        messages.add(mail);
        
        saveMail();
        return true;
    }
    
    /**
     * Reads a mail message for a player.
     *
     * @param player The player to read the mail for
     * @param messageId The ID of the message to read
     * @return The mail message, or null if it doesn't exist
     */
    public MailMessage readMail(ServerPlayer player, UUID messageId) {
        UUID playerUUID = player.getUUID();
        List<MailMessage> messages = playerMail.get(playerUUID);
        
        if (messages != null) {
            for (MailMessage message : messages) {
                if (message.getId().equals(messageId)) {
                    return message;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Deletes a mail message for a player.
     *
     * @param player The player to delete the mail for
     * @param messageId The ID of the message to delete
     * @return True if the message was deleted, false otherwise
     */
    public boolean deleteMail(ServerPlayer player, UUID messageId) {
        UUID playerUUID = player.getUUID();
        List<MailMessage> messages = playerMail.get(playerUUID);
        
        if (messages != null) {
            boolean removed = messages.removeIf(message -> message.getId().equals(messageId));
            
            if (removed) {
                if (messages.isEmpty()) {
                    playerMail.remove(playerUUID);
                }
                
                saveMail();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clears all mail for a player.
     *
     * @param player The player to clear mail for
     * @return The number of messages cleared
     */
    public int clearMail(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        List<MailMessage> messages = playerMail.get(playerUUID);
        
        if (messages != null) {
            int count = messages.size();
            playerMail.remove(playerUUID);
            saveMail();
            return count;
        }
        
        return 0;
    }
    
    /**
     * Gets all mail for a player.
     *
     * @param player The player to get mail for
     * @return The list of mail messages
     */
    public List<MailMessage> getMail(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        List<MailMessage> messages = playerMail.get(playerUUID);
        
        if (messages != null) {
            // Remove expired messages
            messages.removeIf(MailMessage::isExpired);
            
            if (messages.isEmpty()) {
                playerMail.remove(playerUUID);
                saveMail();
            }
            
            return new ArrayList<>(messages);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Gets the number of unread mail messages for a player.
     *
     * @param player The player to get the count for
     * @return The number of unread messages
     */
    public int getUnreadMailCount(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        List<MailMessage> messages = playerMail.get(playerUUID);
        
        if (messages != null) {
            // Remove expired messages and count unread
            messages.removeIf(MailMessage::isExpired);
            
            if (messages.isEmpty()) {
                playerMail.remove(playerUUID);
                saveMail();
                return 0;
            }
            
            int count = 0;
            for (MailMessage message : messages) {
                if (!message.isRead()) {
                    count++;
                }
            }
            
            return count;
        }
        
        return 0;
    }
    
    /**
     * Notifies a player of their unread mail.
     *
     * @param player The player to notify
     */
    public void notifyPlayer(ServerPlayer player) {
        int unreadCount = getUnreadMailCount(player);
        
        if (unreadCount > 0) {
            Component message = Component.literal(com.zerog.neoessentials.utils.TextUtil.formatText(
                    "&aYou have &6" + unreadCount + " &aunread mail message" + (unreadCount == 1 ? "" : "s") + ". Type &6/mail read &ato view them."));
            player.sendSystemMessage(message);
        }
    }
    
    /**
     * Class representing a mail message.
     */
    public static class MailMessage {
        private final UUID id;
        private final String sender;
        private final String message;
        private final long timestamp;
        private final long expiry;
        private boolean read;
        
        public MailMessage(UUID id, String sender, String message, long timestamp, long expiry) {
            this.id = id;
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
            this.expiry = expiry;
            this.read = false;
        }
        
        public UUID getId() {
            return id;
        }
        
        public String getSender() {
            return sender;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public long getExpiry() {
            return expiry;
        }
        
        public boolean isRead() {
            return read;
        }
        
        public void markAsRead() {
            this.read = true;
        }
        
        public boolean isExpired() {
            return expiry > 0 && expiry < Instant.now().getEpochSecond();
        }
        
        /**
         * Gets a formatted date string for the timestamp.
         *
         * @return The formatted date string
         */
        public String getFormattedDate() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp * 1000));
        }
    }
}
