package com.zerog.neoessentials.config;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Home system configuration for NeoEssentials
 * Compatible with EssentialsX home system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HomeConfig {
    
    // Basic home settings
    public boolean enabled = true;
    public int maxHomes = 3;
    public int maxHomesVip = 5;
    public int maxHomesAdmin = 10;
    
    // Home costs
    public BigDecimal setHomeCost = new BigDecimal("50.00");
    public BigDecimal teleportHomeCost = new BigDecimal("10.00");
    public BigDecimal deleteHomeCost = BigDecimal.ZERO;
    
    // Cooldowns (in seconds)
    public int setHomeCooldown = 300; // 5 minutes
    public int teleportHomeCooldown = 60; // 1 minute
    public int deleteHomeCooldown = 30; // 30 seconds
    
    // Teleport settings
    public int teleportWarmup = 3; // 3 seconds warmup
    public boolean cancelOnMove = true;
    public boolean cancelOnDamage = true;
    public double maxMoveDistance = 0.5; // blocks
    
    // Safety settings
    public boolean requireSafeLocation = true;
    public boolean checkForLava = true;
    public boolean checkForVoid = true;
    public boolean checkForSuffocation = true;
    public int safeLocationRadius = 3;
    
    // Bed home settings
    public boolean updateHomeToBed = false; // Whether sleeping updates home location
    public boolean enableBedHome = true; // Whether /home works with bed
    public String bedHomeName = "bed";
    
    // Restricted worlds
    public List<String> restrictedWorlds = Arrays.asList("world_nether", "world_the_end");
    public boolean allowCrossDimensionTeleport = true;
    
    // Home naming
    public int maxHomeNameLength = 16;
    public boolean allowSpacesInNames = false;
    public boolean allowSpecialCharacters = false;
    public List<String> bannedHomeNames = Arrays.asList("spawn", "warp", "admin", "server");
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class MessagesConfig {
        public String homeSet = "&aHome '{HOME}' set successfully!";
        public String homeDeleted = "&cHome '{HOME}' deleted!";
        public String homeNotFound = "&cHome '{HOME}' not found!";
        public String homeTeleporting = "&aTeleporting to home '{HOME}'...";
        public String homeTeleportCancelled = "&cTeleport cancelled due to movement!";
        public String maxHomesReached = "&cYou have reached the maximum number of homes ({MAX})!";
        public String homeAlreadyExists = "&cHome '{HOME}' already exists!";
        public String invalidHomeName = "&cInvalid home name! Use only letters and numbers.";
        public String homeListHeader = "&6Your homes:";
        public String homeListEntry = "&7- &a{HOME} &7({WORLD} {X}, {Y}, {Z})";
        public String homeListEmpty = "&cYou don't have any homes set!";
        public String unsafeLocation = "&cUnsafe location! Teleport cancelled.";
        public String restrictedWorld = "&cYou cannot set homes in this world!";
        public String insufficientFunds = "&cYou need {COST} to set a home!";
        public String cooldownActive = "&cYou must wait {TIME} before using this command again!";
    }
    
    /**
     * Get max homes for a player based on permissions
     */
    public int getMaxHomes(String permissionLevel) {
        switch (permissionLevel.toLowerCase()) {
            case "admin":
                return maxHomesAdmin;
            case "vip":
                return maxHomesVip;
            default:
                return maxHomes;
        }
    }
    
    /**
     * Check if world allows homes
     */
    public boolean isWorldAllowed(String worldName) {
        return !restrictedWorlds.contains(worldName);
    }
    
    /**
     * Check if home name is valid
     */
    public boolean isValidHomeName(String name) {
        if (name.length() > maxHomeNameLength) {
            return false;
        }
        
        if (bannedHomeNames.contains(name.toLowerCase())) {
            return false;
        }
        
        if (!allowSpacesInNames && name.contains(" ")) {
            return false;
        }
        
        if (!allowSpecialCharacters && !name.matches("[a-zA-Z0-9_]+")) {
            return false;
        }
        
        return true;
    }
}
