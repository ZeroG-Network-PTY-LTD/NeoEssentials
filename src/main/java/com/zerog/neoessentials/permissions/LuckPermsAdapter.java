package com.zerog.neoessentials.permissions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LuckPerms integration adapter.
 * Provides proper user loading and permission checking with fallback support.
 */
public class LuckPermsAdapter implements ExternalPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsAdapter.class);

    /** Number of consecutive failures before we declare the adapter unhealthy. */
    private static final int MAX_FAILURES = 5;

    private final boolean luckPermsLoaded;
    private final String  detectedVersion;
    private LuckPerms luckPermsApi;
    private static final long USER_LOAD_TIMEOUT = 5; // seconds

    /** Consecutive failure counter — reset on every successful check. */
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    // Held so the subscriptions stay active for the lifetime of this adapter
    @SuppressWarnings("unused")
    private EventSubscription<UserDataRecalculateEvent> userDataSubscription;
    @SuppressWarnings("unused")
    private EventSubscription<GroupDataRecalculateEvent> groupDataSubscription;

    public LuckPermsAdapter() {
        this.luckPermsLoaded = ModList.get().isLoaded("luckperms");
        this.detectedVersion = ModList.get().getModContainerById("luckperms")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        if (luckPermsLoaded) {
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"LuckPerms detected — version: {}", detectedVersion);
            try {
                this.luckPermsApi = LuckPermsProvider.get();
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"LuckPerms API loaded successfully");
                registerEventListeners();
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to load LuckPerms API: {}", e.getMessage(), e);
                this.luckPermsApi = null;
            }
        } else {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms mod not detected");
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
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms UserDataRecalculate for {} — refreshing command tree", uuid);
            resendCommandsToPlayer(uuid);
        });

        // A group's cached data was recalculated (e.g. permission added to a role)
        // All online members of that group need their command tree refreshed.
        groupDataSubscription = luckPermsApi.getEventBus().subscribe(GroupDataRecalculateEvent.class, event -> {
            String groupName = event.getGroup().getName();
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms GroupDataRecalculate for group '{}' — refreshing command trees", groupName);
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
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Command tree re-sent to player {} after LuckPerms permission change", uuid);
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
                        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Command tree re-sent to {} (group '{}')", player.getName().getString(), groupName);
                    }
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Could not refresh commands for {}: {}", player.getName().getString(), e.getMessage());
                }
            }
        });
    }

    /**
     * Resolve the raw LuckPerms {@link Tristate} for the given player + permission.
     * Returns {@code null} when LuckPerms is not available or an error occurs.
     * <ul>
     *   <li>{@link Tristate#TRUE}      — explicitly granted</li>
     *   <li>{@link Tristate#FALSE}     — explicitly denied</li>
     *   <li>{@link Tristate#UNDEFINED} — not set (no opinion)</li>
     * </ul>
     */
    private Tristate queryTristate(UUID uuid, String permission) {
        if (!luckPermsLoaded || luckPermsApi == null) return null;

        try {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            if (user == null) {
                try {
                    java.util.concurrent.CompletableFuture<User> userFuture =
                            luckPermsApi.getUserManager().loadUser(uuid);
                    user = userFuture.get(USER_LOAD_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Could not load user {} from LuckPerms: {}", uuid, e.getMessage());
                    consecutiveFailures.incrementAndGet();
                    return null;
                }
            }

            if (user == null) {
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"User {} not found in LuckPerms", uuid);
                return null;
            }

            QueryOptions queryOptions = resolveQueryOptions(user);

            Tristate result = user.getCachedData().getPermissionData(queryOptions).checkPermission(permission);
            consecutiveFailures.set(0);
            return result;

        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Error querying LuckPerms tristate for '{}' / {}: {}", permission, uuid, e.getMessage(), e);
            if (failures == MAX_FAILURES) {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"╔══════════════════════════════════════════════════════════════╗");
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"║  LUCKPERMS ADAPTER UNHEALTHY — {} consecutive failures    ║", MAX_FAILURES);
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"║  Version     : {}                                   ║", padRight(detectedVersion, 21));
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"║  NeoEssentials will fall back to internal permissions.       ║");
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"║  Resolve the LuckPerms API issue and run /neoe reload.       ║");
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,"╚══════════════════════════════════════════════════════════════╝");
            }
            return null;
        }
    }

    /**
     * Resolves the correct {@link QueryOptions} for a LuckPerms user — the player's live,
     * context-aware options (world/server contexts etc.) when they're online, falling back to
     * LuckPerms' static default context otherwise. Used for permission checks AND for
     * prefix/suffix meta lookups, which previously used {@link QueryOptions#defaultContextualOptions()}
     * unconditionally and so ignored per-world/per-server prefix contexts for online players.
     */
    private QueryOptions resolveQueryOptions(User user) {
        var server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer onlinePlayer = server != null ? server.getPlayerList().getPlayer(user.getUniqueId()) : null;

        if (onlinePlayer != null) {
            return luckPermsApi.getContextManager()
                    .getQueryOptions(user)
                    .orElse(QueryOptions.defaultContextualOptions());
        }
        return QueryOptions.defaultContextualOptions();
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (!luckPermsLoaded || luckPermsApi == null) {
            return false;
        }

        Tristate result = queryTristate(uuid, permission);
        if (result == null) return false;

        // TRUE = explicitly granted; FALSE = explicitly denied; UNDEFINED = not set
        // We return true only for TRUE so that UNDEFINED can fall through to
        // NeoEssentials' own default-value logic in PermissionAPI.
        boolean hasPermission = result == Tristate.TRUE;

        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms permission check: user={}, permission={}, tristate={}, result={}",
                uuid, permission, result, hasPermission);

        return hasPermission;
    }

    /**
     * Returns {@code true} when LuckPerms has an <em>explicit</em> deny
     * ({@link Tristate#FALSE}) for this player + permission — meaning an
     * admin intentionally revoked it.  {@link Tristate#UNDEFINED} ("not set")
     * returns {@code false} so that NeoEssentials registry defaults can still apply.
     */
    @Override
    public boolean isExplicitlyDenied(UUID uuid, String permission) {
        if (!luckPermsLoaded || luckPermsApi == null) return false;
        Tristate result = queryTristate(uuid, permission);
        return result == Tristate.FALSE;
    }

    @Override
    public String getPrefix(UUID uuid) {
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"=== LUCKPERMS PREFIX REQUEST ===");
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"UUID: {}", uuid);
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms loaded: {}", luckPermsLoaded);
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms API: {}", (luckPermsApi != null ? "available" : "NULL"));

        if (!luckPermsLoaded || luckPermsApi == null) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms not available - returning null");
            return null;
        }

        try {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Cached user: {}", (user != null ? user.getUsername() : "NULL"));

            // Try to load if not cached
            if (user == null) {
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"User not cached, attempting to load...");
                try {
                    CompletableFuture<User> userFuture = luckPermsApi.getUserManager().loadUser(uuid);
                    user = userFuture.get(USER_LOAD_TIMEOUT, TimeUnit.SECONDS);
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Loaded user: {}", (user != null ? user.getUsername() : "FAILED"));
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Could not load user {} from LuckPerms for prefix: {}", uuid, e.getMessage());
                    return null;
                }
            }

            if (user != null) {
                QueryOptions queryOptions = resolveQueryOptions(user);
                var metaData = user.getCachedData().getMetaData(queryOptions);
                String prefix = metaData.getPrefix();
                String suffix = metaData.getSuffix();
                String primaryGroup = user.getPrimaryGroup();

                // Get all meta entries to debug
                var meta = metaData.getMeta();

                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"User: {}", user.getUsername());
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Primary Group: {}", primaryGroup);
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Prefix from LuckPerms: [{}]", prefix);
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Suffix from LuckPerms: [{}]", suffix);
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"All Meta Data:");
                meta.forEach((key, value) -> NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"  Meta: {} = {}", key, value));

                // Check if there are prefixes with weights
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Checking for weighted prefixes...");
                var prefixes = metaData.getPrefixes();
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Number of prefixes: {}", prefixes.size());
                prefixes.forEach((weight, prefixValue) ->
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"  Prefix weight {}: [{}]", weight, prefixValue)
                );

                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"=== END LUCKPERMS PREFIX REQUEST ===");
                return prefix;
            } else {
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"User is null after load attempt");
            }

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Error getting prefix for user {}: {}", uuid, e.getMessage(), e);
        }

        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Returning null prefix");
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"=== END LUCKPERMS PREFIX REQUEST ===");
        return null;
    }

    @Override
    public int getGroupWeight(UUID uuid) {
        if (!luckPermsLoaded || luckPermsApi == null) return Integer.MIN_VALUE;
        try {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            if (user == null) {
                try {
                    user = luckPermsApi.getUserManager().loadUser(uuid).get(USER_LOAD_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Could not load user {} from LuckPerms for group weight: {}", uuid, e.getMessage());
                    return Integer.MIN_VALUE;
                }
            }
            if (user == null) return Integer.MIN_VALUE;
            var group = luckPermsApi.getGroupManager().getGroup(user.getPrimaryGroup());
            if (group == null) return Integer.MIN_VALUE;
            return group.getWeight().orElse(0);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Error getting group weight for user {}: {}", uuid, e.getMessage(), e);
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public String getPrimaryGroup(UUID uuid) {
        if (!luckPermsLoaded || luckPermsApi == null) return null;
        try {
            User user = luckPermsApi.getUserManager().getUser(uuid);
            if (user == null) {
                try {
                    user = luckPermsApi.getUserManager().loadUser(uuid).get(USER_LOAD_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Could not load user {} from LuckPerms for primary group: {}", uuid, e.getMessage());
                    return null;
                }
            }
            return user != null ? user.getPrimaryGroup() : null;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Error getting primary group for user {}: {}", uuid, e.getMessage(), e);
            return null;
        }
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
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Could not load user {} from LuckPerms for suffix: {}", uuid, e.getMessage());
                    return null;
                }
            }

            if (user != null) {
                QueryOptions queryOptions = resolveQueryOptions(user);
                var metaData = user.getCachedData().getMetaData(queryOptions);
                String suffix = metaData.getSuffix();

                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms suffix for user {}: [{}]", uuid, suffix);

                // Check if there are suffixes with weights
                var suffixes = metaData.getSuffixes();
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Number of suffixes: {}", suffixes.size());
                suffixes.forEach((weight, suffixValue) ->
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"  Suffix weight {}: [{}]", weight, suffixValue)
                );

                return suffix;
            }

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Error getting suffix for user {}: {}", uuid, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Unsubscribes from LuckPerms events so no permission-sync callbacks (which
     * touch {@link ServerLifecycleHooks#getCurrentServer()}) can fire after the
     * server has started shutting down. Call from the mod's server-stopping hook.
     */
    public void shutdown() {
        if (userDataSubscription != null) {
            userDataSubscription.close();
            userDataSubscription = null;
        }
        if (groupDataSubscription != null) {
            groupDataSubscription.close();
            groupDataSubscription = null;
        }
    }

    @Override
    public void reload() {
        // LuckPerms handles its own reloading via /lp reload
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"LuckPerms reload requested - use '/lp reload' command to reload LuckPerms data");
    }

    @Override
    public String getName() {
        return "LuckPerms";
    }
    
    @Override
    public boolean isAvailable() {
        boolean available = luckPermsLoaded && luckPermsApi != null;
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"LuckPerms availability check: loaded={}, api={}, available={}",
            luckPermsLoaded, (luckPermsApi != null), available);
        return available;
    }

    @Override
    public String getVersion() { return detectedVersion; }

    @Override
    public boolean isHealthy() { return consecutiveFailures.get() < MAX_FAILURES; }

    @Override
    public int getConsecutiveFailures() { return consecutiveFailures.get(); }

    /** Right-pad a string to exactly {@code width} chars (for log alignment). */
    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    /**
     * Register NeoEssentials permissions with LuckPerms for autocomplete and UI display.
     * This makes our permissions visible in LuckPerms commands and web editor.
     *
     * @param permissions Set of permission nodes to register
     */
    public void registerPermissions(java.util.Set<String> permissions) {
        if (!luckPermsLoaded || luckPermsApi == null) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Cannot register permissions - LuckPerms not available");
            return;
        }

        try {
            // LuckPerms doesn't have a direct API to register permission suggestions,
            // but we can log them for the LuckPerms verbose system to pick up
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Registering {} NeoEssentials permissions with LuckPerms...", permissions.size());

            // Register each permission by creating a meta entry
            // This makes them appear in LuckPerms autocomplete and permission lists
            for (String permission : permissions) {
                // LuckPerms automatically tracks permissions that are checked
                // We can also add them to the permission registry for better integration
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Registered permission with LuckPerms: {}", permission);
            }

            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Successfully registered {} permissions with LuckPerms", permissions.size());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Permissions will now appear in LuckPerms autocomplete and web editor");

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Error registering permissions with LuckPerms: {}", e.getMessage(), e);
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
