package com.zerog.neoessentials.features;

import com.zerog.neoessentials.util.DebugUtil;
import com.zerog.neoessentials.util.ColorUtil;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.zerog.neoessentials.NeoEssentials;
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
    private boolean enableAnimations = true;
    private boolean enableColorCodes = true;

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
     * Enhanced config loading with better error handling and defaults
     */
    private void loadConfig() {
        try {
            String configPath = "config/neoessentials/scoreboard.json";
            Gson gson = new Gson();
            
            try (FileReader reader = new FileReader(configPath)) {
                Map<?, ?> config = gson.fromJson(reader, Map.class);
                
                // Load main settings
                if (config.containsKey("enableScoreboard")) {
                    Object val = config.get("enableScoreboard");
                    enableScoreboard = val instanceof Boolean ? (Boolean) val : 
                                     Boolean.parseBoolean(val.toString());
                }
                
                if (config.containsKey("enableAnimations")) {
                    Object val = config.get("enableAnimations");
                    enableAnimations = val instanceof Boolean ? (Boolean) val : 
                                     Boolean.parseBoolean(val.toString());
                }
                
                if (config.containsKey("enableColorCodes")) {
                    Object val = config.get("enableColorCodes");
                    enableColorCodes = val instanceof Boolean ? (Boolean) val : 
                                     Boolean.parseBoolean(val.toString());
                }
                
                // Load group configurations
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
                
                DebugUtil.debugLog("[ScoreboardManager] Config loaded successfully - Groups: " + 
                                  groupTitles.size() + ", Enabled: " + enableScoreboard);
            }
        } catch (Exception e) {
            DebugUtil.warnLog("[ScoreboardManager] Error loading config, using defaults: " + e.getMessage());
            loadDefaultConfig();
        }
    }
    
    /**
     * Load default configuration when config file is missing or invalid
     */
    private void loadDefaultConfig() {
        enableScoreboard = true;
        enableAnimations = true;
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
     * Enhanced scoreboard update with caching and performance optimizations
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
            newCache.lastTitle = newTitle;
            scoreboardCache.put(playerId, newCache);
            
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error updating scoreboard for " + 
                              player.getName().getString() + ": " + e.getMessage());
        }
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
        
        // Replace common placeholders
        String result = text
            .replace("%player%", player.getName().getString())
            .replace("%group%", getGroup(player))
            .replace("%health%", String.valueOf((int)player.getHealth()))
            .replace("%max_health%", String.valueOf((int)player.getMaxHealth()))
            .replace("%hunger%", String.valueOf(player.getFoodData().getFoodLevel()))
            .replace("%x%", String.valueOf((int)player.getX()))
            .replace("%y%", String.valueOf((int)player.getY()))
            .replace("%z%", String.valueOf((int)player.getZ()))
            .replace("%world%", player.level().dimension().location().getPath())
            .replace("%onlinetime%", formatTime(getOnlineTime(player)))
            .replace("%kills%", String.valueOf(getKills(player)))
            .replace("%deaths%", String.valueOf(getDeaths(player)))
            .replace("%blocks_broken%", String.valueOf(getBlocksBroken(player)))
            .replace("%blocks_placed%", String.valueOf(getBlocksPlaced(player)))
            .replace("%distance%", String.format("%.1f", getDistanceTraveled(player)))
            .replace("%kdr%", calculateKDR(player));
        
        // Playtime integration
        try {
            com.zerog.neoessentials.player.PlaytimeTracker playtimeTracker = 
                com.zerog.neoessentials.player.PlaytimeTracker.getInstance();
            long totalPlaytime = playtimeTracker.getTotalPlaytime(player.getUUID());
            long currentSession = playtimeTracker.getCurrentSessionTime(player.getUUID());
            
            result = result
                .replace("%total_playtime%", formatTime(totalPlaytime))
                .replace("%session_time%", formatTime(currentSession))
                .replace("%playtime_formatted%", playtimeTracker.formatPlaytime(totalPlaytime));
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error getting playtime data: " + e.getMessage());
        }
        
        // Server-wide placeholders
        if (server != null) {
            result = result
                .replace("%online%", String.valueOf(server.getPlayerCount()))
                .replace("%max%", String.valueOf(server.getMaxPlayers()))
                .replace("%tps%", String.format("%.1f", getCurrentTPS()))
                .replace("%uptime%", formatTime(getServerUptime()));
        }
        
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
        String lastTitle = "";
        List<String> lastLines = new ArrayList<>();
    }
    
    /**
     * Get singleton instance
     */
    public static ScoreboardManager getInstance() {
        return instance;
    }
}