package com.zerog.neoessentials.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages database discovery and querying for plugin databases.
 * Provides read-only access to SQLite databases with security controls.
 */
public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    
    private final Map<String, DatabaseInfo> discoveredDatabases = new HashMap<>();
    private final Path configDirectory;
    
    private DatabaseManager() {
        this.configDirectory = Paths.get("config");
        discoverDatabases();
    }
    
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Database information
     */
    public static class DatabaseInfo {
        private final String id;
        private final String name;
        private final Path path;
        private final long size;
        private final Instant modified;
        
        public DatabaseInfo(String id, String name, Path path, long size, Instant modified) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.size = size;
            this.modified = modified;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public Path getPath() { return path; }
        public long getSize() { return size; }
        public Instant getModified() { return modified; }
    }
    
    /**
     * Table information
     */
    public static class TableInfo {
        private final String name;
        private final String type;
        private final long rowCount;
        
        public TableInfo(String name, String type, long rowCount) {
            this.name = name;
            this.type = type;
            this.rowCount = rowCount;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public long getRowCount() { return rowCount; }
    }
    
    /**
     * Column information
     */
    public static class ColumnInfo {
        private final int index;
        private final String name;
        private final String type;
        private final boolean notNull;
        private final String defaultValue;
        private final boolean primaryKey;
        
        public ColumnInfo(int index, String name, String type, boolean notNull, 
                          String defaultValue, boolean primaryKey) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.notNull = notNull;
            this.defaultValue = defaultValue;
            this.primaryKey = primaryKey;
        }
        
        public int getIndex() { return index; }
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isNotNull() { return notNull; }
        public String getDefaultValue() { return defaultValue; }
        public boolean isPrimaryKey() { return primaryKey; }
    }
    
    /**
     * Query result
     */
    public static class QueryResult {
        private final List<String> columns;
        private final List<Map<String, Object>> rows;
        private final int totalRows;
        private final int page;
        private final int pageSize;
        private final long executionTime;
        
        public QueryResult(List<String> columns, List<Map<String, Object>> rows, 
                          int totalRows, int page, int pageSize, long executionTime) {
            this.columns = columns;
            this.rows = rows;
            this.totalRows = totalRows;
            this.page = page;
            this.pageSize = pageSize;
            this.executionTime = executionTime;
        }
        
        public List<String> getColumns() { return columns; }
        public List<Map<String, Object>> getRows() { return rows; }
        public int getTotalRows() { return totalRows; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public long getExecutionTime() { return executionTime; }
    }
    
    /**
     * Discover SQLite databases in config directory
     */
    public void discoverDatabases() {
        discoveredDatabases.clear();
        
        if (!Files.exists(configDirectory)) {
            LOGGER.warn("Config directory does not exist: {}", configDirectory);
            return;
        }
        
        try (Stream<Path> paths = Files.walk(configDirectory, 3)) {
            paths.filter(path -> path.toString().endsWith(".db") || 
                                 path.toString().endsWith(".sqlite") ||
                                 path.toString().endsWith(".sqlite3"))
                 .forEach(this::registerDatabase);
        } catch (IOException e) {
            LOGGER.error("Failed to discover databases", e);
        }
        
        LOGGER.info("Discovered {} database(s)", discoveredDatabases.size());
    }
    
    /**
     * Register a discovered database
     */
    private void registerDatabase(Path dbPath) {
        try {
            if (!Files.exists(dbPath) || !Files.isRegularFile(dbPath)) {
                return;
            }
            
            String fileName = dbPath.getFileName().toString();
            String id = generateDatabaseId(dbPath);
            long size = Files.size(dbPath);
            Instant modified = Files.getLastModifiedTime(dbPath).toInstant();
            
            DatabaseInfo info = new DatabaseInfo(id, fileName, dbPath, size, modified);
            discoveredDatabases.put(id, info);
            
            LOGGER.debug("Registered database: {} at {}", fileName, dbPath);
            
        } catch (IOException e) {
            LOGGER.warn("Failed to register database: {}", dbPath, e);
        }
    }
    
    /**
     * Generate unique database ID from path
     */
    private String generateDatabaseId(Path path) {
        String relativePath = configDirectory.relativize(path).toString();
        return relativePath.replace("\\", "/").replace(".db", "")
                          .replace(".sqlite", "").replace(".sqlite3", "");
    }
    
    /**
     * Get all discovered databases
     */
    public List<DatabaseInfo> getDatabases() {
        return new ArrayList<>(discoveredDatabases.values());
    }
    
    /**
     * Get database by ID
     */
    public DatabaseInfo getDatabase(String databaseId) {
        return discoveredDatabases.get(databaseId);
    }
    
    /**
     * Get connection to database
     */
    private Connection getConnection(String databaseId) throws SQLException {
        DatabaseInfo db = discoveredDatabases.get(databaseId);
        if (db == null) {
            throw new SQLException("Database not found: " + databaseId);
        }
        
        String url = "jdbc:sqlite:" + db.getPath().toString();
        Connection conn = DriverManager.getConnection(url);
        conn.setReadOnly(true); // Read-only for safety
        return conn;
    }
    
    /**
     * Get tables in database
     */
    public List<TableInfo> getTables(String databaseId) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        
        try (Connection conn = getConnection(databaseId)) {
            DatabaseMetaData meta = conn.getMetaData();
            
            try (ResultSet rs = meta.getTables(null, null, null, new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableType = rs.getString("TABLE_TYPE");
                    
                    // Get row count
                    long rowCount = 0;
                    try (Statement stmt = conn.createStatement();
                         ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"")) {
                        if (countRs.next()) {
                            rowCount = countRs.getLong(1);
                        }
                    } catch (SQLException e) {
                        LOGGER.warn("Failed to get row count for table: {}", tableName);
                    }
                    
                    tables.add(new TableInfo(tableName, tableType, rowCount));
                }
            }
        }
        
        return tables;
    }
    
    /**
     * Get table schema (columns)
     */
    public List<ColumnInfo> getTableSchema(String databaseId, String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        
        try (Connection conn = getConnection(databaseId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(\"" + tableName + "\")")) {
            
            while (rs.next()) {
                int cid = rs.getInt("cid");
                String name = rs.getString("name");
                String type = rs.getString("type");
                boolean notNull = rs.getInt("notnull") == 1;
                String defaultValue = rs.getString("dflt_value");
                boolean pk = rs.getInt("pk") > 0;
                
                columns.add(new ColumnInfo(cid, name, type, notNull, defaultValue, pk));
            }
        }
        
        return columns;
    }
    
    /**
     * Execute SELECT query with pagination
     */
    public QueryResult executeQuery(String databaseId, String query, int page, int pageSize) 
            throws SQLException {
        
        // Validate query is SELECT only
        String trimmedQuery = query.trim().toUpperCase();
        if (!trimmedQuery.startsWith("SELECT")) {
            throw new SQLException("Only SELECT queries are allowed");
        }
        
        // Prevent dangerous operations
        if (trimmedQuery.contains("ATTACH") || trimmedQuery.contains("PRAGMA")) {
            throw new SQLException("Query contains forbidden operations");
        }
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = getConnection(databaseId)) {
            // Get total count first
            int totalRows = 0;
            String countQuery = "SELECT COUNT(*) FROM (" + query + ")";
            try (Statement countStmt = conn.createStatement();
                 ResultSet countRs = countStmt.executeQuery(countQuery)) {
                if (countRs.next()) {
                    totalRows = countRs.getInt(1);
                }
            }
            
            // Execute paginated query
            int offset = (page - 1) * pageSize;
            String paginatedQuery = query + " LIMIT " + pageSize + " OFFSET " + offset;
            
            List<String> columns = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(paginatedQuery)) {
                
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                
                // Get column names
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnName(i));
                }
                
                // Get rows
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    rows.add(row);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new QueryResult(columns, rows, totalRows, page, pageSize, executionTime);
        }
    }
    
    /**
     * Export table data as CSV
     */
    public String exportTableAsCSV(String databaseId, String tableName) throws SQLException {
        StringBuilder csv = new StringBuilder();
        
        try (Connection conn = getConnection(databaseId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM \"" + tableName + "\"")) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            
            // Header row
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) csv.append(",");
                csv.append(escapeCSV(meta.getColumnName(i)));
            }
            csv.append("\n");
            
            // Data rows
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) csv.append(",");
                    Object value = rs.getObject(i);
                    csv.append(escapeCSV(value != null ? value.toString() : ""));
                }
                csv.append("\n");
            }
        }
        
        return csv.toString();
    }
    
    /**
     * Escape CSV value
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * Get database statistics
     */
    public Map<String, Object> getDatabaseStats(String databaseId) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        DatabaseInfo db = getDatabase(databaseId);
        if (db == null) {
            throw new SQLException("Database not found: " + databaseId);
        }
        
        stats.put("name", db.getName());
        stats.put("size", db.getSize());
        stats.put("sizeFormatted", formatFileSize(db.getSize()));
        stats.put("modified", db.getModified().toString());
        
        try (Connection conn = getConnection(databaseId)) {
            // Get table count
            List<TableInfo> tables = getTables(databaseId);
            stats.put("tableCount", tables.size());
            
            // Get total row count
            long totalRows = tables.stream().mapToLong(TableInfo::getRowCount).sum();
            stats.put("totalRows", totalRows);
            
            // Get database version
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT sqlite_version()")) {
                if (rs.next()) {
                    stats.put("sqliteVersion", rs.getString(1));
                }
            }
        }
        
        return stats;
    }
    
    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }
}
