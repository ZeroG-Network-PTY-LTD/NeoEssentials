package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Utility class for handling color codes in text.
 * 
 * <p>This class provides methods for processing Minecraft color codes
 * and converting them to appropriate formats for different contexts.
 * 
 * @author ZeroG
 * @since 1.0.2.95
 */
public class ColorUtils {
    
    /**
     * Processes color codes in the given text.
     * 
     * <p>This method handles the conversion of color codes from various formats
     * to the standard Minecraft format using the § symbol.
     * 
     * @param text The text to process
     * @return The processed text with color codes
     */
    public static String processColorCodes(String text) {
        if (text == null) {
            return "";
        }
        
        // Replace & with § for color codes
        text = text.replace("&", "§");
        
        // Ensure we have valid color codes
        return text;
    }
    
    /**
     * Strips all color codes from the given text.
     * 
     * @param text The text to strip color codes from
     * @return The text without color codes
     */
    public static String stripColorCodes(String text) {
        if (text == null) {
            return "";
        }
        
        // Remove all § color codes
        return text.replaceAll("§[0-9a-fklmnor]", "");
    }
    
    /**
     * Translates alternate color codes to the standard § format.
     * 
     * @param altColorChar The alternate color character (e.g., '&')
     * @param text The text to translate
     * @return The translated text
     */
    public static String translateAlternateColorCodes(char altColorChar, String text) {
        if (text == null) {
            return "";
        }
        
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar && isColorCode(chars[i + 1])) {
                chars[i] = '§';
            }
        }
        
        return new String(chars);
    }
    
    /**
     * Checks if a character is a valid color code.
     * 
     * @param c The character to check
     * @return true if the character is a valid color code
     */
    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || 
               (c >= 'a' && c <= 'f') || 
               (c >= 'A' && c <= 'F') ||
               c == 'k' || c == 'K' ||
               c == 'l' || c == 'L' ||
               c == 'm' || c == 'M' ||
               c == 'n' || c == 'N' ||
               c == 'o' || c == 'O' ||
               c == 'r' || c == 'R';
    }
    
    /**
     * Applies color codes to a MutableComponent by parsing color codes and creating styled components.
     * 
     * @param component The component to apply color codes to
     * @return The component with color codes applied
     */
    public static MutableComponent applyColorCodes(MutableComponent component) {
        if (component == null) {
            return Component.literal("");
        }
        
        // Get the text content from the component
        String text = component.getString();
        if (text == null || text.isEmpty()) {
            return component;
        }
        
        // If the text doesn't contain color codes, return as-is
        if (!text.contains("§") && !text.contains("&")) {
            return component;
        }
        
        // Process & codes to § codes first
        String processedText = processColorCodes(text);
        
        // Parse the text and create styled components
        return parseColoredText(processedText);
    }
    
    /**
     * Parses colored text and creates a properly styled component.
     * 
     * @param text The text with color codes to parse
     * @return A styled component
     */
    private static MutableComponent parseColoredText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.literal("");
        }
        
        MutableComponent result = Component.literal("");
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = Style.EMPTY;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '§' && i + 1 < text.length()) {
                // Add current text with current style
                if (currentText.length() > 0) {
                    result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                    currentText = new StringBuilder();
                }
                
                // Parse the color code
                char code = text.charAt(i + 1);
                currentStyle = getStyleFromCode(code);
                i++; // Skip the next character
            } else {
                currentText.append(c);
            }
        }
        
        // Add any remaining text
        if (currentText.length() > 0) {
            result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
        }
        
        return result;
    }
    
    /**
     * Gets a style from a color code character.
     * 
     * @param code The color code character
     * @return The corresponding style
     */
    private static Style getStyleFromCode(char code) {
        Style style = Style.EMPTY;
        
        switch (code) {
            case '0': return style.withColor(ChatFormatting.BLACK);
            case '1': return style.withColor(ChatFormatting.DARK_BLUE);
            case '2': return style.withColor(ChatFormatting.DARK_GREEN);
            case '3': return style.withColor(ChatFormatting.DARK_AQUA);
            case '4': return style.withColor(ChatFormatting.DARK_RED);
            case '5': return style.withColor(ChatFormatting.DARK_PURPLE);
            case '6': return style.withColor(ChatFormatting.GOLD);
            case '7': return style.withColor(ChatFormatting.GRAY);
            case '8': return style.withColor(ChatFormatting.DARK_GRAY);
            case '9': return style.withColor(ChatFormatting.BLUE);
            case 'a': return style.withColor(ChatFormatting.GREEN);
            case 'b': return style.withColor(ChatFormatting.AQUA);
            case 'c': return style.withColor(ChatFormatting.RED);
            case 'd': return style.withColor(ChatFormatting.LIGHT_PURPLE);
            case 'e': return style.withColor(ChatFormatting.YELLOW);
            case 'f': return style.withColor(ChatFormatting.WHITE);
            case 'k': return style.withObfuscated(true);
            case 'l': return style.withBold(true);
            case 'm': return style.withStrikethrough(true);
            case 'n': return style.withUnderlined(true);
            case 'o': return style.withItalic(true);
            case 'r': return Style.EMPTY; // Reset
            default: return style;
        }
    }
}
