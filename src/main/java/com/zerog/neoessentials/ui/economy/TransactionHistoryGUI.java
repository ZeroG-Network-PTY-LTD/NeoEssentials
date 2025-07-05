package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import com.zerog.neoessentials.data.EconomyTransaction;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Transaction History GUI - Shows player's transaction history
 * Chat-based interface for simplicity and reliability
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class TransactionHistoryGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    
    public static void openTransactionHistory(ServerPlayer player) {
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Get recent transactions for this player (last 10)
        List<EconomyTransaction> transactions = economyManager.getRecentTransactions(player.getUUID(), 10);
        
        if (transactions.isEmpty()) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.transaction_history_empty");
            return;
        }
        
        // Display header
        LanguageUtil.sendMessage(player, "neoessentials.economy.transaction_history_header");
        
        // Display each transaction
        for (int i = 0; i < transactions.size(); i++) {
            EconomyTransaction transaction = transactions.get(i);
            String date = DATE_FORMAT.format(new Date(transaction.getTimestamp()));
            String type = getTransactionTypeDisplay(transaction.getType());
            String amount = formatAmount(transaction.getAmount());
            String balance = String.format("$%.2f", transaction.getBalanceAfter());
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.transaction_entry", 
                String.valueOf(i + 1), date, type, amount, balance);
        }
        
        // Display footer
        LanguageUtil.sendMessage(player, "neoessentials.economy.transaction_history_footer");
        LanguageUtil.sendMessage(player, "neoessentials.economy.transaction_history_info", 
            String.valueOf(transactions.size()));
    }
    
    /**
     * Format transaction type for display
     */
    private static String getTransactionTypeDisplay(String type) {
        switch (type.toLowerCase()) {
            case "deposit":
                return "&a+Deposit";
            case "withdraw":
                return "&c-Withdraw";
            case "transfer_send":
                return "&e→Send";
            case "transfer_receive":
                return "&a←Receive";
            case "payment":
                return "&b$Payment";
            case "purchase":
                return "&c-Purchase";
            case "sale":
                return "&a+Sale";
            default:
                return "&7" + type;
        }
    }
    
    /**
     * Format amount with appropriate color
     */
    private static String formatAmount(double amount) {
        if (amount >= 0) {
            return "&a+$" + String.format("%.2f", amount);
        } else {
            return "&c-$" + String.format("%.2f", Math.abs(amount));
        }
    }
}
