package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyLeaderboard {
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
     * Format the leaderboard for display.
     * @param topN Number of top players to display
     * @return List of formatted leaderboard strings
     */
    public static List<String> formatLeaderboard(int topN) {
        List<Map.Entry<UUID, BigDecimal>> top = getTopPlayers(topN);
        List<String> lines = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<UUID, BigDecimal> entry : top) {
            lines.add(MessageUtil.localize("commands.neoessentials.economy.leaderboard_entry", 
                rank++, entry.getKey(), entry.getValue().toPlainString()));
        }
        return lines;
    }
}