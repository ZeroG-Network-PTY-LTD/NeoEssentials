package com.zerog.neoessentials.leaderboard;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of leaderboard boards. Any stat source (economy, a vanilla stat, a custom
 * point total, or a board registered by another mod via {@link LeaderboardAPI}) registers a
 * {@link StatProvider} here under a {@link LeaderboardDefinition}; {@code /leaderboard} and
 * {@code LeaderboardPlaceholderExpansion} both read through this registry rather than
 * knowing about individual stat sources.
 */
public class LeaderboardManager {
    private static final LeaderboardManager INSTANCE = new LeaderboardManager();
    public static LeaderboardManager getInstance() { return INSTANCE; }

    private final Map<String, LeaderboardCache> boards = new LinkedHashMap<>();
    /** Ids that came from leaderboard.json — {@link #clearConfigManagedBoards()} only
     *  touches these, so a {@code /leaderboard reload} never wipes out boards another mod
     *  registered via {@link LeaderboardAPI} at its own startup (those aren't in the config
     *  file at all, and won't re-register themselves on every reload). */
    private final Set<String> configManagedIds = new LinkedHashSet<>();

    private LeaderboardManager() {}

    public void registerBoard(LeaderboardDefinition definition, StatProvider provider) {
        registerBoard(definition, provider, false);
    }

    public void registerBoard(LeaderboardDefinition definition, StatProvider provider, boolean configManaged) {
        String id = definition.id().toLowerCase();
        boards.put(id, new LeaderboardCache(definition, provider));
        if (configManaged) configManagedIds.add(id);
        else configManagedIds.remove(id);
    }

    public boolean unregisterBoard(String id) {
        if (id == null) return false;
        String key = id.toLowerCase();
        configManagedIds.remove(key);
        return boards.remove(key) != null;
    }

    /** Removes only the boards {@code LeaderboardConfigLoader} registered from
     *  leaderboard.json, ahead of re-reading it — leaves API-registered boards untouched. */
    public void clearConfigManagedBoards() {
        for (String id : List.copyOf(configManagedIds)) boards.remove(id);
        configManagedIds.clear();
    }

    public LeaderboardCache getBoard(String id) {
        return id == null ? null : boards.get(id.toLowerCase());
    }

    public boolean isConfigManaged(String id) {
        return id != null && configManagedIds.contains(id.toLowerCase());
    }

    public List<String> getRegisteredBoardIds() {
        return List.copyOf(boards.keySet());
    }
}
