package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Main Economy GUI for managing player finances
 * Chat-based interface for reliability and simplicity
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class EconomyGUI {
    
    /**
     * Opens the economy GUI for a player (chat-based)
     */
    public static void openEconomyGUI(ServerPlayer player) {
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Display financial status
        LanguageUtil.sendMessage(player, "neoessentials.economy.financial_status_header");
        
        // Cash balance - using the data.EconomyManager method
        double cashBalance = economyManager.getBalance(player.getUUID());
        LanguageUtil.sendMessage(player, "neoessentials.economy.cash_on_hand", String.format("$%.2f", cashBalance));
        
        // For now, show simple financial status
        LanguageUtil.sendMessage(player, "neoessentials.economy.total_wealth", String.format("$%.2f", cashBalance));
        
        // Available actions
        LanguageUtil.sendMessage(player, "neoessentials.economy.available_actions_header");
        LanguageUtil.sendMessage(player, "neoessentials.economy.action_send_money");
        LanguageUtil.sendMessage(player, "neoessentials.economy.action_transaction_history");
        LanguageUtil.sendMessage(player, "neoessentials.economy.action_financial_stats");
        LanguageUtil.sendMessage(player, "neoessentials.economy.action_bank_account");
        LanguageUtil.sendMessage(player, "neoessentials.economy.action_loans");
        LanguageUtil.sendMessage(player, "neoessentials.economy.action_settings");
        LanguageUtil.sendMessage(player, "neoessentials.economy.quick_commands_info");
    }
    
    /**
     * Handle economy GUI click events (placeholder for future chest GUI)
     */
    public static void handleEconomyGUIClick(ServerPlayer player, int slot) {
        // Future implementation for chest-based GUI
        switch (slot) {
            case 10: // Cash Balance
                TransactionHistoryGUI.openTransactionHistory(player);
                break;
            case 14: // Send Money
                SendMoneyGUI.openSendMoneyGUI(player);
                break;
            case 16: // Loans
                LoanManagementGUI.openLoanManagementGUI(player);
                break;
            case 19: // Exchange
                CurrencyExchangeGUI.openCurrencyExchangeGUI(player);
                break;
            case 21: // Statistics
                FinancialStatsGUI.openFinancialStatsGUI(player);
                break;
            case 23: // Settings
                EconomySettingsGUI.openEconomySettingsGUI(player);
                break;
            case 26: // Close
                // player.closeContainer();
                break;
            default:
                // Do nothing for other slots
                break;
        }
    }
}