package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.ui.economy.EconomyGUI;
import com.zerog.neoessentials.ui.economy.SendMoneyGUI;
import com.zerog.neoessentials.ui.economy.FinancialStatsGUI;
import com.zerog.neoessentials.ui.economy.TransactionHistoryGUI;
import com.zerog.neoessentials.ui.economy.BankAccountGUI;
import com.zerog.neoessentials.ui.economy.CurrencyExchangeGUI;
import com.zerog.neoessentials.ui.economy.EconomySettingsGUI;
import com.zerog.neoessentials.ui.economy.LoanManagementGUI;
import com.zerog.neoessentials.ui.economy.EconomyHelpGUI;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Enhanced Pay Commands with GUI integration
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class EnhancedPayCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", StringArgumentType.string())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(EnhancedPayCommands::payPlayer)
                )
            )
            .executes(EnhancedPayCommands::openPayGUI)
        );
        
        dispatcher.register(Commands.literal("sendmoney")
            .executes(EnhancedPayCommands::openPayGUI)
        );
        
        dispatcher.register(Commands.literal("economygui")
            .executes(EnhancedPayCommands::openEconomyGUI)
        );
        
        dispatcher.register(Commands.literal("econ")
            .executes(EnhancedPayCommands::openEconomyGUI)
        );
        
        dispatcher.register(Commands.literal("financialstats")
            .executes(EnhancedPayCommands::openFinancialStats)
        );
        
        dispatcher.register(Commands.literal("transactions")
            .executes(EnhancedPayCommands::openTransactionHistory)
        );
        
        dispatcher.register(Commands.literal("bankaccount")
            .executes(EnhancedPayCommands::openBankAccount)
        );
        
        dispatcher.register(Commands.literal("currencyexchange")
            .executes(EnhancedPayCommands::openCurrencyExchange)
        );
        
        dispatcher.register(Commands.literal("economysettings")
            .executes(EnhancedPayCommands::openEconomySettings)
        );
        
        dispatcher.register(Commands.literal("loans")
            .executes(EnhancedPayCommands::openLoanManagement)
        );
        
        dispatcher.register(Commands.literal("economyhelp")
            .executes(EnhancedPayCommands::openEconomyHelp)
        );
    }
    
    private static int payPlayer(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        String targetName = StringArgumentType.getString(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        SendMoneyGUI.handleSendMoneyCommand(player, targetName, amount);
        return 1;
    }
    
    private static int openPayGUI(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        SendMoneyGUI.openSendMoneyGUI(player);
        return 1;
    }
    
    private static int openEconomyGUI(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        try {
            EconomyGUI.openEconomyGUI(player);
        } catch (Exception e) {
            // If GUI fails, fall back to chat-based interface
            LanguageUtil.sendMessage(player, "neoessentials.economy.gui_unavailable");
            LanguageUtil.sendMessage(player, "neoessentials.economy.using_chat_interface");
            // Could add chat-based economy interface here
        }
        return 1;
    }
    
    private static int openFinancialStats(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        try {
            FinancialStatsGUI.openFinancialStatsGUI(player);
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.stats_error", e.getMessage());
        }
        return 1;
    }
    
    private static int openTransactionHistory(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        try {
            TransactionHistoryGUI.openTransactionHistory(player);
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.transaction_history_error", e.getMessage());
        }
        return 1;
    }
    
    private static int openBankAccount(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        try {
            BankAccountGUI.openBankAccountGUI(player);
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_error", e.getMessage());
        }
        return 1;
    }
    
    private static int openCurrencyExchange(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        try {
            CurrencyExchangeGUI.openCurrencyExchangeGUI(player);
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.exchange_error", e.getMessage());
        }
        return 1;
    }
    
    private static int openEconomySettings(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        try {
            EconomySettingsGUI.openEconomySettingsGUI(player);
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_error", e.getMessage());
        }
        return 1;
    }
    
    private static int openLoanManagement(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        try {
            LoanManagementGUI.openLoanManagementGUI(player);
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.loans_error", e.getMessage());
        }
        return 1;
    }
    
    private static int openEconomyHelp(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            LanguageUtil.sendMessage(context.getSource(), "neoessentials.commands.error.not_player");
            return 0;
        }
        
        EconomyHelpGUI.openEconomyHelpGUI(player);
        return 1;
    }
}
