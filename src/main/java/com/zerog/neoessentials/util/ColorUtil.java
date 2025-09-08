package com.zerog.neoessentials.util;

import net.minecraft.network.chat.Component;

/**
 * Color Utility - Returns Components for proper text formatting
 */
public class ColorUtil {
    
    /**
     * Process color codes in text and return as Component
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Simple color code removal for now (instead of processing)
        String processed = text.replaceAll("&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
        return Component.literal(processed);
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
