package com.zerog.neoessentials.security;

/**
 * Security event types for categorizing security activities
 */
public enum SecurityEventType {
    IP_BLOCKED("IP Blocked"),
    IP_UNBLOCKED("IP Unblocked"),
    PLAYER_LOGIN("Player Login"),
    PLAYER_LOGOUT("Player Logout"),
    SUSPICIOUS_ACTIVITY("Suspicious Activity"),
    AUTHENTICATION_FAILURE("Authentication Failure"),
    PERMISSION_VIOLATION("Permission Violation"),
    COMMAND_BLOCKED("Command Blocked"),
    RATE_LIMIT_EXCEEDED("Rate Limit Exceeded"),
    SECURITY_SCAN("Security Scan"),
    THREAT_DETECTED("Threat Detected"),
    SYSTEM_EVENT("System Event"),
    COMMAND_EXECUTED("Command Executed"),
    SYSTEM_STARTUP("System Startup");

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
