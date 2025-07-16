package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
<<<<<<< HEAD
                .then(Commands.literal("status")
                    .executes(EconomyCommands::showEconomyStatus)
                )
                .then(Commands.literal("diagnostics")
                    .executes(EconomyCommands::runEconomyDiagnostics)
                )
                .then(Commands.literal("enable")
                    .executes(EconomyCommands::forceEnableEconomy)
                )
                .then(Commands.literal("disable")
                    .executes(EconomyCommands::forceDisableEconomy)
                )
                .then(Commands.literal("reload")
                    .executes(EconomyCommands::reloadEconomy)
                )
                .then(Commands.literal("validate")
                    .executes(EconomyCommands::validateEconomySystem)
                )
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
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
    }
    
    private static int showBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!validatePlayerEconomyAccess(player, economyManager)) {
            sendErrorMessage(context.getSource(), "Cannot access economy system.");
            return 0;
        }
        
        EconomyAccount account = economyManager.getOrCreateAccount(player.getUUID(), player.getName().getString());
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        BigDecimal balance = account.getBalance(defaultCurrency);
        
        sendInfoMessage(context.getSource(), "Your balance: " + defaultCurrency.format(balance));
        
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
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (!economyManager.isEnabled()) {
            context.getSource().sendFailure(Component.literal("Economy system is disabled."));
            return 0;
        }
        
        // This will be implemented when we create the GUI system
        context.getSource().sendSuccess(() -> Component.literal("Economy menu GUI coming soon!"), false);
        
        return 1;
    }
    
    /**
<<<<<<< HEAD
     * Shows economy system status
     */
    private static int showEconomyStatus(CommandContext<CommandSourceStack> context) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        try {
            sendInfoMessage(context.getSource(), "Economy System Status:");
            sendInfoMessage(context.getSource(), "Enabled: " + (economyManager != null ? "§aYes" : "§cNo"));
            sendInfoMessage(context.getSource(), "Total Accounts: " + (economyManager != null ? economyManager.getTotalAccounts() : 0));
            sendInfoMessage(context.getSource(), "Storage Type: " + (economyManager != null ? economyManager.getStorageType() : "Unknown"));
            return 1;
        } catch (Exception e) {
            sendErrorMessage(context.getSource(), "Error getting economy status: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Runs comprehensive economy diagnostics
     */
    private static int runEconomyDiagnostics(CommandContext<CommandSourceStack> context) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (economyManager == null) {
            sendErrorMessage(context.getSource(), "Economy manager is not available");
            return 0;
        }
        
        try {
            sendInfoMessage(context.getSource(), "Running comprehensive economy diagnostics...");
            
            List<String> diagnostics = economyManager.performSystemDiagnostics();
            
            for (String diagnostic : diagnostics) {
                sendInfoMessage(context.getSource(), diagnostic);
            }
            
            sendSuccessMessage(context.getSource(), "Diagnostics completed successfully");
            return 1;
            
        } catch (Exception e) {
            sendErrorMessage(context.getSource(), "Error running diagnostics: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Force enables the economy system
     */
    private static int forceEnableEconomy(CommandContext<CommandSourceStack> context) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (economyManager == null) {
            sendErrorMessage(context.getSource(), "Economy manager is not available");
            return 0;
        }
        
        try {
            boolean success = economyManager.forceEnable();
            if (success) {
                sendSuccessMessage(context.getSource(), "Economy system force enabled");
                return 1;
            } else {
                sendErrorMessage(context.getSource(), "Failed to force enable economy system");
                return 0;
            }
        } catch (Exception e) {
            sendErrorMessage(context.getSource(), "Error force enabling economy: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Force disables the economy system
     */
    private static int forceDisableEconomy(CommandContext<CommandSourceStack> context) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (economyManager == null) {
            sendErrorMessage(context.getSource(), "Economy manager is not available");
            return 0;
        }
        
        try {
            boolean success = economyManager.forceDisable();
            if (success) {
                sendSuccessMessage(context.getSource(), "Economy system force disabled");
                return 1;
            } else {
                sendErrorMessage(context.getSource(), "Failed to force disable economy system");
                return 0;
            }
        } catch (Exception e) {
            sendErrorMessage(context.getSource(), "Error force disabling economy: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Reloads the economy system
     */
    private static int reloadEconomy(CommandContext<CommandSourceStack> context) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (economyManager == null) {
            sendErrorMessage(context.getSource(), "Economy manager is not available");
            return 0;
        }
        
        try {
            sendInfoMessage(context.getSource(), "Reloading economy system...");
            boolean success = economyManager.reload();
            
            if (success) {
                sendSuccessMessage(context.getSource(), "Economy system reloaded successfully");
                return 1;
            } else {
                sendErrorMessage(context.getSource(), "Failed to reload economy system");
                return 0;
            }
        } catch (Exception e) {
            sendErrorMessage(context.getSource(), "Error reloading economy: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Validates the economy system integrity
     */
    private static int validateEconomySystem(CommandContext<CommandSourceStack> context) {
        EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
        
        if (economyManager == null) {
            sendErrorMessage(context.getSource(), "Economy manager is not available");
            return 0;
        }
        
        try {
            sendInfoMessage(context.getSource(), "Validating economy system...");
            
            boolean isValid = economyManager.validateStorageFunctionality();
            
            if (isValid) {
                sendSuccessMessage(context.getSource(), "Economy system validation passed");
                return 1;
            } else {
                sendErrorMessage(context.getSource(), "Economy system validation failed");
                return 0;
            }
        } catch (Exception e) {
            sendErrorMessage(context.getSource(), "Error validating economy: " + e.getMessage());
            return 0;
        }
    }
    
    /**
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     * Validates that a player can perform economy operations
     */
    private static boolean validatePlayerEconomyAccess(ServerPlayer player, EconomyManager economyManager) {
        if (player == null) {
            return false;
        }
        
        if (!economyManager.isEnabled()) {
            return false;
        }
        
        return economyManager.validatePlayerAccount(player.getUUID());
    }
    
    /**
     * Sends a formatted error message to the player
     */
    private static void sendErrorMessage(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("§c[Economy] " + message));
    }
    
    /**
     * Sends a formatted success message to the player
     */
    private static void sendSuccessMessage(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("§a[Economy] " + message), false);
    }
    
    /**
     * Sends a formatted info message to the player
     */
    private static void sendInfoMessage(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("§b[Economy] " + message), false);
    }
}
