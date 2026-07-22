package com.zerog.neoessentials.webdashboard.security;

import java.util.*;

/**
 * Represents a dashboard user with authentication and permission data
 */
public class User {
    private final String id;
    private final String username;
    private String passwordHash;
    private String email;
    private Role role;
    private boolean enabled;
    private final long createdAt;
    private long lastLoginAt;
    private String lastLoginIp;
    private int failedLoginAttempts;
    private long lockoutUntil;
    private boolean requiresPasswordChange;
    private boolean isTempPassword;
    /** True if {@link #role} was last set by the permission-driven role-sync task rather than a
     *  manual admin action — lets that task tell "should I downgrade this" apart from "an admin
     *  deliberately chose this role, leave it alone" without a separate lookup table. */
    private boolean roleSyncManaged;
    /** The Minecraft account this dashboard user has linked (via /linkaccount or the mod's
     *  existing in-game registration flow), or null if none. Both are always set/cleared
     *  together — see {@link MinecraftAccountLinkManager}. */
    private String mcUuid;
    private String mcUsername;
    private final Set<String> permissions;

    public enum Role {
        ADMIN,
        OPERATOR,
        MODERATOR,
        VIEWER
    }

    // Constructor for new user
    public User(String username, String passwordHash) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Role.VIEWER;
        this.enabled = true;
        this.createdAt = System.currentTimeMillis();
        this.permissions = new HashSet<>();
        this.failedLoginAttempts = 0;
        this.lockoutUntil = 0;
        this.requiresPasswordChange = false;
        this.isTempPassword = false;
    }

    // Constructor for loaded user
    public User(String id, String username, String passwordHash, String email, Role role,
                boolean enabled, long createdAt, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
        this.failedLoginAttempts = 0;
        this.lockoutUntil = 0;
        this.requiresPasswordChange = false;
        this.isTempPassword = false;
    }

    public boolean isLockedOut() {
        return System.currentTimeMillis() < lockoutUntil;
    }

    public boolean hasPermission(String permission) {
        if (role == Role.ADMIN) {
            return true;
        }
        return permissions.contains(permission) || permissions.contains("*");
    }

    public void addPermission(String permission) {
        permissions.add(permission);
    }

    public void removePermission(String permission) {
        permissions.remove(permission);
    }
    
    public com.google.gson.JsonObject toJson() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("id", id);
        json.addProperty("username", username);
        json.addProperty("email", email);
        json.addProperty("role", role.name());
        json.addProperty("enabled", enabled);
        json.addProperty("createdAt", createdAt);
        json.addProperty("lastLoginAt", lastLoginAt);
        json.addProperty("lastLoginIp", lastLoginIp);
        json.addProperty("failedLoginAttempts", failedLoginAttempts);
        json.addProperty("lockoutUntil", lockoutUntil);
        json.addProperty("requiresPasswordChange", requiresPasswordChange);
        json.addProperty("isTempPassword", isTempPassword);
        json.addProperty("mcUuid", mcUuid);
        json.addProperty("mcUsername", mcUsername);

        com.google.gson.JsonArray permsArray = new com.google.gson.JsonArray();
        for (String perm : permissions) {
            permsArray.add(perm);
        }
        json.add("permissions", permsArray);
        
        return json;
    }

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public long getCreatedAt() { return createdAt; }
    public long getLastLoginAt() { return lastLoginAt; }
    public String getLastLoginIp() { return lastLoginIp; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public long getLockoutUntil() { return lockoutUntil; }
    public boolean requiresPasswordChange() { return requiresPasswordChange; }
    public boolean isTempPassword() { return isTempPassword; }
    public boolean isRoleSyncManaged() { return roleSyncManaged; }
    public String getMcUuid() { return mcUuid; }
    public String getMcUsername() { return mcUsername; }
    public Set<String> getPermissions() { return new HashSet<>(permissions); }

    // Setters
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setLastLoginAt(long lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public void setLockoutUntil(long lockoutUntil) { this.lockoutUntil = lockoutUntil; }
    public void setRequiresPasswordChange(boolean requiresPasswordChange) { this.requiresPasswordChange = requiresPasswordChange; }
    public void setTempPassword(boolean isTempPassword) { this.isTempPassword = isTempPassword; }
    public void setRoleSyncManaged(boolean roleSyncManaged) { this.roleSyncManaged = roleSyncManaged; }
    public void setMcUuid(String mcUuid) { this.mcUuid = mcUuid; }
    public void setMcUsername(String mcUsername) { this.mcUsername = mcUsername; }
}
