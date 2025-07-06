package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Currency Exchange GUI for managing different currencies
 * Simplified chat-based interface for reliability
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class CurrencyExchangeGUI {
    
    /**
     * Opens a simplified currency exchange GUI (chat-based)
     */
    public static void openCurrencyExchangeGUI(ServerPlayer player) {
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        try {
            double balance = economyManager.getBalance(player.getUUID());
            String currency = "$"; // Default currency symbol
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.currency_exchange_header");
            LanguageUtil.sendMessage(player, "neoessentials.economy.current_balance", String.format("%s%.2f", currency, balance));
            LanguageUtil.sendMessage(player, "neoessentials.economy.currency_exchange_info");
            LanguageUtil.sendMessage(player, "neoessentials.economy.available_currencies");
            LanguageUtil.sendMessage(player, "neoessentials.economy.currency_primary", currency + " (Primary)");
            LanguageUtil.sendMessage(player, "neoessentials.economy.exchange_coming_soon");
            
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.exchange_error", e.getMessage());
        }
    }
}
