package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Networked backend — a shared MySQL/MariaDB database, one table per collection
 * (same {@code (id VARCHAR PRIMARY KEY, data TEXT)} shape as {@link SqliteDataStore}).
 * This is the backend that actually enables multi-server shared data: point every
 * Minecraft server in a network at the same database and they all see the same
 * bans/mutes/economy/etc. in real time, matching how ban-management plugins like
 * BanManager use MySQL for their WebUI/network-wide moderation.
 *
 * <p>Uses a small HikariCP connection pool rather than one shared {@code Connection} —
 * unlike SQLite, MySQL genuinely supports concurrent connections, and a pool avoids
 * serializing every database-backed manager's reads/writes through a single socket.
 */
public class MySqlDataStore implements DataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlDataStore.class);
    private static final Gson GSON = new Gson();

    private final HikariDataSource dataSource;
    private final Set<String> knownTables = new CopyOnWriteArraySet<>();

    public MySqlDataStore(String host, int port, String database, String username, String password,
                          boolean useSSL, int poolSize) {
        HikariDataSource ds = null;
        Driver driver = MySqlDriverProvisioner.ensureDriver();
        if (driver == null) {
            LOGGER.error("MySqlDataStore: MySQL driver unavailable — see the preceding log entry for why "
                + "(download failed, checksum mismatch, or autoDownloadDriver disabled).");
            this.dataSource = null;
            return;
        }
        try {
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&characterEncoding=utf8";
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);

            HikariConfig config = new HikariConfig();
            // Routed through the Driver instance MySqlDriverProvisioner loaded via its own
            // URLClassLoader (see that class's javadoc) instead of HikariConfig.setJdbcUrl(...) —
            // Hikari's own driverClassName resolution ultimately falls back to
            // DriverManager.getConnection(url), which requires the CALLER's classloader (this mod's)
            // to resolve the driver class back to the same Class object; since the driver lives in
            // an isolated URLClassLoader that check always fails with "No suitable driver found".
            // Calling driver.connect(...) directly here sidesteps that check entirely.
            config.setDataSource(new DriverBackedDataSource(driver, jdbcUrl, props));
            config.setMaximumPoolSize(Math.max(1, poolSize));
            config.setPoolName("NeoEssentials-MySQL");
            // Fail fast at startup rather than hanging the server boot if the DB is unreachable —
            // callers (StorageManager) are expected to catch this and fall back to JSON.
            config.setConnectionTimeout(5000);
            config.setInitializationFailTimeout(5000);
            ds = new HikariDataSource(config);
            NeoLog.info(LOGGER, LogCategory.GENERAL, "MySqlDataStore: connected to {}:{}/{}", host, port, database);
        } catch (Exception e) {
            LOGGER.error("MySqlDataStore: failed to connect to {}:{}/{}", host, port, database, e);
            if (ds != null) ds.close();
            ds = null;
        }
        this.dataSource = ds;
    }

    /**
     * Minimal {@link DataSource} wrapping a specific {@link Driver} instance's {@code connect()} —
     * see the comment in the constructor above for why this is needed instead of
     * {@code HikariConfig.setJdbcUrl(...)}.
     */
    private static final class DriverBackedDataSource implements DataSource {
        private final Driver driver;
        private final String jdbcUrl;
        private final Properties props;

        DriverBackedDataSource(Driver driver, String jdbcUrl, Properties props) {
            this.driver = driver;
            this.jdbcUrl = jdbcUrl;
            this.props = props;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return driver.connect(jdbcUrl, props);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties p = new Properties(props);
            p.setProperty("user", username);
            p.setProperty("password", password);
            return driver.connect(jdbcUrl, p);
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) return iface.cast(this);
            throw new SQLException("Not a wrapper for " + iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public void put(String collection, String id, JsonObject data) {
        if (dataSource == null) return;
        ensureTable(collection);
        String sql = "INSERT INTO " + tableName(collection) + " (id, data) VALUES (?, ?) "
            + "ON DUPLICATE KEY UPDATE data = VALUES(data)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, GSON.toJson(data));
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("MySqlDataStore: failed to put {}/{}", collection, id, e);
        }
    }

    @Override
    public JsonObject get(String collection, String id) {
        if (dataSource == null) return null;
        ensureTable(collection);
        String sql = "SELECT data FROM " + tableName(collection) + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return GSON.fromJson(rs.getString("data"), JsonObject.class);
            }
        } catch (SQLException e) {
            LOGGER.error("MySqlDataStore: failed to get {}/{}", collection, id, e);
        }
        return null;
    }

    @Override
    public boolean delete(String collection, String id) {
        if (dataSource == null) return false;
        ensureTable(collection);
        String sql = "DELETE FROM " + tableName(collection) + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("MySqlDataStore: failed to delete {}/{}", collection, id, e);
            return false;
        }
    }

    @Override
    public Map<String, JsonObject> getAll(String collection) {
        Map<String, JsonObject> results = new LinkedHashMap<>();
        if (dataSource == null) return results;
        ensureTable(collection);
        String sql = "SELECT id, data FROM " + tableName(collection);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.put(rs.getString("id"), GSON.fromJson(rs.getString("data"), JsonObject.class));
            }
        } catch (SQLException e) {
            LOGGER.error("MySqlDataStore: failed to read collection '{}'", collection, e);
        }
        return results;
    }

    @Override
    public boolean hasAnyData(String collection) {
        return !getAll(collection).isEmpty();
    }

    @Override
    public void close() {
        if (dataSource != null) dataSource.close();
    }

    private void ensureTable(String collection) {
        if (knownTables.contains(collection)) return;
        String table = tableName(collection);
        String sql = "CREATE TABLE IF NOT EXISTS " + table
            + " (id VARCHAR(191) PRIMARY KEY, data LONGTEXT NOT NULL)";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            knownTables.add(collection);
        } catch (SQLException e) {
            LOGGER.error("MySqlDataStore: failed to create table for collection '{}'", collection, e);
        }
    }

    /** Collection names come from our own code (not user input), but sanitize defensively anyway. */
    private String tableName(String collection) {
        String safe = collection.replaceAll("[^a-zA-Z0-9_]", "_");
        return "ne_" + safe;
    }
}
