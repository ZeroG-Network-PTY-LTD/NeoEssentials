package com.zerog.neoessentials.votifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.crates.CrateKeyManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Resolves the voter and applies the configured reward for that vote site: console commands
 * ({@code {player}} substituted), and crate keys (if the Crates module is enabled). Also
 * updates per-player vote stats and — if the voter isn't currently online — queues the reward
 * to apply on their next login instead of dropping it.
 */
@EventBusSubscriber(modid = "neoessentials")
public class VoteRewardManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteRewardManager.class);
    private static final String PENDING_COLLECTION = "votifier_pending";
    private static final String STATS_COLLECTION = "votifier_stats";

    @SubscribeEvent
    public static void onVote(PlayerVoteEvent event) {
        Vote vote = event.getVote();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.execute(() -> {
            ServerPlayer online = server.getPlayerList().getPlayerByName(vote.username());
            recordStats(vote);

            if (online != null) {
                applyReward(server, online.getUUID(), vote);
                broadcast(server, vote);
                VotePartyManager.getInstance().onVote(server);
            } else {
                queuePending(vote);
                NeoLog.info(LOGGER, LogCategory.VOTIFIER, "{} voted while offline — reward queued for next login", vote.username());
                VotePartyManager.getInstance().onVote(server);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        DataStore store = StorageManager.getInstance().getStore();
        String key = player.getName().getString().toLowerCase();
        JsonObject record = store.get(PENDING_COLLECTION, key);
        if (record == null || !record.has("votes")) return;

        JsonArray votes = record.getAsJsonArray("votes");
        if (votes.isEmpty()) return;

        int count = votes.size();
        for (var el : votes) {
            JsonObject v = el.getAsJsonObject();
            Vote vote = new Vote(
                v.get("serviceName").getAsString(),
                v.get("username").getAsString(),
                v.has("address") ? v.get("address").getAsString() : "",
                v.has("timestamp") ? v.get("timestamp").getAsString() : "");
            applyReward(server, player.getUUID(), vote);
        }
        store.delete(PENDING_COLLECTION, key);
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.votifier.pending_claimed", String.valueOf(count)));
        NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Delivered {} pending vote reward(s) to {}", count, player.getName().getString());
    }

    private static void applyReward(MinecraftServer server, UUID playerUuid, Vote vote) {
        JsonObject site = resolveSiteConfig(vote.serviceName());
        if (site == null) return;

        if (site.has("commands")) {
            for (var el : site.getAsJsonArray("commands")) {
                String cmd = el.getAsString().replace("{player}", vote.username());
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                String finalCmd = cmd;
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), finalCmd));
            }
        }

        if (site.has("keys") && ConfigManager.isCratesModuleEnabled()) {
            JsonObject keys = site.getAsJsonObject("keys");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : keys.entrySet()) {
                try {
                    int amount = entry.getValue().getAsInt();
                    if (amount > 0) CrateKeyManager.getInstance().addKeys(playerUuid, entry.getKey(), amount);
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Failed to grant crate key '{}' from vote reward", entry.getKey(), e);
                }
            }
        }
    }

    private static void queuePending(Vote vote) {
        DataStore store = StorageManager.getInstance().getStore();
        String key = vote.username().toLowerCase();
        JsonObject record = store.get(PENDING_COLLECTION, key);
        if (record == null) {
            record = new JsonObject();
            record.add("votes", new JsonArray());
        }
        JsonArray votes = record.getAsJsonArray("votes");

        JsonObject v = new JsonObject();
        v.addProperty("serviceName", vote.serviceName());
        v.addProperty("username", vote.username());
        v.addProperty("address", vote.address());
        v.addProperty("timestamp", vote.timestamp());
        votes.add(v);

        store.put(PENDING_COLLECTION, key, record);
    }

    private static void recordStats(Vote vote) {
        DataStore store = StorageManager.getInstance().getStore();
        String key = vote.username().toLowerCase();
        JsonObject record = store.get(STATS_COLLECTION, key);
        if (record == null) {
            record = new JsonObject();
            record.addProperty("total", 0);
        }
        long total = record.has("total") ? record.get("total").getAsLong() : 0L;
        record.addProperty("total", total + 1);
        record.addProperty("lastVote", System.currentTimeMillis());
        store.put(STATS_COLLECTION, key, record);
    }

    private static void broadcast(MinecraftServer server, Vote vote) {
        String template = getBroadcastTemplate();
        if (template == null || template.isBlank()) return;
        String message = template.replace("{player}", vote.username()).replace("{site}", vote.serviceName());
        var component = com.zerog.neoessentials.chat.RichTextFormatter.processTablistText(message);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!VoteBroadcastToggle.isOptedOut(p.getUUID())) {
                p.sendSystemMessage(component);
            }
        }
    }

    private static JsonObject resolveSiteConfig(String serviceName) {
        JsonObject votifier = getVotifierConfig();
        if (votifier == null || !votifier.has("sites")) return null;
        JsonObject sites = votifier.getAsJsonObject("sites");
        for (String key : sites.keySet()) {
            if (key.equalsIgnoreCase(serviceName)) return sites.getAsJsonObject(key);
        }
        return sites.has("default") ? sites.getAsJsonObject("default") : null;
    }

    private static String getBroadcastTemplate() {
        JsonObject votifier = getVotifierConfig();
        if (votifier != null && votifier.has("broadcastMessage")) {
            return votifier.get("broadcastMessage").getAsString();
        }
        return null;
    }

    private static JsonObject getVotifierConfig() {
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.VOTIFIER_CONFIG);
            return root.has("votifier") ? root.getAsJsonObject("votifier") : null;
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Failed to read votifier.json", e);
            return null;
        }
    }
}
