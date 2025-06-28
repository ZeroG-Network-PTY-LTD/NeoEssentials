package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
                
                // List Applications (pending/approved loans)
                .then(Commands.literal("applications")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.list"))
                    .executes(context -> listLoanApplications(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.list.others"))
                        .executes(context -> listLoanApplications(context.getSource(),
                            EntityArgument.getPlayer(context, "player")))))
                
                // Quick recent applications view
                .then(Commands.literal("recent")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.loan.list"))
                    .executes(context -> listRecentApplications(context.getSource())))
                
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
            MessageUtil.sendMessage(player, "§a/loan applications §7- View ALL your loan applications");
            MessageUtil.sendMessage(player, "§a/loan recent §7- View your 5 most recent applications");
            MessageUtil.sendMessage(player, "§e/loan list §7- List active loans only");
            MessageUtil.sendMessage(player, "§e/loan info <loan-id> §7- View loan details");
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
                MessageUtil.sendMessage(player, "§a✓ Loan application submitted successfully!");
                MessageUtil.sendMessage(player, "§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§7📋 Application Details:");
                MessageUtil.sendMessage(player, "§7   Loan ID: §e" + loan.getLoanId().toString().substring(0, 8) + "...");
                MessageUtil.sendMessage(player, "§7   Amount: §a" + defaultCurrency.format(amount));
                MessageUtil.sendMessage(player, "§7   Type: §e" + loanType.getDisplayName());
                MessageUtil.sendMessage(player, "§7   Term: §e" + termMonths + " months");
                MessageUtil.sendMessage(player, "§7   Monthly Payment: §e" + defaultCurrency.format(loan.getMonthlyPayment()));
                MessageUtil.sendMessage(player, "§7   Interest Rate: §e" + String.format("%.2f%%", loan.getInterestRate() * 100));
                MessageUtil.sendMessage(player, "§7   Status: §6" + loan.getStatus().name());
                MessageUtil.sendMessage(player, "§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§a💾 Application saved to database - persists across server restarts!");
                MessageUtil.sendMessage(player, "§e📱 Use §a/loan applications §eto view all your applications");
                MessageUtil.sendMessage(player, "§e📱 Use §a/loan info " + loan.getLoanId().toString().substring(0, 8) + " §efor full details");
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
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            UUID loanUUID;
            try {
                loanUUID = UUID.fromString(loanId);
            } catch (IllegalArgumentException e) {
                // Try to find loan by partial ID
                List<Loan> playerLoans = bankManager.getPlayerLoans(player.getUUID());
                Loan loan = playerLoans.stream()
                    .filter(l -> l.getLoanId().toString().startsWith(loanId))
                    .findFirst()
                    .orElse(null);
                
                if (loan == null) {
                    MessageUtil.sendErrorMessage(player, "Loan not found with ID: " + loanId);
                    return 0;
                }
                loanUUID = loan.getLoanId();
            }
            
            Loan loan = bankManager.getLoan(loanUUID);
            if (loan == null || !loan.getBorrowerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "Loan not found or you don't have permission to view it.");
                return 0;
            }
            
            Currency currency = loan.getCurrency();
            
            MessageUtil.sendMessage(player, "§6=== Loan Information ===");
            MessageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            MessageUtil.sendMessage(player, "§7Type: §e" + loan.getType().getDisplayName());
            MessageUtil.sendMessage(player, "§7Status: §a" + loan.getStatus().name());
            MessageUtil.sendMessage(player, "§7Principal Amount: §a" + currency.format(loan.getPrincipalAmount()));
            MessageUtil.sendMessage(player, "§7Current Balance: §c" + currency.format(loan.getCurrentBalance()));
            MessageUtil.sendMessage(player, "§7Monthly Payment: §e" + currency.format(loan.getMonthlyPayment()));
            MessageUtil.sendMessage(player, "§7Interest Rate: §e" + String.format("%.2f%%", loan.getInterestRate() * 100));
            MessageUtil.sendMessage(player, "§7Payments Remaining: §e" + loan.getPaymentsRemaining());
            MessageUtil.sendMessage(player, "§7Next Payment Due: §e" + new java.text.SimpleDateFormat("MMM dd, yyyy").format(loan.getNextPaymentDue()));
            
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
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> playerLoans = bankManager.getPlayerLoans(targetPlayer.getUUID());
            
            String title = targetPlayer.equals(player) ? "Your Loans" : 
                targetPlayer.getDisplayName().getString() + "'s Loans";
            MessageUtil.sendMessage(player, "§6=== " + title + " ===");
            
            if (playerLoans.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No loans found.");
                return 1;
            }
            
            for (Loan loan : playerLoans) {
                Currency currency = loan.getCurrency();
                String statusColor = switch (loan.getStatus()) {
                    case CURRENT -> "§a";
                    case LATE -> "§c";
                    case PAID_OFF -> "§2";
                    case DEFAULT -> "§4";
                    default -> "§e";
                };
                
                MessageUtil.sendMessage(player, "§7• ID: §e" + loan.getLoanId().toString().substring(0, 8) + 
                    " §7| Type: §e" + loan.getType().getDisplayName() + 
                    " §7| Balance: §c" + currency.format(loan.getCurrentBalance()) + 
                    " §7| Status: " + statusColor + loan.getStatus().name());
            }
            
            MessageUtil.sendMessage(player, "§7Use §e/loan info <loan-id>§7 for detailed information.");
            
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
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            BankManager.LoanManager loanManager = bankManager.getLoanManager();
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            
            UUID loanUUID = null;
            if (loanId != null) {
                try {
                    loanUUID = UUID.fromString(loanId);
                } catch (IllegalArgumentException e) {
                    // Try to find loan by partial ID
                    List<Loan> playerLoans = bankManager.getPlayerLoans(player.getUUID());
                    Loan loan = playerLoans.stream()
                        .filter(l -> l.getLoanId().toString().startsWith(loanId))
                        .findFirst()
                        .orElse(null);
                    
                    if (loan != null) {
                        loanUUID = loan.getLoanId();
                    }
                }
            }
            
            boolean success = loanManager.makePayment(player.getUUID(), loanUUID, amount);
            
            if (success) {
                MessageUtil.sendMessage(player, "§aLoan payment successful!");
                MessageUtil.sendMessage(player, "§7Payment amount: §a" + currency.format(amount));
                if (loanUUID != null) {
                    MessageUtil.sendMessage(player, "§7Loan ID: §e" + loanUUID.toString().substring(0, 8));
                }
                MessageUtil.sendMessage(player, "§7Your loan balance has been updated.");
            } else {
                MessageUtil.sendErrorMessage(player, "Loan payment failed. Check that you have sufficient funds and a valid loan.");
            }
            
            return success ? 1 : 0;
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
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            BankManager.LoanManager loanManager = bankManager.getLoanManager();
            
            int creditScore = loanManager.calculateCreditScore(targetPlayer.getUUID());
            String creditRating = getCreditRating(creditScore);
            String creditColor = getCreditColor(creditScore);
            
            String title = targetPlayer.equals(player) ? "Your Credit Information" : 
                targetPlayer.getDisplayName().getString() + "'s Credit Information";
            
            MessageUtil.sendMessage(player, "§6=== " + title + " ===");
            MessageUtil.sendMessage(player, "§7Credit Score: " + creditColor + creditScore + " §7(" + creditRating + ")");
            
            MessageUtil.sendMessage(player, "§e§lLoan Eligibility:");
            double personalEligibility = loanManager.getMaxLoanEligibility(targetPlayer.getUUID(), Loan.LoanType.PERSONAL);
            double businessEligibility = loanManager.getMaxLoanEligibility(targetPlayer.getUUID(), Loan.LoanType.BUSINESS);
            double mortgageEligibility = loanManager.getMaxLoanEligibility(targetPlayer.getUUID(), Loan.LoanType.MORTGAGE);
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            MessageUtil.sendMessage(player, "§7• Personal Loan: §a" + currency.format(personalEligibility));
            MessageUtil.sendMessage(player, "§7• Business Loan: §a" + currency.format(businessEligibility));
            MessageUtil.sendMessage(player, "§7• Mortgage: §a" + currency.format(mortgageEligibility));
            
            // Show factors affecting credit score
            List<Loan> playerLoans = bankManager.getPlayerLoans(targetPlayer.getUUID());
            long activeLoans = playerLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.CURRENT || l.getStatus() == Loan.LoanStatus.LATE)
                .count();
            
            MessageUtil.sendMessage(player, "§7§lCredit Factors:");
            MessageUtil.sendMessage(player, "§7• Active Loans: §e" + activeLoans);
            MessageUtil.sendMessage(player, "§7• Total Loan History: §e" + playerLoans.size());
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while retrieving credit information: " + e.getMessage()));
            return 0;
        }
    }
    
    private String getCreditRating(int creditScore) {
        if (creditScore >= 800) return "Excellent";
        if (creditScore >= 740) return "Very Good";
        if (creditScore >= 670) return "Good";
        if (creditScore >= 580) return "Fair";
        return "Poor";
    }
    
    private String getCreditColor(int creditScore) {
        if (creditScore >= 740) return "§a"; // Green
        if (creditScore >= 670) return "§e"; // Yellow
        if (creditScore >= 580) return "§6"; // Orange
        return "§c"; // Red
    }
    
    private int listAllLoans(CommandSourceStack source) {
        try {
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> allLoans = bankManager.getAllActiveLoans();
            
            sendMessage(source, "§6=== All Loans (Admin View) ===");
            
            if (allLoans.isEmpty()) {
                sendMessage(source, "§7No active loans found.");
                return 1;
            }
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            
            sendMessage(source, "§7Found §e" + allLoans.size() + "§7 active loans:");
            
            for (Loan loan : allLoans) {
                String statusColor = switch (loan.getStatus()) {
                    case CURRENT -> "§a";
                    case LATE -> "§c";
                    case DEFAULT -> "§4";
                    default -> "§e";
                };
                
                sendMessage(source, "§7• ID: §e" + loan.getLoanId().toString().substring(0, 8) + 
                    " §7| Borrower: §e" + loan.getBorrowerId().toString().substring(0, 8) +
                    " §7| Type: §e" + loan.getType().getDisplayName() + 
                    " §7| Balance: §c" + currency.format(loan.getCurrentBalance()) + 
                    " §7| Status: " + statusColor + loan.getStatus().name());
            }
            
            // Summary statistics
            double totalOutstanding = allLoans.stream()
                .mapToDouble(Loan::getCurrentBalance)
                .sum();
            
            long overdueLoans = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.LATE || l.getStatus() == Loan.LoanStatus.DEFAULT)
                .count();
            
            sendMessage(source, "§7§lSummary:");
            sendMessage(source, "§7Total Outstanding: §c" + currency.format(totalOutstanding));
            sendMessage(source, "§7Overdue Loans: §c" + overdueLoans);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while listing loans: " + e.getMessage()));
            return 0;
        }
    }
    
    private int listLoanApplications(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return listLoanApplications(source, player);
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }
    }
    
    private int listLoanApplications(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            ServerPlayer executor = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            // Check if viewing own applications or has permission to view others
            if (!targetPlayer.getUUID().equals(executor.getUUID()) && 
                !CommandManager.hasPermission(source, "neoessentials.command.loan.list.others")) {
                MessageUtil.sendErrorMessage(executor, "You don't have permission to view other players' loan applications.");
                return 0;
            }
            
            List<Loan> allLoans = bankManager.getPlayerLoans(targetPlayer.getUUID());
            
            String targetName = targetPlayer.getUUID().equals(executor.getUUID()) ? "Your" : targetPlayer.getDisplayName().getString() + "'s";
            MessageUtil.sendMessage(executor, "§6=== " + targetName + " Loan Applications ===");
            
            if (allLoans.isEmpty()) {
                MessageUtil.sendMessage(executor, "§7No loan applications found.");
                return 1;
            }
            
            // Categorize loans by status
            List<Loan> pending = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.PENDING).toList();
            List<Loan> approved = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.APPROVED).toList();
            List<Loan> active = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.CURRENT).toList();
            List<Loan> overdue = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.LATE || l.getStatus() == Loan.LoanStatus.DEFAULT).toList();
            List<Loan> completed = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.PAID_OFF).toList();
            List<Loan> denied = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.FORECLOSED).toList();
            
            // Show pending applications
            if (!pending.isEmpty()) {
                MessageUtil.sendMessage(executor, "§e§lPending Applications (" + pending.size() + "):");
                for (Loan loan : pending) {
                    showLoanApplicationSummary(executor, loan);
                }
            }
            
            // Show approved applications
            if (!approved.isEmpty()) {
                MessageUtil.sendMessage(executor, "§a§lApproved Applications (" + approved.size() + "):");
                for (Loan loan : approved) {
                    showLoanApplicationSummary(executor, loan);
                }
            }
            
            // Show active loans
            if (!active.isEmpty()) {
                MessageUtil.sendMessage(executor, "§2§lActive Loans (" + active.size() + "):");
                for (Loan loan : active) {
                    showLoanApplicationSummary(executor, loan);
                }
            }
            
            // Show overdue loans
            if (!overdue.isEmpty()) {
                MessageUtil.sendMessage(executor, "§c§lOverdue Loans (" + overdue.size() + "):");
                for (Loan loan : overdue) {
                    showLoanApplicationSummary(executor, loan);
                }
            }
            
            // Show completed loans
            if (!completed.isEmpty()) {
                MessageUtil.sendMessage(executor, "§7§lCompleted Loans (" + completed.size() + "):");
                for (Loan loan : completed) {
                    showLoanApplicationSummary(executor, loan);
                }
            }
            
            // Show denied applications
            if (!denied.isEmpty()) {
                MessageUtil.sendMessage(executor, "§4§lDenied Applications (" + denied.size() + "):");
                for (Loan loan : denied) {
                    showLoanApplicationSummary(executor, loan);
                }
            }
            
            // Summary
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            double totalActive = active.stream().mapToDouble(Loan::getCurrentBalance).sum() + 
                               overdue.stream().mapToDouble(Loan::getCurrentBalance).sum();
            
            MessageUtil.sendMessage(executor, "");
            MessageUtil.sendMessage(executor, "§6§lSummary:");
            MessageUtil.sendMessage(executor, "§7Total Applications: §e" + allLoans.size());
            MessageUtil.sendMessage(executor, "§7Active Loan Balance: §c" + currency.format(totalActive));
            MessageUtil.sendMessage(executor, "");
            MessageUtil.sendMessage(executor, "§7Use §e/loan info <loan-id>§7 for detailed information.");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while listing loan applications: " + e.getMessage()));
            return 0;
        }
    }
    
    private int listRecentApplications(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> allLoans = bankManager.getPlayerLoans(player.getUUID());
            
            MessageUtil.sendMessage(player, "§6=== Your Recent Loan Applications ===");
            
            if (allLoans.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No loan applications found.");
                MessageUtil.sendMessage(player, "§e💡 Use §a/loan apply <amount> <type> <term> §eto apply for your first loan!");
                return 1;
            }
            
            // Sort by creation date (most recent first) and take first 5
            List<Loan> recentLoans = allLoans.stream()
                .sorted((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime()))
                .limit(5)
                .toList();
            
            MessageUtil.sendMessage(player, "§7Showing your " + recentLoans.size() + " most recent applications:");
            MessageUtil.sendMessage(player, "");
            
            for (int i = 0; i < recentLoans.size(); i++) {
                Loan loan = recentLoans.get(i);
                String statusColor = switch (loan.getStatus()) {
                    case PENDING -> "§e";
                    case APPROVED -> "§a";
                    case CURRENT -> "§2";
                    case LATE -> "§c";
                    case DEFAULT -> "§4";
                    case PAID_OFF -> "§7";
                    case FORECLOSED -> "§4";
                };
                
                Currency currency = loan.getCurrency();
                String dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(loan.getCreatedTime()));
                
                MessageUtil.sendMessage(player, "§7" + (i + 1) + ". §e" + loan.getLoanId().toString().substring(0, 8) + 
                    " §7| " + statusColor + loan.getStatus().name() + 
                    " §7| §e" + loan.getType().getDisplayName() + 
                    " §7| §a" + currency.format(loan.getPrincipalAmount()) + 
                    " §7| §8" + dateStr);
                
                if (loan.getStatus() == Loan.LoanStatus.CURRENT || loan.getStatus() == Loan.LoanStatus.LATE) {
                    MessageUtil.sendMessage(player, "§7   Balance: §c" + currency.format(loan.getCurrentBalance()) + 
                        " §7| Monthly: §e" + currency.format(loan.getMonthlyPayment()));
                }
            }
            
            MessageUtil.sendMessage(player, "");
            if (allLoans.size() > 5) {
                MessageUtil.sendMessage(player, "§7📋 You have §e" + (allLoans.size() - 5) + "§7 more applications.");
                MessageUtil.sendMessage(player, "§7📋 Use §a/loan applications §7to see all applications.");
            }
            MessageUtil.sendMessage(player, "§7📋 Use §a/loan info <loan-id> §7for detailed information.");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while listing recent applications: " + e.getMessage()));
            return 0;
        }
    }
    
    private void showLoanApplicationSummary(ServerPlayer player, Loan loan) {
        Currency currency = loan.getCurrency();
        String statusColor = switch (loan.getStatus()) {
            case PENDING -> "§e";
            case APPROVED -> "§a";
            case CURRENT -> "§2";
            case LATE -> "§c";
            case DEFAULT -> "§4";
            case PAID_OFF -> "§7";
            case FORECLOSED -> "§4";
        };
        
        String loanInfo = "§7  • §e" + loan.getLoanId().toString().substring(0, 8) + 
                         " §7| §e" + loan.getType().getDisplayName() + 
                         " §7| §a" + currency.format(loan.getPrincipalAmount());
        
        if (loan.getStatus() == Loan.LoanStatus.CURRENT || loan.getStatus() == Loan.LoanStatus.LATE) {
            loanInfo += " §7| Balance: §c" + currency.format(loan.getCurrentBalance());
            loanInfo += " §7| Payment: §e" + currency.format(loan.getMonthlyPayment());
        }
        
        loanInfo += " §7| " + statusColor + loan.getStatus().name();
        
        MessageUtil.sendMessage(player, loanInfo);
    }
    
    // Helper methods for sending messages to CommandSourceStack
    private static void sendMessage(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("§8[§6NeoEssentials§8] §r" + message), false);
    }
}
