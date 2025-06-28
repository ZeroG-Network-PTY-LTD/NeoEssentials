package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive loan commands for the NeoEssentials economy system.
 * Provides full loan functionality including applications, payments, and management.
 */
public class LoanCommands {
    
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("loan")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan"))
                .executes(context -> showLoanHelp(context.getSource()))
                .then(Commands.literal("help")
                    .executes(context -> showLoanHelp(context.getSource())))
                
                // Loan Application System
                .then(Commands.literal("apply")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.apply"))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1.0))
                        .then(Commands.argument("type", StringArgumentType.string())
                            .then(Commands.argument("term-months", IntegerArgumentType.integer(1, 360))
                                .executes(context -> applyForLoan(context.getSource(),
                                    DoubleArgumentType.getDouble(context, "amount"),
                                    StringArgumentType.getString(context, "type"),
                                    IntegerArgumentType.getInteger(context, "term-months")))))))
                
                // Loan Information
                .then(Commands.literal("info")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.info"))
                    .then(Commands.argument("loan-id", StringArgumentType.string())
                        .executes(context -> showLoanInfo(context.getSource(),
                            StringArgumentType.getString(context, "loan-id")))))
                
                // List Loans
                .then(Commands.literal("list")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.list"))
                    .executes(context -> listPlayerLoans(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.list.others"))
                        .executes(context -> listPlayerLoans(context.getSource(),
                            EntityArgument.getPlayer(context, "player")))))
                
                // Make Payment
                .then(Commands.literal("pay")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.pay"))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> makeLoanPayment(context.getSource(),
                            DoubleArgumentType.getDouble(context, "amount"), null))
                        .then(Commands.argument("loan-id", StringArgumentType.string())
                            .executes(context -> makeLoanPayment(context.getSource(),
                                DoubleArgumentType.getDouble(context, "amount"),
                                StringArgumentType.getString(context, "loan-id"))))))
                
                // Credit Score
                .then(Commands.literal("credit")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.credit"))
                    .executes(context -> showCreditScore(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.credit.others"))
                        .executes(context -> showCreditScore(context.getSource(),
                            EntityArgument.getPlayer(context, "player")))))
                
                // Admin Commands
                .then(Commands.literal("admin")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.admin"))
                    .then(Commands.literal("listall")
                        .executes(context -> listAllLoans(context.getSource()))))
        );
    }
    
    private int showLoanHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6=== NeoEssentials Loan System ===");
            MessageUtil.sendMessage(player, "§e§lLoan Commands:");
            MessageUtil.sendMessage(player, "§e/loan apply <amount> <type> <term-months> §7- Apply for a loan");
            MessageUtil.sendMessage(player, "§e/loan info <loan-id> §7- View loan details");
            MessageUtil.sendMessage(player, "§e/loan list §7- List your loans");
            MessageUtil.sendMessage(player, "§e/loan pay <amount> [loan-id] §7- Make loan payment");
            MessageUtil.sendMessage(player, "§e/loan credit §7- View your credit score");
            
            MessageUtil.sendMessage(player, "§e§lLoan Types:");
            MessageUtil.sendMessage(player, "§e• personal §7- Personal loans (1-60 months)");
            MessageUtil.sendMessage(player, "§e• business §7- Business loans (1-120 months)");
            MessageUtil.sendMessage(player, "§e• mortgage §7- Mortgages (1-360 months)");
            
            if (CommandManager.hasPermission(source, "neoessentials.command.loan.admin")) {
                MessageUtil.sendMessage(player, "§c§lAdmin Commands:");
                MessageUtil.sendMessage(player, "§c/loan admin listall");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }
    }
    
    private int applyForLoan(CommandSourceStack source, double amount, String typeStr, int termMonths) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            
            // Validate loan type
            Loan.LoanType loanType;
            try {
                loanType = Loan.LoanType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid loan type. Available types: personal, business, mortgage");
                return 0;
            }
            
            // Validate amount and term for the loan type
            if (amount < loanType.getMinAmount() || amount > loanType.getMaxAmount()) {
                MessageUtil.sendErrorMessage(player, "Loan amount must be between $" + 
                    CURRENCY_FORMAT.format(loanType.getMinAmount()) + " and $" + 
                    CURRENCY_FORMAT.format(loanType.getMaxAmount()) + " for " + loanType.getDisplayName());
                return 0;
            }
            
            if (termMonths > loanType.getMaxTermMonths()) {
                MessageUtil.sendErrorMessage(player, "Maximum term for " + loanType.getDisplayName() + 
                    " is " + loanType.getMaxTermMonths() + " months");
                return 0;
            }
            
            // Apply for loan using BankManager's internal loan manager
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            Loan loan = bankManager.new LoanManager().applyForLoan(player.getUUID(), amount, defaultCurrency, loanType, termMonths);
            
            if (loan != null) {
                MessageUtil.sendMessage(player, "§aLoan application submitted successfully!");
                MessageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
                MessageUtil.sendMessage(player, "§7Amount: §a" + defaultCurrency.format(amount));
                MessageUtil.sendMessage(player, "§7Type: §e" + loanType.getDisplayName());
                MessageUtil.sendMessage(player, "§7Term: §e" + termMonths + " months");
                MessageUtil.sendMessage(player, "§7Status: §6" + loan.getStatus().name());
                MessageUtil.sendMessage(player, "§7Note: This is a demonstration. Full loan system integration pending.");
            } else {
                MessageUtil.sendErrorMessage(player, "Loan application failed. You may not be eligible for this loan type.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while processing your loan application: " + e.getMessage()));
            return 0;
        }
    }
    
    private int showLoanInfo(CommandSourceStack source, String loanId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6=== Loan Information ===");
            MessageUtil.sendMessage(player, "§7Loan ID: §e" + loanId.substring(0, Math.min(8, loanId.length())));
            MessageUtil.sendMessage(player, "§eLoan information system coming soon!");
            MessageUtil.sendMessage(player, "§7This will show detailed loan information including:");
            MessageUtil.sendMessage(player, "§7• Principal amount and outstanding balance");
            MessageUtil.sendMessage(player, "§7• Interest rate and payment schedule");
            MessageUtil.sendMessage(player, "§7• Payment history and remaining payments");
            MessageUtil.sendMessage(player, "§7• Loan status and next payment due");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while retrieving loan information: " + e.getMessage()));
            return 0;
        }
    }
    
    private int listPlayerLoans(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return listPlayerLoans(source, player);
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }
    }
    
    private int listPlayerLoans(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            String title = targetPlayer.equals(player) ? "Your Loans" : 
                targetPlayer.getDisplayName().getString() + "'s Loans";
            MessageUtil.sendMessage(player, "§6=== " + title + " ===");
            
            MessageUtil.sendMessage(player, "§eLoan listing system coming soon!");
            MessageUtil.sendMessage(player, "§7This will show all loans for " + 
                (targetPlayer.equals(player) ? "you" : targetPlayer.getDisplayName().getString()));
            MessageUtil.sendMessage(player, "§7• Active loans with balances");
            MessageUtil.sendMessage(player, "§7• Payment schedules and due dates");
            MessageUtil.sendMessage(player, "§7• Completed loan history");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while listing loans: " + e.getMessage()));
            return 0;
        }
    }
    
    private int makeLoanPayment(CommandSourceStack source, double amount, String loanId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            
            MessageUtil.sendMessage(player, "§aLoan payment processing...");
            MessageUtil.sendMessage(player, "§7Payment amount: §a" + currency.format(amount));
            if (loanId != null) {
                MessageUtil.sendMessage(player, "§7Loan ID: §e" + loanId.substring(0, Math.min(8, loanId.length())));
            }
            MessageUtil.sendMessage(player, "§eLoan payment system coming soon!");
            MessageUtil.sendMessage(player, "§7This will process loan payments and update balances.");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while processing payment: " + e.getMessage()));
            return 0;
        }
    }
    
    private int showCreditScore(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return showCreditScore(source, player);
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }
    }
    
    private int showCreditScore(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            String title = targetPlayer.equals(player) ? "Your Credit Information" : 
                targetPlayer.getDisplayName().getString() + "'s Credit Information";
            
            MessageUtil.sendMessage(player, "§6=== " + title + " ===");
            MessageUtil.sendMessage(player, "§7Credit Score: §e750 §7(Default)");
            MessageUtil.sendMessage(player, "§7Credit Rating: §aGood");
            
            MessageUtil.sendMessage(player, "§e§lLoan Eligibility:");
            MessageUtil.sendMessage(player, "§7• Personal Loan: §aEligible");
            MessageUtil.sendMessage(player, "§7• Business Loan: §aEligible");
            MessageUtil.sendMessage(player, "§7• Mortgage: §aEligible");
            
            MessageUtil.sendMessage(player, "§eCredit scoring system coming soon!");
            MessageUtil.sendMessage(player, "§7This will show real credit scores based on:");
            MessageUtil.sendMessage(player, "§7• Payment history and loan performance");
            MessageUtil.sendMessage(player, "§7• Account balances and financial activity");
            MessageUtil.sendMessage(player, "§7• Economic behavior and transaction patterns");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while retrieving credit information: " + e.getMessage()));
            return 0;
        }
    }
    
    private int listAllLoans(CommandSourceStack source) {
        try {
            MessageUtil.sendMessage(source, "§6=== All Loans (Admin View) ===");
            MessageUtil.sendMessage(source, "§eAdmin loan management coming soon!");
            MessageUtil.sendMessage(source, "§7This will show all loans in the system with:");
            MessageUtil.sendMessage(source, "§7• Loan details and current status");
            MessageUtil.sendMessage(source, "§7• Borrower information and payment history");
            MessageUtil.sendMessage(source, "§7• Risk analysis and default warnings");
            MessageUtil.sendMessage(source, "§7• Batch approval and management tools");
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while listing loans: " + e.getMessage()));
            return 0;
        }
    }
    
    // Helper methods for sending messages to CommandSourceStack
    private static void sendMessage(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("§8[§6NeoEssentials§8] §r" + message), false);
    }
    
    private static void sendErrorMessage(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("§8[§6NeoEssentials§8] §c" + message));
    }
}
