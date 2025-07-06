package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Economy Help GUI for showing all available economy commands and features
 * Simplified chat-based interface for reliability
 * 
 * @author ZeroG
 * @since 1.0.2.133
 */
public class EconomyHelpGUI {
    
    /**
     * Opens the economy help GUI (chat-based)
     */
    public static void openEconomyHelpGUI(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_header");
        
        // Basic commands
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_basic_commands");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_pay");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_econ");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_sendmoney");
        
        // Advanced features
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_advanced_commands");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_transactions");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_financialstats");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_bankaccount");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_loans");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_currencyexchange");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_command_economysettings");
        
        // Usage examples
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_examples_header");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_example_pay");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_example_check_balance");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_example_view_stats");
        
        // Tips and information
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_tips_header");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_tip_safety");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_tip_commands");
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_tip_support");
        
        LanguageUtil.sendMessage(player, "neoessentials.economy.help_footer");
    }
}
