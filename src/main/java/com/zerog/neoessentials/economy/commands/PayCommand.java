package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.managers.EconomyManager;

import com.zerog.neoessentials.economy.EconomyLocalization;
import com.zerog.neoessentials.economy.managers.PayToggleManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PayCommand {
    private static final Map<UUID, Long> payCooldowns = new ConcurrentHashMap<>();
    private static final long PAY_COOLDOWN_MS = 3000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("pay")
                .requires(src -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer() != null ? src.getPlayer().getUUID() : null, "neoessentials.economy.pay"))
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
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        long now = System.currentTimeMillis();
        if (payCooldowns.containsKey(sender.getUUID()) && now - payCooldowns.get(sender.getUUID()) < PAY_COOLDOWN_MS) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.cooldown"));
            return 0;
        }
        payCooldowns.put(sender.getUUID(), now);
        if (!EconomyManager.getInstance().isEnabled()) return 0;
        String targetName = StringArgumentType.getString(ctx, "player");
        double amountRaw = DoubleArgumentType.getDouble(ctx, "amount");
        if (amountRaw <= 0.0) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.invalid_amount"));
            return 0;
        }
        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
        ServerPlayer recipient = server.getPlayerList().getPlayers().stream()
            .filter(p -> p.getGameProfile().getName().equalsIgnoreCase(targetName))
            .findFirst().orElse(null);
        if (recipient == null) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.player_not_found"));
            return 0;
        }
        if (recipient.getUUID().equals(sender.getUUID())) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.cannot_pay_self"));
            return 0;
        }
        if (!PayToggleManager.getInstance().getPayToggle(recipient.getUUID())) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.toggled_off"));
            return 0;
        }
        java.math.BigDecimal amount = java.math.BigDecimal.valueOf(amountRaw);
        double taxPercent = EconomyManager.getInstance().getConfig().taxPercentage;
        java.math.BigDecimal fee = amount.multiply(java.math.BigDecimal.valueOf(taxPercent / 100.0));
        java.math.BigDecimal netAmount = amount.subtract(fee);
        boolean success = com.zerog.neoessentials.api.EconomyAPI.payPlayer(sender.getUUID(), recipient.getUUID(), amount);
        if (!success) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.insufficient_funds"));
            return 0;
        }
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> EconomyLocalization.component(
            "commands.neoessentials.pay.success_fee",
            targetName, amount, fee, netAmount, currency
        ), false);
        recipient.sendSystemMessage(EconomyLocalization.component(
            "commands.neoessentials.pay.received_fee",
            sender.getGameProfile().getName(), netAmount, fee, currency
        ));
        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance().addTransaction(sender.getUUID(), "Paid " + targetName + " " + amount + " (Fee: " + fee + ")");
        com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance().addTransaction(recipient.getUUID(), "Received " + netAmount + " from " + sender.getGameProfile().getName() + " (Fee: " + fee + ")");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.zerog.neoessentials.economy.events.EconomyTransactionEvent(
            com.zerog.neoessentials.economy.events.EconomyTransactionEvent.Type.PAY,
            sender.getUUID(),
            recipient.getUUID(),
            netAmount,
            "Player pay command (Fee: " + fee + ")"
        ));
        return 1;
    }
}
