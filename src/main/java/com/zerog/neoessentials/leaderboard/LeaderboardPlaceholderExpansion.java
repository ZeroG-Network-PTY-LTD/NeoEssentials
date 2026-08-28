package com.zerog.neoessentials.leaderboard;

import com.zerog.neoessentials.api.PlaceholderExpansion;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Exposes leaderboard rank/name/value/medal/rank-color as placeholders:
 * {@code {leaderboard_<board>:<rank>:name}} / {@code :value} / {@code :medal} (🥇🥈🥉 on ranks
 * 1-3, empty otherwise) / {@code :rankcolor} (a gold/silver/bronze color tag on ranks 1-3)
 * e.g. {@code {leaderboard_kills:1:name}}. Intended for consumption by the sidebar
 * scoreboard's line config, or anywhere else placeholders are supported.
 */
public class LeaderboardPlaceholderExpansion extends PlaceholderExpansion {
    @Override
    public String getIdentifier() { return "leaderboard"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public String getAuthor() { return "ZeroG Network"; }

    @Override
    public Set<String> getPlaceholders() {
        return Set.copyOf(LeaderboardManager.getInstance().getRegisteredBoardIds());
    }

    @Nullable
    @Override
    public String onPlaceholderRequest(@Nullable ServerPlayer player, String identifier, @Nullable String params) {
        if (params == null) return null;
        String[] parts = params.split(":", 2);
        if (parts.length != 2) return null;

        int rank;
        try {
            rank = Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        String field = parts[1].trim();

        LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(identifier);
        if (cache == null || player == null) return null;

        List<LeaderboardCache.Entry> top = cache.getTop(player.level().getServer());
        if (rank < 1 || rank > top.size()) return "";
        LeaderboardCache.Entry entry = top.get(rank - 1);

        return switch (field) {
            case "name" -> entry.name();
            case "value" -> cache.getProvider().formatValue(entry.value());
            case "medal" -> LeaderboardStyle.medal(rank);
            case "rankcolor" -> LeaderboardStyle.rankColorTag(rank);
            default -> null;
        };
    }
}
