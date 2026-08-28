package com.zerog.neoessentials.leaderboard;

/**
 * Per-rank medal/color helpers shared by {@link LeaderboardPlaceholderExpansion} (so holograms/
 * scoreboard/tablist lines get automatic rank styling), the {@code /leaderboard} chat command's
 * template rendering, and the leaderboard GUI — one place for rank→style logic instead of each
 * consumer reimplementing it.
 */
public final class LeaderboardStyle {
    private LeaderboardStyle() {}

    /** Unicode medal for ranks 1-3, empty string otherwise. */
    public static String medal(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "";
        };
    }

    /** A {@code RichTextFormatter} {@code <color:#RRGGBB>} tag for ranks 1-3 (gold/silver/
     *  bronze), empty string otherwise — meant to be substituted directly into a template
     *  string that's later run through {@code RichTextFormatter.processTablistText}. */
    public static String rankColorTag(int rank) {
        return switch (rank) {
            case 1 -> "<color:#FFD700>";
            case 2 -> "<color:#C0C0C0>";
            case 3 -> "<color:#CD7F32>";
            default -> "";
        };
    }
}
