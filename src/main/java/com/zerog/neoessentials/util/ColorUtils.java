package com.zerog.neoessentials.util;

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
}
