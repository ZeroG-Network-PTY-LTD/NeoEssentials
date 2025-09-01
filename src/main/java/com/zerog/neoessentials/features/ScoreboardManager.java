package com.zerog.neoessentials.features;

import com.zerog.neoessentials.util.DebugUtil;
import com.zerog.neoessentials.util.ColorUtil;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import com.google.gson.Gson;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced ScoreboardManager with improved performance, caching, and UI features
 */
public class ScoreboardManager {
    
    // Enhanced data structures for better performance
    private final Map<String, String> groupPrefixes = new ConcurrentHashMap<>();
    private final Map<String, String> groupSuffixes = new ConcurrentHashMap<>();
    private final Map<String, String> groupTitles = new ConcurrentHashMap<>();
    private final Map<String, List<String>> groupLines = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerJoinTime = new ConcurrentHashMap<>();
    private final Map<UUID, ScoreboardCache> scoreboardCache = new ConcurrentHashMap<>();
    private final Map<UUID, ScoreboardState> playerScoreboards = new ConcurrentHashMap<>();
    
    // Performance and scheduling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private long lastConfigReload = 0;
    private final long CONFIG_RELOAD_INTERVAL = 30000; // 30 seconds
    private final long UPDATE_INTERVAL = 5000; // 5 seconds for scoreboard updates
    
    // Constants
    private static final String OBJECTIVE_PREFIX = "neoess_sidebar_";
    private static final String TEAM_PREFIX = "neo_";
    
    private static ScoreboardManager instance;
    private MinecraftServer server;
    
    // Configuration
    private boolean enableScoreboard = true;
    private boolean enableColorCodes = true;
    private com.zerog.neoessentials.config.TablistConfig config;

    public ScoreboardManager() {
        DebugUtil.debugLog("[ScoreboardManager] Initializing with enhanced performance features");
        instance = this;
        NeoForge.EVENT_BUS.register(this);
        loadConfig();
        loadGroupConfig();
        startPeriodicUpdates();
        DebugUtil.debugLog("[ScoreboardManager] Initialization complete");
    }
    
    /**
     * Start periodic update task for better performance
     */
    private void startPeriodicUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (enableScoreboard) {
                    updateAllScoreboards();
                }
                
                // Periodic config reload check
                long now = System.currentTimeMillis();
                if (now - lastConfigReload > CONFIG_RELOAD_INTERVAL) {
                    reloadConfigIfNeeded();
                    lastConfigReload = now;
                }
            } catch (Exception e) {
                DebugUtil.errorLog("[ScoreboardManager] Error in periodic update: " + e.getMessage());
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Reload configuration if files have changed
     */
    private void reloadConfigIfNeeded() {
        // Check file modification times and reload if needed
        // This would require file system monitoring for production
        DebugUtil.debugLog("[ScoreboardManager] Checking for config changes...");
    }

    /**
     * Enhanced config loading with unified configuration support
     */
    private void loadConfig() {
        try {
            String configPath = "config/neoessentials/scoreboard.json";
            com.google.gson.GsonBuilder gsonBuilder = new com.google.gson.GsonBuilder();
            com.google.gson.Gson gson = gsonBuilder.create();
            
            java.io.File configFile = new java.io.File(configPath);
            if (!configFile.exists()) {
                // Try fallback to tablist.json scoreboard section
                String fallbackPath = "config/neoessentials/tablist.json";
                java.io.File fallbackFile = new java.io.File(fallbackPath);
                if (fallbackFile.exists()) {
                    loadFromTablistConfig(fallbackFile, gson);
                    return;
                }
                
                // Create default config
                createDefaultScoreboardConfig(configFile, gson);
                return;
            }
            
            try (FileReader reader = new FileReader(configFile)) {
                com.zerog.neoessentials.config.TablistConfig config = gson.fromJson(reader, 
                    com.zerog.neoessentials.config.TablistConfig.class);
                
                if (config != null && config.scoreboard != null) {
                    // Load from unified config structure
                    loadFromUnifiedConfig(config);
                    this.config = config; // Store the loaded config
                } else {
                    // Load from legacy structure
                    reader.close();
                    loadLegacyConfig(configFile);
                }
                
                DebugUtil.debugLog("[ScoreboardManager] Unified config loaded successfully - " + 
                                  "Enabled: " + enableScoreboard + ", Layouts: " + groupTitles.size());
            }
        } catch (Exception e) {
            DebugUtil.warnLog("[ScoreboardManager] Error loading config, using defaults: " + e.getMessage());
            loadDefaultConfig();
        }
    }
    
    /**
     * Load configuration from unified TablistConfig structure
     */
    private void loadFromUnifiedConfig(com.zerog.neoessentials.config.TablistConfig config) {
        enableScoreboard = config.scoreboard.enabled;
        enableColorCodes = true; // Default enabled
        
        groupTitles.clear();
        groupLines.clear();
        
        // Load layouts as groups
        if (config.scoreboard.layouts != null) {
            for (com.zerog.neoessentials.config.TablistConfig.Layout layout : config.scoreboard.layouts) {
                String groupName = layout.conditionType != null ? layout.conditionType : "default";
                if (layout.condition != null && !layout.condition.isEmpty()) {
                    groupName = layout.condition.replace(":", "_");
                }
                
                String title = layout.title != null ? layout.title : config.scoreboard.title;
                groupTitles.put(groupName, title);
                
                if (layout.lines != null) {
                    groupLines.put(groupName, new ArrayList<>(layout.lines));
                }
            }
        }
        
        // Ensure default group exists
        if (!groupTitles.containsKey("default")) {
            groupTitles.put("default", config.scoreboard.title != null ? 
                           config.scoreboard.title : "&e&lServer Stats");
            groupLines.put("default", Arrays.asList(
                "&7━━━━━━━━━━━━━━━━━",
                "&aName: &f{player_name}",
                "&bRank: &f{ftb_rank_display_name}",
                "&cHealth: &f{player_health}❤",
                "&7━━━━━━━━━━━━━━━━━"
            ));
        }
    }
    
    /**
     * Load from tablist.json scoreboard section
     */
    private void loadFromTablistConfig(java.io.File tablistFile, com.google.gson.Gson gson) {
        try (FileReader reader = new FileReader(tablistFile)) {
            com.zerog.neoessentials.config.TablistConfig config = gson.fromJson(reader, 
                com.zerog.neoessentials.config.TablistConfig.class);
            
            if (config != null && config.scoreboard != null) {
                loadFromUnifiedConfig(config);
                DebugUtil.debugLog("[ScoreboardManager] Loaded scoreboard config from tablist.json");
            } else {
                loadDefaultConfig();
            }
        } catch (Exception e) {
            DebugUtil.warnLog("[ScoreboardManager] Error loading from tablist.json: " + e.getMessage());
            loadDefaultConfig();
        }
    }
    
    /**
     * Create default scoreboard.json configuration
     */
    private void createDefaultScoreboardConfig(java.io.File configFile, com.google.gson.Gson gson) {
        try {
            configFile.getParentFile().mkdirs();
            
            com.zerog.neoessentials.config.TablistConfig defaultConfig = new com.zerog.neoessentials.config.TablistConfig();
            defaultConfig.scoreboard = new com.zerog.neoessentials.config.TablistConfig.ScoreboardSection();
            defaultConfig.scoreboard.enabled = true;
            defaultConfig.scoreboard.updateInterval = 20;
            defaultConfig.scoreboard.title = "&6&lNeoEssentials";
            
            // Add default layout
            com.zerog.neoessentials.config.TablistConfig.Layout defaultLayout = 
                new com.zerog.neoessentials.config.TablistConfig.Layout();
            defaultLayout.priority = 1;
            defaultLayout.conditionType = "default";
            defaultLayout.title = "&e&lPLAYER INFO";
            defaultLayout.lines = Arrays.asList(
                "&7&m─────────────────",
                "&e&lPLAYER INFO",
                "&7&m─────────────────",
                "&f● &7Player: &e{player_name}",
                "&f● &7Level: &a{player_level}",
                "&f● &7Health: &c{player_health}&7/&c{player_max_health}",
                "&f● &7Food: &6{player_food}",
                "",
                "&f● &7Team: &b{ftb_team_display_name}",
                "&f● &7Rank: &a{ftb_rank_display_name}",
                "",
                "&f● &7Online: &e{server_players}&7/&e{server_max_players}",
                "&f● &7Time: &f{time}",
                "&7&m─────────────────"
            );
            defaultConfig.scoreboard.layouts = Arrays.asList(defaultLayout);
            
            try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
                gson.toJson(defaultConfig, writer);
            }
            
            loadFromUnifiedConfig(defaultConfig);
            DebugUtil.debugLog("[ScoreboardManager] Created default scoreboard.json configuration");
            
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error creating default config: " + e.getMessage());
            loadDefaultConfig();
        }
    }
    
    /**
     * Load legacy configuration format
     */
    private void loadLegacyConfig(java.io.File configFile) {
        try (FileReader reader = new FileReader(configFile)) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            Map<?, ?> config = gson.fromJson(reader, Map.class);
            
            // Load main settings
            if (config.containsKey("enableScoreboard")) {
                Object val = config.get("enableScoreboard");
                enableScoreboard = val instanceof Boolean ? (Boolean) val : 
                                 Boolean.parseBoolean(val.toString());
            }
            
                if (config.containsKey("enableColorCodes")) {
                    Object val = config.get("enableColorCodes");
                    enableColorCodes = val instanceof Boolean ? (Boolean) val : 
                                     Boolean.parseBoolean(val.toString());
                }            // Load group configurations
            Map<?, ?> groups = (Map<?, ?>) config.get("groups");
            groupTitles.clear();
            groupLines.clear();
            
            if (groups != null) {
                for (Map.Entry<?, ?> entry : groups.entrySet()) {
                    String group = entry.getKey().toString();
                    Map<?, ?> groupConfig = (Map<?, ?>) entry.getValue();
                    
                    String title = groupConfig.containsKey("title") ? 
                                  groupConfig.get("title").toString() : "&e&lPlayer Stats";
                    List<String> lines = new ArrayList<>();
                    
                    Object linesObj = groupConfig.get("lines");
                    if (linesObj instanceof List<?>) {
                        for (Object line : (List<?>) linesObj) {
                            lines.add(line.toString());
                        }
                    }
                    
                    groupTitles.put(group, title);
                    groupLines.put(group, lines);
                }
            }
            
            DebugUtil.debugLog("[ScoreboardManager] Legacy config loaded successfully - Groups: " + 
                              groupTitles.size() + ", Enabled: " + enableScoreboard);
        } catch (Exception e) {
            DebugUtil.warnLog("[ScoreboardManager] Error loading legacy config: " + e.getMessage());
            loadDefaultConfig();
        }
    }
    
    /**
     * Load default configuration when config file is missing or invalid
     */
    private void loadDefaultConfig() {
        enableScoreboard = true;
        enableColorCodes = true;
        
        groupTitles.clear();
        groupLines.clear();
        
        groupTitles.put("default", "&e&lServer Stats");
        groupLines.put("default", Arrays.asList(
            "&7━━━━━━━━━━━━━━━━━",
            "&aName: &f%player%",
            "&bRank: &f%group%",
            "&cHealth: &f%health%❤ &7| &6Food: &f%hunger%🍖",
            "&7━━━━━━━━━━━━━━━━━",
            "&dLocation: &f%world%",
            "&eCoords: &f%x%, %y%, %z%",
            "&7━━━━━━━━━━━━━━━━━",
            "&9Online: &f%onlinetime%",
            "&5TPS: &f%tps%",
            "&7━━━━━━━━━━━━━━━━━"
        ));
        
        groupTitles.put("admin", "&c&lAdmin Panel");
        groupLines.put("admin", Arrays.asList(
            "&7━━━━━━━━━━━━━━━━━",
            "&4Admin: &f%player%",
            "&cRank: &f%group%",
            "&7━━━━━━━━━━━━━━━━━",
            "&aPlayers: &f%online%/%max%",
            "&bTPS: &f%tps%",
            "&dMemory: &f%memory_used%MB",
            "&7━━━━━━━━━━━━━━━━━",
            "&eUptime: &f%uptime%",
            "&6CPU: &f%cpu_usage%%",
            "&7━━━━━━━━━━━━━━━━━"
        ));
        
        DebugUtil.debugLog("[ScoreboardManager] Loaded default configuration");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();
    }


    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[ScoreboardManager] onPlayerJoin event fired for " + player.getGameProfile().getName());
            playerJoinTime.put(player.getUUID(), System.currentTimeMillis());
            updateScoreboard(player);
            com.zerog.neoessentials.features.TabListManager.getInstance().onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cleanupPlayer(player);
            playerJoinTime.remove(player.getUUID());
        }
    }

    public void updateAllScoreboards() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateScoreboard(player);
        }
    }

    /**
     * Enhanced scoreboard update with unified configuration support
     */
    public void updateScoreboard(ServerPlayer player) {
        if (player == null || !enableScoreboard) {
            return;
        }
        
        if (player.getServer() == null) {
            DebugUtil.debugLog("[ScoreboardManager] Server null for " + player.getName().getString() + 
                              ", scheduling delayed update");
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> updateScoreboard(player));
            }
            return;
        }
        
        try {
            // Check if unified config is available
            if (config != null && config.scoreboard != null) {
                updateScoreboardUnified(player);
            } else {
                updateScoreboardLegacy(player);
            }
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error updating scoreboard for " + 
                              player.getName().getString() + ": " + e.getMessage());
        }
    }
    
    /**
     * Update scoreboard using unified configuration
     */
    private void updateScoreboardUnified(ServerPlayer player) {
        if (!config.scoreboard.enabled) return;
        
        ScoreboardState state = playerScoreboards.computeIfAbsent(player.getUUID(), k -> new ScoreboardState());
        
        // Find matching layout with priority system
        com.zerog.neoessentials.config.TablistConfig.Layout matchedLayout = findMatchingLayoutUnified(player);
        if (matchedLayout == null) return;
        
        // Process animated title if enabled
        String title = getAnimatedTitle();
        if (title == null || title.isEmpty()) {
            title = matchedLayout.title != null ? matchedLayout.title : config.scoreboard.title;
        }
        String processedTitle = processPlaceholders(title, player);
        
        // Process lines with conditional logic and animations
        List<String> processedLines = new ArrayList<>();
        if (matchedLayout.lines != null) {
            for (String line : matchedLayout.lines) {
                // Apply conditional logic
                if (line.contains("${") && line.contains("}")) {
                    line = processConditionalLogic(line, player);
                }
                
                // Apply animations
                line = processAnimations(line, player);
                
                // Process placeholders
                String processedLine = processPlaceholders(line, player);
                processedLines.add(processedLine.replace('&', '§'));
            }
        }
        
        // Limit lines to maxLines
        if (processedLines.size() > config.scoreboard.maxLines) {
            processedLines = processedLines.subList(0, config.scoreboard.maxLines);
        }
        
        // Update scoreboard if changed
        if (!processedTitle.equals(state.lastTitle) || !processedLines.equals(state.lastLines)) {
            state.lastTitle = processedTitle;
            state.lastLines = new ArrayList<>(processedLines);
            
            // Send scoreboard update
            sendScoreboardUpdateUnified(player, processedTitle, processedLines);
        }
    }
    
    /**
     * Find matching layout using unified configuration
     */
    private com.zerog.neoessentials.config.TablistConfig.Layout findMatchingLayoutUnified(ServerPlayer player) {
        if (config.scoreboard.layouts == null || config.scoreboard.layouts.isEmpty()) {
            return null;
        }
        
        com.zerog.neoessentials.config.TablistConfig.Layout bestMatch = null;
        int highestPriority = -1;
        
        for (com.zerog.neoessentials.config.TablistConfig.Layout layout : config.scoreboard.layouts) {
            if (layout.priority <= highestPriority) continue;
            
            if (matchesConditionUnified(player, layout)) {
                bestMatch = layout;
                highestPriority = layout.priority;
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Check if player matches layout condition (unified config)
     */
    private boolean matchesConditionUnified(ServerPlayer player, com.zerog.neoessentials.config.TablistConfig.Layout layout) {
        String conditionType = layout.conditionType != null ? layout.conditionType : "default";
        String condition = layout.condition != null ? layout.condition : "";
        
        switch (conditionType.toLowerCase()) {
            case "permission":
                return hasPermissionMethod(player, condition);
            case "placeholder":
                return checkPlaceholderConditionUnified(player, condition);
            case "default":
            default:
                return true;
        }
    }
    
    /**
     * Check placeholder-based condition (unified config)
     */
    private boolean checkPlaceholderConditionUnified(ServerPlayer player, String condition) {
        if (condition == null || !condition.contains(":")) return false;
        
        String[] parts = condition.split(":", 2);
        String placeholderName = parts[0];
        String expectedValue = parts[1];
        
        String actualValue = processPlaceholders("{" + placeholderName + "}", player);
        
        return expectedValue.equals(actualValue);
    }
    
    /**
     * Legacy scoreboard update method
     */
    private void updateScoreboardLegacy(ServerPlayer player) {
        // Check cache for performance
        UUID playerId = player.getUUID();
        ScoreboardCache cache = scoreboardCache.get(playerId);
        long now = System.currentTimeMillis();
        
        // Use cache if valid (within 2 seconds)
        if (cache != null && (now - cache.lastUpdate) < 2000) {
            if (!cache.needsUpdate) {
                return; // No update needed
            }
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        String objectiveName = OBJECTIVE_PREFIX + playerId;
        
        // Create or update objective
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            String title = processPlaceholders(getTitle(player), player);
            Component titleComponent = enableColorCodes ? 
                ColorUtil.colorize(title) : Component.literal(title);
            
            objective = scoreboard.addObjective(
                objectiveName,
                ObjectiveCriteria.DUMMY,
                titleComponent,
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
            );
            
            DebugUtil.debugLog("[ScoreboardManager] Created new objective for " + 
                              player.getName().getString());
        }
        
        // Update title if changed
        String newTitle = processPlaceholders(getTitle(player), player);
        Component newTitleComponent = enableColorCodes ? 
            ColorUtil.colorize(newTitle) : Component.literal(newTitle);
        
        if (!objective.getDisplayName().equals(newTitleComponent)) {
            objective.setDisplayName(newTitleComponent);
        }
        
        // Set display for this player only
        player.connection.send(new ClientboundSetDisplayObjectivePacket(
            DisplaySlot.SIDEBAR, objective
        ));
        
        // Update lines
        updateScoreboardLines(player, objective, scoreboard);
        
        // Update team (prefix/suffix)
        updatePlayerTeam(player, scoreboard);
        
        // Update cache
        ScoreboardCache newCache = new ScoreboardCache();
        newCache.lastUpdate = now;
        newCache.needsUpdate = false;
        scoreboardCache.put(playerId, newCache);
    }
    
    /**
     * Update scoreboard lines with proper score management
     */
    private void updateScoreboardLines(ServerPlayer player, Objective objective, Scoreboard scoreboard) {
        List<String> lines = getLines(player);
        
        // Clear existing scores for this objective
        try {
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(player, objective);
            score.set(lines.size()); // Set total line count as score
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Could not update score: " + e.getMessage());
        }
    }
    
    /**
     * Update player team for prefix/suffix display
     */
    private void updatePlayerTeam(ServerPlayer player, Scoreboard scoreboard) {
        String teamName = TEAM_PREFIX + player.getUUID();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        
        String prefix = processPlaceholders(getPrefix(player), player);
        String suffix = processPlaceholders(getSuffix(player), player);
        
        Component prefixComponent = enableColorCodes ? 
            ColorUtil.colorize(prefix) : Component.literal(prefix);
        Component suffixComponent = enableColorCodes ? 
            ColorUtil.colorize(suffix) : Component.literal(suffix);
        
        team.setPlayerPrefix(prefixComponent);
        team.setPlayerSuffix(suffixComponent);
        
        // Add player to team if not already added
        if (!team.getPlayers().contains(player.getScoreboardName())) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }

    public void cleanupPlayer(ServerPlayer player) {
        if (server == null) return;
        Scoreboard scoreboard = server.getScoreboard();
        String objectiveName = OBJECTIVE_PREFIX + player.getUUID();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective != null) scoreboard.removeObjective(objective);
        String teamName = TEAM_PREFIX + player.getUUID();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team != null) scoreboard.removePlayerTeam(team);
    }

    // PlaceholderManager integration and color code conversion

    // Load group prefix/suffix from groups.json in server root
    private void loadGroupConfig() {
        try {
            String groupConfigPath = "neoessentials/permissions/groups.json";
            Gson gson = new Gson();
            FileReader reader = new FileReader(groupConfigPath);
            java.util.Map<?,?> config = gson.fromJson(reader, java.util.Map.class);
            groupPrefixes.clear();
            groupSuffixes.clear();
            for (Map.Entry<?,?> entry : config.entrySet()) {
                String group = entry.getKey().toString();
                java.util.Map<?,?> groupData = (java.util.Map<?,?>) entry.getValue();
                String prefix = groupData.containsKey("prefix") ? groupData.get("prefix").toString() : "";
                String suffix = groupData.containsKey("suffix") ? groupData.get("suffix").toString() : "";
                groupPrefixes.put(group, prefix);
                groupSuffixes.put(group, suffix);
            }
            reader.close();
        } catch (Exception e) {
            // Fallback: no group config
        }
    }

    // Get title for player/group
    private String getTitle(ServerPlayer player) {
        String group = getGroup(player);
        return groupTitles.getOrDefault(group, groupTitles.get("default"));
    }


    // Get group from permissions (replace with your actual logic)
    private String getGroup(ServerPlayer player) {
        // Use your existing permission system or default
        // Example: get group from CustomPermissionsManager if available
        try {
            Class<?> permMgrClass = Class.forName("com.zerog.neoessentials.permissions.CustomPermissionsManager");
            Object permMgr = permMgrClass.getMethod("getInstance").invoke(null);
            String group = (String) permMgrClass.getMethod("getPlayerGroup", UUID.class).invoke(permMgr, player.getUUID());
            if (group != null && !group.isEmpty()) return group;
        } catch (Exception ignored) {}
        return "default";
    }

    // Get prefix/suffix from permissions (replace with your actual logic)
    private String getPrefix(ServerPlayer player) {
        String group = getGroup(player);
        String prefix = groupPrefixes.getOrDefault(group, "");
        return prefix.replace("&", "§");
    }
    private String getSuffix(ServerPlayer player) {
        String group = getGroup(player);
        String suffix = groupSuffixes.getOrDefault(group, "");
        return suffix.replace("&", "§");
    }

    // Online time calculation
    // Integrate with your existing score system
    public static int getPlayerScore(UUID playerId) {
        // Use your actual score system here
        return 0;
    }

    /**
     * Get lines for player based on their group
     */
    private List<String> getLines(ServerPlayer player) {
        String group = getGroup(player);
        return groupLines.getOrDefault(group, groupLines.get("default"));
    }
    
    /**
     * Integrate with statistics system for player data
     */
    private int getKills(ServerPlayer player) {
        try {
            com.zerog.neoessentials.player.PlayerData playerData = 
                com.zerog.neoessentials.player.PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            Object kills = playerData.getStatistic("player_kills");
            return kills instanceof Number ? ((Number) kills).intValue() : 0;
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error getting kills for " + player.getName().getString() + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get player death count from statistics system
     */
    private int getDeaths(ServerPlayer player) {
        try {
            com.zerog.neoessentials.player.PlayerData playerData = 
                com.zerog.neoessentials.player.PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            Object deaths = playerData.getStatistic("player_deaths");
            return deaths instanceof Number ? ((Number) deaths).intValue() : 0;
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error getting deaths for " + player.getName().getString() + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get blocks broken count
     */
    private int getBlocksBroken(ServerPlayer player) {
        try {
            com.zerog.neoessentials.player.PlayerData playerData = 
                com.zerog.neoessentials.player.PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            Object blocks = playerData.getStatistic("blocks_broken");
            return blocks instanceof Number ? ((Number) blocks).intValue() : 0;
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error getting blocks broken for " + player.getName().getString() + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get blocks placed count
     */
    private int getBlocksPlaced(ServerPlayer player) {
        try {
            com.zerog.neoessentials.player.PlayerData playerData = 
                com.zerog.neoessentials.player.PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            Object blocks = playerData.getStatistic("blocks_placed");
            return blocks instanceof Number ? ((Number) blocks).intValue() : 0;
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error getting blocks placed for " + player.getName().getString() + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get player distance traveled
     */
    private double getDistanceTraveled(ServerPlayer player) {
        try {
            com.zerog.neoessentials.player.PlayerData playerData = 
                com.zerog.neoessentials.player.PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            Object distance = playerData.getStatistic("distance_traveled");
            return distance instanceof Number ? ((Number) distance).doubleValue() : 0.0;
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error getting distance traveled for " + player.getName().getString() + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Process placeholders in text with enhanced statistics integration
     */
    private String processPlaceholders(String text, ServerPlayer player) {
        if (text == null || text.isEmpty()) return "";
        
        try {
            // Use PlaceholderManager if available
            com.zerog.neoessentials.placeholders.PlaceholderManager placeholderMgr = 
                com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
            
            String result = placeholderMgr.processPlaceholders(text, player);
            return result.replace('&', '§');
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error with PlaceholderManager, using fallback: " + e.getMessage());
            return processPlaceholdersFallback(text, player);
        }
    }
    
    /**
     * Fallback placeholder processing
     */
    private String processPlaceholdersFallback(String text, ServerPlayer player) {
        // Replace common placeholders
        String result = text
            .replace("%player%", player.getName().getString())
            .replace("{player_name}", player.getName().getString())
            .replace("%group%", getGroup(player))
            .replace("{ftb_rank_display_name}", getGroup(player))
            .replace("%health%", String.valueOf((int)player.getHealth()))
            .replace("{player_health}", String.valueOf((int)player.getHealth()))
            .replace("%max_health%", String.valueOf((int)player.getMaxHealth()))
            .replace("{player_max_health}", String.valueOf((int)player.getMaxHealth()))
            .replace("%hunger%", String.valueOf(player.getFoodData().getFoodLevel()))
            .replace("{player_food}", String.valueOf(player.getFoodData().getFoodLevel()))
            .replace("%x%", String.valueOf((int)player.getX()))
            .replace("{player_x}", String.valueOf((int)player.getX()))
            .replace("%y%", String.valueOf((int)player.getY()))
            .replace("{player_y}", String.valueOf((int)player.getY()))
            .replace("%z%", String.valueOf((int)player.getZ()))
            .replace("{player_z}", String.valueOf((int)player.getZ()))
            .replace("%world%", player.level().dimension().location().getPath())
            .replace("{world_name}", player.level().dimension().location().getPath())
            .replace("%onlinetime%", formatTime(getOnlineTime(player)))
            .replace("{session_time}", formatTime(getOnlineTime(player)))
            .replace("%kills%", String.valueOf(getKills(player)))
            .replace("{player_kills}", String.valueOf(getKills(player)))
            .replace("%deaths%", String.valueOf(getDeaths(player)))
            .replace("{player_deaths}", String.valueOf(getDeaths(player)))
            .replace("%blocks_broken%", String.valueOf(getBlocksBroken(player)))
            .replace("{blocks_broken}", String.valueOf(getBlocksBroken(player)))
            .replace("%blocks_placed%", String.valueOf(getBlocksPlaced(player)))
            .replace("{blocks_placed}", String.valueOf(getBlocksPlaced(player)))
            .replace("%distance%", String.format("%.1f", getDistanceTraveled(player)))
            .replace("{distance_traveled}", String.format("%.1f", getDistanceTraveled(player)))
            .replace("%kdr%", calculateKDR(player))
            .replace("{kdr}", calculateKDR(player));
        
        // Playtime integration
        try {
            com.zerog.neoessentials.player.PlaytimeTracker playtimeTracker = 
                com.zerog.neoessentials.player.PlaytimeTracker.getInstance();
            long totalPlaytime = playtimeTracker.getTotalPlaytime(player.getUUID());
            long currentSession = playtimeTracker.getCurrentSessionTime(player.getUUID());
            
            result = result
                .replace("%total_playtime%", formatTime(totalPlaytime))
                .replace("{total_playtime}", formatTime(totalPlaytime))
                .replace("%session_time%", formatTime(currentSession))
                .replace("{current_session}", formatTime(currentSession))
                .replace("%playtime_formatted%", playtimeTracker.formatPlaytime(totalPlaytime))
                .replace("{playtime_formatted}", playtimeTracker.formatPlaytime(totalPlaytime));
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error getting playtime data: " + e.getMessage());
        }
        
        // Server-wide placeholders
        if (server != null) {
            result = result
                .replace("%online%", String.valueOf(server.getPlayerCount()))
                .replace("{server_players}", String.valueOf(server.getPlayerCount()))
                .replace("%max%", String.valueOf(server.getMaxPlayers()))
                .replace("{server_max_players}", String.valueOf(server.getMaxPlayers()))
                .replace("%tps%", String.format("%.1f", getCurrentTPS()))
                .replace("{server_tps}", String.format("%.1f", getCurrentTPS()))
                .replace("%uptime%", formatTime(getServerUptime()))
                .replace("{server_uptime}", formatTime(getServerUptime()));
        }
        
        // Time placeholders
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        result = result
            .replace("{time}", now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")))
            .replace("{date}", now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .replace("{datetime}", now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return result;
    }
    
    /**
     * Calculate Kill/Death Ratio
     */
    private String calculateKDR(ServerPlayer player) {
        int kills = getKills(player);
        int deaths = getDeaths(player);
        
        if (deaths == 0) {
            return kills > 0 ? String.valueOf(kills) : "0";
        }
        
        double kdr = (double) kills / deaths;
        return String.format("%.2f", kdr);
    }
    
    /**
     * Format time in human readable format
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Get online time for player
     */
    private long getOnlineTime(ServerPlayer player) {
        Long joinTime = playerJoinTime.get(player.getUUID());
        if (joinTime == null) return 0;
        return System.currentTimeMillis() - joinTime;
    }
    
    /**
     * Get current server TPS (simplified)
     */
    private double getCurrentTPS() {
        if (server == null) return 0.0;
        // This is a simplified TPS calculation
        // In a real implementation, you'd need to track tick times
        return 20.0; // Placeholder
    }
    
    /**
     * Get server uptime
     */
    private long getServerUptime() {
        // This would need to be tracked from server start
        return System.currentTimeMillis(); // Placeholder
    }
    
    /**
     * Shutdown cleanup
     */
    public void shutdown() {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            }
            
            // Clear all player scoreboards
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    cleanupPlayer(player);
                }
            }
            
            // Clear caches
            scoreboardCache.clear();
            playerJoinTime.clear();
            
            DebugUtil.debugLog("[ScoreboardManager] Shutdown complete");
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error during shutdown: " + e.getMessage());
        }
    }
    
    /**
     * Get configuration status
     */
    public String getStatus() {
        return String.format("Scoreboard enabled: %s, Groups: %d, Active players: %d, Cache size: %d", 
                            enableScoreboard, groupTitles.size(), playerJoinTime.size(), scoreboardCache.size());
    }
    
    /**
     * Cache class for scoreboard data
     */
    private static class ScoreboardCache {
        long lastUpdate = 0;
        boolean needsUpdate = true;
    }
    
    /**
     * ScoreboardState class to track individual player's scoreboard state
     */
    public static class ScoreboardState {
        public String lastTitle = "";
        public List<String> lastLines = new ArrayList<>();
    }
    
    /**
     * Get animated title if title animations are enabled
     */
    private String getAnimatedTitle() {
        if (config == null || config.scoreboard == null || config.scoreboard.titleAnimations == null || 
            !config.scoreboard.titleAnimations.enabled) {
            return null;
        }
        
        List<String> frames = config.scoreboard.titleAnimations.frames;
        if (frames == null || frames.isEmpty()) return null;
        
        long currentTime = System.currentTimeMillis();
        double duration = config.scoreboard.titleAnimations.duration * 1000; // Convert to milliseconds
        int frameIndex = (int) ((currentTime / duration) % frames.size());
        
        return frames.get(frameIndex);
    }
    
    /**
     * Process conditional logic in scoreboard lines
     */
    private String processConditionalLogic(String line, ServerPlayer player) {
        if (config == null || config.scoreboard == null || config.scoreboard.conditional_logic == null) return line;
        
        // Look for ${condition_name} patterns
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(line);
        
        String result = line;
        while (matcher.find()) {
            String conditionName = matcher.group(1);
            String conditionLogic = config.scoreboard.conditional_logic.get(conditionName);
            
            if (conditionLogic != null) {
                boolean conditionMet = evaluateCondition(conditionLogic, player);
                // For now, replace with indicator. This could be expanded to show/hide entire lines
                String replacement = conditionMet ? "&a✓" : "&c✗";
                result = result.replace("${" + conditionName + "}", replacement);
            }
        }
        
        return result;
    }
    
    /**
     * Evaluate conditional logic expressions
     */
    private boolean evaluateCondition(String condition, ServerPlayer player) {
        try {
            // Handle permission checks
            if (condition.startsWith("permission:")) {
                String permission = condition.substring(11);
                return hasPermissionMethod(player, permission);
            }
            
            // Handle health checks
            if (condition.contains("health")) {
                float health = player.getHealth();
                if (condition.contains("< 6")) {
                    return health < 6.0f;
                } else if (condition.contains("> 15")) {
                    return health > 15.0f;
                }
            }
            
            // Handle food checks
            if (condition.contains("food")) {
                int food = player.getFoodData().getFoodLevel();
                if (condition.contains("< 6")) {
                    return food < 6;
                } else if (condition.contains("> 15")) {
                    return food > 15;
                }
            }
            
            // Handle FTB checks
            if (condition.contains("ftb_has_team == true")) {
                String teamPlaceholder = processPlaceholders("{ftb_has_team}", player);
                return "true".equals(teamPlaceholder);
            }
            
            if (condition.contains("ftb_has_rank == true")) {
                String rankPlaceholder = processPlaceholders("{ftb_has_rank}", player);
                return "true".equals(rankPlaceholder);
            }
            
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error evaluating condition '" + condition + "': " + e.getMessage());
        }
        
        return false; // Default to false for safety
    }
    
    /**
     * Process animations in scoreboard lines
     */
    private String processAnimations(String line, ServerPlayer player) {
        if (config == null || config.scoreboard == null || config.scoreboard.animations == null || 
            !config.scoreboard.animations.enabled) {
            return line;
        }
        
        // Look for animation placeholders like {animation:health_indicator}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{animation:([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(line);
        
        String result = line;
        while (matcher.find()) {
            String animationId = matcher.group(1);
            String animationFrame = getAnimationFrame(animationId, player);
            if (animationFrame != null) {
                result = result.replace("{animation:" + animationId + "}", animationFrame);
            }
        }
        
        return result;
    }
    
    /**
     * Get current frame for an animation sequence
     */
    private String getAnimationFrame(String animationId, ServerPlayer player) {
        if (config == null || config.scoreboard == null || config.scoreboard.animations == null || 
            config.scoreboard.animations.sequences == null) return null;
        
        for (com.zerog.neoessentials.config.TablistConfig.AnimationSequence sequence : config.scoreboard.animations.sequences) {
            if (animationId.equals(sequence.id)) {
                // Check if condition is met (if any)
                if (sequence.id.equals("health_indicator")) {
                    if (player.getHealth() >= 6.0f) continue; // Only show when health is low
                }
                
                List<String> frames = sequence.frames;
                if (frames == null || frames.isEmpty()) continue;
                
                long currentTime = System.currentTimeMillis();
                double duration = sequence.duration * 1000; // Convert to milliseconds
                int frameIndex = (int) ((currentTime / duration) % frames.size());
                
                return frames.get(frameIndex);
            }
        }
        
        return null;
    }
    
    /**
     * Send unified scoreboard update to player
     */
    private void sendScoreboardUpdateUnified(ServerPlayer player, String title, List<String> lines) {
        try {
            Scoreboard scoreboard = player.getScoreboard();
            String objectiveName = "neoessentials_sb";
            
            // Remove existing objective
            Objective existingObjective = scoreboard.getObjective(objectiveName);
            if (existingObjective != null) {
                scoreboard.removeObjective(existingObjective);
            }
            
            // Create new objective
            Objective objective = scoreboard.addObjective(objectiveName, 
                ObjectiveCriteria.DUMMY, Component.literal(title.replace('&', '§')), 
                ObjectiveCriteria.RenderType.INTEGER, true, null);
            
            // Set display slot
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            
            // Add lines (in reverse order for proper display)
            for (int i = 0; i < Math.min(lines.size(), 15); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) {
                    // Empty line - use spaces with different counts
                    line = " ".repeat(i + 1);
                }
                
                String entryName = "§" + i + "§r" + line;
                // Use a dummy score holder for the scoreboard entry
                net.minecraft.world.scores.ScoreHolder scoreHolder = net.minecraft.world.scores.ScoreHolder.forNameOnly(entryName);
                ScoreAccess score = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);
                score.set(lines.size() - i);
            }
            
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error sending unified scoreboard update: " + e.getMessage());
        }
    }
    
    /**
     * Check if player has permission (wrapper method)
     */
    private boolean hasPermissionMethod(ServerPlayer player, String permission) {
        try {
            com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = 
                com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
            return permMgr.hasPermission(player, permission);
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error checking permission: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update scoreboard with Discord integration
     */
    public void updateScoreboardWithDiscordNotification(ServerPlayer player, String reason) {
        try {
            // Update the scoreboard
            updateScoreboard(player);

            // Notify Discord integration
            com.zerog.neoessentials.integration.DiscordIntegrationManager discordMgr = 
                com.zerog.neoessentials.integration.DiscordIntegrationManager.getInstance();
            
            if (discordMgr.isEnabled()) {
                Map<String, Object> data = new HashMap<>();
                data.put("layout_name", getCurrentLayoutName(player));
                data.put("update_type", reason);
                data.put("animation_active", hasActiveAnimations());
                discordMgr.sendEnrichedNotification("scoreboard_update", player, data);
            }

        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error updating scoreboard with Discord notification: " + e.getMessage());
        }
    }

    /**
     * Get current layout name for a player
     */
    private String getCurrentLayoutName(ServerPlayer player) {
        try {
            // Get the active layout based on player data
            return "unified_scoreboard";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Check if there are active animations
     */
    private boolean hasActiveAnimations() {
        // Check if any animation tasks are active
        return false; // Simplified for now
    }

    /**
     * Notify Discord of scoreboard changes for all players
     */
    public void notifyDiscordScoreboardUpdate(String reason) {
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            com.zerog.neoessentials.integration.DiscordIntegrationManager discordMgr = 
                com.zerog.neoessentials.integration.DiscordIntegrationManager.getInstance();
            
            if (discordMgr.isEnabled()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    updateScoreboardWithDiscordNotification(player, reason);
                }
            }

        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error notifying Discord of scoreboard updates: " + e.getMessage());
        }
    }
    
    /**
     * Get singleton instance
     */
    public static ScoreboardManager getInstance() {
        return instance;
    }
}