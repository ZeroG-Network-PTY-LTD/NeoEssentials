package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * /baltop [page] — Balance leaderboard, ported from EssentialsX BalanceTopImpl.
 *
 * Improvements over old implementation:
 *  - Async cache (recalculated in background, never blocks the server thread)
 *  - Cache age displayed so admins know how fresh the data is
 *  - Pagination support: /baltop [page]
 *  - Total economy wealth shown at footer
 *  - Exempt players (neoessentials.economy.baltop.exempt) excluded from ranking
 *  - Player names resolved from profile cache, not raw UUIDs
 *  - Configurable page size (default 10, matches Essentials)
 */
public class BaltopCommand {

    private static final int PAGE_SIZE = 10;

    // ── Cache (BalanceTopImpl port) ──────────────────────────────────────────
    private static volatile List<BaltopEntry> cachedTop = Collections.emptyList();
    private static volatile BigDecimal cachedTotal = BigDecimal.ZERO;
    private static volatile long cacheAge = 0L;
    private static final AtomicBoolean cacheBuilding = new AtomicBoolean(false);
    // Set whenever invalidateCache() fires while a build is already in flight — that build
    // started reading balancesCache before the invalidating change landed, so its result is
    // stale the moment it finishes. Checked at the end of every build; if set, another build
    // is kicked off immediately instead of the invalidation being silently dropped (previously:
    // a rapid invalidate-while-building could leave the cache stuck on old data indefinitely,
    // since compareAndSet(false, true) just no-ops when a build is already running).
    private static final AtomicBoolean rebuildQueued = new AtomicBoolean(false);
    private static volatile MinecraftServer lastServer = null;

    /** Immutable leaderboard entry. */
    private record BaltopEntry(UUID uuid, String name, BigDecimal balance) {}

    // ── Registration ─────────────────────────────────────────────────────────
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"baltop", "balancetop", "btop"}) {
            dispatcher.register(Commands.literal(name)
                .requires(src -> {
                    var player = src.getPlayer();
                    return player == null
                        || com.zerog.neoessentials.api.permissions.PermissionAPI
                            .hasPermission(player.getUUID(), "neoessentials.economy.baltop");
                })
                .executes(ctx -> execute(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> execute(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "page"))))
            );
        }
    }

    // ── Command handler ───────────────────────────────────────────────────────
    private static int execute(CommandSourceStack source, int page) {
        if (!EconomyManager.getInstance().isEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.eco.disabled"));
            return 0;
        }

        // If cache is stale (>60 s) or empty, rebuild asynchronously
        if (System.currentTimeMillis() - cacheAge > 60_000L || cachedTop.isEmpty()) {
            refreshCacheAsync(source.getServer());
        }

        List<BaltopEntry> top = cachedTop;

        if (top.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.baltop.empty"), false);
            return 1;
        }

        int totalPages = (int) Math.ceil((double) top.size() / PAGE_SIZE);
        int clampedPage = Math.max(1, Math.min(page, totalPages));
        int start = (clampedPage - 1) * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, top.size());

        String currency = EconomyManager.getInstance().getCurrencySymbol();
        long ageSeconds = (System.currentTimeMillis() - cacheAge) / 1000L;

        source.sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.baltop.header", clampedPage, totalPages, ageSeconds), false);

        for (int i = start; i < end; i++) {
            BaltopEntry entry = top.get(i);
            final int rank = i + 1;
            source.sendSuccess(() -> MessageUtil.info(
                "commands.neoessentials.baltop.entry", rank, entry.name(), entry.balance(), currency), false);
        }

        final BigDecimal total = cachedTotal;
        source.sendSuccess(() -> MessageUtil.info(
            "commands.neoessentials.baltop.total", total, currency), false);

        if (cacheBuilding.get()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.baltop.refreshing"), false);
        }

        return 1;
    }

    // ── Async cache rebuild (BalanceTopImpl.calculateBalanceTopMapAsync port) ─
    public static CompletableFuture<Void> refreshCacheAsync(MinecraftServer server) {
        lastServer = server;
        if (!cacheBuilding.compareAndSet(false, true)) {
            // A build is already in flight. It started reading balancesCache before whatever
            // just called refreshCacheAsync() (e.g. this /eco give) landed, so mark that its
            // result will be stale — the in-flight build re-checks this flag when it finishes
            // and immediately re-runs itself if set, instead of this invalidation being lost.
            rebuildQueued.set(true);
            return CompletableFuture.completedFuture(null);
        }
        return runBuild(server);
    }

    private static CompletableFuture<Void> runBuild(MinecraftServer server) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<UUID, BigDecimal> all = EconomyManager.getInstance().getAllBalances();
                List<BaltopEntry> entries = new CopyOnWriteArrayList<>();
                BigDecimal total = BigDecimal.ZERO;

                for (Map.Entry<UUID, BigDecimal> e : all.entrySet()) {
                    // Skip exempt players
                    if (com.zerog.neoessentials.api.permissions.PermissionAPI
                            .hasPermission(e.getKey(), "neoessentials.economy.baltop.exempt")) {
                        continue;
                    }
                    // Resolve display name
                    String displayName = e.getKey().toString(); // fallback
                    try {
                        var profile = server.services().nameToIdCache().get(e.getKey());
                        if (profile.isPresent() && profile.get().name() != null) {
                            displayName = profile.get().name();
                        }
                    } catch (Exception ignored) {}

                    entries.add(new BaltopEntry(e.getKey(), displayName, e.getValue()));
                    total = total.add(e.getValue());
                }

                // Sort descending (Essentials: entries.sort by balance desc)
                entries.sort((a, b) -> b.balance().compareTo(a.balance()));

                cachedTop   = Collections.unmodifiableList(entries);
                cachedTotal = total;
                cacheAge    = System.currentTimeMillis();
            } finally {
                cacheBuilding.set(false);
            }
            // If an invalidation arrived while we were building, our just-published result is
            // already stale — immediately rebuild again rather than waiting for the next
            // /baltop call (which, under the old cacheAge>60s check alone, could be minutes away).
            if (rebuildQueued.compareAndSet(true, false)
                    && cacheBuilding.compareAndSet(false, true)) {
                runBuild(server);
            }
        });
    }

    /** Invalidate cache (call after eco give/take/set). */
    public static void invalidateCache() {
        cacheAge = 0L;
        // Proactively kick off a rebuild now rather than waiting for the next /baltop call to
        // notice staleness — that could otherwise be minutes away, or (for a brand-new account
        // that's never been in the leaderboard before) might not happen until someone thinks to
        // run /baltop again at all. Safe to call before any /baltop has ever run: lastServer is
        // simply null until then, and refreshCacheAsync() itself sets rebuildQueued if a build
        // is already in flight rather than dropping this invalidation.
        MinecraftServer server = lastServer;
        if (server != null) {
            refreshCacheAsync(server);
        }
    }
}
