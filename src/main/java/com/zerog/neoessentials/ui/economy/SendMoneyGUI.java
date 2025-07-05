package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Send Money GUI for transferring money to other players
 * Chat-based interface for simplicity
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class SendMoneyGUI {
    
    public static void openSendMoneyGUI(ServerPlayer player) {
        // For now, use chat-based interface
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        double balance = economyManager.getBalance(player.getUUID());
        
        LanguageUtil.sendMessage(player, "neoessentials.economy.send_money_header");
        LanguageUtil.sendMessage(player, "neoessentials.economy.cash_on_hand", String.format("$%.2f", balance));
        LanguageUtil.sendMessage(player, "neoessentials.economy.send_money_instructions");
        LanguageUtil.sendMessage(player, "neoessentials.economy.send_money_usage");
    }
    
    public static void handleSendMoneyCommand(ServerPlayer player, String targetName, double amount) {
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Validate amount
        if (amount <= 0) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.economy.invalid_amount", amount);
            return;
        }
        
        // Check if player has enough money
        if (economyManager.getBalance(player.getUUID()) < amount) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.economy.insufficient_funds", 
                String.format("$%.2f", amount), 
                String.format("$%.2f", economyManager.getBalance(player.getUUID())));
            return;
        }
        
        // Find target player
        var server = player.getServer();
        if (server == null) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.commands.error.server_not_available");
            return;
        }
        
        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.commands.error.player_not_found", targetName);
            return;
        }
        
        // Prevent sending money to self
        if (target.getUUID().equals(player.getUUID())) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.economy.cannot_send_to_self");
            return;
        }
        
        // Perform the transaction
        try {
            boolean success = economyManager.transfer(player.getUUID(), target.getUUID(), amount);
            if (success) {
                LanguageUtil.sendMessage(player, "neoessentials.economy.money_sent", 
                    String.format("$%.2f", amount), target.getScoreboardName());
                LanguageUtil.sendMessage(target, "neoessentials.economy.money_received", 
                    String.format("$%.2f", amount), player.getScoreboardName());
            } else {
                LanguageUtil.sendErrorMessage(player, "neoessentials.economy.transfer_failed");
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.economy.transfer_error", e.getMessage());
        }
    }
}
