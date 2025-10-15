package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User account for dashboard authentication
 */
public class User {
    private final String id;
    private String username;
    private String passwordHash;
    private String email;
    private Role role;
    private boolean enabled;
    private long createdAt;
    private long lastLoginAt;
    private String lastLoginIp;
    private int failedLoginAttempts;
    private long lockoutUntil;
    private Set<String> permissions;
    private boolean requiresPasswordChange;
    private boolean isTempPassword;
    
    public User(String username, String passwordHash) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Role.VIEWER;
        this.enabled = true;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = 0;
        this.lastLoginIp = "";
        this.failedLoginAttempts = 0;
        this.lockoutUntil = 0;
        this.permissions = new HashSet<>();
        this.requiresPasswordChange = false;
        this.isTempPassword = false;
    }
    
    public User(String id, String username, String passwordHash, String email, Role role, 
                boolean enabled, long createdAt, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.lastLoginAt = 0;
        this.lastLoginIp = "";
        this.failedLoginAttempts = 0;
        this.lockoutUntil = 0;
        this.permissions = permissions != null ? permissions : new HashSet<>();
        this.requiresPasswordChange = false;
        this.isTempPassword = false;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getCreatedAt() { return createdAt; }
    public long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(long lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int attempts) { this.failedLoginAttempts = attempts; }
    public long getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(long lockoutUntil) { this.lockoutUntil = lockoutUntil; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    public boolean requiresPasswordChange() { return requiresPasswordChange; }
    public void setRequiresPasswordChange(boolean requiresPasswordChange) { this.requiresPasswordChange = requiresPasswordChange; }
    public boolean isTempPassword() { return isTempPassword; }
    public void setTempPassword(boolean tempPassword) { this.isTempPassword = tempPassword; }
    
    /**
     * Check if user account is locked due to failed login attempts
     */
    public boolean isLockedOut() {
        return lockoutUntil > System.currentTimeMillis();
    }
    
    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(String permission) {
        // Admins have all permissions
        if (role == Role.ADMIN) {
            return true;
        }
        return permissions.contains(permission);
    }
    
    /**
     * Add permission to user
     */
    public void addPermission(String permission) {
        permissions.add(permission);
    }
    
    /**
     * Remove permission from user
     */
    public void removePermission(String permission) {
        permissions.remove(permission);
    }
    
    /**
     * Convert user to JSON (without password hash)
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("username", username);
        json.addProperty("email", email != null ? email : "");
        json.addProperty("role", role.name());
        json.addProperty("enabled", enabled);
        json.addProperty("createdAt", createdAt);
        json.addProperty("lastLoginAt", lastLoginAt);
        json.addProperty("lastLoginIp", lastLoginIp);
        json.addProperty("isLockedOut", isLockedOut());
        json.addProperty("requiresPasswordChange", requiresPasswordChange);
        json.addProperty("isTempPassword", isTempPassword);
        
        // Add permissions array
        com.google.gson.JsonArray permsArray = new com.google.gson.JsonArray();
        permissions.forEach(permsArray::add);
        json.add("permissions", permsArray);
        
        return json;
    }
    
    /**
     * User roles with hierarchical permissions
     */
    public enum Role {
        ADMIN,      // Full access to all features
        MODERATOR,  // Can manage players, view most data, limited configuration
        OPERATOR,   // Can execute commands, view data, no configuration
        VIEWER      // Read-only access to dashboard
    }
}
