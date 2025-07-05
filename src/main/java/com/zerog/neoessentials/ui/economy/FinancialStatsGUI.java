package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Financial Stats GUI (placeholder)
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class FinancialStatsGUI {
    
    public static void openFinancialStatsGUI(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "neoessentials.economy.stats_coming_soon");
    }
}
