package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Transaction History GUI (placeholder)
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class TransactionHistoryGUI {
    
    public static void openTransactionHistory(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "neoessentials.economy.transaction_history_coming_soon");
    }
}
