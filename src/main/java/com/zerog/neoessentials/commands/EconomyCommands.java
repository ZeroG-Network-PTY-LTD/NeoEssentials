package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyTransaction;
import com.zerog.neoessentials.utils.MessageUtil;
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
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance using bank account system
                    try {
                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                        com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                        
                        // Get player's primary bank account
                        com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                            bankManager.getPrimaryAccount(player.getUUID());
                        
                        if (primaryAccount == null) {
                            MessageUtil.sendErrorMessage(player, "You don't have a bank account. Create one with: /bank create checking");
                            return 0;
                        }
                        
                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                            economyManager.getCurrencyManager().getDefaultCurrency();
                        double balance = primaryAccount.getBalance(defaultCurrency);
                        String formattedBalance = String.format("$%.2f", balance);
                        
                        MessageUtil.sendMessage(player, "Your bank account balance: " + formattedBalance + 
                            " (Account: " + primaryAccount.getAccountNumber() + ")");
                        
                        return 1;
                    } catch (Exception e) {
                        MessageUtil.sendErrorMessage(player, "Error checking balance: " + e.getMessage());
                        return 0;
                    }
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance using bank account system
                            try {
                                com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                    com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                                
                                // Get target player's primary bank account
                                com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                                    bankManager.getPrimaryAccount(target.getUUID());
                                
                                if (primaryAccount == null) {
                                    MessageUtil.sendMessage(source, target.getScoreboardName() + " doesn't have a bank account.");
                                    return 1;
                                }
                                
                                com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                    economyManager.getCurrencyManager().getDefaultCurrency();
                                double balance = primaryAccount.getBalance(defaultCurrency);
                                String formattedBalance = String.format("$%.2f", balance);
                                
                                MessageUtil.sendMessage(source, target.getScoreboardName() + "'s bank account balance: " + 
                                    formattedBalance + " (Account: " + primaryAccount.getAccountNumber() + ")");
                                
                                return 1;
                            } catch (Exception e) {
                                MessageUtil.sendErrorMessage(source, "Error checking balance: " + e.getMessage());
                                return 0;
                            }
                        })
                )
        );        // Register /bal alias for /balance
        dispatcher.register(
            Commands.literal("bal")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance using bank account system
                    try {
                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                        com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                        
                        // Get player's primary bank account
                        com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                            bankManager.getPrimaryAccount(player.getUUID());
                        
                        if (primaryAccount == null) {
                            MessageUtil.sendErrorMessage(player, "You don't have a bank account. Create one with: /bank create checking");
                            return 0;
                        }
                        
                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                            economyManager.getCurrencyManager().getDefaultCurrency();
                        double balance = primaryAccount.getBalance(defaultCurrency);
                        String formattedBalance = String.format("$%.2f", balance);
                        
                        MessageUtil.sendMessage(player, "Your bank account balance: " + formattedBalance + 
                            " (Account: " + primaryAccount.getAccountNumber() + ")");
                        
                        return 1;
                    } catch (Exception e) {
                        MessageUtil.sendErrorMessage(player, "Error checking balance: " + e.getMessage());
                        return 0;
                    }
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance using bank account system
                            try {
                                com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                    com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                                
                                // Get target player's primary bank account
                                com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                                    bankManager.getPrimaryAccount(target.getUUID());
                                
                                if (primaryAccount == null) {
                                    MessageUtil.sendMessage(source, target.getScoreboardName() + " doesn't have a bank account.");
                                    return 1;
                                }
                                
                                com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                    economyManager.getCurrencyManager().getDefaultCurrency();
                                double balance = primaryAccount.getBalance(defaultCurrency);
                                String formattedBalance = String.format("$%.2f", balance);
                                
                                MessageUtil.sendMessage(source, target.getScoreboardName() + "'s bank account balance: " + 
                                    formattedBalance + " (Account: " + primaryAccount.getAccountNumber() + ")");
                                
                                return 1;
                            } catch (Exception e) {
                                MessageUtil.sendErrorMessage(source, "Error checking balance: " + e.getMessage());
                                return 0;
                            }
                        })
                )
        );

        // Register /money alias for /balance
        dispatcher.register(
            Commands.literal("money")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance using bank account system
                    try {
                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                        com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                        
                        // Get player's primary bank account
                        com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                            bankManager.getPrimaryAccount(player.getUUID());
                        
                        if (primaryAccount == null) {
                            MessageUtil.sendErrorMessage(player, "You don't have a bank account. Create one with: /bank create checking");
                            return 0;
                        }
                        
                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                            economyManager.getCurrencyManager().getDefaultCurrency();
                        double balance = primaryAccount.getBalance(defaultCurrency);
                        String formattedBalance = String.format("$%.2f", balance);
                        
                        MessageUtil.sendMessage(player, "Your bank account balance: " + formattedBalance + 
                            " (Account: " + primaryAccount.getAccountNumber() + ")");
                        
                        return 1;
                    } catch (Exception e) {
                        MessageUtil.sendErrorMessage(player, "Error checking balance: " + e.getMessage());
                        return 0;
                    }
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance using bank account system
                            try {
                                com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                    com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                                
                                // Get target player's primary bank account
                                com.zerog.neoessentials.economy.BankAccount primaryAccount = 
                                    bankManager.getPrimaryAccount(target.getUUID());
                                
                                if (primaryAccount == null) {
                                    MessageUtil.sendMessage(source, target.getScoreboardName() + " doesn't have a bank account.");
                                    return 1;
                                }
                                
                                com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                    economyManager.getCurrencyManager().getDefaultCurrency();
                                double balance = primaryAccount.getBalance(defaultCurrency);
                                String formattedBalance = String.format("$%.2f", balance);
                                
                                MessageUtil.sendMessage(source, target.getScoreboardName() + "'s bank account balance: " + 
                                    formattedBalance + " (Account: " + primaryAccount.getAccountNumber() + ")");
                                
                                return 1;
                            } catch (Exception e) {
                                MessageUtil.sendErrorMessage(source, "Error checking balance: " + e.getMessage());
                                return 0;
                            }
                        })
                )
        );        // Register /pay command
        dispatcher.register(
            Commands.literal("pay")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.pay"))
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
                                        MessageUtil.sendErrorMessage(source, "You cannot pay yourself.");
                                        return 0;
                                    }
                                    
                                    try {
                                        // Use the bank-based economy system
                                        com.zerog.neoessentials.economy.EconomyManager economyManager = 
                                            com.zerog.neoessentials.economy.EconomyManager.getInstance();
                                        com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                                        
                                        // Get both players' primary bank accounts
                                        com.zerog.neoessentials.economy.BankAccount sourceAccount = 
                                            bankManager.getPrimaryAccount(source.getUUID());
                                        com.zerog.neoessentials.economy.BankAccount targetAccount = 
                                            bankManager.getPrimaryAccount(target.getUUID());
                                        
                                        if (sourceAccount == null) {
                                            MessageUtil.sendErrorMessage(source, "You don't have a bank account. Create one with: /bank create checking");
                                            return 0;
                                        }
                                        
                                        if (targetAccount == null) {
                                            MessageUtil.sendErrorMessage(source, target.getScoreboardName() + 
                                                " doesn't have a bank account to receive payments.");
                                            return 0;
                                        }
                                        
                                        com.zerog.neoessentials.economy.Currency defaultCurrency = 
                                            economyManager.getCurrencyManager().getDefaultCurrency();
                                        
                                        // Check if source has enough funds
                                        if (sourceAccount.getBalance(defaultCurrency) < amount) {
                                            String formattedBalance = String.format("$%.2f", sourceAccount.getBalance(defaultCurrency));
                                            MessageUtil.sendErrorMessage(source, "You don't have enough funds. Your balance: " + formattedBalance);
                                            return 0;
                                        }
                                        
                                        // Perform the transfer
                                        sourceAccount.withdraw(defaultCurrency, amount, "Payment to " + target.getScoreboardName());
                                        targetAccount.deposit(defaultCurrency, amount, "Payment from " + source.getScoreboardName());
                                        
                                        String formattedAmount = String.format("$%.2f", amount);
                                        MessageUtil.sendMessage(source, "You paid " + formattedAmount + " to " + target.getScoreboardName());
                                        MessageUtil.sendMessage(target, source.getScoreboardName() + " paid you " + formattedAmount);
                                        
                                        return 1;
                                    } catch (Exception e) {
                                        MessageUtil.sendErrorMessage(source, "Payment failed: " + e.getMessage());
                                        return 0;
                                    }
                                })
                        )
                )
        );

        // Register /baltop command
        dispatcher.register(
            Commands.literal("baltop")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.baltop"))
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
                                MessageUtil.sendErrorMessage(player, "Invalid page number: " + pageStr);
                            }
                            
                            return 1;
                        })
                )
        );

        // Register /eco command for admin economy management
        dispatcher.register(
            Commands.literal("eco")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.eco"))
                .then(
                    Commands.literal("give")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.eco.give"))
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
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.eco.take"))
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
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.eco.set"))
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
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.ecotrans"))
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
                                MessageUtil.sendErrorMessage(player, "Invalid page number: " + pageStr);
                                return 0;
                            }
                        })
                )
                .then(
                    Commands.literal("view")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.ecotrans.admin"))
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
                                                MessageUtil.sendErrorMessage(source, "Invalid page number: " + pageStr);
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
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.ecohelp"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    displayEconomyHelp(player);
                    return 1;
                })
        );
        
        // Add help subcommand to /eco
        dispatcher.register(
            Commands.literal("eco")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.eco"))
                .then(
                    Commands.literal("help")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            displayEconomyAdminHelp(player);
                            return 1;
                        })
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
            MessageUtil.sendMessage(player, "No players have any money yet.");
            return;
        }
        
        if (page > totalPages) {
            page = totalPages;
        }
        
        // Calculate start and end index
        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, totalPlayers);
        
        // Display header
        MessageUtil.sendMessage(player, "§6§l--- Top Balances (Page " + page + "/" + totalPages + ") ---");
        
        // Display players
        for (int i = startIndex; i < endIndex; i++) {
            var entry = sortedBalances.get(i);
            String playerName = economyManager.getPlayerName(entry.getKey());
            String formattedBalance = economyManager.formatCurrency(entry.getValue());
            
            if (playerName == null) {
                playerName = "Unknown Player";
            }
            
            MessageUtil.sendMessage(player, "§e" + (i + 1) + ". §r" + playerName + ": §a" + formattedBalance);
        }
        
        // Show navigation hint if there are more pages
        if (page < totalPages) {
            MessageUtil.sendMessage(player, "§6§l--- Use /baltop " + (page + 1) + " for next page ---");
        }
    }
    
    /**
     * Display help information for economy commands
     * 
     * @param player The player to display help to
     */
    private void displayEconomyHelp(ServerPlayer player) {
        MessageUtil.sendMessage(player, "§6§l--- NeoEssentials Economy Commands ---");
        MessageUtil.sendMessage(player, "§e/balance§r - Check your balance");
        MessageUtil.sendMessage(player, "§e/balance <player>§r - Check another player's balance");
        MessageUtil.sendMessage(player, "§e/pay <player> <amount>§r - Pay another player");
        MessageUtil.sendMessage(player, "§e/baltop§r - View the richest players");
        MessageUtil.sendMessage(player, "§e/baltop <page>§r - View a specific page of baltop");
        MessageUtil.sendMessage(player, "§e/ecotrans§r - View your transaction history");
        MessageUtil.sendMessage(player, "§e/ecotrans <page>§r - View a specific page of your transaction history");
        
        // Show admin commands if player has permission
        if (CommandManager.hasPermission(player.createCommandSourceStack(), "neoessentials.command.eco")) {
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§6§l--- Admin Economy Commands ---");
            MessageUtil.sendMessage(player, "§e/eco give <player> <amount> [reason]§r - Give money to a player");
            MessageUtil.sendMessage(player, "§e/eco take <player> <amount> [reason]§r - Take money from a player");
            MessageUtil.sendMessage(player, "§e/eco set <player> <amount> [reason]§r - Set a player's balance");
            MessageUtil.sendMessage(player, "§e/ecotrans view <player> [page]§r - View a player's transaction history");
        }
    }
    
    /**
     * Display admin help for economy commands
     * 
     * @param player The admin player to display help to
     */
    private void displayEconomyAdminHelp(ServerPlayer player) {
        MessageUtil.sendMessage(player, "§6§l--- NeoEssentials Admin Economy Commands ---");
        MessageUtil.sendMessage(player, "§e/eco give <player> <amount> [reason]§r - Give money to a player");
        MessageUtil.sendMessage(player, "§e/eco take <player> <amount> [reason]§r - Take money from a player");
        MessageUtil.sendMessage(player, "§e/eco set <player> <amount> [reason]§r - Set a player's balance");
        MessageUtil.sendMessage(player, "§e/ecotrans view <player> [page]§r - View a player's transaction history");
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
                MessageUtil.sendMessage(player, "You don't have any transactions yet.");
            } else {
                MessageUtil.sendMessage(player, targetName + " doesn't have any transactions yet.");
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
            MessageUtil.sendMessage(player, "§6§l--- Your Transaction History (Page " + page + "/" + totalPages + ") ---");
        } else {
            MessageUtil.sendMessage(player, "§6§l--- " + targetName + "'s Transaction History (Page " + page + "/" + totalPages + ") ---");
        }
        
        // Display transactions
        for (EconomyTransaction transaction : pageTransactions) {
            // Format the transaction for display
            String message = formatTransaction(transaction);
            MessageUtil.sendMessage(player, message);
        }
        
        // Display footer with navigation hints
        if (page < totalPages) {
            if (player.getUUID().equals(targetUUID)) {
                MessageUtil.sendMessage(player, "§6§l--- Use /ecotrans " + (page + 1) + " for next page ---");
            } else {
                MessageUtil.sendMessage(player, "§6§l--- Use /ecotrans view " + targetName + " " + (page + 1) + " for next page ---");
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
                MessageUtil.sendErrorMessage(source, target.getScoreboardName() + 
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
            MessageUtil.sendSuccessMessage(source, "Added " + formattedAmount + " to " + 
                    target.getScoreboardName() + "'s bank account (" + primaryAccount.getAccountNumber() + 
                    "). New balance: " + formattedBalance);
            MessageUtil.sendMessage(target, "You received " + formattedAmount + " from an admin in your bank account. " +
                    "Account balance: " + formattedBalance);
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "Failed to give money: " + e.getMessage());
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
                MessageUtil.sendErrorMessage(source, target.getScoreboardName() + 
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
                MessageUtil.sendErrorMessage(source, target.getScoreboardName() + " does not have enough funds. " +
                        "Current balance: " + formattedBalance);
                return 0;
            }
            
            // Withdraw the money from their primary bank account
            primaryAccount.withdraw(defaultCurrency, amount, "Admin deduction: " + reason);
            
            // Get formatted currency and new balance
            String formattedAmount = String.format("$%.2f", amount);
            String formattedBalance = String.format("$%.2f", primaryAccount.getBalance(defaultCurrency));
            
            // Send messages
            MessageUtil.sendSuccessMessage(source, "Removed " + formattedAmount + " from " + 
                    target.getScoreboardName() + "'s bank account (" + primaryAccount.getAccountNumber() + 
                    "). New balance: " + formattedBalance);
            MessageUtil.sendMessage(target, "An admin removed " + formattedAmount + " from your bank account. " +
                    "Account balance: " + formattedBalance);
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "Failed to take money: " + e.getMessage());
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
                MessageUtil.sendErrorMessage(source, target.getScoreboardName() + 
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
            MessageUtil.sendSuccessMessage(source, "Set " + target.getScoreboardName() + 
                "'s bank account balance (" + primaryAccount.getAccountNumber() + ") to " + formattedAmount);
            MessageUtil.sendMessage(target, "Your bank account balance was set to " + formattedAmount + " by an admin");
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(source, "Failed to set balance: " + e.getMessage());
            return 0;
        }
    }
}