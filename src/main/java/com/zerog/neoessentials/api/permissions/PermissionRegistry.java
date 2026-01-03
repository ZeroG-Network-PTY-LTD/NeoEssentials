package com.zerog.neoessentials.api.permissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all NeoEssentials permission nodes.
 * This class automatically collects and manages all permission nodes used by the mod
 * for integration with permission plugins like PermissionsEX, LuckPerms, etc.
 */
public class PermissionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRegistry.class);
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final PermissionRegistry INSTANCE = new PermissionRegistry();
    }
    
    public static PermissionRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    // Storage for all permission nodes
    private final Set<String> registeredPermissions = ConcurrentHashMap.newKeySet();
    private final Map<String, PermissionInfo> permissionInfo = new ConcurrentHashMap<>();
    
    // Permission categories for organization
    public enum PermissionCategory {
        ADMIN("admin", "Administrative commands"),
        ECONOMY("economy", "Economy system"),
        TELEPORT("teleport", "Teleportation commands"),
        CHAT("chat", "Chat and messaging"),
        KITS("kits", "Kit system"),
        ITEMS("items", "Item management"),
        MISC("misc", "Miscellaneous commands"),
        CORE("core", "Core functionality");
        
        private final String key;
        private final String description;
        
        PermissionCategory(String key, String description) {
            this.key = key;
            this.description = description;
        }
        
        public String getKey() { return key; }
        public String getDescription() { return description; }
    }
    
    // Permission info class
    public static class PermissionInfo {
        private final String permission;
        private final String description;
        private final PermissionCategory category;
        private final boolean defaultValue;
        
        public PermissionInfo(String permission, String description, PermissionCategory category, boolean defaultValue) {
            this.permission = permission;
            this.description = description;
            this.category = category;
            this.defaultValue = defaultValue;
        }
        
        public String getPermission() { return permission; }
        public String getDescription() { return description; }
        public PermissionCategory getCategory() { return category; }
        public boolean getDefaultValue() { return defaultValue; }
    }
    
    private PermissionRegistry() {
        // Initialize with all known permission nodes
        registerAllPermissions();
        
        // Automatically discover and register ALL permissions from the codebase
        autoDiscoverPermissions();
    }
    
    /**
     * Register a permission node with metadata
     */
    public void register(String permission, String description, PermissionCategory category, boolean defaultValue) {
        if (permission == null || permission.trim().isEmpty()) {
            LOGGER.warn("Attempted to register empty or null permission");
            return;
        }
        
        permission = permission.trim();
        
        // Validate permission format
        if (!isValidPermission(permission)) {
            LOGGER.warn("Invalid permission format: {}", permission);
            return;
        }
        
        registeredPermissions.add(permission);
        permissionInfo.put(permission, new PermissionInfo(permission, description, category, defaultValue));
        
        LOGGER.debug("Registered permission: {} ({})", permission, category.getKey());
    }
    
    /**
     * Register a permission node with default settings
     */
    public void register(String permission, String description, PermissionCategory category) {
        register(permission, description, category, false);
    }
    
    /**
     * Register a permission node with minimal info
     */
    public void register(String permission) {
        register(permission, "Permission for " + permission, PermissionCategory.MISC, false);
    }
    
    /**
     * Get all registered permissions
     */
    public Set<String> getAllPermissions() {
        return Collections.unmodifiableSet(registeredPermissions);
    }
    
    /**
     * Get all permissions for a specific category
     */
    public Set<String> getPermissionsByCategory(PermissionCategory category) {
        return permissionInfo.values().stream()
                .filter(info -> info.getCategory() == category)
                .map(PermissionInfo::getPermission)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Get permission info
     */
    public PermissionInfo getPermissionInfo(String permission) {
        return permissionInfo.get(permission);
    }
    
    /**
     * Check if a permission is registered
     */
    public boolean isRegistered(String permission) {
        return registeredPermissions.contains(permission);
    }
    
    /**
     * Get all permissions starting with a prefix (for tab completion)
     */
    public List<String> getPermissionsStartingWith(String prefix) {
        return registeredPermissions.stream()
                .filter(perm -> perm.startsWith(prefix.toLowerCase()))
                .sorted()
                .toList();
    }
    
    /**
     * Get all NeoEssentials permissions (for tab completion)
     * Includes both registered and discovered permissions
     */
    public List<String> getNeoEssentialsPermissions() {
        // Get discovered permissions from scanner as well
        PermissionScanner scanner = PermissionScanner.getInstance();
        scanner.scanForPermissions();
        
        // Combine registered and discovered permissions
        java.util.Set<String> allNeoEssentialsPermissions = new java.util.HashSet<>(getPermissionsStartingWith("neoessentials."));
        allNeoEssentialsPermissions.addAll(scanner.getDiscoveredPermissions().stream()
                .filter(perm -> perm.startsWith("neoessentials."))
                .toList());
        
        return allNeoEssentialsPermissions.stream().sorted().toList();
    }
    
    /**
     * Validate permission format
     */
    private boolean isValidPermission(String permission) {
        return permission.matches("^[a-z0-9._-]+$") && permission.startsWith("neoessentials.");
    }
    
    /**
     * Register all known permission nodes
     */
    private void registerAllPermissions() {
        LOGGER.info("Registering NeoEssentials permission nodes...");
        
        // Core permissions
        register("neoessentials.use", "Basic mod usage", PermissionCategory.CORE, true);
        register("neoessentials.admin", "Administrative access", PermissionCategory.ADMIN, false);
        register("neoessentials.reload", "Reload configuration", PermissionCategory.ADMIN, false);
        
        // Economy permissions
        register("neoessentials.economy.balance", "Check own balance", PermissionCategory.ECONOMY, true);
        register("neoessentials.economy.balance.others", "Check others' balance", PermissionCategory.ECONOMY, false);
        register("neoessentials.economy.pay", "Send payments", PermissionCategory.ECONOMY, true);
        register("neoessentials.economy.pay.toggle", "Toggle payment acceptance", PermissionCategory.ECONOMY, true);
        register("neoessentials.economy.baltop", "View balance leaderboard", PermissionCategory.ECONOMY, true);
        register("neoessentials.economy.admin", "Economy administration", PermissionCategory.ECONOMY, false);
        register("neoessentials.economy.admin.give", "Give money to players", PermissionCategory.ECONOMY, false);
        register("neoessentials.economy.admin.take", "Take money from players", PermissionCategory.ECONOMY, false);
        register("neoessentials.economy.admin.set", "Set player balance", PermissionCategory.ECONOMY, false);
        
        // Teleportation permissions
        register("neoessentials.teleport.admin", "Administrative teleportation", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.admin.tp", "Teleport players", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.admin.tphere", "Teleport players to you", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.admin.tpall", "Teleport all players", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.admin.tppos", "Teleport to coordinates", PermissionCategory.TELEPORT, false);
        
        // Teleport requests
        register("neoessentials.teleport.request.tpa", "Send teleport requests", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.request.tpahere", "Request players teleport to you", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.request.accept", "Accept teleport requests", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.request.deny", "Deny teleport requests", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.request.cancel", "Cancel sent teleport requests", PermissionCategory.TELEPORT, true);
        
        // Home system
        register("neoessentials.teleport.home", "Use home system", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.home.set", "Set home locations", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.home.delete", "Delete home locations", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.home.list", "List home locations", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.home.others", "Access others' homes", PermissionCategory.TELEPORT, false);
        
        // Dynamic home limit permissions
        // Pattern: neoessentials.home.<amount> where <amount> is 1-100
        // Example: neoessentials.home.5 allows 5 homes
        // Note: These are checked dynamically, not registered individually
        // The highest matching permission wins, or config default is used
        
        // Warp system
        register("neoessentials.teleport.warp", "Use warp system", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.warp.create", "Create warps", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.warp.delete", "Delete warps", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.warp.list", "List warps", PermissionCategory.TELEPORT, true);
        
        // Dynamic player warp limit permissions
        // Pattern: neoessentials.warp.limit.<amount> where <amount> is 1-100
        // Example: neoessentials.warp.limit.10 allows 10 player warps
        // Special: neoessentials.warp.limit.unlimited allows unlimited player warps
        register("neoessentials.warp.limit.unlimited", "Unlimited player warps", PermissionCategory.TELEPORT, false);
        
        // Spawn system
        register("neoessentials.teleport.spawn", "Use spawn teleportation", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.spawn.set", "Set spawn location", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.spawn.info", "View spawn information", PermissionCategory.TELEPORT, false);
        register("neoessentials.teleport.spawn.clear", "Clear spawn location", PermissionCategory.TELEPORT, false);
        
        // Misc teleport
        register("neoessentials.teleport.back", "Use back teleportation", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.death", "Teleport to death location", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.top", "Teleport to highest block", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.jump", "Teleport through walls", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.jumpto", "Teleport to looking at", PermissionCategory.TELEPORT, true);
        register("neoessentials.teleport.tpr", "Random teleportation", PermissionCategory.TELEPORT, true);
        
        // Direct teleport - others access
        register("neoessentials.teleport.admin.tpo", "Teleport other players to locations", PermissionCategory.TELEPORT, false);
        
        // Kit system
        register("neoessentials.kits.use", "Use kit system", PermissionCategory.KITS, true);
        register("neoessentials.kits.list", "List available kits", PermissionCategory.KITS, true);
        register("neoessentials.kits.nocooldown", "Bypass kit cooldowns", PermissionCategory.KITS, false);
        register("neoessentials.kits.admin", "Kit administration", PermissionCategory.KITS, false);
        register("neoessentials.kits.admin.create", "Create kits", PermissionCategory.KITS, false);
        register("neoessentials.kits.admin.delete", "Delete kits", PermissionCategory.KITS, false);
        register("neoessentials.kits.admin.list", "List all kits (admin)", PermissionCategory.KITS, false);
        
        // Individual kit permissions (will be added dynamically)
        // These follow the pattern: neoessentials.kits.<kitname>
        // Cooldown exemption can also be per-kit: neoessentials.kits.<kitname>.nocooldown
        
        // Item management
        register("neoessentials.item.repair", "Repair items", PermissionCategory.ITEMS, false);
        register("neoessentials.item.enchant", "Enchant items", PermissionCategory.ITEMS, false);
        register("neoessentials.item.enchant.unsafe", "Unsafe enchanting", PermissionCategory.ITEMS, false);
        register("neoessentials.item.enchant.others", "Enchant others' items", PermissionCategory.ITEMS, false);
        register("neoessentials.item.powertool", "Use powertools", PermissionCategory.ITEMS, false);
        register("neoessentials.item.powertool.toggle", "Toggle powertools", PermissionCategory.ITEMS, false);
        register("neoessentials.item.dispose", "Use disposal system", PermissionCategory.ITEMS, true);
        register("neoessentials.item.clearinventory", "Clear inventory", PermissionCategory.ITEMS, false);
        register("neoessentials.item.clearinventory.others", "Clear others' inventory", PermissionCategory.ITEMS, false);
        
        // Chat system
        register("neoessentials.chat.msg", "Send private messages", PermissionCategory.CHAT, true);
        register("neoessentials.chat.reply", "Reply to messages", PermissionCategory.CHAT, true);
        register("neoessentials.chat.ignore", "Ignore players", PermissionCategory.CHAT, true);
        register("neoessentials.chat.unignore", "Unignore players", PermissionCategory.CHAT, true);
        register("neoessentials.chat.msgtoggle", "Toggle message acceptance", PermissionCategory.CHAT, true);
        register("neoessentials.chat.socialspy", "Use social spy", PermissionCategory.CHAT, false);
        register("neoessentials.chat.mute", "Mute players", PermissionCategory.CHAT, false);
        register("neoessentials.chat.unmute", "Unmute players", PermissionCategory.CHAT, false);
        register("neoessentials.chat.mutelist", "View mute list", PermissionCategory.CHAT, false);
        register("neoessentials.chat.exempt", "Exempt from muting", PermissionCategory.CHAT, false);
        
        // AFK system
        register("neoessentials.afk", "Use AFK system", PermissionCategory.MISC, true);
        register("neoessentials.afk.exempt", "Exempt from AFK kick", PermissionCategory.MISC, false);
        
        // Portable workstations
        register("neoessentials.anvil", "Open portable anvil", PermissionCategory.MISC, true);
        register("neoessentials.crafting", "Open portable crafting table", PermissionCategory.MISC, true);
        register("neoessentials.fletching", "Open portable fletching table", PermissionCategory.MISC, true);
        register("neoessentials.smithing", "Open portable smithing table", PermissionCategory.MISC, true);
        register("neoessentials.stonecutting", "Open portable stonecutter", PermissionCategory.MISC, true);

        // Utility commands
        register("neoessentials.realname", "Find player by nickname", PermissionCategory.MISC, true);
        register("neoessentials.whois", "View player information", PermissionCategory.MISC, true);
        register("neoessentials.whois.detailed", "View detailed player information", PermissionCategory.MISC, false);
        register("neoessentials.seen", "Check when player was last seen", PermissionCategory.MISC, true);
        register("neoessentials.sign", "Edit sign text", PermissionCategory.MISC, true);
        register("neoessentials.sign.colors", "Use colors in signs", PermissionCategory.MISC, false);
        register("neoessentials.rules", "View server rules", PermissionCategory.MISC, true);
        register("neoessentials.rules.admin", "Manage server rules", PermissionCategory.ADMIN, false);
        register("neoessentials.suicide", "Use suicide command", PermissionCategory.MISC, true);
        register("neoessentials.ping", "Check own ping", PermissionCategory.MISC, true);
        register("neoessentials.ping.others", "Check others' ping", PermissionCategory.MISC, false);

        // Permission system
        register("neoessentials.permissions.admin", "Permission system administration", PermissionCategory.ADMIN, false);
        register("neoessentials.permissions.reload", "Reload permissions", PermissionCategory.ADMIN, false);
        register("neoessentials.permissions.list", "List permissions", PermissionCategory.ADMIN, false);
        register("neoessentials.permissions.user", "User permission management", PermissionCategory.ADMIN, false);
        register("neoessentials.permissions.group", "Group permission management", PermissionCategory.ADMIN, false);
        
        // Debug and info
        register("neoessentials.debug", "Debug mode access", PermissionCategory.ADMIN, false);
        register("neoessentials.info", "View mod information", PermissionCategory.MISC, true);
        
        LOGGER.info("Registered {} permission nodes", registeredPermissions.size());
    }
    
    /**
     * Add kit permission dynamically when a kit is created
     */
    public void registerKitPermission(String kitName) {
        if (kitName == null || kitName.trim().isEmpty()) return;
        
        String permission = "neoessentials.kits." + kitName.toLowerCase();
        String nocooldownPermission = permission + ".nocooldown";
        
        register(permission, "Use kit: " + kitName, PermissionCategory.KITS, false);
        register(nocooldownPermission, "Bypass cooldown for kit: " + kitName, PermissionCategory.KITS, false);
    }
    
    /**
     * Remove kit permission when a kit is deleted
     */
    public void unregisterKitPermission(String kitName) {
        if (kitName == null || kitName.trim().isEmpty()) return;
        
        String permission = "neoessentials.kits." + kitName.toLowerCase();
        String nocooldownPermission = permission + ".nocooldown";
        
        registeredPermissions.remove(permission);
        permissionInfo.remove(permission);
        registeredPermissions.remove(nocooldownPermission);
        permissionInfo.remove(nocooldownPermission);
        
        LOGGER.debug("Unregistered kit permissions: {} and {}", permission, nocooldownPermission);
    }
    
    /**
     * Get summary of registered permissions by category
     */
    @SuppressWarnings("unused") // Public API method
    public Map<PermissionCategory, Integer> getPermissionSummary() {
        Map<PermissionCategory, Integer> summary = new EnumMap<>(PermissionCategory.class);
        
        for (PermissionCategory category : PermissionCategory.values()) {
            summary.put(category, getPermissionsByCategory(category).size());
        }
        
        return summary;
    }
    
    /**
     * Automatically discover and register permissions from the codebase
     */
    private void autoDiscoverPermissions() {
        LOGGER.info("Starting automatic permission discovery...");
        
        try {
            // Get the permission scanner and scan for permissions
            PermissionScanner scanner = PermissionScanner.getInstance();
            scanner.scanForPermissions();
            
            // Register all discovered permissions
            Set<String> discoveredPermissions = scanner.getDiscoveredPermissions();
            
            for (String permission : discoveredPermissions) {
                if (!isRegistered(permission)) {
                    // Auto-categorize based on permission structure
                    PermissionCategory category = categorizePermission(permission);
                    register(permission, "Auto-discovered permission", category, false);
                }
            }
            
            LOGGER.info("Auto-discovery completed: {} permissions discovered, {} new permissions registered", 
                discoveredPermissions.size(), 
                discoveredPermissions.stream().mapToInt(p -> isRegistered(p) ? 0 : 1).sum());
                
        } catch (Exception e) {
            LOGGER.error("Error during automatic permission discovery", e);
        }
    }
    
    /**
     * Categorize a permission based on its structure
     */
    private PermissionCategory categorizePermission(String permission) {
        String[] parts = permission.split("\\.");
        
        if (parts.length >= 2) {
            String category = parts[1].toLowerCase();
            
            return switch (category) {
                case "economy", "eco", "balance", "pay", "money" -> PermissionCategory.ECONOMY;
                case "teleport", "tp", "tpa", "home", "warp", "spawn" -> PermissionCategory.TELEPORT;
                case "chat", "msg", "message", "reply", "socialspy", "mute", "ignore" -> PermissionCategory.CHAT;
                case "kit", "kits" -> PermissionCategory.KITS;
                case "item", "items", "give", "enchant", "repair" -> PermissionCategory.ITEMS;
                case "admin", "reload", "permissions", "debug" -> PermissionCategory.ADMIN;
                default -> PermissionCategory.MISC;
            };
        }
        
        return PermissionCategory.CORE;
    }
    
    /**
     * Refresh permissions by re-scanning the codebase (useful for development)
     */
    public void refreshPermissions() {
        LOGGER.info("Refreshing permission registry...");
        
        int initialCount = registeredPermissions.size();
        autoDiscoverPermissions();
        int finalCount = registeredPermissions.size();
        
        LOGGER.info("Permission refresh completed: {} -> {} permissions (+" + (finalCount - initialCount) + " new)", 
            initialCount, finalCount);
    }
    
    /**
     * Get all auto-discovered permissions (separate from manual registrations)
     */
    public Set<String> getAutoDiscoveredPermissions() {
        try {
            PermissionScanner scanner = PermissionScanner.getInstance();
            return scanner.getDiscoveredPermissions();
        } catch (Exception e) {
            LOGGER.error("Error getting auto-discovered permissions", e);
            return Collections.emptySet();
        }
    }
    
    /**
     * Export permissions to a readable format (for documentation)
     */
    public List<String> exportPermissions() {
        List<String> export = new ArrayList<>();
        export.add("# NeoEssentials Permission Nodes");
        export.add("# Total: " + registeredPermissions.size() + " permissions");
        export.add("");
        
        for (PermissionCategory category : PermissionCategory.values()) {
            Set<String> categoryPerms = getPermissionsByCategory(category);
            if (categoryPerms.isEmpty()) continue;
            
            export.add("## " + category.getDescription() + " (" + categoryPerms.size() + ")");
            export.add("");
            
            categoryPerms.stream()
                    .sorted()
                    .forEach(perm -> {
                        PermissionInfo info = permissionInfo.get(perm);
                        export.add("- `" + perm + "` - " + info.getDescription() + 
                                  " (default: " + info.getDefaultValue() + ")");
                    });
            export.add("");
        }
        
        return export;
    }
}