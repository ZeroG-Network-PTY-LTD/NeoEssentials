package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.data.EconomyManager;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

/**
 * Financial Stats GUI showing player's financial statistics
 * Simplified chat-based interface for reliability
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class FinancialStatsGUI {
    
    /**
     * Opens a simplified financial statistics GUI (chat-based)
     */
    public static void openFinancialStatsGUI(ServerPlayer player) {
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        try {
            double balance = economyManager.getBalance(player.getUUID());
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.financial_stats_header");
            LanguageUtil.sendMessage(player, "neoessentials.economy.current_balance", String.format("$%.2f", balance));
            
            // Get basic statistics
            LanguageUtil.sendMessage(player, "neoessentials.economy.financial_stats_basic");
            
            // Show transaction count if available
            var transactions = economyManager.getRecentTransactions(player.getUUID(), 100);
            if (transactions != null && !transactions.isEmpty()) {
                LanguageUtil.sendMessage(player, "neoessentials.economy.total_transactions", String.valueOf(transactions.size()));
                
                // Calculate some basic stats
                double totalSpent = 0;
                double totalReceived = 0;
                
                for (var transaction : transactions) {
                    // For transfers, check if this player sent or received money
                    if (transaction.getType().equals("transfer_send") && transaction.getPlayerUUID().equals(player.getUUID())) {
                        totalSpent += transaction.getAmount();
                    } else if (transaction.getType().equals("transfer_receive") && transaction.getPlayerUUID().equals(player.getUUID())) {
                        totalReceived += transaction.getAmount();
                    }
                }
                
                LanguageUtil.sendMessage(player, "neoessentials.economy.total_spent", String.format("$%.2f", totalSpent));
                LanguageUtil.sendMessage(player, "neoessentials.economy.total_received", String.format("$%.2f", totalReceived));
            } else {
                LanguageUtil.sendMessage(player, "neoessentials.economy.no_transaction_history");
            }
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.financial_stats_footer");
            
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.stats_error", e.getMessage());
        }
    }
}
