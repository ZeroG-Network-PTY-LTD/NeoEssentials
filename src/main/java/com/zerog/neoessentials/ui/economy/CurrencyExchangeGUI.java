package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Currency Exchange GUI (placeholder)
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class CurrencyExchangeGUI {
    
    public static void openCurrencyExchangeGUI(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "neoessentials.economy.exchange_coming_soon");
    }
}
