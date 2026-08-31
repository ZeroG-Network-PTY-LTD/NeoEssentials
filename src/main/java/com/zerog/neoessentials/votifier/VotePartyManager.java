package com.zerog.neoessentials.votifier;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tracks a cumulative server-wide vote counter; grants a bonus to everyone online once the
 *  configured threshold is reached, then resets. */
public class VotePartyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VotePartyManager.class);
    private static final String COLLECTION = "votifier_voteparty";
    private static final String RECORD_ID = "counter";

    private static class Holder {
        static final VotePartyManager INSTANCE = new VotePartyManager();
    }
    public static VotePartyManager getInstance() { return Holder.INSTANCE; }

    private VotePartyManager() {}

    /** Called once per received vote (online or offline voter — vote party counts all votes). */
    public void onVote(MinecraftServer server) {
        JsonObject config = getVotePartyConfig();
        if (config == null || !config.has("enabled") || !config.get("enabled").getAsBoolean()) return;

        int required = config.has("votesRequired") ? config.get("votesRequired").getAsInt() : 50;
        if (required <= 0) return;

        DataStore store = StorageManager.getInstance().getStore();
        JsonObject record = store.get(COLLECTION, RECORD_ID);
        int current = record != null && record.has("count") ? record.get("count").getAsInt() : 0;
        current++;

        if (current >= required) {
            trigger(server, config);
            current = 0;
        }

        JsonObject updated = new JsonObject();
        updated.addProperty("count", current);
        store.put(COLLECTION, RECORD_ID, updated);
    }

    public int getProgress() {
        JsonObject record = StorageManager.getInstance().getStore().get(COLLECTION, RECORD_ID);
        return record != null && record.has("count") ? record.get("count").getAsInt() : 0;
    }

    public int getRequired() {
        JsonObject config = getVotePartyConfig();
        return config != null && config.has("votesRequired") ? config.get("votesRequired").getAsInt() : 50;
    }

    /** Called on server start when {@code resetOnRestart} is true. */
    public void resetIfConfigured() {
        JsonObject config = getVotePartyConfig();
        if (config != null && config.has("resetOnRestart") && config.get("resetOnRestart").getAsBoolean()) {
            JsonObject reset = new JsonObject();
            reset.addProperty("count", 0);
            StorageManager.getInstance().getStore().put(COLLECTION, RECORD_ID, reset);
        }
    }

    private void trigger(MinecraftServer server, JsonObject config) {
        NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Vote party triggered!");
        if (!config.has("commands")) return;
        for (var el : config.getAsJsonArray("commands")) {
            String cmd = el.getAsString();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            String finalCmd = cmd;
            server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), finalCmd));
        }
    }

    private JsonObject getVotePartyConfig() {
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.VOTIFIER_CONFIG);
            if (!root.has("votifier")) return null;
            JsonObject votifier = root.getAsJsonObject("votifier");
            return votifier.has("voteParty") ? votifier.getAsJsonObject("voteParty") : null;
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Failed to read votifier.json voteParty section", e);
            return null;
        }
    }
}
