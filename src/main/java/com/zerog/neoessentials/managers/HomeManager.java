package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;
// import removed: HomeConfig is now centralized in MainConfig
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Home management system for NeoEssentials
 * Handles home creation, deletion, teleportation, and validation
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HomeManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeManager.class);
    private static HomeManager instance;
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Long> teleportCooldowns;
    private final Map<UUID, Long> warmupTasks;
    
    private HomeManager() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.teleportCooldowns = new ConcurrentHashMap<>();
        this.warmupTasks = new ConcurrentHashMap<>();
    }
    
    public static HomeManager getInstance() {
        if (instance == null) {
            instance = new HomeManager();
        }
        return instance;
    }
    
    /**
     * Create or update a home for a player
     */
    public boolean setHome(ServerPlayer player, String homeName) {
    MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
        
    boolean homeModuleEnabled = configManager.getMainConfig().modules.homes;
            if (!homeModuleEnabled) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.disabled"));
                return false;
            }
        
        // Validate home name
            if (!isValidHomeName(homeName)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.invalid_name", homeName));
                return false;
            }
        
        // Check home limit
    int currentHomes = playerDataManager.getHomeCount(player.getUUID());
    int maxHomes = getMaxHomes(player);
        
        boolean isNewHome = !playerDataManager.hasHome(player.getUUID(), homeName);
            if (isNewHome && currentHomes >= maxHomes) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.max_homes_reached", String.valueOf(maxHomes)));
                return false;
            }
        
        // Check world restrictions
        String worldName = player.level().dimension().location().toString();
            if (config.restrictedWorlds.contains(worldName)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.restricted_world"));
                return false;
            }
        
        // Check cost
    if (config.useSetHomeCost && config.setHomeCost > 0) {
            EconomyManager economyManager = EconomyManager.getInstance();
                if (!economyManager.hasBalance(player.getUUID(), config.setHomeCost)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.insufficient_funds", economyManager.formatCurrency(config.setHomeCost)));
                    return false;
                }
            economyManager.withdrawBalance(player.getUUID(), config.setHomeCost, "Home creation: " + homeName);
        }
        
        // Create home location
        LocationUtil.Location homeLocation = new LocationUtil.Location(
            worldName,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            System.currentTimeMillis()
        );
        
        // Save home
        playerDataManager.setHome(player.getUUID(), homeName, homeLocation);
        
    String message = isNewHome ? "neoessentials.home.set" : "neoessentials.home.set";
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(message, homeName));
        
        LOGGER.info("Player {} {} home '{}' at {} in {}", 
            player.getName().getString(),
            isNewHome ? "created" : "updated",
            homeName,
            String.format("%.1f, %.1f, %.1f", homeLocation.x, homeLocation.y, homeLocation.z),
            worldName);
        
        return true;
    }
    
    /**
     * Delete a home for a player
     */
    public boolean deleteHome(ServerPlayer player, String homeName) {
    // ...existing code...
        
    boolean homeModuleEnabled = configManager.getMainConfig().modules.homes;
            if (!homeModuleEnabled) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.disabled"));
                return false;
            }
        
            if (!playerDataManager.hasHome(player.getUUID(), homeName)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.not_found", homeName));
                return false;
            }
        
        playerDataManager.deleteHome(player.getUUID(), homeName);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.deleted", homeName));
        
        LOGGER.info("Player {} deleted home '{}'", player.getName().getString(), homeName);
        return true;
    }
    
    /**
     * Teleport player to their home
     */
    public boolean teleportToHome(ServerPlayer player, String homeName) {
    MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
        
    boolean homeModuleEnabled = configManager.getMainConfig().modules.homes;
            if (!homeModuleEnabled) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.disabled"));
                return false;
            }
        
        // Check if home exists
    LocationUtil.Location home = playerDataManager.getHome(player.getUUID(), homeName);
            if (home == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.not_found", homeName));
                return false;
            }
        
        // Check cooldown
        if (isOnCooldown(player)) {
            long remainingTime = getRemainingCooldown(player);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.cooldown_active", MessageUtil.formatTime(remainingTime)));
            return false;
        }
        
        // Check cost
    if (config.teleportHomeCost > 0) {
            EconomyManager economyManager = EconomyManager.getInstance();
                if (!economyManager.hasBalance(player.getUUID(), config.teleportHomeCost)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.insufficient_funds", economyManager.formatCurrency(config.teleportHomeCost)));
                    return false;
                }
        }
        
        // Validate home location safety if required
        if (config.requireSafeLocation && !isLocationSafe(home)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.unsafe_location"));
            return false;
        }
        
        // Handle warmup
        if (config.teleportWarmup > 0) {
            startWarmup(player, home, homeName);
            return true;
        }
        
        // Immediate teleport
        return performTeleport(player, home, homeName);
    }
    
    /**
     * Get list of player's homes
     */
    public List<String> getPlayerHomes(UUID playerUUID) {
        return playerDataManager.getHomeNames(playerUUID);
    }
    
    /**
     * List all homes for a player
     */
    public boolean listHomes(ServerPlayer player) {
    // ...existing code...
        
    List<String> homes = getPlayerHomes(player.getUUID());
            if (homes.isEmpty()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.list_none"));
                return false;
            }
        
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.list_header", String.valueOf(homes.size()), String.valueOf(getMaxHomes(player))));
        
        for (String homeName : homes) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.home.list_entry", homeName));
        }
        
        return true;
    }
    
    /**
     * Get player's home count
     */
    public int getHomeCount(UUID playerUUID) {
        return playerDataManager.getHomeCount(playerUUID);
    }
    
    /**
     * Get max homes for player based on permissions
     */
    public int getMaxHomes(ServerPlayer player) {
    MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
        
        // Check for unlimited homes permission
        if (PermissionUtil.hasPermission(player, "neoessentials.sethome.unlimited")) {
            return Integer.MAX_VALUE;
        }

        // Check for admin permission
        if (PermissionUtil.hasPermission(player, "neoessentials.sethome.admin")) {
            return config.maxHomesAdmin;
        }

        // Check for VIP permission
        if (PermissionUtil.hasPermission(player, "neoessentials.sethome.vip")) {
            return config.maxHomesVip;
        }

        return config.maxHomes;
    }
    
    /**
     * Check if player is on teleport cooldown
     */
    public boolean isOnCooldown(ServerPlayer player) {
    MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
        if (config.teleportHomeCooldown <= 0) {
            return false;
        }
        
        Long lastTeleport = teleportCooldowns.get(player.getUUID());
        if (lastTeleport == null) {
            return false;
        }
        
        long cooldownTime = config.teleportHomeCooldown * 1000L;
        return System.currentTimeMillis() - lastTeleport < cooldownTime;
    }
    
    /**
     * Get remaining cooldown time in milliseconds
     */
    public long getRemainingCooldown(ServerPlayer player) {
    MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
        Long lastTeleport = teleportCooldowns.get(player.getUUID());
        if (lastTeleport == null) {
            return 0;
        }
        
    long cooldownTime = config.teleportHomeCooldown * 1000L;
    long elapsed = System.currentTimeMillis() - lastTeleport;
    return Math.max(0, cooldownTime - elapsed);
    }
    
    /**
     * Start warmup process for teleportation
     */
    private void startWarmup(ServerPlayer player, LocationUtil.Location home, String homeName) {
    MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
        
        // Cancel existing warmup
        cancelWarmup(player);
        
    MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.home.warmup", String.valueOf(config.teleportWarmup)));
        
        // Store warmup task
        long warmupEnd = System.currentTimeMillis() + (config.teleportWarmup * 1000L);
        warmupTasks.put(player.getUUID(), warmupEnd);
        
        // Schedule teleport (this would need to be implemented with server's scheduler)
        // For now, this is a placeholder for the warmup system
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
    private boolean performTeleport(ServerPlayer player, LocationUtil.Location home, String homeName) {
    MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
        
        try {
            // Find the target world - simplified version
            var server = player.getServer();
            if (server == null) {
                return false;
            }
            ServerLevel targetWorld = server.getLevel(Level.OVERWORLD); // Placeholder - would need proper world lookup
            
            if (targetWorld == null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.home.target_world_not_found"));
                return false;
            }
            
            // Charge cost after successful teleport
    if (config.teleportHomeCost > 0) {
                EconomyManager economyManager = EconomyManager.getInstance();
                economyManager.withdrawBalance(player.getUUID(), config.teleportHomeCost, "Home teleport: " + homeName);
            }
            
            // Set cooldown
        teleportCooldowns.put(player.getUUID(), System.currentTimeMillis());
            
            // Perform teleport
            player.teleportTo(targetWorld, home.x, home.y, home.z, home.yaw, home.pitch);
            
    MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.home.teleporting_named", homeName));
            
            LOGGER.info("Player {} teleported to home '{}' at {}, {}, {} in {}", 
                player.getName().getString(), homeName, home.x, home.y, home.z, home.world);
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} to home '{}'", player.getName().getString(), homeName, e);
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.home.failed"));
            return false;
        }
    }
    
    /**
     * Validate home name
     */
    private boolean isValidHomeName(String homeName) {
        if (homeName == null || homeName.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = homeName.trim();
        
        // Basic validation - check length
        if (trimmed.length() < 1 || trimmed.length() > 16) {
            return false;
        }
        
        // Check allowed characters - only letters, numbers, and underscores
        return trimmed.matches("[a-zA-Z0-9_]+");
    }
    
    /**
     * Check if location is safe for teleportation
     */
    private boolean isLocationSafe(LocationUtil.Location location) {
        // Placeholder implementation - would implement proper safety checks
        return location.y > 0 && location.y < 256;
    }
    
    /**
     * Find a safe location near the given location
     */
    @SuppressWarnings("unused")
    private LocationUtil.Location findSafeLocation(LocationUtil.Location original) {
        // Placeholder implementation - would implement safe location finding
        return original;
    }
    
    /**
     * Clean up expired cooldowns and warmups
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up expired cooldowns
        teleportCooldowns.entrySet().removeIf(entry -> {
        MainConfig.HomeSettings config = configManager.getMainConfig().homeSettings;
            long cooldownTime = config.teleportHomeCooldown * 1000L;
            return currentTime - entry.getValue() > cooldownTime;
        });
        
        // Clean up expired warmups
        warmupTasks.entrySet().removeIf(entry -> currentTime > entry.getValue());
    }
    
    /**
     * Home location data is now handled by LocationUtil.Location
     * @see com.zerog.neoessentials.util.LocationUtil.Location
     */
}
