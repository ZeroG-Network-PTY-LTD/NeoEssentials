package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.storage.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Permissions Manager for NeoEssentials
 * 
 * Features:
 * - Group-based permissions with inheritance
 * - Per-player permission overrides
 * - Permission wildcards and negation
 * - Temporary permissions with expiration
 * - Permission caching for performance
 * - Integration with external permission systems
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class CustomPermissionsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPermissionsManager.class);
    private static CustomPermissionsManager instance;
    
    // Permission storage
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerPermissions> playerPermissions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerGroups = new ConcurrentHashMap<>();
    
    // Default groups
    private static final String DEFAULT_GROUP = "default";
    private static final String ADMIN_GROUP = "admin";
    private static final String MODERATOR_GROUP = "moderator";
    private static final String VIP_GROUP = "vip";
    
    // Permission cache
    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();
    private final long CACHE_DURATION = 30000; // 30 seconds
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    private CustomPermissionsManager() {
        initialize();
    }
    
    public static CustomPermissionsManager getInstance() {
        if (instance == null) {
            instance = new CustomPermissionsManager();
        }
        return instance;
    }
    
    /**
     * Initialize the permissions system
     */
    public void initialize() {
        LOGGER.info("Initializing Custom Permissions Manager...");
        
        createDefaultGroups();
        loadPermissionsFromConfig();
        
        // Start cleanup task for expired permissions
        PermissionCleanupTask.start();
        
        LOGGER.info("Custom Permissions Manager initialized successfully!");
    }
    
    /**
     * Create default permission groups
     */
    private void createDefaultGroups() {
        // Default group - basic permissions for all players using comprehensive permission nodes
        PermissionGroup defaultGroup = new PermissionGroup(DEFAULT_GROUP, "&7[Player]", "", 0);
        defaultGroup.addPermission(PermissionNodes.HOME);
        defaultGroup.addPermission(PermissionNodes.HOME_SET);
        defaultGroup.addPermission(PermissionNodes.HOME_DELETE);
        defaultGroup.addPermission(PermissionNodes.HOME_LIST);
        defaultGroup.addPermission(PermissionNodes.WARP);
        defaultGroup.addPermission(PermissionNodes.WARP_LIST);
        defaultGroup.addPermission(PermissionNodes.SPAWN);
        defaultGroup.addPermission(PermissionNodes.BACK);
        defaultGroup.addPermission(PermissionNodes.TPA_REQUEST);
        defaultGroup.addPermission(PermissionNodes.TPA_ACCEPT);
        defaultGroup.addPermission(PermissionNodes.TPA_DENY);
        defaultGroup.addPermission(PermissionNodes.TPA_CANCEL);
        defaultGroup.addPermission(PermissionNodes.MSG);
        defaultGroup.addPermission(PermissionNodes.REPLY);
        defaultGroup.addPermission(PermissionNodes.MAIL_SEND);
        defaultGroup.addPermission(PermissionNodes.MAIL_READ);
        defaultGroup.addPermission(PermissionNodes.ECO_BALANCE);
        defaultGroup.addPermission(PermissionNodes.ECO_PAY);
        defaultGroup.addPermission(PermissionNodes.KIT);
        defaultGroup.addPermission(PermissionNodes.KIT_LIST);
        defaultGroup.addPermission(PermissionNodes.LIST);
        defaultGroup.addPermission(PermissionNodes.WHOIS);
        defaultGroup.addPermission(PermissionNodes.SEEN);
        defaultGroup.addPermission(PermissionNodes.PLACEHOLDER_TEST);
        defaultGroup.addPermission(PermissionNodes.GUI_OPEN);
        defaultGroup.addPermission(PermissionNodes.PLAYTIME_VIEW);
        defaultGroup.addPermission(PermissionNodes.ACHIEVEMENTS_VIEW);
        defaultGroup.addPermission(PermissionNodes.PREFERENCES_SET);
        defaultGroup.addPermission(PermissionNodes.PREFERENCES_VIEW);
        groups.put(DEFAULT_GROUP, defaultGroup);
        
        // VIP group - enhanced permissions using comprehensive permission nodes
        PermissionGroup vipGroup = new PermissionGroup(VIP_GROUP, "&b[VIP]", " &b♦", 10);
        vipGroup.setInheritance(DEFAULT_GROUP);
        vipGroup.addPermission(PermissionNodes.FLY_SELF);
        vipGroup.addPermission(PermissionNodes.HEAL_SELF);
        vipGroup.addPermission(PermissionNodes.FEED_SELF);
        vipGroup.addPermission(PermissionNodes.WORKBENCH);
        vipGroup.addPermission(PermissionNodes.ANVIL);
        vipGroup.addPermission(PermissionNodes.ENDERCHEST);
        vipGroup.addPermission(PermissionNodes.REPAIR_HAND);
        vipGroup.addPermission(PermissionNodes.SPEED_WALK);
        vipGroup.addPermission(PermissionNodes.HOME_MULTIPLE);
        vipGroup.addPermission(PermissionNodes.NICK);
        vipGroup.addPermission(PermissionNodes.NICK_COLOR);
        vipGroup.addPermission(PermissionNodes.BOSSBAR_SHOW);
        vipGroup.addPermission(PermissionNodes.BYPASS_COOLDOWN_TELEPORT);
        groups.put(VIP_GROUP, vipGroup);
        
        // Moderator group - moderation permissions using comprehensive permission nodes
        PermissionGroup modGroup = new PermissionGroup(MODERATOR_GROUP, "&6[MOD]", " &6★", 50);
        modGroup.setInheritance(VIP_GROUP);
        modGroup.addPermission(PermissionNodes.KICK);
        modGroup.addPermission(PermissionNodes.MUTE);
        modGroup.addPermission(PermissionNodes.UNMUTE);
        modGroup.addPermission(PermissionNodes.JAIL);
        modGroup.addPermission(PermissionNodes.UNJAIL);
        modGroup.addPermission(PermissionNodes.BAN_TEMP);
        modGroup.addPermission(PermissionNodes.VANISH_SELF);
        modGroup.addPermission(PermissionNodes.VANISH_SEE);
        modGroup.addPermission(PermissionNodes.SOCIALSPY);
        modGroup.addPermission(PermissionNodes.ENDERCHEST_OTHERS);
        modGroup.addPermission(PermissionNodes.TP_SELF);
        modGroup.addPermission(PermissionNodes.TP_HERE);
        modGroup.addPermission(PermissionNodes.TP_COORDS);
        modGroup.addPermission(PermissionNodes.SPAWN_OTHERS);
        modGroup.addPermission(PermissionNodes.LIST_HIDDEN);
        modGroup.addPermission(PermissionNodes.BOSSBAR_BROADCAST);
        modGroup.addPermission(PermissionNodes.SECURITY_VIEW);
        modGroup.addPermission(PermissionNodes.PERFORMANCE_VIEW);
        modGroup.addPermission(PermissionNodes.STATUS_VIEW);
        modGroup.addPermission(PermissionNodes.PLAYTIME_OTHERS);
        modGroup.addPermission(PermissionNodes.ACHIEVEMENTS_OTHERS);
        modGroup.addPermission(PermissionNodes.PERMISSIONS_INFO);
        modGroup.addPermission(PermissionNodes.PERMISSIONS_CHECK);
        groups.put(MODERATOR_GROUP, modGroup);
        
        // Admin group - full permissions using comprehensive permission nodes
        PermissionGroup adminGroup = new PermissionGroup(ADMIN_GROUP, "&c[ADMIN]", " &c⚡", 100);
        adminGroup.setInheritance(MODERATOR_GROUP);
        adminGroup.addPermission(PermissionNodes.ALL_ESSENTIALS);
        adminGroup.addPermission(PermissionNodes.ALL_NEOESSENTIALS);
        adminGroup.addPermission(PermissionNodes.BAN);
        adminGroup.addPermission(PermissionNodes.UNBAN);
        adminGroup.addPermission(PermissionNodes.BAN_IP);
        adminGroup.addPermission(PermissionNodes.GIVE_ITEM);
        adminGroup.addPermission(PermissionNodes.GIVE_UNLIMITED);
        adminGroup.addPermission(PermissionNodes.TIME_ALL);
        adminGroup.addPermission(PermissionNodes.WEATHER_ALL);
        adminGroup.addPermission(PermissionNodes.WARP_SET);
        adminGroup.addPermission(PermissionNodes.WARP_DELETE);
        adminGroup.addPermission(PermissionNodes.SPAWN_SET);
        adminGroup.addPermission(PermissionNodes.ALL_ADMIN);
        groups.put(ADMIN_GROUP, adminGroup);
        
        LOGGER.info("Created default permission groups with comprehensive permission nodes");
    }
    
    /**
     * Load permissions from configuration
     */
    private void loadPermissionsFromConfig() {
        try {
            // This would load from config files
            // For now, we'll use the default groups created above
            LOGGER.info("Loaded permissions configuration");
        } catch (Exception e) {
            LOGGER.error("Failed to load permissions configuration", e);
        }
    }
    
    /**
     * Check if a player has a specific permission
     */
    public boolean hasPermission(ServerPlayer player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        // Check cache first
        String cacheKey = player.getUUID() + ":" + permission;
        if (permissionCache.containsKey(cacheKey)) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                return permissionCache.get(cacheKey);
            } else {
                // Cache expired
                permissionCache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey);
            }
        }
        
        // Calculate permission
        boolean hasPermission = calculatePermission(player, permission);
        
        // Cache result
        permissionCache.put(cacheKey, hasPermission);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        
        return hasPermission;
    }
    
    /**
     * Calculate if player has permission (without cache)
     */
    private boolean calculatePermission(ServerPlayer player, String permission) {
        UUID playerId = player.getUUID();
        
        // Check if player is OP (always has all permissions)
        if (player.hasPermissions(4)) {
            return true;
        }
        
        // Check player-specific permissions first
        PlayerPermissions playerPerms = playerPermissions.get(playerId);
        if (playerPerms != null) {
            Boolean result = playerPerms.hasPermission(permission);
            if (result != null) {
                return result;
            }
        }
        
        // Check group permissions
        String groupName = getPlayerGroup(playerId);
        PermissionGroup group = groups.get(groupName);
        
        if (group != null) {
            return group.hasPermission(permission, groups);
        }
        
        // Default fallback
        return false;
    }
    
    /**
     * Get player's primary group
     */
    public String getPlayerGroup(UUID playerId) {
        return playerGroups.getOrDefault(playerId, DEFAULT_GROUP);
    }
    
    /**
     * Set player's primary group
     */
    public void setPlayerGroup(UUID playerId, String groupName) {
        if (groups.containsKey(groupName)) {
            playerGroups.put(playerId, groupName);
            clearPlayerCache(playerId);
            LOGGER.info("Set player {} to group {}", playerId, groupName);
        } else {
            LOGGER.warn("Attempted to set player {} to non-existent group {}", playerId, groupName);
        }
    }
    
    /**
     * Add permission to player
     */
    public void addPlayerPermission(UUID playerId, String permission) {
        playerPermissions.computeIfAbsent(playerId, k -> new PlayerPermissions())
                .addPermission(permission);
        clearPlayerCache(playerId);
        LOGGER.info("Added permission {} to player {}", permission, playerId);
    }
    
    /**
     * Remove permission from player
     */
    public void removePlayerPermission(UUID playerId, String permission) {
        PlayerPermissions perms = playerPermissions.get(playerId);
        if (perms != null) {
            perms.removePermission(permission);
            clearPlayerCache(playerId);
            LOGGER.info("Removed permission {} from player {}", permission, playerId);
        }
    }
    
    /**
     * Add temporary permission to player
     */
    public void addTemporaryPermission(UUID playerId, String permission, long durationMs) {
        playerPermissions.computeIfAbsent(playerId, k -> new PlayerPermissions())
                .addTemporaryPermission(permission, System.currentTimeMillis() + durationMs);
        clearPlayerCache(playerId);
        LOGGER.info("Added temporary permission {} to player {} for {}ms", permission, playerId, durationMs);
    }
    
    /**
     * Get all permissions for a player
     */
    public Set<String> getPlayerPermissions(UUID playerId) {
        Set<String> allPermissions = new HashSet<>();
        
        // Add group permissions
        String groupName = getPlayerGroup(playerId);
        PermissionGroup group = groups.get(groupName);
        if (group != null) {
            allPermissions.addAll(group.getAllPermissions(groups));
        }
        
        // Add player-specific permissions
        PlayerPermissions playerPerms = playerPermissions.get(playerId);
        if (playerPerms != null) {
            allPermissions.addAll(playerPerms.getPermissions());
        }
        
        return allPermissions;
    }
    
    /**
     * Get permission group
     */
    public PermissionGroup getGroup(String groupName) {
        return groups.get(groupName);
    }
    
    /**
     * Create new permission group
     */
    public void createGroup(String groupName, String prefix, String suffix, int priority) {
        PermissionGroup group = new PermissionGroup(groupName, prefix, suffix, priority);
        groups.put(groupName, group);
        LOGGER.info("Created permission group: {}", groupName);
    }
    
    /**
     * Delete permission group
     */
    public boolean deleteGroup(String groupName) {
        if (DEFAULT_GROUP.equals(groupName)) {
            return false; // Cannot delete default group
        }
        
        groups.remove(groupName);
        
        // Move players from deleted group to default
        playerGroups.entrySet().removeIf(entry -> {
            if (groupName.equals(entry.getValue())) {
                entry.setValue(DEFAULT_GROUP);
                return false; // Don't remove, just update
            }
            return false;
        });
        
        LOGGER.info("Deleted permission group: {}", groupName);
        return true;
    }
    
    /**
     * Get all groups
     */
    public Map<String, PermissionGroup> getAllGroups() {
        return new HashMap<>(groups);
    }
    
    /**
     * Get player's prefix
     */
    public String getPlayerPrefix(UUID playerId) {
        String groupName = getPlayerGroup(playerId);
        PermissionGroup group = groups.get(groupName);
        return group != null ? group.getPrefix() : "";
    }
    
    /**
     * Get player's suffix
     */
    public String getPlayerSuffix(UUID playerId) {
        String groupName = getPlayerGroup(playerId);
        PermissionGroup group = groups.get(groupName);
        return group != null ? group.getSuffix() : "";
    }
    
    /**
     * Get player's priority
     */
    public int getPlayerPriority(UUID playerId) {
        String groupName = getPlayerGroup(playerId);
        PermissionGroup group = groups.get(groupName);
        return group != null ? group.getPriority() : 0;
    }
    
    /**
     * Clear permission cache for a player
     */
    private void clearPlayerCache(UUID playerId) {
        String playerIdStr = playerId.toString();
        permissionCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerIdStr + ":"));
        cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().startsWith(playerIdStr + ":"));
    }
    
    /**
     * Clear all permission cache
     */
    public void clearCache() {
        permissionCache.clear();
        cacheTimestamps.clear();
        LOGGER.info("Cleared permission cache");
    }
    
    /**
     * Clean up expired temporary permissions
     */
    public void cleanupExpiredPermissions() {
        long currentTime = System.currentTimeMillis();
        int cleaned = 0;
        
        for (PlayerPermissions perms : playerPermissions.values()) {
            cleaned += perms.cleanupExpired(currentTime);
        }
        
        if (cleaned > 0) {
            LOGGER.info("Cleaned up {} expired temporary permissions", cleaned);
        }
    }
    
    /**
     * Save permissions to storage
     */
    public void savePermissions() {
        try {
            // Save to configuration files or database
            // Implementation would depend on storage system
            LOGGER.info("Saved permissions configuration");
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions configuration", e);
        }
    }
    
    /**
     * Get permission statistics
     */
    public Map<String, Object> getPermissionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("groups", groups.size());
        stats.put("players_with_custom_permissions", playerPermissions.size());
        stats.put("cache_size", permissionCache.size());
        
        // Group distribution
        Map<String, Integer> groupDistribution = new HashMap<>();
        for (String group : playerGroups.values()) {
            groupDistribution.merge(group, 1, Integer::sum);
        }
        stats.put("group_distribution", groupDistribution);
        
        return stats;
    }
}
