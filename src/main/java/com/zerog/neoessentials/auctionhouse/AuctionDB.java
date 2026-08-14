package com.zerog.neoessentials.auctionhouse;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persistence layer for the NeoEssentials Auction House, backed by the pluggable
 * {@link DataStore} (JSON/YAML/SQLite/MySQL — see {@link StorageManager}) instead of its own
 * bespoke SQLite connection, so the Auction House participates in {@code storage.type} the
 * same as every other manager (including true multi-server MySQL sync).
 *
 * <p>Collections:
 * <pre>
 *   auction_listings  (id = listing id, active auction-house listings)
 *   auction_expired    (id = listing id, expired-and-uncollected listings)
 * </pre>
 *
 * <p>Listing ids are no longer SQLite {@code AUTOINCREMENT} values — they're assigned from an
 * in-memory counter seeded from the highest id found across both collections at startup. A
 * listing keeps the SAME id when it moves from {@code auction_listings} to
 * {@code auction_expired} (matching the old schema's behavior of preserving the row id on
 * expiry), so the counter only needs to consider new inserts.
 */
public final class AuctionDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDB.class);

    private static final String ACTIVE_COLLECTION = "auction_listings";
    private static final String EXPIRED_COLLECTION = "auction_expired";

    /** Legacy SQLite database this class used to own directly, only consulted for one-time import. */
    private static final String LEGACY_URL = "jdbc:sqlite:auctionhouse.db";

    private static AuctionDB instance;
    private DataStore store;
    private final AtomicInteger nextId = new AtomicInteger(1);

    // ── Singleton ────────────────────────────────────────────────────────────

    private AuctionDB() {}

    public static AuctionDB getInstance() {
        if (instance == null) instance = new AuctionDB();
        return instance;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        store = StorageManager.getInstance().getStore();
        migrateLegacyDatabaseIfNeeded();

        int maxId = 0;
        for (String id : store.getAll(ACTIVE_COLLECTION).keySet()) {
            maxId = Math.max(maxId, parseIdSafe(id));
        }
        for (String id : store.getAll(EXPIRED_COLLECTION).keySet()) {
            maxId = Math.max(maxId, parseIdSafe(id));
        }
        nextId.set(maxId + 1);

        LOGGER.info("[AuctionHouse] Storage backend ready ('{}').", StorageManager.getInstance().getActiveType());
    }

    public void shutdown() {
        // Nothing to release — every mutation already persists immediately through the shared
        // DataStore, which is closed centrally by StorageManager.shutdown() on server stop.
    }

    // ── Active listings ───────────────────────────────────────────────────────

    /** Load all active listings from storage. */
    public List<AuctionItem> loadAllActive() {
        List<AuctionItem> list = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : store.getAll(ACTIVE_COLLECTION).entrySet()) {
            list.add(fromJson(parseIdSafe(entry.getKey()), entry.getValue(), entry.getValue().has("secondsLeft")
                ? entry.getValue().get("secondsLeft").getAsLong() : 0L));
        }
        return list;
    }

    /**
     * Insert a new listing.
     *
     * @return the assigned listing id, or {@code -1} on failure.
     */
    public synchronized int addItem(String playerUuid, String owner, String nbt, String itemKey,
                       int count, double price, long secondsLeft) {
        try {
            int id = nextId.getAndIncrement();
            JsonObject obj = toJson(playerUuid, owner, nbt, itemKey, count, price, secondsLeft);
            store.put(ACTIVE_COLLECTION, String.valueOf(id), obj);
            return id;
        } catch (Exception e) {
            LOGGER.error("[AuctionHouse] Failed to insert listing", e);
            return -1;
        }
    }

    /** Permanently delete an active listing (e.g., item was sold or cancelled). */
    public void removeActive(int id) {
        store.delete(ACTIVE_COLLECTION, String.valueOf(id));
    }

    /** Update the remaining time for an active listing. */
    public void updateTime(int id, long secondsLeft) {
        JsonObject obj = store.get(ACTIVE_COLLECTION, String.valueOf(id));
        if (obj == null) return;
        obj.addProperty("secondsLeft", secondsLeft);
        store.put(ACTIVE_COLLECTION, String.valueOf(id), obj);
    }

    /** Count how many active listings a player currently has. */
    public int countActiveForPlayer(String playerUuid) {
        int count = 0;
        for (JsonObject obj : store.getAll(ACTIVE_COLLECTION).values()) {
            if (playerUuid.equals(obj.get("playeruuid").getAsString())) count++;
        }
        return count;
    }

    // ── Expired items ─────────────────────────────────────────────────────────

    /** Load all expired items from storage. */
    public List<AuctionItem> loadAllExpired() {
        List<AuctionItem> list = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : store.getAll(EXPIRED_COLLECTION).entrySet()) {
            list.add(fromJson(parseIdSafe(entry.getKey()), entry.getValue(), 0L));
        }
        return list;
    }

    /**
     * Move an active listing to the expired collection, preserving its id.
     * The original listing is removed from {@code auction_listings}.
     */
    public void expireItem(AuctionItem item) {
        removeActive(item.getId());
        JsonObject obj = toJson(item.getUuid(), item.getOwner(), item.getNbt(), item.getItemKey(),
            item.getCount(), item.getPrice(), 0L);
        store.put(EXPIRED_COLLECTION, String.valueOf(item.getId()), obj);
    }

    /** Delete an expired item once the player has collected it. */
    public void removeExpired(int id) {
        store.delete(EXPIRED_COLLECTION, String.valueOf(id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int parseIdSafe(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            NeoLog.debug(LOGGER, LogCategory.AUCTION_HOUSE, "parseIdSafe: non-numeric listing key '" + id + "', treating as 0", e);
            return 0;
        }
    }

    private static JsonObject toJson(String playerUuid, String owner, String nbt, String itemKey,
                                      int count, double price, long secondsLeft) {
        JsonObject obj = new JsonObject();
        obj.addProperty("playeruuid", playerUuid);
        obj.addProperty("owner", owner);
        obj.addProperty("nbt", nbt);
        obj.addProperty("item", itemKey);
        obj.addProperty("count", count);
        obj.addProperty("price", price);
        obj.addProperty("secondsLeft", secondsLeft);
        return obj;
    }

    private static AuctionItem fromJson(int id, JsonObject obj, long secondsLeft) {
        return new AuctionItem(
            id,
            obj.get("playeruuid").getAsString(),
            obj.get("owner").getAsString(),
            obj.has("nbt") && !obj.get("nbt").isJsonNull() ? obj.get("nbt").getAsString() : null,
            obj.get("item").getAsString(),
            obj.get("count").getAsInt(),
            obj.get("price").getAsDouble(),
            secondsLeft);
    }

    /**
     * One-time import of the legacy {@code auctionhouse.db} SQLite file (this class's old,
     * bespoke database, opened directly rather than through {@link DataStore}) into the active
     * storage backend, if both collections are still empty and {@code storage.autoMigrate} is
     * enabled. The legacy database file is left in place, untouched.
     */
    private void migrateLegacyDatabaseIfNeeded() {
        if (store.hasAnyData(ACTIVE_COLLECTION) || store.hasAnyData(EXPIRED_COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;
        if (!new File("auctionhouse.db").exists()) return;

        int migrated = 0;
        try {
            // Loads (downloading on first use if needed) the driver — see
            // SqliteDriverProvisioner for why this isn't just a JarJar-bundled dependency
            // anymore. A legacy auctionhouse.db existing is rare (pre-DataStore installs
            // only), so this is a genuinely occasional download, not a routine one.
            // Connects through the Driver instance directly, not DriverManager.getConnection —
            // see SqliteDataStore.openConnection() for why (classloader-visibility gotcha).
            java.sql.Driver driver = com.zerog.neoessentials.storage.SqliteDriverProvisioner.ensureDriver();
            if (driver == null) {
                LOGGER.error("[AuctionHouse] SQLite driver unavailable, cannot migrate legacy auctionhouse.db");
                return;
            }
            try (Connection connection = driver.connect(LEGACY_URL, new java.util.Properties())) {
                migrated += migrateLegacyTable(connection, "auctionhouse", ACTIVE_COLLECTION, true);
                migrated += migrateLegacyTable(connection, "expireditems", EXPIRED_COLLECTION, false);
            }
        } catch (Exception e) {
            LOGGER.error("[AuctionHouse] Failed to migrate legacy auctionhouse.db: {}", e.getMessage(), e);
        }

        if (migrated > 0) {
            int maxId = 0;
            for (String id : store.getAll(ACTIVE_COLLECTION).keySet()) maxId = Math.max(maxId, parseIdSafe(id));
            for (String id : store.getAll(EXPIRED_COLLECTION).keySet()) maxId = Math.max(maxId, parseIdSafe(id));
            nextId.set(maxId + 1);
            LOGGER.info("[AuctionHouse] migrated {} listing(s) from legacy auctionhouse.db into the '{}' storage backend.",
                migrated, StorageManager.getInstance().getActiveType());
        }
    }

    private int migrateLegacyTable(Connection connection, String table, String collection, boolean hasSecondsLeft) throws SQLException {
        int count = 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
            while (rs.next()) {
                JsonObject obj = toJson(
                    rs.getString("playeruuid"),
                    rs.getString("owner"),
                    rs.getString("nbt"),
                    rs.getString("item"),
                    rs.getInt("count"),
                    rs.getDouble("price"),
                    hasSecondsLeft ? rs.getLong("secondsLeft") : 0L);
                store.put(collection, String.valueOf(rs.getInt("id")), obj);
                count++;
            }
        }
        return count;
    }
}
