package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.config.ChatConfig;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.commands.essentials.NickCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@net.neoforged.fml.common.EventBusSubscriber(modid = "neoessentials")
public class ChatFormattingListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormattingListener.class);

    // Anti-spam tracking (static for static event handler)
    private static final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> messageCount = new ConcurrentHashMap<>();

    /**
     * Handle server chat events for formatting
     */
    @SubscribeEvent
    public static void onServerChat(net.neoforged.neoforge.event.ServerChatEvent event) {
        LOGGER.info("[NeoEssentials] ChatFormattingListener: onServerChat event triggered. Message: {}", event.getMessage().getString());
        com.zerog.neoessentials.config.MainConfig mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig();
        if (mainConfig == null) {
            LOGGER.error("[NeoEssentials] ChatFormattingListener: MainConfig is null! Skipping ALL formatting. Letting vanilla handle chat.");
            return;
        }
    boolean chatEnabled = mainConfig.modules != null && mainConfig.modules.chat != null && mainConfig.modules.chat.enabled;
    LOGGER.info("[NeoEssentials] ChatFormattingListener: mainConfig.modules.chat.enabled = {}", chatEnabled);
        if (!chatEnabled) {
            LOGGER.info("[NeoEssentials] ChatFormattingListener: Chat is DISABLED. Cancelling event and notifying player.");
            event.setCanceled(true);
            ServerPlayer player = event.getPlayer();
            com.zerog.neoessentials.util.MessageUtil.sendMessage(player, "§cChat is currently disabled by the server administrator.");
            return;
        }
        ChatConfig config = com.zerog.neoessentials.config.ConfigManager.getInstance().getChatConfig();
        if (config == null) {
            LOGGER.error("[NeoEssentials] ChatFormattingListener: ChatConfig is null! Skipping ALL formatting. Letting vanilla handle chat.");
            return;
        }
        LOGGER.info("[NeoEssentials] ChatFormattingListener: config.isEnabled() = {}", config.isEnabled());
        if (!config.isEnabled()) {
            LOGGER.info("[NeoEssentials] ChatFormattingListener: Chat formatting is DISABLED. Bypassing ALL formatting and letting vanilla handle chat.");
            // Do not set event message, do not cancel, let vanilla handle everything
            return;
        }
        ServerPlayer player = event.getPlayer();
        String originalMessage = event.getMessage().getString();
        try {
            LOGGER.debug("[NeoEssentials] AntiSpam enabled: {}", config.antiSpam.enabled);
            if (config.antiSpam.enabled && isSpam(player, originalMessage, config)) {
                event.setCanceled(true);
                com.zerog.neoessentials.util.MessageUtil.sendMessage(player, "§cPlease slow down your chat messages!");
                LOGGER.debug("[NeoEssentials] Message blocked for spam: {}", originalMessage);
                return;
            }
            LOGGER.debug("[NeoEssentials] Filter enabled: {}", config.filter.enabled);
            String filteredMessage = config.filter.enabled ? filterMessage(originalMessage, config) : originalMessage;
            if (filteredMessage == null) {
                event.setCanceled(true);
                com.zerog.neoessentials.util.MessageUtil.sendMessage(player, "§cYour message was blocked by the chat filter!");
                LOGGER.debug("[NeoEssentials] Message blocked by filter: {}", originalMessage);
                return;
            }
            String playerName = player.getName().getString();
            String nickname = getPlayerNickname(player);
            String format = config.format;
            if (format == null || format.isEmpty()) {
                format = "{MESSAGE}";
            }
            String displayName = playerName;
            LOGGER.debug("[NeoEssentials] Nicknames enabled: {}, showInChat: {}, nickname: {}", config.nicknames.enabled, config.nicknames.showInChat, nickname);
            if (config.nicknames.enabled && config.nicknames.showInChat && nickname != null) {
                displayName = nickname;
                if (config.nicknames.allowColors) {
                    displayName = com.zerog.neoessentials.util.ColorUtil.colorize(displayName).getString();
                }
            }
            String prefix = "";
            String suffix = "";
            String group = "";
            // Only apply prefix/suffix logic if enabled in config
            LOGGER.debug("[NeoEssentials] PrefixSuffix enabled: {}", config.prefixSuffix.enabled);
            if (config.prefixSuffix.enabled) {
                if (format.contains("{PREFIX}") && config.prefixSuffix.isPermissionSystemEnabled()) {
                    prefix = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerPrefix(player.getUUID());
                    if (prefix == null) prefix = config.prefixSuffix.defaultPrefix;
                } else if (format.contains("{PREFIX}") && config.prefixSuffix.isGroupSystemEnabled()) {
                    prefix = config.prefixSuffix.defaultPrefix;
                } else if (format.contains("{PREFIX}")) {
                    prefix = config.prefixSuffix.defaultPrefix;
                }
                if (format.contains("{SUFFIX}") && config.prefixSuffix.isPermissionSystemEnabled()) {
                    suffix = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerSuffix(player.getUUID());
                    if (suffix == null) suffix = config.prefixSuffix.defaultSuffix;
                } else if (format.contains("{SUFFIX}") && config.prefixSuffix.isGroupSystemEnabled()) {
                    suffix = config.prefixSuffix.defaultSuffix;
                } else if (format.contains("{SUFFIX}")) {
                    suffix = config.prefixSuffix.defaultSuffix;
                }
                if (format.contains("{GROUP}") && config.prefixSuffix.isGroupSystemEnabled()) {
                    group = getPlayerGroup(player);
                    if (group == null) group = "";
                }
                LOGGER.debug("[NeoEssentials] Prefix: '{}', Suffix: '{}', Group: '{}'", prefix, suffix, group);
                if (!config.prefixSuffix.isColorEnabled()) {
                    prefix = prefix.replaceAll("§[0-9a-fk-or]", "");
                    suffix = suffix.replaceAll("§[0-9a-fk-or]", "");
                }
            }
            String formattedText = format
                .replace("{DISPLAYNAME}", displayName)
                .replace("{PREFIX}", prefix)
                .replace("{SUFFIX}", suffix)
                .replace("{GROUP}", group)
                .replace("{MESSAGE}", filteredMessage)
                .replace("{TIME}", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")))
                .replace("{DATE}", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            LOGGER.debug("[NeoEssentials] Final formattedText: {}", formattedText);
            if (config.enableColors) {
                event.setMessage(com.zerog.neoessentials.util.ColorUtil.colorize(formattedText));
                LOGGER.debug("[NeoEssentials] Colors enabled. Message colorized.");
            } else {
                event.setMessage(Component.literal(formattedText));
                LOGGER.debug("[NeoEssentials] Colors disabled. Message sent as plain text.");
            }
        } catch (Exception e) {
            LOGGER.error("Error formatting chat message for player: {}", player.getName().getString(), e);
            // Let the original message through on error
        }
    }

    // Helper methods now static
    private static String getPlayerGroup(ServerPlayer player) {
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

    private static String filterMessage(String message, ChatConfig config) {
        if (!config.filter.enabled) {
            return message;
        }
        String filteredMessage = message;
        for (String blockedWord : config.filter.blockedWords) {
            String pattern = config.filter.caseSensitive ? blockedWord : "(?i)" + blockedWord;
            if (config.filter.censorMode) {
                filteredMessage = filteredMessage.replaceAll(pattern, config.filter.censorReplacement);
            } else {
                if (filteredMessage.matches(".*" + pattern + ".*")) {
                    return null; // Message should be blocked
                }
            }
        }
        return filteredMessage;
    }

    private static boolean isSpam(ServerPlayer player, String message, ChatConfig config) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(playerId);
        if (lastTime != null) {
            long timeDiff = currentTime - lastTime;
            if (timeDiff < (1000 / config.antiSpam.maxMessagesPerSecond)) {
                return true; // Too fast
            }
        }
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
        lastMessageTime.put(playerId, currentTime);
        lastMessage.put(playerId, message);
        return false;
    }

    private static String getPlayerNickname(ServerPlayer player) {
        try {
            java.lang.reflect.Field nicknamesField = NickCommand.class.getDeclaredField("nicknames");
            nicknamesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, String> nicknames = (Map<UUID, String>) nicknamesField.get(null);
            return nicknames.get(player.getUUID());
        } catch (Exception e) {
            LOGGER.debug("Failed to get nickname for {}: {}", player.getName().getString(), e.getMessage());
            return null;
        }
    }
}
