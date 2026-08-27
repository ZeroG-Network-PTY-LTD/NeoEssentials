package com.zerog.neoessentials.leaderboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of leaderboard boards. Any stat source (economy, a vanilla stat, or a future
 * subsystem) registers a {@link StatProvider} here under a {@link LeaderboardDefinition};
 * {@code /leaderboard} and {@code LeaderboardPlaceholderExpansion} both read through this
 * registry rather than knowing about individual stat sources.
 */
public class LeaderboardManager {
    private static final LeaderboardManager INSTANCE = new LeaderboardManager();
    public static LeaderboardManager getInstance() { return INSTANCE; }

    private final Map<String, LeaderboardCache> boards = new LinkedHashMap<>();

    private LeaderboardManager() {}

    public void registerBoard(LeaderboardDefinition definition, StatProvider provider) {
        boards.put(definition.id().toLowerCase(), new LeaderboardCache(definition, provider));
    }

    public LeaderboardCache getBoard(String id) {
        return id == null ? null : boards.get(id.toLowerCase());
    }

    public List<String> getRegisteredBoardIds() {
        return List.copyOf(boards.keySet());
    }
}
