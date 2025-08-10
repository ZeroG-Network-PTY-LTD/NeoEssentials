package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.List;

/**
 * Chat formatting configuration for NeoEssentials
 * Handles chat prefix/suffix display and formatting options
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ChatConfig {
    
    // Main chat formatting settings
    public boolean enabled = true;
    public String format = "{MESSAGE}";
    public boolean enableColors = true;
    public boolean enableHexColors = true;
    public boolean enableFormattingCodes = true;
    
    // Prefix/suffix settings
    public PrefixSuffixConfig prefixSuffix = new PrefixSuffixConfig();
    
    // Nickname settings
    public NicknameConfig nicknames = new NicknameConfig();
    
    // Message filtering
    public FilterConfig filter = new FilterConfig();
    
    // Placeholder settings
    public PlaceholderConfig placeholders = new PlaceholderConfig();
    
    // Anti-spam settings
    public AntiSpamConfig antiSpam = new AntiSpamConfig();
    
    public static class PrefixSuffixConfig {
        public boolean enabled = true;
        public boolean usePermissionSystem = true;
        public boolean useGroupSystem = true;
        public String defaultPrefix = "";
        public String defaultSuffix = "";
        
        // Formatting templates
        public String prefixFormat = "{PREFIX}";
        public String suffixFormat = "{SUFFIX}";
        
        // Color settings
        public boolean inheritGroupColors = true;
        public boolean allowCustomColors = true;
    }
    
    public static class NicknameConfig {
        public boolean enabled = true;
        public boolean showInChat = true;
        public boolean showOriginalOnHover = true;
        public String nicknameFormat = "{NICKNAME}";
        public String hoverText = "Real name: {PLAYER}";
        public int maxLength = 16;
        public boolean allowColors = true;
        public boolean allowFormatting = true;
    }
    
    public static class FilterConfig {
        public boolean enabled = false;
        public List<String> blockedWords = Arrays.asList("spam", "bad");
        public String censorReplacement = "***";
        public boolean censorMode = false; // false = block, true = censor
        public boolean caseSensitive = false;
    }
    
    public static class PlaceholderConfig {
        public boolean enabled = true;
        public boolean enablePlayerPlaceholders = true;
        public boolean enableServerPlaceholders = true;
        public boolean enableTimePlaceholders = true;
        public boolean enableCustomPlaceholders = true;
    }
    
    public static class AntiSpamConfig {
        public boolean enabled = false;
        public int maxMessagesPerSecond = 3;
        public int maxDuplicateMessages = 2;
        public int duplicateTimeWindow = 30; // seconds
        public boolean blockSimilarMessages = true;
        public int similarityThreshold = 80; // percentage
    }
    
    /**
     * Get the full chat format with all placeholders
     */
    public String getFullFormat() {
        StringBuilder formatBuilder = new StringBuilder();
        
        if (prefixSuffix.enabled) {
            formatBuilder.append(prefixSuffix.prefixFormat);
        }
        
        if (nicknames.enabled && nicknames.showInChat) {
            formatBuilder.append(nicknames.nicknameFormat);
        } else {
            formatBuilder.append("{PLAYER}");
        }
        
        if (prefixSuffix.enabled) {
            formatBuilder.append(prefixSuffix.suffixFormat);
        }
        
        formatBuilder.append(": {MESSAGE}");
        
        return formatBuilder.toString();
    }
    
    /**
     * Check if chat formatting is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if colors are enabled
     */
    public boolean areColorsEnabled() {
        return enabled && enableColors;
    }
    
    /**
     * Check if hex colors are enabled
     */
    public boolean areHexColorsEnabled() {
        return enabled && enableColors && enableHexColors;
    }
    
    /**
     * Check if formatting codes are enabled
     */
    public boolean areFormattingCodesEnabled() {
        return enabled && enableFormattingCodes;
    }
}
