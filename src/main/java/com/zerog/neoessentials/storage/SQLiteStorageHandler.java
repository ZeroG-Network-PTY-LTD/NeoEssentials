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
    private Connection connection;
    private final Gson gson;
    
    public SQLiteStorageHandler() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }
    
    @Override
    public void initialize() {
        try {
            // Create directory if it doesn't exist
            File dir = new File("neoessentials");
            if (!dir.exists() && !dir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory: neoessentials");
                return;
            }
            
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_FILE);
            
            // Create tables
            createTables();
            
            NeoEssentials.LOGGER.info("Initialized SQLite storage handler");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize SQLite storage handler: {}", e.getMessage());
            connection = null;
        }
    }
    
    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
            NeoEssentials.LOGGER.info("SQLite storage handler shut down");
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to close SQLite connection: {}", e.getMessage());
        }
    }
    
    private void createTables() {
        if (connection == null) {
            return;
        }
        
        try (Statement stmt = connection.createStatement()) {
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
            
            // Create transactions table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "amount TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL, " +
                "FOREIGN KEY (uuid) REFERENCES economy(uuid)" +
                ")"
            );
            
            // Create kits table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS kits (" +
                "name TEXT PRIMARY KEY, " +
                "cooldown INTEGER NOT NULL, " +
                "permission TEXT, " +
                "items TEXT NOT NULL" +  // JSON array of items
                ")"
            );
            
            // Create kit cooldowns table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS kit_cooldowns (" +
                "uuid TEXT NOT NULL, " +
                "kit_name TEXT NOT NULL, " +
                "last_use INTEGER NOT NULL, " +
                "PRIMARY KEY (uuid, kit_name), " +
                "FOREIGN KEY (kit_name) REFERENCES kits(name)" +
                ")"
            );
            
            // Create spawn table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS spawn (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1), " +  // Ensure only one spawn record
                "dimension TEXT NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "pitch REAL NOT NULL, " +
                "yaw REAL NOT NULL" +
                ")"
            );
            
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to create SQLite tables: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean saveHomeData(UUID uuid, Map<String, HomeData> homes) {
        if (connection == null) {
            return false;
        }
        
        try {
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
            try {
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
        
        if (connection == null) {
            return homes;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement("SELECT home_name, dimension, x, y, z, pitch, yaw FROM homes WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String homeName = rs.getString("home_name");
                String dimension = rs.getString("dimension");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                float pitch = rs.getFloat("pitch");
                float yaw = rs.getFloat("yaw");
                
                BlockPos pos = new BlockPos(x, y, z);
                homes.put(homeName, new HomeData(dimension, pos, pitch, yaw));
            }
            
            return homes;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load home data for {}: {}", uuid, e.getMessage());
            return homes;
        }
    }
    
    @Override
    public boolean saveWarps(Map<String, WarpData> warps) {
        if (connection == null) {
            return false;
        }
        
        try {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Delete existing warps
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM warps");
            }
            
            // Insert new warps
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO warps (name, dimension, x, y, z, pitch, yaw, permission) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                for (WarpData warp : warps.values()) {
                    BlockPos pos = warp.getPosition();
                    
                    stmt.setString(1, warp.getName());
                    stmt.setString(2, warp.getDimension());
                    stmt.setInt(3, pos.getX());
                    stmt.setInt(4, pos.getY());
                    stmt.setInt(5, pos.getZ());
                    stmt.setFloat(6, warp.getPitch());
                    stmt.setFloat(7, warp.getYaw());
                    stmt.setString(8, warp.getPermission());
                    
                    stmt.executeUpdate();
                }
            }
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction: {}", ex.getMessage());
            }
            
            NeoEssentials.LOGGER.error("Failed to save warps: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, WarpData> loadWarps() {
        Map<String, WarpData> warps = new HashMap<>();
        
        if (connection == null) {
            return warps;
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT name, dimension, x, y, z, pitch, yaw, permission FROM warps");
            
            while (rs.next()) {
                String name = rs.getString("name");
                String dimension = rs.getString("dimension");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                float pitch = rs.getFloat("pitch");
                float yaw = rs.getFloat("yaw");
                String permission = rs.getString("permission");
                
                BlockPos pos = new BlockPos(x, y, z);
                warps.put(name.toLowerCase(), new WarpData(name, dimension, pos, pitch, yaw, permission));
            }
            
            return warps;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load warps: {}", e.getMessage());
            return warps;
        }
    }
    
    @Override
    public boolean saveEconomyData(UUID uuid, EconomyData economyData) {
        if (connection == null) {
            return false;
        }
        
        try {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Upsert economy data
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT OR REPLACE INTO economy (uuid, balance) VALUES (?, ?)")) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, economyData.getBalance().toString());
                
                stmt.executeUpdate();
            }
            
            // Delete old transactions (keeping only the most recent 100)
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM transactions WHERE uuid = ? AND id NOT IN (SELECT id FROM transactions WHERE uuid = ? ORDER BY timestamp DESC LIMIT 100)")) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, uuid.toString());
                
                stmt.executeUpdate();
            }
            
            // Insert new transactions
            if (!economyData.getTransactions().isEmpty()) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO transactions (uuid, description, amount, timestamp) VALUES (?, ?, ?, ?)")) {
                    
                    for (EconomyData.Transaction transaction : economyData.getTransactions()) {
                        stmt.setString(1, uuid.toString());
                        stmt.setString(2, transaction.getDescription());
                        stmt.setString(3, transaction.getAmount().toString());
                        stmt.setLong(4, transaction.getTimestamp());
                        
                        stmt.addBatch();
                    }
                    
                    stmt.executeBatch();
                }
            }
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction: {}", ex.getMessage());
            }
            
            NeoEssentials.LOGGER.error("Failed to save economy data for {}: {}", uuid, e.getMessage());
            return false;
        }
    }
    
    @Override
    public EconomyData loadEconomyData(UUID uuid) {
        if (connection == null) {
            return new EconomyData();
        }
        
        try {
            // Load balance
            BigDecimal balance = BigDecimal.ZERO;
            
            try (PreparedStatement stmt = connection.prepareStatement("SELECT balance FROM economy WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    balance = new BigDecimal(rs.getString("balance"));
                }
            }
            
            EconomyData economyData = new EconomyData(balance);
            
            // Load transactions
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT description, amount, timestamp FROM transactions WHERE uuid = ? ORDER BY timestamp DESC LIMIT 100")) {
                
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                List<EconomyData.Transaction> transactions = new ArrayList<>();
                while (rs.next()) {
                    String description = rs.getString("description");
                    BigDecimal amount = new BigDecimal(rs.getString("amount"));
                    long timestamp = rs.getLong("timestamp");
                    
                    transactions.add(new EconomyData.Transaction(description, amount, timestamp));
                }
                
                // Add transactions in reverse order to ensure oldest first
                for (int i = transactions.size() - 1; i >= 0; i--) {
                    economyData.addTransaction(transactions.get(i));
                }
            }
            
            return economyData;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load economy data for {}: {}", uuid, e.getMessage());
            return new EconomyData();
        }
    }
    
    @Override
    public boolean saveKits(Map<String, KitManager.Kit> kits, Map<UUID, Map<String, Long>> cooldowns) {
        if (connection == null) {
            return false;
        }
        
        try {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Clear existing kits and cooldowns
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM kit_cooldowns");
                stmt.executeUpdate("DELETE FROM kits");
            }
            
            // Insert kits
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO kits (name, cooldown, permission, items) VALUES (?, ?, ?, ?)")) {
                
                for (Map.Entry<String, KitManager.Kit> entry : kits.entrySet()) {
                    KitManager.Kit kit = entry.getValue();
                    
                    // Convert items to JSON
                    JsonArray itemsArray = new JsonArray();
                    for (KitManager.ItemDefinition itemDef : kit.getItemDefinitions()) {
                        JsonObject itemObj = new JsonObject();
                        itemObj.addProperty("id", itemDef.getItemId());
                        itemObj.addProperty("count", itemDef.getCount());
                        itemsArray.add(itemObj);
                    }
                    
                    stmt.setString(1, kit.getName());
                    stmt.setLong(2, kit.getCooldown());
                    stmt.setString(3, kit.getPermission());
                    stmt.setString(4, itemsArray.toString());
                    
                    stmt.executeUpdate();
                }
            }
            
            // Insert cooldowns
            if (!cooldowns.isEmpty()) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO kit_cooldowns (uuid, kit_name, last_use) VALUES (?, ?, ?)")) {
                    
                    for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
                        UUID uuid = entry.getKey();
                        Map<String, Long> playerCooldowns = entry.getValue();
                        
                        for (Map.Entry<String, Long> cooldownEntry : playerCooldowns.entrySet()) {
                            String kitName = cooldownEntry.getKey();
                            long lastUse = cooldownEntry.getValue();
                            
                            stmt.setString(1, uuid.toString());
                            stmt.setString(2, kitName);
                            stmt.setLong(3, lastUse);
                            
                            stmt.addBatch();
                        }
                    }
                    
                    stmt.executeBatch();
                }
            }
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction: {}", ex.getMessage());
            }
            
            NeoEssentials.LOGGER.error("Failed to save kits: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<Object> loadKits() {
        Map<String, KitManager.Kit> kits = new HashMap<>();
        Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
        
        if (connection == null) {
            List<Object> result = new ArrayList<>();
            result.add(kits);
            result.add(cooldowns);
            return result;
        }
        
        try {
            // Load kits
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT name, cooldown, permission, items FROM kits");
                
                while (rs.next()) {
                    String name = rs.getString("name");
                    long cooldown = rs.getLong("cooldown");
                    String permission = rs.getString("permission");
                    String itemsJson = rs.getString("items");
                    
                    KitManager.Kit kit = new KitManager.Kit(name);
                    kit.setCooldown(cooldown);
                    kit.setPermission(permission);
                    
                    // Parse items from JSON
                    try {
                        JsonArray itemsArray = gson.fromJson(itemsJson, JsonArray.class);
                        for (int i = 0; i < itemsArray.size(); i++) {
                            JsonObject itemObj = itemsArray.get(i).getAsJsonObject();
                            String id = itemObj.get("id").getAsString();
                            int count = itemObj.get("count").getAsInt();
                            kit.addItemDefinition(id, count);
                        }
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.error("Failed to parse items for kit {}: {}", name, e.getMessage());
                    }
                    
                    kits.put(name.toLowerCase(), kit);
                }
            }
            
            // Load cooldowns
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT uuid, kit_name, last_use FROM kit_cooldowns");
                
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String kitName = rs.getString("kit_name");
                    long lastUse = rs.getLong("last_use");
                    
                    Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
                    playerCooldowns.put(kitName.toLowerCase(), lastUse);
                }
            }
            
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load kits: {}", e.getMessage());
        }
        
        List<Object> result = new ArrayList<>();
        result.add(kits);
        result.add(cooldowns);
        return result;
    }
    
    @Override
    public boolean saveSpawnData(Map<String, Object> spawn) {
        if (connection == null || !spawn.containsKey("dimension") || !spawn.containsKey("position")) {
            return false;
        }
        
        try {
            String dimension = (String) spawn.get("dimension");
            BlockPos pos = (BlockPos) spawn.get("position");
            float pitch = spawn.containsKey("pitch") ? (Float) spawn.get("pitch") : 0.0f;
            float yaw = spawn.containsKey("yaw") ? (Float) spawn.get("yaw") : 0.0f;
            
            // Delete existing spawn
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM spawn");
            }
            
            // Insert new spawn
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO spawn (id, dimension, x, y, z, pitch, yaw) VALUES (1, ?, ?, ?, ?, ?, ?)")) {
                
                stmt.setString(1, dimension);
                stmt.setInt(2, pos.getX());
                stmt.setInt(3, pos.getY());
                stmt.setInt(4, pos.getZ());
                stmt.setFloat(5, pitch);
                stmt.setFloat(6, yaw);
                
                stmt.executeUpdate();
            }
            
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to save spawn data: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> loadSpawnData() {
        Map<String, Object> spawn = new HashMap<>();
        
        if (connection == null) {
            return spawn;
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT dimension, x, y, z, pitch, yaw FROM spawn WHERE id = 1");
            
            if (rs.next()) {
                String dimension = rs.getString("dimension");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                float pitch = rs.getFloat("pitch");
                float yaw = rs.getFloat("yaw");
                
                spawn.put("dimension", dimension);
                spawn.put("position", new BlockPos(x, y, z));
                spawn.put("pitch", pitch);
                spawn.put("yaw", yaw);
            }
            
            return spawn;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load spawn data: {}", e.getMessage());
            return spawn;
        }
    }
}
