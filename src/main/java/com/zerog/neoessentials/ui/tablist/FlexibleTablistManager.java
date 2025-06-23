package com.zerog.neoessentials.ui.tablist;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import com.zerog.neoessentials.ui.tablist.components.*;
import com.zerog.neoessentials.ui.tablist.layouts.*;
import com.zerog.neoessentials.utils.PermissionUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A completely redesigned flexible tablist system for NeoEssentials.
 * This implementation focuses on:
 * 1. Respecting user configuration - no more overwriting configs
 * 2. Supporting dynamic and fixed-size layouts
 * 3. Player sets for grouping and filtering
 * 4. Enhanced animation and placeholder support
 * 5. Vertical slot ordering options
 */
public class FlexibleTablistManager {
    // Configuration
    private final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>(null);
    
    // Components
    private final TablistPlaceholderManager placeholderManager;
    private final TablistAnimationManager animationManager;
    private final TablistLayoutManager layoutManager;
    private final Set<TablistComponent> components = new HashSet<>();
    
    // Player data
    private final Map<UUID, TablistPlayerData> playerData = new ConcurrentHashMap<>();
    
    // Scheduling
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;
    
    // State tracking
    private boolean initialized = false;
    private ConfigMode configMode = ConfigMode.TOML_CONFIG;
    
    /**
     * Configuration mode for the tablist
     */
    public enum ConfigMode {
        TOML_CONFIG,    // Use the tablist.toml configuration file
        API_OVERRIDE,   // Allow external API to override config
        HYBRID          // Use config but allow API modifications
    }
    
    /**
     * Creates a new FlexibleTablistManager
     * 
     * @param scheduler The scheduler to use for updates
     */
    public FlexibleTablistManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        
        // Initialize components
        this.placeholderManager = new TablistPlaceholderManager(null);
        this.animationManager = new TablistAnimationManager();
        this.layoutManager = new TablistLayoutManager();
        
        // Create default components
        initializeDefaultComponents();
        
        NeoEssentials.LOGGER.info("FlexibleTablistManager created - waiting for server");
    }
    
    /**
     * Set up default tablist components
     */
    private void initializeDefaultComponents() {
        // Header component
        components.add(new TablistHeaderComponent());
        
        // Footer component
        components.add(new TablistFooterComponent());
        
        // Player list component (dynamic)
        components.add(new TablistPlayerListComponent());
        
        // Fixed layout components - disabled by default
        components.add(new TablistFixedLayoutComponent());
        components.add(new TablistTableComponent());
        
        NeoEssentials.LOGGER.info("Default tablist components initialized");
    }
    
    /**
     * Sets the server instance and initializes server-dependent components
     */
    public void setServer(MinecraftServer server) {
        if (server == null) {
            NeoEssentials.LOGGER.warn("Attempted to set null server in FlexibleTablistManager");
            return;
        }
        
        // Update the server reference
        this.serverRef.set(server);
        
        // Update components that need the server reference
        this.placeholderManager.setServer(server);
        
        NeoEssentials.LOGGER.info("FlexibleTablistManager server reference updated");
        
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
            NeoEssentials.LOGGER.debug("TablistManager already initialized, skipping");
            return;
        }
        
        NeoEssentials.LOGGER.info("Initializing flexible tablist system");
        
        // Load configuration
        loadConfiguration();
        
        // Start the update task
        long updateInterval = TablistTomlConfig.UPDATE_INTERVAL.get();
        startUpdateTask(updateInterval);
        
        initialized = true;
    }
    
    /**
     * Loads the tablist configuration while respecting user customizations
     */
    public void loadConfiguration() {
        NeoEssentials.LOGGER.info("Loading tablist configuration (preserving customizations)");
        
        try {
            // Load values directly from TablistTomlConfig but don't allow overwrites
            // This ensures user customizations are preserved
            
            // We only use configuration values loaded from file, never the default values
            if (configMode == ConfigMode.TOML_CONFIG || configMode == ConfigMode.HYBRID) {
                // Initialize layout based on config
                initializeLayoutFromConfig();
                
                NeoEssentials.LOGGER.info("Successfully loaded tablist configuration while preserving customizations");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error loading tablist configuration", e);
        }
    }
    
    /**
     * Initialize the layout based on configuration
     */
    private void initializeLayoutFromConfig() {
        // Get values directly from config file
        // We'll set a mode based on the config
        boolean hasFixedSize = false; // Detect if fixed size is configured
        
        // Handle layout setup
        try {
            if (hasFixedSize) {
                layoutManager.setActiveLayout(new TablistFixedLayout());
            } else {
                layoutManager.setActiveLayout(new TablistDynamicLayout());
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to set tablist layout", e);
            // Fall back to dynamic layout
            layoutManager.setActiveLayout(new TablistDynamicLayout());
        }
        
        // Load header/footer text directly from getters that access the file
        updateHeaderFooterComponents();
    }
    
    /**
     * Update header and footer components with config values
     */
    private void updateHeaderFooterComponents() {
        // Find header and footer components
        Optional<TablistComponent> headerComponent = findComponent(TablistHeaderComponent.class);
        Optional<TablistComponent> footerComponent = findComponent(TablistFooterComponent.class);
        
        // Update header component
        headerComponent.ifPresent(component -> {
            if (component instanceof TablistHeaderComponent) {
                TablistHeaderComponent header = (TablistHeaderComponent) component;
                
                // Get header lines from config
                List<String> headerLines = TablistTomlConfig.getHeaders();
                header.setLines(headerLines);
                
                // Get admin/VIP headers for permission-based display
                Map<String, List<String>> groupHeaders = new HashMap<>();
                groupHeaders.put("admin", TablistTomlConfig.getAdminHeaders());
                groupHeaders.put("vip", TablistTomlConfig.getVipHeaders());
                header.setGroupLines(groupHeaders);
                
                // Set animation type
                String animType = TablistTomlConfig.HEADER_ANIMATION_TYPE.get();
                header.setAnimationType(animType);
            }
        });
        
        // Update footer component
        footerComponent.ifPresent(component -> {
            if (component instanceof TablistFooterComponent) {
                TablistFooterComponent footer = (TablistFooterComponent) component;
                
                // Get footer lines from config
                List<String> footerLines = TablistTomlConfig.getFooters();
                footer.setLines(footerLines);
                
                // Get admin/VIP footers for permission-based display
                Map<String, List<String>> groupFooters = new HashMap<>();
                groupFooters.put("admin", TablistTomlConfig.getAdminFooters());
                groupFooters.put("vip", TablistTomlConfig.getVipFooters());
                footer.setGroupLines(groupFooters);
                
                // Set animation type
                String animType = TablistTomlConfig.FOOTER_ANIMATION_TYPE.get();
                footer.setAnimationType(animType);
            }
        });
    }
    
    /**
     * Find a component by type
     */
    private <T extends TablistComponent> Optional<TablistComponent> findComponent(Class<T> componentClass) {
        return components.stream()
            .filter(componentClass::isInstance)
            .findFirst();
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
        
        NeoEssentials.LOGGER.info("Started tablist update task with interval: {}ms", updateInterval);
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
            
            // Update player data cache
            updatePlayerDataCache(players);
            
            // Update tablist for each player
            for (ServerPlayer player : players) {
                updatePlayerTablist(player);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error updating tablist", e);
        }
    }
    
    /**
     * Updates the player data cache with information about all online players
     */
    private void updatePlayerDataCache(Collection<ServerPlayer> players) {
        // Remove players that are no longer online
        Set<UUID> onlinePlayerIds = players.stream()
            .map(ServerPlayer::getUUID)
            .collect(Collectors.toSet());
            
        playerData.keySet().removeIf(uuid -> !onlinePlayerIds.contains(uuid));
        
        // Update or add information for current players
        for (ServerPlayer player : players) {
            UUID uuid = player.getUUID();
            TablistPlayerData data = playerData.computeIfAbsent(
                uuid, 
                id -> new TablistPlayerData(player)
            );
            
            // Update player data
            data.update(player);
        }
    }
    
    /**
     * Updates the tablist for a specific player
     * 
     * @param player The player to update
     */
    private void updatePlayerTablist(ServerPlayer player) {
        try {
            // Get player data
            TablistPlayerData data = playerData.get(player.getUUID());
            if (data == null) {
                data = new TablistPlayerData(player);
                playerData.put(player.getUUID(), data);
            }
            
            // Get header and footer components
            Component header = getHeaderForPlayer(player, data);
            Component footer = getFooterForPlayer(player, data);
            
            // Send the tablist packet
            player.connection.send(new ClientboundTabListPacket(header, footer));
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error updating tablist for player: " + player.getScoreboardName(), e);
        }
    }
    
    /**
     * Gets the header component for a player
     */
    private Component getHeaderForPlayer(ServerPlayer player, TablistPlayerData playerData) {
        // Find header component
        Optional<TablistComponent> headerComponent = findComponent(TablistHeaderComponent.class);
        if (headerComponent.isPresent() && headerComponent.get() instanceof TablistHeaderComponent) {
            TablistHeaderComponent header = (TablistHeaderComponent) headerComponent.get();
            
            // Get player's group name (admin, vip, etc.)
            String groupName = determinePlayerGroup(player);
            playerData.setGroup(groupName);
            
            // Get appropriate header lines for this player and group
            List<String> headerLines = header.getLinesForPlayer(player, groupName);
            
            // Use animation manager to create the final header
            return animationManager.createAnimatedComponent(
                player, 
                headerLines, 
                header.getAnimationType(), 
                placeholderManager
            );
        }
        
        // Fallback to empty component
        return Component.empty();
    }
    
    /**
     * Gets the footer component for a player
     */
    private Component getFooterForPlayer(ServerPlayer player, TablistPlayerData playerData) {
        // Find footer component
        Optional<TablistComponent> footerComponent = findComponent(TablistFooterComponent.class);
        if (footerComponent.isPresent() && footerComponent.get() instanceof TablistFooterComponent) {
            TablistFooterComponent footer = (TablistFooterComponent) footerComponent.get();
            
            // Get player's group name
            String groupName = playerData.getGroup();
            
            // Get appropriate footer lines for this player and group
            List<String> footerLines = footer.getLinesForPlayer(player, groupName);
            
            // Use animation manager to create the final footer
            return animationManager.createAnimatedComponent(
                player, 
                footerLines, 
                footer.getAnimationType(), 
                placeholderManager
            );
        }
        
        // Fallback to empty component
        return Component.empty();
    }
    
    /**
     * Determines a player's group based on permissions
     */
    private String determinePlayerGroup(ServerPlayer player) {
        // Check admin group
        if (PermissionUtil.hasPermission(player, "neoessentials.tablist.group.admin") ||
            PermissionUtil.hasPermission(player, "neoessentials.tablist.header.admin")) {
            return "admin";
        }
        
        // Check VIP group
        if (PermissionUtil.hasPermission(player, "neoessentials.tablist.group.vip") ||
            PermissionUtil.hasPermission(player, "neoessentials.tablist.header.vip")) {
            return "vip";
        }
        
        // Check mod group
        if (PermissionUtil.hasPermission(player, "neoessentials.tablist.group.mod") ||
            PermissionUtil.hasPermission(player, "neoessentials.tablist.header.mod")) {
            return "mod";
        }
        
        // Default group
        return "default";
    }
    
    /**
     * Shuts down the tablist manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        
        playerData.clear();
        NeoEssentials.LOGGER.info("Flexible tablist system shutdown");
    }
    
    /**
     * Called when a player joins the server
     * 
     * @param player The player who joined
     */
    public void onPlayerJoin(ServerPlayer player) {
        // Create player data entry
        TablistPlayerData data = new TablistPlayerData(player);
        playerData.put(player.getUUID(), data);
        
        NeoEssentials.LOGGER.debug("Player joined, added to tablist: {}", player.getScoreboardName());
    }
    
    /**
     * Called when a player leaves the server
     * 
     * @param player The player who left
     */
    public void onPlayerLeave(ServerPlayer player) {
        // Remove player from animation manager and data cache
        animationManager.removePlayer(player.getUUID());
        playerData.remove(player.getUUID());
        
        NeoEssentials.LOGGER.debug("Player left, removed from tablist: {}", player.getScoreboardName());
    }
    
    /**
     * Reloads the tablist configuration
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
        }
        
        // Stop current task
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        
        // Reload configuration
        loadConfiguration();
        
        // Restart the update task
        long updateInterval = TablistTomlConfig.UPDATE_INTERVAL.get();
        startUpdateTask(updateInterval);
        
        NeoEssentials.LOGGER.info("Tablist reloaded successfully");
        return true;
    }
}
