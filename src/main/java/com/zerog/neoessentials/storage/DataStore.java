package com.zerog.neoessentials.storage;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * A generic document store: every record is a JSON object, addressed by a
 * ({@code collection}, {@code id}) pair. "Collection" is the equivalent of a table/file
 * (e.g. {@code "player_bans"}, {@code "warns"}); {@code id} is that collection's primary
 * key (usually a UUID string, sometimes a player name or IP address).
 *
 * <p>This is deliberately schema-less — every backend stores the same JSON blob per
 * record — so any manager already using Gson to build/parse its records (which is all
 * of them) can plug into any backend with the same code, instead of needing a bespoke
 * relational schema per data type. The trade-off is no server-side querying/filtering
 * beyond "all records in a collection" — callers do that in Java, same as they already
 * do today reading a whole JSON file into memory.
 *
 * <p>Implementations: {@link JsonFileDataStore} (default — one JSON file per
 * collection), {@link YamlFileDataStore} (same shape, YAML syntax),
 * {@link SqliteDataStore} (one embedded database file, one table per collection),
 * {@link MySqlDataStore} (same table shape against a shared server, enabling multiple
 * Minecraft servers to see the same bans/mutes/economy/etc. in real time).
 */
public interface DataStore {

    /** Inserts or overwrites a single record. */
    void put(String collection, String id, JsonObject data);

    /** Returns a record, or {@code null} if it doesn't exist. */
    JsonObject get(String collection, String id);

    /** Deletes a record. Returns {@code true} if it existed. */
    boolean delete(String collection, String id);

    /** Every record in a collection, keyed by id. Empty map if the collection doesn't exist yet. */
    Map<String, JsonObject> getAll(String collection);

    /** Whether a collection has ever been written to (used to decide whether migration is needed). */
    boolean hasAnyData(String collection);

    /** Releases any held resources (connections, file handles). Safe to call even if never opened. */
    void close();
}
