package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Configuration for Discord authentication integration
 * Manages role mapping between Discord and Dashboard roles
 * 
 * Note: This config is now managed by ConfigManager for automatic version tracking
 * and template extraction like other config files.
 */
public class DiscordAuthConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordAuthConfig.class);
    @SuppressWarnings("unused") // Reserved for future JSON serialization features
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @SuppressWarnings("unused") // Reserved for future file-based config loading
    private static final Path CONFIG_FILE = Paths.get("config", "neoessentials", "discord_auth.json");
    public static final String CONFIG_NAME = "discord_auth.json";
    
    private boolean enabled;
    private boolean requireLinkedAccount;
    private boolean allowAutoRegistration;
    private User.Role defaultRole;
    private Map<String, String> roleMapping;
    private Map<String, List<String>> roleHierarchy;
    private List<String> whitelistedRoles;
    private List<String> blacklistedUsers;
    private long sessionDuration;
    
    // Permission sync settings
    private boolean permissionSyncEnabled;
    private boolean syncOnJoin;
    private Map<String, List<String>> permissionMappings; // Discord Role ID -> List of Minecraft permissions
    
    private DiscordAuthConfig() {
        // Set defaults
        this.enabled = true;
        this.requireLinkedAccount = true;
        this.allowAutoRegistration = true;
        this.defaultRole = User.Role.VIEWER;
        this.roleMapping = new HashMap<>();
        this.roleHierarchy = new HashMap<>();
        this.whitelistedRoles = new ArrayList<>();
        this.blacklistedUsers = new ArrayList<>();
        this.sessionDuration = 86400000; // 24 hours
        this.permissionSyncEnabled = true;
        this.syncOnJoin = true;
        this.permissionMappings = new HashMap<>();
    }
    
    /**
     * Load configuration from file using ConfigManager for version tracking
     */
    public static DiscordAuthConfig load() {
        DiscordAuthConfig config = new DiscordAuthConfig();
        
        try {
            // Use ConfigManager to get the config with automatic version tracking
            ConfigManager configManager = ConfigManager.getInstance();
            JsonObject root = configManager.getConfig(CONFIG_NAME);
            
            if (root == null) {
                LOGGER.warn("Failed to load Discord auth config from ConfigManager, using defaults");
                return config;
            }
            
            // Parse configuration
            config.enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
            config.requireLinkedAccount = !root.has("requireLinkedAccount") || root.get("requireLinkedAccount").getAsBoolean();
            config.allowAutoRegistration = !root.has("allowAutoRegistration") || root.get("allowAutoRegistration").getAsBoolean();
            
            // Parse default role
            if (root.has("defaultRole")) {
                try {
                    config.defaultRole = User.Role.valueOf(root.get("defaultRole").getAsString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid defaultRole in config, using VIEWER: {}", root.get("defaultRole").getAsString());
                    config.defaultRole = User.Role.VIEWER;
                }
            }
            
            // Parse role mapping (using role IDs, not names)
            if (root.has("roleMapping")) {
                JsonObject mappingObj = root.getAsJsonObject("roleMapping");
                mappingObj.entrySet().forEach(entry -> {
                    // Skip comment fields
                    if (!entry.getKey().startsWith("_")) {
                        // Store role ID as-is (no lowercase conversion for IDs)
                        config.roleMapping.put(entry.getKey(), entry.getValue().getAsString().toUpperCase());
                    }
                });
            }
            
            // Parse role hierarchy (using role IDs, not names)
            if (root.has("roleHierarchy")) {
                JsonObject hierarchyObj = root.getAsJsonObject("roleHierarchy");
                hierarchyObj.entrySet().forEach(entry -> {
                    // Skip comment fields
                    if (!entry.getKey().startsWith("_")) {
                        List<String> roles = new ArrayList<>();
                        entry.getValue().getAsJsonArray().forEach(elem -> roles.add(elem.getAsString()));
                        // Store role ID as-is (no lowercase conversion for IDs)
                        config.roleHierarchy.put(entry.getKey(), roles);
                    }
                });
            }
            
            // Parse whitelist (using role IDs, not names)
            if (root.has("whitelistedRoles")) {
                root.getAsJsonArray("whitelistedRoles").forEach(elem -> 
                    // Store role ID as-is (no lowercase conversion for IDs)
                    config.whitelistedRoles.add(elem.getAsString())
                );
            }
            
            // Parse blacklist
            if (root.has("blacklistedUsers")) {
                root.getAsJsonArray("blacklistedUsers").forEach(elem -> 
                    config.blacklistedUsers.add(elem.getAsString())
                );
            }
            
            // Parse session duration
            if (root.has("sessionDuration")) {
                config.sessionDuration = root.get("sessionDuration").getAsLong();
            }
            
            // Parse permission sync settings
            if (root.has("permissionSync")) {
                JsonObject syncObj = root.getAsJsonObject("permissionSync");
                
                if (syncObj.has("enabled")) {
                    config.permissionSyncEnabled = syncObj.get("enabled").getAsBoolean();
                }
                
                if (syncObj.has("syncOnJoin")) {
                    config.syncOnJoin = syncObj.get("syncOnJoin").getAsBoolean();
                }
                
                // Parse permission mappings: Discord Role ID -> List of Minecraft permissions
                if (syncObj.has("permissionMappings")) {
                    JsonObject mappingsObj = syncObj.getAsJsonObject("permissionMappings");
                    mappingsObj.entrySet().forEach(entry -> {
                        // Skip comment fields
                        if (!entry.getKey().startsWith("_")) {
                            List<String> permissions = new ArrayList<>();
                            entry.getValue().getAsJsonArray().forEach(elem -> 
                                permissions.add(elem.getAsString())
                            );
                            config.permissionMappings.put(entry.getKey(), permissions);
                        }
                    });
                }
            }
            
            LOGGER.info("Discord auth config loaded successfully. Enabled: {}, Permission Sync: {}", 
                config.enabled, config.permissionSyncEnabled);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load Discord auth config: {}", e.getMessage(), e);
        }
        
        return config;
    }
    
    /**
     * Map Discord role ID to Dashboard role
     * 
     * @param discordRoleId Discord role ID (not name)
     * @return Mapped dashboard role or default role
     */
    public User.Role mapDiscordRole(String discordRoleId) {
        if (discordRoleId == null) {
            return defaultRole;
        }
        
        // Look up by role ID directly (no case conversion needed for numeric IDs)
        String mapped = roleMapping.get(discordRoleId);
        if (mapped == null) {
            return defaultRole;
        }
        
        try {
            return User.Role.valueOf(mapped);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid role mapping for Discord role ID '{}': {}", discordRoleId, mapped);
            return defaultRole;
        }
    }
    
    /**
     * Get highest Dashboard role from multiple Discord role IDs
     * 
     * @param discordRoleIds List of Discord role IDs (not names)
     * @return Highest dashboard role found
     */
    public User.Role getHighestRole(List<String> discordRoleIds) {
        if (discordRoleIds == null || discordRoleIds.isEmpty()) {
            return defaultRole;
        }
        
        User.Role highestRole = defaultRole;
        
        for (String discordRoleId : discordRoleIds) {
            User.Role mappedRole = mapDiscordRole(discordRoleId);
            if (mappedRole.ordinal() > highestRole.ordinal()) {
                highestRole = mappedRole;
            }
        }
        
        return highestRole;
    }
    
    /**
     * Check if user has any whitelisted role ID (if whitelist is active)
     * 
     * @param discordRoleIds List of Discord role IDs (not names)
     * @return true if user passes whitelist check
     */
    public boolean passesWhitelist(List<String> discordRoleIds) {
        if (whitelistedRoles.isEmpty()) {
            return true; // No whitelist = everyone passes
        }
        
        if (discordRoleIds == null || discordRoleIds.isEmpty()) {
            return false;
        }
        
        // Check role IDs directly (no case conversion needed)
        return discordRoleIds.stream()
            .anyMatch(roleId -> whitelistedRoles.contains(roleId));
    }
    
    /**
     * Check if user is blacklisted
     */
    public boolean isBlacklisted(String discordId) {
        return discordId != null && blacklistedUsers.contains(discordId);
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean requiresLinkedAccount() { return requireLinkedAccount; }
    public boolean allowsAutoRegistration() { return allowAutoRegistration; }
    public User.Role getDefaultRole() { return defaultRole; }
    public Map<String, String> getRoleMapping() { return new HashMap<>(roleMapping); }
    public Map<String, List<String>> getRoleHierarchy() { return new HashMap<>(roleHierarchy); }
    public List<String> getWhitelistedRoles() { return new ArrayList<>(whitelistedRoles); }
    public List<String> getBlacklistedUsers() { return new ArrayList<>(blacklistedUsers); }
    public long getSessionDuration() { return sessionDuration; }
    
    // Permission sync getters
    public boolean isPermissionSyncEnabled() { return permissionSyncEnabled; }
    public boolean isSyncOnJoin() { return syncOnJoin; }
    public Map<String, List<String>> getPermissionMappings() { return new HashMap<>(permissionMappings); }
    
    // Setters
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRequireLinkedAccount(boolean require) { this.requireLinkedAccount = require; }
    public void setAllowAutoRegistration(boolean allow) { this.allowAutoRegistration = allow; }
    public void setDefaultRole(User.Role role) { this.defaultRole = role; }
    public void setSessionDuration(long duration) { this.sessionDuration = duration; }
    public void setPermissionSyncEnabled(boolean enabled) { this.permissionSyncEnabled = enabled; }
    public void setSyncOnJoin(boolean syncOnJoin) { this.syncOnJoin = syncOnJoin; }
}
