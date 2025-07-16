package com.zerog.neoessentials.data;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.StorageType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
import java.util.concurrent.TimeUnit;
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
import java.util.concurrent.TimeUnit;
>>>>>>> 5791cf5 (feat: Implement connection pooling with HikariCP for improved database performance and reliability)
=======
import java.util.concurrent.TimeUnit;
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d

/**
 * Manages database operations for the NeoEssentials mod.
 * Supports both SQLite and MySQL database backends.
 */
public class DatabaseManager {
    private Connection connection;
    private final StorageType storageType;
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;
    private final String dbFile;
    private boolean initialized = false;
    
    /**
     * Constructor for the DatabaseManager
     * 
     * @param storageType The type of storage (JSON, SQLITE, MYSQL)
     * @param dbHost MySQL host address (ignored for SQLite)
     * @param dbPort MySQL port (ignored for SQLite)
     * @param dbName Database name (or SQLite filename without extension)
     * @param dbUser MySQL username (ignored for SQLite)
     * @param dbPassword MySQL password (ignored for SQLite)
     */
    public DatabaseManager(StorageType storageType, String dbHost, int dbPort, String dbName, String dbUser, String dbPassword) {
        this.storageType = storageType;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.dbFile = "neoessentials" + File.separator + dbName + ".db";
    }
    
    /**
     * Initialize the database connection and create necessary tables
     */
    public void initialize() {
        if (storageType == StorageType.JSON) {
            NeoEssentials.LOGGER.info("Using JSON storage, skipping database initialization");
            return;
        }
        
        try {
            NeoEssentials.LOGGER.info("Initializing database connection for " + storageType);
            connection = connectToDatabase();
            
            if (connection != null) {
                createTables();
                initialized = true;
                NeoEssentials.LOGGER.info("Database initialized successfully");
            } else {
                NeoEssentials.LOGGER.error("Failed to establish database connection");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing database", e);
        }
    }
    
    /**
     * Connect to the database based on storage type
     * 
     * @return The database connection
     */
    private Connection connectToDatabase() {
        try {
            if (storageType == StorageType.SQLITE) {
                // Ensure the directory exists
                File dbDir = new File("neoessentials");
                if (!dbDir.exists()) {
                    dbDir.mkdirs();
                }
                
                // Load SQLite JDBC driver
                Class.forName("org.sqlite.JDBC");
                
                // Connect to SQLite
                String url = "jdbc:sqlite:" + dbFile;
                NeoEssentials.LOGGER.info("Connecting to SQLite database: " + url);
                return DriverManager.getConnection(url);
            } else if (storageType == StorageType.MYSQL) {
                // Load MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Connect to MySQL
                String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
                Properties props = new Properties();
                props.setProperty("user", dbUser);
                props.setProperty("password", dbPassword);
                props.setProperty("useSSL", "false");
                props.setProperty("autoReconnect", "true");
                
                NeoEssentials.LOGGER.info("Connecting to MySQL database: " + url);
                return DriverManager.getConnection(url, props);
            }
        } catch (ClassNotFoundException e) {
            NeoEssentials.LOGGER.error("Database driver not found", e);
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error connecting to database", e);
        }
        return null;
    }
    
    /**
     * Create the necessary tables in the database
     */
    private void createTables() {
        if (connection == null) return;
        
        try (Statement stmt = connection.createStatement()) {
            // Create player data table
            stmt.execute("CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "balance DOUBLE DEFAULT 0.0, " + 
                    "last_login BIGINT DEFAULT 0" +
                    ")");
            
            // Create homes table
            stmt.execute("CREATE TABLE IF NOT EXISTS homes (" +
                    "id INTEGER PRIMARY KEY " + (storageType == StorageType.SQLITE ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "name VARCHAR(32) NOT NULL, " +
                    "dimension VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL, " +
                    "UNIQUE (owner_uuid, name)" +
                    ")");
            
            // Create warps table
            stmt.execute("CREATE TABLE IF NOT EXISTS warps (" +
                    "name VARCHAR(32) PRIMARY KEY, " +
                    "dimension VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL" +
                    ")");
            
            // Create kits table
            stmt.execute("CREATE TABLE IF NOT EXISTS kits (" +
                    "name VARCHAR(32) PRIMARY KEY, " +
                    "cooldown INTEGER NOT NULL, " +
                    "items TEXT NOT NULL" +
                    ")");
            
            // Create kit usage table
            stmt.execute("CREATE TABLE IF NOT EXISTS kit_usage (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "kit_name VARCHAR(32) NOT NULL, " +
                    "last_used BIGINT NOT NULL, " +
                    "PRIMARY KEY (uuid, kit_name)" +
                    ")");
            
            NeoEssentials.LOGGER.info("Database tables created or verified");
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error creating database tables", e);
        }
    }
    
    /**
     * Close the database connection
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                NeoEssentials.LOGGER.info("Database connection closed");
            } catch (SQLException e) {
                NeoEssentials.LOGGER.error("Error closing database connection", e);
            }
        }
    }
    
    /**
     * Check if the database connection is initialized
     * 
     * @return True if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get the storage type being used
     * 
     * @return The storage type
     */
    public StorageType getStorageType() {
        return storageType;
    }
    
    /**
     * Save a player's balance to the database
     * 
     * @param uuid The player's UUID
     * @param name The player's name
     * @param balance The player's balance
     * @return True if successful, false otherwise
     */
    public boolean savePlayerBalance(UUID uuid, String name, double balance) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "INSERT INTO player_data (uuid, name, balance) VALUES (?, ?, ?) " +
                "ON " + (storageType == StorageType.SQLITE ? "CONFLICT" : "DUPLICATE KEY") + 
                " UPDATE SET name = ?, balance = ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setDouble(3, balance);
            stmt.setString(4, name);
            stmt.setDouble(5, balance);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error saving player balance", e);
            return false;
        }
    }
    
    /**
     * Load all player balances from the database
     * 
     * @return Map of player UUIDs to balances
     */
    public Map<UUID, Double> loadAllBalances() {
        Map<UUID, Double> balances = new HashMap<>();
        
        if (!initialized || storageType == StorageType.JSON) return balances;
        
        String sql = "SELECT uuid, balance FROM player_data";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                double balance = rs.getDouble("balance");
                balances.put(uuid, balance);
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error loading player balances", e);
        }
        
        return balances;
    }
    
    /**
     * Save a home to the database
     * 
     * @param ownerUuid The owner's UUID
     * @param homeName The home name
     * @param dimension The dimension
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param yaw The yaw rotation
     * @param pitch The pitch rotation
     * @return True if successful, false otherwise
     */
    public boolean saveHome(UUID ownerUuid, String homeName, String dimension, double x, double y, double z, float yaw, float pitch) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "INSERT INTO homes (owner_uuid, name, dimension, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON " + (storageType == StorageType.SQLITE ? "CONFLICT" : "DUPLICATE KEY") + 
                " UPDATE SET dimension = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setString(2, homeName);
            stmt.setString(3, dimension);
            stmt.setDouble(4, x);
            stmt.setDouble(5, y);
            stmt.setDouble(6, z);
            stmt.setFloat(7, yaw);
            stmt.setFloat(8, pitch);
            stmt.setString(9, dimension);
            stmt.setDouble(10, x);
            stmt.setDouble(11, y);
            stmt.setDouble(12, z);
            stmt.setFloat(13, yaw);
            stmt.setFloat(14, pitch);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error saving home", e);
            return false;
        }
    }
    
    /**
     * Load a player's homes from the database
     * 
     * @param ownerUuid The player's UUID
     * @return Map of home names to home locations
     */
    public Map<String, HomeLocation> loadHomes(UUID ownerUuid) {
        Map<String, HomeLocation> homes = new HashMap<>();
        
        if (!initialized || storageType == StorageType.JSON) return homes;
        
        String sql = "SELECT name, dimension, x, y, z, yaw, pitch FROM homes WHERE owner_uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String dimension = rs.getString("dimension");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float yaw = rs.getFloat("yaw");
                    float pitch = rs.getFloat("pitch");
                    
                    homes.put(name, new HomeLocation(dimension, x, y, z, yaw, pitch));
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error loading homes for player: " + ownerUuid, e);
        }
        
        return homes;
    }
    
    /**
     * Delete a home from the database
     * 
     * @param ownerUuid The owner's UUID
     * @param homeName The home name
     * @return True if successful, false otherwise
     */
    public boolean deleteHome(UUID ownerUuid, String homeName) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "DELETE FROM homes WHERE owner_uuid = ? AND name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setString(2, homeName);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error deleting home", e);
            return false;
        }
    }
    
    /**
     * Save a warp to the database
     * 
     * @param warpName The warp name
     * @param dimension The dimension
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param yaw The yaw rotation
     * @param pitch The pitch rotation
     * @return True if successful, false otherwise
     */
    public boolean saveWarp(String warpName, String dimension, double x, double y, double z, float yaw, float pitch) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "INSERT INTO warps (name, dimension, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON " + (storageType == StorageType.SQLITE ? "CONFLICT" : "DUPLICATE KEY") + 
                " UPDATE SET dimension = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, warpName);
            stmt.setString(2, dimension);
            stmt.setDouble(3, x);
            stmt.setDouble(4, y);
            stmt.setDouble(5, z);
            stmt.setFloat(6, yaw);
            stmt.setFloat(7, pitch);
            stmt.setString(8, dimension);
            stmt.setDouble(9, x);
            stmt.setDouble(10, y);
            stmt.setDouble(11, z);
            stmt.setFloat(12, yaw);
            stmt.setFloat(13, pitch);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error saving warp", e);
            return false;
        }
    }
    
    /**
     * Load all warps from the database
     * 
     * @return Map of warp names to warp locations
     */
    public Map<String, HomeLocation> loadWarps() {
        Map<String, HomeLocation> warps = new HashMap<>();
        
        if (!initialized || storageType == StorageType.JSON) return warps;
        
        String sql = "SELECT name, dimension, x, y, z, yaw, pitch FROM warps";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                String dimension = rs.getString("dimension");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                
                warps.put(name, new HomeLocation(dimension, x, y, z, yaw, pitch));
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error loading warps", e);
        }
        
        return warps;
    }
    
    /**
     * Delete a warp from the database
     * 
     * @param warpName The warp name
     * @return True if successful, false otherwise
     */
    public boolean deleteWarp(String warpName) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "DELETE FROM warps WHERE name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, warpName);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error deleting warp", e);
            return false;
        }
    }
    
    /**
     * Save a kit to the database
     * 
     * @param kitName The kit name
     * @param cooldown The kit cooldown in seconds
     * @param items JSON string of item data
     * @return True if successful, false otherwise
     */
    public boolean saveKit(String kitName, int cooldown, String items) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "INSERT INTO kits (name, cooldown, items) VALUES (?, ?, ?) " +
                "ON " + (storageType == StorageType.SQLITE ? "CONFLICT" : "DUPLICATE KEY") + 
                " UPDATE SET cooldown = ?, items = ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kitName);
            stmt.setInt(2, cooldown);
            stmt.setString(3, items);
            stmt.setInt(4, cooldown);
            stmt.setString(5, items);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error saving kit", e);
            return false;
        }
    }
    
    /**
     * Load all kits from the database
     * 
     * @return Map of kit names to [cooldown, items] array
     */
    public Map<String, Object[]> loadKits() {
        Map<String, Object[]> kits = new HashMap<>();
        
        if (!initialized || storageType == StorageType.JSON) return kits;
        
        String sql = "SELECT name, cooldown, items FROM kits";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                int cooldown = rs.getInt("cooldown");
                String items = rs.getString("items");
                
                kits.put(name, new Object[]{cooldown, items});
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error loading kits", e);
        }
        
        return kits;
    }
    
    /**
     * Delete a kit from the database
     * 
     * @param kitName The kit name
     * @return True if successful, false otherwise
     */
    public boolean deleteKit(String kitName) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "DELETE FROM kits WHERE name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kitName);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error deleting kit", e);
            return false;
        }
    }
    
    /**
     * Record kit usage
     * 
     * @param playerUuid The player's UUID
     * @param kitName The kit name
     * @param timestamp The timestamp of usage
     * @return True if successful, false otherwise
     */
    public boolean recordKitUsage(UUID playerUuid, String kitName, long timestamp) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "INSERT INTO kit_usage (uuid, kit_name, last_used) VALUES (?, ?, ?) " +
                "ON " + (storageType == StorageType.SQLITE ? "CONFLICT" : "DUPLICATE KEY") + 
                " UPDATE SET last_used = ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, kitName);
            stmt.setLong(3, timestamp);
            stmt.setLong(4, timestamp);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error recording kit usage", e);
            return false;
        }
    }
    
    /**
     * Get last kit usage timestamp
     * 
     * @param playerUuid The player's UUID
     * @param kitName The kit name
     * @return The timestamp of last usage, or 0 if never used
     */
    public long getLastKitUsage(UUID playerUuid, String kitName) {
        if (!initialized || storageType == StorageType.JSON) return 0;
        
        String sql = "SELECT last_used FROM kit_usage WHERE uuid = ? AND kit_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, kitName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("last_used");
                }
            }
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error getting last kit usage", e);
        }
        
        return 0;
    }
    
    /**
     * Update a player's last login time
     * 
     * @param uuid The player's UUID
     * @param name The player's name
     * @param timestamp The login timestamp
     * @return True if successful, false otherwise
     */
    public boolean updatePlayerLogin(UUID uuid, String name, long timestamp) {
        if (!initialized || storageType == StorageType.JSON) return false;
        
        String sql = "INSERT INTO player_data (uuid, name, last_login) VALUES (?, ?, ?) " +
                "ON " + (storageType == StorageType.SQLITE ? "CONFLICT" : "DUPLICATE KEY") + 
                " UPDATE SET name = ?, last_login = ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setLong(3, timestamp);
            stmt.setString(4, name);
            stmt.setLong(5, timestamp);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Error updating player login", e);
            return false;
        }
    }
}
