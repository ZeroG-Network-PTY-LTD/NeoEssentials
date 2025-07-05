package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loan Management GUI (placeholder)
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class LoanManagementGUI {
    
    public static void openLoanManagementGUI(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "neoessentials.economy.loans_coming_soon");
    }
}
