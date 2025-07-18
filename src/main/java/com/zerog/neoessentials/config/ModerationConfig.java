package com.zerog.neoessentials.config;

/**
 * Moderation configuration for NeoEssentials
 */
public class ModerationConfig {
    public boolean enabled = true;
    public boolean enableBans = true;
    public boolean enableTempBans = true;
    public boolean enableKicks = true;
    public boolean enableMutes = true;
    public boolean enableJails = true;
    public boolean enableWarnings = true;
    public boolean logActions = true;
    public boolean broadcastActions = true;
    public int maxWarnings = 3;
    public int defaultJailTime = 300; // 5 minutes
    public int defaultMuteTime = 600; // 10 minutes
    
    public static ModerationConfig createDefault() {
        return new ModerationConfig();
    }
}
