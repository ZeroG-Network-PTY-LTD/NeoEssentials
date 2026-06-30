package com.zerog.neoessentials.webdashboard.api;

import com.google.gson.JsonObject;

/**
 * Standard API response format
 * Ensures consistency across all API endpoints
 */
public class APIResponse {
    private final boolean success;
    private final String message;
    private final JsonObject data;
    private final int statusCode;
    private final long timestamp;
    
    private APIResponse(boolean success, String message, JsonObject data, int statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Create a successful response
     */
    public static APIResponse success(JsonObject data) {
        return new APIResponse(true, "Success", data, 200);
    }
    
    /**
     * Create a successful response with custom message
     */
    public static APIResponse success(String message, JsonObject data) {
        return new APIResponse(true, message, data, 200);
    }
    
    /**
     * Create an error response
     */
    public static APIResponse error(String message, int statusCode) {
        return new APIResponse(false, message, null, statusCode);
    }
    
    /**
     * Create a 400 Bad Request error
     */
    public static APIResponse badRequest(String message) {
        return error(message, 400);
    }
    
    /**
     * Create a 401 Unauthorized error
     */
    public static APIResponse unauthorized(String message) {
        return error(message, 401);
    }
    
    /**
     * Create a 403 Forbidden error
     */
    public static APIResponse forbidden(String message) {
        return error(message, 403);
    }
    
    /**
     * Create a 404 Not Found error
     */
    public static APIResponse notFound(String message) {
        return error(message, 404);
    }
    
    /**
     * Create a 500 Internal Server Error
     */
    public static APIResponse serverError(String message) {
        return error(message, 500);
    }
    
    /**
     * Convert to JSON string
     */
    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message);
        json.addProperty("timestamp", timestamp);
        json.addProperty("statusCode", statusCode);
        
        if (data != null) {
            json.add("data", data);
        }
        
        return json.toString();
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public JsonObject getData() { return data; }
    public int getStatusCode() { return statusCode; }
    public long getTimestamp() { return timestamp; }
}
