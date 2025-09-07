package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Commands Configuration for NeoEssentials
 * Represents the commands.json file structure
 */
public class CommandsConfig {
    
    public Settings settings = new Settings();
    public Map<String, CommandConfig> commands = new HashMap<>();
    
    public CommandsConfig() {
        // Initialize with some default commands
        commands.put("feed", new CommandConfig(true, 25.0, 30, 0, "neoessentials.feed", true));
        commands.put("fly", new CommandConfig(true, 0.0, 5, 0, "neoessentials.fly", true));
        commands.put("heal", new CommandConfig(true, 50.0, 30, 0, "neoessentials.heal", true));
        commands.put("home", new CommandConfig(true, 5.0, 10, 2, "neoessentials.home", false));
        commands.put("spawn", new CommandConfig(true, 0.0, 5, 3, "neoessentials.spawn", false));
    }
    
    public static class Settings {
        public boolean enableCosts = true;
        public boolean enableCooldowns = true;
        public boolean enableWarmups = true;
        public boolean enableDiscordLogging = true;
        public int defaultCooldown = 3;
        public int defaultWarmup = 0;
    }
    
    public static class CommandConfig {
        public boolean enabled = true;
        public double cost = 0.0;
        public int cooldown = 0;
        public int warmup = 0;
        public String permission = "";
        public boolean logToDiscord = false;
        
        // Default constructor for JSON deserialization
        public CommandConfig() {}
        
        public CommandConfig(boolean enabled, double cost, int cooldown, int warmup, String permission, boolean logToDiscord) {
            this.enabled = enabled;
            this.cost = cost;
            this.cooldown = cooldown;
            this.warmup = warmup;
            this.permission = permission;
            this.logToDiscord = logToDiscord;
        }
    }
}
