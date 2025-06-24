package com.zerog.neoessentials.ui.tab;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import com.zerog.neoessentials.ui.tab.features.*;
import com.zerog.neoessentials.ui.tab.placeholders.PlaceholderManager;
import com.zerog.neoessentials.ui.tab.utils.ErrorLogger;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main TAB system manager for NeoEssentials
 * Inspired by the TAB plugin from Spigot but fully integrated for NeoForge
 * 
 * This class orchestrates all TAB features:
 * - Header/Footer
 * - BelowName
 * - Bossbar
 * - NameTags
 * - PlayerList formatting and sorting
 * - Scoreboard
 * - Layout customization
 * - Conditional placeholders
 * - RGB/font support
 * - Animation systems
 */
public class TabManager {
    // Core references
    private final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>(null);
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;
    private boolean initialized = false;
    
    // Configuration
    private int updateInterval = 2000; // Default 2 seconds
    
    // Feature managers
    private final HeaderFooterFeature headerFooterFeature;
    private final BelowNameFeature belowNameFeature;
    private final BossBarFeature bossBarFeature;
    private final NameTagsFeature nameTagsFeature;
    private final PlayerListFeature playerListFeature;
    private final ScoreboardFeature scoreboardFeature;
    private final LayoutFeature layoutFeature;
    
    // Support systems
    private final PlaceholderManager placeholderManager;
    private final AnimationManager animationManager;
    private final ErrorLogger errorLogger;
    
    // Player data storage
    private final Map<UUID, TabPlayerData> playerData = new ConcurrentHashMap<>();
    
    /**
     * Creates a new comprehensive TabManager
     * 
     * @param scheduler The scheduler to use for updates
     */
    public TabManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        
        // Initialize support systems first
        this.placeholderManager = new PlaceholderManager(this);
        this.animationManager = new AnimationManager();
        this.errorLogger = new ErrorLogger();
        
        // Initialize feature managers
        this.headerFooterFeature = new HeaderFooterFeature(this);
        this.belowNameFeature = new BelowNameFeature(this);
        this.bossBarFeature = new BossBarFeature(this);
        this.nameTagsFeature = new NameTagsFeature(this);
        this.playerListFeature = new PlayerListFeature(this);
        this.scoreboardFeature = new ScoreboardFeature(this);
        this.layoutFeature = new LayoutFeature(this);
        
        NeoEssentials.LOGGER.info("TabManager created - waiting for server initialization");
    }
    
    /**
     * Sets the server instance for this manager
     * 
     * @param server The Minecraft server instance
     */
    public void setServer(MinecraftServer server) {
        if (server == null) {
            NeoEssentials.LOGGER.warn("Attempted to set null server in TabManager");
            return;
        }
        
        serverRef.set(server);
        
        // Inform feature managers about server change
        headerFooterFeature.onServerChanged(server);
        belowNameFeature.onServerChanged(server);
        bossBarFeature.onServerChanged(server);
        nameTagsFeature.onServerChanged(server);
        playerListFeature.onServerChanged(server);
        scoreboardFeature.onServerChanged(server);
        layoutFeature.onServerChanged(server);
        
        NeoEssentials.LOGGER.info("TabManager server reference updated");
    }
    
    /**
     * Initializes the tablist system on server startup
     */
    public void initialize() {
        if (initialized) {
            NeoEssentials.LOGGER.debug("TabManager already initialized, skipping");
            return;
        }
        
        if (serverRef.get() == null) {
            NeoEssentials.LOGGER.warn("Cannot initialize TabManager without server reference");
            return;
        }
        
        // Load configuration
        loadConfig();
        
        // Initialize all systems
        animationManager.initialize();
        placeholderManager.initialize();
        
        // Initialize feature managers
        headerFooterFeature.initialize();
        belowNameFeature.initialize();
        bossBarFeature.initialize();
        nameTagsFeature.initialize();
        playerListFeature.initialize();
        scoreboardFeature.initialize();
        layoutFeature.initialize();
        
        // Start update task
        startUpdateTask();
        
        initialized = true;
        NeoEssentials.LOGGER.info("TabManager initialized");
    }
      /**
     * Loads configuration settings for the tablist
     */
    public void loadConfig() {
        // Load general settings
        updateInterval = TablistTomlConfig.UPDATE_INTERVAL.get().intValue(); // Convert Long to int
        
        // Each feature loads its own config
        headerFooterFeature.loadConfig();
        belowNameFeature.loadConfig();
        bossBarFeature.loadConfig();
        nameTagsFeature.loadConfig();
        playerListFeature.loadConfig();
        scoreboardFeature.loadConfig();
        layoutFeature.loadConfig();
        
        NeoEssentials.LOGGER.info("TabManager configuration loaded");
    }
    
    /**
     * Starts the periodic update task
     */
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        
        updateTask = scheduler.scheduleAtFixedRate(
            this::updateAll, 
            0, 
            updateInterval, 
            TimeUnit.MILLISECONDS
        );
        
        NeoEssentials.LOGGER.info("TabManager update task scheduled ({}ms interval)", updateInterval);
    }
    
    /**
     * Updates all tablist components for all players
     */
    private void updateAll() {
        try {
            MinecraftServer server = serverRef.get();
            if (server == null) return;
            
            // Update all feature managers
            headerFooterFeature.update();
            belowNameFeature.update();
            bossBarFeature.update();
            nameTagsFeature.update();
            playerListFeature.update();
            scoreboardFeature.update();
            layoutFeature.update();
        } catch (Exception e) {
            errorLogger.logError("Error updating TabManager", e);
        }
    }
    
    /**
     * Called when a player joins the server
     * 
     * @param player The joining player
     */
    public void onPlayerJoin(ServerPlayer player) {
        TabPlayerData data = new TabPlayerData(player);
        playerData.put(player.getUUID(), data);
        
        // Inform feature managers
        headerFooterFeature.onPlayerJoin(player);
        belowNameFeature.onPlayerJoin(player);
        bossBarFeature.onPlayerJoin(player);
        nameTagsFeature.onPlayerJoin(player);
        playerListFeature.onPlayerJoin(player);
        scoreboardFeature.onPlayerJoin(player);
        layoutFeature.onPlayerJoin(player);
    }
    
    /**
     * Called when a player leaves the server
     * 
     * @param player The leaving player
     */
    public void onPlayerLeave(ServerPlayer player) {
        playerData.remove(player.getUUID());
        
        // Inform feature managers
        headerFooterFeature.onPlayerLeave(player);
        belowNameFeature.onPlayerLeave(player);
        bossBarFeature.onPlayerLeave(player);
        nameTagsFeature.onPlayerLeave(player);
        playerListFeature.onPlayerLeave(player);
        scoreboardFeature.onPlayerLeave(player);
        layoutFeature.onPlayerLeave(player);
    }
    
    /**
     * Called when a player changes worlds
     * 
     * @param player The player
     * @param worldName The new world name
     */
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        TabPlayerData data = playerData.get(player.getUUID());
        if (data != null) {
            data.setWorld(worldName);
        }
        
        // Inform feature managers
        headerFooterFeature.onPlayerChangeWorld(player, worldName);
        belowNameFeature.onPlayerChangeWorld(player, worldName);
        bossBarFeature.onPlayerChangeWorld(player, worldName);
        nameTagsFeature.onPlayerChangeWorld(player, worldName);
        playerListFeature.onPlayerChangeWorld(player, worldName);
        scoreboardFeature.onPlayerChangeWorld(player, worldName);
        layoutFeature.onPlayerChangeWorld(player, worldName);
    }
    
    /**
     * Gets the player data for all tracked players
     * 
     * @return Map of player UUIDs to their tab data
     */
    public Map<UUID, TabPlayerData> getPlayerData() {
        return playerData;
    }
    
    /**
     * Gets the player data for a specific player
     * 
     * @param player The player
     * @return The player's tab data, or null if not found
     */
    public TabPlayerData getPlayerData(ServerPlayer player) {
        return playerData.get(player.getUUID());
    }
    
    /**
     * Gets the placeholder manager
     */
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }
    
    /**
     * Gets the animation manager
     */
    public AnimationManager getAnimationManager() {
        return animationManager;
    }
    
    /**
     * Gets the error logger
     */
    public ErrorLogger getErrorLogger() {
        return errorLogger;
    }
    
    /**
     * Gets the server instance
     */
    public MinecraftServer getServer() {
        return serverRef.get();
    }
    
    /**
     * Gets all online players
     */
    public List<ServerPlayer> getOnlinePlayers() {
        MinecraftServer server = serverRef.get();
        if (server == null) return Collections.emptyList();
        
        return new ArrayList<>(server.getPlayerList().getPlayers());
    }
    
    /**
     * Gets the header/footer feature manager
     */
    public HeaderFooterFeature getHeaderFooterFeature() {
        return headerFooterFeature;
    }
    
    /**
     * Gets the below name feature manager
     */
    public BelowNameFeature getBelowNameFeature() {
        return belowNameFeature;
    }
    
    /**
     * Gets the boss bar feature manager
     */
    public BossBarFeature getBossBarFeature() {
        return bossBarFeature;
    }
    
    /**
     * Gets the nametags feature manager
     */
    public NameTagsFeature getNameTagsFeature() {
        return nameTagsFeature;
    }
    
    /**
     * Gets the player list feature manager
     */
    public PlayerListFeature getPlayerListFeature() {
        return playerListFeature;
    }
    
    /**
     * Gets the scoreboard feature manager
     */
    public ScoreboardFeature getScoreboardFeature() {
        return scoreboardFeature;
    }
    
    /**
     * Gets the layout feature manager
     */
    public LayoutFeature getLayoutFeature() {
        return layoutFeature;
    }
}
