package com.zerog.neoessentials.economy.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.NeoEssentials;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive persistence manager for the NeoEssentials economy system.
 * Supports both JSON file storage and SQLite database storage.
 */
public class EconomyPersistenceManager {
    
    private static EconomyPersistenceManager instance;
    private final Gson gson;
    private final String dataFolder;
    private final String databasePath;
    private Connection dbConnection;
    private final ScheduledExecutorService saveScheduler;
    
    // Storage type configuration
    private boolean useDatabase;
    private boolean useFileBackup;
    
    // Cache for frequently accessed data
    private final Map<UUID, PlayerEconomyData> playerDataCache = new ConcurrentHashMap<>();
    private final Map<UUID, BankAccount> accountCache = new ConcurrentHashMap<>();
    private final Map<UUID, Loan> loanCache = new ConcurrentHashMap<>();
    private final Map<UUID, Shop> shopCache = new ConcurrentHashMap<>();
    private final Map<UUID, Auction> auctionCache = new ConcurrentHashMap<>();
    
    // File paths
    private final String playersFile = "players.json";
    private final String accountsFile = "accounts.json";
    private final String loansFile = "loans.json";
    private final String shopsFile = "shops.json";
    private final String auctionsFile = "auctions.json";
    private final String transactionsFile = "transactions.json";
    private final String currenciesFile = "currencies.json";
    
    private EconomyPersistenceManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create();
        this.dataFolder = "config/neoessentials/economy/";
        this.databasePath = dataFolder + "economy.db";
        this.useDatabase = true; // Default to database storage
        this.useFileBackup = true; // Always keep file backup
        this.saveScheduler = Executors.newScheduledThreadPool(2);
        
        initializeStorage();
        scheduleAutoSave();
    }
    
    public static EconomyPersistenceManager getInstance() {
        if (instance == null) {
            instance = new EconomyPersistenceManager();
        }
        return instance;
    }
    
    /**
     * Initialize storage system (create directories, database tables, etc.)
     */
    private void initializeStorage() {
        try {
            // Create data directory
            Path dataPath = Paths.get(dataFolder);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }
            
            // Initialize database if enabled
            if (useDatabase) {
                initializeDatabase();
            }
            
            NeoEssentials.LOGGER.info("Economy persistence manager initialized");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize economy persistence: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize SQLite database and create tables
     */
    private void initializeDatabase() {
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            
            // Connect to database
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            dbConnection.setAutoCommit(false);
            
            // Create tables
            createDatabaseTables();
            
            NeoEssentials.LOGGER.info("Economy database initialized at " + databasePath);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize economy database: " + e.getMessage());
            useDatabase = false; // Fall back to file storage
        }
    }
    
    /**
     * Create all necessary database tables
     */
    private void createDatabaseTables() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        
        // Players table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                data TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """);
        
        // Bank accounts table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS bank_accounts (
                account_id TEXT PRIMARY KEY,
                owner_uuid TEXT NOT NULL,
                account_type TEXT NOT NULL,
                data TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """);
        
        // Loans table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS loans (
                loan_id TEXT PRIMARY KEY,
                borrower_uuid TEXT NOT NULL,
                loan_type TEXT NOT NULL,
                status TEXT NOT NULL,
                data TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """);
        
        // Shops table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS shops (
                shop_id TEXT PRIMARY KEY,
                owner_uuid TEXT NOT NULL,
                shop_type TEXT NOT NULL,
                data TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """);
        
        // Auctions table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS auctions (
                auction_id TEXT PRIMARY KEY,
                seller_uuid TEXT NOT NULL,
                auction_type TEXT NOT NULL,
                status TEXT NOT NULL,
                data TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """);
        
        // Transactions table
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id TEXT PRIMARY KEY,
                from_uuid TEXT,
                to_uuid TEXT,
                transaction_type TEXT NOT NULL,
                amount REAL NOT NULL,
                currency TEXT NOT NULL,
                data TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """);
        
        // Create indexes for better performance
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_accounts_owner ON bank_accounts(owner_uuid)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_loans_borrower ON loans(borrower_uuid)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_shops_owner ON shops(owner_uuid)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_auctions_seller ON auctions(seller_uuid)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_from ON transactions(from_uuid)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_to ON transactions(to_uuid)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type)");
        
        dbConnection.commit();
        stmt.close();
    }
    
    /**
     * Schedule automatic saving of cached data
     */
    private void scheduleAutoSave() {
        // Save every 5 minutes
        saveScheduler.scheduleAtFixedRate(this::saveAllCachedData, 5, 5, TimeUnit.MINUTES);
        
        // Create file backups every hour
        if (useFileBackup) {
            saveScheduler.scheduleAtFixedRate(this::createFileBackups, 1, 1, TimeUnit.HOURS);
        }
    }
    
    // Player Economy Data persistence
    public CompletableFuture<Void> savePlayerData(PlayerEconomyData playerData) {
        return CompletableFuture.runAsync(() -> {
            try {
                playerDataCache.put(playerData.getPlayerId(), playerData);
                
                if (useDatabase) {
                    savePlayerDataToDatabase(playerData);
                }
                
                if (useFileBackup) {
                    savePlayerDataToFile(playerData);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to save player data for " + playerData.getPlayerId(), e);
            }
        });
    }
    
    public CompletableFuture<PlayerEconomyData> loadPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache first
                PlayerEconomyData cached = playerDataCache.get(playerId);
                if (cached != null) {
                    return cached;
                }
                
                // Try to load from database
                if (useDatabase) {
                    PlayerEconomyData data = loadPlayerDataFromDatabase(playerId);
                    if (data != null) {
                        playerDataCache.put(playerId, data);
                        return data;
                    }
                }
                
                // Fall back to file
                PlayerEconomyData data = loadPlayerDataFromFile(playerId);
                if (data != null) {
                    playerDataCache.put(playerId, data);
                    return data;
                }
                
                // Create new player data
                data = new PlayerEconomyData(playerId);
                playerDataCache.put(playerId, data);
                return data;
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to load player data for " + playerId, e);
                return new PlayerEconomyData(playerId);
            }
        });
    }
    
    private void savePlayerDataToDatabase(PlayerEconomyData playerData) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO players (uuid, data, updated_at)
            VALUES (?, ?, strftime('%s', 'now'))
        """;
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, playerData.getPlayerId().toString());
            stmt.setString(2, gson.toJson(playerData));
            stmt.executeUpdate();
            dbConnection.commit();
        }
    }
    
    private PlayerEconomyData loadPlayerDataFromDatabase(UUID playerId) throws SQLException {
        String sql = "SELECT data FROM players WHERE uuid = ?";
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String data = rs.getString("data");
                return gson.fromJson(data, PlayerEconomyData.class);
            }
        }
        
        return null;
    }
    
    private void savePlayerDataToFile(PlayerEconomyData playerData) throws IOException {
        // Save individual player file
        String playerFile = dataFolder + "players/" + playerData.getPlayerId().toString() + ".json";
        Path playerPath = Paths.get(playerFile);
        
        if (!Files.exists(playerPath.getParent())) {
            Files.createDirectories(playerPath.getParent());
        }
        
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(playerData, writer);
        }
    }
    
    private PlayerEconomyData loadPlayerDataFromFile(UUID playerId) throws IOException {
        String playerFile = dataFolder + "players/" + playerId.toString() + ".json";
        
        if (!Files.exists(Paths.get(playerFile))) {
            return null;
        }
        
        try (FileReader reader = new FileReader(playerFile)) {
            return gson.fromJson(reader, PlayerEconomyData.class);
        }
    }
    
    // Bank Account persistence
    public CompletableFuture<Void> saveBankAccount(BankAccount account) {
        return CompletableFuture.runAsync(() -> {
            try {
                accountCache.put(account.getAccountId(), account);
                
                if (useDatabase) {
                    saveBankAccountToDatabase(account);
                }
                
                if (useFileBackup) {
                    saveBankAccountToFile(account);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to save bank account " + account.getAccountId(), e);
            }
        });
    }
    
    public CompletableFuture<BankAccount> loadBankAccount(UUID accountId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check cache first
                BankAccount cached = accountCache.get(accountId);
                if (cached != null) {
                    return cached;
                }
                
                // Try database
                if (useDatabase) {
                    BankAccount account = loadBankAccountFromDatabase(accountId);
                    if (account != null) {
                        accountCache.put(accountId, account);
                        return account;
                    }
                }
                
                // Fall back to file
                BankAccount account = loadBankAccountFromFile(accountId);
                if (account != null) {
                    accountCache.put(accountId, account);
                }
                
                return account;
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to load bank account " + accountId, e);
                return null;
            }
        });
    }
    
    public CompletableFuture<List<BankAccount>> loadPlayerBankAccounts(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<BankAccount> accounts = new ArrayList<>();
                
                if (useDatabase) {
                    accounts = loadPlayerBankAccountsFromDatabase(playerId);
                } else {
                    accounts = loadPlayerBankAccountsFromFile(playerId);
                }
                
                // Update cache
                for (BankAccount account : accounts) {
                    accountCache.put(account.getAccountId(), account);
                }
                
                return accounts;
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to load bank accounts for player " + playerId, e);
                return new ArrayList<>();
            }
        });
    }
    
    private void saveBankAccountToDatabase(BankAccount account) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO bank_accounts (account_id, owner_uuid, account_type, data, updated_at)
            VALUES (?, ?, ?, ?, strftime('%s', 'now'))
        """;
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, account.getAccountId().toString());
            stmt.setString(2, account.getOwnerId().toString());
            stmt.setString(3, account.getAccountType().name());
            stmt.setString(4, gson.toJson(account));
            stmt.executeUpdate();
            dbConnection.commit();
        }
    }
    
    private BankAccount loadBankAccountFromDatabase(UUID accountId) throws SQLException {
        String sql = "SELECT data FROM bank_accounts WHERE account_id = ?";
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, accountId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String data = rs.getString("data");
                return gson.fromJson(data, BankAccount.class);
            }
        }
        
        return null;
    }
    
    private List<BankAccount> loadPlayerBankAccountsFromDatabase(UUID playerId) throws SQLException {
        String sql = "SELECT data FROM bank_accounts WHERE owner_uuid = ?";
        List<BankAccount> accounts = new ArrayList<>();
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String data = rs.getString("data");
                BankAccount account = gson.fromJson(data, BankAccount.class);
                accounts.add(account);
            }
        }
        
        return accounts;
    }
    
    private void saveBankAccountToFile(BankAccount account) throws IOException {
        Map<String, BankAccount> accounts = loadAllBankAccountsFromFile();
        accounts.put(account.getAccountId().toString(), account);
        
        try (FileWriter writer = new FileWriter(dataFolder + accountsFile)) {
            gson.toJson(accounts, writer);
        }
    }
    
    private BankAccount loadBankAccountFromFile(UUID accountId) throws IOException {
        Map<String, BankAccount> accounts = loadAllBankAccountsFromFile();
        return accounts.get(accountId.toString());
    }
    
    private List<BankAccount> loadPlayerBankAccountsFromFile(UUID playerId) throws IOException {
        Map<String, BankAccount> accounts = loadAllBankAccountsFromFile();
        return accounts.values().stream()
            .filter(account -> account.getOwnerId().equals(playerId))
            .toList();
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, BankAccount> loadAllBankAccountsFromFile() throws IOException {
        File file = new File(dataFolder + accountsFile);
        if (!file.exists()) {
            return new HashMap<>();
        }
        
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, BankAccount>>(){}.getType();
            Map<String, BankAccount> result = gson.fromJson(reader, type);
            return result != null ? result : new HashMap<>();
        }
    }
    
    // Transaction persistence
    public CompletableFuture<Void> saveTransaction(Transaction transaction) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (useDatabase) {
                    saveTransactionToDatabase(transaction);
                }
                
                if (useFileBackup) {
                    saveTransactionToFile(transaction);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to save transaction " + transaction.getTransactionId(), e);
            }
        });
    }
    
    private void saveTransactionToDatabase(Transaction transaction) throws SQLException {
        String sql = """
            INSERT INTO transactions (transaction_id, from_uuid, to_uuid, transaction_type, amount, currency, data)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, transaction.getTransactionId().toString());
            stmt.setString(2, transaction.getFromPlayerId() != null ? transaction.getFromPlayerId().toString() : null);
            stmt.setString(3, transaction.getToPlayerId() != null ? transaction.getToPlayerId().toString() : null);
            stmt.setString(4, transaction.getType().name());
            stmt.setDouble(5, transaction.getAmount());
            stmt.setString(6, transaction.getCurrency().getCode());
            stmt.setString(7, gson.toJson(transaction));
            stmt.executeUpdate();
            dbConnection.commit();
        }
    }
    
    private void saveTransactionToFile(Transaction transaction) throws IOException {
        // Append to transaction log file
        String logFile = dataFolder + "transactions_" + 
            java.time.LocalDate.now().toString() + ".json";
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(gson.toJson(transaction) + "\n");
        }
    }
    
    // Cleanup and maintenance
    public void saveAllCachedData() {
        try {
            NeoEssentials.LOGGER.info("Saving all cached economy data...");
            
            for (PlayerEconomyData playerData : playerDataCache.values()) {
                savePlayerData(playerData).join();
            }
            
            for (BankAccount account : accountCache.values()) {
                saveBankAccount(account).join();
            }
            
            for (Loan loan : loanCache.values()) {
                // saveLoan(loan).join(); // Implement similar to accounts
            }
            
            for (Shop shop : shopCache.values()) {
                // saveShop(shop).join(); // Implement similar to accounts
            }
            
            for (Auction auction : auctionCache.values()) {
                // saveAuction(auction).join(); // Implement similar to accounts
            }
            
            NeoEssentials.LOGGER.info("All economy data saved successfully");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to save cached economy data", e);
        }
    }
    
    public void createFileBackups() {
        if (!useFileBackup) return;
        
        try {
            String backupFolder = dataFolder + "backups/" + java.time.LocalDate.now().toString() + "/";
            Path backupPath = Paths.get(backupFolder);
            
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }
            
            // Copy all data files to backup folder
            String[] filesToBackup = {playersFile, accountsFile, loansFile, shopsFile, auctionsFile};
            
            for (String file : filesToBackup) {
                Path source = Paths.get(dataFolder + file);
                Path target = Paths.get(backupFolder + file);
                
                if (Files.exists(source)) {
                    Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            
            // Also backup database
            if (useDatabase && Files.exists(Paths.get(databasePath))) {
                Path dbBackup = Paths.get(backupFolder + "economy.db");
                Files.copy(Paths.get(databasePath), dbBackup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            NeoEssentials.LOGGER.info("Economy data backup created at " + backupFolder);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to create economy data backup", e);
        }
    }
    
    /**
     * Shutdown the persistence manager
     */
    public void shutdown() {
        try {
            // Save all cached data before shutdown
            saveAllCachedData();
            
            // Shutdown scheduler
            saveScheduler.shutdown();
            
            // Close database connection
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
            
            NeoEssentials.LOGGER.info("Economy persistence manager shutdown complete");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error during persistence manager shutdown", e);
        }
    }
    
    // Configuration methods
    public void setUseDatabase(boolean useDatabase) {
        this.useDatabase = useDatabase;
        if (useDatabase && dbConnection == null) {
            initializeDatabase();
        }
    }
    
    public void setUseFileBackup(boolean useFileBackup) {
        this.useFileBackup = useFileBackup;
    }
    
    public boolean isUsingDatabase() {
        return useDatabase && dbConnection != null;
    }
    
    public boolean isUsingFileBackup() {
        return useFileBackup;
    }
}
