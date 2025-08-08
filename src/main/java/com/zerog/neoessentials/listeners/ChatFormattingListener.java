package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.config.ChatConfig;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.commands.essentials.NickCommand;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat formatting event listener for NeoEssentials
 * Handles chat message formatting with prefix/suffix and nickname support
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ChatFormattingListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormattingListener.class);
    private static ChatFormattingListener instance;
    
    // Anti-spam tracking
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> messageCount = new ConcurrentHashMap<>();
    
    // Configuration (would typically be injected)
    private ChatConfig config;
    
    private ChatFormattingListener() {
        NeoForge.EVENT_BUS.register(this);
        loadConfigFromManager(); // Load config from ConfigManager
        LOGGER.info("Chat formatting listener registered");
    }
    
    public static synchronized ChatFormattingListener getInstance() {
        if (instance == null) {
            instance = new ChatFormattingListener();
        }
        return instance;
    }
    
    /**
     * Handle server chat events for formatting
     */
    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (!config.isEnabled()) {
            return; // Chat formatting disabled
        }
        
        ServerPlayer player = event.getPlayer();
        String originalMessage = event.getMessage().getString();
        
        try {
            // Anti-spam check
            if (config.antiSpam.enabled && isSpam(player, originalMessage)) {
                event.setCanceled(true);
                MessageUtil.sendMessage(player, "§cPlease slow down your chat messages!");
                return;
            }
            
            // Filter message if enabled
            String filteredMessage = config.filter.enabled ? 
                filterMessage(originalMessage) : originalMessage;
            
            // Build formatted message
            Component formattedMessage = buildFormattedMessage(player, filteredMessage);
            
            // Replace the message
            event.setMessage(formattedMessage);
            
            LOGGER.debug("Formatted chat message for player: {}", player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Error formatting chat message for player: {}", player.getName().getString(), e);
            // Let the original message through on error
        }
    }
    
    /**
     * Build a formatted chat message with prefix/suffix and nickname
     */
    private Component buildFormattedMessage(ServerPlayer player, String message) {
        // Get player information
        String playerName = player.getName().getString();
        String nickname = getPlayerNickname(player);
        String prefix = getPlayerPrefix(player);
        String suffix = getPlayerSuffix(player);
        String group = getPlayerGroup(player);
        
        // Determine display name
        String displayName = (nickname != null && config.nicknames.showInChat) ? nickname : playerName;
        
        // Build the format string
        String format = config.format;
        if (format == null || format.isEmpty()) {
            format = config.getFullFormat();
        }
        
        // Replace placeholders
        String formattedText = format
            .replace("{PREFIX}", prefix != null ? prefix : "")
            .replace("{SUFFIX}", suffix != null ? suffix : "")
            .replace("{PLAYER}", playerName)
            .replace("{NICKNAME}", nickname != null ? nickname : playerName)
            .replace("{DISPLAYNAME}", displayName)
            .replace("{GROUP}", group != null ? group : "default")
            .replace("{MESSAGE}", message)
            .replace("{TIME}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            .replace("{DATE}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        // Apply color codes if enabled
        if (config.areColorsEnabled()) {
            formattedText = MessageUtil.translateColorCodes(formattedText);
        }
        
        // Create the component
        MutableComponent component = Component.literal(formattedText);
        
        // Add hover text for nickname if enabled
        if (nickname != null && config.nicknames.showOriginalOnHover) {
            String hoverText = config.nicknames.hoverText.replace("{PLAYER}", playerName);
            final String finalHoverText = MessageUtil.translateColorCodes(hoverText);
            component = component.withStyle(style -> style.withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(finalHoverText))
            ));
        }
        
        return component;
    }
    
    /**
     * Get player's nickname from NickCommand system
     */
    private String getPlayerNickname(ServerPlayer player) {
        try {
            // Access the static nickname map from NickCommand
            java.lang.reflect.Field nicknamesField = 
                NickCommand.class.getDeclaredField("nicknames");
            nicknamesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<UUID, String> nicknames = (Map<UUID, String>) nicknamesField.get(null);
            
            return nicknames.get(player.getUUID());
        } catch (Exception e) {
            LOGGER.debug("Failed to get nickname for {}: {}", player.getName().getString(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Get player's prefix from permission system
     */
    private String getPlayerPrefix(ServerPlayer player) {
        try {
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            if (permManager != null && config.prefixSuffix.usePermissionSystem) {
                String prefix = permManager.getPlayerPrefix(player.getUUID());
                return prefix != null ? prefix : config.prefixSuffix.defaultPrefix;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get prefix for {}: {}", player.getName().getString(), e.getMessage());
        }
        return config.prefixSuffix.defaultPrefix;
    }
    
    /**
     * Get player's suffix from permission system
     */
    private String getPlayerSuffix(ServerPlayer player) {
        try {
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            if (permManager != null && config.prefixSuffix.usePermissionSystem) {
                String suffix = permManager.getPlayerSuffix(player.getUUID());
                return suffix != null ? suffix : config.prefixSuffix.defaultSuffix;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get suffix for {}: {}", player.getName().getString(), e.getMessage());
        }
        return config.prefixSuffix.defaultSuffix;
    }
    
    /**
     * Get player's primary group
     */
    private String getPlayerGroup(ServerPlayer player) {
        try {
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            if (permManager != null) {
                return permManager.getPlayerGroup(player.getUUID());
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get group for {}: {}", player.getName().getString(), e.getMessage());
        }
        return "default";
    }
    
    /**
     * Filter message content if filtering is enabled
     */
    private String filterMessage(String message) {
        if (!config.filter.enabled) {
            return message;
        }
        
        String filteredMessage = message;
        for (String blockedWord : config.filter.blockedWords) {
            String pattern = config.filter.caseSensitive ? blockedWord : "(?i)" + blockedWord;
            if (config.filter.censorMode) {
                // Censor mode: replace with censorship characters
                filteredMessage = filteredMessage.replaceAll(pattern, config.filter.censorReplacement);
            } else {
                // Block mode: check if message contains blocked words
                if (filteredMessage.matches(".*" + pattern + ".*")) {
                    return null; // Message should be blocked
                }
            }
        }
        
        return filteredMessage;
    }
    
    /**
     * Check if message is spam
     */
    private boolean isSpam(ServerPlayer player, String message) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        
        // Check message rate
        Long lastTime = lastMessageTime.get(playerId);
        if (lastTime != null) {
            long timeDiff = currentTime - lastTime;
            if (timeDiff < (1000 / config.antiSpam.maxMessagesPerSecond)) {
                return true; // Too fast
            }
        }
        
        // Check duplicate messages
        String lastMsg = lastMessage.get(playerId);
        if (lastMsg != null && lastMsg.equals(message)) {
            Integer count = messageCount.get(playerId);
            count = count != null ? count + 1 : 1;
            messageCount.put(playerId, count);
            
            if (count > config.antiSpam.maxDuplicateMessages) {
                return true; // Too many duplicates
            }
        } else {
            messageCount.put(playerId, 1);
        }
        
        // Update tracking
        lastMessageTime.put(playerId, currentTime);
        lastMessage.put(playerId, message);
        
        return false;
    }
    
    /**
     * Load configuration from ConfigManager
     */
    private void loadConfigFromManager() {
        try {
            com.zerog.neoessentials.config.ConfigManager configManager = 
                com.zerog.neoessentials.config.ConfigManager.getInstance();
            this.config = configManager.getChatConfig();
            LOGGER.debug("Loaded chat configuration from ConfigManager");
        } catch (Exception e) {
            LOGGER.warn("Failed to load chat config from ConfigManager, using defaults: {}", e.getMessage());
            this.config = createDefaultConfig();
        }
    }
    
    /**
     * Create default configuration (fallback)
     */
    private ChatConfig createDefaultConfig() {
        ChatConfig config = new ChatConfig();
        config.enabled = true;
        config.format = "{PREFIX}{NICKNAME}{SUFFIX}: {MESSAGE}";
        config.prefixSuffix.enabled = true;
        config.nicknames.enabled = true;
        return config;
    }
    
    /**
     * Reload configuration from ConfigManager
     */
    public void reloadConfig() {
        loadConfigFromManager();
        LOGGER.info("Chat formatting configuration reloaded");
    }
    
    /**
     * Update configuration
     */
    public void updateConfig(ChatConfig newConfig) {
        this.config = newConfig;
        LOGGER.info("Chat formatting configuration updated");
    }
    
    /**
     * Get current configuration
     */
    public ChatConfig getConfig() {
        return config;
    }
}
