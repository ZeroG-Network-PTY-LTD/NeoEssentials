package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import com.zerog.neoessentials.player.PlayerData;
import com.zerog.neoessentials.player.PlayerDataManager;
import com.zerog.neoessentials.util.DebugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler for tracking player statistics across all features
 * Ensures consistent data handling and automatic stat updates
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class StatisticsEventHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsEventHandler.class);
    private static StatisticsEventHandler instance;
    
    private final PlayerDataManager playerDataManager;
    
    private StatisticsEventHandler() {
        this.playerDataManager = PlayerDataManager.getInstance();
        NeoForge.EVENT_BUS.register(this);
        DebugUtil.debugLog("[StatisticsEventHandler] Registered for event tracking");
    }
    
    public static StatisticsEventHandler getInstance() {
        if (instance == null) {
            instance = new StatisticsEventHandler();
        }
        return instance;
    }
    
    /**
     * Track player deaths
     */
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                PlayerData playerData = playerDataManager.getPlayerData(player.getUUID());
                playerData.incrementStatistic("player_deaths", 1);
                
                // Track death cause
                String deathCause = event.getSource().typeHolder().toString();
                playerData.incrementStatistic("death_" + deathCause, 1);
                
                playerDataManager.updatePlayerData(playerData);
                
                // Achievement system removed - keeping only statistics tracking
                
                DebugUtil.debugLog("[Statistics] Player " + player.getName().getString() + " died. Total deaths: " + 
                                  playerData.getStatistic("player_deaths"));
            } catch (Exception e) {
                LOGGER.error("Error tracking player death for {}", player.getName().getString(), e);
            }
        }
        
        // Track player kills (if player killed another player)
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            try {
                PlayerData killerData = playerDataManager.getPlayerData(killer.getUUID());
                
                if (event.getEntity() instanceof ServerPlayer) {
                    // Player vs Player kill
                    killerData.incrementStatistic("player_kills", 1);
                    killerData.incrementStatistic("pvp_kills", 1);
                } else {
                    // Player vs Entity kill
                    killerData.incrementStatistic("mob_kills", 1);
                    String entityType = event.getEntity().getType().toString();
                    killerData.incrementStatistic("kill_" + entityType, 1);
                }
                
                playerDataManager.updatePlayerData(killerData);
                
                // Check achievements after kill update
                // Achievement system removed
                
                DebugUtil.debugLog("[Statistics] Player " + killer.getName().getString() + " killed " + 
                                  event.getEntity().getType().toString() + ". Total kills: " + 
                                  killerData.getStatistic("player_kills"));
            } catch (Exception e) {
                LOGGER.error("Error tracking kill for {}", killer.getName().getString(), e);
            }
        }
    }
    
    /**
     * Track block breaking
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            try {
                PlayerData playerData = playerDataManager.getPlayerData(player.getUUID());
                playerData.incrementStatistic("blocks_broken", 1);
                
                // Track specific block types
                String blockType = event.getState().getBlock().toString();
                playerData.incrementStatistic("break_" + blockType, 1);
                
                playerDataManager.updatePlayerData(playerData);
                
                // Check achievements after block break update
                // Achievement system removed
                
                if (playerData.getStatistic("blocks_broken") instanceof Number blocks) {
                    if (blocks.intValue() % 100 == 0) { // Log every 100 blocks
                        DebugUtil.debugLog("[Statistics] Player " + player.getName().getString() + 
                                          " broke " + blocks + " blocks total");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error tracking block break for {}", player.getName().getString(), e);
            }
        }
    }
    
    /**
     * Track block placing
     */
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                PlayerData playerData = playerDataManager.getPlayerData(player.getUUID());
                playerData.incrementStatistic("blocks_placed", 1);
                
                // Track specific block types
                String blockType = event.getPlacedBlock().getBlock().toString();
                playerData.incrementStatistic("place_" + blockType, 1);
                
                playerDataManager.updatePlayerData(playerData);
                
                // Check achievements after block place update
                // Achievement system removed
                
                if (playerData.getStatistic("blocks_placed") instanceof Number blocks) {
                    if (blocks.intValue() % 100 == 0) { // Log every 100 blocks
                        DebugUtil.debugLog("[Statistics] Player " + player.getName().getString() + 
                                          " placed " + blocks + " blocks total");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error tracking block place for {}", player.getName().getString(), e);
            }
        }
    }
    
    /**
     * Initialize player statistics on first join
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                PlayerData playerData = playerDataManager.getPlayerData(player.getUUID());
                
                // Initialize statistics if they don't exist
                initializePlayerStatistics(playerData);
                
                playerDataManager.updatePlayerData(playerData);
                
                // Check and award automatic achievements
                // Achievement system removed
                
                DebugUtil.debugLog("[Statistics] Initialized statistics for player: " + 
                                  player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Error initializing statistics for {}", player.getName().getString(), e);
            }
        }
    }
    
    /**
     * Initialize default statistics for a player
     */
    private void initializePlayerStatistics(PlayerData playerData) {
        // Core statistics
        if (playerData.getStatistic("player_kills") == null) {
            playerData.setStatistic("player_kills", 0);
        }
        if (playerData.getStatistic("player_deaths") == null) {
            playerData.setStatistic("player_deaths", 0);
        }
        if (playerData.getStatistic("mob_kills") == null) {
            playerData.setStatistic("mob_kills", 0);
        }
        if (playerData.getStatistic("pvp_kills") == null) {
            playerData.setStatistic("pvp_kills", 0);
        }
        if (playerData.getStatistic("blocks_broken") == null) {
            playerData.setStatistic("blocks_broken", 0);
        }
        if (playerData.getStatistic("blocks_placed") == null) {
            playerData.setStatistic("blocks_placed", 0);
        }
        if (playerData.getStatistic("distance_traveled") == null) {
            playerData.setStatistic("distance_traveled", 0.0);
        }
        if (playerData.getStatistic("jumps") == null) {
            playerData.setStatistic("jumps", 0);
        }
        if (playerData.getStatistic("items_crafted") == null) {
            playerData.setStatistic("items_crafted", 0);
        }
        if (playerData.getStatistic("damage_dealt") == null) {
            playerData.setStatistic("damage_dealt", 0.0);
        }
        if (playerData.getStatistic("damage_taken") == null) {
            playerData.setStatistic("damage_taken", 0.0);
        }
        
        // Session statistics
        if (playerData.getStatistic("session_kills") == null) {
            playerData.setStatistic("session_kills", 0);
        }
        if (playerData.getStatistic("session_deaths") == null) {
            playerData.setStatistic("session_deaths", 0);
        }
        if (playerData.getStatistic("session_blocks_broken") == null) {
            playerData.setStatistic("session_blocks_broken", 0);
        }
        if (playerData.getStatistic("session_blocks_placed") == null) {
            playerData.setStatistic("session_blocks_placed", 0);
        }
    }
    
    /**
     * Reset session statistics when player joins
     */
    public void resetSessionStatistics(PlayerData playerData) {
        playerData.setStatistic("session_kills", 0);
        playerData.setStatistic("session_deaths", 0);
        playerData.setStatistic("session_blocks_broken", 0);
        playerData.setStatistic("session_blocks_placed", 0);
    }
    
    /**
     * Get comprehensive statistics summary for a player
     */
    public StatisticsSummary getPlayerStatistics(ServerPlayer player) {
        PlayerData playerData = playerDataManager.getPlayerData(player.getUUID());
        
        return new StatisticsSummary(
            player.getUUID(),
            player.getName().getString(),
            getStatAsInt(playerData, "player_kills"),
            getStatAsInt(playerData, "player_deaths"),
            getStatAsInt(playerData, "mob_kills"),
            getStatAsInt(playerData, "pvp_kills"),
            getStatAsInt(playerData, "blocks_broken"),
            getStatAsInt(playerData, "blocks_placed"),
            getStatAsDouble(playerData, "distance_traveled"),
            getStatAsInt(playerData, "jumps"),
            getStatAsInt(playerData, "items_crafted"),
            getStatAsDouble(playerData, "damage_dealt"),
            getStatAsDouble(playerData, "damage_taken"),
            calculateKDR(playerData),
            calculateBlockRatio(playerData)
        );
    }
    
    /**
     * Helper methods for safe statistic retrieval
     */
    private int getStatAsInt(PlayerData playerData, String key) {
        Object stat = playerData.getStatistic(key);
        return stat instanceof Number ? ((Number) stat).intValue() : 0;
    }
    
    private double getStatAsDouble(PlayerData playerData, String key) {
        Object stat = playerData.getStatistic(key);
        return stat instanceof Number ? ((Number) stat).doubleValue() : 0.0;
    }
    
    private double calculateKDR(PlayerData playerData) {
        int kills = getStatAsInt(playerData, "player_kills");
        int deaths = getStatAsInt(playerData, "player_deaths");
        return deaths > 0 ? (double) kills / deaths : kills;
    }
    
    private double calculateBlockRatio(PlayerData playerData) {
        int broken = getStatAsInt(playerData, "blocks_broken");
        int placed = getStatAsInt(playerData, "blocks_placed");
        return placed > 0 ? (double) broken / placed : broken;
    }
    
    /**
     * Statistics summary data class
     */
    public static class StatisticsSummary {
        private final java.util.UUID playerUUID;
        private final String playerName;
        private final int playerKills;
        private final int playerDeaths;
        private final int mobKills;
        private final int pvpKills;
        private final int blocksBroken;
        private final int blocksPlaced;
        private final double distanceTraveled;
        private final int jumps;
        private final int itemsCrafted;
        private final double damageDealt;
        private final double damageTaken;
        private final double kdr;
        private final double blockRatio;
        
        public StatisticsSummary(java.util.UUID playerUUID, String playerName, int playerKills, 
                               int playerDeaths, int mobKills, int pvpKills, int blocksBroken, 
                               int blocksPlaced, double distanceTraveled, int jumps, int itemsCrafted,
                               double damageDealt, double damageTaken, double kdr, double blockRatio) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.playerKills = playerKills;
            this.playerDeaths = playerDeaths;
            this.mobKills = mobKills;
            this.pvpKills = pvpKills;
            this.blocksBroken = blocksBroken;
            this.blocksPlaced = blocksPlaced;
            this.distanceTraveled = distanceTraveled;
            this.jumps = jumps;
            this.itemsCrafted = itemsCrafted;
            this.damageDealt = damageDealt;
            this.damageTaken = damageTaken;
            this.kdr = kdr;
            this.blockRatio = blockRatio;
        }
        
        // Getters
        public java.util.UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public int getPlayerKills() { return playerKills; }
        public int getPlayerDeaths() { return playerDeaths; }
        public int getMobKills() { return mobKills; }
        public int getPvpKills() { return pvpKills; }
        public int getBlocksBroken() { return blocksBroken; }
        public int getBlocksPlaced() { return blocksPlaced; }
        public double getDistanceTraveled() { return distanceTraveled; }
        public int getJumps() { return jumps; }
        public int getItemsCrafted() { return itemsCrafted; }
        public double getDamageDealt() { return damageDealt; }
        public double getDamageTaken() { return damageTaken; }
        public double getKdr() { return kdr; }
        public double getBlockRatio() { return blockRatio; }
        
        public String getFormattedKDR() {
            return String.format("%.2f", kdr);
        }
        
        public String getFormattedBlockRatio() {
            return String.format("%.2f", blockRatio);
        }
        
        public String getFormattedDistance() {
            return String.format("%.1f blocks", distanceTraveled);
        }
    }
}
