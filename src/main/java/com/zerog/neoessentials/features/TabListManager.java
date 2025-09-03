package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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
        
        // Register custom tablist permissions
        initializeTablistPermissions();
        
        // Load configuration from file
        loadConfig();
        
        instance = this;
        startUpdateTask();
        DebugUtil.debugLog("[TabListManager] Professional TabList Manager initialized");
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
     * Start the update task for automatic tablist refreshing
     */
    private void startUpdateTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                    updateAllPlayers();
                }
            } catch (Exception e) {
                DebugUtil.errorLog("[TabListManager] Error in update task: " + e.getMessage());
            }
        }, updateInterval, updateInterval, TimeUnit.SECONDS);
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
     * Update player's header and footer using new permission-based system
     */
    private void updatePlayerHeaderFooter(ServerPlayer player) {
        PlayerTabData data = playerData.get(player.getUUID());
        if (data == null) return;
        
        String header = defaultHeaderText;
        String footer = defaultFooterText;
        
        // New permission-based layout selection
        if (config != null && config.tablist != null) {
            String selectedLayoutId = determinePlayerLayout(player);
            com.zerog.neoessentials.config.TablistConfig.Layout selectedLayout = null;
            
            if (selectedLayoutId != null && config.tablist.layouts != null) {
                selectedLayout = config.tablist.layouts.get(selectedLayoutId);
            }
            
            // Use the selected layout
            if (selectedLayout != null) {
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] Using layout '" + selectedLayoutId + 
                        "' with priority " + selectedLayout.priority + 
                        " for player " + player.getName().getString());
                }
                    
                if (selectedLayout.header != null && !selectedLayout.header.isEmpty()) {
                    header = String.join("\n", selectedLayout.header);
                }
                if (selectedLayout.footer != null && !selectedLayout.footer.isEmpty()) {
                    footer = String.join("\n", selectedLayout.footer);
                }
            } else {
                if (Boolean.getBoolean("neoessentials.debug.tablist")) {
                    DebugUtil.debugLog("[TabListManager] No layout found for player " + player.getName().getString() + 
                        " (selected layout ID: " + selectedLayoutId + "), using default");
                }
            }
        }
        
        // Process placeholders
        header = placeholderManager.processPlaceholders(header, player);
        footer = placeholderManager.processPlaceholders(footer, player);
        
        // Check if changed
        if (!header.equals(data.lastHeader) || !footer.equals(data.lastFooter)) {
            // Send packet
            Component headerComponent = ColorUtil.colorize(header);
            Component footerComponent = ColorUtil.colorize(footer);
            
            player.connection.send(new ClientboundTabListPacket(headerComponent, footerComponent));
            
            // Update cached values
            data.lastHeader = header;
            data.lastFooter = footer;
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