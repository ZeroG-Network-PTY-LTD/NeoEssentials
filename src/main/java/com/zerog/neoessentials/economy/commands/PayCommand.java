package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.managers.EconomyManager;

import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.economy.managers.PayToggleManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PayCommand {
    private static final Map<UUID, Long> payCooldowns = new ConcurrentHashMap<>();
    private static long getPayCooldownMs() {
        return com.zerog.neoessentials.config.ConfigManager.getPayCooldownSeconds() * 1000L;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register main command
        dispatcher.register(
            net.minecraft.commands.Commands.literal("pay")
                .requires(src -> src.hasPermission(2) || // Allow ops
                    (src.getPlayer() != null && com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.economy.pay")))
                .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> execute(ctx))))
        );
        
        // Register alias
        dispatcher.register(
            net.minecraft.commands.Commands.literal("p")
                .requires(src -> src.hasPermission(2) || // Allow ops
                    (src.getPlayer() != null && com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.economy.pay")))
                .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> execute(ctx))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        // Validate permission first
        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult = 
            com.zerog.neoessentials.util.PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.economy.pay");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
        ServerPlayer sender = permResult.getPlayer();
        
        // Check cooldown atomically to prevent bypass
        long now = System.currentTimeMillis();
        long cooldownMs = getPayCooldownMs();
        Long lastPay = payCooldowns.putIfAbsent(sender.getUUID(), now);
        if (lastPay != null) {
            long timeSince = now - lastPay;
            if (timeSince < cooldownMs) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.cooldown"));
                return 0;
            }
            // Update cooldown time
            payCooldowns.put(sender.getUUID(), now);
        }
        
        // Check if economy is enabled
        if (!EconomyManager.getInstance().isEnabled()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.economy.disabled"));
            return 0;
        }
        
        // Validate input parameters
        String targetName = StringArgumentType.getString(ctx, "player");
        double amountRaw = DoubleArgumentType.getDouble(ctx, "amount");
        
        // Validate player name
        com.zerog.neoessentials.util.InputValidator.ValidationResult nameValidation = 
            com.zerog.neoessentials.util.InputValidator.validatePlayerName(targetName);
        if (!nameValidation.isValid()) {
            ctx.getSource().sendFailure(MessageUtil.error(nameValidation.getErrorMessage()));
            return 0;
        }
        
        // Validate amount
        com.zerog.neoessentials.util.InputValidator.ValidationResult amountValidation = 
            com.zerog.neoessentials.util.InputValidator.validateEconomyAmount(amountRaw);
        if (!amountValidation.isValid()) {
            ctx.getSource().sendFailure(MessageUtil.error(amountValidation.getErrorMessage()));
            return 0;
        }
        
        // Find recipient player
        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
        com.zerog.neoessentials.util.InputValidator.ValidationResult playerValidation = 
            com.zerog.neoessentials.util.InputValidator.validateOnlinePlayer(targetName, server);
        if (!playerValidation.isValid()) {
            ctx.getSource().sendFailure(MessageUtil.error(playerValidation.getErrorMessage()));
            return 0;
        }
        
        ServerPlayer recipient = playerValidation.getValue(ServerPlayer.class);
        
        // Prevent self-payment
        if (recipient.getUUID().equals(sender.getUUID())) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.cannot_pay_self"));
            return 0;
        }
        // Check if recipient allows payments
        if (!PayToggleManager.getInstance().getPayToggle(recipient.getUUID())) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.toggled_off"));
            return 0;
        }
        
        // Use validated amount
        java.math.BigDecimal amount = amountValidation.getValue(java.math.BigDecimal.class);
        
        // Calculate tax using ConfigManager
        double taxPercent = com.zerog.neoessentials.config.ConfigManager.getEconomyTaxPercentage();
        java.math.BigDecimal fee = amount.multiply(java.math.BigDecimal.valueOf(taxPercent / 100.0));
        java.math.BigDecimal netAmount = amount.subtract(fee);
        boolean success = com.zerog.neoessentials.api.EconomyAPI.payPlayer(sender.getUUID(), recipient.getUUID(), amount);
        if (!success) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.insufficient_funds"));
            return 0;
        }
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.pay.success_fee",
            targetName, amount, fee, netAmount, currency
        ), false);
        recipient.sendSystemMessage(MessageUtil.info(
            "commands.neoessentials.pay.received_fee",
            sender.getGameProfile().getName(), netAmount, fee, currency
        ));
        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance().addTransaction(sender.getUUID(), MessageUtil.localize("commands.neoessentials.transaction.paid", targetName, amount, fee));
        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance().addTransaction(recipient.getUUID(), MessageUtil.localize("commands.neoessentials.transaction.received", netAmount, sender.getGameProfile().getName(), fee));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.zerog.neoessentials.economy.events.EconomyTransactionEvent(
            com.zerog.neoessentials.economy.events.EconomyTransactionEvent.Type.PAY,
            sender.getUUID(),
            recipient.getUUID(),
            netAmount,
            MessageUtil.localize("commands.neoessentials.transaction.pay_description", fee)
        ));
        return 1;
    }
}
