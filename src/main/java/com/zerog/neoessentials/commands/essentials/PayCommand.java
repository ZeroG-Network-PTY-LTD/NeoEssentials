package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.managers.EconomyManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;

/**
 * Pay command implementation for NeoEssentials
 * Allows players to send money to other players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PayCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /pay <player> <amount> - Send money to another player
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", StringArgumentType.word())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(PayCommand::payPlayer)
                )
            )
        );
    }
    
    private static int payPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer receiver = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        
        if (receiver == null) {
            sender.sendSystemMessage(Component.literal("§cPlayer '" + playerName + "' not found or not online"));
            return 0;
        }
        
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (!economyManager.isEnabled()) {
            sender.sendSystemMessage(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        // Prevent self-payment
        if (sender.getUUID().equals(receiver.getUUID())) {
            sender.sendSystemMessage(Component.literal("§cYou cannot pay yourself!"));
            return 0;
        }
        
        BigDecimal payAmount = BigDecimal.valueOf(amount);
        
        // Check if sender has enough money
        if (!economyManager.hasBalance(sender.getUUID(), payAmount)) {
            BigDecimal senderBalance = economyManager.getBalance(sender.getUUID());
            String formattedBalance = economyManager.formatCurrency(senderBalance);
            sender.sendSystemMessage(Component.literal("§cInsufficient funds! Your balance: §e" + formattedBalance));
            return 0;
        }
        
        // Perform the transaction
        String withdrawReason = "Payment to " + receiver.getName().getString();
        String depositReason = "Payment from " + sender.getName().getString();
        
        if (economyManager.withdrawBalance(sender.getUUID(), payAmount, withdrawReason) &&
            economyManager.depositBalance(receiver.getUUID(), payAmount, depositReason)) {
            
            String formattedAmount = economyManager.formatCurrency(payAmount);
            
            // Notify sender
            sender.sendSystemMessage(Component.literal("§6[Economy] §aYou paid §e" + formattedAmount + " §ato §e" + receiver.getName().getString()));
            
            // Notify receiver
            receiver.sendSystemMessage(Component.literal("§6[Economy] §aYou received §e" + formattedAmount + " §afrom §e" + sender.getName().getString()));
            
            return 1;
        } else {
            sender.sendSystemMessage(Component.literal("§cPayment failed! Please try again."));
            return 0;
        }
    }
}
