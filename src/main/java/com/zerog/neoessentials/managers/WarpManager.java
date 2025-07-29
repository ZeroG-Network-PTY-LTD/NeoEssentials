package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.WarpConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Warp management system for NeoEssentials
 * Handles warp creation, deletion, teleportation, and category management
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class WarpManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpManager.class);
    private static WarpManager instance;
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final Map<String, WarpLocation> warps;
    private final Map<UUID, Long> teleportCooldowns;
    private final Map<UUID, Long> warmupTasks;
    
    private WarpManager() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.warps = new ConcurrentHashMap<>();
        this.teleportCooldowns = new ConcurrentHashMap<>();
        this.warmupTasks = new ConcurrentHashMap<>();
        
        loadWarps();
    }
    
    public static WarpManager getInstance() {
        if (instance == null) {
            instance = new WarpManager();
        }
        return instance;
    }
    
    /**
     * Create or update a warp
     */
    public boolean setWarp(ServerPlayer player, String warpName, String category) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (!config.isEnabled()) {
            MessageUtil.sendMessage(player, config.messages.systemDisabled);
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(player, "essentials.setwarp")) {
            MessageUtil.sendMessage(player, config.messages.noPermission);
            return false;
        }
        
        // Validate warp name
        if (!isValidWarpName(warpName)) {
            MessageUtil.sendMessage(player, config.messages.invalidWarpName
                .replace("{NAME}", warpName)
                .replace("{MIN}", String.valueOf(config.minWarpNameLength))
                .replace("{MAX}", String.valueOf(config.maxWarpNameLength)));
            return false;
        }
        
        // Check if warp name is reserved
        if (config.reservedWarpNames.contains(warpName.toLowerCase())) {
            MessageUtil.sendMessage(player, config.messages.reservedWarpName.replace("{NAME}", warpName));
            return false;
        }
        
        // Validate category
        if (category != null && !category.isEmpty()) {
            if (!config.allowedCategories.isEmpty() && !config.allowedCategories.contains(category)) {
                MessageUtil.sendMessage(player, config.messages.invalidCategory
                    .replace("{CATEGORY}", category));
                return false;
            }
        } else {
            category = config.defaultCategory;
        }
        
        // Check world restrictions
        String worldName = player.level().dimension().location().toString();
        if (config.worldRestrictions.enabled) {
            if (config.worldRestrictions.mode == WarpConfig.WorldRestrictions.RestrictionMode.BLACKLIST &&
                config.worldRestrictions.worlds.contains(worldName)) {
                MessageUtil.sendMessage(player, config.messages.worldBlacklisted.replace("{WORLD}", worldName));
                return false;
            }
            if (config.worldRestrictions.mode == WarpConfig.WorldRestrictions.RestrictionMode.WHITELIST &&
                !config.worldRestrictions.worlds.contains(worldName)) {
                MessageUtil.sendMessage(player, config.messages.worldNotWhitelisted.replace("{WORLD}", worldName));
                return false;
            }
        }
        
        // Check cost
        if (config.costs.createWarpCost > 0) {
            EconomyManager economyManager = EconomyManager.getInstance();
            if (!economyManager.hasBalance(player.getUUID(), config.costs.createWarpCost)) {
                MessageUtil.sendMessage(player, config.messages.insufficientFunds
                    .replace("{COST}", economyManager.formatCurrency(config.costs.createWarpCost)));
                return false;
            }
            economyManager.withdrawBalance(player.getUUID(), config.costs.createWarpCost, "Warp creation: " + warpName);
        }
        
        boolean isNewWarp = !warps.containsKey(warpName.toLowerCase());
        
        // Create warp location
        WarpLocation warpLocation = new WarpLocation(
            warpName,
            worldName,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            category,
            player.getUUID(),
            System.currentTimeMillis(),
            true // public by default
        );
        
        // Save warp
        warps.put(warpName.toLowerCase(), warpLocation);
        saveWarps();
        
        String message = isNewWarp ? config.messages.warpCreated : config.messages.warpUpdated;
        MessageUtil.sendMessage(player, message
            .replace("{NAME}", warpName)
            .replace("{CATEGORY}", category));
        
        LOGGER.info("Player {} {} warp '{}' in category '{}' at {} in {}", 
            player.getName().getString(),
            isNewWarp ? "created" : "updated",
            warpName,
            category,
            String.format("%.1f, %.1f, %.1f", warpLocation.x, warpLocation.y, warpLocation.z),
            worldName);
        
        return true;
    }
    
    /**
     * Delete a warp
     */
    public boolean deleteWarp(ServerPlayer player, String warpName) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (!config.isEnabled()) {
            MessageUtil.sendMessage(player, config.messages.systemDisabled);
            return false;
        }
        
        WarpLocation warp = warps.get(warpName.toLowerCase());
        if (warp == null) {
            MessageUtil.sendMessage(player, config.messages.warpNotFound.replace("{NAME}", warpName));
            return false;
        }
        
        // Check permission - owner or admin
        if (!warp.owner.equals(player.getUUID()) && !PermissionUtil.hasPermission(player, "essentials.delwarp.others")) {
            MessageUtil.sendMessage(player, config.messages.notWarpOwner.replace("{NAME}", warpName));
            return false;
        }
        
        warps.remove(warpName.toLowerCase());
        saveWarps();
        
        MessageUtil.sendMessage(player, config.messages.warpDeleted.replace("{NAME}", warpName));
        
        LOGGER.info("Player {} deleted warp '{}'", player.getName().getString(), warpName);
        return true;
    }
    
    /**
     * Teleport player to warp
     */
    public boolean teleportToWarp(ServerPlayer player, String warpName) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (!config.isEnabled()) {
            MessageUtil.sendMessage(player, config.messages.systemDisabled);
            return false;
        }
        
        WarpLocation warp = warps.get(warpName.toLowerCase());
        if (warp == null) {
            MessageUtil.sendMessage(player, config.messages.warpNotFound.replace("{NAME}", warpName));
            return false;
        }
        
        // Check if player can access this warp
        if (!canAccessWarp(player, warp)) {
            MessageUtil.sendMessage(player, config.messages.noWarpAccess.replace("{NAME}", warpName));
            return false;
        }
        
        // Check cooldown
        if (isOnCooldown(player)) {
            long remainingTime = getRemainingCooldown(player);
            MessageUtil.sendMessage(player, config.messages.warpCooldown
                .replace("{TIME}", MessageUtil.formatTime(remainingTime)));
            return false;
        }
        
        // Check cost
        double cost = getCategoryWarpCost(warp.category);
        if (cost > 0) {
            EconomyManager economyManager = EconomyManager.getInstance();
            if (!economyManager.hasBalance(player.getUUID(), cost)) {
                MessageUtil.sendMessage(player, config.messages.insufficientFunds
                    .replace("{COST}", economyManager.formatCurrency(cost)));
                return false;
            }
        }
        
        // Validate warp location safety
        if (config.safety.enabled && !isLocationSafe(warp)) {
            MessageUtil.sendMessage(player, config.messages.warpUnsafe.replace("{NAME}", warpName));
            
            if (config.safety.findSafeLocation) {
                WarpLocation safeLocation = findSafeLocation(warp);
                if (safeLocation != null) {
                    warp = safeLocation;
                    MessageUtil.sendMessage(player, config.messages.warpSafeLocationFound);
                } else {
                    MessageUtil.sendMessage(player, config.messages.warpSafeLocationNotFound);
                    return false;
                }
            } else {
                return false;
            }
        }
        
        // Handle warmup
        if (config.warmup.enabled && config.warmup.warpWarmupSeconds > 0) {
            startWarmup(player, warp, warpName);
            return true;
        }
        
        // Immediate teleport
        return performTeleport(player, warp, warpName);
    }
    
    /**
     * Get list of warps accessible to player
     */
    public List<WarpLocation> getAccessibleWarps(ServerPlayer player) {
        return warps.values().stream()
            .filter(warp -> canAccessWarp(player, warp))
            .sorted(Comparator.comparing(w -> w.name))
            .collect(Collectors.toList());
    }
    
    /**
     * Get warps by category
     */
    public Map<String, List<WarpLocation>> getWarpsByCategory(ServerPlayer player) {
        return getAccessibleWarps(player).stream()
            .collect(Collectors.groupingBy(warp -> warp.category));
    }
    
    /**
     * Get all warp names for tab completion
     */
    public List<String> getWarpNames(ServerPlayer player) {
        return getAccessibleWarps(player).stream()
            .map(warp -> warp.name)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Check if player can access warp
     */
    private boolean canAccessWarp(ServerPlayer player, WarpLocation warp) {
        WarpConfig config = configManager.getWarpConfig();
        
        // Check if warp is public
        if (warp.isPublic) {
            return true;
        }
        
        // Check if player is the owner
        if (warp.owner.equals(player.getUUID())) {
            return true;
        }
        
        // Check admin permission
        if (PermissionUtil.hasPermission(player, "essentials.warp.admin")) {
            return true;
        }
        
        // Check category-specific permission
        String categoryPermission = "essentials.warp." + warp.category.toLowerCase();
        if (PermissionUtil.hasPermission(player, categoryPermission)) {
            return true;
        }
        
        // Check specific warp permission
        String warpPermission = "essentials.warp." + warp.name.toLowerCase();
        if (PermissionUtil.hasPermission(player, warpPermission)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get warp cost based on category
     */
    private double getCategoryWarpCost(String category) {
        WarpConfig config = configManager.getWarpConfig();
        return config.costs.categoryCosts.getOrDefault(category, config.costs.defaultWarpCost);
    }
    
    /**
     * Check if player is on teleport cooldown
     */
    public boolean isOnCooldown(ServerPlayer player) {
        WarpConfig config = configManager.getWarpConfig();
        if (!config.cooldown.enabled || config.cooldown.warpCooldownSeconds <= 0) {
            return false;
        }
        
        // Check if player has cooldown bypass
        if (PermissionUtil.hasPermission(player, "essentials.warp.cooldown.bypass")) {
            return false;
        }
        
        Long lastTeleport = teleportCooldowns.get(player.getUUID());
        if (lastTeleport == null) {
            return false;
        }
        
        long cooldownTime = config.cooldown.warpCooldownSeconds * 1000L;
        return System.currentTimeMillis() - lastTeleport < cooldownTime;
    }
    
    /**
     * Get remaining cooldown time in milliseconds
     */
    public long getRemainingCooldown(ServerPlayer player) {
        WarpConfig config = configManager.getWarpConfig();
        Long lastTeleport = teleportCooldowns.get(player.getUUID());
        if (lastTeleport == null) {
            return 0;
        }
        
        long cooldownTime = config.cooldown.warpCooldownSeconds * 1000L;
        long elapsed = System.currentTimeMillis() - lastTeleport;
        return Math.max(0, cooldownTime - elapsed);
    }
    
    /**
     * Start warmup process for teleportation
     */
    private void startWarmup(ServerPlayer player, WarpLocation warp, String warpName) {
        WarpConfig config = configManager.getWarpConfig();
        
        // Cancel existing warmup
        cancelWarmup(player);
        
        MessageUtil.sendMessage(player, config.messages.warpWarmupStarted
            .replace("{TIME}", String.valueOf(config.warmup.warpWarmupSeconds))
            .replace("{NAME}", warpName));
        
        // Store warmup task
        long warmupEnd = System.currentTimeMillis() + (config.warmup.warpWarmupSeconds * 1000L);
        warmupTasks.put(player.getUUID(), warmupEnd);
        
        // Schedule teleport (placeholder for server scheduler integration)
    }
    
    /**
     * Cancel warmup for player
     */
    public void cancelWarmup(ServerPlayer player) {
        warmupTasks.remove(player.getUUID());
    }
    
    /**
     * Perform the actual teleportation
     */
    private boolean performTeleport(ServerPlayer player, WarpLocation warp, String warpName) {
        WarpConfig config = configManager.getWarpConfig();
        
        try {
            // Find the target world
            ServerLevel targetWorld = player.getServer().getLevel(Level.OVERWORLD); // Placeholder - would need proper world lookup
            
            if (targetWorld == null) {
                MessageUtil.sendMessage(player, config.messages.worldNotFound.replace("{WORLD}", warp.world));
                return false;
            }
            
            // Charge cost after successful teleport
            double cost = getCategoryWarpCost(warp.category);
            if (cost > 0) {
                EconomyManager economyManager = EconomyManager.getInstance();
                economyManager.withdrawBalance(player.getUUID(), cost, "Warp teleport: " + warpName);
            }
            
            // Set cooldown
            teleportCooldowns.put(player.getUUID(), System.currentTimeMillis());
            
            // Perform teleport
            player.teleportTo(targetWorld, warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
            
            MessageUtil.sendMessage(player, config.messages.warpTeleported.replace("{NAME}", warpName));
            
            LOGGER.info("Player {} teleported to warp '{}' at {}, {}, {} in {}", 
                player.getName().getString(), warpName, warp.x, warp.y, warp.z, warp.world);
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} to warp '{}'", player.getName().getString(), warpName, e);
            MessageUtil.sendMessage(player, config.messages.teleportFailed);
            return false;
        }
    }
    
    /**
     * Validate warp name
     */
    private boolean isValidWarpName(String warpName) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (warpName == null || warpName.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = warpName.trim();
        
        if (trimmed.length() < config.minWarpNameLength || trimmed.length() > config.maxWarpNameLength) {
            return false;
        }
        
        // Check allowed characters
        return trimmed.matches(config.allowedWarpNamePattern);
    }
    
    /**
     * Check if location is safe for teleportation
     */
    private boolean isLocationSafe(WarpLocation location) {
        // Placeholder implementation - would implement proper safety checks
        return location.y > 0 && location.y < 256;
    }
    
    /**
     * Find a safe location near the given location
     */
    private WarpLocation findSafeLocation(WarpLocation original) {
        // Placeholder implementation - would implement safe location finding
        return original;
    }
    
    /**
     * Load warps from storage
     */
    private void loadWarps() {
        // Placeholder - would load from persistent storage
        LOGGER.info("Loaded {} warps from storage", warps.size());
    }
    
    /**
     * Save warps to storage
     */
    private void saveWarps() {
        // Placeholder - would save to persistent storage
        LOGGER.debug("Saved {} warps to storage", warps.size());
    }
    
    /**
     * Clean up expired cooldowns and warmups
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up expired cooldowns
        teleportCooldowns.entrySet().removeIf(entry -> {
            WarpConfig config = configManager.getWarpConfig();
            long cooldownTime = config.cooldown.warpCooldownSeconds * 1000L;
            return currentTime - entry.getValue() > cooldownTime;
        });
        
        // Clean up expired warmups
        warmupTasks.entrySet().removeIf(entry -> currentTime > entry.getValue());
    }
    
    /**
     * Warp location data class
     */
    public static class WarpLocation {
        public final String name;
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;
        public final String category;
        public final UUID owner;
        public final long createdTime;
        public final boolean isPublic;
        
        public WarpLocation(String name, String world, double x, double y, double z, 
                           float yaw, float pitch, String category, UUID owner, 
                           long createdTime, boolean isPublic) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.category = category;
            this.owner = owner;
            this.createdTime = createdTime;
            this.isPublic = isPublic;
        }
    }
}
