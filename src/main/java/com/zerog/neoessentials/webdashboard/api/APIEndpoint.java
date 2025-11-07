package com.zerog.neoessentials.webdashboard.api;

/**
 * Base interface for all API endpoint handlers
 * Each endpoint should implement this interface to handle HTTP requests
 */
public interface APIEndpoint {
    
    /**
     * Get the endpoint path (e.g., "/api/server/status")
     */
    String getPath();
    
    /**
     * Get the HTTP method(s) this endpoint supports
     */
    String[] getSupportedMethods();
    
    /**
     * Check if this endpoint requires authentication
     */
    boolean requiresAuthentication();
    
    /**
     * Get required permission to access this endpoint
     * Returns null if no specific permission is required (beyond authentication)
     */
    String getRequiredPermission();
}
