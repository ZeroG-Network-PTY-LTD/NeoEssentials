package com.zerog.neoessentials.config;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Warp system configuration for NeoEssentials
 * Compatible with EssentialsX warp system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class WarpConfig {
    
    // Basic warp settings
    public boolean enabled = true;
    public int maxWarps = 50; // Maximum warps on server
    public int maxWarpsPerPlayer = 5; // Max warps a player can create
    
    // Warp costs
    public BigDecimal createWarpCost = new BigDecimal("500.00");
    public BigDecimal teleportWarpCost = new BigDecimal("25.00");
    public BigDecimal deleteWarpCost = BigDecimal.ZERO;
    
    // Cooldowns (in seconds)
    public int createWarpCooldown = 600; // 10 minutes
    public int teleportWarpCooldown = 120; // 2 minutes
    public int deleteWarpCooldown = 60; // 1 minute
    
    // Teleport settings
    public int teleportWarmup = 5; // 5 seconds warmup
    public boolean cancelOnMove = true;
    public boolean cancelOnDamage = true;
    public double maxMoveDistance = 0.5; // blocks
    
    // Safety settings
    public boolean requireSafeLocation = true;
    public boolean checkForLava = true;
    public boolean checkForVoid = true;
    public boolean checkForSuffocation = true;
    public int safeLocationRadius = 3;
    
    // Access control
    public boolean enablePublicWarps = true;
    public boolean enablePrivateWarps = true;
    public boolean allowCrossDimensionTeleport = true;
    
    // Warp categories/groups
    public boolean enableWarpCategories = true;
    public List<String> defaultCategories = Arrays.asList("spawn", "shops", "arenas", "farms", "builds");
    
    // Restricted worlds
    public List<String> restrictedWorlds = Arrays.asList("world_nether", "world_the_end");
    public List<String> noTeleportWorlds = Arrays.asList(); // Worlds where you can't teleport TO
    
    // Warp naming
    public int maxWarpNameLength = 20;
    public boolean allowSpacesInNames = false;
    public boolean allowSpecialCharacters = false;
    public List<String> bannedWarpNames = Arrays.asList("spawn", "home", "admin", "server", "console");
    
    // Permission-based warps
    public boolean enablePermissionWarps = true;
    public String permissionPrefix = "essentials.warp.";
    
    // Warp signs
    public boolean enableWarpSigns = true;
    public String warpSignFormat = "[Warp]";
    public BigDecimal warpSignCost = new BigDecimal("100.00");
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class MessagesConfig {
        public String warpCreated = "&aWarp '{WARP}' created successfully!";
        public String warpDeleted = "&cWarp '{WARP}' deleted!";
        public String warpNotFound = "&cWarp '{WARP}' not found!";
        public String warpTeleporting = "&aTeleporting to warp '{WARP}'...";
        public String warpTeleportCancelled = "&cTeleport cancelled due to movement!";
        public String maxWarpsReached = "&cMaximum number of warps reached ({MAX})!";
        public String warpAlreadyExists = "&cWarp '{WARP}' already exists!";
        public String invalidWarpName = "&cInvalid warp name! Use only letters and numbers.";
        public String warpListHeader = "&6Available warps:";
        public String warpListEntry = "&7- &a{WARP} &7({OWNER}) [{CATEGORY}] &8({WORLD} {X}, {Y}, {Z})";
        public String warpListEmpty = "&cNo warps available!";
        public String unsafeLocation = "&cUnsafe location! Teleport cancelled.";
        public String restrictedWorld = "&cYou cannot create warps in this world!";
        public String noTeleportWorld = "&cYou cannot teleport to this world!";
        public String insufficientFunds = "&cYou need {COST} to create/use this warp!";
        public String cooldownActive = "&cYou must wait {TIME} before using this command again!";
        public String warpNoPermission = "&cYou don't have permission to access this warp!";
        public String warpPrivate = "&cThis is a private warp!";
        public String warpSignCreated = "&aWarp sign created for '{WARP}'!";
        public String warpSignUsed = "&aTeleporting via warp sign...";
        
        // Category messages
        public String categoryNotFound = "&cCategory '{CATEGORY}' not found!";
        public String categoryListHeader = "&6Warp categories:";
        public String categoryListEntry = "&7- &a{CATEGORY} &7({COUNT} warps)";
        public String categoryWarpsHeader = "&6Warps in category '{CATEGORY}':";
    }
    
    /**
     * Check if world allows warp creation
     */
    public boolean isWarpCreationAllowed(String worldName) {
        return !restrictedWorlds.contains(worldName);
    }
    
    /**
     * Check if world allows teleporting to
     */
    public boolean isTeleportAllowed(String worldName) {
        return !noTeleportWorlds.contains(worldName);
    }
    
    /**
     * Check if warp name is valid
     */
    public boolean isValidWarpName(String name) {
        if (name.length() > maxWarpNameLength) {
            return false;
        }
        
        if (bannedWarpNames.contains(name.toLowerCase())) {
            return false;
        }
        
        if (!allowSpacesInNames && name.contains(" ")) {
            return false;
        }
        
        if (!allowSpecialCharacters && !name.matches("[a-zA-Z0-9_-]+")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get permission node for warp access
     */
    public String getWarpPermission(String warpName) {
        return permissionPrefix + warpName.toLowerCase();
    }
    
    /**
     * Check if category is valid
     */
    public boolean isValidCategory(String category) {
        return defaultCategories.contains(category.toLowerCase());
    }
}
