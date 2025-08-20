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
    
    // Top-level module enable/disable
    public boolean enabled = true;

    // Basic warp settings
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
    public String permissionPrefix = "neoessentials.warp.";
    
    // Warp signs
    public boolean enableWarpSigns = true;
    public String warpSignFormat = "[Warp]";
    public BigDecimal warpSignCost = new BigDecimal("100.00");
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class MessagesConfig {
        public String warpCreated = "&aWarp '{0}' created successfully!";
        public String warpDeleted = "&cWarp '{0}' deleted!";
        public String warpNotFound = "&cWarp '{0}' not found!";
        public String warpTeleporting = "&aTeleporting to warp '{0}'...";
        public String warpTeleportCancelled = "&cTeleport cancelled due to movement!";
        public String maxWarpsReached = "&cMaximum number of warps reached ({0})!";
        public String warpAlreadyExists = "&cWarp '{0}' already exists!";
        public String invalidWarpName = "&cInvalid warp name! Use only letters and numbers.";
        public String warpListHeader = "&6Available warps:";
        public String warpListEntry = "&7- &a{0} &7({1}) [{2}] &8({3} {4}, {5}, {6})";
        public String warpListEmpty = "&cNo warps available!";
        public String unsafeLocation = "&cUnsafe location! Teleport cancelled.";
        public String restrictedWorld = "&cYou cannot create warps in this world!";
        public String noTeleportWorld = "&cYou cannot teleport to this world!";
        public String insufficientFunds = "&cYou need {0} to create/use this warp!";
        public String cooldownActive = "&cYou must wait {0} before using this command again!";
        public String warpNoPermission = "&cYou don't have permission to access this warp!";
        public String warpPrivate = "&cThis is a private warp!";
        public String warpSignCreated = "&aWarp sign created for '{0}'!";
        public String warpSignUsed = "&aTeleporting via warp sign...";
        
        // Category messages
        public String categoryNotFound = "&cCategory '{0}' not found!";
        public String categoryListHeader = "&6Warp categories:";
        public String categoryListEntry = "&7- &a{0} &7({1} warps)";
        public String categoryWarpsHeader = "&6Warps in category '{0}':";
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
