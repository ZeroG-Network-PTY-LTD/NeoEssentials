package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.UserManager;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.CurrencyManager;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles payment preference commands and payment confirmation system.
 * Provides payment toggles and confirmation features missing from the economy system.
 */
public class PaymentCommands {
    
    private final UserManager userManager;
    private final EconomyManager economyManager;
    private final CurrencyManager currencyManager;
    
    // Temporary storage for pending payments awaiting confirmation
    private final Map<UUID, PendingPayment> pendingPayments = new HashMap<>();
    
    // Payment confirmation timeout (30 seconds)
    private static final long CONFIRMATION_TIMEOUT = 30000;
    
    public PaymentCommands() {
        this.userManager = NeoEssentials.getInstance().getDataManager().getUserManager();
        this.economyManager = EconomyManager.getInstance();
        this.currencyManager = economyManager.getCurrencyManager();
    }
    
    /**
     * Register payment-related commands
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Payment toggle command
        dispatcher.register(
            Commands.literal("paytoggle")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.paytoggle"))
                .executes(context -> togglePayments(context, null, null))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> togglePayments(context, 
                        BoolArgumentType.getBool(context, "enabled"), null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.paytoggle.others"))
                        .executes(context -> togglePayments(context,
                            BoolArgumentType.getBool(context, "enabled"),
                            EntityArgument.getPlayer(context, "player")))))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.paytoggle.others"))
                    .executes(context -> togglePayments(context, null,
                        EntityArgument.getPlayer(context, "player"))))
        );
        
        // Payment confirmation toggle command
        dispatcher.register(
            Commands.literal("payconfirm")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.payconfirm"))
                .executes(context -> togglePaymentConfirmation(context, null, null))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> togglePaymentConfirmation(context,
                        BoolArgumentType.getBool(context, "enabled"), null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.payconfirm.others"))
                        .executes(context -> togglePaymentConfirmation(context,
                            BoolArgumentType.getBool(context, "enabled"),
                            EntityArgument.getPlayer(context, "player")))))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.payconfirm.others"))
                    .executes(context -> togglePaymentConfirmation(context, null,
                        EntityArgument.getPlayer(context, "player"))))
        );
        
        // Payment confirmation commands
        dispatcher.register(
            Commands.literal("payyes")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.pay"))
                .executes(context -> confirmPayment(context))
        );
        
        dispatcher.register(
            Commands.literal("payno")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.pay"))
                .executes(context -> cancelPayment(context))
        );
        
        // Enhanced pay command with confirmation support
        dispatcher.register(
            Commands.literal("pay")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.pay"))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(context -> initiatePayment(context,
                            EntityArgument.getPlayer(context, "player"),
                            DoubleArgumentType.getDouble(context, "amount")))))
        );
    }
    
    /**
     * Toggle payment acceptance for a player
     */
    private int togglePayments(CommandContext<CommandSourceStack> context, Boolean enabled, ServerPlayer targetPlayer) 
            throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        ServerPlayer target = targetPlayer != null ? targetPlayer : executor;
        
        boolean currentState = userManager.getUserDataBoolean(target.getUUID(), "payments_enabled", true);
        boolean newState = enabled != null ? enabled : !currentState;
        
        userManager.setUserData(target.getUUID(), "payments_enabled", newState);
        
        if (target == executor) {
            if (newState) {
                LanguageUtil.sendMessage(executor, "§aPayment reception enabled! You can now receive payments from other players.");
            } else {
                LanguageUtil.sendMessage(executor, "§cPayment reception disabled! You will not receive payments from other players.");
            }
        } else {
            if (newState) {
                LanguageUtil.sendMessage(executor, "§aPayment reception enabled for " + target.getDisplayName().getString() + ".");
                LanguageUtil.sendMessage(target, "§aPayment reception has been enabled by an administrator.");
            } else {
                LanguageUtil.sendMessage(executor, "§cPayment reception disabled for " + target.getDisplayName().getString() + ".");
                LanguageUtil.sendMessage(target, "§cPayment reception has been disabled by an administrator.");
            }
        }
        
        return 1;
    }
    
    /**
     * Toggle payment confirmation requirement for a player
     */
    private int togglePaymentConfirmation(CommandContext<CommandSourceStack> context, Boolean enabled, ServerPlayer targetPlayer) 
            throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        ServerPlayer target = targetPlayer != null ? targetPlayer : executor;
        
        boolean currentState = userManager.getUserDataBoolean(target.getUUID(), "payment_confirmation", false);
        boolean newState = enabled != null ? enabled : !currentState;
        
        userManager.setUserData(target.getUUID(), "payment_confirmation", newState);
        
        if (target == executor) {
            if (newState) {
                LanguageUtil.sendMessage(executor, "§aPayment confirmation enabled! You will be asked to confirm large payments.");
                LanguageUtil.sendMessage(executor, "§7Use §e/payyes§7 or §e/payno§7 to confirm or cancel payments.");
            } else {
                LanguageUtil.sendMessage(executor, "§cPayment confirmation disabled! Payments will be sent immediately.");
            }
        } else {
            if (newState) {
                LanguageUtil.sendMessage(executor, "§aPayment confirmation enabled for " + target.getDisplayName().getString() + ".");
                LanguageUtil.sendMessage(target, "§aPayment confirmation has been enabled by an administrator.");
            } else {
                LanguageUtil.sendMessage(executor, "§cPayment confirmation disabled for " + target.getDisplayName().getString() + ".");
                LanguageUtil.sendMessage(target, "§cPayment confirmation has been disabled by an administrator.");
            }
        }
        
        return 1;
    }
    
    /**
     * Initiate a payment with optional confirmation
     */
    private int initiatePayment(CommandContext<CommandSourceStack> context, ServerPlayer recipient, double amount) 
            throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        Currency defaultCurrency = currencyManager.getDefaultCurrency();
        
        // Check if recipient accepts payments
        boolean recipientAcceptsPayments = userManager.getUserDataBoolean(recipient.getUUID(), "payments_enabled", true);
        if (!recipientAcceptsPayments) {
            LanguageUtil.sendErrorMessage(sender, "§c" + recipient.getDisplayName().getString() + " is not accepting payments.");
            return 0;
        }
        
        // Check if sender has sufficient funds
        double senderBalance = economyManager.getBalance(sender.getUUID(), defaultCurrency);
        if (senderBalance < amount) {
            LanguageUtil.sendErrorMessage(sender, "§cInsufficient funds! You need " + 
                defaultCurrency.format(amount) + " but only have " + 
                defaultCurrency.format(senderBalance) + ".");
            return 0;
        }
        
        // Check if confirmation is required
        boolean requiresConfirmation = userManager.getUserDataBoolean(sender.getUUID(), "payment_confirmation", false);
        double confirmationThreshold = userManager.getUserDataNumber(sender.getUUID(), "confirmation_threshold", 1000.0);
        
        if (requiresConfirmation && amount >= confirmationThreshold) {
            // Store pending payment and ask for confirmation
            pendingPayments.put(sender.getUUID(), new PendingPayment(
                sender.getUUID(), recipient.getUUID(), amount, defaultCurrency, System.currentTimeMillis()
            ));
            
            LanguageUtil.sendMessage(sender, "§6⚠ Payment Confirmation Required");
            LanguageUtil.sendMessage(sender, "§7You are about to send §a" + defaultCurrency.format(amount) + 
                "§7 to §e" + recipient.getDisplayName().getString() + "§7.");
            
            // Create clickable confirmation buttons
            Component confirmButton = Component.literal("§a[CONFIRM]")
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/payyes"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Component.literal("§aClick to confirm payment"))));
            
            Component cancelButton = Component.literal("§c[CANCEL]")
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/payno"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Component.literal("§cClick to cancel payment"))));
            
            sender.sendSystemMessage(Component.empty()
                .append(confirmButton)
                .append(Component.literal(" "))
                .append(cancelButton));
            
            LanguageUtil.sendMessage(sender, "§7Or type §e/payyes§7 to confirm or §e/payno§7 to cancel.");
            LanguageUtil.sendMessage(sender, "§7This confirmation will expire in 30 seconds.");
            
            return 1;
        } else {
            // Process payment immediately
            return processPayment(sender, recipient, amount, defaultCurrency);
        }
    }
    
    /**
     * Confirm a pending payment
     */
    private int confirmPayment(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        PendingPayment pending = pendingPayments.remove(sender.getUUID());
        
        if (pending == null) {
            LanguageUtil.sendErrorMessage(sender, "§cNo pending payment to confirm.");
            return 0;
        }
        
        // Check if payment has expired
        if (System.currentTimeMillis() - pending.timestamp > CONFIRMATION_TIMEOUT) {
            LanguageUtil.sendErrorMessage(sender, "§cPayment confirmation expired. Please try again.");
            return 0;
        }
        
        // Find recipient (they might have logged off)
        var server = sender.getServer();
        if (server == null) {
            LanguageUtil.sendErrorMessage(sender, "§cServer unavailable. Payment cancelled.");
            return 0;
        }
        
        ServerPlayer recipient = server.getPlayerList().getPlayer(pending.recipientId);
        if (recipient == null) {
            LanguageUtil.sendErrorMessage(sender, "§cRecipient is no longer online. Payment cancelled.");
            return 0;
        }
        
        return processPayment(sender, recipient, pending.amount, pending.currency);
    }
    
    /**
     * Cancel a pending payment
     */
    private int cancelPayment(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        PendingPayment pending = pendingPayments.remove(sender.getUUID());
        
        if (pending == null) {
            LanguageUtil.sendErrorMessage(sender, "§cNo pending payment to cancel.");
            return 0;
        }
        
        LanguageUtil.sendMessage(sender, "§ePayment cancelled.");
        return 1;
    }
    
    /**
     * Process the actual payment
     */
    private int processPayment(ServerPlayer sender, ServerPlayer recipient, double amount, Currency currency) {
        try {
            // Perform the transaction
            boolean success = economyManager.transferMoney(sender.getUUID(), recipient.getUUID(), 
                amount, currency, "Payment via /pay command");
            
            if (success) {
                LanguageUtil.sendMessage(sender, "§aSent " + currency.format(amount) + " to " + 
                    recipient.getDisplayName().getString() + ".");
                LanguageUtil.sendMessage(recipient, "§aReceived " + currency.format(amount) + " from " + 
                    sender.getDisplayName().getString() + ".");
                
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(sender, "§cPayment failed. Please try again.");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(sender, "§cAn error occurred while processing the payment.");
            return 0;
        }
    }
    
    /**
     * Check if a player accepts payments
     */
    public boolean acceptsPayments(UUID playerId) {
        return userManager.getUserDataBoolean(playerId, "payments_enabled", true);
    }
    
    /**
     * Check if a player requires payment confirmation
     */
    public boolean requiresPaymentConfirmation(UUID playerId) {
        return userManager.getUserDataBoolean(playerId, "payment_confirmation", false);
    }
    
    /**
     * Get the payment confirmation threshold for a player
     */
    public double getConfirmationThreshold(UUID playerId) {
        return userManager.getUserDataNumber(playerId, "confirmation_threshold", 1000.0);
    }
    
    /**
     * Set the payment confirmation threshold for a player
     */
    public void setConfirmationThreshold(UUID playerId, double threshold) {
        userManager.setUserData(playerId, "confirmation_threshold", threshold);
    }
    
    /**
     * Data class for storing pending payments
     */
    private static class PendingPayment {
        final UUID recipientId;
        final double amount;
        final Currency currency;
        final long timestamp;
        
        PendingPayment(UUID senderId, UUID recipientId, double amount, Currency currency, long timestamp) {
            this.recipientId = recipientId;
            this.amount = amount;
            this.currency = currency;
            this.timestamp = timestamp;
        }
    }
}
