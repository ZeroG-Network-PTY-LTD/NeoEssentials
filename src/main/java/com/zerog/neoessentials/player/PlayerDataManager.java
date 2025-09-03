package com.zerog.neoessentials.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Player data management system for NeoEssentials
 * 
 * @deprecated Use com.zerog.neoessentials.storage.PlayerDataManager instead
 * This class is kept for backward compatibility but delegates to the optimized storage implementation
 * 
 * @author ZeroG
 * @since 2.1.0
 */
@Deprecated
public class PlayerDataManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataManager.class);
    private static PlayerDataManager instance;
    
    private final com.zerog.neoessentials.storage.PlayerDataManager storageManager;
    
    private PlayerDataManager() {
        this.storageManager = com.zerog.neoessentials.storage.PlayerDataManager.getInstance();
        LOGGER.warn("PlayerDataManager in player package is deprecated. Use storage.PlayerDataManager instead.");
    }
    
    public static PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }
    
    /**
     * @deprecated Use com.zerog.neoessentials.storage.PlayerDataManager.getInstance().getPlayerData(playerId)
     */
    @Deprecated
    public PlayerData getPlayerData(java.util.UUID playerId) {
        com.zerog.neoessentials.storage.PlayerDataManager.PlayerData storagePlayerData = storageManager.getPlayerData(playerId);
        
        // Convert to legacy format if needed
        PlayerData legacyData = new PlayerData();
        legacyData.setPlayerId(playerId);
        legacyData.setPlayerName("Unknown"); // Will be filled from server data
        legacyData.setBalance(storagePlayerData.balance);
        // Add other conversions as needed
        
        return legacyData;
    }
    
    /**
     * @deprecated Use com.zerog.neoessentials.storage.PlayerDataManager.getInstance().savePlayerData(playerId, data)
     */
    @Deprecated
    public void savePlayerData(java.util.UUID playerId, PlayerData data) {
        // Save using storage manager
        storageManager.savePlayerData(playerId);
    }
    
    /**
     * @deprecated Single parameter version for backward compatibility
     */
    @Deprecated
    public void savePlayerData(PlayerData data) {
        if (data.getPlayerUUID() != null) {
            savePlayerData(data.getPlayerUUID(), data);
        }
    }
    
    /**
     * @deprecated Update player data in storage
     */
    @Deprecated
    public void updatePlayerData(PlayerData data) {
        savePlayerData(data);
    }
    
    /**
     * @deprecated Load player data from storage
     */
    @Deprecated
    public PlayerData loadPlayerData(java.util.UUID playerId) {
        storageManager.loadPlayerDataSync(playerId);
        return getPlayerData(playerId);
    }
    
    /**
     * @deprecated Get top players by playtime
     */
    @Deprecated
    public List<PlayerData> getTopPlayersByPlaytime(int limit) {
        // Return empty list for now - this would require implementing playtime tracking
        LOGGER.warn("getTopPlayersByPlaytime() is deprecated and not fully implemented");
        return new ArrayList<>();
    }
    
    /**
     * @deprecated Save all player data
     */
    @Deprecated
    public void saveAllPlayerData() {
        LOGGER.warn("saveAllPlayerData() is deprecated - storage manager handles this automatically");
    }
    
    /**
     * @deprecated Use com.zerog.neoessentials.storage.PlayerDataManager.getInstance() methods
     */
    @Deprecated
    public java.util.Map<java.util.UUID, PlayerData> getAllPlayerData() {
        // Return empty map to avoid breaking existing code
        LOGGER.warn("getAllPlayerData() is deprecated - use storage.PlayerDataManager methods");
        return new java.util.HashMap<>();
    }
}
