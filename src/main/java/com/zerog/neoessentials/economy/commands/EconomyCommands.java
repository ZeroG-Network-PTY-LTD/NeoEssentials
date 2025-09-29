package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.economy.EconomyLocalization;
import com.zerog.neoessentials.economy.EconomyPlayerUtil;
import com.zerog.neoessentials.economy.EconomyTransactionLogger;
import com.zerog.neoessentials.economy.EconomyConfig;
import com.zerog.neoessentials.economy.managers.PayToggleManager;
import com.zerog.neoessentials.economy.events.EconomyTransactionEvent;
import com.zerog.neoessentials.economy.managers.TransactionHistoryManager;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// NOTE: Use LangUtil.translate for all user-facing messages to ensure proper localization.

public class EconomyCommands {
    // Rate limiting maps
    private static final Map<UUID, Long> payCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> paytoggleCooldowns = new ConcurrentHashMap<>();
    private static final long PAY_COOLDOWN_MS = 3000; // 3 seconds
    private static final long PAYTOGGLE_COOLDOWN_MS = 2000; // 2 seconds

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
            .executes(ctx -> showBalance(ctx, ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(src -> src.hasPermission(2))
                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                        .map(p -> p.getGameProfile().getName()),
                    builder
                ))
                .executes(ctx -> showOtherBalance(ctx)))
        );
        dispatcher.register(Commands.literal("bal")
            .executes(ctx -> showBalance(ctx, ctx.getSource().getPlayerOrException()))
        );
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                        .map(p -> p.getGameProfile().getName()),
                    builder
                ))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(ctx -> payPlayer(ctx))))
        );
        dispatcher.register(Commands.literal("eco")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("give")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> ecoGive(ctx)))))
            .then(Commands.literal("take")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> ecoTake(ctx)))))
            .then(Commands.literal("set")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(ctx -> ecoSet(ctx)))))
            .then(Commands.literal("history")
                .executes(ctx -> showHistory(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", StringArgumentType.word())
                    .requires(src -> src.hasPermission(2))
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .executes(ctx -> showOtherHistory(ctx))
                )
            )
        );
        dispatcher.register(Commands.literal("baltop")
            .executes(ctx -> baltop(ctx, 10))
            .then(Commands.argument("number", DoubleArgumentType.doubleArg(1, 50))
                .executes(ctx -> baltop(ctx, (int) DoubleArgumentType.getDouble(ctx, "number"))))
        );
        dispatcher.register(Commands.literal("paytoggle")
            .executes(ctx -> payToggle(ctx))
        );
    }

    private static int showBalance(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (!checkEconomyEnabled(ctx)) return 0;
        UUID uuid = player.getUUID();
        BigDecimal balance = EconomyManager.getInstance().getBalance(uuid);
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.balance", currency, balance), false);
        return 1;
    }

    private static int showOtherBalance(CommandContext<CommandSourceStack> ctx) {
        if (!checkEconomyEnabled(ctx)) return 0;
        String playerName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.balance.player_not_found"));
            return 0;
        }
        BigDecimal balance = EconomyManager.getInstance().getBalance(uuidOpt.get());
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.balance", currency, balance), false);
        return 1;
    }

    private static int payPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        long now = System.currentTimeMillis();
        if (payCooldowns.containsKey(sender.getUUID()) && now - payCooldowns.get(sender.getUUID()) < PAY_COOLDOWN_MS) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.cooldown"));
            return 0;
        }
        payCooldowns.put(sender.getUUID(), now);
        if (!checkEconomyEnabled(ctx)) return 0;
        String targetName = StringArgumentType.getString(ctx, "player");
        double amountRaw = DoubleArgumentType.getDouble(ctx, "amount");
        if (amountRaw <= 0.0) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.pay.invalid_amount"));
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
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
        // Check paytoggle
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
        // Log transaction history
        TransactionHistoryManager.getInstance().addTransaction(sender.getUUID(), "Paid " + targetName + " " + amount + " (Fee: " + fee + ")");
        TransactionHistoryManager.getInstance().addTransaction(recipient.getUUID(), "Received " + netAmount + " from " + sender.getGameProfile().getName() + " (Fee: " + fee + ")");
        // Fire event
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new EconomyTransactionEvent(
            EconomyTransactionEvent.Type.PAY,
            sender.getUUID(),
            recipient.getUUID(),
            netAmount,
            "Player pay command (Fee: " + fee + ")"
        ));
        return 1;
    }

    private static int ecoGive(CommandContext<CommandSourceStack> ctx) {
        return ecoAdminAction(ctx, "give");
    }
    private static int ecoTake(CommandContext<CommandSourceStack> ctx) {
        return ecoAdminAction(ctx, "take");
    }
    private static int ecoSet(CommandContext<CommandSourceStack> ctx) {
        return ecoAdminAction(ctx, "set");
    }

    private static int ecoAdminAction(CommandContext<CommandSourceStack> ctx, String action) {
        if (!checkEconomyEnabled(ctx)) return 0;
        String playerName = StringArgumentType.getString(ctx, "player");
        double amountRaw = DoubleArgumentType.getDouble(ctx, "amount");
        BigDecimal amount = BigDecimal.valueOf(amountRaw);
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.eco.player_not_found"));
            return 0;
        }
        UUID uuid = uuidOpt.get();
        EconomyManager manager = EconomyManager.getInstance();
        String adminName = ctx.getSource().getTextName();
        switch (action) {
            case "give":
                manager.addBalance(uuid, amount);
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.eco.give", amount, playerName), false);
                EconomyTransactionLogger.log("ADMIN_GIVE", adminName, playerName, amount.toPlainString(), "Admin give");
                TransactionHistoryManager.getInstance().addTransaction(uuid, "Admin gave you " + amount);
                break;
            case "take":
                manager.subtractBalance(uuid, amount);
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.eco.take", amount, playerName), false);
                EconomyTransactionLogger.log("ADMIN_TAKE", adminName, playerName, amount.toPlainString(), "Admin take");
                TransactionHistoryManager.getInstance().addTransaction(uuid, "Admin took " + amount);
                break;
            case "set":
                manager.setBalance(uuid, amount);
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.eco.set", playerName, amount), false);
                EconomyTransactionLogger.log("ADMIN_SET", adminName, playerName, amount.toPlainString(), "Admin set");
                TransactionHistoryManager.getInstance().addTransaction(uuid, "Admin set your balance to " + amount);
                break;
        }
        return 1;
    }

    private static int baltop(CommandContext<CommandSourceStack> ctx, int count) {
        EconomyManager manager = EconomyManager.getInstance();
        Map<UUID, BigDecimal> balances = manager.getAllBalances();
        List<Map.Entry<UUID, BigDecimal>> top = balances.entrySet().stream()
            .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue(Comparator.reverseOrder()))
            .limit(count)
            .collect(Collectors.toList());
        ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.balancetop.header", 1, 1), false);
        int[] rank = {1};
        for (Map.Entry<UUID, BigDecimal> entry : top) {
            String name = ctx.getSource().getServer().getProfileCache().get(entry.getKey()).map(p -> p.getName()).orElse(entry.getKey().toString());
            final int currentRank = rank[0];
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.balancetop.entry", currentRank, name, entry.getValue()), false);
            rank[0]++;
        }
        return 1;
    }

    private static int payToggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long now = System.currentTimeMillis();
        if (paytoggleCooldowns.containsKey(player.getUUID()) && now - paytoggleCooldowns.get(player.getUUID()) < PAYTOGGLE_COOLDOWN_MS) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.paytoggle.cooldown"));
            return 0;
        }
        paytoggleCooldowns.put(player.getUUID(), now);
        UUID uuid = player.getUUID();
        boolean current = PayToggleManager.getInstance().getPayToggle(uuid);
        boolean newState = !current;
        PayToggleManager.getInstance().setPayToggle(uuid, newState);
        ctx.getSource().sendSuccess(() -> EconomyLocalization.component(
            newState ? "commands.neoessentials.paytoggle.enabled" : "commands.neoessentials.paytoggle.disabled"
        ), false);
        return 1;
    }

    private static int showHistory(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        UUID uuid = player.getUUID();
        List<String> history = TransactionHistoryManager.getInstance().getHistory(uuid);
        if (history.isEmpty()) {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.empty"), false);
        } else {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.header"), false);
            for (String entry : history) {
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.entry", entry), false);
            }
        }
        return 1;
    }
    private static int showOtherHistory(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.eco.player_not_found"));
            return 0;
        }
        List<String> history = TransactionHistoryManager.getInstance().getHistory(uuidOpt.get());
        if (history.isEmpty()) {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.empty"), false);
        } else {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.header"), false);
            for (String entry : history) {
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.entry", entry), false);
            }
        }
        return 1;
    }

    private static boolean checkEconomyEnabled(CommandContext<CommandSourceStack> ctx) {
        if (!EconomyManager.getInstance().isEnabled()) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.eco.disabled"));
            return false;
        }
        return true;
    }
}
