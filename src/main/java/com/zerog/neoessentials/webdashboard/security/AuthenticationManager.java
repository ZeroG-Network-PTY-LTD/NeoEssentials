package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages user authentication, sessions, and permissions
 */
public class AuthenticationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static AuthenticationManager INSTANCE;

    // Storage paths
    // NOTE: only dashboard_audit.log still lives on disk as a bespoke file — the user
    // account map (dashboard_users.json) now lives in the DataStore (see COLLECTION below).
    private static final Path AUDIT_LOG = Paths.get("neoessentials", "dashboard_audit.log");

    private static final String COLLECTION = "dashboard_users";
    private final com.zerog.neoessentials.storage.DataStore store;

    // In-memory stores
    private final Map<String, User> users = new ConcurrentHashMap<>();
    // Sessions are deliberately NOT persisted — they're short-lived and re-created on
    // login, exactly as before this class was migrated onto DataStore.
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userIdByUsername = new ConcurrentHashMap<>();

    // Security settings
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    private static final int MIN_PASSWORD_LENGTH = 8;

    private AuthenticationManager() {
        this.store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();
        migrateLegacyFileIfNeeded();
        loadUsers();
        startSessionCleanupTask();
    }

    public static AuthenticationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AuthenticationManager();
        }
        return INSTANCE;
    }
    
    /**
     * Authenticate user with username and password
     */
    public Session authenticate(String username, String password, String ipAddress, String userAgent) {
        // Find user
        String userId = userIdByUsername.get(username.toLowerCase());
        if (userId == null) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication failed for '{}': user not found", username);
            logAuditEvent("LOGIN_FAILED", username, ipAddress, "User not found");
            return null;
        }

        User user = users.get(userId);
        if (user == null) {
            return null;
        }

        // Everything below reads-then-writes user.failedLoginAttempts/lockoutUntil — the
        // dashboard's HttpServer dispatches concurrent requests, and every request for this
        // username shares the same User instance (from the ConcurrentHashMap), so this whole
        // check-then-act sequence must be serialized per-user or parallel login attempts can
        // lose increments and never trip the lockout (classic lost-update TOCTOU race).
        synchronized (user) {
            // Check if account is locked
            if (user.isLockedOut()) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication blocked for '{}': account locked", username);
                logAuditEvent("LOGIN_BLOCKED", username, ipAddress, "Account locked due to failed attempts");
                return null;
            }

            // Check if account is enabled
            if (!user.isEnabled()) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication blocked for '{}': account disabled", username);
                logAuditEvent("LOGIN_BLOCKED", username, ipAddress, "Account disabled");
                return null;
            }

            // Verify password
            if (!verifyPassword(password, user.getPasswordHash())) {
                // Increment failed attempts
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

                // Lock account if max attempts reached
                if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                    user.setLockoutUntil(System.currentTimeMillis() + LOCKOUT_DURATION_MS);
                    logAuditEvent("ACCOUNT_LOCKED", username, ipAddress,
                        "Account locked due to " + MAX_FAILED_ATTEMPTS + " failed attempts");
                }

                saveUsers();
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication failed for '{}': invalid password (attempt {}/{})",
                    username, user.getFailedLoginAttempts(), MAX_FAILED_ATTEMPTS);
                logAuditEvent("LOGIN_FAILED", username, ipAddress, "Invalid password");
                return null;
            }

            // Successful login - reset failed attempts
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(0);
            user.setLastLoginAt(System.currentTimeMillis());
            user.setLastLoginIp(ipAddress);
            saveUsers();
        }

        // Create session
        Session session = new Session(user.getId(), user.getUsername(), user.getRole(), ipAddress, userAgent);

        // Check if user needs to change password (temp password or explicitly flagged)
        if (user.requiresPasswordChange() || user.isTempPassword()) {
            session.setRequiresPasswordChange(true);
            logAuditEvent("LOGIN_SUCCESS", username, ipAddress,
                "Session created with password change requirement: " + session.getSessionId());
        } else {
            logAuditEvent("LOGIN_SUCCESS", username, ipAddress, "Session created: " + session.getSessionId());
        }

        sessions.put(session.getSessionId(), session);
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Authentication succeeded for '{}', session created (requiresPasswordChange={})",
            username, session.requiresPasswordChange());

        return session;
    }
    
    /**
     * Create a session for a user (for external authentication like Discord)
     */
    public Session createSession(String userId, String ipAddress, String userAgent) {
        User user = users.get(userId);
        if (user == null) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Cannot create session: user not found with ID {}", userId);
            return null;
        }

        // Update user login info
        user.setLastLoginAt(System.currentTimeMillis());
        user.setLastLoginIp(ipAddress);
        saveUsers();

        // Create session
        Session session = new Session(user.getId(), user.getUsername(), user.getRole(), ipAddress, userAgent);
        sessions.put(session.getSessionId(), session);

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "External auth session created for '{}'", user.getUsername());
        logAuditEvent("LOGIN_SUCCESS", user.getUsername(), ipAddress, "External auth session created: " + session.getSessionId());

        return session;
    }
    
    /**
     * Validate session token
     */
    public Session validateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "validateSession: sessionId is null or empty");
            return null;
        }
        Session session = sessions.get(sessionId);
        if (session == null) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "validateSession: session not found or already expired");
            return null;
        }
        if (!session.isValid()) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "validateSession: session found for '{}' but is no longer valid", session.getUsername());
            return null;
        }
        // Update access time
        session.updateAccessTime();
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "validateSession: session valid for user '{}', requiresPasswordChange={}",
            session.getUsername(), session.requiresPasswordChange());
        return session;
    }
    
    /**
     * Logout user and invalidate session
     */
    public void logout(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.invalidate();
            sessions.remove(sessionId);
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Session invalidated for user '{}'", session.getUsername());
            logAuditEvent("LOGOUT", session.getUsername(), session.getIpAddress(),
                "Session invalidated: " + sessionId);
        }
    }
    
    /**
     * Create new user account
     */
    public User createUser(String username, String password, String email, User.Role role) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        
        // Check if username already exists
        if (userIdByUsername.containsKey(username.toLowerCase())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        String passwordHash = hashPassword(password);
        User user = new User(username, passwordHash);
        user.setEmail(email);
        user.setRole(role != null ? role : User.Role.VIEWER);
        
        users.put(user.getId(), user);
        userIdByUsername.put(username.toLowerCase(), user.getId());
        saveUsers();

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Dashboard user '{}' created with role {}", username, user.getRole());
        logAuditEvent("USER_CREATED", username, "system", "Role: " + user.getRole());
        DashboardUserSyncWebhook.notify("user_created", user);

        return user;
    }
    
    /**
     * Update user password
     */
    public void updatePassword(String userId, String newPassword) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        String passwordHash = hashPassword(newPassword);
        user.setPasswordHash(passwordHash);
        user.setRequiresPasswordChange(false);
        user.setTempPassword(false);
        saveUsers();

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Password updated for user '{}'", user.getUsername());
        logAuditEvent("PASSWORD_CHANGED", user.getUsername(), "system", "Password updated and flags cleared");
    }
    
    /**
     * Update user role — a manual/admin-initiated change. Clears {@code roleSyncManaged} so the
     * permission-driven role-sync task ({@link PermissionRoleSyncTask}) never overwrites a role
     * an admin deliberately chose.
     */
    public void updateUserRole(String userId, User.Role newRole) {
        updateUserRole(userId, newRole, false);
    }

    /**
     * @param syncManaged true if this change comes from {@link PermissionRoleSyncTask} rather
     *                    than a manual admin action — marks the row so the task knows it's safe
     *                    to later downgrade without clobbering a deliberate manual override.
     */
    public void updateUserRole(String userId, User.Role newRole, boolean syncManaged) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        User.Role oldRole = user.getRole();
        user.setRole(newRole);
        user.setRoleSyncManaged(syncManaged);
        saveUsers();

        logAuditEvent("ROLE_CHANGED", user.getUsername(), syncManaged ? "permission-sync" : "system",
            "Role changed from " + oldRole + " to " + newRole);
        DashboardUserSyncWebhook.notify("user_updated", user);
    }
    
    /**
     * Links a Minecraft account to a dashboard user — called by {@link MinecraftAccountLinkManager}
     * once its code-based verification succeeds. Same webhook-notify pattern as
     * {@link #updateUserRole}, so the paired external dashboard picks up the link for free.
     */
    public void setMinecraftLink(String userId, String mcUuid, String mcUsername) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setMcUuid(mcUuid);
        user.setMcUsername(mcUsername);
        saveUsers();

        logAuditEvent("MC_ACCOUNT_LINKED", user.getUsername(), "system", "Linked to Minecraft account " + mcUsername);
        DashboardUserSyncWebhook.notify("user_updated", user);
    }

    /** Self-service unlink — no code/verification needed to remove an existing link. */
    public void clearMinecraftLink(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setMcUuid(null);
        user.setMcUsername(null);
        saveUsers();

        logAuditEvent("MC_ACCOUNT_UNLINKED", user.getUsername(), "system", "Minecraft account link removed");
        DashboardUserSyncWebhook.notify("user_updated", user);
    }

    /** Finds the dashboard user (if any) already linked to a given Minecraft UUID. */
    public User getUserByMcUuid(String mcUuid) {
        return users.values().stream()
            .filter(u -> mcUuid.equalsIgnoreCase(u.getMcUuid()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Enable/disable user account
     */
    public void setUserEnabled(String userId, boolean enabled) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        user.setEnabled(enabled);
        saveUsers();

        logAuditEvent(enabled ? "USER_ENABLED" : "USER_DISABLED",
            user.getUsername(), "system", "Account " + (enabled ? "enabled" : "disabled"));
        DashboardUserSyncWebhook.notify("user_updated", user);
    }
    
    /**
     * Delete user account
     */
    public void deleteUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        users.remove(userId);
        userIdByUsername.remove(user.getUsername().toLowerCase());
        saveUsers();
        
        // Invalidate all sessions for this user
        sessions.values().stream()
            .filter(s -> s.getUserId().equals(userId))
            .forEach(Session::invalidate);
        
        logAuditEvent("USER_DELETED", user.getUsername(), "system", "Account deleted");
        DashboardUserSyncWebhook.notify("user_deleted", user);
    }
    
    /**
     * Get user by ID
     */
    public User getUser(String userId) {
        return users.get(userId);
    }
    
    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        String userId = userIdByUsername.get(username.toLowerCase());
        return userId != null ? users.get(userId) : null;
    }
    
    /**
     * Get all users
     */
    public Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    /**
     * Get all active sessions
     */
    public Collection<Session> getActiveSessions() {
        return sessions.values().stream()
            .filter(Session::isValid)
            .collect(Collectors.toList());
    }
    
    /**
     * Generate temporary password for a user
     * Requires user to change password on first login
     */
    public String generateTempPassword(String username) {
        User user = getUserByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        
        // Generate random 12-character password
        String tempPassword = generateRandomPassword(12);
        String passwordHash = hashPassword(tempPassword);
        
        // Update user with temp password and set flags
        user.setPasswordHash(passwordHash);
        user.setTempPassword(true);
        user.setRequiresPasswordChange(true);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(0);
        
        saveUsers();
        logAuditEvent("TEMP_PASSWORD_GENERATED", username, "system", "Temporary password created");
        
        return tempPassword;
    }
    
    /**
     * A random password meeting this class's own minimum-length policy, for callers that need
     * to satisfy {@link #createUser}'s validation without a human ever actually typing/using
     * it — e.g. an account whose real login surface is an external system, not this mod's own
     * {@code /api/auth/login}. Not persisted or hashed by this method; the caller passes it
     * straight to {@code createUser}.
     */
    public String generateSecureRandomPassword() {
        return generateRandomPassword(24);
    }

    /**
     * Generate random password with letters, numbers, and special characters
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
    
    /**
     * Check if user has permission
     */
    public boolean hasPermission(String sessionId, String permission) {
        Session session = validateSession(sessionId);
        if (session == null) {
            return false;
        }

        User user = users.get(session.getUserId());
        if (user == null) {
            return false;
        }

        return user.hasPermission(permission);
    }
    
    // PBKDF2 parameters for password hashing — 120k iterations is a reasonable modern default
    // (OWASP recommends 600k for PBKDF2-SHA256 as of 2023, but this runs on the same thread
    // as game-server request handling, so a lower cost factor avoids adding a noticeable
    // per-login stall; still ~1000x more expensive to brute-force than the old unsalted SHA-256).
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    /**
     * Hash a password with a fresh random salt using PBKDF2WithHmacSHA256.
     * Stored format: {@code <hex salt>:<hex hash>}.
     *
     * <p>Previously this was a single unsalted SHA-256 digest of the password — trivially
     * crackable via rainbow tables/GPU brute force if {@code dashboard_users.json} ever leaked.
     * See {@link #verifyPassword(String, String)} for the backward-compatible check against
     * accounts hashed before this fix.</p>
     */
    public String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password, salt);
        return bytesToHex(salt) + ":" + bytesToHex(hash);
    }

    /**
     * Verifies {@code password} against a stored hash, supporting both the current salted
     * PBKDF2 format ({@code salt:hash}) and the legacy unsalted-SHA-256 format (a bare 64-char
     * hex digest, from accounts created before this fix). Legacy accounts keep working but
     * should be migrated by changing their password, which re-hashes with the new scheme.
     */
    public boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null) return false;
        int sep = storedHash.indexOf(':');
        if (sep < 0) {
            // Legacy unsalted SHA-256 — constant-time compare to avoid a timing side-channel.
            return java.security.MessageDigest.isEqual(
                legacySha256(password).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
        }
        try {
            byte[] salt = hexToBytes(storedHash.substring(0, sep));
            byte[] expected = hexToBytes(storedHash.substring(sep + 1));
            byte[] actual = pbkdf2(password, salt);
            return java.security.MessageDigest.isEqual(actual, expected);
        } catch (Exception e) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to verify password hash: {}", e.getMessage());
            return false;
        }
    }

    private byte[] pbkdf2(String password, byte[] salt) {
        try {
            var spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS);
            var factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    private String legacySha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * Load users from the active {@link com.zerog.neoessentials.storage.DataStore}.
     */
    private void loadUsers() {
        if (!store.hasAnyData(COLLECTION)) {
            // A fixed default password ("admin123") would let anyone who can reach the
            // dashboard HTTP port log in as admin immediately, before the operator ever gets
            // a chance to change it. Generate a random one instead and require it be changed
            // on first login (same requiresPasswordChange/isTempPassword mechanism already
            // used for admin-issued password resets elsewhere in this class).
            String tempPassword = generateRandomPassword(12);
            User admin = createUser("admin", tempPassword, "admin@localhost", User.Role.ADMIN);
            admin.setTempPassword(true);
            admin.setRequiresPasswordChange(true);
            saveUsers();
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "=================================================================");
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Created default dashboard admin account — username: admin");
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Temporary password: {}", tempPassword);
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "You will be required to set a new password on first login.");
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "=================================================================");
            return;
        }

        for (JsonObject userJson : store.getAll(COLLECTION).values()) {
            String id = userJson.get("id").getAsString();
            String username = userJson.get("username").getAsString();
            String passwordHash = userJson.get("passwordHash").getAsString();
            String email = userJson.has("email") && !userJson.get("email").isJsonNull() ? userJson.get("email").getAsString() : null;
            User.Role role = User.Role.valueOf(userJson.get("role").getAsString());
            boolean enabled = userJson.get("enabled").getAsBoolean();
            long createdAt = userJson.get("createdAt").getAsLong();

            Set<String> permissions = new HashSet<>();
            if (userJson.has("permissions")) {
                JsonArray permsArray = userJson.getAsJsonArray("permissions");
                permsArray.forEach(p -> permissions.add(p.getAsString()));
            }

            User user = new User(id, username, passwordHash, email, role, enabled, createdAt, permissions);

            // Load password change flags
            if (userJson.has("requiresPasswordChange")) {
                user.setRequiresPasswordChange(userJson.get("requiresPasswordChange").getAsBoolean());
            }
            if (userJson.has("isTempPassword")) {
                user.setTempPassword(userJson.get("isTempPassword").getAsBoolean());
            }
            if (userJson.has("roleSyncManaged")) {
                user.setRoleSyncManaged(userJson.get("roleSyncManaged").getAsBoolean());
            }
            if (userJson.has("mcUuid") && !userJson.get("mcUuid").isJsonNull()) {
                user.setMcUuid(userJson.get("mcUuid").getAsString());
            }
            if (userJson.has("mcUsername") && !userJson.get("mcUsername").isJsonNull()) {
                user.setMcUsername(userJson.get("mcUsername").getAsString());
            }
            // Login history / lockout state — previously never persisted, so it
            // silently reset to "never logged in" / "not locked out" on every
            // server restart instead of surviving across reboots.
            if (userJson.has("lastLoginAt")) {
                user.setLastLoginAt(userJson.get("lastLoginAt").getAsLong());
            }
            if (userJson.has("lastLoginIp") && !userJson.get("lastLoginIp").isJsonNull()) {
                user.setLastLoginIp(userJson.get("lastLoginIp").getAsString());
            }
            if (userJson.has("failedLoginAttempts")) {
                user.setFailedLoginAttempts(userJson.get("failedLoginAttempts").getAsInt());
            }
            if (userJson.has("lockoutUntil")) {
                user.setLockoutUntil(userJson.get("lockoutUntil").getAsLong());
            }

            users.put(id, user);
            userIdByUsername.put(username.toLowerCase(), id);
        }

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Loaded {} users from storage", users.size());
    }

    /**
     * Persist the full user-account map into the DataStore, one record per user (id ->
     * JsonObject). Called after every mutation, same as the old bulk file rewrite — just
     * routed through {@code store.put(...)} per user instead of one big JSON file write.
     */
    public void saveUsers() {
        for (User user : users.values()) {
            store.put(COLLECTION, user.getId(), userToJson(user));
        }
    }

    /** Manual field-by-field JsonObject build — mirrors the pre-migration file format exactly. */
    private JsonObject userToJson(User user) {
        JsonObject userJson = new JsonObject();
        userJson.addProperty("id", user.getId());
        userJson.addProperty("username", user.getUsername());
        userJson.addProperty("passwordHash", user.getPasswordHash());
        userJson.addProperty("email", user.getEmail());
        userJson.addProperty("role", user.getRole().name());
        userJson.addProperty("enabled", user.isEnabled());
        userJson.addProperty("createdAt", user.getCreatedAt());
        userJson.addProperty("requiresPasswordChange", user.requiresPasswordChange());
        userJson.addProperty("isTempPassword", user.isTempPassword());
        userJson.addProperty("roleSyncManaged", user.isRoleSyncManaged());
        userJson.addProperty("mcUuid", user.getMcUuid());
        userJson.addProperty("mcUsername", user.getMcUsername());
        userJson.addProperty("lastLoginAt", user.getLastLoginAt());
        userJson.addProperty("lastLoginIp", user.getLastLoginIp());
        userJson.addProperty("failedLoginAttempts", user.getFailedLoginAttempts());
        userJson.addProperty("lockoutUntil", user.getLockoutUntil());

        JsonArray permsArray = new JsonArray();
        user.getPermissions().forEach(permsArray::add);
        userJson.add("permissions", permsArray);

        return userJson;
    }

    /**
     * One-time import of the legacy dashboard_users.json into the active DataStore, if
     * it's still empty and storage.autoMigrate is enabled. Every field — including
     * passwordHash, the salt embedded within it ("salt:hash"), lockout state, and login
     * history (lastLoginAt/lastLoginIp) — is copied verbatim; the legacy file is already
     * the same shape produced by {@link #userToJson(User)}, so records pass through
     * untouched rather than being re-parsed/re-serialized field by field.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File file = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR, "dashboard_users.json");
        if (!file.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null && root.has("users")) {
                for (JsonElement element : root.getAsJsonArray("users")) {
                    JsonObject userJson = element.getAsJsonObject().deepCopy();
                    if (!userJson.has("id")) continue;
                    store.put(COLLECTION, userJson.get("id").getAsString(), userJson);
                    migrated++;
                }
            }
        } catch (IOException e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to migrate legacy dashboard_users.json", e);
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "AuthenticationManager: migrated {} user account(s) from legacy file into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }
    
    /**
     * Log audit event
     */
    private void logAuditEvent(String eventType, String username, String ipAddress, String details) {
        try {
            Path parent = AUDIT_LOG.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String logEntry = String.format("[%s] %s | User: %s | IP: %s | %s%n", 
                timestamp, eventType, username, ipAddress, details);
            
            Files.writeString(AUDIT_LOG, logEntry, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "AUDIT: {}", logEntry.trim());
        } catch (IOException e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to write audit log", e);
        }
    }
    
    /**
     * Start background task to clean up expired sessions
     */
    private void startSessionCleanupTask() {
        Timer timer = new Timer("SessionCleanup", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int removed = 0;
                Iterator<Map.Entry<String, Session>> iterator = sessions.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Session> entry = iterator.next();
                    if (!entry.getValue().isValid()) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Cleaned up {} expired sessions", removed);
                }
            }
        }, 60000, 60000); // Run every minute
    }
}
