package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ChatFormatter handles chat message formatting with proper color code support.
 * Supports both legacy (&) and section (§) color codes, plus hex colors (&#RRGGBB).
 * Phase 2 Enhancements:
 * - Clickable URLs (auto-detection)
 * - @mention system with sound notifications
 * - [item] links showing held items
 */
public class ChatFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormatter.class);
    
    // Pre-compiled regex patterns for performance
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern AMPERSAND_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-f])");
    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile("&([k-or])");
    // Named-color / format tags (stripped from player messages without permission)
    private static final Pattern NAMED_TAG_PATTERN = Pattern.compile(
        "<(black|dark_blue|dark_green|dark_aqua|dark_cyan|dark_red|dark_purple|gold|"
        + "gray|grey|dark_gray|dark_grey|blue|green|aqua|cyan|red|light_purple|pink|yellow|white|"
        + "bold|b|italic|i|underline|underlined|u|strikethrough|s|obfuscated|magic|reset|r|color"
        + "|hover|click|gradient|rainbow)([^>]*)>",
        Pattern.CASE_INSENSITIVE);
    
    // Phase 2: Enhancement patterns
    @SuppressWarnings("RegExpDuplicateCharacterInClass") // Period in char class is intentional
    private static final Pattern URL_PATTERN = Pattern.compile(
        "\\b(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%.]+)\\b",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{3,16})\\b");
    private static final Pattern ITEM_PATTERN = Pattern.compile("\\[item]", Pattern.CASE_INSENSITIVE);
    /**
     * Formats a chat message using the provided template and player context.
     */
    public static Component formatMessage(String template, ServerPlayer player, String message) {
        try {
            boolean debugEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isDebugLoggingEnabled();

            if (debugEnabled) {
                LOGGER.info("=== CHAT FORMATTING DEBUG ===");
                LOGGER.info("Player: {}, OP: {}", player.getName().getString(), player.hasPermissions(2));
                LOGGER.info("Original message: [{}]", message);
                LOGGER.info("Template: [{}]", template);
            }

            // Normalize placeholders to new format
            String normalizedTemplate = normalizePlaceholders(template);
            if (debugEnabled) {
                LOGGER.debug("After normalization: {}", normalizedTemplate);
            }

            // Inject clickable player-name placeholder when feature is enabled
            if (isClickablePlayerNamesEnabled()) {
                normalizedTemplate = normalizedTemplate
                    .replace("{neoessentials_username}", "{neoessentials_username_hover}")
                    .replace("{neoessentials_displayname}", "{neoessentials_displayname_hover}");
            }

            // Phase 3: Apply badges and icons to template
            normalizedTemplate = BadgeManager.getInstance().applyBadgesAndIcons(player, normalizedTemplate);
            if (debugEnabled) {
                LOGGER.debug("After badges/icons: {}", normalizedTemplate);
            }

            // Restrict colors in message BEFORE inserting into template
            String restrictedMessage = restrictPlayerMessageColors(message, player);
            if (debugEnabled) {
                LOGGER.info("After color restriction: [{}]", restrictedMessage);
            }

            // Directly replace {MESSAGE} before PlaceholderAPI processing
            String preFormatted = normalizedTemplate.replace("{MESSAGE}", restrictedMessage);
            if (debugEnabled) {
                LOGGER.info("After message insertion: [{}]", preFormatted);
            }

            // Resolve all other placeholders via PlaceholderAPI
            String formatted = com.zerog.neoessentials.api.PlaceholderAPI.setPlaceholders(player, preFormatted);
            if (debugEnabled) {
                LOGGER.info("After placeholder resolution: [{}]", formatted);
            }

            // Phase 4: Apply conditional formatting
            formatted = ConditionalFormatter.processConditionals(player, formatted);
            if (debugEnabled) {
                LOGGER.info("After conditional formatting: [{}]", formatted);
            }

            // Clean up formatting
            formatted = cleanupFormatting(formatted);
            if (debugEnabled) {
                LOGGER.info("After cleanup: [{}]", formatted);
            }

            // Phase 4: Pre-process rich text tags (gradient/rainbow → &#RRGGBB hex codes).
            // We keep the result as a String so that & color codes are preserved for the
            // enhancement phase.  componentToFormattedString() used to call getString()
            // here, which stripped all formatting and produced white-only chat output.
            String richPreProcessed = RichTextFormatter.preprocessTags(formatted);
            if (debugEnabled) {
                LOGGER.info("After rich text tag pre-processing: [{}]", richPreProcessed);
            }

            // Apply Phase 2 enhancements if enabled
            Component result;
            if (isChatEnhancementsEnabled()) {
                // Pass the string with & color codes intact; buildComponentFromMarkup will
                // call parseColorCodes on each plain-text segment so all & and &#RRGGBB
                // codes are honoured correctly.
                result = enhanceMessage(richPreProcessed, player, player.getServer());
            } else {
                // No enhancements – do full rich text processing (parseColorCodes included).
                result = RichTextFormatter.processRichText(formatted);
            }

            if (debugEnabled) {
                LOGGER.info("=== END CHAT FORMATTING DEBUG ===");
            }
            return result;

        } catch (Exception e) {
            LOGGER.error("Failed to format chat message for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            // Fallback
            return Component.literal(player.getName().getString() + ": " + message);
        }
    }

    /**
     * Restrict color codes in player's message based on config and permissions.
     * Returns the message with disallowed color codes removed.
     */
    private static String restrictPlayerMessageColors(String message, ServerPlayer player) {
        UUID uuid = player.getUUID();
        String result = message;
        boolean debugEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isDebugLoggingEnabled();

        if (debugEnabled) {
            LOGGER.info(">>> Restricting colors for player {} (UUID: {})", player.getName().getString(), uuid);
            LOGGER.info(">>> Original message: [{}]", message);
        }

        // First check if color codes are enabled globally in config
        boolean colorCodesEnabled = com.zerog.neoessentials.config.ConfigManager.isColorCodesEnabled();
        if (debugEnabled) {
            LOGGER.info(">>> Config enable-color-codes: {}", colorCodesEnabled);
        }

        if (!colorCodesEnabled) {
            // Strip ALL color codes if disabled in config
            result = HEX_PATTERN.matcher(result).replaceAll("");
            result = AMPERSAND_CODE_PATTERN.matcher(result).replaceAll("");
            if (debugEnabled) {
                LOGGER.info(">>> Color codes DISABLED in config - Stripped all codes: [{}]", result);
            }
            return result;
        }
        
        // Config allows colors, now check permissions
        boolean hasHexPerm = PermissionAPI.hasPermission(uuid, "neoessentials.chat.color.hex");
        boolean hasColorPerm = PermissionAPI.hasPermission(uuid, "neoessentials.chat.color");
        boolean hasFormatPerm = PermissionAPI.hasPermission(uuid, "neoessentials.chat.format");
        
        if (debugEnabled) {
            LOGGER.info(">>> Permission Check Results:");
            LOGGER.info(">>>   - neoessentials.chat.color.hex: {}", hasHexPerm);
            LOGGER.info(">>>   - neoessentials.chat.color: {}", hasColorPerm);
            LOGGER.info(">>>   - neoessentials.chat.format: {}", hasFormatPerm);
        }

        if (!hasHexPerm) {
            if (debugEnabled) {
                String before = result;
                result = HEX_PATTERN.matcher(result).replaceAll("");
                LOGGER.info(">>>   Stripped hex codes: [{}] -> [{}]", before, result);
            } else {
                result = HEX_PATTERN.matcher(result).replaceAll("");
            }
        }
        
        if (!hasColorPerm) {
            if (debugEnabled) {
                String before = result;
                result = COLOR_CODE_PATTERN.matcher(result).replaceAll("");
                LOGGER.info(">>>   Stripped color codes: [{}] -> [{}]", before, result);
            } else {
                result = COLOR_CODE_PATTERN.matcher(result).replaceAll("");
            }
        }
        
        if (!hasFormatPerm) {
            if (debugEnabled) {
                String before = result;
                result = FORMAT_CODE_PATTERN.matcher(result).replaceAll("");
                LOGGER.info(">>>   Stripped format codes: [{}] -> [{}]", before, result);
            } else {
                result = FORMAT_CODE_PATTERN.matcher(result).replaceAll("");
            }
        }

        // Strip named color/format tags (e.g. <red>, <bold>) unless player has appropriate perm
        boolean hasNamedTagPerm = PermissionAPI.hasPermission(uuid, "neoessentials.chat.namedcolors");
        if (!hasNamedTagPerm) {
            // Also strip hover/click tags (those always require namedcolors perm in player messages)
            result = NAMED_TAG_PATTERN.matcher(result).replaceAll("");
            // Strip matching close-tags
            result = result.replaceAll("</(\\w+)>", "");
        }
        
        if (debugEnabled) {
            LOGGER.info(">>> Final restricted message: [{}]", result);
        }
        return result;
    }
    
    /**
     * Parse text with color codes to Minecraft Component.
     * Supports: §/& color codes (0-9, a-f), format codes (k-o, r), and hex (&#RRGGBB)
     * Note: This method is kept for fallback scenarios when ChatEnhancer is disabled.
     */
    @SuppressWarnings("unused") // Used as fallback when enhancements disabled
    private static Component parseToComponent(String text) {
        MutableComponent result = Component.empty();
        
        // First convert & to § for uniform processing (using pre-compiled pattern)
        text = AMPERSAND_CODE_PATTERN.matcher(text).replaceAll("§$1");
        
        // Handle hex colors: &#RRGGBB -> RGB color
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hexMatcher.find()) {
            try {
                String hex = hexMatcher.group(1);
                // Replace with placeholder that we'll process later
                hexMatcher.appendReplacement(sb, "§#" + hex + "§");
            } catch (Exception e) {
                hexMatcher.appendReplacement(sb, "");
            }
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();
        
        // Now parse the text character by character, building Components
        StringBuilder currentText = new StringBuilder();
        net.minecraft.network.chat.Style currentStyle = net.minecraft.network.chat.Style.EMPTY;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                
                // Handle hex color: §#RRGGBB§
                if (code == '#' && i + 8 < text.length() && text.charAt(i + 8) == '§') {
                    // Flush current text
                    if (!currentText.isEmpty()) {
                        result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                        currentText = new StringBuilder();
                    }
                    
                    try {
                        String hex = text.substring(i + 2, i + 8);
                        int rgb = Integer.parseInt(hex, 16);
                        currentStyle = currentStyle.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb));
                    } catch (Exception e) {
                        // Ignore invalid hex
                    }
                    i += 8; // Skip the hex color code
                    continue;
                }
                
                // Handle standard color codes
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    // Flush current text
                    if (!currentText.isEmpty()) {
                        result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                        currentText = new StringBuilder();
                    }
                    
                    // Apply the formatting
                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = net.minecraft.network.chat.Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = net.minecraft.network.chat.Style.EMPTY.applyFormat(formatting);
                    } else {
                        // Format codes (bold, italic, etc)
                        currentStyle = currentStyle.applyFormat(formatting);
                    }
                    
                    i++; // Skip the code character
                    continue;
                }
            }
            
            currentText.append(c);
        }
        
        // Append any remaining text
        if (!currentText.isEmpty()) {
            result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
        }
        
        return result;
    }
    
    /**
     * Convert legacy uppercase placeholders to lowercase format.
     */
    private static String normalizePlaceholders(String template) {
        return template
            .replace("{DISPLAYNAME}", "{neoessentials_displayname}")
            .replace("{USERNAME}", "{neoessentials_username}")
            .replace("{PREFIX}", "{neoessentials_prefix}")
            .replace("{SUFFIX}", "{neoessentials_suffix}")
            .replace("{WORLD}", "{neoessentials_world}")
            .replace("{X}", "{neoessentials_x}")
            .replace("{Y}", "{neoessentials_y}")
            .replace("{Z}", "{neoessentials_z}")
            .replace("{HEALTH}", "{neoessentials_health}")
            .replace("{LEVEL}", "{neoessentials_level}")
            .replace("{BALANCE}", "{neoessentials_balance}")
            .replace("{GAMEMODE}", "{neoessentials_gamemode}")
            .replace("{BIOME}", "{neoessentials_biome}");
    }
    
    /**
     * Clean up extra spaces from empty prefixes/suffixes.
     */
    private static String cleanupFormatting(String formatted) {
        formatted = formatted.replaceAll("\\s+", " ");
        formatted = formatted.replaceAll("< >", "");
        formatted = formatted.replaceAll("<\\s+", "<");
        formatted = formatted.replaceAll("\\s+>", ">");
        return formatted.trim();
    }
    
    /**
     * Validate if a format template is well-formed.
     */
    @SuppressWarnings("unused") // Public API method - may be used by other plugins/mods
    public static boolean isValidTemplate(String template) {
        if (template == null || template.trim().isEmpty()) {
            return false;
        }
        
        // Check balanced braces
        int openBraces = 0;
        for (char c : template.toCharArray()) {
            if (c == '{') openBraces++;
            else if (c == '}') openBraces--;
            if (openBraces < 0) return false;
        }
        
        return openBraces == 0;
    }
    
    /**
     * Get the default chat format template.
     */
    @SuppressWarnings("unused") // Public API method - may be used by other plugins/mods
    public static String getDefaultFormat() {
        return "{neoessentials_prefix}{neoessentials_displayname}{neoessentials_suffix}: {MESSAGE}";
    }

    // ============================================================
    // Phase 2: Interactive Chat Enhancements
    // ============================================================

    /**
     * Check if chat enhancements are enabled in config.
     */
    private static boolean isChatEnhancementsEnabled() {
        try {
            return com.zerog.neoessentials.config.ConfigManager.getInstance()
                .getConfig("chat").get("enableChatEnhancements").getAsBoolean();
        } catch (Exception e) {
            return true; // Default enabled
        }
    }

    /**
     * Enhance a formatted message with interactive components.
     */
    private static Component enhanceMessage(String formattedMessage, ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        try {
            // Process [item] links first
            String processed = processItemLinks(formattedMessage, player);
            // Then build interactive component with URLs and mentions
            return buildInteractiveComponent(processed, player, server);
        } catch (Exception e) {
            LOGGER.error("Error enhancing chat message: {}", e.getMessage(), e);
            return com.zerog.neoessentials.util.ChatComponentUtil.parseColorCodes(formattedMessage);
        }
    }

    /**
     * Process [item] placeholders and replace with item name.
     */
    private static String processItemLinks(String message, ServerPlayer player) {
        if (!isItemLinksEnabled()) {
            return message;
        }

        Matcher matcher = ITEM_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            var mainHandItem = player.getMainHandItem();
            if (!mainHandItem.isEmpty()) {
                String itemName = mainHandItem.getHoverName().getString();
                matcher.appendReplacement(result, "§ITEM§" + itemName + "§/ITEM§");
            } else {
                matcher.appendReplacement(result, "[Empty Hand]");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Build interactive component with clickable URLs, mentions, and item links.
     */
    private static Component buildInteractiveComponent(String text, ServerPlayer sender, net.minecraft.server.MinecraftServer server) {
        String processed = text;

        // Process URLs first
        if (isUrlDetectionEnabled()) {
            processed = markupUrls(processed);
        }

        // Then process mentions
        if (isMentionsEnabled()) {
            processed = markupMentions(processed, sender, server);
        }

        // Build final component from marked-up text
        return buildComponentFromMarkup(processed, sender);
    }

    /**
     * Markup URLs for later component creation.
     */
    private static String markupUrls(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String url = matcher.group(1);
            matcher.appendReplacement(result, "§URL§" + url + "§/URL§");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Markup mentions and play sounds.
     */
    private static String markupMentions(String text, ServerPlayer sender, net.minecraft.server.MinecraftServer server) {
        Matcher matcher = MENTION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String mentionedName = matcher.group(1);
            ServerPlayer mentioned = server.getPlayerList().getPlayerByName(mentionedName);

            if (mentioned != null) {
                matcher.appendReplacement(result, "§MENTION§" + mentionedName + "§/MENTION§");

                // Play sound if enabled and not self-mention
                if (isMentionSoundEnabled() && !mentioned.getUUID().equals(sender.getUUID())) {
                    playMentionSound(mentioned);
                }
            } else {
                matcher.appendReplacement(result, "@" + mentionedName);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Build final component from marked-up text.
     */
    private static Component buildComponentFromMarkup(String markup, ServerPlayer sender) {
        MutableComponent result = Component.empty();
        int index = 0;

        while (index < markup.length()) {
            // Check for ITEM marker
            int itemStart = markup.indexOf("§ITEM§", index);
            if (itemStart == index) {
                int itemEnd = markup.indexOf("§/ITEM§", itemStart);
                if (itemEnd != -1) {
                    String itemName = markup.substring(itemStart + 6, itemEnd);
                    result.append(createItemComponent(itemName, sender));
                    index = itemEnd + 7;
                    continue;
                }
            }

            // Check for URL marker
            int urlStart = markup.indexOf("§URL§", index);
            if (urlStart == index) {
                int urlEnd = markup.indexOf("§/URL§", urlStart);
                if (urlEnd != -1) {
                    String url = markup.substring(urlStart + 5, urlEnd);
                    result.append(com.zerog.neoessentials.util.ChatComponentUtil.createClickableUrl(url, url, "Click to open in browser\n" + url));
                    index = urlEnd + 6;
                    continue;
                }
            }

            // Check for MENTION marker
            int mentionStart = markup.indexOf("§MENTION§", index);
            if (mentionStart == index) {
                int mentionEnd = markup.indexOf("§/MENTION§", mentionStart);
                if (mentionEnd != -1) {
                    String playerName = markup.substring(mentionStart + 9, mentionEnd);
                    result.append(createMentionComponent(playerName));
                    index = mentionEnd + 10;
                    continue;
                }
            }

            // Find next marker
            int nextMarker = markup.length();
            int[] markers = {
                markup.indexOf("§ITEM§", index),
                markup.indexOf("§URL§", index),
                markup.indexOf("§MENTION§", index)
            };

            for (int m : markers) {
                if (m != -1 && m < nextMarker) nextMarker = m;
            }

            // Add plain text segment
            if (nextMarker > index) {
                String plainText = markup.substring(index, nextMarker);
                result.append(com.zerog.neoessentials.util.ChatComponentUtil.parseColorCodes(plainText));
                index = nextMarker;
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Create item component using ChatComponentUtil as base.
     */
    private static Component createItemComponent(String itemName, ServerPlayer player) {
        var mainHandItem = player.getMainHandItem();

        MutableComponent component = Component.literal("[" + itemName + "]")
            .withStyle(ChatFormatting.AQUA)
            .withStyle(ChatFormatting.UNDERLINE);

        if (!mainHandItem.isEmpty()) {
            component.setStyle(component.getStyle()
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                    new HoverEvent.ItemStackInfo(mainHandItem)))
            );
        }

        return component;
    }

    /**
     * Create mention component using consistent styling.
     */
    private static Component createMentionComponent(String playerName) {
        ChatFormatting color = getMentionColor();

        return Component.literal("@" + playerName)
            .withStyle(color)
            .withStyle(ChatFormatting.BOLD)
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + playerName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to message " + playerName).withStyle(ChatFormatting.GRAY)))
            );
    }

    /**
     * Play mention sound to a player.
     */
    private static void playMentionSound(ServerPlayer player) {
        try {
            float volume = getMentionSoundVolume();
            player.playNotifySound(
                net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS,
                volume,
                1.0f
            );
        } catch (Exception e) {
            LOGGER.debug("Failed to play mention sound: {}", e.getMessage());
        }
    }

    // Config helper methods

    private static boolean isItemLinksEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("allowItemLinks")) {
                return chatConfig.get("allowItemLinks").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private static boolean isUrlDetectionEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("autoLinkUrls")) {
                return chatConfig.get("autoLinkUrls").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private static boolean isMentionsEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("mentions") && chatConfig.getAsJsonObject("mentions").has("enabled")) {
                return chatConfig.getAsJsonObject("mentions").get("enabled").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private static boolean isMentionSoundEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("mentions") && chatConfig.getAsJsonObject("mentions").has("playSound")) {
                return chatConfig.getAsJsonObject("mentions").get("playSound").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private static ChatFormatting getMentionColor() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("mentions") && chatConfig.getAsJsonObject("mentions").has("highlightColor")) {
                String color = chatConfig.getAsJsonObject("mentions").get("highlightColor").getAsString();
                if (color.startsWith("&") && color.length() == 2) {
                    char code = color.charAt(1);
                    ChatFormatting formatting = ChatFormatting.getByCode(code);
                    if (formatting != null) return formatting;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return ChatFormatting.YELLOW;
    }

    private static float getMentionSoundVolume() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("mentions") && chatConfig.getAsJsonObject("mentions").has("soundVolume")) {
                return chatConfig.getAsJsonObject("mentions").get("soundVolume").getAsFloat();
            }
        } catch (Exception e) {
            // Ignore
        }
        return 1.0f;
    }

    /**
     * Returns true if clickable player names are enabled in the chat config.
     * Config key: chat.clickablePlayerNames
     */
    private static boolean isClickablePlayerNamesEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("clickablePlayerNames")) {
                return chatConfig.get("clickablePlayerNames").getAsBoolean();
            }
        } catch (Exception ignored) {
            // Default to enabled on any error
        }
        return true;
    }
}
