package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
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
        // EconomyModifierManager: players with bypass permission skip the cooldown
        boolean bypassCooldown = com.zerog.neoessentials.economy.compat.EconomyModifierManager
            .getInstance().hasNoPayCooldown(sender.getUUID());
        if (!bypassCooldown) {
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
        } else {
            // Always update timestamp even for bypass players (for record-keeping)
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
        
        // Find recipient player — support offline if sender has neoessentials.economy.pay.offline
        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
        UUID recipientUUID = null;
        String resolvedRecipientName = targetName;

        // Try online first
        ServerPlayer onlineRecipient = server.getPlayerList().getPlayerByName(targetName);
        if (onlineRecipient != null) {
            recipientUUID = onlineRecipient.getUUID();
            resolvedRecipientName = onlineRecipient.getName().getString();
        } else {
            // Offline player — check permission (Essentials: essentials.pay.offline)
            boolean canPayOffline = com.zerog.neoessentials.api.permissions.PermissionAPI
                .hasPermission(sender.getUUID(), "neoessentials.economy.pay.offline");
            if (!canPayOffline) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.offline_not_allowed"));
                return 0;
            }
            // Resolve from profile cache
            Optional<UUID> uuidOpt = com.zerog.neoessentials.economy.EconomyPlayerUtil
                .getUUIDByName(server, targetName);
            if (uuidOpt.isEmpty()) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.player_not_found", targetName));
                return 0;
            }
            recipientUUID = uuidOpt.get();
        }

        final UUID finalRecipientUUID = recipientUUID;
        final String finalRecipientName = resolvedRecipientName;

        // Prevent self-payment
        if (finalRecipientUUID.equals(sender.getUUID())) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.cannot_pay_self"));
            return 0;
        }

        // Check if recipient allows payments (Essentials: !player.isAcceptingPay())
        if (!com.zerog.neoessentials.economy.managers.PayToggleManager.getInstance()
                .getPayToggle(finalRecipientUUID)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.toggled_off"));
            return 0;
        }

        // Ignore check — if online recipient ignores sender, block payment (Essentials: payExcludesIgnoreList)
        if (onlineRecipient != null
                && com.zerog.neoessentials.chat.IgnoreManager.isIgnoring(onlineRecipient, sender)) {
            ctx.getSource().sendFailure(MessageUtil.error(
                "commands.neoessentials.pay.toggled_off")); // same message as Essentials "notAcceptingPay"
            return 0;
        }

        // Use validated amount
        java.math.BigDecimal amount = amountValidation.getValue(java.math.BigDecimal.class);

        // Enforce max transfer amount — use per-player limit from EconomyModifierManager
        // (supports LuckPerms meta and permission tiers)
        BigDecimal perPlayerPayLimit = com.zerog.neoessentials.economy.compat.EconomyModifierManager
            .getInstance().getPayLimit(sender.getUUID());
        double maxTransfer = perPlayerPayLimit != null ? perPlayerPayLimit.doubleValue()
            : com.zerog.neoessentials.config.ConfigManager.getMaxTransferAmount();
        boolean bypassMaxTransfer = com.zerog.neoessentials.api.permissions.PermissionAPI
            .hasPermission(sender.getUUID(), "neoessentials.economy.pay.bypass.limit");
        if (!bypassMaxTransfer && amount.doubleValue() > maxTransfer) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.exceeds_limit",
                maxTransfer, EconomyManager.getInstance().getCurrencySymbol()));
            return 0;
        }

        // Calculate tax using effective per-player rate (respects tax-exempt and LuckPerms meta)
        double taxPercent = com.zerog.neoessentials.economy.compat.EconomyModifierManager
            .getInstance().getEffectiveTaxRate(sender.getUUID());
        java.math.BigDecimal fee = amount.multiply(java.math.BigDecimal.valueOf(taxPercent / 100.0));
        java.math.BigDecimal netAmount = amount.subtract(fee);

        boolean success = com.zerog.neoessentials.api.EconomyAPI.payPlayer(
            sender.getUUID(), finalRecipientUUID, amount, taxPercent);
        if (!success) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.insufficient_funds"));
            return 0;
        }

        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.pay.success_fee",
            finalRecipientName, amount, fee, netAmount, currency), false);

        // Notify recipient if online
        if (onlineRecipient != null) {
            onlineRecipient.sendSystemMessage(MessageUtil.info(
                "commands.neoessentials.pay.received_fee",
                sender.getGameProfile().getName(), netAmount, fee, currency));
        }

        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance()
            .addTransaction(sender.getUUID(), MessageUtil.localize(
                "commands.neoessentials.transaction.paid", finalRecipientName, amount, fee));
        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance()
            .addTransaction(finalRecipientUUID, MessageUtil.localize(
                "commands.neoessentials.transaction.received", netAmount,
                sender.getGameProfile().getName(), fee));

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new com.zerog.neoessentials.economy.events.EconomyTransactionEvent(
                com.zerog.neoessentials.economy.events.EconomyTransactionEvent.Type.PAY,
                sender.getUUID(), finalRecipientUUID, netAmount,
                MessageUtil.localize("commands.neoessentials.transaction.pay_description", fee)));

        BaltopCommand.invalidateCache();
        return 1;
    }
}
