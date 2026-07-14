package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        syncServiceAccountFromConfig();
        startSessionCleanupTask();
    }

    /**
     * Creates (or keeps in sync) a dashboard account from webDashboard.serviceAccount in
     * config.json, for external apps (e.g. the Laravel NeoEssentials-Dashboard) that
     * authenticate against this API server-to-server. Lets the operator set the account's
     * credentials once in config.json instead of provisioning it by hand via the users API —
     * every boot, the account's password/role are pushed to match config if they've drifted.
     */
    private void syncServiceAccountFromConfig() {
        if (!com.zerog.neoessentials.config.ConfigManager.isDashboardServiceAccountEnabled()) {
            return;
        }
        String username = com.zerog.neoessentials.config.ConfigManager.getDashboardServiceAccountUsername();
        String password = com.zerog.neoessentials.config.ConfigManager.getDashboardServiceAccountPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            LOGGER.warn("webDashboard.serviceAccount is enabled but username/password is blank — skipping sync.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            LOGGER.warn("webDashboard.serviceAccount password is shorter than {} characters — skipping sync.", MIN_PASSWORD_LENGTH);
            return;
        }
        User.Role role;
        try {
            role = User.Role.valueOf(com.zerog.neoessentials.config.ConfigManager.getDashboardServiceAccountRole().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("webDashboard.serviceAccount.role '{}' is not a valid role (ADMIN/OPERATOR/MODERATOR/VIEWER) — defaulting to MODERATOR.",
                com.zerog.neoessentials.config.ConfigManager.getDashboardServiceAccountRole());
            role = User.Role.MODERATOR;
        }

        User existing = getUserByUsername(username);
        if (existing == null) {
            User created = createUser(username, password, username + "@service.local", role);
            created.setRequiresPasswordChange(false);
            created.setTempPassword(false);
            saveUsers();
            LOGGER.info("Created dashboard service account '{}' from config (role: {}).", username, role);
            return;
        }

        boolean changed = false;
        if (!hashPassword(password).equals(existing.getPasswordHash())) {
            updatePassword(existing.getId(), password);
            changed = true;
        }
        if (existing.getRole() != role) {
            updateUserRole(existing.getId(), role);
            changed = true;
        }
        if (!existing.isEnabled()) {
            existing.setEnabled(true);
            changed = true;
        }
        if (changed) {
            saveUsers();
            LOGGER.info("Synced dashboard service account '{}' to match config.json (role: {}).", username, role);
        }
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
            logAuditEvent("LOGIN_FAILED", username, ipAddress, "User not found");
            return null;
        }
        
        User user = users.get(userId);
        if (user == null) {
            return null;
        }
        
        // Check if account is locked
        if (user.isLockedOut()) {
            logAuditEvent("LOGIN_BLOCKED", username, ipAddress, "Account locked due to failed attempts");
            return null;
        }
        
        // Check if account is enabled
        if (!user.isEnabled()) {
            logAuditEvent("LOGIN_BLOCKED", username, ipAddress, "Account disabled");
            return null;
        }
        
        // Verify password
        String passwordHash = hashPassword(password);
        if (!passwordHash.equals(user.getPasswordHash())) {
            // Increment failed attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            
            // Lock account if max attempts reached
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockoutUntil(System.currentTimeMillis() + LOCKOUT_DURATION_MS);
                logAuditEvent("ACCOUNT_LOCKED", username, ipAddress, 
                    "Account locked due to " + MAX_FAILED_ATTEMPTS + " failed attempts");
            }
            
            saveUsers();
            logAuditEvent("LOGIN_FAILED", username, ipAddress, "Invalid password");
            return null;
        }
        
        // Successful login - reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(0);
        user.setLastLoginAt(System.currentTimeMillis());
        user.setLastLoginIp(ipAddress);
        saveUsers();
        
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
        
        return session;
    }
    
    /**
     * Create a session for a user (for external authentication like Discord)
     */
    public Session createSession(String userId, String ipAddress, String userAgent) {
        User user = users.get(userId);
        if (user == null) {
            LOGGER.error("Cannot create session: user not found with ID {}", userId);
            return null;
        }
        
        // Update user login info
        user.setLastLoginAt(System.currentTimeMillis());
        user.setLastLoginIp(ipAddress);
        saveUsers();
        
        // Create session
        Session session = new Session(user.getId(), user.getUsername(), user.getRole(), ipAddress, userAgent);
        sessions.put(session.getSessionId(), session);
        
        logAuditEvent("LOGIN_SUCCESS", user.getUsername(), ipAddress, "External auth session created: " + session.getSessionId());
        
        return session;
    }
    
    /**
     * Validate session token
     */
    public Session validateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
                LOGGER.debug("validateSession: sessionId is null or empty");
            }
            return null;
        }
        Session session = sessions.get(sessionId);
        if (session == null) {
            if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
                LOGGER.debug("validateSession: sessionId '{}' not found in sessions map", sessionId);
            }
            return null;
        }
        if (!session.isValid()) {
            if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
                LOGGER.debug("validateSession: sessionId '{}' found but session is not valid", sessionId);
            }
            return null;
        }
        // Update access time
        session.updateAccessTime();
        if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
            LOGGER.debug("validateSession: sessionId '{}' is valid for user '{}', requiresPasswordChange={}",
                sessionId, session.getUsername(), session.requiresPasswordChange());
        }
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
        
        logAuditEvent("USER_CREATED", username, "system", "Role: " + user.getRole());
        
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
        
        logAuditEvent("PASSWORD_CHANGED", user.getUsername(), "system", "Password updated and flags cleared");
    }
    
    /**
     * Update user role
     */
    public void updateUserRole(String userId, User.Role newRole) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        
        User.Role oldRole = user.getRole();
        user.setRole(newRole);
        saveUsers();
        
        logAuditEvent("ROLE_CHANGED", user.getUsername(), "system", 
            "Role changed from " + oldRole + " to " + newRole);
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
    
    /**
     * Hash password with SHA-256
     */
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Load users from the active {@link com.zerog.neoessentials.storage.DataStore}.
     */
    private void loadUsers() {
        if (!store.hasAnyData(COLLECTION)) {
            // Create default admin user with 8+ character password to meet validation requirements
            LOGGER.info("Creating default admin user (username: admin, password: admin123)");
            createUser("admin", "admin123", "admin@localhost", User.Role.ADMIN);
            LOGGER.warn("SECURITY WARNING: Default admin account created with password 'admin123'. Please change it immediately!");
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

        LOGGER.info("Loaded {} users from storage", users.size());
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
            LOGGER.error("Failed to migrate legacy dashboard_users.json: {}", e.getMessage(), e);
        }

        if (migrated > 0) {
            LOGGER.info("AuthenticationManager: migrated {} user account(s) from legacy file into the '{}' storage backend.",
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
            
            LOGGER.info("AUDIT: {}", logEntry.trim());
        } catch (IOException e) {
            LOGGER.error("Failed to write audit log", e);
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
                    LOGGER.debug("Cleaned up {} expired sessions", removed);
                }
            }
        }, 60000, 60000); // Run every minute
    }
}
