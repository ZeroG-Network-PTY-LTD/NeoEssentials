package com.zerog.neoessentials.leaderboard;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.UUID;

/**
 * A single ranked statistic a {@link LeaderboardManager} board is built from — economy
 * balances, a vanilla stat (kills, playtime, ...), or any future subsystem. Implementations
 * should be cheap-per-call full scans; {@link LeaderboardCache} is what adds the caching
 * layer on top, mirroring the same 60s-staleness pattern the original
 * {@code economy/commands/BaltopCommand.java} used for balances alone.
 */
public interface StatProvider {
    /** Every known player's current value for this stat. Offline players are included where
     *  the underlying data source allows it (e.g. economy balances, vanilla stat files). */
    Map<UUID, Number> getAllValues(MinecraftServer server);

    /** Formats a raw value for display (currency symbol, hours/minutes, plain integer, ...). */
    String formatValue(Number value);
}
