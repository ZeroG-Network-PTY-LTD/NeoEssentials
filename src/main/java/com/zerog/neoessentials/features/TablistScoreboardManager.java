package com.zerog.neoessentials.features;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Tablist and Scoreboard Manager for NeoEssentials
 * Provides advanced player list display, multiple themes, and custom scoreboards
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TablistScoreboardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TablistScoreboardManager.class);
    private static TablistScoreboardManager instance;
    
    private MinecraftServer server;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Timer updateTimer = new Timer("TablistScoreboardUpdater", true);
    private boolean updateTaskStarted = false;
    
    private static final String SIDEBAR_OBJECTIVE = "neoessentials_sidebar";
    
    // Enhanced customization features
    private final Map<String, TablistTheme> themes = new ConcurrentHashMap<>();
    private final Map<String, ScoreboardTheme> scoreboardThemes = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTablistThemes = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerScoreboardThemes = new ConcurrentHashMap<>();
    private int animationFrame = 0;
    
    private TablistScoreboardManager() {
        NeoForge.EVENT_BUS.register(this);
        initializeThemes();
    }
    
    public static TablistScoreboardManager getInstance() {
        if (instance == null) {
            instance = new TablistScoreboardManager();
        }
        return instance;
    }
    
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();
        LOGGER.info("TablistScoreboardManager initialized with server instance");
        setupScoreboards();
        startUpdateTask();
    }
    
    /**
     * Manually set the server instance (fallback method)
     */
    public void setServer(MinecraftServer server) {
        if (this.server == null && server != null) {
            this.server = server;
            LOGGER.info("Server instance manually set for TablistScoreboardManager");
            setupScoreboards();
            startUpdateTask();
        }
    }
    
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                // Initialize server reference if not set yet
                if (this.server == null && player.getServer() != null) {
                    this.server = player.getServer();
                    LOGGER.info("Server instance set from player join event");
                    setupScoreboards();
                    startUpdateTask();
                }
                
                initializePlayerStats(player);
                updatePlayerTablist(player);
                updatePlayerScoreboard(player);
                LOGGER.debug("Initialized tablist/scoreboard for player: {}", player.getDisplayName().getString());
            } catch (Exception e) {
                LOGGER.error("Failed to initialize tablist/scoreboard for player: {}", player.getDisplayName().getString(), e);
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerStats.remove(player.getUUID());
            LOGGER.debug("Cleaned up player data for: {}", player.getDisplayName().getString());
        }
    }
    
    /**
     * Setup custom scoreboards
     */
    private void setupScoreboards() {
        if (server == null) return;
        
        try {
            Scoreboard scoreboard = server.getScoreboard();
            
            // Create sidebar objective if it doesn't exist
            Objective sidebarObjective = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
            if (sidebarObjective == null) {
                sidebarObjective = scoreboard.addObjective(
                    SIDEBAR_OBJECTIVE,
                    ObjectiveCriteria.DUMMY,
                    Component.literal("§6§lNeoEssentials§r"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
                );
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, sidebarObjective);
            }
            
            LOGGER.info("Scoreboards setup completed");
        } catch (Exception e) {
            LOGGER.error("Failed to setup scoreboards", e);
        }
    }
    
    /**
     * Initialize player statistics
     */
    private void initializePlayerStats(ServerPlayer player) {
        PlayerStats stats = new PlayerStats();
        stats.joinTime = System.currentTimeMillis();
        playerStats.put(player.getUUID(), stats);
    }
    
    /**
     * Update player tablist display with themes
     */
    public void updatePlayerTablist(ServerPlayer player) {
        try {
            // Get server instance from player if our reference is null
            MinecraftServer serverInstance = this.server != null ? this.server : player.getServer();
            if (serverInstance == null) {
                LOGGER.warn("Cannot update tablist for player {} - server instance is null", player.getDisplayName().getString());
                return;
            }
            
            // Get player's theme or default
            String themeName = playerTablistThemes.getOrDefault(player.getUUID(), "default");
            TablistTheme theme = themes.get(themeName);
            if (theme == null) {
                theme = themes.get("default");
            }
            
            // Calculate animation frame for headers/footers
            int headerIndex = animationFrame % theme.headers.size();
            int footerIndex = animationFrame % theme.footers.size();
            
            // Process header with placeholders
            String headerText = processPlaceholders(theme.headers.get(headerIndex), player);
            MutableComponent header = Component.literal(headerText);
            
            // Process footer with placeholders
            String footerText = processPlaceholders(theme.footers.get(footerIndex), player);
            MutableComponent footer = Component.literal(footerText);
            
            // Send tablist update
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
            
        } catch (Exception e) {
            LOGGER.error("Failed to update tablist for player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Update player scoreboard with themes
     */
    public void updatePlayerScoreboard(ServerPlayer player) {
        try {
            // Get server instance from player if our reference is null
            MinecraftServer serverInstance = this.server != null ? this.server : player.getServer();
            if (serverInstance == null) {
                LOGGER.warn("Cannot update scoreboard for player {} - server instance is null", player.getDisplayName().getString());
                return;
            }
            
            // Get player's scoreboard theme or default
            String themeName = playerScoreboardThemes.getOrDefault(player.getUUID(), "serverinfo");
            ScoreboardTheme theme = scoreboardThemes.get(themeName);
            if (theme == null) {
                theme = scoreboardThemes.get("serverinfo");
            }
            
            Scoreboard scoreboard = serverInstance.getScoreboard();
            Objective objective = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
            if (objective == null) {
                objective = scoreboard.addObjective(
                    SIDEBAR_OBJECTIVE,
                    ObjectiveCriteria.DUMMY,
                    Component.literal(processPlaceholders(theme.title, player)),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
                );
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            } else {
                // Update title
                objective.setDisplayName(Component.literal(processPlaceholders(theme.title, player)));
            }
            
            // Update scoreboard lines with theme
            for (int i = 0; i < theme.lines.size(); i++) {
                String line = theme.lines.get(i);
                String processedLine = processPlaceholders(line, player);
                updateScoreboardLine(scoreboard, objective, processedLine, theme.lines.size() - i);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to update scoreboard for player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Update a single scoreboard line
     */
    private void updateScoreboardLine(Scoreboard scoreboard, Objective objective, String text, int score) {
        try {
            // Create a simple player team for the line
            String teamName = "line_" + score;
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
            }
            
            // Set the team prefix to display the text
            team.setPlayerPrefix(Component.literal(text));
            
            // Add a dummy player name and set score
            String dummyPlayer = "§" + score; // Unique dummy player name
            if (!team.getPlayers().contains(dummyPlayer)) {
                scoreboard.addPlayerToTeam(dummyPlayer, team);
            }
            
            // Create a simple ScoreHolder for the dummy player
            ScoreHolder scoreHolder = () -> dummyPlayer;
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);
            scoreAccess.set(score);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to update scoreboard line: " + text, e);
            // Fallback: just skip this line if there's an API compatibility issue
        }
    }
    
    /**
     * Get server TPS (placeholder implementation)
     */
    private double getServerTPS() {
        if (server == null) return 20.0;
        
        try {
            // Simplified TPS calculation
            return Math.min(20.0, server.tickRateManager().tickrate());
        } catch (Exception e) {
            return 20.0; // Default to 20 TPS if calculation fails
        }
    }
    
    /**
     * Format time duration
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
    
    /**
     * Start the update task
     */
    private void startUpdateTask() {
        if (updateTaskStarted) {
            return; // Task already started
        }
        
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (server != null) {
                    updateAllPlayers();
                }
            }
        }, 1000, 2000); // Update every 2 seconds for enhanced features
        
        updateTaskStarted = true;
        LOGGER.debug("Update task started for TablistScoreboardManager");
    }
    
    /**
     * Update all online players
     */
    private void updateAllPlayers() {
        if (server == null) {
            LOGGER.warn("Cannot update all players - server instance is null");
            return;
        }
        
        try {
            animationFrame++; // Increment for animated themes
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updatePlayerTablist(player);
                updatePlayerScoreboard(player);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to update all players", e);
        }
    }
    
    /**
     * Initialize default themes
     */
    private void initializeThemes() {
        // Default tablist theme
        themes.put("default", new TablistTheme(
            "default",
            Arrays.asList(
                "§6§l✦ §b§lNeoEssentials Server §6§l✦",
                "§eWelcome, §a{player}§e!",
                "§eOnline: §a{online}§e/§a{max} §7| §eTPS: §a{tps}"
            ),
            Arrays.asList(
                "§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§7Website: §b§nwww.example.com",
                "§7Time: §a{time} §7| §7Ping: §a{ping}ms",
                "§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ),
            5000
        ));
        
        // VIP tablist theme
        themes.put("vip", new TablistTheme(
            "vip",
            Arrays.asList(
                "§d§l★ §5§lVIP SERVER §d§l★",
                "§dExclusive access for §6{player}",
                "§5Premium features unlocked!",
                "§dOnline VIPs: §a{online}§d/§a{max}"
            ),
            Arrays.asList(
                "§d§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§dVIP Benefits: §a✓ Fly §a✓ Home §a✓ Kits",
                "§5Support: §b§nvip@example.com",
                "§dThank you for supporting us! §6❤",
                "§d§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ),
            3000
        ));
        
        // Admin tablist theme
        themes.put("admin", new TablistTheme(
            "admin",
            Arrays.asList(
                "§4§l◆ §c§lADMIN PANEL §4§l◆",
                "§cWelcome, Administrator §6{player}",
                "§4Server Control Access Granted",
                "§cManaging §a{online} §cplayers"
            ),
            Arrays.asList(
                "§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§cTPS: §a{tps} §7| §cRAM: §a{ram_used}§7/§a{ram_max}",
                "§4Admin Tools: §e/tp /ban /kick /mute",
                "§cSystem Status: §aOnline",
                "§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ),
            2000
        ));
        
        // Server Info scoreboard theme
        scoreboardThemes.put("serverinfo", new ScoreboardTheme(
            "serverinfo",
            "§6§lServer Info",
            Arrays.asList(
                "§7━━━━━━━━━━━━━━━━━━━━━",
                "§b§lOnline",
                "§f{online}§7/§f{max} players",
                " ",
                "§e§lServer Stats",
                "§fTPS: §a{tps}",
                "§fUptime: §a{uptime}",
                "  ",
                "§d§lYour Info",
                "§fPing: §a{ping}ms",
                "§fWorld: §a{world}",
                "   ",
                "§7━━━━━━━━━━━━━━━━━━━━━"
            )
        ));
        
        // Player Stats scoreboard theme
        scoreboardThemes.put("playerstats", new ScoreboardTheme(
            "playerstats",
            "§a§lPlayer Stats",
            Arrays.asList(
                "§7━━━━━━━━━━━━━━━━━━━━━",
                "§b§l{player}",
                " ",
                "§e§lHealth & Status",
                "§fHealth: §c{health}§7/§c{max_health}",
                "§fFood: §6{food}§7/§620",
                "§fLevel: §a{level} §7(§a{exp}§7%)",
                "  ",
                "§d§lEconomy",
                "§fBalance: §6${balance}",
                "   ",
                "§a§lSession",
                "§fTime: §7{session_time}",
                "    ",
                "§7━━━━━━━━━━━━━━━━━━━━━"
            )
        ));
        
        LOGGER.info("Initialized {} tablist themes and {} scoreboard themes", 
            themes.size(), scoreboardThemes.size());
    }
    
    /**
     * Set player tablist theme
     */
    public void setPlayerTablistTheme(ServerPlayer player, String themeName) {
        if (themes.containsKey(themeName)) {
            playerTablistThemes.put(player.getUUID(), themeName);
            updatePlayerTablist(player);
            LOGGER.debug("Set tablist theme '{}' for player {}", themeName, player.getDisplayName().getString());
        } else {
            LOGGER.warn("Unknown tablist theme: {}", themeName);
        }
    }
    
    /**
     * Set player scoreboard theme
     */
    public void setPlayerScoreboardTheme(ServerPlayer player, String themeName) {
        if (scoreboardThemes.containsKey(themeName)) {
            playerScoreboardThemes.put(player.getUUID(), themeName);
            updatePlayerScoreboard(player);
            LOGGER.debug("Set scoreboard theme '{}' for player {}", themeName, player.getDisplayName().getString());
        } else {
            LOGGER.warn("Unknown scoreboard theme: {}", themeName);
        }
    }
    
    /**
     * Get available tablist themes
     */
    public Set<String> getAvailableTablistThemes() {
        return themes.keySet();
    }
    
    /**
     * Get available scoreboard themes
     */
    public Set<String> getAvailableScoreboardThemes() {
        return scoreboardThemes.keySet();
    }
    
    /**
     * Process placeholders in text
     */
    private String processPlaceholders(String text, ServerPlayer player) {
        if (text == null) return "";
        
        long sessionTime = 0;
        PlayerStats stats = playerStats.get(player.getUUID());
        if (stats != null) {
            sessionTime = (System.currentTimeMillis() - stats.joinTime) / 1000;
        }
        
        MinecraftServer serverInstance = this.server != null ? this.server : player.getServer();
        
        return text
            .replace("{player}", player.getDisplayName().getString())
            .replace("{online}", serverInstance != null ? String.valueOf(serverInstance.getPlayerCount()) : "1")
            .replace("{max}", serverInstance != null ? String.valueOf(serverInstance.getMaxPlayers()) : "20")
            .replace("{tps}", String.format("%.1f", getServerTPS()))
            .replace("{time}", new SimpleDateFormat("HH:mm:ss").format(new Date()))
            .replace("{ping}", String.valueOf(player.connection.latency()))
            .replace("{world}", player.level().dimension().location().getPath())
            .replace("{health}", String.valueOf((int)player.getHealth()))
            .replace("{max_health}", String.valueOf((int)player.getMaxHealth()))
            .replace("{food}", String.valueOf(player.getFoodData().getFoodLevel()))
            .replace("{level}", String.valueOf(player.experienceLevel))
            .replace("{exp}", String.valueOf((int)(player.experienceProgress * 100)))
            .replace("{balance}", "$1000") // Placeholder - would integrate with economy
            .replace("{session_time}", formatTime(sessionTime))
            .replace("{uptime}", getServerUptime())
            .replace("{ram_used}", getUsedRAM())
            .replace("{ram_max}", getMaxRAM())
            .replace("&", "§"); // Color code conversion
    }
    
    /**
     * Get server uptime (placeholder)
     */
    private String getServerUptime() {
        return "2h 30m"; // Placeholder
    }
    
    /**
     * Get used RAM
     */
    private String getUsedRAM() {
        Runtime runtime = Runtime.getRuntime();
        long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        return usedMB + "MB";
    }
    
    /**
     * Get max RAM
     */
    private String getMaxRAM() {
        Runtime runtime = Runtime.getRuntime();
        long maxMB = runtime.maxMemory() / 1024 / 1024;
        return maxMB + "MB";
    }
    
    /**
     * Stop the update task
     */
    public void shutdown() {
        updateTimer.cancel();
        LOGGER.info("Tablist and Scoreboard Manager shutdown");
    }
    
    /**
     * Player statistics class
     */
    private static class PlayerStats {
        long joinTime;
        
        // Additional stats can be added here as needed
        // long lastUpdate;
        // int totalPlayTime;
        // int blocksPlaced;
        // int blocksBroken;
        // int deaths;
        // int kills;
    }
    
    /**
     * Tablist theme data class
     */
    public static class TablistTheme {
        public final String name;
        public final List<String> headers;
        public final List<String> footers;
        public final int rotationInterval;
        
        public TablistTheme(String name, List<String> headers, List<String> footers, int rotationInterval) {
            this.name = name;
            this.headers = new ArrayList<>(headers);
            this.footers = new ArrayList<>(footers);
            this.rotationInterval = rotationInterval;
        }
    }
    
    /**
     * Scoreboard theme data class
     */
    public static class ScoreboardTheme {
        public final String name;
        public final String title;
        public final List<String> lines;
        
        public ScoreboardTheme(String name, String title, List<String> lines) {
            this.name = name;
            this.title = title;
            this.lines = new ArrayList<>(lines);
        }
    }
}
