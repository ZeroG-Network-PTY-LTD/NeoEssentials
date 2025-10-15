package com.zerog.neoessentials.webdashboard.filters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.zerog.neoessentials.webdashboard.security.AuthenticationManager;
import com.zerog.neoessentials.webdashboard.security.Session;
import com.zerog.neoessentials.webdashboard.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Authentication filter for HTTP endpoints
 * Validates session tokens and enforces role-based access control
 */
public class AuthenticationFilter extends Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final Gson GSON = new Gson();
    
    // Public endpoints that don't require authentication
    private static final Set<String> PUBLIC_ENDPOINTS = new HashSet<>();
    static {
        PUBLIC_ENDPOINTS.add("/");
        PUBLIC_ENDPOINTS.add("/login.html");
        PUBLIC_ENDPOINTS.add("/index.html");
        PUBLIC_ENDPOINTS.add("/space-dashboard.css");
        PUBLIC_ENDPOINTS.add("/space-dashboard.js");
        PUBLIC_ENDPOINTS.add("/space-theme.css");
        PUBLIC_ENDPOINTS.add("/space-glass.css");
        PUBLIC_ENDPOINTS.add("/orbitron.css");
        PUBLIC_ENDPOINTS.add("/favicon.ico");
        PUBLIC_ENDPOINTS.add("/api/auth/login");
    }
    
    // Endpoint permission requirements
    private static final Set<String> ADMIN_ONLY = new HashSet<>();
    static {
        ADMIN_ONLY.add("/api/auth/users");
        ADMIN_ONLY.add("/api/auth/sessions");
        ADMIN_ONLY.add("/api/commands/execute");
        ADMIN_ONLY.add("/api/files/write");
        ADMIN_ONLY.add("/api/files/create");
        ADMIN_ONLY.add("/api/files/delete");
    }
    
    private static final Set<String> OPERATOR_ONLY = new HashSet<>();
    static {
        OPERATOR_ONLY.add("/api/server/stop");
        OPERATOR_ONLY.add("/api/server/restart");
        OPERATOR_ONLY.add("/api/server/config");
    }
    
    @Override
    public String description() {
        return "Authentication Filter - Validates session tokens and enforces RBAC";
    }
    
    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        // Allow OPTIONS preflight for CORS
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        // Check if endpoint is public
        if (isPublicEndpoint(path)) {
            chain.doFilter(exchange);
            return;
        }
        
        // Extract session token
        String sessionId = extractSessionId(exchange);
        if (sessionId == null) {
            sendUnauthorized(exchange, "Authentication required");
            return;
        }
        
        // Validate session
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        Session session = authManager.validateSession(sessionId);
        
        if (session == null) {
            sendUnauthorized(exchange, "Invalid or expired session");
            return;
        }
        
        // Check role-based permissions
        if (!checkPermissions(path, method, session)) {
            sendForbidden(exchange, "Insufficient permissions");
            return;
        }
        
        // Store session in exchange attributes for handlers to use
        exchange.setAttribute("session", session);
        exchange.setAttribute("userId", session.getUserId());
        exchange.setAttribute("username", session.getUsername());
        exchange.setAttribute("role", session.getRole().name());
        
        // Continue to next filter or handler
        chain.doFilter(exchange);
    }
    
    /**
     * Check if endpoint is public (doesn't require authentication)
     */
    private boolean isPublicEndpoint(String path) {
        // Exact match
        if (PUBLIC_ENDPOINTS.contains(path)) {
            return true;
        }
        
        // Check for static assets
        if (path.startsWith("/assets/") || path.startsWith("/css/") || 
            path.startsWith("/js/") || path.startsWith("/images/")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if session has required permissions for endpoint
     */
    private boolean checkPermissions(String path, String method, Session session) {
        User.Role role = session.getRole();
        
        // ADMIN has access to everything
        if (role == User.Role.ADMIN) {
            return true;
        }
        
        // Check admin-only endpoints
        if (requiresAdminAccess(path, method)) {
            return false;
        }
        
        // Check operator-only endpoints
        if (requiresOperatorAccess(path, method)) {
            return role == User.Role.OPERATOR || role == User.Role.MODERATOR;
        }
        
        // MODERATOR has access to player management
        if (role == User.Role.MODERATOR) {
            if (path.startsWith("/api/players") || path.startsWith("/api/chat")) {
                return true;
            }
        }
        
        // OPERATOR has access to commands and server control
        if (role == User.Role.OPERATOR) {
            if (path.startsWith("/api/commands") || path.startsWith("/api/server")) {
                return true;
            }
        }
        
        // VIEWER has read-only access
        if (role == User.Role.VIEWER) {
            // Only allow GET requests for viewers
            if ("GET".equals(method)) {
                // Deny access to sensitive endpoints even for GET
                if (path.startsWith("/api/auth/users") || path.startsWith("/api/auth/sessions")) {
                    return false;
                }
                return true;
            }
            return false;
        }
        
        // Default deny
        return false;
    }
    
    /**
     * Check if endpoint requires admin access
     */
    private boolean requiresAdminAccess(String path, String method) {
        // Check exact matches
        for (String adminPath : ADMIN_ONLY) {
            if (path.equals(adminPath) || path.startsWith(adminPath + "/")) {
                return true;
            }
        }
        
        // User management endpoints (except GET for own profile)
        if (path.startsWith("/api/auth/users") && !"GET".equals(method)) {
            return true;
        }
        
        // File modifications
        if (path.startsWith("/api/files") && 
            ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if endpoint requires operator access
     */
    private boolean requiresOperatorAccess(String path, String method) {
        for (String opPath : OPERATOR_ONLY) {
            if (path.equals(opPath) || path.startsWith(opPath + "/")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract session ID from Authorization header
     */
    private String extractSessionId(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    /**
     * Send 401 Unauthorized response
     */
    private void sendUnauthorized(HttpExchange exchange, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("code", 401);
        error.addProperty("timestamp", System.currentTimeMillis());
        
        byte[] response = GSON.toJson(error).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(401, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    /**
     * Send 403 Forbidden response
     */
    private void sendForbidden(HttpExchange exchange, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("code", 403);
        error.addProperty("timestamp", System.currentTimeMillis());
        
        byte[] response = GSON.toJson(error).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(403, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
