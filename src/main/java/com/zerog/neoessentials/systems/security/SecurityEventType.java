package com.zerog.neoessentials.systems.security;

/**
 * Security event type enumeration
 */
public enum SecurityEventType {
    LOGIN_ATTEMPT("Login Attempt"),
    PERMISSION_DENIED("Permission Denied"),
    SUSPICIOUS_ACTIVITY("Suspicious Activity"),
    COMMAND_EXECUTION("Command Execution"),
    DATA_ACCESS("Data Access"),
    SYSTEM_BREACH("System Breach"),
    AUTHENTICATION_FAILURE("Authentication Failure"),
    UNAUTHORIZED_ACCESS("Unauthorized Access");
    
    private final String displayName;
    
    SecurityEventType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
