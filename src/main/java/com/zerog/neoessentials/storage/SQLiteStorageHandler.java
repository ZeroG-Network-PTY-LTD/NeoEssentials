package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.HomeData;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.data.WarpData;

import net.minecraft.core.BlockPos;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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
                "other_uuid TEXT, " +
                "transaction_type TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "balance_after REAL NOT NULL, " +
                "description TEXT, " +
                "timestamp INTEGER NOT NULL" +
                ")"
            );
            
            // Create kits table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS kits (" +
                "name TEXT PRIMARY KEY, " +
                "cooldown INTEGER NOT NULL, " +
                "permission TEXT, " +
                "price REAL NOT NULL, " +
                "items_json TEXT NOT NULL" +
                ")"
            );
            
            // Create kit cooldowns table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS kit_cooldowns (" +
                "uuid TEXT NOT NULL, " +
                "kit_name TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL, " +
                "PRIMARY KEY (uuid, kit_name)" +
                ")"
            );
            
            // Create spawn data table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS spawn_data (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1), " + // Ensure only one row
                "spawn_json TEXT NOT NULL" +
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
                if (connection != null) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
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
                    float yaw = rs.getFloat("yaw");                    BlockPos pos = new BlockPos(x, y, z);
                    homes.put(homeName, new HomeData(dimension, pos, pitch, yaw));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load home data for {}: {}", uuid, e.getMessage());
        }
        
        return homes;
    }
    
    @Override
    public boolean saveWarps(Map<String, WarpData> warps) {
        try (Connection connection = connectionManager.getConnection()) {
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
                        stmt.setNull(8, Types.VARCHAR);
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
    public Map<String, WarpData> loadWarps() {
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
                    String permission = rs.getString("permission");                    BlockPos pos = new BlockPos(x, y, z);
                    WarpData warp = new WarpData(warpName, dimension, pos, pitch, yaw, permission);
                    
                    warps.put(warpName, warp);
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load warp data: {}", e.getMessage());
        }
        
        return warps;
    }
    
    @Override
    public boolean saveKits(Map<String, KitManager.Kit> kits, Map<UUID, Map<String, Long>> cooldowns) {
        try (Connection connection = connectionManager.getConnection()) {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Delete all existing kits and cooldowns
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM kits");
                stmt.executeUpdate("DELETE FROM kit_cooldowns");
            }
            
            // Insert kits
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO kits (name, cooldown, permission, price, items_json) VALUES (?, ?, ?, ?, ?)")) {
                
                for (Map.Entry<String, KitManager.Kit> entry : kits.entrySet()) {
                    String kitName = entry.getKey();
                    KitManager.Kit kit = entry.getValue();
                    
                    // Create JSON representation of items
                    JsonArray itemsArray = new JsonArray();
                    for (KitManager.ItemDefinition item : kit.getItemDefinitions()) {
                        JsonObject itemObj = new JsonObject();
                        itemObj.addProperty("id", item.getItemId());
                        itemObj.addProperty("count", item.getCount());
                        itemsArray.add(itemObj);
                    }
                    
                    stmt.setString(1, kitName);
                    stmt.setLong(2, kit.getCooldown());
                    
                    // Permission may be null
                    String permission = kit.getPermission();
                    if (permission != null && !permission.isEmpty()) {
                        stmt.setString(3, permission);
                    } else {
                        stmt.setNull(3, Types.VARCHAR);
                    }
                    
                    stmt.setDouble(4, kit.getPrice());
                    stmt.setString(5, itemsArray.toString());
                    
                    stmt.executeUpdate();
                }
            }
            
            // Insert cooldowns
            if (!cooldowns.isEmpty()) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO kit_cooldowns (uuid, kit_name, timestamp) VALUES (?, ?, ?)")) {
                    
                    for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldowns.entrySet()) {
                        UUID playerUuid = playerEntry.getKey();
                        Map<String, Long> playerCooldowns = playerEntry.getValue();
                        
                        for (Map.Entry<String, Long> cooldownEntry : playerCooldowns.entrySet()) {
                            String kitName = cooldownEntry.getKey();
                            Long timestamp = cooldownEntry.getValue();
                            
                            stmt.setString(1, playerUuid.toString());
                            stmt.setString(2, kitName);
                            stmt.setLong(3, timestamp);
                            
                            stmt.executeUpdate();
                        }
                    }
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
            
            NeoEssentials.LOGGER.error("Failed to save kit data: {}", e.getMessage());
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> loadKits() {
        Map<String, KitManager.Kit> kits = new HashMap<>();
        Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
        
        try (Connection connection = connectionManager.getConnection()) {
            // First check if the kits table exists
            boolean kitsTableExists = false;
            try (Statement checkStmt = connection.createStatement()) {
                try (ResultSet rs = checkStmt.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='kits'")) {
                    kitsTableExists = rs.next();
                }
            }
            
            if (kitsTableExists) {
                // Load kits
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT name, cooldown, permission, price, items_json FROM kits")) {
                    
                    while (rs.next()) {
                        String kitName = rs.getString("name");
                        long cooldown = rs.getLong("cooldown");
                        String permission = rs.getString("permission");
                        double price = rs.getDouble("price");
                        String itemsJson = rs.getString("items_json");
                        
                        // Create kit
                        KitManager.Kit kit = new KitManager.Kit(kitName);
                        kit.setCooldown(cooldown);
                        
                        if (permission != null && !permission.isEmpty()) {
                            kit.setPermission(permission);
                        }
                        
                        kit.setPrice(price);
                        
                        // Parse items
                        try {
                            JsonArray itemsArray = gson.fromJson(itemsJson, JsonArray.class);
                            for (JsonElement itemElement : itemsArray) {
                                JsonObject itemObj = itemElement.getAsJsonObject();
                                String itemId = itemObj.get("id").getAsString();
                                int count = itemObj.get("count").getAsInt();
                                kit.addItemDefinition(itemId, count);
                            }
                        } catch (Exception e) {
                            NeoEssentials.LOGGER.error("Failed to parse items for kit {}: {}", kitName, e.getMessage());
                        }
                        
                        kits.put(kitName, kit);
                    }
                }
                
                // Load cooldowns
                boolean cooldownsTableExists = false;
                try (Statement checkStmt = connection.createStatement()) {
                    try (ResultSet rs = checkStmt.executeQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' AND name='kit_cooldowns'")) {
                        cooldownsTableExists = rs.next();
                    }
                }
                
                if (cooldownsTableExists) {
                    try (Statement stmt = connection.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT uuid, kit_name, timestamp FROM kit_cooldowns")) {
                        
                        while (rs.next()) {
                            String uuidStr = rs.getString("uuid");
                            String kitName = rs.getString("kit_name");
                            long timestamp = rs.getLong("timestamp");
                            
                            try {
                                UUID uuid = UUID.fromString(uuidStr);
                                Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
                                playerCooldowns.put(kitName, timestamp);
                            } catch (IllegalArgumentException e) {
                                NeoEssentials.LOGGER.error("Invalid UUID in kit cooldowns: {}", uuidStr);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load kit data: {}", e.getMessage());
        }
        
        List<Object> result = new ArrayList<>();
        result.add(kits);
        result.add(cooldowns);
        return result;
    }
    
    @Override
    public boolean saveSpawnData(Map<String, Object> spawn) {
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                    "INSERT OR REPLACE INTO spawn_data (id, spawn_json) VALUES (1, ?)")) {
            
            // Convert spawn data to JSON
            JsonObject spawnJson = new JsonObject();
            for (Map.Entry<String, Object> entry : spawn.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    spawnJson.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    if (value instanceof Integer) {
                        spawnJson.addProperty(key, (Integer) value);
                    } else if (value instanceof Float) {
                        spawnJson.addProperty(key, (Float) value);
                    } else if (value instanceof Double) {
                        spawnJson.addProperty(key, (Double) value);
                    } else if (value instanceof Long) {
                        spawnJson.addProperty(key, (Long) value);
                    }
                } else if (value instanceof Boolean) {
                    spawnJson.addProperty(key, (Boolean) value);
                }
            }
            
            stmt.setString(1, spawnJson.toString());
            stmt.executeUpdate();
            
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to save spawn data: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> loadSpawnData() {
        Map<String, Object> spawnData = new HashMap<>();
        
        try (Connection connection = connectionManager.getConnection()) {
            // First check if the table exists
            boolean tableExists = false;
            try (Statement checkStmt = connection.createStatement()) {
                try (ResultSet rs = checkStmt.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='spawn_data'")) {
                    tableExists = rs.next();
                }
            }
            
            if (tableExists) {
                try (PreparedStatement stmt = connection.prepareStatement("SELECT spawn_json FROM spawn_data WHERE id = 1");
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next()) {
                        String spawnJson = rs.getString("spawn_json");
                        JsonObject jsonObject = gson.fromJson(spawnJson, JsonObject.class);
                        
                        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                            String key = entry.getKey();
                            JsonElement element = entry.getValue();
                            
                            if (element.isJsonPrimitive()) {
                                if (element.getAsJsonPrimitive().isString()) {
                                    spawnData.put(key, element.getAsString());
                                } else if (element.getAsJsonPrimitive().isNumber()) {
                                    spawnData.put(key, element.getAsDouble());
                                } else if (element.getAsJsonPrimitive().isBoolean()) {
                                    spawnData.put(key, element.getAsBoolean());
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load spawn data: {}", e.getMessage());
        }
        
        return spawnData;
    }
    
    // Methods for Economy Transactions
    
    /**
     * Saves a transaction for a player
     * 
     * @param uuid The player UUID
     * @param transaction The transaction to save
     * @return True if successful, false otherwise
     */
    public boolean saveTransaction(UUID uuid, EconomyTransaction transaction) {
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                 "INSERT INTO economy_transactions (uuid, other_uuid, transaction_type, amount, balance_after, description, timestamp) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, uuid.toString());
            
            // Other UUID may be null
            UUID otherUUID = transaction.getOtherPlayerUUID();
            if (otherUUID != null) {
                stmt.setString(2, otherUUID.toString());
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            
            stmt.setString(3, transaction.getType());
            stmt.setDouble(4, transaction.getAmount());
            stmt.setDouble(5, transaction.getBalanceAfter());
            stmt.setString(6, transaction.getDescription());
            stmt.setLong(7, transaction.getTimestamp());
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to save transaction for {}: {}", uuid, e.getMessage());
            return false;
        }
    }
    
    /**
     * Loads transactions for a player
     * 
     * @param uuid The player UUID
     * @param limit The maximum number of transactions to load, or 0 for all
     * @return The list of transactions
     */
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
            
            String sql = "SELECT id, uuid, other_uuid, transaction_type, amount, balance_after, description, timestamp " +
                         "FROM economy_transactions WHERE uuid = ? ORDER BY timestamp DESC";
            if (limit > 0) {
                sql += " LIMIT " + limit;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String otherUuidStr = rs.getString("other_uuid");
                        UUID otherUuid = null;
                        
                        if (otherUuidStr != null) {
                            try {
                                otherUuid = UUID.fromString(otherUuidStr);
                            } catch (IllegalArgumentException e) {
                                // Invalid UUID, just leave as null
                            }
                        }
                        
                        String type = rs.getString("transaction_type");
                        double amount = rs.getDouble("amount");
                        double balanceAfter = rs.getDouble("balance_after");
                        String description = rs.getString("description");
                        long timestamp = rs.getLong("timestamp");
                        
                        EconomyTransaction transaction = new EconomyTransaction(
                            id, uuid, otherUuid, type, amount, balanceAfter, description, timestamp
                        );
                        
                        transactions.add(transaction);
                    }
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load transactions for {}: {}", uuid, e.getMessage());
        }
        
        return transactions;
    }
}
