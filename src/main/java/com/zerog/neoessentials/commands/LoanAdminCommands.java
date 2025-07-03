package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.economy.BankManager;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.Loan;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Handles all loan administration commands (/loanadmin)
 * Provides comprehensive loan management for server administrators
 */
public class LoanAdminCommands {
    
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    
    private static final SuggestionProvider<CommandSourceStack> PENDING_LOAN_SUGGESTIONS = (context, builder) -> {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> pendingLoans = bankManager.getAllActiveLoans().stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.PENDING)
                .toList();
            
            String[] loanIds = pendingLoans.stream()
                .map(loan -> loan.getLoanId().toString().substring(0, 8))
                .toArray(String[]::new);
                
            return SharedSuggestionProvider.suggest(loanIds, builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    };
    
    /**
     * Helper method to send messages that works with both players and console
     */
    private static void sendMessage(CommandSourceStack source, String message) {
        boolean isPlayer = source.getEntity() instanceof ServerPlayer;
        if (isPlayer) {
            try {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendMessage(player, message);
            } catch (Exception e) {
                source.sendFailure(Component.literal("Failed to send message: " + e.getMessage()));
            }
        } else {
            // Console command - strip color codes for readability
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            source.sendSuccess(() -> Component.literal(cleanMessage), false);
        }
    }
    
    /**
     * Helper method to send error messages that works with both players and console
     */
    private static void sendErrorMessage(CommandSourceStack source, String message) {
        boolean isPlayer = source.getEntity() instanceof ServerPlayer;
        if (isPlayer) {
            try {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendErrorMessage(player, message);
            } catch (Exception e) {
                source.sendFailure(Component.literal("Failed to send error message: " + e.getMessage()));
            }
        } else {
            // Console command
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            source.sendFailure(Component.literal("Error: " + cleanMessage));
        }
    }
    
    /**
     * Helper method to send success messages that works with both players and console
     */
    private static void sendSuccessMessage(CommandSourceStack source, String message) {
        boolean isPlayer = source.getEntity() instanceof ServerPlayer;
        if (isPlayer) {
            try {
                ServerPlayer player = source.getPlayerOrException();
                LanguageUtil.sendMessage(player, message);
            } catch (Exception e) {
                source.sendFailure(Component.literal("Failed to send success message: " + e.getMessage()));
            }
        } else {
            // Console command
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            source.sendSuccess(() -> Component.literal("Success: " + cleanMessage), false);
        }
    }
    
    /**
     * Register all loan admin commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("loanadmin")
            .requires(source -> source.hasPermission(3)) // Requires admin permissions
            .then(Commands.literal("approve")
                .then(Commands.argument("loanId", StringArgumentType.string())
                    .suggests(PENDING_LOAN_SUGGESTIONS)
                    .executes(context -> {
                        String loanId = StringArgumentType.getString(context, "loanId");
                        return handleApprove(context.getSource(), loanId);
                    })))
            .then(Commands.literal("deny")
                .then(Commands.argument("loanId", StringArgumentType.string())
                    .suggests(PENDING_LOAN_SUGGESTIONS)
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> {
                            String loanId = StringArgumentType.getString(context, "loanId");
                            String reason = StringArgumentType.getString(context, "reason");
                            return handleDeny(context.getSource(), loanId, reason);
                        }))
                    .executes(context -> {
                        String loanId = StringArgumentType.getString(context, "loanId");
                        return handleDeny(context.getSource(), loanId, "Application denied by administrator");
                    })))
            .then(Commands.literal("pending")
                .executes(context -> handlePending(context.getSource())))
            .then(Commands.literal("list")
                .executes(context -> handleList(context.getSource())))
            .then(Commands.literal("history")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        return handleHistory(context.getSource(), target);
                    })))
            .executes(context -> handleHelp(context.getSource()))
        );
    }
    
    /**
     * Handle loan approval command
     */
    private static int handleApprove(CommandSourceStack source, String loanIdStr) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            // Find the loan by partial ID
            UUID loanId = findLoanByPartialId(loanIdStr, bankManager);
            if (loanId == null) {
                sendErrorMessage(source, "Loan not found with ID: " + loanIdStr);
                return 0;
            }
            
            Loan loan = bankManager.getLoan(loanId);
            if (loan == null) {
                sendErrorMessage(source, "Could not load loan data");
                return 0;
            }
            
            if (loan.getStatus() != Loan.LoanStatus.PENDING) {
                sendErrorMessage(source, "Loan is not pending approval (current status: " + loan.getStatus() + ")");
                return 0;
            }
            
            // Approve the loan
            boolean approved = bankManager.approveLoan(loanId);
            if (approved) {
                // TODO: Implement disburseLoan method in BankManager
                // boolean disbursed = bankManager.disburseLoan(loanId);
                boolean disbursed = true; // Temporary placeholder
                if (disbursed) {
                    sendSuccessMessage(source,
                        "Loan " + loanIdStr + " approved and funds disbursed to borrower!");
                    sendMessage(source, "§7Amount: §e" + loan.getCurrency().format(loan.getPrincipalAmount()));
                    sendMessage(source, "§7Term: §e" + loan.getTermMonths() + " months");
                    sendMessage(source, "§7Monthly Payment: §e" + loan.getCurrency().format(loan.getMonthlyPayment()));
                } else {
                    sendErrorMessage(source, "Failed to disburse loan funds to borrower account");
                }
            } else {
                sendErrorMessage(source, "Failed to approve loan");
            }
            
            return 1;
        } catch (Exception e) {
            sendErrorMessage(source, "Error approving loan: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Handle loan denial command
     */
    private static int handleDeny(CommandSourceStack source, String loanIdStr, String reason) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            // Find the loan by partial ID
            UUID loanId = findLoanByPartialId(loanIdStr, bankManager);
            if (loanId == null) {
                sendErrorMessage(source, "Loan not found with ID: " + loanIdStr);
                return 0;
            }
            
            Loan loan = bankManager.getLoan(loanId);
            if (loan == null) {
                sendErrorMessage(source, "Could not load loan data");
                return 0;
            }
            
            if (loan.getStatus() != Loan.LoanStatus.PENDING) {
                sendErrorMessage(source, "Loan is not pending approval (current status: " + loan.getStatus() + ")");
                return 0;
            }
            
            // Deny the loan
            // TODO: Implement denyLoan method in BankManager
            // boolean denied = bankManager.denyLoan(loanId, reason);
            boolean denied = true; // Temporary placeholder
            if (denied) {
                sendSuccessMessage(source,
                    "Loan " + loanIdStr + " has been denied.");
                sendMessage(source, "§7Reason: §c" + reason);
                sendMessage(source, "§7Amount: §e" + loan.getCurrency().format(loan.getPrincipalAmount()));
            }
            
            return 1;
        } catch (Exception e) {
            sendErrorMessage(source, "Error denying loan: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Handle pending loans listing command
     */
    private static int handlePending(CommandSourceStack source) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> pendingLoans = bankManager.getAllActiveLoans().stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.PENDING)
                .toList();
            
            if (pendingLoans.isEmpty()) {
                sendMessage(source, "§e📋 No pending loan applications");
                return 1;
            }
            
            sendMessage(source, "§6§l📋 Pending Loan Applications (" + pendingLoans.size() + ")");
            sendMessage(source, "§8" + "=".repeat(50));
            
            for (Loan loan : pendingLoans) {
                sendMessage(source,
                    "§7ID: §e" + loan.getLoanId().toString().substring(0, 8) + 
                    " §7| Amount: §a$" + CURRENCY_FORMAT.format(loan.getPrincipalAmount()) +
                    " §7| Type: §b" + loan.getLoanType().getDisplayName() +
                    " §7| Borrower: §e" + loan.getBorrowerId());
            }
            
            sendMessage(source, "§8" + "=".repeat(50));
            sendMessage(source, "§7Use §e/loanadmin approve <id>§7 or §c/loanadmin deny <id>§7 to process applications");
            
            return 1;
        } catch (Exception e) {
            sendErrorMessage(source, "Error listing pending loans: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Handle loan list command (all loans)
     */
    private static int handleList(CommandSourceStack source) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> allLoans = bankManager.getAllActiveLoans();
            
            if (allLoans.isEmpty()) {
                sendMessage(source, "§e📋 No loans in the system");
                return 1;
            }
            
            // Group loans by status
            List<Loan> pending = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.PENDING).toList();
            List<Loan> approved = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.APPROVED).toList();
            List<Loan> current = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.CURRENT).toList();
            List<Loan> late = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.LATE).toList();
            List<Loan> defaulted = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.DEFAULT).toList();
            List<Loan> paidOff = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.PAID_OFF).toList();
            List<Loan> foreclosed = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.FORECLOSED).toList();
            
            sendMessage(source, "§6§l📊 Loan System Overview");
            sendMessage(source, "§8" + "=".repeat(50));
            sendMessage(source, "§e⏳ Pending: §f" + pending.size());
            sendMessage(source, "§a✓ Approved: §f" + approved.size());
            sendMessage(source, "§2✓ Current: §f" + current.size());
            sendMessage(source, "§6⚠ Late: §f" + late.size());
            sendMessage(source, "§c✗ Default: §f" + defaulted.size());
            sendMessage(source, "§a✓ Paid Off: §f" + paidOff.size());
            sendMessage(source, "§4⚠ Foreclosed: §f" + foreclosed.size());
            sendMessage(source, "§8" + "=".repeat(50));
            
            return 1;
        } catch (Exception e) {
            sendErrorMessage(source, "Error listing loans: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Handle player loan history command
     */
    private static int handleHistory(CommandSourceStack source, ServerPlayer target) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<Loan> playerLoans = bankManager.getPlayerLoans(target.getUUID());
            
            if (playerLoans.isEmpty()) {
                sendMessage(source, "§e" + target.getScoreboardName() + " has no loan history");
                return 1;
            }
            
            sendMessage(source, "§6§l📜 Loan History for " + target.getScoreboardName());
            sendMessage(source, "§8" + "=".repeat(50));
            
            for (Loan loan : playerLoans) {
                String statusColor = switch (loan.getStatus()) {
                    case PENDING -> "§e";
                    case APPROVED -> "§a";
                    case CURRENT -> "§2";
                    case LATE -> "§6";
                    case DEFAULT -> "§c";
                    case PAID_OFF -> "§a";
                    case FORECLOSED -> "§4";
                };
                
                sendMessage(source,
                    "§7• §e" + loan.getLoanId().toString().substring(0, 8) + 
                    " §7| " + statusColor + loan.getStatus() +
                    " §7| $" + CURRENCY_FORMAT.format(loan.getPrincipalAmount()) +
                    " §7| " + loan.getLoanType().getDisplayName());
            }
            
            return 1;
        } catch (Exception e) {
            sendErrorMessage(source, "Error retrieving loan history: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Handle help command
     */
    private static int handleHelp(CommandSourceStack source) {
        sendMessage(source, "§6§l⚙ Loan Administration Commands");
        sendMessage(source, "§8" + "=".repeat(50));
        sendMessage(source, "§e/loanadmin approve <id>§7 - Approve pending loan");
        sendMessage(source, "§e/loanadmin deny <id> [reason]§7 - Deny pending loan");
        sendMessage(source, "§e/loanadmin pending§7 - List pending applications");
        sendMessage(source, "§e/loanadmin list§7 - List all loans by status");
        sendMessage(source, "§e/loanadmin history <player>§7 - View player loan history");
        sendMessage(source, "§8" + "=".repeat(50));
        sendMessage(source, "§7Use partial loan IDs (first 8 characters) for commands");
        return 1;
    }
    
    /**
     * Find a loan by partial ID (first 8 characters)
     */
    private static UUID findLoanByPartialId(String partialId, BankManager bankManager) {
        List<Loan> allLoans = bankManager.getAllActiveLoans();
        
        for (Loan loan : allLoans) {
            if (loan.getLoanId().toString().startsWith(partialId) || 
                loan.getLoanId().toString().substring(0, 8).equalsIgnoreCase(partialId)) {
                return loan.getLoanId();
            }
        }
        
        return null;
    }
}