package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
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
        LanguageUtil.sendMessage(player, "neoessentials.economy.bank_feature_placeholder");
        LanguageUtil.sendMessage(player, "neoessentials.economy.bank_coming_soon");
    }
}
