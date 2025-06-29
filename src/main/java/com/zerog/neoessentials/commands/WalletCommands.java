package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Commands for managing player wallets and cash.
 * These handle the "cash on hand" system separate from banking.
 */
public class WalletCommands {
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // Register /wallet command
        dispatcher.register(
            Commands.literal("wallet")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.wallet"))
                .executes(context -> showWalletInfo(context.getSource()))
                .then(Commands.literal("help")
                    .executes(context -> showWalletHelp(context.getSource())))
                .then(Commands.literal("info")
                    .executes(context -> showWalletInfo(context.getSource())))
                    
                // Admin commands for managing cash
                .then(Commands.literal("give")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.admin.wallet.give"))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> giveCash(context.getSource(),
                                EntityArgument.getPlayer(context, "player"),
                                DoubleArgumentType.getDouble(context, "amount"))))))
                                
                .then(Commands.literal("take")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.admin.wallet.take"))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> takeCash(context.getSource(),
                                EntityArgument.getPlayer(context, "player"),
                                DoubleArgumentType.getDouble(context, "amount"))))))
                                
                .then(Commands.literal("set")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.admin.wallet.set"))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                            .executes(context -> setCash(context.getSource(),
                                EntityArgument.getPlayer(context, "player"),
                                DoubleArgumentType.getDouble(context, "amount"))))))
        );
        
        // Register /cash as alias for /wallet info
        dispatcher.register(
            Commands.literal("cash")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.wallet"))
                .executes(context -> showWalletInfo(context.getSource()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.wallet.others"))
                    .executes(context -> showOtherWalletInfo(context.getSource(),
                        EntityArgument.getPlayer(context, "player"))))
        );
    }
    
    private int showWalletHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6§l--- Wallet System Help ---");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§eThe wallet system manages your §acash on hand§e:");
            MessageUtil.sendMessage(player, "§7• Cash is separate from bank accounts");
            MessageUtil.sendMessage(player, "§7• Used for payments, shopping, and auctions");
            MessageUtil.sendMessage(player, "§7• Can be deposited to/withdrawn from bank accounts");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lWallet Commands:");
            MessageUtil.sendMessage(player, "§e/wallet info §7- Show your cash balances");
            MessageUtil.sendMessage(player, "§e/cash §7- Quick cash balance check");
            MessageUtil.sendMessage(player, "§e/balance §7- Show both cash and bank balances");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lUsing Cash:");
            MessageUtil.sendMessage(player, "§e/pay <player> <amount> §7- Pay someone with cash");
            MessageUtil.sendMessage(player, "§e/bank deposit <amount> <account> §7- Deposit cash to bank");
            MessageUtil.sendMessage(player, "§e/bank withdraw <amount> <account> §7- Withdraw bank to cash");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§aYour starting cash balance is automatically provided!");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use wallet commands"));
            return 0;
        }
    }
    
    private int showWalletInfo(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = EconomyManager.getInstance();
            WalletManager walletManager = economyManager.getWalletManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            
            PlayerWallet wallet = walletManager.getWallet(player.getUUID());
            Map<Currency, Double> cashBalances = wallet.getAllCashBalances();
            
            MessageUtil.sendMessage(player, "§6§l--- Your Wallet ---");
            MessageUtil.sendMessage(player, "");
            
            if (cashBalances.isEmpty()) {
                MessageUtil.sendMessage(player, "§7Your wallet is empty!");
            } else {
                MessageUtil.sendMessage(player, "§eCash on Hand:");
                for (Map.Entry<Currency, Double> entry : cashBalances.entrySet()) {
                    Currency currency = entry.getKey();
                    double amount = entry.getValue();
                    String formattedAmount = String.format("%.2f", amount);
                    
                    MessageUtil.sendMessage(player, String.format("§7• %s%s %s", 
                        currency.getSymbol(), formattedAmount, currency.getPluralName()));
                }
                
                // Show total worth
                double totalWorth = wallet.getTotalCashWorth();
                Currency defaultCurrency = currencyManager.getDefaultCurrency();
                if (defaultCurrency != null && totalWorth > 0) {
                    String formattedTotal = String.format("%.2f", totalWorth);
                    MessageUtil.sendMessage(player, "");
                    MessageUtil.sendMessage(player, String.format("§eTotal Worth: %s%s %s", 
                        defaultCurrency.getSymbol(), formattedTotal, defaultCurrency.getPluralName()));
                }
            }
            
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Use §e/wallet help §7for more information");
            MessageUtil.sendMessage(player, "§7Use §e/balance §7to see bank accounts too");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use wallet commands"));
            return 0;
        } catch (Exception e) {
            try {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendErrorMessage(player, "Error checking wallet: " + e.getMessage());
            } catch (CommandSyntaxException ex) {
                source.sendFailure(Component.literal("§cError checking wallet: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private int showOtherWalletInfo(CommandSourceStack source, ServerPlayer target) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = EconomyManager.getInstance();
            WalletManager walletManager = economyManager.getWalletManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured");
                return 0;
            }
            
            double cashBalance = walletManager.getCashBalance(target.getUUID(), defaultCurrency);
            String formattedBalance = String.format("%.2f", cashBalance);
            
            MessageUtil.sendMessage(player, String.format("§e%s's Cash: %s%s %s", 
                target.getScoreboardName(), 
                defaultCurrency.getSymbol(), 
                formattedBalance, 
                defaultCurrency.getPluralName()));
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use wallet commands"));
            return 0;
        } catch (Exception e) {
            try {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendErrorMessage(player, "Error checking wallet: " + e.getMessage());
            } catch (CommandSyntaxException ex) {
                source.sendFailure(Component.literal("§cError checking wallet: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private int giveCash(CommandSourceStack source, ServerPlayer target, double amount) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            WalletManager walletManager = economyManager.getWalletManager();
            Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
            
            if (defaultCurrency == null) {
                source.sendFailure(Component.literal("§cNo default currency configured"));
                return 0;
            }
            
            if (walletManager.addCash(target.getUUID(), defaultCurrency, amount)) {
                String formattedAmount = String.format("%.2f", amount);
                source.sendSuccess(() -> Component.literal(String.format("§aGave %s%s cash to %s", 
                    defaultCurrency.getSymbol(), formattedAmount, target.getScoreboardName())), true);
                
                MessageUtil.sendMessage(target, String.format("§aYou received %s%s cash from an administrator", 
                    defaultCurrency.getSymbol(), formattedAmount));
                
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to give cash (player may have reached maximum balance)"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError giving cash: " + e.getMessage()));
            return 0;
        }
    }
    
    private int takeCash(CommandSourceStack source, ServerPlayer target, double amount) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            WalletManager walletManager = economyManager.getWalletManager();
            Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
            
            if (defaultCurrency == null) {
                source.sendFailure(Component.literal("§cNo default currency configured"));
                return 0;
            }
            
            if (walletManager.subtractCash(target.getUUID(), defaultCurrency, amount)) {
                String formattedAmount = String.format("%.2f", amount);
                source.sendSuccess(() -> Component.literal(String.format("§aRemoved %s%s cash from %s", 
                    defaultCurrency.getSymbol(), formattedAmount, target.getScoreboardName())), true);
                
                MessageUtil.sendMessage(target, String.format("§c%s%s cash was removed by an administrator", 
                    defaultCurrency.getSymbol(), formattedAmount));
                
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to remove cash (insufficient funds or negative balances not allowed)"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError removing cash: " + e.getMessage()));
            return 0;
        }
    }
    
    private int setCash(CommandSourceStack source, ServerPlayer target, double amount) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            WalletManager walletManager = economyManager.getWalletManager();
            Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
            
            if (defaultCurrency == null) {
                source.sendFailure(Component.literal("§cNo default currency configured"));
                return 0;
            }
            
            if (walletManager.setCashBalance(target.getUUID(), defaultCurrency, amount)) {
                String formattedAmount = String.format("%.2f", amount);
                source.sendSuccess(() -> Component.literal(String.format("§aSet %s's cash to %s%s", 
                    target.getScoreboardName(), defaultCurrency.getSymbol(), formattedAmount)), true);
                
                MessageUtil.sendMessage(target, String.format("§eYour cash balance was set to %s%s by an administrator", 
                    defaultCurrency.getSymbol(), formattedAmount));
                
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to set cash balance"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError setting cash: " + e.getMessage()));
            return 0;
        }
    }
}
