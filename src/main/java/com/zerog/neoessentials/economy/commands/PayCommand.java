package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PayCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayCommand.class);
    private static final Map<UUID, Long> payCooldowns = new ConcurrentHashMap<>();
    private static long getPayCooldownMs() {
        return com.zerog.neoessentials.config.ConfigManager.getPayCooldownSeconds() * 1000L;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register main command
        dispatcher.register(
            net.minecraft.commands.Commands.literal("pay")
                .requires(src -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(src, 2) || // Allow ops
                    (src.getPlayer() != null && com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.economy.pay")))
                .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().name()),
                        builder
                    ))
                    .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> execute(ctx))))
        );
        
        // Register alias
        dispatcher.register(
            net.minecraft.commands.Commands.literal("p")
                .requires(src -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(src, 2) || // Allow ops
                    (src.getPlayer() != null && com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.economy.pay")))
                .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().name()),
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
        
        // Check cooldown (read-only) — only actually consumed once the payment succeeds, see
        // below. EconomyModifierManager: players with bypass permission skip the cooldown
        // entirely (never checked, never recorded).
        boolean bypassCooldown = com.zerog.neoessentials.economy.compat.EconomyModifierManager
            .getInstance().hasNoPayCooldown(sender.getUUID());
        if (!bypassCooldown) {
            Long lastPay = payCooldowns.get(sender.getUUID());
            if (lastPay != null && System.currentTimeMillis() - lastPay < getPayCooldownMs()) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.cooldown"));
                return 0;
            }
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

        NeoLog.debug(LOGGER, LogCategory.ECONOMY,
            "pay: sender={} recipient={} amount={} taxPercent={} fee={} netAmount={}",
            sender.getUUID(), finalRecipientUUID, amount, taxPercent, fee, netAmount);

        boolean success = com.zerog.neoessentials.api.EconomyAPI.payPlayer(
            sender.getUUID(), finalRecipientUUID, amount, taxPercent);
        if (!success) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                "pay: transaction failed (insufficient funds) sender={} amount={}", sender.getUUID(), amount);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.pay.insufficient_funds"));
            return 0;
        }
        NeoLog.debug(LOGGER, LogCategory.ECONOMY, "pay: transaction committed sender={} recipient={} netAmount={}",
            sender.getUUID(), finalRecipientUUID, netAmount);

        // Only now — after every validation check has passed and the transfer actually went
        // through — start the cooldown. Consuming it any earlier meant a mistyped name, a
        // toggled-off recipient, or insufficient funds still cost the player a full cooldown
        // for a payment that never happened.
        if (!bypassCooldown) {
            payCooldowns.put(sender.getUUID(), System.currentTimeMillis());
        }

        String currency = EconomyManager.getInstance().getCurrencySymbol();
        // amount keeps whatever scale the parsed input had (e.g. "1000.0"), while fee/netAmount
        // always come out at scale 2 from the BigDecimal arithmetic above — normalize here so
        // all three display consistently.
        java.math.BigDecimal displayAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.pay.success_fee",
            displayAmount, finalRecipientName, fee, netAmount, currency), false);

        // Notify recipient if online
        if (onlineRecipient != null) {
            onlineRecipient.sendSystemMessage(MessageUtil.info(
                "commands.neoessentials.pay.received_fee",
                netAmount, sender.getGameProfile().name(), fee, currency));
        }

        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance()
            .addTransaction(sender.getUUID(), MessageUtil.localize(
                "commands.neoessentials.transaction.paid", finalRecipientName, amount, fee));
        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance()
            .addTransaction(finalRecipientUUID, MessageUtil.localize(
                "commands.neoessentials.transaction.received", netAmount,
                sender.getGameProfile().name(), fee));

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new com.zerog.neoessentials.economy.events.EconomyTransactionEvent(
                com.zerog.neoessentials.economy.events.EconomyTransactionEvent.Type.PAY,
                sender.getUUID(), finalRecipientUUID, netAmount,
                MessageUtil.localize("commands.neoessentials.transaction.pay_description", fee)));

        BaltopCommand.invalidateCache();
        return 1;
    }
}
