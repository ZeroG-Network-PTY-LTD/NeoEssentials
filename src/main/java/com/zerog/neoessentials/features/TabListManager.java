package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import com.zerog.neoessentials.util.ColorUtil;
import com.zerog.neoessentials.util.DebugUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Professional TabList Manager - Like BungeeTabListPlus and TAB plugin
 * Provides clean, efficient tablist management for NeoForge servers
 */
public class TabListManager {
    private static TabListManager instance;
    
    // Core components
    private final PlaceholderManager placeholderManager;
    private final CustomPermissionsManager permissionManager;
    private final ScheduledExecutorService scheduler;
    
    // Player state tracking
    private final Map<UUID, PlayerTabData> playerData = new ConcurrentHashMap<>();
    private final Map<String, PlayerTeam> scoreboardTeams = new ConcurrentHashMap<>();
    
    // Configuration
    public com.zerog.neoessentials.config.TablistConfig config;
    private boolean enabled = true;
    private int updateInterval = 20; // ticks (1 second)
    private String defaultHeaderText = "&6&l╔═══════════════════════════════════╗\n&6&l║         &f&lNeoEssentials         &6&l║\n&6&l║ &7Welcome &e{player_name}           &6&l║\n&6&l╚═══════════════════════════════════╝";
    private String defaultFooterText = "&6&l╔═══════════════════════════════════╗\n&6&l║ &7Online: &e{server_players}&7/&e{server_max_players}              &6&l║\n&6&l║ &7Time: &f{time}                   &6&l║\n&6&l╚═══════════════════════════════════╝";
    
    // Update tracking
    private final Object updateLock = new Object();
    
    public TabListManager() {
        this.placeholderManager = PlaceholderManager.getInstance();
        this.permissionManager = CustomPermissionsManager.getInstance();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TabList-Updater");
            t.setDaemon(true);
            return t;
        });
        
        // Register for permission events
        NeoForge.EVENT_BUS.register(this);
        
        // Register custom tablist permissions
        initializeTablistPermissions();
        
        // Load configuration from file
        loadConfig();
        
        instance = this;
        startUpdateTask();
        DebugUtil.debugLog("[TabListManager] Professional TabList Manager initialized and registered for permission events");
    }
    
    /**
     * Initialize custom tablist permissions for new permission set system
     */
    private void initializeTablistPermissions() {
        // Register permission set permissions
        permissionManager.registerCustomPermissionNode("neoessentials.tablist.owner", "Owner tablist layout access");
        permissionManager.registerCustomPermissionNode("neoessentials.tablist.admin", "Admin tablist layout access");
        permissionManager.registerCustomPermissionNode("neoessentials.tablist.moderator", "Moderator tablist layout access");
        permissionManager.registerCustomPermissionNode("neoessentials.tablist.helper", "Helper tablist layout access");
        permissionManager.registerCustomPermissionNode("neoessentials.tablist.vip", "VIP tablist layout access");
        permissionManager.registerCustomPermissionNode("neoessentials.tablist.member", "Member tablist layout access");
        permissionManager.registerCustomPermissionNode("neoessentials.tablist.verified", "Verified tablist layout access");
        
        // Register Discord integration permissions
        permissionManager.registerCustomPermissionNode("neoessentials.discord.owner", "Discord Owner role sync");
        permissionManager.registerCustomPermissionNode("neoessentials.discord.admin", "Discord Admin role sync");
        permissionManager.registerCustomPermissionNode("neoessentials.discord.moderator", "Discord Moderator role sync");
        permissionManager.registerCustomPermissionNode("neoessentials.discord.helper", "Discord Helper role sync");
        permissionManager.registerCustomPermissionNode("neoessentials.discord.vip", "Discord VIP role sync");
        permissionManager.registerCustomPermissionNode("neoessentials.discord.member", "Discord Member role sync");
        permissionManager.registerCustomPermissionNode("neoessentials.discord.verified", "Discord Verified role sync");
        
        // Keep legacy permissions for backward compatibility
        permissionManager.registerCustomPermissionNode("neoessentials.admin", "Administrator permissions");
        permissionManager.registerCustomPermissionNode("neoessentials.moderator", "Moderator permissions");
        permissionManager.registerCustomPermissionNode("neoessentials.helper", "Helper permissions");
        permissionManager.registerCustomPermissionNode("neoessentials.vip", "VIP permissions");
        permissionManager.registerCustomPermissionNode("neoessentials.member", "Member permissions");
        
        DebugUtil.debugLog("[TabListManager] Registered permission set and Discord integration permissions");
    }
    
    public static TabListManager getInstance() {
        return instance;
    }
    
    /**
     * Load configuration from tablist.json file
     */
    private void loadConfig() {
        try {
            String configPath = "config/neoessentials/tablist.json";
            java.io.File configFile = new java.io.File(configPath);
            
            if (configFile.exists()) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
                    config = gson.fromJson(reader, com.zerog.neoessentials.config.TablistConfig.class);
                    
                    // Apply config settings
                    if (config.tablist != null) {
                        this.enabled = config.tablist.enabled;
                        this.updateInterval = config.tablist.updateInterval;
                        
                        // Load header/footer from first layout if available (backward compatibility)
                        if (config.tablist.layouts != null && !config.tablist.layouts.isEmpty()) {
                            // Check if using new Map-based layouts
                            if (!config.tablist.layouts.isEmpty()) {
                                var firstLayout = config.tablist.layouts.values().iterator().next();
                                if (firstLayout.header != null && !firstLayout.header.isEmpty()) {
                                    this.defaultHeaderText = String.join("\n", firstLayout.header);
                                }
                                if (firstLayout.footer != null && !firstLayout.footer.isEmpty()) {
                                    this.defaultFooterText = String.join("\n", firstLayout.footer);
                                }
                            }
                        }
                        
                        // Debug: Log loaded permission sets and layouts
                        if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                            if (config.tablist.permissionSets != null) {
                                DebugUtil.debugLog("[TabListManager] Loaded " + config.tablist.permissionSets.size() + " permission sets:");
                                for (var entry : config.tablist.permissionSets.entrySet()) {
                                    var permSet = entry.getValue();
                                    DebugUtil.debugLog("[TabListManager]   - " + entry.getKey() + 
                                        ": Priority " + permSet.priority + 
                                        ", Condition: " + permSet.conditionType + 
                                        (permSet.permission != null && !permSet.permission.isEmpty() ? " (" + permSet.permission + ")" : "") +
                                        ", Layout: " + permSet.layoutId);
                                }
                            }
                            
                            if (config.tablist.layouts != null) {
                                DebugUtil.debugLog("[TabListManager] Loaded " + config.tablist.layouts.size() + " layouts:");
                                for (var entry : config.tablist.layouts.entrySet()) {
                                    var layout = entry.getValue();
                                    DebugUtil.debugLog("[TabListManager]   - " + entry.getKey() + 
                                        ": Priority " + layout.priority + 
                                        ", Condition: " + layout.conditionType + 
                                        (layout.condition != null ? " (" + layout.condition + ")" : ""));
                                }
                            }
                        }
                    }
                    
                    DebugUtil.debugLog("[TabListManager] Loaded configuration from " + configPath);
                }
            } else {
                // Create default config
                config = createDefaultUnifiedConfigStatic();
                DebugUtil.debugLog("[TabListManager] Using default configuration (file not found)");
            }
        } catch (Exception e) {
            DebugUtil.errorLog("[TabListManager] Error loading config: " + e.getMessage());
            e.printStackTrace();
            config = createDefaultUnifiedConfigStatic();
        }
    }
    
    /**
     * Start the update task - frequent updates for animations, static content updates on events
     */
    private void startUpdateTask() {
        // Start high-frequency task for animations (every 500ms for smooth animations)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                    updateAnimatedPlaceholdersOnly();
                }
            } catch (Exception e) {
                DebugUtil.errorLog("[TabListManager] Error in animation update task: " + e.getMessage());
            }
        }, 0, 500, TimeUnit.MILLISECONDS); // High frequency for smooth animations
        
        // Also start a slower task for general maintenance (every 20 seconds)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                    updateAllPlayersMaintenanceTask();
                }
            } catch (Exception e) {
                DebugUtil.errorLog("[TabListManager] Error in maintenance update task: " + e.getMessage());
            }
        }, 20, 20, TimeUnit.SECONDS);
    }
    
    /**
     * Called when a player joins the server
     */
    public void onPlayerJoin(ServerPlayer player) {
        if (!enabled) return;
        
        PlayerTabData data = new PlayerTabData(player);
        playerData.put(player.getUUID(), data);
        
        // Initialize player's scoreboard team
        setupPlayerTeam(player);
        
        // Send initial header/footer
        updatePlayerHeaderFooter(player);
        
        DebugUtil.debugLog("[TabListManager] Player joined: " + player.getName().getString());
    }
    
    /**
     * Called when a player leaves the server
     */
    public void onPlayerLeave(ServerPlayer player) {
        playerData.remove(player.getUUID());
        cleanupPlayerTeam(player);
        DebugUtil.debugLog("[TabListManager] Player left: " + player.getName().getString());
    }
    
    /**
     * Called when permissions change for a player - triggers immediate update
     */
    public void onPermissionChange(ServerPlayer player) {
        if (!enabled) return;
        
        DebugUtil.debugLog("[TabListManager] Permission changed for: " + player.getName().getString() + " - clearing cache and updating tablist");
        
        // Clear cached data to force comparison and updates
        PlayerTabData data = playerData.get(player.getUUID());
        if (data != null) {
            data.clearCache(); // This will force the interval-style comparison to detect changes
        }
        
        updatePlayer(player);
    }
    
    /**
     * Called when a player's group/team changes - triggers immediate update  
     */
    public void onPlayerGroupChange(ServerPlayer player) {
        if (!enabled) return;
        
        DebugUtil.debugLog("[TabListManager] Group/Team changed for: " + player.getName().getString() + " - clearing cache and updating tablist");
        
        // Clear cached data to force comparison and updates
        PlayerTabData data = playerData.get(player.getUUID());
        if (data != null) {
            data.clearCache(); // This will force the interval-style comparison to detect changes
        }
        
        updatePlayer(player);
    }
    
    /**
     * Event handler for permission updates - automatically called when permissions change
     * Uses interval-style update approach but triggered by permission events
     */
    @SubscribeEvent
    public void onPermissionUpdate(PermissionUpdateEvent event) {
        if (!enabled) return;
        
        ServerPlayer player = event.getPlayer();
        DebugUtil.debugLog("[TabListManager] PermissionUpdateEvent received for: " + player.getName().getString() + " - updating tablist with interval-style approach");
        
        // Clear cached data to force comparison and updates
        PlayerTabData data = playerData.get(player.getUUID());
        if (data != null) {
            data.clearCache(); // This will force the interval-style comparison to detect changes
        }
        
        // Update using interval-style approach
        updatePlayer(player);
    }
    
    /**
     * Update only animated placeholders without refreshing static tablist content
     */
    private void updateAnimatedPlaceholdersOnly() {
        if (!enabled) return;
        
        synchronized (updateLock) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            
            // Only update players who have animated placeholders in their current layout
            for (ServerPlayer player : players) {
                PlayerTabData data = playerData.get(player.getUUID());
                if (data != null && hasAnimatedPlaceholders(data)) {
                    updatePlayerHeaderFooter(player); // Only update header/footer for animations
                }
            }
        }
    }
    
    /**
     * Maintenance task for general updates (non-animated content)
     */
    private void updateAllPlayersMaintenanceTask() {
        if (!enabled) return;
        
        synchronized (updateLock) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            
            // General maintenance updates for all players
            for (ServerPlayer player : players) {
                PlayerTabData data = playerData.get(player.getUUID());
                if (data != null) {
                    // Update team info (prefix/suffix) which might change less frequently
                    updatePlayerTeam(player, data);
                }
            }
        }
    }
    
    /**
     * Check if a player's current cached content contains animated placeholders
     */
    private boolean hasAnimatedPlaceholders(PlayerTabData data) {
        // Check cached header and footer for animated placeholders
        boolean hasAnimated = containsAnimatedPlaceholder(data.lastHeader) || 
                              containsAnimatedPlaceholder(data.lastFooter);
        
        // If no cached animated content, check if we should force an update anyway
        // This handles cases where placeholders are added/changed dynamically
        if (!hasAnimated) {
            // Check if current layout content has animations (fallback check)
            return checkCurrentLayoutForAnimations(data);
        }
        
        return hasAnimated;
    }
    
    /**
     * Check if current layout content might have animations (fallback method)
     */
    private boolean checkCurrentLayoutForAnimations(PlayerTabData data) {
        // This is a fallback - in most cases the cached content check should work
        // But this helps catch cases where animations are added to layouts
        return true; // For now, assume all players might have animations to ensure updates
    }
    
    /**
     * Check if a line contains animated placeholders that need constant updates
     */
    private boolean containsAnimatedPlaceholder(String content) {
        if (content == null || content.isEmpty()) return false;
        
        // Check for our specific animation placeholders
        return content.contains("server_status_animation") || 
               content.contains("test_animation") ||
               content.contains("welcome_animation") ||
               // Generic animation patterns
               content.contains("_animation") || 
               content.contains("{animated_") ||
               content.contains("_animated}") ||
               // Placeholder patterns that might contain animations
               content.contains("${server_status_animation}") ||
               content.contains("${test_animation}") ||
               content.contains("${welcome_animation}") ||
               // Any placeholder that ends with _animation
               content.matches(".*\\$\\{[^}]*_animation\\}.*");
    }
    
    /**
     * Update all online players' tablists
     */
    public void updateAllPlayers() {
        if (!enabled) return;
        
        synchronized (updateLock) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            
            for (ServerPlayer player : players) {
                updatePlayer(player);
            }
        }
    }
    
    /**
     * Update a specific player's tablist
     */
    public void updatePlayer(ServerPlayer player) {
        if (!enabled || player == null) return;
        
        PlayerTabData data = playerData.get(player.getUUID());
        if (data == null) {
            onPlayerJoin(player); // Initialize if missing
            data = playerData.get(player.getUUID());
        }
        
        // Update player's team and display
        updatePlayerTeam(player, data);
        
        // Update header/footer if needed
        updatePlayerHeaderFooter(player);
    }
    
    /**
     * Setup scoreboard team for a player
     */
    private void setupPlayerTeam(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        String teamName = "ne_" + player.getUUID().toString().substring(0, 8);
        
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setCollisionRule(PlayerTeam.CollisionRule.NEVER);
            team.setNameTagVisibility(PlayerTeam.Visibility.ALWAYS);
        }
        
        // Add player to team
        String playerName = player.getGameProfile().getName();
        if (!team.getPlayers().contains(playerName)) {
            scoreboard.addPlayerToTeam(playerName, team);
        }
        
        scoreboardTeams.put(player.getUUID().toString(), team);
    }
    
    /**
     * Update player's team with prefix/suffix
     */
    private void updatePlayerTeam(ServerPlayer player, PlayerTabData data) {
        PlayerTeam team = scoreboardTeams.get(player.getUUID().toString());
        if (team == null) {
            setupPlayerTeam(player);
            team = scoreboardTeams.get(player.getUUID().toString());
        }
        
        if (team == null) return;
        
        // Get rank-based prefix and suffix
        String prefix = getRankPrefix(player);
        String suffix = getPingSuffix(player);
        
        // Check if prefix/suffix changed
        if (!prefix.equals(data.lastPrefix) || !suffix.equals(data.lastSuffix)) {
            // Convert to components and set
            Component prefixComponent = ColorUtil.colorize(prefix);
            Component suffixComponent = ColorUtil.colorize(suffix);
            
            team.setPlayerPrefix(prefixComponent);
            team.setPlayerSuffix(suffixComponent);
            
            // Update cached values
            data.lastPrefix = prefix;
            data.lastSuffix = suffix;
        }
    }
    
    /**
     * Update player's header and footer using permission-based system with interval-style approach
     */
    private void updatePlayerHeaderFooter(ServerPlayer player) {
        PlayerTabData data = playerData.get(player.getUUID());
        if (data == null) return;
        
        String header = getPlayerDynamicHeader(player);
        String footer = getPlayerDynamicFooter(player);
        
        // Debug logging for header/footer selection
        if (Boolean.getBoolean("neoessentials.debug.tablist")) {
            DebugUtil.debugLog("[TabListManager] Selected header for " + player.getName().getString() + ": " + 
                (header != null ? header.substring(0, Math.min(50, header.length())) + "..." : "null"));
            DebugUtil.debugLog("[TabListManager] Selected footer for " + player.getName().getString() + ": " + 
                (footer != null ? footer.substring(0, Math.min(50, footer.length())) + "..." : "null"));
        }
        
        // Process placeholders in header and footer
        header = placeholderManager.processPlaceholders(header, player);
        footer = placeholderManager.processPlaceholders(footer, player);
        
        // Check if header or footer changed (like interval update approach)
        boolean headerChanged = !header.equals(data.lastHeader);
        boolean footerChanged = !footer.equals(data.lastFooter);
        
        // Debug logging for change detection
        if (Boolean.getBoolean("neoessentials.debug.tablist")) {
            DebugUtil.debugLog("[TabListManager] Change detection for " + player.getName().getString() + 
                " - Header changed: " + headerChanged + ", Footer changed: " + footerChanged);
            if (headerChanged) {
                DebugUtil.debugLog("[TabListManager] Header change: '" + data.lastHeader + "' -> '" + header + "'");
            }
            if (footerChanged) {
                DebugUtil.debugLog("[TabListManager] Footer change: '" + data.lastFooter + "' -> '" + footer + "'");
            }
        }
        
        // Only send packet if something actually changed
        if (headerChanged || footerChanged) {
            try {
                // Send combined header/footer packet (interval approach)
                Component headerComponent = (header != null && !header.isEmpty()) ? 
                    ColorUtil.colorize(header) : Component.empty();
                Component footerComponent = (footer != null && !footer.isEmpty()) ? 
                    ColorUtil.colorize(footer) : Component.empty();
                
                player.connection.send(new ClientboundTabListPacket(headerComponent, footerComponent));
                
                // Update cached values
                data.lastHeader = header;
                data.lastFooter = footer;
                
                DebugUtil.debugLog("[TabListManager] Updated header/footer for " + player.getName().getString() + 
                    " - Header changed: " + headerChanged + ", Footer changed: " + footerChanged);
            } catch (Exception e) {
                DebugUtil.debugLog("[TabListManager] Error updating header/footer for " + player.getName().getString() + ": " + e.getMessage());
            }
        } else {
            if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                DebugUtil.debugLog("[TabListManager] No changes detected for " + player.getName().getString() + 
                    " - skipping update");
            }
        }
    }
    
    /**
     * Get dynamic header content based on player's permissions and group
     */
    private String getPlayerDynamicHeader(ServerPlayer player) {
        // First try to get header from permission-based layout
        if (config != null && config.tablist != null) {
            String selectedLayoutId = determinePlayerLayout(player);
            
            if (selectedLayoutId != null && config.tablist.layouts != null) {
                com.zerog.neoessentials.config.TablistConfig.Layout selectedLayout = 
                    config.tablist.layouts.get(selectedLayoutId);
                    
                if (selectedLayout != null && selectedLayout.header != null && !selectedLayout.header.isEmpty()) {
                    if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                        DebugUtil.debugLog("[TabListManager] Using layout '" + selectedLayoutId + 
                            "' header for player " + player.getName().getString());
                    }
                    return String.join("\n", selectedLayout.header);
                }
            }
        }
        
        // Fallback to permission-based header selection
        return getPermissionBasedHeader(player);
    }
    
    /**
     * Get dynamic footer content based on player's permissions and group
     */
    private String getPlayerDynamicFooter(ServerPlayer player) {
        // First try to get footer from permission-based layout
        if (config != null && config.tablist != null) {
            String selectedLayoutId = determinePlayerLayout(player);
            
            if (selectedLayoutId != null && config.tablist.layouts != null) {
                com.zerog.neoessentials.config.TablistConfig.Layout selectedLayout = 
                    config.tablist.layouts.get(selectedLayoutId);
                    
                if (selectedLayout != null && selectedLayout.footer != null && !selectedLayout.footer.isEmpty()) {
                    if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                        DebugUtil.debugLog("[TabListManager] Using layout '" + selectedLayoutId + 
                            "' footer for player " + player.getName().getString());
                    }
                    return String.join("\n", selectedLayout.footer);
                }
            }
        }
        
        // Fallback to permission-based footer selection
        return getPermissionBasedFooter(player);
    }
    
    /**
     * Get header content based on player's permission level (fallback system - uses config)
     */
    private String getPermissionBasedHeader(ServerPlayer player) {
        // First try to get from config layouts using their defined conditions
        if (config != null && config.tablist != null && config.tablist.layouts != null) {
            
            String bestLayoutId = null;
            int highestPriority = -1;
            
            if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                DebugUtil.debugLog("[TabListManager] Checking " + config.tablist.layouts.size() + 
                    " layouts for header selection for " + player.getName().getString());
            }
            
            // Check all layouts and find the highest priority one where player meets the condition
            for (Map.Entry<String, com.zerog.neoessentials.config.TablistConfig.Layout> entry : config.tablist.layouts.entrySet()) {
                String layoutId = entry.getKey();
                com.zerog.neoessentials.config.TablistConfig.Layout layout = entry.getValue();
                
                boolean meetsCondition = playerMeetsLayoutCondition(player, layout);
                
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] Layout '" + layoutId + "' - Priority: " + layout.priority + 
                        ", Meets condition: " + meetsCondition + ", Current best priority: " + highestPriority);
                }
                
                if (layout.priority > highestPriority && meetsCondition) {
                    bestLayoutId = layoutId;
                    highestPriority = layout.priority;
                    if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                        DebugUtil.debugLog("[TabListManager] New best layout: '" + layoutId + "' with priority " + layout.priority);
                    }
                }
            }
            
            // Use the best matching layout
            if (bestLayoutId != null) {
                com.zerog.neoessentials.config.TablistConfig.Layout selectedLayout = config.tablist.layouts.get(bestLayoutId);
                if (selectedLayout.header != null && !selectedLayout.header.isEmpty()) {
                    String headerContent = String.join("\n", selectedLayout.header);
                    DebugUtil.debugLog("[TabListManager] Using config layout '" + bestLayoutId + "' (priority " + highestPriority + ") header for " + player.getName().getString());
                    return headerContent;
                }
            } else {
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] No qualifying layout found for " + player.getName().getString());
                }
            }
        }
        
        // Final fallback only if config is completely unavailable
        DebugUtil.debugLog("[TabListManager] Config unavailable, using hardcoded fallback header for " + player.getName().getString());
        return "&7&l╔═══════════════════════════════════╗\n" +
               "&7&l║         &f&lNeoEssentials         &7&l║\n" +
               "&7&l║ &fWelcome &e{player_name}           &7&l║\n" +
               "&7&l╚═══════════════════════════════════╝";
    }
    
    /**
     * Get footer content based on player's permission level (fallback system - uses config)
     */
    private String getPermissionBasedFooter(ServerPlayer player) {
        // First try to get from config layouts using their defined conditions
        if (config != null && config.tablist != null && config.tablist.layouts != null) {
            
            String bestLayoutId = null;
            int highestPriority = -1;
            
            if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                DebugUtil.debugLog("[TabListManager] Checking " + config.tablist.layouts.size() + 
                    " layouts for footer selection for " + player.getName().getString());
            }
            
            // Check all layouts and find the highest priority one where player meets the condition
            for (Map.Entry<String, com.zerog.neoessentials.config.TablistConfig.Layout> entry : config.tablist.layouts.entrySet()) {
                String layoutId = entry.getKey();
                com.zerog.neoessentials.config.TablistConfig.Layout layout = entry.getValue();
                
                boolean meetsCondition = playerMeetsLayoutCondition(player, layout);
                
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] Layout '" + layoutId + "' - Priority: " + layout.priority + 
                        ", Meets condition: " + meetsCondition + ", Current best priority: " + highestPriority);
                }
                
                if (layout.priority > highestPriority && meetsCondition) {
                    bestLayoutId = layoutId;
                    highestPriority = layout.priority;
                    if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                        DebugUtil.debugLog("[TabListManager] New best layout: '" + layoutId + "' with priority " + layout.priority);
                    }
                }
            }
            
            // Use the best matching layout
            if (bestLayoutId != null) {
                com.zerog.neoessentials.config.TablistConfig.Layout selectedLayout = config.tablist.layouts.get(bestLayoutId);
                if (selectedLayout.footer != null && !selectedLayout.footer.isEmpty()) {
                    String footerContent = String.join("\n", selectedLayout.footer);
                    DebugUtil.debugLog("[TabListManager] Using config layout '" + bestLayoutId + "' (priority " + highestPriority + ") footer for " + player.getName().getString());
                    return footerContent;
                }
            } else {
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] No qualifying layout found for " + player.getName().getString());
                }
            }
        }
        
        // Final fallback only if config is completely unavailable
        DebugUtil.debugLog("[TabListManager] Config unavailable, using hardcoded fallback footer for " + player.getName().getString());
        String baseFooter = "&7&l╚═══════════════════════════════════╝\n" +
                           "&f&l| &7TPS: &a{server_tps} &f&l| &7Ping: &e{player_ping}ms &f&l|\n" +
                           "&f&l| &7Players: &b{server_players}/{server_max_players} &f&l|\n";
        
        return baseFooter + "&7&l| &fPlayer Access - Basic Features &7&l|\n" +
               "&7&l╚═══════════════════════════════════╝";
    }
    
    /**
     * Check if a player meets the condition requirements for a layout
     */
    private boolean playerMeetsLayoutCondition(ServerPlayer player, com.zerog.neoessentials.config.TablistConfig.Layout layout) {
        if (layout.conditionType == null) {
            if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                DebugUtil.debugLog("[TabListManager] Layout has no condition - everyone qualifies");
            }
            return true; // No condition means everyone qualifies
        }
        
        switch (layout.conditionType) {
            case "default":
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] Layout is default type - player " + 
                        player.getName().getString() + " qualifies");
                }
                return true; // Everyone qualifies for default
                
            case "permission":
                if (layout.condition != null && !layout.condition.isEmpty()) {
                    boolean hasPermission = permissionManager.hasPermission(player, layout.condition);
                    if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                        DebugUtil.debugLog("[TabListManager] Permission check for " + 
                            player.getName().getString() + " - " + layout.condition + ": " + hasPermission);
                    }
                    return hasPermission;
                }
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] Permission type layout but no condition - everyone qualifies");
                }
                return true; // No specific permission means everyone qualifies
                
            default:
                DebugUtil.debugLog("[TabListManager] Unknown condition type: " + layout.conditionType + " for layout");
                return false;
        }
    }
    
    /**
     * Determine which layout a player should use based on permission sets
     */
    private String determinePlayerLayout(ServerPlayer player) {
        if (config == null || config.tablist == null || config.tablist.permissionSets == null) {
            return "default_layout";
        }
        
        String highestPermissionSet = null;
        int highestPriority = -1;
        
        // Check all permission sets to find the highest priority one the player qualifies for
        for (var entry : config.tablist.permissionSets.entrySet()) {
            String permissionSetId = entry.getKey();
            var permissionSet = entry.getValue();
            
            boolean qualifies = false;
            
            if ("default".equals(permissionSet.conditionType)) {
                qualifies = true; // Everyone qualifies for default
            } else if ("permission".equals(permissionSet.conditionType) && 
                       permissionSet.permission != null && !permissionSet.permission.isEmpty()) {
                qualifies = permissionManager.hasPermission(player, permissionSet.permission);
            }
            
            if (qualifies && permissionSet.priority > highestPriority) {
                highestPermissionSet = permissionSetId;
                highestPriority = permissionSet.priority;
                
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] Player " + player.getName().getString() + 
                        " qualifies for permission set '" + permissionSetId + 
                        "' (priority " + permissionSet.priority + 
                        ", condition: " + permissionSet.conditionType + 
                        (permissionSet.permission != null ? ", permission: " + permissionSet.permission : "") + ")");
                }
            }
        }
        
        // Get the layout ID from the highest priority permission set
        if (highestPermissionSet != null) {
            var permissionSet = config.tablist.permissionSets.get(highestPermissionSet);
            if (permissionSet.layoutId != null && !permissionSet.layoutId.isEmpty()) {
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] Selected permission set '" + highestPermissionSet + 
                        "' with layout '" + permissionSet.layoutId + 
                        "' (priority " + highestPriority + ") for player " + player.getName().getString());
                }
                return permissionSet.layoutId;
            }
        }
        
        if (Boolean.getBoolean("neoessentials.debug.tablist")) {
            DebugUtil.debugLog("[TabListManager] No qualifying permission set found for player " + 
                player.getName().getString() + ", using default layout");
        }
        
        // Fallback to default layout
        return "default_layout";
    }
    
    /**
     * Get rank-based prefix for a player using new permission set system
     */
    private String getRankPrefix(ServerPlayer player) {
        // First, try to get prefix from the permission system
        String permissionPrefix = permissionManager.getPlayerPrefix(player.getUUID());
        if (permissionPrefix != null && !permissionPrefix.isEmpty()) {
            return permissionPrefix + " ";
        }
        
        // Use permission set system to determine prefix
        if (config != null && config.tablist != null && config.tablist.permissionSets != null) {
            String selectedPermissionSet = determinePlayerPermissionSet(player);
            if (selectedPermissionSet != null) {
                return getPermissionSetPrefix(selectedPermissionSet) + " ";
            }
        }
        
        // Fallback to legacy permission-based detection
        if (permissionManager.hasPermission(player, "neoessentials.tablist.owner")) {
            return "&4[OWNER]&r ";
        } else if (permissionManager.hasPermission(player, "neoessentials.tablist.admin") || 
                   permissionManager.hasPermission(player, "neoessentials.admin")) {
            return "&c[ADMIN]&r ";
        } else if (permissionManager.hasPermission(player, "neoessentials.tablist.moderator") || 
                   permissionManager.hasPermission(player, "neoessentials.moderator")) {
            return "&6[MOD]&r ";
        } else if (permissionManager.hasPermission(player, "neoessentials.tablist.helper") || 
                   permissionManager.hasPermission(player, "neoessentials.helper")) {
            return "&b[HELPER]&r ";
        } else if (permissionManager.hasPermission(player, "neoessentials.tablist.vip") || 
                   permissionManager.hasPermission(player, "neoessentials.vip")) {
            return "&d[VIP]&r ";
        } else if (permissionManager.hasPermission(player, "neoessentials.tablist.member") || 
                   permissionManager.hasPermission(player, "neoessentials.member")) {
            return "&a[MEMBER]&r ";
        } else if (permissionManager.hasPermission(player, "neoessentials.tablist.verified")) {
            return "&7[VERIFIED]&r ";
        }
        
        return "&7[PLAYER]&r ";
    }
    
    /**
     * Determine which permission set a player belongs to (for prefix/suffix purposes)
     */
    private String determinePlayerPermissionSet(ServerPlayer player) {
        if (config == null || config.tablist == null || config.tablist.permissionSets == null) {
            return "default";
        }
        
        String highestPermissionSet = "default";
        int highestPriority = -1;
        
        // Check all permission sets to find the highest priority one the player qualifies for
        for (var entry : config.tablist.permissionSets.entrySet()) {
            String permissionSetId = entry.getKey();
            var permissionSet = entry.getValue();
            
            boolean qualifies = false;
            
            if ("default".equals(permissionSet.conditionType)) {
                qualifies = true; // Everyone qualifies for default
            } else if ("permission".equals(permissionSet.conditionType) && 
                       permissionSet.permission != null && !permissionSet.permission.isEmpty()) {
                qualifies = permissionManager.hasPermission(player, permissionSet.permission);
            }
            
            if (qualifies && permissionSet.priority > highestPriority) {
                highestPermissionSet = permissionSetId;
                highestPriority = permissionSet.priority;
            }
        }
        
        return highestPermissionSet;
    }
    
    /**
     * Get prefix for a permission set
     */
    private String getPermissionSetPrefix(String permissionSetId) {
        switch (permissionSetId) {
            case "owner": return "&4[OWNER]&r";
            case "admin": return "&c[ADMIN]&r";
            case "moderator": return "&6[MOD]&r";
            case "helper": return "&b[HELPER]&r";
            case "vip": return "&d[VIP]&r";
            case "member": return "&a[MEMBER]&r";
            case "verified": return "&7[VERIFIED]&r";
            default: return "&7[PLAYER]&r";
        }
    }
    
    /**
     * Get ping-based suffix for a player - integrates with permission system
     */
    private String getPingSuffix(ServerPlayer player) {
        // First, try to get suffix from the permission system
        String permissionSuffix = permissionManager.getPlayerSuffix(player.getUUID());
        
        // Calculate ping with color
        int ping = player.connection.latency();
        String pingColor;
        if (ping < 50) {
            pingColor = "&a"; // Green
        } else if (ping < 100) {
            pingColor = "&e"; // Yellow
        } else if (ping < 200) {
            pingColor = "&6"; // Orange
        } else {
            pingColor = "&c"; // Red
        }
        String pingSuffix = " " + pingColor + ping + "ms&r";
        
        // Combine permission suffix with ping
        if (permissionSuffix != null && !permissionSuffix.isEmpty()) {
            return permissionSuffix + pingSuffix;
        }
        
        return pingSuffix;
    }
    
    /**
     * Cleanup player's team when they leave
     */
    private void cleanupPlayerTeam(ServerPlayer player) {
        PlayerTeam team = scoreboardTeams.remove(player.getUUID().toString());
        if (team != null) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                Scoreboard scoreboard = server.getScoreboard();
                scoreboard.removePlayerTeam(team);
            }
        }
    }
    
    /**
     * Force refresh all players (useful for permission changes)
     */
    public void refreshAll() {
        if (!enabled) return;
        
        // Clear cached data to force updates
        for (PlayerTabData data : playerData.values()) {
            data.clearCache();
        }
        
        updateAllPlayers();
        DebugUtil.debugLog("[TabListManager] Force refreshed all players");
    }
    
    /**
     * Force refresh a specific player with cache clearing (useful for permission/group changes)
     */
    public void forceRefreshPlayer(ServerPlayer player) {
        if (!enabled || player == null) return;
        
        DebugUtil.debugLog("[TabListManager] Force refreshing player: " + player.getName().getString());
        
        // Clear cached data to force comparison and updates
        PlayerTabData data = playerData.get(player.getUUID());
        if (data != null) {
            data.clearCache(); // This will force the interval-style comparison to detect changes
            DebugUtil.debugLog("[TabListManager] Cleared cache for player: " + player.getName().getString());
        }
        
        // Force update everything for this player
        updatePlayer(player);
        
        DebugUtil.debugLog("[TabListManager] Force refresh completed for player: " + player.getName().getString());
    }
    
    /**
     * Test method to manually trigger permission change for a player
     * This can be called from commands or other parts of the system
     */
    public void testPermissionChange(ServerPlayer player) {
        DebugUtil.debugLog("[TabListManager] Manual permission change test triggered for: " + player.getName().getString());
        onPermissionChange(player);
    }
    
    /**
     * Enable/disable the tablist system
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        DebugUtil.debugLog("[TabListManager] TabList " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if tablist is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set custom header text
     */
    public void setHeaderText(String headerText) {
        this.defaultHeaderText = headerText;
        refreshAll();
    }
    
    /**
     * Set custom footer text
     */
    public void setFooterText(String footerText) {
        this.defaultFooterText = footerText;
        refreshAll();
    }
    
    /**
     * Get current header text
     */
    public String getHeaderText() {
        return defaultHeaderText;
    }
    
    /**
     * Get current footer text
     */
    public String getFooterText() {
        return defaultFooterText;
    }
    
    // ========================================
    // COMPATIBILITY METHODS FOR OTHER CLASSES
    // ========================================
    
    /**
     * Legacy method - reload configuration (compatibility)
     */
    public void reloadConfig() {
        DebugUtil.debugLog("[TabListManager] Reloading configuration...");
        loadConfig();
        refreshAll();
    }
    
    /**
     * Legacy method - update tablist for collection of players (compatibility)
     */
    public void updateTabList(Collection<ServerPlayer> players) {
        if (!enabled || players == null) return;
        
        for (ServerPlayer player : players) {
            updatePlayer(player);
        }
    }
    
    /**
     * Legacy method - update header/footer (compatibility)
     */
    public void updateHeaderFooter(ServerPlayer player, String displayName) {
        if (!enabled || player == null) return;
        updatePlayerHeaderFooter(player);
    }
    
    /**
     * Legacy method - update player entry (compatibility) 
     */
    public void updatePlayerEntry(ServerPlayer player) {
        if (!enabled || player == null) return;
        updatePlayer(player);
    }
    
    /**
     * Static method for config creation with new permission set system
     */
    public static com.zerog.neoessentials.config.TablistConfig createDefaultUnifiedConfigStatic() {
        com.zerog.neoessentials.config.TablistConfig config = new com.zerog.neoessentials.config.TablistConfig();
        
        // Initialize tablist section with new permission set system
        config.tablist = new com.zerog.neoessentials.config.TablistConfig.TablistSection();
        config.tablist.enabled = true;
        config.tablist.updateInterval = 20;
        config.tablist.format = "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}";
        
        // The TablistSection constructor already initializes permission sets and layouts
        // So we don't need to do it manually here
        
        return config;
    }
    
    /**
     * Legacy method - refresh tablist for all players (compatibility)
     */
    public void refreshTablistForAll(Collection<ServerPlayer> players) {
        updateTabList(players);
    }
    
    /**
     * Legacy method - start animated placeholder refresh (compatibility)
     */
    public void startAnimatedPlaceholderRefresh(String placeholderId, double intervalSeconds) {
        DebugUtil.debugLog("[TabListManager] Animated placeholder refresh requested for: " + placeholderId);
        // The new system updates automatically, so we just log this request
    }
    
    /**
     * Get debug information about the TabList system
     */
    public String getDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("=== TabList Manager Debug Info ===\n");
        debug.append("Enabled: ").append(enabled).append("\n");
        debug.append("Update Interval: ").append(updateInterval).append(" ticks\n");
        debug.append("Active Players: ").append(playerData.size()).append("\n");
        debug.append("Scoreboard Teams: ").append(scoreboardTeams.size()).append("\n");
        
        if (config != null && config.tablist != null) {
            debug.append("Config Loaded: true\n");
            if (config.tablist.layouts != null) {
                debug.append("Available Layouts: ").append(config.tablist.layouts.size()).append("\n");
                for (String layoutId : config.tablist.layouts.keySet()) {
                    debug.append("  - ").append(layoutId).append("\n");
                }
            }
            if (config.tablist.permissionSets != null) {
                debug.append("Permission Sets: ").append(config.tablist.permissionSets.size()).append("\n");
                for (var entry : config.tablist.permissionSets.entrySet()) {
                    var permSet = entry.getValue();
                    debug.append("  - ").append(entry.getKey())
                         .append(" (priority: ").append(permSet.priority)
                         .append(", layout: ").append(permSet.layoutId).append(")\n");
                }
            }
        } else {
            debug.append("Config Loaded: false\n");
        }
        
        debug.append("Default Header: ").append(defaultHeaderText.length()).append(" chars\n");
        debug.append("Default Footer: ").append(defaultFooterText.length()).append(" chars\n");
        
        return debug.toString();
    }
    
    /**
     * Check if there are active config layouts available
     */
    public boolean hasActiveConfigLayouts() {
        return config != null && 
               config.tablist != null && 
               config.tablist.layouts != null && 
               !config.tablist.layouts.isEmpty();
    }
    
    /**
     * Shutdown the manager
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        playerData.clear();
        scoreboardTeams.clear();
        DebugUtil.debugLog("[TabListManager] TabList Manager shutdown");
    }
    
    /**
     * Player-specific tablist data
     */
    private static class PlayerTabData {
        private String lastPrefix = "";
        private String lastSuffix = "";
        private String lastHeader = "";
        private String lastFooter = "";
        
        public PlayerTabData(ServerPlayer player) {
            // Initialize with empty cache
        }
        
        public void clearCache() {
            lastPrefix = "";
            lastSuffix = "";
            lastHeader = "";
            lastFooter = "";
        }
    }
}