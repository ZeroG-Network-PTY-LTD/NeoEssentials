package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.util.CommandConfigUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
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
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_BALANCE))
            .executes(EconomyCommands::checkBalance)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_BALANCE_OTHERS))
                .executes(context -> checkOtherBalance(context, EntityArgument.getPlayer(context, "player")))
            )
        );
        
        // /balance [player] - Alias for /bal
        dispatcher.register(Commands.literal("balance")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_BALANCE))
            .executes(EconomyCommands::checkBalance)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_BALANCE_OTHERS))
                .executes(context -> checkOtherBalance(context, EntityArgument.getPlayer(context, "player")))
            )
        );
        
        // /pay <player> <amount> - Pay another player
        dispatcher.register(Commands.literal("pay")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_PAY))
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
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_ALL))
            .then(Commands.literal("give")
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_GIVE))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> adminGiveMoney(context,
                            EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount")))
                    )
                )
            )
            .then(Commands.literal("take")
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_TAKE))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> adminTakeMoney(context,
                            EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount")))
                    )
                )
            )
            .then(Commands.literal("set")
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_SET))
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
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_TOP))
            .executes(EconomyCommands::showLeaderboard)
            .then(Commands.argument("limit", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 50))
                .executes(context -> showLeaderboard(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "limit")))
            )
        );
        
        // /balancetop [limit] - Alias for /baltop
        dispatcher.register(Commands.literal("balancetop")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.ECO_TOP))
            .executes(EconomyCommands::showLeaderboard)
            .then(Commands.argument("limit", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 50))
                .executes(context -> showLeaderboard(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "limit")))
            )
        );
    }
    
    private static int checkBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // Check if economy system is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "balance", "economy", "Economy")) {
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.economy.balance.your", formattedBalance));
        return 1;
    }
    
    private static int checkOtherBalance(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        // Check if economy system is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "balance", "economy", "Economy")) {
            return 0;
        }
        
        ServerPlayer sender = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal balance = economyManager.getBalance(target.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        MessageUtil.sendMessage(sender, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(sender, "neoessentials.economy.balance.other", target.getName().getString(), formattedBalance));
        return 1;
    }
    
    private static int payPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        // Check if economy system is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "pay", "economy", "Economy")) {
            return 0;
        }
        
        ServerPlayer sender = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (sender.getUUID().equals(target.getUUID())) {
            MessageUtil.sendMessage(sender, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(sender, "neoessentials.economy.pay.error.self"));
            return 0;
        }
        
        BigDecimal payAmount = BigDecimal.valueOf(amount);
        
        if (!economyManager.hasBalance(sender.getUUID(), amount)) {
            MessageUtil.sendMessage(sender, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(sender, "neoessentials.economy.pay.error.insufficient", economyManager.formatCurrency(payAmount)));
            return 0;
        }
        
        // Transfer money
        economyManager.withdrawBalance(sender.getUUID(), amount, "Payment to " + target.getName().getString());
        economyManager.depositBalance(target.getUUID(), amount, "Payment from " + sender.getName().getString());
        
        String formattedAmount = economyManager.formatCurrency(payAmount);
        MessageUtil.sendMessage(sender, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(sender, "neoessentials.economy.pay.success.sender", formattedAmount, target.getName().getString()));
        MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.economy.pay.success.receiver", formattedAmount, sender.getName().getString()));
        
        return 1;
    }
    
    private static int adminGiveMoney(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        // Check if economy system is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "economy", "economy", "Economy")) {
            return 0;
        }
        
        ServerPlayer admin = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal giveAmount = BigDecimal.valueOf(amount);
        economyManager.depositBalance(target.getUUID(), amount, "Admin give by " + admin.getName().getString());
        
        String formattedAmount = economyManager.formatCurrency(giveAmount);
        MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "neoessentials.economy.admin.give.success", formattedAmount, target.getName().getString()));
        MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.economy.admin.give.received", formattedAmount));
        
        return 1;
    }
    
    private static int adminTakeMoney(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        // Check if economy system is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "economy", "economy", "Economy")) {
            return 0;
        }
        
        ServerPlayer admin = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal takeAmount = BigDecimal.valueOf(amount);
        economyManager.withdrawBalance(target.getUUID(), amount, "Admin take by " + admin.getName().getString());
        
        String formattedAmount = economyManager.formatCurrency(takeAmount);
        MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "neoessentials.economy.admin.take.success", formattedAmount, target.getName().getString()));
        MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.economy.admin.take.notification", formattedAmount));
        
        return 1;
    }
    
    private static int adminSetBalance(CommandContext<CommandSourceStack> context, ServerPlayer target, double amount) throws CommandSyntaxException {
        // Check if economy system is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "economy", "economy", "Economy")) {
            return 0;
        }
        
        ServerPlayer admin = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        BigDecimal setAmount = BigDecimal.valueOf(amount);
        economyManager.setBalance(target.getUUID(), setAmount);
        
        String formattedAmount = economyManager.formatCurrency(setAmount);
        MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "neoessentials.economy.admin.set.success", target.getName().getString(), formattedAmount));
        MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.economy.admin.set.notification", formattedAmount));
        
        return 1;
    }
    
    private static int showLeaderboard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return showLeaderboard(context, 10); // Default to top 10
    }
    
    private static int showLeaderboard(CommandContext<CommandSourceStack> context, int limit) throws CommandSyntaxException {
        // Check if economy system is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "baltop", "economy", "Economy")) {
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        var topBalances = economyManager.getTopBalances(limit);
        
        if (topBalances.isEmpty()) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.economy.baltop.no_data"));
            return 0;
        }
        
        MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.economy.baltop.header"));
        MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.economy.baltop.showing", String.valueOf(Math.min(limit, topBalances.size()))));
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
            
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.economy.baltop.entry", positionColor, String.valueOf(position), playerName, formattedBalance));
            position++;
        }
        
        MessageUtil.sendMessage(player, "");
        MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.economy.baltop.footer"));
        
        return 1;
    }
}
