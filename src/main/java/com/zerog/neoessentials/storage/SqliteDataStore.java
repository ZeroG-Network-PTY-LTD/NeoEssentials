package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Single-file embedded database backend — one SQLite file
 * ({@code neoessentials/store/data.db}), one table per collection, schema
 * {@code (id TEXT PRIMARY KEY, data TEXT NOT NULL)} storing each record's JSON blob.
 *
 * <p>All access goes through a single shared {@link Connection} and is synchronized —
 * SQLite only supports one writer at a time regardless, and the mod's write volume
 * (moderation actions, etc.) is far below where that would become a bottleneck.
 */
public class SqliteDataStore implements DataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqliteDataStore.class);
    private static final Gson GSON = new Gson();

    private final File dbFile;
    // Not final: if the very first connection attempt fails (see openConnection()'s
    // Javadoc for why that can legitimately happen at mod-construction time), later
    // callers retry via connection() instead of being permanently stuck with a null
    // connection for the rest of the server's life.
    private volatile Connection connection;
    // Collections whose table we've already confirmed/created, so we don't run
    // CREATE TABLE IF NOT EXISTS on every single call.
    private final Set<String> knownTables = new CopyOnWriteArraySet<>();

    public SqliteDataStore(File dbFile) {
        this.dbFile = dbFile;
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.error("Failed to create storage directory: {}", parent.getAbsolutePath());
        }
        this.connection = openConnection();
    }

    /**
     * Opens the SQLite connection, tolerating failure — returns {@code null} rather than
     * throwing. Callers must retry via {@link #connection()} rather than trusting this
     * result forever.
     *
     * <p>This can genuinely fail the very first time it's called: {@code StorageManager}
     * (and therefore this class) is constructed as a side effect of the first manager
     * singleton's {@code getInstance()} call, which for some managers happens as early as
     * the mod's {@code @Mod} constructor — before NeoForge/JarJar has necessarily finished
     * making the bundled {@code sqlite-jdbc} dependency's classes visible to this mod's
     * classloader. By the time any *real* data operation happens (well after mod
     * construction), that race has always resolved — so {@link #connection()} retrying
     * on next use, instead of this class staying permanently broken, is the actual fix.</p>
     */
    private Connection openConnection() {
        try {
            // Loads (downloading on first use if needed) the driver — see
            // SqliteDriverProvisioner for why this isn't just a JarJar-bundled dependency
            // anymore. Connects through the Driver instance directly, NOT
            // DriverManager.getConnection(url): DriverManager only considers a registered
            // driver "visible" to a caller if Class.forName(driver's class name, true,
            // callerClassLoader) resolves to that same Class object — since this driver is
            // loaded via its own URLClassLoader that NeoForge's mod classloader can't see
            // into, DriverManager would silently skip it and throw "No suitable driver
            // found" even though it's registered. Calling connect() on the Driver instance
            // we already hold sidesteps that classloader-visibility check entirely.
            java.sql.Driver driver = SqliteDriverProvisioner.ensureDriver();
            if (driver == null) {
                LOGGER.error("SqliteDataStore: SQLite driver unavailable, cannot open {}", dbFile.getAbsolutePath());
                return null;
            }
            Connection conn = driver.connect("jdbc:sqlite:" + dbFile.getAbsolutePath(), new java.util.Properties());
            LOGGER.info("SqliteDataStore: opened {}", dbFile.getAbsolutePath());
            return conn;
        } catch (Exception e) {
            LOGGER.error("SqliteDataStore: failed to open {} (will retry on next use)",
                dbFile.getAbsolutePath(), e);
            return null;
        }
    }

    /** Returns the live connection, retrying {@link #openConnection()} if a previous attempt failed. */
    private synchronized Connection connection() {
        if (connection == null) {
            connection = openConnection();
        }
        return connection;
    }

    @Override
    public synchronized void put(String collection, String id, JsonObject data) {
        Connection conn = connection();
        if (conn == null) return;
        ensureTable(collection);
        String sql = "INSERT INTO " + tableName(collection) + " (id, data) VALUES (?, ?) "
            + "ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, GSON.toJson(data));
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("SqliteDataStore: failed to put {}/{}", collection, id, e);
        }
    }

    @Override
    public synchronized JsonObject get(String collection, String id) {
        Connection conn = connection();
        if (conn == null) return null;
        ensureTable(collection);
        String sql = "SELECT data FROM " + tableName(collection) + " WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return GSON.fromJson(rs.getString("data"), JsonObject.class);
            }
        } catch (SQLException e) {
            LOGGER.error("SqliteDataStore: failed to get {}/{}", collection, id, e);
        }
        return null;
    }

    @Override
    public synchronized boolean delete(String collection, String id) {
        Connection conn = connection();
        if (conn == null) return false;
        ensureTable(collection);
        String sql = "DELETE FROM " + tableName(collection) + " WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("SqliteDataStore: failed to delete {}/{}", collection, id, e);
            return false;
        }
    }

    @Override
    public synchronized Map<String, JsonObject> getAll(String collection) {
        Map<String, JsonObject> results = new LinkedHashMap<>();
        Connection conn = connection();
        if (conn == null) return results;
        ensureTable(collection);
        String sql = "SELECT id, data FROM " + tableName(collection);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.put(rs.getString("id"), GSON.fromJson(rs.getString("data"), JsonObject.class));
            }
        } catch (SQLException e) {
            LOGGER.error("SqliteDataStore: failed to read collection '{}'", collection, e);
        }
        return results;
    }

    @Override
    public boolean hasAnyData(String collection) {
        return !getAll(collection).isEmpty();
    }

    @Override
    public void close() {
        // Deliberately reads the raw field, not connection() — closing should never trigger
        // a fresh open just to immediately close it again.
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException e) { LOGGER.warn("SqliteDataStore: error closing connection", e); }
        }
    }

    private synchronized void ensureTable(String collection) {
        if (knownTables.contains(collection)) return;
        Connection conn = connection();
        if (conn == null) return;
        String table = tableName(collection);
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (id TEXT PRIMARY KEY, data TEXT NOT NULL)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            knownTables.add(collection);
        } catch (SQLException e) {
            LOGGER.error("SqliteDataStore: failed to create table for collection '{}'", collection, e);
        }
    }

    /** Collection names come from our own code (not user input), but sanitize defensively anyway. */
    private String tableName(String collection) {
        String safe = collection.replaceAll("[^a-zA-Z0-9_]", "_");
        return "ne_" + safe;
    }
}
