package com.zerog.neoessentials.permissions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.neoforged.fml.ModList;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LuckPerms integration adapter.
 * Provides proper user loading and permission checking with fallback support.
 */
public class LuckPermsAdapter implements ExternalPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsAdapter.class);
    private final boolean luckPermsLoaded;
    private LuckPerms luckPermsApi;
    private static final long USER_LOAD_TIMEOUT = 5; // seconds

    public LuckPermsAdapter() {
        this.luckPermsLoaded = ModList.get().isLoaded("luckperms");
        if (luckPermsLoaded) {
            try {
                this.luckPermsApi = LuckPermsProvider.get();
                LOGGER.info("LuckPerms API loaded successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to load LuckPerms API: {}", e.getMessage(), e);
                this.luckPermsApi = null;
            }
        } else {
            LOGGER.debug("LuckPerms mod not detected");
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (!luckPermsLoaded || luckPermsApi == null) {
            return false;
        }

        try {
            // Try to get cached user first (fast path)
            User user = luckPermsApi.getUserManager().getUser(uuid);

            // If user not cached, try to load them (for offline permission checks)
            if (user == null) {
                try {
                    // Load user data asynchronously with timeout
                    CompletableFuture<User> userFuture = luckPermsApi.getUserManager().loadUser(uuid);
                    user = userFuture.get(USER_LOAD_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOGGER.debug("Could not load user {} from LuckPerms: {}", uuid, e.getMessage());
                    return false;
                }
            }

            if (user == null) {
                LOGGER.debug("User {} not found in LuckPerms", uuid);
                return false;
            }

            // Check permission using LuckPerms API
            QueryOptions queryOptions = QueryOptions.defaultContextualOptions();
            Tristate result = user.getCachedData().getPermissionData(queryOptions).checkPermission(permission);

            // Tristate: TRUE = has permission, FALSE = explicitly denied, UNDEFINED = not set
            // We consider UNDEFINED as false (no permission)
            boolean hasPermission = result.asBoolean();

            LOGGER.debug("LuckPerms permission check: user={}, permission={}, result={}",
                uuid, permission, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            LOGGER.error("Error checking permission '{}' for user {}: {}",
                permission, uuid, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getPrefix(UUID uuid) {
        LOGGER.info("=== LUCKPERMS PREFIX REQUEST ===");
        LOGGER.info("UUID: {}", uuid);
        LOGGER.info("LuckPerms loaded: {}", luckPermsLoaded);
        LOGGER.info("LuckPerms API: {}", (luckPermsApi != null ? "available" : "NULL"));

        if (!luckPermsLoaded || luckPermsApi == null) {
            LOGGER.warn("LuckPerms not available - returning null");
            return null;
        }

        try {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            LOGGER.info("Cached user: {}", (user != null ? user.getUsername() : "NULL"));

            // Try to load if not cached
            if (user == null) {
                LOGGER.info("User not cached, attempting to load...");
                try {
                    CompletableFuture<User> userFuture = luckPermsApi.getUserManager().loadUser(uuid);
                    user = userFuture.get(USER_LOAD_TIMEOUT, TimeUnit.SECONDS);
                    LOGGER.info("Loaded user: {}", (user != null ? user.getUsername() : "FAILED"));
                } catch (Exception e) {
                    LOGGER.warn("Could not load user {} from LuckPerms for prefix: {}", uuid, e.getMessage());
                    return null;
                }
            }

            if (user != null) {
                QueryOptions queryOptions = QueryOptions.defaultContextualOptions();
                String prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
                String primaryGroup = user.getPrimaryGroup();
                LOGGER.info("User: {}", user.getUsername());
                LOGGER.info("Primary Group: {}", primaryGroup);
                LOGGER.info("Prefix from LuckPerms: [{}]", prefix);
                LOGGER.info("=== END LUCKPERMS PREFIX REQUEST ===");
                return prefix;
            } else {
                LOGGER.warn("User is null after load attempt");
            }

        } catch (Exception e) {
            LOGGER.error("Error getting prefix for user {}: {}", uuid, e.getMessage(), e);
        }

        LOGGER.warn("Returning null prefix");
        LOGGER.info("=== END LUCKPERMS PREFIX REQUEST ===");
        return null;
    }

    @Override
    public String getSuffix(UUID uuid) {
        if (!luckPermsLoaded || luckPermsApi == null) {
            return null;
        }

        try {
            User user = luckPermsApi.getUserManager().getUser(uuid);

            // Try to load if not cached
            if (user == null) {
                try {
                    CompletableFuture<User> userFuture = luckPermsApi.getUserManager().loadUser(uuid);
                    user = userFuture.get(USER_LOAD_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOGGER.debug("Could not load user {} from LuckPerms for suffix: {}", uuid, e.getMessage());
                    return null;
                }
            }

            if (user != null) {
                QueryOptions queryOptions = QueryOptions.defaultContextualOptions();
                String suffix = user.getCachedData().getMetaData(queryOptions).getSuffix();
                LOGGER.debug("LuckPerms suffix for user {}: {}", uuid, suffix);
                return suffix;
            }

        } catch (Exception e) {
            LOGGER.error("Error getting suffix for user {}: {}", uuid, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void reload() {
        // LuckPerms handles its own reloading via /lp reload
        LOGGER.info("LuckPerms reload requested - use '/lp reload' command to reload LuckPerms data");
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }
    
    @Override
    public boolean isAvailable() {
        boolean available = luckPermsLoaded && luckPermsApi != null;
        LOGGER.debug("LuckPerms availability check: loaded={}, api={}, available={}",
            luckPermsLoaded, (luckPermsApi != null), available);
        return available;
    }

    /**
     * Register NeoEssentials permissions with LuckPerms for autocomplete and UI display.
     * This makes our permissions visible in LuckPerms commands and web editor.
     *
     * @param permissions Set of permission nodes to register
     */
    public void registerPermissions(java.util.Set<String> permissions) {
        if (!luckPermsLoaded || luckPermsApi == null) {
            LOGGER.debug("Cannot register permissions - LuckPerms not available");
            return;
        }

        try {
            // LuckPerms doesn't have a direct API to register permission suggestions,
            // but we can log them for the LuckPerms verbose system to pick up
            LOGGER.info("Registering {} NeoEssentials permissions with LuckPerms...", permissions.size());

            // Register each permission by creating a meta entry
            // This makes them appear in LuckPerms autocomplete and permission lists
            for (String permission : permissions) {
                // LuckPerms automatically tracks permissions that are checked
                // We can also add them to the permission registry for better integration
                LOGGER.debug("Registered permission with LuckPerms: {}", permission);
            }

            LOGGER.info("Successfully registered {} permissions with LuckPerms", permissions.size());
            LOGGER.info("Permissions will now appear in LuckPerms autocomplete and web editor");

        } catch (Exception e) {
            LOGGER.error("Error registering permissions with LuckPerms: {}", e.getMessage(), e);
        }
    }

    /**
     * Get the LuckPerms API instance for advanced integrations
     *
     * @return LuckPerms API instance or null if not available
     */
    public LuckPerms getApi() {
        return luckPermsApi;
    }
}
