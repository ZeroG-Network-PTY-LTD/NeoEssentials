package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tablist.TablistPlaceholderManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages scoreboard objectives for playerlist and belowname displays
 * Handles creation, updating, and display of objectives similar to TAB plugin
 */
public class ObjectiveManager {
    
    private MinecraftServer server;
    private TABConfig config;
    private TablistPlaceholderManager placeholderManager;
    
    // Objective instances
    private Objective playerlistObjective;
    private Objective belownameObjective;
    
    // Player score cache for performance
    private final Map<String, Integer> playerlistScores = new ConcurrentHashMap<>();
    private final Map<String, Integer> belownameScores = new ConcurrentHashMap<>();
    
    // Last update times for rate limiting
    private long lastPlayerlistUpdate = 0;
    private long lastBelownameUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // 1 second
    
    /**
     * Set the server reference
     * @param server The Minecraft server
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Set the placeholder manager
     * @param placeholderManager The placeholder manager
     */
    public void setPlaceholderManager(TablistPlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }
    
    /**
     * Initialize objectives based on configuration
     * @param config The TAB configuration
     */
    public void initialize(TABConfig config) {
        this.config = config;
        
        if (server == null) {
            NeoEssentials.LOGGER.warn("Cannot initialize ObjectiveManager without server");
            return;
        }
        
        createObjectives();
        NeoEssentials.LOGGER.info("ObjectiveManager initialized");
    }
    
    /**
     * Create objectives based on configuration
     */
    private void createObjectives() {
        if (server == null || config == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        
        // Create playerlist objective if enabled
        if (config.isPlayerlistObjectiveEnabled()) {
            createPlayerlistObjective(scoreboard);
        }
        
        // Create belowname objective if enabled
        if (config.isBelownameObjectiveEnabled()) {
            createBelownameObjective(scoreboard);
        }
    }
    
    /**
     * Create the playerlist objective (shown in TAB list)
     * @param scoreboard The server scoreboard
     */
    private void createPlayerlistObjective(Scoreboard scoreboard) {
        String objectiveName = "tab_playerlist";
        
        // Remove existing objective if it exists
        Objective existing = scoreboard.getObjective(objectiveName);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }
        
        // Create new objective
        playerlistObjective = scoreboard.addObjective(
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(config.getPlayerlistObjectiveTitle())
        );
        
        // Set display slot to player list
        scoreboard.setDisplayObjective(DisplaySlot.LIST, playerlistObjective);
        
        NeoEssentials.LOGGER.debug("Created playerlist objective: {}", objectiveName);
    }
    
    /**
     * Create the belowname objective (shown below player names)
     * @param scoreboard The server scoreboard
     */
    private void createBelownameObjective(Scoreboard scoreboard) {
        String objectiveName = "tab_belowname";
        
        // Remove existing objective if it exists
        Objective existing = scoreboard.getObjective(objectiveName);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }
        
        // Create new objective
        belownameObjective = scoreboard.addObjective(
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(config.getBelownameObjectiveTitle()),
            ObjectiveCriteria.RenderType.INTEGER // belowname always uses integer
        );
        
        // Set display slot to below name
        scoreboard.setDisplayObjective(1, belownameObjective); // 1 = BELOW_NAME slot
        
        NeoEssentials.LOGGER.debug("Created belowname objective: {}", objectiveName);
    }
    
    /**
     * Convert string render type to ObjectiveCriteria.RenderType
     * @param renderType The string render type
     * @return The ObjectiveCriteria.RenderType
     */
    private ObjectiveCriteria.RenderType getObjectiveRenderType(String renderType) {
        switch (renderType.toUpperCase()) {
            case "HEARTS":
                return ObjectiveCriteria.RenderType.HEARTS;
            case "INTEGER":
            default:
                return ObjectiveCriteria.RenderType.INTEGER;
        }
    }
    
    /**
     * Update objectives for all online players
     */
    public void updateObjectives() {
        if (server == null || config == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Update playerlist objective
        if (config.isPlayerlistObjectiveEnabled() && 
            currentTime - lastPlayerlistUpdate > UPDATE_INTERVAL) {
            updatePlayerlistObjective();
            lastPlayerlistUpdate = currentTime;
        }
        
        // Update belowname objective
        if (config.isBelownameObjectiveEnabled() && 
            currentTime - lastBelownameUpdate > UPDATE_INTERVAL) {
            updateBelownameObjective();
            lastBelownameUpdate = currentTime;
        }
    }
    
    /**
     * Update the playerlist objective for all players
     */
    private void updatePlayerlistObjective() {
        if (playerlistObjective == null || placeholderManager == null) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerlistScore(player);
        }
    }
    
    /**
     * Update the belowname objective for all players
     */
    private void updateBelownameObjective() {
        if (belownameObjective == null || placeholderManager == null) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateBelownameScore(player);
        }
    }
    
    /**
     * Update playerlist score for a specific player
     * @param player The player to update
     */
    public void updatePlayerlistScore(ServerPlayer player) {
        if (playerlistObjective == null || placeholderManager == null) return;
        
        String valueText = placeholderManager.replacePlaceholders(
            config.getPlayerlistObjectiveValue(), player);
        
        int score = parseScoreValue(valueText);
        String playerName = player.getScoreboardName();
        
        // Only update if score changed
        Integer lastScore = playerlistScores.get(playerName);
        if (lastScore == null || lastScore != score) {
            server.getScoreboard().getOrCreatePlayerScore(playerName, playerlistObjective).setScore(score);
            playerlistScores.put(playerName, score);
        }
    }
    
    /**
     * Update belowname score for a specific player
     * @param player The player to update
     */
    public void updateBelownameScore(ServerPlayer player) {
        if (belownameObjective == null || placeholderManager == null) return;
        
        String valueText = placeholderManager.replacePlaceholders(
            config.getBelownameObjectiveValue(), player);
        
        int score = parseScoreValue(valueText);
        String playerName = player.getScoreboardName();
        
        // Only update if score changed
        Integer lastScore = belownameScores.get(playerName);
        if (lastScore == null || lastScore != score) {
            server.getScoreboard().getOrCreatePlayerScore(playerName, belownameObjective).setScore(score);
            belownameScores.put(playerName, score);
        }
    }
    
    /**
     * Parse a string value to an integer score
     * @param value The string value (e.g., "123ms", "50%", "15")
     * @return The parsed integer score
     */
    private int parseScoreValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        
        // Remove common suffixes and formatting
        String cleaned = ChatFormatting.stripFormatting(value)
            .replaceAll("[^0-9-]", ""); // Keep only numbers and minus sign
        
        try {
            return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Handle player joining - add them to objectives
     * @param player The joining player
     */
    public void onPlayerJoin(ServerPlayer player) {
        if (config == null) return;
        
        // Update scores for the new player
        if (config.isPlayerlistObjectiveEnabled() && playerlistObjective != null) {
            updatePlayerlistScore(player);
        }
        
        if (config.isBelownameObjectiveEnabled() && belownameObjective != null) {
            updateBelownameScore(player);
        }
    }
    
    /**
     * Handle player leaving - clean up their scores
     * @param player The leaving player
     */
    public void onPlayerLeave(ServerPlayer player) {
        String playerName = player.getScoreboardName();
        
        // Clean up cached scores
        playerlistScores.remove(playerName);
        belownameScores.remove(playerName);
        
        // Remove from objectives
        if (playerlistObjective != null) {
            server.getScoreboard().resetPlayerScore(playerName, playerlistObjective);
        }
        
        if (belownameObjective != null) {
            server.getScoreboard().resetPlayerScore(playerName, belownameObjective);
        }
    }
    
    /**
     * Reload objective configuration
     * @param config The new configuration
     */
    public void reload(TABConfig config) {
        shutdown();
        initialize(config);
    }
    
    /**
     * Clean up all objectives
     */
    public void shutdown() {
        if (server != null) {
            Scoreboard scoreboard = server.getScoreboard();
            
            if (playerlistObjective != null) {
                scoreboard.removeObjective(playerlistObjective);
                playerlistObjective = null;
            }
            
            if (belownameObjective != null) {
                scoreboard.removeObjective(belownameObjective);
                belownameObjective = null;
            }
        }
        
        // Clear caches
        playerlistScores.clear();
        belownameScores.clear();
        
        NeoEssentials.LOGGER.info("ObjectiveManager shutdown");
    }
}
