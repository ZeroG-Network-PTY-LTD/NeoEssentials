package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Main configuration for NeoEssentials
 * 
 * Controls which features are enabled and general mod settings
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MainConfig {
    
    // Feature toggles
    public boolean enableEconomy = true;
    public boolean enableHomes = true;
    public boolean enableWarps = true;
    public boolean enableKits = true;
    public boolean enableModeration = true;
    public boolean enableMessaging = true;
    public boolean enableTeleport = true;
    public boolean enableTablist = true;
    public boolean enableDiscord = false;
    
    // General settings
    public String serverName = "NeoEssentials Server";
    public String currencySymbol = "$";
    public String currencyName = "Coins";
    public boolean debugMode = false;
    public int maxHomesDefault = 3;
    public int maxHomesVIP = 5;
    public int maxHomesAdmin = 10;
    
    // Command cooldowns (in seconds)
    public Map<String, Integer> commandCooldowns = new HashMap<>();
    
    // Placeholder settings
    public boolean enablePlaceholders = true;
    public int placeholderUpdateInterval = 5; // seconds
    
    // Chat settings
    public boolean enableChatFormatting = true;
    public String chatFormat = "&7[{GROUP}] &f{PLAYER}: {MESSAGE}";
    
    // Economy settings
    public double startingBalance = 1000.0;
    public boolean enableSignShops = true;
    public boolean enableVaultIntegration = true;
    
    public static MainConfig createDefault() {
        MainConfig config = new MainConfig();
        
        // Set default command cooldowns
        config.commandCooldowns.put("home", 5);
        config.commandCooldowns.put("warp", 3);
        config.commandCooldowns.put("tpa", 10);
        config.commandCooldowns.put("back", 5);
        config.commandCooldowns.put("spawn", 3);
        config.commandCooldowns.put("kit", 300); // 5 minutes
        config.commandCooldowns.put("feed", 60); // 1 minute
        config.commandCooldowns.put("heal", 60); // 1 minute
        
        return config;
    }
}
