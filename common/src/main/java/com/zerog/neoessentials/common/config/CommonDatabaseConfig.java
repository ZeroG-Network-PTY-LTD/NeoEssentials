package com.zerog.neoessentials.common.config;

/**
 * Common database configuration class that is version-independent.
 * This contains the shared settings and types that apply across all versions.
 */
public class CommonDatabaseConfig {
    
    /**
     * Enumeration of storage types
     */
    public enum StorageType {
        JSON,
        SQLITE,
        MYSQL
    }
    
    // Storage type
    private StorageType storageType = StorageType.JSON;
    
    // MySQL settings
    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "neoessentials";
    private String mysqlUsername = "root";
    private String mysqlPassword = "";
    private boolean mysqlUseSSL = false;
    private String mysqlTablePrefix = "ne_";
    
    // SQLite settings
    private String sqliteFilename = "neoessentials.db";
    
    /**
     * Get the storage type
     * 
     * @return The storage type
     */
    public StorageType getStorageType() {
        return storageType;
    }
    
    /**
     * Set the storage type
     * 
     * @param storageType The storage type
     */
    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }
    
    /**
     * Get the MySQL host
     * 
     * @return The MySQL host
     */
    public String getMysqlHost() {
        return mysqlHost;
    }
    
    /**
     * Set the MySQL host
     * 
     * @param mysqlHost The MySQL host
     */
    public void setMysqlHost(String mysqlHost) {
        this.mysqlHost = mysqlHost;
    }
    
    /**
     * Get the MySQL port
     * 
     * @return The MySQL port
     */
    public int getMysqlPort() {
        return mysqlPort;
    }
    
    /**
     * Set the MySQL port
     * 
     * @param mysqlPort The MySQL port
     */
    public void setMysqlPort(int mysqlPort) {
        this.mysqlPort = mysqlPort;
    }
    
    /**
     * Get the MySQL database name
     * 
     * @return The MySQL database name
     */
    public String getMysqlDatabase() {
        return mysqlDatabase;
    }
    
    /**
     * Set the MySQL database name
     * 
     * @param mysqlDatabase The MySQL database name
     */
    public void setMysqlDatabase(String mysqlDatabase) {
        this.mysqlDatabase = mysqlDatabase;
    }
    
    /**
     * Get the MySQL username
     * 
     * @return The MySQL username
     */
    public String getMysqlUsername() {
        return mysqlUsername;
    }
    
    /**
     * Set the MySQL username
     * 
     * @param mysqlUsername The MySQL username
     */
    public void setMysqlUsername(String mysqlUsername) {
        this.mysqlUsername = mysqlUsername;
    }
    
    /**
     * Get the MySQL password
     * 
     * @return The MySQL password
     */
    public String getMysqlPassword() {
        return mysqlPassword;
    }
    
    /**
     * Set the MySQL password
     * 
     * @param mysqlPassword The MySQL password
     */
    public void setMysqlPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword;
    }
    
    /**
     * Get whether to use SSL for MySQL connections
     * 
     * @return Whether to use SSL for MySQL connections
     */
    public boolean isMysqlUseSSL() {
        return mysqlUseSSL;
    }
    
    /**
     * Set whether to use SSL for MySQL connections
     * 
     * @param mysqlUseSSL Whether to use SSL for MySQL connections
     */
    public void setMysqlUseSSL(boolean mysqlUseSSL) {
        this.mysqlUseSSL = mysqlUseSSL;
    }
    
    /**
     * Get the MySQL table prefix
     * 
     * @return The MySQL table prefix
     */
    public String getMysqlTablePrefix() {
        return mysqlTablePrefix;
    }
    
    /**
     * Set the MySQL table prefix
     * 
     * @param mysqlTablePrefix The MySQL table prefix
     */
    public void setMysqlTablePrefix(String mysqlTablePrefix) {
        this.mysqlTablePrefix = mysqlTablePrefix;
    }
    
    /**
     * Get the SQLite filename
     * 
     * @return The SQLite filename
     */
    public String getSqliteFilename() {
        return sqliteFilename;
    }
    
    /**
     * Set the SQLite filename
     * 
     * @param sqliteFilename The SQLite filename
     */
    public void setSqliteFilename(String sqliteFilename) {
        this.sqliteFilename = sqliteFilename;
    }
    
    /**
     * Get the MySQL connection URL
     * 
     * @return The MySQL connection URL
     */
    public String getMysqlConnectionUrl() {
        String sslParam = mysqlUseSSL ? "useSSL=true" : "useSSL=false";
        return "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase + "?" + sslParam;
    }
    
    /**
     * Get the SQLite connection URL
     * 
     * @param basePath The base path for SQLite database file
     * @return The SQLite connection URL
     */
    public String getSqliteConnectionUrl(String basePath) {
        return "jdbc:sqlite:" + basePath + "/" + sqliteFilename;
    }
}
