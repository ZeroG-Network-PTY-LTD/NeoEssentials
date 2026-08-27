package com.zerog.neoessentials.leaderboard.adapters;

import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.leaderboard.StatProvider;
import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Wraps the existing economy balances as a leaderboard stat — one-line adapter proving the
 *  generalized framework reproduces {@code /baltop} with no behavior change. */
public class EconomyStatProvider implements StatProvider {
    @Override
    public Map<UUID, Number> getAllValues(MinecraftServer server) {
        Map<UUID, Number> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, BigDecimal> e : EconomyManager.getInstance().getAllBalances().entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    @Override
    public String formatValue(Number value) {
        return EconomyManager.getInstance().getCurrencySymbol() + String.format("%.2f", value.doubleValue());
    }
}
