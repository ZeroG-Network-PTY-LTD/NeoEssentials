package com.zerog.neoessentials.listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.zerog.neoessentials.localization.LanguageManager;

/**
 * Chat formatting event listener for NeoEssentials
 * Handles chat message formatting with prefix/suffix and nickname support
 * 
 * @author ZeroG
 * @since 2.0.0
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = "neoessentials")
public class ChatFormattingListener {
    /**
     * Loads external text from a file in the config/neoessentials directory.
     * Returns the file contents as a String, or an empty string if not found or error.
     * This is production logic, not example code.
     * @param type The type of text file to load (e.g. "motd", "help", "info")
     * @return The contents of the file, or an empty string if not found/error
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormattingListener.class);

    // Anti-spam tracking (static for static event handler)
    private static final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> messageCount = new ConcurrentHashMap<>();

    /**
     * Handle server chat events for formatting
     */
    @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST, receiveCanceled = true)
    public static void onServerChat(net.neoforged.neoforge.event.ServerChatEvent event) {
            java.nio.file.Path configPath = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfigPath();
            java.io.File configFile = configPath.resolve("main.json").toFile();
            System.out.println("DEBUG config file path being used: " + configFile.getAbsolutePath());
            System.out.println("DEBUG config file exists: " + configFile.exists());
    // ...existing formatting logic...
    // At the very end, override the message
    // ...existing code...
        LOGGER.info("[NeoEssentials] ChatFormattingListener: onServerChat event triggered. Message: {}", event.getMessage().getString());
        com.zerog.neoessentials.config.MainConfig mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig();
        if (mainConfig == null) {
            LOGGER.error("[NeoEssentials] ChatFormattingListener: MainConfig is null! Skipping ALL formatting. Letting vanilla handle chat.");
            return;
        }
    boolean chatEnabled = mainConfig.modules != null && mainConfig.modules.chat;
    LOGGER.info("[NeoEssentials] ChatFormattingListener: mainConfig.modules.chat.enabled = {}", chatEnabled);
        if (!chatEnabled) {
            LOGGER.info("[NeoEssentials] ChatFormattingListener: Chat is DISABLED. Cancelling event and notifying player.");
            event.setCanceled(true);
            ServerPlayer player = event.getPlayer();
            com.zerog.neoessentials.util.MessageUtil.sendMessage(player, LanguageManager.getInstance().getMessage(player, "chat.disabled"));
            return;
        }
    com.zerog.neoessentials.config.MainConfig.ChatSettings config = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().chatSettings;
        if (config == null) {
            LOGGER.error("[NeoEssentials] ChatFormattingListener: ChatConfig is null! Skipping ALL formatting. Letting vanilla handle chat.");
            return;
        }
    LOGGER.info("[NeoEssentials] ChatFormattingListener: config.isEnabled = {}", config.isEnabled);
    if (!config.isEnabled) {
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
                com.zerog.neoessentials.util.MessageUtil.sendMessage(player, LanguageManager.getInstance().getMessage(player, "chat.spam"));
                LOGGER.debug("[NeoEssentials] Message blocked for spam: {}", originalMessage);
                return;
            }
            LOGGER.debug("[NeoEssentials] Filter enabled: {}", config.filter.enabled);
            String filteredMessage = config.filter.enabled ? filterMessage(originalMessage, config) : originalMessage;
            if (filteredMessage == null) {
                event.setCanceled(true);
                com.zerog.neoessentials.util.MessageUtil.sendMessage(player, LanguageManager.getInstance().getMessage(player, "chat.filter.blocked"));
                LOGGER.debug("[NeoEssentials] Message blocked by filter: {}", originalMessage);
                return;
            }
            // Restore full formatting logic
            String displayName = com.zerog.neoessentials.features.DisplayNameManager.getDisplayName(player);
            String group = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerGroup(player.getUUID());
            // Load group prefix/suffix from groups.json
            String prefix = "";
            String suffix = "";
            try {
                java.io.File groupConfigFile = new java.io.File("neoessentials/permissions/groups.json");
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.io.FileReader reader = new java.io.FileReader(groupConfigFile);
                java.util.Map<?,?> groupConfig = gson.fromJson(reader, java.util.Map.class);
                if (groupConfig != null && group != null && groupConfig.containsKey(group)) {
                    java.util.Map<?,?> groupData = (java.util.Map<?,?>) groupConfig.get(group);
                    prefix = groupData.containsKey("prefix") ? groupData.get("prefix").toString().replace("&", "§") : "";
                    suffix = groupData.containsKey("suffix") ? groupData.get("suffix").toString().replace("&", "§") : "";
                }
                reader.close();
            } catch (Exception e) {
                // fallback: no group config
            }
            System.out.println("DEBUG config.format: " + config.format);
            System.out.println("DEBUG config.groupFormats: " + config.groupFormats);
            System.out.println("DEBUG group: " + group);
            // Always use the main format from config
            String nameFormat = config.format;
            System.out.println("DEBUG nameFormat source: from main format");
            if (nameFormat == null || nameFormat.isEmpty()) {
                nameFormat = "{PREFIX} {DISPLAYNAME} > {MESSAGE}";
            }
            System.out.println("DEBUG nameFormat before replacements: " + nameFormat);
            System.out.println("DEBUG filteredMessage: " + filteredMessage);
            String formattedName = nameFormat
                .replace("{PREFIX}", prefix)
                .replace("{DISPLAYNAME}", displayName)
                .replace("{SUFFIX}", suffix)
                .replace("{GROUP}", group)
                .replace("{MESSAGE}", filteredMessage);
            System.out.println("DEBUG after MESSAGE replacement: " + formattedName);
            System.out.println("DEBUG filteredMessage value: '" + filteredMessage + "'");
            String formattedText = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance().processPlaceholders(formattedName, player);
            System.out.println("FINAL formattedText: " + formattedText);
            event.setCanceled(true);
            MinecraftServer server = player.getServer();
            if (server != null) {
                // Use color code processing for chat output
                net.minecraft.network.chat.Component coloredComponent = com.zerog.neoessentials.util.ColorUtil.colorize(formattedText);
                server.getPlayerList().broadcastSystemMessage(coloredComponent, false);
            }
        } catch (Exception e) {
            LOGGER.error("Error formatting chat message for player: {}", player.getName().getString(), e);
            // Let the original message through on error
        }
    }

    // Helper methods now static

    private static String filterMessage(String message, com.zerog.neoessentials.config.MainConfig.ChatSettings config) {
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

    private static boolean isSpam(ServerPlayer player, String message, com.zerog.neoessentials.config.MainConfig.ChatSettings config) {
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

    /**
     * External MOTD/help/info text response loader
     */
    public static String loadExternalText(String type) {
        java.nio.file.Path path = java.nio.file.Paths.get("config/neoessentials/" + type + ".txt");
        try {
            return java.nio.file.Files.readString(path);
        } catch (Exception e) {
            return "";
        }
    }
}
