package com.zerog.neoessentials.votifier;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;

import java.util.UUID;

/** Persisted per-player opt-out of the "X voted!" server broadcast — {@code /togglevotebroadcast}. */
public final class VoteBroadcastToggle {
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
        StorageManager.getInstance().getStore().put(COLLECTION, playerUuid.toString(), record);
        return newState;
    }
}
