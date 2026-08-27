package com.zerog.neoessentials.leaderboard;

/**
 * Lets other mods register their own leaderboard boards — same one-line integration story
 * as {@link com.zerog.neoessentials.api.PlaceholderAPI#registerExpansion}.
 *
 * <pre>{@code
 * // Register a board (call during your mod's own init/server-starting, since boards
 * // registered this way are in-memory only — NOT written to leaderboard.json, and won't
 * // survive a /leaderboard reload unless you re-register on your own startup hook):
 * LeaderboardAPI.registerBoard(
 *     new LeaderboardDefinition("mymod_wins", "Arena Wins", "mymod.leaderboard.exempt", true),
 *     (server) -> myWinsMap()); // StatProvider: Map<UUID, Number> getAllValues(MinecraftServer)
 * }</pre>
 *
 * @see StatProvider
 * @see LeaderboardDefinition
 */
public interface LeaderboardAPI {

    /** Registers a board. Overwrites any existing board with the same id. */
    static boolean registerBoard(LeaderboardDefinition definition, StatProvider provider) {
        LeaderboardManager.getInstance().registerBoard(definition, provider);
        return true;
    }

    /** Unregisters a board by id. Returns false if no board with that id was registered. */
    static boolean unregisterBoard(String id) {
        return LeaderboardManager.getInstance().unregisterBoard(id);
    }

    /** Looks up a registered board's cache (for reading current standings), or null. */
    static LeaderboardCache getBoard(String id) {
        return LeaderboardManager.getInstance().getBoard(id);
    }
}
