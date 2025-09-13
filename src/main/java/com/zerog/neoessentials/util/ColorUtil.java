package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color Utility - Returns Components for proper text formatting with support for both legacy and hex colors
 */
public class ColorUtil {
    
    // Pattern for hex colors (#RRGGBB or &#RRGGBB)
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    
    /**
     * Process color codes in text and return as Component
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Convert legacy & codes to § codes first
        text = text.replace('&', '§');
        
        // Process hex colors first
        text = processHexColors(text);
        
        // Convert to Component with legacy color support
        return parseToComponent(text);
    }
    
    /**
     * Convert hex color codes to legacy format
     */
    private static String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            String hexCode = matcher.group(1);
            
            // For now, convert hex to closest legacy color (simplified)
            ChatFormatting closest = getClosestLegacyColor(hexCode);
            sb.append(closest.toString());
            
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        
        return sb.toString();
    }
    
    /**
     * Parse text with legacy color codes to Component
     */
    private static Component parseToComponent(String text) {
        if (!text.contains("§")) {
            return Component.literal(text);
        }
        
        MutableComponent component = Component.empty();
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = Style.EMPTY;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '§' && i + 1 < text.length()) {
                // Add current text segment if any
                if (currentText.length() > 0) {
                    component.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                    currentText = new StringBuilder();
                }
                
                // Parse formatting code
                char formatCode = text.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(formatCode);
                
                if (formatting != null) {
                    if (formatting.isColor()) {
                        // Reset style and apply color
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else {
                        // Apply formatting (bold, italic, etc.)
                        currentStyle = applyFormatting(currentStyle, formatting);
                    }
                }
                
                i++; // Skip the format code character
            } else {
                currentText.append(c);
            }
        }
        
        // Add remaining text
        if (currentText.length() > 0) {
            component.append(Component.literal(currentText.toString()).setStyle(currentStyle));
        }
        
        return component;
    }
    
    /**
     * Apply formatting to existing style
     */
    private static Style applyFormatting(Style style, ChatFormatting formatting) {
        switch (formatting) {
            case BOLD:
                return style.withBold(true);
            case ITALIC:
                return style.withItalic(true);
            case UNDERLINE:
                return style.withUnderlined(true);
            case STRIKETHROUGH:
                return style.withStrikethrough(true);
            case OBFUSCATED:
                return style.withObfuscated(true);
            case RESET:
                return Style.EMPTY;
            default:
                return style;
        }
    }
    
    /**
     * Get closest legacy color for a hex code
     */
    private static ChatFormatting getClosestLegacyColor(String hexCode) {
        // Simplified mapping - convert hex to closest ChatFormatting color
        int rgb = Integer.parseInt(hexCode, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // Simple logic to map to closest legacy color
        if (r > 200 && g < 100 && b < 100) return ChatFormatting.RED;
        if (r < 100 && g > 200 && b < 100) return ChatFormatting.GREEN;
        if (r < 100 && g < 100 && b > 200) return ChatFormatting.BLUE;
        if (r > 200 && g > 200 && b < 100) return ChatFormatting.YELLOW;
        if (r > 150 && g < 100 && b > 150) return ChatFormatting.LIGHT_PURPLE;
        if (r < 100 && g > 150 && b > 150) return ChatFormatting.AQUA;
        if (r > 200 && g > 200 && b > 200) return ChatFormatting.WHITE;
        if (r < 100 && g < 100 && b < 100) return ChatFormatting.DARK_GRAY;
        
        return ChatFormatting.WHITE; // Default
    }
    
    /**
     * Strip all color codes from text
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }
    
    /**
     * Strip all color codes from text - alias method
     */
    public static String stripColors(String text) {
        return stripColor(text);
    }
    
    /**
     * Convert hex colors to legacy codes - simplified version
     */
    public static String hexToLegacy(String text) {
        if (text == null) return "";
        // For now, just strip hex codes
        return text.replaceAll("#[0-9a-fA-F]{6}", "");
    }
}
