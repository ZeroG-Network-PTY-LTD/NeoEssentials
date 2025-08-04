package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;

/**
 * Economy command implementation
 * Handles /bal, /pay, /eco commands
 */
public class EconomyCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /bal [player] - Check balance
        dispatcher.register(Commands.literal("bal")
            .executes(EconomyCommands::checkBalance)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(context -> checkOtherBalance(context, EntityArgument.getPlayer(context, "player")))
            )
        );
        
        // /balance [player] - Alias for /bal
        dispatcher.register(Commands.literal("balance")
            .executes(EconomyCommands::checkBalance)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(context -> checkOtherBalance(context, EntityArgument.getPlayer(context, "player")))
            )
        );
        
        // /pay <player> <amount> - Pay another player
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(context -> payPlayer(context, 
                        EntityArgument.getPlayer(context, "player"),
                        DoubleArgumentType.getDouble(context, "amount")))
                )
            )
        );
        
        // /eco <give|take|set> <player> <amount> - Admin economy commands
        dispatcher.register(Commands.literal("eco")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> adminGiveMoney(context,
                            EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount")))
                    )
                )
            )
            .then(Commands.literal("take")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> adminTakeMoney(context,
                            EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount")))
                    )
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> adminSetBalance(context,
                            EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount")))
                    )
                )
            )
        );
        
        // /baltop [limit] - Economy leaderboard
        dispatcher.register(Commands.literal("baltop")
            .executes(EconomyCommands::showLeaderboard)
            .then(Commands.argument("limit", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 50))
                .executes(context -> showLeaderboard(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "limit")))
            )
        );
        
        // /balancetop [limit] - Alias for /baltop
        dispatcher.register(Commands.literal("balancetop")
            .executes(EconomyCommands::showLeaderboard)
            .then(Commands.argument("limit", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 50))
                .executes(context -> showLeaderboard(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "limit")))
            )
        );
    }
    
    private static int checkBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        MessageUtil.sendMessage(player, "&aYour balance: &e" + formattedBalance);
        return 1;
    }
    
    private static int checkOtherBalance(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal balance = economyManager.getBalance(target.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        MessageUtil.sendMessage(sender, "&a" + target.getName().getString() + "'s balance: &e" + formattedBalance);
        return 1;
    }
    
    private static int payPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (sender.getUUID().equals(target.getUUID())) {
            MessageUtil.sendMessage(sender, "&cYou cannot pay yourself!");
            return 0;
        }
        
        BigDecimal payAmount = BigDecimal.valueOf(amount);
        
        if (!economyManager.hasBalance(sender.getUUID(), amount)) {
            MessageUtil.sendMessage(sender, "&cYou don't have enough money! You need " + 
                economyManager.formatCurrency(payAmount));
            return 0;
        }
        
        // Transfer money
        economyManager.withdrawBalance(sender.getUUID(), amount, "Payment to " + target.getName().getString());
        economyManager.depositBalance(target.getUUID(), amount, "Payment from " + sender.getName().getString());
        
        String formattedAmount = economyManager.formatCurrency(payAmount);
        MessageUtil.sendMessage(sender, "&aYou paid &e" + formattedAmount + " &ato " + target.getName().getString());
        MessageUtil.sendMessage(target, "&aYou received &e" + formattedAmount + " &afrom " + sender.getName().getString());
        
        return 1;
    }
    
    private static int adminGiveMoney(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal giveAmount = BigDecimal.valueOf(amount);
        economyManager.depositBalance(target.getUUID(), amount, "Admin give by " + admin.getName().getString());
        
        String formattedAmount = economyManager.formatCurrency(giveAmount);
        MessageUtil.sendMessage(admin, "&aGave &e" + formattedAmount + " &ato " + target.getName().getString());
        MessageUtil.sendMessage(target, "&aYou received &e" + formattedAmount + " &afrom an admin");
        
        return 1;
    }
    
    private static int adminTakeMoney(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal takeAmount = BigDecimal.valueOf(amount);
        economyManager.withdrawBalance(target.getUUID(), amount, "Admin take by " + admin.getName().getString());
        
        String formattedAmount = economyManager.formatCurrency(takeAmount);
        MessageUtil.sendMessage(admin, "&aTook &e" + formattedAmount + " &afrom " + target.getName().getString());
        MessageUtil.sendMessage(target, "&cAn admin took &e" + formattedAmount + " &cfrom your account");
        
        return 1;
    }
    
    private static int adminSetBalance(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal setAmount = BigDecimal.valueOf(amount);
        economyManager.setBalance(target.getUUID(), setAmount);
        
        String formattedAmount = economyManager.formatCurrency(setAmount);
        MessageUtil.sendMessage(admin, "&aSet " + target.getName().getString() + "'s balance to &e" + formattedAmount);
        MessageUtil.sendMessage(target, "&aYour balance has been set to &e" + formattedAmount);
        
        return 1;
    }
    
    private static int showLeaderboard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return showLeaderboard(context, 10); // Default to top 10
    }
    
    private static int showLeaderboard(CommandContext<CommandSourceStack> context, int limit) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        var topBalances = economyManager.getTopBalances(limit);
        
        if (topBalances.isEmpty()) {
            MessageUtil.sendMessage(player, "&cNo economy data available");
            return 0;
        }
        
        MessageUtil.sendMessage(player, "&6&l=== Economy Leaderboard ===");
        MessageUtil.sendMessage(player, "&7Showing top " + Math.min(limit, topBalances.size()) + " players:");
        MessageUtil.sendMessage(player, "");
        
        int position = 1;
        for (var entry : topBalances) {
            java.util.UUID playerUUID = entry.getKey();
            java.math.BigDecimal balance = entry.getValue();
            
            // Get player name from server
            net.minecraft.server.MinecraftServer server = player.getServer();
            String playerName = "Unknown Player";
            if (server != null) {
                net.minecraft.server.level.ServerPlayer targetPlayer = server.getPlayerList().getPlayer(playerUUID);
                if (targetPlayer != null) {
                    playerName = targetPlayer.getName().getString();
                } else {
                    // Try to get from game profile cache
                    var profileCache = server.getProfileCache();
                    if (profileCache != null) {
                        com.mojang.authlib.GameProfile profile = profileCache.get(playerUUID).orElse(null);
                        if (profile != null) {
                            playerName = profile.getName();
                        }
                    }
                }
            }
            
            String formattedBalance = economyManager.formatCurrency(balance);
            String positionColor = position <= 3 ? "&6" : "&f"; // Gold for top 3, white for others
            
            MessageUtil.sendMessage(player, positionColor + "#" + position + " &a" + playerName + " &7- &e" + formattedBalance);
            position++;
        }
        
        MessageUtil.sendMessage(player, "");
        MessageUtil.sendMessage(player, "&7Use &a/baltop <number> &7to see more entries (1-50)");
        
        return 1;
    }
}
