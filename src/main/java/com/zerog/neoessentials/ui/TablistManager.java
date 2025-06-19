package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistConfig;
import com.zerog.neoessentials.data.EconomyManager;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.TextUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages custom tablist headers, footers, and player ordering/formatting.
 */
public class TablistManager {
    // Configuration
    private TablistConfig config;
    
    // Scheduler for updating the tablist
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;
    
    // Cached player data for performance
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    
    // Header and footer rotation
    private final List<String> headers = new ArrayList<>();
    private final List<String> footers = new ArrayList<>();
    private int currentHeaderIndex = 0;
    private int currentFooterIndex = 0;
    
    // Team prefixes and suffixes for player sorting
    private final Map<String, PlayerTeam> teams = new HashMap<>();
    
    /**
     * Creates a new TablistManager
     * 
     * @param scheduler The scheduler to use for tablist updates
     */
    public TablistManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
    
    /**
     * Initializes the tablist manager with default settings
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing TablistManager");
        
        // Set default config
        this.config = NeoEssentials.getInstance().getConfigManager().getTablistConfig();
        if (this.config == null) {
            NeoEssentials.LOGGER.warn("Tablist config not found, using defaults");
            this.config = new TablistConfig();
        }
        
        // Load headers and footers
        loadHeadersAndFooters();
        
        // Start the update task
        startUpdateTask();
    }
    
    /**
     * Loads header and footer templates from config
     */
    private void loadHeadersAndFooters() {
        // Clear existing headers and footers
        headers.clear();
        footers.clear();
        
        // Load from config or use defaults
        if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
            headers.addAll(config.getHeaders());
        } else {
            // Default headers
            headers.add("&6Welcome to &l%server_name%");
            headers.add("&ePlayers Online: &a%online_players%&e/&a%max_players%");
            headers.add("&bServer TPS: &a%server_tps%");
        }
        
        if (config.getFooters() != null && !config.getFooters().isEmpty()) {
            footers.addAll(config.getFooters());
        } else {
            // Default footers
            footers.add("&7Website: &fwww.example.com");
            footers.add("&7Discord: &fdiscord.gg/example");
            footers.add("&7Current Time: &f%time%");
        }
    }
    
    /**
     * Starts the scheduled task to update the tablist
     */
    private void startUpdateTask() {
        // Cancel any existing task
        if (updateTask != null && !updateTask.isDone()) {
            updateTask.cancel(false);
        }
        
        // Start a new update task
        long updateInterval = config.getUpdateInterval();
        updateTask = scheduler.scheduleAtFixedRate(
                this::updateTablist,
                0,
                updateInterval,
                TimeUnit.MILLISECONDS);
        
        NeoEssentials.LOGGER.info("Started tablist update task with interval: {}ms", updateInterval);
    }
    
    /**
     * Updates the tablist for all online players
     */
    public void updateTablist() {
        try {
            MinecraftServer server = NeoEssentials.getInstance().getServer();
            if (server == null) return;
            
            // Rotate header and footer indices
            rotateHeaderAndFooter();
            
            // Get the current header and footer
            Component header = getFormattedHeader();
            Component footer = getFormattedFooter();
            
            // Update player teams and sorting if enabled
            if (config.isEnableSorting()) {
                updatePlayerTeams();
            }
            
            // Update header and footer for all players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // Send the header and footer packet
                player.connection.send(new ClientboundTabListPacket(header, footer));
                
                // Add player to cache if not already present
                if (!playerDataCache.containsKey(player.getUUID())) {
                    PlayerData data = new PlayerData(player.getScoreboardName(), player.getUUID());
                    playerDataCache.put(player.getUUID(), data);
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error updating tablist", e);
        }
    }
    
    /**
     * Rotates the header and footer indices for animation
     */
    private void rotateHeaderAndFooter() {
        if (!headers.isEmpty()) {
            currentHeaderIndex = (currentHeaderIndex + 1) % headers.size();
        }
        
        if (!footers.isEmpty()) {
            currentFooterIndex = (currentFooterIndex + 1) % footers.size();
        }
    }
    
    /**
     * Gets the current formatted header
     *
     * @return The formatted header component
     */    private Component getFormattedHeader() {
        String template = !headers.isEmpty() ? 
                headers.get(currentHeaderIndex) : 
                "&6Welcome to the server!";
        
        return Component.literal(TextUtil.translateColors(parsePlaceholders(template)));
    }
    
    /**
     * Gets the current formatted footer
     *
     * @return The formatted footer component
     */    private Component getFormattedFooter() {
        String template = !footers.isEmpty() ? 
                footers.get(currentFooterIndex) : 
                "&7Thank you for playing!";
        
        return Component.literal(TextUtil.translateColors(parsePlaceholders(template)));
    }
    
    /**
     * Parses placeholders in a string
     *
     * @param template The template string with placeholders
     * @return The parsed string with placeholders replaced
     */
    private String parsePlaceholders(String template) {
        MinecraftServer server = NeoEssentials.getInstance().getServer();
        if (server == null) return template;
        
        // Server placeholders
        template = template.replace("%server_name%", config.getServerName());
        template = template.replace("%online_players%", String.valueOf(server.getPlayerCount()));
        template = template.replace("%max_players%", String.valueOf(server.getMaxPlayers()));
        
        // Time placeholders
        SimpleDateFormat timeFormat = new SimpleDateFormat(config.getTimeFormat());
        template = template.replace("%time%", timeFormat.format(new Date()));        // TPS placeholders - Calculate approximated TPS
        double tps = 20.0; // Default to 20 TPS
        try {
            // We'll use a simple calculation since getAverageTickTime is not directly available in 1.21.1
            long[] tickTimes = (long[]) server.getClass().getMethod("getTickTime").invoke(server);
            if (tickTimes != null && tickTimes.length > 0) {
                double meanTickTime = 0;
                for (long time : tickTimes) {
                    meanTickTime += time;
                }
                meanTickTime /= tickTimes.length;
                meanTickTime /= 1000000; // Convert to ms
                tps = Math.min(20.0, 1000.0 / Math.max(50.0, meanTickTime));
            }
        } catch (Exception e) {
            // Fallback to default TPS if reflection fails
            tps = 20.0;
        }
        template = template.replace("%server_tps%", String.format("%.1f", tps));
        
        // Economy placeholder
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        if (economyManager != null) {
            template = template.replace("%economy_total%", String.format("%.2f", economyManager.getTotalCurrency()));
            template = template.replace("%economy_accounts%", String.valueOf(economyManager.getTotalAccounts()));
        }
        
        return template;
    }
    
    /**
     * Updates player teams for sorting in the tablist
     */
    private void updatePlayerTeams() {
        MinecraftServer server = NeoEssentials.getInstance().getServer();
        if (server == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        
        // Get player sort order based on config
        List<ServerPlayer> sortedPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        
        switch (config.getSortType()) {
            case "name":
                sortedPlayers.sort(Comparator.comparing(ServerPlayer::getScoreboardName));
                break;
            case "rank":
                // Group by ranks (implemented in getRankWeight)
                sortedPlayers.sort(Comparator.comparing(player -> getRankWeight(player)));
                break;
            case "playtime":
                // Sort by playtime if tracked
                sortedPlayers.sort(Comparator.comparing(player -> 
                    playerDataCache.getOrDefault(player.getUUID(), new PlayerData(player.getScoreboardName(), player.getUUID())).getPlaytime()));
                break;
            default:
                // Default to name sorting
                sortedPlayers.sort(Comparator.comparing(ServerPlayer::getScoreboardName));
        }
        
        // Create or update teams for each player
        int order = 0;
        for (ServerPlayer player : sortedPlayers) {
            String teamName = String.format("tab%03d", order); // Format: tab000, tab001, etc.
            
            // Get or create team for this position
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
            }
            
            // Set prefix and suffix based on player's rank or config
            String prefix = getPlayerPrefix(player);
            String suffix = getPlayerSuffix(player);
              team.setPlayerPrefix(Component.literal(TextUtil.translateColors(prefix)));
            team.setPlayerSuffix(Component.literal(TextUtil.translateColors(suffix)));
            
            // Add player to team if not already in it
            if (!team.getPlayers().contains(player.getScoreboardName())) {
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
            }
            
            // Track the team
            teams.put(teamName, team);
            order++;
        }
    }
    
    /**
     * Gets the weight of a player's rank for sorting
     *
     * @param player The player
     * @return The weight of the player's rank
     */
    private int getRankWeight(ServerPlayer player) {
        // Try to get weight from LuckPerms if available
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = luckPermsClass.getMethod("get").invoke(null);
            
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass().getMethod("getUser", UUID.class)
                .invoke(userManager, player.getUUID());
            
            if (user != null) {
                // Try to get primary group
                Object metaData = user.getClass().getMethod("getCachedData").invoke(user);
                metaData = metaData.getClass().getMethod("getMetaData").invoke(metaData);
                
                // Get weight of primary group
                Integer weight = (Integer) metaData.getClass().getMethod("getWeight").invoke(metaData);
                return weight != null ? weight : 0;
            }
        } catch (Exception e) {
            // LuckPerms not present or error occurred
        }
        
        // Default weight based on op status
        return player.hasPermissions(2) ? 100 : 0;
    }
    
    /**
     * Gets the prefix for a player
     *
     * @param player The player
     * @return The prefix string
     */
    private String getPlayerPrefix(ServerPlayer player) {
        // Try to get from LuckPerms
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = luckPermsClass.getMethod("get").invoke(null);
            
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass().getMethod("getUser", UUID.class)
                .invoke(userManager, player.getUUID());
            
            if (user != null) {
                // Try to get prefix
                Object metaData = user.getClass().getMethod("getCachedData").invoke(user);
                metaData = metaData.getClass().getMethod("getMetaData").invoke(metaData);
                
                String prefix = (String) metaData.getClass().getMethod("getPrefix").invoke(metaData);
                if (prefix != null && !prefix.isEmpty()) {
                    return prefix + " ";
                }
            }
        } catch (Exception e) {
            // LuckPerms not present or error occurred
        }
        
        // Return default prefix based on op status
        return player.hasPermissions(2) ? "&c[Admin] " : "&7";
    }
    
    /**
     * Gets the suffix for a player
     *
     * @param player The player
     * @return The suffix string
     */
    private String getPlayerSuffix(ServerPlayer player) {
        // Try to get from LuckPerms
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = luckPermsClass.getMethod("get").invoke(null);
            
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass().getMethod("getUser", UUID.class)
                .invoke(userManager, player.getUUID());
            
            if (user != null) {
                // Try to get suffix
                Object metaData = user.getClass().getMethod("getCachedData").invoke(user);
                metaData = metaData.getClass().getMethod("getMetaData").invoke(metaData);
                
                String suffix = (String) metaData.getClass().getMethod("getSuffix").invoke(metaData);
                if (suffix != null && !suffix.isEmpty()) {
                    return " " + suffix;
                }
            }
        } catch (Exception e) {
            // LuckPerms not present or error occurred
        }
        
        // Add economy info if enabled
        if (config.isShowEconomyInTablist()) {
            EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            if (economyManager != null) {
                double balance = economyManager.getPlayerBalance(player.getUUID());
                return " &6" + String.format("%.2f", balance);
            }
        }
        
        return "";
    }
    
    /**
     * Called when a player joins to initialize their tablist
     *
     * @param player The player who joined
     */
    public void onPlayerJoin(ServerPlayer player) {
        // Add player to cache
        if (!playerDataCache.containsKey(player.getUUID())) {
            PlayerData data = new PlayerData(player.getScoreboardName(), player.getUUID());
            playerDataCache.put(player.getUUID(), data);
        }
        
        // Send initial header and footer
        Component header = getFormattedHeader();
        Component footer = getFormattedFooter();
        player.connection.send(new ClientboundTabListPacket(header, footer));
        
        // Update teams for sorting
        if (config.isEnableSorting()) {
            updatePlayerTeams();
        }
    }
    
    /**
     * Called when a player leaves to clean up
     *
     * @param player The player who left
     */
    public void onPlayerLeave(ServerPlayer player) {
        // Remove from teams
        MinecraftServer server = NeoEssentials.getInstance().getServer();
        if (server != null) {
            Scoreboard scoreboard = server.getScoreboard();
            scoreboard.removePlayerFromTeam(player.getScoreboardName());
        }
        
        // Update teams for sorting
        if (config.isEnableSorting()) {
            updatePlayerTeams();
        }
    }
    
    /**
     * Shutdown the tablist manager
     */
    public void shutdown() {
        // Cancel update task
        if (updateTask != null && !updateTask.isDone()) {
            updateTask.cancel(false);
        }
        
        // Clear teams
        MinecraftServer server = NeoEssentials.getInstance().getServer();
        if (server != null) {
            Scoreboard scoreboard = server.getScoreboard();
            teams.values().forEach(team -> {
                if (scoreboard.getPlayerTeam(team.getName()) != null) {
                    scoreboard.removePlayerTeam(team);
                }
            });
        }
        
        // Clear caches
        teams.clear();
        playerDataCache.clear();
    }
    
    /**
     * Class to store cached player data
     */
    private static class PlayerData {
        private String name;
        private UUID uuid;
        private long joinTime;
        private long playtime;
        
        public PlayerData(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
            this.joinTime = System.currentTimeMillis();
            this.playtime = 0;
        }
        
        public long getPlaytime() {
            return playtime + (System.currentTimeMillis() - joinTime);
        }
    }
}
