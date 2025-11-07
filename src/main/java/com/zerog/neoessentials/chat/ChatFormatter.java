package com.zerog.neoessentials.chat;

import com.zerog.neoessentials.api.PlaceholderAPI;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * ChatFormatter handles chat message formatting using configurable templates.
 * 
 * Now uses PlaceholderAPI for comprehensive placeholder support including:
 *   - All default NeoEssentials placeholders ({DISPLAYNAME}, {USERNAME}, {PREFIX}, {SUFFIX}, etc.)
 *   - Custom placeholders registered by other mods
 *   - Full color code support using & formatting (e.g., &c for red)
 * 
 * The PlaceholderAPI system provides:
 *   - Consistent placeholder resolution across all chat systems
 *   - Extensibility for other mods to add custom placeholders
 *   - Proper PREFIX/SUFFIX integration with the permission system
 *   - Thread-safe placeholder processing
 */
public class ChatFormatter {
    // Pattern to match hex color codes: &#RRGGBB<text> OR just &#RRGGBB
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})(?:<([^>]+)>)?");
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormatter.class);
    
    // Pattern to match color codes (&1, &c, etc.)
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern COLOR_ONLY_PATTERN = Pattern.compile("&([0-9a-f])");
    private static final Pattern FORMAT_BOLD_PATTERN = Pattern.compile("&l");
    private static final Pattern FORMAT_ITALIC_PATTERN = Pattern.compile("&o");
    private static final Pattern FORMAT_UNDERLINE_PATTERN = Pattern.compile("&n");
    private static final Pattern FORMAT_STRIKETHROUGH_PATTERN = Pattern.compile("&m");
    private static final Pattern FORMAT_OBFUSCATED_PATTERN = Pattern.compile("&k");
    @SuppressWarnings("unused") // Reserved for future reset formatting feature
    private static final Pattern FORMAT_RESET_PATTERN = Pattern.compile("&r");
    
    /**
     * Formats a chat message using the provided template and player context.
     * 
     * @param template The format template (e.g., "{DISPLAYNAME}: {MESSAGE}")
     * @param player The player sending the message
     * @param message The raw message content
     * @return Formatted Component ready for display
     */
    public static Component formatMessage(String template, ServerPlayer player, String message) {
        try {
            // Convert legacy uppercase placeholders to neoessentials_ format for backwards compatibility
            String normalizedTemplate = normalizePlaceholders(template);

            // Add the MESSAGE placeholder to the template context if it's not already there
            String templateWithMessage = normalizedTemplate.replace("{MESSAGE}", message);

            // Use PlaceholderAPI to resolve all placeholders (including prefix/suffix from permissions)
            String formatted = PlaceholderAPI.setPlaceholders(player, templateWithMessage);

            // IMPORTANT: Process color codes from prefixes/suffixes FIRST (these come from server config/permissions)
            // Server-defined colors in prefixes/suffixes should always work regardless of player permissions
            formatted = processServerColorCodes(formatted);

            // Clean up extra spaces caused by empty prefixes/suffixes
            formatted = cleanupFormatting(formatted);

            // Now restrict color/format codes in the MESSAGE part based on player permissions
            formatted = restrictFormattingByPermission(formatted, player, message);

            // Then convert remaining & codes to § for Minecraft and create Component
            return parseColorCodes(formatted);

        } catch (Exception e) {
            LOGGER.error("Failed to format chat message for player {}: {}", player.getName().getString(), e.getMessage(), e);
            // Fallback to simple format if formatting fails
            return Component.literal(player.getName().getString() + ": " + message);
        }
    }

    /**
     * Process color codes from server-defined text (prefixes, suffixes, format template).
     * This converts & codes to § codes so they work in chat regardless of player permissions.
     * This should be called BEFORE permission checks so server-defined colors always work.
     */
    private static String processServerColorCodes(String text) {
        // Convert all & color codes to § for server-defined text
        return COLOR_PATTERN.matcher(text).replaceAll("§$1");
    }

    /**
     * Restricts color and formatting codes in the MESSAGE part only based on player permissions.
     * Only checks the player's actual message, not the prefix/suffix/format from server.
     * Strips codes from the message if not permitted.
     */
    private static String restrictFormattingByPermission(String formatted, ServerPlayer player, String originalMessage) {
        UUID uuid = player.getUUID();
        
        // Only restrict colors in the actual message part, not in prefixes/suffixes
        String restrictedMessage = originalMessage;
        
        // Hex color codes (&#RRGGBB or &#RRGGBB<text>)
        if (!PermissionAPI.hasPermission(uuid, "neoessentials.chat.color.hex")) {
            restrictedMessage = HEX_COLOR_PATTERN.matcher(restrictedMessage).replaceAll("$2"); // Remove hex if not permitted
        }
        
        // Color codes (&0-&9, &a-&f)
        if (!PermissionAPI.hasPermission(uuid, "neoessentials.chat.color")) {
            restrictedMessage = COLOR_ONLY_PATTERN.matcher(restrictedMessage).replaceAll("");
        }
        
        // Format codes
        if (!PermissionAPI.hasPermission(uuid, "neoessentials.chat.format.bold")) {
            restrictedMessage = FORMAT_BOLD_PATTERN.matcher(restrictedMessage).replaceAll("");
        }
        if (!PermissionAPI.hasPermission(uuid, "neoessentials.chat.format.italic")) {
            restrictedMessage = FORMAT_ITALIC_PATTERN.matcher(restrictedMessage).replaceAll("");
        }
        if (!PermissionAPI.hasPermission(uuid, "neoessentials.chat.format.underline")) {
            restrictedMessage = FORMAT_UNDERLINE_PATTERN.matcher(restrictedMessage).replaceAll("");
        }
        if (!PermissionAPI.hasPermission(uuid, "neoessentials.chat.format.strikethrough")) {
            restrictedMessage = FORMAT_STRIKETHROUGH_PATTERN.matcher(restrictedMessage).replaceAll("");
        }
        if (!PermissionAPI.hasPermission(uuid, "neoessentials.chat.format.obfuscated")) {
            restrictedMessage = FORMAT_OBFUSCATED_PATTERN.matcher(restrictedMessage).replaceAll("");
        }
        
        // Replace the original message with the restricted version
        // Need to escape regex special characters in originalMessage
        String escapedOriginal = Pattern.quote(originalMessage);
        return formatted.replaceFirst(escapedOriginal, java.util.regex.Matcher.quoteReplacement(restrictedMessage));
    }
    
    /**
     * Converts legacy uppercase placeholders to neoessentials_ format.
     * This ensures backwards compatibility with old config files.
     * 
     * Examples:
     *   {USERNAME} -> {neoessentials_username}
     *   {PREFIX} -> {neoessentials_prefix}
     *   {SUFFIX} -> {neoessentials_suffix}
     */
    private static String normalizePlaceholders(String template) {
        // Map of legacy placeholders to new format
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
     * Cleans up formatting by removing extra spaces and fixing empty placeholders.
     */
    private static String cleanupFormatting(String formatted) {
        // Clean up extra spaces caused by empty prefixes/suffixes
        formatted = formatted.replaceAll("\\s+", " "); // Replace multiple spaces with single space
        formatted = formatted.replaceAll("< >", "<>"); // Fix empty brackets with just spaces
        formatted = formatted.replaceAll("<\\s+", "<"); // Remove spaces after opening brackets
        formatted = formatted.replaceAll("\\s+>", ">"); // Remove spaces before closing brackets
        
        return formatted.trim();
    }
    
    /**
     * Converts color codes (&1, &c, etc.) and hex codes (&#RRGGBB or &#RRGGBB<text>) to Minecraft formatting and creates a Component.
     */
    private static Component parseColorCodes(String text) {
        // First, process hex color codes: &#RRGGBB<text> or just &#RRGGBB
        StringBuffer sb = new StringBuffer();
        java.util.regex.Matcher matcher = HEX_COLOR_PATTERN.matcher(text);
        int lastEnd = 0;
        net.minecraft.network.chat.Style currentStyle = net.minecraft.network.chat.Style.EMPTY;
        
        while (matcher.find()) {
            // Append text before the match with current style
            if (lastEnd < matcher.start()) {
                sb.append(text, lastEnd, matcher.start());
            }
            
            String hex = matcher.group(1);
            String inner = matcher.group(2); // Will be null if no <text> part
            
            try {
                int rgb = Integer.parseInt(hex, 16);
                currentStyle = net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb));
                
                if (inner != null) {
                    // Format: &#RRGGBB<text> - apply color to the inner text only
                    sb.append(net.minecraft.network.chat.Component.literal(inner).withStyle(currentStyle).getString());
                } else {
                    // Format: &#RRGGBB - set the color for all following text (like a persistent color code)
                    // This needs to be handled by converting it to a § code
                    sb.append("§x§" + hex.charAt(0) + "§" + hex.charAt(1) + "§" + hex.charAt(2) + "§" + hex.charAt(3) + "§" + hex.charAt(4) + "§" + hex.charAt(5));
                }
            } catch (Exception e) {
                // Fallback: just append the inner text or continue
                if (inner != null) {
                    sb.append(inner);
                }
            }
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        
        // Now the text has hex colors converted, process remaining & color codes to § for Minecraft processing
        String converted = sb.toString();
        // Don't re-process & codes that were already converted to § in processServerColorCodes
        // Only convert remaining & codes (from player messages with permissions)
        converted = converted.replaceAll("&([0-9a-fk-or])", "§$1");
        
        // Create component with legacy formatting support
        return Component.literal(converted);
    }
    
    /**
     * Validates if a format template is safe and well-formed.
     * 
     * @param template The format template to validate
     * @return true if the template is valid, false otherwise
     */
    public static boolean isValidTemplate(String template) {
        if (template == null || template.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Check for balanced braces
            int openBraces = 0;
            for (char c : template.toCharArray()) {
                if (c == '{') openBraces++;
                else if (c == '}') openBraces--;
                if (openBraces < 0) return false; // More closing than opening
            }
            
            // Should have balanced braces
            if (openBraces != 0) return false;
            
            // Should contain at least {MESSAGE} placeholder
            if (!template.contains("{MESSAGE}")) {
                LOGGER.warn("Chat format template should contain {{MESSAGE}} placeholder: {}", template);
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error validating chat format template: {}", template, e);
            return false;
        }
    }
    
    /**
     * Gets the default chat format template.
     */
    public static String getDefaultFormat() {
        return "{neoessentials_displayname}: {MESSAGE}";
    }
}