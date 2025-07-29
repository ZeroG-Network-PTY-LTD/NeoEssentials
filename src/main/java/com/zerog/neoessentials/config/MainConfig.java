package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Main configuration for NeoEssentials mod
 * Based on EssentialsX configuration structure
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MainConfig {
    
    // Module enabling/disabling
    public ModulesConfig modules = new ModulesConfig();
    
    // Chat configuration
    public ChatConfig chat = new ChatConfig();
    
    // Color permissions
    public ColorPermissionsConfig colorPermissions = new ColorPermissionsConfig();
    
    // Command cooldowns
    public Map<String, Integer> commandCooldowns = new HashMap<>();
    
    // Protection settings
    public ProtectConfig protect = new ProtectConfig();
    
    // Anti-build settings  
    public AntiBuildConfig antiBuild = new AntiBuildConfig();
    
    // Newbie settings
    public NewbieConfig newbies = new NewbieConfig();
    
    // Additional EssentialsX-compatible configuration modules
    public EconomyConfig economy = new EconomyConfig();
    public HomeConfig homes = new HomeConfig();
    public KitConfig kits = new KitConfig();
    public WarpConfig warps = new WarpConfig();
    public ModerationConfig moderation = new ModerationConfig();
    public MessagingConfig messaging = new MessagingConfig();
    public DiscordConfig discord = new DiscordConfig();
    public TablistConfig tablist = new TablistConfig();
    public SpawnConfig spawn = new SpawnConfig();
    
    public MainConfig() {
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Default command cooldowns
        commandCooldowns.put("home", 60);
        commandCooldowns.put("feed", 30);
        commandCooldowns.put("*warp*", 120);
        commandCooldowns.put("heal", 45);
        commandCooldowns.put("fly", 10);
    }
    
    public static class ModulesConfig {
        public boolean antiBuild = true;
        public boolean chat = true;
        public boolean protect = true;
        public boolean spawn = true;
        public boolean economy = true;
        public boolean discord = false;
        public boolean tablist = true;
        public boolean kits = true;
        public boolean warps = true;
        public boolean homes = true;
        public boolean messaging = true;
        public boolean moderation = true;
    }
    
    public static class ChatConfig {
        public int radius = 0; // 0 = global chat
        public String format = "<{PREFIX}{DISPLAYNAME}{SUFFIX}> {MESSAGE}";
        public boolean localChatOnly = false;
        public boolean enableNearbyChat = false;
        public int nearbyRadius = 100;
    }
    
    public static class ColorPermissionsConfig {
        public boolean nick = true;
        public boolean chat = true;
        public boolean rgb = true;
        public boolean signs = true;
        public boolean books = true;
    }
    
    public static class ProtectConfig {
        public boolean fireSpread = false;
        public boolean creeperExplosion = false;
        public boolean tntExplosion = false;
        public boolean endermenPickup = false;
        public boolean mobGriefing = false;
        public boolean preventLavaFlow = false;
        public boolean preventWaterFlow = false;
    }
    
    public static class AntiBuildConfig {
        public boolean build = true;
        public boolean use = true;
        public boolean warnOnBuildDisallow = true;
        public boolean alertOnViolation = true;
        public String alertMessage = "&cPlayer {PLAYER} tried to build without permission!";
    }
    
    public static class NewbieConfig {
        public String announceFormat = "&dWelcome {DISPLAYNAME}&d to the server!";
        public String kit = "tools";
        public String respawnListenerPriority = "high";
        public boolean giveKitOnJoin = true;
        public boolean announceOnJoin = true;
        public int protectionTime = 300; // 5 minutes of protection
    }
}
