package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player nametags in the game world
 */
public class NameTagsFeature extends AbstractFeature {
    // Cache of player teams
    private final Map<String, PlayerTeam> teamCache = new ConcurrentHashMap<>();
    
    // Cache of last sent prefixes and suffixes
    private final Map<UUID, String> lastPrefixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSuffixes = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enabled = false;
    private boolean useTeamColors = true;
    private Map<String, String> groupPrefixes = new HashMap<>();
    private Map<String, String> groupSuffixes = new HashMap<>();
    private Map<String, String> groupColors = new HashMap<>();
    
    /**
     * Creates a new nametags feature
     * 
     * @param tabManager The tab manager
     */
    public NameTagsFeature(TabManager tabManager) {
        super(tabManager);
    }
    
    @Override
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing nametags feature");
    }
    
    @Override
    public void loadConfig() {
        // TODO: Load from config
        // In a real implementation, you'd load these from TablistTomlConfig
        enabled = true;
        useTeamColors = true;
        
        // Example group prefixes and suffixes
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("admin", "&c[Admin] ");
        prefixes.put("mod", "&2[Mod] ");
        prefixes.put("vip", "&e[VIP] ");
        prefixes.put("default", "&7");
        
        Map<String, String> suffixes = new HashMap<>();
        suffixes.put("admin", " &c✦");
        suffixes.put("mod", " &2⚔");
        suffixes.put("vip", " &e★");
        suffixes.put("default", "");
        
        Map<String, String> colors = new HashMap<>();
        colors.put("admin", "red");
        colors.put("mod", "dark_green");
        colors.put("vip", "yellow");
        colors.put("default", "gray");
        
        this.groupPrefixes = prefixes;
        this.groupSuffixes = suffixes;
        this.groupColors = colors;
    }
    
    @Override
    public void update() {
        if (!isEnabled() || server == null) return;
        
        // Update nametags for all online players
        for (ServerPlayer player : tabManager.getOnlinePlayers()) {
            updatePlayerNameTag(player);
        }
    }
    
    /**
     * Updates the nametag for a specific player
     * 
     * @param player The player to update
     */
    private void updatePlayerNameTag(ServerPlayer player) {
        executeWithErrorLogging(() -> {
            TabPlayerData playerData = tabManager.getPlayerData(player);
            if (playerData == null) return;
            
            // Get player's group
            String group = playerData.getGroup();
            if (group == null || group.isEmpty()) {
                group = "default";
            }
            
            // Get prefix for this player
            String rawPrefix = groupPrefixes.getOrDefault(group, "");
            String processedPrefix = tabManager.getPlaceholderManager().replacePlaceholders(rawPrefix, player);
            
            // Get suffix for this player
            String rawSuffix = groupSuffixes.getOrDefault(group, "");
            String processedSuffix = tabManager.getPlaceholderManager().replacePlaceholders(rawSuffix, player);
            
            // Check if update needed
            String currentPrefix = lastPrefixes.getOrDefault(player.getUUID(), "");
            String currentSuffix = lastSuffixes.getOrDefault(player.getUUID(), "");
            
            if (!processedPrefix.equals(currentPrefix) || !processedSuffix.equals(currentSuffix)) {
                // Update is needed
                updateTeam(player, processedPrefix, processedSuffix, group);
                
                // Update cache
                lastPrefixes.put(player.getUUID(), processedPrefix);
                lastSuffixes.put(player.getUUID(), processedSuffix);
                
                // Update player data
                playerData.setNameTagPrefix(processedPrefix);
                playerData.setNameTagSuffix(processedSuffix);
            }
        }, "Error updating nametag for player " + player.getScoreboardName());
    }
    
    /**
     * Updates the team for a player
     * 
     * @param player The player
     * @param prefix The prefix to set
     * @param suffix The suffix to set
     * @param group The player's group
     */
    private void updateTeam(ServerPlayer player, String prefix, String suffix, String group) {
        Scoreboard scoreboard = server.getScoreboard();
        
        // Generate team name based on group and player
        String teamName = "nt_" + group + "_" + Math.abs(player.getUUID().hashCode() % 1000);
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
          // Get or create team
        final String finalTeamName = teamName;  // Create final version for lambda
        PlayerTeam team = teamCache.computeIfAbsent(finalTeamName, k -> {
            // Check if team exists already
            PlayerTeam existing = scoreboard.getPlayerTeam(finalTeamName);
            if (existing != null) {
                return existing;
            }
            
            // Create new team
            return scoreboard.addPlayerTeam(finalTeamName);
        });
          // Set team properties - convert String to Component
        team.setPlayerPrefix(net.minecraft.network.chat.Component.literal(prefix));
        team.setPlayerSuffix(net.minecraft.network.chat.Component.literal(suffix));
        if (useTeamColors) {
            String colorName = groupColors.getOrDefault(group, "white");
            try {
                team.setColor(net.minecraft.ChatFormatting.valueOf(colorName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                NeoEssentials.LOGGER.warn("Invalid team color: {}", colorName);
            }
        }
        
        // Add player to team if not already in it
        if (!team.getPlayers().contains(player.getScoreboardName())) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
        
        // Update the team for all players
        server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(
            team, player.getScoreboardName(), ClientboundSetPlayerTeamPacket.Action.ADD));
    }
    
    /**
     * Removes a player from their team
     * 
     * @param player The player to remove
     */
    private void removePlayerFromTeam(ServerPlayer player) {
        try {
            Scoreboard scoreboard = server.getScoreboard();
            String playerName = player.getScoreboardName();
            PlayerTeam team = scoreboard.getPlayersTeam(playerName);
            
            if (team != null) {
                scoreboard.removePlayerFromTeam(playerName, team);
                server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(
                    team, playerName, ClientboundSetPlayerTeamPacket.Action.REMOVE));
                
                // Clean up empty team if it was created by us
                if (team.getPlayers().isEmpty() && team.getName().startsWith("nt_")) {
                    scoreboard.removePlayerTeam(team);
                    teamCache.values().remove(team);
                }
            }
            
            // Clear cache
            lastPrefixes.remove(player.getUUID());
            lastSuffixes.remove(player.getUUID());
        } catch (Exception e) {
            tabManager.getErrorLogger().logError("Error removing player from team: " + player.getScoreboardName(), e);
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Apply nametag when player joins
        updatePlayerNameTag(player);
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Remove player from team and clean up
        removePlayerFromTeam(player);
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        if (!isEnabled()) return;
        
        // Re-apply nametag when player changes world
        updatePlayerNameTag(player);
    }
}
