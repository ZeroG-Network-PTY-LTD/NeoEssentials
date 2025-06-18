package com.zerog.neoessentials.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for text-related operations that are version-independent.
 * Implementation is free from Minecraft-specific classes to ensure compatibility.
 */
public class CommonTextUtil {
    
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates color codes in a string (e.g., &4 for red)
     * Only translating the syntax, not the actual Minecraft color rendering
     * 
     * @param text The text to translate
     * @return The text with color codes translated to Minecraft format
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
     * Formats text, translating color codes
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
     * Formats text, translating color codes if allowed
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
     * Strip color codes from text
     * 
     * @param text Text possibly containing color codes
     * @return Text with color codes removed
     */
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        
        // Remove Minecraft's internal color codes
        text = text.replaceAll("§[0-9a-fk-or]", "");
        
        // Remove hex color codes in the form §x§r§r§g§g§b§b
        text = text.replaceAll("§x(§[0-9A-Fa-f]){6}", "");
        
        // Remove our color codes
        text = text.replaceAll("&[0-9a-fk-or]", "");
        
        // Remove our hex color codes
        text = text.replaceAll("&#[0-9A-Fa-f]{6}", "");
        
        return text;
    }
}
