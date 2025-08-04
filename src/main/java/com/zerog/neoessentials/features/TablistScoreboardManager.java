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
 * Custom Tablist and Scoreboard Manager for NeoEssentials
 * Provides enhanced player list display and custom scoreboards
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
    
    private static final String SIDEBAR_OBJECTIVE = "neoessentials_sidebar";
    
    private TablistScoreboardManager() {
        NeoForge.EVENT_BUS.register(this);
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
        setupScoreboards();
        startUpdateTask();
        LOGGER.info("Tablist and Scoreboard Manager initialized");
    }
    
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initializePlayerStats(player);
            updatePlayerTablist(player);
            updatePlayerScoreboard(player);
            LOGGER.debug("Initialized tablist/scoreboard for player: {}", player.getDisplayName().getString());
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
     * Update player tablist display
     */
    public void updatePlayerTablist(ServerPlayer player) {
        try {
            // Create custom header
            MutableComponent header = Component.literal("")
                .append(Component.literal("§6§l◆ ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("§f§lNeoEssentials Server").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" §6§l◆\n").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("§7Welcome, ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("§b" + player.getDisplayName().getString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("§7!").withStyle(ChatFormatting.GRAY));
            
            // Create custom footer with server info
            String timeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
            int onlinePlayers = server.getPlayerCount();
            int maxPlayers = server.getMaxPlayers();
            
            MutableComponent footer = Component.literal("")
                .append(Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("§a▪ ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("§fOnline: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("§b" + onlinePlayers + "/" + maxPlayers).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  §e▪ ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("§fTime: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("§6" + timeString).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("\n§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.GRAY));
            
            // Send tablist update
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
            
        } catch (Exception e) {
            LOGGER.error("Failed to update tablist for player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Update player scoreboard
     */
    public void updatePlayerScoreboard(ServerPlayer player) {
        try {
            Scoreboard scoreboard = server.getScoreboard();
            Objective objective = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
            if (objective == null) return;
            
            PlayerStats stats = playerStats.get(player.getUUID());
            if (stats == null) return;
            
            // Simple scoreboard update using basic score setting
            // Clear and recreate scores for this player's display
            
            // Server info section
            updateScoreboardLine(scoreboard, objective, "§7━━━━━━━━━━━━━━━━━━━━━", 15);
            updateScoreboardLine(scoreboard, objective, "§6§lServer Info", 14);
            updateScoreboardLine(scoreboard, objective, "§fOnline: §a" + server.getPlayerCount() + "/" + server.getMaxPlayers(), 13);
            updateScoreboardLine(scoreboard, objective, "§fTPS: §a" + String.format("%.1f", getServerTPS()), 12);
            updateScoreboardLine(scoreboard, objective, " ", 11); // Empty line
            
            // Player info section
            updateScoreboardLine(scoreboard, objective, "§b§lPlayer Info", 10);
            updateScoreboardLine(scoreboard, objective, "§fName: §7" + player.getDisplayName().getString(), 9);
            updateScoreboardLine(scoreboard, objective, "§fLevel: §e" + player.experienceLevel, 8);
            updateScoreboardLine(scoreboard, objective, "§fHealth: §c" + (int)player.getHealth() + "/" + (int)player.getMaxHealth(), 7);
            updateScoreboardLine(scoreboard, objective, "  ", 6); // Empty line
            
            // Economy section (placeholder)
            updateScoreboardLine(scoreboard, objective, "§a§lEconomy", 5);
            updateScoreboardLine(scoreboard, objective, "§fBalance: §6$1000", 4); // Placeholder
            updateScoreboardLine(scoreboard, objective, "   ", 3); // Empty line
            
            // Session info
            updateScoreboardLine(scoreboard, objective, "§d§lSession", 2);
            long sessionTime = (System.currentTimeMillis() - stats.joinTime) / 1000;
            updateScoreboardLine(scoreboard, objective, "§fTime: §7" + formatTime(sessionTime), 1);
            updateScoreboardLine(scoreboard, objective, "§7━━━━━━━━━━━━━━━━━━━━━", 0);
            
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
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (server != null) {
                    updateAllPlayers();
                }
            }
        }, 1000, 5000); // Update every 5 seconds
    }
    
    /**
     * Update all online players
     */
    private void updateAllPlayers() {
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updatePlayerTablist(player);
                updatePlayerScoreboard(player);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to update all players", e);
        }
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
}
