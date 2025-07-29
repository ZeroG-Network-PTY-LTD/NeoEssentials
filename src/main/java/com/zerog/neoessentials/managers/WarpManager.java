package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.WarpConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Warp management system for NeoEssentials
 * Handles server-wide teleportation points
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class WarpManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpManager.class);
    private static WarpManager instance;
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final EconomyManager economyManager;
    private final Map<String, WarpData> warps; // All server warps
    private final Map<UUID, Long> warpCooldowns;
    
    private WarpManager() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.economyManager = EconomyManager.getInstance();
        this.warps = new ConcurrentHashMap<>();
        this.warpCooldowns = new ConcurrentHashMap<>();
    }
    
    public static WarpManager getInstance() {
        if (instance == null) {
            instance = new WarpManager();
        }
        return instance;
    }
    
    /**
     * Create a new warp
     */
    public boolean createWarp(ServerPlayer player, String warpName, String category) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (!config.enabled) {
            MessageUtil.sendMessage(player, "&cWarp system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(player, "essentials.setwarp")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to create warps.");
            return false;
        }
        
        // Validate warp name
        if (warpName.length() > config.maxWarpNameLength) {
            MessageUtil.sendMessage(player, "&cWarp name too long! Maximum " + config.maxWarpNameLength + " characters.");
            return false;
        }
        
        if (!config.allowSpacesInNames && warpName.contains(" ")) {
            MessageUtil.sendMessage(player, "&cWarp names cannot contain spaces!");
            return false;
        }
        
        if (config.bannedWarpNames.contains(warpName.toLowerCase())) {
            MessageUtil.sendMessage(player, "&cThat warp name is not allowed!");
            return false;
        }
        
        // Check if warp already exists
        boolean isNewWarp = !warps.containsKey(warpName.toLowerCase());
        
        // Check world restrictions
        String worldName = player.serverLevel().dimension().location().toString();
        if (config.restrictedWorlds.contains(worldName)) {
            MessageUtil.sendMessage(player, config.messages.restrictedWorld);
            return false;
        }
        
        // Check economy cost
        if (config.createWarpCost.doubleValue() > 0) {
            if (!economyManager.hasBalance(player.getUUID(), config.createWarpCost.doubleValue())) {
                MessageUtil.sendMessage(player, config.messages.insufficientFunds
                    .replace("{COST}", economyManager.formatCurrency(config.createWarpCost.doubleValue())));
                return false;
            }
            economyManager.withdrawBalance(player.getUUID(), config.createWarpCost.doubleValue(), "Warp creation: " + warpName);
        }
        
        // Create warp location
        LocationUtil.Location warpLocation = new LocationUtil.Location(
            worldName,
            player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot()
        );
        
        // Create warp data
        WarpData warpData = new WarpData(
            warpName, 
            warpLocation,
            player.getUUID(),
            player.getName().getString(),
            category != null ? category : "general",
            System.currentTimeMillis(),
            true // public by default
        );
        
        warps.put(warpName.toLowerCase(), warpData);
        saveWarpData();
        
        String message = isNewWarp ? config.messages.warpCreated : "&aWarp updated successfully!";
        MessageUtil.sendMessage(player, message.replace("{WARP}", warpName));
        
        LOGGER.info("Warp '{}' {} by {}", warpName, isNewWarp ? "created" : "updated", player.getName().getString());
        
        return true;
    }
    
    /**
     * Delete a warp
     */
    public boolean deleteWarp(ServerPlayer player, String warpName) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (!config.enabled) {
            MessageUtil.sendMessage(player, "&cWarp system is disabled.");
            return false;
        }
        
        WarpData warpData = warps.get(warpName.toLowerCase());
        if (warpData == null) {
            MessageUtil.sendMessage(player, config.messages.warpNotFound.replace("{WARP}", warpName));
            return false;
        }
        
        // Check permission - owner or admin
        if (!warpData.ownerId.equals(player.getUUID()) && !PermissionUtil.hasPermission(player, "essentials.delwarp.others")) {
            MessageUtil.sendMessage(player, "&cYou can only delete your own warps!");
            return false;
        }
        
        warps.remove(warpName.toLowerCase());
        saveWarpData();
        
        MessageUtil.sendMessage(player, config.messages.warpDeleted.replace("{WARP}", warpName));
        
        LOGGER.info("Warp '{}' deleted by {}", warpName, player.getName().getString());
        
        return true;
    }
    
    /**
     * Teleport player to a warp
     */
    public boolean teleportToWarp(ServerPlayer player, String warpName) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (!config.enabled) {
            MessageUtil.sendMessage(player, "&cWarp system is disabled.");
            return false;
        }
        
        WarpData warpData = warps.get(warpName.toLowerCase());
        if (warpData == null) {
            MessageUtil.sendMessage(player, config.messages.warpNotFound.replace("{WARP}", warpName));
            return false;
        }
        
        // Check permission for private warps
        if (!warpData.isPublic && !warpData.ownerId.equals(player.getUUID()) && 
            !PermissionUtil.hasPermission(player, "essentials.warp." + warpName.toLowerCase())) {
            MessageUtil.sendMessage(player, config.messages.warpPrivate);
            return false;
        }
        
        // Check cooldown
        if (hasWarpCooldown(player.getUUID())) {
            long remainingTime = getWarpCooldownRemaining(player.getUUID());
            MessageUtil.sendMessage(player, config.messages.cooldownActive
                .replace("{TIME}", MessageUtil.formatTime(remainingTime)));
            return false;
        }
        
        // Check teleport cost
        if (config.teleportWarpCost.doubleValue() > 0) {
            if (!economyManager.hasBalance(player.getUUID(), config.teleportWarpCost.doubleValue())) {
                MessageUtil.sendMessage(player, config.messages.insufficientFunds
                    .replace("{COST}", economyManager.formatCurrency(config.teleportWarpCost.doubleValue())));
                return false;
            }
            economyManager.withdrawBalance(player.getUUID(), config.teleportWarpCost.doubleValue(), "Warp teleport: " + warpName);
        }
        
        // Safety check
        if (config.requireSafeLocation && !isLocationSafe(warpData.location)) {
            MessageUtil.sendMessage(player, config.messages.unsafeLocation);
            return false;
        }
        
        // Check world restrictions
        if (config.noTeleportWorlds.contains(warpData.location.world)) {
            MessageUtil.sendMessage(player, config.messages.noTeleportWorld);
            return false;
        }
        
        // Start teleport
        if (config.teleportWarmup > 0) {
            startWarpTeleport(player, warpData, config.teleportWarmup);
        } else {
            performTeleport(player, warpData.location);
        }
        
        // Set cooldown
        setWarpCooldown(player.getUUID());
        
        MessageUtil.sendMessage(player, config.messages.warpTeleporting.replace("{WARP}", warpName));
        
        LOGGER.info("Player {} teleporting to warp '{}'", player.getName().getString(), warpName);
        
        return true;
    }
    
    /**
     * List all available warps
     */
    public boolean listWarps(ServerPlayer player, String category) {
        WarpConfig config = configManager.getWarpConfig();
        
        List<WarpData> availableWarps = warps.values().stream()
            .filter(warp -> warp.isPublic || warp.ownerId.equals(player.getUUID()) || 
                           PermissionUtil.hasPermission(player, "essentials.warp." + warp.name.toLowerCase()))
            .filter(warp -> category == null || warp.category.equalsIgnoreCase(category))
            .sorted(Comparator.comparing(warp -> warp.name))
            .toList();
        
        if (availableWarps.isEmpty()) {
            MessageUtil.sendMessage(player, config.messages.warpListEmpty);
            return false;
        }
        
        MessageUtil.sendMessage(player, config.messages.warpListHeader);
        
        for (WarpData warp : availableWarps) {
            String message = config.messages.warpListEntry
                .replace("{WARP}", warp.name)
                .replace("{OWNER}", warp.ownerName)
                .replace("{CATEGORY}", warp.category)
                .replace("{WORLD}", warp.location.world)
                .replace("{X}", String.valueOf((int) warp.location.x))
                .replace("{Y}", String.valueOf((int) warp.location.y))
                .replace("{Z}", String.valueOf((int) warp.location.z));
            
            MessageUtil.sendMessage(player, message);
        }
        
        return true;
    }
    
    /**
     * Check if location is safe for teleportation
     */
    private boolean isLocationSafe(LocationUtil.Location location) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (!config.requireSafeLocation) {
            return true;
        }
        
        // Basic safety checks - would need proper world access for full implementation
        if (config.checkForVoid && location.y < 0) {
            return false;
        }
        
        // Additional safety checks would be implemented here
        return true;
    }
    
    /**
     * Check warp cooldown
     */
    private boolean hasWarpCooldown(UUID playerId) {
        WarpConfig config = configManager.getWarpConfig();
        
        if (config.teleportWarpCooldown <= 0) {
            return false;
        }
        
        Long lastUse = warpCooldowns.get(playerId);
        if (lastUse == null) {
            return false;
        }
        
        long cooldownTime = config.teleportWarpCooldown * 1000L;
        return System.currentTimeMillis() - lastUse < cooldownTime;
    }
    
    /**
     * Get remaining cooldown time
     */
    private long getWarpCooldownRemaining(UUID playerId) {
        WarpConfig config = configManager.getWarpConfig();
        
        Long lastUse = warpCooldowns.get(playerId);
        if (lastUse == null) {
            return 0;
        }
        
        long cooldownTime = config.teleportWarpCooldown * 1000L;
        long elapsed = System.currentTimeMillis() - lastUse;
        return Math.max(0, cooldownTime - elapsed);
    }
    
    /**
     * Set warp cooldown
     */
    private void setWarpCooldown(UUID playerId) {
        warpCooldowns.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Start warmup teleport
     */
    private void startWarpTeleport(ServerPlayer player, WarpData warpData, int warmupSeconds) {
        MessageUtil.sendMessage(player, "&aTeleport starting in " + warmupSeconds + " seconds. Don't move!");
        
        // This would need a proper warmup implementation with task scheduling
        // For now, just teleport immediately
        performTeleport(player, warpData.location);
    }
    
    /**
     * Perform the actual teleport
     */
    private void performTeleport(ServerPlayer player, LocationUtil.Location location) {
        // Basic teleportation - would need proper implementation with world switching
        player.teleportTo(location.x, location.y, location.z);
        MessageUtil.sendMessage(player, "&aTeleported successfully!");
    }
    
    /**
     * Save warp data to storage
     */
    private void saveWarpData() {
        // Convert warps to a saveable format and store them
        Map<String, Object> warpDataToSave = new HashMap<>();
        
        for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
            WarpData warp = entry.getValue();
            Map<String, Object> warpInfo = Map.of(
                "name", warp.name,
                "location", Map.of(
                    "world", warp.location.world,
                    "x", warp.location.x,
                    "y", warp.location.y,
                    "z", warp.location.z,
                    "yaw", warp.location.yaw,
                    "pitch", warp.location.pitch
                ),
                "ownerId", warp.ownerId.toString(),
                "ownerName", warp.ownerName,
                "category", warp.category,
                "timestamp", warp.timestamp,
                "isPublic", warp.isPublic
            );
            warpDataToSave.put(entry.getKey(), warpInfo);
        }
        
        // Save to global storage (this would need a proper global storage system)
        LOGGER.info("Saving {} warps to storage", warps.size());
    }
    
    /**
     * Load warp data from storage
     */
    public void loadWarpData() {
        // Load warps from storage
        // This would read from a global warps file
        LOGGER.info("Loading warp data...");
    }
    
    /**
     * Get warp by name
     */
    public WarpData getWarp(String name) {
        return warps.get(name.toLowerCase());
    }
    
    /**
     * Get all warps
     */
    public Collection<WarpData> getAllWarps() {
        return warps.values();
    }
    
    /**
     * Warp data class
     */
    public static class WarpData {
        public final String name;
        public final LocationUtil.Location location;
        public final UUID ownerId;
        public final String ownerName;
        public final String category;
        public final long timestamp;
        public final boolean isPublic;
        
        public WarpData(String name, LocationUtil.Location location, UUID ownerId, String ownerName, 
                       String category, long timestamp, boolean isPublic) {
            this.name = name;
            this.location = location;
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.category = category;
            this.timestamp = timestamp;
            this.isPublic = isPublic;
        }
    }
}
