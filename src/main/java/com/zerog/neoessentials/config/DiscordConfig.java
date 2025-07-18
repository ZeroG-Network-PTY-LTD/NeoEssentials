package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Discord configuration for NeoEssentials
 */
public class DiscordConfig {
    public boolean enabled = false;
    public String botToken = "YOUR_BOT_TOKEN_HERE";
    public String guildId = "YOUR_GUILD_ID_HERE";
    
    // Channel mappings
    public Map<String, String> channels = new HashMap<>();
    
    // Chat bridge settings
    public boolean enableChatBridge = true;
    public boolean enableConsoleChannel = true;
    public boolean enableEventAlerts = true;
    public String chatFormat = "**{PLAYER}**: {MESSAGE}";
    public String joinMessage = "➡️ **{PLAYER}** joined the server";
    public String quitMessage = "⬅️ **{PLAYER}** left the server";
    
    // Console settings
    public boolean enableConsoleCommands = true;
    public String consolePrefix = "!";
    
    // Account linking
    public boolean enableAccountLinking = false;
    public boolean requireLinkForPlay = false;
    public boolean enableRoleSync = false;
    public boolean enableNicknameSync = false;
    
    // Voice proximity
    public boolean enableVoiceProximity = false;
    public String voiceCategoryId = "YOUR_VOICE_CATEGORY_ID";
    public int voiceDistance = 50;
    
    public static DiscordConfig createDefault() {
        DiscordConfig config = new DiscordConfig();
        
        // Set default channel mappings
        config.channels.put("global", "GLOBAL_CHAT_CHANNEL_ID");
        config.channels.put("console", "CONSOLE_CHANNEL_ID");
        config.channels.put("alerts", "ALERTS_CHANNEL_ID");
        
        return config;
    }
}
