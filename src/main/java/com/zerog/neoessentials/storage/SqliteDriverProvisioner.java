package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;

/**
 * Loads the sqlite-jdbc driver on demand, instead of it being permanently bundled in the mod
 * jar via JarJar.
 * <p>
 * Why: sqlite-jdbc bundles a native (JNI) library, so JarJar embeds it as its own separate
 * module ({@code org.xerial.sqlitejdbc}). If any OTHER mod on the server also bundles its own
 * copy of sqlite-jdbc without that same isolation (seen in the wild with GriefLogger), Java's
 * module system refuses to boot the ENTIRE server with a split-package
 * {@code ResolutionException} — before any mod, including this one, even loads — regardless
 * of whether {@code storage.type} is actually set to {@code sqlite}. Simply not bundling the
 * driver unless it's genuinely needed means the vast majority of installs (default
 * {@code storage.type: json}) never have the conflicting module in their jar at all.
 * <p>
 * (Relocating/shading the driver's package instead — so a bundled copy could never collide
 * with anyone else's — was tried and doesn't work: sqlite-jdbc's native library has the
 * original {@code org/sqlite/core/NativeDB} class path compiled into it, which shading can't
 * rewrite. See https://github.com/xerial/sqlite-jdbc/issues/145.)
 * <p>
 * On first actual use (SQLite storage backend, or one-time legacy Auction House migration),
 * this downloads the driver from Maven Central into {@code neoessentials/libraries/}, verifies
 * it against a pinned SHA-256 before ever loading it, and loads it through its own
 * {@link URLClassLoader} — no module involved at all, so it can't collide with anyone else's
 * copy the way the old JarJar-embedded module could.
 * <p>
 * Callers must get their {@link Connection} via {@link Driver#connect}
 * on the {@link Driver} instance {@link #ensureDriver()} returns — NOT
 * {@link DriverManager#getConnection(String)}. {@link #ensureDriver()} does also call
 * {@link DriverManager#registerDriver} for compatibility with anything else that might look
 * the driver up conventionally, but {@code DriverManager.getConnection(url)} additionally
 * requires the *caller's* classloader to be able to resolve the driver's class name back to
 * the exact same {@code Class} object (its {@code isDriverAllowed} check) — since this driver
 * is loaded via a {@link URLClassLoader} the caller's (NeoForge mod) classloader can't see
 * into, that check always fails and {@code DriverManager.getConnection} throws "No suitable
 * driver found" even though the driver genuinely is registered and usable.
 */
public final class SqliteDriverProvisioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqliteDriverProvisioner.class);

    private static final String VERSION = "3.45.2.0";
    private static final String DOWNLOAD_URL =
        "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/" + VERSION + "/sqlite-jdbc-" + VERSION + ".jar";
    // Pinned to the exact artifact this project has always shipped (previously via JarJar) —
    // verified locally against the resolved Gradle dependency before pinning here.
    private static final String EXPECTED_SHA256 =
        "a817162384b7d9d98fd616ca880bcbf2528cf29e31393666d2df85b307b03764";

    private static volatile Driver cachedDriver;
    private static volatile boolean attempted = false;

    private SqliteDriverProvisioner() {
    }

    /**
     * Returns a registered sqlite-jdbc {@link Driver}, downloading/loading it first if needed.
     * Only ever attempts once per server run — a failure (network down, checksum mismatch,
     * auto-download disabled) doesn't retry on every subsequent call, so a misconfigured/
     * offline server doesn't hammer Maven Central or spam logs on every storage operation.
     *
     * @return the driver, or {@code null} if unavailable — callers must handle this the same
     *         way they'd handle any other storage-backend failure, not throw/crash.
     */
    public static synchronized Driver ensureDriver() {
        if (cachedDriver != null) return cachedDriver;
        if (attempted) return null;
        attempted = true;

        try {
            File jarFile = ResourceUtil.getDataFile("libraries/sqlite-jdbc-" + VERSION + ".jar");

            if (!isValid(jarFile)) {
                if (!ConfigManager.getInstance().isSqliteAutoDownloadDriverEnabled()) {
                    NeoLog.error(LOGGER, LogCategory.GENERAL,
                        "SQLite driver not found at {} and storage.sqlite.autoDownloadDriver is "
                        + "disabled. Download it yourself from {} and place it at that exact path, "
                        + "or re-enable autoDownloadDriver.", jarFile.getAbsolutePath(), DOWNLOAD_URL);
                    return null;
                }
                NeoLog.info(LOGGER, LogCategory.GENERAL,
                    "SQLite storage is in use but the driver isn't downloaded yet — fetching it "
                    + "from Maven Central ({})...", DOWNLOAD_URL);
                download(jarFile);
                if (!isValid(jarFile)) {
                    NeoLog.error(LOGGER, LogCategory.GENERAL,
                        "Downloaded sqlite-jdbc failed checksum verification — refusing to load it. "
                        + "This may mean Maven Central is serving something unexpected, or the "
                        + "download was corrupted/intercepted. Deleting the file; will not retry "
                        + "this server run.");
                    jarFile.delete();
                    return null;
                }
                NeoLog.info(LOGGER, LogCategory.GENERAL, "SQLite driver downloaded and verified successfully.");
            }

            URLClassLoader loader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()}, SqliteDriverProvisioner.class.getClassLoader());
            Class<?> driverClass = Class.forName("org.sqlite.JDBC", true, loader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(driver);
            cachedDriver = driver;
            return driver;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.GENERAL, "Failed to load the SQLite driver", e);
            return null;
        }
    }

    private static boolean isValid(File file) {
        if (!file.isFile()) return false;
        try {
            return EXPECTED_SHA256.equalsIgnoreCase(sha256(file));
        } catch (IOException e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to hash existing sqlite-jdbc jar, treating as invalid", e);
            return false;
        }
    }

    private static void download(File dest) throws IOException {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent.getAbsolutePath());
        }
        File tmp = new File(parent, dest.getName() + ".downloading");
        try (var in = URI.create(DOWNLOAD_URL).toURL().openStream()) {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    digest.update(buf, 0, n);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JDK — unreachable in practice.
            throw new IOException("SHA-256 unavailable", e);
        }
    }
}
