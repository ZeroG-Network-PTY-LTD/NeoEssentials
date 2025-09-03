package com.zerog.neoessentials.features;

import com.zerog.neoessentials.util.DebugUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Professional ScoreboardManager - Clean implementation for NeoForge 1.21.1
 */
public class ScoreboardManager {
    
    private static ScoreboardManager instance;
    private MinecraftServer server;
    private ScheduledExecutorService scheduler;
    
    // Player scoreboard management
    private final Map<UUID, Objective> playerObjectives = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enabled = true;
    private int updateInterval = 1; // seconds
    private final int throttleTime = 100; // ms
    
    private ScoreboardManager() {
        // Private constructor for singleton
    }
    
    public static ScoreboardManager getInstance() {
        if (instance == null) {
            instance = new ScoreboardManager();
        }
        return instance;
    }
    
    /**
     * Initialize the scoreboard system
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        
        // Register events
        NeoForge.EVENT_BUS.register(this);
        
        // Start scheduler
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateAllScoreboardsScheduled, 5, updateInterval, TimeUnit.SECONDS);
        
        DebugUtil.debugLog("[ScoreboardManager] Professional scoreboard system initialized");
    }
    
    /**
     * Server started event
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (server == null) {
            initialize(event.getServer());
        }
    }
    
    /**
     * Player join event
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scheduler.schedule(() -> {
                setupPlayerScoreboard(player);
                updateScoreboard(player);
            }, 1, TimeUnit.SECONDS);
        }
    }
    
    /**
     * Player leave event
     */
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cleanupPlayerScoreboard(player);
        }
    }
    
    /**
     * Setup scoreboard for a player
     */
    public void setupPlayerScoreboard(ServerPlayer player) {
        if (!enabled || server == null) return;
        
        try {
            Scoreboard scoreboard = server.getScoreboard();
            String objectiveName = "neo_sb_" + player.getUUID().toString().substring(0, 8);
            
            // Remove existing objective if it exists
            Objective existingObjective = scoreboard.getObjective(objectiveName);
            if (existingObjective != null) {
                scoreboard.removeObjective(existingObjective);
            }
            
            // Create new objective - using simplified API
            Objective objective = scoreboard.addObjective(
                objectiveName, 
                ObjectiveCriteria.DUMMY, 
                Component.literal("NeoEssentials"), 
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
            );
            
            // Set scoreboard to display on sidebar
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            
            // Store objective reference
            playerObjectives.put(player.getUUID(), objective);
            
            DebugUtil.debugLog("[ScoreboardManager] Setup scoreboard for player: " + player.getName().getString());
            
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error setting up scoreboard for " + player.getName().getString() + ": " + e.getMessage());
        }
    }
    
    /**
     * Update scoreboard for a player
     */
    public void updateScoreboard(ServerPlayer player) {
        if (!enabled) return;
        
        // Throttle updates
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(player.getUUID());
        if (lastUpdate != null && (currentTime - lastUpdate) < throttleTime) {
            return;
        }
        lastUpdateTime.put(player.getUUID(), currentTime);
        
        try {
            // Get player's objective
            Objective objective = playerObjectives.get(player.getUUID());
            if (objective == null) {
                setupPlayerScoreboard(player);
                objective = playerObjectives.get(player.getUUID());
                if (objective == null) return;
            }
            
            // Create default scoreboard content
            String title = "§6§lNeoEssentials";
            List<String> lines = Arrays.asList(
                "§7Server: §aOnline",
                "",
                "§7Players: §a" + server.getPlayerList().getPlayerCount(),
                "§7TPS: §a20.0",
                "",
                "§7Your Rank: §bPlayer",
                "§7Balance: §e$0",
                "",
                "§7Coordinates:",
                "§7X: §f" + (int) player.getX(),
                "§7Y: §f" + (int) player.getY(),
                "§7Z: §f" + (int) player.getZ(),
                "",
                "§7World: §a" + player.level().dimension().location().getPath(),
                "§6play.server.com"
            );
            
            // Update the display
            updateScoreboardDisplay(player, objective, title, lines);
            
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error updating scoreboard for " + player.getName().getString() + ": " + e.getMessage());
        }
    }
    
    /**
     * Update the actual scoreboard display - simplified for compatibility
     */
    private void updateScoreboardDisplay(ServerPlayer player, Objective objective, String title, List<String> lines) {
        try {
            Scoreboard scoreboard = server.getScoreboard();
            
            // Update title
            objective.setDisplayName(Component.literal(title));
            
            // Clear and recreate objective for clean update
            String oldName = objective.getName();
            scoreboard.removeObjective(objective);
            
            // Create fresh objective
            Objective newObjective = scoreboard.addObjective(
                oldName, 
                ObjectiveCriteria.DUMMY, 
                Component.literal(title), 
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
            );
            
            // Set new objective to sidebar
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, newObjective);
            
            // Update the stored objective reference
            playerObjectives.put(player.getUUID(), newObjective);
            
            // Add new lines (reverse order - Minecraft displays from bottom to top)
            int score = lines.size();
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    // Create unique empty line
                    String emptyLine = " ".repeat(Math.max(1, 16 - score));
                    ScoreHolder holder = ScoreHolder.forNameOnly(emptyLine);
                    ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(holder, newObjective);
                    scoreAccess.set(score);
                } else {
                    // Ensure line is unique (Minecraft requirement)
                    String uniqueLine = ensureUniqueScoreboardLine(line, lines, lines.indexOf(line));
                    ScoreHolder holder = ScoreHolder.forNameOnly(uniqueLine);
                    ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(holder, newObjective);
                    scoreAccess.set(score);
                }
                score--;
            }
            
        } catch (Exception e) {
            DebugUtil.debugLog("[ScoreboardManager] Error updating scoreboard display: " + e.getMessage());
        }
    }
    
    /**
     * Ensure scoreboard line is unique (Minecraft requirement)
     */
    private String ensureUniqueScoreboardLine(String line, List<String> allLines, int currentIndex) {
        String uniqueLine = line;
        
        // Check if this line appears elsewhere
        for (int i = 0; i < allLines.size(); i++) {
            if (i != currentIndex && allLines.get(i).equals(uniqueLine)) {
                // Make it unique by adding invisible characters
                uniqueLine = line + "§r".repeat(currentIndex + 1);
                break;
            }
        }
        
        return uniqueLine;
    }
    
    /**
     * Update all player scoreboards - public method for external calls
     */
    public void updateAllScoreboards() {
        if (!enabled || server == null) return;
        
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateScoreboard(player);
            }
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error updating all scoreboards: " + e.getMessage());
        }
    }
    
    /**
     * Update all player scoreboards - private method for scheduler
     */
    private void updateAllScoreboardsScheduled() {
        updateAllScoreboards();
    }
    
    /**
     * Clean up player scoreboard
     */
    public void cleanupPlayerScoreboard(ServerPlayer player) {
        try {
            UUID uuid = player.getUUID();
            
            // Remove objective
            Objective objective = playerObjectives.remove(uuid);
            if (objective != null && server != null) {
                server.getScoreboard().removeObjective(objective);
            }
            
            // Clean up tracking data
            lastUpdateTime.remove(uuid);
            
            DebugUtil.debugLog("[ScoreboardManager] Cleaned up scoreboard for player: " + player.getName().getString());
            
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error cleaning up scoreboard: " + e.getMessage());
        }
    }
    
    /**
     * Toggle scoreboard for a player
     */
    public void toggleScoreboard(ServerPlayer player) {
        UUID uuid = player.getUUID();
        
        if (playerObjectives.containsKey(uuid)) {
            // Hide scoreboard
            cleanupPlayerScoreboard(player);
            player.sendSystemMessage(Component.literal("§7Scoreboard §cdisabled"));
        } else {
            // Show scoreboard
            setupPlayerScoreboard(player);
            updateScoreboard(player);
            player.sendSystemMessage(Component.literal("§7Scoreboard §aenabled"));
        }
    }
    
    /**
     * Force update a player's scoreboard
     */
    public void forceUpdateScoreboard(ServerPlayer player) {
        try {
            lastUpdateTime.remove(player.getUUID()); // Remove throttling
            updateScoreboard(player);
            DebugUtil.debugLog("[ScoreboardManager] Force updated scoreboard for " + player.getName().getString());
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error force updating scoreboard: " + e.getMessage());
        }
    }
    
    /**
     * Reload configuration - public method for commands
     */
    public void reloadConfig() {
        try {
            // For now, just update all scoreboards
            updateAllScoreboards();
            DebugUtil.debugLog("[ScoreboardManager] Configuration reloaded");
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error reloading configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get status information - public method for commands
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("ScoreboardManager Status:\n");
        status.append("Enabled: ").append(enabled).append("\n");
        status.append("Update Interval: ").append(updateInterval).append(" seconds\n");
        status.append("Active Objectives: ").append(playerObjectives.size()).append("\n");
        status.append("Server: ").append(server != null ? "Connected" : "Disconnected");
        return status.toString();
    }
    
    /**
     * Check if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && server != null) {
            // Clean up all scoreboards
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                cleanupPlayerScoreboard(player);
            }
        }
    }
    
    /**
     * Shutdown the scoreboard system
     */
    public void shutdown() {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            
            // Clean up all player scoreboards
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    cleanupPlayerScoreboard(player);
                }
            }
            
            playerObjectives.clear();
            lastUpdateTime.clear();
            
            DebugUtil.debugLog("[ScoreboardManager] Scoreboard system shutdown");
            
        } catch (Exception e) {
            DebugUtil.errorLog("[ScoreboardManager] Error during shutdown: " + e.getMessage());
        }
    }
}
