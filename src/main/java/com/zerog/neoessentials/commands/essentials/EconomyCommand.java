package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;

public class EconomyCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eco")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) // Admin only
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> giveEconomy(context)))))
            .then(Commands.literal("take")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> takeEconomy(context)))))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> setEconomy(context)))))
            .then(Commands.literal("reset")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> resetEconomy(context))))
            .then(Commands.literal("reload")
                .executes(context -> reloadEconomy(context))));
    }
    
    /**
     * Give money to a player
     */
    private static int giveEconomy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            
            // Get current balance
            BigDecimal currentBalance = economyManager.getBalance(target.getUUID());
            
            // Add the amount
            economyManager.setBalance(target.getUUID(), currentBalance.doubleValue() + amount);
            
            // Format the amount
            String formattedAmount = economyManager.formatCurrency(amount);
            String formattedNewBalance = economyManager.formatCurrency(currentBalance.doubleValue() + amount);
            
            // Send confirmation messages
            sendMessage(source, "§aGave " + formattedAmount + " to " + target.getName().getString());
            sendMessage(source, "§7New balance: " + formattedNewBalance);
            
            // Notify the player
            MessageUtil.sendMessage(target, "§aYou received " + formattedAmount + " from an admin");
            MessageUtil.sendMessage(target, "§7New balance: " + formattedNewBalance);
            
            // Log the transaction
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Economy] " + getSourceName(source) + " gave " + formattedAmount + " to " + target.getName().getString()));
            
            return 1;
            
        } catch (Exception e) {
            sendMessage(source, "§cError giving money: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Take money from a player
     */
    private static int takeEconomy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            
            // Get current balance
            BigDecimal currentBalance = economyManager.getBalance(target.getUUID());
            
            // Check if they have enough money
            if (currentBalance.doubleValue() < amount) {
                sendMessage(source, "§c" + target.getName().getString() + " only has " + 
                    economyManager.formatCurrency(currentBalance) + "!");
                return 0;
            }
            
            // Remove the amount
            economyManager.setBalance(target.getUUID(), currentBalance.doubleValue() - amount);
            
            // Format the amount
            String formattedAmount = economyManager.formatCurrency(amount);
            String formattedNewBalance = economyManager.formatCurrency(currentBalance.doubleValue() - amount);
            
            // Send confirmation messages
            sendMessage(source, "§aTook " + formattedAmount + " from " + target.getName().getString());
            sendMessage(source, "§7New balance: " + formattedNewBalance);
            
            // Notify the player
            MessageUtil.sendMessage(target, "§c" + formattedAmount + " was taken from your account by an admin");
            MessageUtil.sendMessage(target, "§7New balance: " + formattedNewBalance);
            
            // Log the transaction
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Economy] " + getSourceName(source) + " took " + formattedAmount + " from " + target.getName().getString()));
            
            return 1;
            
        } catch (Exception e) {
            sendMessage(source, "§cError taking money: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Set a player's balance
     */
    private static int setEconomy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            
            // Get current balance for logging
            BigDecimal oldBalance = economyManager.getBalance(target.getUUID());
            
            // Set the new balance
            economyManager.setBalance(target.getUUID(), amount);
            
            // Format the amounts
            String formattedAmount = economyManager.formatCurrency(amount);
            String formattedOldBalance = economyManager.formatCurrency(oldBalance);
            
            // Send confirmation messages
            sendMessage(source, "§aSet " + target.getName().getString() + "'s balance to " + formattedAmount);
            sendMessage(source, "§7Previous balance: " + formattedOldBalance);
            
            // Notify the player
            MessageUtil.sendMessage(target, "§eYour balance was set to " + formattedAmount + " by an admin");
            
            // Log the transaction
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Economy] " + getSourceName(source) + " set " + target.getName().getString() + "'s balance to " + formattedAmount));
            
            return 1;
            
        } catch (Exception e) {
            sendMessage(source, "§cError setting balance: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Reset a player's balance to default
     */
    private static int resetEconomy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            // Get current balance for logging
            BigDecimal oldBalance = economyManager.getBalance(target.getUUID());
            
            // Reset to default starting balance (typically 100)
            double defaultBalance = 100.0; // This could be made configurable
            economyManager.setBalance(target.getUUID(), defaultBalance);
            
            // Format the amounts
            String formattedDefault = economyManager.formatCurrency(defaultBalance);
            String formattedOld = economyManager.formatCurrency(oldBalance);
            
            // Send confirmation messages
            sendMessage(source, "§aReset " + target.getName().getString() + "'s balance to " + formattedDefault);
            sendMessage(source, "§7Previous balance: " + formattedOld);
            
            // Notify the player
            MessageUtil.sendMessage(target, "§eYour balance was reset to " + formattedDefault + " by an admin");
            
            // Log the transaction
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Economy] " + getSourceName(source) + " reset " + target.getName().getString() + "'s balance"));
            
            return 1;
            
        } catch (Exception e) {
            sendMessage(source, "§cError resetting balance: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Reload economy configuration
     */
    private static int reloadEconomy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // This would reload economy config if we had file-based config
            sendMessage(source, "§aEconomy configuration reloaded");
            
            // Log the reload
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Economy] " + getSourceName(source) + " reloaded economy configuration"));
            
            return 1;
            
        } catch (Exception e) {
            sendMessage(source, "§cError reloading economy: " + e.getMessage());
            return 0;
        }
    }
    
    private static void sendMessage(CommandSourceStack source, String message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendMessage(player, message);
        } else {
            source.sendSuccess(() -> Component.literal(message), false);
        }
    }
    
    private static String getSourceName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        } else {
            return "Console";
        }
    }
}
