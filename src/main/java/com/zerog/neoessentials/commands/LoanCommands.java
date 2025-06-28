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
                
                // Payoff Loan
                .then(Commands.literal("payoff")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.payoff"))
                    .then(Commands.argument("loan-id", StringArgumentType.string())
                        .executes(context -> payoffLoan(context.getSource(),
                            StringArgumentType.getString(context, "loan-id")))))
                
                // Credit Score
                .then(Commands.literal("credit")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.credit"))
                    .executes(context -> showCreditScore(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.credit.others"))
                        .executes(context -> showCreditScore(context.getSource(),
                            EntityArgument.getPlayer(context, "player")))))
                
                // Loan History
                .then(Commands.literal("history")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.history"))
                    .executes(context -> showLoanHistory(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.history.others"))
                        .executes(context -> showLoanHistory(context.getSource(),
                            EntityArgument.getPlayer(context, "player")))))
                
                // Admin Commands
                .then(Commands.literal("admin")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.admin"))
                    .then(Commands.literal("approve")
                        .then(Commands.argument("loan-id", StringArgumentType.string())
                            .executes(context -> approveLoan(context.getSource(),
                                StringArgumentType.getString(context, "loan-id")))))
                    .then(Commands.literal("deny")
                        .then(Commands.argument("loan-id", StringArgumentType.string())
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> denyLoan(context.getSource(),
                                    StringArgumentType.getString(context, "loan-id"),
                                    StringArgumentType.getString(context, "reason"))))))
                    .then(Commands.literal("forclose")
                        .then(Commands.argument("loan-id", StringArgumentType.string())
                            .executes(context -> forecloseLoan(context.getSource(),
                                StringArgumentType.getString(context, "loan-id")))))
                    .then(Commands.literal("modify")
                        .then(Commands.argument("loan-id", StringArgumentType.string())
                            .then(Commands.argument("field", StringArgumentType.string())
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                    .executes(context -> modifyLoan(context.getSource(),
                                        StringArgumentType.getString(context, "loan-id"),
                                        StringArgumentType.getString(context, "field"),
                                        StringArgumentType.getString(context, "value")))))))
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
            MessageUtil.sendMessage(player, "§e/loan payoff <loan-id> §7- Pay off loan completely");
            MessageUtil.sendMessage(player, "§e/loan credit §7- View your credit score");
            MessageUtil.sendMessage(player, "§e/loan history §7- View loan history");
            
            MessageUtil.sendMessage(player, "§e§lLoan Types:");
            MessageUtil.sendMessage(player, "§e• personal §7- Personal loans (1-60 months)");
            MessageUtil.sendMessage(player, "§e• business §7- Business loans (1-120 months)");
            MessageUtil.sendMessage(player, "§e• mortgage §7- Mortgages (1-360 months)");
            
            if (CommandManager.hasPermission(source, "neoessentials.command.loan.admin")) {
                MessageUtil.sendMessage(player, "§c§lAdmin Commands:");
                MessageUtil.sendMessage(player, "§c/loan admin approve <loan-id>");
                MessageUtil.sendMessage(player, "§c/loan admin deny <loan-id> <reason>");
                MessageUtil.sendMessage(player, "§c/loan admin forclose <loan-id>");
                MessageUtil.sendMessage(player, "§c/loan admin modify <loan-id> <field> <value>");
                MessageUtil.sendMessage(player, "§c/loan admin listall");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        }
    }
    
    private int applyForLoan(CommandSourceStack source, double amount, String typeStr, int termMonths) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
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
            
            // Apply for loan
            String loanId = bankManager.applyForLoan(player.getUUID(), amount, loanType, termMonths);
            
            if (loanId != null) {
                Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
                MessageUtil.sendMessage(player, "§aLoan application submitted successfully!");
                MessageUtil.sendMessage(player, "§7Loan ID: §e" + loanId.substring(0, 8));
                MessageUtil.sendMessage(player, "§7Amount: §a" + currency.format(amount));
                MessageUtil.sendMessage(player, "§7Type: §e" + loanType.getDisplayName());
                MessageUtil.sendMessage(player, "§7Term: §e" + termMonths + " months");
                MessageUtil.sendMessage(player, "§7Status: §6Pending Approval");
                MessageUtil.sendMessage(player, "§7You will be notified when your loan is reviewed.");
            } else {
                MessageUtil.sendErrorMessage(player, "Loan application failed. You may have too many active loans or insufficient credit score.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while processing your loan application: " + e.getMessage());
            return 0;
        }
    }
    
    private int showLoanInfo(CommandSourceStack source, String loanId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            Loan loan = bankManager.getLoan(UUID.fromString(loanId));
            if (loan == null) {
                MessageUtil.sendErrorMessage(player, "Loan not found with ID: " + loanId);
                return 0;
            }
            
            // Check if player owns this loan or has admin permission
            if (!loan.getBorrowerId().equals(player.getUUID()) && 
                !CommandManager.hasPermission(source, "neoessentials.command.loan.admin")) {
                MessageUtil.sendErrorMessage(player, "You don't have permission to view this loan.");
                return 0;
            }
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            
            MessageUtil.sendMessage(player, "§6=== Loan Information ===");
            MessageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            MessageUtil.sendMessage(player, "§7Type: §e" + loan.getLoanType().getDisplayName());
            MessageUtil.sendMessage(player, "§7Principal Amount: §a" + currency.format(loan.getPrincipalAmount()));
            MessageUtil.sendMessage(player, "§7Outstanding Balance: §c" + currency.format(loan.getOutstandingBalance()));
            MessageUtil.sendMessage(player, "§7Interest Rate: §e" + String.format("%.2f%%", loan.getInterestRate() * 100));
            MessageUtil.sendMessage(player, "§7Term: §e" + loan.getTermMonths() + " months");
            MessageUtil.sendMessage(player, "§7Monthly Payment: §e" + currency.format(loan.getMonthlyPayment()));
            MessageUtil.sendMessage(player, "§7Status: " + getStatusColor(loan.getStatus()) + loan.getStatus().name());
            MessageUtil.sendMessage(player, "§7Payments Made: §e" + loan.getPayments().size());
            
            if (loan.getStatus() == Loan.LoanStatus.CURRENT || loan.getStatus() == Loan.LoanStatus.APPROVED) {
                long remainingPayments = loan.getTermMonths() - loan.getPayments().size();
                MessageUtil.sendMessage(player, "§7Remaining Payments: §e" + remainingPayments);
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        } catch (IllegalArgumentException e) {
            MessageUtil.sendErrorMessage(source, "Invalid loan ID format.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while retrieving loan information: " + e.getMessage());
            return 0;
        }
    }
    
    private int listPlayerLoans(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return listPlayerLoans(source, player);
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        }
    }
    
    private int listPlayerLoans(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> loans = bankManager.getPlayerLoans(targetPlayer.getUUID());
            
            if (loans.isEmpty()) {
                String message = targetPlayer.equals(player) ? "You have no loans." : 
                    targetPlayer.getDisplayName().getString() + " has no loans.";
                MessageUtil.sendMessage(player, "§7" + message);
                return 1;
            }
            
            String title = targetPlayer.equals(player) ? "Your Loans" : 
                targetPlayer.getDisplayName().getString() + "'s Loans";
            MessageUtil.sendMessage(player, "§6=== " + title + " ===");
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            
            for (Loan loan : loans) {
                String loanId = loan.getLoanId().toString().substring(0, 8);
                String status = getStatusColor(loan.getStatus()) + loan.getStatus().name();
                String balance = currency.format(loan.getOutstandingBalance());
                String type = loan.getLoanType().getDisplayName();
                
                MessageUtil.sendMessage(player, "§e" + loanId + " §7- §e" + type + " §7- " + status + " §7- §c" + balance);
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while listing loans: " + e.getMessage());
            return 0;
        }
    }
    
    private int makeLoanPayment(CommandSourceStack source, double amount, String loanId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            UUID loanUUID = null;
            if (loanId != null) {
                try {
                    loanUUID = UUID.fromString(loanId);
                } catch (IllegalArgumentException e) {
                    MessageUtil.sendErrorMessage(player, "Invalid loan ID format.");
                    return 0;
                }
            }
            
            boolean success = bankManager.makeLoanPayment(player.getUUID(), loanUUID, amount);
            
            if (success) {
                Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
                MessageUtil.sendMessage(player, "§aLoan payment of " + currency.format(amount) + " processed successfully!");
                
                if (loanUUID != null) {
                    Loan loan = bankManager.getLoan(loanUUID);
                    if (loan != null && loan.getOutstandingBalance() <= 0) {
                        MessageUtil.sendMessage(player, "§a§lCongratulations! Your loan has been paid off completely!");
                    }
                }
            } else {
                MessageUtil.sendErrorMessage(player, "Payment failed. Check your balance and loan status.");
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while processing payment: " + e.getMessage());
            return 0;
        }
    }
    
    private int payoffLoan(CommandSourceStack source, String loanId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            UUID loanUUID;
            try {
                loanUUID = UUID.fromString(loanId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid loan ID format.");
                return 0;
            }
            
            Loan loan = bankManager.getLoan(loanUUID);
            if (loan == null) {
                MessageUtil.sendErrorMessage(player, "Loan not found with ID: " + loanId);
                return 0;
            }
            
            if (!loan.getBorrowerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "You don't own this loan.");
                return 0;
            }
            
            double payoffAmount = loan.getOutstandingBalance();
            boolean success = bankManager.makeLoanPayment(player.getUUID(), loanUUID, payoffAmount);
            
            if (success) {
                Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
                MessageUtil.sendMessage(player, "§a§lLoan paid off successfully!");
                MessageUtil.sendMessage(player, "§7Payoff amount: §a" + currency.format(payoffAmount));
                MessageUtil.sendMessage(player, "§7Your credit score has been improved!");
            } else {
                MessageUtil.sendErrorMessage(player, "Payoff failed. Check your balance.");
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while paying off loan: " + e.getMessage());
            return 0;
        }
    }
    
    private int showCreditScore(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return showCreditScore(source, player);
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        }
    }
    
    private int showCreditScore(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            double creditScore = bankManager.getCreditScore(targetPlayer.getUUID());
            String creditRating = getCreditRating(creditScore);
            
            String title = targetPlayer.equals(player) ? "Your Credit Information" : 
                targetPlayer.getDisplayName().getString() + "'s Credit Information";
            
            MessageUtil.sendMessage(player, "§6=== " + title + " ===");
            MessageUtil.sendMessage(player, "§7Credit Score: §e" + String.format("%.0f", creditScore));
            MessageUtil.sendMessage(player, "§7Credit Rating: " + getCreditRatingColor(creditScore) + creditRating);
            
            // Show loan eligibility
            MessageUtil.sendMessage(player, "§e§lLoan Eligibility:");
            for (Loan.LoanType loanType : Loan.LoanType.values()) {
                boolean eligible = bankManager.isEligibleForLoan(targetPlayer.getUUID(), loanType);
                String status = eligible ? "§aEligible" : "§cNot Eligible";
                MessageUtil.sendMessage(player, "§7• " + loanType.getDisplayName() + ": " + status);
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while retrieving credit information: " + e.getMessage());
            return 0;
        }
    }
    
    private int showLoanHistory(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return showLoanHistory(source, player);
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        }
    }
    
    private int showLoanHistory(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            // This would show historical loans including paid off and foreclosed loans
            MessageUtil.sendMessage(player, "§eLoan history system coming soon!");
            MessageUtil.sendMessage(player, "§7This will show all historical loans for " + 
                (targetPlayer.equals(player) ? "you" : targetPlayer.getDisplayName().getString()));
            return 1;
        } catch (CommandSyntaxException e) {
            MessageUtil.sendErrorMessage(source, "This command can only be used by players.");
            return 0;
        }
    }
    
    // Admin command implementations
    private int approveLoan(CommandSourceStack source, String loanId) {
        try {
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            UUID loanUUID = UUID.fromString(loanId);
            boolean success = bankManager.approveLoan(loanUUID);
            
            if (success) {
                MessageUtil.sendMessage(source, "§aLoan " + loanId.substring(0, 8) + " approved successfully!");
            } else {
                MessageUtil.sendErrorMessage(source, "Failed to approve loan. Check loan ID and status.");
            }
            
            return success ? 1 : 0;
        } catch (IllegalArgumentException e) {
            MessageUtil.sendErrorMessage(source, "Invalid loan ID format.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while approving loan: " + e.getMessage());
            return 0;
        }
    }
    
    private int denyLoan(CommandSourceStack source, String loanId, String reason) {
        try {
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            UUID loanUUID = UUID.fromString(loanId);
            boolean success = bankManager.denyLoan(loanUUID, reason);
            
            if (success) {
                MessageUtil.sendMessage(source, "§cLoan " + loanId.substring(0, 8) + " denied successfully!");
                MessageUtil.sendMessage(source, "§7Reason: " + reason);
            } else {
                MessageUtil.sendErrorMessage(source, "Failed to deny loan. Check loan ID and status.");
            }
            
            return success ? 1 : 0;
        } catch (IllegalArgumentException e) {
            MessageUtil.sendErrorMessage(source, "Invalid loan ID format.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while denying loan: " + e.getMessage());
            return 0;
        }
    }
    
    private int forecloseLoan(CommandSourceStack source, String loanId) {
        try {
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            UUID loanUUID = UUID.fromString(loanId);
            boolean success = bankManager.forecloseLoan(loanUUID);
            
            if (success) {
                MessageUtil.sendMessage(source, "§cLoan " + loanId.substring(0, 8) + " foreclosed successfully!");
            } else {
                MessageUtil.sendErrorMessage(source, "Failed to foreclose loan. Check loan ID and status.");
            }
            
            return success ? 1 : 0;
        } catch (IllegalArgumentException e) {
            MessageUtil.sendErrorMessage(source, "Invalid loan ID format.");
            return 0;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while foreclosing loan: " + e.getMessage());
            return 0;
        }
    }
    
    private int modifyLoan(CommandSourceStack source, String loanId, String field, String value) {
        try {
            MessageUtil.sendMessage(source, "§eLoan modification system coming soon!");
            MessageUtil.sendMessage(source, "§7Will modify " + field + " to " + value + " for loan " + loanId.substring(0, 8));
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while modifying loan: " + e.getMessage());
            return 0;
        }
    }
    
    private int listAllLoans(CommandSourceStack source) {
        try {
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> allLoans = bankManager.getAllLoans();
            
            if (allLoans.isEmpty()) {
                MessageUtil.sendMessage(source, "§7No loans in the system.");
                return 1;
            }
            
            MessageUtil.sendMessage(source, "§6=== All Loans (" + allLoans.size() + ") ===");
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            
            for (Loan loan : allLoans.stream().limit(20).toList()) {
                String loanId = loan.getLoanId().toString().substring(0, 8);
                String status = getStatusColor(loan.getStatus()) + loan.getStatus().name();
                String balance = currency.format(loan.getOutstandingBalance());
                String type = loan.getLoanType().getDisplayName();
                
                MessageUtil.sendMessage(source, "§e" + loanId + " §7- §e" + type + " §7- " + status + " §7- §c" + balance);
            }
            
            if (allLoans.size() > 20) {
                MessageUtil.sendMessage(source, "§7... and " + (allLoans.size() - 20) + " more loans.");
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "An error occurred while listing loans: " + e.getMessage());
            return 0;
        }
    }
    
    // Helper methods
    private String getStatusColor(Loan.LoanStatus status) {
        return switch (status) {
            case PENDING -> "§6";
            case APPROVED, CURRENT -> "§a";
            case OVERDUE -> "§c";
            case DEFAULT, FORECLOSED -> "§4";
            case PAID_OFF -> "§2";
            case DENIED -> "§c";
        };
    }
    
    private String getCreditRating(double score) {
        if (score >= 800) return "Excellent";
        if (score >= 740) return "Very Good";
        if (score >= 670) return "Good";
        if (score >= 580) return "Fair";
        return "Poor";
    }
    
    private String getCreditRatingColor(double score) {
        if (score >= 800) return "§a";
        if (score >= 740) return "§2";
        if (score >= 670) return "§e";
        if (score >= 580) return "§6";
        return "§c";
    }
}
