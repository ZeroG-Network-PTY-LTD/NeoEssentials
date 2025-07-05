package com.zerog.neoessentials.economy.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.Transaction;

import java.io.IOException;
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
        
        String sql = """
            INSERT OR REPLACE INTO accounts (player_id, player_name, last_seen, created_at)
            VALUES (?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.getPlayerId().toString());
            stmt.setString(2, account.getPlayerName());
            stmt.setLong(3, java.time.ZoneOffset.UTC.getRules().getOffset(account.getLastActivity()).getTotalSeconds());
            stmt.setLong(4, java.time.ZoneOffset.UTC.getRules().getOffset(account.getCreatedAt()).getTotalSeconds());
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to save account: " + account.getPlayerId(), e);
            return false;
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
        
        String sql = "DELETE FROM accounts WHERE player_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to delete account: " + playerId, e);
            return false;
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
            INSERT INTO transactions (id, from_account, to_account, amount, currency_id, type, description, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transaction.getId().toString());
            stmt.setString(2, transaction.getFromAccount().toString());
            stmt.setString(3, transaction.getToAccount().toString());
            stmt.setBigDecimal(4, transaction.getAmount());
            stmt.setString(5, transaction.getCurrency().getId());
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
    
    private void createTables() throws SQLException {
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
                currency_id TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT,
                timestamp INTEGER NOT NULL
            )
            """;
        
        // Create account_balances table for multiple currencies
        String balancesTable = """
            CREATE TABLE IF NOT EXISTS account_balances (
                player_id TEXT NOT NULL,
                currency_id TEXT NOT NULL,
                balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                PRIMARY KEY (player_id, currency_id),
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
        long lastSeenTimestamp = rs.getLong("last_seen");
        long createdAtTimestamp = rs.getLong("created_at");
        
        EconomyAccount account = new EconomyAccount(playerId, playerName);
        
        // In a full implementation, you would load the balances for all currencies
        // For now, we'll just create an empty account
        
        return account;
    }
    
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID fromAccount = UUID.fromString(rs.getString("from_account"));
        UUID toAccount = UUID.fromString(rs.getString("to_account"));
        BigDecimal amount = rs.getBigDecimal("amount");
        String currencyId = rs.getString("currency_id");
        Transaction.Type type = Transaction.Type.valueOf(rs.getString("type"));
        String description = rs.getString("description");
        long timestampSeconds = rs.getLong("timestamp");
        
        // Create a basic currency - in practice this should be loaded from config
        Currency currency = Currency.createBasic(currencyId, currencyId, "$", currencyId + "s");
        
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
