package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyLeaderboard {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyLeaderboard.class);

    /**
     * Get the top N players by balance.
     * @param topN Number of top players to return
     * @return List of Map.Entry<UUID, BigDecimal> sorted by balance descending
     */
    public static List<Map.Entry<UUID, BigDecimal>> getTopPlayers(int topN) {
        Map<UUID, BigDecimal> allBalances = EconomyManager.getInstance().getAllBalances();
        return allBalances.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(topN)
                .toList();
    }

    /**
     * Format the leaderboard for display using raw UUIDs (no server access).
     * For player-name-resolved display, prefer {@link #formatLeaderboard(int, MinecraftServer)}.
     * @param topN Number of top players to display
     * @return List of formatted leaderboard strings showing UUID + balance
     */
    public static List<String> formatLeaderboard(int topN) {
        List<Map.Entry<UUID, BigDecimal>> top = getTopPlayers(topN);
        List<String> lines = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<UUID, BigDecimal> entry : top) {
            lines.add(MessageUtil.localize("commands.neoessentials.economy.leaderboard_entry",
                rank++, entry.getKey().toString(), entry.getValue().toPlainString()));
        }
        return lines;
    }

    /**
     * Format the leaderboard for display with player name resolution.
     * @param topN   Number of top players to display
     * @param server MinecraftServer used to look up player names from the profile cache
     * @return List of formatted leaderboard strings showing player name + balance
     */
    public static List<String> formatLeaderboard(int topN, MinecraftServer server) {
        List<Map.Entry<UUID, BigDecimal>> top = getTopPlayers(topN);
        List<String> lines = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<UUID, BigDecimal> entry : top) {
            String displayName = entry.getKey().toString(); // UUID fallback
            try {
                if (server != null) {
                    var profile = server.services().nameToIdCache().get(entry.getKey());
                    if (profile.isPresent() && profile.get().name() != null) {
                        displayName = profile.get().name();
                    }
                }
            } catch (Exception ignored) {
                NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                    "formatLeaderboard: failed to resolve player name for " + entry.getKey() + ", falling back to UUID", ignored);
            }
            lines.add(MessageUtil.localize("commands.neoessentials.economy.leaderboard_entry",
                rank++, displayName, entry.getValue().toPlainString()));
        }
        return lines;
    }
}