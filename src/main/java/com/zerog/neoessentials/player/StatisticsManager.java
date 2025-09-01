package com.zerog.neoessentials.player;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.util.DebugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central statistics management system for NeoEssentials
 * Coordinates between PlayerData, StatisticsEventHandler, and other systems
 * Provides consistent data handling across all features
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class StatisticsManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsManager.class);
    private static StatisticsManager instance;
    
    private final PlayerDataManager playerDataManager;
    private final Map<String, StatisticDefinition> statisticDefinitions;
    
    private StatisticsManager() {
        this.playerDataManager = PlayerDataManager.getInstance();
        this.statisticDefinitions = new ConcurrentHashMap<>();
        
        initializeStatisticDefinitions();
        LOGGER.info("[StatisticsManager] Initialized with " + statisticDefinitions.size() + " statistic definitions");
    }
    
    public static StatisticsManager getInstance() {
        if (instance == null) {
            instance = new StatisticsManager();
        }
        return instance;
    }
    
    /**
     * Initialize all standard statistic definitions
     */
    private void initializeStatisticDefinitions() {
        // Combat statistics
        addStatistic("player_kills", "Player Kills", StatisticType.INTEGER, StatisticCategory.COMBAT);
        addStatistic("player_deaths", "Player Deaths", StatisticType.INTEGER, StatisticCategory.COMBAT);
        addStatistic("mob_kills", "Mob Kills", StatisticType.INTEGER, StatisticCategory.COMBAT);
        addStatistic("pvp_kills", "PvP Kills", StatisticType.INTEGER, StatisticCategory.COMBAT);
        addStatistic("damage_dealt", "Damage Dealt", StatisticType.DOUBLE, StatisticCategory.COMBAT);
        addStatistic("damage_taken", "Damage Taken", StatisticType.DOUBLE, StatisticCategory.COMBAT);
        
        // Building statistics
        addStatistic("blocks_broken", "Blocks Broken", StatisticType.INTEGER, StatisticCategory.BUILDING);
        addStatistic("blocks_placed", "Blocks Placed", StatisticType.INTEGER, StatisticCategory.BUILDING);
        
        // Movement statistics
        addStatistic("distance_traveled", "Distance Traveled", StatisticType.DOUBLE, StatisticCategory.MOVEMENT);
        addStatistic("jumps", "Jumps", StatisticType.INTEGER, StatisticCategory.MOVEMENT);
        
        // Crafting statistics
        addStatistic("items_crafted", "Items Crafted", StatisticType.INTEGER, StatisticCategory.CRAFTING);
        addStatistic("items_smelted", "Items Smelted", StatisticType.INTEGER, StatisticCategory.CRAFTING);
        addStatistic("items_enchanted", "Items Enchanted", StatisticType.INTEGER, StatisticCategory.CRAFTING);
        
        // Social statistics
        addStatistic("chat_messages", "Chat Messages", StatisticType.INTEGER, StatisticCategory.SOCIAL);
        addStatistic("commands_used", "Commands Used", StatisticType.INTEGER, StatisticCategory.SOCIAL);
        
        // Economic statistics
        addStatistic("money_earned", "Money Earned", StatisticType.DOUBLE, StatisticCategory.ECONOMY);
        addStatistic("money_spent", "Money Spent", StatisticType.DOUBLE, StatisticCategory.ECONOMY);
        addStatistic("trades_completed", "Trades Completed", StatisticType.INTEGER, StatisticCategory.ECONOMY);
        
        // Session statistics (reset on login)
        addStatistic("session_kills", "Session Kills", StatisticType.INTEGER, StatisticCategory.SESSION);
        addStatistic("session_deaths", "Session Deaths", StatisticType.INTEGER, StatisticCategory.SESSION);
        addStatistic("session_blocks_broken", "Session Blocks Broken", StatisticType.INTEGER, StatisticCategory.SESSION);
        addStatistic("session_blocks_placed", "Session Blocks Placed", StatisticType.INTEGER, StatisticCategory.SESSION);
    }
    
    /**
     * Add a statistic definition
     */
    private void addStatistic(String key, String displayName, StatisticType type, StatisticCategory category) {
        statisticDefinitions.put(key, new StatisticDefinition(key, displayName, type, category));
    }
    
    /**
     * Get player statistics with full validation
     */
    public PlayerStatistics getPlayerStatistics(UUID playerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        return new PlayerStatistics(playerUUID, playerData, statisticDefinitions);
    }
    
    /**
     * Get player statistics for ServerPlayer
     */
    public PlayerStatistics getPlayerStatistics(ServerPlayer player) {
        return getPlayerStatistics(player.getUUID());
    }
    
    /**
     * Increment a statistic for a player
     */
    public void incrementStatistic(UUID playerUUID, String statisticKey, Number amount) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        playerData.incrementStatistic(statisticKey, amount);
        playerDataManager.updatePlayerData(playerData);
    }
    
    /**
     * Set a statistic for a player
     */
    public void setStatistic(UUID playerUUID, String statisticKey, Object value) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        playerData.setStatistic(statisticKey, value);
        playerDataManager.updatePlayerData(playerData);
    }
    
    /**
     * Get a statistic value for a player
     */
    public Object getStatistic(UUID playerUUID, String statisticKey) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        return playerData.getStatistic(statisticKey);
    }
    
    /**
     * Get leaderboard for a specific statistic
     */
    public List<LeaderboardEntry> getLeaderboard(String statisticKey, int limit) {
        // This is a simplified implementation
        // In a production environment, you'd want to cache this data
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        
        // For now, return empty list as this requires iterating through all player files
        // which could be performance-intensive
        DebugUtil.debugLog("[StatisticsManager] Leaderboard requested for " + statisticKey + " (not implemented)");
        
        return leaderboard.stream()
               .limit(limit)
               .collect(Collectors.toList());
    }
    
    /**
     * Get all statistic categories
     */
    public Set<StatisticCategory> getCategories() {
        return statisticDefinitions.values().stream()
               .map(StatisticDefinition::getCategory)
               .collect(Collectors.toSet());
    }
    
    /**
     * Get statistics by category
     */
    public List<StatisticDefinition> getStatisticsByCategory(StatisticCategory category) {
        return statisticDefinitions.values().stream()
               .filter(def -> def.getCategory() == category)
               .collect(Collectors.toList());
    }
    
    /**
     * Check if a statistic is defined
     */
    public boolean isStatisticDefined(String key) {
        return statisticDefinitions.containsKey(key);
    }
    
    /**
     * Get statistic definition
     */
    public StatisticDefinition getStatisticDefinition(String key) {
        return statisticDefinitions.get(key);
    }
    
    /**
     * Statistic types
     */
    public enum StatisticType {
        INTEGER,
        LONG,
        DOUBLE,
        FLOAT,
        STRING,
        BOOLEAN
    }
    
    /**
     * Statistic categories for organization
     */
    public enum StatisticCategory {
        COMBAT("Combat"),
        BUILDING("Building"),
        MOVEMENT("Movement"),
        CRAFTING("Crafting"),
        SOCIAL("Social"),
        ECONOMY("Economy"),
        SESSION("Session"),
        CUSTOM("Custom");
        
        private final String displayName;
        
        StatisticCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Statistic definition class
     */
    public static class StatisticDefinition {
        private final String key;
        private final String displayName;
        private final StatisticType type;
        private final StatisticCategory category;
        
        public StatisticDefinition(String key, String displayName, StatisticType type, StatisticCategory category) {
            this.key = key;
            this.displayName = displayName;
            this.type = type;
            this.category = category;
        }
        
        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public StatisticType getType() { return type; }
        public StatisticCategory getCategory() { return category; }
    }
    
    /**
     * Player statistics wrapper class
     */
    public static class PlayerStatistics {
        private final UUID playerUUID;
        private final PlayerData playerData;
        private final Map<String, StatisticDefinition> definitions;
        
        public PlayerStatistics(UUID playerUUID, PlayerData playerData, Map<String, StatisticDefinition> definitions) {
            this.playerUUID = playerUUID;
            this.playerData = playerData;
            this.definitions = definitions;
        }
        
        public UUID getPlayerUUID() { return playerUUID; }
        
        public int getKills() { return playerData.getStatisticAsInt("player_kills", 0); }
        public int getDeaths() { return playerData.getStatisticAsInt("player_deaths", 0); }
        public int getMobKills() { return playerData.getStatisticAsInt("mob_kills", 0); }
        public int getPvpKills() { return playerData.getStatisticAsInt("pvp_kills", 0); }
        public int getBlocksBroken() { return playerData.getStatisticAsInt("blocks_broken", 0); }
        public int getBlocksPlaced() { return playerData.getStatisticAsInt("blocks_placed", 0); }
        public double getDistanceTraveled() { return playerData.getStatisticAsDouble("distance_traveled", 0.0); }
        public int getJumps() { return playerData.getStatisticAsInt("jumps", 0); }
        public int getItemsCrafted() { return playerData.getStatisticAsInt("items_crafted", 0); }
        public double getDamageDealt() { return playerData.getStatisticAsDouble("damage_dealt", 0.0); }
        public double getDamageTaken() { return playerData.getStatisticAsDouble("damage_taken", 0.0); }
        
        public double getKDR() { return playerData.getKDR(); }
        public String getFormattedKDR() { return playerData.getFormattedKDR(); }
        public int getTotalBlocksInteracted() { return playerData.getTotalBlocksInteracted(); }
        
        public Object getStatistic(String key) { return playerData.getStatistic(key); }
        public Map<String, Object> getAllStatistics() { return playerData.getStatistics(); }
        
        public Map<StatisticCategory, Map<String, Object>> getStatisticsByCategory() {
            Map<StatisticCategory, Map<String, Object>> categorized = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : playerData.getStatistics().entrySet()) {
                StatisticDefinition def = definitions.get(entry.getKey());
                if (def != null) {
                    categorized.computeIfAbsent(def.getCategory(), k -> new HashMap<>())
                             .put(entry.getKey(), entry.getValue());
                }
            }
            
            return categorized;
        }
    }
    
    /**
     * Leaderboard entry class
     */
    public static class LeaderboardEntry {
        private final UUID playerUUID;
        private final String playerName;
        private final Object value;
        private final int rank;
        
        public LeaderboardEntry(UUID playerUUID, String playerName, Object value, int rank) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.value = value;
            this.rank = rank;
        }
        
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public Object getValue() { return value; }
        public int getRank() { return rank; }
    }
}
