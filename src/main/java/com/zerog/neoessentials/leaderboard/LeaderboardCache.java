package com.zerog.neoessentials.leaderboard;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-board async cache — the generalized lift of {@code BaltopCommand}'s static cache
 * fields into one instance per registered board, so N boards don't each duplicate their own
 * copy of the same caching machinery. Same 60s staleness window, same
 * build-in-flight/rebuild-queued handling for invalidations that land mid-build.
 */
public class LeaderboardCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardCache.class);
    private static final long STALE_MS = 60_000L;

    public record Entry(UUID uuid, String name, Number value) {}

    private final LeaderboardDefinition definition;
    private final StatProvider provider;

    private volatile List<Entry> cachedTop = Collections.emptyList();
    private volatile long cacheAge = 0L;
    private final AtomicBoolean building = new AtomicBoolean(false);
    private final AtomicBoolean rebuildQueued = new AtomicBoolean(false);
    private volatile MinecraftServer lastServer;

    public LeaderboardCache(LeaderboardDefinition definition, StatProvider provider) {
        this.definition = definition;
        this.provider = provider;
    }

    public LeaderboardDefinition getDefinition() { return definition; }
    public StatProvider getProvider() { return provider; }
    public boolean isBuilding() { return building.get(); }
    public long getCacheAgeMs() { return System.currentTimeMillis() - cacheAge; }

    /** Current cached top list, refreshing asynchronously in the background if stale/empty. */
    public List<Entry> getTop(MinecraftServer server) {
        if (System.currentTimeMillis() - cacheAge > STALE_MS || cachedTop.isEmpty()) {
            refreshAsync(server);
        }
        return cachedTop;
    }

    public List<Entry> getPage(MinecraftServer server, int page, int pageSize) {
        List<Entry> top = getTop(server);
        if (top.isEmpty()) return top;
        int totalPages = (int) Math.ceil((double) top.size() / pageSize);
        int clampedPage = Math.max(1, Math.min(page, totalPages));
        int start = (clampedPage - 1) * pageSize;
        int end = Math.min(start + pageSize, top.size());
        return top.subList(start, end);
    }

    public int getTotalPages(int pageSize) {
        List<Entry> top = cachedTop;
        return Math.max(1, (int) Math.ceil((double) top.size() / pageSize));
    }

    public CompletableFuture<Void> refreshAsync(MinecraftServer server) {
        lastServer = server;
        if (!building.compareAndSet(false, true)) {
            rebuildQueued.set(true);
            return CompletableFuture.completedFuture(null);
        }
        return runBuild(server);
    }

    private CompletableFuture<Void> runBuild(MinecraftServer server) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<UUID, Number> all = provider.getAllValues(server);
                List<Entry> entries = new CopyOnWriteArrayList<>();

                for (Map.Entry<UUID, Number> e : all.entrySet()) {
                    if (definition.exemptPermissionSuffix() != null
                            && PermissionAPI.hasPermission(e.getKey(), definition.exemptPermissionSuffix())) {
                        continue;
                    }
                    String displayName = e.getKey().toString();
                    try {
                        var profile = server.getProfileCache().get(e.getKey());
                        if (profile.isPresent() && profile.get().getName() != null) {
                            displayName = profile.get().getName();
                        }
                    } catch (Exception ignored) {
                        NeoLog.debug(LOGGER, LogCategory.GENERAL,
                            "LeaderboardCache[{}]: failed to resolve name for {}", definition.id(), e.getKey(), ignored);
                    }
                    entries.add(new Entry(e.getKey(), displayName, e.getValue()));
                }

                entries.sort((a, b) -> {
                    int cmp = Double.compare(a.value().doubleValue(), b.value().doubleValue());
                    return definition.higherIsBetter() ? -cmp : cmp;
                });

                cachedTop = Collections.unmodifiableList(entries);
                cacheAge = System.currentTimeMillis();
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "LeaderboardCache[{}]: rebuilt — {} entries", definition.id(), entries.size());
            } finally {
                building.set(false);
            }
            if (rebuildQueued.compareAndSet(true, false) && building.compareAndSet(false, true)) {
                runBuild(server);
            }
        });
    }

    public void invalidate() {
        cacheAge = 0L;
        MinecraftServer server = lastServer;
        if (server != null) refreshAsync(server);
    }
}
