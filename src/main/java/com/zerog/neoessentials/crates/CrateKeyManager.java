package com.zerog.neoessentials.crates;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Virtual, non-transferable per-player key balances — {@code (playerUuid, crateId) -> count},
 * mirroring {@code EconomyManager}'s balance model. This is one of two independent ways a
 * player can hold crate keys; the other is a physical key item (see
 * {@link CrateManager#buildKeyItem}), which is self-sufficient on its own and does not touch
 * this balance at all — see {@link CrateManager#tryConsumeKeyAndPick}.
 */
public class CrateKeyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrateKeyManager.class);
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

    /** Removes up to {@code amount} keys, never going below 0. Returns {@code true} only if the
     *  player had enough keys AND the decrement was actually persisted - previously this
     *  returned true purely from the in-memory balance check, so a transient storage failure
     *  right when a virtual key was spent let the player keep whatever the key unlocked while
     *  the balance decrement itself was lost, effectively refunding the spent key on restart. */
    public boolean removeKeys(UUID playerUuid, String crateId, int amount) {
        int current = getKeys(playerUuid, crateId);
        if (current < amount) return false;
        return setKeys(playerUuid, crateId, current - amount);
    }

    /** @return true if the new balance was actually persisted. */
    public boolean setKeys(UUID playerUuid, String crateId, int amount) {
        JsonObject record = new JsonObject();
        record.addProperty("count", Math.max(0, amount));
        boolean saved = StorageManager.getInstance().getStore().put(COLLECTION, key(playerUuid, crateId), record);
        if (!saved) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to persist {}/{} to storage - this change may be lost on restart.", COLLECTION, key(playerUuid, crateId));
        }
        return saved;
    }
}
