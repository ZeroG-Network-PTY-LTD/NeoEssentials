package com.zerog.neoessentials.chat;

import com.zerog.neoessentials.api.PlaceholderAPI;
import net.minecraft.server.level.ServerPlayer;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormatter.class);
    
    // Pattern to match color codes (&1, &c, etc.)
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    
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
            
            // Use PlaceholderAPI to resolve all placeholders
            String formatted = PlaceholderAPI.setPlaceholders(player, templateWithMessage);
            
            // Clean up extra spaces caused by empty prefixes/suffixes
            formatted = cleanupFormatting(formatted);
            
            // Then process color codes and convert to Component
            return parseColorCodes(formatted);
            
        } catch (Exception e) {
            LOGGER.error("Failed to format chat message for player {}: {}", player.getName().getString(), e.getMessage(), e);
            // Fallback to simple format if formatting fails
            return Component.literal(player.getName().getString() + ": " + message);
        }
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
     * Converts color codes (&1, &c, etc.) to Minecraft formatting and creates a Component.
     */
    private static Component parseColorCodes(String text) {
        // Convert & color codes to § for Minecraft processing
        String converted = COLOR_PATTERN.matcher(text).replaceAll("§$1");
        
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