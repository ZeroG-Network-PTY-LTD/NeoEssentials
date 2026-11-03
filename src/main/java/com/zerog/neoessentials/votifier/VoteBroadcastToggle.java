package com.zerog.neoessentials.votifier;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/** Persisted per-player opt-out of the "X voted!" server broadcast — {@code /togglevotebroadcast}. */
public final class VoteBroadcastToggle {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteBroadcastToggle.class);
    private static final String COLLECTION = "votifier_broadcast_toggle";

    private VoteBroadcastToggle() {}

    public static boolean isOptedOut(UUID playerUuid) {
        JsonObject record = StorageManager.getInstance().getStore().get(COLLECTION, playerUuid.toString());
        return record != null && record.has("optedOut") && record.get("optedOut").getAsBoolean();
    }

    /** Flips the toggle and returns the new state (true = now opted out). */
    public static boolean toggle(UUID playerUuid) {
        boolean newState = !isOptedOut(playerUuid);
        JsonObject record = new JsonObject();
        record.addProperty("optedOut", newState);
        if (!StorageManager.getInstance().getStore().put(COLLECTION, playerUuid.toString(), record)) {
            NeoLog.error(LOGGER, LogCategory.VOTIFIER, "Failed to persist {}/{} to storage - this change may be lost on restart.", COLLECTION, playerUuid);
        }
        return newState;
    }
}
