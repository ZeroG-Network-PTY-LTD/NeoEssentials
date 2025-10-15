package com.zerog.neoessentials.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API handler for database browsing.
 * Provides read-only access to plugin databases.
 */
public class DatabaseHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHandler.class);
    private final Gson gson = new Gson();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        try {
            // Remove /api/database prefix
            String endpoint = path.replace("/api/database", "");
            
            if (endpoint.isEmpty() || endpoint.equals("/")) {
                endpoint = "/list";
            }
            
            switch (endpoint) {
                case "/list":
                    if ("GET".equals(method)) {
                        handleListDatabases(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/tables":
                    if ("GET".equals(method)) {
                        handleListTables(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/schema":
                    if ("GET".equals(method)) {
                        handleGetSchema(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/query":
                    if ("POST".equals(method)) {
                        handleQuery(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/export":
                    if ("GET".equals(method)) {
                        handleExport(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/stats":
                    if ("GET".equals(method)) {
                        handleStats(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                case "/refresh":
                    if ("POST".equals(method)) {
                        handleRefresh(exchange);
                    } else {
                        sendMethodNotAllowed(exchange);
                    }
                    break;
                    
                default:
                    sendNotFound(exchange);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling database request", e);
            sendError(exchange, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/database/list
     * List all discovered databases
     */
    private void handleListDatabases(HttpExchange exchange) throws IOException {
        List<DatabaseManager.DatabaseInfo> databases = DatabaseManager.getInstance().getDatabases();
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("count", databases.size());
        
        JsonArray dbArray = new JsonArray();
        for (DatabaseManager.DatabaseInfo db : databases) {
            JsonObject dbObj = new JsonObject();
            dbObj.addProperty("id", db.getId());
            dbObj.addProperty("name", db.getName());
            dbObj.addProperty("path", db.getPath().toString());
            dbObj.addProperty("size", db.getSize());
            dbObj.addProperty("sizeFormatted", formatFileSize(db.getSize()));
            dbObj.addProperty("modified", db.getModified().toString());
            dbArray.add(dbObj);
        }
        response.add("databases", dbArray);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * GET /api/database/tables?database=neoessentials/economy
     * List tables in database
     */
    private void handleListTables(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String databaseId = params.get("database");
        
        if (databaseId == null || databaseId.isEmpty()) {
            sendBadRequest(exchange, "Missing 'database' parameter");
            return;
        }
        
        try {
            List<DatabaseManager.TableInfo> tables = DatabaseManager.getInstance().getTables(databaseId);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("database", databaseId);
            response.addProperty("tableCount", tables.size());
            
            JsonArray tablesArray = new JsonArray();
            for (DatabaseManager.TableInfo table : tables) {
                JsonObject tableObj = new JsonObject();
                tableObj.addProperty("name", table.getName());
                tableObj.addProperty("type", table.getType());
                tableObj.addProperty("rowCount", table.getRowCount());
                tablesArray.add(tableObj);
            }
            response.add("tables", tablesArray);
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (SQLException e) {
            LOGGER.error("Failed to list tables", e);
            sendError(exchange, "Failed to list tables: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/database/schema?database=neoessentials/economy&table=accounts
     * Get table schema
     */
    private void handleGetSchema(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String databaseId = params.get("database");
        String tableName = params.get("table");
        
        if (databaseId == null || databaseId.isEmpty()) {
            sendBadRequest(exchange, "Missing 'database' parameter");
            return;
        }
        if (tableName == null || tableName.isEmpty()) {
            sendBadRequest(exchange, "Missing 'table' parameter");
            return;
        }
        
        try {
            List<DatabaseManager.ColumnInfo> columns = 
                DatabaseManager.getInstance().getTableSchema(databaseId, tableName);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("database", databaseId);
            response.addProperty("table", tableName);
            response.addProperty("columnCount", columns.size());
            
            JsonArray columnsArray = new JsonArray();
            for (DatabaseManager.ColumnInfo column : columns) {
                JsonObject colObj = new JsonObject();
                colObj.addProperty("index", column.getIndex());
                colObj.addProperty("name", column.getName());
                colObj.addProperty("type", column.getType());
                colObj.addProperty("notNull", column.isNotNull());
                colObj.addProperty("defaultValue", column.getDefaultValue());
                colObj.addProperty("primaryKey", column.isPrimaryKey());
                columnsArray.add(colObj);
            }
            response.add("columns", columnsArray);
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (SQLException e) {
            LOGGER.error("Failed to get schema", e);
            sendError(exchange, "Failed to get schema: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/database/query
     * Execute SELECT query with pagination
     * Body: {"database": "id", "query": "SELECT * FROM table", "page": 1, "pageSize": 100}
     */
    private void handleQuery(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject request = gson.fromJson(body, JsonObject.class);
            
            String databaseId = request.has("database") ? request.get("database").getAsString() : null;
            String query = request.has("query") ? request.get("query").getAsString() : null;
            int page = request.has("page") ? request.get("page").getAsInt() : 1;
            int pageSize = request.has("pageSize") ? request.get("pageSize").getAsInt() : 100;
            
            if (databaseId == null || databaseId.isEmpty()) {
                sendBadRequest(exchange, "Missing 'database' field");
                return;
            }
            if (query == null || query.isEmpty()) {
                sendBadRequest(exchange, "Missing 'query' field");
                return;
            }
            
            // Validate page and pageSize
            page = Math.max(1, page);
            pageSize = Math.max(10, Math.min(pageSize, 1000));
            
            try {
                DatabaseManager.QueryResult result = 
                    DatabaseManager.getInstance().executeQuery(databaseId, query, page, pageSize);
                
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("database", databaseId);
                response.addProperty("query", query);
                response.addProperty("page", result.getPage());
                response.addProperty("pageSize", result.getPageSize());
                response.addProperty("totalRows", result.getTotalRows());
                response.addProperty("totalPages", (int) Math.ceil((double) result.getTotalRows() / result.getPageSize()));
                response.addProperty("executionTime", result.getExecutionTime());
                
                JsonArray columnsArray = new JsonArray();
                for (String column : result.getColumns()) {
                    columnsArray.add(column);
                }
                response.add("columns", columnsArray);
                
                JsonArray rowsArray = new JsonArray();
                for (Map<String, Object> row : result.getRows()) {
                    JsonObject rowObj = new JsonObject();
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        Object value = entry.getValue();
                        if (value == null) {
                            rowObj.add(entry.getKey(), null);
                        } else if (value instanceof Number) {
                            rowObj.addProperty(entry.getKey(), (Number) value);
                        } else if (value instanceof Boolean) {
                            rowObj.addProperty(entry.getKey(), (Boolean) value);
                        } else {
                            rowObj.addProperty(entry.getKey(), value.toString());
                        }
                    }
                    rowsArray.add(rowObj);
                }
                response.add("rows", rowsArray);
                
                sendJsonResponse(exchange, 200, response);
                
            } catch (SQLException e) {
                LOGGER.error("Failed to execute query", e);
                sendError(exchange, "Failed to execute query: " + e.getMessage());
            }
        }
    }
    
    /**
     * GET /api/database/export?database=id&table=tableName&format=csv
     * Export table data
     */
    private void handleExport(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String databaseId = params.get("database");
        String tableName = params.get("table");
        String format = params.getOrDefault("format", "csv");
        
        if (databaseId == null || databaseId.isEmpty()) {
            sendBadRequest(exchange, "Missing 'database' parameter");
            return;
        }
        if (tableName == null || tableName.isEmpty()) {
            sendBadRequest(exchange, "Missing 'table' parameter");
            return;
        }
        
        try {
            if ("csv".equalsIgnoreCase(format)) {
                String csv = DatabaseManager.getInstance().exportTableAsCSV(databaseId, tableName);
                
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                exchange.getResponseHeaders().set("Content-Disposition", 
                    "attachment; filename=\"" + tableName + ".csv\"");
                
                byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else if ("json".equalsIgnoreCase(format)) {
                // Export as JSON
                DatabaseManager.QueryResult result = 
                    DatabaseManager.getInstance().executeQuery(databaseId, 
                        "SELECT * FROM \"" + tableName + "\"", 1, 10000);
                
                JsonObject exportData = new JsonObject();
                exportData.addProperty("table", tableName);
                exportData.addProperty("rowCount", result.getTotalRows());
                
                JsonArray columnsArray = new JsonArray();
                for (String column : result.getColumns()) {
                    columnsArray.add(column);
                }
                exportData.add("columns", columnsArray);
                
                JsonArray rowsArray = new JsonArray();
                for (Map<String, Object> row : result.getRows()) {
                    JsonObject rowObj = new JsonObject();
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        Object value = entry.getValue();
                        if (value == null) {
                            rowObj.add(entry.getKey(), null);
                        } else if (value instanceof Number) {
                            rowObj.addProperty(entry.getKey(), (Number) value);
                        } else {
                            rowObj.addProperty(entry.getKey(), value.toString());
                        }
                    }
                    rowsArray.add(rowObj);
                }
                exportData.add("rows", rowsArray);
                
                String json = gson.toJson(exportData);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.getResponseHeaders().set("Content-Disposition", 
                    "attachment; filename=\"" + tableName + ".json\"");
                
                exchange.sendResponseHeaders(200, bytes.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                sendBadRequest(exchange, "Invalid format. Use 'csv' or 'json'");
            }
            
        } catch (SQLException e) {
            LOGGER.error("Failed to export table", e);
            sendError(exchange, "Failed to export table: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/database/stats?database=id
     * Get database statistics
     */
    private void handleStats(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String databaseId = params.get("database");
        
        if (databaseId == null || databaseId.isEmpty()) {
            sendBadRequest(exchange, "Missing 'database' parameter");
            return;
        }
        
        try {
            Map<String, Object> stats = DatabaseManager.getInstance().getDatabaseStats(databaseId);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("database", databaseId);
            
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    response.addProperty(entry.getKey(), (Number) value);
                } else {
                    response.addProperty(entry.getKey(), value.toString());
                }
            }
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (SQLException e) {
            LOGGER.error("Failed to get stats", e);
            sendError(exchange, "Failed to get stats: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/database/refresh
     * Refresh database discovery
     */
    private void handleRefresh(HttpExchange exchange) throws IOException {
        DatabaseManager.getInstance().discoverDatabases();
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Database discovery refreshed");
        response.addProperty("count", DatabaseManager.getInstance().getDatabases().size());
        response.addProperty("timestamp", Instant.now().toString());
        
        sendJsonResponse(exchange, 200, response);
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
    
    /**
     * Parse query parameters from URL
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                try {
                    String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    LOGGER.warn("Failed to decode parameter: {}", param);
                }
            }
        }
        
        return params;
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
        String jsonResponse = gson.toJson(response);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", "Method not allowed");
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 405, response);
    }
    
    private void sendNotFound(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", "Resource not found");
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 404, response);
    }
    
    private void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 400, response);
    }
    
    private void sendError(HttpExchange exchange, String message) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        response.addProperty("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, 500, response);
    }
}
