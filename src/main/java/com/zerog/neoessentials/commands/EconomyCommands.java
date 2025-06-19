package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.NeoEssentials;
<<<<<<< HEAD
<<<<<<< HEAD
import com.zerog.neoessentials.data.EconomyTransaction;
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
import com.zerog.neoessentials.data.EconomyTransaction;
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

<<<<<<< HEAD
<<<<<<< HEAD
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
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
<<<<<<< HEAD
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {        // Register /balance command
=======
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /balance command
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        dispatcher.register(
            Commands.literal("balance")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance
<<<<<<< HEAD
                    var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                    double balance = economyManager.getBalance(player.getUUID());
                    String formattedBalance = economyManager.formatCurrency(balance);
                    
                    MessageUtil.sendMessage(player, "Your balance: " + formattedBalance);
=======
                    double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(player.getUUID());
                    MessageUtil.sendMessage(player, "Your balance: $" + String.format("%.2f", balance));
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance
<<<<<<< HEAD
                            var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                            double balance = economyManager.getBalance(target.getUUID());
                            String formattedBalance = economyManager.formatCurrency(balance);
                            
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: " + formattedBalance);
=======
                            double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(target.getUUID());
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: $" + String.format("%.2f", balance));
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                            
                            return 1;
                        })
                )
<<<<<<< HEAD
        );        // Register /bal alias for /balance
=======
        );

        // Register /bal alias for /balance
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        dispatcher.register(
            Commands.literal("bal")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance
<<<<<<< HEAD
                    var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                    double balance = economyManager.getBalance(player.getUUID());
                    String formattedBalance = economyManager.formatCurrency(balance);
                    
                    MessageUtil.sendMessage(player, "Your balance: " + formattedBalance);
=======
                    double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(player.getUUID());
                    MessageUtil.sendMessage(player, "Your balance: $" + String.format("%.2f", balance));
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance
<<<<<<< HEAD
                            var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                            double balance = economyManager.getBalance(target.getUUID());
                            String formattedBalance = economyManager.formatCurrency(balance);
                            
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: " + formattedBalance);
=======
                            double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(target.getUUID());
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: $" + String.format("%.2f", balance));
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                            
                            return 1;
                        })
                )
        );

        // Register /money alias for /balance
        dispatcher.register(
            Commands.literal("money")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance
<<<<<<< HEAD
                    var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                    double balance = economyManager.getBalance(player.getUUID());
                    String formattedBalance = economyManager.formatCurrency(balance);
                    
                    MessageUtil.sendMessage(player, "Your balance: " + formattedBalance);
                    
                    return 1;                })
=======
                    double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(player.getUUID());
                    MessageUtil.sendMessage(player, "Your balance: $" + String.format("%.2f", balance));
                    
                    return 1;
                })
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance
<<<<<<< HEAD
                            var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                            double balance = economyManager.getBalance(target.getUUID());
                            String formattedBalance = economyManager.formatCurrency(balance);
                            
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: " + formattedBalance);
=======
                            double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(target.getUUID());
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: $" + String.format("%.2f", balance));
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                            
                            return 1;
                        })
                )
<<<<<<< HEAD
        );        // Register /pay command
=======
        );

        // Register /pay command
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
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
<<<<<<< HEAD
                                    var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                                    
                                    // Prevent paying yourself
                                    if (source.getUUID().equals(target.getUUID())) {
                                        MessageUtil.sendErrorMessage(source, "You cannot pay yourself.");
                                        return 0;
                                    }
                                    
                                    // Pay another player
<<<<<<< HEAD
                                    boolean success = economyManager.transfer(source.getUUID(), target.getUUID(), amount);
                                    
                                    if (success) {
                                        String formattedAmount = economyManager.formatCurrency(amount);
                                        MessageUtil.sendMessage(source, "You paid " + formattedAmount + " to " + target.getScoreboardName());
                                        MessageUtil.sendMessage(target, source.getScoreboardName() + " paid you " + formattedAmount);
=======
                                    boolean success = NeoEssentials.getInstance().getDataManager().getEconomyManager()
                                            .transfer(source.getUUID(), target.getUUID(), amount);
                                    
                                    if (success) {
                                        MessageUtil.sendMessage(source, "You paid $" + String.format("%.2f", amount) + " to " + target.getScoreboardName());
                                        MessageUtil.sendMessage(target, source.getScoreboardName() + " paid you $" + String.format("%.2f", amount));
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                                        return 1;
                                    } else {
                                        MessageUtil.sendErrorMessage(source, "You don't have enough funds to make this payment.");
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
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
=======
                                            String reason = "Admin action by " + source.getScoreboardName();
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
                                            
                                            return handleAdminGive(source, target, amount, reason);
                                        })
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
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
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
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
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
=======
                                            String reason = "Admin action by " + source.getScoreboardName();
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
                                            
                                            return handleAdminTake(source, target, amount, reason);
                                        })
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
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
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
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
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
                                              // Set player's balance
                                            NeoEssentials.getInstance().getDataManager().getEconomyManager()
                                                    .setBalance(target.getUUID(), amount);
                                            
                                            MessageUtil.sendSuccessMessage(source, "Set " + target.getScoreboardName() + "'s balance to $" + String.format("%.2f", amount));                                            MessageUtil.sendMessage(target, "Your balance was set to $" + String.format("%.2f", amount) + " by an admin");
                                            return 1;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
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
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
                                        })
                                )
                        )
                )
        );
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
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
        
<<<<<<< HEAD
        NeoEssentials.LOGGER.info("Registered economy commands");
    }
      /**
=======
=======
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
        NeoEssentials.LOGGER.info("Registered economy commands");
    }
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
     * Displays the baltop (top player balances) to a player
     * 
     * @param player The player
     * @param page The page to display
     */
    private void displayBaltop(ServerPlayer player, int page) {
        int playersPerPage = 10;
<<<<<<< HEAD
        var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Get all balances
        Map<UUID, Double> allBalances = economyManager.getAllBalances();
=======
        
        // Get all balances
        Map<UUID, Double> allBalances = NeoEssentials.getInstance().getDataManager().getEconomyManager().getAllBalances();
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        
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
<<<<<<< HEAD
        MessageUtil.sendMessage(player, "§6§l--- Top Balances (Page " + page + "/" + totalPages + ") ---");
=======
        MessageUtil.sendMessage(player, "Top Balances (Page " + page + "/" + totalPages + "):");
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        
        // Display players
        for (int i = startIndex; i < endIndex; i++) {
            var entry = sortedBalances.get(i);
<<<<<<< HEAD
            String playerName = economyManager.getPlayerName(entry.getKey());
            String formattedBalance = economyManager.formatCurrency(entry.getValue());
=======
            String playerName = NeoEssentials.getInstance().getDataManager().getEconomyManager().getPlayerName(entry.getKey());
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
            
            if (playerName == null) {
                playerName = "Unknown Player";
            }
            
<<<<<<< HEAD
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
        var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Apply the admin transaction
        double newBalance = economyManager.adminAddBalance(
            target.getUUID(), 
            source.getUUID(), 
            amount, 
            "Admin give: " + reason
        );
        
        // Get formatted currency
        String formattedAmount = economyManager.formatCurrency(amount);
        String formattedBalance = economyManager.formatCurrency(newBalance);
        
        // Send messages
        MessageUtil.sendSuccessMessage(source, "Added " + formattedAmount + " to " + 
                target.getScoreboardName() + "'s balance. New balance: " + formattedBalance);
        MessageUtil.sendMessage(target, "You received " + formattedAmount + " from an admin. " +
                "New balance: " + formattedBalance);
        
        return 1;
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
        var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Apply the admin transaction
        boolean success = economyManager.adminRemoveBalance(
            target.getUUID(), 
            source.getUUID(), 
            amount, 
            "Admin take: " + reason
        );
        
        if (success) {
            double newBalance = economyManager.getBalance(target.getUUID());
            
            // Get formatted currency
            String formattedAmount = economyManager.formatCurrency(amount);
            String formattedBalance = economyManager.formatCurrency(newBalance);
            
            // Send messages
            MessageUtil.sendSuccessMessage(source, "Removed " + formattedAmount + " from " + 
                    target.getScoreboardName() + "'s balance. New balance: " + formattedBalance);
            MessageUtil.sendMessage(target, "An admin removed " + formattedAmount + " from your account. " +
                    "New balance: " + formattedBalance);
            
            return 1;
        } else {
            String formattedBalance = economyManager.formatCurrency(
                economyManager.getBalance(target.getUUID())
            );
            
            MessageUtil.sendErrorMessage(source, target.getScoreboardName() + " does not have enough funds. " +
                    "Current balance: " + formattedBalance);
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
        var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Apply the admin transaction
        double newBalance = economyManager.adminSetBalance(
            target.getUUID(), 
            source.getUUID(), 
            amount, 
            "Admin set: " + reason
        );
        
        // Get formatted currency
        String formattedAmount = economyManager.formatCurrency(newBalance);
        
        // Send messages
        MessageUtil.sendSuccessMessage(source, "Set " + target.getScoreboardName() + "'s balance to " + formattedAmount);
        MessageUtil.sendMessage(target, "Your balance was set to " + formattedAmount + " by an admin");
        
        return 1;
<<<<<<< HEAD
=======
            MessageUtil.sendMessage(player, (i + 1) + ". " + playerName + ": $" + String.format("%.2f", entry.getValue()));
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> f3a56e8 (Refactor economy commands and transaction management)
    }
}