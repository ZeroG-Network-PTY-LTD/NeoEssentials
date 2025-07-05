package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bank Account GUI for managing bank accounts
 * Simplified chat-based interface for reliability
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class BankAccountGUI {
    
    /**
     * Opens a simplified bank account GUI (chat-based for now)
     */
    public static void openBankAccountGUI(ServerPlayer player) {
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        try {
            double balance = economyManager.getBalance(player.getUUID());
            String currency = "$"; // Default currency symbol
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_feature_placeholder");
            LanguageUtil.sendMessage(player, "neoessentials.economy.current_cash_balance", String.format("%s%.2f", currency, balance));
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_account_info");
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_features_preview");
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_feature_savings");
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_feature_interest");
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_feature_loans");
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_coming_soon");
            
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_error", e.getMessage());
        }
    }
}
