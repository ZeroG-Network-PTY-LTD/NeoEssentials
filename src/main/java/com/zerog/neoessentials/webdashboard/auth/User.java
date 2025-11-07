package com.zerog.neoessentials.webdashboard.auth;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Represents a user with dashboard access
 * Users can authenticate via:
 * - Username/Password (with temporary password change requirement)
 * - Discord OAuth (with role-based permissions)
 */
public class User {
    private final UUID uuid;
    private final String username;
    private String passwordHash;
    private boolean requirePasswordChange;
    private final Set<String> permissions;
    private final Set<String> roles;
    private String discordId;
    private long lastLogin;
    
    public User(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.permissions = new HashSet<>();
        this.roles = new HashSet<>();
        this.requirePasswordChange = false;
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("dashboard.*");
    }
    
    public void addPermission(String permission) {
        permissions.add(permission);
    }
    
    public void removePermission(String permission) {
        permissions.remove(permission);
    }
    
    public void addRole(String role) {
        roles.add(role);
    }
    
    public void removeRole(String role) {
        roles.remove(role);
    }
    
    // Getters and setters
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean requiresPasswordChange() { return requirePasswordChange; }
    public void setRequirePasswordChange(boolean require) { this.requirePasswordChange = require; }
    public Set<String> getPermissions() { return new HashSet<>(permissions); }
    public Set<String> getRoles() { return new HashSet<>(roles); }
    public String getDiscordId() { return discordId; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }
    public long getLastLogin() { return lastLogin; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }
}
