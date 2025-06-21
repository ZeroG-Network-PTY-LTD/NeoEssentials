package com.zerog.neoessentials.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DatabaseConfig;
import com.zerog.neoessentials.config.StorageType;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages database connections using connection pooling for better performance and reliability.
 */
public class DatabaseConnectionManager {
    // Singleton instance
    private static DatabaseConnectionManager instance;
    
    // Connection pool
    private HikariDataSource dataSource;
    
    // State tracking
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    
    // Storage configuration
    private StorageType storageType;
    private String dbFile;
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    
    private DatabaseConnectionManager() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance of the database connection manager
     * 
     * @return The instance
     */
    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }
    
    /**
     * Initialize the connection manager with SQLite configuration
     * 
     * @param dbFile The SQLite database file
     * @return True if initialization was successful, false otherwise
     */
    public boolean initializeSQLite(String dbFile) {
        if (initialized.get()) {
            return true; // Already initialized
        }
        
        this.storageType = StorageType.SQLITE;
        this.dbFile = dbFile;
        
        try {
            setupSQLiteConnectionPool();
            initialized.set(true);
            NeoEssentials.LOGGER.info("SQLite database connection pool initialized");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing SQLite database connection pool", e);
            return false;
        }
    }
    
    /**
     * Initialize the connection manager with MySQL configuration
     * 
     * @param config The database configuration
     * @return True if initialization was successful, false otherwise
     */
    public boolean initializeMySQL(DatabaseConfig config) {
        if (initialized.get()) {
            return true; // Already initialized
        }
        
        this.storageType = StorageType.MYSQL;
        this.dbHost = config.mysqlHost.get();
        this.dbPort = config.mysqlPort.get();
        this.dbName = config.mysqlDatabase.get();
        this.dbUser = config.mysqlUsername.get();
        this.dbPassword = config.mysqlPassword.get();
        
        try {
            setupMySQLConnectionPool();
            initialized.set(true);
            NeoEssentials.LOGGER.info("MySQL database connection pool initialized");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing MySQL database connection pool", e);
            return false;
        }
    }
    
    /**
     * Setup the SQLite connection pool
     */
    private void setupSQLiteConnectionPool() throws ClassNotFoundException {
        // Ensure the directory exists
        File dbDir = new File(new File(dbFile).getParent());
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        
        // Load SQLite JDBC driver
        Class.forName("org.sqlite.JDBC");
        
        // Configure HikariCP for SQLite
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite only supports one connection at a time
        
        // SQLite-specific pool properties
        config.addDataSourceProperty("pragmas", "foreign_keys(1)");
        
        // Common pool settings
        setupCommonPoolSettings(config, "SQLitePool");
        
        // Initialize the datasource
        dataSource = new HikariDataSource(config);
    }
    
    /**
     * Setup the MySQL connection pool
     */
    private void setupMySQLConnectionPool() throws ClassNotFoundException {
        // Load MySQL JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");
        
        // Configure HikariCP for MySQL
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        
        // MySQL-specific pool properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("autoReconnect", "true");
        config.addDataSourceProperty("useSSL", "false");
        
        // Set pool size based on expected load
        config.setMaximumPoolSize(10);
        
        // Common pool settings
        setupCommonPoolSettings(config, "MySQLPool");
        
        // Initialize the datasource
        dataSource = new HikariDataSource(config);
    }
    
    /**
     * Setup common connection pool settings
     * 
     * @param config The HikariCP configuration
     * @param poolName The name of the pool
     */
    private void setupCommonPoolSettings(HikariConfig config, String poolName) {
        config.setPoolName("NeoEssentials-" + poolName);
        config.setMinimumIdle(1);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000); // 30 minutes
    }
    
    /**
     * Get a database connection from the pool
     * 
     * @return A database connection
     * @throws SQLException if a connection cannot be acquired
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Close the database connection pool
     */
    public void close() {
        if (shuttingDown.compareAndSet(false, true)) {
            if (dataSource != null && !dataSource.isClosed()) {
                NeoEssentials.LOGGER.info("Closing database connection pool");
                dataSource.close();
                NeoEssentials.LOGGER.info("Database connection pool closed");
            }
            initialized.set(false);
        }
    }
    
    /**
     * Check if the database connection is initialized
     * 
     * @return True if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * Get the storage type being used
     * 
     * @return The storage type
     */
    public StorageType getStorageType() {
        return storageType;
    }
}
