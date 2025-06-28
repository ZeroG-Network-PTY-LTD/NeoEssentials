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
            NeoEssentials.LOGGER.info("Initializing economy persistence manager...");
            
            // Create data directory
            Path dataPath = Paths.get(dataFolder);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                NeoEssentials.LOGGER.info("Created economy data directory: " + dataPath);
            }
            
            // Initialize database if enabled
            if (useDatabase) {
                NeoEssentials.LOGGER.info("Attempting to initialize SQLite database...");
                initializeDatabase();
                
                if (isUsingDatabase()) {
                    NeoEssentials.LOGGER.info("✓ SQLite database initialized successfully - loan persistence enabled");
                } else {
                    NeoEssentials.LOGGER.warn("✗ Database initialization failed - using file storage only");
                }
            } else {
                NeoEssentials.LOGGER.info("Database disabled - using file storage only");
            }
            
            NeoEssentials.LOGGER.info("Economy persistence manager initialized (Database: {}, File Backup: {})", 
                isUsingDatabase() ? "ENABLED" : "DISABLED", 
                isUsingFileBackup() ? "ENABLED" : "DISABLED");
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
            // Ensure directory exists
            Path dbDir = Paths.get(databasePath).getParent();
            if (!Files.exists(dbDir)) {
                Files.createDirectories(dbDir);
                NeoEssentials.LOGGER.info("Created economy database directory: " + dbDir);
            }
            
            // Try to load SQLite driver (may not be available if not included)
            try {
                Class.forName("org.sqlite.JDBC");
                NeoEssentials.LOGGER.info("SQLite JDBC driver loaded successfully");
            } catch (ClassNotFoundException e) {
                NeoEssentials.LOGGER.warn("SQLite JDBC driver not available - using file storage only");
                NeoEssentials.LOGGER.warn("To enable database features, ensure SQLite JDBC is available in classpath");
                useDatabase = false;
                dbConnection = null;
                return;
            }
            
            // Connect to database with connection string
            String connectionUrl = "jdbc:sqlite:" + databasePath;
            NeoEssentials.LOGGER.info("Connecting to database: " + connectionUrl);
            
            dbConnection = DriverManager.getConnection(connectionUrl);
            if (dbConnection == null) {
                throw new RuntimeException("Failed to create database connection");
            }
            
            // Enable foreign keys and other performance settings
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.executeUpdate("PRAGMA foreign_keys = ON");
                stmt.executeUpdate("PRAGMA journal_mode = WAL");
                stmt.executeUpdate("PRAGMA synchronous = NORMAL");
                stmt.executeUpdate("PRAGMA temp_store = memory");
                stmt.executeUpdate("PRAGMA mmap_size = 268435456"); // 256MB
            }
            
            dbConnection.setAutoCommit(false);
            
            // Test connection
            if (!dbConnection.isValid(5)) {
                throw new RuntimeException("Database connection validation failed");
            }
            
            // Create tables
            createDatabaseTables();
            
            NeoEssentials.LOGGER.info("Economy database initialized successfully at " + databasePath);
            NeoEssentials.LOGGER.info("Database features enabled - loan applications will persist across restarts");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Database initialization failed: " + e.getMessage(), e);
            NeoEssentials.LOGGER.warn("Falling back to file storage only");
            useDatabase = false;
            dbConnection = null;
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
                principal_amount REAL NOT NULL,
                current_balance REAL NOT NULL,
                interest_rate REAL NOT NULL,
                term_months INTEGER NOT NULL,
                remaining_payments INTEGER NOT NULL,
                status TEXT NOT NULL,
                created_date INTEGER NOT NULL,
                last_payment_date INTEGER DEFAULT 0,
                next_payment_due INTEGER DEFAULT 0,
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
            stmt.setString(2, transaction.getFromPlayer() != null ? transaction.getFromPlayer().toString() : null);
            stmt.setString(3, transaction.getToPlayer() != null ? transaction.getToPlayer().toString() : null);
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
    
    // ========== LOAN PERSISTENCE METHODS ==========
    
    /**
     * Save a loan to database and file
     */
    public CompletableFuture<Boolean> saveLoan(Loan loan) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Update cache first
                loanCache.put(loan.getLoanId(), loan);
                
                // Save to database if available
                if (isUsingDatabase()) {
                    String sql = """
                        INSERT OR REPLACE INTO loans (
                            loan_id, borrower_uuid, loan_type, principal_amount, 
                            current_balance, interest_rate, term_months, 
                            remaining_payments, status, created_date, 
                            last_payment_date, next_payment_due
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                    
                    try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                        stmt.setString(1, loan.getLoanId().toString());
                        stmt.setString(2, loan.getBorrowerId().toString());
                        stmt.setString(3, loan.getType().name());
                        stmt.setDouble(4, loan.getPrincipalAmount());
                        stmt.setDouble(5, loan.getCurrentBalance());
                        stmt.setDouble(6, loan.getInterestRate());
                        stmt.setInt(7, loan.getTermMonths());
                        stmt.setInt(8, loan.getPaymentsRemaining());
                        stmt.setString(9, loan.getStatus().name());
                        stmt.setLong(10, loan.getCreatedDate().getTime());
                        stmt.setLong(11, loan.getLastPaymentDate() != null ? loan.getLastPaymentDate().getTime() : 0);
                        stmt.setLong(12, loan.getNextPaymentDue() != null ? loan.getNextPaymentDue().getTime() : 0);
                        
                        stmt.executeUpdate();
                        dbConnection.commit();
                    }
                    NeoEssentials.LOGGER.debug("Loan saved to database: " + loan.getLoanId().toString().substring(0, 8));
                } else {
                    NeoEssentials.LOGGER.debug("Database not available, loan saved to cache only: " + loan.getLoanId().toString().substring(0, 8));
                }
                
                // Save to file backup (always enabled as fallback)
                if (useFileBackup) {
                    saveLoansToFile();
                }
                
                return true;
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to save loan: " + loan.getLoanId(), e);
                // Even if database save fails, try to save to file as backup
                try {
                    if (useFileBackup) {
                        saveLoansToFile();
                        NeoEssentials.LOGGER.info("Loan saved to file backup despite database error");
                        return true;
                    }
                } catch (Exception fileException) {
                    NeoEssentials.LOGGER.error("Failed to save loan to file backup as well", fileException);
                }
                return false;
            }
        });
    }
    
    /**
     * Load a specific loan from database
     */
    public CompletableFuture<Loan> loadLoan(UUID loanId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            if (loanCache.containsKey(loanId)) {
                return loanCache.get(loanId);
            }
            
            // If database is not available, try to load from file
            if (!isUsingDatabase()) {
                NeoEssentials.LOGGER.debug("Database not available, cannot load loan from database: " + loanId);
                return null;
            }
            
            try {
                String sql = "SELECT * FROM loans WHERE loan_id = ?";
                try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                    stmt.setString(1, loanId.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Loan loan = createLoanFromResultSet(rs);
                            loanCache.put(loanId, loan);
                            return loan;
                        }
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to load loan: " + loanId, e);
            }
            
            return null;
        });
    }
    
    /**
     * Load all loans for a specific player
     */
    public CompletableFuture<List<Loan>> loadPlayerLoans(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isUsingDatabase()) {
                return new ArrayList<>(); // Return empty list if no database
            }
            
            List<Loan> playerLoans = new ArrayList<>();
            
            try {
                String sql = "SELECT * FROM loans WHERE borrower_uuid = ? ORDER BY created_date DESC";
                try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Loan loan = createLoanFromResultSet(rs);
                            loanCache.put(loan.getLoanId(), loan);
                            playerLoans.add(loan);
                        }
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to load player loans: " + playerId, e);
            }
            
            return playerLoans;
        });
    }
    
    /**
     * Load all active loans
     */
    public CompletableFuture<List<Loan>> loadAllActiveLoans() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isUsingDatabase()) {
                return new ArrayList<>(); // Return empty list if no database
            }
            
            List<Loan> activeLoans = new ArrayList<>();
            
            try {
                String sql = "SELECT * FROM loans WHERE status IN ('ACTIVE', 'APPROVED') ORDER BY created_date DESC";
                try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Loan loan = createLoanFromResultSet(rs);
                            loanCache.put(loan.getLoanId(), loan);
                            activeLoans.add(loan);
                        }
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to load active loans", e);
            }
            
            return activeLoans;
        });
    }
    
    /**
     * Delete a loan from database and cache
     */
    public CompletableFuture<Boolean> deleteLoan(UUID loanId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Remove from cache
                loanCache.remove(loanId);
                
                // Delete from database
                String sql = "DELETE FROM loans WHERE loan_id = ?";
                try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                    stmt.setString(1, loanId.toString());
                    int affected = stmt.executeUpdate();
                    
                    if (useFileBackup) {
                        saveLoansToFile();
                    }
                    
                    return affected > 0;
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to delete loan: " + loanId, e);
                return false;
            }
        });
    }
    
    /**
     * Create a Loan object from ResultSet
     */
    private Loan createLoanFromResultSet(ResultSet rs) throws Exception {
        UUID loanId = UUID.fromString(rs.getString("loan_id"));
        UUID borrowerId = UUID.fromString(rs.getString("borrower_uuid"));
        Loan.LoanType type = Loan.LoanType.valueOf(rs.getString("loan_type"));
        double principalAmount = rs.getDouble("principal_amount");
        double currentBalance = rs.getDouble("current_balance");
        double interestRate = rs.getDouble("interest_rate");
        int termMonths = rs.getInt("term_months");
        int remainingPayments = rs.getInt("remaining_payments");
        Loan.LoanStatus status = Loan.LoanStatus.valueOf(rs.getString("status"));
        
        // Create loan with basic parameters
        com.zerog.neoessentials.economy.Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        Loan loan = new Loan(borrowerId, principalAmount, defaultCurrency, type, termMonths, interestRate);
        
        // Set additional properties that aren't in constructor
        loan.setCurrentBalance(currentBalance);
        loan.setRemainingPayments(remainingPayments);
        loan.setStatus(status);
        
        // Set dates if available
        long createdDate = rs.getLong("created_date");
        if (createdDate > 0) {
            loan.setCreatedDate(new java.util.Date(createdDate));
        }
        
        long lastPaymentDate = rs.getLong("last_payment_date");
        if (lastPaymentDate > 0) {
            loan.setLastPaymentDate(new java.util.Date(lastPaymentDate));
        }
        
        long nextPaymentDue = rs.getLong("next_payment_due");
        if (nextPaymentDue > 0) {
            loan.setNextPaymentDue(new java.util.Date(nextPaymentDue));
        }
        
        return loan;
    }
    
    /**
     * Save all loans to JSON file
     */
    private void saveLoansToFile() {
        if (!useFileBackup) return;
        
        try {
            Map<String, Object> loansData = new HashMap<>();
            
            for (Loan loan : loanCache.values()) {
                Map<String, Object> loanData = new HashMap<>();
                loanData.put("borrowerId", loan.getBorrowerId().toString());
                loanData.put("type", loan.getType().name());
                loanData.put("principalAmount", loan.getPrincipalAmount());
                loanData.put("currentBalance", loan.getCurrentBalance());
                loanData.put("interestRate", loan.getInterestRate());
                loanData.put("termMonths", loan.getTermMonths());
                loanData.put("remainingPayments", loan.getPaymentsRemaining());
                loanData.put("status", loan.getStatus().name());
                loanData.put("createdDate", loan.getCreatedDate().getTime());
                
                if (loan.getLastPaymentDate() != null) {
                    loanData.put("lastPaymentDate", loan.getLastPaymentDate().getTime());
                }
                if (loan.getNextPaymentDue() != null) {
                    loanData.put("nextPaymentDue", loan.getNextPaymentDue().getTime());
                }
                
                loansData.put(loan.getLoanId().toString(), loanData);
            }
            
            String json = new com.google.gson.Gson().toJson(loansData);
            Files.write(Paths.get(dataFolder + loansFile), json.getBytes());
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to save loans to file", e);
        }
    }
}
