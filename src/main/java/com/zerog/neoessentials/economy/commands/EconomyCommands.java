package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.Currency;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.List;

/**
 * Economy commands for players and administrators
 */
public class EconomyCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("balance")
                .executes(EconomyCommands::showBalance)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .executes(EconomyCommands::showOtherBalance)
                )
        );
        
        dispatcher.register(
            Commands.literal("bal")
                .executes(EconomyCommands::showBalance)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .executes(EconomyCommands::showOtherBalance)
                )
        );
        
        dispatcher.register(
            Commands.literal("money")
                .executes(EconomyCommands::showBalance)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .executes(EconomyCommands::showOtherBalance)
                )
        );
        
        dispatcher.register(
            Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(EconomyCommands::payPlayer)
                    )
                )
        );
        
        dispatcher.register(
            Commands.literal("baltop")
                .executes(EconomyCommands::showTopBalances)
        );
        
        dispatcher.register(
            Commands.literal("balancetop")
                .executes(EconomyCommands::showTopBalances)
        );
        
        // Admin commands
        dispatcher.register(
            Commands.literal("eco")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(EconomyCommands::giveMoneyToPlayer)
                        )
                    )
                )
                .then(Commands.literal("take")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(EconomyCommands::takeMoneyFromPlayer)
                        )
                    )
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                            .executes(EconomyCommands::setPlayerBalance)
                        )
                    )
                )
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(EconomyCommands::resetPlayerBalance)
                    )
                )
        );
        
        // Economy main menu command
        dispatcher.register(
            Commands.literal("economymenu")
                .executes(EconomyCommands::openEconomyMenu)
        );
        
        dispatcher.register(
            Commands.literal("emenu")
                .executes(EconomyCommands::openEconomyMenu)
        );
        
        // Register shop commands
        ShopCommands.register(dispatcher);
    }
    
    private static int showBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        EconomyAccount account = economyManager.getOrCreateAccount(player.getUUID(), player.getName().getString());
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal balance = account.getBalance(defaultCurrency);
        
        context.getSource().sendSuccess(() -> 
            Component.literal("Your balance: " + defaultCurrency.format(balance)), false);
        
        return 1;
    }
    
    private static int showOtherBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        EconomyAccount account = economyManager.getOrCreateAccount(targetPlayer.getUUID(), targetPlayer.getName().getString());
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal balance = account.getBalance(defaultCurrency);
        
        context.getSource().sendSuccess(() -> 
            Component.literal(targetPlayer.getName().getString() + "'s balance: " + defaultCurrency.format(balance)), false);
        
        return 1;
    }
    
    private static int payPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        if (sourcePlayer.equals(targetPlayer)) {
            context.getSource().sendFailure(Component.literal("You cannot pay yourself."));
            return 0;
        }
        
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal payAmount = BigDecimal.valueOf(amount);
        
        boolean success = economyManager.transferMoney(
            sourcePlayer.getUUID(),
            targetPlayer.getUUID(),
            payAmount,
            defaultCurrency,
            "Payment from " + sourcePlayer.getName().getString() + " to " + targetPlayer.getName().getString()
        );
        
        if (success) {
            context.getSource().sendSuccess(() -> 
                Component.literal("Successfully paid " + defaultCurrency.format(payAmount) + " to " + targetPlayer.getName().getString()), false);
            
            targetPlayer.sendSystemMessage(Component.literal("You received " + defaultCurrency.format(payAmount) + " from " + sourcePlayer.getName().getString()));
        } else {
            context.getSource().sendFailure(Component.literal("Payment failed. Insufficient funds or other error."));
        }
        
        return success ? 1 : 0;
    }
    
    private static int showTopBalances(CommandContext<CommandSourceStack> context) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        List<EconomyAccount> topAccounts = economyManager.getTopAccounts(10);
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        
        context.getSource().sendSuccess(() -> Component.literal("§6§l=== Top Balances ==="), false);
        
        for (int i = 0; i < topAccounts.size(); i++) {
            EconomyAccount account = topAccounts.get(i);
            BigDecimal balance = account.getBalance(defaultCurrency);
            int rank = i + 1;
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§e" + rank + ". §f" + account.getPlayerName() + " §7- §a" + defaultCurrency.format(balance)), false);
        }
        
        return 1;
    }
    
    private static int giveMoneyToPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal giveAmount = BigDecimal.valueOf(amount);
        
        boolean success = economyManager.addMoney(targetPlayer.getUUID(), giveAmount, defaultCurrency, "Admin gave money");
        
        if (success) {
            context.getSource().sendSuccess(() -> 
                Component.literal("Successfully gave " + defaultCurrency.format(giveAmount) + " to " + targetPlayer.getName().getString()), false);
            
            targetPlayer.sendSystemMessage(Component.literal("You received " + defaultCurrency.format(giveAmount) + " from an administrator"));
        } else {
            context.getSource().sendFailure(Component.literal("Failed to give money to player."));
        }
        
        return success ? 1 : 0;
    }
    
    private static int takeMoneyFromPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal takeAmount = BigDecimal.valueOf(amount);
        
        boolean success = economyManager.subtractMoney(targetPlayer.getUUID(), takeAmount, defaultCurrency, "Admin took money");
        
        if (success) {
            context.getSource().sendSuccess(() -> 
                Component.literal("Successfully took " + defaultCurrency.format(takeAmount) + " from " + targetPlayer.getName().getString()), false);
            
            targetPlayer.sendSystemMessage(Component.literal("An administrator took " + defaultCurrency.format(takeAmount) + " from your account"));
        } else {
            context.getSource().sendFailure(Component.literal("Failed to take money from player. Insufficient funds?"));
        }
        
        return success ? 1 : 0;
    }
    
    private static int setPlayerBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal setAmount = BigDecimal.valueOf(amount);
        
        boolean success = economyManager.setBalance(targetPlayer.getUUID(), setAmount, defaultCurrency, "Admin set balance");
        
        if (success) {
            context.getSource().sendSuccess(() -> 
                Component.literal("Successfully set " + targetPlayer.getName().getString() + "'s balance to " + defaultCurrency.format(setAmount)), false);
            
            targetPlayer.sendSystemMessage(Component.literal("Your balance has been set to " + defaultCurrency.format(setAmount) + " by an administrator"));
        } else {
            context.getSource().sendFailure(Component.literal("Failed to set player balance."));
        }
        
        return success ? 1 : 0;
    }
    
    private static int resetPlayerBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal startingBalance = economyManager.getConfig().getStartingBalance();
        
        boolean success = economyManager.setBalance(targetPlayer.getUUID(), startingBalance, defaultCurrency, "Admin reset balance");
        
        if (success) {
            context.getSource().sendSuccess(() -> 
                Component.literal("Successfully reset " + targetPlayer.getName().getString() + "'s balance to " + defaultCurrency.format(startingBalance)), false);
            
            targetPlayer.sendSystemMessage(Component.literal("Your balance has been reset to " + defaultCurrency.format(startingBalance) + " by an administrator"));
        } else {
            context.getSource().sendFailure(Component.literal("Failed to reset player balance."));
        }
        
        return success ? 1 : 0;
    }
    
    private static int openEconomyMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        // This will be implemented when we create the GUI system
        context.getSource().sendSuccess(() -> Component.literal("Economy menu GUI coming soon!"), false);
        
        return 1;
    }
}
