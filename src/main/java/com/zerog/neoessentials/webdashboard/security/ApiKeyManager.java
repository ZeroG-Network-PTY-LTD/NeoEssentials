package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages long-lived API keys for server-to-server integrations (the external dashboard,
 * or any other trusted backend) — a separate credential space from the human-facing
 * username/password sessions in {@link AuthenticationManager}.
 *
 * <p>A key never expires on its own and isn't tied to a dashboard user account; it's created
 * once (in-game, via {@code /apikey create}, or through {@code /api/apikeys} by an already
 * logged-in admin) and handed to whatever external service needs to control this server. It
 * can be revoked independently of anything else at any time.
 *
 * <p>Key format: {@code neo_<keyId>_<secret>}. {@code keyId} is public (stored in plaintext,
 * used as the map lookup key — same role a username plays); {@code secret} is never stored,
 * only a salted PBKDF2 hash of it, exactly like {@link AuthenticationManager}'s password
 * hashing. The full token is shown to the admin exactly once, at creation time.
 */
public class ApiKeyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyManager.class);
    private static final String COLLECTION = "api_keys";
    private static final String TOKEN_PREFIX = "neo_";
    private static final int KEY_ID_BYTES = 9;   // -> 12 base64url chars
    private static final int SECRET_BYTES = 24;  // -> 32 base64url chars
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private static ApiKeyManager INSTANCE;

    private final DataStore store;
    private final Map<String, ApiKeyRecord> keys = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public static synchronized ApiKeyManager getInstance() {
        if (INSTANCE == null) INSTANCE = new ApiKeyManager();
        return INSTANCE;
    }

    private ApiKeyManager() {
        this.store = StorageManager.getInstance().getStore();
        loadKeys();
    }

    public static class ApiKeyRecord {
        public final String id;
        public String label;
        public String secretHash;
        public User.Role role;
        public final long createdAt;
        public long lastUsedAt;
        public boolean enabled;

        ApiKeyRecord(String id, String label, String secretHash, User.Role role, long createdAt, long lastUsedAt, boolean enabled) {
            this.id = id;
            this.label = label;
            this.secretHash = secretHash;
            this.role = role;
            this.createdAt = createdAt;
            this.lastUsedAt = lastUsedAt;
            this.enabled = enabled;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("label", label);
            json.addProperty("role", role.name());
            json.addProperty("createdAt", createdAt);
            json.addProperty("lastUsedAt", lastUsedAt);
            json.addProperty("enabled", enabled);
            // secretHash deliberately omitted — never leaves the server, not even to an admin session.
            return json;
        }
    }

    /**
     * Creates a new API key and returns the full token (visible this one time only — only the
     * hash is persisted, so it cannot be recovered later; losing it means revoking and
     * re-creating).
     */
    public synchronized String createKey(String label, User.Role role) {
        String keyId = randomToken(KEY_ID_BYTES);
        String secret = randomToken(SECRET_BYTES);
        String secretHash = hashSecret(secret);

        ApiKeyRecord record = new ApiKeyRecord(keyId, label, secretHash, role, System.currentTimeMillis(), 0, true);
        keys.put(keyId, record);
        saveKey(record);

        LOGGER.info("API key created: '{}' (id: {}, role: {})", label, keyId, role);
        return TOKEN_PREFIX + keyId + "_" + secret;
    }

    /**
     * Validates a full API key token, updating its last-used timestamp on success.
     * @return the matching record, or null if the token is malformed, unknown, disabled, or wrong.
     */
    public ApiKeyRecord validate(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) return null;

        String rest = token.substring(TOKEN_PREFIX.length());
        // Fixed-width keyId slice, not indexOf('_') — base64url's alphabet includes '_', so the
        // first underscore in `rest` isn't reliably the keyId/secret separator. KEY_ID_BYTES (9)
        // always base64url-encodes to exactly 12 chars (9*8 bits / 6 bits-per-char, no padding).
        int keyIdLength = (KEY_ID_BYTES * 8) / 6;
        if (rest.length() <= keyIdLength + 1 || rest.charAt(keyIdLength) != '_') return null;

        String keyId = rest.substring(0, keyIdLength);
        String secret = rest.substring(keyIdLength + 1);

        ApiKeyRecord record = keys.get(keyId);
        if (record == null || !record.enabled) return null;
        if (!verifySecret(secret, record.secretHash)) return null;

        record.lastUsedAt = System.currentTimeMillis();
        saveKey(record); // best-effort persistence of lastUsedAt; validate() is called on every request so this is cheap enough

        return record;
    }

    public List<ApiKeyRecord> getAllKeys() {
        return new ArrayList<>(keys.values());
    }

    /**
     * Extracts the public {@code keyId} portion from a full token string (e.g. so a caller that
     * just minted a key via {@link #createKey} can remember its id for a later {@link #revoke}
     * without having to separately track the return value of some other call). Returns null if
     * the token doesn't match the expected {@code neo_<keyId>_<secret>} shape.
     */
    public static String extractKeyId(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) return null;
        String rest = token.substring(TOKEN_PREFIX.length());
        int keyIdLength = (KEY_ID_BYTES * 8) / 6;
        if (rest.length() <= keyIdLength + 1 || rest.charAt(keyIdLength) != '_') return null;
        return rest.substring(0, keyIdLength);
    }

    public boolean revoke(String keyId) {
        ApiKeyRecord record = keys.remove(keyId);
        if (record == null) return false;
        store.delete(COLLECTION, keyId);
        LOGGER.info("API key revoked: '{}' (id: {})", record.label, keyId);
        return true;
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    private void loadKeys() {
        for (JsonObject json : store.getAll(COLLECTION).values()) {
            try {
                String id = json.get("id").getAsString();
                ApiKeyRecord record = new ApiKeyRecord(
                    id,
                    json.get("label").getAsString(),
                    json.get("secretHash").getAsString(),
                    User.Role.valueOf(json.get("role").getAsString()),
                    json.get("createdAt").getAsLong(),
                    json.has("lastUsedAt") ? json.get("lastUsedAt").getAsLong() : 0,
                    !json.has("enabled") || json.get("enabled").getAsBoolean()
                );
                keys.put(id, record);
            } catch (Exception e) {
                LOGGER.warn("Skipping corrupt API key record: {}", e.getMessage());
            }
        }
        LOGGER.info("Loaded {} API key(s)", keys.size());
    }

    private void saveKey(ApiKeyRecord record) {
        JsonObject json = new JsonObject();
        json.addProperty("id", record.id);
        json.addProperty("label", record.label);
        json.addProperty("secretHash", record.secretHash);
        json.addProperty("role", record.role.name());
        json.addProperty("createdAt", record.createdAt);
        json.addProperty("lastUsedAt", record.lastUsedAt);
        json.addProperty("enabled", record.enabled);
        store.put(COLLECTION, record.id, json);
    }

    // ── Crypto (same PBKDF2 scheme as AuthenticationManager's password hashing) ────────────

    private String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String hashSecret(String secret) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        byte[] hash = pbkdf2(secret, salt);
        return bytesToHex(salt) + ":" + bytesToHex(hash);
    }

    private boolean verifySecret(String secret, String storedHash) {
        int sep = storedHash.indexOf(':');
        if (sep < 0) return false;
        try {
            byte[] salt = hexToBytes(storedHash.substring(0, sep));
            byte[] expected = hexToBytes(storedHash.substring(sep + 1));
            byte[] actual = pbkdf2(secret, salt);
            return java.security.MessageDigest.isEqual(actual, expected);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] pbkdf2(String secret, byte[] salt) {
        try {
            var spec = new javax.crypto.spec.PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS);
            var factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 hashing failed", e);
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
}
