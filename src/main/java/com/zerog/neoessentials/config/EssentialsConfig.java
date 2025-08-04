package com.zerog.neoessentials.config;

/**
 * Main configuration class for NeoEssentials
 * Equivalent to EssentialsX main config
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EssentialsConfig {
    
    // Module Configuration
    public ModuleConfig modules = new ModuleConfig();
    
    // Chat Configuration
    public ChatConfig chat = new ChatConfig();
    
    // Color Permissions
    public ColorPermissionsConfig colorPermissions = new ColorPermissionsConfig();
    
    // Command Cooldowns
    public CooldownConfig commandCooldowns = new CooldownConfig();
    
    // Protection Settings
    public ProtectConfig protect = new ProtectConfig();
    
    // Anti-Build Settings
    public AntiBuildConfig antiBuild = new AntiBuildConfig();
    
    // Newbie Settings
    public NewbieConfig newbies = new NewbieConfig();
    
    // Economy Settings
    public EconomyConfig economy = new EconomyConfig();
    
    // Teleportation Settings
    public TeleportConfig teleport = new TeleportConfig();
    
    // Language Settings
    public LanguageConfig language = new LanguageConfig();
    
    // Notification Settings
    public NotificationConfig notifications = new NotificationConfig();
    
    public static class ModuleConfig {
        public boolean antiBuild = true;
        public boolean chat = true;
        public boolean protect = true;
        public boolean spawn = true;
        public boolean economy = true;
        public boolean discord = false;
        public boolean geoip = false;
    }
    
    public static class ChatConfig {
        public int radius = 0; // 0 = global chat
        public String format = "<{PREFIX}{DISPLAYNAME}{SUFFIX}> {MESSAGE}";
        public boolean enableColors = true;
        public boolean enableFormatting = true;
    }
    
    public static class ColorPermissionsConfig {
        public boolean nick = true;
        public boolean chat = true;
        public boolean rgb = true;
        public boolean legacy = true;
    }
    
    public static class CooldownConfig {
        public int home = 60;
        public int feed = 30;
        public int warp = 120;
        public int tpa = 30;
        public int spawn = 10;
        public boolean persistAcrossRestart = true;
    }
    
    public static class ProtectConfig {
        public boolean fireSpread = false;
        public boolean creeperExplosion = false;
        public boolean tntExplosion = false;
        public boolean endermenBlockPickup = false;
        public boolean weatherDamage = false;
    }
    
    public static class AntiBuildConfig {
        public boolean build = true;
        public boolean use = true;
        public boolean warnOnBuildDisallow = true;
        public boolean alertAdmins = true;
    }
    
    public static class NewbieConfig {
        public String announceFormat = "&dWelcome {DISPLAYNAME}&d to the server!";
        public String kit = "starter";
        public String respawnListenerPriority = "high";
        public boolean giveKitOnFirstJoin = true;
    }
    
    public static class EconomyConfig {
        public boolean enabled = true;
        public String currencySymbol = "$";
        public double startingBalance = 1000.0;
        public double maxBalance = 1000000.0;
        public boolean enableSigns = true;
        public boolean enableCommandCosts = true;
    }
    
    public static class TeleportConfig {
        public int maxHomes = 3;
        public boolean allowCrossDimension = true;
        public int teleportDelay = 3; // seconds
        public boolean cancelOnMove = true;
        public boolean cancelOnDamage = true;
    }
    
    public static class LanguageConfig {
        public String defaultLocale = "en_us";
        public boolean autoDetectPlayerLocale = true;
        public String[] supportedLocales = {"en_us", "de_de", "es_es", "fr_fr", "it_it", "pt_br", "ru_ru", "zh_cn"};
    }
    
    public static class NotificationConfig {
        public boolean enabled = true;
        public boolean logCommands = false; // Log command executions
        public DiscordConfig discord = new DiscordConfig();
        public EmailConfig email = new EmailConfig();
        
        public static class DiscordConfig {
            public boolean enabled = false;
            public String webhookUrl = "";
            public String username = "NeoEssentials";
            public String avatarUrl = "";
        }
        
        public static class EmailConfig {
            public boolean enabled = false;
            public String smtpHost = "smtp.gmail.com";
            public int smtpPort = 587;
            public String username = "";
            public String password = "";
            public String fromAddress = "";
            public String[] toAddresses = {};
            public boolean useTLS = true;
        }
    }
}
