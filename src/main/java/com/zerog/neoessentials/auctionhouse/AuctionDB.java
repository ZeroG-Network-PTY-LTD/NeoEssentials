package com.zerog.neoessentials.auctionhouse;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite persistence layer for the NeoEssentials Auction House.
 *
 * <p>Database file: {@code auctionhouse.db} in the server run directory.
 *
 * <p>Tables:
 * <pre>
 *   auctionhouse  (id, playeruuid, owner, nbt, item, count, price, secondsLeft)
 *   expireditems  (id, playeruuid, owner, nbt, item, count, price)
 * </pre>
 */
public final class AuctionDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDB.class);

    /** JDBC URL — relative to the server's working / run directory. */
    private static final String URL = "jdbc:sqlite:auctionhouse.db";

    private static AuctionDB instance;
    private Connection connection;

    // ── Singleton ────────────────────────────────────────────────────────────

    private AuctionDB() {}

    public static AuctionDB getInstance() {
        if (instance == null) instance = new AuctionDB();
        return instance;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        try {
            // Force the SQLite driver to register (bundled via JarJar)
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(URL);
            createTables();
            LOGGER.info("[AuctionHouse] Database opened at {}", URL);
        } catch (Exception e) {
            LOGGER.error("[AuctionHouse] Failed to open database", e);
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("[AuctionHouse] Database connection closed.");
            } catch (SQLException e) {
                LOGGER.error("[AuctionHouse] Error closing database", e);
            }
            connection = null;
        }
    }

    /** Returns the raw JDBC connection (used only by {@code AuctionHouseManager} ticks). */
    public Connection connection() { return connection; }

    // ── Table creation ────────────────────────────────────────────────────────

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS auctionhouse (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        playeruuid  TEXT NOT NULL,
                        owner       TEXT NOT NULL,
                        nbt         TEXT,
                        item        TEXT NOT NULL,
                        count       INTEGER NOT NULL,
                        price       REAL NOT NULL,
                        secondsLeft INTEGER NOT NULL
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS expireditems (
                        id         INTEGER PRIMARY KEY,
                        playeruuid TEXT NOT NULL,
                        owner      TEXT NOT NULL,
                        nbt        TEXT,
                        item       TEXT NOT NULL,
                        count      INTEGER NOT NULL,
                        price      REAL NOT NULL
                    )
                    """);
        }
    }

    // ── Active listings ───────────────────────────────────────────────────────

    /** Load all active listings from the database. */
    public List<AuctionItem> loadAllActive() {
        List<AuctionItem> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT * FROM auctionhouse")) {
            while (rs.next()) {
                list.add(fromActiveRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to load active listings", e);
        }
        return list;
    }

    /**
     * Insert a new listing.
     *
     * @return the auto-assigned database ID, or {@code -1} on failure.
     */
    public int addItem(String playerUuid, String owner, String nbt, String itemKey,
                       int count, double price, long secondsLeft) {
        String sql = "INSERT INTO auctionhouse(playeruuid,owner,nbt,item,count,price,secondsLeft) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, playerUuid);
            ps.setString(2, owner);
            ps.setString(3, nbt);
            ps.setString(4, itemKey);
            ps.setInt   (5, count);
            ps.setDouble(6, price);
            ps.setLong  (7, secondsLeft);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to insert listing", e);
        }
        return -1;
    }

    /** Permanently delete an active listing (e.g., item was sold or cancelled). */
    public void removeActive(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM auctionhouse WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to remove active listing id={}", id, e);
        }
    }

    /** Update the remaining time for an active listing. */
    public void updateTime(int id, long secondsLeft) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE auctionhouse SET secondsLeft=? WHERE id=?")) {
            ps.setLong(1, secondsLeft);
            ps.setInt (2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to update time for id={}", id, e);
        }
    }

    /** Count how many active listings a player currently has. */
    public int countActiveForPlayer(String playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM auctionhouse WHERE playeruuid=?")) {
            ps.setString(1, playerUuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to count listings for {}", playerUuid, e);
        }
        return 0;
    }

    // ── Expired items ─────────────────────────────────────────────────────────

    /** Load all expired items from the database. */
    public List<AuctionItem> loadAllExpired() {
        List<AuctionItem> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT * FROM expireditems")) {
            while (rs.next()) {
                list.add(fromExpiredRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to load expired items", e);
        }
        return list;
    }

    /**
     * Move an active listing to the {@code expireditems} table.
     * The original listing is deleted from {@code auctionhouse}.
     */
    public void expireItem(AuctionItem item) {
        // Remove from active table first
        removeActive(item.getId());
        // Insert into expired table
        String sql = "INSERT OR IGNORE INTO expireditems(id,playeruuid,owner,nbt,item,count,price) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt   (1, item.getId());
            ps.setString(2, item.getUuid());
            ps.setString(3, item.getOwner());
            ps.setString(4, item.getNbt());
            ps.setString(5, item.getItemKey());
            ps.setInt   (6, item.getCount());
            ps.setDouble(7, item.getPrice());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to expire item id={}", item.getId(), e);
        }
    }

    /** Delete an expired item once the player has collected it. */
    public void removeExpired(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM expireditems WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[AuctionHouse] Failed to remove expired item id={}", id, e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static AuctionItem fromActiveRow(ResultSet rs) throws SQLException {
        return new AuctionItem(
                rs.getInt("id"),
                rs.getString("playeruuid"),
                rs.getString("owner"),
                rs.getString("nbt"),
                rs.getString("item"),
                rs.getInt("count"),
                rs.getDouble("price"),
                rs.getLong("secondsLeft"));
    }

    private static AuctionItem fromExpiredRow(ResultSet rs) throws SQLException {
        return new AuctionItem(
                rs.getInt("id"),
                rs.getString("playeruuid"),
                rs.getString("owner"),
                rs.getString("nbt"),
                rs.getString("item"),
                rs.getInt("count"),
                rs.getDouble("price"),
                0L); // expired — no time remaining
    }
}

