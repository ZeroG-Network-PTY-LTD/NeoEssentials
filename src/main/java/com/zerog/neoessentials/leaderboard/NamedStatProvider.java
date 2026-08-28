package com.zerog.neoessentials.leaderboard;

import net.minecraft.server.MinecraftServer;

import java.util.Map;

/**
 * A {@link StatProvider} variant for boards whose entries aren't players — a shop ranked by
 * total sales, a faction, a team — anything with a stable id and its own display name instead
 * of a player UUID resolved through a Mojang profile lookup. {@link LeaderboardCache} checks
 * for this interface before falling back to the UUID/profile-lookup path {@link StatProvider}
 * implementations use.
 */
public interface NamedStatProvider extends StatProvider {
    record NamedEntry(String displayName, Number value) {}

    /** Every current entry for this stat, keyed by a stable opaque id (NOT a player UUID). */
    Map<String, NamedEntry> getAllNamedValues(MinecraftServer server);

    /** Unused for named boards — {@link LeaderboardCache} never calls this when a provider
     *  implements {@link NamedStatProvider}. */
    @Override
    default Map<java.util.UUID, Number> getAllValues(MinecraftServer server) {
        return Map.of();
    }
}
