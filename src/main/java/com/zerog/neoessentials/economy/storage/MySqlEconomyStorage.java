package com.zerog.neoessentials.economy.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DatabaseConfig;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * MySQL database storage implementation for economy data
 */
public class MySqlEconomyStorage implements EconomyStorage {
    private final DatabaseConfig config;
    private Connection connection;
    private boolean initialized = false;
    
    public MySqlEconomyStorage(DatabaseConfig config) {
        this.config = config;
    }
    
    @Override
    public boolean initialize() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Build connection URL
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8",
                    config.getHost(), config.getPort(), config.getDatabase(), config.isUseSsl());
            
            // Connect to database
            connection = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            connection.setAutoCommit(true);
            
            // Create tables
            createTables();
            
            initialized = true;
            NeoEssentials.LOGGER.info("MySQL Economy Storage initialized successfully");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize MySQL Economy Storage", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                initialized = false;
                NeoEssentials.LOGGER.info("MySQL Economy Storage closed");
            } catch (SQLException e) {
                NeoEssentials.LOGGER.error("Error closing MySQL connection", e);
            }
        }
    }
    
    @Override
    public boolean saveAccount(EconomyAccount account) {
        if (!initialized) return false;
        
        // For now, we'll save the account with a default currency
        // In a full implementation, you'd need to choose which currency to save
        // or modify the database schema to support multiple currencies per account
        String sql = String.format("""
            INSERT INTO %saccounts (player_id, player_name, last_seen, created_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            player_name = VALUES(player_name),
            last_seen = VALUES(last_seen)
            """, config.getTablePrefix());
        
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
        
        String sql = String.format("SELECT * FROM %saccounts WHERE player_id = ?", config.getTablePrefix());
        
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
        
        String sql = String.format("DELETE FROM %saccounts WHERE player_id = ?", config.getTablePrefix());
        
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
        
        String sql = String.format("SELECT 1 FROM %saccounts WHERE player_id = ?", config.getTablePrefix());
        
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
        
        String sql = String.format("SELECT * FROM %saccounts ORDER BY balance DESC", config.getTablePrefix());
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
        
        String sql = String.format("""
            INSERT INTO %stransactions (id, from_account, to_account, amount, currency_id, type, description, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, config.getTablePrefix());
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transaction.getId().toString());
            stmt.setString(2, transaction.getFromAccount().toString());
            stmt.setString(3, transaction.getToAccount().toString());
            stmt.setBigDecimal(4, transaction.getAmount());
            stmt.setString(5, transaction.getCurrency().getId());
            stmt.setString(6, transaction.getType().name());
            stmt.setString(7, transaction.getDescription());
            stmt.setLong(8, transaction.getTimestamp());
            
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
        
        String sql = String.format("""
            SELECT * FROM %stransactions 
            WHERE from_account = ? OR to_account = ? 
            ORDER BY timestamp DESC 
            LIMIT ?
            """, config.getTablePrefix());
        
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
        
        String sql = String.format("""
            SELECT * FROM %stransactions 
            WHERE (from_account = ? OR to_account = ?) 
            AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC 
            LIMIT ?
            """, config.getTablePrefix());
        
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
        
        String sql = String.format("SELECT * FROM %stransactions WHERE id = ?", config.getTablePrefix());
        
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
        
        String sql = String.format("SELECT * FROM %stransactions ORDER BY timestamp DESC LIMIT ?", config.getTablePrefix());
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
            String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
            String backupTablePrefix = config.getTablePrefix() + "backup_" + timestamp + "_";
            
            // Create backup tables
            String[] tables = {"accounts", "transactions"};
            
            for (String table : tables) {
                String sql = String.format("CREATE TABLE %s%s AS SELECT * FROM %s%s",
                        backupTablePrefix, table, config.getTablePrefix(), table);
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                }
            }
            
            NeoEssentials.LOGGER.info("Economy backup created with prefix: {}", backupTablePrefix);
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
            String backupTablePrefix = config.getTablePrefix() + "backup_" + backupName + "_";
            
            // Check if backup exists
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Restore accounts table
            String checkTable = backupTablePrefix + "accounts";
            try (ResultSet rs = metaData.getTables(null, null, checkTable, null)) {
                if (rs.next()) {
                    String sql = String.format("DROP TABLE IF EXISTS %saccounts", config.getTablePrefix());
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sql);
                    }
                    
                    sql = String.format("CREATE TABLE %saccounts AS SELECT * FROM %s",
                            config.getTablePrefix(), checkTable);
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sql);
                    }
                }
            }
            
            // Restore transactions table
            checkTable = backupTablePrefix + "transactions";
            try (ResultSet rs = metaData.getTables(null, null, checkTable, null)) {
                if (rs.next()) {
                    String sql = String.format("DROP TABLE IF EXISTS %stransactions", config.getTablePrefix());
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sql);
                    }
                    
                    sql = String.format("CREATE TABLE %stransactions AS SELECT * FROM %s",
                            config.getTablePrefix(), checkTable);
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sql);
                    }
                }
            }
            
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
            List<String> backups = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet rs = metaData.getTables(null, null, config.getTablePrefix() + "backup_%", null)) {
                Set<String> backupNames = new HashSet<>();
                
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String backupPrefix = config.getTablePrefix() + "backup_";
                    
                    if (tableName.startsWith(backupPrefix)) {
                        String remaining = tableName.substring(backupPrefix.length());
                        int lastUnderscore = remaining.lastIndexOf('_');
                        if (lastUnderscore > 0) {
                            String backupName = remaining.substring(0, lastUnderscore);
                            backupNames.add(backupName);
                        }
                    }
                }
                
                backups.addAll(backupNames);
                backups.sort(Collections.reverseOrder());
            }
            
            return backups;
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
            String sql = String.format("DELETE FROM %stransactions WHERE timestamp < ?", config.getTablePrefix());
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, thirtyDaysAgo);
                int deleted = stmt.executeUpdate();
                NeoEssentials.LOGGER.info("Deleted {} old transactions during maintenance", deleted);
            }
            
            // Optimize tables
            String[] tables = {"accounts", "transactions"};
            for (String table : tables) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(String.format("OPTIMIZE TABLE %s%s", config.getTablePrefix(), table));
                }
            }
            
            // Remove old backups (keep only last 10)
            List<String> backups = getAvailableBackups();
            if (backups.size() > 10) {
                for (int i = 10; i < backups.size(); i++) {
                    String backupName = backups.get(i);
                    String backupPrefix = config.getTablePrefix() + "backup_" + backupName + "_";
                    
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(String.format("DROP TABLE IF EXISTS %saccounts", backupPrefix));
                        stmt.execute(String.format("DROP TABLE IF EXISTS %stransactions", backupPrefix));
                    }
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
            return new StorageStatistics(0, 0, 0, 0, "MySQL", false);
        }
        
        try {
            long totalAccounts = 0;
            long totalTransactions = 0;
            
            // Count accounts
            String sql = String.format("SELECT COUNT(*) FROM %saccounts", config.getTablePrefix());
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalAccounts = rs.getLong(1);
                }
            }
            
            // Count transactions
            sql = String.format("SELECT COUNT(*) FROM %stransactions", config.getTablePrefix());
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalTransactions = rs.getLong(1);
                }
            }
            
            return new StorageStatistics(totalAccounts, totalTransactions, 0, 
                    0, "MySQL", true);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to get storage statistics", e);
            return new StorageStatistics(0, 0, 0, 0, "MySQL", false);
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
            String sql = String.format("SELECT player_id, balance FROM %saccounts WHERE balance < 0", config.getTablePrefix());
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    warnings.add("Account with negative balance: " + rs.getString("player_id"));
                }
            }
            
            // Check for invalid transactions
            sql = String.format("SELECT id, amount FROM %stransactions WHERE amount <= 0", config.getTablePrefix());
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
        // Create accounts table
        String accountsTable = String.format("""
            CREATE TABLE IF NOT EXISTS %saccounts (
                player_id VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                currency_id VARCHAR(32) NOT NULL,
                last_seen BIGINT NOT NULL,
                created_at BIGINT NOT NULL,
                INDEX idx_balance (balance DESC),
                INDEX idx_last_seen (last_seen)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """, config.getTablePrefix());
        
        // Create transactions table
        String transactionsTable = String.format("""
            CREATE TABLE IF NOT EXISTS %stransactions (
                id VARCHAR(36) PRIMARY KEY,
                from_account VARCHAR(36) NOT NULL,
                to_account VARCHAR(36) NOT NULL,
                amount DECIMAL(20,2) NOT NULL,
                currency_id VARCHAR(32) NOT NULL,
                type VARCHAR(32) NOT NULL,
                description TEXT,
                timestamp BIGINT NOT NULL,
                INDEX idx_timestamp (timestamp DESC),
                INDEX idx_from_account (from_account),
                INDEX idx_to_account (to_account),
                INDEX idx_type (type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """, config.getTablePrefix());
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(accountsTable);
            stmt.execute(transactionsTable);
        }
    }
    
    private EconomyAccount mapResultSetToAccount(ResultSet rs) throws SQLException {
        UUID playerId = UUID.fromString(rs.getString("player_id"));
        String playerName = rs.getString("player_name");
        BigDecimal balance = rs.getBigDecimal("balance");
        String currencyId = rs.getString("currency_id");
        long lastSeen = rs.getLong("last_seen");
        long createdAt = rs.getLong("created_at");
        
        // For now, create a default currency - this should be loaded from configuration
        Currency currency = new Currency(currencyId, currencyId, "$", 2);
        
        return new EconomyAccount(playerId, playerName, balance, currency, lastSeen, createdAt);
    }
    
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID fromAccount = UUID.fromString(rs.getString("from_account"));
        UUID toAccount = UUID.fromString(rs.getString("to_account"));
        BigDecimal amount = rs.getBigDecimal("amount");
        String currencyId = rs.getString("currency_id");
        Transaction.TransactionType type = Transaction.TransactionType.valueOf(rs.getString("type"));
        String description = rs.getString("description");
        long timestamp = rs.getLong("timestamp");
        
        // For now, create a default currency - this should be loaded from configuration
        Currency currency = new Currency(currencyId, currencyId, "$", 2);
        
        return new Transaction(id, fromAccount, toAccount, amount, currency, type, description, timestamp);
    }
}
