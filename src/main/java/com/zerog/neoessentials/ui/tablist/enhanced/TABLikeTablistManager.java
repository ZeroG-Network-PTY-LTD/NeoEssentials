package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tablist.TablistAnimationManager;
import com.zerog.neoessentials.ui.tablist.TablistPlaceholderManager;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced TAB-like tablist manager with comprehensive features similar to the TAB plugin
 * 
 * Features implemented:
 * - Header & Footer with animations
 * - Player sorting (by groups, name, custom)
 * - Tablist name formatting with prefixes/suffixes 
 * - Playerlist objective (shows values next to names)
 * - Belowname objective (shows values below nameplates)
 * - Conditional displays based on permissions/worlds
 * - Layout system (dynamic/fixed)
 * - Boss bar integration
 * - Per-world and per-server configurations
 * - Spectator effect prevention
 * - Collision settings
 * - RGB color support
 * - Placeholder refresh intervals
 */
public class TABLikeTablistManager {
    
    private final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>(null);
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;
    
    // Core managers
    private final TablistAnimationManager animationManager;
    private final TablistPlaceholderManager placeholderManager;
    private final TABConfigManager configManager;
    private final TeamManager teamManager;
    private final ObjectiveManager objectiveManager;
    private final BossBarManager bossBarManager;
    
    // Configuration
    private TABConfig config;
    private boolean initialized = false;
    
    // Player data tracking
    private final Map<UUID, PlayerTabData> playerData = new ConcurrentHashMap<>();
    
    public TABLikeTablistManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        this.animationManager = new TablistAnimationManager();
        this.placeholderManager = new TablistPlaceholderManager(null);
        this.configManager = new TABConfigManager();
        this.teamManager = new TeamManager();
        this.objectiveManager = new ObjectiveManager();
        this.bossBarManager = new BossBarManager();
        
        NeoEssentials.LOGGER.info("TAB-like tablist manager created");
    }
    
    public void setServer(MinecraftServer server) {
        if (server == null) return;
        
        this.serverRef.set(server);
        this.placeholderManager.setServer(server);
        this.teamManager.setServer(server);
        this.objectiveManager.setServer(server);
        this.bossBarManager.setServer(server);
        
        if (!initialized) {
            initialize();
        }
    }
    
    public void initialize() {
        if (initialized) return;
        
        NeoEssentials.LOGGER.info("Initializing TAB-like tablist system");
        
        // Load configuration
        this.config = configManager.loadConfig();
        
        // Wire animation manager with placeholder manager
        placeholderManager.setAnimationManager(animationManager);
        
        // Load animations from config if available
        if (config.getAnimationsData() != null && !config.getAnimationsData().isEmpty()) {
            animationManager.loadAnimationsFromConfig(config.getAnimationsData());
            NeoEssentials.LOGGER.info("Loaded animations from YAML configuration");
        }
        
        // Initialize components
        teamManager.initialize(config);
        objectiveManager.initialize(config);
        bossBarManager.initialize(config);
        
        // Start update task
        startUpdateTask();
        
        initialized = true;
        NeoEssentials.LOGGER.info("TAB-like tablist system initialized");
    }
    
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        
        updateTask = scheduler.scheduleAtFixedRate(
            this::updateTablist,
            0,
            config.getUpdateInterval(),
            TimeUnit.MILLISECONDS
        );
    }
    
    public void updateTablist() {
        MinecraftServer server = serverRef.get();
        if (server == null || server.getPlayerList() == null) return;
        
        try {
            // Update animations
            animationManager.updateAnimationFrames();
            
            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            
            // Update player data
            updatePlayerData(players);
            
            // Update each component
            updateHeaderFooter(players);
            updatePlayerNames(players);
            updateObjectives(players);
            updateBossBars(players);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error updating TAB-like tablist", e);
        }
    }
    
    private void updatePlayerData(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            UUID uuid = player.getUUID();
            PlayerTabData data = playerData.computeIfAbsent(uuid, k -> new PlayerTabData(player));
            
            // Update player data
            data.update(player, config, placeholderManager);
        }
        
        // Remove offline players
        playerData.entrySet().removeIf(entry -> 
            players.stream().noneMatch(p -> p.getUUID().equals(entry.getKey())));
    }
    
    private void updateHeaderFooter(Collection<ServerPlayer> players) {
        if (!config.isHeaderFooterEnabled()) return;
        
        for (ServerPlayer player : players) {
            PlayerTabData data = playerData.get(player.getUUID());
            if (data == null) continue;
            
            // Check disable condition
            if (checkDisableCondition(player, config.getHeaderFooterDisableCondition())) {
                continue;
            }
            
            // Get header and footer for this player based on individual settings
            Component header = Component.empty();
            Component footer = Component.empty();
            
            if (config.isEnableHeaders()) {
                header = getHeaderForPlayer(player, data);
            }
            
            if (config.isEnableFooters()) {
                footer = getFooterForPlayer(player, data);
            }
            
            // Send packet
            player.connection.send(new ClientboundTabListPacket(header, footer));
        }
    }
    
    private void updatePlayerNames(Collection<ServerPlayer> players) {
        if (!config.isTablistNameFormattingEnabled()) return;
        
        // Sort players according to configuration
        List<ServerPlayer> sortedPlayers = sortPlayers(new ArrayList<>(players));
        
        for (ServerPlayer player : sortedPlayers) {
            PlayerTabData data = playerData.get(player.getUUID());
            if (data == null) continue;
            
            // Update team for this player
            teamManager.onPlayerJoin(player);
        }
    }
    
    private void updateObjectives(Collection<ServerPlayer> players) {
        // Update objectives for all players
        objectiveManager.updateObjectives();
    }
    
    private void updateBossBars(Collection<ServerPlayer> players) {
        if (!config.isBossBarEnabled()) return;
        
        for (ServerPlayer player : players) {
            PlayerTabData data = playerData.get(player.getUUID());
            if (data == null) continue;
            
            bossBarManager.updatePlayerBossBars(player);
        }
    }
    
    private Component getHeaderForPlayer(ServerPlayer player, PlayerTabData data) {
        // Get appropriate header templates
        List<String> headers = getHeaderTemplatesForPlayer(player);
        
        // Apply animations and placeholders
        return animationManager.getAnimatedHeader(player, headers, placeholderManager);
    }
    
    private Component getFooterForPlayer(ServerPlayer player, PlayerTabData data) {
        // Get appropriate footer templates
        List<String> footers = getFooterTemplatesForPlayer(player);
        
        // Apply animations and placeholders
        return animationManager.getAnimatedFooter(player, footers, placeholderManager);
    }
    
    private List<String> getHeaderTemplatesForPlayer(ServerPlayer player) {
        // Check per-world headers
        String worldName = player.level().dimension().location().toString();
        if (config.hasPerWorldHeaders(worldName)) {
            return config.getPerWorldHeaders(worldName);
        }
        
        // Check per-server headers (if applicable)
        String serverName = getServerName();
        if (config.hasPerServerHeaders(serverName)) {
            return config.getPerServerHeaders(serverName);
        }
        
        // Check group-specific headers
        String group = getPlayerGroup(player);
        if (config.hasGroupHeaders(group)) {
            return config.getGroupHeaders(group);
        }
        
        // Default headers
        return config.getDefaultHeaders();
    }
    
    private List<String> getFooterTemplatesForPlayer(ServerPlayer player) {
        // Similar logic to headers but for footers
        String worldName = player.level().dimension().location().toString();
        if (config.hasPerWorldFooters(worldName)) {
            return config.getPerWorldFooters(worldName);
        }
        
        String serverName = getServerName();
        if (config.hasPerServerFooters(serverName)) {
            return config.getPerServerFooters(serverName);
        }
        
        String group = getPlayerGroup(player);
        if (config.hasGroupFooters(group)) {
            return config.getGroupFooters(group);
        }
        
        return config.getDefaultFooters();
    }
    
    private List<ServerPlayer> sortPlayers(List<ServerPlayer> players) {
        List<String> sortingTypes = config.getSortingTypes();
        
        players.sort((p1, p2) -> {
            for (String sortType : sortingTypes) {
                int result = comparePlayers(p1, p2, sortType);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        });
        
        return players;
    }
    
    private int comparePlayers(ServerPlayer p1, ServerPlayer p2, String sortType) {
        switch (sortType.toLowerCase()) {
            case "groups":
                return compareByGroups(p1, p2);
            case "placeholder_a_to_z":
                return compareByPlaceholder(p1, p2, true);
            case "placeholder_z_to_a":
                return compareByPlaceholder(p1, p2, false);
            default:
                if (sortType.startsWith("placeholder:")) {
                    String placeholder = sortType.substring(12);
                    return compareByCustomPlaceholder(p1, p2, placeholder);
                }
                return p1.getScoreboardName().compareToIgnoreCase(p2.getScoreboardName());
        }
    }
    
    private int compareByGroups(ServerPlayer p1, ServerPlayer p2) {
        String group1 = getPlayerGroup(p1);
        String group2 = getPlayerGroup(p2);
        
        // Get group priorities from config
        int priority1 = config.getGroupPriority(group1);
        int priority2 = config.getGroupPriority(group2);
        
        return Integer.compare(priority1, priority2);
    }
    
    private int compareByPlaceholder(ServerPlayer p1, ServerPlayer p2, boolean ascending) {
        String value1 = placeholderManager.processPlaceholders("%player%", p1);
        String value2 = placeholderManager.processPlaceholders("%player%", p2);
        
        int result = value1.compareToIgnoreCase(value2);
        return ascending ? result : -result;
    }
    
    private int compareByCustomPlaceholder(ServerPlayer p1, ServerPlayer p2, String placeholder) {
        String value1 = placeholderManager.processPlaceholders(placeholder, p1);
        String value2 = placeholderManager.processPlaceholders(placeholder, p2);
        
        return value1.compareToIgnoreCase(value2);
    }
    
    private String getPlayerGroup(ServerPlayer player) {
        // Use the centralized permission system to determine player group
        return PermissionUtil.getPlayerGroup(player);
    }
    
    private String getServerName() {
        // This would be configurable or detected
        return "main";
    }
    
    private boolean checkDisableCondition(ServerPlayer player, String condition) {
        if (condition == null || condition.isEmpty()) return false;
        
        // Parse condition (e.g., "%world%=disabledworld")
        if (condition.contains("=")) {
            String[] parts = condition.split("=", 2);
            String placeholder = parts[0];
            String value = parts[1];
            
            String actualValue = placeholderManager.processPlaceholders(placeholder, player);
            return actualValue.equals(value);
        }
        
        return false;
    }
    
    // Event handlers
    public void onPlayerJoin(ServerPlayer player) {
        PlayerTabData data = new PlayerTabData(player);
        playerData.put(player.getUUID(), data);
        
        // Initialize player in all systems
        teamManager.onPlayerJoin(player);
        objectiveManager.onPlayerJoin(player);
        bossBarManager.onPlayerJoin(player);
        
        NeoEssentials.LOGGER.debug("Player {} added to TAB-like tablist", player.getScoreboardName());
    }
    
    public void onPlayerLeave(ServerPlayer player) {
        playerData.remove(player.getUUID());
        
        // Clean up player from all systems
        teamManager.onPlayerLeave(player);
        objectiveManager.onPlayerLeave(player);
        bossBarManager.onPlayerLeave(player);
        
        NeoEssentials.LOGGER.debug("Player {} removed from TAB-like tablist", player.getScoreboardName());
    }
    
    public void reload() {
        NeoEssentials.LOGGER.info("Reloading TAB-like tablist configuration");
        
        // Stop current task
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        
        // Reload configuration
        this.config = configManager.loadConfig();
        
        // Reinitialize components
        teamManager.reload(config);
        objectiveManager.reload(config);
        bossBarManager.reload(config);
        
        // Restart update task
        startUpdateTask();
        
        NeoEssentials.LOGGER.info("TAB-like tablist system reloaded");
    }
    
    /**
     * Reload the tablist configuration and update all components
     * @return true if reload was successful, false otherwise
     */
    public boolean reloadConfig() {
        NeoEssentials.LOGGER.info("Reloading TAB-like tablist configuration");
        
        try {
            // Reload configuration
            this.config = configManager.reloadConfig();
            
            // Reload animations if available
            if (config.getAnimationsData() != null && !config.getAnimationsData().isEmpty()) {
                animationManager.loadAnimationsFromConfig(config.getAnimationsData());
                NeoEssentials.LOGGER.info("Reloaded animations from YAML configuration");
            }
            
            // Re-initialize components with new config
            teamManager.initialize(config);
            objectiveManager.initialize(config);
            bossBarManager.initialize(config);
            
            // Restart update task with new interval
            startUpdateTask();
            
            NeoEssentials.LOGGER.info("TAB-like tablist configuration reloaded successfully");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to reload TAB-like tablist configuration", e);
            return false;
        }
    }
    
    // Shutdown method for clean cleanup
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
        
        // Clear player data
        playerData.clear();
        
        // Shutdown component managers
        teamManager.shutdown();
        objectiveManager.shutdown();
        bossBarManager.shutdown();
        
        initialized = false;
        NeoEssentials.LOGGER.info("TAB-like tablist system shutdown");
    }
    
    // Getters for external access
    public TABConfig getConfig() {
        return config;
    }
    
    public PlayerTabData getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public AtomicReference<MinecraftServer> getServerRef() {
        return serverRef;
    }
    
    public int getPlayerCount() {
        return playerData.size();
    }
}
