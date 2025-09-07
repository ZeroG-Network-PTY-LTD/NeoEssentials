package com.zerog.neoessentials.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

/**
 * Utility class for cleaning up all scoreboard and bossbar related content on server startup
 * 
 * This ensures that any previous scoreboard/bossbar content from NeoEssentials 
 * is completely removed when the server starts, preventing any conflicts or 
 * leftover display elements.
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ScoreboardCleanupUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreboardCleanupUtil.class);
    
    // Prefixes that identify NeoEssentials scoreboard/bossbar content
    private static final String[] NEOESSENTIALS_PREFIXES = {
        "neoessentials",
        "ne_",
        "neo_",
        "essentials_"
    };
    
    /**
     * Clean up all NeoEssentials-related scoreboard and bossbar content
     * Should be called during server startup
     */
    public static void cleanupAll(MinecraftServer server) {
        LOGGER.info("[NeoEssentials] Starting cleanup of scoreboard and bossbar content...");
        
        try {
            cleanupScoreboards(server);
            cleanupBossBars(server);
            
            LOGGER.info("[NeoEssentials] Successfully cleaned up all scoreboard and bossbar content");
        } catch (Exception e) {
            LOGGER.error("[NeoEssentials] Error during scoreboard/bossbar cleanup", e);
        }
    }
    
    /**
     * Remove all NeoEssentials-related scoreboards
     */
    private static void cleanupScoreboards(MinecraftServer server) {
        try {
            Scoreboard serverScoreboard = server.getScoreboard();
            
            // Remove all objectives that match NeoEssentials patterns
            Collection<Objective> objectives = serverScoreboard.getObjectives();
            Iterator<Objective> objectiveIterator = objectives.iterator();
            
            int removedObjectives = 0;
            while (objectiveIterator.hasNext()) {
                Objective objective = objectiveIterator.next();
                String objectiveName = objective.getName();
                String lowerName = objectiveName.toLowerCase();
                
                // Check if this objective belongs to NeoEssentials (prefix matching)
                boolean isNeoEssentialsObjective = false;
                for (String prefix : NEOESSENTIALS_PREFIXES) {
                    if (lowerName.startsWith(prefix)) {
                        isNeoEssentialsObjective = true;
                        break;
                    }
                }
                
                // Check for conditional placeholder patterns that might be from NeoEssentials
                if (!isNeoEssentialsObjective && (objectiveName.contains("%if:") || objectiveName.contains("Admin Board") || 
                    objectiveName.contains("Top Players") || objectiveName.startsWith("[%"))) {
                    isNeoEssentialsObjective = true;
                }
                
                if (isNeoEssentialsObjective) {
                    try {
                        serverScoreboard.removeObjective(objective);
                        removedObjectives++;
                        LOGGER.debug("[NeoEssentials] Removed scoreboard objective: {}", objective.getName());
                    } catch (Exception e) {
                        LOGGER.warn("[NeoEssentials] Failed to remove scoreboard objective: {}", objective.getName(), e);
                    }
                }
            }
            
            // Clear sidebar displays for all players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                try {
                    // Clear any sidebar scoreboard display
                    serverScoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR, null);
                    serverScoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.LIST, null);
                    serverScoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.BELOW_NAME, null);
                } catch (Exception e) {
                    LOGGER.warn("[NeoEssentials] Failed to clear scoreboard display for player: {}", player.getName().getString(), e);
                }
            }
            
            LOGGER.info("[NeoEssentials] Removed {} scoreboard objectives", removedObjectives);
            
        } catch (Exception e) {
            LOGGER.error("[NeoEssentials] Error cleaning up scoreboards", e);
        }
    }
    
    /**
     * Remove all NeoEssentials-related boss bars
     */
    private static void cleanupBossBars(MinecraftServer server) {
        try {
            // Get all custom boss events from the server
            var bossEvents = server.getCustomBossEvents();
            int removedBossBars = 0;
            
            // Create a list to avoid concurrent modification
            java.util.List<ResourceLocation> bossEventsToRemove = new java.util.ArrayList<>();
            
            for (var entry : bossEvents.getIds()) {
                String bossEventName = entry.toString().toLowerCase();
                
                // Check if this boss bar belongs to NeoEssentials
                boolean isNeoEssentialsBossBar = false;
                for (String prefix : NEOESSENTIALS_PREFIXES) {
                    if (bossEventName.contains(prefix)) {
                        isNeoEssentialsBossBar = true;
                        break;
                    }
                }
                
                if (isNeoEssentialsBossBar) {
                    bossEventsToRemove.add(entry);
                }
            }
            
            // Remove the identified boss events
            for (ResourceLocation bossEventId : bossEventsToRemove) {
                try {
                    CustomBossEvent bossEvent = bossEvents.get(bossEventId);
                    if (bossEvent != null) {
                        // Remove all players from the boss bar
                        bossEvent.removeAllPlayers();
                        // Remove the boss event itself
                        bossEvents.remove(bossEvent);
                        removedBossBars++;
                        LOGGER.debug("[NeoEssentials] Removed boss bar: {}", bossEventId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("[NeoEssentials] Failed to remove boss bar: {}", bossEventId, e);
                }
            }
            
            LOGGER.info("[NeoEssentials] Removed {} boss bars", removedBossBars);
            
        } catch (Exception e) {
            LOGGER.error("[NeoEssentials] Error cleaning up boss bars", e);
        }
    }
    
    /**
     * Clean up any remaining display elements for a specific player
     * Useful when a player joins to ensure clean slate
     */
    public static void cleanupPlayerDisplays(ServerPlayer player) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            
            Scoreboard serverScoreboard = server.getScoreboard();
            
            // Clear any scoreboard displays for this player
            serverScoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR, null);
            serverScoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.LIST, null);
            serverScoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.BELOW_NAME, null);
            
            LOGGER.debug("[NeoEssentials] Cleaned up display elements for player: {}", player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.warn("[NeoEssentials] Error cleaning up player displays for: {}", player.getName().getString(), e);
        }
    }
}
