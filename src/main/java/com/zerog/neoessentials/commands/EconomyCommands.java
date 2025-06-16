package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

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
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /balance command
        dispatcher.register(
            Commands.literal("balance")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance
                    double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(player.getUUID());
                    MessageUtil.sendMessage(player, "Your balance: $" + String.format("%.2f", balance));
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance
                            double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(target.getUUID());
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: $" + String.format("%.2f", balance));
                            
                            return 1;
                        })
                )
        );

        // Register /bal alias for /balance
        dispatcher.register(
            Commands.literal("bal")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Check own balance
                    double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(player.getUUID());
                    MessageUtil.sendMessage(player, "Your balance: $" + String.format("%.2f", balance));
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance
                            double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(target.getUUID());
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: $" + String.format("%.2f", balance));
                            
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
                    double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(player.getUUID());
                    MessageUtil.sendMessage(player, "Your balance: $" + String.format("%.2f", balance));
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.balance.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Check another player's balance
                            double balance = NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(target.getUUID());
                            MessageUtil.sendMessage(source, target.getScoreboardName() + "'s balance: $" + String.format("%.2f", balance));
                            
                            return 1;
                        })
                )
        );

        // Register /pay command
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
                                    
                                    // Pay another player
                                    boolean success = NeoEssentials.getInstance().getDataManager().getEconomyManager()
                                            .transfer(source.getUUID(), target.getUUID(), amount);
                                    
                                    if (success) {
                                        MessageUtil.sendMessage(source, "You paid $" + String.format("%.2f", amount) + " to " + target.getScoreboardName());
                                        MessageUtil.sendMessage(target, source.getScoreboardName() + " paid you $" + String.format("%.2f", amount));
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
                                            
                                            // Give money to player
                                            double newBalance = NeoEssentials.getInstance().getDataManager().getEconomyManager()
                                                    .addBalance(target.getUUID(), amount);
                                            
                                            MessageUtil.sendSuccessMessage(source, "Added $" + String.format("%.2f", amount) + " to " + 
                                                    target.getScoreboardName() + "'s balance. New balance: $" + String.format("%.2f", newBalance));
                                            MessageUtil.sendMessage(target, "You received $" + String.format("%.2f", amount) + " from an admin. " +
                                                    "New balance: $" + String.format("%.2f", newBalance));
                                            
                                            return 1;
                                        })
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
                                            
                                            // Take money from player
                                            boolean success = NeoEssentials.getInstance().getDataManager().getEconomyManager()
                                                    .removeBalance(target.getUUID(), amount);
                                            
                                            if (success) {
                                                double newBalance = NeoEssentials.getInstance().getDataManager().getEconomyManager()
                                                        .getBalance(target.getUUID());
                                                
                                                MessageUtil.sendSuccessMessage(source, "Removed $" + String.format("%.2f", amount) + " from " + 
                                                        target.getScoreboardName() + "'s balance. New balance: $" + String.format("%.2f", newBalance));
                                                MessageUtil.sendMessage(target, "An admin removed $" + String.format("%.2f", amount) + " from your account. " +
                                                        "New balance: $" + String.format("%.2f", newBalance));
                                                
                                                return 1;
                                            } else {
                                                MessageUtil.sendErrorMessage(source, target.getScoreboardName() + " does not have enough funds. " +
                                                        "Current balance: $" + String.format("%.2f", NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(target.getUUID())));
                                                return 0;
                                            }
                                        })
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
                                              // Set player's balance
                                            NeoEssentials.getInstance().getDataManager().getEconomyManager()
                                                    .setBalance(target.getUUID(), amount);
                                            
                                            MessageUtil.sendSuccessMessage(source, "Set " + target.getScoreboardName() + "'s balance to $" + String.format("%.2f", amount));                                            MessageUtil.sendMessage(target, "Your balance was set to $" + String.format("%.2f", amount) + " by an admin");
                                            return 1;
                                        })
                                )
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
        
        // Get all balances
        Map<UUID, Double> allBalances = NeoEssentials.getInstance().getDataManager().getEconomyManager().getAllBalances();
        
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
        MessageUtil.sendMessage(player, "Top Balances (Page " + page + "/" + totalPages + "):");
        
        // Display players
        for (int i = startIndex; i < endIndex; i++) {
            var entry = sortedBalances.get(i);
            String playerName = NeoEssentials.getInstance().getDataManager().getEconomyManager().getPlayerName(entry.getKey());
            
            if (playerName == null) {
                playerName = "Unknown Player";
            }
            
            MessageUtil.sendMessage(player, (i + 1) + ". " + playerName + ": $" + String.format("%.2f", entry.getValue()));
        }
    }
}