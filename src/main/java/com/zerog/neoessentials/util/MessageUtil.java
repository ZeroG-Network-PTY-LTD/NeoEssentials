package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.TimeUnit;
// Removed unused imports
// import com.zerog.neoessentials.util.ColorService;
// import com.zerog.neoessentials.util.ColorPermission;

/**
 * Utility class for sending formatted messages to players
 * Supports Minecraft color codes and placeholders
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MessageUtil {
    // Centralized color service instance (should be initialized from mod setup)
    private static ColorService colorService = new ColorService(
        java.util.Map.of(
            "secondary", "#FF0000"
        ),
        true, // allowLegacyCodes
        true, // allowLegacyRGB
        new ColorPermission()
    );
    /**
     * Send a simple test message to a player to verify color support
     */
    public static void sendColorTest(ServerPlayer player) {
        if (player == null) return;
        Component test = Component.literal("").append(Component.literal("&aGreen &cRed &eYellow &bAqua").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" &cRed").withStyle(ChatFormatting.RED))
            .append(Component.literal(" &eYellow").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" &bAqua").withStyle(ChatFormatting.AQUA));
        player.displayClientMessage(test, false);
    }
    
    // Patterns are now handled by ColorService
    
    /**
     * Send a formatted message to a player with placeholder replacement
     */
    public static void sendMessage(ServerPlayer player, String message, Object... placeholders) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        String processedMessage = replacePlaceholders(message, placeholders);
        Component formattedMessage = colorService.applyUserFormatting(player, processedMessage);
        player.displayClientMessage(formattedMessage, false);
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
    public static void sendMessage(Iterable<ServerPlayer> players, String message) {
        for (ServerPlayer player : players) {
            if (player != null) {
                Component formattedMessage = colorService.applyUserFormatting(player, message);
                player.displayClientMessage(formattedMessage, false);
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
    // Deprecated: use ColorService for formatting
    public static Component formatMessage(String message) {
        return Component.literal(message);
    }
    // Legacy switch statement for color codes
    
    /**
     * Format time duration in milliseconds to human-readable format
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "0s";
        }
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        if (minutes < 60) {
            return minutes + "m " + (seconds % 60) + "s";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
    }
    
    /**
     * Strip color codes from message
     */
    public static String stripColors(String message) {
        if (message == null) {
            return "";
        }
        // Remove legacy color codes and hex codes
        return message.replaceAll("&([0-9a-fk-orA-FK-OR])", "").replaceAll("&#([A-Fa-f0-9]{6})", "");
    }
    
    /**
     * Check if message contains color codes
     */
    public static boolean hasColors(String message) {
        if (message == null) {
            return false;
        }
        return message.matches(".*(&[0-9a-fk-orA-FK-OR]|&#[A-Fa-f0-9]{6}).*");
    }
    
    /**
     * Send action bar message to player
     */
    public static void sendActionBar(ServerPlayer player, String message) {
        if (player == null || message == null) {
            return;
        }
        Component component = colorService.applyUserFormatting(player, message);
        player.displayClientMessage(component, true);
    }
    
    /**
     * Send title to player
     */
    public static void sendTitle(ServerPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        Component titleComponent = title != null ? colorService.applyUserFormatting(player, title) : Component.empty();
        Component subtitleComponent = subtitle != null ? colorService.applyUserFormatting(player, subtitle) : Component.empty();
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
