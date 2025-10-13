package com.zerog.neoessentials.webdashboard;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for the NeoEssentials Web Dashboard
 * Serves static files and provides RESTful API endpoints
 */
public class WebDashboardServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebDashboardServer.class);
    private static WebDashboardServer INSTANCE;
    
    private HttpServer server;
    private final int port;
    private final Path webRoot;
    private boolean running = false;
    
    private WebDashboardServer(int port) {
        this.port = port;
        this.webRoot = Paths.get("data", "webdashboard");
    }
    
    public static WebDashboardServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WebDashboardServer(8080); // Default port
        }
        return INSTANCE;
    }
    
    public static WebDashboardServer getInstance(int port) {
        if (INSTANCE == null) {
            INSTANCE = new WebDashboardServer(port);
        }
        return INSTANCE;
    }
    
    /**
     * Start the web dashboard server
     */
    public void start() {
        if (running) {
            LOGGER.warn("Web dashboard server is already running");
            return;
        }
        
        try {
            // Ensure web root directory exists
            if (!Files.exists(webRoot)) {
                Files.createDirectories(webRoot);
                LOGGER.info("Created web dashboard directory at: {}", webRoot.toAbsolutePath());
            }
            
            // Create HTTP server
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(4));
            
            // Register API endpoints
            registerApiEndpoints();
            
            // Register static file handler (must be last)
            server.createContext("/", new StaticFileHandler(webRoot));
            
            // Start server
            server.start();
            running = true;
            
            LOGGER.info("╔════════════════════════════════════════════════════════╗");
            LOGGER.info("║   NeoEssentials Web Dashboard Started                 ║");
            LOGGER.info("║   Access at: http://localhost:{}                    ║", port);
            LOGGER.info("╚════════════════════════════════════════════════════════╝");
            
        } catch (IOException e) {
            LOGGER.error("Failed to start web dashboard server", e);
        }
    }
    
    /**
     * Stop the web dashboard server
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        if (server != null) {
            server.stop(0);
            running = false;
            LOGGER.info("Web dashboard server stopped");
        }
    }
    
    /**
     * Register all API endpoints
     */
    private void registerApiEndpoints() {
        // Player stats endpoint
        server.createContext("/api/players", new com.zerog.neoessentials.webdashboard.handlers.PlayersHandler());
        
        // Server stats endpoint
        server.createContext("/api/server", new com.zerog.neoessentials.webdashboard.handlers.ServerStatsHandler());
        
        // Logs endpoint
        server.createContext("/api/logs", new com.zerog.neoessentials.webdashboard.handlers.LogsHandler());
        
        // Config endpoints
        server.createContext("/api/config", new com.zerog.neoessentials.webdashboard.handlers.ConfigHandler());
        
        LOGGER.info("Registered API endpoints: /api/players, /api/server, /api/logs, /api/config");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * Static file handler for serving HTML, CSS, JS files
     */
    private static class StaticFileHandler implements HttpHandler {
        private final Path webRoot;
        
        public StaticFileHandler(Path webRoot) {
            this.webRoot = webRoot;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Default to index.html
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }
            
            Path filePath = webRoot.resolve(path.substring(1)); // Remove leading slash
            
            // Security check - prevent directory traversal
            if (!filePath.normalize().startsWith(webRoot.normalize())) {
                sendResponse(exchange, 403, "Forbidden");
                return;
            }
            
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(filePath.toString());
                byte[] content = Files.readAllBytes(filePath);
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } else {
                sendResponse(exchange, 404, "File not found: " + path);
            }
        }
        
        private String getContentType(String filename) {
            if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
            if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            return "text/plain; charset=UTF-8";
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            byte[] response = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}
