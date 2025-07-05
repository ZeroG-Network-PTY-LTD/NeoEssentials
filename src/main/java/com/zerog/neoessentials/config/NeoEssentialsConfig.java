package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for NeoEssentials.
 * Contains all configurable settings for the mod.
 */
public class NeoEssentialsConfig {
    // General settings
    private boolean debug = false;
    private String defaultLanguage = "en_us";
    
    // Teleportation settings
    private boolean teleportEnabled = true;
    private int teleportCooldown = 30;  // seconds
    private int teleportWarmup = 3;     // seconds
    private int maxHomes = 3;
    
    // Warp settings
    private boolean warpsEnabled = true;
    private Map<String, Integer> warpCosts = new HashMap<>();
    
    // Chat settings
    private boolean chatFormattingEnabled = true;
    private String chatFormat = "{DISPLAYNAME} &7: &f{MESSAGE}";
    
    // Command settings
    private Map<String, Boolean> commandsEnabled = new HashMap<>();
    
    // Permission settings
    private Map<String, Boolean> defaultPermissions = new HashMap<>();
    
    /**
     * Default constructor with default settings
     */
    public NeoEssentialsConfig() {
        // Initialize default command settings
        commandsEnabled.put("home", true);
        commandsEnabled.put("warp", true);
        commandsEnabled.put("tpa", true);
        commandsEnabled.put("back", true);
        commandsEnabled.put("spawn", true);
        commandsEnabled.put("heal", true);
        commandsEnabled.put("feed", true);
        commandsEnabled.put("fly", true);
        commandsEnabled.put("gamemode", true);
        commandsEnabled.put("money", true);
        commandsEnabled.put("pay", true);
        commandsEnabled.put("balance", true);
        commandsEnabled.put("time", true);
        commandsEnabled.put("weather", true);
        
        // Initialize default warp costs
        warpCosts.put("spawn", 0);
        warpCosts.put("mine", 10);
<<<<<<< HEAD
<<<<<<< HEAD
          // Initialize default permissions
=======
        
        // Initialize default permissions
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
          // Initialize default permissions
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
        defaultPermissions.put("neoessentials.command.tpa", true);
        defaultPermissions.put("neoessentials.command.tpahere", true);
        defaultPermissions.put("neoessentials.command.tpaccept", true);
        defaultPermissions.put("neoessentials.command.tpdeny", true);
        defaultPermissions.put("neoessentials.command.back", true);
        defaultPermissions.put("neoessentials.command.spawn", true);
        defaultPermissions.put("neoessentials.command.home", true);
        defaultPermissions.put("neoessentials.command.sethome", true);
        defaultPermissions.put("neoessentials.command.delhome", true);
        defaultPermissions.put("neoessentials.command.warp", true);
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
        defaultPermissions.put("neoessentials.command.warp.list", true);
        defaultPermissions.put("neoessentials.command.warp.set", false);
        defaultPermissions.put("neoessentials.command.warp.delete", false);
        defaultPermissions.put("neoessentials.command.warp.player", false);
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
        defaultPermissions.put("neoessentials.command.heal", false);
        defaultPermissions.put("neoessentials.command.heal.others", false);
        defaultPermissions.put("neoessentials.command.feed", true);
        defaultPermissions.put("neoessentials.command.feed.others", false);
        defaultPermissions.put("neoessentials.command.fly", false);
        defaultPermissions.put("neoessentials.command.fly.others", false);
        defaultPermissions.put("neoessentials.command.gamemode.creative", false);
        defaultPermissions.put("neoessentials.command.gamemode.creative.others", false);
        defaultPermissions.put("neoessentials.command.gamemode.survival", false);
        defaultPermissions.put("neoessentials.command.gamemode.survival.others", false);
        defaultPermissions.put("neoessentials.command.gamemode.spectator", false);
        defaultPermissions.put("neoessentials.command.gamemode.spectator.others", false);
        defaultPermissions.put("neoessentials.command.gamemode.adventure", false);
<<<<<<< HEAD
<<<<<<< HEAD
        defaultPermissions.put("neoessentials.command.gamemode.adventure.others", false);        defaultPermissions.put("neoessentials.command.time", true);
        defaultPermissions.put("neoessentials.command.weather", true);
=======
        defaultPermissions.put("neoessentials.command.gamemode.adventure.others", false);
        defaultPermissions.put("neoessentials.command.time", false);
        defaultPermissions.put("neoessentials.command.weather", false);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        defaultPermissions.put("neoessentials.command.gamemode.adventure.others", false);        defaultPermissions.put("neoessentials.command.time", true);
        defaultPermissions.put("neoessentials.command.weather", true);
>>>>>>> 99d6b05 (chore: Update build number to 15 and timestamp in buildnumber.properties; modify default permissions for time and weather commands)
        defaultPermissions.put("neoessentials.command.money", true);
        defaultPermissions.put("neoessentials.command.pay", true);
        defaultPermissions.put("neoessentials.command.balance", true);
        defaultPermissions.put("neoessentials.command.baltop", true);
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 99d6b05 (chore: Update build number to 15 and timestamp in buildnumber.properties; modify default permissions for time and weather commands)
        defaultPermissions.put("neoessentials.command.eco", false);        defaultPermissions.put("neoessentials.command.kit", true);
        defaultPermissions.put("neoessentials.command.kit.list", true);
        defaultPermissions.put("neoessentials.command.kit.create", false);
        defaultPermissions.put("neoessentials.command.kit.delete", false);
        defaultPermissions.put("neoessentials.command.kit.give", false);
<<<<<<< HEAD
=======
        defaultPermissions.put("neoessentials.command.eco", false);
        defaultPermissions.put("neoessentials.command.kit", true);
        defaultPermissions.put("neoessentials.command.createkit", false);
        defaultPermissions.put("neoessentials.command.deletekit", false);
        defaultPermissions.put("neoessentials.command.givekit", false);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 99d6b05 (chore: Update build number to 15 and timestamp in buildnumber.properties; modify default permissions for time and weather commands)
    }

    // Getters and setters for all properties
    
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public boolean isTeleportEnabled() {
        return teleportEnabled;
    }

    public void setTeleportEnabled(boolean teleportEnabled) {
        this.teleportEnabled = teleportEnabled;
    }

    public int getTeleportCooldown() {
        return teleportCooldown;
    }

    public void setTeleportCooldown(int teleportCooldown) {
        this.teleportCooldown = teleportCooldown;
    }

    public int getTeleportWarmup() {
        return teleportWarmup;
    }

    public void setTeleportWarmup(int teleportWarmup) {
        this.teleportWarmup = teleportWarmup;
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public void setMaxHomes(int maxHomes) {
        this.maxHomes = maxHomes;
    }

    public boolean isWarpsEnabled() {
        return warpsEnabled;
    }

    public void setWarpsEnabled(boolean warpsEnabled) {
        this.warpsEnabled = warpsEnabled;
    }

    public Map<String, Integer> getWarpCosts() {
        return warpCosts;
    }

    public void setWarpCosts(Map<String, Integer> warpCosts) {
        this.warpCosts = warpCosts;
    }

    public boolean isChatFormattingEnabled() {
        return chatFormattingEnabled;
    }

    public void setChatFormattingEnabled(boolean chatFormattingEnabled) {
        this.chatFormattingEnabled = chatFormattingEnabled;
    }

    public String getChatFormat() {
        return chatFormat;
    }

    public void setChatFormat(String chatFormat) {
        this.chatFormat = chatFormat;
    }

    public Map<String, Boolean> getCommandsEnabled() {
        return commandsEnabled;
    }

    public void setCommandsEnabled(Map<String, Boolean> commandsEnabled) {
        this.commandsEnabled = commandsEnabled;
    }

    public Map<String, Boolean> defaultPermissions() {
        return defaultPermissions;
    }

    public void setDefaultPermissions(Map<String, Boolean> defaultPermissions) {
        this.defaultPermissions = defaultPermissions;
    }

    public boolean getDefaultPermission(String permission) {
        return defaultPermissions.getOrDefault(permission, true);
    }

    public void setDefaultPermission(String permission, boolean value) {
        defaultPermissions.put(permission, value);
    }
    
    /**
     * Check if a specific command is enabled
     * 
     * @param commandName The name of the command to check
     * @return True if the command is enabled, false otherwise
     */
    public boolean isCommandEnabled(String commandName) {
        return commandsEnabled.getOrDefault(commandName.toLowerCase(), false);
    }
}
