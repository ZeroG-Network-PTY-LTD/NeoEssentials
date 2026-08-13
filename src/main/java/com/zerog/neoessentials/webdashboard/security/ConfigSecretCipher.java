package com.zerog.neoessentials.webdashboard.security;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

/**
 * Reversible AES-256-GCM encryption for secrets the mod must store at rest AND read back later
 * (e.g. the dashboard-pairing Bearer token) — distinct from {@link ApiKeyManager}/
 * {@link AuthenticationManager}'s one-way PBKDF2 hashing, which is for secrets that never need
 * to be recovered.
 *
 * <p>The master key is a random 256-bit value generated on first use and persisted to its own
 * file ({@code config/neoessentials/dashboard.key}) — deliberately outside {@code config.json}
 * so it's never accidentally shared/synced/committed alongside the rest of the config, the same
 * separation Laravel's {@code APP_KEY} keeps from its own config files. Losing this file makes
 * any previously-encrypted value undecryptable — that's expected; affected fields fall back to
 * being treated as plaintext once (see {@code ConfigManager}'s decrypt-or-plaintext handling).
 */
public final class ConfigSecretCipher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSecretCipher.class);
    private static final String KEY_FILE_NAME = "dashboard.key";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32; // 256-bit
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static volatile SecretKeySpec cachedKey;

    private ConfigSecretCipher() {}

    public static synchronized String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        try {
            SecretKeySpec key = loadOrCreateKey();
            byte[] iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv).put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to encrypt dashboard secret — storing nothing rather than plaintext", e);
            return "";
        }
    }

    /**
     * Decrypts a value previously produced by {@link #encrypt(String)}. Returns {@code null} if
     * {@code stored} isn't validly encrypted (e.g. it's leftover plaintext from before this
     * feature existed) so the caller can fall back to treating it as plaintext once.
     */
    public static synchronized String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) return stored;
        try {
            SecretKeySpec key = loadOrCreateKey();
            byte[] raw = Base64.getDecoder().decode(stored);
            if (raw.length <= GCM_IV_BYTES) return null;

            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] ciphertext = new byte[raw.length - GCM_IV_BYTES];
            System.arraycopy(raw, 0, iv, 0, GCM_IV_BYTES);
            System.arraycopy(raw, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Not a value we encrypted (bad base64, wrong tag, etc.) — let the caller treat it
            // as plaintext instead of hard-failing.
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Stored value did not decrypt as a dashboard secret — treating as plaintext", e);
            return null;
        }
    }

    private static SecretKeySpec loadOrCreateKey() throws IOException {
        if (cachedKey != null) return cachedKey;

        Path keyPath = Path.of(ResourceUtil.CONFIG_DIR, KEY_FILE_NAME);
        ResourceUtil.ensureDirectoryExists(ResourceUtil.CONFIG_DIR);

        if (Files.exists(keyPath)) {
            byte[] keyBytes = Base64.getDecoder().decode(Files.readString(keyPath).trim());
            cachedKey = new SecretKeySpec(keyBytes, "AES");
            return cachedKey;
        }

        byte[] keyBytes = new byte[KEY_LENGTH_BYTES];
        new SecureRandom().nextBytes(keyBytes);
        Files.writeString(keyPath, Base64.getEncoder().encodeToString(keyBytes));
        restrictToOwnerOnly(keyPath);

        NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Generated a new dashboard secret-encryption key at {} — back this up or " +
            "existing encrypted config values (e.g. the paired dashboard's token) become unreadable.", keyPath);

        cachedKey = new SecretKeySpec(keyBytes, "AES");
        return cachedKey;
    }

    private static void restrictToOwnerOnly(Path path) {
        try {
            PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (posix != null) {
                Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(path, perms);
            }
            // No portable equivalent on Windows via NIO without JNA/ACL APIs — filesystem ACLs
            // there are left at the OS default (which for a non-shared config dir already
            // restricts access to the machine's own users).
        } catch (Exception e) {
            // Best-effort only — never block key creation on permission tightening.
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Could not restrict dashboard key file permissions", e);
        }
    }
}
