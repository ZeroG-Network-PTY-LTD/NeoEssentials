package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import com.zerog.neoessentials.ui.tablist.TablistAnimationManager;
import com.zerog.neoessentials.ui.tablist.TablistGroupManager;
import com.zerog.neoessentials.ui.tablist.TablistPlaceholderManager;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced TablistManager that incorporates the new animation, placeholder, and grouping systems.
 * This class manages the tablist display for all online players.
 */
public class EnhancedTablistManager {

    // Configuration
    private final MinecraftServer server;
    
    // Components for tablist functionality
    private final TablistAnimationManager animationManager;
    private final TablistPlaceholderManager placeholderManager;
    private final TablistGroupManager groupManager;
    
    // Scheduler for updating the tablist
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;
    
    // Cache for header and footer templates
    private final List<String> headers = new ArrayList<>();
    private final List<String> footers = new ArrayList<>();
    
    /**
     * Creates a new EnhancedTablistManager
     * 
     * @param server The Minecraft server instance
     * @param scheduler The scheduler to use for tablist updates
     */
    public EnhancedTablistManager(MinecraftServer server, ScheduledExecutorService scheduler) {
        this.server = server;
        this.scheduler = scheduler;
        
        // Initialize components
        this.animationManager = new TablistAnimationManager();
        this.placeholderManager = new TablistPlaceholderManager(server);
        this.groupManager = new TablistGroupManager();
        
        NeoEssentials.LOGGER.info("Enhanced TablistManager initialized");
    }
    
    /**
     * Initializes the tablist manager and starts the update task
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Starting enhanced tablist system");
        
        // Load headers and footers from config
        loadHeadersAndFooters();
        
        // Start the update task
        long updateInterval = TablistTomlConfig.UPDATE_INTERVAL.get();
        startUpdateTask(updateInterval);
    }
    
    /**
     * Loads header and footer templates from config
     */
    private void loadHeadersAndFooters() {
        // Clear existing headers and footers
        headers.clear();
        footers.clear();
        
        // Load from TOML config
        List<?> configHeaders = TablistTomlConfig.HEADERS.get();
        List<?> configFooters = TablistTomlConfig.FOOTERS.get();
        
        // Convert and add headers
        for (Object header : configHeaders) {
            if (header instanceof String) {
                headers.add((String) header);
            }
        }
        
        // Convert and add footers
        for (Object footer : configFooters) {
            if (footer instanceof String) {
                footers.add((String) footer);
            }
        }
        
        // Log loaded templates
        NeoEssentials.LOGGER.info("Loaded {} header templates and {} footer templates", 
            headers.size(), footers.size());
    }
    
    /**
     * Starts the scheduled task to update the tablist
     * 
     * @param updateInterval The interval in milliseconds
     */
    private void startUpdateTask(long updateInterval) {
        // Cancel existing task if any
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        
        // Start new task
        updateTask = scheduler.scheduleAtFixedRate(
            this::updateTablist,
            0,
            updateInterval,
            TimeUnit.MILLISECONDS
        );
        
        NeoEssentials.LOGGER.info("Started enhanced tablist update task with interval: {}ms", updateInterval);
    }
    
    /**
     * Updates the tablist for all online players
     */
    public void updateTablist() {
        try {
            // Update animation frames
            animationManager.updateAnimationFrames();
            
            // Get all online players
            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                return;
            }
            
            // Update tablist for each player
            for (ServerPlayer player : players) {
                updatePlayerTablist(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error updating tablist", e);
        }
    }
      /**
     * Updates the tablist for a specific player
     * 
     * @param player The player to update
     */
    private void updatePlayerTablist(ServerPlayer player) {
        try {
            // Get player group information
            String playerGroup = groupManager.getPlayerGroup(player);
            
            // Get animated header and footer for this player
            Component playerHeader = animationManager.getAnimatedHeader(player, headers, placeholderManager);
            Component playerFooter = animationManager.getAnimatedFooter(player, footers, placeholderManager);
            
            // Apply group-specific formatting if needed
            // This is where you could customize headers/footers based on player group
            
            // Log detailed info at debug level
            NeoEssentials.LOGGER.debug("Updating tablist for player {} in group {}", 
                player.getScoreboardName(), playerGroup);
            
            // Send the tablist packet
            player.connection.send(new ClientboundTabListPacket(playerHeader, playerFooter));
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error sending tablist packet to player " + player.getScoreboardName(), e);
        }
    }
    
    /**
     * Handles a player joining the server
     * 
     * @param player The player who joined
     */
    public void onPlayerJoin(ServerPlayer player) {
        // Update the tablist for the player immediately
        updatePlayerTablist(player);
    }
    
    /**
     * Handles a player leaving the server
     * 
     * @param playerId The UUID of the player who left
     */
    public void onPlayerLeave(UUID playerId) {
        // Clean up any player-specific data
        animationManager.removePlayer(playerId);
    }
    
    /**
     * Reloads the tablist configuration
     */
    public void reload() {
        NeoEssentials.LOGGER.info("Reloading tablist configuration");
        
        // Reload configuration
        loadHeadersAndFooters();
        
        // Restart update task with potentially new interval
        long updateInterval = TablistTomlConfig.UPDATE_INTERVAL.get();
        startUpdateTask(updateInterval);
    }
    
    /**
     * Stops the tablist manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
            NeoEssentials.LOGGER.info("Stopped tablist updates");
        }
    }
}
