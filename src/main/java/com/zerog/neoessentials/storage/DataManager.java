package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.economy.PlayerEconomyData;
import com.zerog.neoessentials.economy.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Basic data storage manager for economy system
 * In a full implementation, this would use database or file storage
 */
public class DataManager {
    private static DataManager instance;
    
    // In-memory storage (would be replaced with actual storage)
    private final Map<UUID, PlayerEconomyData> playerEconomyData;
    private final Map<String, Transaction> transactionHistory;
    
    private DataManager() {
        this.playerEconomyData = new HashMap<>();
        this.transactionHistory = new HashMap<>();
    }
    
    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }
    
    // Player economy data methods
    public void savePlayerEconomyData(PlayerEconomyData data) {
        playerEconomyData.put(data.getPlayerId(), data);
    }
    
    public PlayerEconomyData loadPlayerEconomyData(UUID playerId) {
        return playerEconomyData.get(playerId);
    }
    
    public Map<UUID, PlayerEconomyData> loadAllPlayerEconomyData() {
        return new HashMap<>(playerEconomyData);
    }
    
    // Transaction methods
    public void saveTransaction(Transaction transaction) {
        transactionHistory.put(transaction.getTransactionId(), transaction);
    }
    
    public Transaction loadTransaction(String transactionId) {
        return transactionHistory.get(transactionId);
    }
    
    public Map<String, Transaction> loadTransactionHistory() {
        return new HashMap<>(transactionHistory);
    }
}
