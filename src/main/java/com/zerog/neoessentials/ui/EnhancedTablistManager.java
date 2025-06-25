package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.DataManagerHooks;
import com.zerog.neoessentials.ui.tablist.TablistAnimationManager;
import com.zerog.neoessentials.ui.tablist.TablistGroupManager;
import com.zerog.neoessentials.ui.tablist.TablistPlaceholderManager;
import com.zerog.neoessentials.utils.PermissionUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced TablistManager that incorporates the new animation, placeholder, and grouping systems.
 * This class manages the tablist display for all online players.
 */
public class EnhancedTablistManager {

    // Configuration - using AtomicReference to safely update server reference
    private final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>(null);
    
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
    
    // Flag to track initialization status
    private boolean initialized = false;
    
    /**
     * Creates a new EnhancedTablistManager
     * 
     * @param scheduler The scheduler to use for tablist updates
     */
    public EnhancedTablistManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        
        // Initialize components
        this.animationManager = new TablistAnimationManager();
        this.placeholderManager = new TablistPlaceholderManager(null); // Will set server later
        this.groupManager = new TablistGroupManager();
        
        NeoEssentials.LOGGER.info("Enhanced TablistManager created (waiting for server)");
    }
    
    /**
     * Updates the server reference and initializes components that need the server
     * 
     * @param server The Minecraft server instance
     */
    public void setServer(MinecraftServer server) {
        if (server == null) {
            NeoEssentials.LOGGER.warn("Attempted to set null server in EnhancedTablistManager");
            return;
        }
        
        // Update the server reference
        this.serverRef.set(server);
        
        // Update components that need the server reference
        this.placeholderManager.setServer(server);
        
        NeoEssentials.LOGGER.info("EnhancedTablistManager server reference updated");
        
        // If not yet initialized, initialize now
        if (!initialized) {
            initialize();
        }
    }
    
    /**
     * Initializes the tablist manager and starts the update task
     */
    public void initialize() {
        if (initialized) {
            NeoEssentials.LOGGER.debug("EnhancedTablistManager already initialized, skipping");
            return;
        }
        
        NeoEssentials.LOGGER.info("Initializing enhanced tablist system");
        
        // Load headers and footers from config
        loadHeadersAndFooters();
        
        // Start the update task
        long updateInterval = TablistTomlConfig.UPDATE_INTERVAL.get();
        startUpdateTask(updateInterval);
        
        initialized = true;
    }
    
    /**
     * Loads header and footer templates from config
     */
    private void loadHeadersAndFooters() {
        // Clear existing headers and footers        headers.clear();
        footers.clear();
            // Get a TabManager instance if available, or use default templates
        TabManager tabManager = DataManagerHooks.getTabManager();
        if (tabManager != null && tabManager.getTemplateManager() != null) {
            // Load from TemplateManager
            List<String> configHeaders = tabManager.getTemplateManager().getGlobalHeaders();
            List<String> configFooters = tabManager.getTemplateManager().getGlobalFooters();
            
            // Add headers
            headers.addAll(configHeaders);
            
            // Add footers
            footers.addAll(configFooters);
        } else {
            // Fallback to default templates if TabManager not available
            headers.addAll(Arrays.asList(
                "&6&l✦ &b&lNeoEssentials Server &6&l✦",
                "&eWelcome, &a%player%&e!",
                "&eOnline players: &a%online%/%max%",
                "&eServer time: &a%time%"
            ));
            
            footers.addAll(Arrays.asList(
                "&eBalance: &a%balance% coins", 
                "&eWebsite: &awww.example.com", 
                "&eThanks for playing!", 
                "&eServer TPS: &a%tps% &7| &eMemory: &a%memory_percent%"
            ));
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
            // Get server from atomic reference
            MinecraftServer server = serverRef.get();
            
            // Check if server is initialized
            if (server == null) {
                // Server not yet available, skip this update cycle
                return;
            }
            
            // Update animation frames
            animationManager.updateAnimationFrames();
            
            // Get all online players (with additional null check)
            if (server.getPlayerList() == null) {
                return;
            }
            
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
            
            // Get the appropriate header and footer templates for this player
            List<String> playerHeaders = getPlayerSpecificHeaders(player, playerGroup);
            List<String> playerFooters = getPlayerSpecificFooters(player, playerGroup);
            
            // Get animated header and footer for this player
            Component playerHeader = animationManager.getAnimatedHeader(player, playerHeaders, placeholderManager);
            Component playerFooter = animationManager.getAnimatedFooter(player, playerFooters, placeholderManager);
            
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
     * Gets the header templates specific to a player based on their group, if enabled
     * 
     * @param player The player
     * @param group The player's group
     * @return The list of header templates to use
     */
    private List<String> getPlayerSpecificHeaders(ServerPlayer player, String group) {
        // Use default headers if player-specific headers are disabled
        if (!TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_HEADERS.get()) {
            return headers;
        }        // Get a TabManager instance to access templates
        TabManager tabManager = DataManagerHooks.getTabManager();
        if (tabManager != null && tabManager.getTemplateManager() != null) {        // Check for permission-based headers
            if (group.equalsIgnoreCase("Admin") && 
                PermissionUtil.hasPermission(player, "neoessentials.tablist.header.admin")) {
                // Get admin-specific headers from template manager
                List<String> adminHeaders = tabManager.getTemplateManager().getGroupHeaders("admin");
                if (adminHeaders != null && !adminHeaders.isEmpty()) {
                    return new ArrayList<>(adminHeaders);
                }
            } else if (group.equalsIgnoreCase("VIP") && 
                      PermissionUtil.hasPermission(player, "neoessentials.tablist.header.vip")) {
                // Get VIP-specific headers from template manager
                List<String> vipHeaders = tabManager.getTemplateManager().getGroupHeaders("vip");
                if (vipHeaders != null && !vipHeaders.isEmpty()) {
                    return new ArrayList<>(vipHeaders);
                }
            }
        }
        
        // Fall back to default headers
        return headers;
    }
    
    /**
     * Gets the footer templates specific to a player based on their group
     * 
     * @param player The player
     * @param group The player's group
     * @return The list of footer templates to use
     */
    private List<String> getPlayerSpecificFooters(ServerPlayer player, String group) {
        // Use default footers if player-specific footers are disabled
        if (!TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_FOOTERS.get()) {
            return footers;
        }        // Get a TabManager instance to access templates
        TabManager tabManager = DataManagerHooks.getTabManager();
        if (tabManager != null && tabManager.getTemplateManager() != null) {
            // Check for permission-based footers
            if (group.equalsIgnoreCase("Admin") && 
                PermissionUtil.hasPermission(player, "neoessentials.tablist.footer.admin")) {
                // Get admin-specific footers from template manager
                List<String> adminFooters = tabManager.getTemplateManager().getGroupFooters("admin");
                if (adminFooters != null && !adminFooters.isEmpty()) {
                    return new ArrayList<>(adminFooters);
                }
            } else if (group.equalsIgnoreCase("VIP") && 
                      PermissionUtil.hasPermission(player, "neoessentials.tablist.footer.vip")) {
                // Get VIP-specific footers from template manager
                List<String> vipFooters = tabManager.getTemplateManager().getGroupFooters("vip");
                if (vipFooters != null && !vipFooters.isEmpty()) {
                    return new ArrayList<>(vipFooters);
                }
            }
        }
        
        // Fall back to default footers
        return footers;
    }
    
    /**
     * Stops the tablist update task
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        
        NeoEssentials.LOGGER.info("Enhanced tablist system shutdown");
    }
    
    /**
     * Called when a player disconnects
     * 
     * @param player The player who disconnected
     */
    public void onPlayerDisconnect(ServerPlayer player) {
        // Remove player from animation manager
        animationManager.removePlayer(player.getUUID());
    }
    
    /**
     * Called when a player joins the server
     * 
     * @param player The player who joined
     */
    public void onPlayerJoin(ServerPlayer player) {
        // Nothing special needed on join, the player will be included in the next update cycle
        NeoEssentials.LOGGER.debug("Player joined: {}", player.getScoreboardName());
    }
    
    /**
     * Called when a player leaves the server
     * 
     * @param player The player who left
     */
    public void onPlayerLeave(ServerPlayer player) {
        // This is the same as onPlayerDisconnect
        onPlayerDisconnect(player);
    }
    
    /**
     * Reloads the tablist configuration and restarts the service
     * 
     * @param forceExtractConfig Whether to force extract the default config
     * @return True if successful, false otherwise
     */
    public boolean reload(boolean forceExtractConfig) {
        NeoEssentials.LOGGER.info("Reloading tablist configuration...");
        
        // Optionally force extract the config
        if (forceExtractConfig) {
            boolean extracted = com.zerog.neoessentials.utils.ResourceManager.forceExtractTablistConfig();
            if (!extracted) {
                NeoEssentials.LOGGER.warn("Failed to extract tablist config");
                return false;
            }
            
            // Give config system time to detect the change
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Reload configs
        TablistTomlConfig.reload();
        
        // Stop current task
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        
        // Reload headers and footers
        loadHeadersAndFooters();
        
        // Restart the update task
        long updateInterval = TablistTomlConfig.UPDATE_INTERVAL.get();
        startUpdateTask(updateInterval);
        
        NeoEssentials.LOGGER.info("Tablist reloaded successfully");
        return true;
    }
}
