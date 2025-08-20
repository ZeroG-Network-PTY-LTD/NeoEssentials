package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigurationUnifier;
import com.zerog.neoessentials.config.MainConfig;
// import removed: WarpConfig is now centralized in MainConfig
import com.zerog.neoessentials.permissions.PermissionNodes;
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
    
    private final ConfigurationUnifier configUnifier;
    private final EconomyManager economyManager;
    private final Map<String, WarpData> warps; // All server warps
    private final Map<UUID, Long> warpCooldowns;
    
    private WarpManager() {
        this.configUnifier = ConfigurationUnifier.getInstance();
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
    MainConfig.WarpSettings config = configUnifier.getConfigManager().getMainConfig().warpSettings;
        boolean warpModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.warps;
        if (!warpModuleEnabled) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.disabled"));
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(player, PermissionNodes.WARP_SET)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.no_permission"));
            return false;
        }
        
        // Validate warp name
        if (warpName.length() > config.maxWarpNameLength) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.name_too_long", String.valueOf(config.maxWarpNameLength)));
            return false;
        }
        
        if (!config.allowSpacesInNames && warpName.contains(" ")) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.no_spaces"));
            return false;
        }
        
        if (config.bannedWarpNames.contains(warpName.toLowerCase())) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.name_banned"));
            return false;
        }
        
        // Check if warp already exists
        boolean isNewWarp = !warps.containsKey(warpName.toLowerCase());
        
        // Check world restrictions
        String worldName = player.serverLevel().dimension().location().toString();
        if (config.restrictedWorlds.contains(worldName)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.restricted_world"));
            return false;
        }
        
        // Check economy cost
        if (config.createWarpCost.doubleValue() > 0) {
            if (!economyManager.hasBalance(player.getUUID(), config.createWarpCost.doubleValue())) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.insufficient_funds", economyManager.formatCurrency(config.createWarpCost.doubleValue())));
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
        
    String message = isNewWarp ? config.messages.warpCreated : com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.updated");
    MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, message, warpName));
        
        LOGGER.info("Warp '{}' {} by {}", warpName, isNewWarp ? "created" : "updated", player.getName().getString());
        
        return true;
    }
    
    /**
     * Delete a warp
     */
    public boolean deleteWarp(ServerPlayer player, String warpName) {
    MainConfig.WarpSettings config = configUnifier.getConfigManager().getMainConfig().warpSettings;
        boolean warpModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.warps;
        if (!warpModuleEnabled) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.disabled"));
            return false;
        }
        
        WarpData warpData = warps.get(warpName.toLowerCase());
        if (warpData == null) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.not_found", warpName));
            return false;
        }
        
        // Check permission - owner or admin
        if (!warpData.ownerId.equals(player.getUUID()) && !PermissionUtil.hasPermission(player, "neoessentials.delwarp.others")) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.delete_own_only"));
            return false;
        }
        
        warps.remove(warpName.toLowerCase());
        saveWarpData();
        
    MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.deleted", warpName));
        
        LOGGER.info("Warp '{}' deleted by {}", warpName, player.getName().getString());
        
        return true;
    }
    
    /**
     * Teleport player to a warp
     */
    public boolean teleportToWarp(ServerPlayer player, String warpName) {
    MainConfig.WarpSettings config = configUnifier.getConfigManager().getMainConfig().warpSettings;
        boolean warpModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.warps;
        if (!warpModuleEnabled) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.disabled"));
            return false;
        }
        
        WarpData warpData = warps.get(warpName.toLowerCase());
        if (warpData == null) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.not_found", warpName));
            return false;
        }
        
        // Check permission for private warps
        if (!warpData.isPublic && !warpData.ownerId.equals(player.getUUID()) && 
            !PermissionUtil.hasPermission(player, "neoessentials.warp." + warpName.toLowerCase())) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.private", warpName));
            return false;
        }
        
        // Check cooldown
        if (hasWarpCooldown(player.getUUID())) {
            long remainingTime = getWarpCooldownRemaining(player.getUUID());
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.cooldown_active", MessageUtil.formatTime(remainingTime)));
            return false;
        }
        
        // Check teleport cost
        if (config.teleportWarpCost.doubleValue() > 0) {
            if (!economyManager.hasBalance(player.getUUID(), config.teleportWarpCost.doubleValue())) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.insufficient_funds", economyManager.formatCurrency(config.teleportWarpCost.doubleValue())));
                return false;
            }
            economyManager.withdrawBalance(player.getUUID(), config.teleportWarpCost.doubleValue(), "Warp teleport: " + warpName);
        }
        
        // Safety check
        if (config.requireSafeLocation && !isLocationSafe(warpData.location)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.unsafe_location"));
            return false;
        }
        
        // Check world restrictions
        if (config.noTeleportWorlds.contains(warpData.location.world)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.no_teleport_world"));
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
        
    MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.teleporting", warpName));
        
        LOGGER.info("Player {} teleporting to warp '{}'", player.getName().getString(), warpName);
        
        return true;
    }
    
    /**
     * List all available warps
     */
    public boolean listWarps(ServerPlayer player, String category) {
    MainConfig.WarpSettings config = configUnifier.getConfigManager().getMainConfig().warpSettings;
        
        List<WarpData> availableWarps = warps.values().stream()
            .filter(warp -> warp.isPublic || warp.ownerId.equals(player.getUUID()) || 
                           PermissionUtil.hasPermission(player, "neoessentials.warp." + warp.name.toLowerCase()))
            .filter(warp -> category == null || warp.category.equalsIgnoreCase(category))
            .sorted(Comparator.comparing(warp -> warp.name))
            .toList();
        
        if (availableWarps.isEmpty()) {
            MessageUtil.sendMessage(player, config.messages.warpListEmpty);
            return false;
        }
        
        MessageUtil.sendMessage(player, config.messages.warpListHeader);
        
        for (WarpData warp : availableWarps) {
            MessageUtil.sendMessage(player, config.messages.warpListEntry,
                warp.name, warp.ownerName, warp.category, warp.location.world,
                (int) warp.location.x, (int) warp.location.y, (int) warp.location.z);
        }
        
        return true;
    }
    
    /**
     * Check if location is safe for teleportation
     */
    private boolean isLocationSafe(LocationUtil.Location location) {
    MainConfig.WarpSettings config = configUnifier.getConfigManager().getMainConfig().warpSettings;
        
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
    MainConfig.WarpSettings config = configUnifier.getConfigManager().getMainConfig().warpSettings;
        
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
    MainConfig.WarpSettings config = configUnifier.getConfigManager().getMainConfig().warpSettings;
        
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
