package com.zerog.neoessentials.leaderboard.adapters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.leaderboard.StatProvider;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reads a vanilla per-player statistic (kills, mob kills, deaths, playtime, ...) directly —
 * no custom event tracking or storage needed, since Minecraft already persists every
 * player's stats to {@code <world>/stats/<uuid>.json} whether they're online or not.
 * Online players read their live in-memory value (via {@link ServerPlayer#getStats()},
 * already loaded — no disk I/O); offline players are read straight from that JSON file.
 */
public class VanillaStatProvider implements StatProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaStatProvider.class);

    private final Stat<?> stat;
    /** The stat's raw id, e.g. "minecraft:player_kills" — matches the JSON key under
     *  {@code stats["minecraft:custom"]} in a player's stats file. */
    private final String jsonKey;
    private final boolean formatAsTime;

    public VanillaStatProvider(Stat<?> stat, String jsonKey, boolean formatAsTime) {
        this.stat = stat;
        this.jsonKey = jsonKey;
        this.formatAsTime = formatAsTime;
    }

    @Override
    public Map<UUID, Number> getAllValues(MinecraftServer server) {
        Map<UUID, Number> out = new LinkedHashMap<>();

        // Online players — live value, no disk read.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            out.put(player.getUUID(), player.getStats().getValue(stat));
        }

        // Offline players — one JSON file per player who has ever played.
        try {
            Path statsDir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
            if (Files.isDirectory(statsDir)) {
                try (var files = Files.list(statsDir)) {
                    files.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                        String fileName = p.getFileName().toString();
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                        } catch (IllegalArgumentException e) {
                            return;
                        }
                        if (out.containsKey(uuid)) return; // already have the live value
                        out.put(uuid, readStatFromFile(p));
                    });
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "VanillaStatProvider[{}]: failed to scan stats directory", jsonKey, e);
        }

        return out;
    }

    private int readStatFromFile(Path file) {
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("stats")) return 0;
            JsonObject stats = root.getAsJsonObject("stats");
            if (!stats.has("minecraft:custom")) return 0;
            JsonObject custom = stats.getAsJsonObject("minecraft:custom");
            return custom.has(jsonKey) ? custom.get(jsonKey).getAsInt() : 0;
        } catch (IOException | RuntimeException e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "VanillaStatProvider[{}]: failed to read {}", jsonKey, file, e);
            return 0;
        }
    }

    @Override
    public String formatValue(Number value) {
        if (!formatAsTime) return String.valueOf(value.intValue());
        // Playtime is stored in ticks (20/sec).
        long totalSeconds = value.longValue() / 20L;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
