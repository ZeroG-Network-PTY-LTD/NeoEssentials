package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages scoreboards for the TabManager system
 */
public class ScoreboardFeature extends AbstractFeature {
    // Cache of player scoreboards
    private final Map<UUID, String> playerScoreboards = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enabled = false;
    private Map<String, ScoreboardTemplate> scoreboardTemplates = new HashMap<>();
    private Map<String, String> worldScoreboards = new HashMap<>();
    private Map<String, String> groupScoreboards = new HashMap<>();
    
    /**
     * Creates a new scoreboard feature
     * 
     * @param tabManager The tab manager
     */
    public ScoreboardFeature(TabManager tabManager) {
        super(tabManager);
    }
    
    @Override
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing scoreboard feature");
    }
    
    @Override
    public void loadConfig() {
        // TODO: Load from config
        // In a real implementation, you'd load these from TablistTomlConfig
        enabled = true;
        
        // Example scoreboard templates
        Map<String, ScoreboardTemplate> templates = new HashMap<>();
        
        // Default scoreboard
        ScoreboardTemplate defaultTemplate = new ScoreboardTemplate();
        defaultTemplate.title = "&e&lSERVER INFO";
        defaultTemplate.lines = Arrays.asList(
            "&7&m---------------------",
            "&fPlayer: &a%player%",
            "&fRank: &a%group%",
            "&fCoins: &a%balance%",
            "&fOnline: &a%online%/%max%",
            "&fTPS: &a%tps%",
            "&7&m---------------------",
            "&ewww.example.com"
        );
        templates.put("default", defaultTemplate);
        
        // Admin scoreboard
        ScoreboardTemplate adminTemplate = new ScoreboardTemplate();
        adminTemplate.title = "&c&lADMIN PANEL";
        adminTemplate.lines = Arrays.asList(
            "&7&m---------------------",
            "&fServer TPS: &a%tps%",
            "&fMemory: &a%memory_percent%%",
            "&fUptime: &a%uptime%",
            "&fPlayers: &a%online%/%max%",
            "&7&m---------------------",
            "&cAdmin Tools Active"
        );
        templates.put("admin", adminTemplate);
        
        // VIP scoreboard
        ScoreboardTemplate vipTemplate = new ScoreboardTemplate();
        vipTemplate.title = "&6&lVIP STATS";
        vipTemplate.lines = Arrays.asList(
            "&7&m---------------------",
            "&fPlayer: &e%player%",
            "&fVIP Rank: &e%group%",
            "&fCoins: &e%balance%",
            "&fPlaytime: &e%playtime%",
            "&7&m---------------------",
            "&6VIP Perks Active"
        );
        templates.put("vip", vipTemplate);
        
        // Set templates
        this.scoreboardTemplates = templates;
        
        // World-specific scoreboards
        Map<String, String> worldBoards = new HashMap<>();
        worldBoards.put("minecraft:overworld", "default");
        worldBoards.put("minecraft:the_nether", "nether");
        worldBoards.put("minecraft:the_end", "end");
        this.worldScoreboards = worldBoards;
        
        // Group-specific scoreboards
        Map<String, String> groupBoards = new HashMap<>();
        groupBoards.put("admin", "admin");
        groupBoards.put("vip", "vip");
        this.groupScoreboards = groupBoards;
    }
    
    @Override
    public void update() {
        if (!isEnabled() || server == null) return;
        
        // Update scoreboards for all online players
        for (ServerPlayer player : tabManager.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
        }
    }
    
    /**
     * Updates the scoreboard for a specific player
     * 
     * @param player The player to update
     */
    private void updatePlayerScoreboard(ServerPlayer player) {
        executeWithErrorLogging(() -> {
            TabPlayerData playerData = tabManager.getPlayerData(player);
            if (playerData == null) return;
            
            // Determine which scoreboard to show
            String templateKey = getScoreboardTemplateKey(player, playerData);
            ScoreboardTemplate template = scoreboardTemplates.getOrDefault(templateKey, 
                scoreboardTemplates.get("default"));
            
            if (template == null) {
                // No template available
                removePlayerScoreboard(player);
                return;
            }
            
            // Check if the player's current scoreboard has changed
            String currentBoard = playerScoreboards.getOrDefault(player.getUUID(), "");
            if (!templateKey.equals(currentBoard)) {
                // Create a new scoreboard for this player
                createScoreboard(player, template);
                playerScoreboards.put(player.getUUID(), templateKey);
            } else {
                // Update the existing scoreboard
                updateScoreboard(player, template);
            }
        }, "Error updating scoreboard for player " + player.getScoreboardName());
    }
    
    /**
     * Determines the appropriate scoreboard template key for a player
     * 
     * @param player The player
     * @param playerData The player's data
     * @return The template key to use
     */
    private String getScoreboardTemplateKey(ServerPlayer player, TabPlayerData playerData) {
        // First check if there's a group-specific scoreboard
        String group = playerData.getGroup();
        if (groupScoreboards.containsKey(group)) {
            return groupScoreboards.get(group);
        }
        
        // Then check if there's a world-specific scoreboard
        String world = playerData.getWorld();
        if (worldScoreboards.containsKey(world)) {
            return worldScoreboards.get(world);
        }
        
        // Fall back to default
        return "default";
    }
    
    /**
     * Creates a new scoreboard for a player
     * 
     * @param player The player
     * @param template The template to use
     */
    private void createScoreboard(ServerPlayer player, ScoreboardTemplate template) {
        // Generate a unique objective name for this player
        String objectiveName = "tab_" + Math.abs(player.getUUID().hashCode() % 100000);
        if (objectiveName.length() > 16) {
            objectiveName = objectiveName.substring(0, 16);
        }
        
        // Create a new scoreboard instance
        Scoreboard scoreboard = new Scoreboard();
        
        // Create the objective
        Objective objective = scoreboard.addObjective(
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(processText(template.title, player)),
            ObjectiveCriteria.RenderType.INTEGER
        );
        
        // Set the display slot
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        
        // Add the scores
        addScoreboardLines(player, scoreboard, objective, template);
        
        // Set the player's scoreboard
        player.setScoreboard(scoreboard);
    }
    
    /**
     * Updates an existing scoreboard for a player
     * 
     * @param player The player
     * @param template The template to use
     */
    private void updateScoreboard(ServerPlayer player, ScoreboardTemplate template) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) {
            // Create a new scoreboard if the player doesn't have one
            createScoreboard(player, template);
            return;
        }
        
        // Find the objective in the sidebar display slot
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            // Create a new scoreboard if there's no objective
            createScoreboard(player, template);
            return;
        }
        
        // Update the objective display name
        objective.setDisplayName(Component.literal(processText(template.title, player)));
        
        // Clear existing scores
        for (String entry : new ArrayList<>(scoreboard.getPlayerNames())) {
            scoreboard.resetPlayerScore(entry, objective);
        }
        
        // Add the updated scores
        addScoreboardLines(player, scoreboard, objective, template);
    }
    
    /**
     * Adds scoreboard lines from a template
     * 
     * @param player The player
     * @param scoreboard The scoreboard
     * @param objective The objective
     * @param template The template
     */
    private void addScoreboardLines(ServerPlayer player, Scoreboard scoreboard, 
                                  Objective objective, ScoreboardTemplate template) {
        // Add lines in reverse order (to display properly in sidebar)
        int lineCount = template.lines.size();
        for (int i = 0; i < template.lines.size(); i++) {
            String rawLine = template.lines.get(i);
            String processedLine = processText(rawLine, player);
            
            // Create a unique entry name for each line
            String entry = getUniqueEntryName(i, processedLine);
            
            // Set the score
            Score score = scoreboard.getOrCreateScore(entry, objective);
            score.set(lineCount - i);
        }
    }
    
    /**
     * Processes text with placeholders and color codes
     * 
     * @param text The raw text
     * @param player The player context
     * @return The processed text
     */
    private String processText(String text, ServerPlayer player) {
        // Replace placeholders
        return tabManager.getPlaceholderManager().replacePlaceholders(text, player);
    }
    
    /**
     * Generates a unique entry name for a scoreboard line
     * 
     * @param index The line index
     * @param line The processed line text
     * @return A unique entry name (max 40 chars)
     */
    private String getUniqueEntryName(int index, String line) {
        // Create a unique but consistent name for this entry
        // Use color codes to make entries unique while keeping display colors
        ChatFormatting[] colors = ChatFormatting.values();
        ChatFormatting color = colors[index % colors.length];
        
        String entry = color + line;
        if (entry.length() > 40) {
            entry = entry.substring(0, 40);
        }
        
        return entry;
    }
    
    /**
     * Removes a player's scoreboard
     * 
     * @param player The player
     */
    private void removePlayerScoreboard(ServerPlayer player) {
        try {
            // Reset to the server's main scoreboard
            player.setScoreboard(server.getScoreboard());
            playerScoreboards.remove(player.getUUID());
        } catch (Exception e) {
            tabManager.getErrorLogger().logError(
                "Error removing scoreboard for player " + player.getScoreboardName(), e);
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Create scoreboard when player joins
        updatePlayerScoreboard(player);
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Clean up cached data
        playerScoreboards.remove(player.getUUID());
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        if (!isEnabled()) return;
        
        // Update scoreboard when player changes world
        updatePlayerScoreboard(player);
    }
    
    /**
     * Class to store scoreboard template configuration
     */
    private static class ScoreboardTemplate {
        String title = "";
        List<String> lines = new ArrayList<>();
    }
}
