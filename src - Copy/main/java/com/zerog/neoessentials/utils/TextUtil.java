package com.zerog.neoessentials.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for text-related operations
 */
public class TextUtil {
    
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates color codes in a string (e.g., &4 for red)
     * 
     * @param text The text to translate
     * @return The text with color codes translated
     */
    public static String translateColors(String text) {
        if (text == null) {
            return null;
        }
        
        // Replace hex colors &#RRGGBB
        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(sb, "§x§" + hex.charAt(0) + "§" + hex.charAt(1) + "§" +
                                          hex.charAt(2) + "§" + hex.charAt(3) + "§" + 
                                          hex.charAt(4) + "§" + hex.charAt(5));
        }
        hexMatcher.appendTail(sb);
        
        // Replace standard color codes
        String result = sb.toString();
        return COLOR_CODE_PATTERN.matcher(result).replaceAll("§$1");
    }

    /**
     * Converts a string with color codes to a Component
     * 
     * @param text The text to convert
     * @return A Component with formatting applied
     */
    public static Component fromColoredString(String text) {
        if (text == null) {
            return Component.empty();
        }
        
        MutableComponent component = Component.literal("");
        String current = "";
        Style style = Style.EMPTY;
        
        for (int i = 0; i < text.length(); i++) {
            if (i < text.length() - 1 && text.charAt(i) == '§') {
                if (current.length() > 0) {
                    component.append(Component.literal(current).setStyle(style));
                    current = "";
                }
                
                char code = text.charAt(i + 1);
                ChatFormatting formatting = getFormattingByChar(code);
                
                if (formatting != null) {
                    if (formatting.isColor()) {
                        style = Style.EMPTY.withColor(formatting);
                    } else {                        // Apply formatting based on format type
                        if (formatting == ChatFormatting.BOLD) {
                            style = style.withBold(true);
                        } else if (formatting == ChatFormatting.ITALIC) {
                            style = style.withItalic(true);
                        } else if (formatting == ChatFormatting.UNDERLINE) {
                            style = style.withUnderlined(true);
                        } else if (formatting == ChatFormatting.STRIKETHROUGH) {
                            style = style.withStrikethrough(true);
                        } else if (formatting == ChatFormatting.OBFUSCATED) {
                            style = style.withObfuscated(true);
                        } else if (formatting == ChatFormatting.RESET) {
                            style = Style.EMPTY;
                        }
                    }
                } else if (code == 'x' && i + 13 <= text.length()) {
                    // Handle hex color format §x§r§r§g§g§b§b
                    try {
                        String hexColor = String.valueOf(text.charAt(i + 3)) +
                                         text.charAt(i + 5) +
                                         text.charAt(i + 7) +
                                         text.charAt(i + 9) +
                                         text.charAt(i + 11) +
                                         text.charAt(i + 13);
                        
                        int rgb = Integer.parseInt(hexColor, 16);
                        style = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                        i += 12; // Skip the hex color codes
                    } catch (Exception e) {
                        current += text.charAt(i);
                    }
                } else {
                    current += text.charAt(i);
                }
                
                i++; // Skip the color code character
            } else {
                current += text.charAt(i);
            }
        }
        
        if (current.length() > 0) {
            component.append(Component.literal(current).setStyle(style));
        }
        
        return component;
    }
    
    /**
     * Formats text, translating color codes if the user has permission
     * 
     * @param text The text to format
     * @return The formatted text
     */
    public static String formatText(String text) {
        if (text == null) {
            return "";
        }
        
        return translateColors(text);
    }
    
    /**
     * Formats text, translating color codes if the user has permission
     * 
     * @param text The text to format
     * @param allowFormatting Whether to allow color formatting
     * @return The formatted text
     */
    public static String formatText(String text, boolean allowFormatting) {
        if (text == null) {
            return "";
        }
        
        if (allowFormatting) {
            return translateColors(text);
        } else {
            return text;
        }
    }
    
    /**
     * Formats and colorizes a text string with color codes.
     * This is an alias for formatText with formatting allowed.
     * 
     * @param text The text to format with color codes
     * @return The formatted text
     */
    public static String colorize(String text) {
        return formatText(text, true);
    }
    
    /**
     * Convert a colorized string to a Component.
     * This is useful for sending formatted messages to players.
     * 
     * @param text The text to colorize and convert
     * @return A Component with colors and formatting applied
     */
    public static Component colorizedComponent(String text) {
        return fromColoredString(translateColors(text));
    }
    
    /**
     * Get the ChatFormatting for a character code
     */
    private static ChatFormatting getFormattingByChar(char code) {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.getChar() == code) {
                return formatting;
            }
        }
        return null;
    }
}
