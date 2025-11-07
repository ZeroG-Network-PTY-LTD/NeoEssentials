package com.zerog.neoessentials.webdashboard.auth;

/**
 * Result of an authentication attempt
 */
public class AuthResult {
    private final boolean success;
    private final String token;
    private final String refreshToken;
    private final String message;
    private final AuthSession session;
    
    private AuthResult(boolean success, String token, String refreshToken, String message, AuthSession session) {
        this.success = success;
        this.token = token;
        this.refreshToken = refreshToken;
        this.message = message;
        this.session = session;
    }
    
    public static AuthResult success(String token, String refreshToken, AuthSession session) {
        return new AuthResult(true, token, refreshToken, "Authentication successful", session);
    }
    
    public static AuthResult failure(String message) {
        return new AuthResult(false, null, null, message, null);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getMessage() { return message; }
    public AuthSession getSession() { return session; }
}
