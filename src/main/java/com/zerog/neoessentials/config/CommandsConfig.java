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
        // Initialize with comprehensive default commands - ALL ENABLED by default
        commands.put("feed", new CommandConfig(true, 25.0, 30, 0, "neoessentials.feed", true));
        commands.put("fly", new CommandConfig(true, 0.0, 5, 0, "neoessentials.fly", true));
        commands.put("heal", new CommandConfig(true, 50.0, 30, 0, "neoessentials.heal", true));
        commands.put("home", new CommandConfig(true, 5.0, 10, 2, "neoessentials.home", false));
        commands.put("sethome", new CommandConfig(true, 10.0, 5, 0, "neoessentials.home.set", false));
        commands.put("delhome", new CommandConfig(true, 0.0, 0, 0, "neoessentials.home.delete", false));
        commands.put("homes", new CommandConfig(true, 0.0, 0, 0, "neoessentials.home.list", false));
        commands.put("spawn", new CommandConfig(true, 0.0, 5, 3, "neoessentials.spawn", false));
        commands.put("balance", new CommandConfig(true, 0.0, 0, 0, "neoessentials.economy.balance", false));
        commands.put("pay", new CommandConfig(true, 0.0, 1, 0, "neoessentials.economy.pay", true));
        commands.put("baltop", new CommandConfig(true, 0.0, 0, 0, "neoessentials.economy.top", false));
        commands.put("warp", new CommandConfig(true, 25.0, 3, 0, "neoessentials.warp", false));
        commands.put("kit", new CommandConfig(true, 0.0, 0, 0, "neoessentials.kit", false));
        commands.put("ban", new CommandConfig(true, 0.0, 0, 0, "neoessentials.ban", true));
        commands.put("kick", new CommandConfig(true, 0.0, 0, 0, "neoessentials.kick", true));
        commands.put("mute", new CommandConfig(true, 0.0, 0, 0, "neoessentials.mute", true));
        commands.put("message", new CommandConfig(true, 0.0, 0, 0, "neoessentials.message", false));
        commands.put("reply", new CommandConfig(true, 0.0, 0, 0, "neoessentials.reply", false));
        commands.put("nick", new CommandConfig(true, 10.0, 300, 0, "neoessentials.nick", true));
        commands.put("god", new CommandConfig(true, 0.0, 0, 0, "neoessentials.god", true));
        commands.put("vanish", new CommandConfig(true, 0.0, 0, 0, "neoessentials.vanish", true));
        commands.put("speed", new CommandConfig(true, 0.0, 10, 0, "neoessentials.speed", false));
        commands.put("gamemode", new CommandConfig(true, 0.0, 0, 0, "neoessentials.gamemode", true));
        commands.put("repair", new CommandConfig(true, 500.0, 60, 0, "neoessentials.repair", true));
        commands.put("time", new CommandConfig(true, 0.0, 0, 0, "neoessentials.time", true));
        commands.put("weather", new CommandConfig(true, 0.0, 0, 0, "neoessentials.weather", true));
        commands.put("give", new CommandConfig(true, 0.0, 0, 0, "neoessentials.give", true));
        commands.put("workbench", new CommandConfig(true, 5.0, 0, 0, "neoessentials.workbench", false));
        commands.put("anvil", new CommandConfig(true, 10.0, 0, 0, "neoessentials.anvil", false));
        commands.put("smithing", new CommandConfig(true, 15.0, 0, 0, "neoessentials.smithing", false));
        commands.put("stonecutter", new CommandConfig(true, 5.0, 0, 0, "neoessentials.stonecutter", false));
        commands.put("list", new CommandConfig(true, 0.0, 0, 0, "neoessentials.list", false));
        commands.put("whois", new CommandConfig(true, 0.0, 0, 0, "neoessentials.whois", false));
        commands.put("seen", new CommandConfig(true, 0.0, 0, 0, "neoessentials.seen", false));
        commands.put("help", new CommandConfig(true, 0.0, 0, 0, "neoessentials.help", false));
        commands.put("info", new CommandConfig(true, 0.0, 0, 0, "neoessentials.info", false));
        commands.put("motd", new CommandConfig(true, 0.0, 0, 0, "neoessentials.motd", false));
        commands.put("afk", new CommandConfig(true, 0.0, 0, 0, "neoessentials.afk", false));
        commands.put("mail", new CommandConfig(true, 0.0, 0, 0, "neoessentials.mail", false));
        commands.put("teleport", new CommandConfig(true, 0.0, 0, 0, "neoessentials.teleport", true));
        commands.put("tpa", new CommandConfig(true, 0.0, 5, 0, "neoessentials.tpa", false));
        commands.put("rules", new CommandConfig(true, 0.0, 0, 0, "neoessentials.rules", false));
        commands.put("back", new CommandConfig(true, 50.0, 10, 0, "neoessentials.back", false));
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
