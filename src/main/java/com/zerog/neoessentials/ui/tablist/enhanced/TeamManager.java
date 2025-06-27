package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Team management for player sorting and collision
 * Handles creation and management of scoreboard teams for tablist sorting
 */
public class TeamManager {
    
    private MinecraftServer server;
    private TABConfig config;
    private final Map<String, PlayerTeam> teams = new ConcurrentHashMap<>();
    
    /**
     * Set the server reference
     * @param server The Minecraft server
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Initialize teams based on configuration
     * @param config The TAB configuration
     */
    public void initialize(TABConfig config) {
        this.config = config;
        
        if (server == null) {
            NeoEssentials.LOGGER.warn("Cannot initialize TeamManager without server");
            return;
        }
        
        createTeams();
        NeoEssentials.LOGGER.info("TeamManager initialized");
    }
    
    /**
     * Create teams based on configuration
     */
    private void createTeams() {
        if (server == null || config == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        
        // Create teams for each group priority
        for (Map.Entry<String, Integer> entry : config.getGroupPriorities().entrySet()) {
            String groupName = entry.getKey();
            
            // Create team name with priority for sorting
            String teamName = String.format("%03d_%s", entry.getValue(), groupName);
            
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
                team.setCollisionRule(config.isEnableCollision() ? 
                    PlayerTeam.CollisionRule.ALWAYS : PlayerTeam.CollisionRule.NEVER);
                team.setNameTagVisibility(config.isInvisibleNametags() ? 
                    PlayerTeam.Visibility.NEVER : PlayerTeam.Visibility.ALWAYS);
                team.setCanSeeFriendlyInvisibles(config.isCanSeeFriendlyInvisibles());
                
                teams.put(groupName, team);
                NeoEssentials.LOGGER.debug("Created team for group: {}", groupName);
            }
        }
    }
    
    /**
     * Add player to appropriate team when they join
     * @param player The player joining
     */
    public void onPlayerJoin(ServerPlayer player) {
        if (server == null || config == null) return;
        
        String group = determinePlayerGroup(player);
        PlayerTeam team = teams.get(group);
        
        if (team != null) {
            server.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
            NeoEssentials.LOGGER.debug("Added player {} to team {}", player.getScoreboardName(), team.getName());
        }
    }
    
    /**
     * Remove player from team when they leave
     * @param player The player leaving
     */
    public void onPlayerLeave(ServerPlayer player) {
        if (server == null) return;
        
        server.getScoreboard().removePlayerFromTeam(player.getScoreboardName());
        NeoEssentials.LOGGER.debug("Removed player {} from team", player.getScoreboardName());
    }
    
    /**
     * Determine player's group based on permissions
     * @param player The player
     * @return The group name
     */
    private String determinePlayerGroup(ServerPlayer player) {
        // Check for specific group permissions in priority order
        for (Map.Entry<String, Integer> entry : config.getGroupPriorities().entrySet()) {
            String group = entry.getKey();
            if (PermissionUtil.hasPermission(player, "neoessentials.group." + group)) {
                return group;
            }
        }
        
        // Default group
        return "default";
    }
    
    /**
     * Reload team configuration
     * @param config The new configuration
     */
    public void reload(TABConfig config) {
        shutdown();
        initialize(config);
    }
    
    /**
     * Clean up all teams
     */
    public void shutdown() {
        if (server != null) {
            Scoreboard scoreboard = server.getScoreboard();
            for (PlayerTeam team : teams.values()) {
                scoreboard.removePlayerTeam(team);
            }
        }
        
        teams.clear();
        NeoEssentials.LOGGER.info("TeamManager shutdown");
    }
}
