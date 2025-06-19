package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manages the economy system for the NeoEssentials mod.
 */
public class EconomyManager {
    private static final String ECONOMY_DATA_FILE = "neoessentials/economy.json";
    private static final String TRANSACTION_DATA_FILE = "neoessentials/economy_transactions.json";
    
    // Currency configuration
    private String currencyName = "Coins";
    private String currencySingular = "Coin";
    private String currencySymbol = "$";
    private double startingBalance = 100.0;
      // Map of player UUID to balance
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    
    // Map of player UUID to player name (for lookup)
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();
    
    // List of recent transactions (in-memory cache)
    private final List<EconomyTransaction> recentTransactions = Collections.synchronizedList(new ArrayList<>());
    
    // Transaction ID counter
    private long nextTransactionId = 1;
    
    // JSON serialization
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
      /**
     * Initialize the economy manager
     */    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Economy Manager");
        
        // Create the data directory if it doesn't exist
        File dataFolder = new File("neoessentials");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            NeoEssentials.LOGGER.error("Failed to create neoessentials directory");
        }
        
        // Load existing economy data
        loadEconomyData();
        
        // Load transaction history
        loadTransactionHistory();
    }
      /**
     * Save all economy data to disk
     */
    public void saveAll() {
        NeoEssentials.LOGGER.info("Saving economy data");
        
        try {
            File dataFolder = new File("neoessentials");
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create neoessentials directory");
                return;
            }
            
            File dataFile = new File(ECONOMY_DATA_FILE);
            if (!dataFile.exists()) {
                if (!dataFile.createNewFile()) {
                    NeoEssentials.LOGGER.error("Failed to create economy data file");
                    return;
                }
            }
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(balances, writer);
            }
            
            NeoEssentials.LOGGER.info("Economy data saved successfully");
            
            // Also save transaction history
            saveTransactionHistory();
            
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving economy data", e);
        }
    }
    
    /**
     * Load economy data from disk
     */
    @SuppressWarnings("unchecked")
    private void loadEconomyData() {
        File dataFile = new File(ECONOMY_DATA_FILE);
        
        if (!dataFile.exists()) {
            NeoEssentials.LOGGER.info("No existing economy data found, starting fresh");
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            Map<String, Double> rawData = gson.fromJson(reader, Map.class);
            
            if (rawData != null) {
                // Convert the string keys back to UUIDs
                rawData.forEach((key, value) -> {
                    try {
                        UUID uuid = UUID.fromString(key);
                        balances.put(uuid, value);
                    } catch (IllegalArgumentException e) {
                        NeoEssentials.LOGGER.error("Invalid UUID in economy data: " + key);
                    }
                });
                
                NeoEssentials.LOGGER.info("Loaded " + balances.size() + " player economy records");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading economy data", e);
        }
    }
    
    /**
     * Load transaction history from disk
     */
    private void loadTransactionHistory() {
        File transactionFile = new File(TRANSACTION_DATA_FILE);
        
        if (!transactionFile.exists()) {
            NeoEssentials.LOGGER.info("No existing transaction history found");
            return;
        }
        
        try (FileReader reader = new FileReader(transactionFile)) {
            Type listType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
            ArrayList<Map<String, Object>> rawData = gson.fromJson(reader, listType);
            
            if (rawData != null) {
                for (Map<String, Object> rawTransaction : rawData) {
                    try {
                        long id = ((Number) rawTransaction.get("id")).longValue();
                        UUID playerUUID = UUID.fromString((String) rawTransaction.get("playerUUID"));
                        
                        UUID otherPlayerUUID = null;
                        if (rawTransaction.get("otherPlayerUUID") != null) {
                            otherPlayerUUID = UUID.fromString((String) rawTransaction.get("otherPlayerUUID"));
                        }
                        
                        String type = (String) rawTransaction.get("type");
                        double amount = ((Number) rawTransaction.get("amount")).doubleValue();
                        double balanceAfter = ((Number) rawTransaction.get("balanceAfter")).doubleValue();
                        String description = (String) rawTransaction.get("description");
                        long timestamp = ((Number) rawTransaction.get("timestamp")).longValue();
                        
                        EconomyTransaction transaction = new EconomyTransaction(
                            id, playerUUID, otherPlayerUUID, type, amount, balanceAfter, description, timestamp
                        );
                        
                        recentTransactions.add(transaction);
                        
                        // Update the next transaction ID
                        if (id >= nextTransactionId) {
                            nextTransactionId = id + 1;
                        }
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.error("Error parsing transaction: " + rawTransaction, e);
                    }
                }
                
                NeoEssentials.LOGGER.info("Loaded " + recentTransactions.size() + " transactions");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading transaction history", e);
        }
    }
    
    /**
     * Save transaction history to disk
     */
    private void saveTransactionHistory() {
        try {
            File dataFile = new File(TRANSACTION_DATA_FILE);
            
            if (!dataFile.exists()) {
                if (!dataFile.createNewFile()) {
                    NeoEssentials.LOGGER.error("Failed to create transaction history file");
                    return;
                }
            }
            
            // Limit to most recent transactions to avoid huge file
            List<EconomyTransaction> transactionsToSave;
            synchronized (recentTransactions) {
                transactionsToSave = recentTransactions.size() > 1000 
                    ? recentTransactions.subList(recentTransactions.size() - 1000, recentTransactions.size()) 
                    : new ArrayList<>(recentTransactions);
            }
            
            // Convert to maps for serialization
            List<Map<String, Object>> serializedTransactions = new ArrayList<>();
            for (EconomyTransaction transaction : transactionsToSave) {
                Map<String, Object> serialized = new HashMap<>();
                serialized.put("id", transaction.getId());
                serialized.put("playerUUID", transaction.getPlayerUUID().toString());
                
                if (transaction.getOtherPlayerUUID() != null) {
                    serialized.put("otherPlayerUUID", transaction.getOtherPlayerUUID().toString());
                }
                
                serialized.put("type", transaction.getType());
                serialized.put("amount", transaction.getAmount());
                serialized.put("balanceAfter", transaction.getBalanceAfter());
                serialized.put("description", transaction.getDescription());
                serialized.put("timestamp", transaction.getTimestamp());
                
                serializedTransactions.add(serialized);
            }
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(serializedTransactions, writer);
            }
            
            NeoEssentials.LOGGER.info("Transaction history saved successfully");
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving transaction history", e);
        }
    }
    
    /**
     * Record a transaction and update the player's balance
     * 
     * @param playerUUID The UUID of the player
     * @param type The transaction type
     * @param amount The transaction amount
     * @param description The transaction description
     * @return The transaction object that was created
     */
    public EconomyTransaction recordTransaction(UUID playerUUID, String type, double amount, String description) {
        return recordTransaction(playerUUID, null, type, amount, description);
    }
    
    /**
     * Record a transaction and update the player's balance
     * 
     * @param playerUUID The UUID of the player
     * @param otherPlayerUUID The UUID of the other player (for transfers)
     * @param type The transaction type
     * @param amount The transaction amount
     * @param description The transaction description
     * @return The transaction object that was created
     */
    public EconomyTransaction recordTransaction(UUID playerUUID, UUID otherPlayerUUID, String type, double amount, String description) {
        // Update the balance based on transaction type
        double oldBalance = getBalance(playerUUID);
        double newBalance = oldBalance;
        
        if (EconomyTransaction.TYPE_DEPOSIT.equals(type) || EconomyTransaction.TYPE_TRANSFER_RECEIVE.equals(type)) {
            newBalance += amount;
            setBalance(playerUUID, newBalance);
        } else if (EconomyTransaction.TYPE_WITHDRAW.equals(type) || EconomyTransaction.TYPE_TRANSFER_SEND.equals(type)) {
            newBalance -= amount;
            setBalance(playerUUID, newBalance);
        } else if (EconomyTransaction.TYPE_ADMIN.equals(type)) {
            // Admin transactions specify the absolute change, which could be positive or negative
            newBalance += amount;
            setBalance(playerUUID, newBalance);
        }
        
        // Create and record the transaction
        long id;
        synchronized (this) {
            id = nextTransactionId++;
        }
        
        EconomyTransaction transaction = new EconomyTransaction(
            id, playerUUID, otherPlayerUUID, type, amount, newBalance, description
        );
        
        synchronized (recentTransactions) {
            recentTransactions.add(transaction);
            
            // Trim the list if it gets too large to prevent memory issues
            if (recentTransactions.size() > 5000) {
                recentTransactions.subList(0, 1000).clear();
            }
        }
        
        // Save the transaction history periodically
        if (recentTransactions.size() % 10 == 0) {
            saveTransactionHistory();
        }
        
        return transaction;
    }
    
    /**
     * Get recent transactions for a player
     * 
     * @param playerUUID The UUID of the player
     * @param limit Maximum number of transactions to return
     * @return List of recent transactions
     */
    public List<EconomyTransaction> getRecentTransactions(UUID playerUUID, int limit) {
        synchronized (recentTransactions) {
            return recentTransactions.stream()
                .filter(t -> t.getPlayerUUID().equals(playerUUID))
                .sorted(Comparator.comparing(EconomyTransaction::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Get all transactions for a player
     * 
     * @param playerUUID The UUID of the player
     * @return List of all transactions
     */
    public List<EconomyTransaction> getAllTransactions(UUID playerUUID) {
        synchronized (recentTransactions) {
            return recentTransactions.stream()
                .filter(t -> t.getPlayerUUID().equals(playerUUID))
                .sorted(Comparator.comparing(EconomyTransaction::getTimestamp))
                .collect(Collectors.toList());
        }
    }
      /**
     * Get the balance of a player
     * 
     * @param playerId The UUID of the player
     * @return The player's balance
     */
    public double getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, startingBalance);
    }
    
    /**
     * Set the balance of a player
     * 
     * @param playerId The UUID of the player
     * @param amount The new balance amount
     */
    public void setBalance(UUID playerId, double amount) {
        if (amount < 0) {
            amount = 0;
        }
        balances.put(playerId, amount);
    }
    
    /**
     * Add to a player's balance
     * 
     * @param playerId The UUID of the player
     * @param amount The amount to add
     * @return The new balance
     */
    public double addBalance(UUID playerId, double amount) {
        if (amount <= 0) {
            return getBalance(playerId);
        }
        
        recordTransaction(playerId, EconomyTransaction.TYPE_DEPOSIT, amount, "Balance deposit");
        return getBalance(playerId);
    }
      /**
     * Remove from a player's balance
     * 
     * @param playerId The UUID of the player
     * @param amount The amount to remove
     * @return True if the player had enough money, false otherwise
     */
    public boolean removeBalance(UUID playerId, double amount) {
        if (amount <= 0) {
            return true;
        }
        
        double currentBalance = getBalance(playerId);
        if (currentBalance < amount) {
            return false;
        }
        
        recordTransaction(playerId, EconomyTransaction.TYPE_WITHDRAW, amount, "Balance withdrawal");
        return true;
    }
      /**
     * Transfer money from one player to another
     * 
     * @param fromId The UUID of the player sending money
     * @param toId The UUID of the player receiving money
     * @param amount The amount to transfer
     * @return True if the transfer was successful, false otherwise
     */    
    public boolean transfer(UUID fromId, UUID toId, double amount) {
        if (amount <= 0 || fromId.equals(toId)) {
            return false;
        }
        
        double fromBalance = getBalance(fromId);
        if (fromBalance < amount) {
            return false;
        }
        
        // Get player names for better descriptions
        String fromName = getPlayerName(fromId);
        String toName = getPlayerName(toId);
        
        // Record the send transaction
        recordTransaction(fromId, toId, EconomyTransaction.TYPE_TRANSFER_SEND, amount, 
                "Transfer to " + toName);
                
        // Record the receive transaction
        recordTransaction(toId, fromId, EconomyTransaction.TYPE_TRANSFER_RECEIVE, amount, 
                "Transfer from " + fromName);
                
        return true;
    }
      /**
     * Gets a list of top balances for the baltop command
     * 
     * @param limit The maximum number of entries to return
     * @return A map of UUIDs to balances, sorted by balance descending
     */
    public Map<UUID, Double> getTopBalances(int limit) {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Get all balances
     * 
     * @return Map of player UUIDs to their balances
     */
    public Map<UUID, Double> getAllBalances() {
        return new ConcurrentHashMap<>(balances);
    }
      /**
     * Get a player's name from their UUID
     * 
     * @param uuid The player's UUID
     * @return The player's name, or "Unknown Player" if not found
     */
    public String getPlayerName(UUID uuid) {
        // Check if the server is available
        if (NeoEssentials.getInstance() == null || NeoEssentials.getInstance().getServer() == null) {
            return "Unknown Player";
        }
        
        // Try to get from online players first
        for (net.minecraft.server.level.ServerPlayer player : NeoEssentials.getInstance().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(uuid)) {
                return player.getName().getString();
            }
        }
          // Try to get from profile cache
        var profileCache = NeoEssentials.getInstance().getServer().getProfileCache();
        if (profileCache != null) {
            var profileOpt = profileCache.get(uuid);
            if (profileOpt.isPresent()) {
                return profileOpt.get().getName();
            }
        }
        
        // Return unknown if all else fails
        return "Unknown Player";
    }
    
    /**
     * Format a currency amount according to the configured format
     * 
     * @param amount The amount to format
     * @return The formatted amount
     */
    public String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        format.setCurrency(java.util.Currency.getInstance("USD"));
        return format.format(amount).replace("$", currencySymbol);
    }
    
    /**
     * Format a currency amount with the currency name
     * 
     * @param amount The amount to format
     * @return The formatted amount with currency name
     */
    public String formatCurrencyWithName(double amount) {
        String formatted = String.format("%.2f", amount);
        if (Math.abs(amount - 1.0) < 0.009) {
            return formatted + " " + currencySingular;
        } else {
            return formatted + " " + currencyName;
        }
    }
    
    /**
     * Record an admin transaction to add money to a player's account
     * 
     * @param playerUUID The UUID of the player
     * @param adminUUID The UUID of the admin
     * @param amount The amount to add
     * @param description The description of the transaction
     * @return The new balance
     */
    public double adminAddBalance(UUID playerUUID, UUID adminUUID, double amount, String description) {
        if (amount <= 0) {
            return getBalance(playerUUID);
        }
        
        double oldBalance = getBalance(playerUUID);
        double newBalance = oldBalance + amount;
        
        setBalance(playerUUID, newBalance);
        
        // Record the transaction with admin type
        EconomyTransaction transaction = new EconomyTransaction(
            playerUUID, adminUUID, EconomyTransaction.TYPE_ADMIN, amount, newBalance, description
        );
        
        synchronized (recentTransactions) {
            recentTransactions.add(transaction);
            
            // Trim the list if it gets too large to prevent memory issues
            if (recentTransactions.size() > 5000) {
                recentTransactions.subList(0, 1000).clear();
            }
        }
        
        // Save the transaction history periodically
        if (recentTransactions.size() % 10 == 0) {
            saveTransactionHistory();
        }
        
        return newBalance;
    }
    
    /**
     * Record an admin transaction to remove money from a player's account
     * 
     * @param playerUUID The UUID of the player
     * @param adminUUID The UUID of the admin
     * @param amount The amount to remove
     * @param description The description of the transaction
     * @return True if successful, false if the player doesn't have enough funds
     */
    public boolean adminRemoveBalance(UUID playerUUID, UUID adminUUID, double amount, String description) {
        if (amount <= 0) {
            return true;
        }
        
        double oldBalance = getBalance(playerUUID);
        if (oldBalance < amount) {
            return false;
        }
        
        double newBalance = oldBalance - amount;
        setBalance(playerUUID, newBalance);
        
        // Record the transaction with admin type
        EconomyTransaction transaction = new EconomyTransaction(
            playerUUID, adminUUID, EconomyTransaction.TYPE_ADMIN, -amount, newBalance, description
        );
        
        synchronized (recentTransactions) {
            recentTransactions.add(transaction);
            
            // Trim the list if it gets too large to prevent memory issues
            if (recentTransactions.size() > 5000) {
                recentTransactions.subList(0, 1000).clear();
            }
        }
        
        // Save the transaction history periodically
        if (recentTransactions.size() % 10 == 0) {
            saveTransactionHistory();
        }
        
        return true;
    }
    
    /**
     * Record an admin transaction to set a player's balance
     * 
     * @param playerUUID The UUID of the player
     * @param adminUUID The UUID of the admin
     * @param amount The amount to set
     * @param description The description of the transaction
     * @return The new balance
     */
    public double adminSetBalance(UUID playerUUID, UUID adminUUID, double amount, String description) {
        double oldBalance = getBalance(playerUUID);
        double difference = amount - oldBalance;
        
        setBalance(playerUUID, amount);
        
        // Record the transaction with admin type
        EconomyTransaction transaction = new EconomyTransaction(
            playerUUID, adminUUID, EconomyTransaction.TYPE_ADMIN, difference, amount, description
        );
        
        synchronized (recentTransactions) {
            recentTransactions.add(transaction);
            
            // Trim the list if it gets too large to prevent memory issues
            if (recentTransactions.size() > 5000) {
                recentTransactions.subList(0, 1000).clear();
            }
        }
        
        // Save the transaction history periodically
        if (recentTransactions.size() % 10 == 0) {
            saveTransactionHistory();
        }
          return amount;
    }
    
    /**
     * Gets the total amount of currency in the economy
     * 
     * @return The total amount of currency
     */
    public double getTotalCurrency() {
        double total = 0.0;
        for (Double balance : balances.values()) {
            total += balance;
        }
        return total;
    }
    
    /**
     * Gets the number of player accounts in the economy
     * 
     * @return The number of accounts
     */
    public int getTotalAccounts() {
        return balances.size();
    }
  }
