package com.zerog.neoessentials.config;

import java.util.Map;
import java.util.HashMap;

/**
 * Commands Configuration for NeoEssentials
 * Represents the commands.json file structure
 */
public class CommandsConfig {
    
    public Settings settings = new Settings();
    public Map<String, Command> commands = new HashMap<>();
    
    public CommandsConfig() {
        // Initialize with default commands
        commands.put("heal", new Command(true, 50.0, 30, 0, "neoessentials.heal", true));
        commands.put("feed", new Command(true, 25.0, 30, 0, "neoessentials.feed", true));
        commands.put("fly", new Command(true, 0.0, 5, 0, "neoessentials.fly", true));
    }
    
    public static class Settings {
        public boolean enableCosts = true;
        public boolean enableCooldowns = true;
        public boolean enableWarmups = true;
        public boolean enableDiscordLogging = true;
        public int defaultCooldown = 3;
        public int defaultWarmup = 0;
    }
    
    public static class Command {
        public boolean enabled;
        public double cost;
        public int cooldown;
        public int warmup;
        public String permission;
        public boolean logToDiscord;
        
        public Command() {}
        
        public Command(boolean enabled, double cost, int cooldown, int warmup, String permission, boolean logToDiscord) {
            this.enabled = enabled;
            this.cost = cost;
            this.cooldown = cooldown;
            this.warmup = warmup;
            this.permission = permission;
            this.logToDiscord = logToDiscord;
        }
    }
}
