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
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads a vanilla per-player statistic (kills, mob kills, deaths, playtime, blocks mined,
 * mobs killed by type, distance traveled, ...) directly — no custom event tracking or
 * storage needed, since Minecraft already persists every player's stats to
 * {@code <world>/stats/<uuid>.json} whether they're online or not. Online players read
 * their live in-memory value (via {@link ServerPlayer#getStats()}, already loaded — no disk
 * I/O); offline players are read straight from that JSON file.
 */
public class VanillaStatProvider implements StatProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaStatProvider.class);

    private final Stat<?> stat;
    /** Top-level JSON key for this stat's category, e.g. "minecraft:custom" / "minecraft:mined". */
    private final String statTypeKey;
    /** The stat's own key within that category, e.g. "minecraft:player_kills" / "minecraft:diamond_ore". */
    private final String valueKey;
    private final boolean formatAsTime;

    public VanillaStatProvider(Stat<?> stat, String jsonKey, boolean formatAsTime) {
        this(stat, "minecraft:custom", jsonKey, formatAsTime);
    }

    public VanillaStatProvider(Stat<?> stat, String statTypeKey, String valueKey, boolean formatAsTime) {
        this.stat = stat;
        this.statTypeKey = statTypeKey;
        this.valueKey = valueKey;
        this.formatAsTime = formatAsTime;
    }

    /**
     * Resolves a config-authored stat key — vanilla's own scoreboard-objective-criteria
     * grammar, e.g. {@code "minecraft.custom:minecraft.player_kills"} or
     * {@code "minecraft.mined:minecraft.diamond_ore"} (the exact string
     * {@code /scoreboard objectives add <name> <criteria>} accepts in-game) — into a
     * provider. Returns empty (logging why) if the key doesn't resolve to a real per-player
     * stat, so one bad board definition in {@code leaderboard.json} can't break startup.
     */
    public static Optional<VanillaStatProvider> fromStatKey(String statKey, boolean formatAsTime) {
        try {
            Optional<ObjectiveCriteria> criteria = ObjectiveCriteria.byName(statKey);
            if (criteria.isEmpty() || !(criteria.get() instanceof Stat<?> stat)) {
                LOGGER.warn("VanillaStatProvider: '{}' is not a valid per-player stat criteria — skipping this board.", statKey);
                return Optional.empty();
            }

            int colon = statKey.indexOf(':');
            if (colon < 0) {
                LOGGER.warn("VanillaStatProvider: '{}' is missing the ':' separator (expected <type>:<value>) — skipping this board.", statKey);
                return Optional.empty();
            }
            // Criteria-name format is "<type ns>.<type path>:<value ns>.<value path>" (dots);
            // the on-disk stats.json / vanilla's own JSON keys use colons instead.
            String statTypeKey = statKey.substring(0, colon).replaceFirst("\\.", ":");
            String valueKey = statKey.substring(colon + 1).replaceFirst("\\.", ":");

            return Optional.of(new VanillaStatProvider(stat, statTypeKey, valueKey, formatAsTime));
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "VanillaStatProvider: failed to resolve stat key '{}'", statKey, e);
            return Optional.empty();
        }
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
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "VanillaStatProvider[{}]: failed to scan stats directory", valueKey, e);
        }

        return out;
    }

    private int readStatFromFile(Path file) {
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("stats")) return 0;
            JsonObject stats = root.getAsJsonObject("stats");
            if (!stats.has(statTypeKey)) return 0;
            JsonObject category = stats.getAsJsonObject(statTypeKey);
            return category.has(valueKey) ? category.get(valueKey).getAsInt() : 0;
        } catch (IOException | RuntimeException e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "VanillaStatProvider[{}]: failed to read {}", valueKey, file, e);
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
