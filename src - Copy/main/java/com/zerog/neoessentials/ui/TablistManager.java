package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistConfig;
import com.zerog.neoessentials.config.TablistTomlConfig;
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
    private TablistConfig legacyConfig;
    private TablistTomlConfig tomlConfig;
    
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
        
        // Get both configs
        this.legacyConfig = NeoEssentials.getInstance().getConfigManager().getTablistConfig();
        // The TOML config is accessed via its static fields directly
        
        if (this.legacyConfig == null) {
            NeoEssentials.LOGGER.warn("Legacy tablist config not found, using defaults");
            this.legacyConfig = new TablistConfig();
        }
        
        // Load headers and footers
        loadHeadersAndFooters();
        
        // Start the update task with interval from TOML config
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
        
        // Load from config or use defaults
        if (legacyConfig.getHeaders() != null && !legacyConfig.getHeaders().isEmpty()) {
            headers.addAll(legacyConfig.getHeaders());
        } else {
            // Default headers
            headers.add("&6Welcome to &l%server_name%");
            headers.add("&ePlayers Online: &a%online_players%&e/&a%max_players%");
            headers.add("&bServer TPS: &a%server_tps%");
        }
        
        if (legacyConfig.getFooters() != null && !legacyConfig.getFooters().isEmpty()) {
            footers.addAll(legacyConfig.getFooters());
        } else {
            // Default footers
            footers.add("&7Website: &fwww.example.com");
            footers.add("&7Discord: &fdiscord.gg/example");
            footers.add("&7Current Time: &f%time%");
        }
    }
    
    /**
     * Start the scheduled update task for the tablist
     * 
     * @param updateInterval Interval in milliseconds
     */
    private void startUpdateTask(long updateInterval) {
        // Cancel existing task if running
        if (updateTask != null && !updateTask.isDone()) {
            updateTask.cancel(false);
        }
          // Schedule new task
        updateTask = scheduler.scheduleAtFixedRate(this::updateTablist, 
            0, updateInterval, TimeUnit.MILLISECONDS);
        
        NeoEssentials.LOGGER.info("TablistManager update task scheduled every {} ms", updateInterval);
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
            if (legacyConfig.isEnableSorting()) {
                updatePlayerTeams();
            }
            
            // Update header and footer for all players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                try {
                    // Generate player-specific header and footer if needed
                    Component playerHeader = legacyConfig.isEnablePlayerSpecificHeaders() ? 
                        Component.literal(TextUtil.translateColors(parsePlaceholders(
                            headers.get(currentHeaderIndex), player))) : 
                        header;
                            
                    Component playerFooter = legacyConfig.isEnablePlayerSpecificFooters() ? 
                        Component.literal(TextUtil.translateColors(parsePlaceholders(
                            footers.get(currentFooterIndex), player))) : 
                        footer;
                            
                    // Send the header and footer packet
                    player.connection.send(new ClientboundTabListPacket(playerHeader, playerFooter));
                } catch (Exception e) {
                    NeoEssentials.LOGGER.error("Error sending tablist packet to player " + player.getScoreboardName(), e);
                }
                
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
     */    
    private Component getFormattedHeader() {
        String template = !headers.isEmpty() ? 
                headers.get(currentHeaderIndex) : 
                "&6Welcome to the server!";
        
        return Component.literal(TextUtil.translateColors(parsePlaceholders(template, null)));
    }
    
    /**
     * Gets the current formatted footer
     *
     * @return The formatted footer component
     */    
    private Component getFormattedFooter() {
        String template = !footers.isEmpty() ? 
                footers.get(currentFooterIndex) : 
                "&7Thank you for playing!";
        
        return Component.literal(TextUtil.translateColors(parsePlaceholders(template, null)));
    }
    
    /**
     * Parses placeholders in a string
     *
     * @param template The template string with placeholders
     * @param player Optional player for player-specific placeholders
     * @return The parsed string with placeholders replaced
     */
    private String parsePlaceholders(String template, ServerPlayer player) {
        MinecraftServer server = NeoEssentials.getInstance().getServer();
        if (server == null) return template;
        
        // Get economy manager for placeholders
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Server placeholders
        template = template.replace("%server_name%", legacyConfig.getServerName());
        template = template.replace("%online_players%", String.valueOf(server.getPlayerCount()));
        template = template.replace("%max_players%", String.valueOf(server.getMaxPlayers()));
        
        // Server version info
        template = template.replace("%mc_version%", net.minecraft.SharedConstants.getCurrentVersion().getName());
        template = template.replace("%mod_version%", NeoEssentials.getInstance().getVersion());
        
        // Time placeholders
        SimpleDateFormat timeFormat = new SimpleDateFormat(legacyConfig.getTimeFormat());
        template = template.replace("%time%", timeFormat.format(new Date()));
        
        // Player-specific placeholders
        if (player != null) {
            template = template.replace("%player_name%", player.getScoreboardName());
            template = template.replace("%player_displayname%", player.getName().getString());
            template = template.replace("%player_uuid%", player.getUUID().toString());
            
            // Player ping
            int ping = 0;
            try {
                // In 1.21.1, latency is a field in the packet listener impl
                java.lang.reflect.Field field = player.connection.getClass().getDeclaredField("latency");
                field.setAccessible(true);
                ping = field.getInt(player.connection);
            } catch (Exception e) {
                // If we can't access the field, just use 0
                ping = 0;
            }
            template = template.replace("%ping%", String.valueOf(ping));
            
            // Player health
            template = template.replace("%health%", String.format("%.1f", player.getHealth()));
            template = template.replace("%max_health%", String.format("%.1f", player.getMaxHealth()));
            
            // Player coordinates
            template = template.replace("%x%", String.format("%.1f", player.getX()));
            template = template.replace("%y%", String.format("%.1f", player.getY()));
            template = template.replace("%z%", String.format("%.1f", player.getZ()));
            
            // World information
            template = template.replace("%dimension%", player.level().dimension().location().toString());
            
            // Economy placeholder for specific player
            if (economyManager != null) {
                double balance = economyManager.getBalance(player.getUUID());
                template = template.replace("%balance%", String.format("%.2f", balance));
            }
        }
        
        // Default to 20.0 TPS
        double tps = 20.0;
        
        // Get MSPT and calculate TPS safely
        try {
            // NeoForge 1.21.1 offers server ticktime stats
            double mspt = server.getAverageTickTimeNanos() / 1000000.0;
            if (mspt > 0) {
                // Calculate TPS (1000 ms / mspt, cap at 20)
                tps = Math.min(20.0, 1000.0 / mspt);
            }
        } catch (Exception e) {
            // If any error occurs, just use the default
            tps = 20.0;
        }
        
        template = template.replace("%server_tps%", String.format("%.1f", tps));
        
        // Economy global placeholders
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
        
        switch (legacyConfig.getSortType()) {
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
        if (legacyConfig.isShowEconomyInTablist()) {
            EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            if (economyManager != null) {
                double balance = economyManager.getBalance(player.getUUID());
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
        if (legacyConfig.isEnableSorting()) {
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
        if (legacyConfig.isEnableSorting()) {
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
        // These fields must be kept even if not directly accessed - they're needed for identity
        @SuppressWarnings("unused")
        private final String name;
        @SuppressWarnings("unused")
        private final UUID uuid;
        private final long joinTime;
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
