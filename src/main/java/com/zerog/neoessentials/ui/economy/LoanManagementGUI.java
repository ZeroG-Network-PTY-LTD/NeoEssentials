package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loan Management GUI for handling loans and credit
 * Simplified chat-based interface for reliability
 * 
 * @author ZeroG
 * @since 1.0.2.133
 */
public class LoanManagementGUI {
    
    /**
     * Opens a simplified loan management GUI (chat-based)
     */
    public static void openLoanManagementGUI(ServerPlayer player) {
        try {
            EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            double balance = economyManager.getBalance(player.getUUID());
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_header");
            LanguageUtil.sendMessage(player, "neoessentials.economy.current_balance", String.format("$%.2f", balance));
            
            // Display loan system preview
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_preview_header");
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_feature_basic");
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_feature_interest");
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_feature_collateral");
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_feature_repayment");
            
            // Show eligibility information
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_eligibility_header");
            if (balance >= 100.0) {
                LanguageUtil.sendMessage(player, "neoessentials.economy.loans_eligibility_qualified");
                double maxLoan = balance * 2.0; // Example: 2x current balance
                LanguageUtil.sendMessage(player, "neoessentials.economy.loans_max_amount", String.format("$%.2f", maxLoan));
            } else {
                LanguageUtil.sendMessage(player, "neoessentials.economy.loans_eligibility_insufficient");
                LanguageUtil.sendMessage(player, "neoessentials.economy.loans_minimum_balance", "$100.00");
            }
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_coming_soon");
            
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_error", e.getMessage());
        }
    }
}
