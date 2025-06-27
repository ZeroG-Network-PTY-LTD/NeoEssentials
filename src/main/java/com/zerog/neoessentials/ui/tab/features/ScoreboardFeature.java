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
        // In a real implementation, you'd load these from TablistYamlConfig
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
          // Create the objective with updated parameter list
        Objective objective = scoreboard.addObjective(
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(processText(template.title, player)),
            ObjectiveCriteria.RenderType.INTEGER,
            true, // displayAutoUpdate
            null  // numberFormat
        );
        
        // Set the display slot
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        
        // Add the scores
        addScoreboardLines(player, scoreboard, objective, template);
        
        // Set the player's scoreboard using reflection since the method isn't directly available
        try {
            // In Minecraft 1.21.1, we need to use the connection approach
            // Try to access player's connection field - try multiple possible field names
            Object connection = null;
            
            // Get all fields to look for the connection field
            java.lang.reflect.Field[] fields = ServerPlayer.class.getDeclaredFields();
            
            // Try common names and patterns for the connection field
            String[] possibleNames = {"connection", "playerConnection", "f_8941_", "connection", "field_71135_a"};
            
            // First try exact name matches
            for (String possibleName : possibleNames) {
                try {
                    java.lang.reflect.Field field = ServerPlayer.class.getDeclaredField(possibleName);
                    field.setAccessible(true);
                    connection = field.get(player);
                    if (connection != null) {
                        NeoEssentials.LOGGER.debug("Found connection field with name: {}", possibleName);
                        break;
                    }
                } catch (Exception ex) {
                    // Continue trying other names
                }
            }
            
            // If still null, try all fields that might be the connection
            if (connection == null) {
                for (java.lang.reflect.Field field : fields) {
                    try {
                        field.setAccessible(true);
                        Object obj = field.get(player);
                        if (obj != null && obj.getClass().getName().contains("Connection")) {
                            connection = obj;
                            NeoEssentials.LOGGER.debug("Found connection field by pattern: {}", field.getName());
                            break;
                        }
                    } catch (Exception ex) {
                        // Skip fields that can't be accessed
                    }
                }
            }
            
            // Then try to find a method to set the scoreboard on the connection
            if (connection != null) {
                // Try known method names based on common mappings (may vary based on MCP mappings)
                java.lang.reflect.Method setScoreboardMethod = null;
                
                // Look for methods that take a Scoreboard parameter
                for (java.lang.reflect.Method method : connection.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() == 1 && 
                        method.getParameterTypes()[0].getName().endsWith("Scoreboard")) {
                        setScoreboardMethod = method;
                        break;
                    }
                }
                
                if (setScoreboardMethod != null) {
                    setScoreboardMethod.setAccessible(true);
                    setScoreboardMethod.invoke(connection, scoreboard);
                    NeoEssentials.LOGGER.debug("Successfully set custom scoreboard for player {}", player.getScoreboardName());
                } else {
                    // If we can't find a direct method, try to use another approach
                    NeoEssentials.LOGGER.warn("Could not find appropriate method to set scoreboard for player {}", player.getScoreboardName());
                }
            } else {
                // Try alternative approaches if we couldn't find the connection field
                try {
                    NeoEssentials.LOGGER.debug("Connection field not found for player {}. Trying alternative approaches.", 
                        player.getScoreboardName());
                        
                    // Attempt to use server's functions to set player's scoreboard
                    // This is a placeholder for custom logic that would be version-specific
                    // For 1.21.1, we need to find the correct method for setting a scoreboard
                } catch (Exception alternativeEx) {
                    NeoEssentials.LOGGER.debug("Alternative scoreboard setting approach failed too: {}", 
                        alternativeEx.getMessage());
                }
            }
        } catch (Exception e) {
            // Fallback approach - use the server's scoreboard
            NeoEssentials.LOGGER.warn("Could not set custom scoreboard for player {}", player.getScoreboardName(), e);
        }
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
          // Clear existing scores using reflection
        try {
            // Get player names
            java.lang.reflect.Method getPlayerNamesMethod = 
                Scoreboard.class.getDeclaredMethod("getPlayerNames");
            getPlayerNamesMethod.setAccessible(true);
            @SuppressWarnings("unchecked")
            Collection<String> playerNames = 
                (Collection<String>) getPlayerNamesMethod.invoke(scoreboard);
            
            // Reset scores for each player
            java.lang.reflect.Method resetPlayerScoreMethod = 
                Scoreboard.class.getDeclaredMethod("resetPlayerScore", String.class, Objective.class);
            resetPlayerScoreMethod.setAccessible(true);
            
            for (String entry : new ArrayList<>(playerNames)) {
                resetPlayerScoreMethod.invoke(scoreboard, entry, objective);
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to clear scoreboard scores", e);
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
              // Set the score using reflection
            try {
                // Get or create score
                java.lang.reflect.Method getOrCreateScoreMethod = 
                    Scoreboard.class.getDeclaredMethod("getOrCreateScore", String.class, Objective.class);
                getOrCreateScoreMethod.setAccessible(true);
                Score score = (Score) getOrCreateScoreMethod.invoke(scoreboard, entry, objective);
                
                // Set score value
                java.lang.reflect.Method setScoreMethod = Score.class.getDeclaredMethod("set", int.class);
                setScoreMethod.setAccessible(true);
                setScoreMethod.invoke(score, lineCount - i);
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to set scoreboard line", e);
            }
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
            // In Minecraft 1.21.1, the direct setScoreboard method no longer exists
            // Instead, we need to use the connection object to set the scoreboard
            // or get the player's connection first
            try {
                // Try to access player's connection field - try multiple possible field names
                Object connection = null;
                
                // Get all fields to look for the connection field
                java.lang.reflect.Field[] fields = ServerPlayer.class.getDeclaredFields();
                
                // Try common names and patterns for the connection field
                String[] possibleNames = {"connection", "playerConnection", "f_8941_", "connection", "field_71135_a"};
                
                // First try exact name matches
                for (String possibleName : possibleNames) {
                    try {
                        java.lang.reflect.Field field = ServerPlayer.class.getDeclaredField(possibleName);
                        field.setAccessible(true);
                        connection = field.get(player);
                        if (connection != null) {
                            NeoEssentials.LOGGER.debug("Found connection field with name: {}", possibleName);
                            break;
                        }
                    } catch (Exception ex) {
                        // Continue trying other names
                    }
                }
                
                // If still null, try all fields that might be the connection
                if (connection == null) {
                    for (java.lang.reflect.Field field : fields) {
                        try {
                            field.setAccessible(true);
                            Object obj = field.get(player);
                            if (obj != null && obj.getClass().getName().contains("Connection")) {
                                connection = obj;
                                NeoEssentials.LOGGER.debug("Found connection field by pattern: {}", field.getName());
                                break;
                            }
                        } catch (Exception ex) {
                            // Skip fields that can't be accessed
                        }
                    }
                }
                
                // Then try to find a method to set the scoreboard on the connection
                if (connection != null) {
                    Class<?> connectionClass = connection.getClass();
                    java.lang.reflect.Method setScoreboardMethod = null;
                    
                    // Try to find the appropriate method (may have different names)
                    for (java.lang.reflect.Method method : connectionClass.getDeclaredMethods()) {
                        if (method.getParameterCount() == 1 && 
                            method.getParameterTypes()[0].getName().endsWith("Scoreboard")) {
                            setScoreboardMethod = method;
                            break;
                        }
                    }
                    
                    // If we found a method, use it
                    if (setScoreboardMethod != null) {
                        setScoreboardMethod.setAccessible(true);
                        setScoreboardMethod.invoke(connection, server.getScoreboard());
                        NeoEssentials.LOGGER.debug("Successfully reset scoreboard for player {} using connection", player.getScoreboardName());
                    } else {
                        NeoEssentials.LOGGER.debug("Couldn't find scoreboard reset method for player {}", player.getScoreboardName());
                    }
                } else {
                    // Try alternative approaches if we couldn't find the connection field
                    try {
                        // Try sending a direct scoreboard packet using Minecraft's packet system
                        NeoEssentials.LOGGER.debug("Connection field not found for player {}. Trying alternative approaches.", 
                            player.getScoreboardName());
                            
                        // Just log it for now - the scoreboards will reset themselves when players move between worlds
                        // or eventually when they reconnect
                    } catch (Exception alternativeEx) {
                        NeoEssentials.LOGGER.debug("Alternative scoreboard reset approach failed too: {}", 
                            alternativeEx.getMessage());
                    }
                }
            } catch (Exception methodException) {
                // Fallback to letting the scoreboard manager handle it naturally
                NeoEssentials.LOGGER.debug("Using fallback method to reset scoreboard - connection approach failed: {}", 
                    methodException.getMessage());
            }
            
            // Remove from our tracking regardless of outcome
            playerScoreboards.remove(player.getUUID());
            NeoEssentials.LOGGER.debug("Removed player {} from scoreboard tracking", player.getScoreboardName());
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
