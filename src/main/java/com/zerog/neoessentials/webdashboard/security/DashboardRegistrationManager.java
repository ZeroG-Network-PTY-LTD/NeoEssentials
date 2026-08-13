package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dashboard account registrations
 * Allows users to register accounts in-game with permission validation
 * Supports both standalone and Discord-linked registrations
 */
public class DashboardRegistrationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardRegistrationManager.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static DashboardRegistrationManager INSTANCE;

    private static final String COLLECTION = "dashboard_registrations";
    private final com.zerog.neoessentials.storage.DataStore store;

    // In-memory store of registered accounts
    // Key: Minecraft UUID, Value: Registration data
    private final Map<UUID, DashboardAccountRegistration> registrations = new ConcurrentHashMap<>();

    // Temporary registration tokens (for in-game registration flow). These are never
    // persisted — they expire after 5 minutes (see startRegistration()), so surviving a
    // restart would be pointless, and this matches the pre-DataStore behavior where they
    // only ever lived in this in-memory map, never written to dashboard_registrations.json.
    // Key: Token, Value: MinecraftAccountData
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    private DashboardRegistrationManager() {
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Initializing DashboardRegistrationManager...");
        this.store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();
        migrateLegacyFileIfNeeded();
        loadRegistrations();
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "DashboardRegistrationManager initialized with {} existing registration(s)", registrations.size());
    }

    public static DashboardRegistrationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DashboardRegistrationManager();
        }
        return INSTANCE;
    }

    /**
     * Check if a player has registered a dashboard account
     */
    public boolean isRegistered(UUID minecraftUuid) {
        return registrations.containsKey(minecraftUuid);
    }

    /**
     * Get registration for a Minecraft UUID
     */
    public DashboardAccountRegistration getRegistration(UUID minecraftUuid) {
        return registrations.get(minecraftUuid);
    }

    /**
     * Get registration by dashboard username
     */
    public DashboardAccountRegistration getRegistrationByUsername(String username) {
        return registrations.values().stream()
            .filter(reg -> reg.getDashboardUsername().equalsIgnoreCase(username))
            .findFirst()
            .orElse(null);
    }

    /**
     * All linked registrations — used by {@link PermissionRoleSyncTask} to know which players
     * actually have a dashboard account to keep in sync (it never auto-creates one).
     */
    public java.util.Collection<DashboardAccountRegistration> getAllRegistrations() {
        return java.util.List.copyOf(registrations.values());
    }

    /**
     * Start registration process for a player
     * Returns a registration token that must be used within 5 minutes
     */
    public String startRegistration(UUID minecraftUuid, String minecraftUsername) {
        // Check if already registered
        if (isRegistered(minecraftUuid)) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Player {} already has a registered dashboard account", minecraftUsername);
            return null;
        }

        // Generate secure token
        String token = generateToken();

        // Create pending registration
        PendingRegistration pending = new PendingRegistration(
            token,
            minecraftUuid,
            minecraftUsername,
            System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes expiry
        );

        pendingRegistrations.put(token, pending);

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Started dashboard registration for player {}", minecraftUsername);

        return token;
    }

    /**
     * Complete registration with username and password
     */
    public DashboardAccountRegistration completeRegistration(String token, String dashboardUsername, String password) {
        // Get pending registration
        PendingRegistration pending = pendingRegistrations.get(token);
        if (pending == null) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Invalid or expired registration token used");
            return null;
        }

        // Check expiry
        if (System.currentTimeMillis() > pending.getExpiresAt()) {
            pendingRegistrations.remove(token);
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Registration token expired for player: {}", pending.getMinecraftUsername());
            return null;
        }

        // Check if username is already taken
        if (getRegistrationByUsername(dashboardUsername) != null) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard username already taken: {}", dashboardUsername);
            return null;
        }

        // Validate password strength
        if (password == null || password.length() < 8) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Password too weak for dashboard registration: {}", pending.getMinecraftUsername());
            return null;
        }

        // Create registration
        DashboardAccountRegistration registration = new DashboardAccountRegistration(
            pending.getMinecraftUuid(),
            pending.getMinecraftUsername(),
            dashboardUsername,
            hashPassword(password),
            System.currentTimeMillis()
        );

        registrations.put(pending.getMinecraftUuid(), registration);
        pendingRegistrations.remove(token);

        saveRegistration(registration);

        // Create user account in AuthenticationManager
        try {
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            User.Role role = determineRole(pending.getMinecraftUuid());
            authManager.createUser(dashboardUsername, password, null, role);

            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Completed dashboard registration for {} (Minecraft: {})",
                dashboardUsername, pending.getMinecraftUsername());

            return registration;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to create user account for registration", e);
            // Rollback registration
            registrations.remove(pending.getMinecraftUuid());
            store.delete(COLLECTION, pending.getMinecraftUuid().toString());
            return null;
        }
    }

    /**
     * Complete registration by UUID (for in-game command use)
     * Looks up pending registration by player UUID instead of requiring token
     */
    public DashboardAccountRegistration completeRegistrationByUuid(UUID playerUuid, String dashboardUsername, String password) {
        // Find pending registration for this player
        PendingRegistration pending = pendingRegistrations.values().stream()
            .filter(p -> p.getMinecraftUuid().equals(playerUuid))
            .findFirst()
            .orElse(null);

        if (pending == null) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "No pending registration found for UUID: {}", playerUuid);
            return null;
        }

        // Use the existing complete registration method
        return completeRegistration(pending.getToken(), dashboardUsername, password);
    }

    /**
     * Register a dashboard account directly via Discord (SDLink-linked account).
     * Uses the player's Minecraft username as the dashboard username.
     * A random unusable password is set — the player must log in via Discord OAuth2.
     *
     * @return the new registration, or null on failure
     */
    public DashboardAccountRegistration registerWithDiscord(UUID minecraftUuid, String minecraftUsername,
                                                            String discordId, String discordUsername) {
        if (isRegistered(minecraftUuid)) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Player {} already has a registered dashboard account", minecraftUsername);
            return null;
        }

        // Use Minecraft username as the dashboard username (matches OAuth2 flow)
        String dashboardUsername = minecraftUsername;

        // If the Minecraft username is already taken as a dashboard username by a different MC account, append _mc
        if (getRegistrationByUsername(dashboardUsername) != null) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard username '{}' already taken during Discord registration for {}", dashboardUsername, minecraftUsername);
            return null;
        }

        // Random unusable password — login is exclusively via Discord OAuth2
        String unusablePassword = UUID.randomUUID().toString() + UUID.randomUUID();

        DashboardAccountRegistration registration = new DashboardAccountRegistration(
            minecraftUuid,
            minecraftUsername,
            dashboardUsername,
            hashPassword(unusablePassword),
            System.currentTimeMillis()
        );

        // Attach Discord link immediately
        registration.setDiscordId(discordId);
        registration.setDiscordUsername(discordUsername);
        registration.setDiscordLinkedAt(System.currentTimeMillis());

        registrations.put(minecraftUuid, registration);
        saveRegistration(registration);

        // Create the user account in AuthenticationManager (with the random unusable password)
        try {
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            User.Role role = determineRole(minecraftUuid);
            authManager.createUser(dashboardUsername, unusablePassword, discordId + "@discord.oauth", role);

            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Discord-registered dashboard account for {} (MC: {}, Discord: {})",
                dashboardUsername, minecraftUsername, discordUsername);

            return registration;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to create user account for Discord registration", e);
            // Rollback
            registrations.remove(minecraftUuid);
            store.delete(COLLECTION, minecraftUuid.toString());
            return null;
        }
    }

    /**
     * Link a Discord account to an existing registration
     */
    public boolean linkDiscordAccount(UUID minecraftUuid, String discordId, String discordUsername) {
        DashboardAccountRegistration registration = registrations.get(minecraftUuid);
        if (registration == null) {
            return false;
        }

        registration.setDiscordId(discordId);
        registration.setDiscordUsername(discordUsername);
        registration.setDiscordLinkedAt(System.currentTimeMillis());

        saveRegistration(registration);

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Linked Discord account {} to dashboard user {}",
            discordUsername, registration.getDashboardUsername());

        return true;
    }

    /**
     * Unlink Discord account from registration
     */
    public boolean unlinkDiscordAccount(UUID minecraftUuid) {
        DashboardAccountRegistration registration = registrations.get(minecraftUuid);
        if (registration == null) {
            return false;
        }

        registration.setDiscordId(null);
        registration.setDiscordUsername(null);
        registration.setDiscordLinkedAt(0);

        saveRegistration(registration);

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Unlinked Discord account from dashboard user {}",
            registration.getDashboardUsername());

        return true;
    }

    /**
     * Determine user role based on Minecraft permissions
     */
    private User.Role determineRole(UUID minecraftUuid) {
        // Check permissions
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                minecraftUuid, "neoessentials.dashboard.admin")) {
            return User.Role.ADMIN;
        } else if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                minecraftUuid, "neoessentials.dashboard.moderator")) {
            return User.Role.MODERATOR;
        } else if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                minecraftUuid, "neoessentials.dashboard.manage")) {
            return User.Role.OPERATOR;
        }

        return User.Role.VIEWER;
    }

    /**
     * Generate secure random token
     */
    private String generateToken() {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to regular SecureRandom
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
    }

    /**
     * Hash password using SHA-256
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Load registrations from the active {@link com.zerog.neoessentials.storage.DataStore}.
     */
    private void loadRegistrations() {
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            DashboardAccountRegistration reg = DashboardAccountRegistration.fromJson(obj);
            if (reg != null) {
                registrations.put(reg.getMinecraftUuid(), reg);
            }
        }
        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Loaded {} dashboard registrations", registrations.size());
    }

    /**
     * Persist a single registration record, keyed by Minecraft UUID.
     */
    private void saveRegistration(DashboardAccountRegistration registration) {
        store.put(COLLECTION, registration.getMinecraftUuid().toString(), registration.toJson());
    }

    /**
     * One-time import of the legacy dashboard_registrations.json into the active
     * DataStore, if it's still empty and storage.autoMigrate is enabled. The old file's
     * "registrations" array entries are already the same shape produced by
     * {@link DashboardAccountRegistration#toJson()}, so each element is stored verbatim,
     * keyed by minecraftUuid.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File file = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR, "dashboard_registrations.json");
        if (!file.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null && root.has("registrations")) {
                for (JsonElement element : root.getAsJsonArray("registrations")) {
                    JsonObject regObj = element.getAsJsonObject().deepCopy();
                    if (!regObj.has("minecraftUuid")) continue;
                    store.put(COLLECTION, regObj.get("minecraftUuid").getAsString(), regObj);
                    migrated++;
                }
            }
        } catch (IOException e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to migrate legacy dashboard_registrations.json", e);
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "DashboardRegistrationManager: migrated {} registration(s) from legacy file into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }

    /**
     * Clean up expired pending registrations
     */
    public void cleanupExpiredPending() {
        long now = System.currentTimeMillis();
        pendingRegistrations.entrySet().removeIf(entry -> now > entry.getValue().getExpiresAt());
    }

    /**
     * Represents a pending registration (in-progress)
     */
    private static class PendingRegistration {
        private final String token;
        private final UUID minecraftUuid;
        private final String minecraftUsername;
        private final long expiresAt;

        public PendingRegistration(String token, UUID minecraftUuid, String minecraftUsername, long expiresAt) {
            this.token = token;
            this.minecraftUuid = minecraftUuid;
            this.minecraftUsername = minecraftUsername;
            this.expiresAt = expiresAt;
        }

        public String getToken() { return token; }
        public UUID getMinecraftUuid() { return minecraftUuid; }
        public String getMinecraftUsername() { return minecraftUsername; }
        public long getExpiresAt() { return expiresAt; }
    }
}
