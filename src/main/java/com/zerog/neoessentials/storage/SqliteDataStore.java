package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
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

    private final Connection connection;
    // Collections whose table we've already confirmed/created, so we don't run
    // CREATE TABLE IF NOT EXISTS on every single call.
    private final Set<String> knownTables = new CopyOnWriteArraySet<>();

    public SqliteDataStore(File dbFile) {
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.error("Failed to create storage directory: {}", parent.getAbsolutePath());
        }
        Connection conn = null;
        try {
            // Force the driver to register — bundled via JarJar, whose isolated
            // classloader doesn't reliably trigger SQLite's own ServiceLoader
            // auto-registration (same workaround AuctionDB already needed).
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            LOGGER.info("SqliteDataStore: opened {}", dbFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("SqliteDataStore: failed to open {}", dbFile.getAbsolutePath(), e);
        }
        this.connection = conn;
    }

    @Override
    public synchronized void put(String collection, String id, JsonObject data) {
        if (connection == null) return;
        ensureTable(collection);
        String sql = "INSERT INTO " + tableName(collection) + " (id, data) VALUES (?, ?) "
            + "ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, GSON.toJson(data));
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("SqliteDataStore: failed to put {}/{}", collection, id, e);
        }
    }

    @Override
    public synchronized JsonObject get(String collection, String id) {
        if (connection == null) return null;
        ensureTable(collection);
        String sql = "SELECT data FROM " + tableName(collection) + " WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        if (connection == null) return false;
        ensureTable(collection);
        String sql = "DELETE FROM " + tableName(collection) + " WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        if (connection == null) return results;
        ensureTable(collection);
        String sql = "SELECT id, data FROM " + tableName(collection);
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException e) { LOGGER.warn("SqliteDataStore: error closing connection", e); }
        }
    }

    private synchronized void ensureTable(String collection) {
        if (knownTables.contains(collection)) return;
        String table = tableName(collection);
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (id TEXT PRIMARY KEY, data TEXT NOT NULL)";
        try (Statement stmt = connection.createStatement()) {
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
