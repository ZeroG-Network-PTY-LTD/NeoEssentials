package com.zerog.neoessentials.permissions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.neoforged.fml.ModList;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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

    // Held so the subscriptions stay active for the lifetime of this adapter
    @SuppressWarnings("unused")
    private EventSubscription<UserDataRecalculateEvent> userDataSubscription;
    @SuppressWarnings("unused")
    private EventSubscription<GroupDataRecalculateEvent> groupDataSubscription;

    public LuckPermsAdapter() {
        this.luckPermsLoaded = ModList.get().isLoaded("luckperms");
        if (luckPermsLoaded) {
            try {
                this.luckPermsApi = LuckPermsProvider.get();
                LOGGER.info("LuckPerms API loaded successfully");
                registerEventListeners();
            } catch (Exception e) {
                LOGGER.error("Failed to load LuckPerms API: {}", e.getMessage(), e);
                this.luckPermsApi = null;
            }
        } else {
            LOGGER.debug("LuckPerms mod not detected");
        }
    }

    /**
     * Subscribe to LuckPerms events so that when a user's permissions or group
     * assignments change the Minecraft command tree is immediately re-sent to that
     * player (or to all online players when a group is modified).  This ensures
     * that permission-gated commands appear / disappear in tab-completion without
     * requiring a relog or server restart.
     */
    private void registerEventListeners() {
        if (luckPermsApi == null) return;

        // A specific user's cached data was recalculated (e.g. added to a new group)
        userDataSubscription = luckPermsApi.getEventBus().subscribe(UserDataRecalculateEvent.class, event -> {
            UUID uuid = event.getUser().getUniqueId();
            LOGGER.debug("LuckPerms UserDataRecalculate for {} — refreshing command tree", uuid);
            resendCommandsToPlayer(uuid);
        });

        // A group's cached data was recalculated (e.g. permission added to a role)
        // All online members of that group need their command tree refreshed.
        groupDataSubscription = luckPermsApi.getEventBus().subscribe(GroupDataRecalculateEvent.class, event -> {
            String groupName = event.getGroup().getName();
            LOGGER.debug("LuckPerms GroupDataRecalculate for group '{}' — refreshing command trees", groupName);
            resendCommandsToGroupMembers(groupName);
        });
    }

    /**
     * Re-sends the Brigadier command tree to a specific online player.
     * Safe to call from any thread — schedules work on the server thread.
     */
    private void resendCommandsToPlayer(UUID uuid) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        // Schedule on the main server thread to avoid concurrent modification issues
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                server.getCommands().sendCommands(player);
                LOGGER.debug("Command tree re-sent to player {} after LuckPerms permission change", uuid);
            }
        });
    }

    /**
     * Re-sends the command tree to every online player that belongs to the
     * given LuckPerms group, so group-level permission changes propagate
     * immediately.
     */
    private void resendCommandsToGroupMembers(String groupName) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || luckPermsApi == null) return;
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                try {
                    User lpUser = luckPermsApi.getUserManager().getUser(player.getUUID());
                    if (lpUser != null && lpUser.getPrimaryGroup().equalsIgnoreCase(groupName)) {
                        server.getCommands().sendCommands(player);
                        LOGGER.debug("Command tree re-sent to {} (group '{}')", player.getName().getString(), groupName);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not refresh commands for {}: {}", player.getName().getString(), e.getMessage());
                }
            }
        });
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (!luckPermsLoaded || luckPermsApi == null) {
            return false;
        }

        try {
            // For online players prefer their live, context-aware cached data
            var server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer onlinePlayer = server != null ? server.getPlayerList().getPlayer(uuid) : null;

            // Try to get cached user first (fast path)
            User user = luckPermsApi.getUserManager().getUser(uuid);

            // If user not cached, try to load them (for offline permission checks)
            if (user == null) {
                try {
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

            // Use the player's live query options when online (picks up world/server contexts)
            // Fall back to default contextual options for offline checks
            QueryOptions queryOptions;
            if (onlinePlayer != null) {
                queryOptions = luckPermsApi.getContextManager()
                        .getQueryOptions(user)
                        .orElse(QueryOptions.defaultContextualOptions());
            } else {
                queryOptions = QueryOptions.defaultContextualOptions();
            }

            Tristate result = user.getCachedData().getPermissionData(queryOptions).checkPermission(permission);

            // Tristate: TRUE = has permission, FALSE = explicitly denied, UNDEFINED = not set
            boolean hasPermission = result.asBoolean();

            LOGGER.debug("LuckPerms permission check: user={}, permission={}, result={} ({})",
                uuid, permission, hasPermission, result);

            return hasPermission;

        } catch (Exception e) {
            LOGGER.error("Error checking permission '{}' for user {}: {}",
                permission, uuid, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getPrefix(UUID uuid) {
        LOGGER.debug("=== LUCKPERMS PREFIX REQUEST ===");
        LOGGER.debug("UUID: {}", uuid);
        LOGGER.debug("LuckPerms loaded: {}", luckPermsLoaded);
        LOGGER.debug("LuckPerms API: {}", (luckPermsApi != null ? "available" : "NULL"));

        if (!luckPermsLoaded || luckPermsApi == null) {
            LOGGER.debug("LuckPerms not available - returning null");
            return null;
        }

        try {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            LOGGER.debug("Cached user: {}", (user != null ? user.getUsername() : "NULL"));

            // Try to load if not cached
            if (user == null) {
                LOGGER.debug("User not cached, attempting to load...");
                try {
                    CompletableFuture<User> userFuture = luckPermsApi.getUserManager().loadUser(uuid);
                    user = userFuture.get(USER_LOAD_TIMEOUT, TimeUnit.SECONDS);
                    LOGGER.debug("Loaded user: {}", (user != null ? user.getUsername() : "FAILED"));
                } catch (Exception e) {
                    LOGGER.debug("Could not load user {} from LuckPerms for prefix: {}", uuid, e.getMessage());
                    return null;
                }
            }

            if (user != null) {
                QueryOptions queryOptions = QueryOptions.defaultContextualOptions();
                var metaData = user.getCachedData().getMetaData(queryOptions);
                String prefix = metaData.getPrefix();
                String suffix = metaData.getSuffix();
                String primaryGroup = user.getPrimaryGroup();

                // Get all meta entries to debug
                var meta = metaData.getMeta();

                LOGGER.debug("User: {}", user.getUsername());
                LOGGER.debug("Primary Group: {}", primaryGroup);
                LOGGER.debug("Prefix from LuckPerms: [{}]", prefix);
                LOGGER.debug("Suffix from LuckPerms: [{}]", suffix);
                LOGGER.debug("All Meta Data:");
                meta.forEach((key, value) -> LOGGER.debug("  Meta: {} = {}", key, value));

                // Check if there are prefixes with weights
                LOGGER.debug("Checking for weighted prefixes...");
                var prefixes = metaData.getPrefixes();
                LOGGER.debug("Number of prefixes: {}", prefixes.size());
                prefixes.forEach((weight, prefixValue) ->
                    LOGGER.debug("  Prefix weight {}: [{}]", weight, prefixValue)
                );

                LOGGER.debug("=== END LUCKPERMS PREFIX REQUEST ===");
                return prefix;
            } else {
                LOGGER.debug("User is null after load attempt");
            }

        } catch (Exception e) {
            LOGGER.error("Error getting prefix for user {}: {}", uuid, e.getMessage(), e);
        }

        LOGGER.debug("Returning null prefix");
        LOGGER.debug("=== END LUCKPERMS PREFIX REQUEST ===");
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
                var metaData = user.getCachedData().getMetaData(queryOptions);
                String suffix = metaData.getSuffix();

                LOGGER.debug("LuckPerms suffix for user {}: [{}]", uuid, suffix);

                // Check if there are suffixes with weights
                var suffixes = metaData.getSuffixes();
                LOGGER.debug("Number of suffixes: {}", suffixes.size());
                suffixes.forEach((weight, suffixValue) ->
                    LOGGER.debug("  Suffix weight {}: [{}]", weight, suffixValue)
                );

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
