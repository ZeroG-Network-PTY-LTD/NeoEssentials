package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DashboardRegistrationManager INSTANCE;

    // Storage path for registered accounts
    private static final Path REGISTRATIONS_FILE = Paths.get("neoessentials", "dashboard_registrations.json");

    // In-memory store of registered accounts
    // Key: Minecraft UUID, Value: Registration data
    private final Map<UUID, DashboardAccountRegistration> registrations = new ConcurrentHashMap<>();

    // Temporary registration tokens (for in-game registration flow)
    // Key: Token, Value: MinecraftAccountData
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    private DashboardRegistrationManager() {
        LOGGER.info("Initializing DashboardRegistrationManager...");
        loadRegistrations();
        LOGGER.info("DashboardRegistrationManager initialized with {} existing registration(s)", registrations.size());
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
     * Start registration process for a player
     * Returns a registration token that must be used within 5 minutes
     */
    public String startRegistration(UUID minecraftUuid, String minecraftUsername) {
        // Check if already registered
        if (isRegistered(minecraftUuid)) {
            LOGGER.warn("Player {} already has a registered dashboard account", minecraftUsername);
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

        LOGGER.info("Started dashboard registration for player {}: token={}", minecraftUsername, token);

        return token;
    }

    /**
     * Complete registration with username and password
     */
    public DashboardAccountRegistration completeRegistration(String token, String dashboardUsername, String password) {
        // Get pending registration
        PendingRegistration pending = pendingRegistrations.get(token);
        if (pending == null) {
            LOGGER.warn("Invalid or expired registration token: {}", token);
            return null;
        }

        // Check expiry
        if (System.currentTimeMillis() > pending.getExpiresAt()) {
            pendingRegistrations.remove(token);
            LOGGER.warn("Registration token expired for player: {}", pending.getMinecraftUsername());
            return null;
        }

        // Check if username is already taken
        if (getRegistrationByUsername(dashboardUsername) != null) {
            LOGGER.warn("Dashboard username already taken: {}", dashboardUsername);
            return null;
        }

        // Validate password strength
        if (password == null || password.length() < 8) {
            LOGGER.warn("Password too weak for dashboard registration: {}", pending.getMinecraftUsername());
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

        saveRegistrations();

        // Create user account in AuthenticationManager
        try {
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            User.Role role = determineRole(pending.getMinecraftUuid());
            authManager.createUser(dashboardUsername, password, null, role);

            LOGGER.info("Completed dashboard registration for {} (Minecraft: {})",
                dashboardUsername, pending.getMinecraftUsername());

            return registration;
        } catch (Exception e) {
            LOGGER.error("Failed to create user account for registration: {}", e.getMessage(), e);
            // Rollback registration
            registrations.remove(pending.getMinecraftUuid());
            saveRegistrations();
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
            LOGGER.warn("No pending registration found for UUID: {}", playerUuid);
            return null;
        }

        // Use the existing complete registration method
        return completeRegistration(pending.getToken(), dashboardUsername, password);
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

        saveRegistrations();

        LOGGER.info("Linked Discord account {} to dashboard user {}",
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

        saveRegistrations();

        LOGGER.info("Unlinked Discord account from dashboard user {}",
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
     * Load registrations from file
     */
    private void loadRegistrations() {
        if (!Files.exists(REGISTRATIONS_FILE)) {
            LOGGER.info("No existing dashboard registrations file found");
            return;
        }

        try {
            String content = Files.readString(REGISTRATIONS_FILE, StandardCharsets.UTF_8);
            JsonObject data = GSON.fromJson(content, JsonObject.class);

            if (data.has("registrations")) {
                data.getAsJsonArray("registrations").forEach(element -> {
                    JsonObject regObj = element.getAsJsonObject();
                    DashboardAccountRegistration reg = DashboardAccountRegistration.fromJson(regObj);
                    if (reg != null) {
                        registrations.put(reg.getMinecraftUuid(), reg);
                    }
                });
            }

            LOGGER.info("Loaded {} dashboard registrations", registrations.size());

        } catch (IOException e) {
            LOGGER.error("Failed to load dashboard registrations: {}", e.getMessage(), e);
        }
    }

    /**
     * Save registrations to file
     */
    private void saveRegistrations() {
        try {
            // Ensure directory exists
            Files.createDirectories(REGISTRATIONS_FILE.getParent());

            JsonObject data = new JsonObject();
            data.addProperty("version", 1);
            data.addProperty("lastUpdated", System.currentTimeMillis());

            com.google.gson.JsonArray regsArray = new com.google.gson.JsonArray();
            registrations.values().forEach(reg -> regsArray.add(reg.toJson()));
            data.add("registrations", regsArray);

            String json = GSON.toJson(data);
            Files.writeString(REGISTRATIONS_FILE, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            LOGGER.error("Failed to save dashboard registrations: {}", e.getMessage(), e);
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
