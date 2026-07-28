package com.zerog.neoessentials.teams;

import java.util.List;
import java.util.UUID;

/**
 * Aggregates all known {@link TeamProviderAdapter}s so the team chat channel doesn't need
 * to know which specific team mod is installed. Add a new provider here to support another
 * team mod (Towns and Nations, SimpleTeams, etc.) alongside FTB Teams.
 */
public class TeamManager {
    private static final TeamManager INSTANCE = new TeamManager();

    private final List<TeamProviderAdapter> adapters = List.of(
            new FtbTeamsAdapter()
    );

    private TeamManager() {}

    public static TeamManager getInstance() {
        return INSTANCE;
    }

    /** True if at least one supported team mod is installed and its API resolved. */
    public boolean isAnyProviderAvailable() {
        for (TeamProviderAdapter adapter : adapters) {
            if (adapter.isAvailable()) return true;
        }
        return false;
    }

    /** The player's team id from the first available provider that has one, else null. */
    public String getTeamId(UUID playerUUID) {
        for (TeamProviderAdapter adapter : adapters) {
            if (!adapter.isAvailable()) continue;
            String teamId = adapter.getTeamId(playerUUID);
            if (teamId != null) return teamId;
        }
        return null;
    }
}
