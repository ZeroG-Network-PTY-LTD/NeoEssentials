package com.zerog.neoessentials.config;

/**
 * Messaging configuration for NeoEssentials
 */
public class MessagingConfig {
    public boolean enabled = true;
    public boolean enablePrivateMessages = true;
    public boolean enableMail = true;
    public boolean enableBroadcast = true;
    public boolean enableAnnouncements = true;
    public boolean logMessages = true;
    public boolean enableMessageSounds = true;
    public int maxMessageLength = 256;
    public int maxMailMessages = 50;
    public int mailExpireDays = 30;
    public boolean enableSpamProtection = true;
    public int spamDelay = 3; // seconds
    
    public static MessagingConfig createDefault() {
        return new MessagingConfig();
    }
}
