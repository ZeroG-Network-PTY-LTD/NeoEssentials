package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Comprehensive banking commands for the NeoEssentials economy system.
 * Provides full banking functionality including accounts, loans, and investments.
 */
public class BankCommands {
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bank")
                .executes(context -> showBankHelp(context.getSource()))
                .then(Commands.literal("help")
                    .executes(context -> showBankHelp(context.getSource())))
                
                // Account Management
                .then(Commands.literal("create")
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests(TabCompletionUtil.BANK_ACCOUNT_TYPE_SUGGESTIONS)
                        .executes(context -> createAccount(context.getSource(), 
                            StringArgumentType.getString(context, "type")))))
                .then(Commands.literal("deposit")
                    .executes(context -> showDepositHelp(context.getSource()))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .suggests(TabCompletionUtil.AMOUNT_SUGGESTIONS)
                        .executes(context -> showAccountsForDeposit(context.getSource(),
                            DoubleArgumentType.getDouble(context, "amount")))
                        .then(Commands.argument("to-account", StringArgumentType.string())
                            .suggests(TabCompletionUtil.BANK_ACCOUNT_SUGGESTIONS)
                            .executes(context -> showFromAccountsForDeposit(context.getSource(),
                                DoubleArgumentType.getDouble(context, "amount"),
                                StringArgumentType.getString(context, "to-account")))
                            .then(Commands.argument("from-account", StringArgumentType.string())
                                .suggests(TabCompletionUtil.BANK_ACCOUNT_SUGGESTIONS)
                                .executes(context -> depositMoney(context.getSource(),
                                    DoubleArgumentType.getDouble(context, "amount"),
                                    StringArgumentType.getString(context, "to-account"),
                                    StringArgumentType.getString(context, "from-account")))))))
                .then(Commands.literal("withdraw")
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .suggests(TabCompletionUtil.AMOUNT_SUGGESTIONS)
                        .then(Commands.argument("account", StringArgumentType.string())
                            .suggests(TabCompletionUtil.BANK_ACCOUNT_SUGGESTIONS)
                            .executes(context -> withdrawMoney(context.getSource(),
                                DoubleArgumentType.getDouble(context, "amount"),
                                StringArgumentType.getString(context, "account"))))))
                .then(Commands.literal("transfer")
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .suggests(TabCompletionUtil.AMOUNT_SUGGESTIONS)
                        .then(Commands.argument("to-account", StringArgumentType.string())
                            .suggests(TabCompletionUtil.BANK_ACCOUNT_SUGGESTIONS)
                            .executes(context -> transferMoney(context.getSource(),
                                DoubleArgumentType.getDouble(context, "amount"),
                                StringArgumentType.getString(context, "to-account"), null))
                            .then(Commands.argument("from-account", StringArgumentType.string())
                                .suggests(TabCompletionUtil.BANK_ACCOUNT_SUGGESTIONS)
                                .executes(context -> transferMoney(context.getSource(),
                                    DoubleArgumentType.getDouble(context, "amount"),
                                    StringArgumentType.getString(context, "to-account"),
                                    StringArgumentType.getString(context, "from-account")))))))
                .then(Commands.literal("balance")
                    .executes(context -> checkBalance(context.getSource(), null))
                    .then(Commands.argument("account", StringArgumentType.string())
                        .suggests(TabCompletionUtil.BANK_ACCOUNT_SUGGESTIONS)
                        .executes(context -> checkBalance(context.getSource(),
                            StringArgumentType.getString(context, "account")))))
                .then(Commands.literal("list")
                    .executes(context -> listAccounts(context.getSource())))
                .then(Commands.literal("info")
                    .then(Commands.argument("account", StringArgumentType.string())
                        .suggests(TabCompletionUtil.BANK_ACCOUNT_SUGGESTIONS)
                        .executes(context -> accountInfo(context.getSource(),
                            StringArgumentType.getString(context, "account")))))
                
                // Loan System
                .then(Commands.literal("loan")
                    .then(Commands.literal("apply")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(1.0))
                            .then(Commands.argument("type", StringArgumentType.string())
                                .then(Commands.argument("term-months", IntegerArgumentType.integer(1, 360))
                                    .executes(context -> applyForLoan(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "amount"),
                                        StringArgumentType.getString(context, "type"),
                                        IntegerArgumentType.getInteger(context, "term-months")))))))
                    .then(Commands.literal("pay")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> makeLoanPayment(context.getSource(),
                                DoubleArgumentType.getDouble(context, "amount"), null))
                            .then(Commands.argument("loan-id", StringArgumentType.string())
                                .executes(context -> makeLoanPayment(context.getSource(),
                                    DoubleArgumentType.getDouble(context, "amount"),
                                    StringArgumentType.getString(context, "loan-id"))))))
                    .then(Commands.literal("list")
                        .executes(context -> listLoans(context.getSource())))
                    .then(Commands.literal("info")
                        .then(Commands.argument("loan-id", StringArgumentType.string())
                            .executes(context -> loanInfo(context.getSource(),
                                StringArgumentType.getString(context, "loan-id"))))))
        );
    }
    
    private int showBankHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== NeoEssentials Banking System ===");
            MessageUtil.sendMessage(player, "§e§lAccount Management:");
            MessageUtil.sendMessage(player, "§e/bank create <type> §7- Create new account (checking, savings, business, investment)");
            MessageUtil.sendMessage(player, "§e/bank deposit <amount> <to-account> <from-account> §7- Transfer between your accounts");
            MessageUtil.sendMessage(player, "§e/bank withdraw §7- Shows info (withdrawals no longer needed!)");
            MessageUtil.sendMessage(player, "§e/bank transfer <amount> <to-account> [from-account] §7- Transfer money");
            MessageUtil.sendMessage(player, "§e/bank balance [account] §7- Check account balance");
            MessageUtil.sendMessage(player, "§e/bank list §7- List all your accounts");
            MessageUtil.sendMessage(player, "§e/bank info <account> §7- Account details");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lLoan System:");
            MessageUtil.sendMessage(player, "§e/bank loan apply <amount> <type> <term-months> §7- Apply for loan");
            MessageUtil.sendMessage(player, "§e/bank loan pay <amount> [loan-id] §7- Make loan payment");
            MessageUtil.sendMessage(player, "§e/bank loan list §7- List your loans");
            MessageUtil.sendMessage(player, "§e/bank loan info <loan-id> §7- Loan details");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§c§lNew System: §7All money is stored in bank accounts!");
            MessageUtil.sendMessage(player, "§7Use §e/pay <player> <amount> §7to pay others directly from your bank.");
            MessageUtil.sendMessage(player, "§7Account Types: §eChecking§7, §eSavings§7, §eBusiness§7, §eInvestment");
            MessageUtil.sendMessage(player, "§7Loan Types: §ePersonal§7, §eMortgage§7, §eBusiness");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int createAccount(CommandSourceStack source, String typeString) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            BankManager bankManager = economyManager.getBankManager();
            
            BankAccount.AccountType type;
            try {
                type = BankAccount.AccountType.valueOf(typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid account type. Valid types: checking, savings, business, joint, investment");
                return 0;
            }
            
            BankAccount account = bankManager.createAccount(player.getUUID(), type);
            if (account != null) {
                MessageUtil.sendSuccessMessage(player, "Successfully created " + type.name().toLowerCase() + 
                    " account: " + account.getAccountNumber());
                MessageUtil.sendMessage(player, "§7Account ID: §e" + account.getAccountId());
                MessageUtil.sendMessage(player, "§7Interest Rate: §e" + String.format("%.2f%%", account.getInterestRate() * 100));
                return 1;
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to create account. You may have reached the maximum number of accounts.");
                return 0;
            }
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int showDepositHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6§l--- Bank Deposit Help ---");
            MessageUtil.sendMessage(player, "§eUsage: /bank deposit <amount> <to-account> [from-account]");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§eExample: /bank deposit 100 CHK001 SAV002");
            MessageUtil.sendMessage(player, "§7This would transfer $100 from savings account SAV002 to checking account CHK001");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§eTo see your accounts: /bank list");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int showAccountsForDeposit(CommandSourceStack source, double amount) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<BankAccount> accounts = bankManager.getPlayerAccounts(player.getUUID());
            if (accounts.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "You don't have any bank accounts. Create one with: /bank create checking");
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§6§l--- Select Destination Account ---");
            MessageUtil.sendMessage(player, "§7Depositing: §e$" + String.format("%.2f", amount));
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§eYour accounts:");
            
            for (BankAccount account : accounts) {
                if (account.isActive()) {
                    Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
                    double balance = account.getBalance(defaultCurrency);
                    String accountInfo = String.format("§7- §e%s §7(%s) - §a$%.2f", 
                        account.getAccountNumber(), 
                        account.getType().toString().toLowerCase(), 
                        balance);
                    MessageUtil.sendMessage(player, accountInfo);
                }
            }
            
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Use: §e/bank deposit " + amount + " <account-number> [from-account]");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int showFromAccountsForDeposit(CommandSourceStack source, double amount, String toAccountNumber) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
            
            // Verify destination account exists and is owned by player
            BankAccount toAccount = bankManager.getAccountByNumber(toAccountNumber);
            if (toAccount == null || !toAccount.getOwnerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "Destination account not found or you don't have access to it.");
                return 0;
            }
            
            List<BankAccount> accounts = bankManager.getPlayerAccounts(player.getUUID());
            List<BankAccount> validFromAccounts = accounts.stream()
                .filter(account -> account.isActive() 
                    && !account.getAccountNumber().equals(toAccountNumber)
                    && account.getBalance(defaultCurrency) >= amount)
                .toList();
            
            if (validFromAccounts.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "No accounts found with sufficient funds ($" + 
                    String.format("%.2f", amount) + ") to transfer from.");
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§6§l--- Select Source Account ---");
            MessageUtil.sendMessage(player, "§7Transferring: §e$" + String.format("%.2f", amount));
            MessageUtil.sendMessage(player, "§7To: §e" + toAccount.getAccountNumber() + " §7(" + 
                toAccount.getType().toString().toLowerCase() + ")");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§eAccounts with sufficient funds:");
            
            for (BankAccount account : validFromAccounts) {
                double balance = account.getBalance(defaultCurrency);
                String accountInfo = String.format("§7- §e%s §7(%s) - §a$%.2f", 
                    account.getAccountNumber(), 
                    account.getType().toString().toLowerCase(), 
                    balance);
                MessageUtil.sendMessage(player, accountInfo);
            }
            
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Use: §e/bank deposit " + amount + " " + toAccountNumber + " <from-account>");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int depositMoney(CommandSourceStack source, double amount, String toAccountNumber, String fromAccountNumber) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            // Get both accounts
            BankAccount toAccount = bankManager.getAccountByNumber(toAccountNumber);
            BankAccount fromAccount = bankManager.getAccountByNumber(fromAccountNumber);
            
            // Verify accounts exist and are owned by player
            if (toAccount == null || !toAccount.getOwnerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "Destination account not found or you don't have access to it.");
                return 0;
            }
            
            if (fromAccount == null || !fromAccount.getOwnerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "Source account not found or you don't have access to it.");
                return 0;
            }
            
            // Check if accounts are different
            if (toAccount.getAccountNumber().equals(fromAccount.getAccountNumber())) {
                MessageUtil.sendErrorMessage(player, "Cannot transfer money to the same account.");
                return 0;
            }
            
            // Check if both accounts are active
            if (!toAccount.isActive() || !fromAccount.isActive()) {
                MessageUtil.sendErrorMessage(player, "One or both accounts are inactive.");
                return 0;
            }
            
            // Check if source account has sufficient funds
            double fromBalance = fromAccount.getBalance(defaultCurrency);
            if (fromBalance < amount) {
                MessageUtil.sendErrorMessage(player, "Insufficient funds in source account. Available: $" + 
                    String.format("%.2f", fromBalance));
                return 0;
            }
            
            // Perform the transfer
            if (bankManager.transferBetweenAccounts(fromAccount, toAccount, amount, defaultCurrency, 
                "Internal transfer via deposit command")) {
                
                MessageUtil.sendSuccessMessage(player, String.format(
                    "Successfully transferred $%.2f from %s to %s", 
                    amount, fromAccount.getAccountNumber(), toAccount.getAccountNumber()));
                    
                // Show updated balances
                MessageUtil.sendMessage(player, String.format("§7%s balance: §a$%.2f", 
                    fromAccount.getAccountNumber(), fromAccount.getBalance(defaultCurrency)));
                MessageUtil.sendMessage(player, String.format("§7%s balance: §a$%.2f", 
                    toAccount.getAccountNumber(), toAccount.getBalance(defaultCurrency)));
                    
                return 1;
            } else {
                MessageUtil.sendErrorMessage(player, "Transfer failed. Please try again.");
                return 0;
            }
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int showWithdrawHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6§l--- Bank Withdrawal Notice ---");
            MessageUtil.sendMessage(player, "§cWithdrawals are no longer needed!");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7In the new bank-only economy system, all your money is safely");
            MessageUtil.sendMessage(player, "§7stored in bank accounts. You can:");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e• Check balances: §f/bank balance [account]");
            MessageUtil.sendMessage(player, "§e• Transfer between accounts: §f/bank deposit <amount> <to> <from>");
            MessageUtil.sendMessage(player, "§e• Pay other players: §f/pay <player> <amount>");
            MessageUtil.sendMessage(player, "§e• View all accounts: §f/bank list");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§aYour money is always available directly from your bank accounts!");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    // Legacy method - no longer used but kept for compatibility
    private int withdrawMoney(CommandSourceStack source, double amount, String accountNumber) {
        return showWithdrawHelp(source);
    }
    
    private int transferMoney(CommandSourceStack source, double amount, String toAccountNumber, String fromAccountNumber) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            BankManager bankManager = economyManager.getBankManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            BankAccount fromAccount;
            if (fromAccountNumber != null) {
                fromAccount = bankManager.getAccountByNumber(fromAccountNumber);
                if (fromAccount == null || !fromAccount.getOwnerId().equals(player.getUUID())) {
                    MessageUtil.sendErrorMessage(player, "Source account not found or you don't have access to it.");
                    return 0;
                }
            } else {
                fromAccount = bankManager.getPrimaryAccount(player.getUUID());
                if (fromAccount == null) {
                    MessageUtil.sendErrorMessage(player, "No primary account found.");
                    return 0;
                }
            }
            
            BankAccount toAccount = bankManager.getAccountByNumber(toAccountNumber);
            if (toAccount == null) {
                MessageUtil.sendErrorMessage(player, "Destination account not found.");
                return 0;
            }
            
            if (bankManager.transferBetweenAccounts(fromAccount, toAccount, amount, defaultCurrency, "Player transfer")) {
                MessageUtil.sendSuccessMessage(player, "Successfully transferred " + 
                    defaultCurrency.format(amount) + " from " + fromAccount.getAccountNumber() + 
                    " to " + toAccount.getAccountNumber());
                return 1;
            } else {
                MessageUtil.sendErrorMessage(player, "Transfer failed. Check account balances and limits.");
                return 0;
            }
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int checkBalance(CommandSourceStack source, String accountNumber) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            BankManager bankManager = economyManager.getBankManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            BankAccount account;
            if (accountNumber != null) {
                account = bankManager.getAccountByNumber(accountNumber);
                if (account == null || !account.getOwnerId().equals(player.getUUID())) {
                    MessageUtil.sendErrorMessage(player, "Account not found or you don't have access to it.");
                    return 0;
                }
            } else {
                account = bankManager.getPrimaryAccount(player.getUUID());
                if (account == null) {
                    MessageUtil.sendErrorMessage(player, "No primary account found.");
                    return 0;
                }
            }
            
            double balance = account.getBalance(defaultCurrency);
            MessageUtil.sendMessage(player, "§6=== Account Balance ===");
            MessageUtil.sendMessage(player, "§7Account: §e" + account.getAccountNumber());
            MessageUtil.sendMessage(player, "§7Type: §e" + account.getType().name());
            MessageUtil.sendMessage(player, "§7Balance: §e" + defaultCurrency.format(balance));
            MessageUtil.sendMessage(player, "§7Interest Rate: §e" + String.format("%.2f%%", account.getInterestRate() * 100));
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int listAccounts(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            BankManager bankManager = economyManager.getBankManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            List<BankAccount> accounts = bankManager.getPlayerAccounts(player.getUUID());
            if (accounts.isEmpty()) {
                MessageUtil.sendMessage(player, "§7You have no bank accounts. Create one with §e/bank create checking");
                return 1;
            }
            
            MessageUtil.sendMessage(player, "§6=== Your Bank Accounts ===");
            for (BankAccount account : accounts) {
                if (account.isActive()) {
                    double balance = account.getBalance(defaultCurrency);
                    String status = account == bankManager.getPrimaryAccount(player.getUUID()) ? " §a(Primary)" : "";
                    MessageUtil.sendMessage(player, "§e" + account.getAccountNumber() + " §7(" + 
                        account.getType().name() + ") - " + defaultCurrency.format(balance) + status);
                }
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int accountInfo(CommandSourceStack source, String accountNumber) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            BankManager bankManager = economyManager.getBankManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            BankAccount account = bankManager.getAccountByNumber(accountNumber);
            if (account == null || !account.getOwnerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "Account not found or you don't have access to it.");
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§6=== Account Information ===");
            MessageUtil.sendMessage(player, "§7Account Number: §e" + account.getAccountNumber());
            MessageUtil.sendMessage(player, "§7Account Type: §e" + account.getType().name());
            MessageUtil.sendMessage(player, "§7Balance: §e" + defaultCurrency.format(account.getBalance(defaultCurrency)));
            MessageUtil.sendMessage(player, "§7Interest Rate: §e" + String.format("%.2f%%", account.getInterestRate() * 100));
            MessageUtil.sendMessage(player, "§7Credit Limit: §e" + defaultCurrency.format(account.getCreditLimit()));
            MessageUtil.sendMessage(player, "§7Status: §e" + (account.isActive() ? "Active" : "Inactive"));
            MessageUtil.sendMessage(player, "§7Created: §e" + new java.util.Date(account.getCreatedTime()));
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    // Loan system methods (placeholder implementations)
    private int applyForLoan(CommandSourceStack source, double amount, String loanType, int termMonths) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6Loan Application Submitted");
            MessageUtil.sendMessage(player, "§7Amount: §e$" + String.format("%.2f", amount));
            MessageUtil.sendMessage(player, "§7Type: §e" + loanType);
            MessageUtil.sendMessage(player, "§7Term: §e" + termMonths + " months");
            MessageUtil.sendMessage(player, "§7Status: §ePending Review");
            MessageUtil.sendMessage(player, "§7Note: Loan system is in development");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int makeLoanPayment(CommandSourceStack source, double amount, String loanId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6Loan Payment Processing");
            MessageUtil.sendMessage(player, "§7Amount: §e$" + String.format("%.2f", amount));
            if (loanId != null) {
                MessageUtil.sendMessage(player, "§7Loan ID: §e" + loanId);
            }
            MessageUtil.sendMessage(player, "§7Note: Loan system is in development");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int listLoans(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== Your Loans ===");
            MessageUtil.sendMessage(player, "§7No loans found");
            MessageUtil.sendMessage(player, "§7Note: Loan system is in development");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
    
    private int loanInfo(CommandSourceStack source, String loanId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== Loan Information ===");
            MessageUtil.sendMessage(player, "§7Loan ID: §e" + loanId);
            MessageUtil.sendMessage(player, "§7Note: Loan system is in development");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use banking commands"));
            return 0;
        }
    }
}
