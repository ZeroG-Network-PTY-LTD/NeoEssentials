package com.zerog.neoessentials.chat;

import com.zerog.neoessentials.api.PlaceholderAPI;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ChatFormatter handles chat message formatting with proper color code support.
 * Supports both legacy (&) and section (§) color codes, plus hex colors (&#RRGGBB).
 */
public class ChatFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormatter.class);
    
    // Pre-compiled regex patterns for performance
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern AMPERSAND_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-f])");
    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile("&([k-or])");
    
    /**
     * Formats a chat message using the provided template and player context.
     */
    public static Component formatMessage(String template, ServerPlayer player, String message) {
        try {
            LOGGER.info("=== CHAT FORMATTING DEBUG ===");
            LOGGER.info("Player: {}, OP: {}", player.getName().getString(), player.hasPermissions(2));
            LOGGER.info("Original message: [{}]", message);
            LOGGER.info("Template: [{}]", template);
            
            // Normalize placeholders to new format
            String normalizedTemplate = normalizePlaceholders(template);
            LOGGER.debug("After normalization: {}", normalizedTemplate);
            
            // Restrict colors in message BEFORE inserting into template
            String restrictedMessage = restrictPlayerMessageColors(message, player);
            LOGGER.info("After color restriction: [{}]", restrictedMessage);
            
            // Directly replace {MESSAGE} before PlaceholderAPI processing
            // This is simpler than using a temporary placeholder
            String preFormatted = normalizedTemplate.replace("{MESSAGE}", restrictedMessage);
            LOGGER.info("After message insertion: [{}]", preFormatted);
            
            // Resolve all other placeholders via PlaceholderAPI
            String formatted = PlaceholderAPI.setPlaceholders(player, preFormatted);
            LOGGER.info("After placeholder resolution: [{}]", formatted);
            
            // Clean up formatting
            formatted = cleanupFormatting(formatted);
            LOGGER.info("After cleanup: [{}]", formatted);
            LOGGER.info("Final formatted message: [{}]", formatted);
            
            // Convert to Minecraft component with colors
            Component result = parseToComponent(formatted);
            LOGGER.info("=== END CHAT FORMATTING DEBUG ===");
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
        
        if (debugEnabled) {
            LOGGER.info(">>> Final restricted message: [{}]", result);
        }
        return result;
    }
    
    /**
     * Parse text with color codes to Minecraft Component.
     * Supports: §/& color codes (0-9, a-f), format codes (k-o, r), and hex (&#RRGGBB)
     */
    private static Component parseToComponent(String text) {
        MutableComponent result = Component.empty();
        
        // First convert & to § for uniform processing (using pre-compiled pattern)
        text = AMPERSAND_CODE_PATTERN.matcher(text).replaceAll("§$1");
        
        // Handle hex colors: &#RRGGBB -> RGB color
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
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
                    if (currentText.length() > 0) {
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
                    if (currentText.length() > 0) {
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
        if (currentText.length() > 0) {
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
    public static String getDefaultFormat() {
        return "{neoessentials_prefix}{neoessentials_displayname}{neoessentials_suffix}: {MESSAGE}";
    }
}
