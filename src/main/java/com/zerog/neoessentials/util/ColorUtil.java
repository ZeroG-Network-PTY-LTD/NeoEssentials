package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Enhanced color utility for handling both legacy and hex colors
 */
public class ColorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorUtil.class);
    
    // Patterns for different color formats
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern HEX_PATTERN_HASH = Pattern.compile("#([A-Fa-f0-9]{6})"); // #FFFFFF
    private static final Pattern HEX_PATTERN_AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})"); // &#FFFFFF
    private static final Pattern HEX_PATTERN_BRACKETS = Pattern.compile("&\\{#([A-Fa-f0-9]{6})\\}"); // &{#FFFFFF}
    
    /**
     * Convert text with color codes to a formatted Component
     * Supports:
     * - Legacy codes: &0-9, &a-f, &k-o, &r
     * - Hex colors: #FFFFFF, &#FFFFFF, &{#FFFFFF}
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        try {
            return parseColoredText(text);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse colored text: {}", text, e);
            return Component.literal(text);
        }
    }
    
    /**
     * Parse colored text into a Component with proper formatting
     */
    private static Component parseColoredText(String text) {
        MutableComponent result = Component.empty();
        
        String[] parts = splitByColorCodes(text);
        Style currentStyle = Style.EMPTY;
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            if (isColorCode(part)) {
                Style newStyle = parseColorCode(part);
                if (newStyle != null) {
                    currentStyle = mergeStyles(currentStyle, newStyle);
                }
            } else {
                if (!part.isEmpty()) {
                    result.append(Component.literal(part).setStyle(currentStyle));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Split text by color codes while preserving the codes
     */
    private static String[] splitByColorCodes(String text) {
        // Replace all color patterns with a unique delimiter, then split
        String delimiter = "§SPLIT§";
        
        // Handle hex colors first
        text = HEX_PATTERN_HASH.matcher(text).replaceAll(delimiter + "#$1" + delimiter);
        text = HEX_PATTERN_AMPERSAND.matcher(text).replaceAll(delimiter + "&#$1" + delimiter);
        text = HEX_PATTERN_BRACKETS.matcher(text).replaceAll(delimiter + "&{#$1}" + delimiter);
        
        // Handle legacy colors
        text = LEGACY_COLOR_PATTERN.matcher(text).replaceAll(delimiter + "&$1" + delimiter);
        
        return text.split(delimiter);
    }
    
    /**
     * Check if a string is a color code
     */
    private static boolean isColorCode(String text) {
        return text.startsWith("#") || text.startsWith("&");
    }
    
    /**
     * Parse a color code into a Style
     */
    private static Style parseColorCode(String colorCode) {
        try {
            // Handle hex colors
            if (colorCode.startsWith("#")) {
                return parseHexColor(colorCode.substring(1));
            } else if (colorCode.startsWith("&#")) {
                return parseHexColor(colorCode.substring(2));
            } else if (colorCode.startsWith("&{#") && colorCode.endsWith("}")) {
                return parseHexColor(colorCode.substring(3, colorCode.length() - 1));
            }
            
            // Handle legacy colors
            if (colorCode.startsWith("&") && colorCode.length() == 2) {
                return parseLegacyColor(colorCode.charAt(1));
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.debug("Failed to parse color code: {}", colorCode, e);
            return null;
        }
    }
    
    /**
     * Parse hex color string to Style
     */
    private static Style parseHexColor(String hex) {
        try {
            int color = Integer.parseInt(hex, 16);
            return Style.EMPTY.withColor(TextColor.fromRgb(color));
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid hex color: {}", hex);
            return null;
        }
    }
    
    /**
     * Parse legacy color character to Style
     */
    private static Style parseLegacyColor(char code) {
        ChatFormatting formatting = getFormattingByCode(String.valueOf(code));
        if (formatting != null) {
            if (formatting.isColor()) {
                return Style.EMPTY.withColor(formatting);
            } else {
                // Handle formatting codes (bold, italic, etc.)
                return applyFormatting(Style.EMPTY, formatting);
            }
        }
        return null;
    }
    
    /**
     * Apply formatting to a style
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
     * Merge two styles, with the second taking precedence
     */
    private static Style mergeStyles(Style base, Style overlay) {
        Style result = base;
        
        if (overlay.getColor() != null) {
            result = result.withColor(overlay.getColor());
        }
        
        // For boolean properties, we need to check if they are explicitly set
        if (overlay.isBold()) {
            result = result.withBold(true);
        }
        if (overlay.isItalic()) {
            result = result.withItalic(true);
        }
        if (overlay.isUnderlined()) {
            result = result.withUnderlined(true);
        }
        if (overlay.isStrikethrough()) {
            result = result.withStrikethrough(true);
        }
        if (overlay.isObfuscated()) {
            result = result.withObfuscated(true);
        }
        
        return result;
    }
    
    /**
     * Get ChatFormatting by character code
     */
    private static ChatFormatting getFormattingByCode(String code) {
        switch (code.toLowerCase()) {
            case "0": return ChatFormatting.BLACK;
            case "1": return ChatFormatting.DARK_BLUE;
            case "2": return ChatFormatting.DARK_GREEN;
            case "3": return ChatFormatting.DARK_AQUA;
            case "4": return ChatFormatting.DARK_RED;
            case "5": return ChatFormatting.DARK_PURPLE;
            case "6": return ChatFormatting.GOLD;
            case "7": return ChatFormatting.GRAY;
            case "8": return ChatFormatting.DARK_GRAY;
            case "9": return ChatFormatting.BLUE;
            case "a": return ChatFormatting.GREEN;
            case "b": return ChatFormatting.AQUA;
            case "c": return ChatFormatting.RED;
            case "d": return ChatFormatting.LIGHT_PURPLE;
            case "e": return ChatFormatting.YELLOW;
            case "f": return ChatFormatting.WHITE;
            case "k": return ChatFormatting.OBFUSCATED;
            case "l": return ChatFormatting.BOLD;
            case "m": return ChatFormatting.STRIKETHROUGH;
            case "n": return ChatFormatting.UNDERLINE;
            case "o": return ChatFormatting.ITALIC;
            case "r": return ChatFormatting.RESET;
            default: return null;
        }
    }
    
    /**
     * Strip all color codes from text
     */
    public static String stripColors(String text) {
        if (text == null) return null;
        
        text = HEX_PATTERN_HASH.matcher(text).replaceAll("");
        text = HEX_PATTERN_AMPERSAND.matcher(text).replaceAll("");
        text = HEX_PATTERN_BRACKETS.matcher(text).replaceAll("");
        text = LEGACY_COLOR_PATTERN.matcher(text).replaceAll("");
        
        return text;
    }
    
    /**
     * Check if text contains color codes
     */
    public static boolean hasColors(String text) {
        if (text == null) return false;
        
        return HEX_PATTERN_HASH.matcher(text).find() ||
               HEX_PATTERN_AMPERSAND.matcher(text).find() ||
               HEX_PATTERN_BRACKETS.matcher(text).find() ||
               LEGACY_COLOR_PATTERN.matcher(text).find();
    }
    
    /**
     * Convert RGB values to hex color string
     */
    public static String rgbToHex(int red, int green, int blue) {
        return String.format("#%02X%02X%02X", 
            Math.max(0, Math.min(255, red)),
            Math.max(0, Math.min(255, green)), 
            Math.max(0, Math.min(255, blue))
        );
    }
    
    /**
     * Convert hex color to RGB values
     */
    public static int[] hexToRgb(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        
        try {
            int color = Integer.parseInt(hex, 16);
            return new int[] {
                (color >> 16) & 0xFF,  // Red
                (color >> 8) & 0xFF,   // Green
                color & 0xFF           // Blue
            };
        } catch (NumberFormatException e) {
            return new int[] {255, 255, 255}; // Default to white
        }
    }
    
    /**
     * Get a gradient between two hex colors
     */
    public static String[] createGradient(String startHex, String endHex, int steps) {
        int[] startRgb = hexToRgb(startHex);
        int[] endRgb = hexToRgb(endHex);
        
        String[] gradient = new String[steps];
        
        for (int i = 0; i < steps; i++) {
            double ratio = (double) i / (steps - 1);
            
            int red = (int) (startRgb[0] + ratio * (endRgb[0] - startRgb[0]));
            int green = (int) (startRgb[1] + ratio * (endRgb[1] - startRgb[1]));
            int blue = (int) (startRgb[2] + ratio * (endRgb[2] - startRgb[2]));
            
            gradient[i] = rgbToHex(red, green, blue);
        }
        
        return gradient;
    }
}
