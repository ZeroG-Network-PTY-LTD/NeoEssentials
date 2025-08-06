package com.zerog.neoessentials.features;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Tablist Manager for NeoEssentials
 * Provides advanced tablist/scoreboard customization with themes, animations, and conditional displays
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class EnhancedTablistManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedTablistManager.class);
    private static EnhancedTablistManager instance;
    
    // Scoreboard objectives
    private static final String SIDEBAR_OBJECTIVE = "ness_sidebar";
    
    // Tablist themes and configurations
    private final Map<String, TablistTheme> themes = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerTablistData> playerData = new ConcurrentHashMap<>();
    private final Map<String, ScoreboardTheme> scoreboardThemes = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerActiveThemes = new ConcurrentHashMap<>();
    
    // Animation and update management
    private final Timer updateTimer = new Timer("EnhancedTablistUpdater", true);
    private boolean updateTaskStarted = false;
    private int animationFrame = 0;
    
    private MinecraftServer server;
    
    private EnhancedTablistManager() {
        initializeDefaultThemes();
        initializeDefaultScoreboardThemes();
    }
    
    public static EnhancedTablistManager getInstance() {
        if (instance == null) {
            instance = new EnhancedTablistManager();
        }
        return instance;
    }
    
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();
        startUpdateTask();
        LOGGER.info("Enhanced Tablist Manager initialized with {} themes and {} scoreboard themes", 
            themes.size(), scoreboardThemes.size());
    }
    
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initializePlayerData(player);
            updatePlayerDisplay(player);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerData.remove(player.getUUID());
            playerActiveThemes.remove(player.getUUID());
        }
    }
    
    /**
     * Initialize default tablist themes
     */
    private void initializeDefaultThemes() {
        // Default theme
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
            5000, // 5 second rotation
            true  // animated
        ));
        
        // VIP theme
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
            3000, // 3 second rotation
            true
        ));
        
        // Admin theme
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
                "§cSystem Status: §a{status}",
                "§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ),
            2000, // 2 second rotation
            true
        ));
        
        // Event theme
        themes.put("event", new TablistTheme(
            "event",
            Arrays.asList(
                "§a§l⚡ §2§lEVENT ACTIVE §a§l⚡",
                "§2Join the {event_name} event!",
                "§aRewards: §6{event_rewards}",
                "§2Participants: §a{event_players}"
            ),
            Arrays.asList(
                "§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "§2Event Time: §a{event_time_left}",
                "§aLocation: §e{event_location}",
                "§2Type §a/event join §2to participate!",
                "§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ),
            1000, // 1 second rotation (fast for events)
            true
        ));
    }
    
    /**
     * Initialize default scoreboard themes
     */
    private void initializeDefaultScoreboardThemes() {
        // Server Info theme
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
        
        // Player Stats theme
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
                "§fDeaths: §c{deaths}",
                "    ",
                "§7━━━━━━━━━━━━━━━━━━━━━"
            )
        ));
        
        // PvP theme
        scoreboardThemes.put("pvp", new ScoreboardTheme(
            "pvp",
            "§c§lPvP Stats",
            Arrays.asList(
                "§7━━━━━━━━━━━━━━━━━━━━━",
                "§4§lCombat Stats",
                " ",
                "§c§lKills & Deaths",
                "§fKills: §a{kills}",
                "§fDeaths: §c{deaths}",
                "§fK/D Ratio: §e{kd_ratio}",
                "  ",
                "§6§lRanking",
                "§fRank: §b#{rank}",
                "§fKillstreak: §a{killstreak}",
                "   ",
                "§e§lCurrent Match",
                "§fPlayers: §a{pvp_players}",
                "§fTime Left: §a{match_time}",
                "    ",
                "§7━━━━━━━━━━━━━━━━━━━━━"
            )
        ));
    }
    
    /**
     * Initialize player data
     */
    private void initializePlayerData(ServerPlayer player) {
        PlayerTablistData data = new PlayerTablistData();
        data.joinTime = System.currentTimeMillis();
        data.currentTheme = determinePlayerTheme(player);
        data.currentScoreboardTheme = determinePlayerScoreboardTheme(player);
        
        playerData.put(player.getUUID(), data);
        playerActiveThemes.put(player.getUUID(), data.currentTheme);
    }
    
    /**
     * Determine player theme based on permissions/rank
     */
    private String determinePlayerTheme(ServerPlayer player) {
        // Check permissions and return appropriate theme
        if (hasPermission(player, "neoessentials.theme.admin")) {
            return "admin";
        } else if (hasPermission(player, "neoessentials.theme.vip")) {
            return "vip";
        } else if (hasPermission(player, "neoessentials.theme.event")) {
            return "event";
        }
        return "default";
    }
    
    /**
     * Determine player scoreboard theme
     */
    private String determinePlayerScoreboardTheme(ServerPlayer player) {
        // Default logic - can be expanded with permissions
        if (hasPermission(player, "neoessentials.scoreboard.pvp")) {
            return "pvp";
        } else if (hasPermission(player, "neoessentials.scoreboard.stats")) {
            return "playerstats";
        }
        return "serverinfo";
    }
    
    /**
     * Check if player has permission (placeholder implementation)
     */
    private boolean hasPermission(ServerPlayer player, String permission) {
        // Placeholder - would integrate with permission system
        return player.hasPermissions(2); // OP level check as fallback
    }
    
    /**
     * Update player display (tablist + scoreboard)
     */
    public void updatePlayerDisplay(ServerPlayer player) {
        updatePlayerTablist(player);
        updatePlayerScoreboard(player);
    }
    
    /**
     * Update player tablist with theme
     */
    private void updatePlayerTablist(ServerPlayer player) {
        try {
            PlayerTablistData data = playerData.get(player.getUUID());
            if (data == null) return;
            
            TablistTheme theme = themes.get(data.currentTheme);
            if (theme == null) theme = themes.get("default");
            
            // Get current frame for animations
            int frameIndex = theme.animated ? animationFrame % theme.headers.size() : 0;
            int footerFrameIndex = theme.animated ? animationFrame % theme.footers.size() : 0;
            
            // Build header
            MutableComponent header = Component.literal("");
            String headerText = theme.headers.get(frameIndex);
            header.append(Component.literal(processPlaceholders(headerText, player)));
            
            // Build footer
            MutableComponent footer = Component.literal("");
            String footerText = theme.footers.get(footerFrameIndex);
            footer.append(Component.literal(processPlaceholders(footerText, player)));
            
            // Send tablist update
            player.connection.send(new ClientboundTabListPacket(header, footer));
            
        } catch (Exception e) {
            LOGGER.error("Failed to update tablist for player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Update player scoreboard with theme
     */
    private void updatePlayerScoreboard(ServerPlayer player) {
        try {
            PlayerTablistData data = playerData.get(player.getUUID());
            if (data == null) return;
            
            ScoreboardTheme theme = scoreboardThemes.get(data.currentScoreboardTheme);
            if (theme == null) theme = scoreboardThemes.get("serverinfo");
            
            MinecraftServer serverInstance = this.server != null ? this.server : player.getServer();
            if (serverInstance == null) return;
            
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
            
            // Update scoreboard lines
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
            String teamName = "line_" + score;
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
            }
            
            team.setPlayerPrefix(Component.literal(text));
            
            String dummyPlayer = "§" + score;
            if (!team.getPlayers().contains(dummyPlayer)) {
                scoreboard.addPlayerToTeam(dummyPlayer, team);
            }
            
            ScoreHolder scoreHolder = () -> dummyPlayer;
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);
            scoreAccess.set(score);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to update scoreboard line: " + text, e);
        }
    }
    
    /**
     * Process placeholders in text
     */
    private String processPlaceholders(String text, ServerPlayer player) {
        if (text == null) return "";
        
        long sessionTime = 0;
        PlayerTablistData data = playerData.get(player.getUUID());
        if (data != null) {
            sessionTime = (System.currentTimeMillis() - data.joinTime) / 1000;
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
            .replace("{deaths}", "0") // Placeholder - would track deaths
            .replace("{kills}", "0") // Placeholder - would track kills
            .replace("{kd_ratio}", "0.0") // Placeholder
            .replace("{rank}", "1") // Placeholder
            .replace("{killstreak}", "0") // Placeholder
            .replace("{pvp_players}", "0") // Placeholder
            .replace("{match_time}", "0:00") // Placeholder
            .replace("{uptime}", getServerUptime())
            .replace("{ram_used}", getUsedRAM())
            .replace("{ram_max}", getMaxRAM())
            .replace("{status}", "Online")
            .replace("{event_name}", "Build Contest") // Placeholder
            .replace("{event_rewards}", "Diamond Blocks") // Placeholder
            .replace("{event_players}", "5") // Placeholder
            .replace("{event_time_left}", "30:00") // Placeholder
            .replace("{event_location}", "Spawn") // Placeholder
            .replace("&", "§"); // Color code conversion
    }
    
    /**
     * Get server TPS
     */
    private double getServerTPS() {
        if (server == null) return 20.0;
        try {
            return Math.min(20.0, server.tickRateManager().tickrate());
        } catch (Exception e) {
            return 20.0;
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
     * Get server uptime (placeholder)
     */
    private String getServerUptime() {
        return "2h 30m"; // Placeholder
    }
    
    /**
     * Get used RAM (placeholder)
     */
    private String getUsedRAM() {
        Runtime runtime = Runtime.getRuntime();
        long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        return usedMB + "MB";
    }
    
    /**
     * Get max RAM (placeholder)
     */
    private String getMaxRAM() {
        Runtime runtime = Runtime.getRuntime();
        long maxMB = runtime.maxMemory() / 1024 / 1024;
        return maxMB + "MB";
    }
    
    /**
     * Set player theme
     */
    public void setPlayerTheme(ServerPlayer player, String themeName) {
        PlayerTablistData data = playerData.get(player.getUUID());
        if (data != null && themes.containsKey(themeName)) {
            data.currentTheme = themeName;
            playerActiveThemes.put(player.getUUID(), themeName);
            updatePlayerTablist(player);
        }
    }
    
    /**
     * Set player scoreboard theme
     */
    public void setPlayerScoreboardTheme(ServerPlayer player, String themeName) {
        PlayerTablistData data = playerData.get(player.getUUID());
        if (data != null && scoreboardThemes.containsKey(themeName)) {
            data.currentScoreboardTheme = themeName;
            updatePlayerScoreboard(player);
        }
    }
    
    /**
     * Get available themes
     */
    public Set<String> getAvailableThemes() {
        return themes.keySet();
    }
    
    /**
     * Get available scoreboard themes
     */
    public Set<String> getAvailableScoreboardThemes() {
        return scoreboardThemes.keySet();
    }
    
    /**
     * Start the update task
     */
    private void startUpdateTask() {
        if (updateTaskStarted) return;
        
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (server != null) {
                    animationFrame++;
                    updateAllPlayers();
                }
            }
        }, 1000, 2000); // Update every 2 seconds
        
        updateTaskStarted = true;
        LOGGER.debug("Enhanced tablist update task started");
    }
    
    /**
     * Update all online players
     */
    private void updateAllPlayers() {
        if (server == null) return;
        
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updatePlayerDisplay(player);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to update all players", e);
        }
    }
    
    /**
     * Shutdown
     */
    public void shutdown() {
        updateTimer.cancel();
        playerData.clear();
        playerActiveThemes.clear();
        LOGGER.info("Enhanced Tablist Manager shutdown");
    }
    
    /**
     * Tablist theme data class
     */
    public static class TablistTheme {
        public final String name;
        public final List<String> headers;
        public final List<String> footers;
        public final int rotationInterval;
        public final boolean animated;
        
        public TablistTheme(String name, List<String> headers, List<String> footers, 
                           int rotationInterval, boolean animated) {
            this.name = name;
            this.headers = new ArrayList<>(headers);
            this.footers = new ArrayList<>(footers);
            this.rotationInterval = rotationInterval;
            this.animated = animated;
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
    
    /**
     * Player tablist data
     */
    private static class PlayerTablistData {
        long joinTime;
        String currentTheme = "default";
        String currentScoreboardTheme = "serverinfo";
    }
}
