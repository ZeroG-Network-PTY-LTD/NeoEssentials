package com.zerog.neoessentials.teleportation.DirectTeleport;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * RandomTeleportManager — Essentials-inspired random teleport system for NeoForge.
 *
 * Ported concepts from com.earth2me.essentials.RandomTeleport:
 *  - Named locations with centre, min/max range configuration
 *  - Equally-distributed random offset (4-rotation rectangle method)
 *  - Pre-computation cache (filled asynchronously, drained on demand)
 *  - Nether-aware Y detection (scan up from y=32 to below bedrock ceiling)
 *  - World-border awareness
 *  - Excluded biome list
 *  - Configurable find-attempts and cache-threshold
 */
public class RandomTeleportManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomTeleportManager.class);
    private static final Random RANDOM = new Random();

    // Default location name used when no argument is supplied
    private static final String DEFAULT_LOCATION_KEY = "default";

    // Singleton
    private static class Holder {
        static final RandomTeleportManager INSTANCE = new RandomTeleportManager();
    }
    public static RandomTeleportManager getInstance() { return Holder.INSTANCE; }

    // Cache: location-name -> queue of ready TeleportLocations
    private final Map<String, ConcurrentLinkedQueue<TeleportLocation>> locationCache = new ConcurrentHashMap<>();

    // Per-player cooldown tracking (ms timestamps)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private RandomTeleportManager() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Execute a /tpr for a player, using either the default location or a named one.
     * Returns a future that completes with true on success.
     */
    public CompletableFuture<Boolean> randomTeleport(ServerPlayer player, String locationName) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "randomTeleport request: player={} locationName={}",
            player.getName().getString(), locationName);
        // Cooldown check
        int cooldownSecs = getTprCooldown();
        if (cooldownSecs > 0) {
            long last = cooldowns.getOrDefault(player.getUUID(), 0L);
            long remaining = (last + cooldownSecs * 1000L) - System.currentTimeMillis();
            if (remaining > 0) {
                long secs = (remaining / 1000) + 1;
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "randomTeleport: {} blocked by cooldown, {}s remaining", player.getName().getString(), secs);
                player.sendSystemMessage(MessageUtil.error(
                        "commands.neoessentials.teleport.misc.tpr_cooldown",
                        String.valueOf(secs)));
                return CompletableFuture.completedFuture(false);
            }
        }

        String name = (locationName == null || locationName.isEmpty()) ? resolveDefaultName(player) : locationName;
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.misc.tpr_searching"));

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        getRandomLocation(com.zerog.neoessentials.util.LevelCompat.of(player), name)
                .thenAccept(loc -> {
                    if (loc == null) {
                        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "randomTeleport: no safe location found for {} (location={})",
                            player.getName().getString(), name);
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_no_safe_location"));
                        result.complete(false);
                        return;
                    }

                    // Save back-location before teleporting
                    com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance()
                            .saveBackLocation(player);

                    int delayTicks = getTeleportDelaySecs() * 20;
                    TeleportUtil.teleportPlayer(player, loc, delayTicks, false /* we already ensured safety */)
                            .thenAccept(tpResult -> {
                                if (tpResult.isSuccess()) {
                                    cooldowns.put(player.getUUID(), System.currentTimeMillis());
                                    player.sendSystemMessage(MessageUtil.success(
                                            "commands.neoessentials.teleport.misc.tpr_success",
                                            String.valueOf((int) loc.getX()),
                                            String.valueOf((int) loc.getY()),
                                            String.valueOf((int) loc.getZ())));
                                    NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} randomly teleported to ({}, {}, {}) in {}",
                                            player.getName().getString(),
                                            (int) loc.getX(), (int) loc.getY(), (int) loc.getZ(),
                                            loc.getWorldName());
                                    result.complete(true);

                                    // Pre-warm cache in background
                                    prewarmCache(com.zerog.neoessentials.util.LevelCompat.of(player), name);
                                } else {
                                    player.sendSystemMessage(MessageUtil.error(
                                            "commands.neoessentials.teleport.misc.tpr_failed",
                                            tpResult.getMessage()));
                                    result.complete(false);
                                }
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.error("RandomTeleport error for {}: {}", player.getName().getString(), ex.getMessage(), ex);
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_failed", ex.getMessage()));
                    result.complete(false);
                    return null;
                });

        return result;
    }

    /**
     * Biome-targeted variant of {@link #randomTeleport} — used by the RTP GUI (see
     * {@code RandomTeleportMenu}) when a player picks a specific biome instead of "any". Same
     * cooldown/back-location/teleport-delay handling as the plain path; only the search step
     * differs — it uses vanilla's own {@code ServerLevel.findClosestBiome3d} (the same engine
     * {@code /locate biome} uses) instead of an unconstrained random offset.
     */
    public CompletableFuture<Boolean> randomTeleportToBiome(ServerPlayer player, ResourceKey<Biome> biomeKey) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "randomTeleportToBiome request: player={} biome={}",
            player.getName().getString(), biomeKey.location());

        int cooldownSecs = getTprCooldown();
        if (cooldownSecs > 0) {
            long last = cooldowns.getOrDefault(player.getUUID(), 0L);
            long remaining = (last + cooldownSecs * 1000L) - System.currentTimeMillis();
            if (remaining > 0) {
                long secs = (remaining / 1000) + 1;
                player.sendSystemMessage(MessageUtil.error(
                        "commands.neoessentials.teleport.misc.tpr_cooldown", String.valueOf(secs)));
                return CompletableFuture.completedFuture(false);
            }
        }

        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        String name = resolveDefaultName(player);
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.misc.tpr_searching"));

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> findBiomeSafeLocation(level, name, biomeKey), level.getServer())
                .thenAccept(loc -> {
                    if (loc == null) {
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_biome_not_found"));
                        result.complete(false);
                        return;
                    }

                    com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);

                    int delayTicks = getTeleportDelaySecs() * 20;
                    TeleportUtil.teleportPlayer(player, loc, delayTicks, false)
                            .thenAccept(tpResult -> {
                                if (tpResult.isSuccess()) {
                                    cooldowns.put(player.getUUID(), System.currentTimeMillis());
                                    player.sendSystemMessage(MessageUtil.success(
                                            "commands.neoessentials.teleport.misc.tpr_success",
                                            String.valueOf((int) loc.getX()),
                                            String.valueOf((int) loc.getY()),
                                            String.valueOf((int) loc.getZ())));
                                    NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} randomly teleported into biome {} at ({}, {}, {})",
                                            player.getName().getString(), biomeKey.location(),
                                            (int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
                                    result.complete(true);
                                } else {
                                    player.sendSystemMessage(MessageUtil.error(
                                            "commands.neoessentials.teleport.misc.tpr_failed", tpResult.getMessage()));
                                    result.complete(false);
                                }
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.error("randomTeleportToBiome error for {}: {}", player.getName().getString(), ex.getMessage(), ex);
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_failed", ex.getMessage()));
                    result.complete(false);
                    return null;
                });

        return result;
    }

    /** Runs on the server thread (chunk/biome sampling requires it) — same jittered-center
     *  approach as {@link #attemptFind}, but each attempt searches for the target biome via
     *  vanilla's own biome-search engine instead of accepting whatever biome it lands on. */
    private TeleportLocation findBiomeSafeLocation(ServerLevel level, String name, ResourceKey<Biome> biomeKey) {
        double[] center = getCenter(level, name);
        double minRange = getMinRange(name);
        double maxRange = getMaxRange(level, name);
        int attempts = getFindAttempts();
        int radius = getBiomeSearchRadius();
        int step = getBiomeSearchStep();

        for (int i = 0; i < attempts; i++) {
            double[] offset = randomOffset(minRange, maxRange);
            double cx = clampToWorldBorder(level, center[0] + offset[0], true);
            double cz = clampToWorldBorder(level, center[2] + offset[1], false);
            BlockPos searchOrigin = new BlockPos((int) cx, (int) center[1], (int) cz);

            var pair = level.findClosestBiome3d(holder -> holder.is(biomeKey), searchOrigin, radius, step, 64);
            if (pair == null) continue;

            BlockPos found = pair.getFirst();
            if (!level.getWorldBorder().isWithinBounds(found)) continue;

            TeleportLocation loc = findSafeY(level, found.getX(), found.getZ(), name);
            if (loc != null) return loc;
        }
        return null;
    }

    /** Every biome the current dimension's generator can actually produce — used by the RTP
     *  GUI to build its biome list (already correctly scoped per-dimension, and includes any
     *  biome a mod registers into that dimension's generation, no extra plumbing needed). */
    public List<net.minecraft.core.Holder<Biome>> getPossibleBiomes(ServerLevel level) {
        return level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream()
                .sorted(Comparator.comparing(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("")))
                .toList();
    }

    // -----------------------------------------------------------------------
    // Core location finder
    // -----------------------------------------------------------------------

    /**
     * Returns a random safe TeleportLocation for the given named config slot.
     * Uses cache if available, else tries to find one immediately.
     */
    public CompletableFuture<TeleportLocation> getRandomLocation(ServerLevel level, String name) {
        Queue<TeleportLocation> cache = getCache(name);
        if (!cache.isEmpty()) {
            return CompletableFuture.completedFuture(cache.poll());
        }
        // Cache miss — find one now
        double[] center = getCenter(level, name);
        double minRange = getMinRange(name);
        double maxRange = getMaxRange(level, name);
        int attempts = getFindAttempts();
        return attemptFind(level, center[0], center[1], center[2], minRange, maxRange, name, attempts);
    }

    /**
     * Find with explicit parameters (no named config slot).
     */
    public CompletableFuture<TeleportLocation> getRandomLocation(ServerLevel level,
                                                                  double cx, double cy, double cz,
                                                                  double minRange, double maxRange) {
        return attemptFind(level, cx, cy, cz, minRange, maxRange, null, getFindAttempts());
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private CompletableFuture<TeleportLocation> attemptFind(ServerLevel level,
                                                             double cx, double cy, double cz,
                                                             double minRange, double maxRange,
                                                             String locationName,
                                                             int attemptsLeft) {
        if (attemptsLeft <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        double[] offset = randomOffset(minRange, maxRange);
        double rx = cx + offset[0];
        double rz = cz + offset[1];

        // Clamp to world border
        rx = clampToWorldBorder(level, rx, true);
        rz = clampToWorldBorder(level, rz, false);

        final double finalRx = rx;
        final double finalRz = rz;

        return CompletableFuture.supplyAsync(() -> findSafeY(level, finalRx, finalRz, locationName), level.getServer())
                .thenCompose(loc -> {
                    if (loc != null && isValid(loc, locationName)) {
                        return CompletableFuture.completedFuture(loc);
                    }
                    return attemptFind(level, cx, cy, cz, minRange, maxRange, locationName, attemptsLeft - 1);
                });
    }

    /**
     * Finds a safe Y coordinate for the given X,Z and returns a TeleportLocation,
     * or null if none is found.
     */
    private TeleportLocation findSafeY(ServerLevel level, double x, double z, String locationName) {
        try {
            // Force-load the chunk
            int chunkX = (int) x >> 4;
            int chunkZ = (int) z >> 4;
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);

            boolean isNether = level.dimensionType().ultraWarm(); // ultrawarm == nether-like

            int ix = (int) Math.floor(x);
            int iz = (int) Math.floor(z);
            int y;

            if (isNether) {
                y = findNetherY(level, ix, iz);
            } else {
                y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        ix, iz);
            }

            if (y <= com.zerog.neoessentials.util.LevelHeightCompat.minBuildHeight(level)) {
                return null;
            }

            // Verify 2-block clearance (feet + head)
            BlockPos feet = new BlockPos(ix, y, iz);
            if (!isSafeSpot(level, feet)) {
                // Try scanning up a few blocks in case heightmap was a leaf/carpet
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos candidate = feet.above(dy);
                    if (isSafeSpot(level, candidate)) {
                        y = candidate.getY();
                        feet = candidate;
                        break;
                    }
                }
                if (!isSafeSpot(level, feet)) return null;
            }

            return new TeleportLocation(level, feet,
                    RANDOM.nextFloat() * 360f - 180f,
                    0f,
                    "RandomTeleport");

        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "findSafeY error at ({},{}): {}", x, z, e.getMessage());
            return null;
        }
    }

    /**
     * Nether Y scan: scan up from y=32 to find an air gap below the bedrock ceiling.
     */
    private int findNetherY(ServerLevel level, int x, int z) {
        int maxScan = com.zerog.neoessentials.util.LevelHeightCompat.maxBuildHeight(level) - 1;
        for (int y = 32; y < maxScan; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            // Stop at bedrock (approaching ceiling)
            if (state.is(net.minecraft.world.level.block.Blocks.BEDROCK)) break;
            if (isSafeSpot(level, pos)) return y;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * A spot is safe if: solid block below, air at feet, air at head (no lava/fire etc.)
     */
    private boolean isSafeSpot(ServerLevel level, BlockPos feet) {
        BlockPos ground = feet.below();
        BlockPos head = feet.above();

        BlockState groundState = level.getBlockState(ground);
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);

        if (!groundState.isSolid()) return false;
        if (!feetState.isAir()) return false;
        if (!headState.isAir()) return false;

        // Reject dangerous ground types
        if (isDangerous(groundState)) return false;
        if (isDangerous(feetState)) return false;

        return true;
    }

    private boolean isDangerous(BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block == net.minecraft.world.level.block.Blocks.LAVA
                || block == net.minecraft.world.level.block.Blocks.WATER
                || block == net.minecraft.world.level.block.Blocks.FIRE
                || block == net.minecraft.world.level.block.Blocks.SOUL_FIRE
                || block == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK
                || block == net.minecraft.world.level.block.Blocks.CACTUS
                || block == net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH
                || block == net.minecraft.world.level.block.Blocks.WITHER_ROSE
                || block == net.minecraft.world.level.block.Blocks.NETHER_PORTAL;
    }

    /**
     * Equally distributed offset using the 4-rotation rectangle method (from Essentials).
     */
    private double[] randomOffset(double minRange, double maxRange) {
        double rectX = RANDOM.nextDouble() * (maxRange - minRange) + minRange;
        double rectZ = RANDOM.nextDouble() * (maxRange + minRange) - minRange;

        int transform = RANDOM.nextInt(4);
        double offX, offZ;
        switch (transform) {
            case 0: offX = rectX;  offZ = rectZ;  break;
            case 1: offX = -rectZ; offZ = rectX;  break;
            case 2: offX = -rectX; offZ = -rectZ; break;
            default: offX = rectZ;  offZ = -rectX; break;
        }
        return new double[]{offX, offZ};
    }

    private double clampToWorldBorder(ServerLevel level, double coord, boolean isX) {
        var border = level.getWorldBorder();
        double cx = border.getCenterX();
        double cz = border.getCenterZ();
        double half = border.getSize() / 2.0;
        if (isX) {
            return Math.max(cx - half, Math.min(cx + half, coord));
        } else {
            return Math.max(cz - half, Math.min(cz + half, coord));
        }
    }

    private boolean isValid(TeleportLocation loc, String locationName) {
        ServerLevel level = loc.getLevel();
        if (level == null) return false;
        if (loc.getY() <= com.zerog.neoessentials.util.LevelHeightCompat.minBuildHeight(level)) return false;

        // Excluded biomes check
        if (locationName != null) {
            List<String> excluded = getExcludedBiomes(locationName);
            if (!excluded.isEmpty()) {
                BlockPos pos = new BlockPos((int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
                String biomeName = getBiomeName(level, pos);
                if (biomeName != null && excluded.contains(biomeName.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getBiomeName(ServerLevel level, BlockPos pos) {
        try {
            var biomeHolder = level.getBiome(pos);
            return biomeHolder.unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse(null);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "getBiomeName failed at {}: {}", pos, e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Cache
    // -----------------------------------------------------------------------

    private ConcurrentLinkedQueue<TeleportLocation> getCache(String name) {
        return locationCache.computeIfAbsent(name, k -> new ConcurrentLinkedQueue<>());
    }

    private void prewarmCache(ServerLevel level, String name) {
        int threshold = getCacheThreshold();
        int current = getCache(name).size();
        if (current >= threshold) return;

        int toFill = threshold - current;
        double[] center = getCenter(level, name);
        double minRange = getMinRange(name);
        double maxRange = getMaxRange(level, name);

        for (int i = 0; i < toFill; i++) {
            double[] offset = randomOffset(minRange, maxRange);
            double rx = clampToWorldBorder(level, center[0] + offset[0], true);
            double rz = clampToWorldBorder(level, center[2] + offset[1], false);
            final double finalRx = rx;
            final double finalRz = rz;
            CompletableFuture.runAsync(() -> {
                TeleportLocation loc = findSafeY(level, finalRx, finalRz, name);
                if (loc != null && isValid(loc, name)) {
                    getCache(name).add(loc);
                }
            }, level.getServer());
        }
    }

    public void clearCache() {
        locationCache.clear();
    }

    public void clearCache(String name) {
        ConcurrentLinkedQueue<TeleportLocation> q = locationCache.get(name);
        if (q != null) q.clear();
    }

    // -----------------------------------------------------------------------
    // Config helpers
    // -----------------------------------------------------------------------

    private String resolveDefaultName(ServerPlayer player) {
        String def = getConfigString("defaultLocation", "{world}");
        return def.replace("{world}", player.level().dimension().location().toString());
    }

    private double[] getCenter(ServerLevel level, String name) {
        JsonObject tpr = getTprConfig();
        if (tpr != null && tpr.has("locations")) {
            JsonObject locs = tpr.getAsJsonObject("locations");
            if (locs.has(name) && locs.getAsJsonObject(name).has("center")) {
                JsonObject c = locs.getAsJsonObject(name).getAsJsonObject("center");
                double x = c.has("x") ? c.get("x").getAsDouble() : 0;
                double y = c.has("y") ? c.get("y").getAsDouble() : 64;
                double z = c.has("z") ? c.get("z").getAsDouble() : 0;
                return new double[]{x, y, z};
            }
        }
        // Fallback: world border center
        var border = level.getWorldBorder();
        return new double[]{border.getCenterX(), level.getSeaLevel(), border.getCenterZ()};
    }

    private double getMinRange(String name) {
        JsonObject tpr = getTprConfig();
        if (tpr != null && tpr.has("locations")) {
            JsonObject locs = tpr.getAsJsonObject("locations");
            if (locs.has(name) && locs.getAsJsonObject(name).has("minRange")) {
                return locs.getAsJsonObject(name).get("minRange").getAsDouble();
            }
        }
        double def = getConfigDouble("defaultMinRange", 0);
        return def;
    }

    private double getMaxRange(ServerLevel level, String name) {
        JsonObject tpr = getTprConfig();
        if (tpr != null && tpr.has("locations")) {
            JsonObject locs = tpr.getAsJsonObject("locations");
            if (locs.has(name) && locs.getAsJsonObject(name).has("maxRange")) {
                return locs.getAsJsonObject(name).get("maxRange").getAsDouble();
            }
        }
        double def = getConfigDouble("defaultMaxRange", -1);
        if (def <= 0) {
            // Use half the world border size
            return level.getWorldBorder().getSize() / 2.0;
        }
        return def;
    }

    private int getFindAttempts() {
        return (int) getConfigDouble("findAttempts", 10);
    }

    private int getCacheThreshold() {
        return (int) getConfigDouble("cacheThreshold", 10);
    }

    private int getTprCooldown() {
        return (int) getConfigDouble("cooldown", 60);
    }

    /** Whether bare {@code /rtp} (no explicit location argument) should open the biome-select
     *  GUI instead of instantly teleporting — off ("command") by default, byte-for-byte the
     *  original behavior. */
    public boolean isGuiMode() {
        return "gui".equalsIgnoreCase(getConfigString("mode", "command"));
    }

    private int getBiomeSearchRadius() {
        return (int) getConfigDouble("biomeSearchRadius", 6400);
    }

    private int getBiomeSearchStep() {
        return (int) getConfigDouble("biomeSearchStep", 32);
    }

    private int getTeleportDelaySecs() {
        try {
            JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("generalSettings")) {
                    JsonObject gs = tp.getAsJsonObject("generalSettings");
                    if (gs.has("teleportDelay")) return gs.get("teleportDelay").getAsInt();
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.TELEPORTATION,
                "Failed to read teleportation.generalSettings.teleportDelay, defaulting to 3", e);
        }
        return 3;
    }

    private List<String> getExcludedBiomes(String name) {
        List<String> result = new ArrayList<>();
        JsonObject tpr = getTprConfig();
        if (tpr == null) return result;

        // Per-location excluded biomes
        if (tpr.has("locations")) {
            JsonObject locs = tpr.getAsJsonObject("locations");
            if (locs.has(name) && locs.getAsJsonObject(name).has("excludedBiomes")) {
                JsonArray arr = locs.getAsJsonObject(name).getAsJsonArray("excludedBiomes");
                arr.forEach(e -> result.add(e.getAsString().toLowerCase()));
                return result;
            }
        }

        // Global excluded biomes
        if (tpr.has("excludedBiomes")) {
            JsonArray arr = tpr.getAsJsonArray("excludedBiomes");
            arr.forEach(e -> result.add(e.getAsString().toLowerCase()));
        }
        return result;
    }

    private JsonObject getTprConfig() {
        try {
            JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (config.has("teleportation")) {
                JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("randomTeleportSettings")) {
                    return tp.getAsJsonObject("randomTeleportSettings");
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.TELEPORTATION,
                "Failed to read teleportation.randomTeleportSettings", e);
        }
        return null;
    }

    private double getConfigDouble(String key, double def) {
        JsonObject tpr = getTprConfig();
        if (tpr != null && tpr.has(key)) {
            return tpr.get(key).getAsDouble();
        }
        return def;
    }

    private String getConfigString(String key, String def) {
        JsonObject tpr = getTprConfig();
        if (tpr != null && tpr.has(key)) {
            return tpr.get(key).getAsString();
        }
        return def;
    }
}

