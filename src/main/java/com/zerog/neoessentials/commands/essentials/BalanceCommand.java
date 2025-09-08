package com.zerog.neoessentials.commands.essentials;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.EconomyManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.math.BigDecimal;

/**
 * Balance command implementation for NeoEssentials
 * Shows player balance and economy information
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class BalanceCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /balance - Show your balance
        dispatcher.register(Commands.literal("balance")
            .executes(BalanceCommand::showBalance)
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                .executes(BalanceCommand::showOtherBalance)
            )
        );
        
        // /bal - Alias for balance
        dispatcher.register(Commands.literal("bal")
            .executes(BalanceCommand::showBalance)
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                .executes(BalanceCommand::showOtherBalance)
            )
        );
        
        // /balancetop - Show top balances
        dispatcher.register(Commands.literal("balancetop")
            .executes(BalanceCommand::showTopBalances)
        );
        
        // /baltop - Alias for balancetop
        dispatcher.register(Commands.literal("baltop")
            .executes(BalanceCommand::showTopBalances)
        );
    }
    
    private static int showBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        EconomyManager economyManager = EconomyManager.getInstance();
        if (!economyManager.isEnabled()) {
            player.sendSystemMessage(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        player.sendSystemMessage(Component.literal("§6[Economy] §eYour balance: §a" + formattedBalance));
        
        return 1;
    }
    
    private static int showOtherBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        
        if (target == null) {
            executor.sendSystemMessage(Component.literal("§cPlayer '" + playerName + "' not found or not online"));
            return 0;
        }
        
        EconomyManager economyManager = EconomyManager.getInstance();
        if (!economyManager.isEnabled()) {
            executor.sendSystemMessage(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        BigDecimal balance = economyManager.getBalance(target.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        executor.sendSystemMessage(Component.literal("§6[Economy] §e" + target.getName().getString() + "'s balance: §a" + formattedBalance));
        
        return 1;
    }
    
    private static int showTopBalances(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        EconomyManager economyManager = EconomyManager.getInstance();
        if (!economyManager.isEnabled()) {
            player.sendSystemMessage(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        // Display top 10 balances
        var topBalances = economyManager.getTopBalances(10);
        
        player.sendSystemMessage(Component.literal("§6[Economy] §eTop Balances:"));
        
        for (int i = 0; i < topBalances.size(); i++) {
            var entry = topBalances.get(i);
            String playerName = "Unknown";
            var server = player.getServer();
            if (server != null) {
                ServerPlayer targetPlayer = server.getPlayerList().getPlayer(entry.getKey());
                if (targetPlayer != null) {
                    playerName = targetPlayer.getName().getString();
                }
            }
            String formattedBalance = economyManager.formatCurrency(entry.getValue());
            
            player.sendSystemMessage(Component.literal("§7" + (i + 1) + ". §e" + playerName + ": §a" + formattedBalance));
        }
        
        return 1;
    }
}
