package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for sending formatted messages to players
 * Supports Minecraft color codes and placeholders
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MessageUtil {
    
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    /**
     * Send a formatted message to a player with placeholder replacement
     */
    public static void sendMessage(ServerPlayer player, String message, Object... placeholders) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        
        String processedMessage = replacePlaceholders(message, placeholders);
        Component formattedMessage = formatMessage(processedMessage);
        player.sendSystemMessage(formattedMessage);
    }
    
    /**
     * Send a translatable message to a player with arguments
     */
    public static void sendTranslatedMessage(ServerPlayer player, String translationKey, Object... args) {
        if (player == null || translationKey == null || translationKey.isEmpty()) {
            return;
        }
        
        Component translatableMessage = Component.translatable(translationKey, args);
        player.sendSystemMessage(translatableMessage);
    }
    
    /**
     * Send a formatted message to multiple players with placeholder replacement
     */
    public static void sendMessage(Iterable<ServerPlayer> players, String message, Object... placeholders) {
        String processedMessage = replacePlaceholders(message, placeholders);
        Component formattedMessage = formatMessage(processedMessage);
        for (ServerPlayer player : players) {
            if (player != null) {
                player.sendSystemMessage(formattedMessage);
            }
        }
    }
    
    /**
     * Send a formatted message to a player
     */
    public static void sendMessage(ServerPlayer player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        
        Component formattedMessage = formatMessage(message);
        player.sendSystemMessage(formattedMessage);
    }
    
    /**
     * Send a formatted message to multiple players
     */
    public static void sendMessage(Iterable<ServerPlayer> players, String message) {
        Component formattedMessage = formatMessage(message);
        for (ServerPlayer player : players) {
            if (player != null) {
                player.sendSystemMessage(formattedMessage);
            }
        }
    }
    
    /**
     * Replace placeholders in message with actual values
     * Supports both {0}, {1}, {2}... and %s formatting
     */
    public static String replacePlaceholders(String message, Object... placeholders) {
        if (message == null || placeholders == null || placeholders.length == 0) {
            return message;
        }
        
        String result = message;
        
        // Replace {0}, {1}, {2}, etc. placeholders
        for (int i = 0; i < placeholders.length; i++) {
            String placeholder = "{" + i + "}";
            String value = placeholders[i] != null ? placeholders[i].toString() : "null";
            result = result.replace(placeholder, value);
        }
        
        // Also support %s formatting for compatibility
        try {
            if (result.contains("%s") && placeholders.length > 0) {
                // Convert all arguments to strings
                String[] stringArgs = new String[placeholders.length];
                for (int i = 0; i < placeholders.length; i++) {
                    stringArgs[i] = placeholders[i] != null ? placeholders[i].toString() : "null";
                }
                result = String.format(result, (Object[]) stringArgs);
            }
        } catch (Exception e) {
            // If formatting fails, return the original message with {i} replacements
            // This prevents crashes from malformed format strings
        }
        
        return result;
    }
    
    /**
     * Format a message with color codes and hex colors
     */
    public static Component formatMessage(String message) {
        if (message == null) {
            return Component.empty();
        }
        
        // Replace color codes
        String formatted = translateColorCodes(message);
        
        // Create component from formatted text
        return Component.literal(formatted);
    }
    
    /**
     * Translate color codes (&0-9, &a-f, &k-o, &r) to formatting codes
     */
    public static String translateColorCodes(String message) {
        if (message == null) {
            return "";
        }
        
        // Handle hex colors (&#RRGGBB) -> §x§R§R§G§G§B§B
        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            StringBuilder mcHex = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                mcHex.append('§').append(c);
            }
            hexMatcher.appendReplacement(hexBuffer, mcHex.toString());
        }
        hexMatcher.appendTail(hexBuffer);
        message = hexBuffer.toString();
        
        // Handle standard color codes
        Matcher colorMatcher = COLOR_PATTERN.matcher(message);
        StringBuffer colorBuffer = new StringBuffer();
        while (colorMatcher.find()) {
            String code = colorMatcher.group(1).toLowerCase();
            ChatFormatting formatting = getFormattingByCode(code);
            if (formatting != null) {
                colorMatcher.appendReplacement(colorBuffer, formatting.toString());
            } else {
                colorMatcher.appendReplacement(colorBuffer, colorMatcher.group(0));
            }
        }
        colorMatcher.appendTail(colorBuffer);
        
        return colorBuffer.toString();
    }
    
    /**
     * Get ChatFormatting by color code
     */
    private static ChatFormatting getFormattingByCode(String code) {
        return switch (code) {
            case "0" -> ChatFormatting.BLACK;
            case "1" -> ChatFormatting.DARK_BLUE;
            case "2" -> ChatFormatting.DARK_GREEN;
            case "3" -> ChatFormatting.DARK_AQUA;
            case "4" -> ChatFormatting.DARK_RED;
            case "5" -> ChatFormatting.DARK_PURPLE;
            case "6" -> ChatFormatting.GOLD;
            case "7" -> ChatFormatting.GRAY;
            case "8" -> ChatFormatting.DARK_GRAY;
            case "9" -> ChatFormatting.BLUE;
            case "a" -> ChatFormatting.GREEN;
            case "b" -> ChatFormatting.AQUA;
            case "c" -> ChatFormatting.RED;
            case "d" -> ChatFormatting.LIGHT_PURPLE;
            case "e" -> ChatFormatting.YELLOW;
            case "f" -> ChatFormatting.WHITE;
            case "k" -> ChatFormatting.OBFUSCATED;
            case "l" -> ChatFormatting.BOLD;
            case "m" -> ChatFormatting.STRIKETHROUGH;
            case "n" -> ChatFormatting.UNDERLINE;
            case "o" -> ChatFormatting.ITALIC;
            case "r" -> ChatFormatting.RESET;
            default -> null;
        };
    }
    
    /**
     * Format time duration in milliseconds to human-readable format
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "0s";
        }
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Format time duration in seconds to human-readable format
     */
    public static String formatTimeSeconds(long seconds) {
        return formatTime(seconds * 1000);
    }
    
    /**
     * Replace placeholders in message
     */
    public static String replacePlaceholders(String message, String... replacements) {
        if (message == null || replacements.length % 2 != 0) {
            return message;
        }
        
        String result = message;
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String replacement = replacements[i + 1];
            if (placeholder != null && replacement != null) {
                result = result.replace(placeholder, replacement);
            }
        }
        
        return result;
    }
    
    /**
     * Strip color codes from message
     */
    public static String stripColors(String message) {
        if (message == null) {
            return "";
        }
        
        // Remove hex colors
        message = HEX_PATTERN.matcher(message).replaceAll("");
        
        // Remove standard color codes
        message = COLOR_PATTERN.matcher(message).replaceAll("");
        
        return message;
    }
    
    /**
     * Check if message contains color codes
     */
    public static boolean hasColors(String message) {
        if (message == null) {
            return false;
        }
        
        return COLOR_PATTERN.matcher(message).find() || HEX_PATTERN.matcher(message).find();
    }
    
    /**
     * Send action bar message to player
     */
    public static void sendActionBar(ServerPlayer player, String message) {
        if (player == null || message == null) {
            return;
        }
        
        Component component = formatMessage(message);
        player.displayClientMessage(component, true);
    }
    
    /**
     * Send title to player
     */
    public static void sendTitle(ServerPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        
        Component titleComponent = title != null ? formatMessage(title) : Component.empty();
        Component subtitleComponent = subtitle != null ? formatMessage(subtitle) : Component.empty();
        
        // Note: This would need proper implementation with packet sending
        // For now, this is a placeholder
        player.sendSystemMessage(titleComponent);
        if (subtitle != null && !subtitle.isEmpty()) {
            player.sendSystemMessage(subtitleComponent);
        }
    }
    
    /**
     * Broadcast message to all online players
     */
    public static void broadcast(String message) {
        // This would need server instance to get all players
        // Placeholder implementation
    }
    
    /**
     * Format number with thousands separators
     */
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }
    
    /**
     * Format decimal number with thousands separators
     */
    public static String formatNumber(double number) {
        return String.format("%,.2f", number);
    }
}
