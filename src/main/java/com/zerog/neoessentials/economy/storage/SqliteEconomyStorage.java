package com.zerog.neoessentials.economy.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.Transaction;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * SQLite database storage implementation for economy data
 */
public class SqliteEconomyStorage implements EconomyStorage {
    private final Path databaseFile;
    private final Path backupDirectory;
    private Connection connection;
    private boolean initialized = false;
    
    public SqliteEconomyStorage(Path dataDirectory) {
        this.databaseFile = dataDirectory.resolve("economy.db");
        this.backupDirectory = dataDirectory.resolve("backups");
    }
    
    @Override
    public boolean initialize() {
        try {
            // Create directories if they don't exist
            Files.createDirectories(databaseFile.getParent());
            Files.createDirectories(backupDirectory);
            
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Connect to database
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
            connection.setAutoCommit(true);
            
            // Create tables
            createTables();
            
            initialized = true;
            NeoEssentials.LOGGER.info("SQLite Economy Storage initialized successfully");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize SQLite Economy Storage", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                initialized = false;
                NeoEssentials.LOGGER.info("SQLite Economy Storage closed");
            } catch (SQLException e) {
                NeoEssentials.LOGGER.error("Error closing SQLite connection", e);
            }
        }
    }
    
    @Override
    public boolean saveAccount(EconomyAccount account) {
        if (!initialized) return false;
        
        try {
            connection.setAutoCommit(false); // Begin transaction
            
            // Save account metadata
            String accountSql = """
                INSERT OR REPLACE INTO accounts (player_id, player_name, last_seen, created_at)
                VALUES (?, ?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(accountSql)) {
                stmt.setString(1, account.getPlayerId().toString());
                stmt.setString(2, account.getPlayerName());
                stmt.setLong(3, java.time.Instant.from(account.getLastActivity().atZone(java.time.ZoneOffset.UTC)).getEpochSecond());
                stmt.setLong(4, java.time.Instant.from(account.getCreatedAt().atZone(java.time.ZoneOffset.UTC)).getEpochSecond());
                stmt.executeUpdate();
            }
            
            // Clear existing balances for this account
            String clearBalancesSql = "DELETE FROM account_balances WHERE player_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(clearBalancesSql)) {
                stmt.setString(1, account.getPlayerId().toString());
                stmt.executeUpdate();
            }
            
            // Save all balances for this account
            String balanceSql = """
                INSERT INTO account_balances (player_id, currency_name, balance, updated_at)
                VALUES (?, ?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(balanceSql)) {
                Map<Currency, BigDecimal> balances = account.getAllBalances();
                for (Map.Entry<Currency, BigDecimal> entry : balances.entrySet()) {
                    stmt.setString(1, account.getPlayerId().toString());
                    stmt.setString(2, entry.getKey().getName());
                    stmt.setBigDecimal(3, entry.getValue());
                    stmt.setLong(4, System.currentTimeMillis() / 1000);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            connection.commit(); // Commit transaction
            return true;
            
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback on error
            } catch (SQLException rollbackEx) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction", rollbackEx);
            }
            NeoEssentials.LOGGER.error("Failed to save account: " + account.getPlayerId(), e);
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Restore auto-commit
            } catch (SQLException e) {
                NeoEssentials.LOGGER.error("Failed to restore auto-commit", e);
            }
        }
    }
    
    @Override
    public Optional<EconomyAccount> loadAccount(UUID playerId) {
        if (!initialized) return Optional.empty();
        
        String sql = "SELECT * FROM accounts WHERE player_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load account: " + playerId, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public boolean deleteAccount(UUID playerId) {
        if (!initialized) return false;
        
        try {
            connection.setAutoCommit(false); // Begin transaction
            
            // Delete balances first
            String deleteBalancesSql = "DELETE FROM account_balances WHERE player_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteBalancesSql)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            }
            
            // Delete account
            String deleteAccountSql = "DELETE FROM accounts WHERE player_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteAccountSql)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            }
            
            connection.commit(); // Commit transaction
            return true;
            
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback on error
            } catch (SQLException rollbackEx) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction", rollbackEx);
            }
            NeoEssentials.LOGGER.error("Failed to delete account: " + playerId, e);
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Restore auto-commit
            } catch (SQLException e) {
                NeoEssentials.LOGGER.error("Failed to restore auto-commit", e);
            }
        }
    }
    
    @Override
    public boolean accountExists(UUID playerId) {
        if (!initialized) return false;
        
        String sql = "SELECT 1 FROM accounts WHERE player_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to check account existence: " + playerId, e);
            return false;
        }
    }
    
    @Override
    public List<EconomyAccount> getAllAccounts() {
        if (!initialized) return new ArrayList<>();
        
        String sql = "SELECT * FROM accounts ORDER BY balance DESC";
        List<EconomyAccount> accounts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load all accounts", e);
        }
        
        return accounts;
    }
    
    @Override
    public boolean logTransaction(Transaction transaction) {
        if (!initialized) return false;
        
        String sql = """
            INSERT INTO transactions (id, from_account, to_account, amount, currency_name, type, description, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transaction.getId().toString());
            stmt.setString(2, transaction.getFromAccount().toString());
            stmt.setString(3, transaction.getToAccount().toString());
            stmt.setBigDecimal(4, transaction.getAmount());
            stmt.setString(5, transaction.getCurrency().getName());
            stmt.setString(6, transaction.getType().name());
            stmt.setString(7, transaction.getDescription());
            stmt.setLong(8, java.time.ZoneOffset.UTC.getRules().getOffset(transaction.getTimestamp()).getTotalSeconds());
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to log transaction: " + transaction.getId(), e);
            return false;
        }
    }
    
    @Override
    public List<Transaction> getTransactionHistory(UUID playerId, int limit) {
        if (!initialized) return new ArrayList<>();
        
        String sql = """
            SELECT * FROM transactions 
            WHERE from_account = ? OR to_account = ? 
            ORDER BY timestamp DESC 
            LIMIT ?
            """;
        
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, playerId.toString());
            stmt.setInt(3, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to get transaction history for: " + playerId, e);
        }
        
        return transactions;
    }
    
    @Override
    public List<Transaction> getTransactionHistory(UUID playerId, long fromTimestamp, long toTimestamp, int limit) {
        if (!initialized) return new ArrayList<>();
        
        String sql = """
            SELECT * FROM transactions 
            WHERE (from_account = ? OR to_account = ?) 
            AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC 
            LIMIT ?
            """;
        
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, playerId.toString());
            stmt.setLong(3, fromTimestamp);
            stmt.setLong(4, toTimestamp);
            stmt.setInt(5, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to get transaction history for: " + playerId, e);
        }
        
        return transactions;
    }
    
    @Override
    public Optional<Transaction> getTransaction(UUID transactionId) {
        if (!initialized) return Optional.empty();
        
        String sql = "SELECT * FROM transactions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transactionId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to get transaction: " + transactionId, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Transaction> getAllTransactions(int limit) {
        if (!initialized) return new ArrayList<>();
        
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC LIMIT ?";
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to get all transactions", e);
        }
        
        return transactions;
    }
    
    @Override
    public boolean backup() {
        if (!initialized) return false;
        
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            Path backupFile = backupDirectory.resolve("economy_backup_" + timestamp + ".db");
            
            Files.copy(databaseFile, backupFile);
            
            NeoEssentials.LOGGER.info("Economy backup created: {}", backupFile);
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to create economy backup", e);
            return false;
        }
    }
    
    @Override
    public boolean restore(String backupName) {
        if (!initialized) return false;
        
        try {
            Path backupFile = backupDirectory.resolve(backupName);
            if (!Files.exists(backupFile)) {
                NeoEssentials.LOGGER.error("Backup not found: {}", backupName);
                return false;
            }
            
            // Close current connection
            connection.close();
            
            // Replace database file
            Files.copy(backupFile, databaseFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Reconnect
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
            connection.setAutoCommit(true);
            
            NeoEssentials.LOGGER.info("Economy data restored from backup: {}", backupName);
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to restore from backup: " + backupName, e);
            return false;
        }
    }
    
    @Override
    public List<String> getAvailableBackups() {
        try {
            if (!Files.exists(backupDirectory)) {
                return new ArrayList<>();
            }
            
            return Files.list(backupDirectory)
                    .filter(path -> path.getFileName().toString().endsWith(".db"))
                    .map(path -> path.getFileName().toString())
                    .sorted(Collections.reverseOrder())
                    .toList();
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to list backups", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean performMaintenance() {
        if (!initialized) return false;
        
        try {
            // Clean up old transactions (keep only last 30 days)
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            String sql = "DELETE FROM transactions WHERE timestamp < ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, thirtyDaysAgo);
                int deleted = stmt.executeUpdate();
                NeoEssentials.LOGGER.info("Deleted {} old transactions during maintenance", deleted);
            }
            
            // Vacuum database to reclaim space
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("VACUUM");
            }
            
            // Remove old backups (keep only last 10)
            List<String> backups = getAvailableBackups();
            if (backups.size() > 10) {
                for (int i = 10; i < backups.size(); i++) {
                    Path oldBackup = backupDirectory.resolve(backups.get(i));
                    Files.deleteIfExists(oldBackup);
                }
            }
            
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to perform maintenance", e);
            return false;
        }
    }
    
    @Override
    public StorageStatistics getStatistics() {
        if (!initialized) {
            return new StorageStatistics(0, 0, 0, 0, "SQLite", false);
        }
        
        try {
            long totalAccounts = 0;
            long totalTransactions = 0;
            
            // Count accounts
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM accounts");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalAccounts = rs.getLong(1);
                }
            }
            
            // Count transactions
            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM transactions");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalTransactions = rs.getLong(1);
                }
            }
            
            long storageSize = Files.size(databaseFile);
            
            return new StorageStatistics(totalAccounts, totalTransactions, storageSize, 
                    0, "SQLite", true);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to get storage statistics", e);
            return new StorageStatistics(0, 0, 0, 0, "SQLite", false);
        }
    }
    
    @Override
    public StorageValidationReport validateData() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (!initialized) {
            errors.add("Storage not initialized");
            return new StorageValidationReport(false, errors, warnings);
        }
        
        try {
            // Check for accounts with negative balances
            String sql = "SELECT player_id, balance FROM accounts WHERE balance < 0";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    warnings.add("Account with negative balance: " + rs.getString("player_id"));
                }
            }
            
            // Check for invalid transactions
            sql = "SELECT id, amount FROM transactions WHERE amount <= 0";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    errors.add("Transaction with invalid amount: " + rs.getString("id"));
                }
            }
            
            boolean valid = errors.isEmpty();
            return new StorageValidationReport(valid, errors, warnings);
        } catch (SQLException e) {
            errors.add("Database validation failed: " + e.getMessage());
            return new StorageValidationReport(false, errors, warnings);
        }
    }
    
    /**
     * Migrates the database schema if needed
     */
    private void migrateDatabaseIfNeeded() throws SQLException {
        // Check if account_balances table exists
        String checkTableSql = "SELECT name FROM sqlite_master WHERE type='table' AND name='account_balances'";
        try (PreparedStatement stmt = connection.prepareStatement(checkTableSql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Table exists, check if it has currency_name column
                    String checkColumnSql = "PRAGMA table_info(account_balances)";
                    try (PreparedStatement columnStmt = connection.prepareStatement(checkColumnSql)) {
                        try (ResultSet columnRs = columnStmt.executeQuery()) {
                            boolean hasCurrencyName = false;
                            boolean hasCurrencyId = false;
                            
                            while (columnRs.next()) {
                                String columnName = columnRs.getString("name");
                                if ("currency_name".equals(columnName)) {
                                    hasCurrencyName = true;
                                } else if ("currency_id".equals(columnName)) {
                                    hasCurrencyId = true;
                                }
                            }
                            
                            // If table has currency_id but not currency_name, migrate it
                            if (hasCurrencyId && !hasCurrencyName) {
                                NeoEssentials.LOGGER.info("Migrating account_balances table from currency_id to currency_name");
                                migrateCurrencyColumn();
                            }
                        }
                    }
                }
            }
        }
        
        // Check if transactions table exists and needs migration
        String checkTransactionsTableSql = "SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'";
        try (PreparedStatement stmt = connection.prepareStatement(checkTransactionsTableSql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Table exists, check if it has currency_name column
                    String checkColumnSql = "PRAGMA table_info(transactions)";
                    try (PreparedStatement columnStmt = connection.prepareStatement(checkColumnSql)) {
                        try (ResultSet columnRs = columnStmt.executeQuery()) {
                            boolean hasCurrencyName = false;
                            boolean hasCurrencyId = false;
                            
                            while (columnRs.next()) {
                                String columnName = columnRs.getString("name");
                                if ("currency_name".equals(columnName)) {
                                    hasCurrencyName = true;
                                } else if ("currency_id".equals(columnName)) {
                                    hasCurrencyId = true;
                                }
                            }
                            
                            // If table has currency_id but not currency_name, migrate it
                            if (hasCurrencyId && !hasCurrencyName) {
                                NeoEssentials.LOGGER.info("Migrating transactions table from currency_id to currency_name");
                                migrateTransactionsTable();
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Migrates the currency_id column to currency_name
     */
    private void migrateCurrencyColumn() throws SQLException {
        try {
            connection.setAutoCommit(false);
            
            // Create new table with correct schema
            String createNewTable = """
                CREATE TABLE IF NOT EXISTS account_balances_new (
                    player_id TEXT NOT NULL,
                    currency_name TEXT NOT NULL,
                    balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                    updated_at INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_id, currency_name),
                    FOREIGN KEY (player_id) REFERENCES accounts(player_id) ON DELETE CASCADE
                )
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(createNewTable)) {
                stmt.executeUpdate();
            }
            
            // Copy data from old table to new table (convert currency_id to currency_name)
            String copyDataSql = """
                INSERT INTO account_balances_new (player_id, currency_name, balance, updated_at)
                SELECT player_id, currency_id, balance, 0 FROM account_balances
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(copyDataSql)) {
                stmt.executeUpdate();
            }
            
            // Drop old table
            String dropOldTable = "DROP TABLE account_balances";
            try (PreparedStatement stmt = connection.prepareStatement(dropOldTable)) {
                stmt.executeUpdate();
            }
            
            // Rename new table to old table name
            String renameTable = "ALTER TABLE account_balances_new RENAME TO account_balances";
            try (PreparedStatement stmt = connection.prepareStatement(renameTable)) {
                stmt.executeUpdate();
            }
            
            connection.commit();
            NeoEssentials.LOGGER.info("Successfully migrated account_balances table");
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    /**
     * Migrates the transactions table from currency_id to currency_name
     */
    private void migrateTransactionsTable() throws SQLException {
        try {
            connection.setAutoCommit(false);
            
            // Create new table with correct schema
            String createNewTable = """
                CREATE TABLE IF NOT EXISTS transactions_new (
                    id TEXT PRIMARY KEY,
                    from_account TEXT NOT NULL,
                    to_account TEXT NOT NULL,
                    amount DECIMAL(20,2) NOT NULL,
                    currency_name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT,
                    timestamp INTEGER NOT NULL
                )
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(createNewTable)) {
                stmt.executeUpdate();
            }
            
            // Copy data from old table to new table (convert currency_id to currency_name)
            String copyDataSql = """
                INSERT INTO transactions_new (id, from_account, to_account, amount, currency_name, type, description, timestamp)
                SELECT id, from_account, to_account, amount, currency_id, type, description, timestamp FROM transactions
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(copyDataSql)) {
                stmt.executeUpdate();
            }
            
            // Drop old table
            String dropOldTable = "DROP TABLE transactions";
            try (PreparedStatement stmt = connection.prepareStatement(dropOldTable)) {
                stmt.executeUpdate();
            }
            
            // Rename new table to old table name
            String renameTable = "ALTER TABLE transactions_new RENAME TO transactions";
            try (PreparedStatement stmt = connection.prepareStatement(renameTable)) {
                stmt.executeUpdate();
            }
            
            connection.commit();
            NeoEssentials.LOGGER.info("Successfully migrated transactions table");
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    private void createTables() throws SQLException {
        // Check if we need to migrate the database
        migrateDatabaseIfNeeded();
        
        // Create accounts table - simplified for the current data structure
        String accountsTable = """
            CREATE TABLE IF NOT EXISTS accounts (
                player_id TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                last_seen INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """;
        
        // Create transactions table
        String transactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                from_account TEXT NOT NULL,
                to_account TEXT NOT NULL,
                amount DECIMAL(20,2) NOT NULL,
                currency_name TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT,
                timestamp INTEGER NOT NULL
            )
            """;
        
        // Create account_balances table for multiple currencies
        String balancesTable = """
            CREATE TABLE IF NOT EXISTS account_balances (
                player_id TEXT NOT NULL,
                currency_name TEXT NOT NULL,
                balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                updated_at INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (player_id, currency_name),
                FOREIGN KEY (player_id) REFERENCES accounts(player_id) ON DELETE CASCADE
            )
            """;
        
        // Create indexes
        String indexAccounts = "CREATE INDEX IF NOT EXISTS idx_accounts_last_seen ON accounts(last_seen)";
        String indexBalances = "CREATE INDEX IF NOT EXISTS idx_balances_balance ON account_balances(balance DESC)";
        String indexTransactions = "CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp DESC)";
        String indexTransactionsFrom = "CREATE INDEX IF NOT EXISTS idx_transactions_from ON transactions(from_account)";
        String indexTransactionsTo = "CREATE INDEX IF NOT EXISTS idx_transactions_to ON transactions(to_account)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(accountsTable);
            stmt.execute(transactionsTable);
            stmt.execute(balancesTable);
            stmt.execute(indexAccounts);
            stmt.execute(indexBalances);
            stmt.execute(indexTransactions);
            stmt.execute(indexTransactionsFrom);
            stmt.execute(indexTransactionsTo);
        }
    }
    
    private EconomyAccount mapResultSetToAccount(ResultSet rs) throws SQLException {
        UUID playerId = UUID.fromString(rs.getString("player_id"));
        String playerName = rs.getString("player_name");
        
        EconomyAccount account = new EconomyAccount(playerId, playerName);
        
        // Load all balances for this account
        loadAccountBalances(account);
        
        return account;
    }
    
    /**
     * Loads all currency balances for an account from the database
     */
    private void loadAccountBalances(EconomyAccount account) {
        String sql = "SELECT currency_name, balance FROM account_balances WHERE player_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.getPlayerId().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String currencyName = rs.getString("currency_name");
                    BigDecimal balance = rs.getBigDecimal("balance");
                    
                    // Create a simple currency object for loading
                    // In a full implementation, you'd have a currency registry
                    Currency currency = createCurrencyFromName(currencyName);
                    if (currency != null) {
                        account.setBalance(currency, balance);
                    }
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load balances for account: " + account.getPlayerId(), e);
        }
    }
    
    /**
     * Creates a Currency object from a currency name
     * This is a simple implementation - in practice you'd use a currency registry
     */
    private Currency createCurrencyFromName(String currencyName) {
        try {
            // For now, create a basic currency with default values
            // This should be replaced with proper currency registry lookup
            return Currency.createBasic(currencyName, currencyName, "¤", currencyName + "s");
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to create currency from name: " + currencyName, e);
            return null;
        }
    }
    
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID fromAccount = UUID.fromString(rs.getString("from_account"));
        UUID toAccount = UUID.fromString(rs.getString("to_account"));
        BigDecimal amount = rs.getBigDecimal("amount");
        String currencyName = rs.getString("currency_name");
        Transaction.Type type = Transaction.Type.valueOf(rs.getString("type"));
        String description = rs.getString("description");
        
        // Create a basic currency - in practice this should be loaded from config
        Currency currency = Currency.createBasic(currencyName, currencyName, "$", currencyName + "s");
        
        return Transaction.builder()
                .id(id)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .currency(currency)
                .type(type)
                .description(description)
                .build();
    }
}
