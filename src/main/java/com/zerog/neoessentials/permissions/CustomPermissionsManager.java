package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.player.PlayerDataManager;
import com.zerog.neoessentials.player.PlayerData;
import net.minecraft.server.level.ServerPlayer;
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
    // Registered custom permission nodes (for tablist, etc.)
    private final Set<String> registeredCustomTablistPermissions = new HashSet<>();
    private final Map<String, String> customPermissionDescriptions = new HashMap<>();

    /**
     * Register a custom permission node for tablist or other features
     */
    public void registerCustomPermissionNode(String node, String description) {
        if (node != null && !node.isEmpty()) {
            registeredCustomTablistPermissions.add(node);
            if (description != null) customPermissionDescriptions.put(node, description);
            LOGGER.info("Registered custom permission node: {} - {}", node, description);
        }
    }

    /**
     * Get all registered custom tablist permission nodes
     */
    public Set<String> getRegisteredCustomTablistPermissions() {
        return Collections.unmodifiableSet(registeredCustomTablistPermissions);
    }

    /**
     * Get description for a custom permission node
     */
    public String getCustomPermissionDescription(String node) {
        return customPermissionDescriptions.getOrDefault(node, "");
    }
    // Store player group tags for instant display updates
    // Only one definition for playerGroups below

    /**
     * Set player group and refresh display (no CommandSourceStack dependency)
     */
    public void setGroup(ServerPlayer player, String group) {
        playerGroups.put(player.getUUID(), group);
        LOGGER.info("[NeoEssentials] setGroup called for {}. New group: {}. Clearing permission cache.", player.getUUID(), group);
        clearPlayerCache(player.getUUID()); // Clear permission cache for this player
        refreshPlayerDisplay(player);
        // Fire PermissionUpdateEvent for dynamic UI refresh
        try {
            com.zerog.neoessentials.features.PermissionUpdateEvent event =
                new com.zerog.neoessentials.features.PermissionUpdateEvent(player);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            LOGGER.info("[NeoEssentials] Firing PermissionUpdateEvent for player {}.", player.getUUID());
        } catch (Exception e) {
            LOGGER.warn("Failed to fire PermissionUpdateEvent for player {}: {}", player.getUUID(), e.getMessage());
        }
    }

    /**
     * Refresh player display (triggers event-based updates)
     */
    public void refreshPlayerDisplay(ServerPlayer player) {
        // Always reset to raw username or nickname only (no prefix/suffix)
        try {
            String rawName = player.getGameProfile().getName();
            String nickname = null;
            try {
                java.lang.reflect.Field nicknamesField = Class.forName("com.zerog.neoessentials.commands.essentials.NickCommand").getDeclaredField("nicknames");
                nicknamesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<java.util.UUID, String> nicknames = (java.util.Map<java.util.UUID, String>) nicknamesField.get(null);
                nickname = nicknames.get(player.getUUID());
            } catch (Exception ignored) {}
            net.minecraft.network.chat.Component formattedName = net.minecraft.network.chat.Component.literal(nickname != null ? nickname : rawName);
            player.setCustomName(formattedName);
            LOGGER.info("NeoEssentials: Set player displayName='{}' (rawName='{}', nickname='{}')", formattedName.getString(), rawName, nickname);
        } catch (Exception e) {
            LOGGER.warn("Failed to update display for {}: {}", player.getName().getString(), e.getMessage());
        }
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPermissionsManager.class);
    private static CustomPermissionsManager instance;
    
    // Permission storage
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerPermissions> playerPermissions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerGroups = new ConcurrentHashMap<>();
    
    // Persistent storage manager
    private final PermissionStorageManager storageManager;
    
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
    this.storageManager = PermissionStorageManager.getInstance();
    initialize();
    loadMetaFromConfig();
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
        com.zerog.neoessentials.util.DebugUtil.debugLog("Initializing Custom Permissions Manager...");
        
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
        defaultGroup.addPermission(PermissionNodes.PLAYTIME_VIEW);
        defaultGroup.addPermission(PermissionNodes.ACHIEVEMENTS_VIEW);
        defaultGroup.addPermission(PermissionNodes.PREFERENCES_SET);
        defaultGroup.addPermission(PermissionNodes.PREFERENCES_VIEW);
        defaultGroup.addPermission(PermissionNodes.SHOP_SIGN_CREATE);
        defaultGroup.addPermission(PermissionNodes.SHOP_SIGN_USE);
        groups.put(DEFAULT_GROUP, defaultGroup);
        
        // VIP group - enhanced permissions using comprehensive permission nodes
        PermissionGroup vipGroup = new PermissionGroup(VIP_GROUP, "&b[VIP]", " &b♦", 10);
        vipGroup.setInheritance(DEFAULT_GROUP);
        vipGroup.addPermission(PermissionNodes.FLY_SELF);
        vipGroup.addPermission(PermissionNodes.HEAL_SELF);
        vipGroup.addPermission(PermissionNodes.FEED_SELF);
        vipGroup.addPermission(PermissionNodes.FLY_SELF);
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
     * Load permissions from persistent storage
     */
    private void loadPermissionsFromConfig() {
        try {
            // Load groups from storage
            Map<String, PermissionGroup> savedGroups = storageManager.loadGroups();
            
            if (savedGroups.isEmpty()) {
                LOGGER.info("No saved groups found, creating default groups");
                createDefaultGroups();
                // Save the default groups to storage
                storageManager.saveGroups(groups);
            } else {
                // Load saved groups
                groups.clear();
                groups.putAll(savedGroups);
                LOGGER.info("Loaded {} permission groups from storage", groups.size());
            }
            
            LOGGER.info("Permission configuration loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to load permissions configuration", e);
            // Fallback to default groups
            createDefaultGroups();
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
        // Check memory cache first
        String cachedGroup = playerGroups.get(playerId);
        if (cachedGroup != null) {
            return cachedGroup;
        }
        
        // Load from persistent storage
        PermissionStorageManager.PlayerPermissionData data = storageManager.loadPlayerData(playerId);
        String groupName = data.groupName;
        
        // Cache the result
        playerGroups.put(playerId, groupName);
        
        return groupName;
    }
    
    /**
     * Set player's primary group
     */
    public void setPlayerGroup(UUID playerId, String groupName) {
        if (groups.containsKey(groupName)) {
            playerGroups.put(playerId, groupName);
            clearPlayerCache(playerId);
            // Save to persistent storage
            storageManager.savePlayerGroup(playerId, groupName);
            LOGGER.info("Set player {} to group {} (saved to storage)", playerId, groupName);
            // If player is online, use setGroup(ServerPlayer, String) for correct event and tablist update
            net.minecraft.server.MinecraftServer server = getActiveServerInstance();
            ServerPlayer player = null;
            if (server != null) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p.getUUID().equals(playerId)) {
                        player = p;
                        break;
                    }
                }
            }
            if (player != null) {
                setGroup(player, groupName);
            }
        } else {
            LOGGER.warn("Attempted to set player {} to non-existent group {}", playerId, groupName);
        }
    }

    /**
     * Get the active MinecraftServer instance (NeoForge/Forge)
     */
    private net.minecraft.server.MinecraftServer getActiveServerInstance() {
        // NeoForge/Forge: use ServerLifecycleHooks.getCurrentServer()
        try {
            Class<?> hooksClass = Class.forName("net.neoforged.neoforge.server.ServerLifecycleHooks");
            java.lang.reflect.Method getServerMethod = hooksClass.getMethod("getCurrentServer");
            Object server = getServerMethod.invoke(null);
            return (net.minecraft.server.MinecraftServer) server;
        } catch (Exception e) {
            LOGGER.warn("Could not get active server instance: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Add permission to player
     */
    public void addPlayerPermission(UUID playerId, String permission) {
        playerPermissions.computeIfAbsent(playerId, k -> new PlayerPermissions())
                .addPermission(permission);
        clearPlayerCache(playerId);
        
        // Save to persistent storage
        storageManager.savePlayerPermission(playerId, permission, true);
        
        LOGGER.info("Added permission {} to player {} (saved to storage)", permission, playerId);
    }
    
    /**
     * Remove permission from player
     */
    public void removePlayerPermission(UUID playerId, String permission) {
        PlayerPermissions perms = playerPermissions.get(playerId);
        if (perms != null) {
            perms.removePermission(permission);
            clearPlayerCache(playerId);
            
            // Remove from persistent storage
            storageManager.removePlayerPermission(playerId, permission);
            
            LOGGER.info("Removed permission {} from player {} (removed from storage)", permission, playerId);
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
     * Get player permissions as a map for storage
     */
    public Map<String, Boolean> getPlayerPermissionsMap(UUID playerId) {
        Map<String, Boolean> permissionMap = new HashMap<>();
        
        // Get player-specific permissions only (not group permissions)
        PlayerPermissions playerPerms = playerPermissions.get(playerId);
        if (playerPerms != null) {
            Set<String> permissions = playerPerms.getPermissions();
            for (String permission : permissions) {
                if (permission.startsWith("-")) {
                    // Negative permission
                    permissionMap.put(permission.substring(1), false);
                } else {
                    // Positive permission
                    permissionMap.put(permission, true);
                }
            }
        }
        
        return permissionMap;
    }
    
    /**
     * Set player permissions from a map (for loading from storage)
     */
    public void setPlayerPermissionsFromMap(UUID playerId, Map<String, Boolean> permissionMap) {
        // Clear existing player permissions
        playerPermissions.remove(playerId);
        
        // Set new permissions
        for (Map.Entry<String, Boolean> entry : permissionMap.entrySet()) {
            String permission = entry.getKey();
            boolean granted = entry.getValue();
            
            if (granted) {
                addPlayerPermission(playerId, permission);
            } else {
                addPlayerPermission(playerId, "-" + permission);
            }
        }
        
        clearPlayerCache(playerId);
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
        // Save to persistent storage
        storageManager.saveGroup(groupName, group);
        LOGGER.info("Created permission group: {} (saved to storage)", groupName);
        // Fire PermissionUpdateEvent for all players in this group
        try {
            Class<?> eventClass = Class.forName("com.zerog.neoessentials.features.PermissionUpdateEvent");
            java.lang.reflect.Constructor<?> ctor = eventClass.getConstructor(UUID.class);
            for (Map.Entry<UUID, String> entry : playerGroups.entrySet()) {
                if (groupName.equals(entry.getValue())) {
                    Object event = ctor.newInstance(entry.getKey());
                    Class<?> eventBusClass = Class.forName("net.neoforged.bus.api.EventBus");
                    java.lang.reflect.Field busField = Class.forName("net.neoforged.neoforge.eventbus.EventBus").getDeclaredField("EVENT_BUS");
                    Object eventBus = busField.get(null);
                    eventBusClass.getMethod("post", Object.class).invoke(eventBus, event);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to fire PermissionUpdateEvent for group {}: {}", groupName, e.getMessage());
        }
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
        for (Map.Entry<UUID, String> entry : playerGroups.entrySet()) {
            if (groupName.equals(entry.getValue())) {
                UUID playerId = entry.getKey();
                entry.setValue(DEFAULT_GROUP);
                // Update storage
                storageManager.savePlayerGroup(playerId, DEFAULT_GROUP);
                // Fire PermissionUpdateEvent for affected player
                try {
                    Class<?> eventClass = Class.forName("com.zerog.neoessentials.features.PermissionUpdateEvent");
                    java.lang.reflect.Constructor<?> ctor = eventClass.getConstructor(UUID.class);
                    Object event = ctor.newInstance(playerId);
                    Class<?> eventBusClass = Class.forName("net.neoforged.bus.api.EventBus");
                    java.lang.reflect.Field busField = Class.forName("net.neoforged.neoforge.eventbus.EventBus").getDeclaredField("EVENT_BUS");
                    Object eventBus = busField.get(null);
                    eventBusClass.getMethod("post", Object.class).invoke(eventBus, event);
                } catch (Exception e) {
                    LOGGER.warn("Failed to fire PermissionUpdateEvent for player {}: {}", playerId, e.getMessage());
                }
            }
        }
        // Remove from persistent storage
        storageManager.deleteGroup(groupName);
        LOGGER.info("Deleted permission group: {} (removed from storage)", groupName);
        return true;
    }
    
    /**
     * Get all groups
     */
    public Map<String, PermissionGroup> getAllGroups() {
        return new HashMap<>(groups);
    }
    
    /**
     * Get player's prefix with animation support
     */
    public String getPlayerPrefix(UUID playerId) {
        String groupName = getPlayerGroup(playerId);
        PermissionGroup group = groups.get(groupName);
        if (group == null) return "";
        String prefix = group.getPrefix();
        // Animation support: use AnimationManager if available and result is non-empty, else fallback to static
        String animatedPrefix = null;
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    try {
                        Class<?> animationManagerClass = Class.forName("com.zerog.neoessentials.animation.AnimationManager");
                        Object animationManager = animationManagerClass.getDeclaredMethod("getInstance").invoke(null);
                        if (animationManager != null) {
                            java.lang.reflect.Method processMethod = animationManagerClass.getDeclaredMethod("processAnimatedText", String.class, net.minecraft.server.level.ServerPlayer.class);
                            String processed = (String) processMethod.invoke(animationManager, prefix, player);
                            if (processed != null && !processed.isEmpty()) {
                                animatedPrefix = processed;
                                LOGGER.debug("[NeoEssentials] Processed animated prefix for {}: {}", player.getName().getString(), animatedPrefix);
                            } else {
                                LOGGER.debug("[NeoEssentials] AnimationManager returned empty for {}. Using static prefix.", player.getName().getString());
                            }
                        } else {
                            LOGGER.debug("[NeoEssentials] AnimationManager instance is null. Using static prefix for {}.", player.getName().getString());
                        }
                    } catch (Exception e) {
                        LOGGER.debug("[NeoEssentials] AnimationManager not available for {}: {}. Using static prefix.", playerId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[NeoEssentials] Error processing animated prefix for player {}: {}. Using static prefix.", playerId, e.getMessage());
        }
        if (animatedPrefix != null && !animatedPrefix.isEmpty()) {
            return animatedPrefix;
        } else {
            LOGGER.debug("[NeoEssentials] Using static prefix for player {}: {}", playerId, prefix);
            return prefix;
        }
    }
    
    /**
     * Get player's suffix
     */
    public String getPlayerSuffix(UUID playerId) {
        String groupName = getPlayerGroup(playerId);
        PermissionGroup group = groups.get(groupName);
        String suffix = group != null ? group.getSuffix() : "";
        String animatedSuffix = null;
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    try {
                        Class<?> animationManagerClass = Class.forName("com.zerog.neoessentials.animation.AnimationManager");
                        Object animationManager = animationManagerClass.getDeclaredMethod("getInstance").invoke(null);
                        if (animationManager != null) {
                            java.lang.reflect.Method processMethod = animationManagerClass.getDeclaredMethod("processAnimatedText", String.class, net.minecraft.server.level.ServerPlayer.class);
                            String processed = (String) processMethod.invoke(animationManager, suffix, player);
                            if (processed != null && !processed.isEmpty()) {
                                animatedSuffix = processed;
                                LOGGER.debug("[NeoEssentials] Processed animated suffix for {}: {}", player.getName().getString(), animatedSuffix);
                            } else {
                                LOGGER.debug("[NeoEssentials] AnimationManager returned empty for suffix of {}. Using static suffix.", player.getName().getString());
                            }
                        } else {
                            LOGGER.debug("[NeoEssentials] AnimationManager instance is null. Using static suffix for {}.", player.getName().getString());
                        }
                    } catch (Exception e) {
                        LOGGER.debug("[NeoEssentials] AnimationManager not available for suffix of {}: {}. Using static suffix.", playerId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[NeoEssentials] Error processing animated suffix for player {}: {}. Using static suffix.", playerId, e.getMessage());
        }
        if (animatedSuffix != null && !animatedSuffix.isEmpty()) {
            return animatedSuffix;
        } else {
            LOGGER.debug("[NeoEssentials] Using static suffix for player {}: {}", playerId, suffix);
            return suffix;
        }
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
            // Save group assignments and individual permissions to PlayerDataManager
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            
            for (Map.Entry<UUID, String> entry : playerGroups.entrySet()) {
                UUID playerId = entry.getKey();
                String groupName = entry.getValue();
                
                PlayerData playerData = playerDataManager.getPlayerData(playerId);
                if (playerData != null) {
                    playerData.setPermissionGroup(groupName);
                    
                    // Save individual permissions
                    Map<String, Boolean> playerPermsMap = getPlayerPermissionsMap(playerId);
                    playerData.setPlayerPermissions(playerPermsMap);
                    
                    playerDataManager.savePlayerData(playerData);
                }
            }
            
            LOGGER.info("Saved permissions configuration for {} players", playerGroups.size());
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
    
    // =======================
    // GROUP PERMISSION MANAGEMENT WITH STORAGE
    // =======================
    
    /**
     * Add permission to a group
     */
    public void addGroupPermission(String groupName, String permission) {
        PermissionGroup group = groups.get(groupName);
        if (group != null) {
            group.addPermission(permission);
            
            // Save to persistent storage
            storageManager.saveGroup(groupName, group);
            
            LOGGER.info("Added permission {} to group {} (saved to storage)", permission, groupName);
        } else {
            LOGGER.warn("Attempted to add permission to non-existent group: {}", groupName);
        }
    }
    
    /**
     * Remove permission from a group
     */
    public void removeGroupPermission(String groupName, String permission) {
        PermissionGroup group = groups.get(groupName);
        if (group != null) {
            group.removePermission(permission);
            
            // Save to persistent storage
            storageManager.saveGroup(groupName, group);
            
            LOGGER.info("Removed permission {} from group {} (saved to storage)", permission, groupName);
        } else {
            LOGGER.warn("Attempted to remove permission from non-existent group: {}", groupName);
        }
    }
    
    /**
     * Set group inheritance
     */
    public void setGroupInheritance(String groupName, String parentGroup) {
        PermissionGroup group = groups.get(groupName);
        if (group != null) {
            if (parentGroup == null || parentGroup.isEmpty() || "none".equalsIgnoreCase(parentGroup)) {
                group.setInheritance(null);
            } else if (groups.containsKey(parentGroup)) {
                group.setInheritance(parentGroup);
            } else {
                LOGGER.warn("Parent group {} does not exist", parentGroup);
                return;
            }
            
            // Save to persistent storage
            storageManager.saveGroup(groupName, group);
            
            LOGGER.info("Set inheritance for group {} to {} (saved to storage)", groupName, parentGroup);
        } else {
            LOGGER.warn("Attempted to set inheritance for non-existent group: {}", groupName);
        }
    }
    
    /**
     * Modify group prefix
     */
    public void setGroupPrefix(String groupName, String prefix) {
        PermissionGroup group = groups.get(groupName);
        if (group != null) {
            group.setPrefix(prefix);
            
            // Save to persistent storage
            storageManager.saveGroup(groupName, group);
            
            LOGGER.info("Set prefix for group {} to '{}' (saved to storage)", groupName, prefix);
        } else {
            LOGGER.warn("Attempted to set prefix for non-existent group: {}", groupName);
        }
    }
    
    /**
     * Modify group suffix
     */
    public void setGroupSuffix(String groupName, String suffix) {
        PermissionGroup group = groups.get(groupName);
        if (group != null) {
            group.setSuffix(suffix);
            
            // Save to persistent storage
            storageManager.saveGroup(groupName, group);
            
            LOGGER.info("Set suffix for group {} to '{}' (saved to storage)", groupName, suffix);
        } else {
            LOGGER.warn("Attempted to set suffix for non-existent group: {}", groupName);
        }
    }
    
    /**
     * Modify group priority
     */
    public void setGroupPriority(String groupName, int priority) {
        PermissionGroup group = groups.get(groupName);
        if (group != null) {
            group.setPriority(priority);
            
            // Save to persistent storage
            storageManager.saveGroup(groupName, group);
            
            LOGGER.info("Set priority for group {} to {} (saved to storage)", groupName, priority);
        } else {
            LOGGER.warn("Attempted to set priority for non-existent group: {}", groupName);
        }
    }
    
    /**
     * Force save all data to storage
     */
    public void saveAll() {
        try {
            // Save all groups
            storageManager.saveGroups(groups);
            
            // Save all player data
            for (Map.Entry<UUID, String> entry : playerGroups.entrySet()) {
                UUID playerId = entry.getKey();
                String groupName = entry.getValue();
                
                // Get player's individual permissions
                Map<String, Boolean> permissions = new HashMap<>();
                PlayerPermissions playerPerms = playerPermissions.get(playerId);
                if (playerPerms != null) {
                    permissions = getPlayerPermissionsMap(playerId);
                }
                
                storageManager.savePlayerData(playerId, groupName, permissions);
            }
            
            LOGGER.info("Saved all permission data to storage");
        } catch (Exception e) {
            LOGGER.error("Failed to save all permission data", e);
        }
    }

    /**
     * Robust prefix/suffix resolution (user, per-world, group, default)
     */

    // Permissions meta cache
    private final Map<UUID, Map<String, String>> userMetaCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> groupMetaCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> worldOverrideCache = new ConcurrentHashMap<>();

    // Load meta from permissions.toml (roles, users, overrides)
    private void loadMetaFromConfig() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("config/permissions.toml");
            if (!java.nio.file.Files.exists(path)) return;
            List<String> lines = java.nio.file.Files.readAllLines(path);
            String currentSection = "";
            String currentWorld = null;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[")) {
                    currentSection = line.replace("[","").replace("]","");
                    if (currentSection.startsWith("overrides.")) {
                        currentWorld = currentSection.substring("overrides.".length());
                    } else {
                        currentWorld = null;
                    }
                    continue;
                }
                String[] kv = line.split("=",2);
                if (kv.length != 2) continue;
                String key = kv[0].trim();
                String value = kv[1].trim().replaceAll("^\"|\"$", "");
                if (currentSection.startsWith("roles.")) {
                    String role = currentSection.substring("roles.".length());
                    groupMetaCache.computeIfAbsent(role, k->new HashMap<>()).put(key, value);
                } else if (currentSection.startsWith("users.")) {
                    String uuid = currentSection.substring("users.".length());
                    userMetaCache.computeIfAbsent(UUID.fromString(uuid), k->new HashMap<>()).put(key, value);
                } else if (currentSection.startsWith("overrides.")) {
                    // e.g. roles._DEFAULT_.suffix = " (lobby)"
                    if (key.startsWith("roles.")) {
                        String[] parts = key.split("\\.");
                        if (parts.length == 3) {
                            String role = parts[1];
                            String metaKey = parts[2];
                            worldOverrideCache.computeIfAbsent(currentWorld+":"+role, k->new HashMap<>()).put(metaKey, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load meta from permissions.toml: {}", e.getMessage());
        }
    }


    // Robust prefix/suffix resolution
    public String getPrefix(UUID playerId, String world) {
        // 1. User-specific override
        Map<String, String> userMeta = userMetaCache.get(playerId);
        if (userMeta != null && userMeta.get("prefix") != null) {
            return userMeta.get("prefix");
        }
        // 2. Per-world override (role-based)
        String groupName = getPlayerGroup(playerId);
        Map<String, String> worldMeta = worldOverrideCache.get(world+":"+groupName);
        if (worldMeta != null && worldMeta.get("prefix") != null) {
            return worldMeta.get("prefix");
        }
        // 3. Group-level
        Map<String, String> groupMeta = groupMetaCache.get(groupName);
        if (groupMeta != null && groupMeta.get("prefix") != null) {
            return groupMeta.get("prefix");
        }
        // 4. Default fallback
        Map<String, String> defaultMeta = groupMetaCache.get("_DEFAULT_");
        if (defaultMeta != null && defaultMeta.get("prefix") != null) {
            return defaultMeta.get("prefix");
        }
        return "";
    }

    public String getSuffix(UUID playerId, String world) {
        Map<String, String> userMeta = userMetaCache.get(playerId);
        if (userMeta != null && userMeta.get("suffix") != null) {
            return userMeta.get("suffix");
        }
        String groupName = getPlayerGroup(playerId);
        Map<String, String> worldMeta = worldOverrideCache.get(world+":"+groupName);
        if (worldMeta != null && worldMeta.get("suffix") != null) {
            return worldMeta.get("suffix");
        }
        Map<String, String> groupMeta = groupMetaCache.get(groupName);
        if (groupMeta != null && groupMeta.get("suffix") != null) {
            return groupMeta.get("suffix");
        }
        Map<String, String> defaultMeta = groupMetaCache.get("_DEFAULT_");
        if (defaultMeta != null && defaultMeta.get("suffix") != null) {
            return defaultMeta.get("suffix");
        }
        return "";
    }
}
