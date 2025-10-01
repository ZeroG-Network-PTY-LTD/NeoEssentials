package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ChatFormatter handles chat message formatting using configurable templates.
 * 
 * Supports placeholders:
 *   - {DISPLAYNAME} - Player's display name
 *   - {USERNAME} - Player's username
 *   - {MESSAGE} - The chat message
 *   - {PREFIX} - Player's permission prefix (if available)
 *   - {SUFFIX} - Player's permission suffix (if available)
 *   - {WORLD} - Player's current world name
 *   - {X}, {Y}, {Z} - Player's coordinates (rounded)
 *   - {HEALTH} - Player's current health (rounded)
 *   - {LEVEL} - Player's experience level
 * 
 * Color codes are supported using & formatting (e.g., &c for red)
 */
public class ChatFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormatter.class);
    
    // Pattern to match placeholders in format strings
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Z_]+)\\}");
    
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
            // First replace placeholders
            String formatted = replacePlaceholders(template, player, message);
            
            // Then process color codes and convert to Component
            return parseColorCodes(formatted);
            
        } catch (Exception e) {
            LOGGER.error("Failed to format chat message for player {}: {}", player.getName().getString(), e.getMessage(), e);
            // Fallback to simple format if formatting fails
            return Component.literal(player.getName().getString() + ": " + message);
        }
    }
    
    /**
     * Replaces all placeholders in the template with actual values from player context.
     */
    private static String replacePlaceholders(String template, ServerPlayer player, String message) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getPlaceholderValue(placeholder, player, message);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Gets the replacement value for a specific placeholder.
     */
    private static String getPlaceholderValue(String placeholder, ServerPlayer player, String message) {
        switch (placeholder) {
            case "DISPLAYNAME":
                return player.getDisplayName().getString();
            case "USERNAME":
                return player.getName().getString();
            case "MESSAGE":
                return message;
            case "PREFIX":
                return getPlayerPrefix(player);
            case "SUFFIX":
                return getPlayerSuffix(player);
            case "WORLD":
                return player.level().dimension().location().getPath();
            case "X":
                return String.valueOf((int) player.getX());
            case "Y":
                return String.valueOf((int) player.getY());
            case "Z":
                return String.valueOf((int) player.getZ());
            case "HEALTH":
                return String.valueOf((int) player.getHealth());
            case "LEVEL":
                return String.valueOf(player.experienceLevel);
            default:
                LOGGER.warn("Unknown placeholder: {}", placeholder);
                return "{" + placeholder + "}"; // Return as-is if unknown
        }
    }
    
    /**
     * Gets the player's permission prefix (integrates with permission system).
     */
    private static String getPlayerPrefix(ServerPlayer player) {
        try {
            // Try to get prefix from permission system
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.getExternalAdapter() != null) {
                String prefix = com.zerog.neoessentials.api.permissions.PermissionAPI.getExternalAdapter().getPrefix(player.getUUID());
                return prefix != null ? prefix : "";
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get prefix for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "";
    }
    
    /**
     * Gets the player's permission suffix (integrates with permission system).
     */
    private static String getPlayerSuffix(ServerPlayer player) {
        try {
            // Try to get suffix from permission system
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.getExternalAdapter() != null) {
                String suffix = com.zerog.neoessentials.api.permissions.PermissionAPI.getExternalAdapter().getSuffix(player.getUUID());
                return suffix != null ? suffix : "";
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get suffix for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "";
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
        return "{DISPLAYNAME}: {MESSAGE}";
    }
}