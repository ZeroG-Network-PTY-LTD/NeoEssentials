package com.zerog.neoessentials.leaderboard;

/**
 * @param id                     board id, used in commands/placeholders (e.g. "money", "kills")
 * @param displayName            human-readable name shown in headers
 * @param exemptPermissionSuffix full exemption permission node (e.g. "neoessentials.economy.baltop.exempt")
 * @param higherIsBetter         true for "top" boards (kills, money); false would rank ascending
 * @param refreshIntervalSeconds how long {@link LeaderboardCache} serves a stale cached ranking
 *                               before rebuilding on next access — a cheap board (in-memory
 *                               economy balances) can afford a short interval; an expensive one
 *                               (a full offline-player stats-directory scan) may want it longer
 * @param entryFormat            optional per-board template for one {@code /leaderboard} entry
 *                               line — tokens {@code {rank}/{name}/{value}/{medal}/{rankColor}},
 *                               resolved via {@link LeaderboardStyle} and run through
 *                               {@code RichTextFormatter.processTablistText}. {@code null} keeps
 *                               the default lang-key-driven line.
 * @param headerFormat           optional per-board template for the header line — same tokens
 *                               plus {@code {displayName}/{page}/{totalPages}/{age}}.
 *                               {@code null} keeps the default lang-key-driven header.
 * @param icon                   item id (e.g. {@code "minecraft:emerald"}) used as the GUI icon
 *                               for entries with no player UUID (e.g. a shop). {@code null}
 *                               falls back to {@code minecraft:paper}.
 */
public record LeaderboardDefinition(String id, String displayName, String exemptPermissionSuffix,
                                     boolean higherIsBetter, int refreshIntervalSeconds,
                                     String entryFormat, String headerFormat, String icon) {
    public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 60;

    /** Uses {@link #DEFAULT_REFRESH_INTERVAL_SECONDS} and no custom styling — kept for
     *  external mods/existing callers built against the pre-styling 5-arg constructor. */
    public LeaderboardDefinition(String id, String displayName, String exemptPermissionSuffix,
                                  boolean higherIsBetter, int refreshIntervalSeconds) {
        this(id, displayName, exemptPermissionSuffix, higherIsBetter, refreshIntervalSeconds, null, null, null);
    }

    /** Uses {@link #DEFAULT_REFRESH_INTERVAL_SECONDS} — kept for external mods/existing
     *  callers built against the pre-refresh-interval 4-arg constructor. */
    public LeaderboardDefinition(String id, String displayName, String exemptPermissionSuffix, boolean higherIsBetter) {
        this(id, displayName, exemptPermissionSuffix, higherIsBetter, DEFAULT_REFRESH_INTERVAL_SECONDS);
    }
}
