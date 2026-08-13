package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Anti-Spam Filter Manager - Phase 3
 * Handles:
 * - Caps filter (SHOUTING detection)
 * - Repeat filter (duplicate message prevention)
 * - Link filter (URL blocking/whitelisting)
 * - Spam filter (rate limiting)
 */
public class AntiSpamManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AntiSpamManager.class);
    private static volatile AntiSpamManager instance;

    // URL pattern for link detection
    @SuppressWarnings("RegExpDuplicateCharacterInClass") // Period appears in both ._  and %. - intentional for URL matching
    private static final Pattern URL_PATTERN = Pattern.compile(
        "\\b(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%.]+)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Tracking data
    private final Map<UUID, String> lastMessages = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTimes = new ConcurrentHashMap<>();
    private final Map<UUID, MessageRateTracker> rateTrackers = new ConcurrentHashMap<>();

    private AntiSpamManager() {}

    public static AntiSpamManager getInstance() {
        if (instance == null) {
            synchronized (AntiSpamManager.class) {
                if (instance == null) {
                    instance = new AntiSpamManager();
                }
            }
        }
        return instance;
    }

    /**
     * Filter a message through all enabled anti-spam filters.
     * Returns filtered message or null if message should be blocked.
     */
    public FilterResult filterMessage(ServerPlayer player, String message) {
        if (!isAntiSpamEnabled()) {
            return new FilterResult(true, message, null);
        }

        UUID playerId = player.getUUID();

        // 1. Check spam rate limit
        FilterResult spamCheck = checkSpamFilter(player, message);
        if (!spamCheck.allowed) {
            return spamCheck;
        }

        // 2. Check repeat filter
        FilterResult repeatCheck = checkRepeatFilter(player, message);
        if (!repeatCheck.allowed) {
            return repeatCheck;
        }

        // 3. Check link filter
        FilterResult linkCheck = checkLinkFilter(player, message);
        if (!linkCheck.allowed) {
            return linkCheck;
        }

        // 4. Process caps filter (may modify message)
        FilterResult capsCheck = checkCapsFilter(player, message);
        if (!capsCheck.allowed) {
            return capsCheck;
        }

        // Update tracking data
        lastMessages.put(playerId, message);
        lastMessageTimes.put(playerId, System.currentTimeMillis());

        return new FilterResult(true, capsCheck.filteredMessage, null);
    }

    /**
     * Check spam rate limit.
     */
    private FilterResult checkSpamFilter(ServerPlayer player, String message) {
        if (!isSpamFilterEnabled()) {
            return new FilterResult(true, message, null);
        }

        var config = getSpamFilterConfig();

        // Bypass permission
        if (PermissionAPI.hasPermission(player.getUUID(), bypassPermission(config, "neoessentials.chat.spam.bypass"))) {
            return new FilterResult(true, message, null);
        }

        try {
            int maxMessages = config.get("messagesPerPeriod").getAsInt();
            int periodSeconds = config.get("periodSeconds").getAsInt();

            MessageRateTracker tracker = rateTrackers.computeIfAbsent(
                player.getUUID(),
                k -> new MessageRateTracker(maxMessages, periodSeconds)
            );

            if (!tracker.allowMessage()) {
                String action = config.get("action").getAsString();
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Spam filter triggered for {} (action={})", player.getGameProfile().name(), action);
                if ("block".equals(action)) {
                    return new FilterResult(false, null, "§cYou are sending messages too quickly! Please slow down.");
                }
            }

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error checking spam filter for " + player.getGameProfile().name(), e);
        }

        return new FilterResult(true, message, null);
    }

    /**
     * Check for repeated messages.
     */
    private FilterResult checkRepeatFilter(ServerPlayer player, String message) {
        if (!isRepeatFilterEnabled()) {
            return new FilterResult(true, message, null);
        }

        var repeatConfig = getRepeatFilterConfig();

        // Bypass permission
        if (PermissionAPI.hasPermission(player.getUUID(), bypassPermission(repeatConfig, "neoessentials.chat.repeat.bypass"))) {
            return new FilterResult(true, message, null);
        }

        try {
            UUID playerId = player.getUUID();
            String lastMessage = lastMessages.get(playerId);
            Long lastTime = lastMessageTimes.get(playerId);

            if (lastMessage != null && lastMessage.equals(message)) {
                var config = repeatConfig;
                int cooldown = config.get("cooldownSeconds").getAsInt();
                long currentTime = System.currentTimeMillis();

                if (lastTime != null && (currentTime - lastTime) < (cooldown * 1000L)) {
                    String action = config.get("action").getAsString();
                    NeoLog.debug(LOGGER, LogCategory.CHAT, "Repeat filter triggered for {} (action={})", player.getGameProfile().name(), action);
                    if ("block".equals(action)) {
                        long remainingSeconds = cooldown - ((currentTime - lastTime) / 1000);
                        return new FilterResult(false, null,
                            "§cPlease wait " + remainingSeconds + " seconds before sending the same message again.");
                    }
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error checking repeat filter for " + player.getGameProfile().name(), e);
        }

        return new FilterResult(true, message, null);
    }

    /**
     * Check for links in message.
     */
    private FilterResult checkLinkFilter(ServerPlayer player, String message) {
        if (!isLinkFilterEnabled()) {
            return new FilterResult(true, message, null);
        }

        var linkConfig = getLinkFilterConfig();

        // Bypass permission
        if (PermissionAPI.hasPermission(player.getUUID(), bypassPermission(linkConfig, "neoessentials.chat.links.bypass"))) {
            return new FilterResult(true, message, null);
        }

        try {
            var config = linkConfig;
            String action = config.get("action").getAsString();

            // Allow all links
            if ("allow".equals(action)) {
                return new FilterResult(true, message, null);
            }

            // Check if message contains URLs
            Matcher matcher = URL_PATTERN.matcher(message);
            if (matcher.find()) {
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Link detected in message from {} (action={})", player.getGameProfile().name(), action);
                // Block all links
                if ("block".equals(action)) {
                    return new FilterResult(false, null, "§cLinks are not allowed in chat!");
                }

                // Whitelist mode
                if ("whitelist".equals(action)) {
                    var whitelist = config.getAsJsonArray("whitelist");
                    String url = matcher.group(1).toLowerCase();

                    boolean allowed = false;
                    for (var element : whitelist) {
                        String domain = element.getAsString().toLowerCase();
                        if (url.contains(domain)) {
                            allowed = true;
                            break;
                        }
                    }

                    if (!allowed) {
                        NeoLog.debug(LOGGER, LogCategory.CHAT, "Link '{}' from {} not in whitelist, blocking", url, player.getGameProfile().name());
                        return new FilterResult(false, null, "§cOnly whitelisted links are allowed in chat!");
                    }
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error checking link filter for " + player.getGameProfile().name(), e);
        }

        return new FilterResult(true, message, null);
    }

    /**
     * Check and filter excessive caps.
     */
    private FilterResult checkCapsFilter(ServerPlayer player, String message) {
        if (!isCapsFilterEnabled()) {
            return new FilterResult(true, message, null);
        }

        var capsConfig = getCapsFilterConfig();

        // Bypass permission
        if (PermissionAPI.hasPermission(player.getUUID(), bypassPermission(capsConfig, "neoessentials.chat.caps.bypass"))) {
            return new FilterResult(true, message, null);
        }

        try {
            var config = capsConfig;
            int minLength = config.get("minimumLength").getAsInt();

            // Don't check short messages
            if (message.length() < minLength) {
                return new FilterResult(true, message, null);
            }

            // Count caps
            int totalLetters = 0;
            int capsLetters = 0;

            for (char c : message.toCharArray()) {
                if (Character.isLetter(c)) {
                    totalLetters++;
                    if (Character.isUpperCase(c)) {
                        capsLetters++;
                    }
                }
            }

            if (totalLetters == 0) {
                return new FilterResult(true, message, null);
            }

            int capsPercentage = (capsLetters * 100) / totalLetters;
            int maxPercentage = config.get("maxPercentage").getAsInt();

            if (capsPercentage > maxPercentage) {
                String action = config.get("action").getAsString();
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Caps filter triggered for {} ({}% caps, action={})", player.getGameProfile().name(), capsPercentage, action);

                if ("lowercase".equals(action)) {
                    // Convert to lowercase
                    return new FilterResult(true, message.toLowerCase(), null);
                } else if ("block".equals(action)) {
                    return new FilterResult(false, null, "§cPlease don't use excessive caps in chat!");
                } else if ("warn".equals(action)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§ePlease avoid using excessive caps in your messages."
                    ));
                    return new FilterResult(true, message, null);
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error checking caps filter for " + player.getGameProfile().name(), e);
        }

        return new FilterResult(true, message, null);
    }

    // Config helper methods

    private boolean isAntiSpamEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("antiSpam")) {
                return chatConfig.getAsJsonObject("antiSpam").get("enabled").getAsBoolean();
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error reading antiSpam.enabled, defaulting to true", e);
        }
        return true;
    }

    private boolean isSpamFilterEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("antiSpam")) {
                var antiSpam = chatConfig.getAsJsonObject("antiSpam");
                if (antiSpam.has("spamFilter")) {
                    return antiSpam.getAsJsonObject("spamFilter").get("enabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error reading antiSpam.spamFilter.enabled, defaulting to true", e);
        }
        return true;
    }

    private boolean isRepeatFilterEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("antiSpam")) {
                var antiSpam = chatConfig.getAsJsonObject("antiSpam");
                if (antiSpam.has("repeatFilter")) {
                    return antiSpam.getAsJsonObject("repeatFilter").get("enabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error reading antiSpam.repeatFilter.enabled, defaulting to true", e);
        }
        return true;
    }

    private boolean isLinkFilterEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("antiSpam")) {
                var antiSpam = chatConfig.getAsJsonObject("antiSpam");
                if (antiSpam.has("linkFilter")) {
                    return antiSpam.getAsJsonObject("linkFilter").get("enabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error reading antiSpam.linkFilter.enabled, defaulting to false", e);
        }
        return false;
    }

    private boolean isCapsFilterEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("antiSpam")) {
                var antiSpam = chatConfig.getAsJsonObject("antiSpam");
                if (antiSpam.has("capsFilter")) {
                    return antiSpam.getAsJsonObject("capsFilter").get("enabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error reading antiSpam.capsFilter.enabled, defaulting to true", e);
        }
        return true;
    }

    private JsonObject getSafeJsonObject(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || parent.get(key).isJsonNull()) return null;
        try {
            return parent.getAsJsonObject(key);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error reading JSON object for key '" + key + "'", e);
            return null;
        }
    }

    private JsonObject getSpamFilterConfig() {
        var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
        var antiSpam = getSafeJsonObject(chatConfig, "antiSpam");
        return getSafeJsonObject(antiSpam, "spamFilter");
    }

    private JsonObject getRepeatFilterConfig() {
        var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
        var antiSpam = getSafeJsonObject(chatConfig, "antiSpam");
        return getSafeJsonObject(antiSpam, "repeatFilter");
    }

    private JsonObject getLinkFilterConfig() {
        var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
        var antiSpam = getSafeJsonObject(chatConfig, "antiSpam");
        return getSafeJsonObject(antiSpam, "linkFilter");
    }

    private JsonObject getCapsFilterConfig() {
        var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
        var antiSpam = getSafeJsonObject(chatConfig, "antiSpam");
        return getSafeJsonObject(antiSpam, "capsFilter");
    }

    /** Reads a filter's configured bypassPermission, falling back to {@code fallback} if unset. */
    private String bypassPermission(JsonObject filterConfig, String fallback) {
        if (filterConfig != null && filterConfig.has("bypassPermission")) {
            try {
                return filterConfig.get("bypassPermission").getAsString();
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Error reading bypassPermission, using fallback '" + fallback + "'", e);
            }
        }
        return fallback;
    }

    /**
     * Result of filter check.
     */
    public static class FilterResult {
        public final boolean allowed;
        public final String filteredMessage;
        public final String denyReason;

        public FilterResult(boolean allowed, String filteredMessage, String denyReason) {
            this.allowed = allowed;
            this.filteredMessage = filteredMessage;
            this.denyReason = denyReason;
        }
    }

    /**
     * Tracks message rate for spam detection.
     */
    private static class MessageRateTracker {
        private final int maxMessages;
        private final long periodMs;
        private final java.util.Deque<Long> messageTimes = new java.util.concurrent.ConcurrentLinkedDeque<>();

        public MessageRateTracker(int maxMessages, int periodSeconds) {
            this.maxMessages = maxMessages;
            this.periodMs = periodSeconds * 1000L;
        }

        public boolean allowMessage() {
            long now = System.currentTimeMillis();
            long cutoff = now - periodMs;

            // Remove old messages
            messageTimes.removeIf(time -> time < cutoff);

            // Check if under limit
            if (messageTimes.size() >= maxMessages) {
                return false;
            }

            // Add this message
            messageTimes.add(now);
            return true;
        }
    }
}

