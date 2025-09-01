package com.zerog.neoessentials.api;

import com.zerog.neoessentials.api.interfaces.*;
import com.zerog.neoessentials.events.NeoEssentialsEventHandler;
import com.zerog.neoessentials.managers.*;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import com.zerog.neoessentials.utils.PerformanceMonitor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/**
 * Main API interface for NeoEssentials
 * Provides comprehensive access to all mod features for integration with other mods
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NeoEssentialsAPI {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsAPI.class);
    private static NeoEssentialsAPI instance;
    
    private final HomeManager homeManager;
    private final EconomyManager economyManager;
    private final WarpManager warpManager;
    private final KitManager kitManager;
    private final MessagingManager messagingManager;
    private final SpawnManager spawnManager;
    private final ModerationManager moderationManager;
    private final com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager;
    private final PerformanceMonitor performanceMonitor;
    
    // Enhanced API components
    private final NeoEssentialsEventHandler eventHandler;
    private boolean eventSystemInitialized = false;
    
    private NeoEssentialsAPI() {
        this.homeManager = HomeManager.getInstance();
        this.economyManager = EconomyManager.getInstance();
        this.warpManager = WarpManager.getInstance();
        this.kitManager = KitManager.getInstance();
        this.messagingManager = MessagingManager.getInstance();
        this.spawnManager = SpawnManager.getInstance();
        this.moderationManager = ModerationManager.getInstance();
        this.placeholderManager = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
        this.performanceMonitor = PerformanceMonitor.getInstance();
        
        // Initialize enhanced API components
        this.eventHandler = new NeoEssentialsEventHandler();
        
        // Initialize API factory
        NeoEssentialsAPIFactory.initialize();
        
        LOGGER.info("NeoEssentials API initialized with enhanced features");
    }
    
    /**
     * Get the API instance
     */
    public static NeoEssentialsAPI getInstance() {
        if (instance == null) {
            instance = new NeoEssentialsAPI();
        }
        return instance;
    }
    
    /**
     * Initialize the event system
     * Should be called during mod initialization
     */
    public void initializeEventSystem() {
        if (!eventSystemInitialized) {
            NeoForge.EVENT_BUS.register(eventHandler);
            eventSystemInitialized = true;
            LOGGER.info("NeoEssentials event system initialized");
        }
    }
    
    /**
     * Get the event handler for firing custom events
     * @return Event handler instance
     */
    public NeoEssentialsEventHandler getEventHandler() {
        return eventHandler;
    }
    
    /**
     * Check if NeoEssentials API is available
     */
    public static boolean isAvailable() {
        try {
            return instance != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get API version (static method)
     */
    public static String getAPIVersion() {
        return "2.1.0";
    }
    
    /**
     * Get mod version (static method)
     */
    public static String getModVersion() {
        return "1.0.2";
    }
    
    /**
     * Check if API version is at least the specified version
     */
    public static boolean isAPIVersionAtLeast(String minVersion) {
        String current = getAPIVersion();
        return compareVersions(current, minVersion) >= 0;
    }
    
    /**
     * Compare two version strings
     */
    private static int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1Part != v2Part) {
                return Integer.compare(v1Part, v2Part);
            }
        }
        
        return 0;
    }
    
    // ========================= HOME API =========================
    
    /**
     * Set a home for a player
     */
    public boolean setPlayerHome(ServerPlayer player, String homeName) {
        try {
            return homeManager.setHome(player, homeName);
        } catch (Exception e) {
            LOGGER.error("Error setting home for player {}: {}", player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Get player's homes
     */
    public List<String> getPlayerHomes(UUID playerUuid) {
        return homeManager.getPlayerHomes(playerUuid);
    }
    
    /**
     * Teleport player to home
     */
    public boolean teleportToHome(ServerPlayer player, String homeName) {
        try {
            return homeManager.teleportToHome(player, homeName);
        } catch (Exception e) {
            LOGGER.error("Error teleporting player {} to home {}: {}", player.getName().getString(), homeName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete a player's home
     */
    public boolean deletePlayerHome(ServerPlayer player, String homeName) {
        return homeManager.deleteHome(player, homeName);
    }
    
    // ========================= ECONOMY API =========================
    
    /**
     * Get player's balance
     */
    public BigDecimal getPlayerBalance(UUID playerUuid) {
        return economyManager.getBalance(playerUuid);
    }
    
    /**
     * Set player's balance
     */
    public void setPlayerBalance(UUID playerUuid, BigDecimal amount) {
        economyManager.setBalance(playerUuid, amount);
    }
    
    /**
     * Check if player has sufficient balance
     */
    public boolean hasBalance(UUID playerUuid, BigDecimal amount) {
        return economyManager.hasBalance(playerUuid, amount);
    }
    
    /**
     * Withdraw money from player's balance
     */
    public boolean withdrawFromBalance(UUID playerUuid, BigDecimal amount, String reason) {
        return economyManager.withdrawBalance(playerUuid, amount, reason);
    }
    
    /**
     * Deposit money to player's balance
     */
    public void depositToBalance(UUID playerUuid, BigDecimal amount, String reason) {
        economyManager.depositBalance(playerUuid, amount, reason);
    }
    
    /**
     * Format currency amount
     */
    public String formatCurrency(BigDecimal amount) {
        return economyManager.formatCurrency(amount);
    }
    
    // ========================= WARP API =========================
    
    /**
     * Get all available warps
     */
    public Collection<WarpManager.WarpData> getAllWarps() {
        return warpManager.getAllWarps();
    }
    
    /**
     * Teleport player to warp
     */
    public boolean teleportToWarp(ServerPlayer player, String warpName) {
        try {
            return warpManager.teleportToWarp(player, warpName);
        } catch (Exception e) {
            LOGGER.error("Error teleporting player {} to warp {}: {}", player.getName().getString(), warpName, e.getMessage());
            return false;
        }
    }
    
    // ========================= KIT API =========================
    
    /**
     * Give kit to player
     */
    public boolean giveKit(ServerPlayer player, String kitName) {
        try {
            return kitManager.giveKit(player, kitName);
        } catch (Exception e) {
            LOGGER.error("Error giving kit {} to player {}: {}", kitName, player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    // ========================= MESSAGING API =========================
    
    /**
     * Send private message to player
     */
    public boolean sendPrivateMessage(ServerPlayer sender, String recipientName, String message) {
        return messagingManager.sendPrivateMessage(sender, recipientName, message);
    }
    
    /**
     * Send mail to player
     */
    public boolean sendMail(ServerPlayer sender, String recipientName, String message) {
        return messagingManager.sendMail(sender, recipientName, message);
    }
    
    // ========================= SPAWN API =========================
    
    /**
     * Teleport player to spawn
     */
    public boolean teleportToSpawn(ServerPlayer player) {
        try {
            return spawnManager.teleportToSpawn(player);
        } catch (Exception e) {
            LOGGER.error("Error teleporting player {} to spawn: {}", player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Set spawn location
     */
    public boolean setSpawn(ServerPlayer player) {
        return spawnManager.setSpawn(player);
    }
    
    // ========================= PLACEHOLDER API =========================
    
    /**
     * Register a custom placeholder
     */
    public void registerPlaceholder(String identifier, Function<com.zerog.neoessentials.placeholders.PlaceholderManager.PlaceholderContext, String> function) {
        placeholderManager.registerPlaceholder(identifier, function);
        LOGGER.info("Registered custom placeholder: {}", identifier);
    }
    
    /**
     * Process placeholders in text
     */
    public String processPlaceholders(ServerPlayer player, String text) {
    return placeholderManager.processPlaceholders(text, player);
    }
    
    /**
     * Get all registered placeholders
     */
    public java.util.Set<String> getRegisteredPlaceholders() {
        return placeholderManager.getRegisteredPlaceholders();
    }
    
    // ========================= PERFORMANCE API =========================
    
    /**
     * Get performance metrics for a command
     */
    public PerformanceMonitor.CommandMetrics getCommandMetrics(String commandName) {
        return performanceMonitor.getCommandMetrics(commandName);
    }
    
    /**
     * Get system performance metrics
     */
    public PerformanceMonitor.SystemMetrics getSystemMetrics() {
        return performanceMonitor.getSystemMetrics();
    }
    
    /**
     * Generate performance report
     */
    public PerformanceMonitor.PerformanceReport generatePerformanceReport() {
        return performanceMonitor.generateReport();
    }
    
    // ========================= UTILITY API =========================
    
    /**
     * Check if location is safe for teleportation
     */
    public boolean isSafeLocation(LocationUtil.Location location) {
        return LocationUtil.isSafeLocation(location);
    }
    
    /**
     * Find a safe location near the given location
     */
    public LocationUtil.Location findSafeLocation(LocationUtil.Location location) {
        return LocationUtil.findSafeLocation(location);
    }
    
    /**
     * Create location from player
     */
    public LocationUtil.Location createLocationFromPlayer(ServerPlayer player) {
        return new LocationUtil.Location(
            player.level().dimension().location().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            System.currentTimeMillis()
        );
    }
    
    // ========================= VERSION INFO =========================
    
    /**
     * Check if specific feature is available
     */
    public boolean isFeatureAvailable(String featureName) {
        return switch (featureName.toLowerCase()) {
            case "homes", "home" -> true;
            case "economy", "money" -> true;
            case "warps", "warp" -> true;
            case "kits", "kit" -> true;
            case "messaging", "mail" -> true;
            case "spawn" -> true;
            case "moderation", "jail", "mute" -> true;
            case "placeholders" -> true;
            case "performance" -> true;
            default -> false;
        };
    }
    
    // ========================= MANAGER ACCESS =========================
    
    /**
     * Get direct access to managers (for advanced integrations)
     */
    public HomeManager getHomeManager() { return homeManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public WarpManager getWarpManager() { return warpManager; }
    public KitManager getKitManager() { return kitManager; }
    public MessagingManager getMessagingManager() { return messagingManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public ModerationManager getModerationManager() { return moderationManager; }
    public PlaceholderManager getPlaceholderManager() { return placeholderManager; }
    public PerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
}
