package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyData;
import com.zerog.neoessentials.data.HomeData;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.data.WarpData;
import com.zerog.neoessentials.data.EconomyTransaction;

import net.minecraft.core.BlockPos;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Storage handler that saves data to a SQLite database
 */
public class SQLiteStorageHandler implements StorageHandler {
    private static final String DATABASE_FILE = "neoessentials/database.db";
    private final DatabaseConnectionManager connectionManager;
    private final Gson gson;
    
    public SQLiteStorageHandler() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    @Override
    public void initialize() {
        try {
            // Initialize the connection manager
            if (!connectionManager.initializeSQLite(DATABASE_FILE)) {
                NeoEssentials.LOGGER.error("Failed to initialize database connection manager");
                return;
            }
            
            // Test connection
            try (Connection connection = connectionManager.getConnection()) {
                // Connection test successful if we get here
                NeoEssentials.LOGGER.info("Database connection test successful");
            }
            
            // Create tables
            createTables();
            
            NeoEssentials.LOGGER.info("Initialized SQLite storage handler");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize SQLite storage handler: {}", e.getMessage());
        }
    }
    
    @Override
    public void shutdown() {
        connectionManager.close();
        NeoEssentials.LOGGER.info("SQLite storage handler shut down");
    }
    
    private void createTables() {
        try (Connection connection = connectionManager.getConnection();
             Statement stmt = connection.createStatement()) {
            // Create homes table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS homes (" +
                "uuid TEXT NOT NULL, " +
                "home_name TEXT NOT NULL, " +
                "dimension TEXT NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "pitch REAL NOT NULL, " +
                "yaw REAL NOT NULL, " +
                "PRIMARY KEY (uuid, home_name)" +
                ")"
            );
            
            // Create warps table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS warps (" +
                "name TEXT PRIMARY KEY, " +
                "dimension TEXT NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "pitch REAL NOT NULL, " +
                "yaw REAL NOT NULL, " +
                "permission TEXT" +
                ")"
            );
            
            // Create economy table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS economy (" +
                "uuid TEXT PRIMARY KEY, " +
                "balance TEXT NOT NULL" +
                ")"
            );
            
            // Create economy transactions table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS economy_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT NOT NULL, " +
                "transaction_type TEXT NOT NULL, " +
                "amount TEXT NOT NULL, " +
                "description TEXT, " +
                "timestamp INTEGER NOT NULL, " +
                "FOREIGN KEY (uuid) REFERENCES economy(uuid)" +
                ")"
            );
            
            NeoEssentials.LOGGER.info("Database tables created");
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to create database tables: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean saveHomeData(UUID uuid, Map<String, HomeData> homes) {
        try (Connection connection = connectionManager.getConnection()) {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Delete existing homes for this player
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM homes WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
            
            // Insert new homes
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO homes (uuid, home_name, dimension, x, y, z, pitch, yaw) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                for (Map.Entry<String, HomeData> entry : homes.entrySet()) {
                    String homeName = entry.getKey();
                    HomeData home = entry.getValue();
                    BlockPos pos = home.getPosition();
                    
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, homeName);
                    stmt.setString(3, home.getDimension());
                    stmt.setInt(4, pos.getX());
                    stmt.setInt(5, pos.getY());
                    stmt.setInt(6, pos.getZ());
                    stmt.setFloat(7, home.getPitch());
                    stmt.setFloat(8, home.getYaw());
                    
                    stmt.executeUpdate();
                }
            }
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            try (Connection connection = connectionManager.getConnection()) {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction: {}", ex.getMessage());
            }
            
            NeoEssentials.LOGGER.error("Failed to save home data for {}: {}", uuid, e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, HomeData> loadHomeData(UUID uuid) {
        Map<String, HomeData> homes = new HashMap<>();
        
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT home_name, dimension, x, y, z, pitch, yaw FROM homes WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String homeName = rs.getString("home_name");
                    String dimension = rs.getString("dimension");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    float pitch = rs.getFloat("pitch");
                    float yaw = rs.getFloat("yaw");
                    
                    BlockPos pos = new BlockPos(x, y, z);
                    homes.put(homeName, new HomeData(pos, pitch, yaw, dimension));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load home data for {}: {}", uuid, e.getMessage());
        }
        
        return homes;
    }
    
    @Override
    public boolean saveWarpData(Map<String, WarpData> warps) {
        try (Connection connection = connectionManager.getConnection()) {
            // First check if the table exists
            boolean tableExists = false;
            try (Statement checkStmt = connection.createStatement()) {
                try (ResultSet rs = checkStmt.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='warps'")) {
                    tableExists = rs.next();
                }
            }
            
            // Create the table if it doesn't exist
            if (!tableExists) {
                try (Statement createStmt = connection.createStatement()) {
                    createStmt.execute(
                        "CREATE TABLE warps (" +
                        "name TEXT PRIMARY KEY, " +
                        "dimension TEXT NOT NULL, " +
                        "x INTEGER NOT NULL, " +
                        "y INTEGER NOT NULL, " +
                        "z INTEGER NOT NULL, " +
                        "pitch REAL NOT NULL, " +
                        "yaw REAL NOT NULL, " +
                        "permission TEXT" +
                        ")"
                    );
                }
            }
            
            // Start transaction
            connection.setAutoCommit(false);
            
            // Delete all existing warps
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM warps");
            }
            
            // Insert new warps
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO warps (name, dimension, x, y, z, pitch, yaw, permission) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
                    String warpName = entry.getKey();
                    WarpData warp = entry.getValue();
                    BlockPos pos = warp.getPosition();
                    
                    stmt.setString(1, warpName);
                    stmt.setString(2, warp.getDimension());
                    stmt.setInt(3, pos.getX());
                    stmt.setInt(4, pos.getY());
                    stmt.setInt(5, pos.getZ());
                    stmt.setFloat(6, warp.getPitch());
                    stmt.setFloat(7, warp.getYaw());
                    
                    // Permission may be null
                    String permission = warp.getPermission();
                    if (permission != null && !permission.isEmpty()) {
                        stmt.setString(8, permission);
                    } else {
                        stmt.setNull(8, java.sql.Types.VARCHAR);
                    }
                    
                    stmt.executeUpdate();
                }
            }
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            try (Connection connection = connectionManager.getConnection()) {
                if (connection != null) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction: {}", ex.getMessage());
            }
            
            NeoEssentials.LOGGER.error("Failed to save warp data: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, WarpData> loadWarpData() {
        Map<String, WarpData> warps = new HashMap<>();
        
        try (Connection connection = connectionManager.getConnection();
             Statement stmt = connection.createStatement()) {
            
            // First check if the table exists
            boolean tableExists = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='warps'")) {
                tableExists = rs.next();
            }
            
            if (!tableExists) {
                return warps;
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT name, dimension, x, y, z, pitch, yaw, permission FROM warps")) {
                while (rs.next()) {
                    String warpName = rs.getString("name");
                    String dimension = rs.getString("dimension");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    float pitch = rs.getFloat("pitch");
                    float yaw = rs.getFloat("yaw");
                    String permission = rs.getString("permission");
                    
                    BlockPos pos = new BlockPos(x, y, z);
                    WarpData warp = new WarpData(pos, pitch, yaw, dimension);
                    
                    // Set permission if it's not null
                    if (permission != null && !permission.isEmpty()) {
                        warp.setPermission(permission);
                    }
                    
                    warps.put(warpName, warp);
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load warp data: {}", e.getMessage());
        }
        
        return warps;
    }
    
    @Override
    public boolean saveEconomyData(Map<UUID, EconomyData> economyData) {
        try (Connection connection = connectionManager.getConnection()) {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Delete all existing economy data
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM economy");
            }
            
            // Insert new economy data
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO economy (uuid, balance) VALUES (?, ?)")) {
                
                for (Map.Entry<UUID, EconomyData> entry : economyData.entrySet()) {
                    UUID uuid = entry.getKey();
                    EconomyData data = entry.getValue();
                    
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, data.getBalance().toPlainString());
                    
                    stmt.executeUpdate();
                }
            }
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            try (Connection connection = connectionManager.getConnection()) {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction: {}", ex.getMessage());
            }
            
            NeoEssentials.LOGGER.error("Failed to save economy data: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<UUID, EconomyData> loadEconomyData() {
        Map<UUID, EconomyData> economyData = new HashMap<>();
        
        try (Connection connection = connectionManager.getConnection();
             Statement stmt = connection.createStatement()) {
            
            // First check if the table exists
            boolean tableExists = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='economy'")) {
                tableExists = rs.next();
            }
            
            if (!tableExists) {
                return economyData;
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT uuid, balance FROM economy")) {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String balanceStr = rs.getString("balance");
                    
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        BigDecimal balance = new BigDecimal(balanceStr);
                        
                        EconomyData data = new EconomyData(balance);
                        economyData.put(uuid, data);
                    } catch (IllegalArgumentException e) {
                        NeoEssentials.LOGGER.error("Invalid UUID or balance in economy data: {}, {}", uuidStr, balanceStr);
                    }
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load economy data: {}", e.getMessage());
        }
        
        return economyData;
    }
    
    @Override
    public boolean saveTransaction(UUID uuid, EconomyTransaction transaction) {
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                 "INSERT INTO economy_transactions (uuid, transaction_type, amount, description, timestamp) VALUES (?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, transaction.getType());
            stmt.setString(3, transaction.getAmount().toPlainString());
            stmt.setString(4, transaction.getDescription());
            stmt.setLong(5, transaction.getTimestamp());
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to save transaction for {}: {}", uuid, e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<EconomyTransaction> loadTransactions(UUID uuid, int limit) {
        List<EconomyTransaction> transactions = new ArrayList<>();
        
        try (Connection connection = connectionManager.getConnection()) {
            // First check if the table exists
            boolean tableExists = false;
            try (Statement checkStmt = connection.createStatement()) {
                try (ResultSet rs = checkStmt.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='economy_transactions'")) {
                    tableExists = rs.next();
                }
            }
            
            if (!tableExists) {
                return transactions;
            }
            
            String sql = "SELECT transaction_type, amount, description, timestamp FROM economy_transactions WHERE uuid = ? ORDER BY timestamp DESC";
            if (limit > 0) {
                sql += " LIMIT " + limit;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("transaction_type");
                        String amountStr = rs.getString("amount");
                        String description = rs.getString("description");
                        long timestamp = rs.getLong("timestamp");
                        
                        try {
                            BigDecimal amount = new BigDecimal(amountStr);
                            EconomyTransaction transaction = new EconomyTransaction(type, amount, description, timestamp);
                            transactions.add(transaction);
                        } catch (IllegalArgumentException e) {
                            NeoEssentials.LOGGER.error("Invalid amount in transaction data: {}", amountStr);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load transactions for {}: {}", uuid, e.getMessage());
        }
        
        return transactions;
    }
}
