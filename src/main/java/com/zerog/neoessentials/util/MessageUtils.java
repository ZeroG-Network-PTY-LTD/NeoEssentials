package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for formatting messages and text components
 */
public class MessageUtils {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Format a string with color codes to a Component
     */
    public static Component format(String message) {
        return parseColorCodes(message);
    }
    
    /**
     * Format a timestamp to a readable string
     */
    public static String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return TIMESTAMP_FORMATTER.format(dateTime);
    }
    
    /**
     * Parse color codes (&-codes) to Minecraft formatting
     */
    private static Component parseColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        MutableComponent component = Component.empty();
        StringBuilder current = new StringBuilder();
        ChatFormatting currentFormat = null;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '&' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                ChatFormatting format = getFormattingByCode(code);
                
                if (format != null) {
                    // Add current text with current formatting
                    if (current.length() > 0) {
                        MutableComponent part = Component.literal(current.toString());
                        if (currentFormat != null) {
                            part = part.withStyle(currentFormat);
                        }
                        component.append(part);
                        current.setLength(0);
                    }
                    
                    currentFormat = format;
                    i++; // Skip the code character
                    continue;
                }
            }
            
            current.append(c);
        }
        
        // Add remaining text
        if (current.length() > 0) {
            MutableComponent part = Component.literal(current.toString());
            if (currentFormat != null) {
                part = part.withStyle(currentFormat);
            }
            component.append(part);
        }
        
        return component;
    }
    
    /**
     * Get ChatFormatting by color code
     */
    private static ChatFormatting getFormattingByCode(char code) {
        switch (Character.toLowerCase(code)) {
            case '0': return ChatFormatting.BLACK;
            case '1': return ChatFormatting.DARK_BLUE;
            case '2': return ChatFormatting.DARK_GREEN;
            case '3': return ChatFormatting.DARK_AQUA;
            case '4': return ChatFormatting.DARK_RED;
            case '5': return ChatFormatting.DARK_PURPLE;
            case '6': return ChatFormatting.GOLD;
            case '7': return ChatFormatting.GRAY;
            case '8': return ChatFormatting.DARK_GRAY;
            case '9': return ChatFormatting.BLUE;
            case 'a': return ChatFormatting.GREEN;
            case 'b': return ChatFormatting.AQUA;
            case 'c': return ChatFormatting.RED;
            case 'd': return ChatFormatting.LIGHT_PURPLE;
            case 'e': return ChatFormatting.YELLOW;
            case 'f': return ChatFormatting.WHITE;
            case 'k': return ChatFormatting.OBFUSCATED;
            case 'l': return ChatFormatting.BOLD;
            case 'm': return ChatFormatting.STRIKETHROUGH;
            case 'n': return ChatFormatting.UNDERLINE;
            case 'o': return ChatFormatting.ITALIC;
            case 'r': return ChatFormatting.RESET;
            default: return null;
        }
    }
}
