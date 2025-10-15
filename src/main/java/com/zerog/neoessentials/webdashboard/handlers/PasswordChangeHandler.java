package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.security.AuthenticationManager;
import com.zerog.neoessentials.webdashboard.security.Session;
import com.zerog.neoessentials.webdashboard.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handler for password change requests
 * Allows users with temporary passwords to set their permanent password
 */
public class PasswordChangeHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordChangeHandler.class);
    private static final Gson GSON = new Gson();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Only accept POST requests
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method not allowed");
            return;
        }
        
        try {
            // Get session from cookie
            String sessionId = getSessionIdFromCookie(exchange);
            if (sessionId == null) {
                sendErrorResponse(exchange, 401, "Not authenticated");
                return;
            }
            
            // Validate session
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            Session session = authManager.validateSession(sessionId);
            
            if (session == null) {
                sendErrorResponse(exchange, 401, "Invalid or expired session");
                return;
            }
            
            // Check if password change is required
            if (!session.requiresPasswordChange()) {
                sendErrorResponse(exchange, 400, "Password change not required");
                return;
            }
            
            // Parse request body
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
            
            String newPassword = request.has("newPassword") ? request.get("newPassword").getAsString() : null;
            String confirmPassword = request.has("confirmPassword") ? request.get("confirmPassword").getAsString() : null;
            
            // Validate input
            if (newPassword == null || newPassword.isEmpty()) {
                sendErrorResponse(exchange, 400, "New password is required");
                return;
            }
            
            if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
                sendErrorResponse(exchange, 400, "Passwords do not match");
                return;
            }
            
            if (newPassword.length() < 8) {
                sendErrorResponse(exchange, 400, "Password must be at least 8 characters");
                return;
            }
            
            // Get user and update password
            User user = authManager.getUser(session.getUserId());
            if (user == null) {
                sendErrorResponse(exchange, 404, "User not found");
                return;
            }
            
            try {
                // Update password
                authManager.updatePassword(user.getId(), newPassword);
                
                // Clear password change flags
                user.setTempPassword(false);
                user.setRequiresPasswordChange(false);
                
                // Update session to remove password change requirement
                session.setRequiresPasswordChange(false);
                
                LOGGER.info("User {} successfully changed their password", user.getUsername());
                
                // Send success response
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Password changed successfully");
                
                sendJsonResponse(exchange, 200, response);
                
            } catch (IllegalArgumentException e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling password change request", e);
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Get session ID from cookie
     */
    private String getSessionIdFromCookie(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }
        
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && "sessionId".equals(parts[0])) {
                return parts[1];
            }
        }
        
        return null;
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject json) throws IOException {
        String response = GSON.toJson(json);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String error) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", error);
        sendJsonResponse(exchange, statusCode, response);
    }
}
