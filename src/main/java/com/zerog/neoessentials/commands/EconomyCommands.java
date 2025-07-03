package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyTransaction;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles all economy-related commands, including /balance, /pay, and /baltop.
 */
public class EconomyCommands {
    
    /**
     * Registers all economy-related commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {        // Register /balance command
        dispatcher.register(
            Commands.literal("balance")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Show both wallet cash and bank account balance
                    try {
                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                        com.zerog.neoessentials.economy.WalletManager walletManager = economyManager.getWalletManager();
                        com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                            economyManager.getCurrencyManager().getDefaultCurrency();
                        
                        if (defaultCurrency == null) {
                            LanguageUtil.sendErrorMessage(player, "neoessentials.economy.no_default_currency");
                            return 0;
                        }
                        
                        // Get wallet cash balance
                        double cashBalance = walletManager.getCashBalance(player.getUUID(), defaultCurrency);
                        String formattedCash = String.format("$%.2f", cashBalance);
                        
                        // Get bank account balance
                        com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                            bankManager.getPrimaryAccount(player.getUUID());
                        
                        LanguageUtil.sendMessage(player, "neoessentials.economy.financial_status_header");
                        LanguageUtil.sendMessage(player, "neoessentials.economy.cash_on_hand", formattedCash);
                        
                        if (primaryAccount != null) {
                            double bankBalance = primaryAccount.getBalance(defaultCurrency);
                            String formattedBank = String.format("$%.2f", bankBalance);
                            LanguageUtil.sendMessage(player, "neoessentials.economy.bank_account_balance", 
                                formattedBank, primaryAccount.getAccountNumber());
                            
                            double totalWealth = cashBalance + bankBalance;
                            String formattedTotal = String.format("$%.2f", totalWealth);
                            LanguageUtil.sendMessage(player, "neoessentials.economy.total_wealth", formattedTotal);
                        } else {
                            LanguageUtil.sendMessage(player, "neoessentials.economy.no_bank_account");
                        }
                        
                        return 1;
                    } catch (Exception e) {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.economy.balance_check_error", e.getMessage());
                        return 0;
                    }
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance - show wallet, bank, and total (same as self-check)
                            try {
                                com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                    com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                com.zerog.neoessentials.economy.WalletManager walletManager = economyManager.getWalletManager();
                                com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                                com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                    economyManager.getCurrencyManager().getDefaultCurrency();
                                
                                if (defaultCurrency == null) {
                                    LanguageUtil.sendErrorMessage(source, "neoessentials.economy.no_default_currency");
                                    return 0;
                                }
                                
                                // Get wallet cash balance
                                double cashBalance = walletManager.getCashBalance(target.getUUID(), defaultCurrency);
                                String formattedCash = String.format("$%.2f", cashBalance);
                                
                                // Get bank account balance
                                com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                                    bankManager.getPrimaryAccount(target.getUUID());
                                
                                LanguageUtil.sendMessage(source, "neoessentials.economy.financial_status_header_other", target.getScoreboardName());
                                LanguageUtil.sendMessage(source, "neoessentials.economy.cash_on_hand", formattedCash);
                                
                                if (primaryAccount != null) {
                                    double bankBalance = primaryAccount.getBalance(defaultCurrency);
                                    String formattedBank = String.format("$%.2f", bankBalance);
                                    LanguageUtil.sendMessage(source, "neoessentials.economy.bank_account_balance", 
                                        formattedBank, primaryAccount.getAccountNumber());
                                    
                                    double totalWealth = cashBalance + bankBalance;
                                    String formattedTotal = String.format("$%.2f", totalWealth);
                                    LanguageUtil.sendMessage(source, "neoessentials.economy.total_wealth", formattedTotal);
                                } else {
                                    LanguageUtil.sendMessage(source, "neoessentials.economy.no_bank_account_other");
                                }
                                
                                return 1;
                            } catch (Exception e) {
                                LanguageUtil.sendErrorMessage(source, "neoessentials.economy.balance_check_error", e.getMessage());
                                return 0;
                            }
                        })
                )
        );

        // Register /bal alias for /balance
        dispatcher.register(
            Commands.literal("bal")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Show both wallet cash and bank account balance (same as /balance)
                    try {
                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                        com.zerog.neoessentials.economy.WalletManager walletManager = economyManager.getWalletManager();
                        com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                            economyManager.getCurrencyManager().getDefaultCurrency();
                        
                        if (defaultCurrency == null) {
                            LanguageUtil.sendErrorMessage(player, "No default currency configured");
                            return 0;
                        }
                        
                        // Get wallet cash balance
                        double cashBalance = walletManager.getCashBalance(player.getUUID(), defaultCurrency);
                        String formattedCash = String.format("$%.2f", cashBalance);
                        
                        // Get bank account balance
                        com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                            bankManager.getPrimaryAccount(player.getUUID());
                        
                        LanguageUtil.sendMessage(player, "§6§l--- Your Financial Status ---");
                        LanguageUtil.sendMessage(player, "§eCash on Hand: §a" + formattedCash);
                        
                        if (primaryAccount != null) {
                            double bankBalance = primaryAccount.getBalance(defaultCurrency);
                            String formattedBank = String.format("$%.2f", bankBalance);
                            LanguageUtil.sendMessage(player, "§eBank Account: §a" + formattedBank + 
                                " §7(Account: " + primaryAccount.getAccountNumber() + ")");
                            
                            double totalWealth = cashBalance + bankBalance;
                            String formattedTotal = String.format("$%.2f", totalWealth);
                            LanguageUtil.sendMessage(player, "§eTotal Wealth: §a" + formattedTotal);
                        } else {
                            LanguageUtil.sendMessage(player, "§eBank Account: §7None - Create one with: /bank create checking");
                        }
                        
                        return 1;
                    } catch (Exception e) {
                        LanguageUtil.sendErrorMessage(player, "Error checking balance: " + e.getMessage());
                        return 0;
                    }
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance - show wallet, bank, and total (same as self-check)
                            try {
                                com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                    com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                com.zerog.neoessentials.economy.WalletManager walletManager = economyManager.getWalletManager();
                                com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                                com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                    economyManager.getCurrencyManager().getDefaultCurrency();
                                
                                if (defaultCurrency == null) {
                                    LanguageUtil.sendErrorMessage(source, "No default currency configured");
                                    return 0;
                                }
                                
                                // Get wallet cash balance
                                double cashBalance = walletManager.getCashBalance(target.getUUID(), defaultCurrency);
                                String formattedCash = String.format("$%.2f", cashBalance);
                                
                                // Get bank account balance
                                com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                                    bankManager.getPrimaryAccount(target.getUUID());
                                
                                LanguageUtil.sendMessage(source, "§6§l--- " + target.getScoreboardName() + "'s Financial Status ---");
                                LanguageUtil.sendMessage(source, "§eCash on Hand: §a" + formattedCash);
                                
                                if (primaryAccount != null) {
                                    double bankBalance = primaryAccount.getBalance(defaultCurrency);
                                    String formattedBank = String.format("$%.2f", bankBalance);
                                    LanguageUtil.sendMessage(source, "§eBank Account: §a" + formattedBank + 
                                        " §7(Account: " + primaryAccount.getAccountNumber() + ")");
                                    
                                    double totalWealth = cashBalance + bankBalance;
                                    String formattedTotal = String.format("$%.2f", totalWealth);
                                    LanguageUtil.sendMessage(source, "§eTotal Wealth: §a" + formattedTotal);
                                } else {
                                    LanguageUtil.sendMessage(source, "§eBank Account: §7None");
                                }
                                
                                return 1;
                            } catch (Exception e) {
                                LanguageUtil.sendErrorMessage(source, "Error checking balance: " + e.getMessage());
                                return 0;
                            }
                        })
                )
        );

        // Register /money alias for /balance
        dispatcher.register(
            Commands.literal("money")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Show both wallet cash and bank account balance (same as /balance)
                    try {
                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                        com.zerog.neoessentials.economy.WalletManager walletManager = economyManager.getWalletManager();
                        com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                            economyManager.getCurrencyManager().getDefaultCurrency();
                        
                        if (defaultCurrency == null) {
                            LanguageUtil.sendErrorMessage(player, "No default currency configured");
                            return 0;
                        }
                        
                        // Get wallet cash balance
                        double cashBalance = walletManager.getCashBalance(player.getUUID(), defaultCurrency);
                        String formattedCash = String.format("$%.2f", cashBalance);
                        
                        // Get bank account balance
                        com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                            bankManager.getPrimaryAccount(player.getUUID());
                        
                        LanguageUtil.sendMessage(player, "§6§l--- Your Financial Status ---");
                        LanguageUtil.sendMessage(player, "§eCash on Hand: §a" + formattedCash);
                        
                        if (primaryAccount != null) {
                            double bankBalance = primaryAccount.getBalance(defaultCurrency);
                            String formattedBank = String.format("$%.2f", bankBalance);
                            LanguageUtil.sendMessage(player, "§eBank Account: §a" + formattedBank + 
                                " §7(Account: " + primaryAccount.getAccountNumber() + ")");
                            
                            double totalWealth = cashBalance + bankBalance;
                            String formattedTotal = String.format("$%.2f", totalWealth);
                            LanguageUtil.sendMessage(player, "§eTotal Wealth: §a" + formattedTotal);
                        } else {
                            LanguageUtil.sendMessage(player, "§eBank Account: §7None - Create one with: /bank create checking");
                        }
                        
                        return 1;
                    } catch (Exception e) {
                        LanguageUtil.sendErrorMessage(player, "Error checking balance: " + e.getMessage());
                        return 0;
                    }
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance - show wallet, bank, and total (same as self-check)
                            try {
                                com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                    com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                com.zerog.neoessentials.economy.WalletManager walletManager = economyManager.getWalletManager();
                                com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                                com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                    economyManager.getCurrencyManager().getDefaultCurrency();
                                
                                if (defaultCurrency == null) {
                                    LanguageUtil.sendErrorMessage(source, "No default currency configured");
                                    return 0;
                                }
                                
                                // Get wallet cash balance
                                double cashBalance = walletManager.getCashBalance(target.getUUID(), defaultCurrency);
                                String formattedCash = String.format("$%.2f", cashBalance);
                                
                                // Get bank account balance
                                com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                                    bankManager.getPrimaryAccount(target.getUUID());
                                
                                LanguageUtil.sendMessage(source, "§6§l--- " + target.getScoreboardName() + "'s Financial Status ---");
                                LanguageUtil.sendMessage(source, "§eCash on Hand: §a" + formattedCash);
                                
                                if (primaryAccount != null) {
                                    double bankBalance = primaryAccount.getBalance(defaultCurrency);
                                    String formattedBank = String.format("$%.2f", bankBalance);
                                    LanguageUtil.sendMessage(source, "§eBank Account: §a" + formattedBank + 
                                        " §7(Account: " + primaryAccount.getAccountNumber() + ")");
                                    
                                    double totalWealth = cashBalance + bankBalance;
                                    String formattedTotal = String.format("$%.2f", totalWealth);
                                    LanguageUtil.sendMessage(source, "§eTotal Wealth: §a" + formattedTotal);
                                } else {
                                    LanguageUtil.sendMessage(source, "§eBank Account: §7None");
                                }
                                
                                return 1;
                            } catch (Exception e) {
                                LanguageUtil.sendErrorMessage(source, "Error checking balance: " + e.getMessage());
                                return 0;
                            }
                        })
                )
        );        // Register /pay command
        dispatcher.register(
            Commands.literal("pay")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.pay"))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> {
                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    
                                    // Prevent paying yourself
                                    if (source.getUUID().equals(target.getUUID())) {
                                        LanguageUtil.sendErrorMessage(source, "You cannot pay yourself.");
                                        return 0;
                                    }
                                    
                                    try {
                                        // Use the wallet cash system for payments
                                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                        com.zerog.neoessentials.economy.WalletManager walletManager = economyManager.getWalletManager();
                                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                            economyManager.getCurrencyManager().getDefaultCurrency();
                                        
                                        if (defaultCurrency == null) {
                                            LanguageUtil.sendErrorMessage(source, "No default currency configured");
                                            return 0;
                                        }
                                        
                                        // Check if source has enough cash
                                        if (!walletManager.hasCash(source.getUUID(), defaultCurrency, amount)) {
                                            double currentCash = walletManager.getCashBalance(source.getUUID(), defaultCurrency);
                                            String formattedCash = String.format("$%.2f", currentCash);
                                            LanguageUtil.sendErrorMessage(source, "You don't have enough cash. Your cash on hand: " + formattedCash);
                                            return 0;
                                        }
                                        
                                        // Perform the transfer
                                        if (walletManager.transferCash(source.getUUID(), target.getUUID(), defaultCurrency, amount)) {
                                            String formattedAmount = String.format("$%.2f", amount);
                                            LanguageUtil.sendMessage(source, "You paid " + formattedAmount + " cash to " + target.getScoreboardName());
                                            LanguageUtil.sendMessage(target, source.getScoreboardName() + " paid you " + formattedAmount + " cash");
                                            return 1;
                                        } else {
                                            LanguageUtil.sendErrorMessage(source, "Payment failed. Check if the target player can receive this amount.");
                                            return 0;
                                        }
                                    } catch (Exception e) {
                                        LanguageUtil.sendErrorMessage(source, "Payment failed: " + e.getMessage());
                                        return 0;
                                    }
                                })
                        )
                )
        );

        // Register /baltop command
        dispatcher.register(
            Commands.literal("baltop")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.baltop"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Show baltop (default page 1)
                    displayBaltop(player, 1);
                    
                    return 1;
                })
                .then(
                    Commands.argument("page", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String pageStr = StringArgumentType.getString(context, "page");
                            
                            try {
                                int page = Integer.parseInt(pageStr);
                                if (page < 1) {
                                    page = 1;
                                }
                                
                                // Show baltop for specific page
                                displayBaltop(player, page);
                                
                            } catch (NumberFormatException e) {
                                LanguageUtil.sendErrorMessage(player, "Invalid page number: " + pageStr);
                            }
                            
                            return 1;
                        })
                )
        );

        // Register /eco command for admin economy management
        dispatcher.register(
            Commands.literal("eco")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.eco"))
                .then(
                    Commands.literal("give")
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.eco.give"))
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            double amount = DoubleArgumentType.getDouble(context, "amount");
                                            String reason = "Admin action by " + source.getScoreboardName();
                                            
                                            return handleAdminGive(source, target, amount, reason);
                                        })
                                        .then(
                                            Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                                    String reason = StringArgumentType.getString(context, "reason");
                                                    
                                                    return handleAdminGive(source, target, amount, reason);
                                                })
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("take")
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.eco.take"))
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            double amount = DoubleArgumentType.getDouble(context, "amount");
                                            String reason = "Admin action by " + source.getScoreboardName();
                                            
                                            return handleAdminTake(source, target, amount, reason);
                                        })
                                        .then(
                                            Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                                    String reason = StringArgumentType.getString(context, "reason");
                                                    
                                                    return handleAdminTake(source, target, amount, reason);
                                                })
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("set")
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.eco.set"))
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            double amount = DoubleArgumentType.getDouble(context, "amount");
                                            String reason = "Admin action by " + source.getScoreboardName();
                                            
                                            return handleAdminSet(source, target, amount, reason);
                                        })
                                        .then(
                                            Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                                    String reason = StringArgumentType.getString(context, "reason");
                                                    
                                                    return handleAdminSet(source, target, amount, reason);
                                                })
                                        )
                                )
                        )
                )
        );

        // Register transaction history command
        dispatcher.register(
            Commands.literal("ecotrans")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.ecotrans"))
                .executes(context -> {
                    // Show own transaction history
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    displayTransactionHistory(player, player.getUUID(), 1, 10);
                    return 1;
                })
                .then(
                    Commands.argument("page", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String pageStr = StringArgumentType.getString(context, "page");
                            
                            try {
                                int page = Integer.parseInt(pageStr);
                                if (page < 1) page = 1;
                                
                                // Show own transaction history with specified page
                                displayTransactionHistory(player, player.getUUID(), page, 10);
                                return 1;
                            } catch (NumberFormatException e) {
                                LanguageUtil.sendErrorMessage(player, "Invalid page number: " + pageStr);
                                return 0;
                            }
                        })
                )
                .then(
                    Commands.literal("view")
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.ecotrans.admin"))
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    
                                    // Show transaction history for specified player
                                    displayTransactionHistory(source, target.getUUID(), 1, 10);
                                    return 1;
                                })
                                .then(
                                    Commands.argument("page", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            String pageStr = StringArgumentType.getString(context, "page");
                                            
                                            try {
                                                int page = Integer.parseInt(pageStr);
                                                if (page < 1) page = 1;
                                                
                                                // Show transaction history for specified player with page
                                                displayTransactionHistory(source, target.getUUID(), page, 10);
                                                return 1;
                                            } catch (NumberFormatException e) {
                                                LanguageUtil.sendErrorMessage(source, "Invalid page number: " + pageStr);
                                                return 0;
                                            }
                                        })
                                )
                        )
                )
        );
        
        // Register /ecohelp command for help with economy commands
        dispatcher.register(
            Commands.literal("ecohelp")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.ecohelp"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    displayEconomyHelp(player);
                    return 1;
                })
        );
        
        // Add help subcommand to /eco
        dispatcher.register(
            Commands.literal("eco")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.eco"))
                .then(
                    Commands.literal("help")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            displayEconomyAdminHelp(player);
                            return 1;
                        })
                )
        );
        
        // Register /economy command with history subcommand
        dispatcher.register(
            Commands.literal("economy")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.economy"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    displayEconomyHelp(player);
                    return 1;
                })
                .then(
                    Commands.literal("history")
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.economy.history"))
                        .executes(context -> {
                            // Show own transaction history
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            displayTransactionHistory(player, player.getUUID(), 1, 10);
                            return 1;
                        })
                        .then(
                            Commands.argument("page", StringArgumentType.word())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String pageStr = StringArgumentType.getString(context, "page");
                                    
                                    try {
                                        int page = Integer.parseInt(pageStr);
                                        if (page < 1) page = 1;
                                        
                                        // Show own transaction history with specified page
                                        displayTransactionHistory(player, player.getUUID(), page, 10);
                                        return 1;
                                    } catch (NumberFormatException e) {
                                        LanguageUtil.sendErrorMessage(player, "Invalid page number: " + pageStr);
                                        return 0;
                                    }
                                })
                        )
                )
        );
        
        NeoEssentials.LOGGER.info("Registered economy commands");
    }
      /**
     * Displays the baltop (top player balances) to a player
     * 
     * @param player The player
     * @param page The page to display
     */
    private void displayBaltop(ServerPlayer player, int page) {
        int playersPerPage = 10;
        var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Get all balances
        Map<UUID, Double> allBalances = economyManager.getAllBalances();
        
        // Convert to list and sort
        var sortedBalances = allBalances.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))  // Sort descending
                .collect(Collectors.toList());
        
        // Calculate pages
        int totalPlayers = sortedBalances.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / playersPerPage);
        
        if (totalPlayers == 0) {
            LanguageUtil.sendMessage(player, "No players have any money yet.");
            return;
        }
        
        if (page > totalPages) {
            page = totalPages;
        }
        
        // Calculate start and end index
        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, totalPlayers);
        
        // Display header
        LanguageUtil.sendMessage(player, "§6§l--- Top Balances (Page " + page + "/" + totalPages + ") ---");
        
        // Display players
        for (int i = startIndex; i < endIndex; i++) {
            var entry = sortedBalances.get(i);
            String playerName = economyManager.getPlayerName(entry.getKey());
            String formattedBalance = economyManager.formatCurrency(entry.getValue());
            
            if (playerName == null) {
                playerName = "Unknown Player";
            }
            
            LanguageUtil.sendMessage(player, "§e" + (i + 1) + ". §r" + playerName + ": §a" + formattedBalance);
        }
        
        // Show navigation hint if there are more pages
        if (page < totalPages) {
            LanguageUtil.sendMessage(player, "§6§l--- Use /baltop " + (page + 1) + " for next page ---");
        }
    }
    
    /**
     * Display help information for economy commands
     * 
     * @param player The player to display help to
     */
    private void displayEconomyHelp(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "§6§l--- NeoEssentials Economy Commands ---");
        LanguageUtil.sendMessage(player, "§e/balance§r - Check your bank account balance");
        LanguageUtil.sendMessage(player, "§e/balance <player>§r - Check another player's bank account balance");
        LanguageUtil.sendMessage(player, "§e/pay <player> <amount>§r - Pay another player (requires bank accounts)");
        LanguageUtil.sendMessage(player, "§e/baltop§r - View the richest players");
        LanguageUtil.sendMessage(player, "§e/baltop <page>§r - View a specific page of baltop");
        LanguageUtil.sendMessage(player, "§e/ecotrans§r - View your transaction history");
        LanguageUtil.sendMessage(player, "§e/ecotrans <page>§r - View a specific page of your transaction history");
        LanguageUtil.sendMessage(player, "");
        LanguageUtil.sendMessage(player, "§c§lNote:§r All economy commands require a bank account!");
        LanguageUtil.sendMessage(player, "§eCreate one with: /bank create checking");
        
        // Show admin commands if player has permission
        if (PermissionUtil.hasPermission(player.createCommandSourceStack(), "neoessentials.command.eco")) {
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§6§l--- Admin Economy Commands ---");
            LanguageUtil.sendMessage(player, "§e/eco give <player> <amount> [reason]§r - Give money to a player's bank account");
            LanguageUtil.sendMessage(player, "§e/eco take <player> <amount> [reason]§r - Take money from a player's bank account");
            LanguageUtil.sendMessage(player, "§e/eco set <player> <amount> [reason]§r - Set a player's bank account balance");
            LanguageUtil.sendMessage(player, "§e/ecotrans view <player> [page]§r - View a player's transaction history");
        }
    }
    
    /**
     * Display admin help for economy commands
     * 
     * @param player The admin player to display help to
     */
    private void displayEconomyAdminHelp(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "§6§l--- NeoEssentials Admin Economy Commands ---");
        LanguageUtil.sendMessage(player, "§e/eco give <player> <amount> [reason]§r - Give money to a player's bank account");
        LanguageUtil.sendMessage(player, "§e/eco take <player> <amount> [reason]§r - Take money from a player's bank account");
        LanguageUtil.sendMessage(player, "§e/eco set <player> <amount> [reason]§r - Set a player's bank account balance");
        LanguageUtil.sendMessage(player, "§e/ecotrans view <player> [page]§r - View a player's transaction history");
        LanguageUtil.sendMessage(player, "");
        LanguageUtil.sendMessage(player, "§c§lNote:§r Players must have bank accounts to receive money!");
        LanguageUtil.sendMessage(player, "§eThey can create one with: /bank create checking");
    }
    
    /**
     * Displays the transaction history for a player
     * 
     * @param player The player viewing the history
     * @param targetUUID The UUID of the player whose history is to be displayed
     * @param page The page number
     * @param pageSize The number of entries per page
     */
    private void displayTransactionHistory(ServerPlayer player, UUID targetUUID, int page, int pageSize) {
        String targetName = NeoEssentials.getInstance().getDataManager().getEconomyManager().getPlayerName(targetUUID);
        
        // Get transactions
        List<EconomyTransaction> transactions = NeoEssentials.getInstance().getDataManager()
            .getEconomyManager().getRecentTransactions(targetUUID, 1000); // Get up to 1000 transactions
            
        if (transactions.isEmpty()) {
            if (player.getUUID().equals(targetUUID)) {
                LanguageUtil.sendMessage(player, "You don't have any transactions yet.");
            } else {
                LanguageUtil.sendMessage(player, targetName + " doesn't have any transactions yet.");
            }
            return;
        }
        
        // Calculate pagination
        int totalTransactions = transactions.size();
        int totalPages = (int) Math.ceil((double) totalTransactions / pageSize);
        
        if (page > totalPages) {
            page = totalPages;
        }
        
        int startIdx = (page - 1) * pageSize;
        int endIdx = Math.min(startIdx + pageSize, totalTransactions);
        
        // Get the transactions for this page
        List<EconomyTransaction> pageTransactions = transactions.subList(startIdx, endIdx);
        
        // Display header
        if (player.getUUID().equals(targetUUID)) {
            LanguageUtil.sendMessage(player, "§6§l--- Your Transaction History (Page " + page + "/" + totalPages + ") ---");
        } else {
            LanguageUtil.sendMessage(player, "§6§l--- " + targetName + "'s Transaction History (Page " + page + "/" + totalPages + ") ---");
        }
        
        // Display transactions
        for (EconomyTransaction transaction : pageTransactions) {
            // Format the transaction for display
            String message = formatTransaction(transaction);
            LanguageUtil.sendMessage(player, message);
        }
        
        // Display footer with navigation hints
        if (page < totalPages) {
            if (player.getUUID().equals(targetUUID)) {
                LanguageUtil.sendMessage(player, "§6§l--- Use /ecotrans " + (page + 1) + " for next page ---");
            } else {
                LanguageUtil.sendMessage(player, "§6§l--- Use /ecotrans view " + targetName + " " + (page + 1) + " for next page ---");
            }
        }
    }
    
    /**
     * Format a transaction for display
     *
     * @param transaction The transaction to format
     * @return Formatted transaction message
     */    private String formatTransaction(EconomyTransaction transaction) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = dateFormat.format(new Date(transaction.getTimestamp()));
        
        String typeStr;
        String amountStr;
        
        if (transaction.isDeposit()) {
            typeStr = "§a+";
            amountStr = "§a$" + String.format("%.2f", transaction.getAmount());
        } else if (transaction.isWithdrawal()) {
            typeStr = "§c-";
            amountStr = "§c$" + String.format("%.2f", transaction.getAmount());
        } else {
            typeStr = "§e";
            amountStr = "§e$" + String.format("%.2f", transaction.getAmount());
        }
        
        return "§7[" + dateStr + "] " + typeStr + transaction.getType() + " §r" + amountStr + "§7: " + transaction.getDescription() + 
                " §8(Balance: $" + String.format("%.2f", transaction.getBalanceAfter()) + ")";
    }
      /**
     * Handle admin give command
     * 
     * @param source The admin player
     * @param target The target player
     * @param amount The amount to give
     * @param reason The reason for giving
     * @return Command result (1 for success, 0 for failure)
     */
    private int handleAdminGive(ServerPlayer source, ServerPlayer target, double amount, String reason) {
        try {
            // Use the new bank-based economy system instead of pocket money
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
            
            // Check if target player has a bank account, create one if they don't
            com.zerog.neoessentials.economy.BankAccount primaryAccount = bankManager.getPrimaryAccount(target.getUUID());
            
            if (primaryAccount == null) {
                LanguageUtil.sendErrorMessage(source, target.getScoreboardName() + 
                    " must create a bank account first. Tell them to run: /bank create checking");
                return 0;
            }
            
            // Get the default currency
            com.zerog.neoessentials.economy.Currency defaultCurrency = 
                economyManager.getCurrencyManager().getDefaultCurrency();
            
            // Deposit the money into their primary bank account
            primaryAccount.deposit(defaultCurrency, amount, "Admin grant: " + reason);
            
            // Get formatted currency and new balance
            String formattedAmount = String.format("$%.2f", amount);
            String formattedBalance = String.format("$%.2f", primaryAccount.getBalance(defaultCurrency));
            
            // Send messages
            LanguageUtil.sendMessage(source, "Added " + formattedAmount + " to " + 
                    target.getScoreboardName() + "'s bank account (" + primaryAccount.getAccountNumber() + 
                    "). New balance: " + formattedBalance);
            LanguageUtil.sendMessage(target, "You received " + formattedAmount + " from an admin in your bank account. " +
                    "Account balance: " + formattedBalance);
            
            return 1;
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "Failed to give money: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Handle admin take command
     * 
     * @param source The admin player
     * @param target The target player
     * @param amount The amount to take
     * @param reason The reason for taking
     * @return Command result (1 for success, 0 for failure)
     */
    private int handleAdminTake(ServerPlayer source, ServerPlayer target, double amount, String reason) {
        try {
            // Use the new bank-based economy system instead of pocket money
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
            
            // Check if target player has a bank account
            com.zerog.neoessentials.economy.BankAccount primaryAccount = bankManager.getPrimaryAccount(target.getUUID());
            
            if (primaryAccount == null) {
                LanguageUtil.sendErrorMessage(source, target.getScoreboardName() + 
                    " does not have a bank account.");
                return 0;
            }
            
            // Get the default currency
            com.zerog.neoessentials.economy.Currency defaultCurrency = 
                economyManager.getCurrencyManager().getDefaultCurrency();
            
            // Check if they have sufficient funds
            double currentBalance = primaryAccount.getBalance(defaultCurrency);
            if (currentBalance < amount) {
                String formattedBalance = String.format("$%.2f", currentBalance);
                LanguageUtil.sendErrorMessage(source, target.getScoreboardName() + " does not have enough funds. " +
                        "Current balance: " + formattedBalance);
                return 0;
            }
            
            // Withdraw the money from their primary bank account
            primaryAccount.withdraw(defaultCurrency, amount, "Admin deduction: " + reason);
            
            // Get formatted currency and new balance
            String formattedAmount = String.format("$%.2f", amount);
            String formattedBalance = String.format("$%.2f", primaryAccount.getBalance(defaultCurrency));
            
            // Send messages
            LanguageUtil.sendMessage(source, "Removed " + formattedAmount + " from " + 
                    target.getScoreboardName() + "'s bank account (" + primaryAccount.getAccountNumber() + 
                    "). New balance: " + formattedBalance);
            LanguageUtil.sendMessage(target, "An admin removed " + formattedAmount + " from your bank account. " +
                    "Account balance: " + formattedBalance);
            
            return 1;
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "Failed to take money: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Handle admin set command
     * 
     * @param source The admin player
     * @param target The target player
     * @param amount The amount to set
     * @param reason The reason for setting
     * @return Command result (1 for success, 0 for failure)
     */
    private int handleAdminSet(ServerPlayer source, ServerPlayer target, double amount, String reason) {
        try {
            // Use the new bank-based economy system instead of pocket money
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
            
            // Check if target player has a bank account
            com.zerog.neoessentials.economy.BankAccount primaryAccount = bankManager.getPrimaryAccount(target.getUUID());
            
            if (primaryAccount == null) {
                LanguageUtil.sendErrorMessage(source, target.getScoreboardName() + 
                    " does not have a bank account.");
                return 0;
            }
            
            // Get the default currency
            com.zerog.neoessentials.economy.Currency defaultCurrency = 
                economyManager.getCurrencyManager().getDefaultCurrency();
            
            // Set the account balance by adjusting current balance
            double currentBalance = primaryAccount.getBalance(defaultCurrency);
            double difference = amount - currentBalance;
            
            if (difference > 0) {
                // Need to add money
                primaryAccount.deposit(defaultCurrency, difference, "Admin balance set: " + reason);
            } else if (difference < 0) {
                // Need to remove money
                primaryAccount.withdraw(defaultCurrency, Math.abs(difference), "Admin balance set: " + reason);
            }
            // If difference is 0, balance is already correct
            
            // Get formatted currency
            String formattedAmount = String.format("$%.2f", amount);
            
            // Send messages
            LanguageUtil.sendMessage(source, "Set " + target.getScoreboardName() + 
                "'s bank account balance (" + primaryAccount.getAccountNumber() + ") to " + formattedAmount);
            LanguageUtil.sendMessage(target, "Your bank account balance was set to " + formattedAmount + " by an admin");
            
            return 1;
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "Failed to set balance: " + e.getMessage());
            return 0;
        }
    }
}