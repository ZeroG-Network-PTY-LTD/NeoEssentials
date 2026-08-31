package com.zerog.neoessentials.crates;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;

import java.util.UUID;

/**
 * Virtual per-player key balances — {@code (playerUuid, crateId) -> count}, mirroring
 * {@code EconomyManager}'s balance model. This is the source of truth for how many keys a
 * player holds; a physical key item (see {@link CrateManager}) is only a convenience
 * representation that redeems back into a decrement here, never trusted on its own — avoids
 * the classic dupe-exploit risk of an NBT-tagged item being the sole source of truth.
 */
public class CrateKeyManager {
    private static final String COLLECTION = "crate_keys";

    private static class Holder {
        static final CrateKeyManager INSTANCE = new CrateKeyManager();
    }
    public static CrateKeyManager getInstance() { return Holder.INSTANCE; }

    private CrateKeyManager() {}

    private static String key(UUID playerUuid, String crateId) {
        return playerUuid + ":" + crateId.toLowerCase();
    }

    public int getKeys(UUID playerUuid, String crateId) {
        JsonObject record = StorageManager.getInstance().getStore().get(COLLECTION, key(playerUuid, crateId));
        return record != null && record.has("count") ? record.get("count").getAsInt() : 0;
    }

    public void addKeys(UUID playerUuid, String crateId, int amount) {
        setKeys(playerUuid, crateId, getKeys(playerUuid, crateId) + amount);
    }

    /** Removes up to {@code amount} keys, never going below 0. Returns {@code true} if the
     *  player had enough keys and the full amount was removed. */
    public boolean removeKeys(UUID playerUuid, String crateId, int amount) {
        int current = getKeys(playerUuid, crateId);
        if (current < amount) return false;
        setKeys(playerUuid, crateId, current - amount);
        return true;
    }

    public void setKeys(UUID playerUuid, String crateId, int amount) {
        JsonObject record = new JsonObject();
        record.addProperty("count", Math.max(0, amount));
        StorageManager.getInstance().getStore().put(COLLECTION, key(playerUuid, crateId), record);
    }
}
